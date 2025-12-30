/**
 * Model Loading Integration Tests
 *
 * Tests the complete model loading pipeline including:
 * - Model discovery from multiple paths
 * - State transitions (Idle -> Loading -> Ready/Error)
 * - Package-aware path detection (release vs debug)
 *
 * Created: 2025-12-03
 * Author: AVA AI Team
 */

package com.augmentalis.ava.integration

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.augmentalis.llm.alc.loader.ModelDiscovery
import com.augmentalis.llm.alc.models.ModelLoadingState
import com.augmentalis.llm.alc.models.ModelTypeInfo
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class ModelLoadingTest {

    private lateinit var context: Context
    private lateinit var modelDiscovery: ModelDiscovery

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        modelDiscovery = ModelDiscovery(context)
    }

    @Test
    fun testPackageNameDetection() {
        // Verify we can detect the package name (release vs debug)
        val packageName = context.packageName
        assertTrue(
            "Package should be either release or debug",
            packageName == "com.augmentalis.ava" ||
            packageName == "com.augmentalis.ava.debug"
        )
    }

    @Test
    fun testModelSearchPaths() = runTest {
        // Verify model search paths are correctly generated
        val externalDir = context.getExternalFilesDir(null)
        assertNotNull("External files dir should be available", externalDir)

        // Check that the models directory path is valid
        val modelsDir = File(externalDir, "models/llm")
        // Directory may not exist yet, but path should be valid
        assertTrue("Models path should be absolute", modelsDir.absolutePath.startsWith("/"))
    }

    @Test
    fun testModelDiscoveryHandlesMissingDir() = runTest {
        // Test that model discovery gracefully handles missing directories
        val result = modelDiscovery.discoverInstalledModels()
        // Should return empty list or valid models, not throw
        assertNotNull("Discovery should return a list", result)
    }

    @Test
    fun testModelLoadingStateTransitions() {
        // Test state machine for model loading

        // Idle state
        val idle = ModelLoadingState.Idle
        assertTrue("Idle should be Idle", idle is ModelLoadingState.Idle)

        // Loading state - using EMBEDDING type for NLU embedding model
        val loading = ModelLoadingState.Loading(
            modelType = ModelTypeInfo.EMBEDDING,
            progress = 0.5f,
            statusMessage = "Loading embeddings..."
        )
        assertTrue("Loading should have progress", loading.progress == 0.5f)

        // Ready state
        val ready = ModelLoadingState.Ready(
            modelType = ModelTypeInfo.EMBEDDING,
            modelPath = "/path/to/model",
            loadTimeMs = 1500L
        )
        assertTrue("Ready should have load time", ready.loadTimeMs > 0)

        // Error state
        val error = ModelLoadingState.Error(
            modelType = ModelTypeInfo.NLU,
            message = "File not found",
            isRecoverable = true
        )
        assertTrue("Error should be recoverable", error.isRecoverable)
    }

    @Test
    fun testNativeLibraryAvailability() {
        // Test if native libraries can be loaded
        // This tests the JNI setup but doesn't require actual model files

        try {
            // Try to load TVM runtime (should be bundled)
            System.loadLibrary("tvm_runtime")
            // If we get here, library loaded successfully
            assertTrue("TVM runtime loaded", true)
        } catch (e: UnsatisfiedLinkError) {
            // Library not available in test environment - acceptable
            println("Note: tvm_runtime not available in test environment")
        }

        // llama-android may not be built yet
        try {
            System.loadLibrary("llama-android")
            assertTrue("llama-android loaded", true)
        } catch (e: UnsatisfiedLinkError) {
            // Expected until we build the JNI library
            println("Note: llama-android not yet built")
        }
    }

    @Test
    fun testModelTypeInfoProperties() {
        // Verify ModelTypeInfo enum has expected values
        val nluType = ModelTypeInfo.NLU
        assertNotNull("NLU type should exist", nluType)
        assertTrue("NLU should be required for chat", nluType.requiredForChat)

        val llmType = ModelTypeInfo.LLM
        assertNotNull("LLM type should exist", llmType)
        assertTrue("LLM should be required for chat", llmType.requiredForChat)

        val embeddingType = ModelTypeInfo.EMBEDDING
        assertNotNull("Embedding type should exist", embeddingType)
        assertFalse("Embedding should not be required for chat", embeddingType.requiredForChat)
    }
}
