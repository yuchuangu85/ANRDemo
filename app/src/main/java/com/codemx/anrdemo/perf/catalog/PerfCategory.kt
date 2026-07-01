package com.codemx.anrdemo.perf.catalog

/** 性能测试分类，displayName 用于 UI 过滤 chip 与卡片标签。 */
enum class PerfCategory(val displayName: String) {
    Overdraw("过度绘制"),
    Jank("卡顿"),
    FrameDrop("丢帧"),
    Memory("内存抖动"),
    Rendering("渲染/解码"),
    Layout("布局层级"),
    ListScroll("列表滑动"),
}
