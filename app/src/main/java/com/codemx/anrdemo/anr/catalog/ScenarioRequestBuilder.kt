package com.codemx.anrdemo.anr.catalog

import com.codemx.anrdemo.anr.dispatch.AnrTriggerRequest
import com.codemx.anrdemo.anr.safety.ConfirmationToken

object ScenarioRequestBuilder {
    fun defaultsFor(scenario: AnrScenario): Map<String, String> = buildMap {
        scenario.defaultRequest.blockMs?.let { put("blockMs", it.toString()) }
        scenario.defaultRequest.mode?.let { put("mode", it) }
        scenario.defaultRequest.maxMb?.let { put("maxMb", it.toString()) }
        scenario.defaultRequest.chunkMb?.let { put("chunkMb", it.toString()) }
        scenario.defaultRequest.foreground?.let { put("foreground", it.toString()) }
        put("allowDangerousOom", scenario.defaultRequest.allowDangerousOom.toString())
        scenario.parameterSpecs.forEach { spec ->
            when (spec) {
                is ScenarioParameterSpec.DurationMs -> putIfAbsent(spec.key, spec.defaultValue.toString())
                is ScenarioParameterSpec.Choice -> putIfAbsent(spec.key, spec.defaultValue)
                is ScenarioParameterSpec.IntRange -> putIfAbsent(spec.key, spec.defaultValue.toString())
                is ScenarioParameterSpec.BooleanToggle -> putIfAbsent(spec.key, spec.defaultValue.toString())
            }
        }
    }

    fun build(
        scenario: AnrScenario,
        values: Map<String, String>,
        confirmationToken: ConfirmationToken? = null,
    ): AnrTriggerRequest {
        val merged = defaultsFor(scenario) + values
        return AnrTriggerRequest(
            scenarioId = scenario.id,
            blockMs = merged["blockMs"]?.toLongOrNull()?.let { clampDuration(scenario, "blockMs", it) },
            mode = merged["mode"],
            maxMb = merged["maxMb"]?.toIntOrNull()?.let { clampInt(scenario, "maxMb", it) },
            chunkMb = merged["chunkMb"]?.toIntOrNull()?.let { clampInt(scenario, "chunkMb", it) },
            foreground = merged["foreground"]?.toBooleanStrictOrNull(),
            allowDangerousOom = merged["allowDangerousOom"]?.toBooleanStrictOrNull() ?: false,
            confirmationToken = confirmationToken,
        )
    }

    private fun clampDuration(scenario: AnrScenario, key: String, value: Long): Long {
        val spec = scenario.parameterSpecs.filterIsInstance<ScenarioParameterSpec.DurationMs>().firstOrNull { it.key == key }
            ?: return value
        return value.coerceIn(spec.min, spec.max)
    }

    private fun clampInt(scenario: AnrScenario, key: String, value: Int): Int {
        val spec = scenario.parameterSpecs.filterIsInstance<ScenarioParameterSpec.IntRange>().firstOrNull { it.key == key }
            ?: return value
        return value.coerceIn(spec.min, spec.max)
    }
}
