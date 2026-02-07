/**
 * WebAvanueSettingsProvider.kt - Wraps WebAvanue's full settings screen
 *
 * Delegates to WebAvanue's existing SettingsScreen composable.
 * All 60+ SQLDelight-backed settings stay untouched - this is a
 * phase-1 migration that wraps without modifying the storage layer.
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 */

package com.augmentalis.voiceavanue.ui.settings.providers

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Language
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.augmentalis.foundation.settings.SearchableSettingEntry
import com.augmentalis.foundation.settings.SettingsSection
import com.augmentalis.voiceavanue.ui.settings.ComposableSettingsProvider
import com.augmentalis.webavanue.BrowserRepository
import com.augmentalis.webavanue.SettingsViewModel as BrowserSettingsViewModel
import javax.inject.Inject

class WebAvanueSettingsProvider @Inject constructor(
    private val repository: BrowserRepository
) : ComposableSettingsProvider {

    override val moduleId = "webavanue"
    override val displayName = "WebAvanue"
    override val iconName = "Language"
    override val sortOrder = 400

    override val sections = listOf(
        SettingsSection(id = "general", title = "General", sortOrder = 0),
        SettingsSection(id = "appearance", title = "Appearance", sortOrder = 1),
        SettingsSection(id = "privacy", title = "Privacy", sortOrder = 2),
        SettingsSection(id = "more", title = "More", sortOrder = 3)
    )

    override val searchableEntries = listOf(
        SearchableSettingEntry(
            key = "search_engine",
            displayName = "Search Engine",
            sectionId = "general",
            keywords = listOf("google", "bing", "duckduckgo", "search")
        ),
        SearchableSettingEntry(
            key = "homepage",
            displayName = "Homepage",
            sectionId = "general",
            keywords = listOf("home page", "start page", "default page")
        ),
        SearchableSettingEntry(
            key = "javascript",
            displayName = "JavaScript",
            sectionId = "privacy",
            keywords = listOf("scripts", "JS", "javascript enabled")
        ),
        SearchableSettingEntry(
            key = "desktop_mode",
            displayName = "Desktop Mode",
            sectionId = "general",
            keywords = listOf("desktop", "user agent", "mobile view")
        ),
        SearchableSettingEntry(
            key = "theme",
            displayName = "Browser Theme",
            sectionId = "appearance",
            keywords = listOf("dark mode", "light mode", "theme", "appearance")
        ),
        SearchableSettingEntry(
            key = "font_size",
            displayName = "Font Size",
            sectionId = "appearance",
            keywords = listOf("text size", "font", "zoom", "text scaling")
        ),
        SearchableSettingEntry(
            key = "cookies",
            displayName = "Cookies",
            sectionId = "privacy",
            keywords = listOf("cookies", "tracking", "privacy")
        ),
        SearchableSettingEntry(
            key = "downloads",
            displayName = "Download Location",
            sectionId = "more",
            keywords = listOf("download", "save location", "files")
        ),
        SearchableSettingEntry(
            key = "voice_commands",
            displayName = "Voice Commands",
            sectionId = "more",
            keywords = listOf("voice", "browser commands", "speech")
        )
    )

    @Composable
    override fun SectionContent(sectionId: String) {
        val settingsViewModel = remember {
            BrowserSettingsViewModel(repository = repository)
        }

        // Render the full WebAvanue settings - the existing SettingsScreen
        // handles all sections internally with its own scrolling LazyColumn.
        // We embed it without the outer Scaffold/TopAppBar since the unified
        // screen provides its own chrome.
        com.augmentalis.webavanue.SettingsScreen(
            viewModel = settingsViewModel,
            onNavigateBack = { /* Handled by unified screen navigation */ }
        )
    }

    @Composable
    override fun ModuleIcon(): ImageVector = Icons.Default.Language
}
