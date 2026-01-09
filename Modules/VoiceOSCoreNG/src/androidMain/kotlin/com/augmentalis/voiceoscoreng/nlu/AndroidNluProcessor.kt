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
import com.augmentalis.voiceoscoreng.common.QuantizedCommand
import com.augmentalis.ava.core.common.Result as AvaResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

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

    private var intentClassifier: IntentClassifier? = null
    private var isInitialized = false

    override suspend fun initialize(): Result<Unit> = withContext(Dispatchers.IO) {
        if (!config.enabled) {
            println("[AndroidNluProcessor] NLU disabled in config")
            return@withContext Result.success(Unit)
        }

        try {
            // Get singleton instance of IntentClassifier
            intentClassifier = IntentClassifier.getInstance(context)

            // Initialize with default model path
            val initResult = intentClassifier?.initialize(config.modelPath)

            when (initResult) {
                is AvaResult.Success -> {
                    isInitialized = true
                    println("[AndroidNluProcessor] NLU initialized successfully")
                    Result.success(Unit)
                }
                is AvaResult.Error -> {
                    println("[AndroidNluProcessor] NLU initialization failed: ${initResult.message}")
                    Result.failure(initResult.exception)
                }
                null -> {
                    println("[AndroidNluProcessor] NLU initialization returned null")
                    Result.failure(IllegalStateException("IntentClassifier is null"))
                }
            }
        } catch (e: Exception) {
            println("[AndroidNluProcessor] NLU initialization exception: ${e.message}")
            Result.failure(e)
        }
    }

    override suspend fun classify(
        utterance: String,
        candidateCommands: List<QuantizedCommand>
    ): NluResult = withContext(Dispatchers.Default) {
        val classifier = intentClassifier

        if (!config.enabled) {
            return@withContext NluResult.NoMatch
        }

        if (classifier == null || !isInitialized) {
            return@withContext NluResult.Error("NLU not initialized")
        }

        if (candidateCommands.isEmpty()) {
            return@withContext NluResult.NoMatch
        }

        // Extract phrases from commands for classification
        val candidateIntents = candidateCommands.map { it.phrase.lowercase() }

        try {
            val classifyResult = classifier.classifyIntent(utterance.lowercase(), candidateIntents)

            when (classifyResult) {
                is AvaResult.Success -> {
                    val classification = classifyResult.data
                    processClassificationResult(classification, candidateCommands)
                }
                is AvaResult.Error -> {
                    println("[AndroidNluProcessor] Classification failed: ${classifyResult.message}")
                    NluResult.Error(classifyResult.message ?: "Classification failed")
                }
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
        classification: com.augmentalis.nlu.IntentClassification,
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

    override fun isAvailable(): Boolean = isInitialized && config.enabled && intentClassifier != null

    override suspend fun dispose() {
        intentClassifier?.close()
        intentClassifier = null
        isInitialized = false
    }
}
