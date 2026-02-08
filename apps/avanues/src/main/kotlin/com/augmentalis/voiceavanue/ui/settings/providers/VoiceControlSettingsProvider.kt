/**
 * VoiceControlSettingsProvider.kt - Voice feedback and wake word settings
 *
 * Provides voice feedback toggle and wake word configuration.
 * State persisted via AvanuesSettingsRepository (DataStore).
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 */

package com.augmentalis.voiceavanue.ui.settings.providers

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.graphics.vector.ImageVector
import com.augmentalis.avanueui.components.settings.SettingsGroupCard
import com.augmentalis.avanueui.components.settings.SettingsNavigationRow
import com.augmentalis.avanueui.components.settings.SettingsSwitchRow
import com.augmentalis.foundation.settings.SearchableSettingEntry
import com.augmentalis.foundation.settings.SettingsSection
import com.augmentalis.voiceavanue.data.AvanuesSettings
import com.augmentalis.voiceavanue.data.AvanuesSettingsRepository
import com.augmentalis.voiceavanue.ui.settings.ComposableSettingsProvider
import kotlinx.coroutines.launch
import javax.inject.Inject

class VoiceControlSettingsProvider @Inject constructor(
    private val repository: AvanuesSettingsRepository
) : ComposableSettingsProvider {

    override val moduleId = "voicecontrol"
    override val displayName = "Voice Control"
    override val iconName = "RecordVoiceOver"
    override val sortOrder = 300

    override val sections = listOf(
        SettingsSection(id = "voice", title = "Voice")
    )

    override val searchableEntries = listOf(
        SearchableSettingEntry(
            key = "voice_feedback",
            displayName = "Voice Feedback",
            sectionId = "voice",
            keywords = listOf("feedback", "speak", "confirmation", "audio", "TTS")
        ),
        SearchableSettingEntry(
            key = "wake_word",
            displayName = "Wake Word",
            sectionId = "voice",
            keywords = listOf("wake", "activation", "phrase", "hotword", "trigger")
        )
    )

    @Composable
    override fun SectionContent(sectionId: String) {
        val settings by repository.settings.collectAsState(initial = AvanuesSettings())
        val scope = rememberCoroutineScope()

        SettingsGroupCard {
            SettingsSwitchRow(
                title = "Voice Feedback",
                subtitle = "Speak command confirmations",
                icon = Icons.Default.RecordVoiceOver,
                checked = settings.voiceFeedback,
                onCheckedChange = { scope.launch { repository.updateVoiceFeedback(it) } }
            )

            SettingsNavigationRow(
                title = "Wake Word",
                subtitle = "Configure activation phrase",
                icon = Icons.Default.Mic,
                onClick = { /* Wake word configuration screen */ }
            )
        }
    }

    @Composable
    override fun ModuleIcon(): ImageVector = Icons.Default.RecordVoiceOver
}
