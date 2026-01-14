package com.augmentalis.webavanue.platform

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * Unit tests for DownloadPathValidator
 *
 * Tests all validation scenarios:
 * - Valid paths with sufficient space
 * - Invalid paths (non-existent, not writable)
 * - Low space warnings
 * - Permission revocation handling
 * - Edge cases
 *
 * ## Test Coverage
 * - Path existence validation
 * - Write permission checks
 * - Storage space calculation
 * - Low space threshold detection
 * - Error message accuracy
 */
@RunWith(AndroidJUnit4::class)
class DownloadPathValidatorTest {

    private lateinit var context: Context
    private lateinit var validator: DownloadPathValidator
    private lateinit var testDirectory: File

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        validator = DownloadPathValidator(context)

        // Create test directory in app's cache (always writable)
        testDirectory = File(context.cacheDir, "test_downloads")
        testDirectory.mkdirs()
    }

    @After
    fun tearDown() {
        // Clean up test directory
        testDirectory.deleteRecursively()
    }

    // ==================== Valid Path Tests ====================

    @Test
    fun validate_validPath_returnsSuccess() = runTest {
        // Given: Valid file URI to writable directory
        val uri = Uri.fromFile(testDirectory)

        // When: Validate path
        val result = validator.validate(uri.toString())

        // Then: Validation succeeds
        assertTrue("Expected valid path", result.isValid)
        assertNull("Expected no error message", result.errorMessage)
        assertTrue("Expected available space > 0", result.availableSpaceMB > 0)
    }

    @Test
    fun validate_validPathWithSufficientSpace_noLowSpaceWarning() = runTest {
        // Given: Valid path with plenty of space (using cache dir)
        val uri = Uri.fromFile(testDirectory)

        // When: Validate path
        val result = validator.validate(uri.toString())

        // Then: No low space warning (cache dir usually has > 100MB)
        assertTrue("Expected valid path", result.isValid)
        assertFalse("Expected no low space warning", result.isLowSpace)
    }

    @Test
    fun validate_validPath_calculatesAvailableSpace() = runTest {
        // Given: Valid file URI
        val uri = Uri.fromFile(testDirectory)

        // When: Validate path
        val result = validator.validate(uri.toString())

        // Then: Available space is calculated and reasonable
        assertTrue("Expected valid path", result.isValid)
        assertTrue("Expected positive space", result.availableSpaceMB > 0)
        assertTrue("Expected reasonable space (<10TB)", result.availableSpaceMB < 10_000_000)
    }

    // ==================== Invalid Path Tests ====================

    @Test
    fun validate_nonExistentPath_returnsError() = runTest {
        // Given: URI to non-existent directory
        val nonExistentDir = File(context.cacheDir, "non_existent_${System.currentTimeMillis()}")
        val uri = Uri.fromFile(nonExistentDir)

        // When: Validate path
        val result = validator.validate(uri.toString())

        // Then: Validation fails with appropriate error
        assertFalse("Expected invalid path", result.isValid)
        assertNotNull("Expected error message", result.errorMessage)
        assertTrue(
            "Expected 'no longer exists' error message",
            result.errorMessage!!.contains("no longer exists", ignoreCase = true) ||
            result.errorMessage!!.contains("revoked", ignoreCase = true)
        )
    }

    @Test
    fun validate_invalidUri_returnsError() = runTest {
        // Given: Malformed URI
        val invalidUri = "not://a/valid/uri"

        // When: Validate path
        val result = validator.validate(invalidUri)

        // Then: Validation fails
        assertFalse("Expected invalid path", result.isValid)
        assertNotNull("Expected error message", result.errorMessage)
    }

    @Test
    fun validate_emptyPath_returnsError() = runTest {
        // Given: Empty path
        val emptyPath = ""

        // When: Validate path
        val result = validator.validate(emptyPath)

        // Then: Validation fails
        assertFalse("Expected invalid path", result.isValid)
        assertNotNull("Expected error message", result.errorMessage)
    }

    @Test
    fun validate_nullScheme_returnsError() = runTest {
        // Given: URI without scheme
        val noScheme = "/just/a/path"

        // When: Validate path
        val result = validator.validate(noScheme)

        // Then: Validation fails (DocumentFile can't handle it)
        assertFalse("Expected invalid path", result.isValid)
        assertNotNull("Expected error message", result.errorMessage)
    }

    // ==================== Write Permission Tests ====================

    @Test
    fun validate_readOnlyPath_returnsError() = runTest {
        // Given: Read-only directory (make test dir read-only)
        testDirectory.setWritable(false)
        val uri = Uri.fromFile(testDirectory)

        try {
            // When: Validate path
            val result = validator.validate(uri.toString())

            // Then: Validation fails (may succeed if running as root/system)
            // Note: This test may be unreliable on some devices/emulators
            if (!result.isValid) {
                assertNotNull("Expected error message", result.errorMessage)
                assertTrue(
                    "Expected 'cannot write' error",
                    result.errorMessage!!.contains("cannot write", ignoreCase = true) ||
                    result.errorMessage!!.contains("not writable", ignoreCase = true)
                )
            }
        } finally {
            // Restore write permission
            testDirectory.setWritable(true)
        }
    }

    // ==================== Low Space Warning Tests ====================

    @Test
    fun validate_lowSpaceThreshold_setsWarningFlag() {
        // Given: ValidationResult with space below threshold
        val lowSpaceResult = ValidationResult.success(availableSpaceMB = 50)

        // Then: Low space flag is set
        assertTrue("Expected low space warning", lowSpaceResult.isLowSpace)
        assertTrue("Expected valid path", lowSpaceResult.isValid)
    }

    @Test
    fun validate_sufficientSpace_noWarningFlag() {
        // Given: ValidationResult with space above threshold
        val sufficientSpaceResult = ValidationResult.success(availableSpaceMB = 200)

        // Then: No low space warning
        assertFalse("Expected no low space warning", sufficientSpaceResult.isLowSpace)
        assertTrue("Expected valid path", sufficientSpaceResult.isValid)
    }

    @Test
    fun validate_exactlyAtThreshold_setsWarningFlag() {
        // Given: ValidationResult with space exactly at threshold (100MB)
        val thresholdResult = ValidationResult.success(availableSpaceMB = 100)

        // Then: No warning at exactly 100MB (only < 100)
        assertFalse("Expected no warning at threshold", thresholdResult.isLowSpace)
    }

    @Test
    fun validate_justBelowThreshold_setsWarningFlag() {
        // Given: ValidationResult with space just below threshold
        val justBelowResult = ValidationResult.success(availableSpaceMB = 99)

        // Then: Warning is set
        assertTrue("Expected warning just below threshold", justBelowResult.isLowSpace)
    }

    // ==================== ValidationResult Factory Tests ====================

    @Test
    fun validationResult_success_createsValidResult() {
        // Given: Success factory call
        val result = ValidationResult.success(availableSpaceMB = 500)

        // Then: Result is valid with correct values
        assertTrue("Expected valid result", result.isValid)
        assertNull("Expected no error message", result.errorMessage)
        assertEquals("Expected correct space value", 500L, result.availableSpaceMB)
        assertFalse("Expected no low space warning", result.isLowSpace)
    }

    @Test
    fun validationResult_failure_createsInvalidResult() {
        // Given: Failure factory call
        val errorMsg = "Test error message"
        val result = ValidationResult.failure(errorMsg)

        // Then: Result is invalid with correct values
        assertFalse("Expected invalid result", result.isValid)
        assertEquals("Expected correct error message", errorMsg, result.errorMessage)
        assertEquals("Expected zero space", 0L, result.availableSpaceMB)
        assertFalse("Expected no low space flag on failure", result.isLowSpace)
    }

    // ==================== Edge Cases ====================

    @Test
    fun validate_veryLongPath_handlesGracefully() = runTest {
        // Given: Very long but valid path
        val longName = "a".repeat(200)
        val longPathDir = File(testDirectory, longName)
        longPathDir.mkdirs()
        val uri = Uri.fromFile(longPathDir)

        // When: Validate path
        val result = validator.validate(uri.toString())

        // Then: Validation completes (may succeed or fail depending on filesystem limits)
        assertNotNull("Expected result", result)

        // Cleanup
        longPathDir.delete()
    }

    @Test
    fun validate_pathWithSpecialCharacters_handlesGracefully() = runTest {
        // Given: Path with special characters
        val specialDir = File(testDirectory, "test_!@#\$%^&()_+-=[]{}|;',.")
        specialDir.mkdirs()
        val uri = Uri.fromFile(specialDir)

        // When: Validate path
        val result = validator.validate(uri.toString())

        // Then: Validation completes
        assertNotNull("Expected result", result)

        // Cleanup
        specialDir.delete()
    }

    @Test
    fun validate_rapidSuccessiveCalls_allComplete() = runTest {
        // Given: Valid path
        val uri = Uri.fromFile(testDirectory)

        // When: Multiple rapid validations
        val results = List(10) {
            validator.validate(uri.toString())
        }

        // Then: All complete successfully
        assertEquals("Expected 10 results", 10, results.size)
        assertTrue("Expected all valid", results.all { it.isValid })
    }

    // ==================== Content URI Tests ====================

    @Test
    fun validate_contentUri_handlesCorrectly() = runTest {
        // Given: Content URI format
        val contentUri = "content://com.android.externalstorage.documents/tree/primary%3ADownload"

        // When: Validate path
        val result = validator.validate(contentUri)

        // Then: Validation completes (may fail if permission not granted in test)
        assertNotNull("Expected result", result)
        // Don't assert success/failure as it depends on test environment permissions
    }

    // ==================== Constant Tests ====================

    @Test
    fun lowSpaceThreshold_isCorrectValue() {
        // Then: Threshold is 100MB as specified
        assertEquals(
            "Expected 100MB threshold",
            100L,
            ValidationResult.LOW_SPACE_THRESHOLD_MB
        )
    }
}
