// filename: features/overlay/src/main/java/com/augmentalis/ava/features/overlay/integration/NluConnector.kt
// created: 2025-11-01 23:35:00 -0700
// author: Manoj Jhawar
// © Augmentalis Inc, Intelligent Devices LLC
// TCR: Phase 3 - Integration Layer
// agent: Engineer | mode: ACT

package com.augmentalis.overlay.integration

import android.content.Context
import com.augmentalis.ava.core.common.Result
import com.augmentalis.nlu.IntentClassifier
import com.augmentalis.nlu.ModelManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Connector to features:nlu module for intent classification.
 *
 * Wraps the NLU IntentClassifier and provides a simple async interface
 * for classifying user voice input into structured intents.
 *
 * Integration:
 * - Uses features:nlu IntentClassifier with ONNX Runtime
 * - Initializes ModelManager and IntentClassifier on first use
 * - Returns intent category string for suggestion generation
 *
 * @param context Android context
 * @author Manoj Jhawar
 */
class NluConnector(private val context: Context) {

    private val intentClassifier: IntentClassifier? by lazy {
        try {
            IntentClassifier.getInstance(context)
        } catch (e: Exception) {
            null
        }
    }
    private val modelManager: ModelManager? by lazy {
        try {
            ModelManager(context)
        } catch (e: Exception) {
            null
        }
    }
    private var initialized = false

    // Overlay-specific intent candidates
    private val candidateIntents = listOf(
        "search",
        "translate",
        "reminder",
        "message",
        "summarize",
        "query",
        "general"
    )

    /**
     * Ensure models are initialized
     */
    private suspend fun ensureInitialized(): Boolean {
        if (!initialized) {
            val manager = modelManager ?: return false
            val classifier = intentClassifier ?: return false

            // Check if model is available, download if needed
            if (!manager.isModelAvailable()) {
                when (manager.downloadModelsIfNeeded()) {
                    is Result.Error -> return false
                    else -> {}
                }
            }

            // Initialize IntentClassifier with model path
            val modelPath = manager.getModelPath()
            when (classifier.initialize(modelPath)) {
                is Result.Success -> initialized = true
                is Result.Error -> return false
            }
        }
        return initialized
    }

    /**
     * Classify user input into intent category
     *
     * @param text Voice transcript
     * @return Intent category (e.g., "search", "reminder", "translate")
     */
    suspend fun classifyIntent(text: String): String = withContext(Dispatchers.Default) {
        if (!ensureInitialized()) {
            // Fallback to keyword matching if ONNX initialization fails
            return@withContext classifyWithKeywords(text)
        }

        // Use ONNX classifier if available
        intentClassifier?.let { classifier ->
            when (val result = classifier.classifyIntent(text, candidateIntents)) {
                is Result.Success -> {
                    val classification = result.data
                    // Return intent if confidence is above threshold
                    if (classification.confidence >= 0.5f) {
                        return@withContext classification.intent
                    }
                }
                is Result.Error -> {
                    // Fallback to keywords on error
                }
            }
        }

        // Fallback to keyword matching
        classifyWithKeywords(text)
    }

    /**
     * Fallback keyword-based classification
     */
    private fun classifyWithKeywords(text: String): String {
        val lowercaseText = text.lowercase()

        return when {
            // Search intents
            lowercaseText.contains("search") ||
            lowercaseText.contains("find") ||
            lowercaseText.contains("look up") ||
            lowercaseText.contains("google") -> "search"

            // Translation intents
            lowercaseText.contains("translate") ||
            lowercaseText.contains("in spanish") ||
            lowercaseText.contains("in french") ||
            lowercaseText.contains("in german") -> "translate"

            // Reminder/scheduling intents
            lowercaseText.contains("remind") ||
            lowercaseText.contains("schedule") ||
            lowercaseText.contains("calendar") ||
            lowercaseText.contains("appointment") -> "reminder"

            // Message/communication intents
            lowercaseText.contains("message") ||
            lowercaseText.contains("send a text") ||
            lowercaseText.contains("text message") ||
            lowercaseText.contains("call") ||
            lowercaseText.contains("email") -> "message"

            // Summarization intents
            lowercaseText.contains("summarize") ||
            lowercaseText.contains("summary") ||
            lowercaseText.contains("tldr") -> "summarize"

            // Information/query intents
            lowercaseText.contains("what is") ||
            lowercaseText.contains("who is") ||
            lowercaseText.contains("when is") ||
            lowercaseText.contains("where is") ||
            lowercaseText.contains("how to") -> "query"

            // Default to general
            else -> "general"
        }
    }

    /**
     * Extract entities from text using pattern matching
     *
     * Extracts common entities like numbers, dates, names, locations, etc.
     * Uses regex patterns tailored to the classified intent.
     *
     * @param text User input
     * @param intent Classified intent
     * @return Map of entity types to values
     */
    suspend fun extractEntities(
        text: String,
        intent: String
    ): Map<String, String> = withContext(Dispatchers.Default) {
        val entities = mutableMapOf<String, String>()

        // Extract based on intent type
        when (intent) {
            "search", "query" -> {
                extractSearchEntities(text, entities)
            }
            "translate" -> {
                extractTranslateEntities(text, entities)
            }
            "reminder" -> {
                extractReminderEntities(text, entities)
            }
            "message" -> {
                extractMessageEntities(text, entities)
            }
        }

        // Extract common entities regardless of intent
        extractCommonEntities(text, entities)

        entities
    }

    private fun extractSearchEntities(text: String, entities: MutableMap<String, String>) {
        // Extract search query after "search for", "find", etc.
        val searchPatterns = listOf(
            """(?:search for|find|look up|google)\s+(.+)""".toRegex(RegexOption.IGNORE_CASE),
            """what is\s+(.+)""".toRegex(RegexOption.IGNORE_CASE),
            """who is\s+(.+)""".toRegex(RegexOption.IGNORE_CASE)
        )

        for (pattern in searchPatterns) {
            pattern.find(text)?.let { match ->
                entities["query"] = match.groupValues[1].trim()
                return
            }
        }
    }

    private fun extractTranslateEntities(text: String, entities: MutableMap<String, String>) {
        // Extract source text and target language
        val translatePattern = """translate\s+"?([^"]+)"?\s+(?:to|in)\s+(\w+)""".toRegex(RegexOption.IGNORE_CASE)
        translatePattern.find(text)?.let { match ->
            entities["text"] = match.groupValues[1].trim()
            entities["target_language"] = match.groupValues[2].trim().lowercase()
        }

        // Extract just target language if no source text specified
        val langPattern = """(?:in|to)\s+(spanish|french|german|italian|japanese|chinese|korean|hindi)""".toRegex(RegexOption.IGNORE_CASE)
        langPattern.find(text)?.let { match ->
            entities["target_language"] = match.groupValues[1].trim().lowercase()
        }
    }

    private fun extractReminderEntities(text: String, entities: MutableMap<String, String>) {
        // Extract reminder content
        val reminderPattern = """remind me (?:to\s+)?(.+?)(?:\s+at|\s+on|\s+in|$)""".toRegex(RegexOption.IGNORE_CASE)
        reminderPattern.find(text)?.let { match ->
            entities["task"] = match.groupValues[1].trim()
        }

        // Extract time expressions
        val timePatterns = listOf(
            """at\s+(\d{1,2}(?::\d{2})?\s*(?:am|pm))""".toRegex(RegexOption.IGNORE_CASE),
            """in\s+(\d+)\s+(minutes?|hours?|days?)""".toRegex(RegexOption.IGNORE_CASE),
            """(?:tomorrow|today|tonight)""".toRegex(RegexOption.IGNORE_CASE)
        )

        timePatterns.firstOrNull { pattern ->
            pattern.find(text)?.let { match ->
                entities["time"] = match.value.trim()
                true
            } ?: false
        }
    }

    private fun extractMessageEntities(text: String, entities: MutableMap<String, String>) {
        // Extract recipient
        val recipientPattern = """(?:text|message|call|email)\s+(\w+)""".toRegex(RegexOption.IGNORE_CASE)
        recipientPattern.find(text)?.let { match ->
            entities["recipient"] = match.groupValues[1].trim()
        }

        // Extract message content
        val messagePattern = """(?:saying|that says?)\s+"?([^"]+)"?""".toRegex(RegexOption.IGNORE_CASE)
        messagePattern.find(text)?.let { match ->
            entities["message"] = match.groupValues[1].trim()
        }
    }

    private fun extractCommonEntities(text: String, entities: MutableMap<String, String>) {
        // Extract numbers
        val numberPattern = """\b(\d+(?:[.,]\d+)?)\b""".toRegex()
        numberPattern.findAll(text).firstOrNull()?.let { match ->
            if (!entities.containsKey("number")) {
                entities["number"] = match.value
            }
        }

        // Extract phone numbers
        val phonePattern = """\b(\d{3}[-.]?\d{3}[-.]?\d{4})\b""".toRegex()
        phonePattern.find(text)?.let { match ->
            entities["phone"] = match.value
        }

        // Extract email addresses
        val emailPattern = """\b([a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,})\b""".toRegex()
        emailPattern.find(text)?.let { match ->
            entities["email"] = match.value
        }

        // Extract URLs
        val urlPattern = """\b(https?://[^\s]+)\b""".toRegex()
        urlPattern.find(text)?.let { match ->
            entities["url"] = match.value
        }
    }
}
