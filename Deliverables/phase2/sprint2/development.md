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

## 3. Requisitos Não-Funcionais de Performance (NFR06 + NFR07)

### 3.0 Ferramenta de Load Testing — Gatling

**O que é o Gatling?**

[Gatling](https://gatling.io/) é uma ferramenta open-source de load testing orientada a cenários. Os testes são escritos em código (Java, Scala ou Kotlin), compilados e executados como programas JVM, o que permite integração nativa com pipelines Maven/Gradle e controlo de versão junto ao código de produção.

O motor interno é assíncrono e não-bloqueante (Netty + Akka): um único processo Gatling consegue simular milhares de utilizadores virtuais sem criar uma thread por utilizador, ao contrário de ferramentas baseadas em threads como o JMeter. Isto torna-o eficiente em termos de recursos para testes de concorrência.

No final de cada corrida, o Gatling gera um relatório HTML interactivo com percentis de tempo de resposta, throughput, distribuição de erros e linha de tempo de pedidos.

**Porque foi escolhido?**

| Critério | Justificação |
|----------|-------------|
| **Integração Maven** | O `gatling-maven-plugin` executa as simulações directamente no ciclo de vida Maven (`gatling:test`), sem ferramentas externas. O profile `-Pperformance` garante que as classes Gatling só são compiladas quando necessário. |
| **Java DSL** | As simulações são código Java puro — sem ficheiros de configuração proprietários, com suporte a refactoring e revisão de código como qualquer outra classe do projecto. |
| **Assertions programáticas** | As assertions (`percentile(95).lte(500)`, `successfulRequests().percent().gte(95)`) são expressas em código e falham o build automaticamente, tornando os NFR gates executáveis em CI. |
| **Motor assíncrono** | Simular 100 utilizadores concorrentes não requer 100 threads — o Gatling usa I/O não-bloqueante, produzindo carga realista com baixo overhead na máquina de teste. |
| **Relatório HTML** | Artefacto `gatling-performance-report` gerado e publicado automaticamente no CI para cada corrida. |

### 3.1 NFR06 — Tempo de resposta < 500 ms

**Requisito:** "API responses for menu and order listing must complete within 500 ms under normal load."

**Implementação:** Gatling load test (`CafeteriaPerformanceSimulation`) com assertion:

```java
global().responseTime().percentile(95).lte(500)
```

O gate **falha o build** se o P95 de qualquer corrida ultrapassar 500 ms nos endpoints:
- `GET /api/dishes`
- `GET /api/menus`
- `GET /api/ingredients`

### 3.2 NFR07 — 100 requisições concorrentes sem degradação

**Requisito:** "The system must handle at least 100 concurrent requests without degradation."

**Implementação:** A simulação injeta 100 utilizadores virtuais com ramp de 20 segundos (`rampUsers(100).during(20s)`) com assertion:

```java
global().successfulRequests().percent().gte(95.0)
```

O gate **falha o build** se a taxa de sucesso descer abaixo de 95 % sob a carga de 100 utilizadores. O ramp gradual (≈ 5 utilizadores/s) simula crescimento de carga realista e evita artefactos de queuing de um spike instantâneo que inflariam artificialmente o P95.

### 3.3 Execução

| Item | Detalhe |
|------|---------|
| Framework | Gatling 3.11.5 (Java DSL) + `gatling-maven-plugin` 4.10.1 |
| Profile Maven | `performance` (activado com `-Pperformance`) |
| Simulation | `project/src/gatling/java/com/cafeteriamanagement/performance/CafeteriaPerformanceSimulation.java` |
| Comando local | `mvn -f project/pom.xml gatling:test -Pperformance -DbaseUrl=http://localhost:8081` |
| Pré-requisito | Utilizador `perf_admin` / `PerfAdmin@1234` seeded na DB via `docker compose exec psql` |
| CI | Job `performance` no `pipeline.yml` (Stage 3, paralelo ao DAST); artefacto `gatling-performance-report` |
| Isolamento | `src/gatling/java` não faz parte do sourceSet padrão Maven — não é compilado por `mvn verify` sem o profile |

A simulação obtém um JWT partilhado no bloco `before {}` (uma única chamada ao `/api/auth/login`) para que os 100 utilizadores virtuais não gerem 100 logins simultâneos que ativariam o rate limiter.

### 3.4 Reprodução dos Resultados

Passos necessários para reproduzir a corrida localmente a partir de um clone limpo do repositório.

**1. Iniciar o stack Docker (sem JaCoCo)**

```powershell
docker compose -f docker-compose.yml -f docker-compose.local.yml up -d app postgres
```

**2. Aguardar a aplicação estar pronta**

```powershell
docker compose logs -f app
# Aguardar a linha: Started CafeteriaManagementApplication in X seconds
```

**3. Gerar o hash BCrypt e inserir o utilizador `perf_admin`**

```powershell
# Gera o hash BCrypt da password PerfAdmin@1234
$hash = (docker run --rm httpd:alpine htpasswd -nbB "" "PerfAdmin@1234").Split(":")[1]

# Insere o utilizador directamente na base de dados
docker compose exec -T postgres psql -U postgres -d cafeteriadb -c `
  "INSERT INTO users (username, password, balance, user_type) VALUES ('perf_admin', '$hash', 100.0, 'ADMIN') ON CONFLICT (username) DO NOTHING;"
```

> `POST /api/users` requer role ADMIN — não existe registo público, pelo que o utilizador de performance é inserido directamente via psql. `ON CONFLICT DO NOTHING` torna o comando idempotente entre corridas.

**4. Executar a simulação Gatling**

```powershell
mvn -f project/pom.xml gatling:test -Pperformance -DbaseUrl=http://localhost:8081
```

**5. (Opcional) Parar o stack após a corrida**

```powershell
docker compose down
```

---

### 3.5 Resultados Verificados (2026-06-16)

Corrida local contra `http://localhost:8081` com Docker Compose (`docker-compose.local.yml`), seguindo os passos do §3.4.

| Métrica | Threshold | Resultado | Estado |
|---------|-----------|-----------|--------|
| P95 response time | ≤ 500 ms | **22 ms** | PASS |
| Success rate | ≥ 95% | **100%** | PASS |
| Total requests | — | 300 (100 users × 3 endpoints) | — |
| Min response time | — | 6 ms | — |
| Max response time | — | 119 ms | — |
| Mean response time | — | 13 ms | — |

```
Global: 95th percentile of response time is less than or equal to 500.0 : true (actual : 22.0)
Global: percentage of successful events is greater than or equal to 95.0 : true (actual : 100.0)
BUILD SUCCESS
```

Todos os 300 pedidos completaram sem falhas em 19 segundos. O ramp gradual (≈ 5 utilizadores/s durante 20 s) eliminou os artefactos de queuing que provocavam P95 > 500 ms com `atOnceUsers(100)`.

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
