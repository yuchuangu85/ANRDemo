package com.codemx.anrdemo.anr.catalog

import com.codemx.anrdemo.anr.dispatch.AnrTriggerRequest

data class AnrScenario(
    val id: String,
    val title: String,
    val category: AnrCategory,
    val riskLevel: AnrRiskLevel,
    val triggerKind: AnrTriggerKind,
    val timeoutDescription: String,
    val recommendedBlockMs: Long?,
    val minApi: Int? = null,
    val maxApi: Int? = null,
    val expectedReason: String,
    val explanation: String,
    val defaultRequest: AnrTriggerRequest,
    val adbCommand: String?,
    val recoveryCommand: String = "adb shell am force-stop com.codemx.anrdemo",
    val enabledByDefault: Boolean = true,
    val confirmationRequirement: ConfirmationRequirement = ConfirmationRequirement.CountdownConfirm,
    val parameterSpecs: List<ScenarioParameterSpec> = emptyList(),
    val confirmationPhrase: String? = null,
    val documentationOnlyReason: String? = null,
)
