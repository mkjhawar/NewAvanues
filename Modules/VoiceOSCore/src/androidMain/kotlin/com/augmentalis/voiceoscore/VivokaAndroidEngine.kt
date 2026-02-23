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
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
                // Set up result listener - converts RecognitionResult to SpeechResult
                vivokaEngine?.setResultListener { result ->
                    scope.launch {
                        _results.emit(toSpeechResult(result))
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
            // Skip if commands haven't changed (hash comparison)
            val newHash = commands.sorted().hashCode()
            val oldHash = lastCommandsHash.get()
            if (newHash == oldHash) {
                return Result.success(Unit)
            }

            vivokaEngine?.setDynamicCommands(commands)
            lastCommandsHash.set(newHash)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateConfiguration(config: SpeechConfig): Result<Unit> {
        // Configuration updates would require re-initialization
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

    override suspend fun enableWakeWord(wakeWord: String): Result<Unit> {
        _isWakeWordEnabled.value = true
        return Result.success(Unit)
    }

    override suspend fun disableWakeWord(): Result<Unit> {
        _isWakeWordEnabled.value = false
        return Result.success(Unit)
    }

    override fun getAvailableWakeWords(): List<String> {
        return listOf("Hey Ava", "OK Ava", "Ava")
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
