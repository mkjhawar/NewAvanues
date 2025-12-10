package com.avanues.cockpit.ui

/**
 * Window Dock - Bottom center dot indicators
 *
 * macOS/Vision Pro style window indicators showing:
 * - Active window (highlighted dot)
 * - All open windows (dots)
 * - Click/tap or voice to switch
 *
 * Voice Commands:
 * - "Show dock" / "Hide dock"
 * - "Window 3" (switches to 3rd window)
 * - "Next window" / "Previous window"
 */
data class WindowDock(
    val windows: List<DockWindow>,
    val activeWindowId: String?,
    val visible: Boolean = true,
    val position: DockPosition = DockPosition.BOTTOM_CENTER
)

data class DockWindow(
    val id: String,
    val title: String,
    val icon: String?,          // Optional icon for window
    val isActive: Boolean,
    val voiceName: String       // "Gmail", "browser", "calculator"
)

enum class DockPosition {
    BOTTOM_CENTER,
    TOP_CENTER,
    LEFT_SIDE,
    RIGHT_SIDE
}

/**
 * Visual specs from preferred embodiment:
 * - Circular dots (~12-16dp diameter)
 * - Even spacing (~8-12dp between dots)
 * - Active dot: filled/highlighted
 * - Inactive dots: outline or dimmed
 * - Smooth animations on switch
 * - Spatial audio cue when switching via voice
 */
