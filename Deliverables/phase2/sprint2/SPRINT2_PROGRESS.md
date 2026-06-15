# Sprint 2 — Estado do Trabalho (Phase 2)

> Documento para partilhar com a equipa. Última atualização: **2026-06-15**.
> Submissão: **16/06/2026**.

## TL;DR

Temos a **pipeline de CI/CD segura, orquestrada e com gates a sério** (SAST + SCA +
DAST + mutation + coverage), **255 testes** verdes, **hardening** das GitHub Actions e
um modelo de **branching GitFlow-lite com branch protection**. O colega adicionou
**audit logging**, **`@PreAuthorize`** (RBAC ao nível do método), o **ASVS checklist**
e o doc de **Development**.

Falta sobretudo: **README do Sprint 2**, **rastreabilidade ASVS→testes**, docs curtos de
**Production/Operate** e os **FRs restantes**.

---

## 1. O que está feito (mapeado à rubrica §6.3 do PDF)

| Bucket (peso) | Feito ✅ |
|---|---|
| **Build & Test (35%)** | Pipeline orquestrada fail-fast · SAST (CodeQL + SpotBugs/FindSecBugs) · SCA (OWASP Dependency-Check + Dependency-Review + Dependabot) · DAST (OWASP ZAP) · Mutation (PITest) · Coverage JaCoCo **91%** · **255 testes** · hardening Actions · triagem em `security_findings.md` |
| **Development (35%)** | 3 agregados (DDD) · 3 roles (ADMIN/EMPLOYEE/CLIENT) · OS file ops (`FileSystemService`) · **`@PreAuthorize`** · **audit logging** (auth + ficheiros + compras) · `development.md` |
| **ASVS (15%)** | `asvs_checklist.md` |
| **Organization (5%)** | Estrutura do repo · `Deliverables/` · `CONTRIBUTING.md` |
| **Production (5%)** | Logging/traceability (audit logger) · patch mgmt (Dependabot) |
| **Operate (5%)** | Monitoring (`/actuator/health`) · vuln mgmt (Dependabot + Dependency-Check) · pentest (ZAP) |

---

## 2. A pipeline de CI/CD — `.github/workflows/pipeline.yml`

Os **5 workflows separados** foram consolidados numa **pipeline única orquestrada**, com
dependências explícitas entre etapas (**fail-fast**):

```
build-test ─┬─ sast-codeql ───────┐
            ├─ sast-spotbugs ──────┼─ dast (push/main) ─ summary ─ deploy (main)
            ├─ sca-dependency-check┘
            ├─ sca-dependency-review (só PR)
            └─ mutation (só não-PR)
```

**Comportamento por evento:**

| Evento | Corre | Salta |
|---|---|---|
| **Pull Request** | build+testes+coverage + SAST + SCA | DAST, mutation (caros) |
| **Push/merge `develop`/`main`** | tudo + DAST + mutation | dependency-review (só PR) |
| **Push para `main`** | tudo + `deploy` (build da imagem) | — |
| **Só docs** (`*.md`, `Deliverables/**`) | nada (`paths-ignore`) | tudo |

Se `build-test` falha, **nada a jusante corre**; o DAST só arranca depois de SAST+SCA verdes.

**Vulnerabilidades de GitHub Actions corrigidas:**
- Todas as actions **fixadas por commit SHA** (anti supply-chain / tag-hijacking).
- **`permissions: contents: read`** (least-privilege) + elevação só onde é preciso.
- **`concurrency`** (cancela runs antigos) · **`persist-credentials: false`** nos checkouts.

---

## 3. Novo fluxo de Git — IMPORTANTE para todos

`main` e `develop` estão **protegidas**: já **não dá `git push` direto**. Tudo passa por
branch + PR (1 review + `Pipeline Summary` verde).

```bash
git checkout develop && git pull origin develop
git checkout -b feature/o-que-vais-fazer      # ou fix/...
# ... trabalhar e commitar ...
git push -u origin feature/o-que-vais-fazer
gh pr create --base develop --fill            # ou pelo GitHub
# 1 review de outro colega + pipeline verde -> Merge
```

Se já tens commits locais na `main`:
```bash
git checkout -b feature/o-meu-trabalho        # leva os commits
git push -u origin feature/o-meu-trabalho
git checkout main && git reset --hard origin/main
```

Detalhes completos em **`CONTRIBUTING.md`**.

---

## 4. Gates / qualidade (tudo hard-fail, sem "verdes falsos")

| Gate | Valor | Onde |
|---|---|---|
| Cobertura (JaCoCo) | falha < **90%** (atual ~91%) | `pom.xml` |
| Mutation (PITest) | falha < **60%** (atual 68%) | `pom.xml` — estava partido (plugin antigo), corrigido |
| SAST (SpotBugs) | falha em qualquer bug não-justificado | `spotbugs:check` + `config/spotbugs-exclude.xml` |
| SCA (Dependency-Check) | falha em **CVSS ≥ 7** | `pom.xml` |
| DAST (ZAP) | falha acima do threshold | `pipeline.yml` |

Triagem completa dos findings de segurança (93 do SpotBugs → 3 corrigidos em código,
resto informativo/risco-aceite documentado) em **`security_findings.md`**.

---

## 5. O que falta (lista priorizada)

1. **Merge do PR `ci/secure-pipeline`** para a `main` (pipeline + gates + fixes).
2. **`README.md` do Sprint 2** — preencher e ligar todos os deliverables (Organization 5%; o PDF pede "all components linked to a main document").
3. **ASVS** — rastreabilidade requisito→teste no `asvs_checklist.md` (15%).
4. **Docs curtos Production + Operate** (5%+5%) — já temos a substância, é redigir.
5. **FRs restantes** (Development): Purchase.status lifecycle, relatórios de vendas, filtro alergénio, Dish.description, Ingredient.stock.
6. *(Opcional, alto valor)* suite **BDD Cucumber/Gherkin** de segurança — conta como "and more" no Build&Test **e** evidência ASVS.
7. *(Housekeeping)* `.DS_Store` → `.gitignore`.

---

## 6. Como correr localmente

```bash
cd project
mvn verify                 # build + testes + coverage gate
mvn spotbugs:check         # SAST
mvn test-compile org.pitest:pitest-maven:mutationCoverage   # mutation
```

⚠️ **Nota de ambiente:** o SpotBugs (4.8.3.1) e o PITest **não correm em JDK 25** localmente
(erro "Unsupported class file major version"). Usar **JDK 17 ou 21** localmente
(a CI usa JDK 17). Em macOS: `export JAVA_HOME=$(/usr/libexec/java_home -v 21)`.
