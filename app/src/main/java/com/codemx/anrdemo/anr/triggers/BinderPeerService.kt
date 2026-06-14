package com.codemx.anrdemo.anr.triggers

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.os.Parcel
import android.os.SystemClock
import android.util.Log
import com.codemx.anrdemo.anr.catalog.AnrDefaults
import com.codemx.anrdemo.anr.diagnostics.AnrLogTags

class BinderPeerService : Service() {
    private val slowBinder = object : Binder() {
        override fun onTransact(code: Int, data: Parcel, reply: Parcel?, flags: Int): Boolean {
            if (code != TRANSACTION_BLOCK) {
                return super.onTransact(code, data, reply, flags)
            }

            val blockMs = data.readLong().coerceAtLeast(0L)
            val started = SystemClock.elapsedRealtime()
            Log.d(
                AnrLogTags.TRIGGER,
                "BinderPeerService remote onTransact blockMs=$blockMs thread=${Thread.currentThread().name}",
            )
            SystemClock.sleep(blockMs)
            val elapsed = SystemClock.elapsedRealtime() - started
            Log.d(AnrLogTags.TRIGGER, "BinderPeerService remote onTransact finished elapsedMs=$elapsed")

            reply?.writeNoException()
            reply?.writeLong(elapsed)
            return true
        }
    }

    override fun onBind(intent: Intent?): IBinder {
        Log.d(AnrLogTags.TRIGGER, "BinderPeerService onBind process=:binderPeer")
        return slowBinder
    }

    companion object {
        const val TRANSACTION_BLOCK = IBinder.FIRST_CALL_TRANSACTION
        private const val EXTRA_BLOCK_MS = "blockMs"

        fun createIntent(context: Context, blockMs: Long): Intent =
            Intent(context, BinderPeerService::class.java)
                .putExtra(EXTRA_BLOCK_MS, blockMs)
    }
}
