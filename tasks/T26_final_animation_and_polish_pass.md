# T26 — Final animation + polish pass

**Read ONLY:** docs/05 (entire); docs/03 (3.8 hard rules)

**Files you will touch:**
- `ui/**`

**Steps:**
1. Walk doc-05 table row by row; implement/adjust until each row is present.
2. Check haptics, spring values, durations scale with animation speed setting.
3. Fix any visual misalignments; run full QA matrix.

**Acceptance (all must be true):**
- [ ] All 28 motion rows verified; QA matrix passes

**Always:** build with `./gradlew :app:assembleDebug`, tick `PROGRESS.md`, commit `"T26: <title>"`. Stuck 5 times on one error -> `BLOCKERS.md`, next task.
