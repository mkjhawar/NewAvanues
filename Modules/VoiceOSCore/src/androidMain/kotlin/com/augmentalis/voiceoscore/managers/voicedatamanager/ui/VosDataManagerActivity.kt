/**
 * VosDataManagerActivity.kt - Main UI for VOS Data Manager
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2025-01-02
 * Updated: 2026-01-28 - Migrated to KMP structure
 *
 * Comprehensive data management interface with glassmorphism design
 */
package com.augmentalis.voiceoscore.managers.voicedatamanager.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.augmentalis.datamanager.DataStatistics
import com.augmentalis.datamanager.StorageInfo
import com.augmentalis.datamanager.StorageLevel
import com.augmentalis.database.dto.CommandHistoryDTO
import java.text.SimpleDateFormat
import java.util.*

/**
 * Main Activity for VOS Data Manager
 */
class VosDataManagerActivity : ComponentActivity() {
    
    private val viewModel: VosDataViewModel by viewModels {
        VosDataViewModelFactory(this)
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme()
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFF0A0E27)
                ) {
                    VosDataManagerContent(viewModel)
                }
            }
        }
    }
}

/**
 * Main content composable
 */
@Composable
fun VosDataManagerContent(viewModel: VosDataViewModel) {
    val dataStatistics by viewModel.dataStatistics.observeAsState()
    val storageInfo by viewModel.storageInfo.observeAsState()
    val recentHistory by viewModel.recentHistory.observeAsState(emptyList())
    // Future UI sections: userPreferences and customCommands will be displayed here
    // val userPreferences by viewModel.userPreferences.observeAsState(emptyList())
    // val customCommands by viewModel.customCommands.observeAsState(emptyList())
    val isLoading by viewModel.isLoading.observeAsState(false)
    val errorMessage by viewModel.errorMessage.observeAsState()
    val successMessage by viewModel.successMessage.observeAsState()
    val operationProgress by viewModel.operationProgress.observeAsState("" to 0f)

    // val coroutineScope = rememberCoroutineScope() // Not currently needed
    var showExportDialog by remember { mutableStateOf(false) }
    var showImportDialog by remember { mutableStateOf(false) }
    var showCleanupDialog by remember { mutableStateOf(false) }
    var showClearDialog by remember { mutableStateOf(false) }
    var showRetentionDialog by remember { mutableStateOf(false) }
    
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        item {
            HeaderSection()
        }
        
        // Storage Overview
        item {
            storageInfo?.let { info ->
                StorageOverviewCard(
                    storageInfo = info,
                    onRefresh = { viewModel.refreshStorageInfo() }
                )
            }
        }
        
        // Data Statistics
        item {
            dataStatistics?.let { stats ->
                DataStatisticsCard(
                    statistics = stats,
                    onRefresh = { viewModel.refreshStatistics() }
                )
            }
        }
        
        // Quick Actions
        item {
            QuickActionsCard(
                onExport = { showExportDialog = true },
                onImport = { showImportDialog = true },
                onCleanup = { showCleanupDialog = true },
                onClear = { showClearDialog = true },
                isLoading = isLoading
            )
        }
        
        // Data Breakdown
        item {
            dataStatistics?.let { stats ->
                DataBreakdownCard(
                    breakdown = stats.dataBreakdown,
                    onCategoryClick = { _ ->
                        // Handle category selection
                    }
                )
            }
        }
        
        // Recent History
        item {
            RecentHistoryCard(
                history = recentHistory,
                onViewAll = {
                    // Navigate to history view
                }
            )
        }
        
        // Settings
        item {
            dataStatistics?.let { stats ->
                RetentionSettingsCard(
                    retentionDays = stats.retentionDays,
                    autoCleanupEnabled = stats.autoCleanupEnabled,
                    onSettingsClick = { showRetentionDialog = true }
                )
            }
        }
    }
    
    // Operation Progress Overlay
    if (operationProgress.first.isNotEmpty()) {
        OperationProgressOverlay(
            message = operationProgress.first,
            progress = operationProgress.second
        )
    }
    
    // Dialogs
    if (showExportDialog) {
        ExportDataDialog(
            onDismiss = { showExportDialog = false },
            onExport = { selectedTypes ->
                viewModel.exportData(selectedTypes)
                showExportDialog = false
            }
        )
    }
    
    if (showImportDialog) {
        ImportDataDialog(
            onDismiss = { showImportDialog = false },
            onImport = { filePath ->
                viewModel.importData(filePath)
                showImportDialog = false
            }
        )
    }
    
    if (showCleanupDialog) {
        CleanupDialog(
            onDismiss = { showCleanupDialog = false },
            onCleanup = { days ->
                viewModel.performCleanup(days)
                showCleanupDialog = false
            }
        )
    }
    
    if (showClearDialog) {
        ClearDataDialog(
            onDismiss = { showClearDialog = false },
            onClear = {
                viewModel.clearAllData()
                showClearDialog = false
            }
        )
    }
    
    if (showRetentionDialog) {
        dataStatistics?.let { stats ->
            RetentionSettingsDialog(
                currentDays = stats.retentionDays,
                autoCleanupEnabled = stats.autoCleanupEnabled,
                onDismiss = { showRetentionDialog = false },
                onSave = { days, autoCleanup ->
                    viewModel.updateRetentionSettings(days, autoCleanup)
                    showRetentionDialog = false
                }
            )
        }
    }
    
    // Error/Success Messages
    errorMessage?.let { message ->
        ErrorDisplay(
            message = message,
            onDismiss = { viewModel.clearError() }
        )
    }
    
    successMessage?.let { message ->
        SuccessDisplay(
            message = message,
            onDismiss = { viewModel.clearSuccess() }
        )
    }
}

/**
 * Header section
 */
@Composable
fun HeaderSection() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "VOS Data Manager",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = "Manage and monitor your data",
                fontSize = 14.sp,
                color = Color.White.copy(alpha = 0.7f)
            )
        }
        
        Icon(
            imageVector = Icons.Filled.Storage,
            contentDescription = "Data Manager",
            modifier = Modifier.size(48.dp),
            tint = DataColors.TypePreferences
        )
    }
}

/**
 * Storage overview card
 */
@Composable
fun StorageOverviewCard(
    storageInfo: StorageInfo,
    onRefresh: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .glassMorphism(DataGlassConfigs.Storage)
            .testTag("storage_card"),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Storage Overview",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                
                IconButton(onClick = onRefresh) {
                    Icon(
                        imageVector = Icons.Filled.Refresh,
                        contentDescription = "Refresh",
                        tint = Color.White.copy(alpha = 0.7f)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Storage bar
            LinearProgressIndicator(
                progress = { storageInfo.percentUsed / 100f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(24.dp)
                    .clip(RoundedCornerShape(12.dp)),
                color = when (storageInfo.storageLevel) {
                    StorageLevel.NORMAL -> DataColors.StorageNormal
                    StorageLevel.MEDIUM -> DataColors.StorageMedium
                    StorageLevel.HIGH -> DataColors.StorageHigh
                    StorageLevel.CRITICAL -> DataColors.StorageCritical
                },
                trackColor = Color.White.copy(alpha = 0.1f)
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Used: ${formatBytes(storageInfo.databaseSize)}",
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.7f)
                )
                Text(
                    text = "Available: ${formatBytes(storageInfo.availableSpace)}",
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.7f)
                )
            }
            
            Text(
                text = "${String.format("%.1f", storageInfo.percentUsed)}% used",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

/**
 * Data statistics card
 */
@Composable
fun DataStatisticsCard(
    statistics: DataStatistics,
    onRefresh: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .glassMorphism(DataGlassConfigs.Statistics)
            .testTag("statistics_card"),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Data Statistics",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                
                IconButton(onClick = onRefresh) {
                    Icon(
                        imageVector = Icons.Filled.Analytics,
                        contentDescription = "Refresh Stats",
                        tint = Color.White.copy(alpha = 0.7f)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem(
                    label = "Total Records",
                    value = statistics.totalRecords.toString(),
                    icon = Icons.Filled.Storage
                )
                StatItem(
                    label = "Storage Used",
                    value = formatBytes(statistics.storageUsed),
                    icon = Icons.Filled.Memory
                )
                StatItem(
                    label = "Last Sync",
                    value = formatTime(statistics.lastSync),
                    icon = Icons.Filled.Sync
                )
            }
        }
    }
}

/**
 * Quick actions card
 */
@Composable
fun QuickActionsCard(
    onExport: () -> Unit,
    onImport: () -> Unit,
    onCleanup: () -> Unit,
    onClear: () -> Unit,
    isLoading: Boolean
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .glassMorphism(DataGlassConfigs.Actions)
            .testTag("actions_card"),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Quick Actions",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ActionButton(
                    text = "Export",
                    icon = Icons.Filled.Upload,
                    onClick = onExport,
                    enabled = !isLoading,
                    modifier = Modifier.weight(1f),
                    color = DataColors.ActionExport
                )
                ActionButton(
                    text = "Import",
                    icon = Icons.Filled.Download,
                    onClick = onImport,
                    enabled = !isLoading,
                    modifier = Modifier.weight(1f),
                    color = DataColors.ActionImport
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ActionButton(
                    text = "Cleanup",
                    icon = Icons.Filled.CleaningServices,
                    onClick = onCleanup,
                    enabled = !isLoading,
                    modifier = Modifier.weight(1f),
                    color = DataColors.ActionCleanup
                )
                ActionButton(
                    text = "Clear All",
                    icon = Icons.Filled.DeleteForever,
                    onClick = onClear,
                    enabled = !isLoading,
                    modifier = Modifier.weight(1f),
                    color = DataColors.StatusError
                )
            }
        }
    }
}

/**
 * Data breakdown card
 */
@Composable
fun DataBreakdownCard(
    breakdown: Map<String, Int>,
    onCategoryClick: (String) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .glassMorphism(DataGlassConfigs.Primary)
            .testTag("breakdown_card"),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Data Breakdown",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier.height(180.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(breakdown.entries.toList()) { entry ->
                    DataTypeCard(
                        type = entry.key,
                        count = entry.value,
                        onClick = { onCategoryClick(entry.key) }
                    )
                }
            }
        }
    }
}

/**
 * Recent history card
 */
@Composable
fun RecentHistoryCard(
    history: List<CommandHistoryDTO>,
    onViewAll: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .glassMorphism(DataGlassConfigs.History)
            .testTag("history_card"),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Recent Command History",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                
                TextButton(onClick = onViewAll) {
                    Text(
                        text = "View All",
                        color = DataColors.TypeHistory
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            if (history.isEmpty()) {
                Text(
                    text = "No recent commands",
                    fontSize = 14.sp,
                    color = Color.White.copy(alpha = 0.5f),
                    modifier = Modifier.padding(vertical = 16.dp)
                )
            } else {
                val recentItems = history.take(5)
                recentItems.forEachIndexed { index, item ->
                    HistoryItem(item)
                    if (index < recentItems.size - 1) {
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }
    }
}

/**
 * Retention settings card
 */
@Composable
fun RetentionSettingsCard(
    retentionDays: Int,
    autoCleanupEnabled: Boolean,
    onSettingsClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .glassMorphism(DataGlassConfigs.Primary)
            .clickable { onSettingsClick() }
            .testTag("retention_card"),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Data Retention",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "Keep data for $retentionDays days",
                    fontSize = 14.sp,
                    color = Color.White.copy(alpha = 0.7f)
                )
                Text(
                    text = if (autoCleanupEnabled) "Auto-cleanup enabled" else "Auto-cleanup disabled",
                    fontSize = 12.sp,
                    color = if (autoCleanupEnabled) DataColors.StatusActive else Color.White.copy(alpha = 0.5f)
                )
            }
            
            Icon(
                imageVector = Icons.Filled.Settings,
                contentDescription = "Settings",
                tint = Color.White.copy(alpha = 0.7f)
            )
        }
    }
}

// Helper Composables

@Composable
fun StatItem(
    label: String,
    value: String,
    icon: ImageVector
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            modifier = Modifier.size(24.dp),
            tint = DataColors.TypeStatistics
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Text(
            text = label,
            fontSize = 12.sp,
            color = Color.White.copy(alpha = 0.7f)
        )
    }
}

@Composable
fun ActionButton(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    color: Color
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(48.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = color.copy(alpha = 0.2f),
            contentColor = color
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = text,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = text, fontSize = 14.sp)
    }
}

@Composable
fun DataTypeCard(
    type: String,
    count: Int,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = getDataTypeColor(type).copy(alpha = 0.1f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = getDataTypeIcon(type),
                contentDescription = type,
                modifier = Modifier.size(24.dp),
                tint = getDataTypeColor(type)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = count.toString(),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = type,
                fontSize = 10.sp,
                color = Color.White.copy(alpha = 0.7f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun HistoryItem(item: CommandHistoryDTO) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Color.White.copy(alpha = 0.05f),
                RoundedCornerShape(8.dp)
            )
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.processedCommand.take(30) + if (item.processedCommand.length > 30) "..." else "",
                fontSize = 14.sp,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = java.text.SimpleDateFormat("MMM dd, HH:mm", java.util.Locale.getDefault()).format(java.util.Date(item.timestamp)),
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.5f)
            )
        }
        
        Icon(
            imageVector = if (item.success) Icons.Filled.CheckCircle else Icons.Filled.Error,
            contentDescription = "Status",
            modifier = Modifier.size(20.dp),
            tint = if (item.success) DataColors.StatusActive else Color(0xFFF44336)
        )
    }
}

@Composable
fun OperationProgressOverlay(
    message: String,
    progress: Float
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.7f))
            .clickable(enabled = false) { },
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .glassMorphism(DataGlassConfigs.Primary),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.size(64.dp),
                    color = DataColors.TypeStatistics
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = message,
                    fontSize = 16.sp,
                    color = Color.White
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "${(progress * 100).toInt()}%",
                    fontSize = 14.sp,
                    color = Color.White.copy(alpha = 0.7f)
                )
            }
        }
    }
}

// Dialog Composables

@Composable
fun ExportDataDialog(
    onDismiss: () -> Unit,
    onExport: (Set<String>) -> Unit
) {
    val dataTypes = listOf("History", "Preferences", "Commands", "Gestures", "Statistics")
    val selectedTypes = remember { mutableSetOf<String>() }
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .glassMorphism(DataGlassConfigs.Primary),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent)
        ) {
            Column(
                modifier = Modifier.padding(24.dp)
            ) {
                Text(
                    text = "Export Data",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "Select data types to export:",
                    fontSize = 14.sp,
                    color = Color.White.copy(alpha = 0.7f)
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                dataTypes.forEach { type ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = selectedTypes.contains(type),
                            onCheckedChange = { checked ->
                                if (checked) selectedTypes.add(type)
                                else selectedTypes.remove(type)
                            }
                        )
                        Text(
                            text = type,
                            fontSize = 14.sp,
                            color = Color.White,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = Color.White.copy(alpha = 0.7f))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { onExport(selectedTypes) },
                        enabled = selectedTypes.isNotEmpty()
                    ) {
                        Text("Export")
                    }
                }
            }
        }
    }
}

@Composable
fun ImportDataDialog(
    onDismiss: () -> Unit,
    onImport: (String) -> Unit
) {
    var filePath by remember { mutableStateOf("") }
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .glassMorphism(DataGlassConfigs.Primary),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent)
        ) {
            Column(
                modifier = Modifier.padding(24.dp)
            ) {
                Text(
                    text = "Import Data",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "Enter file path or select file:",
                    fontSize = 14.sp,
                    color = Color.White.copy(alpha = 0.7f)
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                OutlinedTextField(
                    value = filePath,
                    onValueChange = { filePath = it },
                    label = { Text("File path") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = Color.White.copy(alpha = 0.7f))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { onImport(filePath) },
                        enabled = filePath.isNotEmpty()
                    ) {
                        Text("Import")
                    }
                }
            }
        }
    }
}

@Composable
fun CleanupDialog(
    onDismiss: () -> Unit,
    onCleanup: (Int) -> Unit
) {
    var days by remember { mutableIntStateOf(30) }
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .glassMorphism(DataGlassConfigs.Primary),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent)
        ) {
            Column(
                modifier = Modifier.padding(24.dp)
            ) {
                Text(
                    text = "Cleanup Old Data",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "Remove data older than:",
                    fontSize = 14.sp,
                    color = Color.White.copy(alpha = 0.7f)
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Slider(
                        value = days.toFloat(),
                        onValueChange = { days = it.toInt() },
                        valueRange = 7f..365f,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "$days days",
                        fontSize = 14.sp,
                        color = Color.White,
                        modifier = Modifier.padding(start = 16.dp)
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = Color.White.copy(alpha = 0.7f))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = { onCleanup(days) }) {
                        Text("Cleanup")
                    }
                }
            }
        }
    }
}

@Composable
fun ClearDataDialog(
    onDismiss: () -> Unit,
    onClear: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .glassMorphism(DataGlassConfigs.Errors),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent)
        ) {
            Column(
                modifier = Modifier.padding(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Warning,
                    contentDescription = "Warning",
                    modifier = Modifier.size(48.dp),
                    tint = DataColors.StatusError
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "Clear All Data",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "This will permanently delete all stored data. This action cannot be undone.",
                    fontSize = 14.sp,
                    color = Color.White.copy(alpha = 0.7f)
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = Color.White.copy(alpha = 0.7f))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = onClear,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = DataColors.StatusError
                        )
                    ) {
                        Text("Clear All")
                    }
                }
            }
        }
    }
}

@Composable
fun RetentionSettingsDialog(
    currentDays: Int,
    autoCleanupEnabled: Boolean,
    onDismiss: () -> Unit,
    onSave: (Int, Boolean) -> Unit
) {
    var days by remember { mutableIntStateOf(currentDays) }
    var autoCleanup by remember { mutableStateOf(autoCleanupEnabled) }
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .glassMorphism(DataGlassConfigs.Primary),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent)
        ) {
            Column(
                modifier = Modifier.padding(24.dp)
            ) {
                Text(
                    text = "Retention Settings",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "Keep data for:",
                    fontSize = 14.sp,
                    color = Color.White.copy(alpha = 0.7f)
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Slider(
                        value = days.toFloat(),
                        onValueChange = { days = it.toInt() },
                        valueRange = 7f..365f,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "$days days",
                        fontSize = 14.sp,
                        color = Color.White,
                        modifier = Modifier.padding(start = 16.dp)
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Auto-cleanup",
                        fontSize = 14.sp,
                        color = Color.White,
                        modifier = Modifier.weight(1f)
                    )
                    Switch(
                        checked = autoCleanup,
                        onCheckedChange = { autoCleanup = it }
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = Color.White.copy(alpha = 0.7f))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = { onSave(days, autoCleanup) }) {
                        Text("Save")
                    }
                }
            }
        }
    }
}

@Composable
fun ErrorDisplay(
    message: String,
    onDismiss: () -> Unit
) {
    Snackbar(
        modifier = Modifier.padding(16.dp),
        action = {
            TextButton(onClick = onDismiss) {
                Text("Dismiss")
            }
        }
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Filled.Error,
                contentDescription = "Error",
                modifier = Modifier.size(20.dp),
                tint = DataColors.StatusError
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = message)
        }
    }
}

@Composable
fun SuccessDisplay(
    message: String,
    onDismiss: () -> Unit
) {
    Snackbar(
        modifier = Modifier.padding(16.dp),
        action = {
            TextButton(onClick = onDismiss) {
                Text("Dismiss")
            }
        }
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Filled.CheckCircle,
                contentDescription = "Success",
                modifier = Modifier.size(20.dp),
                tint = DataColors.StatusActive
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = message)
        }
    }
}

// Helper functions

fun formatBytes(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "${bytes / 1024} KB"
        bytes < 1024 * 1024 * 1024 -> "${bytes / (1024 * 1024)} MB"
        else -> "${bytes / (1024 * 1024 * 1024)} GB"
    }
}

fun formatTime(timestamp: Long): String {
    val diff = System.currentTimeMillis() - timestamp
    return when {
        diff < 60_000 -> "Just now"
        diff < 3_600_000 -> "${diff / 60_000} min ago"
        diff < 86_400_000 -> "${diff / 3_600_000} hours ago"
        else -> "${diff / 86_400_000} days ago"
    }
}

fun formatTimestamp(timestamp: Long): String {
    val sdf = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

fun getDataTypeColor(type: String): Color {
    return when (type) {
        "History" -> DataColors.TypeHistory
        "Preferences" -> DataColors.TypePreferences
        "Commands" -> DataColors.TypeCommands
        "Gestures" -> DataColors.TypeGestures
        "Statistics" -> DataColors.TypeStatistics
        "Profiles" -> DataColors.TypeProfiles
        "Errors" -> DataColors.TypeErrors
        else -> DataColors.TypePreferences
    }
}

fun getDataTypeIcon(type: String): ImageVector {
    return when (type) {
        "History" -> Icons.Filled.History
        "Preferences" -> Icons.Filled.Settings
        "Commands" -> Icons.Filled.SmartButton
        "Gestures" -> Icons.Filled.TouchApp
        "Statistics" -> Icons.Filled.BarChart
        "Profiles" -> Icons.Filled.Person
        "Errors" -> Icons.Filled.BugReport
        else -> Icons.Filled.Folder
    }
}