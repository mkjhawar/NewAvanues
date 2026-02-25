/**
 * macOS implementation of ModelManager for Core ML models
 *
 * Adapted from iOS implementation with macOS-specific changes:
 * - Uses NSApplicationSupportDirectory (~/Library/Application Support/) instead of
 *   NSDocumentDirectory (~/Documents/) — follows macOS app data conventions
 * - NSBundle.mainBundle works for .app bundles; for CLI tools, models must be
 *   placed in the Application Support directory
 * - macOS has more filesystem freedom (no sandbox by default unless App Store)
 */

package com.augmentalis.nlu

import com.augmentalis.ava.core.common.Result
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import platform.Foundation.*

private const val TAG = "ModelManager"

@OptIn(ExperimentalForeignApi::class)
actual class ModelManager {

    // Model file names
    private val mobileBertModel = "intent_classifier_mobilebert.mlpackage"
    private val malbertModel = "intent_classifier_malbert.mlpackage"
    private val vocabFile = "vocab.txt"

    // macOS file system paths — use Application Support for app data
    private val appSupportDir by lazy {
        NSSearchPathForDirectoriesInDomains(
            NSApplicationSupportDirectory,
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
        "$appSupportDir/com.augmentalis.nlu/models"
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
            nluLogError(TAG, "Failed to create models directory: ${e.message}", e)
        }
    }

    /**
     * Check if models are available locally
     * Checks both app bundle and Application Support directory
     */
    actual fun isModelAvailable(): Boolean {
        val bundlePath = getBundleModelPath()
        val appSupportPath = getAppSupportModelPath()

        return (bundlePath != null || appSupportPath != null) && vocabFileExists()
    }

    /**
     * Get path to available model
     * Priority: Application Support (user-downloaded) -> Bundle (bundled)
     */
    actual fun getModelPath(): String {
        // Try Application Support first (user-downloaded or updated models)
        getAppSupportModelPath()?.let { return it }

        // Fallback to bundled model
        getBundleModelPath()?.let { return it }

        nluLogWarn(TAG, "No model found")
        return ""
    }

    /**
     * Get vocabulary file path
     */
    actual fun getVocabPath(): String {
        // Check Application Support first
        val appVocab = "$modelsDir/$vocabFile"
        if (fileExists(appVocab)) {
            return appVocab
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
     * On macOS, models are typically bundled or downloaded to Application Support
     *
     * @param onProgress Callback for progress (0.0 to 1.0)
     */
    actual suspend fun downloadModelsIfNeeded(
        onProgress: (Float) -> Unit
    ): Result<Unit> = withContext(Dispatchers.Default) {
        try {
            if (isModelAvailable()) {
                onProgress(1.0f)
                return@withContext Result.Success(Unit)
            }

            nluLogWarn(TAG, "Models not available and no download configured")
            return@withContext Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(
                exception = e,
                message = "Failed to check model availability: ${e.message}"
            )
        }
    }

    /**
     * Copy model from app bundle to Application Support for easier access
     */
    actual suspend fun copyModelFromAssets(): Result<Unit> = withContext(Dispatchers.Default) {
        try {
            nluLogInfo(TAG, "Copying model from bundle...")

            val fileManager = NSFileManager.defaultManager

            val bundleModel = getBundleModelPath()
            if (bundleModel != null && !fileExists("$modelsDir/${mobileBertModel}")) {
                val sourceUrl = NSURL.fileURLWithPath(bundleModel)
                val destUrl = NSURL.fileURLWithPath("$modelsDir/${mobileBertModel}")

                try {
                    fileManager.copyItemAtURL(sourceUrl, toURL = destUrl, error = null)
                    nluLogInfo(TAG, "Model copied successfully")
                } catch (e: Exception) {
                    nluLogError(TAG, "Failed to copy model: ${e.message}", e)
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

            try {
                fileManager.removeItemAtPath(modelsDir, error = null)
                nluLogInfo(TAG, "Models cleared")
            } catch (e: Exception) {
                nluLogError(TAG, "Failed to clear models: ${e.message}", e)
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
            nluLogDebug(TAG, "Found mALBERT in bundle")
            return it
        }

        // Fallback to MobileBERT
        bundle.pathForResource("intent_classifier_mobilebert.mlpackage", ofType = null)?.let {
            nluLogDebug(TAG, "Found MobileBERT in bundle")
            return it
        }

        // Try without extension (for raw model files)
        bundle.pathForResource("intent_classifier", ofType = "mlmodel")?.let {
            nluLogDebug(TAG, "Found .mlmodel in bundle")
            return it
        }

        return null
    }

    /**
     * Get model path from Application Support directory
     */
    private fun getAppSupportModelPath(): String? {
        // Try mALBERT first
        val malbertPath = "$modelsDir/${malbertModel}"
        if (fileExists(malbertPath)) {
            nluLogDebug(TAG, "Found mALBERT in Application Support")
            return malbertPath
        }

        // Fallback to MobileBERT
        val mobilebertPath = "$modelsDir/${mobileBertModel}"
        if (fileExists(mobilebertPath)) {
            nluLogDebug(TAG, "Found MobileBERT in Application Support")
            return mobilebertPath
        }

        return null
    }

    /**
     * Check if vocabulary file exists
     */
    private fun vocabFileExists(): Boolean {
        if (fileExists("$modelsDir/$vocabFile")) {
            return true
        }

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
