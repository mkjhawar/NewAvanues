// filename: Universal/AVA/Features/RAG/src/androidTest/kotlin/com/augmentalis/ava/features/rag/embeddings/AONFileManagerTest.kt
// created: 2025-11-24
// author: AVA AI Team
// © Augmentalis Inc, Intelligent Devices LLC

package com.augmentalis.ava.features.rag.embeddings

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * Comprehensive tests for AONFileManager
 *
 * Tests:
 * - Wrapping ONNX to AON format
 * - Unwrapping AON with authentication
 * - Security validations (HMAC, package whitelist, expiry)
 * - Format detection
 * - Error handling
 */
@RunWith(AndroidJUnit4::class)
class AONFileManagerTest {

    private lateinit var context: Context
    private lateinit var testDir: File
    private lateinit var testOnnxFile: File
    private lateinit var testAonFile: File

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        testDir = File(context.cacheDir, "aon-test-${System.currentTimeMillis()}")
        testDir.mkdirs()

        // Create a test ONNX file (mock ONNX data)
        testOnnxFile = File(testDir, "test-model.onnx")
        testOnnxFile.writeBytes(createMockONNXData())

        testAonFile = File(testDir, "test-model.AON")
    }

    @After
    fun tearDown() {
        testDir.deleteRecursively()
    }

    private fun createMockONNXData(): ByteArray {
        // Create mock ONNX data (128 bytes for testing)
        return ByteArray(128) { it.toByte() }
    }

    // ==================== Wrapping Tests ====================

    @Test
    fun testWrapONNX_createsValidAONFile() {
        val result = AONFileManager.wrapONNX(
            onnxFile = testOnnxFile,
            outputFile = testAonFile,
            modelId = "test-model",
            modelVersion = 1,
            allowedPackages = listOf("com.augmentalis.ava"),
            licenseTier = 0
        )

        assertTrue("AON file should be created", result.exists())
        assertTrue("AON file should be larger than ONNX (has header/footer)",
            result.length() > testOnnxFile.length())
    }

    @Test
    fun testWrapONNX_containsCorrectMagicBytes() {
        AONFileManager.wrapONNX(
            onnxFile = testOnnxFile,
            outputFile = testAonFile,
            modelId = "test-model",
            allowedPackages = listOf("com.augmentalis.ava")
        )

        val magicBytes = testAonFile.inputStream().use { it.readNBytes(8) }
        assertArrayEquals(
            "Magic bytes should be AVA-AON\\x01",
            byteArrayOf(0x41, 0x56, 0x41, 0x2D, 0x41, 0x4F, 0x4E, 0x01),
            magicBytes
        )
    }

    @Test
    fun testIsAONFile_detectsAONFormat() {
        AONFileManager.wrapONNX(
            onnxFile = testOnnxFile,
            outputFile = testAonFile,
            modelId = "test-model",
            allowedPackages = listOf("com.augmentalis.ava")
        )

        assertTrue("Should detect AON format", AONFileManager.isAONFile(testAonFile))
    }

    @Test
    fun testIsAONFile_rejectsNonAONFormat() {
        assertFalse("Should reject non-AON format", AONFileManager.isAONFile(testOnnxFile))
    }

    @Test
    fun testWrapONNX_handlesMultiplePackages() {
        val packages = listOf(
            "com.augmentalis.ava",
            "com.augmentalis.avaconnect",
            "com.augmentalis.voiceos"
        )

        val result = AONFileManager.wrapONNX(
            onnxFile = testOnnxFile,
            outputFile = testAonFile,
            modelId = "test-model",
            allowedPackages = packages
        )

        assertTrue("Should support 3 packages", result.exists())
    }

    @Test(expected = IllegalArgumentException::class)
    fun testWrapONNX_rejectsTooManyPackages() {
        val packages = listOf(
            "com.augmentalis.ava",
            "com.augmentalis.avaconnect",
            "com.augmentalis.voiceos",
            "com.augmentalis.extra"  // 4th package - should fail
        )

        AONFileManager.wrapONNX(
            onnxFile = testOnnxFile,
            outputFile = testAonFile,
            modelId = "test-model",
            allowedPackages = packages
        )
    }

    // ==================== Unwrapping Tests ====================

    @Test
    fun testUnwrapAON_successfulWithAuthorizedPackage() {
        // Wrap with current app's package
        AONFileManager.wrapONNX(
            onnxFile = testOnnxFile,
            outputFile = testAonFile,
            modelId = "test-model",
            allowedPackages = listOf(context.packageName)
        )

        // Unwrap should succeed
        val unwrapped = AONFileManager.unwrapAON(testAonFile, context)
        val original = testOnnxFile.readBytes()

        assertArrayEquals(
            "Unwrapped data should match original ONNX",
            original,
            unwrapped
        )
    }

    @Test(expected = SecurityException::class)
    fun testUnwrapAON_failsWithUnauthorizedPackage() {
        // Wrap with different package
        AONFileManager.wrapONNX(
            onnxFile = testOnnxFile,
            outputFile = testAonFile,
            modelId = "test-model",
            allowedPackages = listOf("com.other.app")
        )

        // Unwrap should fail
        AONFileManager.unwrapAON(testAonFile, context)
    }

    @Test(expected = SecurityException::class)
    fun testUnwrapAON_failsWithCorruptedHMAC() {
        // Wrap normally
        AONFileManager.wrapONNX(
            onnxFile = testOnnxFile,
            outputFile = testAonFile,
            modelId = "test-model",
            allowedPackages = listOf(context.packageName)
        )

        // Corrupt HMAC (bytes 8-40 in header)
        val bytes = testAonFile.readBytes()
        bytes[10] = (bytes[10].toInt() xor 0xFF).toByte()  // Flip bits
        testAonFile.writeBytes(bytes)

        // Unwrap should fail
        AONFileManager.unwrapAON(testAonFile, context)
    }

    @Test(expected = SecurityException::class)
    fun testUnwrapAON_failsWithCorruptedONNXData() {
        // Wrap normally
        AONFileManager.wrapONNX(
            onnxFile = testOnnxFile,
            outputFile = testAonFile,
            modelId = "test-model",
            allowedPackages = listOf(context.packageName)
        )

        // Corrupt ONNX data (after 256-byte header)
        val bytes = testAonFile.readBytes()
        bytes[256] = (bytes[256].toInt() xor 0xFF).toByte()
        testAonFile.writeBytes(bytes)

        // Unwrap should fail (integrity check)
        AONFileManager.unwrapAON(testAonFile, context)
    }

    // ==================== Expiry Tests ====================

    @Test
    fun testUnwrapAON_allowsNonExpiredModel() {
        val futureTimestamp = System.currentTimeMillis() + 86400000  // +24 hours

        AONFileManager.wrapONNX(
            onnxFile = testOnnxFile,
            outputFile = testAonFile,
            modelId = "test-model",
            allowedPackages = listOf(context.packageName),
            expiryTimestamp = futureTimestamp
        )

        // Should succeed
        val unwrapped = AONFileManager.unwrapAON(testAonFile, context)
        assertNotNull("Should unwrap non-expired model", unwrapped)
    }

    @Test(expected = SecurityException::class)
    fun testUnwrapAON_rejectsExpiredModel() {
        val pastTimestamp = System.currentTimeMillis() - 86400000  // -24 hours

        AONFileManager.wrapONNX(
            onnxFile = testOnnxFile,
            outputFile = testAonFile,
            modelId = "test-model",
            allowedPackages = listOf(context.packageName),
            expiryTimestamp = pastTimestamp
        )

        // Should fail
        AONFileManager.unwrapAON(testAonFile, context)
    }

    @Test
    fun testUnwrapAON_allowsModelWithNoExpiry() {
        AONFileManager.wrapONNX(
            onnxFile = testOnnxFile,
            outputFile = testAonFile,
            modelId = "test-model",
            allowedPackages = listOf(context.packageName),
            expiryTimestamp = 0  // No expiry
        )

        // Should succeed
        val unwrapped = AONFileManager.unwrapAON(testAonFile, context)
        assertNotNull("Should unwrap model with no expiry", unwrapped)
    }

    // ==================== License Tier Tests ====================

    @Test
    fun testWrapONNX_preservesLicenseTier() {
        val licenseTiers = listOf(0, 1, 2)  // Free, Pro, Enterprise

        licenseTiers.forEach { tier ->
            val outputFile = File(testDir, "test-tier-$tier.AON")

            AONFileManager.wrapONNX(
                onnxFile = testOnnxFile,
                outputFile = outputFile,
                modelId = "test-model-$tier",
                allowedPackages = listOf(context.packageName),
                licenseTier = tier
            )

            // Unwrap should succeed (license validation happens in app layer)
            val unwrapped = AONFileManager.unwrapAON(outputFile, context)
            assertNotNull("Should unwrap tier $tier model", unwrapped)

            outputFile.delete()
        }
    }

    // ==================== Model ID Tests ====================

    @Test
    fun testWrapONNX_handlesVariousModelIds() {
        val modelIds = listOf(
            "AVA-384-Base-INT8",
            "AVA-768-Qual-INT8",
            "test_model_123",
            "model-with-dashes",
            "ModelWithUpperCase"
        )

        modelIds.forEach { modelId ->
            val outputFile = File(testDir, "$modelId.AON")

            val result = AONFileManager.wrapONNX(
                onnxFile = testOnnxFile,
                outputFile = outputFile,
                modelId = modelId,
                allowedPackages = listOf(context.packageName)
            )

            assertTrue("Should handle model ID: $modelId", result.exists())

            outputFile.delete()
        }
    }

    // ==================== Edge Cases ====================

    @Test(expected = IllegalArgumentException::class)
    fun testWrapONNX_rejectsEmptyPackageList() {
        AONFileManager.wrapONNX(
            onnxFile = testOnnxFile,
            outputFile = testAonFile,
            modelId = "test-model",
            allowedPackages = emptyList()  // Should fail
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun testWrapONNX_rejectsNonExistentONNXFile() {
        val nonExistentFile = File(testDir, "does-not-exist.onnx")

        AONFileManager.wrapONNX(
            onnxFile = nonExistentFile,
            outputFile = testAonFile,
            modelId = "test-model",
            allowedPackages = listOf(context.packageName)
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun testUnwrapAON_rejectsNonExistentAONFile() {
        val nonExistentFile = File(testDir, "does-not-exist.AON")

        AONFileManager.unwrapAON(nonExistentFile, context)
    }

    @Test
    fun testWrapONNX_handlesLargeONNXFile() {
        // Create larger ONNX file (1 MB)
        val largeOnnxFile = File(testDir, "large-model.onnx")
        largeOnnxFile.writeBytes(ByteArray(1024 * 1024) { it.toByte() })

        val largeAonFile = File(testDir, "large-model.AON")

        val result = AONFileManager.wrapONNX(
            onnxFile = largeOnnxFile,
            outputFile = largeAonFile,
            modelId = "large-model",
            allowedPackages = listOf(context.packageName)
        )

        assertTrue("Should handle large ONNX file", result.exists())
        assertEquals(
            "AON file should be ONNX + header + footer",
            largeOnnxFile.length() + 256 + 128,
            largeAonFile.length()
        )

        // Verify unwrapping
        val unwrapped = AONFileManager.unwrapAON(largeAonFile, context)
        assertArrayEquals(
            "Large file should unwrap correctly",
            largeOnnxFile.readBytes(),
            unwrapped
        )

        largeOnnxFile.delete()
        largeAonFile.delete()
    }

    // ==================== Integration Tests ====================

    @Test
    fun testRoundTrip_wrapAndUnwrap() {
        // Wrap
        AONFileManager.wrapONNX(
            onnxFile = testOnnxFile,
            outputFile = testAonFile,
            modelId = "test-roundtrip",
            modelVersion = 1,
            allowedPackages = listOf(context.packageName),
            expiryTimestamp = 0,
            licenseTier = 0,
            encrypt = false
        )

        // Unwrap
        val unwrapped = AONFileManager.unwrapAON(testAonFile, context)

        // Verify
        val original = testOnnxFile.readBytes()
        assertArrayEquals(
            "Round-trip should preserve ONNX data",
            original,
            unwrapped
        )
    }

    @Test
    fun testMultipleWrapUnwrapCycles() {
        repeat(5) { iteration ->
            val aonFile = File(testDir, "test-cycle-$iteration.AON")

            // Wrap
            AONFileManager.wrapONNX(
                onnxFile = testOnnxFile,
                outputFile = aonFile,
                modelId = "test-cycle-$iteration",
                allowedPackages = listOf(context.packageName)
            )

            // Unwrap
            val unwrapped = AONFileManager.unwrapAON(aonFile, context)

            // Verify
            assertArrayEquals(
                "Cycle $iteration should preserve data",
                testOnnxFile.readBytes(),
                unwrapped
            )

            aonFile.delete()
        }
    }
}
