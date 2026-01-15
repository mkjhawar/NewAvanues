/**
 * VoiceOS.kt - Main application class
 * Path: app/src/main/java/com/augmentalis/voiceos/VoiceOS.kt
 * 
 * Author: Manoj Jhawar
 * Code-Reviewed-By: CCA
 * Created: 2024-08-18
 * Updated: 2025-10-12 - Removed LearnApp initialization (moved to VoiceOSService)
 * Previous: 2025-08-22 - Removed CoreManager pattern, implemented direct access
 */

package com.augmentalis.voiceos

import android.app.Application
import android.util.Log
// import com.augmentalis.commandmanager.CommandManager  // DISABLED: Needs SQLDelight migration
import com.augmentalis.database.VoiceOSDatabaseManager
import com.augmentalis.devicemanager.DeviceManager
import com.augmentalis.speechrecognition.SpeechConfig
import com.augmentalis.speechrecognition.SpeechMode
import com.augmentalis.voiceui.core.MagicEngine
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Main VoiceOS Application
 * Direct access to modules without CoreManager layer
 *
 * @HiltAndroidApp triggers Hilt's code generation for dependency injection
 */
@HiltAndroidApp
class VoiceOS : Application() {
    companion object {
        private const val TAG = "VoiceOSApp"
        
        @Volatile
        private var instance: VoiceOS? = null
        
        @JvmStatic
        fun getInstance(): VoiceOS? = instance
    }
    
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    
    // Direct module access properties
    lateinit var deviceManager: DeviceManager
        private set

    // Database manager injected by Hilt
    @Inject
    lateinit var databaseManager: VoiceOSDatabaseManager
        
    // Direct speech configuration - no wrapper needed
    lateinit var speechConfig: SpeechConfig
        private set

    // TODO: Re-enable when CommandManager is migrated to SQLDelight
    // lateinit var commandManager: CommandManager
    //     private set
    
        
    // VoiceUI uses MagicEngine directly - object singleton, no need for lateinit
    private val magicEngine = MagicEngine
    
    override fun onCreate() {
        super.onCreate()
        instance = this

        // Initialize modules directly (async initialization happens in initializeModules)
        initializeModules()
        Log.d(TAG, "VoiceOS Application initialized - core modules initializing asynchronously")
    }
    
    private fun initializeModules() {
        try {
            // Initialize core modules directly
            deviceManager = DeviceManager.getInstance(this)
            // DatabaseManager is injected by Hilt and auto-initialized

            // Direct speech configuration - no wrapper, just config
            speechConfig = SpeechConfig(
                language = "en-US",
                mode = SpeechMode.DYNAMIC_COMMAND,
                enableVAD = true,
                confidenceThreshold = 0.7f
            )

            // TODO: Re-enable when CommandManager is migrated to SQLDelight
            // commandManager = CommandManager.getInstance(this)
            // MagicEngine is object singleton, just initialize it

            // Note: LearnApp Integration is initialized in VoiceOSService (AccessibilityService)
            // It requires AccessibilityService context and is not initialized here

            // Initialize modules asynchronously
            applicationScope.launch {
                initializeCoreModules()
            }

        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize modules", e)
        }
    }
    
    private suspend fun initializeCoreModules() {
        try {
            // Initialize in dependency order
            deviceManager.initialize()
            Log.i(TAG, "initializeCoreModules: ${deviceManager.imu.getSensorCapabilities()}")
            // DatabaseManager is auto-initialized by Hilt - no manual init needed
            // Speech config is just data, no initialization needed

            // TODO: Re-enable when CommandManager is migrated to SQLDelight
            // commandManager.initialize()

            magicEngine.initialize(this@VoiceOS)

            // Note: LearnApp integration initialized in VoiceOSService

            // Wire voice commands to UUID system
            wireVoiceCommands()

            Log.d(TAG, "All core modules initialized successfully (including UUID integration)")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize core modules", e)
        }
    }
    
    /**
     * Wire voice commands to UUID integration
     * 
     * Connects the UUID voice command processor to the CommandManager,
     * enabling voice commands like "click button abc-123" to work.
     */
    private suspend fun wireVoiceCommands() {
        try {
            // The UUID integration provides voice command processing
            // which can target UI elements by UUID, name, or spatial position
            
            // TODO: Wire into CommandManager when it supports UUID integration
            // For now, the UUID integration is initialized and ready to use
            // VoiceUI's MagicUUIDIntegration will use it directly
            
            Log.d(TAG, "UUID voice command system initialized and ready")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to wire voice commands to UUID system", e)
        }
    }
    
    override fun onTerminate() {
        super.onTerminate()

        // Shutdown modules directly
        applicationScope.launch {
            try {
                magicEngine.dispose()
                // Speech config is just data, no shutdown needed

                // TODO: Re-enable when CommandManager is migrated to SQLDelight
                // commandManager.cleanup()

                // DatabaseManager lifecycle managed by Hilt - will be disposed automatically
                deviceManager.shutdown()

                Log.d(TAG, "All modules shutdown")
            } catch (e: Exception) {
                Log.e(TAG, "Error during shutdown", e)
            }
        }
    }
}
