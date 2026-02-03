/**
 * SpeechRecognitionManagerTest.kt - Comprehensive unit tests for SpeechRecognitionManager
 * 
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2025-01-28
 * 
 * Tests core speech recognition functionality, engine switching, and error handling
 */
package com.augmentalis.speechrecognition

import android.content.Context
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import com.augmentalis.voiceos.speech.api.RecognitionResult
import com.augmentalis.speechrecognition.test.TestConfig
import com.augmentalis.speechrecognition.SpeechRecognitionManager
import com.augmentalis.speechrecognition.SpeechRecognitionCallback
import com.augmentalis.speechrecognition.RecognitionStatus
import com.augmentalis.speechrecognition.SpeechConfig
import com.augmentalis.speechrecognition.SpeechEngine
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import kotlinx.coroutines.Dispatchers
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations
import org.mockito.junit.MockitoJUnitRunner
import kotlin.test.*

@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class SpeechRecognitionManagerTest {
    
    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()
    
    @Mock
    private lateinit var mockContext: Context
    
    @Mock
    private lateinit var mockResultCallback: SpeechRecognitionCallback
    
    @Mock
    private lateinit var statusObserver: Observer<RecognitionStatus>
    
    @Mock
    private lateinit var resultObserver: Observer<RecognitionResult?>
    
    @Mock
    private lateinit var errorObserver: Observer<String?>
    
    private lateinit var speechManager: SpeechRecognitionManager
    private val testDispatcher = UnconfinedTestDispatcher()
    
    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        Dispatchers.setMain(testDispatcher)
        
        // Mock context
        `when`(mockContext.applicationContext).thenReturn(mockContext)
        
        speechManager = SpeechRecognitionManager(mockContext)
        
        // Setup observers
        speechManager.recognitionStatus.observeForever(statusObserver)
        speechManager.recognitionResult.observeForever(resultObserver)
        speechManager.errorMessage.observeForever(errorObserver)
    }
    
    @After
    fun tearDown() {
        speechManager.recognitionStatus.removeObserver(statusObserver)
        speechManager.recognitionResult.removeObserver(resultObserver)
        speechManager.errorMessage.removeObserver(errorObserver)
        Dispatchers.resetMain()
    }
    
    @Test
    fun `test initial state`() {
        assertEquals(RecognitionStatus.IDLE, speechManager.recognitionStatus.value)
        assertNull(speechManager.recognitionResult.value)
        assertNull(speechManager.errorMessage.value)
        assertFalse(speechManager.isListening())
    }
    
    @Test
    fun `test initialize with valid engine`() = runTest {
        val config = TestConfig.createBasicConfig(SpeechEngine.VOSK)
        
        val result = speechManager.initialize(config)
        
        assertTrue(result)
        verify(statusObserver).onChanged(RecognitionStatus.INITIALIZING)
    }
    
    @Test
    fun `test initialize with invalid configuration`() = runTest {
        val invalidConfig = SpeechConfig.default().copy(
            confidenceThreshold = -1.0f // Invalid threshold
        )
        
        val result = speechManager.initialize(invalidConfig)
        
        assertFalse(result)
        verify(errorObserver).onChanged(argThat { it?.contains("Invalid") == true })
    }
    
    @Test
    fun `test start listening with testable engine`() = runTest {
        val config = TestConfig.createBasicConfig(SpeechEngine.VOSK)
        speechManager.initialize(config)
        
        val result = speechManager.startListening(mockResultCallback)
        
        if (TestConfig.isTestableEngine(SpeechEngine.VOSK)) {
            assertTrue(result)
            verify(statusObserver).onChanged(RecognitionStatus.LISTENING)
        } else {
            assertFalse(result)
            verify(errorObserver).onChanged(argThat { it?.contains("not available") == true })
        }
    }
    
    @Test
    fun `test stop listening while active`() = runTest {
        val config = TestConfig.createBasicConfig(SpeechEngine.ANDROID_STT)
        speechManager.initialize(config)
        speechManager.startListening(mockResultCallback)
        
        val result = speechManager.stopListening()
        
        assertTrue(result)
        verify(statusObserver).onChanged(RecognitionStatus.IDLE)
    }
    
    @Test
    fun `test stop listening when not active`() = runTest {
        val result = speechManager.stopListening()
        
        assertTrue(result) // Should succeed gracefully
        assertEquals(RecognitionStatus.IDLE, speechManager.recognitionStatus.value)
    }
    
    @Test
    fun `test engine switching`() = runTest {
        val voskConfig = TestConfig.createBasicConfig(SpeechEngine.VOSK)
        val androidConfig = TestConfig.createBasicConfig(SpeechEngine.ANDROID_STT)
        
        // Initialize with first engine
        speechManager.initialize(voskConfig)
        
        // Switch to second engine
        val result = speechManager.switchEngine(androidConfig)
        
        assertTrue(result)
        verify(statusObserver, atLeast(2)).onChanged(RecognitionStatus.INITIALIZING)
    }
    
    @Test
    fun `test callback invocation on recognition result`() = runTest {
        val config = TestConfig.createBasicConfig(SpeechEngine.ANDROID_STT)
        speechManager.initialize(config)
        speechManager.startListening(mockResultCallback)
        
        // Simulate recognition result
        val testResult = RecognitionResult(
            text = "test command",
            confidence = 0.85f,
            isFinal = true,
            engine = SpeechEngine.ANDROID_STT.name
        )
        
        speechManager.onRecognitionResult(testResult)
        
        verify(mockResultCallback).onResult(testResult)
        verify(resultObserver).onChanged(testResult)
    }
    
    @Test
    fun `test error handling during recognition`() = runTest {
        val config = TestConfig.createBasicConfig(SpeechEngine.VOSK)
        speechManager.initialize(config)
        speechManager.startListening(mockResultCallback)
        
        val errorMessage = "Recognition failed"
        speechManager.onRecognitionError(errorMessage)
        
        verify(mockResultCallback).onError(errorMessage)
        verify(errorObserver).onChanged(errorMessage)
        verify(statusObserver).onChanged(RecognitionStatus.ERROR)
    }
    
    @Test
    fun `test confidence threshold filtering`() = runTest {
        val config = TestConfig.createBasicConfig(SpeechEngine.ANDROID_STT)
            .copy(confidenceThreshold = 0.8f)
        
        speechManager.initialize(config)
        speechManager.startListening(mockResultCallback)
        
        // Test low confidence result
        val lowConfidenceResult = RecognitionResult(
            text = "unclear command",
            confidence = 0.5f,
            isFinal = true,
            engine = SpeechEngine.ANDROID_STT.name
        )
        
        speechManager.onRecognitionResult(lowConfidenceResult)
        
        // Should not invoke callback due to low confidence
        verify(mockResultCallback, never()).onResult(lowConfidenceResult)
        
        // Test high confidence result
        val highConfidenceResult = RecognitionResult(
            text = "clear command",
            confidence = 0.9f,
            isFinal = true,
            engine = SpeechEngine.ANDROID_STT.name
        )
        
        speechManager.onRecognitionResult(highConfidenceResult)
        
        // Should invoke callback
        verify(mockResultCallback).onResult(highConfidenceResult)
    }
    
    @Test
    fun `test timeout handling`() = runTest {
        val config = TestConfig.createBasicConfig(SpeechEngine.ANDROID_STT)
            .copy(timeoutDuration = 100L) // Very short timeout
        
        speechManager.initialize(config)
        speechManager.startListening(mockResultCallback)
        
        // Wait for timeout
        advanceTimeBy(200L)
        
        verify(statusObserver).onChanged(RecognitionStatus.TIMEOUT)
        verify(mockResultCallback).onError(argThat { it.contains("timeout", ignoreCase = true) })
    }
    
    @Test
    fun `test multiple concurrent recognition attempts`() = runTest {
        val config = TestConfig.createBasicConfig(SpeechEngine.ANDROID_STT)
        speechManager.initialize(config)
        
        // Start first recognition
        val result1 = speechManager.startListening(mockResultCallback)
        assertTrue(result1)
        
        // Attempt second recognition while first is active
        val result2 = speechManager.startListening(mockResultCallback)
        assertFalse(result2) // Should fail
        
        verify(errorObserver).onChanged(argThat { it?.contains("already listening") == true })
    }
    
    @Test
    fun `test partial results handling`() = runTest {
        val config = TestConfig.createBasicConfig(SpeechEngine.ANDROID_STT)
        speechManager.initialize(config)
        speechManager.startListening(mockResultCallback)
        
        // Test partial result
        val partialResult = RecognitionResult(
            text = "partial",
            confidence = 0.7f,
            isFinal = false,
            engine = SpeechEngine.ANDROID_STT.name
        )
        
        speechManager.onRecognitionResult(partialResult)
        
        verify(mockResultCallback).onPartialResult(partialResult.text)
        verify(resultObserver).onChanged(partialResult)
    }
    
    @Test
    fun `test cleanup on destruction`() = runTest {
        val config = TestConfig.createBasicConfig(SpeechEngine.ANDROID_STT)
        speechManager.initialize(config)
        speechManager.startListening(mockResultCallback)
        
        speechManager.cleanup()
        
        assertEquals(RecognitionStatus.IDLE, speechManager.recognitionStatus.value)
        assertFalse(speechManager.isListening())
    }
    
    @Test
    fun `test engine availability check`() {
        TestConfig.TESTABLE_ENGINES.forEach { engine ->
            val isAvailable = speechManager.isEngineAvailable(engine)
            assertTrue(isAvailable, "Engine $engine should be available for testing")
        }
        
        // Test unavailable engines
        val unavailableEngines = listOf(SpeechEngine.VIVOKA, SpeechEngine.GOOGLE_CLOUD)
        unavailableEngines.forEach { engine ->
            // May be true or false depending on SDK availability, just ensure no crashes
            speechManager.isEngineAvailable(engine)
        }
    }
    
    @Test
    fun `test configuration validation`() {
        val validConfigs = TestConfig.TESTABLE_ENGINES.map { engine ->
            TestConfig.createBasicConfig(engine)
        }
        
        validConfigs.forEach { config ->
            val result = speechManager.validateConfiguration(config)
            assertTrue(result.isValid, "Config for ${config.engine} should be valid")
        }
        
        // Test invalid configurations
        val invalidConfigs = listOf(
            SpeechConfig.default().copy(confidenceThreshold = 2.0f), // > 1.0
            SpeechConfig.default().copy(confidenceThreshold = -0.5f), // < 0.0
            SpeechConfig.default().copy(timeoutDuration = -1000L), // negative
        )
        
        invalidConfigs.forEach { config ->
            val result = speechManager.validateConfiguration(config)
            assertFalse(result.isValid, "Invalid config should be rejected")
            assertTrue(result.errors.isNotEmpty(), "Should have validation errors")
        }
    }
}