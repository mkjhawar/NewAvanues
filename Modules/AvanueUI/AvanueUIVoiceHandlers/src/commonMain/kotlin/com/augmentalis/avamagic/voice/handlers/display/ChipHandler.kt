/**
 * ChipHandler.kt - Voice handler for Chip/Tag selection interactions
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Created: 2026-01-28
 *
 * Purpose: Voice-driven chip/tag control for selection and filtering interfaces
 * Features:
 * - Select chips by name or index
 * - Deselect individual chips
 * - Clear all selected chips
 * - Multi-select and single-select mode support
 * - Named chip targeting (e.g., "select urgent")
 * - Index-based targeting (e.g., "chip 3")
 * - AVID-based targeting for precise element selection
 * - Voice feedback for selection changes
 *
 * Location: CommandManager module handlers
 *
 * ## Supported Commands
 *
 * Select chip:
 * - "select [chip name]" - Select chip by name
 * - "choose [chip]" - Select chip by name
 * - "chip [N]" - Select Nth chip (1-indexed)
 * - "pick [chip name]" - Select chip by name
 * - "tap [chip name]" - Toggle chip selection
 *
 * Deselect chip:
 * - "deselect [chip name]" - Deselect chip by name
 * - "remove [chip]" - Deselect chip by name
 * - "unselect [chip]" - Deselect chip by name
 * - "deselect chip [N]" - Deselect Nth chip
 *
 * Clear selection:
 * - "clear chips" - Clear all selected chips
 * - "clear selection" - Clear all selected chips
 * - "deselect all" - Deselect all chips
 * - "reset chips" - Reset chip selection
 *
 * ## Index Parsing
 *
 * Supports:
 * - Integer values: "1", "3", "10"
 * - Ordinal words: "first", "second", "third"
 * - Cardinal words: "one", "two", "three"
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
 * Voice command handler for Chip/Tag selection interactions.
 *
 * Provides comprehensive voice control for chip components including:
 * - Selecting chips by name or index
 * - Deselecting individual chips
 * - Clearing all chip selections
 * - Support for both single and multi-select modes
 *
 * Thread Safety:
 * - All operations are suspend functions
 * - State modifications are atomic via executor
 *
 * @param executor Platform-specific executor for chip operations
 */
class ChipHandler(
    private val executor: ChipExecutor
) : BaseHandler() {

    companion object {
        private const val TAG = "ChipHandler"
        private val Log = LoggerFactory.getLogger(TAG)

        // Patterns for parsing commands
        private val SELECT_CHIP_PATTERN = Regex(
            """^(?:select|choose|pick|tap)\s+(.+)$""",
            RegexOption.IGNORE_CASE
        )

        private val DESELECT_CHIP_PATTERN = Regex(
            """^(?:deselect|remove|unselect)\s+(.+)$""",
            RegexOption.IGNORE_CASE
        )

        private val CHIP_INDEX_PATTERN = Regex(
            """^chip\s+(\d+|first|second|third|fourth|fifth|sixth|seventh|eighth|ninth|tenth|one|two|three|four|five|six|seven|eight|nine|ten)$""",
            RegexOption.IGNORE_CASE
        )

        private val DESELECT_CHIP_INDEX_PATTERN = Regex(
            """^deselect\s+chip\s+(\d+|first|second|third|fourth|fifth|sixth|seventh|eighth|ninth|tenth)$""",
            RegexOption.IGNORE_CASE
        )

        // Ordinal word to number mapping
        private val ORDINAL_NUMBERS = mapOf(
            "first" to 1, "second" to 2, "third" to 3, "fourth" to 4, "fifth" to 5,
            "sixth" to 6, "seventh" to 7, "eighth" to 8, "ninth" to 9, "tenth" to 10
        )

        // Cardinal word to number mapping
        private val CARDINAL_NUMBERS = mapOf(
            "one" to 1, "two" to 2, "three" to 3, "four" to 4, "five" to 5,
            "six" to 6, "seven" to 7, "eight" to 8, "nine" to 9, "ten" to 10
        )
    }

    override val category: ActionCategory = ActionCategory.UI

    override val supportedActions: List<String> = listOf(
        // Select chip
        "select", "choose", "pick", "tap",
        "chip", "select chip",
        // Deselect chip
        "deselect", "remove", "unselect",
        "deselect chip",
        // Clear selection
        "clear chips", "clear selection",
        "deselect all", "reset chips"
    )

    /**
     * Callback for voice feedback when chip selection changes.
     */
    var onSelectionChanged: ((chipName: String, selected: Boolean, totalSelected: Int) -> Unit)? = null

    override suspend fun execute(
        command: QuantizedCommand,
        params: Map<String, Any>
    ): HandlerResult {
        val normalizedAction = command.phrase.lowercase().trim()

        Log.d { "Processing chip command: $normalizedAction" }

        return try {
            when {
                // Chip by index: "chip [N]"
                CHIP_INDEX_PATTERN.containsMatchIn(normalizedAction) -> {
                    handleChipByIndex(normalizedAction, command)
                }

                // Deselect chip by index: "deselect chip [N]"
                DESELECT_CHIP_INDEX_PATTERN.containsMatchIn(normalizedAction) -> {
                    handleDeselectChipByIndex(normalizedAction, command)
                }

                // Clear all chips
                normalizedAction in listOf(
                    "clear chips", "clear selection",
                    "deselect all", "reset chips", "clear all chips"
                ) -> {
                    handleClearChips(command)
                }

                // Deselect chip by name
                DESELECT_CHIP_PATTERN.containsMatchIn(normalizedAction) -> {
                    handleDeselectChip(normalizedAction, command)
                }

                // Select chip by name
                SELECT_CHIP_PATTERN.containsMatchIn(normalizedAction) -> {
                    handleSelectChip(normalizedAction, command)
                }

                else -> HandlerResult.notHandled()
            }
        } catch (e: Exception) {
            Log.e({ "Error executing chip command" }, e)
            HandlerResult.failure("Error: ${e.message}", recoverable = true)
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Command Handlers
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Handle "select [chip name]" command.
     */
    private suspend fun handleSelectChip(
        normalizedAction: String,
        command: QuantizedCommand
    ): HandlerResult {
        val matchResult = SELECT_CHIP_PATTERN.find(normalizedAction)
            ?: return HandlerResult.failure("Could not parse select command")

        val chipName = matchResult.groupValues[1].trim()

        // Check if it's actually an index
        val index = parseIndex(chipName)
        if (index != null) {
            return selectChipByIndex(index, command)
        }

        // Find chip by name
        val chipInfo = findChip(name = chipName, avid = command.targetAvid)
            ?: return HandlerResult.Failure(
                reason = "Chip '$chipName' not found",
                recoverable = true,
                suggestedAction = "Available chips: ${getAvailableChipNames()}"
            )

        return applySelection(chipInfo, selected = true)
    }

    /**
     * Handle "deselect [chip name]" command.
     */
    private suspend fun handleDeselectChip(
        normalizedAction: String,
        command: QuantizedCommand
    ): HandlerResult {
        val matchResult = DESELECT_CHIP_PATTERN.find(normalizedAction)
            ?: return HandlerResult.failure("Could not parse deselect command")

        val chipName = matchResult.groupValues[1].trim()

        // Check if it's actually an index
        val index = parseIndex(chipName)
        if (index != null) {
            return deselectChipByIndex(index, command)
        }

        // Find chip by name
        val chipInfo = findChip(name = chipName, avid = command.targetAvid)
            ?: return HandlerResult.Failure(
                reason = "Chip '$chipName' not found",
                recoverable = true,
                suggestedAction = "Available chips: ${getAvailableChipNames()}"
            )

        return applySelection(chipInfo, selected = false)
    }

    /**
     * Handle "chip [N]" command.
     */
    private suspend fun handleChipByIndex(
        normalizedAction: String,
        command: QuantizedCommand
    ): HandlerResult {
        val matchResult = CHIP_INDEX_PATTERN.find(normalizedAction)
            ?: return HandlerResult.failure("Could not parse chip index")

        val indexString = matchResult.groupValues[1]
        val index = parseIndex(indexString)
            ?: return HandlerResult.Failure(
                reason = "Could not parse index: '$indexString'",
                recoverable = true,
                suggestedAction = "Try 'chip 1', 'chip 2', or 'chip first'"
            )

        return selectChipByIndex(index, command)
    }

    /**
     * Handle "deselect chip [N]" command.
     */
    private suspend fun handleDeselectChipByIndex(
        normalizedAction: String,
        command: QuantizedCommand
    ): HandlerResult {
        val matchResult = DESELECT_CHIP_INDEX_PATTERN.find(normalizedAction)
            ?: return HandlerResult.failure("Could not parse chip index")

        val indexString = matchResult.groupValues[1]
        val index = parseIndex(indexString)
            ?: return HandlerResult.Failure(
                reason = "Could not parse index: '$indexString'",
                recoverable = true,
                suggestedAction = "Try 'deselect chip 1' or 'deselect chip first'"
            )

        return deselectChipByIndex(index, command)
    }

    /**
     * Handle "clear chips" / "clear selection" command.
     */
    private suspend fun handleClearChips(command: QuantizedCommand): HandlerResult {
        val result = executor.clearSelection()

        return if (result.success) {
            val clearedCount = result.previouslySelectedCount

            Log.i { "Cleared $clearedCount chip selections" }

            HandlerResult.Success(
                message = if (clearedCount > 0) "Cleared $clearedCount chip${if (clearedCount > 1) "s" else ""}" else "No chips selected",
                data = mapOf(
                    "clearedCount" to clearedCount,
                    "accessibility_announcement" to "Chip selection cleared"
                )
            )
        } else {
            HandlerResult.failure(
                reason = result.error ?: "Could not clear chip selection",
                recoverable = true
            )
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Helper Methods
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Select chip by index (1-indexed).
     */
    private suspend fun selectChipByIndex(index: Int, command: QuantizedCommand): HandlerResult {
        val allChips = executor.getAllChips()

        if (index < 1 || index > allChips.size) {
            return HandlerResult.Failure(
                reason = "Chip $index not found. There are ${allChips.size} chips.",
                recoverable = true,
                suggestedAction = "Try a number between 1 and ${allChips.size}"
            )
        }

        val chipInfo = allChips[index - 1]
        return applySelection(chipInfo, selected = true)
    }

    /**
     * Deselect chip by index (1-indexed).
     */
    private suspend fun deselectChipByIndex(index: Int, command: QuantizedCommand): HandlerResult {
        val allChips = executor.getAllChips()

        if (index < 1 || index > allChips.size) {
            return HandlerResult.Failure(
                reason = "Chip $index not found. There are ${allChips.size} chips.",
                recoverable = true,
                suggestedAction = "Try a number between 1 and ${allChips.size}"
            )
        }

        val chipInfo = allChips[index - 1]
        return applySelection(chipInfo, selected = false)
    }

    /**
     * Find chip by name, AVID, or focus state.
     */
    private suspend fun findChip(
        name: String? = null,
        avid: String? = null
    ): ChipInfo? {
        // Priority 1: AVID lookup (fastest, most precise)
        if (avid != null) {
            val chip = executor.findChipByAvid(avid)
            if (chip != null) return chip
        }

        // Priority 2: Name lookup
        if (name != null) {
            val chip = executor.findChipByName(name)
            if (chip != null) return chip
        }

        // Priority 3: Focused chip
        return executor.findFocusedChip()
    }

    /**
     * Get available chip names for suggestions.
     */
    private suspend fun getAvailableChipNames(): String {
        val chips = executor.getAllChips()
        return if (chips.isEmpty()) {
            "none available"
        } else {
            chips.take(5).joinToString(", ") { it.name }
        }
    }

    /**
     * Apply selection state to a chip and return result.
     */
    private suspend fun applySelection(
        chipInfo: ChipInfo,
        selected: Boolean
    ): HandlerResult {
        // Check if already in desired state
        if (chipInfo.isSelected == selected) {
            val stateWord = if (selected) "selected" else "not selected"
            return HandlerResult.Success(
                message = "'${chipInfo.name}' is already $stateWord",
                data = mapOf(
                    "chipName" to chipInfo.name,
                    "chipAvid" to chipInfo.avid,
                    "selected" to selected,
                    "noChange" to true
                )
            )
        }

        val result = executor.setSelection(chipInfo, selected)

        return if (result.success) {
            // Invoke callback for voice feedback
            onSelectionChanged?.invoke(
                chipInfo.name,
                selected,
                result.totalSelected
            )

            // Build feedback message
            val action = if (selected) "Selected" else "Deselected"
            val feedback = "$action '${chipInfo.name}'"

            Log.i { "Chip selection: ${chipInfo.name} = $selected" }

            HandlerResult.Success(
                message = feedback,
                data = mapOf(
                    "chipName" to chipInfo.name,
                    "chipAvid" to chipInfo.avid,
                    "previouslySelected" to chipInfo.isSelected,
                    "selected" to selected,
                    "totalSelected" to result.totalSelected,
                    "accessibility_announcement" to feedback
                )
            )
        } else {
            HandlerResult.failure(
                reason = result.error ?: "Could not ${if (selected) "select" else "deselect"} chip",
                recoverable = true
            )
        }
    }

    /**
     * Parse an index string into an integer (1-indexed).
     *
     * Supports:
     * - "1" -> 1
     * - "first" -> 1
     * - "one" -> 1
     */
    private fun parseIndex(input: String): Int? {
        val trimmed = input.trim().lowercase()

        // Try direct numeric parsing
        trimmed.toIntOrNull()?.let { return it }

        // Try ordinal parsing
        ORDINAL_NUMBERS[trimmed]?.let { return it }

        // Try cardinal parsing
        CARDINAL_NUMBERS[trimmed]?.let { return it }

        return null
    }

}

// ═══════════════════════════════════════════════════════════════════════════════
// Supporting Types
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * Information about a chip component.
 *
 * @property avid AVID fingerprint for the chip (format: CHP:{hash8})
 * @property name Display text of the chip
 * @property isSelected Whether the chip is currently selected
 * @property isEnabled Whether the chip is enabled and interactive
 * @property index Position in the chip group (0-indexed)
 * @property groupAvid AVID of the parent chip group
 * @property chipType Type of chip (filter, action, input, suggestion)
 * @property bounds Screen bounds for the chip
 * @property isFocused Whether this chip currently has focus
 * @property node Platform-specific node reference
 */
data class ChipInfo(
    val avid: String,
    val name: String = "",
    val isSelected: Boolean = false,
    val isEnabled: Boolean = true,
    val index: Int = 0,
    val groupAvid: String? = null,
    val chipType: ChipType = ChipType.FILTER,
    val bounds: Bounds = Bounds.EMPTY,
    val isFocused: Boolean = false,
    val node: Any? = null
) {
    /**
     * Convert to ElementInfo for general element operations.
     */
    fun toElementInfo(): ElementInfo = ElementInfo(
        className = "Chip",
        text = name,
        bounds = bounds,
        isClickable = true,
        isEnabled = isEnabled,
        avid = avid,
        stateDescription = if (isSelected) "Selected" else "Not selected"
    )
}

/**
 * Types of chip components.
 */
enum class ChipType {
    /** Filter chip for filtering content */
    FILTER,
    /** Action chip for triggering actions */
    ACTION,
    /** Input chip for user input (e.g., tags) */
    INPUT,
    /** Suggestion chip for recommendations */
    SUGGESTION
}

/**
 * Result of a chip selection operation.
 */
data class ChipOperationResult(
    val success: Boolean,
    val error: String? = null,
    val previouslySelected: Boolean = false,
    val nowSelected: Boolean = false,
    val totalSelected: Int = 0
) {
    companion object {
        fun success(
            previouslySelected: Boolean,
            nowSelected: Boolean,
            totalSelected: Int
        ) = ChipOperationResult(
            success = true,
            previouslySelected = previouslySelected,
            nowSelected = nowSelected,
            totalSelected = totalSelected
        )

        fun error(message: String) = ChipOperationResult(
            success = false,
            error = message
        )
    }
}

/**
 * Result of clearing all chip selections.
 */
data class ChipClearResult(
    val success: Boolean,
    val error: String? = null,
    val previouslySelectedCount: Int = 0
) {
    companion object {
        fun success(previouslySelectedCount: Int) = ChipClearResult(
            success = true,
            previouslySelectedCount = previouslySelectedCount
        )

        fun error(message: String) = ChipClearResult(
            success = false,
            error = message
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// Platform Executor Interface
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * Platform-specific executor for chip operations.
 *
 * Implementations should:
 * 1. Find chip components by AVID, name, or focus state
 * 2. Read current selection states
 * 3. Set selection states via accessibility actions
 * 4. Handle various chip implementations (Material, custom)
 *
 * ## Chip Detection Algorithm
 *
 * ```kotlin
 * fun findChipNode(rootNode: AccessibilityNodeInfo): AccessibilityNodeInfo? {
 *     // Look for nodes with className:
 *     // - com.google.android.material.chip.Chip
 *     // - com.google.android.material.chip.ChipGroup
 *     // - Custom chip implementations with checkable state
 * }
 * ```
 *
 * ## Selection Algorithm
 *
 * ```kotlin
 * fun setSelection(node: AccessibilityNodeInfo, selected: Boolean): Boolean {
 *     // Check if chip is checkable
 *     // Use ACTION_CLICK to toggle
 *     // Or ACTION_SELECT/ACTION_CLEAR_SELECTION if supported
 * }
 * ```
 */
interface ChipExecutor {

    // ═══════════════════════════════════════════════════════════════════════════
    // Chip Discovery
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Find a chip by its AVID fingerprint.
     *
     * @param avid The AVID fingerprint (format: CHP:{hash8})
     * @return ChipInfo if found, null otherwise
     */
    suspend fun findChipByAvid(avid: String): ChipInfo?

    /**
     * Find a chip by its text label.
     *
     * @param name The chip text to search for (case-insensitive)
     * @return ChipInfo if found, null otherwise
     */
    suspend fun findChipByName(name: String): ChipInfo?

    /**
     * Find the currently focused chip.
     *
     * @return ChipInfo if a chip has focus, null otherwise
     */
    suspend fun findFocusedChip(): ChipInfo?

    /**
     * Get all chips on the current screen.
     *
     * @return List of all visible chip components
     */
    suspend fun getAllChips(): List<ChipInfo>

    /**
     * Get all currently selected chips.
     *
     * @return List of selected chip components
     */
    suspend fun getSelectedChips(): List<ChipInfo>

    // ═══════════════════════════════════════════════════════════════════════════
    // Selection Operations
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Set the selection state of a chip.
     *
     * @param chip The chip to modify
     * @param selected Whether to select or deselect
     * @return Operation result with selection state changes
     */
    suspend fun setSelection(chip: ChipInfo, selected: Boolean): ChipOperationResult

    /**
     * Toggle the selection state of a chip.
     *
     * @param chip The chip to toggle
     * @return Operation result with selection state changes
     */
    suspend fun toggleSelection(chip: ChipInfo): ChipOperationResult

    /**
     * Clear all chip selections.
     *
     * @return Result indicating how many chips were deselected
     */
    suspend fun clearSelection(): ChipClearResult

    // ═══════════════════════════════════════════════════════════════════════════
    // Convenience Operations
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Select a chip by its index (0-indexed).
     *
     * @param index The chip index
     * @return Operation result
     */
    suspend fun selectByIndex(index: Int): ChipOperationResult

    /**
     * Deselect a chip by its index (0-indexed).
     *
     * @param index The chip index
     * @return Operation result
     */
    suspend fun deselectByIndex(index: Int): ChipOperationResult

    /**
     * Check if any chips are selected.
     *
     * @return true if at least one chip is selected
     */
    suspend fun hasSelection(): Boolean

    /**
     * Get the count of selected chips.
     *
     * @return Number of currently selected chips
     */
    suspend fun getSelectionCount(): Int
}
