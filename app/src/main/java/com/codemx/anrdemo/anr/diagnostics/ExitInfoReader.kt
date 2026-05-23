package com.codemx.anrdemo.anr.diagnostics

import android.app.ActivityManager
import android.app.ApplicationExitInfo
import android.content.Context
import android.os.Build

object ExitInfoReader {
    fun latestAnrSummary(context: Context): String {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return "ApplicationExitInfo 需要 Android 11+"
        val am = context.getSystemService(ActivityManager::class.java)
        val info = am.getHistoricalProcessExitReasons(context.packageName, 0, 5)
            .firstOrNull { it.reason == ApplicationExitInfo.REASON_ANR }
            ?: return "暂无最近 ANR exit info"
        return "最近 ANR: pid=${info.pid}, importance=${info.importance}, time=${info.timestamp}, desc=${info.description.orEmpty()}"
    }
}
