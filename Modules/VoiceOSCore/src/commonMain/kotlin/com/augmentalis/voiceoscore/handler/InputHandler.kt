/**
 * InputHandler.kt - Handles text input and keyboard actions
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Created: 2026-01-06
 *
 * KMP handler for text input and keyboard actions.
 */
package com.augmentalis.voiceoscore

import com.augmentalis.voiceoscore.QuantizedCommand

/**
 * Handler for text input and keyboard actions.
 *
 * Supports:
 * - Text input: type, enter text, input
 * - Deletion: delete, backspace, clear text
 * - Selection: select all
 * - Clipboard: copy, cut, paste
 * - Undo/Redo
 * - Search: search, find
 */
class InputHandler(
    private val executor: InputExecutor
) : BaseHandler() {

    override val category: ActionCategory = ActionCategory.INPUT

    override val supportedActions: List<String> = listOf(
        "type", "enter text", "input",
        "delete", "backspace", "clear text",
        "select all", "copy", "cut", "paste",
        "undo", "redo",
        "search", "find"
    )

    override suspend fun execute(
        command: QuantizedCommand,
        params: Map<String, Any>
    ): HandlerResult {
        val normalizedAction = command.phrase.lowercase().trim()

        return when {
            // Text input
            normalizedAction.startsWith("type ") ||
            normalizedAction.startsWith("enter text ") ||
            normalizedAction.startsWith("input ") -> {
                val text = normalizedAction
                    .removePrefix("type ")
                    .removePrefix("enter text ")
                    .removePrefix("input ")
                    .trim()

                // Validate input
                val validationResult = validateInput(text)
                if (validationResult != null) {
                    return validationResult
                }

                if (executor.enterText(text)) {
                    HandlerResult.success("Entered text")
                } else {
                    HandlerResult.failure("Could not enter text - no focused input field")
                }
            }

            // Deletion
            normalizedAction == "delete" || normalizedAction == "backspace" -> {
                if (executor.deleteCharacter()) {
                    HandlerResult.success("Deleted character")
                } else {
                    HandlerResult.failure("Could not delete - no focused input field")
                }
            }

            normalizedAction == "clear text" || normalizedAction == "clear all" -> {
                if (executor.clearText()) {
                    HandlerResult.success("Cleared text")
                } else {
                    HandlerResult.failure("Could not clear - no focused input field")
                }
            }

            // Selection
            normalizedAction == "select all" -> {
                if (executor.selectAll()) {
                    HandlerResult.success("Selected all text")
                } else {
                    HandlerResult.failure("Could not select all")
                }
            }

            // Clipboard operations
            normalizedAction == "copy" -> {
                if (executor.copy()) {
                    HandlerResult.success("Copied to clipboard")
                } else {
                    HandlerResult.failure("Could not copy")
                }
            }

            normalizedAction == "cut" -> {
                if (executor.cut()) {
                    HandlerResult.success("Cut to clipboard")
                } else {
                    HandlerResult.failure("Could not cut")
                }
            }

            normalizedAction == "paste" -> {
                if (executor.paste()) {
                    HandlerResult.success("Pasted from clipboard")
                } else {
                    HandlerResult.failure("Could not paste")
                }
            }

            // Undo/Redo
            normalizedAction == "undo" -> {
                if (executor.undo()) {
                    HandlerResult.success("Undone")
                } else {
                    HandlerResult.failure("Cannot undo")
                }
            }

            normalizedAction == "redo" -> {
                if (executor.redo()) {
                    HandlerResult.success("Redone")
                } else {
                    HandlerResult.failure("Cannot redo")
                }
            }

            // Search
            normalizedAction.startsWith("search ") ||
            normalizedAction.startsWith("find ") -> {
                val query = normalizedAction
                    .removePrefix("search ")
                    .removePrefix("find ")
                    .trim()

                if (executor.search(query)) {
                    HandlerResult.success("Searching for: $query")
                } else {
                    HandlerResult.failure("Could not search - no search field found")
                }
            }

            else -> HandlerResult.notHandled()
        }
    }

    /**
     * Validate input text for security.
     *
     * @return Failure result if validation fails, null if valid
     */
    private fun validateInput(text: String): HandlerResult? {
        // Check length
        if (text.length > MAX_INPUT_LENGTH) {
            return HandlerResult.failure("Input too long (max $MAX_INPUT_LENGTH characters)")
        }

        // Check for potentially dangerous patterns
        if (containsDangerousPattern(text)) {
            return HandlerResult.failure("Input contains potentially dangerous content")
        }

        return null
    }

    /**
     * Check for dangerous patterns (XSS, SQL injection, etc.)
     */
    private fun containsDangerousPattern(text: String): Boolean {
        val dangerousPatterns = listOf(
            "<script",
            "javascript:",
            "onclick=",
            "onerror=",
            "'; DROP TABLE",
            "1=1",
            "UNION SELECT"
        )
        val lowerText = text.lowercase()
        return dangerousPatterns.any { lowerText.contains(it.lowercase()) }
    }

    companion object {
        private const val MAX_INPUT_LENGTH = 10000
    }
}

/**
 * Platform-specific executor for input actions.
 */
interface InputExecutor {
    suspend fun enterText(text: String): Boolean
    suspend fun deleteCharacter(): Boolean
    suspend fun clearText(): Boolean
    suspend fun selectAll(): Boolean
    suspend fun copy(): Boolean
    suspend fun cut(): Boolean
    suspend fun paste(): Boolean
    suspend fun undo(): Boolean
    suspend fun redo(): Boolean
    suspend fun search(query: String): Boolean
}
