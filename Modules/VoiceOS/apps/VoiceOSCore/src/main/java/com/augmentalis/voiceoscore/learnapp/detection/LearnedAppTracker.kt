/**
 * LearnedAppTracker.kt - Tracks learned apps status
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Created: 2025-12-17
 *
 * Tracks which apps have been learned and their learning status.
 */

package com.augmentalis.voiceoscore.learnapp.detection

import android.content.Context
import android.content.SharedPreferences

/**
 * Learned App Tracker
 *
 * Tracks which apps have been learned and their exploration status.
 * Uses SharedPreferences for persistence.
 */
class LearnedAppTracker(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )

    /**
     * Check if app has been fully learned
     */
    fun isFullyLearned(packageName: String): Boolean {
        return prefs.getBoolean("$KEY_FULLY_LEARNED_PREFIX$packageName", false)
    }

    /**
     * Mark app as fully learned
     */
    fun markFullyLearned(packageName: String) {
        prefs.edit()
            .putBoolean("$KEY_FULLY_LEARNED_PREFIX$packageName", true)
            .putLong("$KEY_LEARNED_AT_PREFIX$packageName", System.currentTimeMillis())
            .apply()
    }

    /**
     * Check if app is partially learned
     */
    fun isPartiallyLearned(packageName: String): Boolean {
        return prefs.getBoolean("$KEY_PARTIALLY_LEARNED_PREFIX$packageName", false)
    }

    /**
     * Mark app as partially learned
     */
    fun markPartiallyLearned(packageName: String) {
        prefs.edit()
            .putBoolean("$KEY_PARTIALLY_LEARNED_PREFIX$packageName", true)
            .apply()
    }

    /**
     * Check if app has any learning data
     */
    fun hasLearningData(packageName: String): Boolean {
        return isFullyLearned(packageName) || isPartiallyLearned(packageName)
    }

    /**
     * Get when app was learned
     */
    fun getLearnedAt(packageName: String): Long {
        return prefs.getLong("$KEY_LEARNED_AT_PREFIX$packageName", 0L)
    }

    /**
     * Get all learned packages
     */
    fun getAllLearnedPackages(): Set<String> {
        return prefs.all.keys
            .filter { it.startsWith(KEY_FULLY_LEARNED_PREFIX) }
            .map { it.removePrefix(KEY_FULLY_LEARNED_PREFIX) }
            .toSet()
    }

    /**
     * Clear learning status for app
     */
    fun clearLearningStatus(packageName: String) {
        prefs.edit()
            .remove("$KEY_FULLY_LEARNED_PREFIX$packageName")
            .remove("$KEY_PARTIALLY_LEARNED_PREFIX$packageName")
            .remove("$KEY_LEARNED_AT_PREFIX$packageName")
            .apply()
    }

    /**
     * Clear all learning data
     */
    fun clearAll() {
        prefs.edit().clear().apply()
    }

    /**
     * Check if app should be auto-detected for learning
     */
    fun shouldAutoDetect(packageName: String): Boolean {
        return !isFullyLearned(packageName) && !isExcluded(packageName)
    }

    /**
     * Check if app is excluded from learning
     */
    fun isExcluded(packageName: String): Boolean {
        val excluded = prefs.getStringSet(KEY_EXCLUDED_PACKAGES, emptySet()) ?: emptySet()
        return packageName in excluded
    }

    /**
     * Exclude app from learning
     */
    fun excludePackage(packageName: String) {
        val excluded = (prefs.getStringSet(KEY_EXCLUDED_PACKAGES, emptySet()) ?: emptySet()).toMutableSet()
        excluded.add(packageName)
        prefs.edit().putStringSet(KEY_EXCLUDED_PACKAGES, excluded).apply()
    }

    companion object {
        private const val PREFS_NAME = "learned_app_tracker"
        private const val KEY_FULLY_LEARNED_PREFIX = "fully_learned_"
        private const val KEY_PARTIALLY_LEARNED_PREFIX = "partially_learned_"
        private const val KEY_LEARNED_AT_PREFIX = "learned_at_"
        private const val KEY_EXCLUDED_PACKAGES = "excluded_packages"
    }
}
