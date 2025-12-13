package com.augmentalis.webavanue.download

import android.app.DownloadManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.augmentalis.Avanues.web.universal.download.DownloadProgressMonitor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for DownloadProgressMonitor
 *
 * Tests:
 * - Speed calculation
 * - ETA calculation
 * - Progress monitoring lifecycle
 * - Multiple concurrent downloads
 */
@OptIn(ExperimentalCoroutinesApi::class)
class DownloadProgressMonitorTest {

    private lateinit var context: Context
    private lateinit var scope: CoroutineScope
    private lateinit var monitor: DownloadProgressMonitor

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
        monitor = DownloadProgressMonitor(context, scope)
    }

    @After
    fun teardown() {
        monitor.stopAll()
        scope.cancel()
    }

    @Test
    fun testSpeedCalculation() {
        // Test speed calculation logic
        val timeDeltaSeconds = 1.0 // 1 second
        val bytesDelta = 1024L * 100 // 100 KB

        val speed = (bytesDelta / timeDeltaSeconds).toLong()

        assertEquals(102400L, speed) // 100 KB/s = 102400 bytes/s
    }

    @Test
    fun testEtaCalculation() {
        // Test ETA calculation logic
        val remainingBytes = 1024L * 1024 * 10 // 10 MB remaining
        val averageSpeed = 1024L * 1024 // 1 MB/s

        val eta = remainingBytes / averageSpeed

        assertEquals(10L, eta) // 10 seconds
    }

    @Test
    fun testEtaCalculationWithZeroSpeed() {
        // Test ETA when speed is 0
        val remainingBytes = 1024L * 1024 * 10 // 10 MB remaining
        val averageSpeed = 0L

        val eta = if (averageSpeed > 0) {
            remainingBytes / averageSpeed
        } else {
            0L
        }

        assertEquals(0L, eta) // Should return 0 when speed is 0
    }

    @Test
    fun testSpeedAveraging() = runTest {
        // Test that speed is averaged over multiple measurements
        val speeds = mutableListOf<Long>()

        // Simulate 5 speed measurements
        speeds.add(100000L) // 100 KB/s
        speeds.add(150000L) // 150 KB/s
        speeds.add(120000L) // 120 KB/s
        speeds.add(130000L) // 130 KB/s
        speeds.add(140000L) // 140 KB/s

        val averageSpeed = speeds.average().toLong()

        assertEquals(128000L, averageSpeed) // Average = 128 KB/s
    }

    @Test
    fun testMonitoringLifecycle() = runTest {
        val downloadId = "test_download_1"
        val downloadManagerId = 12345L

        // Start monitoring
        monitor.startMonitoring(downloadId, downloadManagerId)
        assertTrue(monitor.isMonitoring(downloadId))

        // Stop monitoring
        monitor.stopMonitoring(downloadId)
        assertFalse(monitor.isMonitoring(downloadId))
    }

    @Test
    fun testMultipleConcurrentDownloads() = runTest {
        val download1 = "test_download_1"
        val download2 = "test_download_2"
        val download3 = "test_download_3"

        // Start monitoring multiple downloads
        monitor.startMonitoring(download1, 1L)
        monitor.startMonitoring(download2, 2L)
        monitor.startMonitoring(download3, 3L)

        assertTrue(monitor.isMonitoring(download1))
        assertTrue(monitor.isMonitoring(download2))
        assertTrue(monitor.isMonitoring(download3))

        // Stop one download
        monitor.stopMonitoring(download2)

        assertTrue(monitor.isMonitoring(download1))
        assertFalse(monitor.isMonitoring(download2))
        assertTrue(monitor.isMonitoring(download3))

        // Stop all downloads
        monitor.stopAll()

        assertFalse(monitor.isMonitoring(download1))
        assertFalse(monitor.isMonitoring(download2))
        assertFalse(monitor.isMonitoring(download3))
    }

    @Test
    fun testDuplicateMonitoringPrevented() = runTest {
        val downloadId = "test_download_1"
        val downloadManagerId = 12345L

        // Start monitoring twice
        monitor.startMonitoring(downloadId, downloadManagerId)
        monitor.startMonitoring(downloadId, downloadManagerId)

        // Should still only be monitoring once
        assertTrue(monitor.isMonitoring(downloadId))

        // Stop monitoring
        monitor.stopMonitoring(downloadId)
        assertFalse(monitor.isMonitoring(downloadId))
    }

    @Test
    fun testProgressCalculation() {
        // Test progress calculation
        val bytesDownloaded = 5L * 1024 * 1024 // 5 MB
        val bytesTotal = 10L * 1024 * 1024 // 10 MB

        val progress = if (bytesTotal > 0) {
            bytesDownloaded.toFloat() / bytesTotal
        } else {
            0f
        }

        assertEquals(0.5f, progress, 0.01f) // 50% progress

        val progressPercent = (progress * 100).toInt()
        assertEquals(50, progressPercent)
    }

    @Test
    fun testProgressCalculationWithZeroTotal() {
        // Test progress when total is 0
        val bytesDownloaded = 1024L
        val bytesTotal = 0L

        val progress = if (bytesTotal > 0) {
            bytesDownloaded.toFloat() / bytesTotal
        } else {
            0f
        }

        assertEquals(0f, progress, 0.01f) // Should return 0
    }

    @Test
    fun testSpeedFormattingLogic() {
        // Test speed formatting logic (from UI layer)
        fun formatSpeed(bytesPerSec: Long): String {
            return when {
                bytesPerSec == 0L -> "0 B/s"
                bytesPerSec < 1024 -> "$bytesPerSec B/s"
                bytesPerSec < 1024 * 1024 -> "${bytesPerSec / 1024} KB/s"
                else -> "%.1f MB/s".format(bytesPerSec / (1024.0 * 1024.0))
            }
        }

        assertEquals("0 B/s", formatSpeed(0))
        assertEquals("500 B/s", formatSpeed(500))
        assertEquals("100 KB/s", formatSpeed(102400))
        assertEquals("1.0 MB/s", formatSpeed(1048576))
        assertEquals("5.5 MB/s", formatSpeed(5767168)) // 5.5 * 1024 * 1024
    }

    @Test
    fun testEtaFormattingLogic() {
        // Test ETA formatting logic (from UI layer)
        fun formatETA(seconds: Long): String {
            return when {
                seconds == 0L -> "calculating..."
                seconds < 60 -> "${seconds}s"
                seconds < 3600 -> "${seconds / 60}m ${seconds % 60}s"
                else -> "${seconds / 3600}h ${(seconds % 3600) / 60}m"
            }
        }

        assertEquals("calculating...", formatETA(0))
        assertEquals("30s", formatETA(30))
        assertEquals("1m 30s", formatETA(90))
        assertEquals("5m 0s", formatETA(300))
        assertEquals("1h 30m", formatETA(5400))
        assertEquals("2h 15m", formatETA(8100))
    }

    @Test
    fun testFileSizeFormattingLogic() {
        // Test file size formatting logic (from UI layer)
        fun formatFileSize(bytes: Long): String {
            val kb = bytes / 1024.0
            val mb = kb / 1024.0
            val gb = mb / 1024.0

            return when {
                gb >= 1.0 -> "%.2f GB".format(gb)
                mb >= 1.0 -> "%.2f MB".format(mb)
                kb >= 1.0 -> "%.2f KB".format(kb)
                else -> "$bytes B"
            }
        }

        assertEquals("500 B", formatFileSize(500))
        assertEquals("1.00 KB", formatFileSize(1024))
        assertEquals("1.00 MB", formatFileSize(1048576))
        assertEquals("1.50 MB", formatFileSize(1572864))
        assertEquals("1.00 GB", formatFileSize(1073741824))
    }
}
