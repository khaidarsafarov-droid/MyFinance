#!/usr/bin/env bash
# Build a local-first signed release APK for sharing with friends.
# Does not commit secrets. Requires JDK 21 + Android SDK.
set -eu
if [ -n "${BASH_VERSION:-}" ]; then
  set -o pipefail
fi

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

if [[ ! -f local.properties ]]; then
  echo "Missing local.properties (need sdk.dir). Copy from local.properties.example." >&2
  exit 1
fi

grep -q 'sdk.dir=' local.properties || {
  echo "local.properties must set sdk.dir" >&2
  exit 1
}

if [[ ! -f keystore.properties ]]; then
  echo "Missing keystore.properties — copy keystore.properties.example and generate a keystore." >&2
  exit 1
fi

if ! grep -q '^LOCAL_ONLY_MODE=' local.properties; then
  echo 'LOCAL_ONLY_MODE=true' >> local.properties
fi

echo "Building signed release APK (LOCAL_ONLY / arm phone / no server sync)…"
sh ./gradlew :app:assembleRelease -PfriendsPhoneApk=true

APK="$ROOT/app/build/outputs/apk/release/app-release.apk"
if [[ ! -f "$APK" ]]; then
  echo "APK not found at $APK" >&2
  exit 1
fi

VERSION="$(grep -E 'versionName\s*=' app/build.gradle.kts | head -1 | sed -E 's/.*"([^"]+)".*/\1/')"
VERSION="${VERSION:-friends}"

mkdir -p "$ROOT/dist"
OUT="$ROOT/dist/TruckoRig-${VERSION}-friends.apk"
cp -f "$APK" "$OUT"
echo "OK: $OUT"
ls -lh "$OUT"
