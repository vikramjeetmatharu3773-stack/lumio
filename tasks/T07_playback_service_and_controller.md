# T07 — Playback service + controller

**Read ONLY:** docs/08_PLAYBACK_ENGINE.md (Components, Queue, Persistence, History, Failure)

**Files you will touch:**
- `playback/PlaybackService.kt`
- `playback/PlayerController.kt`
- `util/ArtworkBitmapLoader.kt`

**Steps:**
1. Verify foreground notification shows with artwork on lockscreen.
2. Test queue ops: playSongs, playNext, addToQueue, move, remove, skipTo, clear.
3. Queue persistence + resume without autoplay.
4. History/play-count rules; skip counting.
5. Error handling: skip bad file, stop after 3 consecutive errors.
6. Headset/Bluetooth buttons work; audio focus verified with a phone call.

**Acceptance (all must be true):**
- [ ] 2 h screen-off playback test OK
- [ ] Force-stop app -> reopen -> queue restored at position

**Always:** build with `./gradlew :app:assembleDebug`, tick `PROGRESS.md`, commit `"T07: <title>"`. Stuck 5 times on one error -> `BLOCKERS.md`, next task.
