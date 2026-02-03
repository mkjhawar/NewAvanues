package com.augmentalis.avaelements.input

/**
 * VoiceCursor Integration for AvaElements
 *
 * Enables voice-controlled cursor navigation for all AvaElements components
 * when running on Android with VoiceOS.
 *
 * Features:
 * - Register components as voice targets ("Click [label]")
 * - Track cursor position for hover states
 * - Handle gaze-based clicks
 * - IMU-based head tracking cursor movement
 *
 * Platform Support:
 * - Android: Full VoiceCursor support via VoiceOS
 * - iOS/Desktop/Web: Stub implementation (future expansion)
 */

// ═══════════════════════════════════════════════════════════════
// Voice Target
// ═══════════════════════════════════════════════════════════════

/**
 * Represents a UI element that can be targeted by VoiceCursor.
 * Users can say "Click [label]" to activate the target.
 */
data class VoiceTarget(
    /** Unique identifier for this target */
    val id: String,

    /** Voice label - what users say to select this target */
    val label: String,

    /** Bounding rectangle in screen coordinates */
    val bounds: Rect,

    /** Callback when target is selected via voice */
    val onSelect: () -> Unit,

    /** Callback when cursor enters/exits target bounds */
    val onHover: ((Boolean) -> Unit)? = null,

    /** Callback when cursor moves within target bounds */
    val onCursorMove: ((Offset) -> Unit)? = null,

    /** Whether this target is currently enabled */
    val isEnabled: Boolean = true,

    /** Priority for overlapping targets (higher = takes precedence) */
    val priority: Int = 0
) {
    /** Current hover state */
    internal var isHovered: Boolean = false
}

/**
 * Simple rectangle for bounds checking.
 */
data class Rect(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float
) {
    val width: Float get() = right - left
    val height: Float get() = bottom - top
    val centerX: Float get() = (left + right) / 2f
    val centerY: Float get() = (top + bottom) / 2f

    fun contains(x: Float, y: Float): Boolean =
        x >= left && x <= right && y >= top && y <= bottom

    fun contains(offset: Offset): Boolean =
        contains(offset.x, offset.y)

    companion object {
        val Zero = Rect(0f, 0f, 0f, 0f)
    }
}

// ═══════════════════════════════════════════════════════════════
// VoiceCursor Manager Interfaces (ISP-compliant segregation)
// ═══════════════════════════════════════════════════════════════

/**
 * Interface for registering and managing voice targets.
 * Clients that only need to register components can depend on this interface alone.
 */
interface VoiceTargetRegistry {
    /** Register a voice target */
    fun registerTarget(target: VoiceTarget)

    /** Unregister a voice target */
    fun unregisterTarget(id: String)

    /** Update target bounds (e.g., after layout) */
    fun updateTargetBounds(id: String, bounds: Rect)
}

/**
 * Interface for VoiceCursor lifecycle management.
 * Clients that control cursor activation/deactivation depend on this interface.
 */
interface VoiceCursorLifecycle {
    /** Whether VoiceCursor is available on this platform */
    val isAvailable: Boolean

    /** Whether VoiceCursor is currently active */
    val isActive: Boolean

    /** Start VoiceCursor (if available) */
    fun start()

    /** Stop VoiceCursor */
    fun stop()
}

/**
 * Interface for reading VoiceCursor state.
 * Clients that need to track cursor position depend on this interface.
 */
interface VoiceCursorState {
    /** Current cursor position (null if not active) */
    val cursorPosition: Offset?
}

/**
 * Interface for voice command handling and event listening.
 * Clients that need to respond to voice commands or cursor events depend on this interface.
 */
interface VoiceCursorEventDispatcher {
    /** Handle a voice command */
    fun handleVoiceCommand(command: String, parameters: Map<String, Any>): Boolean

    /** Add listener for cursor events */
    fun addListener(listener: VoiceCursorListener)

    /** Remove listener */
    fun removeListener(listener: VoiceCursorListener)
}

/**
 * Complete VoiceCursor manager interface.
 * Extends all segregated interfaces for full functionality.
 * Platform implementations provide either full support (Android) or stubs (other platforms).
 */
interface VoiceCursorManager :
    VoiceTargetRegistry,
    VoiceCursorLifecycle,
    VoiceCursorState,
    VoiceCursorEventDispatcher

/**
 * Listener for VoiceCursor events.
 */
interface VoiceCursorListener {
    /** Cursor position changed */
    fun onCursorMoved(position: Offset) {}

    /** Cursor entered a target */
    fun onTargetEntered(target: VoiceTarget) {}

    /** Cursor exited a target */
    fun onTargetExited(target: VoiceTarget) {}

    /** Target was selected (click) */
    fun onTargetSelected(target: VoiceTarget) {}

    /** VoiceCursor was activated */
    fun onActivated() {}

    /** VoiceCursor was deactivated */
    fun onDeactivated() {}
}

// ═══════════════════════════════════════════════════════════════
// Default Implementation (Stub for non-Android platforms)
// ═══════════════════════════════════════════════════════════════

/**
 * Default no-op implementation for platforms without VoiceCursor.
 */
class NoOpVoiceCursorManager : VoiceCursorManager {
    override val isAvailable: Boolean = false
    override val isActive: Boolean = false
    override val cursorPosition: Offset? = null

    override fun registerTarget(target: VoiceTarget) {}
    override fun unregisterTarget(id: String) {}
    override fun updateTargetBounds(id: String, bounds: Rect) {}
    override fun handleVoiceCommand(command: String, parameters: Map<String, Any>): Boolean = false
    override fun start() {}
    override fun stop() {}
    override fun addListener(listener: VoiceCursorListener) {}
    override fun removeListener(listener: VoiceCursorListener) {}
}

// ═══════════════════════════════════════════════════════════════
// Voice Commands
// ═══════════════════════════════════════════════════════════════

/**
 * Standard voice commands for UI interaction.
 * These are recognized by VoiceOS and routed to components.
 */
object VoiceCommands {
    // Navigation
    const val CLICK = "click"
    const val DOUBLE_CLICK = "double click"
    const val LONG_PRESS = "long press"
    const val SCROLL_UP = "scroll up"
    const val SCROLL_DOWN = "scroll down"
    const val SCROLL_LEFT = "scroll left"
    const val SCROLL_RIGHT = "scroll right"

    // Focus
    const val NEXT = "next"
    const val PREVIOUS = "previous"
    const val FOCUS = "focus"
    const val SELECT = "select"

    // Input
    const val TYPE = "type"
    const val CLEAR = "clear"
    const val BACKSPACE = "backspace"

    // Cursor
    const val CURSOR_UP = "cursor up"
    const val CURSOR_DOWN = "cursor down"
    const val CURSOR_LEFT = "cursor left"
    const val CURSOR_RIGHT = "cursor right"
    const val CURSOR_CENTER = "cursor center"

    // System
    const val SHOW_CURSOR = "show cursor"
    const val HIDE_CURSOR = "hide cursor"
    const val HELP = "help"
}

// ═══════════════════════════════════════════════════════════════
// Component Extension Points
// ═══════════════════════════════════════════════════════════════

/**
 * Configuration for making a component voice-accessible.
 */
data class VoiceCursorConfig(
    /** Voice label for "Click [label]" command */
    val label: String,

    /** Whether to enable hover state tracking */
    val enableHover: Boolean = true,

    /** Whether to enable cursor position tracking */
    val trackCursorPosition: Boolean = false,

    /** Custom voice commands for this component */
    val customCommands: List<CustomVoiceCommand> = emptyList(),

    /** Priority for overlapping targets */
    val priority: Int = 0
)

/**
 * Custom voice command for a specific component.
 */
data class CustomVoiceCommand(
    /** Command phrase (e.g., "expand", "collapse") */
    val phrase: String,

    /** Synonyms for the command */
    val synonyms: List<String> = emptyList(),

    /** Action to perform */
    val action: () -> Unit
)

// ═══════════════════════════════════════════════════════════════
// Platform Access
// ═══════════════════════════════════════════════════════════════

/**
 * Access to the platform-specific VoiceCursor manager.
 * Uses expect/actual pattern for platform implementations.
 */
expect fun getVoiceCursorManager(): VoiceCursorManager

/**
 * Convenience property to check VoiceCursor availability.
 */
val isVoiceCursorAvailable: Boolean
    get() = getVoiceCursorManager().isAvailable
