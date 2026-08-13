# AGENTS.md

Guidance for AI coding agents working in this repository.

## Project

**jwt4poja** (`com.techindna.anerti`, Gradle project `jwt4poja-962dc383`) — a POJA (poja.io) generated AWS-serverless Spring Boot 3.2 backend for a **grade-management system** serving a 3-year L2 program (`L2` — two learning paths: `EL` / `TN`, with shared `COMMON` courses). Currently **only the auth slice is implemented**; the rest of the domain is the intended contract, captured in the OpenAPI spec and the Obsidian MCD.

- **Stack:** Java 21, Spring Boot 3.2.2, Gradle 8.5 (wrapper), Lombok 1.18.30, JaCoCo 0.8.11; Spring Data JPA, PostgreSQL, Spring Data Redis, Spring Security (Argon2), Thymeleaf, jjwt 0.12, BouncyCastle.
- **Deployment:** AWS Lambda + API Gateway, serverless via SAM. Async via EventBridge → SQS → Lambda worker; email via SES; reports stored on S3 and streamed to the client. CD triggers on push to `preprod` / `prod` branches (see `.github/workflows/cd-compute.yml`). Region: `eu-west-3`.
- **Storage:** PostgreSQL (`PG_URL` / `PG_SCHEMA`, schema `jwt4poja_app`) + Redis (`REDIS_URL`, 15-min email-verification tokens); plus S3 (file / report bucket), SQS queues, EventBridge bus.
- **Package root / layout:** `com.techindna.anerti` — `endpoint/rest/controller/` (REST), `endpoint/event/` (async pipeline), `handler/` (Lambda entry points), `mail/`, `file/`, `concurrency/`, `datastructure/`, plus hand-written `dto/`, `entity/`, `exception/`, `security/`, `mapper/`, `repository/`, `service/`, `validator/`.

## Domain contract (the spec, not yet the code)

The intended product is a grade-management system. Authoritative contract: [`doc/api.yml`](doc/api.yml) (OpenAPI 3.0.3, ~2.5k lines, 21 paths, ~55 schemas). Conceptual data model: [`doc/mcd.canvas`](doc/mcd.canvas) (Obsidian Canvas, MCD format).

### Roles — `UserRole` enum (DB-level `jwt4poja_app.user_role`)

| Role | Meaning |
| --- | --- |
| `ADMIN` | Manages courses, exams, teachers, students, classes, assignments, enrollments, grades, reports. |
| `TEACHER` | Read courses / exams; create / update / delete exams and grade for the courses assigned to them. |
| `STUDENT` | Read their own profile, grades and grade history. |

There is **no public registration**: an `ADMIN` provisions every account through `POST /students` / `POST /teachers`. Only `/auth/**` is public.

### Conceptual data model (9 tables, all in schema `jwt4poja_app`)

- `user` — shared identity (UUID pk, `username` / `email` unique, `password`, `firstName`, `lastName`, `role` enum, `verified`, `createdAt`, `updatedAt`).
- `teacher_extension` — 1-1 with `user` when `role = TEACHER` (`ref` unique, `teacherStatus` enum).
- `student_extension` — 1-1 with `user` when `role = STUDENT` (`ref` unique, `learningPath` enum, `studentStatus` enum, `graduationYear`, `promotionName`).
- `course` — catalog (`ref` unique, `title`, `type` enum, `credits`).
- `teacher_course` — assignment of a teacher to a course (`teacherExtensionId`, `courseId`, `assignedAt`; unique on the pair).
- `class` — student group / class (`ref` unique, `type` enum).
- `student_class` — enrollment of a student in a class (`studentExtensionId`, `classId`, `joinedAt`, `leftAt`; unique on the pair).
- `exam` — exam of a course in an academic year (`courseId`, `coefficient`, `academicYear`, `date`).
- `grade` — grade of a student for an exam (`studentExtensionId`, `examId`).
- `history` — append-only correction log of a grade (`gradeId`, `grade`, `description`, `createdAt`).

### API surface (per tag, from the spec)

- **auth** — `POST /auth/login`, `POST /auth/resend-link`, `GET /auth/verification/{token}` (no public register).
- **courses** — `POST/GET /courses`, `GET/PATCH /courses/{courseId}` (ADMIN manages, TEACHER reads).
- **exams** — `POST/GET /exams`, `GET/PATCH/DELETE /exams/{examId}` (ADMIN + TEACHER; TEACHER scoped to assigned courses).
- **teachers** — `POST/GET /teachers`, `GET/PATCH /teachers/{teacherId}` (ADMIN).
- **students** — `POST/GET /students`, `GET/PATCH /students/{studentId}` (ADMIN manages; TEACHER + owner read).
- **classes** — `POST/GET /classes`, `GET/PATCH/DELETE /classes/{classId}` (ADMIN manages, TEACHER reads).
- **assignments** — `POST/GET /teacher-courses`, `DELETE /teacher-courses/{teacherCourseId}` (ADMIN).
- **enrollments** — `POST/GET /student-classes`, `PATCH /student-classes/{studentClassId}` (ADMIN).
- **grades** — `POST /grades`, `GET /grades/{studentId}`, `PATCH /grades/{gradeId}`, `GET /grades/{gradeId}/history` (ADMIN + TEACHER; STUDENT own).
- **reports** — `POST /grade-reports` (async annual transcript by email), `GET /graduations/{promotion}`, `GET /graduations/{promotion}/download` (XLSX, ADMIN).

## Current state

**Auth slice implemented; everything else is spec-only.** The `user` table is the only ported domain object — `teacher_extension`, `student_extension`, `course`, `exam`, `grade`, etc. have no JPA entities, no DTOs, no controllers, no services, no ITs yet.

### Implemented endpoints (hand-written, no `@PojaGenerated`)

- **Auth** — `endpoint/rest/controller/AuthController.java`:
  - `POST /auth/register` — 202 + verification email (kept for backward compat with the prior customer/admin iteration; the spec no longer advertises it).
  - `POST /auth/login` — 202 + login-verification link.
  - `POST /auth/resend-link?email=` — 202; 403 unknown / already-verified email (`"No pending verification found for this email"`), 422 blank / invalid email, 400 missing param.
  - `GET /auth/verification/{token}` — 200 JWT + user, 401 invalid token.
- **Users** — `endpoint/rest/controller/UserController.java`:
  - `GET /users` — ADMIN-only; lists `CUSTOMER` users (legacy role), `search` substring on `username` / `firstName` / `lastName` / `email`, 1-based `page` / `size` (default 1 / 10, max 100), `sort` `ASC` | `DESC` on `createdAt`.
  - `GET /users/{userId}` — owner or ADMIN; ADMIN cannot read another ADMIN (enforced in `ResourcesAccessRules.grantAccessFor`), 404 unknown, 400 malformed UUID.
- **Health (POJA scaffold)** — `GET /ping`, `GET /health/email`, `GET /health/bucket`.

### Hand-written layer (all ported, none carry `@PojaGenerated`)

- `endpoint/rest/controller/{AuthController, UserController}`.
- `service/AuthService`, `service/UserService`, `service/VerificationCodeStore` (Redis 15-min tokens).
- `security/` — `JwtTokenProvider`, `JwtAuthenticationFilter`, `SecurityConfig`, `ResourcesAccessRules`.
- `repository/AuthRepository`, `repository/UserRepository`, `repository/model/JUser` (single `user` table).
- `mapper/UserMapper`.
- `validator/DataValidator`, `validator/UserValidator`.
- `exception/ErrorBody`, `exception/GlobalExceptionHandler`, `exception/http/*` (BadRequest / Conflict / Forbidden / Gone / NotFound / Unauthorized / UnprocessableContent).
- `entity/User`, `entity/enums/{UserRole, SortDirection}`. **Note:** the current `UserRole` enum still holds `CUSTOMER` / `ADMIN` — it has not been migrated to the spec's `ADMIN` / `TEACHER` / `STUDENT` triple yet. Treat the DB DDL and the spec as the source of truth going forward.
- `dto/` — `RegisterInput`, `LoginInput`, `MessageBody`, `VerifyRegistrationResponse`, `UpdateUserInput`, `Meta`, `UserListResponse`.
- `endpoint/event/model/SendEmailRequested` + `service/event/SendEmailRequestedService` (auth email pipeline).
- Mail templates `resources/templates/mail/{verification, login-verification}.html`.

### Database state

- `src/main/resources/db/migration/V1__init.sql` — still the **legacy** schema: `user_role` enum `(CUSTOMER, ADMIN)` + `user` table, schema `jwt4poja_app`, default role `CUSTOMER`, `username` `VARCHAR(50)`. Applied manually, not via Flyway. **This DDL is out of sync with `doc/mcd.canvas`** (the MCD has 9 tables and the `ADMIN` / `TEACHER` / `STUDENT` role triple). Migration to the full MCD is pending.
- `src/test/resources/test-init.sql` — mirrors the legacy `user` table only (no other domains are ported); its `username` is `VARCHAR(100)` vs V1's `VARCHAR(50)` — only the validator caps at 50.

### Integration tests

`src/test/java/com/techindna/anerti/endpoint/rest/controller/`:

- `auth/RegisterIT`, `auth/LoginIT`, `auth/ResendLinkIT`, `auth/AuthVerificationIT`.
- `users/UserListIT`, `users/UserGetIT`.

No ITs yet for courses / exams / teachers / students / classes / assignments / enrollments / grades / reports.

### Out of scope right now (spec only, no Java)

All paths under `/courses`, `/exams`, `/teachers`, `/students`, `/classes`, `/teacher-courses`, `/student-classes`, `/grades`, `/grade-reports`, `/graduations` — plus the associated JPA entities, DTOs, services, mappers, validators, ITs, and the DB migration that materializes the full MCD. `ResourcesAccessRules` and the security / exception / validation infrastructure are already in place and reusable.

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
- `sh gradlew test` needs **Docker + Testcontainers** (`FacadeIT` base class); CI runs `./gradlew test` on Java 21 corretto. The test env spins up `postgres:16-alpine` + `redis:7-alpine` via `conf/EnvConf`. Any `test` invocation is finalized by `jacocoTestCoverageVerification` + `jacocoTestReport` (LINE coverage, minimum 0.85; full suite currently ~87% — a targeted `--tests` run alone does NOT meet the gate).
- `format.sh` needs `java` on PATH (same JDK export).

## Conventions

- **Formatting:** google-java-format 1.23.0 (`format.sh` uses the jar committed at repo root, 2-space indent). CI's `format` job fails on `git diff` after running it — treat `./format.sh && git diff --exit-code` as the gate before delivering code.
- **Generated code:** `@PojaGenerated` marks POJA-generated files (`PojaApplication`, health controllers, event pipeline, `LambdaHandler` / `MailboxEventHandler`, `FacadeIT`, test `conf/*`). **Never hand-edit annotated files** — `poja` regenerates / overwrites them. All business code is hand-written WITHOUT the annotation.
- **Layering (POJA style):** REST `endpoint/rest/controller/` → `service/` → `repository/`; DTOs as immutable `record`s in `dto/`; async via `EventProducer` in `endpoint/event/` (controller → `EventProducer` → EventBridge → SQS → `MailboxEventHandler` → `EventServiceInvoker` → `service/event/{EventName}Service`).
- **Load-bearing package rules:** event classes MUST live in `endpoint/event/model/` and extend `PojaEvent` (override `maxConsumerDuration()`, `maxConsumerBackoffBetweenRetries()`); consumer services MUST be `service/event/{EventName}Service` implementing `Consumer<{EventName}>` — `EventServiceInvoker` maps event FQCN → service class reflectively.
- **Errors:** shared `ErrorBody` `{status, error, message, timestamp}` via `GlobalExceptionHandler` (in `exception/`); use the existing exception types in `exception/http/` (`ConflictException`, `UnprocessableContentException`, `ForbiddenException`, `UnauthorizedException`, `NotFoundException`, `BadRequestException`, `GoneException`) rather than raw `ResponseEntity`s. Spring's missing-path-variable and missing-request-parameter exceptions map to 400 via dedicated handlers in `GlobalExceptionHandler`.
- **Security:** JWT bearer auth (jjwt 0.12) in `security/` — `JwtTokenProvider` signs with the key from `app.jwt.secret` (base64, ≥256 bits) for `app.jwt.expiration-ms`; `JwtAuthenticationFilter` turns the `role` claim into `ROLE_*` authorities; `SecurityConfig` is stateless and permits `/auth/**` + health endpoints, everything else requires auth. Passwords: `Argon2PasswordEncoder` (needs BouncyCastle). `ResourcesAccessRules` handles owner / role checks inside services.
- **Style:** Lombok `@AllArgsConstructor` / `@Builder` / `@Data`; `lombok.addLombokGeneratedAnnotation = true`.
- **Tests:** `@SpringBootTest(webEnvironment = RANDOM_PORT)` ITs extending `FacadeIT`; AWS beans get dummy values via `@DynamicPropertySource` (`EventConf` / `BucketConf` / `EmailConf` in `src/test/java/.../conf/`). Mock `EventProducer` in ITs that hit endpoints emitting events (real bean would call AWS).
- **Spec-first:** when implementing a new domain, start from the OpenAPI path / schemas, the MCD, and (if needed) the `database conceptual data model` document. Keep `doc/api.yml` and the code in lockstep — update the spec first, then port the Java, then add ITs. The repo's own `mcd.canvas` is the authority for table shape (columns, uniques, enums).

## Environment & secrets

- `application.properties` imports `optional:file:.env[.properties]`; **all 8 variables are plain `${...}` placeholders with NO defaults anywhere in this repo** (`application.properties` and every `@Value` use bare placeholders): `PG_URL`, `PG_SCHEMA`, `REDIS_URL`, `JWT_SECRET` (base64, ≥256 bits), `APP_BASE_URL`, `PG_USERNAME`, `PG_PASSWORD`, `JWT_EXPIRATION_MS` — a missing one fails at startup, tests included. `aws.s3.bucket` / `aws.eventBridge.bus` also have no defaults (only needed for non-test local runs; tests override them via `@DynamicPropertySource`).
- CI sets `AWS_REGION: eu-west-3`. Deploy-time env comes from the Poja console / SAM template, not the repo.
- `.env` exists at repo root and is **gitignored** (`.gitignore` on this branch adds `.env` + `.obsidian/`) — never read, print, or commit credentials. Populate it with the keys above before local runs (tests resolve `app.jwt.secret` / `app.base-url` from it). If you add secret-holding files, leave them untracked too.

## Git

- Branches: `docs` (current work branch — last two commits are spec / MCD updates), `preprod` (deployment branch — pushes trigger CD via the Poja API), `origin/preprod` is the remote default.
- History: Poja auto-commits (`poja: deployment ID: …`). Hand-written work follows short conventional commits (`feat:` / `docs:` / `build:` / `refactor:` / `chore:`), typically one per file; multi-file endpoint features may bundle in one `feat:` commit. The two `docs:` commits at the top of the log updated the spec and the MCD before any Java is written for the new domains.
- Don't commit, push, or rewrite history unless asked.
