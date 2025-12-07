package com.augmentalis.webavanue.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Bookmarks
import androidx.compose.material.icons.outlined.Tab
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Bottom navigation bar for the browser
 */
@Composable
fun BrowserBottomBar(
    tabCount: Int,
    onTabsClick: () -> Unit,
    onFavoritesClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onNewTab: () -> Unit,
    modifier: Modifier = Modifier
) {
    NavigationBar(
        modifier = modifier,
        tonalElevation = 3.dp
    ) {
        NavigationBarItem(
            selected = false,
            onClick = onTabsClick,
            icon = {
                BadgedBox(
                    badge = {
                        if (tabCount > 0) {
                            Badge { Text(tabCount.toString()) }
                        }
                    }
                ) {
                    Icon(Icons.Outlined.Tab, contentDescription = "Tabs")
                }
            },
            label = { Text("Tabs") }
        )

        NavigationBarItem(
            selected = false,
            onClick = onNewTab,
            icon = { Icon(Icons.Default.Add, contentDescription = "New Tab") },
            label = { Text("New") }
        )

        NavigationBarItem(
            selected = false,
            onClick = onFavoritesClick,
            icon = { Icon(Icons.Outlined.Bookmarks, contentDescription = "Favorites") },
            label = { Text("Favorites") }
        )

        NavigationBarItem(
            selected = false,
            onClick = onSettingsClick,
            icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
            label = { Text("Settings") }
        )
    }
}