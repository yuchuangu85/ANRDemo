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
  -d 'anrdemo://scenario/input-dispatch?blockMs=8000' \
  com.codemx.anrdemo

adb shell am start -a android.intent.action.VIEW \
  -d 'anrdemo://scenario/deadlock?mode=contention&blockMs=8000' \
  com.codemx.anrdemo

adb shell am start -a android.intent.action.VIEW \
  -d 'anrdemo://scenario/memory-pressure?maxMb=64&chunkMb=4&blockMs=8000' \
  com.codemx.anrdemo

adb shell am broadcast --receiver-foreground \
  -a com.codemx.anrdemo.ACTION_BLOCKING_BROADCAST \
  -n com.codemx.anrdemo/.anr.triggers.DemoBroadcastReceiver \
  --ez foreground true --el blockMs 12000
```


## Capture logcat, event log, and bugreport

Capture normal logcat, Android events buffer, and bugreport into separate files in the same session:

```bash
scripts/capture-android-logs.sh --clear
```

With an explicit device and output directory:

```bash
scripts/capture-android-logs.sh -s <device-serial> -o logs/anr-case-001 --tail-seconds 5
```

Outputs include `System_MT_logcat_MM_DD_HH_MM_SS.txt`, `System_MT_logcat_event_MM_DD_HH_MM_SS.txt`, the system-generated bugreport file, and `metadata.txt`.

## Smoke test

```bash
scripts/anr-smoke.sh
```

The smoke script intentionally excludes long-running or dangerous scenarios such as background service 200s, shortService 3min, classic deadlock, and dangerous OOM.

## Docs

- `docs/anr-demo-app-plan.md`
- `docs/anr-code-architecture.md`
- `docs/anr-phase2-architecture.md`
