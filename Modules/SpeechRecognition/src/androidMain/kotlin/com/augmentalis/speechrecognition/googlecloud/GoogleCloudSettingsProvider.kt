/**
 * GoogleCloudSettingsProvider.kt - Settings metadata for Google Cloud STT v2
 *
 * Provides settings sections, searchable entries, and configuration metadata
 * for the Unified Adaptive Settings screen. The Compose rendering is handled
 * by the app-level ComposableSettingsProvider implementation.
 *
 * Settings exposed:
 * - Project configuration (GCP project ID, location, recognizer)
 * - Authentication (API key vs Firebase Auth mode selection)
 * - Recognition model (latest_short for commands, latest_long for dictation)
 * - Streaming toggle (VAD_BATCH vs STREAMING mode)
 * - Audio settings (punctuation, word timestamps, profanity filter)
 *
 * Copyright (C) Augmentalis Inc, Intelligent Devices LLC
 */
package com.augmentalis.speechrecognition.googlecloud

import com.augmentalis.foundation.settings.ModuleSettingsProvider
import com.augmentalis.foundation.settings.SearchableSettingEntry
import com.augmentalis.foundation.settings.SettingsSection

/**
 * Settings metadata provider for Google Cloud STT v2.
 *
 * Declares the configuration sections exposed in Unified Adaptive Settings.
 * The app layer maps these sections to Compose UI via ComposableSettingsProvider.
 */
class GoogleCloudSettingsProvider : ModuleSettingsProvider {
    override val moduleId: String = "googlecloud_stt"
    override val displayName: String = "Google Cloud STT"
    override val iconName: String = "Cloud"
    override val sortOrder: Int = 600

    override val sections: List<SettingsSection> = listOf(
        SettingsSection(
            id = "project",
            title = "Project Configuration",
            sortOrder = 0
        ),
        SettingsSection(
            id = "auth",
            title = "Authentication",
            sortOrder = 1
        ),
        SettingsSection(
            id = "recognition",
            title = "Recognition",
            sortOrder = 2
        ),
        SettingsSection(
            id = "advanced",
            title = "Advanced",
            sortOrder = 3
        )
    )

    override val searchableEntries: List<SearchableSettingEntry> = listOf(
        SearchableSettingEntry(
            key = "gcp_project_id",
            displayName = "GCP Project ID",
            sectionId = "project",
            keywords = listOf("google", "cloud", "project", "gcp")
        ),
        SearchableSettingEntry(
            key = "gcp_location",
            displayName = "GCP Location",
            sectionId = "project",
            keywords = listOf("region", "global", "location")
        ),
        SearchableSettingEntry(
            key = "gcp_auth_mode",
            displayName = "Authentication Mode",
            sectionId = "auth",
            keywords = listOf("api key", "firebase", "auth", "token", "bearer")
        ),
        SearchableSettingEntry(
            key = "gcp_api_key",
            displayName = "API Key",
            sectionId = "auth",
            keywords = listOf("key", "credential", "secret")
        ),
        SearchableSettingEntry(
            key = "gcp_model",
            displayName = "Recognition Model",
            sectionId = "recognition",
            keywords = listOf("model", "latest_short", "latest_long", "accuracy")
        ),
        SearchableSettingEntry(
            key = "gcp_streaming",
            displayName = "Streaming Mode",
            sectionId = "recognition",
            keywords = listOf("streaming", "batch", "real-time", "vad")
        ),
        SearchableSettingEntry(
            key = "gcp_language",
            displayName = "Language",
            sectionId = "recognition",
            keywords = listOf("language", "locale", "en-us")
        ),
        SearchableSettingEntry(
            key = "gcp_punctuation",
            displayName = "Automatic Punctuation",
            sectionId = "advanced",
            keywords = listOf("punctuation", "period", "comma")
        ),
        SearchableSettingEntry(
            key = "gcp_profanity_filter",
            displayName = "Profanity Filter",
            sectionId = "advanced",
            keywords = listOf("profanity", "filter", "censor")
        ),
        SearchableSettingEntry(
            key = "gcp_word_timestamps",
            displayName = "Word Timestamps",
            sectionId = "advanced",
            keywords = listOf("word", "time", "offset", "timestamp")
        )
    )

    companion object {
        // Settings keys for DataStore persistence
        const val KEY_PROJECT_ID = "gcp_stt_project_id"
        const val KEY_LOCATION = "gcp_stt_location"
        const val KEY_AUTH_MODE = "gcp_stt_auth_mode"
        const val KEY_API_KEY = "gcp_stt_api_key"
        const val KEY_MODEL = "gcp_stt_model"
        const val KEY_STREAMING = "gcp_stt_streaming_enabled"
        const val KEY_LANGUAGE = "gcp_stt_language"
        const val KEY_PUNCTUATION = "gcp_stt_punctuation"
        const val KEY_PROFANITY_FILTER = "gcp_stt_profanity_filter"
        const val KEY_WORD_TIMESTAMPS = "gcp_stt_word_timestamps"
    }
}
