// auth-load-test.js
// Đo hiệu năng riêng identity-service (register + login) qua api-gateway.
// Chạy: k6 run auth-load-test.js

import http from 'k6/http';
import { check, sleep } from 'k6';
import { BASE_URL, randomEmail, randomPassword, headersJson } from './config.js';

export const options = {
  scenarios: {
    ramp_up_auth: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: '30s', target: 20 },
        { duration: '1m', target: 20 },
        { duration: '30s', target: 50 },
        { duration: '1m', target: 50 },
        { duration: '30s', target: 0 },
      ],
    },
  },
  thresholds: {
    // Auth endpoint có rate-limit 5 req/phút/IP ở gateway (xem 05-nfr mục 1)
    // nên với nhiều VU cùng lúc, tỉ lệ lỗi 429 là dự kiến, không phải bug.
    'http_req_duration{name:register}': ['p(95)<800'],
    'http_req_duration{name:login}': ['p(95)<500'],
  },
};

export default function () {
  const email = randomEmail('auth');
  const password = randomPassword();

  const registerRes = http.post(
    `${BASE_URL}/auth/register`,
    JSON.stringify({ email, password }),
    { headers: headersJson, tags: { name: 'register' } }
  );
  check(registerRes, {
    'register: 201 hoặc 429 (rate limit)': (r) => r.status === 201 || r.status === 429,
  });

  if (registerRes.status !== 201) {
    sleep(1);
    return;
  }

  sleep(0.5);

  const loginRes = http.post(
    `${BASE_URL}/auth/login`,
    JSON.stringify({ email, password }),
    { headers: headersJson, tags: { name: 'login' } }
  );
  check(loginRes, {
    'login: 200 hoặc 429 (rate limit)': (r) => r.status === 200 || r.status === 429,
  });

  sleep(1);
}
