/**
 * LearnAppDeveloperSettings.kt - Developer settings for LearnApp
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Created: 2025-12-16
 *
 * Manages developer-specific settings for LearnApp debugging and testing.
 * These settings are only available in debug builds or developer mode.
 */
package com.augmentalis.voiceoscore.learnapp.settings

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * LearnApp Developer Settings
 *
 * Manages debug and developer settings for LearnApp.
 * Settings are persisted in SharedPreferences.
 *
 * @param context Application context
 */
class LearnAppDeveloperSettings(private val context: Context) {

    companion object {
        private const val TAG = "LearnAppDevSettings"
        private const val PREFS_NAME = "learnapp_developer_settings"

        // Setting keys
        private const val KEY_DEVELOPER_MODE_ENABLED = "developer_mode_enabled"
        private const val KEY_DEBUG_OVERLAY_ENABLED = "debug_overlay_enabled"
        private const val KEY_VERBOSE_LOGGING = "verbose_logging"
        private const val KEY_EXPLORATION_STEP_DELAY = "exploration_step_delay"
        private const val KEY_SHOW_ELEMENT_BOUNDS = "show_element_bounds"
        private const val KEY_SHOW_VUID_LABELS = "show_vuid_labels"
        private const val KEY_LOG_ACCESSIBILITY_EVENTS = "log_accessibility_events"
        private const val KEY_SAVE_SCREEN_DUMPS = "save_screen_dumps"
        private const val KEY_NEO4J_EXPORT_ENABLED = "neo4j_export_enabled"
        private const val KEY_METRICS_COLLECTION_ENABLED = "metrics_collection_enabled"
        private const val KEY_BATCH_SIZE = "batch_size"
        private const val KEY_MAX_EXPLORATION_DEPTH = "max_exploration_depth"
        private const val KEY_ELEMENT_TIMEOUT_MS = "element_timeout_ms"
        private const val KEY_MOCK_DATA_ENABLED = "mock_data_enabled"

        // Default values
        private const val DEFAULT_EXPLORATION_STEP_DELAY = 500L
        private const val DEFAULT_BATCH_SIZE = 50
        private const val DEFAULT_MAX_EXPLORATION_DEPTH = 10
        private const val DEFAULT_ELEMENT_TIMEOUT_MS = 5000L
        private const val DEFAULT_SCREEN_PROCESSING_DELAY_MS = 500L
        private const val DEFAULT_SCROLL_DELAY_MS = 300L
        private const val DEFAULT_CLICK_DELAY_MS = 200L
        private const val DEFAULT_MIN_ALIAS_TEXT_LENGTH = 2
        private const val DEFAULT_ESTIMATED_INITIAL_SCREEN_COUNT = 10
        private const val DEFAULT_COMPLETENESS_THRESHOLD_PERCENT = 80.0
        private const val DEFAULT_EXPLORATION_TIMEOUT_MS = 300000L // 5 minutes
        private const val DEFAULT_BOUNDS_TOLERANCE_PIXELS = 10
        private const val DEFAULT_MAX_CONSECUTIVE_CLICK_FAILURES = 3
        private const val DEFAULT_MIN_LABEL_LENGTH = 1
        private const val DEFAULT_MIN_GENERATED_LABEL_LENGTH = 2
        private const val DEFAULT_MAX_COMMAND_BATCH_SIZE = 100
    }

    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    // State flows for reactive updates
    private val _developerModeEnabled = MutableStateFlow(prefs.getBoolean(KEY_DEVELOPER_MODE_ENABLED, false))
    val developerModeEnabled: StateFlow<Boolean> = _developerModeEnabled.asStateFlow()

    private val _debugOverlayEnabled = MutableStateFlow(prefs.getBoolean(KEY_DEBUG_OVERLAY_ENABLED, false))
    val debugOverlayEnabled: StateFlow<Boolean> = _debugOverlayEnabled.asStateFlow()

    /**
     * Check if developer mode is enabled.
     */
    fun isDeveloperModeEnabled(): Boolean {
        return prefs.getBoolean(KEY_DEVELOPER_MODE_ENABLED, false)
    }

    /**
     * Enable or disable developer mode.
     */
    fun setDeveloperModeEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_DEVELOPER_MODE_ENABLED, enabled).apply()
        _developerModeEnabled.value = enabled
        Log.i(TAG, "Developer mode ${if (enabled) "enabled" else "disabled"}")
    }

    /**
     * Check if debug overlay is enabled.
     */
    fun isDebugOverlayEnabled(): Boolean {
        return prefs.getBoolean(KEY_DEBUG_OVERLAY_ENABLED, false)
    }

    /**
     * Enable or disable debug overlay.
     */
    fun setDebugOverlayEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_DEBUG_OVERLAY_ENABLED, enabled).apply()
        _debugOverlayEnabled.value = enabled
    }

    /**
     * Check if verbose logging is enabled.
     */
    fun isVerboseLoggingEnabled(): Boolean {
        return prefs.getBoolean(KEY_VERBOSE_LOGGING, false)
    }

    /**
     * Enable or disable verbose logging.
     */
    fun setVerboseLoggingEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_VERBOSE_LOGGING, enabled).apply()
    }

    /**
     * Get exploration step delay in milliseconds.
     */
    fun getExplorationStepDelay(): Long {
        return prefs.getLong(KEY_EXPLORATION_STEP_DELAY, DEFAULT_EXPLORATION_STEP_DELAY)
    }

    /**
     * Set exploration step delay.
     */
    fun setExplorationStepDelay(delayMs: Long) {
        prefs.edit().putLong(KEY_EXPLORATION_STEP_DELAY, delayMs.coerceIn(0, 10000)).apply()
    }

    /**
     * Check if element bounds visualization is enabled.
     */
    fun isShowElementBoundsEnabled(): Boolean {
        return prefs.getBoolean(KEY_SHOW_ELEMENT_BOUNDS, false)
    }

    /**
     * Enable or disable element bounds visualization.
     */
    fun setShowElementBoundsEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SHOW_ELEMENT_BOUNDS, enabled).apply()
    }

    /**
     * Check if VUID labels are shown.
     */
    fun isShowVuidLabelsEnabled(): Boolean {
        return prefs.getBoolean(KEY_SHOW_VUID_LABELS, false)
    }

    /**
     * Enable or disable VUID label display.
     */
    fun setShowVuidLabelsEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SHOW_VUID_LABELS, enabled).apply()
    }

    /**
     * Check if accessibility event logging is enabled.
     */
    fun isLogAccessibilityEventsEnabled(): Boolean {
        return prefs.getBoolean(KEY_LOG_ACCESSIBILITY_EVENTS, false)
    }

    /**
     * Enable or disable accessibility event logging.
     */
    fun setLogAccessibilityEventsEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_LOG_ACCESSIBILITY_EVENTS, enabled).apply()
    }

    /**
     * Check if screen dump saving is enabled.
     */
    fun isSaveScreenDumpsEnabled(): Boolean {
        return prefs.getBoolean(KEY_SAVE_SCREEN_DUMPS, false)
    }

    /**
     * Enable or disable screen dump saving.
     */
    fun setSaveScreenDumpsEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SAVE_SCREEN_DUMPS, enabled).apply()
    }

    /**
     * Check if Neo4j export is enabled.
     */
    fun isNeo4jExportEnabled(): Boolean {
        return prefs.getBoolean(KEY_NEO4J_EXPORT_ENABLED, false)
    }

    /**
     * Enable or disable Neo4j export.
     */
    fun setNeo4jExportEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_NEO4J_EXPORT_ENABLED, enabled).apply()
    }

    /**
     * Check if metrics collection is enabled.
     */
    fun isMetricsCollectionEnabled(): Boolean {
        return prefs.getBoolean(KEY_METRICS_COLLECTION_ENABLED, true)
    }

    /**
     * Enable or disable metrics collection.
     */
    fun setMetricsCollectionEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_METRICS_COLLECTION_ENABLED, enabled).apply()
    }

    /**
     * Get batch size for database operations.
     */
    fun getBatchSize(): Int {
        return prefs.getInt(KEY_BATCH_SIZE, DEFAULT_BATCH_SIZE)
    }

    /**
     * Set batch size for database operations.
     */
    fun setBatchSize(size: Int) {
        prefs.edit().putInt(KEY_BATCH_SIZE, size.coerceIn(10, 200)).apply()
    }

    /**
     * Get maximum exploration depth.
     */
    fun getMaxExplorationDepth(): Int {
        return prefs.getInt(KEY_MAX_EXPLORATION_DEPTH, DEFAULT_MAX_EXPLORATION_DEPTH)
    }

    /**
     * Set maximum exploration depth.
     */
    fun setMaxExplorationDepth(depth: Int) {
        prefs.edit().putInt(KEY_MAX_EXPLORATION_DEPTH, depth.coerceIn(1, 50)).apply()
    }

    /**
     * Get element timeout in milliseconds.
     */
    fun getElementTimeoutMs(): Long {
        return prefs.getLong(KEY_ELEMENT_TIMEOUT_MS, DEFAULT_ELEMENT_TIMEOUT_MS)
    }

    /**
     * Set element timeout.
     */
    fun setElementTimeoutMs(timeoutMs: Long) {
        prefs.edit().putLong(KEY_ELEMENT_TIMEOUT_MS, timeoutMs.coerceIn(1000, 30000)).apply()
    }

    /**
     * Check if mock data is enabled (for testing).
     */
    fun isMockDataEnabled(): Boolean {
        return prefs.getBoolean(KEY_MOCK_DATA_ENABLED, false)
    }

    /**
     * Enable or disable mock data.
     */
    fun setMockDataEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_MOCK_DATA_ENABLED, enabled).apply()
    }

    // ========== Exploration timing settings ==========

    /**
     * Get screen processing delay in milliseconds.
     */
    fun getScreenProcessingDelayMs(): Long = DEFAULT_SCREEN_PROCESSING_DELAY_MS

    /**
     * Get scroll delay in milliseconds.
     */
    fun getScrollDelayMs(): Long = DEFAULT_SCROLL_DELAY_MS

    /**
     * Get click delay in milliseconds.
     */
    fun getClickDelayMs(): Long = DEFAULT_CLICK_DELAY_MS

    /**
     * Get minimum alias text length.
     */
    fun getMinAliasTextLength(): Int = DEFAULT_MIN_ALIAS_TEXT_LENGTH

    /**
     * Get estimated initial screen count for progress calculation.
     */
    fun getEstimatedInitialScreenCount(): Int = DEFAULT_ESTIMATED_INITIAL_SCREEN_COUNT

    /**
     * Get completeness threshold percentage.
     */
    fun getCompletenessThresholdPercent(): Double = DEFAULT_COMPLETENESS_THRESHOLD_PERCENT

    /**
     * Get exploration timeout in milliseconds.
     */
    fun getExplorationTimeoutMs(): Long = DEFAULT_EXPLORATION_TIMEOUT_MS

    /**
     * Get bounds tolerance in pixels for element matching.
     */
    fun getBoundsTolerancePixels(): Int = DEFAULT_BOUNDS_TOLERANCE_PIXELS

    /**
     * Get maximum consecutive click failures before giving up.
     */
    fun getMaxConsecutiveClickFailures(): Int = DEFAULT_MAX_CONSECUTIVE_CLICK_FAILURES

    /**
     * Get minimum label length for valid labels.
     */
    fun getMinLabelLength(): Int = DEFAULT_MIN_LABEL_LENGTH

    /**
     * Get minimum generated label length.
     */
    fun getMinGeneratedLabelLength(): Int = DEFAULT_MIN_GENERATED_LABEL_LENGTH

    /**
     * Get maximum command batch size for processing.
     */
    fun getMaxCommandBatchSize(): Int = DEFAULT_MAX_COMMAND_BATCH_SIZE

    /**
     * Check if aggressive fallback mode is needed.
     */
    fun needsAggressiveFallback(): Boolean = false

    /**
     * Check if moderate fallback mode is needed.
     */
    fun needsModerateFallback(): Boolean = false

    /**
     * Reset all developer settings to defaults.
     */
    fun resetToDefaults() {
        prefs.edit().clear().apply()
        _developerModeEnabled.value = false
        _debugOverlayEnabled.value = false
        Log.i(TAG, "Developer settings reset to defaults")
    }

    /**
     * Get all settings as a map (for debugging/export).
     */
    fun getAllSettings(): Map<String, Any> {
        return mapOf(
            KEY_DEVELOPER_MODE_ENABLED to isDeveloperModeEnabled(),
            KEY_DEBUG_OVERLAY_ENABLED to isDebugOverlayEnabled(),
            KEY_VERBOSE_LOGGING to isVerboseLoggingEnabled(),
            KEY_EXPLORATION_STEP_DELAY to getExplorationStepDelay(),
            KEY_SHOW_ELEMENT_BOUNDS to isShowElementBoundsEnabled(),
            KEY_SHOW_VUID_LABELS to isShowVuidLabelsEnabled(),
            KEY_LOG_ACCESSIBILITY_EVENTS to isLogAccessibilityEventsEnabled(),
            KEY_SAVE_SCREEN_DUMPS to isSaveScreenDumpsEnabled(),
            KEY_NEO4J_EXPORT_ENABLED to isNeo4jExportEnabled(),
            KEY_METRICS_COLLECTION_ENABLED to isMetricsCollectionEnabled(),
            KEY_BATCH_SIZE to getBatchSize(),
            KEY_MAX_EXPLORATION_DEPTH to getMaxExplorationDepth(),
            KEY_ELEMENT_TIMEOUT_MS to getElementTimeoutMs(),
            KEY_MOCK_DATA_ENABLED to isMockDataEnabled()
        )
    }

    /**
     * Export settings as JSON string.
     */
    fun exportSettingsJson(): String {
        val settings = getAllSettings()
        return buildString {
            append("{\n")
            settings.entries.forEachIndexed { index, (key, value) ->
                append("  \"$key\": ")
                when (value) {
                    is String -> append("\"$value\"")
                    is Boolean -> append(value)
                    is Number -> append(value)
                    else -> append("\"$value\"")
                }
                if (index < settings.size - 1) append(",")
                append("\n")
            }
            append("}")
        }
    }
}
