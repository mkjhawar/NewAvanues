/**
 * VoskEngineImpl.kt - Real VOSK engine implementation for Android
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2026-01-07
 *
 * Production implementation of ISpeechEngine using VOSK offline speech recognition.
 * Uses Kotlin flows for reactive result delivery and supports grammar-based recognition.
 *
 * Features:
 * - Fully offline recognition (no network required)
 * - Grammar-based recognition for voice commands
 * - Continuous recognition mode
 * - Partial/streaming results
 * - Dynamic vocabulary via updateCommands()
 *
 * Requirements:
 * - RECORD_AUDIO permission
 * - VOSK model downloaded to device storage
 * - vosk-android AAR dependency
 */
package com.augmentalis.voiceoscoreng.features

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import org.vosk.Model
import org.vosk.Recognizer
import java.io.File
import java.io.IOException
import java.util.concurrent.atomic.AtomicBoolean

/**
 * VOSK-based speech recognition engine for Android.
 *
 * This implementation provides offline speech recognition using the VOSK library.
 * It supports both free-form recognition and grammar-based recognition for
 * constrained voice command scenarios.
 *
 * @param context Android application context for file access and permissions
 */
class VoskEngineImpl(
    private val context: Context
) : ISpeechEngine {

    companion object {
        private const val TAG = "VoskEngine"

        // Audio recording parameters
        private const val SAMPLE_RATE = 16000
        private const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
        private const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
        private const val BUFFER_SIZE_MULTIPLIER = 4

        // Default model directory name
        private const val DEFAULT_MODEL_DIR = "vosk-model"
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Flows (ISpeechEngine interface)
    // ═══════════════════════════════════════════════════════════════════════

    private val _state = MutableStateFlow<EngineState>(EngineState.Uninitialized)
    override val state: StateFlow<EngineState> = _state.asStateFlow()

    private val _results = MutableSharedFlow<SpeechResult>(extraBufferCapacity = 16)
    override val results: SharedFlow<SpeechResult> = _results.asSharedFlow()

    private val _errors = MutableSharedFlow<SpeechError>(extraBufferCapacity = 8)
    override val errors: SharedFlow<SpeechError> = _errors.asSharedFlow()

    // ═══════════════════════════════════════════════════════════════════════
    // Internal State
    // ═══════════════════════════════════════════════════════════════════════

    private var model: Model? = null
    private var recognizer: Recognizer? = null
    private var audioRecord: AudioRecord? = null
    private var recognitionJob: Job? = null

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val isRecording = AtomicBoolean(false)
    private var currentConfig: SpeechConfig? = null
    private var currentCommands: List<String>? = null
    private var continuousMode = false

    // Buffer for audio recording
    private var bufferSize: Int = 0

    // ═══════════════════════════════════════════════════════════════════════
    // Lifecycle Methods
    // ═══════════════════════════════════════════════════════════════════════

    override suspend fun initialize(config: SpeechConfig): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Initializing VOSK engine")
                _state.value = EngineState.Initializing

                // Check RECORD_AUDIO permission
                if (ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.RECORD_AUDIO
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    val error = "RECORD_AUDIO permission not granted"
                    Log.e(TAG, error)
                    _state.value = EngineState.Error(error, recoverable = false)
                    emitError(SpeechError.ErrorCode.PERMISSION_DENIED, error, recoverable = false)
                    return@withContext Result.failure(SecurityException(error))
                }

                currentConfig = config

                // Load VOSK model
                val modelPath = config.modelPath ?: findDefaultModel()
                if (modelPath == null) {
                    val error = "VOSK model not found. Please download a model."
                    Log.e(TAG, error)
                    _state.value = EngineState.Error(error, recoverable = false)
                    emitError(SpeechError.ErrorCode.MODEL_NOT_FOUND, error, recoverable = false)
                    return@withContext Result.failure(IOException(error))
                }

                Log.d(TAG, "Loading VOSK model from: $modelPath")
                model = Model(modelPath)

                // Create recognizer
                recognizer = Recognizer(model, SAMPLE_RATE.toFloat())
                recognizer?.setWords(true) // Enable word-level timestamps

                // Calculate buffer size
                bufferSize = AudioRecord.getMinBufferSize(
                    SAMPLE_RATE,
                    CHANNEL_CONFIG,
                    AUDIO_FORMAT
                ) * BUFFER_SIZE_MULTIPLIER

                if (bufferSize == AudioRecord.ERROR_BAD_VALUE || bufferSize == AudioRecord.ERROR) {
                    val error = "Failed to calculate audio buffer size"
                    Log.e(TAG, error)
                    _state.value = EngineState.Error(error, recoverable = false)
                    return@withContext Result.failure(IllegalStateException(error))
                }

                _state.value = EngineState.Ready(SpeechEngine.VOSK)
                Log.i(TAG, "VOSK engine initialized successfully")
                Result.success(Unit)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to initialize VOSK engine", e)
                val error = e.message ?: "Unknown initialization error"
                _state.value = EngineState.Error(error, recoverable = false)
                emitError(SpeechError.ErrorCode.UNKNOWN, error, recoverable = false)
                Result.failure(e)
            }
        }
    }

    override suspend fun startListening(): Result<Unit> {
        return try {
            if (_state.value !is EngineState.Ready && _state.value !is EngineState.Listening) {
                return Result.failure(IllegalStateException("Engine not ready: ${_state.value}"))
            }

            if (isRecording.get()) {
                Log.w(TAG, "Already recording, ignoring startListening call")
                return Result.success(Unit)
            }

            Log.d(TAG, "Starting VOSK listening")
            continuousMode = currentConfig?.mode?.supportsContinuous() == true

            // Create AudioRecord
            try {
                audioRecord = AudioRecord(
                    MediaRecorder.AudioSource.MIC,
                    SAMPLE_RATE,
                    CHANNEL_CONFIG,
                    AUDIO_FORMAT,
                    bufferSize
                )

                if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                    val error = "AudioRecord failed to initialize"
                    Log.e(TAG, error)
                    emitError(SpeechError.ErrorCode.AUDIO_ERROR, error, recoverable = true)
                    return Result.failure(IllegalStateException(error))
                }
            } catch (e: SecurityException) {
                Log.e(TAG, "Permission denied for audio recording", e)
                emitError(SpeechError.ErrorCode.PERMISSION_DENIED, e.message ?: "Permission denied", recoverable = false)
                return Result.failure(e)
            }

            // Start recording
            isRecording.set(true)
            audioRecord?.startRecording()
            _state.value = EngineState.Listening

            // Start recognition loop in coroutine
            recognitionJob = scope.launch(Dispatchers.IO) {
                processAudio()
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error starting VOSK listening", e)
            isRecording.set(false)
            emitError(SpeechError.ErrorCode.AUDIO_ERROR, e.message ?: "Failed to start", recoverable = true)
            Result.failure(e)
        }
    }

    override suspend fun stopListening() {
        Log.d(TAG, "Stopping VOSK listening")
        isRecording.set(false)

        // Cancel recognition job
        recognitionJob?.cancelAndJoin()
        recognitionJob = null

        // Stop and release AudioRecord
        try {
            audioRecord?.stop()
            audioRecord?.release()
            audioRecord = null
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping audio record", e)
        }

        // Get final result if any
        recognizer?.let { rec ->
            val finalResult = rec.finalResult
            parseFinalResult(finalResult)
        }

        if (_state.value is EngineState.Listening || _state.value is EngineState.Processing) {
            _state.value = EngineState.Ready(SpeechEngine.VOSK)
        }
    }

    override suspend fun updateCommands(commands: List<String>): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                if (commands.isEmpty()) {
                    // Reset to free-form recognition
                    Log.d(TAG, "Clearing grammar, switching to free-form recognition")
                    currentCommands = null
                    recreateRecognizer(null)
                    return@withContext Result.success(Unit)
                }

                Log.d(TAG, "Updating commands: ${commands.size} commands")
                currentCommands = commands

                // Build VOSK grammar JSON
                // VOSK grammar format: ["command one", "command two", "[unk]"]
                val grammar = buildGrammarJson(commands)
                Log.d(TAG, "Grammar JSON: $grammar")

                // Recreate recognizer with grammar
                recreateRecognizer(grammar)

                Result.success(Unit)
            } catch (e: Exception) {
                Log.e(TAG, "Error updating commands", e)
                Result.failure(e)
            }
        }
    }

    override suspend fun updateConfiguration(config: SpeechConfig): Result<Unit> {
        currentConfig = config

        // If model path changed, need to reinitialize
        if (config.modelPath != null && config.modelPath != currentConfig?.modelPath) {
            Log.d(TAG, "Model path changed, reinitializing")
            destroy()
            return initialize(config)
        }

        Log.d(TAG, "Configuration updated")
        return Result.success(Unit)
    }

    override fun isRecognizing(): Boolean = isRecording.get()

    override fun isInitialized(): Boolean = model != null && recognizer != null

    override fun getEngineType(): SpeechEngine = SpeechEngine.VOSK

    override fun getSupportedFeatures(): Set<EngineFeature> = setOf(
        EngineFeature.OFFLINE_MODE,
        EngineFeature.CUSTOM_VOCABULARY,
        EngineFeature.CONTINUOUS_RECOGNITION,
        EngineFeature.WORD_TIMESTAMPS
    )

    override suspend fun destroy() {
        Log.d(TAG, "Destroying VOSK engine")

        // Stop recording
        stopListening()

        // Release recognizer
        try {
            recognizer?.close()
            recognizer = null
        } catch (e: Exception) {
            Log.e(TAG, "Error closing recognizer", e)
        }

        // Release model
        try {
            model?.close()
            model = null
        } catch (e: Exception) {
            Log.e(TAG, "Error closing model", e)
        }

        currentConfig = null
        currentCommands = null

        _state.value = EngineState.Destroyed
        Log.i(TAG, "VOSK engine destroyed")
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Audio Processing
    // ═══════════════════════════════════════════════════════════════════════

    private suspend fun processAudio() {
        val buffer = ShortArray(bufferSize / 2)

        Log.d(TAG, "Starting audio processing loop")

        while (isRecording.get() && scope.isActive) {
            val read = audioRecord?.read(buffer, 0, buffer.size) ?: 0

            if (read > 0) {
                recognizer?.let { rec ->
                    // Convert shorts to bytes for VOSK
                    val bytes = shortArrayToByteArray(buffer, read)

                    // Feed audio to recognizer
                    if (rec.acceptWaveForm(bytes, bytes.size)) {
                        // Final result for this utterance
                        val result = rec.result
                        parseFinalResult(result)

                        // In continuous mode, keep going
                        if (!continuousMode) {
                            isRecording.set(false)
                        }
                    } else {
                        // Partial result
                        val partialResult = rec.partialResult
                        parsePartialResult(partialResult)
                    }
                }
            } else if (read < 0) {
                Log.e(TAG, "AudioRecord read error: $read")
                emitError(SpeechError.ErrorCode.AUDIO_ERROR, "Audio read error: $read", recoverable = true)
                isRecording.set(false)
            }
        }

        Log.d(TAG, "Audio processing loop ended")
    }

    private fun shortArrayToByteArray(shorts: ShortArray, count: Int): ByteArray {
        val bytes = ByteArray(count * 2)
        for (i in 0 until count) {
            bytes[i * 2] = (shorts[i].toInt() and 0xFF).toByte()
            bytes[i * 2 + 1] = (shorts[i].toInt() shr 8 and 0xFF).toByte()
        }
        return bytes
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Result Parsing
    // ═══════════════════════════════════════════════════════════════════════

    private fun parseFinalResult(jsonResult: String?) {
        if (jsonResult.isNullOrBlank()) return

        try {
            val json = JSONObject(jsonResult)
            val text = json.optString("text", "").trim()

            if (text.isNotEmpty()) {
                Log.d(TAG, "Final result: '$text'")

                // Calculate confidence (VOSK doesn't always provide per-word confidence)
                val confidence = calculateConfidence(json)

                scope.launch {
                    _results.emit(
                        SpeechResult(
                            text = text,
                            confidence = confidence,
                            isFinal = true,
                            timestamp = System.currentTimeMillis(),
                            alternatives = parseAlternatives(json)
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing final result: $jsonResult", e)
        }
    }

    private fun parsePartialResult(jsonResult: String?) {
        if (jsonResult.isNullOrBlank()) return

        try {
            val json = JSONObject(jsonResult)
            val partial = json.optString("partial", "").trim()

            if (partial.isNotEmpty()) {
                Log.d(TAG, "Partial result: '$partial'")

                scope.launch {
                    _results.emit(
                        SpeechResult(
                            text = partial,
                            confidence = 0.5f, // Partial results have lower confidence
                            isFinal = false,
                            timestamp = System.currentTimeMillis(),
                            alternatives = emptyList()
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing partial result: $jsonResult", e)
        }
    }

    private fun calculateConfidence(json: JSONObject): Float {
        // Try to get confidence from result array
        val result = json.optJSONArray("result")
        if (result != null && result.length() > 0) {
            var totalConf = 0.0f
            var count = 0
            for (i in 0 until result.length()) {
                val word = result.optJSONObject(i)
                val conf = word?.optDouble("conf", 0.8)?.toFloat() ?: 0.8f
                totalConf += conf
                count++
            }
            if (count > 0) {
                return totalConf / count
            }
        }
        // Default confidence for VOSK results
        return 0.85f
    }

    private fun parseAlternatives(json: JSONObject): List<SpeechResult.Alternative> {
        // VOSK typically returns a single best result, but some models support alternatives
        val alternatives = mutableListOf<SpeechResult.Alternative>()

        val altArray = json.optJSONArray("alternatives")
        if (altArray != null) {
            for (i in 0 until altArray.length()) {
                val alt = altArray.optJSONObject(i)
                if (alt != null) {
                    val text = alt.optString("text", "")
                    val conf = alt.optDouble("confidence", 0.5).toFloat()
                    if (text.isNotEmpty()) {
                        alternatives.add(SpeechResult.Alternative(text, conf))
                    }
                }
            }
        }

        return alternatives
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Grammar Support
    // ═══════════════════════════════════════════════════════════════════════

    private fun buildGrammarJson(commands: List<String>): String {
        val grammarArray = JSONArray()

        // Add each command to the grammar
        commands.forEach { command ->
            grammarArray.put(command.lowercase())
        }

        // Add [unk] to allow partial matches and unknown words
        grammarArray.put("[unk]")

        return grammarArray.toString()
    }

    private fun recreateRecognizer(grammar: String?) {
        val oldRecognizer = recognizer
        recognizer = if (grammar != null) {
            Recognizer(model, SAMPLE_RATE.toFloat(), grammar)
        } else {
            Recognizer(model, SAMPLE_RATE.toFloat())
        }
        recognizer?.setWords(true)

        // Close old recognizer
        try {
            oldRecognizer?.close()
        } catch (e: Exception) {
            Log.e(TAG, "Error closing old recognizer", e)
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Model Management
    // ═══════════════════════════════════════════════════════════════════════

    private fun findDefaultModel(): String? {
        // Check common model locations
        val locations = listOf(
            // App files directory
            File(context.filesDir, DEFAULT_MODEL_DIR),
            // External files directory
            context.getExternalFilesDir(null)?.let { File(it, DEFAULT_MODEL_DIR) },
            // Cache directory (not recommended, may be cleared)
            File(context.cacheDir, DEFAULT_MODEL_DIR)
        ).filterNotNull()

        for (location in locations) {
            if (location.exists() && location.isDirectory) {
                Log.d(TAG, "Found model at: ${location.absolutePath}")
                return location.absolutePath
            }
        }

        // Check if there's any model-* directory
        context.filesDir.listFiles()?.forEach { file ->
            if (file.isDirectory && file.name.startsWith("model")) {
                Log.d(TAG, "Found model at: ${file.absolutePath}")
                return file.absolutePath
            }
        }

        Log.w(TAG, "No VOSK model found in standard locations")
        return null
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Error Handling
    // ═══════════════════════════════════════════════════════════════════════

    private fun emitError(code: SpeechError.ErrorCode, message: String, recoverable: Boolean) {
        scope.launch {
            _errors.emit(
                SpeechError(
                    code = code,
                    message = message,
                    recoverable = recoverable,
                    timestamp = System.currentTimeMillis()
                )
            )
        }
    }
}
