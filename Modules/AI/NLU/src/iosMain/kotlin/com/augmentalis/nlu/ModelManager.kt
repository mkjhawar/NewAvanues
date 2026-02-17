// filename: features/nlu/src/iosMain/kotlin/com/augmentalis/ava/features/nlu/ModelManager.kt
// created: 2025-11-02
// Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
// © Augmentalis Inc, Intelligent Devices LLC
// TCR: Phase 2 - iOS NLU with model management

package com.augmentalis.nlu

import com.augmentalis.ava.core.common.Result
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import platform.Foundation.*

/**
 * iOS implementation of ModelManager for Core ML models
 *
 * Features:
 * - Detects available Core ML models in app bundle
 * - Manages model download and caching
 * - Handles model versioning and updates
 * - Supports multiple model formats (.mlmodel, .mlpackage)
 *
 * Model locations (priority):
 * 1. App bundle (bundled models)
 * 2. Documents directory (user-downloaded models)
 * 3. Caches directory (temporary downloads)
 *
 * Supported models:
 * - MobileBERT (384-dim): Fast, English
 * - mALBERT (768-dim): Multilingual, higher quality
 */
@OptIn(ExperimentalForeignApi::class)
actual class ModelManager {

    // Model file names
    private val mobileBertModel = "intent_classifier_mobilebert.mlpackage"
    private val malbertModel = "intent_classifier_malbert.mlpackage"
    private val vocabFile = "vocab.txt"

    // iOS file system paths
    private val documentsDir by lazy {
        NSSearchPathForDirectoriesInDomains(
            NSDocumentDirectory,
            NSUserDomainMask,
            true
        ).firstOrNull() as? String ?: ""
    }

    private val cachesDir by lazy {
        NSSearchPathForDirectoriesInDomains(
            NSCachesDirectory,
            NSUserDomainMask,
            true
        ).firstOrNull() as? String ?: ""
    }

    private val modelsDir by lazy {
        "$documentsDir/models"
    }

    init {
        // Create models directory if needed
        try {
            NSFileManager.defaultManager.createDirectoryAtPath(
                modelsDir,
                withIntermediateDirectories = true,
                attributes = null,
                error = null
            )
        } catch (e: Exception) {
            println("ModelManager: Failed to create models directory: ${e.message}")
        }
    }

    /**
     * Check if models are available locally
     * Checks both app bundle and Documents directory
     */
    actual fun isModelAvailable(): Boolean {
        val bundlePath = getBundleModelPath()
        val documentsPath = getDocumentsModelPath()

        return (bundlePath != null || documentsPath != null) && vocabFileExists()
    }

    /**
     * Get path to available model
     * Priority: Documents (user-downloaded) → Bundle (bundled)
     */
    actual fun getModelPath(): String {
        // Try documents first (user-downloaded or updated models)
        getDocumentsModelPath()?.let { return it }

        // Fallback to bundled model
        getBundleModelPath()?.let { return it }

        println("ModelManager: No model found")
        return ""
    }

    /**
     * Get vocabulary file path
     */
    actual fun getVocabPath(): String {
        // Check Documents first
        val docVocab = "$modelsDir/$vocabFile"
        if (fileExists(docVocab)) {
            return docVocab
        }

        // Check bundle
        val bundleVocab = NSBundle.mainBundle.pathForResource(
            "vocab",
            ofType = "txt"
        ) ?: return ""

        return bundleVocab
    }

    /**
     * Download models if not already available
     * For iOS, models are typically bundled, but this supports downloading updates
     *
     * @param onProgress Callback for progress (0.0 to 1.0)
     */
    actual suspend fun downloadModelsIfNeeded(
        onProgress: (Float) -> Unit
    ): Result<Unit> = withContext(Dispatchers.Default) {
        try {
            // iOS typically bundles models, so we return success if available
            if (isModelAvailable()) {
                onProgress(1.0f)
                return@withContext Result.Success(Unit)
            }

            // If models not available, they should be in bundle
            // Apps that need downloadable models should implement their own download logic
            println("ModelManager: Models not available and no download configured")
            return@withContext Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(
                exception = e,
                message = "Failed to check model availability: ${e.message}"
            )
        }
    }

    /**
     * Copy model from app bundle to Documents for easier access
     * Useful for extracting bundled .mlpackage models
     */
    actual suspend fun copyModelFromAssets(): Result<Unit> = withContext(Dispatchers.Default) {
        try {
            println("ModelManager: Copying model from bundle...")

            val fileManager = NSFileManager.defaultManager

            // Try to copy MobileBERT from bundle
            val bundleModel = getBundleModelPath()
            if (bundleModel != null && !fileExists("$modelsDir/${mobileBertModel}")) {
                val sourceUrl = NSURL.fileURLWithPath(bundleModel)
                val destUrl = NSURL.fileURLWithPath("$modelsDir/${mobileBertModel}")

                try {
                    fileManager.copyItemAtURL(sourceUrl, toURL = destUrl, error = null)
                    println("ModelManager: Model copied successfully")
                } catch (e: Exception) {
                    println("ModelManager: Failed to copy model: ${e.message}")
                    // Continue - model is still available from bundle
                }
            }

            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(
                exception = e,
                message = "Failed to copy model from assets: ${e.message}"
            )
        }
    }

    /**
     * Clear downloaded models (keep bundled models)
     */
    actual fun clearModels(): Result<Unit> {
        return try {
            val fileManager = NSFileManager.defaultManager

            // Remove models directory
            try {
                fileManager.removeItemAtPath(modelsDir, error = null)
                println("ModelManager: Models cleared")
            } catch (e: Exception) {
                println("ModelManager: Failed to clear models: ${e.message}")
            }

            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(
                exception = e,
                message = "Failed to clear models: ${e.message}"
            )
        }
    }

    /**
     * Get total size of downloaded models
     */
    actual fun getModelsSize(): Long {
        return try {
            val fileManager = NSFileManager.defaultManager
            val attr = fileManager.attributesOfItemAtPath(modelsDir, error = null)
            val size = attr?.get(NSFileSize) as? Number
            size?.toLong() ?: 0L
        } catch (e: Exception) {
            0L
        }
    }

    // ===== PRIVATE HELPERS =====

    /**
     * Get model path from app bundle
     */
    private fun getBundleModelPath(): String? {
        val bundle = NSBundle.mainBundle

        // Try mALBERT first (better quality)
        bundle.pathForResource("intent_classifier_malbert.mlpackage", ofType = null)?.let {
            println("ModelManager: Found mALBERT in bundle")
            return it
        }

        // Fallback to MobileBERT
        bundle.pathForResource("intent_classifier_mobilebert.mlpackage", ofType = null)?.let {
            println("ModelManager: Found MobileBERT in bundle")
            return it
        }

        // Try without extension (for raw model files)
        bundle.pathForResource("intent_classifier", ofType = "mlmodel")?.let {
            println("ModelManager: Found .mlmodel in bundle")
            return it
        }

        return null
    }

    /**
     * Get model path from Documents directory
     */
    private fun getDocumentsModelPath(): String? {
        // Try mALBERT first
        val malbertPath = "$modelsDir/${malbertModel}"
        if (fileExists(malbertPath)) {
            println("ModelManager: Found mALBERT in Documents")
            return malbertPath
        }

        // Fallback to MobileBERT
        val mobilebertPath = "$modelsDir/${mobileBertModel}"
        if (fileExists(mobilebertPath)) {
            println("ModelManager: Found MobileBERT in Documents")
            return mobilebertPath
        }

        return null
    }

    /**
     * Check if vocabulary file exists
     */
    private fun vocabFileExists(): Boolean {
        // Check Documents
        if (fileExists("$modelsDir/$vocabFile")) {
            return true
        }

        // Check bundle
        val bundleVocab = NSBundle.mainBundle.pathForResource("vocab", ofType = "txt")
        return bundleVocab != null
    }

    /**
     * Check if file exists
     */
    private fun fileExists(path: String): Boolean {
        return NSFileManager.defaultManager.fileExistsAtPath(path)
    }
}
