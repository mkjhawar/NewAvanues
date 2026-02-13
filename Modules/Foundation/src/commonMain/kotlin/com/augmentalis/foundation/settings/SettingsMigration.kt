/**
 * SettingsMigration.kt - Cross-platform theme settings migration
 *
 * Pure String→String migration functions for upgrading from legacy
 * theme_variant (AvanueThemeVariant enum) to the decoupled v5.1
 * triple-axis system (palette + style + appearance).
 *
 * These functions are platform-agnostic and used by all platform
 * persistence layers during settings read.
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 */
package com.augmentalis.foundation.settings

import com.augmentalis.foundation.settings.models.AvanuesSettings

/**
 * Theme migration utilities for v5.1 decoupled system.
 *
 * Converts legacy `AvanueThemeVariant` names to the new independent
 * palette and style axis values.
 *
 * Legacy mapping:
 * - OCEAN → palette=LUNA, style=Glass
 * - SUNSET → palette=SOL, style=Glass
 * - LIQUID → palette=HYDRA, style=Water
 */
object SettingsMigration {

    /**
     * Migrate old theme_variant to new palette string.
     *
     * @param variant Legacy variant name (OCEAN, SUNSET, LIQUID) or null
     * @return Palette name string (LUNA, SOL, HYDRA)
     */
    fun migrateVariantToPalette(variant: String?): String = when {
        variant.equals("OCEAN", ignoreCase = true) -> "LUNA"
        variant.equals("SUNSET", ignoreCase = true) -> "SOL"
        variant.equals("LIQUID", ignoreCase = true) -> "HYDRA"
        else -> AvanuesSettings.DEFAULT_THEME_PALETTE
    }

    /**
     * Migrate old theme_variant to new style string.
     *
     * @param variant Legacy variant name (OCEAN, SUNSET, LIQUID) or null
     * @return Style name string (Glass, Water)
     */
    fun migrateVariantToStyle(variant: String?): String = when {
        variant.equals("OCEAN", ignoreCase = true) -> "Glass"
        variant.equals("SUNSET", ignoreCase = true) -> "Glass"
        variant.equals("LIQUID", ignoreCase = true) -> "Water"
        else -> AvanuesSettings.DEFAULT_THEME_STYLE
    }
}
