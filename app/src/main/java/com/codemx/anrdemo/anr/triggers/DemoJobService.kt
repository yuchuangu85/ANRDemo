package com.codemx.anrdemo.anr.triggers

import android.app.job.JobParameters
import android.app.job.JobService
import android.os.SystemClock
import android.util.Log
import com.codemx.anrdemo.anr.catalog.AnrDefaults
import com.codemx.anrdemo.anr.diagnostics.AnrLogTags

class DemoJobService : JobService() {
    override fun onStartJob(params: JobParameters): Boolean {
        val blockMs = params.extras.getLong(EXTRA_BLOCK_MS, AnrDefaults.JOB_BLOCK_MS)
        Log.d(AnrLogTags.TRIGGER, "DemoJobService onStartJob blockMs=$blockMs thread=${Thread.currentThread().name}")
        SystemClock.sleep(blockMs)
        jobFinished(params, false)
        return false
    }

    override fun onStopJob(params: JobParameters): Boolean {
        val blockMs = params.extras.getLong(EXTRA_BLOCK_MS, AnrDefaults.JOB_BLOCK_MS)
        val mode = params.extras.getString(EXTRA_MODE, MODE_START)
        if (mode == MODE_STOP) {
            Log.d(AnrLogTags.TRIGGER, "DemoJobService onStopJob blockMs=$blockMs")
            SystemClock.sleep(blockMs)
        }
        return false
    }

    companion object {
        const val JOB_ID = 4201
        const val EXTRA_BLOCK_MS = "blockMs"
        const val EXTRA_MODE = "mode"
        const val MODE_START = "onStartJob"
        const val MODE_STOP = "onStopJob"
    }
}
