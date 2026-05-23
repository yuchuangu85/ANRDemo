package com.codemx.anrdemo.anr.triggers

import android.os.SystemClock
import android.util.Log
import com.codemx.anrdemo.anr.diagnostics.AnrLogTags
import com.codemx.anrdemo.anr.dispatch.AnrTriggerRequest
import com.codemx.anrdemo.anr.dispatch.TriggerResult
import com.codemx.anrdemo.anr.safety.MemoryBudget

object MemoryPressureTriggers {
    private val retainedChunks = mutableListOf<ByteArray>()
    private const val MB = 1024 * 1024

    fun trigger(request: AnrTriggerRequest): TriggerResult {
        val budget = MemoryBudget.resolve(
            requestedMaxMb = request.maxMb ?: 128,
            chunkMb = request.chunkMb ?: 8,
            allowDangerousOom = request.allowDangerousOom,
        )
        leakChunksUntilPressure(budget.maxMb, budget.chunkMb)
        return triggerGcThrashOnMainThread(request.blockMs ?: 8_000L)
    }

    fun leakChunksUntilPressure(maxMb: Int, chunkMb: Int) {
        val targetBytes = maxMb * MB
        val chunkBytes = chunkMb * MB
        Log.d(AnrLogTags.TRIGGER, "Retaining up to ${maxMb}MB in ${chunkMb}MB chunks")
        while (retainedBytes() < targetBytes) {
            retainedChunks += ByteArray(chunkBytes) { 1 }
        }
        Log.d(AnrLogTags.TRIGGER, "Retained memory ~= ${retainedBytes() / MB}MB")
    }

    fun triggerGcThrashOnMainThread(durationMs: Long): TriggerResult {
        val started = SystemClock.elapsedRealtime()
        var allocations = 0
        Log.d(AnrLogTags.TRIGGER, "Starting GC thrash on main thread for ${durationMs}ms")
        while (SystemClock.elapsedRealtime() - started < durationMs) {
            @Suppress("UNUSED_VARIABLE")
            val garbage = Array(256) { ByteArray(16 * 1024) }
            allocations += garbage.size
            if (allocations % 2048 == 0) System.gc()
        }
        return TriggerResult.Completed(SystemClock.elapsedRealtime() - started)
    }

    fun clearRetainedChunks() {
        retainedChunks.clear()
        System.gc()
    }

    fun retainedMb(): Int = (retainedBytes() / MB).toInt()

    private fun retainedBytes(): Long = retainedChunks.sumOf { it.size.toLong() }
}
