package com.augmentalis.nlu.learning

import android.content.Context
import com.augmentalis.ava.core.data.db.AVADatabase
import com.augmentalis.nlu.nluLogDebug
import com.augmentalis.nlu.nluLogError
import com.augmentalis.nlu.nluLogInfo
import com.augmentalis.ava.core.data.db.DatabaseDriverFactory
import com.augmentalis.ava.core.data.db.createDatabase
import com.augmentalis.nlu.IntentClassifier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.security.MessageDigest

/**
 * Intent Learning Manager
 *
 * Learns new intents from LLM responses and stores them in the database for future NLU training.
 *
 * **How it works:**
 * 1. LLM responds to user message with intent hint: [INTENT: greeting] [CONFIDENCE: 95]
 * 2. Extract intent and confidence from LLM response
 * 3. If confidence >= threshold, learn the example
 * 4. Store in database with source="LLM_LEARNED"
 * 5. Re-compute embeddings for updated NLU
 *
 * **Example Flow:**
 * ```
 * User: "hello ava"
 * NLU: unknown (confidence 0.0)
 * LLM: "Hello! I'm AVA. How can I help you?" [INTENT: greeting] [CONFIDENCE: 95]
 * Learning: Add "hello ava" → "greeting" intent to database
 * Next time: "hello ava" recognized by NLU directly!
 * ```
 *
 * Created: 2025-11-17 (Phase 2)
 * Author: AVA AI Team
 */
class IntentLearningManager(
    private val context: Context
) {

    companion object {
        private const val TAG = "IntentLearningManager"

        /**
         * Minimum confidence from LLM to learn intent
         * Below this, ignore the hint (too uncertain)
         */
        private const val LEARNING_CONFIDENCE_THRESHOLD = 70

        /**
         * Regex patterns to extract intent hints from LLM responses
         */
        private val INTENT_PATTERN = """\[INTENT:\s*(\w+)\]""".toRegex()
        private val CONFIDENCE_PATTERN = """\[CONFIDENCE:\s*(\d+)\]""".toRegex()
    }

    /**
     * Extracted intent hint from LLM response
     */
    data class IntentHint(
        val intentName: String,
        val confidence: Int
    )

    /**
     * Learn from LLM response
     *
     * Extracts intent hint from LLM response, validates it, and stores in database.
     *
     * **DEPRECATED (Issue 5.3):** Use NLUSelfLearner.learnFromLLM() instead.
     * This method is retained for backwards compatibility but learning now
     * routes through NLUSelfLearner for proper embedding computation.
     *
     * @param userMessage Original user message
     * @param llmResponse LLM's full response (may contain [INTENT: xxx] markers)
     * @return True if learning was successful, false otherwise
     */
    @Deprecated(
        message = "Issue 5.3: Use NLUSelfLearner.learnFromLLM() for unified learning",
        replaceWith = ReplaceWith(
            "nluSelfLearner.learnFromLLM(userMessage, extractIntentHint(llmResponse)?.intentName ?: \"\", extractIntentHint(llmResponse)?.confidence?.div(100f) ?: 0f)",
            "com.augmentalis.nlu.NLUSelfLearner"
        )
    )
    suspend fun learnFromResponse(
        userMessage: String,
        llmResponse: String
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            // 1. Extract intent hint from LLM response
            val hint = extractIntentHint(llmResponse)

            if (hint == null) {
                nluLogDebug(TAG, "No intent hint found in LLM response")
                return@withContext false
            }

            nluLogInfo(TAG, "Extracted intent hint: ${hint.intentName} (confidence: ${hint.confidence})")

            // 2. Validate confidence threshold
            if (hint.confidence < LEARNING_CONFIDENCE_THRESHOLD) {
                nluLogDebug(TAG, "Intent confidence too low for learning: ${hint.confidence} < $LEARNING_CONFIDENCE_THRESHOLD")
                return@withContext false
            }

            // 3. Learn the intent
            learnIntent(userMessage, hint.intentName)

            nluLogInfo(TAG, "Successfully learned: \"$userMessage\" → ${hint.intentName}")
            true
        } catch (e: Exception) {
            nluLogError(TAG, "Failed to learn from LLM response: ${e.message}", e)
            false
        }
    }

    /**
     * Extract intent hint from LLM response
     *
     * Looks for markers like:
     * - [INTENT: greeting]
     * - [CONFIDENCE: 95]
     *
     * @param llmResponse Full LLM response text
     * @return IntentHint if found, null otherwise
     */
    fun extractIntentHint(llmResponse: String): IntentHint? {
        val intentMatch = INTENT_PATTERN.find(llmResponse)
        val confidenceMatch = CONFIDENCE_PATTERN.find(llmResponse)

        return if (intentMatch != null && confidenceMatch != null) {
            IntentHint(
                intentName = intentMatch.groupValues[1],
                confidence = confidenceMatch.groupValues[1].toInt()
            )
        } else {
            null
        }
    }

    /**
     * Clean LLM response by removing intent markers
     *
     * Removes [INTENT: xxx] and [CONFIDENCE: xxx] from response before showing to user.
     *
     * @param llmResponse Full LLM response with markers
     * @return Cleaned response without markers
     */
    fun cleanResponse(llmResponse: String): String {
        return llmResponse
            .replace(INTENT_PATTERN, "")
            .replace(CONFIDENCE_PATTERN, "")
            .trim()
    }

    /**
     * Learn new intent or add example to existing intent
     *
     * @param userExample User's message (e.g., "hello ava")
     * @param intentName Intent to associate (e.g., "greeting")
     * @param source Source of learning (e.g., "LLM_LEARNED", "USER_CONFIRMED")
     */
    private suspend fun learnIntent(
        userExample: String,
        intentName: String,
        source: String = "LLM_LEARNED"
    ) = withContext(Dispatchers.IO) {
        val database = DatabaseDriverFactory(context).createDriver().createDatabase()
        val exampleQueries = database.intentExampleQueries

        // Check if intent already exists
        val allExamples = exampleQueries.selectAll().executeAsList()
        val existingIntent = allExamples.find { it.intent_id == intentName }

        if (existingIntent != null) {
            // Intent exists - add new example
            nluLogDebug(TAG, "Adding example to existing intent: $intentName")
        } else {
            // Intent doesn't exist - create new intent
            nluLogDebug(TAG, "Creating new intent: $intentName")
        }

        // Insert into database using SQLDelight
        val exampleHash = generateHash(intentName, userExample)
        val currentTime = System.currentTimeMillis()

        exampleQueries.insert(
            example_hash = exampleHash,
            intent_id = intentName,
            example_text = userExample,
            is_primary = if (existingIntent == null) true else false,
            source = source,
            format_version = "2.0",
            ipc_code = null,
            locale = "en-US",
            created_at = currentTime,
            usage_count = 0,
            last_used = null
        )

        nluLogInfo(TAG, "Stored new example in database: \"$userExample\" → $intentName")

        // Re-initialize classifier to recompute embeddings
        val modelPath = context.getExternalFilesDir(null)?.absolutePath + "/models/AVA-384-Base-INT8.AON"
        IntentClassifier.getInstance(context).initialize(modelPath)

        nluLogInfo(TAG, "Recomputed embeddings with new example")
    }

    /**
     * Generate unique hash for intent example
     *
     * Used as primary key in database to prevent duplicates.
     *
     * @param intentId Intent identifier
     * @param exampleText Example text
     * @return MD5 hash of intent + example
     */
    private fun generateHash(intentId: String, exampleText: String): String {
        val input = "$intentId:$exampleText"
        val md = MessageDigest.getInstance("MD5")
        val digest = md.digest(input.toByteArray())
        return digest.joinToString("") { "%02x".format(it) }
    }

    /**
     * Save learned example directly to database (REQ-004).
     *
     * Used by interactive confidence learning dialog when user confirms or corrects NLU's interpretation.
     *
     * @param userText User's original text/query
     * @param intentId Intent to associate with the text
     * @param source Source of learning (default: "USER_CONFIRMED")
     * @return True if successfully saved, false otherwise
     */
    suspend fun saveLearnedExample(
        userText: String,
        intentId: String,
        source: String = "USER_CONFIRMED"
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            learnIntent(userText, intentId, source)
            true
        } catch (e: Exception) {
            nluLogError(TAG, "Failed to save learned example: ${e.message}", e)
            false
        }
    }

    /**
     * Get learning statistics
     *
     * @return Map with stats (total learned, by source, etc.)
     */
    suspend fun getStats(): Map<String, Any> = withContext(Dispatchers.IO) {
        try {
            val database = DatabaseDriverFactory(context).createDriver().createDatabase()
            val exampleQueries = database.intentExampleQueries

            val allExamples = exampleQueries.selectAll().executeAsList()
            val learnedExamples = allExamples.filter { it.source == "LLM_LEARNED" }
            val learnedIntents = learnedExamples.map { it.intent_id }.distinct()

            mapOf(
                "total_examples" to allExamples.size,
                "learned_examples" to learnedExamples.size,
                "learned_intents" to learnedIntents.size,
                "learned_intent_list" to learnedIntents
            )
        } catch (e: Exception) {
            nluLogError(TAG, "Failed to get stats: ${e.message}", e)
            mapOf("error" to (e.message ?: "Unknown error"))
        }
    }
}
