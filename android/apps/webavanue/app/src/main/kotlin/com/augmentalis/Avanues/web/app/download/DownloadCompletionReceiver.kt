package com.augmentalis.Avanues.web.app.download

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.augmentalis.Avanues.web.app.WebAvanueApp
import com.augmentalis.webavanue.domain.model.Download
import com.augmentalis.webavanue.domain.model.DownloadStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * DownloadCompletionReceiver - BroadcastReceiver for Android DownloadManager events
 *
 * Receives:
 * - ACTION_DOWNLOAD_COMPLETE: When a download finishes (success or failure)
 *
 * Updates the repository with:
 * - Completion status and file path (on success)
 * - Failure status and error message (on failure)
 */
class DownloadCompletionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != DownloadManager.ACTION_DOWNLOAD_COMPLETE) {
            return
        }

        val downloadId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
        if (downloadId == -1L) {
            println("DownloadCompletionReceiver: Invalid download ID")
            return
        }

        println("DownloadCompletionReceiver: Download complete event for ID: $downloadId")

        // Query download status
        val progress = DownloadHelper.queryProgress(context, downloadId)
        if (progress == null) {
            println("DownloadCompletionReceiver: Could not query download status")
            return
        }

        // Get repository from Application
        val app = context.applicationContext as? WebAvanueApp
        if (app == null) {
            println("DownloadCompletionReceiver: Could not get WebAvanueApp")
            return
        }

        val repository = app.provideRepository()

        // Update repository based on status
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Find our download by DownloadManager ID
                val download = repository.getDownloadByManagerId(downloadId)
                if (download == null) {
                    println("DownloadCompletionReceiver: Download not found in repository for manager ID: $downloadId")
                    return@launch
                }

                when {
                    progress.isComplete -> {
                        // Download succeeded
                        val filepath = progress.localUri ?: ""
                        repository.completeDownload(
                            downloadId = download.id,
                            filepath = filepath,
                            downloadedSize = progress.bytesDownloaded
                        )
                        println("DownloadCompletionReceiver: Download completed: ${download.filename}")
                    }

                    progress.isFailed -> {
                        // Download failed
                        val errorMessage = DownloadHelper.reasonToString(progress.reason)
                        repository.failDownload(
                            downloadId = download.id,
                            errorMessage = errorMessage
                        )
                        println("DownloadCompletionReceiver: Download failed: ${download.filename} - $errorMessage")
                    }

                    else -> {
                        // Intermediate status - update progress
                        repository.updateDownloadProgress(
                            downloadId = download.id,
                            downloadedSize = progress.bytesDownloaded,
                            status = when (progress.status) {
                                DownloadManager.STATUS_RUNNING -> DownloadStatus.DOWNLOADING
                                DownloadManager.STATUS_PAUSED -> DownloadStatus.PAUSED
                                DownloadManager.STATUS_PENDING -> DownloadStatus.PENDING
                                else -> DownloadStatus.DOWNLOADING
                            }
                        )
                        println("DownloadCompletionReceiver: Download progress: ${download.filename} - ${progress.progressPercent}%")
                    }
                }
            } catch (e: Exception) {
                println("DownloadCompletionReceiver: Error updating download: ${e.message}")
            }
        }
    }
}
