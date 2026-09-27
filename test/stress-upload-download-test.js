// stress-upload-download-test.js
// Mục đích: tìm giới hạn riêng của luồng upload + download, nơi thường chịu tải
// nặng nhất trong hệ thống (MinIO, virus-scan-service, filesystem-service).
// Mỗi VU tạo user 1 lần, sau đó lặp vô hạn: upload file rồi tải lại chính file đó.
//
// Chạy: k6 run stress-upload-download-test.js
// Đổi kích thước file: k6 run -e FILE_SIZE_KB=1024 stress-upload-download-test.js

import http from 'k6/http';
import { check, sleep, fail } from 'k6';
import { BASE_URL, generateFileContent, randomEmail, randomPassword, headersJson } from './config.js';

const FILE_SIZE_BYTES = (Number(__ENV.FILE_SIZE_KB) || 100) * 1024;

export const options = {
  scenarios: {
    stress: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: '1m', target: 20 },
        { duration: '2m', target: 60 },
        { duration: '2m', target: 120 },
        { duration: '2m', target: 200 },
        { duration: '2m', target: 350 },
      ],
    },
  },
  thresholds: {
    http_req_failed: [
      { threshold: 'rate<0.10', abortOnFail: true, delayAbortEval: '15s' },
    ],
    'http_req_duration{name:upload_init}': [
      { threshold: 'p(95)<5000', abortOnFail: true, delayAbortEval: '15s' },
    ],
  },
};

function loginNewUser() {
  const email = randomEmail('stress');
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

  const authHeaders = {
    'Content-Type': 'application/json',
    Authorization: `Bearer ${JSON.parse(loginRes.body).accessToken}`,
  };

  let rootFolderId = null;
  for (let i = 0; i < 5; i++) {
    const rootRes = http.get(`${BASE_URL}/folders/root`, { headers: authHeaders });
    if (rootRes.status === 200) {
      rootFolderId = JSON.parse(rootRes.body).id;
      break;
    }
    sleep(1);
  }
  if (!rootFolderId) return null;

  return { authHeaders, rootFolderId };
}

let vuContext = null;

export default function () {
  if (!vuContext) {
    vuContext = loginNewUser();
    // Nếu không tạo được user (hệ thống đã quá tải ở bước auth), bỏ qua iteration
    // này thay vì fail cứng, để test tiếp tục chạy và đo đúng điểm sập thật sự.
    if (!vuContext) {
      sleep(1);
      return;
    }
  }
  const { authHeaders, rootFolderId } = vuContext;

  // Upload
  const initRes = http.post(
    `${BASE_URL}/uploads`,
    JSON.stringify({
      parentFolderId: rootFolderId,
      name: `stress-${__VU}-${__ITER}-${Date.now()}.txt`,
      size: FILE_SIZE_BYTES,
      mimeType: 'text/plain',
    }),
    { headers: authHeaders, tags: { name: 'upload_init' } }
  );
  const initOk = check(initRes, { 'upload init: không phải 5xx': (r) => r.status < 500 });
  if (!initOk || initRes.status !== 201) {
    sleep(1);
    return;
  }

  const initBody = JSON.parse(initRes.body);

  const putRes = http.put(initBody.uploadUrl, generateFileContent(FILE_SIZE_BYTES), {
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

  // Download ngay file vừa upload (thường vẫn PENDING_SCAN nên có thể 409,
  // đây là hành vi đúng, không tính là lỗi hệ thống).
  const dlRes = http.get(`${BASE_URL}/downloads/files/${initBody.fileId}/url`, {
    headers: authHeaders,
    tags: { name: 'download_url' },
  });
  check(dlRes, {
    'download url: không phải 5xx': (r) => r.status < 500,
  });

  sleep(0.3);
}
