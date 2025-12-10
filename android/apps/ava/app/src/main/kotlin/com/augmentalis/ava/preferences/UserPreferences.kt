/**
 * User Preferences Manager
 *
 * Manages app-wide user preferences using DataStore.
 * Provides type-safe access to user settings with reactive Flow API.
 *
 * Features:
 * - Theme preferences (Light/Dark/Auto)
 * - Analytics opt-in/out
 * - Crash reporting opt-in/out
 * - Model download preferences
 * - UI customization settings
 *
 * Created: 2025-11-07
 * Author: AVA AI Team
 */

package com.augmentalis.ava.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import timber.log.Timber

/**
 * User preferences manager
 *
 * Thread-safe access to user preferences with reactive updates.
 * DataStore is injected via Hilt from DataStoreModule.
 */
class UserPreferences(
    private val dataStore: DataStore<Preferences>
) {

    companion object {
        // Preference keys
        private val CRASH_REPORTING_ENABLED = booleanPreferencesKey("crash_reporting_enabled")
        private val ANALYTICS_ENABLED = booleanPreferencesKey("analytics_enabled")
        private val THEME_MODE = stringPreferencesKey("theme_mode")
        private val ACCENT_COLOR = stringPreferencesKey("accent_color")
        private val USE_DYNAMIC_COLOR = booleanPreferencesKey("use_dynamic_color")
        private val AUTO_DOWNLOAD_MODELS = booleanPreferencesKey("auto_download_models")
        private val WIFI_ONLY_DOWNLOADS = booleanPreferencesKey("wifi_only_downloads")
        private val FIRST_LAUNCH = booleanPreferencesKey("first_launch")

        // Default values
        const val DEFAULT_CRASH_REPORTING = false  // Privacy-first: opt-in
        const val DEFAULT_ANALYTICS = false         // Privacy-first: opt-in
        const val DEFAULT_THEME = "auto"           // Follow system theme
        const val DEFAULT_ACCENT_COLOR = "teal"    // AVA brand teal
        const val DEFAULT_DYNAMIC_COLOR = false    // Disable by default
        const val DEFAULT_AUTO_DOWNLOAD = false    // User controls downloads
        const val DEFAULT_WIFI_ONLY = true         // Protect mobile data
        const val DEFAULT_FIRST_LAUNCH = true
    }

    /**
     * Crash reporting enabled
     *
     * @return Flow<Boolean> emitting current preference
     */
    val crashReportingEnabled: Flow<Boolean> = dataStore.data
        .map { preferences ->
            preferences[CRASH_REPORTING_ENABLED] ?: DEFAULT_CRASH_REPORTING
        }

    /**
     * Analytics enabled
     *
     * @return Flow<Boolean> emitting current preference
     */
    val analyticsEnabled: Flow<Boolean> = dataStore.data
        .map { preferences ->
            preferences[ANALYTICS_ENABLED] ?: DEFAULT_ANALYTICS
        }

    /**
     * Theme mode (light, dark, auto)
     *
     * @return Flow<String> emitting current theme preference
     */
    val themeMode: Flow<String> = dataStore.data
        .map { preferences ->
            preferences[THEME_MODE] ?: DEFAULT_THEME
        }

    /**
     * Accent color (teal, purple, blue, green, orange, pink)
     *
     * @return Flow<String> emitting current accent color preference
     */
    val accentColor: Flow<String> = dataStore.data
        .map { preferences ->
            preferences[ACCENT_COLOR] ?: DEFAULT_ACCENT_COLOR
        }

    /**
     * Use Material You dynamic color
     *
     * @return Flow<Boolean> emitting current dynamic color preference
     */
    val useDynamicColor: Flow<Boolean> = dataStore.data
        .map { preferences ->
            preferences[USE_DYNAMIC_COLOR] ?: DEFAULT_DYNAMIC_COLOR
        }

    /**
     * Auto-download models
     *
     * @return Flow<Boolean> emitting current preference
     */
    val autoDownloadModels: Flow<Boolean> = dataStore.data
        .map { preferences ->
            preferences[AUTO_DOWNLOAD_MODELS] ?: DEFAULT_AUTO_DOWNLOAD
        }

    /**
     * WiFi-only downloads
     *
     * @return Flow<Boolean> emitting current preference
     */
    val wifiOnlyDownloads: Flow<Boolean> = dataStore.data
        .map { preferences ->
            preferences[WIFI_ONLY_DOWNLOADS] ?: DEFAULT_WIFI_ONLY
        }

    /**
     * First launch flag
     *
     * @return Flow<Boolean> emitting true if this is the first launch
     */
    val isFirstLaunch: Flow<Boolean> = dataStore.data
        .map { preferences ->
            preferences[FIRST_LAUNCH] ?: DEFAULT_FIRST_LAUNCH
        }

    /**
     * Set crash reporting preference
     *
     * @param enabled Whether crash reporting should be enabled
     */
    suspend fun setCrashReportingEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[CRASH_REPORTING_ENABLED] = enabled
        }
        Timber.d("Crash reporting preference updated: $enabled")
    }

    /**
     * Set analytics preference
     *
     * @param enabled Whether analytics should be enabled
     */
    suspend fun setAnalyticsEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[ANALYTICS_ENABLED] = enabled
        }
        Timber.d("Analytics preference updated: $enabled")
    }

    /**
     * Set theme mode
     *
     * @param mode Theme mode (light, dark, auto)
     */
    suspend fun setThemeMode(mode: String) {
        require(mode in listOf("light", "dark", "auto")) {
            "Invalid theme mode: $mode. Must be light, dark, or auto"
        }

        dataStore.edit { preferences ->
            preferences[THEME_MODE] = mode
        }
        Timber.d("Theme mode updated: $mode")
    }

    /**
     * Set accent color
     *
     * @param color Accent color (teal, purple, blue, green, orange, pink)
     */
    suspend fun setAccentColor(color: String) {
        require(color.lowercase() in listOf("teal", "purple", "blue", "green", "orange", "pink")) {
            "Invalid accent color: $color"
        }

        dataStore.edit { preferences ->
            preferences[ACCENT_COLOR] = color.lowercase()
        }
        Timber.d("Accent color updated: $color")
    }

    /**
     * Set dynamic color preference
     *
     * @param enabled Whether to use Material You dynamic colors
     */
    suspend fun setUseDynamicColor(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[USE_DYNAMIC_COLOR] = enabled
        }
        Timber.d("Dynamic color updated: $enabled")
    }

    /**
     * Set auto-download models preference
     *
     * @param enabled Whether models should auto-download
     */
    suspend fun setAutoDownloadModels(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[AUTO_DOWNLOAD_MODELS] = enabled
        }
        Timber.d("Auto-download models preference updated: $enabled")
    }

    /**
     * Set WiFi-only downloads preference
     *
     * @param enabled Whether downloads should be WiFi-only
     */
    suspend fun setWifiOnlyDownloads(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[WIFI_ONLY_DOWNLOADS] = enabled
        }
        Timber.d("WiFi-only downloads preference updated: $enabled")
    }

    /**
     * Mark first launch as complete
     */
    suspend fun completeFirstLaunch() {
        dataStore.edit { preferences ->
            preferences[FIRST_LAUNCH] = false
        }
        Timber.d("First launch completed")
    }

    /**
     * Clear all preferences (for testing or reset)
     */
    suspend fun clearAll() {
        dataStore.edit { preferences ->
            preferences.clear()
        }
        Timber.w("All preferences cleared")
    }

    /**
     * Get all preferences as Map (for debugging)
     */
    suspend fun getAllPreferences(): Map<String, Any> {
        val prefs = mutableMapOf<String, Any>()

        dataStore.data.map { preferences ->
            prefs["crashReportingEnabled"] = preferences[CRASH_REPORTING_ENABLED] ?: DEFAULT_CRASH_REPORTING
            prefs["analyticsEnabled"] = preferences[ANALYTICS_ENABLED] ?: DEFAULT_ANALYTICS
            prefs["themeMode"] = preferences[THEME_MODE] ?: DEFAULT_THEME
            prefs["autoDownloadModels"] = preferences[AUTO_DOWNLOAD_MODELS] ?: DEFAULT_AUTO_DOWNLOAD
            prefs["wifiOnlyDownloads"] = preferences[WIFI_ONLY_DOWNLOADS] ?: DEFAULT_WIFI_ONLY
            prefs["isFirstLaunch"] = preferences[FIRST_LAUNCH] ?: DEFAULT_FIRST_LAUNCH
        }.collect {}

        return prefs
    }
}

/**
 * Theme mode enum for type safety
 */
enum class ThemeMode(val value: String) {
    LIGHT("light"),
    DARK("dark"),
    AUTO("auto");

    companion object {
        fun fromString(value: String): ThemeMode = when (value) {
            "light" -> LIGHT
            "dark" -> DARK
            "auto" -> AUTO
            else -> AUTO
        }
    }
}
