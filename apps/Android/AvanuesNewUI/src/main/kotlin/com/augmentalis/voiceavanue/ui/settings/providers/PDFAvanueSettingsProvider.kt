/**
 * PDFAvanueSettingsProvider.kt - PDF viewer settings
 *
 * Provides settings for the PDFAvanue module: view mode (Single, Continuous,
 * Thumbnail), night mode toggle, default zoom level, and page position
 * persistence. State persisted via AvanuesSettingsRepository (DataStore).
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 */

package com.augmentalis.voiceavanue.ui.settings.providers

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.ViewDay
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

class PDFAvanueSettingsProvider @Inject constructor(
    private val repository: AvanuesSettingsRepository
) : ComposableSettingsProvider {

    companion object {
        val VIEW_MODE_OPTIONS = listOf(
            "Single" to "Single",
            "Continuous" to "Continuous",
            "Thumbnail" to "Thumbnail"
        )

        val ZOOM_OPTIONS = listOf(
            "FitWidth" to "Fit Width",
            "FitPage" to "Fit Page",
            "100%" to "100%"
        )
    }

    override val moduleId = "pdfavanue"
    override val displayName = "PDFAvanue"
    override val iconName = "PictureAsPdf"
    override val sortOrder = 450

    override val sections = listOf(
        SettingsSection(id = "viewer", title = "Viewer")
    )

    override val searchableEntries = listOf(
        SearchableSettingEntry(
            key = "pdf_view_mode",
            displayName = "PDF View Mode",
            sectionId = "viewer",
            keywords = listOf("view", "single", "continuous", "thumbnail", "page", "scroll")
        ),
        SearchableSettingEntry(
            key = "pdf_night_mode",
            displayName = "PDF Night Mode",
            sectionId = "viewer",
            keywords = listOf("night", "dark", "invert", "reading", "eye strain")
        ),
        SearchableSettingEntry(
            key = "pdf_zoom",
            displayName = "PDF Default Zoom",
            sectionId = "viewer",
            keywords = listOf("zoom", "fit", "width", "page", "scale", "magnification")
        ),
        SearchableSettingEntry(
            key = "pdf_remember_page",
            displayName = "Remember Page",
            sectionId = "viewer",
            keywords = listOf("remember", "page", "position", "bookmark", "resume")
        )
    )

    @Composable
    override fun SectionContent(sectionId: String) {
        val settings by repository.settings.collectAsState(initial = AvanuesSettings())
        val scope = rememberCoroutineScope()

        SettingsGroupCard {
            SettingsDropdownRow(
                title = "View Mode",
                subtitle = "Page display layout",
                icon = Icons.Default.ViewDay,
                selected = settings.pdfViewMode,
                options = VIEW_MODE_OPTIONS.map { it.first },
                optionLabel = { key ->
                    VIEW_MODE_OPTIONS.find { it.first == key }?.second ?: key
                },
                onSelected = { mode ->
                    scope.launch { repository.updatePdfViewMode(mode) }
                },
                modifier = Modifier.semantics { contentDescription = "Voice: select View Mode" }
            )

            SettingsSwitchRow(
                title = "Night Mode",
                subtitle = "Invert colors for comfortable reading in the dark",
                icon = Icons.Default.DarkMode,
                checked = settings.pdfNightMode,
                onCheckedChange = { scope.launch { repository.updatePdfNightMode(it) } },
                modifier = Modifier.semantics { contentDescription = "Voice: toggle Night Mode" }
            )

            SettingsDropdownRow(
                title = "Default Zoom",
                subtitle = "Initial zoom level when opening a PDF",
                icon = Icons.Default.ZoomIn,
                selected = settings.pdfDefaultZoom,
                options = ZOOM_OPTIONS.map { it.first },
                optionLabel = { key ->
                    ZOOM_OPTIONS.find { it.first == key }?.second ?: key
                },
                onSelected = { zoom ->
                    scope.launch { repository.updatePdfDefaultZoom(zoom) }
                },
                modifier = Modifier.semantics { contentDescription = "Voice: select Default Zoom" }
            )

            SettingsSwitchRow(
                title = "Remember Page",
                subtitle = "Resume from last viewed page when reopening",
                icon = Icons.Default.Bookmark,
                checked = settings.pdfRememberPage,
                onCheckedChange = { scope.launch { repository.updatePdfRememberPage(it) } },
                modifier = Modifier.semantics { contentDescription = "Voice: toggle Remember Page" }
            )
        }
    }

    @Composable
    override fun ModuleIcon(): ImageVector = Icons.Default.PictureAsPdf
}
