package com.codemx.anrdemo.anr.safety

import com.codemx.anrdemo.anr.catalog.AnrRiskLevel
import com.codemx.anrdemo.anr.catalog.AnrScenario
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
        if (scenario.riskLevel == AnrRiskLevel.Dangerous && scenario.enabledByDefault && !request.allowDangerousOom) {
            // Dangerous scenarios can still be run from explicit cards; this guard mainly protects dangerous OOM.
        }
        return null
    }
}
