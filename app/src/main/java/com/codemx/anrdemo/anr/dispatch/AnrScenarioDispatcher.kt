package com.codemx.anrdemo.anr.dispatch

import android.app.job.JobInfo
import android.app.job.JobScheduler
import android.content.ComponentName
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.PersistableBundle
import android.util.Log
import com.codemx.anrdemo.anr.catalog.AnrCatalog
import com.codemx.anrdemo.anr.catalog.AnrDefaults
import com.codemx.anrdemo.anr.catalog.AnrTriggerKind
import com.codemx.anrdemo.anr.diagnostics.AnrLogTags
import com.codemx.anrdemo.anr.safety.SafetyGate
import com.codemx.anrdemo.anr.triggers.BlockingService
import com.codemx.anrdemo.anr.triggers.DeadlockTriggers
import com.codemx.anrdemo.anr.triggers.DemoBroadcastReceiver
import com.codemx.anrdemo.anr.triggers.DemoJobService
import com.codemx.anrdemo.anr.triggers.ForegroundStartTimeoutService
import com.codemx.anrdemo.anr.triggers.MainThreadTriggers
import com.codemx.anrdemo.anr.triggers.MemoryPressureTriggers
import com.codemx.anrdemo.anr.triggers.ShortForegroundService

class AnrScenarioDispatcher(
    private val context: Context,
    private val safetyGate: SafetyGate = SafetyGate(),
) {
    fun dispatch(request: AnrTriggerRequest): TriggerResult {
        val scenario = AnrCatalog.requireScenario(request.scenarioId)
        safetyGate.check(scenario, request)?.let { return it }
        Log.d(AnrLogTags.TRIGGER, "Dispatch scenario=${scenario.id} kind=${scenario.triggerKind} request=$request")

        return when (scenario.triggerKind) {
            AnrTriggerKind.MainThreadBlock -> MainThreadTriggers.blockMainThread(request.blockMs ?: scenario.recommendedBlockMs ?: AnrDefaults.INPUT_BLOCK_MS)
            AnrTriggerKind.Deadlock -> DeadlockTriggers.trigger(request)
            AnrTriggerKind.MemoryPressure -> MemoryPressureTriggers.trigger(request)
            AnrTriggerKind.Broadcast -> sendBroadcast(request, scenario.id)
            AnrTriggerKind.Service -> startBlockingService(request, scenario.id)
            AnrTriggerKind.ForegroundServiceStart -> startForegroundTimeoutService(request)
            AnrTriggerKind.JobService -> scheduleJob(request)
            AnrTriggerKind.ContentProvider -> queryProvider(request)
            AnrTriggerKind.ShortForegroundService -> startShortForegroundService(request)
            AnrTriggerKind.AdbOnly -> TriggerResult.NotRunnableFromUi
        }
    }

    private fun sendBroadcast(request: AnrTriggerRequest, scenarioId: String): TriggerResult {
        val foreground = request.foreground ?: (scenarioId == "broadcast-foreground")
        val blockMs = request.blockMs ?: if (foreground) AnrDefaults.BROADCAST_FOREGROUND_BLOCK_MS else AnrDefaults.BROADCAST_BACKGROUND_BLOCK_MS
        context.sendBroadcast(DemoBroadcastReceiver.createIntent(context, blockMs, foreground, request.mode))
        return TriggerResult.Started("已发送 Broadcast，blockMs=$blockMs foreground=$foreground")
    }

    private fun startBlockingService(request: AnrTriggerRequest, scenarioId: String): TriggerResult {
        val blockMs = request.blockMs ?: if (scenarioId == "service-background") AnrDefaults.SERVICE_BACKGROUND_BLOCK_MS else AnrDefaults.SERVICE_FOREGROUND_BLOCK_MS
        context.startService(BlockingService.createIntent(context, blockMs, request.mode))
        return TriggerResult.Started("已启动 BlockingService，blockMs=$blockMs")
    }

    private fun startForegroundTimeoutService(request: AnrTriggerRequest): TriggerResult {
        val mode = request.mode ?: ForegroundStartTimeoutService.MODE_SKIP
        val blockMs = request.blockMs ?: 6_000L
        val intent = ForegroundStartTimeoutService.createIntent(context, mode, blockMs)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) context.startForegroundService(intent) else context.startService(intent)
        return TriggerResult.Started("已启动 ForegroundStartTimeoutService，mode=$mode")
    }

    private fun scheduleJob(request: AnrTriggerRequest): TriggerResult {
        val scheduler = context.getSystemService(JobScheduler::class.java)
        val extras = PersistableBundle().apply {
            putLong(DemoJobService.EXTRA_BLOCK_MS, request.blockMs ?: AnrDefaults.JOB_BLOCK_MS)
            putString(DemoJobService.EXTRA_MODE, request.mode ?: DemoJobService.MODE_START)
        }
        val job = JobInfo.Builder(
            DemoJobService.JOB_ID,
            ComponentName(context, DemoJobService::class.java),
        )
            .setOverrideDeadline(0L)
            .setExtras(extras)
            .build()
        scheduler.schedule(job)
        return TriggerResult.Started("已调度 DemoJobService")
    }

    private fun queryProvider(request: AnrTriggerRequest): TriggerResult {
        val blockMs = request.blockMs ?: AnrDefaults.PROVIDER_BLOCK_MS
        val uri = Uri.parse("content://${AnrDefaults.BLOCKING_PROVIDER_AUTHORITY}/items?blockMs=$blockMs")
        val started = android.os.SystemClock.elapsedRealtime()
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            while (cursor.moveToNext()) Unit
        }
        return TriggerResult.Completed(android.os.SystemClock.elapsedRealtime() - started)
    }

    private fun startShortForegroundService(request: AnrTriggerRequest): TriggerResult {
        val blockMs = request.blockMs ?: AnrDefaults.SHORT_SERVICE_BLOCK_MS
        val intent = ShortForegroundService.createIntent(context, blockMs)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) context.startForegroundService(intent) else context.startService(intent)
        return TriggerResult.Started("已启动 shortService，blockMs=$blockMs")
    }
}
