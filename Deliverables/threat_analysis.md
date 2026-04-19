# Threat Analysis – Coffeetaria

---

## 1. Threat Identification (STRIDE per DFD Element)

**STRIDE Legend:**
- **S** – Spoofing (fake identity)
- **T** – Tampering (modify data)
- **R** – Repudiation (deny actions)
- **I** – Information Disclosure (leak data)
- **D** – Denial of Service (crash/overload)
- **E** – Elevation of Privilege (gain unauthorized access)

**Applicable categories per element type:**

| Element Type    | S | T | R | I | D | E |
|-----------------|---|---|---|---|---|---|
| External Entity | ✅ |   | ✅ |   |   |   |
| Process         | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| Data Store      |   | ✅ |   | ✅ | ✅ |   |
| Data Flow       |   | ✅ |   | ✅ | ✅ |   |

---

### 1.1 P1 – Authentication (Process)

| ID | STRIDE | Threat Description | Abuse Case |
|----|--------|--------------------|------------|
| T01 | S | An attacker uses stolen credentials to authenticate as a legitimate user | An attacker obtains a credential list from a previous breach and performs credential stuffing against the `/auth/login` endpoint, successfully logging in as a client |
| T02 | T | An attacker tampers with the JWT payload to change their `role` or `userId` field | An authenticated client decodes the JWT, modifies the `role` field from `CLIENT` to `ADMIN`, re-encodes it and submits it to a protected endpoint |
| T03 | R | A user denies having performed a login or a sensitive action because no audit log exists | An employee logs in, deletes a dish, and later claims the action was never performed, with no evidence to contradict it |
| T04 | I | The login error response distinguishes between "email not found" and "wrong password", revealing valid account emails | An attacker enumerates all valid email addresses in the system by testing a list and observing differentiated error messages |
| T05 | D | An attacker floods the `/auth/login` endpoint with requests to lock out users or exhaust server resources | An attacker scripts thousands of login attempts per second against the authentication endpoint, making it unavailable for legitimate users |
| T06 | E | An attacker forges a valid JWT using a weak or exposed signing secret to gain admin privileges | An attacker discovers the JWT signing key in a public repository and uses it to mint a token with `role: ADMIN`, gaining full system access |

---

### 1.2 P2 – User Management (Process)

| ID | STRIDE | Threat Description | Abuse Case |
|----|--------|--------------------|------------|
| T07 | S | An attacker impersonates an admin to perform user management operations | An attacker replays a captured admin JWT (still valid) to call `PATCH /users/{id}/role` and elevate a controlled account to ADMIN |
| T08 | T | An attacker exploits a mass assignment vulnerability to modify protected user fields (e.g. role, active) | A client sends a registration request with an extra `role: ADMIN` field in the JSON body, and the API binds it directly to the User entity |
| T09 | R | An admin changes a user's role or deactivates an account with no audit trail, then denies the action | An admin demotes an employee account and later disputes having done so, with no log entry to prove otherwise |
| T10 | I | The user listing endpoint returns sensitive fields such as password hashes or internal tokens | An employee calls `GET /users` and the response includes `passwordHash` fields, which they exfiltrate |
| T11 | D | An attacker registers thousands of accounts to exhaust database storage or connection capacity | An attacker scripts a loop calling `POST /auth/register` with randomly generated emails, filling the users table |
| T12 | E | A CLIENT-role user directly calls a user management endpoint reserved for ADMINs | A client sends `DELETE /users/123` and, due to missing authorization checks at the service layer, the operation succeeds |

---

### 1.3 P3 – Menu Management (Process)

| ID | STRIDE | Threat Description | Abuse Case |
|----|--------|--------------------|------------|
| T13 | S | An attacker impersonates an employee or admin to publish unauthorized menu changes | An attacker captures an employee JWT and uses it to publish a menu with altered prices for personal gain |
| T14 | T | An attacker intercepts and modifies dish data in transit between the client and the API | An attacker performs a MITM attack on an HTTP connection and modifies the price of a dish in the request payload before it reaches the server |
| T15 | R | An employee modifies or deletes a dish and later denies the action because no change log exists | An employee reduces the price of all dishes and claims it was a system error, with no audit record to attribute the change |
| T16 | I | Ingredient stock levels or internal cost data are exposed to unauthenticated or low-privilege users | A client calls `GET /ingredients` without authorization and receives full stock and cost information intended only for staff |
| T17 | D | An attacker floods the menu listing endpoint to degrade availability for legitimate users | An attacker sends a high volume of `GET /menu` requests using multiple IPs to exhaust API server threads |
| T18 | E | A CLIENT-role user calls dish or ingredient management endpoints reserved for EMPLOYEE or ADMIN | A client sends `POST /dishes` with a crafted payload and, due to a missing role check, successfully creates a new dish |

---

### 1.4 P4 – Order Management (Process)

| ID | STRIDE | Threat Description | Abuse Case |
|----|--------|--------------------|------------|
| T19 | S | An attacker places an order using another user's identity | An attacker obtains a valid client JWT and places an order under the victim's account, associating charges to them |
| T20 | T | A client modifies the order payload (e.g. price, quantity) before submission | A client intercepts their own request and changes `unitPrice` to 0.01 before it reaches the order management process |
| T21 | R | A client denies having placed an order after consuming the item | A client disputes an order charge claiming it was never placed, and without signed order records, the claim cannot be refuted |
| T22 | I | A client accesses another client's order history via an Insecure Direct Object Reference | A client calls `GET /orders/456` where `456` belongs to another user, and the API returns the order without checking ownership |
| T23 | D | An attacker floods the order creation endpoint to exhaust server or database resources | An attacker scripts rapid POST requests to `/orders`, filling the orders table and degrading the system for all users |
| T24 | E | A CLIENT-role user calls the order status update endpoint reserved for EMPLOYEEs | A client sends `PATCH /orders/456/status` with `status: DELIVERED`, bypassing the fulfillment workflow |

---

### 1.5 P5 – Reporting (Process)

| ID | STRIDE | Threat Description | Abuse Case |
|----|--------|--------------------|------------|
| T25 | S | An attacker impersonates an admin to trigger unauthorized report generation | An attacker uses a stolen admin JWT to call `POST /reports/generate` and retrieve sensitive sales data |
| T26 | T | An attacker modifies a generated report file on the filesystem after it is written | An attacker with local access overwrites a sales report file, corrupting financial records used for auditing |
| T27 | R | An admin generates a report and denies having done so, since no audit log records the action | An admin generates a report containing sensitive data and shares it externally, later denying they triggered the export |
| T28 | I | An attacker uses a path traversal payload in the report filename parameter to read arbitrary files | An attacker calls `GET /reports?file=../../etc/passwd` and the server returns the contents of the system password file |
| T29 | D | An attacker triggers repeated large report generation to exhaust disk space or CPU | An attacker repeatedly calls the report generation endpoint with large date ranges, filling the filesystem and crashing the application |

---

### 1.6 Data Stores – DS1/DS2/DS3 (PostgreSQL)

| ID | STRIDE | Threat Description | Abuse Case |
|----|--------|--------------------|------------|
| T30 | T | An attacker exploits SQL injection to modify records in the database | An attacker submits `'; DROP TABLE orders; --` in a dish name field that is concatenated directly into a SQL query |
| T31 | I | An attacker obtains database credentials and dumps all tables | Database credentials hardcoded in a config file are discovered in a public repository, allowing the attacker to connect directly to PostgreSQL and extract all user and order data |
| T32 | D | An attacker exhausts the database connection pool to deny service | An attacker opens thousands of connections to the database directly (if exposed) or through the API, preventing legitimate queries from executing |

---

### 1.7 Data Store – DS4 (File System)

| ID | STRIDE | Threat Description | Abuse Case |
|----|--------|--------------------|------------|
| T33 | T | An attacker with filesystem access modifies audit log files to erase evidence | After compromising the server, an attacker edits the audit log file to remove entries attributing malicious actions to their account |
| T34 | I | A report file containing sensitive financial data is readable by unauthorized OS users | A report generated by the application is stored with world-readable permissions, allowing any OS user on the server to read financial data |
| T35 | D | The reports directory fills up with large files, exhausting disk space and crashing the application | An admin (or attacker impersonating one) repeatedly generates full-history reports, filling the disk and preventing new logs and reports from being written |

---

### 1.8 API Data Flows – F1 to F10

| ID | STRIDE | Threat Description | Abuse Case |
|----|--------|--------------------|------------|
| T36 | T | An attacker intercepts and modifies data in transit between client and API | An attacker performs a MITM attack on an HTTP connection and alters an order payload (e.g. changing dish IDs or quantities) before it reaches the server |
| T37 | I | An attacker intercepts unencrypted credentials transmitted over HTTP | A client submits their login credentials over HTTP (no TLS), and an attacker on the same network captures the plaintext email and password via packet sniffing |
| T38 | D | An attacker floods the network layer with requests to exhaust bandwidth or connection capacity | An attacker launches a volumetric DDoS attack targeting the API's public IP, saturating the network and making the service unreachable |

---

### 1.9 External Entities – Client / Employee / Admin

| ID | STRIDE | Threat Description | Abuse Case |
|----|--------|--------------------|------------|
| T39 | S | An attacker impersonates a legitimate user by stealing their session token | An attacker steals a JWT from a client's browser (XSS or network sniff) and uses it to place orders under the victim's identity |
| T40 | R | A user denies having performed an action (order, role change, report) with no proof of identity binding | A client places and receives an order but disputes the charge, claiming someone else used their account, and there is no request signature or strong audit trail to prove otherwise |

---

## 2. Risk Assessment

### Methodology: Likelihood × Impact Matrix

```
                     IMPACT
               Low      Medium      High
          ┌──────────┬──────────┬──────────┐
 High     │  Medium  │   High   │ Critical │
L         ├──────────┼──────────┼──────────┤
I Medium  │   Low    │  Medium  │   High   │
K         ├──────────┼──────────┼──────────┤
E Low     │   Low    │   Low    │  Medium  │
          └──────────┴──────────┴──────────┘
```

### Risk Rating Table

| ID | Threat (short name) | Likelihood | Impact | Risk Level |
|----|---------------------|------------|--------|------------|
| T01 | Credential stuffing | High | High | 🔴 Critical |
| T02 | JWT payload tampering | Medium | High | 🟠 High |
| T03 | Missing auth audit log | High | Medium | 🟠 High |
| T04 | User enumeration via error messages | High | Medium | 🟠 High |
| T05 | Login endpoint DoS / brute force | High | High | 🔴 Critical |
| T06 | JWT signing key exposure | Low | High | 🟡 Medium |
| T07 | Admin JWT replay | Low | High | 🟡 Medium |
| T08 | Mass assignment – role field | Medium | High | 🟠 High |
| T09 | No audit trail for admin actions | High | Medium | 🟠 High |
| T10 | Sensitive fields in user response | Medium | Medium | 🟡 Medium |
| T11 | Account creation flooding | High | Medium | 🟠 High |
| T12 | IDOR – user management endpoints | Medium | High | 🟠 High |
| T13 | Employee JWT reuse for menu changes | Low | Medium | 🟢 Low |
| T14 | MITM – dish data in transit | Low | Medium | 🟢 Low |
| T15 | No change log for dish modifications | High | Medium | 🟠 High |
| T16 | Ingredient data exposed to clients | Medium | Low | 🟢 Low |
| T17 | Menu endpoint DoS | Medium | Medium | 🟡 Medium |
| T18 | CLIENT accesses dish management | Medium | High | 🟠 High |
| T19 | Order placed under another identity | Low | High | 🟡 Medium |
| T20 | Order price tampering | Medium | High | 🟠 High |
| T21 | Client repudiates order | Medium | Medium | 🟡 Medium |
| T22 | IDOR – order history | High | Medium | 🟠 High |
| T23 | Order creation flooding | High | Medium | 🟠 High |
| T24 | CLIENT updates order status | Medium | Medium | 🟡 Medium |
| T25 | Report generation with stolen admin JWT | Low | High | 🟡 Medium |
| T26 | Report file tampered on filesystem | Low | Medium | 🟢 Low |
| T27 | Report generation not audited | High | Medium | 🟠 High |
| T28 | Path traversal via report filename | Medium | High | 🟠 High |
| T29 | Report generation DoS (disk exhaustion) | Medium | High | 🟠 High |
| T30 | SQL injection | Low | High | 🟡 Medium |
| T31 | Database credential exposure | Low | High | 🟡 Medium |
| T32 | Database connection pool exhaustion | Medium | High | 🟠 High |
| T33 | Audit log file tampering | Low | High | 🟡 Medium |
| T34 | Report file world-readable | Medium | Medium | 🟡 Medium |
| T35 | Disk exhaustion via reports | Medium | High | 🟠 High |
| T36 | MITM – data modification in transit | Low | High | 🟡 Medium |
| T37 | Credential interception over HTTP | Medium | High | 🟠 High |
| T38 | Network-layer DDoS | Low | High | 🟡 Medium |
| T39 | Session token theft | Medium | High | 🟠 High |
| T40 | User repudiation of actions | Medium | Medium | 🟡 Medium |

---

## 3. Mitigations

### Critical Threats

| ID | Risk | Mitigation | Links to SDR |
|----|------|------------|--------------|
| T01 | 🔴 Critical | Implement rate limiting and account lockout after N failed attempts; integrate with HaveIBeenPwned API to reject known breached passwords at registration | SDR04, SDR05 |
| T05 | 🔴 Critical | Apply rate limiting per IP and per account on `/auth/login`; return HTTP 429 after threshold; use CAPTCHA for repeated failures | SDR04 |

### High Threats

| ID | Risk | Mitigation | Links to SDR |
|----|------|------------|--------------|
| T02 | 🟠 High | Sign JWTs with RS256 (asymmetric); validate signature, expiry, and `iss`/`aud` claims on every request; never trust unsigned or user-modified tokens | SDR01, SDR03 |
| T03 | 🟠 High | Log all authentication events (login success/failure, logout) with timestamp, IP, and user ID to an append-only audit log | SDR19 |
| T04 | 🟠 High | Return a single generic error message for all login failures: `"Invalid credentials"` — never distinguish between email not found and wrong password | SDR09 |
| T08 | 🟠 High | Use strict DTOs for all request bodies; never bind request fields directly to domain entities; explicitly whitelist allowed fields | SDR07 |
| T09 | 🟠 High | Log all privileged actions (role change, user deletion, deactivation) with the acting admin's ID, timestamp, and target resource | SDR20 |
| T11 | 🟠 High | Rate-limit the `/auth/register` endpoint per IP; require email verification before account activation | SDR04 |
| T12 | 🟠 High | Enforce role-based access control at the service/application layer; return HTTP 403 for unauthorized role access regardless of HTTP route configuration | SDR02 |
| T15 | 🟠 High | Record all dish and ingredient modifications with the actor's ID, timestamp, and before/after values in the audit log | SDR20 |
| T18 | 🟠 High | Validate user role against required permission on every service method; CLIENT role must not access EMPLOYEE or ADMIN endpoints | SDR02 |
| T20 | 🟠 High | Calculate and fix order prices server-side from the current menu at the time of order placement; never trust client-supplied price values | SDR06 |
| T22 | 🟠 High | On every order retrieval, verify that the authenticated user's ID matches the order's `clientId`; employees and admins may bypass this check via explicit role validation | SDR02, SDR06 |
| T23 | 🟠 High | Rate-limit order creation per authenticated user; set a maximum number of active orders per account | SDR04 |
| T27 | 🟠 High | Log all report generation events (who, when, date range) to the audit log | SDR20 |
| T28 | 🟠 High | Validate report filename parameters against a strict allowlist pattern (e.g. `^[a-zA-Z0-9_\-]+\.csv$`); resolve the final path and verify it is within the allowed reports directory | SDR13 |
| T29 | 🟠 High | Set a maximum disk quota for the reports directory; implement retention policy to delete reports older than N days; enforce request-level rate limiting on report generation | SDR14 |
| T32 | 🟠 High | Configure a maximum database connection pool size; use connection timeouts; expose a health check endpoint to detect pool exhaustion | SDR18 |
| T35 | 🟠 High | Implement disk usage monitoring and alerts; enforce a maximum file size per report; auto-delete reports older than 30 days | SDR14 |
| T37 | 🟠 High | Enforce HTTPS-only at the reverse proxy; reject or redirect all HTTP traffic; set `Strict-Transport-Security` header | SDR10, SDR11 |
| T39 | 🟠 High | Set JWT `exp` to 1 hour; use short-lived access tokens; transmit tokens only over HTTPS; set `Secure` and `HttpOnly` flags if using cookies | SDR01, SDR10 |
