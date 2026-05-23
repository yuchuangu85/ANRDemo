package com.codemx.anrdemo.anr.triggers

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.os.SystemClock
import android.util.Log
import com.codemx.anrdemo.anr.catalog.AnrDefaults
import com.codemx.anrdemo.anr.diagnostics.AnrLogTags

class ShortForegroundService : Service() {
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val blockMs = intent?.getLongExtra(EXTRA_BLOCK_MS, AnrDefaults.SHORT_SERVICE_BLOCK_MS)
            ?: AnrDefaults.SHORT_SERVICE_BLOCK_MS
        Log.d(AnrLogTags.TRIGGER, "ShortForegroundService startForeground then blockMs=$blockMs")
        startForeground(1002, NotificationHelper.build(this, "ANR Demo shortService"))
        SystemClock.sleep(blockMs)
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val EXTRA_BLOCK_MS = "blockMs"
        fun createIntent(context: Context, blockMs: Long): Intent =
            Intent(context, ShortForegroundService::class.java).putExtra(EXTRA_BLOCK_MS, blockMs)
    }
}
