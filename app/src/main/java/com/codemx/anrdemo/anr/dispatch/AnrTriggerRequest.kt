package com.codemx.anrdemo.anr.dispatch

import com.codemx.anrdemo.anr.safety.ConfirmationToken

data class AnrTriggerRequest(
    val scenarioId: String,
    val blockMs: Long? = null,
    val mode: String? = null,
    val maxMb: Int? = null,
    val chunkMb: Int? = null,
    val foreground: Boolean? = null,
    val allowDangerousOom: Boolean = false,
    val confirmationToken: ConfirmationToken? = null,
) {
    fun withScenario(id: String): AnrTriggerRequest = copy(scenarioId = id)
}
