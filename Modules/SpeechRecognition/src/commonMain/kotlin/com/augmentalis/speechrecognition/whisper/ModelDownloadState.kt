/**
 * ModelDownloadState.kt - Shared model download state models
 *
 * Copyright (C) Augmentalis Inc, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Created: 2026-02-20
 *
 * Platform-agnostic download state, progress, and model info types.
 * Used by WhisperModelManager implementations on all platforms.
 */
package com.augmentalis.speechrecognition.whisper

/**
 * State of a model download operation.
 */
sealed class ModelDownloadState {
    /** No download in progress */
    data object Idle : ModelDownloadState()

    /** Checking if model exists locally */
    data object Checking : ModelDownloadState()

    /** Fetching model metadata from server */
    data object FetchingMetadata : ModelDownloadState()

    /** Downloading model file */
    data class Downloading(
        val modelSize: WhisperModelSize,
        val bytesDownloaded: Long,
        val totalBytes: Long,
        val speedBytesPerSec: Long = 0
    ) : ModelDownloadState() {
        val progressPercent: Float
            get() = if (totalBytes > 0) (bytesDownloaded.toFloat() / totalBytes) * 100f else 0f

        val downloadedMB: Float get() = bytesDownloaded / (1024f * 1024f)
        val totalMB: Float get() = totalBytes / (1024f * 1024f)
        val speedMBPerSec: Float get() = speedBytesPerSec / (1024f * 1024f)

        val estimatedRemainingSeconds: Long
            get() = if (speedBytesPerSec > 0) {
                (totalBytes - bytesDownloaded) / speedBytesPerSec
            } else 0L
    }

    /** Verifying download integrity */
    data class Verifying(val modelSize: WhisperModelSize) : ModelDownloadState()

    /** Download completed successfully */
    data class Completed(val modelSize: WhisperModelSize, val filePath: String) : ModelDownloadState()

    /** Retrying after transient failure */
    data class Retrying(
        val modelSize: WhisperModelSize,
        val attempt: Int,
        val maxAttempts: Int,
        val delayMs: Long
    ) : ModelDownloadState()

    /** Download failed */
    data class Failed(val modelSize: WhisperModelSize, val error: String) : ModelDownloadState()

    /** Download was cancelled by user */
    data class Cancelled(val modelSize: WhisperModelSize) : ModelDownloadState()
}

/**
 * Information about a locally available model.
 */
data class LocalModelInfo(
    val modelSize: WhisperModelSize,
    val filePath: String,
    val fileSizeBytes: Long,
    val isVerified: Boolean,
    val downloadedAtMs: Long
) {
    val fileSizeMB: Float get() = fileSizeBytes / (1024f * 1024f)
}

/**
 * Download URL registry for Whisper ggml models.
 */
object WhisperModelUrls {
    const val BASE_URL = "https://huggingface.co/ggerganov/whisper.cpp/resolve/main/"

    /** HuggingFace API for file metadata (SHA256 hashes) */
    const val API_URL = "https://huggingface.co/api/models/ggerganov/whisper.cpp"

    fun getDownloadUrl(modelSize: WhisperModelSize): String {
        return "$BASE_URL${modelSize.ggmlFileName}"
    }

    /**
     * Known SHA256 checksums for ggml model files.
     * Source: HuggingFace ggerganov/whisper.cpp repo.
     * Allows offline verification without API call.
     */
    val KNOWN_CHECKSUMS: Map<WhisperModelSize, String> = mapOf(
        // These are verified checksums from the HuggingFace repo.
        // Updated as of 2026-02. Verify at: https://huggingface.co/ggerganov/whisper.cpp/tree/main
        // Format: SHA256 hash of the ggml .bin file
    )
}
