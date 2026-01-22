package com.augmentalis.avamagic.coretypes

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.jvm.JvmInline

/**
 * Core Types for IDEAMagic DSL
 *
 * Type-safe value classes for dimensions, colors, and sizes.
 * These provide compile-time safety and zero-cost abstractions.
 *
 * @author Manoj Jhawar, manoj@ideahq.net
 * @version 1.0.0
 */

// ===== DIMENSION TYPES =====

/**
 * Type-safe Dp (Density-independent pixels) value
 * Use for dimensions that should scale with screen density
 */
@JvmInline
value class MagicDp(val value: Dp) {
    companion object {
        val ZERO = MagicDp(0.dp)
        val INFINITY = MagicDp(Dp.Infinity)
        val UNSPECIFIED = MagicDp(Dp.Unspecified)
    }

    operator fun plus(other: MagicDp) = MagicDp(value + other.value)
    operator fun minus(other: MagicDp) = MagicDp(value - other.value)
    operator fun times(scale: Float) = MagicDp(value * scale)
    operator fun times(scale: Int) = MagicDp(value * scale)
    operator fun div(scale: Float) = MagicDp(value / scale)
    operator fun div(scale: Int) = MagicDp(value / scale)
    operator fun unaryMinus() = MagicDp(-value)

    operator fun compareTo(other: MagicDp): Int = value.compareTo(other.value)
}

/**
 * Type-safe Sp (Scalable pixels) value
 * Use for font sizes that should scale with user's font size preference
 */
@JvmInline
value class MagicSp(val value: TextUnit) {
    companion object {
        val ZERO = MagicSp(0.sp)
        val UNSPECIFIED = MagicSp(TextUnit.Unspecified)
    }

    operator fun times(scale: Float) = MagicSp(value * scale)
    operator fun times(scale: Int) = MagicSp(value * scale)
    operator fun div(scale: Float) = MagicSp(value / scale)
    operator fun div(scale: Int) = MagicSp(value / scale)
}

/**
 * Type-safe Px (Pixels) value
 * Use when you need exact pixel values (rare - prefer Dp)
 */
@JvmInline
value class MagicPx(val value: Float) {
    companion object {
        val ZERO = MagicPx(0f)
    }

    operator fun plus(other: MagicPx) = MagicPx(value + other.value)
    operator fun minus(other: MagicPx) = MagicPx(value - other.value)
    operator fun times(scale: Float) = MagicPx(value * scale)
    operator fun times(scale: Int) = MagicPx(value * scale)
    operator fun div(scale: Float) = MagicPx(value / scale)
    operator fun div(scale: Int) = MagicPx(value / scale)
    operator fun unaryMinus() = MagicPx(-value)

    operator fun compareTo(other: MagicPx): Int = value.compareTo(other.value)

    fun toDp(density: Float): MagicDp = MagicDp((value / density).dp)
}

// ===== COLOR TYPES =====

/**
 * Type-safe color value
 * Wraps Compose Color with additional utilities
 */
@JvmInline
value class MagicColor(val value: Color) {
    companion object {
        val Transparent = MagicColor(Color.Transparent)
        val Black = MagicColor(Color.Black)
        val DarkGray = MagicColor(Color.DarkGray)
        val Gray = MagicColor(Color.Gray)
        val LightGray = MagicColor(Color.LightGray)
        val White = MagicColor(Color.White)
        val Red = MagicColor(Color.Red)
        val Green = MagicColor(Color.Green)
        val Blue = MagicColor(Color.Blue)
        val Yellow = MagicColor(Color.Yellow)
        val Cyan = MagicColor(Color.Cyan)
        val Magenta = MagicColor(Color.Magenta)
        val Unspecified = MagicColor(Color.Unspecified)
    }

    /**
     * Create color from hex string (#RRGGBB or #AARRGGBB)
     */
    constructor(hex: String) : this(
        Color(parseHexColor(hex))
    )

    /**
     * Create color from ARGB components (0-255)
     */
    constructor(alpha: Int, red: Int, green: Int, blue: Int) : this(
        Color(red, green, blue, alpha)
    )

    /**
     * Create color from RGB components (0-255) with full opacity
     */
    constructor(red: Int, green: Int, blue: Int) : this(
        Color(red, green, blue)
    )

    /**
     * Copy with new alpha (0f-1f)
     */
    fun copy(alpha: Float = value.alpha): MagicColor {
        return MagicColor(value.copy(alpha = alpha))
    }

    /**
     * Get color with specified alpha (0f-1f)
     */
    fun withAlpha(alpha: Float): MagicColor {
        return MagicColor(value.copy(alpha = alpha))
    }
}

// ===== SIZE TYPES =====

/**
 * 2D size with width and height
 */
data class MagicSize(
    val width: MagicDp,
    val height: MagicDp
) {
    companion object {
        val ZERO = MagicSize(MagicDp.ZERO, MagicDp.ZERO)
        val UNSPECIFIED = MagicSize(MagicDp.UNSPECIFIED, MagicDp.UNSPECIFIED)
    }

    /**
     * Check if size is specified (not unspecified or zero)
     */
    val isSpecified: Boolean
        get() = width != MagicDp.UNSPECIFIED && height != MagicDp.UNSPECIFIED

    /**
     * Create square size
     */
    constructor(size: MagicDp) : this(size, size)
}

/**
 * Padding with separate values for each side
 */
data class MagicPadding(
    val start: MagicDp = MagicDp.ZERO,
    val top: MagicDp = MagicDp.ZERO,
    val end: MagicDp = MagicDp.ZERO,
    val bottom: MagicDp = MagicDp.ZERO
) {
    companion object {
        val ZERO = MagicPadding()

        /**
         * Uniform padding on all sides
         */
        fun all(value: MagicDp) = MagicPadding(value, value, value, value)

        /**
         * Horizontal padding (start and end)
         */
        fun horizontal(value: MagicDp) = MagicPadding(start = value, end = value)

        /**
         * Vertical padding (top and bottom)
         */
        fun vertical(value: MagicDp) = MagicPadding(top = value, bottom = value)

        /**
         * Symmetric padding (horizontal and vertical)
         */
        fun symmetric(
            horizontal: MagicDp = MagicDp.ZERO,
            vertical: MagicDp = MagicDp.ZERO
        ) = MagicPadding(
            start = horizontal,
            top = vertical,
            end = horizontal,
            bottom = vertical
        )
    }
}

/**
 * Border radius for rounded corners
 */
data class MagicBorderRadius(
    val topStart: MagicDp = MagicDp.ZERO,
    val topEnd: MagicDp = MagicDp.ZERO,
    val bottomStart: MagicDp = MagicDp.ZERO,
    val bottomEnd: MagicDp = MagicDp.ZERO
) {
    companion object {
        val ZERO = MagicBorderRadius()

        /**
         * Uniform radius on all corners
         */
        fun all(value: MagicDp) = MagicBorderRadius(value, value, value, value)

        /**
         * Fully rounded (pill shape)
         */
        fun circular() = MagicBorderRadius(
            topStart = MagicDp.INFINITY,
            topEnd = MagicDp.INFINITY,
            bottomStart = MagicDp.INFINITY,
            bottomEnd = MagicDp.INFINITY
        )
    }

    /**
     * Create uniform radius
     */
    constructor(radius: MagicDp) : this(radius, radius, radius, radius)
}

// ===== EXTENSION FUNCTIONS =====

/**
 * Convert Int to MagicDp
 * Usage: 16.dp
 */
val Int.magicDp: MagicDp
    get() = MagicDp(this.dp)

/**
 * Convert Float to MagicDp
 * Usage: 16.5f.dp
 */
val Float.magicDp: MagicDp
    get() = MagicDp(this.dp)

/**
 * Convert Int to MagicSp
 * Usage: 16.sp
 */
val Int.magicSp: MagicSp
    get() = MagicSp(this.sp)

/**
 * Convert Float to MagicSp
 * Usage: 16.5f.sp
 */
val Float.magicSp: MagicSp
    get() = MagicSp(this.sp)

/**
 * Convert Int to MagicPx
 * Usage: 100.px
 */
val Int.magicPx: MagicPx
    get() = MagicPx(this.toFloat())

/**
 * Convert Float to MagicPx
 * Usage: 100.5f.px
 */
val Float.magicPx: MagicPx
    get() = MagicPx(this)

/**
 * Parse hex color string to ULong
 */
private fun parseHexColor(hex: String): ULong {
    val cleanHex = hex.removePrefix("#")
    return when (cleanHex.length) {
        6 -> "FF$cleanHex".toULong(16) // RGB -> ARGB (full opacity)
        8 -> cleanHex.toULong(16)      // ARGB
        else -> throw IllegalArgumentException("Invalid hex color: $hex")
    }
}
