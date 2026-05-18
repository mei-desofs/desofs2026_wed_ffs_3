# ASVS Checklist – Phase 2 Sprint 1

> **Primary document:** [`Deliverables/ASVS_5_0_Tracker.xlsx`](../../ASVS_5_0_Tracker.xlsx)
>
> The OWASP ASVS 5.0 compliance tracker is maintained as an Excel workbook with full traceability between requirements, implementation status, observations, and references to SDRs/FRs. It covers all 345 requirements across 17 chapters (V1–V17).
>
> This file provides a human-readable summary of the Sprint 1 findings. For the authoritative status of each requirement, refer to the Excel tracker above.

---

## Summary – Sprint 1 ASVS Coverage

| Chapter | Compliant | In Progress | Not Applicable | Not Started |
|---------|-----------|-------------|----------------|-------------|
| V5 – File Handling | 3 | 0 | 5 | 5 |
| V6 – Authentication | 7 | 0 | 19 | 21 |
| V7 – Session Management | 3 | 0 | 2 | 14 |
| V8 – Authorization | 1 | 1 | 2 | 9 |
| V9 – Self-contained Tokens | 1 | 2 | 0 | 4 |
| V12 – Secure Communication | 3 | 0 | 2 | 7 |
| V13 – Configuration | 3 | 0 | 1 | 17 |
| V15 – Secure Coding | 3 | 0 | 1 | 17 |
| V16 – Security Logging | 7 | 0 | 0 | 10 |

---

## Sprint 1 — Key Controls Implemented

### V6 – Authentication (7 Compliant)

| Req | Description | Implementation |
|-----|-------------|----------------|
| V6.2.1 | Passwords ≥ 12 characters | `@Size(min=12)` in `UserDTO` + `LoginRequestDTO`; `PasswordPolicyService` min=12 |
| V6.2.4 | Breached password rejection | `HaveIBeenPwnedClient` — k-anonymity SHA-1 query to HIBP API |
| V6.2.8 | Correct password comparison | `BCryptPasswordEncoder.matches()` in `AuthController` |
| V6.2.9 | No maximum length truncation | `@Size(max=128)` enforced; passwords up to 128 chars accepted |
| V6.3.1 | Anti-brute-force (rate limiting) | `SimpleRateLimiter`: 5 attempts / 15 min per username and IP |
| V6.3.2 | No default credentials in production | Default user INSERTs removed from `data.sql` (ASVS V6.3.2) |
| V6.3.8 | Generic authentication error message | `AuthController` always returns "Invalid credentials" — no user enumeration |

### V7 – Session Management (3 Compliant)

| Req | Description | Implementation |
|-----|-------------|----------------|
| V7.3.1 | Absolute session timeout | `jwt.expiration=3600` (1 hour); validated on every request |
| V7.3.2 | Session timeout documented and enforced | Configured in `application.properties`; checked in `JwtRequestFilter` |
| V7.4.1 | Logout invalidates token | `POST /api/auth/logout` blocks token in `TokenBlocklist`; `JwtRequestFilter` checks blocklist |

### V12 – Secure Communication (3 Compliant)

| Req | Description | Implementation |
|-----|-------------|----------------|
| V12.1.1 | TLS enforced | nginx reverse proxy with TLS 1.2+ (`deploy/nginx/conf.d/cafeteria.conf`) |
| V12.1.2 | Strong cipher suites | `ssl_ciphers` configured in nginx; RC4/3DES excluded |
| V12.2.1 | HTTP rejected / redirected | nginx port 80 → HTTPS redirect; app internal only |

### V13 – Configuration (3 Compliant)

| Req | Description | Implementation |
|-----|-------------|----------------|
| V13.4.1 | `.dockerignore` excludes `.git` | `project/.dockerignore` created; excludes `.git`, `target/`, `.env*` |
| V13.4.2 | No stack traces in responses | `GlobalExceptionHandler` returns generic message; full trace logged via SLF4J |
| V13.4.5 | Actuator endpoints restricted | `management.endpoints.web.exposure.include=health`; `/actuator/**` denied in `SecurityConfig` |

### V16 – Security Logging and Error Handling (7 Compliant)

| Req | Description | Implementation |
|-----|-------------|----------------|
| V16.2.5 | No credentials/tokens in logs | Debug `println` removed from `CustomUserDetailsService`; HIBP errors use `log.warn` |
| V16.3.1 | Authentication events logged | `SecurityAuditLogger`: IP, timestamp, username, method, path on every auth event |
| V16.3.2 | HTTP 403 events logged | `logAccessDenied()` in `SecurityConfig` and `GlobalExceptionHandler` |
| V16.4.1 | Log injection prevention | `SecurityAuditLogger.sanitize()` strips `\r\n\t` from all logged values |
| V16.5.1 | No internal errors in responses | Generic "An unexpected error occurred" — `ex.getMessage()` never exposed |
| V16.5.3 | Global exception handler | `@RestControllerAdvice GlobalExceptionHandler` handles all exception types |
| V16.5.4 | Catch-all exception handler | `@ExceptionHandler(Exception.class)` defined in `GlobalExceptionHandler` |

---

## 3. ASVS to Automated Test Traceability

Each Sprint 1 security control has at least one automated test that runs on every CI pipeline execution. The mapping below links each ASVS / SDR requirement to the concrete test class and method that exercises it; all tests are part of the `mvn verify` invocation in [`ci.yml`](../../../.github/workflows/ci.yml).

### 3.1 New Sprint 1 unit tests

| ASVS Req | SDR / FR | Automated Test |
|----------|----------|----------------|
| V6.2.1 (min 12 chars) | SDR05 | `PasswordPolicyServiceTest.rejectsPasswordShorterThanMinimum`, `acceptsPasswordExactlyAtMinimum` |
| V6.2.9 (no truncation) | SDR05 | `PasswordPolicyServiceTest.acceptsPasswordExactlyAtMaximum`, `rejectsPasswordLongerThanMaximum` |
| V6.3.1 (anti-automation) | SDR04 | `SimpleRateLimiterTest.fifthFailureBlocksKey`, `additionalFailuresKeepKeyBlocked`, `differentKeysAreTrackedIndependently`, `resetUnblocksKey` |
| V7.4.1 (logout invalidates token) | FR03, SDR03a | `TokenBlocklistTest.blockedTokenReturnsTrue`, `expiredEntryIsNoLongerBlocked`, `blockingSameTokenTwiceIsIdempotent` |
| V16.3.1 (authn events logged) | SDR19 | `SecurityAuditLoggerTest.authenticationSuccessIsLoggedAtInfoLevel`, `authenticationFailureIsLoggedAtWarnLevel`, `authenticationBlockedIsLoggedAtWarnLevel`, `tokenAuthenticationFailureUsesBearerTokenPrincipal` |
| V16.4.1 (log injection prevention) | SDR19b | `SecurityAuditLoggerTest.logInjectionAttemptIsSanitised`, `nullUsernameIsReplacedWithUnknown` |

### 3.2 Existing integration and architecture tests

| ASVS Req | SDR / FR | Automated Test |
|----------|----------|----------------|
| V6.3.2 (no default creds) | SDR18 | Verified: `data.sql` contains no `INSERT INTO users` |
| V7.3.1 (session timeout) | SDR01 | `AuthenticationIntegrationTest.testProtectedEndpointRequiresJwt` |
| V12.2.1 (HTTPS redirect) | SDR10 | `SecureChannelIntegrationTest.testHttpsRedirect` |
| V12.2.x (X-Forwarded-Proto trust) | SDR10 | `SecureChannelIntegrationTest.testXForwardedProtoHttpsAccepted` |
| V13.4.5 (actuator restriction) | SDR18c | Verified: `management.endpoints.web.exposure.include=health` |
| V14.x (layered architecture) | — | `ArchitectureTest.controller_dont_access_repos` and 3 other ArchUnit rules |
| V16.5.1 (no error leak) | SDR12 | `GlobalExceptionHandler` covered by controller unit tests |

### 3.3 Pipeline-level verification (no unit test but enforced in CI)

| ASVS Req | SDR | Pipeline gate |
|----------|-----|---------------|
| V14.2.1 (no known-vulnerable deps) | SDR22 | OWASP Dependency-Check in [`sca.yml`](../../../.github/workflows/sca.yml) — fails build on CVSS ≥ 7 |
| V5.x (SAST signals on code patterns) | — | CodeQL + SpotBugs/FindSecBugs in [`sast.yml`](../../../.github/workflows/sast.yml) |
| V14.2 (DAST on running app) | — | OWASP ZAP baseline in [`dast.yml`](../../../.github/workflows/dast.yml) |

---

> For the full requirement-by-requirement breakdown, open [`ASVS_5_0_Tracker.xlsx`](../../ASVS_5_0_Tracker.xlsx).
