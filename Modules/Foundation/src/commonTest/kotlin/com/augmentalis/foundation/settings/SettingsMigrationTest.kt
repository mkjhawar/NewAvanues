/**
 * SettingsMigrationTest.kt - Unit tests for SettingsMigration
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 */
package com.augmentalis.foundation.settings

import com.augmentalis.foundation.settings.models.AvanuesSettings
import kotlin.test.Test
import kotlin.test.assertEquals

class SettingsMigrationTest {

    // -------------------------------------------------------------------------
    // migrateVariantToPalette
    // -------------------------------------------------------------------------

    @Test
    fun migrateVariantToPalette_ocean_returnsLuna() {
        assertEquals("LUNA", SettingsMigration.migrateVariantToPalette("OCEAN"))
    }

    @Test
    fun migrateVariantToPalette_sunset_returnsSol() {
        assertEquals("SOL", SettingsMigration.migrateVariantToPalette("SUNSET"))
    }

    @Test
    fun migrateVariantToPalette_liquid_returnsHydra() {
        assertEquals("HYDRA", SettingsMigration.migrateVariantToPalette("LIQUID"))
    }

    @Test
    fun migrateVariantToPalette_unknownOrNull_returnsDefault() {
        assertEquals(AvanuesSettings.DEFAULT_THEME_PALETTE, SettingsMigration.migrateVariantToPalette(null))
        assertEquals(AvanuesSettings.DEFAULT_THEME_PALETTE, SettingsMigration.migrateVariantToPalette(""))
        assertEquals(AvanuesSettings.DEFAULT_THEME_PALETTE, SettingsMigration.migrateVariantToPalette("UNKNOWN_VARIANT"))
    }

    @Test
    fun migrateVariantToPalette_isCaseInsensitive() {
        assertEquals("LUNA", SettingsMigration.migrateVariantToPalette("ocean"))
        assertEquals("SOL", SettingsMigration.migrateVariantToPalette("Sunset"))
    }

    // -------------------------------------------------------------------------
    // migrateVariantToStyle
    // -------------------------------------------------------------------------

    @Test
    fun migrateVariantToStyle_ocean_returnsGlass() {
        assertEquals("Glass", SettingsMigration.migrateVariantToStyle("OCEAN"))
    }

    @Test
    fun migrateVariantToStyle_sunset_returnsGlass() {
        assertEquals("Glass", SettingsMigration.migrateVariantToStyle("SUNSET"))
    }

    @Test
    fun migrateVariantToStyle_liquid_returnsWater() {
        assertEquals("Water", SettingsMigration.migrateVariantToStyle("LIQUID"))
    }

    @Test
    fun migrateVariantToStyle_unknownOrNull_returnsDefault() {
        assertEquals(AvanuesSettings.DEFAULT_THEME_STYLE, SettingsMigration.migrateVariantToStyle(null))
        assertEquals(AvanuesSettings.DEFAULT_THEME_STYLE, SettingsMigration.migrateVariantToStyle("RANDOM"))
    }
}
