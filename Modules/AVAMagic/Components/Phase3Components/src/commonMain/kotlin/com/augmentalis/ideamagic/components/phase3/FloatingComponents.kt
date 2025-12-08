package com.augmentalis.avaelements.phase3

import com.augmentalis.avaelements.core.*
import kotlinx.serialization.Serializable

/**
 * FloatingCommandBar - Movable, orientation-adaptive glass panel
 *
 * A GlassAvanue-styled floating panel that can be positioned at any screen edge
 * (left, right, top, bottom) and automatically adjusts between portrait (vertical)
 * and landscape (horizontal) layouts.
 *
 * Features:
 * - Movable to any screen edge
 * - Auto-orientation (vertical for left/right, horizontal for top/bottom)
 * - Collapsible (icons only or icons + labels)
 * - Draggable with snap-to-edge
 * - Full GlassAvanue theme integration
 * - Persistent position across sessions
 *
 * Example:
 * ```
 * FloatingCommandBar(
 *     id = "main_nav",
 *     position = CommandBarPosition.Left,
 *     items = listOf(
 *         CommandBarItem("home", "home", "Home"),
 *         CommandBarItem("search", "search", "Search"),
 *         CommandBarItem("voice", "mic", "Voice")
 *     )
 * )
 * ```
 *
 * Created by: Manoj Jhawar, manoj@ideahq.net
 * Version: 1.0.0
 * Date: 2025-10-31
 */
@Serializable
data class FloatingCommandBar(
    val id: String,

    // Position & Orientation
    var position: CommandBarPosition = CommandBarPosition.Left,
    var orientation: Orientation = Orientation.Vertical, // Auto-set by position

    // Appearance
    var collapsed: Boolean = false,  // Icons only vs icons + labels
    var size: CommandBarSize = CommandBarSize.Medium,

    // Behavior
    var draggable: Boolean = true,
    var snapToEdge: Boolean = true,
    var autoHide: Boolean = false,  // Hide when not in use
    var showOnHover: Boolean = false,

    // Content
    var items: List<CommandBarItem>,

    // Style (inherits from GlassAvanue by default)
    var glassOpacity: Float = 0.75f,  // 75% glass
    var blurRadius: Float = 25f,       // 25px blur
    var cornerRadius: Float = 24f,     // 24px corners
    var backgroundColor: Color? = null, // null = use theme surface color
    var shadowElevation: Int = 3,      // Elevation level (0-5)

    // Callbacks
    var onPositionChange: ((CommandBarPosition) -> Unit)? = null,
    var onItemClick: ((CommandBarItem) -> Unit)? = null,
    var onVisibilityChange: ((Boolean) -> Unit)? = null
) : Component {
    override val type: String = "FloatingCommandBar"

    init {
        // Auto-set orientation based on position
        orientation = when (position) {
            CommandBarPosition.Left, CommandBarPosition.Right -> Orientation.Vertical
            CommandBarPosition.Top, CommandBarPosition.Bottom -> Orientation.Horizontal
            CommandBarPosition.FloatingCustom -> orientation
        }
    }

    /**
     * Update position and auto-adjust orientation
     */
    fun moveTo(newPosition: CommandBarPosition) {
        position = newPosition
        orientation = when (newPosition) {
            CommandBarPosition.Left, CommandBarPosition.Right -> Orientation.Vertical
            CommandBarPosition.Top, CommandBarPosition.Bottom -> Orientation.Horizontal
            CommandBarPosition.FloatingCustom -> orientation
        }
        onPositionChange?.invoke(newPosition)
    }

    /**
     * Toggle collapsed state
     */
    fun toggleCollapsed() {
        collapsed = !collapsed
    }

    /**
     * Get effective width based on orientation and collapsed state
     */
    fun getWidth(): Float {
        return when {
            orientation == Orientation.Vertical && collapsed -> 64f
            orientation == Orientation.Vertical && !collapsed -> 120f
            orientation == Orientation.Horizontal -> (items.size * 72f) + 32f
            else -> 64f
        }
    }

    /**
     * Get effective height based on orientation and collapsed state
     */
    fun getHeight(): Float {
        return when {
            orientation == Orientation.Vertical -> (items.size * 64f) + 32f
            orientation == Orientation.Horizontal && collapsed -> 64f
            orientation == Orientation.Horizontal && !collapsed -> 88f
            else -> 64f
        }
    }
}

/**
 * Command bar position modes
 */
@Serializable
enum class CommandBarPosition {
    Left,      // → Vertical orientation
    Right,     // → Vertical orientation
    Top,       // → Horizontal orientation
    Bottom,    // → Horizontal orientation
    FloatingCustom  // Custom x,y position (stays horizontal/vertical as set)
}

/**
 * Command bar size variants
 */
@Serializable
enum class CommandBarSize {
    Small,   // 48dp width/height
    Medium,  // 64dp width/height (default)
    Large    // 80dp width/height
}

/**
 * Individual command bar item
 */
@Serializable
data class CommandBarItem(
    val id: String,
    val icon: String,  // Icon name or SF Symbol
    val label: String,
    val badge: String? = null,  // Optional badge text
    val enabled: Boolean = true,
    val selected: Boolean = false,
    val tooltip: String? = null,

    // Integration points for special items
    val avaIntegration: AVAIntegration? = null,
    val searchIntegration: SearchIntegration? = null,
    val settingsIntegration: SettingsIntegration? = null,

    // Callback
    var onClick: (() -> Unit)? = null
) {
    companion object {
        // Common pre-configured items
        fun home(onClick: (() -> Unit)? = null) = CommandBarItem(
            id = "home",
            icon = "home",
            label = "Home",
            onClick = onClick
        )

        fun search(onClick: (() -> Unit)? = null, integration: SearchIntegration? = null) = CommandBarItem(
            id = "search",
            icon = "search",
            label = "Search",
            searchIntegration = integration,
            onClick = onClick
        )

        fun voice(onClick: (() -> Unit)? = null) = CommandBarItem(
            id = "voice",
            icon = "mic",
            label = "Voice",
            onClick = onClick
        )

        fun ava(onClick: (() -> Unit)? = null, integration: AVAIntegration? = null) = CommandBarItem(
            id = "ava",
            icon = "assistant",
            label = "AVA",
            avaIntegration = integration,
            onClick = onClick
        )

        fun settings(onClick: (() -> Unit)? = null, integration: SettingsIntegration? = null) = CommandBarItem(
            id = "settings",
            icon = "settings",
            label = "Settings",
            settingsIntegration = integration,
            onClick = onClick
        )

        fun apps(onClick: (() -> Unit)? = null) = CommandBarItem(
            id = "apps",
            icon = "apps",
            label = "Library",
            onClick = onClick
        )

        fun video(onClick: (() -> Unit)? = null) = CommandBarItem(
            id = "video",
            icon = "videocam",
            label = "Video",
            onClick = onClick
        )

        fun camera(onClick: (() -> Unit)? = null) = CommandBarItem(
            id = "camera",
            icon = "camera",
            label = "Camera",
            onClick = onClick
        )
    }
}

/**
 * AVA AI Assistant integration
 *
 * Allows integration with AVA the AI assistant for voice interactions,
 * context-aware suggestions, and intelligent command execution.
 */
@Serializable
data class AVAIntegration(
    val enabled: Boolean = true,
    val autoActivate: Boolean = false,  // Auto-activate AVA when item is clicked
    val contextAware: Boolean = true,   // Use current context for suggestions
    val voiceEnabled: Boolean = true,   // Enable voice input
    val visualFeedback: Boolean = true, // Show pulsing animation when listening

    // Callback for AVA responses
    var onAVAResponse: ((String) -> Unit)? = null,
    var onAVAError: ((String) -> Unit)? = null
)

/**
 * Search engine integration
 *
 * Allows integration with search functionality including web search,
 * local app search, and intelligent query suggestions.
 */
@Serializable
data class SearchIntegration(
    val enabled: Boolean = true,
    val searchScope: SearchScope = SearchScope.All,
    val showSuggestions: Boolean = true,
    val voiceSearch: Boolean = true,
    val visualSearch: Boolean = false,  // Enable camera-based visual search

    // Search filters
    val filters: List<SearchFilter> = emptyList(),

    // Callbacks
    var onSearchQuery: ((String) -> Unit)? = null,
    var onSearchResult: ((List<SearchResult>) -> Unit)? = null
) {
    @Serializable
    enum class SearchScope {
        All,        // Search everything
        Apps,       // Search installed apps only
        Contacts,   // Search contacts
        Files,      // Search files
        Web,        // Web search only
        AR          // AR objects in environment
    }

    @Serializable
    data class SearchFilter(
        val name: String,
        val enabled: Boolean = true
    )

    @Serializable
    data class SearchResult(
        val title: String,
        val description: String?,
        val icon: String?,
        val type: ResultType,
        var onOpen: (() -> Unit)? = null
    ) {
        enum class ResultType {
            App, Contact, File, WebPage, ARObject, Voice, Other
        }
    }
}

/**
 * Settings integration
 *
 * Allows integration with system and app settings, including quick toggles
 * for common settings and deep links to specific settings screens.
 */
@Serializable
data class SettingsIntegration(
    val enabled: Boolean = true,
    val showQuickToggles: Boolean = true,  // Show quick toggle switches
    val quickToggles: List<QuickToggle> = emptyList(),

    // Callbacks
    var onSettingChanged: ((String, Any) -> Unit)? = null,
    var onOpenSettings: ((SettingsScreen?) -> Unit)? = null
) {
    @Serializable
    data class QuickToggle(
        val id: String,
        val label: String,
        val icon: String,
        var enabled: Boolean = false,
        var onToggle: ((Boolean) -> Unit)? = null
    )

    @Serializable
    enum class SettingsScreen {
        General,
        Display,
        Sound,
        Privacy,
        Accessibility,
        Theme,
        Voice,
        AR,
        Network,
        Storage,
        About
    }
}

/**
 * Pre-configured floating navigation variants
 */
object FloatingNavigation {
    /**
     * Default navigation with common items
     */
    fun default() = FloatingCommandBar(
        id = "main_navigation",
        position = CommandBarPosition.Left,
        collapsed = false,
        items = listOf(
            CommandBarItem.home(),
            CommandBarItem.search(),
            CommandBarItem.voice(),
            CommandBarItem.apps(),
            CommandBarItem.settings()
        )
    )

    /**
     * VoiceOS Launcher navigation
     */
    fun launcher(
        onHome: (() -> Unit)? = null,
        onSearch: (() -> Unit)? = null,
        onVoice: (() -> Unit)? = null,
        onApps: (() -> Unit)? = null,
        onSettings: (() -> Unit)? = null
    ) = FloatingCommandBar(
        id = "voiceos_nav",
        position = CommandBarPosition.Left,
        size = CommandBarSize.Medium,
        collapsed = false,
        items = listOf(
            CommandBarItem.home(onHome),
            CommandBarItem.search(onSearch),
            CommandBarItem.voice(onVoice),
            CommandBarItem.apps(onApps),
            CommandBarItem.settings(onSettings)
        ),
        glassOpacity = 0.75f,
        blurRadius = 25f,
        cornerRadius = 24f
    )

    /**
     * Minimal navigation with just essential items
     */
    fun minimal() = FloatingCommandBar(
        id = "minimal_nav",
        position = CommandBarPosition.Left,
        collapsed = true,  // Icons only
        size = CommandBarSize.Small,
        items = listOf(
            CommandBarItem.home(),
            CommandBarItem.search(),
            CommandBarItem.settings()
        )
    )

    /**
     * AR-optimized navigation with transparent panels
     */
    fun ar() = FloatingCommandBar(
        id = "ar_nav",
        position = CommandBarPosition.Bottom,
        collapsed = false,
        size = CommandBarSize.Large,
        glassOpacity = 0.55f,  // More transparent for AR
        blurRadius = 30f,       // More blur for depth
        items = listOf(
            CommandBarItem.home(),
            CommandBarItem("ar_scan", "3d_rotation", "Scan"),
            CommandBarItem("ar_measure", "straighten", "Measure"),
            CommandBarItem.camera(),
            CommandBarItem.settings()
        )
    )

    /**
     * Voice-first navigation with AVA integration
     */
    fun voice(avaIntegration: AVAIntegration? = null) = FloatingCommandBar(
        id = "voice_nav",
        position = CommandBarPosition.Right,
        collapsed = false,
        items = listOf(
            CommandBarItem.ava(integration = avaIntegration),
            CommandBarItem.voice(),
            CommandBarItem("voice_history", "history", "History"),
            CommandBarItem.settings()
        )
    )
}
