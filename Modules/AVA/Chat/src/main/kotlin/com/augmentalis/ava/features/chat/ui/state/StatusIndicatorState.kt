/*
 * Copyright (c) 2024-2025 Intelligent Devices LLC / Augmentalis
 * All Rights Reserved.
 *
 * SOLID Refactoring: Extracted from ChatViewModel (SRP)
 */

package com.augmentalis.ava.features.chat.ui.state

import android.content.Context
import com.augmentalis.ava.core.data.prefs.DeveloperPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
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
 * Manages all status indicator related state (REQ-001, REQ-002, REQ-003):
 * - NLU/LLM loaded states
 * - Last responder tracking (NLU vs LLM)
 * - LLM fallback invocation tracking
 * - Flash mode settings
 *
 * Thread-safe: All state mutations via MutableStateFlow.
 *
 * @param context Application context for DeveloperPreferences access
 *
 * @author Manoj Jhawar / Claude AI
 * @since 2025-12-17
 */
@Singleton
class StatusIndicatorState @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    // ==================== Model Loaded States (REQ-002) ====================

    /**
     * NLU model loaded state.
     * true if NLU model is in memory and ready, false otherwise.
     */
    private val _isNLULoaded = MutableStateFlow(false)
    val isNLULoaded: StateFlow<Boolean> = _isNLULoaded.asStateFlow()

    /**
     * LLM model loaded state.
     * true if LLM model is in memory and ready, false otherwise.
     */
    private val _isLLMLoaded = MutableStateFlow(true) // Assume loaded by default
    val isLLMLoaded: StateFlow<Boolean> = _isLLMLoaded.asStateFlow()

    // ==================== Last Responder State (REQ-001) ====================

    /**
     * Which system last responded: "NLU", "LLM", or null.
     * Triggers 2-second highlight in StatusIndicator to show which system answered.
     * - "NLU": High confidence NLU classification executed action or generated response
     * - "LLM": Low confidence NLU fallback, or direct LLM query, or NLU not ready
     * - null: No response yet
     */
    private val _lastResponder = MutableStateFlow<String?>(null)
    val lastResponder: StateFlow<String?> = _lastResponder.asStateFlow()

    /**
     * Timestamp when last responder was set.
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
     * Flash mode enabled state.
     * When true, StatusIndicator pulses/flashes during active NLU/LLM processing.
     * Useful for developers and QA to see which system is working in real-time.
     */
    val isFlashModeEnabled: StateFlow<Boolean> = DeveloperPreferences(context)
        .isFlashModeEnabled
        .stateIn(
            scope = scope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )

    // ==================== State Mutators ====================

    /**
     * Set the NLU loaded state.
     */
    fun setNLULoaded(loaded: Boolean) {
        _isNLULoaded.value = loaded
    }

    /**
     * Set the LLM loaded state.
     */
    fun setLLMLoaded(loaded: Boolean) {
        _isLLMLoaded.value = loaded
    }

    /**
     * Set the last responder and update timestamp.
     *
     * @param responder "NLU", "LLM", or null
     */
    fun setLastResponder(responder: String?) {
        _lastResponder.value = responder
        _lastResponderTimestamp.value = System.currentTimeMillis()
    }

    /**
     * Set the LLM fallback invoked flag.
     */
    fun setLLMFallbackInvoked(invoked: Boolean) {
        _llmFallbackInvoked.value = invoked
    }

    /**
     * Reset fallback flag (call at start of each message send).
     */
    fun resetLLMFallbackFlag() {
        _llmFallbackInvoked.value = false
    }

    /**
     * Clear last responder (call when appropriate).
     */
    fun clearLastResponder() {
        _lastResponder.value = null
    }

    // ==================== Convenience Methods ====================

    /**
     * Check if the responder highlight should still be shown.
     * Returns true if within the 2-second highlight window.
     */
    fun isHighlightActive(): Boolean {
        val timestamp = _lastResponderTimestamp.value
        return timestamp > 0 && (System.currentTimeMillis() - timestamp < 2000)
    }

    /**
     * Mark NLU as the last responder.
     */
    fun markNLUResponded() {
        setLastResponder("NLU")
    }

    /**
     * Mark LLM as the last responder.
     */
    fun markLLMResponded() {
        setLastResponder("LLM")
        setLLMFallbackInvoked(true)
    }

    /**
     * Reset all status state.
     */
    fun reset() {
        _lastResponder.value = null
        _lastResponderTimestamp.value = 0L
        _llmFallbackInvoked.value = false
    }
}
