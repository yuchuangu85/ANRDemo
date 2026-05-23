package com.codemx.anrdemo.anr.catalog

sealed class ScenarioParameterSpec(open val key: String, open val label: String) {
    data class DurationMs(
        override val key: String = "blockMs",
        override val label: String,
        val defaultValue: Long,
        val min: Long,
        val max: Long,
    ) : ScenarioParameterSpec(key, label)

    data class Choice(
        override val key: String,
        override val label: String,
        val defaultValue: String,
        val options: List<Option>,
    ) : ScenarioParameterSpec(key, label)

    data class IntRange(
        override val key: String,
        override val label: String,
        val defaultValue: Int,
        val min: Int,
        val max: Int,
        val step: Int,
    ) : ScenarioParameterSpec(key, label)

    data class BooleanToggle(
        override val key: String,
        override val label: String,
        val defaultValue: Boolean,
    ) : ScenarioParameterSpec(key, label)
}

data class Option(val value: String, val label: String)
