package com.augmentalis.ava.features.chat.tts

import android.content.Context
import android.content.SharedPreferences
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for TTSPreferences.
 *
 * Tests settings persistence and reactive state management.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class TTSPreferencesTest {

    private lateinit var context: Context
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var editor: SharedPreferences.Editor
    private lateinit var ttsPreferences: TTSPreferences

    @Before
    fun setup() {
        context = mockk(relaxed = true)
        sharedPreferences = mockk(relaxed = true)
        editor = mockk(relaxed = true)

        every { context.getSharedPreferences(any(), any()) } returns sharedPreferences
        every { sharedPreferences.edit() } returns editor
        every { editor.putBoolean(any(), any()) } returns editor
        every { editor.putString(any(), any()) } returns editor
        every { editor.putFloat(any(), any()) } returns editor
        every { editor.apply() } returns Unit
        every { editor.clear() } returns editor

        // Default values
        every { sharedPreferences.getBoolean(any(), any()) } answers { secondArg() }
        every { sharedPreferences.getString(any(), any()) } answers { secondArg() }
        every { sharedPreferences.getFloat(any(), any()) } answers { secondArg() }

        ttsPreferences = TTSPreferences.getInstance(context)
    }

    @Test
    fun `getSettings returns default settings initially`() {
        val settings = ttsPreferences.getSettings()

        assertEquals("Default enabled should be true", true, settings.enabled)
        assertEquals("Default autoSpeak should be false", false, settings.autoSpeak)
        assertEquals("Default speechRate should be 1.0", 1.0f, settings.speechRate, 0.01f)
        assertEquals("Default pitch should be 1.0", 1.0f, settings.pitch, 0.01f)
        assertNull("Default selectedVoice should be null", settings.selectedVoice)
    }

    @Test
    fun `updateSettings saves to SharedPreferences`() {
        val newSettings = TTSSettings(
            enabled = false,
            autoSpeak = true,
            selectedVoice = "en-us-female",
            speechRate = 1.5f,
            pitch = 0.8f,
            speakPunctuation = true
        )

        ttsPreferences.updateSettings(newSettings)

        verify { editor.putBoolean("tts_enabled", false) }
        verify { editor.putBoolean("auto_speak", true) }
        verify { editor.putString("selected_voice", "en-us-female") }
        verify { editor.putFloat("speech_rate", 1.5f) }
        verify { editor.putFloat("pitch", 0.8f) }
        verify { editor.putBoolean("speak_punctuation", true) }
        verify { editor.apply() }
    }

    @Test
    fun `toggleEnabled toggles enabled state`() {
        // Initial state is enabled=true
        val firstToggle = ttsPreferences.toggleEnabled()
        assertFalse("First toggle should disable", firstToggle)

        val secondToggle = ttsPreferences.toggleEnabled()
        assertTrue("Second toggle should enable", secondToggle)
    }

    @Test
    fun `toggleAutoSpeak toggles autoSpeak state`() {
        // Initial state is autoSpeak=false
        val firstToggle = ttsPreferences.toggleAutoSpeak()
        assertTrue("First toggle should enable", firstToggle)

        val secondToggle = ttsPreferences.toggleAutoSpeak()
        assertFalse("Second toggle should disable", secondToggle)
    }

    @Test
    fun `setSpeechRate clamps value to valid range`() {
        ttsPreferences.setSpeechRate(0.3f) // Below minimum
        verify { editor.putFloat("speech_rate", TTSManager.MIN_SPEECH_RATE) }

        ttsPreferences.setSpeechRate(3.0f) // Above maximum
        verify { editor.putFloat("speech_rate", TTSManager.MAX_SPEECH_RATE) }

        ttsPreferences.setSpeechRate(1.5f) // Within range
        verify { editor.putFloat("speech_rate", 1.5f) }
    }

    @Test
    fun `setPitch clamps value to valid range`() {
        ttsPreferences.setPitch(0.3f) // Below minimum
        verify { editor.putFloat("pitch", TTSManager.MIN_PITCH) }

        ttsPreferences.setPitch(3.0f) // Above maximum
        verify { editor.putFloat("pitch", TTSManager.MAX_PITCH) }

        ttsPreferences.setPitch(1.2f) // Within range
        verify { editor.putFloat("pitch", 1.2f) }
    }

    @Test
    fun `setSelectedVoice updates voice preference`() {
        ttsPreferences.setSelectedVoice("en-us-male")
        verify { editor.putString("selected_voice", "en-us-male") }

        ttsPreferences.setSelectedVoice(null)
        verify { editor.putString("selected_voice", null) }
    }

    @Test
    fun `resetToDefaults restores default settings`() {
        // Change settings first
        ttsPreferences.updateSettings(TTSSettings(
            enabled = false,
            autoSpeak = true,
            speechRate = 1.5f
        ))

        // Reset to defaults
        ttsPreferences.resetToDefaults()

        val settings = ttsPreferences.getSettings()
        assertEquals("Should restore default settings", TTSSettings.DEFAULT, settings)
    }

    @Test
    fun `clearAll clears all preferences`() {
        ttsPreferences.clearAll()

        verify { editor.clear() }
        verify { editor.apply() }
    }
}
