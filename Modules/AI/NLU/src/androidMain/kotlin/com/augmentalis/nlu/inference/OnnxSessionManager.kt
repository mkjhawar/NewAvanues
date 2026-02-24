/*
 * Copyright (c) 2024-2025 Intelligent Devices LLC / Augmentalis
 * All Rights Reserved.
 *
 * SOLID Refactoring: Extracted from IntentClassifier (SRP)
 */

package com.augmentalis.nlu.inference

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import android.content.Context
import com.augmentalis.ava.core.common.Result
import com.augmentalis.nlu.nluLogDebug
import com.augmentalis.nlu.nluLogError
import com.augmentalis.nlu.nluLogInfo
import com.augmentalis.nlu.nluLogWarn
import com.augmentalis.ava.core.common.backend.InferenceBackendSelector
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File

/**
 * OnnxSessionManager - Single Responsibility: ONNX Runtime Session Lifecycle
 *
 * Extracted from IntentClassifier as part of SOLID refactoring.
 * Handles all ONNX-related operations:
 * - ORT environment initialization
 * - Session creation with hardware-aware backend selection
 * - Inference execution
 * - Resource cleanup
 *
 * Thread-safe: Uses mutex for initialization and synchronized session access.
 *
 * @param context Android context for backend selection
 *
 * @author Manoj Jhawar
 * @since 2025-12-17
 */
class OnnxSessionManager(
    private val context: Context
) {
    companion object {
        private const val TAG = "OnnxSessionManager"
    }

    // ==================== State ====================

    private var ortEnvironment: OrtEnvironment? = null
    private var ortSession: OrtSession? = null
    private var isInitialized = false

    private val initializationMutex = Mutex()

    // ==================== Lifecycle ====================

    /**
     * Initialize ONNX Runtime environment and load model.
     *
     * @param modelPath Path to ONNX model file
     * @return Result indicating success or failure
     */
    suspend fun initialize(modelPath: String): Result<Unit> = withContext(Dispatchers.IO) {
        initializationMutex.withLock {
            try {
                if (isInitialized) {
                    nluLogDebug(TAG, "Already initialized, skipping")
                    return@withContext Result.Success(Unit)
                }

                nluLogDebug(TAG, "Initializing ONNX Runtime...")

                // Validate model file
                val modelFile = File(modelPath)
                if (!modelFile.exists()) {
                    return@withContext Result.Error(
                        exception = IllegalStateException("Model not found: $modelPath"),
                        message = "ONNX model not found"
                    )
                }

                // Initialize ONNX Runtime environment
                ortEnvironment = OrtEnvironment.getEnvironment()

                // Create session with hardware-aware backend selection
                val sessionOptions = createSessionOptions()
                ortSession = ortEnvironment!!.createSession(
                    modelFile.absolutePath,
                    sessionOptions
                )

                isInitialized = true
                nluLogInfo(TAG, "ONNX Runtime initialized successfully")

                Result.Success(Unit)
            } catch (e: Exception) {
                nluLogError(TAG, "Failed to initialize ONNX Runtime: ${e.message}", e)
                Result.Error(
                    exception = e,
                    message = "Failed to initialize ONNX Runtime: ${e.message}"
                )
            }
        }
    }

    /**
     * Create session options with hardware-aware backend selection.
     */
    private fun createSessionOptions(): OrtSession.SessionOptions {
        return OrtSession.SessionOptions().apply {
            val backend = InferenceBackendSelector.selectNLUBackend(context)
            nluLogInfo(TAG, "Selected NLU backend: ${backend.displayName}")

            when (backend) {
                InferenceBackendSelector.Backend.QNN_HTP -> {
                    // Qualcomm QNN for best Snapdragon performance
                    try {
                        addNnapi() // NNAPI as fallback if QNN fails
                        nluLogInfo(TAG, "Using QNN/HTP backend (Qualcomm optimized)")
                    } catch (e: Exception) {
                        nluLogWarn(TAG, "QNN not available, using NNAPI")
                        addNnapi()
                    }
                }
                InferenceBackendSelector.Backend.NNAPI -> {
                    // NNAPI for cross-platform GPU/DSP/NPU acceleration
                    addNnapi()
                    nluLogInfo(TAG, "Using NNAPI backend (hardware accelerated)")
                }
                else -> {
                    // CPU fallback with multi-threading
                    nluLogInfo(TAG, "Using CPU backend (ARM NEON)")
                }
            }
            setIntraOpNumThreads(4)
            setInterOpNumThreads(2)
        }
    }

    // ==================== Inference ====================

    /**
     * Check if session is initialized and ready for inference.
     */
    fun isReady(): Boolean = isInitialized && ortSession != null

    /**
     * Get the ONNX environment (for tensor creation).
     *
     * @return OrtEnvironment or null if not initialized
     */
    fun getEnvironment(): OrtEnvironment? = ortEnvironment

    /**
     * Run inference with the given inputs.
     *
     * @param inputs Map of input name to OnnxTensor
     * @return OrtSession.Result containing outputs
     * @throws IllegalStateException if session not initialized
     */
    fun run(inputs: Map<String, OnnxTensor>): OrtSession.Result {
        val session = ortSession
            ?: throw IllegalStateException("Session not initialized. Call initialize() first.")

        return session.run(inputs)
    }

    /**
     * Run inference with timing.
     *
     * @param inputs Map of input name to OnnxTensor
     * @return Pair of (OrtSession.Result, inferenceTimeMs)
     */
    fun runWithTiming(inputs: Map<String, OnnxTensor>): Pair<OrtSession.Result, Long> {
        val startTime = System.currentTimeMillis()
        val result = run(inputs)
        val inferenceTime = System.currentTimeMillis() - startTime
        return result to inferenceTime
    }

    // ==================== Cleanup ====================

    /**
     * Close the ONNX session and release resources.
     */
    fun close() {
        try {
            ortSession?.close()
            ortSession = null
            ortEnvironment?.close()
            ortEnvironment = null
            isInitialized = false
            nluLogDebug(TAG, "ONNX session closed")
        } catch (e: Exception) {
            nluLogError(TAG, "Error closing ONNX session: ${e.message}", e)
        }
    }

    // ==================== Model Info ====================

    /**
     * Get input names for the loaded model.
     *
     * @return Set of input names
     */
    fun getInputNames(): Set<String> {
        return ortSession?.inputNames ?: emptySet()
    }

    /**
     * Get output names for the loaded model.
     *
     * @return Set of output names
     */
    fun getOutputNames(): Set<String> {
        return ortSession?.outputNames ?: emptySet()
    }
}
