# T00 — Environment, Gradle wrapper, first build attempt

**Read ONLY:** docs/13_BUILD_ENVIRONMENT.md

**Files you will touch:**
- `scripts/setup_termux.sh (optional)`
- `project/gradle.properties`
- `project/local.properties (create, git-ignored)`

**Steps:**
1. Read doc 13. Choose Option A, B or C (A recommended).
2. Run `cd project && gradle wrapper --gradle-version 8.10.2` so `./gradlew` exists.
3. Create `local.properties` with `sdk.dir=` if building locally.
4. Run `./gradlew :app:assembleDebug` once and save output to `build.log`.
5. Do NOT fix code errors here — just record the first 20 `e:` lines in `BLOCKERS.md` for T01.

**Acceptance (all must be true):**
- [ ] `./gradlew --version` works (or CI workflow file is pushed)
- [ ] `build.log` exists
- [ ] Environment choice written at top of PROGRESS.md

**Always:** build with `./gradlew :app:assembleDebug`, tick `PROGRESS.md`, commit `"T00: <title>"`. Stuck 5 times on one error -> `BLOCKERS.md`, next task.
