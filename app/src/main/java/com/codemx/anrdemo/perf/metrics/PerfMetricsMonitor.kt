package com.codemx.anrdemo.perf.metrics

import android.view.Window
import androidx.metrics.performance.JankStats

/**
 * 用 JankStats 采集帧指标并聚合成 [PerfMetricsSnapshot]。
 *
 * 在性能页的 DisposableEffect 中创建，[dispose] 时释放。通过 [onSnapshot] 把节流后的快照推给 Compose state。
 * onFrame 在主线程回调，快照更新按 [updateIntervalNanos] 节流，避免每帧重组反噬测量结果。
 */
class PerfMetricsMonitor(
    window: Window,
    private val onSnapshot: (PerfMetricsSnapshot) -> Unit,
) {
    private val accumulator = PerfMetricsAccumulator()
    private var lastEmitNanos = 0L
    private val updateIntervalNanos = 250_000_000L // 250ms

    private val jankStats: JankStats = JankStats.createAndTrack(window) { frameData ->
        accumulator.onFrame(
            frameStartNanos = frameData.frameStartNanos,
            frameDurationUiNanos = frameData.frameDurationUiNanos,
            isJank = frameData.isJank,
        )
        val now = frameData.frameStartNanos
        if (now - lastEmitNanos >= updateIntervalNanos) {
            lastEmitNanos = now
            onSnapshot(accumulator.snapshot())
        }
    }

    fun setTracking(enabled: Boolean) {
        jankStats.isTrackingEnabled = enabled
    }

    fun reset() {
        accumulator.reset()
        onSnapshot(accumulator.snapshot())
    }

    fun dispose() {
        jankStats.isTrackingEnabled = false
    }
}
