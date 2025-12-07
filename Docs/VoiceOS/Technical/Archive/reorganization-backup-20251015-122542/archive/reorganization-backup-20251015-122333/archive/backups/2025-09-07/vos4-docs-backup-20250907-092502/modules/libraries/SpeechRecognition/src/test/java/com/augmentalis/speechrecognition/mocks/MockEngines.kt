/**
 * MockEngines.kt - Mock implementations of speech engines for testing
 * 
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2025-01-28
 * 
 * Provides mock implementations of speech recognition engines
 * to enable unit testing without requiring actual engine dependencies.
 */
package com.augmentalis.speechrecognition.mocks

import android.content.Context
import android.speech.RecognitionListener
import com.augmentalis.speechrecognition.SpeechConfig
import com.augmentalis.speechrecognition.SpeechMode
import com.augmentalis.voiceos.speech.engines.common.ServiceState
import com.augmentalis.voiceos.speech.engines.common.ServiceState.State
import com.augmentalis.voiceos.speech.api.RecognitionResult
import com.augmentalis.voiceos.speech.api.OnSpeechResultListener
import com.augmentalis.voiceos.speech.api.OnSpeechErrorListener
import kotlinx.coroutines.delay

/**
 * Mock AndroidSTTEngine for testing
 */
class AndroidSTTEngine(private val context: Context) : RecognitionListener {
    
    private var initialized = false
    private var listening = false
    private var currentConfig: SpeechConfig? = null
    private var state = State.NOT_INITIALIZED
    private var resultListener: ((RecognitionResult) -> Unit)? = null
    private var errorListener: ((String, Int) -> Unit)? = null
    private var partialResultListener: ((String) -> Unit)? = null
    
    // Mock performance metrics
    private val performanceMetrics = MockPerformanceMetrics()
    
    suspend fun initialize(_context: Context, config: SpeechConfig): Boolean {
        return try {
            state = State.INITIALIZING
            delay(100) // Simulate initialization delay
            this.currentConfig = config
            initialized = true
            state = State.INITIALIZED
            true
        } catch (e: Exception) {
            state = State.ERROR
            false
        }
    }
    
    fun startListening(_mode: SpeechMode): Boolean {
        return if (initialized && !listening) {
            listening = true
            state = State.LISTENING
            true
        } else false
    }
    
    fun stopListening() {
        listening = false
        state = State.READY
    }
    
    fun isListening(): Boolean = listening
    
    fun getState(): State = state
    
    fun destroy() {
        initialized = false
        listening = false
        state = State.SHUTDOWN
    }
    
    fun getEngineName(): String = "AndroidSTTEngine"
    fun getEngineVersion(): String = "1.0.0"
    fun requiresNetwork(): Boolean = true
    fun getMemoryUsage(): Int = 15
    fun isInDegradedMode(): Boolean = false
    fun supportsMode(_mode: SpeechMode): Boolean = true
    
    fun setResultListener(listener: (RecognitionResult) -> Unit) {
        resultListener = listener
    }
    
    fun setErrorListener(listener: (String, Int) -> Unit) {
        errorListener = listener
    }
    
    fun setPartialResultListener(listener: (String) -> Unit) {
        partialResultListener = listener
    }
    
    fun getSupportedLanguages(): List<String> = listOf(
        "en-US", "en-GB", "fr-FR", "de-DE", "es-ES", "it-IT",
        "ja-JP", "ko-KR", "zh-CN", "pt-BR", "ru-RU", "nl-NL",
        "hi-IN", "ar-SA", "th-TH", "sv-SE", "da-DK", "no-NO", "he-IL"
    )
    
    fun changeMode(_mode: SpeechMode): Boolean = true
    
    fun addLearnedCommand(_spokenText: String, _matchedCommand: String): Boolean = true
    
    fun setLearningEnabled(_enabled: Boolean) {}
    
    fun getLearningStats(): LearningStats = LearningStats(
        totalCommands = 0,
        learnedCommands = 0,
        accuracyRate = 0.0
    )
    
    fun setStaticCommands(_commands: List<String>) {}
    fun setDynamicCommands(_commands: List<String>) {}
    
    fun getPerformanceMetrics(): PerformanceMetrics = performanceMetrics.toPerformanceMetrics()
    
    // RecognitionListener implementation for Android SpeechRecognizer compatibility
    override fun onResults(results: android.os.Bundle?) {
        results?.let { bundle ->
            val matches = bundle.getStringArrayList(android.speech.SpeechRecognizer.RESULTS_RECOGNITION)
            matches?.firstOrNull()?.let { text ->
                val result = RecognitionResult(
                    text = text,
                    confidence = 0.8f,
                    isFinal = true,
                    engine = "AndroidSTTEngine"
                )
                resultListener?.invoke(result)
            }
        }
    }
    
    override fun onPartialResults(partialResults: android.os.Bundle?) {
        partialResults?.let { bundle ->
            val matches = bundle.getStringArrayList(android.speech.SpeechRecognizer.RESULTS_RECOGNITION)
            matches?.firstOrNull()?.let { text ->
                partialResultListener?.invoke(text)
            }
        }
    }
    
    override fun onError(error: Int) {
        val errorMessage = when (error) {
            android.speech.SpeechRecognizer.ERROR_NETWORK -> "Network error"
            android.speech.SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Speech timeout"
            android.speech.SpeechRecognizer.ERROR_NO_MATCH -> "No match found"
            android.speech.SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED -> "Language not supported"
            else -> "Unknown error: $error"
        }
        state = if (error == android.speech.SpeechRecognizer.ERROR_SPEECH_TIMEOUT || 
                      error == android.speech.SpeechRecognizer.ERROR_NO_MATCH) {
            State.READY // Recoverable errors
        } else {
            State.ERROR // Non-recoverable errors
        }
        errorListener?.invoke(errorMessage, error)
    }
    
    override fun onBeginningOfSpeech() {}
    override fun onBufferReceived(buffer: ByteArray?) {}
    override fun onEndOfSpeech() {}
    override fun onEvent(eventType: Int, params: android.os.Bundle?) {}
    override fun onReadyForSpeech(params: android.os.Bundle?) {}
    override fun onRmsChanged(rmsdB: Float) {}
}

/**
 * Mock VoskEngine for testing
 */
class VoskEngine(private val context: Context) {
    
    private var initialized = false
    
    suspend fun initialize(_config: SpeechConfig) {
        delay(100) // Simulate initialization
        initialized = true
    }
    
    fun destroy() {
        initialized = false
    }
    
    fun isInitialized(): Boolean = initialized
}

/**
 * Mock VivokaEngine for testing (always fails in test environment)
 */
class VivokaEngine(private val _context: Context) {
    
    suspend fun initialize(_config: SpeechConfig) {
        throw Exception("Vivoka SDK not available in test environment")
    }
    
    fun destroy() {}
}

/**
 * Mock WhisperEngine for testing
 */
class WhisperEngine(private val _context: Context) {
    
    private var initialized = false
    
    suspend fun initialize(_config: SpeechConfig) {
        delay(100) // Simulate initialization
        initialized = true
    }
    
    fun destroy() {
        initialized = false
    }
    
    fun isInitialized(): Boolean = initialized
}

/**
 * Mock performance metrics for testing
 */
class MockPerformanceMetrics {
    var totalSessions = 0
    var successCount = 0
    var errorCount = 0
    var uptime = System.currentTimeMillis()
    var memoryUsage = 15 * 1024 * 1024L // 15MB in bytes
    var recognitionLatency = 500L
    val engineSpecificMetrics = mutableMapOf<String, Any>()
    
    fun toPerformanceMetrics(): PerformanceMetrics {
        return PerformanceMetrics(
            totalSessions = totalSessions,
            successCount = successCount,
            errorCount = errorCount,
            uptime = System.currentTimeMillis() - uptime,
            memoryUsage = memoryUsage,
            recognitionLatency = recognitionLatency,
            engineSpecificMetrics = engineSpecificMetrics.toMap()
        )
    }
}

/**
 * Performance metrics data class for testing
 */
data class PerformanceMetrics(
    val totalSessions: Int,
    val successCount: Int,
    val errorCount: Int,
    val uptime: Long,
    val memoryUsage: Long,
    val recognitionLatency: Long,
    val engineSpecificMetrics: Map<String, Any>
)

/**
 * Learning statistics data class for testing
 */
data class LearningStats(
    val totalCommands: Int,
    val learnedCommands: Int,
    val accuracyRate: Double
)