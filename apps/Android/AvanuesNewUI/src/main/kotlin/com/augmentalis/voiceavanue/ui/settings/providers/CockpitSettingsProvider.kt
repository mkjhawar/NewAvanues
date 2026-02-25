/**
 * CockpitSettingsProvider.kt - Cockpit shell, frame, and spatial settings
 *
 * Provides settings for the Cockpit hub: shell mode selection (Classic,
 * AvanueViews, Lens, Canvas), frame management, auto-save intervals,
 * background scenes, and spatial head-tracking configuration.
 * State persisted via AvanuesSettingsRepository (DataStore).
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 */

package com.augmentalis.voiceavanue.ui.settings.providers

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Panorama
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.ViewComfy
import androidx.compose.material.icons.filled.Wallpaper
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
import com.augmentalis.avanueui.components.settings.SettingsSliderRow
import com.augmentalis.avanueui.components.settings.SettingsSwitchRow
import com.augmentalis.foundation.settings.SearchableSettingEntry
import com.augmentalis.foundation.settings.SettingsSection
import com.augmentalis.foundation.settings.models.AvanuesSettings
import com.augmentalis.voiceavanue.data.AvanuesSettingsRepository
import com.augmentalis.voiceavanue.ui.settings.ComposableSettingsProvider
import kotlinx.coroutines.launch
import javax.inject.Inject

class CockpitSettingsProvider @Inject constructor(
    private val repository: AvanuesSettingsRepository
) : ComposableSettingsProvider {

    companion object {
        val SHELL_MODE_OPTIONS = listOf(
            "CLASSIC" to "Classic",
            "AVANUE_VIEWS" to "AvanueViews",
            "LENS" to "Lens",
            "CANVAS" to "Canvas"
        )

        val ARRANGEMENT_OPTIONS = listOf(
            "FOCUS" to "Focus",
            "COMPARE" to "Compare",
            "OVERVIEW" to "Overview",
            "PRESENT" to "Present"
        )

        val AUTOSAVE_OPTIONS = listOf(
            "Off" to "Off",
            "30s" to "30s",
            "1m" to "1m",
            "5m" to "5m"
        )

        val BACKGROUND_SCENE_OPTIONS = listOf(
            "GRADIENT" to "Gradient",
            "STARFIELD" to "Starfield",
            "MINIMAL" to "Minimal",
            "NONE" to "None"
        )

        val SENSITIVITY_OPTIONS = listOf(
            "LOW" to "Low",
            "NORMAL" to "Normal",
            "HIGH" to "High"
        )
    }

    override val moduleId = "cockpit"
    override val displayName = "Cockpit"
    override val iconName = "Dashboard"
    override val sortOrder = 350

    override val sections = listOf(
        SettingsSection(id = "shell", title = "Home Screen", sortOrder = 0),
        SettingsSection(id = "frames", title = "Frames", sortOrder = 1),
        SettingsSection(id = "spatial", title = "Spatial", sortOrder = 2)
    )

    override val searchableEntries = listOf(
        SearchableSettingEntry(
            key = "shell_mode",
            displayName = "Shell Mode",
            sectionId = "shell",
            keywords = listOf("shell", "home", "launcher", "classic", "lens", "canvas", "avanueviews")
        ),
        SearchableSettingEntry(
            key = "default_arrangement",
            displayName = "Default Arrangement",
            sectionId = "shell",
            keywords = listOf("arrangement", "layout", "focus", "compare", "overview", "present")
        ),
        SearchableSettingEntry(
            key = "cockpit_max_frames",
            displayName = "Max Frames",
            sectionId = "frames",
            keywords = listOf("frames", "windows", "max", "limit", "multi-window")
        ),
        SearchableSettingEntry(
            key = "cockpit_spatial",
            displayName = "Spatial Head Tracking",
            sectionId = "spatial",
            keywords = listOf("spatial", "head tracking", "IMU", "gyroscope", "3D", "canvas", "zoom")
        )
    )

    @Composable
    override fun SectionContent(sectionId: String) {
        val settings by repository.settings.collectAsState(initial = AvanuesSettings())
        val scope = rememberCoroutineScope()

        when (sectionId) {
            "shell" -> ShellSection(settings, scope)
            "frames" -> FramesSection(settings, scope)
            "spatial" -> SpatialSection(settings, scope)
        }
    }

    @Composable
    private fun ShellSection(settings: AvanuesSettings, scope: kotlinx.coroutines.CoroutineScope) {
        SettingsGroupCard {
            SettingsDropdownRow(
                title = "Shell Mode",
                subtitle = "Home screen layout style",
                icon = Icons.Default.Dashboard,
                selected = settings.shellMode,
                options = SHELL_MODE_OPTIONS.map { it.first },
                optionLabel = { key ->
                    SHELL_MODE_OPTIONS.find { it.first == key }?.second ?: key
                },
                onSelected = { mode ->
                    scope.launch { repository.updateShellMode(mode) }
                },
                modifier = Modifier.semantics { contentDescription = "Voice: select Shell Mode" }
            )

            SettingsDropdownRow(
                title = "Default Arrangement",
                subtitle = "Initial frame layout when opening Cockpit",
                icon = Icons.Default.ViewComfy,
                selected = settings.defaultArrangement,
                options = ARRANGEMENT_OPTIONS.map { it.first },
                optionLabel = { key ->
                    ARRANGEMENT_OPTIONS.find { it.first == key }?.second ?: key
                },
                onSelected = { arrangement ->
                    scope.launch { repository.updateDefaultArrangement(arrangement) }
                },
                modifier = Modifier.semantics { contentDescription = "Voice: select Default Arrangement" }
            )
        }
    }

    @Composable
    private fun FramesSection(settings: AvanuesSettings, scope: kotlinx.coroutines.CoroutineScope) {
        SettingsGroupCard {
            SettingsSliderRow(
                title = "Max Frames",
                subtitle = "Maximum number of simultaneous frames",
                icon = Icons.Default.GridView,
                value = settings.cockpitMaxFrames.toFloat(),
                valueRange = 1f..12f,
                steps = 10,
                valueLabel = settings.cockpitMaxFrames.toString(),
                onValueChange = { scope.launch { repository.updateCockpitMaxFrames(it.toInt()) } },
                modifier = Modifier.semantics { contentDescription = "Voice: adjust Max Frames" }
            )

            SettingsDropdownRow(
                title = "Auto-Save",
                subtitle = "Interval for automatic frame state saving",
                icon = Icons.Default.Save,
                selected = settings.cockpitAutosaveInterval,
                options = AUTOSAVE_OPTIONS.map { it.first },
                optionLabel = { key ->
                    AUTOSAVE_OPTIONS.find { it.first == key }?.second ?: key
                },
                onSelected = { interval ->
                    scope.launch { repository.updateCockpitAutosaveInterval(interval) }
                },
                modifier = Modifier.semantics { contentDescription = "Voice: select Auto-Save interval" }
            )

            SettingsDropdownRow(
                title = "Background Scene",
                subtitle = "Visual background behind frames",
                icon = Icons.Default.Wallpaper,
                selected = settings.cockpitBackgroundScene,
                options = BACKGROUND_SCENE_OPTIONS.map { it.first },
                optionLabel = { key ->
                    BACKGROUND_SCENE_OPTIONS.find { it.first == key }?.second ?: key
                },
                onSelected = { scene ->
                    scope.launch { repository.updateCockpitBackgroundScene(scene) }
                },
                modifier = Modifier.semantics { contentDescription = "Voice: select Background Scene" }
            )
        }
    }

    @Composable
    private fun SpatialSection(settings: AvanuesSettings, scope: kotlinx.coroutines.CoroutineScope) {
        SettingsGroupCard {
            SettingsSwitchRow(
                title = "Head Tracking",
                subtitle = if (settings.cockpitSpatialEnabled)
                    "IMU-based spatial canvas active"
                else
                    "Enable head-tracking spatial canvas",
                icon = Icons.Default.Sensors,
                checked = settings.cockpitSpatialEnabled,
                onCheckedChange = { scope.launch { repository.updateCockpitSpatialEnabled(it) } },
                modifier = Modifier.semantics { contentDescription = "Voice: toggle Head Tracking" }
            )

            if (settings.cockpitSpatialEnabled) {
                SettingsDropdownRow(
                    title = "Sensitivity",
                    subtitle = "Head tracking response level",
                    icon = Icons.Default.Tune,
                    selected = settings.cockpitSpatialSensitivity,
                    options = SENSITIVITY_OPTIONS.map { it.first },
                    optionLabel = { key ->
                        SENSITIVITY_OPTIONS.find { it.first == key }?.second ?: key
                    },
                    onSelected = { sensitivity ->
                        scope.launch { repository.updateCockpitSpatialSensitivity(sensitivity) }
                    },
                    modifier = Modifier.semantics { contentDescription = "Voice: select Sensitivity" }
                )

                SettingsSwitchRow(
                    title = "Canvas Zoom Persist",
                    subtitle = "Remember zoom level across sessions",
                    icon = Icons.Default.Panorama,
                    checked = settings.cockpitCanvasZoomPersist,
                    onCheckedChange = { scope.launch { repository.updateCockpitCanvasZoomPersist(it) } },
                    modifier = Modifier.semantics { contentDescription = "Voice: toggle Canvas Zoom Persist" }
                )
            }
        }
    }

    @Composable
    override fun ModuleIcon(): ImageVector = Icons.Default.Dashboard
}
