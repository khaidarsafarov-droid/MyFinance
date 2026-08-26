#!/usr/bin/env bash
# Invoked from Xcode (Run Script phase, before Compile Sources).
# Builds :shared as TruckerLoadShared.framework and embeds it in the iOS app.
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"

if [[ "$(uname -s)" != Darwin ]]; then
  echo "error: embedding the iOS KMP framework requires macOS and Xcode" >&2
  exit 1
fi

exec sh ./gradlew :shared:embedAndSignAppleFrameworkForXcode \
  -Ptruckerload.enableIos=true \
  --quiet
