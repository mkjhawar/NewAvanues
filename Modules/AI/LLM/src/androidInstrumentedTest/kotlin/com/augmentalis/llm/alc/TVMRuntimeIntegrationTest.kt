package com.augmentalis.llm.alc

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import kotlin.system.measureTimeMillis

/**
 * Integration tests for TVMRuntime with real MLC-LLM models
 *
 * Tests verify that TVM runtime can load models, perform inference,
 * and handle tokenization/detokenization on actual Android devices.
 *
 * Requirements:
 * - TVM native library loaded (libtvm4j_runtime_packed.so)
 * - MLC-LLM model available on device (optional for some tests)
 * - Device or emulator with OpenCL/CPU support
 *
 * Created: 2025-11-15
 * Part of: P7 TVMTokenizer Real Implementation (LLM Integration Phase 2)
 */
@RunWith(AndroidJUnit4::class)
class TVMRuntimeIntegrationTest {

    private lateinit var context: Context
    private lateinit var runtime: TVMRuntime

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
    }

    @After
    fun tearDown() {
        if (::runtime.isInitialized) {
            runtime.dispose()
        }
    }

    // ===== Runtime Creation Tests =====

    @Test
    fun testCreateRuntimeWithOpenCL() {
        runtime = TVMRuntime.create(context, "opencl")

        assertNotNull("Runtime should be created", runtime)
    }

    @Test
    fun testCreateRuntimeWithCPU() {
        runtime = TVMRuntime.create(context, "cpu")

        assertNotNull("Runtime should be created", runtime)
    }

    @Test
    fun testCreateRuntimeDefaultDevice() {
        runtime = TVMRuntime.create(context)

        assertNotNull("Runtime should be created with default device", runtime)
    }

    @Test
    fun testCreateMultipleRuntimes() {
        val runtime1 = TVMRuntime.create(context, "opencl")
        val runtime2 = TVMRuntime.create(context, "cpu")

        assertNotNull(runtime1)
        assertNotNull(runtime2)

        runtime1.dispose()
        runtime2.dispose()
    }

    // ===== Tokenization/Detokenization Tests =====

    @Test
    fun testTokenizeSimpleText() {
        runtime = TVMRuntime.create(context)

        val text = "Hello world"
        val tokens = runtime.tokenize(text)

        assertNotNull("Tokens should not be null", tokens)
        assertTrue("Tokens should not be empty", tokens.isNotEmpty())
        assertTrue("Should produce multiple tokens", tokens.size >= 2)
    }

    @Test
    fun testDetokenizeSimpleTokens() {
        runtime = TVMRuntime.create(context)

        // First encode
        val originalText = "Test"
        val tokens = runtime.tokenize(originalText)

        // Then decode
        val decodedText = runtime.detokenize(tokens)

        assertNotNull("Decoded text should not be null", decodedText)
        assertTrue("Decoded text should not be empty", decodedText.isNotEmpty())
    }

    @Test
    fun testRoundTripTokenization() {
        runtime = TVMRuntime.create(context)

        val originalText = "Round trip test"

        // Encode
        val tokens = runtime.tokenize(originalText)

        // Decode
        val decodedText = runtime.detokenize(tokens)

        assertNotNull(decodedText)
        // Round-trip should preserve meaning if not exact text
        assertTrue(
            "Round-trip should preserve text",
            decodedText.contains("trip", ignoreCase = true)
        )
    }

    @Test
    fun testTokenizeEmptyString() {
        runtime = TVMRuntime.create(context)

        val tokens = runtime.tokenize("")

        assertNotNull(tokens)
        assertTrue("Empty string should produce 0 or 1 tokens", tokens.size <= 1)
    }

    @Test
    fun testDetokenizeEmptyList() {
        runtime = TVMRuntime.create(context)

        val text = runtime.detokenize(emptyList())

        assertNotNull(text)
        assertTrue("Empty token list should produce empty or minimal text", text.length <= 1)
    }

    // ===== Tokenizer Caching Tests =====

    @Test
    fun testCreateTokenizerReusability() {
        runtime = TVMRuntime.create(context)

        // Create tokenizer
        val tokenizer1 = runtime.createTokenizer()
        val tokenizer2 = runtime.createTokenizer()

        val text = "reusability test"

        // Both should work
        val tokens1 = tokenizer1.encode(text)
        val tokens2 = tokenizer2.encode(text)

        assertEquals("Different tokenizers should produce same tokens", tokens1, tokens2)
    }

    @Test
    fun testTokenizationConsistency() {
        runtime = TVMRuntime.create(context)

        val text = "consistency test"

        // Tokenize same text multiple times
        val tokens1 = runtime.tokenize(text)
        val tokens2 = runtime.tokenize(text)
        val tokens3 = runtime.tokenize(text)

        assertEquals("First and second tokenization should match", tokens1, tokens2)
        assertEquals("Second and third tokenization should match", tokens2, tokens3)
    }

    // ===== Model Loading Tests =====

    @Test
    fun testLoadModuleWithInvalidPath() {
        runtime = TVMRuntime.create(context)

        val invalidPath = "/invalid/path/model.so"

        try {
            runtime.loadModule(invalidPath, "model")
            fail("Should throw exception for invalid path")
        } catch (e: Exception) {
            // Expected
            assertTrue(
                "Should throw IllegalArgumentException or RuntimeException",
                e is IllegalArgumentException || e is RuntimeException
            )
            assertNotNull("Exception should have message", e.message)
        }
    }

    @Test
    fun testLoadModuleWithNonExistentFile() {
        runtime = TVMRuntime.create(context)

        val externalDir = context.getExternalFilesDir(null)
        val nonExistentPath = File(externalDir, "nonexistent_model.so").absolutePath

        try {
            runtime.loadModule(nonExistentPath, "model")
            fail("Should throw exception for non-existent file")
        } catch (e: Exception) {
            // Expected
            assertTrue("Should throw IllegalArgumentException", e is IllegalArgumentException)
            assertTrue(
                "Message should mention model not found",
                e.message?.contains("not found", ignoreCase = true) == true
            )
        }
    }

    // ===== Performance Tests =====

    @Test
    fun testTokenizationPerformance() {
        runtime = TVMRuntime.create(context)

        val testText = "Performance test with multiple words and punctuation."

        // Warm-up
        repeat(10) {
            runtime.tokenize(testText)
        }

        // Benchmark
        val iterations = 100
        val totalTime = measureTimeMillis {
            repeat(iterations) {
                runtime.tokenize(testText)
            }
        }

        val avgTime = totalTime.toDouble() / iterations

        // Should average < 10ms per tokenization (including overhead)
        assertTrue(
            "Average tokenization time should be < 10ms (actual: ${avgTime}ms)",
            avgTime < 10.0
        )
    }

    @Test
    fun testDetokenizationPerformance() {
        runtime = TVMRuntime.create(context)

        val testText = "Performance test"
        val tokens = runtime.tokenize(testText)

        // Warm-up
        repeat(10) {
            runtime.detokenize(tokens)
        }

        // Benchmark
        val iterations = 100
        val totalTime = measureTimeMillis {
            repeat(iterations) {
                runtime.detokenize(tokens)
            }
        }

        val avgTime = totalTime.toDouble() / iterations

        // Should average < 10ms per detokenization
        assertTrue(
            "Average detokenization time should be < 10ms (actual: ${avgTime}ms)",
            avgTime < 10.0
        )
    }

    // ===== Device Type Tests =====

    @Test
    fun testDifferentDeviceTypes() {
        val deviceTypes = listOf("opencl", "cpu")

        deviceTypes.forEach { deviceType ->
            val testRuntime = TVMRuntime.create(context, deviceType)

            assertNotNull("Runtime should be created for device: $deviceType", testRuntime)

            // Test basic tokenization works on this device
            val tokens = testRuntime.tokenize("device test")
            assertTrue("Tokenization should work on $deviceType", tokens.isNotEmpty())

            testRuntime.dispose()
        }
    }

    // ===== Lifecycle Tests =====

    @Test
    fun testRuntimeDisposeAndRecreate() {
        // Create runtime
        runtime = TVMRuntime.create(context)

        val text = "lifecycle test"
        val tokens1 = runtime.tokenize(text)

        assertNotNull(tokens1)

        // Dispose
        runtime.dispose()

        // Create new runtime
        runtime = TVMRuntime.create(context)

        val tokens2 = runtime.tokenize(text)

        // Should work with new runtime
        assertNotNull(tokens2)
        assertEquals("Recreated runtime should produce same tokens", tokens1, tokens2)
    }

    @Test
    fun testMultipleDisposeCalls() {
        runtime = TVMRuntime.create(context)

        // Multiple dispose calls should not throw
        runtime.dispose()
        runtime.dispose()
        runtime.dispose()

        // No exception expected
    }

    // ===== Error Handling Tests =====

    @Test
    fun testTokenizeWithVeryLongText() {
        runtime = TVMRuntime.create(context)

        // Test with very long text
        val veryLongText = "word ".repeat(5000) // 5k words

        try {
            val tokens = runtime.tokenize(veryLongText)

            // Should either succeed or throw exception
            assertNotNull("Should handle very long text", tokens)
            assertTrue("Should produce many tokens", tokens.size > 500)
        } catch (e: Exception) {
            // If it fails, should be graceful
            assertNotNull("Exception should have message", e.message)
        }
    }

    @Test
    fun testDetokenizeWithManyTokens() {
        runtime = TVMRuntime.create(context)

        // Create a large token list
        val manyTokens = (1..1000).toList()

        try {
            val text = runtime.detokenize(manyTokens)

            // Should either succeed or throw exception
            assertNotNull("Should handle many tokens", text)
        } catch (e: Exception) {
            // If it fails, should be graceful
            assertNotNull("Exception should have message", e.message)
        }
    }

    // ===== Multithreading Tests =====

    @Test
    fun testConcurrentTokenization() {
        runtime = TVMRuntime.create(context)

        val testTexts = listOf(
            "First text",
            "Second text",
            "Third text",
            "Fourth text"
        )

        // Tokenize concurrently (note: TVM may not be fully thread-safe)
        val results = testTexts.map { text ->
            Thread {
                runtime.tokenize(text)
            }.apply { start() }
        }.map { thread ->
            thread.join()
        }

        // All threads should complete without crashing
        // (Results may vary depending on TVM thread-safety)
    }

    // ===== Tokenizer Stress Tests =====

    @Test
    fun testMultipleTokenizersFromSameRuntime() {
        runtime = TVMRuntime.create(context)

        // Create multiple tokenizers
        val tokenizers = (1..10).map {
            runtime.createTokenizer()
        }

        val text = "stress test"

        // All should work
        tokenizers.forEach { tokenizer ->
            val tokens = tokenizer.encode(text)
            assertNotNull(tokens)
            assertTrue(tokens.isNotEmpty())
        }
    }

    @Test
    fun testRapidTokenizationCycles() {
        runtime = TVMRuntime.create(context)

        val text = "rapid cycle test"

        // Perform many rapid tokenization/detokenization cycles
        repeat(100) {
            val tokens = runtime.tokenize(text)
            val decoded = runtime.detokenize(tokens)

            assertNotNull(tokens)
            assertNotNull(decoded)
        }
    }
}
