package com.codemx.anrdemo.perf.metrics

/**
 * 纯逻辑的帧指标累加器（无 Android 依赖，便于单测）。
 *
 * - 卡顿判定与帧 UI 耗时来自 JankStats 的 FrameData。
 * - FPS 由相邻帧的 [frameStartNanos] 间隔推算（真实刷新间隔），而非 UI 耗时，避免平滑时高估。
 * - avgFrameMs / approxFps 用指数滑动平均（EMA）平滑抖动。
 */
class PerfMetricsAccumulator {
    private var frames = 0
    private var janky = 0
    private var maxMs = 0f
    private var lastMs = 0f
    private var avgMsEma = 0f
    private var fpsEma = 0f
    private var lastStartNanos = 0L

    fun reset() {
        frames = 0
        janky = 0
        maxMs = 0f
        lastMs = 0f
        avgMsEma = 0f
        fpsEma = 0f
        lastStartNanos = 0L
    }

    fun onFrame(frameStartNanos: Long, frameDurationUiNanos: Long, isJank: Boolean) {
        frames++
        if (isJank) janky++
        val durationMs = frameDurationUiNanos / 1_000_000f
        lastMs = durationMs
        if (durationMs > maxMs) maxMs = durationMs
        avgMsEma = if (avgMsEma == 0f) durationMs else avgMsEma * 0.9f + durationMs * 0.1f

        if (lastStartNanos != 0L) {
            val intervalNanos = frameStartNanos - lastStartNanos
            if (intervalNanos > 0) {
                val instFps = (1_000_000_000f / intervalNanos).coerceIn(0f, 240f)
                fpsEma = if (fpsEma == 0f) instFps else fpsEma * 0.9f + instFps * 0.1f
            }
        }
        lastStartNanos = frameStartNanos
    }

    fun snapshot(): PerfMetricsSnapshot = PerfMetricsSnapshot(
        framesTracked = frames,
        jankyFrames = janky,
        jankPercent = if (frames > 0) janky * 100f / frames else 0f,
        lastFrameMs = lastMs,
        maxFrameMs = maxMs,
        avgFrameMs = avgMsEma,
        approxFps = fpsEma,
    )
}
