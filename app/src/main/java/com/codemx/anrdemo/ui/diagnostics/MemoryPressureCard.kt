package com.codemx.anrdemo.ui.diagnostics

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.codemx.anrdemo.anr.diagnostics.MemoryPressureState

@Composable
fun MemoryPressureCard(state: MemoryPressureState) {
    Text("retained=${state.retainedMemoryMb}MB heap=${state.heapUsedMb}/${state.heapMaxMb}MB")
}
