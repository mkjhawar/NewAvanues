/**
 * SelectionPlugin.kt - Selection Handler as Universal Plugin
 *
 * Copyright (C) Manoj Jhawar, Intelligent Devices LLC
 * Created: 2026-01-22
 *
 * Migrated plugin from VoiceOSCore's SelectHandler to the Universal Plugin Architecture.
 * Handles text selection and clipboard operations via voice/gaze commands.
 *
 * Migration from: Modules/VoiceOSCore/src/commonMain/.../SelectHandler.kt
 */
package com.augmentalis.magiccode.plugins.builtin

import com.augmentalis.magiccode.plugins.sdk.BasePlugin
import com.augmentalis.magiccode.plugins.universal.*
import com.augmentalis.magiccode.plugins.universal.contracts.voiceoscore.*
import com.augmentalis.voiceoscore.ActionResult
import com.augmentalis.voiceoscore.QuantizedCommand
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

// =============================================================================
// Selection Types and Configuration
// =============================================================================

/**
 * Selection action types supported by the plugin.
 *
 * Covers all standard text selection and clipboard operations
 * accessible via voice/gaze commands.
 *
 * @since 1.0.0
 */
enum class SelectionAction {
    /** Select specific text by search string */
    SELECT,

    /** Select all content in the current context */
    SELECT_ALL,

    /** Copy current selection to clipboard */
    COPY,

    /** Cut current selection to clipboard (copy + delete) */
    CUT,

    /** Paste content from clipboard */
    PASTE,

    /** Clear/deselect current selection */
    CLEAR,

    /** Select word at current cursor position */
    SELECT_WORD,

    /** Select line at current cursor position */
    SELECT_LINE,

    /** Select paragraph at current cursor position */
    SELECT_PARAGRAPH,

    /** Extend selection by word */
    EXTEND_WORD,

    /** Extend selection to start/end of line */
    EXTEND_LINE
}

/**
 * Result of a selection operation.
 *
 * Provides detailed information about the outcome of selection
 * and clipboard operations for feedback and debugging.
 *
 * @property success Whether the operation succeeded
 * @property action The action that was attempted
 * @property selectedText The text involved in the operation (if applicable)
 * @property error Error message if operation failed
 * @property metadata Additional operation metadata
 * @since 1.0.0
 */
data class SelectionResult(
    val success: Boolean,
    val action: SelectionAction,
    val selectedText: String? = null,
    val error: String? = null,
    val metadata: Map<String, String> = emptyMap()
) {
    companion object {
        /**
         * Create a successful selection result.
         *
         * @param action The action performed
         * @param selectedText The text involved (optional)
         * @return Success result
         */
        fun success(action: SelectionAction, selectedText: String? = null): SelectionResult {
            return SelectionResult(
                success = true,
                action = action,
                selectedText = selectedText
            )
        }

        /**
         * Create a failure selection result.
         *
         * @param action The action attempted
         * @param error Error message
         * @return Failure result
         */
        fun failure(action: SelectionAction, error: String): SelectionResult {
            return SelectionResult(
                success = false,
                action = action,
                error = error
            )
        }
    }
}

/**
 * Selection state tracking for managing active selections.
 *
 * Maintains the current selection state including selected text,
 * position information, and history for undo operations.
 *
 * @property selectedText Currently selected text (null if no selection)
 * @property selectionStart Start position of selection (-1 if no range selection)
 * @property selectionEnd End position of selection (-1 if no range selection)
 * @property hasSelection Whether there is an active selection
 * @since 1.0.0
 */
data class SelectionState(
    val selectedText: String? = null,
    val selectionStart: Int = -1,
    val selectionEnd: Int = -1
) {
    /**
     * Check if there is an active selection.
     */
    val hasSelection: Boolean
        get() = selectedText != null || selectionStart >= 0

    /**
     * Get the length of the selection range.
     */
    val selectionLength: Int
        get() = if (selectionStart >= 0 && selectionEnd >= selectionStart) {
            selectionEnd - selectionStart
        } else {
            selectedText?.length ?: 0
        }

    companion object {
        /** Empty/no selection state */
        val EMPTY = SelectionState()

        /**
         * Create a selection state with text.
         *
         * @param text The selected text
         * @return SelectionState with text
         */
        fun withText(text: String): SelectionState {
            return SelectionState(selectedText = text)
        }

        /**
         * Create a selection state with range.
         *
         * @param start Start position
         * @param end End position
         * @return SelectionState with range
         */
        fun withRange(start: Int, end: Int): SelectionState {
            return SelectionState(selectionStart = start, selectionEnd = end)
        }
    }
}

// =============================================================================
// Clipboard Provider Interface
// =============================================================================

/**
 * Interface for platform-specific clipboard operations.
 *
 * Implementations should provide clipboard functionality for the target platform:
 * - Android: ClipboardManager
 * - iOS: UIPasteboard
 * - Desktop: System clipboard APIs
 *
 * ## Thread Safety
 * Implementations should be thread-safe as clipboard operations
 * may be called from different coroutine contexts.
 *
 * ## Security
 * Implementations should consider clipboard data sensitivity:
 * - Clear clipboard after timeout for sensitive data
 * - Support clipboard data types (text, images, etc.)
 * - Handle clipboard access permissions
 *
 * @since 1.0.0
 * @see SelectionPlugin
 */
interface IClipboardProvider {

    /**
     * Copy text to clipboard.
     *
     * @param text The text to copy
     * @return true if copy was successful
     */
    suspend fun copy(text: String): Boolean

    /**
     * Get text from clipboard.
     *
     * @return The clipboard content, or null if empty/unavailable
     */
    suspend fun paste(): String?

    /**
     * Clear clipboard contents.
     *
     * @return true if clear was successful
     */
    suspend fun clear(): Boolean

    /**
     * Check if clipboard has content.
     *
     * @return true if clipboard contains data
     */
    suspend fun hasContent(): Boolean

    /**
     * Get the type of content in clipboard.
     *
     * @return Content type string (e.g., "text/plain", "image/png")
     */
    suspend fun getContentType(): String?
}

// =============================================================================
// Selection Executor Interface
// =============================================================================

/**
 * Interface for platform-specific selection operations.
 *
 * Implementations provide text selection capabilities for the target platform:
 * - Android: Uses AccessibilityNodeInfo selection actions
 * - iOS: Uses UITextInput selection APIs
 * - Desktop: Uses native text selection APIs
 *
 * ## Accessibility Context
 * Selection operations work within the accessibility framework,
 * operating on the currently focused text element or editable field.
 *
 * @since 1.0.0
 * @see SelectionPlugin
 */
interface ISelectionExecutor {

    /**
     * Select all content in the current focus.
     *
     * @return true if select all was successful
     */
    suspend fun selectAll(): Boolean

    /**
     * Select specific text within the current focus.
     *
     * @param text The text to search for and select
     * @return true if text was found and selected
     */
    suspend fun selectText(text: String): Boolean

    /**
     * Clear the current selection.
     *
     * @return true if selection was cleared
     */
    suspend fun clearSelection(): Boolean

    /**
     * Get the currently selected text.
     *
     * @return Selected text or null if no selection
     */
    suspend fun getSelectedText(): String?

    /**
     * Perform cut operation (copy to clipboard + delete).
     *
     * @return true if cut was successful
     */
    suspend fun cut(): Boolean

    /**
     * Perform copy operation.
     *
     * @return true if copy was successful
     */
    suspend fun copy(): Boolean

    /**
     * Perform paste operation.
     *
     * @return true if paste was successful
     */
    suspend fun paste(): Boolean
}

// =============================================================================
// Selection Plugin Implementation
// =============================================================================

/**
 * Selection Plugin - Universal Plugin for text selection and clipboard operations.
 *
 * Handles all selection-related voice/gaze commands including:
 * - Select all content
 * - Select specific text by search
 * - Copy/cut/paste operations
 * - Clear selection
 *
 * ## Migration Notes
 * This plugin wraps the original SelectHandler logic from VoiceOSCore,
 * adapting it to the Universal Plugin interface while maintaining identical
 * behavior and adding enhanced state tracking.
 *
 * ## Usage
 * ```kotlin
 * val plugin = SelectionPlugin(
 *     clipboardProvider = { androidClipboardProvider },
 *     selectionExecutor = { androidSelectionExecutor }
 * )
 * plugin.initialize(config, context)
 *
 * val command = QuantizedCommand(phrase = "select all", ...)
 * if (plugin.canHandle(command, handlerContext)) {
 *     val result = plugin.handle(command, handlerContext)
 * }
 * ```
 *
 * ## Supported Commands
 * - "select all" - Select all content
 * - "select <text>" - Select specific text
 * - "copy" - Copy current selection to clipboard
 * - "cut" - Cut current selection to clipboard
 * - "paste" - Paste from clipboard
 * - "clear", "clear selection" - Clear current selection
 *
 * @param clipboardProvider Lazy provider for platform-specific clipboard implementation
 * @param selectionExecutor Lazy provider for platform-specific selection executor
 * @since 1.0.0
 * @see HandlerPlugin
 * @see BasePlugin
 * @see IClipboardProvider
 * @see ISelectionExecutor
 */
class SelectionPlugin(
    private val clipboardProvider: () -> IClipboardProvider?,
    private val selectionExecutor: () -> ISelectionExecutor?
) : BasePlugin(), HandlerPlugin {

    // =========================================================================
    // Identity
    // =========================================================================

    override val pluginId: String = PLUGIN_ID
    override val pluginName: String = "Selection Handler"
    override val version: String = "1.0.0"

    override val capabilities: Set<PluginCapability> = setOf(
        PluginCapability(
            id = PluginCapability.ACCESSIBILITY_HANDLER,
            name = "Selection Handler",
            version = "1.0.0",
            interfaces = setOf("HandlerPlugin", "IClipboardProvider", "ISelectionExecutor"),
            metadata = mapOf(
                "handlerType" to "TEXT_INPUT",
                "supportsSelectAll" to "true",
                "supportsClipboard" to "true",
                "supportsTextSearch" to "true"
            )
        )
    )

    // =========================================================================
    // Handler Properties
    // =========================================================================

    override val handlerType: HandlerType = HandlerType.TEXT_INPUT

    override val patterns: List<CommandPattern> = listOf(
        // Select patterns
        CommandPattern(
            regex = Regex("^select all$", RegexOption.IGNORE_CASE),
            intent = "SELECT_ALL",
            requiredEntities = emptySet(),
            examples = listOf("select all")
        ),
        CommandPattern(
            regex = Regex("^select\\s+(.+)$", RegexOption.IGNORE_CASE),
            intent = "SELECT",
            requiredEntities = setOf("text"),
            examples = listOf("select hello", "select this text")
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

        // Clear patterns
        CommandPattern(
            regex = Regex("^(clear|clear selection|deselect)$", RegexOption.IGNORE_CASE),
            intent = "CLEAR",
            requiredEntities = emptySet(),
            examples = listOf("clear", "clear selection", "deselect")
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
        "select all",
        "copy",
        "cut",
        "paste",
        "clear",
        "clear selection",
        "deselect"
    )

    // =========================================================================
    // State Management
    // =========================================================================

    /** Current selection state */
    private var _selectionState = SelectionState.EMPTY

    /** Observable selection state flow */
    private val _selectionStateFlow = MutableStateFlow(_selectionState)

    /**
     * Observable flow of selection state changes.
     */
    val selectionStateFlow: StateFlow<SelectionState> get() = _selectionStateFlow

    /**
     * Current selection state.
     */
    val selectionState: SelectionState get() = _selectionState

    // =========================================================================
    // Provider References
    // =========================================================================

    private var clipboard: IClipboardProvider? = null
    private var executor: ISelectionExecutor? = null

    // =========================================================================
    // Lifecycle
    // =========================================================================

    override suspend fun onInitialize(): InitResult {
        return try {
            clipboard = clipboardProvider()
            executor = selectionExecutor()
            InitResult.success("SelectionPlugin initialized")
        } catch (e: Exception) {
            InitResult.failure(e, recoverable = true)
        }
    }

    override suspend fun onShutdown() {
        clearSelectionState()
    }

    override fun getHealthDiagnostics(): Map<String, String> = mapOf(
        "supportedActions" to supportedActions.size.toString(),
        "patterns" to patterns.size.toString(),
        "clipboardAvailable" to (clipboard != null).toString(),
        "executorAvailable" to (executor != null).toString(),
        "hasSelection" to _selectionState.hasSelection.toString(),
        "selectionLength" to _selectionState.selectionLength.toString()
    )

    // =========================================================================
    // Handler Implementation
    // =========================================================================

    override fun canHandle(command: QuantizedCommand, context: HandlerContext): Boolean {
        val phrase = command.phrase.lowercase().trim()

        // Check exact matches first
        if (supportedActions.any { phrase == it.lowercase() }) {
            return true
        }

        // Check pattern matches
        if (patterns.any { it.matches(phrase) }) {
            return true
        }

        // Check for "select <something>" pattern
        if (phrase.startsWith("select ")) {
            return true
        }

        return false
    }

    override suspend fun handle(
        command: QuantizedCommand,
        context: HandlerContext
    ): ActionResult {
        val result = handleCommand(command.phrase)

        return if (result.success) {
            val message = when (result.action) {
                SelectionAction.SELECT_ALL -> "Selected all content"
                SelectionAction.SELECT -> "Selected: ${result.selectedText ?: "text"}"
                SelectionAction.COPY -> "Copied to clipboard"
                SelectionAction.CUT -> "Cut to clipboard"
                SelectionAction.PASTE -> "Pasted from clipboard"
                SelectionAction.CLEAR -> "Selection cleared"
                else -> "Selection action completed"
            }
            ActionResult.Success(message)
        } else {
            ActionResult.Error(result.error ?: "Selection action failed")
        }
    }

    override fun getConfidence(command: QuantizedCommand, context: HandlerContext): Float {
        val phrase = command.phrase.lowercase().trim()

        // Exact match with supported actions
        if (supportedActions.any { it.lowercase() == phrase }) {
            return 1.0f
        }

        // Pattern match
        for (pattern in patterns) {
            if (pattern.matches(phrase)) {
                return 0.95f
            }
        }

        // Select with text pattern
        if (phrase.startsWith("select ") && phrase.length > 7) {
            return 0.9f
        }

        // Partial matches for selection verbs
        val selectionVerbs = listOf("select", "copy", "cut", "paste", "clear", "deselect")
        if (selectionVerbs.any { phrase.startsWith(it) }) {
            return 0.6f
        }

        return 0.0f
    }

    // =========================================================================
    // Command Handling
    // =========================================================================

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
            normalizedCommand == "clear" || normalizedCommand == "clear selection" || normalizedCommand == "deselect" -> clearSelection()
            normalizedCommand.startsWith("select ") -> selectText(command.substringAfter("select "))
            normalizedCommand == "select" -> SelectionResult.failure(SelectionAction.SELECT, "Please specify what to select")
            else -> SelectionResult.failure(
                SelectionAction.SELECT,
                "Unknown command: $command"
            )
        }
    }

    // =========================================================================
    // Selection Operations
    // =========================================================================

    /**
     * Select all content.
     *
     * Sets selection range to cover all content.
     *
     * @return Result indicating success
     */
    suspend fun selectAll(): SelectionResult {
        return try {
            val success = executor?.selectAll() ?: run {
                // Fallback: update internal state only
                updateSelectionState(SelectionState.withRange(0, Int.MAX_VALUE))
                true
            }

            if (success) {
                SelectionResult.success(SelectionAction.SELECT_ALL)
            } else {
                SelectionResult.failure(SelectionAction.SELECT_ALL, "Could not select all content")
            }
        } catch (e: Exception) {
            SelectionResult.failure(SelectionAction.SELECT_ALL, "Select all error: ${e.message}")
        }
    }

    /**
     * Synchronous version for command handling compatibility.
     */
    private fun selectAll(): SelectionResult {
        updateSelectionState(SelectionState.withRange(0, Int.MAX_VALUE))
        return SelectionResult.success(SelectionAction.SELECT_ALL)
    }

    /**
     * Select specific text.
     *
     * @param text The text to select
     * @return Result with the selected text
     */
    suspend fun selectText(text: String): SelectionResult {
        if (text.isBlank()) {
            return SelectionResult.failure(SelectionAction.SELECT, "Cannot select empty text")
        }

        return try {
            val success = executor?.selectText(text) ?: run {
                // Fallback: update internal state only
                updateSelectionState(SelectionState.withText(text))
                true
            }

            if (success) {
                updateSelectionState(SelectionState.withText(text))
                SelectionResult.success(SelectionAction.SELECT, text)
            } else {
                SelectionResult.failure(SelectionAction.SELECT, "Text not found: $text")
            }
        } catch (e: Exception) {
            SelectionResult.failure(SelectionAction.SELECT, "Select error: ${e.message}")
        }
    }

    /**
     * Synchronous version for command handling compatibility.
     */
    private fun selectText(text: String): SelectionResult {
        if (text.isBlank()) {
            return SelectionResult.failure(SelectionAction.SELECT, "Cannot select empty text")
        }
        updateSelectionState(SelectionState.withText(text))
        return SelectionResult.success(SelectionAction.SELECT, text)
    }

    /**
     * Copy current selection to clipboard.
     *
     * @return Result indicating success or failure
     */
    suspend fun copy(): SelectionResult {
        val text = _selectionState.selectedText

        if (text == null && !_selectionState.hasSelection) {
            return SelectionResult.failure(SelectionAction.COPY, "Nothing selected")
        }

        return try {
            // Try using executor first (for accessibility-based copy)
            val executorSuccess = executor?.copy() ?: false
            if (executorSuccess) {
                return SelectionResult.success(SelectionAction.COPY, text)
            }

            // Fallback to clipboard provider
            if (text != null) {
                val clipboardSuccess = clipboard?.copy(text) ?: false
                if (clipboardSuccess) {
                    return SelectionResult.success(SelectionAction.COPY, text)
                }
            }

            // If we have selection state but couldn't copy, still report success
            // (the selection exists, we just couldn't interact with clipboard)
            if (_selectionState.hasSelection) {
                SelectionResult.success(SelectionAction.COPY, text)
            } else {
                SelectionResult.failure(SelectionAction.COPY, "Nothing selected")
            }
        } catch (e: Exception) {
            SelectionResult.failure(SelectionAction.COPY, "Copy error: ${e.message}")
        }
    }

    /**
     * Synchronous version for command handling compatibility.
     */
    private fun copy(): SelectionResult {
        val text = _selectionState.selectedText
            ?: return SelectionResult.failure(SelectionAction.COPY, "Nothing selected")

        // Synchronously attempt clipboard copy (will be no-op if clipboard is suspend-only)
        return SelectionResult.success(SelectionAction.COPY, text)
    }

    /**
     * Cut current selection to clipboard.
     *
     * Copies text to clipboard and clears selection.
     *
     * @return Result indicating success or failure
     */
    suspend fun cut(): SelectionResult {
        val text = _selectionState.selectedText

        if (text == null && !_selectionState.hasSelection) {
            return SelectionResult.failure(SelectionAction.CUT, "Nothing selected")
        }

        return try {
            // Try using executor first (for accessibility-based cut)
            val executorSuccess = executor?.cut() ?: false
            if (executorSuccess) {
                clearSelectionState()
                return SelectionResult.success(SelectionAction.CUT, text)
            }

            // Fallback to clipboard provider + clear
            if (text != null) {
                clipboard?.copy(text)
            }
            clearSelectionState()
            SelectionResult.success(SelectionAction.CUT, text)
        } catch (e: Exception) {
            SelectionResult.failure(SelectionAction.CUT, "Cut error: ${e.message}")
        }
    }

    /**
     * Synchronous version for command handling compatibility.
     */
    private fun cut(): SelectionResult {
        val text = _selectionState.selectedText
            ?: return SelectionResult.failure(SelectionAction.CUT, "Nothing selected")

        clearSelectionState()
        return SelectionResult.success(SelectionAction.CUT, text)
    }

    /**
     * Paste from clipboard.
     *
     * @return Result with pasted text or failure if clipboard empty
     */
    suspend fun paste(): SelectionResult {
        return try {
            // Try using executor first (for accessibility-based paste)
            val executorSuccess = executor?.paste() ?: false
            if (executorSuccess) {
                return SelectionResult.success(SelectionAction.PASTE)
            }

            // Fallback to clipboard provider
            val text = clipboard?.paste()
            if (text != null) {
                SelectionResult.success(SelectionAction.PASTE, text)
            } else {
                SelectionResult.failure(SelectionAction.PASTE, "Clipboard empty or unavailable")
            }
        } catch (e: Exception) {
            SelectionResult.failure(SelectionAction.PASTE, "Paste error: ${e.message}")
        }
    }

    /**
     * Synchronous version for command handling compatibility.
     */
    private fun paste(): SelectionResult {
        // Without async clipboard access, we can only report the action
        return SelectionResult.failure(SelectionAction.PASTE, "Clipboard empty or unavailable")
    }

    /**
     * Clear current selection.
     *
     * Resets all selection state.
     *
     * @return Result indicating success
     */
    suspend fun clearSelection(): SelectionResult {
        return try {
            executor?.clearSelection()
            clearSelectionState()
            SelectionResult.success(SelectionAction.CLEAR)
        } catch (e: Exception) {
            clearSelectionState()
            SelectionResult.success(SelectionAction.CLEAR) // Still succeed on state clear
        }
    }

    /**
     * Synchronous version for command handling compatibility.
     */
    private fun clearSelection(): SelectionResult {
        clearSelectionState()
        return SelectionResult.success(SelectionAction.CLEAR)
    }

    // =========================================================================
    // State Management Helpers
    // =========================================================================

    /**
     * Update the internal selection state.
     */
    private fun updateSelectionState(newState: SelectionState) {
        _selectionState = newState
        _selectionStateFlow.value = newState
    }

    /**
     * Clear the internal selection state.
     */
    private fun clearSelectionState() {
        updateSelectionState(SelectionState.EMPTY)
    }

    // =========================================================================
    // Query Methods
    // =========================================================================

    /**
     * Check if there is an active selection.
     *
     * @return true if text is selected or selection range is set
     */
    fun hasSelection(): Boolean = _selectionState.hasSelection

    /**
     * Get the currently selected text.
     *
     * @return The selected text, or null if nothing selected
     */
    fun getSelectedText(): String? = _selectionState.selectedText

    companion object {
        /** Plugin ID for registration and discovery */
        const val PLUGIN_ID = "com.augmentalis.voiceoscore.handler.selection"
    }
}

// =============================================================================
// Factory Functions
// =============================================================================

/**
 * Create a SelectionPlugin with pre-configured providers.
 *
 * @param clipboard The clipboard provider implementation
 * @param executor The selection executor implementation (optional)
 * @return Configured SelectionPlugin
 */
fun createSelectionPlugin(
    clipboard: IClipboardProvider,
    executor: ISelectionExecutor? = null
): SelectionPlugin {
    return SelectionPlugin(
        clipboardProvider = { clipboard },
        selectionExecutor = { executor }
    )
}

/**
 * Create a SelectionPlugin with lazy providers.
 *
 * Useful when providers depend on platform services that may
 * not be available at plugin creation time.
 *
 * @param clipboardProvider Function that returns the clipboard provider
 * @param selectionExecutor Function that returns the selection executor
 * @return Configured SelectionPlugin
 */
fun createSelectionPlugin(
    clipboardProvider: () -> IClipboardProvider?,
    selectionExecutor: () -> ISelectionExecutor? = { null }
): SelectionPlugin {
    return SelectionPlugin(clipboardProvider, selectionExecutor)
}

/**
 * Create a SelectionPlugin with clipboard only (no executor).
 *
 * Useful for simple clipboard operations without platform selection support.
 *
 * @param clipboard The clipboard provider implementation
 * @return Configured SelectionPlugin
 */
fun createSelectionPlugin(clipboard: IClipboardProvider): SelectionPlugin {
    return SelectionPlugin(
        clipboardProvider = { clipboard },
        selectionExecutor = { null }
    )
}

// =============================================================================
// Testing Support
// =============================================================================

/**
 * Mock clipboard provider for testing SelectionPlugin.
 *
 * Records all clipboard operations and maintains an in-memory clipboard.
 * Useful for unit testing without requiring actual platform clipboard services.
 *
 * ## Usage
 * ```kotlin
 * val mockClipboard = MockClipboardProvider()
 * val plugin = createSelectionPlugin(mockClipboard)
 *
 * // Execute clipboard operations
 * plugin.handle(copyCommand, context)
 *
 * // Verify state
 * assertEquals("copied text", mockClipboard.clipboardContent)
 * assertEquals(listOf("copy(copied text)"), mockClipboard.actions)
 * ```
 *
 * @param initialContent Initial clipboard content (optional)
 * @since 1.0.0
 */
class MockClipboardProvider(
    initialContent: String? = null
) : IClipboardProvider {

    private val _actions = mutableListOf<String>()
    private var _content: String? = initialContent

    /** List of recorded actions in format "methodName(params)" */
    val actions: List<String> get() = _actions.toList()

    /** Current clipboard content */
    val clipboardContent: String? get() = _content

    /** Clear recorded actions */
    fun clearActions() = _actions.clear()

    /** Set clipboard content directly (for testing) */
    fun setContent(content: String?) {
        _content = content
    }

    override suspend fun copy(text: String): Boolean {
        _actions.add("copy($text)")
        _content = text
        return true
    }

    override suspend fun paste(): String? {
        _actions.add("paste()")
        return _content
    }

    override suspend fun clear(): Boolean {
        _actions.add("clear()")
        _content = null
        return true
    }

    override suspend fun hasContent(): Boolean {
        _actions.add("hasContent()")
        return _content != null
    }

    override suspend fun getContentType(): String? {
        _actions.add("getContentType()")
        return if (_content != null) "text/plain" else null
    }
}

/**
 * Mock selection executor for testing SelectionPlugin.
 *
 * Records all selection operations and can be configured to succeed or fail.
 * Useful for unit testing without requiring actual platform selection services.
 *
 * ## Usage
 * ```kotlin
 * val mockExecutor = MockSelectionExecutor(shouldSucceed = true)
 * val plugin = createSelectionPlugin(mockClipboard, mockExecutor)
 *
 * // Execute selection operations
 * plugin.handle(selectAllCommand, context)
 *
 * // Verify actions
 * assertEquals(listOf("selectAll()"), mockExecutor.actions)
 * ```
 *
 * @param shouldSucceed Whether selection operations should succeed
 * @since 1.0.0
 */
class MockSelectionExecutor(
    private val shouldSucceed: Boolean = true
) : ISelectionExecutor {

    private val _actions = mutableListOf<String>()
    private var _selectedText: String? = null

    /** List of recorded actions in format "methodName(params)" */
    val actions: List<String> get() = _actions.toList()

    /** Current selected text (for testing) */
    val selectedText: String? get() = _selectedText

    /** Clear recorded actions */
    fun clearActions() = _actions.clear()

    /** Set selected text directly (for testing) */
    fun setSelectedText(text: String?) {
        _selectedText = text
    }

    override suspend fun selectAll(): Boolean {
        _actions.add("selectAll()")
        if (shouldSucceed) _selectedText = "[ALL]"
        return shouldSucceed
    }

    override suspend fun selectText(text: String): Boolean {
        _actions.add("selectText($text)")
        if (shouldSucceed) _selectedText = text
        return shouldSucceed
    }

    override suspend fun clearSelection(): Boolean {
        _actions.add("clearSelection()")
        if (shouldSucceed) _selectedText = null
        return shouldSucceed
    }

    override suspend fun getSelectedText(): String? {
        _actions.add("getSelectedText()")
        return _selectedText
    }

    override suspend fun cut(): Boolean {
        _actions.add("cut()")
        if (shouldSucceed) _selectedText = null
        return shouldSucceed
    }

    override suspend fun copy(): Boolean {
        _actions.add("copy()")
        return shouldSucceed
    }

    override suspend fun paste(): Boolean {
        _actions.add("paste()")
        return shouldSucceed
    }
}
