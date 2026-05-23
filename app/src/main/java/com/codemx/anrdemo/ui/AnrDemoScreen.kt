package com.codemx.anrdemo.ui

import android.os.SystemClock
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.codemx.anrdemo.anr.catalog.AnrCatalog
import com.codemx.anrdemo.anr.catalog.AnrCategory
import com.codemx.anrdemo.anr.catalog.AnrRiskLevel
import com.codemx.anrdemo.anr.catalog.AnrScenario
import com.codemx.anrdemo.anr.catalog.ConfirmationRequirement
import com.codemx.anrdemo.anr.catalog.ScenarioRequestBuilder
import com.codemx.anrdemo.anr.diagnostics.DiagnosticCommands
import com.codemx.anrdemo.anr.diagnostics.DiagnosticsRepository
import com.codemx.anrdemo.anr.dispatch.AnrScenarioDispatcher
import com.codemx.anrdemo.anr.dispatch.AnrTriggerRequest
import com.codemx.anrdemo.anr.safety.ConfirmationToken
import com.codemx.anrdemo.anr.safety.SafetyPolicy
import com.codemx.anrdemo.anr.triggers.MemoryPressureTriggers
import com.codemx.anrdemo.ui.diagnostics.DiagnosticsPanel
import com.codemx.anrdemo.ui.trigger.CountdownDialog
import com.codemx.anrdemo.ui.trigger.CountdownEffect
import com.codemx.anrdemo.ui.trigger.NormalConfirmDialog
import com.codemx.anrdemo.ui.trigger.PhraseConfirmDialog
import com.codemx.anrdemo.ui.trigger.ScenarioOptionsDialog
import com.codemx.anrdemo.ui.trigger.TriggerFlowState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnrDemoScreen(
    dispatcher: AnrScenarioDispatcher,
    diagnosticsRepository: DiagnosticsRepository,
    modifier: Modifier = Modifier,
) {
    var selectedCategory by remember { mutableStateOf<AnrCategory?>(null) }
    var commandScenario by remember { mutableStateOf<AnrScenario?>(null) }
    var flowState by remember { mutableStateOf<TriggerFlowState>(TriggerFlowState.Idle) }
    var diagnostics by remember { mutableStateOf(diagnosticsRepository.snapshot()) }

    fun refreshDiagnostics(refreshExit: Boolean = false) {
        diagnostics = diagnosticsRepository.snapshot(refreshExitInfo = refreshExit)
    }

    fun dispatchAndRecord(scenario: AnrScenario, request: AnrTriggerRequest) {
        val result = dispatcher.dispatch(request)
        diagnosticsRepository.recordTriggerResult(result)
        refreshDiagnostics()
        flowState = TriggerFlowState.Finished(result)
    }

    CountdownEffect(
        state = flowState,
        onTick = { flowState = it },
        onFinished = { counting ->
            flowState = TriggerFlowState.Dispatching(counting.scenario, counting.request)
            dispatchAndRecord(counting.scenario, counting.request)
        }
    )

    val scenarios = remember(selectedCategory) {
        AnrCatalog.scenarios.filter { selectedCategory == null || it.category == selectedCategory }
    }

    Scaffold(modifier = modifier.fillMaxSize()) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Text("ANR Demo", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Text("会故意卡死 App，仅在测试机运行。危险场景触发后可使用 force-stop 恢复。")
                Spacer(Modifier.height(8.dp))
                CategoryChips(selectedCategory) { selectedCategory = it }
                Spacer(Modifier.height(8.dp))
                DiagnosticsPanel(
                    snapshot = diagnostics,
                    onRefresh = { refreshDiagnostics(refreshExit = true) },
                    onClearMemory = {
                        MemoryPressureTriggers.clearRetainedChunks()
                        diagnosticsRepository.recordTriggerResult(com.codemx.anrdemo.anr.dispatch.TriggerResult.Started("已释放 memory-pressure 保留内存"))
                        refreshDiagnostics()
                    }
                )
            }
            items(scenarios, key = { it.id }) { scenario ->
                ScenarioCard(
                    scenario = scenario,
                    onTrigger = { flowState = TriggerFlowState.SelectingOptions(scenario) },
                    onCommand = { commandScenario = scenario },
                )
            }
        }
    }

    when (val state = flowState) {
        TriggerFlowState.Idle -> Unit
        is TriggerFlowState.SelectingOptions -> ScenarioOptionsDialog(
            scenario = state.scenario,
            onDismiss = { flowState = TriggerFlowState.Idle },
            onContinue = { values ->
                val request = ScenarioRequestBuilder.build(state.scenario, values)
                when (SafetyPolicy.requirementFor(state.scenario, request)) {
                    ConfirmationRequirement.DisabledDocumentation -> flowState = TriggerFlowState.Confirming(state.scenario, request)
                    ConfirmationRequirement.TypePhraseConfirm -> flowState = TriggerFlowState.AwaitingPhrase(state.scenario, request, SafetyPolicy.phraseFor(state.scenario, request) ?: state.scenario.id.uppercase())
                    ConfirmationRequirement.LongPressConfirm,
                    ConfirmationRequirement.NormalConfirm,
                    ConfirmationRequirement.CountdownConfirm -> flowState = TriggerFlowState.Confirming(state.scenario, request)
                }
            }
        )
        is TriggerFlowState.Confirming -> NormalConfirmDialog(
            scenario = state.scenario,
            onDismiss = { flowState = TriggerFlowState.Idle },
            onConfirm = {
                val request = withTokenIfNeeded(state.scenario, state.request)
                flowState = TriggerFlowState.CountingDown(state.scenario, request, 3)
            }
        )
        is TriggerFlowState.AwaitingPhrase -> PhraseConfirmDialog(
            scenario = state.scenario,
            phrase = state.phrase,
            onDismiss = { flowState = TriggerFlowState.Idle },
            onConfirm = {
                val request = state.request.copy(
                    confirmationToken = ConfirmationToken(state.scenario.id, SystemClock.elapsedRealtime(), SafetyPolicy.requirementFor(state.scenario, state.request))
                )
                flowState = TriggerFlowState.CountingDown(state.scenario, request, 3)
            }
        )
        is TriggerFlowState.CountingDown -> CountdownDialog(state.secondsLeft) { flowState = TriggerFlowState.Idle }
        is TriggerFlowState.Dispatching -> Unit
        is TriggerFlowState.Finished -> flowState = TriggerFlowState.Idle
    }

    commandScenario?.let { scenario ->
        AdbCommandDialog(scenario = scenario, onDismiss = { commandScenario = null })
    }
}

private fun withTokenIfNeeded(scenario: AnrScenario, request: AnrTriggerRequest): AnrTriggerRequest =
    if (SafetyPolicy.requiresToken(scenario, request)) {
        request.copy(confirmationToken = ConfirmationToken(scenario.id, SystemClock.elapsedRealtime(), SafetyPolicy.requirementFor(scenario, request)))
    } else request

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryChips(selected: AnrCategory?, onSelected: (AnrCategory?) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = selected == null, onClick = { onSelected(null) }, label = { Text("全部") })
            FilterChip(selected = selected == AnrCategory.UserPerceived, onClick = { onSelected(AnrCategory.UserPerceived) }, label = { Text("用户可感知") })
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = selected == AnrCategory.ComponentLifecycle, onClick = { onSelected(AnrCategory.ComponentLifecycle) }, label = { Text("组件") })
            FilterChip(selected = selected == AnrCategory.ResourcePressure, onClick = { onSelected(AnrCategory.ResourcePressure) }, label = { Text("资源压力") })
            FilterChip(selected = selected == AnrCategory.LongRunningAdvanced, onClick = { onSelected(AnrCategory.LongRunningAdvanced) }, label = { Text("高级") })
        }
    }
}

@Composable
private fun ScenarioCard(scenario: AnrScenario, onTrigger: () -> Unit, onCommand: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AssistChip(onClick = {}, label = { Text(scenario.category.displayName) })
                AssistChip(onClick = {}, label = { Text("风险 ${scenario.riskLevel.displayName}") })
            }
            Text(scenario.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(scenario.timeoutDescription, style = MaterialTheme.typography.bodyMedium)
            Text(scenario.explanation, style = MaterialTheme.typography.bodySmall)
            Text("预期：${scenario.expectedReason}", style = MaterialTheme.typography.bodySmall)
            scenario.confirmationPhrase?.let { Text("强确认短语：$it", style = MaterialTheme.typography.bodySmall) }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onTrigger, enabled = scenario.documentationOnlyReason == null) {
                    Text(if (scenario.documentationOnlyReason == null) "触发" else "仅说明")
                }
                OutlinedButton(onClick = onCommand) { Text("命令") }
            }
        }
    }
}

@Composable
private fun AdbCommandDialog(scenario: AnrScenario, onDismiss: () -> Unit) {
    val clipboard = LocalClipboardManager.current
    val command = buildString {
        appendLine(DiagnosticCommands.LOGCAT)
        appendLine()
        appendLine(scenario.adbCommand ?: "该场景暂无 adb 直达命令")
        appendLine()
        appendLine(DiagnosticCommands.EXIT_INFO)
        appendLine(DiagnosticCommands.FORCE_STOP)
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("adb / 诊断命令") },
        text = { Text(command) },
        confirmButton = { Button(onClick = { clipboard.setText(AnnotatedString(command)); onDismiss() }) { Text("复制") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("关闭") } }
    )
}
