# T05 — Permissions + MediaStore scanner

**Read ONLY:** docs/07_LIBRARY_AND_FOLDER_SCANNING.md (Pipeline 1-4, 6-8); docs/01 section A,B

**Files you will touch:**
- `MainActivity.kt`
- `ui/screens/OnboardingScreen.kt`
- `data/scan/MediaScanner.kt`

**Steps:**
1. Verify permission flow on API 26/32/33+ (READ_EXTERNAL vs READ_MEDIA_AUDIO) and notification permission on 33+.
2. Handle denied + 'don't ask again' with an Open Settings button.
3. Scan result must populate Library; verify folderPath format `primary:Music/Rock/`.
4. Metadata cleanup rules from doc 07.
5. Expose ScanState to UI; show progress in onboarding and Library pull-to-refresh.

**Acceptance (all must be true):**
- [ ] Fresh install -> grant -> songs appear
- [ ] Revoking permission shows onboarding again
- [ ] Unit test for `FolderUtils.keyFromRelative/keyFromData`

**Always:** build with `./gradlew :app:assembleDebug`, tick `PROGRESS.md`, commit `"T05: <title>"`. Stuck 5 times on one error -> `BLOCKERS.md`, next task.
