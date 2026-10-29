# T04 — Settings repository verification + tests

**Read ONLY:** docs/06_SETTINGS_AND_CUSTOMIZATION.md

**Files you will touch:**
- `data/settings/AppSettings.kt`
- `data/settings/SettingsRepository.kt`
- `tests`

**Steps:**
1. Diff every field in doc 06 against `AppSettings` + `SettingsRepository.read/write`; add anything missing.
2. Ensure unknown/invalid enum strings fall back to defaults.
3. Add `debounceUpdate` helper so sliders can write at most every 250 ms.
4. Unit-test round-trip of every field.

**Acceptance (all must be true):**
- [ ] Doc-06 table and code are 1:1
- [ ] Round-trip test passes

**Always:** build with `./gradlew :app:assembleDebug`, tick `PROGRESS.md`, commit `"T04: <title>"`. Stuck 5 times on one error -> `BLOCKERS.md`, next task.
