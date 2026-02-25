/**
 * LicenseViewModel.kt - ViewModel for License Manager UI
 * 
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Created: 2025-01-02
 * 
 * Manages UI state and business logic for License Manager
 */
package com.augmentalis.licensemanager.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.augmentalis.licensemanager.LicensingModule
import com.augmentalis.licensemanager.SubscriptionState
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

/**
 * ViewModel for License Manager UI
 */
class LicenseViewModel(private val context: Context) : ViewModel() {
    
    companion object {
        private const val TAG = "LicenseViewModel"
        private const val PURCHASE_URL = "https://voiceos.com/purchase"
        private const val SUPPORT_URL = "https://voiceos.com/support"
    }
    
    private val licensingModule = LicensingModule.getInstance(context)
    
    private val _subscriptionState = MutableLiveData<SubscriptionState>()
    val subscriptionState: LiveData<SubscriptionState> = _subscriptionState
    
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading
    
    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage
    
    private val _successMessage = MutableLiveData<String?>()
    val successMessage: LiveData<String?> = _successMessage
    
    init {
        // Initialize licensing module
        if (!licensingModule.isReady()) {
            licensingModule.initialize()
        }
        
        // Observe subscription state changes
        viewModelScope.launch {
            licensingModule.subscriptionState.collect { state ->
                _subscriptionState.value = state
            }
        }
    }
    
    /**
     * Load current license state
     */
    fun loadLicenseState() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                // Current state is already being observed through flow
                // Just validate the license
                validateLicense()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load license state", e)
                _errorMessage.value = "Failed to load license information: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Start trial period
     */
    fun startTrial() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _errorMessage.value = null
                
                val success = licensingModule.startTrial()
                if (success) {
                    _successMessage.value = "Trial period started! You now have 30 days of premium features."
                    Log.d(TAG, "Trial started successfully")
                } else {
                    _errorMessage.value = "Unable to start trial. You may already have an active license."
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start trial", e)
                _errorMessage.value = "Failed to start trial: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Activate license with key
     */
    fun activateLicense(licenseKey: String) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _errorMessage.value = null
                
                val success = licensingModule.activatePremium(licenseKey)
                if (success) {
                    _successMessage.value = "License activated successfully! Premium features are now available."
                    Log.d(TAG, "License activated: $licenseKey")
                } else {
                    _errorMessage.value = "Invalid license key. Please check your key and try again."
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to activate license", e)
                _errorMessage.value = "Failed to activate license: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Validate current license
     */
    fun validateLicense() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _errorMessage.value = null
                
                // The licensing module handles validation internally
                // and updates the subscription state automatically
                
                // Force a validation check by calling initialize again
                // This will trigger internal validation
                licensingModule.initialize()
                
                Log.d(TAG, "License validation requested")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to validate license", e)
                _errorMessage.value = "Failed to validate license: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Open purchase page in browser
     */
    fun openPurchasePage() {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(PURCHASE_URL)).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            Log.d(TAG, "Opened purchase page")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open purchase page", e)
            _errorMessage.value = "Unable to open purchase page. Please visit $PURCHASE_URL"
        }
    }
    
    /**
     * Open support page in browser
     */
    fun openSupportPage() {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(SUPPORT_URL)).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            Log.d(TAG, "Opened support page")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open support page", e)
            _errorMessage.value = "Unable to open support page. Please visit $SUPPORT_URL"
        }
    }
    
    /**
     * Clear error message
     */
    fun clearError() {
        _errorMessage.value = null
    }
    
    /**
     * Clear success message
     */
    fun clearSuccess() {
        _successMessage.value = null
    }
    
    /**
     * Get license type display name
     */
    fun getLicenseTypeDisplayName(licenseType: String): String {
        return when (licenseType) {
            LicensingModule.LICENSE_FREE -> "Free Version"
            LicensingModule.LICENSE_TRIAL -> "Trial Version"
            LicensingModule.LICENSE_PREMIUM -> "Premium License"
            LicensingModule.LICENSE_ENTERPRISE -> "Enterprise License"
            else -> "Unknown License"
        }
    }
    
    /**
     * Get license status color
     */
    fun getLicenseStatusColor(licenseType: String): androidx.compose.ui.graphics.Color {
        return when (licenseType) {
            LicensingModule.LICENSE_PREMIUM, LicensingModule.LICENSE_ENTERPRISE -> 
                androidx.compose.ui.graphics.Color(0xFF4CAF50) // Green
            LicensingModule.LICENSE_TRIAL -> 
                androidx.compose.ui.graphics.Color(0xFFFF9800) // Orange
            else -> 
                androidx.compose.ui.graphics.Color(0xFF2196F3) // Blue
        }
    }
    
    /**
     * Check if premium features are available
     */
    fun isPremiumAvailable(): Boolean {
        return licensingModule.isPremium()
    }
    
    /**
     * Get trial days remaining
     */
    fun getTrialDaysRemaining(): Int {
        return licensingModule.getTrialDaysRemaining()
    }
    
    override fun onCleared() {
        super.onCleared()
        Log.d(TAG, "LicenseViewModel cleared")
    }
}

/**
 * ViewModelProvider Factory for LicenseViewModel
 */
class LicenseViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(LicenseViewModel::class.java)) {
            return LicenseViewModel(context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}