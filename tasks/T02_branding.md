# T02 — Branding: name, package, icon, splash, strings, fonts

**Read ONLY:** docs/11_APP_ICON_AND_BRAND.md

**Files you will touch:**
- `app/src/main/res/values/strings.xml`
- `res/drawable/ic_launcher_*.xml`
- `res/mipmap-anydpi-v26/*`
- `res/values/themes.xml`
- `(optional) res/font/*`

**Steps:**
1. Verify adaptive icon renders (Android Studio Image Asset preview or device launcher) in round, squircle and themed (Android 13) modes.
2. If the user asks to rename the app: change `app_name`, `rootProject.name`; to change package use IDE refactor (namespace + applicationId + folders).
3. Add Play Store PNG from `assets/icon/lumio_play_store_512.png` to `app/src/main/` as `ic_launcher-playstore.png`.
4. Optional: add Outfit font TTFs (OFL) to `res/font` and wire into `Type.kt`.
5. Splash screen shows icon on midnight background, then fades to UI.

**Acceptance (all must be true):**
- [ ] Icon looks correct in all masks
- [ ] Splash works Android 8–15
- [ ] No hard-coded strings in new code

**Always:** build with `./gradlew :app:assembleDebug`, tick `PROGRESS.md`, commit `"T02: <title>"`. Stuck 5 times on one error -> `BLOCKERS.md`, next task.
