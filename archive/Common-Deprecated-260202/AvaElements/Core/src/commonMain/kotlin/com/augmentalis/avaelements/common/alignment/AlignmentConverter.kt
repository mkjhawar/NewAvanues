package com.augmentalis.avaelements.common.alignment

/**
 * Unified alignment and arrangement conversion logic.
 * Shared across Android, iOS, Desktop, and Web platforms.
 *
 * This eliminates duplicate conversion functions that were
 * previously copy-pasted across platform renderers.
 */

// ═══════════════════════════════════════════════════════════════
// Layout Direction
// ═══════════════════════════════════════════════════════════════

enum class LayoutDirection {
    Ltr,  // Left-to-right
    Rtl   // Right-to-left
}

val LayoutDirection.isRtl: Boolean get() = this == LayoutDirection.Rtl

// ═══════════════════════════════════════════════════════════════
// Alignment Types (Platform-agnostic)
// ═══════════════════════════════════════════════════════════════

enum class WrapAlignment {
    Start,
    End,
    Center,
    SpaceBetween,
    SpaceAround,
    SpaceEvenly
}

enum class MainAxisAlignment {
    Start,
    End,
    Center,
    SpaceBetween,
    SpaceAround,
    SpaceEvenly
}

enum class CrossAxisAlignment {
    Start,
    End,
    Center,
    Stretch,
    Baseline
}

enum class HorizontalAlignment {
    Start,
    End,
    Center
}

enum class VerticalAlignment {
    Top,
    Bottom,
    Center
}

// ═══════════════════════════════════════════════════════════════
// Arrangement Results (For platform mapping)
// ═══════════════════════════════════════════════════════════════

sealed class HorizontalArrangement {
    object Start : HorizontalArrangement()
    object End : HorizontalArrangement()
    object Center : HorizontalArrangement()
    object SpaceBetween : HorizontalArrangement()
    object SpaceAround : HorizontalArrangement()
    object SpaceEvenly : HorizontalArrangement()
    data class SpacedBy(val spacing: Float) : HorizontalArrangement()
}

sealed class VerticalArrangement {
    object Top : VerticalArrangement()
    object Bottom : VerticalArrangement()
    object Center : VerticalArrangement()
    object SpaceBetween : VerticalArrangement()
    object SpaceAround : VerticalArrangement()
    object SpaceEvenly : VerticalArrangement()
    data class SpacedBy(val spacing: Float) : VerticalArrangement()
}

// ═══════════════════════════════════════════════════════════════
// Strategy Interfaces for Extensibility (OCP Compliance)
// ═══════════════════════════════════════════════════════════════

/**
 * Strategy for custom horizontal arrangement conversion.
 *
 * Allows clients to extend alignment conversion behavior without
 * modifying AlignmentConverter source code.
 *
 * @return HorizontalArrangement if this strategy handles the alignment,
 *         null to fall back to default conversion.
 */
fun interface HorizontalArrangementStrategy {
    fun convert(alignment: WrapAlignment, layoutDirection: LayoutDirection): HorizontalArrangement?
}

/**
 * Strategy for custom vertical arrangement conversion.
 *
 * Allows clients to extend alignment conversion behavior without
 * modifying AlignmentConverter source code.
 *
 * @return VerticalArrangement if this strategy handles the alignment,
 *         null to fall back to default conversion.
 */
fun interface VerticalArrangementStrategy {
    fun convert(alignment: WrapAlignment): VerticalArrangement?
}

/**
 * Strategy for custom MainAxisAlignment to horizontal arrangement conversion.
 */
fun interface MainAxisHorizontalStrategy {
    fun convert(alignment: MainAxisAlignment, layoutDirection: LayoutDirection): HorizontalArrangement?
}

/**
 * Strategy for custom MainAxisAlignment to vertical arrangement conversion.
 */
fun interface MainAxisVerticalStrategy {
    fun convert(alignment: MainAxisAlignment): VerticalArrangement?
}

// ═══════════════════════════════════════════════════════════════
// Conversion Functions (Previously duplicated in each renderer)
// ═══════════════════════════════════════════════════════════════

/**
 * Central converter for alignment and arrangement conversions.
 *
 * Supports custom strategies for extensibility (OCP compliance):
 * - Custom strategies are tried first (in registration order)
 * - Falls back to default conversion logic
 *
 * Thread-safety note: Strategy registration is NOT thread-safe.
 * Register all strategies during initialization before concurrent use.
 */
object AlignmentConverter {

    private val customHorizontalStrategies = mutableListOf<HorizontalArrangementStrategy>()
    private val customVerticalStrategies = mutableListOf<VerticalArrangementStrategy>()
    private val customMainAxisHorizontalStrategies = mutableListOf<MainAxisHorizontalStrategy>()
    private val customMainAxisVerticalStrategies = mutableListOf<MainAxisVerticalStrategy>()

    /**
     * Register a custom horizontal arrangement strategy.
     *
     * Strategies are tried in registration order before default conversion.
     * NOT thread-safe - register during initialization only.
     */
    fun registerHorizontalStrategy(strategy: HorizontalArrangementStrategy) {
        customHorizontalStrategies.add(strategy)
    }

    /**
     * Register a custom vertical arrangement strategy.
     *
     * Strategies are tried in registration order before default conversion.
     * NOT thread-safe - register during initialization only.
     */
    fun registerVerticalStrategy(strategy: VerticalArrangementStrategy) {
        customVerticalStrategies.add(strategy)
    }

    /**
     * Register a custom MainAxisAlignment to horizontal strategy.
     *
     * NOT thread-safe - register during initialization only.
     */
    fun registerMainAxisHorizontalStrategy(strategy: MainAxisHorizontalStrategy) {
        customMainAxisHorizontalStrategies.add(strategy)
    }

    /**
     * Register a custom MainAxisAlignment to vertical strategy.
     *
     * NOT thread-safe - register during initialization only.
     */
    fun registerMainAxisVerticalStrategy(strategy: MainAxisVerticalStrategy) {
        customMainAxisVerticalStrategies.add(strategy)
    }

    /**
     * Clear all custom strategies (useful for testing).
     *
     * NOT thread-safe - call during initialization only.
     */
    fun clearCustomStrategies() {
        customHorizontalStrategies.clear()
        customVerticalStrategies.clear()
        customMainAxisHorizontalStrategies.clear()
        customMainAxisVerticalStrategies.clear()
    }

    /**
     * Converts WrapAlignment to HorizontalArrangement, respecting RTL.
     *
     * Tries custom strategies first, then falls back to default logic.
     *
     * Previously duplicated in:
     * - Android/FlutterParityLayoutMappers.kt (lines 443-455)
     * - Desktop/FlutterParityLayoutMappers.kt (lines 473-485)
     */
    fun wrapToHorizontal(
        alignment: WrapAlignment,
        layoutDirection: LayoutDirection
    ): HorizontalArrangement {
        // Try custom strategies first
        for (strategy in customHorizontalStrategies) {
            strategy.convert(alignment, layoutDirection)?.let { return it }
        }
        // Fall back to default
        return defaultWrapToHorizontal(alignment, layoutDirection)
    }

    /**
     * Converts WrapAlignment to VerticalArrangement.
     *
     * Tries custom strategies first, then falls back to default logic.
     */
    fun wrapToVertical(alignment: WrapAlignment): VerticalArrangement {
        // Try custom strategies first
        for (strategy in customVerticalStrategies) {
            strategy.convert(alignment)?.let { return it }
        }
        // Fall back to default
        return defaultWrapToVertical(alignment)
    }

    /**
     * Converts MainAxisAlignment to HorizontalArrangement, respecting RTL.
     *
     * Tries custom strategies first, then falls back to default logic.
     */
    fun mainAxisToHorizontal(
        alignment: MainAxisAlignment,
        layoutDirection: LayoutDirection
    ): HorizontalArrangement {
        // Try custom strategies first
        for (strategy in customMainAxisHorizontalStrategies) {
            strategy.convert(alignment, layoutDirection)?.let { return it }
        }
        // Fall back to default
        return defaultMainAxisToHorizontal(alignment, layoutDirection)
    }

    /**
     * Converts MainAxisAlignment to VerticalArrangement.
     *
     * Tries custom strategies first, then falls back to default logic.
     */
    fun mainAxisToVertical(alignment: MainAxisAlignment): VerticalArrangement {
        // Try custom strategies first
        for (strategy in customMainAxisVerticalStrategies) {
            strategy.convert(alignment)?.let { return it }
        }
        // Fall back to default
        return defaultMainAxisToVertical(alignment)
    }

    /**
     * Converts CrossAxisAlignment to VerticalAlignment.
     *
     * Not extensible - this conversion is straightforward and rarely needs customization.
     */
    fun crossAxisToVertical(alignment: CrossAxisAlignment): VerticalAlignment {
        return when (alignment) {
            CrossAxisAlignment.Start -> VerticalAlignment.Top
            CrossAxisAlignment.End -> VerticalAlignment.Bottom
            CrossAxisAlignment.Center -> VerticalAlignment.Center
            CrossAxisAlignment.Stretch -> VerticalAlignment.Center  // Stretch maps to fill
            CrossAxisAlignment.Baseline -> VerticalAlignment.Center // Fallback
        }
    }

    /**
     * Converts CrossAxisAlignment to HorizontalAlignment.
     *
     * Not extensible - this conversion is straightforward and rarely needs customization.
     */
    fun crossAxisToHorizontal(alignment: CrossAxisAlignment): HorizontalAlignment {
        return when (alignment) {
            CrossAxisAlignment.Start -> HorizontalAlignment.Start
            CrossAxisAlignment.End -> HorizontalAlignment.End
            CrossAxisAlignment.Center -> HorizontalAlignment.Center
            CrossAxisAlignment.Stretch -> HorizontalAlignment.Center
            CrossAxisAlignment.Baseline -> HorizontalAlignment.Center
        }
    }

    // ───────────────────────────────────────────────────────────────
    // Default Conversion Logic (Private)
    // ───────────────────────────────────────────────────────────────

    private fun defaultWrapToHorizontal(
        alignment: WrapAlignment,
        layoutDirection: LayoutDirection
    ): HorizontalArrangement {
        return when (alignment) {
            WrapAlignment.Start -> if (layoutDirection.isRtl) HorizontalArrangement.End else HorizontalArrangement.Start
            WrapAlignment.End -> if (layoutDirection.isRtl) HorizontalArrangement.Start else HorizontalArrangement.End
            WrapAlignment.Center -> HorizontalArrangement.Center
            WrapAlignment.SpaceBetween -> HorizontalArrangement.SpaceBetween
            WrapAlignment.SpaceAround -> HorizontalArrangement.SpaceAround
            WrapAlignment.SpaceEvenly -> HorizontalArrangement.SpaceEvenly
        }
    }

    private fun defaultWrapToVertical(alignment: WrapAlignment): VerticalArrangement {
        return when (alignment) {
            WrapAlignment.Start -> VerticalArrangement.Top
            WrapAlignment.End -> VerticalArrangement.Bottom
            WrapAlignment.Center -> VerticalArrangement.Center
            WrapAlignment.SpaceBetween -> VerticalArrangement.SpaceBetween
            WrapAlignment.SpaceAround -> VerticalArrangement.SpaceAround
            WrapAlignment.SpaceEvenly -> VerticalArrangement.SpaceEvenly
        }
    }

    private fun defaultMainAxisToHorizontal(
        alignment: MainAxisAlignment,
        layoutDirection: LayoutDirection
    ): HorizontalArrangement {
        return when (alignment) {
            MainAxisAlignment.Start -> if (layoutDirection.isRtl) HorizontalArrangement.End else HorizontalArrangement.Start
            MainAxisAlignment.End -> if (layoutDirection.isRtl) HorizontalArrangement.Start else HorizontalArrangement.End
            MainAxisAlignment.Center -> HorizontalArrangement.Center
            MainAxisAlignment.SpaceBetween -> HorizontalArrangement.SpaceBetween
            MainAxisAlignment.SpaceAround -> HorizontalArrangement.SpaceAround
            MainAxisAlignment.SpaceEvenly -> HorizontalArrangement.SpaceEvenly
        }
    }

    private fun defaultMainAxisToVertical(alignment: MainAxisAlignment): VerticalArrangement {
        return when (alignment) {
            MainAxisAlignment.Start -> VerticalArrangement.Top
            MainAxisAlignment.End -> VerticalArrangement.Bottom
            MainAxisAlignment.Center -> VerticalArrangement.Center
            MainAxisAlignment.SpaceBetween -> VerticalArrangement.SpaceBetween
            MainAxisAlignment.SpaceAround -> VerticalArrangement.SpaceAround
            MainAxisAlignment.SpaceEvenly -> VerticalArrangement.SpaceEvenly
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// Extension Functions for Convenience
// ═══════════════════════════════════════════════════════════════

fun WrapAlignment.toHorizontalArrangement(layoutDirection: LayoutDirection): HorizontalArrangement =
    AlignmentConverter.wrapToHorizontal(this, layoutDirection)

fun WrapAlignment.toVerticalArrangement(): VerticalArrangement =
    AlignmentConverter.wrapToVertical(this)

fun MainAxisAlignment.toHorizontalArrangement(layoutDirection: LayoutDirection): HorizontalArrangement =
    AlignmentConverter.mainAxisToHorizontal(this, layoutDirection)

fun MainAxisAlignment.toVerticalArrangement(): VerticalArrangement =
    AlignmentConverter.mainAxisToVertical(this)

fun CrossAxisAlignment.toVerticalAlignment(): VerticalAlignment =
    AlignmentConverter.crossAxisToVertical(this)

fun CrossAxisAlignment.toHorizontalAlignment(): HorizontalAlignment =
    AlignmentConverter.crossAxisToHorizontal(this)

// ═══════════════════════════════════════════════════════════════
// Strategy Builder for Custom Alignments
// ═══════════════════════════════════════════════════════════════

/**
 * Builder for creating and registering custom alignment strategies.
 *
 * Example usage:
 * ```kotlin
 * // Map a custom alignment value
 * AlignmentStrategies.mapHorizontal(
 *     from = WrapAlignment.Start,
 *     to = HorizontalArrangement.Center,
 *     forRtl = true
 * ).let { AlignmentConverter.registerHorizontalStrategy(it) }
 *
 * // Create conditional strategy
 * val strategy = HorizontalArrangementStrategy { alignment, layoutDirection ->
 *     if (someCondition) HorizontalArrangement.Custom else null
 * }
 * AlignmentConverter.registerHorizontalStrategy(strategy)
 * ```
 */
object AlignmentStrategies {

    /**
     * Create a strategy that maps a specific WrapAlignment value to HorizontalArrangement.
     *
     * @param from The WrapAlignment to intercept
     * @param to The HorizontalArrangement to return
     * @param forRtl If specified, only applies for RTL (true) or LTR (false) directions.
     *               If null, applies regardless of direction.
     *
     * @return Strategy that can be registered with AlignmentConverter
     */
    fun mapHorizontal(
        from: WrapAlignment,
        to: HorizontalArrangement,
        forRtl: Boolean? = null
    ): HorizontalArrangementStrategy {
        return HorizontalArrangementStrategy { alignment, layoutDirection ->
            if (alignment == from) {
                when (forRtl) {
                    true -> if (layoutDirection.isRtl) to else null
                    false -> if (!layoutDirection.isRtl) to else null
                    null -> to
                }
            } else {
                null
            }
        }
    }

    /**
     * Create a strategy that maps a specific WrapAlignment value to VerticalArrangement.
     *
     * @param from The WrapAlignment to intercept
     * @param to The VerticalArrangement to return
     *
     * @return Strategy that can be registered with AlignmentConverter
     */
    fun mapVertical(
        from: WrapAlignment,
        to: VerticalArrangement
    ): VerticalArrangementStrategy {
        return VerticalArrangementStrategy { alignment ->
            if (alignment == from) to else null
        }
    }

    /**
     * Create a strategy that maps a specific MainAxisAlignment value to HorizontalArrangement.
     *
     * @param from The MainAxisAlignment to intercept
     * @param to The HorizontalArrangement to return
     * @param forRtl If specified, only applies for RTL (true) or LTR (false) directions.
     *               If null, applies regardless of direction.
     *
     * @return Strategy that can be registered with AlignmentConverter
     */
    fun mapMainAxisHorizontal(
        from: MainAxisAlignment,
        to: HorizontalArrangement,
        forRtl: Boolean? = null
    ): MainAxisHorizontalStrategy {
        return MainAxisHorizontalStrategy { alignment, layoutDirection ->
            if (alignment == from) {
                when (forRtl) {
                    true -> if (layoutDirection.isRtl) to else null
                    false -> if (!layoutDirection.isRtl) to else null
                    null -> to
                }
            } else {
                null
            }
        }
    }

    /**
     * Create a strategy that maps a specific MainAxisAlignment value to VerticalArrangement.
     *
     * @param from The MainAxisAlignment to intercept
     * @param to The VerticalArrangement to return
     *
     * @return Strategy that can be registered with AlignmentConverter
     */
    fun mapMainAxisVertical(
        from: MainAxisAlignment,
        to: VerticalArrangement
    ): MainAxisVerticalStrategy {
        return MainAxisVerticalStrategy { alignment ->
            if (alignment == from) to else null
        }
    }

    /**
     * Create a strategy that swaps Start and End alignments (useful for testing mirroring).
     *
     * @return Strategy that swaps Start ↔ End for all alignments
     */
    fun createMirroredStrategy(): HorizontalArrangementStrategy {
        return HorizontalArrangementStrategy { alignment, _ ->
            when (alignment) {
                WrapAlignment.Start -> HorizontalArrangement.End
                WrapAlignment.End -> HorizontalArrangement.Start
                else -> null  // Use default for others
            }
        }
    }

    /**
     * Create a strategy that forces all alignments to Center (useful for debugging).
     *
     * @return Strategy that centers everything
     */
    fun createCenterEverythingStrategy(): HorizontalArrangementStrategy {
        return HorizontalArrangementStrategy { _, _ ->
            HorizontalArrangement.Center
        }
    }
}
