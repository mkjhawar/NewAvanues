/**
 * RefactoringFeatureFlags.kt - Feature flag management for VoiceOSService refactoring
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Code-Reviewed-By: CCA
 * Created: 2025-10-15
 *
 * Manages feature flags for gradual rollout of refactored VoiceOSService implementation.
 * Supports percentage-based rollout, user ID targeting, and runtime configuration.
 */
package com.augmentalis.voiceoscore.accessibility.refactoring

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

/**
 * Feature flag system for VoiceOSService refactoring
 *
 * Capabilities:
 * - Percentage-based rollout (0-100%)
 * - User ID whitelist/blacklist
 * - Runtime enable/disable
 * - Thread-safe operations
 * - Persistent storage
 *
 * Rollout Strategy:
 * 1. Phase 1: 0% (development only)
 * 2. Phase 2: 1% (canary users)
 * 3. Phase 3: 10% (early adopters)
 * 4. Phase 4: 50% (general rollout)
 * 5. Phase 5: 100% (full migration)
 */
class RefactoringFeatureFlags private constructor(
    private val context: Context
) {

    companion object {
        private const val TAG = "RefactoringFeatureFlags"
        private const val PREFS_NAME = "voiceos_refactoring_flags"

        // Flag keys
        private const val KEY_ENABLED = "refactored_enabled"
        private const val KEY_ROLLOUT_PERCENTAGE = "rollout_percentage"
        private const val KEY_FORCE_LEGACY = "force_legacy"
        private const val KEY_FORCE_REFACTORED = "force_refactored"
        private const val KEY_COMPARISON_MODE = "comparison_mode"
        private const val KEY_AUTO_ROLLBACK_ENABLED = "auto_rollback_enabled"
        private const val KEY_WHITELIST_USERS = "whitelist_users"
        private const val KEY_BLACKLIST_USERS = "blacklist_users"

        // Default values
        private const val DEFAULT_ROLLOUT_PERCENTAGE = 0
        private const val DEFAULT_COMPARISON_MODE = true
        private const val DEFAULT_AUTO_ROLLBACK = true

        @Volatile
        private var instance: RefactoringFeatureFlags? = null

        /**
         * Get singleton instance
         */
        fun getInstance(context: Context): RefactoringFeatureFlags {
            return instance ?: synchronized(this) {
                instance ?: RefactoringFeatureFlags(context.applicationContext).also {
                    instance = it
                }
            }
        }
    }

    // Shared preferences for persistent storage
    private val prefs: SharedPreferences = context.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )

    // Atomic flags for thread-safe access
    private val refactoredEnabled = AtomicBoolean(
        prefs.getBoolean(KEY_ENABLED, false)
    )

    private val rolloutPercentage = AtomicInteger(
        prefs.getInt(KEY_ROLLOUT_PERCENTAGE, DEFAULT_ROLLOUT_PERCENTAGE)
    )

    private val forceLegacy = AtomicBoolean(
        prefs.getBoolean(KEY_FORCE_LEGACY, false)
    )

    private val forceRefactored = AtomicBoolean(
        prefs.getBoolean(KEY_FORCE_REFACTORED, false)
    )

    private val comparisonMode = AtomicBoolean(
        prefs.getBoolean(KEY_COMPARISON_MODE, DEFAULT_COMPARISON_MODE)
    )

    private val autoRollbackEnabled = AtomicBoolean(
        prefs.getBoolean(KEY_AUTO_ROLLBACK_ENABLED, DEFAULT_AUTO_ROLLBACK)
    )

    // Whitelist/blacklist (stored as comma-separated strings)
    private val whitelistedUsers: Set<String> by lazy {
        prefs.getString(KEY_WHITELIST_USERS, "")
            ?.split(",")
            ?.filter { it.isNotBlank() }
            ?.toSet() ?: emptySet()
    }

    private val blacklistedUsers: Set<String> by lazy {
        prefs.getString(KEY_BLACKLIST_USERS, "")
            ?.split(",")
            ?.filter { it.isNotBlank() }
            ?.toSet() ?: emptySet()
    }

    /**
     * Check if refactored implementation should be used
     *
     * Decision Logic:
     * 1. If forceLegacy is true → use legacy
     * 2. If forceRefactored is true → use refactored
     * 3. If user is blacklisted → use legacy
     * 4. If user is whitelisted → use refactored
     * 5. If enabled AND within rollout percentage → use refactored
     * 6. Default → use legacy
     *
     * @param userId Optional user ID for targeting
     * @return true if refactored should be used, false for legacy
     */
    fun shouldUseRefactored(userId: String? = null): Boolean {
        // Check force flags first
        if (forceLegacy.get()) {
            Log.d(TAG, "Using legacy (force_legacy enabled)")
            return false
        }

        if (forceRefactored.get()) {
            Log.d(TAG, "Using refactored (force_refactored enabled)")
            return true
        }

        // Check blacklist/whitelist
        if (userId != null) {
            if (userId in blacklistedUsers) {
                Log.d(TAG, "Using legacy (user $userId is blacklisted)")
                return false
            }

            if (userId in whitelistedUsers) {
                Log.d(TAG, "Using refactored (user $userId is whitelisted)")
                return true
            }
        }

        // Check if refactored is enabled
        if (!refactoredEnabled.get()) {
            Log.d(TAG, "Using legacy (refactored not enabled)")
            return false
        }

        // Check rollout percentage
        val percentage = rolloutPercentage.get()
        if (percentage == 0) {
            Log.d(TAG, "Using legacy (rollout percentage is 0%)")
            return false
        }

        if (percentage >= 100) {
            Log.d(TAG, "Using refactored (rollout percentage is 100%)")
            return true
        }

        // Percentage-based decision (deterministic hash-based)
        val hash = userId?.hashCode() ?: System.currentTimeMillis().hashCode()
        val bucket = (hash.and(Int.MAX_VALUE) % 100) + 1 // 1-100
        val shouldUse = bucket <= percentage

        Log.d(TAG, "Rollout decision: bucket=$bucket, percentage=$percentage, useRefactored=$shouldUse")
        return shouldUse
    }

    /**
     * Check if comparison mode is enabled
     * When true, both implementations run and results are compared
     */
    fun isComparisonModeEnabled(): Boolean = comparisonMode.get()

    /**
     * Check if automatic rollback is enabled
     * When true, automatically switch to legacy on divergence
     */
    fun isAutoRollbackEnabled(): Boolean = autoRollbackEnabled.get()

    /**
     * Enable refactored implementation
     */
    fun enableRefactored() {
        refactoredEnabled.set(true)
        prefs.edit().putBoolean(KEY_ENABLED, true).apply()
        Log.i(TAG, "Refactored implementation ENABLED")
    }

    /**
     * Disable refactored implementation (use legacy)
     */
    fun disableRefactored() {
        refactoredEnabled.set(false)
        prefs.edit().putBoolean(KEY_ENABLED, false).apply()
        Log.i(TAG, "Refactored implementation DISABLED")
    }

    /**
     * Set rollout percentage (0-100)
     */
    fun setRolloutPercentage(percentage: Int) {
        require(percentage in 0..100) { "Percentage must be between 0 and 100" }
        rolloutPercentage.set(percentage)
        prefs.edit().putInt(KEY_ROLLOUT_PERCENTAGE, percentage).apply()
        Log.i(TAG, "Rollout percentage set to $percentage%")
    }

    /**
     * Get current rollout percentage
     */
    fun getRolloutPercentage(): Int = rolloutPercentage.get()

    /**
     * Force legacy implementation (overrides all other flags)
     */
    fun setForceLegacy(force: Boolean) {
        forceLegacy.set(force)
        prefs.edit().putBoolean(KEY_FORCE_LEGACY, force).apply()
        Log.i(TAG, "Force legacy: $force")
    }

    /**
     * Force refactored implementation (overrides all other flags except forceLegacy)
     */
    fun setForceRefactored(force: Boolean) {
        forceRefactored.set(force)
        prefs.edit().putBoolean(KEY_FORCE_REFACTORED, force).apply()
        Log.i(TAG, "Force refactored: $force")
    }

    /**
     * Enable/disable comparison mode
     */
    fun setComparisonMode(enabled: Boolean) {
        comparisonMode.set(enabled)
        prefs.edit().putBoolean(KEY_COMPARISON_MODE, enabled).apply()
        Log.i(TAG, "Comparison mode: $enabled")
    }

    /**
     * Enable/disable automatic rollback
     */
    fun setAutoRollback(enabled: Boolean) {
        autoRollbackEnabled.set(enabled)
        prefs.edit().putBoolean(KEY_AUTO_ROLLBACK_ENABLED, enabled).apply()
        Log.i(TAG, "Auto rollback: $enabled")
    }

    /**
     * Add user to whitelist
     */
    fun addToWhitelist(userId: String) {
        val updated = whitelistedUsers + userId
        prefs.edit().putString(KEY_WHITELIST_USERS, updated.joinToString(",")).apply()
        Log.i(TAG, "User $userId added to whitelist")
    }

    /**
     * Remove user from whitelist
     */
    fun removeFromWhitelist(userId: String) {
        val updated = whitelistedUsers - userId
        prefs.edit().putString(KEY_WHITELIST_USERS, updated.joinToString(",")).apply()
        Log.i(TAG, "User $userId removed from whitelist")
    }

    /**
     * Add user to blacklist
     */
    fun addToBlacklist(userId: String) {
        val updated = blacklistedUsers + userId
        prefs.edit().putString(KEY_BLACKLIST_USERS, updated.joinToString(",")).apply()
        Log.i(TAG, "User $userId added to blacklist")
    }

    /**
     * Remove user from blacklist
     */
    fun removeFromBlacklist(userId: String) {
        val updated = blacklistedUsers - userId
        prefs.edit().putString(KEY_BLACKLIST_USERS, updated.joinToString(",")).apply()
        Log.i(TAG, "User $userId removed from blacklist")
    }

    /**
     * Get current configuration snapshot
     */
    fun getConfigSnapshot(): Map<String, Any> {
        return mapOf(
            "refactoredEnabled" to refactoredEnabled.get(),
            "rolloutPercentage" to rolloutPercentage.get(),
            "forceLegacy" to forceLegacy.get(),
            "forceRefactored" to forceRefactored.get(),
            "comparisonMode" to comparisonMode.get(),
            "autoRollbackEnabled" to autoRollbackEnabled.get(),
            "whitelistedUsers" to whitelistedUsers.size,
            "blacklistedUsers" to blacklistedUsers.size
        )
    }

    /**
     * Reset all flags to defaults
     */
    fun resetToDefaults() {
        prefs.edit().clear().apply()
        refactoredEnabled.set(false)
        rolloutPercentage.set(DEFAULT_ROLLOUT_PERCENTAGE)
        forceLegacy.set(false)
        forceRefactored.set(false)
        comparisonMode.set(DEFAULT_COMPARISON_MODE)
        autoRollbackEnabled.set(DEFAULT_AUTO_ROLLBACK)
        Log.i(TAG, "Feature flags reset to defaults")
    }
}
