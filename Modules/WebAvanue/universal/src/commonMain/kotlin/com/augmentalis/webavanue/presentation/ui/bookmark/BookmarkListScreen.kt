package com.augmentalis.webavanue.ui.screen.bookmark

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.augmentalis.webavanue.domain.model.Favorite
import com.augmentalis.webavanue.domain.model.FavoriteFolder
import com.augmentalis.webavanue.ui.viewmodel.FavoriteViewModel
import kotlinx.coroutines.launch

// Type alias for backward compatibility
typealias BookmarkViewModel = FavoriteViewModel

/**
 * BookmarkListScreen - Main bookmarks screen
 *
 * Features:
 * - Search bookmarks
 * - Filter by folder
 * - Add new bookmarks
 * - Edit/delete existing bookmarks
 * - Navigate to bookmark URL
 *
 * @param viewModel FavoriteViewModel for state and actions
 * @param onNavigateBack Callback to navigate back
 * @param onBookmarkClick Callback when bookmark is clicked (navigate to URL)
 * @param modifier Modifier for customization
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookmarkListScreen(
    viewModel: FavoriteViewModel,
    onNavigateBack: () -> Unit = {},
    onBookmarkClick: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val favorites by viewModel.favorites.collectAsState()
    val folders by viewModel.folders.collectAsState()
    val selectedFolderId by viewModel.selectedFolderId.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()

    // Coroutine scope for async operations (FIX: needed for async addFavorite)
    val scope = rememberCoroutineScope()

    var showAddDialog by remember { mutableStateOf(false) }
    var editingFavorite by remember { mutableStateOf<Favorite?>(null) }
    var showFolderPicker by remember { mutableStateOf(false) }
    var showSearchBar by remember { mutableStateOf(false) }

    // Get folder name for display
    val selectedFolderName = folders.find { it.id == selectedFolderId }?.name

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (showSearchBar) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { viewModel.searchFavorites(it) },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("Search bookmarks...") },
                            singleLine = true
                        )
                    } else {
                        Text(
                            text = selectedFolderName?.let { "Folder: $it" } ?: "Bookmarks"
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (showSearchBar) {
                            showSearchBar = false
                            viewModel.clearSearch()
                        } else if (selectedFolderId != null) {
                            viewModel.filterByFolder(null)
                        } else {
                            onNavigateBack()
                        }
                    }) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    if (!showSearchBar) {
                        IconButton(onClick = { showSearchBar = true }) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search"
                            )
                        }

                        IconButton(onClick = { showFolderPicker = true }) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = "Folders"
                            )
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add Bookmark"
                )
            }
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

                        Button(onClick = { viewModel.loadFavorites() }) {
                            Text("Retry")
                        }
                    }
                }

                favorites.isEmpty() -> {
                    EmptyBookmarksState(
                        onAddBookmark = { showAddDialog = true },
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(vertical = 8.dp)
                    ) {
                        items(favorites, key = { it.id }) { favorite ->
                            val folderName = folders.find { it.id == favorite.folderId }?.name
                            BookmarkItem(
                                bookmark = favorite,
                                folderName = folderName,
                                onClick = { onBookmarkClick(favorite.url) },
                                onEdit = { editingFavorite = favorite },
                                onDelete = { viewModel.removeFavorite(favorite.id) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    // Add/Edit Bookmark Dialog
    if (showAddDialog || editingFavorite != null) {
        val initialFolderName = editingFavorite?.folderId?.let { fid ->
            folders.find { it.id == fid }?.name
        }
        AddBookmarkDialog(
            bookmark = editingFavorite,
            folders = folders.map { it.name },
            initialFolderName = initialFolderName,
            onDismiss = {
                showAddDialog = false
                editingFavorite = null
            },
            onSave = { url, title, folderName ->
                val folderId = folders.find { it.name == folderName }?.id
                if (editingFavorite != null) {
                    val updated = editingFavorite!!.copy(url = url, title = title, folderId = folderId)
                    viewModel.updateFavorite(updated)
                } else {
                    // FIX: Use async addFavorite with duplicate prevention
                    scope.launch {
                        viewModel.addFavorite(url = url, title = title, folderId = folderId)
                    }
                }
                showAddDialog = false
                editingFavorite = null
            }
        )
    }

    // Folder Picker Dialog
    if (showFolderPicker) {
        FolderPickerDialog(
            folders = folders,
            selectedFolderId = selectedFolderId,
            onDismiss = { showFolderPicker = false },
            onFolderSelected = { folderId ->
                viewModel.filterByFolder(folderId)
                showFolderPicker = false
            }
        )
    }
}

/**
 * EmptyBookmarksState - Shown when no bookmarks exist
 */
@Composable
fun EmptyBookmarksState(
    onAddBookmark: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "No bookmarks yet",
            style = MaterialTheme.typography.headlineMedium
        )

        Text(
            text = "Save your favorite pages for quick access",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Button(onClick = onAddBookmark) {
            Text("Add Bookmark")
        }
    }
}

/**
 * FolderPickerDialog - Dialog for selecting bookmark folder
 */
@Composable
fun FolderPickerDialog(
    folders: List<FavoriteFolder>,
    selectedFolderId: String?,
    onDismiss: () -> Unit,
    onFolderSelected: (String?) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Folder") },
        text = {
            LazyColumn {
                item {
                    ListItem(
                        headlineContent = { Text("All Bookmarks") },
                        modifier = Modifier.fillMaxWidth(),
                        leadingContent = {
                            RadioButton(
                                selected = selectedFolderId == null,
                                onClick = { onFolderSelected(null) }
                            )
                        }
                    )
                }

                items(folders) { folder ->
                    ListItem(
                        headlineContent = { Text(folder.name) },
                        modifier = Modifier.fillMaxWidth(),
                        leadingContent = {
                            RadioButton(
                                selected = selectedFolderId == folder.id,
                                onClick = { onFolderSelected(folder.id) }
                            )
                        }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}
