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

    // Boot
    val autoStartOnBoot: Boolean = false,

    // Theme v5.1: decoupled palette + style + appearance
    val themePalette: String = DEFAULT_THEME_PALETTE,
    val themeStyle: String = DEFAULT_THEME_STYLE,
    val themeAppearance: String = DEFAULT_THEME_APPEARANCE,

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
