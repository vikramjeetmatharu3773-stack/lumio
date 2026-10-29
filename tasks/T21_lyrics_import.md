# T21 — Lyrics import, tag editor, delete, ringtone

**Read ONLY:** docs/08 (Lyrics); docs/02 (API matrix)

**Files you will touch:**
- `data/repo/LyricsRepository.kt`
- `ui/screens/TagEditorScreen.kt`
- `ui/components/Sheets.kt`

**Steps:**
1. Import .lrc per song (copy into app files); lyrics folder setting via SAF.
2. Delete from device: `MediaStore.createDeleteRequest` on 30+, RecoverableSecurityException on 29.
3. Tag editor (title/artist/album/year/genre) via MediaStore update where allowed; otherwise explain limits.
4. Set as ringtone with WRITE_SETTINGS flow.

**Acceptance (all must be true):**
- [ ] Each action works on API 29 and 34 devices

**Always:** build with `./gradlew :app:assembleDebug`, tick `PROGRESS.md`, commit `"T21: <title>"`. Stuck 5 times on one error -> `BLOCKERS.md`, next task.
