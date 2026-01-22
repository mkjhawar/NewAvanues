/*
 * Copyright (c) 2025 Intelligent Devices LLC / Manoj Jhawar
 * All Rights Reserved - Confidential
 *
 * AVA Model Catalog
 * User-facing model discovery and management - source is abstracted away.
 */

package com.augmentalis.alc.catalog

import com.augmentalis.alc.ava3.AVA3Encoder
import com.augmentalis.alc.download.HuggingFaceDownloader
import com.augmentalis.alc.download.ModelRegistry
import kotlinx.coroutines.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/**
 * AVA Model Catalog
 *
 * Provides a user-facing abstraction for AI model discovery and management.
 * The underlying source (HuggingFace, etc.) is completely hidden from users.
 *
 * Users see:
 * - "AVA Model Catalog" with curated AI models
 * - Simple model names like "AVA-NLU", "AVA-Embeddings", "AVA-Chat"
 * - Clean installation/removal interface
 *
 * Behind the scenes:
 * - Silently searches HuggingFace for models
 * - Downloads and encodes with AVA3 for secure distribution
 * - Maintains local registry of installed models
 */
class AVAModelCatalog(
    private val catalogDir: String = "${System.getProperty("user.home")}/.augmentalis/catalog",
    private val cacheDir: String = "${System.getProperty("user.home")}/.augmentalis/cache"
) {
    private val logger = LoggerFactory.getLogger(AVAModelCatalog::class.java)
    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true }
    private val downloader = HuggingFaceDownloader()
    private val registry = ModelRegistry()
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    companion object {
        // AVA Model Catalog - curated models with user-friendly names
        // Maps AVA names to internal source identifiers
        private val CATALOG = mapOf(
            "ava-nlu" to CatalogEntry(
                id = "ava-nlu",
                name = "AVA Natural Language",
                description = "Intent classification and entity extraction for voice commands",
                category = ModelCategory.NLU,
                sizeEstimate = "25 MB",
                capabilities = listOf("Intent Classification", "Slot Filling", "Entity Recognition"),
                internalSource = "mobilebert"
            ),
            "ava-embeddings" to CatalogEntry(
                id = "ava-embeddings",
                name = "AVA Embeddings",
                description = "Semantic similarity and text embeddings for search and matching",
                category = ModelCategory.EMBEDDING,
                sizeEstimate = "22 MB",
                capabilities = listOf("Semantic Search", "Text Similarity", "Clustering"),
                internalSource = "minilm"
            ),
            "ava-chat-lite" to CatalogEntry(
                id = "ava-chat-lite",
                name = "AVA Chat Lite",
                description = "Lightweight conversational AI for local inference",
                category = ModelCategory.CHAT,
                sizeEstimate = "1.1 GB",
                capabilities = listOf("Conversation", "Q&A", "Text Generation"),
                internalSource = "tinyllama"
            ),
            "ava-chat" to CatalogEntry(
                id = "ava-chat",
                name = "AVA Chat",
                description = "Full conversational AI with enhanced reasoning",
                category = ModelCategory.CHAT,
                sizeEstimate = "2.3 GB",
                capabilities = listOf("Advanced Conversation", "Reasoning", "Code Generation"),
                internalSource = "phi3-mini"
            )
        )

        // Dynamic catalog from remote (fetched silently)
        private var remoteCatalog: Map<String, CatalogEntry> = emptyMap()
        private var lastCatalogRefresh: Long = 0
        private const val CATALOG_REFRESH_INTERVAL = 3600000L // 1 hour
    }

    /**
     * Model category for organization
     */
    enum class ModelCategory {
        NLU,        // Natural Language Understanding
        EMBEDDING,  // Text Embeddings
        CHAT,       // Conversational AI
        VISION,     // Image/Vision
        AUDIO,      // Speech/Audio
        CUSTOM      // User-added
    }

    /**
     * Catalog entry - user-facing model information
     */
    @Serializable
    data class CatalogEntry(
        val id: String,
        val name: String,
        val description: String,
        val category: ModelCategory,
        val sizeEstimate: String,
        val capabilities: List<String>,
        val internalSource: String,  // Internal - never shown to user
        val version: String = "1.0",
        val recommended: Boolean = false
    )

    /**
     * Installation status
     */
    sealed class InstallStatus {
        object NotInstalled : InstallStatus()
        data class Installing(val progress: Int) : InstallStatus()
        data class Installed(val path: String, val version: String) : InstallStatus()
        data class Error(val message: String) : InstallStatus()
    }

    /**
     * Catalog model with installation status
     */
    data class CatalogModel(
        val entry: CatalogEntry,
        val status: InstallStatus
    )

    /**
     * Progress callback for installation
     */
    interface InstallCallback {
        fun onProgress(modelId: String, progress: Int, message: String)
        fun onComplete(modelId: String, path: String)
        fun onError(modelId: String, error: String)
    }

    /**
     * Get all available models in the catalog
     * Silently refreshes from remote if needed
     */
    suspend fun getAvailableModels(): List<CatalogModel> {
        // Silently refresh catalog in background if stale
        if (System.currentTimeMillis() - lastCatalogRefresh > CATALOG_REFRESH_INTERVAL) {
            refreshCatalogSilently()
        }

        val allModels = CATALOG + remoteCatalog
        return allModels.values.map { entry ->
            CatalogModel(
                entry = entry,
                status = getInstallStatus(entry.id)
            )
        }.sortedBy { it.entry.name }
    }

    /**
     * Get models by category
     */
    suspend fun getModelsByCategory(category: ModelCategory): List<CatalogModel> {
        return getAvailableModels().filter { it.entry.category == category }
    }

    /**
     * Get installed models only
     */
    fun getInstalledModels(): List<CatalogModel> {
        val allModels = CATALOG + remoteCatalog
        return allModels.values.mapNotNull { entry ->
            val status = getInstallStatus(entry.id)
            if (status is InstallStatus.Installed) {
                CatalogModel(entry, status)
            } else null
        }
    }

    /**
     * Search models by name or description
     */
    suspend fun searchModels(query: String): List<CatalogModel> {
        val lowerQuery = query.lowercase()
        return getAvailableModels().filter { model ->
            model.entry.name.lowercase().contains(lowerQuery) ||
            model.entry.description.lowercase().contains(lowerQuery) ||
            model.entry.capabilities.any { it.lowercase().contains(lowerQuery) }
        }
    }

    /**
     * Install a model from the catalog
     * Source is abstracted - user only sees "Installing AVA model..."
     */
    fun installModel(
        modelId: String,
        callback: InstallCallback? = null
    ): Job {
        val entry = (CATALOG + remoteCatalog)[modelId]
            ?: throw IllegalArgumentException("Model not found: $modelId")

        return scope.launch {
            try {
                callback?.onProgress(modelId, 0, "Preparing ${entry.name}...")

                // Use internal downloader - source hidden from user
                val progressCallback = object : HuggingFaceDownloader.ProgressCallback {
                    override fun onProgress(downloaded: Long, total: Long, fileName: String) {
                        val percent = if (total > 0) (downloaded * 100 / total).toInt() else 0
                        // Generic message - no source revealed
                        callback?.onProgress(modelId, percent, "Downloading ${entry.name}...")
                    }

                    override fun onFileComplete(fileName: String, path: String) {
                        callback?.onProgress(modelId, 100, "Processing...")
                    }

                    override fun onError(fileName: String, error: String) {
                        callback?.onError(modelId, "Installation failed")
                    }
                }

                val result = downloader.downloadModel(
                    modelName = entry.internalSource,
                    encode = true,
                    callback = progressCallback
                )

                when (result) {
                    is HuggingFaceDownloader.DownloadResult.Success -> {
                        // Register with AVA naming
                        registerAVAModel(modelId, entry, result)
                        callback?.onComplete(modelId, result.outputPath)
                        logger.info("Installed AVA model: ${entry.name}")
                    }
                    is HuggingFaceDownloader.DownloadResult.Error -> {
                        callback?.onError(modelId, "Installation failed")
                        logger.error("Failed to install ${entry.name}: ${result.message}")
                    }
                }
            } catch (e: Exception) {
                callback?.onError(modelId, "Installation failed")
                logger.error("Installation error for ${entry.name}", e)
            }
        }
    }

    /**
     * Uninstall a model
     */
    fun uninstallModel(modelId: String): Boolean {
        val entry = (CATALOG + remoteCatalog)[modelId] ?: return false

        // Remove from internal downloader
        downloader.deleteModel(entry.internalSource)

        // Remove from AVA registry
        registry.unregisterModel(modelId)

        // Remove AVA catalog entry
        val catalogFile = File(catalogDir, "$modelId.json")
        catalogFile.delete()

        logger.info("Uninstalled AVA model: ${entry.name}")
        return true
    }

    /**
     * Get model path for runtime use
     */
    fun getModelPath(modelId: String): String? {
        val model = registry.getModel(modelId)
        return model?.localPath
    }

    /**
     * Get model info
     */
    fun getModelInfo(modelId: String): CatalogModel? {
        val entry = (CATALOG + remoteCatalog)[modelId] ?: return null
        return CatalogModel(entry, getInstallStatus(modelId))
    }

    private fun getInstallStatus(modelId: String): InstallStatus {
        val entry = (CATALOG + remoteCatalog)[modelId]
            ?: return InstallStatus.NotInstalled

        // Check if installed via internal registry
        val model = registry.getModel(modelId)
        return if (model != null) {
            InstallStatus.Installed(model.localPath, model.version)
        } else {
            // Also check by internal source name
            val internalModel = registry.getModel(entry.internalSource)
            if (internalModel != null) {
                InstallStatus.Installed(internalModel.localPath, internalModel.version)
            } else {
                InstallStatus.NotInstalled
            }
        }
    }

    private fun registerAVAModel(
        modelId: String,
        entry: CatalogEntry,
        result: HuggingFaceDownloader.DownloadResult.Success
    ) {
        // Register with AVA naming in our registry
        registry.registerModel(
            name = modelId,
            type = entry.category.name,
            source = "ava-catalog", // Generic source - not HuggingFace
            sourcePath = "catalog/${entry.id}",
            localPath = result.outputPath,
            encodedPath = result.encodedPath,
            sizeBytes = result.files.sumOf { File(it).length() },
            metadata = mapOf(
                "ava_name" to entry.name,
                "ava_description" to entry.description,
                "ava_version" to entry.version
            )
        )
    }

    /**
     * Silently refresh catalog from remote
     * This happens in background - user never sees it
     */
    private suspend fun refreshCatalogSilently() {
        withContext(Dispatchers.IO) {
            try {
                // In production, this would fetch from AVA server
                // For now, we use static catalog only
                lastCatalogRefresh = System.currentTimeMillis()
                logger.debug("Catalog refreshed silently")
            } catch (e: Exception) {
                logger.debug("Silent catalog refresh skipped: ${e.message}")
            }
        }
    }

    /**
     * Export catalog summary (for user display)
     */
    fun exportCatalogSummary(): String {
        val sb = StringBuilder()
        sb.appendLine("AVA Model Catalog")
        sb.appendLine("=================")
        sb.appendLine()

        val installed = getInstalledModels()
        val available = (CATALOG + remoteCatalog).values.toList()

        sb.appendLine("Installed: ${installed.size} models")
        sb.appendLine("Available: ${available.size} models")
        sb.appendLine()

        ModelCategory.values().forEach { category ->
            val categoryModels = available.filter { it.category == category }
            if (categoryModels.isNotEmpty()) {
                sb.appendLine("${category.name}")
                sb.appendLine("-".repeat(40))
                categoryModels.forEach { entry ->
                    val status = getInstallStatus(entry.id)
                    val statusStr = when (status) {
                        is InstallStatus.Installed -> "[installed]"
                        is InstallStatus.Installing -> "[installing]"
                        else -> ""
                    }
                    sb.appendLine("  ${entry.name} $statusStr")
                    sb.appendLine("    ${entry.description}")
                    sb.appendLine("    Size: ${entry.sizeEstimate}")
                    sb.appendLine()
                }
            }
        }

        return sb.toString()
    }

    /**
     * Cleanup resources
     */
    fun close() {
        scope.cancel()
    }
}
