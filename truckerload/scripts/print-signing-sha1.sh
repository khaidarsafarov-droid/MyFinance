#!/usr/bin/env bash
# Print SHA-1 of project keystores and an optional APK. No secrets are printed.
set -eu
if [ -n "${BASH_VERSION:-}" ]; then
  set -o pipefail
fi

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

sha_from_keystore() {
  store="$1"
  alias="$2"
  pass="$3"
  if [ ! -f "$store" ]; then
    echo "  (file missing: $store)"
    return
  fi
  keytool -list -v -keystore "$store" -alias "$alias" -storepass "$pass" 2>/dev/null \
    | awk '/SHA1:/{print "  "$0; found=1} END{if(!found) print "  (could not read keystore — check alias/password)"}'
}

prop() {
  file="$1"
  key="$2"
  awk -F= -v k="$key" '$1==k {sub(/^[^=]+=/,""); print; exit}' "$file"
}

echo "Package: com.truckorig"
echo

if [ -f keystore.properties ]; then
  echo "keystore.properties (Play upload / release; also used for debug if no debug-keystore.properties):"
  sha_from_keystore \
    "$(prop keystore.properties storeFile)" \
    "$(prop keystore.properties keyAlias)" \
    "$(prop keystore.properties storePassword)"
else
  echo "keystore.properties: not found"
fi
echo

if [ -f debug-keystore.properties ]; then
  echo "debug-keystore.properties:"
  sha_from_keystore \
    "$(prop debug-keystore.properties storeFile)" \
    "$(prop debug-keystore.properties keyAlias)" \
    "$(prop debug-keystore.properties storePassword)"
else
  echo "debug-keystore.properties: not found (debug uses keystore.properties when present)"
fi
echo

DEFAULT_DEBUG="$HOME/.android/debug.keystore"
if [ -f "$DEFAULT_DEBUG" ]; then
  echo "Default Android debug.keystore (only if no project keystore):"
  sha_from_keystore "$DEFAULT_DEBUG" androiddebugkey android
fi
echo

if [ "${1:-}" != "" ]; then
  echo "APK $1:"
  keytool -printcert -jarfile "$1" 2>/dev/null \
    | awk '/SHA1:/{print "  "$0}'
fi
