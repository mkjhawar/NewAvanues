/**
 * CleanupPreviewViewModel.kt - ViewModel for cleanup preview screen
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2025-12-15
 *
 * P2 Task 2.2: Cleanup preview with statistics and execution
 */

package com.augmentalis.voiceoscore.cleanup.ui

import android.app.Application
import android.content.pm.PackageManager
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.augmentalis.database.repositories.IGeneratedCommandRepository
import com.augmentalis.voiceoscore.cleanup.CleanupManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for cleanup preview screen with business logic
 */
@HiltViewModel
class CleanupPreviewViewModel @Inject constructor(
    private val cleanupManager: CleanupManager,
    private val commandRepo: IGeneratedCommandRepository,
    private val application: Application
) : AndroidViewModel(application) {

    companion object {
        private const val TAG = "CleanupPreviewVM"
        private const val DEFAULT_GRACE_PERIOD_DAYS = 30
    }

    private val _uiState = MutableStateFlow<CleanupPreviewUiState>(CleanupPreviewUiState.Loading)
    val uiState: StateFlow<CleanupPreviewUiState> = _uiState.asStateFlow()

    private var currentGracePeriodDays = DEFAULT_GRACE_PERIOD_DAYS
    private var currentKeepUserApproved = true

    init {
        loadPreview()
    }

    /**
     * Load cleanup preview with statistics
     *
     * @param gracePeriodDays Grace period in days (default: 30)
     * @param keepUserApproved Whether to preserve user-approved commands (default: true)
     */
    fun loadPreview(gracePeriodDays: Int = DEFAULT_GRACE_PERIOD_DAYS, keepUserApproved: Boolean = true) {
        viewModelScope.launch {
            try {
                _uiState.value = CleanupPreviewUiState.Loading

                currentGracePeriodDays = gracePeriodDays
                currentKeepUserApproved = keepUserApproved

                // Get cleanup preview from CleanupManager
                val preview = cleanupManager.previewCleanup(gracePeriodDays, keepUserApproved)

                // Calculate total commands
                val totalCommands = commandRepo.count().toInt()
                val commandsToPreserve = totalCommands - preview.commandsToDelete
                val deletionPercentage = if (totalCommands > 0) {
                    ((preview.commandsToDelete.toDouble() / totalCommands) * 100).toInt()
                } else 0

                // Load affected apps info
                val affectedApps = loadAffectedAppsInfo(preview.appsAffected)

                // Create statistics
                val statistics = CleanupStatistics(
                    commandsToDelete = preview.commandsToDelete,
                    commandsToPreserve = commandsToPreserve,
                    appsAffected = preview.appsAffected.size,
                    deletionPercentage = deletionPercentage,
                    estimatedSizeMB = preview.databaseSizeReduction / (1024.0 * 1024.0)
                )

                val safetyLevel = calculateSafetyLevel(deletionPercentage)

                _uiState.value = CleanupPreviewUiState.Preview(
                    statistics = statistics,
                    affectedApps = affectedApps,
                    safetyLevel = safetyLevel,
                    gracePeriodDays = gracePeriodDays,
                    keepUserApproved = keepUserApproved
                )

                Log.i(TAG, "Preview loaded: ${statistics.commandsToDelete} to delete, ${statistics.deletionPercentage}% rate")

            } catch (e: Exception) {
                Log.e(TAG, "Failed to load cleanup preview", e)
                _uiState.value = CleanupPreviewUiState.Error(
                    message = e.message ?: "Failed to load preview",
                    canRetry = true
                )
            }
        }
    }

    /**
     * Execute cleanup operation
     */
    fun executeCleanup() {
        viewModelScope.launch {
            try {
                Log.i(TAG, "Executing cleanup: ${currentGracePeriodDays}d grace period")

                _uiState.value = CleanupPreviewUiState.Executing(
                    progress = 0,
                    currentApp = "Preparing cleanup..."
                )

                // Execute cleanup
                val result = cleanupManager.executeCleanup(
                    gracePeriodDays = currentGracePeriodDays,
                    keepUserApproved = currentKeepUserApproved,
                    dryRun = false
                )

                // Update progress (simulate for UX - actual cleanup is fast)
                _uiState.value = CleanupPreviewUiState.Executing(
                    progress = 100,
                    currentApp = "Finalizing..."
                )

                delay(500)  // Brief pause for UX

                if (result.errors.isEmpty()) {
                    Log.i(TAG, "Cleanup successful: ${result.deletedCount} deleted in ${result.durationMs}ms")
                    _uiState.value = CleanupPreviewUiState.Success(
                        deletedCount = result.deletedCount,
                        preservedCount = result.preservedCount,
                        durationMs = result.durationMs
                    )
                } else {
                    val errorMessage = "Cleanup completed with errors:\n${result.errors.joinToString("\n")}"
                    Log.w(TAG, errorMessage)
                    _uiState.value = CleanupPreviewUiState.Error(
                        message = errorMessage,
                        canRetry = false
                    )
                }

            } catch (e: IllegalStateException) {
                // Safety limit exceeded
                Log.e(TAG, "Safety limit exceeded", e)
                _uiState.value = CleanupPreviewUiState.Error(
                    message = e.message ?: "Safety limit exceeded (>90% deletion)",
                    canRetry = false
                )
            } catch (e: Exception) {
                Log.e(TAG, "Cleanup execution failed", e)
                _uiState.value = CleanupPreviewUiState.Error(
                    message = e.message ?: "Cleanup failed unexpectedly",
                    canRetry = true
                )
            }
        }
    }

    /**
     * Retry loading preview after error
     */
    fun retry() {
        loadPreview(currentGracePeriodDays, currentKeepUserApproved)
    }

    /**
     * Load app information for affected packages
     */
    private suspend fun loadAffectedAppsInfo(packageNames: List<String>): List<AffectedAppInfo> {
        return packageNames.mapNotNull { packageName ->
            try {
                // Get deprecated commands for this package
                val deprecated = commandRepo.getDeprecatedCommands(packageName)

                // Get app info from PackageManager
                val pm = application.packageManager
                val appInfo = try {
                    pm.getApplicationInfo(packageName, 0)
                } catch (e: PackageManager.NameNotFoundException) {
                    Log.w(TAG, "App not found: $packageName")
                    null
                }

                AffectedAppInfo(
                    packageName = packageName,
                    appName = appInfo?.loadLabel(pm)?.toString()
                        ?: packageName.substringAfterLast(".").replaceFirstChar { it.uppercase() },
                    commandsToDelete = deprecated.size,
                    icon = appInfo?.loadIcon(pm)
                )
            } catch (e: Exception) {
                Log.w(TAG, "Failed to load app info for $packageName", e)
                null  // Skip apps we can't load
            }
        }.sortedByDescending { it.commandsToDelete }  // Sort by impact (most affected first)
    }
}
