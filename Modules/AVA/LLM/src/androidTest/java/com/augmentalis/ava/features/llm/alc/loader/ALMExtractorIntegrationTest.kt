/**
 * Integration Tests for ALMExtractor
 *
 * Tests the automatic extraction of .ALM (AVA LLM Model) archives
 * on real Android device/emulator.
 *
 * Created: 2025-11-24
 */

package com.augmentalis.ava.features.llm.alc.loader

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.FileOutputStream
import org.apache.commons.compress.archivers.tar.TarArchiveEntry
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream

/**
 * Integration tests for ALMExtractor
 *
 * These tests create actual .ALM files and verify extraction works correctly.
 */
@RunWith(AndroidJUnit4::class)
class ALMExtractorIntegrationTest {

    private lateinit var context: Context
    private lateinit var extractor: ALMExtractor
    private lateinit var testDir: File

    @Before
    fun setup() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        extractor = ALMExtractor(context)

        // Create test directory
        testDir = File(context.getExternalFilesDir(null), "test-alm")
        testDir.mkdirs()

        // Clean any existing test files
        cleanupTestFiles()
    }

    @After
    fun teardown() {
        cleanupTestFiles()
    }

    private fun cleanupTestFiles() {
        testDir.deleteRecursively()
    }

    /**
     * Test: Create and extract a simple .ALM archive
     */
    @Test
    fun testExtractSimpleALM() = runBlocking {
        // Create a test .ALM archive
        val almFile = createTestALM(
            name = "TestModel",
            files = mapOf(
                "AVALibrary.ADco" to "library content",
                "TestModel.ADco" to "model content",
                "tokenizer.model" to "tokenizer content"
            )
        )

        // Extract the archive
        val extractedDir = extractor.extractALMFile(almFile)

        // Verify extraction succeeded
        assertNotNull("Extraction should succeed", extractedDir)
        assertTrue("Extracted directory should exist", extractedDir!!.exists())
        assertTrue("Extracted directory should be a directory", extractedDir.isDirectory)

        // Verify files were extracted
        assertTrue("AVALibrary.ADco should exist",
            File(extractedDir, "AVALibrary.ADco").exists())
        assertTrue("TestModel.ADco should exist",
            File(extractedDir, "TestModel.ADco").exists())
        assertTrue("tokenizer.model should exist",
            File(extractedDir, "tokenizer.model").exists())

        // Verify file contents
        assertEquals("library content",
            File(extractedDir, "AVALibrary.ADco").readText())
        assertEquals("model content",
            File(extractedDir, "TestModel.ADco").readText())
        assertEquals("tokenizer content",
            File(extractedDir, "tokenizer.model").readText())
    }

    /**
     * Test: Extraction creates marker file
     */
    @Test
    fun testExtractionCreatesMarker() = runBlocking {
        val almFile = createTestALM(
            name = "TestModel2",
            files = mapOf(
                "AVALibrary.ADco" to "lib",
                "TestModel2.ADco" to "model",
                "tokenizer.model" to "tok"
            )
        )

        val extractedDir = extractor.extractALMFile(almFile)
        assertNotNull(extractedDir)

        // Verify marker file exists
        val markerFile = File(extractedDir!!, ".alm_extracted")
        assertTrue("Marker file should exist", markerFile.exists())

        // Verify marker contains timestamp
        val markerContent = markerFile.readText().trim()
        assertEquals("Marker should contain ALM timestamp",
            almFile.lastModified().toString(), markerContent)
    }

    /**
     * Test: Repeated extraction is skipped (uses cache)
     */
    @Test
    fun testRepeatedExtractionSkipped() = runBlocking {
        val almFile = createTestALM(
            name = "TestModel3",
            files = mapOf(
                "AVALibrary.ADco" to "original",
                "TestModel3.ADco" to "original",
                "tokenizer.model" to "original"
            )
        )

        // First extraction
        val extractedDir1 = extractor.extractALMFile(almFile)
        assertNotNull(extractedDir1)

        // Modify extracted file
        File(extractedDir1!!, "AVALibrary.ADco").writeText("modified")

        // Second extraction (should be skipped)
        val extractedDir2 = extractor.extractALMFile(almFile)
        assertNotNull(extractedDir2)

        // Verify file wasn't re-extracted (still has modified content)
        assertEquals("modified",
            File(extractedDir2!!, "AVALibrary.ADco").readText())
    }

    /**
     * Test: Changed .ALM triggers re-extraction
     */
    @Test
    fun testChangedALMTriggersReExtraction() = runBlocking {
        val almFile = createTestALM(
            name = "TestModel4",
            files = mapOf(
                "AVALibrary.ADco" to "version1",
                "TestModel4.ADco" to "version1",
                "tokenizer.model" to "version1"
            )
        )

        // First extraction
        val extractedDir1 = extractor.extractALMFile(almFile)
        assertNotNull(extractedDir1)
        assertEquals("version1", File(extractedDir1!!, "AVALibrary.ADco").readText())

        // Wait a moment to ensure timestamp changes
        Thread.sleep(100)

        // Create new .ALM with updated content
        val newAlmFile = createTestALM(
            name = "TestModel4",
            files = mapOf(
                "AVALibrary.ADco" to "version2",
                "TestModel4.ADco" to "version2",
                "tokenizer.model" to "version2"
            )
        )

        // Second extraction (should re-extract due to timestamp change)
        val extractedDir2 = extractor.extractALMFile(newAlmFile)
        assertNotNull(extractedDir2)

        // Verify content was updated
        assertEquals("version2",
            File(extractedDir2!!, "AVALibrary.ADco").readText())
    }

    /**
     * Test: Extraction verification fails without required files
     */
    @Test
    fun testExtractionVerificationFailsWithoutRequiredFiles() = runBlocking {
        // Create .ALM without required files
        val almFile = createTestALM(
            name = "InvalidModel",
            files = mapOf(
                "some_file.txt" to "content"
            )
        )

        // Extraction should fail verification
        val extractedDir = extractor.extractALMFile(almFile)

        // Should return null due to failed verification
        assertNull("Extraction should fail without required files", extractedDir)
    }

    /**
     * Test: Scan and extract multiple .ALM files
     */
    @Test
    fun testExtractMultipleALMFiles() = runBlocking {
        // Create multiple .ALM files
        createTestALM(
            name = "Model1",
            files = mapOf(
                "AVALibrary.ADco" to "lib1",
                "Model1.ADco" to "model1",
                "tokenizer.model" to "tok1"
            )
        )

        createTestALM(
            name = "Model2",
            files = mapOf(
                "AVALibrary.ADco" to "lib2",
                "Model2.ADco" to "model2",
                "tokenizer.model" to "tok2"
            )
        )

        // Extract all
        val extractedDirs = extractor.extractAllALMFiles()

        // Verify both were extracted
        assertTrue("Should extract at least 2 models", extractedDirs.size >= 2)

        // Note: extractAllALMFiles scans standard locations, so may find more
        // Just verify our test models are present
        assertTrue("Model1 should be extracted",
            extractedDirs.any { it.name == "Model1" })
        assertTrue("Model2 should be extracted",
            extractedDirs.any { it.name == "Model2" })
    }

    /**
     * Test: Extraction status detection
     */
    @Test
    fun testExtractionStatusDetection() {
        // Test NotFound status
        var status = extractor.getExtractionStatus("NonExistentModel")
        assertTrue("Should be NotFound", status is ALMExtractor.ExtractionStatus.NotFound)

        // Create .ALM file
        val almFile = runBlocking {
            createTestALM(
                name = "StatusTest",
                files = mapOf(
                    "AVALibrary.ADco" to "lib",
                    "StatusTest.ADco" to "model",
                    "tokenizer.model" to "tok"
                )
            )
        }

        // Test NeedsExtraction status
        status = extractor.getExtractionStatus("StatusTest")
        assertTrue("Should need extraction",
            status is ALMExtractor.ExtractionStatus.NeedsExtraction)

        // Extract
        runBlocking {
            extractor.extractALMFile(almFile)
        }

        // Test Extracted status
        status = extractor.getExtractionStatus("StatusTest")
        assertTrue("Should be extracted",
            status is ALMExtractor.ExtractionStatus.Extracted)
    }

    /**
     * Test: needsExtraction check
     */
    @Test
    fun testNeedsExtractionCheck() = runBlocking {
        val almFile = createTestALM(
            name = "NeedsExtractionTest",
            files = mapOf(
                "AVALibrary.ADco" to "lib",
                "NeedsExtractionTest.ADco" to "model",
                "tokenizer.model" to "tok"
            )
        )

        val modelDir = File(almFile.parent, "NeedsExtractionTest")

        // Before extraction: should need extraction
        assertTrue("Should need extraction", extractor.needsExtraction(modelDir))

        // Extract
        extractor.extractALMFile(almFile)

        // After extraction: should NOT need extraction
        assertFalse("Should not need extraction", extractor.needsExtraction(modelDir))
    }

    /**
     * Test: Extraction with nested directories
     */
    @Test
    fun testExtractionWithNestedDirectories() = runBlocking {
        val almFile = createTestALM(
            name = "NestedModel",
            files = mapOf(
                "AVALibrary.ADco" to "lib",
                "NestedModel.ADco" to "model",
                "tokenizer.model" to "tok",
                "subdir/config.json" to "config",
                "subdir/weights/weight1.bin" to "weight1"
            )
        )

        val extractedDir = extractor.extractALMFile(almFile)
        assertNotNull(extractedDir)

        // Verify nested files exist
        assertTrue("Nested config should exist",
            File(extractedDir!!, "subdir/config.json").exists())
        assertTrue("Nested weight should exist",
            File(extractedDir, "subdir/weights/weight1.bin").exists())

        // Verify content
        assertEquals("config", File(extractedDir, "subdir/config.json").readText())
        assertEquals("weight1", File(extractedDir, "subdir/weights/weight1.bin").readText())
    }

    /**
     * Test: Cleanup .ALM files after extraction
     */
    @Test
    fun testCleanupALMFiles() = runBlocking {
        // Create and extract .ALM
        val almFile = createTestALM(
            name = "CleanupTest",
            files = mapOf(
                "AVALibrary.ADco" to "lib",
                "CleanupTest.ADco" to "model",
                "tokenizer.model" to "tok"
            )
        )

        extractor.extractALMFile(almFile)
        assertTrue(".ALM file should exist before cleanup", almFile.exists())

        // Cleanup with deletion
        extractor.cleanupALMFiles(deleteAfterExtraction = true)

        // Verify .ALM was deleted
        assertFalse(".ALM file should be deleted after cleanup", almFile.exists())

        // Verify extracted directory still exists
        val extractedDir = File(almFile.parent, "CleanupTest")
        assertTrue("Extracted directory should still exist", extractedDir.exists())
    }

    // ========== Helper Functions ==========

    /**
     * Create a test .ALM archive with specified files
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
