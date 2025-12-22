/**
 * ExplorationCommand.kt - Exploration command data class
 *
 * Parcelable data class representing an exploration command.
 * Passed from LearnApp to JIT service for execution.
 *
 * Author: Manoj Jhawar
 * Created: 2025-12-11
 * Related: VoiceOS-LearnApp-DualEdition-Spec-51211-V1.md
 *
 * @since 2.0.0 (LearnApp Dual-Edition)
 */

package com.augmentalis.jitlearning

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

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

/**
 * Scroll direction.
 */
enum class ScrollDirection {
    UP,
    DOWN,
    LEFT,
    RIGHT
}

/**
 * Exploration Command
 *
 * Represents a command to be executed by the JIT service.
 * Commands are sent from LearnApp exploration engine.
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
 * @property elementUuid Target element UUID (for element-specific commands)
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
@Parcelize
data class ExplorationCommand(
    val type: CommandType,
    val elementUuid: String = "",
    val direction: ScrollDirection = ScrollDirection.DOWN,
    val distance: Int = 0,
    val text: String = "",
    val startX: Int = 0,
    val startY: Int = 0,
    val endX: Int = 0,
    val endY: Int = 0,
    val timeout: Long = 0,
    val timestamp: Long = System.currentTimeMillis()
) : Parcelable {

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
                elementUuid.isNotBlank()

            CommandType.SET_TEXT ->
                elementUuid.isNotBlank() && text.isNotEmpty()

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
            CommandType.CLICK -> "Click on $elementUuid"
            CommandType.LONG_CLICK -> "Long press on $elementUuid"
            CommandType.SCROLL -> "Scroll ${direction.name.lowercase()} ${distance}px"
            CommandType.SWIPE -> "Swipe from ($startX,$startY) to ($endX,$endY)"
            CommandType.SET_TEXT -> "Enter text '$text' in $elementUuid"
            CommandType.BACK -> "Press back"
            CommandType.HOME -> "Press home"
            CommandType.FOCUS -> "Focus on $elementUuid"
            CommandType.CLEAR_TEXT -> "Clear text in $elementUuid"
            CommandType.EXPAND -> "Expand $elementUuid"
            CommandType.SELECT -> "Select $elementUuid"
        }
    }

    companion object {
        /**
         * Create click command.
         */
        fun click(elementUuid: String): ExplorationCommand {
            return ExplorationCommand(
                type = CommandType.CLICK,
                elementUuid = elementUuid
            )
        }

        /**
         * Create long click command.
         */
        fun longClick(elementUuid: String): ExplorationCommand {
            return ExplorationCommand(
                type = CommandType.LONG_CLICK,
                elementUuid = elementUuid
            )
        }

        /**
         * Create scroll command.
         */
        fun scroll(direction: ScrollDirection, distance: Int = 500): ExplorationCommand {
            return ExplorationCommand(
                type = CommandType.SCROLL,
                direction = direction,
                distance = distance
            )
        }

        /**
         * Create swipe command.
         */
        fun swipe(startX: Int, startY: Int, endX: Int, endY: Int): ExplorationCommand {
            return ExplorationCommand(
                type = CommandType.SWIPE,
                startX = startX,
                startY = startY,
                endX = endX,
                endY = endY
            )
        }

        /**
         * Create set text command.
         */
        fun setText(elementUuid: String, text: String): ExplorationCommand {
            return ExplorationCommand(
                type = CommandType.SET_TEXT,
                elementUuid = elementUuid,
                text = text
            )
        }

        /**
         * Create back command.
         */
        fun back(): ExplorationCommand {
            return ExplorationCommand(type = CommandType.BACK)
        }

        /**
         * Create home command.
         */
        fun home(): ExplorationCommand {
            return ExplorationCommand(type = CommandType.HOME)
        }

        /**
         * Create focus command.
         */
        fun focus(elementUuid: String): ExplorationCommand {
            return ExplorationCommand(
                type = CommandType.FOCUS,
                elementUuid = elementUuid
            )
        }

        /**
         * Create clear text command.
         */
        fun clearText(elementUuid: String): ExplorationCommand {
            return ExplorationCommand(
                type = CommandType.CLEAR_TEXT,
                elementUuid = elementUuid
            )
        }

        /**
         * Create expand command.
         */
        fun expand(elementUuid: String): ExplorationCommand {
            return ExplorationCommand(
                type = CommandType.EXPAND,
                elementUuid = elementUuid
            )
        }
    }
}
