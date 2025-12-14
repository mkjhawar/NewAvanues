package com.augmentalis.webavanue.ui.screen.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.DeveloperMode
import androidx.compose.material3.*
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.augmentalis.webavanue.domain.model.BrowserSettings
import com.augmentalis.webavanue.ui.screen.theme.OceanTheme
import com.augmentalis.webavanue.ui.screen.theme.ThemeType
import com.augmentalis.webavanue.ui.viewmodel.SettingsViewModel
import com.augmentalis.webavanue.ui.screen.settings.components.CollapsibleSectionHeader

/**
 * Settings category for two-pane AR/XR layout
 */
enum class SettingsCategory(val title: String, val icon: ImageVector) {
    GENERAL("General", Icons.Default.Settings),
    APPEARANCE("Appearance", Icons.Default.Palette),
    PRIVACY("Privacy & Security", Icons.Default.Shield),
    DESKTOP("Desktop Mode", Icons.Default.Computer),
    DOWNLOADS("Downloads", Icons.Default.Download),
    VOICE("Voice & Commands", Icons.Default.Mic),
    PERFORMANCE("Performance", Icons.Default.Speed),
    SYNC("Sync", Icons.Default.Sync),
    AI("AI Features", Icons.Default.AutoAwesome),
    WEBXR("WebXR", Icons.Default.ViewInAr),
    ADVANCED("Advanced", Icons.Default.DeveloperMode)
}

/**
 * Helper function to check if section title matches search query
 */
private fun matchesSearchQuery(sectionTitle: String, query: String): Boolean {
    if (query.isBlank()) return false
    return sectionTitle.contains(query, ignoreCase = true)
}

/**
 * SettingsScreen - Main browser settings screen
 *
 * Features:
 * - Browser settings (JavaScript, cookies, etc.)
 * - Privacy settings (desktop mode, popup blocker)
 * - Default search engine
 * - Homepage
 * - Theme selection (WebAvanue branding vs AvaMagic)
 * - Clear data options
 *
 * @param viewModel SettingsViewModel for state and actions
 * @param onNavigateBack Callback to navigate back
 * @param modifier Modifier for customization
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onNavigateBack: () -> Unit = {},
    onNavigateToXRSettings: () -> Unit = {},
    onNavigateToSitePermissions: () -> Unit = {},
    onNavigateToARPreview: () -> Unit = {},
    onImportBookmarks: () -> Unit = {},
    onExportBookmarks: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val settings by viewModel.settings.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val saveSuccess by viewModel.saveSuccess.collectAsState()

    // Detect landscape orientation for AR/XR optimized layout
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.screenWidthDp > configuration.screenHeightDp

    // AR/XR: Track selected category for two-pane layout
    var selectedCategory by remember { mutableStateOf(SettingsCategory.GENERAL) }

    // Show success snackbar
    LaunchedEffect(saveSuccess) {
        if (saveSuccess) {
            // Success feedback shown
            kotlinx.coroutines.delay(2000)
            viewModel.clearSaveSuccess()
        }
    }

    // AR/XR gradient background
    val arXrBackground = Brush.horizontalGradient(
        colors = listOf(
            Color(0xFF0A1628),
            Color(0xFF1A2744),
            Color(0xFF0A1628)
        )
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(if (isLandscape) arXrBackground else Brush.verticalGradient(
                colors = listOf(OceanTheme.surface, OceanTheme.background)
            ))
    ) {
        if (isLandscape) {
            // AR/XR Landscape: Two-pane layout
            LandscapeSettingsLayout(
                settings = settings,
                isLoading = isLoading,
                error = error,
                selectedCategory = selectedCategory,
                onCategorySelected = { selectedCategory = it },
                onNavigateBack = onNavigateBack,
                onNavigateToXRSettings = onNavigateToXRSettings,
                onNavigateToSitePermissions = onNavigateToSitePermissions,
                onNavigateToARPreview = onNavigateToARPreview,
                onImportBookmarks = onImportBookmarks,
                onExportBookmarks = onExportBookmarks,
                viewModel = viewModel
            )
        } else {
            // Portrait: Single column with AR/XR styling
            PortraitSettingsLayout(
                settings = settings,
                isLoading = isLoading,
                error = error,
                onNavigateBack = onNavigateBack,
                onNavigateToXRSettings = onNavigateToXRSettings,
                onNavigateToSitePermissions = onNavigateToSitePermissions,
                onNavigateToARPreview = onNavigateToARPreview,
                onImportBookmarks = onImportBookmarks,
                onExportBookmarks = onExportBookmarks,
                viewModel = viewModel
            )
        }

        // Save success indicator
        if (saveSuccess) {
            Snackbar(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp),
                containerColor = OceanTheme.primary,
                contentColor = Color.White,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Settings saved")
            }
        }
    }
}

/**
 * Portrait Settings Layout - Single column with AR/XR styling
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PortraitSettingsLayout(
    settings: BrowserSettings?,
    isLoading: Boolean,
    error: String?,
    onNavigateBack: () -> Unit,
    onNavigateToXRSettings: () -> Unit,
    onNavigateToSitePermissions: () -> Unit,
    onNavigateToARPreview: () -> Unit,
    onImportBookmarks: () -> Unit,
    onExportBookmarks: () -> Unit,
    viewModel: SettingsViewModel
) {
    Scaffold(
        topBar = {
            // AR/XR: Glassmorphism top bar
            Surface(
                color = OceanTheme.surface.copy(alpha = 0.92f),
                shadowElevation = 8.dp
            ) {
                TopAppBar(
                    title = {
                        Text(
                            "Settings",
                            color = OceanTheme.textPrimary
                        )
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = onNavigateBack,
                            modifier = Modifier.size(48.dp)  // AR/XR: 48dp touch target
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "Back",
                                tint = OceanTheme.textPrimary
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent
                    )
                )
            }
        },
        containerColor = Color.Transparent
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Collect search and expansion state
            val searchQuery by viewModel.searchQuery.collectAsState()
            val expandedSections by viewModel.expandedSections.collectAsState()

            // Search bar with expand/collapse controls
            SettingsSearchBar(
                searchQuery = searchQuery,
                onSearchQueryChange = { viewModel.setSearchQuery(it) },
                onExpandAll = { viewModel.expandAllSections() },
                onCollapseAll = { viewModel.collapseAllSections() },
                modifier = Modifier.fillMaxWidth()
            )

            when {
                isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                error != null -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = error ?: "Unknown error",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.error
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(onClick = { viewModel.loadSettings() }) {
                            Text("Retry")
                        }
                    }
                }

                settings != null -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(vertical = 8.dp)
                    ) {
                        // ==================== General Section ====================
                        item {
                            val generalExpanded = expandedSections.contains("General")
                            val generalMatchesSearch = matchesSearchQuery("General", searchQuery) ||
                                listOf("home", "search", "tab", "startup", "links", "restore", "background", "voice").any {
                                    it.contains(searchQuery, ignoreCase = true)
                                }

                            CollapsibleSectionHeader(
                                title = "General",
                                isExpanded = generalExpanded,
                                onToggle = { viewModel.toggleSection("General") },
                                matchesSearch = generalMatchesSearch,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        item {
                            val generalExpanded = expandedSections.contains("General")
                            val generalMatchesSearch = matchesSearchQuery("General", searchQuery) ||
                                listOf("home", "search", "tab", "startup", "links", "restore", "background", "voice").any {
                                    it.contains(searchQuery, ignoreCase = true)
                                }

                            AnimatedVisibility(
                                visible = generalExpanded || generalMatchesSearch,
                                enter = expandVertically() + fadeIn(),
                                exit = shrinkVertically() + fadeOut()
                            ) {
                                Column(
                                    verticalArrangement = Arrangement.spacedBy(12.dp),
                                    modifier = Modifier.padding(horizontal = 16.dp)
                                ) {
                                    SearchEngineSettingItem(
                                        currentEngine = settings!!.defaultSearchEngine,
                                        onEngineSelected = { viewModel.setDefaultSearchEngine(it) }
                                    )

                                    HomepageSettingItem(
                                        currentHomepage = settings!!.homePage,
                                        onHomepageChanged = { viewModel.setHomepage(it) }
                                    )

                                    SwitchSettingItem(
                                        title = "Search Suggestions",
                                subtitle = "Show suggestions as you type",
                                checked = settings!!.searchSuggestions,
                                onCheckedChange = {
                                    viewModel.updateSettings(settings!!.copy(searchSuggestions = it))
                                }
                            )
                        }

                        item {
                            SwitchSettingItem(
                                title = "Voice Search",
                                subtitle = "Enable voice search in search bar",
                                checked = settings!!.voiceSearch,
                                onCheckedChange = {
                                    viewModel.updateSettings(settings!!.copy(voiceSearch = it))
                                }
                            )
                        }

                        item {
                            NewTabPageSettingItem(
                                currentNewTabPage = settings!!.newTabPage,
                                onNewTabPageSelected = {
                                    viewModel.updateSettings(settings!!.copy(newTabPage = it))
                                }
                            )
                        }

                        item {
                            SwitchSettingItem(
                                title = "Restore Tabs on Startup",
                                subtitle = "Reopen tabs from previous session",
                                checked = settings!!.restoreTabsOnStartup,
                                onCheckedChange = {
                                    viewModel.updateSettings(settings!!.copy(restoreTabsOnStartup = it))
                                }
                            )
                        }

                        item {
                            SwitchSettingItem(
                                title = "Open Links in Background",
                                subtitle = "Don't switch to new tabs immediately",
                                checked = settings!!.openLinksInBackground,
                                onCheckedChange = {
                                    viewModel.updateSettings(settings!!.copy(openLinksInBackground = it))
                                }
                            )
                        }

                        item {
                            SwitchSettingItem(
                                title = "Open Links in New Tab",
                                subtitle = "Always open links in new tabs",
                                checked = settings!!.openLinksInNewTab,
                                onCheckedChange = {
                                    viewModel.updateSettings(settings!!.copy(openLinksInNewTab = it))
                                }
                            )
                        }

                        // ==================== Appearance Section ====================
                        item {
                            val appearanceExpanded = expandedSections.contains("Appearance")
                            val appearanceMatchesSearch = matchesSearchQuery("Appearance", searchQuery) ||
                                listOf("theme", "font", "image", "zoom", "scale", "dark", "light", "size").any {
                                    it.contains(searchQuery, ignoreCase = true)
                                }

                            CollapsibleSectionHeader(
                                title = "Appearance",
                                isExpanded = appearanceExpanded,
                                onToggle = { viewModel.toggleSection("Appearance") },
                                matchesSearch = appearanceMatchesSearch,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        item {
                            val appearanceExpanded = expandedSections.contains("Appearance")
                            val appearanceMatchesSearch = matchesSearchQuery("Appearance", searchQuery) ||
                                listOf("theme", "font", "image", "zoom", "scale", "dark", "light", "size").any {
                                    it.contains(searchQuery, ignoreCase = true)
                                }

                            AnimatedVisibility(
                                visible = appearanceExpanded || appearanceMatchesSearch,
                                enter = expandVertically() + fadeIn(),
                                exit = shrinkVertically() + fadeOut()
                            ) {
                                Column(
                                    verticalArrangement = Arrangement.spacedBy(12.dp),
                                    modifier = Modifier.padding(horizontal = 16.dp)
                                ) {
                                    ThemeSettingItem(
                                        currentTheme = settings!!.theme,
                                        onThemeSelected = { viewModel.setTheme(it) }
                                    )

                                    FontSizeSettingItem(
                                        currentFontSize = settings!!.fontSize,
                                        onFontSizeSelected = {
                                            viewModel.updateSettings(settings!!.copy(fontSize = it))
                                        }
                                    )

                                    SwitchSettingItem(
                                        title = "Show Images",
                                        subtitle = "Display images on web pages",
                                        checked = settings!!.showImages,
                                        onCheckedChange = {
                                            viewModel.updateSettings(settings!!.copy(showImages = it))
                                        }
                                    )

                                    SwitchSettingItem(
                                        title = "Force Zoom",
                                        subtitle = "Allow zooming on all pages",
                                        checked = settings!!.forceZoom,
                                        onCheckedChange = {
                                            viewModel.updateSettings(settings!!.copy(forceZoom = it))
                                        }
                                    )

                                    SliderSettingItem(
                                        title = "Initial Page Scale",
                                        subtitle = "Scale: ${(settings!!.initialScale * 100).toInt()}%",
                                        value = settings!!.initialScale,
                                        valueRange = 0.5f..2.0f,
                                        steps = 29,
                                        onValueChange = {
                                            viewModel.updateSettings(settings!!.copy(initialScale = it))
                                        }
                                    )
                                }
                            }
                        }

                        // ==================== Privacy & Security Section ====================
                        item {
                            val privacyExpanded = expandedSections.contains("Privacy & Security")
                            val privacyMatchesSearch = matchesSearchQuery("Privacy & Security", searchQuery) ||
                                listOf("javascript", "cookie", "popup", "ad", "tracker", "track", "webrtc", "cache", "history", "security", "privacy", "dnt", "permission").any {
                                    it.contains(searchQuery, ignoreCase = true)
                                }

                            CollapsibleSectionHeader(
                                title = "Privacy & Security",
                                isExpanded = privacyExpanded,
                                onToggle = { viewModel.toggleSection("Privacy & Security") },
                                matchesSearch = privacyMatchesSearch,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        item {
                            val privacyExpanded = expandedSections.contains("Privacy & Security")
                            val privacyMatchesSearch = matchesSearchQuery("Privacy & Security", searchQuery) ||
                                listOf("javascript", "cookie", "popup", "ad", "tracker", "track", "webrtc", "cache", "history", "security", "privacy", "dnt", "permission").any {
                                    it.contains(searchQuery, ignoreCase = true)
                                }

                            AnimatedVisibility(
                                visible = privacyExpanded || privacyMatchesSearch,
                                enter = expandVertically() + fadeIn(),
                                exit = shrinkVertically() + fadeOut()
                            ) {
                                Column(
                                    verticalArrangement = Arrangement.spacedBy(12.dp),
                                    modifier = Modifier.padding(horizontal = 16.dp)
                                ) {
                                    SwitchSettingItem(
                                        title = "Enable JavaScript",
                                        subtitle = "Required for most modern websites",
                                        checked = settings!!.enableJavaScript,
                                        onCheckedChange = { viewModel.setEnableJavaScript(it) }
                                    )

                                    SwitchSettingItem(
                                        title = "Enable Cookies",
                                        subtitle = "Allow websites to store data",
                                        checked = settings!!.enableCookies,
                                        onCheckedChange = { viewModel.setEnableCookies(it) }
                                    )

                                    SwitchSettingItem(
                                        title = "Block Pop-ups",
                                        subtitle = "Prevent pop-up windows",
                                        checked = settings!!.blockPopups,
                                        onCheckedChange = { viewModel.setBlockPopups(it) }
                                    )

                                    SwitchSettingItem(
                                        title = "Block Ads",
                                        subtitle = "Block advertisements on web pages",
                                        checked = settings!!.blockAds,
                                        onCheckedChange = { viewModel.setBlockAds(it) }
                                    )

                                    SwitchSettingItem(
                                        title = "Block Trackers",
                                        subtitle = "Prevent cross-site tracking",
                                        checked = settings!!.blockTrackers,
                                        onCheckedChange = { viewModel.setBlockTrackers(it) }
                                    )

                                    SwitchSettingItem(
                                        title = "Do Not Track",
                                        subtitle = "Send Do Not Track header with requests",
                                        checked = settings!!.doNotTrack,
                                        onCheckedChange = {
                                            viewModel.updateSettings(settings!!.copy(doNotTrack = it))
                                        }
                                    )

                                    SwitchSettingItem(
                                        title = "Enable WebRTC",
                                        subtitle = "Allow real-time communication features",
                                        checked = settings!!.enableWebRTC,
                                        onCheckedChange = {
                                            viewModel.updateSettings(settings!!.copy(enableWebRTC = it))
                                        }
                                    )

                                    SwitchSettingItem(
                                        title = "Clear Cache on Exit",
                                        subtitle = "Automatically clear browser cache",
                                        checked = settings!!.clearCacheOnExit,
                                        onCheckedChange = {
                                            viewModel.updateSettings(settings!!.copy(clearCacheOnExit = it))
                                        }
                                    )

                                    SwitchSettingItem(
                                        title = "Clear History on Exit",
                                        subtitle = "Automatically clear browsing history",
                                        checked = settings!!.clearHistoryOnExit,
                                        onCheckedChange = {
                                            viewModel.updateSettings(settings!!.copy(clearHistoryOnExit = it))
                                        }
                                    )

                                    SwitchSettingItem(
                                        title = "Clear Cookies on Exit",
                                        subtitle = "Automatically clear all cookies",
                                        checked = settings!!.clearCookiesOnExit,
                                        onCheckedChange = {
                                            viewModel.updateSettings(settings!!.copy(clearCookiesOnExit = it))
                                        }
                                    )

                                    NavigationSettingItem(
                                        title = "Site Permissions",
                                        subtitle = "Manage camera, microphone, and location permissions",
                                        onClick = onNavigateToSitePermissions
                                    )
                                }
                            }
                        }

                        // ==================== Desktop Mode Section ====================
                        item {
                            val desktopExpanded = expandedSections.contains("Desktop Mode")
                            val desktopMatchesSearch = matchesSearchQuery("Desktop Mode", searchQuery) ||
                                listOf("desktop", "agent", "viewport", "scale", "window", "emulation", "zoom", "width", "height").any {
                                    it.contains(searchQuery, ignoreCase = true)
                                }

                            CollapsibleSectionHeader(
                                title = "Desktop Mode",
                                isExpanded = desktopExpanded,
                                onToggle = { viewModel.toggleSection("Desktop Mode") },
                                matchesSearch = desktopMatchesSearch,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        item {
                            val desktopExpanded = expandedSections.contains("Desktop Mode")
                            val desktopMatchesSearch = matchesSearchQuery("Desktop Mode", searchQuery) ||
                                listOf("desktop", "agent", "viewport", "scale", "window", "emulation", "zoom", "width", "height").any {
                                    it.contains(searchQuery, ignoreCase = true)
                                }

                            AnimatedVisibility(
                                visible = desktopExpanded || desktopMatchesSearch,
                                enter = expandVertically() + fadeIn(),
                                exit = shrinkVertically() + fadeOut()
                            ) {
                                Column(
                                    verticalArrangement = Arrangement.spacedBy(12.dp),
                                    modifier = Modifier.padding(horizontal = 16.dp)
                                ) {
                                    SwitchSettingItem(
                                        title = "Enable Desktop Mode",
                                        subtitle = "Request desktop version of websites",
                                        checked = settings!!.useDesktopMode,
                                        onCheckedChange = { viewModel.setDesktopMode(it) }
                                    )

                                    // Desktop Mode sub-settings (only visible when desktop mode is enabled)
                                    if (settings!!.useDesktopMode) {
                                        SliderSettingItem(
                                            title = "Default Zoom Level",
                                            subtitle = "Zoom: ${settings!!.desktopModeDefaultZoom}%",
                                            value = settings!!.desktopModeDefaultZoom.toFloat(),
                                            valueRange = 50f..200f,
                                            steps = 29,
                                            onValueChange = { viewModel.setDesktopModeDefaultZoom(it.toInt()) }
                                        )

                                        SliderSettingItem(
                                            title = "Window Width",
                                            subtitle = "Width: ${settings!!.desktopModeWindowWidth}px",
                                            value = settings!!.desktopModeWindowWidth.toFloat(),
                                            valueRange = 800f..1920f,
                                            steps = 22,
                                            onValueChange = { viewModel.setDesktopModeWindowWidth(it.toInt()) }
                                        )

                                        SliderSettingItem(
                                            title = "Window Height",
                                            subtitle = "Height: ${settings!!.desktopModeWindowHeight}px",
                                            value = settings!!.desktopModeWindowHeight.toFloat(),
                                            valueRange = 600f..1200f,
                                            steps = 11,
                                            onValueChange = { viewModel.setDesktopModeWindowHeight(it.toInt()) }
                                        )

                                        SwitchSettingItem(
                                            title = "Auto-fit Zoom",
                                            subtitle = "Automatically adjust zoom to fit content in viewport",
                                            checked = settings!!.desktopModeAutoFitZoom,
                                            onCheckedChange = { viewModel.setDesktopModeAutoFitZoom(it) }
                                        )
                                    }
                                }
                            }
                        }

                        // Note: AutoPlaySettingItem moved to Advanced section (already wrapped with CollapsibleSectionHeader later in file)

                        // Voice & Commands section
                        item {
                            val voiceExpanded = expandedSections.contains("Voice & Commands")
                            val voiceMatchesSearch = matchesSearchQuery("Voice & Commands", searchQuery) ||
                                listOf("voice", "command", "speech", "mic", "microphone", "recognize", "listen", "dialog").any {
                                    it.contains(searchQuery, ignoreCase = true)
                                }

                            CollapsibleSectionHeader(
                                title = "Voice & Commands",
                                isExpanded = voiceExpanded,
                                onToggle = { viewModel.toggleSection("Voice & Commands") },
                                matchesSearch = voiceMatchesSearch
                            )
                        }

                        item {
                            val voiceExpanded = expandedSections.contains("Voice & Commands")
                            val voiceMatchesSearch = matchesSearchQuery("Voice & Commands", searchQuery) ||
                                listOf("voice", "command", "speech", "mic", "microphone", "recognize", "listen", "dialog").any {
                                    it.contains(searchQuery, ignoreCase = true)
                                }

                            AnimatedVisibility(visible = voiceExpanded || voiceMatchesSearch) {
                                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    SwitchSettingItem(
                                        title = "Voice Commands",
                                        subtitle = "Control browser with voice",
                                        checked = settings!!.enableVoiceCommands,
                                        onCheckedChange = { viewModel.setEnableVoiceCommands(it) }
                                    )

                                    // Voice Dialog Auto-Close settings (only visible when voice commands enabled)
                                    if (settings!!.enableVoiceCommands) {
                                        SwitchSettingItem(
                                            title = "Auto-close Voice Dialog",
                                            subtitle = "Automatically close after command execution",
                                            checked = settings!!.voiceDialogAutoClose,
                                            onCheckedChange = {
                                                viewModel.updateSettings(settings!!.copy(voiceDialogAutoClose = it))
                                            }
                                        )

                                        if (settings!!.voiceDialogAutoClose) {
                                            SliderSettingItem(
                                                title = "Auto-close Delay",
                                                subtitle = "Delay: ${settings!!.voiceDialogAutoCloseDelayMs}ms",
                                                value = settings!!.voiceDialogAutoCloseDelayMs.toFloat(),
                                                valueRange = 500f..5000f,
                                                steps = 9,  // 500ms increments
                                                onValueChange = {
                                                    viewModel.updateSettings(settings!!.copy(voiceDialogAutoCloseDelayMs = it.toLong()))
                                                }
                                            )
                                        }
                                    }
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
                            val downloadsExpanded = expandedSections.contains("Downloads")
                            val downloadsMatchesSearch = matchesSearchQuery("Downloads", searchQuery) ||
                                listOf("download", "path", "wifi", "location", "folder", "save", "file").any {
                                    it.contains(searchQuery, ignoreCase = true)
                                }

                            CollapsibleSectionHeader(
                                title = "Downloads",
                                isExpanded = downloadsExpanded,
                                onToggle = { viewModel.toggleSection("Downloads") },
                                matchesSearch = downloadsMatchesSearch
                            )
                        }

                        item {
                            val downloadsExpanded = expandedSections.contains("Downloads")
                            val downloadsMatchesSearch = matchesSearchQuery("Downloads", searchQuery) ||
                                listOf("download", "path", "wifi", "location", "folder", "save", "file").any {
                                    it.contains(searchQuery, ignoreCase = true)
                                }

                            AnimatedVisibility(visible = downloadsExpanded || downloadsMatchesSearch) {
                                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    val pathValidation by viewModel.pathValidation.collectAsState()
                                    DownloadPathSettingItem(
                                        currentPath = settings!!.downloadPath,
                                        onPathChanged = {
                                            viewModel.updateSettings(settings!!.copy(downloadPath = it))
                                        },
                                        pathValidation = pathValidation,
                                        onValidatePath = { path ->
                                            viewModel.validateDownloadPath(path)
                                        }
                                    )

                                    SwitchSettingItem(
                                        title = "Ask Download Location",
                                        subtitle = "Prompt for location before downloading",
                                        checked = settings!!.askDownloadLocation,
                                        onCheckedChange = {
                                            viewModel.updateSettings(settings!!.copy(askDownloadLocation = it))
                                        }
                                    )

                                    SwitchSettingItem(
                                        title = "Download Over Wi-Fi Only",
                                        subtitle = "Prevent downloads on cellular data",
                                        checked = settings!!.downloadOverWiFiOnly,
                                        onCheckedChange = {
                                            viewModel.updateSettings(settings!!.copy(downloadOverWiFiOnly = it))
                                        }
                                    )
                                }
                            }
                        }

                        // Performance section
                        item {
                            val performanceExpanded = expandedSections.contains("Performance")
                            val performanceMatchesSearch = matchesSearchQuery("Performance", searchQuery) ||
                                listOf("performance", "speed", "cache", "memory", "gpu", "acceleration", "hardware", "preload", "data", "saver").any {
                                    it.contains(searchQuery, ignoreCase = true)
                                }

                            CollapsibleSectionHeader(
                                title = "Performance",
                                isExpanded = performanceExpanded,
                                onToggle = { viewModel.toggleSection("Performance") },
                                matchesSearch = performanceMatchesSearch
                            )
                        }

                        item {
                            val performanceExpanded = expandedSections.contains("Performance")
                            val performanceMatchesSearch = matchesSearchQuery("Performance", searchQuery) ||
                                listOf("performance", "speed", "cache", "memory", "gpu", "acceleration", "hardware", "preload", "data", "saver").any {
                                    it.contains(searchQuery, ignoreCase = true)
                                }

                            AnimatedVisibility(visible = performanceExpanded || performanceMatchesSearch) {
                                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    SwitchSettingItem(
                                        title = "Hardware Acceleration",
                                        subtitle = "Use GPU for faster rendering",
                                        checked = settings!!.hardwareAcceleration,
                                        onCheckedChange = {
                                            viewModel.updateSettings(settings!!.copy(hardwareAcceleration = it))
                                        }
                                    )

                                    SwitchSettingItem(
                                        title = "Preload Pages",
                                        subtitle = "Load pages in background for faster access",
                                        checked = settings!!.preloadPages,
                                        onCheckedChange = {
                                            viewModel.updateSettings(settings!!.copy(preloadPages = it))
                                        }
                                    )

                                    SwitchSettingItem(
                                        title = "Data Saver",
                                        subtitle = "Reduce data usage by compressing pages",
                                        checked = settings!!.dataSaver,
                                        onCheckedChange = {
                                            viewModel.updateSettings(settings!!.copy(dataSaver = it))
                                        }
                                    )

                                    SwitchSettingItem(
                                        title = "Text Reflow",
                                        subtitle = "Automatically reformat text when zooming",
                                        checked = settings!!.textReflow,
                                        onCheckedChange = {
                                            viewModel.updateSettings(settings!!.copy(textReflow = it))
                                        }
                                    )
                                }
                            }
                        }

                        // Sync section
                        item {
                            val syncExpanded = expandedSections.contains("Sync")
                            val syncMatchesSearch = matchesSearchQuery("Sync", searchQuery) ||
                                listOf("sync", "cloud", "backup", "restore", "account", "server", "upload", "bookmarks", "history", "passwords").any {
                                    it.contains(searchQuery, ignoreCase = true)
                                }

                            CollapsibleSectionHeader(
                                title = "Sync",
                                isExpanded = syncExpanded,
                                onToggle = { viewModel.toggleSection("Sync") },
                                matchesSearch = syncMatchesSearch
                            )
                        }

                        item {
                            val syncExpanded = expandedSections.contains("Sync")
                            val syncMatchesSearch = matchesSearchQuery("Sync", searchQuery) ||
                                listOf("sync", "cloud", "backup", "restore", "account", "server", "upload", "bookmarks", "history", "passwords").any {
                                    it.contains(searchQuery, ignoreCase = true)
                                }

                            AnimatedVisibility(visible = syncExpanded || syncMatchesSearch) {
                                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    SwitchSettingItem(
                                        title = "Sync Enabled",
                                        subtitle = "Sync data across devices",
                                        checked = settings!!.syncEnabled,
                                        onCheckedChange = {
                                            viewModel.updateSettings(settings!!.copy(syncEnabled = it))
                                        }
                                    )

                                    if (settings!!.syncEnabled) {
                                        SwitchSettingItem(
                                            title = "Sync Bookmarks",
                                            subtitle = "Sync bookmarks across devices",
                                            checked = settings!!.syncBookmarks,
                                            onCheckedChange = {
                                                viewModel.updateSettings(settings!!.copy(syncBookmarks = it))
                                            }
                                        )

                                        SwitchSettingItem(
                                            title = "Sync History",
                                            subtitle = "Sync browsing history across devices",
                                            checked = settings!!.syncHistory,
                                            onCheckedChange = {
                                                viewModel.updateSettings(settings!!.copy(syncHistory = it))
                                            }
                                        )

                                        SwitchSettingItem(
                                            title = "Sync Passwords",
                                            subtitle = "Sync saved passwords across devices",
                                            checked = settings!!.syncPasswords,
                                            onCheckedChange = {
                                                viewModel.updateSettings(settings!!.copy(syncPasswords = it))
                                            }
                                        )

                                        SwitchSettingItem(
                                            title = "Sync Settings",
                                            subtitle = "Sync browser settings across devices",
                                            checked = settings!!.syncSettings,
                                            onCheckedChange = {
                                                viewModel.updateSettings(settings!!.copy(syncSettings = it))
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        // Bookmarks section
                        item {
                            SettingsSectionHeader("Bookmarks")
                        }

                        item {
                            NavigationSettingItem(
                                title = "Import Bookmarks",
                                subtitle = "Import from HTML bookmark file",
                                onClick = onImportBookmarks
                            )
                        }

                        item {
                            NavigationSettingItem(
                                title = "Export Bookmarks",
                                subtitle = "Export all bookmarks to HTML file",
                                onClick = onExportBookmarks
                            )
                        }

                        // AI Features section
                        item {
                            CollapsibleSectionHeader(
                                title = "AI Features",
                                isExpanded = expandedSections.contains("ai_features"),
                                onToggle = { viewModel.toggleSection("ai_features") },
                                matchesSearch = searchQuery.isNotEmpty() && listOf(
                                    "ai", "artificial", "intelligence", "smart", "assistant",
                                    "auto", "completion", "summaries", "translation", "read aloud"
                                ).any { it.contains(searchQuery, ignoreCase = true) }
                            )
                        }

                        if (expandedSections.contains("ai_features")) {
                            item {
                                SwitchSettingItem(
                                    title = "AI Summaries",
                                    subtitle = "Generate AI-powered page summaries",
                                    checked = settings!!.aiSummaries,
                                    onCheckedChange = {
                                        viewModel.updateSettings(settings!!.copy(aiSummaries = it))
                                    }
                                )
                            }

                            item {
                                SwitchSettingItem(
                                    title = "AI Translation",
                                    subtitle = "Translate pages with AI",
                                    checked = settings!!.aiTranslation,
                                    onCheckedChange = {
                                        viewModel.updateSettings(settings!!.copy(aiTranslation = it))
                                    }
                                )
                            }

                            item {
                                SwitchSettingItem(
                                    title = "Read Aloud",
                                    subtitle = "Text-to-speech for web content",
                                    checked = settings!!.readAloud,
                                    onCheckedChange = {
                                        viewModel.updateSettings(settings!!.copy(readAloud = it))
                                    }
                                )
                            }
                        }

                        // Command Bar section
                        item {
                            SettingsSectionHeader("Command Bar")
                        }

                        item {
                            SwitchSettingItem(
                                title = "Auto-hide Command Bar",
                                subtitle = "Automatically hide command bar after timeout",
                                checked = settings!!.commandBarAutoHide,
                                onCheckedChange = {
                                    viewModel.updateSettings(settings!!.copy(commandBarAutoHide = it))
                                }
                            )
                        }

                        if (settings!!.commandBarAutoHide) {
                            item {
                                SliderSettingItem(
                                    title = "Auto-hide Delay",
                                    subtitle = "Delay: ${settings!!.commandBarAutoHideDelayMs}ms",
                                    value = settings!!.commandBarAutoHideDelayMs.toFloat(),
                                    valueRange = 3000f..30000f,
                                    steps = 26,
                                    onValueChange = {
                                        viewModel.updateSettings(settings!!.copy(commandBarAutoHideDelayMs = it.toLong()))
                                    }
                                )
                            }
                        }

                        // WebXR section
                        item {
                            CollapsibleSectionHeader(
                                title = "WebXR",
                                isExpanded = expandedSections.contains("webxr"),
                                onToggle = { viewModel.toggleSection("webxr") },
                                matchesSearch = searchQuery.isNotEmpty() && listOf(
                                    "xr", "vr", "ar", "virtual", "augmented", "reality",
                                    "3d", "immersive", "headset", "webxr"
                                ).any { it.contains(searchQuery, ignoreCase = true) }
                            )
                        }

                        if (expandedSections.contains("webxr")) {
                            item {
                                SwitchSettingItem(
                                    title = "Enable WebXR",
                                    subtitle = "Master switch for WebXR functionality",
                                    checked = settings!!.enableWebXR,
                                    onCheckedChange = {
                                        viewModel.updateSettings(settings!!.copy(enableWebXR = it))
                                    }
                                )
                            }

                            if (settings!!.enableWebXR) {
                                item {
                                    SwitchSettingItem(
                                        title = "Enable AR",
                                        subtitle = "Allow immersive augmented reality sessions",
                                        checked = settings!!.enableAR,
                                        onCheckedChange = {
                                            viewModel.updateSettings(settings!!.copy(enableAR = it))
                                        }
                                    )
                                }

                                item {
                                    SwitchSettingItem(
                                        title = "Enable VR",
                                        subtitle = "Allow immersive virtual reality sessions",
                                        checked = settings!!.enableVR,
                                        onCheckedChange = {
                                            viewModel.updateSettings(settings!!.copy(enableVR = it))
                                        }
                                    )
                                }

                                item {
                                    XRPerformanceModeSettingItem(
                                        currentMode = settings!!.xrPerformanceMode,
                                        onModeSelected = {
                                            viewModel.updateSettings(settings!!.copy(xrPerformanceMode = it))
                                        }
                                    )
                                }

                                item {
                                    SliderSettingItem(
                                        title = "XR Auto-Pause Timeout",
                                        subtitle = "Auto-pause after ${settings!!.xrAutoPauseTimeout} minutes",
                                        value = settings!!.xrAutoPauseTimeout.toFloat(),
                                        valueRange = 10f..120f,
                                        steps = 21,
                                        onValueChange = {
                                            viewModel.updateSettings(settings!!.copy(xrAutoPauseTimeout = it.toInt()))
                                        }
                                    )
                                }

                                item {
                                    SwitchSettingItem(
                                        title = "Show FPS Indicator",
                                        subtitle = "Display frame rate in XR sessions",
                                        checked = settings!!.xrShowFPSIndicator,
                                        onCheckedChange = {
                                            viewModel.updateSettings(settings!!.copy(xrShowFPSIndicator = it))
                                        }
                                    )
                                }

                                item {
                                    SwitchSettingItem(
                                        title = "Require Wi-Fi for XR",
                                        subtitle = "Only allow XR on Wi-Fi connections",
                                        checked = settings!!.xrRequireWiFi,
                                        onCheckedChange = {
                                            viewModel.updateSettings(settings!!.copy(xrRequireWiFi = it))
                                        }
                                    )
                                }
                            }
                        }

                        // Advanced section
                        item {
                            CollapsibleSectionHeader(
                                title = "Advanced",
                                isExpanded = expandedSections.contains("advanced"),
                                onToggle = { viewModel.toggleSection("advanced") },
                                matchesSearch = searchQuery.isNotEmpty() && listOf(
                                    "advanced", "developer", "debug", "experimental",
                                    "flags", "reset", "clear", "defaults"
                                ).any { it.contains(searchQuery, ignoreCase = true) }
                            )
                        }

                        if (expandedSections.contains("advanced")) {
                            item {
                                Spacer(modifier = Modifier.height(8.dp))
                            }

                            item {
                                TextButton(
                                    onClick = { viewModel.resetToDefaults() },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp)
                                ) {
                                    Text(
                                        "Reset to Defaults",
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                            }

                            item {
                                Spacer(modifier = Modifier.height(16.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Landscape Settings Layout - Two-pane AR/XR optimized layout
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LandscapeSettingsLayout(
    settings: BrowserSettings?,
    isLoading: Boolean,
    error: String?,
    selectedCategory: SettingsCategory,
    onCategorySelected: (SettingsCategory) -> Unit,
    onNavigateBack: () -> Unit,
    onNavigateToXRSettings: () -> Unit,
    onNavigateToSitePermissions: () -> Unit,
    onNavigateToARPreview: () -> Unit,
    onImportBookmarks: () -> Unit,
    onExportBookmarks: () -> Unit,
    viewModel: SettingsViewModel
) {
    Row(
        modifier = Modifier.fillMaxSize()
    ) {
        // Left pane: Category navigation (30% width)
        Surface(
            modifier = Modifier
                .fillMaxHeight()
                .weight(0.3f),
            color = OceanTheme.surface.copy(alpha = 0.85f),
            shadowElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(vertical = 16.dp)
            ) {
                // Back button and title
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = OceanTheme.textPrimary
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Settings",
                        style = MaterialTheme.typography.titleLarge,
                        color = OceanTheme.textPrimary
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Category list
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    SettingsCategory.entries.forEach { category ->
                        CategoryNavItem(
                            category = category,
                            isSelected = category == selectedCategory,
                            onClick = { onCategorySelected(category) }
                        )
                    }
                }
            }
        }

        // Right pane: Settings content (70% width)
        Surface(
            modifier = Modifier
                .fillMaxHeight()
                .weight(0.7f)
                .padding(16.dp),
            color = OceanTheme.surface.copy(alpha = 0.92f),
            shape = RoundedCornerShape(16.dp),
            shadowElevation = 12.dp
        ) {
            when {
                isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = OceanTheme.primary)
                    }
                }

                error != null -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = error,
                            style = MaterialTheme.typography.bodyLarge,
                            color = OceanTheme.error
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { viewModel.loadSettings() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = OceanTheme.primary
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Retry")
                        }
                    }
                }

                settings != null -> {
                    CategorySettingsContent(
                        category = selectedCategory,
                        settings = settings,
                        viewModel = viewModel,
                        onNavigateToXRSettings = onNavigateToXRSettings,
                        onNavigateToSitePermissions = onNavigateToSitePermissions,
                        onNavigateToARPreview = onNavigateToARPreview,
                        onImportBookmarks = onImportBookmarks,
                        onExportBookmarks = onExportBookmarks
                    )
                }
            }
        }
    }
}

/**
 * Category navigation item for landscape two-pane layout
 */
@Composable
private fun CategoryNavItem(
    category: SettingsCategory,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() },
        color = if (isSelected) OceanTheme.primary.copy(alpha = 0.2f) else Color.Transparent,
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = category.icon,
                contentDescription = null,
                tint = if (isSelected) OceanTheme.primary else OceanTheme.textSecondary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = category.title,
                style = MaterialTheme.typography.bodyLarge,
                color = if (isSelected) OceanTheme.primary else OceanTheme.textPrimary,
                modifier = Modifier.weight(1f)
            )
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = OceanTheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

/**
 * Settings content for selected category in landscape mode
 */
@Composable
private fun CategorySettingsContent(
    category: SettingsCategory,
    settings: BrowserSettings,
    viewModel: SettingsViewModel,
    onNavigateToXRSettings: () -> Unit,
    onNavigateToSitePermissions: () -> Unit,
    onNavigateToARPreview: () -> Unit,
    onImportBookmarks: () -> Unit,
    onExportBookmarks: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Text(
                text = category.title,
                style = MaterialTheme.typography.headlineSmall,
                color = OceanTheme.textPrimary,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }

        when (category) {
            SettingsCategory.GENERAL -> {
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

            SettingsCategory.APPEARANCE -> {
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

            SettingsCategory.PRIVACY -> {
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

            SettingsCategory.ADVANCED -> {
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
                    val pathValidation by viewModel.pathValidation.collectAsState()
                    ARXRSettingCard {
                        DownloadPathSettingItem(
                            currentPath = settings.downloadPath,
                            onPathChanged = {
                                viewModel.updateSettings(settings.copy(downloadPath = it))
                            },
                            pathValidation = pathValidation,
                            onValidatePath = { path ->
                                viewModel.validateDownloadPath(path)
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
                // Bookmarks import/export
                item {
                    ARXRSettingCard {
                        NavigationSettingItem(
                            title = "Import Bookmarks",
                            subtitle = "Import from HTML bookmark file",
                            onClick = onImportBookmarks
                        )
                    }
                }
                item {
                    ARXRSettingCard {
                        NavigationSettingItem(
                            title = "Export Bookmarks",
                            subtitle = "Export all bookmarks to HTML file",
                            onClick = onExportBookmarks
                        )
                    }
                }
            }

            SettingsCategory.XR -> {
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
        }

        // Reset button at bottom
        item {
            Spacer(modifier = Modifier.height(24.dp))
            TextButton(
                onClick = { viewModel.resetToDefaults() },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    "Reset to Defaults",
                    color = OceanTheme.error
                )
            }
        }
    }
}

/**
 * AR/XR styled setting card wrapper with glassmorphism
 */
@Composable
private fun ARXRSettingCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = OceanTheme.surfaceElevated.copy(alpha = 0.7f),
        shape = RoundedCornerShape(12.dp),
        shadowElevation = 4.dp
    ) {
        content()
    }
}

/**
 * SettingsSectionHeader - Section header for settings groups
 */
@Composable
fun SettingsSectionHeader(
    title: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier.padding(
            horizontal = 16.dp,
            vertical = 12.dp
        )
    )
}

/**
 * SwitchSettingItem - Setting with toggle switch
 */
@Composable
fun SwitchSettingItem(
    title: String,
    subtitle: String?,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = subtitle?.let { { Text(it) } },
        trailingContent = {
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange
            )
        },
        modifier = modifier
    )
}

/**
 * SliderSettingItem - Setting with slider control for numeric values
 */
@Composable
fun SliderSettingItem(
    title: String,
    subtitle: String?,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int = 0,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge
        )
        subtitle?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            steps = steps,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/**
 * SearchEngineSettingItem - Search engine selection
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchEngineSettingItem(
    currentEngine: BrowserSettings.SearchEngine,
    onEngineSelected: (BrowserSettings.SearchEngine) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    val searchEngines = listOf(
        BrowserSettings.SearchEngine.GOOGLE,
        BrowserSettings.SearchEngine.DUCKDUCKGO,
        BrowserSettings.SearchEngine.BING,
        BrowserSettings.SearchEngine.BRAVE,
        BrowserSettings.SearchEngine.ECOSIA
    )

    ListItem(
        headlineContent = { Text("Search Engine") },
        supportingContent = { Text(currentEngine.name.lowercase().replaceFirstChar { it.uppercase() }) },
        trailingContent = {
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = it }
            ) {
                TextButton(
                    onClick = { expanded = true },
                    modifier = Modifier.menuAnchor()
                ) {
                    Text(currentEngine.name.lowercase().replaceFirstChar { it.uppercase() })
                }

                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    searchEngines.forEach { engine ->
                        DropdownMenuItem(
                            text = { Text(engine.name.lowercase().replaceFirstChar { it.uppercase() }) },
                            onClick = {
                                onEngineSelected(engine)
                                expanded = false
                            }
                        )
                    }
                }
            }
        },
        modifier = modifier
    )
}

/**
 * HomepageSettingItem - Homepage URL configuration
 */
@Composable
fun HomepageSettingItem(
    currentHomepage: String,
    onHomepageChanged: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showDialog by remember { mutableStateOf(false) }

    ListItem(
        headlineContent = { Text("Homepage") },
        supportingContent = { Text(currentHomepage.ifBlank { "Not set" }) },
        modifier = modifier.clickable { showDialog = true }
    )

    if (showDialog) {
        var tempHomepage by remember { mutableStateOf(currentHomepage) }

        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Set Homepage") },
            text = {
                OutlinedTextField(
                    value = tempHomepage,
                    onValueChange = { tempHomepage = it },
                    label = { Text("URL") },
                    placeholder = { Text("https://example.com") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onHomepageChanged(tempHomepage)
                        showDialog = false
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

/**
 * ThemeSettingItem - Theme selection
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemeSettingItem(
    currentTheme: BrowserSettings.Theme,
    onThemeSelected: (BrowserSettings.Theme) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    ListItem(
        headlineContent = { Text("Theme") },
        supportingContent = {
            Text(
                when (currentTheme) {
                    BrowserSettings.Theme.LIGHT -> "Light"
                    BrowserSettings.Theme.DARK -> "Dark"
                    BrowserSettings.Theme.SYSTEM -> "System default"
                    BrowserSettings.Theme.AUTO -> "Auto (time-based)"
                }
            )
        },
        trailingContent = {
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = it }
            ) {
                TextButton(
                    onClick = { expanded = true },
                    modifier = Modifier.menuAnchor()
                ) {
                    Text("Change")
                }

                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("System default") },
                        onClick = {
                            onThemeSelected(BrowserSettings.Theme.SYSTEM)
                            expanded = false
                        }
                    )

                    DropdownMenuItem(
                        text = { Text("Light") },
                        onClick = {
                            onThemeSelected(BrowserSettings.Theme.LIGHT)
                            expanded = false
                        }
                    )

                    DropdownMenuItem(
                        text = { Text("Dark") },
                        onClick = {
                            onThemeSelected(BrowserSettings.Theme.DARK)
                            expanded = false
                        }
                    )

                    DropdownMenuItem(
                        text = { Text("Auto (time-based)") },
                        onClick = {
                            onThemeSelected(BrowserSettings.Theme.AUTO)
                            expanded = false
                        }
                    )
                }
            }
        },
        modifier = modifier
    )
}

/**
 * AutoPlaySettingItem - Auto-play media selection
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AutoPlaySettingItem(
    currentAutoPlay: BrowserSettings.AutoPlay,
    onAutoPlaySelected: (BrowserSettings.AutoPlay) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    ListItem(
        headlineContent = { Text("Media Auto-play") },
        supportingContent = {
            Text(
                when (currentAutoPlay) {
                    BrowserSettings.AutoPlay.ALWAYS -> "Always"
                    BrowserSettings.AutoPlay.WIFI_ONLY -> "Wi-Fi only"
                    BrowserSettings.AutoPlay.NEVER -> "Never"
                    BrowserSettings.AutoPlay.ASK -> "Ask every time"
                }
            )
        },
        trailingContent = {
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = it }
            ) {
                TextButton(
                    onClick = { expanded = true },
                    modifier = Modifier.menuAnchor()
                ) {
                    Text("Change")
                }

                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Always") },
                        onClick = {
                            onAutoPlaySelected(BrowserSettings.AutoPlay.ALWAYS)
                            expanded = false
                        }
                    )

                    DropdownMenuItem(
                        text = { Text("Wi-Fi only") },
                        onClick = {
                            onAutoPlaySelected(BrowserSettings.AutoPlay.WIFI_ONLY)
                            expanded = false
                        }
                    )

                    DropdownMenuItem(
                        text = { Text("Never") },
                        onClick = {
                            onAutoPlaySelected(BrowserSettings.AutoPlay.NEVER)
                            expanded = false
                        }
                    )

                    DropdownMenuItem(
                        text = { Text("Ask every time") },
                        onClick = {
                            onAutoPlaySelected(BrowserSettings.AutoPlay.ASK)
                            expanded = false
                        }
                    )
                }
            }
        },
        modifier = modifier
    )
}

/**
 * NavigationSettingItem - Setting that navigates to another screen
 */
@Composable
fun NavigationSettingItem(
    title: String,
    subtitle: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = subtitle?.let { { Text(it) } },
        trailingContent = {
            Icon(
                imageVector = Icons.Filled.Info,
                contentDescription = "Navigate"
            )
        },
        modifier = modifier.clickable { onClick() }
    )
}

/**
 * FontSizeSettingItem - Font size selection
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FontSizeSettingItem(
    currentFontSize: BrowserSettings.FontSize,
    onFontSizeSelected: (BrowserSettings.FontSize) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    ListItem(
        headlineContent = { Text("Font Size") },
        supportingContent = {
            Text(
                when (currentFontSize) {
                    BrowserSettings.FontSize.TINY -> "Tiny"
                    BrowserSettings.FontSize.SMALL -> "Small"
                    BrowserSettings.FontSize.MEDIUM -> "Medium"
                    BrowserSettings.FontSize.LARGE -> "Large"
                    BrowserSettings.FontSize.HUGE -> "Huge"
                }
            )
        },
        trailingContent = {
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = it }
            ) {
                TextButton(
                    onClick = { expanded = true },
                    modifier = Modifier.menuAnchor()
                ) {
                    Text("Change")
                }

                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    BrowserSettings.FontSize.entries.forEach { size ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    when (size) {
                                        BrowserSettings.FontSize.TINY -> "Tiny"
                                        BrowserSettings.FontSize.SMALL -> "Small"
                                        BrowserSettings.FontSize.MEDIUM -> "Medium"
                                        BrowserSettings.FontSize.LARGE -> "Large"
                                        BrowserSettings.FontSize.HUGE -> "Huge"
                                    }
                                )
                            },
                            onClick = {
                                onFontSizeSelected(size)
                                expanded = false
                            }
                        )
                    }
                }
            }
        },
        modifier = modifier
    )
}

/**
 * NewTabPageSettingItem - New tab page selection
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewTabPageSettingItem(
    currentNewTabPage: BrowserSettings.NewTabPage,
    onNewTabPageSelected: (BrowserSettings.NewTabPage) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    ListItem(
        headlineContent = { Text("New Tab Page") },
        supportingContent = {
            Text(
                when (currentNewTabPage) {
                    BrowserSettings.NewTabPage.BLANK -> "Blank page"
                    BrowserSettings.NewTabPage.HOME_PAGE -> "Home page"
                    BrowserSettings.NewTabPage.TOP_SITES -> "Top sites"
                    BrowserSettings.NewTabPage.MOST_VISITED -> "Most visited"
                    BrowserSettings.NewTabPage.SPEED_DIAL -> "Speed dial"
                    BrowserSettings.NewTabPage.NEWS_FEED -> "News feed"
                }
            )
        },
        trailingContent = {
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = it }
            ) {
                TextButton(
                    onClick = { expanded = true },
                    modifier = Modifier.menuAnchor()
                ) {
                    Text("Change")
                }

                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    BrowserSettings.NewTabPage.entries.forEach { page ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    when (page) {
                                        BrowserSettings.NewTabPage.BLANK -> "Blank page"
                                        BrowserSettings.NewTabPage.HOME_PAGE -> "Home page"
                                        BrowserSettings.NewTabPage.TOP_SITES -> "Top sites"
                                        BrowserSettings.NewTabPage.MOST_VISITED -> "Most visited"
                                        BrowserSettings.NewTabPage.SPEED_DIAL -> "Speed dial"
                                        BrowserSettings.NewTabPage.NEWS_FEED -> "News feed"
                                    }
                                )
                            },
                            onClick = {
                                onNewTabPageSelected(page)
                                expanded = false
                            }
                        )
                    }
                }
            }
        },
        modifier = modifier
    )
}

/**
 * DownloadPathSettingItem - Download path configuration with file picker
 */
@Composable
fun DownloadPathSettingItem(
    currentPath: String?,
    onPathChanged: (String?) -> Unit,
    pathValidation: com.augmentalis.webavanue.platform.ValidationResult? = null,
    onValidatePath: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    // Get platform-specific file picker and permission manager
    val context = androidx.compose.ui.platform.LocalContext.current
    val filePickerLauncher = remember { com.augmentalis.webavanue.platform.DownloadFilePickerLauncher(context) }
    val permissionManager = remember { com.augmentalis.webavanue.platform.DownloadPermissionManager(context) }

    // Permission request state
    var showPermissionDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }

    // Snackbar state for validation errors
    val snackbarHostState = remember { androidx.compose.material3.SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    // Trigger validation when path changes
    LaunchedEffect(currentPath) {
        currentPath?.let { path ->
            onValidatePath(path)
        }
    }

    // Show validation error/warning via Snackbar
    LaunchedEffect(pathValidation) {
        pathValidation?.let { validation ->
            when {
                !validation.isValid && validation.errorMessage != null -> {
                    // Show error
                    val result = snackbarHostState.showSnackbar(
                        message = validation.errorMessage,
                        actionLabel = "Use Default",
                        duration = androidx.compose.material3.SnackbarDuration.Long
                    )
                    if (result == androidx.compose.material3.SnackbarResult.ActionPerformed) {
                        onPathChanged(null)
                    }
                }
                validation.isLowSpace -> {
                    // Show warning
                    snackbarHostState.showSnackbar(
                        message = "Low storage space: ${validation.availableSpaceMB} MB available",
                        actionLabel = "OK",
                        duration = androidx.compose.material3.SnackbarDuration.Short
                    )
                }
            }
        }
    }

    // File picker result launcher
    val filePickerResultLauncher = rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.OpenDocumentTree()
    ) { uri: android.net.Uri? ->
        uri?.let {
            // Take persistable permission
            filePickerLauncher.takePersistablePermission(it.toString())
            // Update path
            onPathChanged(it.toString())
        }
    }

    // Permission result launcher
    val permissionResultLauncher = rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            // Permission granted, launch file picker
            filePickerResultLauncher.launch(currentPath?.let { android.net.Uri.parse(it) })
        } else {
            // Permission denied - show settings dialog
            if (permissionManager.isPermanentlyDenied()) {
                showSettingsDialog = true
            }
        }
    }

    androidx.compose.material3.Scaffold(
        snackbarHost = { androidx.compose.material3.SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        ListItem(
            headlineContent = { Text("Download Location") },
            supportingContent = {
                Column {
                    Text(
                        currentPath?.let { filePickerLauncher.getDisplayPath(it) } ?: "Default system path",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    // Show validation info
                    pathValidation?.let { validation ->
                        if (validation.isValid) {
                            Text(
                                "${validation.availableSpaceMB} MB available",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (validation.isLowSpace) {
                                    MaterialTheme.colorScheme.error
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                        }
                    }
                }
            },
        trailingContent = {
            Row {
                // Browse button
                IconButton(
                    onClick = {
                        // Check if permission is required and not granted
                        if (permissionManager.isPermissionRequired() && !permissionManager.isPermissionGranted()) {
                            // Show rationale or request permission
                            if (permissionManager.shouldShowRationale()) {
                                showPermissionDialog = true
                            } else {
                                // First request - just ask
                                permissionResultLauncher.launch(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                            }
                        } else {
                            // Permission granted or not required - launch picker
                            filePickerResultLauncher.launch(currentPath?.let { android.net.Uri.parse(it) })
                        }
                    }
                ) {
                    Icon(androidx.compose.material.icons.Icons.Default.FolderOpen, contentDescription = "Browse")
                }

                // Use Default button (only show if custom path is set)
                if (currentPath != null) {
                    IconButton(
                        onClick = {
                            // Release persistable permission
                            filePickerLauncher.releasePersistablePermission(currentPath)
                            // Clear path
                            onPathChanged(null)
                        }
                    ) {
                        Icon(androidx.compose.material.icons.Icons.Default.Delete, contentDescription = "Use default")
                    }
                }
            }
        },
            modifier = modifier.padding(paddingValues)
        )
    }

    // Permission rationale dialog
    if (showPermissionDialog) {
        AlertDialog(
            onDismissRequest = { showPermissionDialog = false },
            title = { Text("Storage Permission Required") },
            text = { Text("WebAvanue needs storage permission to save downloads to custom locations. The default Downloads folder will be used if permission is not granted.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showPermissionDialog = false
                        permissionResultLauncher.launch(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    }
                ) {
                    Text("Grant Permission")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPermissionDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Settings dialog (for permanently denied permission)
    if (showSettingsDialog) {
        AlertDialog(
            onDismissRequest = { showSettingsDialog = false },
            title = { Text("Permission Denied") },
            text = { Text("Storage permission was denied. Please enable it in Settings > Permissions to use custom download locations.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showSettingsDialog = false
                        permissionManager.openPermissionSettings()
                    }
                ) {
                    Text("Open Settings")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSettingsDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

/**
 * XRPerformanceModeSettingItem - XR performance mode selection
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun XRPerformanceModeSettingItem(
    currentMode: BrowserSettings.XRPerformanceMode,
    onModeSelected: (BrowserSettings.XRPerformanceMode) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    ListItem(
        headlineContent = { Text("XR Performance Mode") },
        supportingContent = {
            Text(
                when (currentMode) {
                    BrowserSettings.XRPerformanceMode.HIGH_QUALITY -> "High quality (90fps)"
                    BrowserSettings.XRPerformanceMode.BALANCED -> "Balanced (60fps)"
                    BrowserSettings.XRPerformanceMode.BATTERY_SAVER -> "Battery saver (45fps)"
                }
            )
        },
        trailingContent = {
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = it }
            ) {
                TextButton(
                    onClick = { expanded = true },
                    modifier = Modifier.menuAnchor()
                ) {
                    Text("Change")
                }

                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    BrowserSettings.XRPerformanceMode.entries.forEach { mode ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    when (mode) {
                                        BrowserSettings.XRPerformanceMode.HIGH_QUALITY -> "High quality (90fps)"
                                        BrowserSettings.XRPerformanceMode.BALANCED -> "Balanced (60fps)"
                                        BrowserSettings.XRPerformanceMode.BATTERY_SAVER -> "Battery saver (45fps)"
                                    }
                                )
                            },
                            onClick = {
                                onModeSelected(mode)
                                expanded = false
                            }
                        )
                    }
                }
            }
        },
        modifier = modifier
    )
}
// ==================== Search & Collapsible Section Composables ====================

/**
 * Settings search bar with expand/collapse controls
 *
 * Provides:
 * - Search input for filtering settings (case-insensitive)
 * - Clear button when query is active
 * - Expand All / Collapse All buttons when not searching
 *
 * @param searchQuery Current search query
 * @param onSearchQueryChange Callback when search query changes
 * @param onExpandAll Callback to expand all sections
 * @param onCollapseAll Callback to collapse all sections
 * @param modifier Modifier for customization
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsSearchBar(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onExpandAll: () -> Unit,
    onCollapseAll: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = OceanTheme.surface,
        shadowElevation = 4.dp
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("search_bar"),
                placeholder = { Text("Search settings...") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search"
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchQueryChange("") }) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "Clear search",
                                modifier = Modifier.graphicsLayer(rotationZ = 180f)
                            )
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = OceanTheme.primary,
                    unfocusedBorderColor = OceanTheme.textSecondary.copy(alpha = 0.3f)
                )
            )

            // Expand/Collapse all buttons (hidden when searching)
            if (searchQuery.isEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onExpandAll) {
                        Text("Expand All", color = OceanTheme.primary)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    TextButton(onClick = onCollapseAll) {
                        Text("Collapse All", color = OceanTheme.primary)
                    }
                }
            }
        }
    }
}

/**
 * Helper function to check if content matches search query
 *
 * @param title Section title
 * @param query Search query
 * @return True if title contains query (case-insensitive)
 */
private fun matchesSearchQuery(title: String, query: String): Boolean {
    return title.contains(query, ignoreCase = true)
}

/**
 * Collapsible section header with expand/collapse indicator
 *
 * Features:
 * - Expand/collapse icon (chevron up/down)
 * - Highlight when section matches search
 * - Click to toggle expansion
 *
 * @param title Section title
 * @param isExpanded Whether section is currently expanded
 * @param onToggle Callback when header is clicked
 * @param matchesSearch Whether this section matches current search
 * @param modifier Modifier for customization
 */
@Composable
private fun CollapsibleSectionHeader(
    title: String,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    matchesSearch: Boolean = false,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onToggle() }
            .padding(horizontal = 16.dp, vertical = 8.dp),
        color = if (matchesSearch) OceanTheme.primary.copy(alpha = 0.15f) else Color.Transparent,
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = if (matchesSearch) OceanTheme.primary else MaterialTheme.colorScheme.primary
            )
            Icon(
                imageVector = if (isExpanded) Icons.Default.Info else Icons.Default.ChevronRight,
                contentDescription = if (isExpanded) "Collapse" else "Expand",
                tint = if (matchesSearch) OceanTheme.primary else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.graphicsLayer(
                    rotationZ = if (isExpanded && title.isNotEmpty()) 90f else 0f
                )
            )
        }
    }
}
