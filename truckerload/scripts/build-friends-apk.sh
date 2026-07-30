#!/usr/bin/env bash
# Build a signed release APK for sharing with friends (phones only).
# Enables Google + email/password login on-device (no custom backend required).
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

# Friends APK must show login (Google / email+password). Do not ship LOCAL_ONLY auto-login.
if grep -q '^LOCAL_ONLY_MODE=' local.properties; then
  sed -i 's/^LOCAL_ONLY_MODE=.*/LOCAL_ONLY_MODE=false/' local.properties
else
  echo 'LOCAL_ONLY_MODE=false' >> local.properties
fi

# Keep cloud API off unless explicitly configured.
if ! grep -q '^SYNC_BACKEND_URL=' local.properties; then
  echo 'SYNC_BACKEND_URL=' >> local.properties
fi
if ! grep -q '^CLOUD_MEDIA_ENABLED=' local.properties; then
  echo 'CLOUD_MEDIA_ENABLED=false' >> local.properties
fi

# Non-secret Web client ID is required for Credential Manager Google Sign-In.
DEFAULT_WEB_CLIENT_ID='842861516910-gkhu4dh9tu5rc8re40rpe4583hvs4uhv.apps.googleusercontent.com'
if ! grep -q '^GOOGLE_WEB_CLIENT_ID=.\+' local.properties; then
  if grep -q '^GOOGLE_WEB_CLIENT_ID=' local.properties; then
    sed -i "s|^GOOGLE_WEB_CLIENT_ID=.*|GOOGLE_WEB_CLIENT_ID=${DEFAULT_WEB_CLIENT_ID}|" local.properties
  else
    echo "GOOGLE_WEB_CLIENT_ID=${DEFAULT_WEB_CLIENT_ID}" >> local.properties
  fi
fi

echo "Building signed phone APK (arm64 + armeabi-v7a, login enabled)…"
sh ./gradlew :app:assembleRelease -PfriendsPhoneApk=true

APK="$ROOT/app/build/outputs/apk/release/app-release.apk"
if [[ ! -f "$APK" ]]; then
  echo "APK not found at $APK" >&2
  exit 1
fi

mkdir -p "$ROOT/dist"
OUT="$ROOT/dist/TruckerLoad-1.5.6-friends.apk"
cp -f "$APK" "$OUT"
echo "OK: $OUT"
ls -lh "$OUT"
