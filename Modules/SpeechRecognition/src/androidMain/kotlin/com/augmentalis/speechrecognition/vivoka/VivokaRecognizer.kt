/**
 * VivokaRecognizer.kt - Recognition processing for Vivoka VSDK engine
 *
 * Copyright (C) Augmentalis Inc, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Created: 2025-01-28
 * Updated: 2026-01-27 - Migrated to KMP androidMain
 *
 * Extracted from monolithic VivokaEngine.kt as part of SOLID refactoring
 * Handles result processing, command interpretation, and recognition flow control
 */
package com.augmentalis.speechrecognition.vivoka

import android.util.Log
import com.augmentalis.speechrecognition.SpeechConfig
import com.augmentalis.speechrecognition.SpeechMode
import com.augmentalis.speechrecognition.RecognitionResult
import com.augmentalis.speechrecognition.ConfidenceScorer
import com.augmentalis.speechrecognition.RecognitionEngine
import com.augmentalis.speechrecognition.ResultProcessor
import com.vivoka.vsdk.asr.recognizer.RecognizerResultType
import com.vivoka.vsdk.asr.utils.AsrResultParser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * Processes recognition results from Vivoka VSDK and handles command interpretation
 */
class VivokaRecognizer(
    private val coroutineScope: CoroutineScope
) {

    companion object {
        private const val TAG = "VivokaRecognizer"
        private const val PROCESSING_DELAY = 200L
    }

    // Configuration
    private lateinit var config: SpeechConfig

    // Result processing
    private val resultProcessor = ResultProcessor()

    // Confidence scoring
    private val confidenceScorer = ConfidenceScorer()

    // Recognition state
    @Volatile
    private var isProcessingResult = false

    // Result flows
    private val _resultFlow = MutableStateFlow<RecognitionResult?>(null)
    val resultFlow: StateFlow<RecognitionResult?> = _resultFlow

    // Listeners
    private var onResultListener: ((RecognitionResult) -> Unit)? = null
    private var onPartialResultListener: ((String) -> Unit)? = null

    // Command processing
    private var recognitionProcessingJob: Job? = null

    // Recognition modes
    enum class RecognizerMode {
        COMMAND,
        FREE_SPEECH_START,
        FREE_SPEECH_RUNNING,
        STOP_FREE_SPEECH
    }

    /**
     * Initialize the recognizer with configuration
     */
    fun initialize(config: SpeechConfig) {
        this.config = config

        // Configure result processor
        resultProcessor.setMode(config.mode)
        resultProcessor.setConfidenceThreshold(config.confidenceThreshold)

        Log.d(TAG, "Recognizer initialized with confidence threshold: ${config.confidenceThreshold}")
    }

    /**
     * Process recognition result from Vivoka VSDK
     * CRITICAL: Contains the continuous recognition fix
     */
    suspend fun processRecognitionResult(
        result: String?,
        resultType: RecognizerResultType?,
        isDictationActive: Boolean,
        isVoiceSleeping: Boolean,
        onModeSwitch: suspend (RecognizerMode) -> Unit
    ): RecognitionProcessingResult {
        if (result.isNullOrEmpty() || resultType != RecognizerResultType.ASR) {
            onModeSwitch(RecognizerMode.COMMAND)
            return RecognitionProcessingResult.noResult()
        }

        return try {
            isProcessingResult = true
            //Log.d(TAG, "Processing ASR result: $result")
            val parsed = AsrResultParser.parseResult(result)
            val first = parsed.hypotheses.firstOrNull { !cleanString(it.text ?: "").isEmpty() }
            if (first == null) {
                onModeSwitch(RecognizerMode.COMMAND)
                return RecognitionProcessingResult.noResult()
            }

            val command = cleanString(first.text ?: "")
            val confidence = first.confidence

            Log.d(TAG, "Hypothesis: command='$command', confidence=$confidence, threshold=${config.confidenceThreshold}")

            if (confidence < config.confidenceThreshold) {
                onPartialResultListener?.invoke(command)
                recognitionProcessingJob?.cancel()
                onModeSwitch(RecognizerMode.COMMAND)
                return RecognitionProcessingResult.lowConfidence(command, confidence)
            }

            // Wake from sleep (unmute) takes precedence.
            if (isVoiceSleeping && command.equals(config.unmuteCommand, ignoreCase = true)) {
                Log.d(TAG, "Unmute command detected")
                onModeSwitch(RecognizerMode.COMMAND)
                return RecognitionProcessingResult.unmuteCommand()
            }

            // Dictation flow.
            if (isDictationActive) {
                val isStop = command.equals(config.stopDictationCommand, ignoreCase = true)
                val mode = if (isStop) RecognizerMode.STOP_FREE_SPEECH else RecognizerMode.FREE_SPEECH_RUNNING
                onModeSwitch(mode)

                return if (isStop) {
                    RecognitionProcessingResult.dictationEnd()
                } else {
                    val speechResult = createRecognitionResult(command, confidence, true)
                    _resultFlow.value = speechResult
                    Log.d(TAG, "SPEECH_TEST: processRecognitionResult speechResult = $speechResult")
                    onResultListener?.invoke(speechResult)
                    RecognitionProcessingResult.dictationContinue(speechResult)
                }
            }

            // Command mode.
            when {
                command.equals(config.muteCommand, ignoreCase = true) -> {
                    onModeSwitch(RecognizerMode.COMMAND)
                    RecognitionProcessingResult.muteCommand()
                }
                command.equals(config.startDictationCommand, ignoreCase = true) -> {
                    onModeSwitch(RecognizerMode.FREE_SPEECH_START)
                    RecognitionProcessingResult.dictationStart()
                }
                else -> {
                    val speechResult = createRecognitionResult(command, confidence, true)
                    startCommandProcessing(speechResult) // keep async stability behavior
                    onModeSwitch(RecognizerMode.COMMAND)
                    RecognitionProcessingResult.regularCommand(speechResult)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to process recognition result", e)
            onModeSwitch(RecognizerMode.COMMAND)
            RecognitionProcessingResult.error(e.message ?: "Processing failed")
        } finally {
            isProcessingResult = false
        }
    }


    /**
     * Start command processing with delay for response stability
     */
    private fun startCommandProcessing(result: RecognitionResult) {
        recognitionProcessingJob?.cancel()
        recognitionProcessingJob = coroutineScope.launch {
            try {
                delay(PROCESSING_DELAY) // Small delay for response stability

                Log.d(TAG, "Processing command: ${result.text}")

                // Emit result to flow and listener
                _resultFlow.value = result
                Log.d(TAG, "SPEECH_TEST: startCommandProcessing speechResult = $result")
                onResultListener?.invoke(result)

            } catch (e: Exception) {
                Log.e(TAG, "Error during command processing", e)
            }
        }
    }

    /**
     * Create RecognitionResult from recognition data with confidence scoring
     */
    private fun createRecognitionResult(
        text: String,
        confidence: Int,
        isFinal: Boolean
    ): RecognitionResult {
        // Use confidence scorer to normalize and classify
        val confidenceResult = confidenceScorer.createResult(
            text = text,
            rawConfidence = confidence.toFloat(),
            engine = RecognitionEngine.VIVOKA
        )

        Log.d(TAG, "Confidence scoring: raw=$confidence, normalized=${confidenceResult.confidence}, level=${confidenceResult.level}")

        return RecognitionResult(
            text = text,
            confidence = confidenceResult.confidence,
            isFinal = isFinal,
            engine = "vivoka",
            mode = resultProcessor.getMode().toString(),
            timestamp = System.currentTimeMillis(),
            metadata = mapOf(
                "raw_confidence" to confidence,
                "confidence_level" to confidenceResult.level.name,
                "scoring_method" to confidenceResult.scoringMethod.name
            )
        )
    }

    /**
     * Clean recognized string from VSDK artifacts
     * Preserves the exact logic from the original implementation
     */
    private fun cleanString(str: String): String {
        val isDotCom = str.contains("dot_com")
        val text = str.replace('_', ' ')
            .replace("\\preposition", "")
            .replace("\\pronoun", "")
            .replace("\\determiner", "")
            .replace("\\number", "")

        val newText = if (text.contains("\\")) {
            text.split("\\")[0]
        } else {
            text
        }

        return if (isDotCom) {
            newText.replace("\\s".toRegex(), "")
        } else {
            newText
        }
    }

    /**
     * Handle recognition events (silence/speech detection)
     */
    fun processRecognitionEvent(
        codeString: String?,
        @Suppress("UNUSED_PARAMETER") message: String?,
        @Suppress("UNUSED_PARAMETER") time: String?,
        onSilenceDetected: () -> Unit,
        onSpeechDetected: () -> Unit
    ) {
        //Log.d(TAG, "Recognition event: code='$codeString', message='$message'")

        when (codeString) {
            "SilenceDetected" -> {
                //Log.d(TAG, "Silence detected")
                onSilenceDetected()
            }
            "SpeechDetected" -> {
                //Log.d(TAG, "Speech detected")
                onSpeechDetected()
            }
            else -> {
                Log.d(TAG, "Unknown recognition event: $codeString")
            }
        }
    }

    /**
     * Set result listener
     */
    fun setResultListener(listener: (RecognitionResult) -> Unit) {
        Log.d(TAG, "SPEECH_TEST: setResultListener DONE $listener")
        this.onResultListener = listener
    }

    /**
     * Set partial result listener
     */
    fun setPartialResultListener(listener: (String) -> Unit) {
        this.onPartialResultListener = listener
    }

    /**
     * Update recognition mode for result processor
     */
    fun updateRecognitionMode(mode: SpeechMode) {
        resultProcessor.setMode(mode)
        Log.d(TAG, "Recognition mode updated: $mode")
    }

    /**
     * Update confidence threshold
     */
    fun updateConfidenceThreshold(threshold: Float) {
        resultProcessor.setConfidenceThreshold(threshold)
        Log.d(TAG, "Confidence threshold updated: $threshold")
    }

    /**
     * Cancel any ongoing recognition processing
     */
    fun cancelProcessing() {
        recognitionProcessingJob?.cancel()
        isProcessingResult = false
        Log.d(TAG, "Recognition processing cancelled")
    }

    /**
     * Check if currently processing a result
     */
    fun isProcessing(): Boolean = isProcessingResult

    /**
     * Get recognition statistics
     */
    fun getRecognitionStats(): Map<String, Any> {
        return mapOf(
            "isProcessing" to isProcessingResult,
            "hasResultListener" to (onResultListener != null),
            "hasPartialListener" to (onPartialResultListener != null),
            "confidenceThreshold" to (if (::config.isInitialized) config.confidenceThreshold else 0f),
            "currentMode" to resultProcessor.getMode().toString()
        )
    }

    /**
     * Reset recognizer state
     */
    fun reset() {
        Log.d(TAG, "Resetting recognizer")

        cancelProcessing()
        _resultFlow.value = null
        isProcessingResult = false

        // Clear listeners
        onResultListener = null
        onPartialResultListener = null
    }

    /**
     * Destroy recognizer and clean up resources
     */
    fun destroy() {
        Log.d(TAG, "Destroying recognizer")

        cancelProcessing()
        reset()
    }
}

/**
 * Result of recognition processing with action indicators
 */
data class RecognitionProcessingResult(
    val action: ProcessingAction,
    val result: RecognitionResult? = null,
    val command: String? = null,
    val confidence: Int? = null,
    val error: String? = null
) {

    enum class ProcessingAction {
        NO_RESULT,
        REGULAR_COMMAND,
        MUTE_COMMAND,
        UNMUTE_COMMAND,
        DICTATION_START,
        DICTATION_CONTINUE,
        DICTATION_END,
        LOW_CONFIDENCE,
        ERROR
    }

    companion object {
        fun noResult() = RecognitionProcessingResult(ProcessingAction.NO_RESULT)

        fun regularCommand(result: RecognitionResult) =
            RecognitionProcessingResult(ProcessingAction.REGULAR_COMMAND, result = result)

        fun muteCommand() = RecognitionProcessingResult(ProcessingAction.MUTE_COMMAND)

        fun unmuteCommand() = RecognitionProcessingResult(ProcessingAction.UNMUTE_COMMAND)

        fun dictationStart() = RecognitionProcessingResult(ProcessingAction.DICTATION_START)

        fun dictationContinue(result: RecognitionResult) =
            RecognitionProcessingResult(ProcessingAction.DICTATION_CONTINUE, result = result)

        fun dictationEnd() = RecognitionProcessingResult(ProcessingAction.DICTATION_END)

        fun lowConfidence(command: String, confidence: Int) =
            RecognitionProcessingResult(ProcessingAction.LOW_CONFIDENCE, command = command, confidence = confidence)

        fun error(message: String) =
            RecognitionProcessingResult(ProcessingAction.ERROR, error = message)
    }

    // Helper methods for checking action types
    fun isCommand(): Boolean = action == ProcessingAction.REGULAR_COMMAND
    fun isMute(): Boolean = action == ProcessingAction.MUTE_COMMAND
    fun isUnmute(): Boolean = action == ProcessingAction.UNMUTE_COMMAND
    fun isDictationStart(): Boolean = action == ProcessingAction.DICTATION_START
    fun isDictationContinue(): Boolean = action == ProcessingAction.DICTATION_CONTINUE
    fun isDictationEnd(): Boolean = action == ProcessingAction.DICTATION_END
    fun isLowConfidence(): Boolean = action == ProcessingAction.LOW_CONFIDENCE
    fun isError(): Boolean = action == ProcessingAction.ERROR
    fun hasResult(): Boolean = result != null
}
