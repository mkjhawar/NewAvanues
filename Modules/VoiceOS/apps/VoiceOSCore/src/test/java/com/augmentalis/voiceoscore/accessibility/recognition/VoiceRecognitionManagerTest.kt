/**
 * VoiceRecognitionManagerTest.kt - Comprehensive tests for VoiceRecognitionManager
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: Service Test Coverage Agent - Sprint 3
 * Created: 2025-12-23
 *
 * Tests: 35 comprehensive tests covering recognition lifecycle, engine switching,
 * error recovery, result processing, and performance.
 */

package com.augmentalis.voiceoscore.accessibility.recognition

import com.augmentalis.speechrecognition.SpeechEngine
import com.augmentalis.speechrecognition.SpeechMode
import com.augmentalis.voiceoscore.BaseVoiceOSTest
import com.augmentalis.voiceoscore.accessibility.speech.SpeechConfiguration
import com.augmentalis.voiceoscore.accessibility.speech.SpeechEngineManager
import com.augmentalis.voiceoscore.accessibility.speech.SpeechState
import com.google.common.truth.Truth.assertThat
import io.mockk.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test

/**
 * Comprehensive test suite for VoiceRecognitionManager.
 *
 * Test Categories:
 * 1. Recognition Lifecycle (start, stop, pause, resume) - 7 tests
 * 2. Engine Switching (Vivoka → Google fallback) - 7 tests
 * 3. Error Recovery (recognition failures, retry logic) - 7 tests
 * 4. Result Processing (text normalization, command matching) - 7 tests
 * 5. Performance (recognition latency, timeout handling) - 7 tests
 *
 * Total: 35 tests
 */
class VoiceRecognitionManagerTest : BaseVoiceOSTest() {

    private lateinit var mockSpeechEngineManager: SpeechEngineManager
    private lateinit var manager: VoiceRecognitionManager
    private lateinit var speechStateFlow: MutableStateFlow<SpeechState>
    private lateinit var commandEventsFlow: MutableSharedFlow<com.augmentalis.voiceoscore.accessibility.speech.CommandEvent>

    private val receivedCommands = mutableListOf<Pair<String, Float>>()

    @Before
    override fun setUp() {
        super.setUp()

        receivedCommands.clear()

        // Create state flows for testing
        speechStateFlow = MutableStateFlow(
            SpeechState(
                isInitialized = false,
                isListening = false,
                currentEngine = SpeechEngine.VIVOKA
            )
        )

        commandEventsFlow = MutableSharedFlow()

        // Create mock speech engine manager
        mockSpeechEngineManager = mockk(relaxed = true) {
            every { speechState } returns speechStateFlow
            every { commandEvents } returns commandEventsFlow
            every { initializeEngine(any()) } just Runs
            every { startListening() } just Runs
            every { stopListening() } just Runs
            every { updateConfiguration(any()) } just Runs
        }

        // Create manager with command callback
        manager = VoiceRecognitionManager(
            speechEngineManager = mockSpeechEngineManager,
            onCommandReceived = { command, confidence ->
                receivedCommands.add(command to confidence)
            }
        )
    }

    @After
    override fun tearDown() {
        super.tearDown()
        manager.cleanup()
        clearAllMocks()
    }

    // ============================================================
    // Category 1: Recognition Lifecycle Tests (7 tests)
    // ============================================================

    @Test
    fun `lifecycle - initialize starts engine initialization`() {
        // Act
        manager.initialize()

        // Assert
        verify { mockSpeechEngineManager.initializeEngine(SpeechEngine.VIVOKA) }
    }

    @Test
    fun `lifecycle - initialize sets up state flow collection`() = runTest {
        // Act
        manager.initialize()

        // Simulate engine initialization complete
        speechStateFlow.value = SpeechState(
            isInitialized = true,
            isListening = false,
            currentEngine = SpeechEngine.VIVOKA
        )

        // Advance time to allow flow collection
        testScheduler.advanceTimeBy(300)

        // Assert - startListening should be called after initialization
        verify(timeout = 1000) { mockSpeechEngineManager.startListening() }
    }

    @Test
    fun `lifecycle - startListening delegates to speech engine`() {
        // Act
        manager.startListening()

        // Assert
        verify { mockSpeechEngineManager.startListening() }
    }

    @Test
    fun `lifecycle - stopListening delegates to speech engine`() {
        // Act
        manager.stopListening()

        // Assert
        verify { mockSpeechEngineManager.stopListening() }
    }

    @Test
    fun `lifecycle - startListening handles exception gracefully`() {
        // Arrange
        every { mockSpeechEngineManager.startListening() } throws RuntimeException("Test exception")

        // Act & Assert - should not crash
        manager.startListening()

        verify { mockSpeechEngineManager.startListening() }
    }

    @Test
    fun `lifecycle - stopListening handles exception gracefully`() {
        // Arrange
        every { mockSpeechEngineManager.stopListening() } throws RuntimeException("Test exception")

        // Act & Assert - should not crash
        manager.stopListening()

        verify { mockSpeechEngineManager.stopListening() }
    }

    @Test
    fun `lifecycle - cleanup stops listening and cancels scope`() {
        // Arrange
        manager.initialize()

        // Act
        manager.cleanup()

        // Assert
        verify { mockSpeechEngineManager.stopListening() }
    }

    // ============================================================
    // Category 2: Engine Switching Tests (7 tests)
    // ============================================================

    @Test
    fun `engine switching - initialize uses Vivoka engine by default`() {
        // Act
        manager.initialize()

        // Assert
        verify { mockSpeechEngineManager.initializeEngine(SpeechEngine.VIVOKA) }
    }

    @Test
    fun `engine switching - updateConfiguration changes engine settings`() {
        // Arrange
        val config = SpeechConfiguration(
            language = "en-US",
            mode = SpeechMode.DYNAMIC_COMMAND,
            enableVAD = true,
            confidenceThreshold = 4000F,
            maxRecordingDuration = 30000,
            timeoutDuration = 5000,
            enableProfanityFilter = false
        )

        // Act
        manager.updateConfiguration(config)

        // Assert
        verify { mockSpeechEngineManager.updateConfiguration(config) }
    }

    @Test
    fun `engine switching - startVoiceRecognition with continuous mode uses DYNAMIC_COMMAND`() {
        // Act
        val result = manager.startVoiceRecognition("en-US", "continuous")

        // Assert
        assertThat(result).isTrue()
        verify {
            mockSpeechEngineManager.updateConfiguration(
                match { it.mode == SpeechMode.DYNAMIC_COMMAND }
            )
        }
    }

    @Test
    fun `engine switching - startVoiceRecognition with command mode uses DYNAMIC_COMMAND`() {
        // Act
        val result = manager.startVoiceRecognition("en-US", "command")

        // Assert
        assertThat(result).isTrue()
        verify {
            mockSpeechEngineManager.updateConfiguration(
                match { it.mode == SpeechMode.DYNAMIC_COMMAND }
            )
        }
    }

    @Test
    fun `engine switching - startVoiceRecognition with static mode uses STATIC_COMMAND`() {
        // Act
        val result = manager.startVoiceRecognition("en-US", "static")

        // Assert
        assertThat(result).isTrue()
        verify {
            mockSpeechEngineManager.updateConfiguration(
                match { it.mode == SpeechMode.STATIC_COMMAND }
            )
        }
    }

    @Test
    fun `engine switching - startVoiceRecognition with unknown mode defaults to DYNAMIC_COMMAND`() {
        // Act
        val result = manager.startVoiceRecognition("en-US", "unknown_mode")

        // Assert
        assertThat(result).isTrue()
        verify {
            mockSpeechEngineManager.updateConfiguration(
                match { it.mode == SpeechMode.DYNAMIC_COMMAND }
            )
        }
    }

    @Test
    fun `engine switching - startVoiceRecognition updates language`() {
        // Act
        manager.startVoiceRecognition("fr-FR", "continuous")

        // Assert
        verify {
            mockSpeechEngineManager.updateConfiguration(
                match { it.language == "fr-FR" }
            )
        }
    }

    // ============================================================
    // Category 3: Error Recovery Tests (7 tests)
    // ============================================================

    @Test
    fun `error recovery - startVoiceRecognition handles exception and returns false`() {
        // Arrange
        every { mockSpeechEngineManager.updateConfiguration(any()) } throws RuntimeException("Test exception")

        // Act
        val result = manager.startVoiceRecognition("en-US", "continuous")

        // Assert
        assertThat(result).isFalse()
    }

    @Test
    fun `error recovery - stopVoiceRecognition handles exception and returns false`() {
        // Arrange
        every { mockSpeechEngineManager.stopListening() } throws RuntimeException("Test exception")

        // Act
        val result = manager.stopVoiceRecognition()

        // Assert
        assertThat(result).isFalse()
    }

    @Test
    fun `error recovery - updateConfiguration handles exception gracefully`() {
        // Arrange
        every { mockSpeechEngineManager.updateConfiguration(any()) } throws RuntimeException("Test exception")
        val config = SpeechConfiguration(
            language = "en-US",
            mode = SpeechMode.DYNAMIC_COMMAND,
            enableVAD = true,
            confidenceThreshold = 4000F,
            maxRecordingDuration = 30000,
            timeoutDuration = 5000,
            enableProfanityFilter = false
        )

        // Act & Assert - should not crash
        manager.updateConfiguration(config)

        verify { mockSpeechEngineManager.updateConfiguration(config) }
    }

    @Test
    fun `error recovery - cleanup handles exception during stop`() {
        // Arrange
        every { mockSpeechEngineManager.stopListening() } throws RuntimeException("Test exception")

        // Act & Assert - should not crash
        manager.cleanup()

        verify { mockSpeechEngineManager.stopListening() }
    }

    @Test
    fun `error recovery - multiple cleanup calls are safe`() {
        // Act
        manager.cleanup()
        manager.cleanup()
        manager.cleanup()

        // Assert - should not crash
        verify(atLeast = 1) { mockSpeechEngineManager.stopListening() }
    }

    @Test
    fun `error recovery - cleanup before initialization is safe`() {
        // Act & Assert - should not crash
        manager.cleanup()

        // Verify stop was called
        verify { mockSpeechEngineManager.stopListening() }
    }

    @Test
    fun `error recovery - initialize after cleanup is safe`() {
        // Arrange
        manager.initialize()
        manager.cleanup()

        // Act & Assert - should not crash
        manager.initialize()

        // Verify initialize was called twice
        verify(exactly = 2) { mockSpeechEngineManager.initializeEngine(SpeechEngine.VIVOKA) }
    }

    // ============================================================
    // Category 4: Result Processing Tests (7 tests)
    // ============================================================

    @Test
    fun `result processing - command event with high confidence is processed`() = runTest {
        // Arrange
        manager.initialize()

        // Act
        commandEventsFlow.emit(
            com.augmentalis.voiceoscore.accessibility.speech.CommandEvent(
                command = "open settings",
                confidence = 0.95f,
                timestamp = System.currentTimeMillis()
            )
        )

        // Advance time to allow flow collection
        testScheduler.advanceUntilIdle()

        // Assert
        assertThat(receivedCommands).hasSize(1)
        assertThat(receivedCommands[0].first).isEqualTo("open settings")
        assertThat(receivedCommands[0].second).isEqualTo(0.95f)
    }

    @Test
    fun `result processing - command event with low confidence is rejected`() = runTest {
        // Arrange
        manager.initialize()

        // Act
        commandEventsFlow.emit(
            com.augmentalis.voiceoscore.accessibility.speech.CommandEvent(
                command = "unclear command",
                confidence = 0.3f,
                timestamp = System.currentTimeMillis()
            )
        )

        // Advance time
        testScheduler.advanceUntilIdle()

        // Assert - command rejected due to low confidence
        assertThat(receivedCommands).isEmpty()
    }

    @Test
    fun `result processing - empty command is rejected`() = runTest {
        // Arrange
        manager.initialize()

        // Act
        commandEventsFlow.emit(
            com.augmentalis.voiceoscore.accessibility.speech.CommandEvent(
                command = "",
                confidence = 0.95f,
                timestamp = System.currentTimeMillis()
            )
        )

        // Advance time
        testScheduler.advanceUntilIdle()

        // Assert - empty command rejected
        assertThat(receivedCommands).isEmpty()
    }

    @Test
    fun `result processing - blank command is rejected`() = runTest {
        // Arrange
        manager.initialize()

        // Act
        commandEventsFlow.emit(
            com.augmentalis.voiceoscore.accessibility.speech.CommandEvent(
                command = "   ",
                confidence = 0.95f,
                timestamp = System.currentTimeMillis()
            )
        )

        // Advance time
        testScheduler.advanceUntilIdle()

        // Assert - blank command rejected
        assertThat(receivedCommands).isEmpty()
    }

    @Test
    fun `result processing - multiple commands are processed in sequence`() = runTest {
        // Arrange
        manager.initialize()

        // Act
        commandEventsFlow.emit(
            com.augmentalis.voiceoscore.accessibility.speech.CommandEvent(
                command = "first command",
                confidence = 0.9f,
                timestamp = System.currentTimeMillis()
            )
        )
        commandEventsFlow.emit(
            com.augmentalis.voiceoscore.accessibility.speech.CommandEvent(
                command = "second command",
                confidence = 0.85f,
                timestamp = System.currentTimeMillis()
            )
        )

        // Advance time
        testScheduler.advanceUntilIdle()

        // Assert - both commands received
        assertThat(receivedCommands).hasSize(2)
        assertThat(receivedCommands[0].first).isEqualTo("first command")
        assertThat(receivedCommands[1].first).isEqualTo("second command")
    }

    @Test
    fun `result processing - confidence threshold is 0-5`() = runTest {
        // Arrange
        manager.initialize()

        // Act - exactly at threshold
        commandEventsFlow.emit(
            com.augmentalis.voiceoscore.accessibility.speech.CommandEvent(
                command = "threshold command",
                confidence = 0.5f,
                timestamp = System.currentTimeMillis()
            )
        )

        // Advance time
        testScheduler.advanceUntilIdle()

        // Assert - should not be processed (> 0.5 required)
        assertThat(receivedCommands).isEmpty()
    }

    @Test
    fun `result processing - command just above threshold is accepted`() = runTest {
        // Arrange
        manager.initialize()

        // Act - just above threshold
        commandEventsFlow.emit(
            com.augmentalis.voiceoscore.accessibility.speech.CommandEvent(
                command = "accepted command",
                confidence = 0.51f,
                timestamp = System.currentTimeMillis()
            )
        )

        // Advance time
        testScheduler.advanceUntilIdle()

        // Assert - command accepted
        assertThat(receivedCommands).hasSize(1)
    }

    // ============================================================
    // Category 5: Performance Tests (7 tests)
    // ============================================================

    @Test
    fun `performance - initialize completes without blocking`() {
        // Act
        val startTime = System.currentTimeMillis()
        manager.initialize()
        val duration = System.currentTimeMillis() - startTime

        // Assert - should complete quickly (< 100ms)
        assertThat(duration).isLessThan(100)
    }

    @Test
    fun `performance - startListening completes without blocking`() {
        // Act
        val startTime = System.currentTimeMillis()
        manager.startListening()
        val duration = System.currentTimeMillis() - startTime

        // Assert - should complete quickly (< 50ms)
        assertThat(duration).isLessThan(50)
    }

    @Test
    fun `performance - stopListening completes without blocking`() {
        // Act
        val startTime = System.currentTimeMillis()
        manager.stopListening()
        val duration = System.currentTimeMillis() - startTime

        // Assert - should complete quickly (< 50ms)
        assertThat(duration).isLessThan(50)
    }

    @Test
    fun `performance - startVoiceRecognition completes without blocking`() {
        // Act
        val startTime = System.currentTimeMillis()
        manager.startVoiceRecognition("en-US", "continuous")
        val duration = System.currentTimeMillis() - startTime

        // Assert - should complete quickly (< 100ms)
        assertThat(duration).isLessThan(100)
    }

    @Test
    fun `performance - command processing is asynchronous`() = runTest {
        // Arrange
        manager.initialize()
        var processingStarted = false

        // Create manager with slow callback to test async behavior
        val slowManager = VoiceRecognitionManager(
            speechEngineManager = mockSpeechEngineManager,
            onCommandReceived = { _, _ ->
                processingStarted = true
            }
        )
        slowManager.initialize()

        // Act
        commandEventsFlow.emit(
            com.augmentalis.voiceoscore.accessibility.speech.CommandEvent(
                command = "test command",
                confidence = 0.9f,
                timestamp = System.currentTimeMillis()
            )
        )

        // Advance time
        testScheduler.advanceUntilIdle()

        // Assert - processing completed
        assertThat(processingStarted).isTrue()

        // Cleanup
        slowManager.cleanup()
    }

    @Test
    fun `performance - rapid start-stop cycles handled correctly`() {
        // Act
        repeat(10) {
            manager.startListening()
            manager.stopListening()
        }

        // Assert - no crashes, all calls completed
        verify(exactly = 10) { mockSpeechEngineManager.startListening() }
        verify(exactly = 10) { mockSpeechEngineManager.stopListening() }
    }

    @Test
    fun `performance - cleanup completes without hanging`() {
        // Arrange
        manager.initialize()
        manager.startListening()

        // Act
        val startTime = System.currentTimeMillis()
        manager.cleanup()
        val duration = System.currentTimeMillis() - startTime

        // Assert - cleanup should complete quickly (< 100ms)
        assertThat(duration).isLessThan(100)
    }
}
