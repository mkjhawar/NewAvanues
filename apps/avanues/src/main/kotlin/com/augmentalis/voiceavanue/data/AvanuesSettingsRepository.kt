/**
 * AvanuesSettingsRepository.kt - DataStore-backed settings persistence
 *
 * Persists all app-level settings (cursor, voice, boot, browser) using
 * Jetpack DataStore Preferences. Observable via Flow for reactive UI.
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 */

package com.augmentalis.voiceavanue.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.augmentalis.avanueui.theme.AvanueThemeVariant
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * All Avanues app-level settings, persisted to DataStore.
 */
data class AvanuesSettings(
    val cursorEnabled: Boolean = false,
    val dwellClickEnabled: Boolean = true,
    val dwellClickDelayMs: Float = 1500f,
    val cursorSmoothing: Boolean = true,
    val voiceFeedback: Boolean = true,
    val autoStartOnBoot: Boolean = false,
    val themeVariant: String = AvanueThemeVariant.DEFAULT.name,

    // VoiceCursor appearance
    val cursorSize: Int = 48,
    val cursorSpeed: Int = 8,
    val showCoordinates: Boolean = false,
    val cursorAccentOverride: Long? = null  // null = use theme, else custom ARGB
)

@Singleton
class AvanuesSettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {

    companion object {
        private val KEY_CURSOR_ENABLED = booleanPreferencesKey("cursor_enabled")
        private val KEY_DWELL_CLICK_ENABLED = booleanPreferencesKey("dwell_click_enabled")
        private val KEY_DWELL_CLICK_DELAY = floatPreferencesKey("dwell_click_delay_ms")
        private val KEY_CURSOR_SMOOTHING = booleanPreferencesKey("cursor_smoothing")
        private val KEY_VOICE_FEEDBACK = booleanPreferencesKey("voice_feedback")
        private val KEY_AUTO_START_ON_BOOT = booleanPreferencesKey("auto_start_on_boot")
        private val KEY_THEME_VARIANT = stringPreferencesKey("theme_variant")

        // VoiceCursor appearance
        private val KEY_CURSOR_SIZE = intPreferencesKey("cursor_size")
        private val KEY_CURSOR_SPEED = intPreferencesKey("cursor_speed")
        private val KEY_SHOW_COORDINATES = booleanPreferencesKey("show_coordinates")
        private val KEY_CURSOR_ACCENT_OVERRIDE = longPreferencesKey("cursor_accent_override")
    }

    val settings: Flow<AvanuesSettings> = context.avanuesDataStore.data.map { prefs ->
        AvanuesSettings(
            cursorEnabled = prefs[KEY_CURSOR_ENABLED] ?: false,
            dwellClickEnabled = prefs[KEY_DWELL_CLICK_ENABLED] ?: true,
            dwellClickDelayMs = prefs[KEY_DWELL_CLICK_DELAY] ?: 1500f,
            cursorSmoothing = prefs[KEY_CURSOR_SMOOTHING] ?: true,
            voiceFeedback = prefs[KEY_VOICE_FEEDBACK] ?: true,
            autoStartOnBoot = prefs[KEY_AUTO_START_ON_BOOT] ?: false,
            themeVariant = prefs[KEY_THEME_VARIANT] ?: AvanueThemeVariant.DEFAULT.name,
            cursorSize = prefs[KEY_CURSOR_SIZE] ?: 48,
            cursorSpeed = prefs[KEY_CURSOR_SPEED] ?: 8,
            showCoordinates = prefs[KEY_SHOW_COORDINATES] ?: false,
            cursorAccentOverride = prefs[KEY_CURSOR_ACCENT_OVERRIDE]
        )
    }

    suspend fun updateCursorEnabled(enabled: Boolean) {
        context.avanuesDataStore.edit { it[KEY_CURSOR_ENABLED] = enabled }
    }

    suspend fun updateDwellClickEnabled(enabled: Boolean) {
        context.avanuesDataStore.edit { it[KEY_DWELL_CLICK_ENABLED] = enabled }
    }

    suspend fun updateDwellClickDelay(delayMs: Float) {
        context.avanuesDataStore.edit { it[KEY_DWELL_CLICK_DELAY] = delayMs.coerceIn(500f, 3000f) }
    }

    suspend fun updateCursorSmoothing(enabled: Boolean) {
        context.avanuesDataStore.edit { it[KEY_CURSOR_SMOOTHING] = enabled }
    }

    suspend fun updateVoiceFeedback(enabled: Boolean) {
        context.avanuesDataStore.edit { it[KEY_VOICE_FEEDBACK] = enabled }
    }

    suspend fun updateAutoStartOnBoot(enabled: Boolean) {
        context.avanuesDataStore.edit { it[KEY_AUTO_START_ON_BOOT] = enabled }
    }

    suspend fun updateThemeVariant(variant: String) {
        context.avanuesDataStore.edit { it[KEY_THEME_VARIANT] = variant }
    }

    suspend fun updateCursorSize(size: Int) {
        context.avanuesDataStore.edit { it[KEY_CURSOR_SIZE] = size.coerceIn(8, 64) }
    }

    suspend fun updateCursorSpeed(speed: Int) {
        context.avanuesDataStore.edit { it[KEY_CURSOR_SPEED] = speed.coerceIn(1, 15) }
    }

    suspend fun updateShowCoordinates(show: Boolean) {
        context.avanuesDataStore.edit { it[KEY_SHOW_COORDINATES] = show }
    }

    suspend fun updateCursorAccentOverride(argb: Long?) {
        context.avanuesDataStore.edit { prefs ->
            if (argb != null) {
                prefs[KEY_CURSOR_ACCENT_OVERRIDE] = argb
            } else {
                prefs.remove(KEY_CURSOR_ACCENT_OVERRIDE)
            }
        }
    }
}
