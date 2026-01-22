/**
 * SettingsViewModel.kt - Dedicated ViewModel for settings management
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Code-Reviewed-By: CCA
 * Created: 2025-08-28
 */
package com.augmentalis.voiceoscore.accessibility.viewmodel

import android.app.Application
import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.augmentalis.voiceoscore.accessibility.config.ServiceConfiguration
import com.augmentalis.voiceoscore.accessibility.utils.Const.broadcastConfigUpdated
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Performance mode enumeration with display names and configuration mappings
 */
enum class PerformanceMode(
    val displayName: String,
    val commandTimeout: Long,
    val maxCacheSize: Int,
    val description: String
) {
    HIGH_PERFORMANCE(
        displayName = "High Performance",
        commandTimeout = 2000L,
        maxCacheSize = 200,
        description = "Faster response times, higher battery usage"
    ),
    BALANCED(
        displayName = "Balanced",
        commandTimeout = 5000L,
        maxCacheSize = 100,
        description = "Good balance of performance and battery life"
    ),
    POWER_SAVER(
        displayName = "Power Saver",
        commandTimeout = 10000L,
        maxCacheSize = 50,
        description = "Slower response times, optimized for battery life"
    )
}

/**
 * Handler information for UI display and state management
 */
data class HandlerInfo(
    val id: String,
    val name: String,
    val description: String,
    val isCore: Boolean = false, // Core handlers cannot be disabled
    val requiresOtherHandlers: Boolean = false
)

/**
 * Settings ViewModel for comprehensive settings management
 */
class SettingsViewModel(private val context: Application) : AndroidViewModel(context) {


    companion object {
        private const val TAG = "SettingsViewModel"

        // Handler definitions
        val HANDLER_DEFINITIONS = listOf(
            HandlerInfo(
                id = "action_handler",
                name = "Action Handler",
                description = "Core action processing and command coordination",
                isCore = true
            ),
            HandlerInfo(
                id = "app_handler",
                name = "App Handler",
                description = "Application launching and app switching"
            ),
            HandlerInfo(
                id = "device_handler",
                name = "Device Handler",
                description = "Device controls (volume, brightness, etc.)"
            ),
            HandlerInfo(
                id = "input_handler",
                name = "Input Handler",
                description = "Text input and keyboard commands"
            ),
            HandlerInfo(
                id = "navigation_handler",
                name = "Navigation Handler",
                description = "Screen navigation and back/home commands"
            ),
            HandlerInfo(
                id = "system_handler",
                name = "System Handler",
                description = "System settings and notifications"
            ),
            HandlerInfo(
                id = "ui_handler",
                name = "UI Handler",
                description = "UI element interaction and screen tapping"
            )
        )
    }

    // Core configuration state
    private val _configuration = MutableStateFlow(ServiceConfiguration.createDefault())
    val configuration: StateFlow<ServiceConfiguration> = _configuration.asStateFlow()

    // Performance mode state  
    private val _performanceMode = MutableStateFlow(PerformanceMode.BALANCED)
    val performanceMode: StateFlow<PerformanceMode> = _performanceMode.asStateFlow()

    // Handler states
    private val _handlerStates = MutableStateFlow<Map<String, Boolean>>(emptyMap())
    val handlerStates: StateFlow<Map<String, Boolean>> = _handlerStates.asStateFlow()

    // Cursor settings
    private val _cursorEnabled = MutableStateFlow(true)
    val cursorEnabled: StateFlow<Boolean> = _cursorEnabled.asStateFlow()

    private val _cursorSize = MutableStateFlow(48f)
    val cursorSize: StateFlow<Float> = _cursorSize.asStateFlow()

    private val _cursorSpeed = MutableStateFlow(1.0f)
    val cursorSpeed: StateFlow<Float> = _cursorSpeed.asStateFlow()

    private val _cursorColor = MutableStateFlow(0xFF4285F4.toInt())
    val cursorColor: StateFlow<Int> = _cursorColor.asStateFlow()

    // Cache configuration
    private val _cacheEnabled = MutableStateFlow(true)
    val cacheEnabled: StateFlow<Boolean> = _cacheEnabled.asStateFlow()

    private val _maxCacheSize = MutableStateFlow(100)
    val maxCacheSize: StateFlow<Int> = _maxCacheSize.asStateFlow()

    // Advanced settings
    private val _uiScrapingEnabled = MutableStateFlow(false)
    val uiScrapingEnabled: StateFlow<Boolean> = _uiScrapingEnabled.asStateFlow()

    private val _dynamicCommandsEnabled = MutableStateFlow(true)
    val dynamicCommandsEnabled: StateFlow<Boolean> = _dynamicCommandsEnabled.asStateFlow()

    private val _verboseLogging = MutableStateFlow(false)
    val verboseLogging: StateFlow<Boolean> = _verboseLogging.asStateFlow()

    private val _showToasts = MutableStateFlow(true)
    val showToasts: StateFlow<Boolean> = _showToasts.asStateFlow()

    // Loading and error states
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    init {
        loadConfiguration()
        initializeHandlerStates()
    }

    /**
     * Load configuration from storage
     */
    private fun loadConfiguration() {
        viewModelScope.launch {
            try {
                _isLoading.value = true

                // Load configuration from SharedPreferences
                val config = ServiceConfiguration.loadFromPreferences(context)
                _configuration.value = config

                // Update individual state flows from configuration
                updateStateFromConfiguration(config)

                Log.d(TAG, "Configuration loaded successfully")

            } catch (e: Exception) {
                Log.e(TAG, "Error loading configuration", e)
                _errorMessage.value = "Failed to load settings: ${e.message}"

                // Use default configuration on error
                val defaultConfig = ServiceConfiguration.createDefault()
                _configuration.value = defaultConfig
                updateStateFromConfiguration(defaultConfig)

            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Save current configuration to storage
     */
    private fun saveConfiguration() {
        viewModelScope.launch {
            try {
                val currentConfig = _configuration.value
                currentConfig.saveToPreferences(context)
                Log.d(TAG, "Configuration saved successfully")

            } catch (e: Exception) {
                Log.e(TAG, "Error saving configuration", e)
                _errorMessage.value = "Failed to save settings: ${e.message}"
            }
        }
    }

    /**
     * Update state flows from configuration
     */
    private fun updateStateFromConfiguration(config: ServiceConfiguration) {
        // Performance mode
        _performanceMode.value = when {
            config.commandTimeout <= 3000 -> PerformanceMode.HIGH_PERFORMANCE
            config.commandTimeout >= 8000 -> PerformanceMode.POWER_SAVER
            else -> PerformanceMode.BALANCED
        }

        // Cursor settings
        _cursorEnabled.value = config.cursorEnabled
        _cursorSize.value = config.cursorSize
        _cursorSpeed.value = config.cursorSpeed
        _cursorColor.value = config.cursorColor

        // Cache settings
        _cacheEnabled.value = config.commandCachingEnabled
        _maxCacheSize.value = config.maxCacheSize

        // Advanced settings
        _uiScrapingEnabled.value = config.uiScrapingEnabled
        _dynamicCommandsEnabled.value = config.dynamicCommandsEnabled
        _verboseLogging.value = config.verboseLogging
        _showToasts.value = config.showToasts

        // Update handler states
        updateHandlerStatesFromConfig(config)
    }

    /**
     * Initialize handler states map
     */
    private fun initializeHandlerStates() {
        val initialStates = HANDLER_DEFINITIONS.associate { handler ->
            handler.id to when (handler.id) {
                "action_handler" -> true // Always enabled
                "app_handler" -> _configuration.value.appLaunchingEnabled
                else -> _configuration.value.handlersEnabled
            }
        }
        _handlerStates.value = initialStates
    }

    /**
     * Update handler states from configuration
     */
    private fun updateHandlerStatesFromConfig(config: ServiceConfiguration) {
        val updatedStates = HANDLER_DEFINITIONS.associate { handler ->
            handler.id to when (handler.id) {
                "action_handler" -> true // Always enabled
                "app_handler" -> config.appLaunchingEnabled
                else -> config.handlersEnabled
            }
        }
        _handlerStates.value = updatedStates
    }

    /**
     * Update performance mode and apply changes
     */
    fun updatePerformanceMode(mode: PerformanceMode) {
        viewModelScope.launch {
            try {
                _performanceMode.value = mode

                // Update configuration with performance mode settings
                val updatedConfig = _configuration.value.copy(
                    commandTimeout = mode.commandTimeout,
                    maxCacheSize = mode.maxCacheSize
                )

                _configuration.value = updatedConfig
                _maxCacheSize.value = mode.maxCacheSize

                saveConfiguration()

            } catch (e: Exception) {
                Log.e(TAG, "Error updating performance mode", e)
                _errorMessage.value = "Failed to update performance mode: ${e.message}"
            }
        }
    }

    /**
     * Toggle handler enabled state
     */
    fun toggleHandler(handlerId: String, enabled: Boolean) {
        viewModelScope.launch {
            try {
                // Check if handler can be disabled
                val handler = HANDLER_DEFINITIONS.find { it.id == handlerId }
                if (handler?.isCore == true) {
                    Log.w(TAG, "Cannot disable core handler: $handlerId")
                    return@launch
                }

                // Update handler states
                val updatedStates = _handlerStates.value.toMutableMap()
                updatedStates[handlerId] = enabled
                _handlerStates.value = updatedStates

                // Update configuration based on handler
                val updatedConfig = when (handlerId) {
                    "app_handler" -> _configuration.value.copy(appLaunchingEnabled = enabled)
                    else -> {
                        // For general handlers, update the main handler flag
                        val anyHandlerEnabled = updatedStates.filterKeys { it != "action_handler" }.values.any { it }
                        _configuration.value.copy(handlersEnabled = anyHandlerEnabled)
                    }
                }

                _configuration.value = updatedConfig
                saveConfiguration()

            } catch (e: Exception) {
                Log.e(TAG, "Error toggling handler: $handlerId", e)
                _errorMessage.value = "Failed to toggle handler: ${e.message}"
            }
        }
    }

    /**
     * Update cursor settings
     */
    fun updateCursorEnabled(enabled: Boolean) {
        viewModelScope.launch {
            try {
                _cursorEnabled.value = enabled
                val updatedConfig = _configuration.value.copy(cursorEnabled = enabled)
                _configuration.value = updatedConfig
                saveConfiguration()

            } catch (e: Exception) {
                Log.e(TAG, "Error updating cursor enabled", e)
                _errorMessage.value = "Failed to update cursor setting: ${e.message}"
            }
        }
    }

    fun updateCursorSize(size: Float) {
        viewModelScope.launch {
            try {
                _cursorSize.value = size
                val updatedConfig = _configuration.value.copy(cursorSize = size)
                _configuration.value = updatedConfig
                saveConfiguration()

            } catch (e: Exception) {
                Log.e(TAG, "Error updating cursor size", e)
                _errorMessage.value = "Failed to update cursor size: ${e.message}"
            }
        }
    }

    fun updateCursorSpeed(speed: Float) {
        viewModelScope.launch {
            try {
                _cursorSpeed.value = speed
                val updatedConfig = _configuration.value.copy(cursorSpeed = speed)
                _configuration.value = updatedConfig
                saveConfiguration()

            } catch (e: Exception) {
                Log.e(TAG, "Error updating cursor speed", e)
                _errorMessage.value = "Failed to update cursor speed: ${e.message}"
            }
        }
    }

    fun updateCursorColor(color: Int) {
        viewModelScope.launch {
            try {
                _cursorColor.value = color
                val updatedConfig = _configuration.value.copy(cursorColor = color)
                _configuration.value = updatedConfig
                saveConfiguration()

            } catch (e: Exception) {
                Log.e(TAG, "Error updating cursor color", e)
                _errorMessage.value = "Failed to update cursor color: ${e.message}"
            }
        }
    }

    /**
     * Update cache settings
     */
    fun updateCacheEnabled(enabled: Boolean) {
        viewModelScope.launch {
            try {
                _cacheEnabled.value = enabled
                val updatedConfig = _configuration.value.copy(commandCachingEnabled = enabled)
                _configuration.value = updatedConfig
                saveConfiguration()

            } catch (e: Exception) {
                Log.e(TAG, "Error updating cache enabled", e)
                _errorMessage.value = "Failed to update cache setting: ${e.message}"
            }
        }
    }

    fun updateMaxCacheSize(size: Int) {
        viewModelScope.launch {
            try {
                _maxCacheSize.value = size
                val updatedConfig = _configuration.value.copy(maxCacheSize = size)
                _configuration.value = updatedConfig
                saveConfiguration()

            } catch (e: Exception) {
                Log.e(TAG, "Error updating cache size", e)
                _errorMessage.value = "Failed to update cache size: ${e.message}"
            }
        }
    }

    /**
     * Update advanced settings
     */
    fun updateUiScrapingEnabled(enabled: Boolean) {
        viewModelScope.launch {
            try {
                _uiScrapingEnabled.value = enabled
                val updatedConfig = _configuration.value.copy(uiScrapingEnabled = enabled)
                _configuration.value = updatedConfig
                saveConfiguration()

            } catch (e: Exception) {
                Log.e(TAG, "Error updating UI scraping", e)
                _errorMessage.value = "Failed to update UI scraping: ${e.message}"
            }
        }
    }

    fun updateDynamicCommandsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            try {
                _dynamicCommandsEnabled.value = enabled
                val updatedConfig = _configuration.value.copy(dynamicCommandsEnabled = enabled)
                _configuration.value = updatedConfig
                saveConfiguration()

            } catch (e: Exception) {
                Log.e(TAG, "Error updating dynamic commands", e)
                _errorMessage.value = "Failed to update dynamic commands: ${e.message}"
            }
        }
    }

    fun updateVerboseLogging(enabled: Boolean) {
        viewModelScope.launch {
            try {
                _verboseLogging.value = enabled
                val updatedConfig = _configuration.value.copy(verboseLogging = enabled)
                _configuration.value = updatedConfig
                saveConfiguration()

            } catch (e: Exception) {
                Log.e(TAG, "Error updating verbose logging", e)
                _errorMessage.value = "Failed to update verbose logging: ${e.message}"
            }
        }
    }

    fun updateShowToasts(enabled: Boolean) {
        viewModelScope.launch {
            try {
                _showToasts.value = enabled
                val updatedConfig = _configuration.value.copy(showToasts = enabled)
                _configuration.value = updatedConfig
                saveConfiguration()

            } catch (e: Exception) {
                Log.e(TAG, "Error updating show toasts", e)
                _errorMessage.value = "Failed to update toast setting: ${e.message}"
            }
        }
    }

    /**
     * Reset all settings to defaults
     */
    fun resetToDefaults() {
        viewModelScope.launch {
            try {
                _isLoading.value = true

                val defaultConfig = ServiceConfiguration.createDefault()
                _configuration.value = defaultConfig
                updateStateFromConfiguration(defaultConfig)

                saveConfiguration()

                Log.d(TAG, "Settings reset to defaults")

            } catch (e: Exception) {
                Log.e(TAG, "Error resetting settings", e)
                _errorMessage.value = "Failed to reset settings: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Clear error message
     */
    fun clearError() {
        _errorMessage.value = null
    }

    /**
     * Get handler definition by ID
     */
    fun getHandlerDefinition(handlerId: String): HandlerInfo? {
        return HANDLER_DEFINITIONS.find { it.id == handlerId }
    }

    /**
     * Get all handler definitions
     */
    fun getAllHandlerDefinitions(): List<HandlerInfo> {
        return HANDLER_DEFINITIONS
    }

    /**
     * Get active handlers count
     */
    fun getActiveHandlersCount(): Int {
        return _handlerStates.value.values.count { it }
    }

    /**
     * Check if configuration is valid
     */
    fun validateConfiguration(): Boolean {
        return _configuration.value.validate()
    }

    // Dropdown items
    val options = listOf(
        "English USA",
        "English Australia",
        "English China",
        "English UK",
        "English India",
        "English Japan",
        "English Malaysia",
        "English South",
        "Japanese",
        "Spanish",
        "Spanish Spain",
        "Spanish Maxican",
        "Chinese",
        "Cantonese",
        "Cantonese China",
        "Cantonese Hong",
        "Mandarin",
        "Mandarin China",
        "Mandarin Taiwan",
        "Hindi",
        "Portuguese",
        "Portuguese Portugal",
        "Portuguese Brazil",
        "Arabic",
        "Arabic Saudi",
        "Arabic Persian",
        "Danish",
        "Dutch",
        "Bulgarian",
        "Czech",
        "Russian",
        "Polish",
        "Italian",
        "German",
        "French",
        "French France",
        "French Canada",
        "Korean",
        "Indonesian",
        "Finnish",
        "Greek",
        "Hebrew",
        "Hungarian",
        "Norwegian",
        "Slovak",
        "Swedish",
        "Thai",
        "Turkish",
        "Farsi",
        "Marathi",
        "Malay"
    )

    // Currently selected option
    var selectedOption = mutableStateOf(options.first())

    // Function to update selected option
    fun onOptionSelected(option: String) {
        selectedOption.value = option

        viewModelScope.launch {
            try {
                val updatedConfig = _configuration.value.copy(voiceLanguage = "hi")
                _configuration.value = updatedConfig
                val currentConfig = _configuration.value
                currentConfig.saveToPreferences(context)
                Log.d(TAG, "Configuration saved successfully")
                delay(5_00)
                Log.i(TAG, "CHANGE_LANG onOptionSelected: $updatedConfig")
                context.broadcastConfigUpdated("hi")
                Log.i(TAG, "CHANGE_LANG onOptionSelected: $updatedConfig")
            } catch (e: Exception) {
                Log.e(TAG, "Error saving configuration", e)
                _errorMessage.value = "Failed to save settings: ${e.message}"
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        Log.d(TAG, "SettingsViewModel cleared")
    }
}