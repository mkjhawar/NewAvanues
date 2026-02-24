/**
 * SettingsKeys.kt - Cross-platform settings key name constants
 *
 * All DataStore / UserDefaults / preference key names in one place.
 * Platform-specific persistence layers use these constants to ensure
 * consistent key naming across Android, iOS, and Desktop.
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 */
package com.augmentalis.foundation.settings

/**
 * String constants for all settings persistence keys.
 *
 * Organized by category. Each constant is the raw string key name
 * used in platform-specific storage (DataStore, UserDefaults, etc.).
 */
object SettingsKeys {

    // ══════════════════════════════════════════
    // Cursor Settings
    // ══════════════════════════════════════════
    const val CURSOR_ENABLED = "cursor_enabled"
    const val DWELL_CLICK_ENABLED = "dwell_click_enabled"
    const val DWELL_CLICK_DELAY_MS = "dwell_click_delay_ms"
    const val CURSOR_SMOOTHING = "cursor_smoothing"
    const val CURSOR_SIZE = "cursor_size"
    const val CURSOR_SPEED = "cursor_speed"
    const val SHOW_COORDINATES = "show_coordinates"
    const val CURSOR_ACCENT_OVERRIDE = "cursor_accent_override"

    // ══════════════════════════════════════════
    // Voice Settings
    // ══════════════════════════════════════════
    const val VOICE_FEEDBACK = "voice_feedback"
    const val VOICE_COMMAND_LOCALE = "voice_command_locale"

    // ══════════════════════════════════════════
    // Wake Word Settings
    // ══════════════════════════════════════════
    const val WAKE_WORD_ENABLED = "wake_word_enabled"
    const val WAKE_WORD_KEYWORD = "wake_word_keyword"
    const val WAKE_WORD_SENSITIVITY = "wake_word_sensitivity"

    // ══════════════════════════════════════════
    // Boot / Lifecycle
    // ══════════════════════════════════════════
    const val AUTO_START_ON_BOOT = "auto_start_on_boot"

    // ══════════════════════════════════════════
    // Theme v5.1 (Decoupled Palette + Style + Appearance)
    // ══════════════════════════════════════════
    const val THEME_PALETTE = "theme_palette"
    const val THEME_STYLE = "theme_style"
    const val THEME_APPEARANCE = "theme_appearance"
    const val THEME_VARIANT_LEGACY = "theme_variant"

    // ══════════════════════════════════════════
    // VOS Sync (Developer)
    // ══════════════════════════════════════════
    const val VOS_SYNC_ENABLED = "vos_sync_enabled"
    const val VOS_SFTP_HOST = "vos_sftp_host"
    const val VOS_SFTP_PORT = "vos_sftp_port"
    const val VOS_SFTP_USERNAME = "vos_sftp_username"
    const val VOS_SFTP_REMOTE_PATH = "vos_sftp_remote_path"
    const val VOS_SFTP_KEY_PATH = "vos_sftp_key_path"
    const val VOS_LAST_SYNC_TIME = "vos_last_sync_time"
    const val VOS_SFTP_HOST_KEY_MODE = "vos_sftp_host_key_mode"
    const val VOS_AUTO_SYNC_ENABLED = "vos_auto_sync_enabled"
    const val VOS_SYNC_INTERVAL_HOURS = "vos_sync_interval_hours"

    // ══════════════════════════════════════════
    // Voice Command Persistence
    // ══════════════════════════════════════════
    const val DISABLED_COMMANDS = "vcm_disabled_commands"
    const val USER_SYNONYMS = "vcm_user_synonyms"

    // ══════════════════════════════════════════
    // Adaptive Timing (learned by AdaptiveTimingManager)
    // ══════════════════════════════════════════
    const val ADAPTIVE_PROCESSING_DELAY_MS = "adaptive_processing_delay_ms"
    const val ADAPTIVE_SCROLL_DEBOUNCE_MS = "adaptive_scroll_debounce_ms"
    const val ADAPTIVE_SPEECH_UPDATE_DEBOUNCE_MS = "adaptive_speech_update_debounce_ms"
    const val ADAPTIVE_COMMAND_WINDOW_MS = "adaptive_command_window_ms"

    // ══════════════════════════════════════════
    // Cockpit Settings
    // ══════════════════════════════════════════
    const val SHELL_MODE = "shell_mode"
    const val DEFAULT_ARRANGEMENT = "default_arrangement"
    const val COCKPIT_MAX_FRAMES = "cockpit_max_frames"
    const val COCKPIT_AUTOSAVE_INTERVAL = "cockpit_autosave_interval"
    const val COCKPIT_BACKGROUND_SCENE = "cockpit_background_scene"
    const val COCKPIT_SPATIAL_ENABLED = "cockpit_spatial_enabled"
    const val COCKPIT_SPATIAL_SENSITIVITY = "cockpit_spatial_sensitivity"
    const val COCKPIT_CANVAS_ZOOM_PERSIST = "cockpit_canvas_zoom_persist"

    // ══════════════════════════════════════════
    // PDFAvanue Settings
    // ══════════════════════════════════════════
    const val PDF_VIEW_MODE = "pdf_view_mode"
    const val PDF_NIGHT_MODE = "pdf_night_mode"
    const val PDF_DEFAULT_ZOOM = "pdf_default_zoom"
    const val PDF_REMEMBER_PAGE = "pdf_remember_page"

    // ══════════════════════════════════════════
    // PhotoAvanue Settings
    // ══════════════════════════════════════════
    const val CAMERA_DEFAULT_LENS = "camera_default_lens"
    const val CAMERA_RESOLUTION = "camera_resolution"
    const val CAMERA_SAVE_PATH = "camera_save_path"
    const val CAMERA_PRO_DEFAULT = "camera_pro_default"
    const val CAMERA_STABILIZATION = "camera_stabilization"
    const val CAMERA_RAW_ENABLED = "camera_raw_enabled"

    // ══════════════════════════════════════════
    // VideoAvanue Settings
    // ══════════════════════════════════════════
    const val VIDEO_DEFAULT_SPEED = "video_default_speed"
    const val VIDEO_RESUME = "video_resume"
    const val VIDEO_REPEAT_MODE = "video_repeat_mode"
    const val VIDEO_DEFAULT_VOLUME = "video_default_volume"
    const val VIDEO_MUTE_DEFAULT = "video_mute_default"

    // ══════════════════════════════════════════
    // NoteAvanue Settings
    // ══════════════════════════════════════════
    const val NOTE_FONT_SIZE = "note_font_size"
    const val NOTE_AUTOSAVE = "note_autosave"
    const val NOTE_SPELLCHECK = "note_spellcheck"
    const val NOTE_DEFAULT_FORMAT = "note_default_format"

    // ══════════════════════════════════════════
    // FileAvanue Settings
    // ══════════════════════════════════════════
    const val FILE_SORT_MODE = "file_sort_mode"
    const val FILE_VIEW_MODE = "file_view_mode"
    const val FILE_SHOW_HIDDEN = "file_show_hidden"
    const val FILE_DEFAULT_PROVIDER = "file_default_provider"

    // ══════════════════════════════════════════
    // RemoteCast Settings
    // ══════════════════════════════════════════
    const val CAST_JPEG_QUALITY = "cast_jpeg_quality"
    const val CAST_TARGET_FPS = "cast_target_fps"
    const val CAST_RESOLUTION_SCALE = "cast_resolution_scale"
    const val CAST_PORT = "cast_port"
    const val CAST_AUTO_CONNECT = "cast_auto_connect"

    // ══════════════════════════════════════════
    // AnnotationAvanue Settings
    // ══════════════════════════════════════════
    const val ANNOTATION_DEFAULT_TOOL = "annotation_default_tool"
    const val ANNOTATION_DEFAULT_COLOR = "annotation_default_color"
    const val ANNOTATION_STROKE_WIDTH = "annotation_stroke_width"
    const val ANNOTATION_TENSION = "annotation_tension"

    // ══════════════════════════════════════════
    // ImageAvanue Settings
    // ══════════════════════════════════════════
    const val IMAGE_DEFAULT_ZOOM = "image_default_zoom"
    const val IMAGE_SHOW_EXIF = "image_show_exif"

    // ══════════════════════════════════════════
    // WebAvanue Extended Settings
    // ══════════════════════════════════════════
    const val WEB_DEFAULT_ENGINE = "web_default_engine"
    const val WEB_SEARCH_SUGGESTIONS = "web_search_suggestions"
    const val WEB_JAVASCRIPT_ENABLED = "web_javascript_enabled"
    const val WEB_COOKIE_MODE = "web_cookie_mode"
    const val WEB_DO_NOT_TRACK = "web_do_not_track"
    const val WEB_TEXT_SIZE = "web_text_size"
    const val WEB_FORCE_DARK = "web_force_dark"
    const val WEB_DESKTOP_MODE = "web_desktop_mode"
    const val WEB_DOWNLOAD_PATH = "web_download_path"
    const val WEB_ASK_BEFORE_DOWNLOAD = "web_ask_before_download"

    // ══════════════════════════════════════════
    // VoiceIsolation Settings
    // ══════════════════════════════════════════
    const val VOICE_ISOLATION_ENABLED = "voice_isolation_enabled"
    const val VOICE_ISOLATION_NOISE_SUPPRESSION = "voice_isolation_noise_suppression"
    const val VOICE_ISOLATION_ECHO_CANCELLATION = "voice_isolation_echo_cancellation"
    const val VOICE_ISOLATION_AGC = "voice_isolation_agc"
    const val VOICE_ISOLATION_NS_LEVEL = "voice_isolation_ns_level"
    const val VOICE_ISOLATION_GAIN_LEVEL = "voice_isolation_gain_level"
    const val VOICE_ISOLATION_MODE = "voice_isolation_mode"

    // ══════════════════════════════════════════
    // Developer Settings
    // ══════════════════════════════════════════
    const val DEV_STT_TIMEOUT_MS = "dev_stt_timeout_ms"
    const val DEV_END_OF_SPEECH_DELAY_MS = "dev_end_of_speech_delay_ms"
    const val DEV_PARTIAL_RESULT_INTERVAL_MS = "dev_partial_result_interval_ms"
    const val DEV_CONFIDENCE_THRESHOLD = "dev_confidence_threshold"
    const val DEV_DEBUG_MODE = "dev_debug_mode"
    const val DEV_VERBOSE_LOGGING = "dev_verbose_logging"
    const val DEV_DEBUG_OVERLAY = "dev_debug_overlay"
    const val DEV_SCANNER_VERBOSITY = "dev_scanner_verbosity"
    const val DEV_AUTO_START_LISTENING = "dev_auto_start_listening"
    const val DEV_SYNONYMS_ENABLED = "dev_synonyms_enabled"
    const val DEV_STT_ENGINE = "dev_stt_engine"
    const val DEV_VOICE_LANGUAGE = "dev_voice_language"
    const val DEV_CONTENT_CHANGE_DEBOUNCE_MS = "dev_content_change_debounce_ms"
    const val DEV_SCROLL_EVENT_DEBOUNCE_MS = "dev_scroll_event_debounce_ms"
    const val DEV_SCREEN_CHANGE_DELAY_MS = "dev_screen_change_delay_ms"
    const val DEV_MODE_ACTIVATED = "dev_mode_activated"
}
