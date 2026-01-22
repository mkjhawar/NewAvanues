/**
 * CursorHistoryTracker.kt - Cursor position history and undo/redo support
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Code-Reviewed-By: CCA
 * Created: 2025-10-09
 */
package com.augmentalis.voiceoscore.accessibility.cursor

import android.graphics.PointF
import android.util.Log
import java.util.ArrayDeque
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * Represents a cursor position in history with optional description
 */
data class HistoricalCursorPosition(
    val position: CursorPosition,
    val description: String? = null
) {
    /**
     * Calculate distance to another position
     */
    fun distanceTo(other: HistoricalCursorPosition): Float {
        val dx = other.position.x - position.x
        val dy = other.position.y - position.y
        return sqrt(dx.pow(2) + dy.pow(2))
    }

    /**
     * Check if position is expired based on timeout
     */
    fun isExpired(timeoutMillis: Long): Boolean {
        return System.currentTimeMillis() - position.timestamp > timeoutMillis
    }

    /**
     * Convert to PointF
     */
    fun toPointF(): PointF = position.toPointF()

    /**
     * Get x coordinate
     */
    val x: Float get() = position.x

    /**
     * Get y coordinate
     */
    val y: Float get() = position.y

    /**
     * Get timestamp
     */
    val timestamp: Long get() = position.timestamp
}

/**
 * Tracker for cursor position history with undo/redo support
 *
 * Features:
 * - Bounded position stack (max 50 entries by default)
 * - Undo/redo navigation (go back/forward)
 * - Time-based expiration (5 minutes by default)
 * - Significant movement detection (>10px threshold)
 * - Memory-efficient ArrayDeque implementation
 * - Automatic cleanup of expired entries
 *
 * Usage:
 * ```
 * val tracker = CursorHistoryTracker()
 * tracker.recordPosition(100f, 200f)
 * val previous = tracker.undo()  // Go back to previous position
 * val next = tracker.redo()      // Go forward again
 * ```
 */
class CursorHistoryTracker(
    private val maxHistorySize: Int = DEFAULT_MAX_SIZE,
    private val expirationTimeMillis: Long = DEFAULT_EXPIRATION_TIME,
    private val significantMoveThreshold: Float = DEFAULT_MOVE_THRESHOLD
) {

    companion object {
        private const val TAG = "CursorHistoryTracker"

        // Default configuration
        const val DEFAULT_MAX_SIZE = 50  // Maximum history entries
        const val DEFAULT_EXPIRATION_TIME = 300_000L  // 5 minutes
        const val DEFAULT_MOVE_THRESHOLD = 10f  // 10 pixels minimum movement
    }

    // History stack (undo)
    private val historyStack = ArrayDeque<HistoricalCursorPosition>(maxHistorySize)

    // Future stack (redo)
    private val futureStack = ArrayDeque<HistoricalCursorPosition>(maxHistorySize)

    // Current position
    private var currentPosition: HistoricalCursorPosition? = null

    // Last recorded time (for rate limiting)
    private var lastRecordTime: Long = 0L

    // Statistics
    private var totalMoveCount: Long = 0L
    private var significantMoveCount: Long = 0L

    /**
     * Record a new cursor position
     *
     * @param position CursorPosition to record
     * @param description Optional description of this position
     * @param forceRecord Force recording even if movement is insignificant
     * @return true if position was recorded, false if skipped
     */
    fun recordPosition(
        position: CursorPosition,
        description: String? = null,
        forceRecord: Boolean = false
    ): Boolean {
        totalMoveCount++

        val newPosition = HistoricalCursorPosition(position, description)

        // Check if movement is significant
        val current = currentPosition
        if (!forceRecord && current != null) {
            val distance = current.distanceTo(newPosition)
            if (distance < significantMoveThreshold) {
                Log.v(TAG, "Skipping insignificant movement: ${distance}px < ${significantMoveThreshold}px")
                return false
            }
        }

        significantMoveCount++

        // Add current position to history
        if (current != null) {
            historyStack.addLast(current)

            // Enforce size limit
            while (historyStack.size > maxHistorySize) {
                historyStack.removeFirst()
            }
        }

        // Clear future stack (new action invalidates redo)
        if (futureStack.isNotEmpty()) {
            Log.d(TAG, "Clearing ${futureStack.size} redo entries (new position recorded)")
            futureStack.clear()
        }

        // Update current position
        currentPosition = newPosition
        lastRecordTime = System.currentTimeMillis()

        Log.v(TAG, "Recorded position: (${newPosition.x}, ${newPosition.y}) ${description ?: ""} [history: ${historyStack.size}]")

        // Cleanup expired entries
        cleanupExpiredEntries()

        return true
    }

    /**
     * Undo to previous position
     *
     * @return Previous position or null if no history
     */
    fun undo(): HistoricalCursorPosition? {
        if (historyStack.isEmpty()) {
            Log.d(TAG, "Cannot undo: history is empty")
            return null
        }

        val previous = historyStack.removeLast()

        // Move current to future stack
        currentPosition?.let { futureStack.addLast(it) }

        // Update current position
        currentPosition = previous

        Log.d(TAG, "Undo to position: (${previous.x}, ${previous.y}) [history: ${historyStack.size}, future: ${futureStack.size}]")

        return previous
    }

    /**
     * Redo to next position
     *
     * @return Next position or null if no future
     */
    fun redo(): HistoricalCursorPosition? {
        if (futureStack.isEmpty()) {
            Log.d(TAG, "Cannot redo: future is empty")
            return null
        }

        val next = futureStack.removeLast()

        // Move current to history stack
        currentPosition?.let { historyStack.addLast(it) }

        // Update current position
        currentPosition = next

        Log.d(TAG, "Redo to position: (${next.x}, ${next.y}) [history: ${historyStack.size}, future: ${futureStack.size}]")

        return next
    }

    /**
     * Get current position
     */
    fun getCurrentPosition(): HistoricalCursorPosition? = currentPosition

    /**
     * Check if undo is available
     */
    fun canUndo(): Boolean = historyStack.isNotEmpty()

    /**
     * Check if redo is available
     */
    fun canRedo(): Boolean = futureStack.isNotEmpty()

    /**
     * Get number of undo steps available
     */
    fun getUndoCount(): Int = historyStack.size

    /**
     * Get number of redo steps available
     */
    fun getRedoCount(): Int = futureStack.size

    /**
     * Peek at previous position without undoing
     */
    fun peekUndo(): HistoricalCursorPosition? = historyStack.lastOrNull()

    /**
     * Peek at next position without redoing
     */
    fun peekRedo(): HistoricalCursorPosition? = futureStack.lastOrNull()

    /**
     * Get recent history (last N positions)
     *
     * @param count Number of recent positions to retrieve
     * @return List of recent positions (most recent first)
     */
    fun getRecentHistory(count: Int = 10): List<HistoricalCursorPosition> {
        val result = mutableListOf<HistoricalCursorPosition>()

        // Add current position
        currentPosition?.let { result.add(it) }

        // Add from history stack (most recent first)
        val historyList = historyStack.toList()
        for (i in historyList.size - 1 downTo 0) {
            if (result.size >= count) break
            result.add(historyList[i])
        }

        return result
    }

    /**
     * Get all history positions (oldest to newest)
     */
    fun getAllHistory(): List<HistoricalCursorPosition> {
        return historyStack.toList() + listOfNotNull(currentPosition)
    }

    /**
     * Clear all history and future
     */
    fun clear() {
        Log.d(TAG, "Clearing all history (${historyStack.size} undo, ${futureStack.size} redo)")
        historyStack.clear()
        futureStack.clear()
        currentPosition = null
        totalMoveCount = 0L
        significantMoveCount = 0L
    }

    /**
     * Clear only future (redo) history
     */
    fun clearFuture() {
        Log.d(TAG, "Clearing future history (${futureStack.size} entries)")
        futureStack.clear()
    }

    /**
     * Cleanup expired entries from history
     *
     * @return Number of entries removed
     */
    fun cleanupExpiredEntries(): Int {
        var removedCount = 0

        // Clean history stack
        while (historyStack.isNotEmpty()) {
            val first = historyStack.first()
            if (first.isExpired(expirationTimeMillis)) {
                historyStack.removeFirst()
                removedCount++
            } else {
                break  // Stack is ordered by time, so stop when we hit non-expired
            }
        }

        // Clean future stack
        while (futureStack.isNotEmpty()) {
            val first = futureStack.first()
            if (first.isExpired(expirationTimeMillis)) {
                futureStack.removeFirst()
                removedCount++
            } else {
                break
            }
        }

        if (removedCount > 0) {
            Log.d(TAG, "Cleaned up $removedCount expired entries")
        }

        return removedCount
    }

    /**
     * Get time since last position was recorded
     */
    fun getTimeSinceLastRecord(): Long {
        return System.currentTimeMillis() - lastRecordTime
    }

    /**
     * Calculate total distance traveled in history
     */
    fun getTotalDistanceTraveled(): Float {
        if (historyStack.isEmpty()) return 0f

        var totalDistance = 0f
        val positions = getAllHistory()

        for (i in 0 until positions.size - 1) {
            totalDistance += positions[i].distanceTo(positions[i + 1])
        }

        return totalDistance
    }

    /**
     * Get statistics about history tracking
     */
    fun getStatistics(): HistoryStatistics {
        return HistoryStatistics(
            totalMoves = totalMoveCount,
            significantMoves = significantMoveCount,
            historySize = historyStack.size,
            futureSize = futureStack.size,
            totalDistance = getTotalDistanceTraveled(),
            oldestTimestamp = historyStack.firstOrNull()?.timestamp,
            newestTimestamp = currentPosition?.timestamp
        )
    }

    /**
     * Jump to specific position in history by index
     *
     * @param index Index in history (0 = oldest, size-1 = newest)
     * @return Position at index or null if invalid
     */
    fun jumpToIndex(index: Int): HistoricalCursorPosition? {
        if (index < 0 || index >= historyStack.size) {
            Log.w(TAG, "Invalid history index: $index (size: ${historyStack.size})")
            return null
        }

        val historyList = historyStack.toList()
        val targetPosition = historyList[index]

        // Move everything after index to future
        futureStack.clear()
        currentPosition?.let { futureStack.addFirst(it) }

        for (i in historyStack.size - 1 downTo index + 1) {
            futureStack.addFirst(historyStack.removeLast())
        }

        // Remove target and make it current
        historyStack.removeLast()
        currentPosition = targetPosition

        Log.d(TAG, "Jumped to index $index: (${targetPosition.x}, ${targetPosition.y})")

        return targetPosition
    }

    /**
     * Find position by description
     *
     * @param description Description to search for
     * @return First matching position or null
     */
    fun findPositionByDescription(description: String): HistoricalCursorPosition? {
        return getAllHistory().firstOrNull { it.description == description }
    }

    /**
     * Get positions within time range
     *
     * @param startTime Start timestamp
     * @param endTime End timestamp
     * @return List of positions within time range
     */
    fun getPositionsInTimeRange(startTime: Long, endTime: Long): List<HistoricalCursorPosition> {
        return getAllHistory().filter { it.timestamp in startTime..endTime }
    }
}

/**
 * Statistics about history tracking
 */
data class HistoryStatistics(
    val totalMoves: Long,
    val significantMoves: Long,
    val historySize: Int,
    val futureSize: Int,
    val totalDistance: Float,
    val oldestTimestamp: Long?,
    val newestTimestamp: Long?
) {
    /**
     * Get history duration in milliseconds
     */
    fun getHistoryDuration(): Long? {
        return if (oldestTimestamp != null && newestTimestamp != null) {
            newestTimestamp - oldestTimestamp
        } else {
            null
        }
    }

    /**
     * Get percentage of significant moves
     */
    fun getSignificantMovePercentage(): Float {
        return if (totalMoves > 0) {
            (significantMoves.toFloat() / totalMoves) * 100f
        } else {
            0f
        }
    }

    override fun toString(): String {
        return """
            |History Statistics:
            |  Total Moves: $totalMoves
            |  Significant Moves: $significantMoves (${String.format("%.1f", getSignificantMovePercentage())}%)
            |  History Size: $historySize
            |  Future Size: $futureSize
            |  Total Distance: ${String.format("%.1f", totalDistance)}px
            |  Duration: ${getHistoryDuration()?.let { "${it / 1000}s" } ?: "N/A"}
        """.trimMargin()
    }
}
