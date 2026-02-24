package com.augmentalis.nlu

import android.content.Context
import com.augmentalis.ava.core.common.Result
import com.augmentalis.ava.core.data.db.AVADatabase
import com.augmentalis.ava.core.data.db.DatabaseDriverFactory
import com.augmentalis.ava.core.data.db.createDatabase
import com.augmentalis.nlu.ava.AssetExtractor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Initializer for NLU module
 * Handles .ava file extraction, model download, and classifier setup
 *
 * Usage:
 * ```kotlin
 * val initializer = NLUInitializer(context)
 * initializer.initialize { progress ->
 *     println("Download progress: ${progress * 100}%")
 * }
 * ```
 */
class NLUInitializer(private val context: Context) {

    private val assetExtractor = AssetExtractor(context)
    private val modelManager = ModelManager(context)
    private val intentClassifier = IntentClassifier.getInstance(context)

    /**
     * Initialize NLU system
     *
     * Steps:
     * 0. Extract .ava files from assets (first launch only)
     * 1. Check if models are available
     * 2. Download models if needed
     * 3. Initialize ONNX classifier
     *
     * @param onProgress Callback for download progress (0.0 to 1.0)
     * @return Result indicating success or failure
     */
    suspend fun initialize(
        onProgress: (Float) -> Unit = {}
    ): Result<InitializationStatus> = withContext(Dispatchers.IO) {
        try {
            // Step 0: Extract .ava files (first launch only)
            val extracted = assetExtractor.extractIfNeeded()
            if (extracted) {
                nluLogInfo("NLUInitializer", ".ava files extracted to device storage")
            }

            // Step 1: Check model availability
            val modelsAvailable = modelManager.isModelAvailable()

            if (!modelsAvailable) {
                // Step 2: Try copying from APK assets first (faster, offline-friendly)
                onProgress(0.1f)
                val copyResult = modelManager.copyModelFromAssets()

                when (copyResult) {
                    is Result.Error -> {
                        // Fallback to download if assets not available
                        onProgress(0.2f)
                        val downloadResult = modelManager.downloadModelsIfNeeded(onProgress)

                        when (downloadResult) {
                            is Result.Error -> {
                                return@withContext Result.Error(
                                    exception = downloadResult.exception,
                                    message = "Failed to obtain models: ${downloadResult.message}. Asset copy also failed: ${copyResult.message}"
                                )
                            }
                            else -> {}
                        }
                    }
                    else -> {
                        onProgress(1.0f)
                    }
                }
            }

            // Step 3: Initialize classifier
            val modelPath = modelManager.getModelPath()
            val initResult = intentClassifier.initialize(modelPath)

            when (initResult) {
                is Result.Success -> {
                    // Step 4: Check model version and migrate if needed
                    val database = DatabaseDriverFactory(context).createDriver().createDatabase()
                    val metadataQueries = database.embeddingMetadataQueries

                    val versionStatusResult = modelManager.checkVersionStatus(metadataQueries)
                    val versionStatus = when (versionStatusResult) {
                        is Result.Success -> versionStatusResult.data
                        is Result.Error -> {
                            nluLogWarn("NLUInitializer", "Version check failed: ${versionStatusResult.message}")
                            VersionStatus.Current  // Default to current if check fails
                        }
                    }

                    when (versionStatus) {
                        is VersionStatus.NeedsMigration -> {
                            nluLogWarn("NLUInitializer", "Model version changed: ${versionStatus.reason}")
                            nluLogInfo("NLUInitializer", "From: ${versionStatus.fromModel} ${versionStatus.fromVersion}")
                            nluLogInfo("NLUInitializer", "To: ${versionStatus.toModel} ${versionStatus.toVersion}")
                            nluLogInfo("NLUInitializer", "Starting automatic migration...")

                            // Run migration with progress tracking
                            val migrator = EmbeddingMigrator(context, intentClassifier)
                            val currentModelVersion = modelManager.getCurrentModelVersion()

                            when (val migrationResult = migrator.migrateEmbeddings(currentModelVersion) { progress ->
                                nluLogDebug("NLUInitializer", "Migration progress: ${(progress * 100).toInt()}%")
                            }) {
                                is Result.Success -> {
                                    val stats = migrationResult.data
                                    nluLogInfo("NLUInitializer", "Migration complete!")
                                    nluLogInfo("NLUInitializer", "  Processed: ${stats.totalProcessed}")
                                    nluLogInfo("NLUInitializer", "  Successful: ${stats.successful}")
                                    nluLogInfo("NLUInitializer", "  Failed: ${stats.failed}")
                                    nluLogInfo("NLUInitializer", "  Duration: ${stats.duration}ms")
                                }
                                is Result.Error -> {
                                    nluLogError("NLUInitializer", "Migration failed: ${migrationResult.message}", migrationResult.exception)
                                    // Continue anyway - non-fatal
                                }
                            }
                        }

                        is VersionStatus.NewInstall -> {
                            nluLogInfo("NLUInitializer", "New install - will create embeddings on first ontology load")

                            // Save initial metadata
                            val currentModelVersion = modelManager.getCurrentModelVersion()
                            metadataQueries.insert(
                                model_name = currentModelVersion.name,
                                model_version = currentModelVersion.version,
                                embedding_dimension = currentModelVersion.dimension.toLong(),
                                model_checksum = currentModelVersion.checksum,
                                created_at = System.currentTimeMillis(),
                                is_active = true,
                                total_embeddings = 0
                            )
                        }

                        is VersionStatus.Current -> {
                            nluLogInfo("NLUInitializer", "Model version current - no migration needed")
                        }
                    }

                    // Step 5: Load AON ontologies in background (Phase 3 - Parallel Init)
                    // PERF-001 Strategy 6: Don't block app startup
                    // - UI shows immediately
                    // - Embeddings compute in background
                    // - First launch: ~10s background load
                    // - Subsequent launches: <1s (cached)
                    kotlinx.coroutines.CoroutineScope(Dispatchers.IO).launch {
                        loadAonOntologies()
                    }

                    Result.Success(
                        InitializationStatus(
                            isInitialized = true,
                            modelSize = modelManager.getModelsSize(),
                            message = "NLU initialized successfully"
                        )
                    )
                }
                is Result.Error -> {
                    Result.Error(
                        exception = initResult.exception,
                        message = "Classifier initialization failed: ${initResult.message}"
                    )
                }
            }
        } catch (e: Exception) {
            Result.Error(
                exception = e,
                message = "NLU initialization failed: ${e.message}"
            )
        }
    }

    /**
     * Check if NLU is ready to use
     */
    fun isInitialized(): Boolean {
        return modelManager.isModelAvailable()
    }

    /**
     * Get initialization status
     */
    fun getStatus(): InitializationStatus {
        val isAvailable = modelManager.isModelAvailable()
        val extractionStatus = assetExtractor.getExtractionStatus()

        return InitializationStatus(
            isInitialized = isAvailable,
            modelSize = if (isAvailable) modelManager.getModelsSize() else 0,
            message = if (isAvailable) "Ready" else "Not initialized",
            avaFilesExtracted = extractionStatus.extracted && extractionStatus.avaFilesExist
        )
    }

    /**
     * Clear downloaded models (for testing)
     */
    fun clearModels(): Result<Unit> {
        return modelManager.clearModels()
    }

    /**
     * Load AON (.aot) ontology files and populate intent embeddings cache
     *
     * This runs once on first app launch to:
     * 1. Parse all .aot files from assets/ontology/{locale}/
     * 2. Compute embeddings for each intent's semantic description
     * 3. Populate the intent_embeddings table in database
     *
     * Subsequent launches skip this step (embeddings already cached).
     *
     * Performance Impact:
     * - First launch: +5-10s (one-time embedding computation)
     * - Subsequent launches: <1s (loads from cache)
     * - Saves 41 seconds on every launch after first!
     */
    private suspend fun loadAonOntologies() {
        try {
            nluLogInfo("NLUInitializer", "=== AON Ontology Loading ===")

            val aonLoader = com.augmentalis.nlu.aon.AonLoader(
                context = context,
                intentClassifier = intentClassifier
            )

            // Load all ontologies (skips if already loaded)
            val result = aonLoader.loadAllOntologies(forceReload = false)

            when (result) {
                is Result.Success -> {
                    val stats = result.data
                    if (stats.skipped > 0) {
                        nluLogInfo("NLUInitializer", "AON ontologies already loaded (${stats.skipped} intents)")
                    } else {
                        nluLogInfo("NLUInitializer", "AON ontologies loaded successfully:")
                        nluLogInfo("NLUInitializer", "  Intents: ${stats.totalIntents}")
                        nluLogInfo("NLUInitializer", "  Files: ${stats.filesProcessed}")
                        nluLogInfo("NLUInitializer", "  Embeddings: ${stats.embeddingsCreated}")
                        nluLogInfo("NLUInitializer", "  Duration: ${stats.duration}ms")
                    }
                }
                is Result.Error -> {
                    nluLogWarn("NLUInitializer", "Failed to load AON ontologies: ${result.message}")
                    nluLogWarn("NLUInitializer", "Falling back to legacy intent loading")
                    // Non-fatal: app continues with legacy intent examples
                }
            }
        } catch (e: Exception) {
            nluLogError("NLUInitializer", "AON loading error (non-fatal)", e)
        }
    }
}

/**
 * Status of NLU initialization
 */
data class InitializationStatus(
    val isInitialized: Boolean,
    val modelSize: Long,
    val message: String,
    val avaFilesExtracted: Boolean = false
)
