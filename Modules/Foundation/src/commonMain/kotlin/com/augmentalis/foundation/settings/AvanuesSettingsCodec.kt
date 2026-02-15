/**
 * AvanuesSettingsCodec.kt - Codec for AvanuesSettings persistence
 *
 * Maps AvanuesSettings fields to/from SettingsKeys for platform-agnostic
 * persistence. Used by UserDefaultsSettingsStore (iOS) and
 * JavaPreferencesSettingsStore (Desktop).
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 */
package com.augmentalis.foundation.settings

import com.augmentalis.foundation.settings.models.AvanuesSettings

/**
 * Codec for [AvanuesSettings] ↔ key-value persistence.
 *
 * Handles theme v5.1 migration from legacy `theme_variant` key.
 */
object AvanuesSettingsCodec : SettingsCodec<AvanuesSettings> {

    override val defaultValue: AvanuesSettings = AvanuesSettings()

    override fun decode(reader: PreferenceReader): AvanuesSettings {
        val oldVariant = reader.getStringOrNull(SettingsKeys.THEME_VARIANT_LEGACY)
        val palette = reader.getStringOrNull(SettingsKeys.THEME_PALETTE)
            ?: SettingsMigration.migrateVariantToPalette(oldVariant)
        val style = reader.getStringOrNull(SettingsKeys.THEME_STYLE)
            ?: SettingsMigration.migrateVariantToStyle(oldVariant)

        return AvanuesSettings(
            cursorEnabled = reader.getBoolean(SettingsKeys.CURSOR_ENABLED, false),
            dwellClickEnabled = reader.getBoolean(SettingsKeys.DWELL_CLICK_ENABLED, true),
            dwellClickDelayMs = reader.getFloat(SettingsKeys.DWELL_CLICK_DELAY_MS, AvanuesSettings.DEFAULT_DWELL_CLICK_DELAY_MS),
            cursorSmoothing = reader.getBoolean(SettingsKeys.CURSOR_SMOOTHING, true),
            cursorSize = reader.getInt(SettingsKeys.CURSOR_SIZE, AvanuesSettings.DEFAULT_CURSOR_SIZE),
            cursorSpeed = reader.getInt(SettingsKeys.CURSOR_SPEED, AvanuesSettings.DEFAULT_CURSOR_SPEED),
            showCoordinates = reader.getBoolean(SettingsKeys.SHOW_COORDINATES, false),
            cursorAccentOverride = reader.getLongOrNull(SettingsKeys.CURSOR_ACCENT_OVERRIDE),
            voiceFeedback = reader.getBoolean(SettingsKeys.VOICE_FEEDBACK, true),
            voiceLocale = reader.getString(SettingsKeys.VOICE_COMMAND_LOCALE, AvanuesSettings.DEFAULT_VOICE_LOCALE),
            autoStartOnBoot = reader.getBoolean(SettingsKeys.AUTO_START_ON_BOOT, false),
            themePalette = palette,
            themeStyle = style,
            themeAppearance = reader.getString(SettingsKeys.THEME_APPEARANCE, AvanuesSettings.DEFAULT_THEME_APPEARANCE),
            vosSyncEnabled = reader.getBoolean(SettingsKeys.VOS_SYNC_ENABLED, false),
            vosSftpHost = reader.getString(SettingsKeys.VOS_SFTP_HOST, ""),
            vosSftpPort = reader.getInt(SettingsKeys.VOS_SFTP_PORT, AvanuesSettings.DEFAULT_SFTP_PORT),
            vosSftpUsername = reader.getString(SettingsKeys.VOS_SFTP_USERNAME, ""),
            vosSftpRemotePath = reader.getString(SettingsKeys.VOS_SFTP_REMOTE_PATH, AvanuesSettings.DEFAULT_SFTP_REMOTE_PATH),
            vosSftpKeyPath = reader.getString(SettingsKeys.VOS_SFTP_KEY_PATH, ""),
            vosLastSyncTime = reader.getLongOrNull(SettingsKeys.VOS_LAST_SYNC_TIME),
            vosSftpHostKeyMode = reader.getString(SettingsKeys.VOS_SFTP_HOST_KEY_MODE, AvanuesSettings.DEFAULT_HOST_KEY_MODE),
            vosAutoSyncEnabled = reader.getBoolean(SettingsKeys.VOS_AUTO_SYNC_ENABLED, false),
            vosSyncIntervalHours = reader.getInt(SettingsKeys.VOS_SYNC_INTERVAL_HOURS, AvanuesSettings.DEFAULT_SYNC_INTERVAL_HOURS)
        )
    }

    override fun encode(value: AvanuesSettings, writer: PreferenceWriter) {
        writer.putBoolean(SettingsKeys.CURSOR_ENABLED, value.cursorEnabled)
        writer.putBoolean(SettingsKeys.DWELL_CLICK_ENABLED, value.dwellClickEnabled)
        writer.putFloat(SettingsKeys.DWELL_CLICK_DELAY_MS, value.dwellClickDelayMs)
        writer.putBoolean(SettingsKeys.CURSOR_SMOOTHING, value.cursorSmoothing)
        writer.putInt(SettingsKeys.CURSOR_SIZE, value.cursorSize)
        writer.putInt(SettingsKeys.CURSOR_SPEED, value.cursorSpeed)
        writer.putBoolean(SettingsKeys.SHOW_COORDINATES, value.showCoordinates)
        val accent = value.cursorAccentOverride
        if (accent != null) {
            writer.putLong(SettingsKeys.CURSOR_ACCENT_OVERRIDE, accent)
        } else {
            writer.remove(SettingsKeys.CURSOR_ACCENT_OVERRIDE)
        }
        writer.putBoolean(SettingsKeys.VOICE_FEEDBACK, value.voiceFeedback)
        writer.putString(SettingsKeys.VOICE_COMMAND_LOCALE, value.voiceLocale)
        writer.putBoolean(SettingsKeys.AUTO_START_ON_BOOT, value.autoStartOnBoot)
        writer.putString(SettingsKeys.THEME_PALETTE, value.themePalette)
        writer.putString(SettingsKeys.THEME_STYLE, value.themeStyle)
        writer.putString(SettingsKeys.THEME_APPEARANCE, value.themeAppearance)
        writer.putBoolean(SettingsKeys.VOS_SYNC_ENABLED, value.vosSyncEnabled)
        writer.putString(SettingsKeys.VOS_SFTP_HOST, value.vosSftpHost)
        writer.putInt(SettingsKeys.VOS_SFTP_PORT, value.vosSftpPort)
        writer.putString(SettingsKeys.VOS_SFTP_USERNAME, value.vosSftpUsername)
        writer.putString(SettingsKeys.VOS_SFTP_REMOTE_PATH, value.vosSftpRemotePath)
        writer.putString(SettingsKeys.VOS_SFTP_KEY_PATH, value.vosSftpKeyPath)
        val syncTime = value.vosLastSyncTime
        if (syncTime != null) {
            writer.putLong(SettingsKeys.VOS_LAST_SYNC_TIME, syncTime)
        } else {
            writer.remove(SettingsKeys.VOS_LAST_SYNC_TIME)
        }
        writer.putString(SettingsKeys.VOS_SFTP_HOST_KEY_MODE, value.vosSftpHostKeyMode)
        writer.putBoolean(SettingsKeys.VOS_AUTO_SYNC_ENABLED, value.vosAutoSyncEnabled)
        writer.putInt(SettingsKeys.VOS_SYNC_INTERVAL_HOURS, value.vosSyncIntervalHours)
    }
}
