# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

STORYTELLER is a Spring Boot backend for a children's English storybook app. It uses OpenAI GPT to generate story content and DALL-E to generate illustrations, then stores them in AWS S3. Users have child profiles with PIN protection; each profile accumulates books with pages, unknown words, and reading settings.

## Build & Run Commands

```bash
# Build (skipping tests)
./gradlew clean build -x test

# Build with tests
./gradlew clean build

# Run (local profile)
./gradlew bootRun

# Run all tests
./gradlew test

# Run a single test class
./gradlew test --tests "com.cojac.storyteller.book.service.BookServiceTest"

# Run a single test method
./gradlew test --tests "com.cojac.storyteller.unit.book.BookServiceUnitTest.someMethodName"
```

The app requires `application-secret.yml` (not committed) in `src/main/resources/` with credentials for MySQL, Redis, AWS S3, OpenAI, SMTP, and OAuth2. In CI, this is base64-decoded from the `APPLICATION_SECRET` GitHub secret.

## Spring Profiles

| Profile | Port | Purpose |
|---------|------|---------|
| `local` | 8080 | Local development |
| `blue` | 8080 | Production (blue slot) |
| `green` | 8081 | Production (green slot) |

The active profile is `local` by default. Production uses blue-green deployment via Nginx on a single EC2 instance.

## Architecture

### Package Structure

All code lives under `com.cojac.storyteller`. Domains are separated into top-level packages:

- **`book/`** — Book entity, service, controller, batch delete, OpenAI integration
- **`page/`** — Page entity, service, batch insert
- **`profile/`** — Child profile with PIN auth and photo management
- **`user/`** — User accounts: local (username/password) and social (Kakao/Google OAuth2), JWT auth
- **`setting/`** — Per-profile reading settings (font size, reading speed)
- **`unknownWord/`** — Words bookmarked for learning within a book page
- **`common/`** — Shared infrastructure: S3, Redis, OpenAI, email, async, security, Swagger, CORS
- **`performance/`** — AOP-based DBCP/query monitoring (active only on `performance` profile)
- **`response/`** — Unified `ResponseDTO`/`ErrorResponseDTO` and `ResponseCode`/`ErrorCode` enums

Each domain follows: `controller → service → repository → entity`, with `dto/`, `exception/`, and sometimes `mapper/` sub-packages.

### Authentication Flow

- Stateless JWT. Access token in `Authorization` header; refresh token in Redis.
- Two user tables: `LocalUser` (username/password) and `SocialUser` (OAuth2).
- Custom `LoginFilter`, `JWTFilter`, and `LogoutFilter` added to the Spring Security filter chain.
- Public endpoints: `/login`, `/register`, `/reissue`, `/kakao-login`, `/google-login`, email verification, Swagger, health check, and Actuator endpoints.

### Book Creation Flow

`BookController → BookService.createBook()`:
1. Generates story text via `OpenAIService.generateStory()` (ChatGPT)
2. Generates cover image via `ImageGenerationService` (DALL-E) and uploads to S3
3. Publishes `UploadS3Event` after each S3 upload
4. Persists `BookEntity` via `BookRepository` and `PageEntity` records via `BatchPageInsert`
5. On transaction rollback, `S3RollbackHandler` catches `UploadS3Event` and saves orphaned S3 keys to `S3DeleteFile` table for deferred cleanup — this is the data consistency compensation pattern

### S3 Data Consistency

When DB transaction rolls back after S3 upload succeeds, `S3RollbackHandler` listens on `TransactionPhase.AFTER_ROLLBACK` and stores the orphaned S3 key in a `S3DeleteFile` table using `REQUIRES_NEW` propagation. A separate batch job later cleans these up. The handler runs on the `s3ServiceTaskExecutor` async thread pool.

### Async & Retry

- Email sending is async on `mailServiceTaskExecutor` (ThreadPoolTaskExecutor, core=14, max=20, queue=30).
- `MailSendRetryPolicy` extends `SimpleRetryPolicy` to suppress retries for non-transient SMTP failures (wrong address, no recipients, etc.).
- Retry config is in `RetryConfig`.

### Batch Operations

- `BatchPageInsert` — bulk-inserts page rows using JDBC batch for performance.
- `BatchBookDelete` / `BatchProfileDelete` — bulk-deletes to avoid N+1 on cascaded deletes.

### Swagger Docs

API docs interfaces live in `common/swagger/` (e.g., `BookControllerDocs`). Each controller implements its docs interface to keep Swagger annotations out of the controller class body.

### Performance Monitoring

`PerformanceMonitorAop` (active only with `@Profile("performance")`) wraps DataSource connections in a proxy to count DB calls per request, helping detect N+1 queries.

## Key Design Conventions

- All API responses wrap in `ResponseDTO<T>` with a `ResponseCode`.
- All domain exceptions extend a base exception and are caught in `GlobalExceptionHandler`, which maps them to `ErrorCode` enum values.
- QueryDSL is configured in `QueryDSLConfig`; use it for complex query predicates alongside standard Spring Data repositories.
- Spring Cache is enabled; Redis is used as the cache store (also used for refresh token storage).
