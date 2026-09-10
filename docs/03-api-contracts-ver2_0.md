# File Storage Service — 03. API Contracts (v2.0 — Microservices)
> Base path qua `api-gateway`: `/api/v1`. Gateway route tiền tố theo service: `/api/v1/auth/**`, `/api/v1/sessions/**` → identity-service; `/api/v1/folders/**`, `/api/v1/files/**`, `/api/v1/trash/**`, `/api/v1/shares/**`, `/api/v1/quota` → filesystem-service; `/api/v1/uploads/**` → upload-service; `/api/v1/downloads/**`, `/api/v1/zip-jobs/**` → download-service; `/api/v1/admin/**` → audit-analytics-service.

## 1. Identity APIs (`identity-service`)
Giữ nguyên như v1.0: `POST /auth/register`, `POST /auth/login`, `POST /auth/refresh`, `POST /auth/logout`, `POST /auth/logout-all`, `POST /auth/forgot-password`, `POST /auth/reset-password`, `GET /me`, `PATCH /me`, `PUT /me/password`, `GET /sessions`, `DELETE /sessions/{id}`.

`POST /auth/register` response **không còn trả `rootFolderId` ngay lập tức** (khác v1.0) vì root folder được tạo bất đồng bộ bởi `filesystem-service` sau khi nhận `USER_REGISTERED`: `201 {userId, role:"USER"}`. Client nên gọi `GET /folders/root` sau đó; nếu chưa kịp provisioning, trả `404 ROOT_NOT_PROVISIONED_YET` và FE nên tự retry sau ~1 giây (thường xong trong vài trăm ms).

## 2. Filesystem APIs (`filesystem-service`) — giữ phần lớn v1.0, bỏ merge
- `GET /folders/root` → `200 {id, name, parentId:null}` hoặc `404 ROOT_NOT_PROVISIONED_YET`.
- `GET /folders/{id}/children?query=&sort=&direction=&cursor=&limit=`
- `POST /folders`: `{parentId, name}` → **201** nếu tên chưa trùng; nếu trùng → tự động hậu tố `" (n)"` và trả `201 {id, name: "Tên (1)", parentId}` — không còn `{merged:true}`/409 như bản merge cũ.
- `PATCH /files/{id}` / `PATCH /folders/{id}` rename: trùng tên tự hậu tố tương tự, không còn nhánh merge.
- `POST /resources/{type}/{id}/move`: `{targetFolderId}` → `200 {resourceId, targetFolderId, effectiveName}`. `400 MOVE_CYCLE` nếu di chuyển vào chính subtree của nó. Trùng tên ở đích: tự hậu tố, không hỏi confirm nữa (đã bỏ `confirmShareRevocation`/`MERGE_CONFIRMATION_REQUIRED`).
- `DELETE /resources/{type}/{id}` → 204 vào Trash (cascade). `POST /trash/{type}/{id}/restore` → tự hậu tố nếu trùng tên tại vị trí gốc. `DELETE /trash/{type}/{id}?permanent=true` → purge.
- `GET /trash`, `GET /quota` — như v1.0.

## 3. Share APIs (`filesystem-service`)
Giữ nguyên schema v1.0 (`POST /shares`, `PATCH /shares/{id}`, `DELETE /shares/{id}`, `GET /shares`, `GET /share/{token}`, `POST /share/{token}/unlock`). Chỉ khác: không còn rule "chặn tạo ShareLink chồng lấn" bắt buộc nữa nếu bạn muốn đơn giản hơn — **giữ nguyên rule đó** (409 `OVERLAPPING_SHARE`) vì nó không phụ thuộc merge, vẫn hợp lý độc lập.

## 4. Upload APIs (`upload-service`) — MỚI: tách rõ single-file khỏi multipart

### 4.1 Single-file upload (file < `multipart_threshold`, mặc định 100MB)
```
POST /uploads
Request: {parentFolderId: uuid, name: string, size: int64, mimeType: string}
Response 201: {fileId: uuid, mode: "SINGLE", uploadUrl: string, expiresAt: datetime}
Errors: 400 MIME_NOT_ALLOWED | 507 QUOTA_EXCEEDED
```
```
POST /uploads/{fileId}/confirm
Request: {} 
Response 202: {fileId, status: "PENDING_SCAN", size: int64}   // size là số THẬT lấy từ MinIO statObject
Errors: 404 OBJECT_NOT_FOUND_IN_STORAGE (client PUT thất bại/chưa xong)
        409 SIZE_MISMATCH (size thật > size đã reserve quota; file+reservation bị rollback tự động)
```

### 4.2 Multipart upload (file ≥ `multipart_threshold`, resumable)
```
POST /uploads
Request: {parentFolderId, name, size, mimeType}   // size chỉ là ước lượng ban đầu để reserve quota
Response 201: {fileId, mode: "MULTIPART", uploadSessionId, partSize: int64, totalParts: int,
               parts: [{partNumber, url, expiresAt}]}
```
```
GET /uploads/{uploadSessionId}                → {status, uploadedParts:[...], missingParts:[...]}
POST /uploads/{uploadSessionId}/parts/presign  → {partNumbers:[...]} => {parts:[...]}  (chỉ part thiếu)
POST /uploads/{uploadSessionId}/complete       → {parts:[{partNumber, etag}]}
                                                => 202 {fileId, status:"PENDING_SCAN", size: int64}
                                                   // size lấy từ ListParts thật, KHÔNG dùng ETag tổng làm checksum
Errors: 409 PARTS_MISSING | 409 SIZE_MISMATCH | 409 UPLOAD_ALREADY_FINALIZED
DELETE /uploads/{uploadSessionId}              → 204 abort, release quota, AbortMultipartUpload
```

**Lưu ý quan trọng khác biệt so với v1.0**: không còn field `checksum` trong request `POST /uploads` hay `.../complete`. Checksum không còn là input bắt buộc từ client — nó được `virus-scan-service` tính ra và ghi nhận sau (xem file 01-architecture mục 5.1). Client có thể vẫn gửi optional `clientChecksumHint` để hệ thống log cảnh báo lệch, nhưng đây không phải hợp đồng bắt buộc.

## 5. Download/Archive APIs (`download-service`)
- `GET /downloads/files/{id}/url` → `200 {url, expiresAt, supportsRange:true}`. Download Service gọi Filesystem Service để access-check và lấy `storageKey`, tên gốc, MIME type trước khi ký URL MinIO với `Content-Type` và `Content-Disposition`. `409 FILE_NOT_READY` nếu chưa ACTIVE.
- `POST /downloads/folders/{id}/zip-jobs` → `202 {jobId, status:"PENDING"}`
- `GET /zip-jobs/{id}` → `{id, status, resultExpiresAt?, errorMessage?}`
- `POST /zip-jobs/{id}/download-url` → `200 {url, expiresAt}` nếu READY

## 6. Admin/Analytics (`audit-analytics-service`)
`GET /admin/metrics?metricType=&period=day|month&from=&to=` → chỉ role ADMIN (kiểm tra claim JWT do gateway forward qua header `X-User-Role`).

## 7. Internal APIs (không qua gateway, chỉ service-to-service)
Xem đầy đủ ở file `02-services-and-data-model-ver2_0.md` mục 2. Tất cả yêu cầu header `X-Internal-Token: <shared-secret>`; gateway route reject mọi request public tới path `/internal/**`.

## 8. Kafka event contracts (không đổi nhiều, bỏ field checksum khỏi payload nơi không cần)
- `USER_REGISTERED {userId, email}` — identity-service → filesystem-service.
- `FILE_UPLOAD_STARTED {fileId, ownerId, size, mimeType}` — upload-service.
- `FILE_UPLOAD_STORED {fileId, ownerId, storageKey, size}` — upload-service (không còn `checksum` ở đây, vì chưa tính).
- `FILE_UPLOAD_COMPLETED {fileId, ownerId, size, mimeType, checksum}` — filesystem-service (checksum lần đầu xuất hiện ở event này, do virus-scan-service cung cấp).
- `FILE_INFECTED {fileId, ownerId}`, `FILE_DELETED {resourceId, resourceType}` — filesystem-service.
- `FILE_RENAMED {resourceId, resourceType, oldName, newName}`, `FILE_MOVED {resourceId, resourceType, fromParentId, toParentId}`, `FILE_SHARED {shareId, resourceId, resourceType, permission}` — filesystem-service.
- `FILE_DOWNLOAD_REQUESTED {fileId, ownerId, viaShare, shareId?}` — download-service. *(Stretch: `FILE_DOWNLOAD_CONFIRMED`)*
- `ZIP_REQUESTED / ZIP_READY / ZIP_FAILED` — download-service.

## 9. Truy vết
Mục 4 đáp ứng trực tiếp yêu cầu "thêm luồng single file upload" với hợp đồng API tách biệt rõ ràng khỏi multipart. Mục 4.1/4.2 phản ánh cách bỏ checksum khỏi input client (fix vấn đề #1) và đối soát size thật (fix vấn đề #3).
