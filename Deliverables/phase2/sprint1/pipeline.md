# Pipeline Automation – Phase 2 Sprint 1

## 1. Overview

The CI/CD pipeline automates all quality and security gates on every push to the repository. It is implemented with **GitHub Actions** and runs on the `main` branch and on all Pull Requests.

**Status:** ⬜ Pipeline definition in progress — to be committed to `.github/workflows/`

---

## 2. Pipeline Stages

The pipeline is divided into three workflows:

| Workflow file | Trigger | Stages |
|--------------|---------|--------|
| `ci.yml` | Push / PR to `main` | Build → Unit Tests → Coverage → SAST → SCA |
| `dast.yml` | Push to `main` (after `ci.yml`) | Docker Compose up → ZAP scan → Report |
| `dependency-review.yml` | Pull Request | Dependency diff review (GitHub native) |

---

## 3. CI Workflow (`ci.yml`)

### Stages

```
push / PR
    │
    ▼
┌─────────────────┐
│  1. Build       │  mvn compile -B
└────────┬────────┘
         │
    ▼
┌─────────────────┐
│  2. Test        │  mvn test -B
│  + Coverage     │  JaCoCo report uploaded as artefact
└────────┬────────┘
         │
    ▼
┌─────────────────┐
│  3. SAST        │  mvn spotbugs:check (Find Security Bugs)
│                 │  Report uploaded as artefact
└────────┬────────┘
         │
    ▼
┌─────────────────┐
│  4. SCA         │  mvn dependency-check:check
│                 │  Fails on CVSS ≥ 7
│                 │  Report uploaded as artefact
└─────────────────┘
```

### Workflow definition (planned)

```yaml
# .github/workflows/ci.yml
name: CI

on:
  push:
    branches: [ main ]
  pull_request:
    branches: [ main ]

jobs:
  build-and-test:
    runs-on: ubuntu-latest
    defaults:
      run:
        working-directory: project

    steps:
      - uses: actions/checkout@v4

      - name: Set up Java 17
        uses: actions/setup-java@v4
        with:
          java-version: '17'
          distribution: 'temurin'
          cache: maven

      - name: Build
        run: mvn compile -B

      - name: Test + Coverage
        run: mvn verify -B
        env:
          JWT_SECRET: ${{ secrets.JWT_SECRET_TEST }}

      - name: Upload JaCoCo report
        uses: actions/upload-artifact@v4
        with:
          name: jacoco-report
          path: project/target/site/jacoco/

  sast:
    runs-on: ubuntu-latest
    needs: build-and-test
    defaults:
      run:
        working-directory: project

    steps:
      - uses: actions/checkout@v4

      - name: Set up Java 17
        uses: actions/setup-java@v4
        with:
          java-version: '17'
          distribution: 'temurin'
          cache: maven

      - name: SAST — SpotBugs + Find Security Bugs
        run: mvn spotbugs:check -B

      - name: Upload SpotBugs report
        if: always()
        uses: actions/upload-artifact@v4
        with:
          name: spotbugs-report
          path: project/target/spotbugsXml.xml

  sca:
    runs-on: ubuntu-latest
    needs: build-and-test
    defaults:
      run:
        working-directory: project

    steps:
      - uses: actions/checkout@v4

      - name: Set up Java 17
        uses: actions/setup-java@v4
        with:
          java-version: '17'
          distribution: 'temurin'
          cache: maven

      - name: SCA — OWASP Dependency-Check
        run: mvn dependency-check:check -B
        env:
          NVD_API_KEY: ${{ secrets.NVD_API_KEY }}

      - name: Upload Dependency-Check report
        if: always()
        uses: actions/upload-artifact@v4
        with:
          name: dependency-check-report
          path: project/target/dependency-check-report.html
```

---

## 4. DAST Workflow (`dast.yml`)

```yaml
# .github/workflows/dast.yml
name: DAST

on:
  push:
    branches: [ main ]

jobs:
  zap-scan:
    runs-on: ubuntu-latest
    needs: ci  # only runs after CI passes

    steps:
      - uses: actions/checkout@v4

      - name: Start Docker Compose stack
        run: docker compose up --build -d
        env:
          JWT_SECRET: ${{ secrets.JWT_SECRET_TEST }}

      - name: Wait for application to be ready
        run: |
          for i in {1..30}; do
            curl -sf http://localhost:8081/actuator/health && break
            sleep 5
          done

      - name: Run OWASP ZAP baseline scan
        uses: zaproxy/action-baseline@v0.12.0
        with:
          target: 'http://localhost:8081'
          rules_file_name: '.zap/rules.tsv'
          fail_action: false

      - name: Upload ZAP report
        uses: actions/upload-artifact@v4
        with:
          name: zap-report
          path: report_html.html

      - name: Stop stack
        if: always()
        run: docker compose down -v
```

---

## 5. Repository Secrets Required

| Secret | Purpose | How to generate |
|--------|---------|----------------|
| `JWT_SECRET_TEST` | JWT signing key used in CI test context | `openssl rand -hex 32` |
| `NVD_API_KEY` | NVD API key to speed up OWASP Dependency-Check | Register at nvd.nist.gov |

Secrets are configured in **GitHub → Settings → Secrets and variables → Actions**.

---

## 6. Pipeline Results

> This section will be updated with pipeline run links and artefact summaries after the first successful execution.

| Run | Date | Build | Tests | SAST | SCA | DAST |
|-----|------|-------|-------|------|-----|------|
| — | — | — | — | — | — | — |

---

## 7. Branch Protection Rules

To enforce the pipeline as a gate before merge, the following branch protection rules are configured on `main`:

- Require status checks to pass before merging: `build-and-test`, `sast`, `sca`
- Require at least 1 approving review on Pull Requests
- Dismiss stale reviews when new commits are pushed
- Do not allow force pushes to `main`
