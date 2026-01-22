package com.augmentalis.webavanue

/**
 * Result of action execution for VoiceOS commands
 *
 * Used to indicate success/failure and provide user feedback.
 * VoiceOS can use this to provide voice responses like "Zoomed in" or "Error: No active tab".
 *
 * @param success Whether the action succeeded
 * @param message Optional message for user feedback (voice or UI)
 * @param data Optional data to return (e.g., created tab ID, current URL)
 */
data class ActionResult(
    val success: Boolean,
    val message: String? = null,
    val data: Any? = null
) {
    companion object {
        /**
         * Create successful result
         *
         * @param message Optional success message for user feedback
         * @param data Optional data to return (e.g., created tab ID)
         */
        fun success(message: String? = null, data: Any? = null) =
            ActionResult(true, message, data)

        /**
         * Create error result
         *
         * @param message Error message describing what went wrong
         */
        fun error(message: String) =
            ActionResult(false, message, null)
    }
}
