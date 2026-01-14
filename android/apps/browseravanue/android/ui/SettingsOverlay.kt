package com.augmentalis.browseravanue.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.augmentalis.browseravanue.domain.model.BrowserSettings
import com.augmentalis.browseravanue.domain.model.CacheSize
import com.augmentalis.browseravanue.domain.model.SearchEngine
import com.augmentalis.browseravanue.domain.model.ZoomLevel

/**
 * Settings overlay
 *
 * Architecture:
 * - Full-screen overlay
 * - Grouped settings sections
 * - Live preview of changes
 * - Export/import controls
 *
 * Layout:
 * ```
 * ┌────────────────────────────┐
 * │  Settings          [X]     │
 * ├────────────────────────────┤
 * │  General                   │
 * │  ├ Homepage: [...]         │
 * │  ├ Search Engine: [▼]     │
 * │  └ Zoom Level: [▼]        │
 * │                            │
 * │  Privacy                   │
 * │  ├ Accept Cookies  [✓]    │
 * │  ├ Ad Blocking     [✓]    │
 * │  └ Do Not Track    [ ]    │
 * │                            │
 * │  Advanced                  │
 * │  ├ Desktop Mode    [ ]    │
 * │  ├ Dark Mode       [✓]    │
 * │  └ JavaScript      [✓]    │
 * └────────────────────────────┘
 * ```
 *
 * Features:
 * - Grouped settings
 * - Switches, dropdowns, text fields
 * - Save/cancel buttons
 * - Export/import data
 * - Clear browsing data
 *
 * Usage:
 * ```
 * if (showSettings) {
 *     SettingsOverlay(
 *         settings = settings,
 *         onSettingsChange = { ... },
 *         onDismiss = { ... }
 *     )
 * }
 * ```
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsOverlay(
    settings: BrowserSettings,
    onSettingsChange: (BrowserSettings) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var localSettings by remember(settings) { mutableStateOf(settings) }

    MagicSurface(
        modifier = modifier.fillMaxSize()
    ) {
        Scaffold(
            topBar = {
                MagicTopBar(
                    title = "Settings",
                    navigationIcon = {
                        MagicIconButton(
                            icon = {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Close"
                                )
                            },
                            onClick = onDismiss
                        )
                    },
                    actions = {
                        MagicTextButton(
                            text = "Save",
                            onClick = {
                                onSettingsChange(localSettings)
                                onDismiss()
                            }
                        )
                    }
                )
            }
        ) { paddingValues ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(MagicSpacing.md),
                verticalArrangement = Arrangement.spacedBy(MagicSpacing.md)
            ) {
                // General Section
                item {
                    SettingsSectionHeader(title = "General")
                }

                item {
                    MagicTextField(
                        value = localSettings.homepage,
                        onValueChange = { localSettings = localSettings.copy(homepage = it) },
                        label = "Homepage",
                        placeholder = "https://www.google.com",
                        singleLine = true
                    )
                }

                item {
                    SettingsDropdown(
                        label = "Search Engine",
                        selected = localSettings.searchEngine,
                        options = SearchEngine.values().toList(),
                        onSelect = { localSettings = localSettings.copy(searchEngine = it) },
                        displayText = { it.displayName }
                    )
                }

                item {
                    SettingsDropdown(
                        label = "Zoom Level",
                        selected = localSettings.zoomLevel,
                        options = ZoomLevel.values().toList(),
                        onSelect = { localSettings = localSettings.copy(zoomLevel = it) },
                        displayText = { it.displayName }
                    )
                }

                // Privacy Section
                item {
                    MagicSpacer(SpacingSize.LARGE)
                    SettingsSectionHeader(title = "Privacy & Security")
                }

                item {
                    SettingsSwitchRow(
                        title = "Accept Cookies",
                        subtitle = "Allow websites to store cookies",
                        checked = localSettings.acceptCookies,
                        onCheckedChange = { localSettings = localSettings.copy(acceptCookies = it) }
                    )
                }

                item {
                    SettingsSwitchRow(
                        title = "Third-Party Cookies",
                        subtitle = "Allow third-party cookies (less privacy)",
                        checked = localSettings.acceptThirdPartyCookies,
                        onCheckedChange = { localSettings = localSettings.copy(acceptThirdPartyCookies = it) }
                    )
                }

                item {
                    SettingsSwitchRow(
                        title = "Ad Blocking",
                        subtitle = "Block ads and trackers",
                        checked = localSettings.adBlockingEnabled,
                        onCheckedChange = { localSettings = localSettings.copy(adBlockingEnabled = it) }
                    )
                }

                item {
                    SettingsSwitchRow(
                        title = "Do Not Track",
                        subtitle = "Send DNT header (not enforced by sites)",
                        checked = localSettings.doNotTrackEnabled,
                        onCheckedChange = { localSettings = localSettings.copy(doNotTrackEnabled = it) }
                    )
                }

                item {
                    SettingsSwitchRow(
                        title = "Save Passwords",
                        subtitle = "Auto-save login credentials",
                        checked = localSettings.savePasswords,
                        onCheckedChange = { localSettings = localSettings.copy(savePasswords = it) }
                    )
                }

                // Appearance Section
                item {
                    MagicSpacer(SpacingSize.LARGE)
                    SettingsSectionHeader(title = "Appearance")
                }

                item {
                    SettingsSwitchRow(
                        title = "Desktop Mode",
                        subtitle = "Request desktop versions of websites",
                        checked = localSettings.desktopMode,
                        onCheckedChange = { localSettings = localSettings.copy(desktopMode = it) }
                    )
                }

                item {
                    SettingsSwitchRow(
                        title = "Dark Mode",
                        subtitle = "Force dark mode on websites (API 29+)",
                        checked = localSettings.darkMode,
                        onCheckedChange = { localSettings = localSettings.copy(darkMode = it) }
                    )
                }

                // Advanced Section
                item {
                    MagicSpacer(SpacingSize.LARGE)
                    SettingsSectionHeader(title = "Advanced")
                }

                item {
                    SettingsSwitchRow(
                        title = "JavaScript",
                        subtitle = "Enable JavaScript (required for most sites)",
                        checked = localSettings.javascriptEnabled,
                        onCheckedChange = { localSettings = localSettings.copy(javascriptEnabled = it) }
                    )
                }

                item {
                    SettingsSwitchRow(
                        title = "Auto-play Media",
                        subtitle = "Allow media to auto-play",
                        checked = localSettings.autoplayMedia,
                        onCheckedChange = { localSettings = localSettings.copy(autoplayMedia = it) }
                    )
                }

                item {
                    SettingsDropdown(
                        label = "Cache Size",
                        selected = localSettings.cacheSize,
                        options = CacheSize.values().toList(),
                        onSelect = { localSettings = localSettings.copy(cacheSize = it) },
                        displayText = { it.displayName }
                    )
                }

                // Data Management Section
                item {
                    MagicSpacer(SpacingSize.LARGE)
                    SettingsSectionHeader(title = "Data Management")
                }

                item {
                    MagicOutlinedButton(
                        text = "Export Browser Data",
                        onClick = { /* TODO: Export */ },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    MagicOutlinedButton(
                        text = "Import Browser Data",
                        onClick = { /* TODO: Import */ },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    MagicOutlinedButton(
                        text = "Clear Browsing Data",
                        onClick = { /* TODO: Clear data dialog */ },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // About Section
                item {
                    MagicSpacer(SpacingSize.LARGE)
                    SettingsSectionHeader(title = "About")
                }

                item {
                    MagicListItem(
                        title = "Browser Version",
                        subtitle = "1.0.0 (BrowserAvanue)"
                    )
                }

                item {
                    MagicListItem(
                        title = "Avanues Integration",
                        subtitle = "Voice commands enabled"
                    )
                }
            }
        }
    }
}

/**
 * Settings section header
 */
@Composable
private fun SettingsSectionHeader(
    title: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier
    )
}

/**
 * Settings switch row
 */
@Composable
private fun SettingsSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium
            )

            MagicSpacer(SpacingSize.SMALL)

            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        MagicSwitch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

/**
 * Settings dropdown
 */
@Composable
private fun <T> SettingsDropdown(
    label: String,
    selected: T,
    options: List<T>,
    onSelect: (T) -> Unit,
    displayText: (T) -> String,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(bottom = MagicSpacing.sm)
        )

        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = it }
        ) {
            OutlinedTextField(
                value = displayText(selected),
                onValueChange = {},
                readOnly = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(),
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) }
            )

            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(displayText(option)) },
                        onClick = {
                            onSelect(option)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}
