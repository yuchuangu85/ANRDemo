package com.codemx.anrdemo.anr.catalog

enum class AnrTriggerKind {
    MainThreadBlock,
    Deadlock,
    MemoryPressure,
    Broadcast,
    Service,
    ForegroundServiceStart,
    JobService,
    ContentProvider,
    ShortForegroundService,
    AdbOnly
}
