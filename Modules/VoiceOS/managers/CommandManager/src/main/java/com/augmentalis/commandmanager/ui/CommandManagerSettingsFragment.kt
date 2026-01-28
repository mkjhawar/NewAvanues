/**
 * CommandManagerSettingsFragment.kt - Settings UI for CommandManager
 *
 * Purpose: Provide user-facing settings and controls for command management
 * Features:
 * - Reload commands button (forces JSON reload)
 * - Database statistics display
 * - Clear usage data option
 * - Developer mode file watcher toggle
 *
 * Phase 2.4c: Dynamic Command Updates
 */

package com.augmentalis.voiceoscore.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.augmentalis.voiceoscore.CommandManager
import com.augmentalis.voiceoscore.database.CommandDatabase
import com.augmentalis.voiceoscore.loader.CommandLoader
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

/**
 * Settings fragment for CommandManager
 * Uses Jetpack Compose for modern Material 3 UI
 */
class CommandManagerSettingsFragment : Fragment() {

    private lateinit var database: CommandDatabase
    private lateinit var commandLoader: CommandLoader
    private lateinit var commandManager: CommandManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        database = CommandDatabase.getInstance(requireContext())
        commandLoader = CommandLoader.create(requireContext())
        commandManager = CommandManager.getInstance(requireContext())
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                MaterialTheme {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        CommandManagerSettings()
                    }
                }
            }
        }
    }

    @Composable
    private fun CommandManagerSettings() {
        var isLoading by remember { mutableStateOf(false) }
        var stats by remember { mutableStateOf<DatabaseStats?>(null) }
        var errorMessage by remember { mutableStateOf<String?>(null) }

        // Load stats on first composition
        LaunchedEffect(Unit) {
            loadDatabaseStats { stats = it }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Header
            Text(
                text = "Command Manager Settings",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            // Database Statistics Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Database Statistics",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    val s = stats
                    if (s != null) {
                        StatRow("Total Commands", s.totalCommands.toString())
                        StatRow("Locales Loaded", s.locales.joinToString(", "))
                        StatRow("JSON Version", s.jsonVersion)
                        StatRow("Last Loaded", s.lastLoadedFormatted)
                        StatRow("Total Usage Count", s.usageCount.toString())
                        StatRow("Failed Attempts", s.failedAttempts.toString())
                    } else {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                    }
                }
            }

            // Language Selection Card (Phase 1: Multi-Language Support)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Language / Idioma / Langue / Sprache",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    // Current locale display
                    val currentLocale = remember { commandManager.getCurrentLocale() }
                    var selectedLocale by remember { mutableStateOf(currentLocale) }
                    var availableLocales by remember { mutableStateOf<List<String>>(emptyList()) }
                    var isLoadingLocales by remember { mutableStateOf(false) }
                    var isSwitchingLocale by remember { mutableStateOf(false) }

                    // Load available locales on first composition
                    LaunchedEffect(Unit) {
                        isLoadingLocales = true
                        availableLocales = commandManager.getAvailableLocales()
                        isLoadingLocales = false
                    }

                    Text(
                        text = "Current: ${localeDisplayName(selectedLocale)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    Text(
                        text = "Select your preferred language for voice commands:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    // Language buttons
                    if (isLoadingLocales) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .align(Alignment.CenterHorizontally)
                                .padding(16.dp)
                        )
                    } else {
                        availableLocales.forEach { locale ->
                            val isSelected = selectedLocale == locale
                            Button(
                                onClick = {
                                    if (!isSwitchingLocale && locale != selectedLocale) {
                                        isSwitchingLocale = true
                                        lifecycleScope.launch {
                                            try {
                                                val success = commandManager.switchLocale(locale)
                                                if (success) {
                                                    selectedLocale = locale
                                                    showToast("✅ ${localeDisplayName(locale)} activated")
                                                    // Reload stats to show new locale
                                                    loadDatabaseStats { stats = it }
                                                } else {
                                                    showToast("❌ Failed to switch to ${localeDisplayName(locale)}")
                                                }
                                            } catch (e: Exception) {
                                                showToast("❌ Error: ${e.message}")
                                            } finally {
                                                isSwitchingLocale = false
                                            }
                                        }
                                    }
                                },
                                enabled = !isSwitchingLocale,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 8.dp),
                                colors = if (isSelected) {
                                    ButtonDefaults.buttonColors()
                                } else {
                                    ButtonDefaults.outlinedButtonColors()
                                }
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(localeDisplayName(locale))
                                    if (isSelected) {
                                        Text("✓", style = MaterialTheme.typography.titleMedium)
                                    }
                                }
                            }
                        }
                    }

                    // Reset to system default button
                    OutlinedButton(
                        onClick = {
                            isSwitchingLocale = true
                            lifecycleScope.launch {
                                try {
                                    val success = commandManager.resetToSystemLocale()
                                    if (success) {
                                        selectedLocale = commandManager.getCurrentLocale()
                                        showToast("✅ Reset to system default: ${localeDisplayName(selectedLocale)}")
                                        loadDatabaseStats { stats = it }
                                    } else {
                                        showToast("❌ Failed to reset to system default")
                                    }
                                } catch (e: Exception) {
                                    showToast("❌ Error: ${e.message}")
                                } finally {
                                    isSwitchingLocale = false
                                }
                            }
                        },
                        enabled = !isSwitchingLocale,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                    ) {
                        Text("Reset to System Default")
                    }
                }
            }

            // Reload Commands Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Command Management",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    Text(
                        text = "Reload commands from JSON files. Use this after updating localization files.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    Button(
                        onClick = {
                            isLoading = true
                            errorMessage = null
                            lifecycleScope.launch {
                                try {
                                    val result = commandLoader.forceReload()
                                    when (result) {
                                        is CommandLoader.LoadResult.Success -> {
                                            showToast("✅ Commands reloaded: ${result.commandCount} commands")
                                            loadDatabaseStats { stats = it }
                                        }
                                        is CommandLoader.LoadResult.Error -> {
                                            errorMessage = result.message
                                            showToast("❌ Reload failed: ${result.message}")
                                        }
                                        else -> {
                                            errorMessage = "Unexpected result type"
                                        }
                                    }
                                } catch (e: Exception) {
                                    errorMessage = e.message
                                    showToast("❌ Error: ${e.message}")
                                } finally {
                                    isLoading = false
                                }
                            }
                        },
                        enabled = !isLoading,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Text(if (isLoading) "Reloading..." else "Reload Commands")
                    }

                    if (errorMessage != null) {
                        Text(
                            text = "Error: $errorMessage",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
            }

            // Clear Usage Data Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Privacy",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    Text(
                        text = "Clear all command usage statistics. This cannot be undone.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    OutlinedButton(
                        onClick = {
                            lifecycleScope.launch {
                                try {
                                    val deleted = database.commandUsageDao().deleteAllRecords()
                                    showToast("✅ Cleared $deleted usage records")
                                    loadDatabaseStats { stats = it }
                                } catch (e: Exception) {
                                    showToast("❌ Error: ${e.message}")
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Clear Usage Data")
                    }
                }
            }

            // Refresh Statistics Button
            OutlinedButton(
                onClick = {
                    lifecycleScope.launch {
                        loadDatabaseStats { stats = it }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Refresh Statistics")
            }
        }
    }

    @Composable
    private fun StatRow(label: String, value: String) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }

    /**
     * Load database statistics
     */
    private suspend fun loadDatabaseStats(onLoaded: (DatabaseStats) -> Unit) {
        try {
            val versionDao = database.databaseVersionDao()
            val commandDao = database.voiceCommandDao()
            val usageDao = database.commandUsageDao()

            val version = versionDao.getVersion()
            val dbStats = commandDao.getDatabaseStats()
            val totalCommands = dbStats.sumOf { it.count }
            val locales = dbStats.map { it.locale }
            val usageCount = usageDao.getTotalUsageCount()
            val failedCount = usageDao.getFailedAttempts(limit = Int.MAX_VALUE).size

            val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

            onLoaded(
                DatabaseStats(
                    totalCommands = totalCommands,
                    locales = locales,
                    jsonVersion = version?.jsonVersion ?: "Unknown",
                    lastLoadedFormatted = version?.loadedAt?.let { dateFormat.format(it) } ?: "Never",
                    usageCount = usageCount,
                    failedAttempts = failedCount
                )
            )
        } catch (e: Exception) {
            onLoaded(
                DatabaseStats(
                    totalCommands = 0,
                    locales = emptyList(),
                    jsonVersion = "Error",
                    lastLoadedFormatted = "Error: ${e.message}",
                    usageCount = 0,
                    failedAttempts = 0
                )
            )
        }
    }

    /**
     * Show toast message
     */
    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    /**
     * Convert locale code to display name
     */
    private fun localeDisplayName(locale: String): String {
        return when (locale) {
            "en-US" -> "🇺🇸 English (US)"
            "es-ES" -> "🇪🇸 Español (España)"
            "fr-FR" -> "🇫🇷 Français (France)"
            "de-DE" -> "🇩🇪 Deutsch (Deutschland)"
            else -> locale
        }
    }

    /**
     * Data class for database statistics
     */
    private data class DatabaseStats(
        val totalCommands: Int,
        val locales: List<String>,
        val jsonVersion: String,
        val lastLoadedFormatted: String,
        val usageCount: Int,
        val failedAttempts: Int
    )
}
