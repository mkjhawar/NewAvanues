package com.augmentalis.avamagic.core

/**
 * Core types for MagicElements component system.
 *
 * These are stub interfaces that should be replaced with actual implementations
 * from a Core module when available. They provide the minimal contract needed
 * for the StateManagement reactive components.
 */

/**
 * Base interface for all MagicElements components.
 */
interface Component {
    val id: String?
    val style: ComponentStyle?
    val modifiers: List<Modifier>

    /**
     * Render this component using the provided renderer.
     */
    fun render(renderer: Renderer): Any
}

/**
 * Styling configuration for components.
 */
interface ComponentStyle {
    val background: String?
    val foreground: String?
    val padding: Int?
    val margin: Int?
    val borderRadius: Int?
    val borderWidth: Int?
    val borderColor: String?
}

/**
 * Simple component style implementation.
 */
data class SimpleComponentStyle(
    override val background: String? = null,
    override val foreground: String? = null,
    override val padding: Int? = null,
    override val margin: Int? = null,
    override val borderRadius: Int? = null,
    override val borderWidth: Int? = null,
    override val borderColor: String? = null
) : ComponentStyle

/**
 * Modifier for component transformations.
 */
interface Modifier {
    val type: String
    val value: Any?

    companion object {
        /**
         * Create an animated modifier with the given transition.
         */
        fun Animated(transition: Transition): Modifier = AnimatedModifier(transition)

        /**
         * Create a padding modifier.
         */
        fun padding(value: Int): Modifier = SimpleModifier("padding", value)

        /**
         * Create a margin modifier.
         */
        fun margin(value: Int): Modifier = SimpleModifier("margin", value)

        /**
         * Create a width modifier.
         */
        fun width(value: Int): Modifier = SimpleModifier("width", value)

        /**
         * Create a height modifier.
         */
        fun height(value: Int): Modifier = SimpleModifier("height", value)

        /**
         * Create a fill max width modifier.
         */
        fun fillMaxWidth(): Modifier = SimpleModifier("fillMaxWidth", true)

        /**
         * Create a fill max height modifier.
         */
        fun fillMaxHeight(): Modifier = SimpleModifier("fillMaxHeight", true)

        /**
         * Create a clickable modifier.
         */
        fun clickable(onClick: () -> Unit): Modifier = SimpleModifier("clickable", onClick)
    }
}

/**
 * Simple modifier implementation.
 */
data class SimpleModifier(
    override val type: String,
    override val value: Any? = null
) : Modifier

/**
 * Animated modifier for transition animations.
 */
data class AnimatedModifier(
    val transition: Transition
) : Modifier {
    override val type: String = "animated"
    override val value: Any? = transition
}

/**
 * Renderer interface for platform-specific rendering.
 */
interface Renderer {
    /**
     * Render a component and return the platform-specific result.
     */
    fun render(component: Component): Any

    /**
     * Get the renderer name/type.
     */
    val name: String

    /**
     * Get the platform this renderer targets.
     */
    val platform: Platform

    /**
     * Platform enumeration for conditional rendering.
     */
    enum class Platform {
        Android,
        iOS,
        Desktop,
        Web
    }
}

/**
 * Transition configuration for animations.
 */
interface Transition {
    val durationMs: Long
    val easing: Easing
    val delay: Long
}

/**
 * Simple transition implementation.
 */
data class SimpleTransition(
    override val durationMs: Long = 300,
    override val easing: Easing = Easing.EaseInOut,
    override val delay: Long = 0
) : Transition

/**
 * Easing functions for animations.
 */
enum class Easing {
    Linear,
    EaseIn,
    EaseOut,
    EaseInOut,
    Spring
}

/**
 * Common transitions.
 */
object Transitions {
    val fade = SimpleTransition(300, Easing.EaseInOut)
    val slide = SimpleTransition(250, Easing.EaseOut)
    val scale = SimpleTransition(200, Easing.EaseInOut)
    val spring = SimpleTransition(400, Easing.Spring)
    val none = SimpleTransition(0, Easing.Linear)

    fun custom(durationMs: Long, easing: Easing = Easing.EaseInOut, delay: Long = 0) =
        SimpleTransition(durationMs, easing, delay)
}

/**
 * Column layout component that arranges children vertically.
 */
class ColumnComponent(
    override val id: String? = null,
    override val style: ComponentStyle? = null,
    override val modifiers: List<Modifier> = emptyList(),
    val children: List<Component> = emptyList()
) : Component {
    override fun render(renderer: Renderer): Any {
        // Platform-specific column rendering
        return children.map { it.render(renderer) }
    }
}

/**
 * Row layout component that arranges children horizontally.
 */
class RowComponent(
    override val id: String? = null,
    override val style: ComponentStyle? = null,
    override val modifiers: List<Modifier> = emptyList(),
    val children: List<Component> = emptyList()
) : Component {
    override fun render(renderer: Renderer): Any {
        // Platform-specific row rendering
        return children.map { it.render(renderer) }
    }
}

/**
 * Text component for displaying text content.
 */
class TextComponent(
    val text: String,
    override val id: String? = null,
    override val style: ComponentStyle? = null,
    override val modifiers: List<Modifier> = emptyList()
) : Component {
    override fun render(renderer: Renderer): Any {
        return text
    }
}
