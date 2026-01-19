/*
 * Copyright (c) 2024-2025 Intelligent Devices LLC / Augmentalis
 * All Rights Reserved.
 *
 * This file is part of AVA AI and is proprietary software.
 * See LICENSE file in the project root for license information.
 */

/**
 * Local LLM Provider for AVA AI
 *
 * Implements LLMProvider interface using the rewritten ALC Engine.
 * Provides on-device LLM inference with:
 * - 95%+ local processing (privacy-first)
 * - GPU acceleration (OpenCL/Vulkan)
 * - Streaming responses via Kotlin Flow
 * - Offline capability
 * - Memory optimization for <512MB devices
 * - Automatic language detection and model selection
 * - Dynamic model switching for optimal multilingual support
 *
 * Created: 2025-10-30
 * Updated: 2025-11-07 - Added auto-model selection
 * Author: AVA AI Team
 */

package com.augmentalis.llm.provider

import android.content.Context
import com.augmentalis.llm.LLMResult
import com.augmentalis.llm.LLMResponse
import com.augmentalis.llm.CommandInterpretationResult
import com.augmentalis.llm.ClarificationResult
import com.augmentalis.llm.alc.ALCEngine
import com.augmentalis.llm.alc.ALCEngineSingleLanguage
import com.augmentalis.llm.alc.language.LanguagePackManager
import com.augmentalis.llm.alc.interfaces.*
import com.augmentalis.llm.alc.inference.MLCInferenceStrategy
import com.augmentalis.llm.alc.inference.GGUFInferenceStrategy
import com.augmentalis.llm.alc.samplers.TopPSampler
import com.augmentalis.llm.alc.streaming.BackpressureStreamingManager
import com.augmentalis.llm.alc.memory.KVCacheMemoryManager
import com.augmentalis.llm.alc.tokenizer.HuggingFaceTokenizer
import com.augmentalis.llm.cache.TokenCacheManager
import com.augmentalis.llm.metrics.LatencyMetrics
import com.augmentalis.llm.domain.*
import com.augmentalis.llm.ModelSelector
import com.augmentalis.llm.LanguageDetector
import com.augmentalis.llm.SystemPromptManager
import com.augmentalis.llm.ScreenContext
import com.augmentalis.llm.UserContext
import com.augmentalis.llm.config.DeviceModelSelector
import com.augmentalis.llm.config.DeviceProfile
import com.augmentalis.llm.config.ModelConfiguration
import com.augmentalis.llm.config.ConfigurationType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import com.augmentalis.llm.alc.loader.ModelDiscovery
import com.augmentalis.ava.core.data.db.DatabaseDriverFactory
import com.augmentalis.ava.core.data.db.createDatabase

/**
 * Local LLM provider using ALC Engine with auto-model selection
 *
 * Thread Safety (Issue C-03):
 * - All model access is protected by modelMutex
 * - Prevents concurrent initialization and inference
 * - Ensures safe model switching
 */
class LocalLLMProvider(
    private val context: Context,
    private val autoModelSelection: Boolean = true,
    private val tokenCacheManager: TokenCacheManager? = null
) : LLMProvider {

    // Thread safety mutex for model operations (Issue C-03)
    private val modelMutex = Mutex()

    private var alcEngine: ALCEngineSingleLanguage? = null
    private var currentConfig: LLMConfig? = null
    private val modelSelector = ModelSelector(context)
    private val languageDetector = LanguageDetector
    private val systemPromptManager = SystemPromptManager(context)
    private val latencyMetrics = LatencyMetrics()
    private var currentModelId: String? = null
    private var currentScreenContext: ScreenContext? = null
    private var currentUserContext: UserContext? = null

    // HuggingFace tokenizer for token caching
    private var huggingFaceTokenizer: HuggingFaceTokenizer? = null

    // GGUF inference strategy (for llama.cpp models)
    private var ggufStrategy: GGUFInferenceStrategy? = null
    private var isGgufModel: Boolean = false

    // Device-based model selection (from AVA-DEVICE-MODEL-MATRIX.md)
    private val modelDiscovery = ModelDiscovery(context)
    private val deviceModelSelector = DeviceModelSelector(context, modelDiscovery)
    private var detectedDevice: DeviceProfile? = null
    private var currentConfiguration: ModelConfiguration? = null

    override suspend fun initialize(config: LLMConfig): LLMResult<Unit> = modelMutex.withLock {
        return@withLock try {
            Timber.i("Initializing LocalLLMProvider with model: ${config.modelPath}")
            val startTime = System.currentTimeMillis()

            // 1. Validate model path - try provided path first, fallback to discovery
            val modelDir = resolveModelPath(config.modelPath)
                ?: run {
                    // Fallback: Use ModelDiscovery to find an available model
                    Timber.w("Provided model path not found, attempting automatic discovery...")
                    val discovery = ModelDiscovery(context)
                    val discoveredModel = discovery.getFirstAvailableModel()

                    if (discoveredModel != null) {
                        Timber.i("Auto-discovered model: ${discoveredModel.name} at ${discoveredModel.path}")

                        // H-05: Verify model integrity before loading
                        val verificationResult = discovery.verifyModelIntegrity(discoveredModel)
                        when (verificationResult.status) {
                            ModelDiscovery.VerificationStatus.VERIFIED -> {
                                Timber.i("Model integrity verified: ${discoveredModel.name}")
                            }
                            ModelDiscovery.VerificationStatus.NO_CHECKSUM -> {
                                Timber.w("Model has no checksum - skipping verification: ${discoveredModel.name}")
                            }
                            ModelDiscovery.VerificationStatus.CHECKSUM_MISMATCH -> {
                                val error = "Model checksum mismatch! Model may be corrupted: ${discoveredModel.name}\n" +
                                        "Expected: ${verificationResult.expectedChecksum}\n" +
                                        "Actual: ${verificationResult.actualChecksum}"
                                Timber.e(error)
                                latencyMetrics.recordError(error)
                                return LLMResult.Error(
                                    message = "Model checksum verification failed - model may be corrupted or tampered with",
                                    cause = SecurityException("Model integrity verification failed")
                                )
                            }
                            ModelDiscovery.VerificationStatus.CALCULATION_FAILED -> {
                                Timber.w("Failed to calculate model checksum - proceeding anyway: ${discoveredModel.name}")
                            }
                            ModelDiscovery.VerificationStatus.NOT_FOUND -> {
                                val error = "Model directory not found: ${discoveredModel.path}"
                                Timber.e(error)
                                latencyMetrics.recordError(error)
                                return LLMResult.Error(
                                    message = "Model not found",
                                    cause = java.io.FileNotFoundException(error)
                                )
                            }
                        }

                        File(discoveredModel.path)
                    } else {
                        // No models found anywhere
                        val error = "No LLM models found. Checked paths:\n" +
                            "- ${config.modelPath}\n" +
                            "- /sdcard/ava-ai-models/llm/\n" +
                            "Please download a model first."
                        Timber.e(error)
                        latencyMetrics.recordError(error)
                        return LLMResult.Error(
                            message = "No LLM models installed. Please download a model.",
                            cause = java.io.FileNotFoundException(error)
                        )
                    }
                }

            // Validate model directory has required files
            if (!isValidModelDirectory(modelDir)) {
                val error = "Invalid model directory (missing required files): ${modelDir.absolutePath}"
                Timber.e(error)
                latencyMetrics.recordError(error)
                return LLMResult.Error(
                    message = "Model directory missing required files",
                    cause = java.io.FileNotFoundException(error)
                )
            }

            Timber.i("Using model directory: ${modelDir.absolutePath}")

            // Detect model format (GGUF vs MLC/TVM)
            val isGguf = isGgufModelDirectory(modelDir)
            isGgufModel = isGguf

            // 2. Save configuration
            currentConfig = config
            currentModelId = modelDir.name

            if (isGguf) {
                // ==================== GGUF Model Path (llama.cpp) ====================
                Timber.i("Detected GGUF model format - using llama.cpp runtime")

                // Create GGUF inference strategy from model directory
                val strategy = GGUFInferenceStrategy.fromModelDirectory(context, modelDir)

                if (strategy == null) {
                    val error = "Failed to create GGUF inference strategy for: ${modelDir.absolutePath}"
                    Timber.e(error)
                    latencyMetrics.recordError(error)
                    return LLMResult.Error(
                        message = "GGUF model initialization failed",
                        cause = IllegalStateException(error)
                    )
                }

                // Load the model (runs on IO dispatcher internally)
                val loaded = strategy.loadModel()
                if (!loaded) {
                    val error = "Failed to load GGUF model: ${modelDir.absolutePath}"
                    Timber.e(error)
                    latencyMetrics.recordError(error)
                    return LLMResult.Error(
                        message = "GGUF model load failed",
                        cause = IllegalStateException(error)
                    )
                }

                ggufStrategy = strategy

                // 4. Track initialization latency
                val initTime = System.currentTimeMillis() - startTime
                latencyMetrics.recordInitialization(initTime)

                Timber.i("LocalLLMProvider initialized successfully in ${initTime}ms")
                Timber.i("✅ GGUF model loaded via llama.cpp - on-device inference ready")
                Timber.i(latencyMetrics.getSummary())

                return LLMResult.Success(Unit)
            }
            // ==================== LiteRT Model Path (Gemma 3n) ====================
            // Check for LiteRT configuration or .tflite file
            val isLiteRT = config.llmRuntime == "LiteRT" || isLiteRTModelDirectory(modelDir)
            if (isLiteRT) {
                Timber.i("Detected LiteRT model format - using Google LiteRT runtime")

                try {
                    // Create LiteRT inference strategy
                    val strategy = com.augmentalis.llm.alc.inference.LiteRTInferenceStrategy(context, modelDir.absolutePath)
                    
                    if (!strategy.isAvailable()) {
                        throw IllegalStateException("LiteRT library not available or model not found")
                    }

                    // Initialize immediately
                    strategy.initialize()
                    
                    // TODO: Wire up to ALCEngine when multi-strategy engine is ready
                    // For now, we store it for direct access if needed, or fail if ALC engine expects MLC
                    // This is a partial implementation pending Engine refactor
                    Timber.w("LiteRT strategy initialized but ALCEngine wiring is pending Multi-Strategy refactor")
                    
                    // Track initialization latency
                    val initTime = System.currentTimeMillis() - startTime
                    latencyMetrics.recordInitialization(initTime)
                    
                    Timber.i("LocalLLMProvider initialized (LiteRT) in ${initTime}ms")
                    return LLMResult.Success(Unit)
                    
                } catch (e: Exception) {
                     val error = "Failed to initialize LiteRT: ${e.message}"
                     Timber.e(e, error)
                     latencyMetrics.recordError(error)
                     return LLMResult.Error(error, IllegalStateException(error))
                }
            }

            // ==================== MLC/TVM Model Path ====================
            // Use modelDir instead of modelFile for the rest of initialization
            val modelFile = modelDir

            Timber.d("Model file found: ${modelFile.absolutePath} (${modelFile.length() / 1024 / 1024}MB)")

            // 3. Create ALCEngine dependencies
            Timber.d("Creating ALCEngine components...")

            // 3.1 Create memory manager (2GB budget for LLM inference)
            val memoryBudgetBytes = 2L * 1024L * 1024L * 1024L // 2GB
            val memoryManager = KVCacheMemoryManager(memoryBudgetBytes)
            Timber.d("Created KVCacheMemoryManager with ${memoryBudgetBytes / 1024 / 1024}MB budget")

            // 3.2 Create sampler (top-p nucleus sampling)
            val sampler = TopPSampler()
            Timber.d("Created TopPSampler")

            // 3.3 Create TVM runtime
            val tvmRuntime = com.augmentalis.llm.alc.TVMRuntime.create(
                context = context,
                deviceType = config.device
            )
            Timber.d("Created TVMRuntime with device: ${config.device}")

            // 3.4 Load model-specific tokenizer from model directory
            // Issue P0-1 Fix: Load HuggingFace tokenizer.json for correct vocab
            val resolvedModelPath = modelFile.absolutePath
            val modelDirPath = if (modelFile.isDirectory) {
                modelFile.absolutePath
            } else {
                modelFile.parentFile?.absolutePath ?: modelFile.absolutePath
            }
            tvmRuntime.loadTokenizer(modelDirPath)
            if (tvmRuntime.hasModelTokenizer()) {
                Timber.i("Loaded model-specific tokenizer from $modelDirPath")
            } else {
                Timber.w("Using fallback tokenizer (tokenizer.json not found in model)")
            }

            // 3.4.1 Load HuggingFace tokenizer for token caching
            // This enables ~50x faster context building for cached content
            try {
                val modelDirFile = File(modelDirPath)
                if (File(modelDirFile, "tokenizer.json").exists()) {
                    huggingFaceTokenizer = HuggingFaceTokenizer.load(modelDirFile)
                    val modelId = modelDirFile.name
                    currentModelId = modelId
                    tokenCacheManager?.setTokenizer(huggingFaceTokenizer!!, modelId)
                    Timber.i("Token cache enabled for model: $modelId")
                } else {
                    Timber.w("tokenizer.json not found - token caching disabled")
                }
            } catch (e: Exception) {
                Timber.w(e, "Failed to initialize token cache (non-fatal)")
                huggingFaceTokenizer = null
            }

            // 3.5 Create tokenizer wrapper
            val tokenizer = com.augmentalis.llm.alc.tokenizer.TVMTokenizer(tvmRuntime)
            Timber.d("Created TVMTokenizer")

            // 3.6 Load model via TVM runtime (MUST run on IO dispatcher to avoid UI freeze)
            // Module.load() is a blocking JNI call that can take 10-30 seconds for large models
            val modelLib = config.modelLib ?: inferModelLib(resolvedModelPath)
            Timber.i("Loading TVM module on IO dispatcher (this may take 10-30 seconds)...")
            val tvmModule = withContext(Dispatchers.IO) {
                tvmRuntime.loadModule(
                    modelPath = resolvedModelPath,
                    modelLib = modelLib,
                    deviceOverride = config.device
                )
            }
            Timber.d("Loaded TVMModule: $modelLib")

            // 3.7 Create inference strategy
            val inferenceStrategy = MLCInferenceStrategy(model = tvmModule)
            Timber.d("Created MLCInferenceStrategy")

            // 3.8 Create streaming manager
            val streamingManager = BackpressureStreamingManager(
                inferenceStrategy = inferenceStrategy,
                samplerStrategy = sampler,
                memoryManager = memoryManager,
                tokenizer = tokenizer,
                bufferSize = 128 // Buffer 128 tokens for backpressure control
            )
            Timber.d("Created BackpressureStreamingManager with buffer size 128")

            // 3.9 Create model loader for ALCEngine
            val modelLoader = com.augmentalis.llm.alc.loader.TVMModelLoader(context)

            // 3.10 Create ALCEngine (single-language version for simplicity)
            alcEngine = com.augmentalis.llm.alc.ALCEngineSingleLanguage(
                context = context,
                modelLoader = modelLoader,
                inferenceStrategy = inferenceStrategy,
                streamingManager = streamingManager,
                memoryManager = memoryManager,
                samplerStrategy = sampler
            )

            Timber.i("ALCEngine created successfully with all components wired")

            // 4. Track initialization latency
            val initTime = System.currentTimeMillis() - startTime
            latencyMetrics.recordInitialization(initTime)

            Timber.i("LocalLLMProvider initialized successfully in ${initTime}ms")
            Timber.i("✅ ALCEngine fully integrated - on-device inference ready")
            Timber.i(latencyMetrics.getSummary())

            LLMResult.Success(Unit)

        } catch (e: Throwable) {
            // Catch all throwables including Errors to ensure graceful fallback
            val error = "Failed to initialize LocalLLMProvider: ${e.message}"
            Timber.e(e, error)
            latencyMetrics.recordError(error)
            LLMResult.Error(
                message = "Initialization failed: ${e.message}",
                cause = if (e is Exception) e else RuntimeException(e)
            )
        }
    }

    override suspend fun generateResponse(
        prompt: String,
        options: GenerationOptions
    ): Flow<LLMResponse> {
        // Check if model switching is needed (language-aware)
        if (autoModelSelection) {
            val modelId = currentModelId
            if (modelId == null) {
                Timber.w("Cannot perform model selection: no model currently loaded")
            } else {
                val recommendedModelId = modelSelector.getModelSwitchRecommendation(
                    currentModelId = modelId,
                    text = prompt
                )

                if (recommendedModelId != null && recommendedModelId != currentModelId) {
                    Timber.i("Language change detected, switching model: $currentModelId -> $recommendedModelId")
                    // TODO: Implement hot-swapping when ready
                    // For now, just log the recommendation
                }
            }
        }

        // Convert to chat format
        val messages = listOf(
            ChatMessage(
                role = MessageRole.USER,
                content = prompt
            )
        )

        return chat(messages, options)
    }

    override suspend fun chat(
        messages: List<ChatMessage>,
        options: GenerationOptions
    ): Flow<LLMResponse> {
        // Check if using GGUF model
        if (isGgufModel) {
            return chatWithGguf(messages, options)
        }

        // Thread-safe engine access (Issue C-03)
        val engine = modelMutex.withLock { alcEngine } ?: return kotlinx.coroutines.flow.flow {
            latencyMetrics.recordError("Engine not initialized")
            emit(LLMResponse.Error(
                message = "Engine not initialized",
                code = "NOT_INITIALIZED"
            ))
        }

        // Convert AVA GenerationOptions to ALC GenerationOptions
        // Convert AVA GenerationOptions to ALC GenerationOptions
        val alcOptions = com.augmentalis.llm.alc.GenerationOptions(
            temperature = options.temperature,
            maxTokens = options.maxTokens,
            topP = options.topP,
            frequencyPenalty = options.frequencyPenalty,
            presencePenalty = options.presencePenalty,
            stopSequences = options.stopSequences
        )

        // Inject Working Memory Context (Phase 3: Goldfish Memory Fix)
        // Only inject if this is a standard chat (not tool use or specialized)
        val workingMemory = getWorkingMemoryContext()
        val effectiveMessages = if (workingMemory.isNotBlank()) {
            val systemMessage = messages.find { it.role == MessageRole.SYSTEM }
            if (systemMessage != null) {
                // Append to existing system message
                messages.map { 
                    if (it.role == MessageRole.SYSTEM) {
                        it.copy(content = it.content + "\n\n" + workingMemory)
                    } else it
                }
            } else {
                // Create new system message
                listOf(ChatMessage(MessageRole.SYSTEM, workingMemory)) + messages
            }
        } else {
            messages
        }

        // Track inference latency and errors
        val startTime = System.currentTimeMillis()
        var hasError = false

        // Use ALC Engine's chat function with metrics tracking
        return kotlinx.coroutines.flow.flow {
            try {
                engine.chat(effectiveMessages, alcOptions).collect { response ->
                    // Check for errors in the stream
                    if (response is LLMResponse.Error) {
                        hasError = true
                        latencyMetrics.recordError(response.message)
                    }
                    emit(response)
                }

                // Record successful inference if no errors
                if (!hasError) {
                    val latency = System.currentTimeMillis() - startTime
                    latencyMetrics.recordInference(latency)
                }
            } catch (e: Exception) {
                // Record error on exception
                hasError = true
                latencyMetrics.recordError(e.message ?: "Unknown error")

                // Re-throw to maintain error handling behavior
                throw e
            }
        }
    }

    /**
     * Chat using GGUF model via llama.cpp
     *
     * Converts messages to prompt format and streams response tokens
     */
    private fun chatWithGguf(
        messages: List<ChatMessage>,
        options: GenerationOptions
    ): Flow<LLMResponse> = kotlinx.coroutines.flow.flow {
        val strategy = ggufStrategy

        if (strategy == null) {
            latencyMetrics.recordError("GGUF strategy not initialized")
            emit(LLMResponse.Error(
                message = "GGUF model not initialized",
                code = "NOT_INITIALIZED"
            ))
            return@flow
        }

        val startTime = System.currentTimeMillis()
        var hasError = false

        try {
            // Build prompt from messages
            val prompt = buildPromptFromMessages(messages)
            Timber.d("GGUF chat prompt: ${prompt.take(100)}...")

            // Convert to GGUF generation params
            val params = com.augmentalis.llm.alc.models.GenerationParams(
                temperature = options.temperature,
                topP = options.topP,
                topK = 40,
                repeatPenalty = 1.0f + options.frequencyPenalty,
                maxTokens = options.maxTokens ?: 512,
                stopSequences = options.stopSequences
            )

            // Stream response tokens
            val responseBuilder = StringBuilder()
            strategy.generateStreaming(prompt, params).collect { token ->
                responseBuilder.append(token)
                emit(LLMResponse.Streaming(
                    chunk = token,
                    tokenCount = null
                ))
            }

            // Final complete response
            val fullResponse = responseBuilder.toString()
            emit(LLMResponse.Complete(
                fullText = fullResponse,
                usage = TokenUsage(
                    promptTokens = prompt.length / 4, // Rough estimate
                    completionTokens = fullResponse.length / 4,
                    totalTokens = (prompt.length + fullResponse.length) / 4
                )
            ))

            val latency = System.currentTimeMillis() - startTime
            latencyMetrics.recordInference(latency)
            Timber.i("GGUF chat completed in ${latency}ms")

        } catch (e: Exception) {
            hasError = true
            val errorMsg = "GGUF generation failed: ${e.message}"
            Timber.e(e, errorMsg)
            latencyMetrics.recordError(errorMsg)
            emit(LLMResponse.Error(
                message = errorMsg,
                code = "GENERATION_FAILED"
            ))
        }
    }

    /**
     * Build a prompt string from chat messages using Gemma format
     */
    private fun buildPromptFromMessages(messages: List<ChatMessage>): String {
        val builder = StringBuilder()

        for (message in messages) {
            when (message.role) {
                MessageRole.SYSTEM -> {
                    builder.append("<start_of_turn>user\n")
                    builder.append("System: ${message.content}\n")
                    builder.append("<end_of_turn>\n")
                }
                MessageRole.USER -> {
                    builder.append("<start_of_turn>user\n")
                    builder.append(message.content)
                    builder.append("<end_of_turn>\n")
                }
                MessageRole.ASSISTANT -> {
                    builder.append("<start_of_turn>model\n")
                    builder.append(message.content)
                    builder.append("<end_of_turn>\n")
                }
                MessageRole.TOOL -> {
                    // Tool responses treated as system context
                    builder.append("<start_of_turn>user\n")
                    builder.append("Tool result: ${message.content}\n")
                    builder.append("<end_of_turn>\n")
                }
            }
        }

        // Prompt model to respond
        builder.append("<start_of_turn>model\n")

        return builder.toString()
    }

    override suspend fun stop() {
        modelMutex.withLock {
            alcEngine?.stop()
        }
    }

    override suspend fun reset() {
        modelMutex.withLock {
            alcEngine?.reset()
        }
    }

    override suspend fun cleanup() = modelMutex.withLock {
        alcEngine?.cleanup()
        alcEngine = null

        // Cleanup GGUF strategy
        ggufStrategy?.unloadModel()
        ggufStrategy = null
        isGgufModel = false

        huggingFaceTokenizer = null

        currentConfig = null

        Timber.d("LocalLLMProvider cleaned up")
    }

    /**
     * Get token cache manager for external use.
     * Allows callers to cache tokens for context building.
     */
    fun getTokenCacheManager(): TokenCacheManager? = tokenCacheManager

    /**
     * Get HuggingFace tokenizer if available.
     * Can be used for direct tokenization without caching.
     */
    fun getHuggingFaceTokenizer(): HuggingFaceTokenizer? = huggingFaceTokenizer

    /**
     * Unload model to free memory (Issue M-01).
     *
     * Call this when:
     * - System broadcasts ACTION_TRIM_MEMORY with TRIM_MEMORY_RUNNING_LOW or higher
     * - App receives onLowMemory() callback
     * - Background state and memory pressure is high
     *
     * The model can be reloaded via initialize() when needed.
     *
     * @return true if model was unloaded, false if no model was loaded
     */
    suspend fun unloadModelForLowMemory(): Boolean = modelMutex.withLock {
        val engine = alcEngine
        if (engine == null) {
            Timber.d("unloadModelForLowMemory: No model loaded")
            return@withLock false
        }

        Timber.w("Unloading LLM model due to low memory (estimated: ${estimateMemoryUsage()}MB)")

        try {
            engine.cleanup()
            alcEngine = null
            // Keep currentConfig so we can reload later

            // Force GC to release memory faster
            System.gc()

            Timber.i("LLM model unloaded successfully - memory freed")
            latencyMetrics.recordModelUnload()
            return@withLock true
        } catch (e: Exception) {
            Timber.e(e, "Failed to unload LLM model")
            return@withLock false
        }
    }

    /**
     * Check if model is currently loaded.
     *
     * Use this to determine if the model needs to be reloaded after
     * low memory unload.
     *
     * @return true if model is loaded and ready
     */
    fun isModelLoaded(): Boolean {
        return alcEngine != null
    }

    /**
     * Reload model after low memory unload.
     *
     * Uses the last configuration to reload the model.
     *
     * @return LLMResult.Success if reloaded, LLMResult.Error if no previous config or failed
     */
    suspend fun reloadModel(): LLMResult<Unit> {
        val config = currentConfig
            ?: return LLMResult.Error(
                message = "Cannot reload: No previous model configuration stored",
                cause = IllegalStateException("No previous configuration")
            )

        Timber.i("Reloading LLM model: ${config.modelPath}")
        return initialize(config)
    }

    override fun isGenerating(): Boolean {
        return alcEngine?.isGenerating() ?: false
    }

    override fun getInfo(): LLMProviderInfo {
        val config = currentConfig

        return LLMProviderInfo(
            name = "ALC Engine",
            version = "1.0",
            modelName = extractModelName(config?.modelPath),
            isLocal = true,
            capabilities = LLMCapabilities(
                supportsStreaming = true,
                supportsChat = true,
                supportsFunctionCalling = false,
                maxContextLength = 2048
            )
        )
    }

    override suspend fun checkHealth(): LLMResult<ProviderHealth> {
        return try {
            val status = if (alcEngine != null) {
                HealthStatus.HEALTHY
            } else {
                HealthStatus.UNHEALTHY
            }

            LLMResult.Success(
                ProviderHealth(
                    status = status,
                    averageLatencyMs = latencyMetrics.getAverageLatency(),
                    errorRate = latencyMetrics.getErrorRate()?.toDouble(),
                    lastError = latencyMetrics.getLastError() ?:
                        if (status == HealthStatus.UNHEALTHY) "Engine not initialized" else null,
                    lastChecked = System.currentTimeMillis()
                )
            )
        } catch (e: Exception) {
            LLMResult.Error(
                message = "Health check failed: ${e.message}",
                cause = e
            )
        }
    }

    override fun estimateCost(inputTokens: Int, outputTokens: Int): Double {
        // Local LLM has zero cost
        return 0.0
    }

    /**
     * Infer model library name from model path
     */
    private fun inferModelLib(modelPath: String): String {
        val fileName = File(modelPath).nameWithoutExtension
        return fileName.replace("-", "_").lowercase()
    }

    /**
     * Extract model name from path for display
     */
    private fun extractModelName(modelPath: String?): String {
        if (modelPath == null) return "Unknown"

        val fileName = File(modelPath).nameWithoutExtension
        return fileName
            .replace("-", " ")
            .replace("_", " ")
            .split(" ")
            .joinToString(" ") { it.replaceFirstChar { char -> char.uppercase() } }
    }

    /**
     * Estimate memory usage
     */
    private fun estimateMemoryUsage(): Long {
        val config = currentConfig ?: return 0L

        val modelFile = File(config.modelPath)
        if (!modelFile.exists()) return 0L

        // Model size + ~200MB runtime overhead
        val modelSizeMB = modelFile.length() / (1024 * 1024)
        val runtimeOverheadMB = 200L

        return modelSizeMB + runtimeOverheadMB
    }

    /**
     * Detect language from text
     *
     * @param text Input text
     * @return Detected language and confidence score
     */
    fun detectLanguage(text: String): Pair<com.augmentalis.llm.Language, Float> {
        return languageDetector.detectWithConfidence(text)
    }

    /**
     * Get recommended model for text
     *
     * Uses language detection to recommend the best model.
     *
     * @param text Input text
     * @return Recommended model ID
     */
    fun getRecommendedModel(text: String): String {
        return modelSelector.selectBestModel(text, preferredModelId = null)
    }

    /**
     * Get all available models
     *
     * @return List of model information with download status
     */
    fun getAvailableModels(): List<com.augmentalis.llm.ModelInfo> {
        return modelSelector.getAvailableModels()
    }

    /**
     * Get recommended models for a specific language
     *
     * @param language Target language
     * @return List of recommended models (ranked)
     */
    fun getRecommendedModelsForLanguage(
        language: com.augmentalis.llm.Language
    ): List<com.augmentalis.llm.ModelInfo> {
        return modelSelector.getRecommendedModelsForLanguage(language)
    }

    /**
     * Switch to a different model
     *
     * Unloads current model and loads the new one.
     *
     * @param modelId Model identifier
     * @return LLMResult.Success if switch successful, LLMResult.Error otherwise
     */
    suspend fun switchModel(modelId: String): LLMResult<Unit> {
        return try {
            Timber.i("Switching model to: $modelId")
            val startTime = System.currentTimeMillis()

            // 1. Save current engine state (for rollback)
            val previousModelId = currentModelId
            val previousEngine = alcEngine
            val previousConfig = currentConfig

            // 2. Get new model info
            val modelInfo = modelSelector.getModelInfo(modelId)
                ?: return LLMResult.Error(
                    message = "Model not found: $modelId",
                    cause = IllegalArgumentException("Model not found: $modelId")
                )

            val config = LLMConfig(
                modelPath = modelInfo.huggingFaceRepo,
                modelLib = modelId,
                device = currentConfig?.device ?: "opencl"
            )

            // 3. Initialize new engine (keeps old one running)
            val initResult = initialize(config)

            if (initResult is LLMResult.Success) {
                // 4. Clean up old engine AFTER new one is ready
                previousEngine?.cleanup()

                currentModelId = modelId

                val switchTime = System.currentTimeMillis() - startTime
                latencyMetrics.recordModelSwitch(switchTime)

                Timber.i("Model switched successfully in ${switchTime}ms: $previousModelId -> $modelId")
                Timber.i(latencyMetrics.getSummary())

                LLMResult.Success(Unit)
            } else {
                // 5. Rollback on failure
                Timber.w("Model switch failed, rolling back to $previousModelId")
                alcEngine = previousEngine
                currentModelId = previousModelId
                currentConfig = previousConfig

                val error = "Model switch failed, rolled back to $previousModelId"
                latencyMetrics.recordError(error)

                initResult
            }

        } catch (e: Exception) {
            val error = "Failed to switch model: ${e.message}"
            Timber.e(e, error)
            latencyMetrics.recordError(error)
            LLMResult.Error(
                message = "Model switch failed: ${e.message}",
                cause = e
            )
        }
    }

    /**
     * Check if model switching is recommended for given text
     *
     * @param text Input text
     * @return true if switching would improve language support, false otherwise
     */
    fun shouldSwitchModel(text: String): Boolean {
        val modelId = currentModelId ?: return false
        return modelSelector.shouldSwitchModel(modelId, text)
    }

    /**
     * Enable or disable automatic model selection
     *
     * When enabled, the provider will suggest model switches when
     * detecting a language change that's better supported by another model.
     *
     * @param enabled true to enable, false to disable
     */
    private var _autoModelSelection = autoModelSelection
    fun setAutoModelSelection(enabled: Boolean) {
        _autoModelSelection = enabled
        Timber.d("Auto-model selection ${if (enabled) "enabled" else "disabled"}")
    }

    /**
     * Set screen context for dynamic system prompts
     *
     * Updates the system prompt based on the current screen/activity.
     * This allows context-aware responses tailored to what the user is doing.
     *
     * @param screenContext Current screen context
     */
    fun setScreenContext(screenContext: ScreenContext) {
        this.currentScreenContext = screenContext
        Timber.d("Screen context updated: $screenContext")
    }

    /**
     * Set user context for personalized system prompts
     *
     * Updates the system prompt with user-specific information like
     * name, language preference, and expertise level.
     *
     * @param userContext User context
     */
    fun setUserContext(userContext: UserContext) {
        this.currentUserContext = userContext
        Timber.d("User context updated: name=${userContext.name}, language=${userContext.language}")
    }

    /**
     * Build system prompt with current context
     *
     * Creates a system prompt that includes:
     * - Base identity and guidelines
     * - Current date/time
     * - Screen-specific context
     * - User-specific personalization
     *
     * @param customInstructions Optional custom instructions to add
     * @return Complete system prompt
     */
    fun buildSystemPrompt(customInstructions: String? = null): String {
        return if (currentScreenContext != null || currentUserContext != null) {
            // Use context-aware prompt
            systemPromptManager.buildContextAwarePrompt(
                screenContext = currentScreenContext,
                userContext = currentUserContext
            )
        } else {
            // Use standard prompt
            systemPromptManager.buildSystemPrompt(
                customInstructions = customInstructions
            )
        }
    }

    /**
     * Format user message with system prompt
     *
     * Prepends system prompt to user message in the model-specific format.
     * The system prompt is hidden from the user but guides the model's behavior.
     *
     * @param userMessage User's input text
     * @param customSystemPrompt Optional custom system prompt (overrides default)
     * @return Formatted message ready for tokenization
     */
    fun formatWithSystemPrompt(
        userMessage: String,
        customSystemPrompt: String? = null
    ): String {
        val modelId = currentModelId ?: "gemma-2b-it-q4f16_1" // Default
        val systemPrompt = customSystemPrompt ?: buildSystemPrompt()

        return systemPromptManager.formatWithSystemPrompt(
            userMessage = userMessage,
            modelId = modelId,
            systemPrompt = systemPrompt
        )
    }

    /**
     * Generate response with system prompt
     *
     * Convenience method that automatically prepends system prompt
     * before generating a response.
     *
     * @param userMessage User's input text
     * @param options Generation options
     * @param customSystemPrompt Optional custom system prompt
     * @return Flow of response chunks
     */
    suspend fun generateWithSystemPrompt(
        userMessage: String,
        options: GenerationOptions = GenerationOptions(),
        customSystemPrompt: String? = null
    ): Flow<LLMResponse> {
        // Format message with system prompt
        val formattedMessage = formatWithSystemPrompt(
            userMessage = userMessage,
            customSystemPrompt = customSystemPrompt
        )

        Timber.d("Generating with system prompt (${formattedMessage.length} chars)")

        // Generate response using formatted message
        return generateResponse(formattedMessage, options)
    }

    /**
     * Chat with system prompt and conversation history
     *
     * Handles multi-turn conversations with system prompt properly
     * formatted for the current model.
     *
     * @param userMessage Current user message
     * @param conversationHistory Previous messages (without system prompt)
     * @param options Generation options
     * @param customSystemPrompt Optional custom system prompt
     * @return Flow of response chunks
     */
    suspend fun chatWithSystemPrompt(
        userMessage: String,
        conversationHistory: List<ChatMessage> = emptyList(),
        options: GenerationOptions = GenerationOptions(),
        customSystemPrompt: String? = null
    ): Flow<LLMResponse> {
        val systemPrompt = customSystemPrompt ?: buildSystemPrompt()

        // Build messages list with system prompt
        val messages = mutableListOf<ChatMessage>()

        // Add system prompt as first message
        messages.add(ChatMessage(
            role = MessageRole.SYSTEM,
            content = systemPrompt
        ))

        // Add conversation history
        messages.addAll(conversationHistory)

        // Add current user message
        messages.add(ChatMessage(
            role = MessageRole.USER,
            content = userMessage
        ))

        Timber.d("Chat with ${messages.size} messages (includes system prompt)")

        // Generate response
        return chat(messages, options)
    }

    /**
     * Get current system prompt
     *
     * Returns the system prompt that would be used for the next generation.
     * Useful for debugging or displaying to users.
     *
     * @return Current system prompt
     */
    fun getCurrentSystemPrompt(): String {
        return buildSystemPrompt()
    }

    // ==================== Model Path Resolution Helpers ====================

    /**
     * Resolve model path to a valid directory
     *
     * Tries to resolve the provided path (absolute or relative) to an existing
     * directory. Returns null if the path doesn't exist.
     *
     * @param modelPath Path from config (absolute or relative)
     * @return File object if directory exists, null otherwise
     */
    private fun resolveModelPath(modelPath: String): File? {
        // Try absolute path first
        if (modelPath.startsWith("/")) {
            val file = File(modelPath)
            if (file.exists() && file.isDirectory) {
                Timber.d("Found model at absolute path: ${file.absolutePath}")
                return file
            }
        }

        // Try relative to external files directory
        val baseDir = context.getExternalFilesDir(null) ?: context.filesDir
        val relativeFile = File(baseDir, modelPath)
        if (relativeFile.exists() && relativeFile.isDirectory) {
            Timber.d("Found model at relative path: ${relativeFile.absolutePath}")
            return relativeFile
        }

        // Try standard /sdcard location
        val sdcardFile = File("/sdcard/ava-ai-models/llm", modelPath)
        if (sdcardFile.exists() && sdcardFile.isDirectory) {
            Timber.d("Found model at sdcard path: ${sdcardFile.absolutePath}")
            return sdcardFile
        }

        Timber.w("Model path not found: $modelPath")
        return null
    }

    /**
     * Validate that a model directory contains required files
     *
     * Checks for essential model files:
     * - tokenizer.model OR tokenizer.json OR tokenizer.ats/.ath (tokenizer)
     * - mlc-chat-config.json OR .amc OR AVALibrary.adm/.ADco (model config/library)
     * - Or GGUF/LiteRT format indicators (manifest.json, .gguf, .task files)
     *
     * Supports AVA 3-character extension scheme v2.0:
     * - .adm = MLC device code, .ADco = legacy
     * - .ats = SentencePiece tokenizer, .ath = HuggingFace tokenizer
     * - .amc = AVA Model Config (replaces mlc-chat-config.json)
     *
     * NOTE: GGUF models have embedded tokenizers, so external tokenizer files
     * are NOT required for GGUF format.
     *
     * @param modelDir Directory to validate
     * @return true if directory contains required files
     */
    private fun isValidModelDirectory(modelDir: File): Boolean {
        if (!modelDir.exists() || !modelDir.isDirectory) {
            return false
        }

        // Check for tokenizer (v2.0: .ats/.ath, legacy: tokenizer.model/tokenizer.json)
        val hasTokenizer = File(modelDir, "tokenizer.model").exists() ||
            File(modelDir, "tokenizer.json").exists() ||
            File(modelDir, "tokenizer.ats").exists() ||
            File(modelDir, "tokenizer.ath").exists() ||
            modelDir.listFiles()?.any {
                it.name.endsWith(".ats") || it.name.endsWith(".ath")
            } == true

        // Check for MLC model config/library (v2.0: .adm/.amc, legacy: .ADco)
        // .amc = AVA Model Config (JSON config for GGUF models)
        val hasMlcConfig = File(modelDir, "mlc-chat-config.json").exists() ||
            File(modelDir, "AVALibrary.adm").exists() ||
            File(modelDir, "AVALibrary.ADco").exists() ||
            modelDir.listFiles()?.any {
                it.name.endsWith(".adm") || it.name.endsWith(".ADco") || it.name.endsWith(".amc")
            } == true

        // Check for GGUF format (.gguf files or manifest.json with format=AMG)
        val hasGgufFormat = modelDir.listFiles()?.any {
            it.name.endsWith(".gguf", ignoreCase = true)
        } == true

        // Check for LiteRT format (.task files or manifest.json with format=AMR)
        val hasLiteRtFormat = modelDir.listFiles()?.any {
            it.name.endsWith(".task", ignoreCase = true) ||
            it.name.endsWith(".tflite", ignoreCase = true)
        } == true

        // GGUF models have embedded tokenizers - they don't need external tokenizer files
        // Valid GGUF: has .gguf file + config (.amc or mlc-chat-config.json)
        val isValidGguf = hasGgufFormat && hasMlcConfig

        // Valid MLC: has tokenizer AND MLC config
        val isValidMlc = hasTokenizer && hasMlcConfig

        // Valid LiteRT: has tokenizer AND LiteRT files
        val isValidLiteRt = hasTokenizer && hasLiteRtFormat

        // Also valid for GGUF/LiteRT without explicit tokenizer if manifest exists
        val hasManifest = File(modelDir, "manifest.json").exists()
        val isValidAlternate = hasManifest && (hasGgufFormat || hasLiteRtFormat)

        val result = isValidGguf || isValidMlc || isValidLiteRt || isValidAlternate

        if (!result) {
            Timber.w("Invalid model directory: $modelDir (tokenizer=$hasTokenizer, mlc=$hasMlcConfig, gguf=$hasGgufFormat, litert=$hasLiteRtFormat)")
        }

        return result
    }

    /**
     * Check if directory contains a LiteRT model (.tflite)
     */
    private fun isLiteRTModelDirectory(directory: File): Boolean {
        if (!directory.exists() || !directory.isDirectory) return false
        
        // Check for .tflite file
        return directory.listFiles()?.any { 
            it.name.endsWith(".tflite", ignoreCase = true) 
        } ?: false
    }

    /**
     * Check if model directory contains GGUF format model
     *
     * @param modelDir Directory to check
     * @return true if directory contains .gguf files
     */
    private fun isGgufModelDirectory(modelDir: File): Boolean {
        if (!modelDir.exists() || !modelDir.isDirectory) {
            return false
        }

        return modelDir.listFiles()?.any {
            it.name.endsWith(".gguf", ignoreCase = true)
        } == true
    }

    // ==================== Device-Based Model Selection ====================

    /**
     * Auto-detect device and get recommended model configuration
     *
     * Uses AVA-DEVICE-MODEL-MATRIX.md recommendations to select the optimal
     * model configuration based on device capabilities.
     *
     * @param preferMultilingual Prefer multilingual over English-focused models
     * @return Recommended configuration, or null if no compatible models installed
     */
    suspend fun getDeviceRecommendedConfiguration(
        preferMultilingual: Boolean = false
    ): ModelConfiguration? {
        // Detect device if not already done
        if (detectedDevice == null) {
            detectedDevice = deviceModelSelector.detectDevice()
            Timber.i("Device detected: ${detectedDevice?.displayName}")
        }

        val device = detectedDevice ?: return null

        // Get recommended configuration
        val config = deviceModelSelector.getRecommendedConfiguration(device, preferMultilingual)

        if (config != null) {
            Timber.i("Recommended configuration for ${device.displayName}:")
            Timber.i("  Profile: ${config.id}")
            Timber.i("  LLM: ${config.llmModel}")
            Timber.i("  NLU: ${config.nluModel}")
            Timber.i("  ROI: ${config.getRoiDisplay()}")
            Timber.i("  Memory: ${config.totalMemoryGB}GB")
            Timber.i("  Speed: ${config.estimatedSpeed}")
        }

        return config
    }

    /**
     * Get detected device profile
     *
     * @return Detected device profile (auto-detects if not already done)
     */
    fun getDetectedDevice(): DeviceProfile {
        if (detectedDevice == null) {
            detectedDevice = deviceModelSelector.detectDevice()
        }
        return detectedDevice!!
    }

    /**
     * Get all configurations compatible with detected device
     *
     * Returns all model configurations that are compatible with the current device,
     * sorted by ROI score (best first).
     *
     * @return List of compatible configurations
     */
    fun getDeviceCompatibleConfigurations(): List<ModelConfiguration> {
        val device = getDetectedDevice()
        return deviceModelSelector.getConfigurationsForDevice(device)
    }

    /**
     * Get available configurations (models actually installed)
     *
     * Returns only configurations where the required LLM model is installed.
     *
     * @return List of available configurations
     */
    suspend fun getAvailableDeviceConfigurations(): List<ModelConfiguration> {
        val device = getDetectedDevice()
        return deviceModelSelector.getAvailableConfigurations(device)
    }

    /**
     * Initialize with device-optimized configuration
     *
     * Auto-detects device, selects optimal model configuration, and initializes.
     * This is the recommended way to initialize for most use cases.
     *
     * @param preferMultilingual Prefer multilingual over English-focused models
     * @param deviceOverride Optional: Override auto-detected device
     * @return Result with configuration used, or error if no compatible models
     */
    suspend fun initializeWithDeviceOptimization(
        preferMultilingual: Boolean = false,
        deviceOverride: DeviceProfile? = null
    ): LLMResult<ModelConfiguration> {
        return try {
            // Use override or auto-detect
            val device = deviceOverride ?: getDetectedDevice()
            detectedDevice = device

            Timber.i("=== Device-Optimized Initialization ===")
            Timber.i("Device: ${device.displayName}")
            Timber.i("RAM: ${device.ramGB}GB")
            Timber.i("GPU: ${device.gpuType}")
            Timber.i("Mode: ${if (preferMultilingual) "Multilingual" else "English-Focused"}")

            // Get recommended configuration
            val config = deviceModelSelector.getRecommendedConfiguration(device, preferMultilingual)
                ?: return LLMResult.Error(
                    message = "No compatible models found for ${device.displayName}. " +
                        "Please install one of: ${getRequiredModelsForDevice(device)}",
                    cause = IllegalStateException("No compatible models installed")
                )

            Timber.i("Selected configuration: ${config.id}")
            Timber.i("  ${config.getRoiDisplay()}")
            Timber.i("  LLM: ${config.llmModel}")
            Timber.i("  Memory: ${config.totalMemoryGB}GB")

            // Build model path
            val modelPath = "/sdcard/ava-ai-models/llm/${config.llmModel}"

            // Initialize with selected model
            val llmConfig = LLMConfig(
                modelPath = modelPath,
                modelLib = config.llmModel,
                device = if (config.requiresMali) "opencl" else "opencl"  // TODO: Support different backends
            )

            val initResult = initialize(llmConfig)

            when (initResult) {
                is LLMResult.Success -> {
                    currentConfiguration = config
                    currentModelId = config.llmModel
                    Timber.i("✅ Device-optimized initialization complete")
                    LLMResult.Success(config)
                }
                is LLMResult.Error -> {
                    Timber.e("Failed to initialize with config ${config.id}: ${initResult.message}")
                    LLMResult.Error(
                        message = "Failed to load model ${config.llmModel}: ${initResult.message}",
                        cause = initResult.cause
                    )
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Device-optimized initialization failed")
            LLMResult.Error(
                message = "Initialization failed: ${e.message}",
                cause = e
            )
        }
    }

    /**
     * Switch to a specific device configuration
     *
     * Allows user to select a different configuration from the compatible list.
     *
     * @param configId Configuration ID (e.g., "navigator-base", "navigator-multilingual")
     * @return LLMResult with new configuration, or error if not available
     */
    suspend fun switchToConfiguration(configId: String): LLMResult<ModelConfiguration> {
        return try {
            val device = getDetectedDevice()
            val configs = deviceModelSelector.getConfigurationsForDevice(device)

            val config = configs.find { it.id == configId }
                ?: return LLMResult.Error(
                    message = "Configuration '$configId' not found for ${device.displayName}",
                    cause = IllegalArgumentException("Configuration not found: $configId")
                )

            // Check if runtime is available
            if (!config.runtimeAvailable) {
                return LLMResult.Error(
                    message = "${config.displayName} requires ${config.llmRuntime} runtime (not yet implemented)",
                    cause = UnsupportedOperationException("Runtime not available")
                )
            }

            // Check if model is installed
            if (!deviceModelSelector.isConfigurationAvailable(config)) {
                return LLMResult.Error(
                    message = "Model ${config.llmModel} is not installed. Please download it first.",
                    cause = java.io.FileNotFoundException("Model not installed")
                )
            }

            Timber.i("Switching to configuration: ${config.displayName}")

            // Build model path
            val modelPath = "/sdcard/ava-ai-models/llm/${config.llmModel}"

            // Initialize with selected model
            val llmConfig = LLMConfig(
                modelPath = modelPath,
                modelLib = config.llmModel,
                device = "opencl"
            )

            // Clean up existing engine
            alcEngine?.cleanup()

            val initResult = initialize(llmConfig)

            when (initResult) {
                is LLMResult.Success -> {
                    currentConfiguration = config
                    currentModelId = config.llmModel
                    Timber.i("✅ Switched to ${config.displayName}")
                    LLMResult.Success(config)
                }
                is LLMResult.Error -> {
                    LLMResult.Error(
                        message = "Failed to switch: ${initResult.message}",
                        cause = initResult.cause
                    )
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Configuration switch failed")
            LLMResult.Error(
                message = "Switch failed: ${e.message}",
                cause = e
            )
        }
    }

    /**
     * Get current device configuration
     *
     * @return Current configuration, or null if not initialized
     */
    fun getCurrentDeviceConfiguration(): ModelConfiguration? = currentConfiguration

    /**
     * Get required models for a device
     *
     * Returns list of model IDs that would work on this device.
     *
     * @param device Target device
     * @return Comma-separated list of model IDs
     */
    private fun getRequiredModelsForDevice(device: DeviceProfile): String {
        return deviceModelSelector.getConfigurationsForDevice(device)
            .filter { it.runtimeAvailable }
            .map { it.llmModel }
            .distinct()
            .joinToString(", ")
    }

    /**
     * Get ROI summary for all supported devices
     *
     * Useful for documentation or admin interfaces.
     *
     * @return Map of device to best ROI configuration
     */
    fun getDeviceRoiSummary(): Map<DeviceProfile, ModelConfiguration?> {
        return deviceModelSelector.getRoiSummary()
    }

    /**
     * Set device override (for testing or manual selection)
     *
     * @param device Device profile to use instead of auto-detected
     */
    fun setDeviceOverride(device: DeviceProfile) {
        detectedDevice = device
        Timber.i("Device override set: ${device.displayName}")
    }

    /**
     * Clear device override (return to auto-detection)
     */
    fun clearDeviceOverride() {
        detectedDevice = null
        Timber.i("Device override cleared - will auto-detect on next request")
    }

    /**
     * Get working memory context from database
     *
     * Fetches recent "working" memories to provide conversation continuity.
     * Limits to last 5 items to fit in context window.
     */
    private suspend fun getWorkingMemoryContext(): String = withContext(Dispatchers.IO) {
        try {
            val database = DatabaseDriverFactory(context).createDriver().createDatabase()
            // Select recent working memories
            // Note: Using raw query logic via available 'memory queries'
            val memories = database.memoryQueries.selectByMemoryType("working").executeAsList()
                .take(5) // Last 5 items
                .sortedBy { it.created_at } // Chronological order

            if (memories.isEmpty()) return@withContext ""

            val builder = StringBuilder("RECENT MEMORY (Working Context):\n")
            memories.forEach { memory ->
                builder.append("- ${memory.content}\n")
            }
            builder.toString()
        } catch (e: Exception) {
            Timber.w(e, "Failed to load working memory context")
            ""
        }
    }

    // ==================== Command Interpretation (VoiceOS AI Integration) ====================

    /**
     * Interpret a voice command utterance using LLM
     *
     * Maps natural language utterances to available commands when NLU confidence is low.
     */
    override suspend fun interpretCommand(
        utterance: String,
        availableCommands: List<String>,
        context: String?
    ): CommandInterpretationResult {
        if (alcEngine == null && ggufStrategy == null) {
            return CommandInterpretationResult.Error("LLM not initialized")
        }

        val startTime = System.currentTimeMillis()

        try {
            // Build the interpretation prompt
            val prompt = buildCommandInterpretationPrompt(utterance, availableCommands, context)

            Timber.d("Command interpretation prompt: ${prompt.take(200)}...")

            // Generate response
            val responseBuilder = StringBuilder()
            generateResponse(prompt, GenerationOptions(
                temperature = 0.3f, // Lower temperature for more deterministic matching
                maxTokens = 200     // Short response expected
            )).collect { response ->
                when (response) {
                    is LLMResponse.Streaming -> responseBuilder.append(response.chunk)
                    is LLMResponse.Complete -> responseBuilder.append(response.fullText)
                    is LLMResponse.Error -> {
                        Timber.e("Interpretation error: ${response.message}")
                    }
                }
            }

            val llmResponse = responseBuilder.toString().trim()
            Timber.d("LLM interpretation response: $llmResponse")

            // Parse the response
            val result = parseCommandInterpretationResponse(llmResponse, availableCommands)

            val latency = System.currentTimeMillis() - startTime
            Timber.i("Command interpretation completed in ${latency}ms: ${result::class.simpleName}")

            return result

        } catch (e: Exception) {
            Timber.e(e, "Failed to interpret command")
            return CommandInterpretationResult.Error("Interpretation failed: ${e.message}")
        }
    }

    /**
     * Clarify a command when multiple candidates match
     */
    override suspend fun clarifyCommand(
        utterance: String,
        candidates: List<String>
    ): ClarificationResult {
        if (alcEngine == null && ggufStrategy == null) {
            return ClarificationResult(
                selectedCommand = null,
                confidence = 0f,
                clarificationQuestion = "Voice assistant is not ready. Please try again."
            )
        }

        val startTime = System.currentTimeMillis()

        try {
            // Build the clarification prompt
            val prompt = buildClarificationPrompt(utterance, candidates)

            Timber.d("Clarification prompt: ${prompt.take(200)}...")

            // Generate response
            val responseBuilder = StringBuilder()
            generateResponse(prompt, GenerationOptions(
                temperature = 0.3f,
                maxTokens = 150
            )).collect { response ->
                when (response) {
                    is LLMResponse.Streaming -> responseBuilder.append(response.chunk)
                    is LLMResponse.Complete -> responseBuilder.append(response.fullText)
                    is LLMResponse.Error -> {
                        Timber.e("Clarification error: ${response.message}")
                    }
                }
            }

            val llmResponse = responseBuilder.toString().trim()
            Timber.d("LLM clarification response: $llmResponse")

            // Parse the response
            val result = parseClarificationResponse(llmResponse, candidates)

            val latency = System.currentTimeMillis() - startTime
            Timber.i("Command clarification completed in ${latency}ms")

            return result

        } catch (e: Exception) {
            Timber.e(e, "Failed to clarify command")
            return ClarificationResult(
                selectedCommand = null,
                confidence = 0f,
                clarificationQuestion = "I had trouble understanding. Could you please repeat?"
            )
        }
    }

    /**
     * Build prompt for command interpretation
     */
    private fun buildCommandInterpretationPrompt(
        utterance: String,
        availableCommands: List<String>,
        context: String?
    ): String {
        val contextLine = if (context != null) "Context: $context\n" else ""
        val commandList = availableCommands.joinToString("\n") { "- $it" }

        return """You are a voice command interpreter. Given a user's spoken command, determine which available command best matches their intent.

$contextLine
Available commands:
$commandList

User said: "$utterance"

Instructions:
1. Analyze the user's intent
2. Match it to the most appropriate command from the list
3. Respond in this exact format:
   COMMAND: <command_name>
   CONFIDENCE: <0.0-1.0>
   REASONING: <brief explanation>

If no command matches, respond:
   COMMAND: NONE
   CONFIDENCE: 0.0
   REASONING: <why no match>

Response:"""
    }

    /**
     * Parse command interpretation response from LLM
     */
    private fun parseCommandInterpretationResponse(
        response: String,
        availableCommands: List<String>
    ): CommandInterpretationResult {
        try {
            // Extract COMMAND line
            val commandMatch = Regex("COMMAND:\\s*([^\\n]+)", RegexOption.IGNORE_CASE)
                .find(response)
            val commandStr = commandMatch?.groupValues?.get(1)?.trim() ?: ""

            // Extract CONFIDENCE line
            val confidenceMatch = Regex("CONFIDENCE:\\s*([0-9.]+)", RegexOption.IGNORE_CASE)
                .find(response)
            val confidence = confidenceMatch?.groupValues?.get(1)?.toFloatOrNull() ?: 0.5f

            // Extract REASONING line
            val reasoningMatch = Regex("REASONING:\\s*([^\\n]+)", RegexOption.IGNORE_CASE)
                .find(response)
            val reasoning = reasoningMatch?.groupValues?.get(1)?.trim()

            // Check for NONE / no match
            if (commandStr.equals("NONE", ignoreCase = true) || commandStr.isEmpty()) {
                return CommandInterpretationResult.NoMatch
            }

            // Verify command is in available list (case-insensitive match)
            val matchedCommand = availableCommands.find {
                it.equals(commandStr, ignoreCase = true)
            }

            return if (matchedCommand != null) {
                CommandInterpretationResult.Interpreted(
                    matchedCommand = matchedCommand,
                    confidence = confidence.coerceIn(0f, 1f),
                    reasoning = reasoning
                )
            } else {
                // LLM returned a command not in the list - try fuzzy match
                val fuzzyMatch = findFuzzyMatch(commandStr, availableCommands)
                if (fuzzyMatch != null) {
                    CommandInterpretationResult.Interpreted(
                        matchedCommand = fuzzyMatch,
                        confidence = (confidence * 0.8f).coerceIn(0f, 1f), // Reduce confidence for fuzzy match
                        reasoning = reasoning
                    )
                } else {
                    CommandInterpretationResult.NoMatch
                }
            }
        } catch (e: Exception) {
            Timber.w(e, "Failed to parse interpretation response: $response")
            return CommandInterpretationResult.Error("Parse error: ${e.message}")
        }
    }

    /**
     * Build prompt for command clarification
     */
    private fun buildClarificationPrompt(
        utterance: String,
        candidates: List<String>
    ): String {
        val candidateList = candidates.mapIndexed { index, cmd -> "${index + 1}. $cmd" }
            .joinToString("\n")

        return """You are a voice assistant helping to clarify a user's command. The user said something that could match multiple commands.

User said: "$utterance"

Possible matching commands:
$candidateList

Instructions:
1. Determine if you can confidently select one command
2. If confident, respond:
   SELECT: <command_name>
   CONFIDENCE: <0.7-1.0>
3. If unsure, create a clarifying question:
   SELECT: NONE
   CONFIDENCE: <0.0-0.6>
   QUESTION: <simple question to clarify user intent>

Keep questions natural and conversational. Example: "Would you like to open the camera app or take a photo?"

Response:"""
    }

    /**
     * Parse clarification response from LLM
     */
    private fun parseClarificationResponse(
        response: String,
        candidates: List<String>
    ): ClarificationResult {
        try {
            // Extract SELECT line
            val selectMatch = Regex("SELECT:\\s*([^\\n]+)", RegexOption.IGNORE_CASE)
                .find(response)
            val selectStr = selectMatch?.groupValues?.get(1)?.trim() ?: ""

            // Extract CONFIDENCE line
            val confidenceMatch = Regex("CONFIDENCE:\\s*([0-9.]+)", RegexOption.IGNORE_CASE)
                .find(response)
            val confidence = confidenceMatch?.groupValues?.get(1)?.toFloatOrNull() ?: 0.5f

            // Extract QUESTION line
            val questionMatch = Regex("QUESTION:\\s*([^\\n]+)", RegexOption.IGNORE_CASE)
                .find(response)
            val question = questionMatch?.groupValues?.get(1)?.trim()

            // Check if a command was selected
            if (selectStr.equals("NONE", ignoreCase = true) || selectStr.isEmpty()) {
                return ClarificationResult(
                    selectedCommand = null,
                    confidence = confidence.coerceIn(0f, 1f),
                    clarificationQuestion = question ?: generateDefaultClarificationQuestion(candidates)
                )
            }

            // Find matching command
            val matchedCommand = candidates.find {
                it.equals(selectStr, ignoreCase = true)
            } ?: findFuzzyMatch(selectStr, candidates)

            return if (matchedCommand != null && confidence >= 0.7f) {
                ClarificationResult(
                    selectedCommand = matchedCommand,
                    confidence = confidence.coerceIn(0f, 1f),
                    clarificationQuestion = null
                )
            } else {
                ClarificationResult(
                    selectedCommand = null,
                    confidence = confidence.coerceIn(0f, 1f),
                    clarificationQuestion = question ?: generateDefaultClarificationQuestion(candidates)
                )
            }
        } catch (e: Exception) {
            Timber.w(e, "Failed to parse clarification response: $response")
            return ClarificationResult(
                selectedCommand = null,
                confidence = 0f,
                clarificationQuestion = generateDefaultClarificationQuestion(candidates)
            )
        }
    }

    /**
     * Generate a default clarification question
     */
    private fun generateDefaultClarificationQuestion(candidates: List<String>): String {
        return when (candidates.size) {
            0 -> "I'm not sure what you'd like me to do. Could you please repeat?"
            1 -> "Did you mean ${formatCommandName(candidates[0])}?"
            2 -> "Did you want to ${formatCommandName(candidates[0])} or ${formatCommandName(candidates[1])}?"
            else -> {
                val firstTwo = candidates.take(2).joinToString(", ") { formatCommandName(it) }
                "Did you mean $firstTwo, or something else?"
            }
        }
    }

    /**
     * Format command name for display (snake_case to natural language)
     */
    private fun formatCommandName(command: String): String {
        return command
            .replace("_", " ")
            .replace("-", " ")
            .lowercase()
    }

    /**
     * Find fuzzy match for command name
     */
    private fun findFuzzyMatch(input: String, commands: List<String>): String? {
        val normalizedInput = input.lowercase().replace("_", "").replace("-", "").replace(" ", "")

        return commands.find { cmd ->
            val normalizedCmd = cmd.lowercase().replace("_", "").replace("-", "").replace(" ", "")
            normalizedInput.contains(normalizedCmd) || normalizedCmd.contains(normalizedInput)
        }
    }
}

