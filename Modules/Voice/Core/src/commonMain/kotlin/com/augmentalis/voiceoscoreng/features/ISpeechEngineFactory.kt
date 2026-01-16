/**
 * ISpeechEngineFactory.kt - Factory interface for speech engines
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2026-01-05
 *
 * KMP migration of VoiceOSCore ISpeechEngineFactory.
 * Factory Pattern implementation for creating speech engine instances.
 *
 * Benefits:
 * - Open/Closed Principle: Add new engines without modifying existing code
 * - Single Responsibility: Factory handles only creation logic
 * - Dependency Inversion: Clients depend on abstraction, not concrete engines
 */
package com.augmentalis.voiceoscoreng.features

/**
 * Factory interface for creating speech engine instances.
 *
 * Each platform implements this interface to provide platform-specific
 * engine implementations. The factory determines which engines are
 * available and creates appropriate adapters.
 *
 * Usage:
 * ```kotlin
 * val factory = SpeechEngineFactory.create() // platform-specific
 * val engines = factory.getAvailableEngines()
 * val engine = factory.createEngine(SpeechEngine.VOSK)
 * ```
 */
interface ISpeechEngineFactory {

    /**
     * Get list of engines available on this platform.
     *
     * Returns only engines that can be instantiated on the current
     * platform with available dependencies.
     */
    fun getAvailableEngines(): List<SpeechEngine>

    /**
     * Check if a specific engine is available.
     *
     * @param engine Engine type to check
     * @return true if engine can be created
     */
    fun isEngineAvailable(engine: SpeechEngine): Boolean

    /**
     * Create an engine instance.
     *
     * @param engine Engine type to create
     * @return Result containing engine instance or error
     */
    fun createEngine(engine: SpeechEngine): Result<ISpeechEngine>

    /**
     * Get the recommended engine for this platform.
     *
     * Returns the best engine based on:
     * - Platform capabilities
     * - Available dependencies
     * - Default preferences
     */
    fun getRecommendedEngine(): SpeechEngine

    /**
     * Get engine capabilities/features.
     *
     * @param engine Engine to query
     * @return Set of supported features
     */
    fun getEngineFeatures(engine: SpeechEngine): Set<EngineFeature>

    /**
     * Get estimated setup requirements.
     *
     * @param engine Engine to query
     * @return Setup requirements including permissions, downloads, etc.
     */
    fun getSetupRequirements(engine: SpeechEngine): EngineRequirements
}

/**
 * Engine setup requirements
 */
data class EngineRequirements(
    /**
     * Required permissions (platform-specific strings)
     */
    val permissions: List<String>,

    /**
     * Whether model download is required
     */
    val requiresModelDownload: Boolean,

    /**
     * Estimated model size in MB (0 if no download needed)
     */
    val modelSizeMB: Int,

    /**
     * Whether network is required for operation
     */
    val requiresNetwork: Boolean,

    /**
     * Whether API key is required
     */
    val requiresApiKey: Boolean,

    /**
     * Additional setup notes
     */
    val notes: String? = null
)

/**
 * Companion object with platform-specific factory creation.
 * Uses expect/actual for platform implementation.
 */
expect object SpeechEngineFactoryProvider {
    /**
     * Create platform-specific factory instance.
     */
    fun create(): ISpeechEngineFactory
}
