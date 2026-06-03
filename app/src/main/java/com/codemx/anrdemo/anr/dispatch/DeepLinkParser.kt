package com.codemx.anrdemo.anr.dispatch

import android.net.Uri
import java.net.URI
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

object DeepLinkParser {
    fun parse(uri: Uri?): AnrTriggerRequest? = parse(uri?.toString())

    fun parse(rawUri: String?): AnrTriggerRequest? {
        if (rawUri.isNullOrBlank()) return null
        val uri = runCatching { URI(rawUri) }.getOrNull() ?: return null
        if (uri.scheme != "anrdemo" || uri.host != "scenario") return null
        val scenarioId = uri.path.orEmpty().trim('/').substringBefore('/').takeIf { it.isNotBlank() } ?: return null
        val params = parseQuery(uri.rawQuery.orEmpty())
        return AnrTriggerRequest(
            scenarioId = scenarioId,
            blockMs = params["blockMs"]?.toLongOrNull(),
            mode = params["mode"] ?: defaultModeFor(scenarioId),
            maxMb = params["maxMb"]?.toIntOrNull(),
            chunkMb = params["chunkMb"]?.toIntOrNull(),
            foreground = params["foreground"]?.toBooleanStrictOrNull(),
            allowDangerousOom = params["allowDangerousOom"]?.toBooleanStrictOrNull() ?: (scenarioId == "memory-dangerous-oom"),
            adbConfirmed = params["adbConfirmed"]?.toBooleanStrictOrNull() ?: false,
        )
    }

    private fun defaultModeFor(id: String): String? = when (id) {
        "deadlock-classic" -> "classic"
        "job-service-stop" -> "onStopJob"
        else -> null
    }

    private fun parseQuery(rawQuery: String): Map<String, String> {
        if (rawQuery.isBlank()) return emptyMap()
        return rawQuery.split('&')
            .mapNotNull { part ->
                val key = part.substringBefore('=', missingDelimiterValue = "")
                if (key.isBlank()) return@mapNotNull null
                val value = part.substringAfter('=', missingDelimiterValue = "")
                decode(key) to decode(value)
            }
            .toMap()
    }

    private fun decode(value: String): String =
        URLDecoder.decode(value, StandardCharsets.UTF_8.name())
}
