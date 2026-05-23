package com.codemx.anrdemo.ui

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
import com.codemx.anrdemo.anr.diagnostics.DiagnosticCommands
import com.codemx.anrdemo.anr.dispatch.AnrScenarioDispatcher
import com.codemx.anrdemo.anr.dispatch.TriggerResult
import com.codemx.anrdemo.anr.triggers.MemoryPressureTriggers

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnrDemoScreen(dispatcher: AnrScenarioDispatcher, modifier: Modifier = Modifier) {
    var selectedCategory by remember { mutableStateOf<AnrCategory?>(null) }
    var pendingScenario by remember { mutableStateOf<AnrScenario?>(null) }
    var commandScenario by remember { mutableStateOf<AnrScenario?>(null) }
    var lastResult by remember { mutableStateOf<String?>(null) }

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
                lastResult?.let {
                    Spacer(Modifier.height(8.dp))
                    Text("最近触发结果：$it", style = MaterialTheme.typography.bodyMedium)
                }
                Text("内存压力保留：${MemoryPressureTriggers.retainedMb()} MB", style = MaterialTheme.typography.bodySmall)
            }
            items(scenarios, key = { it.id }) { scenario ->
                ScenarioCard(
                    scenario = scenario,
                    onTrigger = { pendingScenario = scenario },
                    onCommand = { commandScenario = scenario },
                    onClearMemory = {
                        MemoryPressureTriggers.clearRetainedChunks()
                        lastResult = "已释放 memory-pressure 保留内存"
                    }
                )
            }
        }
    }

    pendingScenario?.let { scenario ->
        ConfirmAnrDialog(
            scenario = scenario,
            onDismiss = { pendingScenario = null },
            onConfirm = {
                pendingScenario = null
                lastResult = resultText(dispatcher.dispatch(scenario.defaultRequest))
            }
        )
    }

    commandScenario?.let { scenario ->
        AdbCommandDialog(scenario = scenario, onDismiss = { commandScenario = null })
    }
}

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
private fun ScenarioCard(
    scenario: AnrScenario,
    onTrigger: () -> Unit,
    onCommand: () -> Unit,
    onClearMemory: () -> Unit,
) {
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
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onTrigger, enabled = scenario.enabledByDefault || scenario.riskLevel != AnrRiskLevel.Dangerous) {
                    Text(if (scenario.enabledByDefault) "触发" else "高级禁用")
                }
                OutlinedButton(onClick = onCommand) { Text("命令") }
                if (scenario.id == "memory-pressure") {
                    OutlinedButton(onClick = onClearMemory) { Text("释放内存") }
                }
            }
        }
    }
}

@Composable
private fun ConfirmAnrDialog(scenario: AnrScenario, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("确认触发 ${scenario.title}？") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("该操作会故意阻塞或卡死 App。仅在测试设备运行。")
                Text("超时：${scenario.timeoutDescription}")
                Text("恢复：${scenario.recoveryCommand}")
            }
        },
        confirmButton = { Button(onClick = onConfirm) { Text("触发") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
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
        confirmButton = {
            Button(onClick = { clipboard.setText(AnnotatedString(command)); onDismiss() }) { Text("复制") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("关闭") } }
    )
}

private fun resultText(result: TriggerResult): String = when (result) {
    is TriggerResult.Started -> result.message
    is TriggerResult.Completed -> "同步触发完成，耗时 ${result.elapsedMs}ms"
    is TriggerResult.Rejected -> "已拒绝：${result.reason}"
    TriggerResult.NotRunnableFromUi -> "该场景不支持 UI 触发"
}
