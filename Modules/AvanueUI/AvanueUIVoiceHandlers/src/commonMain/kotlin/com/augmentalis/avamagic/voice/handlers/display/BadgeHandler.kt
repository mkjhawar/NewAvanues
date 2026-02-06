/**
 * BadgeHandler.kt - Voice handler for Badge/Notification indicator interactions
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2026-01-28
 *
 * Purpose: Voice-driven badge control for notification indicators and count displays
 * Features:
 * - Show badge with specific count value
 * - Hide/clear badge to remove indicators
 * - Update badge count dynamically
 * - Named badge targeting (e.g., "show badge on messages")
 * - Focused badge targeting for active elements
 * - AVID-based targeting for precise element selection
 * - Voice feedback for badge state changes
 *
 * Location: CommandManager module handlers
 *
 * ## Supported Commands
 *
 * Show badge:
 * - "show badge" - Show badge on focused element
 * - "show badge [count]" - Show badge with specific count
 * - "show badge on [name]" - Show badge on named element
 * - "badge [count]" - Shorthand for showing badge with count
 *
 * Hide badge:
 * - "hide badge" - Hide badge on focused element
 * - "clear badge" - Clear/remove badge
 * - "remove badge" - Remove badge indicator
 * - "hide badge on [name]" - Hide badge on named element
 *
 * Update badge:
 * - "update badge to [N]" - Update badge count
 * - "set badge to [N]" - Set badge to specific count
 * - "increment badge" - Increase badge count by 1
 * - "decrement badge" - Decrease badge count by 1
 *
 * ## Count Parsing
 *
 * Supports:
 * - Integer values: "5", "10", "99"
 * - Word numbers: "five", "ten", "twenty"
 * - Zero handling: "zero" clears badge
 */

package com.augmentalis.avamagic.voice.handlers.display

import com.avanues.logging.LoggerFactory
import com.augmentalis.voiceoscore.ActionCategory
import com.augmentalis.voiceoscore.BaseHandler
import com.augmentalis.voiceoscore.Bounds
import com.augmentalis.voiceoscore.ElementInfo
import com.augmentalis.voiceoscore.HandlerResult
import com.augmentalis.voiceoscore.QuantizedCommand

/**
 * Voice command handler for Badge/Notification indicator interactions.
 *
 * Provides comprehensive voice control for badge components including:
 * - Showing badges with count values
 * - Hiding/clearing badge indicators
 * - Updating badge counts dynamically
 * - Named badge targeting with disambiguation
 *
 * Thread Safety:
 * - All operations are suspend functions
 * - State modifications are atomic via executor
 *
 * @param executor Platform-specific executor for badge operations
 */
class BadgeHandler(
    private val executor: BadgeExecutor
) : BaseHandler() {

    companion object {
        private const val TAG = "BadgeHandler"
        private val Log = LoggerFactory.getLogger(TAG)

        // Patterns for parsing commands
        private val SHOW_BADGE_COUNT_PATTERN = Regex(
            """^(?:show\s+)?badge\s+(\d+|[a-z]+)$""",
            RegexOption.IGNORE_CASE
        )

        private val SHOW_BADGE_ON_PATTERN = Regex(
            """^show\s+badge\s+(?:on\s+)?(.+)$""",
            RegexOption.IGNORE_CASE
        )

        private val HIDE_BADGE_ON_PATTERN = Regex(
            """^(?:hide|clear|remove)\s+badge\s+(?:on\s+|from\s+)?(.+)$""",
            RegexOption.IGNORE_CASE
        )

        private val UPDATE_BADGE_PATTERN = Regex(
            """^(?:update|set)\s+badge\s+to\s+(\d+|[a-z]+)$""",
            RegexOption.IGNORE_CASE
        )

        // Word to number mapping for common spoken numbers
        private val WORD_NUMBERS = mapOf(
            "zero" to 0, "one" to 1, "two" to 2, "three" to 3, "four" to 4,
            "five" to 5, "six" to 6, "seven" to 7, "eight" to 8, "nine" to 9,
            "ten" to 10, "eleven" to 11, "twelve" to 12, "thirteen" to 13,
            "fourteen" to 14, "fifteen" to 15, "sixteen" to 16, "seventeen" to 17,
            "eighteen" to 18, "nineteen" to 19, "twenty" to 20, "thirty" to 30,
            "forty" to 40, "fifty" to 50, "sixty" to 60, "seventy" to 70,
            "eighty" to 80, "ninety" to 90, "hundred" to 100
        )
    }

    override val category: ActionCategory = ActionCategory.UI

    override val supportedActions: List<String> = listOf(
        // Show badge
        "show badge", "badge", "display badge",
        "show badge on", "add badge",
        // Hide badge
        "hide badge", "clear badge", "remove badge",
        "hide badge on", "clear badge from",
        // Update badge
        "update badge to", "set badge to",
        "increment badge", "decrement badge"
    )

    /**
     * Callback for voice feedback when badge state changes.
     */
    var onBadgeChanged: ((elementName: String, count: Int?, visible: Boolean) -> Unit)? = null

    override suspend fun execute(
        command: QuantizedCommand,
        params: Map<String, Any>
    ): HandlerResult {
        val normalizedAction = command.phrase.lowercase().trim()

        Log.d { "Processing badge command: $normalizedAction" }

        return try {
            when {
                // Update badge to specific count
                UPDATE_BADGE_PATTERN.containsMatchIn(normalizedAction) -> {
                    handleUpdateBadge(normalizedAction, command)
                }

                // Show badge with count
                SHOW_BADGE_COUNT_PATTERN.containsMatchIn(normalizedAction) -> {
                    handleShowBadgeWithCount(normalizedAction, command)
                }

                // Show badge on named element
                SHOW_BADGE_ON_PATTERN.containsMatchIn(normalizedAction) -> {
                    handleShowBadgeOn(normalizedAction, command)
                }

                // Hide badge on named element
                HIDE_BADGE_ON_PATTERN.containsMatchIn(normalizedAction) -> {
                    handleHideBadgeOn(normalizedAction, command)
                }

                // Simple show badge
                normalizedAction in listOf("show badge", "display badge", "add badge") -> {
                    handleShowBadge(command)
                }

                // Simple hide/clear badge
                normalizedAction in listOf("hide badge", "clear badge", "remove badge") -> {
                    handleHideBadge(command)
                }

                // Increment badge
                normalizedAction == "increment badge" -> {
                    handleIncrementBadge(command)
                }

                // Decrement badge
                normalizedAction == "decrement badge" -> {
                    handleDecrementBadge(command)
                }

                else -> HandlerResult.notHandled()
            }
        } catch (e: Exception) {
            Log.e({ "Error executing badge command" }, e)
            HandlerResult.failure("Error: ${e.message}", recoverable = true)
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Command Handlers
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Handle "show badge [count]" command.
     */
    private suspend fun handleShowBadgeWithCount(
        normalizedAction: String,
        command: QuantizedCommand
    ): HandlerResult {
        val matchResult = SHOW_BADGE_COUNT_PATTERN.find(normalizedAction)
            ?: return HandlerResult.failure("Could not parse badge count")

        val countString = matchResult.groupValues[1]
        val count = parseCount(countString)
            ?: return HandlerResult.Failure(
                reason = "Could not parse count: '$countString'",
                recoverable = true,
                suggestedAction = "Try 'show badge 5' or 'badge ten'"
            )

        val badgeInfo = findBadge(avid = command.targetAvid)
            ?: return HandlerResult.Failure(
                reason = "No badge element focused",
                recoverable = true,
                suggestedAction = "Focus on an element with a badge or say 'show badge on messages'"
            )

        return applyBadge(badgeInfo, count, visible = true)
    }

    /**
     * Handle "show badge on [name]" command.
     */
    private suspend fun handleShowBadgeOn(
        normalizedAction: String,
        command: QuantizedCommand
    ): HandlerResult {
        val matchResult = SHOW_BADGE_ON_PATTERN.find(normalizedAction)
            ?: return HandlerResult.failure("Could not parse badge command")

        val elementName = matchResult.groupValues[1].trim()

        // Check if name contains a count (e.g., "show badge 5 on messages")
        val countMatch = Regex("""(\d+)\s+on\s+(.+)""", RegexOption.IGNORE_CASE).find(elementName)
        val (count, targetName) = if (countMatch != null) {
            Pair(countMatch.groupValues[1].toIntOrNull(), countMatch.groupValues[2])
        } else {
            Pair(null, elementName)
        }

        val badgeInfo = findBadge(name = targetName, avid = command.targetAvid)
            ?: return HandlerResult.Failure(
                reason = "Element '$targetName' not found",
                recoverable = true,
                suggestedAction = "Check element name and try again"
            )

        return applyBadge(badgeInfo, count ?: badgeInfo.count, visible = true)
    }

    /**
     * Handle "hide/clear badge on [name]" command.
     */
    private suspend fun handleHideBadgeOn(
        normalizedAction: String,
        command: QuantizedCommand
    ): HandlerResult {
        val matchResult = HIDE_BADGE_ON_PATTERN.find(normalizedAction)
            ?: return HandlerResult.failure("Could not parse badge command")

        val elementName = matchResult.groupValues[1].trim()

        val badgeInfo = findBadge(name = elementName, avid = command.targetAvid)
            ?: return HandlerResult.Failure(
                reason = "Element '$elementName' not found",
                recoverable = true,
                suggestedAction = "Check element name and try again"
            )

        return applyBadge(badgeInfo, count = 0, visible = false)
    }

    /**
     * Handle "update badge to [N]" command.
     */
    private suspend fun handleUpdateBadge(
        normalizedAction: String,
        command: QuantizedCommand
    ): HandlerResult {
        val matchResult = UPDATE_BADGE_PATTERN.find(normalizedAction)
            ?: return HandlerResult.failure("Could not parse update command")

        val countString = matchResult.groupValues[1]
        val count = parseCount(countString)
            ?: return HandlerResult.Failure(
                reason = "Could not parse count: '$countString'",
                recoverable = true,
                suggestedAction = "Try 'update badge to 5' or 'set badge to ten'"
            )

        val badgeInfo = findBadge(avid = command.targetAvid)
            ?: return HandlerResult.Failure(
                reason = "No badge element focused",
                recoverable = true,
                suggestedAction = "Focus on an element with a badge first"
            )

        // If count is 0, hide the badge; otherwise show with new count
        return applyBadge(badgeInfo, count, visible = count > 0)
    }

    /**
     * Handle simple "show badge" command.
     */
    private suspend fun handleShowBadge(command: QuantizedCommand): HandlerResult {
        val badgeInfo = findBadge(avid = command.targetAvid)
            ?: return HandlerResult.Failure(
                reason = "No badge element focused",
                recoverable = true,
                suggestedAction = "Focus on an element or say 'show badge on messages'"
            )

        // Show badge with current count or 1 if none
        val count = if (badgeInfo.count > 0) badgeInfo.count else 1
        return applyBadge(badgeInfo, count, visible = true)
    }

    /**
     * Handle simple "hide badge" command.
     */
    private suspend fun handleHideBadge(command: QuantizedCommand): HandlerResult {
        val badgeInfo = findBadge(avid = command.targetAvid)
            ?: return HandlerResult.Failure(
                reason = "No badge element focused",
                recoverable = true,
                suggestedAction = "Focus on an element with a badge first"
            )

        return applyBadge(badgeInfo, count = 0, visible = false)
    }

    /**
     * Handle "increment badge" command.
     */
    private suspend fun handleIncrementBadge(command: QuantizedCommand): HandlerResult {
        val badgeInfo = findBadge(avid = command.targetAvid)
            ?: return HandlerResult.Failure(
                reason = "No badge element focused",
                recoverable = true,
                suggestedAction = "Focus on an element with a badge first"
            )

        val newCount = badgeInfo.count + 1
        return applyBadge(badgeInfo, newCount, visible = true)
    }

    /**
     * Handle "decrement badge" command.
     */
    private suspend fun handleDecrementBadge(command: QuantizedCommand): HandlerResult {
        val badgeInfo = findBadge(avid = command.targetAvid)
            ?: return HandlerResult.Failure(
                reason = "No badge element focused",
                recoverable = true,
                suggestedAction = "Focus on an element with a badge first"
            )

        val newCount = (badgeInfo.count - 1).coerceAtLeast(0)
        return applyBadge(badgeInfo, newCount, visible = newCount > 0)
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Helper Methods
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Find badge by name, AVID, or focus state.
     */
    private suspend fun findBadge(
        name: String? = null,
        avid: String? = null
    ): BadgeInfo? {
        // Priority 1: AVID lookup (fastest, most precise)
        if (avid != null) {
            val badge = executor.findBadgeByAvid(avid)
            if (badge != null) return badge
        }

        // Priority 2: Name lookup
        if (name != null) {
            val badge = executor.findBadgeByName(name)
            if (badge != null) return badge
        }

        // Priority 3: Focused element with badge
        return executor.findFocusedBadge()
    }

    /**
     * Apply badge state and return result.
     */
    private suspend fun applyBadge(
        badgeInfo: BadgeInfo,
        count: Int,
        visible: Boolean
    ): HandlerResult {
        val result = executor.setBadge(badgeInfo, count, visible)

        return if (result.success) {
            // Invoke callback for voice feedback
            onBadgeChanged?.invoke(
                badgeInfo.name.ifBlank { "Element" },
                if (visible) count else null,
                visible
            )

            // Build feedback message
            val feedback = buildString {
                if (badgeInfo.name.isNotBlank()) {
                    append(badgeInfo.name)
                    append(": ")
                }
                if (visible) {
                    append("Badge set to $count")
                } else {
                    append("Badge cleared")
                }
            }

            Log.i { "Badge updated: ${badgeInfo.name} = $count, visible = $visible" }

            HandlerResult.Success(
                message = feedback,
                data = mapOf(
                    "elementName" to badgeInfo.name,
                    "elementAvid" to badgeInfo.avid,
                    "previousCount" to badgeInfo.count,
                    "newCount" to count,
                    "visible" to visible,
                    "accessibility_announcement" to feedback
                )
            )
        } else {
            HandlerResult.failure(
                reason = result.error ?: "Could not update badge",
                recoverable = true
            )
        }
    }

    /**
     * Parse a count string into an integer.
     *
     * Supports:
     * - "5" -> 5
     * - "five" -> 5
     * - "twenty" -> 20
     */
    private fun parseCount(input: String): Int? {
        val trimmed = input.trim().lowercase()

        // Try direct numeric parsing
        trimmed.toIntOrNull()?.let { return it }

        // Try word number parsing
        return WORD_NUMBERS[trimmed]
    }

}

// ═══════════════════════════════════════════════════════════════════════════════
// Supporting Types
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * Information about a badge component.
 *
 * @property avid AVID fingerprint for the badge element (format: BDG:{hash8})
 * @property name Display name or associated label of the parent element
 * @property count Current badge count (0 if not shown)
 * @property isVisible Whether the badge is currently visible
 * @property maxCount Maximum displayable count (e.g., 99 for "99+")
 * @property bounds Screen bounds for the badge
 * @property isFocused Whether this element currently has focus
 * @property node Platform-specific node reference
 */
data class BadgeInfo(
    val avid: String,
    val name: String = "",
    val count: Int = 0,
    val isVisible: Boolean = false,
    val maxCount: Int = 99,
    val bounds: Bounds = Bounds.EMPTY,
    val isFocused: Boolean = false,
    val node: Any? = null
) {
    /**
     * Display string for the badge count (e.g., "99+" if over max).
     */
    val displayCount: String
        get() = if (count > maxCount) "$maxCount+" else count.toString()

    /**
     * Convert to ElementInfo for general element operations.
     */
    fun toElementInfo(): ElementInfo = ElementInfo(
        className = "Badge",
        text = name,
        bounds = bounds,
        isClickable = false,
        isEnabled = true,
        avid = avid,
        stateDescription = if (isVisible) displayCount else "No badge"
    )
}

/**
 * Result of a badge operation.
 */
data class BadgeOperationResult(
    val success: Boolean,
    val error: String? = null,
    val previousCount: Int = 0,
    val newCount: Int = 0,
    val wasVisible: Boolean = false,
    val isVisible: Boolean = false
) {
    companion object {
        fun success(
            previousCount: Int,
            newCount: Int,
            wasVisible: Boolean,
            isVisible: Boolean
        ) = BadgeOperationResult(
            success = true,
            previousCount = previousCount,
            newCount = newCount,
            wasVisible = wasVisible,
            isVisible = isVisible
        )

        fun error(message: String) = BadgeOperationResult(
            success = false,
            error = message
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// Platform Executor Interface
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * Platform-specific executor for badge operations.
 *
 * Implementations should:
 * 1. Find badge components by AVID, name, or focus state
 * 2. Read current badge counts and visibility
 * 3. Set badge counts and visibility via UI updates
 * 4. Handle various badge implementations (Material, custom)
 *
 * ## Badge Detection Algorithm
 *
 * ```kotlin
 * fun findBadgeNode(rootNode: AccessibilityNodeInfo): AccessibilityNodeInfo? {
 *     // Look for nodes with className:
 *     // - com.google.android.material.badge.BadgeDrawable
 *     // - Custom badge implementations
 *     // - Views with badge-related contentDescription
 * }
 * ```
 *
 * ## Badge Update Algorithm
 *
 * ```kotlin
 * fun updateBadge(node: AccessibilityNodeInfo, count: Int, visible: Boolean) {
 *     // Update badge count
 *     // Toggle visibility
 *     // Refresh accessibility announcement
 * }
 * ```
 */
interface BadgeExecutor {

    // ═══════════════════════════════════════════════════════════════════════════
    // Badge Discovery
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Find a badge by its AVID fingerprint.
     *
     * @param avid The AVID fingerprint (format: BDG:{hash8})
     * @return BadgeInfo if found, null otherwise
     */
    suspend fun findBadgeByAvid(avid: String): BadgeInfo?

    /**
     * Find a badge by its parent element name or associated label.
     *
     * Searches for:
     * 1. Element with badge and matching contentDescription
     * 2. Element with badge and matching text
     * 3. Element with badge and associated label nearby
     *
     * @param name The name to search for (case-insensitive)
     * @return BadgeInfo if found, null otherwise
     */
    suspend fun findBadgeByName(name: String): BadgeInfo?

    /**
     * Find the badge on the currently focused element.
     *
     * @return BadgeInfo if focused element has a badge, null otherwise
     */
    suspend fun findFocusedBadge(): BadgeInfo?

    /**
     * Get all visible badges on the current screen.
     *
     * @return List of all visible badge components
     */
    suspend fun getAllBadges(): List<BadgeInfo>

    // ═══════════════════════════════════════════════════════════════════════════
    // Badge Operations
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Set the badge count and visibility.
     *
     * @param badge The badge to modify
     * @param count The new count value
     * @param visible Whether the badge should be visible
     * @return Operation result with previous and new states
     */
    suspend fun setBadge(badge: BadgeInfo, count: Int, visible: Boolean): BadgeOperationResult

    /**
     * Get the current count of a badge.
     *
     * @param badge The badge to query
     * @return Current count, or null if unable to read
     */
    suspend fun getCount(badge: BadgeInfo): Int?

    /**
     * Check if a badge is currently visible.
     *
     * @param badge The badge to query
     * @return true if visible, false otherwise
     */
    suspend fun isVisible(badge: BadgeInfo): Boolean

    // ═══════════════════════════════════════════════════════════════════════════
    // Convenience Operations
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Show the badge with the specified count.
     *
     * @param badge The badge to show
     * @param count The count to display (default 1)
     * @return Operation result
     */
    suspend fun show(badge: BadgeInfo, count: Int = 1): BadgeOperationResult

    /**
     * Hide the badge.
     *
     * @param badge The badge to hide
     * @return Operation result
     */
    suspend fun hide(badge: BadgeInfo): BadgeOperationResult

    /**
     * Increment the badge count by 1.
     *
     * @param badge The badge to increment
     * @return Operation result
     */
    suspend fun increment(badge: BadgeInfo): BadgeOperationResult

    /**
     * Decrement the badge count by 1 (minimum 0).
     *
     * @param badge The badge to decrement
     * @return Operation result
     */
    suspend fun decrement(badge: BadgeInfo): BadgeOperationResult

    /**
     * Clear the badge (hide and reset count to 0).
     *
     * @param badge The badge to clear
     * @return Operation result
     */
    suspend fun clear(badge: BadgeInfo): BadgeOperationResult
}
