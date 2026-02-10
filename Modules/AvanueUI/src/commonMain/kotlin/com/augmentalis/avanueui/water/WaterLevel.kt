/**
 * WaterLevel.kt - Water effect intensity levels
 *
 * Maps to Apple's three Liquid Glass variants:
 * - REGULAR: Default, medium transparency, full refraction + specular + caustics
 * - CLEAR:   High transparency, reduced refraction, no caustics
 * - IDENTITY: No visual effect (accessibility: reduce transparency)
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 */
package com.augmentalis.avanueui.water

enum class WaterLevel {
    /** Default: medium transparency, full refraction + specular + caustics */
    REGULAR,
    /** High transparency, reduced refraction, no caustics */
    CLEAR,
    /** No visual effect — used when accessibility "reduce transparency" is enabled */
    IDENTITY
}
