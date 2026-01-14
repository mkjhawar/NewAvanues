package com.augmentalis.avaelements.input

/**
 * Unified Input State for AvaElements
 *
 * Provides consistent input state tracking across all platforms:
 * - Android (Touch, Mouse, Keyboard, VoiceCursor, Stylus, Gamepad)
 * - iOS (Touch, Mouse, Keyboard, Trackpad, Apple Pencil)
 * - Desktop (Mouse, Keyboard, Trackpad, Touch, Stylus)
 * - Web (Mouse, Keyboard, Touch)
 */

// ═══════════════════════════════════════════════════════════════
// Input State
// ═══════════════════════════════════════════════════════════════

/**
 * Unified input state that works across all platforms.
 * Components observe this state to update their appearance.
 */
data class InputState(
    /** Mouse/cursor is over the element */
    val isHovered: Boolean = false,

    /** Element is currently being pressed/touched */
    val isPressed: Boolean = false,

    /** Element has keyboard/focus ring focus */
    val isFocused: Boolean = false,

    /** Element is in selected state */
    val isSelected: Boolean = false,

    /** Element is being dragged */
    val isDragging: Boolean = false,

    /** Element is disabled */
    val isDisabled: Boolean = false,

    /** Current cursor/pointer position if hovering */
    val cursorPosition: Offset? = null,

    /** Input source that last interacted with this element */
    val inputSource: InputSource = InputSource.Unknown,

    /** Pressure level for stylus input (0.0 - 1.0) */
    val pressure: Float = 1.0f,

    /** Tilt angle for stylus input (degrees) */
    val tilt: Float = 0.0f
) {
    companion object {
        val Default = InputState()
        val Hovered = InputState(isHovered = true)
        val Pressed = InputState(isPressed = true)
        val Focused = InputState(isFocused = true)
        val Disabled = InputState(isDisabled = true)
    }

    /** Returns true if any interactive state is active */
    val isInteractive: Boolean
        get() = isHovered || isPressed || isFocused || isDragging

    /** Returns the appropriate visual state for styling */
    val visualState: VisualState
        get() = when {
            isDisabled -> VisualState.Disabled
            isPressed -> VisualState.Pressed
            isDragging -> VisualState.Dragging
            isFocused -> VisualState.Focused
            isHovered -> VisualState.Hovered
            isSelected -> VisualState.Selected
            else -> VisualState.Default
        }
}

/**
 * Visual states for component styling
 */
enum class VisualState {
    Default,
    Hovered,
    Pressed,
    Focused,
    Selected,
    Dragging,
    Disabled
}

// ═══════════════════════════════════════════════════════════════
// Input Source
// ═══════════════════════════════════════════════════════════════

/**
 * Identifies the source of input for adaptive behavior.
 * Allows components to adjust their behavior based on input method.
 */
enum class InputSource {
    /** Finger touch on touchscreen */
    Touch,

    /** Mouse device (including DeX, Chromebook, iPadOS) */
    Mouse,

    /** Trackpad gestures */
    Trackpad,

    /** Stylus/pen input (S Pen, Apple Pencil, Wacom) */
    Stylus,

    /** Keyboard navigation (Tab, Arrow keys) */
    Keyboard,

    /** VoiceOS VoiceCursor system */
    VoiceCursor,

    /** Game controller/gamepad */
    Gamepad,

    /** Unknown or programmatic */
    Unknown;

    /** Returns true if this is a precise pointer (not touch) */
    val isPrecisePointer: Boolean
        get() = this in listOf(Mouse, Trackpad, Stylus, VoiceCursor)

    /** Returns true if this input supports hover states */
    val supportsHover: Boolean
        get() = this in listOf(Mouse, Trackpad, Stylus, VoiceCursor)

    /** Returns true if this is a voice-based input */
    val isVoice: Boolean
        get() = this == VoiceCursor
}

// ═══════════════════════════════════════════════════════════════
// Platform Capabilities
// ═══════════════════════════════════════════════════════════════

/**
 * Describes the input capabilities available on the current platform/device.
 * Detected at runtime to enable appropriate input handling.
 */
data class InputCapabilities(
    /** Device has touchscreen */
    val hasTouch: Boolean = false,

    /** Device has mouse (external or simulated) */
    val hasMouse: Boolean = false,

    /** Device has physical keyboard */
    val hasKeyboard: Boolean = false,

    /** Device has trackpad */
    val hasTrackpad: Boolean = false,

    /** Device supports stylus/pen input */
    val hasStylus: Boolean = false,

    /** VoiceCursor is available (Android VoiceOS only) */
    val hasVoiceCursor: Boolean = false,

    /** Device has game controller connected */
    val hasGamepad: Boolean = false,

    /** Device supports multi-touch */
    val hasMultiTouch: Boolean = false,

    /** Device supports pressure-sensitive input */
    val hasPressure: Boolean = false
) {
    companion object {
        /** Default desktop capabilities */
        val Desktop = InputCapabilities(
            hasTouch = false,
            hasMouse = true,
            hasKeyboard = true,
            hasTrackpad = true,
            hasStylus = false,
            hasVoiceCursor = false,
            hasGamepad = false
        )

        /** Default mobile capabilities */
        val Mobile = InputCapabilities(
            hasTouch = true,
            hasMouse = false,
            hasKeyboard = false,
            hasTrackpad = false,
            hasStylus = false,
            hasVoiceCursor = false,
            hasGamepad = false,
            hasMultiTouch = true
        )

        /** Default web capabilities (detected at runtime) */
        val Web = InputCapabilities(
            hasTouch = true,  // Assume touch for progressive enhancement
            hasMouse = true,
            hasKeyboard = true,
            hasTrackpad = true,
            hasStylus = false,
            hasVoiceCursor = false,
            hasGamepad = false
        )
    }

    /** Returns true if hover states should be enabled */
    val enableHoverStates: Boolean
        get() = hasMouse || hasTrackpad || hasVoiceCursor

    /** Returns true if keyboard navigation should be enabled */
    val enableKeyboardNavigation: Boolean
        get() = hasKeyboard

    /** Returns the primary input mode */
    val primaryInputMode: InputSource
        get() = when {
            hasVoiceCursor -> InputSource.VoiceCursor
            hasTouch && !hasMouse -> InputSource.Touch
            hasMouse -> InputSource.Mouse
            hasKeyboard -> InputSource.Keyboard
            else -> InputSource.Unknown
        }
}

// ═══════════════════════════════════════════════════════════════
// Offset (Position)
// ═══════════════════════════════════════════════════════════════

/**
 * Simple 2D offset/position class.
 * Platform renderers convert to their native offset types.
 */
data class Offset(
    val x: Float,
    val y: Float
) {
    companion object {
        val Zero = Offset(0f, 0f)
        val Unspecified = Offset(Float.NaN, Float.NaN)
    }

    val isSpecified: Boolean
        get() = !x.isNaN() && !y.isNaN()

    operator fun plus(other: Offset) = Offset(x + other.x, y + other.y)
    operator fun minus(other: Offset) = Offset(x - other.x, y - other.y)
    operator fun times(scalar: Float) = Offset(x * scalar, y * scalar)
    operator fun div(scalar: Float) = Offset(x / scalar, y / scalar)

    fun distanceTo(other: Offset): Float {
        val dx = other.x - x
        val dy = other.y - y
        return kotlin.math.sqrt(dx * dx + dy * dy)
    }
}

// ═══════════════════════════════════════════════════════════════
// Velocity
// ═══════════════════════════════════════════════════════════════

/**
 * Velocity in pixels per second.
 * Used for fling/scroll physics.
 */
data class Velocity(
    val x: Float,
    val y: Float
) {
    companion object {
        val Zero = Velocity(0f, 0f)
    }

    val magnitude: Float
        get() = kotlin.math.sqrt(x * x + y * y)
}
