package com.augmentalis.Avanues.web.universal.presentation.ui.settings

import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import com.augmentalis.webavanue.domain.model.BrowserSettings
import com.augmentalis.Avanues.web.universal.presentation.viewmodel.SettingsViewModel

/**
 * GeneralSettingsSection - General browser settings content
 *
 * Contains:
 * - Search engine selection
 * - Homepage configuration
 * - Search suggestions
 * - Voice search
 * - New tab page options
 * - Tab restore/open link behavior
 */

/**
 * Adds General settings items to a LazyColumn for portrait mode
 */
fun LazyListScope.generalSettingsPortraitItems(
    settings: BrowserSettings,
    viewModel: SettingsViewModel
) {
    item {
        SettingsSectionHeader("General")
    }

    item {
        SearchEngineSettingItem(
            currentEngine = settings.defaultSearchEngine,
            onEngineSelected = { viewModel.setDefaultSearchEngine(it) }
        )
    }

    item {
        HomepageSettingItem(
            currentHomepage = settings.homePage,
            onHomepageChanged = { viewModel.setHomepage(it) }
        )
    }

    item {
        SwitchSettingItem(
            title = "Search Suggestions",
            subtitle = "Show suggestions as you type",
            checked = settings.searchSuggestions,
            onCheckedChange = {
                viewModel.updateSettings(settings.copy(searchSuggestions = it))
            }
        )
    }

    item {
        SwitchSettingItem(
            title = "Voice Search",
            subtitle = "Enable voice search in search bar",
            checked = settings.voiceSearch,
            onCheckedChange = {
                viewModel.updateSettings(settings.copy(voiceSearch = it))
            }
        )
    }

    item {
        NewTabPageSettingItem(
            currentNewTabPage = settings.newTabPage,
            onNewTabPageSelected = {
                viewModel.updateSettings(settings.copy(newTabPage = it))
            }
        )
    }

    item {
        SwitchSettingItem(
            title = "Restore Tabs on Startup",
            subtitle = "Reopen tabs from previous session",
            checked = settings.restoreTabsOnStartup,
            onCheckedChange = {
                viewModel.updateSettings(settings.copy(restoreTabsOnStartup = it))
            }
        )
    }

    item {
        SwitchSettingItem(
            title = "Open Links in Background",
            subtitle = "Don't switch to new tabs immediately",
            checked = settings.openLinksInBackground,
            onCheckedChange = {
                viewModel.updateSettings(settings.copy(openLinksInBackground = it))
            }
        )
    }

    item {
        SwitchSettingItem(
            title = "Open Links in New Tab",
            subtitle = "Always open links in new tabs",
            checked = settings.openLinksInNewTab,
            onCheckedChange = {
                viewModel.updateSettings(settings.copy(openLinksInNewTab = it))
            }
        )
    }
}

/**
 * Adds General settings items to a LazyColumn for landscape mode with AR/XR card wrappers
 */
fun LazyListScope.generalSettingsLandscapeItems(
    settings: BrowserSettings,
    viewModel: SettingsViewModel
) {
    item {
        ARXRSettingCard {
            SearchEngineSettingItem(
                currentEngine = settings.defaultSearchEngine,
                onEngineSelected = { viewModel.setDefaultSearchEngine(it) }
            )
        }
    }
    item {
        ARXRSettingCard {
            HomepageSettingItem(
                currentHomepage = settings.homePage,
                onHomepageChanged = { viewModel.setHomepage(it) }
            )
        }
    }
    item {
        ARXRSettingCard {
            SwitchSettingItem(
                title = "Search Suggestions",
                subtitle = "Show suggestions as you type",
                checked = settings.searchSuggestions,
                onCheckedChange = {
                    viewModel.updateSettings(settings.copy(searchSuggestions = it))
                }
            )
        }
    }
    item {
        ARXRSettingCard {
            SwitchSettingItem(
                title = "Voice Search",
                subtitle = "Enable voice search in search bar",
                checked = settings.voiceSearch,
                onCheckedChange = {
                    viewModel.updateSettings(settings.copy(voiceSearch = it))
                }
            )
        }
    }
    item {
        ARXRSettingCard {
            NewTabPageSettingItem(
                currentNewTabPage = settings.newTabPage,
                onNewTabPageSelected = {
                    viewModel.updateSettings(settings.copy(newTabPage = it))
                }
            )
        }
    }
    item {
        ARXRSettingCard {
            SwitchSettingItem(
                title = "Restore Tabs on Startup",
                subtitle = "Reopen tabs from previous session",
                checked = settings.restoreTabsOnStartup,
                onCheckedChange = {
                    viewModel.updateSettings(settings.copy(restoreTabsOnStartup = it))
                }
            )
        }
    }
    item {
        ARXRSettingCard {
            SwitchSettingItem(
                title = "Open Links in Background",
                subtitle = "Don't switch to new tabs immediately",
                checked = settings.openLinksInBackground,
                onCheckedChange = {
                    viewModel.updateSettings(settings.copy(openLinksInBackground = it))
                }
            )
        }
    }
    item {
        ARXRSettingCard {
            SwitchSettingItem(
                title = "Open Links in New Tab",
                subtitle = "Always open links in new tabs",
                checked = settings.openLinksInNewTab,
                onCheckedChange = {
                    viewModel.updateSettings(settings.copy(openLinksInNewTab = it))
                }
            )
        }
    }
}
