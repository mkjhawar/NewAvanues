/**
 * VUIDMetricsRepository.kt - Repository for VUID creation metrics persistence
 * Path: VoiceOSCore/src/main/java/com/augmentalis/voiceoscore/learnapp/database/repository/VUIDMetricsRepository.kt
 *
 * Author: Manoj Jhawar
 * Created: 2025-12-08
 * Feature: LearnApp VUID Creation Fix - Phase 3 (Observability)
 *
 * Purpose:
 * - Persist VUID creation metrics to SQLite database
 * - Query historical metrics for analysis
 * - Generate aggregate statistics
 * - Support diagnostics and debugging
 *
 * Part of: LearnApp-VUID-Creation-Fix-Spec-5081218-V1.md (Phase 3)
 */

package com.augmentalis.voiceoscore.learnapp.database.repository

import com.augmentalis.database.VoiceOSDatabaseManager
import com.augmentalis.voiceoscore.learnapp.metrics.VUIDCreationMetrics
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

/**
 * Repository for VUID creation metrics
 *
 * Provides CRUD operations for persisting and querying VUID creation metrics.
 *
 * TEMPORARY IMPLEMENTATION (2025-12-08):
 * Uses in-memory storage for Phase 3 observability.
 * TODO: Migrate to SQLDelight schema in Phase 4.
 *
 * @property databaseManager VoiceOS database manager (unused in temporary implementation)
 *
 * @since 2025-12-08 (Phase 3: Observability)
 */
class VUIDMetricsRepository(
    @Suppress("UNUSED_PARAMETER") private val databaseManager: VoiceOSDatabaseManager
) {

    /**
     * In-memory storage for metrics (temporary solution)
     * Key: packageName, Value: List of metrics (newest first)
     */
    private val metricsStorage = ConcurrentHashMap<String, MutableList<VUIDCreationMetrics>>()

    /**
     * Initialize database schema (no-op for in-memory implementation)
     */
    suspend fun initializeSchema() {
        // No-op: In-memory storage doesn't need schema initialization
    }

    /**
     * Save metrics to storage
     *
     * @param metrics Metrics to save
     * @return Row ID (index in list)
     */
    suspend fun saveMetrics(metrics: VUIDCreationMetrics): Long = withContext(Dispatchers.IO) {
        val packageMetrics = metricsStorage.getOrPut(metrics.packageName) { mutableListOf() }
        packageMetrics.add(0, metrics) // Add at beginning (newest first)

        // Limit to 100 entries per package
        if (packageMetrics.size > 100) {
            packageMetrics.removeAt(packageMetrics.size - 1)
        }

        return@withContext packageMetrics.size.toLong()
    }

    /**
     * Get latest metrics for package
     *
     * @param packageName Package to query
     * @return Latest metrics or null if not found
     */
    suspend fun getLatestMetrics(packageName: String): VUIDCreationMetrics? =
        withContext(Dispatchers.IO) {
            metricsStorage[packageName]?.firstOrNull()
        }

    /**
     * Get metrics history for package
     *
     * @param packageName Package to query
     * @param limit Maximum number of entries to return
     * @return List of metrics ordered by timestamp (newest first)
     */
    suspend fun getMetricsHistory(
        packageName: String,
        limit: Int = 10
    ): List<VUIDCreationMetrics> = withContext(Dispatchers.IO) {
        metricsStorage[packageName]?.take(limit) ?: emptyList()
    }

    /**
     * Get metrics for date range
     *
     * @param packageName Package to query
     * @param startTimestamp Range start (Unix milliseconds)
     * @param endTimestamp Range end (Unix milliseconds)
     * @return List of metrics in range
     */
    suspend fun getMetricsInRange(
        packageName: String,
        startTimestamp: Long,
        endTimestamp: Long
    ): List<VUIDCreationMetrics> = withContext(Dispatchers.IO) {
        metricsStorage[packageName]?.filter {
            it.explorationTimestamp in startTimestamp..endTimestamp
        } ?: emptyList()
    }

    /**
     * Delete old metrics
     *
     * @param daysToKeep Number of days of metrics to keep
     * @return Number of rows deleted
     */
    suspend fun deleteOldMetrics(daysToKeep: Int): Int = withContext(Dispatchers.IO) {
        val cutoffTimestamp = System.currentTimeMillis() - (daysToKeep * 24 * 60 * 60 * 1000L)
        var deletedCount = 0

        metricsStorage.forEach { (_, metrics) ->
            val sizeBefore = metrics.size
            metrics.removeAll { it.explorationTimestamp < cutoffTimestamp }
            deletedCount += sizeBefore - metrics.size
        }

        deletedCount
    }

    /**
     * Delete all metrics for package
     *
     * @param packageName Package to delete metrics for
     * @return Number of rows deleted
     */
    suspend fun deleteMetricsForPackage(packageName: String): Int = withContext(Dispatchers.IO) {
        val metrics = metricsStorage.remove(packageName)
        metrics?.size ?: 0
    }

    /**
     * Get aggregate statistics across all apps
     *
     * @return Aggregate statistics
     */
    suspend fun getAggregateStats(): AggregateStats = withContext(Dispatchers.IO) {
        val allMetrics = metricsStorage.values.flatten()
        if (allMetrics.isEmpty()) {
            return@withContext AggregateStats(0, 0, 0, 0.0, 0.0, 0.0)
        }

        AggregateStats(
            totalApps = metricsStorage.keys.size,
            totalElements = allMetrics.sumOf { it.elementsDetected.toLong() },
            totalVuids = allMetrics.sumOf { it.vuidsCreated.toLong() },
            averageRate = allMetrics.map { it.creationRate }.average(),
            minRate = allMetrics.minOf { it.creationRate },
            maxRate = allMetrics.maxOf { it.creationRate }
        )
    }

    /**
     * Get aggregate statistics for specific package
     *
     * @param packageName Package to query
     * @return Package-specific aggregate statistics
     */
    suspend fun getAggregateStatsForPackage(packageName: String): PackageAggregateStats =
        withContext(Dispatchers.IO) {
            val metrics = metricsStorage[packageName]
            if (metrics.isNullOrEmpty()) {
                return@withContext PackageAggregateStats(0, 0.0, 0.0, 0.0)
            }

            PackageAggregateStats(
                totalExplorations = metrics.size,
                averageRate = metrics.map { it.creationRate }.average(),
                minRate = metrics.minOf { it.creationRate },
                maxRate = metrics.maxOf { it.creationRate }
            )
        }
}

/**
 * Aggregate statistics across all apps
 */
data class AggregateStats(
    val totalApps: Int,
    val totalElements: Long,
    val totalVuids: Long,
    val averageRate: Double,
    val minRate: Double,
    val maxRate: Double
)

/**
 * Aggregate statistics for specific package
 */
data class PackageAggregateStats(
    val totalExplorations: Int,
    val averageRate: Double,
    val minRate: Double,
    val maxRate: Double
)
