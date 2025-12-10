/**
 * ElementDiagnosticTracker.kt - Real-time diagnostic tracking for element decisions
 * Path: apps/VoiceOSCore/src/main/java/com/augmentalis/voiceoscore/learnapp/tracking/ElementDiagnosticTracker.kt
 *
 * Author: Manoj Jhawar
 * Code-Reviewed-By: CCA (Swarm Agent 2)
 * Created: 2025-12-08
 *
 * Thread-safe tracker that records WHY every element decision was made during exploration.
 * Provides real-time updates to overlay system and comprehensive diagnostic reports.
 */

package com.augmentalis.voiceoscore.learnapp.tracking

import android.util.Log
import com.augmentalis.voiceoscore.learnapp.models.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Element Diagnostic Tracker
 *
 * Tracks detailed diagnostic information for every element during exploration.
 * Answers the questions:
 * - Why was element X not clicked? (optimization reason)
 * - Why was element Y blocked? (dangerous pattern + reason)
 * - Which elements have VUIDs but weren't clicked?
 * - Why isn't the full app recognized for voice commands?
 *
 * ## Thread Safety
 * - Uses ConcurrentHashMap for diagnostic storage
 * - Uses CopyOnWriteArrayList for listeners
 * - All public methods are thread-safe
 *
 * ## Usage
 *
 * ```kotlin
 * val tracker = ElementDiagnosticTracker()
 *
 * // Record element decision
 * tracker.recordElementDecision(
 *     ElementDiagnostic(
 *         elementUuid = "abc123",
 *         screenHash = "screen-xyz",
 *         appId = "com.example.app",
 *         sessionId = "session-001",
 *         status = ElementStatus.BLOCKED,
 *         reason = ElementStatusReason.BLOCKED_CALL_ACTION,
 *         reasonDetail = "Matches pattern 'make.*call'",
 *         dangerousPattern = "make.*call",
 *         dangerousCategory = DangerousCategory.CRITICAL,
 *         elementText = "Make a call",
 *         discoveredAt = System.currentTimeMillis()
 *     )
 * )
 *
 * // Get session report
 * val report = tracker.getSessionReport("session-001")
 * Log.i(TAG, report.getSummaryText())
 *
 * // Listen for updates (for live overlay)
 * tracker.setLiveUpdateListener { diagnostic ->
 *     overlayService.updateElementStatus(diagnostic.elementUuid, diagnostic.status)
 * }
 * ```
 */
class ElementDiagnosticTracker {

    companion object {
        private const val TAG = "ElementDiagnosticTracker"
    }

    // ═══════════════════════════════════════════════════════════════
    // STORAGE
    // ═══════════════════════════════════════════════════════════════

    /**
     * Map: element UUID → diagnostic record
     * Thread-safe via ConcurrentHashMap
     */
    private val diagnosticsByUuid = ConcurrentHashMap<String, ElementDiagnostic>()

    /**
     * Map: session ID → list of element UUIDs
     * Thread-safe via ConcurrentHashMap + CopyOnWriteArrayList
     */
    private val sessionElements = ConcurrentHashMap<String, CopyOnWriteArrayList<String>>()

    /**
     * Map: screen hash → list of element UUIDs
     * Thread-safe via ConcurrentHashMap + CopyOnWriteArrayList
     */
    private val screenElements = ConcurrentHashMap<String, CopyOnWriteArrayList<String>>()

    // ═══════════════════════════════════════════════════════════════
    // LISTENERS
    // ═══════════════════════════════════════════════════════════════

    /**
     * Live update listener (called immediately when diagnostic recorded)
     * Used for real-time overlay updates
     */
    private var liveUpdateListener: ((ElementDiagnostic) -> Unit)? = null

    /**
     * Batch update listeners (called after multiple updates)
     * Used for UI refresh with debouncing
     */
    private val batchUpdateListeners = CopyOnWriteArrayList<(List<ElementDiagnostic>) -> Unit>()

    // ═══════════════════════════════════════════════════════════════
    // CORE METHODS
    // ═══════════════════════════════════════════════════════════════

    /**
     * Record element decision
     *
     * Records diagnostic info for an element. If element already has a diagnostic,
     * this will update it (useful for status changes like PENDING → CLICKED).
     *
     * @param diagnostic Diagnostic record to store
     */
    fun recordElementDecision(diagnostic: ElementDiagnostic) {
        // Store diagnostic
        diagnosticsByUuid[diagnostic.elementUuid] = diagnostic

        // Update session index
        sessionElements.computeIfAbsent(diagnostic.sessionId) {
            CopyOnWriteArrayList()
        }.addIfAbsent(diagnostic.elementUuid)

        // Update screen index
        screenElements.computeIfAbsent(diagnostic.screenHash) {
            CopyOnWriteArrayList()
        }.addIfAbsent(diagnostic.elementUuid)

        // Log
        Log.d(TAG, "📊 Recorded: ${diagnostic.toLogString()}")

        // Notify live listener (for real-time overlay)
        liveUpdateListener?.invoke(diagnostic)
    }

    /**
     * Get diagnostic for specific element
     *
     * @param elementUuid Element UUID
     * @return Diagnostic record or null if not found
     */
    fun getElementDiagnostic(elementUuid: String): ElementDiagnostic? {
        return diagnosticsByUuid[elementUuid]
    }

    /**
     * Get all diagnostics for a screen
     *
     * @param screenHash Screen hash
     * @return List of diagnostics (may be empty)
     */
    fun getScreenDiagnostics(screenHash: String): List<ElementDiagnostic> {
        val uuids = screenElements[screenHash] ?: return emptyList()
        return uuids.mapNotNull { diagnosticsByUuid[it] }
    }

    /**
     * Get all diagnostics for a session
     *
     * @param sessionId Session ID
     * @return List of diagnostics (may be empty)
     */
    fun getSessionDiagnostics(sessionId: String): List<ElementDiagnostic> {
        val uuids = sessionElements[sessionId] ?: return emptyList()
        return uuids.mapNotNull { diagnosticsByUuid[it] }
    }

    /**
     * Get session diagnostic report
     *
     * Aggregates all diagnostics for a session into a summary report.
     *
     * @param sessionId Session ID
     * @param startedAt Session start time (epoch ms)
     * @param completedAt Session completion time (epoch ms, null if incomplete)
     * @param completionReason Why session ended (optional)
     * @return Session diagnostic report
     */
    fun getSessionReport(
        sessionId: String,
        startedAt: Long = 0,
        completedAt: Long? = null,
        completionReason: String? = null
    ): SessionDiagnosticReport {
        val diagnostics = getSessionDiagnostics(sessionId)

        if (diagnostics.isEmpty()) {
            return SessionDiagnosticReport(
                sessionId = sessionId,
                appId = "",
                startedAt = startedAt,
                completedAt = completedAt,
                totalElements = 0,
                clickedCount = 0,
                blockedCount = 0,
                skippedCount = 0,
                pendingCount = 0,
                reasonCounts = emptyMap(),
                dangerousCategoryCounts = emptyMap(),
                diagnostics = emptyList(),
                completionReason = completionReason
            )
        }

        // Count by status
        val clickedCount = diagnostics.count { it.status == ElementStatus.CLICKED }
        val blockedCount = diagnostics.count { it.status == ElementStatus.BLOCKED }
        val skippedCount = diagnostics.count { it.status == ElementStatus.NOT_CLICKED }
        val pendingCount = diagnostics.count { it.status == ElementStatus.PENDING }

        // Count by reason
        val reasonCounts = diagnostics.groupingBy { it.reason }.eachCount()

        // Count by dangerous category (for blocked elements)
        val dangerousCategoryCounts = diagnostics
            .filter { it.dangerousCategory != null }
            .groupingBy { it.dangerousCategory!! }
            .eachCount()

        return SessionDiagnosticReport(
            sessionId = sessionId,
            appId = diagnostics.firstOrNull()?.appId ?: "",
            startedAt = startedAt,
            completedAt = completedAt,
            totalElements = diagnostics.size,
            clickedCount = clickedCount,
            blockedCount = blockedCount,
            skippedCount = skippedCount,
            pendingCount = pendingCount,
            reasonCounts = reasonCounts,
            dangerousCategoryCounts = dangerousCategoryCounts,
            diagnostics = diagnostics,
            completionReason = completionReason
        )
    }

    // ═══════════════════════════════════════════════════════════════
    // STATUS QUERIES
    // ═══════════════════════════════════════════════════════════════

    /**
     * Get counts by status
     *
     * @return Map of status → count
     */
    fun getStatusCounts(): Map<ElementStatus, Int> {
        return diagnosticsByUuid.values.groupingBy { it.status }.eachCount()
    }

    /**
     * Get counts by reason
     *
     * @return Map of reason → count
     */
    fun getReasonCounts(): Map<ElementStatusReason, Int> {
        return diagnosticsByUuid.values.groupingBy { it.reason }.eachCount()
    }

    /**
     * Get counts by dangerous category
     *
     * @return Map of category → count
     */
    fun getDangerousCategoryCounts(): Map<DangerousCategory, Int> {
        return diagnosticsByUuid.values
            .filter { it.dangerousCategory != null }
            .groupingBy { it.dangerousCategory!! }
            .eachCount()
    }

    /**
     * Get all blocked elements
     *
     * @return List of blocked element diagnostics
     */
    fun getBlockedElements(): List<ElementDiagnostic> {
        return diagnosticsByUuid.values.filter { it.status == ElementStatus.BLOCKED }
    }

    /**
     * Get all clicked elements
     *
     * @return List of clicked element diagnostics
     */
    fun getClickedElements(): List<ElementDiagnostic> {
        return diagnosticsByUuid.values.filter { it.status == ElementStatus.CLICKED }
    }

    /**
     * Get all skipped elements (not clicked due to optimization)
     *
     * @return List of skipped element diagnostics
     */
    fun getSkippedElements(): List<ElementDiagnostic> {
        return diagnosticsByUuid.values.filter { it.status == ElementStatus.NOT_CLICKED }
    }

    /**
     * Get elements with VUIDs but not clicked
     *
     * Includes both BLOCKED and NOT_CLICKED elements (both have VUIDs generated).
     *
     * @return List of element diagnostics with VUIDs but not clicked
     */
    fun getVuidButNotClickedElements(): List<ElementDiagnostic> {
        return diagnosticsByUuid.values.filter {
            it.status == ElementStatus.BLOCKED || it.status == ElementStatus.NOT_CLICKED
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // LISTENERS
    // ═══════════════════════════════════════════════════════════════

    /**
     * Set live update listener
     *
     * Called immediately when diagnostic is recorded. Used for real-time
     * overlay updates (paint elements as status changes).
     *
     * @param listener Callback receiving diagnostic record
     */
    fun setLiveUpdateListener(listener: ((ElementDiagnostic) -> Unit)?) {
        liveUpdateListener = listener
    }

    /**
     * Add batch update listener
     *
     * Called after multiple updates (debounced). Used for UI refresh.
     *
     * @param listener Callback receiving list of diagnostics
     */
    fun addBatchUpdateListener(listener: (List<ElementDiagnostic>) -> Unit) {
        batchUpdateListeners.add(listener)
    }

    /**
     * Remove batch update listener
     *
     * @param listener Listener to remove
     */
    fun removeBatchUpdateListener(listener: (List<ElementDiagnostic>) -> Unit) {
        batchUpdateListeners.remove(listener)
    }

    // ═══════════════════════════════════════════════════════════════
    // CLEANUP
    // ═══════════════════════════════════════════════════════════════

    /**
     * Clear session data
     *
     * Removes all diagnostics for a session. Call when session completes.
     *
     * @param sessionId Session ID to clear
     * @return Number of diagnostics cleared
     */
    fun clearSession(sessionId: String): Int {
        val uuids = sessionElements.remove(sessionId) ?: return 0
        var cleared = 0

        uuids.forEach { uuid ->
            diagnosticsByUuid.remove(uuid)?.let { cleared++ }
        }

        // Also remove from screen indices
        screenElements.values.forEach { it.removeAll(uuids) }

        Log.d(TAG, "🔄 Cleared session $sessionId: $cleared diagnostics")
        return cleared
    }

    /**
     * Clear all data
     *
     * Removes all diagnostic data. Call when starting new exploration
     * or when app exits.
     *
     * @return Number of diagnostics cleared
     */
    fun clearAll(): Int {
        val count = diagnosticsByUuid.size
        diagnosticsByUuid.clear()
        sessionElements.clear()
        screenElements.clear()
        Log.d(TAG, "🔄 Cleared all diagnostics: $count records")
        return count
    }

    // ═══════════════════════════════════════════════════════════════
    // STATISTICS
    // ═══════════════════════════════════════════════════════════════

    /**
     * Get overall statistics
     *
     * @return Map of statistic name → value
     */
    fun getStatistics(): Map<String, Any> {
        val total = diagnosticsByUuid.size
        val statusCounts = getStatusCounts()
        val reasonCounts = getReasonCounts()
        val dangerousCounts = getDangerousCategoryCounts()

        return mapOf(
            "total_elements" to total,
            "clicked" to (statusCounts[ElementStatus.CLICKED] ?: 0),
            "blocked" to (statusCounts[ElementStatus.BLOCKED] ?: 0),
            "skipped" to (statusCounts[ElementStatus.NOT_CLICKED] ?: 0),
            "pending" to (statusCounts[ElementStatus.PENDING] ?: 0),
            "sessions" to sessionElements.size,
            "screens" to screenElements.size,
            "reason_breakdown" to reasonCounts,
            "dangerous_breakdown" to dangerousCounts
        )
    }

    /**
     * Get diagnostic summary string (for logging)
     *
     * @return Human-readable summary
     */
    fun getSummaryString(): String {
        val stats = getStatistics()
        val total = stats["total_elements"] as Int
        val clicked = stats["clicked"] as Int
        val blocked = stats["blocked"] as Int
        val skipped = stats["skipped"] as Int

        return "Diagnostics: $total total | " +
                "✅ $clicked clicked | " +
                "🚫 $blocked blocked | " +
                "⏭️ $skipped skipped"
    }
}
