/**
 * ModalHandler.kt - Voice handler for Modal dialog interactions
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Created: 2026-01-28
 *
 * Purpose: Voice-driven control for modal dialogs
 * Features:
 * - Close/dismiss modal dialogs
 * - Confirm or cancel modal actions
 * - Click specific buttons by number or name
 * - Navigate modal content
 * - AVID-based targeting for precise element selection
 * - Voice feedback for modal actions
 *
 * Location: MagicVoiceHandlers module - feedback category
 *
 * ## Supported Commands
 *
 * Dismissal:
 * - "close modal" - Close the modal dialog
 * - "dismiss" - Dismiss the modal
 * - "close" - Close the modal
 *
 * Confirmation:
 * - "confirm" - Confirm the modal action
 * - "ok" - Click OK/positive button
 * - "yes" - Confirm affirmatively
 *
 * Cancellation:
 * - "cancel" - Cancel the modal action
 * - "no" - Decline the modal
 *
 * Button targeting:
 * - "button [N]" - Click button by number (1-indexed)
 * - "click [button name]" - Click button by label
 * - "press [button name]" - Press specific button
 *
 * ## Modal Detection
 *
 * Supports detection of:
 * - Dialog (Android native)
 * - BottomSheetDialog
 * - Full-screen dialog fragments
 * - Custom modal implementations
 */

package com.augmentalis.avanueui.voice.handlers.feedback

import com.avanues.logging.LoggerFactory
import com.augmentalis.voiceoscore.ActionCategory
import com.augmentalis.voiceoscore.BaseHandler
import com.augmentalis.voiceoscore.Bounds
import com.augmentalis.voiceoscore.ElementInfo
import com.augmentalis.voiceoscore.HandlerResult
import com.augmentalis.voiceoscore.QuantizedCommand

/**
 * Voice command handler for Modal dialog interactions.
 *
 * Provides comprehensive voice control for modal dialogs including:
 * - Dismissal commands (close, dismiss)
 * - Confirmation commands (confirm, ok, yes)
 * - Cancellation commands (cancel, no)
 * - Button targeting by number or name
 * - Modal content navigation
 *
 * Thread Safety:
 * - All operations are suspend functions
 * - State modifications are atomic via executor
 *
 * @param executor Platform-specific executor for modal operations
 */
class ModalHandler(
    private val executor: ModalExecutor
) : BaseHandler() {

    companion object {
        private const val TAG = "ModalHandler"
        private val Log = LoggerFactory.getLogger(TAG)

        // Command patterns for modal actions
        private val CLOSE_COMMANDS = setOf(
            "close modal", "dismiss", "close", "dismiss modal",
            "exit modal", "exit"
        )

        private val CONFIRM_COMMANDS = setOf(
            "confirm", "ok", "okay", "yes", "accept", "agree",
            "proceed", "continue", "submit"
        )

        private val CANCEL_COMMANDS = setOf(
            "cancel", "no", "decline", "reject", "abort", "back"
        )

        // Pattern for "button N" commands
        private val BUTTON_NUMBER_PATTERN = Regex(
            """^button\s+(\d+)$""",
            RegexOption.IGNORE_CASE
        )

        // Pattern for "click [button name]" or "press [button name]"
        private val BUTTON_NAME_PATTERN = Regex(
            """^(?:click|press|tap)\s+(.+)$""",
            RegexOption.IGNORE_CASE
        )
    }

    override val category: ActionCategory = ActionCategory.UI

    override val supportedActions: List<String> = listOf(
        // Close actions
        "close modal", "dismiss", "close", "dismiss modal",
        "exit modal", "exit",
        // Confirm actions
        "confirm", "ok", "okay", "yes", "accept", "agree",
        "proceed", "continue", "submit",
        // Cancel actions
        "cancel", "no", "decline", "reject", "abort", "back",
        // Button targeting
        "button 1", "button 2", "button 3",
        "click", "press", "tap"
    )

    /**
     * Callback for voice feedback when modal is closed.
     */
    var onModalClosed: ((modalTitle: String?) -> Unit)? = null

    /**
     * Callback for voice feedback when modal action is taken.
     */
    var onModalAction: ((action: String, buttonLabel: String?) -> Unit)? = null

    override suspend fun execute(
        command: QuantizedCommand,
        params: Map<String, Any>
    ): HandlerResult {
        val normalizedAction = command.phrase.lowercase().trim()

        Log.d { "Processing modal command: $normalizedAction" }

        return try {
            when {
                // Button number commands: "button 1", "button 2", etc.
                BUTTON_NUMBER_PATTERN.containsMatchIn(normalizedAction) -> {
                    handleButtonByNumber(normalizedAction, command)
                }

                // Button name commands: "click [name]", "press [name]"
                BUTTON_NAME_PATTERN.containsMatchIn(normalizedAction) -> {
                    handleButtonByName(normalizedAction, command)
                }

                // Close/dismiss commands
                CLOSE_COMMANDS.any { normalizedAction.contains(it) } -> {
                    handleClose(command)
                }

                // Confirm commands
                CONFIRM_COMMANDS.any { normalizedAction == it } -> {
                    handleConfirm(command)
                }

                // Cancel commands
                CANCEL_COMMANDS.any { normalizedAction == it } -> {
                    handleCancel(command)
                }

                else -> HandlerResult.notHandled()
            }
        } catch (e: Exception) {
            Log.e({ "Error executing modal command" }, e)
            HandlerResult.failure("Error: ${e.message}", recoverable = true)
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Command Handlers
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Handle close/dismiss modal commands.
     */
    private suspend fun handleClose(command: QuantizedCommand): HandlerResult {
        // Find the current modal
        val modalInfo = findModal(avid = command.targetAvid)
            ?: return HandlerResult.Failure(
                reason = "No modal dialog found",
                recoverable = true,
                suggestedAction = "There doesn't appear to be a modal on screen"
            )

        // Close the modal
        val result = executor.closeModal(modalInfo)

        return if (result.success) {
            onModalClosed?.invoke(modalInfo.title)

            Log.i { "Modal closed: ${modalInfo.title ?: "Untitled"}" }

            HandlerResult.Success(
                message = "Modal closed",
                data = mapOf(
                    "modalAvid" to modalInfo.avid,
                    "modalTitle" to (modalInfo.title ?: ""),
                    "accessibility_announcement" to "Modal closed"
                )
            )
        } else {
            HandlerResult.failure(
                reason = result.error ?: "Could not close modal",
                recoverable = true
            )
        }
    }

    /**
     * Handle confirm/ok/yes commands.
     */
    private suspend fun handleConfirm(command: QuantizedCommand): HandlerResult {
        // Find the current modal
        val modalInfo = findModal(avid = command.targetAvid)
            ?: return HandlerResult.Failure(
                reason = "No modal dialog found",
                recoverable = true,
                suggestedAction = "There doesn't appear to be a modal on screen"
            )

        // Find and click the positive/confirm button
        val result = executor.confirmModal(modalInfo)

        return if (result.success) {
            onModalAction?.invoke("confirm", result.buttonClicked)

            Log.i { "Modal confirmed: ${modalInfo.title ?: "Untitled"}" }

            HandlerResult.Success(
                message = "Confirmed",
                data = mapOf(
                    "modalAvid" to modalInfo.avid,
                    "modalTitle" to (modalInfo.title ?: ""),
                    "action" to "confirm",
                    "buttonClicked" to (result.buttonClicked ?: ""),
                    "accessibility_announcement" to "Confirmed"
                )
            )
        } else {
            HandlerResult.failure(
                reason = result.error ?: "Could not confirm modal",
                recoverable = true
            )
        }
    }

    /**
     * Handle cancel/no commands.
     */
    private suspend fun handleCancel(command: QuantizedCommand): HandlerResult {
        // Find the current modal
        val modalInfo = findModal(avid = command.targetAvid)
            ?: return HandlerResult.Failure(
                reason = "No modal dialog found",
                recoverable = true,
                suggestedAction = "There doesn't appear to be a modal on screen"
            )

        // Find and click the negative/cancel button
        val result = executor.cancelModal(modalInfo)

        return if (result.success) {
            onModalAction?.invoke("cancel", result.buttonClicked)

            Log.i { "Modal cancelled: ${modalInfo.title ?: "Untitled"}" }

            HandlerResult.Success(
                message = "Cancelled",
                data = mapOf(
                    "modalAvid" to modalInfo.avid,
                    "modalTitle" to (modalInfo.title ?: ""),
                    "action" to "cancel",
                    "buttonClicked" to (result.buttonClicked ?: ""),
                    "accessibility_announcement" to "Cancelled"
                )
            )
        } else {
            HandlerResult.failure(
                reason = result.error ?: "Could not cancel modal",
                recoverable = true
            )
        }
    }

    /**
     * Handle "button N" commands.
     */
    private suspend fun handleButtonByNumber(
        normalizedAction: String,
        command: QuantizedCommand
    ): HandlerResult {
        val matchResult = BUTTON_NUMBER_PATTERN.find(normalizedAction)
            ?: return HandlerResult.failure("Could not parse button number")

        val buttonNumber = matchResult.groupValues[1].toIntOrNull()
            ?: return HandlerResult.failure("Invalid button number")

        // Find the current modal
        val modalInfo = findModal(avid = command.targetAvid)
            ?: return HandlerResult.Failure(
                reason = "No modal dialog found",
                recoverable = true,
                suggestedAction = "There doesn't appear to be a modal on screen"
            )

        // Get available buttons
        val buttons = executor.getModalButtons(modalInfo)
        if (buttons.isEmpty()) {
            return HandlerResult.Failure(
                reason = "No buttons found in modal",
                recoverable = true,
                suggestedAction = "This modal has no clickable buttons"
            )
        }

        // Convert to 0-indexed
        val buttonIndex = buttonNumber - 1
        if (buttonIndex < 0 || buttonIndex >= buttons.size) {
            return HandlerResult.Failure(
                reason = "Button $buttonNumber not found. Available buttons: 1 to ${buttons.size}",
                recoverable = true,
                suggestedAction = "Try 'button 1' through 'button ${buttons.size}'"
            )
        }

        val buttonLabel = buttons[buttonIndex]
        val result = executor.clickButton(modalInfo, buttonLabel)

        return if (result.success) {
            onModalAction?.invoke("click", buttonLabel)

            Log.i { "Modal button clicked: $buttonLabel" }

            HandlerResult.Success(
                message = "$buttonLabel clicked",
                data = mapOf(
                    "modalAvid" to modalInfo.avid,
                    "buttonNumber" to buttonNumber,
                    "buttonLabel" to buttonLabel,
                    "accessibility_announcement" to "$buttonLabel clicked"
                )
            )
        } else {
            HandlerResult.failure(
                reason = result.error ?: "Could not click button",
                recoverable = true
            )
        }
    }

    /**
     * Handle "click [button name]" commands.
     */
    private suspend fun handleButtonByName(
        normalizedAction: String,
        command: QuantizedCommand
    ): HandlerResult {
        val matchResult = BUTTON_NAME_PATTERN.find(normalizedAction)
            ?: return HandlerResult.failure("Could not parse button name")

        val buttonName = matchResult.groupValues[1].trim()

        // Find the current modal
        val modalInfo = findModal(avid = command.targetAvid)
            ?: return HandlerResult.Failure(
                reason = "No modal dialog found",
                recoverable = true,
                suggestedAction = "There doesn't appear to be a modal on screen"
            )

        // Find button by name (fuzzy match)
        val buttons = executor.getModalButtons(modalInfo)
        val matchedButton = buttons.find {
            it.equals(buttonName, ignoreCase = true) ||
            it.contains(buttonName, ignoreCase = true)
        }

        if (matchedButton == null) {
            return HandlerResult.Failure(
                reason = "Button '$buttonName' not found",
                recoverable = true,
                suggestedAction = "Available buttons: ${buttons.joinToString(", ")}"
            )
        }

        val result = executor.clickButton(modalInfo, matchedButton)

        return if (result.success) {
            onModalAction?.invoke("click", matchedButton)

            Log.i { "Modal button clicked: $matchedButton" }

            HandlerResult.Success(
                message = "$matchedButton clicked",
                data = mapOf(
                    "modalAvid" to modalInfo.avid,
                    "buttonLabel" to matchedButton,
                    "accessibility_announcement" to "$matchedButton clicked"
                )
            )
        } else {
            HandlerResult.failure(
                reason = result.error ?: "Could not click button",
                recoverable = true
            )
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Helper Methods
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Find modal by AVID or detect current modal on screen.
     */
    private suspend fun findModal(avid: String? = null): ModalInfo? {
        // Priority 1: AVID lookup (fastest, most precise)
        if (avid != null) {
            val modal = executor.findModalByAvid(avid)
            if (modal != null) return modal
        }

        // Priority 2: Find any visible modal
        return executor.findVisibleModal()
    }

}

// ═══════════════════════════════════════════════════════════════════════════════
// Supporting Types
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * Types of modal dialogs.
 */
enum class ModalType {
    /** Standard dialog */
    DIALOG,
    /** Bottom sheet dialog */
    BOTTOM_SHEET,
    /** Full-screen dialog */
    FULL_SCREEN,
    /** Alert-style dialog */
    ALERT,
    /** Custom modal implementation */
    CUSTOM
}

/**
 * Information about a modal dialog component.
 *
 * @property avid AVID fingerprint for the modal (format: MDL:{hash8})
 * @property title Modal title text
 * @property content Modal body content
 * @property modalType Type of modal dialog
 * @property isDismissable Whether the modal can be dismissed by clicking outside
 * @property isCancelable Whether the modal can be cancelled
 * @property bounds Screen bounds for the modal
 * @property node Platform-specific node reference
 */
data class ModalInfo(
    val avid: String,
    val title: String? = null,
    val content: String? = null,
    val modalType: ModalType = ModalType.DIALOG,
    val isDismissable: Boolean = true,
    val isCancelable: Boolean = true,
    val bounds: Bounds = Bounds.EMPTY,
    val node: Any? = null
) {
    /**
     * Convert to ElementInfo for general element operations.
     */
    fun toElementInfo(): ElementInfo = ElementInfo(
        className = "Dialog",
        text = title ?: content ?: "",
        bounds = bounds,
        isClickable = true,
        isEnabled = true,
        avid = avid,
        stateDescription = modalType.name
    )
}

/**
 * Result of a modal operation.
 */
data class ModalOperationResult(
    val success: Boolean,
    val error: String? = null,
    val action: String? = null,
    val buttonClicked: String? = null
) {
    companion object {
        fun success(action: String? = null, buttonClicked: String? = null) = ModalOperationResult(
            success = true,
            action = action,
            buttonClicked = buttonClicked
        )

        fun error(message: String) = ModalOperationResult(
            success = false,
            error = message
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// Platform Executor Interface
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * Platform-specific executor for modal dialog operations.
 *
 * Implementations should:
 * 1. Detect modal dialogs on screen
 * 2. Extract modal content (title, body, buttons)
 * 3. Close/dismiss modals via accessibility actions
 * 4. Handle button interactions
 *
 * ## Modal Detection Algorithm
 *
 * ```kotlin
 * fun findModalNode(rootNode: AccessibilityNodeInfo): AccessibilityNodeInfo? {
 *     // Look for nodes with:
 *     // - className containing "Dialog"
 *     // - paneTitle present
 *     // - Window type TYPE_APPLICATION_OVERLAY
 *     // - role="dialog" in custom implementations
 * }
 * ```
 *
 * ## Button Discovery Algorithm
 *
 * ```kotlin
 * fun findButtons(modalNode: AccessibilityNodeInfo): List<String> {
 *     // Find child nodes with:
 *     // - className: android.widget.Button
 *     // - isClickable = true
 *     // - Text content present
 * }
 * ```
 */
interface ModalExecutor {

    // ═══════════════════════════════════════════════════════════════════════════
    // Modal Discovery
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Find a modal dialog by its AVID fingerprint.
     *
     * @param avid The AVID fingerprint (format: MDL:{hash8})
     * @return ModalInfo if found, null otherwise
     */
    suspend fun findModalByAvid(avid: String): ModalInfo?

    /**
     * Find the currently visible modal dialog.
     *
     * Searches for any modal dialog currently displayed on screen.
     *
     * @return ModalInfo if a modal is visible, null otherwise
     */
    suspend fun findVisibleModal(): ModalInfo?

    /**
     * Check if any modal is currently visible.
     *
     * @return true if a modal is on screen, false otherwise
     */
    suspend fun isModalVisible(): Boolean

    /**
     * Get all visible modals (in case of stacked modals).
     *
     * @return List of visible modals, ordered from front to back
     */
    suspend fun getAllVisibleModals(): List<ModalInfo>

    // ═══════════════════════════════════════════════════════════════════════════
    // Modal Actions
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Close/dismiss the modal dialog.
     *
     * Attempts to close by:
     * 1. Clicking the X/close button if present
     * 2. Clicking outside the modal (if dismissable)
     * 3. Pressing back button
     *
     * @param modal The modal to close
     * @return Operation result
     */
    suspend fun closeModal(modal: ModalInfo): ModalOperationResult

    /**
     * Confirm the modal (click positive/OK button).
     *
     * @param modal The modal to confirm
     * @return Operation result with the button that was clicked
     */
    suspend fun confirmModal(modal: ModalInfo): ModalOperationResult

    /**
     * Cancel the modal (click negative/Cancel button).
     *
     * @param modal The modal to cancel
     * @return Operation result with the button that was clicked
     */
    suspend fun cancelModal(modal: ModalInfo): ModalOperationResult

    /**
     * Click a specific button by its label.
     *
     * @param modal The modal dialog
     * @param buttonLabel Text of the button to click
     * @return Operation result
     */
    suspend fun clickButton(modal: ModalInfo, buttonLabel: String): ModalOperationResult

    // ═══════════════════════════════════════════════════════════════════════════
    // Content Extraction
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Get the list of button labels on the modal.
     *
     * @param modal The modal dialog
     * @return List of button text labels, in visual order
     */
    suspend fun getModalButtons(modal: ModalInfo): List<String>

    /**
     * Get the modal title.
     *
     * @param modal The modal to read
     * @return Title text, or null if no title
     */
    suspend fun getModalTitle(modal: ModalInfo): String?

    /**
     * Get the modal content/body text.
     *
     * @param modal The modal to read
     * @return Content text
     */
    suspend fun getModalContent(modal: ModalInfo): String?
}
