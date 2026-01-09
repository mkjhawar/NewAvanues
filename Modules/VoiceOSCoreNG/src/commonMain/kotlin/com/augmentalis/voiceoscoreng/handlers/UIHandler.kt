/**
 * UIHandler.kt - Handles UI element interaction with disambiguation support
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2026-01-06
 * Updated: 2026-01-08 - Added disambiguation for duplicate elements
 *
 * KMP handler for UI element interactions (click, tap, press, etc.).
 *
 * ## Disambiguation Flow
 *
 * When a command like "click Submit" matches multiple elements:
 * 1. System finds all matching elements
 * 2. Numbers (1, 2, 3...) are shown ONLY on matching elements
 * 3. User says a number to select
 * 4. Action executes on selected element
 *
 * Non-matching elements are NOT numbered.
 */
package com.augmentalis.voiceoscoreng.handlers

import com.augmentalis.voiceoscoreng.common.DisambiguationResult
import com.augmentalis.voiceoscoreng.common.ElementDisambiguator
import com.augmentalis.voiceoscoreng.common.ElementInfo
import com.augmentalis.voiceoscoreng.common.NumberedMatch
import com.augmentalis.voiceoscoreng.common.QuantizedCommand

/**
 * Handler for UI element interactions.
 *
 * Supports:
 * - Click actions: click, tap, press
 * - Long click: long click, long press
 * - Double tap: double tap, double click
 * - Toggle actions: expand, collapse, check, uncheck, toggle
 * - Focus/dismiss: focus, dismiss, close
 *
 * Automatically handles disambiguation when multiple elements match.
 */
class UIHandler(
    private val executor: UIExecutor,
    private val disambiguator: ElementDisambiguator = ElementDisambiguator.default
) : BaseHandler() {

    override val category: ActionCategory = ActionCategory.UI

    override val supportedActions: List<String> = listOf(
        "click", "tap", "press",
        "long click", "long press",
        "double tap", "double click",
        "expand", "collapse",
        "check", "uncheck", "toggle",
        "focus", "dismiss", "close"
    )

    /**
     * Current disambiguation state.
     * When non-null, the system is waiting for user to say a number.
     */
    private var activeDisambiguation: ActiveDisambiguation? = null

    /**
     * Callback invoked when disambiguation overlay should be shown.
     * Platform implementations should show numbered badges on matching elements.
     */
    var onShowDisambiguation: ((DisambiguationResult) -> Unit)? = null

    /**
     * Callback invoked when disambiguation is complete or cancelled.
     * Platform implementations should hide numbered badges.
     */
    var onHideDisambiguation: (() -> Unit)? = null

    override suspend fun execute(
        command: QuantizedCommand,
        params: Map<String, Any>
    ): HandlerResult {
        val normalizedAction = command.phrase.lowercase().trim()

        return when {
            // Click/Tap actions with target
            normalizedAction.startsWith("click ") ||
            normalizedAction.startsWith("tap ") ||
            normalizedAction.startsWith("press ") -> {
                val target = normalizedAction
                    .removePrefix("click ")
                    .removePrefix("tap ")
                    .removePrefix("press ")
                    .trim()

                // Check if target is a VUID (direct reference, no disambiguation needed)
                val vuid = command.targetVuid ?: extractVuid(target)
                if (vuid != null) {
                    if (executor.clickByVuid(vuid)) {
                        HandlerResult.success("Clicked element")
                    } else {
                        HandlerResult.failure("Could not click element with VUID: $vuid")
                    }
                } else {
                    // Find matching elements for disambiguation
                    handleClickWithDisambiguation(target, UIAction.CLICK)
                }
            }

            // Long click
            normalizedAction.startsWith("long click ") ||
            normalizedAction.startsWith("long press ") -> {
                val target = normalizedAction
                    .removePrefix("long click ")
                    .removePrefix("long press ")
                    .trim()

                val vuid = command.targetVuid ?: extractVuid(target)
                if (vuid != null) {
                    if (executor.longClickByVuid(vuid)) {
                        HandlerResult.success("Long clicked element")
                    } else {
                        HandlerResult.failure("Could not long click element with VUID: $vuid")
                    }
                } else if (executor.longClickByText(target)) {
                    HandlerResult.success("Long clicked $target")
                } else {
                    HandlerResult.failure("Could not find element: $target")
                }
            }

            // Double tap
            normalizedAction.startsWith("double tap ") ||
            normalizedAction.startsWith("double click ") -> {
                val target = normalizedAction
                    .removePrefix("double tap ")
                    .removePrefix("double click ")
                    .trim()

                if (executor.doubleClickByText(target)) {
                    HandlerResult.success("Double clicked $target")
                } else {
                    HandlerResult.failure("Could not find element: $target")
                }
            }

            // Expand/Collapse
            normalizedAction.startsWith("expand ") -> {
                val target = normalizedAction.removePrefix("expand ").trim()
                if (executor.expand(target)) {
                    HandlerResult.success("Expanded $target")
                } else {
                    HandlerResult.failure("Could not expand: $target")
                }
            }

            normalizedAction.startsWith("collapse ") -> {
                val target = normalizedAction.removePrefix("collapse ").trim()
                if (executor.collapse(target)) {
                    HandlerResult.success("Collapsed $target")
                } else {
                    HandlerResult.failure("Could not collapse: $target")
                }
            }

            // Check/Uncheck/Toggle
            normalizedAction.startsWith("check ") -> {
                val target = normalizedAction.removePrefix("check ").trim()
                if (executor.setChecked(target, true)) {
                    HandlerResult.success("Checked $target")
                } else {
                    HandlerResult.failure("Could not check: $target")
                }
            }

            normalizedAction.startsWith("uncheck ") -> {
                val target = normalizedAction.removePrefix("uncheck ").trim()
                if (executor.setChecked(target, false)) {
                    HandlerResult.success("Unchecked $target")
                } else {
                    HandlerResult.failure("Could not uncheck: $target")
                }
            }

            normalizedAction.startsWith("toggle ") -> {
                val target = normalizedAction.removePrefix("toggle ").trim()
                if (executor.toggle(target)) {
                    HandlerResult.success("Toggled $target")
                } else {
                    HandlerResult.failure("Could not toggle: $target")
                }
            }

            // Focus
            normalizedAction.startsWith("focus ") -> {
                val target = normalizedAction.removePrefix("focus ").trim()
                if (executor.focus(target)) {
                    HandlerResult.success("Focused $target")
                } else {
                    HandlerResult.failure("Could not focus: $target")
                }
            }

            // Dismiss/Close
            normalizedAction == "dismiss" || normalizedAction == "close" -> {
                if (executor.dismiss()) {
                    HandlerResult.success("Dismissed")
                } else {
                    HandlerResult.failure("Could not dismiss")
                }
            }

            else -> HandlerResult.notHandled()
        }
    }

    /**
     * Extract VUID from target string.
     * VUIDs start with "vuid:" or are 8-character hex strings.
     */
    private fun extractVuid(target: String): String? {
        return when {
            target.startsWith("vuid:") -> target.removePrefix("vuid:")
            target.matches(Regex("^[a-f0-9]{8}$")) -> target
            else -> null
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Disambiguation Support
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Handle click action with disambiguation for duplicate elements.
     *
     * Flow:
     * 1. Get all screen elements from executor
     * 2. Find elements matching the target text
     * 3. If single match: execute immediately
     * 4. If multiple matches: show numbered badges ONLY on matches, wait for selection
     * 5. If no matches: return failure
     */
    private suspend fun handleClickWithDisambiguation(
        target: String,
        action: UIAction
    ): HandlerResult {
        // Get current screen elements
        val screenElements = executor.getScreenElements()

        // Find matches using disambiguator
        val result = disambiguator.findMatches(
            query = target,
            elements = screenElements,
            matchMode = ElementDisambiguator.MatchMode.CONTAINS
        )

        return when {
            // No matches found
            result.noMatches -> {
                HandlerResult.failure("Could not find element: $target")
            }

            // Single match - execute directly
            result.singleMatch != null -> {
                executeActionOnElement(result.singleMatch!!, action)
            }

            // Multiple matches - need disambiguation
            result.needsDisambiguation -> {
                // Store active disambiguation state
                activeDisambiguation = ActiveDisambiguation(
                    result = result,
                    pendingAction = action
                )

                // Notify platform to show numbered badges ONLY on matching elements
                onShowDisambiguation?.invoke(result)

                // Return awaiting selection result
                HandlerResult.awaitingSelection(
                    message = "${result.matchCount} '${target}' elements found. Say a number to select.",
                    matchCount = result.matchCount,
                    accessibilityAnnouncement = result.getAccessibilityAnnouncement()
                )
            }

            else -> HandlerResult.failure("Unexpected disambiguation state")
        }
    }

    /**
     * Handle number selection during disambiguation.
     *
     * Call this when user says "one", "two", "three", etc.
     *
     * @param number The number spoken (1-based)
     * @return Result of the action, or failure if no disambiguation active
     */
    suspend fun handleNumberSelection(number: Int): HandlerResult {
        val disambiguation = activeDisambiguation
            ?: return HandlerResult.failure("No disambiguation active")

        val selectedElement = disambiguator.selectByNumber(disambiguation.result, number)
            ?: return HandlerResult.failure("Invalid selection: $number")

        // Clear disambiguation state
        clearDisambiguation()

        // Execute the pending action
        return executeActionOnElement(selectedElement, disambiguation.pendingAction)
    }

    /**
     * Cancel active disambiguation.
     *
     * Call this when user says "cancel" or times out.
     */
    fun cancelDisambiguation() {
        clearDisambiguation()
    }

    /**
     * Check if disambiguation is currently active.
     */
    fun isDisambiguationActive(): Boolean = activeDisambiguation != null

    /**
     * Get current disambiguation matches (for overlay display).
     */
    fun getActiveDisambiguationMatches(): List<NumberedMatch>? =
        activeDisambiguation?.result?.numberedItems

    private fun clearDisambiguation() {
        activeDisambiguation = null
        onHideDisambiguation?.invoke()
    }

    private suspend fun executeActionOnElement(element: ElementInfo, action: UIAction): HandlerResult {
        val success = when (action) {
            UIAction.CLICK -> executor.clickElement(element)
            UIAction.LONG_CLICK -> executor.longClickElement(element)
            UIAction.DOUBLE_CLICK -> executor.doubleClickElement(element)
        }

        return if (success) {
            HandlerResult.success("${action.displayName} ${element.voiceLabel}")
        } else {
            HandlerResult.failure("Could not ${action.displayName.lowercase()} ${element.voiceLabel}")
        }
    }
}

/**
 * UI action types for disambiguation.
 */
enum class UIAction(val displayName: String) {
    CLICK("Clicked"),
    LONG_CLICK("Long clicked"),
    DOUBLE_CLICK("Double clicked")
}

/**
 * Active disambiguation state.
 */
private data class ActiveDisambiguation(
    val result: DisambiguationResult,
    val pendingAction: UIAction
)

/**
 * Platform-specific executor for UI actions.
 */
interface UIExecutor {
    // ═══════════════════════════════════════════════════════════════════════════
    // Element Discovery (for disambiguation)
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Get all interactive elements currently on screen.
     * Used for disambiguation when multiple elements match a voice command.
     */
    suspend fun getScreenElements(): List<ElementInfo>

    // ═══════════════════════════════════════════════════════════════════════════
    // Direct Element Actions (used after disambiguation)
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Click a specific element.
     */
    suspend fun clickElement(element: ElementInfo): Boolean

    /**
     * Long click a specific element.
     */
    suspend fun longClickElement(element: ElementInfo): Boolean

    /**
     * Double click a specific element.
     */
    suspend fun doubleClickElement(element: ElementInfo): Boolean

    // ═══════════════════════════════════════════════════════════════════════════
    // Legacy Text/VUID Actions (kept for compatibility)
    // ═══════════════════════════════════════════════════════════════════════════

    suspend fun clickByText(text: String): Boolean
    suspend fun clickByVuid(vuid: String): Boolean
    suspend fun longClickByText(text: String): Boolean
    suspend fun longClickByVuid(vuid: String): Boolean
    suspend fun doubleClickByText(text: String): Boolean
    suspend fun expand(target: String): Boolean
    suspend fun collapse(target: String): Boolean
    suspend fun setChecked(target: String, checked: Boolean): Boolean
    suspend fun toggle(target: String): Boolean
    suspend fun focus(target: String): Boolean
    suspend fun dismiss(): Boolean
}
