// breakpoint-test.js
// Mục đích: tìm điểm hệ thống bắt đầu sập, không phải đo hiệu năng ở tải cố định.
// k6 tăng dần VU đều đặn, không dừng theo thời gian mà dừng khi tỉ lệ lỗi
// vượt ngưỡng cho phép (abortOnFail). Điểm k6 dừng lại chính là giới hạn thật.
//
// Chạy: k6 run breakpoint-test.js
// Theo dõi song song: docker stats, để biết CPU/RAM chạm trần lúc nào.

import http from 'k6/http';
import { check, sleep } from 'k6';
import { BASE_URL, randomEmail, randomPassword, headersJson } from './config.js';

export const options = {
  scenarios: {
    breakpoint: {
      executor: 'ramping-vus',
      startVUs: 0,
      // Tăng liên tục, không có giai đoạn giữ ổn định. Nếu hệ thống chịu được
      // hết các stage này mà chưa abort, sửa target cao hơn và chạy lại.
      stages: [
        { duration: '1m', target: 50 },
        { duration: '2m', target: 150 },
        { duration: '2m', target: 300 },
        { duration: '2m', target: 500 },
        { duration: '2m', target: 800 },
        { duration: '2m', target: 1200 },
      ],
    },
  },
  thresholds: {
    // abortOnFail: true khiến k6 dừng toàn bộ test ngay khi điều kiện bị vi phạm,
    // không đợi hết stage. delayAbortEval cho hệ thống vài giây để ổn định trước
    // khi tính, tránh abort nhầm do 1-2 request lẻ tẻ lúc mới tăng VU.
    http_req_failed: [
      { threshold: 'rate<0.10', abortOnFail: true, delayAbortEval: '10s' },
    ],
    http_req_duration: [
      { threshold: 'p(95)<5000', abortOnFail: true, delayAbortEval: '10s' },
    ],
  },
};

export default function () {
  const email = randomEmail('breakpoint');
  const password = randomPassword();

  const registerRes = http.post(
    `${BASE_URL}/auth/register`,
    JSON.stringify({ email, password }),
    { headers: headersJson, tags: { name: 'register' } }
  );
  check(registerRes, { 'register: không phải 5xx': (r) => r.status < 500 });

  if (registerRes.status !== 201) {
    sleep(0.5);
    return;
  }

  const loginRes = http.post(
    `${BASE_URL}/auth/login`,
    JSON.stringify({ email, password }),
    { headers: headersJson, tags: { name: 'login' } }
  );
  check(loginRes, { 'login: không phải 5xx': (r) => r.status < 500 });

  if (loginRes.status !== 200) {
    sleep(0.5);
    return;
  }

  const authHeaders = {
    'Content-Type': 'application/json',
    Authorization: `Bearer ${JSON.parse(loginRes.body).accessToken}`,
  };

  const rootRes = http.get(`${BASE_URL}/folders/root`, {
    headers: authHeaders,
    tags: { name: 'get_root' },
  });
  check(rootRes, { 'root: không phải 5xx': (r) => r.status < 500 });

  sleep(0.5);
}

// In ra khi test dừng, dù dừng do abort hay chạy hết stage.
export function teardown() {
  console.log('Breakpoint test kết thúc. Xem VU tại thời điểm THRESHOLDS báo abort trong output phía trên, đó là điểm giới hạn.');
}
