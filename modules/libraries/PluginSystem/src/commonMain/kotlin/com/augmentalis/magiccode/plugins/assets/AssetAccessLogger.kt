package com.augmentalis.magiccode.plugins.assets

import com.augmentalis.magiccode.plugins.core.PluginLog
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Persistent asset access logger for security auditing (FR-016).
 *
 * Logs all asset resolution attempts with timestamps and outcomes.
 * Supports querying, filtering, and export functionality.
 */
class AssetAccessLogger(
    private val maxInMemoryLogs: Int = 1000
) {
    private val mutex = Mutex()
    private val inMemoryLogs = mutableListOf<AssetAccessLog>()

    companion object {
        private const val TAG = "AssetAccessLogger"
    }

    /**
     * Asset access log entry.
     */
    data class AssetAccessLog(
        val id: Long = System.currentTimeMillis(),
        val uri: String,
        val status: AccessStatus,
        val timestamp: Long = System.currentTimeMillis(),
        val details: String? = null
    )

    /**
     * Access status enum.
     */
    enum class AccessStatus {
        SUCCESS,
        CACHE_HIT,
        INVALID_URI,
        PLUGIN_NOT_FOUND,
        FILE_NOT_FOUND,
        SECURITY_VIOLATION,
        NO_FALLBACK,
        FALLBACK_MISSING,
        FALLBACK_SUCCESS,
        ERROR
    }

    /**
     * Log an asset access attempt.
     *
     * @param uri Asset URI
     * @param status Access status
     * @param details Optional additional details
     */
    suspend fun log(uri: String, status: AccessStatus, details: String? = null) {
        mutex.withLock {
            val logEntry = AssetAccessLog(
                uri = uri,
                status = status,
                details = details
            )

            // Add to in-memory logs
            inMemoryLogs.add(logEntry)

            // Trim if exceeding limit (FIFO)
            if (inMemoryLogs.size > maxInMemoryLogs) {
                inMemoryLogs.removeAt(0)
            }

            PluginLog.d(TAG, "Asset Access: $uri | Status: $status | Time: ${logEntry.timestamp}")

            // TODO: Persist to database for long-term storage
            // persistToDatabase(logEntry)
        }
    }

    /**
     * Get recent access logs.
     *
     * @param limit Maximum number of logs to return
     * @return List of recent access logs
     */
    suspend fun getRecentLogs(limit: Int = 100): List<AssetAccessLog> {
        return mutex.withLock {
            inMemoryLogs.takeLast(limit.coerceAtMost(inMemoryLogs.size))
        }
    }

    /**
     * Get logs for a specific plugin.
     *
     * @param pluginId Plugin identifier
     * @param limit Maximum number of logs to return
     * @return List of access logs for the plugin
     */
    suspend fun getLogsForPlugin(pluginId: String, limit: Int = 100): List<AssetAccessLog> {
        return mutex.withLock {
            inMemoryLogs
                .filter { it.uri.startsWith("plugin://$pluginId/") }
                .takeLast(limit)
        }
    }

    /**
     * Get logs by status.
     *
     * @param status Access status to filter by
     * @param limit Maximum number of logs to return
     * @return List of access logs with the specified status
     */
    suspend fun getLogsByStatus(status: AccessStatus, limit: Int = 100): List<AssetAccessLog> {
        return mutex.withLock {
            inMemoryLogs
                .filter { it.status == status }
                .takeLast(limit)
        }
    }

    /**
     * Get logs within a time range.
     *
     * @param startTime Start timestamp (inclusive)
     * @param endTime End timestamp (inclusive)
     * @return List of access logs within the time range
     */
    suspend fun getLogsByTimeRange(startTime: Long, endTime: Long): List<AssetAccessLog> {
        return mutex.withLock {
            inMemoryLogs.filter { it.timestamp in startTime..endTime }
        }
    }

    /**
     * Clear all in-memory logs.
     *
     * Does not affect database-persisted logs.
     */
    suspend fun clearInMemoryLogs() {
        mutex.withLock {
            val count = inMemoryLogs.size
            inMemoryLogs.clear()
            PluginLog.i(TAG, "Cleared $count in-memory access logs")
        }
    }

    /**
     * Get access statistics.
     *
     * @return Map of access metrics
     */
    suspend fun getStatistics(): Map<String, Any> {
        return mutex.withLock {
            val totalAccesses = inMemoryLogs.size
            val successfulAccesses = inMemoryLogs.count { it.status == AccessStatus.SUCCESS || it.status == AccessStatus.CACHE_HIT }
            val failures = totalAccesses - successfulAccesses
            val securityViolations = inMemoryLogs.count { it.status == AccessStatus.SECURITY_VIOLATION }

            mapOf(
                "totalAccesses" to totalAccesses,
                "successfulAccesses" to successfulAccesses,
                "failures" to failures,
                "securityViolations" to securityViolations,
                "successRate" to if (totalAccesses > 0) (successfulAccesses.toDouble() / totalAccesses * 100.0) else 0.0
            )
        }
    }

    /**
     * Export logs to CSV format.
     *
     * @param logs List of logs to export
     * @return CSV string
     */
    fun exportToCSV(logs: List<AssetAccessLog>): String {
        val header = "ID,URI,Status,Timestamp,Details\n"
        val rows = logs.joinToString("\n") { log ->
            "${log.id},${log.uri},${log.status},${log.timestamp},${log.details ?: ""}"
        }
        return header + rows
    }

    /**
     * Export logs to JSON format.
     *
     * @param logs List of logs to export
     * @return JSON string
     */
    fun exportToJSON(logs: List<AssetAccessLog>): String {
        return buildString {
            append("[\n")
            logs.forEachIndexed { index, log ->
                append("  {\n")
                append("    \"id\": ${log.id},\n")
                append("    \"uri\": \"${log.uri}\",\n")
                append("    \"status\": \"${log.status}\",\n")
                append("    \"timestamp\": ${log.timestamp}")
                if (log.details != null) {
                    append(",\n    \"details\": \"${log.details}\"")
                }
                append("\n  }")
                if (index < logs.size - 1) {
                    append(",")
                }
                append("\n")
            }
            append("]")
        }
    }

    // TODO: Database persistence methods
    // private suspend fun persistToDatabase(log: AssetAccessLog) { ... }
    // suspend fun queryDatabaseLogs(filter: LogFilter): List<AssetAccessLog> { ... }
}
