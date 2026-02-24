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
