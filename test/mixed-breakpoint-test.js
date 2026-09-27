// mixed-breakpoint-test.js
// Khác breakpoint-test.js ở chỗ: mỗi VU chỉ đăng ký/đăng nhập 1 LẦN duy nhất,
// sau đó lặp lại hành vi thật (đọc folder, upload) nhiều lần. Đây mới là con số
// đúng để trả lời "hệ thống chịu được bao nhiêu người dùng đồng thời", vì
// không bị lệch bởi chi phí băm mật khẩu lặp lại không thực tế.
//
// Chạy: k6 run mixed-breakpoint-test.js
// Theo dõi song song: docker stats

import http from 'k6/http';
import { check, sleep, fail, group } from 'k6';
import { BASE_URL, generateFileContent, randomEmail, randomPassword, headersJson } from './config.js';

export const options = {
  scenarios: {
    readers: {
      executor: 'ramping-vus',
      exec: 'readerFlow',
      startVUs: 0,
      stages: [
        { duration: '1m', target: 140 },
        { duration: '2m', target: 350 },
        { duration: '2m', target: 700 },
        { duration: '2m', target: 1000 },
      ],
    },
    writers: {
      executor: 'ramping-vus',
      exec: 'writerFlow',
      startVUs: 0,
      stages: [
        { duration: '1m', target: 60 },
        { duration: '2m', target: 150 },
        { duration: '2m', target: 300 },
        { duration: '2m', target: 430 },
      ],
    },
  },
  thresholds: {
    http_req_failed: [
      { threshold: 'rate<0.10', abortOnFail: true, delayAbortEval: '15s' },
    ],
    http_req_duration: [
      { threshold: 'p(95)<2000', abortOnFail: true, delayAbortEval: '15s' },
    ],
  },
};

// Đăng ký/đăng nhập chỉ chạy 1 lần cho mỗi VU, không lặp lại trong vòng lặp
// iteration như breakpoint-test.js cũ. Đây là điểm khác biệt cốt lõi.
function registerAndLogin(prefix) {
  const email = randomEmail(prefix);
  const password = randomPassword();

  const registerRes = http.post(
    `${BASE_URL}/auth/register`,
    JSON.stringify({ email, password }),
    { headers: headersJson }
  );
  if (registerRes.status !== 201) return null;

  const loginRes = http.post(
    `${BASE_URL}/auth/login`,
    JSON.stringify({ email, password }),
    { headers: headersJson }
  );
  if (loginRes.status !== 200) return null;

  return {
    'Content-Type': 'application/json',
    Authorization: `Bearer ${JSON.parse(loginRes.body).accessToken}`,
  };
}

function getRootFolderId(authHeaders) {
  for (let i = 0; i < 5; i++) {
    const rootRes = http.get(`${BASE_URL}/folders/root`, { headers: authHeaders });
    if (rootRes.status === 200) {
      return JSON.parse(rootRes.body).id;
    }
    sleep(1);
  }
  return null;
}

let readerCtx = null;
let writerCtx = null;

export function readerFlow() {
  if (!readerCtx) {
    const authHeaders = registerAndLogin('bpreader');
    if (!authHeaders) {
      sleep(1);
      return;
    }
    const rootFolderId = getRootFolderId(authHeaders);
    if (!rootFolderId) {
      sleep(1);
      return;
    }
    readerCtx = { authHeaders, rootFolderId };
  }
  const { authHeaders, rootFolderId } = readerCtx;

  group('read_folder', () => {
    const childrenRes = http.get(
      `${BASE_URL}/folders/${rootFolderId}/children`,
      { headers: authHeaders, tags: { name: 'list_children' } }
    );
    check(childrenRes, { 'list children: không phải 5xx': (r) => r.status < 500 });
  });

  group('check_quota', () => {
    const quotaRes = http.get(`${BASE_URL}/quota`, {
      headers: authHeaders,
      tags: { name: 'get_quota' },
    });
    check(quotaRes, { 'quota: không phải 5xx': (r) => r.status < 500 });
  });

  sleep(Math.random() * 2 + 1);
}

export function writerFlow() {
  if (!writerCtx) {
    const authHeaders = registerAndLogin('bpwriter');
    if (!authHeaders) {
      sleep(1);
      return;
    }
    const rootFolderId = getRootFolderId(authHeaders);
    if (!rootFolderId) {
      sleep(1);
      return;
    }
    writerCtx = { authHeaders, rootFolderId };
  }
  const { authHeaders, rootFolderId } = writerCtx;

  const fileSize = 50 * 1024;

  group('upload_file', () => {
    const initRes = http.post(
      `${BASE_URL}/uploads`,
      JSON.stringify({
        parentFolderId: rootFolderId,
        name: `bp-${__VU}-${__ITER}-${Date.now()}.txt`,
        size: fileSize,
        mimeType: 'text/plain',
      }),
      { headers: authHeaders, tags: { name: 'upload_init' } }
    );
    if (!check(initRes, { 'upload init: không phải 5xx': (r) => r.status < 500 })) return;
    if (initRes.status !== 201) return;

    const initBody = JSON.parse(initRes.body);

    const putRes = http.put(initBody.uploadUrl, generateFileContent(fileSize), {
      headers: { 'Content-Type': 'text/plain' },
      tags: { name: 'minio_put' },
    });
    check(putRes, { 'MinIO PUT: không phải 5xx': (r) => r.status < 500 });

    const confirmRes = http.post(
      `${BASE_URL}/uploads/${initBody.fileId}/confirm`,
      JSON.stringify({}),
      { headers: authHeaders, tags: { name: 'upload_confirm' } }
    );
    check(confirmRes, { 'confirm: không phải 5xx': (r) => r.status < 500 });
  });

  sleep(Math.random() * 3 + 2);
}