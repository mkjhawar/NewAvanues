/**
 * Unit tests for GGUFInferenceStrategy
 *
 * Tests llama.cpp JNI integration for GGUF model inference.
 *
 * Created: 2025-12-06
 * Author: Manoj Jhawar
 */

package com.augmentalis.llm.alc.inference

import android.content.Context
import com.augmentalis.llm.alc.models.GenerationParams
import com.augmentalis.llm.alc.models.InferenceRequest
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.io.File

/**
 * Tests for GGUFInferenceStrategy
 *
 * Note: These are unit tests that mock the native library.
 * Integration tests with real models are in androidTest/.
 */
class GGUFInferenceStrategyTest {

    private lateinit var context: Context
    private lateinit var strategy: GGUFInferenceStrategy
    private val mockModelPath = "/mock/path/model.gguf"

    @Before
    fun setup() {
        // Mock Android context
        context = mockk(relaxed = true)

        // Mock System.loadLibrary to prevent UnsatisfiedLinkError
        mockkStatic(System::class)
        every { System.loadLibrary(any()) } returns Unit

        // Create strategy with mock path
        strategy = GGUFInferenceStrategy(
            context = context,
            modelPath = mockModelPath,
            contextLength = 2048,
            gpuLayers = 0  // CPU-only for tests
        )
    }

    @After
    fun teardown() {
        // Cleanup
        strategy.unloadModel()
    }

    @Test
    fun `test strategy name is llama-cpp`() {
        assertEquals("llama.cpp", strategy.getName())
    }

    @Test
    fun `test strategy priority is 100`() {
        assertEquals(100, strategy.getPriority())
    }

    @Test
    fun `test isAvailable returns false when native library not loaded`() {
        // Native library mock won't actually load, so isAvailable should be false
        // (In real scenario, it would check if model file exists)
        val available = strategy.isAvailable()

        // Since we're mocking and model file doesn't exist, should be false
        assertFalse(available)
    }

    @Test
    fun `test getModelInfo returns correct information`() {
        val info = strategy.getModelInfo()

        assertEquals(mockModelPath, info["path"])
        assertEquals(2048, info["context_length"])
        assertEquals(0, info["gpu_layers"])
        assertEquals(false, info["loaded"])  // Model not actually loaded in unit test
        assertTrue(info.containsKey("native_loaded"))
    }

    @Test
    fun `test fromModelDirectory returns null when no amc file`() {
        val mockDir = mockk<File>()
        every { mockDir.listFiles() } returns arrayOf()

        val result = GGUFInferenceStrategy.fromModelDirectory(context, mockDir)

        assertNull(result)
    }

    @Test
    fun `test fromModelDirectory finds gguf file directly`() {
        val mockDir = mockk<File>()
        val mockGgufFile = mockk<File>()

        every { mockDir.name } returns "test-model"
        every { mockDir.listFiles() } returns arrayOf(mockGgufFile)
        every { mockGgufFile.extension } returns "gguf"
        every { mockGgufFile.name } returns "model.gguf"
        every { mockGgufFile.absolutePath } returns "/path/to/model.gguf"

        val result = GGUFInferenceStrategy.fromModelDirectory(context, mockDir)

        assertNotNull(result)
        assertEquals("/path/to/model.gguf", result?.getModelInfo()?.get("path"))
    }

    @Test
    fun `test unloadModel does not crash when called multiple times`() {
        // Should not throw exception
        strategy.unloadModel()
        strategy.unloadModel()
        strategy.unloadModel()
    }

    @Test
    fun `test inference fails gracefully when model not loaded`() = runTest {
        val request = InferenceRequest(
            tokens = listOf(1, 2, 3),
            cache = null
        )

        // Should throw InferenceException since native library is mocked
        assertThrows(com.augmentalis.llm.alc.models.InferenceException::class.java) {
            kotlinx.coroutines.runBlocking {
                strategy.infer(request)
            }
        }
    }

    @Test
    fun `test generateStreaming fails gracefully when model not loaded`() = runTest {
        val params = GenerationParams(
            temperature = 0.7f,
            topP = 0.95f,
            topK = 40,
            repeatPenalty = 1.1f,
            maxTokens = 100,
            stopSequences = listOf("</s>")
        )

        // Should throw InferenceException since native library is mocked
        assertThrows(com.augmentalis.llm.alc.models.InferenceException::class.java) {
            kotlinx.coroutines.runBlocking {
                strategy.generateStreaming("Test prompt", params).toList()
            }
        }
    }
}
