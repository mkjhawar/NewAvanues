package com.augmentalis.commandmanager

/**
 * Represents the result of processing a UI element for voice command generation.
 *
 * This sealed class hierarchy provides type-safe handling of processing outcomes:
 * - [Success]: Element was successfully processed and commands were generated
 * - [Failure]: Processing failed with an error
 * - [Skipped]: Element was intentionally skipped (e.g., not actionable, filtered out)
 */
sealed class ElementProcessingResult {

    /**
     * Element was successfully processed
     *
     * @property avid The generated Augmentalis Voice Identifier for this element
     * @property commandsGenerated Number of voice commands generated
     * @property processingTimeMs Time taken to process in milliseconds
     */
    data class Success(
        val avid: String,
        val commandsGenerated: Int,
        val processingTimeMs: Long
    ) : ElementProcessingResult() {
        /**
         * Legacy alias for avid (deprecated, use avid directly).
         */
        @Deprecated("Use avid instead", ReplaceWith("avid"))
        val vuid: String get() = avid
    }

    /**
     * Processing failed with an error
     *
     * @property error Description of the error
     * @property elementInfo The element that failed processing (if available)
     */
    data class Failure(
        val error: String,
        val elementInfo: ElementInfo? = null
    ) : ElementProcessingResult()

    /**
     * Element was intentionally skipped
     *
     * @property reason Explanation of why the element was skipped
     */
    data class Skipped(
        val reason: String
    ) : ElementProcessingResult()

    /**
     * Check if this result is a success
     */
    fun isSuccess(): Boolean = this is Success

    /**
     * Check if this result is a failure
     */
    fun isFailure(): Boolean = this is Failure

    /**
     * Check if this result is skipped
     */
    fun isSkipped(): Boolean = this is Skipped

    /**
     * Get the AVID if this is a success, null otherwise
     */
    fun getAvidOrNull(): String? = (this as? Success)?.avid

    /**
     * Legacy alias for getAvidOrNull (deprecated).
     */
    @Deprecated("Use getAvidOrNull instead", ReplaceWith("getAvidOrNull()"))
    fun getVuidOrNull(): String? = getAvidOrNull()

    /**
     * Get the error message if this is a failure, null otherwise
     */
    fun getErrorOrNull(): String? = (this as? Failure)?.error

    /**
     * Get the skip reason if this is skipped, null otherwise
     */
    fun getSkipReasonOrNull(): String? = (this as? Skipped)?.reason

    /**
     * Transform the result using the provided functions
     *
     * @param onSuccess Function to call if result is Success
     * @param onFailure Function to call if result is Failure
     * @param onSkipped Function to call if result is Skipped
     * @return The result of calling the appropriate function
     */
    inline fun <T> fold(
        onSuccess: (Success) -> T,
        onFailure: (Failure) -> T,
        onSkipped: (Skipped) -> T
    ): T = when (this) {
        is Success -> onSuccess(this)
        is Failure -> onFailure(this)
        is Skipped -> onSkipped(this)
    }

    /**
     * Execute action if this is a success
     */
    inline fun onSuccess(action: (Success) -> Unit): ElementProcessingResult {
        if (this is Success) action(this)
        return this
    }

    /**
     * Execute action if this is a failure
     */
    inline fun onFailure(action: (Failure) -> Unit): ElementProcessingResult {
        if (this is Failure) action(this)
        return this
    }

    /**
     * Execute action if this is skipped
     */
    inline fun onSkipped(action: (Skipped) -> Unit): ElementProcessingResult {
        if (this is Skipped) action(this)
        return this
    }

    companion object {
        /**
         * Create a success result
         */
        fun success(avid: String, commandsGenerated: Int = 1, processingTimeMs: Long = 0) =
            Success(avid, commandsGenerated, processingTimeMs)

        /**
         * Create a failure result
         */
        fun failure(error: String, elementInfo: ElementInfo? = null) =
            Failure(error, elementInfo)

        /**
         * Create a skipped result
         */
        fun skipped(reason: String) = Skipped(reason)
    }
}
