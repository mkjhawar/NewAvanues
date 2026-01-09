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
 * Currently returns NoMatch for all classifications.
 * TODO: Implement using ONNX Runtime JVM.
 */
class DesktopNluProcessor(
    private val config: NluConfig = NluConfig.DEFAULT
) : INluProcessor {

    override suspend fun initialize(): Result<Unit> {
        // No-op for stub
        return Result.success(Unit)
    }

    override suspend fun classify(
        utterance: String,
        candidateCommands: List<QuantizedCommand>
    ): NluResult {
        // Stub always returns NoMatch
        return NluResult.NoMatch
    }

    override fun isAvailable(): Boolean = false

    override suspend fun dispose() {
        // No-op for stub
    }
}
