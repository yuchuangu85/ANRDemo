package com.codemx.anrdemo.anr.diagnostics

object ThreadStateFormatter {
    fun currentThreadLabel(): String = Thread.currentThread().let { "${it.name}/${it.state}" }
}
