package com.augmentalis.avaelements.core.types

import kotlinx.serialization.Serializable

/**
 * Border definition for components
 *
 * Defines border width, color, radius, and style.
 *
 * @since 2.0.0
 */
@Serializable
data class Border(
    val width: Float = 1f,
    val color: Color = Color.Black,
    val radius: CornerRadius = CornerRadius.Zero,
    val style: Style = Style.Solid
) {
    init {
        require(width >= 0) { "Border width must be non-negative" }
    }

    /**
     * Border style enumeration
     */
    enum class Style {
        /** Solid continuous line */
        Solid,

        /** Dashed line */
        Dashed,

        /** Dotted line */
        Dotted,

        /** Double line */
        Double,

        /** No border */
        None
    }

    companion object {
        /**
         * No border
         */
        val None = Border(width = 0f, style = Style.None)

        /**
         * Thin solid border
         */
        fun thin(color: Color = Color.Black, radius: CornerRadius = CornerRadius.Zero) =
            Border(width = 1f, color = color, radius = radius, style = Style.Solid)

        /**
         * Medium solid border
         */
        fun medium(color: Color = Color.Black, radius: CornerRadius = CornerRadius.Zero) =
            Border(width = 2f, color = color, radius = radius, style = Style.Solid)

        /**
         * Thick solid border
         */
        fun thick(color: Color = Color.Black, radius: CornerRadius = CornerRadius.Zero) =
            Border(width = 4f, color = color, radius = radius, style = Style.Solid)
    }
}

/**
 * Corner radius definition
 *
 * Supports individual corner values or uniform radius.
 *
 * @since 2.0.0
 */
@Serializable
data class CornerRadius(
    val topLeft: Float = 0f,
    val topRight: Float = 0f,
    val bottomRight: Float = 0f,
    val bottomLeft: Float = 0f
) {
    init {
        require(topLeft >= 0) { "Top-left radius must be non-negative" }
        require(topRight >= 0) { "Top-right radius must be non-negative" }
        require(bottomRight >= 0) { "Bottom-right radius must be non-negative" }
        require(bottomLeft >= 0) { "Bottom-left radius must be non-negative" }
    }

    /**
     * Check if radius is zero on all corners
     */
    val isZero: Boolean
        get() = topLeft == 0f && topRight == 0f && bottomRight == 0f && bottomLeft == 0f

    companion object {
        /**
         * Create corner radius with same value for all corners
         *
         * @param value Radius value
         * @return CornerRadius instance
         */
        fun all(value: Float) = CornerRadius(value, value, value, value)

        /**
         * Create corner radius for only top corners
         *
         * @param value Radius value
         * @return CornerRadius instance
         */
        fun top(value: Float) = CornerRadius(topLeft = value, topRight = value)

        /**
         * Create corner radius for only bottom corners
         *
         * @param value Radius value
         * @return CornerRadius instance
         */
        fun bottom(value: Float) = CornerRadius(bottomLeft = value, bottomRight = value)

        /**
         * Create corner radius for left corners
         *
         * @param value Radius value
         * @return CornerRadius instance
         */
        fun left(value: Float) = CornerRadius(topLeft = value, bottomLeft = value)

        /**
         * Create corner radius for right corners
         *
         * @param value Radius value
         * @return CornerRadius instance
         */
        fun right(value: Float) = CornerRadius(topRight = value, bottomRight = value)

        /**
         * Predefined corner radius values
         */
        val Zero = CornerRadius(0f, 0f, 0f, 0f)
        val Small = all(4f)
        val Medium = all(8f)
        val Large = all(16f)
        val ExtraLarge = all(24f)
        val Circle = all(9999f) // Large enough to be circular
    }
}
