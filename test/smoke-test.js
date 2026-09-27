// smoke-test.js
// Chạy trước tiên, với tải rất nhỏ, để xác nhận toàn bộ luồng chạy đúng
// trước khi chạy load test nặng. Nếu smoke test fail, load test cũng sẽ fail.
//
// Chạy: k6 run smoke-test.js
// Chạy với BASE_URL khác: k6 run -e BASE_URL=http://localhost:8080/api/v1 smoke-test.js

import http from 'k6/http';
import { check, sleep, fail } from 'k6';
import { BASE_URL, generateFileContent, randomEmail, randomPassword, headersJson } from './config.js';

export const options = {
  vus: 1,
  iterations: 1,
  thresholds: {
    http_req_failed: ['rate==0'],
  },
};

export default function () {
  const email = randomEmail('smoke');
  const password = randomPassword();

  // 1. Đăng ký
  const registerRes = http.post(
    `${BASE_URL}/auth/register`,
    JSON.stringify({ email, password }),
    { headers: headersJson }
  );
  check(registerRes, {
    'register: status 201': (r) => r.status === 201,
  }) || fail(`Register thất bại: ${registerRes.status} ${registerRes.body}`);

  // 2. Đăng nhập
  const loginRes = http.post(
    `${BASE_URL}/auth/login`,
    JSON.stringify({ email, password }),
    { headers: headersJson }
  );
  check(loginRes, {
    'login: status 200': (r) => r.status === 200,
    'login: có accessToken': (r) => JSON.parse(r.body).accessToken !== undefined,
  }) || fail(`Login thất bại: ${loginRes.status} ${loginRes.body}`);

  const accessToken = JSON.parse(loginRes.body).accessToken;
  const authHeaders = {
    'Content-Type': 'application/json',
    Authorization: `Bearer ${accessToken}`,
  };

  // 3. Lấy root folder, retry vì root được tạo bất đồng bộ qua Kafka (xem R-202)
  let rootFolderId = null;
  for (let i = 0; i < 5; i++) {
    const rootRes = http.get(`${BASE_URL}/folders/root`, { headers: authHeaders });
    if (rootRes.status === 200) {
      rootFolderId = JSON.parse(rootRes.body).id;
      break;
    }
    sleep(1);
  }
  check(rootFolderId, {
    'root folder: đã được provision': (id) => id !== null,
  }) || fail('Root folder không được tạo sau 5 giây, kiểm tra Kafka consumer của filesystem-service');

  // 4. Khởi tạo upload single-file
  const fileSize = 1024; // 1KB, đủ nhỏ để chắc chắn là luồng SINGLE
  const initRes = http.post(
    `${BASE_URL}/uploads`,
    JSON.stringify({
      parentFolderId: rootFolderId,
      name: `smoke-test-${Date.now()}.txt`,
      size: fileSize,
      mimeType: 'text/plain',
    }),
    { headers: authHeaders }
  );
  check(initRes, {
    'upload init: status 201': (r) => r.status === 201,
    'upload init: mode SINGLE': (r) => JSON.parse(r.body).mode === 'SINGLE',
  }) || fail(`Upload init thất bại: ${initRes.status} ${initRes.body}`);

  const initBody = JSON.parse(initRes.body);
  const fileId = initBody.fileId;
  const uploadUrl = initBody.uploadUrl;

  // 5. PUT binary thẳng lên MinIO qua presigned URL, không qua gateway
  const putRes = http.put(uploadUrl, generateFileContent(fileSize), {
    headers: { 'Content-Type': 'text/plain' },
  });
  check(putRes, {
    'MinIO PUT: status 200': (r) => r.status === 200,
  }) || fail(`PUT lên MinIO thất bại: ${putRes.status} ${putRes.body}`);

  // 6. Confirm upload
  const confirmRes = http.post(
    `${BASE_URL}/uploads/${fileId}/confirm`,
    JSON.stringify({}),
    { headers: authHeaders }
  );
  check(confirmRes, {
    'confirm: status 202': (r) => r.status === 202,
    'confirm: status PENDING_SCAN': (r) => JSON.parse(r.body).status === 'PENDING_SCAN',
  }) || fail(`Confirm thất bại: ${confirmRes.status} ${confirmRes.body}`);

  // 7. Chờ virus scan xử lý xong, thử lấy download URL
  // File chỉ tải được sau khi ACTIVE (xem 05-nfr mục 5), có thể mất vài giây tùy tải ClamAV
  let downloadReady = false;
  for (let i = 0; i < 10; i++) {
    const dlRes = http.get(`${BASE_URL}/downloads/files/${fileId}/url`, { headers: authHeaders });
    if (dlRes.status === 200) {
      downloadReady = true;
      break;
    }
    sleep(1);
  }
  check(downloadReady, {
    'file trở thành ACTIVE và tải được sau virus scan': (v) => v === true,
  });

  console.log('Smoke test hoàn tất. Toàn bộ luồng chính đã chạy đúng.');
}
