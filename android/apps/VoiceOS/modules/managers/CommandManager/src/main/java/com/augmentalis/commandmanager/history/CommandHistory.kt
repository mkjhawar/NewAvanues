/**
 * CommandHistory.kt - Command history tracking and management
 * Track and manage command execution history
 */

package com.augmentalis.commandmanager.history

import com.augmentalis.voiceos.command.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicLong

/**
 * Command history manager
 * Tracks command execution history with configurable retention policies
 */
class CommandHistory {
    
    companion object {
        private const val TAG = "CommandHistory"
        private const val DEFAULT_MAX_ENTRIES = 1000
        private const val DEFAULT_MAX_AGE_MS = 24 * 60 * 60 * 1000L // 24 hours
    }
    
    // History storage
    private val historyQueue = ConcurrentLinkedQueue<CommandHistoryEntry>()
    
    // History flow for real-time updates
    private val _historyFlow = MutableSharedFlow<CommandHistoryEntry>(replay = 0)
    private val historyFlow = _historyFlow.asSharedFlow()
    
    // Statistics
    private val totalCommandCount = AtomicLong(0)
    private val successfulCommandCount = AtomicLong(0)
    private val failedCommandCount = AtomicLong(0)
    
    // Configuration
    private var maxEntries = DEFAULT_MAX_ENTRIES
    private var maxAgeMs = DEFAULT_MAX_AGE_MS
    private var retentionEnabled = true
    
    // Command frequency tracking
    private val commandFrequency = mutableMapOf<String, Long>()
    private val commandLastUsed = mutableMapOf<String, Long>()
    
    /**
     * Initialize the history manager
     */
    suspend fun initialize() {
        android.util.Log.i(TAG, "Command history initialized (max entries: $maxEntries, max age: ${maxAgeMs}ms)")
    }
    
    /**
     * Shutdown the history manager
     */
    suspend fun shutdown() {
        historyQueue.clear()
        commandFrequency.clear()
        commandLastUsed.clear()
        android.util.Log.i(TAG, "Command history shutdown")
    }
    
    /**
     * Add a command execution entry to history
     */
    fun addEntry(entry: CommandHistoryEntry) {
        // Add to queue
        historyQueue.offer(entry)
        
        // Update statistics
        totalCommandCount.incrementAndGet()
        if (entry.result.success) {
            successfulCommandCount.incrementAndGet()
        } else {
            failedCommandCount.incrementAndGet()
        }
        
        // Update frequency tracking
        val commandId = entry.command.id
        commandFrequency[commandId] = (commandFrequency[commandId] ?: 0) + 1
        commandLastUsed[commandId] = entry.timestamp
        
        // Emit to flow
        _historyFlow.tryEmit(entry)
        
        // Clean up old entries if retention is enabled
        if (retentionEnabled) {
            cleanupOldEntries()
        }
        
        android.util.Log.v(TAG, "Added history entry: ${entry.command.id} (success: ${entry.result.success})")
    }
    
    /**
     * Get the history flow for real-time updates
     */
    fun getHistoryFlow(): Flow<CommandHistoryEntry> {
        return historyFlow
    }
    
    /**
     * Get recent history entries
     */
    fun getRecentEntries(limit: Int = 50): List<CommandHistoryEntry> {
        return historyQueue.toList().takeLast(limit)
    }
    
    /**
     * Get history entries for a specific command
     */
    fun getEntriesForCommand(commandId: String, limit: Int = 20): List<CommandHistoryEntry> {
        return historyQueue.filter { it.command.id == commandId }.takeLast(limit)
    }
    
    /**
     * Get history entries within a time range
     */
    fun getEntriesInTimeRange(startTime: Long, endTime: Long): List<CommandHistoryEntry> {
        return historyQueue.filter { it.timestamp in startTime..endTime }
    }
    
    /**
     * Get history entries by success status
     */
    fun getEntriesByStatus(successful: Boolean, limit: Int = 50): List<CommandHistoryEntry> {
        return historyQueue.filter { it.result.success == successful }.takeLast(limit)
    }
    
    /**
     * Get command statistics
     */
    fun getStatistics(): CommandHistoryStatistics {
        val currentTime = System.currentTimeMillis()
        val last24Hours = currentTime - (24 * 60 * 60 * 1000L)
        val lastHour = currentTime - (60 * 60 * 1000L)
        
        val recent24hEntries = historyQueue.filter { it.timestamp >= last24Hours }
        val recentHourEntries = historyQueue.filter { it.timestamp >= lastHour }
        
        return CommandHistoryStatistics(
            totalCommands = totalCommandCount.get(),
            successfulCommands = successfulCommandCount.get(),
            failedCommands = failedCommandCount.get(),
            totalEntries = historyQueue.size,
            commandsLast24Hours = recent24hEntries.size,
            commandsLastHour = recentHourEntries.size,
            successRateLast24Hours = if (recent24hEntries.isNotEmpty()) {
                (recent24hEntries.count { it.result.success }.toFloat() / recent24hEntries.size) * 100
            } else 0f,
            averageExecutionTime = calculateAverageExecutionTime(),
            mostUsedCommands = getMostUsedCommands(10),
            recentlyUsedCommands = getRecentlyUsedCommands(10)
        )
    }
    
    /**
     * Get most frequently used commands
     */
    fun getMostUsedCommands(limit: Int = 10): List<CommandFrequency> {
        return commandFrequency.entries
            .sortedByDescending { it.value }
            .take(limit)
            .map { CommandFrequency(it.key, it.value) }
    }
    
    /**
     * Get recently used commands
     */
    fun getRecentlyUsedCommands(limit: Int = 10): List<CommandLastUsed> {
        return commandLastUsed.entries
            .sortedByDescending { it.value }
            .take(limit)
            .map { CommandLastUsed(it.key, it.value) }
    }
    
    /**
     * Search history by text content
     */
    fun searchHistory(query: String, limit: Int = 50): List<CommandHistoryEntry> {
        val normalizedQuery = query.lowercase()
        return historyQueue.filter { entry ->
            entry.command.text.lowercase().contains(normalizedQuery) ||
            entry.command.id.lowercase().contains(normalizedQuery) ||
            entry.result.response?.lowercase()?.contains(normalizedQuery) == true
        }.takeLast(limit)
    }
    
    /**
     * Get command usage trends
     */
    fun getUsageTrends(hours: Int = 24): List<CommandUsageTrend> {
        val currentTime = System.currentTimeMillis()
        val startTime = currentTime - (hours * 60 * 60 * 1000L)
        val hourlyBuckets = mutableMapOf<Long, Int>()
        
        val relevantEntries = historyQueue.filter { it.timestamp >= startTime }
        
        for (entry in relevantEntries) {
            val hourBucket = (entry.timestamp / (60 * 60 * 1000L)) * (60 * 60 * 1000L)
            hourlyBuckets[hourBucket] = (hourlyBuckets[hourBucket] ?: 0) + 1
        }
        
        return hourlyBuckets.entries
            .sortedBy { it.key }
            .map { CommandUsageTrend(it.key, it.value) }
    }
    
    /**
     * Clear all history
     */
    fun clearHistory() {
        historyQueue.clear()
        commandFrequency.clear()
        commandLastUsed.clear()
        totalCommandCount.set(0)
        successfulCommandCount.set(0)
        failedCommandCount.set(0)
        android.util.Log.i(TAG, "History cleared")
    }
    
    /**
     * Configure retention policy
     */
    fun configureRetention(maxEntries: Int, maxAgeMs: Long, enabled: Boolean = true) {
        this.maxEntries = maxEntries
        this.maxAgeMs = maxAgeMs
        this.retentionEnabled = enabled
        
        if (enabled) {
            cleanupOldEntries()
        }
        
        android.util.Log.d(TAG, "Retention policy updated: maxEntries=$maxEntries, maxAge=${maxAgeMs}ms, enabled=$enabled")
    }
    
    /**
     * Export history to structured format
     */
    fun exportHistory(): CommandHistoryExport {
        return CommandHistoryExport(
            exportTimestamp = System.currentTimeMillis(),
            totalEntries = historyQueue.size,
            entries = historyQueue.toList(),
            statistics = getStatistics(),
            configuration = HistoryConfiguration(maxEntries, maxAgeMs, retentionEnabled)
        )
    }
    
    /**
     * Get current history size
     */
    fun getSize(): Int = historyQueue.size
    
    // Private methods
    
    /**
     * Clean up old entries based on retention policy
     */
    private fun cleanupOldEntries() {
        val currentTime = System.currentTimeMillis()
        val cutoffTime = currentTime - maxAgeMs
        
        // Remove entries older than max age
        val iterator = historyQueue.iterator()
        var removedByAge = 0
        while (iterator.hasNext()) {
            val entry = iterator.next()
            if (entry.timestamp < cutoffTime) {
                iterator.remove()
                removedByAge++
            }
        }
        
        // Remove excess entries beyond max count
        var removedByCount = 0
        while (historyQueue.size > maxEntries) {
            historyQueue.poll()
            removedByCount++
        }
        
        if (removedByAge > 0 || removedByCount > 0) {
            android.util.Log.d(TAG, "Cleaned up history: $removedByAge entries by age, $removedByCount by count")
        }
    }
    
    /**
     * Calculate average execution time
     */
    private fun calculateAverageExecutionTime(): Float {
        if (historyQueue.isEmpty()) return 0f
        
        val totalTime = historyQueue.sumOf { it.result.executionTime }
        return totalTime.toFloat() / historyQueue.size
    }
}

/**
 * Command history statistics
 */
data class CommandHistoryStatistics(
    val totalCommands: Long,
    val successfulCommands: Long,
    val failedCommands: Long,
    val totalEntries: Int,
    val commandsLast24Hours: Int,
    val commandsLastHour: Int,
    val successRateLast24Hours: Float,
    val averageExecutionTime: Float,
    val mostUsedCommands: List<CommandFrequency>,
    val recentlyUsedCommands: List<CommandLastUsed>
)

/**
 * Command frequency data
 */
data class CommandFrequency(
    val commandId: String,
    val count: Long
)

/**
 * Command last used data
 */
data class CommandLastUsed(
    val commandId: String,
    val lastUsedTimestamp: Long
)

/**
 * Command usage trend data
 */
data class CommandUsageTrend(
    val timeSlot: Long,
    val commandCount: Int
)

/**
 * History configuration
 */
data class HistoryConfiguration(
    val maxEntries: Int,
    val maxAgeMs: Long,
    val retentionEnabled: Boolean
)

/**
 * History export format
 */
data class CommandHistoryExport(
    val exportTimestamp: Long,
    val totalEntries: Int,
    val entries: List<CommandHistoryEntry>,
    val statistics: CommandHistoryStatistics,
    val configuration: HistoryConfiguration
)