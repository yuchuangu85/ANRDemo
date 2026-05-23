package com.codemx.anrdemo.anr.diagnostics

data class DiagnosticsSnapshot(
    val lastTriggerText: String? = null,
    val latestExitInfo: String? = null,
    val memory: MemoryPressureState = MemoryPressureState(0, 0, 0),
    val lastProviderResult: ProviderDiagnosticResult? = null,
)
