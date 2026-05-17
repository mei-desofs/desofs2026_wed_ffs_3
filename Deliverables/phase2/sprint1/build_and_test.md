# Build and Test – Phase 2 Sprint 1

## 1. Test Results

All tests are executed with `mvn test` from `project/`. The test suite uses H2 in-memory database for isolation — no running PostgreSQL instance is required to run tests.

### 1.1 Test Execution Summary

```
[INFO] Tests run: 66, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

### 1.2 Test Suite Breakdown

| Suite | Type | Tests | Result |
|-------|------|-------|--------|
| `DishControllerTest` | Unit | 8 | ✅ Pass |
| `IngredientControllerTest` | Unit | 8 | ✅ Pass |
| `MenuControllerTest` | Unit | 10 | ✅ Pass |
| `PurchaseControllerTest` | Unit | 10 | ✅ Pass |
| `UserControllerTest` | Unit | 3 | ✅ Pass |
| `DishServiceTest` | Unit | 5 | ✅ Pass |
| `IngredientServiceTest` | Unit | 3 | ✅ Pass |
| `MenuServiceTest` | Unit | 5 | ✅ Pass |
| `PurchaseServiceTest` | Unit | 3 | ✅ Pass |
| `UserServiceTest` | Unit | 3 | ✅ Pass |
| `AuthenticationIntegrationTest` | Integration | 2 | ✅ Pass |
| `SecureChannelIntegrationTest` | Integration | 2 | ✅ Pass |
| `ArchitectureTest` | Architecture | 4 | ✅ Pass |
| **Total** | | **66** | **✅ Pass** |

### 1.3 Security-Relevant Test Cases

| Test | Suite | What it verifies | Threat |
|------|-------|-----------------|--------|
| `testProtectedEndpointRequiresJwt` | `AuthenticationIntegrationTest` | Unauthenticated request to `/api/users` returns HTTP 403 | T12, T18 |
| `testLoginValidationErrors` | `AuthenticationIntegrationTest` | Missing email/password fields return HTTP 400 | T05 |
| `testHttpsRedirect` | `SecureChannelIntegrationTest` | Plain HTTP request redirected or rejected when HTTPS required | T37 |
| `testXForwardedProtoHttpsAccepted` | `SecureChannelIntegrationTest` | Request with `X-Forwarded-Proto: https` accepted by security filter | T36 |

---

## 2. Code Coverage

Coverage is measured by **JaCoCo** (configured in `pom.xml`). Reports are generated at `project/target/site/jacoco/index.html` after running `mvn verify`.

```bash
mvn verify
# Report: project/target/site/jacoco/index.html
```

> **Note:** Full coverage report will be published as a CI artefact once the GitHub Actions pipeline is active. See [pipeline.md](./pipeline.md).

---

## 3. Architecture Tests

**ArchUnit** enforces structural constraints on every test run, preventing regression of the layered architecture.

| Rule | What it enforces |
|------|-----------------|
| `services_must_be_public_and_name` | All classes in `..service..` ending with `Service` must be public |
| `controller_dont_access_repos` | Controllers must never import Repository classes directly |
| `repositories_devem_ser_publicos` | All Repository interfaces must be public |
| `controllers_devem_ser_publicos` | All Controller classes must be public |

All 4 architecture rules pass. Source: `project/src/test/java/com/cafeteriamanagement/ArchitectureTest.java`

---

## 4. SAST — Static Application Security Testing

### Tool: SpotBugs + Find Security Bugs plugin

**Status:** ✅ Configured in `pom.xml` — pending first CI pipeline run for automated report

**Configuration in `pom.xml`:**
```xml
<plugin>
    <groupId>com.github.spotbugs</groupId>
    <artifactId>spotbugs-maven-plugin</artifactId>
    <version>4.8.3.1</version>
    <configuration>
        <plugins>
            <plugin>
                <groupId>com.h3xstream.findsecbugs</groupId>
                <artifactId>findsecbugs-plugin</artifactId>
                <version>1.14.0</version>
            </plugin>
        </plugins>
        <effort>Max</effort>
        <threshold>Low</threshold>
        <failOnError>true</failOnError>
    </configuration>
</plugin>
```

**What it covers:**
- SQL injection patterns
- Hardcoded credentials detection
- Insecure random number generation
- Unsafe deserialization
- Path traversal vulnerabilities

**Manual run:**
```bash
cd project
mvn spotbugs:check
# Report: project/target/spotbugsXml.xml
```

> Results will be added here once the CI pipeline produces its first automated report.

---

## 5. SCA — Software Composition Analysis

### Tool: OWASP Dependency-Check

**Status:** ✅ Integrated in CI pipeline (see [pipeline.md](./pipeline.md))

**Maven configuration:**
```xml
<plugin>
    <groupId>org.owasp</groupId>
    <artifactId>dependency-check-maven</artifactId>
    <version>12.2.0</version>
    <configuration>
        <format>ALL</format>
        <failBuildOnCVSS>7</failBuildOnCVSS>
        <ossindexAnalyzerEnabled>false</ossindexAnalyzerEnabled>
        <nvdApiKeyEnvironmentVariable>NVD_API_KEY</nvdApiKeyEnvironmentVariable>
        <suppressionFiles>
            <suppressionFile>${project.basedir}/config/dependency-check-suppressions.xml</suppressionFile>
        </suppressionFiles>
    </configuration>
</plugin>
```

**Policy:** Build fails if any dependency has a CVE with CVSS score ≥ 7 (High or Critical).

**Manual run (local):**
```bash
cd project
mvn dependency-check:check
# Report: project/target/dependency-check-report.html
```

**Sprint 1 result:** Local SCA verification passed with the CVSS ≥ 7 gate after dependency upgrades and documented time-limited suppressions for latest-line/no-fixed-version findings.

---

## 6. DAST — Dynamic Application Security Testing

### Tool: OWASP ZAP (Zed Attack Proxy)

**Status:** ⬜ To be executed against the Docker Compose stack

**Planned execution:**
1. Start the full stack: `docker compose up --build -d`
2. Wait for health check to pass
3. Run ZAP baseline scan against `http://localhost:8081`
4. Export report as HTML + JSON

**Target endpoints for active scan:**
- `POST /api/auth/login` — brute force, user enumeration
- `GET /api/users` — IDOR, authorisation bypass
- `GET /api/files?path=…` — path traversal
- `POST /api/files` — injection, large payload
- `POST /api/orders` — price tampering, privilege escalation

**Abuse cases covered:**
- AC01 — Brute-force login → rate limiting response
- AC02 — IDOR on order history
- AC06 — Path traversal on file endpoint
- AC08 — DoS via order flooding

> Results will be added here once ZAP scan is executed.

---

## 7. Component Inventory

### Application Components

| Component | Package | Description |
|-----------|---------|-------------|
| `AuthController` | `controller` | JWT login endpoint |
| `UserController` | `controller` | User CRUD (ADMIN) and self-profile (CLIENT) |
| `DishController` | `controller` | Dish management (ADMIN, EMPLOYEE) |
| `IngredientController` | `controller` | Ingredient management (ADMIN, EMPLOYEE) |
| `MenuController` | `controller` | Menu publication and browsing |
| `PurchaseController` | `controller` | Order placement (CLIENT) and tracking |
| `FileController` | `controller` | OS-level file and directory operations |
| `FileSystemService` | `service` | Sandboxed filesystem access with path traversal protection |
| `JwtTokenUtil` | `security` | JWT generation and validation |
| `JwtRequestFilter` | `security` | Per-request JWT extraction and authentication |
| `SecurityConfig` | `config` | Spring Security filter chain and RBAC rules |
| `GlobalExceptionHandler` | `exception` | Centralised error response (no stack trace leakage) |
| `SecurityAuditLogger` | `security` | Structured audit log for all authentication and access-control events |
| `SimpleRateLimiter` | `security` | In-memory rate limiter (5 attempts / 15 min) applied to login endpoint |
| `TokenBlocklist` | `security` | In-memory JWT revocation store — **not yet integrated** into `JwtRequestFilter` |
| `PasswordPolicyService` | `service` | Enforces min/max password length; delegates breach check to HIBP client |
| `HaveIBeenPwnedClient` | `security` | k-anonymity SHA-1 query to HIBP API to reject known breached passwords |

### Runtime Infrastructure

| Component | Technology | Version |
|-----------|-----------|---------|
| Application server | Spring Boot (embedded Tomcat) | 3.5.14 |
| Database | PostgreSQL | 16 |
| Containerisation | Docker Compose | — |
| Build tool | Maven | 3.9 |
| Java runtime | Eclipse Temurin JRE | 17 (Alpine) |

### Key Dependencies

| Dependency | Version | Purpose |
|------------|---------|---------|
| `spring-boot-starter-security` | 3.1.5 | Security filter chain, RBAC |
| `spring-boot-starter-data-jpa` | 3.1.5 | ORM, parameterised queries |
| `spring-boot-starter-validation` | 3.1.5 | Bean validation at API boundary |
| `jjwt-api` + `jjwt-impl` | 0.11.5 | JWT signing and verification |
| `springdoc-openapi-starter-webmvc-ui` | 2.3.0 | Swagger UI |
| `archunit-junit5` | 1.4.1 | Architecture constraint testing |
| `jacoco-maven-plugin` | 0.8.14 | Code coverage |
| `pitest-maven` | 1.15.0 | Mutation testing |

---

## 8. Configuration Validation

| Configuration | Expected | Verified |
|--------------|----------|---------|
| `jwt.expiration` | 3600 (1 hour) | ✅ Set in `application.properties` |
| `JWT_SECRET` | Loaded from env var | ✅ `@Value("${jwt.secret}")` with no fallback in prod |
| Database credentials | Loaded from env vars | ✅ `${SPRING_DATASOURCE_*}` placeholders |
| H2 console | Disabled in main profile | ✅ Removed from `application.properties` |
| `spring.jpa.show-sql` | `false` in prod | ⬜ Currently `true` — to fix in Sprint 2 |
| HTTPS enforcement | Required (NFR01) | ⬜ Requires reverse proxy — Sprint 2 |
