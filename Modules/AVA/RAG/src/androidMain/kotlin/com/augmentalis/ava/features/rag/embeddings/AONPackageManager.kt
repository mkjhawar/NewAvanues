// filename: Universal/AVA/Features/RAG/src/androidMain/kotlin/com/augmentalis/ava/features/rag/embeddings/AONPackageManager.kt
// created: 2025-11-23
// author: AVA AI Team
// © Augmentalis Inc, Intelligent Devices LLC

package com.augmentalis.ava.features.rag.embeddings

/**
 * AON Package Authorization Management
 *
 * Handles package whitelist configurations for AON file access.
 *
 * ## Limitation: 3 Packages Maximum
 * AON v1.0 header has fixed 256-byte size with 48 bytes allocated for package hashes:
 * - 3 packages × 16 bytes (MD5) = 48 bytes
 * - Cannot exceed without breaking format compatibility
 *
 * ## Solution: Multiple Presets
 * Create different AON files for different app groups.
 */
object AONPackageManager {

    /**
     * Standard AVA Ecosystem Apps
     *
     * Use this preset for models shared across primary AVA apps.
     */
    val AVA_STANDARD_APPS = listOf(
        "com.augmentalis.ava",          // AVA Standalone
        "com.augmentalis.avaconnect",   // AVA Connect
        "com.augmentalis.voiceos"       // VoiceOS
    )

    /**
     * Avanues Platform Apps
     *
     * Use this preset for models shared across Avanues ecosystem.
     */
    val AVANUES_PLATFORM_APPS = listOf(
        "com.augmentalis.avanues",      // Avanues Platform
        "com.augmentalis.ava",          // AVA Standalone (shared)
        "com.augmentalis.avaconnect"    // AVA Connect (shared)
    )

    /**
     * Development/Testing Apps
     *
     * Use this preset for debug builds and testing.
     */
    val DEVELOPMENT_APPS = listOf(
        "com.augmentalis.ava.debug",
        "com.augmentalis.ava.staging",
        "com.augmentalis.ava.test"
    )

    /**
     * All AVA Apps (excluding Avanues)
     *
     * Default preset for most models.
     */
    val ALL_AVA_APPS = listOf(
        "com.augmentalis.ava",
        "com.augmentalis.avaconnect",
        "com.augmentalis.voiceos"
    )

    /**
     * Get appropriate package list based on model distribution strategy
     *
     * @param strategy Distribution strategy
     * @return List of up to 3 package names
     */
    fun getPackagesForStrategy(strategy: DistributionStrategy): List<String> {
        return when (strategy) {
            DistributionStrategy.AVA_STANDARD -> AVA_STANDARD_APPS
            DistributionStrategy.AVANUES_PLATFORM -> AVANUES_PLATFORM_APPS
            DistributionStrategy.DEVELOPMENT -> DEVELOPMENT_APPS
            DistributionStrategy.ALL_AVA -> ALL_AVA_APPS
        }
    }

    /**
     * Check if a package is authorized for any AVA app
     *
     * Used during unwrap when you want to allow any AVA ecosystem app.
     * Note: This is less secure than whitelist validation.
     */
    fun isAVAEcosystemPackage(packageName: String): Boolean {
        return packageName.startsWith("com.augmentalis.ava") ||
               packageName.startsWith("com.augmentalis.voiceos") ||
               packageName == "com.augmentalis.avanues"
    }

    /**
     * Distribution strategies for different use cases
     */
    enum class DistributionStrategy {
        /** Standard AVA apps: AVA, AVAConnect, VoiceOS */
        AVA_STANDARD,

        /** Avanues platform apps: Avanues, AVA, AVAConnect */
        AVANUES_PLATFORM,

        /** Development and testing builds */
        DEVELOPMENT,

        /** All primary AVA apps (default) */
        ALL_AVA
    }

    /**
     * Validation: Ensure package list doesn't exceed limit
     *
     * @throws IllegalArgumentException if more than 3 packages
     */
    fun validatePackageList(packages: List<String>) {
        require(packages.size <= 3) {
            "AON v1.0 supports maximum 3 packages. Got: ${packages.size}\n" +
            "Packages: ${packages.joinToString()}\n\n" +
            "Solutions:\n" +
            "1. Create separate AON files for different app groups\n" +
            "2. Use AONPackageManager presets (AVA_STANDARD_APPS, AVANUES_PLATFORM_APPS)\n" +
            "3. Wait for AON v2.0 which supports unlimited packages"
        }
    }
}

/**
 * Extension function to wrap with preset package list
 */
fun AONFileManager.wrapWithPreset(
    onnxFile: java.io.File,
    outputFile: java.io.File,
    modelId: String,
    strategy: AONPackageManager.DistributionStrategy = AONPackageManager.DistributionStrategy.AVA_STANDARD,
    modelVersion: Int = 1,
    expiryTimestamp: Long = 0,
    licenseTier: Int = 0,
    encrypt: Boolean = false
): java.io.File {
    val packages = AONPackageManager.getPackagesForStrategy(strategy)

    return wrapONNX(
        onnxFile = onnxFile,
        outputFile = outputFile,
        modelId = modelId,
        modelVersion = modelVersion,
        allowedPackages = packages,
        expiryTimestamp = expiryTimestamp,
        licenseTier = licenseTier,
        encrypt = encrypt
    )
}
