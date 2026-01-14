/**
 * DesktopNluProcessor.kt - Desktop NLU Processor Stub
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2026-01-09
 *
 * Stub implementation for Desktop (JVM) platform.
 * TODO: Implement using ONNX Runtime JVM for BERT inference.
 */
package com.augmentalis.voiceoscoreng.nlu

import com.augmentalis.voiceoscoreng.common.QuantizedCommand

/**
 * Desktop stub implementation of [INluProcessor].
 *
 * This is a placeholder that always returns NoMatch.
 * The stub reports itself as NOT available via [isAvailable],
 * indicating that NLU functionality is not implemented on this platform.
 *
 * TODO: Implement using ONNX Runtime JVM for desktop BERT inference.
 */
class DesktopNluProcessor(
    private val config: NluConfig = NluConfig.DEFAULT
) : INluProcessor {

    private var initialized = false

    override suspend fun initialize(): Result<Unit> {
        // Stub initialization always succeeds but does nothing
        // isAvailable() will still return false to indicate no real NLU capability
        initialized = true
        println("[DesktopNluProcessor] Stub initialized - NLU not available on Desktop yet")
        return Result.success(Unit)
    }

    override suspend fun classify(
        utterance: String,
        candidateCommands: List<QuantizedCommand>
    ): NluResult {
        // Stub always returns NoMatch - no NLU capability on Desktop yet
        return NluResult.NoMatch
    }

    /**
     * Returns false because NLU is not actually available on Desktop.
     * Callers should check this and fall back to other matching methods.
     */
    override fun isAvailable(): Boolean = false

    override suspend fun dispose() {
        initialized = false
    }
}
