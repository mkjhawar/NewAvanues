/**
 * DesktopAvxNative.kt - Thread-safe wrapper around Sherpa-ONNX OnlineRecognizer for Desktop JVM
 *
 * Copyright (C) Augmentalis Inc, Intelligent Devices LLC
 * Created: 2026-02-24
 *
 * Desktop JVM equivalent of Android's AvxNative. Uses the same Sherpa-ONNX
 * Kotlin API (OnlineRecognizer, OnlineStream) with Desktop-specific native
 * library loading.
 *
 * Native library naming per platform:
 * - macOS:   libsherpa-onnx-jni.dylib
 * - Linux:   libsherpa-onnx-jni.so
 * - Windows: sherpa-onnx-jni.dll
 *
 * Native library search order:
 * 1. java.library.path (standard JVM mechanism)
 * 2. {workingDir}/lib/
 * 3. {workingDir}/natives/
 * 4. ~/.avanues/lib/
 */
package com.augmentalis.speechrecognition.avx

import com.augmentalis.speechrecognition.logError
import com.augmentalis.speechrecognition.logInfo
import com.augmentalis.speechrecognition.logWarn
import com.k2fsa.sherpa.onnx.EndpointConfig
import com.k2fsa.sherpa.onnx.EndpointRule
import com.k2fsa.sherpa.onnx.FeatureConfig
import com.k2fsa.sherpa.onnx.OnlineModelConfig
import com.k2fsa.sherpa.onnx.OnlineRecognizer
import com.k2fsa.sherpa.onnx.OnlineRecognizerConfig
import com.k2fsa.sherpa.onnx.OnlineStream
import com.k2fsa.sherpa.onnx.OnlineTransducerModelConfig
import java.io.File

/**
 * Thread-safe wrapper around Sherpa-ONNX OnlineRecognizer for Desktop JVM.
 *
 * Mirrors Android's AvxNative but handles native library loading for
 * macOS/Linux/Windows instead of relying on the Android AAR.
 */
class DesktopAvxNative {

    companion object {
        private const val TAG = "DesktopAvxNative"

        @Volatile
        private var isLibraryLoaded = false

        /**
         * Ensure the Sherpa-ONNX native library is loaded.
         * Must be called before creating any OnlineRecognizer.
         *
         * Returns false if Sherpa-ONNX classes aren't on the classpath
         * or the native library can't be found.
         */
        fun ensureLoaded(): Boolean {
            if (isLibraryLoaded) return true
            return synchronized(this) {
                if (isLibraryLoaded) return@synchronized true

                // Verify Sherpa-ONNX classes are on the classpath first
                try {
                    Class.forName("com.k2fsa.sherpa.onnx.OnlineRecognizer")
                } catch (_: ClassNotFoundException) {
                    logWarn(TAG, "Sherpa-ONNX classes not on classpath — AVX unavailable")
                    return@synchronized false
                } catch (_: NoClassDefFoundError) {
                    logWarn(TAG, "Sherpa-ONNX class loading failed — AVX unavailable")
                    return@synchronized false
                }

                try {
                    System.loadLibrary("sherpa-onnx-jni")
                    isLibraryLoaded = true
                    logInfo(TAG, "sherpa-onnx-jni loaded via java.library.path")
                    true
                } catch (e: UnsatisfiedLinkError) {
                    tryLoadFromSearchPaths()
                }
            }
        }

        private fun tryLoadFromSearchPaths(): Boolean {
            val osName = System.getProperty("os.name", "").lowercase()
            val libName = when {
                osName.contains("mac") || osName.contains("darwin") -> "libsherpa-onnx-jni.dylib"
                osName.contains("win") -> "sherpa-onnx-jni.dll"
                else -> "libsherpa-onnx-jni.so"
            }

            val searchPaths = listOf(
                System.getProperty("user.dir"),
                System.getProperty("user.dir") + "/lib",
                System.getProperty("user.dir") + "/natives",
                System.getProperty("user.home") + "/.avanues/lib"
            )

            for (basePath in searchPaths) {
                val fullPath = "$basePath/$libName"
                val file = File(fullPath)
                if (file.exists()) {
                    try {
                        System.load(file.absolutePath)
                        isLibraryLoaded = true
                        logInfo(TAG, "sherpa-onnx-jni loaded from: $fullPath")
                        return true
                    } catch (e: UnsatisfiedLinkError) {
                        logError(TAG, "Failed to load from $fullPath: ${e.message}")
                    }
                }
            }

            logError(TAG, "sherpa-onnx-jni native library not found. Searched: $searchPaths")
            return false
        }
    }

    private val lock = Any()

    @Volatile
    private var recognizer: OnlineRecognizer? = null

    @Volatile
    private var stream: OnlineStream? = null

    @Volatile
    private var hotWordsFile: File? = null

    /**
     * Create and configure the OnlineRecognizer with a transducer model.
     *
     * @param modelPaths Paths to the decrypted model files (encoder, decoder, joiner, tokens)
     * @param config AVX engine configuration
     * @return true if recognizer was created successfully
     */
    fun createRecognizer(modelPaths: AvxModelPaths, config: AvxConfig): Boolean {
        return synchronized(lock) {
            try {
                releaseInternal()

                val hwFile = writeHotWordsFile(config.hotWords)

                val transducerConfig = OnlineTransducerModelConfig(
                    encoder = modelPaths.encoderPath,
                    decoder = modelPaths.decoderPath,
                    joiner = modelPaths.joinerPath
                )

                val modelConfig = OnlineModelConfig(
                    transducer = transducerConfig,
                    tokens = modelPaths.tokensPath,
                    numThreads = config.effectiveThreadCount(isAndroid = false),
                    debug = false,
                    provider = "cpu"
                )

                val endpointConfig = EndpointConfig(
                    rule1 = EndpointRule(
                        mustContainNonSilence = false,
                        minTrailingSilence = 2.4f,
                        minUtteranceLength = 0f
                    ),
                    rule2 = EndpointRule(
                        mustContainNonSilence = true,
                        minTrailingSilence = 0.8f,
                        minUtteranceLength = 0f
                    ),
                    rule3 = EndpointRule(
                        mustContainNonSilence = false,
                        minTrailingSilence = 0f,
                        minUtteranceLength = 15f
                    )
                )

                val recognizerConfig = OnlineRecognizerConfig(
                    featConfig = FeatureConfig(sampleRate = config.sampleRate, featureDim = 80),
                    modelConfig = modelConfig,
                    endpointConfig = endpointConfig,
                    enableEndpoint = config.enableEndpoint,
                    decodingMethod = config.decodingMethod,
                    maxActivePaths = config.maxActivePaths,
                    hotwordsFile = hwFile?.absolutePath ?: "",
                    hotwordsScore = config.defaultHotWordBoost,
                    blankPenalty = config.blankPenalty
                )

                recognizer = OnlineRecognizer(config = recognizerConfig)
                stream = recognizer?.createStream()
                hotWordsFile = hwFile

                logInfo(TAG, "Desktop OnlineRecognizer created: lang=${config.language.langCode}, " +
                        "threads=${config.effectiveThreadCount(isAndroid = false)}, " +
                        "hotWords=${config.hotWords.size}, " +
                        "decoding=${config.decodingMethod}")
                true
            } catch (e: Exception) {
                logError(TAG, "Failed to create Desktop OnlineRecognizer", e)
                releaseInternal()
                false
            }
        }
    }

    fun acceptWaveform(samples: FloatArray, sampleRate: Int = 16000) {
        synchronized(lock) {
            val s = stream ?: return
            try {
                s.acceptWaveform(samples, sampleRate)
            } catch (e: Exception) {
                logError(TAG, "acceptWaveform failed", e)
            }
        }
    }

    fun decode() {
        synchronized(lock) {
            val r = recognizer ?: return
            val s = stream ?: return
            try {
                while (r.isReady(s)) {
                    r.decode(s)
                }
            } catch (e: Exception) {
                logError(TAG, "decode failed", e)
            }
        }
    }

    fun getResult(): AvxTranscriptionResult {
        return synchronized(lock) {
            val r = recognizer ?: return AvxTranscriptionResult("", 0f)
            val s = stream ?: return AvxTranscriptionResult("", 0f)
            try {
                val result = r.getResult(s)
                val text = result.text.trim()

                val confidence = if (text.isNotEmpty()) {
                    val tokenBonus = (result.tokens.size.coerceAtMost(10) * 0.03f)
                    (0.7f + tokenBonus).coerceAtMost(1.0f)
                } else {
                    0f
                }

                AvxTranscriptionResult(
                    text = text,
                    confidence = confidence,
                    alternatives = emptyList(),
                    tokens = result.tokens.toList(),
                    timestamps = result.timestamps.toList()
                )
            } catch (e: Exception) {
                logError(TAG, "getResult failed", e)
                AvxTranscriptionResult("", 0f)
            }
        }
    }

    fun isEndpoint(): Boolean {
        return synchronized(lock) {
            val r = recognizer ?: return false
            val s = stream ?: return false
            try {
                r.isEndpoint(s)
            } catch (e: Exception) {
                false
            }
        }
    }

    fun reset() {
        synchronized(lock) {
            val r = recognizer ?: return
            val s = stream ?: return
            try {
                r.reset(s)
            } catch (e: Exception) {
                logError(TAG, "reset failed", e)
            }
        }
    }

    fun updateHotWords(hotWords: List<HotWord>) {
        synchronized(lock) {
            val r = recognizer ?: return
            try {
                val hotWordsStr = hotWords.joinToString("\n") { "${it.phrase} :${it.boost}" }
                stream = r.createStream(hotWordsStr)
                logInfo(TAG, "Desktop hot words updated: ${hotWords.size} phrases")
            } catch (e: Exception) {
                logError(TAG, "updateHotWords failed", e)
            }
        }
    }

    fun isReady(): Boolean = recognizer != null && stream != null

    fun release() {
        synchronized(lock) {
            releaseInternal()
        }
    }

    private fun releaseInternal() {
        try {
            stream = null
            recognizer?.release()
            recognizer = null
            hotWordsFile?.delete()
            hotWordsFile = null
            logInfo(TAG, "Desktop OnlineRecognizer released")
        } catch (e: Exception) {
            logError(TAG, "release failed", e)
        }
    }

    private fun writeHotWordsFile(hotWords: List<HotWord>): File? {
        if (hotWords.isEmpty()) return null

        val cacheDir = File(System.getProperty("java.io.tmpdir"), "avx_hw")
        cacheDir.mkdirs()
        val hwFile = File(cacheDir, "hotwords.txt")

        val content = hotWords.joinToString("\n") { "${it.phrase} :${it.boost}" }
        hwFile.writeText(content)

        logInfo(TAG, "Hot words file written: ${hwFile.absolutePath} (${hotWords.size} entries)")
        return hwFile
    }
}
