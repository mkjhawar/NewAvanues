package com.augmentalis.voiceos.speech.engines.vivoka

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.augmentalis.speechrecognition.SpeechConfig
import com.augmentalis.speechrecognition.SpeechMode
import com.augmentalis.voiceos.speech.api.OnSpeechErrorListener
import com.augmentalis.voiceos.speech.api.OnSpeechResultListener
import com.augmentalis.voiceos.speech.api.RecognitionResult
import com.augmentalis.voiceos.speech.engines.common.ErrorRecoveryManager
import com.augmentalis.voiceos.speech.engines.common.SdkInitializationManager
import com.augmentalis.voiceos.speech.engines.common.UniversalInitializationManager
import com.augmentalis.voiceos.speech.engines.common.VoiceStateManager
import com.augmentalis.voiceos.speech.engines.vivoka.model.FileStatus
import com.augmentalis.voiceos.speech.engines.vivoka.model.FirebaseRemoteConfigRepository
import com.augmentalis.voiceos.speech.engines.vivoka.model.VivokaLanguageRepository
import com.google.firebase.FirebaseApp
import com.vivoka.vsdk.Exception
import com.vivoka.vsdk.asr.csdk.recognizer.IRecognizerListener
import com.vivoka.vsdk.asr.csdk.recognizer.Recognizer
import com.vivoka.vsdk.asr.recognizer.RecognizerResultType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.util.Collections

/**
 * SOLID refactored Vivoka VSDK speech recognition engine
 * Orchestrates 10 specialized components while maintaining 100% functional equivalency
 */
class VivokaEngine(
    private val context: Context
) : IRecognizerListener {

    companion object {
        private const val TAG = "VivokaEngine"
        private const val PREFS_NAME = "vivoka_config_prefs"
        private const val KEY_VIVOKA_CONFIG = "vivoka_config"
        private const val VIVOKA_ENGINE_NAME = "VivokaEngine"
    }

    // Coroutines
    private val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var timeoutJob: Job? = null

    // Shared preferences for state persistence
    private val prefs: SharedPreferences? = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // Two-phase initialization: Prepared config path from Phase 1
    private var preparedConfigPath: String? = null

    // VSDK Components
    private var recognizer: Recognizer? = null

    // SOLID Components - using common components where available
    private val config = VivokaConfig(context)
    private val voiceStateManager = VoiceStateManager(context, "Vivoka")
    private val audio = VivokaAudio(context, coroutineScope)
    private val model = VivokaModel(context, coroutineScope)
    private val recognizerProcessor = VivokaRecognizer(coroutineScope)
    private val learning = VivokaLearning(context, coroutineScope)
    private val performance = VivokaPerformance(coroutineScope)
    private val errorRecoveryManager = ErrorRecoveryManager("Vivoka", context)

    // Command management
    private val registeredCommands = Collections.synchronizedList(arrayListOf<String>())

    // Error listener for propagating errors to clients
    private var errorListener: OnSpeechErrorListener? = null

    init {
        FirebaseApp.initializeApp(context)
    }

    /**
     * Initialize Vivoka engine with two-phase approach
     * Phase 1: Language model preparation (no retry, unlimited time)
     * Phase 2: VSDK initialization (with retry, quick)
     *
     * CRITICAL FIX: Eliminates race conditions by separating downloads from VSDK init
     */
    suspend fun initialize(speechConfig: SpeechConfig): Boolean {
        FirebaseApp.initializeApp(context)
        Log.d(TAG, "VIVOKA_INIT Starting two-phase initialization")

        try {
            // PHASE 1: Language Model Preparation
            // No retry logic, can take arbitrary time
            Log.d(TAG, "VIVOKA_INIT Phase 1: Preparing language models")
            val phase1StartTime = System.currentTimeMillis()

            val downloadSuccess = prepareLanguageModels(speechConfig)

            val phase1Duration = System.currentTimeMillis() - phase1StartTime
            Log.i(TAG, "VIVOKA_INIT Phase 1 completed in ${phase1Duration}ms, success=$downloadSuccess")

            if (!downloadSuccess) {
                Log.e(TAG, "VIVOKA_INIT Language model preparation failed")
                return false
            }

            // PHASE 2: VSDK Initialization
            // Wrapped in retry logic, quick operation
            Log.d(TAG, "VIVOKA_INIT Phase 2: VSDK initialization with retry logic")
            val phase2StartTime = System.currentTimeMillis()

            val initConfig = UniversalInitializationManager.InitializationConfig(
                engineName = VIVOKA_ENGINE_NAME,
                maxRetries = 2,  // Increased from 1
                initialDelayMs = 2000L,  // Increased from 1000
                maxDelayMs = 10000L,  // Increased from 8000
                backoffMultiplier = 2.0,
                jitterMs = 500L,
                timeoutMs = 60000L,  // 1 minute (vs. 30s before)
                allowDegradedMode = true
            )

            val result = UniversalInitializationManager.instance.initializeEngine(
                config = initConfig,
                context = context
            ) { _ ->
                // This function NO LONGER handles downloads
                // Downloads are already complete from Phase 1
                performVSDKInitialization(speechConfig)
            }

            val phase2Duration = System.currentTimeMillis() - phase2StartTime
            Log.i(TAG, "VIVOKA_INIT Phase 2 completed in ${phase2Duration}ms")

            return when {
                result.success && result.state == UniversalInitializationManager.InitializationState.INITIALIZED -> {
                    val totalDuration = phase1Duration + phase2Duration
                    Log.i(TAG, "VIVOKA_INIT Total initialization: ${totalDuration}ms (Phase1: ${phase1Duration}ms, Phase2: ${phase2Duration}ms)")
                    true
                }

                result.success && result.degradedMode -> {
                    Log.w(TAG, "VIVOKA_INIT Running in degraded mode: ${result.error}")
                    true // Still usable
                }

                else -> {
                    Log.e(TAG, "VIVOKA_INIT Initialization failed: ${result.error}")
                    false
                }
            }

        } catch (e: Exception) {
            Log.e(TAG, "VIVOKA_INIT Initialization failed with exception", e)
            errorListener?.invoke("Initialization failed: ${e.message}", 500)
            return false
        }
    }

    /**
     * Phase 1: Prepare language models (downloads, extraction, merging)
     * This phase is NOT retried and can take arbitrary time
     *
     * @return true if models are ready, false if preparation failed
     */
    private suspend fun prepareLanguageModels(speechConfig: SpeechConfig): Boolean {
        return try {
            Log.d(TAG, "VIVOKA_LANG_PREP Starting language model preparation")

            // Initialize performance monitoring
            performance.initialize()

            // Initialize state management
            voiceStateManager.initialize()

            // Initialize configuration
            if (!config.initialize(speechConfig)) {
                throw Exception("Configuration initialization failed")
            }

            // Give filesystem time to sync
            delay(500)

            // Get initial config path (English base config)
            var configPath: String? = config.getConfigPath()
            val targetLanguage = config.dynamicCommandLanguage
            val isLangDownloaded = VivokaLanguageRepository.isLanguageDownloaded(
                targetLanguage,
                loadPersistedConfig()
            )

            Log.i(TAG, "VIVOKA_LANG_PREP Base config: $configPath")
            Log.i(TAG, "VIVOKA_LANG_PREP Target language: $targetLanguage")
            Log.i(TAG, "VIVOKA_LANG_PREP Already downloaded: $isLangDownloaded")

            // Check if we need to download language model
            if (targetLanguage != VivokaLanguageRepository.LANGUAGE_CODE_ENGLISH_USA && !isLangDownloaded) {
                // Language download required
                configPath = downloadAndMergeLanguageModel(targetLanguage)
                Log.i(TAG, "VIVOKA_LANG_PREP downloadAndMergeLanguageModel Base config: $configPath")
                if (configPath == null) {
                    Log.e(TAG, "VIVOKA_LANG_PREP Language download/merge failed")
                    voiceStateManager.setVoiceEnabled(false)
                    return false
                }
            } else {
                Log.i(TAG, "VIVOKA_LANG_PREP Using cached/bundled model for $targetLanguage")
            }

            // Store prepared config path for Phase 2
            this.preparedConfigPath = configPath

            if (preparedConfigPath == null) {
                Log.e(TAG, "VIVOKA_LANG_PREP No valid config path available")
                voiceStateManager.setVoiceEnabled(false)
                return false
            }

            Log.i(TAG, "VIVOKA_LANG_PREP Language preparation complete: $preparedConfigPath")
            true

        } catch (e: Exception) {
            Log.e(TAG, "VIVOKA_LANG_PREP Preparation failed", e)
            errorListener?.invoke("Language preparation failed: ${e.message}", 500)
            false
        }
    }

    /**
     * Download and merge language model configuration
     * Uses BLOCKING download pattern from Avenue4
     *
     * @param languageCode Target language code (e.g., "es", "fr")
     * @return Merged config path or null if failed
     */
    private suspend fun downloadAndMergeLanguageModel(languageCode: String): String? {
        return try {
            Log.i(TAG, "VIVOKA_DOWNLOAD Starting download for language: $languageCode")

            // Initialize Firebase repository
            val firebaseRepo = FirebaseRemoteConfigRepository.getInstance(context)
                ?: throw Exception("Failed to initialize Firebase repository")

            firebaseRepo.init()

            // CRITICAL: This call BLOCKS until download completes or fails
            // Callback provides real-time status updates during the wait
            val downloadStartTime = System.currentTimeMillis()

            val configFile = firebaseRepo.getLanguageResource(
                languageId = languageCode
            ) { status ->
                // Status callback - invoked during download progress
                val elapsedTime = System.currentTimeMillis() - downloadStartTime
                Log.d(TAG, "VIVOKA_DOWNLOAD Status after ${elapsedTime}ms: $status")

                when (status) {
                    FileStatus.Completed -> {
                        Log.i(TAG, "VIVOKA_DOWNLOAD Language model download completed in ${elapsedTime}ms")

                        // Update persisted config with newly downloaded language
                        val updatedDownloadedResource = loadPersistedConfig()
                        val updateDownloadedRes = VivokaLanguageRepository.getDownloadLanguageString(
                            languageCode,
                            updatedDownloadedResource
                        )
                        persistConfig(updateDownloadedRes)

                        // Update state - download finished
                        voiceStateManager.downloadingModels(false)

                        // Notify observers
                        errorListener?.invoke(
                            "Language model downloaded successfully",
                            0  // 0 indicates success
                        )
                    }

                    is FileStatus.Downloading -> {
                        val progress = status.progress
                        Log.d(TAG, "VIVOKA_DOWNLOAD Progress: $progress%")
                        voiceStateManager.downloadingModels(true)

                        // Provide progress feedback to UI
                        errorListener?.invoke(
                            "Downloading $languageCode: $progress%",
                            -1  // -1 indicates progress update, not error
                        )
                    }

                    FileStatus.Extracting -> {
                        Log.d(TAG, "VIVOKA_DOWNLOAD Extracting model files")
                        voiceStateManager.downloadingModels(true)

                        errorListener?.invoke(
                            "Extracting $languageCode model files...",
                            -1
                        )
                    }

                    FileStatus.Initialization -> {
                        Log.d(TAG, "VIVOKA_DOWNLOAD Initializing model files")
                        voiceStateManager.downloadingModels(true)

                        errorListener?.invoke(
                            "Initializing $languageCode model...",
                            -1
                        )
                    }

                    is FileStatus.Error -> {
                        val errorType = if (status.error == com.augmentalis.voiceos.speech.engines.vivoka.model.FileError.REMOTE) "network" else "file"
                        Log.e(TAG, "VIVOKA_DOWNLOAD Download error: $errorType error")
                        voiceStateManager.downloadingModels(false)

                        errorListener?.invoke(
                            "Download failed: $errorType error",
                            503  // Service unavailable
                        )
                    }
                }
            }

            // SAFETY CHECK: Verify download succeeded
            // This code is ONLY reached AFTER getLanguageResource() completes
            if (configFile.isNullOrBlank()) {
                val errorMsg = "Language model download returned empty config"
                Log.e(TAG, "VIVOKA_DOWNLOAD $errorMsg")

                voiceStateManager.setVoiceEnabled(false)
                voiceStateManager.downloadingModels(false)
                errorListener?.invoke(errorMsg, 503)

                return null
            }

            Log.i(TAG, "VIVOKA_DOWNLOAD Download successful, merging configs")

            // Merge downloaded config with base English config
            val mergedConfigPath = config.mergeJsonFiles(configFile)

            if (mergedConfigPath == null) {
                val errorMsg = "Failed to merge language configuration files"
                Log.e(TAG, "VIVOKA_DOWNLOAD $errorMsg")

                voiceStateManager.setVoiceEnabled(false)
                errorListener?.invoke(errorMsg, 500)

                return null
            }

            mergedConfigPath

        } catch (e: Exception) {
            Log.e(TAG, "VIVOKA_DOWNLOAD Exception during download/merge", e)
            voiceStateManager.downloadingModels(false)
            errorListener?.invoke("Download failed: ${e.message}", 500)
            null
        }
    }

    /**
     * Phase 2: VSDK initialization (quick, can be retried safely)
     * Uses the config prepared in Phase 1
     *
     * @return true if VSDK initialized successfully
     */
    private suspend fun performVSDKInitialization(speechConfig: SpeechConfig): Boolean {
        return try {
            Log.d(TAG, "VIVOKA_VSDK Performing VSDK initialization")

            val initStartTime = System.currentTimeMillis()

            // Use the config path prepared in Phase 1
            val configPath = this.preparedConfigPath
                ?: throw Exception("Config path not prepared - Phase 1 must run first")

            Log.i(TAG, "VIVOKA_VSDK Using prepared config: $configPath")

            // Initialize VSDK using enhanced initialization manager
            initializeVSDK(configPath)

            // Initialize learning system
            if (!learning.initialize()) {
                Log.w(TAG, "VIVOKA_VSDK Learning system initialization failed, continuing without learning")
            }

            // Set voice enabled state
            voiceStateManager.setVoiceEnabled(speechConfig.voiceEnabled)

            // Record metrics
            val duration = System.currentTimeMillis() - initStartTime
            performance.recordVSDKInitialization(initStartTime, true, "Phase 2: VSDK init (${duration}ms)")

            Log.i(TAG, "VIVOKA_VSDK Initialization completed successfully in ${duration}ms")
            true

        } catch (e: Exception) {
            Log.e(TAG, "VIVOKA_VSDK Initialization failed", e)

            // Handle "already initialized" error gracefully
            if (e.message?.contains("Cannot call 'Vsdk.init' multiple times") == true) {
                Log.w(TAG, "VIVOKA_VSDK VSDK already initialized, recovering")
                try {
                    initializeRecognizerComponents()
                    return true
                } catch (recoveryError: Exception) {
                    Log.e(TAG, "VIVOKA_VSDK Recovery failed", recoveryError)
                    return false
                }
            }

            false
        }
    }


    /**
     * Initialize VSDK using enhanced initialization manager
     * CRITICAL FIX: Prevents "Cannot call 'Vsdk.init' multiple times" errors
     */
    private suspend fun initializeVSDK(configPath: String) {
        try {
            Log.d(TAG, "CHANGE_LANG Starting enhanced VSDK initialization with robust error handling")

            // Use the new initialization manager to handle all VSDK initialization
            val result = VivokaInitializationManager.instance.initializeVivoka(
                context = context,
                configPath = configPath
            )

            when {
                result.success && result.state == SdkInitializationManager.InitializationState.INITIALIZED -> {
                    Log.i(TAG, "CHANGE_LANG VSDK initialized successfully in ${result.initializationTime}ms")
                    // Continue with recognizer initialization
                    initializeRecognizerComponents()
                }

                result.success && result.degradedMode -> {
                    Log.w(TAG, "CHANGE_LANG VSDK running in degraded mode: ${result.error}")
                    // Initialize in limited functionality mode
                    initializeRecognizerComponentsInDegradedMode()
                }

                else -> {
                    val error = "CHANGE_LANG Enhanced VSDK initialization failed: ${result.error}"
                    Log.e(TAG, error)
                    throw Exception(error)
                }
            }

        } catch (e: Exception) {
            Log.e(TAG, "CHANGE_LANG Enhanced VSDK initialization failed", e)

            // Provide detailed error information for debugging
            val initState = VivokaInitializationManager.instance.getInitializationState()
            Log.e(TAG, "CHANGE_LANG Current VSDK state: $initState")

            throw e
        }
    }

    /**
     * Initialize recognizer components after successful VSDK initialization
     */
    private suspend fun initializeRecognizerComponents() {
        Log.d(TAG, "Initializing recognizer components after VSDK success")

        // Create recognizer with this as listener
        recognizer = com.vivoka.vsdk.asr.csdk.Engine.getInstance().getRecognizer("rec", this)
        if (recognizer == null) {
            throw Exception("Failed to create recognizer after successful VSDK init")
        }

        // Continue with full initialization
        initializeRecognizerAndModel()
    }

    /**
     * Initialize recognizer components in degraded mode
     */
    private suspend fun initializeRecognizerComponentsInDegradedMode() {
        Log.w(TAG, "Initializing recognizer components in degraded mode")

        try {
            // Attempt to create recognizer even in degraded mode
            recognizer = com.vivoka.vsdk.asr.csdk.Engine.getInstance().getRecognizer("rec", this)
            if (recognizer == null) {
                Log.w(TAG, "Failed to create recognizer in degraded mode, continuing with limited functionality")
                return
            }

            // Initialize with reduced functionality
            initializeRecognizerAndModelInDegradedMode()

        } catch (e: Exception) {
            Log.w(TAG, "Degraded mode initialization failed, engine will have very limited functionality", e)
            // Don't throw - allow engine to continue with minimal functionality
        }
    }

    /**
     * Initialize recognizer and model in degraded mode with reduced functionality
     */
    private suspend fun initializeRecognizerAndModelInDegradedMode() {
        Log.d(TAG, "Initializing recognizer and dynamic model in degraded mode")

        val speechConfig = config.getSpeechConfig()

        try {
            // Initialize model component with basic configuration
            val rec = recognizer ?: run {
                Log.w(TAG, "Recognizer is null in degraded mode")
                return
            }
            val asrModelName = config.getAsrModelName(speechConfig.language)
            if (!model.initializeModel(rec, speechConfig.language, asrModelName)) {
                Log.w(TAG, "Failed to initialize dynamic model in degraded mode")
                return
            }

            // Set initial model path
            if (!model.setModelPath(config.getModelPath())) {
                Log.w(TAG, "Failed to set model path in degraded mode")
                return
            }

            // Try to initialize audio pipeline
            if (!audio.initializePipeline(rec)) {
                Log.w(TAG, "Failed to initialize audio pipeline in degraded mode")
                return
            }

            // Initialize recognition processor
            recognizerProcessor.initialize(speechConfig)

            // Use only basic commands in degraded mode
            val basicCommands = listOf(
                speechConfig.muteCommand,
                speechConfig.unmuteCommand,
                speechConfig.startDictationCommand,
                speechConfig.stopDictationCommand
            )

            if (!model.compileModelWithCommands(basicCommands)) {
                Log.w(TAG, "Failed to compile basic model in degraded mode")
                return
            }

            // Register basic commands with learning system
            learning.registerCommands(basicCommands)

            // Start audio pipeline
            if (!audio.startPipeline()) {
                Log.w(TAG, "Failed to start audio pipeline in degraded mode")
                return
            }

            Log.i(TAG, "Degraded mode initialization completed with basic functionality")

        } catch (e: Exception) {
            Log.w(TAG, "Degraded mode component initialization failed", e)
        }
    }


    /**
     * Initialize recognizer and dynamic model using components
     */
    private suspend fun initializeRecognizerAndModel() {
        Log.d(TAG, "Initializing recognizer and dynamic model with components")

        val speechConfig = config.getSpeechConfig()

        // Create recognizer with this as listener
        recognizer = com.vivoka.vsdk.asr.csdk.Engine.getInstance().getRecognizer("rec", this)
        if (recognizer == null) {
            throw Exception("Failed to create recognizer")
        }

        // Initialize model component
        val rec = recognizer ?: throw Exception("Recognizer is null after creation")
        val asrModelName = config.getAsrModelName(speechConfig.language)
        if (!model.initializeModel(rec, speechConfig.language, asrModelName)) {
            throw Exception("Failed to initialize dynamic model")
        }

        // Set initial model path
        if (!model.setModelPath(config.getModelPath())) {
            throw Exception("Failed to set model path")
        }

        // Initialize audio pipeline
        if (!audio.initializePipeline(rec)) {
            throw Exception("Failed to initialize audio pipeline")
        }

        // Initialize recognition processor
        recognizerProcessor.initialize(speechConfig)

        // Compile initial models with default commands
        val defaultCommands = listOf(
            speechConfig.muteCommand,
            speechConfig.unmuteCommand,
            speechConfig.startDictationCommand,
            speechConfig.stopDictationCommand
        )

        val commandsToUse = registeredCommands.ifEmpty { defaultCommands }
        if (!model.compileModelWithCommands(commandsToUse)) {
            throw Exception("Failed to compile initial model")
        }

        // Register commands with learning system
        learning.registerCommands(commandsToUse)

        // Start audio pipeline
        if (!audio.startPipeline()) {
            throw Exception("Failed to start audio pipeline")
        }

        // Update recognition processor mode
        recognizerProcessor.updateRecognitionMode(speechConfig.mode)

        // Start timeout monitoring if voice is enabled
        if (voiceStateManager.isVoiceEnabled()) {
            runTimeout()
        }

        Log.d(TAG, "Recognizer and model initialized successfully with components")
    }

    /**
     * Start listening for speech
     */
    fun startListening() {
        if (!voiceStateManager.isInitialized()) {
            Log.e(TAG, "Engine not initialized")
            return
        }

        // Start performance session
        performance.recordRecognition(System.currentTimeMillis(), null, 0f, true)

        // Update state (listening state managed by VivokaEngine)
        // state.startListening() - handled by engine directly

        // Start audio recording
        audio.startRecording()

        // Reset timeout
        voiceStateManager.updateCommandExecutionTime()
        if (voiceStateManager.isVoiceEnabled() && !voiceStateManager.isVoiceSleeping()) {
            runTimeout()
        }

        Log.d(TAG, "Started listening")
    }

    /**
     * Stop listening for speech
     */
    fun stopListening() {
        // Stop audio recording
        audio.stopRecording()

        // Cancel timeout
        timeoutJob?.cancel()

        // Update state (listening state managed by VivokaEngine)
        // state.stopListening() - handled by engine directly

        Log.d(TAG, "Stopped listening")
    }

    /**
     * Set dynamic commands at runtime
     */
    fun setDynamicCommands(commands: List<String>) {
        coroutineScope.launch {
            Log.d(TAG, "SPEECH_TEST: setDynamicCommands commands = $commands")
            registeredCommands.clear()
            registeredCommands.addAll(commands)

            // Register with model component
            model.registerCommands(commands)

            // Register with learning system
            learning.registerCommands(commands)

            // Compile models if not sleeping and initialized
            Log.i(
                TAG,
                "SPEECH_TEST: setDynamicCommands isVoiceSleeping = ${voiceStateManager.isVoiceSleeping()}, isInitialized = ${voiceStateManager.isInitialized()}"
            )
            if (!voiceStateManager.isVoiceSleeping() && voiceStateManager.isInitialized()) {
                val isCompiled = model.compileModelWithCommands(registeredCommands)
                Log.d(TAG, "SPEECH_TEST: setDynamicCommands isCompiled = $isCompiled")
            }

            Log.d(TAG, "Set ${commands.size} dynamic commands")
        }
    }

    /**
     * Set listeners
     */
    fun setResultListener(listener: OnSpeechResultListener) {
        recognizerProcessor.setResultListener(listener)
    }

    fun setPartialResultListener(listener: (String) -> Unit) {
        recognizerProcessor.setPartialResultListener(listener)
    }

    fun setErrorListener(listener: OnSpeechErrorListener) {
        errorListener = listener
        Log.d(TAG, "Error listener registered")
    }

    // ========== IRecognizerListener Implementation ==========

    override fun onEvent(codeString: String?, message: String?, time: String?) {
        recognizerProcessor.processRecognitionEvent(
            codeString = codeString,
            message = message,
            time = time,
            onSilenceDetected = {
                // Handle silence detection by updating command execution time
                voiceStateManager.updateCommandExecutionTime()
            },
            onSpeechDetected = {
                // Handle speech detection by updating command execution time
                voiceStateManager.updateCommandExecutionTime()
            }
        )
    }

    override fun onResult(
        resultType: RecognizerResultType?,
        result: String?,
        isFinal: Boolean
    ) {
        coroutineScope.launch {
            //Log.d(TAG, "SPEECH_TEST: onResult result = $result")
            processRecognitionResult(result, resultType)
        }

    }

    override fun onError(codeString: String?, message: String?) {
        Log.e(TAG, "VSDK error - Code: $codeString, Message: $message")

        // Record performance failure
        performance.recordRecognition(System.currentTimeMillis(), null, 0f, false)

        // CRITICAL FIX: Notify error listener (matching LegacyAvenue functionality)
        errorListener?.invoke(
            "Vivoka SDK error [$codeString]: $message",
            codeString?.toIntOrNull() ?: 500
        )

        // Handle error with error recovery manager
        coroutineScope.launch {
            Log.e(TAG, "VSDK Error - Code: $codeString, Message: $message")
        }
    }

    /**
     * Process recognition result using components
     * CRITICAL: Contains fix for continuous recognition
     */
    private suspend fun processRecognitionResult(result: String?, resultType: RecognizerResultType?) {
        val processingResult = recognizerProcessor.processRecognitionResult(
            result = result,
            resultType = resultType,
            isDictationActive = voiceStateManager.isDictationActive(),
            isVoiceSleeping = voiceStateManager.isVoiceSleeping(),
            onModeSwitch = { mode ->
                Log.i(TAG, "processRecognitionResult: onModeSwitch mode = $mode")
                handleModeSwitch(mode)
            }
        )
        Log.d(TAG, "SPEECH_TEST: processRecognitionResult processingResult = ${processingResult.result}")
        // Handle the processing result
        when (processingResult.action) {
            RecognitionProcessingResult.ProcessingAction.REGULAR_COMMAND -> {
                val result = processingResult.result ?: run {
                    Log.w(TAG, "REGULAR_COMMAND action but result is null")
                    return
                }
                handleRegularCommand(result)
            }

            RecognitionProcessingResult.ProcessingAction.MUTE_COMMAND -> {
                handleMuteCommand()
            }

            RecognitionProcessingResult.ProcessingAction.UNMUTE_COMMAND -> {
                handleUnmuteCommand()
            }

            RecognitionProcessingResult.ProcessingAction.DICTATION_START -> {
                handleDictationStart()
            }

            RecognitionProcessingResult.ProcessingAction.DICTATION_END -> {
                handleDictationEnd()
            }

            else -> {
                // Handle other cases (low confidence, errors, etc.)
            }
        }
    }

    /**
     * Handle mode switches with model management
     */
    private suspend fun handleModeSwitch(mode: Any) {
        Log.d(TAG, "handleModeSwitch = $mode , extra = $ ")
        when (mode.toString()) {
            "FREE_SPEECH_START", "FREE_SPEECH_RUNNING" -> {
                // Switch to dictation model
                val dictationModelPath = config.getDictationModelPath()
                if (model.switchToDictationModel(dictationModelPath)) {
                    voiceStateManager.enterDictationMode()

                    // Start silence detection
                    val timeout = config.getDictationTimeout()
                    audio.startSilenceDetection(timeout) {
                        if (voiceStateManager.isDictationActive()) { // Simplified condition
                            coroutineScope.launch { handleDictationEnd() }
                        }
                    }

                    recognizerProcessor.updateRecognitionMode(SpeechMode.DICTATION)
                    Log.d(TAG, "Switched to dictation mode")
                }
            }

            "STOP_FREE_SPEECH", "COMMAND" -> {
                // Switch back to command model - THIS IS THE KEY FIX
                if (model.switchToCommandModel(config.getModelPath())) {
                    voiceStateManager.exitDictationMode()
                    audio.stopSilenceDetection()
                    recognizerProcessor.updateRecognitionMode(SpeechMode.DYNAMIC_COMMAND)
                    Log.d(TAG, "Switched to command mode")
                }
            }
        }
    }

    /**
     * Handle regular command with learning integration
     */
    private fun handleRegularCommand(result: RecognitionResult) {
        // Process with learning system
        val (enhancedCommand, wasLearned) = learning.processCommandWithLearning(
            result.text,
            registeredCommands,
            result.confidence
        )

        // Use enhanced command if learning found a match
        val finalResult = if (enhancedCommand != null && wasLearned) {
            result.copy(text = enhancedCommand)
        } else {
            result
        }

        // Record performance
        performance.recordRecognition(System.currentTimeMillis(), finalResult.text, finalResult.confidence, true)

        // Update state
        voiceStateManager.updateCommandExecutionTime()

        Log.d(TAG, "Processed regular command: ${finalResult.text}")
    }

    /**
     * Handle mute command
     */
    private suspend fun handleMuteCommand() {
        Log.i(TAG, "handleMuteCommand: ")
        voiceStateManager.enterSleepMode()
        timeoutJob?.cancel()

        // Compile only unmute commands when sleeping
        model.compileModelWithCommands(listOf(config.getSpeechConfig().unmuteCommand))

        Log.d(TAG, "Voice muted")
    }

    /**
     * Handle unmute command
     */
    private suspend fun handleUnmuteCommand() {
        // Recompile full command set
        model.compileModelWithCommands(registeredCommands)

        // Update state
        voiceStateManager.exitSleepMode()

        // Start timeout if voice enabled
        if (voiceStateManager.isVoiceEnabled()) {
            runTimeout()
        }

        Log.d(TAG, "Voice unmuted")
    }

    /**
     * Handle dictation start
     */
    private suspend fun handleDictationStart() {
        voiceStateManager.enterDictationMode()
        Log.d(TAG, "Dictation started")
    }

    /**
     * Handle dictation end
     */
    private suspend fun handleDictationEnd() {
        voiceStateManager.exitDictationMode()
        Log.d(TAG, "Dictation ended")
    }

    /**
     * Run timeout monitoring using state component
     */
    private fun runTimeout() {
        timeoutJob = coroutineScope.launch {
            while (voiceStateManager.isVoiceEnabled() && !voiceStateManager.isVoiceSleeping()) {
                delay(30000) // Check every 30 seconds

                // Check if timeout should occur
                val timeoutMinutes = config.getSpeechConfig().voiceTimeoutMinutes
                if (voiceStateManager.shouldTimeout(timeoutMinutes.toInt())) {
                    // Enter sleep mode
                    handleMuteCommand()
                    break
                }

                // Periodic maintenance
                if (errorRecoveryManager.checkMemoryPressure()) {
                    Log.w(TAG, "Memory pressure detected")
                }
                performance.updateMetrics()

                // Sync learning data
                if (System.currentTimeMillis() % (5 * 60 * 1000) < 30000) {
                    learning.syncLearningData()
                }

            }
        }
    }

    // ========== Recovery Methods ==========

    private suspend fun recoverInitialization(): Boolean {
        Log.i(TAG, "Attempting initialization recovery")
        return try {
            val speechConfig = config.getSpeechConfig()
            initialize(speechConfig)
        } catch (e: Exception) {
            Log.e(TAG, "Initialization recovery failed", e)
            false
        }
    }

    private suspend fun recoverAudioPipeline(): Boolean {
        Log.i(TAG, "Attempting audio pipeline recovery")
        return audio.recoverPipeline()
    }

    private suspend fun recoverModelLoading(): Boolean {
        Log.i(TAG, "Attempting model loading recovery")
        val asrModelName = config.getAsrModelName(config.getSpeechConfig().language)
        return model.recoverModel(asrModelName)
    }

    private suspend fun recoverFromMemoryError(): Boolean {
        Log.i(TAG, "Attempting memory error recovery")

        // Clear learning caches
        learning.clearAllLearningData()

        // Force garbage collection
        System.gc()
        delay(2000)

        // Reinitialize learning system
        return learning.initialize()
    }

    // ========== Public API Methods (maintaining compatibility) ==========

    fun destroy() {
        try {
            Log.d(TAG, "Destroying Vivoka engine")

            // Stop all operations
            stopListening()

            // Cancel timeout job before cancelling scope
            timeoutJob?.cancel()

            // Create a dedicated cleanup scope to avoid deadlocks
            // Note: coroutineScope is cancelled below, so we need a separate scope
            val cleanupScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

            // Launch cleanup tasks without blocking
            cleanupScope.launch {
                try {
                    learning.destroy()
                } catch (e: Exception) {
                    Log.w(TAG, "Error destroying learning component", e)
                }
            }

            cleanupScope.launch {
                try {
                    model.reset()
                } catch (e: Exception) {
                    Log.w(TAG, "Error resetting model component", e)
                }
            }

            cleanupScope.launch {
                try {
                    UniversalInitializationManager.instance.shutdownEngine(VIVOKA_ENGINE_NAME)
                } catch (e: Exception) {
                    Log.w(TAG, "Error shutting down UniversalInitializationManager", e)
                }
            }

            // Cancel main coroutine scope
            coroutineScope.cancel()

            // Destroy synchronous components
            performance.destroy()
            audio.reset()
            voiceStateManager.destroy()
            errorRecoveryManager.destroy()
            config.reset()

            // Destroy ASR engine
            com.vivoka.vsdk.asr.csdk.Engine.getInstance().destroy()

            // Clear recognizer
            recognizer = null
            registeredCommands.clear()

            // Cancel cleanup scope after a delay to allow tasks to complete
            cleanupScope.launch {
                delay(1000)
                cleanupScope.cancel()
            }

            Log.i(TAG, "Vivoka engine destroyed")

        } catch (e: Exception) {
            Log.e(TAG, "Error during destroy", e)
        }
    }

    /**
     * Load persisted config from SharedPreferences
     */
    private fun loadPersistedConfig(): String {
        return prefs?.getString(KEY_VIVOKA_CONFIG, "") ?: ""
    }

    /**
     * Persist config to SharedPreferences
     */
    private fun persistConfig(config: String) {
        prefs?.edit()?.apply {
            putString(KEY_VIVOKA_CONFIG, config)
            apply()
        }
    }
}