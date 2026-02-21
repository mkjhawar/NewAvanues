/*
 * Copyright (c) 2024-2025 Intelligent Devices LLC / Augmentalis
 * All Rights Reserved.
 *
 * This file is part of AVA AI and is proprietary software.
 * See LICENSE file in the project root for license information.
 */

/**
 * TVM Runtime Wrapper for AVA AI
 *
 * Provides Kotlin-friendly interface to Apache TVM runtime for model inference.
 * Handles:
 * - Model loading from assets or external storage
 * - Tokenization/detokenization
 * - Device management (CPU, OpenCL, Vulkan, GPU)
 * - Memory management
 * - Hardware-aware backend selection
 *
 * Supported Backends:
 * - Vulkan: Modern GPU compute (Snapdragon 625+, all Adreno 5xx+)
 * - OpenCL: Legacy GPU compute (wide support)
 * - CPU: Fallback with ARM NEON SIMD
 *
 * @see ADR-008-Hardware-Aware-Inference-Backend.md
 *
 * Created: 2025-10-30
 * Author: AVA AI Team
 */

package com.augmentalis.llm.alc

import android.content.Context
import com.augmentalis.ava.core.common.ava3.AVA3Decoder
import com.augmentalis.llm.alc.streaming.ITokenizer
import com.augmentalis.llm.alc.tokenizer.HuggingFaceTokenizer
import com.augmentalis.llm.alc.tokenizer.SimpleVocabTokenizer
import com.augmentalis.llm.alc.tokenizer.TVMTokenizer
import timber.log.Timber
import org.apache.tvm.Device
import org.apache.tvm.Module
import org.apache.tvm.Function
import org.apache.tvm.TVMValue
import org.apache.tvm.TensorBase
import org.apache.tvm.Tensor
import org.apache.tvm.TVMType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive
import kotlin.coroutines.coroutineContext
import java.io.File

/**
 * TVM Runtime interface for model execution
 */
class TVMRuntime private constructor(
    private val context: Context,
    private val device: Device
) {
    companion object {
        init {
            try {
                // Load TVM v0.22.0 packed runtime with MLC-LLM
                // Required for all .amm (Ava Model MLC) files compiled with TVM v0.22.0
                // Single packed library includes: TVM runtime, FFI, JNI bridge, tokenizers, MLC-LLM
                System.loadLibrary("tvm4j_runtime_packed")  // TVM v0.22.0 packed runtime (104MB)
                Timber.i("TVM v0.22.0 packed runtime (MLC-LLM) loaded successfully")
            } catch (e: UnsatisfiedLinkError) {
                Timber.e(e, "Failed to load TVM native runtime - on-device inference unavailable")
            }
        }

        /**
         * Create TVM runtime instance
         *
         * Supports hardware-aware backend selection for optimal performance on all
         * Snapdragon devices (625+). Backend priority:
         * 1. Vulkan - Modern GPU compute (Adreno 5xx+, OpenGL ES 3.1+)
         * 2. OpenCL - Legacy GPU compute (all Adreno GPUs)
         * 3. CPU - Fallback with ARM NEON SIMD
         *
         * Snapdragon 625 (Adreno 506) Support:
         * - OpenCL 2.0 Full Profile ✓
         * - Vulkan 1.0 ✓
         *
         * @param context Android context
         * @param deviceType Device type: "vulkan", "opencl", "cpu", "auto" (default)
         *                   "auto" selects optimal backend based on device capabilities
         * @return TVMRuntime instance
         */
        fun create(
            context: Context,
            deviceType: String = "auto"
        ): TVMRuntime {
            val selectedType = if (deviceType == "auto") {
                // Use InferenceBackendSelector for automatic detection
                try {
                    val selector = Class.forName("com.augmentalis.ava.core.common.backend.InferenceBackendSelector")
                    val method = selector.getMethod("selectLLMBackend", Context::class.java)
                    val instance = selector.getDeclaredField("INSTANCE").get(null)
                    method.invoke(instance, context) as String
                } catch (e: Exception) {
                    Timber.w("InferenceBackendSelector not available, defaulting to OpenCL")
                    "opencl"
                }
            } else {
                deviceType.lowercase()
            }

            val device = when (selectedType) {
                "vulkan" -> {
                    Timber.i("Using Vulkan backend (modern GPU compute)")
                    Device.vulkan(0)
                }
                "opencl" -> {
                    Timber.i("Using OpenCL backend (legacy GPU compute)")
                    Device.opencl(0)
                }
                "cpu" -> {
                    Timber.i("Using CPU backend (ARM NEON SIMD)")
                    Device.cpu(0)
                }
                "gpu" -> {
                    // "gpu" alias - prefer Vulkan, fallback to OpenCL
                    Timber.i("GPU requested - selecting Vulkan")
                    Device.vulkan(0)
                }
                else -> {
                    Timber.w("Unknown device type '$selectedType', defaulting to OpenCL")
                    Device.opencl(0)
                }
            }

            Timber.d("TVM runtime created with device: $selectedType (${device.deviceType})")
            return TVMRuntime(context, device)
        }
    }

    // AVA3 decoder for protected model files
    private val ava3Decoder by lazy { AVA3Decoder() }

    /**
     * Load model module from path
     *
     * Loads compiled TVM model library (.so file) and extracts functions.
     * Supports AVA 3.0 encoded files (.ava3) for protected model distribution.
     *
     * @param modelPath Path to model file (.so, .tar, or .so.ava3)
     * @param modelLib Model library name
     * @param deviceOverride Device type override
     * @return Loaded TVM module
     */
    fun loadModule(
        modelPath: String,
        modelLib: String,
        deviceOverride: String = "opencl"
    ): TVMModule {
        return try {
            Timber.i("Loading TVM module: $modelPath")

            // Check if file is AVA3 encoded and decode if needed
            val actualPath = if (ava3Decoder.isAVA3Encoded(modelPath)) {
                Timber.i("Detected AVA 3.0 encoded file, decoding...")
                ava3Decoder.decodeToCache(context, modelPath)
            } else {
                modelPath
            }

            // Load compiled model library
            val modelFile = File(actualPath)
            if (!modelFile.exists()) {
                throw IllegalArgumentException("Model file not found: $actualPath")
            }

            // Determine library file to load
            val libFile = if (modelFile.isDirectory) {
                // For ALC-LLM models, check for library file in order of preference:
                // 1. *.ads (Ava Device Shared - ready to load, with TVM compat shim)
                // 2. *.so (legacy shared object - ready to load)
                // 3. *_devc.o (device code file - ready to load)
                // 4. *.adm (AVA Device MLC object code - NOT loadable directly)
                val adsFile = modelFile.listFiles()?.find { it.name.endsWith(".ads") }
                val soFile = modelFile.listFiles()?.find { it.name.endsWith(".so") }
                val devcFile = modelFile.listFiles()?.find { it.name.endsWith("_devc.o") }

                when {
                    adsFile != null -> {
                        Timber.d("Found .ads library (Ava Device Shared): ${adsFile.name}")
                        adsFile.absolutePath
                    }
                    soFile != null -> {
                        Timber.d("Found .so library: ${soFile.name}")
                        soFile.absolutePath
                    }
                    devcFile != null -> {
                        Timber.d("Found device code file: ${devcFile.name}")
                        devcFile.absolutePath
                    }
                    else -> {
                        Timber.w("No loadable library (.ads/.so) found in $actualPath. ADM files require linking first.")
                        throw IllegalStateException("No loadable library found. Run tvm-compat-shim/build-model.sh to create .ads from .adm files")
                    }
                }
            } else {
                actualPath
            }

            // Android linker namespace restriction: libraries from /sdcard can't be dlopen'd directly
            // Must copy to app's private directory first.
            val sourceFile = File(libFile)
            val finalLibFile = if (libFile.startsWith("/sdcard") ||
                libFile.startsWith("/storage/emulated")) {

                val cacheDir = File(context.cacheDir, "tvm_models")
                if (!cacheDir.exists()) cacheDir.mkdirs()

                // TVM only recognizes .so extension, so rename .ads -> .so when caching
                val targetName = if (sourceFile.extension.equals("ads", ignoreCase = true)) {
                    sourceFile.nameWithoutExtension + ".so"
                } else {
                    sourceFile.name
                }
                val cachedFile = File(cacheDir, targetName)

                // Copy only if not already cached or source is newer
                if (!cachedFile.exists() || sourceFile.lastModified() > cachedFile.lastModified()) {
                    Timber.i("Copying model library to app cache: ${sourceFile.name} -> ${cachedFile.name}")
                    sourceFile.copyTo(cachedFile, overwrite = true)
                } else {
                    Timber.d("Using cached library: ${cachedFile.name}")
                }

                cachedFile.absolutePath
            } else {
                libFile
            }

            Timber.i("Loading TVM module from: $finalLibFile")

            // TVM v0.22.0 API: Module.load() takes only the file path
            // The tvm4j_core.jar has been rebuilt to match TVM v0.22.0 FFI
            // (ffi.ModuleLoadFromFile now takes 1 argument instead of 2)
            val module = Module.load(finalLibFile)
            Timber.d("TVM module loaded successfully")

            // Get model functions (MLC-LLM standard interface)
            val prefillFunc = try {
                module.getFunction("prefill")
            } catch (e: Exception) {
                Timber.w("prefill function not found, using forward")
                module.getFunction("forward")
            }

            val decodeFunc = try {
                module.getFunction("decode")
            } catch (e: Exception) {
                Timber.w("decode function not found, using forward")
                prefillFunc  // Use same function as fallback
            }

            val resetFunc = try {
                module.getFunction("reset_kv_cache")
            } catch (e: Exception) {
                Timber.w("reset_kv_cache function not found")
                null
            }

            // Use device from constructor, or override
            val targetDevice = if (deviceOverride != device.toString()) {
                when (deviceOverride.lowercase()) {
                    "vulkan" -> Device.vulkan(0)
                    "opencl" -> Device.opencl(0)
                    "cpu" -> Device.cpu(0)
                    "gpu" -> Device.vulkan(0)  // Alias - prefer Vulkan for modern devices
                    else -> device
                }
            } else {
                device
            }

            Timber.i("TVM module initialized on device: $targetDevice")

            TVMModule(
                module = module,
                prefillFunc = prefillFunc,
                decodeFunc = decodeFunc,
                resetCacheFunc = resetFunc,
                device = targetDevice
            )

        } catch (e: Throwable) {
            // Catch all throwables including Errors (like UnsatisfiedLinkError)
            Timber.e(e, "Failed to load TVM module")
            throw RuntimeException("TVM module loading failed: ${e.message}", e)
        }
    }

    // Fallback simple tokenizer (used when model-specific tokenizer unavailable)
    private val fallbackTokenizer by lazy {
        SimpleVocabTokenizer(context)
    }

    // Model-specific tokenizer (loaded from model directory)
    private var modelTokenizer: ITokenizer? = null

    /**
     * Load tokenizer from model directory.
     *
     * Issue P0-1 Fix: Loads HuggingFace tokenizer.json from model directory
     * instead of using the incorrect SimpleVocabTokenizer.
     *
     * @param modelDir Model directory containing tokenizer.json
     */
    fun loadTokenizer(modelDir: String) {
        try {
            val dir = java.io.File(modelDir)
            val tokenizerFile = java.io.File(dir, "tokenizer.json")

            if (tokenizerFile.exists()) {
                modelTokenizer = HuggingFaceTokenizer.load(tokenizerFile)
                Timber.i("Loaded HuggingFace tokenizer from ${dir.name}")
            } else {
                Timber.w("No tokenizer.json found in ${dir.name}, using fallback")
                modelTokenizer = null
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to load tokenizer, using fallback")
            modelTokenizer = null
        }
    }

    /**
     * Get the current tokenizer.
     *
     * @return Model-specific tokenizer if loaded, fallback otherwise
     */
    fun getTokenizer(): ITokenizer {
        return modelTokenizer ?: fallbackTokenizer
    }

    /**
     * Create tokenizer for this runtime
     *
     * @return TVMTokenizer instance
     */
    fun createTokenizer(): TVMTokenizer {
        return TVMTokenizer(this)
    }

    /**
     * Tokenize text to IDs
     *
     * Issue P0-1 Fix: Uses model-specific HuggingFace tokenizer when available.
     * Falls back to SimpleVocabTokenizer only when model tokenizer not loaded.
     *
     * @param text Input text
     * @return List of token IDs
     */
    fun tokenize(text: String): List<Int> {
        return getTokenizer().encode(text)
    }

    /**
     * Detokenize IDs to text
     *
     * Issue P0-1 Fix: Uses model-specific HuggingFace tokenizer when available.
     * Falls back to SimpleVocabTokenizer only when model tokenizer not loaded.
     *
     * @param tokenIds List of token IDs
     * @return Decoded text
     */
    fun detokenize(tokenIds: List<Int>): String {
        return getTokenizer().decode(tokenIds)
    }

    /**
     * Check if model-specific tokenizer is loaded
     *
     * @return true if HuggingFace tokenizer loaded, false if using fallback
     */
    fun hasModelTokenizer(): Boolean {
        return modelTokenizer != null
    }

    /**
     * Clean up resources
     *
     * Releases the model-specific tokenizer reference. TVMRuntime does not own
     * TVM modules directly — modules are returned to callers via loadModule() and
     * must be disposed by the caller via TVMModule.dispose(). The fallback tokenizer
     * is a lazy val tied to this instance's lifetime and requires no explicit release.
     */
    fun dispose() {
        Timber.d("Disposing TVM runtime")
        modelTokenizer = null
    }
}

/**
 * TVM Module wrapper for model inference
 *
 * Wraps TVM Module and provides inference functions.
 * Handles both prefill (initial tokens) and decode (single token) passes.
 */
class TVMModule(
    private val module: Module,
    private val prefillFunc: Function?,
    private val decodeFunc: Function?,
    private val resetCacheFunc: Function?,
    private val device: Device
) {

    private var kvCacheInitialized = false

    /**
     * Run forward pass (token prediction)
     *
     * Automatically selects prefill or decode based on input length.
     *
     * @param tokenIds Input token IDs
     * @return Logits (probability distribution over vocabulary)
     */
    fun forward(tokenIds: IntArray): FloatArray {
        return try {
            Timber.v("TVM forward pass: ${tokenIds.size} tokens")

            // Select function based on input length
            val func = if (tokenIds.size > 1 || !kvCacheInitialized) {
                // Prefill: process multiple tokens (first pass)
                kvCacheInitialized = true
                prefillFunc
            } else {
                // Decode: process single token (subsequent passes)
                decodeFunc
            }

            if (func == null) {
                throw IllegalStateException("TVM function not available")
            }

            // Create input tensor from token IDs
            val inputShape = longArrayOf(1, tokenIds.size.toLong())
            val inputTensor = Tensor.empty(inputShape, TVMType("int32", 32), device)
            inputTensor.copyFrom(tokenIds)

            // Run inference by calling TVM function
            val result = func.pushArg(inputTensor).invoke()

            // Extract output tensor (cast TVMValue to Tensor)
            val outputTensor = (result as TensorBase) as Tensor

            // Get output shape to determine vocab size
            val outputShape = outputTensor.shape()
            val vocabSize = outputShape[outputShape.size - 1].toInt()

            // Extract logits as float array
            val logits = outputTensor.asFloatArray()

            // Clean up tensors
            inputTensor.release()
            outputTensor.release()

            logits

        } catch (e: Exception) {
            Timber.e(e, "TVM forward pass failed")
            throw RuntimeException("Forward pass failed: ${e.message}", e)
        }
    }

    /**
     * Generate next token using sampling strategies
     *
     * Combines forward() with TokenSampler for high-quality generation.
     *
     * @param tokenIds Current sequence of token IDs
     * @param temperature Controls randomness (0.1-2.0)
     * @param topP Nucleus sampling threshold (0.9-0.98)
     * @param topK Number of top tokens to consider (40-100)
     * @param repetitionPenalty Penalty for repeating tokens (1.0-1.3)
     * @param previousTokens Recent tokens for repetition penalty
     * @return Next token ID
     */
    fun generateNextToken(
        tokenIds: IntArray,
        temperature: Float = 0.8f,
        topP: Float = 0.95f,
        topK: Int = 50,
        repetitionPenalty: Float = 1.15f,
        previousTokens: List<Int> = emptyList()
    ): Int {
        // Get logits from model
        val logits = forward(tokenIds)

        // Sample next token using strategies
        return TokenSampler.sample(
            logits = logits,
            temperature = temperature,
            topP = topP,
            topK = topK,
            repetitionPenalty = repetitionPenalty,
            previousTokens = previousTokens
        )
    }

    /**
     * Generate text with streaming output
     *
     * Generates tokens one by one and emits decoded text chunks in real-time.
     * Enables typewriter effect in UI.
     *
     * @param tokenIds Initial prompt tokens
     * @param maxTokens Maximum tokens to generate (null = unlimited)
     * @param temperature Sampling temperature
     * @param topP Nucleus sampling threshold
     * @param topK Top-k sampling count
     * @param repetitionPenalty Repetition penalty factor
     * @param stopTokens Token IDs that should stop generation
     * @param tokenizer Tokenizer for decoding (must be provided for streaming)
     * @return Flow of generated text chunks
     */
    fun generateStreaming(
        tokenIds: IntArray,
        maxTokens: Int? = null,
        temperature: Float = 0.8f,
        topP: Float = 0.95f,
        topK: Int = 50,
        repetitionPenalty: Float = 1.15f,
        stopTokens: Set<Int> = emptySet(),
        tokenizer: ((List<Int>) -> String)? = null
    ): Flow<String> = flow {
        try {
            Timber.d("Starting streaming generation (max tokens: $maxTokens)")

            val generatedTokens = mutableListOf<Int>()
            val contextTokens = tokenIds.toMutableList()
            var tokensGenerated = 0

            while (true) {
                // Check if we should stop
                if (maxTokens != null && tokensGenerated >= maxTokens) {
                    Timber.d("Reached max token limit: $maxTokens")
                    break
                }

                // Check if coroutine is still active
                if (!coroutineContext.isActive) {
                    Timber.d("Generation cancelled by coroutine")
                    break
                }

                // Generate next token
                val nextToken = generateNextToken(
                    tokenIds = contextTokens.toIntArray(),
                    temperature = temperature,
                    topP = topP,
                    topK = topK,
                    repetitionPenalty = repetitionPenalty,
                    previousTokens = generatedTokens
                )

                // Check if we hit a stop token
                if (nextToken in stopTokens) {
                    Timber.d("Hit stop token: $nextToken")
                    break
                }

                // Add to generated tokens
                generatedTokens.add(nextToken)
                contextTokens.add(nextToken)
                tokensGenerated++

                // Decode and emit text chunk
                if (tokenizer != null) {
                    // Decode the latest token(s) - use last few for context
                    val textChunk = try {
                        // Decode incrementally for smooth streaming
                        val recentTokens = generatedTokens.takeLast(1)
                        tokenizer(recentTokens)
                    } catch (e: Exception) {
                        Timber.w(e, "Failed to decode token: $nextToken")
                        ""
                    }

                    if (textChunk.isNotEmpty()) {
                        emit(textChunk)
                    }
                } else {
                    // Emit token ID as string if no tokenizer provided
                    emit("$nextToken ")
                }

                Timber.v("Generated token $tokensGenerated: $nextToken")
            }

            Timber.i("Streaming generation complete: $tokensGenerated tokens")

        } catch (e: Exception) {
            Timber.e(e, "Streaming generation failed")
            throw RuntimeException("Streaming generation failed: ${e.message}", e)
        }
    }

    /**
     * Generate complete text (non-streaming)
     *
     * Generates all tokens and returns the complete decoded text.
     * Use this for batch processing when streaming is not needed.
     *
     * @param tokenIds Initial prompt tokens
     * @param maxTokens Maximum tokens to generate
     * @param temperature Sampling temperature
     * @param topP Nucleus sampling threshold
     * @param topK Top-k sampling count
     * @param repetitionPenalty Repetition penalty factor
     * @param stopTokens Token IDs that should stop generation
     * @param tokenizer Tokenizer for decoding
     * @return Generated text
     */
    suspend fun generate(
        tokenIds: IntArray,
        maxTokens: Int = 512,
        temperature: Float = 0.8f,
        topP: Float = 0.95f,
        topK: Int = 50,
        repetitionPenalty: Float = 1.15f,
        stopTokens: Set<Int> = emptySet(),
        tokenizer: (List<Int>) -> String
    ): String {
        val generatedTokens = mutableListOf<Int>()
        val contextTokens = tokenIds.toMutableList()

        repeat(maxTokens) { iteration ->
            // Generate next token
            val nextToken = generateNextToken(
                tokenIds = contextTokens.toIntArray(),
                temperature = temperature,
                topP = topP,
                topK = topK,
                repetitionPenalty = repetitionPenalty,
                previousTokens = generatedTokens
            )

            // Check stop condition
            if (nextToken in stopTokens) {
                Timber.d("Hit stop token at iteration $iteration")
                return@repeat
            }

            generatedTokens.add(nextToken)
            contextTokens.add(nextToken)
        }

        // Decode all tokens
        return tokenizer(generatedTokens)
    }

    /**
     * Reset KV cache (for new conversation)
     *
     * Clears the key-value cache used for autoregressive generation.
     * Call this when starting a new conversation.
     */
    fun resetCache() {
        try {
            resetCacheFunc?.invoke()
            kvCacheInitialized = false
            Timber.d("KV cache reset")
        } catch (e: Exception) {
            Timber.w(e, "Failed to reset KV cache (function may not exist)")
        }
    }

    /**
     * Clean up resources
     *
     * Releases TVM module and frees memory.
     * Call this when done with the model.
     */
    fun dispose() {
        try {
            module.release()
            Timber.d("TVM module disposed")
        } catch (e: Exception) {
            Timber.e(e, "Error disposing TVM module")
        }
    }
}
