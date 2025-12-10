// filename: Universal/AVA/Features/Chat/src/test/kotlin/com/augmentalis/ava/features/chat/voice/VoiceInputIntegrationTest.kt
// created: 2025-11-22
// author: Manoj Jhawar
// © Augmentalis Inc, Intelligent Devices LLC
// Phase 1.2 - Voice Integration: Integration Tests for Voice Input with ChatViewModel

package com.augmentalis.ava.features.chat.voice

import android.content.Context
import app.cash.turbine.test
import com.augmentalis.ava.features.chat.voice.VoiceInputViewModel.VoiceInputState
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Integration tests for voice input with ChatViewModel.
 *
 * Tests cover:
 * - Voice transcription flow (start -> partial -> final)
 * - State transitions (Idle -> Ready -> Speaking -> Processing -> Idle)
 * - Error handling and recovery
 * - Integration with message sending
 *
 * @author Manoj Jhawar
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class VoiceInputIntegrationTest {

    private lateinit var context: Context
    private lateinit var voiceInputManager: VoiceInputManager
    private lateinit var voiceInputViewModel: VoiceInputViewModel
    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setup() {
        context = RuntimeEnvironment.getApplication()
        voiceInputManager = mockk(relaxed = true)
        voiceInputViewModel = VoiceInputViewModel(voiceInputManager)

        // Mock availability check
        every { voiceInputManager.isAvailable() } returns true
        every { voiceInputManager.isActive() } returns false
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `voice input state flow transitions correctly`() = runTest(testDispatcher) {
        // Given
        voiceInputViewModel.state.test {
            // Initial state
            assertEquals(VoiceInputState.Idle, awaitItem())

            // When starting listening
            every { voiceInputManager.isActive() } returns true
            voiceInputViewModel.startListening()

            // Then state should transition to Ready
            val readyState = awaitItem()
            assertTrue(readyState is VoiceInputState.Ready)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `partial text updates during speech recognition`() = runTest(testDispatcher) {
        // Given
        var capturedCallback: VoiceInputManager.VoiceInputCallback? = null
        every {
            voiceInputManager.startListening(capture(slot<VoiceInputManager.VoiceInputCallback>()), any())
        } answers {
            capturedCallback = firstArg()
            every { voiceInputManager.isActive() } returns true
        }

        voiceInputViewModel.startListening()

        // When receiving partial results
        voiceInputViewModel.partialText.test {
            assertEquals("", awaitItem()) // Initial empty state

            capturedCallback?.onReadyForSpeech()
            capturedCallback?.onBeginningOfSpeech()
            capturedCallback?.onPartialResult("Hello")

            // Then partial text should update
            assertEquals("Hello", awaitItem())

            capturedCallback?.onPartialResult("Hello world")
            assertEquals("Hello world", awaitItem())

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `final text is set when recognition completes`() = runTest(testDispatcher) {
        // Given
        var capturedCallback: VoiceInputManager.VoiceInputCallback? = null
        every {
            voiceInputManager.startListening(capture(slot<VoiceInputManager.VoiceInputCallback>()), any())
        } answers {
            capturedCallback = firstArg()
            every { voiceInputManager.isActive() } returns true
        }

        voiceInputViewModel.startListening()

        // When receiving final results
        voiceInputViewModel.finalText.test {
            assertNull(awaitItem()) // Initial null state

            val results = listOf("Hello AVA", "Hello Ava", "Hello Eva")
            val confidence = floatArrayOf(0.95f, 0.85f, 0.75f)
            capturedCallback?.onFinalResult(results, confidence)

            // Then final text should be set to highest confidence result
            assertEquals("Hello AVA", awaitItem())

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `error state is set when recognition fails`() = runTest(testDispatcher) {
        // Given
        var capturedCallback: VoiceInputManager.VoiceInputCallback? = null
        every {
            voiceInputManager.startListening(capture(slot<VoiceInputManager.VoiceInputCallback>()), any())
        } answers {
            capturedCallback = firstArg()
            every { voiceInputManager.isActive() } returns true
        }

        voiceInputViewModel.startListening()

        // When an error occurs
        voiceInputViewModel.state.test {
            skipItems(1) // Skip initial state

            capturedCallback?.onError(VoiceInputManager.VoiceInputError.NoMatch)

            // Then state should transition to Error
            val errorState = awaitItem()
            assertTrue(errorState is VoiceInputState.Error)
            assertEquals("No speech detected. Try again.", (errorState as VoiceInputState.Error).message)
            assertTrue(errorState.isRecoverable)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `audio level updates during speech`() = runTest(testDispatcher) {
        // Given
        var capturedCallback: VoiceInputManager.VoiceInputCallback? = null
        every {
            voiceInputManager.startListening(capture(slot<VoiceInputManager.VoiceInputCallback>()), any())
        } answers {
            capturedCallback = firstArg()
            every { voiceInputManager.isActive() } returns true
        }

        voiceInputViewModel.startListening()

        // When RMS changes occur
        voiceInputViewModel.audioLevel.test {
            assertEquals(0f, awaitItem()) // Initial zero level

            capturedCallback?.onRmsChanged(5.0f)

            // Then audio level should update (normalized 0-1)
            val level = awaitItem()
            assertTrue(level in 0f..1f)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `consumeFinalText clears the text after consumption`() = runTest(testDispatcher) {
        // Given
        var capturedCallback: VoiceInputManager.VoiceInputCallback? = null
        every {
            voiceInputManager.startListening(capture(slot<VoiceInputManager.VoiceInputCallback>()), any())
        } answers {
            capturedCallback = firstArg()
            every { voiceInputManager.isActive() } returns true
        }

        voiceInputViewModel.startListening()

        val results = listOf("Test message")
        capturedCallback?.onFinalResult(results, null)

        // When consuming final text
        val text = voiceInputViewModel.consumeFinalText()

        // Then
        assertEquals("Test message", text)

        // And subsequent calls return null
        assertNull(voiceInputViewModel.consumeFinalText())
    }

    @Test
    fun `cancelListening resets state to Idle`() = runTest(testDispatcher) {
        // Given
        every { voiceInputManager.isActive() } returns true
        voiceInputViewModel.startListening()

        // When cancelling
        voiceInputViewModel.cancelListening()

        // Then
        voiceInputViewModel.state.test {
            assertEquals(VoiceInputState.Idle, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }

        verify { voiceInputManager.cancel() }
    }

    @Test
    fun `clearError transitions from Error to Idle`() = runTest(testDispatcher) {
        // Given
        var capturedCallback: VoiceInputManager.VoiceInputCallback? = null
        every {
            voiceInputManager.startListening(capture(slot<VoiceInputManager.VoiceInputCallback>()), any())
        } answers {
            capturedCallback = firstArg()
            every { voiceInputManager.isActive() } returns true
        }

        voiceInputViewModel.startListening()
        capturedCallback?.onError(VoiceInputManager.VoiceInputError.NetworkError)

        // When clearing error
        voiceInputViewModel.state.test {
            skipItems(1) // Skip current error state

            voiceInputViewModel.clearError()

            // Then state should transition to Idle
            assertEquals(VoiceInputState.Idle, awaitItem())

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `startListening when unavailable triggers error`() = runTest(testDispatcher) {
        // Given
        every { voiceInputManager.isAvailable() } returns false
        val viewModel = VoiceInputViewModel(voiceInputManager)

        // When starting listening
        viewModel.startListening()

        // Then state should be Error
        viewModel.state.test {
            val state = awaitItem()
            assertTrue(state is VoiceInputState.Error)
            assertEquals("Voice recognition not available", (state as VoiceInputState.Error).message)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `isListening returns correct state`() {
        // Given - Initial state
        every { voiceInputManager.isActive() } returns false
        assertEquals(false, voiceInputViewModel.isListening())

        // When - Listening active
        every { voiceInputManager.isActive() } returns true
        assertEquals(true, voiceInputViewModel.isListening())

        // When - Stopped
        every { voiceInputManager.isActive() } returns false
        assertEquals(false, voiceInputViewModel.isListening())
    }
}
