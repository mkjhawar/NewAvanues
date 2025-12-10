/**
 * CommandSynonymViewModel.kt - ViewModel for command synonym management
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VoiceOS Development Team
 * Code-Reviewed-By: Claude Code (IDEACODE v10.3)
 * Created: 2025-12-08
 *
 * State management for command synonym settings UI.
 * Handles app list, command list, and synonym CRUD operations.
 */

package com.augmentalis.voiceoscore.learnapp.ui.settings

import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.augmentalis.database.VoiceOSDatabaseManager
import com.augmentalis.database.dto.GeneratedCommandDTO
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * App info for synonym settings
 */
data class AppInfo(
    val packageName: String,
    val name: String,
    val icon: Drawable,
    val commandCount: Int
)

/**
 * UI state for synonym editor
 */
sealed class SynonymEditorState {
    object Hidden : SynonymEditorState()
    data class Editing(val command: GeneratedCommandDTO) : SynonymEditorState()
}

/**
 * ViewModel for command synonym settings
 *
 * ## Features:
 * - Load apps with voice commands
 * - Display commands for selected app
 * - Edit synonyms for individual commands
 * - Search/filter functionality
 *
 * ## Usage:
 * ```kotlin
 * val viewModel: CommandSynonymViewModel = viewModel(
 *     factory = CommandSynonymViewModelFactory(databaseManager, packageManager)
 * )
 * ```
 */
class CommandSynonymViewModel(
    private val database: VoiceOSDatabaseManager,
    private val packageManager: PackageManager
) : ViewModel() {

    private val _installedApps = MutableStateFlow<List<AppInfo>>(emptyList())
    val installedApps: StateFlow<List<AppInfo>> = _installedApps.asStateFlow()

    private val _selectedApp = MutableStateFlow<String?>(null)
    val selectedApp: StateFlow<String?> = _selectedApp.asStateFlow()

    private val _commandsForApp = MutableStateFlow<List<GeneratedCommandDTO>>(emptyList())
    val commandsForApp: StateFlow<List<GeneratedCommandDTO>> = _commandsForApp.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _editorState = MutableStateFlow<SynonymEditorState>(SynonymEditorState.Hidden)
    val editorState: StateFlow<SynonymEditorState> = _editorState.asStateFlow()

    init {
        loadInstalledApps()
    }

    /**
     * Load all installed apps that have voice commands
     */
    private fun loadInstalledApps() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val apps = withContext(Dispatchers.IO) {
                    packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
                        .mapNotNull { appInfo ->
                            try {
                                loadAppInfo(appInfo)
                            } catch (e: Exception) {
                                Log.e(TAG, "Failed to load app info for ${appInfo.packageName}", e)
                                null
                            }
                        }
                        .filter { it.commandCount > 0 }
                        .sortedBy { it.name }
                }
                _installedApps.value = apps
                Log.d(TAG, "Loaded ${apps.size} apps with commands")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load installed apps", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Load app info including command count
     */
    private suspend fun loadAppInfo(appInfo: ApplicationInfo): AppInfo? {
        return try {
            val name = packageManager.getApplicationLabel(appInfo).toString()
            val commandCount = database.generatedCommands.getByPackage(appInfo.packageName).size

            if (commandCount > 0) {
                AppInfo(
                    packageName = appInfo.packageName,
                    name = name,
                    icon = appInfo.loadIcon(packageManager),
                    commandCount = commandCount
                )
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load app info", e)
            null
        }
    }

    /**
     * Select an app to view its commands
     */
    fun selectApp(packageName: String) {
        _selectedApp.value = packageName
        _searchQuery.value = ""
        loadCommandsForApp(packageName)
    }

    /**
     * Clear app selection and return to app list
     */
    fun clearSelection() {
        _selectedApp.value = null
        _commandsForApp.value = emptyList()
        _searchQuery.value = ""
    }

    /**
     * Load commands for selected app
     */
    private fun loadCommandsForApp(packageName: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val commands = withContext(Dispatchers.IO) {
                    database.generatedCommands.getByPackage(packageName)
                        .sortedBy { it.commandText }
                }
                _commandsForApp.value = commands
                Log.d(TAG, "Loaded ${commands.size} commands for $packageName")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load commands for $packageName", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Set search query and filter commands
     */
    fun setSearchQuery(query: String) {
        _searchQuery.value = query
        _selectedApp.value?.let { packageName ->
            filterCommands(packageName, query)
        }
    }

    /**
     * Filter commands by search query
     */
    private fun filterCommands(packageName: String, query: String) {
        viewModelScope.launch {
            try {
                val allCommands = withContext(Dispatchers.IO) {
                    database.generatedCommands.getByPackage(packageName)
                }

                val filtered = if (query.isBlank()) {
                    allCommands
                } else {
                    allCommands.filter { command ->
                        command.commandText.contains(query, ignoreCase = true) ||
                        command.synonyms?.contains(query, ignoreCase = true) == true
                    }
                }

                _commandsForApp.value = filtered.sortedBy { it.commandText }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to filter commands", e)
            }
        }
    }

    /**
     * Show synonym editor for command
     */
    fun editSynonyms(command: GeneratedCommandDTO) {
        _editorState.value = SynonymEditorState.Editing(command)
    }

    /**
     * Hide synonym editor
     */
    fun hideEditor() {
        _editorState.value = SynonymEditorState.Hidden
    }

    /**
     * Update command synonyms
     */
    fun updateCommand(command: GeneratedCommandDTO) {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    database.generatedCommands.update(command)
                }
                Log.i(TAG, "Updated synonyms for command: ${command.commandText}")

                // Reload commands to reflect changes
                _selectedApp.value?.let { packageName ->
                    loadCommandsForApp(packageName)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update command", e)
            }
        }
    }

    /**
     * Refresh apps list
     */
    fun refreshApps() {
        loadInstalledApps()
    }

    companion object {
        private const val TAG = "CommandSynonymViewModel"
    }
}
