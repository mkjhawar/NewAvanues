// filename: Modules/AVA/core/Domain/src/commonMain/kotlin/com/augmentalis/ava/core/domain/usecase/ClassifyIntentUseCase.kt
// created: 2025-12-18
// author: AVA AI Team
// © Augmentalis Inc, Intelligent Devices LLC

package com.augmentalis.ava.core.domain.usecase

import com.augmentalis.ava.core.common.Result

/**
 * Intent classification result
 *
 * Contains the classified intent, confidence score, and suggestions for training
 */
data class IntentClassificationResult(
    val intent: String?,
    val confidence: Float,
    val needsTraining: Boolean,
    val suggestedIntents: List<SuggestedIntent>,
    val message: String?,
    val inferenceTimeMs: Long = 0
)

/**
 * Suggested intent with confidence score
 */
data class SuggestedIntent(
    val intent: String,
    val confidence: Float
)

/**
 * Use case interface for intent classification.
 *
 * This is an interface in the Domain layer to enable different implementations:
 * - Modules/Shared/NLU provides the ONNX-based implementation
 * - Other implementations can be plugged in (cloud-based, rule-based, etc.)
 *
 * Dependency Inversion Principle: Domain defines the contract, implementations depend on it
 *
 * Implementation location: Modules/Shared/NLU/src/androidMain/kotlin/.../ClassifyIntentUseCase.kt
 */
interface ClassifyIntentUseCase {

    /**
     * Classify user utterance to detect intent
     *
     * @param utterance User input text
     * @param locale User locale (default "en-US")
     * @param confidenceThreshold Minimum confidence to accept (default 0.7)
     * @return Result containing classification with suggestions
     */
    suspend operator fun invoke(
        utterance: String,
        locale: String = "en-US",
        confidenceThreshold: Float = 0.7f
    ): Result<IntentClassificationResult>
}
