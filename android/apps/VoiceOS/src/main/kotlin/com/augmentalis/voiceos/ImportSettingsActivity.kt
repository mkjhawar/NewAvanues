/**
 * ImportSettingsActivity.kt - Activity for importing voice commands from JSON file
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2026-01-22
 *
 * Provides UI for:
 * - Selecting import file via SAF
 * - Previewing import contents
 * - Choosing import strategy (Merge/Replace/Skip Existing)
 */
package com.augmentalis.voiceos

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.augmentalis.voiceoscore.AppImportPreview
import com.augmentalis.voiceoscore.CommandImporter
import com.augmentalis.voiceoscore.ExportPackage
import com.augmentalis.voiceoscore.ImportPreview
import com.augmentalis.voiceoscore.ImportStrategy
import com.augmentalis.voiceoscore.ExportParseException
import com.augmentalis.voiceoscore.ExportSerializer
import com.augmentalis.voiceoscore.ExportVersionException
import com.augmentalis.voiceos.service.AndroidExportFileProvider
import com.augmentalis.voiceos.ui.theme.VoiceOSTheme
import kotlinx.coroutines.launch

/**
 * Activity for importing voice commands from JSON file.
 *
 * Provides options to:
 * - Select import file via SAF
 * - Preview import contents
 * - Choose import strategy (Merge/Replace/Skip Existing)
 */
class ImportSettingsActivity : ComponentActivity() {

    private lateinit var fileProvider: AndroidExportFileProvider
    private lateinit var importer: CommandImporter

    // State holders for Compose
    private var selectedFileNameState = mutableStateOf<String?>(null)
    private var previewState = mutableStateOf<ImportPreview?>(null)
    private var exportPackageState = mutableStateOf<ExportPackage?>(null)
    private var importStrategyState = mutableStateOf(ImportStrategy.MERGE)
    private var isLoadingState = mutableStateOf(false)
    private var isImportingState = mutableStateOf(false)

    // SAF document picker launcher
    private val openDocumentLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { handleSelectedFile(it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        fileProvider = AndroidExportFileProvider(applicationContext)

        val app = VoiceOSApplication.getInstance(applicationContext)
        importer = CommandImporter(app.commandPersistence)

        setContent {
            VoiceOSTheme {
                ImportScreen()
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun ImportScreen() {
        val selectedFileName by selectedFileNameState
        val preview by previewState
        val exportPackage by exportPackageState
        val importStrategy by importStrategyState
        val isLoading by isLoadingState
        val isImporting by isImportingState

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Import Commands") },
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
                // File selection button
                OutlinedButton(
                    onClick = { openDocumentLauncher.launch(arrayOf("application/json")) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        Icons.Default.FileOpen,
                        contentDescription = null,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text(selectedFileName?.let { "Selected: $it" } ?: "Select Import File")
                }

                if (isLoading) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                preview?.let { importPreview ->
                    Spacer(modifier = Modifier.height(16.dp))

                    // Import summary card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                "Import Summary",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Apps:")
                                Text("${importPreview.apps.size}")
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Total commands:")
                                Text("${importPreview.totalCommands}")
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Format version:")
                                Text("${importPreview.manifest.version}")
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Already in database:")
                                Text("${importPreview.existingAppsCount} apps")
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Import strategy selection
                    Text(
                        "Import Strategy:",
                        style = MaterialTheme.typography.titleSmall
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            ImportStrategy.entries.forEach { strategy ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .selectable(
                                            selected = importStrategy == strategy,
                                            onClick = { importStrategyState.value = strategy },
                                            role = Role.RadioButton
                                        )
                                        .padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = importStrategy == strategy,
                                        onClick = null // handled by selectable
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = when (strategy) {
                                                ImportStrategy.MERGE -> "Merge"
                                                ImportStrategy.REPLACE -> "Replace"
                                                ImportStrategy.SKIP_EXISTING -> "Skip Existing"
                                            },
                                            style = MaterialTheme.typography.bodyLarge
                                        )
                                        Text(
                                            text = when (strategy) {
                                                ImportStrategy.MERGE -> "Keep existing commands, add new ones"
                                                ImportStrategy.REPLACE -> "Delete existing, import all from file"
                                                ImportStrategy.SKIP_EXISTING -> "Only import apps not in database"
                                            },
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // App list preview
                    Text(
                        "Apps to import:",
                        style = MaterialTheme.typography.titleSmall
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(importPreview.apps) { app ->
                            AppImportItem(app)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Import button
                    Button(
                        onClick = {
                            exportPackage?.let { pkg ->
                                performImport(pkg, importStrategy)
                            }
                        },
                        enabled = exportPackage != null && !isImporting,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Import Commands")
                    }

                    if (isImporting) {
                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text(
                            "Importing commands...",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }

                // Empty state when no file selected
                if (preview == null && !isLoading) {
                    Spacer(modifier = Modifier.height(32.dp))
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                "Select a VoiceOS export file to import",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "Files should be in .json format",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun AppImportItem(app: AppImportPreview) {
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
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
                        buildString {
                            append("${app.commandCount} commands")
                            if (app.existsInDatabase) {
                                append(" (${app.existingCommandCount} existing)")
                            }
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        app.packageName,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
                if (app.existsInDatabase) {
                    Badge(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                        contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                    ) {
                        Text("Exists")
                    }
                }
            }
        }
    }

    private fun handleSelectedFile(uri: android.net.Uri) {
        isLoadingState.value = true
        previewState.value = null
        exportPackageState.value = null

        // Extract filename for display
        val cursor = contentResolver.query(uri, null, null, null, null)
        val nameIndex = cursor?.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
        cursor?.moveToFirst()
        val fileName = if (nameIndex != null && nameIndex >= 0) {
            cursor.getString(nameIndex)
        } else {
            uri.lastPathSegment ?: "Unknown file"
        }
        cursor?.close()
        selectedFileNameState.value = fileName

        lifecycleScope.launch {
            try {
                val json = fileProvider.readImport(uri.toString()).getOrThrow()
                val pkg = ExportSerializer.deserialize(json).getOrThrow()
                exportPackageState.value = pkg

                val importPreview = importer.preview(pkg)
                previewState.value = importPreview

            } catch (e: ExportVersionException) {
                Toast.makeText(
                    this@ImportSettingsActivity,
                    "Incompatible file version: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
                selectedFileNameState.value = null
            } catch (e: ExportParseException) {
                Toast.makeText(
                    this@ImportSettingsActivity,
                    "Invalid export file: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
                selectedFileNameState.value = null
            } catch (e: Exception) {
                Toast.makeText(
                    this@ImportSettingsActivity,
                    "Failed to read file: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
                selectedFileNameState.value = null
            } finally {
                isLoadingState.value = false
            }
        }
    }

    private fun performImport(pkg: ExportPackage, strategy: ImportStrategy) {
        isImportingState.value = true

        lifecycleScope.launch {
            try {
                val result = importer.import(pkg, strategy)

                if (result.success) {
                    Toast.makeText(
                        this@ImportSettingsActivity,
                        "Imported ${result.commandsImported} commands from ${result.appsImported} apps",
                        Toast.LENGTH_LONG
                    ).show()
                    finish()
                } else {
                    val errorSummary = if (result.errors.isNotEmpty()) {
                        result.errors.first()
                    } else {
                        "Unknown error"
                    }
                    Toast.makeText(
                        this@ImportSettingsActivity,
                        "Import completed with errors: $errorSummary",
                        Toast.LENGTH_LONG
                    ).show()
                }
            } catch (e: Exception) {
                Toast.makeText(
                    this@ImportSettingsActivity,
                    "Import failed: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            } finally {
                isImportingState.value = false
            }
        }
    }
}
