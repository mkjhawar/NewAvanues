// filename: features/nlu/src/iosMain/kotlin/com/augmentalis/ava/features/nlu/BertTokenizer.kt
// created: 2025-11-02
// author: Claude Code
// © Augmentalis Inc, Intelligent Devices LLC
// TCR: Phase 1 - KMP Migration (iOS stub)

package com.augmentalis.ava.features.nlu

/**
 * iOS stub implementation of BertTokenizer
 *
 * TODO Phase 2: Implement WordPiece tokenization for iOS
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
