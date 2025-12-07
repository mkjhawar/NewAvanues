/**
 * LearnedAppTracker.kt - Tracks which apps have been learned
 * Path: libraries/UUIDCreator/src/main/java/com/augmentalis/learnapp/detection/LearnedAppTracker.kt
 *
 * Author: Manoj Jhawar
 * Code-Reviewed-By: CCA
 * Created: 2025-10-08
 *
 * Tracks learned apps and user consent decisions
 */

package com.augmentalis.voiceoscore.learnapp.detection

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Learned App Tracker
 *
 * Tracks which apps have been learned and which have been dismissed by user.
 * Uses SharedPreferences for persistent storage of quick lookups.
 *
 * ## Usage Example
 *
 * ```kotlin
 * val tracker = LearnedAppTracker(context)
 *
 * // Check if app is learned
 * val isLearned = tracker.isAppLearned("com.instagram.android")
 *
 * // Mark app as learned
 * tracker.markAsLearned("com.instagram.android", "Instagram")
 *
 * // Check if user dismissed
 * val wasDismissed = tracker.wasRecentlyDismissed("com.instagram.android")
 * ```
 *
 * ## Storage Format
 *
 * SharedPreferences keys:
 * - `learned_apps`: Set<String> of learned package names
 * - `dismissed_apps`: Set<String> of dismissed package names
 * - `dismissed_<packageName>_timestamp`: Long timestamp of dismissal
 *
 * @property context Application context
 *
 * @since 1.0.0
 */
class LearnedAppTracker(private val context: Context) {

    /**
     * SharedPreferences for persistent storage
     */
    private val prefs: SharedPreferences = context.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )

    /**
     * Mutex for thread-safe operations
     */
    private val mutex = Mutex()

    /**
     * In-memory cache of learned apps (for fast lookups)
     */
    private val learnedCache = mutableSetOf<String>()

    /**
     * In-memory cache of dismissed apps
     */
    private val dismissedCache = mutableSetOf<String>()

    init {
        // Load caches from SharedPreferences
        learnedCache.addAll(prefs.getStringSet(KEY_LEARNED_APPS, emptySet()) ?: emptySet())
        dismissedCache.addAll(prefs.getStringSet(KEY_DISMISSED_APPS, emptySet()) ?: emptySet())
    }

    /**
     * Check if app has been learned
     *
     * Fast O(1) lookup from in-memory cache.
     *
     * @param packageName Package name to check
     * @return true if app has been learned
     */
    fun isAppLearned(packageName: String): Boolean {
        return learnedCache.contains(packageName)
    }

    /**
     * Mark app as learned
     *
     * Updates both cache and persistent storage.
     *
     * @param packageName Package name to mark
     * @param appName Human-readable app name (optional)
     */
    suspend fun markAsLearned(packageName: String, @Suppress("UNUSED_PARAMETER") appName: String? = null) = mutex.withLock {
        // Add to cache
        learnedCache.add(packageName)

        // Save to SharedPreferences
        prefs.edit()
            .putStringSet(KEY_LEARNED_APPS, learnedCache)
            .apply()

        // Remove from dismissed (if it was dismissed before)
        if (dismissedCache.contains(packageName)) {
            dismissedCache.remove(packageName)
            prefs.edit()
                .putStringSet(KEY_DISMISSED_APPS, dismissedCache)
                .remove(getDismissedTimestampKey(packageName))
                .apply()
        }
    }

    /**
     * Mark app as dismissed (user declined consent)
     *
     * Prevents showing consent dialog again for 24 hours.
     *
     * @param packageName Package name to mark
     */
    suspend fun markAsDismissed(packageName: String) = mutex.withLock {
        // Add to cache
        dismissedCache.add(packageName)

        // Save to SharedPreferences with timestamp
        prefs.edit()
            .putStringSet(KEY_DISMISSED_APPS, dismissedCache)
            .putLong(getDismissedTimestampKey(packageName), System.currentTimeMillis())
            .apply()
    }

    /**
     * Check if app was recently dismissed (within 24 hours)
     *
     * @param packageName Package name to check
     * @return true if dismissed within last 24 hours
     */
    fun wasRecentlyDismissed(packageName: String): Boolean {
        if (!dismissedCache.contains(packageName)) {
            return false
        }

        val timestamp = prefs.getLong(getDismissedTimestampKey(packageName), 0L)
        if (timestamp == 0L) {
            return false
        }

        val now = System.currentTimeMillis()
        val elapsed = now - timestamp
        val twentyFourHours = 24 * 60 * 60 * 1000L

        return elapsed < twentyFourHours
    }

    /**
     * Clear dismissal for app (allow showing consent again)
     *
     * @param packageName Package name to clear
     */
    suspend fun clearDismissal(packageName: String) = mutex.withLock {
        dismissedCache.remove(packageName)

        prefs.edit()
            .putStringSet(KEY_DISMISSED_APPS, dismissedCache)
            .remove(getDismissedTimestampKey(packageName))
            .apply()
    }

    /**
     * Remove app from learned list (force re-learning)
     *
     * Useful when app is updated or user wants to re-learn.
     *
     * @param packageName Package name to remove
     */
    suspend fun unmarkAsLearned(packageName: String) = mutex.withLock {
        learnedCache.remove(packageName)

        prefs.edit()
            .putStringSet(KEY_LEARNED_APPS, learnedCache)
            .apply()
    }

    /**
     * Get all learned apps
     *
     * @return Set of learned package names
     */
    fun getAllLearnedApps(): Set<String> {
        return learnedCache.toSet()
    }

    /**
     * Get all dismissed apps (even if expired)
     *
     * @return Set of dismissed package names
     */
    fun getAllDismissedApps(): Set<String> {
        return dismissedCache.toSet()
    }

    /**
     * Clear all learned apps (for testing or reset)
     */
    suspend fun clearAllLearned() = mutex.withLock {
        learnedCache.clear()

        prefs.edit()
            .putStringSet(KEY_LEARNED_APPS, emptySet())
            .apply()
    }

    /**
     * Clear all dismissed apps (for testing or reset)
     */
    suspend fun clearAllDismissed() = mutex.withLock {
        // Remove all timestamp keys
        val editor = prefs.edit()
        dismissedCache.forEach { packageName ->
            editor.remove(getDismissedTimestampKey(packageName))
        }

        dismissedCache.clear()

        editor.putStringSet(KEY_DISMISSED_APPS, emptySet())
            .apply()
    }

    /**
     * Get SharedPreferences key for dismissal timestamp
     *
     * @param packageName Package name
     * @return SharedPreferences key
     */
    private fun getDismissedTimestampKey(packageName: String): String {
        return "${KEY_DISMISSED_PREFIX}${packageName}_timestamp"
    }

    /**
     * Get statistics
     *
     * @return Tracker statistics
     */
    fun getStats(): TrackerStats {
        val activeDismissals = dismissedCache.count { packageName ->
            wasRecentlyDismissed(packageName)
        }

        return TrackerStats(
            totalLearned = learnedCache.size,
            totalDismissed = dismissedCache.size,
            activeDismissals = activeDismissals
        )
    }

    companion object {
        /**
         * SharedPreferences file name
         */
        private const val PREFS_NAME = "learnapp_tracker"

        /**
         * Key for learned apps set
         */
        private const val KEY_LEARNED_APPS = "learned_apps"

        /**
         * Key for dismissed apps set
         */
        private const val KEY_DISMISSED_APPS = "dismissed_apps"

        /**
         * Prefix for dismissed timestamp keys
         */
        private const val KEY_DISMISSED_PREFIX = "dismissed_"
    }
}

/**
 * Tracker Statistics
 *
 * @property totalLearned Total number of learned apps
 * @property totalDismissed Total number of dismissed apps (all time)
 * @property activeDismissals Number of dismissed apps still within 24-hour window
 */
data class TrackerStats(
    val totalLearned: Int,
    val totalDismissed: Int,
    val activeDismissals: Int
) {
    override fun toString(): String {
        return """
            LearnApp Tracker Stats:
            - Learned Apps: $totalLearned
            - Dismissed Apps: $totalDismissed
            - Active Dismissals: $activeDismissals
        """.trimIndent()
    }
}
