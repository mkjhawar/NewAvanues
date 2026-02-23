package com.augmentalis.nlu

import kotlinx.browser.window
import kotlinx.coroutines.await
import org.w3c.fetch.Response
import kotlin.js.Promise

/**
 * JS/Web implementation of BERT tokenizer using WordPiece algorithm
 *
 * Implements the same tokenization strategy as Desktop/Android versions.
 * Vocabulary loading is browser-specific: fetch() from URL or direct text injection.
 *
 * Supports both MobileBERT and mALBERT vocabularies — the WordPiece algorithm
 * is identical, only the vocab content differs. Call loadVocabFromUrl() or
 * loadVocabFromText() to switch vocabularies when the active model changes.
 *
 * Performance: < 5ms for typical utterances (pure string operations, no WASM)
 */
actual class BertTokenizer(
    private val maxSequenceLength: Int = 128
) {

    private var vocab: Map<String, Int> = STUB_VOCAB

    // Special tokens (standard BERT)
    private val clsToken = "[CLS]"
    private val sepToken = "[SEP]"
    private val padToken = "[PAD]"
    private val unkToken = "[UNK]"

    /**
     * Tokenize input text to BERT input format
     *
     * @param text Input text to tokenize
     * @param maxLength Maximum sequence length (0 = use default 128)
     * @return TokenizationResult with input_ids, attention_mask, token_type_ids
     */
    actual fun tokenize(text: String, maxLength: Int): TokenizationResult {
        val effectiveMaxLength = if (maxLength > 0) maxLength else maxSequenceLength

        // Basic preprocessing: lowercase and trim
        val cleanedText = text.lowercase().trim()

        // Word tokenization: split on whitespace and punctuation
        val words = splitOnWhitespaceAndPunctuation(cleanedText)

        // Build token list: [CLS] + wordPiece(words) + [SEP]
        val tokens = mutableListOf(clsToken)
        for (word in words) {
            tokens.addAll(wordPieceTokenize(word))
        }
        tokens.add(sepToken)

        // Truncate if exceeds max length (keep [SEP] at end)
        val truncatedTokens = if (tokens.size > effectiveMaxLength) {
            tokens.take(effectiveMaxLength - 1) + listOf(sepToken)
        } else {
            tokens
        }

        // Convert tokens to IDs using vocabulary
        val inputIds = truncatedTokens.map { token ->
            vocab[token]?.toLong() ?: vocab[unkToken]?.toLong() ?: 0L
        }

        // Create attention mask: 1 for real tokens, 0 for padding
        val attentionMask = MutableList(inputIds.size) { 1L }

        // Pad to max sequence length
        val paddedInputIds = inputIds.toMutableList()
        val paddedAttentionMask = attentionMask.toMutableList()
        while (paddedInputIds.size < effectiveMaxLength) {
            paddedInputIds.add(vocab[padToken]?.toLong() ?: 0L)
            paddedAttentionMask.add(0L)
        }

        // Token type IDs: all 0 for single sentence
        val tokenTypeIds = LongArray(effectiveMaxLength) { 0L }

        return TokenizationResult(
            inputIds = paddedInputIds.toLongArray(),
            attentionMask = paddedAttentionMask.toLongArray(),
            tokenTypeIds = tokenTypeIds
        )
    }

    /**
     * Tokenize a pair of texts (for sentence pair tasks)
     *
     * Format: [CLS] textA [SEP] textB [SEP]
     * Token type IDs: 0 for textA tokens, 1 for textB tokens
     */
    actual fun tokenizePair(
        textA: String,
        textB: String,
        maxLength: Int
    ): TokenizationResult {
        val effectiveMaxLength = if (maxLength > 0) maxLength else maxSequenceLength

        val cleanedTextA = textA.lowercase().trim()
        val cleanedTextB = textB.lowercase().trim()

        val wordsA = splitOnWhitespaceAndPunctuation(cleanedTextA)
        val wordsB = splitOnWhitespaceAndPunctuation(cleanedTextB)

        // Build tokens: [CLS] textA [SEP] textB [SEP]
        val tokens = mutableListOf(clsToken)
        for (word in wordsA) {
            tokens.addAll(wordPieceTokenize(word))
        }
        tokens.add(sepToken)

        val textALength = tokens.size

        for (word in wordsB) {
            tokens.addAll(wordPieceTokenize(word))
        }
        tokens.add(sepToken)

        // Truncate if needed
        val truncatedTokens = if (tokens.size > effectiveMaxLength) {
            tokens.take(effectiveMaxLength - 1) + listOf(sepToken)
        } else {
            tokens
        }

        // Convert to IDs
        val inputIds = truncatedTokens.map { token ->
            vocab[token]?.toLong() ?: vocab[unkToken]?.toLong() ?: 0L
        }

        // Create attention mask
        val attentionMask = MutableList(inputIds.size) { 1L }

        // Token type IDs: 0 for textA, 1 for textB
        val tokenTypeIds = MutableList(truncatedTokens.size) { idx ->
            if (idx < textALength) 0L else 1L
        }

        // Pad to max length
        val paddedInputIds = inputIds.toMutableList()
        val paddedAttentionMask = attentionMask.toMutableList()
        val paddedTokenTypeIds = tokenTypeIds.toMutableList()

        while (paddedInputIds.size < effectiveMaxLength) {
            paddedInputIds.add(vocab[padToken]?.toLong() ?: 0L)
            paddedAttentionMask.add(0L)
            paddedTokenTypeIds.add(0L)
        }

        return TokenizationResult(
            inputIds = paddedInputIds.toLongArray(),
            attentionMask = paddedAttentionMask.toLongArray(),
            tokenTypeIds = paddedTokenTypeIds.toLongArray()
        )
    }

    /**
     * Load vocabulary from a URL using the Fetch API
     *
     * This is the primary vocab loading method for browsers.
     * The URL should point to a vocab.txt file (one token per line).
     *
     * @param url URL to fetch vocabulary from (e.g., HuggingFace CDN)
     */
    suspend fun loadVocabFromUrl(url: String) {
        try {
            val response: Response = window.fetch(url).await()
            if (!response.ok) {
                console.error("[BertTokenizer] Failed to fetch vocab from $url: HTTP ${response.status}")
                return
            }
            val text = response.text().await()
            loadVocabFromText(text)
            console.log("[BertTokenizer] Loaded vocab from URL: ${vocab.size} tokens")
        } catch (e: Exception) {
            console.error("[BertTokenizer] Error loading vocab from URL: ${e.message}")
        }
    }

    /**
     * Load vocabulary from a text string (one token per line)
     *
     * Use this when vocab content is already available (e.g., from IndexedDB cache).
     *
     * @param vocabText Complete vocab.txt content
     */
    fun loadVocabFromText(vocabText: String) {
        val newVocab = mutableMapOf<String, Int>()
        var index = 0
        vocabText.lineSequence().forEach { line ->
            val token = line.trim()
            if (token.isNotEmpty()) {
                newVocab[token] = index++
            }
        }
        if (newVocab.isNotEmpty()) {
            vocab = newVocab
        }
    }

    /**
     * Reload vocabulary (for model switching at runtime)
     *
     * @param url New vocabulary URL
     */
    suspend fun reloadVocab(url: String) {
        loadVocabFromUrl(url)
    }

    /**
     * Check if a real vocabulary has been loaded (not just the stub)
     */
    fun isVocabLoaded(): Boolean = vocab.size > STUB_VOCAB.size

    /**
     * Get current vocabulary size
     */
    fun getVocabSize(): Int = vocab.size

    // ─── WordPiece Algorithm ────────────────────────────────────

    /**
     * WordPiece tokenization algorithm
     *
     * Greedy longest-match-first strategy:
     * 1. Start at beginning of word
     * 2. Find longest substring in vocabulary
     * 3. Add ## prefix for subword tokens (not first token)
     * 4. Continue from end of matched token
     * 5. Unknown characters → [UNK] and skip
     */
    private fun wordPieceTokenize(word: String): List<String> {
        if (word.isEmpty()) return emptyList()

        val tokens = mutableListOf<String>()
        var start = 0

        while (start < word.length) {
            var end = word.length
            var foundToken: String? = null

            // Greedy longest-match-first
            while (start < end) {
                var substr = word.substring(start, end)
                if (start > 0) {
                    substr = "##$substr"
                }

                if (vocab.containsKey(substr)) {
                    foundToken = substr
                    break
                }
                end--
            }

            if (foundToken == null) {
                // Unknown token: use [UNK] and break
                tokens.add(unkToken)
                break
            }

            tokens.add(foundToken)
            start = end
        }

        return tokens
    }

    /**
     * Split text on whitespace and punctuation, keeping tokens clean
     */
    private fun splitOnWhitespaceAndPunctuation(text: String): List<String> {
        return text.split(WHITESPACE_REGEX)
            .flatMap { word ->
                word.split(PUNCTUATION_REGEX).filter { it.isNotEmpty() }
            }
    }

    private companion object {
        val WHITESPACE_REGEX = "\\s+".toRegex()
        val PUNCTUATION_REGEX = "([!\"#\$%&'()*+,\\-./:;<=>?@\\[\\]^_`{|}~])".toRegex()

        /** Minimal stub vocabulary for graceful degradation when no vocab is loaded */
        val STUB_VOCAB = mapOf(
            "[CLS]" to 101,
            "[SEP]" to 102,
            "[PAD]" to 0,
            "[UNK]" to 100
        )
    }
}
