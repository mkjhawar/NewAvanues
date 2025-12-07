/**
 * VivokaInitializationManager.kt - Vivoka-specific initialization with comprehensive error handling
 * 
 * Copyright (C) Augmentalis Inc, Intelligent Devices LLC
 * Author: VOS4 Development Team  
 * Created: 2025-09-06
 * 
 * Addresses critical "VSDK initialization failed" and "Cannot call 'Vsdk.init' multiple times" issues
 * Provides robust initialization with retry logic, graceful degradation, and proper cleanup
 */
package com.augmentalis.voiceos.speech.engines.vivoka

import android.content.Context
import android.util.Log
import com.augmentalis.voiceos.speech.engines.common.SdkInitializationManager
import com.augmentalis.voiceos.speech.engines.common.SdkInitializationManager.InitializationContext
import com.augmentalis.voiceos.speech.engines.common.SdkInitializationManager.InitializationResult
import com.augmentalis.voiceos.speech.engines.common.SdkInitializationManager.InitializationState
import com.vivoka.vsdk.Vsdk
import kotlinx.coroutines.*
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

/**
 * Vivoka-specific initialization manager with comprehensive error handling
 * Singleton pattern prevents multiple concurrent initialization attempts
 */
class VivokaInitializationManager private constructor() {
    
    companion object {
        @JvmStatic
        val instance: VivokaInitializationManager by lazy { VivokaInitializationManager() }
        private const val TAG = "VivokaInitManager"
        private const val VSDK_ASSETS_DIR = "vsdk"
    }
    
    /**
     * Initialize Vivoka VSDK with comprehensive error handling
     * 
     * @param context Application context
     * @param configPath Path to VSDK configuration file
     * @return InitializationResult with success status and detailed information
     */
    suspend fun initializeVivoka(
        context: Context,
        configPath: String
    ): InitializationResult {
        
        val initContext = InitializationContext(
            sdkName = "Vivoka_VSDK",
            configPath = configPath,
            context = context,
            requiredAssets = listOf("vsdk.json", "models/"),
            initializationTimeout = 30000L, // 30 seconds
            maxRetries = 3,
            baseDelayMs = 1000L, // Start with 1 second delay
            backoffMultiplier = 2.0 // Double delay each retry
        )
        
        return SdkInitializationManager.initializeSDK(initContext) { ctx ->
            performVivokaInitialization(ctx)
        }
    }
    
    /**
     * Perform the actual Vivoka initialization with proper error handling
     */
    private suspend fun performVivokaInitialization(
        context: InitializationContext
    ): InitializationResult {
        
        val startTime = System.currentTimeMillis()
        
        try {
            Log.i(TAG, "Starting Vivoka VSDK initialization")
            
            // Step 1: Pre-initialization validation
            val validationError = validatePrerequisites(context)
            if (validationError != null) {
                return InitializationResult(
                    success = false,
                    state = InitializationState.FAILED,
                    error = validationError
                )
            }
            
            // Step 2: Cleanup any existing state
            cleanupExistingVSDK()
            
            // Step 3: Initialize VSDK core
            initializeVSDKCore(context)
            
            // Step 4: Initialize ASR Engine
            initializeASREngine(context.context)
            
            val duration = System.currentTimeMillis() - startTime
            Log.i(TAG, "Vivoka VSDK initialized successfully in ${duration}ms")
            
            return InitializationResult(
                success = true,
                state = InitializationState.INITIALIZED,
                initializationTime = duration,
                metadata = mapOf(
                    "vsdk_version" to getVSDKVersion(),
                    "config_path" to context.configPath,
                    "initialization_time_ms" to duration
                )
            )
            
        } catch (e: Exception) {
            Log.e(TAG, "Vivoka initialization failed", e)
            
            // Attempt graceful degradation
            return attemptDegradedInitialization(context, e)
        }
    }
    
    /**
     * Validate prerequisites before attempting initialization
     */
    private suspend fun validatePrerequisites(context: InitializationContext): String? {
        
        try {
            // Check if assets exist and are valid
            if (!validateAssets(context.configPath, context.requiredAssets)) {
                return "Required VSDK assets missing or invalid at: ${context.configPath}"
            }
            
            // Check if we have required permissions
            if (!hasRequiredPermissions(context.context)) {
                return "Required permissions not granted for VSDK initialization"
            }
            
            // Check available memory
            val availableMemory = getAvailableMemory()
            if (availableMemory < 50 * 1024 * 1024) { // 50MB minimum
                Log.w(TAG, "Low memory warning: ${availableMemory / 1024 / 1024}MB available")
                // Don't fail, just warn
            }
            
            Log.d(TAG, "Prerequisites validation passed")
            return null
            
        } catch (e: Exception) {
            Log.e(TAG, "Prerequisites validation failed", e)
            return "Prerequisites validation failed: ${e.message}"
        }
    }
    
    /**
     * Initialize VSDK core with proper error handling and enhanced asset timing protection
     */
    private suspend fun initializeVSDKCore(context: InitializationContext): Unit = withContext(Dispatchers.IO) {
        
        try {
            // CRITICAL FIX: Check if already initialized before calling init
            if (Vsdk.isInitialized()) {
                Log.i(TAG, "VSDK already initialized, skipping init call")
                return@withContext
            }
            
            // ENHANCED FIX: Ensure assets are fully synchronized before VSDK init
            Log.d(TAG, "Performing enhanced asset validation and filesystem sync")
            ensureAssetsReady(context.configPath)
            
            Log.d(TAG, "Calling Vsdk.init() with config: ${context.configPath}")
            
            val result = suspendCoroutine<Boolean> { continuation ->
                try {
                    Vsdk.init(context.context, context.configPath) { success ->
                        Log.d(TAG, "Vsdk.init() callback received: success=$success")
                        continuation.resume(success)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Exception during Vsdk.init() call", e)
                    continuation.resumeWithException(e)
                }
            }
            
            if (!result) {
                throw InitializationException("VSDK initialization callback returned false")
            }
            
            // Verify VSDK is actually initialized
            if (!Vsdk.isInitialized()) {
                throw InitializationException("VSDK reports not initialized despite successful callback")
            }
            
            Log.i(TAG, "VSDK core initialization completed successfully")
            
        } catch (e: Exception) {
            Log.e(TAG, "VSDK core initialization failed", e)
            throw InitializationException("VSDK initialization failed: ${e.message}", e)
        }
    }
    
    /**
     * Initialize ASR Engine with proper error handling  
     */
    private suspend fun initializeASREngine(context: Context): Unit = withContext(Dispatchers.IO) {
        
        try {
            Log.d(TAG, "Initializing ASR Engine")
            
            val result = suspendCoroutine<Boolean> { continuation ->
                try {
                    com.vivoka.vsdk.asr.csdk.Engine.getInstance().init(context) { success ->
                        Log.d(TAG, "ASR Engine init callback: success=$success")
                        continuation.resume(success)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Exception during ASR Engine initialization", e)
                    continuation.resumeWithException(e)
                }
            }
            
            if (!result) {
                throw InitializationException("ASR Engine initialization callback returned false")
            }
            
            Log.i(TAG, "ASR Engine initialized successfully")
            
        } catch (e: Exception) {
            Log.e(TAG, "ASR Engine initialization failed", e)
            throw InitializationException("ASR Engine initialization failed: ${e.message}", e)
        }
    }
    
    /**
     * Clean up existing VSDK state before initialization
     */
    private suspend fun cleanupExistingVSDK() = withContext(Dispatchers.IO) {
        
        try {
            Log.d(TAG, "Cleaning up existing VSDK state")
            
            // Cleanup ASR Engine first
            try {
                com.vivoka.vsdk.asr.csdk.Engine.getInstance().destroy()
                Log.d(TAG, "ASR Engine destroyed successfully")
            } catch (e: Exception) {
                Log.w(TAG, "ASR Engine cleanup failed (may not be initialized): ${e.message}")
                // Don't fail cleanup due to ASR Engine issues
            }
            
            // Note: VSDK doesn't provide a public cleanup/destroy method
            // The singleton pattern in VSDK handles internal state management
            
            // Force garbage collection to clean up native resources
            System.gc()
            delay(500) // Give GC time to work
            
            Log.d(TAG, "VSDK cleanup completed")
            
        } catch (e: Exception) {
            Log.w(TAG, "VSDK cleanup encountered issues: ${e.message}")
            // Don't fail initialization due to cleanup issues
        }
    }
    
    /**
     * Attempt graceful degradation when normal initialization fails
     */
    private suspend fun attemptDegradedInitialization(
        context: InitializationContext,
        originalError: Exception
    ): InitializationResult {
        
        Log.w(TAG, "Attempting degraded mode initialization after failure: ${originalError.message}")
        
        return try {
            // For Vivoka, degraded mode could mean:
            // 1. Offline-only mode (no cloud features)
            // 2. Simplified model loading
            // 3. Reduced functionality
            
            // Attempt to initialize with minimal configuration
            val degradedResult = initializeDegradedMode(context)
            
            if (degradedResult) {
                Log.i(TAG, "Successfully initialized in degraded mode")
                InitializationResult(
                    success = true,
                    state = InitializationState.DEGRADED,
                    degradedMode = true,
                    error = "Running in degraded mode due to: ${originalError.message}",
                    metadata = mapOf(
                        "degraded_mode" to true,
                        "original_error" to (originalError.message ?: "Unknown error"),
                        "degraded_features" to listOf("offline_only", "basic_models")
                    )
                )
            } else {
                Log.e(TAG, "Degraded mode initialization also failed")
                InitializationResult(
                    success = false,
                    state = InitializationState.FAILED,
                    error = "Both normal and degraded initialization failed: ${originalError.message}"
                )
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Degraded initialization failed", e)
            InitializationResult(
                success = false,
                state = InitializationState.FAILED,
                error = "All initialization attempts failed. Original: ${originalError.message}, Degraded: ${e.message}"
            )
        }
    }
    
    /**
     * Initialize in degraded mode with limited functionality
     */
    private suspend fun initializeDegradedMode(context: InitializationContext): Boolean {
        return try {
            Log.d(TAG, "Attempting degraded mode initialization for SDK: ${context.sdkName}")
            
            // Try simpler initialization without full VSDK features
            // This could involve:
            // 1. Loading only basic models
            // 2. Skipping cloud connectivity checks  
            // 3. Using fallback configurations
            
            // For now, return false as we need to implement specific degraded logic
            // This will be expanded based on Vivoka SDK capabilities
            false
            
        } catch (e: Exception) {
            Log.e(TAG, "Degraded mode initialization failed", e)
            false
        }
    }
    
    /**
     * Validate required assets exist and are accessible
     */
    private fun validateAssets(configPath: String, requiredAssets: List<String>): Boolean {
        
        try {
            val configFile = File(configPath)
            if (!configFile.exists() || !configFile.canRead()) {
                Log.e(TAG, "VSDK config file not found or not readable: $configPath")
                return false
            }
            
            val assetsDir = configFile.parentFile
            if (assetsDir == null || !assetsDir.exists()) {
                Log.e(TAG, "VSDK assets directory not found: ${configFile.parent}")
                return false
            }
            
            // Check required assets
            for (assetPath in requiredAssets) {
                val assetFile = File(assetsDir, assetPath)
                if (!assetFile.exists()) {
                    Log.w(TAG, "Required asset missing: ${assetFile.absolutePath}")
                    // Don't fail validation for missing assets, just log warning
                }
            }
            
            Log.d(TAG, "Assets validation passed")
            return true
            
        } catch (e: Exception) {
            Log.e(TAG, "Assets validation failed", e)
            return false
        }
    }
    
    /**
     * Check if required permissions are granted
     */
    @Suppress("UNUSED_PARAMETER")
    private fun hasRequiredPermissions(context: Context): Boolean {
        // Context will be used for permission checks once Vivoka VSDK requirements are finalized
        // Add specific permission checks for Vivoka VSDK if needed
        // For now, return true as permissions are typically handled at app level
        return true
    }
    
    /**
     * Get available memory in bytes
     */
    private fun getAvailableMemory(): Long {
        return try {
            Runtime.getRuntime().maxMemory() - Runtime.getRuntime().totalMemory() + Runtime.getRuntime().freeMemory()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to get available memory", e)
            Long.MAX_VALUE // Assume plenty of memory if we can't check
        }
    }
    
    /**
     * Get VSDK version information
     */
    private fun getVSDKVersion(): String {
        return try {
            // Get version from VSDK if available
            "Unknown" // Placeholder - implement based on VSDK API
        } catch (e: Exception) {
            "Unknown"
        }
    }
    
    /**
     * Check if VSDK is currently initialized
     */
    fun isVSDKInitialized(): Boolean {
        return try {
            Vsdk.isInitialized()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to check VSDK initialization status", e)
            false
        }
    }
    
    /**
     * Get current initialization state
     */
    fun getInitializationState(): InitializationState {
        return SdkInitializationManager.getInitializationState("Vivoka_VSDK")
    }
    
    /**
     * Force cleanup and reset (for testing or manual recovery)
     */
    suspend fun forceReset() {
        Log.w(TAG, "Force resetting Vivoka initialization state")
        cleanupExistingVSDK()
        SdkInitializationManager.resetInitializationState("Vivoka_VSDK")
    }
    
    /**
     * ENHANCED FIX: Ensure assets are fully ready before VSDK initialization
     * Addresses critical asset timing issues that cause "VSDK initialization failed" errors
     */
    private suspend fun ensureAssetsReady(configPath: String) = withContext(Dispatchers.IO) {
        
        try {
            Log.d(TAG, "Ensuring assets are fully ready for VSDK initialization")
            
            val configFile = File(configPath)
            if (!configFile.exists()) {
                throw InitializationException("Config file not found: $configPath")
            }
            
            // Step 1: Verify all critical files exist and are readable
            val criticalFiles = listOf(
                configFile,
                File(configFile.parent, "models"),
                File(configFile.parent, "grammars")
            )
            
            for (file in criticalFiles) {
                if (file.exists()) {
                    // Multiple validation passes for timing-sensitive files
                    for (attempt in 1..3) {
                        if (!file.canRead()) {
                            Log.w(TAG, "File not readable on attempt $attempt: ${file.absolutePath}")
                            delay(200) // Wait for filesystem
                        } else {
                            Log.d(TAG, "File validated: ${file.absolutePath}")
                            break
                        }
                    }
                    
                    // Final validation
                    if (!file.canRead()) {
                        throw InitializationException("File not readable after retries: ${file.absolutePath}")
                    }
                } else {
                    Log.w(TAG, "Expected file missing: ${file.absolutePath}")
                }
            }
            
            // Step 2: Enhanced filesystem synchronization
            Log.d(TAG, "Performing enhanced filesystem synchronization")
            
            // Force filesystem sync for critical directories
            val assetsDir = configFile.parentFile
            assetsDir?.let { dir ->
                try {
                    // Touch a test file to force directory sync
                    val testFile = File(dir, ".vsdk_sync_test")
                    testFile.writeText("sync_test")
                    testFile.delete()
                    
                    Log.d(TAG, "Directory sync test completed")
                } catch (e: Exception) {
                    Log.w(TAG, "Directory sync test failed: ${e.message}")
                }
            }
            
            // Step 3: Progressive delay with validation
            val delays = listOf(100L, 300L, 500L)
            
            for ((index, delayMs) in delays.withIndex()) {
                Log.d(TAG, "Asset readiness check ${index + 1}/${delays.size} - waiting ${delayMs}ms")
                delay(delayMs)
                
                // Validate config file is still readable
                if (!configFile.canRead()) {
                    throw InitializationException("Config file became unreadable during sync: $configPath")
                }
                
                // Check file size stability (ensure not still being written)
                val initialSize = configFile.length()
                delay(100)
                val finalSize = configFile.length()
                
                if (initialSize != finalSize) {
                    Log.w(TAG, "Config file size changed during validation (${initialSize} -> ${finalSize}), waiting...")
                    delay(500)
                } else {
                    Log.d(TAG, "Config file size stable: $finalSize bytes")
                }
            }
            
            // Step 4: Final validation with detailed logging
            Log.d(TAG, "Performing final asset validation")
            
            val configContent = try {
                configFile.readText()
            } catch (e: Exception) {
                throw InitializationException("Failed to read config file: ${e.message}")
            }
            
            if (configContent.isEmpty()) {
                throw InitializationException("Config file is empty: $configPath")
            }
            
            Log.i(TAG, "Asset readiness validation completed successfully")
            Log.d(TAG, "Config file size: ${configFile.length()} bytes, path: $configPath")
            
        } catch (e: Exception) {
            Log.e(TAG, "Asset readiness validation failed", e)
            throw InitializationException("Asset validation failed: ${e.message}", e)
        }
    }
}

/**
 * Custom exception for Vivoka initialization failures
 */
class InitializationException(
    message: String,
    cause: Throwable? = null
) : Exception(message, cause)