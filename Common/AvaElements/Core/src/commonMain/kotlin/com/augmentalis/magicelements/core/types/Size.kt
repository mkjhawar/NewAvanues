package com.augmentalis.avaelements.core.types

import kotlinx.serialization.Serializable

/**
 * Size definition for component dimensions
 *
 * Supports various units and responsive sizing strategies.
 *
 * @since 2.0.0
 */
@Serializable
sealed class Size {
    /**
     * Fixed size with specific unit
     *
     * @param value Numeric value
     * @param unit Unit of measurement (DP, PT, PX, etc.)
     */
    @Serializable
    data class Fixed(val value: Float, val unit: Unit = Unit.DP) : Size() {
        init {
            require(value >= 0) { "Size value must be non-negative" }
        }
    }

    /**
     * Percentage-based size relative to parent
     *
     * @param value Percentage value (0-100)
     */
    @Serializable
    data class Percent(val value: Float) : Size() {
        init {
            require(value in 0.0f..100.0f) { "Percent must be 0-100" }
        }
    }

    /**
     * Automatic size based on content
     */
    @Serializable
    object Auto : Size()

    /**
     * Fill available space
     */
    @Serializable
    object Fill : Size()

    /**
     * Size unit enumeration
     */
    enum class Unit {
        /** Density-independent pixels (Android, cross-platform default) */
        DP,

        /** Points (iOS, macOS) */
        PT,

        /** Physical pixels */
        PX,

        /** Scalable pixels (for text) */
        SP,

        /** Relative to root font size (Web) */
        REM,

        /** Relative to parent font size (Web) */
        EM
    }

    companion object {
        /**
         * Create fixed size in DP
         */
        fun dp(value: Float) = Fixed(value, Unit.DP)

        /**
         * Create fixed size in PT
         */
        fun pt(value: Float) = Fixed(value, Unit.PT)

        /**
         * Create fixed size in PX
         */
        fun px(value: Float) = Fixed(value, Unit.PX)

        /**
         * Create percentage size
         */
        fun percent(value: Float) = Percent(value)
    }
}
