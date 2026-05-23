package com.codemx.anrdemo.anr.triggers

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.SystemClock
import android.util.Log
import com.codemx.anrdemo.anr.diagnostics.AnrLogTags

class ForegroundStartTimeoutService : Service() {
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val mode = intent?.getStringExtra(EXTRA_MODE) ?: MODE_SKIP
        val delayMs = intent?.getLongExtra(EXTRA_BLOCK_MS, 6_000L) ?: 6_000L
        Log.d(AnrLogTags.TRIGGER, "ForegroundStartTimeoutService mode=$mode delayMs=$delayMs")
        when (mode) {
            MODE_DELAY -> {
                SystemClock.sleep(delayMs)
                startForegroundCompat()
            }
            MODE_SKIP -> Unit
            else -> startForegroundCompat()
        }
        return START_NOT_STICKY
    }

    private fun startForegroundCompat() {
        val notification = NotificationHelper.build(this, "ANR Demo delayed foreground")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(1001, notification)
        } else {
            @Suppress("DEPRECATION")
            startForeground(1001, notification)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val EXTRA_MODE = "mode"
        const val EXTRA_BLOCK_MS = "blockMs"
        const val MODE_SKIP = "skipStartForeground"
        const val MODE_DELAY = "delayStartForeground"

        fun createIntent(context: Context, mode: String, blockMs: Long): Intent =
            Intent(context, ForegroundStartTimeoutService::class.java)
                .putExtra(EXTRA_MODE, mode)
                .putExtra(EXTRA_BLOCK_MS, blockMs)
    }
}
