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

### Security Fixes

| Item | Status | Document |
|------|--------|----------|
| JWT secret loaded from environment variable (`JWT_SECRET`) | ✅ Done | [development.md §2](./development.md#2-security-fixes-and-practices) |
| JWT expiry reduced from 7 days → 1 hour | ✅ Done | [development.md §2](./development.md#2-security-fixes-and-practices) |
| Path traversal protection in File System API | ✅ Done | [development.md §2](./development.md#2-security-fixes-and-practices) |
| `.gitignore` updated (`.env`, secrets excluded) | ✅ Done | [development.md §2](./development.md#2-security-fixes-and-practices) |

### New Functionality

| Item | Status | Document |
|------|--------|----------|
| File System REST API (`/api/files`) | ✅ Done | [development.md §1](./development.md#1-functionality-delivered) |

### Testing & Analysis

| Item | Status | Document |
|------|--------|----------|
| 64 unit + integration tests passing | ✅ Done | [build_and_test.md §1](./build_and_test.md#1-test-results) |
| JaCoCo code coverage report | ✅ Done | [build_and_test.md §2](./build_and_test.md#2-code-coverage) |
| ArchUnit architecture tests | ✅ Done | [build_and_test.md §3](./build_and_test.md#3-architecture-tests) |
| SAST — SpotBugs + Find Security Bugs | ⬜ Planned | [build_and_test.md §4](./build_and_test.md#4-sast--static-application-security-testing) |
| SCA — OWASP Dependency-Check | ⬜ Planned | [build_and_test.md §5](./build_and_test.md#5-sca--software-composition-analysis) |
| DAST — OWASP ZAP | ⬜ Planned | [build_and_test.md §6](./build_and_test.md#6-dast--dynamic-application-security-testing) |

### Pipeline

| Item | Status | Document |
|------|--------|----------|
| GitHub Actions CI pipeline | ⬜ Planned | [pipeline.md](./pipeline.md) |
| Automated build + test | ⬜ Planned | [pipeline.md §2](./pipeline.md#2-pipeline-stages) |
| Automated SAST + SCA in CI | ⬜ Planned | [pipeline.md §2](./pipeline.md#2-pipeline-stages) |
| Automated DAST in CI | ⬜ Planned | [pipeline.md §2](./pipeline.md#2-pipeline-stages) |

---

## Sprint 1 Checklist (Rubric §6.2)

### Development (30%)
- [x] Functionality sufficient to demonstrate automation (File API, Docker, DB)
- [x] Documented set of development best practices adopted
- [x] Evidence of security fixes applied (JWT, path traversal)
- [ ] Evidence of SAST scan with results
- [ ] Evidence of SCA scan with results
- [ ] Code review evidence (PR history)

### Build and Test (30%)
- [x] Component inventory documented
- [x] Unit and integration test execution with results
- [x] Code coverage report (JaCoCo)
- [ ] Dynamic analysis (DAST/ZAP) results
- [ ] Configuration validation results
- [ ] Artifact scanning results

### Pipeline Automation (20%)
- [ ] CI pipeline defined (GitHub Actions)
- [ ] Build and test automated on every push
- [ ] SAST automated on every push
- [ ] SCA automated on every push
- [ ] DAST automated on deploy to staging

### ASVS (15%)
- [x] ASVS checklist updated with Sprint 1 implementation status
- [x] Traceability between ASVS items and security requirements (SDRs)
- [ ] ASVS items verified by automated tests in the pipeline
