package com.codemx.anrdemo.anr.catalog

import android.os.Build
import com.codemx.anrdemo.anr.dispatch.AnrTriggerRequest

object AnrCatalog {
    val scenarios: List<AnrScenario> = listOf(
        AnrScenario(
            id = "input-dispatch",
            title = "Input dispatch timeout",
            category = AnrCategory.UserPerceived,
            riskLevel = AnrRiskLevel.Medium,
            triggerKind = AnrTriggerKind.MainThreadBlock,
            timeoutDescription = "默认 5 秒；Demo 阻塞主线程 8 秒。",
            recommendedBlockMs = AnrDefaults.INPUT_BLOCK_MS,
            expectedReason = "Input dispatching timed out",
            explanation = "前台窗口主线程不响应触摸/按键超过默认阈值。触发后请继续点击或滑动屏幕。",
            defaultRequest = AnrTriggerRequest("input-dispatch", blockMs = AnrDefaults.INPUT_BLOCK_MS),
            adbCommand = deepLink("input-dispatch", "blockMs=${AnrDefaults.INPUT_BLOCK_MS}")
        ),
        AnrScenario(
            id = "no-focused-window",
            title = "Input dispatch - no focused window",
            category = AnrCategory.DiagnosticOnly,
            riskLevel = AnrRiskLevel.High,
            triggerKind = AnrTriggerKind.AdbOnly,
            timeoutDescription = "默认 5 秒；设备/窗口状态相关。",
            recommendedBlockMs = null,
            expectedReason = "Input dispatching timed out ... no focused window",
            explanation = "通常由首帧过慢或窗口不可聚焦造成；首版作为诊断说明，不默认执行。",
            defaultRequest = AnrTriggerRequest("no-focused-window"),
            adbCommand = null,
            enabledByDefault = false
        ),
        AnrScenario(
            id = "deadlock",
            title = "Deadlock / 锁竞争导致 ANR",
            category = AnrCategory.UserPerceived,
            riskLevel = AnrRiskLevel.High,
            triggerKind = AnrTriggerKind.Deadlock,
            timeoutDescription = "死锁不是独立系统阈值；默认用 Input dispatch 5 秒触发。",
            recommendedBlockMs = AnrDefaults.INPUT_BLOCK_MS,
            expectedReason = "Input dispatching timed out; main thread Blocked/Waiting",
            explanation = "根因场景：主线程等待后台线程持有的锁，或进入经典互锁，导致 Input dispatch ANR。默认使用可恢复的锁竞争模式。",
            defaultRequest = AnrTriggerRequest("deadlock", mode = "contention", blockMs = AnrDefaults.INPUT_BLOCK_MS),
            adbCommand = deepLink("deadlock", "mode=contention&blockMs=${AnrDefaults.INPUT_BLOCK_MS}")
        ),
        AnrScenario(
            id = "memory-pressure",
            title = "内存泄漏 / GC thrash 诱发 ANR",
            category = AnrCategory.ResourcePressure,
            riskLevel = AnrRiskLevel.High,
            triggerKind = AnrTriggerKind.MemoryPressure,
            timeoutDescription = "非独立阈值；内存压力拖慢主线程后按对应组件阈值触发。默认按 Input dispatch 5 秒演示。",
            recommendedBlockMs = AnrDefaults.INPUT_BLOCK_MS,
            expectedReason = "Frequent GC followed by Input dispatching timed out or component timeout",
            explanation = "有限额保留内存块并制造主线程 GC 压力；OOM 通常是 crash，不等同于 ANR，因此危险 OOM 模式默认关闭。",
            defaultRequest = AnrTriggerRequest(
                scenarioId = "memory-pressure",
                blockMs = AnrDefaults.INPUT_BLOCK_MS,
                maxMb = AnrDefaults.DEFAULT_MEMORY_MAX_MB,
                chunkMb = AnrDefaults.DEFAULT_MEMORY_CHUNK_MB,
            ),
            adbCommand = deepLink(
                "memory-pressure",
                "maxMb=${AnrDefaults.DEFAULT_MEMORY_MAX_MB}&chunkMb=${AnrDefaults.DEFAULT_MEMORY_CHUNK_MB}&blockMs=${AnrDefaults.INPUT_BLOCK_MS}"
            )
        ),
        AnrScenario(
            id = "broadcast-foreground",
            title = "Broadcast foreground priority timeout",
            category = AnrCategory.ComponentLifecycle,
            riskLevel = AnrRiskLevel.Medium,
            triggerKind = AnrTriggerKind.Broadcast,
            timeoutDescription = "Android 13-：10 秒；Android 14+：10-20 秒。Demo 阻塞 12 秒。",
            recommendedBlockMs = AnrDefaults.BROADCAST_FOREGROUND_BLOCK_MS,
            expectedReason = "Broadcast of Intent ... FLAG_RECEIVER_FOREGROUND",
            explanation = "发送带 foreground priority flag 的显式广播，在 onReceive() 内阻塞。",
            defaultRequest = AnrTriggerRequest("broadcast-foreground", foreground = true, blockMs = AnrDefaults.BROADCAST_FOREGROUND_BLOCK_MS),
            adbCommand = "adb shell am broadcast --receiver-foreground -a ${AnrDefaults.BLOCKING_BROADCAST_ACTION} -n ${AnrDefaults.PACKAGE_NAME}/.anr.triggers.DemoBroadcastReceiver --ez foreground true --el blockMs ${AnrDefaults.BROADCAST_FOREGROUND_BLOCK_MS}"
        ),
        AnrScenario(
            id = "broadcast-background",
            title = "Broadcast background priority timeout",
            category = AnrCategory.LongRunningAdvanced,
            riskLevel = AnrRiskLevel.High,
            triggerKind = AnrTriggerKind.Broadcast,
            timeoutDescription = "Android 13-：60 秒；Android 14+：60-120 秒。Demo 阻塞 70 秒。",
            recommendedBlockMs = AnrDefaults.BROADCAST_BACKGROUND_BLOCK_MS,
            expectedReason = "Broadcast of Intent ... background priority",
            explanation = "不设置 foreground flag 的显式广播，耗时较长，默认放高级分组。",
            defaultRequest = AnrTriggerRequest("broadcast-background", foreground = false, blockMs = AnrDefaults.BROADCAST_BACKGROUND_BLOCK_MS),
            adbCommand = "adb shell am broadcast -a ${AnrDefaults.BLOCKING_BROADCAST_ACTION} -n ${AnrDefaults.PACKAGE_NAME}/.anr.triggers.DemoBroadcastReceiver --ez foreground false --el blockMs ${AnrDefaults.BROADCAST_BACKGROUND_BLOCK_MS}",
            enabledByDefault = false
        ),
        AnrScenario(
            id = "service-foreground",
            title = "Executing service foreground timeout",
            category = AnrCategory.ComponentLifecycle,
            riskLevel = AnrRiskLevel.Medium,
            triggerKind = AnrTriggerKind.Service,
            timeoutDescription = "前台 service 执行默认 20 秒；Demo 阻塞 25 秒。",
            recommendedBlockMs = AnrDefaults.SERVICE_FOREGROUND_BLOCK_MS,
            expectedReason = "executing service",
            explanation = "Service lifecycle 回调运行在主线程，onStartCommand() 阻塞会触发 executing service ANR。",
            defaultRequest = AnrTriggerRequest("service-foreground", mode = "foreground", blockMs = AnrDefaults.SERVICE_FOREGROUND_BLOCK_MS),
            adbCommand = deepLink("service-foreground", "mode=foreground&blockMs=${AnrDefaults.SERVICE_FOREGROUND_BLOCK_MS}")
        ),
        AnrScenario(
            id = "service-background",
            title = "Executing service background timeout",
            category = AnrCategory.LongRunningAdvanced,
            riskLevel = AnrRiskLevel.Dangerous,
            triggerKind = AnrTriggerKind.Service,
            timeoutDescription = "后台 service 执行默认 200 秒；Demo 阻塞 210 秒。",
            recommendedBlockMs = AnrDefaults.SERVICE_BACKGROUND_BLOCK_MS,
            expectedReason = "executing service",
            explanation = "长耗时高级场景，耗时超过 3 分钟，不默认启用。",
            defaultRequest = AnrTriggerRequest("service-background", mode = "background", blockMs = AnrDefaults.SERVICE_BACKGROUND_BLOCK_MS),
            adbCommand = deepLink("service-background", "mode=background&blockMs=${AnrDefaults.SERVICE_BACKGROUND_BLOCK_MS}"),
            enabledByDefault = false
        ),
        AnrScenario(
            id = "fgs-start-timeout",
            title = "startForegroundService 未及时 startForeground",
            category = AnrCategory.ComponentLifecycle,
            riskLevel = AnrRiskLevel.Medium,
            triggerKind = AnrTriggerKind.ForegroundServiceStart,
            timeoutDescription = "官方开发者文档口径：5 秒。Demo 默认跳过 startForeground。",
            recommendedBlockMs = 6_000L,
            minApi = Build.VERSION_CODES.O,
            expectedReason = "did not then call Service.startForeground",
            explanation = "启动前台服务后不及时调用 startForeground()，系统会停止服务并报告错误/ANR。",
            defaultRequest = AnrTriggerRequest("fgs-start-timeout", mode = "skipStartForeground", blockMs = 6_000L),
            adbCommand = deepLink("fgs-start-timeout", "mode=skipStartForeground")
        ),
        AnrScenario(
            id = "job-service",
            title = "JobScheduler callback timeout",
            category = AnrCategory.ComponentLifecycle,
            riskLevel = AnrRiskLevel.Medium,
            triggerKind = AnrTriggerKind.JobService,
            timeoutDescription = "onStartJob/onStopJob 几秒；AOSP 常见操作响应超时约 8 秒。Demo 阻塞 10 秒。",
            recommendedBlockMs = AnrDefaults.JOB_BLOCK_MS,
            expectedReason = "JobScheduler interaction timeout",
            explanation = "JobService 回调运行在主线程；Android 14+ 对此类问题显式上报 ANR。",
            defaultRequest = AnrTriggerRequest("job-service", mode = "onStartJob", blockMs = AnrDefaults.JOB_BLOCK_MS),
            adbCommand = deepLink("job-service", "mode=onStartJob&blockMs=${AnrDefaults.JOB_BLOCK_MS}")
        ),
        AnrScenario(
            id = "content-provider",
            title = "ContentProvider not responding",
            category = AnrCategory.ComponentLifecycle,
            riskLevel = AnrRiskLevel.Medium,
            triggerKind = AnrTriggerKind.ContentProvider,
            timeoutDescription = "官方未统一公开固定阈值；Demo 独立进程 provider 查询阻塞 8 秒。",
            recommendedBlockMs = AnrDefaults.PROVIDER_BLOCK_MS,
            expectedReason = "Content provider not responding or slow provider query",
            explanation = "远程 provider 查询、冷启动和 Binder 线程/主线程阻塞都可能计入。不同设备不一定弹系统 ANR。",
            defaultRequest = AnrTriggerRequest("content-provider", blockMs = AnrDefaults.PROVIDER_BLOCK_MS),
            adbCommand = deepLink("content-provider", "blockMs=${AnrDefaults.PROVIDER_BLOCK_MS}")
        ),
        AnrScenario(
            id = "short-service",
            title = "Foreground service shortService timeout",
            category = AnrCategory.LongRunningAdvanced,
            riskLevel = AnrRiskLevel.Dangerous,
            triggerKind = AnrTriggerKind.ShortForegroundService,
            timeoutDescription = "Android 14+ shortService 约 3 分钟；超时后不停止会 ANR。",
            recommendedBlockMs = AnrDefaults.SHORT_SERVICE_BLOCK_MS,
            minApi = Build.VERSION_CODES.UPSIDE_DOWN_CAKE,
            expectedReason = "FOREGROUND_SERVICE_TYPE_SHORT_SERVICE",
            explanation = "高级长耗时场景，运行超过约 3 分钟且不 stopSelf()。",
            defaultRequest = AnrTriggerRequest("short-service", blockMs = AnrDefaults.SHORT_SERVICE_BLOCK_MS),
            adbCommand = deepLink("short-service", "blockMs=${AnrDefaults.SHORT_SERVICE_BLOCK_MS}"),
            enabledByDefault = false
        ),
    )

    private val byId = scenarios.associateBy { it.id }

    fun scenario(id: String): AnrScenario? = byId[id]

    fun requireScenario(id: String): AnrScenario =
        scenario(id) ?: error("Unknown ANR scenario: $id")

    private fun deepLink(id: String, query: String? = null): String {
        val suffix = if (query.isNullOrBlank()) id else "$id?$query"
        return "adb shell am start -a android.intent.action.VIEW -d 'anrdemo://scenario/$suffix' ${AnrDefaults.PACKAGE_NAME}"
    }
}
