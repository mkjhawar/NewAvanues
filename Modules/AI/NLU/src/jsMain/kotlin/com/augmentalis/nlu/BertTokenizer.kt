// filename: features/nlu/src/jsMain/kotlin/com/augmentalis/ava/features/nlu/BertTokenizer.kt
// created: 2025-11-02
// author: Manoj Jhawar
// © Augmentalis Inc, Intelligent Devices LLC
// TCR: Phase 1 - KMP Migration (JS/Web stub)

package com.augmentalis.nlu

/**
 * JS/Web stub implementation of BertTokenizer
 *
 * TODO Phase 2: Implement using TensorFlow.js tokenizers
 */
actual class BertTokenizer {

    actual fun tokenize(text: String, maxLength: Int): TokenizationResult {
        // Return empty tokenization result
        return TokenizationResult(
            inputIds = LongArray(maxLength),
            attentionMask = LongArray(maxLength),
            tokenTypeIds = LongArray(maxLength)
        )
    }

    actual fun tokenizePair(
        textA: String,
        textB: String,
        maxLength: Int
    ): TokenizationResult {
        // Return empty tokenization result
        return TokenizationResult(
            inputIds = LongArray(maxLength),
            attentionMask = LongArray(maxLength),
            tokenTypeIds = LongArray(maxLength)
        )
    }
}
