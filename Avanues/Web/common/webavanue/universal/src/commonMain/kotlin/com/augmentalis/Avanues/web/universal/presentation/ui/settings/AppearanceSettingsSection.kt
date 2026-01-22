package com.augmentalis.Avanues.web.universal.presentation.ui.settings

import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import com.augmentalis.webavanue.domain.model.BrowserSettings
import com.augmentalis.Avanues.web.universal.presentation.viewmodel.SettingsViewModel

/**
 * AppearanceSettingsSection - Appearance and theme settings content
 *
 * Contains:
 * - Theme selection (Light/Dark/System/Auto)
 * - Font size settings
 * - Image display toggle
 * - Force zoom settings
 * - Initial page scale
 */

/**
 * Adds Appearance settings items to a LazyColumn for portrait mode
 */
fun LazyListScope.appearanceSettingsPortraitItems(
    settings: BrowserSettings,
    viewModel: SettingsViewModel
) {
    item {
        SettingsSectionHeader("Appearance")
    }

    item {
        ThemeSettingItem(
            currentTheme = settings.theme,
            onThemeSelected = { viewModel.setTheme(it) }
        )
    }

    item {
        FontSizeSettingItem(
            currentFontSize = settings.fontSize,
            onFontSizeSelected = {
                viewModel.updateSettings(settings.copy(fontSize = it))
            }
        )
    }

    item {
        SwitchSettingItem(
            title = "Show Images",
            subtitle = "Display images on web pages",
            checked = settings.showImages,
            onCheckedChange = {
                viewModel.updateSettings(settings.copy(showImages = it))
            }
        )
    }

    item {
        SwitchSettingItem(
            title = "Force Zoom",
            subtitle = "Allow zooming on all pages",
            checked = settings.forceZoom,
            onCheckedChange = {
                viewModel.updateSettings(settings.copy(forceZoom = it))
            }
        )
    }

    item {
        SliderSettingItem(
            title = "Initial Page Scale",
            subtitle = "Scale: ${(settings.initialScale * 100).toInt()}%",
            value = settings.initialScale,
            valueRange = 0.5f..2.0f,
            steps = 29,
            onValueChange = {
                viewModel.updateSettings(settings.copy(initialScale = it))
            }
        )
    }
}

/**
 * Adds Appearance settings items to a LazyColumn for landscape mode with AR/XR card wrappers
 */
fun LazyListScope.appearanceSettingsLandscapeItems(
    settings: BrowserSettings,
    viewModel: SettingsViewModel
) {
    item {
        ARXRSettingCard {
            ThemeSettingItem(
                currentTheme = settings.theme,
                onThemeSelected = { viewModel.setTheme(it) }
            )
        }
    }
    item {
        ARXRSettingCard {
            FontSizeSettingItem(
                currentFontSize = settings.fontSize,
                onFontSizeSelected = {
                    viewModel.updateSettings(settings.copy(fontSize = it))
                }
            )
        }
    }
    item {
        ARXRSettingCard {
            SwitchSettingItem(
                title = "Show Images",
                subtitle = "Display images on web pages",
                checked = settings.showImages,
                onCheckedChange = {
                    viewModel.updateSettings(settings.copy(showImages = it))
                }
            )
        }
    }
    item {
        ARXRSettingCard {
            SwitchSettingItem(
                title = "Force Zoom",
                subtitle = "Allow zooming on all pages",
                checked = settings.forceZoom,
                onCheckedChange = {
                    viewModel.updateSettings(settings.copy(forceZoom = it))
                }
            )
        }
    }
    item {
        ARXRSettingCard {
            SliderSettingItem(
                title = "Initial Page Scale",
                subtitle = "Scale: ${(settings.initialScale * 100).toInt()}%",
                value = settings.initialScale,
                valueRange = 0.5f..2.0f,
                steps = 29,
                onValueChange = {
                    viewModel.updateSettings(settings.copy(initialScale = it))
                }
            )
        }
    }
}
