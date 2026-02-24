/**
 * AvxModelManager.kt - AVX model download, encryption, and lifecycle management
 *
 * Copyright (C) Augmentalis Inc, Intelligent Devices LLC
 * Created: 2026-02-24
 *
 * Manages AVX language model files (transducer models from Sherpa-ONNX):
 * 1. Download model files from HuggingFace (encoder, decoder, joiner, tokens)
 * 2. Archive into zip
 * 3. Encrypt via AONFormat.wrap() -> Ava-AvxS-{Lang}.aon
 * 4. Store in /sdcard/ava-ai-models/avx/
 * 5. Track installed models in local inventory
 * 6. Delete/update models on demand
 */
package com.augmentalis.speechrecognition.avx

import android.content.Context
import android.util.Log
import com.augmentalis.crypto.aon.AONFormat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.File
import java.io.FileOutputStream
import java.net.URL
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Download state for UI integration.
 */
sealed class AvxDownloadState {
    object Idle : AvxDownloadState()
    data class Downloading(val language: AvxLanguage, val progress: Float, val currentFile: String) : AvxDownloadState()
    data class Encrypting(val language: AvxLanguage) : AvxDownloadState()
    data class Complete(val language: AvxLanguage) : AvxDownloadState()
    data class Error(val language: AvxLanguage, val message: String) : AvxDownloadState()
}

/**
 * Manages AVX model files: download, encrypt, store, and query.
 *
 * Each language model is a set of 4 files from a Sherpa-ONNX transducer model:
 * - encoder-*.int8.onnx (~40-120MB, bulk of the model)
 * - decoder-*.int8.onnx (~0.5MB)
 * - joiner-*.int8.onnx (~0.3MB)
 * - tokens.txt (~5KB)
 *
 * These are downloaded individually from HuggingFace, zipped together,
 * and AON-encrypted into a single .aon archive file.
 */
class AvxModelManager(
    private val context: Context
) {
    companion object {
        private const val TAG = "AvxModelManager"
        private const val HUGGINGFACE_BASE = "https://huggingface.co"
    }

    private val modelDir = AvxEngine.MODEL_DIR

    private val _downloadState = MutableStateFlow<AvxDownloadState>(AvxDownloadState.Idle)
    val downloadState: StateFlow<AvxDownloadState> = _downloadState.asStateFlow()

    /**
     * Check if a language model is installed.
     */
    fun isModelInstalled(language: AvxLanguage): Boolean {
        val modelFile = File(modelDir, language.aonFileName)
        return modelFile.exists() && modelFile.length() > 0
    }

    /**
     * Get all installed language models.
     */
    fun getInstalledModels(): List<AvxModelInfo> {
        if (!modelDir.exists()) return emptyList()

        return AvxLanguage.availableLanguages().mapNotNull { lang ->
            val file = File(modelDir, lang.aonFileName)
            if (file.exists() && file.length() > 0) {
                AvxModelInfo(
                    language = lang,
                    filePath = file.absolutePath,
                    fileSizeBytes = file.length(),
                    downloadedAtMs = file.lastModified()
                )
            } else null
        }
    }

    /**
     * Get total disk usage of installed AVX models in bytes.
     */
    fun getTotalDiskUsageBytes(): Long {
        return getInstalledModels().sumOf { it.fileSizeBytes }
    }

    /**
     * Download and encrypt a language model.
     *
     * Pipeline:
     * 1. Download 4 model files from HuggingFace -> temp directory
     * 2. Zip the 4 files into a single archive
     * 3. AONFormat.wrap(zipBytes) -> Ava-AvxS-{Lang}.aon
     * 4. Store in model directory
     * 5. Delete temp files
     *
     * @return true if download and encryption succeeded
     */
    suspend fun downloadModel(language: AvxLanguage): Boolean = withContext(Dispatchers.IO) {
        if (!language.hasTransducerModel) {
            Log.w(TAG, "Language ${language.displayName} has no transducer model (tier=${language.tier})")
            _downloadState.value = AvxDownloadState.Error(language, "No transducer model available")
            return@withContext false
        }

        try {
            _downloadState.value = AvxDownloadState.Downloading(language, 0f, "")

            // Ensure model directory exists
            if (!modelDir.exists()) modelDir.mkdirs()

            // Step 1: Download model files to temp directory
            val tempDir = File(context.cacheDir, "avx_download_${language.langCode}")
            if (tempDir.exists()) tempDir.deleteRecursively()
            tempDir.mkdirs()

            val modelFiles = language.modelFiles
            val filesToDownload = listOf(
                modelFiles.encoderFilename,
                modelFiles.decoderFilename,
                modelFiles.joinerFilename,
                modelFiles.tokensFilename
            )

            var totalProgress = 0f
            val progressPerFile = 1f / filesToDownload.size

            for ((index, filename) in filesToDownload.withIndex()) {
                _downloadState.value = AvxDownloadState.Downloading(
                    language, totalProgress, filename
                )

                val downloadUrl = buildDownloadUrl(language.modelRepoId, filename)
                val outputFile = File(tempDir, filename)

                Log.i(TAG, "Downloading: $filename from $downloadUrl")

                downloadFile(downloadUrl, outputFile) { fileProgress ->
                    _downloadState.value = AvxDownloadState.Downloading(
                        language,
                        totalProgress + fileProgress * progressPerFile,
                        filename
                    )
                }

                totalProgress += progressPerFile
                Log.i(TAG, "Downloaded: $filename (${outputFile.length()} bytes)")
            }

            // Step 2: Zip all model files into archive
            Log.i(TAG, "Archiving model files for ${language.displayName}...")

            val zipFile = File(tempDir, "model.zip")
            ZipOutputStream(FileOutputStream(zipFile)).use { zos ->
                for (filename in filesToDownload) {
                    val file = File(tempDir, filename)
                    zos.putNextEntry(ZipEntry(filename))
                    file.inputStream().use { input ->
                        input.copyTo(zos)
                    }
                    zos.closeEntry()
                }
            }

            Log.i(TAG, "Archive created: ${zipFile.length()} bytes")

            // Step 3: Encrypt with AON codec
            _downloadState.value = AvxDownloadState.Encrypting(language)

            val zipBytes = zipFile.readBytes()
            val aonBytes = AONFormat.wrap(
                payload = zipBytes,
                modelId = "avx-${language.langCode}",
                platform = "android"
            )

            // Step 4: Write encrypted model to storage
            val outputFile = File(modelDir, language.aonFileName)
            outputFile.writeBytes(aonBytes)

            Log.i(TAG, "AON encrypted: ${outputFile.absolutePath} (${outputFile.length()} bytes)")

            // Step 5: Clean up temp files
            tempDir.deleteRecursively()

            _downloadState.value = AvxDownloadState.Complete(language)
            true

        } catch (e: Exception) {
            Log.e(TAG, "Failed to download AVX model: ${language.displayName}", e)
            _downloadState.value = AvxDownloadState.Error(language, e.message ?: "Unknown error")
            false
        }
    }

    /**
     * Delete a downloaded language model.
     *
     * @return true if the file was deleted
     */
    fun deleteModel(language: AvxLanguage): Boolean {
        val modelFile = File(modelDir, language.aonFileName)
        if (modelFile.exists()) {
            val deleted = modelFile.delete()
            Log.i(TAG, "Deleted AVX model: ${language.displayName} -> $deleted")
            return deleted
        }
        return false
    }

    /**
     * Download all priority languages that aren't already installed.
     *
     * @return Number of models successfully downloaded
     */
    suspend fun downloadAllPriority(): Int {
        var count = 0
        for (lang in AvxLanguage.priorityLanguages()) {
            if (!isModelInstalled(lang)) {
                if (downloadModel(lang)) count++
            }
        }
        return count
    }

    /**
     * Download only the device locale language + English (first-run default).
     * Only downloads for languages that have transducer models.
     *
     * @return Number of models downloaded
     */
    suspend fun downloadDefault(): Int {
        var count = 0
        val localeCode = java.util.Locale.getDefault().language

        // Always ensure English is installed
        if (!isModelInstalled(AvxLanguage.ENGLISH)) {
            if (downloadModel(AvxLanguage.ENGLISH)) count++
        }

        // Install device locale language if different from English and has model
        val localeLang = AvxLanguage.forCode(localeCode)
        if (localeLang != null && localeLang != AvxLanguage.ENGLISH &&
            localeLang.hasTransducerModel && !isModelInstalled(localeLang)) {
            if (downloadModel(localeLang)) count++
        }

        return count
    }

    /**
     * Build a HuggingFace download URL for a specific model file.
     *
     * HuggingFace raw file URL format:
     * https://huggingface.co/{repo}/resolve/main/{filename}
     */
    private fun buildDownloadUrl(repoId: String, filename: String): String {
        return "$HUGGINGFACE_BASE/$repoId/resolve/main/$filename"
    }

    /**
     * Download a file with progress reporting.
     */
    private fun downloadFile(
        url: String,
        outputFile: File,
        onProgress: (Float) -> Unit
    ) {
        val connection = URL(url).openConnection()
        connection.connectTimeout = 30_000
        connection.readTimeout = 60_000
        val totalBytes = connection.contentLengthLong
        var downloadedBytes = 0L

        BufferedInputStream(connection.getInputStream()).use { input ->
            FileOutputStream(outputFile).use { output ->
                val buffer = ByteArray(8192)
                var bytesRead: Int
                while (input.read(buffer).also { bytesRead = it } != -1) {
                    output.write(buffer, 0, bytesRead)
                    downloadedBytes += bytesRead
                    if (totalBytes > 0) {
                        onProgress((downloadedBytes.toFloat() / totalBytes).coerceIn(0f, 1f))
                    }
                }
            }
        }
    }
}
