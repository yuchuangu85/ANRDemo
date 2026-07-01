package com.codemx.anrdemo.perf.metrics

/** 帧性能指标快照，供指标面板渲染。镜像 anr 侧 DiagnosticsSnapshot 的角色。 */
data class PerfMetricsSnapshot(
    val framesTracked: Int = 0,
    val jankyFrames: Int = 0,
    val jankPercent: Float = 0f,
    val lastFrameMs: Float = 0f,
    val maxFrameMs: Float = 0f,
    val avgFrameMs: Float = 0f,
    val approxFps: Float = 0f,
)
