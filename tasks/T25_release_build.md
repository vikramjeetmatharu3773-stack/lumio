# T25 — Release build, signing, R8, Play assets

**Read ONLY:** docs/12 (Release checklist); docs/11 (Play listing kit)

**Files you will touch:**
- `app/build.gradle.kts`
- `proguard-rules.pro`
- `keystore.properties (local, git-ignored)`

**Steps:**
1. Add signingConfigs.release reading keystore.properties.
2. Enable R8; test release build on device.
3. Generate screenshots + feature graphic from real app.
4. Write privacy policy markdown.

**Acceptance (all must be true):**
- [ ] Signed release APK/AAB installs and runs

**Always:** build with `./gradlew :app:assembleDebug`, tick `PROGRESS.md`, commit `"T25: <title>"`. Stuck 5 times on one error -> `BLOCKERS.md`, next task.
