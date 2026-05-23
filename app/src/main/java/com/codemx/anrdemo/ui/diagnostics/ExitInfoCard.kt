package com.codemx.anrdemo.ui.diagnostics

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

@Composable
fun ExitInfoCard(summary: String?) {
    Text(summary ?: "暂无 ExitInfo")
}
