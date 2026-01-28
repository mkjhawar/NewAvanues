/**
 * TextInputPlugin.kt - Text Input Handler as Universal Plugin
 *
 * Copyright (C) Manoj Jhawar, Intelligent Devices LLC
 * Created: 2026-01-22
 *
 * Universal Plugin implementation for text input and keyboard actions.
 * Migrated from VoiceOSCore's InputHandler to the Universal Plugin Architecture.
 *
 * Migration from: Modules/VoiceOSCore/src/commonMain/.../InputHandler.kt
 */
package com.augmentalis.magiccode.plugins.builtin

import com.augmentalis.magiccode.plugins.sdk.BasePlugin
import com.augmentalis.magiccode.plugins.universal.*
import com.augmentalis.magiccode.plugins.universal.contracts.voiceoscore.*
import com.augmentalis.commandmanager.ActionResult
import com.augmentalis.commandmanager.QuantizedCommand

/**
 * Text Input Handler Plugin - Handles text input and keyboard actions.
 *
 * This plugin processes text input commands including:
 * - Text input: type, enter text, input
 * - Deletion: delete, backspace, clear text
 * - Selection: select all
 * - Clipboard: copy, cut, paste
 * - Undo/Redo
 * - Search: search, find
 *
 * ## Migration Notes
 * This plugin wraps the original InputHandler logic from VoiceOSCore,
 * adapting it to the Universal Plugin interface while maintaining identical
 * behavior including input validation and security checks.
 *
 * ## Security Features
 * - Input length validation (max 10,000 characters)
 * - Dangerous pattern detection (XSS, SQL injection)
 * - Safe handling of special characters
 *
 * ## Usage
 * ```kotlin
 * val plugin = TextInputPlugin { executor }
 * plugin.initialize(config, context)
 *
 * val command = QuantizedCommand(phrase = "type hello world", ...)
 * if (plugin.canHandle(command, handlerContext)) {
 *     val result = plugin.handle(command, handlerContext)
 * }
 * ```
 *
 * @param executorProvider Provider function that returns the platform-specific executor.
 *        Using a provider allows lazy initialization when platform services may not be
 *        available at plugin creation time.
 *
 * @since 1.0.0
 * @see HandlerPlugin
 * @see BasePlugin
 * @see TextInputExecutor
 */
class TextInputPlugin(
    private val executorProvider: () -> TextInputExecutor
) : BasePlugin(), HandlerPlugin {

    // =========================================================================
    // Identity
    // =========================================================================

    override val pluginId: String = PLUGIN_ID
    override val pluginName: String = "Text Input Handler"
    override val version: String = "1.0.0"

    override val capabilities: Set<PluginCapability> = setOf(
        PluginCapability(
            id = PluginCapability.ACCESSIBILITY_HANDLER,
            name = "Text Input Handler",
            version = "1.0.0",
            interfaces = setOf("HandlerPlugin"),
            metadata = mapOf(
                "handlerType" to "TEXT_INPUT",
                "supportsType" to "true",
                "supportsDelete" to "true",
                "supportsClipboard" to "true",
                "supportsUndoRedo" to "true",
                "supportsSearch" to "true",
                "maxInputLength" to MAX_INPUT_LENGTH.toString()
            )
        )
    )

    // =========================================================================
    // Handler Properties
    // =========================================================================

    override val handlerType: HandlerType = HandlerType.TEXT_INPUT

    override val patterns: List<CommandPattern> = listOf(
        // Text input patterns
        CommandPattern(
            regex = Regex("^(type|enter text|input)\\s+(.+)$", RegexOption.IGNORE_CASE),
            intent = "TYPE",
            requiredEntities = setOf("text"),
            examples = listOf("type hello", "enter text password123", "input search query")
        ),
        // Deletion patterns
        CommandPattern(
            regex = Regex("^(delete|backspace)$", RegexOption.IGNORE_CASE),
            intent = "DELETE",
            requiredEntities = emptySet(),
            examples = listOf("delete", "backspace")
        ),
        CommandPattern(
            regex = Regex("^(clear text|clear all|clear)$", RegexOption.IGNORE_CASE),
            intent = "CLEAR",
            requiredEntities = emptySet(),
            examples = listOf("clear text", "clear all", "clear")
        ),
        // Selection pattern
        CommandPattern(
            regex = Regex("^select all$", RegexOption.IGNORE_CASE),
            intent = "SELECT_ALL",
            requiredEntities = emptySet(),
            examples = listOf("select all")
        ),
        // Clipboard patterns
        CommandPattern(
            regex = Regex("^copy$", RegexOption.IGNORE_CASE),
            intent = "COPY",
            requiredEntities = emptySet(),
            examples = listOf("copy")
        ),
        CommandPattern(
            regex = Regex("^cut$", RegexOption.IGNORE_CASE),
            intent = "CUT",
            requiredEntities = emptySet(),
            examples = listOf("cut")
        ),
        CommandPattern(
            regex = Regex("^paste$", RegexOption.IGNORE_CASE),
            intent = "PASTE",
            requiredEntities = emptySet(),
            examples = listOf("paste")
        ),
        // Undo/Redo patterns
        CommandPattern(
            regex = Regex("^undo$", RegexOption.IGNORE_CASE),
            intent = "UNDO",
            requiredEntities = emptySet(),
            examples = listOf("undo")
        ),
        CommandPattern(
            regex = Regex("^redo$", RegexOption.IGNORE_CASE),
            intent = "REDO",
            requiredEntities = emptySet(),
            examples = listOf("redo")
        ),
        // Search patterns
        CommandPattern(
            regex = Regex("^(search|find)\\s+(.+)$", RegexOption.IGNORE_CASE),
            intent = "SEARCH",
            requiredEntities = setOf("query"),
            examples = listOf("search hello", "find settings", "search for results")
        )
    )

    // =========================================================================
    // Supported Actions (for discovery)
    // =========================================================================

    /**
     * List of supported action phrases.
     * Used for command discovery and help systems.
     */
    val supportedActions: List<String> = listOf(
        "type", "enter text", "input",
        "delete", "backspace", "clear text", "clear all", "clear",
        "select all",
        "copy", "cut", "paste",
        "undo", "redo",
        "search", "find"
    )

    // =========================================================================
    // Executor Reference
    // =========================================================================

    private lateinit var executor: TextInputExecutor

    // =========================================================================
    // Lifecycle
    // =========================================================================

    override suspend fun onInitialize(): InitResult {
        return try {
            executor = executorProvider()
            InitResult.success("TextInputPlugin initialized")
        } catch (e: Exception) {
            InitResult.failure(e, recoverable = true)
        }
    }

    override suspend fun onShutdown() {
        // No resources to release
    }

    override fun getHealthDiagnostics(): Map<String, String> = mapOf(
        "supportedActions" to supportedActions.size.toString(),
        "patterns" to patterns.size.toString(),
        "executorInitialized" to (::executor.isInitialized).toString(),
        "maxInputLength" to MAX_INPUT_LENGTH.toString()
    )

    // =========================================================================
    // Handler Implementation
    // =========================================================================

    override fun canHandle(command: QuantizedCommand, context: HandlerContext): Boolean {
        val phrase = command.phrase.lowercase().trim()

        // Check pattern matches
        if (patterns.any { it.matches(phrase) }) {
            return true
        }

        // Check exact actions (for single-word commands)
        if (phrase in listOf("delete", "backspace", "copy", "cut", "paste", "undo", "redo", "clear")) {
            return true
        }

        // Check prefix matches
        if (phrase.startsWith("type ") ||
            phrase.startsWith("enter text ") ||
            phrase.startsWith("input ") ||
            phrase.startsWith("search ") ||
            phrase.startsWith("find ")
        ) {
            return true
        }

        return phrase == "select all" || phrase == "clear text" || phrase == "clear all"
    }

    override suspend fun handle(
        command: QuantizedCommand,
        context: HandlerContext
    ): ActionResult {
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
                val validationError = validateInput(text)
                if (validationError != null) {
                    return ActionResult.Error(validationError)
                }

                if (executor.enterText(text)) {
                    ActionResult.Success("Entered text")
                } else {
                    ActionResult.Error("Could not enter text - no focused input field")
                }
            }

            // Deletion - single character
            normalizedAction == "delete" || normalizedAction == "backspace" -> {
                if (executor.deleteCharacter()) {
                    ActionResult.Success("Deleted character")
                } else {
                    ActionResult.Error("Could not delete - no focused input field")
                }
            }

            // Clear all text
            normalizedAction == "clear text" ||
            normalizedAction == "clear all" ||
            normalizedAction == "clear" -> {
                if (executor.clearText()) {
                    ActionResult.Success("Cleared text")
                } else {
                    ActionResult.Error("Could not clear - no focused input field")
                }
            }

            // Selection
            normalizedAction == "select all" -> {
                if (executor.selectAll()) {
                    ActionResult.Success("Selected all text")
                } else {
                    ActionResult.Error("Could not select all")
                }
            }

            // Clipboard operations
            normalizedAction == "copy" -> {
                if (executor.copy()) {
                    ActionResult.Success("Copied to clipboard")
                } else {
                    ActionResult.Error("Could not copy")
                }
            }

            normalizedAction == "cut" -> {
                if (executor.cut()) {
                    ActionResult.Success("Cut to clipboard")
                } else {
                    ActionResult.Error("Could not cut")
                }
            }

            normalizedAction == "paste" -> {
                if (executor.paste()) {
                    ActionResult.Success("Pasted from clipboard")
                } else {
                    ActionResult.Error("Could not paste")
                }
            }

            // Undo/Redo
            normalizedAction == "undo" -> {
                if (executor.undo()) {
                    ActionResult.Success("Undone")
                } else {
                    ActionResult.Error("Cannot undo")
                }
            }

            normalizedAction == "redo" -> {
                if (executor.redo()) {
                    ActionResult.Success("Redone")
                } else {
                    ActionResult.Error("Cannot redo")
                }
            }

            // Search
            normalizedAction.startsWith("search ") ||
            normalizedAction.startsWith("find ") -> {
                val query = normalizedAction
                    .removePrefix("search ")
                    .removePrefix("find ")
                    .trim()

                if (query.isEmpty()) {
                    ActionResult.Error("Search query cannot be empty")
                } else if (executor.search(query)) {
                    ActionResult.Success("Searching for: $query")
                } else {
                    ActionResult.Error("Could not search - no search field found")
                }
            }

            else -> ActionResult.Error("Unknown text input action: $normalizedAction")
        }
    }

    override fun getConfidence(command: QuantizedCommand, context: HandlerContext): Float {
        val phrase = command.phrase.lowercase().trim()

        // Exact match with single-word actions
        if (phrase in listOf("delete", "backspace", "copy", "cut", "paste", "undo", "redo", "clear")) {
            return 1.0f
        }

        // Exact match with multi-word actions
        if (phrase in listOf("select all", "clear text", "clear all")) {
            return 1.0f
        }

        // Pattern match for parameterized commands
        for (pattern in patterns) {
            if (pattern.matches(phrase)) {
                return 0.95f
            }
        }

        // Partial match (starts with text input verb)
        if (phrase.startsWith("type") ||
            phrase.startsWith("enter text") ||
            phrase.startsWith("input") ||
            phrase.startsWith("search") ||
            phrase.startsWith("find")
        ) {
            return 0.7f
        }

        return 0.0f
    }

    // =========================================================================
    // Input Validation
    // =========================================================================

    /**
     * Validate input text for security and constraints.
     *
     * Performs the following validations:
     * - Length check (max 10,000 characters)
     * - Dangerous pattern detection (XSS, SQL injection)
     *
     * @param text The text to validate
     * @return Error message if validation fails, null if valid
     */
    private fun validateInput(text: String): String? {
        // Check length
        if (text.length > MAX_INPUT_LENGTH) {
            return "Input too long (max $MAX_INPUT_LENGTH characters)"
        }

        // Check for empty text
        if (text.isEmpty()) {
            return "Input text cannot be empty"
        }

        // Check for potentially dangerous patterns
        if (containsDangerousPattern(text)) {
            return "Input contains potentially dangerous content"
        }

        return null
    }

    /**
     * Check for dangerous patterns (XSS, SQL injection, etc.)
     *
     * This helps prevent injection attacks when the text might be
     * used in contexts like web views or database queries.
     *
     * @param text The text to check
     * @return true if dangerous patterns are detected
     */
    private fun containsDangerousPattern(text: String): Boolean {
        val dangerousPatterns = listOf(
            "<script",
            "javascript:",
            "onclick=",
            "onerror=",
            "onload=",
            "onmouseover=",
            "onfocus=",
            "'; DROP TABLE",
            "'; DELETE FROM",
            "1=1",
            "UNION SELECT",
            "UNION ALL SELECT",
            "--",
            "/*",
            "*/"
        )
        val lowerText = text.lowercase()
        return dangerousPatterns.any { lowerText.contains(it.lowercase()) }
    }

    companion object {
        /** Plugin ID for registration and discovery */
        const val PLUGIN_ID = "com.augmentalis.commandmanager.handler.textinput"

        /** Maximum allowed input length for security */
        private const val MAX_INPUT_LENGTH = 10000
    }
}

/**
 * Platform-specific executor interface for text input actions.
 *
 * This interface defines the operations that must be implemented by
 * platform-specific code to perform actual text input operations.
 *
 * Implementations are platform-specific:
 * - Android: Uses AccessibilityService and InputConnection
 * - iOS: Uses UIAccessibility and UITextInput APIs
 * - Desktop: Uses native input APIs or simulated keyboard input
 *
 * @since 1.0.0
 */
interface TextInputExecutor {
    /**
     * Enter text at the current cursor position.
     *
     * The text is inserted at the current cursor position in the
     * focused input field. If there is selected text, it will be
     * replaced.
     *
     * @param text The text to enter
     * @return true if text was entered successfully, false if no input field is focused
     */
    suspend fun enterText(text: String): Boolean

    /**
     * Delete a single character before the cursor (backspace).
     *
     * @return true if a character was deleted, false if no input field is focused
     *         or cursor is at the beginning
     */
    suspend fun deleteCharacter(): Boolean

    /**
     * Clear all text in the current input field.
     *
     * @return true if text was cleared, false if no input field is focused
     */
    suspend fun clearText(): Boolean

    /**
     * Select all text in the current input field.
     *
     * @return true if text was selected, false if no input field is focused
     */
    suspend fun selectAll(): Boolean

    /**
     * Copy selected text to clipboard.
     *
     * @return true if text was copied, false if no text is selected
     */
    suspend fun copy(): Boolean

    /**
     * Cut selected text to clipboard (copy and delete).
     *
     * @return true if text was cut, false if no text is selected
     */
    suspend fun cut(): Boolean

    /**
     * Paste text from clipboard at current cursor position.
     *
     * @return true if text was pasted, false if clipboard is empty or
     *         no input field is focused
     */
    suspend fun paste(): Boolean

    /**
     * Undo the last text operation.
     *
     * @return true if undo was performed, false if nothing to undo
     */
    suspend fun undo(): Boolean

    /**
     * Redo the last undone operation.
     *
     * @return true if redo was performed, false if nothing to redo
     */
    suspend fun redo(): Boolean

    /**
     * Search for text using the platform's search functionality.
     *
     * This may open a search field, trigger in-app search, or
     * perform a system-wide search depending on the context.
     *
     * @param query The search query
     * @return true if search was initiated, false if search is not available
     */
    suspend fun search(query: String): Boolean
}

// =============================================================================
// Factory Functions
// =============================================================================

/**
 * Create a TextInputPlugin with a pre-configured executor.
 *
 * @param executor The text input executor implementation
 * @return Configured TextInputPlugin
 */
fun createTextInputPlugin(
    executor: TextInputExecutor
): TextInputPlugin {
    return TextInputPlugin { executor }
}

/**
 * Create a TextInputPlugin with a lazy executor provider.
 *
 * Useful when the executor depends on platform services that may
 * not be available at plugin creation time.
 *
 * @param executorProvider Function that returns the executor when needed
 * @return Configured TextInputPlugin
 */
fun createTextInputPlugin(
    executorProvider: () -> TextInputExecutor
): TextInputPlugin {
    return TextInputPlugin(executorProvider)
}

// =============================================================================
// Testing Support
// =============================================================================

/**
 * Mock executor for testing TextInputPlugin.
 *
 * Records all actions and can be configured to succeed or fail.
 * Useful for unit testing command handling without platform dependencies.
 *
 * ## Usage
 * ```kotlin
 * val mockExecutor = MockTextInputExecutor(shouldSucceed = true)
 * val plugin = createTextInputPlugin(mockExecutor)
 *
 * // Execute command
 * plugin.handle(command, context)
 *
 * // Verify actions
 * assertEquals(listOf("enterText:hello"), mockExecutor.actions)
 * ```
 *
 * @param shouldSucceed Whether operations should succeed or fail
 */
class MockTextInputExecutor(
    private val shouldSucceed: Boolean = true
) : TextInputExecutor {

    private val _actions = mutableListOf<String>()

    /** List of recorded actions with parameters */
    val actions: List<String> get() = _actions.toList()

    /** The last text that was entered */
    var lastEnteredText: String? = null
        private set

    /** The last search query */
    var lastSearchQuery: String? = null
        private set

    /** Simulated clipboard content */
    var clipboardContent: String = ""

    /** Clear recorded actions */
    fun clearActions() {
        _actions.clear()
        lastEnteredText = null
        lastSearchQuery = null
    }

    override suspend fun enterText(text: String): Boolean {
        _actions.add("enterText:$text")
        lastEnteredText = text
        return shouldSucceed
    }

    override suspend fun deleteCharacter(): Boolean {
        _actions.add("deleteCharacter")
        return shouldSucceed
    }

    override suspend fun clearText(): Boolean {
        _actions.add("clearText")
        return shouldSucceed
    }

    override suspend fun selectAll(): Boolean {
        _actions.add("selectAll")
        return shouldSucceed
    }

    override suspend fun copy(): Boolean {
        _actions.add("copy")
        return shouldSucceed
    }

    override suspend fun cut(): Boolean {
        _actions.add("cut")
        return shouldSucceed
    }

    override suspend fun paste(): Boolean {
        _actions.add("paste")
        return shouldSucceed
    }

    override suspend fun undo(): Boolean {
        _actions.add("undo")
        return shouldSucceed
    }

    override suspend fun redo(): Boolean {
        _actions.add("redo")
        return shouldSucceed
    }

    override suspend fun search(query: String): Boolean {
        _actions.add("search:$query")
        lastSearchQuery = query
        return shouldSucceed
    }
}

/**
 * Mock executor that simulates a real text input field.
 *
 * Maintains state like current text, cursor position, and selection.
 * Useful for more realistic integration testing.
 *
 * @param initialText Initial text in the simulated input field
 */
class StatefulMockTextInputExecutor(
    initialText: String = ""
) : TextInputExecutor {

    /** Current text in the input field */
    var currentText: String = initialText
        private set

    /** Current cursor position */
    var cursorPosition: Int = initialText.length
        private set

    /** Selection start (-1 if no selection) */
    var selectionStart: Int = -1
        private set

    /** Selection end (-1 if no selection) */
    var selectionEnd: Int = -1
        private set

    /** Clipboard content */
    var clipboard: String = ""
        private set

    /** Undo stack */
    private val undoStack = mutableListOf<String>()

    /** Redo stack */
    private val redoStack = mutableListOf<String>()

    /** Whether an input field is "focused" */
    var isFocused: Boolean = true

    /** Check if there is a selection */
    val hasSelection: Boolean
        get() = selectionStart >= 0 && selectionEnd >= 0 && selectionStart != selectionEnd

    override suspend fun enterText(text: String): Boolean {
        if (!isFocused) return false

        saveForUndo()

        if (hasSelection) {
            // Replace selection
            currentText = currentText.substring(0, selectionStart) +
                    text +
                    currentText.substring(selectionEnd)
            cursorPosition = selectionStart + text.length
            clearSelection()
        } else {
            // Insert at cursor
            currentText = currentText.substring(0, cursorPosition) +
                    text +
                    currentText.substring(cursorPosition)
            cursorPosition += text.length
        }

        return true
    }

    override suspend fun deleteCharacter(): Boolean {
        if (!isFocused) return false
        if (cursorPosition <= 0 && !hasSelection) return false

        saveForUndo()

        if (hasSelection) {
            currentText = currentText.substring(0, selectionStart) +
                    currentText.substring(selectionEnd)
            cursorPosition = selectionStart
            clearSelection()
        } else {
            currentText = currentText.substring(0, cursorPosition - 1) +
                    currentText.substring(cursorPosition)
            cursorPosition--
        }

        return true
    }

    override suspend fun clearText(): Boolean {
        if (!isFocused) return false

        saveForUndo()
        currentText = ""
        cursorPosition = 0
        clearSelection()

        return true
    }

    override suspend fun selectAll(): Boolean {
        if (!isFocused) return false

        selectionStart = 0
        selectionEnd = currentText.length

        return true
    }

    override suspend fun copy(): Boolean {
        if (!hasSelection) return false

        clipboard = currentText.substring(selectionStart, selectionEnd)

        return true
    }

    override suspend fun cut(): Boolean {
        if (!hasSelection) return false

        saveForUndo()
        clipboard = currentText.substring(selectionStart, selectionEnd)
        currentText = currentText.substring(0, selectionStart) +
                currentText.substring(selectionEnd)
        cursorPosition = selectionStart
        clearSelection()

        return true
    }

    override suspend fun paste(): Boolean {
        if (!isFocused || clipboard.isEmpty()) return false

        return enterText(clipboard)
    }

    override suspend fun undo(): Boolean {
        if (undoStack.isEmpty()) return false

        redoStack.add(currentText)
        currentText = undoStack.removeLast()
        cursorPosition = currentText.length
        clearSelection()

        return true
    }

    override suspend fun redo(): Boolean {
        if (redoStack.isEmpty()) return false

        undoStack.add(currentText)
        currentText = redoStack.removeLast()
        cursorPosition = currentText.length
        clearSelection()

        return true
    }

    override suspend fun search(query: String): Boolean {
        // Simulate search - just return true if focused
        return isFocused
    }

    private fun saveForUndo() {
        undoStack.add(currentText)
        redoStack.clear()
    }

    private fun clearSelection() {
        selectionStart = -1
        selectionEnd = -1
    }

    /** Reset the executor to initial state */
    fun reset(text: String = "") {
        currentText = text
        cursorPosition = text.length
        clearSelection()
        clipboard = ""
        undoStack.clear()
        redoStack.clear()
        isFocused = true
    }
}
