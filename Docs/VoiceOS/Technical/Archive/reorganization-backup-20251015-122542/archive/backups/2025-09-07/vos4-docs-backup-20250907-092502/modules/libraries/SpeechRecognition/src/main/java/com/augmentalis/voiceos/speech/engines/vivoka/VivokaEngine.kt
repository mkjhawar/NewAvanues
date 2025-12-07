package com.augmentalis.voiceos.speech.engines.vivoka

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import com.augmentalis.speechrecognition.SpeechConfig
import com.augmentalis.speechrecognition.SpeechMode
import com.augmentalis.voiceos.speech.api.OnSpeechErrorListener
import com.augmentalis.voiceos.speech.api.OnSpeechResultListener
import com.augmentalis.voiceos.speech.api.RecognitionResult
import com.augmentalis.voiceos.speech.engines.common.ServiceState
import com.augmentalis.voiceos.speech.engines.common.TimeoutManager
import com.augmentalis.voiceos.speech.engines.common.VoiceStateManager
import com.augmentalis.voiceos.speech.engines.common.ErrorRecoveryManager
import com.augmentalis.voiceos.speech.engines.common.SdkInitializationManager
import com.augmentalis.voiceos.speech.engines.common.UniversalInitializationManager
import com.vivoka.vsdk.Vsdk
import com.vivoka.vsdk.asr.csdk.recognizer.IRecognizerListener
import com.vivoka.vsdk.asr.csdk.recognizer.Recognizer
import com.vivoka.vsdk.asr.recognizer.RecognizerResultType
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.StateFlow
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine
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
    }
    
    // Coroutines
    private val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var timeoutJob: Job? = null
    
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
    private val assets = VivokaAssets(context, coroutineScope)
    private val errorRecoveryManager = ErrorRecoveryManager("Vivoka", context)
    
    // Shared components - using common components
    private val timeoutManager = TimeoutManager(coroutineScope)
    private val serviceState = ServiceState() // For compatibility with error handler
    
    // Command management
    private val registeredCommands = Collections.synchronizedList(arrayListOf<String>())
    
    // Error listener for propagating errors to clients
    private var errorListener: OnSpeechErrorListener? = null
    
    /**
     * Initialize the engine with configuration
     * CRITICAL FIX: Now uses UniversalInitializationManager for thread-safe initialization
     * with retry mechanism and graceful degradation
     */
    suspend fun initialize(speechConfig: SpeechConfig): Boolean {
        Log.d(TAG, "Starting Vivoka engine initialization with universal manager")
        
        val initConfig = UniversalInitializationManager.InitializationConfig(
            engineName = "VivokaEngine",
            maxRetries = 3,
            initialDelayMs = 1000L,
            maxDelayMs = 8000L,
            backoffMultiplier = 2.0,
            jitterMs = 500L,
            timeoutMs = 30000L,
            allowDegradedMode = true
        )
        
        val result = UniversalInitializationManager.instance.initializeEngine(
            config = initConfig,
            context = context
        ) { ctx ->
            performActualInitialization(ctx, speechConfig)
        }
        
        return when {
            result.success && result.state == UniversalInitializationManager.InitializationState.INITIALIZED -> {
                Log.i(TAG, "Vivoka engine initialized successfully in ${result.totalDuration}ms")
                true
            }
            
            result.success && result.degradedMode -> {
                Log.w(TAG, "Vivoka engine running in degraded mode: ${result.error}")
                true // Still usable in degraded mode
            }
            
            else -> {
                Log.e(TAG, "Vivoka engine initialization failed: ${result.error}")
                false
            }
        }
    }
    
    /**
     * Perform the actual initialization logic (thread-safe, single execution)
     */
    @Suppress("UNUSED_PARAMETER")
    private suspend fun performActualInitialization(context: Context, speechConfig: SpeechConfig): Boolean {
        return try {
            Log.d(TAG, "Performing actual Vivoka initialization")
            
            val initStartTime = System.currentTimeMillis()
            
            // Initialize performance monitoring first
            performance.initialize()
            
            // Initialize configuration
            if (!config.initialize(speechConfig)) {
                throw Exception("Configuration initialization failed")
            }
            
            // Initialize assets management
            assets.initialize(config.getAssetsPath())
            
            // Extract and validate assets BEFORE VSDK initialization (CRITICAL FIX)
            Log.d(TAG, "Extracting and validating assets before VSDK init")
            val assetsResult = assets.extractAndValidateAssets()
            if (!assetsResult.isValid) {
                throw Exception("Asset validation failed: ${assetsResult.reason}")
            }
            
            // CRITICAL FIX: Wait for assets to be fully ready
            delay(500) // Give filesystem time to sync
            
            // Initialize VSDK with proper error handling
            initializeVSDK(config.getConfigPath())
            
            // Initialize learning system
            if (!learning.initialize()) {
                Log.w(TAG, "Learning system initialization failed, continuing without learning")
            }
            
            // Initialize state management
            voiceStateManager.initialize()
            voiceStateManager.setVoiceEnabled(speechConfig.voiceEnabled)
            
            // Record performance metrics
            performance.recordVSDKInitialization(initStartTime, true, "Universal manager initialization")
            
            Log.i(TAG, "Vivoka engine actual initialization completed successfully")
            true
            
        } catch (e: Exception) {
            Log.e(TAG, "Actual Vivoka initialization failed", e)
            
            // Attempt recovery if possible
            if (e.message?.contains("Cannot call 'Vsdk.init' multiple times") == true) {
                Log.w(TAG, "VSDK already initialized, continuing with existing instance")
                
                try {
                    // Initialize just the recognizer components
                    initializeRecognizerComponents()
                    Log.i(TAG, "Successfully recovered from VSDK multiple init error")
                    return true
                } catch (recoveryError: Exception) {
                    Log.e(TAG, "Recovery from multiple init error failed", recoveryError)
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
            Log.d(TAG, "Starting enhanced VSDK initialization with robust error handling")
            
            // Use the new initialization manager to handle all VSDK initialization
            val result = VivokaInitializationManager.instance.initializeVivoka(
                context = context,
                configPath = configPath
            )
            
            when {
                result.success && result.state == SdkInitializationManager.InitializationState.INITIALIZED -> {
                    Log.i(TAG, "VSDK initialized successfully in ${result.initializationTime}ms")
                    // Continue with recognizer initialization
                    initializeRecognizerComponents()
                }
                
                result.success && result.degradedMode -> {
                    Log.w(TAG, "VSDK running in degraded mode: ${result.error}")
                    // Initialize in limited functionality mode
                    initializeRecognizerComponentsInDegradedMode()
                }
                
                else -> {
                    val error = "Enhanced VSDK initialization failed: ${result.error}"
                    Log.e(TAG, error)
                    throw Exception(error)
                }
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Enhanced VSDK initialization failed", e)
            
            // Provide detailed error information for debugging
            val initState = VivokaInitializationManager.instance.getInitializationState()
            Log.e(TAG, "Current VSDK state: $initState")
            
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
            val asrModelName = config.getAsrModelName(speechConfig.language)
            if (!model.initializeModel(recognizer!!, speechConfig.language, asrModelName)) {
                Log.w(TAG, "Failed to initialize dynamic model in degraded mode")
                return
            }
            
            // Set initial model path
            if (!model.setModelPath(config.getModelPath())) {
                Log.w(TAG, "Failed to set model path in degraded mode")
                return
            }
            
            // Try to initialize audio pipeline
            if (!audio.initializePipeline(recognizer!!)) {
                Log.w(TAG, "Failed to initialize audio pipeline in degraded mode")
                return
            }
            
            // Initialize recognition processor
            recognizerProcessor.initialize(speechConfig)
            
            // Use only basic commands in degraded mode
            val basicCommands = listOf(
                speechConfig.muteCommand,
                speechConfig.unmuteCommand
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
        val asrModelName = config.getAsrModelName(speechConfig.language)
        if (!model.initializeModel(recognizer!!, speechConfig.language, asrModelName)) {
            throw Exception("Failed to initialize dynamic model")
        }
        
        // Set initial model path
        if (!model.setModelPath(config.getModelPath())) {
            throw Exception("Failed to set model path")
        }
        
        // Initialize audio pipeline
        if (!audio.initializePipeline(recognizer!!)) {
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
        
        val commandsToUse = if (registeredCommands.isNotEmpty()) registeredCommands else defaultCommands
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
            registeredCommands.clear()
            registeredCommands.addAll(commands)
            
            // Register with model component
            model.registerCommands(commands)
            
            // Register with learning system
            learning.registerCommands(commands)
            
            // Compile models if not sleeping and initialized
            if (!voiceStateManager.isVoiceSleeping() && voiceStateManager.isInitialized()) {
                model.compileModelWithCommands(registeredCommands)
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
        result?.let { jsonResult ->
            coroutineScope.launch {
                processRecognitionResult(jsonResult, resultType)
            }
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
            onModeSwitch = { mode, modelPath ->
                handleModeSwitch(mode, modelPath)
            }
        )
        
        // Handle the processing result
        when (processingResult.action) {
            RecognitionProcessingResult.ProcessingAction.REGULAR_COMMAND -> {
                handleRegularCommand(processingResult.result!!)
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
    private suspend fun handleModeSwitch(mode: Any, @Suppress("UNUSED_PARAMETER") extra: String?) {
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
    private suspend fun handleRegularCommand(result: RecognitionResult) {
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
                
                // Periodic asset check
                if (System.currentTimeMillis() % (60 * 60 * 1000) < 30000) {
                    assets.performPeriodicAssetCheck()
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
            
            // Cancel coroutines
            coroutineScope.cancel()
            timeoutJob?.cancel()
            
            // Destroy components
            performance.destroy()
            runBlocking {
                learning.destroy()
            }
            audio.reset()
            runBlocking {
                model.reset()
            }
            voiceStateManager.destroy()
            errorRecoveryManager.destroy()
            assets.reset()
            config.reset()
            
            // Destroy ASR engine
            com.vivoka.vsdk.asr.csdk.Engine.getInstance().destroy()
            
            // Clear recognizer
            recognizer = null
            registeredCommands.clear()
            
            Log.i(TAG, "Vivoka engine destroyed")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error during destroy", e)
        }
    }
    
    // Result flow access
    val resultFlow: StateFlow<RecognitionResult?> = recognizerProcessor.resultFlow
    
    // Learning statistics
    fun getLearningStats(): Map<String, Int> = learning.getLearningStats()
    
    // Performance metrics  
    fun getPerformanceMetrics(): Map<String, Any> = performance.getVivokaMetrics()
    
    // Asset validation status
    fun getAssetValidationStatus(): Map<String, Any> = assets.getAssetValidationStatus()
    
    // Recovery status
    fun getRecoveryStatus(): Map<String, Any> = errorRecoveryManager.getErrorStatistics()
    
    // Component health checks
    fun isPipelineHealthy(): Boolean = audio.isPipelineHealthy()
    fun isModelReady(): Boolean = model.isModelReady()
    fun isInDegradedMode(): Boolean = false // ErrorRecoveryManager doesn't expose this
    
    // Force operations
    suspend fun forceAssetRevalidation(): Boolean = assets.forceAssetRevalidation().isValid
    suspend fun forcePipelineRestart(): Boolean = audio.forcePipelineRestart()
    suspend fun forceModelRecompilation(): Boolean = model.forceRecompilation()
}