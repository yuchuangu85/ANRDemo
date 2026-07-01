package com.codemx.anrdemo.perf.catalog

/**
 * 一次性能场景运行的参数。字段均为可空，未使用的场景保持 null。
 * 由 [PerfRequestBuilder] 从参数 spec 默认值 + UI 覆盖值构建并做范围钳制。
 */
data class PerfRequest(
    val scenarioId: String,
    val blockMs: Long? = null,     // 单帧/单次阻塞主线程时长
    val intervalMs: Long? = null,  // 周期性负载的间隔
    val layerCount: Int? = null,   // 过度绘制叠加层数
    val nestingDepth: Int? = null, // 布局嵌套深度
    val itemCount: Int? = null,    // 列表项数量
    val bitmapPx: Int? = null,     // 大图解码边长（像素）
)
