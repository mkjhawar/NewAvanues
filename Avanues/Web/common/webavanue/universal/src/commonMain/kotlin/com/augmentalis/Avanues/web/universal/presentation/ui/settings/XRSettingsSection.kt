package com.augmentalis.Avanues.web.universal.presentation.ui.settings

import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import com.augmentalis.webavanue.domain.model.BrowserSettings
import com.augmentalis.Avanues.web.universal.presentation.viewmodel.SettingsViewModel

/**
 * XRSettingsSection - AR/XR and WebXR settings content
 *
 * Contains:
 * - WebXR master switch
 * - AR enable/disable
 * - VR enable/disable
 * - XR performance mode selection
 * - Auto-pause timeout
 * - FPS indicator toggle
 * - Wi-Fi requirement for XR
 * - Navigation to detailed XR settings
 */

/**
 * Adds XR/AR settings items to a LazyColumn for portrait mode
 */
fun LazyListScope.xrSettingsPortraitItems(
    settings: BrowserSettings,
    viewModel: SettingsViewModel,
    onNavigateToXRSettings: () -> Unit
) {
    // WebXR section
    item {
        SettingsSectionHeader("WebXR")
    }

    item {
        SwitchSettingItem(
            title = "Enable WebXR",
            subtitle = "Master switch for WebXR functionality",
            checked = settings.enableWebXR,
            onCheckedChange = {
                viewModel.updateSettings(settings.copy(enableWebXR = it))
            }
        )
    }

    if (settings.enableWebXR) {
        item {
            SwitchSettingItem(
                title = "Enable AR",
                subtitle = "Allow immersive augmented reality sessions",
                checked = settings.enableAR,
                onCheckedChange = {
                    viewModel.updateSettings(settings.copy(enableAR = it))
                }
            )
        }

        item {
            SwitchSettingItem(
                title = "Enable VR",
                subtitle = "Allow immersive virtual reality sessions",
                checked = settings.enableVR,
                onCheckedChange = {
                    viewModel.updateSettings(settings.copy(enableVR = it))
                }
            )
        }

        item {
            XRPerformanceModeSettingItem(
                currentMode = settings.xrPerformanceMode,
                onModeSelected = {
                    viewModel.updateSettings(settings.copy(xrPerformanceMode = it))
                }
            )
        }

        item {
            SliderSettingItem(
                title = "XR Auto-Pause Timeout",
                subtitle = "Auto-pause after ${settings.xrAutoPauseTimeout} minutes",
                value = settings.xrAutoPauseTimeout.toFloat(),
                valueRange = 10f..120f,
                steps = 21,
                onValueChange = {
                    viewModel.updateSettings(settings.copy(xrAutoPauseTimeout = it.toInt()))
                }
            )
        }

        item {
            SwitchSettingItem(
                title = "Show FPS Indicator",
                subtitle = "Display frame rate in XR sessions",
                checked = settings.xrShowFPSIndicator,
                onCheckedChange = {
                    viewModel.updateSettings(settings.copy(xrShowFPSIndicator = it))
                }
            )
        }

        item {
            SwitchSettingItem(
                title = "Require Wi-Fi for XR",
                subtitle = "Only allow XR on Wi-Fi connections",
                checked = settings.xrRequireWiFi,
                onCheckedChange = {
                    viewModel.updateSettings(settings.copy(xrRequireWiFi = it))
                }
            )
        }
    }

    item {
        NavigationSettingItem(
            title = "WebXR Settings",
            subtitle = "Configure AR/VR preferences",
            onClick = onNavigateToXRSettings
        )
    }
}

/**
 * Adds XR/AR settings items to a LazyColumn for landscape mode with AR/XR card wrappers
 */
fun LazyListScope.xrSettingsLandscapeItems(
    settings: BrowserSettings,
    viewModel: SettingsViewModel,
    onNavigateToXRSettings: () -> Unit
) {
    item {
        ARXRSettingCard {
            SwitchSettingItem(
                title = "Enable WebXR",
                subtitle = "Master switch for WebXR functionality",
                checked = settings.enableWebXR,
                onCheckedChange = {
                    viewModel.updateSettings(settings.copy(enableWebXR = it))
                }
            )
        }
    }
    if (settings.enableWebXR) {
        item {
            ARXRSettingCard {
                SwitchSettingItem(
                    title = "Enable AR",
                    subtitle = "Allow immersive augmented reality sessions",
                    checked = settings.enableAR,
                    onCheckedChange = {
                        viewModel.updateSettings(settings.copy(enableAR = it))
                    }
                )
            }
        }
        item {
            ARXRSettingCard {
                SwitchSettingItem(
                    title = "Enable VR",
                    subtitle = "Allow immersive virtual reality sessions",
                    checked = settings.enableVR,
                    onCheckedChange = {
                        viewModel.updateSettings(settings.copy(enableVR = it))
                    }
                )
            }
        }
        item {
            ARXRSettingCard {
                XRPerformanceModeSettingItem(
                    currentMode = settings.xrPerformanceMode,
                    onModeSelected = {
                        viewModel.updateSettings(settings.copy(xrPerformanceMode = it))
                    }
                )
            }
        }
        item {
            ARXRSettingCard {
                SliderSettingItem(
                    title = "XR Auto-Pause Timeout",
                    subtitle = "Auto-pause after ${settings.xrAutoPauseTimeout} minutes",
                    value = settings.xrAutoPauseTimeout.toFloat(),
                    valueRange = 10f..120f,
                    steps = 21,
                    onValueChange = {
                        viewModel.updateSettings(settings.copy(xrAutoPauseTimeout = it.toInt()))
                    }
                )
            }
        }
        item {
            ARXRSettingCard {
                SwitchSettingItem(
                    title = "Show FPS Indicator",
                    subtitle = "Display frame rate in XR sessions",
                    checked = settings.xrShowFPSIndicator,
                    onCheckedChange = {
                        viewModel.updateSettings(settings.copy(xrShowFPSIndicator = it))
                    }
                )
            }
        }
        item {
            ARXRSettingCard {
                SwitchSettingItem(
                    title = "Require Wi-Fi for XR",
                    subtitle = "Only allow XR on Wi-Fi connections",
                    checked = settings.xrRequireWiFi,
                    onCheckedChange = {
                        viewModel.updateSettings(settings.copy(xrRequireWiFi = it))
                    }
                )
            }
        }
    }
    item {
        ARXRSettingCard {
            NavigationSettingItem(
                title = "WebXR Settings",
                subtitle = "Configure AR/VR preferences",
                onClick = onNavigateToXRSettings
            )
        }
    }
}
