package com.augmentalis.magicui.components.voicerouter

import com.augmentalis.magicui.components.argscanner.ARGRegistry
import com.augmentalis.magicui.components.argscanner.Capability

/**
 * Intent Router
 *
 * Routes voice command matches to Android Intents for app invocation.
 *
 * ## Usage
 * ```kotlin
 * val router = IntentRouter(registry)
 * val intent = router.createIntent(match)
 *
 * // Launch intent
 * context.startActivity(intent)
 * ```
 *
 * @since 1.0.0
 */
expect class IntentRouter(registry: ARGRegistry) {

    /**
     * Create Android Intent from voice command match
     *
     * @param match Voice command match result
     * @return Platform-specific Intent object
     */
    fun createIntent(match: VoiceCommandMatch): Any

    /**
     * Create Intent for a specific capability with parameters
     *
     * @param packageName Target app package name
     * @param capability Capability to invoke
     * @param parameters Parameter values
     * @return Platform-specific Intent object
     */
    fun createIntent(
        packageName: String,
        capability: Capability,
        parameters: Map<String, String>
    ): Any

    /**
     * Check if an intent can be handled by any installed app
     *
     * @param intent Intent to check
     * @return true if at least one app can handle it
     */
    fun canHandleIntent(intent: Any): Boolean

    /**
     * Get list of apps that can handle an intent
     *
     * @param intent Intent to query
     * @return List of package names
     */
    fun getHandlers(intent: Any): List<String>
}

/**
 * Intent creation result
 */
data class IntentResult(
    val intent: Any,
    val packageName: String,
    val action: String,
    val extras: Map<String, Any> = emptyMap()
)

/**
 * Intent router exception
 */
class IntentRouterException(message: String, cause: Throwable? = null) : Exception(message, cause)
