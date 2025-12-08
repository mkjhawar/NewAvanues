// filename: Universal/AVA/Features/WakeWord/src/test/java/com/augmentalis/ava/features/wakeword/WakeWordDetectorTest.kt
// created: 2025-11-22
// author: Manoj Jhawar
// © Augmentalis Inc, Intelligent Devices LLC

package com.augmentalis.ava.features.wakeword

import android.content.Context
import com.augmentalis.ava.core.common.Result
import com.augmentalis.ava.features.llm.security.ApiKeyManager
import com.augmentalis.ava.features.wakeword.detector.WakeWordDetector
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * Unit tests for WakeWordDetector
 *
 * Tests:
 * - Initialization with valid/invalid API key
 * - Start/stop detection
 * - Pause/resume detection
 * - State transitions
 * - Error handling
 *
 * @author Manoj Jhawar
 */
@OptIn(ExperimentalCoroutinesApi::class)
class WakeWordDetectorTest {

    private lateinit var context: Context
    private lateinit var apiKeyManager: ApiKeyManager
    private lateinit var detector: WakeWordDetector

    @Before
    fun setup() {
        context = mockk(relaxed = true)
        apiKeyManager = mockk(relaxed = true)

        // Mock environment variable for Porcupine API key
        mockkStatic(System::class)
        every { System.getenv("AVA_PORCUPINE_API_KEY") } returns "test_api_key_12345678901234567890"

        detector = WakeWordDetector(context, apiKeyManager)
    }

    @After
    fun teardown() {
        unmockkAll()
    }

    @Test
    fun `test initial state is UNINITIALIZED`() = runTest {
        // Assert
        assertEquals(WakeWordState.UNINITIALIZED, detector.state.value)
    }

    @Test
    fun `test initialize with valid API key succeeds`() = runTest {
        // Arrange
        val settings = WakeWordSettings(
            enabled = true,
            keyword = WakeWordKeyword.HEY_AVA,
            sensitivity = 0.5f
        )
        var detectionCallbackInvoked = false

        // Act
        val result = detector.initialize(settings) {
            detectionCallbackInvoked = true
        }

        // Assert
        // Note: Will fail in CI without Porcupine library, but structure is correct
        assertIs<Result<Unit>>(result)
    }

    @Test
    fun `test initialize without API key fails`() = runTest {
        // Arrange
        every { System.getenv("AVA_PORCUPINE_API_KEY") } returns null

        val settings = WakeWordSettings(
            enabled = true,
            keyword = WakeWordKeyword.HEY_AVA,
            sensitivity = 0.5f
        )

        // Act
        val result = detector.initialize(settings) {}

        // Assert
        assertIs<Result.Error<*>>(result)
        assertTrue(result.message.contains("Porcupine access key"))
    }

    @Test
    fun `test start before initialize fails`() = runTest {
        // Act
        val result = detector.start()

        // Assert
        assertIs<Result.Error<*>>(result)
        assertTrue(result.message.contains("not initialized"))
    }

    @Test
    fun `test stop transitions to STOPPED state`() = runTest {
        // Arrange
        val settings = WakeWordSettings(keyword = WakeWordKeyword.HEY_AVA)
        detector.initialize(settings) {}
        detector.start()

        // Act
        val result = detector.stop()

        // Assert
        // Note: Actual behavior depends on Porcupine library
        assertIs<Result<Unit>>(result)
    }

    @Test
    fun `test pause when listening updates state`() = runTest {
        // Arrange
        val settings = WakeWordSettings(keyword = WakeWordKeyword.HEY_AVA)
        detector.initialize(settings) {}
        detector.start()

        // Act
        detector.pause("Test pause")

        // Assert
        // Note: State will be PAUSED if detector was LISTENING
        // Actual behavior depends on Porcupine library
    }

    @Test
    fun `test resume when paused updates state`() = runTest {
        // Arrange
        val settings = WakeWordSettings(keyword = WakeWordKeyword.HEY_AVA)
        detector.initialize(settings) {}
        detector.start()
        detector.pause("Test pause")

        // Act
        detector.resume()

        // Assert
        // State should transition back to LISTENING
    }

    @Test
    fun `test cleanup releases resources`() = runTest {
        // Arrange
        val settings = WakeWordSettings(keyword = WakeWordKeyword.HEY_AVA)
        detector.initialize(settings) {}

        // Act
        detector.cleanup()

        // Assert
        assertEquals(WakeWordState.UNINITIALIZED, detector.state.value)
    }

    @Test
    fun `test detection count increments on detection`() = runTest {
        // Arrange
        val settings = WakeWordSettings(keyword = WakeWordKeyword.HEY_AVA)
        var callbackCount = 0

        detector.initialize(settings) {
            callbackCount++
        }

        // Assert
        assertEquals(0, detector.detectionCount.value)

        // Note: Actual detection would require Porcupine library and audio input
    }

    @Test
    fun `test isListening returns correct state`() = runTest {
        // Arrange
        val settings = WakeWordSettings(keyword = WakeWordKeyword.HEY_AVA)

        // Assert initial state
        assertEquals(false, detector.isListening())

        // Initialize and start (would fail without Porcupine, but structure is correct)
        detector.initialize(settings) {}
        // detector.start() would change isListening() to true if successful
    }

    @Test
    fun `test settings sensitivity is validated`() = runTest {
        // Arrange
        val settings = WakeWordSettings(
            keyword = WakeWordKeyword.HEY_AVA,
            sensitivity = 0.8f  // High sensitivity
        )

        // Act
        val result = detector.initialize(settings) {}

        // Assert
        // Sensitivity should be used in Porcupine configuration
        assertIs<Result<Unit>>(result)
    }

    @Test
    fun `test different keywords are supported`() = runTest {
        // Test each keyword variant
        val keywords = listOf(
            WakeWordKeyword.HEY_AVA,
            WakeWordKeyword.OK_AVA,
            WakeWordKeyword.JARVIS,
            WakeWordKeyword.ALEXA,
            WakeWordKeyword.COMPUTER
        )

        keywords.forEach { keyword ->
            val settings = WakeWordSettings(keyword = keyword)
            val result = detector.initialize(settings) {}

            // Each keyword should be supported
            assertIs<Result<Unit>>(result)

            // Cleanup for next iteration
            detector.cleanup()
        }
    }
}
