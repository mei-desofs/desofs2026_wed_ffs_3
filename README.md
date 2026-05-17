# Coffeetaria — DESOFS 2025/2026

**Course:** Desenvolvimento de Software Seguro (DESOFS) 2025/2026
**Class:** wed_ffs | **Team:** 3 | **Repository:** desofs2026_wed_ffs_3

**Team Members:**
| Name | Student ID |
|------|-----------|
| Leonardo Costa | 1250532 |
| Henrique Dias | 1201816 |
| Luís Santos | 1250534 |
| Lourenço Mendes | 1201270 |
| Nuno Oliveira | 1210939 |

---

## Project Description

**Coffeetaria** is a secure cafeteria management system built as a Spring Boot REST API backed by PostgreSQL. It supports three roles:

- **Client** — browse daily menus and place purchases
- **Employee** — manage dishes, ingredients and fulfill orders
- **Admin** — full system management including user administration

The system also performs OS-level file operations (report generation, audit log writing) sandboxed to a dedicated base directory.

---

## Technology Stack

| Layer | Technology |
|-------|-----------|
| Language | Java 17 |
| Framework | Spring Boot 3.5.14 |
| Security | Spring Security + JWT (HS256) |
| Database | PostgreSQL 16 |
| ORM | Spring Data JPA / Hibernate |
| Build | Maven 3.9 |
| Containerisation | Docker + Docker Compose |
| Runtime | Eclipse Temurin JRE 17 (Alpine) |

---

## Running the Application

```bash
# Copy and fill in the required environment variables
cp project/.env.example project/.env

# Start the full stack (app + database)
docker compose up --build

# Application:  http://localhost:8081
# Swagger UI:   http://localhost:8081/swagger-ui/index.html
```

**Required environment variables** (see `project/.env.example`):

| Variable | Description |
|----------|-------------|
| `JWT_SECRET` | JWT signing key — minimum 32 characters (`openssl rand -hex 32`) |
| `SPRING_DATASOURCE_URL` | PostgreSQL JDBC URL |
| `SPRING_DATASOURCE_USERNAME` | Database username |
| `SPRING_DATASOURCE_PASSWORD` | Database password |

---

## Project Phases

### Phase 1 — Analysis & Secure Design

> **Period:** Weeks 1–7 | **Status:** ✅ Complete

| Deliverable | Description |
|-------------|-------------|
| [Analysis & Requirements](Deliverables/analysis.md) | User stories, functional/non-functional requirements, SDRs, abuse cases |
| [Design](Deliverables/design.md) | Architecture, domain model, DFDs (Level 0, 1, 2), secure design decisions |
| [Threat Analysis](Deliverables/threat_analysis.md) | STRIDE per DFD element, risk assessment, mitigations |
| [Security Testing Plan](Deliverables/security_testing.md) | Test cases linked to threats and requirements |
| [ASVS 5.0 Tracker](Deliverables/ASVS_5_0_Tracker.xlsx) | OWASP ASVS 5.0 compliance checklist |

---

### Phase 2 — Secure Implementation

#### Sprint 1 — Weeks 8–11 (20/04/2026 – 18/05/2026) | ✅ Complete

| Deliverable | Description |
|-------------|-------------|
| [Sprint 1 Overview](Deliverables/phase2/sprint1/README.md) | Delivery summary and checklist |
| [Development](Deliverables/phase2/sprint1/development.md) | Security fixes, new controls, best practices |
| [Build & Test](Deliverables/phase2/sprint1/build_and_test.md) | Test results, coverage, SAST/SCA/DAST |
| [Pipeline](Deliverables/phase2/sprint1/pipeline.md) | GitHub Actions CI/CD pipeline definition and results |
| [ASVS Checklist](Deliverables/phase2/sprint1/asvs_checklist.md) | ASVS v4.0.3 sprint assessment |

**Key security controls delivered in Sprint 1:**
- JWT secret via environment variable; expiry reduced to 1 hour
- Rate limiting on login (5 attempts / 15 min per user and IP)
- Path traversal protection on the File System API
- Security audit logging for all auth and access-control events
- Password policy with HIBP breach check (k-anonymity)
- HTTPS enforcement + HSTS headers
- OWASP Dependency-Check with CVSS ≥ 7 build gate
- GitHub Actions pipeline: CI (build + test + coverage + mutation), SAST (CodeQL + SpotBugs), SCA, DAST (OWASP ZAP)

#### Sprint 2 — Weeks 12–15 | ⬜ In Progress

| Deliverable | Description |
|-------------|-------------|
| [Sprint 2 Overview](Deliverables/phase2/sprint2/README.md) | Delivery summary and checklist |

**Planned for Sprint 2:**
- Fix wildcard CORS (`@CrossOrigin(origins = "*")`) — replace with explicit allowlist
- Integrate `TokenBlocklist` into `JwtRequestFilter` to make logout functional
- Add `@PreAuthorize` at service layer (defence in depth beyond route-level RBAC)
- Migrate JWT from HS256 (symmetric) to RS256 (asymmetric) as per design spec
- Disable `spring.jpa.show-sql` in production profile
- Add audit logging for file operations (read, write, delete)

---

## CI/CD Pipeline Status

[![CI - Build and Test](https://github.com/mei-desofs/desofs2026_wed_ffs_3/actions/workflows/ci.yml/badge.svg)](https://github.com/mei-desofs/desofs2026_wed_ffs_3/actions/workflows/ci.yml)
[![SAST - Static Analysis](https://github.com/mei-desofs/desofs2026_wed_ffs_3/actions/workflows/sast.yml/badge.svg)](https://github.com/mei-desofs/desofs2026_wed_ffs_3/actions/workflows/sast.yml)
[![SCA - Software Composition Analysis](https://github.com/mei-desofs/desofs2026_wed_ffs_3/actions/workflows/sca.yml/badge.svg)](https://github.com/mei-desofs/desofs2026_wed_ffs_3/actions/workflows/sca.yml)

---

## Repository Structure

```
desofs2026_wed_ffs_3/
├── project/                        # Spring Boot application source
│   ├── src/main/java/              # Application code
│   ├── src/test/java/              # Tests (unit, integration, architecture)
│   ├── src/main/resources/         # application.properties
│   ├── Dockerfile                  # Multi-stage build
│   └── pom.xml                     # Maven build + security plugins
├── docker-compose.yml              # App + PostgreSQL stack
├── Deliverables/
│   ├── analysis.md                 # Phase 1 – requirements
│   ├── design.md                   # Phase 1 – architecture & DFDs
│   ├── threat_analysis.md          # Phase 1 – STRIDE threat model
│   ├── security_testing.md         # Phase 1 – security test cases
│   ├── ASVS_5_0_Tracker.xlsx       # Phase 1 – ASVS 5.0 tracker
│   └── phase2/
│       ├── sprint1/                # Sprint 1 deliverables
│       └── sprint2/                # Sprint 2 deliverables
└── .github/workflows/              # GitHub Actions pipeline definitions
```
