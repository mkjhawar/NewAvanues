/**
 * VoiceControlSettingsProvider.kt - Voice feedback, locale, and wake word settings
 *
 * Provides voice feedback toggle, voice command language selector, and wake word
 * configuration. State persisted via AvanuesSettingsRepository (DataStore).
 *
 * Locale changes trigger CommandManager.switchLocale() via the accessibility
 * service's DataStore observation loop.
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 */

package com.augmentalis.voiceavanue.ui.settings.providers

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.graphics.vector.ImageVector
import com.augmentalis.avanueui.components.settings.SettingsDropdownRow
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

    companion object {
        /** Supported voice command locales — matches bundled .VOS seed files. */
        val SUPPORTED_LOCALES = listOf(
            "en-US" to "English (US)",
            "es-ES" to "Español (España)",
            "fr-FR" to "Français (France)",
            "de-DE" to "Deutsch (Deutschland)",
            "hi-IN" to "Hindi (India)"
        )
    }

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
            key = "voice_locale",
            displayName = "Voice Command Language",
            sectionId = "voice",
            keywords = listOf("language", "locale", "spanish", "french", "german", "hindi", "localization", "translation")
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

            SettingsDropdownRow(
                title = "Voice Command Language",
                subtitle = "Language for voice commands and help menu",
                icon = Icons.Default.Language,
                selected = settings.voiceLocale,
                options = SUPPORTED_LOCALES.map { it.first },
                optionLabel = { code ->
                    SUPPORTED_LOCALES.find { it.first == code }?.second ?: code
                },
                onSelected = { locale ->
                    scope.launch { repository.updateVoiceLocale(locale) }
                }
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
