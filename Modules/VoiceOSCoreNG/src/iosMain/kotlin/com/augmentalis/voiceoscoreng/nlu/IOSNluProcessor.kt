/**
 * IOSNluProcessor.kt - iOS NLU Processor Stub
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2026-01-09
 *
 * Stub implementation for iOS platform.
 * TODO: Implement using CoreML for BERT inference.
 */
package com.augmentalis.voiceoscoreng.nlu

import com.augmentalis.voiceoscoreng.common.QuantizedCommand

/**
 * iOS stub implementation of [INluProcessor].
 *
 * Currently returns NoMatch for all classifications.
 * TODO: Implement using CoreML with BERT model.
 */
class IOSNluProcessor(
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
