package com.augmentalis.nlu

/**
 * Darwin (iOS + macOS) stub implementation of BertTokenizer.
 *
 * Returns zero-filled tokenization results. Full WordPiece tokenization
 * requires CoreML tensor interop to be configured for on-device inference.
 *
 * WARNING: Both tokenize() and tokenizePair() return all-zero arrays.
 * The attention mask being all zeros tells the model to IGNORE all tokens,
 * producing garbage embeddings. Callers should check for this and fall
 * through to keyword matching when BertTokenizer returns zero arrays.
 */
actual class BertTokenizer {

    private var stubWarningEmitted = false

    actual fun tokenize(text: String, maxLength: Int): TokenizationResult {
        if (!stubWarningEmitted) {
            nluLogWarn("BertTokenizer", "Darwin BertTokenizer is a stub — returning zero arrays. " +
                "BERT-based semantic verification will be skipped. " +
                "Configure CoreML tensor interop to enable full tokenization.")
            stubWarningEmitted = true
        }
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
