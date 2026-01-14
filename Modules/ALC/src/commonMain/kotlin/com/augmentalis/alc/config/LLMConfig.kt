package com.augmentalis.alc.config

import com.augmentalis.alc.domain.ProviderType
import kotlinx.serialization.Serializable

/**
 * Main LLM configuration
 */
@Serializable
data class LLMConfig(
    val defaultProvider: ProviderType = ProviderType.ANTHROPIC,
    val fallbackProviders: List<ProviderType> = listOf(
        ProviderType.OPENAI,
        ProviderType.OPENROUTER
    ),
    val localInferenceEnabled: Boolean = true,
    val streamingEnabled: Boolean = true,
    val maxRetries: Int = 3,
    val timeoutMs: Long = 30000,
    val cacheEnabled: Boolean = true
)

/**
 * Model configuration for local inference
 */
@Serializable
data class ModelConfig(
    val modelId: String,
    val modelPath: String,
    val vocabPath: String? = null,
    val contextLength: Int = 4096,
    val batchSize: Int = 1,
    val numThreads: Int = 4,
    val useGPU: Boolean = true,
    val quantization: QuantizationType = QuantizationType.INT8
)

/**
 * Quantization types for local models
 */
@Serializable
enum class QuantizationType {
    FP32,
    FP16,
    INT8,
    INT4
}

/**
 * Device profile for adaptive configuration
 */
@Serializable
data class DeviceProfile(
    val deviceClass: DeviceClass,
    val totalMemoryMB: Int,
    val availableMemoryMB: Int,
    val cpuCores: Int,
    val hasGPU: Boolean,
    val gpuMemoryMB: Int? = null
) {
    companion object {
        fun fromMemory(memoryMB: Int, cores: Int, hasGPU: Boolean): DeviceProfile {
            val deviceClass = when {
                memoryMB >= 8192 && hasGPU -> DeviceClass.HIGH_END
                memoryMB >= 4096 -> DeviceClass.MID_RANGE
                memoryMB >= 2048 -> DeviceClass.LOW_END
                else -> DeviceClass.MINIMAL
            }
            return DeviceProfile(
                deviceClass = deviceClass,
                totalMemoryMB = memoryMB,
                availableMemoryMB = (memoryMB * 0.7).toInt(),
                cpuCores = cores,
                hasGPU = hasGPU
            )
        }
    }
}

/**
 * Device classification
 */
@Serializable
enum class DeviceClass {
    HIGH_END,   // Flagship phones, M1+ Macs, gaming PCs
    MID_RANGE,  // Mid-tier phones, standard laptops
    LOW_END,    // Budget phones, older devices
    MINIMAL     // Very constrained devices
}

/**
 * Model selection based on device profile
 */
object ModelSelector {
    fun selectModel(profile: DeviceProfile): ModelConfig {
        return when (profile.deviceClass) {
            DeviceClass.HIGH_END -> ModelConfig(
                modelId = "phi-2-q4",
                modelPath = "models/phi-2-q4.onnx",
                contextLength = 4096,
                numThreads = profile.cpuCores,
                useGPU = profile.hasGPU
            )
            DeviceClass.MID_RANGE -> ModelConfig(
                modelId = "tinyllama-1.1b-q4",
                modelPath = "models/tinyllama-1.1b-q4.onnx",
                contextLength = 2048,
                numThreads = minOf(profile.cpuCores, 4),
                useGPU = profile.hasGPU
            )
            DeviceClass.LOW_END -> ModelConfig(
                modelId = "tinyllama-1.1b-q8",
                modelPath = "models/tinyllama-1.1b-q8.onnx",
                contextLength = 1024,
                numThreads = 2,
                useGPU = false
            )
            DeviceClass.MINIMAL -> ModelConfig(
                modelId = "template-only",
                modelPath = "",
                contextLength = 512,
                numThreads = 1,
                useGPU = false
            )
        }
    }
}
