/**
 * LearnAppPreferences.kt - Preferences manager for LearnApp
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Created: 2025-12-17
 *
 * Manages user preferences and settings for LearnApp functionality.
 */

package com.augmentalis.voiceoscore.learnapp.settings

import android.content.Context
import android.content.SharedPreferences

/**
 * LearnApp Preferences
 *
 * Manages preferences for LearnApp exploration and learning features.
 */
class LearnAppPreferences(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )

    // ========== Exploration Settings ==========

    var isExplorationEnabled: Boolean
        get() = prefs.getBoolean(KEY_EXPLORATION_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_EXPLORATION_ENABLED, value).apply()

    var isJitLearningEnabled: Boolean
        get() = prefs.getBoolean(KEY_JIT_LEARNING_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_JIT_LEARNING_ENABLED, value).apply()

    var explorationDepthLimit: Int
        get() = prefs.getInt(KEY_EXPLORATION_DEPTH_LIMIT, DEFAULT_DEPTH_LIMIT)
        set(value) = prefs.edit().putInt(KEY_EXPLORATION_DEPTH_LIMIT, value).apply()

    var maxElementsPerScreen: Int
        get() = prefs.getInt(KEY_MAX_ELEMENTS_PER_SCREEN, DEFAULT_MAX_ELEMENTS)
        set(value) = prefs.edit().putInt(KEY_MAX_ELEMENTS_PER_SCREEN, value).apply()

    // ========== Timing Settings ==========

    var screenProcessingDelayMs: Long
        get() = prefs.getLong(KEY_SCREEN_PROCESSING_DELAY, DEFAULT_SCREEN_DELAY)
        set(value) = prefs.edit().putLong(KEY_SCREEN_PROCESSING_DELAY, value).apply()

    var scrollDelayMs: Long
        get() = prefs.getLong(KEY_SCROLL_DELAY, DEFAULT_SCROLL_DELAY)
        set(value) = prefs.edit().putLong(KEY_SCROLL_DELAY, value).apply()

    var clickDelayMs: Long
        get() = prefs.getLong(KEY_CLICK_DELAY, DEFAULT_CLICK_DELAY)
        set(value) = prefs.edit().putLong(KEY_CLICK_DELAY, value).apply()

    // ========== Auto-Detect Settings ==========

    /**
     * Check if auto-detect mode is enabled.
     * When enabled, LearnApp automatically detects app launches and shows consent dialogs.
     */
    fun isAutoDetectEnabled(): Boolean = prefs.getBoolean(KEY_AUTO_DETECT_ENABLED, true)

    /**
     * Enable or disable auto-detect mode.
     */
    fun setAutoDetectEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_AUTO_DETECT_ENABLED, enabled).apply()
    }

    // ========== Consent Settings ==========

    var hasShownGlobalConsent: Boolean
        get() = prefs.getBoolean(KEY_GLOBAL_CONSENT_SHOWN, false)
        set(value) = prefs.edit().putBoolean(KEY_GLOBAL_CONSENT_SHOWN, value).apply()

    var lastConsentTimestamp: Long
        get() = prefs.getLong(KEY_LAST_CONSENT_TIMESTAMP, 0L)
        set(value) = prefs.edit().putLong(KEY_LAST_CONSENT_TIMESTAMP, value).apply()

    // ========== Debug Settings ==========

    var isDebugModeEnabled: Boolean
        get() = prefs.getBoolean(KEY_DEBUG_MODE_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_DEBUG_MODE_ENABLED, value).apply()

    var showExplorationOverlay: Boolean
        get() = prefs.getBoolean(KEY_SHOW_EXPLORATION_OVERLAY, true)
        set(value) = prefs.edit().putBoolean(KEY_SHOW_EXPLORATION_OVERLAY, value).apply()

    // ========== Package-specific methods ==========

    fun isPackageExcluded(packageName: String): Boolean {
        val excluded = prefs.getStringSet(KEY_EXCLUDED_PACKAGES, emptySet()) ?: emptySet()
        return packageName in excluded
    }

    fun excludePackage(packageName: String) {
        val excluded = (prefs.getStringSet(KEY_EXCLUDED_PACKAGES, emptySet()) ?: emptySet()).toMutableSet()
        excluded.add(packageName)
        prefs.edit().putStringSet(KEY_EXCLUDED_PACKAGES, excluded).apply()
    }

    fun includePackage(packageName: String) {
        val excluded = (prefs.getStringSet(KEY_EXCLUDED_PACKAGES, emptySet()) ?: emptySet()).toMutableSet()
        excluded.remove(packageName)
        prefs.edit().putStringSet(KEY_EXCLUDED_PACKAGES, excluded).apply()
    }

    fun getExcludedPackages(): Set<String> {
        return prefs.getStringSet(KEY_EXCLUDED_PACKAGES, emptySet()) ?: emptySet()
    }

    // ========== Consent tracking ==========

    fun hasConsentForPackage(packageName: String): Boolean {
        return prefs.getBoolean("$KEY_PACKAGE_CONSENT_PREFIX$packageName", false)
    }

    fun setConsentForPackage(packageName: String, hasConsent: Boolean) {
        prefs.edit().putBoolean("$KEY_PACKAGE_CONSENT_PREFIX$packageName", hasConsent).apply()
    }

    fun clearAllPreferences() {
        prefs.edit().clear().apply()
    }

    companion object {
        private const val PREFS_NAME = "learnapp_preferences"

        // Keys
        private const val KEY_EXPLORATION_ENABLED = "exploration_enabled"
        private const val KEY_JIT_LEARNING_ENABLED = "jit_learning_enabled"
        private const val KEY_EXPLORATION_DEPTH_LIMIT = "exploration_depth_limit"
        private const val KEY_MAX_ELEMENTS_PER_SCREEN = "max_elements_per_screen"
        private const val KEY_SCREEN_PROCESSING_DELAY = "screen_processing_delay"
        private const val KEY_SCROLL_DELAY = "scroll_delay"
        private const val KEY_CLICK_DELAY = "click_delay"
        private const val KEY_GLOBAL_CONSENT_SHOWN = "global_consent_shown"
        private const val KEY_LAST_CONSENT_TIMESTAMP = "last_consent_timestamp"
        private const val KEY_DEBUG_MODE_ENABLED = "debug_mode_enabled"
        private const val KEY_SHOW_EXPLORATION_OVERLAY = "show_exploration_overlay"
        private const val KEY_EXCLUDED_PACKAGES = "excluded_packages"
        private const val KEY_PACKAGE_CONSENT_PREFIX = "consent_"
        private const val KEY_AUTO_DETECT_ENABLED = "auto_detect_enabled"

        // Defaults
        private const val DEFAULT_DEPTH_LIMIT = 10
        private const val DEFAULT_MAX_ELEMENTS = 500
        private const val DEFAULT_SCREEN_DELAY = 500L
        private const val DEFAULT_SCROLL_DELAY = 300L
        private const val DEFAULT_CLICK_DELAY = 200L
    }
}
