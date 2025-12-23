/**
 * ServiceConfiguration.kt - VoiceOS component
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2025-12-22
 */
package com.augmentalis.voiceoscore.accessibility.config

import android.content.Context

/**
 * Service Configuration
 *
 * Configuration settings for VoiceOS accessibility service
 */
data class ServiceConfiguration(
    val enabled: Boolean = true,
    val verboseLogging: Boolean = false,
    val autoStart: Boolean = true,
    val voiceLanguage: String = "en-US",
    val fingerprintGesturesEnabled: Boolean = false,
    val isLowResourceMode: Boolean = false,
    val shouldProceed: Boolean = true,
    val features: Map<String, Boolean> = emptyMap()
) {
    companion object {
        /**
         * Load configuration from SharedPreferences
         *
         * @param context Android context
         * @return Loaded configuration
         */
        fun loadFromPreferences(context: Context): ServiceConfiguration {
            val prefs = context.getSharedPreferences("voiceos_config", Context.MODE_PRIVATE)
            return ServiceConfiguration(
                enabled = prefs.getBoolean("enabled", true),
                verboseLogging = prefs.getBoolean("verbose_logging", false),
                autoStart = prefs.getBoolean("auto_start", true),
                voiceLanguage = prefs.getString("voice_language", "en-US") ?: "en-US",
                fingerprintGesturesEnabled = prefs.getBoolean("fingerprint_gestures", false),
                isLowResourceMode = prefs.getBoolean("low_resource_mode", false),
                shouldProceed = prefs.getBoolean("should_proceed", true)
            )
        }
    }
}
