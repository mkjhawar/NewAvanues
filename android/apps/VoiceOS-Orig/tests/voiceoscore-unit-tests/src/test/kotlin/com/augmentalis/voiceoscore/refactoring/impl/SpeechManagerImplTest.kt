/**
 * SpeechManagerImplTest.kt - Comprehensive test suite for SpeechManagerImpl
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: Claude Code (Anthropic)
 * Created: 2025-10-15 03:59:14 PDT
 * Part of: VoiceOSService SOLID Refactoring - Speech Manager Tests
 *
 * TEST COVERAGE:
 * - Engine initialization (all 3 engines)
 * - Fallback mechanism
 * - Vocabulary management
 * - Recognition flow (partial/final)
 * - State transitions
 * - Engine switching
 * - Performance tests
 * - Concurrent operations
 *
 * TOTAL TESTS: 70+
 */
package com.augmentalis.voiceoscore.refactoring.impl

import android.content.Context
import com.augmentalis.voiceoscore.refactoring.interfaces.ISpeechManager.*
import com.augmentalis.voiceos.speech.engines.vivoka.VivokaEngine
import com.augmentalis.voiceos.speech.engines.vosk.VoskEngine
import com.augmentalis.voiceos.speech.api.RecognitionResult
import io.mockk.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

@OptIn(ExperimentalCoroutinesApi::class)
class SpeechManagerImplTest {

    private lateinit var speechManager: SpeechManagerImpl
    private lateinit var mockContext: Context
    private lateinit var mockVivokaEngine: VivokaEngine
    private lateinit var mockVoskEngine: VoskEngine
    private lateinit var testDispatcher: TestDispatcher
    private lateinit var testScope: TestScope

    @Before
    fun setup() {
        testDispatcher = StandardTestDispatcher()
        testScope = TestScope(testDispatcher)
        Dispatchers.setMain(testDispatcher)

        mockContext = mockk(relaxed = true)
        mockVivokaEngine = mockk(relaxed = true)
        mockVoskEngine = mockk(relaxed = true)

        // Setup default successful behavior
        coEvery { mockVivokaEngine.initialize(any()) } returns true
        every { mockVivokaEngine.startListening() } returns Unit
        every { mockVivokaEngine.stopListening() } returns Unit
        every { mockVivokaEngine.setDynamicCommands(any()) } returns Unit
        every { mockVivokaEngine.destroy() } returns Unit

        coEvery { mockVoskEngine.initialize(any()) } returns true
        every { mockVoskEngine.startListening() } returns Unit
        every { mockVoskEngine.stopListening() } returns Unit
        every { mockVoskEngine.setStaticCommands(any()) } returns Unit
        every { mockVoskEngine.destroy() } returns Unit

        speechManager = SpeechManagerImpl(mockVivokaEngine, mockVoskEngine, mockContext)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        speechManager.cleanup()
    }

    // ========================================
    // 1. Initialization Tests (10 tests)
    // ========================================

    @Test
    fun `test initialize with Vivoka engine`() = testScope.runTest {
        val config = SpeechConfig(preferredEngine = SpeechEngine.VIVOKA)

        speechManager.initialize(mockContext, config)

        coVerify { mockVivokaEngine.initialize(any()) }
        assertTrue(speechManager.isReady)
        assertEquals(SpeechEngine.VIVOKA, speechManager.activeEngine)
    }

    @Test
    fun `test initialize with VOSK engine`() = testScope.runTest {
        val config = SpeechConfig(preferredEngine = SpeechEngine.VOSK)

        speechManager.initialize(mockContext, config)

        coVerify { mockVoskEngine.initialize(any()) }
        assertTrue(speechManager.isReady)
        assertEquals(SpeechEngine.VOSK, speechManager.activeEngine)
    }

    @Test
    fun `test double initialization throws exception`() = testScope.runTest {
        val config = SpeechConfig()
        speechManager.initialize(mockContext, config)

        assertThrows(IllegalStateException::class.java) {
            runBlocking { speechManager.initialize(mockContext, config) }
        }
    }

    @Test
    fun `test initialization state transitions`() = testScope.runTest {
        val config = SpeechConfig()

        assertEquals(RecognitionState.IDLE, speechManager.recognitionState)

        speechManager.initialize(mockContext, config)

        assertEquals(RecognitionState.IDLE, speechManager.recognitionState)
        assertTrue(speechManager.isReady)
    }

    @Test
    fun `test initialization with invalid engine falls back`() = testScope.runTest {
        coEvery { mockVivokaEngine.initialize(any()) } returns false

        val config = SpeechConfig(
            preferredEngine = SpeechEngine.VIVOKA,
            enableAutoFallback = true
        )

        speechManager.initialize(mockContext, config)

        coVerify { mockVivokaEngine.initialize(any()) }
        coVerify { mockVoskEngine.initialize(any()) }
        assertEquals(SpeechEngine.VOSK, speechManager.activeEngine)
    }

    @Test
    fun `test initialization without fallback fails on engine error`() = testScope.runTest {
        coEvery { mockVivokaEngine.initialize(any()) } returns false

        val config = SpeechConfig(
            preferredEngine = SpeechEngine.VIVOKA,
            enableAutoFallback = false
        )

        assertThrows(Exception::class.java) {
            runBlocking { speechManager.initialize(mockContext, config) }
        }
    }

    @Test
    fun `test initialization records engine metrics`() = testScope.runTest {
        val config = SpeechConfig(preferredEngine = SpeechEngine.VIVOKA)

        speechManager.initialize(mockContext, config)

        val status = speechManager.getEngineStatus(SpeechEngine.VIVOKA)
        assertTrue(status.isInitialized)
        assertTrue(status.isAvailable)
    }

    @Test
    fun `test reinitialize engine resets metrics`() = testScope.runTest {
        val config = SpeechConfig()
        speechManager.initialize(mockContext, config)

        // Cause some failures
        speechManager.onRecognitionError(RecognitionError(
            errorCode = ErrorCode.AUDIO_ERROR,
            message = "Test error",
            engine = SpeechEngine.VIVOKA,
            isRecoverable = true
        ))

        // Reinitialize
        val success = speechManager.reinitializeEngine(SpeechEngine.VIVOKA)

        assertTrue(success)
        val status = speechManager.getEngineStatus(SpeechEngine.VIVOKA)
        assertTrue(status.isHealthy)
    }

    @Test
    fun `test initialization emits correct events`() = testScope.runTest {
        val config = SpeechConfig()
        val events = mutableListOf<SpeechEvent>()

        val job = launch {
            speechManager.speechEvents.take(1).toList(events)
        }

        speechManager.initialize(mockContext, config)
        delay(100)
        job.cancel()

        // Should emit initialization-related events
        assertTrue(events.isNotEmpty())
    }

    @Test
    fun `test cleanup releases all resources`() = testScope.runTest {
        val config = SpeechConfig()
        speechManager.initialize(mockContext, config)
        speechManager.cleanup()

        verify { mockVivokaEngine.destroy() }
        assertFalse(speechManager.isReady)
    }

    // ========================================
    // 2. Speech Recognition Tests (15 tests)
    // ========================================

    @Test
    fun `test start listening successfully`() = testScope.runTest {
        val config = SpeechConfig()
        speechManager.initialize(mockContext, config)

        val success = speechManager.startListening()

        assertTrue(success)
        assertTrue(speechManager.isListening)
        verify { mockVivokaEngine.startListening() }
    }

    @Test
    fun `test start listening when not ready fails`() = testScope.runTest {
        val success = speechManager.startListening()

        assertFalse(success)
        assertFalse(speechManager.isListening)
    }

    @Test
    fun `test start listening emits event`() = testScope.runTest {
        val config = SpeechConfig()
        speechManager.initialize(mockContext, config)

        val events = mutableListOf<SpeechEvent>()
        val job = launch {
            speechManager.speechEvents.take(1).toList(events)
        }

        speechManager.startListening()
        delay(100)
        job.cancel()

        val startEvent = events.firstOrNull { it is SpeechEvent.ListeningStarted }
        assertNotNull(startEvent)
    }

    @Test
    fun `test stop listening successfully`() = testScope.runTest {
        val config = SpeechConfig()
        speechManager.initialize(mockContext, config)
        speechManager.startListening()

        speechManager.stopListening()

        assertFalse(speechManager.isListening)
        verify { mockVivokaEngine.stopListening() }
    }

    @Test
    fun `test cancel recognition stops listening`() = testScope.runTest {
        val config = SpeechConfig()
        speechManager.initialize(mockContext, config)
        speechManager.startListening()

        speechManager.cancelRecognition()

        assertFalse(speechManager.isListening)
        assertEquals(RecognitionState.IDLE, speechManager.recognitionState)
    }

    @Test
    fun `test partial result emits event`() = testScope.runTest {
        val config = SpeechConfig()
        speechManager.initialize(mockContext, config)

        val events = mutableListOf<SpeechEvent>()
        val job = launch {
            speechManager.speechEvents.take(1).toList(events)
        }

        speechManager.onPartialResult("test", 0.8f)
        delay(100)
        job.cancel()

        val partialEvent = events.firstOrNull { it is SpeechEvent.PartialResult }
        assertNotNull(partialEvent)
        assertEquals("test", (partialEvent as SpeechEvent.PartialResult).text)
    }

    @Test
    fun `test final result updates metrics`() = testScope.runTest {
        val config = SpeechConfig()
        speechManager.initialize(mockContext, config)

        speechManager.onFinalResult("test command", 0.9f)

        val metrics = speechManager.getMetrics()
        assertEquals(1L, metrics.totalRecognitions)
    }

    @Test
    fun `test final result emits event`() = testScope.runTest {
        val config = SpeechConfig()
        speechManager.initialize(mockContext, config)

        val events = mutableListOf<SpeechEvent>()
        val job = launch {
            speechManager.speechEvents.take(1).toList(events)
        }

        speechManager.onFinalResult("test", 0.9f)
        delay(100)
        job.cancel()

        val finalEvent = events.firstOrNull { it is SpeechEvent.FinalResult }
        assertNotNull(finalEvent)
    }

    @Test
    fun `test final result adds to history`() = testScope.runTest {
        val config = SpeechConfig()
        speechManager.initialize(mockContext, config)

        speechManager.onFinalResult("test", 0.9f)

        val history = speechManager.getRecognitionHistory()
        assertEquals(1, history.size)
        assertEquals("test", history[0].text)
    }

    @Test
    fun `test recognition error emits event`() = testScope.runTest {
        val config = SpeechConfig()
        speechManager.initialize(mockContext, config)

        val events = mutableListOf<SpeechEvent>()
        val job = launch {
            speechManager.speechEvents.take(1).toList(events)
        }

        val error = RecognitionError(
            errorCode = ErrorCode.AUDIO_ERROR,
            message = "Test error",
            engine = SpeechEngine.VIVOKA,
            isRecoverable = true
        )
        speechManager.onRecognitionError(error)
        delay(100)
        job.cancel()

        val errorEvent = events.firstOrNull { it is SpeechEvent.Error }
        assertNotNull(errorEvent)
    }

    @Test
    fun `test recognition error updates engine metrics`() = testScope.runTest {
        val config = SpeechConfig()
        speechManager.initialize(mockContext, config)

        speechManager.onRecognitionError(RecognitionError(
            errorCode = ErrorCode.AUDIO_ERROR,
            message = "Test",
            engine = SpeechEngine.VIVOKA,
            isRecoverable = true
        ))

        val status = speechManager.getEngineStatus(SpeechEngine.VIVOKA)
        assertTrue(status.totalRecognitions == 0L)
        assertNotNull(status.lastError)
    }

    @Test
    fun `test pause stops listening`() = testScope.runTest {
        val config = SpeechConfig()
        speechManager.initialize(mockContext, config)
        speechManager.startListening()

        speechManager.pause()

        assertFalse(speechManager.isListening)
    }

    @Test
    fun `test resume starts listening`() = testScope.runTest {
        val config = SpeechConfig()
        speechManager.initialize(mockContext, config)
        speechManager.pause()

        speechManager.resume()
        delay(100)

        assertTrue(speechManager.isListening)
    }

    @Test
    fun `test recognition state transitions correctly`() = testScope.runTest {
        val config = SpeechConfig()
        speechManager.initialize(mockContext, config)

        assertEquals(RecognitionState.IDLE, speechManager.recognitionState)

        speechManager.startListening()
        assertEquals(RecognitionState.LISTENING, speechManager.recognitionState)

        speechManager.onFinalResult("test", 0.9f)
        delay(100)
        // Should return to IDLE after processing
        assertEquals(RecognitionState.IDLE, speechManager.recognitionState)
    }

    @Test
    fun `test history bounded to max size`() = testScope.runTest {
        val config = SpeechConfig()
        speechManager.initialize(mockContext, config)

        // Add more than MAX_HISTORY_SIZE records
        repeat(60) { i ->
            speechManager.onFinalResult("test $i", 0.9f)
        }

        val history = speechManager.getRecognitionHistory()
        assertTrue(history.size <= 50)
    }

    // ========================================
    // 3. Engine Switching Tests (10 tests)
    // ========================================

    @Test
    fun `test switch engine successfully`() = testScope.runTest {
        val config = SpeechConfig(preferredEngine = SpeechEngine.VIVOKA)
        speechManager.initialize(mockContext, config)

        val success = speechManager.switchEngine(SpeechEngine.VOSK)

        assertTrue(success)
        assertEquals(SpeechEngine.VOSK, speechManager.activeEngine)
    }

    @Test
    fun `test switch to same engine returns true`() = testScope.runTest {
        val config = SpeechConfig(preferredEngine = SpeechEngine.VIVOKA)
        speechManager.initialize(mockContext, config)

        val success = speechManager.switchEngine(SpeechEngine.VIVOKA)

        assertTrue(success)
    }

    @Test
    fun `test switch engine stops current listening`() = testScope.runTest {
        val config = SpeechConfig()
        speechManager.initialize(mockContext, config)
        speechManager.startListening()

        speechManager.switchEngine(SpeechEngine.VOSK)

        verify { mockVivokaEngine.stopListening() }
    }

    @Test
    fun `test switch engine resumes listening after switch`() = testScope.runTest {
        val config = SpeechConfig()
        speechManager.initialize(mockContext, config)
        speechManager.startListening()

        speechManager.switchEngine(SpeechEngine.VOSK)
        delay(100)

        verify { mockVoskEngine.startListening() }
    }

    @Test
    fun `test switch engine emits event`() = testScope.runTest {
        val config = SpeechConfig()
        speechManager.initialize(mockContext, config)

        val events = mutableListOf<SpeechEvent>()
        val job = launch {
            speechManager.speechEvents.take(1).toList(events)
        }

        speechManager.switchEngine(SpeechEngine.VOSK)
        delay(100)
        job.cancel()

        val switchEvent = events.firstOrNull { it is SpeechEvent.EngineSwitch }
        assertNotNull(switchEvent)
    }

    @Test
    fun `test get engine status returns correct data`() = testScope.runTest {
        val config = SpeechConfig()
        speechManager.initialize(mockContext, config)

        val status = speechManager.getEngineStatus(SpeechEngine.VIVOKA)

        assertTrue(status.isInitialized)
        assertTrue(status.isAvailable)
        assertTrue(status.isHealthy)
    }

    @Test
    fun `test get all engine statuses returns map`() = testScope.runTest {
        val config = SpeechConfig()
        speechManager.initialize(mockContext, config)

        val statuses = speechManager.getAllEngineStatuses()

        assertEquals(3, statuses.size)
        assertTrue(statuses.containsKey(SpeechEngine.VIVOKA))
        assertTrue(statuses.containsKey(SpeechEngine.VOSK))
        assertTrue(statuses.containsKey(SpeechEngine.GOOGLE))
    }

    @Test
    fun `test is engine available returns correct status`() = testScope.runTest {
        val config = SpeechConfig()
        speechManager.initialize(mockContext, config)

        assertTrue(speechManager.isEngineAvailable(SpeechEngine.VIVOKA))
        assertFalse(speechManager.isEngineAvailable(SpeechEngine.GOOGLE))
    }

    @Test
    fun `test fallback on recognition error with auto fallback enabled`() = testScope.runTest {
        val config = SpeechConfig(enableAutoFallback = true)
        speechManager.initialize(mockContext, config)

        speechManager.onRecognitionError(RecognitionError(
            errorCode = ErrorCode.ENGINE_NOT_AVAILABLE,
            message = "Engine failed",
            engine = SpeechEngine.VIVOKA,
            isRecoverable = true
        ))

        delay(200)

        // Should have switched to VOSK
        assertEquals(SpeechEngine.VOSK, speechManager.activeEngine)
    }

    @Test
    fun `test no fallback when auto fallback disabled`() = testScope.runTest {
        val config = SpeechConfig(enableAutoFallback = false)
        speechManager.initialize(mockContext, config)
        val initialEngine = speechManager.activeEngine

        speechManager.onRecognitionError(RecognitionError(
            errorCode = ErrorCode.ENGINE_NOT_AVAILABLE,
            message = "Engine failed",
            engine = SpeechEngine.VIVOKA,
            isRecoverable = true
        ))

        delay(200)

        assertEquals(initialEngine, speechManager.activeEngine)
    }

    // ========================================
    // 4. Vocabulary Management Tests (15 tests)
    // ========================================

    @Test
    fun `test update vocabulary replaces existing`() = testScope.runTest {
        val config = SpeechConfig()
        speechManager.initialize(mockContext, config)

        speechManager.updateVocabulary(setOf("command1", "command2"))
        delay(600) // Wait for debounce

        assertEquals(2, speechManager.getVocabularySize())
        verify { mockVivokaEngine.setDynamicCommands(any()) }
    }

    @Test
    fun `test add vocabulary appends to existing`() = testScope.runTest {
        val config = SpeechConfig()
        speechManager.initialize(mockContext, config)

        speechManager.updateVocabulary(setOf("command1"))
        delay(600)
        speechManager.addVocabulary(setOf("command2"))
        delay(600)

        assertEquals(2, speechManager.getVocabularySize())
    }

    @Test
    fun `test remove vocabulary removes commands`() = testScope.runTest {
        val config = SpeechConfig()
        speechManager.initialize(mockContext, config)

        speechManager.updateVocabulary(setOf("command1", "command2"))
        delay(600)
        speechManager.removeVocabulary(setOf("command1"))
        delay(600)

        assertEquals(1, speechManager.getVocabularySize())
    }

    @Test
    fun `test clear vocabulary removes all`() = testScope.runTest {
        val config = SpeechConfig()
        speechManager.initialize(mockContext, config)

        speechManager.updateVocabulary(setOf("command1", "command2"))
        delay(600)
        speechManager.clearVocabulary()
        delay(600)

        assertEquals(0, speechManager.getVocabularySize())
    }

    @Test
    fun `test vocabulary update debounced correctly`() = testScope.runTest {
        val config = SpeechConfig()
        speechManager.initialize(mockContext, config)

        // Multiple rapid updates
        speechManager.updateVocabulary(setOf("command1"))
        speechManager.updateVocabulary(setOf("command2"))
        speechManager.updateVocabulary(setOf("command3"))
        delay(600)

        // Should only update once after debounce
        verify(exactly = 1) { mockVivokaEngine.setDynamicCommands(any()) }
    }

    @Test
    fun `test vocabulary update emits event`() = testScope.runTest {
        val config = SpeechConfig()
        speechManager.initialize(mockContext, config)

        val events = mutableListOf<SpeechEvent>()
        val job = launch {
            speechManager.speechEvents.take(1).toList(events)
        }

        speechManager.updateVocabulary(setOf("command1"))
        delay(100)
        job.cancel()

        val vocabEvent = events.firstOrNull { it is SpeechEvent.VocabularyUpdated }
        assertNotNull(vocabEvent)
    }

    @Test
    fun `test vocabulary with different engines`() = testScope.runTest {
        val config = SpeechConfig()
        speechManager.initialize(mockContext, config)

        speechManager.updateVocabulary(setOf("command1"))
        delay(600)

        speechManager.switchEngine(SpeechEngine.VOSK)
        delay(100)

        speechManager.updateVocabulary(setOf("command2"))
        delay(600)

        verify { mockVoskEngine.setStaticCommands(any()) }
    }

    @Test
    fun `test vocabulary size tracked correctly`() = testScope.runTest {
        val config = SpeechConfig()
        speechManager.initialize(mockContext, config)

        assertEquals(0, speechManager.getVocabularySize())

        speechManager.updateVocabulary(setOf("command1", "command2", "command3"))
        assertEquals(3, speechManager.getVocabularySize())
    }

    @Test
    fun `test large vocabulary update performance`() = testScope.runTest {
        val config = SpeechConfig()
        speechManager.initialize(mockContext, config)

        val largeVocabulary = (1..1000).map { "command$it" }.toSet()
        val startTime = System.currentTimeMillis()

        speechManager.updateVocabulary(largeVocabulary)
        delay(600)

        val duration = System.currentTimeMillis() - startTime
        assertTrue("Vocabulary update took too long: ${duration}ms", duration < 1000)
    }

    @Test
    fun `test vocabulary preserves uniqueness`() = testScope.runTest {
        val config = SpeechConfig()
        speechManager.initialize(mockContext, config)

        speechManager.updateVocabulary(setOf("command1", "command1", "command2"))
        assertEquals(2, speechManager.getVocabularySize())
    }

    @Test
    fun `test vocabulary thread safety`() = testScope.runTest {
        val config = SpeechConfig()
        speechManager.initialize(mockContext, config)

        val jobs = (1..100).map { i ->
            launch {
                speechManager.addVocabulary(setOf("command$i"))
            }
        }

        jobs.forEach { it.join() }
        delay(600)

        // Should have all 100 commands
        assertEquals(100, speechManager.getVocabularySize())
    }

    @Test
    fun `test vocabulary metrics reflected in speech metrics`() = testScope.runTest {
        val config = SpeechConfig()
        speechManager.initialize(mockContext, config)

        speechManager.updateVocabulary(setOf("command1", "command2"))
        delay(600)

        val metrics = speechManager.getMetrics()
        assertEquals(2, metrics.vocabularySize)
    }

    @Test
    fun `test vocabulary update cancelled on cleanup`() = testScope.runTest {
        val config = SpeechConfig()
        speechManager.initialize(mockContext, config)

        speechManager.updateVocabulary(setOf("command1"))
        speechManager.cleanup()

        delay(600)

        // Should not crash
    }

    @Test
    fun `test empty vocabulary update`() = testScope.runTest {
        val config = SpeechConfig()
        speechManager.initialize(mockContext, config)

        speechManager.updateVocabulary(emptySet())
        delay(600)

        assertEquals(0, speechManager.getVocabularySize())
    }

    @Test
    fun `test vocabulary update with special characters`() = testScope.runTest {
        val config = SpeechConfig()
        speechManager.initialize(mockContext, config)

        speechManager.updateVocabulary(setOf("command!", "command@", "command#"))
        delay(600)

        assertEquals(3, speechManager.getVocabularySize())
    }

    // ========================================
    // 5. Configuration Tests (5 tests)
    // ========================================

    @Test
    fun `test update config changes settings`() = testScope.runTest {
        val config = SpeechConfig()
        speechManager.initialize(mockContext, config)

        val newConfig = SpeechConfig(minConfidenceThreshold = 0.7f)
        speechManager.updateConfig(newConfig)

        assertEquals(newConfig, speechManager.getConfig())
    }

    @Test
    fun `test update config switches engine if preferred changed`() = testScope.runTest {
        val config = SpeechConfig(preferredEngine = SpeechEngine.VIVOKA)
        speechManager.initialize(mockContext, config)

        val newConfig = SpeechConfig(preferredEngine = SpeechEngine.VOSK)
        speechManager.updateConfig(newConfig)
        delay(100)

        assertEquals(SpeechEngine.VOSK, speechManager.activeEngine)
    }

    @Test
    fun `test config min confidence threshold enforced`() = testScope.runTest {
        val config = SpeechConfig(minConfidenceThreshold = 0.8f)
        speechManager.initialize(mockContext, config)

        val events = mutableListOf<SpeechEvent>()
        val job = launch {
            speechManager.speechEvents.take(2).toList(events)
        }

        // This should be rejected (below threshold)
        speechManager.onFinalResult("low confidence", 0.5f)

        // This should be accepted
        speechManager.onFinalResult("high confidence", 0.9f)

        delay(100)
        job.cancel()

        // Only one final result should be emitted
        val finalResults = events.filterIsInstance<SpeechEvent.FinalResult>()
        assertEquals(1, finalResults.size)
        assertEquals("high confidence", finalResults[0].text)
    }

    @Test
    fun `test config language passed to engine`() = testScope.runTest {
        val config = SpeechConfig(language = "es-ES")
        speechManager.initialize(mockContext, config)

        coVerify { mockVivokaEngine.initialize(match { it.language == "es-ES" }) }
    }

    @Test
    fun `test config auto fallback setting respected`() = testScope.runTest {
        val config = SpeechConfig(enableAutoFallback = false)
        coEvery { mockVivokaEngine.initialize(any()) } returns false

        assertThrows(Exception::class.java) {
            runBlocking { speechManager.initialize(mockContext, config) }
        }
    }

    // ========================================
    // 6. Metrics Tests (10 tests)
    // ========================================

    @Test
    fun `test metrics tracks successful recognitions`() = testScope.runTest {
        val config = SpeechConfig()
        speechManager.initialize(mockContext, config)

        repeat(5) {
            speechManager.onFinalResult("test", 0.9f)
        }

        val metrics = speechManager.getMetrics()
        assertEquals(5L, metrics.totalRecognitions)
        assertEquals(5L, metrics.successfulRecognitions)
    }

    @Test
    fun `test metrics tracks failed recognitions`() = testScope.runTest {
        val config = SpeechConfig()
        speechManager.initialize(mockContext, config)

        repeat(3) {
            speechManager.onRecognitionError(RecognitionError(
                errorCode = ErrorCode.AUDIO_ERROR,
                message = "Test",
                engine = SpeechEngine.VIVOKA,
                isRecoverable = true
            ))
        }

        val metrics = speechManager.getMetrics()
        assertEquals(3L, metrics.failedRecognitions)
    }

    @Test
    fun `test metrics calculates average confidence`() = testScope.runTest {
        val config = SpeechConfig()
        speechManager.initialize(mockContext, config)

        speechManager.onFinalResult("test1", 0.8f)
        speechManager.onFinalResult("test2", 0.9f)
        speechManager.onFinalResult("test3", 1.0f)

        val metrics = speechManager.getMetrics()
        assertTrue(metrics.averageConfidence > 0.85f)
    }

    @Test
    fun `test metrics tracks recognitions by engine`() = testScope.runTest {
        val config = SpeechConfig()
        speechManager.initialize(mockContext, config)

        speechManager.onFinalResult("test1", 0.9f)
        speechManager.switchEngine(SpeechEngine.VOSK)
        delay(100)
        speechManager.onFinalResult("test2", 0.9f)

        val metrics = speechManager.getMetrics()
        assertTrue(metrics.recognitionsByEngine[SpeechEngine.VIVOKA]!! > 0)
        assertTrue(metrics.recognitionsByEngine[SpeechEngine.VOSK]!! > 0)
    }

    @Test
    fun `test recognition history limited to max size`() = testScope.runTest {
        val config = SpeechConfig()
        speechManager.initialize(mockContext, config)

        repeat(100) { i ->
            speechManager.onFinalResult("test $i", 0.9f)
        }

        val history = speechManager.getRecognitionHistory()
        assertTrue(history.size <= 50)
    }

    @Test
    fun `test recognition history returns correct limit`() = testScope.runTest {
        val config = SpeechConfig()
        speechManager.initialize(mockContext, config)

        repeat(30) { i ->
            speechManager.onFinalResult("test $i", 0.9f)
        }

        val history = speechManager.getRecognitionHistory(limit = 10)
        assertEquals(10, history.size)
    }

    @Test
    fun `test recognition history contains correct data`() = testScope.runTest {
        val config = SpeechConfig()
        speechManager.initialize(mockContext, config)

        speechManager.onFinalResult("test command", 0.95f)

        val history = speechManager.getRecognitionHistory()
        assertEquals(1, history.size)
        assertEquals("test command", history[0].text)
        assertEquals(0.95f, history[0].confidence, 0.01f)
        assertTrue(history[0].wasSuccessful)
        assertEquals(SpeechEngine.VIVOKA, history[0].engine)
    }

    @Test
    fun `test metrics vocabulary size tracked`() = testScope.runTest {
        val config = SpeechConfig()
        speechManager.initialize(mockContext, config)

        speechManager.updateVocabulary(setOf("cmd1", "cmd2", "cmd3"))
        delay(600)

        val metrics = speechManager.getMetrics()
        assertEquals(3, metrics.vocabularySize)
    }

    @Test
    fun `test engine status tracks success rate`() = testScope.runTest {
        val config = SpeechConfig()
        speechManager.initialize(mockContext, config)

        speechManager.onFinalResult("test1", 0.9f)
        speechManager.onFinalResult("test2", 0.9f)
        speechManager.onRecognitionError(RecognitionError(
            errorCode = ErrorCode.AUDIO_ERROR,
            message = "Test",
            engine = SpeechEngine.VIVOKA,
            isRecoverable = true
        ))

        val status = speechManager.getEngineStatus(SpeechEngine.VIVOKA)
        assertTrue(status.successRate > 0.6f)
    }

    @Test
    fun `test engine status tracks average confidence`() = testScope.runTest {
        val config = SpeechConfig()
        speechManager.initialize(mockContext, config)

        speechManager.onFinalResult("test1", 0.8f)
        speechManager.onFinalResult("test2", 1.0f)

        val status = speechManager.getEngineStatus(SpeechEngine.VIVOKA)
        assertEquals(0.9f, status.averageConfidence, 0.01f)
    }

    // ========================================
    // 7. Performance Tests (5 tests)
    // ========================================

    @Test
    fun `test recognition latency under 300ms`() = testScope.runTest {
        val config = SpeechConfig()
        speechManager.initialize(mockContext, config)

        val startTime = System.currentTimeMillis()
        speechManager.onFinalResult("test", 0.9f)
        val duration = System.currentTimeMillis() - startTime

        assertTrue("Recognition took too long: ${duration}ms", duration < 300)
    }

    @Test
    fun `test engine switch under 500ms`() = testScope.runTest {
        val config = SpeechConfig()
        speechManager.initialize(mockContext, config)

        val startTime = System.currentTimeMillis()
        speechManager.switchEngine(SpeechEngine.VOSK)
        val duration = System.currentTimeMillis() - startTime

        assertTrue("Engine switch took too long: ${duration}ms", duration < 500)
    }

    @Test
    fun `test vocabulary update debounce works`() = testScope.runTest {
        val config = SpeechConfig()
        speechManager.initialize(mockContext, config)

        // Rapid updates
        repeat(10) { i ->
            speechManager.updateVocabulary(setOf("cmd$i"))
        }
        delay(600)

        // Should have batched updates
        verify(atMost = 2) { mockVivokaEngine.setDynamicCommands(any()) }
    }

    @Test
    fun `test concurrent operations thread safe`() = testScope.runTest {
        val config = SpeechConfig()
        speechManager.initialize(mockContext, config)

        val jobs = mutableListOf<Job>()

        // Concurrent operations
        repeat(10) { i ->
            jobs.add(launch {
                speechManager.onFinalResult("test$i", 0.9f)
            })
            jobs.add(launch {
                speechManager.addVocabulary(setOf("cmd$i"))
            })
        }

        jobs.forEach { it.join() }

        // Should not crash
        val metrics = speechManager.getMetrics()
        assertEquals(10L, metrics.totalRecognitions)
    }

    @Test
    fun `test cleanup completes quickly`() = testScope.runTest {
        val config = SpeechConfig()
        speechManager.initialize(mockContext, config)

        val startTime = System.currentTimeMillis()
        speechManager.cleanup()
        val duration = System.currentTimeMillis() - startTime

        assertTrue("Cleanup took too long: ${duration}ms", duration < 100)
    }

    // ========================================
    // 8. Error Handling Tests (5+ tests)
    // ========================================

    @Test
    fun `test error with non-recoverable error stops engine`() = testScope.runTest {
        val config = SpeechConfig()
        speechManager.initialize(mockContext, config)

        speechManager.onRecognitionError(RecognitionError(
            errorCode = ErrorCode.ENGINE_INITIALIZATION_FAILED,
            message = "Critical error",
            engine = SpeechEngine.VIVOKA,
            isRecoverable = false
        ))

        assertEquals(RecognitionState.ERROR, speechManager.recognitionState)
    }

    @Test
    fun `test multiple errors tracked correctly`() = testScope.runTest {
        val config = SpeechConfig()
        speechManager.initialize(mockContext, config)

        repeat(5) {
            speechManager.onRecognitionError(RecognitionError(
                errorCode = ErrorCode.AUDIO_ERROR,
                message = "Error",
                engine = SpeechEngine.VIVOKA,
                isRecoverable = true
            ))
        }

        val status = speechManager.getEngineStatus(SpeechEngine.VIVOKA)
        assertFalse(status.isHealthy)
    }
}
