/**
 * AndroidSTTRecognizer.kt - SpeechRecognizer wrapper for Android STT
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Code-Reviewed-By: CCA
 * Created: 2025-01-27
 * Updated: 2026-01-18 - Migrated to KMP androidMain
 */
package com.augmentalis.speechrecognition

import android.content.Context
import android.content.Intent
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import kotlinx.coroutines.*
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

/**
 * Manages Android SpeechRecognizer with thread-safe operations.
 */
class AndroidSTTRecognizer(
    private val context: Context,
    private val performanceMonitor: PerformanceMonitor
) {

    companion object {
        private const val TAG = "AndroidSTTRecognizer"
        private const val INIT_TIMEOUT_MS = 10000L
        private const val RETRY_DELAY_MS = 1000L
        private const val MAX_RETRIES = 3
    }

    // Core components
    private val speechRecognizer = AtomicReference<SpeechRecognizer?>(null)
    private val recognitionListener = AtomicReference<AndroidSTTListener?>(null)

    // State
    private val isInitialized = AtomicBoolean(false)
    private val isListening = AtomicBoolean(false)
    private val isDestroying = AtomicBoolean(false)

    // Thread safety
    private val recognizerLock = Any()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // Settings
    private var currentMode: SpeechMode = SpeechMode.DYNAMIC_COMMAND
    private var currentLanguage: String = "en-US"
    private var retryAttempts = 0

    /**
     * Initialize recognizer
     */
    suspend fun initialize(listener: AndroidSTTListener): Boolean {
        if (isInitialized.get()) {
            Log.w(TAG, "Already initialized")
            return true
        }

        return withContext(Dispatchers.Main) {
            try {
                val startTime = System.currentTimeMillis()

                if (!SpeechRecognizer.isRecognitionAvailable(context)) {
                    Log.e(TAG, "Speech recognition not available")
                    return@withContext false
                }

                val recognizer = SpeechRecognizer.createSpeechRecognizer(context)
                if (recognizer == null) {
                    Log.e(TAG, "Failed to create SpeechRecognizer")
                    return@withContext false
                }

                recognizer.setRecognitionListener(listener)
                speechRecognizer.set(recognizer)
                recognitionListener.set(listener)
                isInitialized.set(true)

                val initTime = System.currentTimeMillis() - startTime
                performanceMonitor.recordSlowOperation("initialization", initTime, INIT_TIMEOUT_MS)

                Log.i(TAG, "Initialized in ${initTime}ms")
                true

            } catch (e: Exception) {
                Log.e(TAG, "Init failed: ${e.message}", e)
                cleanup()
                false
            }
        }
    }

    /**
     * Start listening
     */
    suspend fun startListening(
        mode: SpeechMode,
        language: String = currentLanguage
    ): Boolean {
        if (!isInitialized.get()) {
            Log.e(TAG, "Not initialized")
            return false
        }

        if (isListening.get()) {
            Log.d(TAG, "Already listening")
            return true
        }

        return withContext(Dispatchers.Main) {
            synchronized(recognizerLock) {
                try {
                    val recognizer = speechRecognizer.get() ?: return@withContext false

                    currentMode = mode
                    currentLanguage = language

                    val intent = createRecognitionIntent(mode, language)
                    recognizer.startListening(intent)
                    isListening.set(true)
                    retryAttempts = 0

                    Log.d(TAG, "Started: mode=$mode, lang=$language")
                    true

                } catch (e: Exception) {
                    Log.e(TAG, "Start failed: ${e.message}", e)
                    isListening.set(false)
                    false
                }
            }
        }
    }

    /**
     * Stop listening
     */
    suspend fun stopListening(): Boolean {
        if (!isListening.get()) return true

        return withContext(Dispatchers.Main) {
            synchronized(recognizerLock) {
                try {
                    speechRecognizer.get()?.stopListening()
                    isListening.set(false)
                    Log.d(TAG, "Stopped")
                    true
                } catch (e: Exception) {
                    Log.e(TAG, "Stop failed: ${e.message}", e)
                    isListening.set(false)
                    false
                }
            }
        }
    }

    /**
     * Cancel recognition
     */
    suspend fun cancel(): Boolean {
        return withContext(Dispatchers.Main) {
            synchronized(recognizerLock) {
                try {
                    speechRecognizer.get()?.cancel()
                    isListening.set(false)
                    Log.d(TAG, "Cancelled")
                    true
                } catch (e: Exception) {
                    Log.e(TAG, "Cancel failed: ${e.message}", e)
                    isListening.set(false)
                    false
                }
            }
        }
    }

    /**
     * Restart recognition
     */
    suspend fun restart(): Boolean {
        Log.d(TAG, "Restarting")
        stopListening()
        delay(100)
        return startListening(currentMode, currentLanguage)
    }

    /**
     * Restart with retry logic
     */
    suspend fun restartWithRetry(): Boolean {
        if (retryAttempts >= MAX_RETRIES) {
            Log.e(TAG, "Max retries reached")
            return false
        }

        retryAttempts++
        val delayMs = RETRY_DELAY_MS * retryAttempts
        Log.d(TAG, "Retry $retryAttempts/$MAX_RETRIES after ${delayMs}ms")
        delay(delayMs)
        return restart()
    }

    /**
     * Switch mode
     */
    suspend fun switchMode(newMode: SpeechMode): Boolean {
        if (currentMode == newMode) return true

        Log.i(TAG, "Switching $currentMode -> $newMode")
        val wasListening = isListening.get()

        if (wasListening) {
            stopListening()
            delay(100)
        }

        currentMode = newMode
        return if (wasListening) startListening(newMode, currentLanguage) else true
    }

    /**
     * Change language
     */
    suspend fun changeLanguage(newLanguage: String): Boolean {
        if (currentLanguage == newLanguage) return true

        Log.i(TAG, "Changing language: $currentLanguage -> $newLanguage")
        val wasListening = isListening.get()

        if (wasListening) {
            stopListening()
            delay(100)
        }

        currentLanguage = newLanguage
        return if (wasListening) startListening(currentMode, newLanguage) else true
    }

    /**
     * Create recognition intent
     */
    private fun createRecognitionIntent(mode: SpeechMode, language: String): Intent {
        return Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, language)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, language)

            when (mode) {
                SpeechMode.FREE_SPEECH, SpeechMode.DICTATION -> {
                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                    putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 10000L)
                    putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 5000L)
                    putExtra(RecognizerIntent.EXTRA_PROMPT, "Start speaking...")
                }
                else -> {
                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                    putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 5000L)
                    putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 2500L)
                    putExtra(RecognizerIntent.EXTRA_PROMPT, "Say a command...")
                }
            }
        }
    }

    // State getters
    fun isRecognitionAvailable(): Boolean = try {
        SpeechRecognizer.isRecognitionAvailable(context)
    } catch (e: Exception) { false }

    fun isListening(): Boolean = isListening.get()
    fun isRecognizerInitialized(): Boolean = isInitialized.get()
    fun getCurrentMode(): SpeechMode = currentMode
    fun getCurrentLanguage(): String = currentLanguage

    /**
     * Cleanup resources
     */
    private fun cleanup() {
        isInitialized.set(false)
        isListening.set(false)
        speechRecognizer.getAndSet(null)?.destroy()
        recognitionListener.set(null)
    }

    /**
     * Destroy recognizer
     */
    suspend fun destroy() {
        if (isDestroying.get()) return
        isDestroying.set(true)

        Log.d(TAG, "Destroying")

        if (isListening.get()) {
            stopListening()
        }

        withContext(Dispatchers.Main) {
            synchronized(recognizerLock) {
                try {
                    cleanup()
                } catch (e: Exception) {
                    Log.e(TAG, "Destroy error: ${e.message}", e)
                } finally {
                    scope.cancel()
                    Log.d(TAG, "Destroyed")
                }
            }
        }
    }
}
