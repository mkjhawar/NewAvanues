package com.augmentalis.webavanue.ui.screen.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.augmentalis.webavanue.ui.screen.security.PermissionType
import com.augmentalis.webavanue.domain.model.SitePermission
import com.augmentalis.webavanue.domain.repository.BrowserRepository
import kotlinx.coroutines.launch

/**
 * SitePermissionsScreen - Manage site permissions
 *
 * Allows users to view and revoke permissions (camera, mic, location)
 * that have been granted to websites.
 *
 * Features:
 * - List all sites with granted/denied permissions
 * - Group permissions by domain
 * - Revoke individual permissions
 * - Clear all permissions for a site
 *
 * @param repository BrowserRepository for permission data
 * @param onNavigateBack Callback to navigate back
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SitePermissionsScreen(
    repository: BrowserRepository,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var permissions by remember { mutableStateOf<List<SitePermission>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var showDeleteDialog by remember { mutableStateOf<String?>(null) }

    val scope = rememberCoroutineScope()

    // Load permissions on first composition
    LaunchedEffect(Unit) {
        loadPermissions(repository) { result ->
            when {
                result.isSuccess -> {
                    permissions = result.getOrNull() ?: emptyList()
                    isLoading = false
                }
                result.isFailure -> {
                    error = result.exceptionOrNull()?.message ?: "Failed to load permissions"
                    isLoading = false
                }
            }
        }
    }

    // Group permissions by domain
    val permissionsByDomain = permissions.groupBy { it.domain }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Site Permissions") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
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

                        Button(onClick = {
                            isLoading = true
                            error = null
                            scope.launch {
                                loadPermissions(repository) { result ->
                                    when {
                                        result.isSuccess -> {
                                            permissions = result.getOrNull() ?: emptyList()
                                            isLoading = false
                                        }
                                        result.isFailure -> {
                                            error = result.exceptionOrNull()?.message
                                            isLoading = false
                                        }
                                    }
                                }
                            }
                        }) {
                            Text("Retry")
                        }
                    }
                }

                permissionsByDomain.isEmpty() -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "No site permissions",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Permissions you grant to websites will appear here",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(vertical = 8.dp)
                    ) {
                        permissionsByDomain.forEach { (domain, sitePermissions) ->
                            item {
                                SitePermissionItem(
                                    domain = domain,
                                    permissions = sitePermissions,
                                    onDeletePermission = { permissionType ->
                                        scope.launch {
                                            repository.deleteSitePermission(domain, permissionType)
                                            // Reload permissions
                                            loadPermissions(repository) { result ->
                                                permissions = result.getOrNull() ?: emptyList()
                                            }
                                        }
                                    },
                                    onDeleteAllPermissions = {
                                        showDeleteDialog = domain
                                    }
                                )
                            }

                            item {
                                Divider()
                            }
                        }
                    }
                }
            }
        }

        // Delete confirmation dialog
        showDeleteDialog?.let { domain ->
            AlertDialog(
                onDismissRequest = { showDeleteDialog = null },
                title = { Text("Clear all permissions?") },
                text = { Text("This will remove all permissions granted to $domain") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            scope.launch {
                                repository.deleteAllSitePermissions(domain)
                                showDeleteDialog = null
                                // Reload permissions
                                loadPermissions(repository) { result ->
                                    permissions = result.getOrNull() ?: emptyList()
                                }
                            }
                        },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("Clear")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteDialog = null }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

/**
 * SitePermissionItem - Display permissions for a single domain
 */
@Composable
fun SitePermissionItem(
    domain: String,
    permissions: List<SitePermission>,
    onDeletePermission: (String) -> Unit,
    onDeleteAllPermissions: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        // Domain header with delete all button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = domain,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )

            IconButton(onClick = onDeleteAllPermissions) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete all permissions",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // List individual permissions
        permissions.forEach { permission ->
            PermissionRow(
                permissionType = permission.permissionType,
                granted = permission.granted,
                onDelete = { onDeletePermission(permission.permissionType) }
            )
        }
    }
}

/**
 * PermissionRow - Display a single permission
 */
@Composable
fun PermissionRow(
    permissionType: String,
    granted: Boolean,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Icon for permission type
            Icon(
                imageVector = when (permissionType) {
                    PermissionType.CAMERA.name -> Icons.Default.Videocam
                    PermissionType.MICROPHONE.name -> Icons.Default.Mic
                    PermissionType.LOCATION.name -> Icons.Default.LocationOn
                    else -> Icons.Default.Security
                },
                contentDescription = null,
                tint = if (granted) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.error
                }
            )

            Text(
                text = permissionType.lowercase().replaceFirstChar { it.uppercase() },
                style = MaterialTheme.typography.bodyMedium
            )

            Text(
                text = if (granted) "Allowed" else "Denied",
                style = MaterialTheme.typography.bodySmall,
                color = if (granted) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.error
                }
            )
        }

        IconButton(onClick = onDelete) {
            Icon(
                imageVector = Icons.Default.Clear,
                contentDescription = "Remove permission",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Load all site permissions from repository
 */
private suspend fun loadPermissions(
    repository: BrowserRepository,
    onResult: (Result<List<SitePermission>>) -> Unit
) {
    val result = repository.getAllSitePermissions()
    onResult(result)
}
