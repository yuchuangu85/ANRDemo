package com.codemx.anrdemo.perf.metrics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PerfMetricsAccumulatorTest {
    private val frameNanos60 = 16_666_667L // 60fps 帧间隔

    @Test
    fun countsFramesAndJank() {
        val acc = PerfMetricsAccumulator()
        var t = 0L
        repeat(10) {
            val jank = it % 5 == 0
            acc.onFrame(frameStartNanos = t, frameDurationUiNanos = if (jank) 40_000_000 else 8_000_000, isJank = jank)
            t += frameNanos60
        }
        val snap = acc.snapshot()
        assertEquals(10, snap.framesTracked)
        assertEquals(2, snap.jankyFrames)
        assertEquals(20f, snap.jankPercent, 0.01f)
    }

    @Test
    fun tracksMaxFrameDuration() {
        val acc = PerfMetricsAccumulator()
        acc.onFrame(0, 8_000_000, false)
        acc.onFrame(frameNanos60, 50_000_000, true)
        acc.onFrame(frameNanos60 * 2, 8_000_000, false)
        assertEquals(50f, acc.snapshot().maxFrameMs, 0.01f)
    }

    @Test
    fun approxFpsDerivedFromFrameIntervals() {
        val acc = PerfMetricsAccumulator()
        var t = 0L
        repeat(60) {
            acc.onFrame(frameStartNanos = t, frameDurationUiNanos = 5_000_000, isJank = false)
            t += frameNanos60
        }
        val fps = acc.snapshot().approxFps
        // 60fps 间隔应收敛到 ~60，而非用 5ms UI 耗时推出的 200
        assertTrue("fps=$fps 应接近 60", fps in 45f..65f)
    }

    @Test
    fun resetClearsState() {
        val acc = PerfMetricsAccumulator()
        acc.onFrame(0, 40_000_000, true)
        acc.reset()
        val snap = acc.snapshot()
        assertEquals(0, snap.framesTracked)
        assertEquals(0, snap.jankyFrames)
        assertEquals(0f, snap.maxFrameMs, 0.01f)
    }
}
