package com.codemx.anrdemo.anr.safety

import com.codemx.anrdemo.anr.catalog.AnrRiskLevel
import com.codemx.anrdemo.anr.catalog.AnrScenario
import com.codemx.anrdemo.anr.catalog.ConfirmationRequirement
import com.codemx.anrdemo.anr.dispatch.AnrTriggerRequest

object SafetyPolicy {
    fun requirementFor(scenario: AnrScenario, request: AnrTriggerRequest = scenario.defaultRequest): ConfirmationRequirement {
        if (scenario.documentationOnlyReason != null) return ConfirmationRequirement.DisabledDocumentation
        if (request.allowDangerousOom) return ConfirmationRequirement.TypePhraseConfirm
        if (request.mode == "classic") return ConfirmationRequirement.TypePhraseConfirm
        if (scenario.riskLevel == AnrRiskLevel.Dangerous) return ConfirmationRequirement.TypePhraseConfirm
        return scenario.confirmationRequirement
    }

    fun phraseFor(scenario: AnrScenario, request: AnrTriggerRequest = scenario.defaultRequest): String? = when {
        request.allowDangerousOom -> "OOM"
        request.mode == "classic" -> "DEADLOCK"
        scenario.confirmationPhrase != null -> scenario.confirmationPhrase
        scenario.riskLevel == AnrRiskLevel.Dangerous -> scenario.id.uppercase().replace('-', '_')
        else -> null
    }

    fun requiresToken(scenario: AnrScenario, request: AnrTriggerRequest): Boolean =
        requirementFor(scenario, request) in setOf(
            ConfirmationRequirement.LongPressConfirm,
            ConfirmationRequirement.TypePhraseConfirm,
        )
}
