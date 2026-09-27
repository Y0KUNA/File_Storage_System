// config.js
// Cấu hình dùng chung cho toàn bộ test. Sửa BASE_URL đúng port api-gateway của bạn.

export const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080/api/v1';

// Sinh nội dung file giả để upload. size tính bằng byte.
export function generateFileContent(size) {
  return 'x'.repeat(size);
}

// Sinh email ngẫu nhiên để tránh trùng khi chạy nhiều VU/iteration.
export function randomEmail(prefix) {
  const rand = Math.random().toString(36).substring(2, 10);
  return `${prefix}_${rand}_${Date.now()}@loadtest.local`;
}

export function randomPassword() {
  return 'LoadTest123!';
}

export const headersJson = { 'Content-Type': 'application/json' };
