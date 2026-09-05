# File Storage Service — 07. Architecture Decisions (v2.0 — Microservices)
> Thay thế `08-decisions-ver1.0.md`. Giữ các ADR còn nguyên giá trị (đánh dấu "kế thừa"), thêm ADR mới cho quyết định chuyển sang microservices và các fix đã thống nhất.

## ADR-101: Chuyển từ modular monolith sang microservices (MỚI — thay thế ADR-001)
- Bối cảnh: v1.0 chọn modular monolith vì requirements không có SLA/quy mô định lượng. Sau khi requirements ổn định, mục tiêu dự án chuyển trọng tâm sang **học và thể hiện kỹ năng microservices cho CV**.
- Phương án đã xét: giữ modular monolith; microservices đầy đủ (database-per-service); microservices nhưng dùng chung 1 database.
- Quyết định: 8 service độc lập, mỗi service 1 database riêng, giao tiếp REST nội bộ (đồng bộ khi cần nhất quán mạnh) + Kafka (bất đồng bộ).
- Lý do: database-per-service là bài học microservices quan trọng nhất, dùng chung DB sẽ chỉ là "monolith nhiều process" và mất giá trị học tập/CV.
- Đánh đổi/hệ quả: mất transaction ACID xuyên service, phải xử lý eventual consistency (đăng ký user → root folder) và gộp API để giữ nhất quán mạnh ở chỗ cần thiết (reserve quota + tạo File). Độ phức tạp vận hành tăng (8 deployable thay vì 1), chấp nhận đánh đổi vì đây chính là mục tiêu học tập.

## ADR-102: `filesystem-service` là chủ sở hữu duy nhất của File/Folder/Quota/Share (MỚI)
- Bối cảnh: File/Folder/ShareLink/StorageQuota liên quan chặt chẽ tới nhau (quota tính theo file, share áp dụng theo cây folder) — tách rời sẽ tạo quá nhiều lời gọi cross-service cho 1 thao tác đơn giản.
- Phương án đã xét: tách Share thành service riêng; tách Quota thành service riêng; gom tất cả vào 1 service.
- Quyết định: gom File, Folder, Trash, StorageQuota, ShareLink vào 1 service (`filesystem-service`) duy nhất.
- Lý do: các entity này cùng 1 bounded context thực sự ("quản lý cây tài nguyên của user và ai được truy cập nó") — tách nhỏ hơn sẽ tạo distributed transaction giả tạo (ví dụ share 1 folder cần biết cây folder, quota cần biết tổng file) mà không có lợi ích cô lập thật sự.
- Đánh đổi/hệ quả: `filesystem-service` trở thành single point of failure về mặt logic (mọi service khác phụ thuộc nó) — chấp nhận vì đây là service ít thay đổi/ít có tải nặng bất thường nhất (không phải nơi chạy CPU-heavy như virus scan hay archive).

## ADR-103: Tách luồng Single-file Upload khỏi Multipart Upload (MỚI)
- Bối cảnh: v1.0 gộp chung 1 luồng "upload" dùng multipart cho mọi kích thước, gây phức tạp không cần thiết cho file nhỏ và mơ hồ về việc "complete" nghĩa là gì.
- Phương án đã xét: luôn dùng multipart (kể cả file nhỏ); tách 2 luồng theo `multipart_threshold`.
- Quyết định: file < threshold dùng 1 presigned PUT + endpoint `/confirm` (đối soát ngay bằng `statObject`); file ≥ threshold dùng multipart đầy đủ.
- Lý do: đơn giản hóa code path cho phần lớn file thực tế (đa số file người dùng nhỏ hơn 100MB); dễ demo sự khác biệt kiến trúc giữa 2 luồng khi phỏng vấn.
- Đánh đổi/hệ quả: 2 code path cần maintain thay vì 1, nhưng mỗi path đơn giản hơn nhiều so với 1 path dùng chung phức tạp.

## ADR-104: Checksum được tính bởi Virus Scan Service, không phải tại thời điểm complete (MỚI — fix vấn đề #1 đã phát hiện ở v1.0)
- Bối cảnh: v1.0 yêu cầu verify checksum tại `/complete` nhưng mâu thuẫn với nguyên tắc "backend không đọc bandwidth file lớn" vì binary không đi qua backend.
- Phương án đã xét: đọc lại object tại `upload-service` để hash (tốn thêm 1 lượt đọc); tin checksum client tự khai (không verify thật); tính checksum trong lúc virus scan (đã đọc object sẵn).
- Quyết định: `virus-scan-service` tính SHA-256 trên cùng stream đưa vào ClamAV, ghi kết quả qua API `/activate`.
- Lý do: tận dụng 1 lượt đọc bắt buộc phải có (cho virus scan) thay vì tạo thêm 1 lượt đọc riêng chỉ để hash — giải quyết mâu thuẫn mà không tốn thêm chi phí I/O.
- Đánh đổi/hệ quả: `File.checksum` chỉ có giá trị sau khi scan xong (không có ngay lúc complete) — API cần phản ánh đúng: `checksum` nullable cho tới khi ACTIVE.

## ADR-105: Gộp release-quota vào cùng API reject khi nhiễm virus (MỚI — fix vấn đề #2)
- Bối cảnh: v1.0 chỉ nói "xóa file khi nhiễm virus" mà không có bước rõ ràng trả quota, dẫn tới rò rỉ quota.
- Phương án đã xét: 2 API riêng (xóa file, trả quota) do `virus-scan-service` gọi tuần tự; 1 API gộp cả 2 xử lý atomic trong `filesystem-service`.
- Quyết định: 1 API `POST /internal/files/{id}/reject` xử lý xóa file + trả quota trong cùng 1 transaction local của `filesystem-service`.
- Lý do: loại bỏ khả năng quên 1 trong 2 bước — atomic hóa ở phía sở hữu dữ liệu (nơi duy nhất có thể transaction thật) thay vì trông chờ caller gọi đủ 2 API.
- Đánh đổi/hệ quả: API nội bộ có phần "dày" hơn (làm 2 việc cùng lúc), nhưng an toàn hơn nhiều so với việc để caller tự orchestrate 2 lời gọi riêng lẻ.

## ADR-106: Size thật lấy từ MinIO, không tin số client khai (MỚI — fix vấn đề #3)
- Bối cảnh: client tự khai `size` lúc khởi tạo upload chỉ dùng để ước lượng reserve quota ban đầu; không có gì đối chiếu với thực tế.
- Phương án đã xét: tin số client khai vĩnh viễn; đối soát bằng MinIO event bất đồng bộ (có độ trễ); đối soát đồng bộ ngay tại bước confirm/complete bằng `statObject`/`ListParts`.
- Quyết định: `upload-service` gọi MinIO lấy size thật ngay tại bước confirm (single)/complete (multipart), ghi đè `File.size`, từ chối và rollback nếu vượt quota đã reserve.
- Lý do: phát hiện gian lận/lỗi sớm nhất có thể (trước khi vào hàng chờ virus scan), không phải đợi đến bước xử lý bất đồng bộ sau này mới biết.
- Đánh đổi/hệ quả: thêm 1 lời gọi MinIO API tại bước confirm/complete (chi phí thấp, chỉ là metadata call, không phải đọc bytes).

## ADR-107: Bỏ recursive folder merge, dùng auto-suffix cho cả File và Folder (MỚI — trim scope)
- Bối cảnh: BR-030 (merge đệ quy khi trùng tên folder) phức tạp, nhiều edge case (ShareLink trên source, cascade nhiều cấp).
- Phương án đã xét: giữ nguyên merge đầy đủ; bỏ hẳn, dùng chung rule tự động hậu tố tên như File.
- Quyết định: bỏ merge ở MVP, đưa xuống Stretch; File và Folder trùng tên đều tự động hậu tố `(n)`.
- Lý do: giảm đáng kể độ phức tạp implement mà không mất tính năng cốt lõi (user vẫn upload/restore/move được, chỉ khác là không tự động gộp nội dung).
- Đánh đổi/hệ quả: trải nghiệm kém "thông minh" hơn khi thực sự có 2 folder cùng tên cần gộp — chấp nhận vì đây không phải trọng tâm học thuật của dự án.

## ADR-108 đến ADR-113: Kế thừa nguyên vẹn từ v1.0 (không đổi bản chất quyết định, chỉ đổi ai là actor thực thi)
- **ADR-108** (= ADR-004 cũ): Multipart làm cơ chế resumable upload — giữ nguyên, nay do `upload-service` thực thi.
- **ADR-109** (= ADR-006 cũ): Transactional Outbox + at-least-once consumer — giữ nguyên, nay mỗi service publish event quan trọng (`filesystem-service`, `identity-service`) tự có outbox riêng thay vì 1 outbox chung.
- **ADR-110** (= ADR-008 cũ): Redis chỉ cho ephemeral security counters — giữ nguyên, dùng chung 1 Redis instance nhưng phân biệt namespace theo service.
- **ADR-111** (= ADR-009 cũ): PostgreSQL search trước, không Elasticsearch baseline — giữ nguyên, nay thuộc phạm vi `filesystem-service`.
- **ADR-112** (= ADR-010 cũ): Streaming asynchronous ZIP — giữ nguyên, nay là 1 phần của `download-service` (gộp vai trò Archive Worker thay vì tách riêng, xem `00-scope-mvp-vs-stretch`).
- **ADR-113** (= ADR-012 cũ): ClamAV — giữ nguyên, nay chạy trong `virus-scan-service`; Tika/ImageMagick/FFmpeg bị cắt khỏi MVP (không có enrichment service riêng).

## ADR-114: API Gateway bằng Spring Cloud Gateway (MỚI)
- Bối cảnh: cần 1 entry point duy nhất route request tới 8 service, xác thực JWT tại edge.
- Phương án đã xét: Spring Cloud Gateway; Nginx/Kong/Traefik; không có gateway (client gọi thẳng từng service).
- Quyết định: Spring Cloud Gateway.
- Lý do: cùng hệ sinh thái Spring, filter JWT/rate-limit tích hợp sẵn, phù hợp CV Java/Spring hơn là học thêm Lua/Kong.
- Đánh đổi/hệ quả: thêm 1 điểm phải luôn chạy đúng (nếu gateway sai logic verify JWT, ảnh hưởng toàn hệ thống) — chấp nhận vì đơn giản hơn nhiều so với việc mỗi service tự verify JWT riêng lẻ.

## ADR-115: Static service discovery qua Docker Compose, không dùng Eureka/Consul ở MVP (MỚI)
- Bối cảnh: 8 service cố định, không co giãn động trong phạm vi đồ án.
- Phương án đã xét: Eureka; Consul; static hostname qua Docker network.
- Quyết định: static hostname (Docker Compose service name).
- Lý do: số lượng/danh tính service không đổi trong suốt vòng đời demo — service discovery động không mang lại lợi ích thực tế ở quy mô này, chỉ thêm phức tạp vận hành.
- Đánh đổi/hệ quả: nếu sau này scale nhiều instance cùng loại service thật, cần load balancer (Compose không tự làm việc này tốt) — ghi nhận là Stretch.

## 2. Truy vết
ADR-101/102 là nền tảng của toàn bộ chuyển đổi kiến trúc. ADR-103/104/105/106 fix trực tiếp 4 vấn đề đã phát hiện qua review (thêm luồng single-upload + 3 vấn đề checksum/quota/size). ADR-107 phản ánh quyết định trim scope. ADR-108–113 xác nhận các quyết định kỹ thuật tốt của v1.0 vẫn đúng, chỉ đổi actor thực thi. ADR-114/115 là quyết định hạ tầng mới thuần túy phát sinh từ microservices.
