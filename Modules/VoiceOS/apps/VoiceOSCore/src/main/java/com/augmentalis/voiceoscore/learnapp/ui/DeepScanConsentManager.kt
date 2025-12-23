/**
 * DeepScanConsentManager.kt - Manages deep scan consent preferences
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Created: 2025-12-22
 *
 * Manages user consent preferences for deep scanning expandable controls
 * (menus, drawers, dropdowns) on a per-app basis.
 */

package com.augmentalis.voiceoscore.learnapp.ui

import android.util.Log
import com.augmentalis.database.repositories.IUserPreferenceRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Deep Scan Consent Manager
 *
 * Manages user consent for deep scanning expandable controls on a per-app basis.
 * Stores preferences in database using UserPreferenceRepository.
 *
 * Key format: `deep_scan_consent_{packageName}`
 * Values: "YES" | "SKIP" | "NO"
 * Type: "DEEP_SCAN_CONSENT"
 *
 * @property userPreferenceRepository Repository for storing preferences
 */
class DeepScanConsentManager(
    private val userPreferenceRepository: IUserPreferenceRepository
) {
    companion object {
        private const val TAG = "DeepScanConsentManager"
        private const val PREFERENCE_TYPE = "DEEP_SCAN_CONSENT"
        private const val KEY_PREFIX = "deep_scan_consent_"
    }

    private val _currentDialogState = MutableStateFlow<DeepScanDialogState>(DeepScanDialogState.Hidden)
    val currentDialogState: StateFlow<DeepScanDialogState> = _currentDialogState.asStateFlow()

    /**
     * Check if consent is needed for package
     *
     * Returns true if:
     * - No preference exists (first time) OR
     * - User chose SKIP (ask again later)
     *
     * Returns false if:
     * - User chose NO (never ask again)
     *
     * @param packageName Package to check
     * @return true if consent dialog should be shown
     */
    suspend fun needsConsent(packageName: String): Boolean {
        val key = generateKey(packageName)
        val consent = getConsent(packageName)

        return when (consent) {
            DeepScanConsentResponse.NO -> {
                Log.d(TAG, "Consent not needed for $packageName - user declined permanently")
                false
            }
            DeepScanConsentResponse.YES -> {
                Log.d(TAG, "Consent already granted for $packageName")
                false
            }
            DeepScanConsentResponse.SKIP, null -> {
                Log.d(TAG, "Consent needed for $packageName")
                true
            }
            else -> true
        }
    }

    /**
     * Get consent value for package
     *
     * @param packageName Package name
     * @return Consent response or null if not set
     */
    suspend fun getConsent(packageName: String): DeepScanConsentResponse? {
        val key = generateKey(packageName)
        val value = userPreferenceRepository.getValue(key)

        return when (value) {
            "YES" -> DeepScanConsentResponse.YES
            "SKIP" -> DeepScanConsentResponse.SKIP
            "NO" -> DeepScanConsentResponse.NO
            "DISMISSED" -> DeepScanConsentResponse.DISMISSED
            else -> null
        }
    }

    /**
     * Set consent value for package
     *
     * @param packageName Package name
     * @param response User's consent response
     */
    suspend fun setConsent(packageName: String, response: DeepScanConsentResponse) {
        val key = generateKey(packageName)
        val value = when (response) {
            DeepScanConsentResponse.YES -> "YES"
            DeepScanConsentResponse.SKIP -> "SKIP"
            DeepScanConsentResponse.NO -> "NO"
            DeepScanConsentResponse.DISMISSED -> "DISMISSED"
        }

        userPreferenceRepository.setValue(key, value, PREFERENCE_TYPE)
        Log.i(TAG, "Set deep scan consent for $packageName: $value")
    }

    /**
     * Show deep scan consent dialog
     *
     * @param packageName Package name
     * @param appName Human-readable app name
     * @param expandableCount Number of expandable controls found
     */
    fun showConsentDialog(
        packageName: String,
        appName: String,
        expandableCount: Int
    ) {
        _currentDialogState.value = DeepScanDialogState.Showing(
            packageName = packageName,
            appName = appName,
            expandableCount = expandableCount
        )
        Log.i(TAG, "Showing deep scan consent dialog for $appName ($packageName) - $expandableCount expandables")
    }

    /**
     * Handle user response to consent dialog
     *
     * @param response User's response
     */
    suspend fun handleConsentResponse(response: DeepScanConsentResponse) {
        val currentState = _currentDialogState.value
        if (currentState is DeepScanDialogState.Showing) {
            setConsent(currentState.packageName, response)

            when (response) {
                DeepScanConsentResponse.YES -> {
                    Log.i(TAG, "User approved deep scan for ${currentState.packageName}")
                }
                DeepScanConsentResponse.SKIP -> {
                    Log.i(TAG, "User skipped deep scan for ${currentState.packageName} - will ask again")
                }
                DeepScanConsentResponse.NO -> {
                    Log.i(TAG, "User declined deep scan for ${currentState.packageName} - never ask again")
                }
                DeepScanConsentResponse.DISMISSED -> {
                    Log.i(TAG, "User dismissed deep scan dialog for ${currentState.packageName}")
                }
            }
        }

        hideDialog()
    }

    /**
     * Hide current dialog
     */
    fun hideDialog() {
        _currentDialogState.value = DeepScanDialogState.Hidden
    }

    /**
     * Check if dialog is currently showing
     */
    fun isDialogShowing(): Boolean {
        return _currentDialogState.value is DeepScanDialogState.Showing
    }

    /**
     * Clear consent for package (reset to first-time state)
     *
     * @param packageName Package name
     */
    suspend fun clearConsent(packageName: String) {
        val key = generateKey(packageName)
        userPreferenceRepository.delete(key)
        Log.i(TAG, "Cleared deep scan consent for $packageName")
    }

    /**
     * Get all packages with stored consent
     *
     * @return Map of packageName to consent response
     */
    suspend fun getAllConsents(): Map<String, DeepScanConsentResponse> {
        val preferences = userPreferenceRepository.getByType(PREFERENCE_TYPE)
        return preferences.associate { pref ->
            val packageName = pref.key.removePrefix(KEY_PREFIX)
            val response = when (pref.value) {
                "YES" -> DeepScanConsentResponse.YES
                "SKIP" -> DeepScanConsentResponse.SKIP
                "NO" -> DeepScanConsentResponse.NO
                "DISMISSED" -> DeepScanConsentResponse.DISMISSED
                else -> DeepScanConsentResponse.DISMISSED
            }
            packageName to response
        }
    }

    /**
     * Clear all deep scan consents
     */
    suspend fun clearAllConsents() {
        userPreferenceRepository.deleteByType(PREFERENCE_TYPE)
        Log.i(TAG, "Cleared all deep scan consents")
    }

    /**
     * Generate preference key for package
     */
    private fun generateKey(packageName: String): String {
        return "$KEY_PREFIX$packageName"
    }

    /**
     * Cleanup resources
     */
    fun cleanup() {
        hideDialog()
    }
}

/**
 * Deep Scan Dialog State
 *
 * Represents current state of the deep scan consent dialog.
 */
sealed class DeepScanDialogState {
    object Hidden : DeepScanDialogState()

    data class Showing(
        val packageName: String,
        val appName: String,
        val expandableCount: Int
    ) : DeepScanDialogState()
}
