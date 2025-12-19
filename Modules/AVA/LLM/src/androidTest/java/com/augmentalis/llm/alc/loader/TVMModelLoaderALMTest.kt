/**
 * Integration Tests for TVMModelLoader with ALM auto-extraction
 *
 * Verifies that TVMModelLoader automatically extracts .ALM files
 * when loading models.
 *
 * Created: 2025-11-24
 */

package com.augmentalis.llm.alc.loader

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.augmentalis.llm.alc.models.ModelConfig
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import org.apache.commons.compress.archivers.tar.TarArchiveEntry
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream

/**
 * Test that TVMModelLoader automatically extracts .ALM archives
 *
 * Note: These tests don't actually load TVM models (that requires real model files),
 * but they verify the extraction logic is triggered correctly.
 */
@RunWith(AndroidJUnit4::class)
class TVMModelLoaderALMTest {

    private lateinit var context: Context
    private lateinit var modelLoader: TVMModelLoader
    private lateinit var extractor: ALMExtractor
    private lateinit var testDir: File

    @Before
    fun setup() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        modelLoader = TVMModelLoader(context)
        extractor = ALMExtractor(context)

        // Setup Timber for test logging
        if (Timber.treeCount == 0) {
            Timber.plant(Timber.DebugTree())
        }

        // Create test directory in standard location
        val externalDir = context.getExternalFilesDir(null)
        testDir = File(externalDir, "ava-ai-models/llm")
        testDir.mkdirs()

        cleanupTestFiles()
    }

    @After
    fun teardown() {
        cleanupTestFiles()
        runBlocking {
            try {
                modelLoader.unloadModel()
            } catch (e: Exception) {
                // Ignore unload errors in tests
            }
        }
    }

    private fun cleanupTestFiles() {
        // Clean test model directories
        testDir.listFiles()?.forEach { file ->
            if (file.name.startsWith("TestALM")) {
                file.deleteRecursively()
            }
        }
    }

    /**
     * Test: ALM extraction is triggered before model loading
     */
    @Test
    fun testALMExtractionTriggeredOnLoad() = runBlocking {
        // Create a test .ALM file
        val modelName = "TestALMModel1"
        val almFile = createTestALM(
            name = modelName,
            files = mapOf(
                "AVALibrary.ADco" to "test library",
                "$modelName.ADco" to "test model",
                "tokenizer.model" to "test tokenizer",
                "ava-model-config.json" to """{"vocab_size": 32000}"""
            )
        )

        // Verify .ALM exists, extracted directory doesn't
        assertTrue(".ALM file should exist", almFile.exists())
        val extractedDir = File(testDir, modelName)
        assertFalse("Extracted directory should not exist yet", extractedDir.exists())

        // Attempt to load model (will fail due to invalid model files, but extraction should happen)
        try {
            val config = ModelConfig(
                modelPath = extractedDir.absolutePath,
                modelName = modelName
            )
            modelLoader.loadModel(config)
        } catch (e: Exception) {
            // Expected - we're using dummy files, not real TVM models
            Timber.d("Expected error during model load: ${e.message}")
        }

        // Verify extraction happened
        assertTrue("Extracted directory should exist after load attempt",
            extractedDir.exists())
        assertTrue("AVALibrary.ADco should be extracted",
            File(extractedDir, "AVALibrary.ADco").exists())
        assertTrue("Model .ADco should be extracted",
            File(extractedDir, "$modelName.ADco").exists())
        assertTrue("Tokenizer should be extracted",
            File(extractedDir, "tokenizer.model").exists())
    }

    /**
     * Test: Multiple .ALM files are all extracted
     */
    @Test
    fun testMultipleALMFilesExtracted() = runBlocking {
        // Create multiple .ALM files
        val model1Name = "TestALMModel2A"
        val model2Name = "TestALMModel2B"

        createTestALM(
            name = model1Name,
            files = mapOf(
                "AVALibrary.ADco" to "lib1",
                "$model1Name.ADco" to "model1",
                "tokenizer.model" to "tok1",
                "ava-model-config.json" to """{"vocab_size": 32000}"""
            )
        )

        createTestALM(
            name = model2Name,
            files = mapOf(
                "AVALibrary.ADco" to "lib2",
                "$model2Name.ADco" to "model2",
                "tokenizer.model" to "tok2",
                "ava-model-config.json" to """{"vocab_size": 32000}"""
            )
        )

        // Attempt to load first model (triggers extraction of all .ALM files)
        try {
            val config = ModelConfig(
                modelPath = File(testDir, model1Name).absolutePath,
                modelName = model1Name
            )
            modelLoader.loadModel(config)
        } catch (e: Exception) {
            // Expected
        }

        // Verify both were extracted
        assertTrue("Model1 should be extracted",
            File(testDir, model1Name).exists())
        assertTrue("Model2 should be extracted",
            File(testDir, model2Name).exists())
    }

    /**
     * Test: Extraction marker prevents re-extraction on second load
     */
    @Test
    fun testExtractionSkippedOnSecondLoad() = runBlocking {
        val modelName = "TestALMModel3"
        createTestALM(
            name = modelName,
            files = mapOf(
                "AVALibrary.ADco" to "original",
                "$modelName.ADco" to "original",
                "tokenizer.model" to "original",
                "ava-model-config.json" to """{"vocab_size": 32000}"""
            )
        )

        // First load attempt (triggers extraction)
        try {
            val config = ModelConfig(
                modelPath = File(testDir, modelName).absolutePath,
                modelName = modelName
            )
            modelLoader.loadModel(config)
        } catch (e: Exception) {
            // Expected
        }

        // Modify extracted file to detect re-extraction
        val extractedDir = File(testDir, modelName)
        File(extractedDir, "AVALibrary.ADco").writeText("modified")

        // Second load attempt (should skip extraction)
        try {
            val config = ModelConfig(
                modelPath = extractedDir.absolutePath,
                modelName = modelName
            )
            modelLoader.loadModel(config)
        } catch (e: Exception) {
            // Expected
        }

        // Verify file wasn't re-extracted (still has modified content)
        assertEquals("File should not be re-extracted",
            "modified",
            File(extractedDir, "AVALibrary.ADco").readText())
    }

    /**
     * Test: Extraction status can be queried
     */
    @Test
    fun testExtractionStatusQuery() = runBlocking {
        val modelName = "TestALMModel4"

        // Before creating .ALM
        var status = extractor.getExtractionStatus(modelName)
        assertTrue("Should be NotFound initially",
            status is ALMExtractor.ExtractionStatus.NotFound)

        // Create .ALM
        createTestALM(
            name = modelName,
            files = mapOf(
                "AVALibrary.ADco" to "lib",
                "$modelName.ADco" to "model",
                "tokenizer.model" to "tok"
            )
        )

        // After creating .ALM
        status = extractor.getExtractionStatus(modelName)
        assertTrue("Should need extraction",
            status is ALMExtractor.ExtractionStatus.NeedsExtraction)

        // Extract via model loader
        try {
            val config = ModelConfig(
                modelPath = File(testDir, modelName).absolutePath,
                modelName = modelName
            )
            modelLoader.loadModel(config)
        } catch (e: Exception) {
            // Expected
        }

        // After extraction
        status = extractor.getExtractionStatus(modelName)
        assertTrue("Should be extracted",
            status is ALMExtractor.ExtractionStatus.Extracted)
    }

    /**
     * Test: Pre-extracted directory works without .ALM file
     */
    @Test
    fun testPreExtractedDirectoryWorks() {
        val modelName = "TestALMModel5"
        val modelDir = File(testDir, modelName)
        modelDir.mkdirs()

        // Create files directly (no .ALM archive)
        File(modelDir, "AVALibrary.ADco").writeText("lib")
        File(modelDir, "$modelName.ADco").writeText("model")
        File(modelDir, "tokenizer.model").writeText("tok")
        File(modelDir, "ava-model-config.json").writeText("""{"vocab_size": 32000}""")

        // Verify no .ALM file exists
        assertFalse("No .ALM file should exist",
            File(testDir, "$modelName.ALM").exists())

        // Attempt to load (should work without extraction)
        try {
            runBlocking {
                val config = ModelConfig(
                    modelPath = modelDir.absolutePath,
                    modelName = modelName
                )
                modelLoader.loadModel(config)
            }
        } catch (e: Exception) {
            // Expected due to invalid model, but no extraction errors
            assertFalse("Should not fail due to extraction",
                e.message?.contains("extraction", ignoreCase = true) ?: false)
        }

        // Verify directory still exists
        assertTrue("Directory should still exist", modelDir.exists())
    }

    // ========== Helper Functions ==========

    /**
     * Create a test .ALM archive in the test directory
     */
    private fun createTestALM(
        name: String,
        files: Map<String, String>
    ): File {
        val almFile = File(testDir, "$name.ALM")

        FileOutputStream(almFile).use { fos ->
            TarArchiveOutputStream(fos).use { tarOut ->
                for ((filename, content) in files) {
                    val contentBytes = content.toByteArray()

                    // Create parent directory entries if needed
                    if (filename.contains("/")) {
                        val parts = filename.split("/")
                        var currentPath = ""
                        for (i in 0 until parts.size - 1) {
                            currentPath += parts[i] + "/"
                            val dirEntry = TarArchiveEntry(currentPath)
                            dirEntry.mode = TarArchiveEntry.DEFAULT_DIR_MODE
                            tarOut.putArchiveEntry(dirEntry)
                            tarOut.closeArchiveEntry()
                        }
                    }

                    // Add file entry
                    val entry = TarArchiveEntry(filename)
                    entry.size = contentBytes.size.toLong()
                    entry.mode = TarArchiveEntry.DEFAULT_FILE_MODE

                    tarOut.putArchiveEntry(entry)
                    tarOut.write(contentBytes)
                    tarOut.closeArchiveEntry()
                }
            }
        }

        return almFile
    }
}
