package com.augmentalis.chat.tts

import com.augmentalis.ava.core.common.Result
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for TTSViewModel.
 *
 * Tests settings management and TTS operations.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class TTSViewModelTest {

    private lateinit var ttsManager: TTSManager
    private lateinit var ttsPreferences: TTSPreferences
    private lateinit var viewModel: TTSViewModel

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        ttsManager = mockk(relaxed = true)
        ttsPreferences = mockk(relaxed = true)

        // Mock flows
        every { ttsManager.isInitialized } returns MutableStateFlow(false)
        every { ttsManager.isSpeaking } returns MutableStateFlow(false)
        every { ttsManager.availableVoices } returns MutableStateFlow(emptyList())
        every { ttsManager.initError } returns MutableStateFlow(null)
        every { ttsPreferences.settings } returns MutableStateFlow(TTSSettings.DEFAULT)
        every { ttsPreferences.getSettings() } returns TTSSettings.DEFAULT

        viewModel = TTSViewModel(ttsManager, ttsPreferences)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `toggleEnabled calls ttsPreferences toggleEnabled`() = runTest {
        every { ttsPreferences.toggleEnabled() } returns true

        viewModel.toggleEnabled()
        advanceUntilIdle()

        verify { ttsPreferences.toggleEnabled() }
    }

    @Test
    fun `toggleAutoSpeak calls ttsPreferences toggleAutoSpeak`() = runTest {
        every { ttsPreferences.toggleAutoSpeak() } returns true

        viewModel.toggleAutoSpeak()
        advanceUntilIdle()

        verify { ttsPreferences.toggleAutoSpeak() }
    }

    @Test
    fun `setSpeechRate updates preferences`() = runTest {
        viewModel.setSpeechRate(1.5f)
        advanceUntilIdle()

        verify { ttsPreferences.setSpeechRate(1.5f) }
    }

    @Test
    fun `setPitch updates preferences`() = runTest {
        viewModel.setPitch(0.9f)
        advanceUntilIdle()

        verify { ttsPreferences.setPitch(0.9f) }
    }

    @Test
    fun `setSelectedVoice updates preferences`() = runTest {
        viewModel.setSelectedVoice("en-us-female")
        advanceUntilIdle()

        verify { ttsPreferences.setSelectedVoice("en-us-female") }
    }

    @Test
    fun `testSpeak calls ttsManager speak with sample text`() = runTest {
        every { ttsManager.speak(any(), any(), any()) } returns Result.Success(Unit)

        viewModel.testSpeak()
        advanceUntilIdle()

        verify { ttsManager.speak(match { it.contains("AVA", ignoreCase = true) }, any(), any()) }
    }

    @Test
    fun `testSpeak handles error from ttsManager`() = runTest {
        every { ttsManager.speak(any(), any(), any()) } returns Result.Error(Exception("TTS failed"))

        viewModel.testSpeak()
        advanceUntilIdle()

        // Error message should be set
        assertNotNull("Error message should be set", viewModel.errorMessage.value)
    }

    @Test
    fun `stopSpeaking calls ttsManager stop`() {
        viewModel.stopSpeaking()

        verify { ttsManager.stop() }
    }

    @Test
    fun `resetToDefaults calls ttsPreferences resetToDefaults`() = runTest {
        viewModel.resetToDefaults()
        advanceUntilIdle()

        verify { ttsPreferences.resetToDefaults() }
    }

    @Test
    fun `clearError clears error message`() = runTest {
        // Set error first via testSpeak failure
        every { ttsManager.speak(any(), any(), any()) } returns Result.Error(Exception("Test error"))
        viewModel.testSpeak()
        advanceUntilIdle()

        // Clear error
        viewModel.clearError()

        assertNull("Error should be cleared", viewModel.errorMessage.value)
    }
}
