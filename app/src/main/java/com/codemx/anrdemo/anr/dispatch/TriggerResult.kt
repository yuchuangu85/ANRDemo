package com.codemx.anrdemo.anr.dispatch

import com.codemx.anrdemo.anr.diagnostics.ProviderDiagnosticResult

sealed class TriggerResult {
    data class Started(val message: String) : TriggerResult()
    data class Completed(val elapsedMs: Long) : TriggerResult()
    data class ProviderCompleted(val result: ProviderDiagnosticResult) : TriggerResult()
    data class Rejected(val reason: String) : TriggerResult()
    data object NotRunnableFromUi : TriggerResult()
}
