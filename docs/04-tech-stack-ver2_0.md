# File Storage Service — 04. Tech Stack (v2.0 — Microservices)

## 1. Mỗi service
Java 21 + Spring Boot 3.x, mỗi service là 1 Maven/Gradle module + 1 Docker image riêng. Dùng Spring Web MVC cho REST. `filesystem-service`, `identity-service`, `upload-service`, `download-service`, `notification-service`, `audit-analytics-service` dùng Spring Data JPA/JDBC + PostgreSQL riêng từng service (có thể chạy 6+ Postgres container/schema riêng trên cùng 1 Postgres instance cho đồ án, miễn là schema tách biệt và không cross-schema query — mô phỏng database-per-service mà không cần 6 server Postgres thật).

## 2. API Gateway
**Spring Cloud Gateway** — routing theo path prefix, filter xác thực JWT tại edge (verify signature bằng public key/JWKS lấy từ `identity-service` lúc khởi động hoặc cấu hình tĩnh), forward claim `userId/role` qua header cho service phía sau. Rate limit auth endpoint bằng Redis + Spring Cloud Gateway RequestRateLimiter filter.

## 3. Service discovery (tùy chọn mức độ)
Cho đồ án quy mô nhỏ, dùng **static config** (Docker Compose service name làm hostname, ví dụ `http://filesystem-service:8080`) là đủ, không bắt buộc Eureka/Consul. Nếu muốn học thêm service discovery thật, có thể thêm **Spring Cloud Netflix Eureka** như stretch goal — không phải yêu cầu MVP.

## 4. Giao tiếp
- REST nội bộ: `RestClient`/`WebClient` (Spring 6) với timeout ngắn (2-3s) + retry đơn giản (Spring Retry) cho các cuộc gọi `upload-service`/`download-service`/`virus-scan-service` → `filesystem-service`.
- Kafka: Spring for Apache Kafka, JSON envelope + `schemaVersion` (không cần Schema Registry ở MVP).

## 5. Object storage & scanning
Giữ nguyên v1.0: MinIO (S3-compatible SDK, presigned PUT/GET, multipart), ClamAV (daemon container, `virus-scan-service` gọi qua TCP INSTREAM protocol).

## 6. Datastore khác
PostgreSQL 16+ (nhiều schema/database logic riêng theo service), Redis 7+ (rate limit + share password lockout + event dedupe cho virus-scan-service).

## 7. Observability tối thiểu cho đồ án
Spring Boot Actuator (`/actuator/health`, `/actuator/metrics`) mỗi service; Docker Compose healthcheck dựa vào endpoint này. Structured JSON log (Logback) có `correlationId` truyền qua header giữa các service (đơn giản hóa: tự sinh UUID ở gateway, forward qua toàn bộ chain gọi REST + Kafka payload). Prometheus/Grafana là stretch, không bắt buộc để demo local.

## 8. Local development
Docker Compose 1 file duy nhất khởi tất cả: 8 service Spring Boot + Postgres + Redis + Kafka (+ Zookeeper hoặc KRaft) + MinIO + ClamAV + MailHog. Đây tự nó là điểm hay để đưa vào CV: "thiết kế và vận hành hệ thống 8 microservices bằng Docker Compose, giao tiếp qua REST nội bộ và Kafka".

## 9. Vì sao giữ tối giản phần hạ tầng
Mục tiêu là học và thể hiện đúng **kỹ năng microservices cốt lõi** (bounded context, database-per-service, sync vs async communication, eventual consistency, event-driven pipeline, security gate bất đồng bộ) — không phải học vận hành Kubernetes/Service Mesh/Schema Registry, những thứ để ở Stretch trong file `00-scope-mvp-vs-stretch-ver2_0.md`.

## 10. Truy vết
File này thay thế `07-tech-stack-ver1.0.md`; giữ nguyên các lựa chọn công nghệ lõi (Postgres/MinIO/Kafka/Redis/ClamAV/Spring Boot) đã có lý do tốt ở v1.0, chỉ thêm Spring Cloud Gateway và điều chỉnh cách triển khai database cho phù hợp microservices.
