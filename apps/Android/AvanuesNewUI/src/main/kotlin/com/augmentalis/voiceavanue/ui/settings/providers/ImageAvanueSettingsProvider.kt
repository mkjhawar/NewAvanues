/**
 * ImageAvanueSettingsProvider.kt - Image viewer default settings
 *
 * Provides default zoom mode and EXIF metadata display toggle.
 * State persisted via AvanuesSettingsRepository (DataStore).
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 */

package com.augmentalis.voiceavanue.ui.settings.providers

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.ZoomIn
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

class ImageAvanueSettingsProvider @Inject constructor(
    private val repository: AvanuesSettingsRepository
) : ComposableSettingsProvider {

    companion object {
        val ZOOM_OPTIONS = listOf("Fit", "Fill", "100%")
    }

    override val moduleId = "imageavanue"
    override val displayName = "ImageAvanue"
    override val iconName = "Image"
    override val sortOrder = 800

    override val sections = listOf(
        SettingsSection(id = "viewer", title = "Viewer")
    )

    override val searchableEntries = listOf(
        SearchableSettingEntry(
            key = "image_default_zoom",
            displayName = "Default Zoom",
            sectionId = "viewer",
            keywords = listOf("zoom", "fit", "fill", "scale", "size", "100%")
        ),
        SearchableSettingEntry(
            key = "image_show_exif",
            displayName = "Show EXIF",
            sectionId = "viewer",
            keywords = listOf("exif", "metadata", "info", "details", "camera info", "photo data")
        )
    )

    @Composable
    override fun SectionContent(sectionId: String) {
        val settings by repository.settings.collectAsState(initial = AvanuesSettings())
        val scope = rememberCoroutineScope()

        SettingsGroupCard {
            SettingsDropdownRow(
                title = "Default Zoom",
                subtitle = "Initial zoom level when opening images",
                icon = Icons.Default.ZoomIn,
                selected = settings.imageDefaultZoom,
                options = ZOOM_OPTIONS,
                optionLabel = { it },
                onSelected = { scope.launch { repository.updateImageDefaultZoom(it) } },
                modifier = Modifier.semantics { contentDescription = "Voice: select Default Zoom" }
            )

            SettingsSwitchRow(
                title = "Show EXIF",
                subtitle = "Display camera metadata (aperture, ISO, date) on images",
                icon = Icons.Default.Info,
                checked = settings.imageShowExif,
                onCheckedChange = { scope.launch { repository.updateImageShowExif(it) } },
                modifier = Modifier.semantics { contentDescription = "Voice: toggle Show EXIF" }
            )
        }
    }

    @Composable
    override fun ModuleIcon(): ImageVector = Icons.Default.Image
}
