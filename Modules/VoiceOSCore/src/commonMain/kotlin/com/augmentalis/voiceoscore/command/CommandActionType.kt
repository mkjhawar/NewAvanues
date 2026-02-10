package com.augmentalis.voiceoscore

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

    /** Tap action (alias for CLICK) */
    TAP,

    /** Long press/hold action */
    LONG_CLICK,

    /** Execute/run action (generic) */
    EXECUTE,

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

    /** Generic scroll (direction determined by context) */
    SCROLL,

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

    /** Numbers overlay: always ON */
    NUMBERS_ON,

    /** Numbers overlay: always OFF */
    NUMBERS_OFF,

    /** Numbers overlay: AUTO (show for lists) */
    NUMBERS_AUTO,

    // ═══════════════════════════════════════════════════════════════════
    // App Actions
    // ═══════════════════════════════════════════════════════════════════

    /** Open an app (requires metadata) */
    OPEN_APP,

    /** Close current app */
    CLOSE_APP,

    // ═══════════════════════════════════════════════════════════════════
    // Text/Clipboard Actions
    // ═══════════════════════════════════════════════════════════════════

    /** Select all text */
    SELECT_ALL,

    /** Copy selection to clipboard */
    COPY,

    /** Paste from clipboard */
    PASTE,

    /** Cut selection to clipboard */
    CUT,

    /** Undo last action */
    UNDO,

    /** Redo last undone action */
    REDO,

    /** Delete selected text or element */
    DELETE,

    // ═══════════════════════════════════════════════════════════════════
    // Screen & Display Actions
    // ═══════════════════════════════════════════════════════════════════

    /** Increase screen brightness */
    BRIGHTNESS_UP,

    /** Decrease screen brightness */
    BRIGHTNESS_DOWN,

    /** Lock screen */
    LOCK_SCREEN,

    /** Rotate screen orientation */
    ROTATE_SCREEN,

    /** Zoom in */
    ZOOM_IN,

    /** Zoom out */
    ZOOM_OUT,

    // ═══════════════════════════════════════════════════════════════════
    // Connectivity Actions
    // ═══════════════════════════════════════════════════════════════════

    /** Toggle WiFi on/off */
    TOGGLE_WIFI,

    /** Toggle Bluetooth on/off */
    TOGGLE_BLUETOOTH,

    // ═══════════════════════════════════════════════════════════════════
    // Cursor Actions
    // ═══════════════════════════════════════════════════════════════════

    /** Show voice cursor */
    CURSOR_SHOW,

    /** Hide voice cursor */
    CURSOR_HIDE,

    /** Cursor click at current position */
    CURSOR_CLICK,

    // ═══════════════════════════════════════════════════════════════════
    // Reading/TTS Actions
    // ═══════════════════════════════════════════════════════════════════

    /** Read screen content aloud */
    READ_SCREEN,

    /** Stop reading */
    STOP_READING,

    // ═══════════════════════════════════════════════════════════════════
    // Input Actions
    // ═══════════════════════════════════════════════════════════════════

    /** Show on-screen keyboard */
    SHOW_KEYBOARD,

    /** Hide on-screen keyboard */
    HIDE_KEYBOARD,

    // ═══════════════════════════════════════════════════════════════════
    // Custom
    // ═══════════════════════════════════════════════════════════════════

    // ═══════════════════════════════════════════════════════════════════
    // Browser Actions
    // ═══════════════════════════════════════════════════════════════════

    /** Force re-scrape of current web page (invalidate cache + rescrape DOM) */
    RETRAIN_PAGE,

    /** Custom/specialized action */
    CUSTOM,

    /** Macro: sequential chain of actions */
    MACRO;

    /**
     * Check if this is an element interaction action
     */
    fun isElementAction(): Boolean = this in listOf(
        CLICK, TAP, LONG_CLICK, TYPE, FOCUS, EXECUTE,
        SCROLL_DOWN, SCROLL_UP, SCROLL_LEFT, SCROLL_RIGHT, SCROLL
    )

    /**
     * Check if this is a system-level action
     */
    fun isSystemAction(): Boolean = this in listOf(
        BACK, HOME, RECENT_APPS, APP_DRAWER,
        OPEN_SETTINGS, NOTIFICATIONS, CLEAR_NOTIFICATIONS,
        SCREENSHOT, FLASHLIGHT_ON, FLASHLIGHT_OFF,
        BRIGHTNESS_UP, BRIGHTNESS_DOWN, LOCK_SCREEN, ROTATE_SCREEN,
        TOGGLE_WIFI, TOGGLE_BLUETOOTH,
        OPEN_APP, CLOSE_APP
    )

    /**
     * Check if this is a text/clipboard action
     */
    fun isTextAction(): Boolean = this in listOf(
        SELECT_ALL, COPY, PASTE, CUT, UNDO, REDO, DELETE
    )

    /**
     * Check if this is a cursor action
     */
    fun isCursorAction(): Boolean = this in listOf(
        CURSOR_SHOW, CURSOR_HIDE, CURSOR_CLICK
    )

    /**
     * Check if this is a reading/TTS action
     */
    fun isReadingAction(): Boolean = this in listOf(
        READ_SCREEN, STOP_READING
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
        VOICE_MUTE, VOICE_WAKE, DICTATION_START, DICTATION_STOP, SHOW_COMMANDS,
        NUMBERS_ON, NUMBERS_OFF, NUMBERS_AUTO
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
