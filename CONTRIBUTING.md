# Contributing & Branching Strategy

This project follows a **GitFlow-lite** model. The CI/CD pipeline
(`.github/workflows/pipeline.yml`) is **branch-aware**: the work each branch
triggers is matched to its risk, so day-to-day pull requests stay fast while
integration and release branches get the full security pipeline.

## Branches

| Branch | Purpose | Who writes to it |
|--------|---------|------------------|
| `main` | Release-ready code. Always green, always deployable. | Only via PR from `develop`. |
| `develop` | Integration branch. Features land here first. | Only via PR from `feature/*` / `fix/*`. |
| `feature/<short-name>` | New functionality. Short-lived. | Branched from `develop`. |
| `fix/<short-name>` | Bug/security fixes. Short-lived. | Branched from `develop` (or `main` for hotfixes). |

```
 feature/*  ─┐
 fix/*       ├──PR──►  develop ───────PR──►  main ──► deploy
             │
        FAST gate     FULL pipeline       FULL pipeline + release image
```

## What the pipeline runs per event

| Event | Stages that run | Skipped |
|-------|-----------------|---------|
| **PR → `develop`/`main`** | build-test, SAST (CodeQL + SpotBugs), SCA (Dependency-Check + Dependency-Review) | DAST, mutation (too expensive for every PR) |
| **push/merge → `develop`** | build-test, SAST, SCA, **mutation**, **DAST** | dependency-review (PR-only) |
| **push/merge → `main`** | everything above **+ `deploy`** (build release image) | dependency-review |
| **scheduled (weekly)** | build-test, SAST, SCA, mutation (refreshes CVE findings) | DAST, dependency-review |
| **docs-only change** | _nothing_ (`paths-ignore`) | all |

Stages are chained with `needs:` so the pipeline is **fail-fast**: if
`build-test` (which includes the JaCoCo coverage gate) fails, nothing
downstream runs; `dast` only starts once every SAST/SCA stage is green.

## Pull request workflow

1. Branch off `develop`: `git checkout develop && git pull && git checkout -b feature/my-thing`.
2. Commit small, focused changes. Push the branch.
3. Open a PR targeting `develop`. The **fast gate** runs automatically.
4. Get **at least one review** and a green `pipeline-summary` before merging.
5. Periodically, `develop` is promoted to `main` via PR → triggers the release/deploy stage.

## Branch protection (configured on the remote)

Both `main` and `develop` are protected:

- Require a pull request before merging (no direct pushes).
- Require the **`Pipeline Summary`** status check to pass (it aggregates every
  required stage — mark only this one as required, not DAST/mutation, since
  those don't run on PRs).
- Require at least **1 approving review**.
- Dismiss stale approvals when new commits are pushed.

> The exact `gh` commands used to create `develop` and apply these rules are in
> the Sprint 2 deliverable.

## Commit messages

Use short, conventional prefixes: `feat:`, `fix:`, `test:`, `docs:`, `ci:`,
`refactor:`, `chore:`. Keep the subject line under ~72 characters.
