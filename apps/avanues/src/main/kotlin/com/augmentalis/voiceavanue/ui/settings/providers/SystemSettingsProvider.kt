/**
 * SystemSettingsProvider.kt - System-level settings (boot, etc.)
 *
 * Provides system settings like "Start on Boot".
 * State persisted via AvanuesSettingsRepository (DataStore).
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 */

package com.augmentalis.voiceavanue.ui.settings.providers

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.graphics.vector.ImageVector
import com.augmentalis.avanueui.components.settings.SettingsGroupCard
import com.augmentalis.avanueui.components.settings.SettingsSwitchRow
import com.augmentalis.foundation.settings.SearchableSettingEntry
import com.augmentalis.foundation.settings.SettingsSection
import com.augmentalis.voiceavanue.data.AvanuesSettings
import com.augmentalis.voiceavanue.data.AvanuesSettingsRepository
import com.augmentalis.voiceavanue.ui.settings.ComposableSettingsProvider
import kotlinx.coroutines.launch
import javax.inject.Inject

class SystemSettingsProvider @Inject constructor(
    private val repository: AvanuesSettingsRepository
) : ComposableSettingsProvider {

    override val moduleId = "system"
    override val displayName = "System"
    override val iconName = "Settings"
    override val sortOrder = 500

    override val sections = listOf(
        SettingsSection(id = "system", title = "System")
    )

    override val searchableEntries = listOf(
        SearchableSettingEntry(
            key = "auto_start_on_boot",
            displayName = "Start on Boot",
            sectionId = "system",
            keywords = listOf("boot", "startup", "autostart", "launch", "restart")
        )
    )

    @Composable
    override fun SectionContent(sectionId: String) {
        val settings by repository.settings.collectAsState(initial = AvanuesSettings())
        val scope = rememberCoroutineScope()

        SettingsGroupCard {
            SettingsSwitchRow(
                title = "Start on Boot",
                subtitle = "Launch AVA when device starts",
                icon = Icons.Default.PowerSettingsNew,
                checked = settings.autoStartOnBoot,
                onCheckedChange = { scope.launch { repository.updateAutoStartOnBoot(it) } }
            )
        }
    }

    @Composable
    override fun ModuleIcon(): ImageVector = Icons.Default.Settings
}
