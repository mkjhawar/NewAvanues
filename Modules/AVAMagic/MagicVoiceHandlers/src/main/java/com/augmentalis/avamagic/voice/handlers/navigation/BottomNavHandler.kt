/**
 * BottomNavHandler.kt - Voice handler for Bottom Navigation interactions
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2026-01-28
 *
 * Purpose: Voice-driven bottom navigation bar control
 * Features:
 * - Navigate to tabs by name (fuzzy matching)
 * - Navigate to tabs by index (1-indexed)
 * - Positional navigation (first, second, third, last)
 * - Sequential navigation (next, previous)
 * - Navigate to home/main tab
 * - Announce current tab for orientation
 * - AVID-based targeting for precise element selection
 * - Voice feedback for tab changes
 *
 * Location: CommandManager module handlers
 *
 * ## Supported Commands
 *
 * By name:
 * - "go to [tab name]" - Navigate to tab by label (e.g., "go to settings")
 * - "[tab name]" - Direct tab name (e.g., "home", "profile")
 *
 * By index:
 * - "tab [N]" - Navigate to Nth tab (1-indexed, e.g., "tab 3")
 * - "tab one/two/three" - Word numbers supported
 *
 * By position:
 * - "first tab" - Go to first tab
 * - "second tab" - Go to second tab
 * - "third tab" - Go to third tab
 * - "last tab" - Go to last tab
 *
 * Sequential:
 * - "next tab" - Go to next tab (wraps to first)
 * - "previous tab" - Go to previous tab (wraps to last)
 *
 * Home:
 * - "home" / "main" - Go to first/home tab
 *
 * Orientation:
 * - "where am I" / "current tab" - Announce current tab
 *
 * ## Tab Matching Algorithm
 *
 * Name matching uses:
 * 1. Exact match (case-insensitive)
 * 2. Contains match (tab label contains spoken name)
 * 3. Fuzzy match (Levenshtein distance for typos)
 */

package com.augmentalis.avamagic.voice.handlers.navigation

import android.util.Log
import com.augmentalis.voiceoscore.ActionCategory
import com.augmentalis.voiceoscore.BaseHandler
import com.augmentalis.voiceoscore.Bounds
import com.augmentalis.voiceoscore.ElementInfo
import com.augmentalis.voiceoscore.HandlerResult
import com.augmentalis.voiceoscore.QuantizedCommand

/**
 * Voice command handler for Bottom Navigation interactions.
 *
 * Provides comprehensive voice control for bottom navigation including:
 * - Tab navigation by name, index, or position
 * - Sequential navigation (next/previous)
 * - Current tab announcement for accessibility
 *
 * Thread Safety:
 * - All operations are suspend functions
 * - State modifications are atomic via executor
 *
 * @param executor Platform-specific executor for bottom navigation operations
 */
class BottomNavHandler(
    private val executor: BottomNavExecutor
) : BaseHandler() {

    companion object {
        private const val TAG = "BottomNavHandler"

        // Pattern for "go to [tab name]"
        private val GO_TO_PATTERN = Regex(
            """^go\s+to\s+(.+)$""",
            RegexOption.IGNORE_CASE
        )

        // Pattern for "tab [N]" or "tab [word]"
        private val TAB_NUMBER_PATTERN = Regex(
            """^tab\s+(\d+|one|two|three|four|five|six|seven|eight|nine|ten)$""",
            RegexOption.IGNORE_CASE
        )

        // Word to number mapping
        private val WORD_NUMBERS = mapOf(
            "one" to 1, "two" to 2, "three" to 3, "four" to 4, "five" to 5,
            "six" to 6, "seven" to 7, "eight" to 8, "nine" to 9, "ten" to 10,
            "first" to 1, "second" to 2, "third" to 3, "fourth" to 4, "fifth" to 5,
            "sixth" to 6, "seventh" to 7, "eighth" to 8, "ninth" to 9, "tenth" to 10
        )

        // Positional patterns
        private val POSITIONAL_PATTERNS = mapOf(
            "first tab" to 1,
            "second tab" to 2,
            "third tab" to 3,
            "fourth tab" to 4,
            "fifth tab" to 5
        )
    }

    override val category: ActionCategory = ActionCategory.NAVIGATION

    override val supportedActions: List<String> = listOf(
        // By name
        "go to", "[tab name]",
        // By index
        "tab 1", "tab 2", "tab 3", "tab one", "tab two", "tab three",
        // By position
        "first tab", "second tab", "third tab", "last tab",
        // Sequential
        "next tab", "previous tab",
        // Home
        "home", "main",
        // Orientation
        "where am I", "current tab"
    )

    /**
     * Callback for voice feedback when tab changes.
     */
    var onTabChanged: ((tabName: String, tabIndex: Int) -> Unit)? = null

    override suspend fun execute(
        command: QuantizedCommand,
        params: Map<String, Any>
    ): HandlerResult {
        val normalizedAction = command.phrase.lowercase().trim()

        Log.d(TAG, "Processing bottom nav command: $normalizedAction")

        return try {
            when {
                // "go to [tab name]"
                GO_TO_PATTERN.containsMatchIn(normalizedAction) -> {
                    handleGoTo(normalizedAction, command)
                }

                // "tab [N]" or "tab [word]"
                TAB_NUMBER_PATTERN.containsMatchIn(normalizedAction) -> {
                    handleTabByNumber(normalizedAction, command)
                }

                // Positional: "first tab", "second tab", etc.
                POSITIONAL_PATTERNS.containsKey(normalizedAction) -> {
                    handlePositionalTab(normalizedAction, command)
                }

                // "last tab"
                normalizedAction == "last tab" -> {
                    handleLastTab(command)
                }

                // Sequential: "next tab"
                normalizedAction == "next tab" -> {
                    handleNextTab(command)
                }

                // Sequential: "previous tab"
                normalizedAction in listOf("previous tab", "prev tab") -> {
                    handlePreviousTab(command)
                }

                // Home/main
                normalizedAction in listOf("home", "main") -> {
                    handleHome(command)
                }

                // Orientation: "where am I", "current tab"
                normalizedAction in listOf("where am i", "current tab") -> {
                    handleCurrentTab(command)
                }

                // Direct tab name (fallback - try to match any tab)
                else -> handleDirectTabName(normalizedAction, command)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error executing bottom nav command", e)
            HandlerResult.failure("Error: ${e.message}", recoverable = true)
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Command Handlers
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Handle "go to [tab name]" command.
     */
    private suspend fun handleGoTo(
        normalizedAction: String,
        command: QuantizedCommand
    ): HandlerResult {
        val matchResult = GO_TO_PATTERN.find(normalizedAction)
            ?: return HandlerResult.failure("Could not parse go to command")

        val tabName = matchResult.groupValues[1].trim()
        return navigateToTabByName(tabName, command)
    }

    /**
     * Handle "tab [N]" command.
     */
    private suspend fun handleTabByNumber(
        normalizedAction: String,
        command: QuantizedCommand
    ): HandlerResult {
        val matchResult = TAB_NUMBER_PATTERN.find(normalizedAction)
            ?: return HandlerResult.failure("Could not parse tab number")

        val numberStr = matchResult.groupValues[1].lowercase()
        val tabIndex = numberStr.toIntOrNull() ?: WORD_NUMBERS[numberStr]
            ?: return HandlerResult.Failure(
                reason = "Could not understand tab number: '$numberStr'",
                recoverable = true,
                suggestedAction = "Try 'tab 1' or 'tab one'"
            )

        return navigateToTabByIndex(tabIndex, command)
    }

    /**
     * Handle positional tab commands ("first tab", "second tab", etc.).
     */
    private suspend fun handlePositionalTab(
        normalizedAction: String,
        command: QuantizedCommand
    ): HandlerResult {
        val tabIndex = POSITIONAL_PATTERNS[normalizedAction]
            ?: return HandlerResult.notHandled()

        return navigateToTabByIndex(tabIndex, command)
    }

    /**
     * Handle "last tab" command.
     */
    private suspend fun handleLastTab(command: QuantizedCommand): HandlerResult {
        val navInfo = executor.findBottomNavigation()
            ?: return HandlerResult.Failure(
                reason = "No bottom navigation found",
                recoverable = true,
                suggestedAction = "This screen may not have bottom navigation"
            )

        val tabCount = navInfo.tabs.size
        if (tabCount == 0) {
            return HandlerResult.Failure(
                reason = "No tabs found in bottom navigation",
                recoverable = true
            )
        }

        return navigateToTabByIndex(tabCount, command)
    }

    /**
     * Handle "next tab" command.
     */
    private suspend fun handleNextTab(command: QuantizedCommand): HandlerResult {
        val navInfo = executor.findBottomNavigation()
            ?: return HandlerResult.Failure(
                reason = "No bottom navigation found",
                recoverable = true,
                suggestedAction = "This screen may not have bottom navigation"
            )

        val currentIndex = navInfo.selectedIndex
        val tabCount = navInfo.tabs.size

        if (tabCount == 0) {
            return HandlerResult.Failure(
                reason = "No tabs found",
                recoverable = true
            )
        }

        // Wrap to first tab if at the end
        val nextIndex = if (currentIndex >= tabCount - 1) 0 else currentIndex + 1
        val targetTab = navInfo.tabs.getOrNull(nextIndex)
            ?: return HandlerResult.failure("Could not find next tab")

        return navigateToTab(targetTab, navInfo)
    }

    /**
     * Handle "previous tab" command.
     */
    private suspend fun handlePreviousTab(command: QuantizedCommand): HandlerResult {
        val navInfo = executor.findBottomNavigation()
            ?: return HandlerResult.Failure(
                reason = "No bottom navigation found",
                recoverable = true,
                suggestedAction = "This screen may not have bottom navigation"
            )

        val currentIndex = navInfo.selectedIndex
        val tabCount = navInfo.tabs.size

        if (tabCount == 0) {
            return HandlerResult.Failure(
                reason = "No tabs found",
                recoverable = true
            )
        }

        // Wrap to last tab if at the beginning
        val prevIndex = if (currentIndex <= 0) tabCount - 1 else currentIndex - 1
        val targetTab = navInfo.tabs.getOrNull(prevIndex)
            ?: return HandlerResult.failure("Could not find previous tab")

        return navigateToTab(targetTab, navInfo)
    }

    /**
     * Handle "home" / "main" command.
     *
     * Navigates to the first tab (assumed to be home).
     */
    private suspend fun handleHome(command: QuantizedCommand): HandlerResult {
        return navigateToTabByIndex(1, command)
    }

    /**
     * Handle "where am I" / "current tab" command.
     */
    private suspend fun handleCurrentTab(command: QuantizedCommand): HandlerResult {
        val navInfo = executor.findBottomNavigation()
            ?: return HandlerResult.Failure(
                reason = "No bottom navigation found",
                recoverable = true,
                suggestedAction = "This screen may not have bottom navigation"
            )

        val currentTab = navInfo.tabs.getOrNull(navInfo.selectedIndex)
            ?: return HandlerResult.failure("Could not determine current tab")

        val announcement = buildString {
            append("You are on ")
            append(currentTab.label)
            append(", tab ${navInfo.selectedIndex + 1} of ${navInfo.tabs.size}")
        }

        Log.i(TAG, "Current tab: ${currentTab.label} (${navInfo.selectedIndex + 1}/${navInfo.tabs.size})")

        return HandlerResult.Success(
            message = announcement,
            data = mapOf(
                "action" to "current_tab",
                "tabName" to currentTab.label,
                "tabIndex" to navInfo.selectedIndex,
                "tabCount" to navInfo.tabs.size,
                "accessibility_announcement" to announcement
            )
        )
    }

    /**
     * Handle direct tab name (fallback for unmatched commands).
     */
    private suspend fun handleDirectTabName(
        normalizedAction: String,
        command: QuantizedCommand
    ): HandlerResult {
        // Don't try to match very short commands or common non-tab words
        if (normalizedAction.length < 3 ||
            normalizedAction in listOf("ok", "yes", "no", "stop", "cancel")) {
            return HandlerResult.notHandled()
        }

        return navigateToTabByName(normalizedAction, command)
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Navigation Helpers
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Navigate to a tab by its name.
     *
     * Uses fuzzy matching to handle slight variations.
     */
    private suspend fun navigateToTabByName(
        name: String,
        command: QuantizedCommand
    ): HandlerResult {
        val navInfo = executor.findBottomNavigation()
            ?: return HandlerResult.Failure(
                reason = "No bottom navigation found",
                recoverable = true,
                suggestedAction = "This screen may not have bottom navigation"
            )

        // Try to find matching tab
        val matchedTab = findTabByName(name, navInfo.tabs)
            ?: return HandlerResult.Failure(
                reason = "No tab found matching '$name'",
                recoverable = true,
                suggestedAction = "Available tabs: ${navInfo.tabs.joinToString { it.label }}"
            )

        return navigateToTab(matchedTab, navInfo)
    }

    /**
     * Navigate to a tab by its index (1-indexed).
     */
    private suspend fun navigateToTabByIndex(
        index: Int,
        command: QuantizedCommand
    ): HandlerResult {
        val navInfo = executor.findBottomNavigation()
            ?: return HandlerResult.Failure(
                reason = "No bottom navigation found",
                recoverable = true,
                suggestedAction = "This screen may not have bottom navigation"
            )

        val tabCount = navInfo.tabs.size

        // Convert to 0-indexed
        val zeroIndex = index - 1

        if (zeroIndex < 0 || zeroIndex >= tabCount) {
            return HandlerResult.Failure(
                reason = "Tab $index does not exist. There are $tabCount tabs.",
                recoverable = true,
                suggestedAction = "Try 'tab 1' through 'tab $tabCount'"
            )
        }

        val targetTab = navInfo.tabs[zeroIndex]
        return navigateToTab(targetTab, navInfo)
    }

    /**
     * Navigate to a specific tab.
     */
    private suspend fun navigateToTab(
        tab: BottomNavTabInfo,
        navInfo: BottomNavInfo
    ): HandlerResult {
        val result = executor.selectTab(tab)

        return if (result.success) {
            val tabIndex = navInfo.tabs.indexOf(tab)
            onTabChanged?.invoke(tab.label, tabIndex)

            val announcement = "Navigated to ${tab.label}"
            Log.i(TAG, "Tab selected: ${tab.label} (index $tabIndex)")

            HandlerResult.Success(
                message = announcement,
                data = mapOf(
                    "action" to "select_tab",
                    "tabName" to tab.label,
                    "tabAvid" to tab.avid,
                    "tabIndex" to tabIndex,
                    "previousIndex" to navInfo.selectedIndex,
                    "accessibility_announcement" to announcement
                )
            )
        } else {
            HandlerResult.Failure(
                reason = result.error ?: "Could not navigate to ${tab.label}",
                recoverable = true,
                suggestedAction = "Try tapping the tab directly"
            )
        }
    }

    /**
     * Find a tab by name using fuzzy matching.
     */
    private fun findTabByName(name: String, tabs: List<BottomNavTabInfo>): BottomNavTabInfo? {
        val lowerName = name.lowercase()

        // Priority 1: Exact match
        tabs.find { it.label.lowercase() == lowerName }?.let { return it }

        // Priority 2: Contains match
        tabs.find { it.label.lowercase().contains(lowerName) }?.let { return it }

        // Priority 3: Name contained in tab label
        tabs.find { lowerName.contains(it.label.lowercase()) }?.let { return it }

        // Priority 4: Fuzzy match (simple Levenshtein threshold)
        val threshold = maxOf(2, name.length / 3)
        tabs.minByOrNull { levenshteinDistance(it.label.lowercase(), lowerName) }
            ?.takeIf { levenshteinDistance(it.label.lowercase(), lowerName) <= threshold }
            ?.let { return it }

        return null
    }

    /**
     * Calculate Levenshtein distance for fuzzy matching.
     */
    private fun levenshteinDistance(s1: String, s2: String): Int {
        val dp = Array(s1.length + 1) { IntArray(s2.length + 1) }

        for (i in 0..s1.length) dp[i][0] = i
        for (j in 0..s2.length) dp[0][j] = j

        for (i in 1..s1.length) {
            for (j in 1..s2.length) {
                val cost = if (s1[i - 1] == s2[j - 1]) 0 else 1
                dp[i][j] = minOf(
                    dp[i - 1][j] + 1,      // deletion
                    dp[i][j - 1] + 1,      // insertion
                    dp[i - 1][j - 1] + cost // substitution
                )
            }
        }

        return dp[s1.length][s2.length]
    }

}

// ═══════════════════════════════════════════════════════════════════════════════
// Supporting Types
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * Information about a bottom navigation component.
 *
 * @property avid AVID fingerprint for the navigation bar (format: NAV:{hash8})
 * @property tabs List of tabs in the navigation bar
 * @property selectedIndex Index of the currently selected tab (0-indexed)
 * @property bounds Screen bounds for the navigation bar
 * @property node Platform-specific node reference
 */
data class BottomNavInfo(
    val avid: String,
    val tabs: List<BottomNavTabInfo> = emptyList(),
    val selectedIndex: Int = 0,
    val bounds: Bounds = Bounds.EMPTY,
    val node: Any? = null
) {
    /**
     * Get the currently selected tab.
     */
    val selectedTab: BottomNavTabInfo?
        get() = tabs.getOrNull(selectedIndex)

    /**
     * Convert to ElementInfo for general element operations.
     */
    fun toElementInfo(): ElementInfo = ElementInfo(
        className = "BottomNavigation",
        text = selectedTab?.label ?: "",
        bounds = bounds,
        isClickable = false,
        isEnabled = true,
        avid = avid,
        stateDescription = "Tab ${selectedIndex + 1} of ${tabs.size}: ${selectedTab?.label ?: "unknown"}"
    )
}

/**
 * Information about a tab in the bottom navigation.
 *
 * @property avid AVID fingerprint for the tab (format: TAB:{hash8})
 * @property label Tab label/title text
 * @property iconDescription Description of the tab icon
 * @property index Tab index in the navigation bar (0-indexed)
 * @property isSelected Whether this tab is currently selected
 * @property hasBadge Whether the tab has a notification badge
 * @property badgeCount Badge count if present (-1 for dot badge)
 * @property bounds Screen bounds for the tab
 * @property node Platform-specific node reference
 */
data class BottomNavTabInfo(
    val avid: String,
    val label: String,
    val iconDescription: String = "",
    val index: Int = 0,
    val isSelected: Boolean = false,
    val hasBadge: Boolean = false,
    val badgeCount: Int = 0,
    val bounds: Bounds = Bounds.EMPTY,
    val node: Any? = null
) {
    /**
     * Convert to ElementInfo for general element operations.
     */
    fun toElementInfo(): ElementInfo = ElementInfo(
        className = "BottomNavigationTab",
        text = label,
        bounds = bounds,
        isClickable = true,
        isEnabled = true,
        isSelected = isSelected,
        avid = avid,
        stateDescription = buildString {
            if (isSelected) append("Selected, ")
            append("Tab ${index + 1}")
            if (hasBadge) {
                if (badgeCount > 0) append(", $badgeCount new")
                else append(", has notification")
            }
        }
    )
}

/**
 * Result of a bottom navigation operation.
 *
 * @property success Whether the operation succeeded
 * @property error Error message if operation failed
 * @property previousTab Previous tab name before navigation
 * @property newTab New tab name after navigation
 */
data class BottomNavOperationResult(
    val success: Boolean,
    val error: String? = null,
    val previousTab: String? = null,
    val newTab: String? = null
) {
    companion object {
        fun success(previousTab: String? = null, newTab: String? = null) = BottomNavOperationResult(
            success = true,
            previousTab = previousTab,
            newTab = newTab
        )

        fun error(message: String) = BottomNavOperationResult(
            success = false,
            error = message
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// Platform Executor Interface
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * Platform-specific executor for bottom navigation operations.
 *
 * Implementations should:
 * 1. Find bottom navigation components by traversing the view hierarchy
 * 2. Identify individual tabs with labels and selection state
 * 3. Execute tab selection via accessibility click actions
 * 4. Handle various bottom navigation types (BottomNavigationView, NavigationBar)
 *
 * ## Bottom Navigation Detection Algorithm
 *
 * ```kotlin
 * fun findBottomNavNode(rootNode: AccessibilityNodeInfo): AccessibilityNodeInfo? {
 *     // Look for nodes with className:
 *     // - com.google.android.material.bottomnavigation.BottomNavigationView
 *     // - com.google.android.material.navigation.NavigationBarView
 *     // - androidx.compose.material3.NavigationBar
 *     // Or nodes at bottom of screen with multiple clickable children
 * }
 * ```
 *
 * ## Tab Detection
 *
 * ```kotlin
 * fun findTabs(navNode: AccessibilityNodeInfo): List<BottomNavTabInfo> {
 *     // Children with:
 *     // - className containing "Tab" or "MenuItem"
 *     // - isClickable = true
 *     // - Has text or contentDescription
 *     // Check isSelected state for current tab
 * }
 * ```
 */
interface BottomNavExecutor {

    // ═══════════════════════════════════════════════════════════════════════════
    // Navigation Bar Discovery
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Find the bottom navigation bar on the current screen.
     *
     * @return BottomNavInfo if found, null otherwise
     */
    suspend fun findBottomNavigation(): BottomNavInfo?

    /**
     * Find a bottom navigation bar by its AVID fingerprint.
     *
     * @param avid The AVID fingerprint (format: NAV:{hash8})
     * @return BottomNavInfo if found, null otherwise
     */
    suspend fun findBottomNavigationByAvid(avid: String): BottomNavInfo?

    // ═══════════════════════════════════════════════════════════════════════════
    // Tab Operations
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Select a specific tab.
     *
     * @param tab The tab to select
     * @return Operation result
     */
    suspend fun selectTab(tab: BottomNavTabInfo): BottomNavOperationResult

    /**
     * Select a tab by its index (0-indexed).
     *
     * @param index Tab index
     * @return Operation result
     */
    suspend fun selectTabByIndex(index: Int): BottomNavOperationResult

    /**
     * Select a tab by its label (fuzzy match).
     *
     * @param label Tab label to search for
     * @return Operation result
     */
    suspend fun selectTabByLabel(label: String): BottomNavOperationResult

    // ═══════════════════════════════════════════════════════════════════════════
    // State Queries
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Get the currently selected tab.
     *
     * @return Currently selected tab info, or null if unavailable
     */
    suspend fun getSelectedTab(): BottomNavTabInfo?

    /**
     * Get all tabs in the navigation bar.
     *
     * @return List of all tabs
     */
    suspend fun getAllTabs(): List<BottomNavTabInfo>

    /**
     * Get the total number of tabs.
     *
     * @return Tab count
     */
    suspend fun getTabCount(): Int

    // ═══════════════════════════════════════════════════════════════════════════
    // Badge Operations
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Get tabs that have notification badges.
     *
     * @return List of tabs with badges
     */
    suspend fun getTabsWithBadges(): List<BottomNavTabInfo>

    /**
     * Check if a specific tab has a badge.
     *
     * @param tabIndex Tab index to check
     * @return True if tab has a badge
     */
    suspend fun hasBadge(tabIndex: Int): Boolean
}
