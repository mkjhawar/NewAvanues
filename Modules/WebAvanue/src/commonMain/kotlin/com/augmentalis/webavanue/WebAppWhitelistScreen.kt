package com.augmentalis.webavanue

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.augmentalis.webavanue.OceanDesignTokens
import kotlinx.coroutines.launch

/**
 * Web App Whitelist data class for UI
 */
data class WebAppWhitelistEntry(
    val id: Long = 0,
    val domain: String,
    val displayName: String,
    val category: String? = null,
    val isEnabled: Boolean = true,
    val commandCount: Int = 0,
    val lastVisited: Long? = null,
    val visitCount: Int = 0
)

/**
 * Category options for web apps
 */
enum class WebAppCategory(val displayName: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    EMAIL("Email", Icons.Default.Email),
    SOCIAL("Social", Icons.Default.People),
    PRODUCTIVITY("Productivity", Icons.Default.Work),
    SHOPPING("Shopping", Icons.Default.ShoppingCart),
    NEWS("News", Icons.Default.Newspaper),
    ENTERTAINMENT("Entertainment", Icons.Default.Movie),
    FINANCE("Finance", Icons.Default.AccountBalance),
    OTHER("Other", Icons.Default.MoreHoriz)
}

/**
 * Web App Whitelist Management Screen
 *
 * Allows users to manage which websites have their voice commands
 * persisted to the database for faster loading.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WebAppWhitelistScreen(
    entries: List<WebAppWhitelistEntry>,
    currentDomain: String?,
    onAddEntry: (domain: String, displayName: String, category: String?) -> Unit,
    onRemoveEntry: (Long) -> Unit,
    onToggleEntry: (Long, Boolean) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showAddDialog by rememberSaveable { mutableStateOf(false) }
    var showAddCurrentDomainPrompt by rememberSaveable { mutableStateOf(false) }

    // Check if current domain is already whitelisted
    val currentDomainWhitelisted = currentDomain != null &&
        entries.any { it.domain.equals(currentDomain, ignoreCase = true) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Saved Web Apps")
                        Text(
                            text = "${entries.count { it.isEnabled }} active",
                            style = MaterialTheme.typography.bodySmall,
                            color = OceanDesignTokens.Text.secondary
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    // Quick add current domain button
                    if (currentDomain != null && !currentDomainWhitelisted) {
                        IconButton(onClick = { showAddCurrentDomainPrompt = true }) {
                            Icon(
                                Icons.Default.AddCircle,
                                contentDescription = "Save current site",
                                tint = OceanDesignTokens.Icon.success
                            )
                        }
                    }
                    IconButton(onClick = { showAddDialog = true }) {
                        Icon(Icons.Default.Add, "Add web app")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = OceanDesignTokens.Surface.default
                )
            )
        },
        floatingActionButton = {
            if (currentDomain != null && !currentDomainWhitelisted) {
                ExtendedFloatingActionButton(
                    onClick = { showAddCurrentDomainPrompt = true },
                    containerColor = OceanDesignTokens.Surface.elevated,
                    contentColor = OceanDesignTokens.Text.primary,
                    icon = { Icon(Icons.Default.Add, null) },
                    text = { Text("Save This Site") }
                )
            }
        },
        modifier = modifier
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Info banner
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(12.dp),
                color = OceanDesignTokens.Surface.elevated
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = null,
                        tint = OceanDesignTokens.Icon.primary,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Voice commands for saved web apps load instantly",
                            style = MaterialTheme.typography.bodyMedium,
                            color = OceanDesignTokens.Text.primary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Commands are cached locally and verified on each visit.",
                            style = MaterialTheme.typography.bodySmall,
                            color = OceanDesignTokens.Text.secondary
                        )
                    }
                }
            }

            if (entries.isEmpty()) {
                // Empty state
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.WebAsset,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = OceanDesignTokens.Icon.disabled
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No saved web apps",
                            style = MaterialTheme.typography.titleMedium,
                            color = OceanDesignTokens.Text.secondary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Add frequently visited sites to speed up voice commands",
                            style = MaterialTheme.typography.bodyMedium,
                            color = OceanDesignTokens.Text.secondary
                        )
                    }
                }
            } else {
                // Whitelist entries
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(entries, key = { it.id }) { entry ->
                        WhitelistEntryCard(
                            entry = entry,
                            isCurrentDomain = currentDomain?.equals(entry.domain, ignoreCase = true) == true,
                            onToggle = { enabled -> onToggleEntry(entry.id, enabled) },
                            onDelete = { onRemoveEntry(entry.id) }
                        )
                    }
                }
            }
        }
    }

    // Add web app dialog
    if (showAddDialog) {
        AddWebAppDialog(
            initialDomain = "",
            onAdd = { domain, displayName, category ->
                onAddEntry(domain, displayName, category)
                showAddDialog = false
            },
            onDismiss = { showAddDialog = false }
        )
    }

    // Quick add current domain dialog
    if (showAddCurrentDomainPrompt && currentDomain != null) {
        AddWebAppDialog(
            initialDomain = currentDomain,
            onAdd = { domain, displayName, category ->
                onAddEntry(domain, displayName, category)
                showAddCurrentDomainPrompt = false
            },
            onDismiss = { showAddCurrentDomainPrompt = false }
        )
    }
}

/**
 * Individual whitelist entry card
 */
@Composable
private fun WhitelistEntryCard(
    entry: WebAppWhitelistEntry,
    isCurrentDomain: Boolean,
    onToggle: (Boolean) -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isCurrentDomain)
                OceanDesignTokens.Surface.elevated
            else
                OceanDesignTokens.Surface.default
        ),
        border = if (isCurrentDomain)
            androidx.compose.foundation.BorderStroke(2.dp, OceanDesignTokens.Icon.primary)
        else
            null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Category icon
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(OceanDesignTokens.Surface.elevated),
                contentAlignment = Alignment.Center
            ) {
                val category = entry.category?.let { cat ->
                    WebAppCategory.entries.find { it.name.equals(cat, ignoreCase = true) }
                } ?: WebAppCategory.OTHER

                Icon(
                    imageVector = category.icon,
                    contentDescription = null,
                    tint = if (entry.isEnabled)
                        OceanDesignTokens.Icon.primary
                    else
                        OceanDesignTokens.Icon.disabled,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Entry details
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = entry.displayName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium,
                        color = if (entry.isEnabled)
                            OceanDesignTokens.Text.primary
                        else
                            OceanDesignTokens.Text.disabled,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (isCurrentDomain) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = OceanDesignTokens.Icon.primary.copy(alpha = 0.2f)
                        ) {
                            Text(
                                text = "CURRENT",
                                style = MaterialTheme.typography.labelSmall,
                                color = OceanDesignTokens.Icon.primary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Text(
                    text = entry.domain,
                    style = MaterialTheme.typography.bodySmall,
                    color = OceanDesignTokens.Text.secondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Mic,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = OceanDesignTokens.Icon.secondary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${entry.commandCount} commands",
                            style = MaterialTheme.typography.labelSmall,
                            color = OceanDesignTokens.Text.secondary
                        )
                    }

                    if (entry.visitCount > 0) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Visibility,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = OceanDesignTokens.Icon.secondary
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${entry.visitCount} visits",
                                style = MaterialTheme.typography.labelSmall,
                                color = OceanDesignTokens.Text.secondary
                            )
                        }
                    }
                }
            }

            // Toggle and delete
            Switch(
                checked = entry.isEnabled,
                onCheckedChange = onToggle,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = OceanDesignTokens.Icon.success,
                    checkedTrackColor = OceanDesignTokens.Icon.success.copy(alpha = 0.3f)
                )
            )

            IconButton(onClick = { showDeleteConfirm = true }) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Remove",
                    tint = OceanDesignTokens.Icon.error
                )
            }
        }
    }

    // Delete confirmation dialog
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            icon = { Icon(Icons.Default.Warning, null, tint = OceanDesignTokens.Icon.warning) },
            title = { Text("Remove ${entry.displayName}?") },
            text = {
                Text("This will delete ${entry.commandCount} saved voice commands for this site. You can add it again later.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete()
                        showDeleteConfirm = false
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = OceanDesignTokens.Icon.error
                    )
                ) {
                    Text("Remove")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

/**
 * Dialog for adding a new web app to whitelist
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddWebAppDialog(
    initialDomain: String,
    onAdd: (domain: String, displayName: String, category: String?) -> Unit,
    onDismiss: () -> Unit
) {
    var domain by rememberSaveable { mutableStateOf(initialDomain) }
    var displayName by rememberSaveable { mutableStateOf("") }
    var selectedCategory by rememberSaveable { mutableStateOf<WebAppCategory?>(null) }
    var showCategoryDropdown by remember { mutableStateOf(false) }

    // Auto-generate display name from domain
    LaunchedEffect(domain) {
        if (displayName.isEmpty() && domain.isNotEmpty()) {
            displayName = domain
                .removePrefix("www.")
                .substringBefore(".")
                .replaceFirstChar { it.uppercase() }
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .widthIn(min = 300.dp, max = 400.dp),
            shape = RoundedCornerShape(16.dp),
            color = OceanDesignTokens.Surface.default,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier.padding(24.dp)
            ) {
                Text(
                    text = "Save Web App",
                    style = MaterialTheme.typography.headlineSmall,
                    color = OceanDesignTokens.Text.primary
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Voice commands for this site will be saved for faster loading.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = OceanDesignTokens.Text.secondary
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Domain input
                OutlinedTextField(
                    value = domain,
                    onValueChange = { domain = it },
                    label = { Text("Domain") },
                    placeholder = { Text("example.com") },
                    leadingIcon = { Icon(Icons.Default.Language, null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Display name input
                OutlinedTextField(
                    value = displayName,
                    onValueChange = { displayName = it },
                    label = { Text("Display Name") },
                    placeholder = { Text("My App") },
                    leadingIcon = { Icon(Icons.Default.Label, null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Category selector
                ExposedDropdownMenuBox(
                    expanded = showCategoryDropdown,
                    onExpandedChange = { showCategoryDropdown = it }
                ) {
                    OutlinedTextField(
                        value = selectedCategory?.displayName ?: "Select category",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Category (optional)") },
                        leadingIcon = {
                            Icon(
                                selectedCategory?.icon ?: Icons.Default.Category,
                                null
                            )
                        },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showCategoryDropdown) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    ExposedDropdownMenu(
                        expanded = showCategoryDropdown,
                        onDismissRequest = { showCategoryDropdown = false }
                    ) {
                        WebAppCategory.entries.forEach { category ->
                            DropdownMenuItem(
                                text = { Text(category.displayName) },
                                leadingIcon = { Icon(category.icon, null) },
                                onClick = {
                                    selectedCategory = category
                                    showCategoryDropdown = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (domain.isNotBlank() && displayName.isNotBlank()) {
                                onAdd(domain, displayName, selectedCategory?.name)
                            }
                        },
                        enabled = domain.isNotBlank() && displayName.isNotBlank(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Save, null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Save")
                    }
                }
            }
        }
    }
}
