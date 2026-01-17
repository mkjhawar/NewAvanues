/**
 * Language Pack Manager
 *
 * Single Responsibility: Manage downloading, installing, and switching language packs
 *
 * Handles:
 * - Fetching available language packs from CDN
 * - Downloading language-specific LLM models
 * - Validating downloads with checksums
 * - Managing local storage of language packs
 * - Cleaning up unused languages
 *
 * Architecture:
 * - English base pack always included
 * - Optional language packs downloaded on demand
 * - Progressive download with resume support
 * - Storage quota management
 *
 * Created: 2025-10-31
 * Author: AVA Team
 */

package com.augmentalis.llm.alc.language

import android.content.Context
import com.augmentalis.llm.LLMResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import timber.log.Timber
import java.io.File
import java.net.URL
import java.security.MessageDigest

/**
 * Manages language packs for multilingual support
 *
 * @param context Android context for accessing storage
 * @param manifestUrl URL to language pack manifest (CDN)
 * @param filterLocalizedOnly If true, only show languages with app localization
 */
class LanguagePackManager(
    private val context: Context,
    private val manifestUrl: String = DEFAULT_MANIFEST_URL,
    private val filterLocalizedOnly: Boolean = true
) {
    // Use external storage for easier model deployment via adb
    private val packDir: File by lazy {
        val externalDir = context.getExternalFilesDir(null)
        val baseDir = externalDir ?: context.filesDir
        File(baseDir, "models/llm").apply {
            if (!exists()) {
                mkdirs()
            }
        }
    }
    private val json = Json { ignoreUnknownKeys = true }
    private val downloadProgressCallbacks = mutableMapOf<String, (Float) -> Unit>()
    private val localizationFilter = LocalizedLanguageFilter(context)

    /**
     * Get list of available language packs from CDN
     *
     * Only returns languages where app has full localization
     * (unless filterLocalizedOnly = false)
     */
    suspend fun getAvailableLanguages(): LLMResult<List<LanguagePack>> = withContext(Dispatchers.IO) {
        try {
            val manifestText = URL(manifestUrl).readText()
            val manifest = json.decodeFromString<LanguageManifest>(manifestText)

            // Filter to only show localized languages
            val languages = if (filterLocalizedOnly) {
                localizationFilter.filterLocalizedOnly(manifest.languages)
            } else {
                manifest.languages
            }

            Timber.d("Available languages: ${languages.map { it.code }.joinToString(", ")}")

            LLMResult.Success(languages)
        } catch (e: Exception) {
            Timber.e(e, "Failed to fetch language manifest")
            LLMResult.Error(
                message = "Failed to fetch available languages: ${e.message}",
                cause = e
            )
        }
    }

    /**
     * Get language pack with localization info
     *
     * @param languageCode ISO 639-1 code
     * @return Language pack with localization metadata
     */
    suspend fun getLanguagePackInfo(languageCode: String): LLMResult<LocalizedLanguagePack> {
        val availableResult = getAvailableLanguages()
        if (availableResult is LLMResult.Error) {
            return LLMResult.Error(
                message = availableResult.message,
                cause = availableResult.cause
            )
        }

        val pack = (availableResult as? LLMResult.Success)?.data
            ?.find { it.code == languageCode }
            ?: return LLMResult.Error(
                message = "Language not found: $languageCode",
                cause = IllegalArgumentException("Language not found: $languageCode")
            )

        return LLMResult.Success(pack.withLocalizationInfo(context))
    }

    /**
     * Check if a language pack is installed
     */
    fun isLanguageInstalled(languageCode: String): Boolean {
        val langDir = File(packDir, languageCode)
        val completionMarker = File(langDir, ".download_complete")
        return langDir.exists() && completionMarker.exists()
    }

    /**
     * Get list of installed languages
     */
    fun getInstalledLanguages(): List<String> {
        return packDir.listFiles()
            ?.filter { it.isDirectory && isLanguageInstalled(it.name) }
            ?.map { it.name}
            ?: listOf("en") // Default to English
    }

    /**
     * Download a language pack
     *
     * @param languageCode Language to download (e.g., "es", "fr")
     * @param progressCallback Optional progress callback (0.0 to 1.0)
     */
    suspend fun downloadLanguagePack(
        languageCode: String,
        progressCallback: ((Float) -> Unit)? = null
    ): LLMResult<Unit> = withContext(Dispatchers.IO) {
        try {
            // Get pack info
            val availableResult = getAvailableLanguages()
            if (availableResult is LLMResult.Error) {
                return@withContext availableResult
            }

            val pack = (availableResult as? LLMResult.Success)?.data
                ?.find { it.code == languageCode }
                ?: return@withContext LLMResult.Error(
                    message = "Language not found: $languageCode",
                    cause = IllegalArgumentException("Language not found: $languageCode")
                )

            // Check if already installed
            if (isLanguageInstalled(languageCode)) {
                return@withContext LLMResult.Success(Unit)
            }

            // Check storage space
            val availableSpace = getAvailableStorageSpace()
            if (availableSpace < pack.sizeBytes) {
                return@withContext LLMResult.Error(
                    message = "Insufficient storage: need ${formatBytes(pack.sizeBytes)}, have ${formatBytes(availableSpace)}",
                    cause = IllegalStateException("Insufficient storage")
                )
            }

            Timber.i("Downloading language pack: $languageCode (${formatBytes(pack.sizeBytes)})")

            // Create language directory
            val langDir = File(packDir, languageCode)
            langDir.mkdirs()

            // Store callback
            if (progressCallback != null) {
                downloadProgressCallbacks[languageCode] = progressCallback
            }

            // Download model file
            val modelFile = File(langDir, pack.llmModel)
            downloadFileWithProgress(
                url = pack.downloadUrl + pack.llmModel,
                destination = modelFile,
                expectedSize = pack.sizeBytes,
                expectedChecksum = pack.checksum,
                progressCallback = { progress ->
                    downloadProgressCallbacks[languageCode]?.invoke(progress)
                }
            )

            // Download metadata
            val metadataUrl = pack.downloadUrl + "metadata.json"
            val metadataFile = File(langDir, "metadata.json")
            downloadFile(metadataUrl, metadataFile)

            // Mark as complete
            File(langDir, ".download_complete").createNewFile()

            // Clear callback
            downloadProgressCallbacks.remove(languageCode)

            Timber.i("Language pack $languageCode downloaded successfully")
            LLMResult.Success(Unit)

        } catch (e: Exception) {
            Timber.e(e, "Failed to download language pack: $languageCode")
            downloadProgressCallbacks.remove(languageCode)
            LLMResult.Error(
                message = "Download failed: ${e.message}",
                cause = e
            )
        }
    }

    /**
     * Delete a language pack
     *
     * @param languageCode Language to delete (cannot delete "en")
     */
    suspend fun deleteLanguagePack(languageCode: String): LLMResult<Unit> = withContext(Dispatchers.IO) {
        if (languageCode == "en") {
            return@withContext LLMResult.Error(
                message = "Cannot delete English (base language)",
                cause = IllegalArgumentException("Cannot delete base language")
            )
        }

        return@withContext try {
            val langDir = File(packDir, languageCode)
            if (langDir.exists()) {
                val deleted = langDir.deleteRecursively()
                if (deleted) {
                    Timber.i("Deleted language pack: $languageCode")
                    LLMResult.Success(Unit)
                } else {
                    LLMResult.Error(
                        message = "Failed to delete directory",
                        cause = java.io.IOException("Failed to delete directory")
                    )
                }
            } else {
                LLMResult.Success(Unit) // Already deleted
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to delete language pack: $languageCode")
            LLMResult.Error(
                message = "Delete failed: ${e.message}",
                cause = e
            )
        }
    }

    /**
     * Get download progress for a language
     */
    fun getDownloadProgress(languageCode: String): Float? {
        return downloadProgressCallbacks[languageCode]?.let { 0f } // Placeholder
    }

    /**
     * Get storage information
     */
    fun getStorageInfo(): StorageInfo {
        val totalSize = getInstalledLanguages().sumOf { lang ->
            File(packDir, lang).walkTopDown()
                .filter { it.isFile }
                .sumOf { it.length() }
        }

        return StorageInfo(
            usedBytes = totalSize,
            availableBytes = getAvailableStorageSpace(),
            installedLanguages = getInstalledLanguages().size
        )
    }

    /**
     * Download a file with progress tracking
     */
    private fun downloadFileWithProgress(
        url: String,
        destination: File,
        expectedSize: Long,
        expectedChecksum: String,
        progressCallback: (Float) -> Unit
    ) {
        val connection = URL(url).openConnection()
        connection.connect()

        val totalBytes = connection.contentLength.toLong()
        var downloadedBytes = 0L

        connection.getInputStream().use { input ->
            destination.outputStream().use { output ->
                val buffer = ByteArray(8192)
                var bytes = input.read(buffer)

                while (bytes >= 0) {
                    output.write(buffer, 0, bytes)
                    downloadedBytes += bytes
                    progressCallback(downloadedBytes.toFloat() / totalBytes)
                    bytes = input.read(buffer)
                }
            }
        }

        // Verify checksum
        val actualChecksum = calculateChecksum(destination)
        if (actualChecksum != expectedChecksum) {
            destination.delete()
            throw SecurityException("Checksum mismatch: expected $expectedChecksum, got $actualChecksum")
        }
    }

    /**
     * Download a file (simple, no progress)
     */
    private fun downloadFile(url: String, destination: File) {
        URL(url).openStream().use { input ->
            destination.outputStream().use { output ->
                input.copyTo(output)
            }
        }
    }

    /**
     * Calculate SHA-256 checksum of a file
     */
    private fun calculateChecksum(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(8192)
            var bytes = input.read(buffer)
            while (bytes >= 0) {
                digest.update(buffer, 0, bytes)
                bytes = input.read(buffer)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    /**
     * Get available storage space
     */
    private fun getAvailableStorageSpace(): Long {
        val statFs = android.os.StatFs(context.filesDir.absolutePath)
        return statFs.availableBytes
    }

    /**
     * Format bytes to human-readable string
     */
    private fun formatBytes(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> "${bytes / 1024} KB"
            bytes < 1024 * 1024 * 1024 -> "${bytes / (1024 * 1024)} MB"
            else -> "%.1f GB".format(bytes / (1024.0 * 1024.0 * 1024.0))
        }
    }

    companion object {
        private const val DEFAULT_MANIFEST_URL = "https://cdn.augmentalis.com/ava/language_packs.json"
    }
}

/**
 * Language pack manifest (from CDN)
 */
@Serializable
data class LanguageManifest(
    val version: String,
    val baseUrl: String,
    val languages: List<LanguagePack>
)

/**
 * Language pack metadata
 */
@Serializable
data class LanguagePack(
    val code: String,
    val name: String,
    val required: Boolean,
    val sizeBytes: Long,
    val llmModel: String,
    val checksum: String,
    val downloadUrl: String
)

/**
 * Storage information
 */
data class StorageInfo(
    val usedBytes: Long,
    val availableBytes: Long,
    val installedLanguages: Int
)
