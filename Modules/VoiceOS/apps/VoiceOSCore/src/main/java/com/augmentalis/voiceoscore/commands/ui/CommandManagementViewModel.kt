/**
 * CommandManagementViewModel.kt - ViewModel for command management screen
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Code-Reviewed-By: CCA
 * Created: 2025-12-15
 *
 * P3 Task 2.1: Version-aware command list with deprecation indicators
 */

package com.augmentalis.voiceoscore.commands.ui

import android.content.pm.PackageManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.augmentalis.database.repositories.IAppVersionRepository
import com.augmentalis.database.repositories.IGeneratedCommandRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * ViewModel for global command management screen.
 *
 * Loads all voice commands across all apps with version information,
 * deprecation status, and usage statistics. Provides data for:
 * - Command list grouped by app
 * - Version badges and deprecation warnings
 * - Usage statistics and confidence indicators
 *
 * ## Architecture:
 * Follows MVVM pattern with unidirectional data flow:
 * Repository → ViewModel → UI State → Compose UI
 *
 * @property commandRepo Repository for generated command operations
 * @property versionRepo Repository for app version tracking
 * @property packageManager Android PackageManager for app name resolution
 */
class CommandManagementViewModel(
    private val commandRepo: IGeneratedCommandRepository,
    private val versionRepo: IAppVersionRepository,
    private val packageManager: PackageManager
) : ViewModel() {

    companion object {
        private const val TAG = "CommandManagementVM"
        private const val GRACE_PERIOD_DAYS = 30
        private const val MILLIS_PER_DAY = 86400000L
    }

    private val _uiState = MutableStateFlow(CommandListUiState())
    val uiState: StateFlow<CommandListUiState> = _uiState.asStateFlow()

    init {
        loadCommands()
    }

    /**
     * Load all commands grouped by app with version info.
     *
     * ## Algorithm:
     * 1. Load all commands from database
     * 2. Group by app package name
     * 3. For each app:
     *    - Resolve app name from PackageManager
     *    - Load app version from version repository
     *    - Transform commands to UI models with:
     *      - Deprecation countdown
     *      - Confidence percentage
     *      - Version badges
     * 4. Sort groups by command count (descending)
     * 5. Sort commands within group by usage count (descending)
     *
     * ## Performance:
     * - Runs on Dispatchers.Default for CPU-bound work
     * - Single database query for all commands
     * - Lazy evaluation of app names (cached by PackageManager)
     */
    fun loadCommands() {
        viewModelScope.launch {
            try {
                _uiState.value = CommandListUiState(isLoading = true)

                // Load all commands in background thread
                val commandGroups = withContext(Dispatchers.Default) {
                    // Fetch all commands
                    val allCommands = commandRepo.getAll()

                    // Group by app package name
                    val commandsByApp = allCommands.groupBy { it.appId }

                    // Transform to UI models
                    commandsByApp.map { (packageName, commands) ->
                        val appName = extractAppName(packageName)
                        val appVersion = versionRepo.getAppVersion(packageName)

                        CommandGroupUiModel(
                            packageName = packageName,
                            appName = appName,
                            commands = commands.map { cmd ->
                                CommandUiModel(
                                    id = cmd.id,
                                    commandText = cmd.commandText,
                                    confidence = cmd.confidence,
                                    versionName = cmd.appVersion,
                                    versionCode = cmd.versionCode,
                                    isDeprecated = cmd.isDeprecated == 1L,
                                    isUserApproved = cmd.isUserApproved == 1L,
                                    usageCount = cmd.usageCount,
                                    lastUsed = cmd.lastUsed,
                                    daysUntilDeletion = if (cmd.isDeprecated == 1L) {
                                        calculateDaysUntilDeletion(cmd.lastVerified ?: cmd.createdAt)
                                    } else null
                                )
                            }.sortedByDescending { it.usageCount }
                        )
                    }.sortedByDescending { it.commands.size }
                }

                _uiState.value = CommandListUiState(
                    commandGroups = commandGroups,
                    isLoading = false
                )

            } catch (e: Exception) {
                _uiState.value = CommandListUiState(
                    isLoading = false,
                    error = "Failed to load commands: ${e.message}"
                )
            }
        }
    }

    /**
     * Extract human-readable app name from package name.
     *
     * Uses PackageManager to resolve app label. Falls back to
     * capitalized package name if app is not installed.
     *
     * ## Examples:
     * - "com.google.android.gm" → "Gmail"
     * - "com.android.chrome" → "Chrome"
     * - "com.unknown.app" → "App" (if not installed)
     *
     * @param packageName App package identifier
     * @return Human-readable app name
     */
    private fun extractAppName(packageName: String): String {
        return try {
            val appInfo = packageManager.getApplicationInfo(packageName, 0)
            packageManager.getApplicationLabel(appInfo).toString()
        } catch (e: Exception) {
            // App not installed - extract from package name
            packageName.substringAfterLast('.').replaceFirstChar { it.uppercase() }
        }
    }

    /**
     * Calculate days until deprecated command will be deleted.
     *
     * ## Grace Period:
     * Commands are kept for 30 days after deprecation before cleanup.
     * This allows:
     * - User to continue using familiar commands during app transition
     * - VoiceOS to re-verify commands are still valid
     * - Rollback if user downgrades app
     *
     * ## Calculation:
     * ```
     * deletionTime = lastVerified + 30 days
     * daysRemaining = (deletionTime - now) / 1 day
     * ```
     *
     * @param lastVerified Timestamp when command was last verified (epoch millis)
     * @return Days remaining until deletion (0 if deletion is imminent)
     */
    private fun calculateDaysUntilDeletion(lastVerified: Long): Int {
        val gracePeriodMs = GRACE_PERIOD_DAYS * MILLIS_PER_DAY
        val deletionTimestamp = lastVerified + gracePeriodMs
        val now = System.currentTimeMillis()
        val remainingMs = deletionTimestamp - now
        val daysRemaining = (remainingMs / MILLIS_PER_DAY).toInt()
        return daysRemaining.coerceAtLeast(0)
    }

    /**
     * Refresh command list.
     *
     * Called when:
     * - User pulls to refresh
     * - Returns from command detail screen
     * - After cleanup operation
     */
    fun refresh() {
        loadCommands()
    }

    /**
     * Filter commands by search query.
     *
     * Filters command text and app name in memory.
     * For large datasets (>10k commands), consider database-level filtering.
     *
     * @param query Search text (case-insensitive)
     */
    fun search(query: String) {
        if (query.isBlank()) {
            // Reset to full list
            loadCommands()
            return
        }

        viewModelScope.launch {
            try {
                val currentState = _uiState.value
                if (currentState.commandGroups.isEmpty()) {
                    // No data to filter
                    return@launch
                }

                val filteredGroups = withContext(Dispatchers.Default) {
                    currentState.commandGroups.mapNotNull { group ->
                        val matchingCommands = group.commands.filter { cmd ->
                            cmd.commandText.contains(query, ignoreCase = true) ||
                                    group.appName.contains(query, ignoreCase = true)
                        }

                        if (matchingCommands.isNotEmpty()) {
                            group.copy(commands = matchingCommands)
                        } else {
                            null
                        }
                    }
                }

                _uiState.value = currentState.copy(
                    commandGroups = filteredGroups
                )

            } catch (e: Exception) {
                // Search failed - keep current state
            }
        }
    }

    /**
     * Delete a specific command.
     *
     * @param commandId Command database ID
     */
    fun deleteCommand(commandId: Long) {
        viewModelScope.launch {
            try {
                commandRepo.deleteById(commandId)
                // Reload to reflect deletion
                loadCommands()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Failed to delete command: ${e.message}"
                )
            }
        }
    }

    /**
     * Clear error state.
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}
