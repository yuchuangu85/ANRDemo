package com.codemx.anrdemo.ui.trigger

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.delay

@Composable
fun CountdownEffect(
    state: TriggerFlowState,
    onTick: (TriggerFlowState.CountingDown) -> Unit,
    onFinished: (TriggerFlowState.CountingDown) -> Unit,
) {
    LaunchedEffect(state) {
        val counting = state as? TriggerFlowState.CountingDown ?: return@LaunchedEffect
        if (counting.secondsLeft <= 0) {
            onFinished(counting)
        } else {
            delay(1_000L)
            onTick(counting.copy(secondsLeft = counting.secondsLeft - 1))
        }
    }
}
