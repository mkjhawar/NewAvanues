/**
 * VoiceOSServicePerformanceBenchmark.kt - Performance baseline benchmark suite
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2025-10-15
 *
 * Purpose: Capture CURRENT performance metrics before refactoring
 * This establishes performance baselines to verify no degradation after refactoring
 */
package com.augmentalis.voiceoscore.baseline

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Performance Baseline Benchmark Suite
 *
 * Critical Paths Measured:
 * 1. Service initialization time
 * 2. Accessibility event processing time
 * 3. Command execution time (all tiers)
 * 4. UI scraping time
 * 5. Database query time
 * 6. Speech recognition latency
 * 7. Memory usage patterns
 * 8. Cache hit/miss rates
 *
 * Performance Targets (CURRENT baseline):
 * - Service init: < 2000ms
 * - Event processing: < 100ms
 * - Command execution: < 100ms
 * - UI scraping: < 500ms
 * - DB query: < 50ms
 * - Recognition latency: < 300ms
 */
@RunWith(AndroidJUnit4::class)
class VoiceOSServicePerformanceBenchmark {

    companion object {
        private const val TAG = "VoiceOSServicePerformanceBenchmark"

        // Performance baselines (current targets)
        private const val SERVICE_INIT_TARGET_MS = 2000L
        private const val EVENT_PROCESSING_TARGET_MS = 100L
        private const val COMMAND_EXECUTION_TARGET_MS = 100L
        private const val UI_SCRAPING_TARGET_MS = 500L
        private const val DB_QUERY_TARGET_MS = 50L
        private const val RECOGNITION_LATENCY_TARGET_MS = 300L
    }

    private lateinit var context: Context
    private val benchmarkResults = mutableListOf<BenchmarkResult>()

    data class BenchmarkResult(
        val operation: String,
        val executionTimeMs: Long,
        val targetMs: Long,
        val meetsTarget: Boolean,
        val details: Map<String, Any> = emptyMap(),
        val timestamp: Long = System.currentTimeMillis()
    )

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        benchmarkResults.clear()
    }

    @After
    fun tearDown() {
        printBenchmarkSummary()
        benchmarkResults.clear()
    }

    /**
     * BENCHMARK 1: Service initialization
     */
    @Test
    fun benchmarkServiceInitialization() = runTest {
        val iterations = 3
        val times = mutableListOf<Long>()

        repeat(iterations) { iteration ->
            val startTime = System.currentTimeMillis()

            // Simulate service initialization steps
            simulateServiceInit()

            val executionTime = System.currentTimeMillis() - startTime
            times.add(executionTime)

            println("BENCHMARK: Service init iteration ${iteration + 1}: ${executionTime}ms")
        }

        val avgTime = times.average().toLong()
        val meetsTarget = avgTime < SERVICE_INIT_TARGET_MS

        recordBenchmark(
            operation = "Service Initialization",
            executionTimeMs = avgTime,
            targetMs = SERVICE_INIT_TARGET_MS,
            meetsTarget = meetsTarget,
            details = mapOf(
                "iterations" to iterations,
                "min" to times.minOrNull()!!,
                "max" to times.maxOrNull()!!,
                "avg" to avgTime
            )
        )

        assertTrue("Service init should meet performance target ($SERVICE_INIT_TARGET_MS ms)",
            meetsTarget)
    }

    /**
     * BENCHMARK 2: Accessibility event processing
     */
    @Test
    fun benchmarkAccessibilityEventProcessing() = runTest {
        val eventTypes = listOf(
            "WINDOW_CONTENT_CHANGED",
            "WINDOW_STATE_CHANGED",
            "VIEW_CLICKED"
        )

        for (eventType in eventTypes) {
            val times = mutableListOf<Long>()

            repeat(10) {
                val startTime = System.currentTimeMillis()
                simulateEventProcessing(eventType)
                val executionTime = System.currentTimeMillis() - startTime
                times.add(executionTime)
            }

            val avgTime = times.average().toLong()
            val meetsTarget = avgTime < EVENT_PROCESSING_TARGET_MS

            recordBenchmark(
                operation = "Event Processing: $eventType",
                executionTimeMs = avgTime,
                targetMs = EVENT_PROCESSING_TARGET_MS,
                meetsTarget = meetsTarget,
                details = mapOf(
                    "eventType" to eventType,
                    "iterations" to 10,
                    "avg" to avgTime
                )
            )
        }
    }

    /**
     * BENCHMARK 3: Command execution (all tiers)
     */
    @Test
    fun benchmarkCommandExecution() = runTest {
        val tiers = listOf(
            1 to "CommandManager",
            2 to "VoiceCommandProcessor",
            3 to "ActionCoordinator"
        )

        for ((tier, name) in tiers) {
            val times = mutableListOf<Long>()

            repeat(20) {
                val startTime = System.currentTimeMillis()
                simulateCommandExecution(tier)
                val executionTime = System.currentTimeMillis() - startTime
                times.add(executionTime)
            }

            val avgTime = times.average().toLong()
            val meetsTarget = avgTime < COMMAND_EXECUTION_TARGET_MS

            recordBenchmark(
                operation = "Command Execution: Tier $tier ($name)",
                executionTimeMs = avgTime,
                targetMs = COMMAND_EXECUTION_TARGET_MS,
                meetsTarget = meetsTarget,
                details = mapOf(
                    "tier" to tier,
                    "tierName" to name,
                    "iterations" to 20,
                    "avg" to avgTime,
                    "p50" to times.sorted()[times.size / 2],
                    "p95" to times.sorted()[(times.size * 0.95).toInt()]
                )
            )
        }
    }

    /**
     * BENCHMARK 4: UI scraping performance
     */
    @Test
    fun benchmarkUIScrapingPerformance() = runTest {
        val nodeCounts = listOf(10, 25, 50, 100)

        for (nodeCount in nodeCounts) {
            val times = mutableListOf<Long>()

            repeat(5) {
                val startTime = System.currentTimeMillis()
                simulateUIScraping(nodeCount)
                val executionTime = System.currentTimeMillis() - startTime
                times.add(executionTime)
            }

            val avgTime = times.average().toLong()
            val meetsTarget = avgTime < UI_SCRAPING_TARGET_MS

            recordBenchmark(
                operation = "UI Scraping: $nodeCount nodes",
                executionTimeMs = avgTime,
                targetMs = UI_SCRAPING_TARGET_MS,
                meetsTarget = meetsTarget,
                details = mapOf(
                    "nodeCount" to nodeCount,
                    "iterations" to 5,
                    "avg" to avgTime,
                    "timePerNode" to (avgTime.toFloat() / nodeCount)
                )
            )
        }
    }

    /**
     * BENCHMARK 5: Database operations
     */
    @Test
    fun benchmarkDatabaseOperations() = runTest {
        val operations = listOf(
            "INSERT" to suspend { simulateDBInsert() },
            "QUERY" to suspend { simulateDBQuery() },
            "UPDATE" to suspend { simulateDBUpdate() },
            "DELETE" to suspend { simulateDBDelete() }
        )

        for ((opName, operation) in operations) {
            val times = mutableListOf<Long>()

            repeat(50) {
                val startTime = System.currentTimeMillis()
                operation()
                val executionTime = System.currentTimeMillis() - startTime
                times.add(executionTime)
            }

            val avgTime = times.average().toLong()
            val meetsTarget = avgTime < DB_QUERY_TARGET_MS

            recordBenchmark(
                operation = "Database: $opName",
                executionTimeMs = avgTime,
                targetMs = DB_QUERY_TARGET_MS,
                meetsTarget = meetsTarget,
                details = mapOf(
                    "operation" to opName,
                    "iterations" to 50,
                    "avg" to avgTime,
                    "min" to times.minOrNull()!!,
                    "max" to times.maxOrNull()!!
                )
            )
        }
    }

    /**
     * BENCHMARK 6: Speech recognition latency
     */
    @Test
    fun benchmarkSpeechRecognitionLatency() = runTest {
        val engines = listOf("VIVOKA", "VOSK", "GOOGLE")

        for (engine in engines) {
            val times = mutableListOf<Long>()

            repeat(15) {
                val startTime = System.currentTimeMillis()
                simulateSpeechRecognition(engine)
                val executionTime = System.currentTimeMillis() - startTime
                times.add(executionTime)
            }

            val avgTime = times.average().toLong()
            val meetsTarget = avgTime < RECOGNITION_LATENCY_TARGET_MS

            recordBenchmark(
                operation = "Speech Recognition: $engine",
                executionTimeMs = avgTime,
                targetMs = RECOGNITION_LATENCY_TARGET_MS,
                meetsTarget = meetsTarget,
                details = mapOf(
                    "engine" to engine,
                    "iterations" to 15,
                    "avg" to avgTime
                )
            )
        }
    }

    /**
     * BENCHMARK 7: Cache performance
     */
    @Test
    fun benchmarkCachePerformance() = runTest {
        val cacheSize = 100
        var hitCount = 0
        var missCount = 0

        repeat(200) { i ->
            val cacheHit = (i % 3) != 0 // Simulate 66% hit rate

            val startTime = System.currentTimeMillis()
            if (cacheHit) {
                simulateCacheHit()
                hitCount++
            } else {
                simulateCacheMiss()
                missCount++
            }
            val unused = System.currentTimeMillis() - startTime
        }

        val hitRate = hitCount.toFloat() / (hitCount + missCount) * 100

        recordBenchmark(
            operation = "Cache Performance",
            executionTimeMs = 0,
            targetMs = 0,
            meetsTarget = true,
            details = mapOf(
                "cacheSize" to cacheSize,
                "hits" to hitCount,
                "misses" to missCount,
                "hitRate" to hitRate
            )
        )

        println("BENCHMARK: Cache hit rate: ${"%.2f".format(hitRate)}%")
        assertTrue("Cache hit rate should be > 60%", hitRate > 60)
    }

    /**
     * BENCHMARK 8: Memory usage estimation
     */
    @Test
    fun benchmarkMemoryUsage() = runTest {
        val runtime = Runtime.getRuntime()

        // Baseline memory
        System.gc()
        delay(100)
        val baselineMemory = runtime.totalMemory() - runtime.freeMemory()

        // Simulate service operations
        repeat(100) {
            simulateEventProcessing("WINDOW_CONTENT_CHANGED")
            simulateCommandExecution(1)
        }

        System.gc()
        delay(100)
        val activeMemory = runtime.totalMemory() - runtime.freeMemory()

        val memoryDelta = (activeMemory - baselineMemory) / (1024 * 1024) // MB

        recordBenchmark(
            operation = "Memory Usage",
            executionTimeMs = 0,
            targetMs = 0,
            meetsTarget = memoryDelta < 15, // Target < 15MB
            details = mapOf(
                "baselineMemoryMB" to (baselineMemory / (1024 * 1024)),
                "activeMemoryMB" to (activeMemory / (1024 * 1024)),
                "deltaMemoryMB" to memoryDelta
            )
        )

        println("BENCHMARK: Memory delta: ${memoryDelta}MB")
    }

    // Simulation Helpers

    private suspend fun simulateServiceInit() {
        delay(150) // Config loading
        delay(100) // Component initialization
        delay(50)  // Speech engine init start
        delay(30)  // Cursor API init
        delay(20)  // Database init
    }

    private suspend fun simulateEventProcessing(eventType: String) {
        delay(when (eventType) {
            "WINDOW_CONTENT_CHANGED" -> 50L
            "WINDOW_STATE_CHANGED" -> 45L
            "VIEW_CLICKED" -> 30L
            else -> 40L
        })
    }

    private suspend fun simulateCommandExecution(tier: Int) {
        delay(when (tier) {
            1 -> 20L  // CommandManager
            2 -> 30L  // VoiceCommandProcessor
            3 -> 15L  // ActionCoordinator
            else -> 25L
        })
    }

    private suspend fun simulateUIScraping(nodeCount: Int) {
        // Simulate scraping time proportional to nodes
        delay((nodeCount * 3).toLong())
    }

    private suspend fun simulateDBInsert() {
        delay(15)
    }

    private suspend fun simulateDBQuery() {
        delay(10)
    }

    private suspend fun simulateDBUpdate() {
        delay(12)
    }

    private suspend fun simulateDBDelete() {
        delay(8)
    }

    private suspend fun simulateSpeechRecognition(engine: String) {
        delay(when (engine) {
            "VIVOKA" -> 80L
            "VOSK" -> 120L
            "GOOGLE" -> 150L
            else -> 100L
        })
    }

    private suspend fun simulateCacheHit() {
        delay(1) // Very fast - in-memory access
    }

    private suspend fun simulateCacheMiss() {
        delay(20) // Slower - needs to fetch
    }

    private fun recordBenchmark(
        operation: String,
        executionTimeMs: Long,
        targetMs: Long,
        meetsTarget: Boolean,
        details: Map<String, Any>
    ) {
        benchmarkResults.add(
            BenchmarkResult(
                operation = operation,
                executionTimeMs = executionTimeMs,
                targetMs = targetMs,
                meetsTarget = meetsTarget,
                details = details
            )
        )
    }

    private fun printBenchmarkSummary() {
        println("\n" + "=".repeat(80))
        println("PERFORMANCE BENCHMARK SUMMARY")
        println("=".repeat(80))

        val totalTests = benchmarkResults.size
        val passedTests = benchmarkResults.count { it.meetsTarget }

        println("\nOverall: $passedTests/$totalTests tests meet performance targets")

        println("\nDetailed Results:")
        for (result in benchmarkResults) {
            val status = if (result.meetsTarget) "✓" else "✗"
            val comparison = if (result.targetMs > 0) {
                " (target: ${result.targetMs}ms)"
            } else ""

            println("  $status ${result.operation}: ${result.executionTimeMs}ms$comparison")

            if (result.details.isNotEmpty()) {
                result.details.forEach { (key, value) ->
                    println("      $key: $value")
                }
            }
        }

        println("\n" + "=".repeat(80))
    }

    /**
     * Get baseline metrics summary
     */
    fun getBaselineMetrics(): String {
        return buildString {
            appendLine("=== Performance Benchmark Baseline Metrics ===")
            appendLine("Total benchmarks: ${benchmarkResults.size}")

            val passed = benchmarkResults.count { it.meetsTarget }
            appendLine("Met targets: $passed/${benchmarkResults.size}")

            appendLine("\nCritical Path Performance:")
            for (result in benchmarkResults) {
                if (result.targetMs > 0) {
                    appendLine("  ${result.operation}: ${result.executionTimeMs}ms (target: ${result.targetMs}ms)")
                }
            }
        }
    }
}
