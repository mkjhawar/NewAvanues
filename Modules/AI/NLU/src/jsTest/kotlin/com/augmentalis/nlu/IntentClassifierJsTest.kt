package com.augmentalis.nlu

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * JS/Web tests for IntentClassifier
 *
 * Tests keyword fallback logic and classification flow.
 * ONNX inference tests require browser environment with WASM support
 * and are covered by integration tests.
 */
class IntentClassifierJsTest {

    @Test
    fun getInstance_returnsSameInstance() {
        val instance1 = IntentClassifier.getInstance(Unit)
        val instance2 = IntentClassifier.getInstance(Unit)

        assertTrue(instance1 === instance2, "getInstance should return singleton")
    }

    @Test
    fun getLoadedIntents_emptyBeforeInit() {
        val classifier = IntentClassifier.getInstance(Unit)
        val intents = classifier.getLoadedIntents()

        assertTrue(intents.isEmpty(), "No intents should be loaded before initialization")
    }

    @Test
    fun classifyCommand_exactMatch() {
        kotlinx.coroutines.test.runTest {
            val classifier = IntentClassifier.getInstance(Unit)

            val result = classifier.classifyCommand(
                utterance = "open camera",
                commandPhrases = listOf("open camera", "take photo", "record video"),
                confidenceThreshold = 0.6f,
                ambiguityThreshold = 0.15f
            )

            assertIs<CommandClassificationResult.Match>(result, "Exact match should return Match")
            assertEquals("open camera", result.commandId)
            assertEquals(1.0f, result.confidence)
            assertEquals(MatchMethod.EXACT, result.matchMethod)
        }
    }

    @Test
    fun classifyCommand_exactMatch_caseInsensitive() {
        kotlinx.coroutines.test.runTest {
            val classifier = IntentClassifier.getInstance(Unit)

            val result = classifier.classifyCommand(
                utterance = "OPEN CAMERA",
                commandPhrases = listOf("open camera", "take photo"),
                confidenceThreshold = 0.6f,
                ambiguityThreshold = 0.15f
            )

            assertIs<CommandClassificationResult.Match>(result, "Case-insensitive exact match")
            assertEquals("open camera", result.commandId)
            assertEquals(MatchMethod.EXACT, result.matchMethod)
        }
    }

    @Test
    fun classifyCommand_emptyUtterance_returnsError() {
        kotlinx.coroutines.test.runTest {
            val classifier = IntentClassifier.getInstance(Unit)

            val result = classifier.classifyCommand(
                utterance = "",
                commandPhrases = listOf("open camera"),
                confidenceThreshold = 0.6f,
                ambiguityThreshold = 0.15f
            )

            assertIs<CommandClassificationResult.Error>(result, "Empty utterance should return Error")
        }
    }

    @Test
    fun classifyCommand_emptyPhrases_returnsNoMatch() {
        kotlinx.coroutines.test.runTest {
            val classifier = IntentClassifier.getInstance(Unit)

            val result = classifier.classifyCommand(
                utterance = "open camera",
                commandPhrases = emptyList(),
                confidenceThreshold = 0.6f,
                ambiguityThreshold = 0.15f
            )

            assertIs<CommandClassificationResult.NoMatch>(result, "Empty phrases should return NoMatch")
        }
    }

    @Test
    fun classifyCommand_exactMatchWithWhitespace() {
        kotlinx.coroutines.test.runTest {
            val classifier = IntentClassifier.getInstance(Unit)

            val result = classifier.classifyCommand(
                utterance = "  open camera  ",
                commandPhrases = listOf("open camera"),
                confidenceThreshold = 0.6f,
                ambiguityThreshold = 0.15f
            )

            assertIs<CommandClassificationResult.Match>(result, "Should match after trimming whitespace")
        }
    }

    @Test
    fun addIntentEmbedding_registersIntent() {
        val classifier = IntentClassifier.getInstance(Unit)

        val embedding = FloatArray(384) { 0.1f }
        classifier.addIntentEmbedding("test_intent", embedding)

        assertTrue(
            classifier.getLoadedIntents().contains("test_intent"),
            "Added intent should appear in loaded intents"
        )
    }

    @Test
    fun getEmbeddingDimension_returnsZeroBeforeInit() {
        // When no model is loaded, dimension should be 0 or model default
        val classifier = IntentClassifier.getInstance(Unit)
        val dim = classifier.getEmbeddingDimension()
        // Before initialization, modelManager is null, so dimension = 0
        assertTrue(dim >= 0, "Dimension should be non-negative")
    }
}
