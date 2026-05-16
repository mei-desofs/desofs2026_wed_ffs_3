# Coffeetaria – Phase 2: Sprint 1 Deliverable

**Course:** Desenvolvimento de Software Seguro (DESOFS) 2025/2026
**Class:** wed_ffs | **Team:** 3 | **Repository:** desofs2026_wed_ffs_3

**Team Members:**
- Leonardo Costa, 1250532
- Henrique Dias, 1201816
- Luís Santos, 1250534
- Lourenço Mendes, 1201270
- Nuno Oliveira, 1210939

---

## What Changed in Sprint 1

Sprint 1 introduced three major infrastructure and functionality milestones on top of the Phase 1 codebase:

1. **PostgreSQL migration** — replaced the H2 in-memory database with a persistent PostgreSQL instance
2. **Docker Compose** — containerised the application stack (app + database) for reproducible deployments
3. **File System Operations** — added OS-level file and directory management via a new REST API

---

## 1. Architecture Overview

```
┌─────────────────────────────────────────────┐
│              Docker Compose                  │
│                                             │
│  ┌────────────────┐   ┌──────────────────┐  │
│  │  Spring Boot   │──▶│   PostgreSQL 16  │  │
│  │  app:8081      │   │   postgres:5432  │  │
│  │                │   │                  │  │
│  │  REST API      │   │  cafeteriadb     │  │
│  │  JWT Auth      │   │  (volume:        │  │
│  │  File I/O      │   │   postgres_data) │  │
│  └────────┬───────┘   └──────────────────┘  │
│           │ volume: app_files                │
│           ▼                                  │
│  /app/files  (sandboxed file storage)        │
└─────────────────────────────────────────────┘
```

---

## 2. Database Migration: H2 → PostgreSQL

### What changed

| Component | Before | After |
|---|---|---|
| Database | H2 (in-memory) | PostgreSQL 16 (persistent) |
| Driver | `org.h2.Driver` | `org.postgresql.Driver` |
| JPA Dialect | `H2Dialect` | `PostgreSQLDialect` |
| DDL strategy | `create-drop` | `create` |
| H2 Console | enabled | removed |

### Configuration (`application.properties`)

All connection details are injected via environment variables with safe defaults for local development:

```properties
spring.datasource.url=${SPRING_DATASOURCE_URL:jdbc:postgresql://localhost:5432/cafeteriadb}
spring.datasource.username=${SPRING_DATASOURCE_USERNAME:cafeteria}
spring.datasource.password=${SPRING_DATASOURCE_PASSWORD:cafeteria_pass}
spring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect
spring.jpa.hibernate.ddl-auto=create
```

### Test isolation

H2 is kept as a `test`-scoped Maven dependency. The existing `src/test/resources/application.properties` overrides the datasource for all test runs, so the 64 existing tests continue to pass without requiring a PostgreSQL instance:

```
[INFO] Tests run: 64, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

### Seed data (`data.sql`)

The `data.sql` was updated for full PostgreSQL compatibility:
- Multi-row `VALUES` syntax replaces the H2-specific `SELECT * FROM (VALUES ...) alias` pattern
- Future dates updated from 2025 → 2027 for menus and purchases
- BCrypt password hashes corrected (verified against `BCryptPasswordEncoder`)

All seed users have password **`123`**:

| Username | Role |
|---|---|
| `admin` | ADMIN |
| `john_employee` | EMPLOYEE |
| `susan_employee` | EMPLOYEE |
| `mary_client` | CLIENT |
| `peter_client` | CLIENT |

---

## 3. Docker Compose

### Files

| File | Purpose |
|---|---|
| `docker-compose.yml` (repo root) | Defines the full application stack |
| `project/Dockerfile` | Multi-stage build: Maven 3.9 + JRE 17 Alpine |

### Dockerfile (multi-stage)

```dockerfile
FROM maven:3.9-eclipse-temurin-17-alpine AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -B    # layer cached for faster rebuilds
COPY src ./src
RUN mvn package -DskipTests -B

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
RUN mkdir -p /app/files
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]
```

### docker-compose.yml highlights

```yaml
services:
  postgres:
    image: postgres:16-alpine
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U cafeteria -d cafeteriadb"]
    volumes:
      - postgres_data:/var/lib/postgresql/data   # persistent DB

  app:
    build: ./project
    environment:
      JWT_SECRET: ${JWT_SECRET:-change-this-in-production...}
      APP_FILESYSTEM_BASE_DIR: /app/files
    volumes:
      - app_files:/app/files                     # persistent file storage
    depends_on:
      postgres:
        condition: service_healthy               # waits for DB readiness
```

### Running the stack

```bash
# Build and start (first run downloads images, ~2-3 min)
docker compose up --build

# Stop and remove containers (data volumes are preserved)
docker compose down

# Stop and remove everything including volumes
docker compose down -v
```

The application is available at `http://localhost:8081`.
Swagger UI: `http://localhost:8081/swagger-ui/index.html`

---

## 4. File System Operations

### Purpose

The mandatory requirement to "execute OS functionalities (creating directories, reading/writing files)" is fulfilled through a dedicated REST resource at `/api/files`. In the cafeteria context, this is used for storing daily menu documents, nutritional information files, and reports.

### Security design

All file operations are sandboxed to a single base directory (`/app/files` in Docker, configurable via `APP_FILESYSTEM_BASE_DIR`). The `FileSystemService` normalises and validates every user-supplied path before touching the filesystem:

```java
private Path resolveSafe(String relativePath) {
    Path resolved = basePath.resolve(relativePath).normalize();
    if (!resolved.startsWith(basePath)) {
        // path traversal detected — generic error, no filesystem info leaked
        throw new IllegalArgumentException(
            "Invalid path: access outside the allowed directory is not permitted");
    }
    return resolved;
}
```

Additional controls:
- Maximum file size: **1 MB** per write operation
- Only text files (UTF-8) are supported
- Parent directories are created automatically on write

### Endpoints

| Method | Path | Body / Params | Roles | Description |
|---|---|---|---|---|
| `POST` | `/api/files/directory` | `{"path":"menus/2027-12"}` | ADMIN, EMPLOYEE | Create directory |
| `POST` | `/api/files` | `{"path":"...", "content":"..."}` | ADMIN, EMPLOYEE | Write text file |
| `GET` | `/api/files?path=...` | query param | ADMIN, EMPLOYEE, CLIENT | Read file content |
| `GET` | `/api/files/list?path=...` | query param (optional) | ADMIN, EMPLOYEE, CLIENT | List directory |
| `DELETE` | `/api/files?path=...` | query param | ADMIN | Delete file or directory |

### Example session

```bash
# 1. Login
TOKEN=$(curl -s -X POST http://localhost:8081/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"123"}' | jq -r .token)

# 2. Create a directory
curl -X POST http://localhost:8081/api/files/directory \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"path":"menus/2027-12"}'

# 3. Write a menu file
curl -X POST http://localhost:8081/api/files \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"path":"menus/2027-12/daily.txt","content":"Meat: Beef Stew\nFish: Salmon\nVeg: Veggie Bowl"}'

# 4. List the directory
curl "http://localhost:8081/api/files/list?path=menus" \
  -H "Authorization: Bearer $TOKEN"
# → {"path":"menus","entries":["menus/2027-12"]}

# 5. Read the file
curl "http://localhost:8081/api/files?path=menus/2027-12/daily.txt" \
  -H "Authorization: Bearer $TOKEN"
# → {"path":"menus/2027-12/daily.txt","content":"Meat: Beef Stew\n..."}

# 6. Path traversal is blocked
curl "http://localhost:8081/api/files?path=../../etc/passwd" \
  -H "Authorization: Bearer $TOKEN"
# → {"error":"Invalid path: access outside the allowed directory is not permitted"}
```

---

## 5. New Files Summary

| Path | Description |
|---|---|
| `docker-compose.yml` | Full stack definition (app + PostgreSQL) |
| `project/Dockerfile` | Multi-stage Docker build |
| `project/src/main/java/.../service/FileSystemService.java` | File I/O service with path traversal protection |
| `project/src/main/java/.../controller/FileController.java` | REST controller for file operations |
| `project/src/main/java/.../dto/FileWriteRequestDTO.java` | DTO for file write requests |
| `project/src/main/java/.../dto/FileContentResponseDTO.java` | DTO for file read responses |
| `project/src/main/java/.../dto/FileListResponseDTO.java` | DTO for directory listing responses |
| `project/src/main/java/.../dto/DirectoryCreateRequestDTO.java` | DTO for directory creation requests |

### Modified files

| Path | Change |
|---|---|
| `project/pom.xml` | PostgreSQL driver added; H2 moved to `test` scope |
| `project/src/main/resources/application.properties` | PostgreSQL config; filesystem base dir |
| `project/src/main/resources/data.sql` | PostgreSQL-compatible SQL; correct BCrypt hashes; future dates |
| `project/src/test/resources/application.properties` | Added `app.filesystem.base-dir` for test context |
| `project/src/main/java/.../config/SecurityConfig.java` | Authorization rules for `/api/files/**` endpoints |

---

## 6. Verification

### Tests (all pass with H2 — no PostgreSQL required)

```
mvn test
[INFO] Tests run: 64, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

### Docker Compose (full stack)

```
docker compose up --build
# PostgreSQL starts → healthcheck passes → app starts → seed data loaded
# App available at http://localhost:8081
```

### ASVS mapping

The changes in this sprint address the following ASVS 5.0 controls:

| ASVS | Requirement | Implementation |
|---|---|---|
| V1.1 | Secure software development lifecycle | Docker Compose enables reproducible environments |
| V2.1 | Password security | BCrypt with cost factor 10 for all stored passwords |
| V2.5 | Credential storage | Passwords never stored in plaintext; hashes in DB |
| V5.2 | Sanitization and Sandboxing | Path traversal prevention in `FileSystemService` |
| V8.1 | General data protection | Persistent volumes; data survives container restarts |
| V9.1 | Communications security | JWT auth required for all file endpoints |
| V14.2 | Dependency security | PostgreSQL driver via Spring Boot BOM; H2 test-only |
