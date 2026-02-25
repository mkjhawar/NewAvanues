/**
 * ThemePreset.kt - Curated theme combination for AvanueUI v5.1
 *
 * Presets don't add new axes — they're opinionated selections within the
 * existing system (palette x materialMode x appearance) plus ThemeOverrides
 * for fine-tuning.
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 */
package com.augmentalis.avanueui.theme

import androidx.compose.runtime.Immutable

/**
 * A curated combination of AvanueUI v5.1 axis selections + style overrides.
 *
 * Presets don't add new axes — they're opinionated selections within the
 * existing system (palette x materialMode x appearance) plus ThemeOverrides
 * for fine-tuning.
 *
 * When [palette] is null, the user's current palette selection is kept.
 * When [appearance] is null, the user's current appearance setting is kept.
 * [materialMode] is always specified since it defines the preset's visual identity.
 */
@Immutable
data class ThemePreset(
    val id: String,
    val displayName: String,
    val description: String,
    val palette: AvanueColorPalette?,
    val materialMode: MaterialMode,
    val appearance: AppearanceMode?,
    val overrides: ThemeOverrides = ThemeOverrides.None,
)
