/**
 * VoiceCursorSettingsProvider.kt - Cursor and dwell click settings
 *
 * Provides dwell click enable/disable, delay slider, and cursor smoothing.
 * All state persisted via AvanuesSettingsRepository (DataStore).
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 */

package com.augmentalis.voiceavanue.ui.settings.providers

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Tune
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.graphics.vector.ImageVector
import com.augmentalis.avanueui.components.settings.SettingsGroupCard
import com.augmentalis.avanueui.components.settings.SettingsSliderRow
import com.augmentalis.avanueui.components.settings.SettingsSwitchRow
import com.augmentalis.foundation.settings.SearchableSettingEntry
import com.augmentalis.foundation.settings.SettingsSection
import com.augmentalis.voiceavanue.data.AvanuesSettings
import com.augmentalis.voiceavanue.data.AvanuesSettingsRepository
import com.augmentalis.voiceavanue.ui.settings.ComposableSettingsProvider
import kotlinx.coroutines.launch
import javax.inject.Inject

class VoiceCursorSettingsProvider @Inject constructor(
    private val repository: AvanuesSettingsRepository
) : ComposableSettingsProvider {

    override val moduleId = "voicecursor"
    override val displayName = "VoiceCursor"
    override val iconName = "TouchApp"
    override val sortOrder = 200

    override val sections = listOf(
        SettingsSection(id = "cursor", title = "Cursor")
    )

    override val searchableEntries = listOf(
        SearchableSettingEntry(
            key = "dwell_click_enabled",
            displayName = "Dwell Click",
            sectionId = "cursor",
            keywords = listOf("auto-click", "dwell", "tap", "cursor click")
        ),
        SearchableSettingEntry(
            key = "dwell_click_delay",
            displayName = "Dwell Click Delay",
            sectionId = "cursor",
            keywords = listOf("delay", "timing", "milliseconds", "dwell time")
        ),
        SearchableSettingEntry(
            key = "cursor_smoothing",
            displayName = "Cursor Smoothing",
            sectionId = "cursor",
            keywords = listOf("smoothing", "jitter", "stabilize", "cursor")
        )
    )

    @Composable
    override fun SectionContent(sectionId: String) {
        val settings by repository.settings.collectAsState(initial = AvanuesSettings())
        val scope = rememberCoroutineScope()

        SettingsGroupCard {
            SettingsSwitchRow(
                title = "Dwell Click",
                subtitle = "Auto-click when cursor stays still",
                icon = Icons.Default.TouchApp,
                checked = settings.dwellClickEnabled,
                onCheckedChange = { scope.launch { repository.updateDwellClickEnabled(it) } }
            )

            SettingsSliderRow(
                title = "Dwell Click Delay",
                subtitle = "${settings.dwellClickDelayMs.toInt()} ms",
                icon = Icons.Default.Timer,
                value = settings.dwellClickDelayMs,
                valueRange = 500f..3000f,
                onValueChange = { scope.launch { repository.updateDwellClickDelay(it) } }
            )

            SettingsSwitchRow(
                title = "Cursor Smoothing",
                subtitle = "Reduce cursor jitter",
                icon = Icons.Default.Tune,
                checked = settings.cursorSmoothing,
                onCheckedChange = { scope.launch { repository.updateCursorSmoothing(it) } }
            )
        }
    }

    @Composable
    override fun ModuleIcon(): ImageVector = Icons.Default.TouchApp
}
