/**
 * GGUF Inference Strategy for llama.cpp Runtime
 *
 * Single Responsibility: Execute inference on GGUF-format models using llama.cpp
 *
 * Supports:
 * - Q4_K_M, Q8_0, and other GGUF quantization formats
 * - Streaming token generation
 * - GPU acceleration via Vulkan (when available)
 * - CPU fallback with optimized SIMD
 *
 * Native Library: libllama.so (bundled separately)
 *
 * Created: 2025-12-03
 * Author: Manoj Jhawar
 */

package com.augmentalis.ava.features.llm.alc.inference

import android.content.Context
import com.augmentalis.ava.features.llm.alc.interfaces.IInferenceStrategy
import com.augmentalis.ava.features.llm.alc.models.InferenceException
import com.augmentalis.ava.features.llm.alc.models.InferenceRequest
import com.augmentalis.ava.features.llm.alc.models.InferenceResult
import com.augmentalis.ava.features.llm.alc.models.GenerationParams
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import org.json.JSONObject

/**
 * GGUF inference strategy using llama.cpp
 *
 * Uses JNI bindings to llama.cpp for efficient inference on GGUF models.
 * Automatically detects and uses GPU acceleration when available.
 *
 * @param context Android context for accessing native libraries
 * @param modelPath Path to the GGUF model file
 * @param contextLength Maximum context length (default 4096)
 * @param gpuLayers Number of layers to offload to GPU (-1 for auto, 0 for CPU only)
 */
class GGUFInferenceStrategy(
    private val context: Context,
    private val modelPath: String,
    private val contextLength: Int = 4096,
    private val gpuLayers: Int = -1  // -1 = auto-detect
) : IInferenceStrategy {

    companion object {
        private const val TAG = "GGUFInference"

        // Native library name (without lib prefix and .so suffix)
        private const val NATIVE_LIB_NAME = "llama-android"

        // Model pointer (0 = not loaded)
        @Volatile
        private var modelPtr: Long = 0

        // Context pointer (0 = not loaded)
        @Volatile
        private var contextPtr: Long = 0

        // Flag indicating if native library is loaded
        @Volatile
        private var nativeLoaded: Boolean = false

        // Load result message
        @Volatile
        private var loadError: String? = null

        /**
         * Create GGUFInferenceStrategy from a model directory containing .amc config
         *
         * Looks for AVA Model Config (.amc) file and extracts:
         * - GGUF model file path
         * - Context window size
         * - Generation parameters
         *
         * @param context Android context
         * @param modelDir Directory containing .amc and .gguf files
         * @return GGUFInferenceStrategy configured from .amc, or null if not found
         */
        fun fromModelDirectory(context: Context, modelDir: File): GGUFInferenceStrategy? {
            // Find .amc config file (AVA Model Config)
            val amcFile = modelDir.listFiles()?.find { it.extension == "amc" }

            if (amcFile == null) {
                Timber.d("$TAG: No .amc config found in ${modelDir.name}, looking for .gguf directly")
                // Fallback: look for .gguf file directly
                val ggufFile = modelDir.listFiles()?.find { it.extension == "gguf" }
                if (ggufFile != null) {
                    Timber.i("$TAG: Found GGUF file directly: ${ggufFile.name}")
                    return GGUFInferenceStrategy(context, ggufFile.absolutePath)
                }
                return null
            }

            return try {
                val config = parseAmcConfig(amcFile)
                val ggufPath = File(modelDir, config.ggufFile).absolutePath

                Timber.i("$TAG: Creating strategy from .amc config: ${amcFile.name}")
                Timber.i("$TAG: GGUF file: ${config.ggufFile}, context: ${config.contextWindowSize}")

                GGUFInferenceStrategy(
                    context = context,
                    modelPath = ggufPath,
                    contextLength = config.contextWindowSize,
                    gpuLayers = -1  // Auto-detect
                )
            } catch (e: Exception) {
                Timber.e(e, "$TAG: Failed to parse .amc config: ${amcFile.name}")
                null
            }
        }

        /**
         * Parse AVA Model Config (.amc) file
         */
        private fun parseAmcConfig(amcFile: File): AmcConfig {
            val json = JSONObject(amcFile.readText())

            return AmcConfig(
                version = json.optString("version", "0.1.0"),
                modelType = json.optString("model_type", "unknown"),
                quantization = json.optString("quantization", "unknown"),
                format = json.optString("format", "gguf"),
                vocabSize = json.optInt("vocab_size", 32000),
                contextWindowSize = json.optInt("context_window_size", 4096),
                temperature = json.optDouble("temperature", 0.7).toFloat(),
                topP = json.optDouble("top_p", 0.95).toFloat(),
                repetitionPenalty = json.optDouble("repetition_penalty", 1.0).toFloat(),
                ggufFile = json.optString("gguf_file", ""),
                promptTemplate = json.optString("prompt_template", ""),
                stopTokenIds = json.optJSONObject("conv_template")
                    ?.optJSONArray("stop_token_ids")
                    ?.let { arr -> (0 until arr.length()).map { arr.getInt(it) } }
                    ?: emptyList()
            )
        }
    }

    /**
     * AVA Model Config (.amc) data class
     */
    data class AmcConfig(
        val version: String,
        val modelType: String,
        val quantization: String,
        val format: String,
        val vocabSize: Int,
        val contextWindowSize: Int,
        val temperature: Float,
        val topP: Float,
        val repetitionPenalty: Float,
        val ggufFile: String,
        val promptTemplate: String,
        val stopTokenIds: List<Int>
    )

    init {
        loadNativeLibrary()
    }

    /**
     * Load the llama.cpp native library
     */
    private fun loadNativeLibrary() {
        if (nativeLoaded) return

        try {
            System.loadLibrary(NATIVE_LIB_NAME)
            nativeLoaded = true
            loadError = null
            Timber.i("$TAG: Successfully loaded native library: $NATIVE_LIB_NAME")
        } catch (e: UnsatisfiedLinkError) {
            loadError = "Failed to load llama.cpp native library: ${e.message}"
            Timber.e(e, "$TAG: $loadError")
            // Not throwing here - will report unavailable in isAvailable()
        }
    }

    /**
     * Load the GGUF model into memory
     *
     * @return true if model loaded successfully
     */
    suspend fun loadModel(): Boolean = withContext(Dispatchers.IO) {
        if (!nativeLoaded) {
            Timber.e("$TAG: Cannot load model - native library not loaded")
            return@withContext false
        }

        if (modelPtr != 0L) {
            Timber.d("$TAG: Model already loaded")
            return@withContext true
        }

        val modelFile = File(modelPath)
        if (!modelFile.exists()) {
            Timber.e("$TAG: Model file not found: $modelPath")
            return@withContext false
        }

        val fileSizeMB = modelFile.length() / 1_000_000
        Timber.i("$TAG: Loading model: ${modelFile.name} (${fileSizeMB}MB)")
        Timber.i("$TAG: Context length: $contextLength, GPU layers: $gpuLayers")

        val startTime = System.currentTimeMillis()

        try {
            // Determine GPU layers: -1 means auto-detect based on model size
            val effectiveGpuLayers = if (gpuLayers == -1) {
                // Auto-detect: smaller models can fit entirely on GPU
                // Larger models (>4GB) need split loading
                when {
                    fileSizeMB < 2000 -> 35  // Small models: all layers on GPU
                    fileSizeMB < 5000 -> 20  // Medium models: partial GPU
                    else -> 10              // Large models: minimal GPU
                }
            } else {
                gpuLayers
            }

            modelPtr = nativeLoadModel(modelPath, contextLength, effectiveGpuLayers)

            if (modelPtr == 0L) {
                Timber.e("$TAG: Failed to load model (native returned null)")
                return@withContext false
            }

            // Create context
            contextPtr = nativeCreateContext(modelPtr, contextLength)

            if (contextPtr == 0L) {
                Timber.e("$TAG: Failed to create context")
                nativeFreeModel(modelPtr)
                modelPtr = 0
                return@withContext false
            }

            val loadTime = System.currentTimeMillis() - startTime
            Timber.i("$TAG: Model loaded successfully in ${loadTime}ms")
            Timber.i("$TAG: GPU layers: $effectiveGpuLayers")

            true
        } catch (e: Exception) {
            Timber.e(e, "$TAG: Exception loading model")
            modelPtr = 0
            contextPtr = 0
            false
        }
    }

    /**
     * Unload model and free resources
     */
    fun unloadModel() {
        if (contextPtr != 0L) {
            try {
                nativeFreeContext(contextPtr)
            } catch (e: Exception) {
                Timber.w(e, "$TAG: Error freeing context")
            }
            contextPtr = 0
        }

        if (modelPtr != 0L) {
            try {
                nativeFreeModel(modelPtr)
            } catch (e: Exception) {
                Timber.w(e, "$TAG: Error freeing model")
            }
            modelPtr = 0
        }

        Timber.i("$TAG: Model unloaded")
    }

    override suspend fun infer(request: InferenceRequest): InferenceResult {
        if (!isAvailable()) {
            throw InferenceException("GGUF inference not available: ${loadError ?: "Unknown error"}")
        }

        if (modelPtr == 0L) {
            val loaded = loadModel()
            if (!loaded) {
                throw InferenceException("Failed to load GGUF model: $modelPath")
            }
        }

        val startTime = System.currentTimeMillis()

        try {
            // Run inference
            val logits = nativeInfer(contextPtr, request.tokens.toIntArray())

            val inferenceTime = System.currentTimeMillis() - startTime
            val tokensPerSecond = if (inferenceTime > 0) {
                request.tokens.size.toFloat() / (inferenceTime / 1000f)
            } else 0f

            Timber.d("$TAG: Inference completed in ${inferenceTime}ms (${tokensPerSecond.toInt()} tokens/s)")

            return InferenceResult(
                logits = logits,
                cache = null,  // KV cache handled internally by llama.cpp
                tokensPerSecond = tokensPerSecond,
                metadata = mapOf(
                    "inference_time_ms" to inferenceTime,
                    "input_tokens" to request.tokens.size,
                    "provider" to "llama.cpp"
                )
            )
        } catch (e: Exception) {
            Timber.e(e, "$TAG: Inference failed")
            throw InferenceException("GGUF inference failed: ${e.message}", e)
        }
    }

    /**
     * Generate tokens as a streaming flow
     *
     * @param prompt Input prompt text
     * @param params Generation parameters
     * @return Flow of generated token strings
     */
    fun generateStreaming(prompt: String, params: GenerationParams): Flow<String> = flow {
        if (!isAvailable()) {
            throw InferenceException("GGUF inference not available")
        }

        if (modelPtr == 0L) {
            val loaded = loadModel()
            if (!loaded) {
                throw InferenceException("Failed to load model")
            }
        }

        val startTime = System.currentTimeMillis()
        var tokenCount = 0

        try {
            // Tokenize prompt
            val inputTokens = nativeTokenize(contextPtr, prompt)
            Timber.d("$TAG: Prompt tokenized to ${inputTokens.size} tokens")

            // Process prompt (prefill)
            val prefillResult = nativePrefill(contextPtr, inputTokens)
            if (!prefillResult) {
                throw InferenceException("Prefill failed")
            }

            // Generate tokens
            val stopSequences = params.stopSequences.toTypedArray()

            while (tokenCount < params.maxTokens) {
                // Sample next token
                val token = nativeSampleToken(
                    contextPtr,
                    params.temperature,
                    params.topP,
                    params.topK,
                    params.repeatPenalty
                )

                // Check for EOS
                if (nativeIsEOS(contextPtr, token)) {
                    Timber.d("$TAG: EOS token reached")
                    break
                }

                // Decode token to text
                val tokenText = nativeTokenToText(contextPtr, token)

                // Check for stop sequences
                if (stopSequences.any { tokenText.contains(it) }) {
                    Timber.d("$TAG: Stop sequence detected")
                    break
                }

                emit(tokenText)
                tokenCount++

                // Update context
                nativeAcceptToken(contextPtr, token)
            }

            val totalTime = System.currentTimeMillis() - startTime
            val tokensPerSecond = if (totalTime > 0) {
                tokenCount.toFloat() / (totalTime / 1000f)
            } else 0f

            Timber.i("$TAG: Generated $tokenCount tokens in ${totalTime}ms (${tokensPerSecond.toInt()} t/s)")

        } catch (e: Exception) {
            Timber.e(e, "$TAG: Streaming generation failed")
            throw InferenceException("Streaming generation failed: ${e.message}", e)
        }
    }.flowOn(Dispatchers.Default)

    override fun isAvailable(): Boolean {
        // Check native library loaded
        if (!nativeLoaded) {
            Timber.d("$TAG: Not available - native library not loaded")
            return false
        }

        // Check model file exists
        val modelFile = File(modelPath)
        if (!modelFile.exists()) {
            Timber.d("$TAG: Not available - model file not found: $modelPath")
            return false
        }

        return true
    }

    override fun getName(): String = "llama.cpp"

    override fun getPriority(): Int = 100  // Lower priority than MLC-LLM (50)

    /**
     * Get model info
     */
    fun getModelInfo(): Map<String, Any> {
        return mapOf(
            "path" to modelPath,
            "loaded" to (modelPtr != 0L),
            "context_length" to contextLength,
            "gpu_layers" to gpuLayers,
            "native_loaded" to nativeLoaded
        )
    }

    // =========================================================================
    // Native Methods (JNI bindings to llama.cpp)
    // =========================================================================

    /**
     * Load a GGUF model
     *
     * @param modelPath Path to .gguf file
     * @param contextLength Maximum context length
     * @param gpuLayers Number of layers on GPU
     * @return Model pointer (0 on failure)
     */
    private external fun nativeLoadModel(modelPath: String, contextLength: Int, gpuLayers: Int): Long

    /**
     * Create inference context
     *
     * @param modelPtr Model pointer from nativeLoadModel
     * @param contextLength Context length
     * @return Context pointer (0 on failure)
     */
    private external fun nativeCreateContext(modelPtr: Long, contextLength: Int): Long

    /**
     * Free model resources
     */
    private external fun nativeFreeModel(modelPtr: Long)

    /**
     * Free context resources
     */
    private external fun nativeFreeContext(contextPtr: Long)

    /**
     * Run inference on token sequence
     *
     * @param contextPtr Context pointer
     * @param tokens Input token IDs
     * @return Logits array for next token prediction
     */
    private external fun nativeInfer(contextPtr: Long, tokens: IntArray): FloatArray

    /**
     * Tokenize text string
     *
     * @param contextPtr Context pointer
     * @param text Input text
     * @return Token IDs
     */
    private external fun nativeTokenize(contextPtr: Long, text: String): IntArray

    /**
     * Process prompt tokens (prefill phase)
     *
     * @param contextPtr Context pointer
     * @param tokens Prompt tokens
     * @return true on success
     */
    private external fun nativePrefill(contextPtr: Long, tokens: IntArray): Boolean

    /**
     * Sample next token with given parameters
     *
     * @param contextPtr Context pointer
     * @param temperature Sampling temperature
     * @param topP Top-p (nucleus) sampling
     * @param topK Top-k sampling
     * @param repeatPenalty Repetition penalty
     * @return Sampled token ID
     */
    private external fun nativeSampleToken(
        contextPtr: Long,
        temperature: Float,
        topP: Float,
        topK: Int,
        repeatPenalty: Float
    ): Int

    /**
     * Check if token is end-of-sequence
     */
    private external fun nativeIsEOS(contextPtr: Long, token: Int): Boolean

    /**
     * Convert token ID to text
     */
    private external fun nativeTokenToText(contextPtr: Long, token: Int): String

    /**
     * Accept token into context (for continued generation)
     */
    private external fun nativeAcceptToken(contextPtr: Long, token: Int)
}
