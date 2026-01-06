package com.augmentalis.voiceoscoreng.avu

/**
 * Command Action Type - Action types for voice commands.
 *
 * Defines the action a command performs on a UI element or system.
 *
 * Categories:
 * - Element Actions: CLICK, LONG_CLICK, TYPE, SCROLL_*
 * - Navigation: BACK, HOME, RECENT_APPS, APP_DRAWER, NAVIGATE
 * - Media: MEDIA_*, VOLUME_*
 * - System: OPEN_SETTINGS, NOTIFICATIONS, SCREENSHOT, FLASHLIGHT_*
 * - VoiceOS: VOICE_*, DICTATION_*, SHOW_COMMANDS
 * - App Launch: OPEN_APP
 */
enum class CommandActionType {
    // ═══════════════════════════════════════════════════════════════════
    // Element Actions (UI interaction)
    // ═══════════════════════════════════════════════════════════════════

    /** Click/tap action */
    CLICK,

    /** Long press/hold action */
    LONG_CLICK,

    /** Text input action */
    TYPE,

    /** Focus an element */
    FOCUS,

    // ═══════════════════════════════════════════════════════════════════
    // Scroll Actions
    // ═══════════════════════════════════════════════════════════════════

    /** Scroll down */
    SCROLL_DOWN,

    /** Scroll up */
    SCROLL_UP,

    /** Scroll left */
    SCROLL_LEFT,

    /** Scroll right */
    SCROLL_RIGHT,

    // ═══════════════════════════════════════════════════════════════════
    // Navigation Actions
    // ═══════════════════════════════════════════════════════════════════

    /** Navigate to previous screen */
    BACK,

    /** Navigate to home screen */
    HOME,

    /** Show recent apps */
    RECENT_APPS,

    /** Open app drawer */
    APP_DRAWER,

    /** Generic navigation action (screen transition) */
    NAVIGATE,

    // ═══════════════════════════════════════════════════════════════════
    // Media Actions
    // ═══════════════════════════════════════════════════════════════════

    /** Play/resume media */
    MEDIA_PLAY,

    /** Pause media */
    MEDIA_PAUSE,

    /** Next track */
    MEDIA_NEXT,

    /** Previous track */
    MEDIA_PREVIOUS,

    /** Increase volume */
    VOLUME_UP,

    /** Decrease volume */
    VOLUME_DOWN,

    /** Mute audio */
    VOLUME_MUTE,

    // ═══════════════════════════════════════════════════════════════════
    // System Actions
    // ═══════════════════════════════════════════════════════════════════

    /** Open system settings */
    OPEN_SETTINGS,

    /** Show notification panel */
    NOTIFICATIONS,

    /** Clear all notifications */
    CLEAR_NOTIFICATIONS,

    /** Take screenshot */
    SCREENSHOT,

    /** Turn flashlight on */
    FLASHLIGHT_ON,

    /** Turn flashlight off */
    FLASHLIGHT_OFF,

    // ═══════════════════════════════════════════════════════════════════
    // VoiceOS Control Actions
    // ═══════════════════════════════════════════════════════════════════

    /** Mute voice recognition */
    VOICE_MUTE,

    /** Wake/unmute voice recognition */
    VOICE_WAKE,

    /** Start dictation mode */
    DICTATION_START,

    /** Stop dictation mode */
    DICTATION_STOP,

    /** Show available voice commands */
    SHOW_COMMANDS,

    // ═══════════════════════════════════════════════════════════════════
    // App Actions
    // ═══════════════════════════════════════════════════════════════════

    /** Open an app (requires metadata) */
    OPEN_APP,

    /** Close current app */
    CLOSE_APP,

    // ═══════════════════════════════════════════════════════════════════
    // Custom
    // ═══════════════════════════════════════════════════════════════════

    /** Custom/specialized action */
    CUSTOM;

    /**
     * Check if this is an element interaction action
     */
    fun isElementAction(): Boolean = this in listOf(
        CLICK, LONG_CLICK, TYPE, FOCUS,
        SCROLL_DOWN, SCROLL_UP, SCROLL_LEFT, SCROLL_RIGHT
    )

    /**
     * Check if this is a system-level action
     */
    fun isSystemAction(): Boolean = this in listOf(
        BACK, HOME, RECENT_APPS, APP_DRAWER,
        OPEN_SETTINGS, NOTIFICATIONS, CLEAR_NOTIFICATIONS,
        SCREENSHOT, FLASHLIGHT_ON, FLASHLIGHT_OFF,
        OPEN_APP, CLOSE_APP
    )

    /**
     * Check if this is a media action
     */
    fun isMediaAction(): Boolean = this in listOf(
        MEDIA_PLAY, MEDIA_PAUSE, MEDIA_NEXT, MEDIA_PREVIOUS,
        VOLUME_UP, VOLUME_DOWN, VOLUME_MUTE
    )

    /**
     * Check if this is a VoiceOS control action
     */
    fun isVoiceOSAction(): Boolean = this in listOf(
        VOICE_MUTE, VOICE_WAKE, DICTATION_START, DICTATION_STOP, SHOW_COMMANDS
    )

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
