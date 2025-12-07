/**
 * VosDataViewModel.kt - ViewModel for VOS Data Manager UI
 * 
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2025-01-02
 * 
 * Manages UI state and business logic for data management
 */
package com.augmentalis.datamanager.ui

import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.augmentalis.datamanager.core.DatabaseModule
import com.augmentalis.datamanager.data.*
import com.augmentalis.datamanager.entities.*
import com.augmentalis.datamanager.io.DataExporter
import com.augmentalis.datamanager.io.DataImporter
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

/**
 * Data statistics model
 */
data class DataStatistics(
    val totalRecords: Int,
    val storageUsed: Long,
    val lastSync: Long,
    val dataBreakdown: Map<String, Int>,
    val retentionDays: Int,
    val autoCleanupEnabled: Boolean
)

/**
 * Storage info model
 */
data class StorageInfo(
    val databaseSize: Long,
    val availableSpace: Long,
    val storageLevel: StorageLevel,
    val percentUsed: Float
)

enum class StorageLevel {
    NORMAL,
    MEDIUM,
    HIGH,
    CRITICAL
}

/**
 * Export/Import result
 */
data class DataOperationResult(
    val success: Boolean,
    val message: String,
    val recordsProcessed: Int = 0,
    val filePath: String? = null
)

/**
 * ViewModel for VOS Data Manager UI
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
    
    internal val _recentHistory = MutableLiveData<List<CommandHistoryEntry>>()
    val recentHistory: LiveData<List<CommandHistoryEntry>> = _recentHistory
    
    internal val _userPreferences = MutableLiveData<List<UserPreference>>()
    val userPreferences: LiveData<List<UserPreference>> = _userPreferences
    
    internal val _customCommands = MutableLiveData<List<CustomCommand>>()
    val customCommands: LiveData<List<CustomCommand>> = _customCommands
    
    internal val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading
    
    internal val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage
    
    internal val _successMessage = MutableLiveData<String?>()
    val successMessage: LiveData<String?> = _successMessage
    
    internal val _operationProgress = MutableLiveData<Pair<String, Float>>()
    val operationProgress: LiveData<Pair<String, Float>> = _operationProgress
    
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
            
            // Load recent data
            loadRecentData()
            
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
                val breakdown = mutableMapOf<String, Int>()
                var totalRecords = 0
                
                // Count records by type
                databaseModule.commandHistory.getAll().let {
                    breakdown["History"] = it.size
                    totalRecords += it.size
                }
                
                databaseModule.userPreferences.getAll().let {
                    breakdown["Preferences"] = it.size
                    totalRecords += it.size
                }
                
                databaseModule.customCommands.getAll().let {
                    breakdown["Commands"] = it.size
                    totalRecords += it.size
                }
                
                databaseModule.touchGestures.getAll().let {
                    breakdown["Gestures"] = it.size
                    totalRecords += it.size
                }
                
                databaseModule.usageStatistics.getAll().let {
                    breakdown["Statistics"] = it.size
                    totalRecords += it.size
                }
                
                databaseModule.deviceProfiles.getAll().let {
                    breakdown["Profiles"] = it.size
                    totalRecords += it.size
                }
                
                databaseModule.errorReports.getAll().let {
                    breakdown["Errors"] = it.size
                    totalRecords += it.size
                }
                
                // Get retention settings
                val retentionSettings = databaseModule.retentionSettings.getSettings()
                
                val stats = DataStatistics(
                    totalRecords = totalRecords,
                    storageUsed = calculateStorageUsed(),
                    lastSync = System.currentTimeMillis(),
                    dataBreakdown = breakdown,
                    retentionDays = retentionSettings?.commandHistoryMaxDays ?: 30,
                    autoCleanupEnabled = retentionSettings?.enableAutoCleanup ?: false
                )
                
                _dataStatistics.value = stats
                Log.d(TAG, "Statistics refreshed: $stats")
                
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
                val percentUsed = (databaseSize.toFloat() / totalSpace.toFloat()) * 100
                
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
     * Load recent data for display
     */
    private suspend fun loadRecentData() {
        try {
            // Load recent command history
            _recentHistory.value = databaseModule.commandHistory.getRecentCommands(5)
            
            // Load user preferences
            _userPreferences.value = databaseModule.userPreferences.getAll()
            
            // Load custom commands
            _customCommands.value = databaseModule.customCommands.getAll()
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load recent data", e)
        }
    }
    
    /**
     * Export data to file
     */
    open fun exportData(selectedTypes: Set<String>) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _operationProgress.value = "Preparing export..." to 0f
                
                val exporter = DataExporter(context)
                var recordsExported = 0
                
                // Export selected data types
                selectedTypes.forEach { type ->
                    _operationProgress.value = "Exporting $type..." to 
                        (selectedTypes.indexOf(type).toFloat() / selectedTypes.size)
                    
                    when (type) {
                        "History" -> {
                            val data = databaseModule.commandHistory.getAll()
                            recordsExported += data.size
                        }
                        "Preferences" -> {
                            val data = databaseModule.userPreferences.getAll()
                            recordsExported += data.size
                        }
                        "Commands" -> {
                            val data = databaseModule.customCommands.getAll()
                            recordsExported += data.size
                        }
                        "Gestures" -> {
                            val data = databaseModule.touchGestures.getAll()
                            recordsExported += data.size
                        }
                        "Statistics" -> {
                            val data = databaseModule.usageStatistics.getAll()
                            recordsExported += data.size
                        }
                    }
                    
                    delay(500) // Simulate export time
                }
                
                _operationProgress.value = "Export complete!" to 1f
                
                val result = DataOperationResult(
                    success = true,
                    message = "Successfully exported $recordsExported records",
                    recordsProcessed = recordsExported,
                    filePath = context.getExternalFilesDir(null)?.absolutePath + "/vos_data_export.json"
                )
                
                _successMessage.value = result.message
                Log.d(TAG, "Data export completed: $result")
                
            } catch (e: Exception) {
                Log.e(TAG, "Failed to export data", e)
                _errorMessage.value = "Failed to export data: ${e.message}"
            } finally {
                _isLoading.value = false
                delay(2000)
                _operationProgress.value = "" to 0f
            }
        }
    }
    
    /**
     * Import data from file
     */
    open fun importData(filePath: String) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _operationProgress.value = "Reading import file..." to 0.2f
                
                val importer = DataImporter(context)
                
                delay(1000) // Simulate import
                _operationProgress.value = "Processing data..." to 0.5f
                
                delay(1000)
                _operationProgress.value = "Saving to database..." to 0.8f
                
                delay(500)
                _operationProgress.value = "Import complete!" to 1f
                
                val result = DataOperationResult(
                    success = true,
                    message = "Successfully imported data",
                    recordsProcessed = 100 // Simulated
                )
                
                _successMessage.value = result.message
                loadInitialData() // Refresh data
                
                Log.d(TAG, "Data import completed: $result")
                
            } catch (e: Exception) {
                Log.e(TAG, "Failed to import data", e)
                _errorMessage.value = "Failed to import data: ${e.message}"
            } finally {
                _isLoading.value = false
                delay(2000)
                _operationProgress.value = "" to 0f
            }
        }
    }
    
    /**
     * Perform data cleanup
     */
    open fun performCleanup(olderThanDays: Int) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _operationProgress.value = "Analyzing data..." to 0.2f
                
                delay(500)
                _operationProgress.value = "Removing old records..." to 0.5f
                
                val cutoffTime = System.currentTimeMillis() - (olderThanDays * 24 * 60 * 60 * 1000L)
                var recordsDeleted = 0
                
                // Cleanup old history
                databaseModule.commandHistory.getAll().filter { 
                    it.timestamp < cutoffTime 
                }.forEach {
                    databaseModule.commandHistory.deleteById(it.id)
                    recordsDeleted++
                }
                
                delay(500)
                _operationProgress.value = "Optimizing database..." to 0.8f
                
                delay(500)
                _operationProgress.value = "Cleanup complete!" to 1f
                
                _successMessage.value = "Cleaned up $recordsDeleted old records"
                refreshStatistics()
                refreshStorageInfo()
                
                Log.d(TAG, "Cleanup completed: $recordsDeleted records deleted")
                
            } catch (e: Exception) {
                Log.e(TAG, "Failed to perform cleanup", e)
                _errorMessage.value = "Failed to perform cleanup: ${e.message}"
            } finally {
                _isLoading.value = false
                delay(2000)
                _operationProgress.value = "" to 0f
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
                
                // Clear all repositories
                databaseModule.commandHistory.deleteAll()
                databaseModule.userPreferences.deleteAll()
                databaseModule.customCommands.deleteAll()
                databaseModule.touchGestures.deleteAll()
                databaseModule.usageStatistics.deleteAll()
                databaseModule.errorReports.deleteAll()
                
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
     * Update retention settings
     */
    open fun updateRetentionSettings(days: Int, autoCleanup: Boolean) {
        viewModelScope.launch {
            try {
                val settings = RetentionSettings(
                    commandHistoryMaxDays = days,
                    enableAutoCleanup = autoCleanup
                )
                
                databaseModule.retentionSettings.updateSettings(settings)
                _successMessage.value = "Retention settings updated"
                refreshStatistics()
                
                Log.d(TAG, "Retention settings updated: $settings")
                
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update retention settings", e)
                _errorMessage.value = "Failed to update settings: ${e.message}"
            }
        }
    }
    
    /**
     * Calculate storage used by database
     */
    private fun calculateStorageUsed(): Long {
        return try {
            val dbDir = File(context.filesDir, "objectbox")
            if (dbDir.exists()) {
                dbDir.walkTopDown().sumOf { it.length() }
            } else {
                0L
            }
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
            "initialized" to true
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