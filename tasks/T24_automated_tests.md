# T24 — Automated tests

**Read ONLY:** docs/12 (Automated)

**Files you will touch:**
- `app/src/test/*`
- `app/src/androidTest/*`

**Steps:**
1. Write the unit/DB/UI tests listed in doc 12.
2. CI workflow runs unit tests.

**Acceptance (all must be true):**
- [ ] `./gradlew testDebugUnitTest` green

**Always:** build with `./gradlew :app:assembleDebug`, tick `PROGRESS.md`, commit `"T24: <title>"`. Stuck 5 times on one error -> `BLOCKERS.md`, next task.
