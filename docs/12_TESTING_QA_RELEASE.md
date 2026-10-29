# 12 — Testing, QA & Release

## Automated (T24)
| Level | What | Tool |
|---|---|---|
| Unit | LRC parser, folder-key conversion, sort/group logic, mood mapping, backup JSON, EQ preset interpolation, sleep timer | JUnit4 + kotlinx-coroutines-test |
| DB | DAOs, user_data survives song replace, playlist cascade | Room in-memory + Robolectric (optional) |
| UI | Onboarding flow, Library tabs, Now Playing controls semantics | compose-ui-test |
| Lint | `./gradlew lint` zero errors | Android Lint |

## Manual QA matrix
Devices/emulators: API 26 (small phone), 31, 34, 35; one tablet; one with SD card if possible.
| Area | Checks |
|---|---|
| Permission | grant / deny / deny-forever / revoke while running |
| Scan | 10 songs, 5 k songs, SD card, `.nomedia`, include/exclude, switch modes, rebuild keeps favorites |
| Playback | gapless album, seek, speed 0.5/2.0, shuffle/repeat, headphone unplug, call interruption, Bluetooth buttons, 2 h background play |
| Queue | reorder, remove, play next, persists after force-stop |
| Now Playing | open/close gesture, rotate, dark/light, artwork missing |
| Glass | each slider extremes, reduce transparency, AMOLED, 200 % font, 1.5× display size |
| Widgets | 3 sizes, config, after reboot, app killed |
| Edge | airplane mode (should be identical), low storage, 50 k songs, songs w/ emoji/RTL titles, corrupt mp3 |
| A11y | TalkBack labels, focus order, contrast, touch targets |

## Performance budgets
Cold start ≤ 1.2 s (mid-range) · scroll jank < 3 % on 10 k songs · memory ≤ 250 MB · APK ≤ 20 MB (release, arm64 split) · battery: playback with screen off ≤ 3 %/h.
Tools: Android Studio Profiler, `adb shell dumpsys gfxinfo`, Macrobenchmark (optional).

## Release checklist (T25)
1. `versionCode`/`versionName` bump.
2. Create keystore: `keytool -genkeypair -v -keystore lumio-release.jks -alias lumio -keyalg RSA -keysize 2048 -validity 10000` — **store outside the repo**; never commit.
3. `keystore.properties` (git-ignored) → `signingConfigs.release` in `app/build.gradle.kts` (task T25 adds it).
4. `./gradlew :app:bundleRelease :app:assembleRelease` → verify with `apksigner verify`.
5. R8 on: test every screen on the release build (media3/room/glance keep rules are consumer-provided; app rules in `proguard-rules.pro`).
6. Install release APK on a clean device; run smoke test (scan, play, widget, notification).
7. Privacy policy page: "Lumio collects no data. No network access." Data safety form: no data collected/shared.
8. Content rating: Everyone. Category: Music & Audio.
