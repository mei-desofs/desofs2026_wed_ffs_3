# ASVS Checklist – Phase 2 Sprint 2

> **Documento primário:** [`Deliverables/ASVS_5_0_Tracker.xlsx`](../../ASVS_5_0_Tracker.xlsx)
>
> O tracker OWASP ASVS 5.0 é mantido em Excel com rastreabilidade completa por requisito. Este ficheiro é o resumo legível do Sprint 2. Para o detalhe por requisito, consultar o Excel.

---

## 1. Resumo por Capítulo (Sprint 1 + Sprint 2 acumulado)

| Capítulo | Compliant | In Progress | Not Applicable | Not Started |
|----------|-----------|-------------|----------------|-------------|
| V1 – Encoding and Sanitisation | 3 | 1 | 0 | 2 |
| V4 – Input Validation | 5 | 1 | 1 | 3 |
| V5 – File Handling | 3 | 0 | 5 | 5 |
| V6 – Authentication | 7 | 0 | 19 | 21 |
| V7 – Session Management | 3 | 0 | 2 | 14 |
| V8 – Authorization | 2 | 1 | 2 | 8 |
| V9 – Self-contained Tokens | 1 | 2 | 0 | 4 |
| V12 – Secure Communication | 3 | 0 | 2 | 7 |
| V13 – Configuration | 3 | 0 | 1 | 17 |
| V15 – Secure Coding | 4 | 0 | 1 | 16 |
| V16 – Security Logging | 7 | 0 | 0 | 10 |

---

## 2. Controlos Novos do Sprint 2

### V1 – Encoding and Sanitisation

| Req | Descrição | Implementação |
|-----|-----------|---------------|
| V1.1.1 | Validação de todos os dados controlados pelo utilizador | `@Valid` + `@Pattern` / `@Size` em todas as DTOs; `resolveSafe()` em `FileSystemService` |
| V1.5.1 | Validação próxima do input | `@Valid` na fronteira do controller, antes da lógica de serviço |

### V4 – Input Validation

| Req | Descrição | Implementação |
|-----|-----------|---------------|
| V4.1.1 | Validação positiva em todo o input | `@Pattern(regexp="^[a-zA-Z0-9._/\\-]+$")` em `FileWriteRequestDTO` e `DirectoryCreateRequestDTO` |
| V4.3.1 | Output encoding contextual | Respostas JSON via Spring MVC `@RestController` |
| V4.4.1 | Rejeição de caracteres perigosos | Sequências `../` bloqueadas por `resolveSafe()` + `@Pattern` |

### V6 – Authentication

| Req | Descrição | Implementação |
|-----|-----------|---------------|
| V6.2.1 | Passwords ≥ 12 caracteres | `@Size(min=12)` em `UserDTO` + `LoginRequestDTO`; `PasswordPolicyService` |
| V6.2.4 | Rejeição de passwords comprometidas | `HaveIBeenPwnedClient` — k-anonymity SHA-1 |
| V6.2.8 | Comparação correta de passwords | `BCryptPasswordEncoder.matches()` |
| V6.2.9 | Sem truncação de password | `@Size(max=128)` enforced |
| V6.3.1 | Anti-brute-force (rate limiting) | `SimpleRateLimiter`: 5 tentativas / 15 min por username e IP |
| V6.3.2 | Sem credenciais default em produção | `data.sql` sem `INSERT INTO users` |
| V6.3.8 | Mensagem de erro genérica | `"Invalid credentials"` sempre — sem enumeração de utilizadores |

### V7 – Session Management

| Req | Descrição | Implementação |
|-----|-----------|---------------|
| V7.3.1 | Timeout absoluto de sessão | `jwt.expiration=3600` (1 hora) |
| V7.3.2 | Timeout enforced | Verificado em cada pedido em `JwtRequestFilter` |
| V7.4.1 | Logout invalida token | `POST /api/auth/logout` → `TokenBlocklist.block()`; filtro rejeita tokens bloqueados |

### V8 – Authorization

| Req | Descrição | Implementação |
|-----|-----------|---------------|
| V8.1.1 | Controlo de acesso ao nível da aplicação | `SecurityConfig.filterChain()` por método HTTP e path |
| V8.1.2 | Default deny | `anyRequest().authenticated()` em `SecurityConfig` |
| V8.3.x | Verificação de ownership | `PurchaseController` verifica `currentUsername` em update/delete; `@PreAuthorize` em create/update/delete |

### V9 – Self-contained Tokens

| Req | Descrição | Implementação |
|-----|-----------|---------------|
| V9.1.1 | Expiração JWT validada | `JwtTokenUtil.isTokenExpired()` verificado em cada pedido |
| V9.2.1 (in progress) | Algoritmo de assinatura forte | HS256 implementado; RS256 pendente |

### V12 – Secure Communication

| Req | Descrição | Implementação |
|-----|-----------|---------------|
| V12.1.1 | TLS enforced | nginx reverse proxy com TLS (`deploy/nginx/conf.d/`) |
| V12.1.2 | Cipher suites fortes | `ssl_ciphers` configurado em nginx; RC4/3DES excluídos |
| V12.2.1 | HTTP rejeitado / redirecionado | nginx porta 80 → HTTPS redirect |

### V13 – Configuration

| Req | Descrição | Implementação |
|-----|-----------|---------------|
| V13.4.1 | `.dockerignore` exclui `.git` | `project/.dockerignore`: exclui `.git`, `target/`, `.env*` |
| V13.4.2 | Sem stack traces nas respostas | `GlobalExceptionHandler` retorna mensagem genérica |
| V13.4.5 | Actuator restrito | `management.endpoints.web.exposure.include=health`; `/actuator/**` negado |

### V15 – Secure Coding

| Req | Descrição | Implementação |
|-----|-----------|---------------|
| V15.1.3 | Container executa como não-root | `USER appuser` em `project/Dockerfile` |
| V15.2.1 | Sem componentes com vulnerabilidades conhecidas | OWASP Dependency-Check CVSS ≥ 7 gate no CI; Dependabot activo |
| V15.3.1 | Build multi-stage (imagem mínima) | `FROM maven:3.9 AS build` → `FROM eclipse-temurin:17-jre-alpine` |
| V15.3.2 | Sem credenciais na imagem | Todos os segredos via env vars; `.dockerignore` exclui `.env*` |

### V16 – Security Logging

| Req | Descrição | Implementação |
|-----|-----------|---------------|
| V16.2.5 | Sem credenciais/tokens em logs | Debug `println` removido; `HaveIBeenPwnedClient` usa `log.warn` |
| V16.3.1 | Eventos de autenticação logados | `SecurityAuditLogger`: auth success/fail/block em cada evento |
| V16.3.1 | Operações de ficheiros logadas | `logFileOperation()` em `FileSystemService` — create/write/read/delete |
| V16.3.1 | Operações de compras logadas | `logPurchaseOperation()` em `PurchaseService` — create/update/delete |
| V16.3.2 | Eventos HTTP 403 logados | `logAccessDenied()` em `SecurityConfig` |
| V16.4.1 | Prevenção de log injection | `sanitize()` remove `\r\n\t` de todos os valores logados |
| V16.5.1 | Sem erros internos nas respostas | `GlobalExceptionHandler` retorna `"An unexpected error occurred."` |
| V16.5.3 | Global exception handler | `@RestControllerAdvice GlobalExceptionHandler` |

---

## 3. Traceabilidade ASVS → Testes Automatizados

### 3.1 Testes unitários de segurança

| Req ASVS | SDR | Teste Automatizado |
|----------|-----|-------------------|
| V6.2.1 (min 12 chars) | SDR05 | `PasswordPolicyServiceTest.rejectsPasswordShorterThanMinimum` |
| V6.2.9 (sem truncação) | SDR05 | `PasswordPolicyServiceTest.rejectsPasswordLongerThanMaximum` |
| V6.3.1 (rate limit) | SDR04 | `SimpleRateLimiterTest.fifthFailureBlocksKey`, `resetUnblocksKey` |
| V7.4.1 (logout) | SDR03a | `TokenBlocklistTest.blockedTokenReturnsTrue`, `expiredEntryIsNoLongerBlocked` |
| V16.3.1 (auth events) | SDR19 | `SecurityAuditLoggerTest.authenticationSuccessIsLoggedAtInfoLevel` |
| V16.4.1 (log injection) | SDR19b | `SecurityAuditLoggerTest.logInjectionAttemptIsSanitised` |

### 3.2 Testes de integração e arquitectura

| Req ASVS | SDR | Teste Automatizado |
|----------|-----|-------------------|
| V8.1.1 (RBAC HTTP) | SDR02 | `AuthenticationIntegrationTest.testProtectedEndpointRequiresJwt` |
| V12.2.1 (HTTPS) | SDR10 | `SecureChannelIntegrationTest.testHttpsRedirect` |
| V14 (arquitectura em camadas) | — | `ArchitectureTest.controller_dont_access_repos` |

### 3.3 Verificação ao nível do pipeline

| Req ASVS | Gate CI |
|----------|---------|
| V15.2.1 (sem deps vulneráveis) | OWASP Dependency-Check em `sca.yml` — falha em CVSS ≥ 7 |
| V4.x, V16.x (SAST) | CodeQL + SpotBugs/FindSecBugs em `sast.yml` |
| V4.x, V6.x, V8.x (DAST) | OWASP ZAP baseline em `dast.yml` |
| V15.x (atualizações de deps) | Dependabot + `dependency-review.yml` em PRs |

---

## 4. Gaps Conhecidos

| Capítulo | Gap | Risco | Plano |
|----------|-----|-------|-------|
| V8.3 | `@PreAuthorize` só em controller — não há verificação de ownership ao nível do serviço | Baixo | Hardening futuro |
| V9.2.1 | JWT usa HS256 (simétrico) — RS256 assimétrico previsto no design | Médio | Requer gestão de keypair; adiado |
| V16.3 | Operações de listDirectory não geram evento de audit | Baixo | Tradeoff de ruído vs. utilidade |

> Para o detalhe requisito a requisito, abrir [`ASVS_5_0_Tracker.xlsx`](../../ASVS_5_0_Tracker.xlsx).
