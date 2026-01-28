/**
 * CommandManager - Action Result Models
 *
 * Sealed class hierarchy for action execution results.
 * Provides rich error information and recovery hints.
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Created: 2026-01-28
 */
package com.augmentalis.commandmanager

import kotlinx.serialization.Serializable

/**
 * Result of an action execution.
 *
 * Sealed hierarchy provides exhaustive handling:
 * - Success: Action completed successfully
 * - Failure variants: Different failure modes with context
 */
@Serializable
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
    @Serializable
    data class Success(
        override val message: String = "Action completed",
        val data: Map<String, String>? = null
    ) : ActionResult() {
        override val isSuccess: Boolean = true
    }

    // ═══════════════════════════════════════════════════════════════════
    // Failure Results
    // ═══════════════════════════════════════════════════════════════════

    /**
     * Element not found for the given AVID.
     */
    @Serializable
    data class ElementNotFound(
        val avid: String,
        override val message: String = "Element not found: $avid"
    ) : ActionResult() {
        override val isSuccess: Boolean = false
    }

    /**
     * Element found but not actionable (disabled, obscured, etc.)
     */
    @Serializable
    data class ElementNotActionable(
        val avid: String,
        val reason: String,
        override val message: String = "Element not actionable: $reason"
    ) : ActionResult() {
        override val isSuccess: Boolean = false
    }

    /**
     * Action not supported on this platform.
     */
    @Serializable
    data class NotSupported(
        val actionType: String,
        override val message: String = "Action not supported: $actionType"
    ) : ActionResult() {
        override val isSuccess: Boolean = false
    }

    /**
     * Permission required to perform action.
     */
    @Serializable
    data class PermissionRequired(
        val permission: String,
        override val message: String = "Permission required: $permission"
    ) : ActionResult() {
        override val isSuccess: Boolean = false
    }

    /**
     * Action timed out.
     */
    @Serializable
    data class Timeout(
        val timeoutMs: Long,
        override val message: String = "Action timed out after ${timeoutMs}ms"
    ) : ActionResult() {
        override val isSuccess: Boolean = false
    }

    /**
     * Generic execution error.
     */
    @Serializable
    data class Error(
        val error: String,
        override val message: String = "Execution error: $error"
    ) : ActionResult() {
        override val isSuccess: Boolean = false
    }

    /**
     * Service not available (e.g., accessibility service not running).
     */
    @Serializable
    data class ServiceUnavailable(
        val serviceName: String,
        override val message: String = "Service unavailable: $serviceName"
    ) : ActionResult() {
        override val isSuccess: Boolean = false
    }

    /**
     * Multiple matching elements found, disambiguation needed.
     */
    @Serializable
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
         * Create error result
         */
        fun error(message: String): ActionResult = Error(message)

        /**
         * Create element not found result
         */
        fun notFound(avid: String): ActionResult = ElementNotFound(avid)

        /**
         * Create not supported result
         */
        fun notSupported(action: String): ActionResult = NotSupported(action)
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
