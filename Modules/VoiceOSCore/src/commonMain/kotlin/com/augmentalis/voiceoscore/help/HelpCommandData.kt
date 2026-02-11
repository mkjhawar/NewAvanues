/**
 * HelpCommandData.kt - Data model for voice command help screen
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2026-01-29
 *
 * Provides structured data for the help screen, including command categories,
 * individual commands with variations, and quick reference data.
 */
package com.augmentalis.voiceoscore.help

/**
 * Represents a single voice command with its variations.
 *
 * @property primaryPhrase The main command phrase (e.g., "scroll down")
 * @property variations Alternative ways to say the same command
 * @property description Brief description of what the command does
 * @property actionResult The result/feedback after executing (e.g., "Scrolls page down")
 */
data class HelpCommand(
    val primaryPhrase: String,
    val variations: List<String> = emptyList(),
    val description: String,
    val actionResult: String
) {
    /**
     * All phrases including primary and variations.
     */
    val allPhrases: List<String>
        get() = listOf(primaryPhrase) + variations
}

/**
 * Represents a category of commands in the help screen.
 *
 * @property id Unique identifier for the category
 * @property title Display title (e.g., "Navigation")
 * @property iconName Icon identifier (Material icon name)
 * @property commands List of commands in this category
 * @property color Optional color for the category card (hex string)
 */
data class HelpCategory(
    val id: String,
    val title: String,
    val iconName: String,
    val commands: List<HelpCommand>,
    val color: String? = null
) {
    /**
     * Number of commands in this category.
     */
    val commandCount: Int get() = commands.size

    /**
     * Preview text showing first few commands.
     */
    val previewText: String
        get() = commands.take(3).joinToString(", ") { "\"${it.primaryPhrase}\"" } +
                if (commands.size > 3) "..." else ""
}

/**
 * Quick reference entry for the table view.
 *
 * @property command Primary command phrase
 * @property variations Alternative phrases (comma-separated for display)
 * @property action What happens when command is executed
 */
data class QuickReferenceEntry(
    val command: String,
    val variations: String,
    val action: String
)

/**
 * Complete help screen data.
 *
 * @property categories All command categories
 * @property quickReference Entries for quick reference table
 */
data class HelpScreenData(
    val categories: List<HelpCategory>,
    val quickReference: List<QuickReferenceEntry>
)

/**
 * Provides all help command data for the help screen.
 */
object HelpCommandDataProvider {

    // ═══════════════════════════════════════════════════════════════════
    // Category: Navigation
    // ═══════════════════════════════════════════════════════════════════
    private val navigationCommands = listOf(
        HelpCommand(
            primaryPhrase = "go back",
            variations = listOf("back", "navigate back", "previous screen"),
            description = "Go to previous screen",
            actionResult = "Returns to previous screen"
        ),
        HelpCommand(
            primaryPhrase = "go home",
            variations = listOf("home", "navigate home"),
            description = "Go to home screen",
            actionResult = "Opens home screen"
        ),
        HelpCommand(
            primaryPhrase = "recent apps",
            variations = listOf("show recents", "app switcher"),
            description = "Show recent apps",
            actionResult = "Opens app switcher"
        ),
        HelpCommand(
            primaryPhrase = "scroll down",
            variations = listOf("page down", "swipe up"),
            description = "Scroll the page down",
            actionResult = "Scrolls content down"
        ),
        HelpCommand(
            primaryPhrase = "scroll up",
            variations = listOf("page up", "swipe down"),
            description = "Scroll the page up",
            actionResult = "Scrolls content up"
        ),
        HelpCommand(
            primaryPhrase = "scroll left",
            variations = listOf("swipe right"),
            description = "Scroll to the left",
            actionResult = "Scrolls content left"
        ),
        HelpCommand(
            primaryPhrase = "scroll right",
            variations = listOf("swipe left"),
            description = "Scroll to the right",
            actionResult = "Scrolls content right"
        )
    )

    // ═══════════════════════════════════════════════════════════════════
    // Category: App Control
    // ═══════════════════════════════════════════════════════════════════
    private val appControlCommands = listOf(
        HelpCommand(
            primaryPhrase = "open [app name]",
            variations = listOf("launch [app]", "start [app]"),
            description = "Open any installed app",
            actionResult = "Launches the specified app"
        ),
        HelpCommand(
            primaryPhrase = "open camera",
            variations = listOf("camera", "take photo"),
            description = "Open the camera app",
            actionResult = "Launches camera"
        ),
        HelpCommand(
            primaryPhrase = "open settings",
            variations = listOf("settings", "device settings"),
            description = "Open system settings",
            actionResult = "Opens settings app"
        ),
        HelpCommand(
            primaryPhrase = "open browser",
            variations = listOf("browser", "web browser"),
            description = "Open web browser",
            actionResult = "Launches default browser"
        ),
        HelpCommand(
            primaryPhrase = "open messages",
            variations = listOf("messages", "sms"),
            description = "Open messaging app",
            actionResult = "Launches messages app"
        ),
        HelpCommand(
            primaryPhrase = "open calculator",
            variations = listOf("calculator"),
            description = "Open calculator",
            actionResult = "Launches calculator"
        )
    )

    // ═══════════════════════════════════════════════════════════════════
    // Category: UI Interaction
    // ═══════════════════════════════════════════════════════════════════
    private val uiInteractionCommands = listOf(
        HelpCommand(
            primaryPhrase = "click [element]",
            variations = listOf("tap [element]", "press [element]"),
            description = "Tap on an element by name",
            actionResult = "Clicks the specified element"
        ),
        HelpCommand(
            primaryPhrase = "long press [element]",
            variations = listOf("long click [element]", "hold [element]"),
            description = "Long press on element",
            actionResult = "Long presses the element"
        ),
        HelpCommand(
            primaryPhrase = "double tap [element]",
            variations = listOf("double click [element]"),
            description = "Double tap on element",
            actionResult = "Double taps the element"
        ),
        HelpCommand(
            primaryPhrase = "tap [number]",
            variations = listOf("click [number]", "[number]"),
            description = "Tap numbered element",
            actionResult = "Clicks element with that number"
        ),
        HelpCommand(
            primaryPhrase = "expand [element]",
            variations = emptyList(),
            description = "Expand a collapsible section",
            actionResult = "Expands the section"
        ),
        HelpCommand(
            primaryPhrase = "collapse [element]",
            variations = emptyList(),
            description = "Collapse an expanded section",
            actionResult = "Collapses the section"
        ),
        HelpCommand(
            primaryPhrase = "toggle [element]",
            variations = listOf("check [element]", "uncheck [element]"),
            description = "Toggle a checkbox or switch",
            actionResult = "Toggles the element state"
        )
    )

    // ═══════════════════════════════════════════════════════════════════
    // Category: Text Input
    // ═══════════════════════════════════════════════════════════════════
    private val textInputCommands = listOf(
        HelpCommand(
            primaryPhrase = "type [text]",
            variations = listOf("enter text [text]", "input [text]"),
            description = "Type text into focused field",
            actionResult = "Enters the specified text"
        ),
        HelpCommand(
            primaryPhrase = "delete",
            variations = listOf("backspace"),
            description = "Delete one character",
            actionResult = "Deletes previous character"
        ),
        HelpCommand(
            primaryPhrase = "clear text",
            variations = listOf("clear all"),
            description = "Clear all text in field",
            actionResult = "Clears the text field"
        ),
        HelpCommand(
            primaryPhrase = "select all",
            variations = emptyList(),
            description = "Select all text",
            actionResult = "Selects all text in field"
        ),
        HelpCommand(
            primaryPhrase = "copy",
            variations = emptyList(),
            description = "Copy selected text",
            actionResult = "Copies to clipboard"
        ),
        HelpCommand(
            primaryPhrase = "cut",
            variations = emptyList(),
            description = "Cut selected text",
            actionResult = "Cuts to clipboard"
        ),
        HelpCommand(
            primaryPhrase = "paste",
            variations = emptyList(),
            description = "Paste from clipboard",
            actionResult = "Pastes clipboard content"
        ),
        HelpCommand(
            primaryPhrase = "undo",
            variations = emptyList(),
            description = "Undo last action",
            actionResult = "Undoes last text change"
        ),
        HelpCommand(
            primaryPhrase = "search [query]",
            variations = listOf("find [query]"),
            description = "Search for text",
            actionResult = "Initiates search"
        )
    )

    // ═══════════════════════════════════════════════════════════════════
    // Category: System
    // ═══════════════════════════════════════════════════════════════════
    private val systemCommands = listOf(
        HelpCommand(
            primaryPhrase = "show notifications",
            variations = listOf("notifications", "notification panel"),
            description = "Show notification panel",
            actionResult = "Opens notifications"
        ),
        HelpCommand(
            primaryPhrase = "clear notifications",
            variations = listOf("dismiss notifications"),
            description = "Clear all notifications",
            actionResult = "Dismisses all notifications"
        ),
        HelpCommand(
            primaryPhrase = "quick settings",
            variations = emptyList(),
            description = "Show quick settings panel",
            actionResult = "Opens quick settings"
        ),
        HelpCommand(
            primaryPhrase = "take screenshot",
            variations = listOf("screenshot", "capture screen"),
            description = "Capture the screen",
            actionResult = "Takes a screenshot"
        ),
        HelpCommand(
            primaryPhrase = "flashlight on",
            variations = listOf("turn on flashlight", "torch on"),
            description = "Turn on flashlight",
            actionResult = "Turns flashlight on"
        ),
        HelpCommand(
            primaryPhrase = "flashlight off",
            variations = listOf("turn off flashlight", "torch off"),
            description = "Turn off flashlight",
            actionResult = "Turns flashlight off"
        ),
        HelpCommand(
            primaryPhrase = "brightness up",
            variations = emptyList(),
            description = "Increase screen brightness",
            actionResult = "Increases brightness by 10%"
        ),
        HelpCommand(
            primaryPhrase = "brightness down",
            variations = emptyList(),
            description = "Decrease screen brightness",
            actionResult = "Decreases brightness by 10%"
        ),
        HelpCommand(
            primaryPhrase = "lock screen",
            variations = listOf("lock"),
            description = "Lock the device",
            actionResult = "Locks the screen"
        )
    )

    // ═══════════════════════════════════════════════════════════════════
    // Category: Media
    // ═══════════════════════════════════════════════════════════════════
    private val mediaCommands = listOf(
        HelpCommand(
            primaryPhrase = "play music",
            variations = listOf("play", "resume"),
            description = "Play/resume media",
            actionResult = "Plays or resumes media"
        ),
        HelpCommand(
            primaryPhrase = "pause music",
            variations = listOf("pause", "stop music"),
            description = "Pause media playback",
            actionResult = "Pauses current media"
        ),
        HelpCommand(
            primaryPhrase = "next track",
            variations = listOf("next song", "skip"),
            description = "Skip to next track",
            actionResult = "Plays next track"
        ),
        HelpCommand(
            primaryPhrase = "previous track",
            variations = listOf("previous song"),
            description = "Go to previous track",
            actionResult = "Plays previous track"
        ),
        HelpCommand(
            primaryPhrase = "volume up",
            variations = listOf("increase volume", "louder"),
            description = "Increase volume",
            actionResult = "Increases volume by 10%"
        ),
        HelpCommand(
            primaryPhrase = "volume down",
            variations = listOf("decrease volume", "quieter"),
            description = "Decrease volume",
            actionResult = "Decreases volume by 10%"
        ),
        HelpCommand(
            primaryPhrase = "mute",
            variations = listOf("mute volume", "silence"),
            description = "Mute audio",
            actionResult = "Sets volume to 0"
        ),
        HelpCommand(
            primaryPhrase = "set volume [number]",
            variations = emptyList(),
            description = "Set volume to specific level",
            actionResult = "Sets volume to specified %"
        )
    )

    // ═══════════════════════════════════════════════════════════════════
    // Category: VoiceOS Control
    // ═══════════════════════════════════════════════════════════════════
    private val voiceOSCommands = listOf(
        HelpCommand(
            primaryPhrase = "numbers on",
            variations = listOf("show numbers", "numbers always"),
            description = "Always show element numbers",
            actionResult = "Shows numbers on all elements"
        ),
        HelpCommand(
            primaryPhrase = "numbers off",
            variations = listOf("hide numbers", "no numbers"),
            description = "Hide element numbers",
            actionResult = "Hides all element numbers"
        ),
        HelpCommand(
            primaryPhrase = "numbers auto",
            variations = listOf("numbers automatic", "auto numbers"),
            description = "Show numbers only for lists",
            actionResult = "Auto-shows numbers in lists"
        ),
        HelpCommand(
            primaryPhrase = "mute voice",
            variations = listOf("stop listening", "voice off"),
            description = "Pause voice recognition",
            actionResult = "Stops listening for commands"
        ),
        HelpCommand(
            primaryPhrase = "wake up voice",
            variations = listOf("start listening", "voice on"),
            description = "Resume voice recognition",
            actionResult = "Resumes listening"
        ),
        HelpCommand(
            primaryPhrase = "start dictation",
            variations = listOf("dictation", "type mode"),
            description = "Enter dictation mode",
            actionResult = "Switches to text dictation"
        ),
        HelpCommand(
            primaryPhrase = "stop dictation",
            variations = listOf("end dictation", "command mode"),
            description = "Exit dictation mode",
            actionResult = "Returns to command mode"
        ),
        HelpCommand(
            primaryPhrase = "what can I say",
            variations = listOf("show voice commands", "help"),
            description = "Show this help screen",
            actionResult = "Opens command help"
        )
    )

    // ═══════════════════════════════════════════════════════════════════
    // Category: Web Gestures
    // ═══════════════════════════════════════════════════════════════════
    private val webGestureCommands = listOf(
        HelpCommand(
            primaryPhrase = "pan left",
            variations = listOf("pan viewport left", "slide view left", "move view left"),
            description = "Pan viewport left",
            actionResult = "Shifts viewport left"
        ),
        HelpCommand(
            primaryPhrase = "pan right",
            variations = listOf("pan viewport right", "slide view right", "move view right"),
            description = "Pan viewport right",
            actionResult = "Shifts viewport right"
        ),
        HelpCommand(
            primaryPhrase = "pan up",
            variations = listOf("pan viewport up", "move view up"),
            description = "Pan viewport up",
            actionResult = "Shifts viewport up"
        ),
        HelpCommand(
            primaryPhrase = "pan down",
            variations = listOf("pan viewport down", "move view down"),
            description = "Pan viewport down",
            actionResult = "Shifts viewport down"
        ),
        HelpCommand(
            primaryPhrase = "tilt up",
            variations = listOf("tilt viewport up", "angle up"),
            description = "Tilt viewport up",
            actionResult = "Angles viewport upward"
        ),
        HelpCommand(
            primaryPhrase = "tilt down",
            variations = listOf("tilt viewport down", "angle down"),
            description = "Tilt viewport down",
            actionResult = "Angles viewport downward"
        ),
        HelpCommand(
            primaryPhrase = "orbit left",
            variations = listOf("orbit around left", "circle left"),
            description = "Orbit left around element",
            actionResult = "Orbits camera left"
        ),
        HelpCommand(
            primaryPhrase = "orbit right",
            variations = listOf("orbit around right", "circle right"),
            description = "Orbit right around element",
            actionResult = "Orbits camera right"
        ),
        HelpCommand(
            primaryPhrase = "rotate x",
            variations = listOf("rotate x axis", "flip vertical"),
            description = "Rotate element around X axis",
            actionResult = "Rotates on X axis"
        ),
        HelpCommand(
            primaryPhrase = "rotate y",
            variations = listOf("rotate y axis", "flip horizontal"),
            description = "Rotate element around Y axis",
            actionResult = "Rotates on Y axis"
        ),
        HelpCommand(
            primaryPhrase = "rotate z",
            variations = listOf("rotate z axis", "spin"),
            description = "Rotate element around Z axis",
            actionResult = "Spins element"
        ),
        HelpCommand(
            primaryPhrase = "pinch in",
            variations = listOf("pinch to zoom out", "squeeze"),
            description = "Pinch in to zoom out",
            actionResult = "Zooms out via pinch"
        ),
        HelpCommand(
            primaryPhrase = "pinch out",
            variations = listOf("pinch to zoom in", "spread"),
            description = "Pinch out to zoom in",
            actionResult = "Zooms in via pinch"
        ),
        HelpCommand(
            primaryPhrase = "fling up",
            variations = listOf("flick up"),
            description = "Fling content upward",
            actionResult = "Fast-scrolls content up"
        ),
        HelpCommand(
            primaryPhrase = "fling down",
            variations = listOf("flick down"),
            description = "Fling content downward",
            actionResult = "Fast-scrolls content down"
        ),
        HelpCommand(
            primaryPhrase = "throw",
            variations = listOf("toss", "throw element", "toss element"),
            description = "Throw element with velocity",
            actionResult = "Launches element with momentum"
        ),
        HelpCommand(
            primaryPhrase = "scale up",
            variations = listOf("enlarge", "make bigger"),
            description = "Scale element up",
            actionResult = "Enlarges element by 50%"
        ),
        HelpCommand(
            primaryPhrase = "scale down",
            variations = listOf("shrink", "make smaller"),
            description = "Scale element down",
            actionResult = "Shrinks element by 33%"
        ),
        HelpCommand(
            primaryPhrase = "reset zoom",
            variations = listOf("zoom reset", "normal zoom"),
            description = "Reset zoom to default",
            actionResult = "Returns to 100% zoom"
        ),
        HelpCommand(
            primaryPhrase = "grab",
            variations = listOf("grab element", "lock", "lock element", "hold", "latch"),
            description = "Grab/lock an element for dragging",
            actionResult = "Grabs element for manipulation"
        ),
        HelpCommand(
            primaryPhrase = "select word",
            variations = listOf("pick word", "highlight word"),
            description = "Select word at cursor",
            actionResult = "Highlights word under cursor"
        ),
        HelpCommand(
            primaryPhrase = "clear selection",
            variations = listOf("deselect", "unselect"),
            description = "Clear text selection",
            actionResult = "Removes selection highlight"
        ),
        HelpCommand(
            primaryPhrase = "hover out",
            variations = listOf("stop hovering", "unhover"),
            description = "Stop hovering over element",
            actionResult = "Triggers mouse-leave event"
        )
    )

    // ═══════════════════════════════════════════════════════════════════
    // All Categories
    // ═══════════════════════════════════════════════════════════════════

    private val navigationCategory = HelpCategory(
        id = "navigation",
        title = "Navigation",
        iconName = "navigation",
        commands = navigationCommands,
        color = "#4285F4" // Blue
    )

    private val appControlCategory = HelpCategory(
        id = "app_control",
        title = "App Control",
        iconName = "apps",
        commands = appControlCommands,
        color = "#34A853" // Green
    )

    private val uiInteractionCategory = HelpCategory(
        id = "ui_interaction",
        title = "UI Interaction",
        iconName = "touch_app",
        commands = uiInteractionCommands,
        color = "#FBBC04" // Yellow
    )

    private val textInputCategory = HelpCategory(
        id = "text_input",
        title = "Text Input",
        iconName = "keyboard",
        commands = textInputCommands,
        color = "#EA4335" // Red
    )

    private val systemCategory = HelpCategory(
        id = "system",
        title = "System",
        iconName = "settings",
        commands = systemCommands,
        color = "#9C27B0" // Purple
    )

    private val mediaCategory = HelpCategory(
        id = "media",
        title = "Media",
        iconName = "play_circle",
        commands = mediaCommands,
        color = "#FF5722" // Orange
    )

    private val voiceOSCategory = HelpCategory(
        id = "voiceos",
        title = "VoiceOS",
        iconName = "mic",
        commands = voiceOSCommands,
        color = "#00BCD4" // Cyan
    )

    private val webGestureCategory = HelpCategory(
        id = "web_gestures",
        title = "Web Gestures",
        iconName = "gesture",
        commands = webGestureCommands,
        color = "#E91E63" // Pink
    )

    /**
     * Get all help categories.
     */
    fun getCategories(): List<HelpCategory> = listOf(
        navigationCategory,
        appControlCategory,
        uiInteractionCategory,
        textInputCategory,
        systemCategory,
        mediaCategory,
        voiceOSCategory,
        webGestureCategory
    )

    /**
     * Get quick reference entries for table view.
     */
    fun getQuickReference(): List<QuickReferenceEntry> {
        val allCommands = getCategories().flatMap { it.commands }
        return allCommands.map { cmd ->
            QuickReferenceEntry(
                command = cmd.primaryPhrase,
                variations = cmd.variations.take(2).joinToString(", ").ifEmpty { "-" },
                action = cmd.actionResult
            )
        }
    }

    /**
     * Get complete help screen data.
     */
    fun getHelpScreenData(): HelpScreenData {
        return HelpScreenData(
            categories = getCategories(),
            quickReference = getQuickReference()
        )
    }

    /**
     * Find commands matching a search query.
     *
     * @param query Search text
     * @return List of matching commands
     */
    fun searchCommands(query: String): List<HelpCommand> {
        if (query.isBlank()) return emptyList()
        val normalizedQuery = query.lowercase().trim()

        return getCategories()
            .flatMap { it.commands }
            .filter { cmd ->
                cmd.primaryPhrase.lowercase().contains(normalizedQuery) ||
                cmd.variations.any { it.lowercase().contains(normalizedQuery) } ||
                cmd.description.lowercase().contains(normalizedQuery)
            }
    }

    /**
     * Get commands by category ID.
     *
     * @param categoryId Category identifier
     * @return List of commands, or empty if category not found
     */
    fun getCommandsByCategory(categoryId: String): List<HelpCommand> {
        return getCategories().find { it.id == categoryId }?.commands ?: emptyList()
    }

    /**
     * Get total command count across all categories.
     */
    fun getTotalCommandCount(): Int {
        return getCategories().sumOf { it.commandCount }
    }

    /**
     * Get all phrases for speech engine registration.
     * This enables voice-based help navigation.
     */
    fun getAllPhrases(): List<String> {
        return getCategories()
            .flatMap { it.commands }
            .flatMap { it.allPhrases }
            .distinct()
    }
}
