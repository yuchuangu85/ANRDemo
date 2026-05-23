package com.codemx.anrdemo.anr.diagnostics

import android.content.Context
import com.codemx.anrdemo.anr.dispatch.TriggerResult
import com.codemx.anrdemo.anr.triggers.MemoryPressureTriggers

class DiagnosticsRepository(private val context: Context) {
    private var lastTriggerText: String? = null
    private var lastExitInfo: String? = null
    private var lastProviderResult: ProviderDiagnosticResult? = null

    fun snapshot(refreshExitInfo: Boolean = false): DiagnosticsSnapshot {
        if (refreshExitInfo) lastExitInfo = ExitInfoReader.latestAnrSummary(context)
        val runtime = Runtime.getRuntime()
        val totalMb = (runtime.totalMemory() / MB).toInt()
        val freeMb = (runtime.freeMemory() / MB).toInt()
        val maxMb = (runtime.maxMemory() / MB).toInt()
        return DiagnosticsSnapshot(
            lastTriggerText = lastTriggerText,
            latestExitInfo = lastExitInfo,
            memory = MemoryPressureState(
                retainedMemoryMb = MemoryPressureTriggers.retainedMb(),
                heapUsedMb = totalMb - freeMb,
                heapMaxMb = maxMb,
            ),
            lastProviderResult = lastProviderResult,
        )
    }

    fun recordTriggerResult(result: TriggerResult) {
        lastTriggerText = when (result) {
            is TriggerResult.Started -> result.message
            is TriggerResult.Completed -> "同步触发完成，耗时 ${result.elapsedMs}ms"
            is TriggerResult.ProviderCompleted -> {
                lastProviderResult = result.result
                "Provider 查询完成，耗时 ${result.result.elapsedMs}ms"
            }
            is TriggerResult.Rejected -> "已拒绝：${result.reason}"
            TriggerResult.NotRunnableFromUi -> "该场景不支持 UI 触发"
        }
    }

    companion object {
        private const val MB = 1024 * 1024L
    }
}
