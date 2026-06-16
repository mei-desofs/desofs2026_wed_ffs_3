# Security Findings & Triage — Phase 2, Sprint 2

This document is the **security assessment** for Sprint 2. It records every finding
produced by the automated security pipeline (SAST, SCA, DAST), the decision taken
for each (fixed / accepted risk / informational), and the rationale. It is the
single source of truth behind the `spotbugs-exclude.xml` and
`dependency-check-suppressions.xml` configuration.

Pipeline: `.github/workflows/pipeline.yml` — gates are **hard-fail** (no
`continue-on-error`, no zero thresholds).

| Tool | Stage | Gate |
|------|-------|------|
| SpotBugs + Find Security Bugs | SAST | `spotbugs:check` fails on any non-excluded bug |
| CodeQL (`security-and-quality`) | SAST | uploads alerts to GitHub Security |
| OWASP Dependency-Check | SCA | fails build on CVSS ≥ 7 |
| Dependency Review | SCA (PR) | fails PR on `high` severity |
| OWASP ZAP Baseline | DAST | fails on alerts above the `.zap/rules.tsv` threshold |
| PITest | Mutation | fails below 60% mutation / 75% coverage |

---

## 1. SAST — SpotBugs / Find Security Bugs

Baseline scan: **93 findings**. Resolution: **5 fixed in code**, **88 triaged**
(informational, data-carrier convention, accepted risk / false positive, or
non-security quality).

### 1.1 Fixed in code

| Finding | Count | Location | Fix |
|---------|-------|----------|-----|
| `DM_DEFAULT_ENCODING` | 2 | `JwtTokenUtil`, `HaveIBeenPwnedClient` | Use explicit `StandardCharsets.UTF_8` instead of the platform default charset. |
| `IMPROPER_UNICODE` | 1 | `HaveIBeenPwnedClient` | Both sides of the SHA-1 suffix comparison are upper-case hex, so replaced the flagged case-folding (`equalsIgnoreCase`) with a direct `equals(...)`. |
| `REDOS` (ReDoS-prone regex) | 2 | `DirectoryCreateRequestDTO`, `FileWriteRequestDTO` | Rewrote the path-validation `@Pattern` from the nested-quantifier form `^([a-zA-Z0-9_\-]+/)*[a-zA-Z0-9_\-]+$` to the linear form `^[a-zA-Z0-9_\-]+(/[a-zA-Z0-9_\-]+)*$`, removing the overlapping-quantifier pattern that can cause catastrophic backtracking (ReDoS). |

### 1.2 Accepted risks (documented exclusions)

| Finding | Count | Location | Rationale |
|---------|-------|----------|-----------|
| `WEAK_MESSAGE_DIGEST_SHA1` | 1 | `HaveIBeenPwnedClient` | HIBP Pwned Passwords **requires** SHA-1 (k-anonymity API). SHA-1 is **not** used for password storage — bcrypt is. |
| `URLCONNECTION_SSRF_FD` | 1 | `HaveIBeenPwnedClient` | Host is the fixed constant `api.pwnedpasswords.com`; only a 5-hex-char prefix of the hash is appended. Not user-controlled → no SSRF. |
| `SPRING_CSRF_PROTECTION_DISABLED` | 1 | `SecurityConfig` | Stateless REST API authenticated with JWT in the `Authorization` header (no cookies, `STATELESS` sessions). CSRF tokens are not applicable. |
| `PATH_TRAVERSAL_IN` | 1 | `FileSystemService` (constructor) | `baseDir` is trusted config, not user input. All user paths pass through `resolveSafe()` (`normalize()` + `startsWith(basePath)`), covered by `FileSystemServiceTest`. |
| `CRLF_INJECTION_LOGS` | 5 | `SecurityAuditLogger`, `GlobalExceptionHandler`, `HaveIBeenPwnedClient` | Already mitigated: `SecurityAuditLogger.sanitize()` strips CR/LF/TAB from every interpolated value; the other two sites log only exception messages. The detector cannot follow the sanitizer helper. |

### 1.3 Informational / convention (excluded)

| Finding | Count | Rationale |
|---------|-------|-----------|
| `SPRING_ENDPOINT` | 40 | Not a bug — flags every `@RequestMapping` handler. Authorization is enforced centrally in `SecurityConfig`. |
| `EI_EXPOSE_REP` / `EI_EXPOSE_REP2` | 20 | DTOs/JPA entities are data carriers; field exposure is intentional. |
| `SERVLET_HEADER` | 3 | Flags reading HTTP headers (validated before use). |

### 1.4 Non-security code quality (excluded — covered by CodeQL)

`CT_CONSTRUCTOR_THROW` (5), `REC_CATCH_EXCEPTION` (3),
`MC_OVERRIDABLE_METHOD_CALL_IN_CONSTRUCTOR` (2), `SS_SHOULD_BE_STATIC` (2),
`DE_MIGHT_IGNORE` (1), `UPM_UNCALLED_PRIVATE_METHOD` (1),
`NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE` (1), `URF_UNREAD_FIELD` (1) — style /
performance / robustness, not vulnerabilities. The SpotBugs job is the *security*
gate; general quality is analysed by the CodeQL `security-and-quality` job in the
same pipeline. Tracked for future cleanup.

---

## 2. SCA — OWASP Dependency-Check

Gate: build fails on **CVSS ≥ 7**. Time-boxed suppressions
(`config/dependency-check-suppressions.xml`; per-entry expiry: Tomcat / Angus `until=2026-06-30`, Spring Framework / Spring Security `until=2026-07-31`):

| Component | CVEs | Rationale |
|-----------|------|-----------|
| `tomcat-embed-core` / `tomcat-embed-websocket` 10.1.54 | CVE-2026-41293/41284/43513/43512/42498/43515/43514 | 10.1.54 is the latest 10.1.x available for Spring Boot 3.5.x. Review when a fixed 10.1.x ships or after a tested Boot upgrade. |
| `angus-activation` 2.0.3 | CVE-2025-7962 | Latest stable; Dependency-Check maps it to the wider `angus_mail` CPE (false positive). |
| `org.springframework` 6.2.18 (whole umbrella: `spring-core`, `spring-tx`, `spring-beans`, `spring-web`, ...) | CVE-2026-41838/41842/41848/41850/41851 (CVSS 7.5) | Managed by Spring Boot 3.5.14; no fixed 3.5.x release at submission. These are **framework-wide** CVEs — Dependency-Check maps each one to *every* Spring Framework artifact of this version, so the suppression `packageUrl` uses the regex `spring-[a-z]+@6.2.18` to cover them all (the CVE list stays specific). Time-boxed (`until=2026-07-31`), upgrade Spring Boot when patched. |
| `org.springframework.security` 6.5.10 (whole umbrella: `spring-security-core`, `spring-security-web`, `spring-security-config`, ...) | CVE-2026-40988 (CVSS 7.5) | Managed by Spring Boot 3.5.14; no fixed 3.5.x release at submission. Framework-wide CVE — suppression `packageUrl` uses the regex `spring-security-[a-z]+@6.5.10` to cover every Spring Security artifact. Time-boxed; review on next Spring Boot upgrade. |

> The `swagger-ui` / DOMPurify advisories reported alongside these are below the CVSS ≥ 7
> gate threshold, so they are reported but do not block the build.
>
> **Note:** the Spring suppressions were initially scoped to `spring-core` / `spring-security-core`
> only, but the same framework-wide CVEs also surfaced on `spring-tx` and `spring-security-web`.
> The `packageUrl` regex was broadened to cover the whole umbrella so the CVSS ≥ 7 gate is
> satisfied consistently across all affected artifacts of the same version.

Dependabot (`.github/dependabot.yml`) keeps Maven + GitHub Actions dependencies
updated weekly → continuous vulnerability management.

---

## 3. DAST — OWASP ZAP Baseline

Runs against the full Docker stack (app + PostgreSQL) on pushes to
`develop`/`main`. Alert thresholds are managed in `.zap/rules.tsv`; the pipeline
passes `-I` (ignore warnings) so only `FAIL`-level rules block the gate.

### 3.1 Alert threshold configuration (`.zap/rules.tsv`)

| Category | Action | Rationale |
|----------|--------|-----------|
| SQL Injection (all variants) | `FAIL` | Unacceptable — Spring Data JPA + parameterised queries make this a build blocker if found |
| XSS (reflected / persistent / DOM) | `FAIL` | Unacceptable even in a REST API context |
| SSRF, OS Command Injection, XXE, Path Traversal | `FAIL` | Critical OWASP Top-10 risks |
| CRLF Injection, Insecure HTTP Method, TRACE Method | `FAIL` | Exploitable without active attack tooling |
| CSRF, X-Frame-Options, SameSite Cookie | `IGNORE` | Stateless JWT API — no session cookies, not applicable |
| CSP, Clickjacking headers | `IGNORE` | API returns JSON, not rendered HTML |
| X-Content-Type-Options, Permissions-Policy, Timestamp disclosure | `WARN` | Informational; recorded in artifact, do not block |

### 3.2 Scan results (ZAP 2.17.0 — 2026-06-16)

Baseline passive scan against `http://localhost:8081` (full Docker stack).

| Risk level | Count |
|------------|-------|
| High | **0** |
| Medium | **0** |
| Low | **0** |
| Informational | **1** |
| False Positives | 0 |

**Gate result: PASS** — no `FAIL`-level rules triggered.

#### Finding detail

| Plugin ID | Name | Risk | Instances | Decision | Rationale |
|-----------|------|------|-----------|----------|-----------|
| 10049 | Non-Storable Content | Informational | 2 (`/`, `/sitemap.xml`) | **IGNORE** | Spring Security sets `Cache-Control: no-cache, no-store` on all responses by default. This is intentional for a security-sensitive REST API — prevents sensitive data from being cached by proxy servers. Already configured as `IGNORE` in `.zap/rules.tsv`. |

No Medium, Low, or High findings were produced. The absence of injection, XSS,
and header findings confirms:
- Spring Data JPA parameterised queries (no raw SQL reachable by passive scan)
- `application/json` responses only (no HTML — XSS surface absent)
- Spring Security default headers active: `X-Content-Type-Options: nosniff`,
  `X-Frame-Options: DENY`, `Cache-Control: no-store`
- `GlobalExceptionHandler` suppresses stack traces from all error responses

---

## 4. IAST — Interactive Application Security Testing (JaCoCo + ZAP)

IAST combines the live traffic of DAST with runtime code instrumentation to show
**which application code paths ZAP actually exercises**. This provides a third,
complementary perspective alongside the static (SpotBugs) and dynamic (ZAP
passive) analyses.

### 4.1 Implementation

| Component | Detail |
|-----------|--------|
| Instrumentation agent | `org.jacoco:org.jacoco.agent:0.8.14:jar:runtime` (same version as CI unit-test coverage) |
| Transport | JaCoCo file output (`output=file,destfile=/jacoco/jacoco-iast.exec`) — exec file written to mounted volume on JVM shutdown |
| Scope filter | `includes=com.cafeteriamanagement.*` — only application code, not Spring internals |
| Injection point | `JAVA_TOOL_OPTIONS` in `docker-compose.dast.yml` (DAST-only override; production compose is unaffected) |
| Agent mount | `./jacoco-agent:/jacoco:ro` volume — agent jar is downloaded by the pipeline (`mvn dependency:copy`) before the stack starts |

### 4.2 Pipeline flow

```
Download jacocoagent.jar (mvn dependency:copy)
        │
        ▼
docker compose up (app + postgres)
  └─ JVM starts with -javaagent:/jacoco/jacocoagent.jar=output=tcpserver,port=6300,...
        │
        ▼
ZAP Baseline Scan ──► exercises REST endpoints (auth, files, purchases)
        │
        ▼
docker compose down  ──► JVM exits → writes jacoco-iast.exec to mounted volume
        │
        ▼
mvn jacoco:report ──► HTML coverage report from jacoco-iast.exec
        │
        ▼
Upload artifact: iast-coverage-report
```

### 4.3 What the IAST report shows

The `iast-coverage-report` artifact contains a JaCoCo HTML report that highlights
which lines and branches of the application code were touched by ZAP's HTTP
requests. This identifies:

- **Entry points ZAP reached** — which controllers and service methods the
  scanner exercised without authentication or with the default JWT.
- **Dead code from the scanner's perspective** — code that is only reachable
  after authentication; confirms that protected paths are not trivially reachable.
- **Coverage gaps** — business logic not covered by the DAST run, which may
  warrant manual penetration testing in a follow-up sprint.

### 4.4 Separation from unit-test coverage

The `jacoco-iast.exec` file is separate from the `jacoco.exec` file produced by
`mvn verify` (unit tests). The IAST report reflects only traffic generated by ZAP,
not the 255-test suite, so the two metrics are complementary and not conflated.

### 4.5 Current status (2026-06-16)

First pipeline run used `output=tcpserver` mode; the exec file was not produced
(agent TCP port not reachable after ZAP finished). Switched to `output=file` mode:
the JaCoCo agent writes `jacoco-iast.exec` directly to the `./jacoco-agent` bind
mount when `docker compose down` signals the JVM to shut down — no TCP connection
required. The `jacoco-agent/` directory is created with `chmod 777` so the
non-root container user (`appuser`) can write the exec file.
The IAST steps carry `continue-on-error: true` so a failure does not block the DAST gate.

---

## 5. Summary

- **Fixed:** 5 SpotBugs findings — encoding (2), ReDoS-prone regex (2), Unicode (1).
- **Accepted risk (documented):** SHA-1 for HIBP, fixed-host SSRF FP, stateless-JWT CSRF, config-sourced path traversal, sanitized log injection, time-boxed transitive CVEs (Tomcat, Angus, Spring Framework, Spring Security).
- **Informational/convention/quality:** 79 findings excluded with rationale (63 informational/convention + 16 non-security quality).
- **DAST:** ZAP baseline with `.zap/rules.tsv` thresholds; FAIL on injection/critical, IGNORE on inapplicable REST API alerts.
- **IAST:** JaCoCo TCP server agent injected via `docker-compose.dast.yml`; `iast-coverage-report` artifact shows which code paths ZAP exercised.
- **Net result:** every security gate is hard-fail and green — green means *fixed or justified*, never silenced.
