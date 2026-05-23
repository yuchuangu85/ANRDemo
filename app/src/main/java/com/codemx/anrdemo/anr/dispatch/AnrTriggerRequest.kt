package com.codemx.anrdemo.anr.dispatch

data class AnrTriggerRequest(
    val scenarioId: String,
    val blockMs: Long? = null,
    val mode: String? = null,
    val maxMb: Int? = null,
    val chunkMb: Int? = null,
    val foreground: Boolean? = null,
    val allowDangerousOom: Boolean = false,
) {
    fun withScenario(id: String): AnrTriggerRequest = copy(scenarioId = id)
}
