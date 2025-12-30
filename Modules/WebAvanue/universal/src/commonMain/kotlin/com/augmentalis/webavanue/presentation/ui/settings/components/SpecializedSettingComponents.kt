package com.augmentalis.webavanue.ui.screen.settings.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.augmentalis.webavanue.domain.model.BrowserSettings

/**
 * Theme setting item with dropdown
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemeSettingItem(
    currentTheme: BrowserSettings.Theme,
    onThemeSelected: (BrowserSettings.Theme) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        OutlinedTextField(
            value = currentTheme.name,
            onValueChange = {},
            readOnly = true,
            label = { Text("Theme") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor().fillMaxWidth()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            BrowserSettings.Theme.entries.forEach { theme ->
                DropdownMenuItem(
                    text = { Text(theme.name) },
                    onClick = {
                        onThemeSelected(theme)
                        expanded = false
                    }
                )
            }
        }
    }
}

/**
 * Search engine setting item with dropdown
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchEngineSettingItem(
    currentEngine: BrowserSettings.SearchEngine,
    onEngineSelected: (BrowserSettings.SearchEngine) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        OutlinedTextField(
            value = currentEngine.name,
            onValueChange = {},
            readOnly = true,
            label = { Text("Search Engine") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor().fillMaxWidth()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            BrowserSettings.SearchEngine.entries.forEach { engine ->
                DropdownMenuItem(
                    text = { Text(engine.name) },
                    onClick = {
                        onEngineSelected(engine)
                        expanded = false
                    }
                )
            }
        }
    }
}

/**
 * Homepage setting item with text input
 */
@Composable
fun HomepageSettingItem(
    currentHomepage: String,
    onHomepageChanged: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = currentHomepage,
        onValueChange = onHomepageChanged,
        label = { Text("Homepage") },
        placeholder = { Text("https://example.com") },
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        singleLine = true
    )
}

/**
 * Font size setting item with dropdown
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FontSizeSettingItem(
    currentFontSize: BrowserSettings.FontSize,
    onFontSizeSelected: (BrowserSettings.FontSize) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        OutlinedTextField(
            value = currentFontSize.name,
            onValueChange = {},
            readOnly = true,
            label = { Text("Font Size") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor().fillMaxWidth()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            BrowserSettings.FontSize.entries.forEach { fontSize ->
                DropdownMenuItem(
                    text = { Text(fontSize.name) },
                    onClick = {
                        onFontSizeSelected(fontSize)
                        expanded = false
                    }
                )
            }
        }
    }
}

/**
 * Download path setting item
 */
@Composable
fun DownloadPathSettingItem(
    currentPath: String?,
    onPathSelected: () -> Unit,
    modifier: Modifier = Modifier
) {
    NavigationSettingItem(
        title = "Download Location",
        subtitle = "Choose where files are saved",
        currentValue = currentPath ?: "Default",
        onClick = onPathSelected,
        modifier = modifier
    )
}

/**
 * New tab page setting item with dropdown
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewTabPageSettingItem(
    currentNewTabPage: BrowserSettings.NewTabPage,
    onNewTabPageSelected: (BrowserSettings.NewTabPage) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        OutlinedTextField(
            value = currentNewTabPage.name,
            onValueChange = {},
            readOnly = true,
            label = { Text("New Tab Page") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor().fillMaxWidth()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            BrowserSettings.NewTabPage.entries.forEach { page ->
                DropdownMenuItem(
                    text = { Text(page.name) },
                    onClick = {
                        onNewTabPageSelected(page)
                        expanded = false
                    }
                )
            }
        }
    }
}

/**
 * XR Performance Mode setting item with dropdown
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun XRPerformanceModeSettingItem(
    currentMode: String,
    onModeSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val modes = listOf("Balanced", "Performance", "Battery Saver")

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        OutlinedTextField(
            value = currentMode,
            onValueChange = {},
            readOnly = true,
            label = { Text("Performance Mode") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor().fillMaxWidth()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            modes.forEach { mode ->
                DropdownMenuItem(
                    text = { Text(mode) },
                    onClick = {
                        onModeSelected(mode)
                        expanded = false
                    }
                )
            }
        }
    }
}
