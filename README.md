# jwt4poja

JWT authentication template built on a [POJA](https://poja.io)-generated Spring Boot app.
Package root `com.techindna.anerti`, database schema `jwt4poja_app`.
Hand-written auth/security layer; everything else is the POJA scaffold.

## Stack

- Java 21, Spring Boot 3.2, Gradle
- PostgreSQL (JPA) + Redis (verification tokens)
- Spring Security + jjwt 0.12 (HMAC-signed JWT bearer tokens)
- Argon2 password hashing (BouncyCastle)
- Thymeleaf mail templates (verification / login-verification emails)
- AWS: EventBridge → SQS → SES email, S3 (POJA scaffold)

## Roles

Two roles only, DB enum `jwt4poja_app.user_role`:

| Role | Meaning |
| --- | --- |
| `CUSTOMER` | Default role for new registrations |
| `ADMIN` | Privileged role (currently only used by the planned `/users/**` endpoints) |

## API

Full OpenAPI spec: [`doc/api.yml`](doc/api.yml).

### Implemented — auth (`/auth/**`, public)

| Method | Path | Description |
| --- | --- | --- |
| `POST` | `/auth/register` | Register (202; verification email sent) |
| `POST` | `/auth/login` | Login (202; login-verification email sent) |
| `POST` | `/auth/resend-link?email=` | Resend verification email (202) |
| `GET` | `/auth/verification/{token}` | Verify token → JWT + user (200; 401 invalid) |

Flow: register/login → 15-min single-use token in Redis → email link →
`GET /auth/verification/{token}` → JWT (subject = user id, `role` claim).

### Implemented — users (`/users/**`, JWT required)

| Method | Path | Description |
| --- | --- | --- |
| `GET` | `/users` | List users (ADMIN; CUSTOMER-only, `search` matches username/first/last/email, 1-based pagination: `page` default 1, `size` default 10 max 100, `sort` ASC\|DESC default ASC on `createdAt`) |

### Specified but not yet implemented — users (`/users/**`)

| Method | Path | Description |
| --- | --- | --- |
| `GET` | `/users/{userId}` | Get user (owner or ADMIN) |
| `PATCH` | `/users/{userId}` | Partial profile update (owner or ADMIN) |
| `DELETE` | `/users/{userId}` | Delete account (owner or ADMIN) |

These are documented in `doc/api.yml` as the intended contract
(`UserController`/`UserService`), but the controllers/services are not implemented yet.
`ResourcesAccessRules` (owner/role checks) and the mapper/validators are already in place.

### Health (POJA scaffold)

`GET /ping`, `GET /health/email`, `GET /health/bucket`.

## Environment (`.env`)

Create `.env` at the repo root (gitignored — never commit credentials):

| Variable | Required | Default | Description |
| --- | --- | --- | --- |
| `PG_URL` | ✅ | — | JDBC URL, e.g. `jdbc:postgresql://localhost:5432/postgres` |
| `PG_SCHEMA` | ✅ | — | e.g. `jwt4poja_app` |
| `REDIS_URL` | ✅ | — | e.g. `redis://localhost:6379` |
| `JWT_SECRET` | ✅ | — | Base64-encoded, ≥ 256 bits |
| `APP_BASE_URL` | ✅ | — | Used in verification email links |
| `PG_USERNAME` | ✅ | — | |
| `PG_PASSWORD` | ✅ | — | |
| `JWT_EXPIRATION_MS` | ✅ | — | JWT lifetime |
| `aws.s3.bucket` | ✅ | — | S3 bucket (POJA scaffold) — required for non-test local runs |
| `aws.eventBridge.bus` | ✅ | — | EventBridge bus (POJA scaffold) — required for non-test local runs |

`src/main/resources/application.properties` imports the file via
`spring.config.import=optional:file:.env[.properties]`.

No defaults are defined anywhere in the repo — every variable above is a plain
`${...}` placeholder, so a missing one fails at startup (`aws.*` only affects
non-test local runs; the rest apply to tests too).

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

Auth flow is covered by Testcontainers integration tests (PostgreSQL + Redis via
`FacadeIT`) in `src/test/java/com/techindna/anerti/endpoint/rest/controller/auth/`:
`RegisterIT`, `LoginIT`, `ResendLinkIT`, `AuthVerificationIT`. User listing is
covered by `UserListIT` in `src/test/java/com/techindna/anerti/endpoint/rest/controller/users/`.
Targeted run:

```bash
sh gradlew test --tests "com.techindna.anerti.endpoint.rest.controller.auth.*"
```

## Layout notes

- `src/main/java/com/techindna/anerti/` — hand-written business code (no `@PojaGenerated`):
  `endpoint/rest/controller/AuthController`, `service/AuthService` + `VerificationCodeStore`,
  `security/` (`SecurityConfig`, `JwtTokenProvider`, `JwtAuthenticationFilter`,
  `ResourcesAccessRules`), `repository/`, `mapper/`, `validator/`, `exception/`, `entity/`, `dto/`.
- `src/main/resources/db/migration/V1__init.sql` — schema DDL (`user_role` enum + `user` table);
  applied manually, not via Flyway. `src/test/resources/test-init.sql` mirrors it for tests.
- Everything else (`health/`, `file/`, `mail/`, `concurrency/`, `datastructure/`, the
  `endpoint/event/` pipeline) is the POJA scaffold — except the hand-written
  `endpoint/event/model/SendEmailRequested` + `service/event/SendEmailRequestedService`
  (auth email pipeline).
