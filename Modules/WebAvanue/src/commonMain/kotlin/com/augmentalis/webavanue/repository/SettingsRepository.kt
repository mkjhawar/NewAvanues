package com.augmentalis.webavanue.repository

import com.augmentalis.webavanue.BrowserSettings
import com.augmentalis.webavanue.SettingsPreset
import com.augmentalis.webavanue.data.db.BrowserDatabase
import com.augmentalis.webavanue.toDomainModel
import io.github.aakira.napier.Napier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

/**
 * Repository interface for browser settings operations.
 *
 * Handles all settings-related data persistence including:
 * - Settings CRUD operations
 * - Preset configurations
 * - Reactive observation via Flow
 */
interface SettingsRepository {
    /** Gets browser settings */
    suspend fun getSettings(): Result<BrowserSettings>

    /** Observes settings changes */
    fun observeSettings(): Flow<BrowserSettings>

    /** Updates browser settings */
    suspend fun updateSettings(settings: BrowserSettings): Result<Unit>

    /** Updates a specific setting */
    suspend fun <T> updateSetting(key: String, value: T): Result<Unit>

    /** Resets settings to default */
    suspend fun resetSettings(): Result<Unit>

    /** Applies a preset configuration */
    suspend fun applyPreset(preset: SettingsPreset): Result<Unit>

    /** Updates in-memory state directly (for fast startup) */
    suspend fun updateState(settings: BrowserSettings)
}

/**
 * SQLDelight implementation of SettingsRepository.
 *
 * @param database SQLDelight database instance
 */
class SettingsRepositoryImpl(
    private val database: BrowserDatabase
) : SettingsRepository {

    private val queries = database.browserDatabaseQueries
    private val _settings = MutableStateFlow(BrowserSettings.default())

    override suspend fun getSettings(): Result<BrowserSettings> = withContext(Dispatchers.IO) {
        try {
            queries.insertDefaultSettings()
            val dbSettings = queries.selectSettings().executeAsOneOrNull()
            val settings = dbSettings?.toDomainModel() ?: BrowserSettings.default()
            Result.success(settings)
        } catch (e: Exception) {
            Napier.e("Error getting settings: ${e.message}", e, tag = TAG)
            Result.failure(e)
        }
    }

    override fun observeSettings(): Flow<BrowserSettings> = _settings.asStateFlow()

    override suspend fun updateSettings(settings: BrowserSettings): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            queries.updateSettings(
                theme = settings.theme.name,
                font_size = settings.fontSize.name,
                force_zoom = if (settings.forceZoom) 1L else 0L,
                show_images = if (settings.showImages) 1L else 0L,
                use_desktop_mode = if (settings.useDesktopMode) 1L else 0L,
                block_popups = if (settings.blockPopups) 1L else 0L,
                block_ads = if (settings.blockAds) 1L else 0L,
                block_trackers = if (settings.blockTrackers) 1L else 0L,
                do_not_track = if (settings.doNotTrack) 1L else 0L,
                clear_cache_on_exit = if (settings.clearCacheOnExit) 1L else 0L,
                clear_history_on_exit = if (settings.clearHistoryOnExit) 1L else 0L,
                clear_cookies_on_exit = if (settings.clearCookiesOnExit) 1L else 0L,
                enable_cookies = if (settings.enableCookies) 1L else 0L,
                enable_javascript = if (settings.enableJavaScript) 1L else 0L,
                enable_webrtc = if (settings.enableWebRTC) 1L else 0L,
                default_search_engine = settings.defaultSearchEngine.name,
                custom_search_engine_name = settings.customSearchEngineName,
                custom_search_engine_url = settings.customSearchEngineUrl,
                search_suggestions = if (settings.searchSuggestions) 1L else 0L,
                voice_search = if (settings.voiceSearch) 1L else 0L,
                home_page = settings.homePage,
                new_tab_page = settings.newTabPage.name,
                restore_tabs_on_startup = if (settings.restoreTabsOnStartup) 1L else 0L,
                open_links_in_background = if (settings.openLinksInBackground) 1L else 0L,
                open_links_in_new_tab = if (settings.openLinksInNewTab) 1L else 0L,
                download_path = settings.downloadPath,
                ask_download_location = if (settings.askDownloadLocation) 1L else 0L,
                download_over_wifi_only = if (settings.downloadOverWiFiOnly) 1L else 0L,
                sync_enabled = if (settings.syncEnabled) 1L else 0L,
                sync_bookmarks = if (settings.syncBookmarks) 1L else 0L,
                sync_history = if (settings.syncHistory) 1L else 0L,
                sync_passwords = if (settings.syncPasswords) 1L else 0L,
                sync_settings = if (settings.syncSettings) 1L else 0L,
                hardware_acceleration = if (settings.hardwareAcceleration) 1L else 0L,
                preload_pages = if (settings.preloadPages) 1L else 0L,
                data_saver = if (settings.dataSaver) 1L else 0L,
                auto_play = settings.autoPlay.name,
                text_reflow = if (settings.textReflow) 1L else 0L,
                enable_voice_commands = if (settings.enableVoiceCommands) 1L else 0L,
                ai_summaries = if (settings.aiSummaries) 1L else 0L,
                ai_translation = if (settings.aiTranslation) 1L else 0L,
                read_aloud = if (settings.readAloud) 1L else 0L
            )
            _settings.value = settings
            Result.success(Unit)
        } catch (e: Exception) {
            Napier.e("Error updating settings: ${e.message}", e, tag = TAG)
            Result.failure(e)
        }
    }

    override suspend fun <T> updateSetting(key: String, value: T): Result<Unit> = withContext(Dispatchers.IO) {
        // Individual setting updates can be implemented if needed
        Result.success(Unit)
    }

    override suspend fun resetSettings(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            updateSettings(BrowserSettings.default())
        } catch (e: Exception) {
            Napier.e("Error resetting settings: ${e.message}", e, tag = TAG)
            Result.failure(e)
        }
    }

    override suspend fun applyPreset(preset: SettingsPreset): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val settings = when (preset) {
                SettingsPreset.DEFAULT -> BrowserSettings.default()
                SettingsPreset.PRIVACY -> BrowserSettings.default().copy(
                    blockAds = true,
                    blockTrackers = true,
                    doNotTrack = true,
                    clearCacheOnExit = true,
                    clearHistoryOnExit = true,
                    clearCookiesOnExit = true,
                    enableWebRTC = false
                )
                SettingsPreset.PERFORMANCE -> BrowserSettings.default().copy(
                    hardwareAcceleration = true,
                    preloadPages = true,
                    showImages = true,
                    dataSaver = false
                )
                SettingsPreset.ACCESSIBILITY -> BrowserSettings.default().copy(
                    forceZoom = true,
                    textReflow = true,
                    fontSize = BrowserSettings.FontSize.LARGE
                )
            }
            updateSettings(settings)
        } catch (e: Exception) {
            Napier.e("Error applying preset: ${e.message}", e, tag = TAG)
            Result.failure(e)
        }
    }

    override suspend fun updateState(settings: BrowserSettings) {
        _settings.value = settings
    }

    companion object {
        private const val TAG = "SettingsRepository"
    }
}
