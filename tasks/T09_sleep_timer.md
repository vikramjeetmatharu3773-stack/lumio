# T09 — Sleep timer, fade, speed/pitch, skip silence

**Read ONLY:** docs/08_PLAYBACK_ENGINE.md (Fade, Sleep timer)

**Files you will touch:**
- `playback/SleepTimerController.kt`
- `playback/PlaybackService.kt`
- `ui/components/Sheets.kt`

**Steps:**
1. Sleep timer sheet with presets + custom + end-of-track; countdown chip in Now Playing.
2. 5 s volume fade before timer pause (J2).
3. Speed/pitch sheet with reset; apply defaults from settings at service start.
4. Verify fade never leaves volume stuck < 1 after setting fade to 0.

**Acceptance (all must be true):**
- [ ] Timer pauses at 0; end-of-track pauses at boundary
- [ ] Fade slider audibly works

**Always:** build with `./gradlew :app:assembleDebug`, tick `PROGRESS.md`, commit `"T09: <title>"`. Stuck 5 times on one error -> `BLOCKERS.md`, next task.
