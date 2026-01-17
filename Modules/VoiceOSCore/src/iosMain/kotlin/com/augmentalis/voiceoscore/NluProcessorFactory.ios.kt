/**
 * NluProcessorFactory.ios.kt - iOS actual implementation
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2026-01-17
 *
 * iOS implementation of NLU processor factory.
 * TODO: Integrate with CoreML for on-device NLU.
 */
package com.augmentalis.voiceoscore

/**
 * iOS NLU processor factory implementation.
 *
 * Creates stub NLU processor until CoreML integration is complete.
 */
actual object NluProcessorFactory {
    /**
     * Create iOS-specific NLU processor.
     *
     * @param config NLU configuration
     * @return INluProcessor implementation (stub for now)
     */
    actual fun create(config: NluConfig): INluProcessor = StubNluProcessorIOS(config)
}

/**
 * Stub NLU processor for iOS.
 * Returns NoMatch for all classifications until real implementation.
 */
internal class StubNluProcessorIOS(private val config: NluConfig) : INluProcessor {
    private var initialized = false

    override suspend fun initialize(): Result<Unit> {
        initialized = config.enabled
        return Result.success(Unit)
    }

    override suspend fun classify(
        utterance: String,
        candidateCommands: List<QuantizedCommand>
    ): NluResult {
        if (!initialized || !config.enabled) {
            return NluResult.Error("NLU processor not initialized or disabled")
        }
        // Stub: Always return NoMatch
        return NluResult.NoMatch
    }

    override fun isAvailable(): Boolean = initialized && config.enabled

    override suspend fun dispose() {
        initialized = false
    }
}
