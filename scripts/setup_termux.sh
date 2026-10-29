#!/data/data/com.termux/files/usr/bin/bash
# Best-effort Termux setup for building Lumio. NOT verified on every device. If it fails twice, use GitHub Actions (docs/13 Option A).
set -e
pkg update -y
pkg install -y openjdk-17 git gradle aapt2 wget unzip zip
echo "JDK:"; java -version
echo
echo "NEXT (manual): install Android SDK platform 35 + build-tools to \$HOME/android-sdk and set:"
echo "  export ANDROID_HOME=\$HOME/android-sdk"
echo "  echo sdk.dir=\$ANDROID_HOME > project/local.properties"
echo "  echo android.aapt2FromMavenOverride=\$PREFIX/bin/aapt2 >> project/gradle.properties"
echo "  echo org.gradle.jvmargs=-Xmx1536m >> project/gradle.properties"
