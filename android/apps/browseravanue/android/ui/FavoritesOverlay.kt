package com.augmentalis.browseravanue.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import com.augmentalis.browseravanue.domain.model.Favorite

/**
 * Favorites overlay
 *
 * Architecture:
 * - Full-screen overlay
 * - List of bookmarked sites
 * - Folders/tags support
 * - Search/filter
 *
 * Layout:
 * ```
 * ┌────────────────────────────┐
 * │  Favorites         [X]     │
 * ├────────────────────────────┤
 * │  [Search...]               │
 * ├────────────────────────────┤
 * │  ★ Google                  │
 * │    google.com         [Del]│
 * │  ★ GitHub                  │
 * │    github.com         [Del]│
 * │  ★ Stack Overflow          │
 * │    stackoverflow.com  [Del]│
 * └────────────────────────────┘
 * ```
 *
 * Features:
 * - Scrollable favorites list
 * - Search/filter by name or URL
 * - Delete button per favorite
 * - Folder grouping
 * - Tag filtering
 * - Empty state
 *
 * Usage:
 * ```
 * if (showFavorites) {
 *     FavoritesOverlay(
 *         favorites = favorites,
 *         onFavoriteClick = { ... },
 *         onDismiss = { ... }
 *     )
 * }
 * ```
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoritesOverlay(
    favorites: List<Favorite>,
    onFavoriteClick: (Favorite) -> Unit,
    onFavoriteDelete: (Favorite) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }

    // Filter favorites by search query
    val filteredFavorites = remember(favorites, searchQuery) {
        if (searchQuery.isBlank()) {
            favorites
        } else {
            favorites.filter { favorite ->
                favorite.title.contains(searchQuery, ignoreCase = true) ||
                favorite.url.contains(searchQuery, ignoreCase = true) ||
                favorite.tags.any { it.contains(searchQuery, ignoreCase = true) }
            }
        }
    }

    // Group by folder
    val groupedFavorites = remember(filteredFavorites) {
        filteredFavorites.groupBy { it.folder ?: "Unsorted" }
    }

    MagicSurface(
        modifier = modifier.fillMaxSize()
    ) {
        Scaffold(
            topBar = {
                Column {
                    MagicTopBar(
                        title = "Favorites (${favorites.size})",
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
                        }
                    )

                    // Search bar
                    MagicSearchField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        onSearch = { /* Search happens on every change */ },
                        placeholder = "Search favorites...",
                        modifier = Modifier.padding(MagicSpacing.md)
                    )
                }
            }
        ) { paddingValues ->
            if (favorites.isEmpty()) {
                // Empty state
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(MagicSpacing.md)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )

                        Text(
                            text = "No favorites yet",
                            style = MaterialTheme.typography.headlineMedium
                        )

                        Text(
                            text = "Bookmark your favorite sites",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else if (filteredFavorites.isEmpty()) {
                // No search results
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(MagicSpacing.md)
                    ) {
                        Text(
                            text = "No results found",
                            style = MaterialTheme.typography.headlineMedium
                        )

                        Text(
                            text = "Try a different search term",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                // Favorites list (grouped by folder)
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentPadding = PaddingValues(MagicSpacing.md),
                    verticalArrangement = Arrangement.spacedBy(MagicSpacing.md)
                ) {
                    groupedFavorites.forEach { (folder, favoritesInFolder) ->
                        // Folder header
                        item(key = "folder_$folder") {
                            Text(
                                text = folder,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(vertical = MagicSpacing.sm)
                            )
                        }

                        // Favorites in folder
                        items(
                            items = favoritesInFolder,
                            key = { it.id }
                        ) { favorite ->
                            FavoriteCard(
                                favorite = favorite,
                                onClick = { onFavoriteClick(favorite) },
                                onDelete = { onFavoriteDelete(favorite) }
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Individual favorite card
 */
@Composable
private fun FavoriteCard(
    favorite: Favorite,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    MagicCard(
        modifier = modifier.fillMaxWidth(),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Favorite info
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(MagicSpacing.sm),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Star icon
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )

                // Title and URL
                Column {
                    Text(
                        text = favorite.title,
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    MagicSpacer(SpacingSize.SMALL)

                    Text(
                        text = favorite.url,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Tags
                    if (favorite.tags.isNotEmpty()) {
                        MagicSpacer(SpacingSize.SMALL)
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(MagicSpacing.xs)
                        ) {
                            favorite.tags.take(3).forEach { tag ->
                                MagicChip(
                                    text = tag,
                                    onClick = {},
                                    selected = false
                                )
                            }

                            if (favorite.tags.size > 3) {
                                Text(
                                    text = "+${favorite.tags.size - 3}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // Delete button
            MagicIconButton(
                icon = {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete favorite"
                    )
                },
                onClick = onDelete
            )
        }
    }
}
