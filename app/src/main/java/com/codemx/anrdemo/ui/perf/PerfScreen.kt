package com.codemx.anrdemo.ui.perf

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.codemx.anrdemo.perf.catalog.PerfCatalog
import com.codemx.anrdemo.perf.catalog.PerfCategory
import com.codemx.anrdemo.perf.catalog.PerfRequestBuilder
import com.codemx.anrdemo.perf.catalog.PerfScenario
import com.codemx.anrdemo.perf.catalog.PerfTriggerKind
import com.codemx.anrdemo.perf.load.PerfLoadController
import com.codemx.anrdemo.perf.metrics.PerfMetricsMonitor
import com.codemx.anrdemo.perf.metrics.PerfMetricsSnapshot
import com.codemx.anrdemo.ui.perf.stages.DeepNestingStage
import com.codemx.anrdemo.ui.perf.stages.HeavyScrollStage
import com.codemx.anrdemo.ui.perf.stages.JankAnimationStage
import com.codemx.anrdemo.ui.perf.stages.OverdrawStage
import com.codemx.anrdemo.ui.perf.stages.RecompositionStormStage

@Composable
fun PerfScreen(
    perfLoadController: PerfLoadController,
    modifier: Modifier = Modifier,
) {
    val activity = LocalContext.current.findActivity()
    val snapshotState = remember { mutableStateOf(PerfMetricsSnapshot()) }
    val monitor = remember(activity) {
        activity?.window?.let { window -> PerfMetricsMonitor(window) { snapshotState.value = it } }
    }
    DisposableEffect(monitor) {
        monitor?.setTracking(true)
        onDispose { monitor?.dispose() }
    }
    // 离开性能页时停止任何仍在运行的负载
    DisposableEffect(Unit) {
        onDispose { perfLoadController.stop() }
    }

    var selectedCategory by remember { mutableStateOf<PerfCategory?>(null) }
    var optionsScenario by remember { mutableStateOf<PerfScenario?>(null) }

    val scenarios = remember(selectedCategory) {
        PerfCatalog.scenarios.filter { selectedCategory == null || it.category == selectedCategory }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text("性能测试", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text("触发过度绘制/卡顿/丢帧等性能问题，配合下方实时指标与开发者选项观察。负载场景到时自动停止。")
            Spacer(Modifier.height(8.dp))
            PerfMetricsPanel(snapshot = snapshotState.value, onReset = { monitor?.reset() })
            Spacer(Modifier.height(8.dp))
            PerfCategoryChips(selectedCategory) { selectedCategory = it }
            Spacer(Modifier.height(8.dp))
            DemoStage(perfLoadController)
        }
        items(scenarios, key = { it.id }) { scenario ->
            PerfScenarioCard(
                scenario = scenario,
                active = perfLoadController.isActive(scenario.id),
                onStart = {
                    if (scenario.parameterSpecs.isEmpty()) {
                        perfLoadController.start(scenario, PerfRequestBuilder.build(scenario, emptyMap()))
                    } else {
                        optionsScenario = scenario
                    }
                },
                onStop = { perfLoadController.stop() },
            )
        }
    }

    optionsScenario?.let { scenario ->
        PerfOptionsDialog(
            scenario = scenario,
            onDismiss = { optionsScenario = null },
            onContinue = { values ->
                perfLoadController.start(scenario, PerfRequestBuilder.build(scenario, values))
                optionsScenario = null
            },
        )
    }
}

@Composable
private fun DemoStage(controller: PerfLoadController) {
    val scenario = controller.activeScenario
    if (scenario == null) {
        Text("演示区：开始一个场景后在此显示效果 / 参照动画。", style = MaterialTheme.typography.bodyMedium)
        return
    }
    val request = controller.activeRequest
    when (scenario.triggerKind) {
        PerfTriggerKind.OverdrawLayers -> OverdrawStage(request?.layerCount ?: 8)
        PerfTriggerKind.DeepLayoutNesting -> DeepNestingStage(request?.nestingDepth ?: 30)
        PerfTriggerKind.HeavyScroll -> HeavyScrollStage(request?.itemCount ?: 300)
        PerfTriggerKind.RecompositionStorm -> RecompositionStormStage(controller.stormTick)
        // 其余 Load 型：显示参照动画，加压时可见卡顿/丢帧
        PerfTriggerKind.PeriodicMainThreadBlock,
        PerfTriggerKind.OneShotLongTask,
        PerfTriggerKind.PerFrameHeavyWork,
        PerfTriggerKind.AllocationChurn,
        PerfTriggerKind.LargeBitmapDecode -> JankAnimationStage()
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PerfCategoryChips(selected: PerfCategory?, onSelected: (PerfCategory?) -> Unit) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        FilterChip(selected = selected == null, onClick = { onSelected(null) }, label = { Text("全部") })
        PerfCategory.entries.forEach { category ->
            FilterChip(
                selected = selected == category,
                onClick = { onSelected(category) },
                label = { Text(category.displayName) },
            )
        }
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
