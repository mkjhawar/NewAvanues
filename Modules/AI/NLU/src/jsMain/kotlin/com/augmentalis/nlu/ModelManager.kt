package com.augmentalis.nlu

import com.augmentalis.ava.core.common.Result
import com.augmentalis.crypto.aon.AONCodec
import com.augmentalis.nlu.locale.LocaleManager
import kotlinx.coroutines.await
import org.khronos.webgl.ArrayBuffer
import org.khronos.webgl.Uint8Array
import org.w3c.dom.events.Event
import org.w3c.fetch.Response
import kotlin.js.Promise

/**
 * JS/Web implementation of ModelManager
 *
 * Manages dual-model download, IndexedDB caching, and model selection:
 * - MobileBERT (384-dim, ~25MB) for English locales
 * - mALBERT (768-dim, ~41MB) for all other locales (52 languages)
 *
 * Storage: IndexedDB "ava_nlu_models" object store
 * Download: Fetch API with progress tracking from HuggingFace CDN
 *
 * Model switching: call switchModel() when locale changes
 */
actual class ModelManager {

    private val localeManager = LocaleManager()

    // Active model type determined by locale
    private var activeModelType: ModelType = detectModelTypeForLocale()

    // In-memory cache of downloaded model data
    private var cachedModelBuffer: ArrayBuffer? = null
    private var cachedVocabText: String? = null
    private var modelAvailable = false

    // IndexedDB database name and store
    private companion object {
        const val DB_NAME = "ava_nlu_models"
        const val DB_VERSION = 1
        const val STORE_NAME = "models"

        // AVA Model CDN URLs (AON-wrapped ONNX files)
        const val MOBILEBERT_MODEL_URL = "https://huggingface.co/AvanueAI/ava-nlu/resolve/main/AVA-384-Base-INT8.AON"
        const val MOBILEBERT_VOCAB_URL = "https://huggingface.co/AvanueAI/ava-nlu/resolve/main/AVA-384-Base-vocab.txt"
        const val MALBERT_MODEL_URL = "https://huggingface.co/AvanueAI/ava-nlu/resolve/main/AVA-768-Base-INT8.AON"
        const val MALBERT_VOCAB_URL = "https://huggingface.co/AvanueAI/ava-nlu/resolve/main/AVA-768-Base-vocab.txt"

    }

    /**
     * Check if the active model is available (cached in IndexedDB or in-memory)
     */
    actual fun isModelAvailable(): Boolean = modelAvailable

    /**
     * Get IndexedDB key for the active model file
     */
    actual fun getModelPath(): String = activeModelType.modelFileName

    /**
     * Get IndexedDB key for the active vocab file
     */
    actual fun getVocabPath(): String = activeModelType.vocabFileName

    /**
     * Download the active model if not already cached
     *
     * @param onProgress Callback for download progress (0.0 to 1.0)
     * @return Result indicating success or failure
     */
    actual suspend fun downloadModelsIfNeeded(
        onProgress: (Float) -> Unit
    ): Result<Unit> {
        return try {
            // Check IndexedDB cache first
            val cachedModel = getFromIndexedDB(activeModelType.modelFileName)
            val cachedVocab = getFromIndexedDB(getVocabPath())

            if (cachedModel != null && cachedVocab != null) {
                cachedModelBuffer = cachedModel
                cachedVocabText = decodeArrayBufferToString(cachedVocab)
                modelAvailable = true
                console.log("[ModelManager] Loaded ${activeModelType.displayName} from IndexedDB cache")
                onProgress(1.0f)
                return Result.Success(Unit)
            }

            console.log("[ModelManager] Downloading ${activeModelType.displayName}...")
            onProgress(0.05f)

            // Determine URLs based on active model
            val modelUrl = when (activeModelType) {
                ModelType.MOBILEBERT -> MOBILEBERT_MODEL_URL
                ModelType.MALBERT -> MALBERT_MODEL_URL
            }
            val vocabUrl = when (activeModelType) {
                ModelType.MOBILEBERT -> MOBILEBERT_VOCAB_URL
                ModelType.MALBERT -> MALBERT_VOCAB_URL
            }

            // Download model (90% of progress)
            val modelResponse: Response = kotlinx.browser.window.fetch(modelUrl).await()
            if (!modelResponse.ok) {
                return Result.Error(
                    exception = IllegalStateException("HTTP ${modelResponse.status}"),
                    message = "Failed to download model: HTTP ${modelResponse.status}"
                )
            }
            onProgress(0.1f)

            val rawModelBuffer = modelResponse.arrayBuffer().await()
            onProgress(0.80f)

            // Unwrap AON with full HMAC-SHA256 verification + integrity checks
            val rawBytes = arrayBufferToByteArray(rawModelBuffer)
            val onnxBytes = AONCodec.unwrap(rawBytes)
            val modelBuffer = byteArrayToArrayBuffer(onnxBytes)
            onProgress(0.85f)

            // Download vocabulary (10% of progress)
            val vocabResponse: Response = kotlinx.browser.window.fetch(vocabUrl).await()
            if (!vocabResponse.ok) {
                return Result.Error(
                    exception = IllegalStateException("HTTP ${vocabResponse.status}"),
                    message = "Failed to download vocabulary: HTTP ${vocabResponse.status}"
                )
            }

            val vocabBuffer = vocabResponse.arrayBuffer().await()
            onProgress(0.95f)

            // Store unwrapped ONNX data in IndexedDB for persistent caching
            putToIndexedDB(activeModelType.modelFileName, modelBuffer)
            putToIndexedDB(getVocabPath(), vocabBuffer)

            // Keep in-memory references
            cachedModelBuffer = modelBuffer
            cachedVocabText = decodeArrayBufferToString(vocabBuffer)
            modelAvailable = true

            onProgress(1.0f)
            console.log("[ModelManager] Download complete: ${activeModelType.displayName}")
            Result.Success(Unit)
        } catch (e: Exception) {
            console.error("[ModelManager] Download failed: ${e.message}")
            Result.Error(
                exception = e,
                message = "Failed to download models: ${e.message}"
            )
        }
    }

    /**
     * Copy model from bundled assets — not applicable for JS/Web
     * Web models are always fetched from CDN and cached in IndexedDB
     *
     * @return Result.Success (no-op for web)
     */
    actual suspend fun copyModelFromAssets(): Result<Unit> {
        // On the web, models are fetched from CDN, not bundled assets
        // If the model is already cached in IndexedDB, consider it "copied"
        return if (modelAvailable) {
            Result.Success(Unit)
        } else {
            downloadModelsIfNeeded()
        }
    }

    /**
     * Delete cached models from IndexedDB
     */
    actual fun clearModels(): Result<Unit> {
        return try {
            // Clear both model types from IndexedDB
            for (type in ModelType.entries) {
                deleteFromIndexedDB(type.modelFileName)
                deleteFromIndexedDB(type.vocabFileName)
            }
            cachedModelBuffer = null
            cachedVocabText = null
            modelAvailable = false
            console.log("[ModelManager] Cleared all cached models")
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(
                exception = e,
                message = "Failed to clear models: ${e.message}"
            )
        }
    }

    /**
     * Get approximate size of cached models
     * Returns 0 if not available (IndexedDB doesn't easily report sizes)
     */
    actual fun getModelsSize(): Long {
        val modelSize = cachedModelBuffer?.byteLength ?: 0
        // Estimate vocab at ~1MB
        val vocabSize = cachedVocabText?.length?.toLong() ?: 0L
        return modelSize.toLong() + vocabSize
    }

    // ─── JS-Specific Public API ─────────────────────────────────

    /**
     * Get the currently active model type
     */
    fun getActiveModelType(): ModelType = activeModelType

    /**
     * Get the embedding dimension for the active model
     */
    fun getEmbeddingDimension(): Int = activeModelType.embeddingDimension

    /**
     * Get the ONNX model as an ArrayBuffer for creating an ONNX session
     */
    fun getModelArrayBuffer(): ArrayBuffer? = cachedModelBuffer

    /**
     * Get the vocabulary text for the tokenizer
     */
    fun getVocabText(): String? = cachedVocabText

    /**
     * Switch to a different model (e.g., when locale changes)
     *
     * This clears the current in-memory cache and sets the new active model.
     * Call downloadModelsIfNeeded() after switching to load the new model.
     *
     * @param modelType The new model type to use
     */
    suspend fun switchModel(modelType: ModelType) {
        if (modelType == activeModelType && modelAvailable) return

        console.log("[ModelManager] Switching from ${activeModelType.displayName} to ${modelType.displayName}")
        activeModelType = modelType
        cachedModelBuffer = null
        cachedVocabText = null
        modelAvailable = false

        // Try to load from IndexedDB cache
        val cachedModel = getFromIndexedDB(modelType.modelFileName)
        val cachedVocab = getFromIndexedDB(getVocabPath())

        if (cachedModel != null && cachedVocab != null) {
            cachedModelBuffer = cachedModel
            cachedVocabText = decodeArrayBufferToString(cachedVocab)
            modelAvailable = true
            console.log("[ModelManager] Loaded ${modelType.displayName} from IndexedDB cache")
        }
    }

    // ─── IndexedDB Operations ───────────────────────────────────

    /**
     * Open the IndexedDB database
     */
    private fun openDatabase(): Promise<dynamic> {
        return Promise { resolve, reject ->
            val request = kotlinx.browser.window.asDynamic().indexedDB.open(DB_NAME, DB_VERSION)
            request.onupgradeneeded = { _: Event ->
                val db = request.result
                if (!db.objectStoreNames.contains(STORE_NAME)) {
                    db.createObjectStore(STORE_NAME)
                }
            }
            request.onsuccess = { _: Event -> resolve(request.result) }
            request.onerror = { _: Event -> reject(Exception("Failed to open IndexedDB: ${request.error}")) }
        }
    }

    /**
     * Get a value from IndexedDB by key
     */
    private suspend fun getFromIndexedDB(key: String): ArrayBuffer? {
        return try {
            val db = openDatabase().await()
            val result = Promise<ArrayBuffer?> { resolve, reject ->
                val transaction = db.transaction(STORE_NAME, "readonly")
                val store = transaction.objectStore(STORE_NAME)
                val request = store.get(key)
                request.onsuccess = { _: Event ->
                    val value = request.result
                    resolve(value?.unsafeCast<ArrayBuffer>())
                }
                request.onerror = { _: Event -> reject(Exception("IndexedDB get failed: ${request.error}")) }
            }.await()
            db.close()
            result
        } catch (e: Exception) {
            console.warn("[ModelManager] IndexedDB get failed for $key: ${e.message}")
            null
        }
    }

    /**
     * Store a value in IndexedDB
     */
    private suspend fun putToIndexedDB(key: String, value: ArrayBuffer) {
        try {
            val db = openDatabase().await()
            Promise<Unit> { resolve, reject ->
                val transaction = db.transaction(STORE_NAME, "readwrite")
                val store = transaction.objectStore(STORE_NAME)
                val request = store.put(value, key)
                request.onsuccess = { _: Event -> resolve(Unit) }
                request.onerror = { _: Event -> reject(Exception("IndexedDB put failed: ${request.error}")) }
            }.await()
            db.close()
        } catch (e: Exception) {
            console.warn("[ModelManager] IndexedDB put failed for $key: ${e.message}")
        }
    }

    /**
     * Delete a value from IndexedDB
     */
    private fun deleteFromIndexedDB(key: String) {
        try {
            val request = kotlinx.browser.window.asDynamic().indexedDB.open(DB_NAME, DB_VERSION)
            request.onsuccess = { _: Event ->
                val db = request.result
                try {
                    val transaction = db.transaction(STORE_NAME, "readwrite")
                    val store = transaction.objectStore(STORE_NAME)
                    store.delete(key)
                } catch (_: Exception) {
                    // Store may not exist yet
                }
                db.close()
            }
        } catch (_: Exception) {
            // Best-effort delete
        }
    }

    // ─── Buffer Conversion ──────────────────────────────────────

    /**
     * Convert ArrayBuffer to ByteArray for AONCodec
     */
    private fun arrayBufferToByteArray(buffer: ArrayBuffer): ByteArray {
        val uint8 = Uint8Array(buffer)
        return ByteArray(uint8.length) { i ->
            (uint8.asDynamic()[i] as Int).toByte()
        }
    }

    /**
     * Convert ByteArray back to ArrayBuffer for ONNX Runtime Web
     */
    private fun byteArrayToArrayBuffer(data: ByteArray): ArrayBuffer {
        val uint8 = Uint8Array(data.size)
        for (i in data.indices) {
            uint8.asDynamic()[i] = data[i]
        }
        return uint8.buffer
    }

    // ─── Utility ────────────────────────────────────────────────

    /**
     * Detect which model type to use based on current locale
     *
     * English locales → MobileBERT (smaller, faster)
     * Everything else → mALBERT (multilingual)
     */
    private fun detectModelTypeForLocale(): ModelType {
        val locale = localeManager.getCurrentLocale()
        return if (locale.startsWith("en")) {
            ModelType.MOBILEBERT
        } else {
            ModelType.MALBERT
        }
    }

    /**
     * Decode an ArrayBuffer to a UTF-8 string
     */
    private fun decodeArrayBufferToString(buffer: ArrayBuffer): String {
        val decoder = js("new TextDecoder('utf-8')")
        return decoder.decode(buffer) as String
    }
}

/**
 * Model type enumeration for JS/Web platform
 *
 * MobileBERT: 384-dim, ~25MB, English-optimized, faster WASM inference
 * mALBERT: 768-dim, ~41MB, 52 languages, full multilingual support
 */
/**
 * Model type enumeration for JS/Web platform
 *
 * Uses AVA proprietary naming convention: AVA-{DIM}-{Variant}-{Quant}.AON
 * MobileBERT: 384-dim, ~25MB, English-optimized, faster WASM inference
 * mALBERT: 768-dim, ~41MB, 52 languages, full multilingual support
 */
enum class ModelType(
    val modelFileName: String,
    val vocabFileName: String,
    val displayName: String,
    val embeddingDimension: Int,
    val description: String
) {
    MOBILEBERT(
        modelFileName = "AVA-384-Base-INT8.AON",
        vocabFileName = "AVA-384-Base-vocab.txt",
        displayName = "MobileBERT Lite",
        embeddingDimension = 384,
        description = "Lightweight English model"
    ),
    MALBERT(
        modelFileName = "AVA-768-Base-INT8.AON",
        vocabFileName = "AVA-768-Base-vocab.txt",
        displayName = "mALBERT Multilingual",
        embeddingDimension = 768,
        description = "Multilingual model (52+ languages)"
    );

    fun isMultilingual(): Boolean = this == MALBERT

    fun getModelVersion(): String = when (this) {
        MOBILEBERT -> "MobileBERT-uncased-onnx-384"
        MALBERT -> "mALBERT-base-v2-onnx-768"
    }
}
