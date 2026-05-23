package com.codemx.anrdemo.anr.triggers

import android.os.SystemClock
import android.util.Log
import com.codemx.anrdemo.anr.diagnostics.AnrLogTags
import com.codemx.anrdemo.anr.dispatch.TriggerResult

object MainThreadTriggers {
    fun blockMainThread(durationMs: Long): TriggerResult {
        val started = SystemClock.elapsedRealtime()
        Log.d(AnrLogTags.TRIGGER, "Blocking main thread for ${durationMs}ms on ${Thread.currentThread().name}")
        Thread.sleep(durationMs)
        return TriggerResult.Completed(SystemClock.elapsedRealtime() - started)
    }
}
