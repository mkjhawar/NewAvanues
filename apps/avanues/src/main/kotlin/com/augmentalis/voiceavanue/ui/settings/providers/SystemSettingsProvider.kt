/**
 * SystemSettingsProvider.kt - System-level settings (theme, boot)
 *
 * Theme v5.1: Three independent dropdowns for palette, material style, and appearance.
 * State persisted via AvanuesSettingsRepository (DataStore).
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 */

package com.augmentalis.voiceavanue.ui.settings.providers

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Brightness6
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.graphics.vector.ImageVector
import com.augmentalis.avanueui.components.settings.SettingsDropdownRow
import com.augmentalis.avanueui.components.settings.SettingsGroupCard
import com.augmentalis.avanueui.components.settings.SettingsSwitchRow
import com.augmentalis.avanueui.theme.AppearanceMode
import com.augmentalis.avanueui.theme.AvanueColorPalette
import com.augmentalis.avanueui.theme.MaterialMode
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
            key = "theme_palette",
            displayName = "Color Palette",
            sectionId = "system",
            keywords = listOf("theme", "color", "palette", "sol", "luna", "terra", "hydra", "appearance")
        ),
        SearchableSettingEntry(
            key = "theme_style",
            displayName = "Material Style",
            sectionId = "system",
            keywords = listOf("style", "glass", "water", "cupertino", "mountainview", "material", "rendering")
        ),
        SearchableSettingEntry(
            key = "theme_appearance",
            displayName = "Appearance",
            sectionId = "system",
            keywords = listOf("appearance", "light", "dark", "auto", "mode", "brightness", "night")
        ),
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

        val currentPalette = AvanueColorPalette.fromString(settings.themePalette)
        val currentStyle = MaterialMode.fromString(settings.themeStyle)
        val currentAppearance = AppearanceMode.fromString(settings.themeAppearance)

        SettingsGroupCard {
            SettingsDropdownRow(
                title = "Color Palette",
                subtitle = "App color scheme",
                icon = Icons.Default.Palette,
                selected = currentPalette,
                options = AvanueColorPalette.entries.toList(),
                optionLabel = { it.displayName },
                onSelected = { scope.launch { repository.updateThemePalette(it.name) } }
            )
            SettingsDropdownRow(
                title = "Material Style",
                subtitle = "Visual rendering style",
                icon = Icons.Default.Brush,
                selected = currentStyle,
                options = MaterialMode.entries.toList(),
                optionLabel = { it.displayName },
                onSelected = { scope.launch { repository.updateThemeStyle(it.name) } }
            )
            SettingsDropdownRow(
                title = "Appearance",
                subtitle = "Light or dark mode",
                icon = Icons.Default.Brightness6,
                selected = currentAppearance,
                options = AppearanceMode.entries.toList(),
                optionLabel = { it.displayName },
                onSelected = { scope.launch { repository.updateThemeAppearance(it.name) } }
            )
        }

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
