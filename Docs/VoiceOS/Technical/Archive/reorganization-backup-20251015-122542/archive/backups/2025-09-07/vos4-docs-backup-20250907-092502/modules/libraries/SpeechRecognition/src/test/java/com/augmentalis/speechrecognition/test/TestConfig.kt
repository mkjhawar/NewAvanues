/**
 * TestConfig.kt - Test configuration for unit testing
 * 
 * Provides test-safe configurations that avoid dependencies on external SDKs
 */
package com.augmentalis.speechrecognition.test

import com.augmentalis.speechrecognition.SpeechConfig
import com.augmentalis.speechrecognition.SpeechEngine
import com.augmentalis.speechrecognition.SpeechMode

/**
 * Test configuration that avoids problematic dependencies
 */
object TestConfig {
    
    /**
     * List of engines that can be safely tested without external SDK dependencies
     */
    val TESTABLE_ENGINES = listOf(
        SpeechEngine.VOSK,
        SpeechEngine.ANDROID_STT,
        SpeechEngine.WHISPER
        // Exclude VIVOKA and GOOGLE_CLOUD due to SDK dependencies
    )
    
    /**
     * Create a basic test configuration
     */
    fun createBasicConfig(engine: SpeechEngine = SpeechEngine.VOSK): SpeechConfig {
        return when (engine) {
            SpeechEngine.VOSK -> SpeechConfig.vosk()
            SpeechEngine.ANDROID_STT -> SpeechConfig.googleSTT()
            SpeechEngine.GOOGLE_CLOUD -> SpeechConfig.googleCloud("test-key")
            else -> SpeechConfig.default()
        }.copy(
            timeoutDuration = 1000L, // Short timeout for tests
            confidenceThreshold = 0.5f
        )
    }
    
    /**
     * Check if an engine can be tested
     */
    fun isTestableEngine(engine: SpeechEngine): Boolean {
        return engine in TESTABLE_ENGINES
    }
}
