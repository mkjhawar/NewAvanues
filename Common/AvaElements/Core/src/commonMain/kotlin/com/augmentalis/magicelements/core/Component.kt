package com.augmentalis.avaelements.core

import kotlinx.serialization.Serializable
import com.augmentalis.avaelements.core.types.ComponentStyle
import com.augmentalis.avaelements.core.types.Color
import com.augmentalis.avaelements.core.types.Size
import com.augmentalis.avaelements.core.types.Spacing
import com.augmentalis.avaelements.core.types.Border
import com.augmentalis.avaelements.core.types.Shadow
import com.augmentalis.avaelements.core.types.CornerRadius

/**
 * Base component interface for all AvaElements UI components
 */
interface Component {
    val type: String
    val id: String?
    val style: ComponentStyle?
    val modifiers: List<Modifier>

    /**
     * Render this component to a platform-specific representation
     */
    fun render(renderer: Renderer): Any
}

/**
 * Note: ComponentStyle is defined in types/ package
 */

/**
 * Modifier system for declarative component enhancement
 */
sealed class Modifier {
    data class Padding(val spacing: Spacing) : Modifier()
    data class Background(val color: Color) : Modifier()
    data class BackgroundGradient(val gradient: Gradient) : Modifier()
    data class Border(val border: com.augmentalis.avaelements.core.types.Border) : Modifier()
    data class CornerRadius(val radius: com.augmentalis.avaelements.core.types.CornerRadius) : Modifier()
    data class Shadow(val shadow: com.augmentalis.avaelements.core.types.Shadow) : Modifier()
    data class Opacity(val value: Float) : Modifier()
    data class Size(val width: com.augmentalis.avaelements.core.types.Size?, val height: com.augmentalis.avaelements.core.types.Size?) : Modifier()
    data class Clickable(val onClick: () -> Unit) : Modifier()
    data class Hoverable(val onHover: (Boolean) -> Unit) : Modifier()
    data class Focusable(val onFocus: (Boolean) -> Unit) : Modifier()
    data class Animated(val transition: Transition) : Modifier()
    data class Align(val alignment: Alignment) : Modifier()
    data class Weight(val value: Float) : Modifier()  // For flex layouts
    data class ZIndex(val value: Int) : Modifier()
    data class Clip(val shape: ClipShape) : Modifier()
    data class Transform(val transformation: Transformation) : Modifier()
    object FillMaxWidth : Modifier()
    object FillMaxHeight : Modifier()
    object FillMaxSize : Modifier()

    sealed class ClipShape {
        data class Rectangle(val radius: com.augmentalis.avaelements.core.types.CornerRadius = com.augmentalis.avaelements.core.types.CornerRadius.Zero) : ClipShape()
        object Circle : ClipShape()
        data class RoundedRectangle(val radius: Float) : ClipShape()
    }

    sealed class Transformation {
        data class Rotate(val degrees: Float) : Transformation()
        data class Scale(val x: Float = 1f, val y: Float = 1f) : Transformation()
        data class Translate(val x: Float = 0f, val y: Float = 0f) : Transformation()
    }
}

/**
 * Platform-specific renderer interface
 */
interface Renderer {
    val platform: Platform

    /**
     * Render a component tree to platform-specific UI
     */
    fun render(component: Component): Any

    /**
     * Apply platform-specific theme
     */
    fun applyTheme(theme: Theme)

    enum class Platform {
        Android,
        iOS,
        macOS,
        Windows,
        Linux,
        Web,
        VisionOS,
        AndroidXR
    }
}

/**
 * Component builder scope for DSL
 */
abstract class ComponentScope {
    protected val modifiers = mutableListOf<Modifier>()

    fun padding(spacing: Spacing) {
        modifiers.add(Modifier.Padding(spacing))
    }

    fun padding(all: Float) {
        modifiers.add(Modifier.Padding(Spacing.all(all)))
    }

    fun padding(vertical: Float = 0f, horizontal: Float = 0f) {
        modifiers.add(Modifier.Padding(Spacing.symmetric(vertical, horizontal)))
    }

    fun background(color: Color) {
        modifiers.add(Modifier.Background(color))
    }

    fun background(gradient: Gradient) {
        modifiers.add(Modifier.BackgroundGradient(gradient))
    }

    fun border(width: Float = 1f, color: Color = Color.Black, radius: CornerRadius = CornerRadius.Zero) {
        modifiers.add(Modifier.Border(Border(width, color, radius)))
    }

    fun cornerRadius(radius: Float) {
        modifiers.add(Modifier.CornerRadius(com.augmentalis.avaelements.core.types.CornerRadius.all(radius)))
    }

    fun shadow(offsetX: Float = 0f, offsetY: Float = 4f, blurRadius: Float = 8f, color: Color = Color(0, 0, 0, 0.25f)) {
        modifiers.add(Modifier.Shadow(Shadow(offsetX, offsetY, blurRadius, color = color)))
    }

    fun opacity(value: Float) {
        modifiers.add(Modifier.Opacity(value))
    }

    fun size(width: Size? = null, height: Size? = null) {
        modifiers.add(Modifier.Size(width, height))
    }

    fun fillMaxWidth() {
        modifiers.add(Modifier.FillMaxWidth)
    }

    fun fillMaxHeight() {
        modifiers.add(Modifier.FillMaxHeight)
    }

    fun fillMaxSize() {
        modifiers.add(Modifier.FillMaxSize)
    }

    fun clickable(onClick: () -> Unit) {
        modifiers.add(Modifier.Clickable(onClick))
    }

    fun align(alignment: Alignment) {
        modifiers.add(Modifier.Align(alignment))
    }

    fun weight(value: Float) {
        modifiers.add(Modifier.Weight(value))
    }

    fun zIndex(value: Int) {
        modifiers.add(Modifier.ZIndex(value))
    }

    fun clip(shape: Modifier.ClipShape) {
        modifiers.add(Modifier.Clip(shape))
    }

    fun rotate(degrees: Float) {
        modifiers.add(Modifier.Transform(Modifier.Transformation.Rotate(degrees)))
    }

    fun scale(x: Float = 1f, y: Float = 1f) {
        modifiers.add(Modifier.Transform(Modifier.Transformation.Scale(x, y)))
    }
}
