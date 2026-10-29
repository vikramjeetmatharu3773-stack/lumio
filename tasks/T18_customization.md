# T18 — Customization: presets, theme editor, backup/restore

**Read ONLY:** docs/06 ('Customization ideas'); docs/10 (Backup JSON)

**Files you will touch:**
- `ui/screens/ThemeEditorScreen.kt`
- `data/settings/Presets.kt`
- `data/repo/BackupRepository.kt`

**Steps:**
1. 8 built-in presets, custom presets, export/import look JSON.
2. Backup/restore settings+playlists+favorites+play counts via SAF.
3. Color picker (swatches + HSV sliders).

**Acceptance (all must be true):**
- [ ] Applying a preset changes UI instantly
- [ ] Backup -> wipe data -> restore returns playlists/favorites

**Always:** build with `./gradlew :app:assembleDebug`, tick `PROGRESS.md`, commit `"T18: <title>"`. Stuck 5 times on one error -> `BLOCKERS.md`, next task.
