# File Storage Service — 08. Risks & Open Items (v2.0 — Microservices)
> Thay thế `09-risks-open-items-ver1.0.md`. Risk nào đã được fix ở v2.0 được đánh dấu RESOLVED thay vì xóa, để giữ lịch sử quyết định.

## 1. Assumptions (giữ phần lớn từ v1.0, cập nhật theo kiến trúc mới)
- Tác động thấp: quy mô vẫn là đồ án/CV, không có concurrent user/throughput SLA — cho phép static service discovery, 1 Redis/1 Postgres instance chứa nhiều database logic thay vì hạ tầng riêng biệt hoàn toàn.
- Tác động thấp: JWT RS256, verify tại gateway, các service tin claim đã verify — chấp nhận gateway là điểm phải đúng tuyệt đối để đổi lấy đơn giản hóa cho từng service.
- Tác động thấp: checksum canonical SHA-256, tính bởi `virus-scan-service`, không còn là input bắt buộc từ client.
- Tác động thấp: 8 service là con số cố định cho MVP; không giả định thêm service mới ngoài danh sách đã liệt kê ở `01-architecture-microservices`.
- Không có `open_questions` nào bị bỏ ngỏ âm thầm — toàn bộ quyết định trim scope (merge, admin, download reconciliation) đều ghi rõ ở `00-scope-mvp-vs-stretch`, không phải giả định ẩn.

## 2. Risk đã giải quyết ở v2.0 (RESOLVED — giữ lại để tránh tái phát sinh)
### R-101 [RESOLVED] — Checksum verification tốn bandwidth backend
Đã fix bằng ADR-104: chuyển tính checksum sang `virus-scan-service`, dùng chung lượt đọc với virus scan. Không còn đọc object 2 lần.

### R-102 [RESOLVED] — Quota rò rỉ khi file nhiễm virus
Đã fix bằng ADR-105: gộp xóa file + trả quota vào 1 API atomic `/internal/files/{id}/reject`.

### R-103 [RESOLVED] — Size client khai không được đối soát
Đã fix bằng ADR-106: `upload-service` lấy size thật từ MinIO tại bước confirm/complete, ghi đè giá trị client khai.

## 3. Risk mới phát sinh từ việc chuyển sang microservices (MỚI)

### R-201 [MITIGATED] — `filesystem-service` là single point of failure về logic
Vì ADR-102 gom File/Folder/Quota/Share vào 1 service, service này down làm gần như toàn hệ thống ngưng hoạt động (upload/download/share đều cần nó). **Quyết định (ADR-116)**: thay vì tách nhỏ service (đánh đổi mất transaction atomic ở ADR-105, phải làm Saga), chọn giảm thiểu bằng **chạy nhiều replica** của `filesystem-service` đằng sau load balancer — xem `01-architecture-microservices` mục 12 và `06-tech-stack` mục 14. Đây là cách rẻ hơn để giải quyết đúng vấn đề "1 instance chết làm sập hệ thống" (availability) mà không cần giải quyết vấn đề "cô lập lỗi logic giữa các tính năng" (blast radius) — 2 mục tiêu khác nhau, dự án hiện chỉ cần mục tiêu đầu. Tác động: toàn bộ FR liên quan file, đã giảm từ "chưa xử lý" xuống "có phương án cụ thể".

### R-202 — Cửa sổ eventual consistency khi đăng ký user
`USER_REGISTERED` → root folder được tạo bất đồng bộ. Nếu `filesystem-service` chậm/down đúng lúc, user đăng ký xong nhưng gọi `GET /folders/root` liên tục nhận `404 ROOT_NOT_PROVISIONED_YET`. Giảm thiểu: FE nên có retry với backoff ngắn (vài lần trong 2-3 giây); nếu quá lâu, hiển thị thông báo "đang khởi tạo tài khoản" thay vì lỗi cứng. Tác động: FR-001, trải nghiệm đăng ký.

### R-203 — Cascading failure khi gọi REST nội bộ đồng bộ
`upload-service`/`download-service`/`virus-scan-service` gọi đồng bộ tới `filesystem-service`; nếu service đó chậm (không phải down hẳn), các service gọi nó có thể bị block/timeout hàng loạt, gây hiệu ứng dây chuyền. Giảm thiểu: timeout ngắn (2-3s) + retry giới hạn số lần (không retry vô hạn) + có thể cân nhắc circuit breaker (Resilience4j) như Stretch nếu muốn học thêm pattern này. Tác động: mọi luồng gọi nội bộ.

### R-204 — Không có JOIN xuyên service, dữ liệu tổng hợp có thể lệch tạm thời
`audit-analytics-service` tổng hợp dữ liệu qua Kafka event, không truy vấn trực tiếp `filesystem_db`. Nếu 1 event bị delay/mất tạm thời (trước khi DLQ retry), số liệu thống kê có thể sai lệch tạm thời so với trạng thái thật trong `filesystem_db`. Giảm thiểu: chấp nhận eventual consistency cho phần analytics (không phải nghiệp vụ core), monitor Kafka consumer lag. Tác động: FR-022 (đã trim), không ảnh hưởng luồng chính.

### R-205 — Internal-token dùng chung là single secret cho toàn bộ giao tiếp nội bộ
Nếu `X-Internal-Token` bị lộ (ví dụ log nhầm), toàn bộ API `/internal/**` của mọi service bị lộ theo, không phân quyền được "upload-service chỉ được gọi API X, không được gọi API Y". Giảm thiểu ở quy mô đồ án: đủ dùng vì network Docker đã cô lập; ghi rõ đây là giới hạn đã biết, không phải thiết kế production-grade (production cần mTLS + service identity riêng biệt, ghi vào Stretch).

### R-206 — Testing cần mock nhiều service hơn
Muốn test `upload-service` độc lập cần mock `filesystem-service` (WireMock) — nếu không quen kỹ thuật này, dễ rơi vào việc phải dựng toàn bộ 8 service chỉ để chạy 1 unit test, làm chậm vòng lặp phát triển. Giảm thiểu: viết interface/port cho lời gọi REST nội bộ ngay từ đầu (tương tự cách v1.0 dùng port `ObjectStorage`/`EventPublisher`), dễ mock hơn là gọi `RestClient` trực tiếp rải rác trong code.

## 4. Risk kế thừa nguyên vẹn từ v1.0 (vẫn còn hiệu lực, chưa đổi)
- **R-004** (Virus scanner throughput/security): giữ nguyên, nay là risk riêng của `virus-scan-service`.
- **R-005** (MIME allowlist có `application/octet-stream`): giữ nguyên, nay enforce ở `upload-service`.
- **R-006** (Presigned URL là capability tạm thời, không revoke được giữa chừng): giữ nguyên.
- **R-008** (Share inheritance query cost): giữ nguyên, đơn giản hơn vì đã bỏ merge (ADR-107), nhưng logic ancestor traversal vẫn còn.
- **R-009** (Share EDIT tiêu quota của owner): giữ nguyên.
- **R-010** (Fingerprint IP+UA không phải identity mạnh): giữ nguyên.
- **R-011** (Kafka duplicate/order/schema evolution): giữ nguyên, nay áp dụng cho nhiều consumer group hơn (8 service thay vì vài worker).
- **R-014** (Email provider chưa xác định production): giữ nguyên, MailHog đủ cho đồ án.

## 5. Open items cần chốt trước khi coi là "production-ready" (không chặn việc code MVP)
- Circuit breaker cho gọi REST nội bộ (R-203) — Resilience4j, Stretch.
- mTLS hoặc service identity riêng cho từng service thay vì 1 internal-token chung (R-205) — Stretch.
- Circuit breaker/timeout tuning cần benchmark thật khi có traffic, hiện chỉ đặt giá trị hợp lý theo kinh nghiệm (2-3s), không có số liệu đo thật.
- Load balancer cho các service khác ngoài `filesystem-service` (upload/download) nếu benchmark cho thấy cần — hiện chỉ `filesystem-service` được ưu tiên multi-replica vì là điểm phụ thuộc nhiều nhất (R-201).

## 6. Security review checklist bổ sung cho microservices (thêm vào checklist v1.0)
Ngoài checklist v1.0 (PENDING_SCAN/INFECTED không nhận download URL, ADMIN không đọc file USER, Trash không truy cập qua inherited share, move cycle bị reject, concurrent quota không oversubscribe...), thêm:
- API `/internal/**` không thể truy cập được từ bên ngoài qua `api-gateway` (test bằng cách gọi trực tiếp path này qua gateway, phải nhận 404/403).
- Service A không tự ý ghi trực tiếp vào database của Service B (kiểm tra bằng connection string/credential — mỗi service chỉ có quyền connect vào đúng 1 database của mình).
- JWT bị revoke (`token_version` tăng) phải có hiệu lực ngay ở gateway trong vòng tối đa thời gian cache public key/claim đã định nghĩa, không kéo dài vô thời hạn.

## 7. Truy vết
Mục 2 xác nhận 3 vấn đề đã phát hiện qua review trước đó nay đã có giải pháp kiến trúc cụ thể (không chỉ là "sẽ làm sau"). Mục 3 là risk hoàn toàn mới, đặc thù của microservices, không tồn tại khi còn là monolith — đây cũng là phần "học được gì" quan trọng nhất khi làm dự án theo hướng này.
