/**
 * AccessibilityViewModel.kt - Comprehensive ViewModel for VoiceOS Accessibility
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Code-Reviewed-By: CCA
 * Created: 2025-08-28
 */
package com.augmentalis.voiceoscore.accessibility.viewmodel

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.augmentalis.voiceoscore.accessibility.VoiceOSService
import com.augmentalis.voiceoscore.accessibility.config.ServiceConfiguration
import com.augmentalis.voiceoscore.accessibility.ui.screens.CommandTestResult
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.random.Random

/**
 * Comprehensive ViewModel for accessibility functionality
 */
class AccessibilityViewModel(application: Application) : AndroidViewModel(application) {
    
    private val context: Context = application.applicationContext
    private val accessibilityManager = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
    
    // Service status
    private val _serviceEnabled = MutableLiveData(false)
    val serviceEnabled: LiveData<Boolean> = _serviceEnabled
    
    private val _overlayPermissionGranted = MutableLiveData(false)
    val overlayPermissionGranted: LiveData<Boolean> = _overlayPermissionGranted
    
    // Configuration
    private val _configuration = MutableLiveData(ServiceConfiguration.createDefault())
    val configuration: LiveData<ServiceConfiguration> = _configuration
    
    // Statistics
    private val _commandsExecuted = MutableStateFlow(0)
    val commandsExecuted: StateFlow<Int> = _commandsExecuted.asStateFlow()
    
    private val _successRate = MutableStateFlow(0.0f)
    val successRate: StateFlow<Float> = _successRate.asStateFlow()
    
    private val _performanceMode = MutableStateFlow("Balanced")
    val performanceMode: StateFlow<String> = _performanceMode.asStateFlow()
    
    // Command testing
    private val _testResults = MutableStateFlow<List<CommandTestResult>>(emptyList())
    val testResults: StateFlow<List<CommandTestResult>> = _testResults.asStateFlow()
    
    private val _isExecutingCommand = MutableStateFlow(false)
    val isExecutingCommand: StateFlow<Boolean> = _isExecutingCommand.asStateFlow()
    
    // Loading states
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()
    
    init {
        checkServiceStatus()
        loadConfiguration()
        startPeriodicStatusCheck()
    }
    
    /**
     * Initialize ViewModel with context if needed
     */
    fun initialize(@Suppress("UNUSED_PARAMETER") context: Context) {
        // Additional initialization if needed
        // Context parameter reserved for future context-specific initialization tasks
        checkAllPermissions()
    }
    
    /**
     * Check if accessibility service is enabled
     */
    fun checkServiceStatus() {
        try {
            val enabledServices = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            )
            
            val componentName = ComponentName(
                context.packageName,
                VoiceOSService::class.java.name
            )
            
            val isEnabled = enabledServices?.contains(componentName.flattenToString()) == true
            _serviceEnabled.postValue(isEnabled)
            
        } catch (e: Exception) {
            _serviceEnabled.postValue(false)
            _errorMessage.value = "Error checking service status: ${e.message}"
        }
    }
    
    /**
     * Check overlay permission
     */
    fun checkOverlayPermission() {
        try {
            val hasPermission = Settings.canDrawOverlays(context)
            _overlayPermissionGranted.postValue(hasPermission)
        } catch (e: Exception) {
            _overlayPermissionGranted.postValue(false)
            _errorMessage.value = "Error checking overlay permission: ${e.message}"
        }
    }
    
    /**
     * Check all permissions at once
     */
    fun checkAllPermissions() {
        viewModelScope.launch {
            _isLoading.value = true
            checkServiceStatus()
            checkOverlayPermission()
            delay(500) // Small delay for UI feedback
            _isLoading.value = false
        }
    }
    
    /**
     * Load configuration from storage or create default
     */
    private fun loadConfiguration() {
        viewModelScope.launch {
            try {
                // For now, use default configuration
                // In a real app, this would load from SharedPreferences or database
                val config = ServiceConfiguration.createDefault()
                _configuration.postValue(config)
                updatePerformanceModeFromConfig(config)
            } catch (e: Exception) {
                _errorMessage.value = "Error loading configuration: ${e.message}"
                _configuration.postValue(ServiceConfiguration.createDefault())
            }
        }
    }
    
    /**
     * Update configuration
     */
    fun updateConfiguration(newConfig: ServiceConfiguration) {
        viewModelScope.launch {
            try {
                _configuration.postValue(newConfig)
                updatePerformanceModeFromConfig(newConfig)
                // In a real app, this would save to SharedPreferences or database
            } catch (e: Exception) {
                _errorMessage.value = "Error updating configuration: ${e.message}"
            }
        }
    }
    
    /**
     * Update performance mode based on configuration
     */
    private fun updatePerformanceModeFromConfig(config: ServiceConfiguration) {
        val mode = when {
            config.commandTimeout <= 3000 -> "High Performance"
            config.commandTimeout >= 8000 -> "Power Saver"
            else -> "Balanced"
        }
        _performanceMode.value = mode
    }
    
    /**
     * Execute a voice command for testing
     */
    fun executeCommand(command: String): CommandTestResult {
        return try {
            _isExecutingCommand.value = true
            val startTime = System.currentTimeMillis()
            
            // Simulate command execution
            val handler = determineHandler(command)
            val success = simulateCommandExecution(command)
            val executionTime = System.currentTimeMillis() - startTime
            
            val result = CommandTestResult(
                command = command,
                timestamp = System.currentTimeMillis(),
                success = success,
                executionTime = executionTime,
                result = if (success) "Command executed successfully" else "Command failed to execute",
                handlerUsed = handler
            )
            
            // Update statistics
            updateCommandStatistics(success)
            
            // Add to results
            val currentResults = _testResults.value.toMutableList()
            currentResults.add(0, result) // Add to beginning
            if (currentResults.size > 10) {
                currentResults.removeAt(currentResults.size - 1) // Keep only last 10
            }
            _testResults.value = currentResults
            
            _isExecutingCommand.value = false
            result
            
        } catch (e: Exception) {
            _isExecutingCommand.value = false
            CommandTestResult(
                command = command,
                timestamp = System.currentTimeMillis(),
                success = false,
                executionTime = 0,
                result = "Error: ${e.message}",
                handlerUsed = "Error"
            )
        }
    }
    
    /**
     * Clear test results
     */
    fun clearTestResults() {
        _testResults.value = emptyList()
    }
    
    /**
     * Update command execution statistics
     */
    private fun updateCommandStatistics(success: Boolean) {
        val currentCount = _commandsExecuted.value
        _commandsExecuted.value = currentCount + 1
        
        // Calculate success rate based on recent results
        val recentResults = _testResults.value.take(20) // Last 20 commands
        if (recentResults.isNotEmpty()) {
            val successCount = recentResults.count { it.success }
            _successRate.value = successCount.toFloat() / recentResults.size
        } else {
            _successRate.value = if (success) 1.0f else 0.0f
        }
    }
    
    /**
     * Determine which handler would process the command
     */
    private fun determineHandler(command: String): String {
        val lowerCommand = command.lowercase()
        
        return when {
            lowerCommand.contains("open") && (lowerCommand.contains("app") || lowerCommand.contains("application")) -> "App Handler"
            lowerCommand.contains("go back") || lowerCommand.contains("home") || lowerCommand.contains("recent") -> "Navigation Handler"
            lowerCommand.contains("settings") || lowerCommand.contains("notification") || lowerCommand.contains("volume") -> "System Handler"
            lowerCommand.contains("tap") || lowerCommand.contains("click") || lowerCommand.contains("scroll") -> "UI Handler"
            lowerCommand.contains("screenshot") || lowerCommand.contains("flashlight") || lowerCommand.contains("airplane") -> "Device Handler"
            lowerCommand.contains("type") || lowerCommand.contains("input") || lowerCommand.contains("text") -> "Input Handler"
            else -> "Action Handler"
        }
    }
    
    /**
     * Simulate command execution with realistic success rates
     */
    private fun simulateCommandExecution(command: String): Boolean {
        // Simulate different success rates based on command complexity
        val successProbability = when {
            command.length < 10 -> 0.95f // Simple commands
            command.length < 20 -> 0.85f // Medium complexity
            else -> 0.75f // Complex commands
        }
        
        return Random.nextFloat() < successProbability
    }
    
    /**
     * Start periodic status checking
     */
    private fun startPeriodicStatusCheck() {
        viewModelScope.launch {
            while (true) {
                delay(5000) // Check every 5 seconds
                checkServiceStatus()
                checkOverlayPermission()
            }
        }
    }
    
    /**
     * Get handler status for dashboard
     */
    fun getHandlerStatuses(): Map<String, Boolean> {
        val config = _configuration.value ?: ServiceConfiguration.createDefault()
        return mapOf(
            "App Handler" to (config.handlersEnabled && config.appLaunchingEnabled),
            "Navigation Handler" to config.handlersEnabled,
            "System Handler" to config.handlersEnabled,
            "UI Handler" to config.handlersEnabled,
            "Device Handler" to config.handlersEnabled,
            "Input Handler" to config.handlersEnabled,
            "Action Handler" to true // Always enabled
        )
    }
    
    /**
     * Get active handlers count
     */
    fun getActiveHandlersCount(): Int {
        val config = _configuration.value ?: ServiceConfiguration.createDefault()
        var count = 1 // Action Handler always enabled
        
        if (config.handlersEnabled) {
            count += 5 // Navigation, System, UI, Device, Input handlers
            if (config.appLaunchingEnabled) count++ // App handler
        }
        
        return count
    }
    
    /**
     * Clear error message
     */
    fun clearError() {
        _errorMessage.value = null
    }
    
    /**
     * Get service info for debugging
     */
    fun getServiceInfo(): String {
        return buildString {
            appendLine("Service Enabled: ${_serviceEnabled.value}")
            appendLine("Overlay Permission: ${_overlayPermissionGranted.value}")
            appendLine("Commands Executed: ${_commandsExecuted.value}")
            appendLine("Success Rate: ${(_successRate.value * 100).toInt()}%")
            appendLine("Performance Mode: ${_performanceMode.value}")
            appendLine("Active Handlers: ${getActiveHandlersCount()}")
        }
    }
    
    /**
     * Simulate service restart (for testing)
     */
    fun restartService() {
        viewModelScope.launch {
            _isLoading.value = true
            delay(2000) // Simulate restart time
            checkAllPermissions()
            _isLoading.value = false
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        // Clean up any resources
    }
}