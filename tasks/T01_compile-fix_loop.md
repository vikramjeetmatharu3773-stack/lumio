# T01 — Compile-fix loop: scaffold -> BUILD SUCCESSFUL

**Read ONLY:** HERMES_START_HERE.md (section 2); docs/02_ARCHITECTURE.md

**Files you will touch:**
- `Any file under project/app/src/main/java/app/lumio/player/`

**Steps:**
1. Run the build loop. Fix the FIRST error only; rebuild.
2. Typical issues: missing import, wrong Compose parameter name, Media3 API signature, Room type converter, nullability.
3. When an API is uncertain use the simplest known-good form; do not change library versions.
4. After build passes run `./gradlew :app:lint` and fix only errors (not warnings).
5. Install the debug APK on a device/emulator and confirm the app opens to onboarding.

**Acceptance (all must be true):**
- [ ] `BUILD SUCCESSFUL` for assembleDebug
- [ ] App launches without crash on API 26+ device
- [ ] BLOCKERS.md lists anything skipped

**Always:** build with `./gradlew :app:assembleDebug`, tick `PROGRESS.md`, commit `"T01: <title>"`. Stuck 5 times on one error -> `BLOCKERS.md`, next task.
