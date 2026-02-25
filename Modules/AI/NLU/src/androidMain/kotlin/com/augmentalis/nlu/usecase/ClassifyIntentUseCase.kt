package com.augmentalis.nlu.usecase

import com.augmentalis.ava.core.common.Result
import com.augmentalis.ava.core.domain.model.TrainExample
import com.augmentalis.ava.core.domain.repository.TrainExampleRepository
import com.augmentalis.nlu.IntentClassification
import com.augmentalis.nlu.IntentClassifier
import com.augmentalis.nlu.NluThresholds
import kotlinx.coroutines.flow.first

/**
 * Use case: Classify user intent using ONNX NLU + Teach-Ava training examples
 *
 * Flow:
 * 1. Get trained intents from TrainExampleRepository
 * 2. Run ONNX classifier with candidate intents
 * 3. If high confidence (>0.7), return intent
 * 4. If low confidence (<0.7), suggest training
 * 5. Increment usage count for matched training example
 */
class ClassifyIntentUseCase(
    private val intentClassifier: IntentClassifier,
    private val trainExampleRepository: TrainExampleRepository
) {

    /**
     * Classify user utterance
     *
     * @param utterance User input text
     * @param locale User locale (default "en-US")
     * @param confidenceThreshold Minimum confidence to accept (default 0.7)
     * @return Classification result with suggestions
     */
    suspend operator fun invoke(
        utterance: String,
        locale: String = "en-US",
        confidenceThreshold: Float = NluThresholds.CLASSIFY_ACCEPT_THRESHOLD
    ): Result<ClassificationResult> {
        try {
            // Get trained intents for locale
            val trainedExamples = trainExampleRepository
                .getExamplesForLocale(locale)
                .first()

            if (trainedExamples.isEmpty()) {
                return Result.Success(
                    ClassificationResult(
                        intent = null,
                        confidence = 0f,
                        needsTraining = true,
                        suggestedIntents = emptyList(),
                        message = "No training examples available. Please train AVA first."
                    )
                )
            }

            // Extract unique intents
            val candidateIntents = trainedExamples
                .map { it.intent }
                .distinct()

            // Run ONNX classifier
            val classificationResult = intentClassifier.classifyIntent(
                utterance = utterance,
                candidateIntents = candidateIntents
            )

            when (classificationResult) {
                is Result.Success -> {
                    val classification = classificationResult.data

                    // High confidence - accept classification
                    if (classification.confidence >= confidenceThreshold) {
                        // Increment usage count for matched training example
                        incrementMatchedExample(utterance, classification.intent)

                        return Result.Success(
                            ClassificationResult(
                                intent = classification.intent,
                                confidence = classification.confidence,
                                needsTraining = false,
                                suggestedIntents = getSuggestedIntents(classification),
                                message = null,
                                inferenceTimeMs = classification.inferenceTimeMs
                            )
                        )
                    }

                    // Low confidence - suggest training
                    return Result.Success(
                        ClassificationResult(
                            intent = classification.intent,
                            confidence = classification.confidence,
                            needsTraining = true,
                            suggestedIntents = getSuggestedIntents(classification),
                            message = "Low confidence. Consider training AVA with this example.",
                            inferenceTimeMs = classification.inferenceTimeMs
                        )
                    )
                }

                is Result.Error -> {
                    return Result.Error(
                        exception = classificationResult.exception,
                        message = "Classification failed: ${classificationResult.message}"
                    )
                }
            }
        } catch (e: Exception) {
            return Result.Error(
                exception = e,
                message = "Intent classification use case failed: ${e.message}"
            )
        }
    }

    /**
     * Increment usage count for matched training example
     */
    private suspend fun incrementMatchedExample(utterance: String, intent: String) {
        try {
            // Find closest matching training example
            val examples = trainExampleRepository.getExamplesForIntent(intent).first()
            val closestExample = examples.firstOrNull { example ->
                // Simple similarity check (could be improved with Levenshtein distance)
                example.utterance.lowercase() == utterance.lowercase()
            }

            closestExample?.let { example ->
                trainExampleRepository.incrementUsage(
                    id = example.id,
                    timestamp = System.currentTimeMillis()
                )
            }
        } catch (e: Exception) {
            // Non-critical, log and continue
        }
    }

    /**
     * Get top 3 suggested intents from all scores
     */
    private fun getSuggestedIntents(classification: IntentClassification): List<SuggestedIntent> {
        return classification.allScores
            .entries
            .sortedByDescending { it.value }
            .take(3)
            .map { (intent, score) ->
                SuggestedIntent(intent = intent, confidence = score)
            }
    }
}

/**
 * Result of intent classification
 */
data class ClassificationResult(
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
