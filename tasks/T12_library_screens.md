# T12 — Library screens

**Read ONLY:** docs/04 (4.4); docs/06 Library layout rows

**Files you will touch:**
- `ui/screens/LibraryScreen.kt`
- `ui/components/FastScroller.kt`
- `ui/components/SongRow.kt`

**Steps:**
1. 7 tabs, sort sheet (7 fields + asc/desc), list/grid toggle, grid columns setting.
2. Alphabet fast scroller with bubble.
3. Multi-select mode with bulk actions.
4. Ignore-articles sorting; remember last tab.
5. Pull-to-refresh runs scan.

**Acceptance (all must be true):**
- [ ] Smooth 10k-song list
- [ ] All bulk actions work

**Always:** build with `./gradlew :app:assembleDebug`, tick `PROGRESS.md`, commit `"T12: <title>"`. Stuck 5 times on one error -> `BLOCKERS.md`, next task.
