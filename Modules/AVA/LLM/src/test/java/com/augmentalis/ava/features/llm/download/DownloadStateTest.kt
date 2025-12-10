package com.augmentalis.ava.features.llm.download

import org.junit.Assert.*
import org.junit.Test

/**
 * Tests for DownloadState sealed class and extension functions
 * P8 Week 3: Strategic coverage for LLM download state management
 */
class DownloadStateTest {

    // ========================================
    // Downloading state tests
    // ========================================

    @Test
    fun `test Downloading state creation`() {
        val state = DownloadState.Downloading(
            modelId = "model-123",
            bytesDownloaded = 50_000_000L, // 50 MB
            totalBytes = 100_000_000L, // 100 MB
            progress = 0.5f,
            speedBytesPerSecond = 1_000_000L, // 1 MB/s
            estimatedTimeRemainingMs = 50_000L // 50 seconds
        )

        assertEquals("model-123", state.modelId)
        assertEquals(50_000_000L, state.bytesDownloaded)
        assertEquals(100_000_000L, state.totalBytes)
        assertEquals(0.5f, state.progress, 0.001f)
        assertEquals(1_000_000L, state.speedBytesPerSecond)
        assertEquals(50_000L, state.estimatedTimeRemainingMs)
    }

    @Test
    fun `test Downloading getProgressPercentage()`() {
        val state = DownloadState.Downloading(
            modelId = "model", bytesDownloaded = 0, totalBytes = 100,
            progress = 0.753f
        )

        assertEquals(75, state.getProgressPercentage())
    }

    @Test
    fun `test Downloading getProgressPercentage() with zero progress`() {
        val state = DownloadState.Downloading(
            modelId = "model", bytesDownloaded = 0, totalBytes = 100,
            progress = 0.0f
        )

        assertEquals(0, state.getProgressPercentage())
    }

    @Test
    fun `test Downloading getProgressPercentage() with complete progress`() {
        val state = DownloadState.Downloading(
            modelId = "model", bytesDownloaded = 100, totalBytes = 100,
            progress = 1.0f
        )

        assertEquals(100, state.getProgressPercentage())
    }

    @Test
    fun `test Downloading getDownloadedSize() formats bytes`() {
        val state = DownloadState.Downloading(
            modelId = "model", bytesDownloaded = 512L, totalBytes = 1000,
            progress = 0.5f
        )

        assertEquals("512 B", state.getDownloadedSize())
    }

    @Test
    fun `test Downloading getDownloadedSize() formats KB`() {
        val state = DownloadState.Downloading(
            modelId = "model", bytesDownloaded = 5120L, // 5 KB
            totalBytes = 10000, progress = 0.5f
        )

        assertEquals("5 KB", state.getDownloadedSize())
    }

    @Test
    fun `test Downloading getDownloadedSize() formats MB`() {
        val state = DownloadState.Downloading(
            modelId = "model", bytesDownloaded = 5_242_880L, // 5 MB
            totalBytes = 10_000_000, progress = 0.5f
        )

        assertEquals("5 MB", state.getDownloadedSize())
    }

    @Test
    fun `test Downloading getDownloadedSize() formats GB`() {
        val state = DownloadState.Downloading(
            modelId = "model", bytesDownloaded = 5_368_709_120L, // 5 GB
            totalBytes = 10_000_000_000, progress = 0.5f
        )

        assertEquals("5 GB", state.getDownloadedSize())
    }

    @Test
    fun `test Downloading getTotalSize()`() {
        val state = DownloadState.Downloading(
            modelId = "model", bytesDownloaded = 50_000_000,
            totalBytes = 100_000_000L, // 100 MB
            progress = 0.5f
        )

        assertEquals("95 MB", state.getTotalSize())
    }

    @Test
    fun `test Downloading getSpeed()`() {
        val state = DownloadState.Downloading(
            modelId = "model", bytesDownloaded = 0, totalBytes = 100,
            progress = 0.0f, speedBytesPerSecond = 2_097_152L // 2 MB/s
        )

        assertEquals("2 MB/s", state.getSpeed())
    }

    @Test
    fun `test Downloading getSpeed() with zero speed`() {
        val state = DownloadState.Downloading(
            modelId = "model", bytesDownloaded = 0, totalBytes = 100,
            progress = 0.0f, speedBytesPerSecond = 0L
        )

        assertEquals("0 B/s", state.getSpeed())
    }

    @Test
    fun `test Downloading getTimeRemaining() in seconds`() {
        val state = DownloadState.Downloading(
            modelId = "model", bytesDownloaded = 0, totalBytes = 100,
            progress = 0.0f, estimatedTimeRemainingMs = 30_000L // 30 seconds
        )

        assertEquals("30s", state.getTimeRemaining())
    }

    @Test
    fun `test Downloading getTimeRemaining() in minutes`() {
        val state = DownloadState.Downloading(
            modelId = "model", bytesDownloaded = 0, totalBytes = 100,
            progress = 0.0f, estimatedTimeRemainingMs = 150_000L // 2 min 30 sec
        )

        assertEquals("2m 30s", state.getTimeRemaining())
    }

    @Test
    fun `test Downloading getTimeRemaining() in hours`() {
        val state = DownloadState.Downloading(
            modelId = "model", bytesDownloaded = 0, totalBytes = 100,
            progress = 0.0f, estimatedTimeRemainingMs = 7_380_000L // 2h 3m
        )

        assertEquals("2h 3m", state.getTimeRemaining())
    }

    @Test
    fun `test Downloading getTimeRemaining() exactly 1 minute`() {
        val state = DownloadState.Downloading(
            modelId = "model", bytesDownloaded = 0, totalBytes = 100,
            progress = 0.0f, estimatedTimeRemainingMs = 60_000L
        )

        assertEquals("1m 0s", state.getTimeRemaining())
    }

    // ========================================
    // Paused state tests
    // ========================================

    @Test
    fun `test Paused state creation without reason`() {
        val state = DownloadState.Paused(
            modelId = "model-pause",
            bytesDownloaded = 25_000_000L,
            totalBytes = 100_000_000L,
            progress = 0.25f,
            pauseReason = null
        )

        assertEquals("model-pause", state.modelId)
        assertEquals(0.25f, state.progress, 0.001f)
        assertNull(state.pauseReason)
    }

    @Test
    fun `test Paused state creation with reason`() {
        val state = DownloadState.Paused(
            modelId = "model-pause",
            bytesDownloaded = 25_000_000L,
            totalBytes = 100_000_000L,
            progress = 0.25f,
            pauseReason = "User paused"
        )

        assertEquals("User paused", state.pauseReason)
    }

    // ========================================
    // Completed state tests
    // ========================================

    @Test
    fun `test Completed state creation`() {
        val state = DownloadState.Completed(
            modelId = "model-complete",
            filePath = "/data/models/model.bin",
            fileSize = 100_000_000L,
            checksum = "abc123",
            downloadDurationMs = 120_000L
        )

        assertEquals("model-complete", state.modelId)
        assertEquals("/data/models/model.bin", state.filePath)
        assertEquals(100_000_000L, state.fileSize)
        assertEquals("abc123", state.checksum)
        assertEquals(120_000L, state.downloadDurationMs)
    }

    @Test
    fun `test Completed state without checksum`() {
        val state = DownloadState.Completed(
            modelId = "model", filePath = "/path", fileSize = 100,
            checksum = null
        )

        assertNull(state.checksum)
    }

    // ========================================
    // Error state tests
    // ========================================

    @Test
    fun `test Error state with all fields`() {
        val exception = Exception("Network error")
        val state = DownloadState.Error(
            modelId = "model-error",
            error = exception,
            message = "Failed to download model",
            code = ErrorCode.NETWORK_ERROR,
            canRetry = true,
            bytesDownloaded = 10_000_000L
        )

        assertEquals("model-error", state.modelId)
        assertEquals(exception, state.error)
        assertEquals("Failed to download model", state.message)
        assertEquals(ErrorCode.NETWORK_ERROR, state.code)
        assertTrue(state.canRetry)
        assertEquals(10_000_000L, state.bytesDownloaded)
    }

    @Test
    fun `test Error state with default values`() {
        val exception = Exception("Unknown error")
        val state = DownloadState.Error(
            modelId = "model",
            error = exception,
            message = "Error occurred"
        )

        assertEquals(ErrorCode.UNKNOWN, state.code)
        assertTrue(state.canRetry)
        assertEquals(0L, state.bytesDownloaded)
    }

    @Test
    fun `test Error state with canRetry false`() {
        val exception = Exception("Fatal error")
        val state = DownloadState.Error(
            modelId = "model", error = exception, message = "Fatal",
            code = ErrorCode.INVALID_CONFIG, canRetry = false
        )

        assertFalse(state.canRetry)
        assertEquals(ErrorCode.INVALID_CONFIG, state.code)
    }

    // ========================================
    // ErrorCode enum tests
    // ========================================

    @Test
    fun `test all ErrorCode values exist`() {
        val codes = ErrorCode.values()

        assertTrue(codes.contains(ErrorCode.UNKNOWN))
        assertTrue(codes.contains(ErrorCode.NETWORK_ERROR))
        assertTrue(codes.contains(ErrorCode.HTTP_ERROR))
        assertTrue(codes.contains(ErrorCode.INSUFFICIENT_STORAGE))
        assertTrue(codes.contains(ErrorCode.IO_ERROR))
        assertTrue(codes.contains(ErrorCode.CHECKSUM_MISMATCH))
        assertTrue(codes.contains(ErrorCode.CANCELLED))
        assertTrue(codes.contains(ErrorCode.TIMEOUT))
        assertTrue(codes.contains(ErrorCode.INVALID_CONFIG))
        assertEquals(9, codes.size)
    }

    // ========================================
    // Extension function tests: State checks
    // ========================================

    @Test
    fun `test isInProgress() returns true for Downloading`() {
        val state: DownloadState = DownloadState.Downloading(
            "model", 50, 100, 0.5f
        )
        assertTrue(state.isInProgress())
    }

    @Test
    fun `test isInProgress() returns false for non-Downloading`() {
        val states = listOf(
            DownloadState.Idle,
            DownloadState.Paused("model", 50, 100, 0.5f),
            DownloadState.Completed("model", "/path", 100),
            DownloadState.Error("model", Exception(), "Error")
        )

        states.forEach { state ->
            assertFalse(state.isInProgress())
        }
    }

    @Test
    fun `test isPaused() returns true for Paused`() {
        val state: DownloadState = DownloadState.Paused("model", 50, 100, 0.5f)
        assertTrue(state.isPaused())
    }

    @Test
    fun `test isComplete() returns true for Completed`() {
        val state: DownloadState = DownloadState.Completed("model", "/path", 100)
        assertTrue(state.isComplete())
    }

    @Test
    fun `test hasError() returns true for Error`() {
        val state: DownloadState = DownloadState.Error("model", Exception(), "Error")
        assertTrue(state.hasError())
    }

    @Test
    fun `test isIdle() returns true for Idle`() {
        val state: DownloadState = DownloadState.Idle
        assertTrue(state.isIdle())
    }

    // ========================================
    // Extension function tests: canResume()
    // ========================================

    @Test
    fun `test canResume() returns true for Paused`() {
        val state: DownloadState = DownloadState.Paused("model", 50, 100, 0.5f)
        assertTrue(state.canResume())
    }

    @Test
    fun `test canResume() returns true for Error with retry and bytes`() {
        val state: DownloadState = DownloadState.Error(
            "model", Exception(), "Error",
            canRetry = true, bytesDownloaded = 50
        )
        assertTrue(state.canResume())
    }

    @Test
    fun `test canResume() returns false for Error with retry but no bytes`() {
        val state: DownloadState = DownloadState.Error(
            "model", Exception(), "Error",
            canRetry = true, bytesDownloaded = 0
        )
        assertFalse(state.canResume())
    }

    @Test
    fun `test canResume() returns false for Error without retry`() {
        val state: DownloadState = DownloadState.Error(
            "model", Exception(), "Error",
            canRetry = false, bytesDownloaded = 50
        )
        assertFalse(state.canResume())
    }

    @Test
    fun `test canResume() returns false for Idle`() {
        val state: DownloadState = DownloadState.Idle
        assertFalse(state.canResume())
    }

    @Test
    fun `test canResume() returns false for Downloading`() {
        val state: DownloadState = DownloadState.Downloading("model", 50, 100, 0.5f)
        assertFalse(state.canResume())
    }

    @Test
    fun `test canResume() returns false for Completed`() {
        val state: DownloadState = DownloadState.Completed("model", "/path", 100)
        assertFalse(state.canResume())
    }

    // ========================================
    // Extension function tests: getModelId()
    // ========================================

    @Test
    fun `test getModelId() returns id for Downloading`() {
        val state: DownloadState = DownloadState.Downloading("model-123", 50, 100, 0.5f)
        assertEquals("model-123", state.getModelId())
    }

    @Test
    fun `test getModelId() returns id for Paused`() {
        val state: DownloadState = DownloadState.Paused("model-456", 50, 100, 0.5f)
        assertEquals("model-456", state.getModelId())
    }

    @Test
    fun `test getModelId() returns id for Completed`() {
        val state: DownloadState = DownloadState.Completed("model-789", "/path", 100)
        assertEquals("model-789", state.getModelId())
    }

    @Test
    fun `test getModelId() returns id for Error`() {
        val state: DownloadState = DownloadState.Error("model-error", Exception(), "Error")
        assertEquals("model-error", state.getModelId())
    }

    @Test
    fun `test getModelId() returns null for Idle`() {
        val state: DownloadState = DownloadState.Idle
        assertNull(state.getModelId())
    }

    // ========================================
    // Extension function tests: getProgress()
    // ========================================

    @Test
    fun `test getProgress() returns progress for Downloading`() {
        val state: DownloadState = DownloadState.Downloading("model", 75, 100, 0.75f)
        assertEquals(0.75f, state.getProgress()!!, 0.001f)
    }

    @Test
    fun `test getProgress() returns progress for Paused`() {
        val state: DownloadState = DownloadState.Paused("model", 50, 100, 0.5f)
        assertEquals(0.5f, state.getProgress()!!, 0.001f)
    }

    @Test
    fun `test getProgress() returns 1_0 for Completed`() {
        val state: DownloadState = DownloadState.Completed("model", "/path", 100)
        assertEquals(1.0f, state.getProgress()!!, 0.001f)
    }

    @Test
    fun `test getProgress() returns null for Error`() {
        val state: DownloadState = DownloadState.Error("model", Exception(), "Error")
        assertNull(state.getProgress())
    }

    @Test
    fun `test getProgress() returns null for Idle`() {
        val state: DownloadState = DownloadState.Idle
        assertNull(state.getProgress())
    }

    // ========================================
    // Edge cases and special scenarios
    // ========================================

    @Test
    fun `test Downloading with very large file sizes`() {
        val state = DownloadState.Downloading(
            modelId = "large-model",
            bytesDownloaded = 5_000_000_000L, // 5 GB
            totalBytes = 10_000_000_000L, // 10 GB
            progress = 0.5f
        )

        assertEquals("4 GB", state.getDownloadedSize())
        assertEquals("9 GB", state.getTotalSize())
    }

    @Test
    fun `test Downloading with zero bytes downloaded`() {
        val state = DownloadState.Downloading(
            modelId = "model", bytesDownloaded = 0, totalBytes = 1000, progress = 0.0f
        )

        assertEquals("0 B", state.getDownloadedSize())
        assertEquals(0, state.getProgressPercentage())
    }

    @Test
    fun `test formatBytes boundary values`() {
        // Test boundary between B and KB
        val state1 = DownloadState.Downloading("model", 1023L, 2000, 0.5f)
        assertEquals("1023 B", state1.getDownloadedSize())

        val state2 = DownloadState.Downloading("model", 1024L, 2000, 0.5f)
        assertEquals("1 KB", state2.getDownloadedSize())

        // Test boundary between KB and MB
        val state3 = DownloadState.Downloading("model", 1024 * 1024 - 1L, 2000000, 0.5f)
        assertEquals("1023 KB", state3.getDownloadedSize())

        val state4 = DownloadState.Downloading("model", 1024 * 1024L, 2000000, 0.5f)
        assertEquals("1 MB", state4.getDownloadedSize())
    }

    @Test
    fun `test formatTime boundary values`() {
        // Test boundary at 1 minute
        val state1 = DownloadState.Downloading("model", 0, 100, 0.0f, estimatedTimeRemainingMs = 59_999L)
        assertEquals("59s", state1.getTimeRemaining())

        val state2 = DownloadState.Downloading("model", 0, 100, 0.0f, estimatedTimeRemainingMs = 60_000L)
        assertEquals("1m 0s", state2.getTimeRemaining())

        // Test boundary at 1 hour
        val state3 = DownloadState.Downloading("model", 0, 100, 0.0f, estimatedTimeRemainingMs = 3_599_000L)
        assertEquals("59m 59s", state3.getTimeRemaining())

        val state4 = DownloadState.Downloading("model", 0, 100, 0.0f, estimatedTimeRemainingMs = 3_600_000L)
        assertEquals("1h 0m", state4.getTimeRemaining())
    }

    @Test
    fun `test Error with all ErrorCode types`() {
        val errorCodes = ErrorCode.values()

        errorCodes.forEach { code ->
            val state = DownloadState.Error(
                modelId = "model",
                error = Exception("Test"),
                message = "Test error",
                code = code
            )

            assertEquals(code, state.code)
        }
    }

    @Test
    fun `test Idle singleton behavior`() {
        val idle1 = DownloadState.Idle
        val idle2 = DownloadState.Idle

        assertSame(idle1, idle2) // data object should be singleton
    }
}
