# Security Testing Plan – Coffeetaria

---

## 1. Security Testing Methodology

### 1.1 Overview

Security testing in Coffeetaria follows a layered approach aligned with the SSDLC. Tests are derived directly from the threat model and security requirements, ensuring full traceability from threat → requirement → test.

### 1.2 Testing Techniques

| Technique | Tool(s) | When | What it covers |
|-----------|---------|------|----------------|
| **SAST** – Static Application Security Testing | SpotBugs, SonarQube, Semgrep | Every commit (CI) | Injection flaws, insecure patterns, hardcoded secrets |
| **SCA** – Software Composition Analysis | OWASP Dependency-Check | Every build (CI) | Known CVEs in third-party dependencies |
| **DAST** – Dynamic Application Security Testing | OWASP ZAP | After deployment to staging | Authentication bypass, injection, IDOR, path traversal |
| **Manual Penetration Testing** | Burp Suite, curl | Per sprint | Business logic flaws, authorization bypass, abuse cases |
| **Security Unit Tests** | JUnit + Spring Security Test | Every commit (CI) | Role enforcement, input validation, JWT validation |
| **Threat Model Review** | Manual review | End of Phase 1 | Validate mitigations cover all High/Critical threats |

### 1.3 Threat Model Review Process

1. Review DFD elements against the implemented architecture at the end of each sprint
2. Verify each High/Critical threat has a corresponding mitigation implemented or planned
3. Update the threat model if new components or data flows are introduced
4. Validate ASVS checklist items against the current implementation

---

## 2. Test Cases (Traceability Matrix)

### Threat → Requirement → Test

| Test ID | Threat | SDR | Description | Technique | Expected Result |
|---------|--------|-----|-------------|-----------|-----------------|
| TC01 | T01 | SDR04, SDR05 | Attempt credential stuffing with 100 known breached passwords against a valid account | DAST / Manual | Account locked or rate limit triggered after N attempts; HTTP 429 returned |
| TC02 | T02 | SDR01, SDR03 | Decode a valid JWT, modify the `role` field to `ADMIN`, re-encode and submit to an admin endpoint | Manual | HTTP 401 returned; tampered token rejected |
| TC03 | T04 | SDR09 | Submit login requests with a valid email + wrong password, then with an unknown email | Manual | Both return identical error message and HTTP 401 |
| TC04 | T05 | SDR04 | Send 200 login requests per second to `/auth/login` | DAST | HTTP 429 returned after threshold; service remains available for others |
| TC05 | T06 | SDR01 | Attempt to forge a JWT signed with a different key or using algorithm `none` | Manual | HTTP 401 returned; forged token rejected |
| TC06 | T08 | SDR07 | Submit a registration request with extra `role: ADMIN` field in the JSON body | Manual | Field ignored; user created with default `CLIENT` role |
| TC07 | T10 | SDR09 | Call `GET /users` as an EMPLOYEE and inspect the response body | Manual | Response contains no `passwordHash`, tokens, or other sensitive internal fields |
| TC08 | T11 | SDR04 | Script 500 POST requests to `/auth/register` with random emails | DAST | Rate limit triggered; no more than N accounts created per IP per minute |
| TC09 | T12 | SDR02 | Authenticate as CLIENT and call `DELETE /users/{id}` | Manual | HTTP 403 returned; action not performed |
| TC10 | T18 | SDR02 | Authenticate as CLIENT and call `POST /dishes` | Manual | HTTP 403 returned; dish not created |
| TC11 | T20 | SDR06 | Place an order with a manipulated `unitPrice` field in the request body | Manual | Order created using server-side price; client-supplied price ignored |
| TC12 | T22 | SDR02, SDR06 | Authenticate as CLIENT A and call `GET /orders/{id}` where the order belongs to CLIENT B | Manual | HTTP 403 returned; order details not disclosed |
| TC13 | T23 | SDR04 | Script 300 order creation requests per minute from a single account | DAST | Rate limit triggered; HTTP 429 returned |
| TC14 | T24 | SDR02 | Authenticate as CLIENT and call `PATCH /orders/{id}/status` | Manual | HTTP 403 returned; status not changed |
| TC15 | T28 | SDR13 | Call `GET /reports?file=../../etc/passwd` | Manual | HTTP 400 returned; file not read; path traversal blocked |
| TC16 | T29 | SDR14 | Trigger 50 large report generation requests in succession | Manual | Rate limit or quota enforced; disk usage bounded |
| TC17 | T30 | SDR08 | Submit SQL injection payloads in dish name, order quantity, and search fields | DAST / SAST | Inputs sanitized; no SQL error leaked; queries unaffected |
| TC18 | T31 | SDR18 | Scan source code and Docker images for hardcoded database credentials | SAST | No credentials found in code, config files, or Docker layers |
| TC19 | T33 | SDR21 | Verify audit log file permissions after application startup | Manual | Log file is not writable by the application process (append-only or external sink) |
| TC20 | T34 | SDR14 | Check filesystem permissions on generated report files | Manual | Reports are readable only by the application user; not world-readable |
| TC21 | T37 | SDR10, SDR11 | Send a login request over plain HTTP | Manual | Connection refused or redirected to HTTPS; credentials not transmitted in plaintext |
| TC22 | T39 | SDR01, SDR10 | Verify JWT expiry and that expired tokens are rejected | Manual | HTTP 401 returned for requests with expired JWT |
| TC23 | — | SDR22, SDR23 | Run OWASP Dependency-Check on the project dependencies | SCA | No HIGH or CRITICAL CVEs in dependency tree |
| TC24 | — | SDR06 | Verify input validation on all POST/PUT endpoints with invalid types, empty fields, and oversized inputs | DAST / Unit | HTTP 400 returned with validation error; no 500 errors |
| TC25 | — | SDR11 | Inspect HTTP response headers on all endpoints | Manual | `Strict-Transport-Security`, `X-Content-Type-Options`, `X-Frame-Options` present |
| TC26 | T07 | SDR01, SDR03 | Log in as ADMIN, call `POST /auth/logout`, then reuse the same JWT on a privileged endpoint | Manual | HTTP 401 returned; revoked token rejected by blocklist |
| TC27 | T17 | SDR04 | Send 500 `GET /menu` requests per minute from a single IP | DAST | Rate limit triggered; HTTP 429 returned; service remains available for other IPs |
| TC28 | T19 | SDR01, SDR06 | Place an order with an explicit `clientId` field in the request body referencing another user's ID | Manual | Field ignored; order linked to the authenticated JWT `sub`; no cross-user assignment |
| TC29 | T21 | SDR19, SDR20 | Place an order and inspect the API response and the audit log | Manual | Response contains a unique `requestId`; audit log contains matching entry with `sub`, IP, and timestamp |
| TC30 | T25, T27 | SDR01, SDR20 | Call `POST /reports/generate` as ADMIN and inspect the audit log | Manual | Audit log contains entry with actor `sub`, IP, timestamp, date range, and output filename |
| TC31 | T40 | SDR19, SDR20 | Perform a role-change action as ADMIN and inspect the audit log for non-repudiation fields | Manual | Audit log entry contains actor `sub`, target user ID, old role, new role, and timestamp; `requestId` matches API response |
| TC32 | T03 | SDR19 | Perform login (success), failed login, and logout; inspect the audit log for each event | Manual | Each event is logged with timestamp, IP, user ID, and outcome |
| TC33 | T09 | SDR20 | As ADMIN, perform a role change on a user account; inspect the audit log | Manual | Audit log entry contains actor `sub`, target user ID, old role, new role, and timestamp |
| TC34 | T15 | SDR20 | As EMPLOYEE, update a dish; inspect the audit log for the change record | Manual | Audit log entry records actor `sub`, dish ID, field changed, old/new values, and timestamp |
| TC35 | T32 | SDR18 | Review database connection pool configuration; verify max pool size and application behavior when pool is exhausted | Manual / Config Review | Pool limit is configured; application responds gracefully (503 or queue) rather than crashing or leaking credentials |
| TC36 | T35 | SDR14 | Verify that the reports directory has a disk quota and that a retention/cleanup policy is in place for old reports | Manual / Config Review | Quota is enforced; old reports are cleaned up automatically; disk exhaustion is prevented |

---

## 3. ASVS Checklist (Level 1 – Architecture Focus)

> Based on OWASP Application Security Verification Standard v5.0.0  
> Focus: Architecture, design, and threat modeling requirements (Phase 1 scope)

### V1 – Architecture, Design and Threat Modeling

| ID | Requirement | Status | Notes |
|----|-------------|--------|-------|
| V1.1.1 | Verify the use of a secure SDLC with security considered in all phases | ✅ Done | Phase 1 follows SSDLC with threat modeling and secure requirements |
| V1.1.2 | Verify the use of threat modeling for every design change | ✅ Done | STRIDE model produced per DFD element |
| V1.1.3 | Verify all user stories and features have functional security constraints | ✅ Done | Abuse cases defined in `analysis.md §4` |
| V1.1.4 | Verify documentation and justification of all application trust boundaries | ✅ Done | Trust boundaries defined in `design.md §3` |
| V1.1.5 | Verify definition and security analysis of all application components | ✅ Done | Components described in `design.md §1.3` |
| V1.1.6 | Verify a high-level architecture exists and is maintained | ✅ Done | Architecture defined in `design.md §1` |
| V1.2.1 | Verify that communications between components use secure channels | ⬜ Planned | HTTPS enforced; internal service calls via localhost |
| V1.2.2 | Verify that all components have necessary authentication | ⬜ Planned | JWT enforced on all protected endpoints |
| V1.2.3 | Verify that a single vetted authentication mechanism is used | ⬜ Planned | JWT RS256 — single auth mechanism |
| V1.2.4 | Verify that all authentication pathways use equivalent security | ⬜ Planned | Single login endpoint; no alternative auth paths |

### V2 – Authentication

| ID | Requirement | Status | Notes |
|----|-------------|--------|-------|
| V2.1.1 | Verify passwords are at least 12 characters | ⬜ Planned | SDR05 |
| V2.1.2 | Verify passwords up to 128 characters are allowed | ⬜ Planned | No arbitrary truncation |
| V2.1.3 | Verify password truncation is not performed | ⬜ Planned | |
| V2.1.6 | Verify that a "change password" feature requires current + new password | ⬜ Planned | |
| V2.1.9 | Verify no composition rules limiting characters | ⬜ Planned | |
| V2.2.1 | Verify anti-automation controls to prevent brute force attacks | ⬜ Planned | SDR04 — rate limiting |
| V2.2.2 | Verify weak passwords are rejected (HaveIBeenPwned or similar) | ⬜ Planned | SDR05 |
| V2.3.1 | Verify system-generated initial passwords are random and at least 6 chars | ⬜ Planned | |
| V2.6.1 | Verify lookup secrets are random and at least 112 bits of entropy | N/A | No lookup secrets used |
| V2.8.1 | Verify time-based OTPs have a defined lifetime | N/A | OTP not used |

### V3 – Session Management

| ID | Requirement | Status | Notes |
|----|-------------|--------|-------|
| V3.2.1 | Verify the application generates a new session token on authentication | ✅ Done | New JWT issued on each login |
| V3.2.2 | Verify session tokens have at least 64 bits of entropy | ✅ Done | JWT with UUID + RS256 |
| V3.3.1 | Verify logout invalidates the session token | ⬜ Planned | Token blacklist or short expiry strategy |
| V3.3.2 | Verify session tokens expire after inactivity | ⬜ Planned | JWT exp claim set to 1h (SDR01) |
| V3.4.1 | Verify cookie-based tokens use the Secure attribute | N/A | Stateless JWT; no cookies |

### V4 – Access Control

| ID | Requirement | Status | Notes |
|----|-------------|--------|-------|
| V4.1.1 | Verify access control rules are enforced on trusted server-side points | ⬜ Planned | SDR02 — role checks at service layer |
| V4.1.2 | Verify all user and data attributes used for access control cannot be manipulated by users | ⬜ Planned | Role sourced from JWT, not request body |
| V4.1.3 | Verify the principle of least privilege exists | ⬜ Planned | SDR15 — minimal OS and role permissions |
| V4.1.5 | Verify access control failures are logged | ⬜ Planned | SDR19 — all 403s logged with user and resource |
| V4.2.1 | Verify all user and data attributes and policy information used for access control cannot be manipulated by end users | ⬜ Planned | SDR02, SDR07 |
| V4.2.2 | Verify CSRF protection is implemented | N/A | Stateless JWT API; no session cookies |
| V4.3.1 | Verify admin interfaces use appropriate authentication and authorization | ⬜ Planned | Admin endpoints protected by ADMIN role JWT |

### V5 – Validation, Sanitization and Encoding

| ID | Requirement | Status | Notes |
|----|-------------|--------|-------|
| V5.1.1 | Verify that the application has defenses against HTTP parameter pollution attacks | ⬜ Planned | Spring Boot rejects duplicate params by default |
| V5.1.2 | Verify that frameworks protect against mass parameter assignment attacks | ⬜ Planned | SDR07 — strict DTOs |
| V5.1.3 | Verify all input is validated against an allowlist | ⬜ Planned | SDR06 — schema validation at API boundary |
| V5.1.4 | Verify that structured data is strongly typed | ⬜ Planned | |
| V5.2.1 | Verify all untrusted HTML input is sanitized | N/A | API returns JSON; no HTML rendering |
| V5.3.4 | Verify data selection or database queries use parameterized queries | ⬜ Planned | SDR08 — ORM with parameterized queries |
| V5.3.8 | Verify protection against OS command injection | ⬜ Planned | SDR13 — no user input passed to OS commands |

### V7 – Error Handling and Logging

| ID | Requirement | Status | Notes |
|----|-------------|--------|-------|
| V7.1.1 | Verify no sensitive credentials or PII in logs | ⬜ Planned | SDR16 |
| V7.1.2 | Verify no sensitive data in error messages returned to clients | ⬜ Planned | SDR12 — generic error responses |
| V7.2.1 | Verify all authentication decisions are logged | ⬜ Planned | SDR19 |
| V7.2.2 | Verify all access control failures are logged | ⬜ Planned | SDR19 |
| V7.3.1 | Verify logs are protected from injection | ⬜ Planned | Structured logging with no string concatenation |
| V7.4.1 | Verify a generic message is shown on unexpected errors | ⬜ Planned | SDR12 |

### V8 – Data Protection

| ID | Requirement | Status | Notes |
|----|-------------|--------|-------|
| V8.1.1 | Verify the application protects sensitive data from being cached | ⬜ Planned | `Cache-Control: no-store` on sensitive endpoints |
| V8.2.1 | Verify all cached or temporary copies of sensitive data are protected | ⬜ Planned | |
| V8.3.1 | Verify sensitive data is not sent in URL parameters | ⬜ Planned | Credentials only in request body |
| V8.3.4 | Verify all sensitive data created and processed by the application is identified | ✅ Done | Identified in `analysis.md`: passwords, tokens, PII |

### V9 – Communication

| ID | Requirement | Status | Notes |
|----|-------------|--------|-------|
| V9.1.1 | Verify TLS is used for all client connectivity | ⬜ Planned | SDR10 — HTTPS enforced |
| V9.1.2 | Verify TLS 1.2 or higher is used | ⬜ Planned | SDR10 |
| V9.1.3 | Verify only the latest TLS cipher suites are enabled | ⬜ Planned | Configured at reverse proxy level |
| V9.2.1 | Verify that connections from the server use trusted certificates | ⬜ Planned | |

### V12 – Files and Resources

| ID | Requirement | Status | Notes |
|----|-------------|--------|-------|
| V12.1.1 | Verify that the application will not accept large files that could fill storage | ⬜ Planned | SDR14 — quota on reports directory |
| V12.3.1 | Verify user-submitted filenames are not used directly in file operations | ⬜ Planned | SDR13 — allowlist validation on filenames |
| V12.3.2 | Verify user-submitted filenames are validated against an allowlist | ⬜ Planned | SDR13 |
| V12.3.3 | Verify protection against path traversal | ⬜ Planned | SDR13 — path canonicalization and prefix check |
| V12.5.1 | Verify the web tier is configured to serve only files with permitted extensions | ⬜ Planned | Only `.csv` and `.log` extensions allowed |

### V13 – API and Web Service

| ID | Requirement | Status | Notes |
|----|-------------|--------|-------|
| V13.1.1 | Verify all application components use the same encoding and parsers | ⬜ Planned | UTF-8 enforced throughout |
| V13.1.2 | Verify API URLs do not expose sensitive information | ⬜ Planned | No credentials or tokens in URLs |
| V13.1.3 | Verify HTTP methods are validated against allowlist per endpoint | ⬜ Planned | Spring `@RequestMapping` restricts methods |
| V13.2.1 | Verify enabled RESTful methods are a valid choice | ⬜ Planned | Only GET/POST/PATCH/DELETE used where appropriate |
| V13.2.3 | Verify RESTful services are protected against CSRF | N/A | Stateless JWT; no cookies |

### V15 – Secure Coding and Architecture

| ID | Requirement | Status | Notes |
|----|-------------|--------|-------|
| V15.2.2 | Verify that the application has defenses against resource exhaustion from expensive or long-running operations | ⬜ Planned | SDR14 — quota on reports directory; TC36 |

### V16 – Security Logging and Error Handling

| ID | Requirement | Status | Notes |
|----|-------------|--------|-------|
| V16.3.1 | Verify that all authentication operations (login, failed login, logout) are logged with sufficient detail | ⬜ Planned | SDR19 — TC32 |
| V16.3.2 | Verify that access control failures are logged with sufficient detail to identify the user and the denied resource | ⬜ Planned | SDR19 — all 403s logged with `sub` and resource |
| V16.3.3 | Verify that all security-relevant events are logged, including privileged actions such as role changes and report generation | ⬜ Planned | SDR20 — TC33, TC34, TC30 |
| V16.4.2 | Verify that log entries cannot be injected by external parties through structured log formatting | ⬜ Planned | Structured logging; no raw string concatenation |

---

## 4. Traceability Summary

| Threat ID | Risk | Test Case(s) | SDR(s) | ASVS |
|-----------|------|--------------|--------|------|
| T01 | Critical | TC01 | SDR04, SDR05 | V2.2.1, V2.2.2 |
| T02 | High | TC02 | SDR01, SDR03 | V3.2.1 |
| T04 | High | TC03 | SDR09 | V7.4.1 |
| T05 | Critical | TC04 | SDR04 | V2.2.1 |
| T06 | Medium | TC05 | SDR01 | V3.2.2 |
| T08 | High | TC06 | SDR07 | V5.1.2 |
| T10 | Medium | TC07 | SDR09 | V8.3.4 |
| T11 | High | TC08 | SDR04 | V2.2.1 |
| T12 | High | TC09 | SDR02 | V4.1.1 |
| T18 | High | TC10 | SDR02 | V4.1.1 |
| T20 | High | TC11 | SDR06 | V5.1.3 |
| T22 | High | TC12 | SDR02, SDR06 | V4.1.1, V4.2.1 |
| T23 | High | TC13 | SDR04 | V2.2.1 |
| T24 | Medium | TC14 | SDR02 | V4.1.1 |
| T28 | High | TC15 | SDR13 | V12.3.2, V12.3.3 |
| T29 | High | TC16 | SDR14 | V12.1.1 |
| T30 | Medium | TC17 | SDR08 | V5.3.4 |
| T31 | Medium | TC18 | SDR18 | — |
| T33 | Medium | TC19 | SDR21 | V7.3.1 |
| T34 | Medium | TC20 | SDR14 | V12.5.1 |
| T37 | High | TC21 | SDR10, SDR11 | V9.1.1 |
| T39 | High | TC22 | SDR01, SDR10 | V3.3.2 |
| T07 | Medium | TC26 | SDR01, SDR03 | V3.3.1 |
| T17 | Medium | TC27 | SDR04 | V2.2.1 |
| T19 | Medium | TC28 | SDR01, SDR06 | V4.1.2 |
| T21 | Medium | TC29 | SDR19, SDR20 | V7.2.1 |
| T25 | Medium | TC30 | SDR01, SDR20 | V7.2.1 |
| T40 | Medium | TC31 | SDR19, SDR20 | V7.2.1, V7.2.2 |
| T03 | High | TC32 | SDR19 | V16.3.1 |
| T09 | High | TC33 | SDR20 | V16.3.3 |
| T15 | High | TC34 | SDR20 | V16.3.3 |
| T27 | High | TC30 | SDR01, SDR20 | V16.3.3 |
| T32 | High | TC35 | SDR18 | V13.1.2 |
| T35 | High | TC36 | SDR14 | V15.2.2 |
