# File Storage Service — 05. Non-Functional Requirements (v2.0 — Microservices)
> Thay thế `06-nfr-ver1.0.md`. Cấu trúc giữ tương tự nhưng mọi mục được viết lại theo ranh giới 8 service độc lập thay vì 1 monolith.

## 1. Security — authentication/session
`identity-service` là nơi duy nhất verify credential và ký JWT (RS256/ES256 ưu tiên để các service khác verify bằng public key mà không cần gọi ngược). Access token 15 phút, refresh token 7 ngày lưu hash trong `identity_db`. `api-gateway` verify chữ ký + `exp` + `token_version` claim tại edge trước khi forward — các service phía sau (filesystem/upload/download...) **tin tưởng claim đã được gateway verify**, chỉ đọc `userId/role` từ header, không tự verify lại JWT (giảm tải, nhưng đồng nghĩa gateway là single point phải đúng tuyệt đối — xem R-201).

Password Argon2id/BCrypt; reset token hash + one-time + TTL ngắn. Auth endpoint rate-limit 5 req/phút/IP qua Redis tại `api-gateway` (chặn sớm nhất có thể, trước khi vào `identity-service`).

## 2. Security — giao tiếp nội bộ giữa service
Đây là bề mặt tấn công **mới xuất hiện** so với v1.0 monolith: các API `/internal/**` (filesystem-service expose cho upload/download/virus-scan) không đi qua gateway, nhưng vẫn phải xác thực — dùng `X-Internal-Token` (shared secret cấu hình qua biến môi trường, khác nhau giữa dev/prod). Network policy (Docker network riêng cho internal traffic) là lớp phòng thủ thứ hai. Không dùng lại JWT user cho internal call vì gọi này không đại diện 1 user cụ thể lúc đó (ví dụ virus-scan-service gọi thay mặt hệ thống, không thay mặt user).

## 3. Security — authorization và sharing
Không đổi nhiều so với v1.0 nhưng giờ **chỉ `filesystem-service` giữ logic này** — không service nào khác được tự ý quyết định quyền truy cập file. Share token CSPRNG; PRIVATE password hash; Redis đếm sai `(shareId, hash(IP+UA))` TTL 15 phút, khóa theo cặp (đã chốt). Trash luôn bị loại khỏi resolve trước khi trả kết quả.

## 4. Security — object storage và input
MinIO credentials chỉ nằm ở `upload-service`/`download-service`/`virus-scan-service` (3 service có lý do đọc/ghi MinIO); các service khác (identity, notification, audit-analytics) **không có** credential MinIO — nguyên tắc least privilege giữa các service, không chỉ giữa user/backend như v1.0. CORS MinIO allowlist đúng origin frontend. Path normalize/validate ở `filesystem-service` trước khi tạo hierarchy. MIME allowlist enforce ở `upload-service` trước khi presign; không coi allowlist là malware control (virus scan mới là gate thật).

## 5. Reliability — malware gate (không đổi bản chất, đổi nơi tính checksum)
`File.status` bắt đầu `PENDING_SCAN`, chỉ `filesystem-service` được chuyển `ACTIVE` (qua API nội bộ `/activate` do `virus-scan-service` gọi) hoặc xóa (`/reject`). `download-service` luôn kiểm tra `ACTIVE` ngay trước khi presign — không cache trạng thái này lâu hơn 1 request. Checksum được tính **một lần duy nhất** trong `virus-scan-service` cùng lúc quét virus (fix vấn đề đọc object 2 lần của v1.0).

## 6. Reliability — nhất quán dữ liệu xuyên service (khác biệt lớn nhất so với v1.0)
Không còn 1 transaction DB duy nhất bao trùm nghiệp vụ như monolith. Các điểm cần nhất quán được xử lý như sau:
- **Reserve quota + tạo File**: gộp thành 1 API nội bộ `POST /internal/files` trong `filesystem-service`, xử lý atomic ở phía đó (1 transaction cục bộ) — `upload-service` chỉ gọi và chờ kết quả thành/bại, không tự làm 2 bước riêng.
- **Đăng ký user → tạo root folder**: eventual consistency qua Kafka (`USER_REGISTERED`), có cửa sổ trễ ngắn; API filesystem trả `404 ROOT_NOT_PROVISIONED_YET` trong lúc chờ thay vì lỗi mơ hồ.
- **Nhiễm virus → xóa file + trả quota**: gộp vào 1 API nội bộ `/reject` xử lý atomic trong `filesystem-service`, tránh tình trạng "xóa file nhưng quên trả quota" (lỗi đã có ở v1.0).
- **Không có 2PC/distributed transaction** giữa các service — mọi thao tác cross-service hoặc là 1 lời gọi REST đồng bộ có timeout+retry ngắn (chấp nhận thất bại toàn bộ nếu phía kia down), hoặc là Kafka event chấp nhận độ trễ.

## 7. Reliability — DB/Kafka/MinIO consistency (kế thừa v1.0, mở rộng cho nhiều service)
Mỗi service publish event quan trọng qua Outbox riêng của nó (không dùng chung 1 outbox). Consumer dedupe theo `eventId`. Retry exponential backoff + DLQ theo từng consumer group. Upload TTL scheduler (trong `upload-service`) abort MinIO multipart + gọi `filesystem-service` release quota — 2 hành động độc lập, cần cả hai cùng thành công hoặc retry riêng lẻ (không atomic được vì khác hệ thống, chấp nhận đây là nơi cần idempotent + retry thay vì transaction).

## 8. Performance
Binary luôn đi trực tiếp browser↔MinIO ở cả single-file lẫn multipart. Gọi REST nội bộ (`upload-service`/`download-service` → `filesystem-service`) là đồng bộ nhưng nhẹ (chỉ trao đổi metadata nhỏ, không phải bytes) — không phải nguồn nghẽn chính. Virus scan/checksum stream object 1 lần duy nhất, không đọc lại. ZIP archive stream thay vì buffer toàn bộ RAM.

## 9. Scalability
Mỗi service scale độc lập theo tải riêng — đây là lợi ích chính của việc tách service so với v1.0: `virus-scan-service` có thể scale nhiều instance theo backlog Kafka mà không ảnh hưởng `identity-service`. Kafka partition theo `fileId` giữ ordering trong phạm vi 1 file. Chưa cần service mesh/Kubernetes cho quy mô đồ án — Docker Compose scale bằng cách tăng replica container thủ công nếu cần demo.

## 10. Availability và graceful degradation (mở rộng — nhiều failure domain hơn v1.0)
Vì có nhiều service hơn, cần định nghĩa rõ **điều gì xảy ra khi 1 service down**:
- `filesystem-service` down → gần như toàn hệ thống ngưng hoạt động vì mọi service khác phụ thuộc nó (single point of failure về mặt logic, dù không phải về hạ tầng) — đây là đánh đổi đã biết của việc gom toàn bộ file/folder/quota/share vào 1 service, xem R-201.
- `upload-service` down → upload mới thất bại nhưng file đã ACTIVE vẫn download được bình thường (không phụ thuộc upload-service).
- `virus-scan-service` down/chậm → file mới kẹt ở `PENDING_SCAN` lâu hơn, không download được, nhưng không có gì bị mất — retry khi service sống lại.
- `notification-service`/`audit-analytics-service` down → không ảnh hưởng luồng chính, Kafka giữ event, xử lý bù sau.
- Redis down → fail-closed cho unlock share PRIVATE/auth rate limit (không âm thầm bỏ qua bảo vệ brute-force).

## 11. Observability (mở rộng cho nhiều service)
`correlationId` sinh tại `api-gateway`, forward qua mọi REST call nội bộ (header) và Kafka event (field trong envelope) — bắt buộc để trace 1 request xuyên qua nhiều service khi debug. Mỗi service có `/actuator/health` riêng. Metrics quan trọng thêm so với v1.0: latency + tỉ lệ lỗi của từng cặp gọi REST nội bộ (ví dụ `upload-service → filesystem-service`), vì đây là điểm nghẽn/lỗi mới không tồn tại khi còn là monolith.

## 12. Configurability
Mỗi service có `application.yml` riêng, không dùng chung 1 file cấu hình như monolith. Secrets (DB password, internal-token, MinIO key) qua biến môi trường/`.env`, không commit. `multipart-threshold=100MB`, `upload-session-ttl=24h`, `presigned-url-ttl=5m`, MIME allowlist, auth rate limit, share password threshold — mỗi giá trị nằm trong `application.yml` của đúng service cần nó (ví dụ `multipart-threshold` chỉ ở `upload-service`, không lặp lại ở service khác).

## 13. Truy vết
Toàn bộ NFR bảo mật/reliability của v1.0 vẫn giữ nguyên ở tầng nghiệp vụ; điểm khác biệt là mục 2 (bảo mật giao tiếp nội bộ) và mục 6 (nhất quán xuyên service) là 2 mục hoàn toàn mới, phát sinh từ việc chuyển sang microservices, không tồn tại trong bản v1.0.
