#!/usr/bin/env bash
# TruckerLoad emulator smoke script (ADB).
# Usage: docs/smoke_adb.sh [serial]
set -euo pipefail

SERIAL="${1:-}"
ADB=(adb)
if [[ -n "$SERIAL" ]]; then
  ADB=(adb -s "$SERIAL")
fi

PKG=com.truckerload
PASS=0
FAIL=0

step() { echo ""; echo "==> $*"; }
ok() { echo "OK: $*"; PASS=$((PASS + 1)); }
bad() { echo "FAIL: $*"; FAIL=$((FAIL + 1)); }

"${ADB[@]}" wait-for-device
step "Device online"
"${ADB[@]}" shell getprop sys.boot_completed | grep -q 1 && ok "boot_completed" || bad "not booted"

step "Disable animations (slow emulator)"
"${ADB[@]}" shell settings put global window_animation_scale 0 || true
"${ADB[@]}" shell settings put global transition_animation_scale 0 || true
"${ADB[@]}" shell settings put global animator_duration_scale 0 || true
ok "animations disabled"

step "Launch app"
"${ADB[@]}" shell am force-stop "$PKG" || true
"${ADB[@]}" shell monkey -p "$PKG" -c android.intent.category.LAUNCHER 1 >/dev/null
sleep 4
pid=$("${ADB[@]}" shell pidof "$PKG" | tr -d '\r' || true)
if [[ -n "${pid:-}" ]]; then ok "process running pid=$pid"; else bad "app not running"; fi

step "Screenshot home"
mkdir -p /tmp/truckerload-smoke
"${ADB[@]}" exec-out screencap -p > /tmp/truckerload-smoke/home.png && ok "screencap home.png" || bad "screencap"

step "Cold restart"
"${ADB[@]}" shell am force-stop "$PKG"
"${ADB[@]}" shell monkey -p "$PKG" -c android.intent.category.LAUNCHER 1 >/dev/null
sleep 5
pid2=$("${ADB[@]}" shell pidof "$PKG" | tr -d '\r' || true)
if [[ -n "${pid2:-}" ]]; then ok "restart ok"; else bad "restart failed"; fi

step "Check for fatal ANR/crash snippets in logcat (last 200)"
if "${ADB[@]}" logcat -d -t 200 | grep -E "FATAL EXCEPTION|ANR in $PKG" >/tmp/truckerload-smoke/logcat-fail.txt; then
  bad "found crash/ANR (see /tmp/truckerload-smoke/logcat-fail.txt)"
else
  ok "no FATAL/ANR in recent logcat"
  rm -f /tmp/truckerload-smoke/logcat-fail.txt
fi

echo ""
echo "Smoke summary: PASS=$PASS FAIL=$FAIL"
[[ "$FAIL" -eq 0 ]]
