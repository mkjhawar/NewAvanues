/**
 * DeveloperConsoleScreen.kt - Hidden developer/debug console
 *
 * Accessible via 7-tap on version number in Settings.
 * Shows raw DataStore values, service debug info, RPC port table,
 * database file listing, and build metadata.
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 */

package com.augmentalis.voiceavanue.ui.developer

import android.content.Context
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.augmentalis.avanueui.components.AvanueCard
import com.augmentalis.avanueui.theme.AvanueTheme
import com.augmentalis.voiceavanue.BuildConfig
import androidx.datastore.preferences.core.edit
import com.augmentalis.voiceavanue.data.avanuesDataStore
import com.augmentalis.voiceavanue.service.VoiceAvanueAccessibilityService
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

// =============================================================================
// Data Models
// =============================================================================

data class ServiceDebugInfo(
    val name: String,
    val status: String,
    val isHealthy: Boolean,
    val details: Map<String, String> = emptyMap()
)

data class DatabaseFileInfo(
    val fileName: String,
    val sizeBytes: Long,
    val lastModified: Long
)

data class RpcPortInfo(
    val serviceName: String,
    val port: Int,
    val transport: String
)

// =============================================================================
// ViewModel
// =============================================================================

@HiltViewModel
class DeveloperConsoleViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _rawPreferences = MutableStateFlow<Map<String, String>>(emptyMap())
    val rawPreferences: StateFlow<Map<String, String>> = _rawPreferences

    private val _services = MutableStateFlow<List<ServiceDebugInfo>>(emptyList())
    val services: StateFlow<List<ServiceDebugInfo>> = _services

    val buildInfo: Map<String, String> = buildMap {
        put("Version", BuildConfig.VERSION_NAME)
        put("Version Code", BuildConfig.VERSION_CODE.toString())
        put("Application ID", BuildConfig.APPLICATION_ID)
        put("Build Type", BuildConfig.BUILD_TYPE)
        put("Min SDK", "29")
        put("Target SDK", "34")
        put("Device SDK", Build.VERSION.SDK_INT.toString())
        put("Device", "${Build.MANUFACTURER} ${Build.MODEL}")
        put("Android", "${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
    }

    val rpcPorts: List<RpcPortInfo> = listOf(
        RpcPortInfo("PluginRegistry", 50050, "UDS/TCP"),
        RpcPortInfo("VoiceOS", 50051, "UDS/TCP"),
        RpcPortInfo("AVA", 50052, "UDS/TCP"),
        RpcPortInfo("Cockpit", 50053, "TCP"),
        RpcPortInfo("NLU", 50054, "UDS/TCP"),
        RpcPortInfo("WebAvanue", 50055, "UDS/TCP")
    )

    init {
        observeDataStore()
        refreshServices()
    }

    private fun observeDataStore() {
        viewModelScope.launch {
            context.avanuesDataStore.data.collect { prefs ->
                val map = mutableMapOf<String, String>()
                prefs.asMap().forEach { (key, value) ->
                    map[key.name] = value.toString()
                }
                _rawPreferences.value = map.toSortedMap()
            }
        }
    }

    fun refreshServices() {
        viewModelScope.launch {
            val states = mutableListOf<ServiceDebugInfo>()

            val accessibilityOn = VoiceAvanueAccessibilityService.isEnabled(context)
            states.add(
                ServiceDebugInfo(
                    name = "Accessibility Service",
                    status = if (accessibilityOn) "ENABLED" else "DISABLED",
                    isHealthy = accessibilityOn,
                    details = mapOf("Class" to "VoiceAvanueAccessibilityService")
                )
            )

            val overlayOn = Settings.canDrawOverlays(context)
            states.add(
                ServiceDebugInfo(
                    name = "Overlay Permission",
                    status = if (overlayOn) "GRANTED" else "DENIED",
                    isHealthy = overlayOn
                )
            )

            val notifEnabled = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                val nm = context.getSystemService(Context.NOTIFICATION_SERVICE)
                        as? android.app.NotificationManager
                nm?.areNotificationsEnabled() ?: false
            } else true
            states.add(
                ServiceDebugInfo(
                    name = "Notifications",
                    status = if (notifEnabled) "ENABLED" else "BLOCKED",
                    isHealthy = notifEnabled
                )
            )

            val pm = context.getSystemService(Context.POWER_SERVICE) as? android.os.PowerManager
            val batteryExempt = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                pm?.isIgnoringBatteryOptimizations(context.packageName) ?: false
            } else true
            states.add(
                ServiceDebugInfo(
                    name = "Battery Optimization",
                    status = if (batteryExempt) "EXEMPT" else "OPTIMIZED",
                    isHealthy = batteryExempt,
                    details = mapOf("Desired" to "Exempt (unrestricted)")
                )
            )

            _services.value = states
        }
    }

    fun getDatabaseFiles(): List<DatabaseFileInfo> {
        val dbDir = context.getDatabasePath("_probe").parentFile ?: return emptyList()
        if (!dbDir.exists()) return emptyList()
        return dbDir.listFiles()
            ?.filter { it.isFile && (it.name.endsWith(".db") || it.name.endsWith(".db-shm") || it.name.endsWith(".db-wal")) }
            ?.sortedBy { it.name }
            ?.map { file ->
                DatabaseFileInfo(
                    fileName = file.name,
                    sizeBytes = file.length(),
                    lastModified = file.lastModified()
                )
            } ?: emptyList()
    }

    fun clearDataStore() {
        viewModelScope.launch {
            context.avanuesDataStore.edit { it.clear() }
        }
    }
}

// =============================================================================
// Screen
// =============================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeveloperConsoleScreen(
    onNavigateBack: () -> Unit,
    viewModel: DeveloperConsoleViewModel = hiltViewModel()
) {
    val rawPrefs by viewModel.rawPreferences.collectAsState()
    val services by viewModel.services.collectAsState()
    val dbFiles = remember { viewModel.getDatabaseFiles() }
    var showResetConfirm by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Developer Console", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Debug & diagnostics",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.refreshServices() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { Spacer(modifier = Modifier.height(4.dp)) }

            // Build Info
            item {
                ConsoleSection(title = "Build Info", icon = Icons.Default.Build) {
                    viewModel.buildInfo.forEach { (key, value) ->
                        KeyValueRow(key = key, value = value)
                    }
                }
            }

            // DataStore Raw Values
            item {
                ConsoleSection(
                    title = "DataStore (avanues_settings)",
                    icon = Icons.Default.Storage,
                    action = {
                        TextButton(onClick = { showResetConfirm = true }) {
                            Text("RESET", color = AvanueTheme.colors.error, fontSize = 12.sp)
                        }
                    }
                ) {
                    if (rawPrefs.isEmpty()) {
                        Text(
                            "(empty — all defaults)",
                            color = AvanueTheme.colors.textSecondary,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 13.sp
                        )
                    } else {
                        rawPrefs.forEach { (key, value) ->
                            KeyValueRow(key = key, value = value, mono = true)
                        }
                    }
                }
            }

            // Service States
            item {
                ConsoleSection(title = "Service States", icon = Icons.Default.HealthAndSafety) {
                    services.forEach { svc ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (svc.isHealthy) Icons.Default.CheckCircle
                                else Icons.Default.Cancel,
                                contentDescription = null,
                                tint = if (svc.isHealthy) AvanueTheme.colors.success
                                else AvanueTheme.colors.error,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                svc.name,
                                modifier = Modifier.weight(1f),
                                fontSize = 14.sp
                            )
                            Text(
                                svc.status,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (svc.isHealthy) AvanueTheme.colors.success
                                else AvanueTheme.colors.error
                            )
                        }
                        svc.details.forEach { (k, v) ->
                            Text(
                                "  $k: $v",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                color = AvanueTheme.colors.textSecondary,
                                modifier = Modifier.padding(start = 26.dp)
                            )
                        }
                    }
                }
            }

            // RPC Ports
            item {
                ConsoleSection(title = "RPC Port Allocation", icon = Icons.Default.Hub) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 4.dp)
                    ) {
                        Text(
                            "Service",
                            modifier = Modifier.weight(1f),
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = AvanueTheme.colors.textSecondary
                        )
                        Text(
                            "Port",
                            modifier = Modifier.width(60.dp),
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = AvanueTheme.colors.textSecondary
                        )
                        Text(
                            "Transport",
                            modifier = Modifier.width(72.dp),
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = AvanueTheme.colors.textSecondary
                        )
                    }
                    HorizontalDivider(color = AvanueTheme.colors.textSecondary.copy(alpha = 0.3f))
                    viewModel.rpcPorts.forEach { rpc ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp)
                        ) {
                            Text(
                                rpc.serviceName,
                                modifier = Modifier.weight(1f),
                                fontSize = 13.sp
                            )
                            Text(
                                rpc.port.toString(),
                                modifier = Modifier.width(60.dp),
                                fontFamily = FontFamily.Monospace,
                                fontSize = 13.sp,
                                color = AvanueTheme.colors.info
                            )
                            Text(
                                rpc.transport,
                                modifier = Modifier.width(72.dp),
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                color = AvanueTheme.colors.textSecondary
                            )
                        }
                    }
                }
            }

            // Database Files
            item {
                ConsoleSection(title = "Database Files", icon = Icons.Default.FolderOpen) {
                    if (dbFiles.isEmpty()) {
                        Text(
                            "No database files found",
                            color = AvanueTheme.colors.textSecondary,
                            fontSize = 13.sp
                        )
                    } else {
                        val dateFormat = remember {
                            SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)
                        }
                        dbFiles.forEach { db ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        db.fileName,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 13.sp
                                    )
                                    Text(
                                        "Modified: ${dateFormat.format(Date(db.lastModified))}",
                                        fontSize = 11.sp,
                                        color = AvanueTheme.colors.textSecondary
                                    )
                                }
                                Text(
                                    formatFileSize(db.sizeBytes),
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 12.sp,
                                    color = AvanueTheme.colors.info
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Path: /data/data/${BuildConfig.APPLICATION_ID}/databases/",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            color = AvanueTheme.colors.textSecondary,
                            modifier = Modifier.horizontalScroll(rememberScrollState())
                        )
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }

    // Reset DataStore confirmation dialog
    if (showResetConfirm) {
        AlertDialog(
            onDismissRequest = { showResetConfirm = false },
            icon = { Icon(Icons.Default.Warning, contentDescription = null) },
            title = { Text("Reset DataStore?") },
            text = {
                Text("This will clear ALL persisted preferences and restore defaults. " +
                        "The app may behave unexpectedly until restarted.")
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.clearDataStore()
                    showResetConfirm = false
                }) {
                    Text("Reset", color = AvanueTheme.colors.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

// =============================================================================
// Reusable Components
// =============================================================================

@Composable
private fun ConsoleSection(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    action: @Composable (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    AvanueCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = AvanueTheme.colors.info,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = title,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
                modifier = Modifier.weight(1f)
            )
            action?.invoke()
        }
        content()
    }
}

@Composable
private fun KeyValueRow(key: String, value: String, mono: Boolean = false) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            key,
            modifier = Modifier.weight(1f),
            fontSize = 13.sp,
            color = AvanueTheme.colors.textSecondary
        )
        Text(
            value,
            fontFamily = if (mono) FontFamily.Monospace else FontFamily.Default,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

private fun formatFileSize(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "${bytes / 1024} KB"
        else -> "%.1f MB".format(bytes / (1024.0 * 1024.0))
    }
}
