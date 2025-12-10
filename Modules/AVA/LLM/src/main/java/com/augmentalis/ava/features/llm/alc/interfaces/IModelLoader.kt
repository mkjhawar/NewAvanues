/**
 * Model Loader Interface
 *
 * Single Responsibility: Load and unload ML models
 *
 * Implementations:
 * - TVMModelLoader: Load models via TVM runtime
 * - CachedModelLoader: Load models with caching layer
 * - MockModelLoader: For testing
 *
 * Created: 2025-10-31
 */

package com.augmentalis.ava.features.llm.alc.interfaces

import com.augmentalis.ava.features.llm.alc.models.ModelConfig
import com.augmentalis.ava.features.llm.alc.models.LoadedModel

/**
 * Interface for loading ML models from various sources
 */
interface IModelLoader {
    /**
     * Load a model from storage
     *
     * @param config Model configuration (path, device, format)
     * @return Loaded model instance
     * @throws ModelLoadException if loading fails
     */
    suspend fun loadModel(config: ModelConfig): LoadedModel

    /**
     * Unload the currently loaded model
     *
     * Releases all resources (memory, GPU, file handles)
     */
    suspend fun unloadModel()

    /**
     * Check if a model is currently loaded
     *
     * @return true if a model is loaded and ready for inference
     */
    fun isModelLoaded(): Boolean

    /**
     * Get information about the currently loaded model
     *
     * @return Model metadata, or null if no model is loaded
     */
    fun getModelInfo(): ModelConfig?
}
