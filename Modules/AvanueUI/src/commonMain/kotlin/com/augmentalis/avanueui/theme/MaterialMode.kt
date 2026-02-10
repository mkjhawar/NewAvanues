/**
 * MaterialMode.kt - Controls which visual effect layer to apply
 *
 * Part of the Unified Component Architecture (Phase 2).
 * Consumers use AvanueCard, AvanueButton, etc. — the MaterialMode
 * determines whether Glass, Water, or Plain rendering is used.
 *
 * PLAIN mode uses standard Material3 with AvanueTheme colors today.
 * After a future Kotlin 2.1+ / Compose 1.9+ upgrade, PLAIN becomes
 * M3 Expressive — only the PLAIN branch implementations change,
 * zero consumer impact.
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 */
package com.augmentalis.avanueui.theme

/**
 * Visual rendering mode for unified Avanue* components.
 *
 * Each mode controls the "material" effect applied to surfaces:
 * - [GLASS]: Frosted glass overlay via [Modifier.glass()]
 * - [WATER]: Liquid refraction + caustics via [Modifier.waterEffect()]
 * - [PLAIN]: No effects, standard Material3 with AvanueTheme colors
 */
enum class MaterialMode {
    /** Frosted glass overlay (Modifier.glass) */
    GLASS,
    /** Liquid refraction + caustics (Modifier.waterEffect) */
    WATER,
    /** No effects, just Material3 with AvanueTheme colors */
    PLAIN
}
