#!/usr/bin/env bash
# Verifies required files exist and no INTERNET permission slipped in.
cd "$(dirname "$0")/.." || exit 1
miss=0
for f in README.md HERMES_START_HERE.md PROGRESS.md docs/13_BUILD_ENVIRONMENT.md tasks/INDEX.md \
 project/settings.gradle.kts project/app/build.gradle.kts project/gradle/libs.versions.toml \
 project/app/src/main/AndroidManifest.xml project/app/src/main/java/app/lumio/player/MainActivity.kt; do
  [ -f "$f" ] || { echo "MISSING $f"; miss=1; }
done
if grep -R "android.permission.INTERNET" project/app/src/main/AndroidManifest.xml >/dev/null 2>&1; then echo "FAIL: INTERNET permission present"; miss=1; fi
echo "Kotlin files: $(find project -name '*.kt' | wc -l)"
echo "Lines of Kotlin: $(find project -name '*.kt' -exec cat {} + | wc -l)"
[ $miss -eq 0 ] && echo "PACK OK" || echo "PACK HAS PROBLEMS"
