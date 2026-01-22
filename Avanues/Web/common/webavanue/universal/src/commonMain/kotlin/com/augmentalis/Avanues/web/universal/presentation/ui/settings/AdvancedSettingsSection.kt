package com.augmentalis.Avanues.web.universal.presentation.ui.settings

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.augmentalis.webavanue.domain.model.BrowserSettings
import com.augmentalis.Avanues.web.universal.presentation.viewmodel.SettingsViewModel

/**
 * AdvancedSettingsSection - Advanced browser settings content
 *
 * Contains:
 * - Desktop mode and related settings
 * - Auto-play media settings
 * - Voice commands and dialog settings
 * - Downloads section
 * - Performance section (hardware acceleration, preload, data saver, text reflow)
 * - Sync section
 * - Voice & AI section
 * - Command Bar section
 * - Navigation to XR and AR Preview
 */

/**
 * Adds Advanced settings items to a LazyColumn for portrait mode
 */
fun LazyListScope.advancedSettingsPortraitItems(
    settings: BrowserSettings,
    viewModel: SettingsViewModel,
    onNavigateToXRSettings: () -> Unit,
    onNavigateToARPreview: () -> Unit
) {
    // Advanced section
    item {
        SettingsSectionHeader("Advanced")
    }

    item {
        SwitchSettingItem(
            title = "Desktop Mode",
            subtitle = "Request desktop version of websites",
            checked = settings.useDesktopMode,
            onCheckedChange = { viewModel.setDesktopMode(it) }
        )
    }

    // Desktop Mode sub-settings (only visible when desktop mode is enabled)
    if (settings.useDesktopMode) {
        item {
            SliderSettingItem(
                title = "Default Zoom Level",
                subtitle = "Zoom: ${settings.desktopModeDefaultZoom}%",
                value = settings.desktopModeDefaultZoom.toFloat(),
                valueRange = 50f..200f,
                steps = 29, // 5% increments
                onValueChange = { viewModel.setDesktopModeDefaultZoom(it.toInt()) }
            )
        }

        item {
            SliderSettingItem(
                title = "Window Width",
                subtitle = "Width: ${settings.desktopModeWindowWidth}px",
                value = settings.desktopModeWindowWidth.toFloat(),
                valueRange = 800f..1920f,
                steps = 22, // ~50px increments
                onValueChange = { viewModel.setDesktopModeWindowWidth(it.toInt()) }
            )
        }

        item {
            SliderSettingItem(
                title = "Window Height",
                subtitle = "Height: ${settings.desktopModeWindowHeight}px",
                value = settings.desktopModeWindowHeight.toFloat(),
                valueRange = 600f..1200f,
                steps = 11, // ~50px increments
                onValueChange = { viewModel.setDesktopModeWindowHeight(it.toInt()) }
            )
        }

        item {
            SwitchSettingItem(
                title = "Auto-fit Zoom",
                subtitle = "Automatically adjust zoom to fit content in viewport",
                checked = settings.desktopModeAutoFitZoom,
                onCheckedChange = { viewModel.setDesktopModeAutoFitZoom(it) }
            )
        }
    }

    item {
        AutoPlaySettingItem(
            currentAutoPlay = settings.autoPlay,
            onAutoPlaySelected = { viewModel.setAutoPlay(it) }
        )
    }

    item {
        SwitchSettingItem(
            title = "Voice Commands",
            subtitle = "Control browser with voice",
            checked = settings.enableVoiceCommands,
            onCheckedChange = { viewModel.setEnableVoiceCommands(it) }
        )
    }

    // Voice Dialog Auto-Close settings (only visible when voice commands enabled)
    if (settings.enableVoiceCommands) {
        item {
            SwitchSettingItem(
                title = "Auto-close Voice Dialog",
                subtitle = "Automatically close after command execution",
                checked = settings.voiceDialogAutoClose,
                onCheckedChange = {
                    viewModel.updateSettings(settings.copy(voiceDialogAutoClose = it))
                }
            )
        }

        if (settings.voiceDialogAutoClose) {
            item {
                SliderSettingItem(
                    title = "Auto-close Delay",
                    subtitle = "Delay: ${settings.voiceDialogAutoCloseDelayMs}ms",
                    value = settings.voiceDialogAutoCloseDelayMs.toFloat(),
                    valueRange = 500f..5000f,
                    steps = 9,  // 500ms increments
                    onValueChange = {
                        viewModel.updateSettings(settings.copy(voiceDialogAutoCloseDelayMs = it.toLong()))
                    }
                )
            }
        }
    }

    item {
        NavigationSettingItem(
            title = "WebXR Settings",
            subtitle = "Configure AR/VR preferences",
            onClick = onNavigateToXRSettings
        )
    }

    item {
        NavigationSettingItem(
            title = "AR Layout Preview",
            subtitle = "Test spatial arc layout and glassmorphic design",
            onClick = onNavigateToARPreview
        )
    }

    // Downloads section
    item {
        SettingsSectionHeader("Downloads")
    }

    item {
        DownloadPathSettingItem(
            currentPath = settings.downloadPath,
            onPathChanged = {
                viewModel.updateSettings(settings.copy(downloadPath = it))
            }
        )
    }

    item {
        SwitchSettingItem(
            title = "Ask Download Location",
            subtitle = "Prompt for location before downloading",
            checked = settings.askDownloadLocation,
            onCheckedChange = {
                viewModel.updateSettings(settings.copy(askDownloadLocation = it))
            }
        )
    }

    item {
        SwitchSettingItem(
            title = "Download Over Wi-Fi Only",
            subtitle = "Prevent downloads on cellular data",
            checked = settings.downloadOverWiFiOnly,
            onCheckedChange = {
                viewModel.updateSettings(settings.copy(downloadOverWiFiOnly = it))
            }
        )
    }

    // Performance section
    item {
        SettingsSectionHeader("Performance")
    }

    item {
        SwitchSettingItem(
            title = "Hardware Acceleration",
            subtitle = "Use GPU for faster rendering",
            checked = settings.hardwareAcceleration,
            onCheckedChange = {
                viewModel.updateSettings(settings.copy(hardwareAcceleration = it))
            }
        )
    }

    item {
        SwitchSettingItem(
            title = "Preload Pages",
            subtitle = "Load pages in background for faster access",
            checked = settings.preloadPages,
            onCheckedChange = {
                viewModel.updateSettings(settings.copy(preloadPages = it))
            }
        )
    }

    item {
        SwitchSettingItem(
            title = "Data Saver",
            subtitle = "Reduce data usage by compressing pages",
            checked = settings.dataSaver,
            onCheckedChange = {
                viewModel.updateSettings(settings.copy(dataSaver = it))
            }
        )
    }

    item {
        SwitchSettingItem(
            title = "Text Reflow",
            subtitle = "Automatically reformat text when zooming",
            checked = settings.textReflow,
            onCheckedChange = {
                viewModel.updateSettings(settings.copy(textReflow = it))
            }
        )
    }

    // Sync section
    item {
        SettingsSectionHeader("Sync")
    }

    item {
        SwitchSettingItem(
            title = "Sync Enabled",
            subtitle = "Sync data across devices",
            checked = settings.syncEnabled,
            onCheckedChange = {
                viewModel.updateSettings(settings.copy(syncEnabled = it))
            }
        )
    }

    if (settings.syncEnabled) {
        item {
            SwitchSettingItem(
                title = "Sync Bookmarks",
                subtitle = "Sync bookmarks across devices",
                checked = settings.syncBookmarks,
                onCheckedChange = {
                    viewModel.updateSettings(settings.copy(syncBookmarks = it))
                }
            )
        }

        item {
            SwitchSettingItem(
                title = "Sync History",
                subtitle = "Sync browsing history across devices",
                checked = settings.syncHistory,
                onCheckedChange = {
                    viewModel.updateSettings(settings.copy(syncHistory = it))
                }
            )
        }

        item {
            SwitchSettingItem(
                title = "Sync Passwords",
                subtitle = "Sync saved passwords across devices",
                checked = settings.syncPasswords,
                onCheckedChange = {
                    viewModel.updateSettings(settings.copy(syncPasswords = it))
                }
            )
        }

        item {
            SwitchSettingItem(
                title = "Sync Settings",
                subtitle = "Sync browser settings across devices",
                checked = settings.syncSettings,
                onCheckedChange = {
                    viewModel.updateSettings(settings.copy(syncSettings = it))
                }
            )
        }
    }

    // Voice & AI section
    item {
        SettingsSectionHeader("Voice & AI")
    }

    item {
        SwitchSettingItem(
            title = "AI Summaries",
            subtitle = "Generate AI-powered page summaries",
            checked = settings.aiSummaries,
            onCheckedChange = {
                viewModel.updateSettings(settings.copy(aiSummaries = it))
            }
        )
    }

    item {
        SwitchSettingItem(
            title = "AI Translation",
            subtitle = "Translate pages with AI",
            checked = settings.aiTranslation,
            onCheckedChange = {
                viewModel.updateSettings(settings.copy(aiTranslation = it))
            }
        )
    }

    item {
        SwitchSettingItem(
            title = "Read Aloud",
            subtitle = "Text-to-speech for web content",
            checked = settings.readAloud,
            onCheckedChange = {
                viewModel.updateSettings(settings.copy(readAloud = it))
            }
        )
    }

    // Command Bar section
    item {
        SettingsSectionHeader("Command Bar")
    }

    item {
        SwitchSettingItem(
            title = "Auto-hide Command Bar",
            subtitle = "Automatically hide command bar after timeout",
            checked = settings.commandBarAutoHide,
            onCheckedChange = {
                viewModel.updateSettings(settings.copy(commandBarAutoHide = it))
            }
        )
    }

    if (settings.commandBarAutoHide) {
        item {
            SliderSettingItem(
                title = "Auto-hide Delay",
                subtitle = "Delay: ${settings.commandBarAutoHideDelayMs}ms",
                value = settings.commandBarAutoHideDelayMs.toFloat(),
                valueRange = 3000f..30000f,
                steps = 26,
                onValueChange = {
                    viewModel.updateSettings(settings.copy(commandBarAutoHideDelayMs = it.toLong()))
                }
            )
        }
    }
}

/**
 * Adds Advanced settings items to a LazyColumn for landscape mode with AR/XR card wrappers
 */
fun LazyListScope.advancedSettingsLandscapeItems(
    settings: BrowserSettings,
    viewModel: SettingsViewModel,
    onNavigateToARPreview: () -> Unit
) {
    item {
        ARXRSettingCard {
            SwitchSettingItem(
                title = "Desktop Mode",
                subtitle = "Request desktop version of websites",
                checked = settings.useDesktopMode,
                onCheckedChange = { viewModel.setDesktopMode(it) }
            )
        }
    }
    if (settings.useDesktopMode) {
        item {
            ARXRSettingCard {
                SliderSettingItem(
                    title = "Default Zoom Level",
                    subtitle = "Zoom: ${settings.desktopModeDefaultZoom}%",
                    value = settings.desktopModeDefaultZoom.toFloat(),
                    valueRange = 50f..200f,
                    steps = 29,
                    onValueChange = { viewModel.setDesktopModeDefaultZoom(it.toInt()) }
                )
            }
        }
        item {
            ARXRSettingCard {
                SwitchSettingItem(
                    title = "Auto-fit Zoom",
                    subtitle = "Automatically adjust zoom to fit content",
                    checked = settings.desktopModeAutoFitZoom,
                    onCheckedChange = { viewModel.setDesktopModeAutoFitZoom(it) }
                )
            }
        }
    }
    item {
        ARXRSettingCard {
            AutoPlaySettingItem(
                currentAutoPlay = settings.autoPlay,
                onAutoPlaySelected = { viewModel.setAutoPlay(it) }
            )
        }
    }
    item {
        ARXRSettingCard {
            SwitchSettingItem(
                title = "Voice Commands",
                subtitle = "Control browser with voice",
                checked = settings.enableVoiceCommands,
                onCheckedChange = { viewModel.setEnableVoiceCommands(it) }
            )
        }
    }
    // Voice Dialog Auto-Close settings
    if (settings.enableVoiceCommands) {
        item {
            ARXRSettingCard {
                SwitchSettingItem(
                    title = "Auto-close Voice Dialog",
                    subtitle = "Automatically close after command execution",
                    checked = settings.voiceDialogAutoClose,
                    onCheckedChange = {
                        viewModel.updateSettings(settings.copy(voiceDialogAutoClose = it))
                    }
                )
            }
        }
        if (settings.voiceDialogAutoClose) {
            item {
                ARXRSettingCard {
                    SliderSettingItem(
                        title = "Auto-close Delay",
                        subtitle = "Delay: ${settings.voiceDialogAutoCloseDelayMs}ms",
                        value = settings.voiceDialogAutoCloseDelayMs.toFloat(),
                        valueRange = 500f..5000f,
                        steps = 9,
                        onValueChange = {
                            viewModel.updateSettings(settings.copy(voiceDialogAutoCloseDelayMs = it.toLong()))
                        }
                    )
                }
            }
        }
    }
    // Downloads
    item {
        ARXRSettingCard {
            DownloadPathSettingItem(
                currentPath = settings.downloadPath,
                onPathChanged = {
                    viewModel.updateSettings(settings.copy(downloadPath = it))
                }
            )
        }
    }
    item {
        ARXRSettingCard {
            SwitchSettingItem(
                title = "Ask Download Location",
                subtitle = "Prompt for location before downloading",
                checked = settings.askDownloadLocation,
                onCheckedChange = {
                    viewModel.updateSettings(settings.copy(askDownloadLocation = it))
                }
            )
        }
    }
    item {
        ARXRSettingCard {
            SwitchSettingItem(
                title = "Download Over Wi-Fi Only",
                subtitle = "Prevent downloads on cellular data",
                checked = settings.downloadOverWiFiOnly,
                onCheckedChange = {
                    viewModel.updateSettings(settings.copy(downloadOverWiFiOnly = it))
                }
            )
        }
    }
    // Performance
    item {
        ARXRSettingCard {
            SwitchSettingItem(
                title = "Hardware Acceleration",
                subtitle = "Use GPU for faster rendering",
                checked = settings.hardwareAcceleration,
                onCheckedChange = {
                    viewModel.updateSettings(settings.copy(hardwareAcceleration = it))
                }
            )
        }
    }
    item {
        ARXRSettingCard {
            SwitchSettingItem(
                title = "Preload Pages",
                subtitle = "Load pages in background for faster access",
                checked = settings.preloadPages,
                onCheckedChange = {
                    viewModel.updateSettings(settings.copy(preloadPages = it))
                }
            )
        }
    }
    item {
        ARXRSettingCard {
            SwitchSettingItem(
                title = "Data Saver",
                subtitle = "Reduce data usage by compressing pages",
                checked = settings.dataSaver,
                onCheckedChange = {
                    viewModel.updateSettings(settings.copy(dataSaver = it))
                }
            )
        }
    }
    item {
        ARXRSettingCard {
            SwitchSettingItem(
                title = "Text Reflow",
                subtitle = "Automatically reformat text when zooming",
                checked = settings.textReflow,
                onCheckedChange = {
                    viewModel.updateSettings(settings.copy(textReflow = it))
                }
            )
        }
    }
    // Sync
    item {
        ARXRSettingCard {
            SwitchSettingItem(
                title = "Sync Enabled",
                subtitle = "Sync data across devices",
                checked = settings.syncEnabled,
                onCheckedChange = {
                    viewModel.updateSettings(settings.copy(syncEnabled = it))
                }
            )
        }
    }
    if (settings.syncEnabled) {
        item {
            ARXRSettingCard {
                SwitchSettingItem(
                    title = "Sync Bookmarks",
                    subtitle = "Sync bookmarks across devices",
                    checked = settings.syncBookmarks,
                    onCheckedChange = {
                        viewModel.updateSettings(settings.copy(syncBookmarks = it))
                    }
                )
            }
        }
        item {
            ARXRSettingCard {
                SwitchSettingItem(
                    title = "Sync History",
                    subtitle = "Sync browsing history across devices",
                    checked = settings.syncHistory,
                    onCheckedChange = {
                        viewModel.updateSettings(settings.copy(syncHistory = it))
                    }
                )
            }
        }
        item {
            ARXRSettingCard {
                SwitchSettingItem(
                    title = "Sync Passwords",
                    subtitle = "Sync saved passwords across devices",
                    checked = settings.syncPasswords,
                    onCheckedChange = {
                        viewModel.updateSettings(settings.copy(syncPasswords = it))
                    }
                )
            }
        }
        item {
            ARXRSettingCard {
                SwitchSettingItem(
                    title = "Sync Settings",
                    subtitle = "Sync browser settings across devices",
                    checked = settings.syncSettings,
                    onCheckedChange = {
                        viewModel.updateSettings(settings.copy(syncSettings = it))
                    }
                )
            }
        }
    }
    // Voice & AI
    item {
        ARXRSettingCard {
            SwitchSettingItem(
                title = "AI Summaries",
                subtitle = "Generate AI-powered page summaries",
                checked = settings.aiSummaries,
                onCheckedChange = {
                    viewModel.updateSettings(settings.copy(aiSummaries = it))
                }
            )
        }
    }
    item {
        ARXRSettingCard {
            SwitchSettingItem(
                title = "AI Translation",
                subtitle = "Translate pages with AI",
                checked = settings.aiTranslation,
                onCheckedChange = {
                    viewModel.updateSettings(settings.copy(aiTranslation = it))
                }
            )
        }
    }
    item {
        ARXRSettingCard {
            SwitchSettingItem(
                title = "Read Aloud",
                subtitle = "Text-to-speech for web content",
                checked = settings.readAloud,
                onCheckedChange = {
                    viewModel.updateSettings(settings.copy(readAloud = it))
                }
            )
        }
    }
    // Command Bar
    item {
        ARXRSettingCard {
            SwitchSettingItem(
                title = "Auto-hide Command Bar",
                subtitle = "Automatically hide command bar after timeout",
                checked = settings.commandBarAutoHide,
                onCheckedChange = {
                    viewModel.updateSettings(settings.copy(commandBarAutoHide = it))
                }
            )
        }
    }
    if (settings.commandBarAutoHide) {
        item {
            ARXRSettingCard {
                SliderSettingItem(
                    title = "Auto-hide Delay",
                    subtitle = "Delay: ${settings.commandBarAutoHideDelayMs}ms",
                    value = settings.commandBarAutoHideDelayMs.toFloat(),
                    valueRange = 3000f..30000f,
                    steps = 26,
                    onValueChange = {
                        viewModel.updateSettings(settings.copy(commandBarAutoHideDelayMs = it.toLong()))
                    }
                )
            }
        }
    }
    item {
        ARXRSettingCard {
            NavigationSettingItem(
                title = "AR Layout Preview",
                subtitle = "Test spatial arc layout and glassmorphic design",
                onClick = onNavigateToARPreview
            )
        }
    }
}
