package com.codemx.anrdemo.anr.triggers

import android.content.ComponentName
import android.content.Context
import android.content.ServiceConnection
import android.os.IBinder
import android.os.Parcel
import android.os.SystemClock
import android.util.Log
import com.codemx.anrdemo.anr.diagnostics.AnrLogTags
import com.codemx.anrdemo.anr.dispatch.TriggerResult

object BinderPeerTriggers {
    private var activeConnection: ServiceConnection? = null

    fun trigger(context: Context, blockMs: Long): TriggerResult {
        val appContext = context.applicationContext
        val connection = object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName, service: IBinder) {
                Log.d(
                    AnrLogTags.TRIGGER,
                    "Binder peer connected name=$name; issuing synchronous transact on thread=${Thread.currentThread().name} blockMs=$blockMs",
                )
                callRemotePeerOnMainThread(service, blockMs)
                unbind(appContext, this)
                if (activeConnection === this) activeConnection = null
            }

            override fun onServiceDisconnected(name: ComponentName) {
                Log.d(AnrLogTags.TRIGGER, "Binder peer disconnected name=$name")
                if (activeConnection === this) activeConnection = null
            }
        }

        activeConnection?.let { unbind(appContext, it) }
        activeConnection = connection

        val bound = appContext.bindService(
            BinderPeerService.createIntent(appContext, blockMs),
            connection,
            Context.BIND_AUTO_CREATE,
        )
        return if (bound) {
            TriggerResult.Started("已绑定独立进程 BinderPeerService；连接成功后主线程会同步等待远端 ${blockMs}ms")
        } else {
            activeConnection = null
            TriggerResult.Rejected("无法绑定 BinderPeerService")
        }
    }

    private fun callRemotePeerOnMainThread(service: IBinder, blockMs: Long) {
        val data = Parcel.obtain()
        val reply = Parcel.obtain()
        val started = SystemClock.elapsedRealtime()
        try {
            data.writeLong(blockMs)
            val transacted = service.transact(BinderPeerService.TRANSACTION_BLOCK, data, reply, 0)
            val elapsed = SystemClock.elapsedRealtime() - started
            if (transacted) {
                reply.readException()
                val remoteElapsed = if (reply.dataAvail() > 0) reply.readLong() else -1L
                Log.d(
                    AnrLogTags.TRIGGER,
                    "Synchronous Binder peer transact returned elapsedMs=$elapsed remoteElapsedMs=$remoteElapsed thread=${Thread.currentThread().name}",
                )
            } else {
                Log.w(AnrLogTags.TRIGGER, "Binder peer transact returned false elapsedMs=$elapsed")
            }
        } catch (throwable: Throwable) {
            val elapsed = SystemClock.elapsedRealtime() - started
            Log.e(AnrLogTags.TRIGGER, "Binder peer transact failed elapsedMs=$elapsed", throwable)
        } finally {
            data.recycle()
            reply.recycle()
        }
    }

    private fun unbind(context: Context, connection: ServiceConnection) {
        runCatching { context.unbindService(connection) }
            .onFailure { Log.w(AnrLogTags.TRIGGER, "Binder peer unbind ignored: ${it.message}") }
    }
}
