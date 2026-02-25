/**
 * IconPickerHandler.kt - Voice handler for Icon Picker interactions
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Created: 2026-01-28
 *
 * Purpose: Voice-driven icon picker control with selection, search, and navigation
 * Features:
 * - Select icons by name or index
 * - Search for icons using voice queries
 * - Navigate through icons (next/previous)
 * - Navigate through pages of icons
 * - Filter icons by category
 * - Clear selection and show all icons
 * - AVID-based targeting for precise element selection
 * - Voice feedback for selections and operations
 *
 * Location: CommandManager module handlers
 *
 * ## Supported Commands
 *
 * Selection:
 * - "select [icon name]" - Select icon by name
 * - "choose [icon name]" - Select icon by name (alias)
 * - "icon [N]" - Select Nth icon (1-indexed)
 *
 * Search:
 * - "search [query]" - Search for icons matching query
 * - "find [query]" - Search for icons matching query (alias)
 *
 * Navigation:
 * - "next" - Navigate to next icon
 * - "previous" / "back" - Navigate to previous icon
 * - "next page" - Navigate to next page of icons
 * - "previous page" - Navigate to previous page of icons
 *
 * Filtering:
 * - "category [name]" - Filter icons by category
 * - "show all" / "all icons" - Show all icons (clear filter)
 *
 * Selection Management:
 * - "clear selection" / "deselect" - Clear selected icon
 *
 * ## Icon Resolution
 *
 * Icons are resolved by:
 * 1. Exact name match (case-insensitive)
 * 2. Partial name match (starts with)
 * 3. Index-based selection (1-indexed)
 * 4. Fuzzy matching for common variations
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
 * Voice command handler for Icon Picker interactions.
 *
 * Provides comprehensive voice control for icon picker components including:
 * - Icon selection by name or index
 * - Search functionality for finding icons
 * - Navigation between icons and pages
 * - Category-based filtering
 * - Selection management (clear, deselect)
 *
 * Thread Safety:
 * - All operations are suspend functions
 * - State modifications are atomic via executor
 *
 * @param executor Platform-specific executor for icon picker operations
 */
class IconPickerHandler(
    private val executor: IconPickerExecutor
) : BaseHandler() {

    companion object {
        private const val TAG = "IconPickerHandler"
        private val Log = LoggerFactory.getLogger(TAG)

        // Patterns for parsing commands
        private val SELECT_PATTERN = Regex(
            """^(?:select|choose)\s+(.+)$""",
            RegexOption.IGNORE_CASE
        )

        private val SEARCH_PATTERN = Regex(
            """^(?:search|find)\s+(.+)$""",
            RegexOption.IGNORE_CASE
        )

        private val ICON_INDEX_PATTERN = Regex(
            """^icon\s+(\d+)$""",
            RegexOption.IGNORE_CASE
        )

        private val CATEGORY_PATTERN = Regex(
            """^category\s+(.+)$""",
            RegexOption.IGNORE_CASE
        )

        // Word to number mapping for common spoken numbers
        private val WORD_NUMBERS = mapOf(
            "one" to 1, "two" to 2, "three" to 3, "four" to 4, "five" to 5,
            "six" to 6, "seven" to 7, "eight" to 8, "nine" to 9, "ten" to 10,
            "eleven" to 11, "twelve" to 12, "thirteen" to 13, "fourteen" to 14,
            "fifteen" to 15, "sixteen" to 16, "seventeen" to 17, "eighteen" to 18,
            "nineteen" to 19, "twenty" to 20, "first" to 1, "second" to 2,
            "third" to 3, "fourth" to 4, "fifth" to 5, "sixth" to 6,
            "seventh" to 7, "eighth" to 8, "ninth" to 9, "tenth" to 10
        )
    }

    override val category: ActionCategory = ActionCategory.UI

    override val supportedActions: List<String> = listOf(
        // Selection
        "select", "choose",
        // Search
        "search", "find",
        // Index selection
        "icon",
        // Navigation
        "next", "previous", "back",
        "next page", "previous page",
        // Category filtering
        "category",
        // Clear filter
        "show all", "all icons",
        // Deselection
        "clear selection", "deselect"
    )

    /**
     * Callback for voice feedback when icon selection changes.
     */
    var onIconSelected: ((iconName: String, category: String?) -> Unit)? = null

    /**
     * Callback for voice feedback when search is performed.
     */
    var onSearchPerformed: ((query: String, resultCount: Int) -> Unit)? = null

    override suspend fun execute(
        command: QuantizedCommand,
        params: Map<String, Any>
    ): HandlerResult {
        val normalizedAction = command.phrase.lowercase().trim()

        Log.d { "Processing icon picker command: $normalizedAction" }

        return try {
            when {
                // Select icon by name: "select [name]" or "choose [name]"
                SELECT_PATTERN.containsMatchIn(normalizedAction) -> {
                    handleSelectByName(normalizedAction, command)
                }

                // Search for icons: "search [query]" or "find [query]"
                SEARCH_PATTERN.containsMatchIn(normalizedAction) -> {
                    handleSearch(normalizedAction, command)
                }

                // Select icon by index: "icon [N]"
                ICON_INDEX_PATTERN.containsMatchIn(normalizedAction) -> {
                    handleSelectByIndex(normalizedAction, command)
                }

                // Category filter: "category [name]"
                CATEGORY_PATTERN.containsMatchIn(normalizedAction) -> {
                    handleCategoryFilter(normalizedAction, command)
                }

                // Navigation: next icon
                normalizedAction == "next" -> {
                    handleNext(command)
                }

                // Navigation: previous icon
                normalizedAction in listOf("previous", "back") -> {
                    handlePrevious(command)
                }

                // Navigation: next page
                normalizedAction == "next page" -> {
                    handleNextPage(command)
                }

                // Navigation: previous page
                normalizedAction == "previous page" -> {
                    handlePreviousPage(command)
                }

                // Clear selection
                normalizedAction in listOf("clear selection", "deselect") -> {
                    handleClearSelection(command)
                }

                // Show all icons (clear filter)
                normalizedAction in listOf("show all", "all icons") -> {
                    handleShowAll(command)
                }

                else -> HandlerResult.notHandled()
            }
        } catch (e: Exception) {
            Log.e({ "Error executing icon picker command" }, e)
            HandlerResult.failure("Error: ${e.message}", recoverable = true)
        }
    }

    // ===============================================================================
    // Command Handlers
    // ===============================================================================

    /**
     * Handle "select [name]" or "choose [name]" command.
     */
    private suspend fun handleSelectByName(
        normalizedAction: String,
        command: QuantizedCommand
    ): HandlerResult {
        val matchResult = SELECT_PATTERN.find(normalizedAction)
            ?: return HandlerResult.failure("Could not parse select command")

        val iconName = matchResult.groupValues[1].trim()

        // Find the icon picker
        val pickerInfo = findIconPicker(
            avid = command.targetAvid
        ) ?: return HandlerResult.Failure(
            reason = "No icon picker focused",
            recoverable = true,
            suggestedAction = "Focus on an icon picker first"
        )

        // Select the icon by name
        val result = executor.selectIcon(pickerInfo, iconName)

        return if (result.success) {
            // Invoke callback for voice feedback
            onIconSelected?.invoke(
                result.selectedIconName ?: iconName,
                result.selectedIconCategory
            )

            val feedback = buildString {
                append("Selected ")
                append(result.selectedIconName ?: iconName)
                if (result.selectedIconCategory != null) {
                    append(" from ")
                    append(result.selectedIconCategory)
                }
            }

            Log.i { "Icon selected: ${result.selectedIconName}" }

            HandlerResult.Success(
                message = feedback,
                data = mapOf(
                    "pickerAvid" to pickerInfo.avid,
                    "iconName" to (result.selectedIconName ?: iconName),
                    "iconCategory" to (result.selectedIconCategory ?: ""),
                    "iconId" to (result.selectedIconId ?: ""),
                    "accessibility_announcement" to feedback
                )
            )
        } else {
            HandlerResult.Failure(
                reason = result.error ?: "Could not select icon '$iconName'",
                recoverable = true,
                suggestedAction = "Try 'search $iconName' to find similar icons"
            )
        }
    }

    /**
     * Handle "search [query]" or "find [query]" command.
     */
    private suspend fun handleSearch(
        normalizedAction: String,
        command: QuantizedCommand
    ): HandlerResult {
        val matchResult = SEARCH_PATTERN.find(normalizedAction)
            ?: return HandlerResult.failure("Could not parse search command")

        val query = matchResult.groupValues[1].trim()

        // Find the icon picker
        val pickerInfo = findIconPicker(
            avid = command.targetAvid
        ) ?: return HandlerResult.Failure(
            reason = "No icon picker focused",
            recoverable = true,
            suggestedAction = "Focus on an icon picker first"
        )

        // Perform the search
        val result = executor.searchIcons(pickerInfo, query)

        return if (result.success) {
            val resultCount = result.resultCount ?: 0

            // Invoke callback for voice feedback
            onSearchPerformed?.invoke(query, resultCount)

            val feedback = when {
                resultCount == 0 -> "No icons found for '$query'"
                resultCount == 1 -> "Found 1 icon for '$query'"
                else -> "Found $resultCount icons for '$query'"
            }

            Log.i { "Search performed: '$query' returned $resultCount results" }

            HandlerResult.Success(
                message = feedback,
                data = mapOf(
                    "pickerAvid" to pickerInfo.avid,
                    "searchQuery" to query,
                    "resultCount" to resultCount,
                    "accessibility_announcement" to feedback
                )
            )
        } else {
            HandlerResult.failure(
                reason = result.error ?: "Could not search for '$query'",
                recoverable = true
            )
        }
    }

    /**
     * Handle "icon [N]" command to select icon by index.
     */
    private suspend fun handleSelectByIndex(
        normalizedAction: String,
        command: QuantizedCommand
    ): HandlerResult {
        val matchResult = ICON_INDEX_PATTERN.find(normalizedAction)
            ?: return HandlerResult.failure("Could not parse icon index command")

        val indexString = matchResult.groupValues[1]
        val index = parseIndex(indexString)
            ?: return HandlerResult.Failure(
                reason = "Could not parse index: '$indexString'",
                recoverable = true,
                suggestedAction = "Try 'icon 1' or 'icon 5'"
            )

        // Find the icon picker
        val pickerInfo = findIconPicker(
            avid = command.targetAvid
        ) ?: return HandlerResult.Failure(
            reason = "No icon picker focused",
            recoverable = true,
            suggestedAction = "Focus on an icon picker first"
        )

        // Validate index is within bounds
        if (index < 1 || index > pickerInfo.icons.size) {
            return HandlerResult.Failure(
                reason = "Icon index $index is out of range (1-${pickerInfo.icons.size})",
                recoverable = true,
                suggestedAction = "Try 'icon 1' through 'icon ${pickerInfo.icons.size}'"
            )
        }

        // Select by index (1-indexed)
        val result = executor.selectByIndex(pickerInfo, index)

        return if (result.success) {
            onIconSelected?.invoke(
                result.selectedIconName ?: "Icon $index",
                result.selectedIconCategory
            )

            val feedback = buildString {
                append("Selected ")
                append(result.selectedIconName ?: "icon $index")
            }

            Log.i { "Icon selected by index: $index -> ${result.selectedIconName}" }

            HandlerResult.Success(
                message = feedback,
                data = mapOf(
                    "pickerAvid" to pickerInfo.avid,
                    "selectedIndex" to index,
                    "iconName" to (result.selectedIconName ?: ""),
                    "iconCategory" to (result.selectedIconCategory ?: ""),
                    "accessibility_announcement" to feedback
                )
            )
        } else {
            HandlerResult.failure(
                reason = result.error ?: "Could not select icon at index $index",
                recoverable = true
            )
        }
    }

    /**
     * Handle "category [name]" command to filter by category.
     */
    private suspend fun handleCategoryFilter(
        normalizedAction: String,
        command: QuantizedCommand
    ): HandlerResult {
        val matchResult = CATEGORY_PATTERN.find(normalizedAction)
            ?: return HandlerResult.failure("Could not parse category command")

        val categoryName = matchResult.groupValues[1].trim()

        // Find the icon picker
        val pickerInfo = findIconPicker(
            avid = command.targetAvid
        ) ?: return HandlerResult.Failure(
            reason = "No icon picker focused",
            recoverable = true,
            suggestedAction = "Focus on an icon picker first"
        )

        // Check if category exists
        val matchedCategory = pickerInfo.categories.find {
            it.equals(categoryName, ignoreCase = true)
        } ?: pickerInfo.categories.find {
            it.lowercase().contains(categoryName.lowercase())
        }

        if (matchedCategory == null) {
            return HandlerResult.Failure(
                reason = "Category '$categoryName' not found",
                recoverable = true,
                suggestedAction = "Available categories: ${pickerInfo.categories.joinToString(", ")}"
            )
        }

        // Apply category filter
        val result = executor.filterByCategory(pickerInfo, matchedCategory)

        return if (result.success) {
            val feedback = "Showing icons in category '$matchedCategory'"

            Log.i { "Category filter applied: $matchedCategory" }

            HandlerResult.Success(
                message = feedback,
                data = mapOf(
                    "pickerAvid" to pickerInfo.avid,
                    "category" to matchedCategory,
                    "iconCount" to (result.resultCount ?: 0),
                    "accessibility_announcement" to feedback
                )
            )
        } else {
            HandlerResult.failure(
                reason = result.error ?: "Could not filter by category '$categoryName'",
                recoverable = true
            )
        }
    }

    /**
     * Handle "next" command to navigate to next icon.
     */
    private suspend fun handleNext(command: QuantizedCommand): HandlerResult {
        val pickerInfo = findIconPicker(avid = command.targetAvid)
            ?: return HandlerResult.Failure(
                reason = "No icon picker focused",
                recoverable = true,
                suggestedAction = "Focus on an icon picker first"
            )

        val result = executor.next(pickerInfo)

        return if (result.success) {
            val feedback = result.selectedIconName?.let { "Selected $it" } ?: "Moved to next icon"

            Log.i { "Navigated to next icon: ${result.selectedIconName}" }

            HandlerResult.Success(
                message = feedback,
                data = mapOf(
                    "pickerAvid" to pickerInfo.avid,
                    "iconName" to (result.selectedIconName ?: ""),
                    "accessibility_announcement" to feedback
                )
            )
        } else {
            HandlerResult.Failure(
                reason = result.error ?: "Cannot navigate to next icon",
                recoverable = true,
                suggestedAction = "You may be at the last icon. Try 'next page' or 'previous'"
            )
        }
    }

    /**
     * Handle "previous" or "back" command to navigate to previous icon.
     */
    private suspend fun handlePrevious(command: QuantizedCommand): HandlerResult {
        val pickerInfo = findIconPicker(avid = command.targetAvid)
            ?: return HandlerResult.Failure(
                reason = "No icon picker focused",
                recoverable = true,
                suggestedAction = "Focus on an icon picker first"
            )

        val result = executor.previous(pickerInfo)

        return if (result.success) {
            val feedback = result.selectedIconName?.let { "Selected $it" } ?: "Moved to previous icon"

            Log.i { "Navigated to previous icon: ${result.selectedIconName}" }

            HandlerResult.Success(
                message = feedback,
                data = mapOf(
                    "pickerAvid" to pickerInfo.avid,
                    "iconName" to (result.selectedIconName ?: ""),
                    "accessibility_announcement" to feedback
                )
            )
        } else {
            HandlerResult.Failure(
                reason = result.error ?: "Cannot navigate to previous icon",
                recoverable = true,
                suggestedAction = "You may be at the first icon. Try 'previous page' or 'next'"
            )
        }
    }

    /**
     * Handle "next page" command to navigate to next page of icons.
     */
    private suspend fun handleNextPage(command: QuantizedCommand): HandlerResult {
        val pickerInfo = findIconPicker(avid = command.targetAvid)
            ?: return HandlerResult.Failure(
                reason = "No icon picker focused",
                recoverable = true,
                suggestedAction = "Focus on an icon picker first"
            )

        if (pickerInfo.currentPage >= pickerInfo.totalPages) {
            return HandlerResult.Failure(
                reason = "Already on the last page",
                recoverable = true,
                suggestedAction = "Try 'previous page' or 'show all'"
            )
        }

        val result = executor.nextPage(pickerInfo)

        return if (result.success) {
            val newPage = pickerInfo.currentPage + 1
            val feedback = "Page $newPage of ${pickerInfo.totalPages}"

            Log.i { "Navigated to next page: $newPage" }

            HandlerResult.Success(
                message = feedback,
                data = mapOf(
                    "pickerAvid" to pickerInfo.avid,
                    "currentPage" to newPage,
                    "totalPages" to pickerInfo.totalPages,
                    "accessibility_announcement" to feedback
                )
            )
        } else {
            HandlerResult.failure(
                reason = result.error ?: "Could not navigate to next page",
                recoverable = true
            )
        }
    }

    /**
     * Handle "previous page" command to navigate to previous page of icons.
     */
    private suspend fun handlePreviousPage(command: QuantizedCommand): HandlerResult {
        val pickerInfo = findIconPicker(avid = command.targetAvid)
            ?: return HandlerResult.Failure(
                reason = "No icon picker focused",
                recoverable = true,
                suggestedAction = "Focus on an icon picker first"
            )

        if (pickerInfo.currentPage <= 1) {
            return HandlerResult.Failure(
                reason = "Already on the first page",
                recoverable = true,
                suggestedAction = "Try 'next page' or 'show all'"
            )
        }

        val result = executor.previousPage(pickerInfo)

        return if (result.success) {
            val newPage = pickerInfo.currentPage - 1
            val feedback = "Page $newPage of ${pickerInfo.totalPages}"

            Log.i { "Navigated to previous page: $newPage" }

            HandlerResult.Success(
                message = feedback,
                data = mapOf(
                    "pickerAvid" to pickerInfo.avid,
                    "currentPage" to newPage,
                    "totalPages" to pickerInfo.totalPages,
                    "accessibility_announcement" to feedback
                )
            )
        } else {
            HandlerResult.failure(
                reason = result.error ?: "Could not navigate to previous page",
                recoverable = true
            )
        }
    }

    /**
     * Handle "clear selection" or "deselect" command.
     */
    private suspend fun handleClearSelection(command: QuantizedCommand): HandlerResult {
        val pickerInfo = findIconPicker(avid = command.targetAvid)
            ?: return HandlerResult.Failure(
                reason = "No icon picker focused",
                recoverable = true,
                suggestedAction = "Focus on an icon picker first"
            )

        if (pickerInfo.selectedIcon == null) {
            return HandlerResult.Success(
                message = "No icon selected",
                data = mapOf(
                    "pickerAvid" to pickerInfo.avid,
                    "accessibility_announcement" to "No icon selected"
                )
            )
        }

        val result = executor.clearSelection(pickerInfo)

        return if (result.success) {
            val feedback = "Selection cleared"

            Log.i { "Selection cleared" }

            HandlerResult.Success(
                message = feedback,
                data = mapOf(
                    "pickerAvid" to pickerInfo.avid,
                    "previousSelection" to (pickerInfo.selectedIcon?.name ?: ""),
                    "accessibility_announcement" to feedback
                )
            )
        } else {
            HandlerResult.failure(
                reason = result.error ?: "Could not clear selection",
                recoverable = true
            )
        }
    }

    /**
     * Handle "show all" or "all icons" command to clear filters.
     */
    private suspend fun handleShowAll(command: QuantizedCommand): HandlerResult {
        val pickerInfo = findIconPicker(avid = command.targetAvid)
            ?: return HandlerResult.Failure(
                reason = "No icon picker focused",
                recoverable = true,
                suggestedAction = "Focus on an icon picker first"
            )

        val result = executor.showAll(pickerInfo)

        return if (result.success) {
            val feedback = "Showing all icons"

            Log.i { "Showing all icons (filters cleared)" }

            HandlerResult.Success(
                message = feedback,
                data = mapOf(
                    "pickerAvid" to pickerInfo.avid,
                    "totalIcons" to (result.resultCount ?: pickerInfo.icons.size),
                    "accessibility_announcement" to feedback
                )
            )
        } else {
            HandlerResult.failure(
                reason = result.error ?: "Could not show all icons",
                recoverable = true
            )
        }
    }

    // ===============================================================================
    // Helper Methods
    // ===============================================================================

    /**
     * Find icon picker by AVID or focus state.
     */
    private suspend fun findIconPicker(
        avid: String? = null,
        name: String? = null
    ): IconPickerInfo? {
        // Priority 1: AVID lookup (fastest, most precise)
        if (avid != null) {
            val picker = executor.findByAvid(avid)
            if (picker != null) return picker
        }

        // Priority 2: Name lookup
        if (name != null) {
            val picker = executor.findByName(name)
            if (picker != null) return picker
        }

        // Priority 3: Focused picker
        return executor.findFocused()
    }

    /**
     * Parse an index string into an integer.
     *
     * Supports:
     * - "1", "5", "10" (numeric)
     * - "one", "five", "ten" (word numbers)
     * - "first", "second", "third" (ordinals)
     */
    private fun parseIndex(input: String): Int? {
        val trimmed = input.trim().lowercase()

        // Try direct numeric parsing
        trimmed.toIntOrNull()?.let { return it }

        // Try word number parsing
        WORD_NUMBERS[trimmed]?.let { return it }

        return null
    }

}

// ===================================================================================
// Supporting Types
// ===================================================================================

/**
 * Information about an icon picker component.
 *
 * @property avid AVID fingerprint for the icon picker (format: ICP:{hash8})
 * @property name Display name or associated label
 * @property selectedIcon Currently selected icon, if any
 * @property icons List of available icons in current view
 * @property categories List of available categories for filtering
 * @property activeCategory Currently active category filter, if any
 * @property searchQuery Current search query, if any
 * @property currentPage Current page number (1-indexed)
 * @property totalPages Total number of pages
 * @property bounds Screen bounds for the icon picker
 * @property isFocused Whether this icon picker currently has focus
 * @property node Platform-specific node reference
 */
data class IconPickerInfo(
    val avid: String,
    val name: String = "",
    val selectedIcon: IconInfo? = null,
    val icons: List<IconInfo> = emptyList(),
    val categories: List<String> = emptyList(),
    val activeCategory: String? = null,
    val searchQuery: String? = null,
    val currentPage: Int = 1,
    val totalPages: Int = 1,
    val bounds: Bounds = Bounds.EMPTY,
    val isFocused: Boolean = false,
    val node: Any? = null
) {
    /**
     * Convert to ElementInfo for general element operations.
     */
    fun toElementInfo(): ElementInfo = ElementInfo(
        className = "IconPicker",
        text = name,
        bounds = bounds,
        isClickable = true,
        isEnabled = true,
        avid = avid,
        stateDescription = buildString {
            selectedIcon?.let { append("Selected: ${it.name}") }
            if (searchQuery != null) append(" | Search: $searchQuery")
            if (activeCategory != null) append(" | Category: $activeCategory")
            append(" | Page $currentPage of $totalPages")
        }
    )
}

/**
 * Information about an individual icon.
 *
 * @property id Unique identifier for the icon
 * @property name Display name of the icon
 * @property category Category the icon belongs to
 * @property imageResource Resource identifier or URL for the icon image
 */
data class IconInfo(
    val id: String,
    val name: String,
    val category: String? = null,
    val imageResource: String? = null
)

/**
 * Result of an icon picker operation.
 *
 * @property success Whether the operation succeeded
 * @property error Error message if operation failed
 * @property selectedIconId ID of the selected icon
 * @property selectedIconName Name of the selected icon
 * @property selectedIconCategory Category of the selected icon
 * @property resultCount Number of results (for search/filter operations)
 */
data class IconPickerOperationResult(
    val success: Boolean,
    val error: String? = null,
    val selectedIconId: String? = null,
    val selectedIconName: String? = null,
    val selectedIconCategory: String? = null,
    val resultCount: Int? = null
) {
    companion object {
        fun success(
            iconId: String? = null,
            iconName: String? = null,
            iconCategory: String? = null,
            resultCount: Int? = null
        ) = IconPickerOperationResult(
            success = true,
            selectedIconId = iconId,
            selectedIconName = iconName,
            selectedIconCategory = iconCategory,
            resultCount = resultCount
        )

        fun error(message: String) = IconPickerOperationResult(
            success = false,
            error = message
        )
    }
}

// ===================================================================================
// Platform Executor Interface
// ===================================================================================

/**
 * Platform-specific executor for icon picker operations.
 *
 * Implementations should:
 * 1. Find icon picker components by AVID, name, or focus state
 * 2. Read icon picker state (selected icon, available icons, categories)
 * 3. Perform icon selection, search, and navigation operations
 * 4. Handle various icon picker implementations (grid, list, carousel)
 *
 * ## Icon Picker Detection Algorithm
 *
 * ```kotlin
 * fun findIconPickerNode(rootNode: AccessibilityNodeInfo): AccessibilityNodeInfo? {
 *     // Look for nodes with:
 *     // - RecyclerView/GridView containing image items
 *     // - Custom IconPicker implementations
 *     // - ViewPager with icon grid pages
 *     // - Nodes with contentDescription containing "icon picker"
 * }
 * ```
 *
 * ## Icon Selection Algorithm
 *
 * ```kotlin
 * fun selectIcon(picker: IconPickerInfo, iconName: String): Boolean {
 *     // 1. Search icons by exact name match (case-insensitive)
 *     // 2. Fall back to partial match (starts with)
 *     // 3. Fall back to fuzzy match
 *     // 4. Perform click action on matched icon node
 * }
 * ```
 */
interface IconPickerExecutor {

    // ===============================================================================
    // Icon Picker Discovery
    // ===============================================================================

    /**
     * Find an icon picker by its AVID fingerprint.
     *
     * @param avid The AVID fingerprint (format: ICP:{hash8})
     * @return IconPickerInfo if found, null otherwise
     */
    suspend fun findByAvid(avid: String): IconPickerInfo?

    /**
     * Find an icon picker by its name or associated label.
     *
     * Searches for:
     * 1. Icon picker with matching contentDescription
     * 2. Icon picker with label text matching name
     * 3. Icon picker with associated TextView label nearby
     *
     * @param name The name to search for (case-insensitive)
     * @return IconPickerInfo if found, null otherwise
     */
    suspend fun findByName(name: String): IconPickerInfo?

    /**
     * Find the currently focused icon picker.
     *
     * @return IconPickerInfo if an icon picker has focus, null otherwise
     */
    suspend fun findFocused(): IconPickerInfo?

    // ===============================================================================
    // Selection Operations
    // ===============================================================================

    /**
     * Select an icon by name.
     *
     * @param picker The icon picker to operate on
     * @param iconName The name of the icon to select
     * @return Operation result with selected icon details
     */
    suspend fun selectIcon(picker: IconPickerInfo, iconName: String): IconPickerOperationResult

    /**
     * Select an icon by index (1-indexed).
     *
     * @param picker The icon picker to operate on
     * @param index The 1-indexed position of the icon to select
     * @return Operation result with selected icon details
     */
    suspend fun selectByIndex(picker: IconPickerInfo, index: Int): IconPickerOperationResult

    // ===============================================================================
    // Search Operations
    // ===============================================================================

    /**
     * Search for icons matching a query.
     *
     * @param picker The icon picker to operate on
     * @param query The search query
     * @return Operation result with result count
     */
    suspend fun searchIcons(picker: IconPickerInfo, query: String): IconPickerOperationResult

    // ===============================================================================
    // Navigation Operations
    // ===============================================================================

    /**
     * Navigate to the next icon.
     *
     * @param picker The icon picker to operate on
     * @return Operation result with newly selected icon details
     */
    suspend fun next(picker: IconPickerInfo): IconPickerOperationResult

    /**
     * Navigate to the previous icon.
     *
     * @param picker The icon picker to operate on
     * @return Operation result with newly selected icon details
     */
    suspend fun previous(picker: IconPickerInfo): IconPickerOperationResult

    /**
     * Navigate to the next page of icons.
     *
     * @param picker The icon picker to operate on
     * @return Operation result
     */
    suspend fun nextPage(picker: IconPickerInfo): IconPickerOperationResult

    /**
     * Navigate to the previous page of icons.
     *
     * @param picker The icon picker to operate on
     * @return Operation result
     */
    suspend fun previousPage(picker: IconPickerInfo): IconPickerOperationResult

    // ===============================================================================
    // Filter Operations
    // ===============================================================================

    /**
     * Filter icons by category.
     *
     * @param picker The icon picker to operate on
     * @param category The category name to filter by
     * @return Operation result with count of icons in category
     */
    suspend fun filterByCategory(picker: IconPickerInfo, category: String): IconPickerOperationResult

    /**
     * Clear the current selection.
     *
     * @param picker The icon picker to operate on
     * @return Operation result
     */
    suspend fun clearSelection(picker: IconPickerInfo): IconPickerOperationResult

    /**
     * Show all icons (clear search and category filters).
     *
     * @param picker The icon picker to operate on
     * @return Operation result with total icon count
     */
    suspend fun showAll(picker: IconPickerInfo): IconPickerOperationResult
}
