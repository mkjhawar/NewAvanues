package com.augmentalis.magicui.components.floating

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

/**
 * MagicUI Floating Components
 *
 * FloatingCommandBar - Movable, orientation-adaptive glass panel
 */

/**
 * Color wrapper for serialization (avoiding core dependency)
 */
@Serializable
data class FloatingColor(
    val red: Int,
    val green: Int,
    val blue: Int,
    val alpha: Float = 1.0f
)

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
 */
@Serializable
data class FloatingCommandBar(
    val id: String,
    var position: CommandBarPosition = CommandBarPosition.Left,
    var orientation: Orientation = Orientation.Vertical,
    var collapsed: Boolean = false,
    var size: CommandBarSize = CommandBarSize.Medium,
    var draggable: Boolean = true,
    var snapToEdge: Boolean = true,
    var autoHide: Boolean = false,
    var showOnHover: Boolean = false,
    var items: List<CommandBarItem>,
    var glassOpacity: Float = 0.75f,
    var blurRadius: Float = 25f,
    var cornerRadius: Float = 24f,
    var backgroundColor: FloatingColor? = null,
    var shadowElevation: Int = 3
) {
    val type: String = "FloatingCommandBar"

    @Transient
    var onPositionChange: ((CommandBarPosition) -> Unit)? = null
    @Transient
    var onItemClick: ((CommandBarItem) -> Unit)? = null
    @Transient
    var onVisibilityChange: ((Boolean) -> Unit)? = null

    init {
        orientation = when (position) {
            CommandBarPosition.Left, CommandBarPosition.Right -> Orientation.Vertical
            CommandBarPosition.Top, CommandBarPosition.Bottom -> Orientation.Horizontal
            CommandBarPosition.FloatingCustom -> orientation
        }
    }

    fun moveTo(newPosition: CommandBarPosition) {
        position = newPosition
        orientation = when (newPosition) {
            CommandBarPosition.Left, CommandBarPosition.Right -> Orientation.Vertical
            CommandBarPosition.Top, CommandBarPosition.Bottom -> Orientation.Horizontal
            CommandBarPosition.FloatingCustom -> orientation
        }
        onPositionChange?.invoke(newPosition)
    }

    fun toggleCollapsed() {
        collapsed = !collapsed
    }

    fun getWidth(): Float = when {
        orientation == Orientation.Vertical && collapsed -> 64f
        orientation == Orientation.Vertical && !collapsed -> 120f
        orientation == Orientation.Horizontal -> (items.size * 72f) + 32f
        else -> 64f
    }

    fun getHeight(): Float = when {
        orientation == Orientation.Vertical -> (items.size * 64f) + 32f
        orientation == Orientation.Horizontal && collapsed -> 64f
        orientation == Orientation.Horizontal && !collapsed -> 88f
        else -> 64f
    }
}

@Serializable
enum class CommandBarPosition {
    Left,
    Right,
    Top,
    Bottom,
    FloatingCustom
}

@Serializable
enum class CommandBarSize {
    Small,
    Medium,
    Large
}

@Serializable
enum class Orientation {
    Horizontal,
    Vertical
}

@Serializable
data class CommandBarItem(
    val id: String,
    val icon: String,
    val label: String,
    val badge: String? = null,
    val enabled: Boolean = true,
    val selected: Boolean = false,
    val tooltip: String? = null,
    val avaIntegration: AVAIntegration? = null,
    val searchIntegration: SearchIntegration? = null,
    val settingsIntegration: SettingsIntegration? = null
) {
    @Transient
    var onClick: (() -> Unit)? = null

    companion object {
        fun home() = CommandBarItem("home", "home", "Home")
        fun search(integration: SearchIntegration? = null) = CommandBarItem("search", "search", "Search", searchIntegration = integration)
        fun voice() = CommandBarItem("voice", "mic", "Voice")
        fun ava(integration: AVAIntegration? = null) = CommandBarItem("ava", "assistant", "AVA", avaIntegration = integration)
        fun settings(integration: SettingsIntegration? = null) = CommandBarItem("settings", "settings", "Settings", settingsIntegration = integration)
        fun apps() = CommandBarItem("apps", "apps", "Library")
        fun video() = CommandBarItem("video", "videocam", "Video")
        fun camera() = CommandBarItem("camera", "camera", "Camera")
    }
}

@Serializable
data class AVAIntegration(
    val enabled: Boolean = true,
    val autoActivate: Boolean = false,
    val contextAware: Boolean = true,
    val voiceEnabled: Boolean = true,
    val visualFeedback: Boolean = true
) {
    @Transient
    var onAVAResponse: ((String) -> Unit)? = null
    @Transient
    var onAVAError: ((String) -> Unit)? = null
}

@Serializable
data class SearchIntegration(
    val enabled: Boolean = true,
    val searchScope: SearchScope = SearchScope.All,
    val showSuggestions: Boolean = true,
    val voiceSearch: Boolean = true,
    val visualSearch: Boolean = false,
    val filters: List<SearchFilter> = emptyList()
) {
    @Transient
    var onSearchQuery: ((String) -> Unit)? = null
    @Transient
    var onSearchResult: ((List<SearchResult>) -> Unit)? = null

    @Serializable
    enum class SearchScope { All, Apps, Contacts, Files, Web, AR }

    @Serializable
    data class SearchFilter(val name: String, val enabled: Boolean = true)

    @Serializable
    data class SearchResult(
        val title: String,
        val description: String?,
        val icon: String?,
        val type: ResultType
    ) {
        @Transient
        var onOpen: (() -> Unit)? = null

        @Serializable
        enum class ResultType { App, Contact, File, WebPage, ARObject, Voice, Other }
    }
}

@Serializable
data class SettingsIntegration(
    val enabled: Boolean = true,
    val showQuickToggles: Boolean = true,
    val quickToggles: List<QuickToggle> = emptyList()
) {
    @Transient
    var onSettingChanged: ((String, String) -> Unit)? = null
    @Transient
    var onOpenSettings: ((SettingsScreen?) -> Unit)? = null

    @Serializable
    data class QuickToggle(
        val id: String,
        val label: String,
        val icon: String,
        var enabled: Boolean = false
    ) {
        @Transient
        var onToggle: ((Boolean) -> Unit)? = null
    }

    @Serializable
    enum class SettingsScreen {
        General, Display, Sound, Privacy, Accessibility, Theme, Voice, AR, Network, Storage, About
    }
}

/**
 * Pre-configured floating navigation variants
 */
object FloatingNavigation {
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

    fun launcher() = FloatingCommandBar(
        id = "voiceos_nav",
        position = CommandBarPosition.Left,
        size = CommandBarSize.Medium,
        collapsed = false,
        items = listOf(
            CommandBarItem.home(),
            CommandBarItem.search(),
            CommandBarItem.voice(),
            CommandBarItem.apps(),
            CommandBarItem.settings()
        ),
        glassOpacity = 0.75f,
        blurRadius = 25f,
        cornerRadius = 24f
    )

    fun minimal() = FloatingCommandBar(
        id = "minimal_nav",
        position = CommandBarPosition.Left,
        collapsed = true,
        size = CommandBarSize.Small,
        items = listOf(
            CommandBarItem.home(),
            CommandBarItem.search(),
            CommandBarItem.settings()
        )
    )

    fun ar() = FloatingCommandBar(
        id = "ar_nav",
        position = CommandBarPosition.Bottom,
        collapsed = false,
        size = CommandBarSize.Large,
        glassOpacity = 0.55f,
        blurRadius = 30f,
        items = listOf(
            CommandBarItem.home(),
            CommandBarItem("ar_scan", "3d_rotation", "Scan"),
            CommandBarItem("ar_measure", "straighten", "Measure"),
            CommandBarItem.camera(),
            CommandBarItem.settings()
        )
    )

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
