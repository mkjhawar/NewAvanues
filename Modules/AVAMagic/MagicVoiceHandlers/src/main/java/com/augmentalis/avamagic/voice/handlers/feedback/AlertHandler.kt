/**
 * AlertHandler.kt - Voice handler for Alert Dialog interactions
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2026-01-28
 *
 * Purpose: Voice-driven control for alert dialogs
 * Features:
 * - Dismiss/close alert dialogs
 * - Acknowledge/confirm alert messages
 * - Read alert content aloud via TTS
 * - AVID-based targeting for precise element selection
 * - Voice feedback for alert actions
 *
 * Location: MagicVoiceHandlers module - feedback category
 *
 * ## Supported Commands
 *
 * Dismissal:
 * - "dismiss" - Dismiss the current alert
 * - "close alert" - Close the alert dialog
 * - "close" - Close the alert
 *
 * Acknowledgment:
 * - "ok" - Acknowledge/confirm the alert
 * - "acknowledge" - Confirm alert message
 * - "confirm" - Confirm the alert
 *
 * Reading:
 * - "read alert" - Read the alert content aloud
 * - "what does it say" - Read the alert message
 * - "read message" - Read alert text content
 *
 * ## Alert Detection
 *
 * Supports detection of:
 * - AlertDialog (Android native)
 * - MaterialAlertDialog
 * - Custom alert implementations with role="alert"
 * - Dialog components with alert semantics
 */

package com.augmentalis.avamagic.voice.handlers.feedback

import android.util.Log
import com.augmentalis.voiceoscore.ActionCategory
import com.augmentalis.voiceoscore.BaseHandler
import com.augmentalis.voiceoscore.Bounds
import com.augmentalis.voiceoscore.ElementInfo
import com.augmentalis.voiceoscore.HandlerResult
import com.augmentalis.voiceoscore.QuantizedCommand

/**
 * Voice command handler for Alert Dialog interactions.
 *
 * Provides comprehensive voice control for alert dialogs including:
 * - Dismissal commands (dismiss, close)
 * - Acknowledgment commands (ok, acknowledge)
 * - Content reading via TTS
 * - Automatic alert detection and targeting
 *
 * Thread Safety:
 * - All operations are suspend functions
 * - State modifications are atomic via executor
 *
 * @param executor Platform-specific executor for alert operations
 */
class AlertHandler(
    private val executor: AlertExecutor
) : BaseHandler() {

    companion object {
        private const val TAG = "AlertHandler"

        // Command patterns for alert actions
        private val DISMISS_COMMANDS = setOf(
            "dismiss", "close alert", "close", "dismiss alert"
        )

        private val ACKNOWLEDGE_COMMANDS = setOf(
            "ok", "okay", "acknowledge", "confirm", "got it", "understood"
        )

        private val READ_COMMANDS = setOf(
            "read alert", "what does it say", "read message",
            "read it", "what is it", "tell me"
        )
    }

    override val category: ActionCategory = ActionCategory.UI

    override val supportedActions: List<String> = listOf(
        // Dismiss actions
        "dismiss", "close alert", "close", "dismiss alert",
        // Acknowledge actions
        "ok", "okay", "acknowledge", "confirm", "got it", "understood",
        // Read actions
        "read alert", "what does it say", "read message",
        "read it", "what is it", "tell me"
    )

    /**
     * Callback for voice feedback when alert content is read.
     */
    var onAlertRead: ((title: String?, message: String) -> Unit)? = null

    /**
     * Callback for voice feedback when alert is dismissed.
     */
    var onAlertDismissed: ((alertType: String) -> Unit)? = null

    override suspend fun execute(
        command: QuantizedCommand,
        params: Map<String, Any>
    ): HandlerResult {
        val normalizedAction = command.phrase.lowercase().trim()

        Log.d(TAG, "Processing alert command: $normalizedAction")

        return try {
            when {
                // Dismiss commands
                DISMISS_COMMANDS.any { normalizedAction.contains(it) } -> {
                    handleDismiss(command)
                }

                // Acknowledge commands
                ACKNOWLEDGE_COMMANDS.any { normalizedAction == it } -> {
                    handleAcknowledge(command)
                }

                // Read commands
                READ_COMMANDS.any { normalizedAction.contains(it) } -> {
                    handleReadAlert(command)
                }

                else -> HandlerResult.notHandled()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error executing alert command", e)
            HandlerResult.failure("Error: ${e.message}", recoverable = true)
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Command Handlers
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Handle dismiss/close alert commands.
     */
    private suspend fun handleDismiss(command: QuantizedCommand): HandlerResult {
        // Find the current alert
        val alertInfo = findAlert(avid = command.targetAvid)
            ?: return HandlerResult.Failure(
                reason = "No alert dialog found",
                recoverable = true,
                suggestedAction = "There doesn't appear to be an alert on screen"
            )

        // Dismiss the alert
        val result = executor.dismissAlert(alertInfo)

        return if (result.success) {
            onAlertDismissed?.invoke(alertInfo.alertType.name)

            Log.i(TAG, "Alert dismissed: ${alertInfo.title ?: "Untitled"}")

            HandlerResult.Success(
                message = "Alert dismissed",
                data = mapOf(
                    "alertAvid" to alertInfo.avid,
                    "alertTitle" to (alertInfo.title ?: ""),
                    "alertType" to alertInfo.alertType.name,
                    "accessibility_announcement" to "Alert dismissed"
                )
            )
        } else {
            HandlerResult.failure(
                reason = result.error ?: "Could not dismiss alert",
                recoverable = true
            )
        }
    }

    /**
     * Handle acknowledge/ok commands.
     */
    private suspend fun handleAcknowledge(command: QuantizedCommand): HandlerResult {
        // Find the current alert
        val alertInfo = findAlert(avid = command.targetAvid)
            ?: return HandlerResult.Failure(
                reason = "No alert dialog found",
                recoverable = true,
                suggestedAction = "There doesn't appear to be an alert on screen"
            )

        // Acknowledge the alert (click OK button or positive action)
        val result = executor.acknowledgeAlert(alertInfo)

        return if (result.success) {
            onAlertDismissed?.invoke(alertInfo.alertType.name)

            Log.i(TAG, "Alert acknowledged: ${alertInfo.title ?: "Untitled"}")

            HandlerResult.Success(
                message = "Alert acknowledged",
                data = mapOf(
                    "alertAvid" to alertInfo.avid,
                    "alertTitle" to (alertInfo.title ?: ""),
                    "action" to "acknowledge",
                    "accessibility_announcement" to "Alert acknowledged"
                )
            )
        } else {
            HandlerResult.failure(
                reason = result.error ?: "Could not acknowledge alert",
                recoverable = true
            )
        }
    }

    /**
     * Handle read alert content commands.
     */
    private suspend fun handleReadAlert(command: QuantizedCommand): HandlerResult {
        // Find the current alert
        val alertInfo = findAlert(avid = command.targetAvid)
            ?: return HandlerResult.Failure(
                reason = "No alert dialog found",
                recoverable = true,
                suggestedAction = "There doesn't appear to be an alert on screen"
            )

        // Get the alert content
        val content = executor.getAlertContent(alertInfo)

        // Build the message to read
        val messageToRead = buildString {
            if (!alertInfo.title.isNullOrBlank()) {
                append("Alert: ")
                append(alertInfo.title)
                append(". ")
            }
            if (!content.message.isNullOrBlank()) {
                append(content.message)
            }
            if (content.buttons.isNotEmpty()) {
                append(". Available actions: ")
                append(content.buttons.joinToString(", "))
            }
        }

        onAlertRead?.invoke(alertInfo.title, content.message ?: "")

        Log.i(TAG, "Reading alert content: $messageToRead")

        return HandlerResult.Success(
            message = messageToRead,
            data = mapOf(
                "alertAvid" to alertInfo.avid,
                "alertTitle" to (alertInfo.title ?: ""),
                "alertMessage" to (content.message ?: ""),
                "alertButtons" to content.buttons,
                "shouldSpeak" to true,
                "accessibility_announcement" to messageToRead
            )
        )
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Helper Methods
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Find alert by AVID or detect current alert on screen.
     */
    private suspend fun findAlert(avid: String? = null): AlertInfo? {
        // Priority 1: AVID lookup (fastest, most precise)
        if (avid != null) {
            val alert = executor.findAlertByAvid(avid)
            if (alert != null) return alert
        }

        // Priority 2: Find any visible alert
        return executor.findVisibleAlert()
    }

}

// ═══════════════════════════════════════════════════════════════════════════════
// Supporting Types
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * Types of alert dialogs.
 */
enum class AlertType {
    /** Standard information alert */
    INFO,
    /** Warning alert */
    WARNING,
    /** Error alert */
    ERROR,
    /** Success/confirmation alert */
    SUCCESS,
    /** Custom or unknown alert type */
    CUSTOM
}

/**
 * Information about an alert dialog component.
 *
 * @property avid AVID fingerprint for the alert (format: ALT:{hash8})
 * @property title Alert title text
 * @property message Alert message/body text
 * @property alertType Type of alert (info, warning, error, etc.)
 * @property isDismissable Whether the alert can be dismissed
 * @property bounds Screen bounds for the alert
 * @property node Platform-specific node reference
 */
data class AlertInfo(
    val avid: String,
    val title: String? = null,
    val message: String? = null,
    val alertType: AlertType = AlertType.INFO,
    val isDismissable: Boolean = true,
    val bounds: Bounds = Bounds.EMPTY,
    val node: Any? = null
) {
    /**
     * Convert to ElementInfo for general element operations.
     */
    fun toElementInfo(): ElementInfo = ElementInfo(
        className = "AlertDialog",
        text = title ?: message ?: "",
        bounds = bounds,
        isClickable = true,
        isEnabled = true,
        avid = avid,
        stateDescription = alertType.name
    )
}

/**
 * Content extracted from an alert dialog.
 *
 * @property title Alert title
 * @property message Alert body message
 * @property buttons List of button labels
 * @property icon Icon resource identifier if present
 */
data class AlertContent(
    val title: String? = null,
    val message: String? = null,
    val buttons: List<String> = emptyList(),
    val icon: String? = null
)

/**
 * Result of an alert operation.
 */
data class AlertOperationResult(
    val success: Boolean,
    val error: String? = null,
    val action: String? = null
) {
    companion object {
        fun success(action: String? = null) = AlertOperationResult(
            success = true,
            action = action
        )

        fun error(message: String) = AlertOperationResult(
            success = false,
            error = message
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// Platform Executor Interface
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * Platform-specific executor for alert dialog operations.
 *
 * Implementations should:
 * 1. Detect alert dialogs on screen
 * 2. Extract alert content (title, message, buttons)
 * 3. Dismiss alerts via accessibility actions
 * 4. Handle various alert implementations
 *
 * ## Alert Detection Algorithm
 *
 * ```kotlin
 * fun findAlertNode(rootNode: AccessibilityNodeInfo): AccessibilityNodeInfo? {
 *     // Look for nodes with:
 *     // - className: android.app.AlertDialog
 *     // - className: androidx.appcompat.app.AlertDialog
 *     // - paneTitle containing "alert"
 *     // - role="alertdialog" (custom implementations)
 * }
 * ```
 *
 * ## Content Extraction Algorithm
 *
 * ```kotlin
 * fun extractContent(alertNode: AccessibilityNodeInfo): AlertContent {
 *     // Find title: First TextView child or node with "title" in viewId
 *     // Find message: TextView with "message" in viewId or second text child
 *     // Find buttons: Nodes with className Button or role="button"
 * }
 * ```
 */
interface AlertExecutor {

    // ═══════════════════════════════════════════════════════════════════════════
    // Alert Discovery
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Find an alert dialog by its AVID fingerprint.
     *
     * @param avid The AVID fingerprint (format: ALT:{hash8})
     * @return AlertInfo if found, null otherwise
     */
    suspend fun findAlertByAvid(avid: String): AlertInfo?

    /**
     * Find the currently visible alert dialog.
     *
     * Searches for any alert dialog currently displayed on screen.
     *
     * @return AlertInfo if an alert is visible, null otherwise
     */
    suspend fun findVisibleAlert(): AlertInfo?

    /**
     * Check if any alert is currently visible.
     *
     * @return true if an alert is on screen, false otherwise
     */
    suspend fun isAlertVisible(): Boolean

    // ═══════════════════════════════════════════════════════════════════════════
    // Alert Actions
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Dismiss the alert dialog.
     *
     * Attempts to dismiss by:
     * 1. Clicking the dismiss/cancel button
     * 2. Clicking outside the dialog (if dismissable)
     * 3. Pressing back button
     *
     * @param alert The alert to dismiss
     * @return Operation result
     */
    suspend fun dismissAlert(alert: AlertInfo): AlertOperationResult

    /**
     * Acknowledge the alert (click OK/positive button).
     *
     * @param alert The alert to acknowledge
     * @return Operation result
     */
    suspend fun acknowledgeAlert(alert: AlertInfo): AlertOperationResult

    /**
     * Click a specific button on the alert.
     *
     * @param alert The alert dialog
     * @param buttonText Text of the button to click
     * @return Operation result
     */
    suspend fun clickButton(alert: AlertInfo, buttonText: String): AlertOperationResult

    // ═══════════════════════════════════════════════════════════════════════════
    // Content Extraction
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Get the full content of an alert dialog.
     *
     * @param alert The alert to extract content from
     * @return AlertContent with title, message, and button labels
     */
    suspend fun getAlertContent(alert: AlertInfo): AlertContent

    /**
     * Get the list of button labels on the alert.
     *
     * @param alert The alert dialog
     * @return List of button text labels
     */
    suspend fun getAlertButtons(alert: AlertInfo): List<String>
}
