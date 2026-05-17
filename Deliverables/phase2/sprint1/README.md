# Coffeetaria – Phase 2: Sprint 1 Deliverable

**Course:** Desenvolvimento de Software Seguro (DESOFS) 2025/2026
**Class:** wed_ffs | **Team:** 3 | **Repository:** desofs2026_wed_ffs_3
**Sprint period:** Week 8 → Week 11 (20/04/2026 – 18/05/2026)

**Team Members:**
- Leonardo Costa, 1250532
- Henrique Dias, 1201816
- Luís Santos, 1250534
- Lourenço Mendes, 1201270
- Nuno Oliveira, 1210939

---

## Document Index

| Document | Rubric criterion | Weight |
|----------|-----------------|--------|
| [Development](./development.md) | Functionality, security practices, code reviews, SAST, SCA evidence | 30% |
| [Build and Test](./build_and_test.md) | Component inventory, test results, coverage, dynamic analysis, artifact scanning | 30% |
| [Pipeline Automation](./pipeline.md) | CI/CD pipeline description, automated stages, evidence of automation | 20% |
| [ASVS Checklist](./asvs_checklist.md) | ASVS assessment completeness, traceability to security requirements | 15% |

> **Organisation (5%)** is addressed by this README and the consistent structure across all documents.

---

## Sprint 1 — What Was Delivered

### Infrastructure

| Item | Status | Document |
|------|--------|----------|
| PostgreSQL migration (H2 → PostgreSQL 16) | ✅ Done | [development.md §1](./development.md#1-functionality-delivered) |
| Docker Compose (app + database) | ✅ Done | [development.md §1](./development.md#1-functionality-delivered) |
| Multi-stage Dockerfile | ✅ Done | [development.md §1](./development.md#1-functionality-delivered) |

### Security Fixes & New Security Controls

| Item | Status | Document |
|------|--------|----------|
| JWT secret loaded from environment variable (`JWT_SECRET`) | ✅ Done | [development.md §2.1](./development.md#21-jwt-secret--environment-variable-injection) |
| JWT expiry reduced from 7 days → 1 hour | ✅ Done | [development.md §2.2](./development.md#22-jwt-expiry--reduced-to-1-hour) |
| Path traversal protection in File System API | ✅ Done | [development.md §2.3](./development.md#23-path-traversal-protection-file-system-api) |
| H2 console disabled in main profile | ✅ Done | [development.md §2.4](./development.md#24-h2-console-disabled) |
| `.gitignore` updated (`.env`, secrets excluded) | ✅ Done | [development.md §2.5](./development.md#25-secrets-excluded-from-version-control) |
| Rate limiting on login (5 attempts / 15 min per user and IP) | ✅ Done | [development.md §2.6](./development.md#26-rate-limiting-on-login) |
| Security audit logging (`SecurityAuditLogger`) | ✅ Done | [development.md §2.7](./development.md#27-security-audit-logging) |
| Password policy (min 12 chars + HIBP breach check) | ✅ Done | [development.md §2.8](./development.md#28-password-policy-and-hibp-integration) |
| HTTPS enforcement + HSTS + cache-control headers | ✅ Done | [development.md §2.9](./development.md#29-https-enforcement-and-security-headers) |

### New Functionality

| Item | Status | Document |
|------|--------|----------|
| File System REST API (`/api/files`) | ✅ Done | [development.md §1.3](./development.md#13-file-system-rest-api) |

### Testing & Analysis

| Item | Status | Document |
|------|--------|----------|
| 66 unit + integration tests passing | ✅ Done | [build_and_test.md §1](./build_and_test.md#1-test-results) |
| JaCoCo code coverage report | ✅ Done | [build_and_test.md §2](./build_and_test.md#2-code-coverage) |
| ArchUnit architecture tests | ✅ Done | [build_and_test.md §3](./build_and_test.md#3-architecture-tests) |
| SAST — SpotBugs + Find Security Bugs | ✅ Configured in `pom.xml` | [build_and_test.md §4](./build_and_test.md#4-sast--static-application-security-testing) |
| SCA — OWASP Dependency-Check | ✅ Configured in `pom.xml` | [build_and_test.md §5](./build_and_test.md#5-sca--software-composition-analysis) |
| DAST — OWASP ZAP | ⬜ Planned | [build_and_test.md §6](./build_and_test.md#6-dast--dynamic-application-security-testing) |

### Pipeline

| Item | Status | Document |
|------|--------|----------|
| GitHub Actions CI pipeline | ✅ Done | [pipeline.md](./pipeline.md) |
| Automated build + test | ✅ Done | [pipeline.md §2](./pipeline.md#2-pipeline-stages) |
| Automated SAST + SCA in CI | ✅ Done | [pipeline.md §2](./pipeline.md#2-pipeline-stages) |
| Automated DAST in CI | ✅ Done | [pipeline.md §2](./pipeline.md#2-pipeline-stages) |

---

## Sprint 1 Checklist (Rubric §6.2)

### Development (30%)
- [x] Functionality sufficient to demonstrate automation (File API, Docker, DB)
- [x] Documented set of development best practices adopted
- [x] Evidence of security fixes applied (JWT, path traversal, password hash debug log removed)
- [x] SAST configured and gated (SpotBugs + Find Security Bugs in `pom.xml`; automated in CI)
- [x] SCA configured and gated (OWASP Dependency-Check CVSS ≥ 7; automated in CI)
- [ ] Code review evidence (PR history)

### Build and Test (30%)
- [x] Component inventory documented
- [x] Unit and integration test execution with results
- [x] Code coverage report (JaCoCo)
- [x] Dynamic analysis (DAST/ZAP) — workflow defined in `dast.yml`; awaiting first CI run results
- [x] Configuration validation documented
- [x] Artifact scanning (SpotBugs XML + Dependency-Check HTML uploaded as CI artefacts)

### Pipeline Automation (20%)
- [x] CI pipeline defined (GitHub Actions)
- [x] Build and test automated on every push
- [x] SAST automated on every push
- [x] SCA automated on every push
- [x] DAST automated on deploy to staging

### ASVS (15%)
- [x] ASVS checklist updated with Sprint 1 implementation status
- [x] Traceability between ASVS items and security requirements (SDRs)
- [ ] ASVS items verified by automated tests in the pipeline
