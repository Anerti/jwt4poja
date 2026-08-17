# AGENTS.md

Guidance for AI coding agents working in this repository.

## Project

**jwt4poja** (`com.techindna.anerti`, Gradle project `jwt4poja-962dc383`) — a POJA (poja.io) generated AWS-serverless Spring Boot 3.2 backend for a **grade-management system** serving a 3-year L2 program (`L2` — two learning paths: `EL` / `TN`, with shared `COMMON` courses). **All core domain slices are implemented:** auth, teacher/course/group/class/student provisioning, teacher-course assignments, exam management, grade management, grade reports (async PDF), graduation lists (Excel download), and a Thymeleaf promotions page.

- **Stack:** Java 21, Spring Boot 3.2.2, Gradle 8.5 (wrapper), Lombok 1.18.30, JaCoCo 0.8.11; Spring Data JPA, PostgreSQL, Spring Data Redis, Spring Security (Argon2), Thymeleaf, jjwt 0.12, BouncyCastle, OpenPDF 1.3.30, Apache POI 5.2.5.
- **Deployment:** AWS Lambda + API Gateway, serverless via SAM. Async via EventBridge → SQS → Lambda worker; email via SES; reports stored on S3 and streamed to the client. CD triggers on push to `preprod` / `prod` branches (see `.github/workflows/cd-compute.yml`). Region: `eu-west-3`.
- **Storage:** PostgreSQL (`PG_URL` / `PG_SCHEMA`, schema `jwt4poja_app`) + Redis (`REDIS_URL`, 15-min email-verification tokens); plus S3 (file / report bucket), SQS queues, EventBridge bus.
- **Package root / layout:** `com.techindna.anerti` — `endpoint/rest/controller/` (REST), `endpoint/web/` (Thymeleaf), `endpoint/event/` (async pipeline), `handler/` (Lambda entry points), `mail/`, `file/`, `concurrency/`, `datastructure/`, plus hand-written `dto/`, `entity/`, `exception/`, `security/`, `mapper/`, `repository/`, `service/`, `validator/`.

## Domain contract

The intended product is a grade-management system. Authoritative contract: [`doc/api.yml`](doc/api.yml) (OpenAPI 3.0.3). Conceptual data model: [`doc/mcd.canvas`](doc/mcd.canvas) (Obsidian Canvas, MCD format).

### Roles — `UserRole` enum (DB-level `jwt4poja_app.user_role`)

| Role | Meaning |
| --- | --- |
| `ADMIN` | Manages courses, exams, teachers, students, classes, assignments, grades, reports, graduations. |
| `TEACHER` | Read courses / exams / grades; create / update exams and grades for assigned courses. |
| `STUDENT` | Read their own profile, grades, and grade history. |

There is **no public registration**: an `ADMIN` provisions every account through `POST /students` / `POST /teachers`. Only `/auth/**`, the course catalog (`GET /courses`), and the promotions page (`GET /promotions`) are public.

### Implemented endpoints

- **Auth** — `endpoint/rest/controller/AuthController.java`:
  - `POST /auth/login` — 202 + login-verification link.
  - `GET /auth/verification/{token}` — 200 JWT + user, 401 invalid token.
- **Teachers** — `endpoint/rest/controller/TeacherController.java`:
  - `POST /teachers` — 201 (ADMIN); provisions user + teacher_inheritance.
  - `GET /teachers` — 200 (ADMIN); search + teacherStatus filter, pagination.
- **Courses** — `endpoint/rest/controller/CourseController.java`:
  - `POST /courses` — 201 (ADMIN); validates input, maps unique ref violations to 409.
  - `GET /courses` — 200 (public); search on ref/title, type filter, pagination.
- **Students** — `endpoint/rest/controller/StudentController.java`:
  - `POST /students` — 201 (ADMIN); provisions user + student_inheritance.
  - `GET /students` — 200 (ADMIN + TEACHER); search + level/learningPath/studentStatus/groupRef/className filters, pagination.
- **Groups** — `endpoint/rest/controller/GroupController.java`:
  - `POST /groups` — 201 (ADMIN).
  - `GET /groups` — 200 (ADMIN + TEACHER); search + type filter, pagination.
  - `DELETE /groups/{groupId}` — 204 (ADMIN).
- **Classes** — `endpoint/rest/controller/ClassController.java`:
  - `POST /classes` — 201 (ADMIN); validates name (max 30) and yearOf (2000–2100).
  - `GET /classes` — 200 (ADMIN + TEACHER); search by name, pagination.
- **Assignments** — `endpoint/rest/controller/TeacherCourseController.java`:
  - `POST /teacher-courses` — 201 (ADMIN); validates teacher/course exist, unique pair check.
- **Exams** — `endpoint/rest/controller/ExamController.java`:
  - `POST /exams` — 201 (ADMIN + TEACHER); validates coefficient sum ≤ 1 per course/academic-year.
  - `GET /exams` — 200 (ADMIN + TEACHER + STUDENT); ref/academicYear/date-range filters, teacher scoped to assigned courses.
- **Grades** — `endpoint/rest/controller/GradeController.java`:
  - `POST /grades` — 201 (ADMIN + TEACHER); validates teacher is assigned to the exam's course.
  - `GET /grades/{studentInheritanceId}` — 200 (ADMIN + TEACHER + STUDENT); courseRef/academicYear filters, pagination; student sees own only.
  - `GET /grades/compute/{studentInheritanceId}/course/{courseRef}` — 200 (ADMIN + TEACHER + STUDENT); weighted average via CTE with `ROW_NUMBER()` dedup.
- **Grade Reports** — `endpoint/rest/controller/GradeReportController.java`:
  - `POST /grade-reports` — 202 (ADMIN + TEACHER); fires async `GradeReportRequested` event → PDF generated → uploaded to S3 → presigned URL emailed to student.
- **Graduations** — `endpoint/rest/controller/GraduationController.java`:
  - `GET /graduations/{classId}` — 200 (ADMIN); ranked list of graduated students with weighted averages via native SQL CTE.
  - `GET /graduations/{classId}/download` — 200 (ADMIN); styled .xlsx download via Apache POI.
- **Promotions (Thymeleaf)** — `endpoint/web/PromotionController.java`:
  - `GET /promotions` — HTML page listing all classes.
- **Health (POJA scaffold)** — `GET /ping`, `GET /health/email`, `GET /health/bucket`.

### Implemented services

- `service/AuthService` — login (email verification link) + token verification (Redis).
- `service/TeacherService` — CRUD teachers (user + teacher_inheritance).
- `service/CourseService` — list/create courses.
- `service/StudentService` — list/create students (user + student_inheritance).
- `service/GroupService` — list/create/delete groups.
- `service/ClassService` — list/create classes (JdbcTemplate for native SQL search).
- `service/TeacherCourseService` — assign teacher to course.
- `service/ExamService` — list/create exams (coefficient sum constraint, teacher assignment check).
- `service/GradeService` — list/create grades, compute weighted course average (native SQL CTE).
- `service/GraduationService` — list graduated students ranked by weighted average (JdbcTemplate + native SQL CTE).
- `service/GradeReportService` — generate PDF grade report → S3 upload → presigned URL.
- `service/PdfGenerator` — OpenPDF-based "Releve de Notes" PDF generation.
- `service/ExcelGenerator` — Apache POI .xlsx graduation list generation.
- `service/UserConflictHandler` — maps DB constraint violations to 409 ConflictException.
- `service/PageRequestData` — page/size validation (1–100, defaults 1/10).
- `service/VerificationCodeStore` — Redis 15-min verification tokens.
- `service/event/SendEmailRequestedService` — sends emails via SES.
- `service/event/GradeReportRequestedService` — orchestrates PDF generation + email notification.

### Implemented repositories

| Repository | Entity | Custom queries |
|---|---|---|
| `AuthRepository` | `JUser` | `findByEmail`, `findByUsername`, `findByStudentInheritanceId` |
| `UserRepository` | `JUser` | `searchTeachers` (JPQL), `searchStudents` (JPQL, 6 optional filters) |
| `ClassRepository` | `JClass` | `findByName`, `search` (native SQL with CAST) |
| `CourseRepository` | `JCourse` | `findByRef`, `search` (JPQL) |
| `GroupRepository` | `JGroup` | `findByRef`, `search` (JPQL), `delete` (@Modifying) |
| `ExamRepository` | `JExam` | `sumCoefficients`, `search` (native SQL with teacher_course join) |
| `GradeRepository` | `JGrade` | `search` (native SQL, 4 optional filters), `computeCourseGrade` (CTE + ROW_NUMBER) |
| `StudentInheritanceRepository` | `JStudentInheritance` | `findByRef` |
| `TeacherInheritanceRepository` | `JTeacherInheritance` | `findByRef` |
| `TeacherCourseRepository` | `JTeacherCourse` | `existsByTeacherInheritanceIdAndCourseId` |

### Implemented mappers

`UserMapper`, `TeacherInheritanceMapper`, `TeacherCourseMapper`, `StudentInheritanceMapper`, `ClassMapper`, `CourseMapper`, `GroupMapper`, `ExamMapper`, `GradeMapper`.

### Implemented validators

`DataValidator` (core), `UserValidator`, `StudentValidator`, `CourseValidator`, `ClassValidator`, `ExamValidator`, `GradeValidator`, `TeacherCourseValidator`.

### DTOs (34 records)

`LoginInput`, `MessageBody`, `VerifyRegistrationResponse`, `Meta`, `CreateClassInput`, `ClassOutput`, `ClassListResponse`, `CreateCourseInput`, `CourseOutput`, `CourseListResponse`, `CourseGradeOutput`, `CreateGroupInput`, `GroupOutput`, `GroupListResponse`, `CreateExamInput`, `ExamOutput`, `ExamListResponse`, `CreateGradeInput`, `GradeOutput`, `GradeListResponse`, `GradeReportRequest`, `GradeReportOutput`, `GraduateOutput`, `GraduationListResponse`, `CreateStudentInput`, `StudentInheritance`, `UserExtendStudent`, `StudentListResponse`, `CreateTeacherInput`, `TeacherInheritance`, `UserExtendTeacher`, `TeacherListResponse`, `TeacherCourse`, `CreateTeacherCourseInput`.

### Database migrations

All applied manually (not via Flyway). All in `src/main/resources/db/migration/`:

| File | Content |
|---|---|
| `V1__init.sql` | Schema + enums (`user_role`, `teacher_status`, `student_level`, `learning_path`, `student_status`) + `teacher_inheritance`, `student_inheritance`, `user` tables. |
| `V2__add_course.sql` | `course_type` enum + `course` table. |
| `V3__add_group.sql` | `group_type` enum + `"group"` table. |
| `V4__add_class.sql` | `"class"` table + adds `group_id`/`class_id` FK to `student_inheritance` (drops `class_name`/`graduation_year`). |
| `V5__add_teacher_course.sql` | `teacher_course` junction table. |
| `V6__add_exam.sql` | `exam` table (`course_id` FK ON DELETE CASCADE, `coefficient`, `academic_year`, `date`). |
| `V7__add_grade.sql` | `grade` table (`student_inheritance_id` FK, `exam_id` FK ON DELETE CASCADE, `value`, `description`, `created_at`). |

### Integration tests (247 total, all passing)

`src/test/java/com/techindna/anerti/endpoint/rest/controller/`:

- `auth/LoginIT`, `auth/AuthVerificationIT`
- `teachers/PostTeachersIT`, `teachers/GetTeachersIT`
- `courses/PostCoursesIT`, `courses/GetCoursesIT`
- `students/PostStudentsIT`, `students/GetStudentsIT`
- `groups/PostGroupsIT`, `groups/GetGroupsIT`, `groups/DeleteGroupsIT`
- `assignments/PostTeacherCoursesIT`
- `exams/PostExamsIT`, `exams/GetExamsIT`
- `grades/PostGradesIT`, `grades/GetGradesIT`, `grades/GetComputeCourseGradeIT`
- `classes/PostClassesIT`, `classes/GetClassesIT`
- `graduations/GetGraduationIT`

### Still spec-only (no Java)

- `GET/PATCH /teachers/{teacherId}`, `GET/PATCH /students/{studentId}`, `GET/PATCH /courses/{courseId}`, `GET/PATCH /groups/{groupId}`
- `DELETE /teacher-courses/{teacherCourseId}`, `GET /teacher-courses`
- `GET/PATCH/DELETE /exams/{examId}`, `PATCH /grades/{gradeId}`, `GET /grades/{gradeId}/history`
- `history` table (append-only grade correction log)

## Commands

```bash
export JAVA_HOME=$HOME/.jdks/ms-21.0.11 && export PATH=$JAVA_HOME/bin:$PATH   # java is not on PATH
sh gradlew compileJava                                                          # verified locally
sh gradlew test                                                                 # derived from CI — NOT run locally (needs Docker)
sh gradlew test --tests "com.techindna.anerti.conf.*"                           # targeted ITs; jacocoTestCoverageVerification still runs
./format.sh                                                                     # google-java-format (NO --aosp flag); then gate: git diff --exit-code
```

- `gradlew` is committed **without the exec bit** — use `sh gradlew …` locally (CI does `chmod +x` itself).
- Java must be ≤ 21 — Gradle rejects newer JDKs. Use the JDK export above.
- `sh gradlew test` needs **Docker + Testcontainers** (`FacadeIT` base class); CI runs `./gradlew test` on Java 21 corretto. The test env spins up `postgres:16-alpine` + `redis:7-alpine` via `conf/EnvConf`. Any `test` invocation is finalized by `jacocoTestCoverageVerification` (LINE coverage, minimum 0.85).
- `format.sh` needs `java` on PATH (same JDK export). **NEVER use `--aosp` flag** with google-java-format.

## Conventions

- **Formatting:** google-java-format 1.23.0 (`format.sh` uses the jar committed at repo root, 2-space indent, default Google style — NO `--aosp`). CI's `format` job fails on `git diff` after running it — treat `./format.sh && git diff --exit-code` as the gate before delivering code.
- **Generated code:** `@PojaGenerated` marks POJA-generated files (`PojaApplication`, health controllers, event pipeline, `LambdaHandler` / `MailboxEventHandler`, `FacadeIT`, test `conf/*`). **Never hand-edit annotated files** — `poja` regenerates / overwrites them. All business code is hand-written WITHOUT the annotation.
- **Layering (POJA style):** REST `endpoint/rest/controller/` → `service/` → `repository/`; DTOs as immutable `record`s in `dto/`; async via `EventProducer` in `endpoint/event/` (controller → `EventProducer` → EventBridge → SQS → `MailboxEventHandler` → `EventServiceInvoker` → `service/event/{EventName}Service`).
- **Load-bearing package rules:** event classes MUST live in `endpoint/event/model/` and extend `PojaEvent` (override `maxConsumerDuration()`, `maxConsumerBackoffBetweenRetries()`); consumer services MUST be `service/event/{EventName}Service` implementing `Consumer<{EventName}>` — `EventServiceInvoker` maps event FQCN → service class reflectively.
- **Errors:** shared `ErrorBody` `{status, error, message, timestamp}` via `GlobalExceptionHandler` (in `exception/`); use the existing exception types in `exception/http/` (`ConflictException`, `UnprocessableContentException`, `ForbiddenException`, `UnauthorizedException`, `NotFoundException`, `BadRequestException`, `GoneException`) rather than raw `ResponseEntity`s.
- **Security:** JWT bearer auth (jjwt 0.12) in `security/` — `JwtTokenProvider` signs with the key from `app.jwt.secret` (base64, ≥256 bits) for `app.jwt.expiration-ms`; `JwtAuthenticationFilter` turns the `role` claim into `ROLE_*` authorities; `SecurityConfig` is stateless and permits `/auth/**`, `/ping`, `GET /courses`, and `GET /promotions`, everything else requires auth. Passwords: `Argon2PasswordEncoder` (needs BouncyCastle). Role checks live in `SecurityConfig` via `requestMatchers(...).hasRole("ADMIN")`.
- **Style:** Lombok `@AllArgsConstructor` / `@Builder` / `@Data`; `lombok.addLombokGeneratedAnnotation = true`.
- **Tests:** `@SpringBootTest(webEnvironment = RANDOM_PORT)` ITs extending `FacadeIT`; AWS beans get dummy values via `@DynamicPropertySource` (`EventConf` / `BucketConf` / `EmailConf` in `src/test/java/.../conf/`). Each test cleans up after itself in `@BeforeEach`. Each `adminToken()` call creates a fresh user — avoid calling it twice in the same test without reusing the result.
- **Native SQL:** for complex dynamic queries with JOINs, native SQL via `JdbcTemplate` or `@Query(nativeQuery = true)` is preferred over JPQL. Use `CAST(:param AS text)` for optional string parameters (not `CAST(:param AS string)`).

## Environment & secrets

- `application.properties` imports `optional:file:.env[.properties]`; **all variables are plain `${...}` placeholders with NO defaults** (`PG_URL`, `PG_SCHEMA`, `REDIS_URL`, `JWT_SECRET`, `APP_BASE_URL`, `PG_USERNAME`, `PG_PASSWORD`, `JWT_EXPIRATION_MS`). `aws.s3.bucket` / `aws.eventBridge.bus` also have no defaults (tests override them via `@DynamicPropertySource`).
- CI sets `AWS_REGION: eu-west-3`. Deploy-time env comes from the Poja console / SAM template, not the repo.
- `.env` exists at repo root and is **gitignored** — never read, print, or commit credentials.

## Git

- Branches: `feat/remaining-features` (current work branch, PR #32 against `preprod`), `preprod` (deployment branch — pushes trigger CD).
- History: Poja auto-commits (`poja: deployment ID: …`). Hand-written work follows short conventional commits (`feat:` / `fix:` / `docs:` / `build:` / `refactor:` / `test:` / `chore:`), in English, with `V` version tags excluded from commit messages.
- Don't commit, push, or rewrite history unless asked.
