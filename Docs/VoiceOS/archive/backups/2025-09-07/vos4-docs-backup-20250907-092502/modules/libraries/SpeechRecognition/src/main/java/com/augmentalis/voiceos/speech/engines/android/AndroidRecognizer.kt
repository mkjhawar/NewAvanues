/**
 * AndroidRecognizer.kt - SpeechRecognizer wrapper for AndroidSTTEngine
 * 
 * Extracted from AndroidSTTEngine as part of SOLID refactoring
 * Handles all Android SpeechRecognizer operations:
 * - SpeechRecognizer lifecycle management
 * - Recognition session control
 * - Thread-safe operations
 * - State synchronization
 * - Resource cleanup
 * 
 * © Augmentalis Inc, Intelligent Devices LLC, Manoj Jhawar, Aman Jhawar
 */
package com.augmentalis.voiceos.speech.engines.android

import android.content.Context
import android.content.Intent
import android.speech.SpeechRecognizer
import android.util.Log
import com.augmentalis.speechrecognition.SpeechMode
import com.augmentalis.voiceos.speech.engines.common.ServiceState
import com.augmentalis.voiceos.speech.engines.common.PerformanceMonitor
import kotlinx.coroutines.*
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

/**
 * Manages Android SpeechRecognizer instances with thread-safe operations.
 * Provides controlled access to speech recognition functionality.
 */
class AndroidRecognizer(
    private val context: Context,
    private val serviceState: ServiceState,
    private val performanceMonitor: PerformanceMonitor
) {
    
    companion object {
        private const val TAG = "AndroidRecognizer"
        private const val INITIALIZATION_TIMEOUT_MS = 10000L
        private const val RECOGNITION_RETRY_DELAY_MS = 1000L
        private const val MAX_RETRY_ATTEMPTS = 3
    }
    
    // Core components
    private val speechRecognizer = AtomicReference<SpeechRecognizer?>(null)
    private val recognitionListener = AtomicReference<AndroidListener?>(null)
    private val androidIntent = AndroidIntent(context)
    
    // State management
    private val isInitialized = AtomicBoolean(false)
    private val isListening = AtomicBoolean(false)
    private val isDestroying = AtomicBoolean(false)
    
    // Thread safety
    private val recognizerLock = Any()
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val mainScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    
    // Recognition state
    private var currentMode: SpeechMode = SpeechMode.DYNAMIC_COMMAND
    private var currentLanguage: String = "en-US"
    private var retryAttempts = 0
    
    /**
     * Initialize the recognizer with listener
     */
    suspend fun initialize(listener: AndroidListener): Boolean {
        if (isInitialized.get()) {
            Log.w(TAG, "Recognizer already initialized")
            return true
        }
        
        return withContext(Dispatchers.Main) {
            try {
                val startTime = System.currentTimeMillis()
                
                if (!SpeechRecognizer.isRecognitionAvailable(context)) {
                    Log.e(TAG, "Speech recognition not available on this device")
                    return@withContext false
                }
                
                val recognizer = SpeechRecognizer.createSpeechRecognizer(context)
                if (recognizer == null) {
                    Log.e(TAG, "Failed to create SpeechRecognizer")
                    return@withContext false
                }
                
                // Set up listener
                recognizer.setRecognitionListener(listener)
                
                // Store references atomically
                speechRecognizer.set(recognizer)
                recognitionListener.set(listener)
                isInitialized.set(true)
                
                val initTime = System.currentTimeMillis() - startTime
                performanceMonitor.recordSlowOperation("initialization", initTime, INITIALIZATION_TIMEOUT_MS)
                
                Log.i(TAG, "AndroidRecognizer initialized in ${initTime}ms")
                true
                
            } catch (e: Exception) {
                Log.e(TAG, "Failed to initialize recognizer: ${e.message}", e)
                cleanup()
                false
            }
        }
    }
    
    /**
     * Start listening for speech recognition
     */
    suspend fun startListening(
        mode: SpeechMode,
        language: String = currentLanguage
    ): Boolean {
        if (!isInitialized.get()) {
            Log.e(TAG, "Cannot start listening - recognizer not initialized")
            return false
        }
        
        if (isListening.get()) {
            Log.d(TAG, "Already listening")
            return true
        }
        
        return withContext(Dispatchers.Main) {
            synchronized(recognizerLock) {
                try {
                    val recognizer = speechRecognizer.get()
                    if (recognizer == null) {
                        Log.e(TAG, "SpeechRecognizer is null")
                        return@withContext false
                    }
                    
                    // Update current settings
                    currentMode = mode
                    currentLanguage = language
                    
                    // Create recognition intent
                    val intent = androidIntent.createRecognitionIntent(mode, language)
                    
                    // Start recognition
                    recognizer.startListening(intent)
                    isListening.set(true)
                    retryAttempts = 0
                    
                    Log.d(TAG, "Started listening: mode=$mode, language=$language")
                    return@withContext true
                    
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to start listening: ${e.message}", e)
                    isListening.set(false)
                    return@withContext false
                }
            }
        }
    }
    
    /**
     * Stop listening
     */
    suspend fun stopListening(): Boolean {
        if (!isListening.get()) {
            Log.d(TAG, "Not currently listening")
            return true
        }
        
        return withContext(Dispatchers.Main) {
            synchronized(recognizerLock) {
                try {
                    val recognizer = speechRecognizer.get()
                    if (recognizer != null) {
                        recognizer.stopListening()
                        Log.d(TAG, "Stopped listening")
                    }
                    
                    isListening.set(false)
                    return@withContext true
                    
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to stop listening: ${e.message}", e)
                    isListening.set(false)
                    return@withContext false
                }
            }
        }
    }
    
    /**
     * Cancel current recognition
     */
    suspend fun cancel(): Boolean {
        return withContext(Dispatchers.Main) {
            synchronized(recognizerLock) {
                try {
                    val recognizer = speechRecognizer.get()
                    if (recognizer != null) {
                        recognizer.cancel()
                        Log.d(TAG, "Recognition cancelled")
                    }
                    
                    isListening.set(false)
                    return@withContext true
                    
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to cancel recognition: ${e.message}", e)
                    isListening.set(false)
                    return@withContext false
                }
            }
        }
    }
    
    /**
     * Restart recognition with current settings
     */
    suspend fun restart(): Boolean {
        Log.d(TAG, "Restarting recognition")
        
        // Stop current recognition
        stopListening()
        
        // Small delay to allow cleanup
        delay(100)
        
        // Start with current settings
        return startListening(currentMode, currentLanguage)
    }
    
    /**
     * Restart recognition with retry logic
     */
    suspend fun restartWithRetry(): Boolean {
        if (retryAttempts >= MAX_RETRY_ATTEMPTS) {
            Log.e(TAG, "Max retry attempts reached ($MAX_RETRY_ATTEMPTS)")
            return false
        }
        
        retryAttempts++
        Log.d(TAG, "Attempting restart (attempt $retryAttempts/$MAX_RETRY_ATTEMPTS)")
        
        // Exponential backoff delay
        val delay = RECOGNITION_RETRY_DELAY_MS * retryAttempts
        delay(delay)
        
        return restart()
    }
    
    /**
     * Check if recognizer is available
     */
    fun isRecognitionAvailable(): Boolean {
        return try {
            SpeechRecognizer.isRecognitionAvailable(context)
        } catch (e: Exception) {
            Log.e(TAG, "Error checking recognition availability: ${e.message}")
            false
        }
    }
    
    /**
     * Check if currently listening
     */
    fun isCurrentlyListening(): Boolean = isListening.get()
    
    /**
     * Check if initialized
     */
    fun isRecognizerInitialized(): Boolean = isInitialized.get()
    
    /**
     * Get current mode
     */
    fun getCurrentMode(): SpeechMode = currentMode
    
    /**
     * Get current language
     */
    fun getCurrentLanguage(): String = currentLanguage
    
    /**
     * Get retry attempts count
     */
    fun getRetryAttempts(): Int = retryAttempts
    
    /**
     * Reset retry counter
     */
    fun resetRetryCounter() {
        retryAttempts = 0
    }
    
    /**
     * Switch to different mode
     */
    suspend fun switchMode(newMode: SpeechMode): Boolean {
        if (currentMode == newMode) {
            Log.d(TAG, "Already in mode: $newMode")
            return true
        }
        
        Log.i(TAG, "Switching from $currentMode to $newMode")
        
        val wasListening = isListening.get()
        
        if (wasListening) {
            stopListening()
            delay(100) // Allow cleanup
        }
        
        currentMode = newMode
        
        return if (wasListening) {
            startListening(newMode, currentLanguage)
        } else {
            true
        }
    }
    
    /**
     * Change language
     */
    suspend fun changeLanguage(newLanguage: String): Boolean {
        if (currentLanguage == newLanguage) {
            Log.d(TAG, "Already using language: $newLanguage")
            return true
        }
        
        Log.i(TAG, "Changing language from $currentLanguage to $newLanguage")
        
        val wasListening = isListening.get()
        
        if (wasListening) {
            stopListening()
            delay(100) // Allow cleanup
        }
        
        currentLanguage = newLanguage
        androidIntent.setDefaultLanguage(newLanguage)
        
        return if (wasListening) {
            startListening(currentMode, newLanguage)
        } else {
            true
        }
    }
    
    /**
     * Get recognizer status
     */
    fun getStatus(): RecognizerStatus {
        return RecognizerStatus(
            isInitialized = isInitialized.get(),
            isListening = isListening.get(),
            isDestroying = isDestroying.get(),
            currentMode = currentMode,
            currentLanguage = currentLanguage,
            retryAttempts = retryAttempts,
            isRecognitionAvailable = isRecognitionAvailable()
        )
    }
    
    /**
     * Cleanup and release resources
     */
    private fun cleanup() {
        isInitialized.set(false)
        isListening.set(false)
        speechRecognizer.getAndSet(null)?.destroy()
        recognitionListener.set(null)
    }
    
    /**
     * Destroy the recognizer
     */
    suspend fun destroy() {
        if (isDestroying.get()) {
            Log.d(TAG, "Already destroying")
            return
        }
        
        isDestroying.set(true)
        Log.d(TAG, "Destroying AndroidRecognizer")
        
        // Stop listening first (outside of synchronized block)
        if (isListening.get()) {
            stopListening()
        }
        
        withContext(Dispatchers.Main) {
            synchronized(recognizerLock) {
                try {
                    // Cleanup resources
                    cleanup()
                    
                } catch (e: Exception) {
                    Log.e(TAG, "Error during destroy: ${e.message}", e)
                } finally {
                    // Cancel coroutines
                    scope.cancel()
                    mainScope.cancel()
                    
                    Log.d(TAG, "AndroidRecognizer destroyed")
                }
            }
        }
    }
    
    /**
     * Data class for recognizer status
     */
    data class RecognizerStatus(
        val isInitialized: Boolean,
        val isListening: Boolean,
        val isDestroying: Boolean,
        val currentMode: SpeechMode,
        val currentLanguage: String,
        val retryAttempts: Int,
        val isRecognitionAvailable: Boolean
    )
}