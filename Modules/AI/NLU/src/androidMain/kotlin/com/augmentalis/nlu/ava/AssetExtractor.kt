package com.augmentalis.nlu.ava

import android.content.Context
import com.augmentalis.ava.core.common.AVAException
import com.augmentalis.nlu.nluLogDebug
import com.augmentalis.nlu.nluLogError
import com.augmentalis.nlu.nluLogInfo
import java.io.File
import java.io.FileOutputStream

/**
 * Asset Extractor for .ava Files
 *
 * Extracts .ava intent files and manifest.json from app assets to device storage
 * on first launch. This enables IntentSourceCoordinator to load intents from .ava
 * files instead of falling back to JSON.
 *
 * Target Structure:
 * ```
 * /storage/emulated/0/.ava/
 * ├── core/
 * │   ├── manifest.json
 * │   └── en-US/
 * │       ├── information.ava        (time, weather queries)
 * │       ├── productivity.ava        (alarms, reminders)
 * │       ├── smart-home.ava          (lights, temperature)
 * │       ├── system.ava              (device control)
 * │       ├── navigation.ava          (VoiceOS navigation)
 * │       ├── media-control.ava       (VoiceOS media)
 * │       └── system-control.ava      (VoiceOS system)
 * ├── voiceos/
 * │   └── en-US/
 * └── user/
 *     └── en-US/
 * ```
 *
 * @param context Android application context
 */
class AssetExtractor(private val context: Context) {

    companion object {
        private const val TAG = "AssetExtractor"
        private const val PREF_NAME = "ava_asset_extraction"
        private const val PREF_KEY_EXTRACTED = "assets_extracted_v1"

        // Asset paths
        private const val ASSETS_BASE = "ava-examples"

        // Use app-specific external storage (no permissions required)
        // This resolves to: /storage/emulated/0/Android/data/com.augmentalis.ava.debug/files/.ava
        fun getStorageBasePath(context: Context): String {
            val externalFilesDir = context.getExternalFilesDir(null)
            return "${externalFilesDir?.absolutePath}/.ava"
        }
    }

    private val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    // Storage paths (use app-specific storage)
    private val storageBase = getStorageBasePath(context)
    private val corePath = "$storageBase/core"
    private val voiceosPath = "$storageBase/voiceos"
    private val userPath = "$storageBase/user"

    /**
     * Extract .ava files from assets to device storage if not already extracted
     *
     * @return true if extraction was performed, false if already extracted
     */
    suspend fun extractIfNeeded(): Boolean {
        if (isAlreadyExtracted()) {
            nluLogInfo(TAG, "Assets already extracted, skipping")
            return false
        }

        nluLogInfo(TAG, "Starting asset extraction...")

        try {
            // Create directory structure
            createDirectoryStructure()

            // Extract manifest.json
            extractManifest()

            // Extract .ava files
            extractAvaFiles()

            // Mark as extracted
            markAsExtracted()

            nluLogInfo(TAG, "Asset extraction complete")
            return true
        } catch (e: Exception) {
            nluLogError(TAG, "Asset extraction failed: ${e.message}", e)
            throw e
        }
    }

    /**
     * Force re-extraction (useful for testing or updates)
     */
    suspend fun forceExtraction() {
        nluLogInfo(TAG, "Force extraction requested")
        clearExtractionFlag()
        extractIfNeeded()
    }

    /**
     * Check if assets have already been extracted
     */
    private fun isAlreadyExtracted(): Boolean {
        val extracted = prefs.getBoolean(PREF_KEY_EXTRACTED, false)

        // Also verify files actually exist
        val manifestExists = File("$corePath/manifest.json").exists()

        return extracted && manifestExists
    }

    /**
     * Create directory structure on device storage
     */
    private fun createDirectoryStructure() {
        nluLogDebug(TAG, "Creating directory structure at: $storageBase")

        val directories = listOf(
            storageBase,
            corePath,
            "$corePath/en-US",
            voiceosPath,
            "$voiceosPath/en-US",
            userPath,
            "$userPath/en-US"
        )

        directories.forEach { path ->
            val dir = File(path)
            if (!dir.exists()) {
                val created = dir.mkdirs()
                if (created) {
                    nluLogDebug(TAG, "Created directory: $path")
                } else {
                    throw AVAException.ResourceNotFoundException("Failed to create directory: $path")
                }
            } else {
                nluLogDebug(TAG, "Directory already exists: $path")
            }
        }
    }

    /**
     * Extract manifest.json from assets
     */
    private fun extractManifest() {
        nluLogDebug(TAG, "Extracting manifest.json...")

        val manifestJson = """
            {
              "s": "ava-manifest-1.0",
              "v": "1.0.0",
              "packs": [
                {
                  "l": "en-US",
                  "sz": 125000,
                  "url": "",
                  "h": "",
                  "d": ${System.currentTimeMillis()},
                  "built_in": true
                }
              ],
              "installed": ["en-US"],
              "active": "en-US"
            }
        """.trimIndent()

        val manifestFile = File("$corePath/manifest.json")
        manifestFile.writeText(manifestJson)

        nluLogInfo(TAG, "Manifest extracted to: ${manifestFile.absolutePath}")
    }

    /**
     * Extract .ava files from assets to device storage
     */
    private fun extractAvaFiles() {
        nluLogDebug(TAG, "Extracting .ava files...")

        val locale = "en-US"
        val avaFiles = listOf(
            // Core conversational intents
            "information.ava",
            "productivity.ava",
            // VoiceOS control intents
            "navigation.ava",
            "media-control.ava",
            "system-control.ava"
        )

        var extractedCount = 0

        avaFiles.forEach { fileName ->
            try {
                val assetPath = "$ASSETS_BASE/$locale/$fileName"
                val targetPath = "$corePath/$locale/$fileName"

                extractAsset(assetPath, targetPath)
                extractedCount++

                nluLogDebug(TAG, "Extracted: $fileName")
            } catch (e: Exception) {
                nluLogError(TAG, "Failed to extract $fileName: ${e.message}", e)
                throw e
            }
        }

        nluLogInfo(TAG, "Extracted $extractedCount .ava files to $corePath/$locale/")
    }

    /**
     * Extract a single asset file to device storage
     */
    private fun extractAsset(assetPath: String, targetPath: String) {
        context.assets.open(assetPath).use { inputStream ->
            FileOutputStream(targetPath).use { outputStream ->
                inputStream.copyTo(outputStream)
            }
        }
    }

    /**
     * Mark assets as extracted in SharedPreferences
     */
    private fun markAsExtracted() {
        prefs.edit().putBoolean(PREF_KEY_EXTRACTED, true).apply()
        nluLogDebug(TAG, "Marked assets as extracted")
    }

    /**
     * Clear extraction flag (for testing)
     */
    private fun clearExtractionFlag() {
        prefs.edit().putBoolean(PREF_KEY_EXTRACTED, false).apply()
        nluLogDebug(TAG, "Cleared extraction flag")
    }

    /**
     * Get extraction status information
     */
    fun getExtractionStatus(): ExtractionStatus {
        val extracted = prefs.getBoolean(PREF_KEY_EXTRACTED, false)
        val manifestExists = File("$corePath/manifest.json").exists()

        val avaFilesExist = listOf(
            "$corePath/en-US/information.ava",
            "$corePath/en-US/productivity.ava",
            "$corePath/en-US/navigation.ava",
            "$corePath/en-US/media-control.ava",
            "$corePath/en-US/system-control.ava"
        ).all { File(it).exists() }

        return ExtractionStatus(
            extracted = extracted,
            manifestExists = manifestExists,
            avaFilesExist = avaFilesExist,
            corePath = corePath
        )
    }
}

/**
 * Extraction status data class
 */
data class ExtractionStatus(
    val extracted: Boolean,
    val manifestExists: Boolean,
    val avaFilesExist: Boolean,
    val corePath: String
)
