package com.augmentalis.voiceavanue.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Application Settings entity for cross-process communication.
 * Maps to Room Settings entity internally.
 *
 * @property id Unique settings identifier (typically 1, singleton)
 * @property voiceEnabled Whether voice input is enabled
 * @property theme Current theme ("light", "dark", "system")
 * @property language Current language code (e.g., "en", "es")
 * @property notificationsEnabled Whether notifications are enabled
 *
 * @author Manoj Jhawar, manoj@ideahq.net
 * @version 1.0.0
 */
@Parcelize
data class AppSettings(
    val id: Int,
    val voiceEnabled: Boolean = true,
    val theme: String = "system",
    val language: String = "en",
    val notificationsEnabled: Boolean = true
) : Parcelable {

    companion object {
        /**
         * Create default settings.
         */
        fun default() = AppSettings(
            id = 1,
            voiceEnabled = true,
            theme = "system",
            language = "en",
            notificationsEnabled = true
        )

        /**
         * Create empty settings for testing.
         */
        fun empty() = AppSettings(
            id = 0,
            voiceEnabled = false,
            theme = "",
            language = "",
            notificationsEnabled = false
        )
    }
}
