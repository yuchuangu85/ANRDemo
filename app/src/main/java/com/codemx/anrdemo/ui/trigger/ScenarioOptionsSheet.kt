package com.codemx.anrdemo.ui.trigger

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.dp
import com.codemx.anrdemo.anr.catalog.AnrScenario
import com.codemx.anrdemo.anr.catalog.ScenarioParameterSpec
import com.codemx.anrdemo.anr.catalog.ScenarioRequestBuilder

@Composable
fun ScenarioOptionsDialog(
    scenario: AnrScenario,
    onDismiss: () -> Unit,
    onContinue: (Map<String, String>) -> Unit,
) {
    val values = remember(scenario.id) { mutableStateMapOf<String, String>().apply { putAll(ScenarioRequestBuilder.defaultsFor(scenario)) } }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("配置 ${scenario.title}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                scenario.parameterSpecs.forEach { spec ->
                    when (spec) {
                        is ScenarioParameterSpec.DurationMs -> StepperRow(spec.label, values[spec.key]?.toLongOrNull() ?: spec.defaultValue, spec.min, spec.max, 1_000L) { values[spec.key] = it.toString() }
                        is ScenarioParameterSpec.IntRange -> StepperRow(spec.label, (values[spec.key]?.toIntOrNull() ?: spec.defaultValue).toLong(), spec.min.toLong(), spec.max.toLong(), spec.step.toLong()) { values[spec.key] = it.toString() }
                        is ScenarioParameterSpec.Choice -> {
                            Text(spec.label)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                spec.options.forEach { option ->
                                    FilterChip(selected = values[spec.key] == option.value, onClick = { values[spec.key] = option.value }, label = { Text(option.label) })
                                }
                            }
                        }
                        is ScenarioParameterSpec.BooleanToggle -> {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(spec.label)
                                Switch(checked = values[spec.key]?.toBooleanStrictOrNull() ?: spec.defaultValue, onCheckedChange = { values[spec.key] = it.toString() })
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { Button(onClick = { onContinue(values.toMap()) }) { Text("继续") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}

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
