# File Storage Service — 06. Tech Stack (v2.0 — Microservices)
> Thay thế cả `07-tech-stack-ver1.0.md` lẫn bản rút gọn `04-tech-stack-ver2_0.md` trước đó — đây là bản đầy đủ, cùng độ chi tiết với NFR/Decisions/Risks.

## 1. Backend — mỗi service
Java 21 LTS + Spring Boot 3.x, Spring Web MVC, Spring Security, Spring Validation cho tất cả 8 service. Lý do giữ nguyên như v1.0: constraint dự án, hệ sinh thái JWT/scheduling/Kafka mature. Mỗi service là 1 module Gradle/Maven **độc lập, build/deploy riêng** (khác v1.0 nơi mọi module nằm chung 1 deployable).

Persistence: Spring Data JPA cho CRUD đơn giản (identity, upload, download, notification, audit-analytics); riêng `filesystem-service` cần thêm JDBC/native query cho recursive ancestor traversal (đi ngược `parent_id`) và `SKIP LOCKED` nếu có scheduled job — lý do tương tự v1.0 (JPA thuần dễ N+1 với thao tác cây).

## 2. API Gateway
**Spring Cloud Gateway** (reactive, WebFlux-based) — được chọn vì tích hợp tốt với Spring Security cho JWT filter và có `RequestRateLimiter` dùng Redis sẵn có. Phương án khác đã xét: Nginx/Kong/Traefik (loại vì học Spring Cloud Gateway phù hợp hơn cho CV Java/Spring; Nginx thuần không có filter JWT built-in dễ dùng bằng Spring). Kong bị loại vì thêm 1 hệ sinh thái Lua/plugin ngoài Java, không cần thiết cho quy mô đồ án.

## 3. Service discovery
**Static config qua Docker Compose network** (hostname = tên service, ví dụ `filesystem-service:8080`) — đủ cho local dev và demo. **Eureka/Consul là stretch goal**, chỉ thêm nếu muốn CV có thêm dòng "dùng Spring Cloud Netflix Eureka cho service discovery động"; không bắt buộc vì quy mô 8 service cố định không cần discovery động thật sự.

## 4. Giao tiếp đồng bộ (REST nội bộ)
`RestClient` (Spring 6.1+, thay cho `RestTemplate` cũ) hoặc `WebClient` nếu cần non-blocking — MVP dùng `RestClient` cho đơn giản vì các service là blocking MVC thông thường. Timeout ngắn (2-3s) + `Spring Retry` cho các cuộc gọi `upload-service`/`download-service`/`virus-scan-service` → `filesystem-service`. Không dùng OpenFeign để tránh thêm abstraction không cần thiết cho quy mô 8 service.

## 5. Giao tiếp bất đồng bộ
**Apache Kafka** (giữ nguyên constraint) qua Spring for Apache Kafka — producer/consumer, consumer group riêng theo service, retry + DLQ qua `DefaultErrorHandler`. JSON envelope + `schemaVersion`, chưa cần Schema Registry (Avro là stretch nếu số service/team tăng — hiện 8 service cố định, JSON đủ dùng và dễ debug hơn khi học).

**Transactional Outbox**: `filesystem-service` và `identity-service` (2 service publish event quan trọng cần đảm bảo không mất) dùng bảng `outbox_event` + Spring `@Scheduled` relay job đọc `FOR UPDATE SKIP LOCKED`. Các service chỉ consume (virus-scan, notification, audit-analytics) không cần outbox.

## 6. Transactional database
**PostgreSQL 16+**, mỗi service 1 schema/database riêng (`identity_db`, `filesystem_db`, `upload_db`, `download_db`, `notification_db`, `audit_analytics_db`) — có thể chạy trên **1 Postgres container duy nhất với nhiều database** cho đồ án (đơn giản vận hành) miễn là không có service nào query cross-database; đây là điểm cần tự kỷ luật khi code vì Postgres không chặn được việc lỡ tay JOIN nếu vô tình cùng connection pool trỏ sai chỗ.

Schema migration: **Flyway**, mỗi service có thư mục migration riêng, versioned độc lập với service khác — 1 service update schema không ảnh hưởng service kia (điểm khác biệt quan trọng so với 1 schema chung của monolith).

## 7. Object storage
**MinIO** (constraint), dùng bởi 3 service: `upload-service` (PUT/multipart), `download-service` (GET/Range/ZIP), `virus-scan-service` (đọc object để scan). Access key/secret riêng cho từng service nếu MinIO hỗ trợ policy theo bucket/prefix (least privilege giữa các service, không chỉ giữa user/backend).

## 8. Virus scanning & file processing
**ClamAV** (daemon/container), `virus-scan-service` gọi qua TCP `INSTREAM` protocol (clamd), không cần library file-processing riêng (Tika/ImageMagick/FFmpeg) ở MVP vì đã cắt enrichment (thumbnail/preview) — xem file `00-scope-mvp-vs-stretch`, mục Stretch có thể bổ sung sau nếu muốn thêm 1 service `file-processing-service` riêng.

## 9. Redis
**Redis 7+** dùng cho: (a) `api-gateway` rate limit auth endpoint, (b) `filesystem-service` đếm sai password ShareLink `(shareId, IP+UA hash)`, (c) `virus-scan-service` dedupe `processed_event:{eventId}`. Một Redis instance dùng chung, phân biệt bằng key prefix theo service — không phải business source of truth ở service nào.

## 10. Authentication/cryptography
Spring Security filter chain ở `api-gateway` (verify JWT) và `identity-service` (issue JWT). JWT RS256 ưu tiên (asymmetric) để các service chỉ cần public key verify mà không giữ private key — quan trọng hơn ở kiến trúc microservices so với monolith, vì nhiều service tiềm năng cần verify token độc lập. Argon2id/BCrypt cho password; `SecureRandom` cho refresh/share/reset token.

## 11. Email
FR-002b (quên mật khẩu) qua `MailGateway` (Spring Mail abstraction) trong `identity-service`; **MailHog/Mailpit** cho local dev — không cần cấu hình provider production thật cho đồ án (xem `00-scope-mvp-vs-stretch`).

## 12. Observability
Spring Boot Actuator + Micrometer mỗi service; Prometheus scrape tất cả endpoint `/actuator/prometheus` — **stretch**, không bắt buộc để chạy demo. Structured JSON log (Logback), `correlationId` forward qua mọi service. OpenTelemetry cho distributed tracing là stretch rõ ràng có giá trị cao nếu có thời gian (giúp thấy trực quan 1 request đi qua bao nhiêu service) nhưng không bắt buộc để "chạy được".

## 13. Testing
JUnit 5 + Spring Boot Test cho từng service riêng biệt (test 1 service không cần dựng cả 8 service kia — điểm mạnh của kiến trúc này). **Testcontainers** cho Postgres/Kafka/MinIO/Redis khi test tích hợp. **WireMock** để giả lập response từ service khác khi test `upload-service` mà không cần chạy thật `filesystem-service` — đây là kỹ thuật quan trọng cần học khi test microservices (contract testing/mocking dependency service), khác hẳn cách test 1 monolith.

## 14. Deployment
Docker/OCI container cho từng service (Dockerfile riêng, viết ở giai đoạn cuối theo kế hoạch đã thống nhất). Docker Compose cho dev/demo (1 file infra trước, 1 file đầy đủ sau khi có Dockerfile từng service). Không mặc định Kubernetes.

**Nginx** làm reverse proxy/load balancer riêng cho `filesystem-service` (ADR-116) — chạy 3 replica bằng `docker compose up --scale filesystem-service=3`, Nginx round-robin phía trước. Các service khác gọi qua hostname Nginx thay vì gọi thẳng 1 instance cụ thể. Đây là load balancer duy nhất cần thiết ở MVP; các service còn lại chạy 1 instance là đủ.

## 15. Bảng so sánh nhanh: cái gì đổi so với v1.0
| Hạng mục | v1.0 (monolith) | v2.0 (microservices) |
|---|---|---|
| Deploy unit | 1 Spring Boot app + N Kafka worker | 8 Spring Boot app độc lập |
| Database | 1 PostgreSQL, nhiều bảng chung schema | Nhiều database/schema, 1-per-service |
| Gọi giữa module | Java method call trong cùng process | REST nội bộ (đồng bộ) hoặc Kafka (bất đồng bộ) |
| Transaction xuyên nghiệp vụ | 1 DB transaction | Không có; xử lý bằng API gộp (reserve+create) hoặc saga/eventual consistency |
| Entry point | 1 port | `api-gateway` route tới nhiều port/service |

## 16. Truy vết
Giữ nguyên toàn bộ lựa chọn công nghệ lõi đã có lý do tốt ở v1.0 (Postgres/MinIO/Kafka/Redis/ClamAV/Spring Boot); bổ sung Spring Cloud Gateway, static service discovery, RestClient+Retry cho giao tiếp nội bộ, và WireMock cho testing — đây là những công nghệ mới phát sinh trực tiếp từ việc chuyển sang microservices.
