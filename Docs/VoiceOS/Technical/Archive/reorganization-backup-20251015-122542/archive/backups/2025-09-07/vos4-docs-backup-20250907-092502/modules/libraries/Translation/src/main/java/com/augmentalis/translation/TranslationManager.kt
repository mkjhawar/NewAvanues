// Author: Manoj Jhawar
// Purpose: Translation services management - extracted from AccessibilityManager for better separation of concerns

package com.augmentalis.translation

import android.content.Context
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.util.*

/**
 * TranslationManager - Dedicated translation services management
 * 
 * Provides translation capabilities including:
 * - Offline translation using downloaded language packs
 * - Online translation via cloud services (Google Translate API, etc.)
 * - Hybrid mode (offline first, fallback to online)
 * - Language detection
 * - Translation caching for performance
 * 
 * COT Reflection:
 * - Single Responsibility: Only handles translation, nothing else
 * - Separate module: Translation is not a device capability, so separate from DeviceManager
 * - Clear separation: Not mixed with accessibility features
 * - Extensible: Can easily add new translation providers
 */
class TranslationManager(private val context: Context) {
    
    companion object {
        private const val TAG = "TranslationManager"
        
        // Translation modes
        const val MODE_OFFLINE = "offline"
        const val MODE_ONLINE = "online"
        const val MODE_HYBRID = "hybrid"
        
        // Cache settings
        private const val CACHE_SIZE = 1000
        private const val CACHE_EXPIRY_MS = 24 * 60 * 60 * 1000L // 24 hours
        
        // Text limits
        const val MAX_TEXT_LENGTH = 5000
        const val MAX_BATCH_SIZE = 100
        
        // Common language codes
        const val LANG_AUTO = "auto"
        const val LANG_ENGLISH = "en"
        const val LANG_SPANISH = "es"
        const val LANG_FRENCH = "fr"
        const val LANG_GERMAN = "de"
        const val LANG_ITALIAN = "it"
        const val LANG_PORTUGUESE = "pt"
        const val LANG_RUSSIAN = "ru"
        const val LANG_CHINESE_SIMPLIFIED = "zh-CN"
        const val LANG_CHINESE_TRADITIONAL = "zh-TW"
        const val LANG_JAPANESE = "ja"
        const val LANG_KOREAN = "ko"
        const val LANG_ARABIC = "ar"
        const val LANG_HINDI = "hi"
    }
    
    // State management
    private val _translationState = MutableStateFlow(TranslationState())
    val translationState: StateFlow<TranslationState> = _translationState.asStateFlow()
    
    private val _translationResult = MutableSharedFlow<TranslationResult>()
    val translationResult: SharedFlow<TranslationResult> = _translationResult.asSharedFlow()
    
    // Translation cache
    private val translationCache = mutableMapOf<String, CachedTranslation>()
    
    // Coroutine scope
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    // Translation providers (would be actual implementations in production)
    private var offlineTranslator: OfflineTranslator? = null
    private var onlineTranslator: OnlineTranslator? = null
    
    // ========== DATA MODELS ==========
    
    data class TranslationState(
        val isAvailable: Boolean = false,
        val currentMode: String = MODE_HYBRID,
        val isTranslating: Boolean = false,
        val supportedLanguages: List<TranslationLanguage> = emptyList(),
        val downloadedLanguages: Set<String> = emptySet(),
        val capabilities: TranslationCapabilities? = null,
        val activeTranslations: Int = 0,
        val cacheSize: Int = 0
    )
    
    data class TranslationLanguage(
        val code: String,
        val name: String,
        val nativeName: String,
        val locale: Locale,
        val isDownloaded: Boolean = false,
        val downloadSize: Long = 0L,
        val supportsOffline: Boolean = false,
        val supportsVoice: Boolean = false,
        val supportsHandwriting: Boolean = false
    )
    
    data class TranslationCapabilities(
        val supportsOfflineTranslation: Boolean,
        val supportsOnlineTranslation: Boolean,
        val supportsRealTimeTranslation: Boolean,
        val supportsVoiceTranslation: Boolean,
        val supportsImageTranslation: Boolean,
        val supportsDocumentTranslation: Boolean,
        val supportsBatchTranslation: Boolean,
        val supportsLanguageDetection: Boolean,
        val maxTextLength: Int,
        val maxBatchSize: Int,
        val supportedFormats: List<String>
    )
    
    data class TranslationRequest(
        val text: String,
        val sourceLanguage: String = LANG_AUTO,
        val targetLanguage: String,
        val mode: String = MODE_HYBRID,
        val options: TranslationOptions = TranslationOptions(),
        val priority: TranslationPriority = TranslationPriority.NORMAL
    )
    
    data class TranslationOptions(
        val preserveFormatting: Boolean = true,
        val detectSourceLanguage: Boolean = true,
        val includeAlternatives: Boolean = false,
        val maxAlternatives: Int = 3,
        val useCache: Boolean = true,
        val contextHint: String? = null,
        val domain: TranslationDomain = TranslationDomain.GENERAL
    )
    
    enum class TranslationDomain {
        GENERAL,
        TECHNICAL,
        MEDICAL,
        LEGAL,
        CONVERSATIONAL,
        FORMAL,
        INFORMAL
    }
    
    enum class TranslationPriority {
        LOW,
        NORMAL,
        HIGH,
        URGENT
    }
    
    data class TranslationResult(
        val originalText: String,
        val translatedText: String,
        val sourceLanguage: String,
        val targetLanguage: String,
        val detectedLanguage: String? = null,
        val confidence: Float,
        val alternatives: List<AlternativeTranslation> = emptyList(),
        val translationTime: Long,
        val fromCache: Boolean = false,
        val provider: String,
        val timestamp: Long = System.currentTimeMillis()
    )
    
    data class AlternativeTranslation(
        val text: String,
        val confidence: Float,
        val explanation: String? = null
    )
    
    data class BatchTranslationRequest(
        val texts: List<String>,
        val sourceLanguage: String = LANG_AUTO,
        val targetLanguage: String,
        val mode: String = MODE_HYBRID,
        val options: TranslationOptions = TranslationOptions()
    )
    
    data class BatchTranslationResult(
        val results: List<TranslationResult>,
        val totalTime: Long,
        val successCount: Int,
        val failureCount: Int
    )
    
    private data class CachedTranslation(
        val result: TranslationResult,
        val timestamp: Long
    )
    
    // ========== INITIALIZATION ==========
    
    init {
        initialize()
    }
    
    private fun initialize() {
        initializeTranslationProviders()
        loadSupportedLanguages()
        loadDownloadedLanguages()
        updateCapabilities()
    }
    
    private fun initializeTranslationProviders() {
        // Initialize offline translator (would use ML Kit or similar in production)
        offlineTranslator = OfflineTranslator(context)
        
        // Initialize online translator (would use Google Translate API or similar)
        onlineTranslator = OnlineTranslator(context)
        
        _translationState.update { it.copy(isAvailable = true) }
        Log.d(TAG, "Translation providers initialized")
    }
    
    private fun loadSupportedLanguages() {
        // In production, this would query available languages from providers
        val languages = listOf(
            TranslationLanguage(
                code = LANG_ENGLISH,
                name = "English",
                nativeName = "English",
                locale = Locale.ENGLISH,
                isDownloaded = true,
                supportsOffline = true,
                supportsVoice = true
            ),
            TranslationLanguage(
                code = LANG_SPANISH,
                name = "Spanish",
                nativeName = "Español",
                locale = Locale("es"),
                isDownloaded = false,
                downloadSize = 25 * 1024 * 1024, // 25MB
                supportsOffline = true,
                supportsVoice = true
            ),
            TranslationLanguage(
                code = LANG_FRENCH,
                name = "French",
                nativeName = "Français",
                locale = Locale.FRENCH,
                isDownloaded = false,
                downloadSize = 23 * 1024 * 1024,
                supportsOffline = true,
                supportsVoice = true
            ),
            TranslationLanguage(
                code = LANG_GERMAN,
                name = "German",
                nativeName = "Deutsch",
                locale = Locale.GERMAN,
                isDownloaded = false,
                downloadSize = 24 * 1024 * 1024,
                supportsOffline = true,
                supportsVoice = true
            ),
            TranslationLanguage(
                code = LANG_CHINESE_SIMPLIFIED,
                name = "Chinese (Simplified)",
                nativeName = "简体中文",
                locale = Locale.SIMPLIFIED_CHINESE,
                isDownloaded = false,
                downloadSize = 40 * 1024 * 1024,
                supportsOffline = true,
                supportsVoice = true,
                supportsHandwriting = true
            ),
            TranslationLanguage(
                code = LANG_JAPANESE,
                name = "Japanese",
                nativeName = "日本語",
                locale = Locale.JAPANESE,
                isDownloaded = false,
                downloadSize = 35 * 1024 * 1024,
                supportsOffline = true,
                supportsVoice = true,
                supportsHandwriting = true
            )
        )
        
        _translationState.update { it.copy(supportedLanguages = languages) }
    }
    
    private fun loadDownloadedLanguages() {
        // In production, check which language packs are downloaded
        val downloaded = setOf(LANG_ENGLISH) // English always available
        _translationState.update { it.copy(downloadedLanguages = downloaded) }
    }
    
    private fun updateCapabilities() {
        val capabilities = TranslationCapabilities(
            supportsOfflineTranslation = true,
            supportsOnlineTranslation = true,
            supportsRealTimeTranslation = true,
            supportsVoiceTranslation = true,
            supportsImageTranslation = false, // Could add with ML Kit
            supportsDocumentTranslation = true,
            supportsBatchTranslation = true,
            supportsLanguageDetection = true,
            maxTextLength = MAX_TEXT_LENGTH,
            maxBatchSize = MAX_BATCH_SIZE,
            supportedFormats = listOf("text", "html", "markdown")
        )
        
        _translationState.update { it.copy(capabilities = capabilities) }
    }
    
    // ========== PUBLIC API ==========
    
    /**
     * Translate text
     */
    suspend fun translate(request: TranslationRequest): TranslationResult {
        // Validate request
        if (request.text.isEmpty()) {
            return createErrorResult(request, "Empty text")
        }
        
        if (request.text.length > MAX_TEXT_LENGTH) {
            return createErrorResult(request, "Text too long")
        }
        
        // Check cache first if enabled
        if (request.options.useCache) {
            getCachedTranslation(request)?.let { return it }
        }
        
        _translationState.update { 
            it.copy(isTranslating = true, activeTranslations = it.activeTranslations + 1)
        }
        
        val startTime = System.currentTimeMillis()
        
        try {
            val result = when (request.mode) {
                MODE_OFFLINE -> translateOffline(request)
                MODE_ONLINE -> translateOnline(request)
                MODE_HYBRID -> translateHybrid(request)
                else -> createErrorResult(request, "Invalid mode")
            }
            
            // Cache successful translation
            if (request.options.useCache && result.confidence > 0.5f) {
                cacheTranslation(request, result)
            }
            
            // Emit result
            _translationResult.emit(result)
            
            return result
        } finally {
            _translationState.update { 
                it.copy(
                    isTranslating = it.activeTranslations == 1,
                    activeTranslations = (it.activeTranslations - 1).coerceAtLeast(0)
                )
            }
        }
    }
    
    /**
     * Translate batch of texts
     */
    suspend fun translateBatch(request: BatchTranslationRequest): BatchTranslationResult {
        if (request.texts.size > MAX_BATCH_SIZE) {
            throw IllegalArgumentException("Batch size exceeds maximum of $MAX_BATCH_SIZE")
        }
        
        val startTime = System.currentTimeMillis()
        val results = mutableListOf<TranslationResult>()
        var successCount = 0
        var failureCount = 0
        
        coroutineScope {
            request.texts.map { text ->
                async {
                    try {
                        val result = translate(
                            TranslationRequest(
                                text = text,
                                sourceLanguage = request.sourceLanguage,
                                targetLanguage = request.targetLanguage,
                                mode = request.mode,
                                options = request.options
                            )
                        )
                        if (result.confidence > 0) successCount++ else failureCount++
                        result
                    } catch (e: Exception) {
                        failureCount++
                        createErrorResult(
                            TranslationRequest(text, request.sourceLanguage, request.targetLanguage),
                            e.message ?: "Translation failed"
                        )
                    }
                }
            }.awaitAll().forEach { results.add(it) }
        }
        
        return BatchTranslationResult(
            results = results,
            totalTime = System.currentTimeMillis() - startTime,
            successCount = successCount,
            failureCount = failureCount
        )
    }
    
    /**
     * Detect language of text
     */
    suspend fun detectLanguage(text: String): String {
        // In production, use ML Kit or similar for language detection
        return when {
            text.contains("hola", ignoreCase = true) -> LANG_SPANISH
            text.contains("bonjour", ignoreCase = true) -> LANG_FRENCH
            text.contains("guten tag", ignoreCase = true) -> LANG_GERMAN
            text.matches(Regex(".*[\\u4e00-\\u9fff]+.*")) -> LANG_CHINESE_SIMPLIFIED
            text.matches(Regex(".*[\\u3040-\\u309f\\u30a0-\\u30ff]+.*")) -> LANG_JAPANESE
            else -> LANG_ENGLISH
        }
    }
    
    /**
     * Download language pack for offline translation
     */
    suspend fun downloadLanguage(languageCode: String): Boolean {
        val language = _translationState.value.supportedLanguages.find { it.code == languageCode }
        if (language == null || !language.supportsOffline) {
            return false
        }
        
        // In production, download actual language model
        delay(2000) // Simulate download
        
        _translationState.update { state ->
            state.copy(
                downloadedLanguages = state.downloadedLanguages + languageCode,
                supportedLanguages = state.supportedLanguages.map {
                    if (it.code == languageCode) it.copy(isDownloaded = true) else it
                }
            )
        }
        
        Log.d(TAG, "Downloaded language pack for: $languageCode")
        return true
    }
    
    /**
     * Delete language pack
     */
    fun deleteLanguage(languageCode: String): Boolean {
        if (languageCode == LANG_ENGLISH) {
            Log.w(TAG, "Cannot delete base language (English)")
            return false
        }
        
        _translationState.update { state ->
            state.copy(
                downloadedLanguages = state.downloadedLanguages - languageCode,
                supportedLanguages = state.supportedLanguages.map {
                    if (it.code == languageCode) it.copy(isDownloaded = false) else it
                }
            )
        }
        
        Log.d(TAG, "Deleted language pack for: $languageCode")
        return true
    }
    
    /**
     * Set translation mode
     */
    fun setMode(mode: String) {
        if (mode in listOf(MODE_OFFLINE, MODE_ONLINE, MODE_HYBRID)) {
            _translationState.update { it.copy(currentMode = mode) }
        }
    }
    
    /**
     * Clear translation cache
     */
    fun clearCache() {
        translationCache.clear()
        _translationState.update { it.copy(cacheSize = 0) }
        Log.d(TAG, "Translation cache cleared")
    }
    
    /**
     * Get supported languages
     */
    fun getSupportedLanguages(): List<TranslationLanguage> {
        return _translationState.value.supportedLanguages
    }
    
    /**
     * Check if language is downloaded
     */
    fun isLanguageDownloaded(languageCode: String): Boolean {
        return _translationState.value.downloadedLanguages.contains(languageCode)
    }
    
    // ========== PRIVATE METHODS ==========
    
    private suspend fun translateOffline(request: TranslationRequest): TranslationResult {
        // Check if languages are downloaded
        if (!isLanguageDownloaded(request.sourceLanguage) && request.sourceLanguage != LANG_AUTO) {
            return createErrorResult(request, "Source language not downloaded")
        }
        
        if (!isLanguageDownloaded(request.targetLanguage)) {
            return createErrorResult(request, "Target language not downloaded")
        }
        
        // Use offline translator
        return offlineTranslator?.translate(request) ?: createErrorResult(request, "Offline translator not available")
    }
    
    private suspend fun translateOnline(request: TranslationRequest): TranslationResult {
        // Use online translator
        return onlineTranslator?.translate(request) ?: createErrorResult(request, "Online translator not available")
    }
    
    private suspend fun translateHybrid(request: TranslationRequest): TranslationResult {
        // Try offline first if available
        if (isLanguageDownloaded(request.targetLanguage)) {
            try {
                val offlineResult = translateOffline(request)
                if (offlineResult.confidence > 0.7f) {
                    return offlineResult
                }
            } catch (e: Exception) {
                Log.w(TAG, "Offline translation failed, falling back to online", e)
            }
        }
        
        // Fallback to online
        return translateOnline(request)
    }
    
    private fun getCachedTranslation(request: TranslationRequest): TranslationResult? {
        val cacheKey = createCacheKey(request)
        val cached = translationCache[cacheKey]
        
        return if (cached != null && !isCacheExpired(cached)) {
            cached.result.copy(fromCache = true)
        } else {
            null
        }
    }
    
    private fun cacheTranslation(request: TranslationRequest, result: TranslationResult) {
        val cacheKey = createCacheKey(request)
        translationCache[cacheKey] = CachedTranslation(result, System.currentTimeMillis())
        
        // Limit cache size
        if (translationCache.size > CACHE_SIZE) {
            // Remove oldest entries
            val entriesToRemove = translationCache.entries
                .sortedBy { it.value.timestamp }
                .take(translationCache.size - CACHE_SIZE)
            
            entriesToRemove.forEach { translationCache.remove(it.key) }
        }
        
        _translationState.update { it.copy(cacheSize = translationCache.size) }
    }
    
    private fun createCacheKey(request: TranslationRequest): String {
        return "${request.sourceLanguage}_${request.targetLanguage}_${request.text.hashCode()}"
    }
    
    private fun isCacheExpired(cached: CachedTranslation): Boolean {
        return System.currentTimeMillis() - cached.timestamp > CACHE_EXPIRY_MS
    }
    
    private fun createErrorResult(request: TranslationRequest, error: String): TranslationResult {
        return TranslationResult(
            originalText = request.text,
            translatedText = "",
            sourceLanguage = request.sourceLanguage,
            targetLanguage = request.targetLanguage,
            confidence = 0f,
            translationTime = 0,
            provider = "error",
            alternatives = emptyList()
        )
    }
    
    // ========== MOCK TRANSLATORS ==========
    
    private inner class OfflineTranslator(private val context: Context) {
        suspend fun translate(request: TranslationRequest): TranslationResult {
            delay(100) // Simulate processing
            
            // Mock offline translation
            val translatedText = when {
                request.targetLanguage == LANG_SPANISH -> "Texto traducido (offline)"
                request.targetLanguage == LANG_FRENCH -> "Texte traduit (offline)"
                else -> "Translated text (offline)"
            }
            
            return TranslationResult(
                originalText = request.text,
                translatedText = translatedText,
                sourceLanguage = request.sourceLanguage,
                targetLanguage = request.targetLanguage,
                confidence = 0.85f,
                translationTime = 100,
                provider = "offline",
                alternatives = emptyList()
            )
        }
    }
    
    private inner class OnlineTranslator(private val context: Context) {
        suspend fun translate(request: TranslationRequest): TranslationResult {
            delay(500) // Simulate network delay
            
            // Mock online translation
            val translatedText = when {
                request.targetLanguage == LANG_SPANISH -> "Texto traducido (online)"
                request.targetLanguage == LANG_FRENCH -> "Texte traduit (online)"
                else -> "Translated text (online)"
            }
            
            return TranslationResult(
                originalText = request.text,
                translatedText = translatedText,
                sourceLanguage = request.sourceLanguage,
                targetLanguage = request.targetLanguage,
                confidence = 0.95f,
                translationTime = 500,
                provider = "online",
                alternatives = if (request.options.includeAlternatives) {
                    listOf(
                        AlternativeTranslation("Alternative 1", 0.9f),
                        AlternativeTranslation("Alternative 2", 0.85f)
                    )
                } else emptyList()
            )
        }
    }
    
    // ========== CLEANUP ==========
    
    fun release() {
        clearCache()
        scope.cancel()
        offlineTranslator = null
        onlineTranslator = null
    }
}