// filename: features/nlu/src/jsMain/kotlin/com/augmentalis/ava/features/nlu/ModelManager.kt
// created: 2025-11-02
// author: Manoj Jhawar
// © Augmentalis Inc, Intelligent Devices LLC
// TCR: Phase 1 - KMP Migration (JS/Web stub)

package com.augmentalis.nlu

import com.augmentalis.ava.core.common.Result

/**
 * JS/Web stub implementation of ModelManager
 *
 * TODO Phase 2: Implement model loading from CDN or IndexedDB
 */
actual class ModelManager {

    actual fun isModelAvailable(): Boolean = false

    actual fun getModelPath(): String = ""

    actual fun getVocabPath(): String = ""

    actual suspend fun downloadModelsIfNeeded(
        onProgress: (Float) -> Unit
    ): Result<Unit> {
        return Result.Error(
            exception = NotImplementedError("Web model management not yet implemented"),
            message = "Model download not available on Web yet"
        )
    }

    actual suspend fun copyModelFromAssets(): Result<Unit> {
        return Result.Error(
            exception = NotImplementedError("Web model management not yet implemented"),
            message = "Model assets not available on Web yet"
        )
    }

    actual fun clearModels(): Result<Unit> = Result.Success(Unit)

    actual fun getModelsSize(): Long = 0L
}
