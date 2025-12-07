package com.augmentalis.voiceos.speech.engines.vivoka

import android.content.Context
import android.util.Log
import com.vivoka.vsdk.Vsdk
import com.vivoka.vsdk.asr.csdk.recognizer.Recognizer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Handles Vivoka VSDK initialization with proper license management
 * and error recovery mechanisms
 */
class VivokaInitializer(private val context: Context) {
    
    companion object {
        private const val TAG = "VivokaInitializer"
        private const val LICENSE_FILE = "vivoka_license.key"
        private const val MODELS_DIR = "vivoka_models"
        private const val DEFAULT_TIMEOUT_MS = 30000L
        
        // Initialization states
        enum class InitState {
            NOT_INITIALIZED,
            INITIALIZING,
            INITIALIZED,
            FAILED,
            DEGRADED
        }
    }
    
    private var currentState = InitState.NOT_INITIALIZED
    private var initializationError: String? = null
    
    /**
     * Initialize the Vivoka SDK with proper error handling
     * Following legacy Avenue implementation pattern with multi-location fallback
     * @return true if initialization successful, false otherwise
     */
    suspend fun initialize(): Boolean = withContext(Dispatchers.IO) {
        if (currentState == InitState.INITIALIZED) {
            Log.d(TAG, "Vivoka SDK already initialized")
            return@withContext true
        }

        if (currentState == InitState.INITIALIZING) {
            Log.w(TAG, "Vivoka SDK initialization already in progress")
            return@withContext false
        }

        currentState = InitState.INITIALIZING

        try {
            Log.i(TAG, "Starting Vivoka SDK initialization")

            // Step 1: Use path resolver to find VSDK in multiple locations
            val pathResolver = VivokaPathResolver(context)
            Log.d(TAG, "Searching for VSDK in locations:\n${pathResolver.getSearchPathsForLogging()}")

            val vsdkDir = pathResolver.resolveVsdkPath()
            val assetsPath = vsdkDir.absolutePath

            // Step 1a: If VSDK not found in any external location, extract from APK assets
            if (!checkVivokaFilesExist(assetsPath)) {
                Log.i(TAG, "VSDK not found in any location, extracting from APK assets to: $assetsPath")
                extractAssets(context, "vsdk", assetsPath)
            } else {
                Log.i(TAG, "Found existing VSDK at: $assetsPath")
            }

            // Step 2: Get config file path
            val configPath = getConfigFilePath(assetsPath)
            if (configPath.isNullOrEmpty()) {
                Log.e(TAG, "Config file not found")
                currentState = InitState.FAILED
                initializationError = "Config file missing"
                return@withContext false
            }
            
            // Step 3: Initialize SDK with config path (no separate license needed)
            Log.d(TAG, "Initializing Vsdk with config path: $configPath")
            Vsdk.init(context, configPath) { success ->
                if (!success) {
                    Log.e(TAG, "Vsdk.init() callback returned false")
                    currentState = InitState.FAILED
                    initializationError = "SDK initialization failed"
                }
            }
            
            // Step 3: Configure ASR components
            configureASR()
            
            // Step 4: Verify models are available
            if (!verifyModels()) {
                Log.w(TAG, "Models not available, entering degraded mode")
                currentState = InitState.DEGRADED
                initializationError = "Models not available"
                // Continue in degraded mode - models can be downloaded later
            } else {
                currentState = InitState.INITIALIZED
            }
            
            // Step 5: Set up crash handlers
            setupCrashHandlers()
            
            Log.i(TAG, "Vivoka SDK initialization complete: $currentState")
            return@withContext currentState == InitState.INITIALIZED || 
                               currentState == InitState.DEGRADED
            
        } catch (e: Exception) {
            Log.e(TAG, "Vivoka SDK initialization failed", e)
            currentState = InitState.FAILED
            initializationError = e.message
            return@withContext false
        }
    }
    
    /**
     * Check if Vivoka files exist in the specified path
     * Following legacy VsdkHandlerUtils pattern
     */
    private fun checkVivokaFilesExist(assetsPath: String): Boolean {
        val configFile = File(assetsPath, "config/vsdk.json")
        val dataDir = File(assetsPath, "data/csdk")
        return configFile.exists() && dataDir.exists()
    }
    
    /**
     * Extract assets to the specified path
     * Uses Vivoka's AssetsExtractor utility
     */
    private fun extractAssets(context: Context, assetPath: String, targetPath: String) {
        try {
            // Use Vivoka's built-in AssetsExtractor
            com.vivoka.vsdk.util.AssetsExtractor.extract(context, assetPath, targetPath)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to extract assets", e)
            throw e
        }
    }
    
    /**
     * Get the config file path
     * Following legacy pattern
     */
    private fun getConfigFilePath(assetsPath: String): String? {
        val configFile = File(assetsPath, "config/vsdk.json")
        return if (configFile.exists()) {
            configFile.absolutePath
        } else {
            null
        }
    }
    
    /**
     * Configure ASR (Automatic Speech Recognition) settings
     */
    private fun configureASR() {
        try {
            // Configure recognizer settings
            // TODO: Implement when Vivoka SDK provides these methods
            // Use debug flag from context for log level
            // val isDebug = (context.applicationInfo.flags and
            //              android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0
            // Recognizer.setLogLevel(if (isDebug) 2 else 0)
            
            // Set default parameters - TODO: Enable when SDK supports
            // Recognizer.setMaxRecordingDuration(60000) // 60 seconds max
            // Recognizer.setSilenceDetection(true)
            // Recognizer.setBeamWidth(10)
            
            // Configure audio settings - TODO: Enable when SDK supports
            // Recognizer.setSampleRate(16000)
            // Recognizer.setAudioFormat(Recognizer.AudioFormat.PCM_16BIT)
            
            Log.d(TAG, "ASR configuration complete")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to configure ASR", e)
            // Non-fatal, continue with defaults
        }
    }
    
    /**
     * Verify that required models are available
     */
    private fun verifyModels(): Boolean {
        val modelsDir = File(context.filesDir, MODELS_DIR)
        if (!modelsDir.exists() || !modelsDir.isDirectory) {
            Log.w(TAG, "Models directory does not exist: ${modelsDir.absolutePath}")
            return false
        }
        
        // Check for required model files
        val requiredModels = listOf(
            "acoustic_model.vsdk",
            "language_model.vsdk",
            "lexicon.vsdk"
        )
        
        for (modelFile in requiredModels) {
            val model = File(modelsDir, modelFile)
            if (!model.exists()) {
                Log.w(TAG, "Missing model file: $modelFile")
                return false
            }
        }
        
        Log.d(TAG, "All required models verified")
        return true
    }
    
    /**
     * Set up crash handlers for SDK issues
     */
    private fun setupCrashHandlers() {
        Thread.setDefaultUncaughtExceptionHandler { _, exception ->
            if (exception.stackTrace.any { it.className.startsWith("com.vivoka") }) {
                Log.e(TAG, "Vivoka SDK crash detected", exception)
                // Attempt to reinitialize in next session
                currentState = InitState.FAILED
                initializationError = "SDK crash: ${exception.message}"
            }
            // Re-throw for default handling
            throw exception
        }
    }
    
    /**
     * Shutdown the SDK and clean up resources
     */
    suspend fun shutdown() = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Shutting down Vivoka SDK")
            // TODO: Implement cleanup when SDK provides method
            // Vsdk.cleanup()
            currentState = InitState.NOT_INITIALIZED
            initializationError = null
        } catch (e: Exception) {
            Log.e(TAG, "Error during shutdown", e)
        }
    }
    
    /**
     * Get current initialization state
     */
    fun getState(): InitState = currentState
    
    /**
     * Get initialization error if any
     */
    fun getError(): String? = initializationError
    
    /**
     * Check if SDK is ready for use
     */
    fun isReady(): Boolean = 
        currentState == InitState.INITIALIZED || 
        currentState == InitState.DEGRADED
    
    /**
     * Force re-initialization (useful after errors)
     */
    suspend fun reinitialize(): Boolean {
        shutdown()
        return initialize()
    }
}