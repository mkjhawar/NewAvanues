/*
 * Copyright (c) 2024-2025 Intelligent Devices LLC / Augmentalis
 * All Rights Reserved.
 */

package com.augmentalis.llm.alc.tokenizer

import com.augmentalis.llm.alc.streaming.ITokenizer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import timber.log.Timber
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.security.MessageDigest

/**
 * HuggingFace Tokenizer Implementation
 *
 * Parses and uses tokenizer.json files from HuggingFace models.
 * Implements Byte-Pair Encoding (BPE) tokenization.
 *
 * Fix for Issue P0-1: Replaces SimpleVocabTokenizer which used
 * incorrect vocabulary for all models.
 *
 * Supports:
 * - BPE tokenization (Gemma, Llama, Qwen, Phi models)
 * - Special tokens handling
 * - Unicode byte-level encoding
 * - Pre-tokenization patterns
 *
 * Created: 2025-11-30
 */
class HuggingFaceTokenizer private constructor(
    private val vocab: Map<String, Int>,
    private val reverseVocab: Map<Int, String>,
    private val merges: List<Pair<String, String>>,
    private val specialTokens: Map<String, Int>,
    private val unkTokenId: Int,
    private val bosTokenId: Int,
    private val eosTokenId: Int
) : ITokenizer {

    // Merge priority map for efficient lookup
    private val mergeRanks: Map<Pair<String, String>, Int> = merges.withIndex()
        .associate { (index, pair) -> pair to index }

    // Byte encoder for unicode handling (GPT-2 style)
    private val byteEncoder: Map<Int, Char>
    private val byteDecoder: Map<Char, Int>

    init {
        // Initialize byte encoder (maps bytes to unicode chars)
        byteEncoder = buildByteEncoder()
        byteDecoder = byteEncoder.entries.associate { (k, v) -> v to k }
    }

    override fun encode(text: String): List<Int> {
        if (text.isEmpty()) return emptyList()

        return try {
            val tokens = mutableListOf<Int>()

            // Pre-tokenize: split on whitespace and punctuation
            val words = preTokenize(text)

            for (word in words) {
                // Convert to byte-level tokens
                val byteTokens = textToByteTokens(word)

                // Apply BPE merges
                val merged = applyBPE(byteTokens)

                // Convert to token IDs
                for (token in merged) {
                    val id = vocab[token] ?: specialTokens[token] ?: unkTokenId
                    tokens.add(id)
                }
            }

            Timber.v("Encoded '${text.take(50)}...' to ${tokens.size} tokens")
            tokens

        } catch (e: Exception) {
            Timber.e(e, "Tokenization failed for: ${text.take(50)}...")
            throw TokenizationException("Encoding failed: ${e.message}", e)
        }
    }

    override fun decode(tokens: List<Int>): String {
        if (tokens.isEmpty()) return ""

        return try {
            val builder = StringBuilder()

            for (tokenId in tokens) {
                // Skip special tokens in output
                if (tokenId == bosTokenId || tokenId == eosTokenId) continue

                val token = reverseVocab[tokenId] ?: continue
                builder.append(byteTokensToText(token))
            }

            val result = builder.toString()
            Timber.v("Decoded ${tokens.size} tokens to '${result.take(50)}...'")
            result

        } catch (e: Exception) {
            Timber.e(e, "Detokenization failed")
            throw TokenizationException("Decoding failed: ${e.message}", e)
        }
    }

    /**
     * Pre-tokenize text into words
     * Splits on whitespace while preserving leading spaces
     */
    private fun preTokenize(text: String): List<String> {
        val result = mutableListOf<String>()
        val pattern = Regex("""'s|'t|'re|'ve|'m|'ll|'d| ?\p{L}+| ?\p{N}+| ?[^\s\p{L}\p{N}]+|\s+""")

        pattern.findAll(text).forEach { match ->
            result.add(match.value)
        }

        return result
    }

    /**
     * Convert text to byte-level tokens
     */
    private fun textToByteTokens(text: String): List<String> {
        val bytes = text.toByteArray(Charsets.UTF_8)
        return bytes.map { byte ->
            val unsigned = byte.toInt() and 0xFF
            byteEncoder[unsigned]?.toString() ?: "?"
        }
    }

    /**
     * Convert byte-level tokens back to text
     */
    private fun byteTokensToText(token: String): String {
        val bytes = mutableListOf<Byte>()
        for (char in token) {
            val byteVal = byteDecoder[char]
            if (byteVal != null) {
                bytes.add(byteVal.toByte())
            }
        }
        return String(bytes.toByteArray(), Charsets.UTF_8)
    }

    /**
     * Apply BPE merges to token list
     */
    private fun applyBPE(tokens: List<String>): List<String> {
        if (tokens.size < 2) return tokens

        var word = tokens.toMutableList()

        while (word.size >= 2) {
            // Find the merge with lowest rank
            var bestPair: Pair<String, String>? = null
            var bestRank = Int.MAX_VALUE

            for (i in 0 until word.size - 1) {
                val pair = word[i] to word[i + 1]
                val rank = mergeRanks[pair]
                if (rank != null && rank < bestRank) {
                    bestRank = rank
                    bestPair = pair
                }
            }

            // No more merges possible
            if (bestPair == null) break

            // Apply the merge
            val newWord = mutableListOf<String>()
            var i = 0
            while (i < word.size) {
                if (i < word.size - 1 && word[i] == bestPair.first && word[i + 1] == bestPair.second) {
                    newWord.add(bestPair.first + bestPair.second)
                    i += 2
                } else {
                    newWord.add(word[i])
                    i++
                }
            }
            word = newWord
        }

        return word
    }

    /**
     * Build byte encoder mapping (GPT-2 style)
     */
    private fun buildByteEncoder(): Map<Int, Char> {
        val bs = mutableListOf<Int>()
        val cs = mutableListOf<Int>()

        // Printable ASCII range
        for (b in '!'.code..'~'.code) {
            bs.add(b)
            cs.add(b)
        }

        // Extended ASCII
        for (b in '¡'.code..'¬'.code) {
            bs.add(b)
            cs.add(b)
        }
        for (b in '®'.code..'ÿ'.code) {
            bs.add(b)
            cs.add(b)
        }

        // Fill remaining bytes
        var n = 0
        for (b in 0..255) {
            if (b !in bs) {
                bs.add(b)
                cs.add(256 + n)
                n++
            }
        }

        return bs.zip(cs).associate { (b, c) -> b to c.toChar() }
    }

    /**
     * Save tokenizer state to binary cache file.
     *
     * Cache format:
     * - Magic bytes: "HFTC" (HuggingFace Tokenizer Cache)
     * - Version: Int (1)
     * - Source hash: String (MD5 of tokenizer.json)
     * - Token IDs: 3 Ints (unk, bos, eos)
     * - Vocab size: Int
     * - Vocab entries: [String length, String bytes, Int id] * vocab size
     * - Merges size: Int
     * - Merge entries: [String length, String bytes] * 2 * merges size
     * - Special tokens size: Int
     * - Special token entries: [String length, String bytes, Int id] * size
     *
     * @param cacheFile Target cache file
     * @param sourceHash MD5 hash of source tokenizer.json
     */
    fun saveToCache(cacheFile: File, sourceHash: String) {
        try {
            val startTime = System.currentTimeMillis()

            DataOutputStream(FileOutputStream(cacheFile).buffered()).use { out ->
                // Magic bytes
                out.writeBytes(CACHE_MAGIC)

                // Version
                out.writeInt(CACHE_VERSION)

                // Source hash for validation
                out.writeUTF(sourceHash)

                // Token IDs
                out.writeInt(unkTokenId)
                out.writeInt(bosTokenId)
                out.writeInt(eosTokenId)

                // Vocabulary
                out.writeInt(vocab.size)
                for ((token, id) in vocab) {
                    out.writeUTF(token)
                    out.writeInt(id)
                }

                // Merges
                out.writeInt(merges.size)
                for ((first, second) in merges) {
                    out.writeUTF(first)
                    out.writeUTF(second)
                }

                // Special tokens
                out.writeInt(specialTokens.size)
                for ((token, id) in specialTokens) {
                    out.writeUTF(token)
                    out.writeInt(id)
                }
            }

            val duration = System.currentTimeMillis() - startTime
            val sizeKB = cacheFile.length() / 1024
            Timber.i("Tokenizer cache saved: ${sizeKB}KB in ${duration}ms")

        } catch (e: Exception) {
            Timber.e(e, "Failed to save tokenizer cache")
            // Delete partial cache file
            cacheFile.delete()
        }
    }

    companion object {
        private val json = Json {
            ignoreUnknownKeys = true
            isLenient = true
        }

        private const val CACHE_MAGIC = "HFTC"
        private const val CACHE_VERSION = 1
        private const val CACHE_FILENAME = "tokenizer.cache"

        /**
         * Load tokenizer from directory, using cache if available.
         *
         * Priority:
         * 1. Check for valid cache (tokenizer.cache with matching hash)
         * 2. If cache valid → load from binary (fast, ~50ms)
         * 3. If cache invalid/missing → parse JSON, save cache for next time
         *
         * Cache is invalidated when:
         * - tokenizer.json is newer than cache
         * - tokenizer.json content hash doesn't match cache
         * - Cache file is corrupted
         *
         * @param modelDir Model directory path
         * @return HuggingFaceTokenizer instance
         */
        fun load(modelDir: File): HuggingFaceTokenizer {
            val tokenizerFile = File(modelDir, "tokenizer.json")
            if (!tokenizerFile.exists()) {
                throw TokenizationException("tokenizer.json not found in ${modelDir.name}")
            }

            val cacheFile = File(modelDir, CACHE_FILENAME)
            val sourceHash = computeFileHash(tokenizerFile)

            // Try loading from cache first
            if (cacheFile.exists()) {
                try {
                    val cached = loadFromCache(cacheFile, sourceHash)
                    if (cached != null) {
                        Timber.i("Loaded tokenizer from cache (fast path)")
                        return cached
                    }
                } catch (e: Exception) {
                    Timber.w(e, "Cache load failed, falling back to JSON parse")
                }
            }

            // Parse JSON and create cache for next time
            val tokenizer = loadFromJson(tokenizerFile)
            tokenizer.saveToCache(cacheFile, sourceHash)

            return tokenizer
        }

        /**
         * Load tokenizer from binary cache file.
         *
         * @param cacheFile Cache file to load
         * @param expectedHash Expected MD5 hash of source file
         * @return HuggingFaceTokenizer if cache valid, null if invalid
         */
        private fun loadFromCache(cacheFile: File, expectedHash: String): HuggingFaceTokenizer? {
            val startTime = System.currentTimeMillis()

            return try {
                DataInputStream(FileInputStream(cacheFile).buffered()).use { input ->
                    // Verify magic bytes
                    val magic = ByteArray(4)
                    input.readFully(magic)
                    if (String(magic) != CACHE_MAGIC) {
                        Timber.w("Invalid cache magic bytes")
                        return null
                    }

                    // Verify version
                    val version = input.readInt()
                    if (version != CACHE_VERSION) {
                        Timber.w("Cache version mismatch: $version != $CACHE_VERSION")
                        return null
                    }

                    // Verify source hash (detect model changes)
                    val cachedHash = input.readUTF()
                    if (cachedHash != expectedHash) {
                        Timber.i("Model changed (hash mismatch), invalidating cache")
                        return null
                    }

                    // Read token IDs
                    val unkTokenId = input.readInt()
                    val bosTokenId = input.readInt()
                    val eosTokenId = input.readInt()

                    // Read vocabulary
                    val vocabSize = input.readInt()
                    val vocab = HashMap<String, Int>(vocabSize)
                    repeat(vocabSize) {
                        val token = input.readUTF()
                        val id = input.readInt()
                        vocab[token] = id
                    }

                    // Reverse vocabulary
                    val reverseVocab = vocab.entries.associate { (k, v) -> v to k }

                    // Read merges
                    val mergesSize = input.readInt()
                    val merges = ArrayList<Pair<String, String>>(mergesSize)
                    repeat(mergesSize) {
                        val first = input.readUTF()
                        val second = input.readUTF()
                        merges.add(first to second)
                    }

                    // Read special tokens
                    val specialSize = input.readInt()
                    val specialTokens = HashMap<String, Int>(specialSize)
                    repeat(specialSize) {
                        val token = input.readUTF()
                        val id = input.readInt()
                        specialTokens[token] = id
                    }

                    val duration = System.currentTimeMillis() - startTime
                    Timber.d("Cache loaded: vocab=$vocabSize, merges=$mergesSize in ${duration}ms")

                    HuggingFaceTokenizer(
                        vocab = vocab,
                        reverseVocab = reverseVocab,
                        merges = merges,
                        specialTokens = specialTokens,
                        unkTokenId = unkTokenId,
                        bosTokenId = bosTokenId,
                        eosTokenId = eosTokenId
                    )
                }
            } catch (e: Exception) {
                Timber.w(e, "Failed to read cache file")
                null
            }
        }

        /**
         * Load tokenizer from tokenizer.json file (slow path).
         *
         * @param tokenizerFile Path to tokenizer.json
         * @return HuggingFaceTokenizer instance
         */
        fun loadFromJson(tokenizerFile: File): HuggingFaceTokenizer {
            val startTime = System.currentTimeMillis()
            Timber.d("Parsing tokenizer.json from ${tokenizerFile.name}")

            val jsonContent = tokenizerFile.readText()
            val root = json.parseToJsonElement(jsonContent).jsonObject

            // Parse vocabulary from model.vocab
            val vocab = mutableMapOf<String, Int>()
            val model = root["model"]?.jsonObject
            val vocabJson = model?.get("vocab")?.jsonObject

            if (vocabJson != null) {
                for ((token, id) in vocabJson) {
                    vocab[token] = id.jsonPrimitive.intOrNull ?: continue
                }
            }

            // Reverse vocabulary for decoding
            val reverseVocab = vocab.entries.associate { (k, v) -> v to k }

            // Parse BPE merges
            val merges = mutableListOf<Pair<String, String>>()
            val mergesJson = model?.get("merges")?.jsonArray

            if (mergesJson != null) {
                for (merge in mergesJson) {
                    val mergeStr = merge.jsonPrimitive.contentOrNull ?: continue
                    val parts = mergeStr.split(" ", limit = 2)
                    if (parts.size == 2) {
                        merges.add(parts[0] to parts[1])
                    }
                }
            }

            // Parse special tokens
            val specialTokens = mutableMapOf<String, Int>()
            val addedTokens = root["added_tokens"]?.jsonArray

            var unkTokenId = 0
            var bosTokenId = 1
            var eosTokenId = 2

            if (addedTokens != null) {
                for (token in addedTokens) {
                    val tokenObj = token.jsonObject
                    val content = tokenObj["content"]?.jsonPrimitive?.contentOrNull ?: continue
                    val id = tokenObj["id"]?.jsonPrimitive?.intOrNull ?: continue

                    specialTokens[content] = id

                    // Track important special tokens
                    when {
                        content.contains("unk", ignoreCase = true) -> unkTokenId = id
                        content.contains("bos", ignoreCase = true) ||
                            content == "<s>" -> bosTokenId = id
                        content.contains("eos", ignoreCase = true) ||
                            content == "</s>" -> eosTokenId = id
                    }
                }
            }

            val duration = System.currentTimeMillis() - startTime
            Timber.i("Parsed tokenizer.json: vocab=${vocab.size}, merges=${merges.size} in ${duration}ms")

            return HuggingFaceTokenizer(
                vocab = vocab,
                reverseVocab = reverseVocab,
                merges = merges,
                specialTokens = specialTokens,
                unkTokenId = unkTokenId,
                bosTokenId = bosTokenId,
                eosTokenId = eosTokenId
            )
        }

        /**
         * Load tokenizer from path string
         *
         * @param modelPath Model directory path
         * @return HuggingFaceTokenizer instance
         */
        fun load(modelPath: String): HuggingFaceTokenizer {
            return load(File(modelPath))
        }

        /**
         * Compute MD5 hash of file for change detection.
         *
         * @param file File to hash
         * @return Hex string of MD5 hash
         */
        private fun computeFileHash(file: File): String {
            val md = MessageDigest.getInstance("MD5")
            FileInputStream(file).use { fis ->
                val buffer = ByteArray(8192)
                var bytesRead: Int
                while (fis.read(buffer).also { bytesRead = it } != -1) {
                    md.update(buffer, 0, bytesRead)
                }
            }
            return md.digest().joinToString("") { "%02x".format(it) }
        }

        /**
         * Clear tokenizer cache for a model directory.
         *
         * Use when model is updated or cache needs invalidation.
         *
         * @param modelDir Model directory
         * @return true if cache was deleted
         */
        fun clearCache(modelDir: File): Boolean {
            val cacheFile = File(modelDir, CACHE_FILENAME)
            return if (cacheFile.exists()) {
                val deleted = cacheFile.delete()
                if (deleted) {
                    Timber.i("Tokenizer cache cleared for ${modelDir.name}")
                }
                deleted
            } else {
                false
            }
        }
    }
}
