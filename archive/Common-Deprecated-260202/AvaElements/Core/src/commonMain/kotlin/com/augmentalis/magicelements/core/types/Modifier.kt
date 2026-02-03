package com.augmentalis.avaelements.core.types

import kotlinx.serialization.Serializable

/**
 * Modifier system for declarative component enhancement
 *
 * Modifiers provide a chainable way to apply styling, behavior, and transformations
 * to components without modifying their core definition.
 *
 * Inspired by Jetpack Compose's Modifier system but adapted for cross-platform use.
 *
 * @since 2.0.0
 */
sealed class Modifier {
    // Layout Modifiers
    data class Padding(val spacing: Spacing) : Modifier()
    data class Size(val width: com.augmentalis.avaelements.core.types.Size?, val height: com.augmentalis.avaelements.core.types.Size?) : Modifier()
    object FillMaxWidth : Modifier()
    object FillMaxHeight : Modifier()
    object FillMaxSize : Modifier()
    data class Weight(val value: Float) : Modifier() {
        init {
            require(value > 0) { "Weight must be positive" }
        }
    }
    data class Align(val alignment: Alignment) : Modifier()

    // Visual Modifiers
    data class Background(val color: Color) : Modifier()
    data class BackgroundGradient(val gradient: Gradient) : Modifier()
    data class Border(val border: com.augmentalis.avaelements.core.types.Border) : Modifier()
    data class CornerRadius(val radius: com.augmentalis.avaelements.core.types.CornerRadius) : Modifier()
    data class Shadow(val shadow: com.augmentalis.avaelements.core.types.Shadow) : Modifier()
    data class Opacity(val value: Float) : Modifier() {
        init {
            require(value in 0.0f..1.0f) { "Opacity must be 0.0-1.0" }
        }
    }

    // Interaction Modifiers
    data class Clickable(val onClick: () -> Unit) : Modifier()
    data class Hoverable(val onHover: (Boolean) -> Unit) : Modifier()
    data class Focusable(val onFocus: (Boolean) -> Unit) : Modifier()
    data class Draggable(val onDrag: (DragEvent) -> Unit) : Modifier()

    // Transform Modifiers
    data class Transform(val transformation: Transformation) : Modifier()
    data class Clip(val shape: ClipShape) : Modifier()
    data class ZIndex(val value: Int) : Modifier()

    // Animation Modifiers
    data class Animated(val transition: Transition) : Modifier()

    /**
     * Clipping shapes
     */
    sealed class ClipShape {
        data class Rectangle(val radius: com.augmentalis.avaelements.core.types.CornerRadius = com.augmentalis.avaelements.core.types.CornerRadius.Zero) : ClipShape()
        object Circle : ClipShape()
        data class RoundedRectangle(val radius: Float) : ClipShape()
        data class Ellipse(val radiusX: Float, val radiusY: Float) : ClipShape()
    }

    /**
     * Transformations
     */
    sealed class Transformation {
        data class Rotate(val degrees: Float) : Transformation()
        data class Scale(val x: Float = 1f, val y: Float = 1f) : Transformation() {
            init {
                require(x > 0) { "Scale X must be positive" }
                require(y > 0) { "Scale Y must be positive" }
            }
        }
        data class Translate(val x: Float = 0f, val y: Float = 0f) : Transformation()
        data class Skew(val x: Float = 0f, val y: Float = 0f) : Transformation()
    }

    companion object {
        /**
         * Combine multiple modifiers
         */
        fun combine(vararg modifiers: Modifier): List<Modifier> = modifiers.toList()
    }
}

/**
 * Drag event data
 */
data class DragEvent(
    val x: Float,
    val y: Float,
    val deltaX: Float,
    val deltaY: Float,
    val state: DragState
)

/**
 * Drag state
 */
enum class DragState {
    Started,
    Moved,
    Ended,
    Cancelled
}

/**
 * Alignment enumeration
 */
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

/**
 * Arrangement enumeration for layout distribution
 */
@Serializable
enum class Arrangement {
    /** Align items to the start */
    Start,

    /** Center items */
    Center,

    /** Align items to the end */
    End,

    /** Space between items with no space at edges */
    SpaceBetween,

    /** Space around items with half space at edges */
    SpaceAround,

    /** Even space between and around items */
    SpaceEvenly
}
