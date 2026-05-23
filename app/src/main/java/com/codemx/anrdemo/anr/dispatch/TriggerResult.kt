package com.codemx.anrdemo.anr.dispatch

sealed class TriggerResult {
    data class Started(val message: String) : TriggerResult()
    data class Completed(val elapsedMs: Long) : TriggerResult()
    data class Rejected(val reason: String) : TriggerResult()
    data object NotRunnableFromUi : TriggerResult()
}
