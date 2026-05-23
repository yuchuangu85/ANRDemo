package com.codemx.anrdemo.anr.safety

import android.os.Build

object DeviceCapabilities {
    fun isAtLeast(api: Int): Boolean = Build.VERSION.SDK_INT >= api
}
