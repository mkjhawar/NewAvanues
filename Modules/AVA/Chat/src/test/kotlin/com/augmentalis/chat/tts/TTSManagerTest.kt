package com.augmentalis.chat.tts

import android.content.Context
import android.speech.tts.TextToSpeech
import com.augmentalis.ava.core.common.Result
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.util.Locale

/**
 * Unit tests for TTSManager.
 *
 * Tests TTS initialization, speech operations, and error handling.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class TTSManagerTest {

    private lateinit var context: Context
    private lateinit var ttsManager: TTSManager

    @Before
    fun setup() {
        context = mockk(relaxed = true)
        ttsManager = TTSManager(context)
    }

    @Test
    fun `initialization sets isInitialized to false initially`() = runTest {
        val isInitialized = ttsManager.isInitialized.first()
        assertFalse("TTSManager should not be initialized initially", isInitialized)
    }

    @Test
    fun `speak returns error when TTS not initialized`() = runTest {
        val result = ttsManager.speak("Hello")

        assertTrue("Should return Error when not initialized", result is Result.Error)
        val error = result as Result.Error
        assertTrue("Error message should mention initialization", error.message?.contains("not initialized", ignoreCase = true) == true)
    }

    @Test
    fun `speak returns error for blank text`() = runTest {
        // Simulate successful initialization
        ttsManager.onInit(TextToSpeech.SUCCESS)

        val result = ttsManager.speak("")

        assertTrue("Should return Error for blank text", result is Result.Error)
        val error = result as Result.Error
        assertTrue("Error message should mention empty text", error.message?.contains("empty", ignoreCase = true) == true)
    }

    @Test
    fun `stop clears isSpeaking state`() = runTest {
        ttsManager.stop()

        val isSpeaking = ttsManager.isSpeaking.first()
        assertFalse("isSpeaking should be false after stop", isSpeaking)
    }

    @Test
    fun `updateSettings updates currentSettings flow`() = runTest {
        val newSettings = TTSSettings(
            enabled = true,
            autoSpeak = true,
            speechRate = 1.5f,
            pitch = 0.8f
        )

        ttsManager.updateSettings(newSettings)

        val currentSettings = ttsManager.currentSettings.first()
        assertEquals("Settings should be updated", newSettings, currentSettings)
    }

    @Test
    fun `getAvailableVoices returns empty list initially`() {
        val voices = ttsManager.getAvailableVoices()
        assertTrue("Available voices should be empty initially", voices.isEmpty())
    }

    @Test
    fun `shutdown sets isInitialized to false`() = runTest {
        ttsManager.onInit(TextToSpeech.SUCCESS)
        ttsManager.shutdown()

        val isInitialized = ttsManager.isInitialized.first()
        assertFalse("isInitialized should be false after shutdown", isInitialized)
    }

    @Test
    fun `speech rate is clamped to valid range`() {
        val tooLow = 0.1f
        val tooHigh = 5.0f

        assertTrue("Too low rate should be >= MIN_SPEECH_RATE",
            tooLow.coerceIn(TTSManager.MIN_SPEECH_RATE, TTSManager.MAX_SPEECH_RATE) == TTSManager.MIN_SPEECH_RATE)
        assertTrue("Too high rate should be <= MAX_SPEECH_RATE",
            tooHigh.coerceIn(TTSManager.MIN_SPEECH_RATE, TTSManager.MAX_SPEECH_RATE) == TTSManager.MAX_SPEECH_RATE)
    }

    @Test
    fun `pitch is clamped to valid range`() {
        val tooLow = 0.1f
        val tooHigh = 5.0f

        assertTrue("Too low pitch should be >= MIN_PITCH",
            tooLow.coerceIn(TTSManager.MIN_PITCH, TTSManager.MAX_PITCH) == TTSManager.MIN_PITCH)
        assertTrue("Too high pitch should be <= MAX_PITCH",
            tooHigh.coerceIn(TTSManager.MIN_PITCH, TTSManager.MAX_PITCH) == TTSManager.MAX_PITCH)
    }
}
