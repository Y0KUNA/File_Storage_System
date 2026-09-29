// mixed-breakpoint-test.js (phiên bản sửa)
//
// Thay đổi chính so với bản cũ:
//  1. Request auth (register/login/root) được gắn tag flow:'auth', luồng thật
//     (đọc/upload) gắn flow:'core'. Threshold + abortOnFail CHỈ tính flow:'core',
//     nên chi phí băm mật khẩu không còn làm lệch kết quả "bao nhiêu user đồng thời".
//  2. Setup của mỗi VU là máy trạng thái có giới hạn retry (không đăng ký lại bằng
//     email mới mỗi lần thất bại => hết "retry storm" và tài khoản mồ côi).
//  3. Check chặt hơn: đúng status (kể cả status 0 = timeout/lỗi kết nối bị bắt).
//  4. Chỉ confirm upload khi MinIO PUT thành công.
//  5. Thêm metric: auth_duration, auth_failed, auth_gave_up, vus_ready.
//  6. Ramp đầu dài hơn (2 phút) để giảm bão đăng ký lúc khởi động.
//
// Chạy: k6 run mixed-breakpoint-test.js
// Theo dõi song song: docker stats (backend, DB, MinIO) và CPU của máy chạy k6.

import http from 'k6/http';
import { check, sleep } from 'k6';
import { Trend, Counter } from 'k6/metrics';
import { BASE_URL, generateFileContent, randomEmail, randomPassword, headersJson } from './config.js';

// ---- Tham số có thể chỉnh qua biến môi trường: k6 run -e READERS=1000 ... ----
const READERS = parseInt(__ENV.READERS || '1000');
const WRITERS = parseInt(__ENV.WRITERS || '500');
const FILE_SIZE = parseInt(__ENV.FILE_SIZE || String(50 * 1024));
const MAX_SETUP_ATTEMPTS = parseInt(__ENV.MAX_SETUP_ATTEMPTS || '6');

// ---- Metric tùy chỉnh ----
const authDuration = new Trend('auth_duration', true);
const authFailed = new Counter('auth_failed');
const authGaveUp = new Counter('auth_gave_up');
const vusReady = new Counter('vus_ready');

export const options = {
  scenarios: {
    readers: {
      executor: 'ramping-vus',
      exec: 'readerFlow',
      startVUs: 0,
      gracefulRampDown: '30s',
      stages: [
        { duration: '2m', target: Math.round(READERS * 0.25) },
        { duration: '2m', target: Math.round(READERS * 0.5) },
        { duration: '2m', target: Math.round(READERS * 0.75) },
        { duration: '2m', target: READERS },
      ],
    },
    writers: {
      executor: 'ramping-vus',
      exec: 'writerFlow',
      startVUs: 0,
      gracefulRampDown: '30s',
      stages: [
        { duration: '2m', target: Math.round(WRITERS * 0.17) },
        { duration: '2m', target: Math.round(WRITERS * 0.5) },
        { duration: '2m', target: Math.round(WRITERS * 0.67) },
        { duration: '2m', target: WRITERS },
      ],
    },
  },
  thresholds: {
    // Điều kiện dừng: chỉ dựa trên luồng thật (core), bỏ qua auth.
    'http_req_failed{flow:core}': [
      { threshold: 'rate<0.05', abortOnFail: true, delayAbortEval: '30s' },
    ],
    'http_req_duration{flow:core}': [
      { threshold: 'p(95)<5000', abortOnFail: true, delayAbortEval: '30s' },
    ],
    // Theo dõi từng endpoint (không abort) để biết bước nào chậm.
    'http_req_duration{name:list_children}': ['p(95)<2000'],
    'http_req_duration{name:get_quota}': ['p(95)<2000'],
    'http_req_duration{name:upload_init}': ['p(95)<3000'],
    'http_req_duration{name:minio_put}': ['p(95)<3000'],
    'http_req_duration{name:upload_confirm}': ['p(95)<3000'],
    // Auth chỉ để quan sát, ngưỡng rộng, không abort.
    'http_req_duration{name:auth_register}': ['p(95)<15000'],
    'http_req_duration{name:auth_login}': ['p(95)<15000'],
  },
};

// ---------------------------------------------------------------------------
// Setup mỗi VU: register -> login -> lấy root folder. Mỗi bước thất bại chỉ
// tăng bộ đếm attempts và thử lại bước đó ở iteration sau (cùng email).
// ---------------------------------------------------------------------------
function newCtx(prefix) {
  return {
    email: randomEmail(prefix),
    password: randomPassword(),
    registered: false,
    authHeaders: null,
    rootFolderId: null,
    attempts: 0,
    ready: false,
    gaveUp: false,
  };
}

function failStep(ctx) {
  authFailed.add(1);
  ctx.attempts += 1;
  if (ctx.attempts >= MAX_SETUP_ATTEMPTS && !ctx.gaveUp) {
    ctx.gaveUp = true;
    authGaveUp.add(1);
  }
  // backoff + jitter: 1s, 2s, 4s... tối đa 10s
  sleep(Math.min(2 ** (ctx.attempts - 1), 10) + Math.random());
}

function ensureReady(ctx) {
  if (ctx.ready) return true;
  if (ctx.gaveUp) {
    sleep(10); // VU đã bỏ cuộc: nằm chờ, không thêm tải
    return false;
  }

  if (!ctx.registered) {
    const res = http.post(
      `${BASE_URL}/auth/register`,
      JSON.stringify({ email: ctx.email, password: ctx.password }),
      { headers: headersJson, tags: { name: 'auth_register', flow: 'auth' } }
    );
    authDuration.add(res.timings.duration, { step: 'register' });
    // 409 = email đã tồn tại (lần trước đăng ký xong nhưng phản hồi bị mất)
    if (res.status === 201 || res.status === 409) {
      ctx.registered = true;
    } else {
      failStep(ctx);
      return false;
    }
  }

  if (!ctx.authHeaders) {
    const res = http.post(
      `${BASE_URL}/auth/login`,
      JSON.stringify({ email: ctx.email, password: ctx.password }),
      { headers: headersJson, tags: { name: 'auth_login', flow: 'auth' } }
    );
    authDuration.add(res.timings.duration, { step: 'login' });
    if (res.status === 200) {
      ctx.authHeaders = {
        'Content-Type': 'application/json',
        Authorization: `Bearer ${JSON.parse(res.body).accessToken}`,
      };
    } else {
      failStep(ctx);
      return false;
    }
  }

  if (!ctx.rootFolderId) {
    const res = http.get(`${BASE_URL}/folders/root`, {
      headers: ctx.authHeaders,
      tags: { name: 'auth_root', flow: 'auth' },
    });
    if (res.status === 200) {
      ctx.rootFolderId = JSON.parse(res.body).id;
    } else {
      failStep(ctx); // thư mục gốc có thể được tạo bất đồng bộ, thử lại sau
      return false;
    }
  }

  ctx.ready = true;
  vusReady.add(1);
  return true;
}

// Biến module-level là riêng cho từng VU trong k6.
let readerCtx = null;
let writerCtx = null;

// ---------------------------------------------------------------------------
export function readerFlow() {
  if (!readerCtx) readerCtx = newCtx('bpreader');
  if (!ensureReady(readerCtx)) return;
  const { authHeaders, rootFolderId } = readerCtx;

  const childrenRes = http.get(`${BASE_URL}/folders/${rootFolderId}/children`, {
    headers: authHeaders,
    tags: { name: 'list_children', flow: 'core' },
  });
  check(childrenRes, { 'list children: 200': (r) => r.status === 200 });

  const quotaRes = http.get(`${BASE_URL}/quota`, {
    headers: authHeaders,
    tags: { name: 'get_quota', flow: 'core' },
  });
  check(quotaRes, { 'quota: 200': (r) => r.status === 200 });

  sleep(Math.random() * 2 + 1);
}

// ---------------------------------------------------------------------------
export function writerFlow() {
  if (!writerCtx) writerCtx = newCtx('bpwriter');
  if (!ensureReady(writerCtx)) return;
  const { authHeaders, rootFolderId } = writerCtx;

  const initRes = http.post(
    `${BASE_URL}/uploads`,
    JSON.stringify({
      parentFolderId: rootFolderId,
      name: `bp-${__VU}-${__ITER}-${Date.now()}.txt`,
      size: FILE_SIZE,
      mimeType: 'text/plain',
    }),
    { headers: authHeaders, tags: { name: 'upload_init', flow: 'core' } }
  );
  if (!check(initRes, { 'upload init: 201': (r) => r.status === 201 })) {
    sleep(Math.random() * 3 + 2);
    return;
  }
  const initBody = JSON.parse(initRes.body);

  const putRes = http.put(initBody.uploadUrl, generateFileContent(FILE_SIZE), {
    headers: { 'Content-Type': 'text/plain' },
    tags: { name: 'minio_put', flow: 'core' },
  });
  const putOk = check(putRes, { 'MinIO PUT: 200': (r) => r.status === 200 });

  // Chỉ confirm khi dữ liệu đã lên MinIO, tránh sinh lỗi giả.
  if (putOk) {
    const confirmRes = http.post(
      `${BASE_URL}/uploads/${initBody.fileId}/confirm`,
      JSON.stringify({}),
      { headers: authHeaders, tags: { name: 'upload_confirm', flow: 'core' } }
    );
    check(confirmRes, {
      'confirm: 2xx': (r) => r.status >= 200 && r.status < 300,
    });
  }

  sleep(Math.random() * 3 + 2);
}