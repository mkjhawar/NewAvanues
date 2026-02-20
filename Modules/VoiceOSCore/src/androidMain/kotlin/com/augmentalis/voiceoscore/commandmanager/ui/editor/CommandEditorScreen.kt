/**
 * CommandEditorScreen.kt - Main command editor UI (Jetpack Compose + Material 3)
 *
 * Features:
 * - Command list with search/filter
 * - Navigation to wizard, testing, template browser
 * - Import/export commands
 * - Material Design 3 styling
 */

package com.augmentalis.voiceoscore.commandmanager.ui.editor

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.LibraryBooks
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.augmentalis.avanueui.theme.AvanueTheme
import com.augmentalis.voiceoscore.commandmanager.registry.VoiceCommand

/**
 * Main command editor screen
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommandEditorScreen(
    viewModel: CommandEditorViewModel = viewModel(),
    onNavigateToWizard: () -> Unit = {},
    onNavigateToTemplates: () -> Unit = {},
    onNavigateToTesting: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    var showMenu by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var showImportDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Command Editor") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = AvanueTheme.colors.primaryContainer,
                    titleContentColor = AvanueTheme.colors.onPrimaryContainer
                ),
                actions = {
                    // Search
                    IconButton(onClick = { /* Show search */ }) {
                        Icon(Icons.Default.Search, "Search")
                    }

                    // More menu
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, "More options")
                    }

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Import Commands") },
                            onClick = {
                                showImportDialog = true
                                showMenu = false
                            },
                            leadingIcon = { Icon(Icons.Default.Upload, null) }
                        )
                        DropdownMenuItem(
                            text = { Text("Export Commands") },
                            onClick = {
                                viewModel.exportCommands(context)
                                showMenu = false
                            },
                            leadingIcon = { Icon(Icons.Default.Download, null) }
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    viewModel.startWizard()
                    onNavigateToWizard()
                },
                containerColor = AvanueTheme.colors.primaryContainer
            ) {
                Icon(Icons.Default.Add, "Create command")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Quick action buttons
            QuickActionButtons(
                onCreateCommand = {
                    viewModel.startWizard()
                    onNavigateToWizard()
                },
                onBrowseTemplates = onNavigateToTemplates,
                onTestCommands = onNavigateToTesting
            )

            HorizontalDivider()

            // Search bar
            SearchBar(
                query = searchQuery,
                onQueryChange = {
                    searchQuery = it
                    viewModel.searchCommands(it)
                }
            )

            // Error/Success messages
            uiState.error?.let { error ->
                ErrorMessage(
                    message = error,
                    onDismiss = { viewModel.clearError() }
                )
            }

            uiState.successMessage?.let { message ->
                SuccessMessage(
                    message = message,
                    onDismiss = { viewModel.clearSuccessMessage() }
                )
            }

            // Command list
            if (uiState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                CommandList(
                    commands = uiState.commands,
                    onDeleteCommand = { viewModel.unregisterCommand(it.id) },
                    onEditCommand = { /* TODO */ }
                )
            }
        }
    }
}

/**
 * Quick action buttons row
 */
@Composable
private fun QuickActionButtons(
    onCreateCommand: () -> Unit,
    onBrowseTemplates: () -> Unit,
    onTestCommands: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Button(
            onClick = onCreateCommand,
            modifier = Modifier.weight(1f)
        ) {
            Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(4.dp))
            Text("Create")
        }

        OutlinedButton(
            onClick = onBrowseTemplates,
            modifier = Modifier.weight(1f)
        ) {
            Icon(Icons.AutoMirrored.Filled.LibraryBooks, null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(4.dp))
            Text("Templates")
        }

        OutlinedButton(
            onClick = onTestCommands,
            modifier = Modifier.weight(1f)
        ) {
            Icon(Icons.Default.Science, null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(4.dp))
            Text("Test")
        }
    }
}

/**
 * Search bar component
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        placeholder = { Text("Search commands...") },
        leadingIcon = { Icon(Icons.Default.Search, "Search") },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(Icons.Default.Clear, "Clear")
                }
            }
        },
        singleLine = true
    )
}

/**
 * Command list component
 */
@Composable
private fun CommandList(
    commands: List<VoiceCommand>,
    onDeleteCommand: (VoiceCommand) -> Unit,
    onEditCommand: (VoiceCommand) -> Unit
) {
    if (commands.isEmpty()) {
        EmptyState()
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(commands) { command ->
                CommandCard(
                    command = command,
                    onDelete = { onDeleteCommand(command) },
                    onEdit = { onEditCommand(command) }
                )
            }
        }
    }
}

/**
 * Command card component
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CommandCard(
    command: VoiceCommand,
    onDelete: () -> Unit,
    onEdit: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = command.phrases.firstOrNull() ?: command.id,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "ID: ${command.id}",
                        style = MaterialTheme.typography.bodySmall,
                        color = AvanueTheme.colors.textSecondary
                    )
                }

                IconButton(onClick = { showMenu = true }) {
                    Icon(Icons.Default.MoreVert, "More options")
                }

                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Edit") },
                        onClick = {
                            onEdit()
                            showMenu = false
                        },
                        leadingIcon = { Icon(Icons.Default.Edit, null) }
                    )
                    DropdownMenuItem(
                        text = { Text("Delete") },
                        onClick = {
                            onDelete()
                            showMenu = false
                        },
                        leadingIcon = { Icon(Icons.Default.Delete, null) }
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // Phrases
            if (command.phrases.size > 1) {
                Text(
                    text = "Phrases: ${command.phrases.joinToString(", ")}",
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(Modifier.height(4.dp))

            // Metadata chips
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AssistChip(
                    onClick = { },
                    label = { Text("Priority: ${command.priority}") }
                )
                AssistChip(
                    onClick = { },
                    label = { Text(command.actionType.name) }
                )
                AssistChip(
                    onClick = { },
                    label = { Text(command.namespace) }
                )
            }
        }
    }
}

/**
 * Empty state component
 */
@Composable
private fun EmptyState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                Icons.Default.VoiceChat,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = AvanueTheme.colors.textSecondary
            )
            Text(
                text = "No commands yet",
                style = MaterialTheme.typography.titleMedium,
                color = AvanueTheme.colors.textSecondary
            )
            Text(
                text = "Create your first voice command",
                style = MaterialTheme.typography.bodyMedium,
                color = AvanueTheme.colors.textSecondary
            )
        }
    }
}

/**
 * Error message component
 */
@Composable
private fun ErrorMessage(
    message: String,
    onDismiss: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = AvanueTheme.colors.errorContainer
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Error,
                contentDescription = null,
                tint = AvanueTheme.colors.onErrorContainer
            )
            Text(
                text = message,
                modifier = Modifier.weight(1f),
                color = AvanueTheme.colors.onErrorContainer
            )
            IconButton(onClick = onDismiss) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Dismiss",
                    tint = AvanueTheme.colors.onErrorContainer
                )
            }
        }
    }
}

/**
 * Success message component
 */
@Composable
private fun SuccessMessage(
    message: String,
    onDismiss: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = AvanueTheme.colors.tertiaryContainer
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.CheckCircle,
                contentDescription = null,
                tint = AvanueTheme.colors.onTertiaryContainer
            )
            Text(
                text = message,
                modifier = Modifier.weight(1f),
                color = AvanueTheme.colors.onTertiaryContainer
            )
            IconButton(onClick = onDismiss) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Dismiss",
                    tint = AvanueTheme.colors.onTertiaryContainer
                )
            }
        }
    }
}
