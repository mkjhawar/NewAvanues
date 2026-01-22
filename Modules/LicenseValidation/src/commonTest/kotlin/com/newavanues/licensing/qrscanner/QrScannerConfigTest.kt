package com.newavanues.licensing.qrscanner

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class QrScannerConfigTest {

    @Test
    fun `DEFAULT config has expected defaults`() {
        val config = QrScannerConfig.DEFAULT
        assertEquals(CameraFacing.BACK, config.cameraFacing)
        assertEquals(ScanMode.SINGLE, config.scanMode)
        assertTrue(config.enableHapticFeedback)
        assertFalse(config.enableSoundFeedback)
        assertEquals(setOf(QrFormat.QR_CODE), config.acceptedFormats)
        assertTrue(config.showOverlay)
        assertEquals(1000L, config.scanDelayMs)
        assertTrue(config.autoStopOnSuccess)
        assertEquals(ScannerResolution.HD, config.resolution)
        assertTrue(config.enableTorch)
    }

    @Test
    fun `CONTINUOUS config has correct settings`() {
        val config = QrScannerConfig.CONTINUOUS
        assertEquals(ScanMode.CONTINUOUS, config.scanMode)
        assertFalse(config.autoStopOnSuccess)
        assertEquals(500L, config.scanDelayMs)
    }

    @Test
    fun `FILE_IMPORT config has correct settings`() {
        val config = QrScannerConfig.FILE_IMPORT
        assertEquals(ScanMode.SINGLE, config.scanMode)
        assertFalse(config.enableHapticFeedback)
        assertFalse(config.showOverlay)
    }

    @Test
    fun `acceptsFormat returns true for accepted format`() {
        val config = QrScannerConfig(
            acceptedFormats = setOf(QrFormat.QR_CODE, QrFormat.DATA_MATRIX)
        )
        assertTrue(config.acceptsFormat(QrFormat.QR_CODE))
        assertTrue(config.acceptsFormat(QrFormat.DATA_MATRIX))
    }

    @Test
    fun `acceptsFormat returns false for non-accepted format`() {
        val config = QrScannerConfig(
            acceptedFormats = setOf(QrFormat.QR_CODE)
        )
        assertFalse(config.acceptsFormat(QrFormat.DATA_MATRIX))
        assertFalse(config.acceptsFormat(QrFormat.AZTEC))
    }

    @Test
    fun `acceptsFormat returns true for any format when acceptedFormats is empty`() {
        val config = QrScannerConfig(acceptedFormats = emptySet())
        assertTrue(config.acceptsFormat(QrFormat.QR_CODE))
        assertTrue(config.acceptsFormat(QrFormat.DATA_MATRIX))
        assertTrue(config.acceptsFormat(QrFormat.AZTEC))
        assertTrue(config.acceptsFormat(QrFormat.UNKNOWN))
    }

    @Test
    fun `CameraFacing toggle returns opposite facing`() {
        assertEquals(CameraFacing.FRONT, CameraFacing.BACK.toggle())
        assertEquals(CameraFacing.BACK, CameraFacing.FRONT.toggle())
    }

    @Test
    fun `ScannerResolution has correct dimensions`() {
        assertEquals(640, ScannerResolution.SD.width)
        assertEquals(480, ScannerResolution.SD.height)

        assertEquals(1280, ScannerResolution.HD.width)
        assertEquals(720, ScannerResolution.HD.height)

        assertEquals(1920, ScannerResolution.FULL_HD.width)
        assertEquals(1080, ScannerResolution.FULL_HD.height)
    }

    @Test
    fun `custom config preserves all values`() {
        val config = QrScannerConfig(
            cameraFacing = CameraFacing.FRONT,
            scanMode = ScanMode.CONTINUOUS,
            enableHapticFeedback = false,
            enableSoundFeedback = true,
            acceptedFormats = setOf(QrFormat.QR_CODE, QrFormat.AZTEC),
            showOverlay = false,
            scanDelayMs = 2000L,
            autoStopOnSuccess = false,
            resolution = ScannerResolution.FULL_HD,
            enableTorch = false
        )

        assertEquals(CameraFacing.FRONT, config.cameraFacing)
        assertEquals(ScanMode.CONTINUOUS, config.scanMode)
        assertFalse(config.enableHapticFeedback)
        assertTrue(config.enableSoundFeedback)
        assertEquals(setOf(QrFormat.QR_CODE, QrFormat.AZTEC), config.acceptedFormats)
        assertFalse(config.showOverlay)
        assertEquals(2000L, config.scanDelayMs)
        assertFalse(config.autoStopOnSuccess)
        assertEquals(ScannerResolution.FULL_HD, config.resolution)
        assertFalse(config.enableTorch)
    }
}
