#!/usr/bin/env bash
# Usage: scripts/build_debug.sh   (run from pack root)
set -o pipefail
cd "$(dirname "$0")/../project" || exit 1
if [ -x ./gradlew ]; then G=./gradlew; else G=gradle; fi
$G :app:assembleDebug --stacktrace 2>&1 | tee build.log | grep -E "^e: |error:|FAILED|BUILD" | head -30
echo "Full log: project/build.log"
ls -la app/build/outputs/apk/debug/*.apk 2>/dev/null
