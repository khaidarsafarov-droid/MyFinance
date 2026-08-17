#!/usr/bin/env bash
# Print SHA-1 of project keystores and an optional APK. No secrets are printed.
set -eu
if [ -n "${BASH_VERSION:-}" ]; then
  set -o pipefail
fi

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

sha_from_keystore() {
  local store="$1"
  local alias="$2"
  local pass="$3"
  if [[ ! -f "$store" ]]; then
    echo "  (file missing: $store)"
    return
  fi
  keytool -list -v -keystore "$store" -alias "$alias" -storepass "$pass" 2>/dev/null \
    | awk '/^\s*SHA1:/{print "  "$0; found=1} END{if(!found) print "  (could not read keystore — check alias/password)"}'
}

prop() {
  local file="$1"
  local key="$2"
  awk -F= -v k="$key" '$1==k {sub(/^[^=]+=/,""); print; exit}' "$file"
}

echo "Package: com.truckerload"
echo "Canonical friends SHA-1: 66:46:40:1E:70:B7:3A:9C:28:D6:7E:4B:68:19:76:AD:46:C6:27:2C"
echo

if [[ -f keystore.properties ]]; then
  echo "keystore.properties (release/friends):"
  sha_from_keystore \
    "$(prop keystore.properties storeFile)" \
    "$(prop keystore.properties keyAlias)" \
    "$(prop keystore.properties storePassword)"
else
  echo "keystore.properties: not found"
fi
echo

if [[ -f debug-keystore.properties ]]; then
  echo "debug-keystore.properties (Android Studio debug):"
  sha_from_keystore \
    "$(prop debug-keystore.properties storeFile)" \
    "$(prop debug-keystore.properties keyAlias)" \
    "$(prop debug-keystore.properties storePassword)"
else
  echo "debug-keystore.properties: not found (debug uses ~/.android/debug.keystore)"
fi
echo

DEFAULT_DEBUG="$HOME/.android/debug.keystore"
if [[ -f "$DEFAULT_DEBUG" ]]; then
  echo "Default Android debug.keystore:"
  sha_from_keystore "$DEFAULT_DEBUG" androiddebugkey android
fi
echo

if [[ "${1:-}" != "" ]]; then
  echo "APK $1:"
  keytool -printcert -jarfile "$1" 2>/dev/null \
    | awk '/SHA1:/{print "  "$0}'
fi
