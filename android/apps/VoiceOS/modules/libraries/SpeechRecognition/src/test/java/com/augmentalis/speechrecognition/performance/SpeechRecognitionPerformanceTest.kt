/**
 * SpeechRecognitionPerformanceTest.kt - Performance and stress testing
 * 
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2025-01-28
 * 
 * Comprehensive performance testing for speech recognition under various load conditions
 */
package com.augmentalis.speechrecognition.performance

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.augmentalis.speechrecognition.*
import com.augmentalis.voiceos.speech.api.RecognitionResult
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import kotlinx.coroutines.Dispatchers
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.*
import kotlin.system.measureTimeMillis
import java.util.concurrent.atomic.AtomicInteger
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

@RunWith(AndroidJUnit4::class)
class SpeechRecognitionPerformanceTest {
    
    private lateinit var context: Context
    private lateinit var speechManager: SpeechRecognitionManager
    
    companion object {
        private const val PERFORMANCE_TIMEOUT_MS = 30000L
        private const val STRESS_TEST_ITERATIONS = 1000
        private const val CONCURRENT_REQUESTS = 50
        private const val MEMORY_THRESHOLD_MB = 200L
    }
    
    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        speechManager = SpeechRecognitionManager(context)
    }
    
    @Test
    fun `performance test recognition latency under normal load`() = runTest(timeout = PERFORMANCE_TIMEOUT_MS.milliseconds) {
        val config = SpeechConfig.vosk().copy(timeoutDuration = 5000L)
        speechManager.initialize(config)
        
        val latencies = mutableListOf<Long>()
        
        repeat(100) { _ ->
            val latency = measureTimeMillis {
                val callback = MockRecognitionCallback()
                speechManager.startListening(callback)
                delay(50) // Simulate recognition time
                speechManager.stopListening()
            }
            latencies.add(latency)
            
            // Brief pause between tests
            delay(10)
        }
        
        val averageLatency = latencies.average()
        val maxLatency = latencies.max()
        val p95Latency = latencies.sorted()[95]
        
        // Performance assertions
        assertTrue(averageLatency < 200.0, "Average latency should be < 200ms, was ${averageLatency}ms")
        assertTrue(maxLatency < 500L, "Max latency should be < 500ms, was ${maxLatency}ms")
        assertTrue(p95Latency < 300L, "95th percentile should be < 300ms, was ${p95Latency}ms")
        
        println("Performance Results:")
        println("Average Latency: ${averageLatency}ms")
        println("Max Latency: ${maxLatency}ms")
        println("95th Percentile: ${p95Latency}ms")
    }
    
    @Test
    fun `stress test concurrent recognition requests`() = runTest(timeout = PERFORMANCE_TIMEOUT_MS.milliseconds) {
        val config = SpeechConfig.vosk()
        speechManager.initialize(config)
        
        val successCount = AtomicInteger(0)
        val errorCount = AtomicInteger(0)
        val startTime = System.currentTimeMillis()
        
        // Launch concurrent recognition attempts
        val jobs = (1..CONCURRENT_REQUESTS).map { _ ->
            async {
                try {
                    val callback = MockRecognitionCallback()
                    val result = speechManager.startListening(callback)
                    
                    if (result) {
                        delay(100) // Simulate recognition processing
                        speechManager.stopListening()
                        successCount.incrementAndGet()
                    } else {
                        errorCount.incrementAndGet()
                    }
                } catch (e: Exception) {
                    errorCount.incrementAndGet()
                }
            }
        }
        
        jobs.awaitAll()
        val totalTime = System.currentTimeMillis() - startTime
        
        val totalRequests = successCount.get() + errorCount.get()
        val successRate = successCount.get().toFloat() / totalRequests
        val throughput = totalRequests.toFloat() / (totalTime / 1000f)
        
        // Stress test assertions
        assertTrue(successRate >= 0.7f, "Success rate should be >= 70% under stress, was ${successRate * 100}%")
        assertTrue(throughput >= 5.0f, "Throughput should be >= 5 req/sec, was ${throughput}")
        assertTrue(totalTime < 15000L, "Total time should be < 15s, was ${totalTime}ms")
        
        println("Stress Test Results:")
        println("Success Rate: ${successRate * 100}%")
        println("Throughput: ${throughput} req/sec")
        println("Total Time: ${totalTime}ms")
    }
    
    @Test
    fun `memory leak test during extended operation`() = runTest(timeout = PERFORMANCE_TIMEOUT_MS.milliseconds) {
        val config = SpeechConfig.vosk()
        speechManager.initialize(config)
        
        val runtime = Runtime.getRuntime()
        val initialMemory = (runtime.totalMemory() - runtime.freeMemory()) / 1024 / 1024
        
        repeat(STRESS_TEST_ITERATIONS) { iteration ->
            val callback = MockRecognitionCallback()
            speechManager.startListening(callback)
            delay(5) // Brief recognition simulation
            speechManager.stopListening()
            
            // Force garbage collection every 100 iterations
            if (iteration % 100 == 0) {
                runtime.gc()
                delay(50)
                
                val currentMemory = (runtime.totalMemory() - runtime.freeMemory()) / 1024 / 1024
                val memoryGrowth = currentMemory - initialMemory
                
                assertTrue(
                    memoryGrowth < MEMORY_THRESHOLD_MB,
                    "Memory growth should be < ${MEMORY_THRESHOLD_MB}MB after $iteration iterations, was ${memoryGrowth}MB"
                )
            }
        }
        
        // Final memory check
        runtime.gc()
        delay(100)
        val finalMemory = (runtime.totalMemory() - runtime.freeMemory()) / 1024 / 1024
        val totalMemoryGrowth = finalMemory - initialMemory
        
        assertTrue(
            totalMemoryGrowth < MEMORY_THRESHOLD_MB,
            "Total memory growth should be < ${MEMORY_THRESHOLD_MB}MB, was ${totalMemoryGrowth}MB"
        )
        
        println("Memory Test Results:")
        println("Initial Memory: ${initialMemory}MB")
        println("Final Memory: ${finalMemory}MB")
        println("Memory Growth: ${totalMemoryGrowth}MB")
    }
    
    @Test
    fun `load test with realistic voice command patterns`() = runTest(timeout = PERFORMANCE_TIMEOUT_MS.milliseconds) {
        val config = SpeechConfig.vosk()
        speechManager.initialize(config)
        
        val commonCommands = listOf(
            "go back", "scroll down", "open settings", "volume up",
            "click button", "select first", "home screen", "recent apps"
        )
        
        val metrics = mutableListOf<RecognitionMetric>()
        
        repeat(500) { _ ->
            val command = commonCommands.random()
            val startTime = System.nanoTime()
            
            try {
                val callback = MockRecognitionCallback()
                val success = speechManager.startListening(callback)
                
                if (success) {
                    // Simulate processing time based on command complexity
                    val processingTime = when (command.split(" ").size) {
                        1 -> (50..100).random()
                        2 -> (100..200).random()
                        else -> (200..400).random()
                    }
                    delay(processingTime.toLong())
                    
                    speechManager.stopListening()
                }
                
                val endTime = System.nanoTime()
                val latencyMs = (endTime - startTime) / 1_000_000
                
                metrics.add(RecognitionMetric(command, latencyMs, success))
                
            } catch (e: Exception) {
                val endTime = System.nanoTime()
                val latencyMs = (endTime - startTime) / 1_000_000
                metrics.add(RecognitionMetric(command, latencyMs, false))
            }
            
            // Realistic pause between commands
            delay((100..500).random().toLong())
        }
        
        // Analyze results
        val successfulMetrics = metrics.filter { it.success }
        val averageLatency = successfulMetrics.map { it.latencyMs }.average()
        val successRate = successfulMetrics.size.toFloat() / metrics.size
        
        val commandStats = commonCommands.associateWith { cmd ->
            val cmdMetrics = successfulMetrics.filter { it.command == cmd }
            if (cmdMetrics.isNotEmpty()) {
                cmdMetrics.map { it.latencyMs }.average()
            } else 0.0
        }
        
        // Load test assertions
        assertTrue(successRate >= 0.9f, "Success rate should be >= 90%, was ${successRate * 100}%")
        assertTrue(averageLatency < 300.0, "Average latency should be < 300ms, was ${averageLatency}ms")
        
        // Verify no command type has excessive latency
        commandStats.forEach { (command, avgLatency) ->
            assertTrue(
                avgLatency < 500.0,
                "Command '$command' average latency should be < 500ms, was ${avgLatency}ms"
            )
        }
        
        println("Load Test Results:")
        println("Success Rate: ${successRate * 100}%")
        println("Average Latency: ${averageLatency}ms")
        commandStats.forEach { (cmd, latency) ->
            println("'$cmd': ${latency}ms")
        }
    }
    
    data class RecognitionMetric(
        val command: String,
        val latencyMs: Long,
        val success: Boolean
    )
    
    private class MockRecognitionCallback : SpeechRecognitionCallback {
        override fun onResult(result: RecognitionResult) {}
        override fun onError(error: String) {}
        override fun onPartialResult(partialText: String) {}
    }
}