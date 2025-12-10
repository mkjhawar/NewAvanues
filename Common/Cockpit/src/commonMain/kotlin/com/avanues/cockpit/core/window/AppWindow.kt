package com.avanues.cockpit.core.window

import com.avanues.cockpit.core.workspace.Vector3D
import kotlinx.serialization.Serializable

/**
 * Represents a window in the Cockpit spatial workspace
 *
 * Core window model supporting multiple content types (Android apps, web apps,
 * remote desktops, widgets) with full state persistence and voice-first interaction.
 *
 * **Voice-First Integration:**
 * - Every window has a voiceName for VoiceOS commands: "Open Gmail", "Focus browser"
 * - VoiceOS announces voiceDescription when window gains focus
 * - Custom voiceShortcuts for app-specific commands
 * - Spatial audio positioning based on 3D location
 *
 * @property id Unique identifier (UUID)
 * @property title Display title (shown in UI)
 * @property type Window content type (Android app, web, remote desktop, widget)
 * @property sourceId Content identifier (package name, URL, connection ID)
 * @property position 3D spatial position (Vector3D in meters)
 * @property widthMeters Physical width in meters at viewing distance
 * @property heightMeters Physical height in meters at viewing distance
 * @property zLayer Rendering layer (0 = background, 999 = foreground)
 * @property pinned Whether window stays in fixed position
 * @property visible Whether window is currently rendered
 * @property state Persistent state (scroll, zoom, media playback)
 * @property voiceName Short name for voice commands (e.g., "Gmail", "browser", "calculator")
 * @property voiceDescription Text announced when window gains focus
 * @property voiceShortcuts Custom voice commands specific to this window
 * @property spatialAudioEnabled Whether to use 3D audio positioning for voice feedback
 */
@Serializable
data class AppWindow(
    val id: String,
    val title: String,
    val type: WindowType,
    val sourceId: String,

    // Spatial positioning
    val position: Vector3D = Vector3D(0f, 0f, -2f),
    val widthMeters: Float = 0.8f,
    val heightMeters: Float = 0.6f,
    val zLayer: Int = 0,

    // Window behavior
    val pinned: Boolean = false,
    val visible: Boolean = true,

    // State persistence (NEW - Task 1.2)
    val state: WindowState = WindowState.DEFAULT,

    // Voice-first properties
    val voiceName: String = title.lowercase(),
    val voiceDescription: String = "$title window",
    val voiceShortcuts: List<String> = emptyList(),
    val spatialAudioEnabled: Boolean = true
) {
    companion object {
        /** Default window dimensions at 2 meters distance (~31" × 24") */
        const val DEFAULT_WIDTH_METERS = 0.8f
        const val DEFAULT_HEIGHT_METERS = 0.6f

        /** Minimum window dimensions (prevents too-small windows) */
        const val MIN_WIDTH_METERS = 0.3f
        const val MIN_HEIGHT_METERS = 0.2f

        /** Maximum window dimensions (prevents overly large windows) */
        const val MAX_WIDTH_METERS = 3.0f
        const val MAX_HEIGHT_METERS = 2.0f

        /**
         * Creates a new AppWindow for an Android app
         * Voice command: "Open [app name]"
         */
        fun androidApp(
            id: String,
            title: String,
            packageName: String,
            position: Vector3D = Vector3D(0f, 0f, -2f),
            voiceName: String = title.lowercase()
        ) = AppWindow(
            id = id,
            title = title,
            type = WindowType.ANDROID_APP,
            sourceId = packageName,
            position = position,
            voiceName = voiceName,
            voiceDescription = "$title app"
        )

        /**
         * Creates a new AppWindow for a web app
         * Voice command: "Open [site name]"
         */
        fun webApp(
            id: String,
            title: String,
            url: String,
            position: Vector3D = Vector3D(0f, 0f, -2f),
            voiceName: String = title.lowercase()
        ) = AppWindow(
            id = id,
            title = title,
            type = WindowType.WEB_APP,
            sourceId = url,
            position = position,
            voiceName = voiceName,
            voiceDescription = "$title website"
        )

        /**
         * Creates a new AppWindow for a remote desktop
         * Voice command: "Open [desktop name] desktop"
         */
        fun remoteDesktop(
            id: String,
            title: String,
            connectionId: String,
            position: Vector3D = Vector3D(0f, 0f, -2f),
            voiceName: String = title.lowercase()
        ) = AppWindow(
            id = id,
            title = title,
            type = WindowType.REMOTE_DESKTOP,
            sourceId = connectionId,
            position = position,
            widthMeters = 1.2f,  // Desktops typically larger
            heightMeters = 0.9f,
            voiceName = voiceName,
            voiceDescription = "$title remote desktop"
        )

        /**
         * Creates a new AppWindow for a widget
         * Voice command: "Show [widget name]"
         */
        fun widget(
            id: String,
            title: String,
            widgetType: String,
            position: Vector3D = Vector3D(0f, 0f, -2f),
            widthMeters: Float = 0.4f,
            heightMeters: Float = 0.3f,
            voiceName: String = title.lowercase()
        ) = AppWindow(
            id = id,
            title = title,
            type = WindowType.WIDGET,
            sourceId = widgetType,
            position = position,
            widthMeters = widthMeters,
            heightMeters = heightMeters,
            voiceName = voiceName,
            voiceDescription = "$title widget"
        )
    }

    /**
     * Updates window position
     * Voice commands: "Move [window] left/right/up/down"
     */
    fun withPosition(newPosition: Vector3D): AppWindow = copy(position = newPosition)

    /**
     * Moves window by offset
     * Voice commands: "Move [window] a bit to the right"
     */
    fun moveBy(offset: Vector3D): AppWindow = copy(position = position + offset)

    /**
     * Updates window dimensions
     * Voice commands: "Make [window] bigger/smaller"
     */
    fun withDimensions(width: Float, height: Float): AppWindow = copy(
        widthMeters = width.coerceIn(MIN_WIDTH_METERS, MAX_WIDTH_METERS),
        heightMeters = height.coerceIn(MIN_HEIGHT_METERS, MAX_HEIGHT_METERS)
    )

    /**
     * Scales window dimensions by factor
     * Voice command: "Make [window] 150% size"
     */
    fun scale(factor: Float): AppWindow = withDimensions(
        width = widthMeters * factor,
        height = heightMeters * factor
    )

    /**
     * Increases window size by 20%
     * Voice command: "Make [window] bigger"
     */
    fun makeBigger(): AppWindow = scale(1.2f)

    /**
     * Decreases window size by 20%
     * Voice command: "Make [window] smaller"
     */
    fun makeSmaller(): AppWindow = scale(0.8f)

    /**
     * Toggles window pinned state
     * Voice command: "Pin/unpin this window"
     */
    fun togglePin(): AppWindow = copy(pinned = !pinned)

    /**
     * Toggles window visibility
     * Voice command: "Hide/show [window]"
     */
    fun toggleVisibility(): AppWindow = copy(visible = !visible)

    /**
     * Brings window to front (highest z-layer)
     * Voice command: "Focus [window]", "Bring [window] to front"
     */
    fun bringToFront(): AppWindow = copy(zLayer = 999)

    /**
     * Sends window to back (lowest z-layer)
     * Voice command: "Send [window] to back"
     */
    fun sendToBack(): AppWindow = copy(zLayer = 0)

    /**
     * Updates window state (scroll, zoom, media)
     * Called automatically during user interaction or voice commands
     */
    fun withState(newState: WindowState): AppWindow = copy(
        state = newState.touch() // Updates lastAccessed timestamp
    )

    /**
     * Updates scroll position
     * Voice commands: "Scroll down", "Go to top"
     */
    fun scrollTo(x: Int, y: Int): AppWindow = withState(state.withScroll(x, y))

    /**
     * Updates zoom level
     * Voice commands: "Zoom in", "Zoom out", "Reset zoom"
     */
    fun zoomTo(level: Float): AppWindow = withState(state.withZoom(level))

    /**
     * Starts media playback
     * Voice command: "Play", "Resume"
     */
    fun playMedia(): AppWindow = withState(state.play())

    /**
     * Pauses media playback
     * Voice command: "Pause"
     */
    fun pauseMedia(): AppWindow = withState(state.pause())

    /**
     * Updates media playback position
     * Voice commands: "Skip to 2 minutes 30 seconds"
     */
    fun seekMedia(positionMs: Long): AppWindow = withState(
        state.withMediaPosition(positionMs, state.isPlaying)
    )

    /**
     * Generates voice description for VoiceOS announcements
     * Called when window gains focus or user asks "What's in this window?"
     *
     * Example output:
     * "Gmail window at center front, scrolled down, zoomed to 150%"
     */
    fun toVoiceAnnouncement(): String {
        val posDesc = when {
            position.x < -0.5f -> "left"
            position.x > 0.5f -> "right"
            else -> "center"
        }
        val depthDesc = when {
            position.z < -2.5f -> "far"
            position.z > -1.5f -> "near"
            else -> "front"
        }

        val stateDesc = state.toVoiceDescription()
        val pinnedDesc = if (pinned) ", pinned" else ""

        return "$voiceDescription at $posDesc $depthDesc, $stateDesc$pinnedDesc"
    }
}

/**
 * Window content type
 */
@Serializable
enum class WindowType {
    /** Native Android application (via accessibility service) */
    ANDROID_APP,

    /** Web application (via WebView) */
    WEB_APP,

    /** Remote desktop connection (RDP/VNC) */
    REMOTE_DESKTOP,

    /** Small widget (clock, weather, shortcuts) */
    WIDGET
}
