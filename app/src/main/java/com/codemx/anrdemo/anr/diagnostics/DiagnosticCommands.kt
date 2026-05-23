package com.codemx.anrdemo.anr.diagnostics

object DiagnosticCommands {
    const val LOGCAT = "adb logcat -v time ActivityManager:E AndroidRuntime:E ANRDemo:D ANRDemo.Trigger:D *:S"
    const val EXIT_INFO = "adb shell dumpsys activity exit-info com.codemx.anrdemo"
    const val FORCE_STOP = "adb shell am force-stop com.codemx.anrdemo"
}
