package com.augmentalis.avanues.avamagic.components.core

import kotlinx.serialization.Serializable

/**
 * AvaElements Type System
 *
 * Core type definitions for cross-platform UI components with support
 * for DSL and YAML serialization.
 */

// ==================== Primitive Types ====================

@Serializable
data class Color(
    val red: Int,
    val green: Int,
    val blue: Int,
    val alpha: Float = 1.0f
) {
    init {
        require(red in 0..255) { "Red must be 0-255" }
        require(green in 0..255) { "Green must be 0-255" }
        require(blue in 0..255) { "Blue must be 0-255" }
        require(alpha in 0.0f..1.0f) { "Alpha must be 0.0-1.0" }
    }

    companion object {
        fun hex(value: String): Color {
            val hex = value.removePrefix("#")
            require(hex.length == 6 || hex.length == 8) { "Invalid hex color" }

            val r = hex.substring(0, 2).toInt(16)
            val g = hex.substring(2, 4).toInt(16)
            val b = hex.substring(4, 6).toInt(16)
            val a = if (hex.length == 8) hex.substring(6, 8).toInt(16) / 255f else 1.0f

            return Color(r, g, b, a)
        }

        val Transparent = Color(0, 0, 0, 0.0f)
        val Black = Color(0, 0, 0)
        val White = Color(255, 255, 255)
        val Red = Color(255, 0, 0)
        val Green = Color(0, 255, 0)
        val Blue = Color(0, 0, 255)
    }

    fun toHex(): String {
        val a = (alpha * 255).toInt()
        val r = red.toString(16).padStart(2, '0')
        val g = green.toString(16).padStart(2, '0')
        val b = blue.toString(16).padStart(2, '0')
        val aHex = a.toString(16).padStart(2, '0')
        return "#$r$g$b$aHex".uppercase()
    }
}

@Serializable
sealed class Size {
    @Serializable
    data class Fixed(val value: Float, val unit: Unit = Unit.DP) : Size()

    @Serializable
    data class Percent(val value: Float) : Size() {
        init {
            require(value in 0.0f..100.0f) { "Percent must be 0-100" }
        }
    }

    @Serializable
    object Auto : Size()

    @Serializable
    object Fill : Size()

    enum class Unit {
        DP,  // Density-independent pixels (Android, cross-platform default)
        PT,  // Points (iOS, macOS)
        PX,  // Physical pixels
        SP,  // Scalable pixels (for text)
        REM, // Relative to root font size (Web)
        EM   // Relative to parent font size (Web)
    }
}

@Serializable
data class Spacing(
    val top: Float = 0f,
    val right: Float = 0f,
    val bottom: Float = 0f,
    val left: Float = 0f,
    val unit: Size.Unit = Size.Unit.DP
) {
    companion object {
        fun all(value: Float, unit: Size.Unit = Size.Unit.DP) =
            Spacing(value, value, value, value, unit)

        fun symmetric(vertical: Float = 0f, horizontal: Float = 0f, unit: Size.Unit = Size.Unit.DP) =
            Spacing(vertical, horizontal, vertical, horizontal, unit)

        fun horizontal(value: Float, unit: Size.Unit = Size.Unit.DP) =
            Spacing(0f, value, 0f, value, unit)

        fun vertical(value: Float, unit: Size.Unit = Size.Unit.DP) =
            Spacing(value, 0f, value, 0f, unit)

        val Zero = Spacing(0f, 0f, 0f, 0f)
    }
}

@Serializable
data class Font(
    val family: String = "System",
    val size: Float = 16f,
    val weight: Weight = Weight.Regular,
    val style: Style = Style.Normal,
    val lineHeight: Float = size * 1.5f  // Default to 1.5x the font size
) {
    enum class Weight {
        Thin,        // 100
        ExtraLight,  // 200
        Light,       // 300
        Regular,     // 400
        Medium,      // 500
        SemiBold,    // 600
        Bold,        // 700
        ExtraBold,   // 800
        Black        // 900
    }

    enum class Style {
        Normal,
        Italic,
        Oblique
    }

    companion object {
        val System = Font()
        val Title = Font(size = 24f, weight = Weight.Bold)
        val Heading = Font(size = 20f, weight = Weight.SemiBold)
        val Body = Font(size = 16f, weight = Weight.Regular)
        val Caption = Font(size = 12f, weight = Weight.Regular)
    }
}

// ==================== Composite Types ====================

@Serializable
data class Border(
    val width: Float = 1f,
    val color: Color = Color.Black,
    val radius: CornerRadius = CornerRadius.Zero,
    val style: Style = Style.Solid
) {
    enum class Style {
        Solid,
        Dashed,
        Dotted,
        Double,
        None
    }
}

@Serializable
data class CornerRadius(
    val topLeft: Float = 0f,
    val topRight: Float = 0f,
    val bottomRight: Float = 0f,
    val bottomLeft: Float = 0f
) {
    companion object {
        fun all(value: Float) = CornerRadius(value, value, value, value)
        val Zero = CornerRadius(0f, 0f, 0f, 0f)
        val Small = all(4f)
        val Medium = all(8f)
        val Large = all(16f)
        val ExtraLarge = all(24f)
    }
}

@Serializable
data class Shadow(
    val offsetX: Float = 0f,
    val offsetY: Float = 4f,
    val blurRadius: Float = 8f,
    val spreadRadius: Float = 0f,
    val color: Color = Color(0, 0, 0, 0.25f)
)

@Serializable
sealed class Gradient {
    @Serializable
    data class Linear(
        val colors: List<ColorStop>,
        val angle: Float = 0f  // 0 = left to right, 90 = top to bottom
    ) : Gradient()

    @Serializable
    data class Radial(
        val colors: List<ColorStop>,
        val centerX: Float = 0.5f,
        val centerY: Float = 0.5f,
        val radius: Float = 1.0f
    ) : Gradient()

    @Serializable
    data class ColorStop(
        val color: Color,
        val position: Float  // 0.0 to 1.0
    ) {
        init {
            require(position in 0.0f..1.0f) { "Position must be 0.0-1.0" }
        }
    }
}

@Serializable
enum class Alignment {
    TopStart,
    TopCenter,
    TopEnd,
    CenterStart,
    Center,
    CenterEnd,
    BottomStart,
    BottomCenter,
    BottomEnd;

    companion object {
        val Start = CenterStart
        val End = CenterEnd
        val Top = TopCenter
        val Bottom = BottomCenter
    }
}

@Serializable
enum class Arrangement {
    Start,
    Center,
    End,
    SpaceBetween,
    SpaceAround,
    SpaceEvenly
}

// ==================== Layout Types ====================

@Serializable
data class Constraints(
    val minWidth: Size? = null,
    val maxWidth: Size? = null,
    val minHeight: Size? = null,
    val maxHeight: Size? = null
)

@Serializable
enum class Overflow {
    Visible,
    Hidden,
    Scroll,
    Clip
}

@Serializable
enum class Orientation {
    Horizontal,
    Vertical
}

// ==================== Animation Types ====================

@Serializable
data class Animation(
    val duration: Long = 300,  // milliseconds
    val easing: Easing = Easing.EaseInOut,
    val delay: Long = 0
) {
    enum class Easing {
        Linear,
        EaseIn,
        EaseOut,
        EaseInOut,
        EaseInBack,
        EaseOutBack,
        EaseInOutBack,
        Spring
    }
}

@Serializable
sealed class Transition {
    @Serializable
    data class Fade(val animation: Animation = Animation()) : Transition()

    @Serializable
    data class Scale(
        val from: Float = 0f,
        val to: Float = 1f,
        val animation: Animation = Animation()
    ) : Transition()

    @Serializable
    data class Slide(
        val direction: Direction,
        val animation: Animation = Animation()
    ) : Transition() {
        enum class Direction {
            Up, Down, Left, Right
        }
    }
}

// ==================== State Types ====================

@Serializable
enum class ComponentState {
    Default,
    Hover,
    Pressed,
    Focused,
    Disabled,
    Selected,
    Error
}

@Serializable
data class StateConfig<T>(
    val default: T,
    val hover: T? = null,
    val pressed: T? = null,
    val focused: T? = null,
    val disabled: T? = null,
    val selected: T? = null,
    val error: T? = null
) {
    fun get(state: ComponentState): T = when (state) {
        ComponentState.Default -> default
        ComponentState.Hover -> hover ?: default
        ComponentState.Pressed -> pressed ?: default
        ComponentState.Focused -> focused ?: default
        ComponentState.Disabled -> disabled ?: default
        ComponentState.Selected -> selected ?: default
        ComponentState.Error -> error ?: default
    }
}

// ==================== Position & Severity ====================

/**
 * Positioning and alignment options for UI components.
 *
 * Defines where a component should be positioned relative to its container
 * or anchor point. Supports 9 positions covering all corners, edges, and center.
 *
 * @since 1.0.0
 */
@Serializable
enum class Position {
    /** Top edge, horizontally centered */
    TOP,

    /** Bottom edge, horizontally centered */
    BOTTOM,

    /** Left edge, vertically centered */
    LEFT,

    /** Right edge, vertically centered */
    RIGHT,

    /** Center both horizontally and vertically */
    CENTER,

    /** Top-left corner */
    TOP_LEFT,

    /** Top-right corner */
    TOP_RIGHT,

    /** Bottom-left corner */
    BOTTOM_LEFT,

    /** Bottom-right corner */
    BOTTOM_RIGHT
}

/**
 * Severity levels for alerts, notifications, and status indicators.
 *
 * Provides semantic meaning for messages, typically mapped to colors
 * and icons in the UI (e.g., INFO = blue/i, ERROR = red/x).
 *
 * @since 1.0.0
 */
@Serializable
enum class Severity {
    /** Informational message (neutral) */
    INFO,

    /** Success or completion message (positive) */
    SUCCESS,

    /** Warning message (caution) */
    WARNING,

    /** Error message (problem) */
    ERROR,

    /** Critical error (urgent attention required) */
    CRITICAL
}

// ==================== Component Size & Variant ====================

/**
 * Standard component sizes (used by Avatar, Badge, Button, etc.)
 */
@Serializable
enum class ComponentSize {
    XS,   // Extra Small
    SM,   // Small
    MD,   // Medium (default)
    LG,   // Large
    XL    // Extra Large
}

/**
 * Component variants/styles (used by Badge, Chip, Button, etc.)
 */
@Serializable
enum class ComponentVariant {
    PRIMARY,
    SECONDARY,
    SUCCESS,
    WARNING,
    ERROR,
    INFO,
    LIGHT,
    DARK,
    OUTLINED,
    FILLED
}
