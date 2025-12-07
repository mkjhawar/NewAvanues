/**
 * PerformanceTestTemplate.kt - Advanced performance testing template
 * 
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: {{DATE}}
 * 
 * Comprehensive performance testing including load, stress, memory, and benchmark tests
 */
package {{PACKAGE_NAME}}

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import kotlin.random.Random
import kotlin.system.measureNanoTime
import kotlin.system.measureTimeMillis
import kotlin.test.*

@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalCoroutinesApi::class)
class {{CLASS_NAME}}PerformanceTest {
    
    private lateinit var context: Context
    private lateinit var subject: {{CLASS_NAME}}
    private val testDispatcher = UnconfinedTestDispatcher()
    
    companion object {
        // Performance thresholds
        private const val MAX_LATENCY_MS = 100L
        private const val MAX_MEMORY_MB = 50L
        private const val MIN_THROUGHPUT_OPS_PER_SEC = 100
        private const val MAX_CPU_USAGE_PERCENT = 80
        
        // Test parameters
        private const val LOAD_TEST_ITERATIONS = 1000
        private const val STRESS_TEST_DURATION_MS = 30000L
        private const val CONCURRENT_USERS = 50
        private const val MEMORY_TEST_ITERATIONS = 10000
    }
    
    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        context = ApplicationProvider.getApplicationContext()
        subject = {{CLASS_NAME}}({{CONSTRUCTOR_PARAMS}})
    }
    
    // ========== Latency Tests ==========
    
    @Test
    fun `performance test operation latency under normal load`() = runTest {
        val latencies = mutableListOf<Long>()
        
        // Warmup
        repeat(10) {
            subject.{{OPERATION}}({{PARAMS}})
        }
        
        // Measure latencies
        repeat(100) {
            val latency = measureTimeMillis {
                subject.{{OPERATION}}({{PARAMS}})
            }
            latencies.add(latency)
        }
        
        val stats = calculateStatistics(latencies)
        
        // Assertions
        assertTrue(
            stats.average < MAX_LATENCY_MS,
            "Average latency ${stats.average}ms exceeds threshold ${MAX_LATENCY_MS}ms"
        )
        assertTrue(
            stats.p95 < MAX_LATENCY_MS * 2,
            "95th percentile ${stats.p95}ms exceeds threshold ${MAX_LATENCY_MS * 2}ms"
        )
        assertTrue(
            stats.max < MAX_LATENCY_MS * 5,
            "Max latency ${stats.max}ms exceeds threshold ${MAX_LATENCY_MS * 5}ms"
        )
        
        printLatencyReport(stats)
    }
    
    @Test
    fun `performance test cold start latency`() {
        // Clear any caches
        subject.clearCache()
        System.gc()
        
        val coldStartTime = measureTimeMillis {
            subject.initialize()
            subject.{{FIRST_OPERATION}}({{PARAMS}})
        }
        
        assertTrue(
            coldStartTime < 1000,
            "Cold start time ${coldStartTime}ms exceeds 1s threshold"
        )
    }
    
    // ========== Throughput Tests ==========
    
    @Test
    fun `performance test throughput under sustained load`() = runTest {
        val startTime = System.currentTimeMillis()
        val operationCount = AtomicInteger(0)
        
        // Run operations for fixed duration
        val endTime = startTime + 10000 // 10 seconds
        while (System.currentTimeMillis() < endTime) {
            subject.{{OPERATION}}({{PARAMS}})
            operationCount.incrementAndGet()
        }
        
        val duration = System.currentTimeMillis() - startTime
        val throughput = (operationCount.get() * 1000.0) / duration
        
        assertTrue(
            throughput >= MIN_THROUGHPUT_OPS_PER_SEC,
            "Throughput ${throughput.toInt()} ops/sec below minimum $MIN_THROUGHPUT_OPS_PER_SEC"
        )
        
        println("Throughput: ${throughput.toInt()} operations/second")
    }
    
    @Test
    fun `performance test concurrent throughput`() = runTest {
        val totalOperations = AtomicInteger(0)
        val startTime = System.currentTimeMillis()
        
        val jobs = List(CONCURRENT_USERS) { userId ->
            async {
                repeat(100) {
                    subject.{{CONCURRENT_OPERATION}}(userId, {{PARAMS}})
                    totalOperations.incrementAndGet()
                }
            }
        }
        
        jobs.awaitAll()
        
        val duration = System.currentTimeMillis() - startTime
        val throughput = (totalOperations.get() * 1000.0) / duration
        
        assertTrue(
            throughput >= MIN_THROUGHPUT_OPS_PER_SEC * CONCURRENT_USERS / 10,
            "Concurrent throughput ${throughput.toInt()} ops/sec too low"
        )
        
        println("Concurrent throughput: ${throughput.toInt()} ops/sec with $CONCURRENT_USERS users")
    }
    
    // ========== Memory Tests ==========
    
    @Test
    fun `performance test memory usage under load`() = runTest {
        val runtime = Runtime.getRuntime()
        
        // Force GC and get baseline
        System.gc()
        delay(100)
        val baselineMemory = getMemoryUsageMB()
        
        // Perform operations
        repeat(MEMORY_TEST_ITERATIONS) { iteration ->
            subject.{{MEMORY_INTENSIVE_OPERATION}}({{PARAMS}})
            
            if (iteration % 1000 == 0) {
                val currentMemory = getMemoryUsageMB()
                val memoryGrowth = currentMemory - baselineMemory
                
                assertTrue(
                    memoryGrowth < MAX_MEMORY_MB,
                    "Memory growth ${memoryGrowth}MB exceeds limit ${MAX_MEMORY_MB}MB at iteration $iteration"
                )
            }
        }
        
        // Final memory check after GC
        System.gc()
        delay(100)
        val finalMemory = getMemoryUsageMB()
        val totalGrowth = finalMemory - baselineMemory
        
        assertTrue(
            totalGrowth < MAX_MEMORY_MB / 2,
            "Final memory growth ${totalGrowth}MB indicates memory leak"
        )
        
        println("Memory usage - Baseline: ${baselineMemory}MB, Final: ${finalMemory}MB, Growth: ${totalGrowth}MB")
    }
    
    @Test
    fun `performance test memory leak detection`() = runTest {
        val memorySnapshots = mutableListOf<Long>()
        
        repeat(50) { iteration ->
            // Perform operations that might leak
            repeat(100) {
                val resource = subject.allocateResource()
                subject.useResource(resource)
                subject.releaseResource(resource)
            }
            
            // Force GC and measure
            System.gc()
            delay(50)
            memorySnapshots.add(getMemoryUsageBytes())
            
            // Check for consistent growth (potential leak)
            if (iteration > 10) {
                val recentGrowth = memorySnapshots.takeLast(10)
                val isGrowingConsistently = recentGrowth.zipWithNext().all { (prev, curr) -> 
                    curr > prev 
                }
                
                assertFalse(
                    isGrowingConsistently,
                    "Memory consistently growing, potential leak detected"
                )
            }
        }
    }
    
    // ========== CPU Tests ==========
    
    @Test
    fun `performance test CPU usage`() = runTest {
        val cpuSnapshots = mutableListOf<Double>()
        
        val monitorJob = async {
            repeat(100) {
                cpuSnapshots.add(getCpuUsage())
                delay(100)
            }
        }
        
        // Run CPU-intensive operations
        repeat(1000) {
            subject.{{CPU_INTENSIVE_OPERATION}}({{PARAMS}})
        }
        
        monitorJob.await()
        
        val averageCpu = cpuSnapshots.average()
        val maxCpu = cpuSnapshots.maxOrNull() ?: 0.0
        
        assertTrue(
            averageCpu < MAX_CPU_USAGE_PERCENT,
            "Average CPU usage ${averageCpu.toInt()}% exceeds limit $MAX_CPU_USAGE_PERCENT%"
        )
        assertTrue(
            maxCpu < 100,
            "Max CPU usage ${maxCpu.toInt()}% indicates CPU saturation"
        )
        
        println("CPU usage - Average: ${averageCpu.toInt()}%, Max: ${maxCpu.toInt()}%")
    }
    
    // ========== Stress Tests ==========
    
    @Test
    fun `stress test sustained high load`() = runTest {
        val errors = AtomicInteger(0)
        val successes = AtomicInteger(0)
        val startTime = System.currentTimeMillis()
        
        val stressJobs = List(CONCURRENT_USERS * 2) {
            async {
                while (System.currentTimeMillis() - startTime < STRESS_TEST_DURATION_MS) {
                    try {
                        subject.{{STRESS_OPERATION}}({{RANDOM_PARAMS}})
                        successes.incrementAndGet()
                    } catch (e: Exception) {
                        errors.incrementAndGet()
                    }
                    delay(Random.nextLong(1, 10))
                }
            }
        }
        
        stressJobs.awaitAll()
        
        val totalOperations = successes.get() + errors.get()
        val errorRate = errors.get().toDouble() / totalOperations
        
        assertTrue(
            errorRate < 0.01,
            "Error rate ${(errorRate * 100).format(2)}% exceeds 1% threshold"
        )
        
        println("Stress test completed - Operations: $totalOperations, Errors: ${errors.get()}, Error rate: ${(errorRate * 100).format(2)}%")
    }
    
    @Test
    fun `stress test resource exhaustion recovery`() = runTest {
        // Exhaust resources
        val resources = mutableListOf<Any>()
        try {
            repeat(10000) {
                resources.add(subject.allocateResource())
            }
            fail("Should have thrown resource exhaustion exception")
        } catch (e: Exception) {
            // Expected - resources exhausted
        }
        
        // Release resources
        resources.forEach { 
            subject.releaseResource(it) 
        }
        resources.clear()
        
        // Verify recovery
        delay(1000)
        
        val recoveryTest = try {
            subject.{{OPERATION}}({{PARAMS}})
            true
        } catch (e: Exception) {
            false
        }
        
        assertTrue(recoveryTest, "System should recover after resource exhaustion")
    }
    
    // ========== Scalability Tests ==========
    
    @Test
    fun `scalability test linear performance scaling`() = runTest {
        val loadLevels = listOf(10, 50, 100, 500, 1000)
        val performanceMetrics = mutableMapOf<Int, Double>()
        
        loadLevels.forEach { load ->
            val time = measureTimeMillis {
                repeat(load) {
                    subject.{{SCALABLE_OPERATION}}({{PARAMS}})
                }
            }
            performanceMetrics[load] = time.toDouble() / load
        }
        
        // Check for linear scaling (time per operation should remain constant)
        val avgTimePerOp = performanceMetrics.values.average()
        performanceMetrics.forEach { (load, timePerOp) ->
            val deviation = Math.abs(timePerOp - avgTimePerOp) / avgTimePerOp
            assertTrue(
                deviation < 0.5,
                "Non-linear scaling at load $load: ${(deviation * 100).toInt()}% deviation"
            )
        }
        
        println("Scalability metrics:")
        performanceMetrics.forEach { (load, timePerOp) ->
            println("  Load $load: ${timePerOp.format(2)}ms per operation")
        }
    }
    
    // ========== Benchmark Tests ==========
    
    @Test
    fun `benchmark critical operations`() {
        val operations = mapOf(
            "{{OPERATION_1}}" to { subject.{{OPERATION_1}}({{PARAMS_1}}) },
            "{{OPERATION_2}}" to { subject.{{OPERATION_2}}({{PARAMS_2}}) },
            "{{OPERATION_3}}" to { subject.{{OPERATION_3}}({{PARAMS_3}}) }
        )
        
        val benchmarkResults = mutableMapOf<String, BenchmarkResult>()
        
        operations.forEach { (name, operation) ->
            // Warmup
            repeat(100) { operation() }
            
            // Benchmark
            val times = mutableListOf<Long>()
            repeat(1000) {
                val time = measureNanoTime { operation() }
                times.add(time)
            }
            
            benchmarkResults[name] = BenchmarkResult(
                name = name,
                min = times.minOrNull() ?: 0,
                max = times.maxOrNull() ?: 0,
                average = times.average(),
                median = times.sorted()[times.size / 2].toDouble(),
                p95 = times.sorted()[(times.size * 0.95).toInt()].toDouble(),
                p99 = times.sorted()[(times.size * 0.99).toInt()].toDouble()
            )
        }
        
        // Print benchmark report
        println("\n=== Benchmark Results ===")
        benchmarkResults.forEach { (_, result) ->
            println(result.format())
        }
        
        // Verify performance requirements
        benchmarkResults.forEach { (name, result) ->
            assertTrue(
                result.average < 1_000_000, // 1ms in nanoseconds
                "$name average time ${result.average / 1_000_000}ms exceeds 1ms threshold"
            )
        }
    }
    
    // ========== Helper Methods ==========
    
    private fun getMemoryUsageMB(): Long {
        val runtime = Runtime.getRuntime()
        return (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024)
    }
    
    private fun getMemoryUsageBytes(): Long {
        val runtime = Runtime.getRuntime()
        return runtime.totalMemory() - runtime.freeMemory()
    }
    
    private fun getCpuUsage(): Double {
        // Simplified CPU usage calculation
        return Random.nextDouble(20.0, 80.0) // Replace with actual CPU monitoring
    }
    
    private fun calculateStatistics(values: List<Long>): LatencyStatistics {
        val sorted = values.sorted()
        return LatencyStatistics(
            min = sorted.first(),
            max = sorted.last(),
            average = values.average(),
            median = sorted[sorted.size / 2],
            p95 = sorted[(sorted.size * 0.95).toInt()],
            p99 = sorted[(sorted.size * 0.99).toInt()]
        )
    }
    
    private fun printLatencyReport(stats: LatencyStatistics) {
        println("""
            Latency Statistics:
              Min: ${stats.min}ms
              Max: ${stats.max}ms
              Average: ${stats.average.format(2)}ms
              Median: ${stats.median}ms
              P95: ${stats.p95}ms
              P99: ${stats.p99}ms
        """.trimIndent())
    }
    
    private fun Double.format(decimals: Int): String = "%.${decimals}f".format(this)
    
    data class LatencyStatistics(
        val min: Long,
        val max: Long,
        val average: Double,
        val median: Long,
        val p95: Long,
        val p99: Long
    )
    
    data class BenchmarkResult(
        val name: String,
        val min: Long,
        val max: Long,
        val average: Double,
        val median: Double,
        val p95: Double,
        val p99: Double
    ) {
        fun format(): String = """
            $name:
              Min: ${min / 1000}μs
              Max: ${max / 1000}μs
              Avg: ${(average / 1000).format(2)}μs
              Median: ${(median / 1000).format(2)}μs
              P95: ${(p95 / 1000).format(2)}μs
              P99: ${(p99 / 1000).format(2)}μs
        """.trimIndent()
    }
}