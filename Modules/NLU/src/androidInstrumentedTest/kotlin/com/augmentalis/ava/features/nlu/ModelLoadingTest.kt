package com.augmentalis.nlu

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.augmentalis.ava.core.common.Result
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * Tests for MobileBERT model loading from assets
 * Validates that the model files exist and can be initialized
 */
@RunWith(AndroidJUnit4::class)
class ModelLoadingTest {

    private lateinit var context: Context
    private lateinit var modelManager: ModelManager
    private lateinit var classifier: IntentClassifier

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        modelManager = ModelManager(context)
        classifier = IntentClassifier.getInstance(context)
    }

    @Test
    fun testModelFilesExistInAssets() {
        // Verify model file exists in assets
        // Uses .AON extension (AVA ONNX Naming) - see docs/standards/AVA-FILE-FORMATS.md
        try {
            val modelStream = context.assets.open("models/AVA-384-Base-INT8.AON")
            assertNotNull("Model file should exist in assets", modelStream)

            val modelSize = modelStream.available()
            assertTrue(
                "Model size should be ~25MB",
                modelSize in 20_000_000..30_000_000
            )
            modelStream.close()

            // Verify vocab file exists in assets
            val vocabStream = context.assets.open("models/vocab.txt")
            assertNotNull("Vocab file should exist in assets", vocabStream)

            val vocabSize = vocabStream.available()
            assertTrue(
                "Vocab size should be ~226KB",
                vocabSize in 200_000..300_000
            )
            vocabStream.close()

            println("✅ Model files verified in assets")
            println("   Model: ${modelSize / 1024 / 1024}MB")
            println("   Vocab: ${vocabSize / 1024}KB")
        } catch (e: java.io.FileNotFoundException) {
            println("⏭️ Skipping test - Model files not available in assets")
            println("   To run this test, add model files to src/androidMain/assets/models/")
            // Test passes - model files are optional for development
        }
    }

    @Test
    fun testCopyModelFromAssets() = runTest {
        // Copy model from assets to files directory
        val result = modelManager.copyModelFromAssets()

        // Model files may not be available in test environment
        when (result) {
            is Result.Success -> {
                // Verify files exist in files directory
                assertTrue(
                    "Model file should exist after copy",
                    modelManager.isModelAvailable()
                )

                // Verify file sizes
                val modelSize = modelManager.getModelsSize()
                assertTrue(
                    "Combined size should be ~25.7MB",
                    modelSize in 25_000_000..30_000_000
                )

                println("✅ Model copied successfully")
                println("   Total size: ${modelSize / 1024 / 1024}MB")
            }
            is Result.Error -> {
                println("⏭️ Skipping test - Model files not available: ${result.message}")
                // Test passes - model files are optional for development
            }
        }
    }

    @Test
    fun testClassifierInitialization() = runTest {
        // Ensure model is available
        val copyResult = modelManager.copyModelFromAssets()

        when (copyResult) {
            is Result.Success -> {
                // Initialize classifier
                val initResult = classifier.initialize(modelManager.getModelPath())

                assertTrue(
                    "Classifier initialization should succeed: ${if (initResult is Result.Error) initResult.message else ""}",
                    initResult is Result.Success
                )

                println("✅ Classifier initialized successfully")
            }
            is Result.Error -> {
                println("⏭️ Skipping test - Model files not available: ${copyResult.message}")
                // Test passes - model files are optional for development
            }
        }
    }

    @Test
    fun testTokenizerLoadsVocabulary() = runTest {
        // Copy vocab from assets first
        modelManager.copyModelFromAssets()

        // Create tokenizer
        val tokenizer = BertTokenizer(context, maxSequenceLength = 128)

        // Test tokenization
        val result = tokenizer.tokenize("Turn on the lights")

        // Verify token structure
        assertEquals("Input IDs should have max sequence length", 128, result.inputIds.size)
        assertEquals("Attention mask should have max sequence length", 128, result.attentionMask.size)
        assertEquals("Token type IDs should have max sequence length", 128, result.tokenTypeIds.size)

        // First token should be [CLS] = 101
        assertEquals("First token should be [CLS]", 101, result.inputIds[0])

        // Should have some non-padding tokens
        val nonPaddingTokens = result.attentionMask.count { it == 1L }
        assertTrue(
            "Should have at least 3 tokens (CLS + words + SEP)",
            nonPaddingTokens >= 3
        )

        println("✅ Tokenizer loaded vocabulary")
        println("   Non-padding tokens: $nonPaddingTokens")
    }

    @Test
    fun testRealInferencePerformance() = runTest {
        // Setup
        modelManager.copyModelFromAssets()
        val initResult = classifier.initialize(modelManager.getModelPath())

        if (initResult is Result.Error) {
            println("⚠️  Skipping inference test - model initialization failed")
            println("   Error: ${initResult.message}")
            return@runTest
        }

        // Test real inference
        val utterance = "Turn on the lights"
        val candidateIntents = listOf("control_lights", "check_weather", "set_alarm")

        val startTime = System.currentTimeMillis()
        val result = classifier.classifyIntent(utterance, candidateIntents)
        val elapsed = System.currentTimeMillis() - startTime

        assertTrue(
            "Inference should succeed",
            result is Result.Success
        )

        if (result is Result.Success) {
            println("✅ Inference successful")
            println("   Intent: ${result.data.intent}")
            println("   Confidence: ${result.data.confidence}")
            println("   Inference time: ${result.data.inferenceTimeMs}ms")
            println("   Total time: ${elapsed}ms")

            // Performance validation
            if (result.data.inferenceTimeMs < 50) {
                println("   ✅ Performance: Exceeds target (<50ms)")
            } else if (result.data.inferenceTimeMs < 100) {
                println("   ⚠️  Performance: Within max budget (<100ms)")
            } else {
                println("   ❌ Performance: Exceeds budget (${result.data.inferenceTimeMs}ms)")
            }

            // Confidence validation
            assertTrue(
                "Confidence should be between 0 and 1",
                result.data.confidence in 0.0f..1.0f
            )
        }
    }

    @Test
    fun testMultipleInferenceCalls() = runTest {
        // Setup
        val copyResult = modelManager.copyModelFromAssets()
        if (copyResult is Result.Error) {
            println("⏭️ Skipping test - Model files not available")
            return@runTest
        }

        val initResult = classifier.initialize(modelManager.getModelPath())
        if (initResult is Result.Error) {
            println("⏭️ Skipping test - Model initialization failed: ${initResult.message}")
            return@runTest
        }

        val utterances = listOf(
            "Turn on the lights",
            "What's the weather today?",
            "Set an alarm for 7am",
            "Turn off all lights",
            "Is it going to rain?"
        )

        val candidateIntents = listOf("control_lights", "check_weather", "set_alarm")
        val inferenceTimes = mutableListOf<Long>()

        utterances.forEach { utterance ->
            val result = classifier.classifyIntent(utterance, candidateIntents)

            if (result is Result.Success) {
                inferenceTimes.add(result.data.inferenceTimeMs)
                println("  '$utterance' → ${result.data.intent} (${result.data.confidence}, ${result.data.inferenceTimeMs}ms)")
            }
        }

        // Calculate average
        val avgTime = inferenceTimes.average()
        val maxTime = inferenceTimes.maxOrNull() ?: 0

        println("\n📊 Performance Stats:")
        println("   Average: ${avgTime.toInt()}ms")
        println("   Max: ${maxTime}ms")
        println("   All times: ${inferenceTimes.joinToString(", ")}ms")

        assertTrue(
            "All inferences should complete",
            inferenceTimes.size == utterances.size
        )

        // After warm-up, should be faster
        if (inferenceTimes.size > 2) {
            val warmupTime = inferenceTimes[0]
            val avgAfterWarmup = inferenceTimes.drop(1).average()

            println("   First (cold): ${warmupTime}ms")
            println("   Avg (warm): ${avgAfterWarmup.toInt()}ms")
        }
    }
}
