# T14 — Search

**Read ONLY:** docs/04 (4.6)

**Files you will touch:**
- `ui/screens/SearchScreen.kt`
- `data/repo/LibraryRepository.kt (search)`

**Steps:**
1. Normalize text (lowercase, strip diacritics), AND-match all words.
2. Chips filter, grouped results, recent searches (max 10).
3. Debounce 120 ms; results computed off main thread.

**Acceptance (all must be true):**
- [ ] 10k songs: results < 100 ms after typing

**Always:** build with `./gradlew :app:assembleDebug`, tick `PROGRESS.md`, commit `"T14: <title>"`. Stuck 5 times on one error -> `BLOCKERS.md`, next task.
