/**
 * ActionResult.kt - Action execution results
 *
 * Re-exports ActionResult from CommandManager and adds VoiceOSCore-specific extensions.
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Migrated: 2026-01-28
 */
package com.augmentalis.voiceoscore

import com.augmentalis.commandmanager.ActionResult as CMActionResult

/**
 * Result of an action execution.
 *
 * Sealed hierarchy provides exhaustive handling:
 * - Success: Action completed successfully
 * - Failure: Action failed with error details
 * - Pending: Action requires additional user input
 */
sealed class ActionResult {

    abstract val isSuccess: Boolean
    abstract val message: String

    // ═══════════════════════════════════════════════════════════════════
    // Success Results
    // ═══════════════════════════════════════════════════════════════════

    data class Success(
        override val message: String = "Action completed",
        val data: Map<String, Any>? = null
    ) : ActionResult() {
        override val isSuccess: Boolean = true
    }

    // ═══════════════════════════════════════════════════════════════════
    // Failure Results
    // ═══════════════════════════════════════════════════════════════════

    data class ElementNotFound(
        val avid: String,
        override val message: String = "Element not found: $avid"
    ) : ActionResult() {
        override val isSuccess: Boolean = false

        @Deprecated("Use avid instead", ReplaceWith("avid"))
        val vuid: String get() = avid
    }

    data class ElementNotActionable(
        val avid: String,
        val reason: String,
        override val message: String = "Element not actionable: $reason"
    ) : ActionResult() {
        override val isSuccess: Boolean = false

        @Deprecated("Use avid instead", ReplaceWith("avid"))
        val vuid: String get() = avid
    }

    data class NotSupported(
        val actionType: CommandActionType,
        override val message: String = "Action not supported: ${actionType.name}"
    ) : ActionResult() {
        override val isSuccess: Boolean = false
    }

    data class PermissionRequired(
        val permission: String,
        override val message: String = "Permission required: $permission"
    ) : ActionResult() {
        override val isSuccess: Boolean = false
    }

    data class Timeout(
        val timeoutMs: Long,
        override val message: String = "Action timed out after ${timeoutMs}ms"
    ) : ActionResult() {
        override val isSuccess: Boolean = false
    }

    data class Error(
        val error: String,
        val exception: Throwable? = null,
        override val message: String = "Execution error: $error"
    ) : ActionResult() {
        override val isSuccess: Boolean = false
    }

    data class ServiceUnavailable(
        val serviceName: String,
        override val message: String = "Service unavailable: $serviceName"
    ) : ActionResult() {
        override val isSuccess: Boolean = false
    }

    // ═══════════════════════════════════════════════════════════════════
    // Pending Results (Require User Action)
    // ═══════════════════════════════════════════════════════════════════

    data class ConfirmationRequired(
        val prompt: String,
        val confirmAction: String,
        override val message: String = "Confirmation required: $prompt"
    ) : ActionResult() {
        override val isSuccess: Boolean = false
    }

    data class Ambiguous(
        val candidates: List<String>,
        override val message: String = "Multiple matches found (${candidates.size})"
    ) : ActionResult() {
        override val isSuccess: Boolean = false
    }

    companion object {
        fun success(message: String = "OK"): ActionResult = Success(message)
        fun fromException(e: Throwable): ActionResult = Error(
            error = e.message ?: "Unknown error",
            exception = e
        )
        fun notFound(avid: String): ActionResult = ElementNotFound(avid)
        fun notSupported(action: CommandActionType): ActionResult = NotSupported(action)
    }
}

inline fun <reified T : ActionResult> ActionResult.isType(): Boolean = this is T
inline fun <reified T : ActionResult> ActionResult.asType(): T? = this as? T

inline fun ActionResult.onSuccess(action: (ActionResult.Success) -> Unit): ActionResult {
    if (this is ActionResult.Success) action(this)
    return this
}

inline fun ActionResult.onFailure(action: (ActionResult) -> Unit): ActionResult {
    if (!isSuccess) action(this)
    return this
}
