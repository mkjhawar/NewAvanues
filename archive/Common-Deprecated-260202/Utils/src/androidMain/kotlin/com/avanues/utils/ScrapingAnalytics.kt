/**
 * ScrapingAnalytics.kt - UI scraping performance analytics and monitoring
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: Claude Code (AI Assistant)
 * Created: 2025-11-09
 * Phase: 3 (Medium Priority)
 * Issue: Scraping analytics for performance tracking
 */
package com.avanues.utils

import android.util.Log
import com.augmentalis.voiceos.constants.VoiceOSConstants
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Thread-safe analytics collector for UI scraping operations
 *
 * Tracks:
 * - Scraping performance metrics (time, element count)
 * - App scraping patterns
 * - Error rates and types
 * - Cache hit/miss ratios
 * - Tree traversal statistics
 * - Memory usage trends
 *
 * Usage:
 * ```kotlin
 * // Record scraping operation
 * val startTime = System.currentTimeMillis()
 * val elements = scrapeApp(packageName)
 * scrapingAnalytics.recordScrape(
 *     packageName = packageName,
 *     elementCount = elements.size,
 *     durationMs = System.currentTimeMillis() - startTime,
 *     cacheHit = false,
 *     treeDepth = maxDepth
 * )
 *
 * // Get analytics summary
 * val summary = scrapingAnalytics.getSummary()
 * Log.i(TAG, "Avg scrape time: ${summary.avgScrapeTimeMs}ms")
 * Log.i(TAG, "Cache hit rate: ${summary.cacheHitRate}%")
 * ```
 *
 * Thread Safety: All methods are thread-safe using ConcurrentHashMap and atomic operations
 */
class ScrapingAnalytics(
    private val maxAppTracking: Int = VoiceOSConstants.Metrics.MAX_METRICS_COMMANDS
) {
    private val TAG = "ScrapingAnalytics"

    // Global counters
    private val totalScrapes = AtomicLong(0)
    private val successfulScrapes = AtomicLong(0)
    private val failedScrapes = AtomicLong(0)
    private val cacheHits = AtomicLong(0)
    private val cacheMisses = AtomicLong(0)

    // Per-app statistics
    private val appStats = ConcurrentHashMap<String, AppScrapingStats>()

    // Error tracking
    private val errorCounts = ConcurrentHashMap<String, AtomicInteger>()

    // Performance tracking
    private val scrapingTimes = ConcurrentHashMap<Long, Long>() // timestamp -> duration
    private val maxStoredTimes = 1000 // Keep last 1000 scrapes

    // Tree statistics
    private val treeDepths = ConcurrentHashMap<Long, Int>() // timestamp -> depth
    private val elementCounts = ConcurrentHashMap<Long, Int>() // timestamp -> count

    /**
     * Record a scraping operation
     *
     * @param packageName The app package that was scraped
     * @param elementCount Number of elements found
     * @param durationMs Scraping duration in milliseconds
     * @param success Whether the scrape succeeded
     * @param cacheHit Whether result came from cache
     * @param treeDepth Maximum tree depth traversed
     * @param errorType Optional error type if scrape failed
     */
    fun recordScrape(
        packageName: String,
        elementCount: Int,
        durationMs: Long,
        success: Boolean = true,
        cacheHit: Boolean = false,
        treeDepth: Int = 0,
        errorType: String? = null
    ) {
        // Update global counters
        totalScrapes.incrementAndGet()
        if (success) {
            successfulScrapes.incrementAndGet()
        } else {
            failedScrapes.incrementAndGet()
            errorType?.let { recordError(it) }
        }

        if (cacheHit) {
            cacheHits.incrementAndGet()
        } else {
            cacheMisses.incrementAndGet()
        }

        // Update app-specific stats
        val stats = appStats.computeIfAbsent(packageName) { AppScrapingStats(packageName) }
        stats.recordScrape(elementCount, durationMs, success, cacheHit, treeDepth)

        // Store performance metrics
        val timestamp = System.currentTimeMillis()
        scrapingTimes[timestamp] = durationMs
        treeDepths[timestamp] = treeDepth
        elementCounts[timestamp] = elementCount

        // Evict old metrics if over limit
        evictOldMetrics()

        // Check if we need to evict old apps
        if (appStats.size > maxAppTracking) {
            evictLeastScrapedApp()
        }
    }

    /**
     * Record a cache hit (without full scrape)
     */
    fun recordCacheHit(packageName: String) {
        cacheHits.incrementAndGet()
        totalScrapes.incrementAndGet()

        val stats = appStats.computeIfAbsent(packageName) { AppScrapingStats(packageName) }
        stats.recordCacheHit()
    }

    /**
     * Record an error occurrence
     */
    private fun recordError(errorType: String) {
        errorCounts.computeIfAbsent(errorType) { AtomicInteger(0) }
            .incrementAndGet()
    }

    /**
     * Get comprehensive analytics summary
     */
    fun getSummary(): AnalyticsSummary {
        val total = totalScrapes.get()
        val successful = successfulScrapes.get()
        val failed = failedScrapes.get()
        val hits = cacheHits.get()
        val misses = cacheMisses.get()

        val successRate = if (total > 0) {
            (successful.toDouble() / total.toDouble() * 100.0)
        } else {
            0.0
        }

        val cacheHitRate = if ((hits + misses) > 0) {
            (hits.toDouble() / (hits + misses).toDouble() * 100.0)
        } else {
            0.0
        }

        // Calculate timing statistics
        val times = scrapingTimes.values.toList()
        val avgTime = if (times.isNotEmpty()) times.average() else 0.0
        val maxTime = times.maxOrNull() ?: 0L
        val minTime = times.minOrNull() ?: 0L
        val p95Time = calculatePercentile(times, 95.0)

        // Calculate tree statistics
        val depths = treeDepths.values.toList()
        val avgDepth = if (depths.isNotEmpty()) depths.average() else 0.0
        val maxDepth = depths.maxOrNull() ?: 0

        // Calculate element statistics
        val counts = elementCounts.values.toList()
        val avgElements = if (counts.isNotEmpty()) counts.average() else 0.0
        val maxElements = counts.maxOrNull() ?: 0

        // Top scraped apps
        val topApps = appStats.values
            .sortedByDescending { it.getTotalScrapes() }
            .take(10)
            .map { it.packageName to it.getTotalScrapes() }

        // Top errors
        val topErrors = errorCounts.entries
            .sortedByDescending { it.value.get() }
            .take(10)
            .map { it.key to it.value.get() }

        return AnalyticsSummary(
            totalScrapes = total,
            successfulScrapes = successful,
            failedScrapes = failed,
            successRate = successRate,
            cacheHits = hits,
            cacheMisses = misses,
            cacheHitRate = cacheHitRate,
            avgScrapeTimeMs = avgTime,
            minScrapeTimeMs = minTime,
            maxScrapeTimeMs = maxTime,
            p95ScrapeTimeMs = p95Time,
            avgTreeDepth = avgDepth,
            maxTreeDepth = maxDepth,
            avgElementCount = avgElements,
            maxElementCount = maxElements,
            topApps = topApps,
            topErrors = topErrors,
            uniqueAppsTracked = appStats.size
        )
    }

    /**
     * Get analytics for a specific app
     */
    fun getAppAnalytics(packageName: String): AppAnalytics? {
        val stats = appStats[packageName] ?: return null

        val total = stats.getTotalScrapes()
        val successful = stats.getSuccessfulScrapes()
        val failed = stats.getFailedScrapes()
        val hits = stats.getCacheHits()

        val successRate = if (total > 0) {
            (successful.toDouble() / total.toDouble() * 100.0)
        } else {
            0.0
        }

        val cacheHitRate = if (total > 0) {
            (hits.toDouble() / total.toDouble() * 100.0)
        } else {
            0.0
        }

        val times = stats.getScrapeTimes()
        val avgTime = if (times.isNotEmpty()) times.average() else 0.0
        val maxTime = times.maxOrNull() ?: 0L

        return AppAnalytics(
            packageName = packageName,
            totalScrapes = total,
            successfulScrapes = successful,
            failedScrapes = failed,
            successRate = successRate,
            cacheHits = hits,
            cacheHitRate = cacheHitRate,
            avgScrapeTimeMs = avgTime,
            maxScrapeTimeMs = maxTime,
            avgElementCount = stats.getAvgElementCount(),
            maxTreeDepth = stats.getMaxTreeDepth()
        )
    }

    /**
     * Clear all analytics
     */
    fun reset() {
        totalScrapes.set(0)
        successfulScrapes.set(0)
        failedScrapes.set(0)
        cacheHits.set(0)
        cacheMisses.set(0)
        appStats.clear()
        errorCounts.clear()
        scrapingTimes.clear()
        treeDepths.clear()
        elementCounts.clear()
    }

    /**
     * Export analytics to JSON-compatible map
     */
    fun exportAnalytics(): Map<String, Any> {
        val summary = getSummary()
        return mapOf(
            "timestamp" to System.currentTimeMillis(),
            "totalScrapes" to summary.totalScrapes,
            "successfulScrapes" to summary.successfulScrapes,
            "failedScrapes" to summary.failedScrapes,
            "successRate" to summary.successRate,
            "cacheHitRate" to summary.cacheHitRate,
            "avgScrapeTimeMs" to summary.avgScrapeTimeMs,
            "maxScrapeTimeMs" to summary.maxScrapeTimeMs,
            "p95ScrapeTimeMs" to summary.p95ScrapeTimeMs,
            "avgTreeDepth" to summary.avgTreeDepth,
            "avgElementCount" to summary.avgElementCount,
            "topApps" to summary.topApps.map { mapOf("package" to it.first, "scrapes" to it.second) },
            "topErrors" to summary.topErrors.map { mapOf("error" to it.first, "count" to it.second) },
            "uniqueApps" to summary.uniqueAppsTracked
        )
    }

    /**
     * Evict least-scraped app to maintain memory bounds
     */
    private fun evictLeastScrapedApp() {
        val leastScraped = appStats.values
            .minByOrNull { it.getTotalScrapes() }
            ?.packageName

        leastScraped?.let {
            appStats.remove(it)
            Log.d(TAG, "Evicted least-scraped app: $it")
        }
    }

    /**
     * Evict old metrics to maintain memory bounds
     */
    private fun evictOldMetrics() {
        if (scrapingTimes.size > maxStoredTimes) {
            val oldestTime = scrapingTimes.keys.minOrNull()
            oldestTime?.let {
                scrapingTimes.remove(it)
                treeDepths.remove(it)
                elementCounts.remove(it)
            }
        }
    }

    /**
     * Calculate percentile from list
     */
    private fun calculatePercentile(values: List<Long>, percentile: Double): Long {
        if (values.isEmpty()) return 0L

        val sorted = values.sorted()
        val index = ((percentile / 100.0) * sorted.size).toInt()
            .coerceIn(0, sorted.size - 1)

        return sorted[index]
    }

    /**
     * Thread-safe per-app scraping statistics
     */
    private class AppScrapingStats(val packageName: String) {
        private val totalScrapes = AtomicLong(0)
        private val successfulScrapes = AtomicLong(0)
        private val failedScrapes = AtomicLong(0)
        private val cacheHits = AtomicLong(0)

        // Store recent metrics (bounded)
        private val scrapeTimes = ConcurrentHashMap<Long, Long>() // timestamp -> duration
        private val elementCounts = ConcurrentHashMap<Long, Int>() // timestamp -> count
        private val treeDepths = ConcurrentHashMap<Long, Int>() // timestamp -> depth
        private val maxStored = 100 // Keep last 100 per app

        fun recordScrape(elementCount: Int, durationMs: Long, success: Boolean, cacheHit: Boolean, treeDepth: Int) {
            totalScrapes.incrementAndGet()
            if (success) {
                successfulScrapes.incrementAndGet()
            } else {
                failedScrapes.incrementAndGet()
            }
            if (cacheHit) {
                cacheHits.incrementAndGet()
            }

            // Store metrics
            val timestamp = System.currentTimeMillis()
            scrapeTimes[timestamp] = durationMs
            elementCounts[timestamp] = elementCount
            treeDepths[timestamp] = treeDepth

            // Evict old metrics
            if (scrapeTimes.size > maxStored) {
                val oldest = scrapeTimes.keys.minOrNull()
                oldest?.let {
                    scrapeTimes.remove(it)
                    elementCounts.remove(it)
                    treeDepths.remove(it)
                }
            }
        }

        fun recordCacheHit() {
            totalScrapes.incrementAndGet()
            cacheHits.incrementAndGet()
        }

        fun getTotalScrapes(): Long = totalScrapes.get()
        fun getSuccessfulScrapes(): Long = successfulScrapes.get()
        fun getFailedScrapes(): Long = failedScrapes.get()
        fun getCacheHits(): Long = cacheHits.get()
        fun getScrapeTimes(): List<Long> = scrapeTimes.values.toList()
        fun getAvgElementCount(): Double {
            val counts = elementCounts.values.toList()
            return if (counts.isNotEmpty()) counts.average() else 0.0
        }
        fun getMaxTreeDepth(): Int = treeDepths.values.maxOrNull() ?: 0
    }
}

/**
 * Comprehensive scraping analytics summary
 */
data class AnalyticsSummary(
    val totalScrapes: Long,
    val successfulScrapes: Long,
    val failedScrapes: Long,
    val successRate: Double,
    val cacheHits: Long,
    val cacheMisses: Long,
    val cacheHitRate: Double,
    val avgScrapeTimeMs: Double,
    val minScrapeTimeMs: Long,
    val maxScrapeTimeMs: Long,
    val p95ScrapeTimeMs: Long,
    val avgTreeDepth: Double,
    val maxTreeDepth: Int,
    val avgElementCount: Double,
    val maxElementCount: Int,
    val topApps: List<Pair<String, Long>>,
    val topErrors: List<Pair<String, Int>>,
    val uniqueAppsTracked: Int
)

/**
 * Per-app scraping analytics
 */
data class AppAnalytics(
    val packageName: String,
    val totalScrapes: Long,
    val successfulScrapes: Long,
    val failedScrapes: Long,
    val successRate: Double,
    val cacheHits: Long,
    val cacheHitRate: Double,
    val avgScrapeTimeMs: Double,
    val maxScrapeTimeMs: Long,
    val avgElementCount: Double,
    val maxTreeDepth: Int
)
