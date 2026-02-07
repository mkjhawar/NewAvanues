/**
 * ModuleSettingsProvider.kt - Dynamic settings registration interface
 *
 * Each module (VoiceCursor, VoiceControl, WebAvanue, etc.) implements this
 * interface to declare its settings sections. The unified settings screen
 * collects all providers and renders them in an adaptive layout.
 *
 * Pure KMP interface - no Compose dependency. Foundation stays clean.
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 */

package com.augmentalis.foundation.settings

/**
 * Interface for modules to register their settings sections with the unified screen.
 *
 * Implementations declare metadata (id, name, icon, order) and the list of
 * settings sections they contribute. The rendering is handled at the app level
 * by ComposableSettingsProvider which extends this interface with Compose.
 *
 * Sort order convention: use multiples of 100 to leave room for insertion.
 * Lower values appear higher in the settings list.
 */
interface ModuleSettingsProvider {
    /** Unique identifier for this module's settings (e.g. "voicecursor", "webavanue") */
    val moduleId: String

    /** Human-readable name shown in the settings list (e.g. "VoiceCursor") */
    val displayName: String

    /** Material icon name for cross-platform use (e.g. "TouchApp", "Language") */
    val iconName: String

    /** Sort order in the module list. Lower = higher. Use multiples of 100. */
    val sortOrder: Int

    /** Whether this module's settings are currently available */
    val isEnabled: Boolean get() = true

    /** Sections this module contributes to settings */
    val sections: List<SettingsSection>

    /** Searchable entries for the settings search bar */
    val searchableEntries: List<SearchableSettingEntry> get() = emptyList()
}

/**
 * A named section within a module's settings.
 *
 * Modules with a single section render content directly.
 * Modules with multiple sections get a ScrollableTabRow for section tabs.
 *
 * @param id Unique section identifier within the module
 * @param title Display title for the section tab/header
 * @param sortOrder Order within the module (lower = first). Default 0.
 */
data class SettingsSection(
    val id: String,
    val title: String,
    val sortOrder: Int = 0
)

/**
 * A single searchable setting entry for the unified search bar.
 *
 * When the user types in the settings search field, entries matching the
 * displayName or keywords surface the corresponding module and section.
 *
 * @param key Unique key for this setting (e.g. "dwell_click_enabled")
 * @param displayName Human-readable name shown in search results
 * @param sectionId Which section this entry belongs to
 * @param keywords Additional search terms (e.g. ["auto-click", "tap", "dwell"])
 */
data class SearchableSettingEntry(
    val key: String,
    val displayName: String,
    val sectionId: String,
    val keywords: List<String> = emptyList()
)
