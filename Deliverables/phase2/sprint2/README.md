# Coffeetaria – Phase 2: Sprint 2 Deliverable

**Course:** Desenvolvimento de Software Seguro (DESOFS) 2025/2026
**Class:** wed_ffs | **Team:** 3 | **Repository:** desofs2026_wed_ffs_3
**Sprint period:** Week 11 → Week 15 (18/05/2026 – 16/06/2026)

**Team Members:**
- Leonardo Costa, 1250532
- Henrique Dias, 1201816
- Luís Santos, 1250534
- Lourenço Mendes, 1201270
- Nuno Oliveira, 1210939

---

## Document Index

| Document | Rubric criterion (§6.3) | Weight |
|----------|------------------------|--------|
| [Development](./development.md) | Complexity requirements (3 aggregates, 3 roles, OS ops), security controls, `@PreAuthorize`, audit logging, code reviews | 35% |
| [Security Findings & Triage](./security_findings.md) | Build & Test — SAST/SCA/DAST findings, decision per finding, accepted-risk rationale | 35% |
| [Sprint 2 Progress](./SPRINT2_PROGRESS.md) | Pipeline overview, gates, GitFlow branching, environment notes | 35% / 20% |
| [ASVS Checklist](./asvs_checklist.md) | ASVS 5.0 assessment; traceability requirement → test | 15% |
| [ASVS 5.0 Tracker](../../ASVS_5_0_Tracker.xlsx) | Full requirement-by-requirement ASVS workbook (V1–V17) | 15% |
| [CONTRIBUTING.md](../../../CONTRIBUTING.md) | Organisation — GitFlow-lite branching model and PR workflow | 5% |

> **Organisation (5%)** is addressed by this README (the single document that links every component), the consistent structure across documents, and the documented branching model.

---

## Sprint 2 — What Was Delivered

### Development (35%)

| Item | Status | Reference |
|------|--------|-----------|
| 3 DDD aggregates (User, Catalogue, Purchase) | ✅ | [development.md §1.1](./development.md#11-agregados-ddd-mínimo-3) |
| 3 roles (ADMIN / EMPLOYEE / CLIENT) | ✅ | [development.md §1.2](./development.md#12-autorização-com-3-roles) |
| OS-level file operations (`FileSystemService`) | ✅ | [development.md §1.3](./development.md#13-operações-os-no-servidor) |
| `@PreAuthorize` — method-level RBAC (defence in depth) | ✅ | [development.md §1.2](./development.md#12-autorização-com-3-roles) |
| Logout token revocation (`TokenBlocklist`) | ✅ | [development.md §2.1](./development.md#21-logout-com-revogação-de-token) |
| Audit logging for auth, file and purchase operations | ✅ | [development.md §2.9–2.10](./development.md#29-logging-de-operações-de-ficheiros) |
| Debug utilities removed from `src/main` | ✅ | [development.md](./development.md) |
| Non-root Docker user + `.dockerignore` | ✅ | [development.md §2.6–2.7](./development.md#26-dockerfile-com-utilizador-não-root) |

### Build & Test (35%)

| Item | Status | Reference |
|------|--------|-----------|
| 255 unit + integration tests passing | ✅ | [SPRINT2_PROGRESS.md](./SPRINT2_PROGRESS.md) |
| JaCoCo coverage ~91% (gate fails < 90%) | ✅ | `pom.xml` |
| Mutation testing (PITest, gate 60%) | ✅ | `pom.xml` |
| SAST — CodeQL + SpotBugs/Find Security Bugs | ✅ | [security_findings.md §1](./security_findings.md#1-sast--spotbugs--find-security-bugs) |
| SCA — OWASP Dependency-Check + Dependency Review + Dependabot | ✅ | [security_findings.md §2](./security_findings.md#2-sca--owasp-dependency-check) |
| DAST — OWASP ZAP baseline (`.zap/rules.tsv` thresholds) | ✅ | [security_findings.md §3](./security_findings.md#3-dast--owasp-zap-baseline) |
| IAST — JaCoCo TCP server agent during ZAP scan | ✅ | [security_findings.md §4](./security_findings.md#4-iast--interactive-application-security-testing-jacoco--zap) |
| Security findings triaged (93 SpotBugs → 5 fixed, 88 triaged) | ✅ | [security_findings.md](./security_findings.md) |

### Pipeline Automation (20%)

| Item | Status | Reference |
|------|--------|-----------|
| Single orchestrated pipeline (`pipeline.yml`) with `needs:` fail-fast | ✅ | [SPRINT2_PROGRESS.md §2](./SPRINT2_PROGRESS.md) |
| Branch-aware execution (PR = fast gate; push = full + DAST + mutation) | ✅ | [SPRINT2_PROGRESS.md §2](./SPRINT2_PROGRESS.md) |
| GitHub Actions hardening (SHA-pinned actions, least-privilege, concurrency) | ✅ | [SPRINT2_PROGRESS.md §2](./SPRINT2_PROGRESS.md) |
| Branch protection + GitFlow-lite (PR + review required) | ✅ | [CONTRIBUTING.md](../../../CONTRIBUTING.md) |

### ASVS (15%)

| Item | Status | Reference |
|------|--------|-----------|
| ASVS 5.0 checklist updated (Sprint 1 + Sprint 2) | ✅ | [asvs_checklist.md](./asvs_checklist.md) |
| Traceability requirement → implementation → test | ✅ | [asvs_checklist.md §3](./asvs_checklist.md) |
| Full requirement workbook | ✅ | [ASVS_5_0_Tracker.xlsx](../../ASVS_5_0_Tracker.xlsx) |

### Production (5%) & Operate (5%)

These buckets are *appreciated, not the emphasis* of the project (per §6.3). Evidence:

| Practice | Where it lives |
|----------|---------------|
| Logging & traceability | `SecurityAuditLogger` — structured audit log for auth, file and purchase events |
| Patch management | Dependabot (`.github/dependabot.yml`) — weekly Maven + Actions updates |
| Configuration management | Docker Compose + environment-variable secrets; non-root container; `.dockerignore` |
| Monitoring | `/actuator/health` (restricted; all other actuator endpoints denied) |
| Vulnerability management | OWASP Dependency-Check (CVSS ≥ 7 gate) + Dependency Review + time-boxed suppressions with expiry |
| Penetration / dynamic testing | OWASP ZAP baseline scan on every push to `develop`/`main` |

---

## Sprint 2 Checklist (Rubric §6.3)

### Development (35%)
- [x] Functionality per complexity requirements (3 aggregates, 3 roles, backend OS ops)
- [x] Well-organised code (domain encapsulation, layered architecture enforced by ArchUnit)
- [x] Consistent security controls (route-level + method-level `@PreAuthorize`)
- [x] Logging mechanisms introduced (auth, file, purchase audit events)

### Build & Test (35%)
- [x] Scripted, mostly-automated builds and tests (`mvn verify`, orchestrated pipeline)
- [x] Static analysis (CodeQL + SpotBugs)
- [x] Component analysis (OWASP Dependency-Check + Dependency Review + Dependabot)
- [x] Dynamic analysis (OWASP ZAP) with alert thresholds (`.zap/rules.tsv`)
- [x] Interactive analysis (IAST) — JaCoCo runtime instrumentation during ZAP scan
- [x] Mutation testing (PITest) and coverage gate (JaCoCo)

### Pipeline Automation (20%)
- [x] Most practices automated in a single orchestrated pipeline
- [x] Fail-fast dependencies between stages
- [x] Hardened GitHub Actions configuration

### ASVS (15%)
- [x] ASVS 5.0 assessment completeness
- [x] Traceability between documented security requirements and tests

### Production / Operate (5% + 5%)
- [x] Evidence of logging/traceability, patch management, monitoring and vulnerability management (see table above)

---

## How to Run

```bash
# Full stack (app + PostgreSQL + nginx TLS reverse proxy)
docker compose up --build

# Local build, tests and coverage gate
cd project
mvn verify

# Security tooling (local)
mvn spotbugs:check                                            # SAST
mvn dependency-check:check                                    # SCA
mvn test-compile org.pitest:pitest-maven:mutationCoverage     # mutation
```

> Use JDK 17 or 21 locally — SpotBugs 4.8.x and PITest do not run on JDK 25. The CI uses JDK 17.

---

## Branching & Contribution Model

`main` and `develop` are protected — no direct pushes. All work flows through
feature/`fix`/`docs` branches → Pull Request → 1 review + green pipeline → merge.
Full details in [CONTRIBUTING.md](../../../CONTRIBUTING.md).
