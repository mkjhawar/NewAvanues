/**
 * AutocompleteHandler.kt - Voice handler for Autocomplete/Combobox interactions
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Created: 2026-01-28
 *
 * Purpose: Voice-driven autocomplete and combobox control with suggestion selection
 * Features:
 * - Select autocomplete suggestion by text match
 * - Select suggestion by position (first, second, third, last)
 * - Select suggestion by index number (option 1, pick 2)
 * - Type text into autocomplete input field
 * - Clear autocomplete input
 * - Show/hide suggestion dropdown
 * - Named autocomplete targeting (e.g., "select New York in city")
 * - Focused autocomplete targeting (e.g., "select first option")
 * - AVID-based targeting for precise element selection
 * - Voice feedback for selections and actions
 *
 * Location: CommandManager module handlers
 *
 * ## Supported Commands
 *
 * Selection by text:
 * - "select [option]" - Select suggestion matching text
 * - "choose [option]" - Choose suggestion matching text
 * - "select [option] in [name]" - Select in named autocomplete
 *
 * Selection by position:
 * - "first option" / "select first" - Select first suggestion
 * - "second option" / "select second" - Select second suggestion
 * - "third option" / "select third" - Select third suggestion
 * - "last option" / "select last" - Select last suggestion
 *
 * Selection by index:
 * - "option [N]" - Select Nth suggestion (1-indexed)
 * - "pick [N]" - Pick Nth suggestion (1-indexed)
 * - "number [N]" - Select Nth suggestion
 *
 * Input operations:
 * - "type [text]" - Type text into autocomplete input
 * - "clear" / "clear input" - Clear the input field
 *
 * Dropdown control:
 * - "show suggestions" / "show options" - Open dropdown
 * - "hide suggestions" / "close" - Close dropdown
 */

package com.augmentalis.avanueui.voice.handlers.input

import com.avanues.logging.LoggerFactory
import com.augmentalis.voiceoscore.ActionCategory
import com.augmentalis.voiceoscore.BaseHandler
import com.augmentalis.voiceoscore.Bounds
import com.augmentalis.voiceoscore.ElementInfo
import com.augmentalis.voiceoscore.HandlerResult
import com.augmentalis.voiceoscore.QuantizedCommand

/**
 * Voice command handler for Autocomplete/Combobox interactions.
 *
 * Provides comprehensive voice control for autocomplete components including:
 * - Text-based suggestion selection
 * - Positional selection (first, second, third, last)
 * - Index-based selection (option 1, pick 2)
 * - Input field manipulation (type, clear)
 * - Dropdown visibility control (show, hide)
 * - Named autocomplete targeting with disambiguation
 *
 * Thread Safety:
 * - All operations are suspend functions
 * - State modifications are atomic via executor
 *
 * @param executor Platform-specific executor for autocomplete operations
 */
class AutocompleteHandler(
    private val executor: AutocompleteExecutor
) : BaseHandler() {

    companion object {
        private const val TAG = "AutocompleteHandler"
        private val Log = LoggerFactory.getLogger(TAG)

        // Patterns for parsing commands
        private val SELECT_OPTION_PATTERN = Regex(
            """^(?:select|choose)\s+(.+?)(?:\s+(?:in|from)\s+(.+))?$""",
            RegexOption.IGNORE_CASE
        )

        private val POSITIONAL_PATTERN = Regex(
            """^(?:select\s+)?(first|second|third|fourth|fifth|last)\s*(?:option|item|suggestion)?$""",
            RegexOption.IGNORE_CASE
        )

        private val INDEX_PATTERN = Regex(
            """^(?:option|pick|number|item)\s+(\d+)$""",
            RegexOption.IGNORE_CASE
        )

        private val TYPE_PATTERN = Regex(
            """^type\s+(.+)$""",
            RegexOption.IGNORE_CASE
        )

        private val CLEAR_PATTERN = Regex(
            """^(?:clear|clear\s+input|clear\s+text|erase)$""",
            RegexOption.IGNORE_CASE
        )

        private val SHOW_SUGGESTIONS_PATTERN = Regex(
            """^(?:show\s+(?:suggestions|options|dropdown|list)|open\s+(?:dropdown|list|suggestions))$""",
            RegexOption.IGNORE_CASE
        )

        private val HIDE_SUGGESTIONS_PATTERN = Regex(
            """^(?:hide\s+(?:suggestions|options|dropdown|list)|close|close\s+(?:dropdown|list|suggestions)|dismiss)$""",
            RegexOption.IGNORE_CASE
        )

        // Positional word to index mapping (0-indexed)
        private val POSITIONAL_WORDS = mapOf(
            "first" to 0,
            "second" to 1,
            "third" to 2,
            "fourth" to 3,
            "fifth" to 4
        )
    }

    override val category: ActionCategory = ActionCategory.UI

    override val supportedActions: List<String> = listOf(
        // Selection by text
        "select", "choose",
        "select [option]", "choose [option]",
        // Selection by position
        "first option", "second option", "third option", "last option",
        "select first", "select second", "select third", "select last",
        // Selection by index
        "option [N]", "pick [N]", "number [N]",
        // Input operations
        "type [text]", "clear", "clear input",
        // Dropdown control
        "show suggestions", "show options",
        "hide suggestions", "close"
    )

    /**
     * Callback for voice feedback when an option is selected.
     */
    var onOptionSelected: ((autocompleteName: String, selectedOption: String, index: Int) -> Unit)? = null

    /**
     * Callback for voice feedback when input is typed.
     */
    var onTextTyped: ((autocompleteName: String, typedText: String) -> Unit)? = null

    /**
     * Callback for voice feedback when input is cleared.
     */
    var onInputCleared: ((autocompleteName: String) -> Unit)? = null

    override suspend fun execute(
        command: QuantizedCommand,
        params: Map<String, Any>
    ): HandlerResult {
        val normalizedAction = command.phrase.lowercase().trim()

        Log.d { "Processing autocomplete command: $normalizedAction" }

        return try {
            when {
                // Positional selection: "first option", "select second", etc.
                POSITIONAL_PATTERN.containsMatchIn(normalizedAction) -> {
                    handlePositionalSelection(normalizedAction, command)
                }

                // Index selection: "option 1", "pick 2", etc.
                INDEX_PATTERN.containsMatchIn(normalizedAction) -> {
                    handleIndexSelection(normalizedAction, command)
                }

                // Clear input: "clear", "clear input"
                CLEAR_PATTERN.containsMatchIn(normalizedAction) -> {
                    handleClearInput(command)
                }

                // Type text: "type hello world"
                TYPE_PATTERN.containsMatchIn(normalizedAction) -> {
                    handleTypeText(normalizedAction, command)
                }

                // Show suggestions: "show suggestions", "open dropdown"
                SHOW_SUGGESTIONS_PATTERN.containsMatchIn(normalizedAction) -> {
                    handleShowSuggestions(command)
                }

                // Hide suggestions: "hide suggestions", "close"
                HIDE_SUGGESTIONS_PATTERN.containsMatchIn(normalizedAction) -> {
                    handleHideSuggestions(command)
                }

                // Select by text: "select New York", "choose California"
                SELECT_OPTION_PATTERN.containsMatchIn(normalizedAction) -> {
                    handleTextSelection(normalizedAction, command)
                }

                else -> HandlerResult.notHandled()
            }
        } catch (e: Exception) {
            Log.e({ "Error executing autocomplete command" }, e)
            HandlerResult.failure("Error: ${e.message}", recoverable = true)
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Command Handlers
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Handle "select [option]" or "choose [option]" command.
     */
    private suspend fun handleTextSelection(
        normalizedAction: String,
        command: QuantizedCommand
    ): HandlerResult {
        val matchResult = SELECT_OPTION_PATTERN.find(normalizedAction)
            ?: return HandlerResult.failure("Could not parse select command")

        val optionText = matchResult.groupValues[1].trim()
        val autocompleteName = matchResult.groupValues[2].takeIf { it.isNotBlank() }?.trim()

        // Find the autocomplete
        val autocompleteInfo = findAutocomplete(
            name = autocompleteName,
            avid = command.targetAvid
        ) ?: return HandlerResult.Failure(
            reason = if (autocompleteName != null) "Autocomplete '$autocompleteName' not found" else "No autocomplete focused",
            recoverable = true,
            suggestedAction = "Focus on an autocomplete or say 'select New York in city'"
        )

        // Find matching suggestion
        val matchingIndex = findMatchingSuggestion(autocompleteInfo, optionText)
            ?: return HandlerResult.Failure(
                reason = "No suggestion matching '$optionText' found",
                recoverable = true,
                suggestedAction = buildSuggestionHint(autocompleteInfo)
            )

        return selectOption(autocompleteInfo, matchingIndex)
    }

    /**
     * Handle positional selection: "first option", "select second", etc.
     */
    private suspend fun handlePositionalSelection(
        normalizedAction: String,
        command: QuantizedCommand
    ): HandlerResult {
        val matchResult = POSITIONAL_PATTERN.find(normalizedAction)
            ?: return HandlerResult.failure("Could not parse positional command")

        val positionWord = matchResult.groupValues[1].lowercase()

        // Find the autocomplete
        val autocompleteInfo = findAutocomplete(avid = command.targetAvid)
            ?: return HandlerResult.Failure(
                reason = "No autocomplete focused",
                recoverable = true,
                suggestedAction = "Focus on an autocomplete first"
            )

        // Ensure suggestions are available
        if (autocompleteInfo.suggestions.isEmpty()) {
            return HandlerResult.Failure(
                reason = "No suggestions available",
                recoverable = true,
                suggestedAction = "Type something to get suggestions or say 'show suggestions'"
            )
        }

        // Calculate index
        val index = if (positionWord == "last") {
            autocompleteInfo.suggestions.lastIndex
        } else {
            POSITIONAL_WORDS[positionWord] ?: return HandlerResult.failure("Unknown position: $positionWord")
        }

        // Validate index
        if (index < 0 || index >= autocompleteInfo.suggestions.size) {
            return HandlerResult.Failure(
                reason = "Position '$positionWord' is out of range (${autocompleteInfo.suggestions.size} suggestions available)",
                recoverable = true,
                suggestedAction = "Try 'first option' or 'option 1'"
            )
        }

        return selectOption(autocompleteInfo, index)
    }

    /**
     * Handle index selection: "option 1", "pick 2", etc.
     */
    private suspend fun handleIndexSelection(
        normalizedAction: String,
        command: QuantizedCommand
    ): HandlerResult {
        val matchResult = INDEX_PATTERN.find(normalizedAction)
            ?: return HandlerResult.failure("Could not parse index command")

        val indexStr = matchResult.groupValues[1]
        val oneBasedIndex = indexStr.toIntOrNull()
            ?: return HandlerResult.failure("Invalid number: $indexStr")

        // Convert to 0-indexed
        val index = oneBasedIndex - 1

        // Find the autocomplete
        val autocompleteInfo = findAutocomplete(avid = command.targetAvid)
            ?: return HandlerResult.Failure(
                reason = "No autocomplete focused",
                recoverable = true,
                suggestedAction = "Focus on an autocomplete first"
            )

        // Ensure suggestions are available
        if (autocompleteInfo.suggestions.isEmpty()) {
            return HandlerResult.Failure(
                reason = "No suggestions available",
                recoverable = true,
                suggestedAction = "Type something to get suggestions or say 'show suggestions'"
            )
        }

        // Validate index
        if (index < 0 || index >= autocompleteInfo.suggestions.size) {
            return HandlerResult.Failure(
                reason = "Option $oneBasedIndex is out of range (1 to ${autocompleteInfo.suggestions.size} available)",
                recoverable = true,
                suggestedAction = "Try 'option 1' or say the suggestion name"
            )
        }

        return selectOption(autocompleteInfo, index)
    }

    /**
     * Handle "type [text]" command.
     */
    private suspend fun handleTypeText(
        normalizedAction: String,
        command: QuantizedCommand
    ): HandlerResult {
        val matchResult = TYPE_PATTERN.find(normalizedAction)
            ?: return HandlerResult.failure("Could not parse type command")

        val textToType = matchResult.groupValues[1].trim()

        if (textToType.isBlank()) {
            return HandlerResult.Failure(
                reason = "No text specified to type",
                recoverable = true,
                suggestedAction = "Say 'type hello world' to enter text"
            )
        }

        // Find the autocomplete
        val autocompleteInfo = findAutocomplete(avid = command.targetAvid)
            ?: return HandlerResult.Failure(
                reason = "No autocomplete focused",
                recoverable = true,
                suggestedAction = "Focus on an autocomplete first"
            )

        // Type the text
        val result = executor.type(autocompleteInfo, textToType)

        return if (result.success) {
            // Invoke callback for voice feedback
            onTextTyped?.invoke(
                autocompleteInfo.name.ifBlank { "Autocomplete" },
                textToType
            )

            val feedback = buildString {
                append("Typed '")
                append(textToType)
                append("'")
                if (autocompleteInfo.name.isNotBlank()) {
                    append(" in ")
                    append(autocompleteInfo.name)
                }
            }

            Log.i { "Text typed: $textToType in ${autocompleteInfo.name}" }

            HandlerResult.Success(
                message = feedback,
                data = mapOf(
                    "autocompleteName" to autocompleteInfo.name,
                    "autocompleteAvid" to autocompleteInfo.avid,
                    "typedText" to textToType,
                    "accessibility_announcement" to feedback
                )
            )
        } else {
            HandlerResult.failure(
                reason = result.error ?: "Could not type text",
                recoverable = true
            )
        }
    }

    /**
     * Handle "clear" or "clear input" command.
     */
    private suspend fun handleClearInput(command: QuantizedCommand): HandlerResult {
        // Find the autocomplete
        val autocompleteInfo = findAutocomplete(avid = command.targetAvid)
            ?: return HandlerResult.Failure(
                reason = "No autocomplete focused",
                recoverable = true,
                suggestedAction = "Focus on an autocomplete first"
            )

        // Clear the input
        val result = executor.clearInput(autocompleteInfo)

        return if (result.success) {
            // Invoke callback for voice feedback
            onInputCleared?.invoke(autocompleteInfo.name.ifBlank { "Autocomplete" })

            val feedback = buildString {
                if (autocompleteInfo.name.isNotBlank()) {
                    append(autocompleteInfo.name)
                    append(" cleared")
                } else {
                    append("Input cleared")
                }
            }

            Log.i { "Input cleared: ${autocompleteInfo.name}" }

            HandlerResult.Success(
                message = feedback,
                data = mapOf(
                    "autocompleteName" to autocompleteInfo.name,
                    "autocompleteAvid" to autocompleteInfo.avid,
                    "previousValue" to autocompleteInfo.currentValue,
                    "accessibility_announcement" to feedback
                )
            )
        } else {
            HandlerResult.failure(
                reason = result.error ?: "Could not clear input",
                recoverable = true
            )
        }
    }

    /**
     * Handle "show suggestions" or "show options" command.
     */
    private suspend fun handleShowSuggestions(command: QuantizedCommand): HandlerResult {
        // Find the autocomplete
        val autocompleteInfo = findAutocomplete(avid = command.targetAvid)
            ?: return HandlerResult.Failure(
                reason = "No autocomplete focused",
                recoverable = true,
                suggestedAction = "Focus on an autocomplete first"
            )

        // Check if already expanded
        if (autocompleteInfo.isExpanded) {
            return HandlerResult.Success(
                message = "Suggestions already showing",
                data = mapOf(
                    "autocompleteName" to autocompleteInfo.name,
                    "autocompleteAvid" to autocompleteInfo.avid,
                    "suggestionsCount" to autocompleteInfo.suggestions.size,
                    "alreadyExpanded" to true
                )
            )
        }

        // Show suggestions
        val result = executor.showSuggestions(autocompleteInfo)

        return if (result.success) {
            val feedback = buildString {
                append("Showing ")
                append(autocompleteInfo.suggestions.size)
                append(" suggestion")
                if (autocompleteInfo.suggestions.size != 1) append("s")
                if (autocompleteInfo.name.isNotBlank()) {
                    append(" for ")
                    append(autocompleteInfo.name)
                }
            }

            Log.i { "Suggestions shown: ${autocompleteInfo.name}" }

            HandlerResult.Success(
                message = feedback,
                data = mapOf(
                    "autocompleteName" to autocompleteInfo.name,
                    "autocompleteAvid" to autocompleteInfo.avid,
                    "suggestionsCount" to autocompleteInfo.suggestions.size,
                    "suggestions" to autocompleteInfo.suggestions,
                    "accessibility_announcement" to feedback
                )
            )
        } else {
            HandlerResult.failure(
                reason = result.error ?: "Could not show suggestions",
                recoverable = true
            )
        }
    }

    /**
     * Handle "hide suggestions" or "close" command.
     */
    private suspend fun handleHideSuggestions(command: QuantizedCommand): HandlerResult {
        // Find the autocomplete
        val autocompleteInfo = findAutocomplete(avid = command.targetAvid)
            ?: return HandlerResult.Failure(
                reason = "No autocomplete focused",
                recoverable = true,
                suggestedAction = "Focus on an autocomplete first"
            )

        // Check if already collapsed
        if (!autocompleteInfo.isExpanded) {
            return HandlerResult.Success(
                message = "Suggestions already hidden",
                data = mapOf(
                    "autocompleteName" to autocompleteInfo.name,
                    "autocompleteAvid" to autocompleteInfo.avid,
                    "alreadyCollapsed" to true
                )
            )
        }

        // Hide suggestions
        val result = executor.hideSuggestions(autocompleteInfo)

        return if (result.success) {
            val feedback = buildString {
                append("Suggestions hidden")
                if (autocompleteInfo.name.isNotBlank()) {
                    append(" for ")
                    append(autocompleteInfo.name)
                }
            }

            Log.i { "Suggestions hidden: ${autocompleteInfo.name}" }

            HandlerResult.Success(
                message = feedback,
                data = mapOf(
                    "autocompleteName" to autocompleteInfo.name,
                    "autocompleteAvid" to autocompleteInfo.avid,
                    "accessibility_announcement" to feedback
                )
            )
        } else {
            HandlerResult.failure(
                reason = result.error ?: "Could not hide suggestions",
                recoverable = true
            )
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Helper Methods
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Find autocomplete by name, AVID, or focus state.
     */
    private suspend fun findAutocomplete(
        name: String? = null,
        avid: String? = null
    ): AutocompleteInfo? {
        // Priority 1: AVID lookup (fastest, most precise)
        if (avid != null) {
            val autocomplete = executor.findByAvid(avid)
            if (autocomplete != null) return autocomplete
        }

        // Priority 2: Name lookup
        if (name != null) {
            val autocomplete = executor.findByName(name)
            if (autocomplete != null) return autocomplete
        }

        // Priority 3: Focused autocomplete
        return executor.findFocused()
    }

    /**
     * Find a suggestion that matches the given text.
     *
     * Matching priority:
     * 1. Exact match (case-insensitive)
     * 2. Starts with match
     * 3. Contains match
     *
     * @return Index of matching suggestion, or null if not found
     */
    private fun findMatchingSuggestion(
        autocompleteInfo: AutocompleteInfo,
        searchText: String
    ): Int? {
        val normalizedSearch = searchText.lowercase().trim()

        // Priority 1: Exact match
        autocompleteInfo.suggestions.forEachIndexed { index, suggestion ->
            if (suggestion.lowercase() == normalizedSearch) {
                return index
            }
        }

        // Priority 2: Starts with match
        autocompleteInfo.suggestions.forEachIndexed { index, suggestion ->
            if (suggestion.lowercase().startsWith(normalizedSearch)) {
                return index
            }
        }

        // Priority 3: Contains match
        autocompleteInfo.suggestions.forEachIndexed { index, suggestion ->
            if (suggestion.lowercase().contains(normalizedSearch)) {
                return index
            }
        }

        return null
    }

    /**
     * Select an option by index and return result.
     */
    private suspend fun selectOption(
        autocompleteInfo: AutocompleteInfo,
        index: Int
    ): HandlerResult {
        val selectedOption = autocompleteInfo.suggestions[index]

        // Select via executor
        val result = executor.selectOption(autocompleteInfo, index)

        return if (result.success) {
            // Invoke callback for voice feedback
            onOptionSelected?.invoke(
                autocompleteInfo.name.ifBlank { "Autocomplete" },
                selectedOption,
                index
            )

            // Build feedback message
            val feedback = buildString {
                append("Selected '")
                append(selectedOption)
                append("'")
                if (autocompleteInfo.name.isNotBlank()) {
                    append(" in ")
                    append(autocompleteInfo.name)
                }
            }

            Log.i { "Option selected: $selectedOption (index $index) in ${autocompleteInfo.name}" }

            HandlerResult.Success(
                message = feedback,
                data = mapOf(
                    "autocompleteName" to autocompleteInfo.name,
                    "autocompleteAvid" to autocompleteInfo.avid,
                    "selectedOption" to selectedOption,
                    "selectedIndex" to index,
                    "previousValue" to autocompleteInfo.currentValue,
                    "accessibility_announcement" to feedback
                )
            )
        } else {
            HandlerResult.failure(
                reason = result.error ?: "Could not select option",
                recoverable = true
            )
        }
    }

    /**
     * Build a suggestion hint message for when no match is found.
     */
    private fun buildSuggestionHint(autocompleteInfo: AutocompleteInfo): String {
        return if (autocompleteInfo.suggestions.isEmpty()) {
            "Type something to get suggestions or say 'show suggestions'"
        } else {
            val preview = autocompleteInfo.suggestions.take(3).joinToString(", ")
            "Available suggestions include: $preview"
        }
    }

}

// ═══════════════════════════════════════════════════════════════════════════════
// Supporting Types
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * Information about an autocomplete/combobox component.
 *
 * @property avid AVID fingerprint for the autocomplete (format: ACM:{hash8})
 * @property name Display name or associated label
 * @property currentValue Current text value in the input field
 * @property suggestions List of available suggestions
 * @property isExpanded Whether the suggestion dropdown is currently visible
 * @property bounds Screen bounds for the autocomplete
 * @property isFocused Whether this autocomplete currently has focus
 * @property node Platform-specific node reference
 */
data class AutocompleteInfo(
    val avid: String,
    val name: String = "",
    val currentValue: String = "",
    val suggestions: List<String> = emptyList(),
    val isExpanded: Boolean = false,
    val bounds: Bounds = Bounds.EMPTY,
    val isFocused: Boolean = false,
    val node: Any? = null
) {
    /**
     * Convert to ElementInfo for general element operations.
     */
    fun toElementInfo(): ElementInfo = ElementInfo(
        className = "AutocompleteTextView",
        text = name,
        bounds = bounds,
        isClickable = true,
        isEnabled = true,
        avid = avid,
        stateDescription = if (isExpanded) "expanded, $currentValue" else currentValue
    )
}

/**
 * Result of an autocomplete operation.
 */
data class AutocompleteOperationResult(
    val success: Boolean,
    val error: String? = null,
    val previousValue: String = "",
    val newValue: String = ""
) {
    companion object {
        fun success(previousValue: String = "", newValue: String = "") = AutocompleteOperationResult(
            success = true,
            previousValue = previousValue,
            newValue = newValue
        )

        fun error(message: String) = AutocompleteOperationResult(
            success = false,
            error = message
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// Platform Executor Interface
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * Platform-specific executor for autocomplete operations.
 *
 * Implementations should:
 * 1. Find autocomplete components by AVID, name, or focus state
 * 2. Read current input values and suggestions
 * 3. Select suggestions via accessibility actions or clicks
 * 4. Handle both AutoCompleteTextView and custom combobox components
 *
 * ## Autocomplete Detection Algorithm
 *
 * ```kotlin
 * fun findAutocompleteNode(rootNode: AccessibilityNodeInfo): AccessibilityNodeInfo? {
 *     // Look for nodes with className:
 *     // - android.widget.AutoCompleteTextView
 *     // - android.widget.MultiAutoCompleteTextView
 *     // - androidx.appcompat.widget.AppCompatAutoCompleteTextView
 *     // - Custom combobox implementations with dropdown popup
 * }
 * ```
 *
 * ## Suggestion Extraction Algorithm
 *
 * ```kotlin
 * fun getSuggestions(node: AccessibilityNodeInfo): List<String> {
 *     // 1. Find the dropdown popup window
 *     // 2. Extract text from each list item
 *     // 3. Return ordered list of suggestion strings
 * }
 * ```
 */
interface AutocompleteExecutor {

    // ═══════════════════════════════════════════════════════════════════════════
    // Autocomplete Discovery
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Find an autocomplete by its AVID fingerprint.
     *
     * @param avid The AVID fingerprint (format: ACM:{hash8})
     * @return AutocompleteInfo if found, null otherwise
     */
    suspend fun findByAvid(avid: String): AutocompleteInfo?

    /**
     * Find an autocomplete by its name or associated label.
     *
     * Searches for:
     * 1. Autocomplete with matching hint text
     * 2. Autocomplete with matching contentDescription
     * 3. Autocomplete with associated TextView label nearby
     *
     * @param name The name to search for (case-insensitive)
     * @return AutocompleteInfo if found, null otherwise
     */
    suspend fun findByName(name: String): AutocompleteInfo?

    /**
     * Find the currently focused autocomplete.
     *
     * @return AutocompleteInfo if an autocomplete has focus, null otherwise
     */
    suspend fun findFocused(): AutocompleteInfo?

    // ═══════════════════════════════════════════════════════════════════════════
    // Selection Operations
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Select a suggestion by its index.
     *
     * The index is 0-based and corresponds to the position in the suggestions list.
     * The dropdown should be expanded if not already.
     *
     * @param autocomplete The autocomplete to modify
     * @param index The 0-based index of the suggestion to select
     * @return Operation result with previous and new values
     */
    suspend fun selectOption(autocomplete: AutocompleteInfo, index: Int): AutocompleteOperationResult

    // ═══════════════════════════════════════════════════════════════════════════
    // Input Operations
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Clear the autocomplete input field.
     *
     * @param autocomplete The autocomplete to clear
     * @return Operation result
     */
    suspend fun clearInput(autocomplete: AutocompleteInfo): AutocompleteOperationResult

    /**
     * Type text into the autocomplete input field.
     *
     * This should:
     * 1. Focus the input if not already focused
     * 2. Append or replace text based on implementation
     * 3. Trigger suggestion filtering/refresh
     *
     * @param autocomplete The autocomplete to type into
     * @param text The text to type
     * @return Operation result
     */
    suspend fun type(autocomplete: AutocompleteInfo, text: String): AutocompleteOperationResult

    // ═══════════════════════════════════════════════════════════════════════════
    // Dropdown Operations
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Show the suggestions dropdown.
     *
     * This should:
     * 1. Focus the input if not already focused
     * 2. Trigger the dropdown to appear
     * 3. Refresh suggestion list
     *
     * @param autocomplete The autocomplete to expand
     * @return Operation result
     */
    suspend fun showSuggestions(autocomplete: AutocompleteInfo): AutocompleteOperationResult

    /**
     * Hide the suggestions dropdown.
     *
     * @param autocomplete The autocomplete to collapse
     * @return Operation result
     */
    suspend fun hideSuggestions(autocomplete: AutocompleteInfo): AutocompleteOperationResult
}
