package com.codemx.anrdemo.ui.perf

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.codemx.anrdemo.anr.catalog.ScenarioParameterSpec
import com.codemx.anrdemo.perf.catalog.PerfRequestBuilder
import com.codemx.anrdemo.perf.catalog.PerfScenario

@Composable
fun PerfScenarioCard(
    scenario: PerfScenario,
    active: Boolean,
    onStart: () -> Unit,
    onStop: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AssistChip(onClick = {}, label = { Text(scenario.category.displayName) })
                AssistChip(onClick = {}, label = { Text(if (scenario.isLoad) "负载" else "渲染") })
            }
            Text(scenario.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(scenario.description, style = MaterialTheme.typography.bodyMedium)
            Text("如何观察：${scenario.howToObserve}", style = MaterialTheme.typography.bodySmall)
            Text("预期：${scenario.expectedSymptom}", style = MaterialTheme.typography.bodySmall)
            scenario.autoStopMs?.let { Text("自动停止：${it / 1000}s", style = MaterialTheme.typography.bodySmall) }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (active) {
                    Button(onClick = onStop) { Text("停止") }
                } else {
                    Button(onClick = onStart) { Text(if (scenario.isLoad) "开始" else "显示") }
                }
            }
        }
    }
}

/** 参数配置弹窗（复用 ANR 侧的 ScenarioParameterSpec 类型）。 */
@Composable
fun PerfOptionsDialog(
    scenario: PerfScenario,
    onDismiss: () -> Unit,
    onContinue: (Map<String, String>) -> Unit,
) {
    val values = remember(scenario.id) {
        mutableStateMapOf<String, String>().apply { putAll(PerfRequestBuilder.defaultsFor(scenario)) }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("配置 ${scenario.title}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                scenario.parameterSpecs.forEach { spec ->
                    when (spec) {
                        is ScenarioParameterSpec.DurationMs -> StepperRow(spec.label, values[spec.key]?.toLongOrNull() ?: spec.defaultValue, spec.min, spec.max, stepFor(spec.min, spec.max)) { values[spec.key] = it.toString() }
                        is ScenarioParameterSpec.IntRange -> StepperRow(spec.label, (values[spec.key]?.toIntOrNull() ?: spec.defaultValue).toLong(), spec.min.toLong(), spec.max.toLong(), spec.step.toLong()) { values[spec.key] = it.toString() }
                        is ScenarioParameterSpec.Choice -> {
                            Text(spec.label)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                spec.options.forEach { option ->
                                    FilterChip(selected = values[spec.key] == option.value, onClick = { values[spec.key] = option.value }, label = { Text(option.label) })
                                }
                            }
                        }
                        is ScenarioParameterSpec.BooleanToggle -> Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(spec.label)
                            Switch(checked = values[spec.key]?.toBooleanStrictOrNull() ?: spec.defaultValue, onCheckedChange = { values[spec.key] = it.toString() })
                        }
                    }
                }
            }
        },
        confirmButton = { Button(onClick = { onContinue(values.toMap()) }) { Text("开始") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}

private fun stepFor(min: Long, max: Long): Long = if (max - min >= 2_000) 100L else 10L

@Composable
private fun StepperRow(label: String, value: Long, min: Long, max: Long, step: Long, onValue: (Long) -> Unit) {
    Column {
        Text("$label: $value")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { onValue((value - step).coerceAtLeast(min)) }) { Text("-") }
            OutlinedButton(onClick = { onValue((value + step).coerceAtMost(max)) }) { Text("+") }
        }
    }
}
