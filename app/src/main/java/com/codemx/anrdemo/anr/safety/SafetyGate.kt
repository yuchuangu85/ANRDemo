package com.codemx.anrdemo.anr.safety

import android.os.SystemClock
import com.codemx.anrdemo.anr.catalog.AnrScenario
import com.codemx.anrdemo.anr.catalog.ConfirmationRequirement
import com.codemx.anrdemo.anr.dispatch.AnrTriggerRequest
import com.codemx.anrdemo.anr.dispatch.TriggerResult

class SafetyGate {
    fun check(scenario: AnrScenario, request: AnrTriggerRequest): TriggerResult.Rejected? {
        scenario.minApi?.let { minApi ->
            if (!DeviceCapabilities.isAtLeast(minApi)) {
                return TriggerResult.Rejected("需要 Android API $minApi 或更高版本")
            }
        }
        scenario.maxApi?.let { maxApi ->
            if (DeviceCapabilities.isAtLeast(maxApi + 1)) {
                return TriggerResult.Rejected("该场景只适用于 Android API $maxApi 或更低版本")
            }
        }
        if (SafetyPolicy.requirementFor(scenario, request) == ConfirmationRequirement.DisabledDocumentation) {
            return TriggerResult.Rejected(scenario.documentationOnlyReason ?: "该场景仅用于说明")
        }
        if (SafetyPolicy.requiresToken(scenario, request)) {
            val token = request.confirmationToken
            if (token == null || token.scenarioId != scenario.id) {
                return TriggerResult.Rejected("高危场景需要通过 UI 强确认后才能触发")
            }
            val ageMs = SystemClock.elapsedRealtime() - token.issuedAtElapsedMs
            if (ageMs > 30_000L) {
                return TriggerResult.Rejected("确认已过期，请重新确认")
            }
        }
        return null
    }
}
