package com.augmentalis.httpavanue.voice

import com.augmentalis.httpavanue.http.HttpRequest
import com.augmentalis.httpavanue.http.HttpResponse
import com.augmentalis.httpavanue.routing.RouteHandler
import com.augmentalis.httpavanue.routing.RouterImpl

/**
 * Voice Route annotations — enables HTTP endpoints to be discoverable
 * and invocable by voice commands in the VoiceOS ecosystem.
 *
 * Branch B (Avanues internal): This package uses ONLY HTTPAvanue types.
 * Zero imports from VoiceOSCore, AVACode, or any external module.
 *
 * The VOS export format (pipe-delimited v3.0) is generated as plain text
 * so VoiceOS can consume it without HTTPAvanue depending on VoiceOS.
 */
data class VoiceRouteConfig(
    val phrase: String,
    val aliases: List<String> = emptyList(),
    val category: String = "API",
    val description: String = "",
)

/** Register a GET route with voice metadata. */
fun RouterImpl.getVoiced(
    pattern: String,
    voice: VoiceRouteConfig,
    handler: RouteHandler,
) {
    get(pattern, handler = handler)
    VoiceRouteRegistry.register(pattern, "GET", voice)
}

/** Register a POST route with voice metadata. */
fun RouterImpl.postVoiced(
    pattern: String,
    voice: VoiceRouteConfig,
    handler: RouteHandler,
) {
    post(pattern, handler = handler)
    VoiceRouteRegistry.register(pattern, "POST", voice)
}

/** Register a PUT route with voice metadata. */
fun RouterImpl.putVoiced(
    pattern: String,
    voice: VoiceRouteConfig,
    handler: RouteHandler,
) {
    put(pattern, handler = handler)
    VoiceRouteRegistry.register(pattern, "PUT", voice)
}

/** Register a DELETE route with voice metadata. */
fun RouterImpl.deleteVoiced(
    pattern: String,
    voice: VoiceRouteConfig,
    handler: RouteHandler,
) {
    delete(pattern, handler = handler)
    VoiceRouteRegistry.register(pattern, "DELETE", voice)
}

/**
 * Internal registry tracking voice-annotated routes.
 * Separate from RouteRegistry to maintain zero coupling.
 */
object VoiceRouteRegistry {
    private val entries = mutableListOf<VoiceRouteEntry>()

    internal fun register(pattern: String, method: String, config: VoiceRouteConfig) {
        entries.add(VoiceRouteEntry(pattern, method, config))
    }

    fun getAll(): List<VoiceRouteEntry> = entries.toList()

    fun clear() = entries.clear()
}

data class VoiceRouteEntry(
    val pattern: String,
    val method: String,
    val config: VoiceRouteConfig,
)

/**
 * Export voice routes to VOS compact format (pipe-delimited v3.0).
 * Output can be saved as a .vos file for VoiceOS to consume.
 *
 * Format per line: `category|phrase|action_type|handler_info|locale`
 */
object VoiceRouteExporter {
    fun toVosString(
        entries: List<VoiceRouteEntry> = VoiceRouteRegistry.getAll(),
        locale: String = "en-US",
    ): String = buildString {
        appendLine("# VOS Compact v3.0 — HTTPAvanue Voice Routes")
        appendLine("# Auto-generated from voice-annotated endpoints")
        appendLine()
        for (entry in entries) {
            val category = entry.config.category.uppercase()
            val phrase = entry.config.phrase
            val actionType = "HTTP_${entry.method}"
            val handler = "${entry.method} ${entry.pattern}"
            appendLine("$category|$phrase|$actionType|$handler|$locale")

            // Also emit aliases
            for (alias in entry.config.aliases) {
                appendLine("$category|$alias|$actionType|$handler|$locale")
            }
        }
    }
}
