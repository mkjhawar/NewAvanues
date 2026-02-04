/**
 * VosDataViewModel.kt - ViewModel for VOS Data Manager UI
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2025-01-02
 * Updated: 2026-01-28 - Migrated to KMP structure, uses common data models
 *
 * Manages UI state and business logic for data management.
 * Uses SQLDelight DatabaseManager for data access.
 *
 * Note: This is a simplified stub until the module is enabled.
 */
package com.augmentalis.voiceoscore.managers.voicedatamanager.ui

import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.augmentalis.voiceoscore.managers.voicedatamanager.core.DatabaseModule
import com.augmentalis.voiceoscore.managers.voicedatamanager.core.DatabaseManager
import com.augmentalis.database.dto.CommandHistoryDTO
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// Data classes DataStatistics, StorageInfo, StorageLevel moved to commonMain DataModels.kt

/**
 * ViewModel for VOS Data Manager UI
 *
 * Simplified stub implementation using DatabaseManager directly.
 */
open class VosDataViewModel(private val context: Context) : ViewModel() {

    companion object {
        private const val TAG = "VosDataViewModel"
    }

    private val databaseModule = DatabaseModule(context)

    internal val _dataStatistics = MutableLiveData<DataStatistics>()
    val dataStatistics: LiveData<DataStatistics> = _dataStatistics

    internal val _storageInfo = MutableLiveData<StorageInfo>()
    val storageInfo: LiveData<StorageInfo> = _storageInfo

    internal val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    internal val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    internal val _successMessage = MutableLiveData<String?>()
    val successMessage: LiveData<String?> = _successMessage

    internal val _operationProgress = MutableLiveData<Pair<String, Float>>()
    val operationProgress: LiveData<Pair<String, Float>> = _operationProgress

    internal val _recentHistory = MutableLiveData<List<CommandHistoryDTO>>()
    val recentHistory: LiveData<List<CommandHistoryDTO>> = _recentHistory

    init {
        // Initialize database module
        viewModelScope.launch {
            try {
                if (databaseModule.initialize()) {
                    loadInitialData()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to initialize database module", e)
                _errorMessage.value = "Failed to initialize database: ${e.message}"
            }
        }
    }

    /**
     * Load initial data
     */
    open fun loadData() {
        viewModelScope.launch {
            try {
                loadInitialData()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load data", e)
                _errorMessage.value = "Failed to load data: ${e.message}"
            }
        }
    }

    private suspend fun loadInitialData() {
        _isLoading.value = true

        try {
            // Load statistics
            refreshStatistics()

            // Load storage info
            refreshStorageInfo()

            // Load recent history (stubbed for now)
            _recentHistory.value = emptyList()

            Log.d(TAG, "Initial data loaded successfully")
        } finally {
            _isLoading.value = false
        }
    }

    /**
     * Refresh data statistics
     */
    open fun refreshStatistics() {
        viewModelScope.launch {
            try {
                val stats = DatabaseManager.dbManager.getStats()

                val breakdown = mapOf(
                    "Command History" to stats.commandHistoryCount.toInt(),
                    "Custom Commands" to stats.customCommandCount.toInt(),
                    "Recognition Learning" to stats.recognitionLearningCount.toInt(),
                    "Generated Commands" to stats.generatedCommandCount.toInt(),
                    "Scraped Apps" to stats.scrapedAppCount.toInt(),
                    "Error Reports" to stats.errorReportCount.toInt()
                )

                val totalRecords = breakdown.values.sum()

                val dataStats = DataStatistics(
                    totalRecords = totalRecords,
                    storageUsed = calculateStorageUsed(),
                    lastSync = System.currentTimeMillis(),
                    dataBreakdown = breakdown,
                    retentionDays = 30, // Default value
                    autoCleanupEnabled = false // Default value
                )

                _dataStatistics.value = dataStats
                Log.d(TAG, "Statistics refreshed: $dataStats")

            } catch (e: Exception) {
                Log.e(TAG, "Failed to refresh statistics", e)
                _errorMessage.value = "Failed to refresh statistics: ${e.message}"
            }
        }
    }

    /**
     * Refresh storage information
     */
    open fun refreshStorageInfo() {
        viewModelScope.launch {
            try {
                val databaseSize = calculateStorageUsed()
                val availableSpace = context.filesDir.freeSpace
                val totalSpace = context.filesDir.totalSpace
                val percentUsed = if (totalSpace > 0) {
                    (databaseSize.toFloat() / totalSpace.toFloat()) * 100
                } else {
                    0f
                }

                val storageLevel = when {
                    percentUsed < 50 -> StorageLevel.NORMAL
                    percentUsed < 75 -> StorageLevel.MEDIUM
                    percentUsed < 90 -> StorageLevel.HIGH
                    else -> StorageLevel.CRITICAL
                }

                val info = StorageInfo(
                    databaseSize = databaseSize,
                    availableSpace = availableSpace,
                    storageLevel = storageLevel,
                    percentUsed = percentUsed
                )

                _storageInfo.value = info
                Log.d(TAG, "Storage info refreshed: $info")

            } catch (e: Exception) {
                Log.e(TAG, "Failed to refresh storage info", e)
                _errorMessage.value = "Failed to refresh storage info: ${e.message}"
            }
        }
    }

    /**
     * Clear all data
     */
    open fun clearAllData() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _operationProgress.value = "Clearing all data..." to 0.5f

                DatabaseManager.clearAllData()

                delay(1000)
                _operationProgress.value = "Data cleared!" to 1f

                _successMessage.value = "All data has been cleared"
                loadInitialData()

                Log.d(TAG, "All data cleared successfully")

            } catch (e: Exception) {
                Log.e(TAG, "Failed to clear data", e)
                _errorMessage.value = "Failed to clear data: ${e.message}"
            } finally {
                _isLoading.value = false
                delay(2000)
                _operationProgress.value = "" to 0f
            }
        }
    }

    /**
     * Calculate storage used by database
     */
    private fun calculateStorageUsed(): Long {
        return try {
            val databaseSizeMB = DatabaseManager.getDatabaseSizeMB()
            (databaseSizeMB * 1024 * 1024).toLong()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to calculate storage", e)
            0L
        }
    }

    /**
     * Get database info
     */
    open fun getDatabaseInfo(): Map<String, Any> {
        return mapOf(
            "version" to databaseModule.version,
            "name" to databaseModule.name,
            "description" to databaseModule.description,
            "initialized" to databaseModule.isReady()
        )
    }

    /**
     * Clear error message
     */
    open fun clearError() {
        _errorMessage.value = null
    }

    /**
     * Clear success message
     */
    open fun clearSuccess() {
        _successMessage.value = null
    }

    /**
     * Export data to file
     * TODO: Implement actual export logic
     */
    open fun exportData(selectedTypes: Set<String>) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _operationProgress.value = "Exporting data..." to 0.5f

                Log.w(TAG, "Export data not yet implemented: $selectedTypes")
                delay(1000)

                _successMessage.value = "Export functionality coming soon"
            } catch (e: Exception) {
                Log.e(TAG, "Export failed", e)
                _errorMessage.value = "Export failed: ${e.message}"
            } finally {
                _isLoading.value = false
                delay(2000)
                _operationProgress.value = "" to 0f
            }
        }
    }

    /**
     * Import data from file
     * TODO: Implement actual import logic
     */
    open fun importData(filePath: String) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _operationProgress.value = "Importing data..." to 0.5f

                Log.w(TAG, "Import data not yet implemented: $filePath")
                delay(1000)

                _successMessage.value = "Import functionality coming soon"
            } catch (e: Exception) {
                Log.e(TAG, "Import failed", e)
                _errorMessage.value = "Import failed: ${e.message}"
            } finally {
                _isLoading.value = false
                delay(2000)
                _operationProgress.value = "" to 0f
            }
        }
    }

    /**
     * Perform cleanup of old data
     * TODO: Implement actual cleanup logic
     */
    open fun performCleanup(days: Int) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _operationProgress.value = "Cleaning up data older than $days days..." to 0.5f

                Log.w(TAG, "Cleanup not yet implemented: $days days")
                delay(1000)

                _successMessage.value = "Cleanup functionality coming soon"
                loadInitialData()
            } catch (e: Exception) {
                Log.e(TAG, "Cleanup failed", e)
                _errorMessage.value = "Cleanup failed: ${e.message}"
            } finally {
                _isLoading.value = false
                delay(2000)
                _operationProgress.value = "" to 0f
            }
        }
    }

    /**
     * Update retention settings
     * TODO: Implement actual retention settings logic
     */
    open fun updateRetentionSettings(days: Int, autoCleanup: Boolean) {
        viewModelScope.launch {
            try {
                _isLoading.value = true

                Log.w(TAG, "Update retention settings not yet implemented: $days days, autoCleanup=$autoCleanup")
                delay(500)

                _successMessage.value = "Settings will be saved in future update"
                refreshStatistics()
            } catch (e: Exception) {
                Log.e(TAG, "Update retention settings failed", e)
                _errorMessage.value = "Failed to update settings: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        Log.d(TAG, "VosDataViewModel cleared")
    }
}

/**
 * ViewModelProvider Factory for VosDataViewModel
 */
class VosDataViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(VosDataViewModel::class.java)) {
            return VosDataViewModel(context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
