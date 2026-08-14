# jwt4poja

Grade-management backend for a 3-year L2 program, built on a [POJA](https://poja.io)-generated Spring Boot app. Two learning paths — `EL` and `TN` — share a pool of `COMMON` courses. The OpenAPI spec is the intended contract; today only the **auth slice is implemented** (registration, email-verified login, JWT issuance).

Package root `com.techindna.anerti`, database schema `jwt4poja_app`.

## Stack

- Java 21, Spring Boot 3.2, Gradle
- PostgreSQL (JPA) + Redis (email-verification tokens, 15-minute TTL)
- Spring Security + jjwt 0.12 (HMAC-signed JWT bearer tokens)
- Argon2 password hashing (BouncyCastle)
- Thymeleaf mail templates (registration / login-verification emails)
- AWS: EventBridge → SQS → SES email, S3 for files and grade-report downloads (POJA scaffold)

## Roles

Three roles, DB enum `jwt4poja_app.user_role` (defined in the conceptual data model, MCD still being ported to DDL):

| Role | Meaning |
| --- | --- |
| `ADMIN` | Manages everything: courses, exams, teachers, students, classes, assignments, enrollments, grades, reports. |
| `TEACHER` | Reads courses and exams; creates / updates / deletes exams and grades for the courses assigned to them. |
| `STUDENT` | Reads their own profile, grades, and grade-correction history. |

There is **no public registration** in the spec — an `ADMIN` provisions every account through `POST /students` or `POST /teachers`. The current implementation still exposes a public `POST /auth/register` for backward compatibility with the prior customer / admin iteration; new work should follow the spec.

## API

Full OpenAPI spec: [`doc/api.yml`](doc/api.yml). Conceptual data model: [`doc/mcd.canvas`](doc/mcd.canvas).

### Implemented today — auth (`/auth/**`, public)

| Method | Path | Description |
| --- | --- | --- |
| `POST` | `/auth/register` | Public self-registration (legacy). 202 + verification email. **Not in the target spec — slated for removal.** |
| `POST` | `/auth/login` | Login (202; login-verification email sent). |
| `GET` | `/auth/verification/{token}` | Verify a single-use 15-minute token → JWT + user (200; 401 invalid). |

Flow: `register` or `login` → 15-minute single-use token in Redis → email link → `GET /auth/verification/{token}` → JWT (subject = user id, `role` claim).

### Implemented today — users (`/users/**`, JWT required)

| Method | Path | Description |
| --- | --- | --- |
| `GET` | `/users` | List users (ADMIN-only; currently `CUSTOMER` users, `search` matches username / first / last / email, 1-based `page` / `size` capped at 100, `sort` `ASC` / `DESC` on `createdAt`). **Legacy role; will be replaced by the spec's `GET /students` and `GET /teachers`.** |
| `GET` | `/users/{userId}` | Get a user (owner or ADMIN; ADMIN cannot read another ADMIN; 404 unknown, 400 malformed UUID). |

### Spec target — the rest of the domain

These are documented in `doc/api.yml` as the contract to implement, **no Java yet**. Authorization reflects the spec.

| Tag | Paths | Who can act |
| --- | --- | --- |
| `courses` | `POST/GET /courses`, `GET/PATCH /courses/{courseId}` | ADMIN manages, TEACHER reads |
| `exams` | `POST/GET /exams`, `GET/PATCH/DELETE /exams/{examId}` | ADMIN + TEACHER (scoped to assigned courses) |
| `teachers` | `POST/GET /teachers`, `GET/PATCH /teachers/{teacherId}` | ADMIN |
| `students` | `POST/GET /students`, `GET/PATCH /students/{studentId}` | ADMIN manages; TEACHER + owner read |
| `classes` | `POST/GET /classes`, `GET/PATCH/DELETE /classes/{classId}` | ADMIN manages, TEACHER reads |
| `assignments` | `POST/GET /teacher-courses`, `DELETE /teacher-courses/{teacherCourseId}` | ADMIN |
| `enrollments` | `POST/GET /student-classes`, `PATCH /student-classes/{studentClassId}` | ADMIN |
| `grades` | `POST /grades`, `GET /grades/{studentId}`, `PATCH /grades/{gradeId}`, `GET /grades/{gradeId}/history` | ADMIN + TEACHER (scoped); STUDENT own |
| `reports` | `POST /grade-reports`, `GET /graduations/{promotion}`, `GET /graduations/{promotion}/download` (XLSX) | ADMIN |

### Health (POJA scaffold)

`GET /ping`, `GET /health/email`, `GET /health/bucket`.

## Conceptual data model (target)

Nine tables, all in schema `jwt4poja_app` — see [`doc/mcd.canvas`](doc/mcd.canvas) for the full diagram.

- `user` — shared identity (UUID pk, unique `username` / `email`, `password`, `firstName`, `lastName`, `role` enum, timestamps).
- `teacher_extension` (1-1 with `user`, role `TEACHER`) — `ref` unique, `teacherStatus` enum (`ACTIVE` / `INACTIVE` / `OTHER`).
- `student_extension` (1-1 with `user`, role `STUDENT`) — `ref` unique, `learningPath` enum (`EL` / `TN` / `COMMON`), `studentStatus` enum (`GRADUATED` / `ACTIVE` / `INACTIVE`), `graduationYear`, `promotionName`.
- `course` — `ref` unique, `title`, `type` enum, `credits` 1-30.
- `teacher_course` — assignment of a teacher to a course (pair unique, `assignedAt`).
- `class` — student group / class (`ref` unique, `type` enum).
- `student_class` — enrollment of a student in a class (pair unique, `joinedAt`, `leftAt`).
- `exam` — exam of a course in an academic year (`coefficient`, `academicYear`, `date`).
- `grade` — grade of a student for an exam.
- `history` — append-only correction log of a grade (`grade`, `description`, `createdAt`).

The current `src/main/resources/db/migration/V1__init.sql` still ships the **legacy** single-`user` table with roles `CUSTOMER` / `ADMIN`; the MCD migration to the full schema is pending.

## Environment (`.env`)

Create `.env` at the repo root (gitignored — never commit credentials):

| Variable | Required | Default | Description |
| --- | --- | --- | --- |
| `PG_URL` | ✅ | — | JDBC URL, e.g. `jdbc:postgresql://localhost:5432/postgres` |
| `PG_SCHEMA` | ✅ | — | e.g. `jwt4poja_app` |
| `REDIS_URL` | ✅ | — | e.g. `redis://localhost:6379` |
| `JWT_SECRET` | ✅ | — | Base64-encoded, ≥ 256 bits |
| `APP_BASE_URL` | ✅ | — | Public base URL used in verification email links |
| `PG_USERNAME` | ✅ | — | |
| `PG_PASSWORD` | ✅ | — | |
| `JWT_EXPIRATION_MS` | ✅ | — | JWT lifetime in milliseconds |
| `aws.s3.bucket` | ✅ | — | S3 bucket (POJA scaffold) — required for non-test local runs |
| `aws.eventBridge.bus` | ✅ | — | EventBridge bus (POJA scaffold) — required for non-test local runs |

`src/main/resources/application.properties` imports the file via `spring.config.import=optional:file:.env[.properties]`.

No defaults are defined anywhere in the repo — every variable above is a plain `${...}` placeholder, so a missing one fails at startup (`aws.*` only affects non-test local runs; the rest apply to tests too).

## Commands

```bash
export JAVA_HOME=$HOME/.jdks/ms-21.0.11 && export PATH=$JAVA_HOME/bin:$PATH
sh gradlew compileJava         # compile main sources
sh gradlew compileTestJava     # compile tests
sh gradlew test                # integration tests — needs Docker (Testcontainers:
                               #   postgres:16-alpine + redis:7-alpine)
./format.sh                    # google-java-format; CI gate is `git diff --exit-code`
```

Note: `gradlew` has no exec bit in this repo — use `sh gradlew …`.

## Tests

The auth slice is covered by Testcontainers integration tests (PostgreSQL + Redis via `FacadeIT`) in `src/test/java/com/techindna/anerti/endpoint/rest/controller/auth/`: `LoginIT`, `AuthVerificationIT`. The user endpoints are covered by `UserListIT` and `UserGetIT` in `.../controller/users/`. Targeted run:

```bash
sh gradlew test --tests "com.techindna.anerti.endpoint.rest.controller.users.*"
```

Every `test` run is finalized by `jacocoTestCoverageVerification` (LINE coverage, minimum 85%) + `jacocoTestReport`. New domain features are expected to land with their own ITs (controller + service + repository, constructor injection, no `@Autowired`).

## Layout notes

- `src/main/java/com/techindna/anerti/` — hand-written business code (no `@PojaGenerated`):
  `endpoint/rest/controller/AuthController`, `service/AuthService` + `VerificationCodeStore`, `service/UserService`, `security/` (`SecurityConfig`, `JwtTokenProvider`, `JwtAuthenticationFilter`, `ResourcesAccessRules`), `repository/`, `mapper/`, `validator/`, `exception/`, `entity/`, `dto/`.
- `src/main/resources/db/migration/V1__init.sql` — schema DDL (legacy `user_role` enum + `user` table); applied manually, not via Flyway. `src/test/resources/test-init.sql` mirrors the same legacy `user` table for Testcontainers.
- Everything else (`health/`, `file/`, `mail/`, `concurrency/`, `datastructure/`, the `endpoint/event/` pipeline) is the POJA scaffold — except the hand-written `endpoint/event/model/SendEmailRequested` + `service/event/SendEmailRequestedService` (auth email pipeline).
- `doc/api.yml` (OpenAPI 3.0.3) and `doc/mcd.canvas` (Obsidian MCD) are the source of truth for the target domain. Update them in lockstep with any new feature.
