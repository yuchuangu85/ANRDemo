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
            adbCommand = deepLink("input-dispatch", "blockMs=${AnrDefaults.INPUT_BLOCK_MS}"),
            parameterSpecs = listOf(duration("阻塞主线程", AnrDefaults.INPUT_BLOCK_MS, 5_000, 20_000)),
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
            explanation = "通常由首帧过慢或窗口不可聚焦造成；当前作为诊断说明，不默认执行。",
            defaultRequest = AnrTriggerRequest("no-focused-window"),
            adbCommand = null,
            enabledByDefault = false,
            confirmationRequirement = ConfirmationRequirement.DisabledDocumentation,
            documentationOnlyReason = "no focused window 依赖窗口焦点/首帧状态，首版仅说明，不提供自动触发。",
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
            adbCommand = deepLink("deadlock", "mode=contention&blockMs=${AnrDefaults.INPUT_BLOCK_MS}"),
            parameterSpecs = listOf(
                ScenarioParameterSpec.Choice("mode", "死锁模式", "contention", listOf(Option("contention", "可恢复锁竞争"), Option("classic", "经典死锁"))),
                duration("锁等待", AnrDefaults.INPUT_BLOCK_MS, 5_000, 30_000),
            ),
        ),
        AnrScenario(
            id = "deadlock-classic",
            title = "Classic deadlock / 经典不可恢复死锁",
            category = AnrCategory.LongRunningAdvanced,
            riskLevel = AnrRiskLevel.Dangerous,
            triggerKind = AnrTriggerKind.Deadlock,
            timeoutDescription = "根因场景；按 Input dispatch 5 秒观察，通常需要 force-stop 恢复。",
            recommendedBlockMs = null,
            expectedReason = "Input dispatching timed out; main thread in classic deadlock",
            explanation = "主线程和后台线程互相等待两个锁，无法自动恢复。",
            defaultRequest = AnrTriggerRequest("deadlock-classic", mode = "classic"),
            adbCommand = deepLink("deadlock-classic", "mode=classic"),
            enabledByDefault = true,
            confirmationRequirement = ConfirmationRequirement.TypePhraseConfirm,
            confirmationPhrase = "DEADLOCK",
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
            defaultRequest = AnrTriggerRequest("memory-pressure", blockMs = AnrDefaults.INPUT_BLOCK_MS, maxMb = AnrDefaults.DEFAULT_MEMORY_MAX_MB, chunkMb = AnrDefaults.DEFAULT_MEMORY_CHUNK_MB),
            adbCommand = deepLink("memory-pressure", "maxMb=${AnrDefaults.DEFAULT_MEMORY_MAX_MB}&chunkMb=${AnrDefaults.DEFAULT_MEMORY_CHUNK_MB}&blockMs=${AnrDefaults.INPUT_BLOCK_MS}"),
            parameterSpecs = listOf(
                ScenarioParameterSpec.IntRange("maxMb", "保留内存上限 MB", AnrDefaults.DEFAULT_MEMORY_MAX_MB, 16, 256, 8),
                ScenarioParameterSpec.IntRange("chunkMb", "每块 MB", AnrDefaults.DEFAULT_MEMORY_CHUNK_MB, 1, 32, 1),
                duration("GC thrash 时长", AnrDefaults.INPUT_BLOCK_MS, 5_000, 20_000),
            ),
        ),
        AnrScenario(
            id = "memory-dangerous-oom",
            title = "危险 OOM / 内存无限压力",
            category = AnrCategory.LongRunningAdvanced,
            riskLevel = AnrRiskLevel.Dangerous,
            triggerKind = AnrTriggerKind.MemoryPressure,
            timeoutDescription = "OOM 通常是 crash，不是 ANR；仅用于对照演示。",
            recommendedBlockMs = AnrDefaults.INPUT_BLOCK_MS,
            expectedReason = "OutOfMemoryError crash or memory pressure before ANR",
            explanation = "危险模式允许更高内存分配，可能直接崩溃而非 ANR。",
            defaultRequest = AnrTriggerRequest("memory-dangerous-oom", blockMs = AnrDefaults.INPUT_BLOCK_MS, maxMb = 512, chunkMb = 16, allowDangerousOom = true),
            adbCommand = deepLink("memory-dangerous-oom", "maxMb=512&chunkMb=16&blockMs=${AnrDefaults.INPUT_BLOCK_MS}&allowDangerousOom=true"),
            confirmationRequirement = ConfirmationRequirement.TypePhraseConfirm,
            confirmationPhrase = "OOM",
            parameterSpecs = listOf(
                ScenarioParameterSpec.IntRange("maxMb", "保留内存上限 MB", 512, 64, 1024, 16),
                ScenarioParameterSpec.IntRange("chunkMb", "每块 MB", 16, 1, 64, 1),
                duration("GC thrash 时长", AnrDefaults.INPUT_BLOCK_MS, 5_000, 20_000),
                ScenarioParameterSpec.BooleanToggle("allowDangerousOom", "允许危险 OOM", true),
            ),
        ),
        broadcastScenario("broadcast-foreground", "Broadcast foreground priority timeout", AnrRiskLevel.Medium, true, AnrDefaults.BROADCAST_FOREGROUND_BLOCK_MS, ConfirmationRequirement.CountdownConfirm),
        broadcastScenario("broadcast-background", "Broadcast background priority timeout", AnrRiskLevel.High, false, AnrDefaults.BROADCAST_BACKGROUND_BLOCK_MS, ConfirmationRequirement.LongPressConfirm),
        AnrScenario(
            id = "broadcast-async-no-finish",
            title = "Broadcast goAsync 不 finish timeout",
            category = AnrCategory.ComponentLifecycle,
            riskLevel = AnrRiskLevel.High,
            triggerKind = AnrTriggerKind.Broadcast,
            timeoutDescription = "按 foreground/background broadcast 阈值触发；默认 foreground 12 秒。",
            recommendedBlockMs = AnrDefaults.BROADCAST_FOREGROUND_BLOCK_MS,
            expectedReason = "Broadcast of Intent ... goAsync pending result not finished",
            explanation = "BroadcastReceiver 调用 goAsync() 后故意不 finish()，演示异步 receiver 超时。",
            defaultRequest = AnrTriggerRequest("broadcast-async-no-finish", foreground = true, mode = "goAsyncNoFinish", blockMs = AnrDefaults.BROADCAST_FOREGROUND_BLOCK_MS),
            adbCommand = deepLink("broadcast-async-no-finish", "foreground=true&mode=goAsyncNoFinish&blockMs=${AnrDefaults.BROADCAST_FOREGROUND_BLOCK_MS}"),
            parameterSpecs = broadcastParams(true, AnrDefaults.BROADCAST_FOREGROUND_BLOCK_MS, "goAsyncNoFinish"),
        ),
        serviceScenario("service-foreground", "Executing service foreground timeout", AnrRiskLevel.Medium, "foreground", AnrDefaults.SERVICE_FOREGROUND_BLOCK_MS, ConfirmationRequirement.CountdownConfirm),
        serviceScenario("service-background", "Executing service long timeout / background-sensitive", AnrRiskLevel.Dangerous, "background", AnrDefaults.SERVICE_BACKGROUND_BLOCK_MS, ConfirmationRequirement.TypePhraseConfirm, "SERVICE200"),
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
            adbCommand = deepLink("fgs-start-timeout", "mode=skipStartForeground"),
            parameterSpecs = listOf(
                ScenarioParameterSpec.Choice("mode", "模式", "skipStartForeground", listOf(Option("skipStartForeground", "跳过 startForeground"), Option("delayStartForeground", "延迟 startForeground"))),
                duration("延迟", 6_000L, 5_000, 15_000),
            ),
        ),
        jobScenario("job-service", "JobScheduler onStartJob timeout", "onStartJob"),
        jobScenario("job-service-stop", "JobScheduler onStopJob timeout", "onStopJob"),
        AnrScenario(
            id = "content-provider",
            title = "Slow ContentProvider query / caller Binder wait",
            category = AnrCategory.ComponentLifecycle,
            riskLevel = AnrRiskLevel.Medium,
            triggerKind = AnrTriggerKind.ContentProvider,
            timeoutDescription = "官方未统一公开固定阈值；Demo 独立进程 provider 查询阻塞 8 秒。",
            recommendedBlockMs = AnrDefaults.PROVIDER_BLOCK_MS,
            expectedReason = "Slow provider query; system ContentProvider ANR only if ActivityManager/exit-info reports it",
            explanation = "远程 provider 查询会让调用方主线程等待 Binder 返回；Demo 记录慢查询耗时，但不再把本地 5 秒阈值当作系统 provider ANR 证据。",
            defaultRequest = AnrTriggerRequest("content-provider", blockMs = AnrDefaults.PROVIDER_BLOCK_MS),
            adbCommand = deepLink("content-provider", "blockMs=${AnrDefaults.PROVIDER_BLOCK_MS}"),
            parameterSpecs = listOf(duration("Provider query 阻塞", AnrDefaults.PROVIDER_BLOCK_MS, 1_000, 30_000)),
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
            confirmationRequirement = ConfirmationRequirement.TypePhraseConfirm,
            confirmationPhrase = "SHORT_SERVICE",
            parameterSpecs = listOf(duration("运行时长", AnrDefaults.SHORT_SERVICE_BLOCK_MS, 180_000, 240_000)),
        ),
    )

    private val byId = scenarios.associateBy { it.id }

    fun scenario(id: String): AnrScenario? = byId[id]

    fun requireScenario(id: String): AnrScenario = scenario(id) ?: error("Unknown ANR scenario: $id")

    private fun duration(label: String, default: Long, min: Long, max: Long) =
        ScenarioParameterSpec.DurationMs(label = label, defaultValue = default, min = min, max = max)

    private fun broadcastParams(foreground: Boolean, blockMs: Long, mode: String = "sync") = listOf(
        ScenarioParameterSpec.BooleanToggle("foreground", "Foreground priority", foreground),
        ScenarioParameterSpec.Choice("mode", "模式", mode, listOf(Option("sync", "同步阻塞"), Option("goAsyncNoFinish", "goAsync 不 finish"))),
        duration("Receiver 阻塞", blockMs, 5_000, 120_000),
    )

    private fun broadcastScenario(id: String, title: String, risk: AnrRiskLevel, foreground: Boolean, blockMs: Long, confirmation: ConfirmationRequirement) =
        AnrScenario(
            id = id,
            title = title,
            category = if (foreground) AnrCategory.ComponentLifecycle else AnrCategory.LongRunningAdvanced,
            riskLevel = risk,
            triggerKind = AnrTriggerKind.Broadcast,
            timeoutDescription = if (foreground) "Android 13-：10 秒；Android 14+：10-20 秒。Demo 阻塞 12 秒。" else "Android 13-：60 秒；Android 14+：60-120 秒。Demo 阻塞 70 秒。",
            recommendedBlockMs = blockMs,
            expectedReason = "Broadcast of Intent ...",
            explanation = if (foreground) "发送带 foreground priority flag 的显式广播，在 onReceive() 内阻塞。" else "不设置 foreground flag 的显式广播，耗时较长。",
            defaultRequest = AnrTriggerRequest(id, foreground = foreground, blockMs = blockMs, mode = "sync"),
            adbCommand = deepLink(id, "foreground=$foreground&blockMs=$blockMs"),
            confirmationRequirement = confirmation,
            parameterSpecs = broadcastParams(foreground, blockMs),
        )

    private fun serviceScenario(id: String, title: String, risk: AnrRiskLevel, mode: String, blockMs: Long, confirmation: ConfirmationRequirement, phrase: String? = null) =
        AnrScenario(
            id = id,
            title = title,
            category = if (mode == "background") AnrCategory.LongRunningAdvanced else AnrCategory.ComponentLifecycle,
            riskLevel = risk,
            triggerKind = AnrTriggerKind.Service,
            timeoutDescription = if (mode == "background") "后台 service 执行经典阈值约 200 秒；现代 Android 后台启动受限，Demo 长阻塞 210 秒并记录进程 importance，最终分类以系统日志为准。" else "前台 service 执行默认 20 秒；Demo 阻塞 25 秒。",
            recommendedBlockMs = blockMs,
            expectedReason = "executing service",
            explanation = if (mode == "background") "Service lifecycle 回调运行在主线程；该场景用于长耗时/后台敏感对照，不再宣称所有设备必定产生后台 service 分类。" else "Service lifecycle 回调运行在主线程，onStartCommand() 阻塞会触发 executing service ANR。",
            defaultRequest = AnrTriggerRequest(id, mode = mode, blockMs = blockMs),
            adbCommand = deepLink(id, "mode=$mode&blockMs=$blockMs"),
            confirmationRequirement = confirmation,
            confirmationPhrase = phrase,
            parameterSpecs = listOf(ScenarioParameterSpec.Choice("mode", "模式", mode, listOf(Option("foreground", "前台进程 service"), Option("background", "后台 service"))), duration("Service 阻塞", blockMs, 5_000, 240_000)),
        )

    private fun jobScenario(id: String, title: String, mode: String) =
        AnrScenario(
            id = id,
            title = title,
            category = AnrCategory.ComponentLifecycle,
            riskLevel = AnrRiskLevel.Medium,
            triggerKind = AnrTriggerKind.JobService,
            timeoutDescription = "onStartJob/onStopJob 几秒；AOSP 常见操作响应超时约 8 秒。Demo 阻塞 10 秒。",
            recommendedBlockMs = AnrDefaults.JOB_BLOCK_MS,
            expectedReason = "JobScheduler interaction timeout",
            explanation = "JobService 回调运行在主线程；Android 14+ 对此类问题显式上报 ANR。",
            defaultRequest = AnrTriggerRequest(id, mode = mode, blockMs = AnrDefaults.JOB_BLOCK_MS),
            adbCommand = deepLink(id, "mode=$mode&blockMs=${AnrDefaults.JOB_BLOCK_MS}"),
            parameterSpecs = listOf(ScenarioParameterSpec.Choice("mode", "回调", mode, listOf(Option("onStartJob", "onStartJob"), Option("onStopJob", "onStopJob"))), duration("Job 回调阻塞", AnrDefaults.JOB_BLOCK_MS, 5_000, 20_000)),
        )

    private fun deepLink(id: String, query: String? = null): String {
        val confirmedQuery = listOfNotNull(query?.takeIf { it.isNotBlank() }, "adbConfirmed=true").joinToString("&")
        val suffix = "$id?$confirmedQuery"
        return "adb shell am start -a android.intent.action.VIEW -d 'anrdemo://scenario/$suffix' ${AnrDefaults.PACKAGE_NAME}"
    }
}
