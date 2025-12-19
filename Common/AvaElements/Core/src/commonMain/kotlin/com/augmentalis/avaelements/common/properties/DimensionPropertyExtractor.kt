package com.augmentalis.avaelements.common.properties

/**
 * Dimension Property Extractor
 *
 * Handles extraction and parsing of dimensional values with units.
 * Part of the SOLID refactoring of PropertyExtractor.
 *
 * Responsibilities:
 * - Dimension value extraction with unit support
 * - Unit parsing (dp, sp, px, %)
 * - Dimension value conversion (to pixels, to dp)
 *
 * Supported units:
 * - dp: Density-independent pixels (default)
 * - sp: Scale-independent pixels (for text)
 * - px: Raw pixels
 * - %: Percentage of parent
 *
 * SRP: Single responsibility - dimension extraction and parsing only
 * ISP: Clients depend only on dimension extraction methods
 */
object DimensionPropertyExtractor {

    // ─────────────────────────────────────────────────────────────
    // Dimension Extraction
    // ─────────────────────────────────────────────────────────────

    /**
     * Parse dimension value (supports dp, sp, px, %)
     *
     * Supports:
     * - Number: Treated as dp
     * - String: Parsed with unit suffix (e.g., "16dp", "14sp", "100px", "50%")
     * - DimensionValue: Passed through
     *
     * @param props Property map
     * @param key Property key
     * @param default Default value in dp
     * @return DimensionValue with value and unit
     */
    fun getDimension(
        props: Map<String, Any?>,
        key: String,
        default: Float = 0f
    ): DimensionValue {
        return when (val value = props[key]) {
            is Number -> DimensionValue(value.toFloat(), DimensionUnit.Dp)
            is String -> parseDimension(value) ?: DimensionValue(default, DimensionUnit.Dp)
            is DimensionValue -> value
            else -> DimensionValue(default, DimensionUnit.Dp)
        }
    }

    // ─────────────────────────────────────────────────────────────
    // Dimension Parsing
    // ─────────────────────────────────────────────────────────────

    /**
     * Parse dimension string to DimensionValue
     *
     * Supports:
     * - "16dp" -> DimensionValue(16f, Dp)
     * - "14sp" -> DimensionValue(14f, Sp)
     * - "100px" -> DimensionValue(100f, Px)
     * - "50%" -> DimensionValue(0.5f, Percent)
     * - "16" -> DimensionValue(16f, Dp)
     *
     * @param value Dimension string to parse
     * @return DimensionValue or null if invalid
     */
    private fun parseDimension(value: String): DimensionValue? {
        val clean = value.trim().lowercase()

        return when {
            clean.endsWith("dp") -> {
                clean.removeSuffix("dp").toFloatOrNull()?.let {
                    DimensionValue(it, DimensionUnit.Dp)
                }
            }
            clean.endsWith("sp") -> {
                clean.removeSuffix("sp").toFloatOrNull()?.let {
                    DimensionValue(it, DimensionUnit.Sp)
                }
            }
            clean.endsWith("px") -> {
                clean.removeSuffix("px").toFloatOrNull()?.let {
                    DimensionValue(it, DimensionUnit.Px)
                }
            }
            clean.endsWith("%") -> {
                clean.removeSuffix("%").toFloatOrNull()?.let {
                    DimensionValue(it / 100f, DimensionUnit.Percent)
                }
            }
            else -> clean.toFloatOrNull()?.let { DimensionValue(it, DimensionUnit.Dp) }
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// Supporting Types
// ═══════════════════════════════════════════════════════════════

/**
 * Dimension value with unit
 *
 * Represents a dimensional value (e.g., width, height, margin, padding)
 * with its associated unit. Provides conversion methods for different
 * unit systems and screen densities.
 */
data class DimensionValue(
    val value: Float,
    val unit: DimensionUnit
) {
    /**
     * Convert to pixels given screen density
     *
     * @param density Screen density (pixels per dp)
     * @param fontScale Font scaling factor (for sp units)
     * @return Value in pixels
     */
    fun toPx(density: Float, fontScale: Float = 1f): Float {
        return when (unit) {
            DimensionUnit.Px -> value
            DimensionUnit.Dp -> value * density
            DimensionUnit.Sp -> value * density * fontScale
            DimensionUnit.Percent -> value  // Handled by layout
        }
    }

    /**
     * Convert to dp given screen density
     *
     * @param density Screen density (pixels per dp)
     * @return Value in dp
     */
    fun toDp(density: Float): Float {
        return when (unit) {
            DimensionUnit.Px -> value / density
            DimensionUnit.Dp -> value
            DimensionUnit.Sp -> value
            DimensionUnit.Percent -> value
        }
    }

    companion object {
        val Zero = DimensionValue(0f, DimensionUnit.Dp)

        fun dp(value: Float) = DimensionValue(value, DimensionUnit.Dp)
        fun sp(value: Float) = DimensionValue(value, DimensionUnit.Sp)
        fun px(value: Float) = DimensionValue(value, DimensionUnit.Px)
        fun percent(value: Float) = DimensionValue(value, DimensionUnit.Percent)
    }
}

/**
 * Dimension unit types
 *
 * Represents the different unit systems supported for dimensional values.
 */
enum class DimensionUnit {
    Dp,      // Density-independent pixels
    Sp,      // Scale-independent pixels (for text)
    Px,      // Raw pixels
    Percent  // Percentage of parent
}
