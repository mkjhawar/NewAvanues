/**
 * AvxModelManager.kt - AVX model download, encryption, and lifecycle management
 *
 * Copyright (C) Augmentalis Inc, Intelligent Devices LLC
 * Created: 2026-02-24
 *
 * Manages AVX language model files:
 * 1. Download raw ONNX from CDN/GitHub releases
 * 2. Encrypt via AONCodec.wrap() -> Ava-AvxS-{Lang}.aon
 * 3. Store in /sdcard/ava-ai-models/avx/
 * 4. Track installed models in local inventory
 * 5. Delete/update models on demand
 */
package com.augmentalis.speechrecognition.avx

import android.content.Context
import android.util.Log
import com.augmentalis.crypto.aon.AONCodec
import com.augmentalis.crypto.aon.AONFormat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.net.URL

/**
 * Download state for UI integration.
 */
sealed class AvxDownloadState {
    object Idle : AvxDownloadState()
    data class Downloading(val language: AvxLanguage, val progress: Float) : AvxDownloadState()
    data class Encrypting(val language: AvxLanguage) : AvxDownloadState()
    data class Complete(val language: AvxLanguage) : AvxDownloadState()
    data class Error(val language: AvxLanguage, val message: String) : AvxDownloadState()
}

/**
 * Manages AVX model files: download, encrypt, store, and query.
 */
class AvxModelManager(
    private val context: Context
) {
    companion object {
        private const val TAG = "AvxModelManager"

        /**
         * Base URL for AVX model downloads.
         * Sherpa-ONNX models are hosted on HuggingFace/GitHub.
         * This URL will be configured via Firebase Remote Config in production.
         */
        private const val MODEL_BASE_URL = "https://huggingface.co/csukuangfj/sherpa-onnx-streaming-zipformer-bilingual-zh-en-2023-02-20/resolve/main/"
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

        return AvxLanguage.entries.mapNotNull { lang ->
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
     * 1. Download raw ONNX from CDN -> temp file
     * 2. AONCodec.wrap(tempFile) -> Ava-AvxS-{Lang}.aon
     * 3. Store in model directory
     * 4. Delete temp file
     *
     * @return true if download and encryption succeeded
     */
    suspend fun downloadModel(language: AvxLanguage): Boolean = withContext(Dispatchers.IO) {
        try {
            _downloadState.value = AvxDownloadState.Downloading(language, 0f)

            // Ensure model directory exists
            if (!modelDir.exists()) modelDir.mkdirs()

            // Step 1: Download raw ONNX to temp file
            val tempDir = File(context.cacheDir, "avx_download")
            tempDir.mkdirs()
            val tempFile = File(tempDir, "model_${language.langCode}.onnx")

            val downloadUrl = getDownloadUrl(language)
            Log.i(TAG, "Downloading AVX model: ${language.displayName} from $downloadUrl")

            val connection = URL(downloadUrl).openConnection()
            val totalBytes = connection.contentLengthLong
            var downloadedBytes = 0L

            connection.getInputStream().use { input ->
                tempFile.outputStream().use { output ->
                    val buffer = ByteArray(8192)
                    var bytesRead: Int
                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        downloadedBytes += bytesRead
                        if (totalBytes > 0) {
                            _downloadState.value = AvxDownloadState.Downloading(
                                language,
                                (downloadedBytes.toFloat() / totalBytes).coerceIn(0f, 1f)
                            )
                        }
                    }
                }
            }

            Log.i(TAG, "Download complete: ${tempFile.length()} bytes")

            // Step 2: Encrypt with AON codec
            _downloadState.value = AvxDownloadState.Encrypting(language)

            val rawBytes = tempFile.readBytes()
            val aonBytes = AONFormat.wrap(
                payload = rawBytes,
                modelId = "avx-${language.langCode}",
                platform = "android"
            )

            // Step 3: Write encrypted model to storage
            val outputFile = File(modelDir, language.aonFileName)
            outputFile.writeBytes(aonBytes)

            Log.i(TAG, "AON encrypted: ${outputFile.absolutePath} (${outputFile.length()} bytes)")

            // Step 4: Clean up temp file
            tempFile.delete()
            tempDir.delete()

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

        // Install device locale language if different from English
        val localeLang = AvxLanguage.forCode(localeCode)
        if (localeLang != null && localeLang != AvxLanguage.ENGLISH && !isModelInstalled(localeLang)) {
            if (downloadModel(localeLang)) count++
        }

        return count
    }

    /**
     * Get the download URL for a language model.
     * In production, this will be resolved via Firebase Remote Config.
     */
    private fun getDownloadUrl(language: AvxLanguage): String {
        // Sherpa-ONNX model naming varies by language — this mapping will be
        // refined when actual model URLs are configured in Firebase Remote Config
        return "${MODEL_BASE_URL}${language.langCode}/model.onnx"
    }
}
