package com.newavanues.licensing.qrscanner

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class QrScanResultTest {

    @Test
    fun `Success result isSuccess returns true`() {
        val result = QrScanResult.Success(content = "test-content")
        assertTrue(result.isSuccess)
    }

    @Test
    fun `Success result contentOrNull returns content`() {
        val result = QrScanResult.Success(content = "test-content")
        assertEquals("test-content", result.contentOrNull())
    }

    @Test
    fun `Success result errorOrNull returns null`() {
        val result = QrScanResult.Success(content = "test-content")
        assertNull(result.errorOrNull())
    }

    @Test
    fun `Success result with format preserves format`() {
        val result = QrScanResult.Success(
            content = "test-content",
            format = QrFormat.DATA_MATRIX
        )
        assertEquals(QrFormat.DATA_MATRIX, result.format)
    }

    @Test
    fun `Error result isSuccess returns false`() {
        val result = QrScanResult.Error(error = QrScanError.NO_CODE_FOUND)
        assertFalse(result.isSuccess)
    }

    @Test
    fun `Error result contentOrNull returns null`() {
        val result = QrScanResult.Error(error = QrScanError.NO_CODE_FOUND)
        assertNull(result.contentOrNull())
    }

    @Test
    fun `Error result errorOrNull returns error`() {
        val result = QrScanResult.Error(error = QrScanError.NO_CODE_FOUND)
        assertEquals(QrScanError.NO_CODE_FOUND, result.errorOrNull())
    }

    @Test
    fun `Error result with message preserves message`() {
        val result = QrScanResult.Error(
            error = QrScanError.PROCESSING_ERROR,
            message = "Custom error message"
        )
        assertEquals("Custom error message", result.message)
    }

    @Test
    fun `PermissionDenied isSuccess returns false`() {
        assertFalse(QrScanResult.PermissionDenied.isSuccess)
    }

    @Test
    fun `NoCameraAvailable isSuccess returns false`() {
        assertFalse(QrScanResult.NoCameraAvailable.isSuccess)
    }

    @Test
    fun `Cancelled isSuccess returns false`() {
        assertFalse(QrScanResult.Cancelled.isSuccess)
    }

    @Test
    fun `QrScanError toUserMessage returns meaningful messages`() {
        // Test a few error messages
        assertTrue(QrScanError.CAMERA_INIT_FAILED.toUserMessage().isNotBlank())
        assertTrue(QrScanError.NO_CODE_FOUND.toUserMessage().isNotBlank())
        assertTrue(QrScanError.DECODE_FAILED.toUserMessage().isNotBlank())
        assertTrue(QrScanError.UNKNOWN.toUserMessage().isNotBlank())
    }

    @Test
    fun `QrFormat enum has expected values`() {
        val formats = QrFormat.entries
        assertTrue(formats.contains(QrFormat.QR_CODE))
        assertTrue(formats.contains(QrFormat.DATA_MATRIX))
        assertTrue(formats.contains(QrFormat.AZTEC))
        assertTrue(formats.contains(QrFormat.PDF417))
    }
}
