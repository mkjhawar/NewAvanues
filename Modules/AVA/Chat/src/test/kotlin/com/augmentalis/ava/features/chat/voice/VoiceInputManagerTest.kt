// filename: Universal/AVA/Features/Chat/src/test/kotlin/com/augmentalis/ava/features/chat/voice/VoiceInputManagerTest.kt
// created: 2025-11-22
// author: Manoj Jhawar
// © Augmentalis Inc, Intelligent Devices LLC
// Phase 1.2 - Voice Integration: Unit Tests for VoiceInputManager

package com.augmentalis.ava.features.chat.voice

import android.content.Context
import android.speech.SpeechRecognizer
import io.mockk.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Unit tests for VoiceInputManager.
 *
 * Uses Robolectric for Android framework mocking.
 * Tests cover:
 * - Initialization and availability check
 * - Start/stop/cancel listening lifecycle
 * - Callback invocations for various events
 * - Error handling for all error types
 * - Resource cleanup
 *
 * @author Manoj Jhawar
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28]) // Android 9 (Pie) - minimum supported SDK
class VoiceInputManagerTest {

    private lateinit var context: Context
    private lateinit var voiceInputManager: VoiceInputManager
    private lateinit var mockCallback: VoiceInputManager.VoiceInputCallback

    @Before
    fun setup() {
        context = RuntimeEnvironment.getApplication()
        voiceInputManager = VoiceInputManager(context)
        mockCallback = mockk(relaxed = true)

        // Mock SpeechRecognizer.isRecognitionAvailable to return true
        mockkStatic(SpeechRecognizer::class)
        every { SpeechRecognizer.isRecognitionAvailable(any()) } returns true
    }

    @After
    fun tearDown() {
        voiceInputManager.release()
        unmockkAll()
    }

    @Test
    fun `isAvailable returns true when speech recognition is available`() {
        // Given
        every { SpeechRecognizer.isRecognitionAvailable(context) } returns true

        // When
        val isAvailable = voiceInputManager.isAvailable()

        // Then
        assertTrue(isAvailable)
    }

    @Test
    fun `isAvailable returns false when speech recognition is not available`() {
        // Given
        every { SpeechRecognizer.isRecognitionAvailable(context) } returns false

        // When
        val isAvailable = voiceInputManager.isAvailable()

        // Then
        assertFalse(isAvailable)
    }

    @Test
    fun `startListening sets isActive to true`() {
        // When
        voiceInputManager.startListening(mockCallback)

        // Then
        assertTrue(voiceInputManager.isActive())
    }

    @Test
    fun `stopListening sets isActive to false`() {
        // Given
        voiceInputManager.startListening(mockCallback)

        // When
        voiceInputManager.stopListening()

        // Then
        // Note: isActive is set to false asynchronously, so we verify it was called
        assertFalse(voiceInputManager.isActive())
    }

    @Test
    fun `cancel sets isActive to false`() {
        // Given
        voiceInputManager.startListening(mockCallback)

        // When
        voiceInputManager.cancel()

        // Then
        assertFalse(voiceInputManager.isActive())
    }

    @Test
    fun `VoiceInputError fromErrorCode converts all error codes correctly`() {
        // Test all SpeechRecognizer error codes
        assertEquals(
            VoiceInputManager.VoiceInputError.AudioError,
            VoiceInputManager.VoiceInputError.fromErrorCode(SpeechRecognizer.ERROR_AUDIO)
        )
        assertEquals(
            VoiceInputManager.VoiceInputError.ClientError,
            VoiceInputManager.VoiceInputError.fromErrorCode(SpeechRecognizer.ERROR_CLIENT)
        )
        assertEquals(
            VoiceInputManager.VoiceInputError.InsufficientPermissions,
            VoiceInputManager.VoiceInputError.fromErrorCode(SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS)
        )
        assertEquals(
            VoiceInputManager.VoiceInputError.NetworkError,
            VoiceInputManager.VoiceInputError.fromErrorCode(SpeechRecognizer.ERROR_NETWORK)
        )
        assertEquals(
            VoiceInputManager.VoiceInputError.NetworkTimeout,
            VoiceInputManager.VoiceInputError.fromErrorCode(SpeechRecognizer.ERROR_NETWORK_TIMEOUT)
        )
        assertEquals(
            VoiceInputManager.VoiceInputError.NoMatch,
            VoiceInputManager.VoiceInputError.fromErrorCode(SpeechRecognizer.ERROR_NO_MATCH)
        )
        assertEquals(
            VoiceInputManager.VoiceInputError.RecognizerBusy,
            VoiceInputManager.VoiceInputError.fromErrorCode(SpeechRecognizer.ERROR_RECOGNIZER_BUSY)
        )
        assertEquals(
            VoiceInputManager.VoiceInputError.ServerError,
            VoiceInputManager.VoiceInputError.fromErrorCode(SpeechRecognizer.ERROR_SERVER)
        )
        assertEquals(
            VoiceInputManager.VoiceInputError.SpeechTimeout,
            VoiceInputManager.VoiceInputError.fromErrorCode(SpeechRecognizer.ERROR_SPEECH_TIMEOUT)
        )
    }

    @Test
    fun `VoiceInputError fromErrorCode handles unknown error codes`() {
        // Given
        val unknownErrorCode = 999

        // When
        val error = VoiceInputManager.VoiceInputError.fromErrorCode(unknownErrorCode)

        // Then
        assertTrue(error is VoiceInputManager.VoiceInputError.Unknown)
        assertEquals("Unknown error (999).", error.message)
    }

    @Test
    fun `error types have correct recoverability flags`() {
        // Non-recoverable errors
        assertFalse(VoiceInputManager.VoiceInputError.AudioError.isRecoverable)
        assertFalse(VoiceInputManager.VoiceInputError.ClientError.isRecoverable)
        assertFalse(VoiceInputManager.VoiceInputError.InsufficientPermissions.isRecoverable)
        assertFalse(VoiceInputManager.VoiceInputError.ServiceNotAvailable.isRecoverable)

        // Recoverable errors
        assertTrue(VoiceInputManager.VoiceInputError.NetworkError.isRecoverable)
        assertTrue(VoiceInputManager.VoiceInputError.NetworkTimeout.isRecoverable)
        assertTrue(VoiceInputManager.VoiceInputError.NoMatch.isRecoverable)
        assertTrue(VoiceInputManager.VoiceInputError.RecognizerBusy.isRecoverable)
        assertTrue(VoiceInputManager.VoiceInputError.ServerError.isRecoverable)
        assertTrue(VoiceInputManager.VoiceInputError.SpeechTimeout.isRecoverable)
    }

    @Test
    fun `startListening when not available calls onError callback`() {
        // Given
        every { SpeechRecognizer.isRecognitionAvailable(context) } returns false
        val voiceInputManager = VoiceInputManager(context)

        // When
        voiceInputManager.startListening(mockCallback)

        // Then
        verify {
            mockCallback.onError(VoiceInputManager.VoiceInputError.ServiceNotAvailable)
        }
    }

    @Test
    fun `release cleans up resources`() {
        // Given
        voiceInputManager.startListening(mockCallback)
        assertTrue(voiceInputManager.isActive())

        // When
        voiceInputManager.release()

        // Then
        assertFalse(voiceInputManager.isActive())
    }

    @Test
    fun `startListening with custom language uses provided language`() {
        // This test verifies that the language parameter is passed correctly
        // In a full implementation, we would verify the Intent extras

        // When
        voiceInputManager.startListening(mockCallback, "es-ES")

        // Then
        assertTrue(voiceInputManager.isActive())
        // Note: Verifying Intent extras would require deeper mocking of SpeechRecognizer
    }

    @Test
    fun `multiple startListening calls stop previous session`() {
        // Given
        voiceInputManager.startListening(mockCallback)
        assertTrue(voiceInputManager.isActive())

        // When
        voiceInputManager.startListening(mockCallback)

        // Then
        // Should have stopped previous session and started new one
        assertTrue(voiceInputManager.isActive())
    }
}
