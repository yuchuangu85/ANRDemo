#!/usr/bin/env bash
set -euo pipefail

usage() {
  cat <<'USAGE'
Usage: scripts/capture-android-logs.sh [options]

Capture normal logcat, event logcat, and bugreport in one synchronized session.

Options:
  -s, --serial SERIAL     adb device serial. Defaults to ANDROID_SERIAL or adb default.
  -o, --out DIR          output directory. Defaults to logs/android-capture-YYYYmmdd-HHMMSS.
  --clear                clear logcat buffers before capture.
  --tail-seconds N       keep logcat running N seconds after bugreport finishes. Default: 3.
  -h, --help             show this help.

Outputs:
  System_MT_logcat_MM_DD_HH_MM_SS.txt         main/system/crash/radio/kernel buffers
  System_MT_logcat_event_MM_DD_HH_MM_SS.txt   events buffer
  bugreport-* or *.zip                       adb bugreport archive using system-generated name
  metadata.txt                                device and timing metadata
USAGE
}

SERIAL="${ANDROID_SERIAL:-}"
OUT_DIR=""
CLEAR_LOGS=0
TAIL_SECONDS=3

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
    --clear)
      CLEAR_LOGS=1
      shift
      ;;
    --tail-seconds)
      TAIL_SECONDS="${2:?missing seconds}"
      shift 2
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

if ! command -v adb >/dev/null 2>&1; then
  echo "adb not found in PATH" >&2
  exit 127
fi

if ! [[ "$TAIL_SECONDS" =~ ^[0-9]+$ ]]; then
  echo "--tail-seconds must be a non-negative integer" >&2
  exit 2
fi

ADB=(adb)
if [[ -n "$SERIAL" ]]; then
  ADB+=( -s "$SERIAL" )
fi

STAMP="$(date +%Y%m%d-%H%M%S)"
if [[ -z "$OUT_DIR" ]]; then
  OUT_DIR="logs/android-capture-$STAMP"
fi
mkdir -p "$OUT_DIR"

LOGCAT_STAMP="$(date +%m_%d_%H_%M_%S)"
LOGCAT_FILE="$OUT_DIR/System_MT_logcat_${LOGCAT_STAMP}.txt"
EVENT_STAMP="$(date +%m_%d_%H_%M_%S)"
EVENT_FILE="$OUT_DIR/System_MT_logcat_event_${EVENT_STAMP}.txt"
BUGREPORT_TARGET="$OUT_DIR"
METADATA_FILE="$OUT_DIR/metadata.txt"

LOGCAT_PID=""
EVENT_PID=""
BUGREPORT_PID=""

cleanup() {
  local code=$?
  for pid in "$LOGCAT_PID" "$EVENT_PID"; do
    if [[ -n "$pid" ]] && kill -0 "$pid" 2>/dev/null; then
      kill "$pid" 2>/dev/null || true
      wait "$pid" 2>/dev/null || true
    fi
  done
  if [[ -n "$BUGREPORT_PID" ]] && kill -0 "$BUGREPORT_PID" 2>/dev/null; then
    echo "Waiting for bugreport process $BUGREPORT_PID to finish..." >&2
    wait "$BUGREPORT_PID" || true
  fi
  echo "Capture directory: $OUT_DIR" >&2
  exit "$code"
}
trap cleanup EXIT INT TERM

{
  echo "capture_start=$(date -Is)"
  echo "out_dir=$OUT_DIR"
  echo "adb=${ADB[*]}"
  echo "tail_seconds=$TAIL_SECONDS"
  echo
  "${ADB[@]}" devices -l || true
  echo
  "${ADB[@]}" shell getprop ro.product.manufacturer 2>/dev/null | sed 's/^/manufacturer=/' || true
  "${ADB[@]}" shell getprop ro.product.model 2>/dev/null | sed 's/^/model=/' || true
  "${ADB[@]}" shell getprop ro.build.version.release 2>/dev/null | sed 's/^/android_release=/' || true
  "${ADB[@]}" shell getprop ro.build.version.sdk 2>/dev/null | sed 's/^/android_sdk=/' || true
} > "$METADATA_FILE"

"${ADB[@]}" wait-for-device

if [[ "$CLEAR_LOGS" -eq 1 ]]; then
  echo "Clearing logcat buffers..."
  "${ADB[@]}" logcat -c || true
fi

echo "Starting logcat -> $LOGCAT_FILE"
"${ADB[@]}" logcat -v threadtime -b main -b system -b crash -b radio -b kernel > "$LOGCAT_FILE" 2>&1 &
LOGCAT_PID=$!

echo "Starting event logcat -> $EVENT_FILE"
"${ADB[@]}" logcat -v threadtime -b events > "$EVENT_FILE" 2>&1 &
EVENT_PID=$!

sleep 1

echo "Starting bugreport -> $BUGREPORT_TARGET (system-generated filename)"
"${ADB[@]}" bugreport "$BUGREPORT_TARGET" > "$OUT_DIR/bugreport.stdout.txt" 2> "$OUT_DIR/bugreport.stderr.txt" &
BUGREPORT_PID=$!

BUGREPORT_STATUS=0
wait "$BUGREPORT_PID" || BUGREPORT_STATUS=$?
BUGREPORT_PID=""

echo "bugreport_exit=$BUGREPORT_STATUS" >> "$METADATA_FILE"
echo "bugreport_end=$(date -Is)" >> "$METADATA_FILE"

if [[ "$TAIL_SECONDS" -gt 0 ]]; then
  echo "Keeping logcat running for ${TAIL_SECONDS}s after bugreport..."
  sleep "$TAIL_SECONDS"
fi

echo "Stopping logcat captures..."
kill "$LOGCAT_PID" "$EVENT_PID" 2>/dev/null || true
wait "$LOGCAT_PID" 2>/dev/null || true
wait "$EVENT_PID" 2>/dev/null || true
LOGCAT_PID=""
EVENT_PID=""

echo "capture_end=$(date -Is)" >> "$METADATA_FILE"

echo "Done. Files:"
printf '  %s\n' "$LOGCAT_FILE" "$EVENT_FILE" "$METADATA_FILE"
find "$OUT_DIR" -maxdepth 1 -type f \( -name 'bugreport*' -o -name '*.zip' \) -print | sed 's/^/  /'

if [[ "$BUGREPORT_STATUS" -ne 0 ]]; then
  echo "Warning: bugreport exited with status $BUGREPORT_STATUS. Check bugreport stdout/stderr files." >&2
  exit "$BUGREPORT_STATUS"
fi
