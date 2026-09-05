# File Storage Service — 02. Services & Data Model theo Database-per-Service (v2.0)

## 1. `identity-service` — DB: `identity_db`
- `users(id uuid PK, email varchar UNIQUE, password_hash, role varchar CHECK(USER,ADMIN), token_version int DEFAULT 0, display_name, created_at, updated_at)`
- `login_sessions(id uuid PK, user_id FK, refresh_token_hash, device_info, ip_address, created_at, last_used_at, expires_at, revoked_at)`
- `password_reset_tokens(id uuid PK, user_id FK, token_hash, expires_at, used_at, created_at)`
- `outbox_event` (chuẩn, xem mục 8) — dùng để publish `USER_REGISTERED`.

Vai trò `role` giữ USER/ADMIN nhưng ADMIN ở MVP chỉ cần seed sẵn 1 tài khoản qua migration, không có luồng cấp quyền admin qua API.

## 2. `filesystem-service` — DB: `filesystem_db` (service "nặng" nhất, là nguồn sự thật cho cây file)
- `folders(id uuid PK, owner_id uuid, name, parent_id FK NULL, created_at, updated_at)`. Unique partial index `(owner_id) WHERE parent_id IS NULL` (1 root/user). Unique `(owner_id, parent_id, name)` — trùng tên tự động hậu tố (đã bỏ merge, xem file 00-scope).
- `files(id uuid PK, owner_id, name, size bigint, mime_type, storage_key UNIQUE, parent_folder_id FK, checksum varchar NULL, checksum_algorithm DEFAULT 'SHA-256', status CHECK(PENDING_SCAN,ACTIVE,INFECTED,DELETING), created_at, updated_at)`. `checksum` nullable vì chỉ được điền sau khi `virus-scan-service` gọi `/activate` — trước đó là NULL, không phải giá trị giả.
- `trash_items(id uuid PK, resource_id, resource_type CHECK(FILE,FOLDER), original_parent_id NULL, trashed_at, size)`
- `storage_quota(user_id uuid PK, quota bigint DEFAULT 10737418240, current_usage bigint, updated_at)`
- `share_links(id uuid PK, resource_id, resource_type CHECK(FILE,FOLDER), token UNIQUE, access_mode CHECK(PUBLIC,PRIVATE), permission CHECK(VIEW,EDIT), password_hash NULL, expires_at NULL, created_by, created_at, revoked_at NULL, download_limit NULL, access_count DEFAULT 0)`. CHECK: PRIVATE bắt buộc có `password_hash`.
- `outbox_event` — publish `FILE_RENAMED`, `FILE_MOVED`, `FILE_DELETED`, `FILE_SHARED`, `FILE_UPLOAD_COMPLETED`, `FILE_INFECTED`.

### API nội bộ quan trọng (chỉ gọi từ service khác, không lộ ra gateway)
- `POST /internal/files` — tạo File PENDING_SCAN + reserve quota atomic (dùng bởi `upload-service`).
- `PATCH /internal/files/{id}/size` — cập nhật size thật sau khi đối soát MinIO (dùng bởi `upload-service`).
- `POST /internal/files/{id}/activate` — chuyển ACTIVE + lưu checksum (dùng bởi `virus-scan-service`).
- `POST /internal/files/{id}/reject` — xóa file + release quota trong 1 transaction (dùng bởi `virus-scan-service`).
- `GET /internal/files/{id}/access-check?userId=&shareToken=` — trả về có được phép download/sửa không (dùng bởi `download-service`).
- `POST /internal/quota/release` — dùng khi `upload-service` phát hiện size-mismatch hoặc abort upload TTL.

## 3. `upload-service` — DB: `upload_db`
- `upload_sessions(id uuid PK, file_id uuid, minio_upload_id varchar NULL, mode CHECK(SINGLE,MULTIPART), uploaded_parts jsonb NULL, status CHECK(IN_PROGRESS,COMPLETED,ABORTED), reserved_size bigint, part_size bigint NULL, last_activity_at, created_at)`. `file_id` chỉ là tham chiếu logic tới `filesystem-service`, không FK thật (khác database).

Không có bảng `File`/`Folder` — service này **không sở hữu** dữ liệu file, chỉ điều phối quá trình upload rồi giao lại cho `filesystem-service`.

## 4. `download-service` — DB: `download_db`
- `zip_jobs(id uuid PK, folder_id uuid, requested_by uuid, status CHECK(PENDING,PROCESSING,READY,FAILED), result_storage_key NULL, error_message NULL, expires_at NULL, created_at, updated_at)`

## 5. `virus-scan-service` — không cần Postgres riêng
- Dùng Redis (dùng chung instance với `api-gateway`/`filesystem-service` cho rate limit, khác keyspace) để lưu `processed_event:{eventId}` TTL vài ngày, phục vụ dedupe khi Kafka redeliver — đủ cho MVP, không cần bảng quan hệ riêng.

## 6. `notification-service` — DB: `notification_db`
- `notifications(id uuid PK, user_id, type varchar, payload jsonb, created_at, read_at NULL)`

## 7. `audit-analytics-service` — DB: `audit_analytics_db`
- `audit_records(id uuid PK, event_id UNIQUE, action varchar, actor_id NULL, resource_type NULL, resource_id NULL, correlation_id NULL, details jsonb NULL, created_at)`
- `analytics_metrics(metric_type varchar, period varchar, dimension_key varchar DEFAULT 'ALL', value numeric, updated_at, PRIMARY KEY(metric_type, period, dimension_key))`

Endpoint đọc duy nhất: `GET /admin/metrics?metricType=&period=` — bảo vệ bằng role `ADMIN` lấy từ JWT claim do `identity-service` cấp (gateway forward claim qua header, service tự kiểm tra, không cần gọi ngược `identity-service`).

## 8. Bảng kỹ thuật dùng chung ở các service có publish event
```
outbox_event(id uuid PK, event_type, aggregate_type, aggregate_id, payload jsonb, occurred_at, published_at NULL, retry_count)
```
Mỗi service tự có bảng này trong DB riêng của mình nếu service đó publish event quan trọng cần đảm bảo không mất (chủ yếu là `filesystem-service`; `identity-service` cho `USER_REGISTERED`). Các service chỉ consume (virus-scan, notification, audit-analytics) không cần outbox.

## 9. Vì sao không dùng chung 1 database
Đây là điểm học thuật quan trọng nhất của bản v2.0: nếu 8 service dùng chung 1 Postgres, đó vẫn là monolith về mặt dữ liệu dù có nhiều process — mất hết lợi ích cô lập lỗi/schema evolution độc lập của microservices. Cái giá phải trả (và cũng là bài học) là không thể `JOIN` xuyên service — mọi truy vấn cần dữ liệu từ 2 service phải qua REST nội bộ hoặc tổng hợp sẵn qua Kafka event (như cách `audit-analytics-service` tổng hợp `AnalyticsMetric` mà không cần hỏi ngược `filesystem-service`).

## 10. Truy vết
Bảng ở mục 2 giữ đầy đủ entity gốc (File/Folder/ShareLink/TrashItem/StorageQuota) nhưng gom về 1 service duy nhất sở hữu — đúng nguyên tắc "1 bounded context, 1 chủ sở hữu dữ liệu". Mục 2's API nội bộ trực tiếp fix 3 vấn đề đã nêu trước đó (activate mang checksum thật, reject giải phóng quota atomic, PATCH size dùng số liệu MinIO thật).
