/**
 * Core ML Model Manager — Darwin (iOS + macOS) implementation.
 *
 * Handles Core ML model lifecycle for Apple platform NLU inference.
 * CoreML API surface is identical on iOS 14+ and macOS 11+.
 *
 * Compute backends:
 * - ANE: Apple Neural Engine (A12+ / Apple Silicon)
 * - GPU: Metal GPU compute
 * - CPU: Universal fallback
 * - Auto: Let Core ML decide (recommended)
 */

package com.augmentalis.nlu.coreml

import com.augmentalis.ava.core.common.Result
import com.augmentalis.nlu.nluLogDebug
import com.augmentalis.nlu.nluLogInfo
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

private const val TAG = "CoreMLModelManager"

@OptIn(ExperimentalForeignApi::class, kotlinx.cinterop.BetaInteropApi::class)
internal class CoreMLModelManager {

    enum class ComputeBackend {
        Auto,
        ANE,
        GPU,
        CPU
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
     * Load Core ML model from specified path.
     *
     * Supports both .mlmodel and .mlpackage bundle formats.
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
            nluLogInfo(TAG, "Loading model from: $modelPath")

            this.computeBackend = computeBackend

            modelUrl = NSURL.fileURLWithPath(modelPath)
                ?: return Result.Error(
                    exception = IllegalArgumentException("Invalid model path: $modelPath"),
                    message = "Failed to create URL from path"
                )

            val fileManager = NSFileManager.defaultManager
            if (!fileManager.fileExistsAtPath(modelPath)) {
                return Result.Error(
                    exception = IllegalStateException("Model file not found: $modelPath"),
                    message = "Model file does not exist at specified path"
                )
            }

            val config = configureModel(computeBackend)

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
            nluLogInfo(TAG, "Model loaded successfully")
            nluLogDebug(TAG, "Using compute backend: $computeBackend")

            return Result.Success(Unit)
        } catch (e: Exception) {
            return Result.Error(
                exception = e,
                message = "Failed to load Core ML model: ${e.message}"
            )
        }
    }

    private fun configureModel(computeBackend: ComputeBackend): MLModelConfiguration {
        val config = MLModelConfiguration()

        when (computeBackend) {
            ComputeBackend.ANE -> {
                config.computeUnits = MLComputeUnitsAll
                nluLogDebug(TAG, "Configured for Apple Neural Engine")
            }
            ComputeBackend.GPU -> {
                config.computeUnits = MLComputeUnitsCPUAndGPU
                nluLogDebug(TAG, "Configured for GPU + CPU fallback")
            }
            ComputeBackend.CPU -> {
                config.computeUnits = MLComputeUnitsCPUOnly
                nluLogDebug(TAG, "Configured for CPU only")
            }
            ComputeBackend.Auto -> {
                config.computeUnits = MLComputeUnitsAll
                nluLogDebug(TAG, "Configured for automatic backend selection")
            }
        }

        config.allowLowPrecisionAccumulationOnGPU = true

        return config
    }

    /**
     * Run inference on input tokens.
     *
     * CoreML tensor interop is not yet configured — returns explicit error
     * rather than a zero vector that downstream consumers would treat as valid.
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
        nluLogDebug(TAG, "Cleaned up resources")
    }
}
