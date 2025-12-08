package com.augmentalis.ava.features.nlu

/**
 * Supported embedding model types
 *
 * AVA supports dual model architecture:
 * - MobileBERT: Lightweight, bundled in APK (default fallback)
 * - mALBERT: Higher quality, downloaded or pre-installed on device
 *
 * Model selection priority:
 * 1. mALBERT-768 (if available on device) - Best quality
 * 2. MobileBERT-384 (bundled in APK) - Fast fallback
 */
enum class ModelType(
    val displayName: String,
    val embeddingDimension: Int,
    val modelFileName: String,
    val description: String
) {
    /**
     * MobileBERT - Lightweight model bundled in APK
     *
     * Specs:
     * - Embedding dimension: 384
     * - Model size: ~25 MB (INT8 quantized)
     * - Languages: English-only
     * - Speed: <50ms inference
     * - Source: Bundled in APK assets
     */
    MOBILEBERT(
        displayName = "MobileBERT Lite",
        embeddingDimension = 384,
        modelFileName = "AVA-384-Base-INT8.AON",
        description = "Lightweight English model (bundled)"
    ),

    /**
     * mALBERT - Multilingual model (downloaded or pre-installed)
     *
     * Specs:
     * - Embedding dimension: 768
     * - Model size: ~41 MB
     * - Languages: 52+ languages
     * - Speed: <80ms inference
     * - Source: Downloaded from cloud or pre-installed
     */
    MALBERT(
        displayName = "mALBERT Multilingual",
        embeddingDimension = 768,
        modelFileName = "AVA-768-Base-INT8.AON",
        description = "Multilingual model (52+ languages)"
    );

    /**
     * Check if this is a multilingual model
     */
    fun isMultilingual(): Boolean = this == MALBERT

    /**
     * Get model version identifier for database storage
     */
    fun getModelVersion(): String = when (this) {
        MOBILEBERT -> "MobileBERT-uncased-onnx-384"
        MALBERT -> "mALBERT-base-v2-onnx-768"
    }
}
