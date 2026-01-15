package com.augmentalis.Avanues.web.universal.presentation.ui.settings

import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import com.augmentalis.webavanue.domain.model.BrowserSettings
import com.augmentalis.Avanues.web.universal.presentation.viewmodel.SettingsViewModel

/**
 * PrivacySettingsSection - Privacy and security settings content
 *
 * Contains:
 * - JavaScript enable/disable
 * - Cookies enable/disable
 * - Pop-up blocker
 * - Ad blocker
 * - Tracker blocker
 * - Do Not Track header
 * - WebRTC settings
 * - Clear on exit options (cache, history, cookies)
 * - Site permissions navigation
 */

/**
 * Adds Privacy & Security settings items to a LazyColumn for portrait mode
 */
fun LazyListScope.privacySettingsPortraitItems(
    settings: BrowserSettings,
    viewModel: SettingsViewModel,
    onNavigateToSitePermissions: () -> Unit
) {
    item {
        SettingsSectionHeader("Privacy & Security")
    }

    item {
        SwitchSettingItem(
            title = "Enable JavaScript",
            subtitle = "Required for most modern websites",
            checked = settings.enableJavaScript,
            onCheckedChange = { viewModel.setEnableJavaScript(it) }
        )
    }

    item {
        SwitchSettingItem(
            title = "Enable Cookies",
            subtitle = "Allow websites to store data",
            checked = settings.enableCookies,
            onCheckedChange = { viewModel.setEnableCookies(it) }
        )
    }

    item {
        SwitchSettingItem(
            title = "Block Pop-ups",
            subtitle = "Prevent pop-up windows",
            checked = settings.blockPopups,
            onCheckedChange = { viewModel.setBlockPopups(it) }
        )
    }

    item {
        SwitchSettingItem(
            title = "Block Ads",
            subtitle = "Block advertisements on web pages",
            checked = settings.blockAds,
            onCheckedChange = { viewModel.setBlockAds(it) }
        )
    }

    item {
        SwitchSettingItem(
            title = "Block Trackers",
            subtitle = "Prevent cross-site tracking",
            checked = settings.blockTrackers,
            onCheckedChange = { viewModel.setBlockTrackers(it) }
        )
    }

    item {
        SwitchSettingItem(
            title = "Do Not Track",
            subtitle = "Send Do Not Track header with requests",
            checked = settings.doNotTrack,
            onCheckedChange = {
                viewModel.updateSettings(settings.copy(doNotTrack = it))
            }
        )
    }

    item {
        SwitchSettingItem(
            title = "Enable WebRTC",
            subtitle = "Allow real-time communication features",
            checked = settings.enableWebRTC,
            onCheckedChange = {
                viewModel.updateSettings(settings.copy(enableWebRTC = it))
            }
        )
    }

    item {
        SwitchSettingItem(
            title = "Clear Cache on Exit",
            subtitle = "Automatically clear browser cache",
            checked = settings.clearCacheOnExit,
            onCheckedChange = {
                viewModel.updateSettings(settings.copy(clearCacheOnExit = it))
            }
        )
    }

    item {
        SwitchSettingItem(
            title = "Clear History on Exit",
            subtitle = "Automatically clear browsing history",
            checked = settings.clearHistoryOnExit,
            onCheckedChange = {
                viewModel.updateSettings(settings.copy(clearHistoryOnExit = it))
            }
        )
    }

    item {
        SwitchSettingItem(
            title = "Clear Cookies on Exit",
            subtitle = "Automatically clear all cookies",
            checked = settings.clearCookiesOnExit,
            onCheckedChange = {
                viewModel.updateSettings(settings.copy(clearCookiesOnExit = it))
            }
        )
    }

    item {
        NavigationSettingItem(
            title = "Site Permissions",
            subtitle = "Manage camera, microphone, and location permissions",
            onClick = onNavigateToSitePermissions
        )
    }
}

/**
 * Adds Privacy & Security settings items to a LazyColumn for landscape mode with AR/XR card wrappers
 */
fun LazyListScope.privacySettingsLandscapeItems(
    settings: BrowserSettings,
    viewModel: SettingsViewModel,
    onNavigateToSitePermissions: () -> Unit
) {
    item {
        ARXRSettingCard {
            SwitchSettingItem(
                title = "Enable JavaScript",
                subtitle = "Required for most modern websites",
                checked = settings.enableJavaScript,
                onCheckedChange = { viewModel.setEnableJavaScript(it) }
            )
        }
    }
    item {
        ARXRSettingCard {
            SwitchSettingItem(
                title = "Enable Cookies",
                subtitle = "Allow websites to store data",
                checked = settings.enableCookies,
                onCheckedChange = { viewModel.setEnableCookies(it) }
            )
        }
    }
    item {
        ARXRSettingCard {
            SwitchSettingItem(
                title = "Block Pop-ups",
                subtitle = "Prevent pop-up windows",
                checked = settings.blockPopups,
                onCheckedChange = { viewModel.setBlockPopups(it) }
            )
        }
    }
    item {
        ARXRSettingCard {
            SwitchSettingItem(
                title = "Block Ads",
                subtitle = "Block advertisements on web pages",
                checked = settings.blockAds,
                onCheckedChange = { viewModel.setBlockAds(it) }
            )
        }
    }
    item {
        ARXRSettingCard {
            SwitchSettingItem(
                title = "Block Trackers",
                subtitle = "Prevent cross-site tracking",
                checked = settings.blockTrackers,
                onCheckedChange = { viewModel.setBlockTrackers(it) }
            )
        }
    }
    item {
        ARXRSettingCard {
            SwitchSettingItem(
                title = "Do Not Track",
                subtitle = "Send Do Not Track header with requests",
                checked = settings.doNotTrack,
                onCheckedChange = {
                    viewModel.updateSettings(settings.copy(doNotTrack = it))
                }
            )
        }
    }
    item {
        ARXRSettingCard {
            SwitchSettingItem(
                title = "Enable WebRTC",
                subtitle = "Allow real-time communication features",
                checked = settings.enableWebRTC,
                onCheckedChange = {
                    viewModel.updateSettings(settings.copy(enableWebRTC = it))
                }
            )
        }
    }
    item {
        ARXRSettingCard {
            SwitchSettingItem(
                title = "Clear Cache on Exit",
                subtitle = "Automatically clear browser cache",
                checked = settings.clearCacheOnExit,
                onCheckedChange = {
                    viewModel.updateSettings(settings.copy(clearCacheOnExit = it))
                }
            )
        }
    }
    item {
        ARXRSettingCard {
            SwitchSettingItem(
                title = "Clear History on Exit",
                subtitle = "Automatically clear browsing history",
                checked = settings.clearHistoryOnExit,
                onCheckedChange = {
                    viewModel.updateSettings(settings.copy(clearHistoryOnExit = it))
                }
            )
        }
    }
    item {
        ARXRSettingCard {
            SwitchSettingItem(
                title = "Clear Cookies on Exit",
                subtitle = "Automatically clear all cookies",
                checked = settings.clearCookiesOnExit,
                onCheckedChange = {
                    viewModel.updateSettings(settings.copy(clearCookiesOnExit = it))
                }
            )
        }
    }
    item {
        ARXRSettingCard {
            NavigationSettingItem(
                title = "Site Permissions",
                subtitle = "Manage camera, microphone, and location permissions",
                onClick = onNavigateToSitePermissions
            )
        }
    }
}
