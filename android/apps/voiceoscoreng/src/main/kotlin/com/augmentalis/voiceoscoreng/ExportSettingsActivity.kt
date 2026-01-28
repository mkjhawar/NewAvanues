/**
 * ExportSettingsActivity.kt - Activity for exporting voice commands to JSON file
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2026-01-22
 *
 * Provides UI for:
 * - Viewing apps available for export
 * - Selecting apps to export
 * - Choosing export location via SAF
 */
package com.augmentalis.voiceoscoreng

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.augmentalis.voiceoscore.AppMetadata
import com.augmentalis.voiceoscore.CommandExporter
import com.augmentalis.voiceoscore.AppExportSummary
import com.augmentalis.voiceoscore.ExportSerializer
import com.augmentalis.voiceoscoreng.service.AndroidExportFileProvider
import com.augmentalis.voiceoscoreng.ui.theme.VoiceOSCoreNGTheme
import kotlinx.coroutines.launch

/**
 * Activity for exporting voice commands to JSON file.
 *
 * Provides options to:
 * - Export all apps
 * - Export selected apps
 * - Choose export location via SAF
 */
class ExportSettingsActivity : ComponentActivity() {

    private lateinit var fileProvider: AndroidExportFileProvider
    private lateinit var exporter: CommandExporter

    // State holders for Compose
    private var exportableAppsState = mutableStateOf<List<AppExportSummary>>(emptyList())
    private var selectedAppsState = mutableStateOf<Set<String>>(emptySet())
    private var isLoadingState = mutableStateOf(true)
    private var isExportingState = mutableStateOf(false)

    // SAF document creation launcher
    private val createDocumentLauncher = registerForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri?.let { handleExportLocation(it) }
    }

    private var pendingExportData: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        fileProvider = AndroidExportFileProvider(applicationContext)

        // Initialize exporter with app's repositories
        val app = VoiceOSCoreNGApplication.getInstance(applicationContext)
        exporter = CommandExporter(
            commandPersistence = app.commandPersistence,
            getPackageNames = {
                app.scrapedAppRepository.getAll().map { it.packageName }
            },
            getAppInfo = { packageName ->
                val appDto = app.scrapedAppRepository.getByPackage(packageName)
                if (appDto != null) {
                    AppMetadata(
                        appName = appDto.packageName.substringAfterLast(".").replaceFirstChar {
                            if (it.isLowerCase()) it.titlecase() else it.toString()
                        },
                        versionCode = appDto.versionCode,
                        versionName = appDto.versionName,
                        lastUpdated = appDto.lastScrapedAt
                    )
                } else {
                    AppMetadata.fromPackageName(packageName)
                }
            }
        )

        // Load exportable apps
        loadExportableApps()

        setContent {
            VoiceOSCoreNGTheme {
                ExportScreen()
            }
        }
    }

    private fun loadExportableApps() {
        lifecycleScope.launch {
            try {
                val apps = exporter.getExportableApps()
                exportableAppsState.value = apps
            } catch (e: Exception) {
                Toast.makeText(
                    this@ExportSettingsActivity,
                    "Failed to load apps: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            } finally {
                isLoadingState.value = false
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun ExportScreen() {
        val exportableApps by exportableAppsState
        val selectedApps by selectedAppsState
        val isLoading by isLoadingState
        val isExporting by isExportingState

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Export Commands") },
                    navigationIcon = {
                        IconButton(onClick = { finish() }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp)
            ) {
                if (isLoading) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                } else if (exportableApps.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "No apps with learned commands to export",
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                } else {
                    Text(
                        "Select apps to export:",
                        style = MaterialTheme.typography.titleMedium
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Select all toggle
                    Card(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedAppsState.value = if (selectedApps.size == exportableApps.size) {
                                        emptySet()
                                    } else {
                                        exportableApps.map { it.packageName }.toSet()
                                    }
                                }
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    "Select All",
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                Text(
                                    "${exportableApps.size} apps, ${exportableApps.sumOf { it.commandCount }} commands",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Checkbox(
                                checked = selectedApps.size == exportableApps.size && exportableApps.isNotEmpty(),
                                onCheckedChange = { checked ->
                                    selectedAppsState.value = if (checked) {
                                        exportableApps.map { it.packageName }.toSet()
                                    } else {
                                        emptySet()
                                    }
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    HorizontalDivider()

                    Spacer(modifier = Modifier.height(8.dp))

                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(exportableApps) { app ->
                            AppExportItem(
                                app = app,
                                isSelected = app.packageName in selectedApps,
                                onToggle = { selected ->
                                    selectedAppsState.value = if (selected) {
                                        selectedApps + app.packageName
                                    } else {
                                        selectedApps - app.packageName
                                    }
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Export buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { performExport(selectedApps.toList()) },
                            enabled = selectedApps.isNotEmpty() && !isExporting,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Export Selected (${selectedApps.size})")
                        }

                        OutlinedButton(
                            onClick = { performExport(exportableApps.map { it.packageName }) },
                            enabled = exportableApps.isNotEmpty() && !isExporting,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Export All")
                        }
                    }

                    if (isExporting) {
                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text(
                            "Preparing export...",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }
        }
    }

    @Composable
    private fun AppExportItem(
        app: AppExportSummary,
        isSelected: Boolean,
        onToggle: (Boolean) -> Unit
    ) {
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggle(!isSelected) }
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        app.appName,
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        "${app.commandCount} commands",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        app.packageName,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = onToggle
                )
            }
        }
    }

    private fun performExport(packageNames: List<String>) {
        isExportingState.value = true

        lifecycleScope.launch {
            try {
                val exportPackage = if (packageNames.isEmpty()) {
                    exporter.exportAll()
                } else {
                    exporter.exportApps(packageNames)
                }

                val json = ExportSerializer.serialize(exportPackage)
                pendingExportData = json

                // Launch SAF file picker
                val filename = ExportSerializer.generateExportFilename(
                    exportPackage.manifest.exportType,
                    packageNames.firstOrNull()
                )
                createDocumentLauncher.launch(filename)

            } catch (e: Exception) {
                Toast.makeText(
                    this@ExportSettingsActivity,
                    "Export failed: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
                isExportingState.value = false
            }
        }
    }

    private fun handleExportLocation(uri: android.net.Uri) {
        val data = pendingExportData
        if (data == null) {
            isExportingState.value = false
            return
        }
        pendingExportData = null

        lifecycleScope.launch {
            fileProvider.writeToUri(uri, data).fold(
                onSuccess = {
                    Toast.makeText(
                        this@ExportSettingsActivity,
                        "Export successful!",
                        Toast.LENGTH_SHORT
                    ).show()
                    finish()
                },
                onFailure = { e ->
                    Toast.makeText(
                        this@ExportSettingsActivity,
                        "Failed to save: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                    isExportingState.value = false
                }
            )
        }
    }
}
