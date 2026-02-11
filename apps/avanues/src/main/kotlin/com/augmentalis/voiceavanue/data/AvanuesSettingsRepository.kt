/**
 * AvanuesSettingsRepository.kt - DataStore-backed settings persistence
 *
 * Persists all app-level settings (cursor, voice, boot, browser) using
 * Jetpack DataStore Preferences. Observable via Flow for reactive UI.
 *
 * Theme v5.1: Palette, style, and appearance stored independently.
 * Migration: old theme_variant → new palette + style keys.
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
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.augmentalis.avanueui.theme.AppearanceMode
import com.augmentalis.avanueui.theme.AvanueColorPalette
import com.augmentalis.avanueui.theme.MaterialMode
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject
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
    val themePalette: String = AvanueColorPalette.DEFAULT.name,
    val themeStyle: String = MaterialMode.DEFAULT.name,
    val themeAppearance: String = AppearanceMode.DEFAULT.name,

    // Voice command locale (e.g., "en-US", "es-ES")
    val voiceLocale: String = "en-US",

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

        // Theme v5.1: decoupled palette + style + appearance
        private val KEY_THEME_PALETTE = stringPreferencesKey("theme_palette")
        private val KEY_THEME_STYLE = stringPreferencesKey("theme_style")
        private val KEY_THEME_APPEARANCE = stringPreferencesKey("theme_appearance")
        // Legacy key — read-only for migration
        private val KEY_THEME_VARIANT = stringPreferencesKey("theme_variant")

        // Voice command locale
        private val KEY_VOICE_LOCALE = stringPreferencesKey("voice_command_locale")

        // VoiceCursor appearance
        private val KEY_CURSOR_SIZE = intPreferencesKey("cursor_size")
        private val KEY_CURSOR_SPEED = intPreferencesKey("cursor_speed")
        private val KEY_SHOW_COORDINATES = booleanPreferencesKey("show_coordinates")
        private val KEY_CURSOR_ACCENT_OVERRIDE = longPreferencesKey("cursor_accent_override")

        // Voice command persistence (AVU wire protocol format)
        private val KEY_DISABLED_COMMANDS = stringSetPreferencesKey("vcm_disabled_commands")
        private val KEY_USER_SYNONYMS = stringPreferencesKey("vcm_user_synonyms")

        /**
         * Migrate old theme_variant to new palette string.
         * OCEAN→LUNA, SUNSET→SOL, LIQUID→HYDRA
         */
        private fun migrateVariantToPalette(variant: String?): String = when {
            variant.equals("OCEAN", ignoreCase = true) -> "LUNA"
            variant.equals("SUNSET", ignoreCase = true) -> "SOL"
            variant.equals("LIQUID", ignoreCase = true) -> "HYDRA"
            else -> AvanueColorPalette.DEFAULT.name
        }

        /**
         * Migrate old theme_variant to new style string.
         * OCEAN→GLASS, SUNSET→GLASS, LIQUID→WATER
         */
        private fun migrateVariantToStyle(variant: String?): String = when {
            variant.equals("OCEAN", ignoreCase = true) -> "GLASS"
            variant.equals("SUNSET", ignoreCase = true) -> "GLASS"
            variant.equals("LIQUID", ignoreCase = true) -> "WATER"
            else -> MaterialMode.DEFAULT.name
        }
    }

    val settings: Flow<AvanuesSettings> = context.avanuesDataStore.data.map { prefs ->
        // Migration: if new keys don't exist, derive from old theme_variant
        val oldVariant = prefs[KEY_THEME_VARIANT]
        val palette = prefs[KEY_THEME_PALETTE] ?: migrateVariantToPalette(oldVariant)
        val style = prefs[KEY_THEME_STYLE] ?: migrateVariantToStyle(oldVariant)

        AvanuesSettings(
            cursorEnabled = prefs[KEY_CURSOR_ENABLED] ?: false,
            dwellClickEnabled = prefs[KEY_DWELL_CLICK_ENABLED] ?: true,
            dwellClickDelayMs = prefs[KEY_DWELL_CLICK_DELAY] ?: 1500f,
            cursorSmoothing = prefs[KEY_CURSOR_SMOOTHING] ?: true,
            voiceFeedback = prefs[KEY_VOICE_FEEDBACK] ?: true,
            autoStartOnBoot = prefs[KEY_AUTO_START_ON_BOOT] ?: false,
            themePalette = palette,
            themeStyle = style,
            themeAppearance = prefs[KEY_THEME_APPEARANCE] ?: AppearanceMode.DEFAULT.name,
            voiceLocale = prefs[KEY_VOICE_LOCALE] ?: "en-US",
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

    suspend fun updateThemePalette(palette: String) {
        context.avanuesDataStore.edit { it[KEY_THEME_PALETTE] = palette }
    }

    suspend fun updateThemeStyle(style: String) {
        context.avanuesDataStore.edit { it[KEY_THEME_STYLE] = style }
    }

    suspend fun updateThemeAppearance(appearance: String) {
        context.avanuesDataStore.edit { it[KEY_THEME_APPEARANCE] = appearance }
    }

    suspend fun updateVoiceLocale(locale: String) {
        context.avanuesDataStore.edit { it[KEY_VOICE_LOCALE] = locale }
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

    // ==================== Voice Command Persistence ====================

    /**
     * Flow of disabled command IDs.
     * Commands not in this set are enabled (default state).
     */
    val disabledCommands: Flow<Set<String>> = context.avanuesDataStore.data.map { prefs ->
        prefs[KEY_DISABLED_COMMANDS] ?: emptySet()
    }

    /**
     * Flow of user-added synonym entries, parsed from AVU-format JSON.
     * Format: [{"vu":"SYN","canonical":"click","synonyms":["tap","push"],"v":1}, ...]
     */
    val userSynonyms: Flow<List<PersistedSynonym>> = context.avanuesDataStore.data.map { prefs ->
        val json = prefs[KEY_USER_SYNONYMS] ?: "[]"
        parseUserSynonyms(json)
    }

    /**
     * Toggle a command's disabled state.
     * If currently disabled, removes from set (re-enables).
     * If currently enabled, adds to set (disables).
     */
    suspend fun setCommandDisabled(commandId: String, disabled: Boolean) {
        context.avanuesDataStore.edit { prefs ->
            val current = prefs[KEY_DISABLED_COMMANDS] ?: emptySet()
            prefs[KEY_DISABLED_COMMANDS] = if (disabled) {
                current + commandId
            } else {
                current - commandId
            }
        }
    }

    /**
     * Save a user synonym entry. Merges with existing if canonical already present.
     * Stored as AVU-format JSON: {"vu":"SYN","canonical":"click","synonyms":["tap"],"v":1}
     */
    suspend fun saveUserSynonym(canonical: String, synonyms: List<String>) {
        context.avanuesDataStore.edit { prefs ->
            val json = prefs[KEY_USER_SYNONYMS] ?: "[]"
            val entries = parseUserSynonyms(json).toMutableList()
            val existingIdx = entries.indexOfFirst {
                it.canonical.equals(canonical, ignoreCase = true)
            }
            if (existingIdx >= 0) {
                val existing = entries[existingIdx]
                entries[existingIdx] = existing.copy(
                    synonyms = (existing.synonyms + synonyms).distinct()
                )
            } else {
                entries.add(PersistedSynonym(canonical, synonyms))
            }
            prefs[KEY_USER_SYNONYMS] = serializeUserSynonyms(entries)
        }
    }

    /**
     * Remove a user synonym entry by canonical name.
     */
    suspend fun removeUserSynonym(canonical: String) {
        context.avanuesDataStore.edit { prefs ->
            val json = prefs[KEY_USER_SYNONYMS] ?: "[]"
            val entries = parseUserSynonyms(json).filter {
                !it.canonical.equals(canonical, ignoreCase = true)
            }
            prefs[KEY_USER_SYNONYMS] = serializeUserSynonyms(entries)
        }
    }

    private fun parseUserSynonyms(json: String): List<PersistedSynonym> {
        return try {
            val array = JSONArray(json)
            (0 until array.length()).map { i ->
                val obj = array.getJSONObject(i)
                val synArray = obj.getJSONArray("synonyms")
                PersistedSynonym(
                    canonical = obj.getString("canonical"),
                    synonyms = (0 until synArray.length()).map { j -> synArray.getString(j) }
                )
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun serializeUserSynonyms(entries: List<PersistedSynonym>): String {
        val array = JSONArray()
        for (entry in entries) {
            val obj = JSONObject()
            obj.put("vu", "SYN")
            obj.put("v", 1)
            obj.put("canonical", entry.canonical)
            obj.put("synonyms", JSONArray(entry.synonyms))
            array.put(obj)
        }
        return array.toString()
    }
}

/**
 * Persisted user synonym: canonical verb → list of alternatives.
 * AVU unit type: SYN (Synonym mapping).
 */
data class PersistedSynonym(
    val canonical: String,
    val synonyms: List<String>
)
