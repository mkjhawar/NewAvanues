/**
 * AndroidVivokaEngine.kt - Android Vivoka implementation
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2026-01-06
 *
 * Android implementation of IVivokaEngine using Vivoka SDK.
 * Connects to VoiceOS/libraries/SpeechRecognition/VivokaEngine for actual SDK calls.
 */
package com.augmentalis.voiceoscoreng.speech.vivoka

import android.content.Context
import com.augmentalis.voiceoscoreng.speech.*
import kotlinx.coroutines.flow.*

/**
 * Android Vivoka engine implementation.
 *
 * This is a bridge to the full VivokaEngine in VoiceOS/libraries/SpeechRecognition.
 * It delegates actual SDK calls to that implementation while providing the
 * KMP-compatible interface.
 *
 * @param context Android application context
 * @param config Vivoka configuration
 */
class AndroidVivokaEngine(
    private val context: Context,
    private val config: VivokaConfig
) : IVivokaEngine {

    private val _state = MutableStateFlow<EngineState>(EngineState.Uninitialized)
    private val _results = MutableSharedFlow<SpeechResult>(replay = 1)
    private val _errors = MutableSharedFlow<SpeechError>(replay = 1)
    private val _isWakeWordEnabled = MutableStateFlow(false)
    private val _wakeWordDetected = MutableSharedFlow<WakeWordEvent>(replay = 1)
    private val _availableModels = MutableStateFlow<List<VivokaModel>>(emptyList())
    private val _currentModel = MutableStateFlow<VivokaModel?>(null)

    override val state: StateFlow<EngineState> = _state.asStateFlow()
    override val results: SharedFlow<SpeechResult> = _results.asSharedFlow()
    override val errors: SharedFlow<SpeechError> = _errors.asSharedFlow()
    override val isWakeWordEnabled: StateFlow<Boolean> = _isWakeWordEnabled.asStateFlow()
    override val wakeWordDetected: SharedFlow<WakeWordEvent> = _wakeWordDetected.asSharedFlow()
    override val availableModels: StateFlow<List<VivokaModel>> = _availableModels.asStateFlow()
    override val currentModel: StateFlow<VivokaModel?> = _currentModel.asStateFlow()

    // Delegate to actual Vivoka engine from SpeechRecognition library
    private var vivokaDelegate: Any? = null

    override suspend fun initialize(config: SpeechConfig): Result<Unit> {
        return try {
            _state.value = EngineState.Initializing

            // Initialize Vivoka SDK via reflection to avoid hard dependency
            val engineClass = Class.forName(
                "com.augmentalis.speechrecognition.VivokaEngine"
            )
            val constructor = engineClass.getConstructor(Context::class.java)
            vivokaDelegate = constructor.newInstance(context)

            // Initialize the delegate
            val initMethod = engineClass.getMethod("initialize")
            initMethod.invoke(vivokaDelegate)

            // Load default model if specified
            this.config.modelId?.let { modelId ->
                loadModel(modelId)
            }

            // Enable wake word if specified
            this.config.wakeWord?.let { wakeWord ->
                enableWakeWord(wakeWord)
            }

            _state.value = EngineState.Ready(SpeechEngine.VIVOKA)
            refreshAvailableModels()
            Result.success(Unit)
        } catch (e: Exception) {
            _state.value = EngineState.Error(
                message = "Failed to initialize Vivoka: ${e.message}",
                recoverable = false
            )
            Result.failure(e)
        }
    }

    override suspend fun startListening(): Result<Unit> {
        return try {
            if (_state.value !is EngineState.Ready) {
                return Result.failure(IllegalStateException("Engine not ready"))
            }

            _state.value = EngineState.Listening(SpeechEngine.VIVOKA)

            // Start delegate listening
            vivokaDelegate?.let { delegate ->
                val method = delegate.javaClass.getMethod("startListening")
                method.invoke(delegate)
            }

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

    override suspend fun stopListening(): Result<Unit> {
        return try {
            vivokaDelegate?.let { delegate ->
                val method = delegate.javaClass.getMethod("stopListening")
                method.invoke(delegate)
            }

            _state.value = EngineState.Ready(SpeechEngine.VIVOKA)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun shutdown(): Result<Unit> {
        return try {
            vivokaDelegate?.let { delegate ->
                val method = delegate.javaClass.getMethod("shutdown")
                method.invoke(delegate)
            }
            vivokaDelegate = null
            _state.value = EngineState.Uninitialized
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun loadModel(modelId: String): Result<Unit> {
        return try {
            vivokaDelegate?.let { delegate ->
                val method = delegate.javaClass.getMethod("loadModel", String::class.java)
                method.invoke(delegate, modelId)

                _currentModel.value = _availableModels.value.find { it.id == modelId }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun unloadModel(): Result<Unit> {
        return try {
            vivokaDelegate?.let { delegate ->
                val method = delegate.javaClass.getMethod("unloadModel")
                method.invoke(delegate)
            }
            _currentModel.value = null
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun enableWakeWord(wakeWord: String): Result<Unit> {
        return try {
            vivokaDelegate?.let { delegate ->
                val method = delegate.javaClass.getMethod(
                    "enableWakeWord",
                    String::class.java
                )
                method.invoke(delegate, wakeWord)
            }
            _isWakeWordEnabled.value = true
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun disableWakeWord(): Result<Unit> {
        return try {
            vivokaDelegate?.let { delegate ->
                val method = delegate.javaClass.getMethod("disableWakeWord")
                method.invoke(delegate)
            }
            _isWakeWordEnabled.value = false
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun getAvailableWakeWords(): List<String> {
        return listOf("Hey Ava", "OK Ava", "Ava")
    }

    override suspend fun isModelDownloaded(modelId: String): Boolean {
        return _availableModels.value.find { it.id == modelId }?.isDownloaded == true
    }

    override suspend fun downloadModel(
        modelId: String,
        progressCallback: ((Float) -> Unit)?
    ): Result<Unit> {
        return try {
            vivokaDelegate?.let { delegate ->
                val method = delegate.javaClass.getMethod(
                    "downloadModel",
                    String::class.java
                )
                method.invoke(delegate, modelId)
            }
            refreshAvailableModels()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteModel(modelId: String): Result<Unit> {
        return try {
            vivokaDelegate?.let { delegate ->
                val method = delegate.javaClass.getMethod(
                    "deleteModel",
                    String::class.java
                )
                method.invoke(delegate, modelId)
            }
            refreshAvailableModels()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getModelsDiskUsage(): Long {
        return try {
            vivokaDelegate?.let { delegate ->
                val method = delegate.javaClass.getMethod("getModelsDiskUsage")
                method.invoke(delegate) as? Long ?: 0L
            } ?: 0L
        } catch (e: Exception) {
            0L
        }
    }

    private fun refreshAvailableModels() {
        // Refresh from delegate
        try {
            vivokaDelegate?.let { delegate ->
                val method = delegate.javaClass.getMethod("getAvailableModels")
                @Suppress("UNCHECKED_CAST")
                val models = method.invoke(delegate) as? List<*>
                // Convert to VivokaModel (would need proper conversion)
                _availableModels.value = models?.mapNotNull { convertToVivokaModel(it) }
                    ?: getDefaultModels()
            } ?: run {
                _availableModels.value = getDefaultModels()
            }
        } catch (e: Exception) {
            _availableModels.value = getDefaultModels()
        }
    }

    private fun convertToVivokaModel(obj: Any?): VivokaModel? {
        // Convert from delegate's model type
        return try {
            obj?.let {
                val idMethod = it.javaClass.getMethod("getId")
                val nameMethod = it.javaClass.getMethod("getName")
                val id = idMethod.invoke(it) as? String ?: return null
                val name = nameMethod.invoke(it) as? String ?: id

                VivokaModel(
                    id = id,
                    name = name,
                    language = "en-US",
                    sizeBytes = 0L,
                    isDownloaded = true,
                    version = "1.0",
                    features = setOf(VivokaFeature.OFFLINE_RECOGNITION)
                )
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun getDefaultModels(): List<VivokaModel> {
        return listOf(
            VivokaModel(
                id = "vivoka-en-us-general",
                name = "English (US) General",
                language = "en-US",
                sizeBytes = 50_000_000,
                isDownloaded = false,
                version = "2.0",
                features = setOf(
                    VivokaFeature.OFFLINE_RECOGNITION,
                    VivokaFeature.WAKE_WORD,
                    VivokaFeature.CONTINUOUS_LISTENING
                )
            ),
            VivokaModel(
                id = "vivoka-en-us-commands",
                name = "English (US) Commands",
                language = "en-US",
                sizeBytes = 25_000_000,
                isDownloaded = false,
                version = "2.0",
                features = setOf(
                    VivokaFeature.OFFLINE_RECOGNITION,
                    VivokaFeature.LOW_LATENCY
                )
            )
        )
    }
}
