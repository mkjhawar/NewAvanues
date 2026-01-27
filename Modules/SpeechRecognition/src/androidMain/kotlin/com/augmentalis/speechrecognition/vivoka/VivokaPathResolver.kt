/**
 * VivokaPathResolver.kt - Multi-location path resolution for Vivoka model files
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Created: 2025-11-20
 * Updated: 2026-01-27 - Migrated to KMP androidMain
 *
 * Provides fallback mechanism for locating Vivoka model files across multiple
 * storage locations before triggering downloads.
 */
package com.augmentalis.speechrecognition.vivoka

import android.content.Context
import android.os.Environment
import android.util.Log
import java.io.File

/**
 * Resolves Vivoka VSDK paths with external storage fallback
 *
 * Priority order:
 * 1. Internal app storage (default/current)
 * 2. External app-specific storage (accessible via file manager)
 * 3. Shared hidden folder (survives uninstall)
 * 4. If none found, returns primary path for download
 */
class VivokaPathResolver(private val context: Context) {

    companion object {
        private const val TAG = "VivokaPathResolver"

        // Directory constants
        private const val VSDK_DIR = "vsdk"
        private const val VIVOKA_MODELS_DIR = "vivoka_models"
        private const val VOICE_RESOURCE_SUBDIR = "data/csdk/asr"

        // Hidden folder name (survives uninstall)
        private const val SHARED_HIDDEN_DIR = ".voiceos"
        private const val SHARED_VIVOKA_SUBDIR = "vivoka"
    }

    /**
     * Resolve VSDK base path with fallback
     * Checks multiple locations before returning primary path for download
     *
     * @return Existing path or primary path for download
     */
    fun resolveVsdkPath(): File {
        val candidates = getVsdkCandidatePaths()

        // Check each candidate in priority order
        for (candidate in candidates) {
            if (isValidVsdkDirectory(candidate)) {
                Log.i(TAG, "Found valid VSDK directory at: ${candidate.absolutePath}")
                return candidate
            }
        }

        // None found - return primary (internal) for download
        val primary = candidates.first()
        Log.d(TAG, "No existing VSDK found. Will use primary: ${primary.absolutePath}")
        return primary
    }

    /**
     * Resolve language model path with fallback
     * Checks for pre-deployed models before triggering download
     *
     * @param languageCode Language code (e.g., "es", "fr", "ja")
     * @return Existing path or primary path for download
     */
    fun resolveLanguageModelPath(languageCode: String): File {
        val candidates = getLanguageModelCandidatePaths(languageCode)

        // Check each candidate in priority order
        for (candidate in candidates) {
            if (isValidLanguageModelDirectory(candidate)) {
                Log.i(TAG, "Found valid model for '$languageCode' at: ${candidate.absolutePath}")
                return candidate
            }
        }

        // None found - return primary (internal) for download
        val primary = candidates.first()
        Log.d(TAG, "No existing model for '$languageCode'. Will download to: ${primary.absolutePath}")
        return primary
    }

    /**
     * Get all candidate paths for VSDK base directory
     * Ordered by priority
     */
    private fun getVsdkCandidatePaths(): List<File> {
        return listOfNotNull(
            // 1. Internal app storage (current default)
            File(context.filesDir, VSDK_DIR),

            // 2. External app-specific storage (accessible, survives until uninstall)
            context.getExternalFilesDir(VSDK_DIR),

            // 3. Shared hidden folder (survives uninstall)
            if (Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED) {
                File(Environment.getExternalStorageDirectory(), "$SHARED_HIDDEN_DIR/$SHARED_VIVOKA_SUBDIR/$VSDK_DIR")
            } else null
        )
    }

    /**
     * Get all candidate paths for language model directory
     * Ordered by priority
     */
    private fun getLanguageModelCandidatePaths(languageCode: String): List<File> {
        return listOfNotNull(
            // 1. Internal app storage (current default)
            // Per documentation: context.filesDir/vsdk/data/csdk/asr/
            File(context.filesDir, "$VSDK_DIR/$VOICE_RESOURCE_SUBDIR"),

            // 2. External app-specific storage
            context.getExternalFilesDir("$VSDK_DIR/$VOICE_RESOURCE_SUBDIR"),

            // 3. Shared hidden folder
            if (Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED) {
                File(
                    Environment.getExternalStorageDirectory(),
                    "$SHARED_HIDDEN_DIR/$SHARED_VIVOKA_SUBDIR/$VSDK_DIR/$VOICE_RESOURCE_SUBDIR"
                )
            } else null
        )
    }

    /**
     * Validate VSDK directory contains required structure
     *
     * Expected structure:
     * vsdk/
     * └── config/
     *     └── vsdk.json
     */
    private fun isValidVsdkDirectory(dir: File): Boolean {
        if (!dir.exists() || !dir.isDirectory) {
            return false
        }

        // Check for essential config file
        val configFile = File(dir, "config/vsdk.json")
        if (!configFile.exists()) {
            Log.d(TAG, "Missing vsdk.json in: ${dir.absolutePath}")
            return false
        }

        Log.d(TAG, "Valid VSDK directory: ${dir.absolutePath}")
        return true
    }

    /**
     * Validate language model directory contains required structure
     *
     * Expected structure per documentation:
     * data/csdk/asr/
     * ├── acmod/    # Acoustic models (required)
     * ├── clc/      # Language components (for dynamic grammar)
     * ├── ctx/      # Context files (required)
     * └── lm/       # Language models (for free speech)
     */
    private fun isValidLanguageModelDirectory(dir: File): Boolean {
        if (!dir.exists() || !dir.isDirectory) {
            return false
        }

        // Check for essential subdirectories (acmod and ctx are mandatory)
        val acmodDir = File(dir, "acmod")
        val ctxDir = File(dir, "ctx")

        if (!acmodDir.exists() || !acmodDir.isDirectory) {
            Log.d(TAG, "Missing acmod/ in: ${dir.absolutePath}")
            return false
        }

        if (!ctxDir.exists() || !ctxDir.isDirectory) {
            Log.d(TAG, "Missing ctx/ in: ${dir.absolutePath}")
            return false
        }

        // Verify acmod directory has files
        val acmodFiles = acmodDir.listFiles() ?: emptyArray()
        if (acmodFiles.isEmpty()) {
            Log.d(TAG, "Empty acmod/ in: ${dir.absolutePath}")
            return false
        }

        Log.d(TAG, "Valid language model directory: ${dir.absolutePath}")
        return true
    }

    /**
     * Get human-readable path list for logging/debugging
     */
    fun getSearchPathsForLogging(): String {
        val vsdkPaths = getVsdkCandidatePaths()
        return vsdkPaths.joinToString("\n") { "  - ${it.absolutePath}" }
    }

    /**
     * Check if external storage is available and writable
     */
    fun isExternalStorageAvailable(): Boolean {
        return Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED
    }

    /**
     * Get recommended manual deployment path for ADB push
     * Returns the shared hidden folder path (survives uninstall)
     */
    fun getManualDeploymentPath(): File {
        return File(
            Environment.getExternalStorageDirectory(),
            "$SHARED_HIDDEN_DIR/$SHARED_VIVOKA_SUBDIR/$VSDK_DIR"
        )
    }
}
