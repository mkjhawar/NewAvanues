package com.augmentalis.intentactions

/**
 * Result of an intent action execution.
 */
sealed class IntentResult {
    /** Action executed successfully */
    data class Success(
        val message: String,
        val data: Map<String, Any>? = null
    ) : IntentResult()

    /** Action needs additional information from the user */
    data class NeedsMoreInfo(
        val missingEntity: EntityType,
        val prompt: String
    ) : IntentResult()

    /** Action execution failed */
    data class Failed(
        val reason: String,
        val exception: Throwable? = null
    ) : IntentResult()
}
