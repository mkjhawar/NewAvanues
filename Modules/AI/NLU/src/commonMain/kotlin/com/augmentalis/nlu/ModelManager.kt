// filename: features/nlu/src/commonMain/kotlin/com/augmentalis/ava/features/nlu/ModelManager.kt
// created: 2025-11-02
// author: Manoj Jhawar
// © Augmentalis Inc, Intelligent Devices LLC
// TCR: Phase 1 - KMP Migration

package com.augmentalis.nlu

import com.augmentalis.ava.core.common.Result

/**
 * Cross-platform model download and management
 *
 * Handles:
 * - Model download from Hugging Face or other sources
 * - Progress tracking
 * - Local caching
 * - Platform-specific file system operations
 */
expect class ModelManager {
    /**
     * Check if models are available locally
     */
    fun isModelAvailable(): Boolean

    /**
     * Get model file path (platform-specific)
     */
    fun getModelPath(): String

    /**
     * Get vocabulary file path (platform-specific)
     */
    fun getVocabPath(): String

    /**
     * Download models if not available
     * @param onProgress Callback for download progress (0.0 to 1.0)
     * @return Result indicating success or failure
     */
    suspend fun downloadModelsIfNeeded(
        onProgress: (Float) -> Unit = {}
    ): Result<Unit>

    /**
     * Copy model from bundled assets (fallback)
     */
    suspend fun copyModelFromAssets(): Result<Unit>

    /**
     * Delete downloaded models (for cache clearing)
     */
    fun clearModels(): Result<Unit>

    /**
     * Get total size of downloaded models in bytes
     */
    fun getModelsSize(): Long
}
