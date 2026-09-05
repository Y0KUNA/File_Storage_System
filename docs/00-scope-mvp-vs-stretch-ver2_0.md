# File Storage Service — 00. Scope: MVP vs Stretch (v2.0 — Microservices)
> Mục tiêu bản v2.0: đủ chất về mặt kỹ thuật để đưa vào CV (microservices, event-driven, presigned upload, security gate), nhưng đủ nhỏ để 1 người tự làm trong thời gian hợp lý.

## 1. Vì sao đổi sang microservices
Bản v1.0 dùng modular monolith + Kafka workers — hợp lý về mặt kỹ thuật, nhưng **không thể hiện được kỹ năng microservices thật sự** (service tự trị, database-per-service, giao tiếp liên service, saga/eventual consistency) là những gì nhà tuyển dụng thường tìm khi đọc CV có chữ "microservices". Bản v2.0 tách thành các service độc lập, mỗi service có database riêng, để bạn thực hành đúng các vấn đề mà microservices thật sự gặp phải (nhất quán dữ liệu xuyên service, service discovery, API gateway, event-driven integration).

## 2. Danh sách bị cắt/giảm so với v1.0 (và lý do)

| Nghiệp vụ v1.0 | Quyết định v2.0 | Lý do |
|---|---|---|
| Recursive folder merge (BR-030) khi trùng tên | **Cắt** → dùng chung 1 rule: tự động hậu tố tên `(n)` cho cả File lẫn Folder khi trùng, giống nhau ở mọi nơi (upload/restore/move) | Merge đệ quy + revoke ShareLink khi merge là nhiều edge case, tốn thời gian implement mà giá trị học thuật/CV thấp so với công sức bỏ ra. Đưa xuống mục Stretch. |
| ADMIN role + Dashboard Analytics đầy đủ | **Giảm** → chỉ còn 1 endpoint đọc thống kê cơ bản (tổng upload/download/user theo ngày) trong `audit-analytics-service`, bảo vệ bằng 1 role đơn giản gán sẵn qua seed data, không có UI quản trị | Xây dựng cả hệ role-based admin panel không phải trọng tâm học microservices; vẫn giữ lại phần "đọc dữ liệu tổng hợp từ Kafka" vì đây là điểm hay để demo CQRS-lite. |
| MinIO bucket notification cho **download** (`FILE_DOWNLOAD_CONFIRMED`) | **Chuyển xuống Stretch** — MVP chỉ có `FILE_DOWNLOAD_REQUESTED` | Cấu hình MinIO webhook cho GET event là phần hạ tầng phụ, không ảnh hưởng learning value cốt lõi (event-driven upload pipeline mới là phần chính). Có thể bật lại khi đã chạy ổn phần lõi. |
| Zip bomb / advanced malware sandboxing | **Không làm** | Ngoài phạm vi đồ án; ClamAV signature-based là đủ để demo virus-scan gate. |
| Email provider production-grade (SES/SendGrid) | **Giữ MailHog/Mailpit cho local, không cần cấu hình provider thật** | Forgot-password vẫn cần để demo, nhưng không cần production email thật cho 1 dự án CV. |

## 3. MVP giữ lại (đây là phần "phải làm" — giá trị CV cao nhất)
- **Microservices thật**: mỗi service 1 database riêng, giao tiếp REST nội bộ (khi cần đồng bộ) + Kafka (khi async), qua API Gateway.
- **JWT auth** đầy đủ: access/refresh, revoke từng session, logout-all, quên/đổi mật khẩu.
- **Presigned direct upload/download** với MinIO — cả **single-file** và **multipart resumable**.
- **Security gate bằng virus scan bất đồng bộ** (ClamAV) — điểm nhấn kiến trúc quan trọng nhất, giữ nguyên 100%.
- **Checksum verification hợp nhất vào virus scan pass** (fix vấn đề #1 của v1.0 — xem file 02-architecture).
- **Quota + Trash** (giữ, đơn giản nhưng đủ để demo transaction/atomic accounting).
- **ShareLink** PUBLIC/PRIVATE, VIEW/EDIT, kế thừa theo ancestor gần nhất (giữ, đây là phần logic hay, không quá phức tạp nếu bỏ merge).
- **Kafka event-driven pipeline** + Outbox pattern — điểm nhấn thứ hai, giữ nguyên.
- **ZIP folder bất đồng bộ** — vẫn giữ vì demo tốt async job pattern, không quá tốn công so với giá trị.

## 4. Stretch goals (làm sau nếu còn thời gian, không bắt buộc để "xong" đồ án)
1. Bật lại `FILE_DOWNLOAD_CONFIRMED` qua MinIO bucket notification thật.
2. Recursive folder merge đầy đủ (BR-030 gốc).
3. RBAC admin đầy đủ, dashboard UI riêng.
4. Schema Registry (Avro/Protobuf) thay JSON envelope.
5. Kubernetes deployment thay vì Docker Compose.
6. Rate limit chi tiết hơn cho upload/download (hiện MVP không giới hạn theo yêu cầu gốc).

## 5. Công nghệ mới cần học thêm khi chuyển sang microservices
- **Spring Cloud Gateway** (routing, JWT filter tại edge).
- **Service-to-service REST nội bộ** có xác thực riêng (shared secret hoặc mTLS đơn giản cho đồ án).
- **Database-per-service**: mỗi service 1 schema/Postgres instance riêng — không JOIN chéo service.
- **Saga / eventual consistency**: ví dụ đăng ký user ở `identity-service` rồi tạo root folder ở `filesystem-service` qua Kafka event, không phải 1 transaction.
- **Docker Compose multi-service** để chạy toàn bộ hệ thống cục bộ.
