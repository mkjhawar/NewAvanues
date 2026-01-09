/**
 * AndroidNluProcessor.kt - Android NLU Processor Implementation
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2026-01-09
 *
 * Android implementation of INluProcessor using IntentClassifier from Shared/NLU.
 * Uses BERT embeddings via ONNX Runtime for semantic intent classification.
 */
package com.augmentalis.voiceoscoreng.nlu

import android.content.Context
import com.augmentalis.nlu.IntentClassifier
import com.augmentalis.nlu.IntentClassification
import com.augmentalis.voiceoscoreng.common.QuantizedCommand
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicReference
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Android implementation of [INluProcessor] using IntentClassifier.
 *
 * Wraps the IntentClassifier from Shared/NLU module to provide
 * semantic intent classification using BERT embeddings.
 *
 * @param context Android application context
 * @param config NLU configuration
 */
class AndroidNluProcessor(
    private val context: Context,
    private val config: NluConfig = NluConfig.DEFAULT
) : INluProcessor {

    /** Thread-safe reference to intent classifier */
    private val classifierRef = AtomicReference<IntentClassifier?>(null)

    /** Thread-safe initialization state */
    private val initialized = AtomicBoolean(false)

    /** Thread-safe initialization failure tracking */
    private val initializationFailed = AtomicBoolean(false)

    /** Mutex for initialization to prevent concurrent init calls */
    private val initMutex = Mutex()

    override suspend fun initialize(): Result<Unit> = initMutex.withLock {
        withContext(Dispatchers.IO) {
            // Already initialized successfully
            if (initialized.get()) {
                return@withContext Result.success(Unit)
            }

            // Already failed - don't retry
            if (initializationFailed.get()) {
                return@withContext Result.failure(
                    IllegalStateException("NLU initialization previously failed")
                )
            }

            if (!config.enabled) {
                println("[AndroidNluProcessor] NLU disabled in config")
                return@withContext Result.success(Unit)
            }

            try {
                // Get singleton instance of IntentClassifier
                val classifier = IntentClassifier.getInstance(context)

                if (classifier == null) {
                    initializationFailed.set(true)
                    println("[AndroidNluProcessor] IntentClassifier is null")
                    return@withContext Result.failure(IllegalStateException("IntentClassifier is null"))
                }

                // Initialize with default model path
                val initResult = classifier.initialize(config.modelPath)

                // Handle AVA Result type using isSuccess/isError properties
                if (initResult.isSuccess) {
                    classifierRef.set(classifier)
                    initialized.set(true)
                    println("[AndroidNluProcessor] NLU initialized successfully")
                    Result.success(Unit)
                } else {
                    // Result is error - extract exception using getOrThrow in try-catch
                    initializationFailed.set(true)
                    val errorMessage = try {
                        initResult.getOrThrow()
                        "Unknown error"
                    } catch (e: Throwable) {
                        e.message ?: "NLU initialization failed"
                    }
                    println("[AndroidNluProcessor] NLU initialization failed: $errorMessage")
                    Result.failure(IllegalStateException(errorMessage))
                }
            } catch (e: Exception) {
                initializationFailed.set(true)
                println("[AndroidNluProcessor] NLU initialization exception: ${e.message}")
                Result.failure(e)
            }
        }
    }

    override suspend fun classify(
        utterance: String,
        candidateCommands: List<QuantizedCommand>
    ): NluResult = withContext(Dispatchers.Default) {
        if (!config.enabled) {
            return@withContext NluResult.NoMatch
        }

        val classifier = classifierRef.get()
        if (classifier == null || !initialized.get()) {
            return@withContext NluResult.Error("NLU not initialized")
        }

        if (candidateCommands.isEmpty()) {
            return@withContext NluResult.NoMatch
        }

        // Extract phrases from commands for classification
        val candidateIntents = candidateCommands.map { it.phrase.lowercase() }

        try {
            val classifyResult = classifier.classifyIntent(utterance.lowercase(), candidateIntents)

            // Handle AVA Result type using isSuccess/isError properties
            if (classifyResult.isSuccess) {
                val classification = classifyResult.getOrNull()
                if (classification != null) {
                    processClassificationResult(classification, candidateCommands)
                } else {
                    NluResult.Error("Classification returned null")
                }
            } else {
                val errorMessage = try {
                    classifyResult.getOrThrow()
                    "Unknown classification error"
                } catch (e: Throwable) {
                    e.message ?: "Classification failed"
                }
                println("[AndroidNluProcessor] Classification failed: $errorMessage")
                NluResult.Error(errorMessage)
            }
        } catch (e: Exception) {
            println("[AndroidNluProcessor] Classification exception: ${e.message}")
            NluResult.Error(e.message ?: "Unknown error during classification")
        }
    }

    /**
     * Process classification result and map to NluResult.
     */
    private fun processClassificationResult(
        classification: IntentClassification,
        candidateCommands: List<QuantizedCommand>
    ): NluResult {
        // Find the matched command
        val matchedCommand = candidateCommands.find { cmd ->
            cmd.phrase.equals(classification.intent, ignoreCase = true)
        }

        // Check confidence threshold
        if (classification.confidence >= config.confidenceThreshold && matchedCommand != null) {
            return NluResult.Match(
                command = matchedCommand,
                confidence = classification.confidence,
                intent = classification.intent
            )
        }

        // Check for ambiguous matches (multiple high-scoring candidates)
        val highScoreCandidates = classification.allScores
            .filter { (_, score) -> score >= config.confidenceThreshold * 0.7f }
            .mapNotNull { (intent, score) ->
                candidateCommands.find { it.phrase.equals(intent, ignoreCase = true) }
                    ?.let { it to score }
            }
            .sortedByDescending { it.second }

        if (highScoreCandidates.size > 1) {
            // Multiple candidates with similar scores - ambiguous
            val topScore = highScoreCandidates.first().second
            val closeMatches = highScoreCandidates.filter { it.second >= topScore * 0.9f }

            if (closeMatches.size > 1) {
                return NluResult.Ambiguous(closeMatches)
            }
        }

        // Single match but below threshold - still return if close
        if (highScoreCandidates.isNotEmpty() && classification.confidence >= config.confidenceThreshold * 0.8f) {
            val (cmd, score) = highScoreCandidates.first()
            return NluResult.Match(
                command = cmd,
                confidence = score,
                intent = classification.intent
            )
        }

        // No match found
        return NluResult.NoMatch
    }

    override fun isAvailable(): Boolean =
        initialized.get() && config.enabled && classifierRef.get() != null

    /**
     * Check if initialization failed.
     * Useful for distinguishing "not initialized yet" from "failed to initialize".
     */
    fun isInitializationFailed(): Boolean = initializationFailed.get()

    override suspend fun dispose() {
        classifierRef.get()?.close()
        classifierRef.set(null)
        initialized.set(false)
        initializationFailed.set(false)
    }
}
