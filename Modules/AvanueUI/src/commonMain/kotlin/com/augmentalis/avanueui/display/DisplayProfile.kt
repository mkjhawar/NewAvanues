package com.augmentalis.avanueui.display

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Layout strategy for different display form factors.
 *
 * Controls how screens organize content based on available space
 * and interaction model (touch, head gesture, voice).
 */
enum class LayoutStrategy {
    /** One item at a time, swipe/voice to navigate. Monocular micro displays. */
    SINGLE_PANE_PAGINATED,
    /** Scrollable single column. Compact smart glasses. */
    SINGLE_PANE_SCROLL,
    /** M3 WindowSizeClass adaptive layout. Phones, HD glasses. */
    ADAPTIVE,
    /** ListDetailPaneScaffold two-pane. Tablets, foldables. */
    LIST_DETAIL
}

/**
 * Display profiles controlling how dp/sp map to physical pixels.
 *
 * Compose's [LocalDensity] controls dp→px conversion. By overriding density per profile,
 * ALL existing token usage (SpacingTokens.md, SizeTokens.iconMd, etc.) automatically
 * scales with ZERO code changes to consumers.
 *
 * Example: SpacingTokens.md (16dp) renders as:
 * - 10dp physical on GLASS_MICRO (0.625x)
 * - 12dp physical on GLASS_COMPACT (0.75x)
 * - 16dp physical on PHONE (1.0x)
 *
 * @property densityScale Multiplied into LocalDensity.density
 * @property fontScale Multiplied into LocalDensity.fontScale
 * @property minTouchTarget Physical minimum touch/gesture target (NOT scaled)
 * @property layoutStrategy How screens should organize content
 */
enum class DisplayProfile(
    val densityScale: Float,
    val fontScale: Float,
    val minTouchTarget: Dp,
    val layoutStrategy: LayoutStrategy
) {
    /** Vuzix Blade 480x480, Vuzix Shield 640x360. Monocular micro displays. */
    GLASS_MICRO(
        densityScale = 0.625f,
        fontScale = 0.75f,
        minTouchTarget = 36.dp,
        layoutStrategy = LayoutStrategy.SINGLE_PANE_PAGINATED
    ),

    /** Vuzix M400 640x360, RealWear HMT 854x480. Compact assisted reality. */
    GLASS_COMPACT(
        densityScale = 0.75f,
        fontScale = 0.85f,
        minTouchTarget = 40.dp,
        layoutStrategy = LayoutStrategy.SINGLE_PANE_SCROLL
    ),

    /** RealWear Nav520 1280x720, Vuzix M4000. Standard assisted reality. */
    GLASS_STANDARD(
        densityScale = 0.875f,
        fontScale = 0.9f,
        minTouchTarget = 44.dp,
        layoutStrategy = LayoutStrategy.SINGLE_PANE_SCROLL
    ),

    /** Standard mobile phones (default). */
    PHONE(
        densityScale = 1.0f,
        fontScale = 1.0f,
        minTouchTarget = 48.dp,
        layoutStrategy = LayoutStrategy.ADAPTIVE
    ),

    /** Tablets, foldables. Same density, different layout. */
    TABLET(
        densityScale = 1.0f,
        fontScale = 1.0f,
        minTouchTarget = 48.dp,
        layoutStrategy = LayoutStrategy.LIST_DETAIL
    ),

    /** XREAL Air 1920x1080, Vuzix Z100. HD smart glasses. */
    GLASS_HD(
        densityScale = 0.9f,
        fontScale = 0.95f,
        minTouchTarget = 48.dp,
        layoutStrategy = LayoutStrategy.ADAPTIVE
    );

    /** Whether this profile targets a smart glass form factor. */
    val isGlass: Boolean
        get() = this == GLASS_MICRO || this == GLASS_COMPACT ||
                this == GLASS_STANDARD || this == GLASS_HD

    /** Whether this profile uses paginated (single-item) navigation. */
    val isPaginated: Boolean
        get() = layoutStrategy == LayoutStrategy.SINGLE_PANE_PAGINATED
}
