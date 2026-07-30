#!/usr/bin/env bash
# Build a local-first signed release APK for sharing with friends.
# Does not commit secrets. Requires JDK 21 + Android SDK.
set -eu
# pipefail when bash
if [ -n "${BASH_VERSION:-}" ]; then
  set -o pipefail
fi

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

if [[ ! -f local.properties ]]; then
  echo "Missing local.properties (need sdk.dir). Copy from local.properties.example." >&2
  exit 1
fi

# Friends build: local Room only — no cloud backend URL baked in.
# Tokens/keys stay empty unless you intentionally set them in local.properties.
grep -q 'sdk.dir=' local.properties || {
  echo "local.properties must set sdk.dir" >&2
  exit 1
}

if [[ ! -f keystore.properties ]]; then
  echo "Missing keystore.properties — copy keystore.properties.example and generate a keystore." >&2
  exit 1
fi

# Ensure friends defaults without rewriting the whole file.
if ! grep -q '^LOCAL_ONLY_MODE=' local.properties; then
  echo 'LOCAL_ONLY_MODE=true' >> local.properties
fi

echo "Building signed release APK (LOCAL_ONLY / no server sync)…"
sh ./gradlew :app:assembleRelease

APK="$ROOT/app/build/outputs/apk/release/app-release.apk"
if [[ ! -f "$APK" ]]; then
  echo "APK not found at $APK" >&2
  exit 1
fi

mkdir -p "$ROOT/dist"
OUT="$ROOT/dist/TruckerLoad-1.5.3-friends.apk"
cp -f "$APK" "$OUT"
echo "OK: $OUT"
ls -lh "$OUT"
