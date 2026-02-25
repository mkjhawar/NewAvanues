/**
 * FileAvanueSettingsProvider.kt - File browser settings
 *
 * One section:
 * - Browser: sort mode, view mode, show hidden, default provider
 *
 * State persisted via AvanuesSettingsRepository (DataStore).
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 */

package com.augmentalis.voiceavanue.ui.settings.providers

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.Source
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.augmentalis.avanueui.components.settings.SettingsDropdownRow
import com.augmentalis.avanueui.components.settings.SettingsGroupCard
import com.augmentalis.avanueui.components.settings.SettingsSwitchRow
import com.augmentalis.foundation.settings.SearchableSettingEntry
import com.augmentalis.foundation.settings.SettingsSection
import com.augmentalis.foundation.settings.models.AvanuesSettings
import com.augmentalis.voiceavanue.data.AvanuesSettingsRepository
import com.augmentalis.voiceavanue.ui.settings.ComposableSettingsProvider
import kotlinx.coroutines.launch
import javax.inject.Inject

class FileAvanueSettingsProvider @Inject constructor(
    private val repository: AvanuesSettingsRepository
) : ComposableSettingsProvider {

    companion object {
        /** Sort mode options with display labels. */
        val SORT_OPTIONS = listOf(
            "Name" to "Name",
            "Date" to "Date",
            "Size" to "Size",
            "Type" to "Type"
        )

        /** View mode options with display labels. */
        val VIEW_OPTIONS = listOf(
            "List" to "List",
            "Grid" to "Grid"
        )

        /** Default provider options with display labels. */
        val PROVIDER_OPTIONS = listOf(
            "Local" to "Local",
            "Downloads" to "Downloads"
        )
    }

    override val moduleId = "fileavanue"
    override val displayName = "FileAvanue"
    override val iconName = "Folder"
    override val sortOrder = 650

    override val sections = listOf(
        SettingsSection(id = "browser", title = "Browser", sortOrder = 0)
    )

    override val searchableEntries = listOf(
        SearchableSettingEntry(
            key = "file_sort_mode",
            displayName = "Sort Mode",
            sectionId = "browser",
            keywords = listOf("sort", "order", "name", "date", "size", "type", "arrange")
        ),
        SearchableSettingEntry(
            key = "file_view_mode",
            displayName = "View Mode",
            sectionId = "browser",
            keywords = listOf("view", "list", "grid", "layout", "display")
        ),
        SearchableSettingEntry(
            key = "file_show_hidden",
            displayName = "Show Hidden",
            sectionId = "browser",
            keywords = listOf("hidden", "dot files", "invisible", "show all", "hidden files")
        ),
        SearchableSettingEntry(
            key = "file_default_provider",
            displayName = "Default Provider",
            sectionId = "browser",
            keywords = listOf("provider", "storage", "local", "downloads", "source", "default location")
        )
    )

    @Composable
    override fun SectionContent(sectionId: String) {
        val settings by repository.settings.collectAsState(initial = AvanuesSettings())
        val scope = rememberCoroutineScope()

        SettingsGroupCard {
            SettingsDropdownRow(
                title = "Sort Mode",
                subtitle = "How files are sorted in the browser",
                icon = Icons.Default.Sort,
                selected = settings.fileSortMode,
                options = SORT_OPTIONS.map { it.first },
                optionLabel = { code ->
                    SORT_OPTIONS.find { it.first == code }?.second ?: code
                },
                onSelected = { mode ->
                    scope.launch { repository.updateFileSortMode(mode) }
                },
                modifier = Modifier.semantics { contentDescription = "Voice: select Sort Mode" }
            )

            SettingsDropdownRow(
                title = "View Mode",
                subtitle = "File listing layout",
                icon = Icons.Default.GridView,
                selected = settings.fileViewMode,
                options = VIEW_OPTIONS.map { it.first },
                optionLabel = { code ->
                    VIEW_OPTIONS.find { it.first == code }?.second ?: code
                },
                onSelected = { mode ->
                    scope.launch { repository.updateFileViewMode(mode) }
                },
                modifier = Modifier.semantics { contentDescription = "Voice: select View Mode" }
            )

            SettingsSwitchRow(
                title = "Show Hidden",
                subtitle = "Display hidden files and folders",
                icon = Icons.Default.VisibilityOff,
                checked = settings.fileShowHidden,
                onCheckedChange = { scope.launch { repository.updateFileShowHidden(it) } },
                modifier = Modifier.semantics { contentDescription = "Voice: toggle Show Hidden" }
            )

            SettingsDropdownRow(
                title = "Default Provider",
                subtitle = "Starting location when opening file browser",
                icon = Icons.Default.Source,
                selected = settings.fileDefaultProvider,
                options = PROVIDER_OPTIONS.map { it.first },
                optionLabel = { code ->
                    PROVIDER_OPTIONS.find { it.first == code }?.second ?: code
                },
                onSelected = { provider ->
                    scope.launch { repository.updateFileDefaultProvider(provider) }
                },
                modifier = Modifier.semantics { contentDescription = "Voice: select Default Provider" }
            )
        }
    }

    @Composable
    override fun ModuleIcon(): ImageVector = Icons.Default.Folder
}
