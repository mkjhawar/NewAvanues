/**
 * DialogHandler.kt - Handles confirmation dialog interactions via voice
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2026-01-27
 *
 * Voice handler for confirmation dialogs and modal interactions.
 * Supports yes/no, cancel/ok, button selection, and custom button text.
 *
 * ## Supported Commands
 *
 * Confirmation:
 * - "yes", "confirm", "ok", "accept", "proceed", "agree"
 *
 * Cancellation:
 * - "no", "cancel", "dismiss", "decline", "reject", "deny"
 *
 * Dismissal (neutral close):
 * - "close", "close dialog", "exit dialog"
 *
 * Button Selection:
 * - "button 1", "button 2", "option 1", "option 2"
 * - Custom button text matching (e.g., "save", "delete", "retry")
 *
 * ## Dialog Detection
 *
 * Detects active dialogs via accessibility service by checking:
 * 1. Window type (TYPE_APPLICATION, isDialog flag)
 * 2. Class name patterns (AlertDialog, DialogFragment, BottomSheetDialog)
 * 3. Container AVID type code (DLG)
 *
 * ## Button Mapping Strategy
 *
 * 1. AVID-based: If dialog buttons have AVIDs, use direct AVID targeting
 * 2. Text matching: Map common responses to standard button labels:
 *    - "yes" -> "OK", "Yes", "Confirm", "Accept", positive buttons
 *    - "no" -> "Cancel", "No", "Dismiss", negative buttons
 * 3. Numbered: "button 1" clicks the first button, "button 2" the second
 * 4. Custom: Match spoken text to any button's label
 */
package com.augmentalis.avamagic.voice.handlers.feedback

import com.augmentalis.voiceoscore.ActionCategory
import com.augmentalis.voiceoscore.BaseHandler
import com.augmentalis.voiceoscore.ElementInfo
import com.augmentalis.voiceoscore.HandlerResult
import com.augmentalis.voiceoscore.QuantizedCommand

/**
 * Handler for dialog confirmation interactions.
 *
 * Provides voice control for confirmation dialogs, alert dialogs,
 * bottom sheets, and other modal UI components.
 *
 * @param executor Platform-specific executor for dialog actions
 */
class DialogHandler(
    private val executor: DialogExecutor
) : BaseHandler() {

    override val category: ActionCategory = ActionCategory.UI

    override val supportedActions: List<String> = listOf(
        // Confirmation commands
        "yes", "confirm", "ok", "okay", "accept", "proceed", "agree",
        // Cancellation commands
        "no", "cancel", "dismiss", "decline", "reject", "deny",
        // Neutral close
        "close", "close dialog", "exit dialog",
        // Numbered button selection
        "button 1", "button 2", "button 3",
        "option 1", "option 2", "option 3",
        // Custom button text (handled dynamically)
        "button [text]", "[button label]"
    )

    companion object {
        private const val TAG = "DialogHandler"

        // Positive response mappings (voice phrase -> possible button labels)
        private val POSITIVE_RESPONSES = setOf(
            "yes", "confirm", "ok", "okay", "accept", "proceed", "agree", "sure", "alright"
        )

        // Negative response mappings
        private val NEGATIVE_RESPONSES = setOf(
            "no", "cancel", "dismiss", "decline", "reject", "deny", "never", "not now"
        )

        // Neutral close commands
        private val CLOSE_COMMANDS = setOf(
            "close", "close dialog", "exit dialog", "close window"
        )

        // Common positive button labels (for matching)
        private val POSITIVE_BUTTON_LABELS = setOf(
            "ok", "okay", "yes", "confirm", "accept", "proceed", "continue",
            "save", "submit", "done", "apply", "agree", "allow", "enable",
            "got it", "understood", "i agree", "accept all"
        )

        // Common negative button labels (for matching)
        private val NEGATIVE_BUTTON_LABELS = setOf(
            "cancel", "no", "dismiss", "decline", "reject", "deny", "close",
            "not now", "skip", "later", "never", "no thanks", "don't allow",
            "disable", "reject all", "decline all"
        )

        // Button number extraction regex
        private val BUTTON_NUMBER_REGEX = Regex("^(?:button|option)\\s+(\\d+)$", RegexOption.IGNORE_CASE)
    }

    override suspend fun execute(
        command: QuantizedCommand,
        params: Map<String, Any>
    ): HandlerResult {
        val normalizedAction = command.phrase.lowercase().trim()

        // First, check if a dialog is currently active
        val dialogInfo = executor.getActiveDialog()
        if (dialogInfo == null) {
            return HandlerResult.Failure(
                reason = "No dialog detected",
                recoverable = false,
                suggestedAction = "Wait for a dialog to appear or say a different command"
            )
        }

        // Get dialog buttons
        val buttons = executor.getDialogButtons(dialogInfo)
        if (buttons.isEmpty()) {
            return HandlerResult.Failure(
                reason = "No buttons found in dialog",
                recoverable = false,
                suggestedAction = "Try 'close dialog' or 'go back'"
            )
        }

        return when {
            // Handle positive responses (yes, ok, confirm, etc.)
            normalizedAction in POSITIVE_RESPONSES -> {
                handlePositiveResponse(dialogInfo, buttons)
            }

            // Handle negative responses (no, cancel, dismiss, etc.)
            normalizedAction in NEGATIVE_RESPONSES -> {
                handleNegativeResponse(dialogInfo, buttons)
            }

            // Handle close commands (close dialog, exit dialog)
            normalizedAction in CLOSE_COMMANDS -> {
                handleCloseDialog(dialogInfo, buttons)
            }

            // Handle numbered button selection (button 1, option 2)
            BUTTON_NUMBER_REGEX.matches(normalizedAction) -> {
                handleNumberedButton(normalizedAction, dialogInfo, buttons)
            }

            // Handle custom button text matching
            else -> {
                handleCustomButtonText(normalizedAction, dialogInfo, buttons)
            }
        }
    }

    /**
     * Handle positive response commands (yes, ok, confirm, etc.)
     * Maps to the most likely positive/affirmative button in the dialog.
     */
    private suspend fun handlePositiveResponse(
        dialogInfo: DialogInfo,
        buttons: List<ElementInfo>
    ): HandlerResult {
        // Strategy: Find positive button by AVID type, then by label match
        val positiveButton = findPositiveButton(buttons)

        return if (positiveButton != null) {
            if (executor.clickButton(positiveButton)) {
                HandlerResult.Success(
                    message = "Confirmed: ${positiveButton.voiceLabel}",
                    data = mapOf(
                        "action" to "confirm",
                        "button_clicked" to positiveButton.voiceLabel,
                        "dialog_title" to (dialogInfo.title ?: "Dialog")
                    )
                )
            } else {
                HandlerResult.Failure(
                    reason = "Could not click confirmation button",
                    recoverable = true,
                    suggestedAction = "Try 'button 1' or say the button text directly"
                )
            }
        } else {
            // Fallback: click first button (usually positive in Android dialogs)
            val firstButton = buttons.first()
            if (executor.clickButton(firstButton)) {
                HandlerResult.Success(
                    message = "Clicked: ${firstButton.voiceLabel}",
                    data = mapOf(
                        "action" to "confirm_fallback",
                        "button_clicked" to firstButton.voiceLabel
                    )
                )
            } else {
                HandlerResult.Failure(
                    reason = "No positive button found",
                    recoverable = true,
                    suggestedAction = "Available buttons: ${buttons.joinToString { it.voiceLabel }}"
                )
            }
        }
    }

    /**
     * Handle negative response commands (no, cancel, dismiss, etc.)
     * Maps to the most likely negative/cancel button in the dialog.
     */
    private suspend fun handleNegativeResponse(
        dialogInfo: DialogInfo,
        buttons: List<ElementInfo>
    ): HandlerResult {
        val negativeButton = findNegativeButton(buttons)

        return if (negativeButton != null) {
            if (executor.clickButton(negativeButton)) {
                HandlerResult.Success(
                    message = "Cancelled: ${negativeButton.voiceLabel}",
                    data = mapOf(
                        "action" to "cancel",
                        "button_clicked" to negativeButton.voiceLabel,
                        "dialog_title" to (dialogInfo.title ?: "Dialog")
                    )
                )
            } else {
                HandlerResult.Failure(
                    reason = "Could not click cancel button",
                    recoverable = true,
                    suggestedAction = "Try 'button 2' or say the button text directly"
                )
            }
        } else {
            // Fallback: try to dismiss dialog without button
            if (executor.dismissDialog(dialogInfo)) {
                HandlerResult.Success(
                    message = "Dialog dismissed",
                    data = mapOf("action" to "dismiss_fallback")
                )
            } else {
                HandlerResult.Failure(
                    reason = "No cancel button found",
                    recoverable = true,
                    suggestedAction = "Available buttons: ${buttons.joinToString { it.voiceLabel }}"
                )
            }
        }
    }

    /**
     * Handle neutral close commands (close dialog, exit dialog)
     * Dismisses without necessarily clicking a button.
     */
    private suspend fun handleCloseDialog(
        dialogInfo: DialogInfo,
        buttons: List<ElementInfo>
    ): HandlerResult {
        // First try to find an explicit close/dismiss button
        val closeButton = buttons.find { button ->
            val label = button.voiceLabel.lowercase()
            label in setOf("close", "dismiss", "x", "exit")
        }

        return if (closeButton != null) {
            if (executor.clickButton(closeButton)) {
                HandlerResult.Success(
                    message = "Dialog closed",
                    data = mapOf(
                        "action" to "close",
                        "button_clicked" to closeButton.voiceLabel
                    )
                )
            } else {
                HandlerResult.failure("Could not click close button")
            }
        } else {
            // Try system dismiss (back button behavior)
            if (executor.dismissDialog(dialogInfo)) {
                HandlerResult.Success(
                    message = "Dialog closed",
                    data = mapOf("action" to "dismiss_system")
                )
            } else {
                HandlerResult.Failure(
                    reason = "Could not close dialog",
                    recoverable = true,
                    suggestedAction = "Try 'go back' or click a button directly"
                )
            }
        }
    }

    /**
     * Handle numbered button selection (button 1, option 2, etc.)
     */
    private suspend fun handleNumberedButton(
        command: String,
        dialogInfo: DialogInfo,
        buttons: List<ElementInfo>
    ): HandlerResult {
        val matchResult = BUTTON_NUMBER_REGEX.find(command)
        val buttonNumber = matchResult?.groupValues?.get(1)?.toIntOrNull() ?: 1

        // Convert to 0-based index
        val buttonIndex = buttonNumber - 1

        return if (buttonIndex in buttons.indices) {
            val button = buttons[buttonIndex]
            if (executor.clickButton(button)) {
                HandlerResult.Success(
                    message = "Clicked button $buttonNumber: ${button.voiceLabel}",
                    data = mapOf(
                        "action" to "numbered_selection",
                        "button_number" to buttonNumber,
                        "button_clicked" to button.voiceLabel
                    )
                )
            } else {
                HandlerResult.Failure(
                    reason = "Could not click button $buttonNumber",
                    recoverable = true
                )
            }
        } else {
            HandlerResult.Failure(
                reason = "Button $buttonNumber not found. Dialog has ${buttons.size} button(s).",
                recoverable = true,
                suggestedAction = "Say 'button 1' through 'button ${buttons.size}'"
            )
        }
    }

    /**
     * Handle custom button text matching.
     * Matches the spoken text against button labels.
     */
    private suspend fun handleCustomButtonText(
        command: String,
        dialogInfo: DialogInfo,
        buttons: List<ElementInfo>
    ): HandlerResult {
        // Try exact match first
        var matchedButton = buttons.find { button ->
            button.voiceLabel.lowercase() == command
        }

        // Try contains match
        if (matchedButton == null) {
            matchedButton = buttons.find { button ->
                button.voiceLabel.lowercase().contains(command) ||
                command.contains(button.voiceLabel.lowercase())
            }
        }

        // Try fuzzy match (words overlap)
        if (matchedButton == null) {
            val commandWords = command.split("\\s+".toRegex()).toSet()
            matchedButton = buttons.maxByOrNull { button ->
                val buttonWords = button.voiceLabel.lowercase().split("\\s+".toRegex()).toSet()
                (commandWords intersect buttonWords).size
            }?.takeIf { button ->
                val buttonWords = button.voiceLabel.lowercase().split("\\s+".toRegex()).toSet()
                (commandWords intersect buttonWords).isNotEmpty()
            }
        }

        return if (matchedButton != null) {
            if (executor.clickButton(matchedButton)) {
                HandlerResult.Success(
                    message = "Clicked: ${matchedButton.voiceLabel}",
                    data = mapOf(
                        "action" to "custom_text",
                        "spoken" to command,
                        "button_clicked" to matchedButton.voiceLabel
                    )
                )
            } else {
                HandlerResult.failure("Could not click '${matchedButton.voiceLabel}'")
            }
        } else {
            HandlerResult.Failure(
                reason = "No button matching '$command' found",
                recoverable = true,
                suggestedAction = "Available buttons: ${buttons.joinToString { "'${it.voiceLabel}'" }}"
            )
        }
    }

    /**
     * Find the positive/affirmative button in the dialog.
     * Uses AVID typing and label matching.
     */
    private fun findPositiveButton(buttons: List<ElementInfo>): ElementInfo? {
        // Strategy 1: Find by typical positive button labels
        return buttons.find { button ->
            val label = button.voiceLabel.lowercase()
            POSITIVE_BUTTON_LABELS.any { positiveLabel ->
                label == positiveLabel || label.contains(positiveLabel)
            }
        }
        // Strategy 2: In Android dialogs, positive button is often the last one
        // (right-most in ButtonBar), but we prefer label matching
            ?: buttons.lastOrNull()
    }

    /**
     * Find the negative/cancel button in the dialog.
     * Uses AVID typing and label matching.
     */
    private fun findNegativeButton(buttons: List<ElementInfo>): ElementInfo? {
        // Strategy 1: Find by typical negative button labels
        return buttons.find { button ->
            val label = button.voiceLabel.lowercase()
            NEGATIVE_BUTTON_LABELS.any { negativeLabel ->
                label == negativeLabel || label.contains(negativeLabel)
            }
        }
        // Strategy 2: In Android dialogs, negative button is often the first one
        // (left-most in ButtonBar), but we prefer label matching
            ?: if (buttons.size > 1) buttons.first() else null
    }

}

// ═══════════════════════════════════════════════════════════════════════════════
// Data Classes
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * Information about an active dialog.
 *
 * @property avid Dialog's AVID fingerprint (format: DLG:{hash8})
 * @property title Dialog title text (if available)
 * @property message Dialog message/body text (if available)
 * @property className Dialog class name for type identification
 * @property isModal Whether the dialog blocks interaction with content behind it
 * @property isCancellable Whether the dialog can be dismissed by tapping outside
 * @property node Platform-specific node reference
 */
data class DialogInfo(
    val avid: String?,
    val title: String?,
    val message: String?,
    val className: String,
    val isModal: Boolean = true,
    val isCancellable: Boolean = true,
    val node: Any? = null
)

// ═══════════════════════════════════════════════════════════════════════════════
// Executor Interface
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * Platform-specific executor for dialog actions.
 *
 * Implementations should:
 * 1. Detect active dialogs via accessibility service
 * 2. Extract button information from dialog nodes
 * 3. Execute click actions on dialog buttons
 * 4. Support dialog dismissal (back button behavior)
 *
 * ## Dialog Detection Algorithm
 *
 * ```kotlin
 * fun getActiveDialog(): DialogInfo? {
 *     // 1. Get all windows from accessibility service
 *     // 2. Find window with TYPE_APPLICATION and isDialog flag
 *     // 3. Or find node with className containing "Dialog"
 *     // 4. Extract title, message, and button information
 * }
 * ```
 *
 * ## Button Detection Algorithm
 *
 * ```kotlin
 * fun getDialogButtons(dialog: DialogInfo): List<ElementInfo> {
 *     // 1. Find ButtonBar or button container in dialog
 *     // 2. Collect all clickable Button elements
 *     // 3. Sort by position (left-to-right for horizontal, top-to-bottom for vertical)
 *     // 4. Return with AVID fingerprints for tracking
 * }
 * ```
 */
interface DialogExecutor {

    /**
     * Get the currently active dialog, if any.
     *
     * Checks accessibility service for modal dialogs including:
     * - AlertDialog
     * - DialogFragment
     * - BottomSheetDialog
     * - DatePickerDialog, TimePickerDialog
     * - Custom dialogs with dialog window flags
     *
     * @return DialogInfo if a dialog is active, null otherwise
     */
    suspend fun getActiveDialog(): DialogInfo?

    /**
     * Get all buttons in the dialog.
     *
     * Buttons are returned in visual order (left-to-right, top-to-bottom).
     * This order is used for numbered selection ("button 1", "button 2").
     *
     * @param dialog The dialog to get buttons from
     * @return List of button elements, empty if no buttons found
     */
    suspend fun getDialogButtons(dialog: DialogInfo): List<ElementInfo>

    /**
     * Click a button in the dialog.
     *
     * Uses the element's AVID for reliable targeting if available,
     * falling back to bounds-based clicking.
     *
     * @param button The button element to click
     * @return true if click was performed successfully
     */
    suspend fun clickButton(button: ElementInfo): Boolean

    /**
     * Click a button by its AVID fingerprint.
     *
     * @param avid The button's AVID (format: BTN:{hash8})
     * @return true if click was performed successfully
     */
    suspend fun clickButtonByAvid(avid: String): Boolean

    /**
     * Click a button by its label text.
     *
     * Searches for a button with matching text (case-insensitive).
     *
     * @param label The button label to search for
     * @return true if button was found and clicked
     */
    suspend fun clickButtonByLabel(label: String): Boolean

    /**
     * Dismiss the dialog without clicking a button.
     *
     * Equivalent to pressing the back button or tapping outside
     * the dialog (if cancellable).
     *
     * @param dialog The dialog to dismiss
     * @return true if dialog was dismissed
     */
    suspend fun dismissDialog(dialog: DialogInfo): Boolean

    /**
     * Check if a dialog is currently showing.
     *
     * Quick check without full dialog info retrieval.
     *
     * @return true if any dialog is active
     */
    suspend fun isDialogActive(): Boolean

    /**
     * Get dialog title text.
     *
     * @param dialog The dialog to get title from
     * @return Title text, or null if no title
     */
    suspend fun getDialogTitle(dialog: DialogInfo): String?

    /**
     * Get dialog message/body text.
     *
     * @param dialog The dialog to get message from
     * @return Message text, or null if no message
     */
    suspend fun getDialogMessage(dialog: DialogInfo): String?
}

// ═══════════════════════════════════════════════════════════════════════════════
// Extension Properties
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * Get a voice-friendly label for this element.
 *
 * Priority:
 * 1. text (button label)
 * 2. contentDescription
 * 3. resourceId (cleaned up)
 * 4. "Button" fallback
 */
val ElementInfo.voiceLabel: String
    get() = when {
        text.isNotBlank() -> text
        contentDescription.isNotBlank() -> contentDescription
        resourceId.isNotBlank() -> resourceId
            .substringAfterLast("/")
            .replace("_", " ")
            .replaceFirstChar { it.uppercaseChar() }
        else -> "Button"
    }
