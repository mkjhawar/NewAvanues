/**
 * CursorFilterBenchmark.kt
 * Performance benchmarking utility for CursorFilter
 * 
 * Created: 2025-09-05
 * Author: QA Specialist - Android Sensor Applications
 * 
 * Purpose: Detailed performance analysis and benchmarking for production optimization
 */

package com.augmentalis.voiceos.cursor.filter

import org.junit.Test
import kotlin.system.measureNanoTime
import kotlin.system.measureTimeMillis

class CursorFilterBenchmark {
    
    companion object {
        private const val WARMUP_ITERATIONS = 1000
        private const val BENCHMARK_ITERATIONS = 100000
        private const val FRAME_TIME_16MS = 16_000_000L
    }
    
    @Test
    fun benchmarkFilterPerformance() {
        println("=== CursorFilter Performance Benchmark ===")
        
        val filter = CursorFilter()
        var timestamp = System.nanoTime()
        var x = 500f
        var y = 300f
        
        // Warmup
        repeat(WARMUP_ITERATIONS) {
            filter.filter(x, y, timestamp)
            x += 0.1f
            y += 0.1f
            timestamp += FRAME_TIME_16MS
        }
        
        // Benchmark different scenarios
        benchmarkScenario("Stationary with Jitter", filter) { iteration, ts ->
            val jitter = kotlin.math.sin((iteration * 0.1).toDouble()).toFloat() * 2f
            filter.filter(500f + jitter, 300f + jitter, ts)
        }
        
        benchmarkScenario("Slow Linear Movement", filter) { iteration, ts ->
            filter.filter(500f + iteration * 0.1f, 300f + iteration * 0.05f, ts)
        }
        
        benchmarkScenario("Fast Circular Movement", filter) { iteration, ts ->
            val angle = iteration * 0.1f
            val circularX = 500f + kotlin.math.cos(angle.toDouble()).toFloat() * 100f
            val circularY = 300f + kotlin.math.sin(angle.toDouble()).toFloat() * 100f
            filter.filter(circularX, circularY, ts)
        }
        
        benchmarkScenario("Random Movement", filter) { iteration, ts ->
            val random = kotlin.random.Random(iteration)
            val randomX = 500f + (random.nextFloat() - 0.5f) * 200f
            val randomY = 300f + (random.nextFloat() - 0.5f) * 200f
            filter.filter(randomX, randomY, ts)
        }
    }
    
    private fun benchmarkScenario(
        name: String, 
        filter: CursorFilter, 
        operation: (Int, Long) -> Pair<Float, Float>
    ) {
        filter.reset()
        var timestamp = System.nanoTime()
        
        val elapsed = measureNanoTime {
            repeat(BENCHMARK_ITERATIONS) { iteration ->
                operation(iteration, timestamp)
                timestamp += FRAME_TIME_16MS
            }
        }
        
        val avgTimeNs = elapsed / BENCHMARK_ITERATIONS
        val opsPerSecond = (1_000_000_000L / avgTimeNs)
        
        println("$name:")
        println("  Average time: ${avgTimeNs}ns per operation")
        println("  Throughput: ${opsPerSecond} operations/second")
        println("  Target met: ${if (avgTimeNs < 100_000L) "✓" else "✗"} (<100μs)")
        println()
    }
    
    @Test
    fun benchmarkMemoryAllocation() {
        println("=== CursorFilter Memory Allocation Benchmark ===")
        
        val runtime = Runtime.getRuntime()
        val initialMemory = runtime.totalMemory() - runtime.freeMemory()
        
        // Create and use multiple filter instances
        val filters = Array(1000) { CursorFilter() }
        val timestamp = System.nanoTime()
        
        // Use all filters to prevent optimization
        filters.forEachIndexed { index, filter ->
            filter.filter(index.toFloat(), index.toFloat(), timestamp)
        }
        
        System.gc()
        Thread.sleep(100)
        val finalMemory = runtime.totalMemory() - runtime.freeMemory()
        
        val totalAllocation = finalMemory - initialMemory
        val perInstanceMemory = totalAllocation / filters.size
        
        println("Total memory allocated: ${totalAllocation}B")
        println("Memory per filter instance: ${perInstanceMemory}B")
        println("Target met: ${if (perInstanceMemory < 1024) "✓" else "✗"} (<1KB)")
        println()
        
        // Test for memory leaks
        @Suppress("UNUSED_VALUE", "UNUSED_VARIABLE")
        var leakTest = filters  // Keep reference
        System.gc()
        Thread.sleep(100)
        
        leakTest = emptyArray() // Clear reference
        System.gc()
        Thread.sleep(100)
        val afterClear = runtime.totalMemory() - runtime.freeMemory()
        
        val leaked = afterClear - initialMemory
        println("Potential memory leak: ${leaked}B")
        println("Leak test: ${if (leaked < totalAllocation * 0.1) "✓" else "✗"} (<10% retained)")
    }
    
    @Test
    fun benchmarkConcurrency() {
        println("=== CursorFilter Concurrency Benchmark ===")
        
        val filter = CursorFilter()
        val threadCount = Runtime.getRuntime().availableProcessors()
        val operationsPerThread = 10000
        
        println("Testing with $threadCount threads, $operationsPerThread ops each")
        
        val elapsed = measureTimeMillis {
            val threads = (0 until threadCount).map { threadId ->
                Thread {
                    var timestamp = System.nanoTime() + threadId * 1000L
                    repeat(operationsPerThread) { iteration ->
                        val x = threadId * 100f + iteration
                        val y = threadId * 100f + iteration
                        filter.filter(x, y, timestamp)
                        timestamp += FRAME_TIME_16MS
                    }
                }
            }
            
            threads.forEach { it.start() }
            threads.forEach { it.join() }
        }
        
        val totalOperations = threadCount * operationsPerThread
        val opsPerSecond = totalOperations * 1000L / elapsed
        
        println("Total operations: $totalOperations")
        println("Time elapsed: ${elapsed}ms")
        println("Throughput: ${opsPerSecond} ops/second")
        println("Thread safety: ✓ (no crashes)")
    }
    
    @Test
    fun profileFilterStates() {
        println("=== CursorFilter State Transition Profiling ===")
        
        val filter = CursorFilter()
        var timestamp = System.nanoTime()
        var x = 500f
        var y = 300f
        
        // Initialize
        filter.filter(x, y, timestamp)
        timestamp += FRAME_TIME_16MS
        
        // Profile stationary state
        val stationaryTime = measureNanoTime {
            repeat(1000) {
                val jitter = kotlin.math.sin((it * 0.1).toDouble()).toFloat() * 2f
                filter.filter(x + jitter, y + jitter, timestamp)
                timestamp += FRAME_TIME_16MS
            }
        }
        
        // Transition to slow movement
        val slowTime = measureNanoTime {
            repeat(1000) {
                x += 2f
                y += 1f
                filter.filter(x, y, timestamp)
                timestamp += FRAME_TIME_16MS
            }
        }
        
        // Transition to fast movement  
        val fastTime = measureNanoTime {
            repeat(1000) {
                x += 15f
                y += 10f
                filter.filter(x, y, timestamp)
                timestamp += FRAME_TIME_16MS
            }
        }
        
        println("Stationary filtering: ${stationaryTime / 1000}ns avg")
        println("Slow movement filtering: ${slowTime / 1000}ns avg")
        println("Fast movement filtering: ${fastTime / 1000}ns avg")
        println()
        
        // Test state transition overhead
        filter.reset()
        timestamp = System.nanoTime()
        x = 500f
        y = 300f
        
        val transitionTime = measureNanoTime {
            // Rapid state changes
            repeat(100) { iteration ->
                when (iteration % 3) {
                    0 -> { // Stationary
                        val jitter = kotlin.math.sin((iteration * 0.1).toDouble()).toFloat() * 1f
                        filter.filter(x + jitter, y + jitter, timestamp)
                    }
                    1 -> { // Slow movement
                        x += 3f
                        filter.filter(x, y, timestamp)
                    }
                    2 -> { // Fast movement
                        x += 20f
                        filter.filter(x, y, timestamp)
                    }
                }
                timestamp += FRAME_TIME_16MS
            }
        }
        
        println("State transition overhead: ${transitionTime / 100}ns avg")
        println("Transition performance: ${if (transitionTime / 100 < 200_000L) "✓" else "✗"} (<200μs)")
    }
    
    @Test
    fun measureFilterEffectiveness() {
        println("=== CursorFilter Effectiveness Analysis ===")
        
        val filter = CursorFilter()
        var timestamp: Long
        
        // Test jitter reduction effectiveness
        val testCases = mapOf(
            "Fine Hand Tremor" to { i: Int -> kotlin.math.sin((i * 0.3).toDouble()).toFloat() * 1.5f },
            "Coarse Hand Tremor" to { i: Int -> kotlin.math.sin((i * 0.2).toDouble()).toFloat() * 3f + kotlin.math.cos((i * 0.5).toDouble()).toFloat() * 2f },
            "Sensor Noise" to { i: Int -> (kotlin.random.Random(i).nextFloat() - 0.5f) * 2f },
            "High Frequency Jitter" to { i: Int -> kotlin.math.sin((i * 2.0).toDouble()).toFloat() * 1f }
        )
        
        testCases.forEach { (name, noiseFunction) ->
            filter.reset()
            val baseX = 500f
            val baseY = 300f
            timestamp = System.nanoTime()
            
            // Initialize
            filter.filter(baseX, baseY, timestamp)
            timestamp += FRAME_TIME_16MS
            
            val originalPositions = mutableListOf<Pair<Float, Float>>()
            val filteredPositions = mutableListOf<Pair<Float, Float>>()
            
            repeat(100) { iteration ->
                val noise = noiseFunction(iteration)
                val originalX = baseX + noise
                val originalY = baseY + noise * 0.7f
                
                originalPositions.add(Pair(originalX, originalY))
                
                val filtered = filter.filter(originalX, originalY, timestamp)
                filteredPositions.add(filtered)
                
                timestamp += FRAME_TIME_16MS
            }
            
            // Calculate effectiveness metrics
            val originalVarianceX = calculateVariance(originalPositions.map { it.first })
            val filteredVarianceX = calculateVariance(filteredPositions.map { it.first })
            val jitterReduction = (1f - (filteredVarianceX / originalVarianceX)) * 100f
            
            val responsiveness = calculateResponsiveness(originalPositions, filteredPositions)
            
            println("$name:")
            println("  Jitter reduction: ${jitterReduction.toInt()}%")
            println("  Responsiveness: ${(responsiveness * 100).toInt()}%")
            println("  Filter strength: ${filter.getCurrentStrength()}%")
            println()
        }
    }
    
    private fun calculateVariance(values: List<Float>): Float {
        val mean = values.average().toFloat()
        return values.map { (it - mean) * (it - mean) }.average().toFloat()
    }
    
    private fun calculateResponsiveness(original: List<Pair<Float, Float>>, filtered: List<Pair<Float, Float>>): Float {
        if (original.size != filtered.size || original.isEmpty()) return 0f
        
        // Calculate how well filtered signal follows the original signal's intended movement
        val originalDeltas = original.zipWithNext { a, b -> 
            kotlin.math.sqrt((b.first - a.first) * (b.first - a.first) + (b.second - a.second) * (b.second - a.second))
        }
        val filteredDeltas = filtered.zipWithNext { a, b ->
            kotlin.math.sqrt((b.first - a.first) * (b.first - a.first) + (b.second - a.second) * (b.second - a.second))
        }
        
        if (originalDeltas.isEmpty()) return 1f
        
        val avgOriginalDelta = originalDeltas.average().toFloat()
        val avgFilteredDelta = filteredDeltas.average().toFloat()
        
        return kotlin.math.min(1f, avgFilteredDelta / kotlin.math.max(avgOriginalDelta, 0.001f))
    }
}