/**
 * CompilationTest.kt - Test to verify our fixes work
 * 
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2025-01-28
 * 
 * Tests compilation of all the classes we've created or fixed
 */
package com.augmentalis.speechrecognition

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.augmentalis.speechrecognition.mocks.AndroidSTTEngine
import com.augmentalis.speechrecognition.mocks.VoskEngine
import com.augmentalis.speechrecognition.mocks.WhisperEngine
import com.augmentalis.speechrecognition.test.TestConfig
import com.augmentalis.voiceos.speech.api.RecognitionResult
import com.augmentalis.voiceos.speech.engines.common.CommandCache
import com.vivoka.vsdk.Vsdk
import com.vivoka.vsdk.AsrEngine
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.*

@RunWith(AndroidJUnit4::class)
class CompilationTest {
    
    private lateinit var context: Context
    
    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
    }
    
    @Test
    fun `test all our mock classes compile correctly`() {
        // Test RecognitionStatus enum
        val status = RecognitionStatus.IDLE
        assertEquals(RecognitionStatus.IDLE, status)
        
        // Test SpeechRecognitionManager
        val manager = SpeechRecognitionManager(context)
        assertNotNull(manager)
        
        // Test that we can create mock engines
        val androidEngine = AndroidSTTEngine(context)
        val voskEngine = VoskEngine(context)
        val whisperEngine = WhisperEngine(context)
        
        assertNotNull(androidEngine)
        assertNotNull(voskEngine)
        assertNotNull(whisperEngine)
        
        // Test CommandCache alias
        val cache = CommandCache()
        assertNotNull(cache)
        
        // Test Vivoka mocks (should not throw exceptions)
        val vivokaAvailable = Vsdk.isAvailable()
        val asrEngine = AsrEngine()
        
        assertFalse(vivokaAvailable) // Should be false in test environment
        assertNotNull(asrEngine)
    }
    
    @Test
    fun `test TestConfig works`() {
        val config = TestConfig.createBasicConfig(SpeechEngine.VOSK)
        assertNotNull(config)
        assertEquals(SpeechEngine.VOSK, config.engine)
        
        assertTrue(TestConfig.isTestableEngine(SpeechEngine.VOSK))
        assertTrue(TestConfig.isTestableEngine(SpeechEngine.ANDROID_STT))
        assertFalse(TestConfig.isTestableEngine(SpeechEngine.VIVOKA))
    }
    
    @Test
    fun `test speech recognition manager integration`() = runTest {
        val manager = SpeechRecognitionManager(context)
        val config = TestConfig.createBasicConfig(SpeechEngine.VOSK)
        
        // Test initialization
        val initialized = manager.initialize(config)
        assertTrue(initialized)
        
        // Test callback functionality
        val callback = object : SpeechRecognitionCallback {
            var resultReceived = false
            var errorReceived = false
            var partialReceived = false
            
            override fun onResult(result: RecognitionResult) {
                resultReceived = true
            }
            
            override fun onPartialResult(partialText: String) {
                partialReceived = true
            }
            
            override fun onError(error: String) {
                errorReceived = true
            }
        }
        
        // Test starting listening
        val listening = manager.startListening(callback)
        assertTrue(listening)
        assertTrue(manager.isListening())
        
        // Test stopping listening
        val stopped = manager.stopListening()
        assertTrue(stopped)
        assertFalse(manager.isListening())
        
        // Test cleanup
        manager.cleanup()
    }
}