/**
 * VivokaAndroidEngine.kt - Android Vivoka implementation
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Created: 2026-01-06
 * Updated: 2026-01-08 - Use VivokaEngine (same as VoiceOSCore) for consistency
 *
 * Android-only implementation that delegates to VivokaEngine from SpeechRecognition library.
 * Uses the same VivokaEngine class that VoiceOSCore uses (proven working).
 * Provides ISpeechEngine interface for KMP compatibility.
 *
 * Models are loaded from external storage to reduce APK size.
 */
package com.augmentalis.voiceoscore

import android.content.Context
import com.augmentalis.speechrecognition.SpeechConfig as SRSpeechConfig
import com.augmentalis.speechrecognition.SpeechMode as SRSpeechMode
import com.augmentalis.speechrecognition.RecognitionResult
import com.augmentalis.speechrecognition.vivoka.VivokaEngine
import com.augmentalis.speechrecognition.vivoka.VivokaPathResolver
import com.augmentalis.speechrecognition.vivoka.model.VsdkHandlerUtils
import com.vivoka.vsdk.Constants
import com.vivoka.vsdk.util.AssetsExtractor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.util.Log
import java.io.File
import java.util.concurrent.atomic.AtomicReference

/**
 * Status of the Vivoka SDK models on external storage.
 */
sealed class VivokaModelStatus {
    /** Models are present and ready to use */
    object Ready : VivokaModelStatus()

    /** Models not found at expected location */
    data class Missing(
        val expectedPath: String,
        val searchedPaths: List<String>
    ) : VivokaModelStatus()

    /** Models are being downloaded */
    data class Downloading(val progress: Int) : VivokaModelStatus()

    /** Error checking model status */
    data class Error(val message: String) : VivokaModelStatus()
}

/**
 * Android Vivoka engine implementation.
 *
 * Delegates to VivokaEngine from SpeechRecognition library.
 * Provides KMP-compatible ISpeechEngine interface.
 *
 * Models are loaded from external storage paths:
 * - /sdcard/.voiceos/vivoka/vsdk/ (recommended, survives uninstall)
 * - /sdcard/Android/data/{app}/files/vsdk/
 * - /data/data/{app}/files/vsdk/
 *
 * @param context Android application context
 * @param config Vivoka configuration
 */
class VivokaAndroidEngine(
    private val context: Context,
    private val config: VivokaConfig
) : IVivokaEngine {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // State flows for KMP interface
    private val _state = MutableStateFlow<EngineState>(EngineState.Uninitialized)
    /**
     * FIX: Increased SharedFlow buffer capacity to prevent emitter blocking.
     *
     * - replay = 1: Latest result available to new collectors
     * - extraBufferCapacity = 64: Allows up to 64 emissions to buffer before
     *   suspending the emitter. This prevents the emit() call from blocking
     *   even if the collector is temporarily slow (e.g., during heavy
     *   accessibility event processing).
     *
     * The collector side also adds buffer(64) for additional protection.
     */
    private val _results = MutableSharedFlow<SpeechResult>(
        replay = 1,
        extraBufferCapacity = 64
    )
    private val _errors = MutableSharedFlow<SpeechError>(
        replay = 1,
        extraBufferCapacity = 16
    )
    private val _isWakeWordEnabled = MutableStateFlow(false)
    private val _wakeWordDetected = MutableSharedFlow<WakeWordEvent>(
        replay = 1,
        extraBufferCapacity = 8
    )
    private val _availableModels = MutableStateFlow<List<VivokaModel>>(emptyList())
    private val _currentModel = MutableStateFlow<VivokaModel?>(null)
    private val _modelStatus = MutableStateFlow<VivokaModelStatus>(VivokaModelStatus.Missing("", emptyList()))

    override val state: StateFlow<EngineState> = _state.asStateFlow()
    override val results: SharedFlow<SpeechResult> = _results.asSharedFlow()
    override val errors: SharedFlow<SpeechError> = _errors.asSharedFlow()
    override val isWakeWordEnabled: StateFlow<Boolean> = _isWakeWordEnabled.asStateFlow()
    override val wakeWordDetected: SharedFlow<WakeWordEvent> = _wakeWordDetected.asSharedFlow()
    override val availableModels: StateFlow<List<VivokaModel>> = _availableModels.asStateFlow()
    override val currentModel: StateFlow<VivokaModel?> = _currentModel.asStateFlow()

    /** Model status flow for UI to observe */
    val modelStatus: StateFlow<VivokaModelStatus> = _modelStatus.asStateFlow()

    // Direct reference to SpeechRecognition's VivokaEngine (same as VoiceOSCore uses)
    private var vivokaEngine: VivokaEngine? = null
    private val pathResolver = VivokaPathResolver(context)

    /** Track last command set hash to skip truly redundant Vivoka grammar updates */
    private val lastCommandsHash = AtomicReference<Int>(0)

    // ==================== Wake Word State ====================

    companion object {
        private const val TAG = "VivokaAndroidEngine"
    }

    /** The wake phrase currently being listened for (e.g. "hey ava") */
    private var activeWakeWord: String? = null

    /** Sensitivity threshold for wake word detection (0.0-1.0) */
    private var wakeWordSensitivity: Float = 0.5f

    /** Job for the command window timeout — cancels when a command is received */
    private var wakeWordTimeoutJob: Job? = null

    /** Whether we're in the post-detection command window (full grammar active) */
    private var isInCommandWindow: Boolean = false

    /** Cached copy of the full command list for recompilation after command window */
    private val lastCommandList = AtomicReference<List<String>>(emptyList())

    /** Timestamp when command window opened — used to measure response time for adaptive timing */
    @Volatile
    private var commandWindowOpenedAt: Long = 0L

    /**
     * Check if Vivoka models are available at external storage paths.
     * Call this before initialize() to show appropriate UI.
     */
    fun checkModelStatus(): VivokaModelStatus {
        val vsdkPath = pathResolver.resolveVsdkPath()
        val isValid = isValidVsdkDirectory(vsdkPath)

        return if (isValid) {
            _modelStatus.value = VivokaModelStatus.Ready
            VivokaModelStatus.Ready
        } else {
            val status = VivokaModelStatus.Missing(
                expectedPath = pathResolver.getManualDeploymentPath().absolutePath,
                searchedPaths = listOf(pathResolver.getSearchPathsForLogging())
            )
            _modelStatus.value = status
            status
        }
    }

    /**
     * Get the recommended path for manual model deployment.
     * Users can copy VSDK files to this location.
     */
    fun getManualDeploymentPath(): String {
        return pathResolver.getManualDeploymentPath().absolutePath
    }

    private fun isValidVsdkDirectory(dir: File): Boolean {
        if (!dir.exists() || !dir.isDirectory) return false
        val configFile = File(dir, "config/vsdk.json")
        return configFile.exists()
    }

    /**
     * Convert KMP SpeechConfig to SpeechRecognition's SpeechConfig.
     */
    private fun toSRSpeechConfig(config: SpeechConfig): SRSpeechConfig {
        return SRSpeechConfig(
            language = config.language,
            mode = when (config.mode) {
                SpeechMode.STATIC_COMMAND -> SRSpeechMode.STATIC_COMMAND
                SpeechMode.DYNAMIC_COMMAND -> SRSpeechMode.DYNAMIC_COMMAND
                SpeechMode.COMBINED_COMMAND -> SRSpeechMode.STATIC_COMMAND  // Combined logic handled by VoiceOSCore coordinator; engine uses restricted grammar
                SpeechMode.DICTATION -> SRSpeechMode.DICTATION
                SpeechMode.FREE_SPEECH -> SRSpeechMode.FREE_SPEECH
                SpeechMode.MUTED -> SRSpeechMode.STATIC_COMMAND  // Muted = restricted wake-only grammar
                SpeechMode.HYBRID -> SRSpeechMode.HYBRID
            },
            voiceEnabled = true,
            muteCommand = "go to sleep",
            unmuteCommand = "wake up",
            startDictationCommand = "start dictation",
            stopDictationCommand = "stop dictation"
        )
    }

    /**
     * Convert SpeechRecognition's RecognitionResult to KMP SpeechResult.
     */
    private fun toSpeechResult(result: RecognitionResult): SpeechResult {
        return SpeechResult(
            text = result.text,
            confidence = result.confidence,
            isFinal = !result.isPartial,
            timestamp = result.timestamp
        )
    }

    override suspend fun initialize(config: SpeechConfig): Result<Unit> {
        return try {
            _state.value = EngineState.Initializing

            // Check model status first
            val status = checkModelStatus()
            if (status is VivokaModelStatus.Missing) {
                val extract = initializeEnglishModels()
                if (!extract){
                    _state.value = EngineState.Error(
                        message = "Vivoka models not found. Please place VSDK files at: ${status.expectedPath}",
                        recoverable = true
                    )
                    return Result.failure(
                        IllegalStateException("Models missing at: ${status.expectedPath}")
                    )
                }
            }

            // Create and initialize VivokaEngine from SpeechRecognition library
            // Using VivokaEngine (same as VoiceOSCore) instead of VivokaAndroidEngine
            vivokaEngine = VivokaEngine(context)

            // Convert to SpeechRecognition's SpeechConfig format
            val srConfig = toSRSpeechConfig(config)

            val success = vivokaEngine?.initialize(srConfig) ?: false

            if (success) {
                // Wire adaptive processing delay from AdaptiveTimingManager
                vivokaEngine?.setProcessingDelay(AdaptiveTimingManager.getProcessingDelayMs())

                // Set up result listener — intercepts wake word results, passes others through
                vivokaEngine?.setResultListener { result ->
                    scope.launch {
                        handleRecognitionResult(result)
                    }
                }

                // Set up error listener (KMP API uses SpeechError object)
                vivokaEngine?.setErrorListener { error ->
                    scope.launch {
                        _errors.emit(SpeechError(
                            code = SpeechError.ErrorCode.RECOGNITION_FAILED,
                            message = error.message,
                            recoverable = error.isRecoverable
                        ))
                    }
                }

                _state.value = EngineState.Ready(SpeechEngine.VIVOKA)
                refreshAvailableModels()
                Result.success(Unit)
            } else {
                _state.value = EngineState.Error(
                    message = "Failed to initialize Vivoka engine",
                    recoverable = false
                )
                Result.failure(RuntimeException("Vivoka initialization failed"))
            }
        } catch (e: Exception) {
            _state.value = EngineState.Error(
                message = "Failed to initialize Vivoka: ${e.message}",
                recoverable = false
            )
            Result.failure(e)
        }
    }

    private suspend fun initializeEnglishModels(): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val assetsPath = "${context.filesDir.absolutePath}${Constants.vsdkPath}"
                val vsdkHandlerUtils = VsdkHandlerUtils(assetsPath)

                if (!vsdkHandlerUtils.checkVivokaFilesExist()) {
                    AssetsExtractor.extract(context, "vsdk", assetsPath)
                }
                true
            } catch (e: com.vivoka.vsdk.Exception) {
                e.printStackTrace()
                false
            }
        }
    }

    override suspend fun startListening(): Result<Unit> {
        return try {
            if (_state.value !is EngineState.Ready) {
                return Result.failure(IllegalStateException("Engine not ready"))
            }

            _state.value = EngineState.Listening
            vivokaEngine?.startListening()
            Result.success(Unit)
        } catch (e: Exception) {
            _errors.emit(SpeechError(
                code = SpeechError.ErrorCode.AUDIO_ERROR,
                message = "Failed to start listening: ${e.message}",
                recoverable = true
            ))
            Result.failure(e)
        }
    }

    override suspend fun stopListening() {
        try {
            vivokaEngine?.stopListening()
            _state.value = EngineState.Ready(SpeechEngine.VIVOKA)
        } catch (e: Exception) {
            _errors.tryEmit(SpeechError(
                code = SpeechError.ErrorCode.AUDIO_ERROR,
                message = "Failed to stop listening: ${e.message}",
                recoverable = true
            ))
        }
    }

    /**
     * Update dynamic commands with Vivoka grammar.
     *
     * Uses hash comparison to skip redundant updates (same command set).
     * Concurrent calls are handled by Vivoka's internal compilationMutex —
     * the last call always wins, which is the correct behavior for screen changes.
     */
    override suspend fun updateCommands(commands: List<String>): Result<Unit> {
        return try {
            // Refresh processing delay from AdaptiveTimingManager (may have adapted)
            vivokaEngine?.setProcessingDelay(AdaptiveTimingManager.getProcessingDelayMs())

            // Always cache the full command list for wake-word → command-mode recompilation
            lastCommandList.set(commands.toList())

            // If wake word is active and we're NOT in the command window,
            // don't push the full grammar — keep the restricted wake-word grammar
            if (_isWakeWordEnabled.value && !isInCommandWindow) {
                Log.d(TAG, "updateCommands: wake word active, caching ${commands.size} commands (grammar unchanged)")
                return Result.success(Unit)
            }

            // Skip if commands haven't changed (hash comparison)
            val newHash = commands.sorted().hashCode()
            val oldHash = lastCommandsHash.get()
            if (newHash == oldHash) {
                return Result.success(Unit)
            }

            val compileStart = System.currentTimeMillis()
            val compiled = vivokaEngine?.setDynamicCommandsAwait(commands) ?: true
            val compileDuration = System.currentTimeMillis() - compileStart
            // Only cache hash if compilation succeeded, so next call retries on failure
            if (compiled) {
                lastCommandsHash.set(newHash)
                // Feed grammar compile time to adaptive timing
                AdaptiveTimingManager.recordGrammarCompile(compileDuration)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateConfiguration(config: SpeechConfig): Result<Unit> {
        // Clear command hash so next updateCommands() always pushes grammar
        // after a mode change (prevents stale hash from skipping the update)
        lastCommandsHash.set(0)
        return Result.success(Unit)
    }

    override fun isRecognizing(): Boolean = _state.value is EngineState.Listening

    override fun isInitialized(): Boolean = _state.value is EngineState.Ready

    override fun getEngineType(): SpeechEngine = SpeechEngine.VIVOKA

    override fun getSupportedFeatures(): Set<EngineFeature> = setOf(
        EngineFeature.OFFLINE_MODE,
        EngineFeature.CONTINUOUS_RECOGNITION,
        EngineFeature.WAKE_WORD,
        EngineFeature.CUSTOM_VOCABULARY
    )

    override suspend fun destroy() {
        try {
            vivokaEngine?.destroy()
            vivokaEngine = null
            _state.value = EngineState.Destroyed
        } catch (e: Exception) {
            _state.value = EngineState.Error(
                message = "Failed to destroy: ${e.message}",
                recoverable = false
            )
        }
    }

    override suspend fun loadModel(modelId: String): Result<Unit> {
        // Model loading is handled by VivokaEngine internally
        _currentModel.value = _availableModels.value.find { it.id == modelId }
        return Result.success(Unit)
    }

    override suspend fun unloadModel(): Result<Unit> {
        _currentModel.value = null
        return Result.success(Unit)
    }

    override suspend fun enableWakeWord(wakeWord: String, sensitivity: Float): Result<Unit> {
        return try {
            Log.i(TAG, "enableWakeWord: phrase='$wakeWord', sensitivity=$sensitivity")
            activeWakeWord = wakeWord
            wakeWordSensitivity = sensitivity.coerceIn(0.1f, 0.9f)
            isInCommandWindow = false
            wakeWordTimeoutJob?.cancel()

            // Compile a restricted grammar containing ONLY the wake phrase.
            // This mirrors handleMuteCommand() which compiles only "wake up".
            val wakeGrammar = listOf(wakeWord)
            val compiled = vivokaEngine?.setDynamicCommandsAwait(wakeGrammar) ?: false
            if (!compiled) {
                Log.e(TAG, "enableWakeWord: grammar compilation failed for '$wakeWord'")
                return Result.failure(RuntimeException("Wake word grammar compilation failed"))
            }

            // Clear the command hash so that when we later recompile full grammar,
            // the hash check doesn't skip it
            lastCommandsHash.set(0)
            _isWakeWordEnabled.value = true

            Log.i(TAG, "enableWakeWord: active, listening for '$wakeWord'")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "enableWakeWord: failed", e)
            Result.failure(e)
        }
    }

    override suspend fun disableWakeWord(): Result<Unit> {
        return try {
            Log.i(TAG, "disableWakeWord: deactivating")
            _isWakeWordEnabled.value = false
            isInCommandWindow = false
            wakeWordTimeoutJob?.cancel()
            wakeWordTimeoutJob = null
            activeWakeWord = null

            // Recompile full command grammar so normal recognition resumes
            val commands = lastCommandList.get()
            if (commands.isNotEmpty()) {
                lastCommandsHash.set(0) // force recompilation
                vivokaEngine?.setDynamicCommandsAwait(commands)
            }

            Log.i(TAG, "disableWakeWord: full grammar restored (${commands.size} commands)")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "disableWakeWord: failed", e)
            Result.failure(e)
        }
    }

    override fun getAvailableWakeWords(): List<String> {
        return listOf("hey ava", "ok ava", "computer")
    }

    // ==================== Wake Word Internal Logic ====================

    /**
     * Central result handler — intercepts wake-word matches when in wake-word mode,
     * passes all other results through to the normal SpeechResult flow.
     */
    private suspend fun handleRecognitionResult(result: RecognitionResult) {
        val wakeWord = activeWakeWord

        if (_isWakeWordEnabled.value && wakeWord != null && !isInCommandWindow) {
            // In wake-word-only grammar mode — check if result matches wake phrase
            val resultText = result.text.trim().lowercase()
            val wakePhrase = wakeWord.trim().lowercase()

            if (resultText == wakePhrase && result.confidence >= wakeWordSensitivity) {
                Log.i(TAG, "Wake word detected: '$resultText' (confidence=${result.confidence})")
                transitionToCommandMode(wakeWord, result.confidence)
                return // consumed — don't emit as normal result
            } else {
                // Low confidence or non-match on restricted grammar — ignore
                Log.v(TAG, "Wake word miss: '$resultText' (confidence=${result.confidence}, need>=$wakeWordSensitivity)")
                return
            }
        }

        if (_isWakeWordEnabled.value && isInCommandWindow) {
            // In command window — pass result through AND reset the timeout
            // (the user is actively speaking commands)
            wakeWordTimeoutJob?.cancel()

            // Record wake word hit with response time for adaptive timing
            val responseTime = System.currentTimeMillis() - commandWindowOpenedAt
            if (responseTime > 0 && commandWindowOpenedAt > 0) {
                AdaptiveTimingManager.recordWakeWordHit(responseTime)
            }

            _results.emit(toSpeechResult(result))
            // Restart the command window timeout after each command
            startCommandWindowTimeout()
            return
        }

        // Normal mode — pass through
        _results.emit(toSpeechResult(result))
    }

    /**
     * Transition from wake-word-only grammar to full command grammar.
     * Emits WakeWordEvent, recompiles grammar, starts command window timeout.
     */
    private suspend fun transitionToCommandMode(detectedPhrase: String, confidence: Float) {
        Log.i(TAG, "transitionToCommandMode: wake word confirmed, opening command window")

        // Emit detection event for UI/service layer
        _wakeWordDetected.emit(
            WakeWordEvent(
                wakeWord = detectedPhrase,
                confidence = confidence,
                timestamp = System.currentTimeMillis()
            )
        )

        isInCommandWindow = true
        commandWindowOpenedAt = System.currentTimeMillis()

        // Recompile with full command grammar
        val commands = lastCommandList.get()
        if (commands.isNotEmpty()) {
            lastCommandsHash.set(0) // force recompilation
            val compiled = vivokaEngine?.setDynamicCommandsAwait(commands) ?: false
            if (!compiled) {
                Log.w(TAG, "transitionToCommandMode: full grammar compilation failed, returning to wake mode")
                returnToWakeWordMode()
                return
            }
            Log.d(TAG, "transitionToCommandMode: full grammar compiled (${commands.size} commands)")
        } else {
            Log.w(TAG, "transitionToCommandMode: no cached commands, staying in command window anyway")
        }

        // Start the command window timeout
        startCommandWindowTimeout()
    }

    /**
     * Start (or restart) the command window timeout.
     * Uses adaptive duration from AdaptiveTimingManager (learns from user behavior).
     */
    private fun startCommandWindowTimeout() {
        wakeWordTimeoutJob?.cancel()
        val windowMs = AdaptiveTimingManager.getCommandWindowMs()
        wakeWordTimeoutJob = scope.launch {
            delay(windowMs)
            Log.i(TAG, "Command window timeout (${windowMs}ms) — returning to wake word mode")
            AdaptiveTimingManager.recordWakeWordTimeout()
            returnToWakeWordMode()
        }
    }

    /**
     * Return from command mode to wake-word-only grammar.
     * Recompiles the restricted grammar containing only the wake phrase.
     */
    private suspend fun returnToWakeWordMode() {
        isInCommandWindow = false
        wakeWordTimeoutJob?.cancel()
        wakeWordTimeoutJob = null

        val wakeWord = activeWakeWord
        if (wakeWord != null && _isWakeWordEnabled.value) {
            val compiled = vivokaEngine?.setDynamicCommandsAwait(listOf(wakeWord)) ?: false
            lastCommandsHash.set(0) // invalidate hash for next full grammar push
            if (compiled) {
                Log.d(TAG, "returnToWakeWordMode: listening for '$wakeWord' again")
            } else {
                Log.e(TAG, "returnToWakeWordMode: grammar compilation failed")
            }
        }
    }

    override suspend fun isModelDownloaded(modelId: String): Boolean {
        return checkModelStatus() is VivokaModelStatus.Ready
    }

    override suspend fun downloadModel(
        modelId: String,
        progressCallback: ((Float) -> Unit)?
    ): Result<Unit> {
        // Future: Implement model download
        // For now, return instructions to manually place files
        return Result.failure(
            UnsupportedOperationException(
                "Automatic download not yet implemented. " +
                        "Please manually place VSDK files at: ${getManualDeploymentPath()}"
            )
        )
    }

    override suspend fun deleteModel(modelId: String): Result<Unit> {
        return Result.failure(
            UnsupportedOperationException("Model deletion not supported")
        )
    }

    override suspend fun getModelsDiskUsage(): Long {
        val vsdkPath = pathResolver.resolveVsdkPath()
        return if (vsdkPath.exists()) {
            calculateDirectorySize(vsdkPath)
        } else {
            0L
        }
    }

    private fun calculateDirectorySize(dir: File): Long {
        var size = 0L
        dir.walkTopDown().forEach { file ->
            if (file.isFile) {
                size += file.length()
            }
        }
        return size
    }

    private fun refreshAvailableModels() {
        _availableModels.value = listOf(
            VivokaModel(
                id = "vivoka-en-us-general",
                name = "English (US) General",
                language = "en-US",
                sizeBytes = 500_000_000,
                isDownloaded = checkModelStatus() is VivokaModelStatus.Ready,
                version = "6.0",
                features = setOf(
                    VivokaFeature.OFFLINE_RECOGNITION,
                    VivokaFeature.WAKE_WORD,
                    VivokaFeature.CONTINUOUS_LISTENING
                )
            )
        )
    }
}
