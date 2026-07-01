package com.codemx.anrdemo.perf.catalog

import com.codemx.anrdemo.anr.catalog.ScenarioParameterSpec

/** 性能测试场景清单，镜像 [com.codemx.anrdemo.anr.catalog.AnrCatalog]。 */
object PerfCatalog {
    val scenarios: List<PerfScenario> = listOf(
        PerfScenario(
            id = "overdraw-layers",
            title = "过度绘制 / 多层不透明背景叠加",
            category = PerfCategory.Overdraw,
            triggerKind = PerfTriggerKind.OverdrawLayers,
            description = "在同一区域叠加多层不透明背景，同一像素被反复绘制，浪费 GPU 填充率。",
            howToObserve = "开启「开发者选项 → 调试 GPU 过度绘制」，叠加区域会显示红色（4x+ 过度绘制）。",
            expectedSymptom = "演示区出现层层叠加的色块；GPU 过度绘制调试下呈红色。",
            defaultRequest = PerfRequest("overdraw-layers", layerCount = 8),
            parameterSpecs = listOf(ScenarioParameterSpec.IntRange("layerCount", "叠加层数", 8, 2, 20, 1)),
        ),
        PerfScenario(
            id = "jank-periodic-block",
            title = "卡顿 / 主线程周期性阻塞",
            category = PerfCategory.Jank,
            triggerKind = PerfTriggerKind.PeriodicMainThreadBlock,
            description = "每隔一段时间在主线程忙等若干毫秒，动画会周期性卡住。",
            howToObserve = "看演示区匀速动画是否周期性顿挫；观察指标面板卡顿率上升、maxFrameMs 增大。",
            expectedSymptom = "动画周期性卡顿，卡顿帧占比升高。",
            defaultRequest = PerfRequest("jank-periodic-block", blockMs = 120, intervalMs = 600),
            parameterSpecs = listOf(
                ScenarioParameterSpec.DurationMs(key = "blockMs", label = "每次阻塞", defaultValue = 120, min = 30, max = 700),
                ScenarioParameterSpec.DurationMs(key = "intervalMs", label = "阻塞间隔", defaultValue = 600, min = 200, max = 3_000),
            ),
            autoStopMs = 20_000,
        ),
        PerfScenario(
            id = "jank-oneshot-task",
            title = "卡顿 / 单次长任务阻塞",
            category = PerfCategory.Jank,
            triggerKind = PerfTriggerKind.OneShotLongTask,
            description = "在主线程执行一次较长任务（低于 ANR 阈值），造成明显掉帧。",
            howToObserve = "点击后动画会明显停顿一下；maxFrameMs 出现一次尖峰。",
            expectedSymptom = "一次明显长卡顿后恢复。",
            defaultRequest = PerfRequest("jank-oneshot-task", blockMs = 700),
            parameterSpecs = listOf(ScenarioParameterSpec.DurationMs(key = "blockMs", label = "阻塞时长", defaultValue = 700, min = 100, max = 2_000)),
            autoStopMs = 3_000,
        ),
        PerfScenario(
            id = "recomposition-storm",
            title = "卡顿 / 重组风暴",
            category = PerfCategory.Jank,
            triggerKind = PerfTriggerKind.RecompositionStorm,
            description = "以极高频率变更 state 触发大量重组，主线程被重组与布局占满。",
            howToObserve = "动画卡顿、CPU 升高；指标面板卡顿率上升。",
            expectedSymptom = "持续卡顿，帧率下降。",
            defaultRequest = PerfRequest("recomposition-storm", intervalMs = 0),
            autoStopMs = 15_000,
        ),
        PerfScenario(
            id = "frame-drop-perframe",
            title = "丢帧 / 每帧繁重工作",
            category = PerfCategory.FrameDrop,
            triggerKind = PerfTriggerKind.PerFrameHeavyWork,
            description = "在每一帧回调里 busy-spin 约 12ms，逼近/超过单帧预算，持续丢帧。",
            howToObserve = "动画整体变慢、不流畅；指标面板 FPS 明显低于屏幕刷新率、卡顿率高。",
            expectedSymptom = "持续丢帧，FPS 下降。",
            defaultRequest = PerfRequest("frame-drop-perframe", blockMs = 12),
            parameterSpecs = listOf(ScenarioParameterSpec.DurationMs(key = "blockMs", label = "每帧工作", defaultValue = 12, min = 4, max = 40)),
            autoStopMs = 20_000,
        ),
        PerfScenario(
            id = "allocation-churn",
            title = "丢帧 / 内存分配抖动触发 GC",
            category = PerfCategory.Memory,
            triggerKind = PerfTriggerKind.AllocationChurn,
            description = "在主线程周期性分配并丢弃大数组，制造 GC 压力，GC 停顿导致丢帧。",
            howToObserve = "动画间歇性卡顿（GC 停顿）；Logcat 可见 GC 日志；指标面板出现帧时间尖峰。",
            expectedSymptom = "间歇性丢帧，帧时间出现尖峰。",
            defaultRequest = PerfRequest("allocation-churn", intervalMs = 50),
            parameterSpecs = listOf(ScenarioParameterSpec.DurationMs(key = "intervalMs", label = "分配间隔", defaultValue = 50, min = 10, max = 500)),
            autoStopMs = 20_000,
        ),
        PerfScenario(
            id = "large-bitmap-decode",
            title = "渲染 / 大图主线程解码",
            category = PerfCategory.Rendering,
            triggerKind = PerfTriggerKind.LargeBitmapDecode,
            description = "在主线程重复解码一张大位图（有界尺寸），阻塞渲染。",
            howToObserve = "动画停顿；maxFrameMs 尖峰；演示图片刷新时卡顿。",
            expectedSymptom = "解码期间明显卡顿。",
            defaultRequest = PerfRequest("large-bitmap-decode", bitmapPx = 1500),
            parameterSpecs = listOf(ScenarioParameterSpec.IntRange("bitmapPx", "位图边长(px)", 1500, 500, 3000, 100)),
            autoStopMs = 15_000,
        ),
        PerfScenario(
            id = "deep-layout-nesting",
            title = "布局层级 / 深层嵌套",
            category = PerfCategory.Layout,
            triggerKind = PerfTriggerKind.DeepLayoutNesting,
            description = "渲染深度嵌套的布局，放大 measure/layout 开销。",
            howToObserve = "在演示区渲染深层嵌套；配合每帧负载可放大 measure 成本。可用 Layout Inspector 观察层级。",
            expectedSymptom = "布局层级过深，measure/layout 耗时增加。",
            defaultRequest = PerfRequest("deep-layout-nesting", nestingDepth = 30),
            parameterSpecs = listOf(ScenarioParameterSpec.IntRange("nestingDepth", "嵌套深度", 30, 5, 80, 5)),
        ),
        PerfScenario(
            id = "heavy-scroll",
            title = "列表滑动 / 重 item 组合",
            category = PerfCategory.ListScroll,
            triggerKind = PerfTriggerKind.HeavyScroll,
            description = "渲染一个每个 item 组合成本较高的长列表，滑动时容易掉帧。",
            howToObserve = "在演示区快速滑动列表，观察指标面板卡顿率/丢帧。",
            expectedSymptom = "滑动时掉帧、卡顿率升高。",
            defaultRequest = PerfRequest("heavy-scroll", itemCount = 300),
            parameterSpecs = listOf(ScenarioParameterSpec.IntRange("itemCount", "列表项数", 300, 50, 1000, 50)),
        ),
    )

    private val byId = scenarios.associateBy { it.id }

    fun scenario(id: String): PerfScenario? = byId[id]

    fun requireScenario(id: String): PerfScenario = scenario(id) ?: error("Unknown perf scenario: $id")
}
