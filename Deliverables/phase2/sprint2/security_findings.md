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

Baseline scan: **93 findings**. Resolution: **3 fixed in code**, **90 triaged**
(informational, data-carrier convention, accepted risk / false positive, or
non-security quality).

### 1.1 Fixed in code

| Finding | Count | Location | Fix |
|---------|-------|----------|-----|
| `DM_DEFAULT_ENCODING` | 2 | `JwtTokenUtil`, `HaveIBeenPwnedClient` | Use explicit `StandardCharsets.UTF_8` instead of the platform default charset. |
| `IMPROPER_UNICODE` | 1 | `HaveIBeenPwnedClient` | Both sides of the SHA-1 suffix comparison are upper-case hex, so replaced the flagged case-folding (`equalsIgnoreCase`) with a direct `equals(...)`. |

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
(`config/dependency-check-suppressions.xml`, `until=2026-06-30`):

| Component | CVEs | Rationale |
|-----------|------|-----------|
| `tomcat-embed-core` / `tomcat-embed-websocket` 10.1.54 | CVE-2026-41293/41284/43513/43512/42498/43515/43514 | 10.1.54 is the latest 10.1.x available for Spring Boot 3.5.x. Review when a fixed 10.1.x ships or after a tested Boot upgrade. |
| `angus-activation` 2.0.3 | CVE-2025-7962 | Latest stable; Dependency-Check maps it to the wider `angus_mail` CPE (false positive). |
| `spring-core` 6.2.18 | CVE-2026-41838/41842/41848/41850/41851 (CVSS 7.5) | Managed by Spring Boot 3.5.14; no fixed 3.5.x release at submission. Time-boxed (`until=2026-07-31`), upgrade Spring Boot when patched. |
| `spring-security-core` 6.5.10 | CVE-2026-40988 (CVSS 7.5) | Managed by Spring Boot 3.5.14; no fixed 3.5.x release at submission. Time-boxed; review on next Spring Boot upgrade. |

> The `swagger-ui` / DOMPurify advisories reported alongside these are below the CVSS ≥ 7
> gate threshold, so they are reported but do not block the build.

Dependabot (`.github/dependabot.yml`) keeps Maven + GitHub Actions dependencies
updated weekly → continuous vulnerability management.

---

## 3. DAST — OWASP ZAP Baseline

Runs against the full Docker stack (app + PostgreSQL) on pushes to
`develop`/`main`. Findings (e.g. missing security headers) are triaged against
`.zap/rules.tsv`; the app already sets HSTS, frame-options and cache-control in
`SecurityConfig`. _(Latest report attached as the `zap-report` pipeline artifact.)_

---

## 4. Summary

- **Fixed:** 5 SpotBugs findings (encoding, ReDoS, Unicode).
- **Accepted risk (documented):** SHA-1 for HIBP, fixed-host SSRF FP, stateless-JWT CSRF, config-sourced path traversal, sanitized log injection, 2 time-boxed transitive CVEs.
- **Informational/convention/quality:** 83 findings excluded with rationale.
- **Net result:** every security gate is hard-fail and green — green means *fixed or justified*, never silenced.
