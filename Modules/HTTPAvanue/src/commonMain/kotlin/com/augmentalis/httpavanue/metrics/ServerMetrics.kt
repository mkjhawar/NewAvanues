package com.augmentalis.httpavanue.metrics

import com.augmentalis.httpavanue.platform.currentTimeMillis
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Server metrics collector — tracks request counts, response times, error rates, and active connections
 */
class ServerMetrics {
    private val mutex = Mutex()

    var totalRequests: Long = 0L; private set
    var activeConnections: Int = 0; private set
    var totalErrors: Long = 0L; private set
    private var totalResponseTimeMs: Long = 0L
    private val statusCounts = mutableMapOf<Int, Long>()
    private val methodCounts = mutableMapOf<String, Long>()
    private val pathCounts = mutableMapOf<String, Long>()
    private var startTimeMs: Long = currentTimeMillis()

    suspend fun recordRequest(method: String, path: String) = mutex.withLock {
        totalRequests++
        methodCounts[method] = (methodCounts[method] ?: 0) + 1
        pathCounts[path] = (pathCounts[path] ?: 0) + 1
    }

    suspend fun recordResponse(statusCode: Int, durationMs: Long) = mutex.withLock {
        statusCounts[statusCode] = (statusCounts[statusCode] ?: 0) + 1
        totalResponseTimeMs += durationMs
        if (statusCode >= 500) totalErrors++
    }

    suspend fun incrementConnections() = mutex.withLock { activeConnections++ }
    suspend fun decrementConnections() = mutex.withLock { activeConnections-- }

    suspend fun snapshot(): MetricsSnapshot = mutex.withLock {
        val uptimeMs = currentTimeMillis() - startTimeMs
        MetricsSnapshot(
            totalRequests = totalRequests,
            activeConnections = activeConnections,
            totalErrors = totalErrors,
            averageResponseTimeMs = if (totalRequests > 0) totalResponseTimeMs.toDouble() / totalRequests else 0.0,
            requestsPerSecond = if (uptimeMs > 0) totalRequests.toDouble() / (uptimeMs / 1000.0) else 0.0,
            uptimeMs = uptimeMs,
            statusCounts = statusCounts.toMap(),
            methodCounts = methodCounts.toMap(),
            topPaths = pathCounts.entries.sortedByDescending { it.value }.take(10).associate { it.key to it.value },
        )
    }

    suspend fun reset() = mutex.withLock {
        totalRequests = 0; activeConnections = 0; totalErrors = 0
        totalResponseTimeMs = 0; statusCounts.clear(); methodCounts.clear(); pathCounts.clear()
        startTimeMs = currentTimeMillis()
    }
}

data class MetricsSnapshot(
    val totalRequests: Long,
    val activeConnections: Int,
    val totalErrors: Long,
    val averageResponseTimeMs: Double,
    val requestsPerSecond: Double,
    val uptimeMs: Long,
    val statusCounts: Map<Int, Long>,
    val methodCounts: Map<String, Long>,
    val topPaths: Map<String, Long>,
)
