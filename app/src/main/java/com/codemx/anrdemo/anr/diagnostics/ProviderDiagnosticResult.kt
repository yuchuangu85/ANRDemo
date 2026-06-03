package com.codemx.anrdemo.anr.diagnostics

data class ProviderDiagnosticResult(
    val uri: String,
    val elapsedMs: Long,
    val requestedBlockMs: Long,
    val likelyAnrThresholdExceeded: Boolean,
    val systemAnrEvidence: Boolean,
    val note: String,
)
