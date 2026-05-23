package com.codemx.anrdemo.ui.trigger

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import com.codemx.anrdemo.anr.catalog.AnrScenario

@Composable
fun NormalConfirmDialog(
    scenario: AnrScenario,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
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
        confirmButton = { Button(onClick = onConfirm) { Text("开始倒计时") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}

@Composable
fun PhraseConfirmDialog(
    scenario: AnrScenario,
    phrase: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    var text by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("高危确认：${scenario.title}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("该场景可能长时间卡死、不可自动恢复或直接崩溃。请输入 $phrase 继续。")
                OutlinedTextField(value = text, onValueChange = { text = it }, label = { Text("确认短语") })
                Text("恢复：${scenario.recoveryCommand}")
            }
        },
        confirmButton = { Button(onClick = onConfirm, enabled = text == phrase) { Text("开始倒计时") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}

@Composable
fun CountdownDialog(secondsLeft: Int, onCancel: () -> Unit) {
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text("即将触发 ANR") },
        text = { Text("$secondsLeft 秒后触发，可取消。") },
        confirmButton = { TextButton(onClick = onCancel) { Text("取消") } },
    )
}
