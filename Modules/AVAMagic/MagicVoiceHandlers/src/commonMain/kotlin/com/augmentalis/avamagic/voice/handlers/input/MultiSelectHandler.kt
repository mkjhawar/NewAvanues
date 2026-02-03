/**
 * MultiSelectHandler.kt - Handles multi-select component voice interactions
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2026-01-27
 *
 * KMP handler for multi-select component voice interactions.
 * Supports adding, removing, toggling, and range selection of options.
 *
 * ## Supported Commands
 *
 * - "add [option]" - Add option to selection
 * - "remove [option]" - Remove option from selection
 * - "toggle [option]" - Toggle option state
 * - "select all" - Select all options
 * - "clear all" / "deselect all" - Clear selection
 * - "select options 1 through 5" - Range selection
 *
 * ## AVID-based Targeting
 *
 * MultiSelect components are identified by AVID. Options within are matched by:
 * 1. Text content (visible label)
 * 2. Position/number (e.g., "option 3")
 * 3. contentDescription fallback
 *
 * ## Disambiguation
 *
 * When multiple options match a voice command, numbered badges are shown
 * only on matching options, and the user says a number to select.
 */
package com.augmentalis.avamagic.voice.handlers.input

import com.avanues.logging.LoggerFactory
import com.augmentalis.voiceoscore.ActionCategory
import com.augmentalis.voiceoscore.BaseHandler
import com.augmentalis.voiceoscore.Bounds
import com.augmentalis.voiceoscore.DisambiguationResult
import com.augmentalis.voiceoscore.ElementDisambiguator
import com.augmentalis.voiceoscore.ElementInfo
import com.augmentalis.voiceoscore.HandlerResult
import com.augmentalis.voiceoscore.NumberedMatch
import com.augmentalis.voiceoscore.QuantizedCommand

/**
 * Handler for multi-select component voice interactions.
 *
 * Supports voice commands for:
 * - Adding/removing individual options
 * - Toggling option state
 * - Selecting/clearing all options
 * - Range selection (e.g., "options 1 through 5")
 *
 * @param executor Platform-specific executor for multi-select operations
 * @param disambiguator Element disambiguator for handling duplicate options
 */
class MultiSelectHandler(
    private val executor: MultiSelectExecutor,
    private val disambiguator: ElementDisambiguator = ElementDisambiguator.default
) : BaseHandler() {

    companion object {
        private const val TAG = "MultiSelectHandler"
        private val Log = LoggerFactory.getLogger(TAG)

        // Pattern for range selection: "select options 1 through 5"
        private val RANGE_PATTERN = Regex(
            """(?:select\s+)?options?\s+(\d+)\s+(?:through|thru|to)\s+(\d+)""",
            RegexOption.IGNORE_CASE
        )

        // Pattern for single number selection: "option 3", "select 3"
        private val NUMBER_PATTERN = Regex(
            """(?:option|select|add|remove|toggle)?\s*(\d+)""",
            RegexOption.IGNORE_CASE
        )
    }

    override val category: ActionCategory = ActionCategory.UI

    override val supportedActions: List<String> = listOf(
        // Add actions
        "add", "add option",
        // Remove actions
        "remove", "remove option", "deselect",
        // Toggle actions
        "toggle", "toggle option",
        // Select all
        "select all", "choose all", "check all",
        // Clear all
        "clear all", "deselect all", "uncheck all", "clear selection",
        // Range selection
        "select options", "options through"
    )

    /**
     * Current disambiguation state for pending actions.
     * When non-null, the system is waiting for user to say a number.
     */
    @Volatile
    private var activeDisambiguation: ActiveMultiSelectDisambiguation? = null

    /**
     * Callback invoked when disambiguation overlay should be shown.
     * Platform implementations should show numbered badges on matching options.
     */
    var onShowDisambiguation: ((DisambiguationResult) -> Unit)? = null

    /**
     * Callback invoked when disambiguation is complete or cancelled.
     * Platform implementations should hide numbered badges.
     */
    var onHideDisambiguation: (() -> Unit)? = null

    /**
     * Callback invoked to provide voice feedback to the user.
     */
    var onVoiceFeedback: ((String) -> Unit)? = null

    override suspend fun execute(
        command: QuantizedCommand,
        params: Map<String, Any>
    ): HandlerResult {
        val normalizedAction = command.phrase.lowercase().trim()

        Log.d { "Processing multi-select command: $normalizedAction" }

        return when {
            // Select all options
            normalizedAction in listOf("select all", "choose all", "check all") -> {
                handleSelectAll(command)
            }

            // Clear all selections
            normalizedAction in listOf("clear all", "deselect all", "uncheck all", "clear selection") -> {
                handleClearAll(command)
            }

            // Range selection: "select options 1 through 5"
            RANGE_PATTERN.containsMatchIn(normalizedAction) -> {
                handleRangeSelection(normalizedAction, command)
            }

            // Add option: "add [option]"
            normalizedAction.startsWith("add ") -> {
                val target = normalizedAction.removePrefix("add ").removePrefix("option ").trim()
                handleOptionAction(target, MultiSelectAction.ADD, command)
            }

            // Remove option: "remove [option]" or "deselect [option]"
            normalizedAction.startsWith("remove ") || normalizedAction.startsWith("deselect ") -> {
                val target = normalizedAction
                    .removePrefix("remove ")
                    .removePrefix("deselect ")
                    .removePrefix("option ")
                    .trim()
                handleOptionAction(target, MultiSelectAction.REMOVE, command)
            }

            // Toggle option: "toggle [option]"
            normalizedAction.startsWith("toggle ") -> {
                val target = normalizedAction.removePrefix("toggle ").removePrefix("option ").trim()
                handleOptionAction(target, MultiSelectAction.TOGGLE, command)
            }

            else -> HandlerResult.notHandled()
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Select All / Clear All
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Handle "select all" command.
     */
    private suspend fun handleSelectAll(command: QuantizedCommand): HandlerResult {
        // Find target MultiSelect component by AVID if specified
        val targetAvid = command.targetAvid
        val multiSelect = if (targetAvid != null) {
            executor.findMultiSelectByAvid(targetAvid)
        } else {
            executor.findFocusedMultiSelect()
        }

        if (multiSelect == null) {
            return HandlerResult.Failure(
                reason = "No multi-select component found",
                recoverable = true,
                suggestedAction = "Focus on a multi-select component first"
            )
        }

        val result = executor.selectAll(multiSelect)
        return if (result.success) {
            val feedback = "Selected all ${result.selectedCount} options"
            onVoiceFeedback?.invoke(feedback)
            HandlerResult.Success(
                message = feedback,
                data = mapOf(
                    "selectedCount" to result.selectedCount,
                    "multiSelectAvid" to multiSelect.avid,
                    "accessibility_announcement" to feedback
                )
            )
        } else {
            HandlerResult.failure(
                reason = result.error ?: "Could not select all options",
                recoverable = true
            )
        }
    }

    /**
     * Handle "clear all" / "deselect all" command.
     */
    private suspend fun handleClearAll(command: QuantizedCommand): HandlerResult {
        val targetAvid = command.targetAvid
        val multiSelect = if (targetAvid != null) {
            executor.findMultiSelectByAvid(targetAvid)
        } else {
            executor.findFocusedMultiSelect()
        }

        if (multiSelect == null) {
            return HandlerResult.Failure(
                reason = "No multi-select component found",
                recoverable = true,
                suggestedAction = "Focus on a multi-select component first"
            )
        }

        val result = executor.clearAll(multiSelect)
        return if (result.success) {
            val feedback = "Cleared all selections"
            onVoiceFeedback?.invoke(feedback)
            HandlerResult.Success(
                message = feedback,
                data = mapOf(
                    "clearedCount" to result.clearedCount,
                    "multiSelectAvid" to multiSelect.avid,
                    "accessibility_announcement" to feedback
                )
            )
        } else {
            HandlerResult.failure(
                reason = result.error ?: "Could not clear selections",
                recoverable = true
            )
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Range Selection
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Handle range selection: "select options 1 through 5"
     */
    private suspend fun handleRangeSelection(
        normalizedAction: String,
        command: QuantizedCommand
    ): HandlerResult {
        val matchResult = RANGE_PATTERN.find(normalizedAction)
            ?: return HandlerResult.failure("Could not parse range selection")

        val startIndex = matchResult.groupValues[1].toIntOrNull()
        val endIndex = matchResult.groupValues[2].toIntOrNull()

        if (startIndex == null || endIndex == null) {
            return HandlerResult.failure("Invalid range numbers")
        }

        if (startIndex > endIndex) {
            return HandlerResult.Failure(
                reason = "Start index ($startIndex) must be less than or equal to end index ($endIndex)",
                recoverable = true,
                suggestedAction = "Try 'select options $endIndex through $startIndex'"
            )
        }

        // Find target MultiSelect
        val targetAvid = command.targetAvid
        val multiSelect = if (targetAvid != null) {
            executor.findMultiSelectByAvid(targetAvid)
        } else {
            executor.findFocusedMultiSelect()
        }

        if (multiSelect == null) {
            return HandlerResult.Failure(
                reason = "No multi-select component found",
                recoverable = true,
                suggestedAction = "Focus on a multi-select component first"
            )
        }

        // Get all options
        val options = executor.getOptions(multiSelect)
        if (options.isEmpty()) {
            return HandlerResult.failure("Multi-select has no options")
        }

        // Validate range (1-based indices)
        if (startIndex < 1 || endIndex > options.size) {
            return HandlerResult.failure(
                reason = "Range $startIndex to $endIndex is out of bounds (1 to ${options.size})",
                recoverable = true
            )
        }

        // Select the range (convert to 0-based indices)
        val result = executor.selectRange(
            multiSelect = multiSelect,
            startIndex = startIndex - 1,
            endIndex = endIndex - 1
        )

        return if (result.success) {
            val feedback = "Selected options $startIndex through $endIndex (${result.selectedCount} options)"
            onVoiceFeedback?.invoke(feedback)
            HandlerResult.Success(
                message = feedback,
                data = mapOf(
                    "startIndex" to startIndex,
                    "endIndex" to endIndex,
                    "selectedCount" to result.selectedCount,
                    "accessibility_announcement" to feedback
                )
            )
        } else {
            HandlerResult.failure(
                reason = result.error ?: "Could not select range",
                recoverable = true
            )
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Individual Option Actions (with disambiguation)
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Handle add/remove/toggle action on a specific option.
     *
     * If the target is a number, executes directly.
     * If the target is text and matches multiple options, triggers disambiguation.
     */
    private suspend fun handleOptionAction(
        target: String,
        action: MultiSelectAction,
        command: QuantizedCommand
    ): HandlerResult {
        // Find target MultiSelect
        val targetAvid = command.targetAvid
        val multiSelect = if (targetAvid != null) {
            executor.findMultiSelectByAvid(targetAvid)
        } else {
            executor.findFocusedMultiSelect()
        }

        if (multiSelect == null) {
            return HandlerResult.Failure(
                reason = "No multi-select component found",
                recoverable = true,
                suggestedAction = "Focus on a multi-select component first"
            )
        }

        // Get all options
        val options = executor.getOptions(multiSelect)
        if (options.isEmpty()) {
            return HandlerResult.failure("Multi-select has no options")
        }

        // Check if target is a number (direct selection by position)
        val numberMatch = NUMBER_PATTERN.find(target)
        if (numberMatch != null) {
            val optionNumber = numberMatch.groupValues[1].toIntOrNull()
            if (optionNumber != null && optionNumber in 1..options.size) {
                return executeOptionAction(
                    option = options[optionNumber - 1],
                    action = action,
                    multiSelect = multiSelect
                )
            }
        }

        // Text-based matching with disambiguation
        return handleOptionWithDisambiguation(
            target = target,
            action = action,
            options = options,
            multiSelect = multiSelect
        )
    }

    /**
     * Handle option matching with disambiguation support.
     *
     * Flow:
     * 1. Find all options matching the target text
     * 2. If single match: execute directly
     * 3. If multiple matches: show numbered badges, await user selection
     * 4. If no matches: return failure
     */
    private suspend fun handleOptionWithDisambiguation(
        target: String,
        action: MultiSelectAction,
        options: List<OptionInfo>,
        multiSelect: MultiSelectInfo
    ): HandlerResult {
        // Convert OptionInfo to ElementInfo for disambiguation
        val elementOptions = options.map { it.toElementInfo() }

        // For short targets, use exact match; for longer targets, use contains
        val matchMode = if (target.length <= 2) {
            ElementDisambiguator.MatchMode.EXACT
        } else {
            ElementDisambiguator.MatchMode.CONTAINS
        }

        val result = disambiguator.findMatches(
            query = target,
            elements = elementOptions,
            matchMode = matchMode
        )

        return when {
            // No matches found
            result.noMatches -> {
                HandlerResult.Failure(
                    reason = "Could not find option: $target",
                    recoverable = true,
                    suggestedAction = "Try saying the exact option name or its number"
                )
            }

            // Single match - execute directly
            result.singleMatch != null -> {
                val matchedOption = findOptionByAvid(options, result.singleMatch!!.avid)
                    ?: return HandlerResult.failure("Option match lost unexpectedly")

                executeOptionAction(
                    option = matchedOption,
                    action = action,
                    multiSelect = multiSelect
                )
            }

            // Multiple matches - need disambiguation
            result.needsDisambiguation -> {
                // Store disambiguation state
                activeDisambiguation = ActiveMultiSelectDisambiguation(
                    result = result,
                    pendingAction = action,
                    multiSelect = multiSelect,
                    options = options
                )

                // Notify platform to show numbered badges
                onShowDisambiguation?.invoke(result)

                HandlerResult.awaitingSelection(
                    message = "${result.matchCount} '${target}' options found. Say a number to select.",
                    matchCount = result.matchCount,
                    accessibilityAnnouncement = result.getAccessibilityAnnouncement()
                )
            }

            else -> HandlerResult.failure("Unexpected disambiguation state")
        }
    }

    /**
     * Execute the actual option action (add/remove/toggle).
     */
    private suspend fun executeOptionAction(
        option: OptionInfo,
        action: MultiSelectAction,
        multiSelect: MultiSelectInfo
    ): HandlerResult {
        val result = when (action) {
            MultiSelectAction.ADD -> executor.addOption(multiSelect, option)
            MultiSelectAction.REMOVE -> executor.removeOption(multiSelect, option)
            MultiSelectAction.TOGGLE -> executor.toggleOption(multiSelect, option)
        }

        return if (result.success) {
            val actionVerb = when (action) {
                MultiSelectAction.ADD -> if (result.wasAlreadySelected) "already selected" else "added"
                MultiSelectAction.REMOVE -> if (!result.wasAlreadySelected) "already deselected" else "removed"
                MultiSelectAction.TOGGLE -> if (result.isNowSelected) "selected" else "deselected"
            }
            val feedback = "${option.label} $actionVerb"
            onVoiceFeedback?.invoke(feedback)

            HandlerResult.Success(
                message = feedback,
                data = mapOf(
                    "option" to option.label,
                    "action" to action.name,
                    "isNowSelected" to result.isNowSelected,
                    "accessibility_announcement" to feedback
                )
            )
        } else {
            HandlerResult.failure(
                reason = result.error ?: "Could not ${action.name.lowercase()} option: ${option.label}",
                recoverable = true
            )
        }
    }

    /**
     * Find option by AVID from option list.
     */
    private fun findOptionByAvid(options: List<OptionInfo>, avid: String?): OptionInfo? {
        if (avid == null) return null
        return options.find { it.avid == avid }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Disambiguation Support
    // ═══════════════════════════════════════════════════════════════════════════

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

        // Find the corresponding option
        val selectedOption = findOptionByAvid(disambiguation.options, selectedElement.avid)
            ?: return HandlerResult.failure("Selected option not found")

        // Clear disambiguation state
        clearDisambiguation()

        // Execute the pending action
        return executeOptionAction(
            option = selectedOption,
            action = disambiguation.pendingAction,
            multiSelect = disambiguation.multiSelect
        )
    }

    /**
     * Cancel active disambiguation.
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

    /**
     * Get current selection state for voice feedback.
     */
    suspend fun getSelectionSummary(command: QuantizedCommand): HandlerResult {
        val targetAvid = command.targetAvid
        val multiSelect = if (targetAvid != null) {
            executor.findMultiSelectByAvid(targetAvid)
        } else {
            executor.findFocusedMultiSelect()
        }

        if (multiSelect == null) {
            return HandlerResult.failure("No multi-select component found")
        }

        val state = executor.getSelectionState(multiSelect)
        val feedback = buildString {
            append("${state.selectedCount} of ${state.totalCount} options selected")
            if (state.selectedCount > 0 && state.selectedCount <= 5) {
                append(": ")
                append(state.selectedOptions.joinToString(", ") { it.label })
            }
        }

        onVoiceFeedback?.invoke(feedback)

        return HandlerResult.Success(
            message = feedback,
            data = mapOf(
                "selectedCount" to state.selectedCount,
                "totalCount" to state.totalCount,
                "selectedOptions" to state.selectedOptions.map { it.label },
                "accessibility_announcement" to feedback
            )
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// Supporting Types
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * Actions that can be performed on multi-select options.
 */
enum class MultiSelectAction {
    /** Add option to selection */
    ADD,
    /** Remove option from selection */
    REMOVE,
    /** Toggle option selection state */
    TOGGLE
}

/**
 * Active disambiguation state for multi-select operations.
 */
private data class ActiveMultiSelectDisambiguation(
    val result: DisambiguationResult,
    val pendingAction: MultiSelectAction,
    val multiSelect: MultiSelectInfo,
    val options: List<OptionInfo>
)

/**
 * Information about a multi-select component.
 *
 * @property avid AVID fingerprint for the component
 * @property name Display name or label
 * @property bounds Screen bounds for the component
 * @property isFocused Whether this component currently has focus
 */
data class MultiSelectInfo(
    val avid: String,
    val name: String = "",
    val bounds: Bounds = Bounds.EMPTY,
    val isFocused: Boolean = false
)

/**
 * Information about an option within a multi-select.
 *
 * @property avid AVID fingerprint for the option
 * @property label Display text for the option
 * @property isSelected Whether this option is currently selected
 * @property index 0-based index within the multi-select
 * @property bounds Screen bounds for the option
 */
data class OptionInfo(
    val avid: String,
    val label: String,
    val isSelected: Boolean = false,
    val index: Int = -1,
    val bounds: Bounds = Bounds.EMPTY
) {
    /**
     * Convert to ElementInfo for disambiguation.
     */
    fun toElementInfo(): ElementInfo = ElementInfo(
        className = "MultiSelectOption",
        text = label,
        bounds = bounds,
        isClickable = true,
        isEnabled = true,
        avid = avid,
        isSelected = isSelected
    )
}

/**
 * Result of a multi-select operation.
 */
data class MultiSelectOperationResult(
    val success: Boolean,
    val error: String? = null,
    val selectedCount: Int = 0,
    val clearedCount: Int = 0,
    val isNowSelected: Boolean = false,
    val wasAlreadySelected: Boolean = false
) {
    companion object {
        fun success(
            selectedCount: Int = 0,
            clearedCount: Int = 0,
            isNowSelected: Boolean = false,
            wasAlreadySelected: Boolean = false
        ) = MultiSelectOperationResult(
            success = true,
            selectedCount = selectedCount,
            clearedCount = clearedCount,
            isNowSelected = isNowSelected,
            wasAlreadySelected = wasAlreadySelected
        )

        fun error(message: String) = MultiSelectOperationResult(
            success = false,
            error = message
        )
    }
}

/**
 * Current selection state of a multi-select component.
 */
data class MultiSelectState(
    val totalCount: Int,
    val selectedCount: Int,
    val selectedOptions: List<OptionInfo>
)

// ═══════════════════════════════════════════════════════════════════════════════
// Platform Executor Interface
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * Platform-specific executor for multi-select operations.
 *
 * Implementations should:
 * 1. Find multi-select components by AVID or focus state
 * 2. Enumerate options within a multi-select
 * 3. Perform add/remove/toggle operations
 * 4. Handle select all and clear all operations
 * 5. Support range selection
 */
interface MultiSelectExecutor {
    // ═══════════════════════════════════════════════════════════════════════════
    // Component Discovery
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Find a multi-select component by its AVID.
     *
     * @param avid The AVID fingerprint of the multi-select
     * @return MultiSelectInfo if found, null otherwise
     */
    suspend fun findMultiSelectByAvid(avid: String): MultiSelectInfo?

    /**
     * Find a multi-select component by its name/label.
     *
     * @param name The display name to search for
     * @return MultiSelectInfo if found, null otherwise
     */
    suspend fun findMultiSelectByName(name: String): MultiSelectInfo?

    /**
     * Find the currently focused multi-select component.
     *
     * @return MultiSelectInfo if a multi-select has focus, null otherwise
     */
    suspend fun findFocusedMultiSelect(): MultiSelectInfo?

    /**
     * Get all multi-select components on the current screen.
     *
     * @return List of all visible multi-select components
     */
    suspend fun getAllMultiSelects(): List<MultiSelectInfo>

    // ═══════════════════════════════════════════════════════════════════════════
    // Option Enumeration
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Get all options within a multi-select component.
     *
     * @param multiSelect The multi-select component
     * @return List of options, in display order
     */
    suspend fun getOptions(multiSelect: MultiSelectInfo): List<OptionInfo>

    /**
     * Get current selection state.
     *
     * @param multiSelect The multi-select component
     * @return Current selection state
     */
    suspend fun getSelectionState(multiSelect: MultiSelectInfo): MultiSelectState

    // ═══════════════════════════════════════════════════════════════════════════
    // Single Option Operations
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Add an option to the selection.
     *
     * @param multiSelect The multi-select component
     * @param option The option to add
     * @return Operation result
     */
    suspend fun addOption(multiSelect: MultiSelectInfo, option: OptionInfo): MultiSelectOperationResult

    /**
     * Remove an option from the selection.
     *
     * @param multiSelect The multi-select component
     * @param option The option to remove
     * @return Operation result
     */
    suspend fun removeOption(multiSelect: MultiSelectInfo, option: OptionInfo): MultiSelectOperationResult

    /**
     * Toggle an option's selection state.
     *
     * @param multiSelect The multi-select component
     * @param option The option to toggle
     * @return Operation result
     */
    suspend fun toggleOption(multiSelect: MultiSelectInfo, option: OptionInfo): MultiSelectOperationResult

    // ═══════════════════════════════════════════════════════════════════════════
    // Bulk Operations
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Select all options.
     *
     * @param multiSelect The multi-select component
     * @return Operation result with count of newly selected options
     */
    suspend fun selectAll(multiSelect: MultiSelectInfo): MultiSelectOperationResult

    /**
     * Clear all selections.
     *
     * @param multiSelect The multi-select component
     * @return Operation result with count of cleared options
     */
    suspend fun clearAll(multiSelect: MultiSelectInfo): MultiSelectOperationResult

    /**
     * Select a range of options by index.
     *
     * @param multiSelect The multi-select component
     * @param startIndex 0-based start index (inclusive)
     * @param endIndex 0-based end index (inclusive)
     * @return Operation result with count of selected options
     */
    suspend fun selectRange(
        multiSelect: MultiSelectInfo,
        startIndex: Int,
        endIndex: Int
    ): MultiSelectOperationResult
}
