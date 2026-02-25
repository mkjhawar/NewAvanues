/**
 * PhotoAvanueSettingsProvider.kt - Camera and pro mode settings
 *
 * Provides settings for the PhotoAvanue module: default lens selection,
 * resolution preference, save path, and pro-mode controls including
 * stabilization and RAW capture. State persisted via
 * AvanuesSettingsRepository (DataStore).
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 */

package com.augmentalis.voiceavanue.ui.settings.providers

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CameraFront
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.RawOn
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.Tune
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

class PhotoAvanueSettingsProvider @Inject constructor(
    private val repository: AvanuesSettingsRepository
) : ComposableSettingsProvider {

    companion object {
        val LENS_OPTIONS = listOf(
            "Front" to "Front",
            "Back" to "Back"
        )

        val RESOLUTION_OPTIONS = listOf(
            "Auto" to "Auto",
            "Max" to "Max",
            "Balanced" to "Balanced"
        )

        val SAVE_PATH_OPTIONS = listOf(
            "DCIM" to "DCIM",
            "Custom" to "Custom"
        )

        val STABILIZATION_OPTIONS = listOf(
            "Off" to "Off",
            "Standard" to "Standard",
            "Cinematic" to "Cinematic"
        )
    }

    override val moduleId = "photoavanue"
    override val displayName = "PhotoAvanue"
    override val iconName = "CameraAlt"
    override val sortOrder = 500

    override val sections = listOf(
        SettingsSection(id = "camera", title = "Camera", sortOrder = 0),
        SettingsSection(id = "pro", title = "Pro Mode", sortOrder = 1)
    )

    override val searchableEntries = listOf(
        SearchableSettingEntry(
            key = "camera_lens",
            displayName = "Default Lens",
            sectionId = "camera",
            keywords = listOf("lens", "front", "back", "selfie", "rear", "camera")
        ),
        SearchableSettingEntry(
            key = "camera_resolution",
            displayName = "Resolution",
            sectionId = "camera",
            keywords = listOf("resolution", "quality", "megapixel", "photo size", "auto", "max")
        ),
        SearchableSettingEntry(
            key = "camera_save_path",
            displayName = "Save Path",
            sectionId = "camera",
            keywords = listOf("save", "path", "DCIM", "folder", "storage", "location")
        ),
        SearchableSettingEntry(
            key = "camera_pro",
            displayName = "Pro Controls",
            sectionId = "pro",
            keywords = listOf("pro", "manual", "controls", "stabilization", "RAW", "cinematic")
        )
    )

    @Composable
    override fun SectionContent(sectionId: String) {
        val settings by repository.settings.collectAsState(initial = AvanuesSettings())
        val scope = rememberCoroutineScope()

        when (sectionId) {
            "camera" -> CameraSection(settings, scope)
            "pro" -> ProSection(settings, scope)
        }
    }

    @Composable
    private fun CameraSection(settings: AvanuesSettings, scope: kotlinx.coroutines.CoroutineScope) {
        SettingsGroupCard {
            SettingsDropdownRow(
                title = "Default Lens",
                subtitle = "Camera lens used on launch",
                icon = Icons.Default.CameraFront,
                selected = settings.cameraDefaultLens,
                options = LENS_OPTIONS.map { it.first },
                optionLabel = { key ->
                    LENS_OPTIONS.find { it.first == key }?.second ?: key
                },
                onSelected = { lens ->
                    scope.launch { repository.updateCameraDefaultLens(lens) }
                },
                modifier = Modifier.semantics { contentDescription = "Voice: select Default Lens" }
            )

            SettingsDropdownRow(
                title = "Resolution",
                subtitle = "Photo capture resolution preference",
                icon = Icons.Default.HighQuality,
                selected = settings.cameraResolution,
                options = RESOLUTION_OPTIONS.map { it.first },
                optionLabel = { key ->
                    RESOLUTION_OPTIONS.find { it.first == key }?.second ?: key
                },
                onSelected = { resolution ->
                    scope.launch { repository.updateCameraResolution(resolution) }
                },
                modifier = Modifier.semantics { contentDescription = "Voice: select Resolution" }
            )

            SettingsDropdownRow(
                title = "Save Path",
                subtitle = "Where captured photos are stored",
                icon = Icons.Default.Folder,
                selected = settings.cameraSavePath,
                options = SAVE_PATH_OPTIONS.map { it.first },
                optionLabel = { key ->
                    SAVE_PATH_OPTIONS.find { it.first == key }?.second ?: key
                },
                onSelected = { path ->
                    scope.launch { repository.updateCameraSavePath(path) }
                },
                modifier = Modifier.semantics { contentDescription = "Voice: select Save Path" }
            )
        }
    }

    @Composable
    private fun ProSection(settings: AvanuesSettings, scope: kotlinx.coroutines.CoroutineScope) {
        SettingsGroupCard {
            SettingsSwitchRow(
                title = "Pro Controls Default",
                subtitle = "Show manual exposure and focus controls on launch",
                icon = Icons.Default.Tune,
                checked = settings.cameraProDefault,
                onCheckedChange = { scope.launch { repository.updateCameraProDefault(it) } },
                modifier = Modifier.semantics { contentDescription = "Voice: toggle Pro Controls Default" }
            )

            SettingsDropdownRow(
                title = "Stabilization",
                subtitle = "Video and photo stabilization mode",
                icon = Icons.Default.Videocam,
                selected = settings.cameraStabilization,
                options = STABILIZATION_OPTIONS.map { it.first },
                optionLabel = { key ->
                    STABILIZATION_OPTIONS.find { it.first == key }?.second ?: key
                },
                onSelected = { mode ->
                    scope.launch { repository.updateCameraStabilization(mode) }
                },
                modifier = Modifier.semantics { contentDescription = "Voice: select Stabilization" }
            )

            SettingsSwitchRow(
                title = "RAW Capture",
                subtitle = "Save photos in DNG RAW format alongside JPEG",
                icon = Icons.Default.RawOn,
                checked = settings.cameraRawEnabled,
                onCheckedChange = { scope.launch { repository.updateCameraRawEnabled(it) } },
                modifier = Modifier.semantics { contentDescription = "Voice: toggle RAW Capture" }
            )
        }
    }

    @Composable
    override fun ModuleIcon(): ImageVector = Icons.Default.CameraAlt
}
