// upload-load-test.js
// Đo hiệu năng luồng upload single-file: init -> PUT MinIO -> confirm.
// Mỗi VU tự đăng ký + đăng nhập 1 lần trong setup của iteration đầu, sau đó lặp lại upload.
// Chạy: k6 run upload-load-test.js
// Đổi kích thước file test: k6 run -e FILE_SIZE_KB=500 upload-load-test.js

import http from 'k6/http';
import { check, sleep, fail } from 'k6';
import { BASE_URL, generateFileContent, randomEmail, randomPassword, headersJson } from './config.js';

const FILE_SIZE_BYTES = (Number(__ENV.FILE_SIZE_KB) || 100) * 1024;

export const options = {
  scenarios: {
    ramp_up_upload: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: '30s', target: 10 },
        { duration: '2m', target: 10 },
        { duration: '30s', target: 30 },
        { duration: '2m', target: 30 },
        { duration: '30s', target: 0 },
      ],
    },
  },
  thresholds: {
    'http_req_duration{name:upload_init}': ['p(95)<600'],
    'http_req_duration{name:minio_put}': ['p(95)<2000'],
    'http_req_duration{name:upload_confirm}': ['p(95)<800'],
    http_req_failed: ['rate<0.01'],
  },
};

// Chạy 1 lần mỗi VU khi bắt đầu, tạo user riêng cho VU đó và lấy accessToken + rootFolderId.
export function setup() {
  return {};
}

function loginNewUser() {
  const email = randomEmail('upload');
  const password = randomPassword();

  const registerRes = http.post(
    `${BASE_URL}/auth/register`,
    JSON.stringify({ email, password }),
    { headers: headersJson }
  );
  if (registerRes.status !== 201) {
    fail(`Không đăng ký được user cho VU: ${registerRes.status} ${registerRes.body}`);
  }

  const loginRes = http.post(
    `${BASE_URL}/auth/login`,
    JSON.stringify({ email, password }),
    { headers: headersJson }
  );
  if (loginRes.status !== 200) {
    fail(`Không đăng nhập được user cho VU: ${loginRes.status} ${loginRes.body}`);
  }
  const accessToken = JSON.parse(loginRes.body).accessToken;
  const authHeaders = {
    'Content-Type': 'application/json',
    Authorization: `Bearer ${accessToken}`,
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
  if (!rootFolderId) {
    fail('Root folder không được tạo kịp cho VU, kiểm tra tải Kafka consumer');
  }

  return { authHeaders, rootFolderId };
}

// Cache theo VU để không phải đăng ký user mới mỗi iteration.
let vuContext = null;

export default function () {
  if (!vuContext) {
    vuContext = loginNewUser();
  }
  const { authHeaders, rootFolderId } = vuContext;

  const initRes = http.post(
    `${BASE_URL}/uploads`,
    JSON.stringify({
      parentFolderId: rootFolderId,
      name: `loadtest-${__VU}-${__ITER}-${Date.now()}.txt`,
      size: FILE_SIZE_BYTES,
      mimeType: 'text/plain',
    }),
    { headers: authHeaders, tags: { name: 'upload_init' } }
  );

  const initOk = check(initRes, {
    'upload init: 201': (r) => r.status === 201,
  });
  if (!initOk) {
    sleep(1);
    return;
  }

  const initBody = JSON.parse(initRes.body);

  const putRes = http.put(initBody.uploadUrl, generateFileContent(FILE_SIZE_BYTES), {
    headers: { 'Content-Type': 'text/plain' },
    tags: { name: 'minio_put' },
  });
  check(putRes, { 'MinIO PUT: 200': (r) => r.status === 200 });

  const confirmRes = http.post(
    `${BASE_URL}/uploads/${initBody.fileId}/confirm`,
    JSON.stringify({}),
    { headers: authHeaders, tags: { name: 'upload_confirm' } }
  );
  check(confirmRes, {
    'confirm: 202': (r) => r.status === 202,
  });

  sleep(1);
}
