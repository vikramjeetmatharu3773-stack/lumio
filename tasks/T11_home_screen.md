# T11 — Home screen

**Read ONLY:** docs/04_SCREENS_AND_FLOWS.md (4.3); docs/05 rows 4,5

**Files you will touch:**
- `ui/screens/HomeScreen.kt`
- `data/repo/LibraryRepository.kt (mixes)`

**Steps:**
1. Match reference layout: header + icon capsule, mood chips, hero pager, mixes row, sections.
2. Mood keywords map (Feel good, Work out, Energise, Relax) + fallback when no genre tags.
3. Section stagger animation (first load only).
4. Empty library state with Scan CTA.
5. Add History/Stats/EQ/Settings buttons wired to routes.

**Acceptance (all must be true):**
- [ ] Looks like the screenshot structure; all taps work

**Always:** build with `./gradlew :app:assembleDebug`, tick `PROGRESS.md`, commit `"T11: <title>"`. Stuck 5 times on one error -> `BLOCKERS.md`, next task.
