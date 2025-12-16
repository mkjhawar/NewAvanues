package com.augmentalis.webavanue.ui.screen.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.augmentalis.webavanue.ui.viewmodel.SettingsViewModel
import com.augmentalis.webavanue.domain.model.BrowserSettings
import com.augmentalis.webavanue.ui.screen.settings.components.*

/**
 * Main Settings Screen
 * Clean, simple implementation with all browser settings
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val settings by viewModel.settings.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                }
            )
        },
        modifier = modifier
    ) { paddingValues ->
        when {
            isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.align(androidx.compose.ui.Alignment.Center)
                    )
                }
            }
            error != null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                ) {
                    Text(
                        text = "Error: $error",
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.align(androidx.compose.ui.Alignment.Center)
                    )
                }
            }
            settings != null -> {
                SettingsContent(
                    settings = settings!!,
                    onUpdateSettings = { viewModel.updateSettings(it) },
                    modifier = Modifier.padding(paddingValues)
                )
            }
        }
    }
}

@Composable
private fun SettingsContent(
    settings: BrowserSettings,
    onUpdateSettings: (BrowserSettings) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 8.dp)
    ) {
        // General Section
        item {
            SectionHeader("General")
        }
        item {
            HomepageSettingItem(
                currentHomepage = settings.homePage,
                onHomepageChanged = { onUpdateSettings(settings.copy(homePage = it)) }
            )
        }
        item {
            SearchEngineSettingItem(
                currentEngine = settings.defaultSearchEngine,
                onEngineSelected = { onUpdateSettings(settings.copy(defaultSearchEngine = it)) }
            )
        }
        item {
            NewTabPageSettingItem(
                currentNewTabPage = settings.newTabPage,
                onNewTabPageSelected = { onUpdateSettings(settings.copy(newTabPage = it)) }
            )
        }
        item {
            SwitchSettingItem(
                title = "Restore Tabs on Startup",
                subtitle = "Continue where you left off",
                checked = settings.restoreTabsOnStartup,
                onCheckedChange = { onUpdateSettings(settings.copy(restoreTabsOnStartup = it)) }
            )
        }
        item {
            SwitchSettingItem(
                title = "Open Links in New Tab",
                subtitle = "Open all links in new tabs",
                checked = settings.openLinksInNewTab,
                onCheckedChange = { onUpdateSettings(settings.copy(openLinksInNewTab = it)) }
            )
        }
        item {
            SwitchSettingItem(
                title = "Open Links in Background",
                subtitle = "Don't switch to new tabs immediately",
                checked = settings.openLinksInBackground,
                onCheckedChange = { onUpdateSettings(settings.copy(openLinksInBackground = it)) }
            )
        }

        // Appearance Section
        item {
            SectionHeader("Appearance")
        }
        item {
            ThemeSettingItem(
                currentTheme = settings.theme,
                onThemeSelected = { onUpdateSettings(settings.copy(theme = it)) }
            )
        }
        item {
            FontSizeSettingItem(
                currentFontSize = settings.fontSize,
                onFontSizeSelected = { onUpdateSettings(settings.copy(fontSize = it)) }
            )
        }
        item {
            SwitchSettingItem(
                title = "Show Images",
                subtitle = "Display images on web pages",
                checked = settings.showImages,
                onCheckedChange = { onUpdateSettings(settings.copy(showImages = it)) }
            )
        }
        item {
            SwitchSettingItem(
                title = "Force Zoom",
                subtitle = "Enable zoom on all pages",
                checked = settings.forceZoom,
                onCheckedChange = { onUpdateSettings(settings.copy(forceZoom = it)) }
            )
        }
        item {
            SliderSettingItem(
                title = "Mobile Portrait Scale",
                subtitle = "Scale in portrait mode: ${(settings.mobilePortraitScale * 100).toInt()}%",
                value = settings.mobilePortraitScale,
                valueRange = 0.5f..2.0f,
                steps = 14,
                onValueChange = { onUpdateSettings(settings.copy(mobilePortraitScale = it)) }
            )
        }
        item {
            SliderSettingItem(
                title = "Mobile Landscape Scale",
                subtitle = "Scale in landscape mode: ${(settings.mobileLandscapeScale * 100).toInt()}%",
                value = settings.mobileLandscapeScale,
                valueRange = 0.5f..2.0f,
                steps = 14,
                onValueChange = { onUpdateSettings(settings.copy(mobileLandscapeScale = it)) }
            )
        }

        // Privacy & Security Section
        item {
            SectionHeader("Privacy & Security")
        }
        item {
            SwitchSettingItem(
                title = "Enable JavaScript",
                subtitle = "Required for most modern websites",
                checked = settings.enableJavaScript,
                onCheckedChange = { onUpdateSettings(settings.copy(enableJavaScript = it)) }
            )
        }
        item {
            SwitchSettingItem(
                title = "Enable Cookies",
                subtitle = "Allow sites to save cookies",
                checked = settings.enableCookies,
                onCheckedChange = { onUpdateSettings(settings.copy(enableCookies = it)) }
            )
        }
        item {
            SwitchSettingItem(
                title = "Block Pop-ups",
                subtitle = "Prevent pop-up windows",
                checked = settings.blockPopups,
                onCheckedChange = { onUpdateSettings(settings.copy(blockPopups = it)) }
            )
        }
        item {
            SwitchSettingItem(
                title = "Block Ads",
                subtitle = "Block advertisements",
                checked = settings.blockAds,
                onCheckedChange = { onUpdateSettings(settings.copy(blockAds = it)) }
            )
        }
        item {
            SwitchSettingItem(
                title = "Block Trackers",
                subtitle = "Prevent tracking cookies",
                checked = settings.blockTrackers,
                onCheckedChange = { onUpdateSettings(settings.copy(blockTrackers = it)) }
            )
        }
        item {
            SwitchSettingItem(
                title = "Do Not Track",
                subtitle = "Send DNT header",
                checked = settings.doNotTrack,
                onCheckedChange = { onUpdateSettings(settings.copy(doNotTrack = it)) }
            )
        }
        item {
            SwitchSettingItem(
                title = "Enable WebRTC",
                subtitle = "Allow real-time communication",
                checked = settings.enableWebRTC,
                onCheckedChange = { onUpdateSettings(settings.copy(enableWebRTC = it)) }
            )
        }
        item {
            SwitchSettingItem(
                title = "Clear Cache on Exit",
                subtitle = "Delete cached files when closing",
                checked = settings.clearCacheOnExit,
                onCheckedChange = { onUpdateSettings(settings.copy(clearCacheOnExit = it)) }
            )
        }
        item {
            SwitchSettingItem(
                title = "Clear History on Exit",
                subtitle = "Delete browsing history when closing",
                checked = settings.clearHistoryOnExit,
                onCheckedChange = { onUpdateSettings(settings.copy(clearHistoryOnExit = it)) }
            )
        }
        item {
            SwitchSettingItem(
                title = "Clear Cookies on Exit",
                subtitle = "Delete all cookies when closing",
                checked = settings.clearCookiesOnExit,
                onCheckedChange = { onUpdateSettings(settings.copy(clearCookiesOnExit = it)) }
            )
        }

        // Desktop Mode Section
        item {
            SectionHeader("Desktop Mode")
        }
        item {
            SwitchSettingItem(
                title = "Use Desktop Mode",
                subtitle = "Request desktop version of websites",
                checked = settings.useDesktopMode,
                onCheckedChange = { onUpdateSettings(settings.copy(useDesktopMode = it)) }
            )
        }
        if (settings.useDesktopMode) {
            item {
                SliderSettingItem(
                    title = "Default Zoom Level",
                    subtitle = "Zoom: ${settings.desktopModeDefaultZoom}%",
                    value = settings.desktopModeDefaultZoom.toFloat(),
                    valueRange = 50f..200f,
                    steps = 29,
                    onValueChange = { onUpdateSettings(settings.copy(desktopModeDefaultZoom = it.toInt())) }
                )
            }
            item {
                SwitchSettingItem(
                    title = "Auto-fit Zoom",
                    subtitle = "Automatically adjust zoom to fit content",
                    checked = settings.desktopModeAutoFitZoom,
                    onCheckedChange = { onUpdateSettings(settings.copy(desktopModeAutoFitZoom = it)) }
                )
            }
        }

        // Downloads Section
        item {
            SectionHeader("Downloads")
        }
        item {
            DownloadPathSettingItem(
                currentPath = settings.downloadPath,
                onPathSelected = {
                    // TODO: Navigate to file picker
                }
            )
        }
        item {
            SwitchSettingItem(
                title = "Ask Download Location",
                subtitle = "Prompt for location before downloading",
                checked = settings.askDownloadLocation,
                onCheckedChange = { onUpdateSettings(settings.copy(askDownloadLocation = it)) }
            )
        }
        item {
            SwitchSettingItem(
                title = "Download Over Wi-Fi Only",
                subtitle = "Prevent downloads on cellular data",
                checked = settings.downloadOverWiFiOnly,
                onCheckedChange = { onUpdateSettings(settings.copy(downloadOverWiFiOnly = it)) }
            )
        }

        // Voice & Commands Section
        item {
            SectionHeader("Voice & Commands")
        }
        item {
            SwitchSettingItem(
                title = "Voice Commands",
                subtitle = "Control browser with voice",
                checked = settings.enableVoiceCommands,
                onCheckedChange = { onUpdateSettings(settings.copy(enableVoiceCommands = it)) }
            )
        }
        if (settings.enableVoiceCommands) {
            item {
                SwitchSettingItem(
                    title = "Auto-close Voice Dialog",
                    subtitle = "Close dialog after command execution",
                    checked = settings.voiceDialogAutoClose,
                    onCheckedChange = { onUpdateSettings(settings.copy(voiceDialogAutoClose = it)) }
                )
            }
            if (settings.voiceDialogAutoClose) {
                item {
                    SliderSettingItem(
                        title = "Auto-close Delay",
                        subtitle = "Delay: ${settings.voiceDialogAutoCloseDelayMs}ms",
                        value = settings.voiceDialogAutoCloseDelayMs.toFloat(),
                        valueRange = 500f..5000f,
                        steps = 8,
                        onValueChange = { onUpdateSettings(settings.copy(voiceDialogAutoCloseDelayMs = it.toLong())) }
                    )
                }
            }
        }
        item {
            SwitchSettingItem(
                title = "Command Bar Auto-hide",
                subtitle = "Hide command bar after timeout",
                checked = settings.commandBarAutoHide,
                onCheckedChange = { onUpdateSettings(settings.copy(commandBarAutoHide = it)) }
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
                    onValueChange = { onUpdateSettings(settings.copy(commandBarAutoHideDelayMs = it.toLong())) }
                )
            }
        }

        // Performance Section
        item {
            SectionHeader("Performance")
        }
        item {
            SwitchSettingItem(
                title = "Hardware Acceleration",
                subtitle = "Use GPU for faster rendering",
                checked = settings.hardwareAcceleration,
                onCheckedChange = { onUpdateSettings(settings.copy(hardwareAcceleration = it)) }
            )
        }
        item {
            SwitchSettingItem(
                title = "Preload Pages",
                subtitle = "Load pages in background",
                checked = settings.preloadPages,
                onCheckedChange = { onUpdateSettings(settings.copy(preloadPages = it)) }
            )
        }
        item {
            SwitchSettingItem(
                title = "Data Saver",
                subtitle = "Reduce data usage",
                checked = settings.dataSaver,
                onCheckedChange = { onUpdateSettings(settings.copy(dataSaver = it)) }
            )
        }
        item {
            SwitchSettingItem(
                title = "Text Reflow",
                subtitle = "Reformat text when zooming",
                checked = settings.textReflow,
                onCheckedChange = { onUpdateSettings(settings.copy(textReflow = it)) }
            )
        }

        // Sync Section
        item {
            SectionHeader("Sync")
        }
        item {
            SwitchSettingItem(
                title = "Enable Sync",
                subtitle = "Sync data across devices",
                checked = settings.syncEnabled,
                onCheckedChange = { onUpdateSettings(settings.copy(syncEnabled = it)) }
            )
        }
        if (settings.syncEnabled) {
            item {
                SwitchSettingItem(
                    title = "Sync Bookmarks",
                    subtitle = "Sync your bookmarks",
                    checked = settings.syncBookmarks,
                    onCheckedChange = { onUpdateSettings(settings.copy(syncBookmarks = it)) }
                )
            }
            item {
                SwitchSettingItem(
                    title = "Sync History",
                    subtitle = "Sync browsing history",
                    checked = settings.syncHistory,
                    onCheckedChange = { onUpdateSettings(settings.copy(syncHistory = it)) }
                )
            }
            item {
                SwitchSettingItem(
                    title = "Sync Passwords",
                    subtitle = "Sync saved passwords",
                    checked = settings.syncPasswords,
                    onCheckedChange = { onUpdateSettings(settings.copy(syncPasswords = it)) }
                )
            }
            item {
                SwitchSettingItem(
                    title = "Sync Settings",
                    subtitle = "Sync preferences",
                    checked = settings.syncSettings,
                    onCheckedChange = { onUpdateSettings(settings.copy(syncSettings = it)) }
                )
            }
        }

        // AI Features Section
        item {
            SectionHeader("AI Features")
        }
        item {
            SwitchSettingItem(
                title = "AI Summaries",
                subtitle = "Generate page summaries",
                checked = settings.aiSummaries,
                onCheckedChange = { onUpdateSettings(settings.copy(aiSummaries = it)) }
            )
        }
        item {
            SwitchSettingItem(
                title = "AI Translation",
                subtitle = "Translate pages with AI",
                checked = settings.aiTranslation,
                onCheckedChange = { onUpdateSettings(settings.copy(aiTranslation = it)) }
            )
        }
        item {
            SwitchSettingItem(
                title = "Read Aloud",
                subtitle = "Text-to-speech for content",
                checked = settings.readAloud,
                onCheckedChange = { onUpdateSettings(settings.copy(readAloud = it)) }
            )
        }

        // WebXR Section
        item {
            SectionHeader("WebXR")
        }
        item {
            SwitchSettingItem(
                title = "Enable WebXR",
                subtitle = "Master switch for WebXR",
                checked = settings.enableWebXR,
                onCheckedChange = { onUpdateSettings(settings.copy(enableWebXR = it)) }
            )
        }
        if (settings.enableWebXR) {
            item {
                SwitchSettingItem(
                    title = "Enable AR",
                    subtitle = "Allow augmented reality",
                    checked = settings.enableAR,
                    onCheckedChange = { onUpdateSettings(settings.copy(enableAR = it)) }
                )
            }
            item {
                SwitchSettingItem(
                    title = "Enable VR",
                    subtitle = "Allow virtual reality",
                    checked = settings.enableVR,
                    onCheckedChange = { onUpdateSettings(settings.copy(enableVR = it)) }
                )
            }
            item {
                XRPerformanceModeSettingItem(
                    currentMode = when (settings.xrPerformanceMode) {
                        BrowserSettings.XRPerformanceMode.HIGH_QUALITY -> "Performance"
                        BrowserSettings.XRPerformanceMode.BALANCED -> "Balanced"
                        BrowserSettings.XRPerformanceMode.BATTERY_SAVER -> "Battery Saver"
                    },
                    onModeSelected = { modeString ->
                        val mode = when (modeString) {
                            "Performance" -> BrowserSettings.XRPerformanceMode.HIGH_QUALITY
                            "Balanced" -> BrowserSettings.XRPerformanceMode.BALANCED
                            "Battery Saver" -> BrowserSettings.XRPerformanceMode.BATTERY_SAVER
                            else -> BrowserSettings.XRPerformanceMode.BALANCED
                        }
                        onUpdateSettings(settings.copy(xrPerformanceMode = mode))
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
                    onValueChange = { onUpdateSettings(settings.copy(xrAutoPauseTimeout = it.toInt())) }
                )
            }
            item {
                SwitchSettingItem(
                    title = "Show FPS Indicator",
                    subtitle = "Display frame rate in XR",
                    checked = settings.xrShowFPSIndicator,
                    onCheckedChange = { onUpdateSettings(settings.copy(xrShowFPSIndicator = it)) }
                )
            }
            item {
                SwitchSettingItem(
                    title = "Require Wi-Fi for XR",
                    subtitle = "Only allow XR on Wi-Fi",
                    checked = settings.xrRequireWiFi,
                    onCheckedChange = { onUpdateSettings(settings.copy(xrRequireWiFi = it)) }
                )
            }
        }

        // Advanced Section
        item {
            SectionHeader("Advanced")
        }
        item {
            SwitchSettingItem(
                title = "Database Encryption",
                subtitle = "Encrypt local database",
                checked = settings.enableDatabaseEncryption,
                onCheckedChange = { onUpdateSettings(settings.copy(enableDatabaseEncryption = it)) }
            )
        }
        item {
            SwitchSettingItem(
                title = "Secure Storage",
                subtitle = "Encrypt saved data",
                checked = settings.enableSecureStorage,
                onCheckedChange = { onUpdateSettings(settings.copy(enableSecureStorage = it)) }
            )
        }
        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun SectionHeader(title: String, modifier: Modifier = Modifier) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier.padding(horizontal = 16.dp, vertical = 12.dp)
    )
}
