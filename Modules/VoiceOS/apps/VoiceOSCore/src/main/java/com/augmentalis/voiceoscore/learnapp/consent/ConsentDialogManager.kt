/**
 * ConsentDialogManager.kt - Manages consent dialogs for exploration
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Created: 2025-12-17
 *
 * Manages showing and handling consent dialogs for app exploration.
 */

package com.augmentalis.voiceoscore.learnapp.consent

import android.accessibilityservice.AccessibilityService
import com.augmentalis.voiceoscore.learnapp.detection.LearnedAppTracker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Consent Dialog Manager
 *
 * Manages consent dialogs for app exploration.
 * Shows consent dialog when user enters an unlearned app.
 */
class ConsentDialogManager(
    private val accessibilityService: AccessibilityService,
    private val learnedAppTracker: LearnedAppTracker
) {
    private val _currentConsentState = MutableStateFlow<ConsentState>(ConsentState.None)
    val currentConsentState: StateFlow<ConsentState> = _currentConsentState.asStateFlow()

    private var pendingCallback: ((ConsentResponse) -> Unit)? = null

    /**
     * Check if consent is needed for package
     */
    fun needsConsent(packageName: String): Boolean {
        return !learnedAppTracker.isFullyLearned(packageName) &&
               !learnedAppTracker.isExcluded(packageName)
    }

    /**
     * Show consent dialog for package
     *
     * @param packageName Package to show consent for
     * @param appName Display name of the app
     * @param callback Callback for user response
     */
    fun showConsentDialog(
        packageName: String,
        appName: String,
        callback: (ConsentResponse) -> Unit
    ) {
        pendingCallback = callback
        _currentConsentState.value = ConsentState.Showing(packageName, appName)
        // Actual dialog showing would be implemented here
        // For now, this is a placeholder that would integrate with overlay system
    }

    /**
     * Handle user response to consent dialog
     */
    fun handleConsentResponse(response: ConsentResponse) {
        val currentState = _currentConsentState.value
        if (currentState is ConsentState.Showing) {
            when (response) {
                ConsentResponse.Accept -> {
                    // Mark consent accepted
                }
                ConsentResponse.Skip -> {
                    // Enable JIT learning only
                }
                ConsentResponse.Decline -> {
                    learnedAppTracker.excludePackage(currentState.packageName)
                }
                else -> {}
            }
        }

        _currentConsentState.value = ConsentState.None
        pendingCallback?.invoke(response)
        pendingCallback = null
    }

    /**
     * Dismiss current consent dialog
     */
    fun dismissDialog() {
        if (_currentConsentState.value is ConsentState.Showing) {
            _currentConsentState.value = ConsentState.None
            pendingCallback?.invoke(ConsentResponse.Dismissed)
            pendingCallback = null
        }
    }

    /**
     * Check if consent dialog is currently showing
     */
    fun isDialogShowing(): Boolean {
        return _currentConsentState.value is ConsentState.Showing
    }
}

/**
 * Consent State
 *
 * Current state of consent dialog.
 */
sealed class ConsentState {
    object None : ConsentState()

    data class Showing(
        val packageName: String,
        val appName: String
    ) : ConsentState()
}
