/**
 * VoiceRecognitionService.kt - AIDL-based voice recognition service
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Created: 2025-01-28
 *
 * Purpose: Service implementation for cross-app voice recognition
 * Direct implementation following VOS4 zero-overhead architecture
 */
package com.augmentalis.voicerecognition.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.os.RemoteCallbackList
import android.os.RemoteException
import android.util.Log
import com.augmentalis.speechrecognition.SpeechEngine
import com.augmentalis.speechrecognition.SpeechConfig
import com.augmentalis.speechrecognition.SpeechMode
import com.augmentalis.speechrecognition.RecognitionResult
import com.augmentalis.speechrecognition.SpeechListenerManager
// import com.augmentalis.speechrecognition.android.AndroidSTTEngine  // DISABLED: User wants only VivokaEngine
// import com.augmentalis.speechrecognition.vosk.VoskEngine  // DISABLED: Learning dependency
import com.augmentalis.speechrecognition.vivoka.VivokaEngine
// import com.augmentalis.speechrecognition.whisper.WhisperEngine  // DISABLED: Learning dependency
import com.augmentalis.voicerecognition.IVoiceRecognitionService
import com.augmentalis.voicerecognition.IRecognitionCallback
import kotlinx.coroutines.*

/**
 * Service implementation for voice recognition with AIDL interface
 *
 * Manages multiple client callbacks and bridges to existing SpeechViewModel logic
 * using the speech recognition library engines directly.
 */
class VoiceRecognitionService : Service() {

    companion object {
        private const val TAG = "VoiceRecognitionService"
        private const val DEFAULT_ENGINE = "vivoka"  // Default to Vivoka per requirements
        private const val PREFS_NAME = "voice_recognition_prefs"
        private const val PREF_SELECTED_ENGINE = "selected_engine"
    }

    // Service lifecycle
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    // Callback management
    private val callbacks = RemoteCallbackList<IRecognitionCallback>()

    // Recognition state
    private var currentEngine: Any? = null
    private var isCurrentlyRecognizing = false
    private val listenerManager = SpeechListenerManager()
    private var currentEngineType: SpeechEngine? = null

    /**
     * AIDL service implementation
     */
    private val binder = object : IVoiceRecognitionService.Stub() {

        override fun startRecognition(engine: String, language: String, mode: Int): Boolean {
            return try {
                // Use provided engine or get from preferences if empty string, defaulting to Vivoka
                val selectedEngine = if (engine.isEmpty()) getSelectedEngine() else engine
                Log.d(TAG, "startRecognition called: engine=$selectedEngine, language=$language, mode=$mode")
                
                // Save selection if explicitly provided (not empty)
                if (engine.isNotEmpty()) {
                    saveSelectedEngine(engine)
                }

                val speechEngine = stringToSpeechEngine(selectedEngine)
                val speechMode = intToSpeechMode(mode)

                serviceScope.launch {
                    initializeAndStartRecognition(speechEngine, language, speechMode)
                }

                true
            } catch (e: Exception) {
                Log.e(TAG, "Error starting recognition", e)
                broadcastError(500, "Failed to start recognition: ${e.message}")
                false
            }
        }

        override fun stopRecognition(): Boolean {
            return try {
                Log.d(TAG, "stopRecognition called")
                serviceScope.launch {
                    stopCurrentRecognition()
                }
                true
            } catch (e: Exception) {
                Log.e(TAG, "Error stopping recognition", e)
                false
            }
        }

        override fun isRecognizing(): Boolean {
            return isCurrentlyRecognizing
        }

        override fun registerCallback(callback: IRecognitionCallback) {
            Log.d(TAG, "Registering callback")
            callbacks.register(callback)
        }

        override fun unregisterCallback(callback: IRecognitionCallback) {
            Log.d(TAG, "Unregistering callback")
            callbacks.unregister(callback)
        }

        override fun getAvailableEngines(): List<String> {
            return listOf(
                SpeechEngine.ANDROID_STT.name.lowercase(),
                SpeechEngine.VOSK.name.lowercase(),
                SpeechEngine.VIVOKA.name.lowercase(),
                SpeechEngine.GOOGLE_CLOUD.name.lowercase(),
                SpeechEngine.WHISPER.name.lowercase()
            )
        }

        override fun getStatus(): String {
            return when {
                currentEngine == null -> "Not initialized"
                isCurrentlyRecognizing -> "Recognizing with ${currentEngineType?.name}"
                else -> "Ready with ${currentEngineType?.name}"
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Service created")
        setupListeners()
    }

    override fun onBind(intent: Intent?): IBinder {
        Log.d(TAG, "Service bound")
        return binder
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "Service destroyed")

        serviceScope.launch {
            stopCurrentRecognition()
            serviceScope.cancel()
        }

        callbacks.kill()
    }

    /**
     * Setup speech recognition listeners that bridge to AIDL callbacks
     */
    private fun setupListeners() {
        listenerManager.onResult = { result ->
            handleSpeechResult(result)
        }

        listenerManager.onError = { error ->
            broadcastError(error.code, error.message)
            isCurrentlyRecognizing = false
            broadcastStateChange(0, "Error occurred")
        }

        listenerManager.onStateChange = { state, message ->
            // Convert enum state to int for AIDL callback
            val stateInt = when (state.name.lowercase()) {
                "uninitialized", "stopped", "ready" -> 0
                "listening" -> 1
                "processing", "initializing" -> 2
                "error" -> 3
                else -> 0
            }
            broadcastStateChange(stateInt, message ?: state.name)
        }
    }

    /**
     * Initialize and start recognition with specified engine
     */
    private suspend fun initializeAndStartRecognition(
        engine: SpeechEngine,
        language: String,
        mode: SpeechMode
    ) {
        try {
            // Stop current recognition if active
            stopCurrentRecognition()

            broadcastStateChange(1, "Initializing ${engine.name}")

            // Create configuration
            val config = SpeechConfig(
                language = language,
                mode = mode,
                enableVAD = true,
                confidenceThreshold = 0.5f,
                maxRecordingDuration = 30000L,
                timeoutDuration = 5000L,
                enableProfanityFilter = false
            )

            // Initialize engine based on type (following SpeechViewModel pattern)
            currentEngine = when (engine) {
                // SpeechEngine.ANDROID_STT -> {  // DISABLED: User wants only VivokaEngine
                //     AndroidSTTEngine(this@VoiceRecognitionService).apply {
                //         initialize(this@VoiceRecognitionService, config)
                //         setResultListener { result -> listenerManager.onResult?.invoke(result) }
                //         setErrorListener { error, code -> listenerManager.onError?.invoke(error, code) }
                //     }
                // }
                // SpeechEngine.VOSK -> {  // DISABLED: Learning dependency
                //     VoskEngine(this@VoiceRecognitionService).apply {
                //         initialize(config)
                //         setResultListener { result -> listenerManager.onResult?.invoke(result) }
                //         setErrorListener { error, code -> listenerManager.onError?.invoke(error, code) }
                //     }
                // }
                SpeechEngine.VIVOKA -> {
                    VivokaEngine(this@VoiceRecognitionService).apply {
                        initialize(config.copy(engine =  SpeechEngine.VIVOKA))
                        setResultListener { result -> listenerManager.onResult?.invoke(result) }
                        setErrorListener { error -> listenerManager.onError?.invoke(error) }
                    }
                }
                // SpeechEngine.GOOGLE_CLOUD -> {  // DISABLED: Learning dependency
                //     // GoogleCloudEngine is temporarily disabled - using Android STT as fallback
                //     AndroidSTTEngine(this@VoiceRecognitionService).apply {
                //         initialize(this@VoiceRecognitionService, config)
                //         setResultListener { result -> listenerManager.onResult?.invoke(result) }
                //         setErrorListener { error -> listenerManager.onError?.invoke(error) }
                //     }
                // }
                // SpeechEngine.WHISPER -> {  // DISABLED: Learning dependency
                //     WhisperEngine(this@VoiceRecognitionService).apply {
                //         initialize(config)
                //         setResultListener { result -> listenerManager.onResult?.invoke(result) }
                //         setErrorListener { error -> listenerManager.onError?.invoke(error) }
                //     }
                // }
                else -> {
                    // Fallback to VivokaEngine for all other engine types
                    VivokaEngine(this@VoiceRecognitionService).apply {
                        initialize(config.copy(engine = SpeechEngine.VIVOKA))
                        setResultListener { result -> listenerManager.onResult?.invoke(result) }
                        setErrorListener { error -> listenerManager.onError?.invoke(error) }
                    }
                }
            }

            currentEngineType = engine

            // Start listening
            when (val eng = currentEngine) {
                // is AndroidSTTEngine -> eng.startListening(mode)  // DISABLED: User wants only VivokaEngine
                // is VoskEngine -> eng.startListening()  // DISABLED: Learning dependency
                is VivokaEngine -> eng.startListening()
                // GoogleCloudEngine disabled - handled by fallback
                // is WhisperEngine -> eng.startListening()  // DISABLED: Learning dependency
                else -> {
                    broadcastError(502, "Engine type not supported")
                    return
                }
            }

            isCurrentlyRecognizing = true
            broadcastStateChange(1, "Listening...")

        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize recognition", e)
            broadcastError(500, "Initialization failed: ${e.message}")
            isCurrentlyRecognizing = false
        }
    }

    /**
     * Stop current recognition session
     */
    private suspend fun stopCurrentRecognition() {
        try {
            when (val eng = currentEngine) {
                // is AndroidSTTEngine -> eng.stopListening()  // DISABLED: User wants only VivokaEngine
                // is VoskEngine -> eng.stopListening()  // DISABLED: Learning dependency
                is VivokaEngine -> eng.stopListening()
                // GoogleCloudEngine disabled - handled by fallback
                // is WhisperEngine -> eng.stopListening()  // DISABLED: Learning dependency
                else -> { /* No-op */ }
            }

            isCurrentlyRecognizing = false
            broadcastStateChange(0, "Stopped")

        } catch (e: Exception) {
            Log.e(TAG, "Error stopping recognition", e)
        }
    }

    /**
     * Handle speech recognition results and broadcast to clients
     */
    private fun handleSpeechResult(result: RecognitionResult) {
        val text = result.text
        val confidence = result.confidence
        val isFinal = result.isFinal

        Log.d(TAG, "Recognition result: text='$text', confidence=$confidence, isFinal=$isFinal")

        // Broadcast result to all registered callbacks
        val callbackCount = callbacks.beginBroadcast()
        try {
            for (i in 0 until callbackCount) {
                try {
                    callbacks.getBroadcastItem(i)?.onRecognitionResult(text, confidence, isFinal)
                } catch (e: RemoteException) {
                    Log.w(TAG, "Failed to deliver result to callback $i", e)
                }
            }
        } finally {
            callbacks.finishBroadcast()
        }

        // Also send as partial result if not final
        if (!isFinal) {
            broadcastPartialResult(text)
        }
    }

    /**
     * Broadcast error to all registered callbacks
     */
    private fun broadcastError(errorCode: Int, message: String) {
        Log.e(TAG, "Broadcasting error: code=$errorCode, message=$message")

        val callbackCount = callbacks.beginBroadcast()
        try {
            for (i in 0 until callbackCount) {
                try {
                    callbacks.getBroadcastItem(i)?.onError(errorCode, message)
                } catch (e: RemoteException) {
                    Log.w(TAG, "Failed to deliver error to callback $i", e)
                }
            }
        } finally {
            callbacks.finishBroadcast()
        }
    }

    /**
     * Broadcast state change to all registered callbacks
     */
    private fun broadcastStateChange(state: Int, message: String) {
        Log.d(TAG, "Broadcasting state change: state=$state, message=$message")

        val callbackCount = callbacks.beginBroadcast()
        try {
            for (i in 0 until callbackCount) {
                try {
                    callbacks.getBroadcastItem(i)?.onStateChanged(state, message)
                } catch (e: RemoteException) {
                    Log.w(TAG, "Failed to deliver state change to callback $i", e)
                }
            }
        } finally {
            callbacks.finishBroadcast()
        }
    }

    /**
     * Broadcast partial result to all registered callbacks
     */
    private fun broadcastPartialResult(partialText: String) {
        val callbackCount = callbacks.beginBroadcast()
        try {
            for (i in 0 until callbackCount) {
                try {
                    callbacks.getBroadcastItem(i)?.onPartialResult(partialText)
                } catch (e: RemoteException) {
                    Log.w(TAG, "Failed to deliver partial result to callback $i", e)
                }
            }
        } finally {
            callbacks.finishBroadcast()
        }
    }

    /**
     * Get selected engine from preferences or default
     */
    private fun getSelectedEngine(): String {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(PREF_SELECTED_ENGINE, DEFAULT_ENGINE) ?: DEFAULT_ENGINE
    }
    
    /**
     * Save selected engine to preferences
     */
    private fun saveSelectedEngine(engine: String) {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(PREF_SELECTED_ENGINE, engine).apply()
    }
    
    /**
     * Convert string engine name to SpeechEngine enum
     */
    private fun stringToSpeechEngine(engine: String): SpeechEngine {
        return when (engine.lowercase()) {
            "android", "android_stt", "google", "google_stt" -> SpeechEngine.ANDROID_STT
            "vivoka" -> SpeechEngine.VIVOKA
            "google_cloud", "googlecloud" -> SpeechEngine.GOOGLE_CLOUD
            "vosk" -> SpeechEngine.VOSK
            "whisper" -> SpeechEngine.WHISPER
            else -> throw IllegalArgumentException("Unknown engine: $engine")
        }
    }

    /**
     * Convert int mode to SpeechMode enum
     */
    private fun intToSpeechMode(mode: Int): SpeechMode {
        return when (mode) {
            0 -> SpeechMode.STATIC_COMMAND
            1 -> SpeechMode.DYNAMIC_COMMAND
            2 -> SpeechMode.DICTATION
            3 -> SpeechMode.FREE_SPEECH
            else -> SpeechMode.FREE_SPEECH // Default to most flexible mode
        }
    }
}