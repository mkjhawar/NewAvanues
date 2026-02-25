package com.augmentalis.nlu

import android.content.Context
import com.augmentalis.ava.core.common.AVAException
import com.augmentalis.ava.core.common.Result
import com.augmentalis.nlu.ava.AssetExtractor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.URL
import java.security.MessageDigest
import java.util.zip.ZipInputStream

/**
 * Language Pack Manager - Download and manage .ava language packs
 *
 * Manages downloadable language packs for AVA intent system.
 * Supports on-demand download of locale-specific .ava files.
 *
 * VOS4 Pattern: Progressive download with offline-first design
 *
 * Architecture:
 * - Built-in: en-US (included in APK at /.ava/core/en-US/)
 * - Downloadable: All other languages (downloaded to /.ava/core/{locale}/)
 * - Language packs: .avapack files (ZIP archives with .ava files + manifest)
 *
 * Manifest Structure (/.ava/core/manifest.json):
 * {
 *   "s": "ava-manifest-1.0",
 *   "v": "1.0.0",
 *   "packs": [
 *     {
 *       "l": "en-US",
 *       "sz": 125000,
 *       "url": "https://ava.augmentalis.com/lang/en-US-v1.0.0.avapack",
 *       "h": "sha256:abc123...",
 *       "d": 1700179200,
 *       "built_in": true
 *     }
 *   ],
 *   "installed": ["en-US"],
 *   "active": "en-US"
 * }
 *
 * .avapack Structure (ZIP file):
 * - manifest.json (pack metadata)
 * - smart-home.ava
 * - productivity.ava
 * - information.ava
 * - system.ava
 * - (additional .ava files)
 *
 * Features:
 * - Download language packs on demand
 * - SHA-256 verification for security
 * - Atomic installation (temp → final)
 * - Progress callbacks
 * - Auto-cleanup of incomplete downloads
 * - Offline-first (use installed packs, download if missing)
 *
 * Usage:
 *   val manager = LanguagePackManager(context)
 *   manager.downloadLanguagePack("es-ES") { progress ->
 *       nluLogDebug("Download", "Progress: $progress%")
 *   }
 *   manager.setActiveLanguage("es-ES")
 */
class LanguagePackManager(
    private val context: Context
) {
    companion object {
        private const val TAG = "LanguagePackManager"

        // Download settings
        private const val CONNECT_TIMEOUT_MS = 15000
        private const val READ_TIMEOUT_MS = 30000
        private const val CHUNK_SIZE = 8192
    }

    // Use app-specific storage paths
    private val storageBase = AssetExtractor.getStorageBasePath(context)
    private val coreDir = "$storageBase/core"
    private val manifestPath = "$coreDir/manifest.json"
    private val cacheDir = "$storageBase/cache"

    /**
     * Language pack metadata
     */
    data class LanguagePack(
        val locale: String,
        val size: Long,
        val downloadUrl: String,
        val sha256Hash: String,
        val date: Long,
        val isBuiltIn: Boolean,
        val isInstalled: Boolean
    )

    /**
     * Download progress callback
     */
    fun interface ProgressCallback {
        fun onProgress(bytesDownloaded: Long, totalBytes: Long, percentage: Int)
    }

    /**
     * Load manifest from disk
     *
     * @return Manifest JSON object or throws exception if not found
     */
    private fun loadManifest(): JSONObject {
        val manifestFile = File(manifestPath)
        if (!manifestFile.exists()) {
            throw AVAException.ResourceNotFoundException("Manifest not found: $manifestPath")
        }

        val jsonString = manifestFile.readText()
        return JSONObject(jsonString)
    }

    /**
     * Save manifest to disk
     *
     * @param manifest Manifest JSON object
     */
    private fun saveManifest(manifest: JSONObject) {
        val manifestFile = File(manifestPath)
        manifestFile.writeText(manifest.toString(2))
        nluLogDebug(TAG, "Manifest saved: $manifestPath")
    }

    /**
     * Get all available language packs
     *
     * @return List of all language packs (built-in + downloadable)
     */
    fun getAvailableLanguagePacks(): List<LanguagePack> {
        val manifest = loadManifest()
        val packs = mutableListOf<LanguagePack>()

        val packsArray = manifest.getJSONArray("packs")
        val installed = manifest.getJSONArray("installed").toStringList()

        for (i in 0 until packsArray.length()) {
            val packJson = packsArray.getJSONObject(i)

            val pack = LanguagePack(
                locale = packJson.getString("l"),
                size = packJson.getLong("sz"),
                downloadUrl = packJson.getString("url"),
                sha256Hash = packJson.getString("h"),
                date = packJson.getLong("d"),
                isBuiltIn = packJson.optBoolean("built_in", false),
                isInstalled = packJson.getString("l") in installed
            )

            packs.add(pack)
        }

        return packs
    }

    /**
     * Get installed language packs
     *
     * @return List of installed language pack locales
     */
    fun getInstalledLanguages(): List<String> {
        val manifest = loadManifest()
        return manifest.getJSONArray("installed").toStringList()
    }

    /**
     * Get active language
     *
     * @return Active language locale code
     */
    fun getActiveLanguage(): String {
        val manifest = loadManifest()
        return manifest.getString("active")
    }

    /**
     * Set active language
     *
     * @param locale Language locale code (e.g., "es-ES")
     * @return Result indicating success or error if language not installed
     */
    suspend fun setActiveLanguage(locale: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val installed = getInstalledLanguages()
            if (locale !in installed) {
                return@withContext Result.Error(
                    exception = AVAException.LanguagePackException("Language not installed: $locale"),
                    message = "Language not installed: $locale. Call downloadLanguagePack() first."
                )
            }

            val manifest = loadManifest()
            manifest.put("active", locale)
            saveManifest(manifest)

            nluLogInfo(TAG, "Active language set to: $locale")
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(
                exception = e,
                message = "Failed to set active language: ${e.message}"
            )
        }
    }

    /**
     * Check if a language pack is installed
     *
     * @param locale Language locale code
     * @return True if installed, false otherwise
     */
    fun isLanguageInstalled(locale: String): Boolean {
        return locale in getInstalledLanguages()
    }

    /**
     * Download a language pack
     *
     * Downloads .avapack file from server, verifies SHA-256 hash, extracts to temp directory,
     * then moves to final location atomically.
     *
     * @param locale Language locale code (e.g., "es-ES")
     * @param progressCallback Optional progress callback
     * @return True if download and installation successful
     */
    suspend fun downloadLanguagePack(
        locale: String,
        progressCallback: ProgressCallback? = null
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            nluLogInfo(TAG, "Starting download for language pack: $locale")

            // Get pack metadata
            val pack = getAvailableLanguagePacks().find { it.locale == locale }
                ?: return@withContext false.also {
                    nluLogError(TAG, "Language pack not found: $locale")
                }

            if (pack.isInstalled) {
                nluLogInfo(TAG, "Language pack already installed: $locale")
                return@withContext true
            }

            // Create cache directory
            val cacheDirFile = File(cacheDir)
            if (!cacheDirFile.exists()) {
                cacheDirFile.mkdirs()
            }

            // Download to temp file
            val tempFile = File(cacheDir, "$locale.avapack.tmp")
            val success = downloadFile(pack.downloadUrl, tempFile, pack.size, progressCallback)

            if (!success) {
                nluLogError(TAG, "Download failed for $locale")
                tempFile.delete()
                return@withContext false
            }

            // Verify SHA-256 hash
            val actualHash = calculateSHA256(tempFile)
            if (actualHash != pack.sha256Hash && pack.sha256Hash != "pending") {
                nluLogError(TAG, "SHA-256 verification failed for $locale")
                nluLogError(TAG, "Expected: ${pack.sha256Hash}, Actual: $actualHash")
                tempFile.delete()
                return@withContext false
            }

            nluLogDebug(TAG, "SHA-256 verification passed for $locale")

            // Extract .avapack to temp directory
            val tempExtractDir = File(cacheDir, "$locale-extract")
            if (tempExtractDir.exists()) {
                tempExtractDir.deleteRecursively()
            }
            tempExtractDir.mkdirs()

            extractZip(tempFile, tempExtractDir)

            // Move to final location atomically
            val finalDir = File("$coreDir/$locale")
            if (finalDir.exists()) {
                finalDir.deleteRecursively()
            }
            tempExtractDir.renameTo(finalDir)

            // Update manifest
            updateManifestAfterInstall(locale)

            // Cleanup
            tempFile.delete()
            tempExtractDir.deleteRecursively()

            nluLogInfo(TAG, "Language pack installed successfully: $locale")
            true
        } catch (e: Exception) {
            nluLogError(TAG, "Failed to download language pack $locale: ${e.message}", e)
            false
        }
    }

    /**
     * Download file from URL with progress tracking
     *
     * @param url Download URL
     * @param outputFile Output file
     * @param expectedSize Expected file size in bytes
     * @param progressCallback Progress callback
     * @return True if download successful
     */
    private fun downloadFile(
        url: String,
        outputFile: File,
        expectedSize: Long,
        progressCallback: ProgressCallback?
    ): Boolean {
        try {
            val connection = URL(url).openConnection()
            connection.connectTimeout = CONNECT_TIMEOUT_MS
            connection.readTimeout = READ_TIMEOUT_MS
            connection.connect()

            val contentLength = connection.contentLength.toLong()
            if (contentLength != expectedSize && expectedSize > 0) {
                nluLogWarn(TAG, "Size mismatch: expected $expectedSize, got $contentLength")
            }

            // Use .use{} for automatic resource management (prevents leaks)
            var totalRead = 0L
            connection.getInputStream().use { inputStream ->
                FileOutputStream(outputFile).use { outputStream ->
                    val buffer = ByteArray(CHUNK_SIZE)
                    var bytesRead: Int

                    while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                        outputStream.write(buffer, 0, bytesRead)
                        totalRead += bytesRead

                        // Report progress
                        if (progressCallback != null && contentLength > 0) {
                            val percentage = ((totalRead * 100) / contentLength).toInt()
                            progressCallback.onProgress(totalRead, contentLength, percentage)
                        }
                    }
                } // outputStream automatically closed
            } // inputStream automatically closed

            nluLogDebug(TAG, "Download complete: ${outputFile.absolutePath} ($totalRead bytes)")
            return true
        } catch (e: Exception) {
            nluLogError(TAG, "Download failed: ${e.message}", e)
            return false
        }
    }

    /**
     * Calculate SHA-256 hash of file
     *
     * @param file File to hash
     * @return SHA-256 hash as hex string
     */
    private fun calculateSHA256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val buffer = ByteArray(CHUNK_SIZE)

        file.inputStream().use { inputStream ->
            var bytesRead: Int
            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                digest.update(buffer, 0, bytesRead)
            }
        }

        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    /**
     * Extract ZIP file to directory
     *
     * @param zipFile ZIP file to extract
     * @param outputDir Output directory
     */
    private fun extractZip(zipFile: File, outputDir: File) {
        ZipInputStream(zipFile.inputStream()).use { zis ->
            var entry = zis.nextEntry

            while (entry != null) {
                val file = File(outputDir, entry.name)

                if (entry.isDirectory) {
                    file.mkdirs()
                } else {
                    file.parentFile?.mkdirs()
                    FileOutputStream(file).use { fos ->
                        val buffer = ByteArray(CHUNK_SIZE)
                        var len: Int
                        while (zis.read(buffer).also { len = it } > 0) {
                            fos.write(buffer, 0, len)
                        }
                    }
                }

                zis.closeEntry()
                entry = zis.nextEntry
            }
        }

        nluLogDebug(TAG, "Extracted ZIP to ${outputDir.absolutePath}")
    }

    /**
     * Update manifest after successful installation
     *
     * @param locale Installed language locale
     */
    private fun updateManifestAfterInstall(locale: String) {
        val manifest = loadManifest()

        val installedArray = manifest.getJSONArray("installed")
        val installed = installedArray.toStringList().toMutableList()

        if (locale !in installed) {
            installed.add(locale)
        }

        // Replace installed array
        manifest.put("installed", JSONArray(installed))

        saveManifest(manifest)
        nluLogDebug(TAG, "Manifest updated: $locale added to installed list")
    }

    /**
     * Uninstall a language pack
     *
     * WARNING: Cannot uninstall built-in language (en-US)
     *
     * @param locale Language locale code
     * @return True if uninstalled successfully
     */
    suspend fun uninstallLanguagePack(locale: String): Boolean = withContext(Dispatchers.IO) {
        try {
            // Cannot uninstall built-in language
            if (locale == "en-US") {
                nluLogWarn(TAG, "Cannot uninstall built-in language: $locale")
                return@withContext false
            }

            // Cannot uninstall active language
            if (locale == getActiveLanguage()) {
                nluLogWarn(TAG, "Cannot uninstall active language: $locale. Set another language first.")
                return@withContext false
            }

            // Delete directory
            val langDir = File("$coreDir/$locale")
            if (langDir.exists()) {
                langDir.deleteRecursively()
                nluLogDebug(TAG, "Deleted language pack directory: ${langDir.absolutePath}")
            }

            // Update manifest
            val manifest = loadManifest()
            val installedArray = manifest.getJSONArray("installed")
            val installed = installedArray.toStringList().toMutableList()
            installed.remove(locale)
            manifest.put("installed", JSONArray(installed))
            saveManifest(manifest)

            nluLogInfo(TAG, "Language pack uninstalled: $locale")
            true
        } catch (e: Exception) {
            nluLogError(TAG, "Failed to uninstall language pack $locale: ${e.message}", e)
            false
        }
    }

    /**
     * Get download statistics
     *
     * @return Map with download stats (total size, installed count, etc.)
     */
    fun getDownloadStats(): Map<String, Any> {
        val packs = getAvailableLanguagePacks()
        val installed = packs.filter { it.isInstalled }

        val totalSize = packs.sumOf { it.size }
        val installedSize = installed.sumOf { it.size }

        return mapOf(
            "total_packs" to packs.size,
            "installed_packs" to installed.size,
            "total_size_bytes" to totalSize,
            "installed_size_bytes" to installedSize,
            "total_size_mb" to (totalSize / 1024 / 1024),
            "installed_size_mb" to (installedSize / 1024 / 1024),
            "active_language" to getActiveLanguage()
        )
    }

    /**
     * Helper: Convert JSONArray to List<String>
     */
    private fun JSONArray.toStringList(): List<String> {
        val list = mutableListOf<String>()
        for (i in 0 until length()) {
            list.add(getString(i))
        }
        return list
    }
}
