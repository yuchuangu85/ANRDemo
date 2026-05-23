package com.codemx.anrdemo.ui.trigger

import com.codemx.anrdemo.anr.catalog.AnrScenario
import com.codemx.anrdemo.anr.dispatch.AnrTriggerRequest
import com.codemx.anrdemo.anr.dispatch.TriggerResult

sealed interface TriggerFlowState {
    data object Idle : TriggerFlowState
    data class SelectingOptions(val scenario: AnrScenario) : TriggerFlowState
    data class Confirming(val scenario: AnrScenario, val request: AnrTriggerRequest) : TriggerFlowState
    data class AwaitingPhrase(val scenario: AnrScenario, val request: AnrTriggerRequest, val phrase: String) : TriggerFlowState
    data class CountingDown(val scenario: AnrScenario, val request: AnrTriggerRequest, val secondsLeft: Int) : TriggerFlowState
    data class Dispatching(val scenario: AnrScenario, val request: AnrTriggerRequest) : TriggerFlowState
    data class Finished(val result: TriggerResult) : TriggerFlowState
}
