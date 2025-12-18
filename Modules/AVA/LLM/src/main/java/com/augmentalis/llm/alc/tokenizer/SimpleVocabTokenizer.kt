package com.augmentalis.llm.alc.tokenizer

import android.content.Context
import com.augmentalis.llm.alc.streaming.ITokenizer
import timber.log.Timber
import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * Simple vocabulary-based tokenizer implementation
 *
 * This is a basic tokenizer that uses a vocab.txt file to map between
 * words and token IDs. It provides a working tokenizer to unblock the
 * LocalLLMProvider while a full TVM tokenizer integration is developed.
 *
 * Features:
 * - Loads vocabulary from vocab.txt asset
 * - Simple word-level tokenization with basic punctuation handling
 * - Special tokens: [PAD], [UNK], [CLS], [SEP], [MASK]
 * - Fallback to character-level for unknown words
 */
class SimpleVocabTokenizer(
    private val context: Context
) : ITokenizer {

    private val wordToId = mutableMapOf<String, Int>()
    private val idToWord = mutableMapOf<Int, String>()
    private var vocabSize = 0

    // Special tokens
    private val PAD_TOKEN = "[PAD]"
    private val UNK_TOKEN = "[UNK]"
    private val CLS_TOKEN = "[CLS]"
    private val SEP_TOKEN = "[SEP]"
    private val MASK_TOKEN = "[MASK]"

    private var padId = 0
    private var unkId = 1
    private var clsId = 2
    private var sepId = 3
    private var maskId = 4

    init {
        loadVocabulary()
    }

    private fun loadVocabulary() {
        try {
            // Try to load vocab.txt from assets
            val vocabFiles = listOf(
                "models/vocab.txt",
                "models/gemma-2b-it/vocab.txt",
                "models/mobilellm/vocab.txt"
            )

            var loaded = false
            for (vocabFile in vocabFiles) {
                try {
                    context.assets.open(vocabFile).use { inputStream ->
                        BufferedReader(InputStreamReader(inputStream)).use { reader ->
                            var id = 0
                            reader.forEachLine { line ->
                                val word = line.trim()
                                if (word.isNotEmpty()) {
                                    wordToId[word] = id
                                    idToWord[id] = word

                                    // Track special token IDs
                                    when (word) {
                                        PAD_TOKEN -> padId = id
                                        UNK_TOKEN -> unkId = id
                                        CLS_TOKEN -> clsId = id
                                        SEP_TOKEN -> sepId = id
                                        MASK_TOKEN -> maskId = id
                                    }

                                    id++
                                }
                            }
                            vocabSize = id
                            Timber.d("Loaded vocabulary from $vocabFile: $vocabSize tokens")
                            loaded = true
                        }
                    }
                    if (loaded) break
                } catch (e: Exception) {
                    // Try next file
                }
            }

            if (!loaded) {
                // Create a basic vocabulary if no vocab file found
                createBasicVocabulary()
            }

        } catch (e: Exception) {
            Timber.w(e, "Failed to load vocabulary, using basic fallback")
            createBasicVocabulary()
        }
    }

    private fun createBasicVocabulary() {
        // Create a minimal vocabulary for basic functionality
        val basicTokens = listOf(
            PAD_TOKEN, UNK_TOKEN, CLS_TOKEN, SEP_TOKEN, MASK_TOKEN,
            ".", ",", "!", "?", "'", "\"", "-", "(", ")", " ",
            "the", "a", "an", "is", "are", "was", "were", "be", "been",
            "have", "has", "had", "do", "does", "did", "will", "would",
            "can", "could", "may", "might", "must", "shall", "should",
            "I", "you", "he", "she", "it", "we", "they", "me", "him", "her",
            "what", "when", "where", "who", "why", "how", "which",
            "hello", "hi", "yes", "no", "please", "thank", "thanks",
            "weather", "time", "light", "lights", "temperature", "alarm"
        )

        basicTokens.forEachIndexed { index, token ->
            wordToId[token.lowercase()] = index
            idToWord[index] = token.lowercase()
        }

        // Add common letters and numbers for character-level fallback
        for (c in 'a'..'z') {
            val id = wordToId.size
            wordToId[c.toString()] = id
            idToWord[id] = c.toString()
        }

        for (i in 0..9) {
            val id = wordToId.size
            wordToId[i.toString()] = id
            idToWord[id] = i.toString()
        }

        vocabSize = wordToId.size
        Timber.d("Created basic vocabulary: $vocabSize tokens")
    }

    override fun encode(text: String): List<Int> {
        val tokens = mutableListOf<Int>()

        // Simple tokenization: lowercase, split by spaces and punctuation
        val normalized = text.lowercase().trim()

        // Split into words, keeping punctuation as separate tokens
        val words = normalized.split(Regex("(\\s+|(?=[.,!?'\"-()])|(?<=[.,!?'\"-()]))"))
            .filter { it.isNotBlank() }

        for (word in words) {
            val id = wordToId[word]
            if (id != null) {
                tokens.add(id)
            } else {
                // For unknown words, try character-level tokenization
                if (word.length <= 5) {
                    // Short words: use character-level
                    for (char in word) {
                        val charId = wordToId[char.toString()] ?: unkId
                        tokens.add(charId)
                    }
                } else {
                    // Long words: just use UNK token
                    tokens.add(unkId)
                }
            }
        }

        // Add special tokens if needed (simplified for now)
        if (tokens.isEmpty()) {
            tokens.add(unkId)
        }

        Timber.v("Encoded '$text' to ${tokens.size} tokens")
        return tokens
    }

    override fun decode(tokens: List<Int>): String {
        val words = mutableListOf<String>()

        for (tokenId in tokens) {
            val word = idToWord[tokenId]
            if (word != null && word !in listOf(PAD_TOKEN, CLS_TOKEN, SEP_TOKEN)) {
                if (word == UNK_TOKEN) {
                    words.add("?")
                } else {
                    words.add(word)
                }
            }
        }

        // Simple detokenization: join with appropriate spacing
        val result = StringBuilder()
        for (i in words.indices) {
            val word = words[i]

            // Add space before word unless it's punctuation or first word
            if (i > 0 && word !in listOf(".", ",", "!", "?", "'", "\"", ")", "-")) {
                val prevWord = words[i - 1]
                if (prevWord !in listOf("(", "\"", "'", "-")) {
                    result.append(" ")
                }
            }

            result.append(word)
        }

        val text = result.toString().trim()
        Timber.v("Decoded ${tokens.size} tokens to '$text'")
        return text
    }
}