package com.augmentalis.webavanue.presentation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.outlined.Bookmark
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.augmentalis.webavanue.*

/**
 * Tab manager dialog for switching and managing tabs
 */
@Composable
fun TabManagerDialog(
    tabs: List<Tab>,
    onTabSelect: (Tab) -> Unit,
    onTabClose: (Tab) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .fillMaxHeight(0.8f),
            shape = MaterialTheme.shapes.large
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Tabs (${tabs.size})",
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(tabs) { tab ->
                        TabItem(
                            tab = tab,
                            onSelect = { onTabSelect(tab) },
                            onClose = { onTabClose(tab) }
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Close")
                    }
                }
            }
        }
    }
}

@Composable
private fun TabItem(
    tab: Tab,
    onSelect: () -> Unit,
    onClose: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = tab.title.ifEmpty { tab.url },
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = tab.url,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (tab.isPinned) {
                Icon(
                    Icons.Outlined.PushPin,
                    contentDescription = "Pinned",
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
            }

            if (tab.isIncognito) {
                Icon(
                    Icons.Outlined.VisibilityOff,
                    contentDescription = "Incognito",
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
            }

            IconButton(onClick = onClose) {
                Icon(Icons.Default.Close, contentDescription = "Close tab")
            }
        }
    }
}

/**
 * Favorites dialog for managing bookmarks
 */
@Composable
fun FavoritesDialog(
    favorites: List<Favorite>,
    onFavoriteSelect: (Favorite) -> Unit,
    onFavoriteDelete: (Favorite) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .fillMaxHeight(0.8f),
            shape = MaterialTheme.shapes.large
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Favorites",
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(favorites) { favorite ->
                        FavoriteItem(
                            favorite = favorite,
                            onSelect = { onFavoriteSelect(favorite) },
                            onDelete = { onFavoriteDelete(favorite) }
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Close")
                    }
                }
            }
        }
    }
}

@Composable
private fun FavoriteItem(
    favorite: Favorite,
    onSelect: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Outlined.Bookmark,
                contentDescription = null,
                modifier = Modifier.padding(end = 12.dp)
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = favorite.title,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = favorite.url,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete favorite")
            }
        }
    }
}

/**
 * Settings dialog for browser preferences
 */
@Composable
fun SettingsDialog(
    settings: BrowserSettings,
    onSettingsUpdate: (BrowserSettings) -> Unit,
    onDismiss: () -> Unit
) {
    var localSettings by remember { mutableStateOf(settings) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .fillMaxHeight(0.8f),
            shape = MaterialTheme.shapes.large
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Settings",
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    item {
                        Text(
                            text = "Privacy",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }

                    item {
                        SwitchPreference(
                            title = "Block Pop-ups",
                            checked = localSettings.blockPopups,
                            onCheckedChange = {
                                localSettings = localSettings.copy(blockPopups = it)
                            }
                        )
                    }

                    item {
                        SwitchPreference(
                            title = "Block Ads",
                            checked = localSettings.blockAds,
                            onCheckedChange = {
                                localSettings = localSettings.copy(blockAds = it)
                            }
                        )
                    }

                    item {
                        SwitchPreference(
                            title = "Block Trackers",
                            checked = localSettings.blockTrackers,
                            onCheckedChange = {
                                localSettings = localSettings.copy(blockTrackers = it)
                            }
                        )
                    }

                    item {
                        Text(
                            text = "Display",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }

                    item {
                        SwitchPreference(
                            title = "Show Images",
                            checked = localSettings.showImages,
                            onCheckedChange = {
                                localSettings = localSettings.copy(showImages = it)
                            }
                        )
                    }

                    item {
                        SwitchPreference(
                            title = "JavaScript",
                            checked = localSettings.enableJavaScript,
                            onCheckedChange = {
                                localSettings = localSettings.copy(enableJavaScript = it)
                            }
                        )
                    }

                    item {
                        SwitchPreference(
                            title = "Desktop Mode",
                            checked = localSettings.useDesktopMode,
                            onCheckedChange = {
                                localSettings = localSettings.copy(useDesktopMode = it)
                            }
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            onSettingsUpdate(localSettings)
                            onDismiss()
                        }
                    ) {
                        Text("Save")
                    }
                }
            }
        }
    }
}

@Composable
private fun SwitchPreference(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title)
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

/**
 * Voice command dialog
 */
@Composable
fun VoiceCommandDialog(
    onCommand: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var commandText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Voice Command") },
        text = {
            Column {
                Text("Speak or type your command:")
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = commandText,
                    onValueChange = { commandText = it },
                    placeholder = { Text("e.g., 'Search for news' or 'Go to YouTube'") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (commandText.isNotBlank()) {
                        onCommand(commandText)
                    }
                }
            ) {
                Text("Execute")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}