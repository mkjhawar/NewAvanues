package com.augmentalis.avaelements.common.spacing

/**
 * Universal Spacing and Sizing Utilities
 *
 * Platform-agnostic spacing, padding, and margin calculations
 * shared across Android, iOS, Desktop, and Web renderers.
 *
 * Eliminates duplicate edge insets and spacing calculations
 * that were previously copy-pasted across platform renderers.
 */

// ═══════════════════════════════════════════════════════════════
// Edge Insets (Padding/Margin)
// ═══════════════════════════════════════════════════════════════

/**
 * Platform-agnostic edge insets.
 * Converted to native types by each platform renderer:
 * - Android: PaddingValues
 * - iOS: EdgeInsets
 * - Desktop: Modifier.padding()
 * - Web: CSS padding/margin
 */
data class EdgeInsets(
    val start: Float = 0f,
    val top: Float = 0f,
    val end: Float = 0f,
    val bottom: Float = 0f
) {
    companion object {
        val Zero = EdgeInsets(0f, 0f, 0f, 0f)

        /**
         * Create symmetric edge insets
         */
        fun symmetric(horizontal: Float = 0f, vertical: Float = 0f) =
            EdgeInsets(horizontal, vertical, horizontal, vertical)

        /**
         * Create uniform edge insets
         */
        fun all(value: Float) = EdgeInsets(value, value, value, value)

        /**
         * Create only horizontal edge insets
         */
        fun horizontal(value: Float) = EdgeInsets(value, 0f, value, 0f)

        /**
         * Create only vertical edge insets
         */
        fun vertical(value: Float) = EdgeInsets(0f, value, 0f, value)

        /**
         * Create edge insets from a map
         */
        fun fromMap(map: Map<String, Any?>): EdgeInsets {
            return EdgeInsets(
                start = (map["start"] as? Number)?.toFloat()
                    ?: (map["left"] as? Number)?.toFloat()
                    ?: (map["horizontal"] as? Number)?.toFloat()
                    ?: (map["all"] as? Number)?.toFloat()
                    ?: 0f,
                top = (map["top"] as? Number)?.toFloat()
                    ?: (map["vertical"] as? Number)?.toFloat()
                    ?: (map["all"] as? Number)?.toFloat()
                    ?: 0f,
                end = (map["end"] as? Number)?.toFloat()
                    ?: (map["right"] as? Number)?.toFloat()
                    ?: (map["horizontal"] as? Number)?.toFloat()
                    ?: (map["all"] as? Number)?.toFloat()
                    ?: 0f,
                bottom = (map["bottom"] as? Number)?.toFloat()
                    ?: (map["vertical"] as? Number)?.toFloat()
                    ?: (map["all"] as? Number)?.toFloat()
                    ?: 0f
            )
        }
    }

    /** Total horizontal insets */
    val horizontalTotal: Float get() = start + end

    /** Total vertical insets */
    val verticalTotal: Float get() = top + bottom

    /** Add another EdgeInsets */
    operator fun plus(other: EdgeInsets) = EdgeInsets(
        start + other.start,
        top + other.top,
        end + other.end,
        bottom + other.bottom
    )

    /** Scale EdgeInsets by a factor */
    operator fun times(factor: Float) = EdgeInsets(
        start * factor,
        top * factor,
        end * factor,
        bottom * factor
    )

    /** Check if all values are zero */
    val isZero: Boolean
        get() = start == 0f && top == 0f && end == 0f && bottom == 0f
}

// ═══════════════════════════════════════════════════════════════
// Size
// ═══════════════════════════════════════════════════════════════

/**
 * Platform-agnostic size representation.
 */
data class Size(
    val width: Float,
    val height: Float
) {
    companion object {
        val Zero = Size(0f, 0f)
        val Unspecified = Size(Float.NaN, Float.NaN)

        /**
         * Create a square size
         */
        fun square(dimension: Float) = Size(dimension, dimension)
    }

    val isSpecified: Boolean
        get() = !width.isNaN() && !height.isNaN()

    val aspectRatio: Float
        get() = if (height != 0f) width / height else 0f

    operator fun times(factor: Float) = Size(width * factor, height * factor)
    operator fun div(factor: Float) = Size(width / factor, height / factor)
}

// ═══════════════════════════════════════════════════════════════
// Constraint Size
// ═══════════════════════════════════════════════════════════════

/**
 * Size constraints for layout (min/max width/height).
 */
data class SizeConstraints(
    val minWidth: Float = 0f,
    val maxWidth: Float = Float.POSITIVE_INFINITY,
    val minHeight: Float = 0f,
    val maxHeight: Float = Float.POSITIVE_INFINITY
) {
    companion object {
        val Unbounded = SizeConstraints()

        fun exact(width: Float, height: Float) = SizeConstraints(
            minWidth = width, maxWidth = width,
            minHeight = height, maxHeight = height
        )

        fun exactWidth(width: Float) = SizeConstraints(minWidth = width, maxWidth = width)
        fun exactHeight(height: Float) = SizeConstraints(minHeight = height, maxHeight = height)
    }

    fun constrain(size: Size): Size {
        return Size(
            width = size.width.coerceIn(minWidth, maxWidth),
            height = size.height.coerceIn(minHeight, maxHeight)
        )
    }

    val hasBoundedWidth: Boolean get() = maxWidth.isFinite()
    val hasBoundedHeight: Boolean get() = maxHeight.isFinite()
    val hasFixedWidth: Boolean get() = minWidth == maxWidth
    val hasFixedHeight: Boolean get() = minHeight == maxHeight
}

// ═══════════════════════════════════════════════════════════════
// Corner Radius
// ═══════════════════════════════════════════════════════════════

/**
 * Corner radius for rounded rectangles.
 */
data class CornerRadius(
    val topStart: Float = 0f,
    val topEnd: Float = 0f,
    val bottomStart: Float = 0f,
    val bottomEnd: Float = 0f
) {
    companion object {
        val Zero = CornerRadius(0f, 0f, 0f, 0f)

        /**
         * Create uniform corner radius
         */
        fun all(radius: Float) = CornerRadius(radius, radius, radius, radius)

        /**
         * Create corner radius from a single value or map
         */
        fun from(value: Any?): CornerRadius {
            return when (value) {
                is Number -> all(value.toFloat())
                is Map<*, *> -> {
                    @Suppress("UNCHECKED_CAST")
                    val map = value as Map<String, Any?>
                    CornerRadius(
                        topStart = (map["topStart"] as? Number)?.toFloat()
                            ?: (map["topLeft"] as? Number)?.toFloat()
                            ?: (map["top"] as? Number)?.toFloat()
                            ?: (map["all"] as? Number)?.toFloat()
                            ?: 0f,
                        topEnd = (map["topEnd"] as? Number)?.toFloat()
                            ?: (map["topRight"] as? Number)?.toFloat()
                            ?: (map["top"] as? Number)?.toFloat()
                            ?: (map["all"] as? Number)?.toFloat()
                            ?: 0f,
                        bottomStart = (map["bottomStart"] as? Number)?.toFloat()
                            ?: (map["bottomLeft"] as? Number)?.toFloat()
                            ?: (map["bottom"] as? Number)?.toFloat()
                            ?: (map["all"] as? Number)?.toFloat()
                            ?: 0f,
                        bottomEnd = (map["bottomEnd"] as? Number)?.toFloat()
                            ?: (map["bottomRight"] as? Number)?.toFloat()
                            ?: (map["bottom"] as? Number)?.toFloat()
                            ?: (map["all"] as? Number)?.toFloat()
                            ?: 0f
                    )
                }
                else -> Zero
            }
        }
    }

    /** Check if all corners have the same radius */
    val isUniform: Boolean
        get() = topStart == topEnd && topEnd == bottomStart && bottomStart == bottomEnd

    /** Get uniform radius (only valid if isUniform is true) */
    val uniform: Float
        get() = topStart

    /** Scale corner radius */
    operator fun times(factor: Float) = CornerRadius(
        topStart * factor,
        topEnd * factor,
        bottomStart * factor,
        bottomEnd * factor
    )
}

// ═══════════════════════════════════════════════════════════════
// Border
// ═══════════════════════════════════════════════════════════════

/**
 * Border configuration.
 */
data class Border(
    val width: Float = 0f,
    val color: Int = 0xFF000000.toInt(),  // ARGB
    val style: BorderStyle = BorderStyle.Solid
) {
    companion object {
        val None = Border(0f)

        fun solid(width: Float, color: Int) = Border(width, color, BorderStyle.Solid)
        fun dashed(width: Float, color: Int) = Border(width, color, BorderStyle.Dashed)
        fun dotted(width: Float, color: Int) = Border(width, color, BorderStyle.Dotted)
    }

    val isVisible: Boolean get() = width > 0f
}

enum class BorderStyle {
    Solid,
    Dashed,
    Dotted,
    None
}

// ═══════════════════════════════════════════════════════════════
// Shadow
// ═══════════════════════════════════════════════════════════════

/**
 * Shadow configuration for elevation effects.
 */
data class Shadow(
    val color: Int = 0x29000000,  // ARGB (semi-transparent black)
    val blurRadius: Float = 0f,
    val spreadRadius: Float = 0f,
    val offsetX: Float = 0f,
    val offsetY: Float = 0f
) {
    companion object {
        val None = Shadow()

        /**
         * Create elevation-based shadow (Material Design style)
         */
        fun elevation(dp: Float, color: Int = 0x29000000): Shadow {
            return Shadow(
                color = color,
                blurRadius = dp * 0.8f,
                spreadRadius = 0f,
                offsetX = 0f,
                offsetY = dp * 0.5f
            )
        }

        /**
         * Create from map
         */
        fun fromMap(map: Map<String, Any?>): Shadow {
            return Shadow(
                color = (map["color"] as? Number)?.toInt() ?: 0x29000000,
                blurRadius = (map["blur"] as? Number)?.toFloat()
                    ?: (map["blurRadius"] as? Number)?.toFloat()
                    ?: 0f,
                spreadRadius = (map["spread"] as? Number)?.toFloat()
                    ?: (map["spreadRadius"] as? Number)?.toFloat()
                    ?: 0f,
                offsetX = (map["x"] as? Number)?.toFloat()
                    ?: (map["offsetX"] as? Number)?.toFloat()
                    ?: 0f,
                offsetY = (map["y"] as? Number)?.toFloat()
                    ?: (map["offsetY"] as? Number)?.toFloat()
                    ?: 0f
            )
        }
    }

    val isVisible: Boolean get() = blurRadius > 0f || spreadRadius > 0f
}

// ═══════════════════════════════════════════════════════════════
// Spacing Scale
// ═══════════════════════════════════════════════════════════════

/**
 * Customizable spacing scale provider.
 * Allows projects to define their own spacing systems.
 *
 * SOLID Compliance:
 * - OCP: Open for extension via custom providers
 * - DIP: SpacingScale depends on abstraction
 */
interface SpacingScaleProvider {
    val base: Float
    val none: Float
    val xxs: Float
    val xs: Float
    val sm: Float
    val md: Float
    val lg: Float
    val xl: Float
    val xxl: Float
    val xxxl: Float

    /**
     * Get spacing by multiplier of base unit
     */
    fun get(multiplier: Float): Float = base * multiplier

    /**
     * Get spacing by name (case-insensitive)
     */
    fun byName(name: String): Float {
        return when (name.lowercase()) {
            "none", "0" -> none
            "xxs", "2xs" -> xxs
            "xs" -> xs
            "sm", "s", "small" -> sm
            "md", "m", "medium" -> md
            "lg", "l", "large" -> lg
            "xl" -> xl
            "xxl", "2xl" -> xxl
            "xxxl", "3xl" -> xxxl
            else -> name.toFloatOrNull() ?: md
        }
    }
}

/**
 * Default Material Design spacing scale (4dp base).
 *
 * SOLID Compliance:
 * - SRP: Single responsibility - Material spacing values
 * - OCP: Can be replaced without modifying SpacingScale
 */
object MaterialSpacingScale : SpacingScaleProvider {
    override val base = 4f
    override val none = 0f
    override val xxs = base * 0.5f   // 2dp
    override val xs = base * 1f      // 4dp
    override val sm = base * 2f      // 8dp
    override val md = base * 3f      // 12dp
    override val lg = base * 4f      // 16dp
    override val xl = base * 6f      // 24dp
    override val xxl = base * 8f     // 32dp
    override val xxxl = base * 12f   // 48dp
}

/**
 * Standard spacing scale for consistent layouts.
 * Based on 4dp base unit (Material Design) by default.
 *
 * Can be customized via setProvider() for different design systems.
 *
 * SOLID Compliance:
 * - OCP: Open for extension via custom providers, closed for modification
 * - DIP: Delegates to provider abstraction
 *
 * Example custom provider:
 * ```kotlin
 * object CustomSpacing : SpacingScaleProvider {
 *     override val base = 8f  // 8dp base instead of 4dp
 *     override val none = 0f
 *     override val xxs = base * 0.25f  // 2dp
 *     override val xs = base * 0.5f    // 4dp
 *     override val sm = base * 1f      // 8dp
 *     override val md = base * 2f      // 16dp
 *     override val lg = base * 3f      // 24dp
 *     override val xl = base * 4f      // 32dp
 *     override val xxl = base * 6f     // 48dp
 *     override val xxxl = base * 8f    // 64dp
 * }
 *
 * SpacingScale.setProvider(CustomSpacing)
 * ```
 */
object SpacingScale {
    private var provider: SpacingScaleProvider = MaterialSpacingScale

    /**
     * Set custom spacing scale provider.
     * Useful for non-Material design systems.
     *
     * @param customProvider Custom spacing scale implementation
     */
    fun setProvider(customProvider: SpacingScaleProvider) {
        provider = customProvider
    }

    /**
     * Reset to default Material Design spacing scale
     */
    fun resetToDefault() {
        provider = MaterialSpacingScale
    }

    /**
     * Get current provider (for testing/debugging)
     */
    fun getProvider(): SpacingScaleProvider = provider

    // ─────────────────────────────────────────────────────────────
    // Delegate to provider (backward compatibility)
    // ─────────────────────────────────────────────────────────────

    val Base: Float get() = provider.base
    val None: Float get() = provider.none
    val XXS: Float get() = provider.xxs
    val XS: Float get() = provider.xs
    val SM: Float get() = provider.sm
    val MD: Float get() = provider.md
    val LG: Float get() = provider.lg
    val XL: Float get() = provider.xl
    val XXL: Float get() = provider.xxl
    val XXXL: Float get() = provider.xxxl

    /**
     * Get spacing by multiplier of base unit
     */
    fun get(multiplier: Float): Float = provider.get(multiplier)

    /**
     * Get spacing by name (case-insensitive)
     */
    fun byName(name: String): Float = provider.byName(name)
}

// ═══════════════════════════════════════════════════════════════
// Extension Functions
// ═══════════════════════════════════════════════════════════════

/**
 * Convert number to EdgeInsets.all()
 */
fun Number.toEdgeInsets() = EdgeInsets.all(this.toFloat())

/**
 * Convert to Size
 */
fun Pair<Number, Number>.toSize() = Size(first.toFloat(), second.toFloat())
