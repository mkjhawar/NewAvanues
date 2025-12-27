/**
 * SettingItem.kt - VoiceOS component
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2025-12-22
 *
 * Represents a setting item in the settings UI
 */
package com.augmentalis.voiceoscore.learnapp.settings.ui

/**
 * A setting item for display in settings UI
 *
 * @param key Unique key for this setting
 * @param label Display label for the setting
 * @param description Description of what the setting does
 * @param type Type of setting (toggle, number, etc.)
 * @param value Current value of the setting
 * @param enabled Whether the setting is enabled for interaction
 */
data class SettingItem(
    val key: String,
    val label: String,
    val description: String,
    val type: SettingType,
    val value: Any,
    val enabled: Boolean = true
)
