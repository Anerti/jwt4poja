# AGENTS.md

Guidance for AI coding agents working in this repository.

## Project

**jwt4poja** (`com.techindna.anerti`, Gradle project `jwt4poja-962dc383`) — a POJA (poja.io) generated AWS-serverless Spring Boot 3.2 backend for a **grade-management system** serving a 3-year L2 program (`L2` — two learning paths: `EL` / `TN`, with shared `COMMON` courses). Currently **the auth slice, teacher provisioning, course management, group management, student provisioning, teacher-course assignments and exam management are implemented**; the rest of the domain is the intended contract, captured in the OpenAPI spec and the Obsidian MCD.

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

There is **no public registration**: an `ADMIN` provisions every account through `POST /students` / `POST /teachers`. Only `/auth/**` and the course catalog listing (`GET /courses`) are public.

### Conceptual data model (10 tables, all in schema `jwt4poja_app`)

- `user` — shared identity (UUID pk, `username` / `email` unique, `password`, `firstName`, `lastName`, `role` enum, `createdAt`, `updatedAt`).
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

- **auth** — `POST /auth/login`, `GET /auth/verification/{token}` (no public register).
- **courses** — `POST/GET /courses`, `GET/PATCH /courses/{courseId}` (ADMIN manages, public catalog listing).
- **exams** — `POST/GET /exams`, `GET/PATCH/DELETE /exams/{examId}` (ADMIN + TEACHER; TEACHER scoped to assigned courses).
- **teachers** — `POST/GET /teachers`, `GET/PATCH /teachers/{teacherId}` (ADMIN).
- **students** — `POST/GET /students`, `GET/PATCH /students/{studentId}` (ADMIN manages; TEACHER + owner read).
- **groups** — `POST/GET /groups`, `GET/DELETE /groups/{groupId}` (ADMIN manages, TEACHER reads).
- **assignments** — `POST/GET /teacher-courses`, `DELETE /teacher-courses/{teacherCourseId}` (ADMIN).
- **grades** — `POST /grades`, `GET /grades/{studentId}`, `PATCH /grades/{gradeId}`, `GET /grades/{gradeId}/history` (ADMIN + TEACHER; STUDENT own).
- **reports** — `POST /grade-reports` (async annual transcript by email), `GET /graduations/{promotion}`, `GET /graduations/{promotion}/download` (XLSX, ADMIN).

## Current state

**Auth + teacher provisioning, course management, group management, student provisioning, assignments and exam management are implemented; everything else is spec-only.** Ported so far: `user` + `teacher_inheritance` (entities `JUser` / `JTeacherInheritance`, DTOs `UserExtendTeacher` / `TeacherInheritance`, and `POST /teachers`), `course` (entity `JCourse`, DTOs `CourseOutput` / `CreateCourseInput` / `CourseListResponse`, `POST /courses` + public `GET /courses`), `group` (entity `JGroup` / `Group`, DTOs `GroupOutput` / `CreateGroupInput` / `GroupListResponse`, `POST /groups` + `GET /groups` + `DELETE /groups/{groupId}`), `student_inheritance` (entity `JStudentInheritance`, DTOs `UserExtendStudent` / `StudentInheritance` / `CreateStudentInput`, and `POST /students`), `teacher_course` (entity `JTeacherCourse`, DTOs `TeacherCourse` / `CreateTeacherCourseInput`, and `POST /teacher-courses`), and `exam` (entity `JExam`, DTOs `ExamOutput` / `CreateExamInput` / `ExamListResponse`, `POST /exams` + `GET /exams`). `grade`, `history`, etc. have no Java at all.

### Implemented endpoints (hand-written, no `@PojaGenerated`)

- **Auth** — `endpoint/rest/controller/AuthController.java`:
  - `POST /auth/login` — 202 + login-verification link.
  - `GET /auth/verification/{token}` — 200 JWT + user, 401 invalid token.
- **Teachers** — `endpoint/rest/controller/TeacherController.java`:
  - `POST /teachers` — 201 `UserExtendTeacher` (ADMIN-only); provisions a `user` row plus its `teacher_inheritance`, validates input, maps unique violations to 409.
  - `GET /teachers` — 200 `TeacherListResponse` (ADMIN-only); `search` substring, exact `teacherStatus` filter, `page`/`size` pagination.
- **Courses** — `endpoint/rest/controller/CourseController.java`:
  - `POST /courses` — 201 `CourseOutput` (ADMIN-only); validates input, maps unique `ref` violations to 409.
  - `GET /courses` — 200 `CourseListResponse` (public); `search` substring on `ref`/`title`, exact `type` filter, `page`/`size` pagination.
- **Students** — `endpoint/rest/controller/StudentController.java`:
  - `POST /students` — 201 `UserExtendStudent` (ADMIN-only); provisions a `user` row plus its `student_inheritance` (`level` / `learningPath` required, `studentStatus` defaults `ACTIVE`), validates input, maps unique violations to 409.
  - `GET /students` — 200 `StudentListResponse` (ADMIN + TEACHER); `search` substring, exact `level` / `learningPath` / `studentStatus` / `groupRef` / `className` filters, `page`/`size` pagination.
- **Groups** — `endpoint/rest/controller/GroupController.java`:
  - `POST /groups` — 201 `GroupOutput` (ADMIN-only); validates input, maps unique `ref` violations to 409.
  - `GET /groups` — 200 `GroupListResponse` (ADMIN + TEACHER); `search` substring on `ref`, exact `type` filter, `page`/`size` pagination.
  - `DELETE /groups/{groupId}` — 204 (ADMIN-only); 404 unknown id.
- **Exams** — `endpoint/rest/controller/ExamController.java`:
  - `POST /exams` — 201 `ExamOutput` (ADMIN + TEACHER); validates input, checks course exists, TEACHER must be assigned to the course (`requireAssignedToCourse`), rejects when the sum of `coefficient`s for the course/academic-year would exceed 1 (409).
  - `GET /exams` — 200 `ExamListResponse` (ADMIN + TEACHER + STUDENT); `ref` (course-ref, partial match), exact `academicYear`, inclusive `startDate`/`endDate` filters, `page`/`size` pagination; ordered by `date`; TEACHER scoped to assigned courses via SQL `EXISTS` on `teacher_course`.
- **Assignments** — `endpoint/rest/controller/TeacherCourseController.java`:
  - `POST /teacher-courses` — 201 `TeacherCourse` (ADMIN-only); validates input, maps unique pair violations to 409.
- **Health (POJA scaffold)** — `GET /ping`, `GET /health/email`, `GET /health/bucket`.

### Hand-written layer (all ported, none carry `@PojaGenerated`)

- `endpoint/rest/controller/{AuthController, TeacherController, CourseController, StudentController, GroupController, ExamController, TeacherCourseController}`.
- `service/AuthService`, `service/TeacherService`, `service/CourseService`, `service/StudentService`, `service/GroupService`, `service/ExamService`, `service/TeacherCourseService`, `service/UserConflictHandler`, `service/PageRequestData` (page/size clamped to 1–100, defaults 1/10), `service/VerificationCodeStore` (Redis 15-min tokens).
- `security/` — `SecurityConfig` (stateless; role enforcement via `requestMatchers(...).hasRole("ADMIN")`), `security/jwt/{JwtTokenProvider, JwtAuthenticationFilter}`, `security/AccessRules` (`requireAssignedToCourse` guard for TEACHER-scoped writes).
- `repository/{AuthRepository, UserRepository, TeacherInheritanceRepository, StudentInheritanceRepository, CourseRepository, GroupRepository, ExamRepository, TeacherCourseRepository}`, `repository/model/{JUser, JTeacherInheritance, JStudentInheritance, JCourse, JGroup, JClass, JExam, JTeacherCourse}`.
- `mapper/UserMapper` (user → JPA / domain), `mapper/TeacherInheritanceMapper` (`CreateTeacherInput` → `JTeacherInheritance`, `JUser` → `UserExtendTeacher`), `mapper/StudentInheritanceMapper` (`CreateStudentInput` → `JStudentInheritance`, `JUser` → `UserExtendStudent`), `mapper/CourseMapper` (`CreateCourseInput` → `JCourse`, `JCourse` → `CourseOutput`), `mapper/GroupMapper` (`CreateGroupInput` → `JGroup`, `JGroup` → `GroupOutput`), `mapper/ExamMapper` (`CreateExamInput` → `JExam`, `JExam` → `ExamOutput`).
- `validator/DataValidator`, `validator/UserValidator`, `validator/CourseValidator`, `validator/ExamValidator`.
- `exception/ErrorBody`, `exception/GlobalExceptionHandler`, `exception/http/*` (BadRequest / Conflict / Forbidden / Gone / NotFound / Unauthorized / UnprocessableContent).
- `entity/User`, `entity/Group` (domain records) and `repository/enums/{UserRole, TeacherStatus, StudentStatus, Level, LearningPath, CourseType}`. `UserRole` already matches the spec triple `ADMIN` / `TEACHER` / `STUDENT` (no legacy `CUSTOMER`).
- `dto/` — `CreateTeacherInput`, `LoginInput`, `MessageBody`, `TeacherInheritance`, `UserExtendTeacher`, `VerifyRegistrationResponse`, `CourseOutput`, `CreateCourseInput`, `CourseListResponse`, `CreateStudentInput`, `StudentInheritance`, `UserExtendStudent`, `CreateGroupInput`, `GroupOutput`, `GroupListResponse`, `Meta`, `StudentListResponse`, `TeacherListResponse`, `CreateExamInput`, `ExamOutput`, `ExamListResponse`, `CreateTeacherCourseInput`, `TeacherCourse`.
- `endpoint/event/model/SendEmailRequested` + `service/event/SendEmailRequestedService` (auth email pipeline).
- Mail template `resources/templates/mail/login-verification.html`.

### Database state

- `src/main/resources/db/migration/V1__init.sql` — schema `jwt4poja_app` with the spec role triple `(ADMIN, TEACHER, STUDENT)` and the `teacher_inheritance` / `student_inheritance` / `user` tables. Applied manually, not via Flyway. Still ahead of the code: `grade` / `history` etc. exist only in `doc/mcd.canvas`, not in the DDL.
- `src/main/resources/db/migration/V2__add_course.sql` — the `course` table (`ref` unique, `title`, `type` enum, `credits`) + its `course_type` enum.
- `src/main/resources/db/migration/V3__add_group.sql` — the `"group"` table (`ref` unique, `type` enum) + its `group_type` enum.
- `src/main/resources/db/migration/V4__add_class.sql` — the `"class"` table (spec-side `class` / `student_class`); no endpoints yet.
- `src/main/resources/db/migration/V5__add_teacher_course.sql` — the `teacher_course` table (`teacher_inheritance_id` FK, `course_id` FK, `assigned_at`; unique on the pair).
- `src/main/resources/db/migration/V6__add_exam.sql` — the `exam` table (`course_id` FK ON DELETE CASCADE, `coefficient` numeric(10,2), `academic_year`, `date`).
- `src/test/resources/test-init.sql` — mirrors V1 (same enums + tables); note its `user.username` is `VARCHAR(100)` vs V1's `VARCHAR(50)` — only the validator caps at 50. `test-course-init.sql` mirrors V2, `test-group-init.sql` mirrors V3, `test-class-init.sql` mirrors V4, `test-teacher-course-init.sql` mirrors V5 and `test-exam-init.sql` mirrors V6 for the Testcontainers test DB.

### Integration tests

`src/test/java/com/techindna/anerti/endpoint/rest/controller/`:

- `auth/LoginIT`, `auth/AuthVerificationIT`.
- `teachers/PostTeachersIT`, `teachers/GetTeachersIT`.
- `courses/PostCoursesIT`, `courses/GetCoursesIT`.
- `students/PostStudentsIT`, `students/GetStudentsIT`.
- `groups/PostGroupsIT`, `groups/GetGroupsIT`, `groups/DeleteGroupsIT`.
- `assignments/PostTeacherCoursesIT`.
- `exams/PostExamsIT`, `exams/GetExamsIT`.

No ITs yet for grades / reports, nor for `GET/PATCH /teachers/{teacherId}`, `GET/PATCH /students/{studentId}`, `GET/PATCH /courses/{courseId}`, `GET/PATCH /groups/{groupId}`, or the remaining exam paths (`GET/PATCH/DELETE /exams/{examId}`).

### Out of scope right now (spec only, no Java)

Everything under `/grades`, `/grade-reports`, `/graduations`, plus `GET/PATCH /students/{studentId}`, `GET/PATCH /teachers/{teacherId}`, the remaining course paths (`GET/PATCH /courses/{courseId}`), the remaining group paths (`GET/PATCH /groups/{groupId}`) and the remaining exam paths (`GET/PATCH/DELETE /exams/{examId}`) — along with the associated JPA entities, DTOs, services, mappers, validators, ITs, and the DB migration that materializes the full MCD. The security (`SecurityConfig` role matchers), exception, and validation infrastructure is already in place and reusable.

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
- `sh gradlew test` needs **Docker + Testcontainers** (`FacadeIT` base class); CI runs `./gradlew test` on Java 21 corretto. The test env spins up `postgres:16-alpine` + `redis:7-alpine` via `conf/EnvConf`. Any `test` invocation is finalized by `jacocoTestCoverageVerification` + `jacocoTestReport` (LINE coverage, minimum 0.85; full suite currently ~94% — a targeted `--tests` run alone does NOT meet the gate).
- `format.sh` needs `java` on PATH (same JDK export).

## Conventions

- **Formatting:** google-java-format 1.23.0 (`format.sh` uses the jar committed at repo root, 2-space indent). CI's `format` job fails on `git diff` after running it — treat `./format.sh && git diff --exit-code` as the gate before delivering code.
- **Generated code:** `@PojaGenerated` marks POJA-generated files (`PojaApplication`, health controllers, event pipeline, `LambdaHandler` / `MailboxEventHandler`, `FacadeIT`, test `conf/*`). **Never hand-edit annotated files** — `poja` regenerates / overwrites them. All business code is hand-written WITHOUT the annotation.
- **Layering (POJA style):** REST `endpoint/rest/controller/` → `service/` → `repository/`; DTOs as immutable `record`s in `dto/`; async via `EventProducer` in `endpoint/event/` (controller → `EventProducer` → EventBridge → SQS → `MailboxEventHandler` → `EventServiceInvoker` → `service/event/{EventName}Service`).
- **Load-bearing package rules:** event classes MUST live in `endpoint/event/model/` and extend `PojaEvent` (override `maxConsumerDuration()`, `maxConsumerBackoffBetweenRetries()`); consumer services MUST be `service/event/{EventName}Service` implementing `Consumer<{EventName}>` — `EventServiceInvoker` maps event FQCN → service class reflectively.
- **Errors:** shared `ErrorBody` `{status, error, message, timestamp}` via `GlobalExceptionHandler` (in `exception/`); use the existing exception types in `exception/http/` (`ConflictException`, `UnprocessableContentException`, `ForbiddenException`, `UnauthorizedException`, `NotFoundException`, `BadRequestException`, `GoneException`) rather than raw `ResponseEntity`s. Spring's missing-path-variable and missing-request-parameter exceptions map to 400 via dedicated handlers in `GlobalExceptionHandler`.
- **Security:** JWT bearer auth (jjwt 0.12) in `security/` — `JwtTokenProvider` signs with the key from `app.jwt.secret` (base64, ≥256 bits) for `app.jwt.expiration-ms`; `JwtAuthenticationFilter` turns the `role` claim into `ROLE_*` authorities; `SecurityConfig` is stateless and permits `/auth/**`, `/ping` and `GET /courses`, everything else requires auth. Passwords: `Argon2PasswordEncoder` (needs BouncyCastle). Role checks live in `SecurityConfig` via `requestMatchers(...).hasRole("ADMIN")` (e.g. `POST /teachers`); owner / finer-grained checks go inside services.
- **Style:** Lombok `@AllArgsConstructor` / `@Builder` / `@Data`; `lombok.addLombokGeneratedAnnotation = true`.
- **Tests:** `@SpringBootTest(webEnvironment = RANDOM_PORT)` ITs extending `FacadeIT`; AWS beans get dummy values via `@DynamicPropertySource` (`EventConf` / `BucketConf` / `EmailConf` in `src/test/java/.../conf/`). Mock `EventProducer` in ITs that hit endpoints emitting events (real bean would call AWS).
- **Spec-first:** when implementing a new domain, start from the OpenAPI path / schemas, the MCD, and (if needed) the `database conceptual data model` document. Keep `doc/api.yml` and the code in lockstep — update the spec first, then port the Java, then add ITs. The repo's own `mcd.canvas` is the authority for table shape (columns, uniques, enums).

## Environment & secrets

- `application.properties` imports `optional:file:.env[.properties]`; **all 8 variables are plain `${...}` placeholders with NO defaults anywhere in this repo** (`application.properties` and every `@Value` use bare placeholders): `PG_URL`, `PG_SCHEMA`, `REDIS_URL`, `JWT_SECRET` (base64, ≥256 bits), `APP_BASE_URL`, `PG_USERNAME`, `PG_PASSWORD`, `JWT_EXPIRATION_MS` — a missing one fails at startup, tests included. `aws.s3.bucket` / `aws.eventBridge.bus` also have no defaults (only needed for non-test local runs; tests override them via `@DynamicPropertySource`).
- CI sets `AWS_REGION: eu-west-3`. Deploy-time env comes from the Poja console / SAM template, not the repo.
- `.env` exists at repo root and is **gitignored** (`.gitignore` on this branch adds `.env` + `.obsidian/`) — never read, print, or commit credentials. Populate it with the keys above before local runs (tests resolve `app.jwt.secret` / `app.base-url` from it). If you add secret-holding files, leave them untracked too.

## Git

- Branches: `get-exams` (current work branch), `post-student-groups` (student provisioning), `get-courses` (course management), `post-teachers` (teacher provisioning), `preprod` (deployment branch — pushes trigger CD via the Poja API), `origin/preprod` is the remote default.
- History: Poja auto-commits (`poja: deployment ID: …`). Hand-written work follows short conventional commits (`feat:` / `docs:` / `build:` / `refactor:` / `test:` / `chore:`), typically one per file; multi-file endpoint features may bundle in one `feat:` commit.
- Don't commit, push, or rewrite history unless asked.
