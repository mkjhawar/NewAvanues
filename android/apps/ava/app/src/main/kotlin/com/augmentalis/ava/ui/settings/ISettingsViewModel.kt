// filename: apps/ava-standalone/src/main/kotlin/com/augmentalis/ava/ui/settings/ISettingsViewModel.kt
// created: 2025-11-16
// author: AVA AI Team
// © Augmentalis Inc, Intelligent Devices LLC

package com.augmentalis.ava.ui.settings

import kotlinx.coroutines.flow.StateFlow

/**
 * Interface for SettingsViewModel to enable isolated component testing
 *
 * This interface defines the contract that both the real SettingsViewModel
 * (with Hilt dependencies) and FakeSettingsViewModel (for tests) must implement.
 *
 * Benefits:
 * - Enables isolated component testing without MainActivity navigation
 * - Allows tests to use FakeViewModel without Hilt setup
 * - Maintains clean architecture and testability
 *
 * Created: 2025-11-16 (Technical Debt Resolution - UI Test Coverage)
 */
interface ISettingsViewModel {
    /**
     * UI state exposed to SettingsScreen
     */
    val uiState: StateFlow<SettingsUiState>

    // NLU Settings
    fun setNluEnabled(enabled: Boolean)
    fun setNluConfidenceThreshold(threshold: Float)

    // ADR-014: Advanced NLU Thresholds (Developer Mode)
    fun setTeachThreshold(threshold: Float)
    fun setLLMFallbackThreshold(threshold: Float)
    fun setSelfLearningThreshold(threshold: Float)

    // LLM Settings
    fun setLlmProvider(provider: String)
    fun setLlmStreamingEnabled(enabled: Boolean)

    // Privacy Settings
    fun setCrashReportingEnabled(enabled: Boolean)
    fun setAnalyticsEnabled(enabled: Boolean)

    // UI Preferences
    fun setTheme(theme: String)
    fun setConversationMode(mode: String)

    // Theme Customization (Phase 1.1)
    fun setAccentColor(color: String)
    fun setDynamicColor(enabled: Boolean)

    // Storage Actions
    fun clearCache()

    // Developer Settings
    fun setEmbeddingModel(modelId: String)
    fun showModelInfo(modelInfo: ModelInfo)
    fun dismissModelInfoDialog()

    // Model Download Management
    fun startModelDownload(modelId: String)
    fun cancelModelDownload(modelId: String)
    fun deleteModel(modelId: String)

    // NLU Model Download
    fun downloadNLUModel()
    fun deleteNLUModel()

    // About Actions
    fun openLicenses()

    // App Preferences (Chapter 71: Intelligent Resolution)
    fun loadAppPreferences()
    fun clearAppPreference(capability: String)
    fun clearAllAppPreferences()
}
