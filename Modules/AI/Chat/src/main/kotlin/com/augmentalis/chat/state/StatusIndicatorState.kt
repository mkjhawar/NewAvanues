/*
 * Copyright (c) 2024-2025 Intelligent Devices LLC / Augmentalis
 * All Rights Reserved.
 *
 * SOLID Refactoring: Extracted from ChatViewModel (SRP)
 */

package com.augmentalis.chat.state

import android.content.Context
import android.util.Log
import com.augmentalis.ava.core.data.prefs.DeveloperPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject
import javax.inject.Singleton

/**
 * StatusIndicatorState - Single Responsibility: NLU/LLM Status Indicator State
 *
 * Extracted from ChatViewModel as part of SOLID refactoring.
 * Manages state for the StatusIndicator UI component (REQ-001, REQ-002, REQ-003):
 * - Model loaded states (NLU, LLM)
 * - Last responder tracking with timestamp
 * - LLM fallback invocation tracking
 * - Flash mode (developer feature)
 *
 * Thread-safe: Uses StateFlow for all mutable state.
 *
 * Usage in UI:
 * ```kotlin
 * @Composable
 * fun StatusIndicator(state: StatusIndicatorState) {
 *     val isNLULoaded by state.isNLULoaded.collectAsState()
 *     val lastResponder by state.lastResponder.collectAsState()
 *     // ... render indicator based on state
 * }
 * ```
 *
 * @param context Application context for DeveloperPreferences
 *
 * @author Manoj Jhawar / Claude AI
 * @since 2025-12-17
 */
@Singleton
class StatusIndicatorState @Inject constructor(
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "StatusIndicatorState"

        /**
         * Duration in milliseconds for which the "active" highlight is shown
         * after a responder answers. UI computes visibility based on timestamp.
         */
        const val ACTIVE_HIGHLIGHT_DURATION_MS = 2000L
    }

    // ==================== Model Loaded State (REQ-002) ====================

    /**
     * NLU model loaded state (REQ-002).
     * true if NLU model is in memory and ready, false otherwise.
     * Set by NLU initialization, cleared on model unload or failure.
     */
    private val _isNLULoaded = MutableStateFlow(false)
    val isNLULoaded: StateFlow<Boolean> = _isNLULoaded.asStateFlow()

    /**
     * LLM model loaded state (REQ-002).
     * true if LLM model is in memory and ready, false otherwise.
     * Currently defaults to true (LLM assumed always loaded via API),
     * but can be set to false if LLM becomes unavailable.
     */
    private val _isLLMLoaded = MutableStateFlow(true)
    val isLLMLoaded: StateFlow<Boolean> = _isLLMLoaded.asStateFlow()

    /**
     * NLU ready state (initialization complete).
     * Differs from isNLULoaded: this tracks if initialization is complete,
     * isNLULoaded tracks if model is in memory.
     */
    private val _isNLUReady = MutableStateFlow(false)
    val isNLUReady: StateFlow<Boolean> = _isNLUReady.asStateFlow()

    // ==================== Last Responder State (REQ-001) ====================

    /**
     * Which system last responded: "NLU", "LLM", or null (REQ-001).
     * Triggers 2-second highlight in StatusIndicator to show which system answered.
     * - "NLU": High confidence NLU classification executed action or generated response
     * - "LLM": Low confidence NLU fallback, or direct LLM query, or NLU not ready
     * - null: No response yet
     */
    private val _lastResponder = MutableStateFlow<String?>(null)
    val lastResponder: StateFlow<String?> = _lastResponder.asStateFlow()

    /**
     * Timestamp when last responder was set (REQ-001).
     * Used to calculate 2-second highlight duration in UI.
     * UI computes: if (System.currentTimeMillis() - timestamp < 2000) show ACTIVE state
     */
    private val _lastResponderTimestamp = MutableStateFlow(0L)
    val lastResponderTimestamp: StateFlow<Long> = _lastResponderTimestamp.asStateFlow()

    // ==================== LLM Fallback State (REQ-003) ====================

    /**
     * LLM fallback invoked flag (REQ-003 CRITICAL).
     * Set to true when NLU confidence < threshold and LLM is invoked for fallback.
     * Used for testing and verification that LLM fallback is working correctly.
     * Reset to false at start of each sendMessage() call.
     */
    private val _llmFallbackInvoked = MutableStateFlow(false)
    val llmFallbackInvoked: StateFlow<Boolean> = _llmFallbackInvoked.asStateFlow()

    // ==================== Developer Settings (REQ-007) ====================

    /**
     * Flash mode enabled state (REQ-007).
     * When true, StatusIndicator pulses/flashes during active NLU/LLM processing.
     * Useful for developers and QA to see which system is working in real-time.
     *
     * Note: This requires a CoroutineScope to be provided via initFlashMode().
     */
    private var _isFlashModeEnabled: StateFlow<Boolean>? = null
    val isFlashModeEnabled: StateFlow<Boolean>
        get() = _isFlashModeEnabled ?: MutableStateFlow(false)

    /**
     * Initialize flash mode state with the given coroutine scope.
     * Call this from ViewModel's init block.
     */
    fun initFlashMode(scope: CoroutineScope) {
        _isFlashModeEnabled = DeveloperPreferences(context)
            .isFlashModeEnabled
            .stateIn(
                scope = scope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = false
            )
    }

    // ==================== Model State Operations ====================

    /**
     * Set NLU loaded state.
     */
    fun setNLULoaded(loaded: Boolean) {
        _isNLULoaded.value = loaded
        Log.d(TAG, "NLU loaded: $loaded")
    }

    /**
     * Set LLM loaded state.
     */
    fun setLLMLoaded(loaded: Boolean) {
        _isLLMLoaded.value = loaded
        Log.d(TAG, "LLM loaded: $loaded")
    }

    /**
     * Set NLU ready state.
     */
    fun setNLUReady(ready: Boolean) {
        _isNLUReady.value = ready
        Log.d(TAG, "NLU ready: $ready")
    }

    // ==================== Responder Operations ====================

    /**
     * Set the last responder and update timestamp.
     * Call this when NLU or LLM responds to a user query.
     *
     * @param responder "NLU" or "LLM"
     */
    fun setLastResponder(responder: String) {
        _lastResponder.value = responder
        _lastResponderTimestamp.value = System.currentTimeMillis()
        Log.d(TAG, "Last responder: $responder")
    }

    /**
     * Clear last responder state.
     */
    fun clearLastResponder() {
        _lastResponder.value = null
    }

    /**
     * Check if the active highlight should be shown.
     * Returns true if last responder was set within ACTIVE_HIGHLIGHT_DURATION_MS.
     */
    fun isActiveHighlightVisible(): Boolean {
        val timestamp = _lastResponderTimestamp.value
        return timestamp > 0 && System.currentTimeMillis() - timestamp < ACTIVE_HIGHLIGHT_DURATION_MS
    }

    // ==================== LLM Fallback Operations ====================

    /**
     * Set LLM fallback invoked flag.
     * Call this when LLM is invoked due to low NLU confidence.
     *
     * @param invoked true if LLM fallback was invoked
     */
    fun setLLMFallbackInvoked(invoked: Boolean) {
        _llmFallbackInvoked.value = invoked
        if (invoked) {
            Log.d(TAG, "LLM fallback invoked")
        }
    }

    /**
     * Reset LLM fallback flag.
     * Call this at the start of each message send.
     */
    fun resetLLMFallbackInvoked() {
        _llmFallbackInvoked.value = false
    }

    // ==================== Convenience Methods ====================

    /**
     * Mark NLU as the responder.
     */
    fun markNLUResponded() {
        setLastResponder("NLU")
    }

    /**
     * Mark LLM as the responder.
     */
    fun markLLMResponded() {
        setLastResponder("LLM")
    }

    /**
     * Mark LLM as the responder due to fallback.
     */
    fun markLLMFallbackResponded() {
        setLLMFallbackInvoked(true)
        setLastResponder("LLM")
    }

    /**
     * Reset all state for a new message.
     */
    fun resetForNewMessage() {
        resetLLMFallbackInvoked()
    }

    /**
     * Get a summary of current state (for debugging).
     */
    fun getStatusSummary(): String {
        return """
            NLU Loaded: ${_isNLULoaded.value}
            LLM Loaded: ${_isLLMLoaded.value}
            NLU Ready: ${_isNLUReady.value}
            Last Responder: ${_lastResponder.value}
            LLM Fallback: ${_llmFallbackInvoked.value}
        """.trimIndent()
    }
}
