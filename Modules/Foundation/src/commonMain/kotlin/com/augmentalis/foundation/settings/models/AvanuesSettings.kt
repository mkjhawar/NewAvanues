/**
 * AvanuesSettings.kt - Cross-platform settings data model
 *
 * Pure Kotlin data class representing all Avanues app-level settings.
 * Platform-specific persistence (DataStore on Android, UserDefaults on iOS)
 * reads/writes this model through [ISettingsStore].
 *
 * Theme defaults are string constants (not AvanueUI enum references)
 * to keep Foundation free of AvanueUI dependency.
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 */
package com.augmentalis.foundation.settings.models

/**
 * All Avanues app-level settings.
 *
 * This data class is the single source of truth for settings shape and defaults.
 * It lives in Foundation commonMain so Android, iOS, Desktop, and Web can all
 * use the same model.
 */
data class AvanuesSettings(
    // Cursor
    val cursorEnabled: Boolean = false,
    val dwellClickEnabled: Boolean = true,
    val dwellClickDelayMs: Float = DEFAULT_DWELL_CLICK_DELAY_MS,
    val cursorSmoothing: Boolean = true,
    val cursorSize: Int = DEFAULT_CURSOR_SIZE,
    val cursorSpeed: Int = DEFAULT_CURSOR_SPEED,
    val showCoordinates: Boolean = false,
    val cursorAccentOverride: Long? = null,

    // Voice
    val voiceFeedback: Boolean = true,
    val voiceLocale: String = DEFAULT_VOICE_LOCALE,

    // Wake Word
    val wakeWordEnabled: Boolean = false,
    val wakeWordKeyword: String = DEFAULT_WAKE_WORD_KEYWORD,
    val wakeWordSensitivity: Float = DEFAULT_WAKE_WORD_SENSITIVITY,

    // Boot
    val autoStartOnBoot: Boolean = false,

    // Theme v5.1: decoupled palette + style + appearance
    val themePalette: String = DEFAULT_THEME_PALETTE,
    val themeStyle: String = DEFAULT_THEME_STYLE,
    val themeAppearance: String = DEFAULT_THEME_APPEARANCE,

    // Cockpit
    val shellMode: String = DEFAULT_SHELL_MODE,
    val defaultArrangement: String = DEFAULT_ARRANGEMENT,
    val cockpitMaxFrames: Int = DEFAULT_MAX_FRAMES,
    val cockpitAutosaveInterval: String = DEFAULT_AUTOSAVE_INTERVAL,
    val cockpitBackgroundScene: String = DEFAULT_BACKGROUND_SCENE,
    val cockpitSpatialEnabled: Boolean = false,
    val cockpitSpatialSensitivity: String = DEFAULT_SPATIAL_SENSITIVITY,
    val cockpitCanvasZoomPersist: Boolean = true,

    // PDFAvanue
    val pdfViewMode: String = DEFAULT_PDF_VIEW_MODE,
    val pdfNightMode: Boolean = false,
    val pdfDefaultZoom: String = DEFAULT_PDF_ZOOM,
    val pdfRememberPage: Boolean = true,

    // PhotoAvanue
    val cameraDefaultLens: String = DEFAULT_CAMERA_LENS,
    val cameraResolution: String = DEFAULT_CAMERA_RESOLUTION,
    val cameraSavePath: String = DEFAULT_CAMERA_SAVE_PATH,
    val cameraProDefault: Boolean = false,
    val cameraStabilization: String = DEFAULT_CAMERA_STABILIZATION,
    val cameraRawEnabled: Boolean = false,

    // VideoAvanue
    val videoDefaultSpeed: String = DEFAULT_VIDEO_SPEED,
    val videoResume: Boolean = true,
    val videoRepeatMode: String = DEFAULT_VIDEO_REPEAT,
    val videoDefaultVolume: Int = DEFAULT_VIDEO_VOLUME,
    val videoMuteDefault: Boolean = false,

    // NoteAvanue
    val noteFontSize: String = DEFAULT_NOTE_FONT_SIZE,
    val noteAutosave: String = DEFAULT_NOTE_AUTOSAVE,
    val noteSpellcheck: Boolean = true,
    val noteDefaultFormat: String = DEFAULT_NOTE_FORMAT,

    // FileAvanue
    val fileSortMode: String = DEFAULT_FILE_SORT,
    val fileViewMode: String = DEFAULT_FILE_VIEW,
    val fileShowHidden: Boolean = false,
    val fileDefaultProvider: String = DEFAULT_FILE_PROVIDER,

    // RemoteCast
    val castJpegQuality: Int = DEFAULT_CAST_JPEG_QUALITY,
    val castTargetFps: Int = DEFAULT_CAST_FPS,
    val castResolutionScale: String = DEFAULT_CAST_RESOLUTION_SCALE,
    val castPort: Int = DEFAULT_CAST_PORT,
    val castAutoConnect: Boolean = false,

    // AnnotationAvanue
    val annotationDefaultTool: String = DEFAULT_ANNOTATION_TOOL,
    val annotationDefaultColor: String = DEFAULT_ANNOTATION_COLOR,
    val annotationStrokeWidth: Int = DEFAULT_ANNOTATION_STROKE_WIDTH,
    val annotationTension: Float = DEFAULT_ANNOTATION_TENSION,

    // ImageAvanue
    val imageDefaultZoom: String = DEFAULT_IMAGE_ZOOM,
    val imageShowExif: Boolean = false,

    // WebAvanue Extended
    val webDefaultEngine: String = DEFAULT_WEB_ENGINE,
    val webSearchSuggestions: Boolean = true,
    val webJavascriptEnabled: Boolean = true,
    val webCookieMode: String = DEFAULT_WEB_COOKIE_MODE,
    val webDoNotTrack: Boolean = false,
    val webTextSize: Int = DEFAULT_WEB_TEXT_SIZE,
    val webForceDark: Boolean = false,
    val webDesktopMode: Boolean = false,
    val webDownloadPath: String = DEFAULT_WEB_DOWNLOAD_PATH,
    val webAskBeforeDownload: Boolean = true,

    // VoiceIsolation — audio preprocessing for speech recognition
    // NOTE: Apps must wire these settings to VoiceIsolation.updateConfig()
    // when settings change. See VoiceIsolationConfig for field descriptions.
    // Integration needed in: VoiceOSAccessibilityService, SpeechRecognitionService
    val voiceIsolationEnabled: Boolean = DEFAULT_VOICE_ISOLATION_ENABLED,
    val voiceIsolationNoiseSuppression: Boolean = true,
    val voiceIsolationEchoCancellation: Boolean = false,
    val voiceIsolationAgc: Boolean = true,
    val voiceIsolationNsLevel: Float = DEFAULT_VOICE_ISOLATION_NS_LEVEL,
    val voiceIsolationGainLevel: Float = DEFAULT_VOICE_ISOLATION_GAIN_LEVEL,
    val voiceIsolationMode: String = DEFAULT_VOICE_ISOLATION_MODE,

    // VOS Sync (Developer)
    val vosSyncEnabled: Boolean = false,
    val vosSftpHost: String = "",
    val vosSftpPort: Int = DEFAULT_SFTP_PORT,
    val vosSftpUsername: String = "",
    val vosSftpRemotePath: String = DEFAULT_SFTP_REMOTE_PATH,
    val vosSftpKeyPath: String = "",
    val vosLastSyncTime: Long? = null,
    val vosSftpHostKeyMode: String = DEFAULT_HOST_KEY_MODE,
    val vosAutoSyncEnabled: Boolean = false,
    val vosSyncIntervalHours: Int = DEFAULT_SYNC_INTERVAL_HOURS
) {
    companion object {
        // Theme defaults (string values, not AvanueUI enum references)
        const val DEFAULT_THEME_PALETTE = "HYDRA"
        const val DEFAULT_THEME_STYLE = "Water"
        const val DEFAULT_THEME_APPEARANCE = "Auto"

        // Cursor defaults
        const val DEFAULT_CURSOR_SIZE = 48
        const val DEFAULT_CURSOR_SPEED = 8
        const val DEFAULT_DWELL_CLICK_DELAY_MS = 1500f

        // Voice defaults
        const val DEFAULT_VOICE_LOCALE = "en-US"

        // Wake Word defaults
        const val DEFAULT_WAKE_WORD_KEYWORD = "HEY_AVA"
        const val DEFAULT_WAKE_WORD_SENSITIVITY = 0.5f

        // Cockpit defaults
        const val DEFAULT_SHELL_MODE = "LENS"
        const val DEFAULT_ARRANGEMENT = "FOCUS"
        const val DEFAULT_MAX_FRAMES = 6
        const val DEFAULT_AUTOSAVE_INTERVAL = "1m"
        const val DEFAULT_BACKGROUND_SCENE = "GRADIENT"
        const val DEFAULT_SPATIAL_SENSITIVITY = "NORMAL"

        // PDFAvanue defaults
        const val DEFAULT_PDF_VIEW_MODE = "Continuous"
        const val DEFAULT_PDF_ZOOM = "FitWidth"

        // PhotoAvanue defaults
        const val DEFAULT_CAMERA_LENS = "Back"
        const val DEFAULT_CAMERA_RESOLUTION = "Auto"
        const val DEFAULT_CAMERA_SAVE_PATH = "DCIM"
        const val DEFAULT_CAMERA_STABILIZATION = "Standard"

        // VideoAvanue defaults
        const val DEFAULT_VIDEO_SPEED = "1.0"
        const val DEFAULT_VIDEO_REPEAT = "Off"
        const val DEFAULT_VIDEO_VOLUME = 100

        // NoteAvanue defaults
        const val DEFAULT_NOTE_FONT_SIZE = "Medium"
        const val DEFAULT_NOTE_AUTOSAVE = "15s"
        const val DEFAULT_NOTE_FORMAT = "Markdown"

        // FileAvanue defaults
        const val DEFAULT_FILE_SORT = "Name"
        const val DEFAULT_FILE_VIEW = "List"
        const val DEFAULT_FILE_PROVIDER = "Local"

        // RemoteCast defaults
        const val DEFAULT_CAST_JPEG_QUALITY = 60
        const val DEFAULT_CAST_FPS = 15
        const val DEFAULT_CAST_RESOLUTION_SCALE = "75"
        const val DEFAULT_CAST_PORT = 54321

        // AnnotationAvanue defaults
        const val DEFAULT_ANNOTATION_TOOL = "Pen"
        const val DEFAULT_ANNOTATION_COLOR = "#FFFFFF"
        const val DEFAULT_ANNOTATION_STROKE_WIDTH = 4
        const val DEFAULT_ANNOTATION_TENSION = 0.3f

        // ImageAvanue defaults
        const val DEFAULT_IMAGE_ZOOM = "Fit"

        // WebAvanue defaults
        const val DEFAULT_WEB_ENGINE = "Google"
        const val DEFAULT_WEB_COOKIE_MODE = "AcceptAll"
        const val DEFAULT_WEB_TEXT_SIZE = 100
        const val DEFAULT_WEB_DOWNLOAD_PATH = "Downloads"

        // VoiceIsolation defaults
        const val DEFAULT_VOICE_ISOLATION_ENABLED = true
        const val DEFAULT_VOICE_ISOLATION_NS_LEVEL = 0.7f
        const val DEFAULT_VOICE_ISOLATION_GAIN_LEVEL = 0.5f
        const val DEFAULT_VOICE_ISOLATION_MODE = "BALANCED"

        // VOS Sync defaults
        const val DEFAULT_SFTP_PORT = 22
        const val DEFAULT_SFTP_REMOTE_PATH = "/vos"
        const val DEFAULT_HOST_KEY_MODE = "accept-new"
        const val DEFAULT_SYNC_INTERVAL_HOURS = 4
    }
}

/**
 * Persisted user synonym: canonical verb -> list of alternatives.
 * AVU unit type: SYN (Synonym mapping).
 */
data class PersistedSynonym(
    val canonical: String,
    val synonyms: List<String>
)
