package com.codemx.anrdemo.anr.catalog

object AnrDefaults {
    const val PACKAGE_NAME = "com.codemx.anrdemo"
    const val BLOCKING_BROADCAST_ACTION = "com.codemx.anrdemo.ACTION_BLOCKING_BROADCAST"
    const val BLOCKING_PROVIDER_AUTHORITY = "com.codemx.anrdemo.blocking-provider"
    const val INPUT_BLOCK_MS = 8_000L
    const val BROADCAST_FOREGROUND_BLOCK_MS = 12_000L
    const val BROADCAST_BACKGROUND_BLOCK_MS = 70_000L
    const val SERVICE_FOREGROUND_BLOCK_MS = 25_000L
    const val SERVICE_BACKGROUND_BLOCK_MS = 210_000L
    const val JOB_BLOCK_MS = 10_000L
    const val PROVIDER_BLOCK_MS = 8_000L
    const val BINDER_PEER_BLOCK_MS = 8_000L
    const val SHORT_SERVICE_BLOCK_MS = 190_000L
    const val DEFAULT_MEMORY_MAX_MB = 128
    const val DEFAULT_MEMORY_CHUNK_MB = 8
}
