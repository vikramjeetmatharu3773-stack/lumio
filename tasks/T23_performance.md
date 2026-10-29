# T23 — Performance, accessibility, localization

**Read ONLY:** docs/12 (Performance budgets, A11y rows)

**Files you will touch:**
- `whole app`

**Steps:**
1. Profile cold start + scrolling; fix hot spots (stable params, remember, derivedStateOf).
2. contentDescription on every icon button; 48 dp targets; TalkBack pass.
3. Move all strings to resources; add hi + pa translations scaffolds.
4. Test 200 % font scale.

**Acceptance (all must be true):**
- [ ] Budgets in doc 12 met

**Always:** build with `./gradlew :app:assembleDebug`, tick `PROGRESS.md`, commit `"T23: <title>"`. Stuck 5 times on one error -> `BLOCKERS.md`, next task.
