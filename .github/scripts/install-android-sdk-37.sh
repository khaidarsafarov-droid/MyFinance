#!/usr/bin/env bash
# Install the Android 17 (API 37.0) platform and matching build-tools.
# The package id is platforms;android-37.0 — platforms;android-37 does not exist.
set -euo pipefail

sdk_root="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-}}"
if [ -z "$sdk_root" ]; then
  for candidate in \
    "${ANDROID_SDK_ROOT:-}" \
    /usr/local/lib/android/sdk \
    "${HOME}/Android/Sdk"; do
    if [ -d "$candidate" ]; then
      sdk_root="$candidate"
      break
    fi
  done
fi
if [ -z "$sdk_root" ] || [ ! -d "$sdk_root" ]; then
  echo "Android SDK root not found (ANDROID_HOME / ANDROID_SDK_ROOT)." >&2
  exit 1
fi
export ANDROID_HOME="$sdk_root"
export ANDROID_SDK_ROOT="$sdk_root"

sdkmanager=""
for candidate in \
  "$sdk_root/cmdline-tools/latest/bin/sdkmanager" \
  "$sdk_root/cmdline-tools/bin/sdkmanager"; do
  if [ -x "$candidate" ]; then
    sdkmanager="$candidate"
    break
  fi
done
if [ -z "$sdkmanager" ]; then
  # GitHub-hosted runners sometimes keep cmdline-tools under a versioned folder.
  sdkmanager="$(find "$sdk_root/cmdline-tools" -type f -name sdkmanager -perm -u+x 2>/dev/null | head -n 1 || true)"
fi
if [ -z "$sdkmanager" ]; then
  echo "sdkmanager not found under $sdk_root" >&2
  ls -la "$sdk_root" >&2 || true
  exit 1
fi

echo "Installing platforms;android-37.0 and build-tools;37.0.0 into $sdk_root"
# sdkmanager prompts for licenses; --sdk_root keeps the install in the same tree Gradle uses.
yes | "$sdkmanager" --sdk_root="$sdk_root" --install \
  "platforms;android-37.0" \
  "build-tools;37.0.0" \
  || true
# Fail if the platform still isn't present after the install attempt.
if [ ! -d "$sdk_root/platforms/android-37.0" ]; then
  echo "platforms;android-37.0 was not installed at $sdk_root/platforms/android-37.0" >&2
  ls -la "$sdk_root/platforms" >&2 || true
  exit 1
fi
echo "Android SDK 37.0 is ready."
