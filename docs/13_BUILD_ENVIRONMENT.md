# 13 — Build Environment (read in T00)

There are three ways to get an APK. Pick the first that works.

## Option A — GitHub Actions (most reliable, recommended for Termux users)
1. Put the `project/` folder contents in a GitHub repo (root of repo = contents of `project/`).
2. Push. `.github/workflows/android.yml` builds with JDK 17 + Gradle 8.10.2 and uploads `lumio-debug-apk` as an artifact.
3. Download the APK from the Actions run → install.
Build errors appear in the Actions log — copy the first `e:` line back to Hermes.

## Option B — PC with Android Studio (Ladybug+ / JDK 17)
Open `project/`, let Gradle sync, press Run. No other setup.

## Option C — Inside Termux (best effort — heavy, ARM)
Needs ≥ 6 GB RAM free for Gradle + Kotlin; ~6 GB storage. Steps (`scripts/setup_termux.sh` automates 1–3):
1. `pkg install openjdk-17 git gradle aapt2 wget unzip`
2. Install an Android SDK (platform 35, build-tools 34/35, platform-tools) into `$HOME/android-sdk` using a Termux-compatible SDK installer, then set `ANDROID_HOME`.
3. In `project/local.properties`: `sdk.dir=/data/data/com.termux/files/home/android-sdk`
4. Add to `project/gradle.properties` (Termux only): `android.aapt2FromMavenOverride=/data/data/com.termux/files/usr/bin/aapt2` — the AAPT2 from Maven is x86 and will not run on ARM.
5. Lower memory: `org.gradle.jvmargs=-Xmx1536m`, `kotlin.daemon.jvmargs=-Xmx1024m`, `org.gradle.workers.max=2`.
6. First build downloads ~1 GB of dependencies: needs network once; afterwards `--offline` works.
If this fails after 2 honest attempts → go to Option A. Do not burn hours.

## Gradle wrapper
The wrapper JAR is binary and not shipped. Run once: `gradle wrapper --gradle-version 8.10.2` inside `project/` (then `./gradlew` works). `gradle/wrapper/gradle-wrapper.properties` is already present.

## Installing the APK from Termux
`termux-open app/build/outputs/apk/debug/app-debug.apk` (needs "install unknown apps" for Termux) or copy to shared storage and open from Files.

## Version pinning
All versions live in `gradle/libs.versions.toml`. If a dependency cannot be resolved offline, report in `BLOCKERS.md`; do not swap libraries.
