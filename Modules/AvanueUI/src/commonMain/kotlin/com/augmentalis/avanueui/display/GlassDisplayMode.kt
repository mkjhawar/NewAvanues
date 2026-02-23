package com.augmentalis.avanueui.display

/**
 * Determines rendering strategy based on display hardware type.
 *
 * - FLAT_SCREEN: Standard LCD/OLED (phones, tablets) — PseudoSpatial rendering, appearance-aware colors
 * - SEE_THROUGH: Optical waveguide (Vuzix M4000, RealWear) — True Spatial, colorsXR (additive-optimized)
 * - OPAQUE_GLASS: Micro-display (Vuzix Blade, XReal Air) — Spatial, standard dark theme
 */
enum class GlassDisplayMode {
    FLAT_SCREEN,
    SEE_THROUGH,
    OPAQUE_GLASS;

    /** Whether this display supports spatial rendering (head-tracking, depth layers). */
    val isSpatial: Boolean get() = this != FLAT_SCREEN

    /** Whether this display uses additive light (text/graphics glow on transparent background). */
    val isAdditive: Boolean get() = this == SEE_THROUGH

    /** Whether to use XR-optimized color tokens (boosted luminance for waveguide visibility). */
    val useXRColors: Boolean get() = this == SEE_THROUGH
}
