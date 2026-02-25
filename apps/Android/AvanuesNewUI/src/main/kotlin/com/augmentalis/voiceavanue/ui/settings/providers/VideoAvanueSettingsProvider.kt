/**
 * VideoAvanueSettingsProvider.kt - Video playback and audio settings
 *
 * Two sections:
 * - Playback: default speed, resume position, repeat mode
 * - Audio: default volume, mute by default
 *
 * State persisted via AvanuesSettingsRepository (DataStore).
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 */

package com.augmentalis.voiceavanue.ui.settings.providers

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.SlowMotionVideo
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
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

class VideoAvanueSettingsProvider @Inject constructor(
    private val repository: AvanuesSettingsRepository
) : ComposableSettingsProvider {

    companion object {
        /** Supported playback speeds with display labels. */
        val SPEED_OPTIONS = listOf(
            "0.5" to "0.5x",
            "0.75" to "0.75x",
            "1.0" to "1.0x",
            "1.25" to "1.25x",
            "1.5" to "1.5x",
            "2.0" to "2.0x"
        )

        /** Repeat mode values with display labels. */
        val REPEAT_OPTIONS = listOf(
            "Off" to "Off",
            "One" to "One",
            "All" to "All"
        )
    }

    override val moduleId = "videoavanue"
    override val displayName = "VideoAvanue"
    override val iconName = "VideoLibrary"
    override val sortOrder = 550

    override val sections = listOf(
        SettingsSection(id = "playback", title = "Playback", sortOrder = 0),
        SettingsSection(id = "audio", title = "Audio", sortOrder = 1)
    )

    override val searchableEntries = listOf(
        SearchableSettingEntry(
            key = "video_default_speed",
            displayName = "Default Speed",
            sectionId = "playback",
            keywords = listOf("speed", "playback", "rate", "slow motion", "fast", "0.5x", "2x")
        ),
        SearchableSettingEntry(
            key = "video_resume",
            displayName = "Resume Playback",
            sectionId = "playback",
            keywords = listOf("resume", "continue", "position", "remember", "where I left off")
        ),
        SearchableSettingEntry(
            key = "video_repeat_mode",
            displayName = "Repeat Mode",
            sectionId = "playback",
            keywords = listOf("repeat", "loop", "replay", "cycle", "one", "all")
        ),
        SearchableSettingEntry(
            key = "video_default_volume",
            displayName = "Default Volume",
            sectionId = "audio",
            keywords = listOf("volume", "loudness", "audio", "sound level")
        ),
        SearchableSettingEntry(
            key = "video_mute_default",
            displayName = "Mute by Default",
            sectionId = "audio",
            keywords = listOf("mute", "silent", "no sound", "quiet")
        )
    )

    @Composable
    override fun SectionContent(sectionId: String) {
        when (sectionId) {
            "playback" -> PlaybackSection()
            "audio" -> AudioSection()
        }
    }

    @Composable
    private fun PlaybackSection() {
        val settings by repository.settings.collectAsState(initial = AvanuesSettings())
        val scope = rememberCoroutineScope()

        SettingsGroupCard {
            SettingsDropdownRow(
                title = "Default Speed",
                subtitle = "Playback speed for new videos",
                icon = Icons.Default.SlowMotionVideo,
                selected = settings.videoDefaultSpeed,
                options = SPEED_OPTIONS.map { it.first },
                optionLabel = { code ->
                    SPEED_OPTIONS.find { it.first == code }?.second ?: code
                },
                onSelected = { speed ->
                    scope.launch { repository.updateVideoDefaultSpeed(speed) }
                },
                modifier = Modifier.semantics { contentDescription = "Voice: select Default Speed" }
            )

            SettingsSwitchRow(
                title = "Resume Playback",
                subtitle = "Continue from where you left off",
                icon = Icons.Default.Speed,
                checked = settings.videoResume,
                onCheckedChange = { scope.launch { repository.updateVideoResume(it) } },
                modifier = Modifier.semantics { contentDescription = "Voice: toggle Resume Playback" }
            )

            SettingsDropdownRow(
                title = "Repeat Mode",
                subtitle = "How videos repeat after finishing",
                icon = Icons.Default.Repeat,
                selected = settings.videoRepeatMode,
                options = REPEAT_OPTIONS.map { it.first },
                optionLabel = { code ->
                    REPEAT_OPTIONS.find { it.first == code }?.second ?: code
                },
                onSelected = { mode ->
                    scope.launch { repository.updateVideoRepeatMode(mode) }
                },
                modifier = Modifier.semantics { contentDescription = "Voice: select Repeat Mode" }
            )
        }
    }

    @Composable
    private fun AudioSection() {
        val settings by repository.settings.collectAsState(initial = AvanuesSettings())
        val scope = rememberCoroutineScope()

        SettingsGroupCard {
            SettingsSliderRow(
                title = "Default Volume",
                subtitle = "Initial volume level for video playback",
                icon = Icons.Default.VolumeUp,
                value = settings.videoDefaultVolume.toFloat(),
                valueRange = 0f..100f,
                steps = 9,
                valueLabel = "${settings.videoDefaultVolume}%",
                onValueChange = { scope.launch { repository.updateVideoDefaultVolume(it.toInt()) } },
                modifier = Modifier.semantics { contentDescription = "Voice: adjust Default Volume" }
            )

            SettingsSwitchRow(
                title = "Mute by Default",
                subtitle = "Start videos with audio muted",
                icon = Icons.Default.VolumeOff,
                checked = settings.videoMuteDefault,
                onCheckedChange = { scope.launch { repository.updateVideoMuteDefault(it) } },
                modifier = Modifier.semantics { contentDescription = "Voice: toggle Mute by Default" }
            )
        }
    }

    @Composable
    override fun ModuleIcon(): ImageVector = Icons.Default.VideoLibrary
}
