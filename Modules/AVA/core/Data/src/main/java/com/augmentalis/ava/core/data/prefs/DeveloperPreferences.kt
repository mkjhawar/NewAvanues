package com.augmentalis.ava.core.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Developer preferences for testing and debugging features.
 *
 * Stores settings that are useful for developers and QA testers but not shown to end users.
 *
 * Features:
 * - Flash mode: Real-time visual feedback showing which system (NLU/LLM) is actively processing
 * - (Future) Verbose logging toggle
 * - (Future) Performance metrics display
 * - (Future) Model switching
 *
 * @property context Application context for DataStore
 */
class DeveloperPreferences(private val context: Context) {

    companion object {
        private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "developer_preferences")

        /**
         * Flash mode toggle key.
         * When enabled, StatusIndicator shows real-time pulsing during NLU/LLM processing.
         */
        private val FLASH_MODE_ENABLED = booleanPreferencesKey("flash_mode_enabled")

        /**
         * Verbose logging toggle key (future).
         */
        private val VERBOSE_LOGGING_ENABLED = booleanPreferencesKey("verbose_logging_enabled")

        /**
         * Performance metrics display toggle key (future).
         */
        private val SHOW_PERFORMANCE_METRICS = booleanPreferencesKey("show_performance_metrics")
    }

    /**
     * Flash mode enabled state (REQ-007).
     *
     * When true, StatusIndicator will flash/pulse during active NLU/LLM processing.
     * Useful for developers and QA to see which system is working in real-time.
     *
     * Default: false (disabled for end users)
     */
    val isFlashModeEnabled: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[FLASH_MODE_ENABLED] ?: false
        }

    /**
     * Enable or disable flash mode (REQ-007).
     *
     * @param enabled True to enable flash mode, false to disable
     */
    suspend fun setFlashModeEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[FLASH_MODE_ENABLED] = enabled
        }
    }

    /**
     * Verbose logging enabled state (future feature).
     *
     * When true, enables detailed debug logging for all modules.
     * Useful for diagnosing issues in testing.
     *
     * Default: false
     */
    val isVerboseLoggingEnabled: Flow<Boolean> = context.dataStore.data
        .map { preferences: Preferences ->
            preferences[VERBOSE_LOGGING_ENABLED] ?: false
        }

    /**
     * Enable or disable verbose logging (future feature).
     *
     * @param enabled True to enable verbose logging, false to disable
     */
    suspend fun setVerboseLoggingEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences: androidx.datastore.preferences.core.MutablePreferences ->
            preferences[VERBOSE_LOGGING_ENABLED] = enabled
        }
    }

    /**
     * Performance metrics display enabled state (future feature).
     *
     * When true, shows real-time performance metrics overlay:
     * - Frame time (FPS)
     * - Memory usage
     * - CPU usage
     * - Network latency
     * - NLU inference time
     * - LLM response time
     *
     * Default: false
     */
    val isShowPerformanceMetrics: Flow<Boolean> = context.dataStore.data
        .map { preferences: Preferences ->
            preferences[SHOW_PERFORMANCE_METRICS] ?: false
        }

    /**
     * Enable or disable performance metrics display (future feature).
     *
     * @param enabled True to show performance metrics, false to hide
     */
    suspend fun setShowPerformanceMetrics(enabled: Boolean) {
        context.dataStore.edit { preferences: androidx.datastore.preferences.core.MutablePreferences ->
            preferences[SHOW_PERFORMANCE_METRICS] = enabled
        }
    }

    /**
     * Clear all developer preferences (reset to defaults).
     */
    suspend fun clearAll() {
        context.dataStore.edit { preferences: androidx.datastore.preferences.core.MutablePreferences ->
            preferences.clear()
        }
    }
}
