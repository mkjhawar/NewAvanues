package com.augmentalis.nlu

/**
 * macOS implementation of BertTokenizer
 *
 * Returns zero-filled tokenization results matching iOS behavior.
 * Full WordPiece tokenization to be implemented when ONNX/CoreML
 * tensor interop is configured for macOS inference.
 */
actual class BertTokenizer {

    actual fun tokenize(text: String, maxLength: Int): TokenizationResult {
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
        return TokenizationResult(
            inputIds = LongArray(maxLength),
            attentionMask = LongArray(maxLength),
            tokenTypeIds = LongArray(maxLength)
        )
    }
}
