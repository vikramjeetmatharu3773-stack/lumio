# T06 — Folder scanning: include/exclude, deep scan, .nomedia, auto-rescan

**Read ONLY:** docs/07_LIBRARY_AND_FOLDER_SCANNING.md (all)

**Files you will touch:**
- `data/scan/MediaScanner.kt`
- `data/scan/FolderUtils.kt`
- `ui/screens/FoldersScreen.kt`

**Steps:**
1. Test folder picker -> persistable permission -> row in `scan_folders`.
2. Verify precedence: exclude wins; SELECTED_ONLY empty list warning.
3. Test deep scan finds songs under `.nomedia`.
4. Check SD-card volumes (case-insensitive volume match).
5. ContentObserver auto-rescan (3 s debounce) + WorkManager one-off for launch scan when app was closed (optional).
6. Folder rows: song count, last scan, 'Needs access' state.

**Acceptance (all must be true):**
- [ ] All 4 acceptance items at bottom of doc 07 pass

**Always:** build with `./gradlew :app:assembleDebug`, tick `PROGRESS.md`, commit `"T06: <title>"`. Stuck 5 times on one error -> `BLOCKERS.md`, next task.
