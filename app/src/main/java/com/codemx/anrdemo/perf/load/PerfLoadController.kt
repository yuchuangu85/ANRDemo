package com.codemx.anrdemo.perf.load

import android.graphics.Bitmap
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Choreographer
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.codemx.anrdemo.perf.PerfLogTags
import com.codemx.anrdemo.perf.catalog.PerfCatalog
import com.codemx.anrdemo.perf.catalog.PerfRequest
import com.codemx.anrdemo.perf.catalog.PerfScenario
import com.codemx.anrdemo.perf.catalog.PerfTriggerKind

/**
 * 性能负载引擎。类比 AnrScenarioDispatcher，但性能场景是有状态的 start/stop：
 * - Load 型：在主线程/内存加压（周期阻塞、每帧忙等、分配抖动、大图解码…），带 [PerfScenario.autoStopMs] 自动停止护栏。
 * - Visual 型：只记录 activeScenario，重 UI 由 PerfScreen 演示区渲染。
 *
 * 在 MainActivity 构造并下传（无 ViewModel/DI，沿用现有风格）。所有回调运行在主线程。
 */
class PerfLoadController {
    var activeScenario by mutableStateOf<PerfScenario?>(null)
        private set
    var activeRequest by mutableStateOf<PerfRequest?>(null)
        private set

    /** 重组风暴驱动的计数器：每帧自增，供演示区订阅以强制高频重组。 */
    var stormTick by mutableIntStateOf(0)
        private set

    private val handler = Handler(Looper.getMainLooper())
    private val choreographer = Choreographer.getInstance()
    private var frameCallback: Choreographer.FrameCallback? = null
    private val churnSink = ArrayList<ByteArray>()

    fun isActive(id: String): Boolean = activeScenario?.id == id

    fun start(scenario: PerfScenario, request: PerfRequest) {
        stop()
        Log.d(PerfLogTags.LOAD, "start ${scenario.id} req=$request")
        activeScenario = scenario
        activeRequest = request
        when (scenario.triggerKind) {
            PerfTriggerKind.PeriodicMainThreadBlock -> startPeriodicBlock(request)
            PerfTriggerKind.OneShotLongTask -> startOneShot(request)
            PerfTriggerKind.RecompositionStorm -> startRecompositionStorm()
            PerfTriggerKind.PerFrameHeavyWork -> startPerFrameHeavyWork(request)
            PerfTriggerKind.AllocationChurn -> startAllocationChurn(request)
            PerfTriggerKind.LargeBitmapDecode -> startLargeBitmapDecode(request)
            // Visual 型无后台负载，仅渲染演示区
            PerfTriggerKind.OverdrawLayers,
            PerfTriggerKind.DeepLayoutNesting,
            PerfTriggerKind.HeavyScroll -> Unit
        }
        scenario.autoStopMs?.let { autoStop ->
            handler.postDelayed({ stop() }, autoStop)
        }
    }

    fun stop() {
        val previous = activeScenario ?: return
        Log.d(PerfLogTags.LOAD, "stop ${previous.id}")
        handler.removeCallbacksAndMessages(null)
        frameCallback?.let { choreographer.removeFrameCallback(it) }
        frameCallback = null
        churnSink.clear()
        activeScenario = null
        activeRequest = null
    }

    private fun startPeriodicBlock(request: PerfRequest) {
        val blockMs = request.blockMs ?: 120
        val intervalMs = request.intervalMs ?: 600
        val runnable = object : Runnable {
            override fun run() {
                Thread.sleep(blockMs) // 阻塞主线程 → 卡顿
                handler.postDelayed(this, intervalMs)
            }
        }
        handler.postDelayed(runnable, intervalMs)
    }

    private fun startOneShot(request: PerfRequest) {
        val blockMs = request.blockMs ?: 700
        handler.post {
            Thread.sleep(blockMs)
            stop()
        }
    }

    private fun startRecompositionStorm() {
        val callback = object : Choreographer.FrameCallback {
            override fun doFrame(frameTimeNanos: Long) {
                stormTick++ // 每帧变更 state → 触发订阅子树高频重组
                frameCallback?.let { choreographer.postFrameCallback(it) }
            }
        }
        frameCallback = callback
        choreographer.postFrameCallback(callback)
    }

    private fun startPerFrameHeavyWork(request: PerfRequest) {
        val blockMs = request.blockMs ?: 12
        val callback = object : Choreographer.FrameCallback {
            override fun doFrame(frameTimeNanos: Long) {
                busySpin(blockMs) // 每帧 CPU 忙等，逼近/超过单帧预算 → 丢帧
                frameCallback?.let { choreographer.postFrameCallback(it) }
            }
        }
        frameCallback = callback
        choreographer.postFrameCallback(callback)
    }

    private fun startAllocationChurn(request: PerfRequest) {
        val intervalMs = request.intervalMs ?: 50
        val runnable = object : Runnable {
            override fun run() {
                churnSink.add(ByteArray(1_000_000)) // ~1MB
                if (churnSink.size > 20) churnSink.clear() // 丢弃 → GC 压力
                handler.postDelayed(this, intervalMs)
            }
        }
        handler.post(runnable)
    }

    private fun startLargeBitmapDecode(request: PerfRequest) {
        val px = request.bitmapPx ?: 1500
        val runnable = object : Runnable {
            override fun run() {
                val bitmap = Bitmap.createBitmap(px, px, Bitmap.Config.ARGB_8888)
                bitmap.eraseColor(0xFF3366CC.toInt()) // 触碰像素模拟解码成本
                bitmap.recycle()
                handler.postDelayed(this, 200)
            }
        }
        handler.post(runnable)
    }

    private fun busySpin(durationMs: Long) {
        val end = System.nanoTime() + durationMs * 1_000_000
        while (System.nanoTime() < end) {
            // 主动占用主线程
        }
    }

    companion object {
        /** 从 catalog 恢复场景（用于按 id 启动，便于扩展/测试）。 */
        fun scenarioOf(id: String): PerfScenario? = PerfCatalog.scenario(id)
    }
}
