/**
 * ExplorationCommand.kt - Exploration command data class
 *
 * Cross-platform data class representing an exploration command.
 * Migrated from JITLearning library for KMP compatibility.
 *
 * Copyright (c) 2026 Augmentalis. All rights reserved.
 * Author: Manoj Jhawar
 * Created: 2026-01-16
 * Migrated from: JITLearning/ExplorationCommand.kt
 *
 * @since 3.0.0 (KMP Migration)
 */

package com.augmentalis.commandmanager

/**
 * Exploration command type.
 */
enum class CommandType {
    /** Click on element */
    CLICK,

    /** Long press on element */
    LONG_CLICK,

    /** Scroll in direction */
    SCROLL,

    /** Swipe gesture */
    SWIPE,

    /** Enter text in field */
    SET_TEXT,

    /** Press back button */
    BACK,

    /** Press home button */
    HOME,

    /** Focus on element */
    FOCUS,

    /** Clear text field */
    CLEAR_TEXT,

    /** Expand/collapse element */
    EXPAND,

    /** Select item */
    SELECT
}

// ScrollDirection is defined in DirectionTypes.kt

/**
 * Exploration Command
 *
 * Represents a command to be executed by the JIT service or exploration engine.
 * Cross-platform model for UI automation commands.
 *
 * ## Usage - Click:
 * ```kotlin
 * val cmd = ExplorationCommand.click("btn-001")
 * jitService.performAction(cmd)
 * ```
 *
 * ## Usage - Scroll:
 * ```kotlin
 * val cmd = ExplorationCommand.scroll(ScrollDirection.DOWN, 500)
 * jitService.performAction(cmd)
 * ```
 *
 * ## Usage - Set Text:
 * ```kotlin
 * val cmd = ExplorationCommand.setText("input-001", "Search query")
 * jitService.performAction(cmd)
 * ```
 *
 * @property type Command type
 * @property elementAvid Target element AVID (for element-specific commands)
 * @property direction Scroll direction (for SCROLL command)
 * @property distance Scroll/swipe distance in pixels
 * @property text Text to enter (for SET_TEXT command)
 * @property startX Swipe start X (for SWIPE command)
 * @property startY Swipe start Y (for SWIPE command)
 * @property endX Swipe end X (for SWIPE command)
 * @property endY Swipe end Y (for SWIPE command)
 * @property timeout Timeout in milliseconds (0 = default)
 * @property timestamp When command was created
 */
data class ExplorationCommand(
    val type: CommandType,
    val elementAvid: String = "",
    val direction: ScrollDirection = ScrollDirection.DOWN,
    val distance: Int = 0,
    val text: String = "",
    val startX: Int = 0,
    val startY: Int = 0,
    val endX: Int = 0,
    val endY: Int = 0,
    val timeout: Long = 0,
    val timestamp: Long = 0
) {

    /**
     * Check if this is an element-specific command.
     */
    fun requiresElement(): Boolean {
        return type in listOf(
            CommandType.CLICK,
            CommandType.LONG_CLICK,
            CommandType.SET_TEXT,
            CommandType.FOCUS,
            CommandType.CLEAR_TEXT,
            CommandType.EXPAND,
            CommandType.SELECT
        )
    }

    /**
     * Validate command has required fields.
     */
    fun isValid(): Boolean {
        return when (type) {
            CommandType.CLICK, CommandType.LONG_CLICK, CommandType.FOCUS,
            CommandType.CLEAR_TEXT, CommandType.EXPAND, CommandType.SELECT ->
                elementAvid.isNotBlank()

            CommandType.SET_TEXT ->
                elementAvid.isNotBlank() && text.isNotEmpty()

            CommandType.SWIPE ->
                startX != endX || startY != endY

            CommandType.SCROLL, CommandType.BACK, CommandType.HOME ->
                true
        }
    }

    /**
     * Get human-readable description.
     */
    fun getDescription(): String {
        return when (type) {
            CommandType.CLICK -> "Click on $elementAvid"
            CommandType.LONG_CLICK -> "Long press on $elementAvid"
            CommandType.SCROLL -> "Scroll ${direction.name.lowercase()} ${distance}px"
            CommandType.SWIPE -> "Swipe from ($startX,$startY) to ($endX,$endY)"
            CommandType.SET_TEXT -> "Enter text '$text' in $elementAvid"
            CommandType.BACK -> "Press back"
            CommandType.HOME -> "Press home"
            CommandType.FOCUS -> "Focus on $elementAvid"
            CommandType.CLEAR_TEXT -> "Clear text in $elementAvid"
            CommandType.EXPAND -> "Expand $elementAvid"
            CommandType.SELECT -> "Select $elementAvid"
        }
    }

    companion object {
        /**
         * Create click command.
         */
        fun click(elementAvid: String, timestamp: Long = 0): ExplorationCommand {
            return ExplorationCommand(
                type = CommandType.CLICK,
                elementAvid = elementAvid,
                timestamp = timestamp
            )
        }

        /**
         * Create long click command.
         */
        fun longClick(elementAvid: String, timestamp: Long = 0): ExplorationCommand {
            return ExplorationCommand(
                type = CommandType.LONG_CLICK,
                elementAvid = elementAvid,
                timestamp = timestamp
            )
        }

        /**
         * Create scroll command.
         */
        fun scroll(direction: ScrollDirection, distance: Int = 500, timestamp: Long = 0): ExplorationCommand {
            return ExplorationCommand(
                type = CommandType.SCROLL,
                direction = direction,
                distance = distance,
                timestamp = timestamp
            )
        }

        /**
         * Create swipe command.
         */
        fun swipe(startX: Int, startY: Int, endX: Int, endY: Int, timestamp: Long = 0): ExplorationCommand {
            return ExplorationCommand(
                type = CommandType.SWIPE,
                startX = startX,
                startY = startY,
                endX = endX,
                endY = endY,
                timestamp = timestamp
            )
        }

        /**
         * Create set text command.
         */
        fun setText(elementAvid: String, text: String, timestamp: Long = 0): ExplorationCommand {
            return ExplorationCommand(
                type = CommandType.SET_TEXT,
                elementAvid = elementAvid,
                text = text,
                timestamp = timestamp
            )
        }

        /**
         * Create back command.
         */
        fun back(timestamp: Long = 0): ExplorationCommand {
            return ExplorationCommand(type = CommandType.BACK, timestamp = timestamp)
        }

        /**
         * Create home command.
         */
        fun home(timestamp: Long = 0): ExplorationCommand {
            return ExplorationCommand(type = CommandType.HOME, timestamp = timestamp)
        }

        /**
         * Create focus command.
         */
        fun focus(elementAvid: String, timestamp: Long = 0): ExplorationCommand {
            return ExplorationCommand(
                type = CommandType.FOCUS,
                elementAvid = elementAvid,
                timestamp = timestamp
            )
        }

        /**
         * Create clear text command.
         */
        fun clearText(elementAvid: String, timestamp: Long = 0): ExplorationCommand {
            return ExplorationCommand(
                type = CommandType.CLEAR_TEXT,
                elementAvid = elementAvid,
                timestamp = timestamp
            )
        }

        /**
         * Create expand command.
         */
        fun expand(elementAvid: String, timestamp: Long = 0): ExplorationCommand {
            return ExplorationCommand(
                type = CommandType.EXPAND,
                elementAvid = elementAvid,
                timestamp = timestamp
            )
        }

        /**
         * Create select command.
         */
        fun select(elementAvid: String, timestamp: Long = 0): ExplorationCommand {
            return ExplorationCommand(
                type = CommandType.SELECT,
                elementAvid = elementAvid,
                timestamp = timestamp
            )
        }
    }
}
