// Author: Manoj Jhawar
// Code-Reviewed-By: CCA

package com.augmentalis.datamanager.core

import android.content.Context
import android.util.Log
import com.augmentalis.datamanager.entities.AnalyticsSettings
import com.augmentalis.datamanager.io.DataExporter
import com.augmentalis.datamanager.io.DataImporter
import com.augmentalis.datamanager.data.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Database Module - Direct implementation, manages all Room persistence
 */
open class DatabaseModule(private val context: Context) {
    
    companion object {
        private const val TAG = "DatabaseModule"
        private const val MODULE_NAME = "Database"
        private const val MODULE_VERSION = "1.0.0"
        
        const val EVENT_DATABASE_READY = "database.ready"
        const val EVENT_DATABASE_ERROR = "database.error"
        const val EVENT_CLEANUP_STARTED = "database.cleanup.started"
        const val EVENT_CLEANUP_COMPLETED = "database.cleanup.completed"
        const val EVENT_STORAGE_WARNING = "database.storage.warning"
    }
    
    private val moduleScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var isReady = false
    
    // Direct properties - no interface
    val name: String = MODULE_NAME
    val version: String = MODULE_VERSION
    val description: String = "Room-based data persistence manager"
    
    // Repositories
    lateinit var userPreferences: UserPreferenceRepo
        private set
    lateinit var commandHistory: CommandHistoryRepo
        private set
    lateinit var customCommands: CustomCommandRepo
        private set
    lateinit var touchGestures: TouchGestureRepo
        private set
    lateinit var userSequences: UserSequenceRepo
        private set
    lateinit var deviceProfiles: DeviceProfileRepo
        private set
    lateinit var usageStatistics: UsageStatisticRepo
        private set
    lateinit var languageModels: LanguageModelRepo
        private set
    lateinit var retentionSettings: RetentionSettingsRepo
        private set
    lateinit var analyticsSettings: AnalyticsSettingsRepo
        private set
    lateinit var errorReports: ErrorReportRepo
        private set
    lateinit var gestureLearning: GestureLearningRepo
        private set
    
    // Export/Import
    private lateinit var dataExporter: DataExporter
    private lateinit var dataImporter: DataImporter
    
    fun getDependencies(): List<String> = emptyList()
    
    suspend fun initialize(): Boolean {
        if (isReady) return true
        
        return try {
            
            // Initialize Room Database
            if (!DatabaseManager.init(context)) {
                throw Exception("Failed to initialize DatabaseManager")
            }
            
            // Initialize repositories
            initializeRepositories()
            
            // Initialize export/import
            dataExporter = DataExporter(context)
            dataImporter = DataImporter(context)
            
            // Initialize settings if needed
            initializeDefaultSettings()
            
            // Schedule cleanup if enabled
            scheduleAutoCleanup()
            
            isReady = true
            Log.i(TAG, "$MODULE_NAME module initialized successfully")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize $MODULE_NAME module", e)
            isReady = false
            false
        }
    }
    
    private fun initializeRepositories() {
        userPreferences = UserPreferenceRepo()
        commandHistory = CommandHistoryRepo()
        customCommands = CustomCommandRepo()
        touchGestures = TouchGestureRepo()
        userSequences = UserSequenceRepo()
        deviceProfiles = DeviceProfileRepo()
        usageStatistics = UsageStatisticRepo()
        languageModels = LanguageModelRepo()
        retentionSettings = RetentionSettingsRepo()
        analyticsSettings = AnalyticsSettingsRepo()
        errorReports = ErrorReportRepo()
        gestureLearning = GestureLearningRepo()
    }
    
    private suspend fun initializeDefaultSettings() {
        // Initialize retention settings if not present
        if (retentionSettings.getSettings() == null) {
            retentionSettings.initializeDefaults()
        }
        
        // Initialize analytics settings if not present
        if (analyticsSettings.getSettings() == null) {
            analyticsSettings.initializeDefaults()
        }
    }
    
    private fun scheduleAutoCleanup() {
        moduleScope.launch {
            while (isActive) {
                delay(24 * 60 * 60 * 1000L) // Daily check
                performAutoCleanup()
            }
        }
    }
    
    private suspend fun performAutoCleanup() {
        val settings = retentionSettings.getSettings() ?: return
        
        if (!settings.enableAutoCleanup) return
        
        val dbSizeMB = DatabaseManager.getDatabaseSizeMB()
        
        // Check if database size is approaching limit
        if (dbSizeMB > settings.maxDatabaseSizeMB * 0.8f) {
            if (settings.notifyBeforeCleanup) {
                // Notify user about upcoming cleanup
                publishEvent(EVENT_STORAGE_WARNING, mapOf("sizeMB" to dbSizeMB))
            }
        }
        
        publishEvent(EVENT_CLEANUP_STARTED, emptyMap())
        
        try {
            // Cleanup command history
            commandHistory.cleanupOldEntries(
                settings.commandHistoryRetainCount,
                settings.commandHistoryMaxDays
            )
            
            // Cleanup usage statistics
            usageStatistics.cleanupOldStatistics(settings.statisticsRetentionDays)
            
            // Cleanup unused system gestures
            touchGestures.cleanupUnusedSystemGestures()
            
            // Cleanup old error reports
            errorReports.cleanupSentReports()
            
            publishEvent(EVENT_CLEANUP_COMPLETED, mapOf("newSizeMB" to DatabaseManager.getDatabaseSizeMB()))
        } catch (e: Exception) {
            Log.e(TAG, "Error during auto cleanup", e)
        }
    }
    
    suspend fun exportData(includeAll: Boolean = true): String? {
        return try {
            dataExporter.exportToJson(includeAll)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to export data", e)
            null
        }
    }
    
    suspend fun importData(jsonData: String, replaceExisting: Boolean = false): Boolean {
        return try {
            dataImporter.importFromJson(jsonData, replaceExisting)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to import data", e)
            false
        }
    }
    
    suspend fun shutdown() {
        isReady = false
        moduleScope.cancel()
        Log.d(TAG, "Database module shutdown")
    }
    
    fun isReady(): Boolean = isReady
    
    // Data module specific methods
    
    @Suppress("UNUSED_PARAMETER")
    suspend fun trackCommandExecution(command: String, success: Boolean) {
        // Implementation for tracking command execution
        customCommands.findCommandByPhrase(command)?.let {
            customCommands.incrementUsageCount(it.id)
        }
    }
    
    @Suppress("UNUSED_PARAMETER")
    private fun publishEvent(event: String, data: Map<String, Any>) {
        // Publish event through EventBus
        // This would integrate with the Core module's EventBus
    }
    
    fun getDatabaseSizeMB(): Float = DatabaseManager.getDatabaseSizeMB()
    
    suspend fun clearAllData() {
        DatabaseManager.clearAllData()
    }
    
    // Analytics business logic methods (moved from repository)
    suspend fun initializeAnalyticsDefaults() = withContext(Dispatchers.IO) {
        if (analyticsSettings.getSettings() == null) {
            analyticsSettings.insert(AnalyticsSettings(id = 1))
        }
    }
    
    suspend fun setUserConsent(consent: Boolean) = withContext(Dispatchers.IO) {
        val current = analyticsSettings.getSettings() ?: AnalyticsSettings(id = 1)
        val updated = current.copy(
            userConsent = consent,
            consentDate = if (consent) System.currentTimeMillis() else null,
            sendAnonymousReports = consent
        )
        analyticsSettings.update(updated)
    }
    
    suspend fun togglePerformanceTracking() = withContext(Dispatchers.IO) {
        val current = analyticsSettings.getSettings() ?: AnalyticsSettings(id = 1)
        val updated = current.copy(trackPerformance = !current.trackPerformance)
        analyticsSettings.update(updated)
    }
    
    suspend fun setErrorThreshold(threshold: Float) = withContext(Dispatchers.IO) {
        val current = analyticsSettings.getSettings() ?: AnalyticsSettings(id = 1)
        val updated = current.copy(errorThreshold = threshold.coerceIn(0.01f, 0.5f))
        analyticsSettings.update(updated)
    }
    
    suspend fun shouldTrackPerformance(): Boolean = withContext(Dispatchers.IO) {
        analyticsSettings.getSettings()?.trackPerformance ?: false // OFF by default - privacy first
    }
    
    suspend fun shouldSendReports(): Boolean = withContext(Dispatchers.IO) {
        val settings = analyticsSettings.getSettings() ?: return@withContext false
        settings.userConsent && settings.sendAnonymousReports
    }
}