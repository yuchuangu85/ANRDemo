package com.codemx.anrdemo.ui.diagnostics

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.codemx.anrdemo.anr.diagnostics.ProviderDiagnosticResult

@Composable
fun ProviderDiagnosticCard(result: ProviderDiagnosticResult?) {
    Text(result?.let { "Provider query ${it.elapsedMs}ms (${it.uri})" } ?: "暂无 Provider 诊断")
}
