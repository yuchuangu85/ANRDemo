package com.codemx.anrdemo.ui.perf.stages

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/** 演示区固定高度，用于承载各类重 UI 与参照动画。 */
private val StageHeight = 240.dp

/** 过度绘制：同一区域叠加多层不透明背景，同一像素被反复绘制。 */
@Composable
fun OverdrawStage(layerCount: Int, modifier: Modifier = Modifier) {
    val palette = listOf(
        Color(0xFF1565C0), Color(0xFF2E7D32), Color(0xFFC62828), Color(0xFF6A1B9A),
        Color(0xFFEF6C00), Color(0xFF00838F), Color(0xFF4E342E), Color(0xFFAD1457),
    )
    Box(modifier.fillMaxWidth().height(StageHeight)) {
        repeat(layerCount) { i ->
            Box(
                Modifier
                    .fillMaxSize()
                    .padding((i * 6).dp)
                    .background(palette[i % palette.size]) // 不透明叠加 → 过度绘制
            )
        }
        Text(
            "叠加 $layerCount 层不透明背景\n开启「调试 GPU 过度绘制」查看红色区域",
            color = Color.White,
            modifier = Modifier.align(Alignment.Center),
        )
    }
}

/** 参照动画：匀速左右移动的方块，加压时会肉眼可见地卡顿/丢帧。 */
@Composable
fun JankAnimationStage(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "jank-ref")
    val fraction by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1200, easing = LinearEasing), RepeatMode.Reverse),
        label = "offset",
    )
    Box(
        modifier
            .fillMaxWidth()
            .height(StageHeight)
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(
            Modifier
                .offset(x = (fraction * 300).dp)
                .size(48.dp)
                .background(MaterialTheme.colorScheme.primary)
        )
        Text("参照动画：应保持匀速；卡顿/丢帧时会顿挫", modifier = Modifier.align(Alignment.BottomCenter))
    }
}

/** 重组风暴：读取每帧自增的 tick，强制高频重组一颗较重的子树。 */
@Composable
fun RecompositionStormStage(tick: Int, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().height(StageHeight), contentAlignment = Alignment.Center) {
        Column {
            Text("重组风暴 tick=$tick", style = MaterialTheme.typography.titleMedium)
            // 每帧因 tick 变化而重组，构造较重内容放大重组成本
            repeat(30) { row ->
                Row {
                    repeat(6) { col ->
                        Text(text = ((tick + row * 6 + col) % 10).toString(), modifier = Modifier.padding(1.dp))
                    }
                }
            }
        }
    }
}

/** 深层布局嵌套：递归嵌套 Box，放大 measure/layout 开销。 */
@Composable
fun DeepNestingStage(depth: Int, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().height(StageHeight), contentAlignment = Alignment.Center) {
        NestedBox(depth)
        Text("嵌套深度=$depth", modifier = Modifier.align(Alignment.TopCenter))
    }
}

@Composable
private fun NestedBox(remaining: Int) {
    if (remaining <= 0) return
    Box(
        Modifier
            .padding(2.dp)
            .background(if (remaining % 2 == 0) Color(0x22000000) else Color(0x11000000))
    ) {
        NestedBox(remaining - 1)
    }
}

/** 重 item 长列表：每个 item 组合成本较高，滑动时易掉帧。 */
@Composable
fun HeavyScrollStage(itemCount: Int, modifier: Modifier = Modifier) {
    val data = (0 until itemCount).toList()
    LazyColumn(
        modifier.fillMaxWidth().height(StageHeight),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        items(data) { index ->
            Row(
                Modifier.fillMaxWidth().padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                // 每个 item 内放较多小色块，抬高单项组合/绘制成本
                repeat(24) { c ->
                    Box(
                        Modifier
                            .size(10.dp)
                            .background(Color(0xFF000000 + ((index * 37 + c * 991) and 0xFFFFFF)))
                    )
                }
                Text("#$index")
            }
        }
    }
}
