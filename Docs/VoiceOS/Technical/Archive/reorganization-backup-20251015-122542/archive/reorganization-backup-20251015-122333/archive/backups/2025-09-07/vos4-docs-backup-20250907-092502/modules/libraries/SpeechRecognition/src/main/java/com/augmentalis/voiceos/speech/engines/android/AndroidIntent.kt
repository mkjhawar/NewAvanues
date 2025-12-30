/**
 * AndroidIntent.kt - Intent creation and management for AndroidSTTEngine
 * 
 * Extracted from AndroidSTTEngine as part of SOLID refactoring
 * Handles all Android RecognizerIntent creation and configuration:
 * - Intent parameter setup
 * - Language configuration
 * - Recognition model selection
 * - Partial results configuration
 * - Intent validation
 * 
 * © Augmentalis Inc, Intelligent Devices LLC, Manoj Jhawar, Aman Jhawar
 */
package com.augmentalis.voiceos.speech.engines.android

import android.content.Context
import android.content.Intent
import android.speech.RecognizerIntent
import android.util.Log
import com.augmentalis.speechrecognition.SpeechMode

/**
 * Manages Android RecognizerIntent creation and configuration.
 * Provides optimized intent settings for different recognition modes.
 */
class AndroidIntent(private val context: Context) {
    
    companion object {
        private const val TAG = "AndroidIntent"
        private const val DEFAULT_MAX_RESULTS = 1
        private const val ENHANCED_MAX_RESULTS = 3
        private const val DEFAULT_TIMEOUT_MS = 5000L
        private const val DICTATION_TIMEOUT_MS = 10000L
    }
    
    // Intent configuration
    private var currentLanguage: String = "en-US"
    private var enablePartialResults: Boolean = true
    private var maxResults: Int = DEFAULT_MAX_RESULTS
    private var recognitionTimeout: Long = DEFAULT_TIMEOUT_MS
    
    /**
     * Create recognition intent for the specified mode
     */
    fun createRecognitionIntent(
        mode: SpeechMode,
        language: String = currentLanguage,
        enablePartialResults: Boolean = true
    ): Intent {
        
        Log.d(TAG, "Creating recognition intent for mode: $mode, language: $language")
        
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            // Set language model based on mode
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                when (mode) {
                    SpeechMode.FREE_SPEECH -> RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                    SpeechMode.DYNAMIC_COMMAND -> RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                    SpeechMode.STATIC_COMMAND -> RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                    else -> RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                }
            )
            
            // Set calling package for proper attribution
            putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
            
            // Set language
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, language)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, language)
            
            // Configure results based on mode
            when (mode) {
                SpeechMode.FREE_SPEECH -> {
                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, ENHANCED_MAX_RESULTS)
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, enablePartialResults)
                    // Longer timeout for dictation
                    putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, DICTATION_TIMEOUT_MS)
                    putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, DICTATION_TIMEOUT_MS / 2)
                }
                
                SpeechMode.DYNAMIC_COMMAND, SpeechMode.STATIC_COMMAND -> {
                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, DEFAULT_MAX_RESULTS)
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, enablePartialResults)
                    // Shorter timeout for commands
                    putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, DEFAULT_TIMEOUT_MS)
                    putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, DEFAULT_TIMEOUT_MS / 2)
                }
                
                else -> {
                    // Default configuration
                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, DEFAULT_MAX_RESULTS)
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, enablePartialResults)
                }
            }
            
            // Optional: Add hint for what we're expecting
            when (mode) {
                SpeechMode.FREE_SPEECH, SpeechMode.DICTATION -> {
                    putExtra(RecognizerIntent.EXTRA_PROMPT, "Start speaking...")
                }
                SpeechMode.DYNAMIC_COMMAND, SpeechMode.STATIC_COMMAND -> {
                    putExtra(RecognizerIntent.EXTRA_PROMPT, "Say a command...")
                }
                SpeechMode.HYBRID -> {
                    putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak naturally or say a command...")
                }
            }
            
            // Enable confidence scores if available
            putExtra("android.speech.extra.GET_AUDIO_FORMAT", "audio/AMR")
            putExtra("android.speech.extra.GET_AUDIO", true)
        }
        
        // Store current configuration
        this.currentLanguage = language
        this.enablePartialResults = enablePartialResults
        this.maxResults = when (mode) {
            SpeechMode.FREE_SPEECH -> ENHANCED_MAX_RESULTS
            else -> DEFAULT_MAX_RESULTS
        }
        this.recognitionTimeout = when (mode) {
            SpeechMode.FREE_SPEECH -> DICTATION_TIMEOUT_MS
            else -> DEFAULT_TIMEOUT_MS
        }
        
        Log.d(TAG, "Recognition intent created with maxResults=$maxResults, timeout=${recognitionTimeout}ms")
        return intent
    }
    
    /**
     * Create intent with custom parameters
     */
    fun createCustomIntent(
        language: String,
        maxResults: Int = DEFAULT_MAX_RESULTS,
        partialResults: Boolean = true,
        prompt: String? = null,
        languageModel: String = RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
    ): Intent {
        
        Log.d(TAG, "Creating custom intent: language=$language, maxResults=$maxResults")
        
        return Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, languageModel)
            putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, language)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, language)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, maxResults)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, partialResults)
            
            if (prompt != null) {
                putExtra(RecognizerIntent.EXTRA_PROMPT, prompt)
            }
        }
    }
    
    /**
     * Validate intent configuration
     */
    fun validateIntent(intent: Intent): Boolean {
        val action = intent.action
        if (action != RecognizerIntent.ACTION_RECOGNIZE_SPEECH) {
            Log.e(TAG, "Invalid action: $action")
            return false
        }
        
        val language = intent.getStringExtra(RecognizerIntent.EXTRA_LANGUAGE)
        if (language.isNullOrBlank()) {
            Log.e(TAG, "No language specified in intent")
            return false
        }
        
        val packageName = intent.getStringExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE)
        if (packageName != context.packageName) {
            Log.w(TAG, "Package name mismatch: expected=${context.packageName}, got=$packageName")
        }
        
        return true
    }
    
    /**
     * Get intent configuration summary
     */
    fun getIntentSummary(intent: Intent): String {
        return """Intent Configuration:
            ├── Action: ${intent.action}
            ├── Language: ${intent.getStringExtra(RecognizerIntent.EXTRA_LANGUAGE)}
            ├── Language Model: ${intent.getStringExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL)}
            ├── Max Results: ${intent.getIntExtra(RecognizerIntent.EXTRA_MAX_RESULTS, -1)}
            ├── Partial Results: ${intent.getBooleanExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)}
            ├── Calling Package: ${intent.getStringExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE)}
            └── Prompt: ${intent.getStringExtra(RecognizerIntent.EXTRA_PROMPT) ?: "None"}
        """.trimIndent()
    }
    
    /**
     * Create intent optimized for offline recognition
     */
    fun createOfflineIntent(
        mode: SpeechMode,
        language: String = currentLanguage
    ): Intent {
        val intent = createRecognitionIntent(mode, language, false)
        
        // Add offline-specific parameters
        intent.putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
        
        Log.d(TAG, "Created offline-optimized intent")
        return intent
    }
    
    /**
     * Create intent for quick command recognition
     */
    fun createQuickCommandIntent(
        language: String = currentLanguage,
        expectedCommands: List<String>? = null
    ): Intent {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, language)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
            
            // Quick timeout for commands
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 2000L)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 1000L)
        }
        
        // Add expected commands as hint if available
        expectedCommands?.let { commands ->
            if (commands.isNotEmpty()) {
                intent.putStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS, ArrayList(commands))
            }
        }
        
        Log.d(TAG, "Created quick command intent with ${expectedCommands?.size ?: 0} expected commands")
        return intent
    }
    
    // Getters for current configuration
    fun getCurrentLanguage(): String = currentLanguage
    fun getMaxResults(): Int = maxResults
    fun isPartialResultsEnabled(): Boolean = enablePartialResults
    fun getRecognitionTimeout(): Long = recognitionTimeout
    
    /**
     * Set default language for future intents
     */
    fun setDefaultLanguage(language: String) {
        currentLanguage = language
        Log.d(TAG, "Default language set to: $language")
    }
    
    /**
     * Reset to default configuration
     */
    fun reset() {
        currentLanguage = "en-US"
        enablePartialResults = true
        maxResults = DEFAULT_MAX_RESULTS
        recognitionTimeout = DEFAULT_TIMEOUT_MS
        Log.d(TAG, "AndroidIntent reset to defaults")
    }
}