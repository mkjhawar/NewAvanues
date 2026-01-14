package com.augmentalis.avaelements.input

/**
 * Unified Input Events for AvaElements
 *
 * Platform-agnostic input events that are translated
 * from native events by each platform's input handler.
 */

// ═══════════════════════════════════════════════════════════════
// Input Events
// ═══════════════════════════════════════════════════════════════

/**
 * Base sealed class for all input events.
 * Each platform translates native events to these unified types.
 */
sealed class InputEvent {

    /** Timestamp when the event occurred (milliseconds) */
    abstract val timestamp: Long

    /** Source of the input */
    abstract val source: InputSource

    // ─────────────────────────────────────────────────────────────
    // Pointer Events (Touch, Mouse, Stylus)
    // ─────────────────────────────────────────────────────────────

    /** Single tap/click event */
    data class Tap(
        val position: Offset,
        override val source: InputSource,
        override val timestamp: Long = currentTimeMillis()
    ) : InputEvent()

    /** Double tap/click event */
    data class DoubleTap(
        val position: Offset,
        override val source: InputSource,
        override val timestamp: Long = currentTimeMillis()
    ) : InputEvent()

    /** Long press event */
    data class LongPress(
        val position: Offset,
        override val source: InputSource,
        val duration: Long = 500L,
        override val timestamp: Long = currentTimeMillis()
    ) : InputEvent()

    /** Pointer/cursor enters element bounds */
    data class HoverEnter(
        val position: Offset,
        override val source: InputSource,
        override val timestamp: Long = currentTimeMillis()
    ) : InputEvent()

    /** Pointer/cursor moves within element bounds */
    data class HoverMove(
        val position: Offset,
        override val source: InputSource,
        override val timestamp: Long = currentTimeMillis()
    ) : InputEvent()

    /** Pointer/cursor exits element bounds */
    data class HoverExit(
        val position: Offset,
        override val source: InputSource,
        override val timestamp: Long = currentTimeMillis()
    ) : InputEvent()

    // ─────────────────────────────────────────────────────────────
    // Drag Events
    // ─────────────────────────────────────────────────────────────

    /** Drag gesture started */
    data class DragStart(
        val position: Offset,
        override val source: InputSource,
        override val timestamp: Long = currentTimeMillis()
    ) : InputEvent()

    /** Drag gesture in progress */
    data class Drag(
        val position: Offset,
        val delta: Offset,
        override val source: InputSource,
        override val timestamp: Long = currentTimeMillis()
    ) : InputEvent()

    /** Drag gesture ended */
    data class DragEnd(
        val position: Offset,
        val velocity: Velocity,
        override val source: InputSource,
        override val timestamp: Long = currentTimeMillis()
    ) : InputEvent()

    /** Drag was cancelled (e.g., gesture interrupted) */
    data class DragCancel(
        override val source: InputSource,
        override val timestamp: Long = currentTimeMillis()
    ) : InputEvent()

    // ─────────────────────────────────────────────────────────────
    // Scroll Events
    // ─────────────────────────────────────────────────────────────

    /** Scroll event (mouse wheel, trackpad, touch) */
    data class Scroll(
        val delta: Offset,
        val scrollSource: ScrollSource,
        override val source: InputSource,
        override val timestamp: Long = currentTimeMillis()
    ) : InputEvent()

    /** Fling gesture (fast scroll with momentum) */
    data class Fling(
        val velocity: Velocity,
        override val source: InputSource,
        override val timestamp: Long = currentTimeMillis()
    ) : InputEvent()

    // ─────────────────────────────────────────────────────────────
    // Keyboard Events
    // ─────────────────────────────────────────────────────────────

    /** Key pressed down */
    data class KeyDown(
        val key: Key,
        val modifiers: KeyModifiers,
        val isRepeat: Boolean = false,
        override val source: InputSource = InputSource.Keyboard,
        override val timestamp: Long = currentTimeMillis()
    ) : InputEvent()

    /** Key released */
    data class KeyUp(
        val key: Key,
        val modifiers: KeyModifiers,
        override val source: InputSource = InputSource.Keyboard,
        override val timestamp: Long = currentTimeMillis()
    ) : InputEvent()

    /** Text input (for text fields) */
    data class TextInput(
        val text: String,
        override val source: InputSource = InputSource.Keyboard,
        override val timestamp: Long = currentTimeMillis()
    ) : InputEvent()

    // ─────────────────────────────────────────────────────────────
    // Focus Events
    // ─────────────────────────────────────────────────────────────

    /** Element gained focus */
    data class FocusGained(
        val focusSource: FocusSource,
        override val source: InputSource,
        override val timestamp: Long = currentTimeMillis()
    ) : InputEvent()

    /** Element lost focus */
    data class FocusLost(
        override val source: InputSource,
        override val timestamp: Long = currentTimeMillis()
    ) : InputEvent()

    // ─────────────────────────────────────────────────────────────
    // VoiceCursor Events (Android VoiceOS)
    // ─────────────────────────────────────────────────────────────

    /** Voice command received */
    data class VoiceCommand(
        val command: String,
        val parameters: Map<String, Any> = emptyMap(),
        val confidence: Float = 1.0f,
        override val source: InputSource = InputSource.VoiceCursor,
        override val timestamp: Long = currentTimeMillis()
    ) : InputEvent()

    /** VoiceCursor position changed */
    data class VoiceCursorMove(
        val position: Offset,
        override val source: InputSource = InputSource.VoiceCursor,
        override val timestamp: Long = currentTimeMillis()
    ) : InputEvent()

    /** VoiceCursor click (gaze or voice-triggered) */
    data class VoiceCursorClick(
        val clickType: ClickType,
        val position: Offset,
        override val source: InputSource = InputSource.VoiceCursor,
        override val timestamp: Long = currentTimeMillis()
    ) : InputEvent()

    /** VoiceCursor entered element bounds */
    data class VoiceCursorEnter(
        val position: Offset,
        override val source: InputSource = InputSource.VoiceCursor,
        override val timestamp: Long = currentTimeMillis()
    ) : InputEvent()

    /** VoiceCursor exited element bounds */
    data class VoiceCursorExit(
        val position: Offset,
        override val source: InputSource = InputSource.VoiceCursor,
        override val timestamp: Long = currentTimeMillis()
    ) : InputEvent()

    // ─────────────────────────────────────────────────────────────
    // Stylus Events
    // ─────────────────────────────────────────────────────────────

    /** Stylus pressure changed */
    data class StylusPressure(
        val position: Offset,
        val pressure: Float,  // 0.0 - 1.0
        val tilt: Float,      // degrees
        override val source: InputSource = InputSource.Stylus,
        override val timestamp: Long = currentTimeMillis()
    ) : InputEvent()

    /** Stylus button pressed */
    data class StylusButton(
        val button: Int,
        val isPressed: Boolean,
        override val source: InputSource = InputSource.Stylus,
        override val timestamp: Long = currentTimeMillis()
    ) : InputEvent()

    // ─────────────────────────────────────────────────────────────
    // Gamepad Events
    // ─────────────────────────────────────────────────────────────

    /** Gamepad button pressed/released */
    data class GamepadButton(
        val button: GamepadButton,
        val isPressed: Boolean,
        override val source: InputSource = InputSource.Gamepad,
        override val timestamp: Long = currentTimeMillis()
    ) : InputEvent()

    /** Gamepad analog stick moved */
    data class GamepadStick(
        val stick: GamepadStick,
        val x: Float,  // -1.0 to 1.0
        val y: Float,  // -1.0 to 1.0
        override val source: InputSource = InputSource.Gamepad,
        override val timestamp: Long = currentTimeMillis()
    ) : InputEvent()
}

// ═══════════════════════════════════════════════════════════════
// Supporting Types
// ═══════════════════════════════════════════════════════════════

enum class ScrollSource {
    MouseWheel,
    Trackpad,
    Touch,
    Keyboard,
    Voice,
    Programmatic
}

enum class ClickType {
    Single,
    Double,
    LongPress
}

enum class FocusSource {
    Keyboard,  // Tab navigation
    Mouse,     // Click to focus
    Touch,     // Tap to focus
    Voice,     // Voice command
    Programmatic  // Code-triggered
}

// ═══════════════════════════════════════════════════════════════
// Keyboard Types
// ═══════════════════════════════════════════════════════════════

/**
 * Platform-agnostic key identifiers.
 * Maps to native key codes on each platform.
 */
enum class Key {
    // Letters
    A, B, C, D, E, F, G, H, I, J, K, L, M,
    N, O, P, Q, R, S, T, U, V, W, X, Y, Z,

    // Numbers
    Num0, Num1, Num2, Num3, Num4,
    Num5, Num6, Num7, Num8, Num9,

    // Function keys
    F1, F2, F3, F4, F5, F6, F7, F8, F9, F10, F11, F12,

    // Navigation
    Tab, Enter, Escape, Space, Backspace, Delete,
    ArrowUp, ArrowDown, ArrowLeft, ArrowRight,
    Home, End, PageUp, PageDown,

    // Modifiers (for key identification, not as modifiers)
    ShiftLeft, ShiftRight,
    CtrlLeft, CtrlRight,
    AltLeft, AltRight,
    MetaLeft, MetaRight,  // Cmd on Mac, Win on Windows

    // Symbols
    Minus, Equals, BracketLeft, BracketRight,
    Backslash, Semicolon, Quote, Comma, Period, Slash,
    Grave,

    // Unknown
    Unknown
}

/**
 * Keyboard modifier state.
 */
data class KeyModifiers(
    val ctrl: Boolean = false,
    val alt: Boolean = false,
    val shift: Boolean = false,
    val meta: Boolean = false  // Cmd on Mac, Win on Windows
) {
    companion object {
        val None = KeyModifiers()
    }

    val hasAny: Boolean
        get() = ctrl || alt || shift || meta

    fun withCtrl() = copy(ctrl = true)
    fun withAlt() = copy(alt = true)
    fun withShift() = copy(shift = true)
    fun withMeta() = copy(meta = true)
}

// ═══════════════════════════════════════════════════════════════
// Gamepad Types
// ═══════════════════════════════════════════════════════════════

enum class GamepadButton {
    A, B, X, Y,
    LeftBumper, RightBumper,
    LeftTrigger, RightTrigger,
    DpadUp, DpadDown, DpadLeft, DpadRight,
    Start, Select, Home,
    LeftStickButton, RightStickButton,
    Unknown
}

enum class GamepadStick {
    Left, Right
}

// ═══════════════════════════════════════════════════════════════
// Utility
// ═══════════════════════════════════════════════════════════════

/** Platform-agnostic current time in milliseconds */
expect fun currentTimeMillis(): Long
