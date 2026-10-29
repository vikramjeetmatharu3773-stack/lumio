# T13 — Collection detail + song options

**Read ONLY:** docs/04 (4.5)

**Files you will touch:**
- `ui/screens/CollectionScreen.kt`
- `ui/components/Sheets.kt (SongOptionsSheet)`
- `ui/nav/*`

**Steps:**
1. All collection types: album, artist (with albums row), genre, folder, playlist, favorites, mostplayed, recent, recentlyadded, forgotten, mood, all.
2. Header with blurred art, Play/Shuffle.
3. Song options: play next, queue, add to playlist, favorite, go to album/artist, info, share, hide.
4. Predictive back.

**Acceptance (all must be true):**
- [ ] Every entry point from Home/Library opens correct list

**Always:** build with `./gradlew :app:assembleDebug`, tick `PROGRESS.md`, commit `"T13: <title>"`. Stuck 5 times on one error -> `BLOCKERS.md`, next task.
