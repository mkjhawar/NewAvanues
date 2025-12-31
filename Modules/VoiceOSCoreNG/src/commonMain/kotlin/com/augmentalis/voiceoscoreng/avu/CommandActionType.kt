package com.augmentalis.voiceoscoreng.avu

/**
 * Command Action Type - Action types for voice commands.
 *
 * Defines the action a command performs on a UI element.
 */
enum class CommandActionType {
    /** Click/tap action */
    CLICK,

    /** Long press/hold action */
    LONG_CLICK,

    /** Text input action */
    TYPE,

    /** Navigation action (screen transition) */
    NAVIGATE,

    /** Custom/specialized action */
    CUSTOM;

    companion object {
        /**
         * Parse action type from string.
         *
         * @param value Action type string
         * @return CommandActionType, defaults to CLICK if invalid
         */
        fun fromString(value: String): CommandActionType {
            return try {
                valueOf(value.uppercase())
            } catch (e: Exception) {
                CLICK
            }
        }
    }
}
