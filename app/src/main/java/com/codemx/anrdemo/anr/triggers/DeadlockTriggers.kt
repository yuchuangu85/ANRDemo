package com.codemx.anrdemo.anr.triggers

import android.os.SystemClock
import android.util.Log
import com.codemx.anrdemo.anr.diagnostics.AnrLogTags
import com.codemx.anrdemo.anr.dispatch.AnrTriggerRequest
import com.codemx.anrdemo.anr.dispatch.TriggerResult
import java.util.concurrent.CountDownLatch

object DeadlockTriggers {
    private val lockA = Any()
    private val lockB = Any()

    fun trigger(request: AnrTriggerRequest): TriggerResult = when (request.mode) {
        "classic" -> triggerClassicDeadlock()
        else -> triggerLockContention(request.blockMs ?: 8_000L)
    }

    fun triggerLockContention(durationMs: Long): TriggerResult {
        val lockHeld = CountDownLatch(1)
        val worker = Thread({
            synchronized(lockA) {
                Log.d(AnrLogTags.TRIGGER, "Worker holds lockA for ${durationMs}ms")
                lockHeld.countDown()
                SystemClock.sleep(durationMs)
            }
        }, "ANRDemo-lock-holder")
        worker.start()
        lockHeld.await()
        val started = SystemClock.elapsedRealtime()
        Log.d(AnrLogTags.TRIGGER, "Main thread waiting for lockA; expected lock contention ANR root cause")
        synchronized(lockA) {
            return TriggerResult.Completed(SystemClock.elapsedRealtime() - started)
        }
    }

    fun triggerClassicDeadlock(): TriggerResult {
        val workerReady = CountDownLatch(1)
        Thread({
            synchronized(lockA) {
                workerReady.countDown()
                SystemClock.sleep(200)
                synchronized(lockB) {
                    Log.d(AnrLogTags.TRIGGER, "Worker unexpectedly acquired lockB")
                }
            }
        }, "ANRDemo-classic-deadlock-worker").start()
        workerReady.await()
        synchronized(lockB) {
            SystemClock.sleep(200)
            Log.d(AnrLogTags.TRIGGER, "Main thread entering classic deadlock waiting for lockA")
            synchronized(lockA) {
                return TriggerResult.Completed(0)
            }
        }
    }
}
