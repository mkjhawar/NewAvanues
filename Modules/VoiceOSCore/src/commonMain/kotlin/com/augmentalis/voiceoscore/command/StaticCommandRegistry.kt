/**
 * StaticCommandRegistry.kt - Predefined static voice commands
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2026-01-05
 *
 * Registry of predefined voice commands that work system-wide,
 * independent of the current screen or app context.
 */
package com.augmentalis.voiceoscore

import com.augmentalis.voiceoscore.CommandActionType

/**
 * Registry of predefined static voice commands.
 *
 * These commands are always available regardless of screen context.
 * They provide system-level voice control functionality.
 *
 * Categories:
 * - Navigation: back, home, recent apps
 * - Media: play, pause, volume
 * - System: settings, notifications
 * - VoiceOS: mute, wake, dictation
 */
object StaticCommandRegistry {

    // ═══════════════════════════════════════════════════════════════════
    // Navigation Commands
    // ═══════════════════════════════════════════════════════════════════

    val navigationCommands = listOf(
        StaticCommand(
            phrases = listOf("go back", "navigate back", "back", "previous screen"),
            actionType = CommandActionType.BACK,
            category = CommandCategory.NAVIGATION,
            description = "Navigate to previous screen"
        ),
        StaticCommand(
            phrases = listOf("go home", "home", "navigate home", "open home"),
            actionType = CommandActionType.HOME,
            category = CommandCategory.NAVIGATION,
            description = "Go to home screen"
        ),
        StaticCommand(
            phrases = listOf("show recent apps", "recent apps", "open recents", "app switcher"),
            actionType = CommandActionType.RECENT_APPS,
            category = CommandCategory.NAVIGATION,
            description = "Show recent apps"
        ),
        StaticCommand(
            phrases = listOf("open app drawer", "app drawer", "all apps"),
            actionType = CommandActionType.APP_DRAWER,
            category = CommandCategory.NAVIGATION,
            description = "Open app drawer"
        ),
        StaticCommand(
            phrases = listOf("scroll down", "page down"),
            actionType = CommandActionType.SCROLL_DOWN,
            category = CommandCategory.NAVIGATION,
            description = "Scroll down"
        ),
        StaticCommand(
            phrases = listOf("scroll up", "page up"),
            actionType = CommandActionType.SCROLL_UP,
            category = CommandCategory.NAVIGATION,
            description = "Scroll up"
        ),
        StaticCommand(
            phrases = listOf("scroll left"),
            actionType = CommandActionType.SCROLL_LEFT,
            category = CommandCategory.NAVIGATION,
            description = "Scroll left"
        ),
        StaticCommand(
            phrases = listOf("scroll right"),
            actionType = CommandActionType.SCROLL_RIGHT,
            category = CommandCategory.NAVIGATION,
            description = "Scroll right"
        )
    )

    // ═══════════════════════════════════════════════════════════════════
    // Media Commands
    // ═══════════════════════════════════════════════════════════════════

    val mediaCommands = listOf(
        StaticCommand(
            phrases = listOf("play music", "play", "resume"),
            actionType = CommandActionType.MEDIA_PLAY,
            category = CommandCategory.MEDIA,
            description = "Play/resume media"
        ),
        StaticCommand(
            phrases = listOf("pause music", "pause", "stop music"),
            actionType = CommandActionType.MEDIA_PAUSE,
            category = CommandCategory.MEDIA,
            description = "Pause media"
        ),
        StaticCommand(
            phrases = listOf("next song", "next track", "skip"),
            actionType = CommandActionType.MEDIA_NEXT,
            category = CommandCategory.MEDIA,
            description = "Next track"
        ),
        StaticCommand(
            phrases = listOf("previous song", "previous track", "go back"),
            actionType = CommandActionType.MEDIA_PREVIOUS,
            category = CommandCategory.MEDIA,
            description = "Previous track"
        ),
        StaticCommand(
            phrases = listOf("increase volume", "volume up", "louder"),
            actionType = CommandActionType.VOLUME_UP,
            category = CommandCategory.MEDIA,
            description = "Increase volume"
        ),
        StaticCommand(
            phrases = listOf("decrease volume", "volume down", "lower volume", "quieter"),
            actionType = CommandActionType.VOLUME_DOWN,
            category = CommandCategory.MEDIA,
            description = "Decrease volume"
        ),
        StaticCommand(
            phrases = listOf("mute volume", "mute", "silence"),
            actionType = CommandActionType.VOLUME_MUTE,
            category = CommandCategory.MEDIA,
            description = "Mute audio"
        )
    )

    // ═══════════════════════════════════════════════════════════════════
    // System Commands
    // ═══════════════════════════════════════════════════════════════════

    val systemCommands = listOf(
        StaticCommand(
            phrases = listOf("open settings", "settings", "show settings", "device settings"),
            actionType = CommandActionType.OPEN_SETTINGS,
            category = CommandCategory.SYSTEM,
            description = "Open system settings"
        ),
        StaticCommand(
            phrases = listOf("show notifications", "notifications", "notification panel"),
            actionType = CommandActionType.NOTIFICATIONS,
            category = CommandCategory.SYSTEM,
            description = "Show notifications"
        ),
        StaticCommand(
            phrases = listOf("clear notifications", "dismiss notifications"),
            actionType = CommandActionType.CLEAR_NOTIFICATIONS,
            category = CommandCategory.SYSTEM,
            description = "Clear all notifications"
        ),
        StaticCommand(
            phrases = listOf("take screenshot", "screenshot", "capture screen"),
            actionType = CommandActionType.SCREENSHOT,
            category = CommandCategory.SYSTEM,
            description = "Take a screenshot"
        ),
        StaticCommand(
            phrases = listOf("turn on flashlight", "flashlight on", "torch on"),
            actionType = CommandActionType.FLASHLIGHT_ON,
            category = CommandCategory.SYSTEM,
            description = "Turn on flashlight"
        ),
        StaticCommand(
            phrases = listOf("turn off flashlight", "flashlight off", "torch off"),
            actionType = CommandActionType.FLASHLIGHT_OFF,
            category = CommandCategory.SYSTEM,
            description = "Turn off flashlight"
        )
    )

    // ═══════════════════════════════════════════════════════════════════
    // VoiceOS Control Commands
    // ═══════════════════════════════════════════════════════════════════

    val voiceOSCommands = listOf(
        StaticCommand(
            phrases = listOf("mute voice", "stop listening", "voice off"),
            actionType = CommandActionType.VOICE_MUTE,
            category = CommandCategory.VOICE_CONTROL,
            description = "Mute voice recognition"
        ),
        StaticCommand(
            phrases = listOf("wake up voice", "start listening", "voice on"),
            actionType = CommandActionType.VOICE_WAKE,
            category = CommandCategory.VOICE_CONTROL,
            description = "Wake voice recognition"
        ),
        StaticCommand(
            phrases = listOf("start dictation", "dictation", "type mode"),
            actionType = CommandActionType.DICTATION_START,
            category = CommandCategory.VOICE_CONTROL,
            description = "Start dictation mode"
        ),
        StaticCommand(
            phrases = listOf("stop dictation", "end dictation", "command mode"),
            actionType = CommandActionType.DICTATION_STOP,
            category = CommandCategory.VOICE_CONTROL,
            description = "Stop dictation mode"
        ),
        StaticCommand(
            phrases = listOf("show voice commands", "what can I say", "help"),
            actionType = CommandActionType.SHOW_COMMANDS,
            category = CommandCategory.VOICE_CONTROL,
            description = "Show available commands"
        ),
        // Numbers Overlay Control
        StaticCommand(
            phrases = listOf("numbers on", "show numbers", "numbers always"),
            actionType = CommandActionType.NUMBERS_ON,
            category = CommandCategory.VOICE_CONTROL,
            description = "Always show numbered badges on screen elements"
        ),
        StaticCommand(
            phrases = listOf("numbers off", "hide numbers", "no numbers"),
            actionType = CommandActionType.NUMBERS_OFF,
            category = CommandCategory.VOICE_CONTROL,
            description = "Never show numbered badges"
        ),
        StaticCommand(
            phrases = listOf("numbers auto", "numbers automatic", "auto numbers"),
            actionType = CommandActionType.NUMBERS_AUTO,
            category = CommandCategory.VOICE_CONTROL,
            description = "Show numbers only for lists (emails, messages, etc.)"
        )
    )

    // ═══════════════════════════════════════════════════════════════════
    // App Launcher Commands
    // ═══════════════════════════════════════════════════════════════════

    val appCommands = listOf(
        StaticCommand(
            phrases = listOf("open browser", "browser", "open web browser"),
            actionType = CommandActionType.OPEN_APP,
            category = CommandCategory.APP_LAUNCH,
            description = "Open web browser",
            metadata = mapOf("app_type" to "browser")
        ),
        StaticCommand(
            phrases = listOf("open camera", "camera", "take photo"),
            actionType = CommandActionType.OPEN_APP,
            category = CommandCategory.APP_LAUNCH,
            description = "Open camera",
            metadata = mapOf("app_type" to "camera")
        ),
        StaticCommand(
            phrases = listOf("open gallery", "gallery", "photos"),
            actionType = CommandActionType.OPEN_APP,
            category = CommandCategory.APP_LAUNCH,
            description = "Open photo gallery",
            metadata = mapOf("app_type" to "gallery")
        ),
        StaticCommand(
            phrases = listOf("open calculator", "calculator"),
            actionType = CommandActionType.OPEN_APP,
            category = CommandCategory.APP_LAUNCH,
            description = "Open calculator",
            metadata = mapOf("app_type" to "calculator")
        ),
        StaticCommand(
            phrases = listOf("open calendar", "calendar"),
            actionType = CommandActionType.OPEN_APP,
            category = CommandCategory.APP_LAUNCH,
            description = "Open calendar",
            metadata = mapOf("app_type" to "calendar")
        ),
        StaticCommand(
            phrases = listOf("open phone", "phone", "dialer"),
            actionType = CommandActionType.OPEN_APP,
            category = CommandCategory.APP_LAUNCH,
            description = "Open phone dialer",
            metadata = mapOf("app_type" to "phone")
        ),
        StaticCommand(
            phrases = listOf("open messages", "messages", "sms"),
            actionType = CommandActionType.OPEN_APP,
            category = CommandCategory.APP_LAUNCH,
            description = "Open messages app",
            metadata = mapOf("app_type" to "messages")
        ),
        StaticCommand(
            phrases = listOf("open contacts", "contacts"),
            actionType = CommandActionType.OPEN_APP,
            category = CommandCategory.APP_LAUNCH,
            description = "Open contacts",
            metadata = mapOf("app_type" to "contacts")
        )
    )

    // ═══════════════════════════════════════════════════════════════════
    // Accessibility Commands (Zoom + Element Interaction)
    // ═══════════════════════════════════════════════════════════════════

    val accessibilityCommands = listOf(
        StaticCommand(
            phrases = listOf("click", "tap", "press"),
            actionType = CommandActionType.CLICK,
            category = CommandCategory.ACCESSIBILITY,
            description = "Click/tap on a named or numbered element"
        ),
        StaticCommand(
            phrases = listOf("long press", "long click", "press and hold", "hold"),
            actionType = CommandActionType.LONG_CLICK,
            category = CommandCategory.ACCESSIBILITY,
            description = "Long press on element"
        ),
        StaticCommand(
            phrases = listOf("zoom in", "magnify", "enlarge"),
            actionType = CommandActionType.ZOOM_IN,
            category = CommandCategory.ACCESSIBILITY,
            description = "Zoom in"
        ),
        StaticCommand(
            phrases = listOf("zoom out", "shrink"),
            actionType = CommandActionType.ZOOM_OUT,
            category = CommandCategory.ACCESSIBILITY,
            description = "Zoom out"
        )
    )

    // ═══════════════════════════════════════════════════════════════════
    // Text & Clipboard Commands
    // ═══════════════════════════════════════════════════════════════════

    val textCommands = listOf(
        StaticCommand(
            phrases = listOf("select all", "highlight all"),
            actionType = CommandActionType.SELECT_ALL,
            category = CommandCategory.TEXT,
            description = "Select all text"
        ),
        StaticCommand(
            phrases = listOf("copy", "copy that", "copy text"),
            actionType = CommandActionType.COPY,
            category = CommandCategory.TEXT,
            description = "Copy selection to clipboard"
        ),
        StaticCommand(
            phrases = listOf("paste", "paste text"),
            actionType = CommandActionType.PASTE,
            category = CommandCategory.TEXT,
            description = "Paste from clipboard"
        ),
        StaticCommand(
            phrases = listOf("cut", "cut text"),
            actionType = CommandActionType.CUT,
            category = CommandCategory.TEXT,
            description = "Cut selection to clipboard"
        ),
        StaticCommand(
            phrases = listOf("undo", "undo that", "take back"),
            actionType = CommandActionType.UNDO,
            category = CommandCategory.TEXT,
            description = "Undo last action"
        ),
        StaticCommand(
            phrases = listOf("redo", "redo that"),
            actionType = CommandActionType.REDO,
            category = CommandCategory.TEXT,
            description = "Redo last undone action"
        ),
        StaticCommand(
            phrases = listOf("delete", "delete that", "erase", "remove"),
            actionType = CommandActionType.DELETE,
            category = CommandCategory.TEXT,
            description = "Delete selected text"
        )
    )

    // ═══════════════════════════════════════════════════════════════════
    // Screen & Display Commands
    // ═══════════════════════════════════════════════════════════════════

    val screenCommands = listOf(
        StaticCommand(
            phrases = listOf("brightness up", "increase brightness", "brighter"),
            actionType = CommandActionType.BRIGHTNESS_UP,
            category = CommandCategory.SYSTEM,
            description = "Increase screen brightness"
        ),
        StaticCommand(
            phrases = listOf("brightness down", "decrease brightness", "dimmer"),
            actionType = CommandActionType.BRIGHTNESS_DOWN,
            category = CommandCategory.SYSTEM,
            description = "Decrease screen brightness"
        ),
        StaticCommand(
            phrases = listOf("lock screen", "lock phone", "lock"),
            actionType = CommandActionType.LOCK_SCREEN,
            category = CommandCategory.SYSTEM,
            description = "Lock the screen"
        ),
        StaticCommand(
            phrases = listOf("rotate screen", "rotate", "change orientation"),
            actionType = CommandActionType.ROTATE_SCREEN,
            category = CommandCategory.SYSTEM,
            description = "Toggle screen rotation"
        ),
        StaticCommand(
            phrases = listOf("toggle wifi", "wifi on", "wifi off", "turn on wifi", "turn off wifi"),
            actionType = CommandActionType.TOGGLE_WIFI,
            category = CommandCategory.SYSTEM,
            description = "Toggle WiFi"
        ),
        StaticCommand(
            phrases = listOf("toggle bluetooth", "bluetooth on", "bluetooth off", "turn on bluetooth", "turn off bluetooth"),
            actionType = CommandActionType.TOGGLE_BLUETOOTH,
            category = CommandCategory.SYSTEM,
            description = "Toggle Bluetooth"
        )
    )

    // ═══════════════════════════════════════════════════════════════════
    // Cursor Commands
    // ═══════════════════════════════════════════════════════════════════

    val cursorCommands = listOf(
        StaticCommand(
            phrases = listOf("show cursor", "cursor on", "enable cursor"),
            actionType = CommandActionType.CURSOR_SHOW,
            category = CommandCategory.VOICE_CONTROL,
            description = "Show the voice cursor overlay"
        ),
        StaticCommand(
            phrases = listOf("hide cursor", "cursor off", "disable cursor"),
            actionType = CommandActionType.CURSOR_HIDE,
            category = CommandCategory.VOICE_CONTROL,
            description = "Hide the voice cursor overlay"
        ),
        StaticCommand(
            phrases = listOf("cursor click", "click here"),
            actionType = CommandActionType.CURSOR_CLICK,
            category = CommandCategory.VOICE_CONTROL,
            description = "Click at cursor position"
        )
    )

    // ═══════════════════════════════════════════════════════════════════
    // Reading / TTS Commands
    // ═══════════════════════════════════════════════════════════════════

    val readingCommands = listOf(
        StaticCommand(
            phrases = listOf("read screen", "read aloud", "read this", "read page"),
            actionType = CommandActionType.READ_SCREEN,
            category = CommandCategory.ACCESSIBILITY,
            description = "Read screen content aloud"
        ),
        StaticCommand(
            phrases = listOf("stop reading", "stop", "quiet", "be quiet"),
            actionType = CommandActionType.STOP_READING,
            category = CommandCategory.ACCESSIBILITY,
            description = "Stop reading aloud"
        )
    )

    // ═══════════════════════════════════════════════════════════════════
    // Input Commands
    // ═══════════════════════════════════════════════════════════════════

    val inputCommands = listOf(
        StaticCommand(
            phrases = listOf("show keyboard", "open keyboard", "keyboard"),
            actionType = CommandActionType.SHOW_KEYBOARD,
            category = CommandCategory.INPUT,
            description = "Show on-screen keyboard"
        ),
        StaticCommand(
            phrases = listOf("hide keyboard", "close keyboard", "dismiss keyboard"),
            actionType = CommandActionType.HIDE_KEYBOARD,
            category = CommandCategory.INPUT,
            description = "Hide on-screen keyboard"
        )
    )

    // ═══════════════════════════════════════════════════════════════════
    // App Control Commands
    // ═══════════════════════════════════════════════════════════════════

    val appControlCommands = listOf(
        StaticCommand(
            phrases = listOf("close app", "close this", "exit app", "quit"),
            actionType = CommandActionType.CLOSE_APP,
            category = CommandCategory.APP_CONTROL,
            description = "Close the current app"
        )
    )

    // ═══════════════════════════════════════════════════════════════════
    // Browser Commands
    // ═══════════════════════════════════════════════════════════════════

    val browserCommands = listOf(
        StaticCommand(
            phrases = listOf("retrain page", "rescan page", "rescan"),
            actionType = CommandActionType.RETRAIN_PAGE,
            category = CommandCategory.BROWSER,
            description = "Force re-scrape current web page and regenerate voice commands"
        ),
        // Browser Navigation
        StaticCommand(
            phrases = listOf("go back", "browser back"),
            actionType = CommandActionType.PAGE_BACK,
            category = CommandCategory.BROWSER,
            description = "Navigate to previous page in browser history"
        ),
        StaticCommand(
            phrases = listOf("go forward", "browser forward"),
            actionType = CommandActionType.PAGE_FORWARD,
            category = CommandCategory.BROWSER,
            description = "Navigate to next page in browser history"
        ),
        StaticCommand(
            phrases = listOf("refresh page", "reload page"),
            actionType = CommandActionType.PAGE_REFRESH,
            category = CommandCategory.BROWSER,
            description = "Reload the current web page"
        ),
        // Page Scrolling
        StaticCommand(
            phrases = listOf("go to top", "top of page", "scroll to top"),
            actionType = CommandActionType.SCROLL_TO_TOP,
            category = CommandCategory.BROWSER,
            description = "Scroll to the top of the page"
        ),
        StaticCommand(
            phrases = listOf("go to bottom", "bottom of page", "scroll to bottom"),
            actionType = CommandActionType.SCROLL_TO_BOTTOM,
            category = CommandCategory.BROWSER,
            description = "Scroll to the bottom of the page"
        ),
        // Form Navigation
        StaticCommand(
            phrases = listOf("next field", "tab forward"),
            actionType = CommandActionType.TAB_NEXT,
            category = CommandCategory.BROWSER,
            description = "Focus the next focusable element"
        ),
        StaticCommand(
            phrases = listOf("previous field", "tab back"),
            actionType = CommandActionType.TAB_PREV,
            category = CommandCategory.BROWSER,
            description = "Focus the previous focusable element"
        ),
        StaticCommand(
            phrases = listOf("submit form", "submit"),
            actionType = CommandActionType.SUBMIT_FORM,
            category = CommandCategory.BROWSER,
            description = "Submit the current form"
        ),
        // Gestures
        StaticCommand(
            phrases = listOf("swipe left"),
            actionType = CommandActionType.SWIPE_LEFT,
            category = CommandCategory.BROWSER,
            description = "Swipe left on the page"
        ),
        StaticCommand(
            phrases = listOf("swipe right"),
            actionType = CommandActionType.SWIPE_RIGHT,
            category = CommandCategory.BROWSER,
            description = "Swipe right on the page"
        ),
        StaticCommand(
            phrases = listOf("swipe up"),
            actionType = CommandActionType.SWIPE_UP,
            category = CommandCategory.BROWSER,
            description = "Swipe up on the page"
        ),
        StaticCommand(
            phrases = listOf("swipe down"),
            actionType = CommandActionType.SWIPE_DOWN,
            category = CommandCategory.BROWSER,
            description = "Swipe down on the page"
        ),
        StaticCommand(
            phrases = listOf("grab", "grab this"),
            actionType = CommandActionType.GRAB,
            category = CommandCategory.BROWSER,
            description = "Grab/start dragging an element"
        ),
        StaticCommand(
            phrases = listOf("release", "let go", "drop"),
            actionType = CommandActionType.RELEASE,
            category = CommandCategory.BROWSER,
            description = "Release a grabbed element"
        ),
        StaticCommand(
            phrases = listOf("rotate left"),
            actionType = CommandActionType.ROTATE,
            category = CommandCategory.BROWSER,
            description = "Rotate element left",
            metadata = mapOf("direction" to "left")
        ),
        StaticCommand(
            phrases = listOf("rotate right"),
            actionType = CommandActionType.ROTATE,
            category = CommandCategory.BROWSER,
            description = "Rotate element right",
            metadata = mapOf("direction" to "right")
        ),
        StaticCommand(
            phrases = listOf("double tap", "double click"),
            actionType = CommandActionType.DOUBLE_CLICK,
            category = CommandCategory.BROWSER,
            description = "Double-click/double-tap an element"
        )
    )

    // ═══════════════════════════════════════════════════════════════════
    // Registry Access Methods
    // ═══════════════════════════════════════════════════════════════════

    /**
     * Get all static commands
     */
    fun all(): List<StaticCommand> =
        navigationCommands +
        mediaCommands +
        systemCommands +
        screenCommands +
        voiceOSCommands +
        cursorCommands +
        appCommands +
        appControlCommands +
        accessibilityCommands +
        textCommands +
        readingCommands +
        inputCommands +
        browserCommands

    /**
     * Get all phrase strings (for speech engine vocabulary)
     */
    fun allPhrases(): List<String> = all().flatMap { it.phrases }

    /**
     * Get commands by category
     */
    fun byCategory(category: CommandCategory): List<StaticCommand> =
        all().filter { it.category == category }

    /**
     * Find command matching phrase
     */
    fun findByPhrase(phrase: String): StaticCommand? {
        val normalized = phrase.lowercase().trim()
        return all().find { cmd ->
            cmd.phrases.any { it.lowercase() == normalized }
        }
    }

    /**
     * Get command count
     */
    val commandCount: Int get() = all().size

    /**
     * Get phrase count
     */
    val phraseCount: Int get() = allPhrases().size

    // ═══════════════════════════════════════════════════════════════════
    // NLU/LLM Integration - QuantizedCommand Export
    // ═══════════════════════════════════════════════════════════════════

    /**
     * Get all static commands as QuantizedCommand objects for NLU/LLM.
     *
     * Each phrase variant becomes a separate QuantizedCommand with:
     * - targetVuid = null (system commands, no element target)
     * - confidence = 1.0 (always available)
     * - metadata includes category, description, source
     *
     * @return List of QuantizedCommand for all static commands
     */
    fun allAsQuantized(): List<QuantizedCommand> {
        return all().flatMap { it.toQuantizedCommands() }
    }

    /**
     * Get static commands by category as QuantizedCommand.
     *
     * @param category Command category
     * @return List of QuantizedCommand for category
     */
    fun byCategoryAsQuantized(category: CommandCategory): List<QuantizedCommand> {
        return byCategory(category).flatMap { it.toQuantizedCommands() }
    }

    /**
     * Export all commands in AVU CMD format for NLU/LLM.
     *
     * Format: CMD:uuid:trigger:action:element_uuid:confidence
     * Static commands have empty element_uuid (system commands).
     *
     * @return Multi-line string with all commands in CMD format
     */
    fun toAvuFormat(): String {
        return allAsQuantized().joinToString("\n") { it.toCmdLine() }
    }

    /**
     * Get a concise NLU schema describing available commands.
     *
     * Returns a structured format suitable for LLM prompts:
     * - Category grouping
     * - Primary phrase + action type
     * - Description for context
     */
    fun toNluSchema(): String {
        return buildString {
            appendLine("# Static Voice Commands")
            appendLine()

            CommandCategory.entries.forEach { category ->
                val commands = byCategory(category)
                if (commands.isNotEmpty()) {
                    appendLine("## ${category.name}")
                    commands.forEach { cmd ->
                        appendLine("- ${cmd.primaryPhrase}: ${cmd.actionType.name} - ${cmd.description}")
                        if (cmd.phrases.size > 1) {
                            appendLine("  Aliases: ${cmd.phrases.drop(1).joinToString(", ")}")
                        }
                    }
                    appendLine()
                }
            }
        }
    }
}

/**
 * Represents a static/predefined voice command
 */
data class StaticCommand(
    /**
     * Alternative phrases that trigger this command
     */
    val phrases: List<String>,

    /**
     * Action type to execute
     */
    val actionType: CommandActionType,

    /**
     * Command category for organization
     */
    val category: CommandCategory,

    /**
     * Human-readable description
     */
    val description: String,

    /**
     * Additional metadata for command execution
     */
    val metadata: Map<String, String> = emptyMap()
) {
    /**
     * Primary phrase (first in list)
     */
    val primaryPhrase: String get() = phrases.first()

    /**
     * Convert to QuantizedCommand objects for NLU/LLM.
     *
     * Creates one QuantizedCommand per phrase variant.
     * Static commands have:
     * - targetVuid = null (system command, no element target)
     * - confidence = 1.0 (always available)
     * - uuid = static__{category}__{normalized_phrase}
     *
     * @return List of QuantizedCommand for each phrase
     */
    fun toQuantizedCommands(): List<QuantizedCommand> {
        return phrases.map { phrase ->
            val normalizedPhrase = phrase.lowercase().replace(" ", "_")
            val commandAvid = "static__${category.name.lowercase()}__$normalizedPhrase"

            QuantizedCommand(
                avid = commandAvid,
                phrase = phrase,
                actionType = actionType,
                targetAvid = null, // Static commands have no target element
                confidence = 1.0f, // Static commands are always available
                metadata = metadata + mapOf(
                    "source" to "static",
                    "category" to category.name,
                    "description" to description,
                    "primary_phrase" to primaryPhrase
                )
            )
        }
    }

    /**
     * Convert primary phrase to a single QuantizedCommand.
     *
     * @return QuantizedCommand for primary phrase only
     */
    fun toQuantizedCommand(): QuantizedCommand {
        val normalizedPhrase = primaryPhrase.lowercase().replace(" ", "_")
        val commandAvid = "static__${category.name.lowercase()}__$normalizedPhrase"

        return QuantizedCommand(
            avid = commandAvid,
            phrase = primaryPhrase,
            actionType = actionType,
            targetAvid = null,
            confidence = 1.0f,
            metadata = metadata + mapOf(
                "source" to "static",
                "category" to category.name,
                "description" to description,
                "aliases" to phrases.drop(1).joinToString("|")
            )
        )
    }
}
