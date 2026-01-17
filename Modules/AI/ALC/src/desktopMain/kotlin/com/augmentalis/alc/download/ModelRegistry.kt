/*
 * Copyright (c) 2025 Intelligent Devices LLC / Manoj Jhawar
 * All Rights Reserved - Confidential
 *
 * Model Registry for ALC
 * Tracks downloaded models, versions, and paths for the application.
 */

package com.augmentalis.alc.download

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory
import java.io.File
import java.time.Instant

/**
 * Model Registry
 *
 * Maintains a registry of downloaded models with metadata.
 * Supports both local file registry and optional repo sync.
 *
 * Registry location: ~/.augmentalis/models/registry.json
 */
class ModelRegistry(
    private val registryPath: String = "${System.getProperty("user.home")}/.augmentalis/models/registry.json"
) {
    private val logger = LoggerFactory.getLogger(ModelRegistry::class.java)
    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true }
    private var registry: Registry

    init {
        registry = loadRegistry()
    }

    /**
     * Registry data structure
     */
    @Serializable
    data class Registry(
        val version: Int = 1,
        val models: MutableMap<String, ModelEntry> = mutableMapOf(),
        var lastUpdated: String = Instant.now().toString()
    )

    /**
     * Model entry in registry
     */
    @Serializable
    data class ModelEntry(
        val name: String,
        val type: String,
        val source: String,
        val sourcePath: String,
        val localPath: String,
        val encodedPath: String? = null,
        val version: String = "1.0",
        val hash: String? = null,
        val sizeBytes: Long = 0,
        val downloadedAt: String = Instant.now().toString(),
        val metadata: Map<String, String> = emptyMap()
    )

    /**
     * Register a new model
     */
    fun registerModel(
        name: String,
        type: String,
        source: String,
        sourcePath: String,
        localPath: String,
        encodedPath: String? = null,
        version: String = "1.0",
        hash: String? = null,
        sizeBytes: Long = 0,
        metadata: Map<String, String> = emptyMap()
    ): ModelEntry {
        val entry = ModelEntry(
            name = name,
            type = type,
            source = source,
            sourcePath = sourcePath,
            localPath = localPath,
            encodedPath = encodedPath,
            version = version,
            hash = hash,
            sizeBytes = sizeBytes,
            metadata = metadata
        )

        registry.models[name] = entry
        registry.lastUpdated = Instant.now().toString()
        saveRegistry()

        logger.info("Registered model: $name at $localPath")
        return entry
    }

    /**
     * Get a model entry
     */
    fun getModel(name: String): ModelEntry? {
        return registry.models[name]
    }

    /**
     * Check if a model is registered
     */
    fun hasModel(name: String): Boolean {
        return registry.models.containsKey(name)
    }

    /**
     * List all registered models
     */
    fun listModels(): List<ModelEntry> {
        return registry.models.values.toList()
    }

    /**
     * List models by type
     */
    fun listModelsByType(type: String): List<ModelEntry> {
        return registry.models.values.filter { it.type == type }
    }

    /**
     * Remove a model from registry
     */
    fun unregisterModel(name: String): Boolean {
        val removed = registry.models.remove(name)
        if (removed != null) {
            registry.lastUpdated = Instant.now().toString()
            saveRegistry()
            logger.info("Unregistered model: $name")
            return true
        }
        return false
    }

    /**
     * Update model metadata
     */
    fun updateModel(name: String, updates: Map<String, String>): ModelEntry? {
        val existing = registry.models[name] ?: return null

        val updated = existing.copy(
            metadata = existing.metadata + updates
        )
        registry.models[name] = updated
        registry.lastUpdated = Instant.now().toString()
        saveRegistry()

        return updated
    }

    /**
     * Get registry statistics
     */
    fun getStats(): RegistryStats {
        val totalSize = registry.models.values.sumOf { it.sizeBytes }
        val byType = registry.models.values.groupBy { it.type }
            .mapValues { it.value.size }

        return RegistryStats(
            totalModels = registry.models.size,
            totalSizeBytes = totalSize,
            modelsByType = byType,
            lastUpdated = registry.lastUpdated
        )
    }

    /**
     * Registry statistics
     */
    data class RegistryStats(
        val totalModels: Int,
        val totalSizeBytes: Long,
        val modelsByType: Map<String, Int>,
        val lastUpdated: String
    )

    /**
     * Export registry to markdown for documentation
     */
    fun exportToMarkdown(): String {
        val sb = StringBuilder()
        sb.appendLine("# Model Registry")
        sb.appendLine()
        sb.appendLine("Last updated: ${registry.lastUpdated}")
        sb.appendLine()
        sb.appendLine("## Models")
        sb.appendLine()
        sb.appendLine("| Name | Type | Version | Size | Source |")
        sb.appendLine("|------|------|---------|------|--------|")

        for (model in registry.models.values.sortedBy { it.name }) {
            val sizeMb = "%.2f MB".format(model.sizeBytes / (1024.0 * 1024.0))
            sb.appendLine("| ${model.name} | ${model.type} | ${model.version} | $sizeMb | ${model.source} |")
        }

        return sb.toString()
    }

    /**
     * Sync registry with repo (if configured)
     * This updates the repo's model registry file
     */
    fun syncToRepo(repoPath: String): Boolean {
        return try {
            val repoRegistryPath = File(repoPath, "docs/models/MODEL-REGISTRY.md")
            repoRegistryPath.parentFile?.mkdirs()
            repoRegistryPath.writeText(exportToMarkdown())
            logger.info("Synced registry to $repoRegistryPath")
            true
        } catch (e: Exception) {
            logger.error("Failed to sync registry to repo", e)
            false
        }
    }

    private fun loadRegistry(): Registry {
        val file = File(registryPath)
        return if (file.exists()) {
            try {
                json.decodeFromString<Registry>(file.readText())
            } catch (e: Exception) {
                logger.warn("Failed to load registry, creating new", e)
                Registry()
            }
        } else {
            Registry()
        }
    }

    private fun saveRegistry() {
        val file = File(registryPath)
        file.parentFile?.mkdirs()
        file.writeText(json.encodeToString(registry))
        logger.debug("Saved registry to $registryPath")
    }

    /**
     * Reload registry from disk
     */
    fun reload() {
        registry = loadRegistry()
    }

    /**
     * Clear all entries (use with caution)
     */
    fun clear() {
        registry.models.clear()
        registry.lastUpdated = Instant.now().toString()
        saveRegistry()
        logger.warn("Cleared all registry entries")
    }
}
