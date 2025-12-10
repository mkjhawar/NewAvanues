// filename: Universal/AVA/Features/WakeWord/src/main/java/com/augmentalis/ava/features/wakeword/settings/WakeWordSettingsRepository.kt
// created: 2025-11-22
// author: Manoj Jhawar
// © Augmentalis Inc, Intelligent Devices LLC

package com.augmentalis.ava.features.wakeword.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.augmentalis.ava.features.wakeword.WakeWordKeyword
import com.augmentalis.ava.features.wakeword.WakeWordSettings
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Wake Word Settings Repository
 *
 * Persists wake word configuration using DataStore preferences.
 *
 * @author Manoj Jhawar
 */
@Singleton
class WakeWordSettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val DATASTORE_NAME = "wake_word_settings"

        // Preference keys
        private val KEY_ENABLED = booleanPreferencesKey("enabled")
        private val KEY_KEYWORD = stringPreferencesKey("keyword")
        private val KEY_SENSITIVITY = floatPreferencesKey("sensitivity")
        private val KEY_BACKGROUND_LISTENING = booleanPreferencesKey("background_listening")
        private val KEY_BATTERY_OPTIMIZATION = booleanPreferencesKey("battery_optimization")
        private val KEY_SHOW_NOTIFICATION = booleanPreferencesKey("show_notification")
        private val KEY_SOUND_FEEDBACK = booleanPreferencesKey("sound_feedback")
        private val KEY_VIBRATE = booleanPreferencesKey("vibrate")
    }

    private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(
        name = DATASTORE_NAME
    )

    /**
     * Get wake word settings as Flow
     */
    val settings: Flow<WakeWordSettings> = context.dataStore.data.map { prefs ->
        WakeWordSettings(
            enabled = prefs[KEY_ENABLED] ?: false,
            keyword = WakeWordKeyword.valueOf(
                prefs[KEY_KEYWORD] ?: WakeWordKeyword.HEY_AVA.name
            ),
            sensitivity = prefs[KEY_SENSITIVITY] ?: 0.5f,
            backgroundListening = prefs[KEY_BACKGROUND_LISTENING] ?: true,
            batteryOptimization = prefs[KEY_BATTERY_OPTIMIZATION] ?: true,
            showNotification = prefs[KEY_SHOW_NOTIFICATION] ?: true,
            playSoundFeedback = prefs[KEY_SOUND_FEEDBACK] ?: true,
            vibrateOnDetection = prefs[KEY_VIBRATE] ?: false
        )
    }

    /**
     * Update wake word settings
     */
    suspend fun updateSettings(settings: WakeWordSettings) {
        context.dataStore.edit { prefs ->
            prefs[KEY_ENABLED] = settings.enabled
            prefs[KEY_KEYWORD] = settings.keyword.name
            prefs[KEY_SENSITIVITY] = settings.sensitivity
            prefs[KEY_BACKGROUND_LISTENING] = settings.backgroundListening
            prefs[KEY_BATTERY_OPTIMIZATION] = settings.batteryOptimization
            prefs[KEY_SHOW_NOTIFICATION] = settings.showNotification
            prefs[KEY_SOUND_FEEDBACK] = settings.playSoundFeedback
            prefs[KEY_VIBRATE] = settings.vibrateOnDetection
        }
        Timber.d("Wake word settings updated")
    }

    /**
     * Update enabled state
     */
    suspend fun setEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_ENABLED] = enabled
        }
        Timber.d("Wake word enabled: $enabled")
    }

    /**
     * Update keyword
     */
    suspend fun setKeyword(keyword: WakeWordKeyword) {
        context.dataStore.edit { prefs ->
            prefs[KEY_KEYWORD] = keyword.name
        }
        Timber.d("Wake word keyword: ${keyword.displayName}")
    }

    /**
     * Update sensitivity
     */
    suspend fun setSensitivity(sensitivity: Float) {
        context.dataStore.edit { prefs ->
            prefs[KEY_SENSITIVITY] = sensitivity.coerceIn(0.0f, 1.0f)
        }
        Timber.d("Wake word sensitivity: $sensitivity")
    }

    /**
     * Update battery optimization
     */
    suspend fun setBatteryOptimization(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_BATTERY_OPTIMIZATION] = enabled
        }
        Timber.d("Battery optimization: $enabled")
    }

    /**
     * Reset to defaults
     */
    suspend fun resetToDefaults() {
        context.dataStore.edit { prefs ->
            prefs.clear()
        }
        Timber.i("Wake word settings reset to defaults")
    }
}
