package com.codemx.anrdemo.perf.catalog

/**
 * 性能问题的触发机制。
 *
 * [nature] 区分两种性质：
 * - [PerfNature.Load]：对主线程/内存加压的运行时负载，由 PerfLoadController 以 start/stop 管理，带自动停止护栏。
 * - [PerfNature.Visual]：在页面「演示区」渲染的重 UI，只需按 activeScenario 渲染，不启动后台负载。
 */
enum class PerfTriggerKind(val nature: PerfNature) {
    OverdrawLayers(PerfNature.Visual),
    PeriodicMainThreadBlock(PerfNature.Load),
    OneShotLongTask(PerfNature.Load),
    RecompositionStorm(PerfNature.Load),
    PerFrameHeavyWork(PerfNature.Load),
    AllocationChurn(PerfNature.Load),
    LargeBitmapDecode(PerfNature.Load),
    DeepLayoutNesting(PerfNature.Visual),
    HeavyScroll(PerfNature.Visual),
}

enum class PerfNature { Load, Visual }
