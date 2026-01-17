/**
 * TVM Model Loader
 *
 * Single Responsibility: Load ML models via TVM runtime
 *
 * Handles model loading, validation, and resource management
 * for TVM-based models (MLC LLM format).
 *
 * Created: 2025-10-31
 */

package com.augmentalis.llm.alc.loader

import android.content.Context
import com.augmentalis.llm.alc.TVMRuntime
import com.augmentalis.llm.alc.TVMModule
import com.augmentalis.llm.alc.interfaces.IModelLoader
import com.augmentalis.llm.alc.models.LoadedModel
import com.augmentalis.llm.alc.models.ModelConfig
import com.augmentalis.llm.alc.models.ModelLoadException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import timber.log.Timber
import java.io.File
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Model loader for TVM runtime
 */
class TVMModelLoader(
    private val context: Context
) : IModelLoader {

    private val mutex = Mutex()
    private var runtime: TVMRuntime? = null
    private var loadedModel: LoadedModel? = null
    private val downloader = HuggingFaceModelDownloader(context)
    private val almExtractor = ALMExtractor(context)
    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun loadModel(config: ModelConfig): LoadedModel = mutex.withLock {
        try {
            Timber.d("Loading model: ${config.modelName}")

            // Step 1: Check for and extract any model archives (.amm, .amg, .amr)
            Timber.d("Checking for model archives to extract...")
            almExtractor.extractAllALMFiles()

            // Step 2: Check if model is downloaded, download if needed
            if (!downloader.isModelDownloaded(config.modelName)) {
                Timber.i("Model not found locally, downloading: ${config.modelName}")

                val downloadConfig = ModelDownloadConfig(
                    modelId = config.modelName,
                    modelUrl = config.modelPath // modelPath should be HuggingFace URL
                )

                // Download with progress tracking (collect first item to ensure download starts)
                downloader.downloadModel(downloadConfig).first()

                Timber.i("Model download initiated: ${config.modelName}")
            }

            // Step 3: Get local model path (may be from extracted .amm archive)
            val localModelPath = downloader.getModelPath(config.modelName)

            // Load model metadata
            val metadata = loadModelMetadata(localModelPath)
            val vocabSize = metadata["vocab_size"]?.toIntOrNull() ?: 32000

            // Initialize TVM runtime if needed
            if (runtime == null) {
                runtime = TVMRuntime.create(context, config.deviceType)
            }

            // Load model via TVM
            val runtimeInstance = runtime ?: throw ModelLoadException("TVM runtime not initialized")
            val tvmModule = runtimeInstance.loadModule(
                modelPath = localModelPath,
                modelLib = config.modelName,
                deviceOverride = config.deviceType
            )

            // Create loaded model wrapper
            val model = LoadedModel(
                config = config,
                handle = tvmModule,
                vocabSize = vocabSize,
                metadata = metadata + mapOf(
                    "device" to config.deviceType,
                    "context_length" to config.contextLength,
                    "model_path" to localModelPath
                )
            )

            loadedModel = model
            Timber.i("Model loaded successfully: ${config.modelName}")
            return@withLock model

        } catch (e: Exception) {
            Timber.e(e, "Failed to load model: ${config.modelName}")
            throw ModelLoadException("Failed to load model: ${e.message}", e)
        }
    }

    /**
     * Load model metadata from config.json
     */
    private fun loadModelMetadata(modelPath: String): Map<String, String> {
        return try {
            val configFile = File(modelPath, "config.json")
            if (!configFile.exists()) {
                Timber.w("config.json not found, using defaults")
                return emptyMap()
            }

            val configText = configFile.readText()
            val jsonElement = json.parseToJsonElement(configText).jsonObject

            mapOf(
                "vocab_size" to (jsonElement["vocab_size"]?.jsonPrimitive?.content ?: "32000"),
                "hidden_size" to (jsonElement["hidden_size"]?.jsonPrimitive?.content ?: "unknown"),
                "num_layers" to (jsonElement["num_hidden_layers"]?.jsonPrimitive?.content ?: "unknown"),
                "model_type" to (jsonElement["model_type"]?.jsonPrimitive?.content ?: "unknown")
            )
        } catch (e: Exception) {
            Timber.e(e, "Failed to load model metadata")
            emptyMap()
        }
    }


    override suspend fun unloadModel() = mutex.withLock {
        try {
            Timber.d("Unloading model")

            loadedModel?.let { model ->
                (model.handle as? TVMModule)?.dispose()
            }

            loadedModel = null
            runtime?.dispose()
            runtime = null

            Timber.i("Model unloaded successfully")
        } catch (e: Exception) {
            Timber.e(e, "Error unloading model")
        }
    }

    override fun isModelLoaded(): Boolean {
        return loadedModel != null
    }

    override fun getModelInfo(): ModelConfig? {
        return loadedModel?.config
    }
}
