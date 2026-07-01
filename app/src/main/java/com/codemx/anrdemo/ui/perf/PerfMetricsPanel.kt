package com.codemx.anrdemo.ui.perf

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.codemx.anrdemo.perf.metrics.PerfMetricsSnapshot

/** 实时帧指标面板，镜像 anr 侧 DiagnosticsPanel 的角色。按卡顿率/FPS 颜色分级。 */
@Composable
fun PerfMetricsPanel(
    snapshot: PerfMetricsSnapshot,
    onReset: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("实时指标 (JankStats)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("FPS ≈ ${"%.0f".format(snapshot.approxFps)}", color = fpsColor(snapshot.approxFps))
                Text("卡顿率 ${"%.1f".format(snapshot.jankPercent)}%", color = jankColor(snapshot.jankPercent))
            }
            Text("卡顿帧 ${snapshot.jankyFrames} / ${snapshot.framesTracked}")
            Text("帧耗时 平均 ${"%.1f".format(snapshot.avgFrameMs)}ms · 最近 ${"%.1f".format(snapshot.lastFrameMs)}ms · 峰值 ${"%.1f".format(snapshot.maxFrameMs)}ms")
            OutlinedButton(onClick = onReset) { Text("重置指标") }
        }
    }
}

private fun jankColor(percent: Float): Color = when {
    percent >= 30f -> Color(0xFFC62828)
    percent >= 10f -> Color(0xFFEF6C00)
    else -> Color(0xFF2E7D32)
}

private fun fpsColor(fps: Float): Color = when {
    fps in 0.1f..30f -> Color(0xFFC62828)
    fps in 30f..50f -> Color(0xFFEF6C00)
    else -> Color(0xFF2E7D32)
}
