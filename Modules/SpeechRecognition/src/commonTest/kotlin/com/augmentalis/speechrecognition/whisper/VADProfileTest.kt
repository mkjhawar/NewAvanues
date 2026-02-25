package com.augmentalis.speechrecognition.whisper

import com.augmentalis.speechrecognition.SpeechMode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class VADProfileTest {

    @Test
    fun defaultProfileIsConversation() {
        assertEquals(VADProfile.CONVERSATION, VADProfile.DEFAULT)
    }

    @Test
    fun commandProfileHasShortestSilenceTimeout() {
        assertTrue(VADProfile.COMMAND.silenceTimeoutMs < VADProfile.CONVERSATION.silenceTimeoutMs)
        assertTrue(VADProfile.CONVERSATION.silenceTimeoutMs < VADProfile.DICTATION.silenceTimeoutMs)
    }

    @Test
    fun dictationProfileHasLongestMinSpeechDuration() {
        assertTrue(VADProfile.COMMAND.minSpeechDurationMs < VADProfile.CONVERSATION.minSpeechDurationMs)
        assertTrue(VADProfile.CONVERSATION.minSpeechDurationMs < VADProfile.DICTATION.minSpeechDurationMs)
    }

    @Test
    fun hangoverFramesIncreaseWithProfileTolerance() {
        assertTrue(VADProfile.COMMAND.hangoverFrames < VADProfile.CONVERSATION.hangoverFrames)
        assertTrue(VADProfile.CONVERSATION.hangoverFrames < VADProfile.DICTATION.hangoverFrames)
    }

    @Test
    fun thresholdAlphaDecreasesForMoreStableProfiles() {
        // Higher alpha = faster adaptation (command), lower alpha = slower adaptation (dictation)
        assertTrue(VADProfile.DICTATION.thresholdAlpha < VADProfile.CONVERSATION.thresholdAlpha)
        assertTrue(VADProfile.CONVERSATION.thresholdAlpha < VADProfile.COMMAND.thresholdAlpha)
    }

    @Test
    fun allProfilesHaveValidSensitivityRange() {
        for (profile in VADProfile.entries) {
            assertTrue(profile.vadSensitivity in 0f..1f, "${profile.name} sensitivity out of range")
        }
    }

    @Test
    fun allProfilesHavePositiveThresholds() {
        for (profile in VADProfile.entries) {
            assertTrue(profile.thresholdAlpha > 0f, "${profile.name} thresholdAlpha must be positive")
            assertTrue(profile.minThreshold > 0f, "${profile.name} minThreshold must be positive")
            assertTrue(profile.silenceTimeoutMs > 0, "${profile.name} silenceTimeoutMs must be positive")
            assertTrue(profile.minSpeechDurationMs > 0, "${profile.name} minSpeechDurationMs must be positive")
            assertTrue(profile.hangoverFrames > 0, "${profile.name} hangoverFrames must be positive")
        }
    }

    @Test
    fun conversationProfileMatchesOriginalDefaults() {
        // CONVERSATION must match the original WhisperVAD hardcoded values exactly
        val conv = VADProfile.CONVERSATION
        assertEquals(700L, conv.silenceTimeoutMs)
        assertEquals(300L, conv.minSpeechDurationMs)
        assertEquals(5, conv.hangoverFrames)
        assertEquals(0.02f, conv.thresholdAlpha)
        assertEquals(0.001f, conv.minThreshold)
    }

    @Test
    fun forSpeechModeStaticCommand() {
        assertEquals(VADProfile.COMMAND, VADProfile.forSpeechMode(SpeechMode.STATIC_COMMAND))
    }

    @Test
    fun forSpeechModeDynamicCommand() {
        assertEquals(VADProfile.COMMAND, VADProfile.forSpeechMode(SpeechMode.DYNAMIC_COMMAND))
    }

    @Test
    fun forSpeechModeDictation() {
        assertEquals(VADProfile.DICTATION, VADProfile.forSpeechMode(SpeechMode.DICTATION))
    }

    @Test
    fun forSpeechModeFreeSpeech() {
        assertEquals(VADProfile.CONVERSATION, VADProfile.forSpeechMode(SpeechMode.FREE_SPEECH))
    }

    @Test
    fun forSpeechModeHybrid() {
        assertEquals(VADProfile.CONVERSATION, VADProfile.forSpeechMode(SpeechMode.HYBRID))
    }

    @Test
    fun allSpeechModesMapped() {
        // Ensure no SpeechMode is left unmapped (would cause a when-expression crash)
        for (mode in SpeechMode.entries) {
            val profile = VADProfile.forSpeechMode(mode)
            assertTrue(profile in VADProfile.entries, "SpeechMode.$mode returned invalid profile")
        }
    }
}
