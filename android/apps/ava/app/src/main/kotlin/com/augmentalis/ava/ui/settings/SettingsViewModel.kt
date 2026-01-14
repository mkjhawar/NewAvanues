// filename: apps/ava-standalone/src/main/kotlin/com/augmentalis/ava/ui/settings/SettingsViewModel.kt
// created: 2025-11-04
// author: AVA AI Team
// © Augmentalis Inc, Intelligent Devices LLC

package com.augmentalis.ava.ui.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.augmentalis.ava.crashreporting.CrashReporter
import com.augmentalis.llm.alc.loader.HuggingFaceModelDownloader
import com.augmentalis.llm.alc.loader.ModelDownloadConfig
import com.augmentalis.ava.preferences.UserPreferences
import com.augmentalis.ava.ui.settings.ModelInfo
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.File
import javax.inject.Inject

/**
 * ViewModel for Settings screen
 *
 * Manages user preferences using DataStore:
 * - NLU settings (enabled, confidence threshold, cache size)
 * - LLM settings (provider, streaming)
 * - Privacy settings (crash reporting, analytics)
 * - UI preferences (theme)
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val userPreferences: UserPreferences,
    private val dataStore: DataStore<Preferences>,
    private val appPreferencesRepository: com.augmentalis.ava.core.domain.repository.AppPreferencesRepository
) : ViewModel(), ISettingsViewModel {

    // Chat preferences
    private val chatPreferences = com.augmentalis.ava.core.data.prefs.ChatPreferences.getInstance(context)

    // Model downloader (LLM models)
    private val modelDownloader = HuggingFaceModelDownloader(context)

    // NLU model downloader
    private val nluDownloader = com.augmentalis.nlu.download.NLUModelDownloader(
        context = context,
        workManager = androidx.work.WorkManager.getInstance(context)
    )

    // Download jobs map (for cancellation)
    private val downloadJobs = mutableMapOf<String, Job>()

    // Preference keys (for settings not in UserPreferences)
    private object PreferencesKeys {
        val NLU_ENABLED = booleanPreferencesKey("nlu_enabled")
        val NLU_CONFIDENCE_THRESHOLD = floatPreferencesKey("nlu_confidence_threshold")
        val LLM_PROVIDER = stringPreferencesKey("llm_provider")
        val LLM_STREAMING_ENABLED = booleanPreferencesKey("llm_streaming_enabled")
        val EMBEDDING_MODEL = stringPreferencesKey("embedding_model")
    }

    // UI State
    private val _uiState = MutableStateFlow(SettingsUiState())
    override val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        loadPreferences()
        calculateCacheSize()
        checkDownloadedModels()
        checkNLUModels()
        loadAppPreferences()
    }

    private fun loadPreferences() {
        viewModelScope.launch {
            // Combine UserPreferences with local DataStore and ChatPreferences
            combine(
                userPreferences.crashReportingEnabled,
                userPreferences.analyticsEnabled,
                userPreferences.themeMode,
                chatPreferences.conversationMode,
                chatPreferences.confidenceThreshold,
                chatPreferences.teachThreshold,
                chatPreferences.llmFallbackThreshold,
                chatPreferences.selfLearningThreshold,
                dataStore.data
            ) { values ->
                // Destructure the array of values
                val crashReporting = values[0] as Boolean
                val analytics = values[1] as Boolean
                val theme = values[2] as String
                val conversationMode = values[3] as com.augmentalis.ava.core.data.prefs.ConversationMode
                val confidenceThreshold = values[4] as Float
                val teachThreshold = values[5] as Float
                val llmFallbackThreshold = values[6] as Float
                val selfLearningThreshold = values[7] as Float
                val localPrefs = values[8] as Preferences

                SettingsUiState(
                    nluEnabled = localPrefs[PreferencesKeys.NLU_ENABLED] ?: true,
                    nluConfidenceThreshold = confidenceThreshold,
                    teachThreshold = teachThreshold,
                    llmFallbackThreshold = llmFallbackThreshold,
                    selfLearningThreshold = selfLearningThreshold,
                    llmProvider = localPrefs[PreferencesKeys.LLM_PROVIDER] ?: "Local (On-Device)",
                    llmStreamingEnabled = localPrefs[PreferencesKeys.LLM_STREAMING_ENABLED] ?: true,
                    crashReportingEnabled = crashReporting,
                    analyticsEnabled = analytics,
                    theme = theme.replaceFirstChar { it.uppercase() },  // "auto" -> "Auto"
                    conversationMode = conversationMode.name.lowercase().replaceFirstChar { it.uppercase() }, // "APPEND" -> "Append"
                    selectedEmbeddingModel = localPrefs[PreferencesKeys.EMBEDDING_MODEL] ?: "AVA-ONX-384-BASE-INT8",
                    deviceLanguage = getDeviceLanguage(),
                    recommendedModel = getRecommendedModelForLanguage()
                )
            }.collect { newState ->
                _uiState.update { it.copy(
                    nluEnabled = newState.nluEnabled,
                    nluConfidenceThreshold = newState.nluConfidenceThreshold,
                    teachThreshold = newState.teachThreshold,
                    llmFallbackThreshold = newState.llmFallbackThreshold,
                    selfLearningThreshold = newState.selfLearningThreshold,
                    llmProvider = newState.llmProvider,
                    llmStreamingEnabled = newState.llmStreamingEnabled,
                    crashReportingEnabled = newState.crashReportingEnabled,
                    analyticsEnabled = newState.analyticsEnabled,
                    theme = newState.theme,
                    conversationMode = newState.conversationMode,
                    selectedEmbeddingModel = newState.selectedEmbeddingModel,
                    deviceLanguage = newState.deviceLanguage,
                    recommendedModel = newState.recommendedModel
                )}
            }
        }
    }

    private fun getDeviceLanguage(): String {
        return java.util.Locale.getDefault().language
    }

    private fun getRecommendedModelForLanguage(): String {
        val language = getDeviceLanguage()
        return when (language) {
            "en" -> "AVA-ONX-384-BASE-INT8"  // Bundled, instant use
            "zh" -> "AVA-ONX-384-MULTI-INT8" // Chinese
            "ja" -> "AVA-ONX-384-MULTI-INT8" // Japanese
            "ko" -> "AVA-ONX-384-MULTI-INT8" // Korean
            "hi" -> "AVA-ONX-384-MULTI-INT8" // Hindi
            "ru" -> "AVA-ONX-384-MULTI-INT8" // Russian
            "ar" -> "AVA-ONX-384-MULTI-INT8" // Arabic
            "es", "fr", "de", "it", "pt" -> "AVA-ONX-384-MULTI-INT8" // Romance languages
            else -> "AVA-ONX-384-MULTI-INT8" // Default to multilingual
        }
    }

    fun shouldSuggestMultilingualDownload(): Boolean {
        val language = getDeviceLanguage()
        val currentModel = _uiState.value.selectedEmbeddingModel

        // Suggest if not English and not already using MULTI model
        return language != "en" && !currentModel.contains("MULTI")
    }

    private fun calculateCacheSize() {
        viewModelScope.launch {
            try {
                val cacheDir = context.cacheDir
                val size = calculateDirectorySize(cacheDir)
                val sizeMb = (size / (1024.0 * 1024.0)).toInt()
                _uiState.update { it.copy(cacheSize = sizeMb) }
            } catch (e: Exception) {
                Timber.e(e, "Failed to calculate cache size")
                _uiState.update { it.copy(cacheSize = 0) }
            }
        }
    }

    private fun calculateDirectorySize(directory: File): Long {
        var size: Long = 0
        if (directory.exists() && directory.isDirectory) {
            directory.listFiles()?.forEach { file ->
                size += if (file.isDirectory) {
                    calculateDirectorySize(file)
                } else {
                    file.length()
                }
            }
        }
        return size
    }

    // NLU Settings
    override fun setNluEnabled(enabled: Boolean) {
        viewModelScope.launch {
            dataStore.edit { preferences ->
                preferences[PreferencesKeys.NLU_ENABLED] = enabled
            }
            Timber.d("NLU enabled: $enabled")
        }
    }

    override fun setNluConfidenceThreshold(threshold: Float) {
        viewModelScope.launch {
            // Sync to both DataStore (for UI) and ChatPreferences (for ChatViewModel)
            dataStore.edit { preferences ->
                preferences[PreferencesKeys.NLU_CONFIDENCE_THRESHOLD] = threshold
            }
            chatPreferences.setConfidenceThreshold(threshold)
            Timber.d("NLU confidence threshold: $threshold")
        }
    }

    // ADR-014: Advanced threshold setters (developer mode)
    override fun setTeachThreshold(threshold: Float) {
        viewModelScope.launch {
            chatPreferences.setTeachThreshold(threshold)
            Timber.d("Teach threshold: $threshold")
        }
    }

    override fun setLLMFallbackThreshold(threshold: Float) {
        viewModelScope.launch {
            chatPreferences.setLLMFallbackThreshold(threshold)
            Timber.d("LLM fallback threshold: $threshold")
        }
    }

    override fun setSelfLearningThreshold(threshold: Float) {
        viewModelScope.launch {
            chatPreferences.setSelfLearningThreshold(threshold)
            Timber.d("Self-learning threshold: $threshold")
        }
    }

    // LLM Settings
    override fun setLlmProvider(provider: String) {
        viewModelScope.launch {
            dataStore.edit { preferences ->
                preferences[PreferencesKeys.LLM_PROVIDER] = provider
            }
            Timber.d("LLM provider: $provider")
        }
    }

    override fun setLlmStreamingEnabled(enabled: Boolean) {
        viewModelScope.launch {
            dataStore.edit { preferences ->
                preferences[PreferencesKeys.LLM_STREAMING_ENABLED] = enabled
            }
            Timber.d("LLM streaming enabled: $enabled")
        }
    }

    // Privacy Settings
    override fun setCrashReportingEnabled(enabled: Boolean) {
        viewModelScope.launch {
            // Use UserPreferences for crash reporting
            userPreferences.setCrashReportingEnabled(enabled)
            Timber.d("Crash reporting enabled: $enabled")

            // Enable/disable crash reporting in CrashReporter
            CrashReporter.setEnabled(enabled)
        }
    }

    override fun setAnalyticsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            // Use UserPreferences for analytics
            userPreferences.setAnalyticsEnabled(enabled)
            Timber.d("Analytics enabled: $enabled")

            // TODO: Enable/disable analytics SDK when integrated
        }
    }

    // UI Preferences
    override fun setTheme(theme: String) {
        viewModelScope.launch {
            // Convert display name to mode (e.g., "System Default" -> "auto")
            val mode = when (theme.lowercase()) {
                "light" -> "light"
                "dark" -> "dark"
                else -> "auto"  // System Default
            }

            // Use UserPreferences for theme
            userPreferences.setThemeMode(mode)
            Timber.d("Theme: $theme (mode: $mode)")

            // Theme change is applied automatically via Flow observation in MainActivity
        }
    }

    override fun setConversationMode(mode: String) {
        viewModelScope.launch {
            val conversationMode = when (mode.lowercase()) {
                "new" -> com.augmentalis.ava.core.data.prefs.ConversationMode.NEW
                else -> com.augmentalis.ava.core.data.prefs.ConversationMode.APPEND
            }
            chatPreferences.setConversationMode(conversationMode)
            Timber.d("Conversation mode: $mode")
        }
    }

    // Theme Customization (Phase 1.1)
    override fun setAccentColor(color: String) {
        viewModelScope.launch {
            userPreferences.setAccentColor(color)
            Timber.d("Accent color: $color")
            // Theme change is applied automatically via Flow observation in MainActivity
        }
    }

    override fun setDynamicColor(enabled: Boolean) {
        viewModelScope.launch {
            userPreferences.setUseDynamicColor(enabled)
            Timber.d("Dynamic color: $enabled")
            // Theme change is applied automatically via Flow observation in MainActivity
        }
    }

    // Storage Actions
    override fun clearCache() {
        viewModelScope.launch {
            try {
                Timber.d("Clearing cache...")
                context.cacheDir.deleteRecursively()
                context.cacheDir.mkdirs()
                calculateCacheSize()
                Timber.d("Cache cleared successfully")
            } catch (e: Exception) {
                Timber.e(e, "Failed to clear cache")
            }
        }
    }

    // About Actions
    override fun openLicenses() {
        Timber.d("Opening licenses screen")
        // TODO: Open licenses screen
    }

    // ==================== App Preferences (Chapter 71: Intelligent Resolution) ====================

    /**
     * Load saved app preferences for display in Settings
     */
    override fun loadAppPreferences() {
        viewModelScope.launch {
            try {
                val prefsMap = appPreferencesRepository.getAllPreferences()
                val savedPrefs = prefsMap.values.map { pref ->
                    val displayName = com.augmentalis.ava.core.domain.resolution.CapabilityRegistry
                        .capabilities[pref.capability]?.displayName
                        ?: pref.capability.replaceFirstChar { c -> c.uppercase() }

                    SavedAppPreference(
                        capability = pref.capability,
                        capabilityDisplayName = displayName,
                        appName = pref.appName,
                        packageName = pref.packageName
                    )
                }
                _uiState.update { it.copy(savedAppPreferences = savedPrefs) }
                Timber.d("Loaded ${savedPrefs.size} app preferences")
            } catch (e: Exception) {
                Timber.e(e, "Failed to load app preferences")
            }
        }
    }

    /**
     * Clear a specific app preference
     */
    override fun clearAppPreference(capability: String) {
        viewModelScope.launch {
            try {
                appPreferencesRepository.clearPreferredApp(capability)
                loadAppPreferences() // Refresh the list
                Timber.d("Cleared app preference for: $capability")
            } catch (e: Exception) {
                Timber.e(e, "Failed to clear app preference: $capability")
            }
        }
    }

    /**
     * Clear all app preferences
     */
    override fun clearAllAppPreferences() {
        viewModelScope.launch {
            try {
                // Clear each preference individually
                val prefsMap = appPreferencesRepository.getAllPreferences()
                prefsMap.keys.forEach { capability ->
                    appPreferencesRepository.clearPreferredApp(capability)
                }
                _uiState.update { it.copy(savedAppPreferences = emptyList()) }
                Timber.d("Cleared all app preferences")
            } catch (e: Exception) {
                Timber.e(e, "Failed to clear all app preferences")
            }
        }
    }

    // Public accessor for preferences
    fun getPreferences(): Flow<Preferences> {
        return dataStore.data
    }

    // Get specific preference values
    suspend fun isNluEnabled(): Boolean {
        return dataStore.data.first()[PreferencesKeys.NLU_ENABLED] ?: true
    }

    suspend fun getNluConfidenceThreshold(): Float {
        return dataStore.data.first()[PreferencesKeys.NLU_CONFIDENCE_THRESHOLD] ?: 0.75f
    }

    suspend fun getLlmProvider(): String {
        return dataStore.data.first()[PreferencesKeys.LLM_PROVIDER] ?: "Local (On-Device)"
    }

    suspend fun isLlmStreamingEnabled(): Boolean {
        return dataStore.data.first()[PreferencesKeys.LLM_STREAMING_ENABLED] ?: true
    }

    suspend fun isCrashReportingEnabled(): Boolean {
        return userPreferences.crashReportingEnabled.first()
    }

    // Developer Settings
    override fun setEmbeddingModel(modelId: String) {
        viewModelScope.launch {
            dataStore.edit { preferences ->
                preferences[PreferencesKeys.EMBEDDING_MODEL] = modelId
            }
            Timber.d("Embedding model: $modelId")
        }
    }

    override fun showModelInfo(modelInfo: ModelInfo) {
        _uiState.update { currentState ->
            currentState.copy(
                showModelInfoDialog = true,
                modelInfoToShow = modelInfo
            )
        }
    }

    override fun dismissModelInfoDialog() {
        _uiState.update { currentState ->
            currentState.copy(
                showModelInfoDialog = false,
                modelInfoToShow = null
            )
        }
    }

    suspend fun getEmbeddingModel(): String {
        return dataStore.data.first()[PreferencesKeys.EMBEDDING_MODEL] ?: "AVA-ONX-384-BASE-INT8"
    }

    // Model Download Management

    /**
     * Check which models are already downloaded
     */
    private fun checkDownloadedModels() {
        viewModelScope.launch {
            val models = getAvailableLlmModelIds()
            val downloadStates = models.associate { modelId ->
                modelId to ModelDownloadState(
                    isDownloaded = modelDownloader.isModelDownloaded(modelId)
                )
            }
            _uiState.update { it.copy(modelDownloadStates = downloadStates) }
        }
    }

    /**
     * Start downloading a model
     */
    override fun startModelDownload(modelId: String) {
        // Cancel any existing download for this model
        downloadJobs[modelId]?.cancel()

        val job = viewModelScope.launch {
            try {
                Timber.i("Starting model download: $modelId")

                // Get model URL from model info
                val modelUrl = getModelUrl(modelId)
                val config = ModelDownloadConfig(
                    modelId = modelId,
                    modelUrl = modelUrl
                )

                // Update state to downloading
                updateModelDownloadState(modelId) {
                    ModelDownloadState(isDownloading = true)
                }

                // Start download with progress tracking
                modelDownloader.downloadModel(config).collect { progress ->
                    val sizeMB = progress.totalBytes / (1024 * 1024)
                    val downloadedMB = progress.bytesDownloaded / (1024 * 1024)

                    updateModelDownloadState(modelId) {
                        ModelDownloadState(
                            isDownloading = true,
                            currentFile = progress.fileName,
                            percentage = progress.percentage,
                            totalMB = sizeMB.toInt(),
                            downloadedMB = downloadedMB.toInt()
                        )
                    }
                }

                // Download complete
                updateModelDownloadState(modelId) {
                    ModelDownloadState(isDownloaded = true)
                }

                Timber.i("Model download complete: $modelId")

            } catch (e: Exception) {
                Timber.e(e, "Model download failed: $modelId")

                // Reset state on error
                updateModelDownloadState(modelId) {
                    ModelDownloadState()
                }
            }
        }

        downloadJobs[modelId] = job
    }

    /**
     * Cancel ongoing model download
     */
    override fun cancelModelDownload(modelId: String) {
        Timber.d("Cancelling model download: $modelId")
        downloadJobs[modelId]?.cancel()
        downloadJobs.remove(modelId)

        // Reset download state
        updateModelDownloadState(modelId) {
            ModelDownloadState()
        }
    }

    /**
     * Delete downloaded model
     */
    override fun deleteModel(modelId: String) {
        viewModelScope.launch {
            try {
                Timber.i("Deleting model: $modelId")
                modelDownloader.deleteModel(modelId)

                // Update state
                updateModelDownloadState(modelId) {
                    ModelDownloadState()
                }

                Timber.i("Model deleted: $modelId")
            } catch (e: Exception) {
                Timber.e(e, "Failed to delete model: $modelId")
            }
        }
    }

    /**
     * Update download state for a specific model
     */
    private fun updateModelDownloadState(
        modelId: String,
        update: () -> ModelDownloadState
    ) {
        _uiState.update { currentState ->
            val newStates = currentState.modelDownloadStates.toMutableMap()
            newStates[modelId] = update()
            currentState.copy(modelDownloadStates = newStates)
        }
    }

    /**
     * Get model URL from model ID
     */
    private fun getModelUrl(modelId: String): String {
        return when (modelId) {
            "gemma-2-2b-it-q4f16_1-MLC" -> "https://huggingface.co/mlc-ai/gemma-2-2b-it-q4f16_1-MLC"
            "Qwen2.5-1.5B-Instruct-q4f16_1-MLC" -> "https://huggingface.co/mlc-ai/Qwen2.5-1.5B-Instruct-q4f16_1-MLC"
            "Llama-3.2-3B-Instruct-q4f16_0-MLC" -> "https://huggingface.co/mlc-ai/Llama-3.2-3B-Instruct-q4f16_0-MLC"
            "Phi-3.5-mini-instruct-q4f16_0-MLC" -> "https://huggingface.co/mlc-ai/Phi-3.5-mini-instruct-q4f16_0-MLC"
            "Mistral-7B-Instruct-v0.3-q4f16_1-MLC" -> "https://huggingface.co/mlc-ai/Mistral-7B-Instruct-v0.3-q4f16_1-MLC"
            else -> throw IllegalArgumentException("Unknown model: $modelId")
        }
    }

    /**
     * Get list of available LLM model IDs
     */
    private fun getAvailableLlmModelIds(): List<String> {
        return listOf(
            "gemma-2-2b-it-q4f16_1-MLC",
            "Qwen2.5-1.5B-Instruct-q4f16_1-MLC",
            "Llama-3.2-3B-Instruct-q4f16_0-MLC",
            "Phi-3.5-mini-instruct-q4f16_0-MLC",
            "Mistral-7B-Instruct-v0.3-q4f16_1-MLC"
        )
    }

    // ==================== NLU Model Download ====================

    /**
     * Check which NLU models are downloaded
     */
    private fun checkNLUModels() {
        viewModelScope.launch {
            val malbertDownloaded = nluDownloader.isModelDownloaded("AVA-768-Base-INT8.AON")
            _uiState.update { it.copy(nluModelDownloaded = malbertDownloaded) }
        }
    }

    /**
     * Download NLU model (mALBERT)
     */
    override fun downloadNLUModel() {
        viewModelScope.launch {
            try {
                Timber.i("Starting NLU model download...")

                // Update state to downloading
                _uiState.update { it.copy(nluDownloadProgress = 0f) }

                // Collect download state
                nluDownloader.downloadState.collect { state ->
                    when (state) {
                        is com.augmentalis.nlu.download.NLUModelDownloader.DownloadState.Downloading -> {
                            _uiState.update { it.copy(nluDownloadProgress = state.progress) }
                            Timber.d("NLU download progress: ${(state.progress * 100).toInt()}%")
                        }
                        is com.augmentalis.nlu.download.NLUModelDownloader.DownloadState.Success -> {
                            _uiState.update {
                                it.copy(
                                    nluModelDownloaded = true,
                                    nluDownloadProgress = 1f
                                )
                            }
                            Timber.i("NLU model download complete: ${state.modelPath}")
                            // TODO: Show success notification
                        }
                        is com.augmentalis.nlu.download.NLUModelDownloader.DownloadState.Failed -> {
                            _uiState.update { it.copy(nluDownloadProgress = 0f) }
                            Timber.e("NLU model download failed: ${state.error}")
                            // TODO: Show error notification
                        }
                        else -> {
                            // Idle state
                        }
                    }
                }

                // Start download
                val result = nluDownloader.downloadModel(
                    modelId = "AVA-768-Base-INT8.AON",
                    modelUrl = "https://huggingface.co/sentence-transformers/paraphrase-multilingual-MiniLM-L12-v2/resolve/main/onnx/model.onnx",
                    expectedChecksum = "TBD" // TODO: Add actual checksum
                )

                when (result) {
                    is com.augmentalis.ava.core.common.Result.Success -> {
                        Timber.i("NLU model downloaded successfully")
                    }
                    is com.augmentalis.ava.core.common.Result.Error -> {
                        Timber.e("NLU model download failed: ${result.message}")
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to download NLU model")
                _uiState.update { it.copy(nluDownloadProgress = 0f) }
            }
        }
    }

    /**
     * Delete NLU model
     */
    override fun deleteNLUModel() {
        viewModelScope.launch {
            try {
                Timber.i("Deleting NLU model...")
                nluDownloader.deleteModel("AVA-768-Base-INT8.AON")
                _uiState.update {
                    it.copy(
                        nluModelDownloaded = false,
                        nluDownloadProgress = 0f
                    )
                }
                Timber.i("NLU model deleted")
            } catch (e: Exception) {
                Timber.e(e, "Failed to delete NLU model")
            }
        }
    }
}

/**
 * UI State for Settings screen
 */
data class SettingsUiState(
    // NLU Settings
    val nluEnabled: Boolean = true,
    val nluConfidenceThreshold: Float = 0.65f,

    // ADR-014: Advanced NLU thresholds (developer mode)
    val teachThreshold: Float = 0.5f,
    val llmFallbackThreshold: Float = 0.65f,
    val selfLearningThreshold: Float = 0.65f,
    val developerModeEnabled: Boolean = false,

    // LLM Settings
    val llmProvider: String = "Local (On-Device)",
    val llmStreamingEnabled: Boolean = true,

    // Privacy Settings
    val crashReportingEnabled: Boolean = false,
    val analyticsEnabled: Boolean = false,

    // UI Preferences
    val theme: String = "System Default",

    // Chat Preferences
    val conversationMode: String = "Append",

    // App Preferences (Chapter 71: Intelligent Resolution)
    val savedAppPreferences: List<SavedAppPreference> = emptyList(),

    // Storage
    val cacheSize: Int = 0, // in MB

    // Developer Settings
    val selectedEmbeddingModel: String = "AVA-ONX-384-BASE-INT8",
    val showModelInfoDialog: Boolean = false,
    val modelInfoToShow: ModelInfo? = null,
    val deviceLanguage: String = "en",
    val recommendedModel: String = "AVA-ONX-384-BASE-INT8",

    // Model Downloads
    val modelDownloadStates: Map<String, ModelDownloadState> = emptyMap(),

    // NLU Model Downloads
    val nluModelDownloaded: Boolean = false,
    val nluDownloadProgress: Float = 0f
)

/**
 * Data class representing a saved app preference for display in Settings
 *
 * Part of Intelligent Resolution System (Chapter 71)
 */
data class SavedAppPreference(
    val capability: String,
    val capabilityDisplayName: String,
    val appName: String,
    val packageName: String
)
