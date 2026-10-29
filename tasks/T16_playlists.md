# T16 — Playlists

**Read ONLY:** docs/01 section G; docs/10

**Files you will touch:**
- `data/repo/PlaylistRepository.kt`
- `ui/screens/PlaylistsUi.kt`
- `ui/components/Sheets.kt (AddToPlaylist)`

**Steps:**
1. Create/rename/delete; add from any list; reorder; remove.
2. M3U/M3U8 export/import via SAF (match by path then title+artist).
3. Smart playlist rules engine (genre/year/plays/added/favorite) — P2.

**Acceptance (all must be true):**
- [ ] Playlist survives rescan + app restart

**Always:** build with `./gradlew :app:assembleDebug`, tick `PROGRESS.md`, commit `"T16: <title>"`. Stuck 5 times on one error -> `BLOCKERS.md`, next task.
