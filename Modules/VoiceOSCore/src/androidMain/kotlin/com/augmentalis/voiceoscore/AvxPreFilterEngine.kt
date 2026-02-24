/**
 * AvxPreFilterEngine.kt - AVX → Vivoka pre-filter for command recognition
 *
 * Copyright (C) Augmentalis Inc, Intelligent Devices LLC
 * Created: 2026-02-24
 *
 * Simple pre-filter that runs AVX as a lightweight first pass for command recognition.
 * If AVX returns high confidence (>= threshold), the result is accepted immediately.
 * If confidence is low, the audio is forwarded to Vivoka for grammar-based recognition.
 *
 * This is NOT an ensemble — only ONE engine produces the final result.
 * AVX handles the easy ~85% of commands. Vivoka handles the uncertain ~15%.
 *
 * Active only in COMMAND mode on Android where both engines are available.
 * DICTATION mode uses Whisper directly (open vocabulary).
 * WAKE_WORD mode uses Vivoka grammar directly (already implemented).
 */
package com.augmentalis.voiceoscore

import android.content.Context
import android.util.Log
import com.augmentalis.speechrecognition.RecognitionResult
import com.augmentalis.speechrecognition.SpeechMode
import com.augmentalis.speechrecognition.avx.AvxConfig
import com.augmentalis.speechrecognition.avx.AvxEngine
import com.augmentalis.speechrecognition.avx.AvxEngineState
import com.augmentalis.speechrecognition.avx.AvxLanguage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

/**
 * Pre-filter mode determines when AVX is used as a first-pass engine.
 */
enum class PreFilterMode {
    /** Pre-filter disabled — Vivoka handles all commands (default, same as today) */
    DISABLED,
    /** Pre-filter active in COMMAND mode only (STATIC_COMMAND + DYNAMIC_COMMAND) */
    COMMAND_ONLY,
    /** Pre-filter active in all speech modes except DICTATION */
    ALL_EXCEPT_DICTATION
}

/**
 * Orchestrates AVX as a lightweight pre-filter before Vivoka.
 *
 * Architecture:
 * ```
 * Audio → AVX Engine (hot words boosted)
 *             ↓
 *         confidence >= threshold? ──YES──→ Accept → emit result
 *             ↓ NO
 *         (audio already processed by Vivoka via its own listener)
 *         Vivoka result → emit result
 * ```
 *
 * The key insight: since both AVX and Vivoka share the same audio source
 * (Android AudioRecord), we DON'T forward audio between them. Instead:
 * - AVX processes the audio chunk first (via its own VAD + ONNX)
 * - If high confidence: emit immediately, suppress next Vivoka result
 * - If low confidence: let Vivoka's result pass through normally
 *
 * This works because AVX is faster (~50-100ms) than Vivoka grammar compilation,
 * so AVX finishes before Vivoka in most cases.
 */
class AvxPreFilterEngine(
    private val context: Context
) {
    companion object {
        private const val TAG = "AvxPreFilter"

        /** Default confidence threshold for accepting AVX results without Vivoka fallback */
        const val DEFAULT_CONFIDENCE_THRESHOLD = 0.85f
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /** The AVX engine instance (lazy-loaded to save memory) */
    private var avxEngine: AvxEngine? = null

    /** Current pre-filter mode */
    var mode: PreFilterMode = PreFilterMode.DISABLED
        private set

    /** Confidence threshold for accepting AVX results */
    var confidenceThreshold: Float = DEFAULT_CONFIDENCE_THRESHOLD
        private set

    /** Whether AVX is loaded and ready */
    val isAvxReady: Boolean
        get() = avxEngine?.isReady() == true

    /** Whether pre-filter accepted the last result (for metrics) */
    @Volatile
    var lastResultFromAvx: Boolean = false
        private set

    /** Metrics: how many results AVX handled vs Vivoka fallback */
    @Volatile
    var avxAcceptCount: Int = 0
        private set

    @Volatile
    var vivokaFallbackCount: Int = 0
        private set

    /** Callback for results that AVX handles with high confidence */
    var onAvxResult: ((RecognitionResult) -> Unit)? = null

    /**
     * Enable pre-filtering with the given mode and language.
     * Lazy-loads the AVX engine if not already loaded.
     *
     * @param filterMode When to use pre-filtering
     * @param language Language code for AVX model
     * @param threshold Confidence threshold for accepting AVX results
     */
    suspend fun enable(
        filterMode: PreFilterMode = PreFilterMode.COMMAND_ONLY,
        language: String = "en",
        threshold: Float = DEFAULT_CONFIDENCE_THRESHOLD
    ): Boolean {
        mode = filterMode
        confidenceThreshold = threshold

        if (filterMode == PreFilterMode.DISABLED) {
            Log.i(TAG, "Pre-filter disabled")
            return true
        }

        // Lazy-load AVX engine
        if (avxEngine == null) {
            avxEngine = AvxEngine(context)
        }

        val avxLang = AvxLanguage.forCode(language) ?: AvxLanguage.ENGLISH
        val config = AvxConfig(
            language = avxLang,
            confidenceThreshold = threshold
        )

        val success = avxEngine?.initialize(config) ?: false
        if (!success) {
            Log.w(TAG, "Failed to initialize AVX engine — pre-filter will be disabled")
            mode = PreFilterMode.DISABLED
            return false
        }

        // Wire AVX results to the pre-filter evaluation
        avxEngine?.resultFlow?.onEach { result ->
            evaluateAvxResult(result)
        }?.launchIn(scope)

        Log.i(TAG, "Pre-filter enabled: mode=$filterMode, lang=${avxLang.langCode}, threshold=$threshold")
        return true
    }

    /**
     * Disable pre-filtering and release AVX resources.
     */
    fun disable() {
        mode = PreFilterMode.DISABLED
        avxEngine?.destroy()
        avxEngine = null
        Log.i(TAG, "Pre-filter disabled, AVX destroyed")
    }

    /**
     * Check if pre-filtering should be active for the given speech mode.
     */
    fun isActiveFor(speechMode: SpeechMode): Boolean {
        if (mode == PreFilterMode.DISABLED) return false
        if (avxEngine?.isReady() != true) return false

        return when (mode) {
            PreFilterMode.DISABLED -> false
            PreFilterMode.COMMAND_ONLY -> speechMode in listOf(
                SpeechMode.STATIC_COMMAND,
                SpeechMode.DYNAMIC_COMMAND,
                SpeechMode.COMBINED_COMMAND
            )
            PreFilterMode.ALL_EXCEPT_DICTATION -> speechMode != SpeechMode.DICTATION
        }
    }

    /**
     * Start AVX listening (call when Vivoka starts, if pre-filter is active).
     */
    fun startListening(speechMode: SpeechMode) {
        if (!isActiveFor(speechMode)) return
        avxEngine?.startListening(
            when (speechMode) {
                SpeechMode.STATIC_COMMAND -> com.augmentalis.speechrecognition.SpeechMode.STATIC_COMMAND
                SpeechMode.DYNAMIC_COMMAND -> com.augmentalis.speechrecognition.SpeechMode.DYNAMIC_COMMAND
                else -> com.augmentalis.speechrecognition.SpeechMode.DYNAMIC_COMMAND
            }
        )
    }

    /**
     * Stop AVX listening.
     */
    fun stopListening() {
        avxEngine?.stopListening()
    }

    /**
     * Update AVX hot words from the current command list.
     */
    fun updateCommands(commands: List<String>) {
        avxEngine?.updateCommands(commands)
    }

    /**
     * Evaluate an AVX result and decide whether to accept or fall back.
     *
     * This is the core pre-filter logic — a simple if/else:
     * - High confidence (>= threshold): accept, emit, suppress Vivoka
     * - Low confidence: let Vivoka handle it
     */
    private fun evaluateAvxResult(result: RecognitionResult) {
        if (result.isPartial || result.text.isBlank()) return

        if (result.confidence >= confidenceThreshold) {
            // AVX is confident — accept this result
            lastResultFromAvx = true
            avxAcceptCount++
            Log.d(TAG, "AVX ACCEPT: '${result.text}' conf=${result.confidence} (threshold=$confidenceThreshold)")
            onAvxResult?.invoke(result)
        } else {
            // Low confidence — let Vivoka's result pass through
            lastResultFromAvx = false
            vivokaFallbackCount++
            Log.d(TAG, "AVX DEFER: '${result.text}' conf=${result.confidence} → Vivoka fallback")
        }
    }

    /**
     * Get pre-filter metrics for diagnostics.
     */
    fun getMetrics(): Map<String, Any> = mapOf(
        "mode" to mode.name,
        "avxReady" to isAvxReady,
        "confidenceThreshold" to confidenceThreshold,
        "avxAcceptCount" to avxAcceptCount,
        "vivokaFallbackCount" to vivokaFallbackCount,
        "avxAcceptRate" to if (avxAcceptCount + vivokaFallbackCount > 0) {
            avxAcceptCount.toFloat() / (avxAcceptCount + vivokaFallbackCount)
        } else 0f
    )

    /**
     * Release all resources.
     */
    fun destroy() {
        avxEngine?.destroy()
        avxEngine = null
        mode = PreFilterMode.DISABLED
    }
}
