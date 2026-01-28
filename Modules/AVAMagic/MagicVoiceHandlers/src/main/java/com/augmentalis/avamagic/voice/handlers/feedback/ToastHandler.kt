/**
 * ToastHandler.kt - Voice handler for Toast message interactions
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2026-01-28
 *
 * Purpose: Voice-driven control for toast messages
 * Features:
 * - Dismiss dismissable toast messages
 * - Read toast message content aloud
 * - Repeat the last toast message
 * - Track toast history for re-reading
 * - AVID-based targeting for precise element selection
 * - Voice feedback for toast actions
 *
 * Location: MagicVoiceHandlers module - feedback category
 *
 * ## Supported Commands
 *
 * Dismissal:
 * - "dismiss toast" - Dismiss the current toast if dismissable
 * - "close toast" - Close the toast
 * - "hide toast" - Hide the toast message
 *
 * Reading:
 * - "read toast" - Read the current toast message
 * - "what was that" - Read the toast content
 * - "what did it say" - Read toast message
 *
 * Repeat:
 * - "repeat" - Repeat the last toast message
 * - "repeat toast" - Re-read last toast
 * - "say again" - Repeat last toast message
 *
 * ## Toast Detection
 *
 * Supports detection of:
 * - Android Toast (system toasts)
 * - Custom toast implementations
 * - Third-party toast libraries
 */

package com.augmentalis.avamagic.voice.handlers.feedback

import android.util.Log
import com.augmentalis.voiceoscore.ActionCategory
import com.augmentalis.voiceoscore.BaseHandler
import com.augmentalis.voiceoscore.Bounds
import com.augmentalis.voiceoscore.ElementInfo
import com.augmentalis.voiceoscore.HandlerResult
import com.augmentalis.voiceoscore.QuantizedCommand
import java.util.concurrent.ConcurrentLinkedDeque

/**
 * Voice command handler for Toast message interactions.
 *
 * Provides comprehensive voice control for toast messages including:
 * - Dismissal commands (dismiss toast, close toast)
 * - Reading commands (read toast, what was that)
 * - Repeat commands (repeat, say again)
 * - Toast history tracking
 *
 * Thread Safety:
 * - All operations are suspend functions
 * - Toast history is maintained in a thread-safe deque
 * - State modifications are atomic via executor
 *
 * @param executor Platform-specific executor for toast operations
 */
class ToastHandler(
    private val executor: ToastExecutor
) : BaseHandler() {

    companion object {
        private const val TAG = "ToastHandler"

        /** Maximum number of toasts to keep in history */
        private const val MAX_TOAST_HISTORY = 10

        // Command patterns for toast actions
        private val DISMISS_COMMANDS = setOf(
            "dismiss toast", "close toast", "hide toast",
            "dismiss", "hide"
        )

        private val READ_COMMANDS = setOf(
            "read toast", "what was that", "what did it say",
            "read message", "what is it"
        )

        private val REPEAT_COMMANDS = setOf(
            "repeat", "repeat toast", "say again",
            "repeat message", "again"
        )
    }

    override val category: ActionCategory = ActionCategory.UI

    override val supportedActions: List<String> = listOf(
        // Dismiss actions
        "dismiss toast", "close toast", "hide toast",
        "dismiss", "hide",
        // Read actions
        "read toast", "what was that", "what did it say",
        "read message", "what is it",
        // Repeat actions
        "repeat", "repeat toast", "say again",
        "repeat message", "again"
    )

    /**
     * Thread-safe toast history (most recent first).
     */
    private val toastHistory = ConcurrentLinkedDeque<ToastInfo>()

    /**
     * Callback for voice feedback when toast message is read.
     */
    var onToastRead: ((message: String) -> Unit)? = null

    /**
     * Callback for voice feedback when toast is dismissed.
     */
    var onToastDismissed: (() -> Unit)? = null

    /**
     * Add a toast to history. Called when a new toast appears.
     */
    fun recordToast(toast: ToastInfo) {
        toastHistory.addFirst(toast)
        // Trim history to max size
        while (toastHistory.size > MAX_TOAST_HISTORY) {
            toastHistory.removeLast()
        }
        Log.d(TAG, "Toast recorded: ${toast.message}")
    }

    /**
     * Get the most recent toast from history.
     */
    fun getLastToast(): ToastInfo? = toastHistory.peekFirst()

    /**
     * Clear toast history.
     */
    fun clearHistory() {
        toastHistory.clear()
    }

    override suspend fun execute(
        command: QuantizedCommand,
        params: Map<String, Any>
    ): HandlerResult {
        val normalizedAction = command.phrase.lowercase().trim()

        Log.d(TAG, "Processing toast command: $normalizedAction")

        return try {
            when {
                // Dismiss commands
                DISMISS_COMMANDS.any { normalizedAction.contains(it) } -> {
                    handleDismiss(command)
                }

                // Read commands
                READ_COMMANDS.any { normalizedAction.contains(it) } -> {
                    handleReadToast(command)
                }

                // Repeat commands
                REPEAT_COMMANDS.any { normalizedAction.contains(it) } -> {
                    handleRepeat(command)
                }

                else -> HandlerResult.notHandled()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error executing toast command", e)
            HandlerResult.failure("Error: ${e.message}", recoverable = true)
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Command Handlers
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Handle dismiss toast commands.
     */
    private suspend fun handleDismiss(command: QuantizedCommand): HandlerResult {
        // Find the current toast
        val toastInfo = findToast(avid = command.targetAvid)
            ?: return HandlerResult.Failure(
                reason = "No toast found",
                recoverable = true,
                suggestedAction = "There doesn't appear to be a toast on screen"
            )

        // Check if toast is dismissable
        if (!toastInfo.isDismissable) {
            return HandlerResult.Failure(
                reason = "Toast cannot be dismissed",
                recoverable = true,
                suggestedAction = "This toast will disappear automatically"
            )
        }

        // Dismiss the toast
        val result = executor.dismissToast(toastInfo)

        return if (result.success) {
            onToastDismissed?.invoke()

            Log.i(TAG, "Toast dismissed")

            HandlerResult.Success(
                message = "Toast dismissed",
                data = mapOf(
                    "toastAvid" to toastInfo.avid,
                    "toastMessage" to toastInfo.message,
                    "accessibility_announcement" to "Toast dismissed"
                )
            )
        } else {
            HandlerResult.failure(
                reason = result.error ?: "Could not dismiss toast",
                recoverable = true
            )
        }
    }

    /**
     * Handle read toast commands.
     */
    private suspend fun handleReadToast(command: QuantizedCommand): HandlerResult {
        // Try to find current visible toast
        var toastInfo = findToast(avid = command.targetAvid)

        // If no visible toast, use the most recent from history
        if (toastInfo == null) {
            toastInfo = getLastToast()
            if (toastInfo == null) {
                return HandlerResult.Failure(
                    reason = "No toast found",
                    recoverable = true,
                    suggestedAction = "There's no recent toast message to read"
                )
            }
        }

        val messageToRead = toastInfo.message

        onToastRead?.invoke(messageToRead)

        Log.i(TAG, "Reading toast content: $messageToRead")

        return HandlerResult.Success(
            message = messageToRead,
            data = mapOf(
                "toastAvid" to toastInfo.avid,
                "toastMessage" to toastInfo.message,
                "shouldSpeak" to true,
                "accessibility_announcement" to messageToRead
            )
        )
    }

    /**
     * Handle repeat toast commands.
     */
    private suspend fun handleRepeat(command: QuantizedCommand): HandlerResult {
        // Get the most recent toast from history
        val lastToast = getLastToast()
            ?: return HandlerResult.Failure(
                reason = "No toast to repeat",
                recoverable = true,
                suggestedAction = "There's no recent toast message to repeat"
            )

        val messageToRead = lastToast.message

        onToastRead?.invoke(messageToRead)

        Log.i(TAG, "Repeating toast: $messageToRead")

        return HandlerResult.Success(
            message = messageToRead,
            data = mapOf(
                "toastAvid" to lastToast.avid,
                "toastMessage" to lastToast.message,
                "isRepeat" to true,
                "shouldSpeak" to true,
                "accessibility_announcement" to messageToRead
            )
        )
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Helper Methods
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Find toast by AVID or detect current toast on screen.
     */
    private suspend fun findToast(avid: String? = null): ToastInfo? {
        // Priority 1: AVID lookup (fastest, most precise)
        if (avid != null) {
            val toast = executor.findToastByAvid(avid)
            if (toast != null) {
                recordToast(toast)
                return toast
            }
        }

        // Priority 2: Find any visible toast
        val visibleToast = executor.findVisibleToast()
        if (visibleToast != null) {
            recordToast(visibleToast)
        }
        return visibleToast
    }

}

// ═══════════════════════════════════════════════════════════════════════════════
// Supporting Types
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * Duration types for toasts.
 */
enum class ToastDuration {
    /** Short duration (~2000ms) */
    SHORT,
    /** Long duration (~3500ms) */
    LONG,
    /** Custom duration */
    CUSTOM
}

/**
 * Position of toast on screen.
 */
enum class ToastGravity {
    TOP,
    CENTER,
    BOTTOM
}

/**
 * Information about a toast message component.
 *
 * @property avid AVID fingerprint for the toast (format: TST:{hash8})
 * @property message Toast message text
 * @property duration Toast display duration
 * @property gravity Position of toast on screen
 * @property isDismissable Whether the toast can be manually dismissed
 * @property timestamp When the toast appeared (epoch millis)
 * @property bounds Screen bounds for the toast
 * @property node Platform-specific node reference
 */
data class ToastInfo(
    val avid: String,
    val message: String,
    val duration: ToastDuration = ToastDuration.SHORT,
    val gravity: ToastGravity = ToastGravity.BOTTOM,
    val isDismissable: Boolean = false,
    val timestamp: Long = System.currentTimeMillis(),
    val bounds: Bounds = Bounds.EMPTY,
    val node: Any? = null
) {
    /**
     * Convert to ElementInfo for general element operations.
     */
    fun toElementInfo(): ElementInfo = ElementInfo(
        className = "Toast",
        text = message,
        bounds = bounds,
        isClickable = false,
        isEnabled = true,
        avid = avid,
        stateDescription = duration.name
    )
}

/**
 * Result of a toast operation.
 */
data class ToastOperationResult(
    val success: Boolean,
    val error: String? = null
) {
    companion object {
        fun success() = ToastOperationResult(success = true)

        fun error(message: String) = ToastOperationResult(
            success = false,
            error = message
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// Platform Executor Interface
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * Platform-specific executor for toast operations.
 *
 * Implementations should:
 * 1. Detect toast messages on screen
 * 2. Extract toast content
 * 3. Dismiss toasts if supported
 * 4. Track toast appearances for history
 *
 * ## Toast Detection Algorithm
 *
 * ```kotlin
 * fun findToastNode(rootNode: AccessibilityNodeInfo): AccessibilityNodeInfo? {
 *     // Look for nodes with:
 *     // - packageName: android (system toasts)
 *     // - className: android.widget.Toast
 *     // - Window type: TYPE_TOAST
 *     // - paneTitle containing "toast"
 * }
 * ```
 *
 * ## Toast Tracking
 *
 * ```kotlin
 * // In AccessibilityService.onAccessibilityEvent()
 * fun handleToastEvent(event: AccessibilityEvent) {
 *     if (event.eventType == TYPE_NOTIFICATION_STATE_CHANGED) {
 *         if (event.className == "android.widget.Toast") {
 *             val text = event.text.joinToString()
 *             // Record toast in handler
 *         }
 *     }
 * }
 * ```
 */
interface ToastExecutor {

    // ═══════════════════════════════════════════════════════════════════════════
    // Toast Discovery
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Find a toast by its AVID fingerprint.
     *
     * @param avid The AVID fingerprint (format: TST:{hash8})
     * @return ToastInfo if found, null otherwise
     */
    suspend fun findToastByAvid(avid: String): ToastInfo?

    /**
     * Find the currently visible toast.
     *
     * Searches for any toast currently displayed on screen.
     *
     * @return ToastInfo if a toast is visible, null otherwise
     */
    suspend fun findVisibleToast(): ToastInfo?

    /**
     * Check if any toast is currently visible.
     *
     * @return true if a toast is on screen, false otherwise
     */
    suspend fun isToastVisible(): Boolean

    // ═══════════════════════════════════════════════════════════════════════════
    // Toast Actions
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Dismiss the toast if possible.
     *
     * Note: Standard Android toasts cannot be dismissed programmatically.
     * This only works for custom dismissable toast implementations.
     *
     * @param toast The toast to dismiss
     * @return Operation result
     */
    suspend fun dismissToast(toast: ToastInfo): ToastOperationResult

    // ═══════════════════════════════════════════════════════════════════════════
    // Content Extraction
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Get the message text from a toast.
     *
     * @param toast The toast to read
     * @return Message text
     */
    suspend fun getMessage(toast: ToastInfo): String

    /**
     * Get the remaining display time for a toast.
     *
     * @param toast The toast to check
     * @return Remaining time in milliseconds, or null if unknown
     */
    suspend fun getRemainingTime(toast: ToastInfo): Long?

    // ═══════════════════════════════════════════════════════════════════════════
    // Toast Observation
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Register a callback for toast appearances.
     *
     * @param callback Called when a new toast appears
     */
    fun setToastObserver(callback: (ToastInfo) -> Unit)

    /**
     * Unregister the toast observer.
     */
    fun clearToastObserver()
}
