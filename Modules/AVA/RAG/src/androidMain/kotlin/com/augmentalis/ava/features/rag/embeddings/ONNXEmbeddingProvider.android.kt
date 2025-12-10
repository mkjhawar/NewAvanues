// filename: Universal/AVA/Features/RAG/src/androidMain/kotlin/com/augmentalis/ava/features/rag/embeddings/ONNXEmbeddingProvider.android.kt
// created: 2025-11-04
// author: AVA AI Team
// © Augmentalis Inc, Intelligent Devices LLC

package com.augmentalis.ava.features.rag.embeddings

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import android.content.Context
import com.augmentalis.ava.core.common.AVAException
import com.augmentalis.ava.features.rag.domain.Embedding
import java.io.File
import java.nio.LongBuffer

/**
 * ONNX Runtime embedding provider for Android
 *
 * Uses all-MiniLM-L6-v2 ONNX model for generating embeddings.
 *
 * ## AVA File Format Convention
 * .AON (AVA ONNX Naming) is the AVA standard extension for ONNX models.
 * AON files are identical to ONNX files - same format, different extension.
 * This provides:
 * - Proprietary branding for AVA models
 * - Clear distinction from third-party ONNX files
 * - Backward compatibility with .onnx files is maintained
 *
 * Model Loading Priority:
 * 1. External storage: /sdcard/Android/data/{package}/files/models/{modelId}.AON
 * 2. Fallback to legacy .onnx extension for backward compatibility
 * 3. Downloaded via ModelDownloadManager
 * 4. Bundled in assets (legacy, not recommended - adds 90MB to APK)
 *
 * IMPORTANT: To keep APK size small (~30MB), users should place models in external storage.
 * See docs/MODEL-SETUP.md for download and setup instructions.
 *
 * NOTE Phase 2: Uses simplified tokenization. Phase 3 will add proper
 * BERT WordPiece tokenization using ONNX Runtime Extensions.
 */
class ONNXEmbeddingProvider(
    private val context: Context,
    private val modelPath: String? = null,
    private val modelId: String = "AVA-384-Base-INT8"  // Default: bundled quantized model
) : EmbeddingProvider {

    override val name = "ONNX ($modelId)"
    override val dimension = 384

    // Model ID mapping: AVA names → original model names
    // New naming: AVA-{DIM}-{TYPE}[-QUANT] (e.g., AVA-384-Base-INT8)
    // Extension: .AON (AVA ONNX Model)
    private val modelIdMap = mapOf(
        // English-only models (FP32)
        "AVA-384-Base" to "all-MiniLM-L6-v2",
        "AVA-384-Fast" to "paraphrase-MiniLM-L3-v2",
        "AVA-768-Qual" to "all-mpnet-base-v2",

        // English-only models (Quantized)
        "AVA-384-Base-INT8" to "all-MiniLM-L6-v2",
        "AVA-384-Base-FP16" to "all-MiniLM-L6-v2",
        "AVA-384-Fast-INT8" to "paraphrase-MiniLM-L3-v2",
        "AVA-768-Qual-INT8" to "all-mpnet-base-v2",

        // Multilingual models (50+ languages, FP32)
        "AVA-384-Multi" to "paraphrase-multilingual-MiniLM-L12-v2",
        "AVA-768-Multi" to "paraphrase-multilingual-mpnet-base-v2",
        "AVA-512-Multi" to "distiluse-base-multilingual-cased-v2",

        // Multilingual models (Quantized)
        "AVA-384-Multi-INT8" to "paraphrase-multilingual-MiniLM-L12-v2",
        "AVA-384-Multi-FP16" to "paraphrase-multilingual-MiniLM-L12-v2",
        "AVA-768-Multi-INT8" to "paraphrase-multilingual-mpnet-base-v2",
        "AVA-512-Multi-INT8" to "distiluse-base-multilingual-cased-v2",

        // Language-specific models (FP32)
        "AVA-384-ZH" to "sbert-chinese-general-v2",
        "AVA-768-JA" to "sentence-bert-base-ja-mean-tokens-v2",

        // Language-specific models (Quantized)
        "AVA-384-ZH-INT8" to "sbert-chinese-general-v2",
        "AVA-768-JA-INT8" to "sentence-bert-base-ja-mean-tokens-v2",

        // Legacy names (backward compatibility)
        "AVA-ONX-384-BASE-INT8" to "all-MiniLM-L6-v2",
        "AVA-ONX-384-MULTI-INT8" to "paraphrase-multilingual-MiniLM-L12-v2"
    )

    private var ortEnvironment: OrtEnvironment? = null
    private var ortSession: OrtSession? = null
    private var initialized = false

    /**
     * Initialize ONNX Runtime session
     */
    private fun initialize() {
        if (initialized) return

        try {
            // Create ONNX Runtime environment
            ortEnvironment = OrtEnvironment.getEnvironment()

            // Load model from assets
            val modelFile = loadModelFromAssets()

            // Create session options
            val sessionOptions = OrtSession.SessionOptions()

            // Create inference session
            ortSession = ortEnvironment!!.createSession(
                modelFile.absolutePath,
                sessionOptions
            )

            initialized = true
        } catch (e: Exception) {
            throw AVAException.InitializationException("Failed to initialize ONNX Runtime: ${e.message}", e)
        }
    }

    /**
     * Load ONNX model with AON wrapper support
     *
     * Priority (Updated 2025-11-23):
     * 1. Unified model repository: /sdcard/ava-ai-models/embeddings/ - PRIMARY
     *    - Single source of truth for all AVA models
     *    - Mirrors local development structure
     *    - Supports both .AON (secured) and .onnx (legacy) formats
     * 2. Bundled assets (app-included models for fallback)
     *    - AVA-384-Base-INT8.AON bundled for immediate English support
     * 3. Legacy app-specific: /sdcard/Android/data/{package}/files/models/
     *    - Backward compatibility with old installations
     * 4. Use provided modelPath (absolute path)
     * 5. Check ModelDownloadManager for downloaded models
     *
     * ## AON File Security (v1.0):
     * - AON files have AVA-specific authentication header
     * - Prevents unauthorized use (third-party ONNX loaders fail with "Invalid format")
     * - Package name verification (only authorized apps can unwrap)
     * - Optional expiry timestamp and license tier enforcement
     * - HMAC-SHA256 signature + SHA256 integrity checks
     */
    private fun loadModelFromAssets(): File {
        // Option 0: Check unified model repository FIRST
        val unifiedEmbeddingsDir = File("/sdcard/ava-ai-models/embeddings")
        val unifiedModelFile = File(unifiedEmbeddingsDir, "$modelId.AON")
        if (unifiedModelFile.exists()) {
            return unwrapIfNeeded(unifiedModelFile)
        }

        // Also check legacy .onnx in unified repo
        val unifiedOnnxFile = File(unifiedEmbeddingsDir, "$modelId.onnx")
        if (unifiedOnnxFile.exists()) {
            return unifiedOnnxFile
        }

        // Option 1: Check bundled assets FIRST (instant use, no download needed)
        // Try new .AON extension first, then legacy .onnx
        val assetPathAON = "models/$modelId.AON"
        val assetPathOnnx = "models/$modelId.onnx"
        val cacheDir = context.cacheDir
        val cachedModelFile = File(cacheDir, "$modelId.onnx")  // Cache as .onnx (unwrapped)

        // Check if already extracted and unwrapped from assets to cache
        if (cachedModelFile.exists()) {
            return cachedModelFile
        }

        // Try to extract from assets (bundled models) - try .AON first
        try {
            val tempAONFile = File(cacheDir, "$modelId.aon.temp")
            context.assets.open(assetPathAON).use { input ->
                tempAONFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }

            // Check if it's an AON file and unwrap it
            if (AONFileManager.isAONFile(tempAONFile)) {
                val onnxData = AONFileManager.unwrapAON(tempAONFile, context)
                cachedModelFile.writeBytes(onnxData)
                tempAONFile.delete()  // Clean up temp file
                return cachedModelFile
            } else {
                // It's a legacy .onnx file with .AON extension, just rename
                tempAONFile.renameTo(cachedModelFile)
                return cachedModelFile
            }
        } catch (e: Exception) {
            // Try legacy .onnx extension
            try {
                context.assets.open(assetPathOnnx).use { input ->
                    cachedModelFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                return cachedModelFile
            } catch (e2: Exception) {
                // Model not bundled in assets, continue to other sources
            }
        }

        // Option 2: Check external storage (user-placed or downloaded)
        val externalModelsDir = File(context.getExternalFilesDir(null), "models").apply {
            if (!exists()) {
                mkdirs()
                // Create .nomedia file to hide folder from media scanners
                File(this, ".nomedia").createNewFile()
            }
        }

        // Check for AVA filename with new .AON extension first
        val avaModelFileAON = File(externalModelsDir, "$modelId.AON")
        if (avaModelFileAON.exists()) {
            return unwrapIfNeeded(avaModelFileAON)
        }

        // Check legacy .onnx extension
        val avaModelFileOnnx = File(externalModelsDir, "$modelId.onnx")
        if (avaModelFileOnnx.exists()) {
            return avaModelFileOnnx
        }

        // Fall back to original filename for backward compatibility
        val originalModelId = modelIdMap[modelId] ?: modelId
        val originalModelFileAON = File(externalModelsDir, "$originalModelId.AON")
        if (originalModelFileAON.exists()) {
            return unwrapIfNeeded(originalModelFileAON)
        }
        val originalModelFileOnnx = File(externalModelsDir, "$originalModelId.onnx")
        if (originalModelFileOnnx.exists()) {
            return originalModelFileOnnx
        }

        // Option 3: Use provided path (if specified)
        if (modelPath != null) {
            val file = File(modelPath)
            if (file.exists()) {
                return unwrapIfNeeded(file)
            }
        }

        // Option 4: Check ModelDownloadManager
        val downloadManager = AndroidModelDownloadManager(context)
        val downloadedPath = kotlinx.coroutines.runBlocking {
            downloadManager.getModelPath(modelId)
        }

        if (downloadedPath != null) {
            val file = File(downloadedPath)
            if (file.exists()) {
                return unwrapIfNeeded(file)
            }
        }

        // No model found anywhere
        val fallbackModelId = modelIdMap[modelId] ?: modelId
        throw AVAException.ResourceNotFoundException(
            "Model '$modelId' not found.\n\n" +
            "Bundled models: AVA-384-Base-INT8 (English)\n\n" +
            "For other models, download and place in external storage:\n" +
            "Location: /sdcard/Android/data/com.augmentalis.ava/files/models/$modelId.AON\n\n" +
            "Download instructions in Settings → Developer Settings → RAG Embedding Model\n" +
            "Or see docs/AVA-MODEL-NAMING-REGISTRY.md for complete model list."
        )
    }

    /**
     * Unwrap AON file if needed, otherwise return original file
     *
     * Checks if file is AON format and unwraps to temp .onnx file.
     * If file is already ONNX, returns it directly.
     *
     * @param file File to check (could be .aon or .onnx)
     * @return ONNX file ready for ONNX Runtime
     */
    private fun unwrapIfNeeded(file: File): File {
        // Check if it's an AON file
        if (!AONFileManager.isAONFile(file)) {
            // It's a standard ONNX file, use directly
            return file
        }

        // It's an AON file - unwrap to cache directory
        val cacheDir = context.cacheDir
        val unwrappedFile = File(cacheDir, "${file.nameWithoutExtension}.onnx")

        // Check if already unwrapped and cached
        if (unwrappedFile.exists() &&
            unwrappedFile.lastModified() >= file.lastModified()) {
            return unwrappedFile
        }

        // Unwrap AON file (includes authentication and integrity checks)
        try {
            val onnxData = AONFileManager.unwrapAON(file, context)
            unwrappedFile.writeBytes(onnxData)
            return unwrappedFile
        } catch (e: java.lang.SecurityException) {
            throw AVAException.SecurityException(
                "AON file authentication failed: ${e.message}\n\n" +
                "This model is not authorized for this app.\n" +
                "Package: ${context.packageName}\n" +
                "Model: ${file.name}",
                e
            )
        } catch (e: Exception) {
            throw AVAException.ResourceNotFoundException(
                "Failed to unwrap AON file: ${e.message}\n" +
                "File may be corrupted or expired: ${file.name}",
                e
            )
        }
    }

    override suspend fun isAvailable(): Boolean {
        return try {
            // Check bundled assets FIRST (instant availability) - try .AON then .onnx
            val assetPathAON = "models/$modelId.AON"
            val assetPathOnnx = "models/$modelId.onnx"
            try {
                context.assets.open(assetPathAON).use { it.available() > 0 }
                return true
            } catch (e: Exception) {
                try {
                    context.assets.open(assetPathOnnx).use { it.available() > 0 }
                    return true
                } catch (e2: Exception) {
                    // Not bundled, check other sources
                }
            }

            // Check external storage (user-placed or downloaded)
            val externalModelsDir = File(context.getExternalFilesDir(null), "models").apply {
                if (!exists()) {
                    mkdirs()
                    // Create .nomedia file to hide folder from media scanners
                    File(this, ".nomedia").createNewFile()
                }
            }
            // Check .AON extension first, then .onnx
            val externalModelFileAON = File(externalModelsDir, "$modelId.AON")
            val externalModelFileOnnx = File(externalModelsDir, "$modelId.onnx")
            if (externalModelFileAON.exists() || externalModelFileOnnx.exists()) {
                return true
            }

            // Check if model is downloaded via ModelDownloadManager
            val downloadManager = AndroidModelDownloadManager(context)
            val isDownloaded = downloadManager.isModelAvailable(modelId)

            isDownloaded
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun embed(text: String): Result<Embedding.Float32> {
        return try {
            if (!initialized) initialize()

            // Tokenize input
            val tokenized = SimpleTokenizer.tokenize(text)

            // Run inference
            val embedding = runInference(
                inputIds = tokenized.inputIds,
                attentionMask = tokenized.attentionMask
            )

            Result.success(Embedding.Float32(embedding))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun embedBatch(texts: List<String>): Result<List<Embedding.Float32>> {
        return try {
            if (!initialized) initialize()

            // Batch size for processing (avoid OOM on large batches)
            val batchSize = 32
            val allEmbeddings = mutableListOf<Embedding.Float32>()

            // Process in chunks
            texts.chunked(batchSize).forEach { batch ->
                val batchResult = embedSingleBatch(batch)
                if (batchResult.isFailure) {
                    return batchResult.map { emptyList() } // Convert to List type
                } else {
                    allEmbeddings.addAll(batchResult.getOrThrow())
                }
            }

            Result.success(allEmbeddings)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Process a single batch of texts through ONNX inference
     *
     * This is the core batch processing method that achieves 20x speedup
     * by running ONNX inference once for all texts instead of once per text.
     *
     * @param texts Batch of texts to embed (should be <= 32 for memory safety)
     * @return List of embeddings in same order as input texts
     */
    private suspend fun embedSingleBatch(
        texts: List<String>
    ): Result<List<Embedding.Float32>> {
        var inputIdsTensor: OnnxTensor? = null
        var attentionMaskTensor: OnnxTensor? = null

        return try {
            val env = ortEnvironment ?: throw AVAException.InitializationException("ONNX Runtime not initialized")
            val session = ortSession ?: throw AVAException.InitializationException("ONNX session not created")

            // Tokenize all texts
            val tokenizedInputs = texts.map { SimpleTokenizer.tokenize(it) }
            val batchSize = texts.size
            val seqLength = 128 // MAX_SEQ_LENGTH from SimpleTokenizer

            // Stack tokenized inputs into batch tensors
            val batchInputIds = LongArray(batchSize * seqLength)
            val batchAttentionMask = LongArray(batchSize * seqLength)

            tokenizedInputs.forEachIndexed { batchIdx, tokenized ->
                val offset = batchIdx * seqLength
                tokenized.inputIds.copyInto(batchInputIds, offset)
                tokenized.attentionMask.copyInto(batchAttentionMask, offset)
            }

            // Create batch tensors with shape [batchSize, seqLength]
            inputIdsTensor = OnnxTensor.createTensor(
                env,
                java.nio.LongBuffer.wrap(batchInputIds),
                longArrayOf(batchSize.toLong(), seqLength.toLong())
            )

            attentionMaskTensor = OnnxTensor.createTensor(
                env,
                java.nio.LongBuffer.wrap(batchAttentionMask),
                longArrayOf(batchSize.toLong(), seqLength.toLong())
            )

            // Run batch inference (single ONNX call for all texts!)
            val inputs = mapOf(
                "input_ids" to inputIdsTensor,
                "attention_mask" to attentionMaskTensor
            )

            val outputs = session.run(inputs)

            try {
                // Extract batch embeddings
                val outputTensor = outputs[0] as? OnnxTensor
                    ?: throw AVAException.ModelNotLoadedException("Invalid output tensor")

                val embeddings = extractBatchEmbeddings(
                    outputTensor,
                    batchSize,
                    seqLength,
                    tokenizedInputs.map { it.attentionMask }
                )

                Result.success(embeddings)
            } finally {
                outputs.forEach { it.value.close() }
            }
        } catch (e: Exception) {
            android.util.Log.e("ONNXEmbeddingProvider", "Batch inference failed", e)
            Result.failure(e)
        } finally {
            // Resource cleanup (critical for preventing memory leaks)
            inputIdsTensor?.close()
            attentionMaskTensor?.close()
        }
    }

    /**
     * Extract embeddings from ONNX batch output
     *
     * Processes output tensor with shape [batchSize, seqLength, hiddenSize]
     * and applies mean pooling to each batch item to get sentence embeddings.
     *
     * @param outputTensor ONNX output tensor from batch inference
     * @param batchSize Number of texts in batch
     * @param seqLength Sequence length (128)
     * @param attentionMasks Attention masks for each text in batch
     * @return List of normalized embeddings
     */
    private fun extractBatchEmbeddings(
        outputTensor: OnnxTensor,
        batchSize: Int,
        seqLength: Int,
        attentionMasks: List<LongArray>
    ): List<Embedding.Float32> {
        val hiddenStates = outputTensor.floatBuffer.array()
        val embeddingDim = dimension // 384 for all-MiniLM-L6-v2
        val embeddings = mutableListOf<Embedding.Float32>()

        for (batchIdx in 0 until batchSize) {
            // Extract hidden states for this batch item
            val offset = batchIdx * seqLength * embeddingDim
            val itemHiddenStates = hiddenStates.sliceArray(offset until offset + seqLength * embeddingDim)

            // Apply mean pooling with attention mask
            val pooled = meanPooling(
                itemHiddenStates,
                attentionMasks[batchIdx],
                embeddingDim
            )

            embeddings.add(Embedding.Float32(pooled))
        }

        return embeddings
    }

    override fun estimateTimeMs(count: Int): Long {
        // Updated estimate with batch processing: ~0.5ms per embedding with batching
        // (20x faster than sequential 10ms per embedding)
        return (count * 0.5).toLong().coerceAtLeast(1)
    }

    /**
     * Generate embedding and quantize to INT8
     *
     * Phase 3.1: INT8 quantization for 75% storage reduction
     *
     * @param text Text to embed
     * @return Quantized embedding or error
     */
    suspend fun embedWithQuantization(text: String): Result<QuantizedEmbedding> {
        return try {
            val result = embed(text).getOrThrow()
            val floatEmbedding = result.values
            val quantized = Quantization.quantizeToInt8(floatEmbedding)
            Result.success(quantized)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Generate embeddings for batch and quantize to INT8
     *
     * @param texts Texts to embed
     * @return List of quantized embeddings or error
     */
    suspend fun embedBatchWithQuantization(texts: List<String>): Result<List<QuantizedEmbedding>> {
        return try {
            val embeddings = embedBatch(texts).getOrThrow()
            val quantizedList = embeddings.map { embedding ->
                Quantization.quantizeToInt8(embedding.values)
            }
            Result.success(quantizedList)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Run ONNX inference
     *
     * @param inputIds Token IDs (shape: [1, 128])
     * @param attentionMask Attention mask (shape: [1, 128])
     * @return Embedding vector (384 dimensions)
     */
    private fun runInference(
        inputIds: LongArray,
        attentionMask: LongArray
    ): FloatArray {
        val env = ortEnvironment ?: throw AVAException.InitializationException("ONNX Runtime not initialized")
        val session = ortSession ?: throw AVAException.InitializationException("ONNX session not created")

        // Create input tensors
        val inputIdsTensor = OnnxTensor.createTensor(
            env,
            LongBuffer.wrap(inputIds),
            longArrayOf(1, inputIds.size.toLong())
        )

        val attentionMaskTensor = OnnxTensor.createTensor(
            env,
            LongBuffer.wrap(attentionMask),
            longArrayOf(1, attentionMask.size.toLong())
        )

        try {
            // Create input map
            val inputs = mapOf(
                "input_ids" to inputIdsTensor,
                "attention_mask" to attentionMaskTensor
            )

            // Run inference
            val outputs = session.run(inputs)

            try {
                // Get output tensor
                // Model outputs: last_hidden_state (batch_size, sequence_length, hidden_size)
                val outputTensor = outputs[0] as? OnnxTensor
                    ?: throw AVAException.ModelNotLoadedException("Invalid output tensor")

                // Extract embeddings
                // For sentence embeddings, we use mean pooling over the sequence
                val hiddenStates = outputTensor.floatBuffer.array()

                // Mean pooling: average all token embeddings (considering attention mask)
                return meanPooling(hiddenStates, attentionMask, dimension)
            } finally {
                outputs.forEach { it.value.close() }
            }
        } finally {
            inputIdsTensor.close()
            attentionMaskTensor.close()
        }
    }

    /**
     * Mean pooling over token embeddings
     *
     * Averages token embeddings, weighted by attention mask
     *
     * @param hiddenStates All token embeddings (sequence_length * hidden_size)
     * @param attentionMask Mask indicating real vs padding tokens
     * @param embeddingDim Embedding dimension (384)
     * @return Pooled sentence embedding
     */
    private fun meanPooling(
        hiddenStates: FloatArray,
        attentionMask: LongArray,
        embeddingDim: Int
    ): FloatArray {
        val result = FloatArray(embeddingDim) { 0f }
        var tokenCount = 0

        // Sum embeddings for real tokens (where mask = 1)
        for (tokenIdx in attentionMask.indices) {
            if (attentionMask[tokenIdx] == 1L) {
                for (dim in 0 until embeddingDim) {
                    val hiddenIdx = tokenIdx * embeddingDim + dim
                    if (hiddenIdx < hiddenStates.size) {
                        result[dim] += hiddenStates[hiddenIdx]
                    }
                }
                tokenCount++
            }
        }

        // Average
        if (tokenCount > 0) {
            for (dim in result.indices) {
                result[dim] /= tokenCount.toFloat()
            }
        }

        // Normalize (L2 normalization for cosine similarity)
        val norm = kotlin.math.sqrt(result.sumOf { (it * it).toDouble() }).toFloat()
        if (norm > 0) {
            for (i in result.indices) {
                result[i] /= norm
            }
        }

        return result
    }

    /**
     * Clean up resources
     */
    fun close() {
        ortSession?.close()
        ortSession = null
        initialized = false
    }
}
