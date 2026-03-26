#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT/android-app"

if [[ ! -f keystore.properties ]]; then
  echo "[ERROR] android-app/keystore.properties not found."
  echo "Copy keystore.properties.example to keystore.properties and add release.keystore."
  exit 1
fi

echo ""
echo "=== Signed release APK ==="
echo "Gradle: parallel + build cache (see android-app/gradle.properties)"
echo ""

./gradlew assembleRelease --build-cache

APK="$ROOT/android-app/app/build/outputs/apk/release/app-release.apk"
if [[ ! -f "$APK" ]]; then
  echo "[ERROR] APK not found: $APK"
  exit 1
fi

echo ""
echo "OK — signed release APK:"
echo "  $APK"
echo ""
