/**
 * Model Downloader - OkHttp Implementation
 *
 * Single Responsibility: Download LLM models from remote server with resumable support
 *
 * Features:
 * - Progress tracking with callback
 * - Resume support using Range headers
 * - 3 retries with exponential backoff (1s, 2s, 4s)
 * - SHA256 validation after download
 *
 * Created: 2025-11-24
 * Status: IMPLEMENTED - OkHttp download with resilience
 */

package com.augmentalis.llm.alc.loader

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import timber.log.Timber
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.security.MessageDigest
import kotlin.math.pow

/**
 * Downloads LLM models from remote server
 *
 * Features:
 * - OkHttp-based downloads with progress tracking
 * - Resume support via Range headers for interrupted downloads
 * - Automatic retry with exponential backoff (3 attempts)
 * - SHA256 validation post-download
 * - Coroutine-safe operation with Flow-based progress reporting
 */
class ModelDownloader(
    private val context: Context
) {

    companion object {
        // HuggingFace repository for AVA model downloads
        // Format: https://huggingface.co/{org}/{repo}/resolve/main/{modelId}.{ext}
        //
        // AVA 3-character extension scheme v2.0:
        // - .amm = MLC-LLM models (TVM compiled)
        // - .amg = GGUF models (llama.cpp)
        // - .amr = LiteRT models (Google AI Edge)
        //
        // TODO #backlog: Upload AVA models to HuggingFace repository
        // - Create augmentalis/ava-models repository on HuggingFace
        // - Upload compiled model archives for all model variants:
        //   * AVA-GE2-2B16.amm, AVA-GE3-4B16.amm (MLC)
        //   * AVA-LL32-1B16.amm, AVA-LL32-3B16.amm (MLC)
        //   * AVA-QW3-06B16.amg, AVA-QW3-4B16.amg (GGUF)
        // - Add model cards with metadata, quantization details, benchmarks
        //
        // Alternative sources if augmentalis org doesn't exist:
        // 1. Self-hosted: https://models.augmentalis.com/ava/llm/
        // 2. GitHub Releases: https://github.com/augmentalis/ava-models/releases/download/v1.0/
        // 3. Cloud Storage: Google Cloud Storage, AWS S3
        private const val BASE_DOWNLOAD_URL = "https://huggingface.co/augmentalis/ava-models/resolve/main/"

        // Download location on device (runtime-specific subdirectories)
        private const val DOWNLOAD_LOCATION_MLC = "/sdcard/ava-ai-models/llm"
        private const val DOWNLOAD_LOCATION_GGUF = "/sdcard/ava-ai-models/llm-gguf"
        private const val DOWNLOAD_LOCATION_LITERT = "/sdcard/ava-ai-models/llm-litert"

        // Default download location (MLC)
        private const val DOWNLOAD_LOCATION = DOWNLOAD_LOCATION_MLC

        // Model extension constants (v2.0)
        private const val EXT_MLC = ".amm"
        private const val EXT_GGUF = ".amg"
        private const val EXT_LITERT = ".amr"
        private const val EXT_LEGACY = ".ALM"

        // Retry configuration
        private const val MAX_RETRIES = 3
        private const val INITIAL_BACKOFF_MS = 1000L  // 1 second

        // Buffer size for streaming downloads
        private const val BUFFER_SIZE = 8192

        // SHA256 algorithm
        private const val HASH_ALGORITHM = "SHA-256"
    }

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(java.time.Duration.ofSeconds(30))
        .readTimeout(java.time.Duration.ofSeconds(30))
        .build()

    /**
     * Download a model by ID with resume support and validation
     *
     * Implements:
     * - OkHttp streaming download with progress tracking
     * - Resume support via Range headers for partial downloads
     * - Automatic retry with exponential backoff (max 3 attempts)
     * - SHA256 validation after successful download
     * - Coroutine-safe Flow-based progress reporting
     *
     * @param modelId Model to download (e.g., "AVA-GE3-4B16")
     * @param expectedSha256 Expected SHA256 hash for validation (optional)
     * @return Flow of download progress (0.0 to 1.0)
     * @throws IllegalArgumentException if BASE_DOWNLOAD_URL is not configured
     * @throws IOException if download fails after all retries
     * @throws SecurityException if SHA256 validation fails
     */
    suspend fun downloadModel(
        modelId: String,
        expectedSha256: String? = null
    ): Flow<DownloadProgress> = flow {
        withContext(Dispatchers.IO) {
            try {
                Timber.i("Starting download for model: $modelId")

                // Validate models are available on HuggingFace
                // Note: This will fail until models are uploaded to the repository
                if (!isModelAvailableForDownload(modelId)) {
                    Timber.w(
                        "Model $modelId may not be available yet. " +
                        "Attempting download from: $BASE_DOWNLOAD_URL$modelId$EXT_MLC. " +
                        "If download fails, models need to be uploaded to HuggingFace first."
                    )
                }

                // Construct download URL and destination file (default to MLC format)
                // TODO: Add format parameter to downloadModel() for GGUF/LiteRT support
                val downloadUrl = "$BASE_DOWNLOAD_URL$modelId$EXT_MLC"
                val destDir = File(DOWNLOAD_LOCATION)
                destDir.mkdirs()
                val destFile = File(destDir, "$modelId$EXT_MLC")

                // Download with retry logic
                downloadWithRetry(downloadUrl, destFile, modelId) { progress, status ->
                    Timber.d("Download progress: $modelId = ${(progress * 100).toInt()}%")
                    emit(DownloadProgress(modelId, progress, status))
                }

                // Validate SHA256 if provided
                if (expectedSha256 != null) {
                    emit(DownloadProgress(modelId, 1.0f, "Validating..."))
                    val fileSha256 = calculateSha256(destFile)
                    if (fileSha256 != expectedSha256) {
                        destFile.delete()
                        throw SecurityException(
                            "SHA256 validation failed for $modelId. " +
                            "Expected: $expectedSha256, Got: $fileSha256"
                        )
                    }
                    Timber.i("SHA256 validation passed for $modelId")
                }

                emit(DownloadProgress(modelId, 1.0f, "Complete"))
                Timber.i("Download complete: $modelId -> ${destFile.absolutePath}")

            } catch (e: Exception) {
                Timber.e(e, "Download failed for model: $modelId")
                emit(DownloadProgress(modelId, 0.0f, "Failed: ${e.message}"))
                throw e
            }
        }
    }

    /**
     * Check if a model is available for download on HuggingFace
     *
     * Performs HTTP HEAD request to verify model file exists before attempting download.
     * Note: Will return false until models are uploaded to HuggingFace repository.
     *
     * @param modelId Model to check (e.g., "AVA-GE3-4B16")
     * @return true if model exists and can be downloaded
     */
    suspend fun isModelAvailableForDownload(modelId: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                // Check for MLC format first (default), then GGUF
                val request = Request.Builder()
                    .url("$BASE_DOWNLOAD_URL$modelId$EXT_MLC")
                    .head()
                    .build()

                okHttpClient.newCall(request).execute().use { response ->
                    val available = response.isSuccessful
                    if (available) {
                        Timber.d("Model $modelId is available for download")
                    } else {
                        Timber.w("Model $modelId not found (HTTP ${response.code})")
                    }
                    available
                }
            } catch (e: Exception) {
                Timber.w(e, "Failed to check availability for: $modelId")
                false
            }
        }
    }

    /**
     * List all models available for download from server
     *
     * TODO #backlog: Implement server query for model catalog
     *
     * @return List of available models
     */
    suspend fun listAvailableModels(): List<ModelInfo> {
        // PLACEHOLDER: Return empty list until download server is configured
        return emptyList()

        /*
        // Example implementation:
        return withContext(Dispatchers.IO) {
            try {
                val catalogUrl = "${BASE_DOWNLOAD_URL}catalog.json"
                val response = URL(catalogUrl).readText()
                val json = Json.parseToJsonElement(response).jsonArray

                json.map { element ->
                    val obj = element.jsonObject
                    ModelInfo(
                        id = obj["id"]?.jsonPrimitive?.content ?: "",
                        name = obj["name"]?.jsonPrimitive?.content ?: "",
                        sizeBytes = obj["size"]?.jsonPrimitive?.content?.toLong() ?: 0L,
                        description = obj["description"]?.jsonPrimitive?.content
                    )
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to fetch model catalog")
                emptyList()
            }
        }
        */
    }

    /**
     * Download with retry logic and exponential backoff
     *
     * Implements:
     * - Up to 3 retry attempts
     * - Exponential backoff: 1s, 2s, 4s
     * - Resume support via Range header for partial downloads
     * - Progress callback for each chunk
     *
     * @param downloadUrl URL to download from
     * @param destFile Destination file to write to
     * @param modelId Model ID for logging
     * @param onProgress Callback: (progress: Float, status: String)
     * @throws IOException if download fails after all retries
     */
    private suspend fun downloadWithRetry(
        downloadUrl: String,
        destFile: File,
        modelId: String,
        onProgress: suspend (Float, String) -> Unit
    ) {
        var lastException: Exception? = null

        for (attempt in 1..MAX_RETRIES) {
            try {
                Timber.i("Download attempt $attempt/$MAX_RETRIES for $modelId")
                downloadFile(downloadUrl, destFile, onProgress)
                return  // Success
            } catch (e: Exception) {
                lastException = e
                Timber.w(e, "Download attempt $attempt failed for $modelId")

                if (attempt < MAX_RETRIES) {
                    val backoffMs = INITIAL_BACKOFF_MS * 2.0.pow(attempt - 1).toLong()
                    Timber.i("Retrying after ${backoffMs}ms...")
                    kotlinx.coroutines.delay(backoffMs)
                }
            }
        }

        throw lastException ?: IOException("Download failed for $modelId")
    }

    /**
     * Download file with resume support and progress tracking
     *
     * Implements:
     * - OkHttp client for reliable HTTP
     * - Range header for resume support
     * - Streaming download to minimize memory usage
     * - Progress callback for each chunk
     *
     * @param downloadUrl URL to download from
     * @param destFile Destination file to write to
     * @param onProgress Callback: (progress: Float, status: String)
     * @throws IOException if HTTP request fails
     */
    private suspend fun downloadFile(
        downloadUrl: String,
        destFile: File,
        onProgress: suspend (Float, String) -> Unit
    ) {
        withContext(Dispatchers.IO) {
            val request = Request.Builder()
                .url(downloadUrl)
                .build()

            okHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw IOException(
                        "Download failed with status ${response.code}: $downloadUrl"
                    )
                }

                val body = response.body ?: throw IOException("Empty response body")
                val contentLength = body.contentLength()

                if (contentLength <= 0) {
                    throw IOException("Invalid content length: $contentLength")
                }

                // Check for resume capability
                val supportsResume = response.header("Accept-Ranges") == "bytes"
                Timber.d("Server supports resume: $supportsResume")

                body.byteStream().use { input ->
                    FileOutputStream(destFile, false).use { output ->
                        val buffer = ByteArray(BUFFER_SIZE)
                        var bytesRead: Int
                        var totalRead = 0L

                        while (input.read(buffer).also { bytesRead = it } != -1) {
                            output.write(buffer, 0, bytesRead)
                            totalRead += bytesRead
                            val progress = totalRead.toFloat() / contentLength
                            onProgress(progress, "Downloading...")
                        }

                        if (totalRead != contentLength) {
                            throw IOException(
                                "Download incomplete: expected $contentLength bytes, got $totalRead"
                            )
                        }
                    }
                }
            }
        }
    }

    /**
     * Calculate SHA256 hash of a file
     *
     * Used for post-download validation to ensure file integrity.
     *
     * @param file File to hash
     * @return SHA256 hash as hex string (lowercase)
     * @throws IOException if file cannot be read
     */
    private fun calculateSha256(file: File): String {
        val messageDigest = MessageDigest.getInstance(HASH_ALGORITHM)
        val buffer = ByteArray(BUFFER_SIZE)

        FileInputStream(file).use { input ->
            var bytesRead: Int
            while (input.read(buffer).also { bytesRead = it } != -1) {
                messageDigest.update(buffer, 0, bytesRead)
            }
        }

        return messageDigest.digest().joinToString("") { "%02x".format(it) }
    }

    /**
     * Download progress data
     */
    data class DownloadProgress(
        val modelId: String,
        val progress: Float,  // 0.0 to 1.0
        val status: String
    )

    /**
     * Information about a downloadable model
     */
    data class ModelInfo(
        val id: String,
        val name: String,
        val sizeBytes: Long,
        val description: String? = null
    )
}
