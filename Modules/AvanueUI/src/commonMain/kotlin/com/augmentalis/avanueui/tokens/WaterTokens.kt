/**
 * WaterTokens.kt - Static design parameters for AvanueWaterUI effects
 *
 * Apple Liquid Glass-inspired token set. These are universal constants
 * shared across all themes; theme-variable colors live in [AvanueWaterScheme].
 *
 * Three visual layers (Apple model): Highlight / Shadow / Illumination
 * Three WaterLevels: REGULAR (full effect), CLEAR (subtle), IDENTITY (disabled)
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 */
package com.augmentalis.avanueui.tokens

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

object WaterTokens {

    // --- Three visual layers (Apple: Highlight / Shadow / Illumination) ---
    const val highlightOpacity: Float = 0.25f
    const val shadowOpacity: Float = 0.30f
    const val illuminationOpacity: Float = 0.18f

    // --- Panel background opacity (main frosted-glass fill) ---
    const val panelOpacityRegular: Float = 0.82f
    const val panelOpacityClear: Float = 0.60f
    const val panelOpacityIdentity: Float = 0.0f

    // --- Refraction / Lensing (disabled: distorts content text) ---
    val refractionStrengthClear: Dp = 0.dp
    val refractionStrengthRegular: Dp = 0.dp
    const val refractionFrequency: Float = 0.03f

    // --- Specular highlight ---
    val specularRadius: Dp = 80.dp
    const val specularIntensity: Float = 0.15f
    const val specularFalloff: Float = 2.0f

    // --- Blur per WaterLevel (disabled: content must stay sharp) ---
    val blurClear: Dp = 0.dp
    val blurRegular: Dp = 0.dp
    val blurIdentity: Dp = 0.dp

    // --- Color accent overlay (subtle tint on top of panel) ---
    const val overlayOpacityClear: Float = 0.04f
    const val overlayOpacityRegular: Float = 0.08f
    const val overlayOpacityIdentity: Float = 0.0f

    // --- Interactive ---
    const val pressScaleFactor: Float = 0.96f
    const val pressScaleDuration: Int = 100
    const val shimmerDuration: Int = 2000
    val illuminationRadius: Dp = 80.dp

    // --- Caustics (animated ripple patterns) ---
    const val causticSpeed: Float = 1.5f
    const val causticIntensity: Float = 0.04f
    const val causticScale: Float = 0.02f

    // --- Border ---
    val borderWidth: Dp = 0.5.dp
    const val borderOpacityTop: Float = 0.25f
    const val borderOpacityBottom: Float = 0.10f

    // --- Morphing ---
    const val morphDuration: Int = 400
    val containerSpacing: Dp = 40.dp

    /**
     * String-based token resolution for DSL/JSON runtime.
     * IDs are prefixed with "water." (e.g., "water.blurRegular").
     */
    fun resolve(id: String): Any? = when (id) {
        "water.panelOpacityRegular" -> panelOpacityRegular
        "water.panelOpacityClear" -> panelOpacityClear
        "water.panelOpacityIdentity" -> panelOpacityIdentity
        "water.highlightOpacity" -> highlightOpacity
        "water.shadowOpacity" -> shadowOpacity
        "water.illuminationOpacity" -> illuminationOpacity
        "water.refractionStrengthClear" -> refractionStrengthClear
        "water.refractionStrengthRegular" -> refractionStrengthRegular
        "water.refractionFrequency" -> refractionFrequency
        "water.specularRadius" -> specularRadius
        "water.specularIntensity" -> specularIntensity
        "water.specularFalloff" -> specularFalloff
        "water.blurClear" -> blurClear
        "water.blurRegular" -> blurRegular
        "water.blurIdentity" -> blurIdentity
        "water.overlayOpacityClear" -> overlayOpacityClear
        "water.overlayOpacityRegular" -> overlayOpacityRegular
        "water.overlayOpacityIdentity" -> overlayOpacityIdentity
        "water.pressScaleFactor" -> pressScaleFactor
        "water.pressScaleDuration" -> pressScaleDuration
        "water.shimmerDuration" -> shimmerDuration
        "water.illuminationRadius" -> illuminationRadius
        "water.causticSpeed" -> causticSpeed
        "water.causticIntensity" -> causticIntensity
        "water.causticScale" -> causticScale
        "water.borderWidth" -> borderWidth
        "water.borderOpacityTop" -> borderOpacityTop
        "water.borderOpacityBottom" -> borderOpacityBottom
        "water.morphDuration" -> morphDuration
        "water.containerSpacing" -> containerSpacing
        else -> null
    }
}
