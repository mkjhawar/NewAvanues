/**
 * SpeechModelPathResolver.kt - Universal path resolution for speech recognition models
 *
 * Copyright (C) Augmentalis Inc, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Created: 2025-11-21
 * Updated: 2025-11-22 - Added permission checks for external storage access
 *
 * Provides fallback mechanism for locating speech recognition model files across multiple
 * storage locations before triggering downloads. Used by all speech engines.
 *
 * Priority order for model discovery (with permissions):
 * 1. APK assets folder (if models bundled)
 * 2. Internal app storage (default/current) - always accessible
 * 3. External app-specific storage (accessible via file manager) - requires permission
 * 4. Shared hidden folder (survives uninstall) - requires permission
 * 5. If none found, returns primary path for download
 *
 * Without storage permissions, only internal storage is checked.
 */
package com.augmentalis.voiceos.speech.engines.common

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Environment
import android.util.Log
import androidx.core.content.ContextCompat
import java.io.File

/**
 * Resolves speech model paths with external storage fallback
 * Generic implementation used by all speech engines (Vivoka, Whisper, Vosk, etc.)
 */
class SpeechModelPathResolver(
    private val context: Context,
    private val engineName: String,
    private val modelDirName: String
) {

    companion object {
        private const val TAG = "SpeechModelPathResolver"

        // Hidden folder name (survives uninstall)
        private const val SHARED_HIDDEN_DIR = ".voiceos"
        private const val MODELS_SUBDIR = "models"
    }

    /**
     * Resolve model directory path with fallback
     * Checks multiple locations before returning primary path for download
     *
     * @param validationFunction Optional function to validate if directory is valid
     * @return Existing path or primary path for download
     */
    fun resolveModelPath(validationFunction: ((File) -> Boolean)? = null): File {
        val candidates = getModelCandidatePaths()

        // Check each candidate in priority order
        for (candidate in candidates) {
            // Use custom validation if provided, otherwise check basic existence
            val isValid = if (validationFunction != null) {
                validationFunction(candidate)
            } else {
                candidate.exists() && candidate.isDirectory
            }

            if (isValid) {
                Log.i(TAG, "[$engineName] Found valid model directory at: ${candidate.absolutePath}")
                return candidate
            }
        }

        // None found - return primary (internal) for download
        val primary = candidates.first()
        Log.d(TAG, "[$engineName] No existing models found. Will use primary: ${primary.absolutePath}")
        return primary
    }

    /**
     * Get all candidate paths for model directory
     * Ordered by priority
     *
     * External storage paths are only included if storage permissions are granted
     */
    private fun getModelCandidatePaths(): List<File> {
        val hasPermission = hasStoragePermission()

        Log.d(TAG, "[$engineName] Building candidate paths (permission: $hasPermission)")

        return listOfNotNull(
            // 1. Internal app storage (current default) - ALWAYS accessible
            File(context.filesDir, modelDirName),

            // 2. External app-specific storage (accessible, survives until uninstall) - Requires permission
            if (hasPermission) {
                context.getExternalFilesDir(modelDirName)
            } else {
                Log.d(TAG, "[$engineName] Skipping external app-specific storage (no permission)")
                null
            },

            // 3. Shared hidden folder (survives uninstall) - Requires permission
            if (hasPermission && Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED) {
                File(
                    Environment.getExternalStorageDirectory(),
                    "$SHARED_HIDDEN_DIR/$MODELS_SUBDIR/$engineName/$modelDirName"
                )
            } else {
                if (!hasPermission) {
                    Log.d(TAG, "[$engineName] Skipping shared hidden folder (no permission)")
                }
                null
            }
        )
    }

    /**
     * Check if storage permissions are granted
     * Handles API level differences
     */
    private fun hasStoragePermission(): Boolean {
        val requiredPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // API 30+: Only READ_EXTERNAL_STORAGE needed for reading shared storage
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        } else {
            // API 23-29: Both READ and WRITE needed
            arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
        }

        return requiredPermissions.all { permission ->
            ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
        }
    }

    /**
     * Check if models exist in APK assets folder
     * @param assetsSubPath Subpath within assets/ to check (e.g., "whisper_models", "vosk_models")
     * @return true if assets exist, false otherwise
     */
    fun hasModelsInAssets(assetsSubPath: String): Boolean {
        return try {
            val assetList = context.assets.list(assetsSubPath)
            assetList?.isNotEmpty() == true
        } catch (e: Exception) {
            Log.d(TAG, "[$engineName] No models found in assets/$assetsSubPath")
            false
        }
    }

    /**
     * Extract models from APK assets to target directory
     * @param assetsSubPath Subpath within assets/ to extract from
     * @param targetDir Target directory to extract to
     * @return true if extraction successful, false otherwise
     */
    fun extractModelsFromAssets(assetsSubPath: String, targetDir: File): Boolean {
        return try {
            Log.i(TAG, "[$engineName] Extracting models from assets/$assetsSubPath to ${targetDir.absolutePath}")

            // Create target directory
            if (!targetDir.exists()) {
                targetDir.mkdirs()
            }

            // List all files in assets subdirectory
            val assetFiles = context.assets.list(assetsSubPath) ?: emptyArray()

            if (assetFiles.isEmpty()) {
                Log.w(TAG, "[$engineName] No assets found in $assetsSubPath")
                return false
            }

            // Extract each file
            for (fileName in assetFiles) {
                val assetPath = "$assetsSubPath/$fileName"
                val outputFile = File(targetDir, fileName)

                context.assets.open(assetPath).use { input ->
                    outputFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }

                Log.d(TAG, "[$engineName] Extracted: $fileName")
            }

            Log.i(TAG, "[$engineName] Successfully extracted ${assetFiles.size} model files")
            true

        } catch (e: Exception) {
            Log.e(TAG, "[$engineName] Failed to extract models from assets", e)
            false
        }
    }

    /**
     * Get human-readable path list for logging/debugging
     */
    fun getSearchPathsForLogging(): String {
        val paths = getModelCandidatePaths()
        return paths.joinToString("\n") { "  - ${it.absolutePath}" }
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
            "$SHARED_HIDDEN_DIR/$MODELS_SUBDIR/$engineName/$modelDirName"
        )
    }

    /**
     * Get ADB push command for manual deployment
     */
    fun getAdbPushCommand(localPath: String = "./$modelDirName"): String {
        val remotePath = getManualDeploymentPath().absolutePath
        return "adb push $localPath $remotePath"
    }
}
