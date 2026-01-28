/**
 * UserPreference.kt - Data class for storing user preferences
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2025-09-06
 * Updated: 2025-12-01 - Removed Room annotations for SQLDelight migration
 */
package com.augmentalis.localizationmanager.data

/**
 * User preference data class
 *
 * Maps to SQLDelight user_preference table in VoiceOSDatabase
 */
data class UserPreference(
    val key: String,
    val value: String,
    val lastModified: Long = System.currentTimeMillis()
)

/**
 * Strongly-typed preference keys to avoid magic strings
 */
object PreferenceKeys {
    const val MESSAGE_DEBOUNCE_DURATION = "message_debounce_duration"
    const val STATISTICS_AUTO_SHOW = "statistics_auto_show"
    const val LANGUAGE_ANIMATION_ENABLED = "language_animation_enabled"
    const val PREFERRED_DETAIL_LEVEL = "preferred_detail_level"
}

/**
 * Default preference values
 */
object PreferenceDefaults {
    const val MESSAGE_DEBOUNCE_DURATION = 2000L // 2 seconds
    const val STATISTICS_AUTO_SHOW = false
    const val LANGUAGE_ANIMATION_ENABLED = true
    const val PREFERRED_DETAIL_LEVEL = "STANDARD"
}

/**
 * Debounce duration options for user selection
 */
enum class DebounceDuration(val displayName: String, val milliseconds: Long) {
    INSTANT("Instant (No delay)", 0L),
    FAST("Fast (1 second)", 1000L),
    NORMAL("Normal (2 seconds)", 2000L),
    SLOW("Slow (3 seconds)", 3000L),
    VERY_SLOW("Very Slow (5 seconds)", 5000L);
    
    companion object {
        fun fromMilliseconds(ms: Long): DebounceDuration {
            return values().find { it.milliseconds == ms } ?: NORMAL
        }
    }
}

/**
 * Statistics detail level options
 */
enum class DetailLevel(val displayName: String) {
    MINIMAL("Minimal"),
    STANDARD("Standard"),
    COMPREHENSIVE("Comprehensive")
}