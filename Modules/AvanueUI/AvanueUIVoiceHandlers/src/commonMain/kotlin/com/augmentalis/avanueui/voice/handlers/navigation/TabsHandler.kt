/**
 * TabsHandler.kt - Voice handler for Tab navigation interactions
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Created: 2026-01-28
 *
 * Purpose: Voice-driven tab navigation and management
 * Features:
 * - Switch to tab by name (e.g., "tab settings", "go to home tab")
 * - Switch to tab by index (e.g., "tab 1", "switch to tab 3")
 * - Positional tab selection (e.g., "first tab", "last tab")
 * - Sequential navigation (e.g., "next tab", "previous tab")
 * - Tab management (e.g., "close tab", "new tab")
 * - AVID-based targeting for precise tab container selection
 * - Voice feedback for tab changes
 *
 * Location: CommandManager module handlers
 *
 * ## Supported Commands
 *
 * Switch by name:
 * - "tab [name]" - Switch to tab with matching name
 * - "go to [name] tab" - Switch to named tab
 * - "[name] tab" - Shorthand for tab switch
 *
 * Switch by index:
 * - "tab [N]" - Switch to Nth tab (1-indexed)
 * - "switch to tab [N]" - Switch to Nth tab
 *
 * Positional selection:
 * - "first tab" - Switch to first tab
 * - "second tab" - Switch to second tab
 * - "third tab" - Switch to third tab
 * - "last tab" - Switch to last tab
 *
 * Sequential navigation:
 * - "next tab" / "right" - Switch to next tab
 * - "previous tab" / "left" / "prev tab" - Switch to previous tab
 *
 * Tab management:
 * - "close tab" / "close" - Close current tab (if closable)
 * - "close [name] tab" - Close specific tab by name
 * - "new tab" / "add tab" - Create new tab (if supported)
 */

package com.augmentalis.avanueui.voice.handlers.navigation

import com.avanues.logging.LoggerFactory
import com.augmentalis.voiceoscore.ActionCategory
import com.augmentalis.voiceoscore.BaseHandler
import com.augmentalis.voiceoscore.Bounds
import com.augmentalis.voiceoscore.ElementInfo
import com.augmentalis.voiceoscore.HandlerResult
import com.augmentalis.voiceoscore.QuantizedCommand

/**
 * Voice command handler for Tab navigation interactions.
 *
 * Provides comprehensive voice control for tab components including:
 * - Name-based tab switching with fuzzy matching
 * - Index-based tab selection (1-indexed for natural speech)
 * - Positional selection (first, second, third, last)
 * - Sequential navigation (next, previous)
 * - Tab management (close, new tab)
 *
 * Thread Safety:
 * - All operations are suspend functions
 * - State modifications are atomic via executor
 *
 * @param executor Platform-specific executor for tab operations
 */
class TabsHandler(
    private val executor: TabsExecutor
) : BaseHandler() {

    companion object {
        private const val TAG = "TabsHandler"
        private val Log = LoggerFactory.getLogger(TAG)

        // Patterns for parsing commands
        private val TAB_NAME_PATTERN = Regex(
            """^(?:tab|go\s+to)\s+(.+?)(?:\s+tab)?$""",
            RegexOption.IGNORE_CASE
        )

        private val GO_TO_TAB_PATTERN = Regex(
            """^go\s+to\s+(.+?)\s+tab$""",
            RegexOption.IGNORE_CASE
        )

        private val NAME_TAB_PATTERN = Regex(
            """^(.+?)\s+tab$""",
            RegexOption.IGNORE_CASE
        )

        private val TAB_INDEX_PATTERN = Regex(
            """^(?:tab|switch\s+to\s+tab)\s+(\d+)$""",
            RegexOption.IGNORE_CASE
        )

        private val SWITCH_TO_TAB_PATTERN = Regex(
            """^switch\s+to\s+tab\s+(\d+)$""",
            RegexOption.IGNORE_CASE
        )

        private val ORDINAL_TAB_PATTERN = Regex(
            """^(first|second|third|fourth|fifth|sixth|seventh|eighth|ninth|tenth|last)\s+tab$""",
            RegexOption.IGNORE_CASE
        )

        private val CLOSE_TAB_NAME_PATTERN = Regex(
            """^close\s+(.+?)\s+tab$""",
            RegexOption.IGNORE_CASE
        )

        // Ordinal word to index mapping (0-indexed internally)
        private val ORDINAL_TO_INDEX = mapOf(
            "first" to 0,
            "second" to 1,
            "third" to 2,
            "fourth" to 3,
            "fifth" to 4,
            "sixth" to 5,
            "seventh" to 6,
            "eighth" to 7,
            "ninth" to 8,
            "tenth" to 9
        )

        // Word number to index mapping for "tab one", "tab two", etc.
        private val WORD_NUMBERS = mapOf(
            "one" to 1, "two" to 2, "three" to 3, "four" to 4, "five" to 5,
            "six" to 6, "seven" to 7, "eight" to 8, "nine" to 9, "ten" to 10,
            "eleven" to 11, "twelve" to 12
        )
    }

    override val category: ActionCategory = ActionCategory.NAVIGATION

    override val supportedActions: List<String> = listOf(
        // Switch by name
        "tab", "go to tab", "go to [name] tab",
        // Switch by index
        "tab [N]", "switch to tab",
        // Positional
        "first tab", "second tab", "third tab", "fourth tab", "fifth tab",
        "last tab",
        // Sequential
        "next tab", "previous tab", "prev tab", "right", "left",
        // Management
        "close tab", "close", "close [name] tab",
        "new tab", "add tab"
    )

    /**
     * Callback for voice feedback when tab changes.
     */
    var onTabChanged: ((tabName: String, tabIndex: Int, totalTabs: Int) -> Unit)? = null

    override suspend fun execute(
        command: QuantizedCommand,
        params: Map<String, Any>
    ): HandlerResult {
        val normalizedAction = command.phrase.lowercase().trim()

        Log.d { "Processing tab command: $normalizedAction" }

        return try {
            when {
                // Sequential navigation: next tab
                normalizedAction in listOf("next tab", "right") -> {
                    handleNextTab(command)
                }

                // Sequential navigation: previous tab
                normalizedAction in listOf("previous tab", "prev tab", "left") -> {
                    handlePreviousTab(command)
                }

                // Tab management: close current tab
                normalizedAction in listOf("close tab", "close") -> {
                    handleCloseTab(command)
                }

                // Tab management: new tab
                normalizedAction in listOf("new tab", "add tab") -> {
                    handleNewTab(command)
                }

                // Close specific tab by name: "close [name] tab"
                CLOSE_TAB_NAME_PATTERN.containsMatchIn(normalizedAction) -> {
                    handleCloseTabByName(normalizedAction, command)
                }

                // Ordinal tab selection: "first tab", "second tab", "last tab"
                ORDINAL_TAB_PATTERN.containsMatchIn(normalizedAction) -> {
                    handleOrdinalTab(normalizedAction, command)
                }

                // Switch by index: "tab 1", "switch to tab 3"
                TAB_INDEX_PATTERN.containsMatchIn(normalizedAction) -> {
                    handleTabByIndex(normalizedAction, command)
                }

                // Switch by index (alternative): "switch to tab 3"
                SWITCH_TO_TAB_PATTERN.containsMatchIn(normalizedAction) -> {
                    handleTabByIndex(normalizedAction, command)
                }

                // Go to named tab: "go to settings tab"
                GO_TO_TAB_PATTERN.containsMatchIn(normalizedAction) -> {
                    handleGoToTab(normalizedAction, command)
                }

                // Named tab pattern: "[name] tab" (must check after ordinals)
                NAME_TAB_PATTERN.containsMatchIn(normalizedAction) -> {
                    handleNamedTab(normalizedAction, command)
                }

                // Simple tab command: "tab [name]" or "tab [number word]"
                TAB_NAME_PATTERN.containsMatchIn(normalizedAction) -> {
                    handleTabCommand(normalizedAction, command)
                }

                else -> HandlerResult.notHandled()
            }
        } catch (e: Exception) {
            Log.e({ "Error executing tab command" }, e)
            HandlerResult.failure("Error: ${e.message}", recoverable = true)
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Command Handlers
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Handle "next tab" or "right" command.
     */
    private suspend fun handleNextTab(command: QuantizedCommand): HandlerResult {
        val tabsInfo = findTabsContainer(avid = command.targetAvid)
            ?: return HandlerResult.Failure(
                reason = "No tab container found",
                recoverable = true,
                suggestedAction = "Focus on a tab bar or tab container"
            )

        val result = executor.nextTab(tabsInfo)

        return if (result.success) {
            val feedback = result.newTabName?.let { "Switched to $it" } ?: "Switched to next tab"

            onTabChanged?.invoke(
                result.newTabName ?: "",
                result.newTabIndex,
                tabsInfo.tabs.size
            )

            Log.i { "Switched to next tab: ${result.newTabName} (${result.newTabIndex + 1}/${tabsInfo.tabs.size})" }

            HandlerResult.Success(
                message = feedback,
                data = mapOf(
                    "tabsAvid" to tabsInfo.avid,
                    "previousTab" to (result.previousTabName ?: ""),
                    "previousIndex" to result.previousTabIndex,
                    "newTab" to (result.newTabName ?: ""),
                    "newIndex" to result.newTabIndex,
                    "totalTabs" to tabsInfo.tabs.size,
                    "accessibility_announcement" to feedback
                )
            )
        } else {
            HandlerResult.failure(
                reason = result.error ?: "Could not switch to next tab",
                recoverable = true
            )
        }
    }

    /**
     * Handle "previous tab" or "left" command.
     */
    private suspend fun handlePreviousTab(command: QuantizedCommand): HandlerResult {
        val tabsInfo = findTabsContainer(avid = command.targetAvid)
            ?: return HandlerResult.Failure(
                reason = "No tab container found",
                recoverable = true,
                suggestedAction = "Focus on a tab bar or tab container"
            )

        val result = executor.previousTab(tabsInfo)

        return if (result.success) {
            val feedback = result.newTabName?.let { "Switched to $it" } ?: "Switched to previous tab"

            onTabChanged?.invoke(
                result.newTabName ?: "",
                result.newTabIndex,
                tabsInfo.tabs.size
            )

            Log.i { "Switched to previous tab: ${result.newTabName} (${result.newTabIndex + 1}/${tabsInfo.tabs.size})" }

            HandlerResult.Success(
                message = feedback,
                data = mapOf(
                    "tabsAvid" to tabsInfo.avid,
                    "previousTab" to (result.previousTabName ?: ""),
                    "previousIndex" to result.previousTabIndex,
                    "newTab" to (result.newTabName ?: ""),
                    "newIndex" to result.newTabIndex,
                    "totalTabs" to tabsInfo.tabs.size,
                    "accessibility_announcement" to feedback
                )
            )
        } else {
            HandlerResult.failure(
                reason = result.error ?: "Could not switch to previous tab",
                recoverable = true
            )
        }
    }

    /**
     * Handle "close tab" or "close" command.
     */
    private suspend fun handleCloseTab(command: QuantizedCommand): HandlerResult {
        val tabsInfo = findTabsContainer(avid = command.targetAvid)
            ?: return HandlerResult.Failure(
                reason = "No tab container found",
                recoverable = true,
                suggestedAction = "Focus on a tab bar or tab container"
            )

        val currentTab = tabsInfo.tabs.getOrNull(tabsInfo.currentIndex)
            ?: return HandlerResult.failure(
                reason = "No current tab selected",
                recoverable = true
            )

        if (!currentTab.isClosable) {
            return HandlerResult.Failure(
                reason = "This tab cannot be closed",
                recoverable = true,
                suggestedAction = "Try closing a different tab"
            )
        }

        val result = executor.closeTab(tabsInfo)

        return if (result.success) {
            val feedback = "Closed ${currentTab.name}"

            Log.i { "Closed tab: ${currentTab.name}" }

            HandlerResult.Success(
                message = feedback,
                data = mapOf(
                    "tabsAvid" to tabsInfo.avid,
                    "closedTab" to currentTab.name,
                    "closedIndex" to tabsInfo.currentIndex,
                    "accessibility_announcement" to feedback
                )
            )
        } else {
            HandlerResult.failure(
                reason = result.error ?: "Could not close tab",
                recoverable = true
            )
        }
    }

    /**
     * Handle "close [name] tab" command.
     */
    private suspend fun handleCloseTabByName(
        normalizedAction: String,
        command: QuantizedCommand
    ): HandlerResult {
        val matchResult = CLOSE_TAB_NAME_PATTERN.find(normalizedAction)
            ?: return HandlerResult.failure("Could not parse close tab command")

        val tabName = matchResult.groupValues[1].trim()

        val tabsInfo = findTabsContainer(avid = command.targetAvid)
            ?: return HandlerResult.Failure(
                reason = "No tab container found",
                recoverable = true,
                suggestedAction = "Focus on a tab bar or tab container"
            )

        val targetTab = tabsInfo.tabs.find {
            it.name.equals(tabName, ignoreCase = true)
        } ?: return HandlerResult.Failure(
            reason = "Tab '$tabName' not found",
            recoverable = true,
            suggestedAction = "Available tabs: ${tabsInfo.tabs.joinToString(", ") { it.name }}"
        )

        if (!targetTab.isClosable) {
            return HandlerResult.Failure(
                reason = "Tab '${targetTab.name}' cannot be closed",
                recoverable = true,
                suggestedAction = "Try closing a different tab"
            )
        }

        val result = executor.closeTabByName(tabsInfo, tabName)

        return if (result.success) {
            val feedback = "Closed ${targetTab.name}"

            Log.i { "Closed tab: ${targetTab.name}" }

            HandlerResult.Success(
                message = feedback,
                data = mapOf(
                    "tabsAvid" to tabsInfo.avid,
                    "closedTab" to targetTab.name,
                    "closedIndex" to targetTab.index,
                    "accessibility_announcement" to feedback
                )
            )
        } else {
            HandlerResult.failure(
                reason = result.error ?: "Could not close tab '$tabName'",
                recoverable = true
            )
        }
    }

    /**
     * Handle "new tab" or "add tab" command.
     */
    private suspend fun handleNewTab(command: QuantizedCommand): HandlerResult {
        val tabsInfo = findTabsContainer(avid = command.targetAvid)
            ?: return HandlerResult.Failure(
                reason = "No tab container found",
                recoverable = true,
                suggestedAction = "Focus on a tab bar or tab container"
            )

        val result = executor.newTab(tabsInfo)

        return if (result.success) {
            val feedback = result.newTabName?.let { "Created $it" } ?: "Created new tab"

            onTabChanged?.invoke(
                result.newTabName ?: "",
                result.newTabIndex,
                tabsInfo.tabs.size + 1
            )

            Log.i { "Created new tab: ${result.newTabName}" }

            HandlerResult.Success(
                message = feedback,
                data = mapOf(
                    "tabsAvid" to tabsInfo.avid,
                    "newTab" to (result.newTabName ?: ""),
                    "newIndex" to result.newTabIndex,
                    "accessibility_announcement" to feedback
                )
            )
        } else {
            HandlerResult.failure(
                reason = result.error ?: "Could not create new tab",
                recoverable = true
            )
        }
    }

    /**
     * Handle ordinal tab selection: "first tab", "second tab", "last tab".
     */
    private suspend fun handleOrdinalTab(
        normalizedAction: String,
        command: QuantizedCommand
    ): HandlerResult {
        val matchResult = ORDINAL_TAB_PATTERN.find(normalizedAction)
            ?: return HandlerResult.failure("Could not parse ordinal tab command")

        val ordinal = matchResult.groupValues[1].lowercase()

        val tabsInfo = findTabsContainer(avid = command.targetAvid)
            ?: return HandlerResult.Failure(
                reason = "No tab container found",
                recoverable = true,
                suggestedAction = "Focus on a tab bar or tab container"
            )

        val targetIndex = if (ordinal == "last") {
            tabsInfo.tabs.size - 1
        } else {
            ORDINAL_TO_INDEX[ordinal] ?: return HandlerResult.failure(
                reason = "Unknown ordinal: $ordinal",
                recoverable = true
            )
        }

        if (targetIndex < 0 || targetIndex >= tabsInfo.tabs.size) {
            return HandlerResult.Failure(
                reason = "Tab index out of range",
                recoverable = true,
                suggestedAction = "There are only ${tabsInfo.tabs.size} tabs"
            )
        }

        return switchToTabByIndex(tabsInfo, targetIndex)
    }

    /**
     * Handle tab by index: "tab 1", "switch to tab 3".
     */
    private suspend fun handleTabByIndex(
        normalizedAction: String,
        command: QuantizedCommand
    ): HandlerResult {
        // Try both patterns
        val matchResult = TAB_INDEX_PATTERN.find(normalizedAction)
            ?: SWITCH_TO_TAB_PATTERN.find(normalizedAction)
            ?: return HandlerResult.failure("Could not parse tab index command")

        val tabNumber = matchResult.groupValues[1].toIntOrNull()
            ?: return HandlerResult.failure("Invalid tab number")

        val tabsInfo = findTabsContainer(avid = command.targetAvid)
            ?: return HandlerResult.Failure(
                reason = "No tab container found",
                recoverable = true,
                suggestedAction = "Focus on a tab bar or tab container"
            )

        // Convert 1-indexed user input to 0-indexed
        val targetIndex = tabNumber - 1

        if (targetIndex < 0 || targetIndex >= tabsInfo.tabs.size) {
            return HandlerResult.Failure(
                reason = "Tab $tabNumber not found",
                recoverable = true,
                suggestedAction = "There are ${tabsInfo.tabs.size} tabs available"
            )
        }

        return switchToTabByIndex(tabsInfo, targetIndex)
    }

    /**
     * Handle "go to [name] tab" command.
     */
    private suspend fun handleGoToTab(
        normalizedAction: String,
        command: QuantizedCommand
    ): HandlerResult {
        val matchResult = GO_TO_TAB_PATTERN.find(normalizedAction)
            ?: return HandlerResult.failure("Could not parse go to tab command")

        val tabName = matchResult.groupValues[1].trim()

        return switchToTabByName(tabName, command)
    }

    /**
     * Handle "[name] tab" command.
     */
    private suspend fun handleNamedTab(
        normalizedAction: String,
        command: QuantizedCommand
    ): HandlerResult {
        val matchResult = NAME_TAB_PATTERN.find(normalizedAction)
            ?: return HandlerResult.failure("Could not parse named tab command")

        val tabName = matchResult.groupValues[1].trim()

        // Check if it's an ordinal we missed
        if (ORDINAL_TO_INDEX.containsKey(tabName) || tabName == "last") {
            return handleOrdinalTab(normalizedAction, command)
        }

        return switchToTabByName(tabName, command)
    }

    /**
     * Handle simple "tab [name]" or "tab [number word]" command.
     */
    private suspend fun handleTabCommand(
        normalizedAction: String,
        command: QuantizedCommand
    ): HandlerResult {
        val matchResult = TAB_NAME_PATTERN.find(normalizedAction)
            ?: return HandlerResult.failure("Could not parse tab command")

        val argument = matchResult.groupValues[1].trim()

        // Check if it's a number (e.g., "tab 1")
        argument.toIntOrNull()?.let { tabNumber ->
            val tabsInfo = findTabsContainer(avid = command.targetAvid)
                ?: return HandlerResult.Failure(
                    reason = "No tab container found",
                    recoverable = true,
                    suggestedAction = "Focus on a tab bar or tab container"
                )

            val targetIndex = tabNumber - 1
            if (targetIndex >= 0 && targetIndex < tabsInfo.tabs.size) {
                return switchToTabByIndex(tabsInfo, targetIndex)
            }
        }

        // Check if it's a word number (e.g., "tab one", "tab two")
        WORD_NUMBERS[argument]?.let { tabNumber ->
            val tabsInfo = findTabsContainer(avid = command.targetAvid)
                ?: return HandlerResult.Failure(
                    reason = "No tab container found",
                    recoverable = true,
                    suggestedAction = "Focus on a tab bar or tab container"
                )

            val targetIndex = tabNumber - 1
            if (targetIndex >= 0 && targetIndex < tabsInfo.tabs.size) {
                return switchToTabByIndex(tabsInfo, targetIndex)
            }
        }

        // Otherwise treat as tab name
        return switchToTabByName(argument, command)
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Helper Methods
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Find tab container by AVID or focus state.
     */
    private suspend fun findTabsContainer(
        name: String? = null,
        avid: String? = null
    ): TabsInfo? {
        // Priority 1: AVID lookup (fastest, most precise)
        if (avid != null) {
            val tabs = executor.findByAvid(avid)
            if (tabs != null) return tabs
        }

        // Priority 2: Name lookup
        if (name != null) {
            val tabs = executor.findByName(name)
            if (tabs != null) return tabs
        }

        // Priority 3: Focused tab container
        return executor.findFocused()
    }

    /**
     * Switch to a tab by its 0-indexed position.
     */
    private suspend fun switchToTabByIndex(
        tabsInfo: TabsInfo,
        targetIndex: Int
    ): HandlerResult {
        val targetTab = tabsInfo.tabs.getOrNull(targetIndex)
            ?: return HandlerResult.Failure(
                reason = "Tab at index ${targetIndex + 1} not found",
                recoverable = true,
                suggestedAction = "There are ${tabsInfo.tabs.size} tabs available"
            )

        val result = executor.switchToTabByIndex(tabsInfo, targetIndex)

        return if (result.success) {
            val feedback = "Switched to ${targetTab.name}"

            onTabChanged?.invoke(
                targetTab.name,
                targetIndex,
                tabsInfo.tabs.size
            )

            Log.i { "Switched to tab: ${targetTab.name} (${targetIndex + 1}/${tabsInfo.tabs.size})" }

            HandlerResult.Success(
                message = feedback,
                data = mapOf(
                    "tabsAvid" to tabsInfo.avid,
                    "previousTab" to (result.previousTabName ?: ""),
                    "previousIndex" to result.previousTabIndex,
                    "newTab" to targetTab.name,
                    "newIndex" to targetIndex,
                    "totalTabs" to tabsInfo.tabs.size,
                    "accessibility_announcement" to feedback
                )
            )
        } else {
            HandlerResult.failure(
                reason = result.error ?: "Could not switch to tab ${targetIndex + 1}",
                recoverable = true
            )
        }
    }

    /**
     * Switch to a tab by its name.
     */
    private suspend fun switchToTabByName(
        tabName: String,
        command: QuantizedCommand
    ): HandlerResult {
        val tabsInfo = findTabsContainer(avid = command.targetAvid)
            ?: return HandlerResult.Failure(
                reason = "No tab container found",
                recoverable = true,
                suggestedAction = "Focus on a tab bar or tab container"
            )

        // Find matching tab (case-insensitive, partial match)
        val targetTab = tabsInfo.tabs.find {
            it.name.equals(tabName, ignoreCase = true)
        } ?: tabsInfo.tabs.find {
            it.name.contains(tabName, ignoreCase = true)
        } ?: return HandlerResult.Failure(
            reason = "Tab '$tabName' not found",
            recoverable = true,
            suggestedAction = "Available tabs: ${tabsInfo.tabs.joinToString(", ") { it.name }}"
        )

        val result = executor.switchToTab(tabsInfo, targetTab)

        return if (result.success) {
            val feedback = "Switched to ${targetTab.name}"

            onTabChanged?.invoke(
                targetTab.name,
                targetTab.index,
                tabsInfo.tabs.size
            )

            Log.i { "Switched to tab: ${targetTab.name} (${targetTab.index + 1}/${tabsInfo.tabs.size})" }

            HandlerResult.Success(
                message = feedback,
                data = mapOf(
                    "tabsAvid" to tabsInfo.avid,
                    "previousTab" to (result.previousTabName ?: ""),
                    "previousIndex" to result.previousTabIndex,
                    "newTab" to targetTab.name,
                    "newIndex" to targetTab.index,
                    "totalTabs" to tabsInfo.tabs.size,
                    "accessibility_announcement" to feedback
                )
            )
        } else {
            HandlerResult.failure(
                reason = result.error ?: "Could not switch to tab '$tabName'",
                recoverable = true
            )
        }
    }

}

// ═══════════════════════════════════════════════════════════════════════════════
// Supporting Types
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * Information about a tab container component.
 *
 * @property avid AVID fingerprint for the tab container (format: TAB:{hash8})
 * @property name Display name or associated label for the tab container
 * @property tabs List of tabs in this container
 * @property currentIndex Index of the currently selected tab (0-indexed)
 * @property isScrollable Whether the tab bar supports scrolling
 * @property bounds Screen bounds for the tab container
 * @property isFocused Whether this tab container currently has focus
 * @property node Platform-specific node reference
 */
data class TabsInfo(
    val avid: String,
    val name: String = "",
    val tabs: List<TabInfo> = emptyList(),
    val currentIndex: Int = 0,
    val isScrollable: Boolean = false,
    val bounds: Bounds = Bounds.EMPTY,
    val isFocused: Boolean = false,
    val node: Any? = null
) {
    /**
     * Get the currently selected tab.
     */
    val currentTab: TabInfo?
        get() = tabs.getOrNull(currentIndex)

    /**
     * Convert to ElementInfo for general element operations.
     */
    fun toElementInfo(): ElementInfo = ElementInfo(
        className = "TabLayout",
        text = name,
        bounds = bounds,
        isClickable = true,
        isEnabled = true,
        avid = avid,
        stateDescription = "Tab ${currentIndex + 1} of ${tabs.size}"
    )
}

/**
 * Information about an individual tab within a tab container.
 *
 * @property id Unique identifier for this tab
 * @property name Display name/label of the tab
 * @property index Position in the tab list (0-indexed)
 * @property isSelected Whether this tab is currently selected
 * @property isClosable Whether this tab can be closed by the user
 * @property icon Icon resource name or path (optional)
 */
data class TabInfo(
    val id: String,
    val name: String,
    val index: Int,
    val isSelected: Boolean = false,
    val isClosable: Boolean = false,
    val icon: String? = null
)

/**
 * Result of a tab operation.
 *
 * @property success Whether the operation completed successfully
 * @property error Error message if operation failed
 * @property previousTabName Name of the tab before the operation
 * @property previousTabIndex Index of the tab before the operation
 * @property newTabName Name of the tab after the operation
 * @property newTabIndex Index of the tab after the operation
 */
data class TabsOperationResult(
    val success: Boolean,
    val error: String? = null,
    val previousTabName: String? = null,
    val previousTabIndex: Int = -1,
    val newTabName: String? = null,
    val newTabIndex: Int = -1
) {
    companion object {
        /**
         * Create a successful tab switch result.
         */
        fun switchSuccess(
            previousName: String?,
            previousIndex: Int,
            newName: String?,
            newIndex: Int
        ) = TabsOperationResult(
            success = true,
            previousTabName = previousName,
            previousTabIndex = previousIndex,
            newTabName = newName,
            newTabIndex = newIndex
        )

        /**
         * Create a successful close result.
         */
        fun closeSuccess(closedName: String, closedIndex: Int) = TabsOperationResult(
            success = true,
            previousTabName = closedName,
            previousTabIndex = closedIndex
        )

        /**
         * Create a successful new tab result.
         */
        fun newTabSuccess(newName: String?, newIndex: Int) = TabsOperationResult(
            success = true,
            newTabName = newName,
            newTabIndex = newIndex
        )

        /**
         * Create an error result.
         */
        fun error(message: String) = TabsOperationResult(
            success = false,
            error = message
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// Platform Executor Interface
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * Platform-specific executor for tab operations.
 *
 * Implementations should:
 * 1. Find tab containers by AVID, name, or focus state
 * 2. Read tab list and current selection state
 * 3. Switch between tabs via accessibility actions
 * 4. Handle TabLayout, ViewPager, and custom tab implementations
 *
 * ## Tab Container Detection Algorithm
 *
 * ```kotlin
 * fun findTabContainer(rootNode: AccessibilityNodeInfo): AccessibilityNodeInfo? {
 *     // Look for nodes with className:
 *     // - com.google.android.material.tabs.TabLayout
 *     // - android.widget.TabWidget
 *     // - androidx.viewpager.widget.ViewPager
 *     // - androidx.viewpager2.widget.ViewPager2
 *     // - Custom implementations with tab-like children
 * }
 * ```
 *
 * ## Tab Selection Algorithm
 *
 * ```kotlin
 * fun selectTab(tabNode: AccessibilityNodeInfo): Boolean {
 *     // Try ACTION_CLICK on tab item
 *     // Or ACTION_SELECT if available
 *     // Or simulate touch gesture on tab bounds
 * }
 * ```
 */
interface TabsExecutor {

    // ═══════════════════════════════════════════════════════════════════════════
    // Tab Container Discovery
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Find a tab container by its AVID fingerprint.
     *
     * @param avid The AVID fingerprint (format: TAB:{hash8})
     * @return TabsInfo if found, null otherwise
     */
    suspend fun findByAvid(avid: String): TabsInfo?

    /**
     * Find a tab container by its name or associated label.
     *
     * Searches for:
     * 1. Tab container with matching contentDescription
     * 2. Tab container with associated label matching name
     * 3. Tab container containing tab with matching name
     *
     * @param name The name to search for (case-insensitive)
     * @return TabsInfo if found, null otherwise
     */
    suspend fun findByName(name: String): TabsInfo?

    /**
     * Find the currently focused tab container.
     *
     * @return TabsInfo if a tab container has focus, null otherwise
     */
    suspend fun findFocused(): TabsInfo?

    // ═══════════════════════════════════════════════════════════════════════════
    // Tab Navigation
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Switch to a specific tab.
     *
     * @param tabsInfo The tab container
     * @param tab The tab to switch to
     * @return Operation result with previous and new tab info
     */
    suspend fun switchToTab(tabsInfo: TabsInfo, tab: TabInfo): TabsOperationResult

    /**
     * Switch to a tab at a specific index.
     *
     * @param tabsInfo The tab container
     * @param index The 0-indexed tab position
     * @return Operation result with previous and new tab info
     */
    suspend fun switchToTabByIndex(tabsInfo: TabsInfo, index: Int): TabsOperationResult

    /**
     * Switch to the next tab.
     *
     * If currently on the last tab, behavior depends on implementation:
     * - May wrap to first tab
     * - May stay on last tab and return error
     *
     * @param tabsInfo The tab container
     * @return Operation result
     */
    suspend fun nextTab(tabsInfo: TabsInfo): TabsOperationResult

    /**
     * Switch to the previous tab.
     *
     * If currently on the first tab, behavior depends on implementation:
     * - May wrap to last tab
     * - May stay on first tab and return error
     *
     * @param tabsInfo The tab container
     * @return Operation result
     */
    suspend fun previousTab(tabsInfo: TabsInfo): TabsOperationResult

    // ═══════════════════════════════════════════════════════════════════════════
    // Tab Management
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Close the current tab.
     *
     * Only works if the tab is closable.
     *
     * @param tabsInfo The tab container
     * @return Operation result
     */
    suspend fun closeTab(tabsInfo: TabsInfo): TabsOperationResult

    /**
     * Close a tab by name.
     *
     * Only works if the tab is closable.
     *
     * @param tabsInfo The tab container
     * @param tabName The name of the tab to close (case-insensitive)
     * @return Operation result
     */
    suspend fun closeTabByName(tabsInfo: TabsInfo, tabName: String): TabsOperationResult

    /**
     * Create a new tab.
     *
     * Only works if the tab container supports dynamic tab creation.
     *
     * @param tabsInfo The tab container
     * @return Operation result with the new tab info
     */
    suspend fun newTab(tabsInfo: TabsInfo): TabsOperationResult

    // ═══════════════════════════════════════════════════════════════════════════
    // Tab Information
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Get all tabs in a container.
     *
     * @param tabsInfo The tab container
     * @return List of all tabs
     */
    suspend fun getTabs(tabsInfo: TabsInfo): List<TabInfo>

    /**
     * Get the currently selected tab.
     *
     * @param tabsInfo The tab container
     * @return Currently selected tab, or null if none
     */
    suspend fun getCurrentTab(tabsInfo: TabsInfo): TabInfo?
}
