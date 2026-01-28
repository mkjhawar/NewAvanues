/**
 * BreadcrumbHandler.kt - Voice handler for Breadcrumb navigation interactions
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2026-01-28
 *
 * Purpose: Voice-driven breadcrumb navigation with hierarchical path traversal
 * Features:
 * - Navigate to specific breadcrumb by name
 * - Navigate to Nth breadcrumb position (1-indexed)
 * - Quick navigation to home/root
 * - Navigate up one or multiple levels
 * - Announce current location in breadcrumb trail
 * - AVID-based targeting for precise element selection
 * - Voice feedback for navigation changes
 *
 * Location: CommandManager module handlers
 *
 * ## Supported Commands
 *
 * Navigate by name:
 * - "go to [name]" - Navigate to breadcrumb by name
 * - "navigate to [name]" - Navigate to breadcrumb by name
 *
 * Navigate by position:
 * - "breadcrumb [N]" - Navigate to Nth breadcrumb (1-indexed)
 * - "level [N]" - Navigate to Nth breadcrumb (1-indexed)
 *
 * Navigate to home:
 * - "home" - Navigate to first/home breadcrumb
 * - "root" - Navigate to first/home breadcrumb
 * - "start" - Navigate to first/home breadcrumb
 *
 * Navigate up:
 * - "back" - Navigate up one level
 * - "up" - Navigate up one level
 * - "parent" - Navigate up one level
 * - "back [N] levels" - Navigate up N levels
 *
 * Announce location:
 * - "current" - Announce current location
 * - "where am I" - Announce current location
 */

package com.augmentalis.avamagic.voice.handlers.navigation

import android.util.Log
import com.augmentalis.voiceoscore.ActionCategory
import com.augmentalis.voiceoscore.BaseHandler
import com.augmentalis.voiceoscore.Bounds
import com.augmentalis.voiceoscore.HandlerResult
import com.augmentalis.voiceoscore.QuantizedCommand

/**
 * Voice command handler for Breadcrumb navigation interactions.
 *
 * Provides comprehensive voice control for breadcrumb components including:
 * - Navigation by breadcrumb name (fuzzy matching)
 * - Navigation by position (1-indexed)
 * - Quick home/root navigation
 * - Hierarchical level navigation (up one or multiple levels)
 * - Current location announcements
 *
 * Thread Safety:
 * - All operations are suspend functions
 * - State modifications are atomic via executor
 *
 * @param executor Platform-specific executor for breadcrumb operations
 */
class BreadcrumbHandler(
    private val executor: BreadcrumbExecutor
) : BaseHandler() {

    companion object {
        private const val TAG = "BreadcrumbHandler"

        // Patterns for parsing commands
        private val GO_TO_PATTERN = Regex(
            """^(?:go\s+to|navigate\s+to)\s+(.+)$""",
            RegexOption.IGNORE_CASE
        )

        private val BREADCRUMB_POSITION_PATTERN = Regex(
            """^(?:breadcrumb|level)\s+(\d+)$""",
            RegexOption.IGNORE_CASE
        )

        private val BACK_LEVELS_PATTERN = Regex(
            """^back\s+(\d+)\s+levels?$""",
            RegexOption.IGNORE_CASE
        )

        // Word to number mapping for common spoken numbers
        private val WORD_NUMBERS = mapOf(
            "one" to 1, "two" to 2, "three" to 3, "four" to 4, "five" to 5,
            "six" to 6, "seven" to 7, "eight" to 8, "nine" to 9, "ten" to 10,
            "first" to 1, "second" to 2, "third" to 3, "fourth" to 4, "fifth" to 5,
            "sixth" to 6, "seventh" to 7, "eighth" to 8, "ninth" to 9, "tenth" to 10
        )
    }

    override val category: ActionCategory = ActionCategory.NAVIGATION

    override val supportedActions: List<String> = listOf(
        // Navigate by name
        "go to", "navigate to",
        // Navigate by position
        "breadcrumb", "level",
        // Home navigation
        "home", "root", "start",
        // Up navigation
        "back", "up", "parent",
        "back [N] levels",
        // Location announcement
        "current", "where am I"
    )

    /**
     * Callback for voice feedback when navigation occurs.
     */
    var onNavigated: ((breadcrumbName: String, path: String) -> Unit)? = null

    /**
     * Callback for location announcement.
     */
    var onLocationAnnounced: ((currentName: String, fullPath: String) -> Unit)? = null

    override suspend fun execute(
        command: QuantizedCommand,
        params: Map<String, Any>
    ): HandlerResult {
        val normalizedAction = command.phrase.lowercase().trim()

        Log.d(TAG, "Processing breadcrumb command: $normalizedAction")

        return try {
            when {
                // Navigate by name: "go to [name]" or "navigate to [name]"
                GO_TO_PATTERN.containsMatchIn(normalizedAction) -> {
                    handleGoTo(normalizedAction, command)
                }

                // Navigate by position: "breadcrumb [N]" or "level [N]"
                BREADCRUMB_POSITION_PATTERN.containsMatchIn(normalizedAction) -> {
                    handleBreadcrumbPosition(normalizedAction, command)
                }

                // Navigate up multiple levels: "back [N] levels"
                BACK_LEVELS_PATTERN.containsMatchIn(normalizedAction) -> {
                    handleBackLevels(normalizedAction, command)
                }

                // Home navigation
                normalizedAction in listOf("home", "root", "start") -> {
                    handleHome(command)
                }

                // Up navigation
                normalizedAction in listOf("back", "up", "parent") -> {
                    handleUp(command)
                }

                // Location announcement
                normalizedAction in listOf("current", "where am i") -> {
                    handleWhereAmI(command)
                }

                else -> HandlerResult.notHandled()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error executing breadcrumb command", e)
            HandlerResult.failure("Error: ${e.message}", recoverable = true)
        }
    }

    // ===============================================================================
    // Command Handlers
    // ===============================================================================

    /**
     * Handle "go to [name]" or "navigate to [name]" command.
     */
    private suspend fun handleGoTo(
        normalizedAction: String,
        command: QuantizedCommand
    ): HandlerResult {
        val matchResult = GO_TO_PATTERN.find(normalizedAction)
            ?: return HandlerResult.failure("Could not parse go to command")

        val targetName = matchResult.groupValues[1].trim()

        // Find the breadcrumb component
        val breadcrumbInfo = findBreadcrumb(avid = command.targetAvid)
            ?: return HandlerResult.Failure(
                reason = "No breadcrumb navigation found",
                recoverable = true,
                suggestedAction = "Make sure a breadcrumb component is visible"
            )

        // Find the breadcrumb item by name
        val targetItem = breadcrumbInfo.breadcrumbs.find { item ->
            item.name.equals(targetName, ignoreCase = true)
        } ?: return HandlerResult.Failure(
            reason = "Breadcrumb '$targetName' not found",
            recoverable = true,
            suggestedAction = "Available: ${breadcrumbInfo.breadcrumbs.joinToString(", ") { it.name }}"
        )

        // Check if clickable
        if (!targetItem.isClickable) {
            return HandlerResult.Failure(
                reason = "'${targetItem.name}' is the current location",
                recoverable = true,
                suggestedAction = "Try navigating to a different breadcrumb"
            )
        }

        // Navigate
        return navigateTo(breadcrumbInfo, targetItem)
    }

    /**
     * Handle "breadcrumb [N]" or "level [N]" command.
     */
    private suspend fun handleBreadcrumbPosition(
        normalizedAction: String,
        command: QuantizedCommand
    ): HandlerResult {
        val matchResult = BREADCRUMB_POSITION_PATTERN.find(normalizedAction)
            ?: return HandlerResult.failure("Could not parse breadcrumb position command")

        val positionStr = matchResult.groupValues[1]
        val position = positionStr.toIntOrNull()
            ?: WORD_NUMBERS[positionStr.lowercase()]
            ?: return HandlerResult.Failure(
                reason = "Could not parse position: '$positionStr'",
                recoverable = true,
                suggestedAction = "Try 'breadcrumb 1' or 'level 2'"
            )

        // Find the breadcrumb component
        val breadcrumbInfo = findBreadcrumb(avid = command.targetAvid)
            ?: return HandlerResult.Failure(
                reason = "No breadcrumb navigation found",
                recoverable = true,
                suggestedAction = "Make sure a breadcrumb component is visible"
            )

        // Validate position (1-indexed)
        val index = position - 1
        if (index < 0 || index >= breadcrumbInfo.breadcrumbs.size) {
            return HandlerResult.Failure(
                reason = "Position $position is out of range (1-${breadcrumbInfo.breadcrumbs.size})",
                recoverable = true,
                suggestedAction = "Try a number between 1 and ${breadcrumbInfo.breadcrumbs.size}"
            )
        }

        val targetItem = breadcrumbInfo.breadcrumbs[index]

        // Check if clickable
        if (!targetItem.isClickable) {
            return HandlerResult.Failure(
                reason = "'${targetItem.name}' is the current location",
                recoverable = true,
                suggestedAction = "Try navigating to a different breadcrumb"
            )
        }

        // Navigate by index
        return navigateByIndex(breadcrumbInfo, index)
    }

    /**
     * Handle "back [N] levels" command.
     */
    private suspend fun handleBackLevels(
        normalizedAction: String,
        command: QuantizedCommand
    ): HandlerResult {
        val matchResult = BACK_LEVELS_PATTERN.find(normalizedAction)
            ?: return HandlerResult.failure("Could not parse back levels command")

        val levelsStr = matchResult.groupValues[1]
        val levels = levelsStr.toIntOrNull()
            ?: WORD_NUMBERS[levelsStr.lowercase()]
            ?: return HandlerResult.Failure(
                reason = "Could not parse levels: '$levelsStr'",
                recoverable = true,
                suggestedAction = "Try 'back 2 levels' or 'back 3 levels'"
            )

        if (levels < 1) {
            return HandlerResult.Failure(
                reason = "Invalid number of levels: $levels",
                recoverable = true,
                suggestedAction = "Specify at least 1 level"
            )
        }

        // Find the breadcrumb component
        val breadcrumbInfo = findBreadcrumb(avid = command.targetAvid)
            ?: return HandlerResult.Failure(
                reason = "No breadcrumb navigation found",
                recoverable = true,
                suggestedAction = "Make sure a breadcrumb component is visible"
            )

        // Check if we can go up that many levels
        if (breadcrumbInfo.currentIndex < levels) {
            return HandlerResult.Failure(
                reason = "Cannot go back $levels levels (only ${breadcrumbInfo.currentIndex} available)",
                recoverable = true,
                suggestedAction = "Try 'back ${breadcrumbInfo.currentIndex} levels' or 'home'"
            )
        }

        // Navigate up N levels
        return goUpLevels(breadcrumbInfo, levels)
    }

    /**
     * Handle "home", "root", or "start" command.
     */
    private suspend fun handleHome(command: QuantizedCommand): HandlerResult {
        // Find the breadcrumb component
        val breadcrumbInfo = findBreadcrumb(avid = command.targetAvid)
            ?: return HandlerResult.Failure(
                reason = "No breadcrumb navigation found",
                recoverable = true,
                suggestedAction = "Make sure a breadcrumb component is visible"
            )

        // Check if already at home
        if (breadcrumbInfo.currentIndex == 0) {
            return HandlerResult.Success(
                message = "Already at ${breadcrumbInfo.breadcrumbs.firstOrNull()?.name ?: "home"}",
                data = mapOf(
                    "currentLocation" to (breadcrumbInfo.breadcrumbs.firstOrNull()?.name ?: "home"),
                    "atHome" to true
                )
            )
        }

        // Navigate to home
        return goHome(breadcrumbInfo)
    }

    /**
     * Handle "back", "up", or "parent" command.
     */
    private suspend fun handleUp(command: QuantizedCommand): HandlerResult {
        // Find the breadcrumb component
        val breadcrumbInfo = findBreadcrumb(avid = command.targetAvid)
            ?: return HandlerResult.Failure(
                reason = "No breadcrumb navigation found",
                recoverable = true,
                suggestedAction = "Make sure a breadcrumb component is visible"
            )

        // Check if at root
        if (breadcrumbInfo.currentIndex == 0) {
            return HandlerResult.Failure(
                reason = "Already at root level",
                recoverable = true,
                suggestedAction = "Cannot go up further"
            )
        }

        // Navigate up one level
        return goUp(breadcrumbInfo)
    }

    /**
     * Handle "current" or "where am I" command.
     */
    private suspend fun handleWhereAmI(command: QuantizedCommand): HandlerResult {
        // Find the breadcrumb component
        val breadcrumbInfo = findBreadcrumb(avid = command.targetAvid)
            ?: return HandlerResult.Failure(
                reason = "No breadcrumb navigation found",
                recoverable = true,
                suggestedAction = "Make sure a breadcrumb component is visible"
            )

        // Get current location
        val locationResult = executor.getCurrentLocation(breadcrumbInfo)

        if (!locationResult.success) {
            return HandlerResult.failure(
                reason = locationResult.error ?: "Could not determine current location",
                recoverable = true
            )
        }

        val currentItem = breadcrumbInfo.breadcrumbs.getOrNull(breadcrumbInfo.currentIndex)
        val currentName = currentItem?.name ?: "Unknown"
        val fullPath = breadcrumbInfo.breadcrumbs
            .take(breadcrumbInfo.currentIndex + 1)
            .joinToString(" > ") { it.name }

        // Invoke callback
        onLocationAnnounced?.invoke(currentName, fullPath)

        val feedback = "You are at $currentName. Full path: $fullPath"

        Log.i(TAG, "Location announced: $fullPath")

        return HandlerResult.Success(
            message = feedback,
            data = mapOf(
                "currentName" to currentName,
                "currentIndex" to breadcrumbInfo.currentIndex,
                "fullPath" to fullPath,
                "totalLevels" to breadcrumbInfo.breadcrumbs.size,
                "breadcrumbs" to breadcrumbInfo.breadcrumbs.map { it.name },
                "accessibility_announcement" to feedback
            )
        )
    }

    // ===============================================================================
    // Helper Methods
    // ===============================================================================

    /**
     * Find breadcrumb by AVID or focus state.
     */
    private suspend fun findBreadcrumb(
        name: String? = null,
        avid: String? = null
    ): BreadcrumbInfo? {
        // Priority 1: AVID lookup (fastest, most precise)
        if (avid != null) {
            val breadcrumb = executor.findByAvid(avid)
            if (breadcrumb != null) return breadcrumb
        }

        // Priority 2: Name lookup
        if (name != null) {
            val breadcrumb = executor.findByName(name)
            if (breadcrumb != null) return breadcrumb
        }

        // Priority 3: Focused breadcrumb
        return executor.findFocused()
    }

    /**
     * Navigate to a specific breadcrumb item.
     */
    private suspend fun navigateTo(
        breadcrumbInfo: BreadcrumbInfo,
        targetItem: BreadcrumbItem
    ): HandlerResult {
        val result = executor.navigateTo(breadcrumbInfo, targetItem)

        return if (result.success) {
            val fullPath = breadcrumbInfo.breadcrumbs
                .takeWhile { it.id != targetItem.id }
                .plus(targetItem)
                .joinToString(" > ") { it.name }

            // Invoke callback
            onNavigated?.invoke(targetItem.name, fullPath)

            val feedback = "Navigated to ${targetItem.name}"

            Log.i(TAG, "Navigated to: ${targetItem.name}")

            HandlerResult.Success(
                message = feedback,
                data = mapOf(
                    "navigatedTo" to targetItem.name,
                    "breadcrumbId" to targetItem.id,
                    "breadcrumbAvid" to breadcrumbInfo.avid,
                    "fullPath" to fullPath,
                    "accessibility_announcement" to feedback
                )
            )
        } else {
            HandlerResult.failure(
                reason = result.error ?: "Could not navigate to ${targetItem.name}",
                recoverable = true
            )
        }
    }

    /**
     * Navigate to a breadcrumb by index.
     */
    private suspend fun navigateByIndex(
        breadcrumbInfo: BreadcrumbInfo,
        index: Int
    ): HandlerResult {
        val result = executor.navigateByIndex(breadcrumbInfo, index)

        return if (result.success) {
            val targetItem = breadcrumbInfo.breadcrumbs[index]
            val fullPath = breadcrumbInfo.breadcrumbs
                .take(index + 1)
                .joinToString(" > ") { it.name }

            // Invoke callback
            onNavigated?.invoke(targetItem.name, fullPath)

            val feedback = "Navigated to ${targetItem.name}"

            Log.i(TAG, "Navigated to index $index: ${targetItem.name}")

            HandlerResult.Success(
                message = feedback,
                data = mapOf(
                    "navigatedTo" to targetItem.name,
                    "navigatedToIndex" to index,
                    "breadcrumbAvid" to breadcrumbInfo.avid,
                    "fullPath" to fullPath,
                    "accessibility_announcement" to feedback
                )
            )
        } else {
            HandlerResult.failure(
                reason = result.error ?: "Could not navigate to position ${index + 1}",
                recoverable = true
            )
        }
    }

    /**
     * Navigate to the home/root breadcrumb.
     */
    private suspend fun goHome(breadcrumbInfo: BreadcrumbInfo): HandlerResult {
        val result = executor.goHome(breadcrumbInfo)

        return if (result.success) {
            val homeItem = breadcrumbInfo.breadcrumbs.firstOrNull()
            val homeName = homeItem?.name ?: "Home"

            // Invoke callback
            onNavigated?.invoke(homeName, homeName)

            val feedback = "Navigated to $homeName"

            Log.i(TAG, "Navigated to home: $homeName")

            HandlerResult.Success(
                message = feedback,
                data = mapOf(
                    "navigatedTo" to homeName,
                    "navigatedToIndex" to 0,
                    "breadcrumbAvid" to breadcrumbInfo.avid,
                    "isHome" to true,
                    "accessibility_announcement" to feedback
                )
            )
        } else {
            HandlerResult.failure(
                reason = result.error ?: "Could not navigate to home",
                recoverable = true
            )
        }
    }

    /**
     * Navigate up one level.
     */
    private suspend fun goUp(breadcrumbInfo: BreadcrumbInfo): HandlerResult {
        val result = executor.goUp(breadcrumbInfo)

        return if (result.success) {
            val parentIndex = breadcrumbInfo.currentIndex - 1
            val parentItem = breadcrumbInfo.breadcrumbs.getOrNull(parentIndex)
            val parentName = parentItem?.name ?: "Parent"
            val fullPath = breadcrumbInfo.breadcrumbs
                .take(parentIndex + 1)
                .joinToString(" > ") { it.name }

            // Invoke callback
            onNavigated?.invoke(parentName, fullPath)

            val feedback = "Navigated up to $parentName"

            Log.i(TAG, "Navigated up to: $parentName")

            HandlerResult.Success(
                message = feedback,
                data = mapOf(
                    "navigatedTo" to parentName,
                    "navigatedToIndex" to parentIndex,
                    "breadcrumbAvid" to breadcrumbInfo.avid,
                    "fullPath" to fullPath,
                    "accessibility_announcement" to feedback
                )
            )
        } else {
            HandlerResult.failure(
                reason = result.error ?: "Could not navigate up",
                recoverable = true
            )
        }
    }

    /**
     * Navigate up multiple levels.
     */
    private suspend fun goUpLevels(
        breadcrumbInfo: BreadcrumbInfo,
        levels: Int
    ): HandlerResult {
        val result = executor.goUpLevels(breadcrumbInfo, levels)

        return if (result.success) {
            val targetIndex = breadcrumbInfo.currentIndex - levels
            val targetItem = breadcrumbInfo.breadcrumbs.getOrNull(targetIndex)
            val targetName = targetItem?.name ?: "Unknown"
            val fullPath = breadcrumbInfo.breadcrumbs
                .take(targetIndex + 1)
                .joinToString(" > ") { it.name }

            // Invoke callback
            onNavigated?.invoke(targetName, fullPath)

            val feedback = "Navigated up $levels levels to $targetName"

            Log.i(TAG, "Navigated up $levels levels to: $targetName")

            HandlerResult.Success(
                message = feedback,
                data = mapOf(
                    "navigatedTo" to targetName,
                    "navigatedToIndex" to targetIndex,
                    "levelsNavigated" to levels,
                    "breadcrumbAvid" to breadcrumbInfo.avid,
                    "fullPath" to fullPath,
                    "accessibility_announcement" to feedback
                )
            )
        } else {
            HandlerResult.failure(
                reason = result.error ?: "Could not navigate up $levels levels",
                recoverable = true
            )
        }
    }

}

// ===================================================================================
// Supporting Types
// ===================================================================================

/**
 * Information about a breadcrumb navigation component.
 *
 * @property avid AVID fingerprint for the breadcrumb (format: BCR:{hash8})
 * @property name Display name or associated label
 * @property breadcrumbs Ordered list of breadcrumb items (first = root/home)
 * @property currentIndex Index of the currently selected breadcrumb (0-indexed)
 * @property bounds Screen bounds for the breadcrumb component
 * @property isFocused Whether this breadcrumb component currently has focus
 * @property node Platform-specific node reference
 */
data class BreadcrumbInfo(
    val avid: String,
    val name: String = "",
    val breadcrumbs: List<BreadcrumbItem> = emptyList(),
    val currentIndex: Int = 0,
    val bounds: Bounds = Bounds.EMPTY,
    val isFocused: Boolean = false,
    val node: Any? = null
) {
    /**
     * Get the current breadcrumb item.
     */
    val currentItem: BreadcrumbItem?
        get() = breadcrumbs.getOrNull(currentIndex)

    /**
     * Get the full path as a string.
     */
    val fullPath: String
        get() = breadcrumbs
            .take(currentIndex + 1)
            .joinToString(" > ") { it.name }

    /**
     * Check if at the root/home level.
     */
    val isAtRoot: Boolean
        get() = currentIndex == 0

    /**
     * Get the number of levels available to go up.
     */
    val levelsToRoot: Int
        get() = currentIndex
}

/**
 * Individual breadcrumb item within the navigation trail.
 *
 * @property id Unique identifier for the breadcrumb item
 * @property name Display name of the breadcrumb
 * @property url Optional URL or path associated with this breadcrumb
 * @property isClickable Whether this breadcrumb can be clicked (false for current item)
 * @property isCurrent Whether this is the currently selected breadcrumb
 */
data class BreadcrumbItem(
    val id: String,
    val name: String,
    val url: String? = null,
    val isClickable: Boolean = true,
    val isCurrent: Boolean = false
)

/**
 * Result of a breadcrumb operation.
 */
data class BreadcrumbOperationResult(
    val success: Boolean,
    val error: String? = null,
    val previousIndex: Int = 0,
    val newIndex: Int = 0,
    val navigatedTo: String? = null
) {
    companion object {
        fun success(
            previousIndex: Int,
            newIndex: Int,
            navigatedTo: String? = null
        ) = BreadcrumbOperationResult(
            success = true,
            previousIndex = previousIndex,
            newIndex = newIndex,
            navigatedTo = navigatedTo
        )

        fun error(message: String) = BreadcrumbOperationResult(
            success = false,
            error = message
        )
    }
}

// ===================================================================================
// Platform Executor Interface
// ===================================================================================

/**
 * Platform-specific executor for breadcrumb operations.
 *
 * Implementations should:
 * 1. Find breadcrumb components by AVID, name, or focus state
 * 2. Read breadcrumb items and their properties
 * 3. Navigate to specific breadcrumb items via clicks
 * 4. Handle various breadcrumb UI patterns (web-style, mobile, custom)
 *
 * ## Breadcrumb Detection Algorithm
 *
 * ```kotlin
 * fun findBreadcrumbNode(rootNode: AccessibilityNodeInfo): AccessibilityNodeInfo? {
 *     // Look for nodes with:
 *     // - roleDescription containing "breadcrumb"
 *     // - className containing "Breadcrumb"
 *     // - Navigation patterns with ">" or "/" separators
 *     // - ViewGroup with multiple clickable text children in hierarchy pattern
 * }
 * ```
 *
 * ## Navigation Algorithm
 *
 * ```kotlin
 * fun navigateTo(breadcrumb: BreadcrumbInfo, item: BreadcrumbItem): Boolean {
 *     // 1. Find the clickable element for the target item
 *     // 2. Perform click action
 *     // 3. Verify navigation occurred (URL change, focus change, etc.)
 * }
 * ```
 */
interface BreadcrumbExecutor {

    // ===============================================================================
    // Breadcrumb Discovery
    // ===============================================================================

    /**
     * Find a breadcrumb component by its AVID fingerprint.
     *
     * @param avid The AVID fingerprint (format: BCR:{hash8})
     * @return BreadcrumbInfo if found, null otherwise
     */
    suspend fun findByAvid(avid: String): BreadcrumbInfo?

    /**
     * Find a breadcrumb component by its name or associated label.
     *
     * Searches for:
     * 1. Breadcrumb with matching contentDescription
     * 2. Breadcrumb with aria-label matching name
     * 3. Breadcrumb component in a labeled container
     *
     * @param name The name to search for (case-insensitive)
     * @return BreadcrumbInfo if found, null otherwise
     */
    suspend fun findByName(name: String): BreadcrumbInfo?

    /**
     * Find the currently focused breadcrumb component.
     *
     * @return BreadcrumbInfo if a breadcrumb has focus, null otherwise
     */
    suspend fun findFocused(): BreadcrumbInfo?

    // ===============================================================================
    // Navigation Operations
    // ===============================================================================

    /**
     * Navigate to a specific breadcrumb item.
     *
     * @param breadcrumb The breadcrumb component
     * @param item The target breadcrumb item to navigate to
     * @return Operation result
     */
    suspend fun navigateTo(breadcrumb: BreadcrumbInfo, item: BreadcrumbItem): BreadcrumbOperationResult

    /**
     * Navigate to a breadcrumb by its index.
     *
     * @param breadcrumb The breadcrumb component
     * @param index The 0-indexed position to navigate to
     * @return Operation result
     */
    suspend fun navigateByIndex(breadcrumb: BreadcrumbInfo, index: Int): BreadcrumbOperationResult

    /**
     * Navigate to the home/root breadcrumb (first item).
     *
     * @param breadcrumb The breadcrumb component
     * @return Operation result
     */
    suspend fun goHome(breadcrumb: BreadcrumbInfo): BreadcrumbOperationResult

    /**
     * Navigate up one level (to parent breadcrumb).
     *
     * @param breadcrumb The breadcrumb component
     * @return Operation result
     */
    suspend fun goUp(breadcrumb: BreadcrumbInfo): BreadcrumbOperationResult

    /**
     * Navigate up multiple levels.
     *
     * @param breadcrumb The breadcrumb component
     * @param levels Number of levels to navigate up
     * @return Operation result
     */
    suspend fun goUpLevels(breadcrumb: BreadcrumbInfo, levels: Int): BreadcrumbOperationResult

    // ===============================================================================
    // Information Retrieval
    // ===============================================================================

    /**
     * Get the current location information.
     *
     * @param breadcrumb The breadcrumb component
     * @return Operation result with location details
     */
    suspend fun getCurrentLocation(breadcrumb: BreadcrumbInfo): BreadcrumbOperationResult

    /**
     * Get all breadcrumb items for a breadcrumb component.
     *
     * Refreshes the breadcrumb items from the current UI state.
     *
     * @param breadcrumb The breadcrumb component
     * @return Updated list of breadcrumb items, or null if unable to read
     */
    suspend fun getBreadcrumbs(breadcrumb: BreadcrumbInfo): List<BreadcrumbItem>?
}
