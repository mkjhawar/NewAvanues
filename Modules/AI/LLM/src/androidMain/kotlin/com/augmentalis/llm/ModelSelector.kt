/**
 * Model Selector for AVA AI
 *
 * Automatically selects the best LLM model based on:
 * - User input language
 * - Available models
 * - Model capabilities
 * - User preferences
 *
 * Enables intelligent model switching for optimal multilingual support.
 *
 * Created: 2025-11-07
 * Author: AVA AI Team
 */

package com.augmentalis.llm

import android.content.Context
import com.augmentalis.llm.alc.loader.HuggingFaceModelDownloader
import com.augmentalis.llm.alc.loader.ModelDiscovery
import timber.log.Timber

/**
 * Model selector with language-aware selection
 */
class ModelSelector(
    private val context: Context
) {

    private val downloader = HuggingFaceModelDownloader(context)
    private val languageDetector = LanguageDetector

    /**
     * Available models with their capabilities
     * Model IDs use AVA naming convention: AVA-{TYPE}-{SIZE}-{QUANT}
     * See: docs/AVA-MODEL-NAMING-REGISTRY.md
     */
    private val availableModels = listOf(
        // Gemma 3 4B - Universal multilingual model (NEW - for A/B testing)
        // FIX: Updated to AVA naming convention v2.0
        ModelInfo(
            id = "AVA-GE3-4B16",
            displayName = "Gemma 3 4B Instruct (Multilingual)",
            huggingFaceRepo = "mlc-ai/gemma-3-4b-it-q4bf16_1-MLC",
            localSourcePath = "",
            sizeGB = 2.3f,
            supportedLanguages = Language.values().toList(), // ALL 140+ languages
            strengths = listOf("140+ languages", "Better reasoning", "Vision-ready", "128K context"),
            weaknesses = listOf("Larger size", "Requires Android lib compilation")
        ),
        // Gemma 2B - Original English-focused model
        // FIX: Updated to AVA naming convention v2.0
        ModelInfo(
            id = "AVA-GE2-2B16",
            displayName = "Gemma 2B Instruct (English)",
            huggingFaceRepo = "mlc-ai/gemma-2b-it-q4f16_1-MLC",
            localSourcePath = "",
            sizeGB = 1.2f,
            supportedLanguages = listOf(
                Language.ENGLISH,
                Language.SPANISH,
                Language.FRENCH,
                Language.GERMAN,
                Language.ITALIAN,
                Language.PORTUGUESE
            ),
            strengths = listOf("English", "European languages", "Instruction following", "Smaller size"),
            weaknesses = listOf("Asian languages", "Arabic", "Limited to ~20-30 languages")
        ),
        ModelInfo(
            id = "AVA-QWN-1B-Q4",
            displayName = "Qwen 2.5 1.5B Instruct",
            huggingFaceRepo = "mlc-ai/Qwen2.5-1.5B-Instruct-q4f16_1-MLC",
            localSourcePath = null, // Not in local downloads yet
            sizeGB = 1.0f,
            supportedLanguages = Language.values().toList(), // All languages
            strengths = listOf("Multilingual", "Asian languages", "Efficient"),
            weaknesses = listOf("Smaller model", "Less nuanced English")
        ),
        ModelInfo(
            id = "AVA-LLM-3B-Q4",
            displayName = "Llama 3.2 3B Instruct",
            huggingFaceRepo = "mlc-ai/Llama-3.2-3B-Instruct-q4f16_1-MLC",
            localSourcePath = null, // Not in local downloads yet
            sizeGB = 1.9f,
            supportedLanguages = listOf(
                Language.ENGLISH,
                Language.SPANISH,
                Language.FRENCH,
                Language.GERMAN,
                Language.ITALIAN,
                Language.PORTUGUESE,
                Language.RUSSIAN
            ),
            strengths = listOf("Larger model", "Better reasoning", "European languages"),
            weaknesses = listOf("Larger size", "Limited Asian language support")
        ),
        ModelInfo(
            id = "AVA-PHI-3B-Q4",
            displayName = "Phi 3.5 Mini",
            huggingFaceRepo = "mlc-ai/Phi-3.5-mini-instruct-q4f16_1-MLC",
            localSourcePath = "",
            sizeGB = 2.4f,
            supportedLanguages = listOf(Language.ENGLISH),
            strengths = listOf("Strong English", "Reasoning", "Math"),
            weaknesses = listOf("English-only", "Largest size")
        ),
        ModelInfo(
            id = "AVA-MST-7B-Q4",
            displayName = "Mistral 7B Instruct",
            huggingFaceRepo = "mlc-ai/Mistral-7B-Instruct-v0.3-q4f16_1-MLC",
            localSourcePath = "",
            sizeGB = 4.5f,
            supportedLanguages = listOf(
                Language.ENGLISH,
                Language.SPANISH,
                Language.FRENCH,
                Language.GERMAN,
                Language.ITALIAN
            ),
            strengths = listOf("Best quality", "European languages", "Reasoning"),
            weaknesses = listOf("Very large", "Slow on mobile", "High memory")
        )
    )

    /**
     * Select best model for given text input
     *
     * @param text User input text
     * @param preferredModelId Optional preferred model (overrides auto-selection)
     * @return Selected model ID
     */
    fun selectBestModel(
        text: String,
        preferredModelId: String? = null
    ): String {
        // If user has preference, respect it
        if (preferredModelId != null && isModelAvailable(preferredModelId)) {
            Timber.d("Using preferred model: $preferredModelId")
            return preferredModelId
        }

        // Detect language
        val (language, confidence) = languageDetector.detectWithConfidence(text)
        Timber.d("Detected language: $language (confidence: $confidence)")

        // If low confidence, use first available model (dynamic discovery)
        // NO HARDCODED MODEL NAMES - discovers what's installed
        if (confidence < 0.3f) {
            Timber.w("Low confidence language detection, using first available model")
            val discovery = ModelDiscovery(context)
            val firstModel = kotlinx.coroutines.runBlocking {
                discovery.getFirstAvailableModel()
            }
            return firstModel?.id ?: run {
                Timber.e("No models installed - cannot select model")
                return "NO_MODEL_INSTALLED"  // Caller should handle this
            }
        }

        // Find best model for language
        val bestModel = findBestModelForLanguage(language)
        Timber.i("Selected model: ${bestModel.id} for language: $language")

        return bestModel.id
    }

    /**
     * Select best model for a specific language
     *
     * @param language Target language
     * @return Model ID
     */
    fun selectModelForLanguage(language: Language): String {
        val bestModel = findBestModelForLanguage(language)
        return bestModel.id
    }

    /**
     * Find best model for given language
     *
     * Prioritizes:
     * 1. Models that support the language
     * 2. Downloaded models (already available locally)
     * 3. Smaller size (faster download/loading)
     */
    private fun findBestModelForLanguage(language: Language): ModelInfo {
        // Filter models that support the language
        val supportedModels = availableModels.filter { model ->
            language in model.supportedLanguages
        }

        if (supportedModels.isEmpty()) {
            // Fallback: use Qwen (multilingual)
            return availableModels.first { it.id.contains("qwen") }
        }

        // Prioritize downloaded models
        val downloadedModels = supportedModels.filter { model ->
            downloader.isModelDownloaded(model.id)
        }

        if (downloadedModels.isNotEmpty()) {
            // Use smallest downloaded model
            return downloadedModels.minByOrNull { it.sizeGB } ?: supportedModels.first()
        }

        // Use smallest supported model (for faster download)
        return supportedModels.minByOrNull { it.sizeGB } ?: supportedModels.first()
    }

    /**
     * Check if model is available (downloaded)
     *
     * @param modelId Model identifier
     * @return true if downloaded, false otherwise
     */
    fun isModelAvailable(modelId: String): Boolean {
        return downloader.isModelDownloaded(modelId)
    }

    /**
     * Get all available models
     *
     * @return List of model information
     */
    fun getAvailableModels(): List<ModelInfo> {
        return availableModels.map { model ->
            model.copy(isDownloaded = downloader.isModelDownloaded(model.id))
        }
    }

    /**
     * Get model info by ID
     *
     * @param modelId Model identifier
     * @return Model info or null if not found
     */
    fun getModelInfo(modelId: String): ModelInfo? {
        val model = availableModels.find { it.id == modelId } ?: return null
        return model.copy(isDownloaded = downloader.isModelDownloaded(modelId))
    }

    /**
     * Get recommended models for a language
     *
     * Returns up to 3 models that best support the language, ranked by quality.
     *
     * @param language Target language
     * @return List of recommended models (ranked best to worst)
     */
    fun getRecommendedModelsForLanguage(language: Language): List<ModelInfo> {
        return availableModels
            .filter { model -> language in model.supportedLanguages }
            .sortedWith(compareBy(
                // Prioritize downloaded models
                { !downloader.isModelDownloaded(it.id) },
                // Then by size (smaller = better for mobile)
                { it.sizeGB }
            ))
            .take(3)
            .map { model ->
                model.copy(isDownloaded = downloader.isModelDownloaded(model.id))
            }
    }

    /**
     * Check if automatic model switching is beneficial
     *
     * Returns true if switching to a different model would provide better
     * language support for the given text.
     *
     * @param currentModelId Currently loaded model
     * @param text User input text
     * @return true if switching is recommended, false otherwise
     */
    fun shouldSwitchModel(currentModelId: String, text: String): Boolean {
        val currentModel = availableModels.find { it.id == currentModelId }
            ?: return false

        val (language, confidence) = languageDetector.detectWithConfidence(text)

        // Don't switch if low confidence
        if (confidence < 0.5f) {
            return false
        }

        // Don't switch if current model supports the language
        if (language in currentModel.supportedLanguages) {
            return false
        }

        // Switch if a better model exists
        val bestModel = findBestModelForLanguage(language)
        return bestModel.id != currentModelId
    }

    /**
     * Get model switch recommendation
     *
     * @param currentModelId Current model
     * @param text User input text
     * @return Recommended model ID if switching is beneficial, null otherwise
     */
    fun getModelSwitchRecommendation(currentModelId: String, text: String): String? {
        if (!shouldSwitchModel(currentModelId, text)) {
            return null
        }

        return selectBestModel(text)
    }
}

/**
 * Model information data class
 */
data class ModelInfo(
    /**
     * Model identifier (used internally) - AVA naming convention
     * Format: AVA-{TYPE}-{SIZE}-{QUANT} (e.g., AVA-GEM-2B-Q4)
     */
    val id: String,

    /**
     * Display name for UI
     */
    val displayName: String,

    /**
     * HuggingFace repository for download
     */
    val huggingFaceRepo: String,

    /**
     * Local source path for testing (copy from here instead of downloading)
     * Set to null if no local source available
     */
    val localSourcePath: String? = null,

    /**
     * Model size in GB
     */
    val sizeGB: Float,

    /**
     * Languages supported by this model
     */
    val supportedLanguages: List<Language>,

    /**
     * Model strengths (for user info)
     */
    val strengths: List<String>,

    /**
     * Model weaknesses (for user info)
     */
    val weaknesses: List<String>,

    /**
     * Whether model is downloaded (runtime info)
     */
    val isDownloaded: Boolean = false
) {
    /**
     * Check if model supports a given language
     */
    fun supportsLanguage(language: Language): Boolean {
        return language in supportedLanguages
    }

    /**
     * Get language support quality (1-3 stars)
     */
    fun getLanguageSupport(language: Language): LanguageSupport {
        return when {
            !supportsLanguage(language) -> LanguageSupport.NONE
            language == Language.ENGLISH && id.contains("phi") -> LanguageSupport.EXCELLENT
            language.isAsian() && id.contains("qwen") -> LanguageSupport.EXCELLENT
            language.isLatinScript() && id.contains("gemma") -> LanguageSupport.GOOD
            language in supportedLanguages -> LanguageSupport.FAIR
            else -> LanguageSupport.NONE
        }
    }
}

/**
 * Language support quality enum
 */
enum class LanguageSupport {
    NONE,       // Model doesn't support this language
    FAIR,       // Basic support, may have issues
    GOOD,       // Solid support
    EXCELLENT   // Optimized for this language
}
