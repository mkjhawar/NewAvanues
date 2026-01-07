/**
 * TestUtils.kt - Comprehensive Test Utilities for SpeechRecognition Library
 * 
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2025-08-28
 * 
 * Provides comprehensive testing utilities for speech recognition engines,
 * service operations, performance measurement, and test data generation.
 * 
 * VOS4 Standards Compliance:
 * - Direct implementation pattern
 * - Multi-engine testing support
 * - Performance optimized utilities
 * - Comprehensive error handling
 */

package com.augmentalis.speechrecognition.test

import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Bundle
import android.util.Log
import androidx.test.platform.app.InstrumentationRegistry
import com.augmentalis.speechrecognition.SpeechConfig
import com.augmentalis.voiceos.speech.api.RecognitionResult
import com.augmentalis.voiceos.speech.api.OnSpeechResultListener
import com.augmentalis.voiceos.speech.engines.common.ServiceState
import com.augmentalis.voiceos.speech.engines.common.CommandCache
import com.augmentalis.speechrecognition.mocks.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference
import java.time.Duration
import kotlin.random.Random

/**
 * Comprehensive test utilities for SpeechRecognition library testing
 */
object TestUtils {
    
    private const val TAG = "SpeechRecognition_TestUtils"
    
    // Test timeouts (in milliseconds)
    object Timeouts {
        const val ENGINE_INIT = 5000L
        const val ENGINE_DESTROY = 3000L
        const val RECOGNITION_COMPLETE = 15000L
        const val AUDIO_PROCESSING = 10000L
        const val CACHE_OPERATION = 2000L
        const val PERFORMANCE_TEST = 30000L
        const val ASYNC_OPERATION = 5000L
        const val ENGINE_SWITCH = 3000L
    }
    
    // Speech Engine Test Helpers
    
    /**
     * Multi-engine testing helper with lifecycle management
     */
    class SpeechEngineTestHelper(
        private val context: Context
    ) {
        
        private val availableEngines = mutableMapOf<String, Any>()
        private val engineStates = mutableMapOf<String, EngineState>()
        private val initializationResults = mutableMapOf<String, Boolean>()
        
        enum class EngineState {
            NOT_INITIALIZED,
            INITIALIZING,
            READY,
            PROCESSING,
            ERROR,
            DESTROYED
        }
        
        /**
         * Initialize all available speech engines
         */
        suspend fun initializeAllEngines(timeoutMs: Long = Timeouts.ENGINE_INIT): Map<String, Boolean> {
            val results = mutableMapOf<String, Boolean>()
            
            // Initialize VOSK Engine
            try {
                withTimeout(timeoutMs) {
                    val voskEngine = VoskEngine(context)
                    engineStates["vosk"] = EngineState.INITIALIZING
                    val config = SpeechConfig.vosk()
                    voskEngine.initialize(config)
                    availableEngines["vosk"] = voskEngine
                    engineStates["vosk"] = EngineState.READY
                    results["vosk"] = true
                    initializationResults["vosk"] = true
                }
            } catch (e: Exception) {
                Log.e(TAG, "VOSK engine initialization failed: ${e.message}")
                engineStates["vosk"] = EngineState.ERROR
                results["vosk"] = false
                initializationResults["vosk"] = false
            }
            
            // Skip Vivoka Engine in unit tests (requires SDK dependencies)
            // Uncomment for integration tests with Vivoka SDK available
            /*
            try {
                withTimeout(timeoutMs) {
                    val vivokaEngine = VivokaAndroidEngine(context)
                    engineStates["vivoka"] = EngineState.INITIALIZING
                    val config = SpeechConfig.vivoka()
                    vivokaEngine.initialize(config)
                    availableEngines["vivoka"] = vivokaEngine
                    engineStates["vivoka"] = EngineState.READY
                    results["vivoka"] = true
                    initializationResults["vivoka"] = true
                }
            } catch (e: Exception) {
                Log.e(TAG, "Vivoka engine initialization failed: ${e.message}")
                engineStates["vivoka"] = EngineState.ERROR
                results["vivoka"] = false
                initializationResults["vivoka"] = false
            }
            */
            Log.i(TAG, "Vivoka engine skipped in unit tests (SDK dependency)")
            engineStates["vivoka"] = EngineState.ERROR
            results["vivoka"] = false
            initializationResults["vivoka"] = false
            
            // Initialize Android STT Engine
            try {
                withTimeout(timeoutMs) {
                    val androidSTTEngine = AndroidSTTEngine(context)
                    engineStates["android_stt"] = EngineState.INITIALIZING
                    val config = SpeechConfig.googleSTT()
                    androidSTTEngine.initialize(context, config)
                    availableEngines["android_stt"] = androidSTTEngine
                    engineStates["android_stt"] = EngineState.READY
                    results["android_stt"] = true
                    initializationResults["android_stt"] = true
                }
            } catch (e: Exception) {
                Log.e(TAG, "Android STT engine initialization failed: ${e.message}")
                engineStates["android_stt"] = EngineState.ERROR
                results["android_stt"] = false
                initializationResults["android_stt"] = false
            }
            
            // Google Cloud Engine is disabled - skip initialization
            
            return results
        }
        
        /**
         * Get list of successfully initialized engines
         */
        fun getReadyEngines(): List<String> {
            return engineStates.filter { it.value == EngineState.READY }.keys.toList()
        }
        
        /**
         * Get engine by name if ready
         */
        fun getEngine(engineName: String): Any? {
            return if (engineStates[engineName] == EngineState.READY) {
                availableEngines[engineName]
            } else null
        }
        
        /**
         * Get engine state
         */
        fun getEngineState(engineName: String): EngineState? {
            return engineStates[engineName]
        }
        
        /**
         * Destroy all engines
         */
        suspend fun destroyAllEngines(timeoutMs: Long = Timeouts.ENGINE_DESTROY) {
            availableEngines.forEach { (name, engine) ->
                try {
                    withTimeout(timeoutMs) {
                        engineStates[name] = EngineState.DESTROYED
                        // Call appropriate destroy method based on engine type
                        when (engine) {
                            is VoskEngine -> engine.destroy()
                            // is VivokaEngine -> engine.destroy() // Skip in unit tests
                            is AndroidSTTEngine -> engine.destroy()
                            is WhisperEngine -> engine.destroy()
                            else -> Log.w(TAG, "Unknown engine type: $engine")
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Error destroying engine $name: ${e.message}")
                }
            }
            
            availableEngines.clear()
            engineStates.clear()
            initializationResults.clear()
        }
        
        /**
         * Get initialization statistics
         */
        fun getInitializationStats(): EngineInitializationStats {
            val total = initializationResults.size
            val successful = initializationResults.values.count { it }
            val failed = total - successful
            
            return EngineInitializationStats(
                totalEngines = total,
                successfulInitializations = successful,
                failedInitializations = failed,
                successRate = if (total > 0) successful.toDouble() / total else 0.0,
                readyEngines = getReadyEngines()
            )
        }
        
        data class EngineInitializationStats(
            val totalEngines: Int,
            val successfulInitializations: Int,
            val failedInitializations: Int,
            val successRate: Double,
            val readyEngines: List<String>
        )
    }
    
    // Recognition Result Verification
    
    /**
     * Recognition result verification helper
     */
    class RecognitionResultVerifier {
        
        private val results = mutableListOf<RecognitionResult>()
        private val timestamps = mutableListOf<Long>()
        private val engineResults = mutableMapOf<String, MutableList<RecognitionResult>>()
        
        /**
         * Add result for verification
         */
        fun addResult(result: RecognitionResult) {
            results.add(result)
            timestamps.add(System.currentTimeMillis())
            engineResults.getOrPut(result.engine) { mutableListOf() }.add(result)
            
            Log.d(TAG, "Added result: ${result.text} from ${result.engine} (confidence: ${result.confidence})")
        }
        
        /**
         * Verify recognition accuracy
         */
        fun verifyAccuracy(expectedTexts: List<String>, threshold: Float = 0.8f): AccuracyReport {
            val exactMatches = mutableListOf<Pair<String, RecognitionResult>>()
            val partialMatches = mutableListOf<Pair<String, RecognitionResult>>()
            val noMatches = mutableListOf<String>()
            
            expectedTexts.forEach { expected ->
                val exactMatch = results.find { it.text.equals(expected, ignoreCase = true) }
                if (exactMatch != null) {
                    exactMatches.add(Pair(expected, exactMatch))
                } else {
                    val partialMatch = results.find { result ->
                        val similarity = calculateSimilarity(expected, result.text)
                        similarity >= threshold
                    }
                    if (partialMatch != null) {
                        partialMatches.add(Pair(expected, partialMatch))
                    } else {
                        noMatches.add(expected)
                    }
                }
            }
            
            return AccuracyReport(
                totalExpected = expectedTexts.size,
                exactMatches = exactMatches,
                partialMatches = partialMatches,
                noMatches = noMatches,
                overallAccuracy = (exactMatches.size + partialMatches.size).toDouble() / expectedTexts.size
            )
        }
        
        /**
         * Calculate text similarity (Levenshtein distance based)
         */
        private fun calculateSimilarity(text1: String, text2: String): Float {
            val maxLength = maxOf(text1.length, text2.length)
            if (maxLength == 0) return 1.0f
            
            val distance = levenshteinDistance(text1.lowercase(), text2.lowercase())
            return 1.0f - (distance.toFloat() / maxLength)
        }
        
        /**
         * Calculate Levenshtein distance
         */
        private fun levenshteinDistance(s1: String, s2: String): Int {
            val m = s1.length
            val n = s2.length
            val dp = Array(m + 1) { IntArray(n + 1) }
            
            for (i in 0..m) dp[i][0] = i
            for (j in 0..n) dp[0][j] = j
            
            for (i in 1..m) {
                for (j in 1..n) {
                    if (s1[i - 1] == s2[j - 1]) {
                        dp[i][j] = dp[i - 1][j - 1]
                    } else {
                        dp[i][j] = 1 + minOf(dp[i - 1][j], dp[i][j - 1], dp[i - 1][j - 1])
                    }
                }
            }
            
            return dp[m][n]
        }
        
        /**
         * Get engine-specific statistics
         */
        fun getEngineStats(): Map<String, EngineStats> {
            return engineResults.mapValues { (engine, results) ->
                EngineStats(
                    engine = engine,
                    totalResults = results.size,
                    averageConfidence = results.map { it.confidence }.average(),
                    highConfidenceResults = results.count { it.confidence >= 0.8f },
                    averageLatency = 0.0 // processingTimeMs not available in RecognitionResult
                )
            }
        }
        
        data class AccuracyReport(
            val totalExpected: Int,
            val exactMatches: List<Pair<String, RecognitionResult>>,
            val partialMatches: List<Pair<String, RecognitionResult>>,
            val noMatches: List<String>,
            val overallAccuracy: Double
        ) {
            val exactMatchRate = exactMatches.size.toDouble() / totalExpected
            val partialMatchRate = partialMatches.size.toDouble() / totalExpected
            val noMatchRate = noMatches.size.toDouble() / totalExpected
        }
        
        data class EngineStats(
            val engine: String,
            val totalResults: Int,
            val averageConfidence: Double,
            val highConfidenceResults: Int,
            val averageLatency: Double
        )
        
        /**
         * Reset verifier state
         */
        fun reset() {
            results.clear()
            timestamps.clear()
            engineResults.clear()
        }
    }
    
    // Audio Test Data Generator
    
    /**
     * Audio test data generation for speech recognition testing
     */
    object AudioTestDataGenerator {
        
        private val sampleRate = 16000
        private val channelConfig = AudioFormat.CHANNEL_IN_MONO
        private val audioFormat = AudioFormat.ENCODING_PCM_16BIT
        
        /**
         * Generate silence audio data
         */
        fun generateSilence(durationMs: Int): ByteArray {
            val sampleCount = (sampleRate * durationMs / 1000)
            return ByteArray(sampleCount * 2) // 16-bit = 2 bytes per sample
        }
        
        /**
         * Generate sine wave audio (for testing audio processing)
         */
        fun generateSineWave(frequency: Double, durationMs: Int, amplitude: Double = 0.5): ByteArray {
            val sampleCount = (sampleRate * durationMs / 1000)
            val buffer = ByteArray(sampleCount * 2)
            
            for (i in 0 until sampleCount) {
                val sample = (amplitude * Short.MAX_VALUE * 
                    kotlin.math.sin(2 * kotlin.math.PI * frequency * i / sampleRate)).toInt().toShort()
                
                buffer[i * 2] = (sample.toInt() and 0xFF).toByte()
                buffer[i * 2 + 1] = ((sample.toInt() shr 8) and 0xFF).toByte()
            }
            
            return buffer
        }
        
        /**
         * Generate white noise audio
         */
        fun generateWhiteNoise(durationMs: Int, amplitude: Double = 0.3): ByteArray {
            val sampleCount = (sampleRate * durationMs / 1000)
            val buffer = ByteArray(sampleCount * 2)
            
            for (i in 0 until sampleCount) {
                val sample = (amplitude * Short.MAX_VALUE * (Random.nextDouble() * 2 - 1)).toInt().toShort()
                
                buffer[i * 2] = (sample.toInt() and 0xFF).toByte()
                buffer[i * 2 + 1] = ((sample.toInt() shr 8) and 0xFF).toByte()
            }
            
            return buffer
        }
        
        /**
         * Save audio data to file for debugging
         */
        fun saveAudioToFile(audioData: ByteArray, filename: String, context: Context): File {
            val file = File(context.cacheDir, filename)
            file.writeBytes(audioData)
            return file
        }
        
        /**
         * Create AudioRecord configuration for testing
         */
        fun createTestAudioRecord(): AudioRecord? {
            val bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)
            return if (bufferSize > 0) {
                AudioRecord(MediaRecorder.AudioSource.MIC, sampleRate, channelConfig, audioFormat, bufferSize)
            } else null
        }
        
        /**
         * Generate test audio parameters
         */
        fun generateTestAudioParams(): AudioTestParams {
            return AudioTestParams(
                sampleRate = sampleRate,
                channels = 1,
                bitsPerSample = 16,
                durationMs = Random.nextInt(1000, 5000),
                frequency = Random.nextDouble(200.0, 2000.0),
                amplitude = Random.nextDouble(0.1, 0.8)
            )
        }
        
        data class AudioTestParams(
            val sampleRate: Int,
            val channels: Int,
            val bitsPerSample: Int,
            val durationMs: Int,
            val frequency: Double,
            val amplitude: Double
        )
    }
    
    // Service State Testing
    
    /**
     * Service state testing utilities
     */
    class ServiceStateTestHelper {
        
        private val stateChanges = mutableListOf<StateChange>()
        private val currentStates = mutableMapOf<String, ServiceState>()
        
        data class StateChange(
            val serviceName: String,
            val fromState: ServiceState?,
            val toState: ServiceState,
            val timestamp: Long = System.currentTimeMillis()
        )
        
        /**
         * Record state change
         */
        fun recordStateChange(serviceName: String, newState: ServiceState) {
            val oldState = currentStates[serviceName]
            currentStates[serviceName] = newState
            stateChanges.add(StateChange(serviceName, oldState, newState))
            
            Log.d(TAG, "Service $serviceName state: $oldState -> $newState")
        }
        
        /**
         * Get current state of service
         */
        fun getCurrentState(serviceName: String): ServiceState? {
            return currentStates[serviceName]
        }
        
        /**
         * Verify state transitions follow expected pattern
         */
        fun verifyStateTransitions(serviceName: String, expectedTransitions: List<ServiceState>): Boolean {
            val actualTransitions = stateChanges
                .filter { it.serviceName == serviceName }
                .map { it.toState }
            
            return actualTransitions == expectedTransitions
        }
        
        /**
         * Get state transition timing
         */
        fun getTransitionTiming(serviceName: String): List<Long> {
            val serviceChanges = stateChanges.filter { it.serviceName == serviceName }
            return serviceChanges.zipWithNext { a, b ->
                b.timestamp - a.timestamp
            }
        }
        
        /**
         * Reset state tracking
         */
        fun reset() {
            stateChanges.clear()
            currentStates.clear()
        }
    }
    
    // Cache Testing Utilities
    
    /**
     * Command cache testing helper
     */
    class CacheTestHelper(private val commandCache: CommandCache) {
        
        /**
         * Fill cache with test data
         */
        suspend fun populateTestCache(count: Int = 100) {
            repeat(count) { index ->
                val command = "test_command_$index"
                // CommandCache stores strings, not results
                commandCache.setStaticCommands(listOf(command))
            }
        }
        
        /**
         * Verify cache performance
         */
        suspend fun measureCachePerformance(operationCount: Int = 1000): CachePerformanceReport {
            val putTimes = mutableListOf<Long>()
            val getTimes = mutableListOf<Long>()
            val keys = mutableListOf<String>()
            
            // Measure put operations
            repeat(operationCount) { index ->
                val key = "perf_test_$index"
                keys.add(key)
                // CommandCache stores commands as strings
                val startTime = System.nanoTime()
                commandCache.setDynamicCommands(listOf(key))
                val endTime = System.nanoTime()
                putTimes.add((endTime - startTime) / 1_000_000L) // Convert to milliseconds
            }
            
            // Measure get operations
            keys.forEach { key ->
                val startTime = System.nanoTime()
                commandCache.findMatch(key)
                val endTime = System.nanoTime()
                getTimes.add((endTime - startTime) / 1_000_000L)
            }
            
            return CachePerformanceReport(
                putOperations = putTimes,
                getOperations = getTimes,
                averagePutTime = putTimes.average(),
                averageGetTime = getTimes.average(),
                maxPutTime = putTimes.maxOrNull() ?: 0L,
                maxGetTime = getTimes.maxOrNull() ?: 0L
            )
        }
        
        data class CachePerformanceReport(
            val putOperations: List<Long>,
            val getOperations: List<Long>,
            val averagePutTime: Double,
            val averageGetTime: Double,
            val maxPutTime: Long,
            val maxGetTime: Long
        ) {
            val totalOperations = putOperations.size + getOperations.size
            val overallAverageTime = (averagePutTime + averageGetTime) / 2
        }
    }
    
    // Test Configuration Generator
    
    /**
     * Generate test configurations for different scenarios
     */
    object TestConfigurationGenerator {
        
        /**
         * Generate basic speech configuration
         */
        fun generateBasicConfig(): SpeechConfig {
            return SpeechConfig(
                language = "en-US",
                confidenceThreshold = 0.7f,
                timeoutDuration = 10000L
            )
        }
        
        /**
         * Generate performance test configuration
         */
        fun generatePerformanceConfig(): SpeechConfig {
            return SpeechConfig(
                language = "en-US",
                confidenceThreshold = 0.5f,
                timeoutDuration = 5000L
            )
        }
        
        /**
         * Generate multi-engine test configuration
         */
        fun generateMultiEngineConfig(): SpeechConfig {
            return SpeechConfig(
                language = "en-US",
                confidenceThreshold = 0.8f,
                timeoutDuration = 15000L
            )
        }
        
        /**
         * Generate random test configuration
         */
        fun generateRandomConfig(): SpeechConfig {
            val languages = listOf("en-US", "en-GB", "es-ES", "fr-FR", "de-DE")
            
            return SpeechConfig(
                language = languages.random(),
                confidenceThreshold = Random.nextFloat() * 0.5f + 0.5f, // 0.5-1.0
                timeoutDuration = Random.nextLong(3000L, 20000L)
            )
        }
    }
    
    // Helper Methods
    
    /**
     * Create test listener that captures all events
     */
    fun createTestListener(): TestSpeechListener {
        return TestSpeechListener()
    }
    
    class TestSpeechListener : OnSpeechResultListener {
        
        val events = mutableListOf<SpeechEvent>()
        private val eventChannel = Channel<SpeechEvent>(capacity = Channel.UNLIMITED)
        
        sealed class SpeechEvent {
            object Started : SpeechEvent()
            data class PartialResult(val result: RecognitionResult) : SpeechEvent()
            data class FinalResult(val result: RecognitionResult) : SpeechEvent()
            data class Error(val error: String) : SpeechEvent()
            object Stopped : SpeechEvent()
        }
        
        // OnSpeechResultListener implementation
        override fun invoke(result: RecognitionResult) {
            val event = if (result.isPartial) {
                SpeechEvent.PartialResult(result)
            } else {
                SpeechEvent.FinalResult(result)
            }
            events.add(event)
            eventChannel.trySend(event)
        }
        
        /**
         * Wait for specific event with timeout
         */
        suspend fun waitForEvent(expectedEvent: Class<out SpeechEvent>, timeoutMs: Long): SpeechEvent? {
            return withTimeoutOrNull(timeoutMs) {
                while (true) {
                    val event = eventChannel.receive()
                    if (expectedEvent.isInstance(event)) {
                        return@withTimeoutOrNull event
                    }
                }
                @Suppress("UNREACHABLE_CODE")
                null
            }
        }
        
        fun reset() {
            events.clear()
            // Drain existing events
            while (!eventChannel.isEmpty) {
                eventChannel.tryReceive()
            }
        }
    }
    
    /**
     * Get test context
     */
    fun getTestContext(): Context = InstrumentationRegistry.getInstrumentation().targetContext
    
    /**
     * Get instrumentation context
     */
    fun getInstrumentationContext(): Context = InstrumentationRegistry.getInstrumentation().context
    
    /**
     * Log test result with detailed information
     */
    fun logTestResult(testName: String, success: Boolean, details: String = "", metrics: Map<String, Any> = emptyMap()) {
        val status = if (success) "PASSED" else "FAILED"
        val timestamp = System.currentTimeMillis()
        val metricsStr = if (metrics.isNotEmpty()) {
            metrics.entries.joinToString(", ") { "${it.key}: ${it.value}" }
        } else ""
        
        Log.i(TAG, "TEST [$testName] $status at $timestamp - $details ${if (metricsStr.isNotEmpty()) "| Metrics: $metricsStr" else ""}")
    }
    
    /**
     * Create test bundle with common parameters
     */
    fun createTestBundle(): Bundle {
        return Bundle().apply {
            putString("test_session_id", "session_${System.currentTimeMillis()}")
            putBoolean("enable_debug_logging", true)
            putInt("test_timeout_ms", 10000)
            putFloat("test_confidence_threshold", 0.7f)
            putStringArray("test_engines", arrayOf("vosk", "vivoka", "google_stt"))
        }
    }
}