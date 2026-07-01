package com.codemx.anrdemo.perf.catalog

import com.codemx.anrdemo.anr.catalog.ScenarioParameterSpec

/** 从场景参数 spec 默认值 + UI 覆盖值构建 [PerfRequest]，并按 spec 范围钳制。镜像 ScenarioRequestBuilder。 */
object PerfRequestBuilder {
    fun defaultsFor(scenario: PerfScenario): Map<String, String> = buildMap {
        scenario.defaultRequest.blockMs?.let { put("blockMs", it.toString()) }
        scenario.defaultRequest.intervalMs?.let { put("intervalMs", it.toString()) }
        scenario.defaultRequest.layerCount?.let { put("layerCount", it.toString()) }
        scenario.defaultRequest.nestingDepth?.let { put("nestingDepth", it.toString()) }
        scenario.defaultRequest.itemCount?.let { put("itemCount", it.toString()) }
        scenario.defaultRequest.bitmapPx?.let { put("bitmapPx", it.toString()) }
        scenario.parameterSpecs.forEach { spec ->
            when (spec) {
                is ScenarioParameterSpec.DurationMs -> putIfAbsent(spec.key, spec.defaultValue.toString())
                is ScenarioParameterSpec.Choice -> putIfAbsent(spec.key, spec.defaultValue)
                is ScenarioParameterSpec.IntRange -> putIfAbsent(spec.key, spec.defaultValue.toString())
                is ScenarioParameterSpec.BooleanToggle -> putIfAbsent(spec.key, spec.defaultValue.toString())
            }
        }
    }

    fun build(scenario: PerfScenario, values: Map<String, String>): PerfRequest {
        val merged = defaultsFor(scenario) + values
        return PerfRequest(
            scenarioId = scenario.id,
            blockMs = merged["blockMs"]?.toLongOrNull()?.let { clampLong(scenario, "blockMs", it) },
            intervalMs = merged["intervalMs"]?.toLongOrNull()?.let { clampLong(scenario, "intervalMs", it) },
            layerCount = merged["layerCount"]?.toIntOrNull()?.let { clampInt(scenario, "layerCount", it) },
            nestingDepth = merged["nestingDepth"]?.toIntOrNull()?.let { clampInt(scenario, "nestingDepth", it) },
            itemCount = merged["itemCount"]?.toIntOrNull()?.let { clampInt(scenario, "itemCount", it) },
            bitmapPx = merged["bitmapPx"]?.toIntOrNull()?.let { clampInt(scenario, "bitmapPx", it) },
        )
    }

    private fun clampLong(scenario: PerfScenario, key: String, value: Long): Long {
        val spec = scenario.parameterSpecs.filterIsInstance<ScenarioParameterSpec.DurationMs>().firstOrNull { it.key == key }
            ?: return value
        return value.coerceIn(spec.min, spec.max)
    }

    private fun clampInt(scenario: PerfScenario, key: String, value: Int): Int {
        val spec = scenario.parameterSpecs.filterIsInstance<ScenarioParameterSpec.IntRange>().firstOrNull { it.key == key }
            ?: return value
        return value.coerceIn(spec.min, spec.max)
    }
}
