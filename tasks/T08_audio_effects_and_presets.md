# T08 — Audio effects + presets

**Read ONLY:** docs/08_PLAYBACK_ENGINE.md (Audio effects)

**Files you will touch:**
- `playback/AudioEffectsController.kt`
- `ui/screens/EqualizerScreen.kt`

**Steps:**
1. Verify attach on session id change; effects survive track changes.
2. Preset interpolation unit test (5-point curve -> N bands).
3. Add custom preset save/delete (stored as JSON string in settings).
4. Unsupported-device card. Never crash if an effect throws.

**Acceptance (all must be true):**
- [ ] EQ audibly changes sound; settings persist after restart

**Always:** build with `./gradlew :app:assembleDebug`, tick `PROGRESS.md`, commit `"T08: <title>"`. Stuck 5 times on one error -> `BLOCKERS.md`, next task.
