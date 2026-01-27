/**
 * TagInputHandler.kt - Voice handler for Tag/Chip input interactions
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2026-01-28
 *
 * Purpose: Voice-driven tag/chip input control for adding, removing, and managing tags
 * Features:
 * - Add new tags by name
 * - Remove tags by name or position
 * - Clear all tags at once
 * - Select and edit existing tags
 * - Navigate to first/last tag
 * - AVID-based targeting for precise element selection
 * - Voice feedback for tag operations
 *
 * Location: CommandManager module handlers
 *
 * ## Supported Commands
 *
 * Add tags:
 * - "add tag [name]" - Add a new tag with given name
 * - "add [name]" - Shorthand for adding a tag
 *
 * Remove tags:
 * - "remove tag [name]" - Remove tag by name
 * - "delete [name]" - Shorthand for removing tag by name
 * - "remove last" / "delete last" - Remove the most recently added tag
 *
 * Clear tags:
 * - "clear all tags" - Remove all tags
 * - "clear tags" - Remove all tags
 *
 * Select tags:
 * - "select tag [name]" - Select/focus a tag by name
 * - "tag [N]" - Select the Nth tag (1-indexed)
 * - "first tag" - Select the first tag
 * - "last tag" - Select the last tag
 *
 * Edit tags:
 * - "edit tag [name]" - Edit an existing tag by name
 *
 * ## Tag Input Components
 *
 * Supports:
 * - Material Chip Groups
 * - Custom tag/chip input components
 * - Autocomplete tag inputs
 * - Multi-select chip components
 */

package com.augmentalis.avamagic.voice.handlers.input

import android.util.Log
import com.augmentalis.voiceoscore.ActionCategory
import com.augmentalis.voiceoscore.BaseHandler
import com.augmentalis.voiceoscore.Bounds
import com.augmentalis.voiceoscore.ElementInfo
import com.augmentalis.voiceoscore.HandlerResult
import com.augmentalis.voiceoscore.QuantizedCommand

/**
 * Voice command handler for Tag/Chip input interactions.
 *
 * Provides comprehensive voice control for tag input components including:
 * - Adding new tags by name
 * - Removing tags by name or position
 * - Clearing all tags
 * - Selecting and editing tags
 * - Navigating to specific tags by index
 *
 * Thread Safety:
 * - All operations are suspend functions
 * - State modifications are atomic via executor
 *
 * @param executor Platform-specific executor for tag input operations
 */
class TagInputHandler(
    private val executor: TagInputExecutor
) : BaseHandler() {

    companion object {
        private const val TAG = "TagInputHandler"

        // Patterns for parsing commands

        // Add tag patterns
        private val ADD_TAG_PATTERN = Regex(
            """^add\s+tag\s+(.+)$""",
            RegexOption.IGNORE_CASE
        )

        private val ADD_SHORTHAND_PATTERN = Regex(
            """^add\s+(.+)$""",
            RegexOption.IGNORE_CASE
        )

        // Remove tag patterns
        private val REMOVE_TAG_PATTERN = Regex(
            """^remove\s+tag\s+(.+)$""",
            RegexOption.IGNORE_CASE
        )

        private val DELETE_TAG_PATTERN = Regex(
            """^delete\s+(.+)$""",
            RegexOption.IGNORE_CASE
        )

        private val REMOVE_LAST_PATTERN = Regex(
            """^(?:remove|delete)\s+last$""",
            RegexOption.IGNORE_CASE
        )

        // Clear tags patterns
        private val CLEAR_ALL_TAGS_PATTERN = Regex(
            """^clear\s+(?:all\s+)?tags$""",
            RegexOption.IGNORE_CASE
        )

        // Select tag patterns
        private val SELECT_TAG_PATTERN = Regex(
            """^select\s+tag\s+(.+)$""",
            RegexOption.IGNORE_CASE
        )

        private val TAG_NUMBER_PATTERN = Regex(
            """^tag\s+(\d+)$""",
            RegexOption.IGNORE_CASE
        )

        private val FIRST_LAST_TAG_PATTERN = Regex(
            """^(first|last)\s+tag$""",
            RegexOption.IGNORE_CASE
        )

        // Edit tag pattern
        private val EDIT_TAG_PATTERN = Regex(
            """^edit\s+tag\s+(.+)$""",
            RegexOption.IGNORE_CASE
        )

        // Word to number mapping for spoken ordinals and numbers
        private val WORD_NUMBERS = mapOf(
            "one" to 1, "two" to 2, "three" to 3, "four" to 4, "five" to 5,
            "six" to 6, "seven" to 7, "eight" to 8, "nine" to 9, "ten" to 10,
            "first" to 1, "second" to 2, "third" to 3, "fourth" to 4, "fifth" to 5,
            "sixth" to 6, "seventh" to 7, "eighth" to 8, "ninth" to 9, "tenth" to 10
        )
    }

    override val category: ActionCategory = ActionCategory.UI

    override val supportedActions: List<String> = listOf(
        // Add tags
        "add tag", "add",
        // Remove tags
        "remove tag", "delete", "remove last", "delete last",
        // Clear tags
        "clear all tags", "clear tags",
        // Select tags
        "select tag", "tag", "first tag", "last tag",
        // Edit tags
        "edit tag"
    )

    /**
     * Callback for voice feedback when a tag is added.
     */
    var onTagAdded: ((tagName: String, totalTags: Int) -> Unit)? = null

    /**
     * Callback for voice feedback when a tag is removed.
     */
    var onTagRemoved: ((tagName: String, remainingTags: Int) -> Unit)? = null

    /**
     * Callback for voice feedback when tags are cleared.
     */
    var onTagsCleared: ((clearedCount: Int) -> Unit)? = null

    /**
     * Callback for voice feedback when a tag is selected.
     */
    var onTagSelected: ((tagName: String, index: Int) -> Unit)? = null

    override suspend fun execute(
        command: QuantizedCommand,
        params: Map<String, Any>
    ): HandlerResult {
        val normalizedAction = command.phrase.lowercase().trim()

        Log.d(TAG, "Processing tag input command: $normalizedAction")

        return try {
            when {
                // Add tag: "add tag [name]"
                ADD_TAG_PATTERN.containsMatchIn(normalizedAction) -> {
                    handleAddTag(normalizedAction, command, ADD_TAG_PATTERN)
                }

                // Remove/Delete last tag
                REMOVE_LAST_PATTERN.containsMatchIn(normalizedAction) -> {
                    handleRemoveLastTag(command)
                }

                // Clear all tags
                CLEAR_ALL_TAGS_PATTERN.containsMatchIn(normalizedAction) -> {
                    handleClearTags(command)
                }

                // Remove tag by name: "remove tag [name]"
                REMOVE_TAG_PATTERN.containsMatchIn(normalizedAction) -> {
                    handleRemoveTag(normalizedAction, command, REMOVE_TAG_PATTERN)
                }

                // Delete tag by name: "delete [name]"
                DELETE_TAG_PATTERN.containsMatchIn(normalizedAction) -> {
                    handleRemoveTag(normalizedAction, command, DELETE_TAG_PATTERN)
                }

                // Select tag by name: "select tag [name]"
                SELECT_TAG_PATTERN.containsMatchIn(normalizedAction) -> {
                    handleSelectTagByName(normalizedAction, command)
                }

                // Select tag by number: "tag [N]"
                TAG_NUMBER_PATTERN.containsMatchIn(normalizedAction) -> {
                    handleSelectTagByNumber(normalizedAction, command)
                }

                // First/last tag
                FIRST_LAST_TAG_PATTERN.containsMatchIn(normalizedAction) -> {
                    handleFirstLastTag(normalizedAction, command)
                }

                // Edit tag: "edit tag [name]"
                EDIT_TAG_PATTERN.containsMatchIn(normalizedAction) -> {
                    handleEditTag(normalizedAction, command)
                }

                // Add shorthand: "add [name]" (must be checked last to avoid conflicts)
                ADD_SHORTHAND_PATTERN.containsMatchIn(normalizedAction) -> {
                    handleAddTag(normalizedAction, command, ADD_SHORTHAND_PATTERN)
                }

                else -> HandlerResult.notHandled()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error executing tag input command", e)
            HandlerResult.failure("Error: ${e.message}", recoverable = true)
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Command Handlers
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Handle "add tag [name]" or "add [name]" command.
     */
    private suspend fun handleAddTag(
        normalizedAction: String,
        command: QuantizedCommand,
        pattern: Regex
    ): HandlerResult {
        val matchResult = pattern.find(normalizedAction)
            ?: return HandlerResult.failure("Could not parse add tag command")

        val tagName = matchResult.groupValues[1].trim()

        if (tagName.isBlank()) {
            return HandlerResult.Failure(
                reason = "Tag name cannot be empty",
                recoverable = true,
                suggestedAction = "Say 'add tag work' or 'add important'"
            )
        }

        // Find the tag input
        val tagInputInfo = findTagInput(avid = command.targetAvid)
            ?: return HandlerResult.Failure(
                reason = "No tag input found",
                recoverable = true,
                suggestedAction = "Focus on a tag input field first"
            )

        // Check for max tags limit
        if (tagInputInfo.maxTags != null && tagInputInfo.tags.size >= tagInputInfo.maxTags) {
            return HandlerResult.Failure(
                reason = "Maximum number of tags (${tagInputInfo.maxTags}) reached",
                recoverable = true,
                suggestedAction = "Remove some tags first"
            )
        }

        // Check for duplicates if not allowed
        if (!tagInputInfo.allowDuplicates && tagInputInfo.tags.any { it.name.equals(tagName, ignoreCase = true) }) {
            return HandlerResult.Failure(
                reason = "Tag '$tagName' already exists",
                recoverable = true,
                suggestedAction = "Try adding a different tag"
            )
        }

        // Add the tag
        val result = executor.addTag(tagInputInfo, tagName)

        return if (result.success) {
            val newCount = (tagInputInfo.tags.size + 1)

            // Invoke callback for voice feedback
            onTagAdded?.invoke(tagName, newCount)

            Log.i(TAG, "Tag added: $tagName (total: $newCount)")

            HandlerResult.Success(
                message = "Added tag '$tagName'",
                data = mapOf(
                    "operation" to "add",
                    "tagName" to tagName,
                    "tagInputAvid" to tagInputInfo.avid,
                    "totalTags" to newCount,
                    "accessibility_announcement" to "Added tag $tagName"
                )
            )
        } else {
            HandlerResult.failure(
                reason = result.error ?: "Could not add tag",
                recoverable = true
            )
        }
    }

    /**
     * Handle "remove tag [name]" or "delete [name]" command.
     */
    private suspend fun handleRemoveTag(
        normalizedAction: String,
        command: QuantizedCommand,
        pattern: Regex
    ): HandlerResult {
        val matchResult = pattern.find(normalizedAction)
            ?: return HandlerResult.failure("Could not parse remove tag command")

        val tagName = matchResult.groupValues[1].trim()

        if (tagName.isBlank()) {
            return HandlerResult.Failure(
                reason = "Tag name cannot be empty",
                recoverable = true,
                suggestedAction = "Say 'remove tag work' or 'delete important'"
            )
        }

        // Find the tag input
        val tagInputInfo = findTagInput(avid = command.targetAvid)
            ?: return HandlerResult.Failure(
                reason = "No tag input found",
                recoverable = true,
                suggestedAction = "Focus on a tag input field first"
            )

        // Find the tag to remove
        val tagToRemove = tagInputInfo.tags.find { it.name.equals(tagName, ignoreCase = true) }
            ?: return HandlerResult.Failure(
                reason = "Tag '$tagName' not found",
                recoverable = true,
                suggestedAction = "Check the tag name and try again"
            )

        // Check if tag is removable
        if (!tagToRemove.isRemovable) {
            return HandlerResult.Failure(
                reason = "Tag '$tagName' cannot be removed",
                recoverable = true,
                suggestedAction = "This tag is locked and cannot be removed"
            )
        }

        // Remove the tag
        val result = executor.removeTag(tagInputInfo, tagToRemove.id)

        return if (result.success) {
            val remainingCount = (tagInputInfo.tags.size - 1).coerceAtLeast(0)

            // Invoke callback for voice feedback
            onTagRemoved?.invoke(tagName, remainingCount)

            Log.i(TAG, "Tag removed: $tagName (remaining: $remainingCount)")

            HandlerResult.Success(
                message = "Removed tag '$tagName'",
                data = mapOf(
                    "operation" to "remove",
                    "tagName" to tagName,
                    "tagId" to tagToRemove.id,
                    "tagInputAvid" to tagInputInfo.avid,
                    "remainingTags" to remainingCount,
                    "accessibility_announcement" to "Removed tag $tagName"
                )
            )
        } else {
            HandlerResult.failure(
                reason = result.error ?: "Could not remove tag",
                recoverable = true
            )
        }
    }

    /**
     * Handle "remove last" or "delete last" command.
     */
    private suspend fun handleRemoveLastTag(command: QuantizedCommand): HandlerResult {
        // Find the tag input
        val tagInputInfo = findTagInput(avid = command.targetAvid)
            ?: return HandlerResult.Failure(
                reason = "No tag input found",
                recoverable = true,
                suggestedAction = "Focus on a tag input field first"
            )

        // Check if there are any tags
        if (tagInputInfo.tags.isEmpty()) {
            return HandlerResult.Failure(
                reason = "No tags to remove",
                recoverable = true,
                suggestedAction = "Add some tags first"
            )
        }

        // Get the last tag
        val lastTag = tagInputInfo.tags.last()

        // Check if tag is removable
        if (!lastTag.isRemovable) {
            return HandlerResult.Failure(
                reason = "Last tag '${lastTag.name}' cannot be removed",
                recoverable = true,
                suggestedAction = "This tag is locked and cannot be removed"
            )
        }

        // Remove the last tag
        val result = executor.removeLastTag(tagInputInfo)

        return if (result.success) {
            val remainingCount = (tagInputInfo.tags.size - 1).coerceAtLeast(0)

            // Invoke callback for voice feedback
            onTagRemoved?.invoke(lastTag.name, remainingCount)

            Log.i(TAG, "Last tag removed: ${lastTag.name} (remaining: $remainingCount)")

            HandlerResult.Success(
                message = "Removed last tag '${lastTag.name}'",
                data = mapOf(
                    "operation" to "remove_last",
                    "tagName" to lastTag.name,
                    "tagId" to lastTag.id,
                    "tagInputAvid" to tagInputInfo.avid,
                    "remainingTags" to remainingCount,
                    "accessibility_announcement" to "Removed tag ${lastTag.name}"
                )
            )
        } else {
            HandlerResult.failure(
                reason = result.error ?: "Could not remove last tag",
                recoverable = true
            )
        }
    }

    /**
     * Handle "clear all tags" or "clear tags" command.
     */
    private suspend fun handleClearTags(command: QuantizedCommand): HandlerResult {
        // Find the tag input
        val tagInputInfo = findTagInput(avid = command.targetAvid)
            ?: return HandlerResult.Failure(
                reason = "No tag input found",
                recoverable = true,
                suggestedAction = "Focus on a tag input field first"
            )

        // Check if there are any tags
        if (tagInputInfo.tags.isEmpty()) {
            return HandlerResult.Failure(
                reason = "No tags to clear",
                recoverable = true,
                suggestedAction = "Add some tags first"
            )
        }

        // Count removable tags
        val removableTags = tagInputInfo.tags.filter { it.isRemovable }
        val lockedTagCount = tagInputInfo.tags.size - removableTags.size

        if (removableTags.isEmpty()) {
            return HandlerResult.Failure(
                reason = "All tags are locked and cannot be removed",
                recoverable = true,
                suggestedAction = "No removable tags available"
            )
        }

        val tagCount = removableTags.size

        // Clear the tags
        val result = executor.clearTags(tagInputInfo)

        return if (result.success) {
            // Invoke callback for voice feedback
            onTagsCleared?.invoke(tagCount)

            Log.i(TAG, "All tags cleared: $tagCount tags removed")

            val message = if (lockedTagCount > 0) {
                "Cleared $tagCount tags ($lockedTagCount locked tags remain)"
            } else {
                "Cleared all $tagCount tags"
            }

            HandlerResult.Success(
                message = message,
                data = mapOf(
                    "operation" to "clear",
                    "clearedCount" to tagCount,
                    "lockedCount" to lockedTagCount,
                    "tagInputAvid" to tagInputInfo.avid,
                    "accessibility_announcement" to "Cleared $tagCount tags"
                )
            )
        } else {
            HandlerResult.failure(
                reason = result.error ?: "Could not clear tags",
                recoverable = true
            )
        }
    }

    /**
     * Handle "select tag [name]" command.
     */
    private suspend fun handleSelectTagByName(
        normalizedAction: String,
        command: QuantizedCommand
    ): HandlerResult {
        val matchResult = SELECT_TAG_PATTERN.find(normalizedAction)
            ?: return HandlerResult.failure("Could not parse select tag command")

        val tagName = matchResult.groupValues[1].trim()

        if (tagName.isBlank()) {
            return HandlerResult.Failure(
                reason = "Tag name cannot be empty",
                recoverable = true,
                suggestedAction = "Say 'select tag work' to select the work tag"
            )
        }

        // Find the tag input
        val tagInputInfo = findTagInput(avid = command.targetAvid)
            ?: return HandlerResult.Failure(
                reason = "No tag input found",
                recoverable = true,
                suggestedAction = "Focus on a tag input field first"
            )

        // Find the tag to select
        val tagToSelect = tagInputInfo.tags.find { it.name.equals(tagName, ignoreCase = true) }
            ?: return HandlerResult.Failure(
                reason = "Tag '$tagName' not found",
                recoverable = true,
                suggestedAction = "Check the tag name and try again"
            )

        val tagIndex = tagInputInfo.tags.indexOf(tagToSelect)

        // Select the tag
        val result = executor.selectTag(tagInputInfo, tagToSelect.id)

        return if (result.success) {
            // Invoke callback for voice feedback
            onTagSelected?.invoke(tagName, tagIndex + 1)

            Log.i(TAG, "Tag selected: $tagName (index: ${tagIndex + 1})")

            HandlerResult.Success(
                message = "Selected tag '$tagName'",
                data = mapOf(
                    "operation" to "select",
                    "tagName" to tagName,
                    "tagId" to tagToSelect.id,
                    "tagIndex" to (tagIndex + 1),
                    "tagInputAvid" to tagInputInfo.avid,
                    "accessibility_announcement" to "Selected tag $tagName"
                )
            )
        } else {
            HandlerResult.failure(
                reason = result.error ?: "Could not select tag",
                recoverable = true
            )
        }
    }

    /**
     * Handle "tag [N]" command to select tag by number.
     */
    private suspend fun handleSelectTagByNumber(
        normalizedAction: String,
        command: QuantizedCommand
    ): HandlerResult {
        val matchResult = TAG_NUMBER_PATTERN.find(normalizedAction)
            ?: return HandlerResult.failure("Could not parse tag number command")

        val tagNumber = matchResult.groupValues[1].toIntOrNull()
            ?: return HandlerResult.Failure(
                reason = "Invalid tag number",
                recoverable = true,
                suggestedAction = "Say 'tag 1', 'tag 2', etc."
            )

        // Find the tag input
        val tagInputInfo = findTagInput(avid = command.targetAvid)
            ?: return HandlerResult.Failure(
                reason = "No tag input found",
                recoverable = true,
                suggestedAction = "Focus on a tag input field first"
            )

        // Validate tag number (1-indexed)
        if (tagNumber < 1 || tagNumber > tagInputInfo.tags.size) {
            return HandlerResult.Failure(
                reason = "Tag number $tagNumber is out of range (1-${tagInputInfo.tags.size})",
                recoverable = true,
                suggestedAction = if (tagInputInfo.tags.isEmpty()) {
                    "No tags available"
                } else {
                    "Try 'tag 1' through 'tag ${tagInputInfo.tags.size}'"
                }
            )
        }

        // Get the tag (convert to 0-indexed)
        val tagToSelect = tagInputInfo.tags[tagNumber - 1]

        // Select the tag
        val result = executor.selectTag(tagInputInfo, tagToSelect.id)

        return if (result.success) {
            // Invoke callback for voice feedback
            onTagSelected?.invoke(tagToSelect.name, tagNumber)

            Log.i(TAG, "Tag $tagNumber selected: ${tagToSelect.name}")

            HandlerResult.Success(
                message = "Selected tag $tagNumber: '${tagToSelect.name}'",
                data = mapOf(
                    "operation" to "select_by_number",
                    "tagNumber" to tagNumber,
                    "tagName" to tagToSelect.name,
                    "tagId" to tagToSelect.id,
                    "tagInputAvid" to tagInputInfo.avid,
                    "accessibility_announcement" to "Selected tag ${tagToSelect.name}"
                )
            )
        } else {
            HandlerResult.failure(
                reason = result.error ?: "Could not select tag",
                recoverable = true
            )
        }
    }

    /**
     * Handle "first tag" or "last tag" command.
     */
    private suspend fun handleFirstLastTag(
        normalizedAction: String,
        command: QuantizedCommand
    ): HandlerResult {
        val matchResult = FIRST_LAST_TAG_PATTERN.find(normalizedAction)
            ?: return HandlerResult.failure("Could not parse first/last tag command")

        val position = matchResult.groupValues[1].lowercase()

        // Find the tag input
        val tagInputInfo = findTagInput(avid = command.targetAvid)
            ?: return HandlerResult.Failure(
                reason = "No tag input found",
                recoverable = true,
                suggestedAction = "Focus on a tag input field first"
            )

        // Check if there are any tags
        if (tagInputInfo.tags.isEmpty()) {
            return HandlerResult.Failure(
                reason = "No tags available",
                recoverable = true,
                suggestedAction = "Add some tags first"
            )
        }

        // Get the tag based on position
        val (tagToSelect, tagIndex) = when (position) {
            "first" -> tagInputInfo.tags.first() to 1
            "last" -> tagInputInfo.tags.last() to tagInputInfo.tags.size
            else -> return HandlerResult.failure("Unknown position: $position")
        }

        // Select the tag
        val result = executor.selectTag(tagInputInfo, tagToSelect.id)

        return if (result.success) {
            // Invoke callback for voice feedback
            onTagSelected?.invoke(tagToSelect.name, tagIndex)

            Log.i(TAG, "$position tag selected: ${tagToSelect.name}")

            HandlerResult.Success(
                message = "Selected $position tag: '${tagToSelect.name}'",
                data = mapOf(
                    "operation" to "select_$position",
                    "position" to position,
                    "tagName" to tagToSelect.name,
                    "tagId" to tagToSelect.id,
                    "tagIndex" to tagIndex,
                    "tagInputAvid" to tagInputInfo.avid,
                    "accessibility_announcement" to "Selected $position tag ${tagToSelect.name}"
                )
            )
        } else {
            HandlerResult.failure(
                reason = result.error ?: "Could not select $position tag",
                recoverable = true
            )
        }
    }

    /**
     * Handle "edit tag [name]" command.
     */
    private suspend fun handleEditTag(
        normalizedAction: String,
        command: QuantizedCommand
    ): HandlerResult {
        val matchResult = EDIT_TAG_PATTERN.find(normalizedAction)
            ?: return HandlerResult.failure("Could not parse edit tag command")

        val tagName = matchResult.groupValues[1].trim()

        if (tagName.isBlank()) {
            return HandlerResult.Failure(
                reason = "Tag name cannot be empty",
                recoverable = true,
                suggestedAction = "Say 'edit tag work' to edit the work tag"
            )
        }

        // Find the tag input
        val tagInputInfo = findTagInput(avid = command.targetAvid)
            ?: return HandlerResult.Failure(
                reason = "No tag input found",
                recoverable = true,
                suggestedAction = "Focus on a tag input field first"
            )

        // Find the tag to edit
        val tagToEdit = tagInputInfo.tags.find { it.name.equals(tagName, ignoreCase = true) }
            ?: return HandlerResult.Failure(
                reason = "Tag '$tagName' not found",
                recoverable = true,
                suggestedAction = "Check the tag name and try again"
            )

        val tagIndex = tagInputInfo.tags.indexOf(tagToEdit)

        // Edit the tag (enters edit mode)
        val result = executor.editTag(tagInputInfo, tagToEdit.id)

        return if (result.success) {
            Log.i(TAG, "Editing tag: $tagName")

            HandlerResult.Success(
                message = "Editing tag '$tagName'",
                data = mapOf(
                    "operation" to "edit",
                    "tagName" to tagName,
                    "tagId" to tagToEdit.id,
                    "tagIndex" to (tagIndex + 1),
                    "tagInputAvid" to tagInputInfo.avid,
                    "accessibility_announcement" to "Editing tag $tagName"
                )
            )
        } else {
            HandlerResult.failure(
                reason = result.error ?: "Could not edit tag",
                recoverable = true
            )
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Helper Methods
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Find tag input by AVID, name, or focus state.
     */
    private suspend fun findTagInput(
        name: String? = null,
        avid: String? = null
    ): TagInputInfo? {
        // Priority 1: AVID lookup (fastest, most precise)
        if (avid != null) {
            val tagInput = executor.findByAvid(avid)
            if (tagInput != null) return tagInput
        }

        // Priority 2: Name lookup
        if (name != null) {
            val tagInput = executor.findByName(name)
            if (tagInput != null) return tagInput
        }

        // Priority 3: Focused tag input
        return executor.findFocused()
    }

    /**
     * Parse a tag index from text (supports word numbers).
     *
     * Supports:
     * - "1", "2", "3" -> 1, 2, 3
     * - "one", "two", "three" -> 1, 2, 3
     * - "first", "second", "third" -> 1, 2, 3
     */
    private fun parseTagIndex(input: String): Int? {
        val trimmed = input.trim().lowercase()

        // Try direct numeric parsing
        trimmed.toIntOrNull()?.let { return it }

        // Try word number parsing
        return WORD_NUMBERS[trimmed]
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Voice Phrases for Speech Engine Registration
    // ═══════════════════════════════════════════════════════════════════════════

    override fun getVoicePhrases(): List<String> {
        return listOf(
            "add tag",
            "add",
            "remove tag",
            "delete",
            "remove last",
            "delete last",
            "clear all tags",
            "clear tags",
            "select tag",
            "tag",
            "first tag",
            "last tag",
            "edit tag"
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// Supporting Types
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * Information about an individual tag within a tag input.
 *
 * @property id Unique identifier for the tag within the component
 * @property name Display text of the tag
 * @property isSelected Whether this tag is currently selected/focused
 * @property isRemovable Whether this tag can be removed (some tags may be locked)
 */
data class TagInfo(
    val id: String,
    val name: String,
    val isSelected: Boolean = false,
    val isRemovable: Boolean = true
)

/**
 * Information about a tag input component.
 *
 * @property avid AVID fingerprint for the tag input (format: TAG:{hash8})
 * @property name Display name or associated label
 * @property tags List of current tags in the component
 * @property maxTags Maximum allowed tags (null for unlimited)
 * @property allowDuplicates Whether duplicate tag names are allowed
 * @property bounds Screen bounds for the tag input
 * @property isFocused Whether this tag input currently has focus
 * @property node Platform-specific node reference
 */
data class TagInputInfo(
    val avid: String,
    val name: String = "",
    val tags: List<TagInfo> = emptyList(),
    val maxTags: Int? = null,
    val allowDuplicates: Boolean = false,
    val bounds: Bounds = Bounds.EMPTY,
    val isFocused: Boolean = false,
    val node: Any? = null
) {
    /**
     * Convert to ElementInfo for general element operations.
     */
    fun toElementInfo(): ElementInfo = ElementInfo(
        className = "TagInput",
        text = name,
        bounds = bounds,
        isClickable = true,
        isEnabled = true,
        avid = avid,
        stateDescription = "${tags.size} tags"
    )
}

/**
 * Result of a tag input operation.
 *
 * @property success Whether the operation succeeded
 * @property error Error message if the operation failed
 * @property tagId ID of the affected tag (if applicable)
 * @property tagName Name of the affected tag (if applicable)
 */
data class TagInputOperationResult(
    val success: Boolean,
    val error: String? = null,
    val tagId: String? = null,
    val tagName: String? = null
) {
    companion object {
        /**
         * Create a successful result.
         */
        fun success(tagId: String? = null, tagName: String? = null) = TagInputOperationResult(
            success = true,
            tagId = tagId,
            tagName = tagName
        )

        /**
         * Create a failure result.
         */
        fun error(message: String) = TagInputOperationResult(
            success = false,
            error = message
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// Platform Executor Interface
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * Platform-specific executor for tag input operations.
 *
 * Implementations should:
 * 1. Find tag input components by AVID, name, or focus state
 * 2. Read current tags and their properties
 * 3. Add, remove, and manipulate tags via accessibility actions
 * 4. Handle both ChipGroup and custom tag input components
 *
 * ## Tag Input Detection Algorithm
 *
 * ```kotlin
 * fun findTagInputNode(rootNode: AccessibilityNodeInfo): AccessibilityNodeInfo? {
 *     // Look for nodes with className:
 *     // - com.google.android.material.chip.ChipGroup
 *     // - Custom tag/chip input implementations
 *     // - Nodes containing multiple Chip children
 * }
 * ```
 *
 * ## Tag Detection Algorithm
 *
 * ```kotlin
 * fun findTags(chipGroupNode: AccessibilityNodeInfo): List<TagInfo> {
 *     // Find all Chip children
 *     // - com.google.android.material.chip.Chip
 *     // - Filter for entry chips (isCheckable or has close button)
 *     // - Extract text and ID
 * }
 * ```
 *
 * ## Adding Tags
 *
 * ```kotlin
 * fun addTag(tagInput: TagInputInfo, tagName: String): Boolean {
 *     // Focus the input field within the ChipGroup
 *     // Set text via ACTION_SET_TEXT
 *     // Trigger chip creation (Enter key or action)
 * }
 * ```
 */
interface TagInputExecutor {

    // ═══════════════════════════════════════════════════════════════════════════
    // Tag Input Discovery
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Find a tag input by its AVID fingerprint.
     *
     * @param avid The AVID fingerprint (format: TAG:{hash8})
     * @return TagInputInfo if found, null otherwise
     */
    suspend fun findByAvid(avid: String): TagInputInfo?

    /**
     * Find a tag input by its name or associated label.
     *
     * Searches for:
     * 1. Tag input with matching contentDescription
     * 2. Tag input with label text matching name
     * 3. Tag input with associated TextView label nearby
     *
     * @param name The name to search for (case-insensitive)
     * @return TagInputInfo if found, null otherwise
     */
    suspend fun findByName(name: String): TagInputInfo?

    /**
     * Find the currently focused tag input.
     *
     * @return TagInputInfo if a tag input has focus, null otherwise
     */
    suspend fun findFocused(): TagInputInfo?

    // ═══════════════════════════════════════════════════════════════════════════
    // Tag Operations
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Add a new tag to the tag input.
     *
     * @param tagInput The tag input to add to
     * @param tagName The name of the new tag
     * @return Operation result
     */
    suspend fun addTag(tagInput: TagInputInfo, tagName: String): TagInputOperationResult

    /**
     * Remove a tag by its ID.
     *
     * @param tagInput The tag input containing the tag
     * @param tagId The ID of the tag to remove
     * @return Operation result
     */
    suspend fun removeTag(tagInput: TagInputInfo, tagId: String): TagInputOperationResult

    /**
     * Remove the last (most recently added) tag.
     *
     * @param tagInput The tag input to remove from
     * @return Operation result
     */
    suspend fun removeLastTag(tagInput: TagInputInfo): TagInputOperationResult

    /**
     * Clear all removable tags from the tag input.
     *
     * Tags marked as non-removable (locked) should not be cleared.
     *
     * @param tagInput The tag input to clear
     * @return Operation result
     */
    suspend fun clearTags(tagInput: TagInputInfo): TagInputOperationResult

    /**
     * Select/focus a specific tag.
     *
     * @param tagInput The tag input containing the tag
     * @param tagId The ID of the tag to select
     * @return Operation result
     */
    suspend fun selectTag(tagInput: TagInputInfo, tagId: String): TagInputOperationResult

    /**
     * Edit an existing tag (enter edit mode).
     *
     * @param tagInput The tag input containing the tag
     * @param tagId The ID of the tag to edit
     * @return Operation result
     */
    suspend fun editTag(tagInput: TagInputInfo, tagId: String): TagInputOperationResult

    /**
     * Get all current tags in the tag input.
     *
     * @param tagInput The tag input to query
     * @return List of current tags
     */
    suspend fun getTags(tagInput: TagInputInfo): List<TagInfo>
}
