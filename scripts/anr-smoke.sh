#!/usr/bin/env bash
set -euo pipefail

PKG="com.codemx.anrdemo"
MAIN="$PKG/.MainActivity"
ACTION="com.codemx.anrdemo.ACTION_BLOCKING_BROADCAST"
RECEIVER="$PKG/.anr.triggers.DemoBroadcastReceiver"

run_deeplink() {
  local name="$1"
  local uri="$2"
  echo "==> $name"
  adb shell am force-stop "$PKG" || true
  adb shell am start -n "$MAIN" >/dev/null
  sleep 2
  adb shell am start -a android.intent.action.VIEW -d "$uri" "$PKG" || true
  sleep 10
  adb shell am force-stop "$PKG" || true
}

echo "ANRDemo smoke. Run logcat separately:"
echo "adb logcat -v time ActivityManager:E AndroidRuntime:E ANRDemo:D ANRDemo.Trigger:D '*:S'"

run_deeplink "input-dispatch" "anrdemo://scenario/input-dispatch?blockMs=8000"
run_deeplink "deadlock-contention" "anrdemo://scenario/deadlock?mode=contention&blockMs=8000"
run_deeplink "memory-pressure-bounded" "anrdemo://scenario/memory-pressure?maxMb=64&chunkMb=4&blockMs=8000"

echo "==> broadcast-foreground"
adb shell am force-stop "$PKG" || true
adb shell am broadcast --receiver-foreground -a "$ACTION" -n "$RECEIVER" --ez foreground true --el blockMs 12000 || true
sleep 15
adb shell am force-stop "$PKG" || true

echo "Smoke finished. Long-running/dangerous scenarios are intentionally excluded."
