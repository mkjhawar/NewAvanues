/**
 * LicensingModule.kt - Direct implementation licensing manager
 * 
 * Author: Manoj Jhawar
 * Code-Reviewed-By: CCA
 * Created: 2025-08-22
 */

package com.augmentalis.licensemanager

import android.content.Context
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * Licensing Module - Direct implementation, manages subscriptions and licenses
 */
open class LicensingModule(private val context: Context) {
    companion object {
        private const val TAG = "LicensingModule"
        const val MODULE_ID = "licensing"
        const val MODULE_VERSION = "1.0.0"
        
        // Trial period
        const val TRIAL_DAYS = 30
        const val TRIAL_WARNING_DAYS = 7
        
        // License types
        const val LICENSE_FREE = "free"
        const val LICENSE_TRIAL = "trial"
        const val LICENSE_PREMIUM = "premium"
        const val LICENSE_ENTERPRISE = "enterprise"
        
        @Volatile
        private var instance: LicensingModule? = null
        
        fun getInstance(context: Context): LicensingModule {
            return instance ?: synchronized(this) {
                instance ?: LicensingModule(context.applicationContext).also { instance = it }
            }
        }
    }
    
    private var isReady = false
    private lateinit var subscriptionManager: SubscriptionManager
    private val licenseValidator = LicenseValidator()
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    private val _subscriptionState = MutableStateFlow(SubscriptionState())
    val subscriptionState: StateFlow<SubscriptionState> = _subscriptionState.asStateFlow()
    
    // Direct properties - no interface
    val name: String = MODULE_ID
    val version: String = MODULE_VERSION
    val description: String = "Manages subscriptions and licenses"
    
    fun getDependencies(): List<String> = emptyList()
    
    fun initialize(): Boolean {
        if (isReady) return true
        
        return try {
            // Initialize subscription manager
            subscriptionManager = SubscriptionManager(context)
            
            // Load saved subscription state
            val savedState = subscriptionManager.loadSubscriptionState()
            _subscriptionState.value = savedState
            
            // Start background tasks
            scope.launch {
                // Validate license
                validateLicense()
                
                // Start periodic validation
                startPeriodicValidation()
                
                // Check trial status
                checkTrialStatus()
            }
            
            isReady = true
            Log.d(TAG, "Licensing module initialized")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize licensing module", e)
            false
        }
    }
    
    fun shutdown() {
        scope.cancel()
        isReady = false
        instance = null
        Log.d(TAG, "Licensing module shutdown")
    }
    
    fun isReady(): Boolean = isReady
    
    fun getCapabilities(): ModuleCapabilities {
        return ModuleCapabilities(
            requiresNetwork = true,
            requiresStorage = true,
            requiresAccessibility = false,
            requiresMicrophone = false,
            requiresNotification = false,
            supportsOffline = true,
            memoryImpact = MemoryImpact.LOW
        )
    }
    
    /**
     * Start trial period
     */
    suspend fun startTrial(): Boolean {
        if (_subscriptionState.value.licenseType != LICENSE_FREE) {
            Log.w(TAG, "Cannot start trial - already have license: ${_subscriptionState.value.licenseType}")
            return false
        }
        
        val trialEndDate = System.currentTimeMillis() + TimeUnit.DAYS.toMillis(TRIAL_DAYS.toLong())
        
        val newState = _subscriptionState.value.copy(
            licenseType = LICENSE_TRIAL,
            isPremium = true,
            trialStartDate = System.currentTimeMillis(),
            trialEndDate = trialEndDate,
            isValid = true
        )
        
        _subscriptionState.value = newState
        subscriptionManager.saveSubscriptionState(newState)
        
        Log.d(TAG, "Trial period started - ends: ${Date(trialEndDate)}")
        return true
    }
    
    /**
     * Activate premium subscription
     */
    suspend fun activatePremium(licenseKey: String): Boolean {
        // Validate license key
        val validationResult = licenseValidator.validateKey(licenseKey)
        
        if (!validationResult.isValid) {
            Log.e(TAG, "Invalid license key")
            return false
        }
        
        val newState = _subscriptionState.value.copy(
            licenseType = validationResult.licenseType ?: LICENSE_PREMIUM,
            isPremium = true,
            licenseKey = licenseKey,
            expiryDate = validationResult.expiryDate,
            isValid = true,
            lastValidation = System.currentTimeMillis()
        )
        
        _subscriptionState.value = newState
        subscriptionManager.saveSubscriptionState(newState)
        
        Log.d(TAG, "Premium subscription activated: ${validationResult.licenseType}")
        return true
    }
    
    /**
     * Check if premium features are available
     */
    fun isPremium(): Boolean = _subscriptionState.value.isPremium
    
    /**
     * Get current license type
     */
    fun getLicenseType(): String = _subscriptionState.value.licenseType
    
    /**
     * Get current subscription state
     */
    fun getSubscriptionState(): SubscriptionState = _subscriptionState.value
    
    /**
     * Activate free license (reset to free state)
     */
    suspend fun activateFree() {
        val newState = SubscriptionState(
            licenseType = LICENSE_FREE,
            isPremium = false,
            isValid = true
        )
        
        _subscriptionState.value = newState
        subscriptionManager.saveSubscriptionState(newState)
        
        Log.d(TAG, "Free license activated")
    }
    
    /**
     * Get days remaining in trial
     */
    fun getTrialDaysRemaining(): Int {
        val state = _subscriptionState.value
        if (state.licenseType != LICENSE_TRIAL) return 0
        
        val remaining = state.trialEndDate - System.currentTimeMillis()
        return TimeUnit.MILLISECONDS.toDays(remaining).toInt().coerceAtLeast(0)
    }
    
    /**
     * Public method to validate a license key
     */
    suspend fun validateLicense(licenseKey: String): ValidationResult {
        return licenseValidator.validateKey(licenseKey)
    }
    
    /**
     * Validate current license
     */
    private suspend fun validateLicense() {
        val state = _subscriptionState.value
        
        when (state.licenseType) {
            LICENSE_TRIAL -> validateTrial()
            LICENSE_PREMIUM, LICENSE_ENTERPRISE -> validatePremiumLicense()
            else -> {
                // Free license always valid
                _subscriptionState.value = state.copy(isValid = true)
            }
        }
    }
    
    /**
     * Validate trial period
     */
    private fun validateTrial() {
        val state = _subscriptionState.value
        val now = System.currentTimeMillis()
        
        if (now > state.trialEndDate) {
            // Trial expired
            val newState = state.copy(
                licenseType = LICENSE_FREE,
                isPremium = false,
                isValid = true
            )
            _subscriptionState.value = newState
            subscriptionManager.saveSubscriptionState(newState)
            
            Log.d(TAG, "Trial period expired")
        }
    }
    
    /**
     * Validate premium license
     */
    private suspend fun validatePremiumLicense() {
        val state = _subscriptionState.value
        val licenseKey = state.licenseKey ?: return
        
        // Validate with server
        val validationResult = licenseValidator.validateKey(licenseKey)
        
        if (!validationResult.isValid) {
            // License invalid
            val newState = state.copy(
                licenseType = LICENSE_FREE,
                isPremium = false,
                isValid = false,
                licenseKey = null
            )
            _subscriptionState.value = newState
            subscriptionManager.saveSubscriptionState(newState)
            
            Log.w(TAG, "License validation failed")
        } else {
            // Update validation timestamp
            _subscriptionState.value = state.copy(
                lastValidation = System.currentTimeMillis(),
                isValid = true
            )
        }
    }
    
    /**
     * Check trial status and send warnings
     */
    private fun checkTrialStatus() {
        val state = _subscriptionState.value
        if (state.licenseType != LICENSE_TRIAL) return
        
        val daysRemaining = getTrialDaysRemaining()
        
        if (daysRemaining in 1..TRIAL_WARNING_DAYS) {
            Log.d(TAG, "Trial ending warning: $daysRemaining days remaining")
        }
    }
    
    /**
     * Start periodic license validation
     */
    private fun startPeriodicValidation() {
        scope.launch {
            while (isActive) {
                delay(TimeUnit.HOURS.toMillis(24)) // Check daily
                validateLicense()
                checkTrialStatus()
            }
        }
    }
}

/**
 * Subscription state
 */
data class SubscriptionState(
    val licenseType: String = LicensingModule.LICENSE_FREE,
    val isPremium: Boolean = false,
    val licenseKey: String? = null,
    val trialStartDate: Long = 0,
    val trialEndDate: Long = 0,
    val expiryDate: Long? = null,
    val lastValidation: Long = 0,
    val isValid: Boolean = true
)

/**
 * Subscription manager for persistence
 */
open class SubscriptionManager(private val context: Context) {
    companion object {
        private const val PREFS_NAME = "voiceos_licensing"
        private const val KEY_LICENSE_TYPE = "license_type"
        private const val KEY_IS_PREMIUM = "is_premium"
        private const val KEY_LICENSE_KEY = "license_key"
        private const val KEY_TRIAL_START = "trial_start"
        private const val KEY_TRIAL_END = "trial_end"
        private const val KEY_EXPIRY_DATE = "expiry_date"
        private const val KEY_LAST_VALIDATION = "last_validation"
    }
    
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    
    fun loadSubscriptionState(): SubscriptionState {
        return SubscriptionState(
            licenseType = prefs.getString(KEY_LICENSE_TYPE, LicensingModule.LICENSE_FREE) ?: LicensingModule.LICENSE_FREE,
            isPremium = prefs.getBoolean(KEY_IS_PREMIUM, false),
            licenseKey = prefs.getString(KEY_LICENSE_KEY, null),
            trialStartDate = prefs.getLong(KEY_TRIAL_START, 0),
            trialEndDate = prefs.getLong(KEY_TRIAL_END, 0),
            expiryDate = if (prefs.contains(KEY_EXPIRY_DATE)) {
                prefs.getLong(KEY_EXPIRY_DATE, 0)
            } else null,
            lastValidation = prefs.getLong(KEY_LAST_VALIDATION, 0)
        )
    }
    
    fun saveSubscriptionState(state: SubscriptionState) {
        prefs.edit().apply {
            putString(KEY_LICENSE_TYPE, state.licenseType)
            putBoolean(KEY_IS_PREMIUM, state.isPremium)
            state.licenseKey?.let { putString(KEY_LICENSE_KEY, it) }
            putLong(KEY_TRIAL_START, state.trialStartDate)
            putLong(KEY_TRIAL_END, state.trialEndDate)
            state.expiryDate?.let { putLong(KEY_EXPIRY_DATE, it) }
            putLong(KEY_LAST_VALIDATION, state.lastValidation)
            apply()
        }
    }
}

/**
 * License validator
 */
class LicenseValidator {
    companion object {
        private const val TAG = "LicenseValidator"
    }
    
    /**
     * Validate license key
     * In production, this would connect to a license server
     */
    suspend fun validateKey(licenseKey: String): ValidationResult {
        // Simulate network validation
        delay(500)
        
        // For demo purposes, accept specific patterns
        return when {
            licenseKey.startsWith("PREMIUM-") -> {
                ValidationResult(
                    isValid = true,
                    licenseType = LicensingModule.LICENSE_PREMIUM,
                    expiryDate = System.currentTimeMillis() + TimeUnit.DAYS.toMillis(365)
                )
            }
            licenseKey.startsWith("ENTERPRISE-") -> {
                ValidationResult(
                    isValid = true,
                    licenseType = LicensingModule.LICENSE_ENTERPRISE,
                    expiryDate = null // No expiry for enterprise
                )
            }
            else -> {
                ValidationResult(isValid = false)
            }
        }
    }
}

/**
 * License validation result
 */
data class ValidationResult(
    val isValid: Boolean,
    val licenseType: String? = null,
    val expiryDate: Long? = null,
    val errors: List<String> = if (isValid) emptyList() else listOf("Invalid format")
)

/**
 * Module capabilities data class
 */
data class ModuleCapabilities(
    val requiresNetwork: Boolean = false,
    val requiresStorage: Boolean = false,
    val requiresAccessibility: Boolean = false,
    val requiresMicrophone: Boolean = false,
    val requiresNotification: Boolean = false,
    val supportsOffline: Boolean = true,
    val memoryImpact: MemoryImpact = MemoryImpact.LOW
)

/**
 * Memory impact enum
 */
enum class MemoryImpact {
    LOW,
    MEDIUM, 
    HIGH
}
