/**
 * AvanuesSettingsCodecTest.kt - Unit tests for AvanuesSettingsCodec
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 */
package com.augmentalis.foundation.settings

import com.augmentalis.foundation.settings.models.AvanuesSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class AvanuesSettingsCodecTest {

    private fun freshStore() = InMemoryPreferenceStore()

    @Test
    fun defaultValue_matchesAvanuesSettingsDefaults() {
        val defaults = AvanuesSettingsCodec.defaultValue
        assertEquals(AvanuesSettings.DEFAULT_THEME_PALETTE, defaults.themePalette)
        assertEquals(AvanuesSettings.DEFAULT_THEME_STYLE, defaults.themeStyle)
        assertEquals(AvanuesSettings.DEFAULT_VOICE_LOCALE, defaults.voiceLocale)
    }

    @Test
    fun encodeDecodeRoundtrip_preservesAllFields() {
        val original = AvanuesSettings(
            cursorEnabled = true,
            dwellClickEnabled = false,
            dwellClickDelayMs = 800f,
            cursorSize = 32,
            cursorSpeed = 5,
            showCoordinates = true,
            cursorAccentOverride = 0xFF0000FFL,
            voiceFeedback = false,
            voiceLocale = "fr-FR",
            autoStartOnBoot = true,
            themePalette = "SOL",
            themeStyle = "Glass",
            themeAppearance = "Dark",
            voiceIsolationEnabled = false,
            voiceIsolationNoiseSuppression = false,
            voiceIsolationEchoCancellation = true,
            voiceIsolationAgc = false,
            voiceIsolationNsLevel = 0.9f,
            voiceIsolationGainLevel = 0.3f,
            voiceIsolationMode = "HIGH_QUALITY",
            vosSyncEnabled = true,
            vosSftpHost = "sync.example.com",
            vosSftpPort = 2222,
            vosSftpUsername = "mjhawar",
            vosSftpRemotePath = "/vos/profiles",
            vosSftpKeyPath = "/home/user/.ssh/id_rsa",
            vosLastSyncTime = 1_700_000_000L,
            vosSftpHostKeyMode = "strict",
            vosAutoSyncEnabled = true,
            vosSyncIntervalHours = 8
        )

        val store = freshStore()
        AvanuesSettingsCodec.encode(original, store)
        val decoded = AvanuesSettingsCodec.decode(store)

        assertEquals(original, decoded)
    }

    @Test
    fun encodeDecodeRoundtrip_nullCursorAccentAndSyncTime() {
        val original = AvanuesSettings(
            cursorAccentOverride = null,
            vosLastSyncTime = null
        )

        val store = freshStore()
        AvanuesSettingsCodec.encode(original, store)
        val decoded = AvanuesSettingsCodec.decode(store)

        assertNull(decoded.cursorAccentOverride)
        assertNull(decoded.vosLastSyncTime)
    }

    @Test
    fun decode_emptyStore_returnsDefaults() {
        val store = freshStore()
        val decoded = AvanuesSettingsCodec.decode(store)
        assertEquals(AvanuesSettings(), decoded)
    }

    @Test
    fun decode_emptyStore_returnsVoiceIsolationDefaults() {
        val store = freshStore()
        val decoded = AvanuesSettingsCodec.decode(store)
        assertEquals(true, decoded.voiceIsolationEnabled)
        assertEquals(true, decoded.voiceIsolationNoiseSuppression)
        assertEquals(false, decoded.voiceIsolationEchoCancellation)
        assertEquals(true, decoded.voiceIsolationAgc)
        assertEquals(0.7f, decoded.voiceIsolationNsLevel)
        assertEquals(0.5f, decoded.voiceIsolationGainLevel)
        assertEquals("BALANCED", decoded.voiceIsolationMode)
    }

    @Test
    fun decode_legacyOceanVariant_migratesCorrectly() {
        val store = freshStore()
        store.putString(SettingsKeys.THEME_VARIANT_LEGACY, "OCEAN")
        val decoded = AvanuesSettingsCodec.decode(store)
        assertEquals("LUNA", decoded.themePalette)
        assertEquals("Glass", decoded.themeStyle)
    }
}
