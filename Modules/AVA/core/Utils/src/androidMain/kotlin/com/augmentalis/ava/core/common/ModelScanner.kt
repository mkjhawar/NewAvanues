/*
 * Copyright (c) 2024-2025 Intelligent Devices LLC / Augmentalis
 * All Rights Reserved.
 *
 * This file is part of AVA AI and is proprietary software.
 * See LICENSE file in the project root for license information.
 */

package com.augmentalis.ava.core.common

import java.io.File

/**
 * Model Scanner for AVA AI Models Repository
 *
 * Scans /sdcard/ava-ai-models/ to discover available models.
 * This utility helps the app dynamically detect which models are installed
 * without hardcoding paths or model lists.
 *
 * Repository Structure:
 * ```
 * /sdcard/ava-ai-models/
 * ├── embeddings/              # ONNX embedding models
 * │   └── AVA-384-Base-INT8.AON
 * └── llm/                     # Large Language Models
 *     ├── AVA-GE2-2B16/
 *     └── AVA-GE3-4B16/
 * ```
 *
 * Created: 2025-11-20
 * Author: AVA AI Team
 */
object ModelScanner {

    private const val BASE_PATH = "/sdcard/ava-ai-models"

    /**
     * Model information
     */
    data class ModelInfo(
        val id: String,              // e.g., "AVA-GE3-4B16"
        val type: ModelType,         // LLM or Embedding
        val path: String,            // Absolute path
        val size: Long,              // Total size in bytes
        val hasConfig: Boolean = false,  // Has ava-model-config.json
        val hasWeights: Boolean = false  // Has params_shard files
    )

    enum class ModelType {
        LLM,          // Large Language Model
        EMBEDDING     // Embedding model (ONNX)
    }

    /**
     * Scan for all available models
     *
     * @return List of discovered models
     */
    fun scanAllModels(): List<ModelInfo> {
        val models = mutableListOf<ModelInfo>()
        models.addAll(scanEmbeddings())
        models.addAll(scanLLMs())
        return models
    }

    /**
     * Scan embedding models
     */
    fun scanEmbeddings(): List<ModelInfo> {
        val embeddingsDir = File(BASE_PATH, "embeddings")
        if (!embeddingsDir.exists() || !embeddingsDir.isDirectory) {
            return emptyList()
        }

        return embeddingsDir.listFiles { file ->
            file.isFile && file.extension.equals("AON", ignoreCase = true)
        }?.map { file ->
            ModelInfo(
                id = file.nameWithoutExtension,
                type = ModelType.EMBEDDING,
                path = file.absolutePath,
                size = file.length()
            )
        } ?: emptyList()
    }

    /**
     * Scan LLM models
     */
    fun scanLLMs(): List<ModelInfo> {
        val llmDir = File(BASE_PATH, "llm")
        if (!llmDir.exists() || !llmDir.isDirectory) {
            return emptyList()
        }

        return llmDir.listFiles { file ->
            file.isDirectory && file.name.startsWith("AVA-")
        }?.mapNotNull { modelDir ->
            val hasConfig = File(modelDir, "ava-model-config.json").exists()
            val hasWeights = modelDir.listFiles { f ->
                f.name.startsWith("params_shard_") && f.extension == "bin"
            }?.isNotEmpty() ?: false

            ModelInfo(
                id = modelDir.name,
                type = ModelType.LLM,
                path = modelDir.absolutePath,
                size = calculateDirectorySize(modelDir),
                hasConfig = hasConfig,
                hasWeights = hasWeights
            )
        } ?: emptyList()
    }

    /**
     * Check if a specific model exists
     */
    fun hasModel(modelId: String, type: ModelType): Boolean {
        return when (type) {
            ModelType.EMBEDDING -> {
                val file = File(BASE_PATH, "embeddings/$modelId.AON")
                file.exists()
            }
            ModelType.LLM -> {
                val dir = File(BASE_PATH, "llm/$modelId")
                dir.exists() && dir.isDirectory
            }
        }
    }

    /**
     * Get path to a specific model
     */
    fun getModelPath(modelId: String, type: ModelType): String? {
        return when (type) {
            ModelType.EMBEDDING -> {
                val file = File(BASE_PATH, "embeddings/$modelId.AON")
                if (file.exists()) file.absolutePath else null
            }
            ModelType.LLM -> {
                val dir = File(BASE_PATH, "llm/$modelId")
                if (dir.exists()) dir.absolutePath else null
            }
        }
    }

    /**
     * Get model configuration path (LLM only)
     */
    fun getModelConfigPath(modelId: String): String? {
        val configFile = File(BASE_PATH, "llm/$modelId/ava-model-config.json")
        return if (configFile.exists()) configFile.absolutePath else null
    }

    /**
     * Check if unified repository exists
     */
    fun isRepositoryAvailable(): Boolean {
        val baseDir = File(BASE_PATH)
        return baseDir.exists() && baseDir.isDirectory
    }

    /**
     * Get repository statistics
     */
    data class RepositoryStats(
        val totalModels: Int,
        val llmModels: Int,
        val embeddingModels: Int,
        val totalSize: Long
    )

    fun getRepositoryStats(): RepositoryStats {
        val allModels = scanAllModels()
        return RepositoryStats(
            totalModels = allModels.size,
            llmModels = allModels.count { it.type == ModelType.LLM },
            embeddingModels = allModels.count { it.type == ModelType.EMBEDDING },
            totalSize = allModels.sumOf { it.size }
        )
    }

    // Helper: Calculate directory size recursively
    private fun calculateDirectorySize(dir: File): Long {
        var size = 0L
        dir.listFiles()?.forEach { file ->
            size += if (file.isDirectory) {
                calculateDirectorySize(file)
            } else {
                file.length()
            }
        }
        return size
    }

    /**
     * Format size in human-readable format
     */
    fun formatSize(bytes: Long): String {
        val kb = bytes / 1024.0
        val mb = kb / 1024.0
        val gb = mb / 1024.0

        return when {
            gb >= 1.0 -> String.format("%.2f GB", gb)
            mb >= 1.0 -> String.format("%.2f MB", mb)
            kb >= 1.0 -> String.format("%.2f KB", kb)
            else -> "$bytes B"
        }
    }
}
