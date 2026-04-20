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
4. Validate that all High/Critical threats from the threat model have corresponding test cases

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
| TC11 | T20 | SDR06 | Place a purchase with a manipulated `unitPrice` field in the request body | Manual | Purchase created using server-side price; client-supplied price ignored |
| TC12 | T22 | SDR02, SDR06 | Authenticate as CLIENT A and call `GET /api/purchases/{externalId}` where the purchase belongs to CLIENT B | Manual | HTTP 403 returned; purchase details not disclosed |
| TC13 | T23 | SDR04 | Script 300 `POST /api/purchases` requests per minute from a single account | DAST | Rate limit triggered; HTTP 429 returned |
| TC14 | T24 | SDR02 | Authenticate as CLIENT and call `PUT /api/purchases/{id}` attempting to update the purchase status | Manual | HTTP 403 returned; status not changed |
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
| TC28 | T19 | SDR01, SDR06 | Call `POST /api/purchases` with an explicit `clientId` field in the request body referencing another user's ID | Manual | Field ignored; purchase linked to the authenticated JWT `sub`; no cross-user assignment |
| TC29 | T21 | SDR19, SDR20 | Place a purchase and inspect the API response and the audit log | Manual | Response contains a unique `requestId`; audit log contains matching entry with `sub`, IP, and timestamp |
| TC30 | T25, T27 | SDR01, SDR20 | Call `POST /reports/generate` as ADMIN and inspect the audit log | Manual | Audit log contains entry with actor `sub`, IP, timestamp, date range, and output filename |
| TC31 | T40 | SDR19, SDR20 | Perform a role-change action as ADMIN and inspect the audit log for non-repudiation fields | Manual | Audit log entry contains actor `sub`, target user ID, old role, new role, and timestamp; `requestId` matches API response |
| TC32 | T03 | SDR19 | Perform login (success), failed login, and logout; inspect the audit log for each event | Manual | Each event is logged with timestamp, IP, user ID, and outcome |
| TC33 | T09 | SDR20 | As ADMIN, perform a role change on a user account; inspect the audit log | Manual | Audit log entry contains actor `sub`, target user ID, old role, new role, and timestamp |
| TC34 | T15 | SDR20 | As EMPLOYEE, update a dish; inspect the audit log for the change record | Manual | Audit log entry records actor `sub`, dish ID, field changed, old/new values, and timestamp |
| TC35 | T32 | SDR18 | Review database connection pool configuration; verify max pool size and application behavior when pool is exhausted | Manual / Config Review | Pool limit is configured; application responds gracefully (503 or queue) rather than crashing or leaking credentials |
| TC36 | T35 | SDR14 | Verify that the reports directory has a disk quota and that a retention/cleanup policy is in place for old reports | Manual / Config Review | Quota is enforced; old reports are cleaned up automatically; disk exhaustion is prevented |
| TC37 | — | SDR13, SDR14 | Generate a report where a dish name contains a formula injection payload (e.g. `=SYSTEM("cmd")`, `=SUM(1+1)`); open the resulting CSV in a spreadsheet application | Manual | Payload is escaped (e.g. prefixed with `'`); no formula is executed when the file is opened |
| TC38 | — | SDR02 | As EMPLOYEE, attempt to transition a purchase directly from `PENDING` to `DELIVERED`, skipping intermediate states | Manual | HTTP 400 returned; only valid sequential transitions (PENDING → PREPARING → READY → DELIVERED) are accepted |
| TC39 | — | SDR06 | Attempt to place a purchase as a CLIENT whose balance is insufficient to cover the dish price; verify the database state after the rejected request | Manual | HTTP 400 returned; no purchase record created; client balance unchanged (atomic rollback) |

---

## 3. ASVS 5.0 Compliance Tracking

The full OWASP ASVS 5.0.0 compliance checklist — covering V1 through V17, with status, observations, and references per requirement — is maintained in the dedicated Excel tracker:

> **`Deliverables/ASVS_5_0_Tracker.xlsx`**

The tracker records, for each applicable ASVS requirement: compliance status (Done / Planned / N/A), project-specific observations, and links to the relevant SDR, test case, or design section.

---

## 4. Traceability Summary

| Threat ID | Risk | Test Case(s) | SDR(s) |
|-----------|------|--------------|--------|
| T01 | Critical | TC01 | SDR04, SDR05 |
| T02 | High | TC02 | SDR01, SDR03 |
| T03 | High | TC32 | SDR19 |
| T04 | High | TC03 | SDR09 |
| T05 | Critical | TC04 | SDR04 |
| T06 | Medium | TC05 | SDR01 |
| T07 | Medium | TC26 | SDR01, SDR03 |
| T08 | High | TC06 | SDR07 |
| T09 | High | TC33 | SDR20 |
| T10 | Medium | TC07 | SDR09 |
| T11 | High | TC08 | SDR04 |
| T12 | High | TC09 | SDR02 |
| T15 | High | TC34 | SDR20 |
| T17 | Medium | TC27 | SDR04 |
| T18 | High | TC10 | SDR02 |
| T19 | Medium | TC28 | SDR01, SDR06 |
| T20 | High | TC11 | SDR06 |
| T21 | Medium | TC29 | SDR19, SDR20 |
| T22 | High | TC12 | SDR02, SDR06 |
| T23 | High | TC13 | SDR04 |
| T24 | Medium | TC14 | SDR02 |
| T25, T27 | Medium | TC30 | SDR01, SDR20 |
| T28 | High | TC15 | SDR13 |
| T29 | High | TC16 | SDR14 |
| T30 | Medium | TC17 | SDR08 |
| T31 | Medium | TC18 | SDR18 |
| T32 | High | TC35 | SDR18 |
| T33 | Medium | TC19 | SDR21 |
| T34 | Medium | TC20 | SDR14 |
| T35 | High | TC36 | SDR14 |
| T37 | High | TC21 | SDR10, SDR11 |
| T39 | High | TC22 | SDR01, SDR10 |
| T40 | Medium | TC31 | SDR19, SDR20 |
| — | — | TC37 | SDR13, SDR14 |
| — | — | TC38 | SDR02 |
| — | — | TC39 | SDR06 |
