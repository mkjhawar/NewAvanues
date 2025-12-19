package com.augmentalis.nlu

import androidx.work.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.security.MessageDigest
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Orchestrates NLU self-learning from LLM classifications.
 *
 * When LLM processes a query:
 * 1. Saves the utterance + intent to TrainExample table
 * 2. Optionally saves variations (if high confidence)
 * 3. Schedules background embedding computation
 * 4. Updates NLU cache for immediate recognition
 *
 * Learning sources tracked:
 * - user: Manual user teaching via Teach AVA UI
 * - llm_auto: Automatic LLM classification
 * - llm_variation: LLM-generated phrase variations
 * - llm_confirmed: LLM classification confirmed by user
 *
 * @see ADR-013: Self-Learning NLU with LLM-as-Teacher Architecture
 */
@Singleton
class NLUSelfLearner @Inject constructor(
    private val intentClassifier: IntentClassifier,
    private val workManager: WorkManager
) {
    companion object {
        /** Minimum confidence to save variations */
        const val VARIATION_CONFIDENCE_THRESHOLD = 0.85f

        /** Minimum confidence to save at all */
        const val MIN_CONFIDENCE_THRESHOLD = 0.60f

        /** Maximum utterance length to save */
        const val MAX_UTTERANCE_LENGTH = 500

        /** Work tag for embedding computation jobs */
        const val WORK_TAG_EMBEDDING = "embedding_compute"

        /** Intents that should not be auto-taught */
        val EXCLUDED_INTENTS = setOf(
            "unknown",
            "teach_ava",
            "clarify_request",
            "general_question" // Too broad to teach effectively
        )
    }

    /**
     * Input data for learning from LLM classification.
     * This is a simple data class that avoids coupling NLU module to LLM module.
     */
    data class LLMTeachingInput(
        val intent: String,
        val confidence: Float,
        val variations: List<String> = emptyList()
    ) {
        /** Check if this input is valid for teaching the NLU */
        fun isValidForTeaching(): Boolean {
            return intent.isNotBlank() &&
                   intent != "unknown" &&
                   intent != "clarify_request" &&
                   confidence >= MIN_CONFIDENCE_THRESHOLD
        }
    }

    /**
     * Learn from LLM classification result.
     *
     * @param utterance Original user input
     * @param intent The classified intent name
     * @param confidence Confidence score 0.0-1.0
     * @param variations Optional phrase variations
     * @return true if learning was successful
     */
    suspend fun learnFromLLM(
        utterance: String,
        intent: String,
        confidence: Float,
        variations: List<String> = emptyList()
    ): Boolean = learnFromLLM(utterance, LLMTeachingInput(intent, confidence, variations))

    /**
     * Learn from LLM classification result.
     *
     * @param utterance Original user input
     * @param input Parsed LLM teaching input
     * @return true if learning was successful
     */
    suspend fun learnFromLLM(
        utterance: String,
        input: LLMTeachingInput
    ): Boolean = withContext(Dispatchers.IO) {
        // Validate input
        if (!input.isValidForTeaching()) {
            Timber.d("Input not valid for teaching: confidence=${input.confidence}")
            return@withContext false
        }

        // Check excluded intents
        if (input.intent in EXCLUDED_INTENTS) {
            Timber.d("Intent '${input.intent}' excluded from auto-teaching")
            return@withContext false
        }

        // Check utterance length
        if (utterance.length > MAX_UTTERANCE_LENGTH) {
            Timber.d("Utterance too long: ${utterance.length} chars")
            return@withContext false
        }

        // Check confidence threshold
        if (input.confidence < MIN_CONFIDENCE_THRESHOLD) {
            Timber.d("Confidence too low: ${input.confidence}")
            return@withContext false
        }

        try {
            // 1. Check if already exists
            val existingEmbedding = intentClassifier.findEmbeddingByUtterance(utterance)
            if (existingEmbedding != null) {
                Timber.d("Utterance already in database: '$utterance'")
                return@withContext false
            }

            // 2. Save primary utterance and compute embedding immediately
            val embedding = intentClassifier.computeEmbedding(utterance)
            if (embedding != null) {
                intentClassifier.saveTrainedEmbedding(
                    utterance = utterance,
                    intent = input.intent,
                    embedding = embedding,
                    source = "llm_auto",
                    confidence = input.confidence
                )
                Timber.i("Learned immediately: '$utterance' -> ${input.intent}")
            } else {
                // Schedule for later if model not ready
                scheduleEmbeddingComputation(utterance, input.intent, input.confidence)
            }

            // 3. Save variations if high confidence
            if (input.confidence >= VARIATION_CONFIDENCE_THRESHOLD && input.variations.isNotEmpty()) {
                input.variations.forEach { variation ->
                    if (variation.isNotBlank() && variation.length <= MAX_UTTERANCE_LENGTH) {
                        // Check not duplicate
                        val varExists = intentClassifier.findEmbeddingByUtterance(variation)
                        if (varExists == null) {
                            scheduleEmbeddingComputation(
                                utterance = variation,
                                intent = input.intent,
                                confidence = input.confidence * 0.9f // Slightly lower for generated
                            )
                        }
                    }
                }
                Timber.d("Scheduled ${input.variations.size} variations for learning")
            }

            true
        } catch (e: Exception) {
            Timber.e(e, "Failed to learn from LLM")
            false
        }
    }

    /**
     * Schedule background embedding computation using WorkManager.
     *
     * Constraints:
     * - Battery not low
     * - Exponential backoff on failure
     */
    private fun scheduleEmbeddingComputation(
        utterance: String,
        intent: String,
        confidence: Float
    ) {
        val constraints = Constraints.Builder()
            .setRequiresBatteryNotLow(true)
            .build()

        val inputData = workDataOf(
            EmbeddingComputeWorker.KEY_UTTERANCE to utterance,
            EmbeddingComputeWorker.KEY_INTENT to intent,
            EmbeddingComputeWorker.KEY_CONFIDENCE to confidence
        )

        val workRequest = OneTimeWorkRequestBuilder<EmbeddingComputeWorker>()
            .setConstraints(constraints)
            .setInputData(inputData)
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                WorkRequest.MIN_BACKOFF_MILLIS,
                TimeUnit.MILLISECONDS
            )
            .addTag(WORK_TAG_EMBEDDING)
            .build()

        // Issue I-05: Use MD5 hash instead of hashCode to avoid collisions
        val md5Hash = MessageDigest.getInstance("MD5")
            .digest(utterance.toByteArray())
            .joinToString("") { "%02x".format(it) }

        workManager.enqueueUniqueWork(
            "embedding_$md5Hash",
            ExistingWorkPolicy.KEEP, // Don't duplicate
            workRequest
        )

        Timber.d("Scheduled embedding computation for '$utterance' -> $intent")
    }

    /**
     * Confirm an LLM-classified intent (user feedback).
     * Increases confidence and changes source to confirmed.
     *
     * @param utterance The utterance to confirm
     */
    suspend fun confirmIntent(utterance: String) = withContext(Dispatchers.IO) {
        try {
            intentClassifier.confirmTrainedEmbedding(utterance)
            Timber.i("User confirmed: '$utterance'")
        } catch (e: Exception) {
            Timber.e(e, "Failed to confirm intent")
        }
    }

    /**
     * Correct an LLM-classified intent (user feedback).
     * Updates the intent and recomputes embedding.
     *
     * @param utterance The utterance to correct
     * @param correctIntent The correct intent
     */
    suspend fun correctIntent(
        utterance: String,
        correctIntent: String
    ) = withContext(Dispatchers.IO) {
        try {
            // Delete old embedding
            intentClassifier.deleteTrainedEmbedding(utterance)

            // Schedule new computation with correct intent
            scheduleEmbeddingComputation(
                utterance = utterance,
                intent = correctIntent,
                confidence = 1.0f // User-corrected = high confidence
            )

            Timber.i("User corrected: '$utterance' -> $correctIntent")
        } catch (e: Exception) {
            Timber.e(e, "Failed to correct intent")
        }
    }

    /**
     * Get learning statistics.
     *
     * @return LearningStats with counts by source
     */
    suspend fun getStats(): LearningStats = withContext(Dispatchers.IO) {
        try {
            val stats = intentClassifier.getLearningStats()
            LearningStats(
                totalExamples = stats.total,
                llmAutoTaught = stats.llmAuto,
                llmVariations = stats.llmVariation,
                userTaught = stats.user,
                userConfirmed = stats.confirmed
            )
        } catch (e: Exception) {
            Timber.e(e, "Failed to get stats")
            LearningStats(0, 0, 0, 0, 0)
        }
    }

    /**
     * Check if an utterance already exists in the database.
     *
     * @param utterance The utterance to check
     * @return True if exists
     */
    suspend fun hasExistingEmbedding(utterance: String): Boolean = withContext(Dispatchers.IO) {
        intentClassifier.findEmbeddingByUtterance(utterance) != null
    }

    /**
     * Clear all learned examples.
     * Use with caution - this deletes user training data.
     */
    suspend fun clearAllLearning() = withContext(Dispatchers.IO) {
        try {
            intentClassifier.clearAllTrainedEmbeddings()
            Timber.w("Cleared all learned examples")
        } catch (e: Exception) {
            Timber.e(e, "Failed to clear learning")
        }
    }

    /**
     * Learning statistics data class.
     */
    data class LearningStats(
        val totalExamples: Int,
        val llmAutoTaught: Int,
        val llmVariations: Int,
        val userTaught: Int,
        val userConfirmed: Int
    ) {
        /** Percentage of examples from automatic LLM teaching */
        val autoLearnPercent: Int
            get() = if (totalExamples > 0) {
                ((llmAutoTaught + llmVariations) * 100) / totalExamples
            } else 0
    }
}
