# BLOCKERS

## T00 — First build (sdk.dir not available)
- Error: `SDK location not found. Define a valid SDK location with an ANDROID_HOME environment variable or by setting the sdk.dir path in your project's local properties file.`
- Cause: No Android SDK installed on this system (Termux/Ubuntu host).
- Resolution per docs/13: Use Option A — GitHub Actions. Push `project/` contents to a GitHub repo; `.github/workflows/android.yml` builds with JDK 17 + Gradle 8.10.2 and uploads `lumio-debug-apk` artifact.
