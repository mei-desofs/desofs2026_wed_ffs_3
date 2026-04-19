# Design – Coffeetaria

---

## 1. System Overview & Architecture

Coffeetaria is a backend REST API application following a layered architecture based on Domain-Driven Design (DDD). It exposes HTTP endpoints consumed by clients (web/mobile frontends or direct API consumers) and persists data in a relational database. The system also interacts with the server filesystem for report generation and audit logging.

### 1.1 Architectural Layers

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

> **Diagram:** [`diagrams/domain_model.puml`](./diagrams/domain_model.puml)

![Domain Model](./diagrams/domain_model.png)

### Aggregates Summary

| Aggregate | Root | Entities | Value Objects |
|-----------|------|----------|---------------|
| User | `User` | — | `Email`, `Role` |
| Menu | `Menu` | `Dish`, `Ingredient` | `Money` |
| Order | `Order` | `OrderItem` | `OrderRef`, `OrderStatus`, `Money` |

**Roles:** `CLIENT` · `EMPLOYEE` · `ADMIN`

**Order lifecycle:** `PENDING` → `PREPARING` → `READY` → `DELIVERED`

---

## 3. Data Flow Diagrams

### 3.1 DFD Level 0 – Context Diagram

> **Diagram:** [`diagrams/dfd_level0.puml`](./diagrams/dfd_level0.puml)

![DFD Level 0](./diagrams/dfd_level0.png)

**Trust Boundaries:**
- All external entities (Client, Employee, Admin) are **outside** the trust boundary
- The system trusts only requests bearing a valid signed JWT
- The filesystem and database are **inside** the trust boundary (internal infrastructure)

---

### 3.2 DFD Level 1 – Internal Processes

> **Diagram:** [`diagrams/dfd_level1.puml`](./diagrams/dfd_level1.puml)

![DFD Level 1](./diagrams/dfd_level1.png)

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

---

## 4. Secure Design Decisions

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
