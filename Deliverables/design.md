# Design – Coffeetaria

---

## 1. System Overview & Architecture

Coffeetaria is a backend REST API application following a layered architecture based on Domain-Driven Design (DDD). It exposes HTTP endpoints consumed by clients (web/mobile frontends or direct API consumers) and persists data in a relational database. The system also interacts with the server filesystem for report generation and audit logging.

### 1.1 Architectural Layers

> [`diagrams/src/architecture.puml`](./diagrams/src/architecture.puml) · [`diagrams/img/Architecture_Coffeetaria.png`](./diagrams/img/Architecture_Coffeetaria.png)

| Layer | Responsibility |
|-------|---------------|
| API / Controller | HTTP request handling, input validation, authentication enforcement |
| Application | Use cases, orchestration, authorization checks |
| Domain | Aggregates, entities, value objects, domain business rules |
| Infrastructure | Database (ORM), filesystem access, external services |

### 1.2 Technology Stack

| Component | Technology |
|-----------|------------|
| Language | Java 21 |
| Framework | Spring Boot 3 |
| Database | PostgreSQL |
| Authentication | JWT (RS256) |
| Containerization | Docker / Docker Compose |
| Build tool | Maven |

### 1.3 System Components

| Component | Description |
|-----------|-------------|
| REST API | Exposes all business operations as HTTP endpoints |
| Auth Module | Handles registration, login, JWT issuance and validation |
| User Module | Manages user accounts and role assignments |
| Menu Module | Manages ingredients, dishes, and daily menus |
| Order Module | Handles order lifecycle from placement to delivery |
| Report Module | Generates daily sales reports written to the filesystem |
| Audit Logger | Writes security and order events to append-only log files |

---

## 2. Domain Model

The domain is organized into three aggregates following DDD principles.

| Aggregate | Source | Image |
|-----------|--------|-------|
| User | [`src/domain_user.puml`](./diagrams/src/domain_user.puml) | [`img/Domain_User_Aggregate.png`](./diagrams/img/Domain_User_Aggregate.png) |
| Menu | [`src/domain_menu.puml`](./diagrams/src/domain_menu.puml) | [`img/Domain_Menu_Aggregate.png`](./diagrams/img/Domain_Menu_Aggregate.png) |
| Purchase | [`src/domain_purchase.puml`](./diagrams/src/domain_purchase.puml) | [`img/Domain_Purchase_Aggregate.png`](./diagrams/img/Domain_Purchase_Aggregate.png) |

### Aggregates Summary

| Aggregate | Root | Entities | Value Objects |
|-----------|------|----------|---------------|
| User | `User` | — | `UserType` |
| Menu | `Menu` | `Dish`, `Ingredient` | `Name`, `IngredientType`, `Allergen` |
| Purchase | `Purchase` | — | — |

**Roles:** `CLIENT` · `EMPLOYEE` · `ADMIN`

**Order lifecycle:** `PENDING` → `PREPARING` → `READY` → `DELIVERED`

---

## 3. Data Flow Diagrams

### 3.1 DFD Level 0 – Context Diagram

> **Diagram:** [`diagrams/src/dfd_level0.puml`](./diagrams/src/dfd_level0.puml)

![DFD Level 0](./diagrams/img/DFD_Level0_Coffeetaria.png)

**Trust Boundaries:**
- All external entities (Client, Employee, Admin) are **outside** the trust boundary
- The system trusts only requests bearing a valid signed JWT
- The filesystem and database are **inside** the trust boundary (internal infrastructure)

---

### 3.2 DFD Level 1 – Internal Processes

> **Diagram:** [`diagrams/src/dfd_level1.puml`](./diagrams/src/dfd_level1.puml)

![DFD Level 1](./diagrams/img/DFD_Level1_Coffeetaria.png)

**Processes:**

| ID | Process | Description |
|----|---------|-------------|
| P1 | Authentication | Login, registration, JWT issuance and validation |
| P2 | User Management | CRUD user accounts, role assignment (Admin only) |
| P3 | Menu Management | Manage dishes, ingredients and daily menu publication |
| P4 | Order Management | Place orders (Client), fulfill and track orders (Employee) |
| P5 | Reporting | Generate sales reports and write audit log entries to filesystem |

**Data Stores:**

| ID | Store | Technology |
|----|-------|------------|
| DS1 | Users | PostgreSQL |
| DS2 | Menu, Dishes, Ingredients | PostgreSQL |
| DS3 | Orders | PostgreSQL |
| DS4 | Reports & Audit Logs | Filesystem |

**Data Flows:**

| Flow | From | To | Data | Protocol |
|------|------|----|------|----------|
| F1 | Client / Employee / Admin | P1. Auth | Credentials (email, password) | HTTPS |
| F2 | P1. Auth | Client / Employee / Admin | JWT Access Token | HTTPS |
| F3 | Client | P3. Menu Mgmt | Browse menu request | HTTPS + JWT |
| F4 | Client | P4. Order Mgmt | Order placement (dish IDs, quantities) | HTTPS + JWT |
| F5 | Employee | P4. Order Mgmt | Order status update | HTTPS + JWT |
| F6 | Admin | P2. User Mgmt | User CRUD, role assignment | HTTPS + JWT |
| F7 | Admin | P3. Menu Mgmt | Dish / ingredient management | HTTPS + JWT |
| F8 | Admin | P5. Reporting | Report generation request | HTTPS + JWT |
| F9 | P4. Order Mgmt | DS4. File System | Audit log entries (append-only) | Internal |
| F10 | P5. Reporting | DS4. File System | Daily sales report (write) | Internal |

### 3.3 DFD Level 2 – P5 Reporting Process

The Reporting process interacts with the filesystem and multiple data stores, making it the highest-risk process in terms of path traversal, information disclosure, and audit trail integrity. A Level 2 decomposition is therefore justified.

> **Diagram:** [`diagrams/src/dfd_level2_reporting.puml`](./diagrams/src/dfd_level2_reporting.puml)

**Sub-processes:**

| ID | Sub-process | Description |
|----|-------------|-------------|
| P5.1 | Authorise Request | Validates the JWT and confirms the caller holds the `ADMIN` role |
| P5.2 | Validate Parameters | Enforces date-range bounds and validates the filename against an allowlist pattern (`^[a-zA-Z0-9_\-]+\.csv$`) |
| P5.3 | Query Order Data | Reads order records from DS3 for the validated date range |
| P5.4 | Format Report (CSV) | Aggregates and serialises order data into CSV; triggers the audit event |
| P5.5 | Write Report File | Resolves the canonical path, confirms it is within the reports directory, writes with permissions `600` |
| P5.6 | Log Audit Event | Appends an entry to DS4 (audit log) recording actor `sub`, IP, timestamp, date range, and output filename |

**Trust boundary note:** All sub-processes are inside the trust boundary. The Admin is the only external entity. DS1 is read only to validate the JWT role; DS3 is read-only for order data; DS4 is write-only (append) for both the report file and the audit log.

**Key data flows:**

| Flow | From | To | Data |
|------|------|----|------|
| F-R1 | Admin | P5.1 | JWT + date range + filename [HTTPS] |
| F-R2 | P5.1 | DS1 | Read user role for JWT validation |
| F-R3 | P5.1 | P5.2 | Validated identity + raw params |
| F-R4 | P5.2 | P5.3 | Sanitised query params |
| F-R5 | P5.3 | DS3 | Read orders for date range |
| F-R6 | P5.3 | P5.4 | Raw order records |
| F-R7 | P5.4 | P5.5 | Formatted CSV content |
| F-R8 | P5.4 | P5.6 | Audit event (actor, params, filename) |
| F-R9 | P5.5 | DS4 | Write report file (restricted path, perms 600) |
| F-R10 | P5.6 | DS4 | Append audit log entry |
| F-R11 | P5.5 | Admin | HTTP 200 + confirmed filename [HTTPS] |

---

## 4. Database Schema

> [`diagrams/src/db_schema.puml`](./diagrams/src/db_schema.puml) · [`diagrams/img/DB_Schema_Coffeetaria.png`](./diagrams/img/DB_Schema_Coffeetaria.png)

| Table | Description |
|-------|-------------|
| `users` | User accounts with role and optional pre-paid balance |
| `dishes` | Cafeteria dishes with name and price |
| `ingredients` | Ingredients with type and allergen classification |
| `dish_ingredients` | Many-to-many join between dishes and ingredients |
| `menus` | Daily menus with references to meat, fish and vegetarian dishes |
| `purchases` | Client purchase records linking user, dish and date |

---

## 5. Authentication Flow

> [`diagrams/src/auth_flow.puml`](./diagrams/src/auth_flow.puml) · [`diagrams/img/Auth_Flow_Coffeetaria.png`](./diagrams/img/Auth_Flow_Coffeetaria.png)

---

## 6. API Design

### 6.1 Authentication

| Method | Endpoint | Auth | Roles | Description |
|--------|----------|------|-------|-------------|
| POST | `/api/auth/login` | None | Public | Authenticate with email + password; returns JWT |
| POST | `/api/auth/register` | None | Public | Register a new user account (default role: CLIENT) |

### 6.2 User Management

| Method | Endpoint | Auth | Roles | Description |
|--------|----------|------|-------|-------------|
| GET | `/api/users` | JWT | ADMIN | List all users |
| GET | `/api/users/{id}` | JWT | ADMIN | Get user by ID |
| GET | `/api/users/by-username/{username}` | JWT | ADMIN | Get user by username |
| POST | `/api/users` | JWT | ADMIN | Create a new user |
| PUT | `/api/users/{id}` | JWT | ADMIN | Update user by ID |
| DELETE | `/api/users/{id}` | JWT | ADMIN | Delete user by ID |
| GET | `/api/users/me` | JWT | ADMIN, CLIENT | Get own profile |
| PUT | `/api/users/me` | JWT | ADMIN, CLIENT | Update own profile |

### 6.3 Dish Management

| Method | Endpoint | Auth | Roles | Description |
|--------|----------|------|-------|-------------|
| GET | `/api/dishes` | JWT | ADMIN, EMPLOYEE, CLIENT | List all dishes |
| GET | `/api/dishes/{id}` | JWT | ADMIN, EMPLOYEE, CLIENT | Get dish by ID |
| POST | `/api/dishes` | JWT | ADMIN, EMPLOYEE | Create a new dish |
| PUT | `/api/dishes/{id}` | JWT | ADMIN, EMPLOYEE | Update dish by ID |
| DELETE | `/api/dishes/{id}` | JWT | ADMIN, EMPLOYEE | Delete dish by ID |

### 6.4 Ingredient Management

| Method | Endpoint | Auth | Roles | Description |
|--------|----------|------|-------|-------------|
| GET | `/api/ingredients` | JWT | ADMIN, EMPLOYEE, CLIENT | List all ingredients |
| GET | `/api/ingredients/{id}` | JWT | ADMIN, EMPLOYEE, CLIENT | Get ingredient by ID |
| POST | `/api/ingredients` | JWT | ADMIN, EMPLOYEE | Create a new ingredient |
| PUT | `/api/ingredients/{id}` | JWT | ADMIN, EMPLOYEE | Update ingredient by ID |
| DELETE | `/api/ingredients/{id}` | JWT | ADMIN, EMPLOYEE | Delete ingredient by ID |

### 6.5 Menu Management

| Method | Endpoint | Auth | Roles | Description |
|--------|----------|------|-------|-------------|
| GET | `/api/menus` | JWT | ADMIN, EMPLOYEE, CLIENT | List all menus |
| GET | `/api/menus/{id}` | JWT | ADMIN, EMPLOYEE, CLIENT | Get menu by ID |
| GET | `/api/menus/by-date/{date}` | JWT | ADMIN, EMPLOYEE, CLIENT | Get menu by date |
| POST | `/api/menus` | JWT | ADMIN, EMPLOYEE¹ | Create/publish a menu |
| PUT | `/api/menus/{id}` | JWT | ADMIN, EMPLOYEE | Update menu by ID |
| DELETE | `/api/menus/{id}` | JWT | ADMIN, EMPLOYEE | Delete menu by ID |

> ¹ EMPLOYEE can only publish menus for future dates (`@PreAuthorize` constraint)

### 6.6 Purchase Management

| Method | Endpoint | Auth | Roles | Description |
|--------|----------|------|-------|-------------|
| GET | `/api/purchases` | JWT | ADMIN, CLIENT | List purchases |
| GET | `/api/purchases/{id}` | JWT | ADMIN, CLIENT | Get purchase by ID |
| GET | `/api/purchases/by-client/{clientId}` | JWT | ADMIN, CLIENT | List purchases by client |
| GET | `/api/purchases/client/{clientId}` | JWT | ADMIN, CLIENT | List purchases by client (alt) |
| GET | `/api/purchases/date/{date}` | JWT | ADMIN, CLIENT | List purchases by date |
| POST | `/api/purchases` | JWT | ADMIN, CLIENT | Place a new purchase/order |
| PUT | `/api/purchases/{id}` | JWT | ADMIN, CLIENT | Update purchase by ID |
| DELETE | `/api/purchases/{id}` | JWT | ADMIN, CLIENT | Delete purchase by ID |

### 6.7 Authentication Flow

```
Client                        API
  │                            │
  │── POST /api/auth/login ───▶│
  │   { email, password }      │  1. Validate credentials
  │                            │  2. Issue JWT (RS256, exp: 1h)
  │◀── 200 { token: "..." } ───│
  │                            │
  │── GET /api/menus           │
  │   Authorization: Bearer …  │  3. Validate JWT signature + expiry
  │                            │  4. Extract role from claims
  │◀── 200 [ menus... ] ───────│  5. Enforce role-based access
```

---

## 7. Secure Design Decisions

| Decision | Rationale |
|----------|-----------|
| JWT with RS256 (asymmetric) | Private key signs tokens; public key verifies — compromise of the API does not expose signing capability |
| Stateless authentication | No server-side session storage; scales horizontally without shared session state |
| Role enforcement at service layer | Authorization checks in the domain/application layer, not only at the HTTP route level (defense in depth) |
| Parameterized queries via ORM | Eliminates SQL injection at the persistence layer |
| Append-only audit log | Logs written by the application cannot be modified; tampering requires OS-level access |
| Reports in isolated directory | File output restricted to a dedicated path; user-supplied filenames validated against an allowlist pattern |
| Passwords hashed with Bcrypt (cost ≥ 12) | Adaptive hashing resists brute-force even if the database is exfiltrated |
| Environment-based secrets | Database credentials and JWT keys loaded from environment variables; never hardcoded |
| HTTPS enforced at entry point | All plaintext HTTP connections rejected or redirected at the reverse proxy level |
| Minimal OS permissions | Application process runs as a non-root user with access only to its working directory |
