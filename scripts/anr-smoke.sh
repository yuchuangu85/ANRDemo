#!/usr/bin/env bash
set -euo pipefail

PKG="com.codemx.anrdemo"
MAIN="$PKG/.MainActivity"
OUT_DIR="logs/anr-smoke-$(date +%Y%m%d-%H%M%S)"
SERIAL="${ANDROID_SERIAL:-}"
VERIFY=1

usage() {
  cat <<'USAGE'
Usage: scripts/anr-smoke.sh [options]

Runs short ANRDemo scenarios, injects input during main-thread stalls, and stores
per-scenario logcat/event/exit-info evidence under logs/anr-smoke-*.

Options:
  -s, --serial SERIAL   adb device serial. Defaults to ANDROID_SERIAL or adb default.
  -o, --out DIR         output directory. Defaults to logs/anr-smoke-YYYYmmdd-HHMMSS.
  --no-verify           do not fail when expected log signatures are absent.
  -h, --help            show this help.
USAGE
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    -s|--serial)
      SERIAL="${2:?missing serial}"
      shift 2
      ;;
    -o|--out)
      OUT_DIR="${2:?missing output directory}"
      shift 2
      ;;
    --no-verify)
      VERIFY=0
      shift
      ;;
    -h|--help)
      usage
      exit 0
      ;;
    *)
      echo "Unknown option: $1" >&2
      usage >&2
      exit 2
      ;;
  esac
done

ADB=(adb)
if [[ -n "$SERIAL" ]]; then
  ADB+=( -s "$SERIAL" )
fi

mkdir -p "$OUT_DIR"
SUMMARY="$OUT_DIR/summary.tsv"
printf 'scenario\texpected\tmatched\tlogcat\tevent_log\texit_info\n' > "$SUMMARY"

logcat_file_for() { printf '%s/%s-logcat.txt' "$OUT_DIR" "$1"; }
event_file_for() { printf '%s/%s-events.txt' "$OUT_DIR" "$1"; }
exit_file_for() { printf '%s/%s-exit-info.txt' "$OUT_DIR" "$1"; }

uri_with_confirm() {
  local uri="$1"
  if [[ "$uri" == *\?* ]]; then
    printf '%s&adbConfirmed=true' "$uri"
  else
    printf '%s?adbConfirmed=true' "$uri"
  fi
}

inject_input_during_block() {
  local seconds="$1"
  local end=$((SECONDS + seconds))
  while [[ "$SECONDS" -lt "$end" ]]; do
    "${ADB[@]}" shell input tap 500 500 >/dev/null 2>&1 || true
    sleep 1
  done
}

capture_case_logs() {
  local name="$1"
  "${ADB[@]}" logcat -d -v threadtime -b main -b system -b crash > "$(logcat_file_for "$name")" 2>&1 || true
  "${ADB[@]}" logcat -d -v threadtime -b events > "$(event_file_for "$name")" 2>&1 || true
  "${ADB[@]}" shell dumpsys activity exit-info "$PKG" > "$(exit_file_for "$name")" 2>&1 || true
}

verify_case() {
  local name="$1"
  local expected_regex="$2"
  local combined="$OUT_DIR/$name-combined.txt"
  cat "$(logcat_file_for "$name")" "$(event_file_for "$name")" "$(exit_file_for "$name")" > "$combined"
  if grep -Eiq "$expected_regex" "$combined"; then
    printf '%s\t%s\t%s\t%s\t%s\t%s\n' "$name" "$expected_regex" yes "$(logcat_file_for "$name")" "$(event_file_for "$name")" "$(exit_file_for "$name")" >> "$SUMMARY"
    return 0
  fi
  printf '%s\t%s\t%s\t%s\t%s\t%s\n' "$name" "$expected_regex" no "$(logcat_file_for "$name")" "$(event_file_for "$name")" "$(exit_file_for "$name")" >> "$SUMMARY"
  [[ "$VERIFY" -eq 0 ]]
}

run_deeplink() {
  local name="$1"
  local uri="$2"
  local input_seconds="$3"
  local settle_seconds="$4"
  local expected_regex="$5"

  echo "==> $name"
  "${ADB[@]}" shell am force-stop "$PKG" || true
  "${ADB[@]}" logcat -c || true
  "${ADB[@]}" shell am start -n "$MAIN" >/dev/null
  sleep 2
  "${ADB[@]}" shell am start -a android.intent.action.VIEW -d "$(uri_with_confirm "$uri")" "$PKG" >/dev/null || true
  if [[ "$input_seconds" -gt 0 ]]; then
    inject_input_during_block "$input_seconds" &
    local injector_pid=$!
    sleep "$settle_seconds"
    wait "$injector_pid" 2>/dev/null || true
  else
    sleep "$settle_seconds"
  fi
  capture_case_logs "$name"
  "${ADB[@]}" shell am force-stop "$PKG" || true
  verify_case "$name" "$expected_regex"
}

echo "ANRDemo smoke evidence output: $OUT_DIR"
"${ADB[@]}" wait-for-device

run_deeplink "input-dispatch" "anrdemo://scenario/input-dispatch?blockMs=8000" 8 10 "Input dispatching timed out|ANR in $PKG|am_anr"
run_deeplink "deadlock-contention" "anrdemo://scenario/deadlock?mode=contention&blockMs=8000" 8 10 "Input dispatching timed out|ANRDemo-lock-holder|ANR in $PKG|am_anr"
run_deeplink "memory-pressure-bounded" "anrdemo://scenario/memory-pressure?maxMb=64&chunkMb=4&blockMs=8000" 8 10 "Input dispatching timed out|Starting GC thrash|ANR in $PKG|am_anr"
run_deeplink "broadcast-foreground" "anrdemo://scenario/broadcast-foreground?foreground=true&blockMs=12000" 0 15 "Broadcast of Intent|Broadcast receiver|ANR in $PKG|am_anr"

cat "$SUMMARY"
echo "Smoke finished. Long-running/dangerous scenarios remain excluded. Use scripts/capture-android-logs.sh for full bugreports."
