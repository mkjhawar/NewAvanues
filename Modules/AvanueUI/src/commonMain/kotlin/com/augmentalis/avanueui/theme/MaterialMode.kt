/**
 * MaterialMode.kt - Controls which visual effect layer to apply
 *
 * Part of the Unified Component Architecture (Phase 2) + Theme v5.0.
 * Consumers use AvanueCard, AvanueButton, etc. — the MaterialMode
 * determines whether Glass, Water, Cupertino, or MountainView rendering is used.
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 */
package com.augmentalis.avanueui.theme

/**
 * Visual rendering mode for unified Avanue* components.
 *
 * Each mode controls the "material" effect applied to surfaces:
 * - [Glass]: Frosted glass overlay via Modifier.glass()
 * - [Water]: Liquid refraction + caustics via Modifier.waterEffect()
 * - [Cupertino]: iOS-style flat surfaces, 0dp elevation, hairline dividers
 * - [MountainView]: Full M3 tonal elevation + standard shapes
 */
enum class MaterialMode(val displayName: String) {
    /** Frosted glass overlay (Modifier.glass) */
    Glass("Glass"),
    /** Liquid refraction + caustics (Modifier.waterEffect) */
    Water("Water"),
    /** iOS-style: flat, 0dp elevation, hairline dividers, continuous corners */
    Cupertino("Cupertino"),
    /** Full Material3: tonal elevation, standard shapes */
    MountainView("MountainView");

    companion object {
        val DEFAULT = Water

        fun fromString(value: String): MaterialMode {
            // Direct match by name (PascalCase)
            entries.find { it.name.equals(value, ignoreCase = true) }?.let { return it }
            // Migration: old "PLAIN" or "MOUNTAIN_VIEW" with underscores
            if (value.equals("PLAIN", ignoreCase = true)) return MountainView
            if (value.replace("_", "").equals("MOUNTAINVIEW", ignoreCase = true)) return MountainView
            return DEFAULT
        }
    }
}
