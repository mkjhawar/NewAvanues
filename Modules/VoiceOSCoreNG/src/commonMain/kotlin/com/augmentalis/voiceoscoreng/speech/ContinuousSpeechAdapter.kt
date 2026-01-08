/**
 * ContinuousSpeechAdapter.kt - Adapter for continuous speech engines
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2026-01-07
 *
 * Wraps continuous speech recognition engines (Vosk, Google, Azure) with
 * CommandWordDetector to provide command-word detection capability.
 *
 * This brings continuous engines to feature parity with Vivoka's
 * grammar-based command-word recognition.
 */
package com.augmentalis.voiceoscoreng.speech

import com.augmentalis.voiceoscoreng.features.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * Adapter that wraps a continuous speech engine with command word detection.
 *
 * ## Architecture
 *
 * ```
 * ┌────────────────────────────────────────────────────────┐
 * │              ContinuousSpeechAdapter                   │
 * ├────────────────────────────────────────────────────────┤
 * │                                                        │
 * │  ┌─────────────────┐    ┌──────────────────────────┐  │
 * │  │ Wrapped Engine  │───▶│ CommandWordDetector      │  │
 * │  │ (Vosk/Google/   │    │ - Fuzzy matching         │  │
 * │  │  Azure)         │    │ - Confidence scoring     │  │
 * │  └─────────────────┘    └───────────┬──────────────┘  │
 * │          │                          │                 │
 * │          ▼                          ▼                 │
 * │  ┌─────────────────┐    ┌──────────────────────────┐  │
 * │  │ Raw Results     │    │ Command Results          │  │
 * │  │ "go back pls"   │    │ ("go back", 0.95)        │  │
 * │  └─────────────────┘    └──────────────────────────┘  │
 * │                                                        │
 * └────────────────────────────────────────────────────────┘
 * ```
 *
 * ## Usage
 *
 * ```kotlin
 * val voskEngine = VoskEngine()
 * val adapter = ContinuousSpeechAdapter(
 *     engine = voskEngine,
 *     detector = CommandWordDetector(confidenceThreshold = 0.75f)
 * )
 *
 * // Use like any ISpeechEngine
 * adapter.initialize(config)
 * adapter.updateCommands(listOf("go back", "scroll down"))
 * adapter.startListening()
 *
 * // Results are command-word matches, not raw text
 * adapter.results.collect { result ->
 *     println("Detected: ${result.text}, confidence: ${result.confidence}")
 * }
 * ```
 */
class ContinuousSpeechAdapter(
    /**
     * The underlying continuous speech engine.
     */
    private val engine: ISpeechEngine,

    /**
     * Command word detector for extracting commands from continuous text.
     */
    private val detector: CommandWordDetector = CommandWordDetector(),

    /**
     * Whether to emit raw results in addition to command matches.
     */
    private val emitRawResults: Boolean = false,

    /**
     * Minimum words required before attempting command detection.
     * Helps avoid false positives on short utterances.
     */
    private val minWordsForDetection: Int = 1
) : ISpeechEngine {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    // ═══════════════════════════════════════════════════════════════════════
    // ISpeechEngine Delegation with Command Detection
    // ═══════════════════════════════════════════════════════════════════════

    override val state: StateFlow<EngineState> = engine.state

    private val _results = MutableSharedFlow<SpeechResult>(replay = 0)
    override val results: Flow<SpeechResult> = _results.asSharedFlow()

    override val errors: Flow<SpeechError> = engine.errors

    init {
        // Transform raw results through command detector
        scope.launch {
            engine.results.collect { rawResult ->
                processRawResult(rawResult)
            }
        }
    }

    private suspend fun processRawResult(rawResult: SpeechResult) {
        // Optionally emit raw result for debugging/logging
        if (emitRawResults) {
            _results.emit(rawResult.copy(
                text = "[RAW] ${rawResult.text}"
            ))
        }

        // Skip detection for very short utterances
        val wordCount = rawResult.text.split(" ").filter { it.isNotBlank() }.size
        if (wordCount < minWordsForDetection) {
            return
        }

        // Only process final results (skip interim)
        if (!rawResult.isFinal) {
            return
        }

        // Detect commands in the raw text
        val matches = detector.detectCommands(rawResult.text)

        if (matches.isNotEmpty()) {
            // Emit best match as speech result
            val bestMatch = matches.first()
            _results.emit(
                SpeechResult(
                    text = bestMatch.command,
                    confidence = bestMatch.confidence,
                    isFinal = true,
                    timestamp = rawResult.timestamp,
                    alternatives = matches.drop(1).map { match ->
                        SpeechResult.Alternative(
                            text = match.command,
                            confidence = match.confidence
                        )
                    }
                )
            )
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // ISpeechEngine Implementation
    // ═══════════════════════════════════════════════════════════════════════

    override suspend fun initialize(config: SpeechConfig): Result<Unit> {
        return engine.initialize(config)
    }

    override suspend fun startListening(): Result<Unit> {
        return engine.startListening()
    }

    override suspend fun stopListening() {
        engine.stopListening()
    }

    /**
     * Update commands - forwards to both detector and underlying engine.
     *
     * For grammar-capable engines like Vosk, this also updates the
     * recognition grammar for improved accuracy.
     */
    override suspend fun updateCommands(commands: List<String>): Result<Unit> {
        // Update detector with new commands
        detector.updateCommands(commands)

        // Also update engine (for grammar-based optimization if supported)
        return engine.updateCommands(commands)
    }

    override suspend fun updateConfiguration(config: SpeechConfig): Result<Unit> {
        // Update detector threshold from config
        detector.confidenceThreshold = config.confidenceThreshold

        return engine.updateConfiguration(config)
    }

    override fun isRecognizing(): Boolean = engine.isRecognizing()

    override fun isInitialized(): Boolean = engine.isInitialized()

    override fun getEngineType(): SpeechEngine = engine.getEngineType()

    override fun getSupportedFeatures(): Set<EngineFeature> {
        // Add CUSTOM_VOCABULARY since we provide command detection
        return engine.getSupportedFeatures() + EngineFeature.CUSTOM_VOCABULARY
    }

    override suspend fun destroy() {
        engine.destroy()
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Additional API
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Get the underlying command word detector for configuration.
     */
    fun getDetector(): CommandWordDetector = detector

    /**
     * Get the wrapped engine.
     */
    fun getWrappedEngine(): ISpeechEngine = engine

    /**
     * Add static commands to detector.
     */
    fun addStaticCommands() {
        detector.addStaticCommands()
    }
}

/**
 * Extension function to wrap any ISpeechEngine with command detection.
 */
fun ISpeechEngine.withCommandDetection(
    confidenceThreshold: Float = 0.7f,
    emitRawResults: Boolean = false
): ContinuousSpeechAdapter {
    return ContinuousSpeechAdapter(
        engine = this,
        detector = CommandWordDetector(confidenceThreshold = confidenceThreshold),
        emitRawResults = emitRawResults
    )
}
