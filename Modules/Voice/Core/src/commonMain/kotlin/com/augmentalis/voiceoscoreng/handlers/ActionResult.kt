/**
 * ActionResult.kt - Action execution results
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2026-01-05
 *
 * Sealed class hierarchy for action execution results.
 * Provides rich error information and recovery hints.
 */
package com.augmentalis.voiceoscoreng.handlers

import com.augmentalis.voiceoscoreng.common.CommandActionType

/**
 * Result of an action execution.
 *
 * Sealed hierarchy provides exhaustive handling:
 * - Success: Action completed successfully
 * - Failure: Action failed with error details
 * - Pending: Action requires additional user input
 */
sealed class ActionResult {

    /**
     * Whether the action succeeded
     */
    abstract val isSuccess: Boolean

    /**
     * Human-readable message
     */
    abstract val message: String

    // ═══════════════════════════════════════════════════════════════════
    // Success Results
    // ═══════════════════════════════════════════════════════════════════

    /**
     * Action completed successfully.
     *
     * @param message Success message
     * @param data Optional result data
     */
    data class Success(
        override val message: String = "Action completed",
        val data: Map<String, Any>? = null
    ) : ActionResult() {
        override val isSuccess: Boolean = true
    }

    // ═══════════════════════════════════════════════════════════════════
    // Failure Results
    // ═══════════════════════════════════════════════════════════════════

    /**
     * Element not found for the given VUID.
     */
    data class ElementNotFound(
        val vuid: String,
        override val message: String = "Element not found: $vuid"
    ) : ActionResult() {
        override val isSuccess: Boolean = false
    }

    /**
     * Element found but not actionable (disabled, obscured, etc.)
     */
    data class ElementNotActionable(
        val vuid: String,
        val reason: String,
        override val message: String = "Element not actionable: $reason"
    ) : ActionResult() {
        override val isSuccess: Boolean = false
    }

    /**
     * Action not supported on this platform.
     */
    data class NotSupported(
        val actionType: CommandActionType,
        override val message: String = "Action not supported: ${actionType.name}"
    ) : ActionResult() {
        override val isSuccess: Boolean = false
    }

    /**
     * Permission required to perform action.
     */
    data class PermissionRequired(
        val permission: String,
        override val message: String = "Permission required: $permission"
    ) : ActionResult() {
        override val isSuccess: Boolean = false
    }

    /**
     * Action timed out.
     */
    data class Timeout(
        val timeoutMs: Long,
        override val message: String = "Action timed out after ${timeoutMs}ms"
    ) : ActionResult() {
        override val isSuccess: Boolean = false
    }

    /**
     * Generic execution error.
     */
    data class Error(
        val error: String,
        val exception: Throwable? = null,
        override val message: String = "Execution error: $error"
    ) : ActionResult() {
        override val isSuccess: Boolean = false
    }

    /**
     * Service not available (e.g., accessibility service not running).
     */
    data class ServiceUnavailable(
        val serviceName: String,
        override val message: String = "Service unavailable: $serviceName"
    ) : ActionResult() {
        override val isSuccess: Boolean = false
    }

    // ═══════════════════════════════════════════════════════════════════
    // Pending Results (Require User Action)
    // ═══════════════════════════════════════════════════════════════════

    /**
     * Action requires confirmation from user.
     */
    data class ConfirmationRequired(
        val prompt: String,
        val confirmAction: String,
        override val message: String = "Confirmation required: $prompt"
    ) : ActionResult() {
        override val isSuccess: Boolean = false
    }

    /**
     * Multiple matching elements found, disambiguation needed.
     */
    data class Ambiguous(
        val candidates: List<String>,
        override val message: String = "Multiple matches found (${candidates.size})"
    ) : ActionResult() {
        override val isSuccess: Boolean = false
    }

    companion object {
        /**
         * Create success result
         */
        fun success(message: String = "OK"): ActionResult = Success(message)

        /**
         * Create error result from exception
         */
        fun fromException(e: Throwable): ActionResult = Error(
            error = e.message ?: "Unknown error",
            exception = e
        )

        /**
         * Create element not found result
         */
        fun notFound(vuid: String): ActionResult = ElementNotFound(vuid)

        /**
         * Create not supported result
         */
        fun notSupported(action: CommandActionType): ActionResult = NotSupported(action)
    }
}

/**
 * Extension to check if result is a specific type
 */
inline fun <reified T : ActionResult> ActionResult.isType(): Boolean = this is T

/**
 * Extension to get result as specific type or null
 */
inline fun <reified T : ActionResult> ActionResult.asType(): T? = this as? T

/**
 * Extension to map success result
 */
inline fun ActionResult.onSuccess(action: (ActionResult.Success) -> Unit): ActionResult {
    if (this is ActionResult.Success) action(this)
    return this
}

/**
 * Extension to map failure result
 */
inline fun ActionResult.onFailure(action: (ActionResult) -> Unit): ActionResult {
    if (!isSuccess) action(this)
    return this
}
