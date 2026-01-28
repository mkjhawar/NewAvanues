package com.augmentalis.webavanue

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.augmentalis.webavanue.BrowserSettings

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
 * Search engine setting item with dropdown and custom URL support
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchEngineSettingItem(
    currentEngine: BrowserSettings.SearchEngine,
    customEngineName: String = "Custom",
    customEngineUrl: String = "",
    onEngineSelected: (BrowserSettings.SearchEngine) -> Unit,
    onCustomNameChanged: ((String) -> Unit)? = null,
    onCustomUrlChanged: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = it },
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = if (currentEngine == BrowserSettings.SearchEngine.CUSTOM) customEngineName else currentEngine.name,
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
                        text = { Text(if (engine == BrowserSettings.SearchEngine.CUSTOM) "Custom..." else engine.name) },
                        onClick = {
                            onEngineSelected(engine)
                            expanded = false
                        }
                    )
                }
            }
        }

        // Show custom URL configuration when CUSTOM is selected
        if (currentEngine == BrowserSettings.SearchEngine.CUSTOM) {
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = customEngineName,
                onValueChange = { onCustomNameChanged?.invoke(it) },
                label = { Text("Engine Name") },
                placeholder = { Text("My Search Engine") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = customEngineUrl,
                onValueChange = { onCustomUrlChanged?.invoke(it) },
                label = { Text("Search URL") },
                placeholder = { Text("https://example.com/search?q=%s") },
                supportingText = { Text("Use %s where the search query should go") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
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
