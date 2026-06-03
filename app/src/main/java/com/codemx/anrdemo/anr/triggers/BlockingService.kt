package com.codemx.anrdemo.anr.triggers

import android.app.Service
import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.os.SystemClock
import android.util.Log
import com.codemx.anrdemo.anr.catalog.AnrDefaults
import com.codemx.anrdemo.anr.diagnostics.AnrLogTags

class BlockingService : Service() {
    override fun onCreate() {
        super.onCreate()
        Log.d(AnrLogTags.TRIGGER, "BlockingService onCreate")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val blockMs = intent?.getLongExtra(EXTRA_BLOCK_MS, AnrDefaults.SERVICE_FOREGROUND_BLOCK_MS)
            ?: AnrDefaults.SERVICE_FOREGROUND_BLOCK_MS
        val mode = intent?.getStringExtra(EXTRA_MODE).orEmpty()
        val processInfo = ActivityManager.RunningAppProcessInfo()
        ActivityManager.getMyMemoryState(processInfo)
        val importance = processInfo.importance
        Log.d(AnrLogTags.TRIGGER, "BlockingService onStartCommand mode=$mode blockMs=$blockMs importance=$importance thread=${Thread.currentThread().name}")
        SystemClock.sleep(blockMs)
        stopSelf(startId)
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val EXTRA_BLOCK_MS = "blockMs"
        const val EXTRA_MODE = "mode"

        fun createIntent(context: Context, blockMs: Long, mode: String? = null): Intent =
            Intent(context, BlockingService::class.java)
                .putExtra(EXTRA_BLOCK_MS, blockMs)
                .apply { if (mode != null) putExtra(EXTRA_MODE, mode) }
    }
}
