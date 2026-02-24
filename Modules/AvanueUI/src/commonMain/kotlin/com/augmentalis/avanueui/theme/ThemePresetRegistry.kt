/**
 * ThemePresetRegistry.kt - Registry of all premade theme presets
 *
 * Each preset is a curated combination of the 3 AvanueUI axes + ThemeOverrides.
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 */
package com.augmentalis.avanueui.theme

import androidx.compose.ui.unit.dp

/**
 * Registry of all premade theme presets.
 * Each preset is a curated combination of the 3 AvanueUI axes + ThemeOverrides.
 */
object ThemePresetRegistry {

    val CUPERTINO = ThemePreset(
        id = "cupertino",
        displayName = "Cupertino",
        description = "Apple-inspired clean design with flat surfaces and hairline borders",
        palette = null,
        materialMode = MaterialMode.Cupertino,
        appearance = null,
        overrides = ThemeOverrides(
            cornerRadiusMd = 12.dp,
            defaultElevation = 0.dp,
            borderWidth = 0.33.dp,
            borderOpacity = 0.2f,
            springDamping = 0.85f,
            transitionDurationMs = 350
        )
    )

    val MOUNTAIN_VIEW = ThemePreset(
        id = "mountainview",
        displayName = "MountainView",
        description = "Google Material 3 Extended with tonal elevation",
        palette = null,
        materialMode = MaterialMode.MountainView,
        appearance = null,
        overrides = ThemeOverrides(
            cornerRadiusMd = 12.dp,
            cardElevation = 2.dp,
            transitionDurationMs = 300
        )
    )

    val MOUNTAIN_VIEW_XR = ThemePreset(
        id = "mountainview_xr",
        displayName = "MountainView XR",
        description = "Material Design for spatial computing with depth and orbiting panels",
        palette = null,
        materialMode = MaterialMode.MountainView,
        appearance = AppearanceMode.Dark,
        overrides = ThemeOverrides(
            cornerRadiusMd = 16.dp,
            cardElevation = 8.dp,
            panelCurvature = 0.3f,
            typographyScale = 1.1f,
            minTouchTarget = 80.dp,
            transitionDurationMs = 400
        )
    )

    val META_FACIAL = ThemePreset(
        id = "meta_facial",
        displayName = "MetaFacial",
        description = "Meta Horizon OS-inspired with curved panels and gesture-friendly targets",
        palette = AvanueColorPalette.LUNA,
        materialMode = MaterialMode.Glass,
        appearance = AppearanceMode.Dark,
        overrides = ThemeOverrides(
            cornerRadiusLg = 24.dp,
            panelCurvature = 0.5f,
            minTouchTarget = 56.dp,
            glassBlur = 16.dp,
            transitionDurationMs = 250
        )
    )

    val NEUMORPHIC = ThemePreset(
        id = "neumorphic",
        displayName = "Neumorphic",
        description = "Soft extruded elements with dual-direction shadows",
        palette = null,
        materialMode = MaterialMode.MountainView,
        appearance = null,
        overrides = ThemeOverrides(
            cornerRadiusMd = 16.dp,
            cornerRadiusLg = 24.dp,
            defaultElevation = 0.dp,
            dualShadow = true,
            lightShadowOffset = 6.dp,
            darkShadowOffset = 6.dp,
            lightShadowBlur = 16.dp,
            darkShadowBlur = 16.dp,
            borderWidth = 0.dp
        )
    )

    val VISION_OS = ThemePreset(
        id = "visionos",
        displayName = "VisionOS",
        description = "Apple Vision spatial glass with specular highlights and ambient tint",
        palette = AvanueColorPalette.HYDRA,
        materialMode = MaterialMode.Glass,
        appearance = AppearanceMode.Dark,
        overrides = ThemeOverrides(
            cornerRadiusMd = 16.dp,
            cornerRadiusLg = 28.dp,
            glassBlur = 24.dp,
            specularHighlight = true,
            ambientTint = true,
            cardElevation = 0.dp,
            panelCurvature = 0.15f,
            transitionDurationMs = 450,
            springDamping = 0.75f
        )
    )

    val LIQUID_UI = ThemePreset(
        id = "liquid_ui",
        displayName = "LiquidUI",
        description = "Fluid organic design with morphing animations and blob shapes",
        palette = AvanueColorPalette.HYDRA,
        materialMode = MaterialMode.Water,
        appearance = null,
        overrides = ThemeOverrides(
            cornerRadiusMd = 20.dp,
            cornerRadiusLg = 32.dp,
            springDamping = 0.6f,
            transitionDurationMs = 500,
            typographyScale = 1.05f
        )
    )

    /** All registered presets. */
    val ALL: List<ThemePreset> = listOf(
        CUPERTINO, MOUNTAIN_VIEW, MOUNTAIN_VIEW_XR,
        META_FACIAL, NEUMORPHIC, VISION_OS, LIQUID_UI
    )

    /** Find a preset by its ID. Returns null if not found. */
    fun findById(id: String): ThemePreset? = ALL.find { it.id == id }
}
