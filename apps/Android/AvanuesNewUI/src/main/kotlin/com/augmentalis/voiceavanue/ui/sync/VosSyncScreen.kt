/**
 * VosSyncScreen.kt - Full VOS sync management screen
 *
 * Provides connection status, upload/download/sync actions, progress indicator,
 * and a file list showing all VOS registry entries.
 *
 * Accessed via Settings → System → "Manage VOS Sync" when sync is enabled.
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Created: 2026-02-11
 */
package com.augmentalis.voiceavanue.ui.sync

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.augmentalis.avanueui.components.AvanueButton
import com.augmentalis.avanueui.components.AvanueCard
import com.augmentalis.avanueui.components.settings.SettingsDropdownRow
import com.augmentalis.avanueui.components.settings.SettingsGroupCard
import com.augmentalis.avanueui.components.settings.SettingsSwitchRow
import com.augmentalis.avanueui.theme.AvanueTheme
import com.augmentalis.avanueui.tokens.SpacingTokens
import com.augmentalis.database.dto.VosFileRegistryDTO
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VosSyncScreen(
    onNavigateBack: () -> Unit,
    viewModel: VosSyncViewModel = hiltViewModel()
) {
    val syncStatus by viewModel.syncStatus.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val registryFiles by viewModel.registryFiles.collectAsState()
    val actionMessage by viewModel.actionMessage.collectAsState()
    val pendingSuggestions by viewModel.pendingSuggestionCount.collectAsState()

    val backgroundGradient = Brush.verticalGradient(
        colors = listOf(
            AvanueTheme.colors.background,
            AvanueTheme.colors.surface.copy(alpha = 0.6f),
            AvanueTheme.colors.background
        )
    )

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "VOS Sync",
                        color = AvanueTheme.colors.textPrimary
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.semantics { contentDescription = "Voice: click Back" }
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = AvanueTheme.colors.textPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundGradient)
                .padding(padding)
                .padding(horizontal = SpacingTokens.md),
            verticalArrangement = Arrangement.spacedBy(SpacingTokens.sm)
        ) {
            // Connection status card
            item {
                ConnectionStatusCard(
                    host = settings.vosSftpHost,
                    port = settings.vosSftpPort,
                    isConnected = syncStatus.isConnected,
                    isSyncing = syncStatus.isSyncing,
                    lastSyncTime = settings.vosLastSyncTime
                )
            }

            // Progress indicator
            if (syncStatus.isSyncing && syncStatus.progress != null) {
                item {
                    val progress = syncStatus.progress!!
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = AvanueTheme.colors.surface
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(SpacingTokens.md)
                        ) {
                            Text(
                                text = "${progress.currentIndex}/${progress.totalFiles}: ${progress.currentFile}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = AvanueTheme.colors.textPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            LinearProgressIndicator(
                                progress = { progress.fraction },
                                modifier = Modifier.fillMaxWidth(),
                                color = AvanueTheme.colors.primary
                            )
                        }
                    }
                }
            }

            // Action message
            if (actionMessage != null) {
                item {
                    val isError = actionMessage!!.contains("failed", ignoreCase = true) ||
                            actionMessage!!.contains("error", ignoreCase = true)
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isError) {
                                AvanueTheme.colors.error.copy(alpha = 0.15f)
                            } else {
                                AvanueTheme.colors.success.copy(alpha = 0.15f)
                            }
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(SpacingTokens.md),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (isError) Icons.Default.Error else Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = if (isError) AvanueTheme.colors.error else AvanueTheme.colors.success,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = actionMessage!!,
                                style = MaterialTheme.typography.bodyMedium,
                                color = AvanueTheme.colors.textPrimary
                            )
                        }
                    }
                }
            }

            // Action buttons
            item {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SyncActionButton(
                            text = "Test",
                            icon = Icons.Default.NetworkCheck,
                            enabled = !syncStatus.isSyncing && settings.vosSftpHost.isNotBlank(),
                            onClick = { viewModel.testConnection() },
                            modifier = Modifier.weight(1f).semantics { contentDescription = "Voice: click Test Connection" }
                        )
                        SyncActionButton(
                            text = "Upload",
                            icon = Icons.Default.CloudUpload,
                            enabled = !syncStatus.isSyncing && settings.vosSftpHost.isNotBlank(),
                            onClick = { viewModel.uploadAll() },
                            modifier = Modifier.weight(1f).semantics { contentDescription = "Voice: click Upload VOS Files" }
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SyncActionButton(
                            text = "Download",
                            icon = Icons.Default.CloudDownload,
                            enabled = !syncStatus.isSyncing && settings.vosSftpHost.isNotBlank(),
                            onClick = { viewModel.downloadAll() },
                            modifier = Modifier.weight(1f).semantics { contentDescription = "Voice: click Download VOS Files" }
                        )
                        SyncActionButton(
                            text = "Full Sync",
                            icon = Icons.Default.Sync,
                            enabled = !syncStatus.isSyncing && settings.vosSftpHost.isNotBlank(),
                            onClick = { viewModel.fullSync() },
                            modifier = Modifier.weight(1f).semantics { contentDescription = "Voice: click Full Sync" }
                        )
                    }
                }
            }

            // Auto-sync section
            item {
                SettingsGroupCard {
                    SettingsSwitchRow(
                        title = "Auto Sync",
                        subtitle = if (settings.vosAutoSyncEnabled)
                            "Every ${settings.vosSyncIntervalHours}h when connected"
                        else
                            "Periodic background sync",
                        icon = Icons.Default.Schedule,
                        checked = settings.vosAutoSyncEnabled,
                        onCheckedChange = { viewModel.updateAutoSync(it) }
                    )
                    if (settings.vosAutoSyncEnabled) {
                        SettingsDropdownRow(
                            title = "Sync Interval",
                            subtitle = "How often to sync in background",
                            icon = Icons.Default.Schedule,
                            selected = settings.vosSyncIntervalHours,
                            options = listOf(1, 2, 4, 8, 12, 24),
                            optionLabel = { "${it}h" },
                            onSelected = { viewModel.updateSyncInterval(it) }
                        )
                    }
                }
            }

            // File registry list
            item {
                Spacer(modifier = Modifier.height(SpacingTokens.sm))
                Text(
                    text = "REGISTRY (${registryFiles.size} files)",
                    style = MaterialTheme.typography.labelMedium,
                    color = AvanueTheme.colors.primary,
                    fontWeight = FontWeight.Bold
                )
            }

            if (registryFiles.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = AvanueTheme.colors.surface
                        )
                    ) {
                        Text(
                            text = "No VOS files in registry",
                            style = MaterialTheme.typography.bodyMedium,
                            color = AvanueTheme.colors.textSecondary,
                            modifier = Modifier.padding(SpacingTokens.md)
                        )
                    }
                }
            }

            items(registryFiles, key = { it.id }) { file ->
                RegistryFileItem(file)
            }

            // Suggestions section
            item {
                Spacer(modifier = Modifier.height(SpacingTokens.md))
                Text(
                    text = "SUGGESTIONS",
                    style = MaterialTheme.typography.labelMedium,
                    color = AvanueTheme.colors.primary,
                    fontWeight = FontWeight.Bold
                )
            }

            item {
                AvanueCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(SpacingTokens.md),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lightbulb,
                            contentDescription = null,
                            tint = AvanueTheme.colors.warning,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "$pendingSuggestions pending",
                                style = MaterialTheme.typography.bodyMedium,
                                color = AvanueTheme.colors.textPrimary
                            )
                            Text(
                                text = "User-submitted phrase suggestions",
                                style = MaterialTheme.typography.bodySmall,
                                color = AvanueTheme.colors.textSecondary
                            )
                        }
                    }
                }
            }

            if (pendingSuggestions > 0) {
                item {
                    AvanueButton(
                        onClick = { viewModel.exportSuggestions() },
                        modifier = Modifier.fillMaxWidth().semantics { contentDescription = "Voice: click Export Suggestions" }
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudUpload,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Export Suggestions")
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(SpacingTokens.lg)) }
        }
    }
}

@Composable
private fun ConnectionStatusCard(
    host: String,
    port: Int,
    isConnected: Boolean,
    isSyncing: Boolean,
    lastSyncTime: Long?
) {
    AvanueCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(SpacingTokens.md)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(
                            when {
                                isSyncing -> AvanueTheme.colors.warning
                                isConnected -> AvanueTheme.colors.success
                                else -> AvanueTheme.colors.textDisabled
                            }
                        )
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = when {
                        isSyncing -> "Syncing..."
                        isConnected -> "Connected"
                        else -> "Disconnected"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    color = AvanueTheme.colors.textPrimary
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = if (host.isNotBlank()) "$host:$port" else "No server configured",
                style = MaterialTheme.typography.bodySmall,
                color = AvanueTheme.colors.textSecondary
            )
            if (lastSyncTime != null) {
                Text(
                    text = "Last sync: ${formatTimestamp(lastSyncTime)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = AvanueTheme.colors.textSecondary
                )
            }
        }
    }
}

@Composable
private fun SyncActionButton(
    text: String,
    icon: ImageVector,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    AvanueButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(text)
    }
}

@Composable
private fun RegistryFileItem(file: VosFileRegistryDTO) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = AvanueTheme.colors.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(SpacingTokens.sm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // File type badge
            val typeColor = if (file.fileType == "app") {
                AvanueTheme.colors.primary
            } else {
                AvanueTheme.colors.info
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(typeColor.copy(alpha = 0.15f))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = file.fileType.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = typeColor,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = file.fileName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = AvanueTheme.colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${file.commandCount} commands | v${file.version} | ${file.source}",
                    style = MaterialTheme.typography.bodySmall,
                    color = AvanueTheme.colors.textSecondary
                )
            }

            // Upload status
            val uploadIcon = when {
                file.uploadedAt != null -> Icons.Default.CheckCircle
                else -> Icons.Default.CloudUpload
            }
            val uploadColor = when {
                file.uploadedAt != null -> AvanueTheme.colors.success
                else -> AvanueTheme.colors.textDisabled
            }
            Icon(
                imageVector = uploadIcon,
                contentDescription = if (file.uploadedAt != null) "Uploaded" else "Not uploaded",
                tint = uploadColor,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

private fun formatTimestamp(millis: Long): String {
    val sdf = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
    return sdf.format(Date(millis))
}
