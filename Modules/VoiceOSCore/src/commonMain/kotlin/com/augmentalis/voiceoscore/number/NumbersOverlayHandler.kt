/**
 * NumbersOverlayHandler.kt - Handles numbers overlay control commands
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2026-01-27
 *
 * KMP handler for numbers overlay visibility control.
 * Supports "numbers on", "numbers off", "numbers auto" commands.
 *
 * IMPORTANT: Number assignments are PERSISTENT and tied to element AVIDs.
 * - When elements scroll off-screen, their numbers are remembered
 * - When scrolling reveals new elements, they get the next available number
 * - When scrolling back, elements retain their original numbers
 * - This ensures consistent voice targeting (e.g., "tap 3" always taps the same item)
 */
package com.augmentalis.voiceoscore

/**
 * Handler for numbers overlay control commands.
 *
 * Controls the visibility mode of numbered badges displayed on screen elements.
 * These badges allow users to tap elements by saying a number (e.g., "tap 3").
 *
 * ## Number Persistence for Scrolling
 *
 * Numbers are tied to element AVIDs (Augmentalis Voice IDs), NOT screen positions:
 *
 * ```
 * Initial screen:          After scroll down:       After scroll back up:
 * ┌─────────────────┐      ┌─────────────────┐      ┌─────────────────┐
 * │ [1] Email A     │      │ [4] Email D     │      │ [1] Email A     │  <- Same number!
 * │ [2] Email B     │      │ [5] Email E     │      │ [2] Email B     │
 * │ [3] Email C     │      │ [6] Email F     │      │ [3] Email C     │
 * └─────────────────┘      └─────────────────┘      └─────────────────┘
 * ```
 *
 * This ensures:
 * - User says "tap 1" and it ALWAYS taps "Email A" (if visible)
 * - New items get incrementing numbers (4, 5, 6...)
 * - Scrolling back shows original numbers (1, 2, 3)
 *
 * ## Visibility Modes
 *
 * - ON: Always show numbered badges on all interactive elements
 * - OFF: Never show numbered badges
 * - AUTO: Show numbers only for lists (emails, messages, etc.)
 *
 * ## Supported Commands
 *
 * - "numbers on", "show numbers", "numbers always"
 * - "numbers off", "hide numbers", "no numbers"
 * - "numbers auto", "numbers automatic", "auto numbers"
 *
 * @param executor Platform-specific executor for overlay control
 */
class NumbersOverlayHandler(
    private val executor: NumbersOverlayExecutor
) : BaseHandler() {

    override val category: ActionCategory = ActionCategory.ACCESSIBILITY

    override val supportedActions: List<String> = listOf(
        // Numbers ON commands
        "numbers on", "show numbers", "numbers always",
        // Numbers OFF commands
        "numbers off", "hide numbers", "no numbers",
        // Numbers AUTO commands
        "numbers auto", "numbers automatic", "auto numbers"
    )

    override suspend fun execute(
        command: QuantizedCommand,
        params: Map<String, Any>
    ): HandlerResult {
        val normalizedAction = command.phrase.lowercase().trim()

        return when (normalizedAction) {
            // Numbers ON commands
            "numbers on", "show numbers", "numbers always" -> {
                if (executor.setNumbersMode(NumbersOverlayMode.ON)) {
                    HandlerResult.Success(
                        message = "Numbers always on",
                        data = mapOf("accessibility_announcement" to "Numbers overlay enabled. Numbers will always be shown.")
                    )
                } else {
                    HandlerResult.failure("Could not enable numbers overlay")
                }
            }

            // Numbers OFF commands
            "numbers off", "hide numbers", "no numbers" -> {
                if (executor.setNumbersMode(NumbersOverlayMode.OFF)) {
                    // Clear number assignments when turning off
                    executor.clearNumberAssignments()
                    HandlerResult.Success(
                        message = "Numbers off",
                        data = mapOf("accessibility_announcement" to "Numbers overlay disabled. Numbers will be hidden.")
                    )
                } else {
                    HandlerResult.failure("Could not disable numbers overlay")
                }
            }

            // Numbers AUTO commands
            "numbers auto", "numbers automatic", "auto numbers" -> {
                if (executor.setNumbersMode(NumbersOverlayMode.AUTO)) {
                    HandlerResult.Success(
                        message = "Numbers auto mode",
                        data = mapOf("accessibility_announcement" to "Numbers overlay set to automatic. Numbers shown for lists only.")
                    )
                } else {
                    HandlerResult.failure("Could not set numbers to auto mode")
                }
            }

            else -> HandlerResult.notHandled()
        }
    }
}

/**
 * Numbers overlay visibility mode.
 */
enum class NumbersOverlayMode {
    /** Always show numbered badges on interactive elements */
    ON,
    /** Never show numbered badges */
    OFF,
    /** Show numbers only for lists (auto-detect) */
    AUTO
}

/**
 * Represents a persistent number assignment for an element.
 *
 * @property number The assigned number (1-based, monotonically increasing)
 * @property avid The element's Augmentalis Voice ID (stable across scroll)
 * @property scrollContainerAvid The parent scrollable container's AVID (for scoped numbering)
 */
data class NumberAssignment(
    val number: Int,
    val avid: String,
    val scrollContainerAvid: String? = null
)

/**
 * Platform-specific executor for numbers overlay control.
 *
 * Implementations should:
 * 1. Control the accessibility service's number overlay visibility
 * 2. Maintain persistent AVID -> number mappings per scroll container
 * 3. Track the next available number per scroll container
 * 4. Re-display original numbers when elements scroll back into view
 *
 * ## Number Assignment Algorithm
 *
 * For each scroll container (identified by scrollContainerAvid):
 * 1. Maintain a Map<AVID, Int> of assigned numbers
 * 2. Track nextNumber (starts at 1, only increments)
 * 3. On screen update:
 *    - For each visible element:
 *      - If AVID exists in map: use existing number
 *      - Else: assign nextNumber++, store in map
 * 4. Numbers are NEVER reassigned or reused within a session
 * 5. Clear assignments only on: app change, numbers off, or explicit clear
 *
 * ## Example Implementation
 *
 * ```kotlin
 * class AndroidNumbersOverlayExecutor : NumbersOverlayExecutor {
 *     // Per-container number assignments
 *     private val assignments = mutableMapOf<String, MutableMap<String, Int>>()
 *     private val nextNumbers = mutableMapOf<String, Int>()
 *
 *     override fun getOrAssignNumber(avid: String, scrollContainerAvid: String?): Int {
 *         val containerId = scrollContainerAvid ?: "root"
 *         val containerMap = assignments.getOrPut(containerId) { mutableMapOf() }
 *
 *         return containerMap.getOrPut(avid) {
 *             val next = nextNumbers.getOrPut(containerId) { 1 }
 *             nextNumbers[containerId] = next + 1
 *             next
 *         }
 *     }
 * }
 * ```
 */
interface NumbersOverlayExecutor {
    /**
     * Set the numbers overlay visibility mode.
     *
     * @param mode The visibility mode to set
     * @return true if mode was set successfully
     */
    suspend fun setNumbersMode(mode: NumbersOverlayMode): Boolean

    /**
     * Get the current numbers overlay mode.
     *
     * @return The current visibility mode
     */
    suspend fun getCurrentMode(): NumbersOverlayMode

    /**
     * Get or assign a number for an element.
     *
     * If the element (by AVID) already has an assigned number, returns it.
     * Otherwise, assigns the next available number and returns it.
     *
     * Numbers are scoped by scroll container - each scrollable list has
     * its own number sequence starting from 1.
     *
     * @param avid The element's Augmentalis Voice ID
     * @param scrollContainerAvid The parent scroll container's AVID (null for root/non-scrolling)
     * @return The assigned number for this element
     */
    suspend fun getOrAssignNumber(avid: String, scrollContainerAvid: String? = null): Int

    /**
     * Get the existing number assignment for an element, if any.
     *
     * @param avid The element's AVID
     * @param scrollContainerAvid The parent scroll container's AVID
     * @return The assigned number, or null if not yet assigned
     */
    suspend fun getAssignedNumber(avid: String, scrollContainerAvid: String? = null): Int?

    /**
     * Get all number assignments for a scroll container.
     *
     * @param scrollContainerAvid The container's AVID (null for root)
     * @return Map of AVID to assigned number
     */
    suspend fun getAssignmentsForContainer(scrollContainerAvid: String? = null): Map<String, Int>

    /**
     * Clear all number assignments.
     *
     * Called when:
     * - User says "numbers off"
     * - App changes (new activity/screen)
     * - Session ends
     *
     * After clearing, numbers will start fresh from 1 on next assignment.
     */
    suspend fun clearNumberAssignments()

    /**
     * Clear number assignments for a specific scroll container.
     *
     * @param scrollContainerAvid The container's AVID to clear
     */
    suspend fun clearContainerAssignments(scrollContainerAvid: String)

    /**
     * Notify that a screen transition occurred.
     *
     * This should clear assignments if the app/activity changed,
     * but preserve them for minor UI updates within the same screen.
     *
     * @param newScreenId Identifier for the new screen (e.g., activity name)
     * @param isMajorTransition true if app/activity changed, false for minor updates
     */
    suspend fun onScreenTransition(newScreenId: String, isMajorTransition: Boolean)
}
