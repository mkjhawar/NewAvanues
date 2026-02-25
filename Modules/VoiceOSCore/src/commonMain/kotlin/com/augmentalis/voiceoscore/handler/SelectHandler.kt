/**
 * SelectHandler.kt - Handles selection and clipboard actions
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Created: 2026-01-06
 *
 * KMP handler for text selection and clipboard operations.
 * Part of Phase 12 handler system.
 */
package com.augmentalis.voiceoscore

/**
 * Selection action types.
 */
enum class SelectionAction {
    /** Select specific text */
    SELECT,
    /** Select all content */
    SELECT_ALL,
    /** Copy selection to clipboard */
    COPY,
    /** Cut selection to clipboard */
    CUT,
    /** Paste from clipboard */
    PASTE,
    /** Clear current selection */
    CLEAR
}

/**
 * Result of a selection operation.
 *
 * @param success Whether the operation succeeded
 * @param action The action that was attempted
 * @param selectedText The text involved in the operation (if applicable)
 * @param error Error message if operation failed
 */
data class SelectionResult(
    val success: Boolean,
    val action: SelectionAction,
    val selectedText: String? = null,
    val error: String? = null
)

/**
 * Interface for platform-specific clipboard operations.
 *
 * Implement this interface to provide clipboard functionality
 * for the target platform (Android, iOS, Desktop).
 */
interface IClipboardProvider {
    /**
     * Copy text to clipboard.
     *
     * @param text The text to copy
     * @return true if successful
     */
    fun copy(text: String): Boolean

    /**
     * Get text from clipboard.
     *
     * @return The clipboard content, or null if empty/unavailable
     */
    fun paste(): String?

    /**
     * Clear clipboard contents.
     *
     * @return true if successful
     */
    fun clear(): Boolean
}

/**
 * Handler for text selection and clipboard operations.
 *
 * Supports voice commands:
 * - "select all" - Select all content
 * - "select X" - Select specific text
 * - "copy" - Copy current selection
 * - "cut" - Cut current selection
 * - "paste" - Paste from clipboard
 * - "clear" / "clear selection" - Clear selection
 *
 * Usage:
 * ```kotlin
 * val clipboardProvider = AndroidClipboardProvider(context)
 * val handler = SelectHandler(clipboardProvider)
 *
 * // Handle voice command
 * val result = handler.handleCommand("select all")
 * if (result.success) {
 *     // Selection successful
 * }
 *
 * // Direct API usage
 * handler.selectText("some text")
 * handler.copy()
 * ```
 *
 * @param clipboardProvider Optional platform-specific clipboard implementation
 */
class SelectHandler(
    private val clipboardProvider: IClipboardProvider? = null
) {
    /** Currently selected text */
    private var selectedText: String? = null

    /** Selection start position (for range-based selection) */
    private var selectionStart: Int = -1

    /** Selection end position (for range-based selection) */
    private var selectionEnd: Int = -1

    /**
     * Handle a voice command for selection operations.
     *
     * @param command The voice command (e.g., "select all", "copy")
     * @return Result of the operation
     */
    fun handleCommand(command: String): SelectionResult {
        val normalizedCommand = command.lowercase().trim()

        return when {
            normalizedCommand == "select all" -> selectAll()
            normalizedCommand == "copy" -> copy()
            normalizedCommand == "cut" -> cut()
            normalizedCommand == "paste" -> paste()
            normalizedCommand == "clear" || normalizedCommand == "clear selection" -> clearSelection()
            normalizedCommand.startsWith("select ") -> selectText(command.substringAfter("select "))
            normalizedCommand == "select" -> selectText("") // Handle "select" with no text
            else -> SelectionResult(
                success = false,
                action = SelectionAction.SELECT,
                error = "Unknown command: $command"
            )
        }
    }

    /**
     * Select all content.
     *
     * Sets selection range to cover all content (0 to MAX_VALUE).
     *
     * @return Result indicating success
     */
    fun selectAll(): SelectionResult {
        selectionStart = 0
        selectionEnd = Int.MAX_VALUE
        return SelectionResult(
            success = true,
            action = SelectionAction.SELECT_ALL
        )
    }

    /**
     * Select specific text.
     *
     * @param text The text to select
     * @return Result with the selected text
     */
    fun selectText(text: String): SelectionResult {
        selectedText = text
        return SelectionResult(
            success = true,
            action = SelectionAction.SELECT,
            selectedText = text
        )
    }

    /**
     * Copy current selection to clipboard.
     *
     * @return Result indicating success or failure
     */
    fun copy(): SelectionResult {
        val text = selectedText
            ?: return SelectionResult(
                success = false,
                action = SelectionAction.COPY,
                error = "Nothing selected"
            )

        clipboardProvider?.copy(text)

        return SelectionResult(
            success = true,
            action = SelectionAction.COPY,
            selectedText = text
        )
    }

    /**
     * Cut current selection to clipboard.
     *
     * Copies text to clipboard and clears selection.
     *
     * @return Result indicating success or failure
     */
    fun cut(): SelectionResult {
        val text = selectedText
            ?: return SelectionResult(
                success = false,
                action = SelectionAction.CUT,
                error = "Nothing selected"
            )

        clipboardProvider?.copy(text)
        selectedText = null

        return SelectionResult(
            success = true,
            action = SelectionAction.CUT,
            selectedText = text
        )
    }

    /**
     * Paste from clipboard.
     *
     * @return Result with pasted text or failure if clipboard empty
     */
    fun paste(): SelectionResult {
        val text = clipboardProvider?.paste()
            ?: return SelectionResult(
                success = false,
                action = SelectionAction.PASTE,
                error = "Clipboard empty or unavailable"
            )

        return SelectionResult(
            success = true,
            action = SelectionAction.PASTE,
            selectedText = text
        )
    }

    /**
     * Clear current selection.
     *
     * Resets all selection state.
     *
     * @return Result indicating success
     */
    fun clearSelection(): SelectionResult {
        selectedText = null
        selectionStart = -1
        selectionEnd = -1

        return SelectionResult(
            success = true,
            action = SelectionAction.CLEAR
        )
    }

    /**
     * Check if there is an active selection.
     *
     * @return true if text is selected or selection range is set
     */
    fun hasSelection(): Boolean = selectedText != null || selectionStart >= 0

    /**
     * Get the currently selected text.
     *
     * @return The selected text, or null if nothing selected
     */
    fun getSelectedText(): String? = selectedText
}
