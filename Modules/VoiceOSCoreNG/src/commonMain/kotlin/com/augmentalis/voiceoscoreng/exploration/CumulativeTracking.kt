/**
 * CumulativeTracking.kt - Thread-safe exploration progress tracking
 *
 * Tracks VUID discovery, clicks, and blocks across the entire exploration session.
 * Uses synchronized blocks for thread-safe access from multiple coroutines.
 *
 * @author Manoj Jhawar
 * @since 2026-01-15
 */

package com.augmentalis.voiceoscoreng.exploration

/**
 * Cumulative VUID tracking for exploration progress.
 *
 * Thread-safe tracking of:
 * - Discovered VUIDs (elements found)
 * - Clicked VUIDs (elements successfully clicked)
 * - Blocked VUIDs (critical dangerous elements skipped)
 *
 * Uses synchronized blocks for thread-safe set operations since KMP doesn't have
 * ConcurrentHashMap.newKeySet().
 */
class CumulativeTracking {
    private val lock = Any()

    /**
     * Set of all discovered VUIDs
     */
    private val _discoveredVuids = mutableSetOf<String>()

    /**
     * Set of clicked VUIDs
     */
    private val _clickedVuids = mutableSetOf<String>()

    /**
     * Set of blocked VUIDs (critical dangerous)
     */
    private val _blockedVuids = mutableSetOf<String>()

    // ═══════════════════════════════════════════════════════════════════
    // DISCOVERED VUIDs
    // ═══════════════════════════════════════════════════════════════════

    /**
     * Add a discovered VUID
     */
    fun addDiscovered(vuid: String) {
        synchronized(lock) { _discoveredVuids.add(vuid) }
    }

    /**
     * Add multiple discovered VUIDs
     */
    fun addAllDiscovered(vuids: Collection<String>) {
        synchronized(lock) { _discoveredVuids.addAll(vuids) }
    }

    /**
     * Get count of discovered VUIDs
     */
    fun discoveredCount(): Int = synchronized(lock) { _discoveredVuids.size }

    /**
     * Get copy of discovered VUIDs
     */
    fun getDiscoveredVuids(): Set<String> = synchronized(lock) { _discoveredVuids.toSet() }

    // ═══════════════════════════════════════════════════════════════════
    // CLICKED VUIDs
    // ═══════════════════════════════════════════════════════════════════

    /**
     * Add a clicked VUID
     */
    fun addClicked(vuid: String) {
        synchronized(lock) { _clickedVuids.add(vuid) }
    }

    /**
     * Get count of clicked VUIDs
     */
    fun clickedCount(): Int = synchronized(lock) { _clickedVuids.size }

    /**
     * Get copy of clicked VUIDs
     */
    fun getClickedVuids(): Set<String> = synchronized(lock) { _clickedVuids.toSet() }

    // ═══════════════════════════════════════════════════════════════════
    // BLOCKED VUIDs
    // ═══════════════════════════════════════════════════════════════════

    /**
     * Add a blocked VUID
     */
    fun addBlocked(vuid: String) {
        synchronized(lock) { _blockedVuids.add(vuid) }
    }

    /**
     * Add multiple blocked VUIDs
     */
    fun addAllBlocked(vuids: Collection<String>) {
        synchronized(lock) { _blockedVuids.addAll(vuids) }
    }

    /**
     * Get count of blocked VUIDs
     */
    fun blockedCount(): Int = synchronized(lock) { _blockedVuids.size }

    /**
     * Get copy of blocked VUIDs
     */
    fun getBlockedVuids(): Set<String> = synchronized(lock) { _blockedVuids.toSet() }

    // ═══════════════════════════════════════════════════════════════════
    // METRICS
    // ═══════════════════════════════════════════════════════════════════

    /**
     * Calculate exploration completeness as percentage.
     *
     * Completeness = (clicked / discovered) * 100
     * Only counts non-blocked elements.
     *
     * @return Percentage from 0.0 to 100.0
     */
    fun getCompleteness(): Float {
        return synchronized(lock) {
            if (_discoveredVuids.isEmpty()) {
                0f
            } else {
                (_clickedVuids.size.toFloat() / _discoveredVuids.size.toFloat()) * 100f
            }
        }
    }

    /**
     * Get summary statistics
     */
    fun getStats(): TrackingStats {
        return synchronized(lock) {
            TrackingStats(
                discovered = _discoveredVuids.size,
                clicked = _clickedVuids.size,
                blocked = _blockedVuids.size,
                completeness = getCompleteness()
            )
        }
    }

    /**
     * Clear all tracking data for a new session
     */
    fun clear() {
        synchronized(lock) {
            _discoveredVuids.clear()
            _clickedVuids.clear()
            _blockedVuids.clear()
        }
    }
}

/**
 * Summary statistics for tracking
 */
data class TrackingStats(
    val discovered: Int,
    val clicked: Int,
    val blocked: Int,
    val completeness: Float
) {
    override fun toString(): String {
        return "Stats: ${completeness.toInt()}% complete " +
               "($clicked/$discovered clicked, $blocked blocked)"
    }
}
