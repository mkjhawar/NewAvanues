package com.augmentalis.magicelements.core.resources

import android.content.Context
import kotlinx.coroutines.runBlocking
import kotlin.system.measureTimeMillis

/**
 * Performance benchmark for icon loading
 *
 * Measures:
 * - Cold cache load times
 * - Warm cache load times
 * - Batch preload performance
 * - Cache hit rates
 *
 * @since 3.0.0-flutter-parity
 */
class IconPerformanceBenchmark(
    private val context: Context
) {
    private val iconManager = AndroidIconResourceManager.getInstance(context)

    /**
     * Benchmark result
     */
    data class BenchmarkResult(
        val operation: String,
        val iterations: Int,
        val totalTimeMs: Long,
        val avgTimeMs: Double,
        val minTimeMs: Long,
        val maxTimeMs: Long,
        val cacheHitRate: Float? = null
    ) {
        fun prettyPrint(): String = buildString {
            appendLine("=== $operation ===")
            appendLine("Iterations: $iterations")
            appendLine("Total Time: ${totalTimeMs}ms")
            appendLine("Average Time: ${"%.2f".format(avgTimeMs)}ms")
            appendLine("Min Time: ${minTimeMs}ms")
            appendLine("Max Time: ${maxTimeMs}ms")
            cacheHitRate?.let {
                appendLine("Cache Hit Rate: ${"%.1f".format(it * 100)}%")
            }
        }
    }

    /**
     * Benchmark Material Icon loading (cold cache)
     */
    fun benchmarkMaterialIconColdCache(iterations: Int = 100): BenchmarkResult {
        val times = mutableListOf<Long>()
        val commonIcons = listOf(
            "check", "close", "add", "remove", "delete",
            "edit", "settings", "person", "home", "search",
            "star", "favorite", "share", "send", "info"
        )

        iconManager.clearCache(memoryOnly = false)

        repeat(iterations) { iteration ->
            iconManager.clearCache(memoryOnly = false)
            val iconName = commonIcons[iteration % commonIcons.size]
            val icon = IconResource.MaterialIcon(iconName)

            val time = measureTimeMillis {
                runBlocking {
                    iconManager.loadIcon(icon)
                }
            }
            times.add(time)
        }

        return BenchmarkResult(
            operation = "Material Icon Load (Cold Cache)",
            iterations = iterations,
            totalTimeMs = times.sum(),
            avgTimeMs = times.average(),
            minTimeMs = times.minOrNull() ?: 0,
            maxTimeMs = times.maxOrNull() ?: 0
        )
    }

    /**
     * Benchmark Material Icon loading (warm cache)
     */
    fun benchmarkMaterialIconWarmCache(iterations: Int = 100): BenchmarkResult {
        val times = mutableListOf<Long>()
        val commonIcons = listOf(
            "check", "close", "add", "remove", "delete",
            "edit", "settings", "person", "home", "search"
        )

        // Warm up cache
        iconManager.clearCache(memoryOnly = false)
        runBlocking {
            commonIcons.forEach { iconName ->
                iconManager.loadIcon(IconResource.MaterialIcon(iconName))
            }
        }

        val initialStats = iconManager.getCacheStats()

        repeat(iterations) { iteration ->
            val iconName = commonIcons[iteration % commonIcons.size]
            val icon = IconResource.MaterialIcon(iconName)

            val time = measureTimeMillis {
                runBlocking {
                    iconManager.loadIcon(icon)
                }
            }
            times.add(time)
        }

        val finalStats = iconManager.getCacheStats()
        val cacheHitRate = if (finalStats.totalRequests > initialStats.totalRequests) {
            (finalStats.cacheHits - initialStats.cacheHits).toFloat() /
                (finalStats.totalRequests - initialStats.totalRequests)
        } else {
            null
        }

        return BenchmarkResult(
            operation = "Material Icon Load (Warm Cache)",
            iterations = iterations,
            totalTimeMs = times.sum(),
            avgTimeMs = times.average(),
            minTimeMs = times.minOrNull() ?: 0,
            maxTimeMs = times.maxOrNull() ?: 0,
            cacheHitRate = cacheHitRate
        )
    }

    /**
     * Benchmark batch icon preloading
     */
    fun benchmarkBatchPreload(iconCount: Int = 50): BenchmarkResult {
        val commonIcons = listOf(
            "check", "close", "add", "remove", "delete",
            "edit", "settings", "person", "home", "search",
            "star", "favorite", "share", "send", "info",
            "warning", "error", "help", "notifications", "mail",
            "phone", "camera", "image", "video", "music",
            "play", "pause", "stop", "refresh", "sync",
            "arrow_back", "arrow_forward", "menu", "more_vert", "expand_more",
            "lock", "visibility", "vpn_key", "place", "map",
            "shopping_cart", "payment", "alarm", "event", "cloud",
            "folder", "filter_list", "sort", "dashboard", "group"
        )

        val iconsToPreload = commonIcons.take(iconCount).map {
            IconResource.MaterialIcon(it)
        }

        iconManager.clearCache(memoryOnly = false)

        val time = measureTimeMillis {
            runBlocking {
                iconManager.preloadIcons(iconsToPreload)
            }
        }

        return BenchmarkResult(
            operation = "Batch Preload ($iconCount icons)",
            iterations = 1,
            totalTimeMs = time,
            avgTimeMs = time.toDouble(),
            minTimeMs = time,
            maxTimeMs = time
        )
    }

    /**
     * Benchmark cache performance under load
     */
    fun benchmarkCachePerformance(iterations: Int = 1000): BenchmarkResult {
        val commonIcons = listOf(
            "check", "close", "add", "remove", "delete",
            "edit", "settings", "person", "home", "search",
            "star", "favorite", "share", "send", "info",
            "warning", "error", "help", "notifications", "mail"
        )

        iconManager.clearCache(memoryOnly = false)

        // Preload half the icons
        runBlocking {
            commonIcons.take(10).forEach { iconName ->
                iconManager.loadIcon(IconResource.MaterialIcon(iconName))
            }
        }

        val initialStats = iconManager.getCacheStats()
        val times = mutableListOf<Long>()

        repeat(iterations) { iteration ->
            val iconName = commonIcons[iteration % commonIcons.size]
            val icon = IconResource.MaterialIcon(iconName)

            val time = measureTimeMillis {
                runBlocking {
                    iconManager.loadIcon(icon)
                }
            }
            times.add(time)
        }

        val finalStats = iconManager.getCacheStats()
        val cacheHitRate = if (finalStats.totalRequests > initialStats.totalRequests) {
            (finalStats.cacheHits - initialStats.cacheHits).toFloat() /
                (finalStats.totalRequests - initialStats.totalRequests)
        } else {
            null
        }

        return BenchmarkResult(
            operation = "Cache Performance Under Load",
            iterations = iterations,
            totalTimeMs = times.sum(),
            avgTimeMs = times.average(),
            minTimeMs = times.minOrNull() ?: 0,
            maxTimeMs = times.maxOrNull() ?: 0,
            cacheHitRate = cacheHitRate
        )
    }

    /**
     * Run all benchmarks and return results
     */
    fun runAllBenchmarks(): List<BenchmarkResult> {
        return listOf(
            benchmarkMaterialIconColdCache(100),
            benchmarkMaterialIconWarmCache(100),
            benchmarkBatchPreload(50),
            benchmarkCachePerformance(1000)
        )
    }

    /**
     * Print benchmark results
     */
    fun printResults(results: List<BenchmarkResult>) {
        println("\n=================================================")
        println("           ICON PERFORMANCE BENCHMARK")
        println("=================================================\n")

        results.forEach { result ->
            println(result.prettyPrint())
            println()
        }

        println("=================================================\n")
    }

    companion object {
        /**
         * Run benchmarks and print results
         */
        fun runAndPrint(context: Context) {
            val benchmark = IconPerformanceBenchmark(context)
            val results = benchmark.runAllBenchmarks()
            benchmark.printResults(results)
        }
    }
}
