package com.augmentalis.alc.engine

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * TVM (Tensor Virtual Machine) Runtime for Android
 *
 * Provides low-level inference using Apache TVM optimized models.
 * Supports CPU and GPU execution via OpenCL/Vulkan.
 */
class TVMRuntime(
    private val context: Context,
    private val useGPU: Boolean = true
) {
    private var moduleHandle: Long = 0L
    private var graphHandle: Long = 0L
    private var paramsHandle: Long = 0L
    private var isInitialized = false

    companion object {
        init {
            System.loadLibrary("tvm4j_runtime_packed")
        }

        // JNI methods
        @JvmStatic private external fun nativeLoadModule(libPath: String, deviceType: Int): Long
        @JvmStatic private external fun nativeLoadGraph(moduleHandle: Long, graphJson: String): Long
        @JvmStatic private external fun nativeLoadParams(moduleHandle: Long, paramsBytes: ByteArray): Long
        @JvmStatic private external fun nativeSetInput(graphHandle: Long, inputName: String, data: FloatArray): Boolean
        @JvmStatic private external fun nativeRun(graphHandle: Long): Boolean
        @JvmStatic private external fun nativeGetOutput(graphHandle: Long, outputIndex: Int): FloatArray
        @JvmStatic private external fun nativeRelease(moduleHandle: Long, graphHandle: Long, paramsHandle: Long)
        @JvmStatic private external fun nativeGetDeviceType(): Int

        const val DEVICE_CPU = 1
        const val DEVICE_GPU = 2
        const val DEVICE_OPENCL = 4
        const val DEVICE_VULKAN = 7
    }

    /**
     * Initialize TVM runtime with model files
     */
    suspend fun initialize(
        libPath: String,
        graphJsonPath: String,
        paramsPath: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val deviceType = if (useGPU) detectBestDevice() else DEVICE_CPU

            // Load compiled library
            moduleHandle = nativeLoadModule(libPath, deviceType)
            if (moduleHandle == 0L) {
                return@withContext Result.failure(Exception("Failed to load TVM module"))
            }

            // Load graph JSON
            val graphJson = File(graphJsonPath).readText()
            graphHandle = nativeLoadGraph(moduleHandle, graphJson)
            if (graphHandle == 0L) {
                return@withContext Result.failure(Exception("Failed to load graph"))
            }

            // Load parameters
            val paramsBytes = File(paramsPath).readBytes()
            paramsHandle = nativeLoadParams(moduleHandle, paramsBytes)
            if (paramsHandle == 0L) {
                return@withContext Result.failure(Exception("Failed to load parameters"))
            }

            isInitialized = true
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Run inference with input data
     */
    suspend fun infer(inputName: String, inputData: FloatArray): Result<FloatArray> =
        withContext(Dispatchers.Default) {
            if (!isInitialized) {
                return@withContext Result.failure(Exception("TVM runtime not initialized"))
            }

            try {
                // Set input
                if (!nativeSetInput(graphHandle, inputName, inputData)) {
                    return@withContext Result.failure(Exception("Failed to set input"))
                }

                // Run inference
                if (!nativeRun(graphHandle)) {
                    return@withContext Result.failure(Exception("Inference failed"))
                }

                // Get output
                val output = nativeGetOutput(graphHandle, 0)
                Result.success(output)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    /**
     * Run inference with multiple inputs
     */
    suspend fun inferMultiple(
        inputs: Map<String, FloatArray>
    ): Result<List<FloatArray>> = withContext(Dispatchers.Default) {
        if (!isInitialized) {
            return@withContext Result.failure(Exception("TVM runtime not initialized"))
        }

        try {
            // Set all inputs
            inputs.forEach { (name, data) ->
                if (!nativeSetInput(graphHandle, name, data)) {
                    return@withContext Result.failure(Exception("Failed to set input: $name"))
                }
            }

            // Run inference
            if (!nativeRun(graphHandle)) {
                return@withContext Result.failure(Exception("Inference failed"))
            }

            // Get outputs (assuming 1 output for now, extend as needed)
            val outputs = listOf(nativeGetOutput(graphHandle, 0))
            Result.success(outputs)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun detectBestDevice(): Int {
        return try {
            val type = nativeGetDeviceType()
            when {
                type == DEVICE_VULKAN -> DEVICE_VULKAN
                type == DEVICE_OPENCL -> DEVICE_OPENCL
                else -> DEVICE_CPU
            }
        } catch (e: Exception) {
            DEVICE_CPU
        }
    }

    fun isReady(): Boolean = isInitialized

    fun release() {
        if (isInitialized) {
            nativeRelease(moduleHandle, graphHandle, paramsHandle)
            moduleHandle = 0L
            graphHandle = 0L
            paramsHandle = 0L
            isInitialized = false
        }
    }
}

/**
 * Tokenizer for TVM LLM models
 */
class TVMTokenizer(private val vocabPath: String) {
    private val vocab = mutableMapOf<String, Int>()
    private val reverseVocab = mutableMapOf<Int, String>()

    init {
        loadVocab()
    }

    private fun loadVocab() {
        File(vocabPath).readLines().forEachIndexed { index, line ->
            val token = line.trim()
            vocab[token] = index
            reverseVocab[index] = token
        }
    }

    fun encode(text: String): IntArray {
        // Simple whitespace tokenization with BPE fallback
        val tokens = mutableListOf<Int>()
        val words = text.split(Regex("\\s+"))

        for (word in words) {
            if (vocab.containsKey(word)) {
                tokens.add(vocab[word]!!)
            } else {
                // Character-level fallback
                for (char in word) {
                    val charStr = char.toString()
                    tokens.add(vocab[charStr] ?: vocab["<unk>"] ?: 0)
                }
            }
        }

        return tokens.toIntArray()
    }

    fun decode(tokens: IntArray): String {
        return tokens.toList()
            .mapNotNull { reverseVocab[it] }
            .joinToString("")
            .replace("▁", " ")
            .trim()
    }

    fun vocabSize(): Int = vocab.size
}
