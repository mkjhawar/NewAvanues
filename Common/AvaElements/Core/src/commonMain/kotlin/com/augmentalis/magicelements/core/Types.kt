package com.augmentalis.avaelements.core

import kotlinx.serialization.Serializable

// Import types from the types package to avoid duplication
import com.augmentalis.avaelements.core.types.Color
import com.augmentalis.avaelements.core.types.Size
import com.augmentalis.avaelements.core.types.Spacing
import com.augmentalis.avaelements.core.types.Border
import com.augmentalis.avaelements.core.types.Shadow
import com.augmentalis.avaelements.core.types.CornerRadius

/**
 * AvaElements Type System
 *
 * Core type definitions for cross-platform UI components with support
 * for DSL and YAML serialization.
 *
 * Note: Color, Size, Spacing, Border, and Shadow are defined in the types/ package
 */

// ==================== Primitive Types ====================

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
// Note: Border, Shadow, and CornerRadius are defined in types/ package

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
