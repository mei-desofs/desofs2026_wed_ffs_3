# Development – Phase 2 Sprint 2

## 1. Requisitos de Complexidade

### 1.1 Agregados DDD (mínimo 3)

| Agregado | Entidade raiz | Entidades / Value Objects |
|----------|--------------|--------------------------|
| **User** | `User` | `Name` (value object), `UserType` (ADMIN, EMPLOYEE, CLIENT) |
| **Catalogue** | `Dish` | `Ingredient`, `Menu`, `Allergen`, `IngredientType` |
| **Purchase** | `Purchase` | Referências a User e Dish; lógica de dedução de saldo |

O `FileSystemService` cumpre o requisito obrigatório de operações ao nível do OS (criar diretórios, ler/escrever/apagar ficheiros) e é tratado como agregado de infraestrutura sandboxed em `/app/files`.

### 1.2 Autorização com 3 roles

| Role | Permissões |
|------|-----------|
| **ADMIN** | CRUD completo em todos os recursos, gestão de utilizadores, apagar ficheiros |
| **EMPLOYEE** | Criar/editar Ingredientes, Pratos, Menus; criar/ler ficheiros; ver compras |
| **CLIENT** | Ver Ingredientes, Pratos, Menus; criar/atualizar/apagar as suas próprias compras; ler ficheiros |

Enforced em dois níveis:
- **HTTP route level** — `SecurityConfig.filterChain()` com `.requestMatchers()` por método e path
- **Method level** — `@PreAuthorize` em `MenuController` (constraint de data futura para EMPLOYEE) e nos métodos create/update/delete de `PurchaseController` (ADMIN ou CLIENT apenas)

### 1.3 Operações OS no servidor

`FileSystemService` expõe 5 operações via REST:

| Método | Endpoint | Roles | Operação OS |
|--------|----------|-------|------------|
| `POST` | `/api/files/directory` | ADMIN, EMPLOYEE | `Files.createDirectories()` |
| `POST` | `/api/files` | ADMIN, EMPLOYEE | `Files.writeString()` |
| `GET` | `/api/files?path=…` | ADMIN, EMPLOYEE, CLIENT | `Files.readString()` |
| `GET` | `/api/files/list?path=…` | ADMIN, EMPLOYEE, CLIENT | `Files.list()` |
| `DELETE` | `/api/files?path=…` | ADMIN | `Files.delete()` / `Files.walk()` |

Todas as paths são normalizadas e validadas via `resolveSafe()` antes da execução. Tamanho máximo de ficheiro: 1 MB.

---

## 2. Controlos de Segurança Implementados no Sprint 2

### 2.1 Logout com revogação de token

`POST /api/auth/logout` extrai o header `Authorization: Bearer <token>`, obtém a data de expiração via `JwtTokenUtil` e regista o token no `TokenBlocklist`. O `JwtRequestFilter` verifica o blocklist em cada pedido — tokens bloqueados recebem HTTP 401.

**ASVS:** V7.4.1 | **Commit:** `a4876fd`

### 2.2 Validação de input nas DTOs de ficheiros

`@Pattern(regexp="^[a-zA-Z0-9._/\\-]+$")` adicionado a `FileWriteRequestDTO` e `DirectoryCreateRequestDTO`. Sequências `../` são rejeitadas na fronteira da API antes de chegarem ao serviço.

**ASVS:** V5.1.1, V12.3.2 | **Commit:** `a4876fd`

### 2.3 Validação de tamanho na DTO de login

`@Size(min=12, max=128)` no campo `password` de `LoginRequestDTO` — consistente com `PasswordPolicyService`.

**ASVS:** V6.2.1 | **Commit:** `a4876fd`

### 2.4 Exception handler sem leakage

`GlobalExceptionHandler` retorna sempre `"An unexpected error occurred."` no catch-all. `ex.getMessage()` é apenas logado server-side via SLF4J.

**ASVS:** V16.5.1 | **Commit:** `a4876fd`

### 2.5 Actuator restrito a /health

```properties
management.endpoints.web.exposure.include=health
management.endpoint.health.show-details=never
```
`SecurityConfig` nega explicitamente todos os pedidos a `/actuator/**` exceto `/actuator/health`.

**ASVS:** V13.4.5 | **Commit:** `a4876fd`

### 2.6 Dockerfile com utilizador não-root

```dockerfile
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser
```
Um atacante que consiga execução de código no container não obtém privilégios root.

**ASVS:** V15.1.3 | **Commit:** `a4876fd`

### 2.7 `.dockerignore`

`project/.dockerignore` exclui `.git`, `target/`, `.env*`, `.vscode/`, `.idea/` do build context Docker.

**ASVS:** V13.4.1 | **Commit:** `a4876fd`

### 2.8 CORS centralizado

Todas as anotações `@CrossOrigin(origins = "*")` por controller foram removidas. Um único `CorsConfigurationSource` em `SecurityConfig` aplica uma allowlist configurável (`app.cors.allowed-origins`) ao path `/api/**`.

**Commit:** `f3903e7`

### 2.9 Logging de operações de ficheiros

`FileSystemService` agora chama `SecurityAuditLogger.logFileOperation()` em cada operação:

| Operação | Evento auditado |
|----------|----------------|
| `createDirectory()` | `CREATE_DIRECTORY` com username e path |
| `writeFile()` | `WRITE_FILE` com username e path |
| `readFile()` | `READ_FILE` com username e path |
| `delete()` | `DELETE` com username e path |

**ASVS:** V16.3.1 | **Commit:** Sprint 2

### 2.10 Logging de operações de compras

`PurchaseService` agora chama `SecurityAuditLogger.logPurchaseOperation()` em cada operação:

| Operação | Evento auditado |
|----------|----------------|
| `createPurchase()` | `PURCHASE_CREATE` com username, purchaseId, dishName |
| `updatePurchase()` | `PURCHASE_UPDATE` com username, purchaseId, dishName |
| `deletePurchase()` | `PURCHASE_DELETE` com username, purchaseId, dishName |

**ASVS:** V16.3.1 | **Commit:** Sprint 2

### 2.11 Remoção de utilitários de debug

`PasswordUtil.java`, `HashGenerator.java` e `PasswordTest.java` removidos de `src/main/java/com/cafeteriamanagement/util/`. Estes ficheiros continham passwords hardcoded e `System.out.println` e não devem existir em código de produção.

**ASVS:** V16.2.5 | **Commit:** Sprint 2

### 2.12 Credenciais default removidas

`data.sql` não contém nenhum `INSERT INTO users`. As 5 contas default com password "123" foram removidas.

**ASVS:** V6.3.2 | **Commit:** `a4876fd`

---

## 3. Arquitectura — Modelo C4

Quatro níveis de diagramas C4 produzidos no Sprint 2 em `Deliverables/diagrams/img/c4/`:

| Diagrama | Ficheiro | Descrição |
|---------|----------|-----------|
| Overview | `01_c4_zoom_overview.svg` | Resumo único de todos os níveis C4 |
| L1 – System Context | `02_c1_system_context.svg` | Coffeetaria em relação a utilizadores (Admin, Employee, Client) e HIBP API |
| L2 – Container | `03_c2_containers.svg` | nginx → Spring Boot → PostgreSQL; volumes Docker |
| L3 – Component | `04_c3_backend_components.svg` | Controllers, services, security layer, repositories |
| L4 – Code (Security) | `05_c4_code_security.svg` | Detalhe: JwtRequestFilter, SecurityConfig, TokenBlocklist, SecurityAuditLogger, SimpleRateLimiter |

Fonte: `Deliverables/diagrams/src/c4_architecture.drawio`

---

## 4. Boas Práticas Adoptadas

| Prática | Estado | Evidência |
|---------|--------|-----------|
| Arquitectura em camadas (Controller → Service → Repository) | ✅ | ArchUnit `controller_dont_access_repos` |
| Sem credenciais hardcoded | ✅ | JWT secret e credenciais DB via env vars |
| Queries parametrizadas (Spring Data JPA) | ✅ | Todos os repositórios estendem `JpaRepository` |
| DTOs com `@Valid` / `@Size` / `@Pattern` | ✅ | Validação na fronteira da API |
| BCrypt para passwords | ✅ | `BCryptPasswordEncoder` em `SecurityConfig` |
| Política de passwords (tamanho + HIBP) | ✅ | `PasswordPolicyService` + `HaveIBeenPwnedClient` |
| RBAC a nível HTTP | ✅ | `SecurityConfig.filterChain()` |
| RBAC a nível de método (`@PreAuthorize`) | ✅ | `MenuController` (constraint de data) e `PurchaseController` (create/update/delete) |
| Ownership de compras verificado | ✅ | `PurchaseController` verifica `currentUsername` antes de update/delete |
| Mensagens de erro genéricas | ✅ | `GlobalExceptionHandler` sem `ex.getMessage()` |
| Rate limiting no login | ✅ | `SimpleRateLimiter` — 5 tentativas / 15 min por username e IP |
| Revogação de JWT no logout | ✅ | `TokenBlocklist` verificado em cada pedido |
| Audit log estruturado (auth + ficheiros + compras) | ✅ | `SecurityAuditLogger` — todos os eventos relevantes |
| HTTPS com HSTS | ✅ | nginx + `app.security.require-https=true`; HSTS 1 ano |
| CORS com allowlist explícita | ✅ | `CorsConfigurationSource` centralizado; sem wildcard |
| Container com utilizador não-root | ✅ | `USER appuser` em `Dockerfile` |
| `.dockerignore` sem segredos | ✅ | `project/.dockerignore` criado no Sprint 2 |
| CVE gate no CI (CVSS ≥ 7 falha o build) | ✅ | OWASP Dependency-Check em `sca.yml` |

---

## 5. Code Review

Todas as alterações de segurança do Sprint 2 foram submetidas via Pull Requests no GitHub antes de merge. O pipeline CI actua como revisor não-humano em cada push.

### 5.1 Contribuições no Sprint 2

| Autor | Áreas |
|-------|-------|
| Henrique Dias (1201816) | Security fixes (`a4876fd`), centralização CORS, workflows DAST, atualização ASVS |
| Leonardo Costa (1250532) | Diagramas C4, ASVS checklist (V1, V4, V7, data protection, communication, configs) |
| Lourenço Mendes (1201270) | Testes unitários de segurança, fixes de documentação |
| Luís Santos (1250534) | Estabilização PITest, integração CI |
| Nuno Oliveira (1210939) | Logging de ficheiros e compras, `@PreAuthorize`, remoção de debug utils, documentação Sprint 2 |

### 5.2 Backlog do Sprint 1 — Resolução

| # | Problema | Estado no Sprint 2 |
|---|----------|-------------------|
| 1 | Debug `println` em `CustomUserDetailsService` | ✅ Resolvido Sprint 1 (`a4876fd`) |
| 2 | `TokenBlocklist` não ligado ao `JwtRequestFilter` | ✅ Resolvido Sprint 1 (`a4876fd`) |
| 3 | Credenciais default em `data.sql` | ✅ Resolvido Sprint 1 (`a4876fd`) |
| 6 | Debug utilities em `src/main/java/util/` | ✅ Resolvido Sprint 2 — ficheiros eliminados |
| 7 | `@PreAuthorize` só no `MenuController` | ✅ Resolvido Sprint 2 — adicionado em `PurchaseController` |
| 8 | Sem audit log para operações de ficheiros | ✅ Resolvido Sprint 2 — `logFileOperation()` em `FileSystemService` |
| 9 | JWT usa HS256 — RS256 previsto no design | 🟡 Pendente — JJWT RS256 requer gestão de keypair; adiado |
| 10 | Rate limiting só no login | 🟡 Pendente — endpoints protegidos por RBAC e JWT |
