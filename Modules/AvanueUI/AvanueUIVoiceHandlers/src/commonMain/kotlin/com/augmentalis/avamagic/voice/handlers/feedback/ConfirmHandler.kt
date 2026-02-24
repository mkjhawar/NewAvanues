/**
 * ConfirmHandler.kt - Voice handler for Confirmation dialog interactions
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Created: 2026-01-28
 *
 * Purpose: Voice-driven control for confirmation dialogs
 * Features:
 * - Confirm actions with "yes", "confirm", "proceed"
 * - Cancel actions with "no", "cancel", "abort"
 * - Read the confirmation prompt/question
 * - Handle destructive action confirmations
 * - AVID-based targeting for precise element selection
 * - Voice feedback for confirmation actions
 *
 * Location: MagicVoiceHandlers module - feedback category
 *
 * ## Supported Commands
 *
 * Confirmation:
 * - "yes" - Confirm the action
 * - "confirm" - Confirm the action
 * - "proceed" - Proceed with the action
 * - "continue" - Continue with the action
 * - "do it" - Confirm (colloquial)
 *
 * Cancellation:
 * - "no" - Cancel the action
 * - "cancel" - Cancel the action
 * - "abort" - Abort the action
 * - "stop" - Stop/cancel
 * - "don't" - Cancel (colloquial)
 *
 * Reading:
 * - "read question" - Read the confirmation prompt
 * - "what is it asking" - Read the prompt
 * - "read prompt" - Read confirmation text
 *
 * ## Confirmation Dialog Detection
 *
 * Supports detection of:
 * - AlertDialog with positive/negative buttons
 * - MaterialAlertDialog
 * - Custom confirmation implementations
 * - Destructive action confirmations (delete, remove, etc.)
 */

package com.augmentalis.avamagic.voice.handlers.feedback

import com.avanues.logging.LoggerFactory
import com.augmentalis.voiceoscore.ActionCategory
import com.augmentalis.voiceoscore.BaseHandler
import com.augmentalis.voiceoscore.Bounds
import com.augmentalis.voiceoscore.ElementInfo
import com.augmentalis.voiceoscore.HandlerResult
import com.augmentalis.voiceoscore.QuantizedCommand

/**
 * Voice command handler for Confirmation dialog interactions.
 *
 * Provides comprehensive voice control for confirmation dialogs including:
 * - Confirmation commands (yes, confirm, proceed)
 * - Cancellation commands (no, cancel, abort)
 * - Prompt reading via TTS
 * - Destructive action awareness
 *
 * Thread Safety:
 * - All operations are suspend functions
 * - State modifications are atomic via executor
 *
 * @param executor Platform-specific executor for confirmation operations
 */
class ConfirmHandler(
    private val executor: ConfirmExecutor
) : BaseHandler() {

    companion object {
        private const val TAG = "ConfirmHandler"
        private val Log = LoggerFactory.getLogger(TAG)

        // Command patterns for confirmation actions
        private val CONFIRM_COMMANDS = setOf(
            "yes", "confirm", "proceed", "continue", "do it",
            "okay", "ok", "accept", "agree", "sure", "affirmative",
            "go ahead", "yes please", "do that"
        )

        private val CANCEL_COMMANDS = setOf(
            "no", "cancel", "abort", "stop", "don't",
            "nope", "negative", "decline", "reject", "back",
            "no thanks", "never mind", "forget it"
        )

        private val READ_COMMANDS = setOf(
            "read question", "what is it asking", "read prompt",
            "what does it say", "read it", "tell me"
        )

        // Keywords that indicate destructive actions
        private val DESTRUCTIVE_KEYWORDS = setOf(
            "delete", "remove", "erase", "clear", "reset",
            "discard", "destroy", "permanent", "irreversible"
        )
    }

    override val category: ActionCategory = ActionCategory.UI

    override val supportedActions: List<String> = listOf(
        // Confirm actions
        "yes", "confirm", "proceed", "continue", "do it",
        "okay", "ok", "accept", "agree", "sure", "affirmative",
        "go ahead", "yes please",
        // Cancel actions
        "no", "cancel", "abort", "stop", "don't",
        "nope", "negative", "decline", "reject", "back",
        "no thanks", "never mind",
        // Read actions
        "read question", "what is it asking", "read prompt",
        "what does it say", "read it", "tell me"
    )

    /**
     * Callback for voice feedback when confirmation is made.
     */
    var onConfirmed: ((question: String?, isDestructive: Boolean) -> Unit)? = null

    /**
     * Callback for voice feedback when cancellation is made.
     */
    var onCancelled: ((question: String?) -> Unit)? = null

    /**
     * Callback for voice feedback when prompt is read.
     */
    var onPromptRead: ((question: String, isDestructive: Boolean) -> Unit)? = null

    /**
     * Whether to require explicit confirmation for destructive actions.
     * If true, "yes" alone may not be sufficient for destructive actions.
     */
    var requireExplicitDestructiveConfirm: Boolean = false

    override suspend fun execute(
        command: QuantizedCommand,
        params: Map<String, Any>
    ): HandlerResult {
        val normalizedAction = command.phrase.lowercase().trim()

        Log.d { "Processing confirmation command: $normalizedAction" }

        return try {
            when {
                // Confirm commands
                CONFIRM_COMMANDS.any { normalizedAction == it || normalizedAction.contains(it) } -> {
                    handleConfirm(normalizedAction, command)
                }

                // Cancel commands
                CANCEL_COMMANDS.any { normalizedAction == it || normalizedAction.contains(it) } -> {
                    handleCancel(command)
                }

                // Read commands
                READ_COMMANDS.any { normalizedAction.contains(it) } -> {
                    handleReadQuestion(command)
                }

                else -> HandlerResult.notHandled()
            }
        } catch (e: Exception) {
            Log.e({ "Error executing confirmation command" }, e)
            HandlerResult.failure("Error: ${e.message}", recoverable = true)
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Command Handlers
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Handle confirmation commands (yes, confirm, proceed).
     */
    private suspend fun handleConfirm(
        normalizedAction: String,
        command: QuantizedCommand
    ): HandlerResult {
        // Find the current confirmation dialog
        val confirmInfo = findConfirmation(avid = command.targetAvid)
            ?: return HandlerResult.Failure(
                reason = "No confirmation dialog found",
                recoverable = true,
                suggestedAction = "There doesn't appear to be a confirmation on screen"
            )

        // Check for destructive action and require explicit confirmation
        if (confirmInfo.isDestructive && requireExplicitDestructiveConfirm) {
            val isExplicit = normalizedAction == "confirm" ||
                             normalizedAction.contains("confirm") ||
                             normalizedAction == "proceed" ||
                             normalizedAction.contains("do it")

            if (!isExplicit) {
                return HandlerResult.Failure(
                    reason = "This is a destructive action. Please say 'confirm' or 'proceed' explicitly.",
                    recoverable = true,
                    suggestedAction = "Say 'confirm' to proceed with this action"
                )
            }
        }

        // Confirm the action
        val result = executor.confirm(confirmInfo)

        return if (result.success) {
            onConfirmed?.invoke(confirmInfo.question, confirmInfo.isDestructive)

            val message = if (confirmInfo.isDestructive) {
                "Action confirmed"
            } else {
                "Confirmed"
            }

            Log.i { "Confirmation: $message - ${confirmInfo.question}" }

            HandlerResult.Success(
                message = message,
                data = mapOf(
                    "confirmAvid" to confirmInfo.avid,
                    "question" to (confirmInfo.question ?: ""),
                    "action" to "confirm",
                    "isDestructive" to confirmInfo.isDestructive,
                    "buttonClicked" to (result.buttonClicked ?: ""),
                    "accessibility_announcement" to message
                )
            )
        } else {
            HandlerResult.failure(
                reason = result.error ?: "Could not confirm action",
                recoverable = true
            )
        }
    }

    /**
     * Handle cancellation commands (no, cancel, abort).
     */
    private suspend fun handleCancel(command: QuantizedCommand): HandlerResult {
        // Find the current confirmation dialog
        val confirmInfo = findConfirmation(avid = command.targetAvid)
            ?: return HandlerResult.Failure(
                reason = "No confirmation dialog found",
                recoverable = true,
                suggestedAction = "There doesn't appear to be a confirmation on screen"
            )

        // Cancel the action
        val result = executor.cancel(confirmInfo)

        return if (result.success) {
            onCancelled?.invoke(confirmInfo.question)

            Log.i { "Cancellation: ${confirmInfo.question}" }

            HandlerResult.Success(
                message = "Cancelled",
                data = mapOf(
                    "confirmAvid" to confirmInfo.avid,
                    "question" to (confirmInfo.question ?: ""),
                    "action" to "cancel",
                    "buttonClicked" to (result.buttonClicked ?: ""),
                    "accessibility_announcement" to "Cancelled"
                )
            )
        } else {
            HandlerResult.failure(
                reason = result.error ?: "Could not cancel action",
                recoverable = true
            )
        }
    }

    /**
     * Handle read question commands.
     */
    private suspend fun handleReadQuestion(command: QuantizedCommand): HandlerResult {
        // Find the current confirmation dialog
        val confirmInfo = findConfirmation(avid = command.targetAvid)
            ?: return HandlerResult.Failure(
                reason = "No confirmation dialog found",
                recoverable = true,
                suggestedAction = "There doesn't appear to be a confirmation on screen"
            )

        // Get the confirmation content
        val content = executor.getConfirmContent(confirmInfo)

        // Build the message to read
        val messageToRead = buildString {
            if (confirmInfo.isDestructive) {
                append("Warning: This is a destructive action. ")
            }
            if (!content.title.isNullOrBlank()) {
                append(content.title)
                append(". ")
            }
            if (!content.question.isNullOrBlank()) {
                append(content.question)
            }
            if (content.positiveButton != null || content.negativeButton != null) {
                append(". Say ")
                if (content.positiveButton != null) {
                    append("'yes' to ${content.positiveButton.lowercase()}")
                }
                if (content.positiveButton != null && content.negativeButton != null) {
                    append(" or ")
                }
                if (content.negativeButton != null) {
                    append("'no' to ${content.negativeButton.lowercase()}")
                }
            }
        }

        onPromptRead?.invoke(content.question ?: "", confirmInfo.isDestructive)

        Log.i { "Reading confirmation prompt: $messageToRead" }

        return HandlerResult.Success(
            message = messageToRead,
            data = mapOf(
                "confirmAvid" to confirmInfo.avid,
                "title" to (content.title ?: ""),
                "question" to (content.question ?: ""),
                "positiveButton" to (content.positiveButton ?: ""),
                "negativeButton" to (content.negativeButton ?: ""),
                "isDestructive" to confirmInfo.isDestructive,
                "shouldSpeak" to true,
                "accessibility_announcement" to messageToRead
            )
        )
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Helper Methods
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Find confirmation dialog by AVID or detect current confirmation on screen.
     */
    private suspend fun findConfirmation(avid: String? = null): ConfirmInfo? {
        // Priority 1: AVID lookup (fastest, most precise)
        if (avid != null) {
            val confirm = executor.findConfirmByAvid(avid)
            if (confirm != null) return confirm
        }

        // Priority 2: Find any visible confirmation dialog
        return executor.findVisibleConfirm()
    }

    /**
     * Check if text contains destructive action keywords.
     */
    private fun containsDestructiveKeywords(text: String?): Boolean {
        if (text == null) return false
        val lowerText = text.lowercase()
        return DESTRUCTIVE_KEYWORDS.any { lowerText.contains(it) }
    }

}

// ═══════════════════════════════════════════════════════════════════════════════
// Supporting Types
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * Types of confirmation dialogs.
 */
enum class ConfirmationType {
    /** Standard yes/no confirmation */
    STANDARD,
    /** Destructive action confirmation (delete, remove, etc.) */
    DESTRUCTIVE,
    /** Permission request confirmation */
    PERMISSION,
    /** Action requiring authentication */
    AUTHENTICATED,
    /** Custom confirmation type */
    CUSTOM
}

/**
 * Information about a confirmation dialog component.
 *
 * @property avid AVID fingerprint for the confirmation (format: CFM:{hash8})
 * @property question The confirmation prompt/question
 * @property confirmationType Type of confirmation
 * @property isDestructive Whether this confirms a destructive action
 * @property positiveLabel Label for the positive/confirm button
 * @property negativeLabel Label for the negative/cancel button
 * @property bounds Screen bounds for the confirmation dialog
 * @property node Platform-specific node reference
 */
data class ConfirmInfo(
    val avid: String,
    val question: String? = null,
    val confirmationType: ConfirmationType = ConfirmationType.STANDARD,
    val isDestructive: Boolean = false,
    val positiveLabel: String? = null,
    val negativeLabel: String? = null,
    val bounds: Bounds = Bounds.EMPTY,
    val node: Any? = null
) {
    /**
     * Convert to ElementInfo for general element operations.
     */
    fun toElementInfo(): ElementInfo = ElementInfo(
        className = "ConfirmDialog",
        text = question ?: "",
        bounds = bounds,
        isClickable = true,
        isEnabled = true,
        avid = avid,
        stateDescription = if (isDestructive) "destructive" else confirmationType.name
    )
}

/**
 * Content extracted from a confirmation dialog.
 *
 * @property title Dialog title
 * @property question The confirmation question/prompt
 * @property positiveButton Label of the positive/confirm button
 * @property negativeButton Label of the negative/cancel button
 * @property neutralButton Label of the neutral button (if present)
 */
data class ConfirmContent(
    val title: String? = null,
    val question: String? = null,
    val positiveButton: String? = null,
    val negativeButton: String? = null,
    val neutralButton: String? = null
)

/**
 * Result of a confirmation operation.
 */
data class ConfirmOperationResult(
    val success: Boolean,
    val error: String? = null,
    val action: String? = null,
    val buttonClicked: String? = null
) {
    companion object {
        fun success(action: String? = null, buttonClicked: String? = null) = ConfirmOperationResult(
            success = true,
            action = action,
            buttonClicked = buttonClicked
        )

        fun error(message: String) = ConfirmOperationResult(
            success = false,
            error = message
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// Platform Executor Interface
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * Platform-specific executor for confirmation dialog operations.
 *
 * Implementations should:
 * 1. Detect confirmation dialogs on screen
 * 2. Extract confirmation content (question, buttons)
 * 3. Execute confirm/cancel actions
 * 4. Identify destructive action confirmations
 *
 * ## Confirmation Detection Algorithm
 *
 * ```kotlin
 * fun findConfirmNode(rootNode: AccessibilityNodeInfo): AccessibilityNodeInfo? {
 *     // Look for nodes with:
 *     // - className: android.app.AlertDialog
 *     // - Has exactly two action buttons
 *     // - Contains question text
 *     // - paneTitle present
 * }
 * ```
 *
 * ## Destructive Action Detection
 *
 * ```kotlin
 * fun isDestructive(confirmNode: AccessibilityNodeInfo): Boolean {
 *     // Check for:
 *     // - Keywords: delete, remove, erase, clear, reset, discard
 *     // - Red/warning colored positive button
 *     // - Icon indicating danger
 * }
 * ```
 */
interface ConfirmExecutor {

    // ═══════════════════════════════════════════════════════════════════════════
    // Confirmation Discovery
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Find a confirmation dialog by its AVID fingerprint.
     *
     * @param avid The AVID fingerprint (format: CFM:{hash8})
     * @return ConfirmInfo if found, null otherwise
     */
    suspend fun findConfirmByAvid(avid: String): ConfirmInfo?

    /**
     * Find the currently visible confirmation dialog.
     *
     * Searches for any confirmation dialog currently displayed on screen.
     *
     * @return ConfirmInfo if a confirmation is visible, null otherwise
     */
    suspend fun findVisibleConfirm(): ConfirmInfo?

    /**
     * Check if any confirmation dialog is currently visible.
     *
     * @return true if a confirmation is on screen, false otherwise
     */
    suspend fun isConfirmVisible(): Boolean

    // ═══════════════════════════════════════════════════════════════════════════
    // Confirmation Actions
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Confirm the action (click positive button).
     *
     * @param confirm The confirmation dialog
     * @return Operation result with the button that was clicked
     */
    suspend fun confirm(confirm: ConfirmInfo): ConfirmOperationResult

    /**
     * Cancel the action (click negative button).
     *
     * @param confirm The confirmation dialog
     * @return Operation result with the button that was clicked
     */
    suspend fun cancel(confirm: ConfirmInfo): ConfirmOperationResult

    /**
     * Dismiss the confirmation without action (if possible).
     *
     * @param confirm The confirmation dialog
     * @return Operation result
     */
    suspend fun dismiss(confirm: ConfirmInfo): ConfirmOperationResult

    // ═══════════════════════════════════════════════════════════════════════════
    // Content Extraction
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Get the full content of a confirmation dialog.
     *
     * @param confirm The confirmation dialog
     * @return ConfirmContent with title, question, and button labels
     */
    suspend fun getConfirmContent(confirm: ConfirmInfo): ConfirmContent

    /**
     * Check if the confirmation is for a destructive action.
     *
     * @param confirm The confirmation dialog
     * @return true if destructive action, false otherwise
     */
    suspend fun isDestructiveAction(confirm: ConfirmInfo): Boolean

    /**
     * Get the question/prompt text from the confirmation.
     *
     * @param confirm The confirmation dialog
     * @return Question text
     */
    suspend fun getQuestion(confirm: ConfirmInfo): String?
}
