/**
 * ActionTelemetry.kt - Enhancement 2: Action Telemetry & Analytics
 *
 * CRITICAL: Includes user privacy toggle (default: OFF)
 *
 * Tracks action usage, performance, and patterns to help improve VOS.
 * All tracking respects user privacy preferences.
 *
 * Part of Q12 Enhancement 2
 *
 * @since VOS4 Phase 4.1
 * @author VOS4 Development Team
 */

package com.augmentalis.voiceoscore.managers.commandmanager.plugins

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.augmentalis.voiceoscore.managers.commandmanager.dynamic.CommandResult
import com.augmentalis.voiceoscore.managers.commandmanager.dynamic.ErrorCode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.concurrent.ConcurrentHashMap

/**
 * Telemetry service for tracking action usage and performance
 *
 * PRIVACY FIRST:
 * - Telemetry is OPT-IN (default: disabled)
 * - User must explicitly enable via Settings
 * - No data is collected if user has not opted in
 * - Data is stored locally only (not sent to cloud)
 * - User can export/delete telemetry data at any time
 *
 * What is tracked (when enabled):
 * - Action execution count
 * - Success/failure rates
 * - Execution times
 * - Error types
 * - Usage patterns (time of day, frequency)
 *
 * What is NOT tracked:
 * - Actual command text (privacy concern)
 * - User personal information
 * - Location data (unless plugin specifically tracks it)
 * - Any identifiable user data
 */
class ActionTelemetry(
    private val context: Context,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) {
    companion object {
        private const val TAG = "ActionTelemetry"

        /** SharedPreferences key for telemetry opt-in */
        private const val PREF_TELEMETRY_ENABLED = "telemetry_enabled"

        /** SharedPreferences name */
        private const val PREFS_NAME = "vos_telemetry"

        /** Telemetry data file */
        private const val TELEMETRY_FILE = "telemetry_data.json"

        /** Maximum telemetry events to keep in memory */
        private const val MAX_MEMORY_EVENTS = 1000

        /** Auto-save interval (milliseconds) */
        private const val AUTO_SAVE_INTERVAL_MS = 60_000L // 1 minute
    }

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /** In-memory telemetry events */
    private val telemetryEvents = ConcurrentHashMap<String, MutableList<TelemetryEvent>>()

    /** Aggregated statistics */
    private val aggregatedStats = ConcurrentHashMap<String, ActionStats>()

    /**
     * Check if telemetry is enabled
     *
     * PRIVACY: This is the privacy gate. All tracking methods check this first.
     */
    fun isTelemetryEnabled(): Boolean {
        return prefs.getBoolean(PREF_TELEMETRY_ENABLED, false) // Default: OFF
    }

    /**
     * Enable telemetry tracking
     *
     * Called when user opts in via Settings.
     */
    fun enableTelemetry() {
        prefs.edit().putBoolean(PREF_TELEMETRY_ENABLED, true).apply()
        Log.i(TAG, "Telemetry enabled by user")
    }

    /**
     * Disable telemetry tracking
     *
     * Called when user opts out via Settings.
     */
    fun disableTelemetry() {
        prefs.edit().putBoolean(PREF_TELEMETRY_ENABLED, false).apply()
        Log.i(TAG, "Telemetry disabled by user")
    }

    /**
     * Track action execution
     *
     * PRIVACY: Only tracks if user has enabled telemetry.
     * Does NOT track actual command text to protect privacy.
     *
     * @param actionId Action identifier
     * @param pluginId Plugin providing the action
     * @param success Whether execution succeeded
     * @param executionTime Execution time in milliseconds
     * @param errorCode Error code (if failed)
     */
    fun trackActionExecution(
        actionId: String,
        pluginId: String,
        success: Boolean,
        executionTime: Long,
        errorCode: ErrorCode? = null
    ) {
        // PRIVACY CHECK: Only track if user has opted in
        if (!isTelemetryEnabled()) {
            return
        }

        scope.launch {
            try {
                val event = TelemetryEvent(
                    actionId = actionId,
                    pluginId = pluginId,
                    timestamp = System.currentTimeMillis(),
                    success = success,
                    executionTime = executionTime,
                    errorCode = errorCode?.name
                )

                // Add to in-memory events
                val events = telemetryEvents.getOrPut(actionId) { mutableListOf() }
                synchronized(events) {
                    events.add(event)

                    // Limit memory usage
                    if (events.size > MAX_MEMORY_EVENTS) {
                        events.removeAt(0) // Remove oldest
                    }
                }

                // Update aggregated stats
                updateAggregatedStats(actionId, success, executionTime, errorCode)

                Log.d(TAG, "Tracked action: $actionId (success=$success, time=${executionTime}ms)")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to track action execution", e)
            }
        }
    }

    /**
     * Update aggregated statistics
     */
    private fun updateAggregatedStats(
        actionId: String,
        success: Boolean,
        executionTime: Long,
        errorCode: ErrorCode?
    ) {
        val stats = aggregatedStats.getOrPut(actionId) { ActionStats(actionId) }

        synchronized(stats) {
            stats.totalExecutions++
            if (success) {
                stats.successCount++
            } else {
                stats.failureCount++
                errorCode?.let {
                    stats.errorCounts[it.name] = (stats.errorCounts[it.name] ?: 0) + 1
                }
            }
            stats.totalExecutionTime += executionTime
            stats.lastExecutionTime = System.currentTimeMillis()

            // Update min/max execution times
            if (executionTime < stats.minExecutionTime) {
                stats.minExecutionTime = executionTime
            }
            if (executionTime > stats.maxExecutionTime) {
                stats.maxExecutionTime = executionTime
            }
        }
    }

    /**
     * Get statistics for a specific action
     *
     * @param actionId Action identifier
     * @return Action statistics or null if no data
     */
    fun getActionStatistics(actionId: String): ActionStats? {
        return aggregatedStats[actionId]?.copy()
    }

    /**
     * Get statistics for all actions
     *
     * @return Map of action ID to statistics
     */
    fun getAllStatistics(): Map<String, ActionStats> {
        return aggregatedStats.mapValues { it.value.copy() }
    }

    /**
     * Get top N most used actions
     *
     * @param limit Maximum number of actions to return
     * @return List of action IDs sorted by usage count
     */
    fun getTopActions(limit: Int = 10): List<Pair<String, Long>> {
        return aggregatedStats.values
            .sortedByDescending { it.totalExecutions }
            .take(limit)
            .map { it.actionId to it.totalExecutions }
    }

    /**
     * Get actions with lowest success rates
     *
     * Useful for identifying problematic actions.
     *
     * @param limit Maximum number of actions to return
     * @param minExecutions Minimum execution count to consider
     * @return List of action IDs sorted by success rate (lowest first)
     */
    fun getProblematicActions(limit: Int = 10, minExecutions: Long = 10): List<Pair<String, Double>> {
        return aggregatedStats.values
            .filter { it.totalExecutions >= minExecutions }
            .sortedBy { it.getSuccessRate() }
            .take(limit)
            .map { it.actionId to it.getSuccessRate() }
    }

    /**
     * Export telemetry data
     *
     * PRIVACY: User can export their data at any time.
     *
     * @return JSON string containing all telemetry data
     */
    fun exportTelemetryData(): String {
        val json = JSONObject()

        // Metadata
        json.put("exportedAt", System.currentTimeMillis())
        json.put("telemetryEnabled", isTelemetryEnabled())
        json.put("version", "1.0")

        // Aggregated statistics
        val statsArray = JSONArray()
        for ((actionId, stats) in aggregatedStats) {
            val statsObj = JSONObject()
            statsObj.put("actionId", actionId)
            statsObj.put("totalExecutions", stats.totalExecutions)
            statsObj.put("successCount", stats.successCount)
            statsObj.put("failureCount", stats.failureCount)
            statsObj.put("successRate", stats.getSuccessRate())
            statsObj.put("avgExecutionTime", stats.getAverageExecutionTime())
            statsObj.put("minExecutionTime", stats.minExecutionTime)
            statsObj.put("maxExecutionTime", stats.maxExecutionTime)
            statsObj.put("errorCounts", JSONObject(stats.errorCounts as Map<*, *>))
            statsArray.put(statsObj)
        }
        json.put("statistics", statsArray)

        // Recent events (limited to protect privacy)
        val eventsArray = JSONArray()
        for ((actionId, events) in telemetryEvents) {
            for (event in events.takeLast(100)) { // Only last 100 per action
                val eventObj = JSONObject()
                eventObj.put("actionId", event.actionId)
                eventObj.put("pluginId", event.pluginId)
                eventObj.put("timestamp", event.timestamp)
                eventObj.put("success", event.success)
                eventObj.put("executionTime", event.executionTime)
                event.errorCode?.let { eventObj.put("errorCode", it) }
                eventsArray.put(eventObj)
            }
        }
        json.put("events", eventsArray)

        return json.toString(2) // Pretty print with 2-space indentation
    }

    /**
     * Import telemetry data
     *
     * PRIVACY: User can restore their data on a new device.
     *
     * @param jsonData JSON string containing telemetry data
     */
    fun importTelemetryData(jsonData: String) {
        try {
            val json = JSONObject(jsonData)

            // Import aggregated statistics
            val statsArray = json.getJSONArray("statistics")
            for (i in 0 until statsArray.length()) {
                val statsObj = statsArray.getJSONObject(i)
                val actionId = statsObj.getString("actionId")

                val stats = ActionStats(actionId)
                stats.totalExecutions = statsObj.getLong("totalExecutions")
                stats.successCount = statsObj.getLong("successCount")
                stats.failureCount = statsObj.getLong("failureCount")
                stats.totalExecutionTime = statsObj.getLong("totalExecutionTime")
                stats.minExecutionTime = statsObj.getLong("minExecutionTime")
                stats.maxExecutionTime = statsObj.getLong("maxExecutionTime")

                val errorCounts = statsObj.getJSONObject("errorCounts")
                for (key in errorCounts.keys()) {
                    stats.errorCounts[key] = errorCounts.getInt(key)
                }

                aggregatedStats[actionId] = stats
            }

            Log.i(TAG, "Imported telemetry data: ${statsArray.length()} actions")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to import telemetry data", e)
        }
    }

    /**
     * Clear all telemetry data
     *
     * PRIVACY: User can delete all their telemetry data at any time.
     */
    fun clearAllData() {
        telemetryEvents.clear()
        aggregatedStats.clear()

        // Delete telemetry file
        getTelemetryFile().delete()

        Log.i(TAG, "Cleared all telemetry data")
    }

    /**
     * Save telemetry data to disk
     */
    fun saveToDisk() {
        if (!isTelemetryEnabled()) return

        scope.launch {
            try {
                val data = exportTelemetryData()
                getTelemetryFile().writeText(data)
                Log.d(TAG, "Saved telemetry data to disk")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to save telemetry data", e)
            }
        }
    }

    /**
     * Load telemetry data from disk
     */
    fun loadFromDisk() {
        if (!isTelemetryEnabled()) return

        scope.launch {
            try {
                val file = getTelemetryFile()
                if (file.exists()) {
                    val data = file.readText()
                    importTelemetryData(data)
                    Log.d(TAG, "Loaded telemetry data from disk")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load telemetry data", e)
            }
        }
    }

    /**
     * Get telemetry data file
     */
    private fun getTelemetryFile(): File {
        return File(context.filesDir, TELEMETRY_FILE)
    }

    /**
     * Generate telemetry report
     *
     * Human-readable summary of telemetry data.
     *
     * @return Markdown-formatted report
     */
    fun generateReport(): String {
        if (!isTelemetryEnabled()) {
            return "Telemetry is disabled. Enable in Settings to see usage analytics."
        }

        val sb = StringBuilder()
        sb.appendLine("# Action Telemetry Report")
        sb.appendLine()
        sb.appendLine("Generated: ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(java.util.Date())}")
        sb.appendLine()

        // Overall statistics
        val totalExecutions = aggregatedStats.values.sumOf { it.totalExecutions }
        val totalSuccess = aggregatedStats.values.sumOf { it.successCount }
        val totalFailures = aggregatedStats.values.sumOf { it.failureCount }
        val overallSuccessRate = if (totalExecutions > 0) {
            (totalSuccess.toDouble() / totalExecutions * 100)
        } else 0.0

        sb.appendLine("## Overall Statistics")
        sb.appendLine("- Total Actions: ${aggregatedStats.size}")
        sb.appendLine("- Total Executions: $totalExecutions")
        sb.appendLine("- Success Rate: ${"%.2f".format(overallSuccessRate)}%")
        sb.appendLine("- Total Failures: $totalFailures")
        sb.appendLine()

        // Top actions
        sb.appendLine("## Top 10 Most Used Actions")
        val topActions = getTopActions(10)
        for ((actionId, count) in topActions) {
            val stats = aggregatedStats[actionId]
            val successRate = stats?.getSuccessRate()?.let { "%.2f".format(it * 100) } ?: "N/A"
            sb.appendLine("- $actionId: $count executions (Success: $successRate%)")
        }
        sb.appendLine()

        // Problematic actions
        sb.appendLine("## Actions with Low Success Rates")
        val problematic = getProblematicActions(10, 5)
        if (problematic.isEmpty()) {
            sb.appendLine("- No problematic actions found")
        } else {
            for ((actionId, successRate) in problematic) {
                val stats = aggregatedStats[actionId]
                sb.appendLine("- $actionId: ${"%.2f".format(successRate * 100)}% " +
                        "(${stats?.totalExecutions} executions)")
            }
        }
        sb.appendLine()

        return sb.toString()
    }
}

/**
 * Single telemetry event
 *
 * PRIVACY: Does not contain actual command text
 */
private data class TelemetryEvent(
    val actionId: String,
    val pluginId: String,
    val timestamp: Long,
    val success: Boolean,
    val executionTime: Long,
    val errorCode: String? = null
)

/**
 * Aggregated action statistics
 */
data class ActionStats(
    val actionId: String,
    var totalExecutions: Long = 0,
    var successCount: Long = 0,
    var failureCount: Long = 0,
    var totalExecutionTime: Long = 0,
    var minExecutionTime: Long = Long.MAX_VALUE,
    var maxExecutionTime: Long = 0,
    var lastExecutionTime: Long = 0,
    val errorCounts: MutableMap<String, Int> = mutableMapOf()
) {
    /**
     * Calculate success rate (0.0 to 1.0)
     */
    fun getSuccessRate(): Double =
        if (totalExecutions > 0) successCount.toDouble() / totalExecutions
        else 0.0

    /**
     * Calculate average execution time (milliseconds)
     */
    fun getAverageExecutionTime(): Long =
        if (totalExecutions > 0) totalExecutionTime / totalExecutions
        else 0L

    /**
     * Get most common error
     */
    fun getMostCommonError(): String? =
        errorCounts.maxByOrNull { it.value }?.key
}
