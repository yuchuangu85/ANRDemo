package com.codemx.anrdemo.anr.triggers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import android.util.Log
import com.codemx.anrdemo.anr.catalog.AnrDefaults
import com.codemx.anrdemo.anr.diagnostics.AnrLogTags

class DemoBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val blockMs = intent.getLongExtra(EXTRA_BLOCK_MS, AnrDefaults.BROADCAST_FOREGROUND_BLOCK_MS)
        val mode = intent.getStringExtra(EXTRA_MODE).orEmpty()
        Log.d(AnrLogTags.TRIGGER, "Broadcast receiver mode=$mode blockMs=$blockMs thread=${Thread.currentThread().name}")
        if (mode == MODE_GO_ASYNC_NO_FINISH) {
            val pending = goAsync()
            Thread({
                Log.d(AnrLogTags.TRIGGER, "goAsync worker intentionally not finishing pendingResult=$pending")
                SystemClock.sleep(blockMs)
            }, "ANRDemo-broadcast-goAsync").start()
            return
        }
        SystemClock.sleep(blockMs)
    }

    companion object {
        const val EXTRA_BLOCK_MS = "blockMs"
        const val EXTRA_MODE = "mode"
        const val EXTRA_FOREGROUND = "foreground"
        const val MODE_GO_ASYNC_NO_FINISH = "goAsyncNoFinish"

        fun createIntent(context: Context, blockMs: Long, foreground: Boolean, mode: String? = null): Intent =
            Intent(AnrDefaults.BLOCKING_BROADCAST_ACTION)
                .setClass(context, DemoBroadcastReceiver::class.java)
                .putExtra(EXTRA_BLOCK_MS, blockMs)
                .putExtra(EXTRA_FOREGROUND, foreground)
                .apply {
                    if (foreground) addFlags(Intent.FLAG_RECEIVER_FOREGROUND)
                    if (mode != null) putExtra(EXTRA_MODE, mode)
                }
    }
}
