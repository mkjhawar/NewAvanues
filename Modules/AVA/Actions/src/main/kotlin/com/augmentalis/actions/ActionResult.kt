package com.augmentalis.actions

/**
 * Result of an action execution.
 *
 * Represents the outcome of executing an intent action, including success/failure
 * status, user-visible message, and optional data payload.
 *
 * Design:
 * - Sealed class for exhaustive when() handling
 * - Success contains optional message and data
 * - Failure contains error message and exception
 * - NeedsResolution signals that app resolution is needed (Chapter 71)
 *
 * @see IntentActionHandler
 */
sealed class ActionResult {

    /**
     * Action executed successfully.
     *
     * @param message Optional user-visible message (e.g., "Clock app opened")
     * @param data Optional result data (e.g., Intent extras, API response)
     */
    data class Success(
        val message: String? = null,
        val data: Map<String, Any>? = null
    ) : ActionResult()

    /**
     * Action execution failed.
     *
     * @param message User-visible error message (e.g., "Clock app not installed")
     * @param exception Optional exception for logging/debugging
     */
    data class Failure(
        val message: String,
        val exception: Throwable? = null
    ) : ActionResult()

    /**
     * Action needs app resolution before execution.
     *
     * Part of Intelligent Resolution System (Chapter 71).
     *
     * Returned when an action requires a specific app (email, SMS, etc.)
     * and the user's preferred app needs to be determined.
     *
     * ActionsManager will:
     * 1. Call AppResolverService.resolveApp(capability)
     * 2. If resolved → execute with the resolved package
     * 3. If multiple apps → trigger UI prompt
     * 4. If no apps → show install suggestion
     *
     * @param capability The capability ID (e.g., "email", "sms", "music")
     * @param data Action parameters to use after resolution
     */
    data class NeedsResolution(
        val capability: String,
        val data: Map<String, Any> = emptyMap()
    ) : ActionResult()
}
