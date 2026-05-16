# ASVS Checklist – Phase 2 Sprint 1

> Based on OWASP Application Security Verification Standard v4.0.3
> Status updated at end of Sprint 1 to reflect implemented controls.
> Phase 1 baseline is in [`../../security_testing.md §3`](../../security_testing.md).

**Status legend:**
- ✅ Implemented and verified
- ⬜ Planned — not yet implemented
- 🔄 Partially implemented
- N/A — Not applicable to this system

---

## V1 – Architecture, Design and Threat Modeling

| ID | Requirement | Sprint 1 Status | Notes |
|----|-------------|-----------------|-------|
| V1.1.1 | Verify use of a secure SDLC | ✅ | SSDLC followed since Phase 1; Sprint 1 adds CI pipeline |
| V1.1.2 | Verify threat modeling for every design change | ✅ | Threat model reviewed; filesystem API threats covered in Phase 1 STRIDE |
| V1.1.4 | Verify documentation of all trust boundaries | ✅ | DFD Level 2 (P5-Reporting) added in Phase 1 |
| V1.2.1 | Verify components use secure communication channels | ⬜ | HTTPS requires reverse proxy — Sprint 2 |
| V1.2.2 | Verify all components require authentication | ✅ | JWT required on all non-public endpoints |

---

## V2 – Authentication

| ID | Requirement | Sprint 1 Status | SDR | Notes |
|----|-------------|-----------------|-----|-------|
| V2.1.1 | Passwords at least 12 characters | ⬜ | SDR05 | Validation not yet enforced in `UserService` |
| V2.1.2 | Passwords up to 128 characters allowed | ⬜ | SDR05 | No truncation found, but not explicitly validated |
| V2.2.1 | Anti-automation controls (brute force) | ⬜ | SDR04 | Rate limiting not implemented — Sprint 2 |
| V2.2.2 | Weak password rejection | ⬜ | SDR05 | HaveIBeenPwned integration not implemented |
| V2.10.4 | Secrets not stored in source code | ✅ | SDR18 | JWT secret moved to `JWT_SECRET` env var in Sprint 1 |

---

## V3 – Session Management

| ID | Requirement | Sprint 1 Status | SDR | Notes |
|----|-------------|-----------------|-----|-------|
| V3.2.1 | New session token on authentication | ✅ | SDR01 | New JWT issued on every login |
| V3.2.2 | Session tokens have at least 64 bits of entropy | ✅ | SDR01 | JWT signed with HS256; UUID-based subject |
| V3.3.1 | Logout invalidates session token | ⬜ | SDR01 | No token blocklist — Sprint 2 |
| V3.3.2 | Session tokens expire after defined period | ✅ | SDR01 | **Sprint 1 fix:** `jwt.expiration=3600` (1 hour) |

---

## V4 – Access Control

| ID | Requirement | Sprint 1 Status | SDR | Notes |
|----|-------------|-----------------|-----|-------|
| V4.1.1 | Access control rules enforced server-side | 🔄 | SDR02 | Enforced at HTTP route level; service-layer `@PreAuthorize` pending Sprint 2 |
| V4.1.2 | User attributes for access control not user-controlled | ✅ | SDR02 | Role sourced from JWT, never from request body |
| V4.1.3 | Principle of least privilege | ✅ | SDR15 | File API restricted to ADMIN/EMPLOYEE for writes; CLIENT read-only |
| V4.1.5 | Access control failures logged | ⬜ | SDR19 | Audit logging not implemented — Sprint 2 |
| V4.3.1 | Admin interfaces use appropriate auth | ✅ | SDR02 | All admin endpoints require `ADMIN` role JWT |

---

## V5 – Validation, Sanitization and Encoding

| ID | Requirement | Sprint 1 Status | SDR | Notes |
|----|-------------|-----------------|-----|-------|
| V5.1.2 | Protection against mass parameter assignment | ✅ | SDR07 | All controllers use explicit DTOs |
| V5.1.3 | All input validated against allowlist | 🔄 | SDR06 | Bean validation (`@Valid`) on DTOs; file path validated via `resolveSafe()` |
| V5.3.4 | Database queries use parameterized queries | ✅ | SDR08 | Spring Data JPA / Hibernate — no string-concatenated SQL |
| V5.3.8 | Protection against OS command injection | ✅ | SDR13 | No OS commands executed; filesystem access via Java NIO only |

---

## V7 – Error Handling and Logging

| ID | Requirement | Sprint 1 Status | SDR | Notes |
|----|-------------|-----------------|-----|-------|
| V7.1.1 | No sensitive data in logs | 🔄 | SDR16 | Passwords not logged; JWT tokens logged in some debug paths |
| V7.1.2 | No sensitive data in error messages | ✅ | SDR12 | `GlobalExceptionHandler` returns generic messages; stack traces not exposed |
| V7.2.1 | Authentication decisions logged | ⬜ | SDR19 | Audit logging not implemented — Sprint 2 |
| V7.2.2 | Access control failures logged | ⬜ | SDR19 | Sprint 2 |
| V7.4.1 | Generic message shown on unexpected errors | ✅ | SDR12 | `GlobalExceptionHandler` handles all unhandled exceptions |

---

## V8 – Data Protection

| ID | Requirement | Sprint 1 Status | SDR | Notes |
|----|-------------|-----------------|-----|-------|
| V8.1.1 | Sensitive data not cached | ⬜ | — | `Cache-Control: no-store` not yet set on sensitive endpoints |
| V8.3.1 | Sensitive data not in URL parameters | ✅ | — | Credentials only in request body (POST) |
| V8.3.4 | Sensitive data identified | ✅ | — | Identified in Phase 1 `analysis.md` |

---

## V9 – Communication Security

| ID | Requirement | Sprint 1 Status | SDR | Notes |
|----|-------------|-----------------|-----|-------|
| V9.1.1 | TLS for all client connectivity | ⬜ | SDR10 | Requires reverse proxy (nginx/Traefik) — Sprint 2 |
| V9.1.2 | TLS 1.2 or higher | ⬜ | SDR10 | Pending reverse proxy setup |

---

## V12 – Files and Resources

| ID | Requirement | Sprint 1 Status | SDR | Notes |
|----|-------------|-----------------|-----|-------|
| V12.1.1 | Application rejects oversized files | ✅ | SDR14 | **Sprint 1:** Max file size 1 MB enforced in `FileSystemService` |
| V12.3.1 | User-submitted filenames not used directly | ✅ | SDR13 | `resolveSafe()` normalises and validates all paths before use |
| V12.3.2 | Filenames validated against allowlist | 🔄 | SDR13 | Path traversal blocked; extension allowlist pending Sprint 2 |
| V12.3.3 | Protection against path traversal | ✅ | SDR13 | **Sprint 1:** `basePath.resolve().normalize()` + prefix check |

---

## V14 – Configuration

| ID | Requirement | Sprint 1 Status | SDR | Notes |
|----|-------------|-----------------|-----|-------|
| V14.2.1 | All components up to date and not using deprecated libraries | ⬜ | SDR22, SDR23 | SCA (OWASP DC) not yet run — Sprint 1 pipeline target |
| V14.2.2 | Remove unnecessary features, documentation, and sample code | 🔄 | — | `PasswordTest.java` in `src/main` to be moved — Sprint 2 |
| V14.3.2 | Web or application server error handling configured to prevent stack trace disclosure | ✅ | SDR12 | `GlobalExceptionHandler` prevents all stack trace leakage |

---

## Sprint 1 ASVS Progress Summary

| Category | Total items tracked | ✅ Implemented | 🔄 Partial | ⬜ Planned |
|----------|--------------------|--------------:|----------:|----------:|
| V1 Architecture | 5 | 3 | 0 | 2 |
| V2 Authentication | 5 | 1 | 0 | 4 |
| V3 Session Mgmt | 4 | 3 | 0 | 1 |
| V4 Access Control | 5 | 3 | 1 | 1 |
| V5 Validation | 4 | 2 | 2 | 0 |
| V7 Error / Logging | 5 | 2 | 1 | 2 |
| V8 Data Protection | 3 | 2 | 0 | 1 |
| V9 Communication | 2 | 0 | 0 | 2 |
| V12 Files | 4 | 3 | 1 | 0 |
| V14 Configuration | 3 | 1 | 1 | 1 |
| **Total** | **40** | **20** | **6** | **14** |

---

## Traceability: ASVS → Security Requirements → Tests

| ASVS | SDR | Test Case | Sprint 1 change |
|------|-----|-----------|-----------------|
| V2.10.4 | SDR18 | TC18 | JWT secret env var (Sprint 1) |
| V3.3.2 | SDR01 | TC22 | JWT expiry 1h (Sprint 1) |
| V12.1.1 | SDR14 | TC16 | Max file size 1 MB (Sprint 1) |
| V12.3.3 | SDR13 | TC15 | Path traversal protection (Sprint 1) |
| V5.3.4 | SDR08 | TC17 | Parameterised queries via JPA (Phase 1 → Sprint 1 verified) |
| V4.1.2 | SDR02 | TC09, TC10 | Role from JWT only (Phase 1 → Sprint 1 verified) |
