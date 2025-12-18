// filename: Universal/AVA/Features/RAG/src/commonMain/kotlin/com/augmentalis/ava/features/rag/domain/RAGConfig.kt
// created: 2025-11-04
// author: AVA AI Team
// © Augmentalis Inc, Intelligent Devices LLC

package com.augmentalis.rag.domain

import kotlinx.serialization.Serializable

/**
 * Configuration for the RAG system
 *
 * This controls embedding models, processing location, storage limits,
 * and power optimization settings.
 */
@Serializable
data class RAGConfig(
    val embeddingConfig: EmbeddingConfig = EmbeddingConfig(),
    val storageConfig: StorageConfig = StorageConfig(),
    val processingConfig: ProcessingConfig = ProcessingConfig(),
    val powerConfig: PowerConfig = PowerConfig(),
    val chunkingConfig: ChunkingConfig = ChunkingConfig()
)

/**
 * Embedding model configuration
 */
@Serializable
data class EmbeddingConfig(
    val preferredProvider: EmbeddingProvider = EmbeddingProvider.ONNX,
    val fallbackProviders: List<EmbeddingProvider> = listOf(
        EmbeddingProvider.LOCAL_LLM,
        EmbeddingProvider.CLOUD_API
    ),
    val modelName: String = "all-MiniLM-L6-v2",
    val dimension: Int = 384,
    val batchSize: Int = 32,
    val quantize: Boolean = true  // Use int8 quantization for 75% space savings
)

/**
 * Embedding provider options
 */
@Serializable
enum class EmbeddingProvider {
    /** ONNX Runtime (on-device, fast) */
    ONNX,

    /** Local LLM (already loaded for chat) */
    LOCAL_LLM,

    /** Cloud API (fallback, requires internet) */
    CLOUD_API
}

/**
 * Storage configuration
 */
@Serializable
data class StorageConfig(
    val tier: StorageTier = StorageTier.MOBILE,
    val maxChunks: Int = 200_000,
    val enableClustering: Boolean = true,
    val clusterCount: Int = 256,
    val cacheSize: Int = 1000,
    val useMemoryMapping: Boolean = true
)

/**
 * Storage tier determines capacity and features
 */
@Serializable
enum class StorageTier {
    /** Mobile: SQLite-vec, 200k chunks, quantized */
    MOBILE,

    /** Desktop: SQLite-vec, 1M chunks, quantized + full precision */
    DESKTOP,

    /** Cloud: ChromaDB, unlimited, full precision */
    CLOUD
}

/**
 * Processing configuration
 */
@Serializable
data class ProcessingConfig(
    val defaultLocation: ProcessingLocation = ProcessingLocation.ON_DEVICE,
    val autoOffloadToDesktop: Boolean = true,
    val autoOffloadToCloud: Boolean = false,
    val maxConcurrentProcessing: Int = 2,
    val processingPriority: ProcessingPriority = ProcessingPriority.NORMAL
)

/**
 * Where document processing happens
 */
@Serializable
enum class ProcessingLocation {
    /** On-device (mobile/desktop) */
    ON_DEVICE,

    /** Connected desktop via LAN */
    DESKTOP,

    /** Cloud service */
    CLOUD,

    /** Automatic: desktop if available, otherwise on-device */
    HYBRID
}

/**
 * Processing priority affects CPU/battery usage
 */
@Serializable
enum class ProcessingPriority {
    /** Low priority, background only, minimal battery */
    LOW,

    /** Normal priority, opportunistic processing */
    NORMAL,

    /** High priority, process immediately */
    HIGH
}

/**
 * Power optimization configuration
 */
@Serializable
data class PowerConfig(
    val mode: PowerMode = PowerMode.AUTO,
    val enableBackgroundProcessing: Boolean = true,
    val processOnlyWhileCharging: Boolean = false,
    val maxBatteryImpact: BatteryImpact = BatteryImpact.LOW
)

/**
 * Power mode for RAG operations
 */
@Serializable
enum class PowerMode {
    /** Automatic detection based on context */
    AUTO,

    /** Field mode: zero background processing, instant search only */
    FIELD,

    /** Office mode: normal processing, balanced */
    OFFICE,

    /** Charging mode: aggressive processing, no limits */
    CHARGING
}

/**
 * Target battery impact level
 */
@Serializable
enum class BatteryImpact {
    /** <5mA average (field safe) */
    ZERO,

    /** <10mA average (minimal) */
    LOW,

    /** <25mA average (moderate) */
    MEDIUM,

    /** >25mA average (performance) */
    HIGH
}

/**
 * Update strategy configuration
 */
@Serializable
data class UpdateConfig(
    val enableAutoDetection: Boolean = true,
    val promptUserOnChanges: Boolean = true,
    val autoUpdateSchedule: UpdateSchedule? = null,
    val useIncrementalUpdates: Boolean = true
)

/**
 * Scheduled update configuration
 */
@Serializable
data class UpdateSchedule(
    val frequency: UpdateFrequency,
    val preferredTime: String = "02:00",  // 24-hour format
    val onlyWhileCharging: Boolean = true
)

/**
 * Update frequency options
 */
@Serializable
enum class UpdateFrequency {
    DAILY,
    WEEKLY,
    MONTHLY
}
