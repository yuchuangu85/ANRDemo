package com.codemx.anrdemo.ui.diagnostics

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.codemx.anrdemo.anr.diagnostics.DiagnosticsSnapshot

@Composable
fun DiagnosticsPanel(
    snapshot: DiagnosticsSnapshot,
    onRefresh: () -> Unit,
    onClearMemory: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("诊断面板", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            snapshot.lastTriggerText?.let { Text("最近触发：$it") }
            Text("内存：retained=${snapshot.memory.retainedMemoryMb}MB, heap=${snapshot.memory.heapUsedMb}/${snapshot.memory.heapMaxMb}MB")
            snapshot.latestExitInfo?.let { Text("ExitInfo：$it") }
            snapshot.lastProviderResult?.let {
                Text("Provider：${it.elapsedMs}ms / requested=${it.requestedBlockMs}ms / slowQuery=${it.likelyAnrThresholdExceeded} / systemAnrEvidence=${it.systemAnrEvidence}")
                Text(it.note, style = MaterialTheme.typography.bodySmall)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onRefresh) { Text("刷新 ExitInfo") }
                OutlinedButton(onClick = onClearMemory) { Text("释放内存") }
            }
        }
    }
}
