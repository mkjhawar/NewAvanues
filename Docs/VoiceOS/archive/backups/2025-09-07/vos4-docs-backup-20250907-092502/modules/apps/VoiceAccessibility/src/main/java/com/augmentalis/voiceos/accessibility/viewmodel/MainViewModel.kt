/**
 * MainViewModel.kt - Main screen view model for VoiceOS Accessibility
 * 
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2025-01-28
 * 
 * ViewModel for managing main activity state, permissions, and service status.
 * Direct implementation following VOS4 standards.
 */
package com.augmentalis.voiceos.accessibility.viewmodel

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.ComponentName
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.augmentalis.voiceos.accessibility.client.VoiceRecognitionClient
import com.augmentalis.voiceos.accessibility.config.ServiceConfiguration
import com.augmentalis.voiceos.accessibility.service.VoiceAccessibilityService
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Main screen view model
 * Direct implementation - no interfaces (VOS4 compliance)
 */
class MainViewModel : ViewModel() {
    
    private var context: Context? = null
    
    // Service status
    private val _serviceEnabled = MutableLiveData(false)
    val serviceEnabled: LiveData<Boolean> = _serviceEnabled
    
    // Permissions
    private val _overlayPermissionGranted = MutableLiveData(false)
    val overlayPermissionGranted: LiveData<Boolean> = _overlayPermissionGranted
    
    private val _writeSettingsPermissionGranted = MutableLiveData(false)
    val writeSettingsPermissionGranted: LiveData<Boolean> = _writeSettingsPermissionGranted
    
    // Configuration
    private val _configuration = MutableLiveData(ServiceConfiguration.createDefault())
    val configuration: LiveData<ServiceConfiguration> = _configuration
    
    // Loading states
    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading
    
    // Error states
    private val _errorMessage = MutableLiveData<String?>(null)
    val errorMessage: LiveData<String?> = _errorMessage
    
    // Engine selection for testing
    private val _selectedEngine = MutableLiveData("vivoka")
    val selectedEngine: LiveData<String> = _selectedEngine
    
    private val _isRecognizing = MutableLiveData(false)
    val isRecognizing: LiveData<Boolean> = _isRecognizing
    
    // Voice recognition client
    private var voiceRecognitionClient: VoiceRecognitionClient? = null
    
    /**
     * Initialize the ViewModel with context
     */
    fun initialize(context: Context) {
        this.context = context.applicationContext
        loadConfiguration()
        initializeVoiceClient()
    }
    
    /**
     * Initialize voice recognition client
     */
    private fun initializeVoiceClient() {
        context?.let { ctx ->
            voiceRecognitionClient = VoiceRecognitionClient(ctx).apply {
                setConnectionCallback(object : VoiceRecognitionClient.ConnectionCallback {
                    override fun onConnected() {
                        // Client connected
                    }
                    
                    override fun onDisconnected() {
                        _isRecognizing.postValue(false)
                    }
                    
                    override fun onError(error: String) {
                        _errorMessage.postValue(error)
                        _isRecognizing.postValue(false)
                    }
                })
                
                setRecognitionCallback(object : VoiceRecognitionClient.RecognitionCallback {
                    override fun onResult(text: String, confidence: Float, isFinal: Boolean) {
                        // Handle recognition results
                        if (isFinal) {
                            _errorMessage.postValue("Recognized: $text (${(confidence * 100).toInt()}%)")
                        }
                    }
                    
                    override fun onPartialResult(text: String) {
                        // Handle partial results
                    }
                    
                    override fun onError(errorCode: Int, message: String?) {
                        _errorMessage.postValue("Recognition error: $message")
                        _isRecognizing.postValue(false)
                    }
                    
                    override fun onStateChanged(state: Int, message: String?) {
                        // Handle state changes
                    }
                })
                
                connect()
            }
        }
    }
    
    /**
     * Check all permissions and service status
     */
    fun checkAllPermissions() {
        viewModelScope.launch {
            _isLoading.value = true
            
            try {
                checkServiceStatus()
                checkOverlayPermission()
                checkWriteSettingsPermission()
            } catch (e: Exception) {
                _errorMessage.value = "Error checking permissions: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Check if accessibility service is enabled
     */
    fun checkServiceStatus() {
        val ctx = context ?: return
        
        viewModelScope.launch {
            try {
                val accessibilityManager = ctx.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
                val enabledServices = accessibilityManager.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
                
                val serviceName = ComponentName(ctx, VoiceAccessibilityService::class.java).flattenToString()
                val isEnabled = enabledServices.any { service ->
                    service.id == serviceName
                }
                
                _serviceEnabled.value = isEnabled
            } catch (e: Exception) {
                _serviceEnabled.value = false
                _errorMessage.value = "Error checking service status: ${e.message}"
            }
        }
    }
    
    /**
     * Check system overlay permission
     */
    fun checkOverlayPermission() {
        val ctx = context ?: return
        
        viewModelScope.launch {
            try {
                val hasPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    Settings.canDrawOverlays(ctx)
                } else {
                    true // Not needed on older versions
                }
                
                _overlayPermissionGranted.value = hasPermission
            } catch (e: Exception) {
                _overlayPermissionGranted.value = false
                _errorMessage.value = "Error checking overlay permission: ${e.message}"
            }
        }
    }
    
    /**
     * Check write settings permission
     */
    fun checkWriteSettingsPermission() {
        val ctx = context ?: return
        
        viewModelScope.launch {
            try {
                val hasPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    Settings.System.canWrite(ctx)
                } else {
                    true // Granted by default on older versions
                }
                
                _writeSettingsPermissionGranted.value = hasPermission
            } catch (e: Exception) {
                _writeSettingsPermissionGranted.value = false
                _errorMessage.value = "Error checking write settings permission: ${e.message}"
            }
        }
    }
    
    /**
     * Load configuration from preferences
     */
    fun loadConfiguration() {
        val ctx = context ?: return
        
        viewModelScope.launch {
            try {
                val config = ServiceConfiguration.loadFromPreferences(ctx)
                _configuration.value = config
            } catch (e: Exception) {
                _errorMessage.value = "Error loading configuration: ${e.message}"
                _configuration.value = ServiceConfiguration.createDefault()
            }
        }
    }
    
    /**
     * Save configuration to preferences
     */
    fun saveConfiguration(config: ServiceConfiguration) {
        val ctx = context ?: return
        
        viewModelScope.launch {
            try {
                config.saveToPreferences(ctx)
                _configuration.value = config
            } catch (e: Exception) {
                _errorMessage.value = "Error saving configuration: ${e.message}"
            }
        }
    }
    
    /**
     * Select speech recognition engine
     */
    fun selectEngine(engine: String) {
        _selectedEngine.value = engine
        saveEnginePreference(engine)
    }
    
    /**
     * Start recognition with selected engine
     */
    fun startRecognitionWithEngine(engine: String) {
        viewModelScope.launch {
            try {
                selectEngine(engine)
                voiceRecognitionClient?.startRecognition(engine, "en-US", 0)
                _isRecognizing.value = true
                _errorMessage.value = "Starting $engine engine..."
            } catch (e: Exception) {
                _errorMessage.value = "Failed to start $engine: ${e.message}"
                _isRecognizing.value = false
            }
        }
    }
    
    /**
     * Stop recognition
     */
    fun stopRecognition() {
        viewModelScope.launch {
            try {
                voiceRecognitionClient?.stopRecognition()
                _isRecognizing.value = false
                _errorMessage.value = "Recognition stopped"
            } catch (e: Exception) {
                _errorMessage.value = "Failed to stop: ${e.message}"
            }
        }
    }
    
    /**
     * Save engine preference
     */
    private fun saveEnginePreference(engine: String) {
        context?.let { ctx ->
            val prefs = ctx.getSharedPreferences("voice_recognition_prefs", Context.MODE_PRIVATE)
            prefs.edit().putString("selected_engine", engine).apply()
        }
    }
    
    /**
     * Toggle service enabled state (opens accessibility settings)
     */
    fun toggleService() {
        // This will be handled by the UI opening accessibility settings
        // We can't programmatically enable/disable accessibility services
        checkServiceStatus()
    }
    
    /**
     * Request system overlay permission
     */
    fun requestOverlayPermission(): Uri? {
        val ctx = context ?: return null
        
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Uri.parse("package:${ctx.packageName}")
        } else {
            null
        }
    }
    
    /**
     * Request write settings permission
     */
    fun requestWriteSettingsPermission(): Uri? {
        val ctx = context ?: return null
        
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Uri.parse("package:${ctx.packageName}")
        } else {
            null
        }
    }
    
    /**
     * Clear error message
     */
    fun clearError() {
        _errorMessage.value = null
    }
    
    /**
     * Refresh all data
     */
    fun refresh() {
        viewModelScope.launch {
            delay(500) // Small delay for UI feedback
            loadConfiguration()
            checkAllPermissions()
        }
    }
    
    /**
     * Get permission status summary
     */
    fun getPermissionSummary(): PermissionSummary {
        return PermissionSummary(
            serviceEnabled = _serviceEnabled.value ?: false,
            overlayPermission = _overlayPermissionGranted.value ?: false,
            writeSettingsPermission = _writeSettingsPermissionGranted.value ?: false
        )
    }
    
    /**
     * Check if all required permissions are granted
     */
    fun areAllPermissionsGranted(): Boolean {
        val summary = getPermissionSummary()
        return summary.serviceEnabled && summary.overlayPermission && summary.writeSettingsPermission
    }
}

/**
 * Data class for permission summary
 */
data class PermissionSummary(
    val serviceEnabled: Boolean,
    val overlayPermission: Boolean,
    val writeSettingsPermission: Boolean
) {
    val allGranted: Boolean
        get() = serviceEnabled && overlayPermission && writeSettingsPermission
        
    val grantedCount: Int
        get() = listOf(serviceEnabled, overlayPermission, writeSettingsPermission).count { it }
        
    val totalCount: Int = 3
    
    val percentage: Float
        get() = grantedCount.toFloat() / totalCount.toFloat()
}