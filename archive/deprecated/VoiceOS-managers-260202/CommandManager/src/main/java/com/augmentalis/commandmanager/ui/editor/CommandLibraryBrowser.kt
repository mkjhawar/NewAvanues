/**
 * CommandLibraryBrowser.kt - Template library browser
 *
 * Features:
 * - Browse 15+ pre-built command templates
 * - Filter by category
 * - Template preview and customization
 * - Import template into wizard
 */

package com.augmentalis.commandmanager.ui.editor

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.LibraryBooks
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

/**
 * Template library browser screen
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommandLibraryBrowser(
    viewModel: CommandEditorViewModel = viewModel(),
    onNavigateBack: () -> Unit = {},
    onApplyTemplate: (CommandTemplate) -> Unit = {}
) {
    val templates by viewModel.templates.collectAsState()

    var selectedCategory by remember { mutableStateOf<TemplateCategory?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedTemplate by remember { mutableStateOf<CommandTemplate?>(null) }
    var showPreviewDialog by remember { mutableStateOf(false) }

    // Filter templates
    val filteredTemplates = remember(templates, selectedCategory, searchQuery) {
        var filtered = templates

        selectedCategory?.let { category ->
            filtered = filtered.filter { it.category == category }
        }

        if (searchQuery.isNotEmpty()) {
            filtered = viewModel.searchTemplates(searchQuery)
        }

        filtered
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Template Library") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Search bar
            SearchSection(
                searchQuery = searchQuery,
                onSearchChange = { searchQuery = it }
            )

            // Category filter chips
            CategoryFilterSection(
                selectedCategory = selectedCategory,
                onCategorySelected = { selectedCategory = it }
            )

            HorizontalDivider()

            // Template grid
            if (filteredTemplates.isEmpty()) {
                EmptyLibraryMessage()
            } else {
                TemplateGrid(
                    templates = filteredTemplates,
                    onTemplateClick = { template ->
                        selectedTemplate = template
                        showPreviewDialog = true
                    }
                )
            }
        }
    }

    // Template preview dialog
    selectedTemplate?.let { template ->
        if (showPreviewDialog) {
            TemplatePreviewDialog(
                template = template,
                onDismiss = { showPreviewDialog = false },
                onApply = {
                    onApplyTemplate(template)
                    showPreviewDialog = false
                },
                onCustomize = {
                    viewModel.applyTemplate(template)
                    showPreviewDialog = false
                }
            )
        }
    }
}

/**
 * Search section
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchSection(
    searchQuery: String,
    onSearchChange: (String) -> Unit
) {
    OutlinedTextField(
        value = searchQuery,
        onValueChange = onSearchChange,
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        placeholder = { Text("Search templates...") },
        leadingIcon = { Icon(Icons.Default.Search, "Search") },
        trailingIcon = {
            if (searchQuery.isNotEmpty()) {
                IconButton(onClick = { onSearchChange("") }) {
                    Icon(Icons.Default.Clear, "Clear")
                }
            }
        },
        singleLine = true
    )
}

/**
 * Category filter section
 */
@Composable
private fun CategoryFilterSection(
    selectedCategory: TemplateCategory?,
    onCategorySelected: (TemplateCategory?) -> Unit
) {
    Column(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Categories",
            style = MaterialTheme.typography.titleSmall
        )

        LazyColumn(
            modifier = Modifier.height(80.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // All categories chip
                    FilterChip(
                        selected = selectedCategory == null,
                        onClick = { onCategorySelected(null) },
                        label = { Text("All") }
                    )

                    // Category chips
                    TemplateCategory.values().forEach { category ->
                        FilterChip(
                            selected = selectedCategory == category,
                            onClick = { onCategorySelected(category) },
                            label = { Text(category.displayName) }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Template grid
 */
@Composable
private fun TemplateGrid(
    templates: List<CommandTemplate>,
    onTemplateClick: (CommandTemplate) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(templates) { template ->
            TemplateCard(
                template = template,
                onClick = { onTemplateClick(template) }
            )
        }
    }
}

/**
 * Template card
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TemplateCard(
    template: CommandTemplate,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = template.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )

                // Category badge
                AssistChip(
                    onClick = { },
                    label = {
                        Text(
                            template.category.displayName,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                )
            }

            Text(
                text = template.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            // Phrases preview
            Text(
                text = "Phrases: ${template.phrases.take(3).joinToString(", ")}${if (template.phrases.size > 3) "..." else ""}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Metadata row
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    getActionTypeIcon(template.actionType),
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = template.actionType.name,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(Modifier.weight(1f))

                Text(
                    text = "Priority: ${template.priority}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Tags
            if (template.tags.isNotEmpty()) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    template.tags.take(3).forEach { tag ->
                        SuggestionChip(
                            onClick = { },
                            label = {
                                Text(
                                    tag,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Template preview dialog
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TemplatePreviewDialog(
    template: CommandTemplate,
    onDismiss: () -> Unit,
    onApply: () -> Unit,
    onCustomize: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(template.name) },
        text = {
            Column(
                modifier = Modifier.verticalScrollableContainer(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Description
                Text(
                    text = template.description,
                    style = MaterialTheme.typography.bodyMedium
                )

                HorizontalDivider()

                // Category and action type
                InfoRow("Category", template.category.displayName)
                InfoRow("Action Type", template.actionType.name)
                InfoRow("Priority", template.priority.toString())
                InfoRow("Namespace", template.namespace)

                HorizontalDivider()

                // Phrases
                Text(
                    text = "Phrases:",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                template.phrases.forEach { phrase ->
                    Text("• $phrase", style = MaterialTheme.typography.bodySmall)
                }

                // Parameters
                if (template.defaultParams.isNotEmpty()) {
                    HorizontalDivider()
                    Text(
                        text = "Default Parameters:",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    template.defaultParams.forEach { (key, value) ->
                        InfoRow(key, value)
                    }
                }

                // Example usage
                template.exampleUsage?.let { example ->
                    HorizontalDivider()
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        )
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "Example:",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = example,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onCustomize) {
                    Text("Customize")
                }
                Button(onClick = onApply) {
                    Text("Apply")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

/**
 * Info row component
 */
@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "$label:",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium
        )
    }
}

/**
 * Empty library message
 */
@Composable
private fun EmptyLibraryMessage() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                Icons.AutoMirrored.Filled.LibraryBooks,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "No templates found",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Try adjusting your search or filters",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Vertical scrollable container modifier
 */
private fun Modifier.verticalScrollableContainer(): Modifier {
    return this.heightIn(max = 500.dp)
}

/**
 * Get icon for action type
 */
private fun getActionTypeIcon(actionType: com.augmentalis.commandmanager.registry.ActionType): androidx.compose.ui.graphics.vector.ImageVector {
    return when (actionType) {
        com.augmentalis.commandmanager.registry.ActionType.LAUNCH_APP -> Icons.Default.Apps
        com.augmentalis.commandmanager.registry.ActionType.NAVIGATE -> Icons.Default.Navigation
        com.augmentalis.commandmanager.registry.ActionType.SYSTEM_COMMAND -> Icons.Default.Settings
        com.augmentalis.commandmanager.registry.ActionType.CUSTOM_ACTION -> Icons.Default.Extension
        com.augmentalis.commandmanager.registry.ActionType.TEXT_EDITING -> Icons.Default.Edit
        com.augmentalis.commandmanager.registry.ActionType.MEDIA_CONTROL -> Icons.Default.MusicNote
        com.augmentalis.commandmanager.registry.ActionType.ACCESSIBILITY -> Icons.Default.Accessibility
    }
}
