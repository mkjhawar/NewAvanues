/**
 * DeveloperSettingsCodecTest.kt - Unit tests for DeveloperSettingsCodec
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 */
package com.augmentalis.foundation.settings

import com.augmentalis.foundation.settings.models.DeveloperSettings
import kotlin.test.Test
import kotlin.test.assertEquals

class DeveloperSettingsCodecTest {

    private fun freshStore() = InMemoryPreferenceStore()

    @Test
    fun defaultValue_matchesDeveloperSettingsDefaults() {
        val defaults = DeveloperSettingsCodec.defaultValue
        assertEquals(DeveloperSettings.DEFAULT_STT_TIMEOUT_MS, defaults.sttTimeoutMs)
        assertEquals(DeveloperSettings.DEFAULT_CONFIDENCE_THRESHOLD, defaults.confidenceThreshold)
        assertEquals(DeveloperSettings.DEFAULT_STT_ENGINE, defaults.sttEngine)
    }

    @Test
    fun encodeDecodeRoundtrip_preservesAllFields() {
        val original = DeveloperSettings(
            sttTimeoutMs = 5000L,
            endOfSpeechDelayMs = 2000L,
            partialResultIntervalMs = 150L,
            confidenceThreshold = 0.7f,
            debugMode = false,
            verboseLogging = true,
            debugOverlay = true,
            scannerVerbosity = 3,
            autoStartListening = true,
            synonymsEnabled = false,
            sttEngine = "WHISPER",
            voiceLanguage = "de-DE",
            contentChangeDebounceMs = 500L,
            scrollEventDebounceMs = 200L,
            screenChangeDelayMs = 350L,
            developerModeActivated = true
        )

        val store = freshStore()
        DeveloperSettingsCodec.encode(original, store)
        val decoded = DeveloperSettingsCodec.decode(store)

        assertEquals(original, decoded)
    }

    @Test
    fun decode_emptyStore_returnsDefaults() {
        val store = freshStore()
        val decoded = DeveloperSettingsCodec.decode(store)
        assertEquals(DeveloperSettings(), decoded)
    }

    @Test
    fun encodeDecodeRoundtrip_extremeTimingValues() {
        val original = DeveloperSettings(
            sttTimeoutMs = Long.MAX_VALUE,
            endOfSpeechDelayMs = 0L,
            confidenceThreshold = 0.0f
        )

        val store = freshStore()
        DeveloperSettingsCodec.encode(original, store)
        val decoded = DeveloperSettingsCodec.decode(store)

        assertEquals(Long.MAX_VALUE, decoded.sttTimeoutMs)
        assertEquals(0L, decoded.endOfSpeechDelayMs)
        assertEquals(0.0f, decoded.confidenceThreshold)
    }
}
