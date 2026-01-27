/**
 * SearchBarHandler.kt - Voice handler for Search Bar interactions
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2026-01-28
 *
 * Purpose: Voice-driven search bar control with query execution and suggestions
 * Features:
 * - Execute search queries with voice commands
 * - Type text into search bar
 * - Clear search input
 * - Activate voice search mode
 * - Apply search filters
 * - Browse search history
 * - Select search suggestions by position
 * - AVID-based targeting for precise element selection
 * - Named search bar targeting
 * - Focused search bar targeting
 *
 * Location: CommandManager module handlers
 *
 * ## Supported Commands
 *
 * Search execution:
 * - "search for [query]" - Execute search with query
 * - "find [query]" - Execute search with query
 * - "search" / "go" / "submit" - Submit current search
 *
 * Text input:
 * - "type [text]" - Type text into search bar
 * - "clear search" / "clear" - Clear search input
 *
 * Voice and suggestions:
 * - "voice search" - Activate voice input mode
 * - "search suggestion [N]" - Select Nth search suggestion
 * - "suggestion [N]" - Select Nth suggestion (shorthand)
 *
 * Filter and history:
 * - "filter by [category]" - Apply search filter
 * - "recent searches" / "show history" - Show search history
 *
 * ## Query Parsing
 *
 * Supports:
 * - Direct queries: "search for weather today"
 * - Quoted queries: "search for \"best restaurants\""
 * - Numeric suggestion selection: "suggestion 1", "suggestion two"
 */

package com.augmentalis.avamagic.voice.handlers

import android.util.Log
import com.augmentalis.voiceoscore.ActionCategory
import com.augmentalis.voiceoscore.BaseHandler
import com.augmentalis.voiceoscore.Bounds
import com.augmentalis.voiceoscore.ElementInfo
import com.augmentalis.voiceoscore.HandlerResult
import com.augmentalis.voiceoscore.QuantizedCommand

/**
 * Voice command handler for Search Bar interactions.
 *
 * Provides comprehensive voice control for search bar components including:
 * - Query execution with natural language
 * - Text input and clearing
 * - Voice search activation
 * - Search suggestion selection
 * - Filter application
 * - Search history browsing
 *
 * Thread Safety:
 * - All operations are suspend functions
 * - State modifications are atomic via executor
 *
 * @param executor Platform-specific executor for search bar operations
 */
class SearchBarHandler(
    private val executor: SearchBarExecutor
) : BaseHandler() {

    companion object {
        private const val TAG = "SearchBarHandler"

        // Patterns for parsing commands
        private val SEARCH_FOR_PATTERN = Regex(
            """^(?:search\s+for|find)\s+(.+)$""",
            RegexOption.IGNORE_CASE
        )

        private val TYPE_PATTERN = Regex(
            """^type\s+(.+)$""",
            RegexOption.IGNORE_CASE
        )

        private val FILTER_BY_PATTERN = Regex(
            """^filter\s+by\s+(.+)$""",
            RegexOption.IGNORE_CASE
        )

        private val SUGGESTION_PATTERN = Regex(
            """^(?:search\s+)?suggestion\s+(\d+|one|two|three|four|five|six|seven|eight|nine|ten)$""",
            RegexOption.IGNORE_CASE
        )

        // Word to number mapping for suggestion selection
        private val WORD_NUMBERS = mapOf(
            "one" to 1, "two" to 2, "three" to 3, "four" to 4, "five" to 5,
            "six" to 6, "seven" to 7, "eight" to 8, "nine" to 9, "ten" to 10
        )
    }

    override val category: ActionCategory = ActionCategory.UI

    override val supportedActions: List<String> = listOf(
        // Search execution
        "search for", "find", "search", "go", "submit",
        // Text input
        "type", "clear search", "clear",
        // Voice and suggestions
        "voice search", "search suggestion", "suggestion",
        // Filter and history
        "filter by", "recent searches", "show history"
    )

    /**
     * Callback for voice feedback when search is executed.
     */
    var onSearchExecuted: ((query: String) -> Unit)? = null

    /**
     * Callback for voice feedback when suggestions are shown.
     */
    var onSuggestionsShown: ((suggestions: List<String>) -> Unit)? = null

    override suspend fun execute(
        command: QuantizedCommand,
        params: Map<String, Any>
    ): HandlerResult {
        val normalizedAction = command.phrase.lowercase().trim()

        Log.d(TAG, "Processing search bar command: $normalizedAction")

        return try {
            when {
                // Search for query: "search for [query]" or "find [query]"
                SEARCH_FOR_PATTERN.containsMatchIn(normalizedAction) -> {
                    handleSearchFor(normalizedAction, command)
                }

                // Type text: "type [text]"
                TYPE_PATTERN.containsMatchIn(normalizedAction) -> {
                    handleType(normalizedAction, command)
                }

                // Filter by category: "filter by [category]"
                FILTER_BY_PATTERN.containsMatchIn(normalizedAction) -> {
                    handleFilterBy(normalizedAction, command)
                }

                // Select suggestion: "suggestion [N]" or "search suggestion [N]"
                SUGGESTION_PATTERN.containsMatchIn(normalizedAction) -> {
                    handleSelectSuggestion(normalizedAction, command)
                }

                // Submit current search
                normalizedAction in listOf("search", "go", "submit") -> {
                    handleSubmitSearch(command)
                }

                // Clear search input
                normalizedAction in listOf("clear search", "clear") -> {
                    handleClearSearch(command)
                }

                // Activate voice search
                normalizedAction == "voice search" -> {
                    handleVoiceSearch(command)
                }

                // Show search history
                normalizedAction in listOf("recent searches", "show history") -> {
                    handleShowHistory(command)
                }

                else -> HandlerResult.notHandled()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error executing search bar command", e)
            HandlerResult.failure("Error: ${e.message}", recoverable = true)
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Command Handlers
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Handle "search for [query]" or "find [query]" command.
     */
    private suspend fun handleSearchFor(
        normalizedAction: String,
        command: QuantizedCommand
    ): HandlerResult {
        val matchResult = SEARCH_FOR_PATTERN.find(normalizedAction)
            ?: return HandlerResult.failure("Could not parse search command")

        val query = matchResult.groupValues[1].trim()
            .removeSurrounding("\"")
            .removeSurrounding("'")

        if (query.isBlank()) {
            return HandlerResult.Failure(
                reason = "Search query is empty",
                recoverable = true,
                suggestedAction = "Try 'search for weather' or 'find restaurants'"
            )
        }

        // Find the search bar
        val searchBarInfo = findSearchBar(
            avid = command.targetAvid
        ) ?: return HandlerResult.Failure(
            reason = "No search bar found",
            recoverable = true,
            suggestedAction = "Focus on a search bar first"
        )

        // Execute the search
        val result = executor.executeSearch(searchBarInfo, query)

        return if (result.success) {
            // Invoke callback for voice feedback
            onSearchExecuted?.invoke(query)

            Log.i(TAG, "Search executed: '$query'")

            HandlerResult.Success(
                message = "Searching for '$query'",
                data = mapOf(
                    "searchBarAvid" to searchBarInfo.avid,
                    "query" to query,
                    "previousQuery" to (searchBarInfo.currentQuery ?: ""),
                    "accessibility_announcement" to "Searching for $query"
                )
            )
        } else {
            HandlerResult.failure(
                reason = result.error ?: "Could not execute search",
                recoverable = true
            )
        }
    }

    /**
     * Handle "type [text]" command.
     */
    private suspend fun handleType(
        normalizedAction: String,
        command: QuantizedCommand
    ): HandlerResult {
        val matchResult = TYPE_PATTERN.find(normalizedAction)
            ?: return HandlerResult.failure("Could not parse type command")

        val text = matchResult.groupValues[1].trim()
            .removeSurrounding("\"")
            .removeSurrounding("'")

        if (text.isBlank()) {
            return HandlerResult.Failure(
                reason = "Text to type is empty",
                recoverable = true,
                suggestedAction = "Try 'type hello world'"
            )
        }

        // Find the search bar
        val searchBarInfo = findSearchBar(
            avid = command.targetAvid
        ) ?: return HandlerResult.Failure(
            reason = "No search bar found",
            recoverable = true,
            suggestedAction = "Focus on a search bar first"
        )

        // Type the text
        val result = executor.typeText(searchBarInfo, text)

        return if (result.success) {
            Log.i(TAG, "Typed text: '$text'")

            HandlerResult.Success(
                message = "Typed '$text'",
                data = mapOf(
                    "searchBarAvid" to searchBarInfo.avid,
                    "text" to text,
                    "accessibility_announcement" to "Typed $text"
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
     * Handle "filter by [category]" command.
     */
    private suspend fun handleFilterBy(
        normalizedAction: String,
        command: QuantizedCommand
    ): HandlerResult {
        val matchResult = FILTER_BY_PATTERN.find(normalizedAction)
            ?: return HandlerResult.failure("Could not parse filter command")

        val category = matchResult.groupValues[1].trim()

        if (category.isBlank()) {
            return HandlerResult.Failure(
                reason = "Filter category is empty",
                recoverable = true,
                suggestedAction = "Try 'filter by images' or 'filter by videos'"
            )
        }

        // Find the search bar
        val searchBarInfo = findSearchBar(
            avid = command.targetAvid
        ) ?: return HandlerResult.Failure(
            reason = "No search bar found",
            recoverable = true,
            suggestedAction = "Focus on a search bar first"
        )

        // Check if category is available
        if (searchBarInfo.filters.isNotEmpty()) {
            val matchingFilter = searchBarInfo.filters.find {
                it.equals(category, ignoreCase = true)
            }
            if (matchingFilter == null) {
                return HandlerResult.Failure(
                    reason = "Filter '$category' not available",
                    recoverable = true,
                    suggestedAction = "Available filters: ${searchBarInfo.filters.joinToString(", ")}"
                )
            }
        }

        // Apply the filter
        val result = executor.applyFilter(searchBarInfo, category)

        return if (result.success) {
            Log.i(TAG, "Applied filter: '$category'")

            HandlerResult.Success(
                message = "Filtered by '$category'",
                data = mapOf(
                    "searchBarAvid" to searchBarInfo.avid,
                    "filter" to category,
                    "previousFilter" to (searchBarInfo.activeFilter ?: ""),
                    "accessibility_announcement" to "Filtered by $category"
                )
            )
        } else {
            HandlerResult.failure(
                reason = result.error ?: "Could not apply filter",
                recoverable = true
            )
        }
    }

    /**
     * Handle "suggestion [N]" or "search suggestion [N]" command.
     */
    private suspend fun handleSelectSuggestion(
        normalizedAction: String,
        command: QuantizedCommand
    ): HandlerResult {
        val matchResult = SUGGESTION_PATTERN.find(normalizedAction)
            ?: return HandlerResult.failure("Could not parse suggestion command")

        val indexStr = matchResult.groupValues[1].lowercase()
        val index = indexStr.toIntOrNull() ?: WORD_NUMBERS[indexStr]
            ?: return HandlerResult.Failure(
                reason = "Could not parse suggestion number: '$indexStr'",
                recoverable = true,
                suggestedAction = "Try 'suggestion 1' or 'suggestion one'"
            )

        // Find the search bar
        val searchBarInfo = findSearchBar(
            avid = command.targetAvid
        ) ?: return HandlerResult.Failure(
            reason = "No search bar found",
            recoverable = true,
            suggestedAction = "Focus on a search bar first"
        )

        // Check if suggestion exists
        if (searchBarInfo.suggestions.isEmpty()) {
            return HandlerResult.Failure(
                reason = "No suggestions available",
                recoverable = true,
                suggestedAction = "Type in the search bar to see suggestions"
            )
        }

        if (index < 1 || index > searchBarInfo.suggestions.size) {
            return HandlerResult.Failure(
                reason = "Suggestion $index not available",
                recoverable = true,
                suggestedAction = "Available suggestions: 1 to ${searchBarInfo.suggestions.size}"
            )
        }

        // Select the suggestion (1-indexed from user, 0-indexed internally)
        val result = executor.selectSuggestion(searchBarInfo, index - 1)

        return if (result.success) {
            val selectedSuggestion = searchBarInfo.suggestions.getOrNull(index - 1) ?: "suggestion $index"

            Log.i(TAG, "Selected suggestion $index: '$selectedSuggestion'")

            HandlerResult.Success(
                message = "Selected '$selectedSuggestion'",
                data = mapOf(
                    "searchBarAvid" to searchBarInfo.avid,
                    "suggestionIndex" to index,
                    "suggestion" to selectedSuggestion,
                    "accessibility_announcement" to "Selected $selectedSuggestion"
                )
            )
        } else {
            HandlerResult.failure(
                reason = result.error ?: "Could not select suggestion",
                recoverable = true
            )
        }
    }

    /**
     * Handle "search" / "go" / "submit" command to submit current search.
     */
    private suspend fun handleSubmitSearch(command: QuantizedCommand): HandlerResult {
        // Find the search bar
        val searchBarInfo = findSearchBar(
            avid = command.targetAvid
        ) ?: return HandlerResult.Failure(
            reason = "No search bar found",
            recoverable = true,
            suggestedAction = "Focus on a search bar first"
        )

        val currentQuery = searchBarInfo.currentQuery

        if (currentQuery.isNullOrBlank()) {
            return HandlerResult.Failure(
                reason = "Search bar is empty",
                recoverable = true,
                suggestedAction = "Type something first or say 'search for [query]'"
            )
        }

        // Submit the current search
        val result = executor.executeSearch(searchBarInfo, currentQuery)

        return if (result.success) {
            onSearchExecuted?.invoke(currentQuery)

            Log.i(TAG, "Submitted search: '$currentQuery'")

            HandlerResult.Success(
                message = "Searching for '$currentQuery'",
                data = mapOf(
                    "searchBarAvid" to searchBarInfo.avid,
                    "query" to currentQuery,
                    "accessibility_announcement" to "Searching for $currentQuery"
                )
            )
        } else {
            HandlerResult.failure(
                reason = result.error ?: "Could not submit search",
                recoverable = true
            )
        }
    }

    /**
     * Handle "clear search" / "clear" command.
     */
    private suspend fun handleClearSearch(command: QuantizedCommand): HandlerResult {
        // Find the search bar
        val searchBarInfo = findSearchBar(
            avid = command.targetAvid
        ) ?: return HandlerResult.Failure(
            reason = "No search bar found",
            recoverable = true,
            suggestedAction = "Focus on a search bar first"
        )

        // Clear the search
        val result = executor.clearSearch(searchBarInfo)

        return if (result.success) {
            Log.i(TAG, "Cleared search")

            HandlerResult.Success(
                message = "Search cleared",
                data = mapOf(
                    "searchBarAvid" to searchBarInfo.avid,
                    "previousQuery" to (searchBarInfo.currentQuery ?: ""),
                    "accessibility_announcement" to "Search cleared"
                )
            )
        } else {
            HandlerResult.failure(
                reason = result.error ?: "Could not clear search",
                recoverable = true
            )
        }
    }

    /**
     * Handle "voice search" command.
     */
    private suspend fun handleVoiceSearch(command: QuantizedCommand): HandlerResult {
        // Find the search bar
        val searchBarInfo = findSearchBar(
            avid = command.targetAvid
        ) ?: return HandlerResult.Failure(
            reason = "No search bar found",
            recoverable = true,
            suggestedAction = "Focus on a search bar first"
        )

        // Check if voice search is available
        if (!searchBarInfo.hasVoiceSearch) {
            return HandlerResult.Failure(
                reason = "Voice search not available",
                recoverable = false,
                suggestedAction = "This search bar does not support voice input"
            )
        }

        // Activate voice search
        val result = executor.activateVoiceSearch(searchBarInfo)

        return if (result.success) {
            Log.i(TAG, "Activated voice search")

            HandlerResult.Success(
                message = "Voice search activated",
                data = mapOf(
                    "searchBarAvid" to searchBarInfo.avid,
                    "accessibility_announcement" to "Voice search activated, speak now"
                )
            )
        } else {
            HandlerResult.failure(
                reason = result.error ?: "Could not activate voice search",
                recoverable = true
            )
        }
    }

    /**
     * Handle "recent searches" / "show history" command.
     */
    private suspend fun handleShowHistory(command: QuantizedCommand): HandlerResult {
        // Find the search bar
        val searchBarInfo = findSearchBar(
            avid = command.targetAvid
        ) ?: return HandlerResult.Failure(
            reason = "No search bar found",
            recoverable = true,
            suggestedAction = "Focus on a search bar first"
        )

        // Show search history
        val result = executor.showHistory(searchBarInfo)

        return if (result.success) {
            Log.i(TAG, "Showing search history")

            HandlerResult.Success(
                message = "Showing recent searches",
                data = mapOf(
                    "searchBarAvid" to searchBarInfo.avid,
                    "accessibility_announcement" to "Showing recent searches"
                )
            )
        } else {
            HandlerResult.failure(
                reason = result.error ?: "Could not show search history",
                recoverable = true
            )
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Helper Methods
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Find search bar by name, AVID, or focus state.
     */
    private suspend fun findSearchBar(
        name: String? = null,
        avid: String? = null
    ): SearchBarInfo? {
        // Priority 1: AVID lookup (fastest, most precise)
        if (avid != null) {
            val searchBar = executor.findByAvid(avid)
            if (searchBar != null) return searchBar
        }

        // Priority 2: Name lookup
        if (name != null) {
            val searchBar = executor.findByName(name)
            if (searchBar != null) return searchBar
        }

        // Priority 3: Focused search bar
        return executor.findFocused()
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Voice Phrases for Speech Engine Registration
    // ═══════════════════════════════════════════════════════════════════════════

    override fun getVoicePhrases(): List<String> {
        return listOf(
            "search for",
            "find",
            "search",
            "go",
            "submit",
            "type",
            "clear search",
            "clear",
            "voice search",
            "filter by",
            "recent searches",
            "show history",
            "suggestion",
            "search suggestion"
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// Supporting Types
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * Information about a search bar component.
 *
 * @property avid AVID fingerprint for the search bar (format: SBR:{hash8})
 * @property name Display name or associated label
 * @property currentQuery Current text in the search input
 * @property suggestions List of current search suggestions
 * @property filters Available filter categories
 * @property activeFilter Currently applied filter (null if none)
 * @property hasVoiceSearch Whether voice search is available
 * @property bounds Screen bounds for the search bar
 * @property isFocused Whether this search bar currently has focus
 * @property node Platform-specific node reference
 */
data class SearchBarInfo(
    val avid: String,
    val name: String = "",
    val currentQuery: String? = null,
    val suggestions: List<String> = emptyList(),
    val filters: List<String> = emptyList(),
    val activeFilter: String? = null,
    val hasVoiceSearch: Boolean = false,
    val bounds: Bounds = Bounds.EMPTY,
    val isFocused: Boolean = false,
    val node: Any? = null
) {
    /**
     * Convert to ElementInfo for general element operations.
     */
    fun toElementInfo(): ElementInfo = ElementInfo(
        className = "SearchBar",
        text = name,
        bounds = bounds,
        isClickable = true,
        isEnabled = true,
        avid = avid,
        stateDescription = currentQuery ?: ""
    )
}

/**
 * Result of a search bar operation.
 */
data class SearchBarOperationResult(
    val success: Boolean,
    val error: String? = null,
    val data: Map<String, Any>? = null
) {
    companion object {
        fun success(data: Map<String, Any>? = null) = SearchBarOperationResult(
            success = true,
            data = data
        )

        fun error(message: String) = SearchBarOperationResult(
            success = false,
            error = message
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// Platform Executor Interface
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * Platform-specific executor for search bar operations.
 *
 * Implementations should:
 * 1. Find search bar components by AVID, name, or focus state
 * 2. Read current search text and suggestions
 * 3. Execute search queries via accessibility actions
 * 4. Handle various search bar implementations (SearchView, EditText with search icon, etc.)
 *
 * ## Search Bar Detection Algorithm
 *
 * ```kotlin
 * fun findSearchBarNode(rootNode: AccessibilityNodeInfo): AccessibilityNodeInfo? {
 *     // Look for nodes with className:
 *     // - android.widget.SearchView
 *     // - androidx.appcompat.widget.SearchView
 *     // - android.widget.EditText (with search hint or icon)
 *     // - Custom search implementations
 *     // Also check for:
 *     // - contentDescription containing "search"
 *     // - hint text containing "search"
 *     // - associated search icon button
 * }
 * ```
 *
 * ## Search Execution Algorithm
 *
 * ```kotlin
 * fun executeSearch(node: AccessibilityNodeInfo, query: String): Boolean {
 *     // 1. Set text using ACTION_SET_TEXT
 *     // 2. Dispatch IME action (ACTION_SUBMIT or EditorInfo.IME_ACTION_SEARCH)
 *     // 3. Or click associated search button
 * }
 * ```
 */
interface SearchBarExecutor {

    // ═══════════════════════════════════════════════════════════════════════════
    // Search Bar Discovery
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Find a search bar by its AVID fingerprint.
     *
     * @param avid The AVID fingerprint (format: SBR:{hash8})
     * @return SearchBarInfo if found, null otherwise
     */
    suspend fun findByAvid(avid: String): SearchBarInfo?

    /**
     * Find a search bar by its name or associated label.
     *
     * Searches for:
     * 1. SearchView with matching contentDescription
     * 2. Search input with hint text matching name
     * 3. Search bar with associated label nearby
     *
     * @param name The name to search for (case-insensitive)
     * @return SearchBarInfo if found, null otherwise
     */
    suspend fun findByName(name: String): SearchBarInfo?

    /**
     * Find the currently focused search bar.
     *
     * @return SearchBarInfo if a search bar has focus, null otherwise
     */
    suspend fun findFocused(): SearchBarInfo?

    // ═══════════════════════════════════════════════════════════════════════════
    // Search Operations
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Execute a search with the given query.
     *
     * Sets the search text and submits the search.
     *
     * @param searchBar The search bar to use
     * @param query The search query to execute
     * @return Operation result
     */
    suspend fun executeSearch(searchBar: SearchBarInfo, query: String): SearchBarOperationResult

    /**
     * Clear the current search input.
     *
     * @param searchBar The search bar to clear
     * @return Operation result
     */
    suspend fun clearSearch(searchBar: SearchBarInfo): SearchBarOperationResult

    /**
     * Type text into the search bar without submitting.
     *
     * @param searchBar The search bar to type into
     * @param text The text to type
     * @return Operation result
     */
    suspend fun typeText(searchBar: SearchBarInfo, text: String): SearchBarOperationResult

    // ═══════════════════════════════════════════════════════════════════════════
    // Voice Search Operations
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Activate voice search mode.
     *
     * Triggers the search bar's voice input feature if available.
     *
     * @param searchBar The search bar to activate voice search on
     * @return Operation result
     */
    suspend fun activateVoiceSearch(searchBar: SearchBarInfo): SearchBarOperationResult

    // ═══════════════════════════════════════════════════════════════════════════
    // Filter Operations
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Apply a search filter.
     *
     * @param searchBar The search bar to filter
     * @param filter The filter category to apply
     * @return Operation result
     */
    suspend fun applyFilter(searchBar: SearchBarInfo, filter: String): SearchBarOperationResult

    // ═══════════════════════════════════════════════════════════════════════════
    // History Operations
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Show search history.
     *
     * Displays recent searches, typically by focusing the search bar
     * and showing the history dropdown.
     *
     * @param searchBar The search bar to show history for
     * @return Operation result
     */
    suspend fun showHistory(searchBar: SearchBarInfo): SearchBarOperationResult

    // ═══════════════════════════════════════════════════════════════════════════
    // Suggestion Operations
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Select a search suggestion by index.
     *
     * @param searchBar The search bar with suggestions
     * @param index Zero-based index of the suggestion to select
     * @return Operation result
     */
    suspend fun selectSuggestion(searchBar: SearchBarInfo, index: Int): SearchBarOperationResult
}
