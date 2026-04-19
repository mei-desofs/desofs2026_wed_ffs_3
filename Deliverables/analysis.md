# Analysis & Requirements – Coffeetaria

## System Overview

Coffeetaria is a cafeteria management backend system that allows clients to browse menus and place orders, employees to manage and fulfill orders, and admins to manage the full system including users, dishes, ingredients, and reports. The system is exposed as a REST API backed by a relational database.

**Domain Aggregates (DDD):**
- **User** – manages identity, credentials, and roles
- **Menu** – manages dishes, ingredients, and daily availability
- **Order** – manages the purchase lifecycle from placement to fulfillment

**Roles:**
- `CLIENT` – authenticated customer who browses and orders
- `EMPLOYEE` – cafeteria staff who manages and fulfills orders
- `ADMIN` – full system access including user and menu management

---

## 1. Functional Requirements

### 1.1 Authentication & Session Management

| ID | Requirement |
|----|-------------|
| FR01 | The system shall allow users to register with email, password, and name |
| FR02 | The system shall authenticate users via email and password, returning a JWT token |
| FR03 | The system shall allow users to log out, invalidating their session token |
| FR04 | The system shall enforce role-based access on all protected endpoints |
| FR05 | The system shall allow admins to deactivate or delete user accounts |

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

### 1.4 Order Management

| ID | Requirement |
|----|-------------|
| FR16 | The system shall allow clients to place an order from the current day's menu |
| FR17 | The system shall assign a unique order reference to each placed order |
| FR18 | The system shall allow clients to view their own order history |
| FR19 | The system shall allow employees to list all pending orders |
| FR20 | The system shall allow employees to update an order's status (PENDING → PREPARING → READY → DELIVERED) |
| FR21 | The system shall prevent clients from placing orders when the daily menu has no available dishes |

### 1.5 Reporting & File Operations

| ID | Requirement |
|----|-------------|
| FR22 | The system shall allow admins to generate a daily sales report exported to a file on the server filesystem |
| FR23 | The system shall allow admins to retrieve previously generated reports by filename |
| FR24 | The system shall create a dedicated output directory for reports on startup if it does not exist |
| FR25 | The system shall log all order state transitions to an audit log file on the server filesystem |

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
| SDR01 | Implement JWT-based stateless authentication with signed tokens (HS256 or RS256) | Prevents token forgery and replay attacks |
| SDR02 | Enforce role-based access control (RBAC) at the service layer, not only at the route level | Defense in depth; prevents privilege escalation via indirect calls |
| SDR03 | Reject expired, malformed, or unsigned tokens with HTTP 401 | Ensures only valid sessions access protected resources |
| SDR04 | Implement account lockout or rate limiting after N failed login attempts | Mitigates brute-force and credential stuffing attacks |
| SDR05 | Passwords must have a minimum length of 12 characters and complexity requirements | Reduces risk of weak credential exploitation |

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
| SDR10 | Enforce HTTPS-only communication; redirect or reject HTTP requests | Prevents man-in-the-middle and eavesdropping attacks |
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
| SDR17 | Personal data (email, name) must be accessible only to the owning user or admins | Enforces data minimization and access control per user |
| SDR18 | Database connections must use credentials stored in environment variables, not hardcoded | Prevents credential exposure in source code |

### 3.6 Logging & Monitoring

| ID | Requirement | Justification |
|----|-------------|---------------|
| SDR19 | Log all authentication events (success, failure, logout) with timestamp and IP | Enables detection of brute-force and unauthorized access attempts |
| SDR20 | Log all privileged actions (role changes, user deletion, report generation) with actor identity | Provides non-repudiation and audit capability |
| SDR21 | Log files must not be writable by the application process (append-only or external sink) | Prevents log tampering after a compromise |

### 3.7 Third-Party Dependencies

| ID | Requirement | Justification |
|----|-------------|---------------|
| SDR22 | All third-party dependencies must be pinned to specific versions in the build manifest | Prevents unexpected breaking changes or supply chain substitution |
| SDR23 | A Software Composition Analysis (SCA) tool must be run in CI to detect known vulnerabilities in dependencies | Ensures timely detection of CVEs in the dependency tree |
| SDR24 | Dependencies must be reviewed before addition; avoid unmaintained libraries | Reduces exposure to abandoned or compromised packages |

---

## 4. Abuse Cases

| ID | Actor | Goal | Abuse Scenario |
|----|-------|------|----------------|
| AC01 | Unauthenticated attacker | Gain unauthorized access | Attacker performs brute-force login using a list of common passwords against registered email addresses |
| AC02 | Authenticated client | Access another user's data | Client modifies the user ID in the request to retrieve another user's order history |
| AC03 | Authenticated client | Escalate privileges | Client manipulates the JWT payload to change their role from CLIENT to ADMIN |
| AC04 | Authenticated employee | Place an order without paying | Employee directly calls the order creation endpoint bypassing the client-facing flow |
| AC05 | External attacker | Inject malicious data | Attacker submits a dish name containing SQL injection payload to corrupt the database |
| AC06 | External attacker | Read server files | Attacker submits a report filename like `../../etc/passwd` to read arbitrary server files |
| AC07 | Authenticated admin | Cover tracks after malicious action | Admin deletes a user account and then attempts to modify or delete the audit log file |
| AC08 | External attacker | Deny service | Attacker floods the order creation endpoint with requests to exhaust server resources |
| AC09 | External attacker | Intercept credentials | Attacker performs a MITM attack on an HTTP (non-TLS) connection to capture login credentials |
| AC10 | Malicious insider | Exfiltrate data | Employee queries the full user list endpoint to extract customer PII for external use |
