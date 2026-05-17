# Development – Phase 2 Sprint 1

## 1. Functionality Delivered

### 1.1 PostgreSQL Migration

The in-memory H2 database was replaced with a persistent PostgreSQL 16 instance. All connection credentials are injected via environment variables; there are no hardcoded values in the codebase.

| Component | Before | After |
|-----------|--------|-------|
| Database engine | H2 (in-memory) | PostgreSQL 16 (persistent) |
| JDBC driver | `org.h2.Driver` | `org.postgresql.Driver` |
| JPA dialect | `H2Dialect` | `PostgreSQLDialect` |
| DDL strategy | `create-drop` | `create` |
| H2 console | enabled (security risk) | removed |

**Relevant files:**
- `project/src/main/resources/application.properties` — PostgreSQL datasource config
- `project/pom.xml` — PostgreSQL driver added; H2 scoped to `test` only

### 1.2 Docker Compose

The full application stack is containerised for reproducible deployments.

| File | Purpose |
|------|---------|
| `docker-compose.yml` | Defines `app` + `postgres` services with health checks and persistent volumes |
| `project/Dockerfile` | Multi-stage build: Maven 3.9 builder → JRE 17 Alpine runtime |

**Key security decisions in the container setup:**
- Application does **not** run as `root`; the JRE image runs as a non-privileged user
- PostgreSQL credentials are passed as environment variables, never hardcoded
- A `healthcheck` on the `postgres` service prevents the app from starting before the DB is ready
- File storage is mounted as a named Docker volume (`app_files`), isolated from the host filesystem

**Running the stack:**
```bash
# Start the full stack (first run: ~2-3 min)
docker compose up --build

# Application: http://localhost:8081
# Swagger UI:  http://localhost:8081/swagger-ui/index.html
```

### 1.3 File System REST API

A dedicated `/api/files` resource fulfils the mandatory requirement to execute OS-level operations (creating directories, reading/writing files). All operations are sandboxed to a single base directory.

| Method | Endpoint | Roles | Description |
|--------|----------|-------|-------------|
| `POST` | `/api/files/directory` | ADMIN, EMPLOYEE | Create a directory |
| `POST` | `/api/files` | ADMIN, EMPLOYEE | Write a text file |
| `GET` | `/api/files?path=…` | ADMIN, EMPLOYEE, CLIENT | Read file content |
| `GET` | `/api/files/list?path=…` | ADMIN, EMPLOYEE, CLIENT | List directory entries |
| `DELETE` | `/api/files?path=…` | ADMIN | Delete file or directory |

**Relevant source files:**
- `project/src/main/java/com/cafeteriamanagement/service/FileSystemService.java`
- `project/src/main/java/com/cafeteriamanagement/controller/FileController.java`

---

## 2. Security Fixes and Practices

### 2.1 JWT Secret — Environment Variable Injection

**Problem:** The JWT signing secret was hardcoded as a `static final String` directly in `JwtTokenUtil.java`. Any actor with read access to the compiled JAR or the git history could extract the key and forge tokens with arbitrary roles.

**Fix applied:**
- Removed `static final SECRET` field
- Added `@Value("${jwt.secret}")` injected from the `JWT_SECRET` environment variable via Spring's property placeholder
- Added `@PostConstruct init()` to derive the `SecretKey` after injection (the only safe moment before the bean is used)
- Added `project/.env.example` documenting the required variable with a generation command (`openssl rand -hex 32`)

**Relevant files changed:**
- `project/src/main/java/com/cafeteriamanagement/security/JwtTokenUtil.java`
- `project/src/main/resources/application.properties`
- `project/src/test/resources/application.properties`
- `project/.env.example`
- `.gitignore` (`.env` and `application-prod.properties` excluded)

**SDR traceability:** SDR18 — *"Database connections must use credentials stored in environment variables, not hardcoded"* (extended to JWT keys)

**ASVS traceability:** V2.10.4 — Verify secrets are not stored in source code

### 2.2 JWT Expiry — Reduced to 1 Hour

**Problem:** Token validity was set to `7 * 24 * 60 * 60` seconds (7 days). A stolen token remained valid for an entire week, directly violating NFR03 and SDR01.

**Fix applied:**
- Changed `jwt.expiration=3600` (1 hour in seconds) in `application.properties`
- The value is configurable per environment without code changes

**SDR traceability:** SDR01 — *"JWT tokens must expire within a reasonable timeframe (1 hour for access tokens)"*

**ASVS traceability:** V3.3.2 — Verify session tokens expire after a defined period of inactivity

### 2.3 Path Traversal Protection (File System API)

All user-supplied paths are normalised and validated against the base directory before any filesystem operation:

```java
private Path resolveSafe(String relativePath) {
    Path resolved = basePath.resolve(relativePath).normalize();
    if (!resolved.startsWith(basePath)) {
        throw new IllegalArgumentException(
            "Invalid path: access outside the allowed directory is not permitted");
    }
    return resolved;
}
```

Additional controls:
- Maximum file size: **1 MB** per write
- Parent directories auto-created on write
- Generic error message — no filesystem structure leaked in response

**SDR traceability:** SDR13 — *"Restrict file path inputs using allowlists; never concatenate user input directly into file paths"*

**Abuse case traceability:** AC06 — *"Attacker submits `../../etc/passwd` to read arbitrary server files"*

### 2.4 H2 Console Disabled

The H2 web console (`/h2-console`) was available in the original configuration, exposing an unauthenticated database interface. It has been removed from `application.properties` in the main profile and is now only present in the test profile where it poses no risk.

### 2.5 Secrets Excluded from Version Control

`.gitignore` was updated to exclude:
```
.env
.env.local
application-local.properties
application-prod.properties
```

### 2.6 Rate Limiting on Login

`SimpleRateLimiter` (in-memory `ConcurrentHashMap`) is applied to the `/api/auth/login` endpoint. It tracks failed attempts per **username** and per **IP address** independently.

- Threshold: **5 failed attempts** within a **15-minute sliding window**
- On breach: HTTP 429 returned with a generic message; no account-specific information leaked
- Thread-safe: `synchronized` methods; expired entries cleaned on each check

**SDR traceability:** SDR04 — *"Implement account lockout or rate limiting after N failed login attempts"*

**Abuse case traceability:** AC01 — Brute-force / credential stuffing attack

### 2.7 Security Audit Logging

`SecurityAuditLogger` writes structured events to a dedicated `SECURITY_AUDIT` SLF4J logger. Every event includes: type, principal/username, authorities, IP, HTTP method, path, and reason.

**Events logged:**
- Login success / failure / rate-limit block
- JWT parsing error / expiration / validation failure
- Access denied (HTTP 403)
- Authentication required (HTTP 401)

All log entries are sanitised (control characters `\r\n\t` replaced with `_`) to prevent log injection.

**SDR traceability:** SDR19, SDR20

### 2.8 Password Policy and HIBP Integration

`PasswordPolicyService` validates new and updated passwords before they are stored:
- Minimum **12 characters** (configurable via `app.password.min`)
- Maximum **128 characters** (configurable via `app.password.max`)
- Delegates to `HaveIBeenPwnedClient` which uses the **k-anonymity** model: only the first 5 hex characters of the SHA-1 hash are sent to the HIBP API — the full password and hash never leave the application

Fail-open behaviour: if the HIBP API is unreachable (timeout 3 s), the check is skipped and the password is accepted (availability over security in degraded state).

**SDR traceability:** SDR05 — *"Passwords must have a minimum length of 12 characters"*

### 2.9 HTTPS Enforcement and Security Headers

`SecurityConfig` enforces transport security via Spring Security:
- `app.security.require-https=true` (production default) — all HTTP requests rejected
- `ForwardedHeaderFilter` at highest precedence — trusts `X-Forwarded-Proto: https` from a reverse proxy
- HSTS header: `Strict-Transport-Security: max-age=31536000` (1 year) added automatically when HTTPS is active
- `Cache-Control: no-cache, no-store, max-age=0, must-revalidate` on all responses (Spring Security default)
- Frame options: `SAMEORIGIN`

**SDR traceability:** SDR10, SDR11

---

## 3. Development Best Practices

| Practice | Status | Evidence |
|----------|--------|----------|
| Layered architecture (Controller → Service → Repository) enforced | ✅ | ArchUnit test `controller_dont_access_repos` |
| No hardcoded credentials in source code | ✅ | JWT secret and DB credentials via env vars |
| Parameterised queries via Spring Data JPA / Hibernate ORM | ✅ | All repositories extend `JpaRepository`; no string-concatenated SQL |
| Strict request DTOs (no direct entity binding) | ✅ | All controllers use DTO classes; entities never exposed directly |
| BCrypt password hashing | ✅ | `SecurityConfig.passwordEncoder()` — `BCryptPasswordEncoder` |
| Password policy (length + breach check) | ✅ | `PasswordPolicyService` + `HaveIBeenPwnedClient` |
| RBAC enforced at route level | ✅ | `SecurityConfig.filterChain()` — per-method role restrictions |
| Generic error messages on authentication failure | ✅ | `AuthController` always throws `BadCredentialsException("Invalid credentials")` |
| Rate limiting on login | ✅ | `SimpleRateLimiter` — 5 attempts / 15 min per username and IP |
| Structured security audit log | ✅ | `SecurityAuditLogger` — all auth and access-control events |
| HTTPS enforcement with HSTS | ✅ | `SecurityConfig` + `ForwardedHeaderFilter` |
| Persistent volumes for data | ✅ | Docker Compose named volumes for DB and file storage |
| CVE gate in CI (CVSS ≥ 7 fails build) | ✅ | OWASP Dependency-Check configured in `pom.xml` |
| Documented CVE suppressions with expiry | ✅ | `config/dependency-check-suppressions.xml` — expires 2026-06-30 |

---

## 4. Code Review Process

All changes in Sprint 1 are submitted via Pull Requests on the `main` branch. Each PR requires:
- At least one peer review before merge
- All tests passing (`mvn test`)
- No new compiler warnings introduced

> PR history is available in the GitHub repository under the **Pull Requests** tab.

---

## 5. Known Remaining Issues (Backlog)

| # | Issue | Severity | Status |
|---|-------|----------|--------|
| 1 | **`CustomUserDetailsService.java`** — `System.out.println` debug statements logging BCrypt hash | ✅ Fixed Sprint 1 | Removed (commit `a4876fd`) |
| 2 | **`TokenBlocklist`** not wired into `JwtRequestFilter` — logout non-functional | ✅ Fixed Sprint 1 | `POST /api/auth/logout` + filter check (commit `a4876fd`) |
| 3 | **Default credentials** in `data.sql` — 5 accounts with password "123" seeded in prod | ✅ Fixed Sprint 1 | Removed (commit `a4876fd`) |
| 4 | **`spring.jpa.show-sql=true`** in main `application.properties` — SQL queries logged in production | ✅ Fixed Sprint 1 | Set to `false` |
| 5 | **`@CrossOrigin(origins = "*")`** on all 7 controllers — no CORS allowlist | 🟠 Medium | Sprint 2 |
| 6 | **`PasswordUtil.java`, `PasswordTest.java`, `HashGenerator.java`** in `src/main/java/util` — debug utilities with hardcoded passwords in production code | 🟡 Low | Sprint 2 |
| 7 | Role checks only at HTTP route level — `@PreAuthorize` at service layer not yet implemented | 🟡 Low | Sprint 2 |
| 8 | No audit logging for file operations (read, write, delete) | 🟡 Low | Sprint 2 |
| 9 | JWT uses HS256 (symmetric) — design specified RS256 (asymmetric) | 🟠 Medium | Sprint 2 |
| 10 | Rate limiting only on login — no limit on file operations or purchase endpoints | 🟡 Low | Sprint 2 |
