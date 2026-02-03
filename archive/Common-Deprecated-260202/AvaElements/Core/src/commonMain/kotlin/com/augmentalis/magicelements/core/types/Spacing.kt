package com.augmentalis.avaelements.core.types

import kotlinx.serialization.Serializable

/**
 * Spacing definition for padding and margin
 *
 * Supports individual edge values and convenience constructors for common patterns.
 *
 * @since 2.0.0
 */
@Serializable
data class Spacing(
    val top: Float = 0f,
    val right: Float = 0f,
    val bottom: Float = 0f,
    val left: Float = 0f,
    val unit: Size.Unit = Size.Unit.DP
) {
    init {
        require(top >= 0) { "Top spacing must be non-negative" }
        require(right >= 0) { "Right spacing must be non-negative" }
        require(bottom >= 0) { "Bottom spacing must be non-negative" }
        require(left >= 0) { "Left spacing must be non-negative" }
    }

    /**
     * Check if spacing is zero on all sides
     */
    val isZero: Boolean
        get() = top == 0f && right == 0f && bottom == 0f && left == 0f

    companion object {
        /**
         * Create spacing with same value on all sides
         *
         * @param value Spacing value
         * @param unit Unit of measurement
         * @return Spacing instance
         */
        fun all(value: Float, unit: Size.Unit = Size.Unit.DP) =
            Spacing(value, value, value, value, unit)

        /**
         * Create spacing with symmetric vertical and horizontal values
         *
         * @param vertical Top and bottom spacing
         * @param horizontal Left and right spacing
         * @param unit Unit of measurement
         * @return Spacing instance
         */
        fun symmetric(
            vertical: Float = 0f,
            horizontal: Float = 0f,
            unit: Size.Unit = Size.Unit.DP
        ) = Spacing(vertical, horizontal, vertical, horizontal, unit)

        /**
         * Create spacing with only horizontal values (left and right)
         *
         * @param value Left and right spacing
         * @param unit Unit of measurement
         * @return Spacing instance
         */
        fun horizontal(value: Float, unit: Size.Unit = Size.Unit.DP) =
            Spacing(0f, value, 0f, value, unit)

        /**
         * Create spacing with only vertical values (top and bottom)
         *
         * @param value Top and bottom spacing
         * @param unit Unit of measurement
         * @return Spacing instance
         */
        fun vertical(value: Float, unit: Size.Unit = Size.Unit.DP) =
            Spacing(value, 0f, value, 0f, unit)

        /**
         * Create spacing with individual values for each side
         *
         * @param top Top spacing
         * @param right Right spacing
         * @param bottom Bottom spacing
         * @param left Left spacing
         * @param unit Unit of measurement
         * @return Spacing instance
         */
        fun of(
            top: Float = 0f,
            right: Float = 0f,
            bottom: Float = 0f,
            left: Float = 0f,
            unit: Size.Unit = Size.Unit.DP
        ) = Spacing(top, right, bottom, left, unit)

        /**
         * Zero spacing (no padding/margin)
         */
        val Zero = Spacing(0f, 0f, 0f, 0f)

        /**
         * Predefined spacing values (Material Design spec)
         */
        val None = Zero
        val XSmall = all(4f)
        val Small = all(8f)
        val Medium = all(16f)
        val Large = all(24f)
        val XLarge = all(32f)
        val XXLarge = all(48f)
    }
}
