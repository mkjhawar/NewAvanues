package com.augmentalis.Avanues.web.universal.presentation.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.augmentalis.webavanue.domain.model.BrowserSettings
import com.augmentalis.Avanues.web.universal.presentation.ui.theme.ThemeType
import com.augmentalis.Avanues.web.universal.presentation.viewmodel.SettingsViewModel

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
    modifier: Modifier = Modifier
) {
    val settings by viewModel.settings.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val saveSuccess by viewModel.saveSuccess.collectAsState()

    // Show success snackbar
    LaunchedEffect(saveSuccess) {
        if (saveSuccess) {
            // Success feedback shown
            kotlinx.coroutines.delay(2000)
            viewModel.clearSaveSuccess()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        },
        modifier = modifier
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
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
                        // General section
                        item {
                            SettingsSectionHeader("General")
                        }

                        item {
                            SearchEngineSettingItem(
                                currentEngine = settings!!.defaultSearchEngine,
                                onEngineSelected = { viewModel.setDefaultSearchEngine(it) }
                            )
                        }

                        item {
                            HomepageSettingItem(
                                currentHomepage = settings!!.homePage,
                                onHomepageChanged = { viewModel.setHomepage(it) }
                            )
                        }

                        // Theme section
                        item {
                            SettingsSectionHeader("Appearance")
                        }

                        item {
                            ThemeSettingItem(
                                currentTheme = settings!!.theme,
                                onThemeSelected = { viewModel.setTheme(it) }
                            )
                        }

                        // Privacy section
                        item {
                            SettingsSectionHeader("Privacy & Security")
                        }

                        item {
                            SwitchSettingItem(
                                title = "Enable JavaScript",
                                subtitle = "Required for most modern websites",
                                checked = settings!!.enableJavaScript,
                                onCheckedChange = { viewModel.setEnableJavaScript(it) }
                            )
                        }

                        item {
                            SwitchSettingItem(
                                title = "Enable Cookies",
                                subtitle = "Allow websites to store data",
                                checked = settings!!.enableCookies,
                                onCheckedChange = { viewModel.setEnableCookies(it) }
                            )
                        }

                        item {
                            SwitchSettingItem(
                                title = "Block Pop-ups",
                                subtitle = "Prevent pop-up windows",
                                checked = settings!!.blockPopups,
                                onCheckedChange = { viewModel.setBlockPopups(it) }
                            )
                        }

                        item {
                            SwitchSettingItem(
                                title = "Block Ads",
                                subtitle = "Block advertisements on web pages",
                                checked = settings!!.blockAds,
                                onCheckedChange = { viewModel.setBlockAds(it) }
                            )
                        }

                        item {
                            SwitchSettingItem(
                                title = "Block Trackers",
                                subtitle = "Prevent cross-site tracking",
                                checked = settings!!.blockTrackers,
                                onCheckedChange = { viewModel.setBlockTrackers(it) }
                            )
                        }

                        item {
                            SwitchSettingItem(
                                title = "Clear History on Exit",
                                subtitle = "Automatically clear browsing history",
                                checked = settings!!.clearHistoryOnExit,
                                onCheckedChange = {
                                    viewModel.updateSettings(settings!!.copy(clearHistoryOnExit = it))
                                }
                            )
                        }

                        // Advanced section
                        item {
                            SettingsSectionHeader("Advanced")
                        }

                        item {
                            SwitchSettingItem(
                                title = "Desktop Mode",
                                subtitle = "Request desktop version of websites",
                                checked = settings!!.useDesktopMode,
                                onCheckedChange = { viewModel.setDesktopMode(it) }
                            )
                        }

                        item {
                            AutoPlaySettingItem(
                                currentAutoPlay = settings!!.autoPlay,
                                onAutoPlaySelected = { viewModel.setAutoPlay(it) }
                            )
                        }

                        item {
                            SwitchSettingItem(
                                title = "Voice Commands",
                                subtitle = "Control browser with voice",
                                checked = settings!!.enableVoiceCommands,
                                onCheckedChange = { viewModel.setEnableVoiceCommands(it) }
                            )
                        }

                        item {
                            NavigationSettingItem(
                                title = "WebXR Settings",
                                subtitle = "Configure AR/VR preferences",
                                onClick = onNavigateToXRSettings
                            )
                        }

                        // Reset section
                        item {
                            Spacer(modifier = Modifier.height(24.dp))
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

            // Save success indicator
            if (saveSuccess) {
                Snackbar(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp)
                ) {
                    Text("Settings saved")
                }
            }
        }
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
                onCheckedChange = { newValue ->
                    println("SwitchSettingItem: $title toggled to $newValue")
                    onCheckedChange(newValue)
                }
            )
        },
        modifier = modifier
    )
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
