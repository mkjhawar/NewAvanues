/**
 * SnackbarHandler.kt - Voice handler for Snackbar notification interactions
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2026-01-28
 *
 * Purpose: Voice-driven control for snackbar notifications
 * Features:
 * - Dismiss snackbar notifications
 * - Trigger snackbar action buttons (e.g., Undo)
 * - Read snackbar message content aloud
 * - Automatic snackbar detection and targeting
 * - AVID-based targeting for precise element selection
 * - Voice feedback for snackbar actions
 *
 * Location: MagicVoiceHandlers module - feedback category
 *
 * ## Supported Commands
 *
 * Dismissal:
 * - "dismiss" - Dismiss the current snackbar
 * - "close" - Close the snackbar
 * - "dismiss snackbar" - Dismiss snackbar explicitly
 *
 * Action:
 * - "action" - Trigger the snackbar action button
 * - "undo" - Trigger undo action (common snackbar use case)
 * - "click action" - Click the action button
 *
 * Reading:
 * - "read message" - Read the snackbar text aloud
 * - "what did it say" - Read snackbar content
 * - "read snackbar" - Read snackbar message
 *
 * ## Snackbar Detection
 *
 * Supports detection of:
 * - Material Snackbar
 * - Custom snackbar implementations
 * - Toast-like snackbars
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
 * Voice command handler for Snackbar notification interactions.
 *
 * Provides comprehensive voice control for snackbar notifications including:
 * - Dismissal commands (dismiss, close)
 * - Action triggering (action, undo)
 * - Content reading via TTS
 * - Automatic snackbar detection
 *
 * Thread Safety:
 * - All operations are suspend functions
 * - State modifications are atomic via executor
 *
 * @param executor Platform-specific executor for snackbar operations
 */
class SnackbarHandler(
    private val executor: SnackbarExecutor
) : BaseHandler() {

    companion object {
        private const val TAG = "SnackbarHandler"
        private val Log = LoggerFactory.getLogger(TAG)

        // Command patterns for snackbar actions
        private val DISMISS_COMMANDS = setOf(
            "dismiss", "close", "dismiss snackbar", "close snackbar",
            "hide", "hide snackbar"
        )

        private val ACTION_COMMANDS = setOf(
            "action", "undo", "click action", "snackbar action",
            "do action", "tap action"
        )

        private val READ_COMMANDS = setOf(
            "read message", "what did it say", "read snackbar",
            "what does it say", "read it", "repeat message"
        )
    }

    override val category: ActionCategory = ActionCategory.UI

    override val supportedActions: List<String> = listOf(
        // Dismiss actions
        "dismiss", "close", "dismiss snackbar", "close snackbar",
        "hide", "hide snackbar",
        // Action triggers
        "action", "undo", "click action", "snackbar action",
        "do action", "tap action",
        // Read actions
        "read message", "what did it say", "read snackbar",
        "what does it say", "read it", "repeat message"
    )

    /**
     * Callback for voice feedback when snackbar message is read.
     */
    var onSnackbarRead: ((message: String, actionLabel: String?) -> Unit)? = null

    /**
     * Callback for voice feedback when snackbar is dismissed.
     */
    var onSnackbarDismissed: (() -> Unit)? = null

    /**
     * Callback for voice feedback when snackbar action is triggered.
     */
    var onSnackbarActionTriggered: ((actionLabel: String) -> Unit)? = null

    override suspend fun execute(
        command: QuantizedCommand,
        params: Map<String, Any>
    ): HandlerResult {
        val normalizedAction = command.phrase.lowercase().trim()

        Log.d { "Processing snackbar command: $normalizedAction" }

        return try {
            when {
                // Dismiss commands
                DISMISS_COMMANDS.any { normalizedAction.contains(it) } -> {
                    handleDismiss(command)
                }

                // Action commands
                ACTION_COMMANDS.any { normalizedAction.contains(it) } -> {
                    handleAction(command)
                }

                // Read commands
                READ_COMMANDS.any { normalizedAction.contains(it) } -> {
                    handleReadMessage(command)
                }

                else -> HandlerResult.notHandled()
            }
        } catch (e: Exception) {
            Log.e({ "Error executing snackbar command" }, e)
            HandlerResult.failure("Error: ${e.message}", recoverable = true)
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Command Handlers
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Handle dismiss/close snackbar commands.
     */
    private suspend fun handleDismiss(command: QuantizedCommand): HandlerResult {
        // Find the current snackbar
        val snackbarInfo = findSnackbar(avid = command.targetAvid)
            ?: return HandlerResult.Failure(
                reason = "No snackbar found",
                recoverable = true,
                suggestedAction = "There doesn't appear to be a snackbar on screen"
            )

        // Dismiss the snackbar
        val result = executor.dismissSnackbar(snackbarInfo)

        return if (result.success) {
            onSnackbarDismissed?.invoke()

            Log.i { "Snackbar dismissed" }

            HandlerResult.Success(
                message = "Snackbar dismissed",
                data = mapOf(
                    "snackbarAvid" to snackbarInfo.avid,
                    "snackbarMessage" to snackbarInfo.message,
                    "accessibility_announcement" to "Snackbar dismissed"
                )
            )
        } else {
            HandlerResult.failure(
                reason = result.error ?: "Could not dismiss snackbar",
                recoverable = true
            )
        }
    }

    /**
     * Handle action/undo commands.
     */
    private suspend fun handleAction(command: QuantizedCommand): HandlerResult {
        // Find the current snackbar
        val snackbarInfo = findSnackbar(avid = command.targetAvid)
            ?: return HandlerResult.Failure(
                reason = "No snackbar found",
                recoverable = true,
                suggestedAction = "There doesn't appear to be a snackbar on screen"
            )

        // Check if snackbar has an action
        if (snackbarInfo.actionLabel == null) {
            return HandlerResult.Failure(
                reason = "Snackbar has no action button",
                recoverable = true,
                suggestedAction = "This snackbar doesn't have an action to trigger"
            )
        }

        // Trigger the action
        val result = executor.triggerAction(snackbarInfo)

        return if (result.success) {
            onSnackbarActionTriggered?.invoke(snackbarInfo.actionLabel)

            Log.i { "Snackbar action triggered: ${snackbarInfo.actionLabel}" }

            HandlerResult.Success(
                message = "${snackbarInfo.actionLabel} action triggered",
                data = mapOf(
                    "snackbarAvid" to snackbarInfo.avid,
                    "actionLabel" to snackbarInfo.actionLabel,
                    "accessibility_announcement" to "${snackbarInfo.actionLabel} action triggered"
                )
            )
        } else {
            HandlerResult.failure(
                reason = result.error ?: "Could not trigger snackbar action",
                recoverable = true
            )
        }
    }

    /**
     * Handle read message commands.
     */
    private suspend fun handleReadMessage(command: QuantizedCommand): HandlerResult {
        // Find the current snackbar
        val snackbarInfo = findSnackbar(avid = command.targetAvid)
            ?: return HandlerResult.Failure(
                reason = "No snackbar found",
                recoverable = true,
                suggestedAction = "There doesn't appear to be a snackbar on screen"
            )

        // Build the message to read
        val messageToRead = buildString {
            append(snackbarInfo.message)
            if (snackbarInfo.actionLabel != null) {
                append(". Action available: ")
                append(snackbarInfo.actionLabel)
            }
        }

        onSnackbarRead?.invoke(snackbarInfo.message, snackbarInfo.actionLabel)

        Log.i { "Reading snackbar content: $messageToRead" }

        return HandlerResult.Success(
            message = messageToRead,
            data = mapOf(
                "snackbarAvid" to snackbarInfo.avid,
                "snackbarMessage" to snackbarInfo.message,
                "snackbarAction" to (snackbarInfo.actionLabel ?: ""),
                "shouldSpeak" to true,
                "accessibility_announcement" to messageToRead
            )
        )
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Helper Methods
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Find snackbar by AVID or detect current snackbar on screen.
     */
    private suspend fun findSnackbar(avid: String? = null): SnackbarInfo? {
        // Priority 1: AVID lookup (fastest, most precise)
        if (avid != null) {
            val snackbar = executor.findSnackbarByAvid(avid)
            if (snackbar != null) return snackbar
        }

        // Priority 2: Find any visible snackbar
        return executor.findVisibleSnackbar()
    }

}

// ═══════════════════════════════════════════════════════════════════════════════
// Supporting Types
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * Duration types for snackbars.
 */
enum class SnackbarDuration {
    /** Short duration (~1500ms) */
    SHORT,
    /** Long duration (~2750ms) */
    LONG,
    /** Indefinite - stays until dismissed */
    INDEFINITE
}

/**
 * Information about a snackbar notification component.
 *
 * @property avid AVID fingerprint for the snackbar (format: SNK:{hash8})
 * @property message Snackbar message text
 * @property actionLabel Label for the action button (null if no action)
 * @property duration Snackbar display duration
 * @property isDismissable Whether the snackbar can be manually dismissed
 * @property bounds Screen bounds for the snackbar
 * @property node Platform-specific node reference
 */
data class SnackbarInfo(
    val avid: String,
    val message: String,
    val actionLabel: String? = null,
    val duration: SnackbarDuration = SnackbarDuration.SHORT,
    val isDismissable: Boolean = true,
    val bounds: Bounds = Bounds.EMPTY,
    val node: Any? = null
) {
    /**
     * Convert to ElementInfo for general element operations.
     */
    fun toElementInfo(): ElementInfo = ElementInfo(
        className = "Snackbar",
        text = message,
        bounds = bounds,
        isClickable = actionLabel != null,
        isEnabled = true,
        avid = avid,
        stateDescription = if (actionLabel != null) "with action: $actionLabel" else ""
    )
}

/**
 * Result of a snackbar operation.
 */
data class SnackbarOperationResult(
    val success: Boolean,
    val error: String? = null,
    val action: String? = null
) {
    companion object {
        fun success(action: String? = null) = SnackbarOperationResult(
            success = true,
            action = action
        )

        fun error(message: String) = SnackbarOperationResult(
            success = false,
            error = message
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// Platform Executor Interface
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * Platform-specific executor for snackbar operations.
 *
 * Implementations should:
 * 1. Detect snackbar components on screen
 * 2. Extract snackbar content (message, action label)
 * 3. Dismiss snackbars via swipe or timeout
 * 4. Trigger action buttons
 *
 * ## Snackbar Detection Algorithm
 *
 * ```kotlin
 * fun findSnackbarNode(rootNode: AccessibilityNodeInfo): AccessibilityNodeInfo? {
 *     // Look for nodes with:
 *     // - className: com.google.android.material.snackbar.Snackbar
 *     // - viewId containing "snackbar"
 *     // - paneTitle "Snackbar"
 *     // - Child of CoordinatorLayout at bottom
 * }
 * ```
 *
 * ## Dismissal Algorithm
 *
 * ```kotlin
 * fun dismiss(snackbarNode: AccessibilityNodeInfo): Boolean {
 *     // Option 1: ACTION_DISMISS if supported
 *     // Option 2: Swipe right gesture
 *     // Option 3: Wait for auto-dismiss (if not indefinite)
 * }
 * ```
 */
interface SnackbarExecutor {

    // ═══════════════════════════════════════════════════════════════════════════
    // Snackbar Discovery
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Find a snackbar by its AVID fingerprint.
     *
     * @param avid The AVID fingerprint (format: SNK:{hash8})
     * @return SnackbarInfo if found, null otherwise
     */
    suspend fun findSnackbarByAvid(avid: String): SnackbarInfo?

    /**
     * Find the currently visible snackbar.
     *
     * Searches for any snackbar currently displayed on screen.
     *
     * @return SnackbarInfo if a snackbar is visible, null otherwise
     */
    suspend fun findVisibleSnackbar(): SnackbarInfo?

    /**
     * Check if any snackbar is currently visible.
     *
     * @return true if a snackbar is on screen, false otherwise
     */
    suspend fun isSnackbarVisible(): Boolean

    // ═══════════════════════════════════════════════════════════════════════════
    // Snackbar Actions
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Dismiss the snackbar.
     *
     * Attempts to dismiss by:
     * 1. ACTION_DISMISS accessibility action
     * 2. Swipe gesture (right or down)
     * 3. Clicking outside (if supported)
     *
     * @param snackbar The snackbar to dismiss
     * @return Operation result
     */
    suspend fun dismissSnackbar(snackbar: SnackbarInfo): SnackbarOperationResult

    /**
     * Trigger the snackbar action button.
     *
     * @param snackbar The snackbar with the action
     * @return Operation result
     */
    suspend fun triggerAction(snackbar: SnackbarInfo): SnackbarOperationResult

    // ═══════════════════════════════════════════════════════════════════════════
    // Content Extraction
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Get the message text from a snackbar.
     *
     * @param snackbar The snackbar to read
     * @return Message text
     */
    suspend fun getMessage(snackbar: SnackbarInfo): String

    /**
     * Get the action label from a snackbar.
     *
     * @param snackbar The snackbar to read
     * @return Action label, or null if no action
     */
    suspend fun getActionLabel(snackbar: SnackbarInfo): String?

    /**
     * Get the remaining display time for a snackbar.
     *
     * @param snackbar The snackbar to check
     * @return Remaining time in milliseconds, or null if indefinite
     */
    suspend fun getRemainingTime(snackbar: SnackbarInfo): Long?
}
