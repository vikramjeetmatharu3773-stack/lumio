# T03 — Database verification, schema export, DAO tests

**Read ONLY:** docs/10_DATABASE_SCHEMA.md

**Files you will touch:**
- `data/db/*`
- `app/schemas/ (generated)`
- `app/src/test/.../DbTest.kt`

**Steps:**
1. Confirm KSP generates `schemas/1.json`.
2. Write in-memory Room tests: upsert songs, favorites survive `songs` replace, playlist cascade delete, history stats queries.
3. Add `Converters` only if needed (none expected).
4. Add index review: searching 10k songs must stay in memory (no SQL LIKE).

**Acceptance (all must be true):**
- [ ] Tests pass: `./gradlew :app:testDebugUnitTest`
- [ ] schema JSON committed

**Always:** build with `./gradlew :app:assembleDebug`, tick `PROGRESS.md`, commit `"T03: <title>"`. Stuck 5 times on one error -> `BLOCKERS.md`, next task.
