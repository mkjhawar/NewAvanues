/**
 * HandlerResult.kt - Result type for handler execution
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2026-01-06
 *
 * Rich result type for handler execution with feedback support.
 */
package com.augmentalis.voiceoscore

/**
 * Result of handler execution.
 *
 * Provides rich feedback beyond simple boolean success/failure.
 */
sealed class HandlerResult {
    /**
     * Handler successfully executed the action.
     *
     * @param message Optional feedback message
     * @param data Optional result data
     */
    data class Success(
        val message: String? = null,
        val data: Map<String, Any?> = emptyMap()
    ) : HandlerResult()

    /**
     * Handler failed to execute the action.
     *
     * @param reason Failure reason
     * @param recoverable Whether the action can be retried
     * @param suggestedAction Suggested alternative action
     */
    data class Failure(
        val reason: String,
        val recoverable: Boolean = true,
        val suggestedAction: String? = null
    ) : HandlerResult()

    /**
     * Handler could not handle the action (not a failure, just wrong handler).
     */
    data object NotHandled : HandlerResult()

    /**
     * Action requires additional input from user.
     *
     * @param prompt Prompt to show user
     * @param inputType Type of input expected
     */
    data class RequiresInput(
        val prompt: String,
        val inputType: InputType = InputType.TEXT
    ) : HandlerResult()

    /**
     * Action is in progress (for long-running operations).
     *
     * @param progress Progress percentage (0-100)
     * @param statusMessage Current status
     */
    data class InProgress(
        val progress: Int = 0,
        val statusMessage: String = ""
    ) : HandlerResult()

    /**
     * Action requires user to select from multiple options.
     *
     * Used for disambiguation when multiple elements match a voice command.
     * The system shows numbered badges only on matching elements and waits
     * for the user to say a number.
     *
     * @param message Message to display/speak
     * @param matchCount Number of matching elements
     * @param accessibilityAnnouncement Full announcement for screen readers
     */
    data class AwaitingSelection(
        val message: String,
        val matchCount: Int,
        val accessibilityAnnouncement: String
    ) : HandlerResult()

    /**
     * Convenience property to check if result is successful.
     */
    val isSuccess: Boolean get() = this is Success

    /**
     * Convenience property to check if result is failure.
     */
    val isFailure: Boolean get() = this is Failure

    /**
     * Convenience property to check if awaiting user selection.
     */
    val isAwaitingSelection: Boolean get() = this is AwaitingSelection

    companion object {
        /**
         * Create success result.
         *
         * @param message Optional feedback message
         * @param data Optional result data map
         */
        fun success(message: String? = null, data: Map<String, Any?> = emptyMap()): HandlerResult =
            Success(message, data)

        /**
         * Create failure result.
         *
         * @param reason Failure reason
         * @param recoverable Whether the action can be retried
         * @param suggestedAction Optional suggested alternative action
         */
        fun failure(
            reason: String,
            recoverable: Boolean = true,
            suggestedAction: String? = null
        ): HandlerResult = Failure(reason, recoverable, suggestedAction)

        /**
         * Create not handled result.
         */
        fun notHandled(): HandlerResult = NotHandled

        /**
         * Create awaiting selection result for disambiguation.
         *
         * @param message Message for display
         * @param matchCount Number of elements that matched
         * @param accessibilityAnnouncement Full announcement for TTS
         */
        fun awaitingSelection(
            message: String,
            matchCount: Int,
            accessibilityAnnouncement: String
        ): HandlerResult = AwaitingSelection(message, matchCount, accessibilityAnnouncement)
    }
}

/**
 * Type of input expected from user.
 */
enum class InputType {
    TEXT,
    NUMBER,
    CHOICE,
    CONFIRMATION
}
