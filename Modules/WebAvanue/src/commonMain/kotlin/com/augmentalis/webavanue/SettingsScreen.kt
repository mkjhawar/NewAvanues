package com.augmentalis.webavanue

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.DesktopWindows
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.augmentalis.avanueui.components.navigation.GroupedListDetailScaffold
import com.augmentalis.avanueui.components.navigation.GroupedListRow
import com.augmentalis.avanueui.components.navigation.GroupedListSection

/**
 * Browser settings categories for GroupedListDetail navigation.
 */
private enum class BrowserSettingsCategory(
    val displayTitle: String,
    val icon: ImageVector
) {
    GENERAL("General", Icons.Default.Settings),
    APPEARANCE("Appearance", Icons.Default.Palette),
    PRIVACY("Privacy & Security", Icons.Default.Lock),
    DESKTOP_MODE("Advanced Mode", Icons.Default.DesktopWindows),
    DOWNLOADS("Downloads", Icons.Default.Download),
    VOICE("Voice & Commands", Icons.Default.Mic),
    PERFORMANCE("Performance", Icons.Default.Speed),
    SYNC("Sync", Icons.Default.Sync),
    AI("AI Features", Icons.Default.AutoAwesome),
    WEBXR("WebXR", Icons.Default.ViewInAr),
    ADVANCED("Advanced", Icons.Default.Tune)
}

/**
 * Summary text for each category based on current settings.
 */
private fun categorySummary(
    category: BrowserSettingsCategory,
    settings: BrowserSettings
): String = when (category) {
    BrowserSettingsCategory.GENERAL -> {
        val engine = settings.defaultSearchEngine.name.lowercase()
            .replaceFirstChar { it.uppercase() }
        "$engine · ${settings.newTabPage.name.lowercase().replace('_', ' ')}"
    }
    BrowserSettingsCategory.APPEARANCE -> {
        "${settings.theme.name.lowercase().replaceFirstChar { it.uppercase() }} · ${settings.fontSize.name.lowercase().replaceFirstChar { it.uppercase() }}"
    }
    BrowserSettingsCategory.PRIVACY -> {
        val protections = listOfNotNull(
            if (settings.blockAds) "ads" else null,
            if (settings.blockTrackers) "trackers" else null,
            if (settings.blockPopups) "popups" else null,
            if (settings.doNotTrack) "DNT" else null
        )
        if (protections.isEmpty()) "No protections" else "Blocking: ${protections.joinToString(", ")}"
    }
    BrowserSettingsCategory.DESKTOP_MODE -> {
        if (settings.useDesktopMode) "On · ${settings.desktopModeDefaultZoom}% zoom" else "Off"
    }
    BrowserSettingsCategory.DOWNLOADS -> {
        val path = settings.downloadPath?.substringAfterLast('/') ?: "Default"
        if (settings.downloadOverWiFiOnly) "$path · Wi-Fi only" else path
    }
    BrowserSettingsCategory.VOICE -> {
        val voice = if (settings.enableVoiceCommands) "On" else "Off"
        val layout = settings.commandBarOrientation.name.lowercase().replaceFirstChar { it.uppercase() }
        "$voice · Bar: $layout"
    }
    BrowserSettingsCategory.PERFORMANCE -> {
        val features = listOfNotNull(
            if (settings.hardwareAcceleration) "GPU" else null,
            if (settings.preloadPages) "preload" else null,
            if (settings.dataSaver) "saver" else null
        )
        features.joinToString(", ").ifEmpty { "Default" }
    }
    BrowserSettingsCategory.SYNC -> {
        if (settings.syncEnabled) "On" else "Off"
    }
    BrowserSettingsCategory.AI -> {
        val features = listOfNotNull(
            if (settings.aiSummaries) "summaries" else null,
            if (settings.aiTranslation) "translate" else null,
            if (settings.readAloud) "read aloud" else null
        )
        features.joinToString(", ").ifEmpty { "All off" }
    }
    BrowserSettingsCategory.WEBXR -> {
        if (settings.enableWebXR) {
            val modes = listOfNotNull(
                if (settings.enableAR) "AR" else null,
                if (settings.enableVR) "VR" else null
            )
            "On · ${modes.joinToString("+")}"
        } else "Off"
    }
    BrowserSettingsCategory.ADVANCED -> {
        val features = listOfNotNull(
            if (settings.enableDatabaseEncryption) "encrypted DB" else null,
            if (settings.enableSecureStorage) "secure storage" else null
        )
        features.joinToString(", ").ifEmpty { "Standard"  }
    }
}

/**
 * Main Settings Screen — GroupedListDetail pattern.
 *
 * Shows category rows (General, Appearance, Privacy, etc.) in a grouped list.
 * Tapping a category navigates to its detail view with editable settings.
 */
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val settings by viewModel.settings.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isSaving by viewModel.isSaving.collectAsState()
    val error by viewModel.error.collectAsState()

    val currentSettings = settings
    val sections = if (currentSettings != null) {
        buildCategorySections(currentSettings)
    } else {
        emptyList()
    }

    GroupedListDetailScaffold(
        title = "Settings",
        sections = sections,
        itemKey = { it.name },
        onNavigateBack = onNavigateBack,
        listRow = { category, onClick ->
            GroupedListRow(
                title = category.displayTitle,
                subtitle = if (currentSettings != null) categorySummary(category, currentSettings) else null,
                icon = category.icon,
                onClick = onClick
            )
        },
        detailTitle = { it.displayTitle },
        detailContent = { category, paddingValues ->
            if (currentSettings != null) {
                CategoryDetailContent(
                    category = category,
                    settings = currentSettings,
                    onUpdateSettings = { viewModel.updateSettings(it) },
                    modifier = Modifier.padding(paddingValues)
                )
            }
        },
        modifier = modifier,
        loading = currentSettings == null && isLoading,
        savingIndicator = isSaving,
        error = if (error != null && currentSettings == null) error else null
    )
}

/**
 * Build the section groupings for the category list.
 */
private fun buildCategorySections(
    settings: BrowserSettings
): List<GroupedListSection<BrowserSettingsCategory>> = listOf(
    GroupedListSection(
        title = "Browsing",
        items = listOf(
            BrowserSettingsCategory.GENERAL,
            BrowserSettingsCategory.APPEARANCE,
            BrowserSettingsCategory.DESKTOP_MODE
        )
    ),
    GroupedListSection(
        title = "Privacy & Content",
        items = listOf(
            BrowserSettingsCategory.PRIVACY,
            BrowserSettingsCategory.DOWNLOADS,
            BrowserSettingsCategory.VOICE
        )
    ),
    GroupedListSection(
        title = "Features",
        items = buildList {
            add(BrowserSettingsCategory.AI)
            add(BrowserSettingsCategory.WEBXR)
            add(BrowserSettingsCategory.SYNC)
        }
    ),
    GroupedListSection(
        title = "System",
        items = listOf(
            BrowserSettingsCategory.PERFORMANCE,
            BrowserSettingsCategory.ADVANCED
        )
    )
)

// =============================================================================
// Category Detail Content — each section's editable settings
// =============================================================================

@Composable
private fun CategoryDetailContent(
    category: BrowserSettingsCategory,
    settings: BrowserSettings,
    onUpdateSettings: (BrowserSettings) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 8.dp)
    ) {
        when (category) {
            BrowserSettingsCategory.GENERAL -> {
                item {
                    HomepageSettingItem(
                        currentHomepage = settings.homePage,
                        onHomepageChanged = { onUpdateSettings(settings.copy(homePage = it)) }
                    )
                }
                item {
                    SearchEngineSettingItem(
                        currentEngine = settings.defaultSearchEngine,
                        customEngineName = settings.customSearchEngineName,
                        customEngineUrl = settings.customSearchEngineUrl,
                        onEngineSelected = { onUpdateSettings(settings.copy(defaultSearchEngine = it)) },
                        onCustomNameChanged = { onUpdateSettings(settings.copy(customSearchEngineName = it)) },
                        onCustomUrlChanged = { onUpdateSettings(settings.copy(customSearchEngineUrl = it)) }
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
            }

            BrowserSettingsCategory.APPEARANCE -> {
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
            }

            BrowserSettingsCategory.PRIVACY -> {
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
            }

            BrowserSettingsCategory.DESKTOP_MODE -> {
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
            }

            BrowserSettingsCategory.DOWNLOADS -> {
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
            }

            BrowserSettingsCategory.VOICE -> {
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
                item {
                    DropdownSettingItem(
                        title = "Command Bar Layout",
                        subtitle = when (settings.commandBarOrientation) {
                            BrowserSettings.CommandBarOrientation.AUTO -> "Auto (follows orientation)"
                            BrowserSettings.CommandBarOrientation.HORIZONTAL -> "Always horizontal"
                            BrowserSettings.CommandBarOrientation.VERTICAL -> "Always vertical"
                        },
                        options = BrowserSettings.CommandBarOrientation.entries.map { it.name.lowercase().replaceFirstChar { c -> c.uppercase() } },
                        selectedIndex = settings.commandBarOrientation.ordinal,
                        onOptionSelected = { index ->
                            onUpdateSettings(settings.copy(
                                commandBarOrientation = BrowserSettings.CommandBarOrientation.entries[index]
                            ))
                        }
                    )
                }
            }

            BrowserSettingsCategory.PERFORMANCE -> {
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
            }

            BrowserSettingsCategory.SYNC -> {
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
            }

            BrowserSettingsCategory.AI -> {
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
            }

            BrowserSettingsCategory.WEBXR -> {
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
            }

            BrowserSettingsCategory.ADVANCED -> {
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
            }
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
