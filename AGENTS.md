# AGENTS.md

Guidance for AI coding agents working in this repository.

## Project

**jwt4poja** (`com.techindna.anerti`, Gradle project `jwt4poja-962dc383`) — a POJA (poja.io) generated AWS-serverless Spring Boot app: a JWT auth template (register/login with email verification, Argon2 passwords).

- **Stack:** Java 21, Spring Boot 3.2.2, Gradle 8.5 (wrapper), Lombok 1.18.30, JaCoCo 0.8.11; Spring Data JPA, PostgreSQL, Spring Data Redis, Spring Security (Argon2), Thymeleaf, jjwt 0.12, BouncyCastle
- **Deployment:** AWS Lambda + API Gateway, serverless via SAM. Async via EventBridge → SQS → Lambda worker; email via SES. CD triggers on push to `preprod`/`prod` branches (see `.github/workflows/cd-compute.yml`). Region: eu-west-3.
- **Storage:** PostgreSQL (`PG_URL`/`PG_SCHEMA`, schema `jwt4poja_app`) + Redis (`REDIS_URL`, 15-min email-verification tokens); plus S3 (file bucket), SQS queues, EventBridge bus.
- **Package root / layout:** `com.techindna.anerti` — `endpoint/rest/controller/` (REST), `endpoint/event/` (async pipeline), `handler/` (Lambda entry points), `mail/`, `file/`, `concurrency/`, `datastructure/`, plus hand-written `dto/`, `entity/`, `exception/`, `security/`, `mapper/`, `repository/`, `service/`, `validator/`.

## Current state

**Auth flow implemented; everything else is POJA scaffold.** Hand-written auth layer on top of the generated scaffold (package root `com.techindna.anerti`, schema `jwt4poja_app`).

- `doc/api.yml` — OpenAPI 3.0.3 spec: implemented auth endpoints (`/auth/**`) + users endpoints (`/users/**`) specified as the intended contract but **not yet implemented** (marked as such in the spec).
- **Auth endpoints** (hand-written, `endpoint/rest/controller/AuthController.java`): `POST /auth/register` (202 + verification email), `POST /auth/login` (202 + login-verification link), `POST /auth/resend-link` (202, `email` query param; 403 unknown/already-verified email — "No pending verification found for this email", 422 blank/invalid email, 400 missing param), `GET /auth/verification/{token}` (200 JWT + user, 401 invalid token).
- **Generated health endpoints:** `GET /ping`, `GET /health/email`, `GET /health/bucket`.
- **Hand-written layer** (all ported, none carry `@PojaGenerated`): `service/AuthService` + `service/VerificationCodeStore` (Redis 15-min tokens), `security/` (JWT: `JwtTokenProvider`, `JwtAuthenticationFilter`, `SecurityConfig`; `ResourcesAccessRules`), `repository/AuthRepository` + `repository/model/JUser`, `mapper/UserMapper`, `validator/DataValidator` + `UserValidator`, `exception/` (`ErrorBody` + `GlobalExceptionHandler` + `http/*`), `entity/User` + `entity/enums/UserRole`, `dto/` (`RegisterInput`, `LoginInput`, `MessageBody`, `VerifyRegistrationResponse`, `UpdateUserInput`, `UserFilters`), `endpoint/event/model/SendEmailRequested` + `service/event/SendEmailRequestedService`, mail templates `resources/templates/mail/{verification,login-verification}.html`.
- **DB:** `resources/db/migration/V1__init.sql` — native DDL (`user_role` enum `CUSTOMER`/`ADMIN` + `user` table, schema `jwt4poja_app`, default role `CUSTOMER`); applied manually, not via Flyway. `src/test/resources/test-init.sql` mirrors it for Testcontainers (user table only — no other domains are ported; its `username` is `VARCHAR(100)` vs V1's `VARCHAR(50)` — only the validator caps at 50).
- **ITs:** `src/test/.../endpoint/rest/controller/auth/` — `RegisterIT`, `LoginIT`, `ResendLinkIT`, `AuthVerificationIT`.
- **Out of scope / not implemented:** movies, projections, rooms, and `GET /users` user management; `SecurityConfig` gates only `/auth/**` + health endpoints (no admin-role routes).
- `README.md` documents the project (stack, roles, API, env, commands); `.github/workflows/release-version.yml` references a `gradle.properties` that does not exist in this repo (generated workflow quirk — leave it).

## Commands

```bash
export JAVA_HOME=$HOME/.jdks/ms-21.0.11 && export PATH=$JAVA_HOME/bin:$PATH   # java is not on PATH
sh gradlew compileJava                                                          # verified locally
sh gradlew test                                                                 # derived from CI — NOT run locally (needs Docker)
sh gradlew test --tests "com.techindna.anerti.conf.*"                           # targeted ITs; jacocoTestCoverageVerification still runs
./format.sh                                                                     # google-java-format; then gate: git diff --exit-code
```

- `gradlew` is committed **without the exec bit** — use `sh gradlew …` locally (CI does `chmod +x` itself).
- Java must be ≤ 21 — Gradle rejects newer JDKs. Use the JDK export above.
- `sh gradlew test` needs **Docker + Testcontainers** (`FacadeIT` base class); CI runs `./gradlew test` on Java 21 corretto. The test env spins up `postgres:16-alpine` + `redis:7-alpine` via `conf/EnvConf`. Any `test` invocation is finalized by `jacocoTestCoverageVerification` + `jacocoTestReport` (LINE coverage, minimum 0).
- `format.sh` needs `java` on PATH (same JDK export).

## Conventions

- **Formatting:** google-java-format 1.23.0 (`format.sh` uses the jar committed at repo root, 2-space indent). CI's `format` job fails on `git diff` after running it — treat `./format.sh && git diff --exit-code` as the gate before delivering code.
- **Generated code:** `@PojaGenerated` marks POJA-generated files (`PojaApplication`, health controllers, event pipeline, `LambdaHandler`/`MailboxEventHandler`, `FacadeIT`, test `conf/*`). **Never hand-edit annotated files** — `poja` regenerates/overwrites them. All business code is hand-written WITHOUT the annotation.
- **Layering (POJA style):** REST `endpoint/rest/controller/` → `service/` → `repository/`; DTOs as immutable `record`s in `dto/`; async via `EventProducer` in `endpoint/event/` (controller → EventProducer → EventBridge → SQS → `MailboxEventHandler` → `EventServiceInvoker` → `service/event/{EventName}Service`).
- **Load-bearing package rules:** event classes MUST live in `endpoint/event/model/` and extend `PojaEvent` (override `maxConsumerDuration()`, `maxConsumerBackoffBetweenRetries()`); consumer services MUST be `service/event/{EventName}Service` implementing `Consumer<{EventName}>` — `EventServiceInvoker` maps event FQCN → service class reflectively.
- **Errors:** shared `ErrorBody` `{status, error, message, timestamp}` via `GlobalExceptionHandler` (in `exception/`); use the existing exception types in `exception/http/` (`ConflictException`, `UnprocessableContentException`, `ForbiddenException`, `UnauthorizedException`, `NotFoundException`, `BadRequestException`, `GoneException`) rather than raw `ResponseEntity`s. Spring's missing-path-variable and missing-request-parameter exceptions map to 400 via dedicated handlers in `GlobalExceptionHandler`.
- **Security:** JWT bearer auth (jjwt 0.12) in `security/` — `JwtTokenProvider` signs with the key from `app.jwt.secret` (base64, ≥256 bits) for `app.jwt.expiration-ms`; `JwtAuthenticationFilter` turns the `role` claim into `ROLE_*` authorities; `SecurityConfig` is stateless and permits `/auth/**` + health endpoints, everything else requires auth. Passwords: `Argon2PasswordEncoder` (needs BouncyCastle). `ResourcesAccessRules` handles owner/role checks inside services.
- **Style:** Lombok `@AllArgsConstructor`/`@Builder`/`@Data`; `lombok.addLombokGeneratedAnnotation=true`.
- **Tests:** `@SpringBootTest(webEnvironment = RANDOM_PORT)` ITs extending `FacadeIT`; AWS beans get dummy values via `@DynamicPropertySource` (`EventConf`/`BucketConf`/`EmailConf` in `src/test/java/.../conf/`). Mock `EventProducer` in ITs that hit endpoints emitting events (real bean would call AWS).

## Environment & secrets

- `application.properties` imports `optional:file:.env[.properties]`; **all 8 variables are plain `${...}` placeholders with NO defaults anywhere in this repo** (`application.properties` and every `@Value` use bare placeholders): `PG_URL`, `PG_SCHEMA`, `REDIS_URL`, `JWT_SECRET` (base64, ≥256 bits), `APP_BASE_URL`, `PG_USERNAME`, `PG_PASSWORD`, `JWT_EXPIRATION_MS` — a missing one fails at startup, tests included. `aws.s3.bucket` / `aws.eventBridge.bus` also have no defaults (only needed for non-test local runs; tests override them via `@DynamicPropertySource`).
- CI sets `AWS_REGION: eu-west-3`. Deploy-time env comes from the Poja console / SAM template, not the repo.
- `.env` exists at repo root and is **gitignored** (`.gitignore` on this branch adds `.env` + `.obsidian/`) — never read, print, or commit credentials. Populate it with the keys above before local runs (tests resolve `app.jwt.secret`/`app.base-url` from it). If you add secret-holding files, leave them untracked too.

## Git

- Branches: `test-post-resend-link` (current work branch), `preprod` (deployment branch — pushes trigger CD via the Poja API), `origin/preprod` is the remote default.
- History: Poja auto-commits (`poja: deployment ID: …`). Hand-written work follows short conventional commits (`feat:`/`docs:`/`build:`/`refactor:`/`chore:`), typically one per file; multi-file endpoint features may bundle in one `feat:` commit.
- Don't commit, push, or rewrite history unless asked.
