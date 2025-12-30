package com.augmentalis.avaelements.core.types

import kotlinx.serialization.Serializable

/**
 * Shadow definition for components
 *
 * Defines shadow offset, blur, spread, and color for elevation effects.
 *
 * @since 2.0.0
 */
@Serializable
data class Shadow(
    val offsetX: Float = 0f,
    val offsetY: Float = 4f,
    val blurRadius: Float = 8f,
    val spreadRadius: Float = 0f,
    val color: Color = Color(0, 0, 0, 0.25f)
) {
    init {
        require(blurRadius >= 0) { "Blur radius must be non-negative" }
        require(spreadRadius >= 0) { "Spread radius must be non-negative" }
    }

    companion object {
        /**
         * No shadow
         */
        val None = Shadow(offsetX = 0f, offsetY = 0f, blurRadius = 0f, color = Color.Transparent)

        /**
         * Small elevation shadow (2dp)
         */
        val Small = Shadow(
            offsetX = 0f,
            offsetY = 2f,
            blurRadius = 4f,
            spreadRadius = 0f,
            color = Color(0, 0, 0, 0.15f)
        )

        /**
         * Medium elevation shadow (4dp)
         */
        val Medium = Shadow(
            offsetX = 0f,
            offsetY = 4f,
            blurRadius = 8f,
            spreadRadius = 0f,
            color = Color(0, 0, 0, 0.20f)
        )

        /**
         * Large elevation shadow (8dp)
         */
        val Large = Shadow(
            offsetX = 0f,
            offsetY = 8f,
            blurRadius = 16f,
            spreadRadius = 0f,
            color = Color(0, 0, 0, 0.25f)
        )

        /**
         * Extra large elevation shadow (16dp)
         */
        val ExtraLarge = Shadow(
            offsetX = 0f,
            offsetY = 16f,
            blurRadius = 24f,
            spreadRadius = 0f,
            color = Color(0, 0, 0, 0.30f)
        )
    }
}

/**
 * Gradient definition for backgrounds
 *
 * Supports linear and radial gradients with color stops.
 *
 * @since 2.0.0
 */
@Serializable
sealed class Gradient {
    /**
     * Linear gradient
     *
     * @param colors List of color stops
     * @param angle Angle in degrees (0 = left to right, 90 = top to bottom)
     */
    @Serializable
    data class Linear(
        val colors: List<ColorStop>,
        val angle: Float = 0f
    ) : Gradient() {
        init {
            require(colors.size >= 2) { "Linear gradient requires at least 2 colors" }
        }
    }

    /**
     * Radial gradient
     *
     * @param colors List of color stops
     * @param centerX Center X position (0.0-1.0)
     * @param centerY Center Y position (0.0-1.0)
     * @param radius Radius (0.0-1.0, relative to smaller dimension)
     */
    @Serializable
    data class Radial(
        val colors: List<ColorStop>,
        val centerX: Float = 0.5f,
        val centerY: Float = 0.5f,
        val radius: Float = 1.0f
    ) : Gradient() {
        init {
            require(colors.size >= 2) { "Radial gradient requires at least 2 colors" }
            require(centerX in 0.0f..1.0f) { "Center X must be 0.0-1.0" }
            require(centerY in 0.0f..1.0f) { "Center Y must be 0.0-1.0" }
            require(radius in 0.0f..1.0f) { "Radius must be 0.0-1.0" }
        }
    }

    /**
     * Color stop definition
     *
     * @param color Color at this stop
     * @param position Position along gradient (0.0-1.0)
     */
    @Serializable
    data class ColorStop(
        val color: Color,
        val position: Float
    ) {
        init {
            require(position in 0.0f..1.0f) { "Position must be 0.0-1.0" }
        }
    }

    companion object {
        /**
         * Create simple two-color linear gradient
         */
        fun linear(startColor: Color, endColor: Color, angle: Float = 0f) = Linear(
            colors = listOf(
                ColorStop(startColor, 0f),
                ColorStop(endColor, 1f)
            ),
            angle = angle
        )

        /**
         * Create simple two-color radial gradient
         */
        fun radial(centerColor: Color, edgeColor: Color) = Radial(
            colors = listOf(
                ColorStop(centerColor, 0f),
                ColorStop(edgeColor, 1f)
            )
        )
    }
}

/**
 * Transition definition for animations
 *
 * @since 2.0.0
 */
@Serializable
sealed class Transition {
    /**
     * Fade transition
     */
    @Serializable
    data class Fade(val animation: Animation = Animation()) : Transition()

    /**
     * Scale transition
     */
    @Serializable
    data class Scale(
        val from: Float = 0f,
        val to: Float = 1f,
        val animation: Animation = Animation()
    ) : Transition()

    /**
     * Slide transition
     */
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

/**
 * Animation configuration
 *
 * @since 2.0.0
 */
@Serializable
data class Animation(
    val duration: Long = 300,  // milliseconds
    val easing: Easing = Easing.EaseInOut,
    val delay: Long = 0
) {
    init {
        require(duration > 0) { "Duration must be positive" }
        require(delay >= 0) { "Delay must be non-negative" }
    }

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
