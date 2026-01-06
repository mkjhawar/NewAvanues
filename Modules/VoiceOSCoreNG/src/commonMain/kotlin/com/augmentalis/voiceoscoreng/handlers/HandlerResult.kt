/**
 * HandlerResult.kt - Result type for handler execution
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2026-01-06
 *
 * Rich result type for handler execution with feedback support.
 */
package com.augmentalis.voiceoscoreng.handlers

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
     * Convenience property to check if result is successful.
     */
    val isSuccess: Boolean get() = this is Success

    /**
     * Convenience property to check if result is failure.
     */
    val isFailure: Boolean get() = this is Failure

    companion object {
        /**
         * Create success result.
         */
        fun success(message: String? = null): HandlerResult = Success(message)

        /**
         * Create failure result.
         */
        fun failure(reason: String, recoverable: Boolean = true): HandlerResult =
            Failure(reason, recoverable)

        /**
         * Create not handled result.
         */
        fun notHandled(): HandlerResult = NotHandled
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
