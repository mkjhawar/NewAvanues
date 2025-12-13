package com.augmentalis.Avanues.web.universal.download

import android.content.Context
import com.augmentalis.Avanues.web.app.download.DownloadHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * DownloadProgressMonitor - Monitors download progress using DownloadManager
 *
 * Polls DownloadManager at 500ms intervals to track:
 * - Bytes downloaded
 * - Download speed (calculated from deltas)
 * - Estimated time remaining (based on average speed)
 *
 * Usage:
 * ```kotlin
 * val monitor = DownloadProgressMonitor(context, scope)
 * monitor.startMonitoring(downloadId)
 * monitor.progressFlow.collect { progressMap ->
 *     // Update UI with progress
 * }
 * ```
 */
class DownloadProgressMonitor(
    private val context: Context,
    private val scope: CoroutineScope
) {
    // Progress flow emitting map of downloadId -> DownloadProgressData
    private val _progressFlow = MutableStateFlow<Map<String, DownloadProgressData>>(emptyMap())
    val progressFlow: StateFlow<Map<String, DownloadProgressData>> = _progressFlow.asStateFlow()

    // Active monitoring jobs (downloadId -> Job)
    private val activeDownloads = mutableMapOf<String, Job>()

    // Speed history for smoothing (downloadId -> list of speeds)
    private val speedHistory = mutableMapOf<String, MutableList<Long>>()

    companion object {
        private const val POLL_INTERVAL_MS = 500L
        private const val SPEED_HISTORY_SIZE = 5 // Average last 5 measurements
    }

    /**
     * Start monitoring a download
     *
     * @param downloadId Download ID to monitor
     * @param downloadManagerId Android DownloadManager ID
     */
    fun startMonitoring(downloadId: String, downloadManagerId: Long) {
        // Don't start if already monitoring
        if (activeDownloads.containsKey(downloadId)) {
            return
        }

        val job = scope.launch {
            var lastBytes = 0L
            var lastTime = System.currentTimeMillis()
            speedHistory[downloadId] = mutableListOf()

            while (isActive) {
                val progress = DownloadHelper.queryProgress(context, downloadManagerId)

                if (progress != null) {
                    val currentTime = System.currentTimeMillis()
                    val timeDelta = (currentTime - lastTime) / 1000.0 // seconds

                    // Calculate instantaneous speed
                    val bytesDelta = progress.bytesDownloaded - lastBytes
                    val instantSpeed = if (timeDelta > 0) (bytesDelta / timeDelta).toLong() else 0L

                    // Add to speed history and calculate average
                    val speeds = speedHistory[downloadId] ?: mutableListOf()
                    speeds.add(instantSpeed)
                    if (speeds.size > SPEED_HISTORY_SIZE) {
                        speeds.removeAt(0)
                    }
                    speedHistory[downloadId] = speeds

                    val averageSpeed = if (speeds.isNotEmpty()) {
                        speeds.average().toLong()
                    } else {
                        0L
                    }

                    // Calculate ETA
                    val remainingBytes = progress.bytesTotal - progress.bytesDownloaded
                    val eta = if (averageSpeed > 0) {
                        (remainingBytes / averageSpeed)
                    } else {
                        0L
                    }

                    // Emit progress update
                    val progressData = DownloadProgressData(
                        downloadId = downloadId,
                        bytesDownloaded = progress.bytesDownloaded,
                        bytesTotal = progress.bytesTotal,
                        downloadSpeed = averageSpeed,
                        estimatedTimeRemaining = eta,
                        status = progress.status,
                        localUri = progress.localUri
                    )

                    _progressFlow.value = _progressFlow.value + (downloadId to progressData)

                    // Update tracking variables
                    lastBytes = progress.bytesDownloaded
                    lastTime = currentTime

                    // Stop monitoring if complete or failed
                    if (progress.isComplete || progress.isFailed) {
                        stopMonitoring(downloadId)
                        break
                    }
                }

                delay(POLL_INTERVAL_MS)
            }
        }

        activeDownloads[downloadId] = job
    }

    /**
     * Stop monitoring a download
     *
     * @param downloadId Download ID to stop monitoring
     */
    fun stopMonitoring(downloadId: String) {
        activeDownloads[downloadId]?.cancel()
        activeDownloads.remove(downloadId)
        speedHistory.remove(downloadId)
    }

    /**
     * Stop monitoring all downloads
     */
    fun stopAll() {
        activeDownloads.values.forEach { it.cancel() }
        activeDownloads.clear()
        speedHistory.clear()
        _progressFlow.value = emptyMap()
    }

    /**
     * Check if a download is being monitored
     */
    fun isMonitoring(downloadId: String): Boolean {
        return activeDownloads.containsKey(downloadId)
    }
}

/**
 * Download progress data
 */
data class DownloadProgressData(
    val downloadId: String,
    val bytesDownloaded: Long,
    val bytesTotal: Long,
    val downloadSpeed: Long, // Bytes per second
    val estimatedTimeRemaining: Long, // Seconds
    val status: Int,
    val localUri: String?
) {
    val progress: Float
        get() = if (bytesTotal > 0) bytesDownloaded.toFloat() / bytesTotal else 0f

    val progressPercent: Int
        get() = (progress * 100).toInt()
}
