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
import com.augmentalis.voiceoscore.AdaptiveTimingManager
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

        // VoiceIsolation
        private val KEY_VOICE_ISOLATION_ENABLED = booleanPreferencesKey("voice_isolation_enabled")
        private val KEY_VOICE_ISOLATION_NS = booleanPreferencesKey("voice_isolation_noise_suppression")
        private val KEY_VOICE_ISOLATION_AEC = booleanPreferencesKey("voice_isolation_echo_cancellation")
        private val KEY_VOICE_ISOLATION_AGC = booleanPreferencesKey("voice_isolation_agc")
        private val KEY_VOICE_ISOLATION_NS_LEVEL = floatPreferencesKey("voice_isolation_ns_level")
        private val KEY_VOICE_ISOLATION_GAIN_LEVEL = floatPreferencesKey("voice_isolation_gain_level")
        private val KEY_VOICE_ISOLATION_MODE = stringPreferencesKey("voice_isolation_mode")

        // Cockpit
        private val KEY_SHELL_MODE = stringPreferencesKey("shell_mode")
        private val KEY_DEFAULT_ARRANGEMENT = stringPreferencesKey("default_arrangement")
        private val KEY_COCKPIT_MAX_FRAMES = intPreferencesKey("cockpit_max_frames")
        private val KEY_COCKPIT_AUTOSAVE_INTERVAL = stringPreferencesKey("cockpit_autosave_interval")
        private val KEY_COCKPIT_BACKGROUND_SCENE = stringPreferencesKey("cockpit_background_scene")
        private val KEY_COCKPIT_SPATIAL_ENABLED = booleanPreferencesKey("cockpit_spatial_enabled")
        private val KEY_COCKPIT_SPATIAL_SENSITIVITY = stringPreferencesKey("cockpit_spatial_sensitivity")
        private val KEY_COCKPIT_CANVAS_ZOOM_PERSIST = booleanPreferencesKey("cockpit_canvas_zoom_persist")

        // PDFAvanue
        private val KEY_PDF_VIEW_MODE = stringPreferencesKey("pdf_view_mode")
        private val KEY_PDF_NIGHT_MODE = booleanPreferencesKey("pdf_night_mode")
        private val KEY_PDF_DEFAULT_ZOOM = stringPreferencesKey("pdf_default_zoom")
        private val KEY_PDF_REMEMBER_PAGE = booleanPreferencesKey("pdf_remember_page")

        // PhotoAvanue
        private val KEY_CAMERA_DEFAULT_LENS = stringPreferencesKey("camera_default_lens")
        private val KEY_CAMERA_RESOLUTION = stringPreferencesKey("camera_resolution")
        private val KEY_CAMERA_SAVE_PATH = stringPreferencesKey("camera_save_path")
        private val KEY_CAMERA_PRO_DEFAULT = booleanPreferencesKey("camera_pro_default")
        private val KEY_CAMERA_STABILIZATION = stringPreferencesKey("camera_stabilization")
        private val KEY_CAMERA_RAW_ENABLED = booleanPreferencesKey("camera_raw_enabled")

        // VideoAvanue
        private val KEY_VIDEO_DEFAULT_SPEED = stringPreferencesKey("video_default_speed")
        private val KEY_VIDEO_RESUME = booleanPreferencesKey("video_resume")
        private val KEY_VIDEO_REPEAT_MODE = stringPreferencesKey("video_repeat_mode")
        private val KEY_VIDEO_DEFAULT_VOLUME = intPreferencesKey("video_default_volume")
        private val KEY_VIDEO_MUTE_DEFAULT = booleanPreferencesKey("video_mute_default")

        // NoteAvanue
        private val KEY_NOTE_FONT_SIZE = stringPreferencesKey("note_font_size")
        private val KEY_NOTE_AUTOSAVE = stringPreferencesKey("note_autosave")
        private val KEY_NOTE_SPELLCHECK = booleanPreferencesKey("note_spellcheck")
        private val KEY_NOTE_DEFAULT_FORMAT = stringPreferencesKey("note_default_format")

        // FileAvanue
        private val KEY_FILE_SORT_MODE = stringPreferencesKey("file_sort_mode")
        private val KEY_FILE_VIEW_MODE = stringPreferencesKey("file_view_mode")
        private val KEY_FILE_SHOW_HIDDEN = booleanPreferencesKey("file_show_hidden")
        private val KEY_FILE_DEFAULT_PROVIDER = stringPreferencesKey("file_default_provider")

        // RemoteCast
        private val KEY_CAST_JPEG_QUALITY = intPreferencesKey("cast_jpeg_quality")
        private val KEY_CAST_TARGET_FPS = intPreferencesKey("cast_target_fps")
        private val KEY_CAST_RESOLUTION_SCALE = stringPreferencesKey("cast_resolution_scale")
        private val KEY_CAST_PORT = intPreferencesKey("cast_port")
        private val KEY_CAST_AUTO_CONNECT = booleanPreferencesKey("cast_auto_connect")

        // AnnotationAvanue
        private val KEY_ANNOTATION_DEFAULT_TOOL = stringPreferencesKey("annotation_default_tool")
        private val KEY_ANNOTATION_DEFAULT_COLOR = stringPreferencesKey("annotation_default_color")
        private val KEY_ANNOTATION_STROKE_WIDTH = intPreferencesKey("annotation_stroke_width")
        private val KEY_ANNOTATION_TENSION = floatPreferencesKey("annotation_tension")

        // ImageAvanue
        private val KEY_IMAGE_DEFAULT_ZOOM = stringPreferencesKey("image_default_zoom")
        private val KEY_IMAGE_SHOW_EXIF = booleanPreferencesKey("image_show_exif")

        // WebAvanue Extended
        private val KEY_WEB_DEFAULT_ENGINE = stringPreferencesKey("web_default_engine")
        private val KEY_WEB_SEARCH_SUGGESTIONS = booleanPreferencesKey("web_search_suggestions")
        private val KEY_WEB_JAVASCRIPT_ENABLED = booleanPreferencesKey("web_javascript_enabled")
        private val KEY_WEB_COOKIE_MODE = stringPreferencesKey("web_cookie_mode")
        private val KEY_WEB_DO_NOT_TRACK = booleanPreferencesKey("web_do_not_track")
        private val KEY_WEB_TEXT_SIZE = intPreferencesKey("web_text_size")
        private val KEY_WEB_FORCE_DARK = booleanPreferencesKey("web_force_dark")
        private val KEY_WEB_DESKTOP_MODE = booleanPreferencesKey("web_desktop_mode")
        private val KEY_WEB_DOWNLOAD_PATH = stringPreferencesKey("web_download_path")
        private val KEY_WEB_ASK_BEFORE_DOWNLOAD = booleanPreferencesKey("web_ask_before_download")

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
            // VoiceIsolation
            voiceIsolationEnabled = prefs[KEY_VOICE_ISOLATION_ENABLED] ?: AvanuesSettings.DEFAULT_VOICE_ISOLATION_ENABLED,
            voiceIsolationNoiseSuppression = prefs[KEY_VOICE_ISOLATION_NS] ?: true,
            voiceIsolationEchoCancellation = prefs[KEY_VOICE_ISOLATION_AEC] ?: false,
            voiceIsolationAgc = prefs[KEY_VOICE_ISOLATION_AGC] ?: true,
            voiceIsolationNsLevel = prefs[KEY_VOICE_ISOLATION_NS_LEVEL] ?: AvanuesSettings.DEFAULT_VOICE_ISOLATION_NS_LEVEL,
            voiceIsolationGainLevel = prefs[KEY_VOICE_ISOLATION_GAIN_LEVEL] ?: AvanuesSettings.DEFAULT_VOICE_ISOLATION_GAIN_LEVEL,
            voiceIsolationMode = prefs[KEY_VOICE_ISOLATION_MODE] ?: AvanuesSettings.DEFAULT_VOICE_ISOLATION_MODE,
            // Cockpit
            shellMode = prefs[KEY_SHELL_MODE] ?: AvanuesSettings.DEFAULT_SHELL_MODE,
            defaultArrangement = prefs[KEY_DEFAULT_ARRANGEMENT] ?: AvanuesSettings.DEFAULT_ARRANGEMENT,
            cockpitMaxFrames = prefs[KEY_COCKPIT_MAX_FRAMES] ?: AvanuesSettings.DEFAULT_MAX_FRAMES,
            cockpitAutosaveInterval = prefs[KEY_COCKPIT_AUTOSAVE_INTERVAL] ?: AvanuesSettings.DEFAULT_AUTOSAVE_INTERVAL,
            cockpitBackgroundScene = prefs[KEY_COCKPIT_BACKGROUND_SCENE] ?: AvanuesSettings.DEFAULT_BACKGROUND_SCENE,
            cockpitSpatialEnabled = prefs[KEY_COCKPIT_SPATIAL_ENABLED] ?: false,
            cockpitSpatialSensitivity = prefs[KEY_COCKPIT_SPATIAL_SENSITIVITY] ?: AvanuesSettings.DEFAULT_SPATIAL_SENSITIVITY,
            cockpitCanvasZoomPersist = prefs[KEY_COCKPIT_CANVAS_ZOOM_PERSIST] ?: true,
            // PDFAvanue
            pdfViewMode = prefs[KEY_PDF_VIEW_MODE] ?: AvanuesSettings.DEFAULT_PDF_VIEW_MODE,
            pdfNightMode = prefs[KEY_PDF_NIGHT_MODE] ?: false,
            pdfDefaultZoom = prefs[KEY_PDF_DEFAULT_ZOOM] ?: AvanuesSettings.DEFAULT_PDF_ZOOM,
            pdfRememberPage = prefs[KEY_PDF_REMEMBER_PAGE] ?: true,
            // PhotoAvanue
            cameraDefaultLens = prefs[KEY_CAMERA_DEFAULT_LENS] ?: AvanuesSettings.DEFAULT_CAMERA_LENS,
            cameraResolution = prefs[KEY_CAMERA_RESOLUTION] ?: AvanuesSettings.DEFAULT_CAMERA_RESOLUTION,
            cameraSavePath = prefs[KEY_CAMERA_SAVE_PATH] ?: AvanuesSettings.DEFAULT_CAMERA_SAVE_PATH,
            cameraProDefault = prefs[KEY_CAMERA_PRO_DEFAULT] ?: false,
            cameraStabilization = prefs[KEY_CAMERA_STABILIZATION] ?: AvanuesSettings.DEFAULT_CAMERA_STABILIZATION,
            cameraRawEnabled = prefs[KEY_CAMERA_RAW_ENABLED] ?: false,
            // VideoAvanue
            videoDefaultSpeed = prefs[KEY_VIDEO_DEFAULT_SPEED] ?: AvanuesSettings.DEFAULT_VIDEO_SPEED,
            videoResume = prefs[KEY_VIDEO_RESUME] ?: true,
            videoRepeatMode = prefs[KEY_VIDEO_REPEAT_MODE] ?: AvanuesSettings.DEFAULT_VIDEO_REPEAT,
            videoDefaultVolume = prefs[KEY_VIDEO_DEFAULT_VOLUME] ?: AvanuesSettings.DEFAULT_VIDEO_VOLUME,
            videoMuteDefault = prefs[KEY_VIDEO_MUTE_DEFAULT] ?: false,
            // NoteAvanue
            noteFontSize = prefs[KEY_NOTE_FONT_SIZE] ?: AvanuesSettings.DEFAULT_NOTE_FONT_SIZE,
            noteAutosave = prefs[KEY_NOTE_AUTOSAVE] ?: AvanuesSettings.DEFAULT_NOTE_AUTOSAVE,
            noteSpellcheck = prefs[KEY_NOTE_SPELLCHECK] ?: true,
            noteDefaultFormat = prefs[KEY_NOTE_DEFAULT_FORMAT] ?: AvanuesSettings.DEFAULT_NOTE_FORMAT,
            // FileAvanue
            fileSortMode = prefs[KEY_FILE_SORT_MODE] ?: AvanuesSettings.DEFAULT_FILE_SORT,
            fileViewMode = prefs[KEY_FILE_VIEW_MODE] ?: AvanuesSettings.DEFAULT_FILE_VIEW,
            fileShowHidden = prefs[KEY_FILE_SHOW_HIDDEN] ?: false,
            fileDefaultProvider = prefs[KEY_FILE_DEFAULT_PROVIDER] ?: AvanuesSettings.DEFAULT_FILE_PROVIDER,
            // RemoteCast
            castJpegQuality = prefs[KEY_CAST_JPEG_QUALITY] ?: AvanuesSettings.DEFAULT_CAST_JPEG_QUALITY,
            castTargetFps = prefs[KEY_CAST_TARGET_FPS] ?: AvanuesSettings.DEFAULT_CAST_FPS,
            castResolutionScale = prefs[KEY_CAST_RESOLUTION_SCALE] ?: AvanuesSettings.DEFAULT_CAST_RESOLUTION_SCALE,
            castPort = prefs[KEY_CAST_PORT] ?: AvanuesSettings.DEFAULT_CAST_PORT,
            castAutoConnect = prefs[KEY_CAST_AUTO_CONNECT] ?: false,
            // AnnotationAvanue
            annotationDefaultTool = prefs[KEY_ANNOTATION_DEFAULT_TOOL] ?: AvanuesSettings.DEFAULT_ANNOTATION_TOOL,
            annotationDefaultColor = prefs[KEY_ANNOTATION_DEFAULT_COLOR] ?: AvanuesSettings.DEFAULT_ANNOTATION_COLOR,
            annotationStrokeWidth = prefs[KEY_ANNOTATION_STROKE_WIDTH] ?: AvanuesSettings.DEFAULT_ANNOTATION_STROKE_WIDTH,
            annotationTension = prefs[KEY_ANNOTATION_TENSION] ?: AvanuesSettings.DEFAULT_ANNOTATION_TENSION,
            // ImageAvanue
            imageDefaultZoom = prefs[KEY_IMAGE_DEFAULT_ZOOM] ?: AvanuesSettings.DEFAULT_IMAGE_ZOOM,
            imageShowExif = prefs[KEY_IMAGE_SHOW_EXIF] ?: false,
            // WebAvanue Extended
            webDefaultEngine = prefs[KEY_WEB_DEFAULT_ENGINE] ?: AvanuesSettings.DEFAULT_WEB_ENGINE,
            webSearchSuggestions = prefs[KEY_WEB_SEARCH_SUGGESTIONS] ?: true,
            webJavascriptEnabled = prefs[KEY_WEB_JAVASCRIPT_ENABLED] ?: true,
            webCookieMode = prefs[KEY_WEB_COOKIE_MODE] ?: AvanuesSettings.DEFAULT_WEB_COOKIE_MODE,
            webDoNotTrack = prefs[KEY_WEB_DO_NOT_TRACK] ?: false,
            webTextSize = prefs[KEY_WEB_TEXT_SIZE] ?: AvanuesSettings.DEFAULT_WEB_TEXT_SIZE,
            webForceDark = prefs[KEY_WEB_FORCE_DARK] ?: false,
            webDesktopMode = prefs[KEY_WEB_DESKTOP_MODE] ?: false,
            webDownloadPath = prefs[KEY_WEB_DOWNLOAD_PATH] ?: AvanuesSettings.DEFAULT_WEB_DOWNLOAD_PATH,
            webAskBeforeDownload = prefs[KEY_WEB_ASK_BEFORE_DOWNLOAD] ?: true,
            // VOS Sync
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
        // VoiceIsolation
        prefs[KEY_VOICE_ISOLATION_ENABLED] = s.voiceIsolationEnabled
        prefs[KEY_VOICE_ISOLATION_NS] = s.voiceIsolationNoiseSuppression
        prefs[KEY_VOICE_ISOLATION_AEC] = s.voiceIsolationEchoCancellation
        prefs[KEY_VOICE_ISOLATION_AGC] = s.voiceIsolationAgc
        prefs[KEY_VOICE_ISOLATION_NS_LEVEL] = s.voiceIsolationNsLevel
        prefs[KEY_VOICE_ISOLATION_GAIN_LEVEL] = s.voiceIsolationGainLevel
        prefs[KEY_VOICE_ISOLATION_MODE] = s.voiceIsolationMode
        // Cockpit
        prefs[KEY_SHELL_MODE] = s.shellMode
        prefs[KEY_DEFAULT_ARRANGEMENT] = s.defaultArrangement
        prefs[KEY_COCKPIT_MAX_FRAMES] = s.cockpitMaxFrames
        prefs[KEY_COCKPIT_AUTOSAVE_INTERVAL] = s.cockpitAutosaveInterval
        prefs[KEY_COCKPIT_BACKGROUND_SCENE] = s.cockpitBackgroundScene
        prefs[KEY_COCKPIT_SPATIAL_ENABLED] = s.cockpitSpatialEnabled
        prefs[KEY_COCKPIT_SPATIAL_SENSITIVITY] = s.cockpitSpatialSensitivity
        prefs[KEY_COCKPIT_CANVAS_ZOOM_PERSIST] = s.cockpitCanvasZoomPersist
        // PDFAvanue
        prefs[KEY_PDF_VIEW_MODE] = s.pdfViewMode
        prefs[KEY_PDF_NIGHT_MODE] = s.pdfNightMode
        prefs[KEY_PDF_DEFAULT_ZOOM] = s.pdfDefaultZoom
        prefs[KEY_PDF_REMEMBER_PAGE] = s.pdfRememberPage
        // PhotoAvanue
        prefs[KEY_CAMERA_DEFAULT_LENS] = s.cameraDefaultLens
        prefs[KEY_CAMERA_RESOLUTION] = s.cameraResolution
        prefs[KEY_CAMERA_SAVE_PATH] = s.cameraSavePath
        prefs[KEY_CAMERA_PRO_DEFAULT] = s.cameraProDefault
        prefs[KEY_CAMERA_STABILIZATION] = s.cameraStabilization
        prefs[KEY_CAMERA_RAW_ENABLED] = s.cameraRawEnabled
        // VideoAvanue
        prefs[KEY_VIDEO_DEFAULT_SPEED] = s.videoDefaultSpeed
        prefs[KEY_VIDEO_RESUME] = s.videoResume
        prefs[KEY_VIDEO_REPEAT_MODE] = s.videoRepeatMode
        prefs[KEY_VIDEO_DEFAULT_VOLUME] = s.videoDefaultVolume
        prefs[KEY_VIDEO_MUTE_DEFAULT] = s.videoMuteDefault
        // NoteAvanue
        prefs[KEY_NOTE_FONT_SIZE] = s.noteFontSize
        prefs[KEY_NOTE_AUTOSAVE] = s.noteAutosave
        prefs[KEY_NOTE_SPELLCHECK] = s.noteSpellcheck
        prefs[KEY_NOTE_DEFAULT_FORMAT] = s.noteDefaultFormat
        // FileAvanue
        prefs[KEY_FILE_SORT_MODE] = s.fileSortMode
        prefs[KEY_FILE_VIEW_MODE] = s.fileViewMode
        prefs[KEY_FILE_SHOW_HIDDEN] = s.fileShowHidden
        prefs[KEY_FILE_DEFAULT_PROVIDER] = s.fileDefaultProvider
        // RemoteCast
        prefs[KEY_CAST_JPEG_QUALITY] = s.castJpegQuality
        prefs[KEY_CAST_TARGET_FPS] = s.castTargetFps
        prefs[KEY_CAST_RESOLUTION_SCALE] = s.castResolutionScale
        prefs[KEY_CAST_PORT] = s.castPort
        prefs[KEY_CAST_AUTO_CONNECT] = s.castAutoConnect
        // AnnotationAvanue
        prefs[KEY_ANNOTATION_DEFAULT_TOOL] = s.annotationDefaultTool
        prefs[KEY_ANNOTATION_DEFAULT_COLOR] = s.annotationDefaultColor
        prefs[KEY_ANNOTATION_STROKE_WIDTH] = s.annotationStrokeWidth
        prefs[KEY_ANNOTATION_TENSION] = s.annotationTension
        // ImageAvanue
        prefs[KEY_IMAGE_DEFAULT_ZOOM] = s.imageDefaultZoom
        prefs[KEY_IMAGE_SHOW_EXIF] = s.imageShowExif
        // WebAvanue Extended
        prefs[KEY_WEB_DEFAULT_ENGINE] = s.webDefaultEngine
        prefs[KEY_WEB_SEARCH_SUGGESTIONS] = s.webSearchSuggestions
        prefs[KEY_WEB_JAVASCRIPT_ENABLED] = s.webJavascriptEnabled
        prefs[KEY_WEB_COOKIE_MODE] = s.webCookieMode
        prefs[KEY_WEB_DO_NOT_TRACK] = s.webDoNotTrack
        prefs[KEY_WEB_TEXT_SIZE] = s.webTextSize
        prefs[KEY_WEB_FORCE_DARK] = s.webForceDark
        prefs[KEY_WEB_DESKTOP_MODE] = s.webDesktopMode
        prefs[KEY_WEB_DOWNLOAD_PATH] = s.webDownloadPath
        prefs[KEY_WEB_ASK_BEFORE_DOWNLOAD] = s.webAskBeforeDownload
        // VOS Sync
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

    // ==================== Cockpit Settings ====================

    suspend fun updateShellMode(mode: String) {
        context.avanuesDataStore.edit { it[KEY_SHELL_MODE] = mode }
    }

    suspend fun updateDefaultArrangement(arrangement: String) {
        context.avanuesDataStore.edit { it[KEY_DEFAULT_ARRANGEMENT] = arrangement }
    }

    suspend fun updateCockpitMaxFrames(max: Int) {
        context.avanuesDataStore.edit { it[KEY_COCKPIT_MAX_FRAMES] = max.coerceIn(1, 12) }
    }

    suspend fun updateCockpitAutosaveInterval(interval: String) {
        context.avanuesDataStore.edit { it[KEY_COCKPIT_AUTOSAVE_INTERVAL] = interval }
    }

    suspend fun updateCockpitBackgroundScene(scene: String) {
        context.avanuesDataStore.edit { it[KEY_COCKPIT_BACKGROUND_SCENE] = scene }
    }

    suspend fun updateCockpitSpatialEnabled(enabled: Boolean) {
        context.avanuesDataStore.edit { it[KEY_COCKPIT_SPATIAL_ENABLED] = enabled }
    }

    suspend fun updateCockpitSpatialSensitivity(sensitivity: String) {
        context.avanuesDataStore.edit { it[KEY_COCKPIT_SPATIAL_SENSITIVITY] = sensitivity }
    }

    suspend fun updateCockpitCanvasZoomPersist(persist: Boolean) {
        context.avanuesDataStore.edit { it[KEY_COCKPIT_CANVAS_ZOOM_PERSIST] = persist }
    }

    // ==================== PDF Settings ====================

    suspend fun updatePdfViewMode(mode: String) {
        context.avanuesDataStore.edit { it[KEY_PDF_VIEW_MODE] = mode }
    }

    suspend fun updatePdfNightMode(enabled: Boolean) {
        context.avanuesDataStore.edit { it[KEY_PDF_NIGHT_MODE] = enabled }
    }

    suspend fun updatePdfDefaultZoom(zoom: String) {
        context.avanuesDataStore.edit { it[KEY_PDF_DEFAULT_ZOOM] = zoom }
    }

    suspend fun updatePdfRememberPage(enabled: Boolean) {
        context.avanuesDataStore.edit { it[KEY_PDF_REMEMBER_PAGE] = enabled }
    }

    // ==================== Camera Settings ====================

    suspend fun updateCameraDefaultLens(lens: String) {
        context.avanuesDataStore.edit { it[KEY_CAMERA_DEFAULT_LENS] = lens }
    }

    suspend fun updateCameraResolution(resolution: String) {
        context.avanuesDataStore.edit { it[KEY_CAMERA_RESOLUTION] = resolution }
    }

    suspend fun updateCameraSavePath(path: String) {
        context.avanuesDataStore.edit { it[KEY_CAMERA_SAVE_PATH] = path }
    }

    suspend fun updateCameraProDefault(enabled: Boolean) {
        context.avanuesDataStore.edit { it[KEY_CAMERA_PRO_DEFAULT] = enabled }
    }

    suspend fun updateCameraStabilization(mode: String) {
        context.avanuesDataStore.edit { it[KEY_CAMERA_STABILIZATION] = mode }
    }

    suspend fun updateCameraRawEnabled(enabled: Boolean) {
        context.avanuesDataStore.edit { it[KEY_CAMERA_RAW_ENABLED] = enabled }
    }

    // ==================== Video Settings ====================

    suspend fun updateVideoDefaultSpeed(speed: String) {
        context.avanuesDataStore.edit { it[KEY_VIDEO_DEFAULT_SPEED] = speed }
    }

    suspend fun updateVideoResume(enabled: Boolean) {
        context.avanuesDataStore.edit { it[KEY_VIDEO_RESUME] = enabled }
    }

    suspend fun updateVideoRepeatMode(mode: String) {
        context.avanuesDataStore.edit { it[KEY_VIDEO_REPEAT_MODE] = mode }
    }

    suspend fun updateVideoDefaultVolume(volume: Int) {
        context.avanuesDataStore.edit { it[KEY_VIDEO_DEFAULT_VOLUME] = volume.coerceIn(0, 100) }
    }

    suspend fun updateVideoMuteDefault(enabled: Boolean) {
        context.avanuesDataStore.edit { it[KEY_VIDEO_MUTE_DEFAULT] = enabled }
    }

    // ==================== Note Settings ====================

    suspend fun updateNoteFontSize(size: String) {
        context.avanuesDataStore.edit { it[KEY_NOTE_FONT_SIZE] = size }
    }

    suspend fun updateNoteAutosave(interval: String) {
        context.avanuesDataStore.edit { it[KEY_NOTE_AUTOSAVE] = interval }
    }

    suspend fun updateNoteSpellcheck(enabled: Boolean) {
        context.avanuesDataStore.edit { it[KEY_NOTE_SPELLCHECK] = enabled }
    }

    suspend fun updateNoteDefaultFormat(format: String) {
        context.avanuesDataStore.edit { it[KEY_NOTE_DEFAULT_FORMAT] = format }
    }

    // ==================== File Settings ====================

    suspend fun updateFileSortMode(mode: String) {
        context.avanuesDataStore.edit { it[KEY_FILE_SORT_MODE] = mode }
    }

    suspend fun updateFileViewMode(mode: String) {
        context.avanuesDataStore.edit { it[KEY_FILE_VIEW_MODE] = mode }
    }

    suspend fun updateFileShowHidden(show: Boolean) {
        context.avanuesDataStore.edit { it[KEY_FILE_SHOW_HIDDEN] = show }
    }

    suspend fun updateFileDefaultProvider(provider: String) {
        context.avanuesDataStore.edit { it[KEY_FILE_DEFAULT_PROVIDER] = provider }
    }

    // ==================== RemoteCast Settings ====================

    suspend fun updateCastJpegQuality(quality: Int) {
        context.avanuesDataStore.edit { it[KEY_CAST_JPEG_QUALITY] = quality.coerceIn(30, 100) }
    }

    suspend fun updateCastTargetFps(fps: Int) {
        context.avanuesDataStore.edit { it[KEY_CAST_TARGET_FPS] = fps.coerceIn(5, 60) }
    }

    suspend fun updateCastResolutionScale(scale: String) {
        context.avanuesDataStore.edit { it[KEY_CAST_RESOLUTION_SCALE] = scale }
    }

    suspend fun updateCastPort(port: Int) {
        context.avanuesDataStore.edit { it[KEY_CAST_PORT] = port.coerceIn(1024, 65535) }
    }

    suspend fun updateCastAutoConnect(enabled: Boolean) {
        context.avanuesDataStore.edit { it[KEY_CAST_AUTO_CONNECT] = enabled }
    }

    // ==================== Annotation Settings ====================

    suspend fun updateAnnotationDefaultTool(tool: String) {
        context.avanuesDataStore.edit { it[KEY_ANNOTATION_DEFAULT_TOOL] = tool }
    }

    suspend fun updateAnnotationDefaultColor(color: String) {
        context.avanuesDataStore.edit { it[KEY_ANNOTATION_DEFAULT_COLOR] = color }
    }

    suspend fun updateAnnotationStrokeWidth(width: Int) {
        context.avanuesDataStore.edit { it[KEY_ANNOTATION_STROKE_WIDTH] = width.coerceIn(1, 20) }
    }

    suspend fun updateAnnotationTension(tension: Float) {
        context.avanuesDataStore.edit { it[KEY_ANNOTATION_TENSION] = tension.coerceIn(0f, 1f) }
    }

    // ==================== Image Settings ====================

    suspend fun updateImageDefaultZoom(zoom: String) {
        context.avanuesDataStore.edit { it[KEY_IMAGE_DEFAULT_ZOOM] = zoom }
    }

    suspend fun updateImageShowExif(show: Boolean) {
        context.avanuesDataStore.edit { it[KEY_IMAGE_SHOW_EXIF] = show }
    }

    // ==================== WebAvanue Extended Settings ====================

    suspend fun updateWebDefaultEngine(engine: String) {
        context.avanuesDataStore.edit { it[KEY_WEB_DEFAULT_ENGINE] = engine }
    }

    suspend fun updateWebSearchSuggestions(enabled: Boolean) {
        context.avanuesDataStore.edit { it[KEY_WEB_SEARCH_SUGGESTIONS] = enabled }
    }

    suspend fun updateWebJavascriptEnabled(enabled: Boolean) {
        context.avanuesDataStore.edit { it[KEY_WEB_JAVASCRIPT_ENABLED] = enabled }
    }

    suspend fun updateWebCookieMode(mode: String) {
        context.avanuesDataStore.edit { it[KEY_WEB_COOKIE_MODE] = mode }
    }

    suspend fun updateWebDoNotTrack(enabled: Boolean) {
        context.avanuesDataStore.edit { it[KEY_WEB_DO_NOT_TRACK] = enabled }
    }

    suspend fun updateWebTextSize(size: Int) {
        context.avanuesDataStore.edit { it[KEY_WEB_TEXT_SIZE] = size.coerceIn(50, 200) }
    }

    suspend fun updateWebForceDark(enabled: Boolean) {
        context.avanuesDataStore.edit { it[KEY_WEB_FORCE_DARK] = enabled }
    }

    suspend fun updateWebDesktopMode(enabled: Boolean) {
        context.avanuesDataStore.edit { it[KEY_WEB_DESKTOP_MODE] = enabled }
    }

    suspend fun updateWebDownloadPath(path: String) {
        context.avanuesDataStore.edit { it[KEY_WEB_DOWNLOAD_PATH] = path }
    }

    suspend fun updateWebAskBeforeDownload(ask: Boolean) {
        context.avanuesDataStore.edit { it[KEY_WEB_ASK_BEFORE_DOWNLOAD] = ask }
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

    // ==================== VoiceIsolation Settings ====================
    // TODO: Wire these to VoiceIsolation.updateConfig() in:
    //   - VoiceAvanueAccessibilityService (observe settings → apply config)
    //   - SpeechRecognitionService (apply before audio processing)
    //   - Any app that uses speech recognition with audio preprocessing

    suspend fun updateVoiceIsolationEnabled(enabled: Boolean) {
        context.avanuesDataStore.edit { it[KEY_VOICE_ISOLATION_ENABLED] = enabled }
    }

    suspend fun updateVoiceIsolationNoiseSuppression(enabled: Boolean) {
        context.avanuesDataStore.edit { it[KEY_VOICE_ISOLATION_NS] = enabled }
    }

    suspend fun updateVoiceIsolationEchoCancellation(enabled: Boolean) {
        context.avanuesDataStore.edit { it[KEY_VOICE_ISOLATION_AEC] = enabled }
    }

    suspend fun updateVoiceIsolationAgc(enabled: Boolean) {
        context.avanuesDataStore.edit { it[KEY_VOICE_ISOLATION_AGC] = enabled }
    }

    suspend fun updateVoiceIsolationNsLevel(level: Float) {
        context.avanuesDataStore.edit { it[KEY_VOICE_ISOLATION_NS_LEVEL] = level.coerceIn(0f, 1f) }
    }

    suspend fun updateVoiceIsolationGainLevel(level: Float) {
        context.avanuesDataStore.edit { it[KEY_VOICE_ISOLATION_GAIN_LEVEL] = level.coerceIn(0f, 1f) }
    }

    suspend fun updateVoiceIsolationMode(mode: String) {
        context.avanuesDataStore.edit { it[KEY_VOICE_ISOLATION_MODE] = mode }
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
        context.avanuesDataStore.data.first().let { prefs ->
            val map = mutableMapOf<String, Long>()
            prefs[KEY_ADAPTIVE_PROCESSING_DELAY]?.let { map[AdaptiveTimingManager.Keys.PROCESSING_DELAY] = it }
            prefs[KEY_ADAPTIVE_SCROLL_DEBOUNCE]?.let { map[AdaptiveTimingManager.Keys.SCROLL_DEBOUNCE] = it }
            prefs[KEY_ADAPTIVE_SPEECH_UPDATE_DEBOUNCE]?.let { map[AdaptiveTimingManager.Keys.SPEECH_UPDATE_DEBOUNCE] = it }
            prefs[KEY_ADAPTIVE_COMMAND_WINDOW]?.let { map[AdaptiveTimingManager.Keys.COMMAND_WINDOW] = it }
            if (map.isNotEmpty()) {
                AdaptiveTimingManager.applyPersistedValues(map)
            }
        }
    }

    /**
     * Persist current AdaptiveTimingManager learned values to DataStore.
     * Call periodically (e.g., every 60s) or on app pause/stop.
     */
    suspend fun persistAdaptiveTimingValues() {
        val values = AdaptiveTimingManager.toPersistedMap()
        context.avanuesDataStore.edit { prefs ->
            // toPersistedMap() returns non-nullable values; direct assignment is safe
            prefs[KEY_ADAPTIVE_PROCESSING_DELAY] = values.getValue(AdaptiveTimingManager.Keys.PROCESSING_DELAY)
            prefs[KEY_ADAPTIVE_SCROLL_DEBOUNCE] = values.getValue(AdaptiveTimingManager.Keys.SCROLL_DEBOUNCE)
            prefs[KEY_ADAPTIVE_SPEECH_UPDATE_DEBOUNCE] = values.getValue(AdaptiveTimingManager.Keys.SPEECH_UPDATE_DEBOUNCE)
            prefs[KEY_ADAPTIVE_COMMAND_WINDOW] = values.getValue(AdaptiveTimingManager.Keys.COMMAND_WINDOW)
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
