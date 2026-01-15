package com.augmentalis.Avanues.web.universal.presentation.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.augmentalis.webavanue.domain.model.BrowserSettings
import com.augmentalis.Avanues.web.universal.presentation.ui.theme.OceanTheme

/**
 * Shared setting item composables used across all settings sections.
 * These components provide consistent UI for different types of settings.
 */

/**
 * AR/XR styled setting card wrapper with glassmorphism
 */
@Composable
fun ARXRSettingCard(
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
 * DownloadPathSettingItem - Download path configuration
 */
@Composable
fun DownloadPathSettingItem(
    currentPath: String?,
    onPathChanged: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    var showDialog by remember { mutableStateOf(false) }

    ListItem(
        headlineContent = { Text("Download Location") },
        supportingContent = { Text(currentPath ?: "Default system path") },
        modifier = modifier.clickable { showDialog = true }
    )

    if (showDialog) {
        var tempPath by remember { mutableStateOf(currentPath ?: "") }

        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Set Download Location") },
            text = {
                OutlinedTextField(
                    value = tempPath,
                    onValueChange = { tempPath = it },
                    label = { Text("Path") },
                    placeholder = { Text("Leave empty for default") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onPathChanged(tempPath.ifBlank { null })
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
