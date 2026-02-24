/**
 * Core ML Model Manager - macOS implementation
 *
 * Handles Core ML model lifecycle for macOS-based NLU.
 * CoreML is available on macOS 10.15+ with the same API surface as iOS.
 *
 * Key differences from iOS:
 * - macOS has more compute resources (larger CPU/GPU)
 * - ANE availability varies by Apple Silicon generation
 * - Model files may come from app bundle or arbitrary filesystem paths
 */

package com.augmentalis.nlu.coreml

import com.augmentalis.ava.core.common.Result
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCObjectVar
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.value
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import platform.CoreML.*
import platform.Foundation.*

@OptIn(ExperimentalForeignApi::class, kotlinx.cinterop.BetaInteropApi::class)
internal class CoreMLModelManager {

    // Compute backend options
    enum class ComputeBackend {
        Auto,      // Let Core ML decide (recommended)
        ANE,       // Apple Neural Engine (Apple Silicon Macs)
        GPU,       // Metal GPU
        CPU        // CPU only (universal fallback)
    }

    private var model: MLModel? = null
    private var modelUrl: NSURL? = null
    private var computeBackend = ComputeBackend.Auto
    private var isLoaded = false

    // Performance tracking
    private var lastInferenceTimeMs = 0L
    private var totalInferencesCount = 0
    private var totalInferenceTimeMs = 0L

    /**
     * Load Core ML model from specified path
     *
     * Supports both .mlmodel and .mlpackage bundle formats.
     * On macOS, models can be loaded from arbitrary filesystem paths
     * (not limited to app bundle like iOS sandboxed apps).
     *
     * @param modelPath Path to .mlmodel or .mlpackage file
     * @param computeBackend Compute unit to use (default: Auto)
     * @return Result indicating success or failure
     */
    fun loadModel(
        modelPath: String,
        computeBackend: ComputeBackend = ComputeBackend.Auto
    ): Result<Unit> {
        try {
            println("CoreMLModelManager: Loading model from: $modelPath")

            this.computeBackend = computeBackend

            // Create URL from path
            modelUrl = NSURL.fileURLWithPath(modelPath)
                ?: return Result.Error(
                    exception = IllegalArgumentException("Invalid model path: $modelPath"),
                    message = "Failed to create URL from path"
                )

            // Check if file exists
            val fileManager = NSFileManager.defaultManager
            if (!fileManager.fileExistsAtPath(modelPath)) {
                return Result.Error(
                    exception = IllegalStateException("Model file not found: $modelPath"),
                    message = "Model file does not exist at specified path"
                )
            }

            // Load model configuration
            val config = configureModel(computeBackend)

            // Load the model with configuration
            model = memScoped {
                val error = alloc<ObjCObjectVar<NSError?>>()
                val loadedModel = MLModel.modelWithContentsOfURL(
                    url = modelUrl!!,
                    configuration = config,
                    error = error.ptr
                )

                if (loadedModel == null || error.value != null) {
                    val errorMsg = error.value?.localizedDescription ?: "Unknown error"
                    return Result.Error(
                        exception = Exception("Core ML model loading failed: $errorMsg"),
                        message = "Failed to load model: $errorMsg"
                    )
                }

                loadedModel
            }

            isLoaded = true
            println("CoreMLModelManager: Model loaded successfully")
            println("CoreMLModelManager: Using compute backend: $computeBackend")

            return Result.Success(Unit)
        } catch (e: Exception) {
            return Result.Error(
                exception = e,
                message = "Failed to load Core ML model: ${e.message}"
            )
        }
    }

    /**
     * Configure Core ML model with optimal settings for macOS
     */
    private fun configureModel(computeBackend: ComputeBackend): MLModelConfiguration {
        val config = MLModelConfiguration()

        when (computeBackend) {
            ComputeBackend.ANE -> {
                config.computeUnits = MLComputeUnitsAll
                println("CoreMLModelManager: Configured for Apple Neural Engine")
            }
            ComputeBackend.GPU -> {
                config.computeUnits = MLComputeUnitsCPUAndGPU
                println("CoreMLModelManager: Configured for GPU + CPU fallback")
            }
            ComputeBackend.CPU -> {
                config.computeUnits = MLComputeUnitsCPUOnly
                println("CoreMLModelManager: Configured for CPU only")
            }
            ComputeBackend.Auto -> {
                config.computeUnits = MLComputeUnitsAll
                println("CoreMLModelManager: Configured for automatic backend selection")
            }
        }

        config.allowLowPrecisionAccumulationOnGPU = true

        return config
    }

    /**
     * Run inference on input tokens
     *
     * @param inputIds Tokenized input IDs [batch_size=1, seq_length]
     * @param attentionMask Attention mask [1, seq_length]
     * @param tokenTypeIds Token type IDs [1, seq_length]
     * @return Result containing output embedding vector or error
     */
    suspend fun runInference(
        inputIds: LongArray,
        attentionMask: LongArray,
        tokenTypeIds: LongArray
    ): Result<FloatArray> = withContext(Dispatchers.Default) {
        if (!isLoaded || model == null) {
            return@withContext Result.Error(
                exception = IllegalStateException("Model not loaded"),
                message = "Call loadModel() first"
            )
        }

        // CoreML tensor interop is not yet configured — return explicit error
        // rather than a zero vector that downstream consumers would treat as valid embeddings.
        Result.Error(
            exception = UnsupportedOperationException(
                "CoreML inference not available — tensor interop not configured"
            ),
            message = "CoreML model loaded but inference requires MLMultiArray cinterop bindings. " +
                "Configure CoreML tensor interop to enable on-device NLU inference."
        )
    }

    fun getLastInferenceTimeMs(): Long = lastInferenceTimeMs

    fun getAverageInferenceTimeMs(): Long {
        return if (totalInferencesCount > 0) {
            totalInferenceTimeMs / totalInferencesCount
        } else {
            0L
        }
    }

    fun getInferenceCount(): Int = totalInferencesCount

    fun close() {
        model = null
        modelUrl = null
        isLoaded = false
        println("CoreMLModelManager: Cleaned up resources")
    }
}
