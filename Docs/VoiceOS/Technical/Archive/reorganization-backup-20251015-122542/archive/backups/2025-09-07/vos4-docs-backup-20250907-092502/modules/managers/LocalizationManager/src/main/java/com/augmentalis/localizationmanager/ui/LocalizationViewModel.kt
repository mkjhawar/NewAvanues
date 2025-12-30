/**
 * LocalizationViewModel.kt - ViewModel for Localization Manager UI
 * 
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2025-01-02
 * 
 * Manages UI state and business logic for localization management
 */
package com.augmentalis.localizationmanager.ui

import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.augmentalis.localizationmanager.LocalizationModule
import com.augmentalis.localizationmanager.repository.PreferencesRepository
import com.augmentalis.localizationmanager.data.DebounceDuration
import com.augmentalis.localizationmanager.data.DetailLevel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * Language info model
 */
data class LanguageInfo(
    val code: String,
    val name: String,
    val nativeName: String,
    val region: String,
    val isActive: Boolean,
    val isDownloaded: Boolean,
    val downloadSize: Long,
    val supportedEngines: List<String>
)

/**
 * Translation pair model
 */
data class TranslationPair(
    val sourceLanguage: String,
    val targetLanguage: String,
    val isAvailable: Boolean,
    val isOfflineCapable: Boolean
)

/**
 * Language statistics model
 */
data class LanguageStatistics(
    val totalLanguages: Int,
    val downloadedLanguages: Int,
    val activeLanguage: String,
    val totalTranslations: Int,
    val storageUsed: Long,
    val lastSync: Long,
    val voskSupported: Int,
    val vivokaSupported: Int
)

/**
 * Download progress model
 */
data class DownloadProgress(
    val languageCode: String,
    val languageName: String,
    val progress: Float,
    val downloadedBytes: Long,
    val totalBytes: Long,
    val status: DownloadStatus
)

enum class DownloadStatus {
    PENDING,
    DOWNLOADING,
    PAUSED,
    COMPLETED,
    FAILED,
    CANCELLED
}

/**
 * Translation request model
 */
data class TranslationRequest(
    val text: String,
    val sourceLanguage: String,
    val targetLanguage: String,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Translation result model
 */
data class TranslationResult(
    val originalText: String,
    val translatedText: String,
    val sourceLanguage: String,
    val targetLanguage: String,
    val confidence: Float,
    val timestamp: Long
)

/**
 * UI State model for testing compatibility
 */
data class LocalizationUiState(
    val currentLanguage: LanguageInfo?,
    val availableLanguages: List<LanguageInfo>,
    val downloadedLanguages: List<LanguageInfo>,
    val downloadingLanguage: String?,
    val downloadProgress: Float,
    val searchQuery: String,
    val searchResults: List<LanguageInfo>,
    val selectedRegion: String,
    val statistics: LanguageStatistics?,
    val isLoading: Boolean,
    val errorMessage: String?
)

/**
 * ViewModel for Localization Manager UI
 * Direct implementation pattern - no dependency injection
 */
open class LocalizationViewModel(
    private val context: Context,
    private val preferencesRepository: PreferencesRepository
) : ViewModel() {
    
    companion object {
        private const val TAG = "LocalizationViewModel"
    }
    
    private val localizationModule = LocalizationModule.getInstance(context)
    
    private val _currentLanguage = MutableStateFlow("en")
    val currentLanguage: StateFlow<String> = _currentLanguage.asStateFlow()
    
    private val _availableLanguages = MutableLiveData<List<LanguageInfo>>()
    val availableLanguages: LiveData<List<LanguageInfo>> = _availableLanguages
    
    private val _downloadedLanguages = MutableLiveData<List<LanguageInfo>>()
    val downloadedLanguages: LiveData<List<LanguageInfo>> = _downloadedLanguages
    
    private val _languageStatistics = MutableLiveData<LanguageStatistics>()
    val languageStatistics: LiveData<LanguageStatistics> = _languageStatistics
    
    private val _translationPairs = MutableLiveData<List<TranslationPair>>()
    val translationPairs: LiveData<List<TranslationPair>> = _translationPairs
    
    private val _downloadProgress = MutableStateFlow<DownloadProgress?>(null)
    val downloadProgress: StateFlow<DownloadProgress?> = _downloadProgress.asStateFlow()
    
    private val _recentTranslations = MutableLiveData<List<TranslationResult>>()
    val recentTranslations: LiveData<List<TranslationResult>> = _recentTranslations
    
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading
    
    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage
    
    private val _successMessage = MutableLiveData<String?>()
    val successMessage: LiveData<String?> = _successMessage
    
    // User preferences
    val debounceDuration: StateFlow<Long> = preferencesRepository.getDebounceDuration()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 2000L)
    
    val statisticsAutoShow: StateFlow<Boolean> = preferencesRepository.getStatisticsAutoShow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
    
    val languageAnimationEnabled: StateFlow<Boolean> = preferencesRepository.getLanguageAnimationEnabled()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)
    
    // Combined UI state for testing compatibility
    private val _uiState = MutableLiveData<LocalizationUiState>()
    val uiState: LiveData<LocalizationUiState> = _uiState
    
    // Search and filter state
    private var currentSearchQuery: String = ""
    private var currentSearchResults: List<LanguageInfo> = emptyList()
    private var selectedRegion: String = "All"
    
    init {
        loadInitialData()
        observeLanguageChanges()
        updateUiState()
    }
    
    /**
     * Update combined UI state for testing compatibility
     */
    private fun updateUiState() {
        val currentLang = _currentLanguage.value
        val available = _availableLanguages.value ?: emptyList()
        val downloaded = _downloadedLanguages.value ?: emptyList()
        val stats = _languageStatistics.value
        val isLoading = _isLoading.value ?: false
        val errorMessage = _errorMessage.value
        val downloadProgress = _downloadProgress.value
        
        val currentLanguageInfo = available.find { it.code == currentLang }
        
        _uiState.value = LocalizationUiState(
            currentLanguage = currentLanguageInfo,
            availableLanguages = available,
            downloadedLanguages = downloaded,
            downloadingLanguage = downloadProgress?.languageCode,
            downloadProgress = downloadProgress?.progress ?: 0f,
            searchQuery = currentSearchQuery,
            searchResults = currentSearchResults,
            selectedRegion = selectedRegion,
            statistics = stats,
            isLoading = isLoading,
            errorMessage = errorMessage
        )
    }
    
    /**
     * Load initial data
     */
    private fun loadInitialData() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                
                // Initialize localization module
                localizationModule.initialize()
                
                // Load current language
                loadCurrentLanguage()
                
                // Load available languages
                loadAvailableLanguages()
                
                // Load downloaded languages
                loadDownloadedLanguages()
                
                // Load language statistics
                refreshStatistics()
                
                // Load translation pairs
                loadTranslationPairs()
                
                // Load recent translations
                loadRecentTranslations()
                
                Log.d(TAG, "Initial data loaded successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load initial data", e)
                _errorMessage.value = "Failed to load localization data: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Load current language
     */
    private fun loadCurrentLanguage() {
        val currentLocale = Locale.getDefault()
        _currentLanguage.value = currentLocale.language
    }
    
    /**
     * Load available languages
     */
    private fun loadAvailableLanguages() {
        val allLanguages = mutableListOf<LanguageInfo>()
        
        // Add VOSK languages
        LocalizationModule.VOSK_LANGUAGES.forEach { code ->
            val locale = Locale(code)
            allLanguages.add(
                LanguageInfo(
                    code = code,
                    name = locale.displayName,
                    nativeName = locale.getDisplayName(locale),
                    region = getRegionForLanguage(code),
                    isActive = code == _currentLanguage.value,
                    isDownloaded = isLanguageDownloaded(code),
                    downloadSize = getLanguageSize(code),
                    supportedEngines = listOf("VOSK")
                )
            )
        }
        
        // Add VIVOKA languages
        LocalizationModule.VIVOKA_LANGUAGES.forEach { (code, name) ->
            if (!allLanguages.any { it.code == code }) {
                val locale = Locale(code)
                allLanguages.add(
                    LanguageInfo(
                        code = code,
                        name = name,
                        nativeName = locale.getDisplayName(locale),
                        region = getRegionForLanguage(code),
                        isActive = code == _currentLanguage.value,
                        isDownloaded = isLanguageDownloaded(code),
                        downloadSize = getLanguageSize(code),
                        supportedEngines = listOf("VIVOKA")
                    )
                )
            } else {
                // Update existing entry to include VIVOKA
                val index = allLanguages.indexOfFirst { it.code == code }
                if (index >= 0) {
                    val existing = allLanguages[index]
                    allLanguages[index] = existing.copy(
                        supportedEngines = existing.supportedEngines + "VIVOKA"
                    )
                }
            }
        }
        
        _availableLanguages.value = allLanguages.sortedBy { it.name }
    }
    
    /**
     * Load downloaded languages
     */
    private fun loadDownloadedLanguages() {
        val downloaded = _availableLanguages.value?.filter { it.isDownloaded } ?: emptyList()
        _downloadedLanguages.value = downloaded
    }
    
    /**
     * Refresh language statistics
     */
    fun refreshStatistics() {
        viewModelScope.launch {
            try {
                val allLanguages = _availableLanguages.value ?: emptyList()
                val downloaded = allLanguages.filter { it.isDownloaded }
                
                val voskSupportedCount = allLanguages.count { "VOSK" in it.supportedEngines }
                val vivokaSupportedCount = allLanguages.count { "VIVOKA" in it.supportedEngines }
                
                val stats = LanguageStatistics(
                    totalLanguages = allLanguages.size,
                    downloadedLanguages = downloaded.size,
                    activeLanguage = _currentLanguage.value,
                    totalTranslations = calculateTotalTranslations(),
                    storageUsed = calculateStorageUsed(downloaded),
                    lastSync = System.currentTimeMillis(),
                    voskSupported = voskSupportedCount,
                    vivokaSupported = vivokaSupportedCount
                )
                
                _languageStatistics.value = stats
                Log.d(TAG, "Statistics refreshed: $stats")
                
            } catch (e: Exception) {
                Log.e(TAG, "Failed to refresh statistics", e)
                _errorMessage.value = "Failed to refresh statistics: ${e.message}"
            }
        }
    }
    
    /**
     * Load translation pairs
     */
    private fun loadTranslationPairs() {
        val pairs = mutableListOf<TranslationPair>()
        val downloadedCodes = _downloadedLanguages.value?.map { it.code } ?: emptyList()
        
        // Generate translation pairs for downloaded languages
        downloadedCodes.forEach { source ->
            downloadedCodes.forEach { target ->
                if (source != target) {
                    pairs.add(
                        TranslationPair(
                            sourceLanguage = source,
                            targetLanguage = target,
                            isAvailable = true,
                            isOfflineCapable = true
                        )
                    )
                }
            }
        }
        
        _translationPairs.value = pairs
    }
    
    /**
     * Load recent translations
     */
    private fun loadRecentTranslations() {
        // Simulated recent translations
        _recentTranslations.value = listOf(
            TranslationResult(
                originalText = "Hello, how are you?",
                translatedText = "Hola, ¿cómo estás?",
                sourceLanguage = "en",
                targetLanguage = "es",
                confidence = 0.95f,
                timestamp = System.currentTimeMillis() - 60000
            ),
            TranslationResult(
                originalText = "Good morning",
                translatedText = "Bonjour",
                sourceLanguage = "en",
                targetLanguage = "fr",
                confidence = 0.98f,
                timestamp = System.currentTimeMillis() - 120000
            )
        )
    }
    
    /**
     * Change active language
     */
    fun changeLanguage(languageCode: String) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                
                // Simulate language change
                delay(1000)
                
                _currentLanguage.value = languageCode
                
                // Update language info
                val languages = _availableLanguages.value?.map { lang ->
                    lang.copy(isActive = lang.code == languageCode)
                }
                _availableLanguages.value = languages
                
                _successMessage.value = "Language changed to ${getLanguageName(languageCode)}"
                
                // Refresh data
                loadTranslationPairs()
                refreshStatistics()
                
                // Update UI state
                updateUiState()
                
                Log.d(TAG, "Language changed to: $languageCode")
                
            } catch (e: Exception) {
                Log.e(TAG, "Failed to change language", e)
                _errorMessage.value = "Failed to change language: ${e.message}"
            } finally {
                _isLoading.value = false
                updateUiState()
            }
        }
    }
    
    /**
     * Download language pack
     */
    fun downloadLanguage(languageCode: String) {
        viewModelScope.launch {
            try {
                val languageName = getLanguageName(languageCode)
                val totalSize = getLanguageSize(languageCode)
                
                _downloadProgress.value = DownloadProgress(
                    languageCode = languageCode,
                    languageName = languageName,
                    progress = 0f,
                    downloadedBytes = 0,
                    totalBytes = totalSize,
                    status = DownloadStatus.DOWNLOADING
                )
                
                // Simulate download progress
                for (i in 1..10) {
                    delay(500)
                    val progress = i * 0.1f
                    val downloadedBytes = (totalSize * progress).toLong()
                    
                    _downloadProgress.value = DownloadProgress(
                        languageCode = languageCode,
                        languageName = languageName,
                        progress = progress,
                        downloadedBytes = downloadedBytes,
                        totalBytes = totalSize,
                        status = DownloadStatus.DOWNLOADING
                    )
                }
                
                // Mark as completed
                _downloadProgress.value = _downloadProgress.value?.copy(
                    progress = 1f,
                    downloadedBytes = totalSize,
                    status = DownloadStatus.COMPLETED
                )
                
                // Update language info
                updateLanguageDownloadStatus(languageCode, true)
                
                _successMessage.value = "$languageName downloaded successfully"
                
                // Clear progress after delay
                delay(2000)
                _downloadProgress.value = null
                
                // Update UI state
                updateUiState()
                
                Log.d(TAG, "Language downloaded: $languageCode")
                
            } catch (e: Exception) {
                Log.e(TAG, "Failed to download language", e)
                _downloadProgress.value = _downloadProgress.value?.copy(
                    status = DownloadStatus.FAILED
                )
                _errorMessage.value = "Failed to download language: ${e.message}"
                
                delay(2000)
                _downloadProgress.value = null
                updateUiState()
            }
        }
    }
    
    /**
     * Delete language pack
     */
    fun deleteLanguage(languageCode: String) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                
                // Simulate deletion
                delay(500)
                
                updateLanguageDownloadStatus(languageCode, false)
                
                _successMessage.value = "${getLanguageName(languageCode)} deleted"
                
                // Refresh data
                loadDownloadedLanguages()
                loadTranslationPairs()
                refreshStatistics()
                
                Log.d(TAG, "Language deleted: $languageCode")
                
            } catch (e: Exception) {
                Log.e(TAG, "Failed to delete language", e)
                _errorMessage.value = "Failed to delete language: ${e.message}"
            } finally {
                _isLoading.value = false
                updateUiState()
            }
        }
    }
    
    /**
     * Translate text
     */
    fun translateText(text: String, sourceLanguage: String, targetLanguage: String) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                
                // Simulate translation
                delay(1500)
                
                val translatedText = simulateTranslation(text, sourceLanguage, targetLanguage)
                
                val result = TranslationResult(
                    originalText = text,
                    translatedText = translatedText,
                    sourceLanguage = sourceLanguage,
                    targetLanguage = targetLanguage,
                    confidence = 0.92f,
                    timestamp = System.currentTimeMillis()
                )
                
                // Add to recent translations
                val recent = _recentTranslations.value?.toMutableList() ?: mutableListOf()
                recent.add(0, result)
                if (recent.size > 10) {
                    recent.removeAt(recent.size - 1)
                }
                _recentTranslations.value = recent
                
                _successMessage.value = "Translation completed"
                
                Log.d(TAG, "Text translated: $sourceLanguage -> $targetLanguage")
                
            } catch (e: Exception) {
                Log.e(TAG, "Failed to translate text", e)
                _errorMessage.value = "Translation failed: ${e.message}"
            } finally {
                _isLoading.value = false
                updateUiState()
            }
        }
    }
    
    /**
     * Translate text synchronously for testing
     */
    suspend fun translateTextSync(text: String, sourceLanguage: String, targetLanguage: String): String {
        return try {
            // Simulate translation delay
            delay(100)
            simulateTranslation(text, sourceLanguage, targetLanguage)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to translate text", e)
            "Translation failed"
        }
    }
    
    /**
     * Search languages by query
     */
    fun searchLanguages(query: String) {
        currentSearchQuery = query
        val available = _availableLanguages.value ?: emptyList()
        
        currentSearchResults = if (query.isBlank()) {
            available
        } else {
            available.filter { language ->
                language.name.contains(query, ignoreCase = true) ||
                language.nativeName.contains(query, ignoreCase = true) ||
                language.code.contains(query, ignoreCase = true)
            }
        }
        
        updateUiState()
        Log.d(TAG, "Search completed for query: '$query', found ${currentSearchResults.size} results")
    }
    
    /**
     * Refresh languages data
     */
    fun refreshLanguages() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                updateUiState()
                
                // Simulate refresh
                delay(1000)
                
                // Reload data
                loadAvailableLanguages()
                loadDownloadedLanguages()
                refreshStatistics()
                
                _successMessage.value = "Languages refreshed"
                Log.d(TAG, "Languages data refreshed")
                
            } catch (e: Exception) {
                Log.e(TAG, "Failed to refresh languages", e)
                _errorMessage.value = "Failed to refresh languages: ${e.message}"
            } finally {
                _isLoading.value = false
                updateUiState()
            }
        }
    }
    
    /**
     * Cancel download
     */
    fun cancelDownload() {
        _downloadProgress.value = _downloadProgress.value?.copy(
            status = DownloadStatus.CANCELLED
        )
        viewModelScope.launch {
            delay(1000)
            _downloadProgress.value = null
        }
    }
    
    /**
     * Test speech recognition
     */
    fun testSpeechRecognition(languageCode: String) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _successMessage.value = "Testing speech recognition for ${getLanguageName(languageCode)}..."
                
                // Simulate test
                delay(2000)
                
                _successMessage.value = "Speech recognition test successful for ${getLanguageName(languageCode)}"
                
            } catch (e: Exception) {
                _errorMessage.value = "Speech recognition test failed: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Observe language changes
     */
    private fun observeLanguageChanges() {
        viewModelScope.launch {
            localizationModule.languageState.collect { language ->
                if (language != _currentLanguage.value) {
                    _currentLanguage.value = language
                    loadInitialData()
                }
            }
        }
    }
    
    // Helper functions
    
    private fun getRegionForLanguage(code: String): String {
        return when (code) {
            "en", "es", "pt" -> "Americas"
            "fr", "de", "it", "nl", "pl", "cs", "da", "fi", "el", "hu", "no", "sv", "bg", "hr", "ro", "sk", "sl", "et", "lv", "lt", "is", "ga", "mt", "sq", "mk", "sr", "bs", "cy" -> "Europe"
            "zh", "ja", "ko", "hi", "th" -> "Asia"
            "ar", "he", "tr" -> "Middle East"
            "ru", "uk" -> "Eurasia"
            else -> "Other"
        }
    }
    
    private fun isLanguageDownloaded(code: String): Boolean {
        // Simulate downloaded status
        return code in listOf("en", "es", "fr", "de", "zh", "ja")
    }
    
    private fun getLanguageSize(code: String): Long {
        // Simulate language pack sizes in bytes
        return when (code) {
            "en" -> 50 * 1024 * 1024 // 50 MB
            "zh", "ja", "ko" -> 80 * 1024 * 1024 // 80 MB
            else -> 60 * 1024 * 1024 // 60 MB
        }
    }
    
    private fun getLanguageName(code: String): String {
        return LocalizationModule.VIVOKA_LANGUAGES[code] ?: Locale(code).displayName
    }
    
    private fun calculateTotalTranslations(): Int {
        val downloaded = _downloadedLanguages.value?.size ?: 0
        return if (downloaded > 1) downloaded * (downloaded - 1) else 0
    }
    
    private fun calculateStorageUsed(languages: List<LanguageInfo>): Long {
        return languages.sumOf { it.downloadSize }
    }
    
    private fun updateLanguageDownloadStatus(code: String, isDownloaded: Boolean) {
        val languages = _availableLanguages.value?.map { lang ->
            if (lang.code == code) {
                lang.copy(isDownloaded = isDownloaded)
            } else {
                lang
            }
        }
        _availableLanguages.value = languages
        loadDownloadedLanguages()
    }
    
    private fun simulateTranslation(text: String, source: String, target: String): String {
        // Simple simulation - in real app would use translation API
        // Now includes the actual text in the translation
        val translatedPrefix = when {
            source == "en" && target == "es" -> "[ES] "
            source == "en" && target == "fr" -> "[FR] "
            source == "en" && target == "de" -> "[DE] "
            source == "en" && target == "zh" -> "[ZH] "
            source == "en" && target == "ja" -> "[JA] "
            else -> "[${target.uppercase()}] "
        }
        
        // For simulation, we'll reverse the text and add language prefix
        // In production, this would call actual translation API
        val simulatedTranslation = if (text.length > 20) {
            "${translatedPrefix}${text.take(20)}..."
        } else {
            "${translatedPrefix}${text.reversed()}"
        }
        
        return simulatedTranslation
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
    
    // Preference update methods
    
    /**
     * Update message debounce duration
     */
    fun updateDebounceDuration(duration: Long) {
        viewModelScope.launch {
            try {
                preferencesRepository.saveDebounceDuration(duration)
                Log.d(TAG, "Debounce duration updated to: ${duration}ms")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update debounce duration", e)
                _errorMessage.value = "Failed to save preference: ${e.message}"
            }
        }
    }
    
    /**
     * Update statistics auto-show preference
     */
    fun updateStatisticsAutoShow(autoShow: Boolean) {
        viewModelScope.launch {
            try {
                preferencesRepository.saveStatisticsAutoShow(autoShow)
                Log.d(TAG, "Statistics auto-show updated to: $autoShow")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update statistics auto-show", e)
                _errorMessage.value = "Failed to save preference: ${e.message}"
            }
        }
    }
    
    /**
     * Update language animation preference
     */
    fun updateLanguageAnimationEnabled(enabled: Boolean) {
        viewModelScope.launch {
            try {
                preferencesRepository.saveLanguageAnimationEnabled(enabled)
                Log.d(TAG, "Language animation enabled updated to: $enabled")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update language animation preference", e)
                _errorMessage.value = "Failed to save preference: ${e.message}"
            }
        }
    }
    
    /**
     * Reset all preferences to defaults
     */
    fun resetPreferences() {
        viewModelScope.launch {
            try {
                preferencesRepository.clearAllPreferences()
                _successMessage.value = "All preferences reset to defaults"
                Log.d(TAG, "All preferences reset to defaults")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to reset preferences", e)
                _errorMessage.value = "Failed to reset preferences: ${e.message}"
            }
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        Log.d(TAG, "LocalizationViewModel cleared")
    }
}

/**
 * ViewModelProvider Factory for LocalizationViewModel
 */
class LocalizationViewModelFactory(
    private val context: Context,
    private val preferencesRepository: PreferencesRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(LocalizationViewModel::class.java)) {
            return LocalizationViewModel(context, preferencesRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}