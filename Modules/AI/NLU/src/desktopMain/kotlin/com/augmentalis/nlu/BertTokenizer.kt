/*
 * Copyright (c) 2024-2025 Intelligent Devices LLC / Augmentalis
 * All Rights Reserved.
 *
 * This file is part of AVA AI and is proprietary software.
 * See LICENSE file in the project root for license information.
 */

package com.augmentalis.nlu

import java.io.File

private const val TAG = "BertTokenizer"

/**
 * Desktop (JVM) implementation of BERT tokenizer using WordPiece algorithm
 *
 * Implements the same tokenization strategy as Android version but uses
 * JVM file I/O to load vocabulary from files or classpath resources.
 *
 * Performance: < 5ms for typical utterances
 */
actual class BertTokenizer(
    private val maxSequenceLength: Int = 128
) {

    private val vocab: Map<String, Int>

    // Special tokens
    private val clsToken = "[CLS]"
    private val sepToken = "[SEP]"
    private val padToken = "[PAD]"
    private val unkToken = "[UNK]"

    init {
        // Load vocabulary from file or classpath
        vocab = loadVocabulary()
    }

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

        // Word tokenization: split on whitespace and special characters
        val words = cleanedText.split("\\s+".toRegex())
            .flatMap { word ->
                // Split on punctuation while keeping tokens
                word.split("([!\"#\$%&'()*+,\\-./:;<=>?@\\[\\]^_`{|}~])".toRegex())
                    .filter { it.isNotEmpty() }
            }

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
            vocab[token] ?: vocab[unkToken] ?: 0L
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
            inputIds = LongArray(paddedInputIds.size) { paddedInputIds[it].toLong() },
            attentionMask = LongArray(paddedAttentionMask.size) { paddedAttentionMask[it].toLong() },
            tokenTypeIds = tokenTypeIds
        )
    }

    /**
     * Tokenize a pair of texts (for sentence pair tasks)
     *
     * Format: [CLS] textA [SEP] textB [SEP]
     * Token type IDs: 0 for textA, 1 for textB
     *
     * @param textA First text to tokenize
     * @param textB Second text to tokenize
     * @param maxLength Maximum sequence length (0 = use default 128)
     * @return TokenizationResult with paired tokenization
     */
    actual fun tokenizePair(
        textA: String,
        textB: String,
        maxLength: Int
    ): TokenizationResult {
        val effectiveMaxLength = if (maxLength > 0) maxLength else maxSequenceLength

        // Preprocess both texts
        val cleanedTextA = textA.lowercase().trim()
        val cleanedTextB = textB.lowercase().trim()

        val wordsA = cleanedTextA.split("\\s+".toRegex())
            .flatMap { word ->
                word.split("([!\"#\$%&'()*+,\\-./:;<=>?@\\[\\]^_`{|}~])".toRegex())
                    .filter { it.isNotEmpty() }
            }
        val wordsB = cleanedTextB.split("\\s+".toRegex())
            .flatMap { word ->
                word.split("([!\"#\$%&'()*+,\\-./:;<=>?@\\[\\]^_`{|}~])".toRegex())
                    .filter { it.isNotEmpty() }
            }

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
            vocab[token] ?: vocab[unkToken] ?: 0L
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
            inputIds = LongArray(paddedInputIds.size) { paddedInputIds[it].toLong() },
            attentionMask = LongArray(paddedAttentionMask.size) { paddedAttentionMask[it].toLong() },
            tokenTypeIds = LongArray(paddedTokenTypeIds.size) { paddedTokenTypeIds[it].toLong() }
        )
    }

    /**
     * WordPiece tokenization algorithm
     *
     * Implements greedy longest-match-first strategy:
     * 1. Start at beginning of word
     * 2. Find longest substring that exists in vocabulary
     * 3. Add ## prefix for subword tokens
     * 4. Continue from end of matched token
     *
     * @param word Word to tokenize
     * @return List of subword tokens
     */
    private fun wordPieceTokenize(word: String): List<String> {
        if (word.isEmpty()) return emptyList()

        val tokens = mutableListOf<String>()
        var start = 0

        while (start < word.length) {
            var end = word.length
            var foundToken: String? = null

            // Greedy longest-match-first: try longest substring first
            while (start < end) {
                var substr = word.substring(start, end)
                if (start > 0) {
                    // Add ## prefix for subwords (not first token)
                    substr = "##$substr"
                }

                if (vocab.containsKey(substr)) {
                    foundToken = substr
                    break
                }
                end--
            }

            if (foundToken == null) {
                // Unknown token: use [UNK] and skip this character
                tokens.add(unkToken)
                break
            }

            tokens.add(foundToken)
            start = end
        }

        return tokens
    }

    /**
     * Load vocabulary from file system or classpath
     *
     * Search order:
     * 1. models/vocab.txt in current working directory
     * 2. vocab.txt in classpath resources
     * 3. Fall back to minimal stub vocabulary
     *
     * @return Map of token -> ID
     */
    private fun loadVocabulary(): Map<String, Int> {
        val vocab = mutableMapOf<String, Int>()

        // Try 1: Load from models/vocab.txt in current directory
        val currentDirFile = File("models/vocab.txt")
        if (currentDirFile.exists()) {
            try {
                currentDirFile.bufferedReader().useLines { lines ->
                    var index = 0
                    lines.forEach { line ->
                        vocab[line.trim()] = index++
                    }
                }
                if (vocab.isNotEmpty()) {
                    nluLogInfo(TAG, "Loaded vocab from file: ${currentDirFile.absolutePath}")
                    return vocab
                }
            } catch (e: Exception) {
                nluLogError(TAG, "Failed to load vocab from file: ${e.message}", e)
            }
        }

        // Try 2: Load from classpath resource
        try {
            val resourceStream = Thread.currentThread().contextClassLoader.getResourceAsStream("models/vocab.txt")
                ?: this::class.java.getResourceAsStream("/models/vocab.txt")

            if (resourceStream != null) {
                resourceStream.bufferedReader().useLines { lines ->
                    var index = 0
                    lines.forEach { line ->
                        vocab[line.trim()] = index++
                    }
                }
                if (vocab.isNotEmpty()) {
                    nluLogInfo(TAG, "Loaded vocab from classpath resource")
                    return vocab
                }
            }
        } catch (e: Exception) {
            nluLogError(TAG, "Failed to load vocab from classpath: ${e.message}", e)
        }

        // Fallback: minimal stub vocabulary for testing
        nluLogWarn(TAG, "Using stub vocabulary (tokenization will be limited)")
        return mapOf(
            clsToken to 101,
            sepToken to 102,
            padToken to 0,
            unkToken to 100
        )
    }
}
