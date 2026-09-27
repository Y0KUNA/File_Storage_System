// mixed-scenario-test.js
// Mô phỏng hành vi người dùng thật: phần lớn là đọc (list folder, download),
// một phần nhỏ hơn là ghi (upload). Dùng để đo hiệu năng toàn hệ thống dưới
// tải hỗn hợp, gần với thực tế hơn so với test riêng từng endpoint.
//
// Chạy: k6 run mixed-scenario-test.js
// Xem báo cáo tóm tắt dạng JSON: k6 run --summary-export=summary.json mixed-scenario-test.js

import http from 'k6/http';
import { check, sleep, fail, group } from 'k6';
import { BASE_URL, generateFileContent, randomEmail, randomPassword, headersJson } from './config.js';

export const options = {
  scenarios: {
    // 70% VU chỉ đọc: xem root folder + children
    readers: {
      executor: 'ramping-vus',
      exec: 'readerFlow',
      startVUs: 0,
      stages: [
        { duration: '30s', target: 35 },
        { duration: '3m', target: 35 },
        { duration: '30s', target: 0 },
      ],
    },
    // 30% VU ghi: upload file mới
    writers: {
      executor: 'ramping-vus',
      exec: 'writerFlow',
      startVUs: 0,
      stages: [
        { duration: '30s', target: 15 },
        { duration: '3m', target: 15 },
        { duration: '30s', target: 0 },
      ],
    },
  },
  thresholds: {
    http_req_failed: ['rate<0.02'],
    http_req_duration: ['p(95)<1500'],
  },
};

function registerAndLogin(prefix) {
  const email = randomEmail(prefix);
  const password = randomPassword();

  const registerRes = http.post(
    `${BASE_URL}/auth/register`,
    JSON.stringify({ email, password }),
    { headers: headersJson }
  );
  if (registerRes.status !== 201) {
    fail(`Đăng ký thất bại: ${registerRes.status} ${registerRes.body}`);
  }

  const loginRes = http.post(
    `${BASE_URL}/auth/login`,
    JSON.stringify({ email, password }),
    { headers: headersJson }
  );
  if (loginRes.status !== 200) {
    fail(`Đăng nhập thất bại: ${loginRes.status} ${loginRes.body}`);
  }
  const accessToken = JSON.parse(loginRes.body).accessToken;
  return {
    'Content-Type': 'application/json',
    Authorization: `Bearer ${accessToken}`,
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
  fail('Root folder không được tạo kịp');
}

let readerCtx = null;
let writerCtx = null;

export function readerFlow() {
  if (!readerCtx) {
    const authHeaders = registerAndLogin('reader');
    const rootFolderId = getRootFolderId(authHeaders);
    readerCtx = { authHeaders, rootFolderId };
  }
  const { authHeaders, rootFolderId } = readerCtx;

  group('read_folder', () => {
    const childrenRes = http.get(
      `${BASE_URL}/folders/${rootFolderId}/children`,
      { headers: authHeaders, tags: { name: 'list_children' } }
    );
    check(childrenRes, { 'list children: 200': (r) => r.status === 200 });
  });

  group('check_quota', () => {
    const quotaRes = http.get(`${BASE_URL}/quota`, {
      headers: authHeaders,
      tags: { name: 'get_quota' },
    });
    check(quotaRes, { 'quota: 200': (r) => r.status === 200 });
  });

  sleep(Math.random() * 2 + 1); // 1-3s, mô phỏng người dùng đọc màn hình
}

export function writerFlow() {
  if (!writerCtx) {
    const authHeaders = registerAndLogin('writer');
    const rootFolderId = getRootFolderId(authHeaders);
    writerCtx = { authHeaders, rootFolderId };
  }
  const { authHeaders, rootFolderId } = writerCtx;

  const fileSize = 50 * 1024; // 50KB, đại diện file nhỏ điển hình

  group('upload_file', () => {
    const initRes = http.post(
      `${BASE_URL}/uploads`,
      JSON.stringify({
        parentFolderId: rootFolderId,
        name: `mixed-${__VU}-${__ITER}-${Date.now()}.txt`,
        size: fileSize,
        mimeType: 'text/plain',
      }),
      { headers: authHeaders, tags: { name: 'upload_init' } }
    );
    if (!check(initRes, { 'upload init: 201': (r) => r.status === 201 })) return;

    const initBody = JSON.parse(initRes.body);

    const putRes = http.put(initBody.uploadUrl, generateFileContent(fileSize), {
      headers: { 'Content-Type': 'text/plain' },
      tags: { name: 'minio_put' },
    });
    check(putRes, { 'MinIO PUT: 200': (r) => r.status === 200 });

    const confirmRes = http.post(
      `${BASE_URL}/uploads/${initBody.fileId}/confirm`,
      JSON.stringify({}),
      { headers: authHeaders, tags: { name: 'upload_confirm' } }
    );
    check(confirmRes, { 'confirm: 202': (r) => r.status === 202 });
  });

  sleep(Math.random() * 3 + 2); // 2-5s giữa các lần upload
}
