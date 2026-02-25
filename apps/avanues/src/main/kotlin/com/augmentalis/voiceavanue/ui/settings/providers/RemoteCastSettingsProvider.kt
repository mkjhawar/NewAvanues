/**
 * RemoteCastSettingsProvider.kt - RemoteCast streaming quality and network settings
 *
 * Provides JPEG quality, target FPS, resolution scale, port selection, and
 * auto-connect toggle. State persisted via AvanuesSettingsRepository (DataStore).
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 */

package com.augmentalis.voiceavanue.ui.settings.providers

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cast
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Wifi
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

class RemoteCastSettingsProvider @Inject constructor(
    private val repository: AvanuesSettingsRepository
) : ComposableSettingsProvider {

    companion object {
        val FPS_OPTIONS = listOf("10", "15", "24", "30")
        val RESOLUTION_SCALE_OPTIONS = listOf("50", "75", "100")
        val PORT_OPTIONS = listOf("54321", "8080", "9090")

        fun resolutionScaleLabel(scale: String): String = "$scale%"
    }

    override val moduleId = "remotecast"
    override val displayName = "RemoteCast"
    override val iconName = "Cast"
    override val sortOrder = 700

    override val sections = listOf(
        SettingsSection(id = "quality", title = "Quality", sortOrder = 0),
        SettingsSection(id = "network", title = "Network", sortOrder = 1)
    )

    override val searchableEntries = listOf(
        SearchableSettingEntry(
            key = "cast_jpeg_quality",
            displayName = "JPEG Quality",
            sectionId = "quality",
            keywords = listOf("jpeg", "compression", "quality", "image", "streaming")
        ),
        SearchableSettingEntry(
            key = "cast_target_fps",
            displayName = "Target FPS",
            sectionId = "quality",
            keywords = listOf("fps", "framerate", "frame rate", "frames per second", "speed")
        ),
        SearchableSettingEntry(
            key = "cast_resolution_scale",
            displayName = "Resolution Scale",
            sectionId = "quality",
            keywords = listOf("resolution", "scale", "size", "downscale")
        ),
        SearchableSettingEntry(
            key = "cast_port",
            displayName = "Port",
            sectionId = "network",
            keywords = listOf("port", "network", "connection", "socket")
        ),
        SearchableSettingEntry(
            key = "cast_auto_connect",
            displayName = "Auto-Connect",
            sectionId = "network",
            keywords = listOf("auto", "connect", "automatic", "reconnect")
        )
    )

    @Composable
    override fun SectionContent(sectionId: String) {
        val settings by repository.settings.collectAsState(initial = AvanuesSettings())
        val scope = rememberCoroutineScope()

        when (sectionId) {
            "quality" -> QualitySection(settings, scope)
            "network" -> NetworkSection(settings, scope)
        }
    }

    @Composable
    private fun QualitySection(settings: AvanuesSettings, scope: kotlinx.coroutines.CoroutineScope) {
        SettingsGroupCard {
            SettingsSliderRow(
                title = "JPEG Quality",
                subtitle = "Higher quality uses more bandwidth",
                icon = Icons.Default.HighQuality,
                value = settings.castJpegQuality.toFloat(),
                valueRange = 30f..100f,
                steps = 13,
                valueLabel = "${settings.castJpegQuality}%",
                onValueChange = { scope.launch { repository.updateCastJpegQuality(it.toInt()) } },
                modifier = Modifier.semantics { contentDescription = "Voice: adjust JPEG Quality" }
            )

            SettingsDropdownRow(
                title = "Target FPS",
                subtitle = "Frames per second for screen casting",
                icon = Icons.Default.Speed,
                selected = settings.castTargetFps.toString(),
                options = FPS_OPTIONS,
                optionLabel = { "$it fps" },
                onSelected = { scope.launch { repository.updateCastTargetFps(it.toInt()) } },
                modifier = Modifier.semantics { contentDescription = "Voice: select Target FPS" }
            )

            SettingsDropdownRow(
                title = "Resolution Scale",
                subtitle = "Percentage of original screen resolution",
                icon = Icons.Default.Tune,
                selected = settings.castResolutionScale,
                options = RESOLUTION_SCALE_OPTIONS,
                optionLabel = { resolutionScaleLabel(it) },
                onSelected = { scope.launch { repository.updateCastResolutionScale(it) } },
                modifier = Modifier.semantics { contentDescription = "Voice: select Resolution Scale" }
            )
        }
    }

    @Composable
    private fun NetworkSection(settings: AvanuesSettings, scope: kotlinx.coroutines.CoroutineScope) {
        SettingsGroupCard {
            SettingsDropdownRow(
                title = "Port",
                subtitle = "WebSocket server port for casting",
                icon = Icons.Default.NetworkCheck,
                selected = settings.castPort.toString(),
                options = PORT_OPTIONS,
                optionLabel = { it },
                onSelected = { scope.launch { repository.updateCastPort(it.toInt()) } },
                modifier = Modifier.semantics { contentDescription = "Voice: select Port" }
            )

            SettingsSwitchRow(
                title = "Auto-Connect",
                subtitle = "Automatically reconnect to last known receiver",
                icon = Icons.Default.Wifi,
                checked = settings.castAutoConnect,
                onCheckedChange = { scope.launch { repository.updateCastAutoConnect(it) } },
                modifier = Modifier.semantics { contentDescription = "Voice: toggle Auto-Connect" }
            )
        }
    }

    @Composable
    override fun ModuleIcon(): ImageVector = Icons.Default.Cast
}
