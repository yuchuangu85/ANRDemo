package com.codemx.anrdemo.perf.catalog

import com.codemx.anrdemo.anr.catalog.ScenarioParameterSpec

/**
 * 单个性能测试场景。数据驱动，镜像 [com.codemx.anrdemo.anr.catalog.AnrScenario]。
 *
 * @param howToObserve 引导用户如何观察现象（如开发者选项开关、看指标面板）。
 * @param expectedSymptom 预期现象。
 * @param autoStopMs Load 型场景的自动停止护栏（毫秒）；Visual 型为 null。
 * @param parameterSpecs 复用 ANR 侧的 [ScenarioParameterSpec] 驱动参数 UI。
 */
data class PerfScenario(
    val id: String,
    val title: String,
    val category: PerfCategory,
    val triggerKind: PerfTriggerKind,
    val description: String,
    val howToObserve: String,
    val expectedSymptom: String,
    val defaultRequest: PerfRequest,
    val parameterSpecs: List<ScenarioParameterSpec> = emptyList(),
    val autoStopMs: Long? = null,
) {
    val isLoad: Boolean get() = triggerKind.nature == PerfNature.Load
    val isVisual: Boolean get() = triggerKind.nature == PerfNature.Visual
}
