/**
 * ConsentDialogManager.kt - Manages consent dialogs for exploration
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Created: 2025-12-17
 *
 * Manages showing and handling consent dialogs for app exploration.
 * VoiceOSCore-specific version that integrates with VoiceOS infrastructure.
 */
package com.augmentalis.voiceoscore.learnapp.ui

import android.accessibilityservice.AccessibilityService
import com.augmentalis.voiceoscore.learnapp.detection.LearnedAppTracker
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Consent Dialog Manager
 *
 * Manages consent dialogs for app exploration in VoiceOSCore.
 * Shows consent dialog when user enters an unlearned app.
 */
class ConsentDialogManager(
    private val accessibilityService: AccessibilityService,
    private val learnedAppTracker: LearnedAppTracker
) {
    private val _currentConsentState = MutableStateFlow<ConsentState>(ConsentState.None)
    val currentConsentState: StateFlow<ConsentState> = _currentConsentState.asStateFlow()

    private val _consentResponses = MutableSharedFlow<ConsentResponse>(
        replay = 0,
        extraBufferCapacity = 10
    )
    /** Flow of consent responses for reactive handling */
    val consentResponses: Flow<ConsentResponse> = _consentResponses.asSharedFlow()

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
    }

    /**
     * Show consent dialog for package (Flow-based API)
     *
     * Uses consentResponses Flow instead of callback for reactive handling.
     *
     * @param packageName Package to show consent for
     * @param appName Display name of the app
     */
    fun showConsentDialog(
        packageName: String,
        appName: String
    ) {
        _currentConsentState.value = ConsentState.Showing(packageName, appName)
    }

    /**
     * Emit consent response to Flow
     *
     * @param response The consent response to emit
     */
    fun emitConsentResponse(response: ConsentResponse) {
        _consentResponses.tryEmit(response)
    }

    /**
     * Handle user response to consent dialog
     */
    fun handleConsentResponse(response: ConsentResponse) {
        val currentState = _currentConsentState.value
        if (currentState is ConsentState.Showing) {
            when (response) {
                is ConsentResponse.Approved -> {
                    // Mark consent accepted
                }
                is ConsentResponse.Skipped -> {
                    // Enable JIT learning only
                }
                is ConsentResponse.Declined -> {
                    learnedAppTracker.excludePackage(currentState.packageName)
                }
                is ConsentResponse.Dismissed, is ConsentResponse.Timeout -> {
                    // No action needed
                }
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
        val currentState = _currentConsentState.value
        if (currentState is ConsentState.Showing) {
            _currentConsentState.value = ConsentState.None
            val response = ConsentResponse.Dismissed(currentState.packageName)
            pendingCallback?.invoke(response)
            pendingCallback = null
        }
    }

    /**
     * Check if consent dialog is currently showing
     */
    fun isDialogShowing(): Boolean {
        return _currentConsentState.value is ConsentState.Showing
    }

    /**
     * Cleanup resources
     */
    fun cleanup() {
        dismissDialog()
        pendingCallback = null
    }
}

/**
 * Consent State
 */
sealed class ConsentState {
    object None : ConsentState()

    data class Showing(
        val packageName: String,
        val appName: String
    ) : ConsentState()
}
