package com.augmentalis.ava.features.nlu

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import timber.log.Timber

/**
 * Background worker for computing embeddings of new training examples.
 *
 * Runs with WorkManager constraints:
 * - Battery not low
 * - Exponential backoff on failure
 *
 * Computes BERT embedding using IntentClassifier's ONNX model,
 * then saves to database for future NLU matching.
 *
 * @see ADR-013: Self-Learning NLU with LLM-as-Teacher Architecture
 */
@HiltWorker
class EmbeddingComputeWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val intentClassifier: IntentClassifier
) : CoroutineWorker(context, params) {

    companion object {
        /** Input data key for utterance text */
        const val KEY_UTTERANCE = "utterance"

        /** Input data key for intent name */
        const val KEY_INTENT = "intent"

        /** Input data key for confidence score */
        const val KEY_CONFIDENCE = "confidence"

        /** Maximum retry attempts */
        const val MAX_ATTEMPTS = 3
    }

    override suspend fun doWork(): Result {
        val utterance = inputData.getString(KEY_UTTERANCE)
        val intent = inputData.getString(KEY_INTENT)
        val confidence = inputData.getFloat(KEY_CONFIDENCE, 0.8f)

        if (utterance.isNullOrBlank() || intent.isNullOrBlank()) {
            Timber.w("EmbeddingComputeWorker: Missing utterance or intent")
            return Result.failure()
        }

        return try {
            Timber.d("Computing embedding for: '$utterance' -> $intent")

            // Check if already exists (may have been computed elsewhere)
            val existing = intentClassifier.findEmbeddingByUtterance(utterance)
            if (existing != null) {
                Timber.d("Embedding already exists for '$utterance'")
                return Result.success()
            }

            // Compute embedding using BERT model
            val embedding = intentClassifier.computeEmbedding(utterance)

            if (embedding == null) {
                Timber.w("Failed to compute embedding - model not loaded")
                return if (runAttemptCount < MAX_ATTEMPTS) {
                    Result.retry()
                } else {
                    Result.failure()
                }
            }

            // Save embedding to database
            val saved = intentClassifier.saveTrainedEmbedding(
                utterance = utterance,
                intent = intent,
                embedding = embedding,
                source = if (runAttemptCount == 0) "llm_auto" else "llm_variation",
                confidence = confidence
            )

            if (saved) {
                Timber.i("Saved embedding for '$utterance' -> $intent (conf=$confidence)")
                Result.success()
            } else {
                Timber.w("Failed to save embedding to database")
                if (runAttemptCount < MAX_ATTEMPTS) Result.retry() else Result.failure()
            }
        } catch (e: Exception) {
            Timber.e(e, "Error in EmbeddingComputeWorker")
            if (runAttemptCount < MAX_ATTEMPTS) Result.retry() else Result.failure()
        }
    }
}
