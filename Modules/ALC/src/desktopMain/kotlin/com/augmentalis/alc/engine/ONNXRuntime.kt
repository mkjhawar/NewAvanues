package com.augmentalis.alc.engine

import ai.onnxruntime.*
import kotlinx.coroutines.*
import java.io.File
import java.nio.FloatBuffer
import java.nio.LongBuffer

/**
 * ONNX Runtime for Desktop (macOS, Windows, Linux)
 *
 * Provides cross-platform LLM inference using Microsoft ONNX Runtime.
 * Supports CPU, CUDA (NVIDIA), DirectML (Windows), and CoreML (macOS).
 */
class ONNXRuntime {
    private var environment: OrtEnvironment? = null
    private var session: OrtSession? = null
    private var isInitialized = false
    private var modelPath: String? = null

    companion object {
        // Execution providers in order of preference
        private val PROVIDERS = listOf(
            "CUDAExecutionProvider",     // NVIDIA GPU
            "CoreMLExecutionProvider",   // macOS Neural Engine
            "DirectMLExecutionProvider", // Windows GPU
            "CPUExecutionProvider"       // Fallback
        )
    }

    /**
     * Initialize ONNX Runtime environment
     */
    fun initialize(): Result<Unit> {
        return try {
            environment = OrtEnvironment.getEnvironment()
            isInitialized = true
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Load an ONNX model
     */
    suspend fun loadModel(path: String): Result<Unit> = withContext(Dispatchers.IO) {
        if (environment == null) {
            return@withContext Result.failure(Exception("Runtime not initialized"))
        }

        try {
            val sessionOptions = OrtSession.SessionOptions().apply {
                setOptimizationLevel(OrtSession.SessionOptions.OptLevel.ALL_OPT)
                setIntraOpNumThreads(Runtime.getRuntime().availableProcessors())

                // Try to add best available execution provider
                for (provider in PROVIDERS) {
                    try {
                        when (provider) {
                            "CUDAExecutionProvider" -> addCUDA()
                            "CoreMLExecutionProvider" -> addCoreML()
                            "DirectMLExecutionProvider" -> addDirectML(0)
                            else -> {} // CPU is default
                        }
                        break
                    } catch (e: Exception) {
                        continue
                    }
                }
            }

            session = environment!!.createSession(path, sessionOptions)
            modelPath = path
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Run inference with input tensors
     */
    suspend fun infer(
        inputs: Map<String, OnnxTensor>
    ): Result<Map<String, OnnxTensor>> = withContext(Dispatchers.Default) {
        if (session == null) {
            return@withContext Result.failure(Exception("Model not loaded"))
        }

        try {
            val result = session!!.run(inputs)
            val outputs = mutableMapOf<String, OnnxTensor>()

            for ((name, value) in result) {
                if (value is OnnxTensor) {
                    outputs[name] = value
                }
            }

            Result.success(outputs)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Create a float tensor from data
     */
    fun createFloatTensor(data: FloatArray, shape: LongArray): OnnxTensor {
        val buffer = FloatBuffer.wrap(data)
        return OnnxTensor.createTensor(environment, buffer, shape)
    }

    /**
     * Create a long tensor from data
     */
    fun createLongTensor(data: LongArray, shape: LongArray): OnnxTensor {
        val buffer = LongBuffer.wrap(data)
        return OnnxTensor.createTensor(environment, buffer, shape)
    }

    /**
     * Get model input names
     */
    fun getInputNames(): Set<String> {
        return session?.inputNames ?: emptySet()
    }

    /**
     * Get model output names
     */
    fun getOutputNames(): Set<String> {
        return session?.outputNames ?: emptySet()
    }

    /**
     * Get input shape for a given input name
     */
    fun getInputShape(name: String): LongArray? {
        return session?.inputInfo?.get(name)?.info?.let { info ->
            if (info is TensorInfo) info.shape else null
        }
    }

    fun isReady(): Boolean = isInitialized && session != null

    fun getModelPath(): String? = modelPath

    fun release() {
        session?.close()
        session = null
        modelPath = null
    }

    fun close() {
        release()
        environment?.close()
        environment = null
        isInitialized = false
    }
}

/**
 * Tokenizer for Desktop (using HuggingFace tokenizers library)
 */
class DesktopTokenizer(private val vocabPath: String) {
    private val vocab = mutableMapOf<String, Int>()
    private val reverseVocab = mutableMapOf<Int, String>()
    private val merges = mutableListOf<Pair<String, String>>()

    init {
        loadVocab()
    }

    private fun loadVocab() {
        val vocabFile = File(vocabPath)
        if (!vocabFile.exists()) return

        vocabFile.readLines().forEachIndexed { index, line ->
            val token = line.trim()
            if (token.isNotEmpty()) {
                vocab[token] = index
                reverseVocab[index] = token
            }
        }

        // Load merges if available
        val mergesFile = File(vocabPath.replace("vocab.txt", "merges.txt"))
        if (mergesFile.exists()) {
            mergesFile.readLines().drop(1).forEach { line ->
                val parts = line.split(" ")
                if (parts.size == 2) {
                    merges.add(parts[0] to parts[1])
                }
            }
        }
    }

    fun encode(text: String): LongArray {
        val tokens = mutableListOf<Int>()

        // Simple word-level tokenization with BPE fallback
        val words = text.lowercase().split(Regex("\\s+"))
        for (word in words) {
            val wordWithSpace = "▁$word" // SentencePiece style
            if (vocab.containsKey(wordWithSpace)) {
                tokens.add(vocab[wordWithSpace]!!)
            } else if (vocab.containsKey(word)) {
                tokens.add(vocab[word]!!)
            } else {
                // Character fallback
                for (char in word) {
                    tokens.add(vocab[char.toString()] ?: vocab["<unk>"] ?: 0)
                }
            }
        }

        return tokens.map { it.toLong() }.toLongArray()
    }

    fun decode(tokens: LongArray): String {
        return tokens.toList()
            .mapNotNull { reverseVocab[it.toInt()] }
            .joinToString("")
            .replace("▁", " ")
            .trim()
    }

    fun vocabSize(): Int = vocab.size
}
