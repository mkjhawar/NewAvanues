package com.augmentalis.avaelements.core.types

import kotlinx.serialization.Serializable

/**
 * ComponentStyle
 *
 * Visual styling properties for components.
 * Provides a unified way to style components across platforms.
 */
@Serializable
data class ComponentStyle(
    /**
     * Background color
     */
    val backgroundColor: Color? = null,

    /**
     * Foreground/text color
     */
    val foregroundColor: Color? = null,

    /**
     * Border configuration
     */
    val border: Border? = null,

    /**
     * Shadow configuration
     */
    val shadow: Shadow? = null,

    /**
     * Padding (all sides)
     */
    val padding: Spacing? = null,

    /**
     * Margin (all sides)
     */
    val margin: Spacing? = null,

    /**
     * Width
     */
    val width: Size? = null,

    /**
     * Height
     */
    val height: Size? = null,

    /**
     * Minimum width
     */
    val minWidth: Size? = null,

    /**
     * Minimum height
     */
    val minHeight: Size? = null,

    /**
     * Maximum width
     */
    val maxWidth: Size? = null,

    /**
     * Maximum height
     */
    val maxHeight: Size? = null,

    /**
     * Corner radius
     */
    val cornerRadius: Float? = null,

    /**
     * Opacity (0.0 - 1.0)
     */
    val opacity: Float? = null,

    /**
     * Z-index for stacking order
     */
    val zIndex: Int? = null,

    /**
     * Custom CSS-like properties
     */
    val customProperties: Map<String, String> = emptyMap()
) {
    init {
        opacity?.let { require(it in 0f..1f) { "Opacity must be between 0.0 and 1.0" } }
        cornerRadius?.let { require(it >= 0f) { "Corner radius must be non-negative" } }
    }

    /**
     * Merge with another style
     * Properties from [other] override properties from this style
     */
    fun merge(other: ComponentStyle): ComponentStyle {
        return ComponentStyle(
            backgroundColor = other.backgroundColor ?: this.backgroundColor,
            foregroundColor = other.foregroundColor ?: this.foregroundColor,
            border = other.border ?: this.border,
            shadow = other.shadow ?: this.shadow,
            padding = other.padding ?: this.padding,
            margin = other.margin ?: this.margin,
            width = other.width ?: this.width,
            height = other.height ?: this.height,
            minWidth = other.minWidth ?: this.minWidth,
            minHeight = other.minHeight ?: this.minHeight,
            maxWidth = other.maxWidth ?: this.maxWidth,
            maxHeight = other.maxHeight ?: this.maxHeight,
            cornerRadius = other.cornerRadius ?: this.cornerRadius,
            opacity = other.opacity ?: this.opacity,
            zIndex = other.zIndex ?: this.zIndex,
            customProperties = this.customProperties + other.customProperties
        )
    }

    companion object {
        /**
         * Empty style (no properties set)
         */
        val Empty = ComponentStyle()

        /**
         * Default card style
         */
        val Card = ComponentStyle(
            backgroundColor = Color(0xFF, 0xFF, 0xFF),
            cornerRadius = 8f,
            shadow = Shadow(
                color = Color(0x00, 0x00, 0x00, 0.1f),
                offsetX = 0f,
                offsetY = 2f,
                blurRadius = 4f
            ),
            padding = Spacing.all(16f)
        )

        /**
         * Default button style
         */
        val Button = ComponentStyle(
            cornerRadius = 4f,
            padding = Spacing(
                left = 16f,
                right = 16f,
                top = 8f,
                bottom = 8f
            )
        )

        /**
         * Default input style
         */
        val Input = ComponentStyle(
            cornerRadius = 4f,
            padding = Spacing(
                left = 12f,
                right = 12f,
                top = 8f,
                bottom = 8f
            ),
            border = Border(
                width = 1f,
                color = Color(0xCC, 0xCC, 0xCC)
            )
        )
    }
}
