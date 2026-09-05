# File Storage Service — 09. Implementation Plan & Progress

> Mục tiêu: triển khai MVP microservices theo bộ tài liệu `docs/00` đến `docs/08`, ưu tiên nền tảng chạy được và các ranh giới kiến trúc quan trọng trước khi đi sâu vào từng edge case.

## 1. Nguyên tắc triển khai
- Bám đúng ranh giới v2.0: `filesystem-service` là nguồn sự thật duy nhất cho File/Folder/Quota/ShareLink.
- Mỗi service là một module Spring Boot độc lập, cấu hình DB/schema riêng, không query chéo database.
- Giao tiếp public đi qua `api-gateway`; endpoint `/internal/**` chỉ dành cho service-to-service với `X-Internal-Token`.
- Event dùng JSON envelope chung để có thể nối Kafka/outbox dần mà không đổi hợp đồng nghiệp vụ.
- MVP ưu tiên single-file upload, quota reservation, trạng thái `PENDING_SCAN -> ACTIVE`, download presigned URL, root provisioning async và observability tối thiểu.

## 2. Phạm vi MVP theo đợt

### Phase 0 — Khởi tạo nền dự án
- [x] Đọc tài liệu thiết kế trong `docs`.
- [x] Tạo kế hoạch triển khai và file theo dõi tiến độ.
- [x] Tạo cấu trúc multi-module Spring Boot/Gradle cho 8 service và `common`.
- [x] Thêm Docker Compose hạ tầng/service skeleton.

### Phase 1 — Shared contracts và gateway
- [x] Tạo event envelope, error model, header constants và DTO dùng chung.
- [x] Tạo `api-gateway` route theo path `/api/v1/**`.
- [x] Chặn `/internal/**` ở gateway và forward correlation/user headers.

### Phase 2 — Identity service
- [x] Tạo API đăng ký/login/refresh/logout/me/session/reset-password dạng MVP.
- [x] Thêm model user/session/token-version theo tài liệu.
- [x] Phát event `USER_REGISTERED` qua publisher abstraction.
- [x] Hoàn thiện JWT RS256 thật, refresh token hash và revoke session bền vững trong DB.
- [ ] Thêm Flyway migration chi tiết cho `identity_db`.

### Phase 3 — Filesystem service
- [x] Tạo API root folder, folder children, create folder, quota.
- [x] Tạo internal API `POST /internal/files`, `PATCH /internal/files/{id}/size`, `/activate`, `/reject`, access-check.
- [x] Implement quota reservation/release atomic ở service layer MVP.
- [x] Implement auto-suffix khi trùng tên.
- [x] Thêm Trash/restore/purge MVP cho file/folder, gồm soft-delete, list trash, restore và purge giải phóng quota.
- [ ] Thêm ShareLink PUBLIC/PRIVATE với ancestor inheritance và Redis lockout.
- [ ] Thêm Flyway migration chi tiết cho `filesystem_db`.

### Phase 4 — Upload service
- [x] Tạo `POST /uploads` tách SINGLE/MULTIPART theo threshold.
- [x] Tạo `POST /uploads/{fileId}/confirm` cho single-file, gọi filesystem internal client.
- [x] Tạo DTO/session model cho multipart scaffold.
- [x] Nối MinIO SDK thật cho presigned PUT, statObject, multipart compose-object và abort TTL.
- [x] Publish `FILE_UPLOAD_STORED` vào Kafka sau confirm/complete.

### Phase 5 — Download service
- [x] Tạo API presigned download URL scaffold.
- [x] Tạo ZIP job API scaffold.
- [x] Nối MinIO SDK thật cho presigned GET và ZIP streaming.
- [x] Publish `FILE_DOWNLOAD_REQUESTED`, `ZIP_REQUESTED`, `ZIP_READY`.

### Phase 6 — Async services
- [x] Tạo `virus-scan-service` consumer/service scaffold, checksum + scan abstraction.
- [x] Tạo `notification-service` consumer/storage scaffold.
- [x] Tạo `audit-analytics-service` metrics endpoint scaffold.
- [x] Nối Kafka consumer/producer thật, retry/DLQ và Redis dedupe.
- [x] Nối ClamAV INSTREAM thật.
- [ ] Thêm outbox relay cho `identity-service` và `filesystem-service`.

### Phase 7 — Kiểm thử và hoàn thiện
- [x] Rà soát tĩnh cấu trúc file và các placeholder/TODO lộ thiên.
- [ ] Thêm unit/integration test theo service.
- [ ] Thêm WireMock cho internal client tests.
- [ ] Chạy build/test toàn repo bằng Gradle.
- [ ] Chạy Docker Compose end-to-end.

### Phase 8 — Frontend MVP
- [x] Tạo SPA tĩnh trong `frontend/` với login/register, file browser, create folder, upload single/multipart, download, ZIP folder, Trash, notifications và admin metrics.
- [x] Thêm frontend dev server Node không cần dependency ngoài.
- [x] Cấu hình CORS gateway cho frontend `localhost:5173`.
- [ ] Kiểm thử end-to-end với Docker Compose đang chạy đủ hạ tầng/service.

## 3. Trạng thái hiện tại
- Ngày cập nhật: 2026-09-05.
- Trạng thái: đã dựng nền ứng dụng, các tích hợp backend chính của MVP và frontend SPA đầu tiên. Backend đã có JWT RS256/session refresh bền vững, Trash/restore/purge, MinIO upload/download/ZIP, Kafka events với retry/DLQ/Redis dedupe, và ClamAV INSTREAM. Flyway chi tiết, outbox relay và E2E Docker Compose còn ở backlog.
- Ghi chú môi trường: Gradle Wrapper đã chạy được sau khi cho phép tải Gradle distribution; dự án vẫn target Java 21 theo tài liệu.
- Kiểm tra đã chạy: `rg --files`, `rg "dev-|placeholder|TODO|FIXME|ddl-auto"`, `git status --short`, `./gradlew.bat :filesystem-service:compileJava`, `./gradlew.bat --no-daemon compileJava`, `docker-compose config`, `node --check frontend/app.js`, `node --check frontend/server.mjs`, HTTP 200 cho `http://localhost:5173`.
