# ASVS Checklist – Phase 2 Sprint 2

> **Documento primário:** [`Deliverables/ASVS_5_0_Tracker.xlsx`](../../ASVS_5_0_Tracker.xlsx)
>
> O tracker OWASP ASVS 5.0 é mantido em Excel com rastreabilidade completa por requisito. Este ficheiro é o resumo legível do Sprint 2. Para o detalhe por requisito, consultar o Excel.

---

## 1. Resumo por Capítulo (Sprint 1 + Sprint 2 acumulado)

| Capítulo | Compliant | In Progress | Not Applicable | Not Started |
|----------|-----------|-------------|----------------|-------------|
| V1 – Encoding and Sanitisation | 2 | 0 | 17 | 11 |
| V2 – Validation and Business Logic | 4 | 5 | 1 | 3 |
| V3 – Web Frontend Security | 5 | 0 | 26 | 0 |
| V4 – API and Web Service | 7 | 1 | 8 | 0 |
| V5 – File Handling | 4 | 0 | 9 | 0 |
| V6 – Authentication | 13 | 0 | 27 | 7 |
| V7 – Session Management | 6 | 1 | 5 | 7 |
| V8 – Authorization | 5 | 3 | 4 | 1 |
| V9 – Self-contained Tokens | 4 | 0 | 3 | 0 |
| V10 – OAuth and OIDC | 0 | 0 | 36 | 0 |
| V11 – Cryptography | 10 | 4 | 8 | 2 |
| V12 – Secure Communication | 2 | 1 | 2 | 7 |
| V13 – Configuration | 5 | 1 | 3 | 12 |
| V14 – Data Protection | 5 | 0 | 4 | 4 |
| V15 – Secure Coding and Architecture | 11 | 0 | 1 | 9 |
| V16 – Security Logging and Error Handling | 9 | 2 | 0 | 6 |
| V17 – WebRTC | 0 | 0 | 12 | 0 |

---

## 2. Controlos Novos do Sprint 2

### V1 – Encoding and Sanitisation

| Req | Descrição | Implementação |
|-----|-----------|---------------|
| V1.1.1 | Validação de todos os dados controlados pelo utilizador | `@Valid` + `@Pattern` / `@Size` em todas as DTOs; `resolveSafe()` em `FileSystemService` |
| V1.5.1 | Validação próxima do input | `@Valid` na fronteira do controller, antes da lógica de serviço |

### V2 – Validation and Business Logic

| Req | Descrição | Implementação |
|-----|-----------|---------------|
| V2.2.1 | Input validado contra expectativas de negócio | `@Valid`/`@Pattern`/`@Size` nas DTOs + regras de domínio: data futura (`Purchase`/`Menu`), prato no menu do dia, saldo suficiente |
| V2.2.2 | Validação numa camada de serviço de confiança | Validação server-side na fronteira (`@Valid`) e nas entidades de domínio (`User.deductBalance`, construtores `Purchase`/`Menu`) — nunca só no cliente |
| V2.2.3 | Combinações de dados relacionados coerentes | Prato tem de pertencer ao menu da data; tipos de prato (meat/fish/vegetarian) validados no construtor de `Menu` |
| V2.3.3 | Transações ao nível da lógica de negócio | `PurchaseService` é `@Transactional` — operação completa-se na totalidade ou reverte; coberto por TC39 |

### V3 – Web Frontend Security

> Backend REST puro (sem frontend HTML/DOM/cookies) → 26 requisitos N/A. Os 5 Compliant são headers HTTP / política de origem.

| Req | Descrição | Implementação |
|-----|-----------|---------------|
| V3.4.1 | HSTS | nginx `Strict-Transport-Security: max-age=31536000; includeSubDomains` + Spring `httpStrictTransportSecurity` (1 ano, subdomínios) |
| V3.4.2 | CORS allowlist | `CorsConfiguration.setAllowedOrigins(app.cors.allowed-origins)` — allowlist, nunca `*` |
| V3.4.4 | `X-Content-Type-Options: nosniff` | Default do Spring Security (não desativado) |
| V3.5.1 | Anti-CSRF | API stateless; proteção via header `Authorization: Bearer` (não-safelisted por CORS) |
| V3.5.3 | Métodos HTTP corretos | POST/PUT/DELETE para alterar estado; GET só leitura |

### V4 – API and Web Service

| Req | Descrição | Implementação |
|-----|-----------|---------------|
| V4.1.1 | Content-Type correto com charset | `@RestController` + Jackson → `application/json` (UTF-8) em todas as respostas com corpo |
| V4.1.3 | Headers de intermediário não sobreponíveis (*In Progress*) | App confia em `X-Forwarded-Proto` atrás do nginx; pressuposto de deployment (só alcançável via nginx), sem allowlist de proxies ainda |
| V4.1.4 | Apenas métodos HTTP suportados | Spring MVC devolve `405` para métodos não mapeados; cada endpoint declara os métodos |
| V4.2.1–V4.2.4 | Anti request-smuggling / header injection | Tomcat embebido (HTTP/1.1 via nginx) faz framing correto e rejeita CR/LF em headers |
| V4.2.5 | Limites de URI/header em pedidos outbound | Única chamada externa é HIBP com prefixo SHA-1 de 5 chars (comprimento fixo) |

### V5 – File Handling

| Req | Descrição | Implementação |
|-----|-----------|---------------|
| V5.1.1 | Tipos/extensões de ficheiro permitidos | `@Pattern` em `FileWriteRequestDTO.path` restringe extensões e estrutura de caminho |
| V5.3.1 | Ficheiros não executados como código server-side | `basePath` dedicado (não é web root nem servido estaticamente); conteúdo devolvido em JSON via controller |
| V5.3.2 | Paths de ficheiro a partir de dados de confiança | `FileSystemService.resolveSafe()` confina ao `basePath` + `@Pattern` bloqueia `..` |
| V5.4.1 | Filenames submetidos validados/ignorados | Allowlist `@Pattern` em `FileWriteRequestDTO` e `DirectoryCreateRequestDTO` |

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
| V6.2.2 | Utilizador muda a própria password | `PUT /api/users/me` → `UserService.updateUser` (policy + HIBP) |
| V6.2.5 | Sem regras de composição que bloqueiem | `PasswordPolicyService` valida só comprimento (12–128) |
| V6.2.10 | Sem rotação periódica forçada | Password válida até rotação pelo utilizador ou deteção de breach |
| V6.2.12 | Breach check no registo e na mudança | `HaveIBeenPwnedClient` em `createUser` **e** `updateUser` |
| V6.3.4 | Caminho de autenticação único e documentado | Login username/password → JWT; sem pathways não documentados |

> Gaps conhecidos (Not Started): banlist de palavras de contexto (V6.1.2/V6.2.11), mudança de password não exige a atual (V6.2.3), sem notificação ao utilizador (V6.3.5/V6.3.7), sem fluxo de reset (V6.4.3/V6.4.6).

### V7 – Session Management

| Req | Descrição | Implementação |
|-----|-----------|---------------|
| V7.3.1 | Timeout absoluto de sessão | `jwt.expiration=3600` (1 hora) |
| V7.3.2 | Timeout enforced | Verificado em cada pedido em `JwtRequestFilter` |
| V7.4.1 | Logout invalida token | `POST /api/auth/logout` → `TokenBlocklist.block()`; filtro rejeita tokens bloqueados |
| V7.1.1 | Tempo de vida da sessão documentado | `jwt.expiration=3600` (1h) em `application.properties` + NFR03 |
| V7.2.1 | Verificação do token no backend | Assinatura JWT (HS256) validada server-side em cada pedido (`JwtRequestFilter`) |
| V7.2.2 | Tokens auto-contidos dinâmicos | JWT HS256 gerado a cada login; sem segredos/chaves estáticos como token |
| V7.2.4 | Novo token na autenticação (*In Progress*) | Novo JWT por login; token anterior só termina por expiração ou `TokenBlocklist` (stateless) |

### V8 – Authorization

| Req | Descrição | Implementação |
|-----|-----------|---------------|
| V8.1.1 | RBAC ao nível de função, por rota | `SecurityConfig.filterChain()` por método HTTP e path (ADMIN/EMPLOYEE/CLIENT) |
| V8.2.2 | Acesso a dados / anti-IDOR (BOLA) | `PurchaseController` compara `clientId`/`clientUsername` com `auth.getName()` → 403 se não for dono |
| V8.2.3 | Acesso ao nível de campo (BOPLA) | `UserService.convertToDTO` só devolve externalId/username/type/balance; nunca password hash nem ID interno |
| V8.3.1 | Autorização em camada de confiança | `SecurityConfig` + `@PreAuthorize` + ownership pelo principal autenticado (não por valores do cliente) |
| V8.3.3 | Acesso pelo subject autenticado | Ownership verificada contra o JWT sub / `auth.getName()`, não por IDs fornecidos pelo cliente |

> In Progress: V8.1.2 (docs de campo), V8.2.1 (RBAC de serviço parcial), V8.3.2 (role change imediato; desativação ainda não). Not Started: V8.4.2 (step-up admin).

### V9 – Self-contained Tokens

| Req | Descrição | Implementação |
|-----|-----------|---------------|
| V9.1.1 | MAC verificado antes de aceitar claims | `parserBuilder().setSigningKey(key).parseClaimsJws()` (HS256); assinatura inválida rejeitada |
| V9.1.2 | Allowlist de algoritmos, sem `none` | Algoritmo único HS256; JJWT rejeita `alg:none` e key confusion |
| V9.1.3 | Chave de fonte pré-configurada | `jwt.secret` via `hmacShaKeyFor`; nunca derivada de `jku`/`x5u`/`jwk` |
| V9.2.1 | Validade temporal (`exp`) validada | `JwtTokenUtil.isTokenExpired()` verificado em cada pedido |

> N/A: V9.2.2/V9.2.3/V9.2.4 — token único, serviço único, audiência única. (RS256 seria melhoria, não gap.)

### V12 – Secure Communication

| Req | Descrição | Implementação |
|-----|-----------|---------------|
| V12.1.1 | TLS 1.2/1.3 enforced | nginx reverse proxy `ssl_protocols TLSv1.2 TLSv1.3` (`deploy/nginx/conf.d/cafeteria.conf`) |
| V12.2.1 | HTTP rejeitado / redirecionado | nginx porta 80 → `301 https://` redirect |

> ⚠️ V12.1.2 está **In Progress**, não Compliant: o nginx **não define `ssl_ciphers`** (usa o default). TLS 1.3 garante forward secrecy, mas o default de TLS 1.2 ainda permite cifras CBC. Gaps de deployment (Not Started): app↔PostgreSQL sem TLS, sem OCSP stapling, cert self-signed em dev.

### V13 – Configuration

| Req | Descrição | Implementação |
|-----|-----------|---------------|
| V13.4.1 | `.dockerignore` exclui `.git` | `project/.dockerignore`: exclui `.git`, `target/`, `.env*` |
| V13.4.2 | Sem stack traces nas respostas | `GlobalExceptionHandler` retorna mensagem genérica |
| V13.4.5 | Actuator restrito | `management.endpoints.web.exposure.include=health`; `/actuator/**` negado |
| V13.2.3 | Sem credenciais default na BD | PostgreSQL com utilizador dedicado `cafeteria` (não `postgres/postgres`) |
| V13.4.4 | HTTP TRACE desativado | Default do Tomcat embebido (`allowTrace=false`) |
| V13.3.1 | Segredos via env vars (*In Progress*) | `JWT_SECRET`/`SPRING_DATASOURCE_PASSWORD`; `.env*` em `.gitignore`+`.dockerignore`; defaults dev-only por substituir em prod |

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

| Req ASVS | SDR / TC | Teste Automatizado |
|----------|----------|-------------------|
| V2 (lógica de negócio / integridade do saldo) | FR17b, TC39 | `UserTest.deductBalance_insufficient_throws`, `PurchaseServiceTest.createPurchase_insufficientBalance_isRejectedAndNothingPersisted` |
| V2 (EMPLOYEE não tem saldo nem compra) | FR17c | `UserTest.deductBalance_employee_throws`, `addBalance_employee_throws` |
| V2 (prato tem de estar no menu do dia) | — | `PurchaseServiceTest.createPurchase_dishNotInMenu_throws` |
| V4 (input inválido → 400) | SDR06 | `GlobalExceptionHandlerTest.validationException_returns400WithFieldErrors`, `illegalArgument_returns400` |
| V5 (path traversal bloqueado) | SDR13, TC15 | `FileSystemServiceTest.pathTraversal_isRejected` |
| V5 (tamanho máximo de ficheiro 1 MB) | SDR14, TC16 | `FileSystemServiceTest.writeFile_exceedingMaxSize_throws` |
| V6.2.1 (min 12 chars) | SDR05 | `PasswordPolicyServiceTest.rejectsPasswordShorterThanMinimum` |
| V6.2.9 (sem truncação) | SDR05 | `PasswordPolicyServiceTest.rejectsPasswordLongerThanMaximum` |
| V6.2.8 (comparação correta de password) | SDR05a, TC03 | `AuthControllerTest.login_badPassword_throwsBadCredentials` |
| V6.3.1 (rate limit) | SDR04, TC01/TC04 | `SimpleRateLimiterTest.fifthFailureBlocksKey`, `resetUnblocksKey`, `AuthControllerTest.login_alreadyRateLimited_returns429` |
| V7.3.1 (expiração de token) | SDR01, TC22 | `JwtTokenUtilTest.expiredToken_isRejectedOnParse` |
| V7.4.1 (logout) | SDR03a, TC26 | `TokenBlocklistTest.blockedTokenReturnsTrue`, `expiredEntryIsNoLongerBlocked`, `JwtRequestFilterTest.blockedToken_isRejected` |
| V8 (identidade do JWT, anti-IDOR) | SDR02, TC28 | `UserControllerTest.getCurrentUser_found`, `updateCurrentUser_success` |
| V8.2.2 (ownership, anti-IDOR/BOLA) | SDR02, TC09/TC10/TC14 | `PurchaseControllerTest.getPurchasesByClient_asClient_otherData_forbidden`, `createPurchase_asClient_forAnotherUser_forbidden`, `updatePurchase_asClient_otherUsersPurchase_forbidden`, `deletePurchase_asClient_otherUsersPurchase_forbidden` |
| V15.3.3 / V8.2.3 (mass assignment — gap F2) | — | `UserControllerTest.updateCurrentUser_forwardsClientControlledTypeAndBalance` (caracteriza o bug: `/me` propaga `type`/`balance` do cliente) |
| V9 (token só válido p/ utilizador certo) | SDR01, TC02 | `JwtTokenUtilTest.validateToken_falseForDifferentUser`, `JwtRequestFilterTest.invalidToken_isNotAuthenticated` |
| V16.3.1 (auth events) | SDR19, TC32 | `SecurityAuditLoggerTest.authenticationSuccess/Failure/BlockedIsLogged...` |
| V16.4.1 (log injection) | SDR19b | `SecurityAuditLoggerTest.logInjectionAttemptIsSanitised` |
| V16.5.1 (erro genérico sem leak) | SDR12 | `GlobalExceptionHandlerTest.genericException_returns500WithSafeMessage`, `dataIntegrity_unknownConstraint_genericMessage` |
| V16.3.2 (403 auditado) | SDR20a | `GlobalExceptionHandlerTest.accessDenied_returns403AndIsLogged` |

### 3.2 Testes de integração e arquitectura

| Req ASVS | SDR | Teste Automatizado |
|----------|-----|-------------------|
| V8.1.1 (RBAC HTTP) | SDR02 | `AuthenticationIntegrationTest.testProtectedEndpointRequiresJwt` |
| V12.2.1 (HTTPS redirect) | SDR10 | `SecureChannelIntegrationTest.testHttpsRedirect` |
| V12.2.x (X-Forwarded-Proto) | SDR10 | `SecureChannelIntegrationTest.testXForwardedProtoHttpsAccepted` |
| V14 (arquitectura em camadas) | — | `ArchitectureTest.controller_dont_access_repos` (+ 3 regras ArchUnit) |

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
| V2 (lógica de negócio) | `PurchaseService.createPurchase` deduz o saldo **antes** de construir `Purchase`, e o construtor re-valida `hasEnoughBalance(price)` sobre o saldo já reduzido → um cliente precisa de ≥ 2×preço para comprar. Não manifesta na demo porque os pratos seeded têm preço 0.00. O `@Transactional` garante que a dedução é revertida, mas a compra falha indevidamente. | Médio | Corrigir ordem (validar antes de deduzir, ou remover a re-validação no construtor); só então adicionar o teste de rollback transacional verdadeiro (deduzir → falhar → reverter) |
| V15.3.3 / V8.2.3 | `PUT /api/users/me` (`UserController.updateCurrentUser`) só força o `username`; deixa o cliente enviar `type` e `balance` no corpo, aplicados por `UserService.updateUser`. Um **CLIENT** pode escalar-se a **ADMIN** e definir o próprio **saldo** (mass assignment / escalada de privilégios). | Alto | No `/me` ignorar `type`/`balance` do payload (preservar valores atuais) ou usar DTO de self-update sem esses campos |

> Para o detalhe requisito a requisito, abrir [`ASVS_5_0_Tracker.xlsx`](../../ASVS_5_0_Tracker.xlsx).
