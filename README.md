# File Storage System

Microservices-based file storage MVP based on the design documents in `docs/`.

## Services
- `api-gateway`: public entry point and route guard for `/api/v1/**`.
- `identity-service`: register/login/profile/session API scaffold.
- `filesystem-service`: source of truth for folders, files, quota and internal file state transitions.
- `upload-service`: single/multipart upload orchestration scaffold.
- `download-service`: file download URL and ZIP job scaffold.
- `virus-scan-service`: async scan/checksum scaffold.
- `notification-service`: notification storage/read scaffold.
- `audit-analytics-service`: admin metrics read scaffold.
- `common`: shared DTOs, headers and event envelope.

## Local Build

The project is Gradle-based and targets Java 21:

```powershell
gradle clean build
```

This machine currently does not have `gradle`/`mvn` in `PATH`; install Gradle or add a Gradle Wrapper before running the command locally.

## Local Run

After building the service jars:

```powershell
docker compose up --build
```

The gateway listens on `http://localhost:8080`.

## Progress

Implementation progress is tracked in [docs/09-implementation-plan.md](docs/09-implementation-plan.md).
