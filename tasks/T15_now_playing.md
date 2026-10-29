# T15 — Now Playing

**Read ONLY:** docs/04 (4.7, 4.8); docs/05 rows 7-13,21

**Files you will touch:**
- `ui/screens/NowPlayingScreen.kt`
- `ui/screens/QueueSheet.kt`
- `ui/components/LiquidSeekBar.kt`
- `ui/components/LyricsView.kt`

**Steps:**
1. Expand/collapse sheet: drag-follow, velocity fling, back handler, mini fade.
2. Artwork pager sync with queue; breathing scale; vinyl spin if shape CIRCLE.
3. Seek bar haptics + label bubble. Favorite particle burst.
4. Lyrics flip view; font size from settings.
5. Queue sheet: drag reorder (Reorderable lib), swipe remove, save as playlist.
6. Landscape layout (F12).

**Acceptance (all must be true):**
- [ ] All rows of doc 05 numbered 7-13 and 21 visibly work

**Always:** build with `./gradlew :app:assembleDebug`, tick `PROGRESS.md`, commit `"T15: <title>"`. Stuck 5 times on one error -> `BLOCKERS.md`, next task.
