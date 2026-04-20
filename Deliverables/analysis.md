# Analysis & Requirements – Coffeetaria

## System Overview

Coffeetaria is a cafeteria management backend system that allows clients to browse menus and place orders, employees to manage and fulfill orders, and admins to manage the full system including users, dishes, ingredients, and reports. The system is exposed as a REST API backed by a relational database.

**Domain Aggregates (DDD):**
- **User** – manages identity, credentials, and roles
- **Menu** – manages dishes, ingredients, and daily availability
- **Purchase** – manages the purchase lifecycle from placement to fulfillment

**Roles:**
- `CLIENT` – authenticated customer who browses and orders
- `EMPLOYEE` – cafeteria staff who manages and fulfills orders
- `ADMIN` – full system access including user and menu management

---

## User Stories

User stories capture functional intent from each role's perspective. Each story includes a security constraint to ensure that security requirements are embedded in the definition of done.

### Authentication & Session Management

| ID | Role | Story | Security Constraint |
|----|------|-------|---------------------|
| US01 | Visitor | As a visitor, I want to register with a username and password so that I can access the system as a client | Password must meet complexity rules (≥ 12 chars); duplicate usernames are rejected |
| US02 | User | As a registered user, I want to log in with my credentials so that I receive a JWT token to access protected resources | Failed attempts are counted; account is locked after repeated failures |
| US03 | User | As an authenticated user, I want to log out so that my session token is invalidated and cannot be reused | On logout the server increments the user's `tokenVersion` in the database; subsequent requests with the old token are rejected with HTTP 401 |
| US04 | Admin | As an admin, I want to deactivate a user account so that a departing employee or abusive client loses access immediately | Deactivation takes effect on the next request; existing tokens for that user are rejected |

### User Management

| ID | Role | Story | Security Constraint |
|----|------|-------|---------------------|
| US05 | Admin | As an admin, I want to list all registered users so that I can monitor system access | Response must never include password hashes, raw tokens, or other sensitive internal fields |
| US06 | Admin | As an admin, I want to assign or change a user's role so that permissions reflect current responsibilities | Role changes are logged in the audit trail with actor identity, target user, old role, and new role |
| US07 | User | As an authenticated user, I want to view and update my own profile so that my contact information stays current | Users can only access their own profile; attempting to access another user's profile returns HTTP 403 |

### Menu & Dish Management

| ID | Role | Story | Security Constraint |
|----|------|-------|---------------------|
| US08 | Admin / Employee | As an admin or employee, I want to create and manage dishes with ingredients so that the daily menu reflects the current offering | Only ADMIN and EMPLOYEE roles can create or modify dishes; CLIENT role is read-only |
| US09 | Admin / Employee | As an admin or employee, I want to publish a daily menu selecting available dishes so that clients can browse and order | Menu publication is restricted to ADMIN and EMPLOYEE; employees may only publish menus for future dates |
| US10 | Client | As a client, I want to view the current menu including allergen information so that I can make an informed choice | Allergen data is displayed for all authenticated users; no role restriction needed |

### Purchase Management

| ID | Role | Story | Security Constraint |
|----|------|-------|---------------------|
| US11 | Client | As a client, I want to place a purchase from the available menu so that my meal is reserved for collection | Purchase is linked to the authenticated user's JWT `sub`; client-supplied user IDs in the request body are ignored |
| US12 | Client | As a client, I want to view my own purchase history so that I can track past purchases | Clients can only retrieve their own purchases; IDOR attempts return HTTP 403 |
| US13 | Employee | As an employee, I want to list all pending purchases and update their status so that I can manage fulfillment efficiently | Only EMPLOYEE and ADMIN roles can change purchase status; status transitions follow the defined lifecycle |

### Reporting & File Operations

| ID | Role | Story | Security Constraint |
|----|------|-------|---------------------|
| US14 | Admin | As an admin, I want to generate a daily sales report exported to a file so that I can review revenue and order volume | Filename is validated against an allowlist pattern (`^[a-zA-Z0-9_\-]+\.csv$`); path traversal is blocked |
| US15 | Admin | As an admin, I want to retrieve a previously generated report by filename so that I can share or archive financial data | Only files within the dedicated reports directory can be served; canonical path resolution is enforced |

---

## 1. Functional Requirements

### 1.1 Authentication & Session Management

| ID | Requirement |
|----|-------------|
| FR01 | The system shall allow users to register with a unique username and password |
| FR02 | The system shall authenticate users via username and password, returning a JWT token |
| FR03 | The system shall allow users to log out, invalidating their session token |
| FR04 | The system shall enforce role-based access on all protected endpoints |
| FR05 | The system shall allow admins to deactivate or delete user accounts |
| FR05a | The system shall not ship with any default user accounts (e.g., admin/admin); all accounts must be explicitly created |
| FR05b | The system shall allow users to reset their password through a secure, authenticated flow |

### 1.2 User Management

| ID | Requirement |
|----|-------------|
| FR06 | The system shall allow admins to list all registered users |
| FR07 | The system shall allow admins to assign or change a user's role |
| FR08 | The system shall allow users to view and update their own profile |
| FR09 | The system shall allow admins to create employee accounts directly |

### 1.3 Menu & Dish Management

| ID | Requirement |
|----|-------------|
| FR10 | The system shall allow admins and employees to create dishes with name, description, price, and list of ingredients |
| FR11 | The system shall allow admins and employees to update or deactivate existing dishes |
| FR12 | The system shall allow admins and employees to manage ingredients (name, stock quantity, allergen flags) |
| FR13 | The system shall allow admins and employees to publish a daily menu selecting available dishes |
| FR14 | The system shall allow all authenticated users to view the current menu |
| FR15 | The system shall allow filtering of dishes by allergens or dietary tags |

### 1.4 Purchase Management

| ID | Requirement |
|----|-------------|
| FR16 | The system shall allow clients to place a purchase for a future date from the available menu |
| FR17 | The system shall assign a unique reference to each placed purchase |
| FR17a | The system shall maintain a pre-paid balance for CLIENT accounts; placing a purchase deducts the dish price from the client's balance |
| FR17b | The system shall reject a purchase if the client's balance is insufficient to cover the total price |
| FR17c | EMPLOYEE accounts shall not have a balance and shall not be able to place purchases |
| FR18 | The system shall allow clients to view their own purchase history |
| FR19 | The system shall allow employees to list all pending purchases |
| FR20 | The system shall allow employees to update a purchase's status (PENDING → PREPARING → READY → DELIVERED) |
| FR21 | The system shall prevent clients from placing purchases when the daily menu has no available dishes |

### 1.5 Reporting & File Operations

| ID | Requirement |
|----|-------------|
| FR22 | The system shall allow admins to generate a daily sales report exported to a file on the server filesystem |
| FR23 | The system shall allow admins to retrieve previously generated reports by filename |
| FR24 | The system shall create a dedicated output directory for reports on startup if it does not exist |
| FR25 | The system shall log all purchase state transitions to an audit log file on the server filesystem |

---

## 2. Non-Functional Requirements

### 2.1 Security

| ID | Requirement |
|----|-------------|
| NFR01 | All API communication must occur over HTTPS/TLS 1.2+ |
| NFR02 | Passwords must be stored using a strong adaptive hashing algorithm (bcrypt, Argon2) |
| NFR03 | JWT tokens must expire within a reasonable timeframe (e.g., 1 hour for access tokens) |
| NFR04 | All endpoints must enforce authorization based on the authenticated user's role |
| NFR05 | The system must sanitize and validate all input received at API boundaries |

### 2.2 Performance

| ID | Requirement |
|----|-------------|
| NFR06 | API responses for menu and order listing must complete within 500ms under normal load |
| NFR07 | The system must handle at least 100 concurrent requests without degradation |

### 2.3 Reliability & Availability

| ID | Requirement |
|----|-------------|
| NFR08 | The system must be able to restart and recover without data loss |
| NFR09 | Database transactions must ensure consistency (ACID compliance) |

### 2.4 Maintainability

| ID | Requirement |
|----|-------------|
| NFR10 | The codebase must follow a layered architecture (controller / service / repository) |
| NFR11 | All domain logic must reside in the domain layer, isolated from framework concerns |
| NFR12 | The system must be containerizable via Docker for reproducible deployments |

### 2.5 Observability

| ID | Requirement |
|----|-------------|
| NFR13 | The system must write structured logs (timestamp, level, user, action, resource) |
| NFR14 | Failed authentication attempts must be logged with IP and timestamp |
| NFR15 | All admin actions must be recorded in an audit trail |

---

## 3. Secure Development Requirements

### 3.1 Authentication & Access Control

| ID | Requirement | Justification |
|----|-------------|---------------|
| SDR01 | Implement JWT-based stateless authentication with signed tokens (RS256); explicitly reject tokens with `alg: none` or weak algorithms | Prevents token forgery, algorithm confusion, and replay attacks |
| SDR01a | The RS256 private key used to sign JWTs must be stored securely (e.g., environment variable or secrets manager), rotated periodically, and never committed to source control | Prevents long-term key compromise; limits blast radius of key exposure |
| SDR01b | All security-sensitive random values (e.g., JWT JTI, temporary passwords, activation codes) must be generated using a cryptographically secure pseudo-random number generator (CSPRNG) | Prevents predictable token/code generation that enables forgery or brute force |
| SDR02 | Enforce role-based access control (RBAC) at the service layer, not only at the route level | Defense in depth; prevents privilege escalation via indirect calls |
| SDR03 | Reject expired, malformed, or unsigned tokens with HTTP 401 | Ensures only valid sessions access protected resources |
| SDR03a | The system must invalidate JWTs on logout or account deactivation using a `tokenVersion` counter stored in the database; the JWT payload must include the version value issued at login; any request whose token version does not match the current value in the database must be rejected with HTTP 401 | Prevents reuse of valid tokens after logout or deactivation without requiring an external cache |
| SDR03b | The maximum number of concurrent active sessions per user must be defined and enforced; additional logins beyond the limit must invalidate the oldest session | Prevents session proliferation and limits blast radius of stolen tokens |
| SDR04 | Implement account lockout or rate limiting after N failed login attempts | Mitigates brute-force and credential stuffing attacks |
| SDR05 | Passwords must have a minimum length of 12 characters and a maximum of at least 64 characters; context-specific words (e.g., the application name, username) must be blocked | Reduces risk of weak credential exploitation |
| SDR05a | Authentication failure responses must use generic error messages that do not reveal whether the username or password was incorrect | Prevents user enumeration attacks |
| SDR05b | Passwords must be checked against a known breached password list during registration and password change | Prevents use of credentials exposed in prior data breaches |

### 3.2 Input Validation & Data Integrity

| ID | Requirement | Justification |
|----|-------------|---------------|
| SDR06 | Validate all request inputs against strict schemas (type, length, format, range) at the API boundary | Prevents injection attacks and unexpected application states |
| SDR07 | Reject requests with unexpected or extra fields (strict deserialization) | Prevents mass assignment vulnerabilities |
| SDR08 | Use parameterized queries or ORM abstractions for all database interactions | Prevents SQL injection |
| SDR09 | Sanitize all output returned to clients to prevent data leakage of internal fields | Avoids accidental exposure of sensitive data (e.g., password hashes, internal IDs) |

### 3.3 Secure Communication

| ID | Requirement | Justification |
|----|-------------|---------------|
| SDR10 | Enforce HTTPS-only communication with TLS 1.2 or 1.3; disable older protocol versions (TLS 1.0, 1.1, SSL); reject HTTP requests | Prevents man-in-the-middle and eavesdropping attacks |
| SDR10a | Only recommended cipher suites must be enabled; weak or deprecated ciphers (RC4, 3DES, anonymous suites) must be explicitly disabled | Prevents downgrade attacks and exploitation of weak cipher vulnerabilities |
| SDR10b | The connection between the application and the PostgreSQL database must use TLS; certificate validation must be enabled | Prevents interception of sensitive data on the internal network (even within Docker) |
| SDR11 | Set secure HTTP headers: `Strict-Transport-Security`, `X-Content-Type-Options`, `X-Frame-Options`, `Content-Security-Policy` | Reduces attack surface for common web vulnerabilities |
| SDR12 | Do not expose stack traces or internal error messages in API responses | Prevents information disclosure that aids attackers |

### 3.4 File System Operations Security

| ID | Requirement | Justification |
|----|-------------|---------------|
| SDR13 | Restrict file path inputs using allowlists; never concatenate user input directly into file paths | Prevents path traversal attacks (e.g., `../../etc/passwd`) |
| SDR14 | Generated report files must be stored in a dedicated, access-controlled directory | Prevents unauthorized access to sensitive financial data |
| SDR15 | The application must run with the minimum OS permissions required (principle of least privilege) | Limits blast radius if the process is compromised |

### 3.5 Data Security & Privacy

| ID | Requirement | Justification |
|----|-------------|---------------|
| SDR16 | Sensitive fields (passwords, tokens) must never appear in logs or API responses | Prevents credential leakage through observability tooling |
| SDR16a | Sensitive data (credentials, tokens, balance) must not be transmitted in URL query strings or path parameters; use HTTP request body or headers only | Prevents credential leakage via server logs, browser history, and proxy caches |
| SDR16b | API responses containing sensitive data (user profile, orders, balance) must include `Cache-Control: no-store` and `Pragma: no-cache` headers | Prevents sensitive data from being cached by intermediaries or client-side agents |
| SDR16c | A data retention policy must be defined specifying how long orders, audit logs, and generated reports are retained; data beyond the retention period must be securely deleted | Ensures compliance with data minimization principles and reduces exposure of stale sensitive data |
| SDR17 | Personal data (email, name) must be accessible only to the owning user or admins | Enforces data minimization and access control per user |
| SDR18 | Database connections must use credentials stored in environment variables, not hardcoded | Prevents credential exposure in source code |
| SDR18a | All application secrets (JWT private key, database credentials) must have a documented rotation schedule; rotation must not require application redeployment | Limits the window of exposure if a secret is compromised |
| SDR18b | The Docker image must not include source control metadata (e.g., `.git` folder); the `.dockerignore` file must exclude sensitive files | Prevents accidental leakage of commit history, credentials, or configuration from the container image |
| SDR18c | Spring Boot Actuator endpoints must be disabled or restricted to internal/admin access only in production; debug mode must be disabled | Prevents unintended exposure of health, env, heap-dump, and metrics endpoints to external clients |
| SDR18d | The application must not make outbound HTTP requests to user-supplied URLs; outbound connections must target only a pre-approved allowlist of hosts | Prevents Server-Side Request Forgery (SSRF) attacks |

### 3.6 Logging & Monitoring

| ID | Requirement | Justification |
|----|-------------|---------------|
| SDR19 | Log all authentication events (success, failure, logout) with timestamp and IP | Enables detection of brute-force and unauthorized access attempts |
| SDR19a | All log timestamps must be in UTC and synchronized via NTP; the logging format must be structured JSON (timestamp, level, user, action, resource, IP) | Ensures consistent log correlation and forensic accuracy across components |
| SDR19b | All user-supplied data written to logs must be sanitized to prevent log injection (e.g., strip or encode newline characters `\n`, `\r`, ANSI escape sequences) | Prevents attackers from forging log entries by injecting newlines into logged user input |
| SDR20 | Log all privileged actions (role changes, user deletion, report generation) with actor identity | Provides non-repudiation and audit capability |
| SDR20a | Failed authorization attempts (HTTP 403) must be logged with the authenticated user identity, requested resource, and timestamp | Enables detection of privilege escalation and IDOR attempts |
| SDR20b | Unexpected application errors and security control failures (e.g., TLS handshake failures, token validation errors) must be logged at ERROR level with sufficient context for incident response | Enables detection of active attacks and infrastructure failures |
| SDR21 | Log files must not be writable by the application process (append-only or external sink) | Prevents log tampering after a compromise |
| SDR21a | The application must define a global exception handler that catches all unhandled exceptions; the handler must return a generic HTTP 500 response to the client and log the full error internally | Prevents fail-open conditions and information disclosure from unhandled stack traces |

### 3.7 Third-Party Dependencies

| ID | Requirement | Justification |
|----|-------------|---------------|
| SDR22 | All third-party dependencies must be pinned to specific versions in the build manifest | Prevents unexpected breaking changes or supply chain substitution |
| SDR22a | A Software Bill of Materials (SBOM) must be generated and maintained listing all third-party libraries and transitive dependencies with their versions | Enables rapid impact assessment when a CVE is disclosed in a dependency |
| SDR22b | Risk-based remediation timeframes must be defined for vulnerable dependencies: Critical CVEs within 24h, High within 7 days, Medium within 30 days | Ensures timely response to disclosed vulnerabilities without relying on ad-hoc decisions |
| SDR23 | A Software Composition Analysis (SCA) tool must be run in CI to detect known vulnerabilities in dependencies | Ensures timely detection of CVEs in the dependency tree |
| SDR24 | Dependencies must be reviewed before addition; avoid unmaintained libraries | Reduces exposure to abandoned or compromised packages |
| SDR24a | The production Docker image must not include development tools, test dependencies, debug agents, or source code; use multi-stage builds to minimize the attack surface | Reduces the blast radius if the container is compromised |
| SDR24b | Proxies and load balancers must forward the client's original IP address via a trusted header (e.g., `X-Forwarded-For`); the application must read the IP from this trusted header for rate limiting and audit logging | Ensures accurate attribution of requests for security controls and audit trail |

---

## 4. Abuse Cases

| ID | Actor | Goal | Abuse Scenario |
|----|-------|------|----------------|
| AC01 | Unauthenticated attacker | Gain unauthorized access | Attacker performs brute-force login using a list of common passwords against registered email addresses |
| AC02 | Authenticated client | Access another user's data | Client modifies the user ID in the request to retrieve another user's purchase history |
| AC03 | Authenticated client | Escalate privileges | Client manipulates the JWT payload to change their role from CLIENT to ADMIN |
| AC04 | Authenticated employee | Place a purchase without paying | Employee directly calls the purchase creation endpoint, bypassing the client-facing flow |
| AC05 | External attacker | Inject malicious data | Attacker submits a dish name containing SQL injection payload to corrupt the database |
| AC06 | External attacker | Read server files | Attacker submits a report filename like `../../etc/passwd` to read arbitrary server files |
| AC07 | Authenticated admin | Cover tracks after malicious action | Admin deletes a user account and then attempts to modify or delete the audit log file |
| AC08 | External attacker | Deny service | Attacker floods the purchase creation endpoint with requests to exhaust server resources |
| AC09 | External attacker | Intercept credentials | Attacker performs a MITM attack on an HTTP (non-TLS) connection to capture login credentials |
| AC10 | Malicious insider | Exfiltrate data | Employee queries the full user list endpoint to extract customer PII for external use |
