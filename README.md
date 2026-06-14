# ANRDemo

ANRDemo is a self-contained Android Compose app for demonstrating common Android ANR types, root causes, and diagnostic commands.

## Build

```bash
./gradlew :app:testDebugUnitTest :app:assembleDebug --no-daemon
```

## Safety

This app intentionally blocks the main thread, services, broadcasts, jobs, content providers, or memory allocator. Run only on a test device.

Recovery command:

```bash
adb shell am force-stop com.codemx.anrdemo
```

## Diagnostics

Recommended logcat:

```bash
adb logcat -v time ActivityManager:E AndroidRuntime:E ANRDemo:D ANRDemo.Trigger:D '*:S'
```

Android 11+ exit info:

```bash
adb shell dumpsys activity exit-info com.codemx.anrdemo
```

## Scenario examples

```bash
adb shell am start -a android.intent.action.VIEW \
  -d 'anrdemo://scenario/input-dispatch?blockMs=8000&adbConfirmed=true' \
  com.codemx.anrdemo

adb shell am start -a android.intent.action.VIEW \
  -d 'anrdemo://scenario/deadlock?mode=contention&blockMs=8000&adbConfirmed=true' \
  com.codemx.anrdemo

adb shell am start -a android.intent.action.VIEW \
  -d 'anrdemo://scenario/memory-pressure?maxMb=64&chunkMb=4&blockMs=8000&adbConfirmed=true' \
  com.codemx.anrdemo

adb shell am start -a android.intent.action.VIEW \
  -d 'anrdemo://scenario/broadcast-foreground?foreground=true&blockMs=12000&adbConfirmed=true' \
  com.codemx.anrdemo

adb shell am start -a android.intent.action.VIEW \
  -d 'anrdemo://scenario/binder-peer-stall?blockMs=8000&adbConfirmed=true' \
  com.codemx.anrdemo
```

`adbConfirmed=true` is intentionally required for adb-driven deep links. The app no longer exposes browser deep links, and the blocking broadcast receiver is internal-only, to avoid accidental third-party ANR triggers.


## Capture logcat, event log, and bugreport

Capture normal logcat, Android events buffer, exit-info, and bugreport into separate files in the same session:

```bash
scripts/capture-android-logs.sh --clear
```

With an explicit device and output directory:

```bash
scripts/capture-android-logs.sh -s <device-serial> -o logs/anr-case-001 --tail-seconds 5
```

Outputs include `System_MT_logcat_MM_DD_HH_MM_SS.txt`, `System_MT_logcat_event_MM_DD_HH_MM_SS.txt`, `exit-info.txt`, the system-generated bugreport file, and `metadata.txt`.

## Smoke test

```bash
scripts/anr-smoke.sh
```

The smoke script injects input during main-thread stalls, stores per-scenario logcat/event/exit-info files, and fails if expected system/app signatures are missing. It intentionally excludes long-running or dangerous scenarios such as service 200s, shortService 3min, classic deadlock, and dangerous OOM.

## Docs

- `docs/anr-demo-app-plan.md`
- `docs/anr-code-architecture.md`
- `docs/anr-phase2-architecture.md`
