// filename: apps/ava-standalone/src/androidTest/kotlin/com/augmentalis/ava/fakes/FakeSettingsViewModel.kt
// created: 2025-11-16
// author: AVA AI Team
// © Augmentalis Inc, Intelligent Devices LLC

package com.augmentalis.ava.fakes

import androidx.lifecycle.ViewModel
import com.augmentalis.ava.ui.settings.ISettingsViewModel
import com.augmentalis.ava.ui.settings.ModelDownloadState
import com.augmentalis.ava.ui.settings.ModelInfo
import com.augmentalis.ava.ui.settings.SettingsUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Fake SettingsViewModel for testing
 *
 * Provides same interface as real SettingsViewModel but with in-memory state
 * No dependencies on Context, UserPreferences, or DataStore
 */
class FakeSettingsViewModel : ViewModel(), ISettingsViewModel {

    private val _uiState = MutableStateFlow(SettingsUiState())
    override val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    // NLU Settings
    override fun setNluEnabled(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(nluEnabled = enabled)
    }

    override fun setNluConfidenceThreshold(threshold: Float) {
        _uiState.value = _uiState.value.copy(nluConfidenceThreshold = threshold)
    }

    // ADR-014: Advanced NLU Thresholds
    override fun setTeachThreshold(threshold: Float) {
        _uiState.value = _uiState.value.copy(teachThreshold = threshold)
    }

    override fun setLLMFallbackThreshold(threshold: Float) {
        _uiState.value = _uiState.value.copy(llmFallbackThreshold = threshold)
    }

    override fun setSelfLearningThreshold(threshold: Float) {
        _uiState.value = _uiState.value.copy(selfLearningThreshold = threshold)
    }

    // LLM Settings
    override fun setLlmProvider(provider: String) {
        _uiState.value = _uiState.value.copy(llmProvider = provider)
    }

    override fun setLlmStreamingEnabled(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(llmStreamingEnabled = enabled)
    }

    // Privacy Settings
    override fun setCrashReportingEnabled(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(crashReportingEnabled = enabled)
    }

    override fun setAnalyticsEnabled(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(analyticsEnabled = enabled)
    }

    // UI Preferences
    override fun setTheme(theme: String) {
        _uiState.value = _uiState.value.copy(theme = theme)
    }

    override fun setConversationMode(mode: String) {
        _uiState.value = _uiState.value.copy(conversationMode = mode)
    }

    // Theme Customization (Phase 1.1)
    override fun setAccentColor(color: String) {
        // No-op for testing - SettingsUiState doesn't have accentColor field
    }

    override fun setDynamicColor(enabled: Boolean) {
        // No-op for testing - SettingsUiState doesn't have dynamicColor field
    }

    // Storage Actions
    override fun clearCache() {
        _uiState.value = _uiState.value.copy(cacheSize = 0)
    }

    // Developer Settings
    override fun setEmbeddingModel(modelId: String) {
        _uiState.value = _uiState.value.copy(selectedEmbeddingModel = modelId)
    }

    override fun showModelInfo(modelInfo: ModelInfo) {
        _uiState.value = _uiState.value.copy(
            showModelInfoDialog = true,
            modelInfoToShow = modelInfo
        )
    }

    override fun dismissModelInfoDialog() {
        _uiState.value = _uiState.value.copy(
            showModelInfoDialog = false,
            modelInfoToShow = null
        )
    }

    // Model Download Management
    override fun startModelDownload(modelId: String) {
        val currentStates = _uiState.value.modelDownloadStates.toMutableMap()
        currentStates[modelId] = ModelDownloadState(isDownloading = true, percentage = 0f)
        _uiState.value = _uiState.value.copy(modelDownloadStates = currentStates)
    }

    override fun cancelModelDownload(modelId: String) {
        val currentStates = _uiState.value.modelDownloadStates.toMutableMap()
        currentStates[modelId] = ModelDownloadState()
        _uiState.value = _uiState.value.copy(modelDownloadStates = currentStates)
    }

    override fun deleteModel(modelId: String) {
        val currentStates = _uiState.value.modelDownloadStates.toMutableMap()
        currentStates.remove(modelId)
        _uiState.value = _uiState.value.copy(modelDownloadStates = currentStates)
    }

    // About Actions
    override fun openLicenses() {
        // No-op for testing
    }

    // App Preferences (Chapter 71: Intelligent Resolution)
    override fun loadAppPreferences() {
        // No-op for testing
    }

    override fun clearAppPreference(capability: String) {
        // No-op for testing
    }

    override fun clearAllAppPreferences() {
        // No-op for testing
    }

    // Test helpers - Set initial state for testing
    fun setInitialState(state: SettingsUiState) {
        _uiState.value = state
    }

    fun setModelDownloadStates(states: Map<String, ModelDownloadState>) {
        _uiState.value = _uiState.value.copy(modelDownloadStates = states)
    }
}
