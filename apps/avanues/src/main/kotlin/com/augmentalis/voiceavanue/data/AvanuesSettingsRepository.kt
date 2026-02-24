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
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.augmentalis.foundation.settings.ISettingsStore
import com.augmentalis.foundation.settings.SettingsMigration
import com.augmentalis.foundation.settings.models.AvanuesSettings
import com.augmentalis.foundation.settings.models.PersistedSynonym
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AvanuesSettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context
) : ISettingsStore<AvanuesSettings> {

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

        // Wake Word
        private val KEY_WAKE_WORD_ENABLED = booleanPreferencesKey("wake_word_enabled")
        private val KEY_WAKE_WORD_KEYWORD = stringPreferencesKey("wake_word_keyword")
        private val KEY_WAKE_WORD_SENSITIVITY = floatPreferencesKey("wake_word_sensitivity")

        // VoiceCursor appearance
        private val KEY_CURSOR_SIZE = intPreferencesKey("cursor_size")
        private val KEY_CURSOR_SPEED = intPreferencesKey("cursor_speed")
        private val KEY_SHOW_COORDINATES = booleanPreferencesKey("show_coordinates")
        private val KEY_CURSOR_ACCENT_OVERRIDE = longPreferencesKey("cursor_accent_override")

        // VOS Sync (Developer)
        private val KEY_VOS_SYNC_ENABLED = booleanPreferencesKey("vos_sync_enabled")
        private val KEY_VOS_SFTP_HOST = stringPreferencesKey("vos_sftp_host")
        private val KEY_VOS_SFTP_PORT = intPreferencesKey("vos_sftp_port")
        private val KEY_VOS_SFTP_USERNAME = stringPreferencesKey("vos_sftp_username")
        private val KEY_VOS_SFTP_REMOTE_PATH = stringPreferencesKey("vos_sftp_remote_path")
        private val KEY_VOS_SFTP_KEY_PATH = stringPreferencesKey("vos_sftp_key_path")
        private val KEY_VOS_LAST_SYNC_TIME = longPreferencesKey("vos_last_sync_time")
        private val KEY_VOS_SFTP_HOST_KEY_MODE = stringPreferencesKey("vos_sftp_host_key_mode")
        private val KEY_VOS_AUTO_SYNC_ENABLED = booleanPreferencesKey("vos_auto_sync_enabled")
        private val KEY_VOS_SYNC_INTERVAL_HOURS = intPreferencesKey("vos_sync_interval_hours")

        // Voice command persistence (AVU wire protocol format)
        private val KEY_DISABLED_COMMANDS = stringSetPreferencesKey("vcm_disabled_commands")
        private val KEY_USER_SYNONYMS = stringPreferencesKey("vcm_user_synonyms")

        // Adaptive Timing (learned by AdaptiveTimingManager, persisted across restarts)
        private val KEY_ADAPTIVE_PROCESSING_DELAY = longPreferencesKey("adaptive_processing_delay_ms")
        private val KEY_ADAPTIVE_SCROLL_DEBOUNCE = longPreferencesKey("adaptive_scroll_debounce_ms")
        private val KEY_ADAPTIVE_SPEECH_UPDATE_DEBOUNCE = longPreferencesKey("adaptive_speech_update_debounce_ms")
        private val KEY_ADAPTIVE_COMMAND_WINDOW = longPreferencesKey("adaptive_command_window_ms")

        // Migration functions now in Foundation: SettingsMigration
    }

    override val settings: Flow<AvanuesSettings> = context.avanuesDataStore.data.map { prefs ->
        readFromPreferences(prefs)
    }

    override suspend fun update(block: (AvanuesSettings) -> AvanuesSettings) {
        context.avanuesDataStore.edit { prefs ->
            val current = readFromPreferences(prefs)
            val updated = block(current)
            writeToPreferences(prefs, updated)
        }
    }

    private fun readFromPreferences(prefs: Preferences): AvanuesSettings {
        // Migration: if new keys don't exist, derive from old theme_variant
        val oldVariant = prefs[KEY_THEME_VARIANT]
        val palette = prefs[KEY_THEME_PALETTE] ?: SettingsMigration.migrateVariantToPalette(oldVariant)
        val style = prefs[KEY_THEME_STYLE] ?: SettingsMigration.migrateVariantToStyle(oldVariant)

        return AvanuesSettings(
            cursorEnabled = prefs[KEY_CURSOR_ENABLED] ?: false,
            dwellClickEnabled = prefs[KEY_DWELL_CLICK_ENABLED] ?: true,
            dwellClickDelayMs = prefs[KEY_DWELL_CLICK_DELAY] ?: 1500f,
            cursorSmoothing = prefs[KEY_CURSOR_SMOOTHING] ?: true,
            voiceFeedback = prefs[KEY_VOICE_FEEDBACK] ?: true,
            autoStartOnBoot = prefs[KEY_AUTO_START_ON_BOOT] ?: false,
            themePalette = palette,
            themeStyle = style,
            themeAppearance = prefs[KEY_THEME_APPEARANCE] ?: AvanuesSettings.DEFAULT_THEME_APPEARANCE,
            voiceLocale = prefs[KEY_VOICE_LOCALE] ?: "en-US",
            wakeWordEnabled = prefs[KEY_WAKE_WORD_ENABLED] ?: false,
            wakeWordKeyword = prefs[KEY_WAKE_WORD_KEYWORD] ?: AvanuesSettings.DEFAULT_WAKE_WORD_KEYWORD,
            wakeWordSensitivity = prefs[KEY_WAKE_WORD_SENSITIVITY] ?: AvanuesSettings.DEFAULT_WAKE_WORD_SENSITIVITY,
            cursorSize = prefs[KEY_CURSOR_SIZE] ?: 48,
            cursorSpeed = prefs[KEY_CURSOR_SPEED] ?: 8,
            showCoordinates = prefs[KEY_SHOW_COORDINATES] ?: false,
            cursorAccentOverride = prefs[KEY_CURSOR_ACCENT_OVERRIDE],
            vosSyncEnabled = prefs[KEY_VOS_SYNC_ENABLED] ?: false,
            vosSftpHost = prefs[KEY_VOS_SFTP_HOST] ?: "",
            vosSftpPort = prefs[KEY_VOS_SFTP_PORT] ?: 22,
            vosSftpUsername = prefs[KEY_VOS_SFTP_USERNAME] ?: "",
            vosSftpRemotePath = prefs[KEY_VOS_SFTP_REMOTE_PATH] ?: "/vos",
            vosSftpKeyPath = prefs[KEY_VOS_SFTP_KEY_PATH] ?: "",
            vosLastSyncTime = prefs[KEY_VOS_LAST_SYNC_TIME],
            vosSftpHostKeyMode = prefs[KEY_VOS_SFTP_HOST_KEY_MODE] ?: "strict",
            vosAutoSyncEnabled = prefs[KEY_VOS_AUTO_SYNC_ENABLED] ?: false,
            vosSyncIntervalHours = prefs[KEY_VOS_SYNC_INTERVAL_HOURS] ?: 4
        )
    }

    private fun writeToPreferences(prefs: MutablePreferences, s: AvanuesSettings) {
        prefs[KEY_CURSOR_ENABLED] = s.cursorEnabled
        prefs[KEY_DWELL_CLICK_ENABLED] = s.dwellClickEnabled
        prefs[KEY_DWELL_CLICK_DELAY] = s.dwellClickDelayMs
        prefs[KEY_CURSOR_SMOOTHING] = s.cursorSmoothing
        prefs[KEY_VOICE_FEEDBACK] = s.voiceFeedback
        prefs[KEY_AUTO_START_ON_BOOT] = s.autoStartOnBoot
        prefs[KEY_THEME_PALETTE] = s.themePalette
        prefs[KEY_THEME_STYLE] = s.themeStyle
        prefs[KEY_THEME_APPEARANCE] = s.themeAppearance
        prefs[KEY_VOICE_LOCALE] = s.voiceLocale
        prefs[KEY_WAKE_WORD_ENABLED] = s.wakeWordEnabled
        prefs[KEY_WAKE_WORD_KEYWORD] = s.wakeWordKeyword
        prefs[KEY_WAKE_WORD_SENSITIVITY] = s.wakeWordSensitivity
        prefs[KEY_CURSOR_SIZE] = s.cursorSize
        prefs[KEY_CURSOR_SPEED] = s.cursorSpeed
        prefs[KEY_SHOW_COORDINATES] = s.showCoordinates
        val accentOverride = s.cursorAccentOverride
        if (accentOverride != null) {
            prefs[KEY_CURSOR_ACCENT_OVERRIDE] = accentOverride
        } else {
            prefs.remove(KEY_CURSOR_ACCENT_OVERRIDE)
        }
        prefs[KEY_VOS_SYNC_ENABLED] = s.vosSyncEnabled
        prefs[KEY_VOS_SFTP_HOST] = s.vosSftpHost
        prefs[KEY_VOS_SFTP_PORT] = s.vosSftpPort
        prefs[KEY_VOS_SFTP_USERNAME] = s.vosSftpUsername
        prefs[KEY_VOS_SFTP_REMOTE_PATH] = s.vosSftpRemotePath
        prefs[KEY_VOS_SFTP_KEY_PATH] = s.vosSftpKeyPath
        val lastSyncTime = s.vosLastSyncTime
        if (lastSyncTime != null) {
            prefs[KEY_VOS_LAST_SYNC_TIME] = lastSyncTime
        } else {
            prefs.remove(KEY_VOS_LAST_SYNC_TIME)
        }
        prefs[KEY_VOS_SFTP_HOST_KEY_MODE] = s.vosSftpHostKeyMode
        prefs[KEY_VOS_AUTO_SYNC_ENABLED] = s.vosAutoSyncEnabled
        prefs[KEY_VOS_SYNC_INTERVAL_HOURS] = s.vosSyncIntervalHours
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

    // ==================== Wake Word Settings ====================

    suspend fun updateWakeWordEnabled(enabled: Boolean) {
        context.avanuesDataStore.edit { it[KEY_WAKE_WORD_ENABLED] = enabled }
    }

    suspend fun updateWakeWordKeyword(keyword: String) {
        context.avanuesDataStore.edit { it[KEY_WAKE_WORD_KEYWORD] = keyword }
    }

    suspend fun updateWakeWordSensitivity(sensitivity: Float) {
        context.avanuesDataStore.edit { it[KEY_WAKE_WORD_SENSITIVITY] = sensitivity.coerceIn(0.1f, 0.9f) }
    }

    // ==================== VOS Sync Settings ====================

    suspend fun updateVosSyncEnabled(enabled: Boolean) {
        context.avanuesDataStore.edit { it[KEY_VOS_SYNC_ENABLED] = enabled }
    }

    suspend fun updateVosSftpHost(host: String) {
        context.avanuesDataStore.edit { it[KEY_VOS_SFTP_HOST] = host }
    }

    suspend fun updateVosSftpPort(port: Int) {
        context.avanuesDataStore.edit { it[KEY_VOS_SFTP_PORT] = port.coerceIn(1, 65535) }
    }

    suspend fun updateVosSftpUsername(username: String) {
        context.avanuesDataStore.edit { it[KEY_VOS_SFTP_USERNAME] = username }
    }

    suspend fun updateVosSftpRemotePath(path: String) {
        context.avanuesDataStore.edit { it[KEY_VOS_SFTP_REMOTE_PATH] = path }
    }

    suspend fun updateVosSftpKeyPath(path: String) {
        context.avanuesDataStore.edit { it[KEY_VOS_SFTP_KEY_PATH] = path }
    }

    suspend fun updateVosLastSyncTime(time: Long) {
        context.avanuesDataStore.edit { it[KEY_VOS_LAST_SYNC_TIME] = time }
    }

    suspend fun updateVosSftpHostKeyMode(mode: String) {
        context.avanuesDataStore.edit { it[KEY_VOS_SFTP_HOST_KEY_MODE] = mode }
    }

    suspend fun updateVosAutoSyncEnabled(enabled: Boolean) {
        context.avanuesDataStore.edit { it[KEY_VOS_AUTO_SYNC_ENABLED] = enabled }
    }

    suspend fun updateVosSyncIntervalHours(hours: Int) {
        context.avanuesDataStore.edit { it[KEY_VOS_SYNC_INTERVAL_HOURS] = hours.coerceIn(1, 24) }
    }

    // ==================== Adaptive Timing Persistence ====================

    /**
     * Load persisted adaptive timing values and apply to AdaptiveTimingManager.
     * Call once on startup after AdaptiveTimingManager is available.
     */
    suspend fun loadAdaptiveTimingValues() {
        val mgr = com.augmentalis.voiceoscore.AdaptiveTimingManager
        context.avanuesDataStore.data.first().let { prefs ->
            val map = mutableMapOf<String, Long>()
            prefs[KEY_ADAPTIVE_PROCESSING_DELAY]?.let { map[mgr.Keys.PROCESSING_DELAY] = it }
            prefs[KEY_ADAPTIVE_SCROLL_DEBOUNCE]?.let { map[mgr.Keys.SCROLL_DEBOUNCE] = it }
            prefs[KEY_ADAPTIVE_SPEECH_UPDATE_DEBOUNCE]?.let { map[mgr.Keys.SPEECH_UPDATE_DEBOUNCE] = it }
            prefs[KEY_ADAPTIVE_COMMAND_WINDOW]?.let { map[mgr.Keys.COMMAND_WINDOW] = it }
            if (map.isNotEmpty()) {
                mgr.applyPersistedValues(map)
            }
        }
    }

    /**
     * Persist current AdaptiveTimingManager learned values to DataStore.
     * Call periodically (e.g., every 60s) or on app pause/stop.
     */
    suspend fun persistAdaptiveTimingValues() {
        val mgr = com.augmentalis.voiceoscore.AdaptiveTimingManager
        val values = mgr.toPersistedMap()
        context.avanuesDataStore.edit { prefs ->
            values[mgr.Keys.PROCESSING_DELAY]?.let {
                prefs[KEY_ADAPTIVE_PROCESSING_DELAY] = it
            }
            values[mgr.Keys.SCROLL_DEBOUNCE]?.let {
                prefs[KEY_ADAPTIVE_SCROLL_DEBOUNCE] = it
            }
            values[mgr.Keys.SPEECH_UPDATE_DEBOUNCE]?.let {
                prefs[KEY_ADAPTIVE_SPEECH_UPDATE_DEBOUNCE] = it
            }
            values[mgr.Keys.COMMAND_WINDOW]?.let {
                prefs[KEY_ADAPTIVE_COMMAND_WINDOW] = it
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

// PersistedSynonym now in Foundation: com.augmentalis.foundation.settings.models.PersistedSynonym
