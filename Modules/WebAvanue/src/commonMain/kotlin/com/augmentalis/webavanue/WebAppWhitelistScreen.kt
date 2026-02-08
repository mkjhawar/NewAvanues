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
import com.augmentalis.avanueui.theme.AvanueTheme
import com.augmentalis.avanueui.tokens.ShapeTokens
import com.augmentalis.avanueui.tokens.SpacingTokens
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
                            color = AvanueTheme.colors.textSecondary
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
                                tint = AvanueTheme.colors.success
                            )
                        }
                    }
                    IconButton(onClick = { showAddDialog = true }) {
                        Icon(Icons.Default.Add, "Add web app")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = AvanueTheme.colors.surface
                )
            )
        },
        floatingActionButton = {
            if (currentDomain != null && !currentDomainWhitelisted) {
                ExtendedFloatingActionButton(
                    onClick = { showAddCurrentDomainPrompt = true },
                    containerColor = AvanueTheme.colors.surfaceElevated,
                    contentColor = AvanueTheme.colors.textPrimary,
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
                    .padding(SpacingTokens.md),
                shape = RoundedCornerShape(ShapeTokens.md),
                color = AvanueTheme.colors.surfaceElevated
            ) {
                Row(
                    modifier = Modifier.padding(SpacingTokens.md),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = null,
                        tint = AvanueTheme.colors.iconPrimary,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Voice commands for saved web apps load instantly",
                            style = MaterialTheme.typography.bodyMedium,
                            color = AvanueTheme.colors.textPrimary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Commands are cached locally and verified on each visit.",
                            style = MaterialTheme.typography.bodySmall,
                            color = AvanueTheme.colors.textSecondary
                        )
                    }
                }
            }

            if (entries.isEmpty()) {
                // Empty state
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(SpacingTokens.xl),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.WebAsset,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = AvanueTheme.colors.iconDisabled
                        )
                        Spacer(modifier = Modifier.height(SpacingTokens.md))
                        Text(
                            text = "No saved web apps",
                            style = MaterialTheme.typography.titleMedium,
                            color = AvanueTheme.colors.textSecondary
                        )
                        Spacer(modifier = Modifier.height(SpacingTokens.sm))
                        Text(
                            text = "Add frequently visited sites to speed up voice commands",
                            style = MaterialTheme.typography.bodyMedium,
                            color = AvanueTheme.colors.textSecondary
                        )
                    }
                }
            } else {
                // Whitelist entries
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = SpacingTokens.md, vertical = SpacingTokens.sm),
                    verticalArrangement = Arrangement.spacedBy(SpacingTokens.sm)
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
        shape = RoundedCornerShape(ShapeTokens.md),
        colors = CardDefaults.cardColors(
            containerColor = if (isCurrentDomain)
                AvanueTheme.colors.surfaceElevated
            else
                AvanueTheme.colors.surface
        ),
        border = if (isCurrentDomain)
            androidx.compose.foundation.BorderStroke(2.dp, AvanueTheme.colors.iconPrimary)
        else
            null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(SpacingTokens.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Category icon
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(AvanueTheme.colors.surfaceElevated),
                contentAlignment = Alignment.Center
            ) {
                val category = entry.category?.let { cat ->
                    WebAppCategory.entries.find { it.name.equals(cat, ignoreCase = true) }
                } ?: WebAppCategory.OTHER

                Icon(
                    imageVector = category.icon,
                    contentDescription = null,
                    tint = if (entry.isEnabled)
                        AvanueTheme.colors.iconPrimary
                    else
                        AvanueTheme.colors.iconDisabled,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(SpacingTokens.md))

            // Entry details
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = entry.displayName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium,
                        color = if (entry.isEnabled)
                            AvanueTheme.colors.textPrimary
                        else
                            AvanueTheme.colors.textDisabled,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (isCurrentDomain) {
                        Spacer(modifier = Modifier.width(SpacingTokens.sm))
                        Surface(
                            shape = RoundedCornerShape(ShapeTokens.xs),
                            color = AvanueTheme.colors.iconPrimary.copy(alpha = 0.2f)
                        ) {
                            Text(
                                text = "CURRENT",
                                style = MaterialTheme.typography.labelSmall,
                                color = AvanueTheme.colors.iconPrimary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Text(
                    text = entry.domain,
                    style = MaterialTheme.typography.bodySmall,
                    color = AvanueTheme.colors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(SpacingTokens.md)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Mic,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = AvanueTheme.colors.iconSecondary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${entry.commandCount} commands",
                            style = MaterialTheme.typography.labelSmall,
                            color = AvanueTheme.colors.textSecondary
                        )
                    }

                    if (entry.visitCount > 0) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Visibility,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = AvanueTheme.colors.iconSecondary
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${entry.visitCount} visits",
                                style = MaterialTheme.typography.labelSmall,
                                color = AvanueTheme.colors.textSecondary
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
                    checkedThumbColor = AvanueTheme.colors.success,
                    checkedTrackColor = AvanueTheme.colors.success.copy(alpha = 0.3f)
                )
            )

            IconButton(onClick = { showDeleteConfirm = true }) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Remove",
                    tint = AvanueTheme.colors.error
                )
            }
        }
    }

    // Delete confirmation dialog
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            icon = { Icon(Icons.Default.Warning, null, tint = AvanueTheme.colors.warning) },
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
                        contentColor = AvanueTheme.colors.error
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
            shape = RoundedCornerShape(ShapeTokens.lg),
            color = AvanueTheme.colors.surface,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier.padding(SpacingTokens.lg)
            ) {
                Text(
                    text = "Save Web App",
                    style = MaterialTheme.typography.headlineSmall,
                    color = AvanueTheme.colors.textPrimary
                )

                Spacer(modifier = Modifier.height(SpacingTokens.sm))

                Text(
                    text = "Voice commands for this site will be saved for faster loading.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = AvanueTheme.colors.textSecondary
                )

                Spacer(modifier = Modifier.height(SpacingTokens.lg))

                // Domain input
                OutlinedTextField(
                    value = domain,
                    onValueChange = { domain = it },
                    label = { Text("Domain") },
                    placeholder = { Text("example.com") },
                    leadingIcon = { Icon(Icons.Default.Language, null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(ShapeTokens.md)
                )

                Spacer(modifier = Modifier.height(SpacingTokens.md))

                // Display name input
                OutlinedTextField(
                    value = displayName,
                    onValueChange = { displayName = it },
                    label = { Text("Display Name") },
                    placeholder = { Text("My App") },
                    leadingIcon = { Icon(Icons.Default.Label, null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(ShapeTokens.md)
                )

                Spacer(modifier = Modifier.height(SpacingTokens.md))

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
                        shape = RoundedCornerShape(ShapeTokens.md)
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

                Spacer(modifier = Modifier.height(SpacingTokens.lg))

                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(SpacingTokens.sm))
                    Button(
                        onClick = {
                            if (domain.isNotBlank() && displayName.isNotBlank()) {
                                onAdd(domain, displayName, selectedCategory?.name)
                            }
                        },
                        enabled = domain.isNotBlank() && displayName.isNotBlank(),
                        shape = RoundedCornerShape(ShapeTokens.md)
                    ) {
                        Icon(Icons.Default.Save, null)
                        Spacer(modifier = Modifier.width(SpacingTokens.sm))
                        Text("Save")
                    }
                }
            }
        }
    }
}
