/**
 * VoiceOS.kt - Main application class
 * Path: app/src/main/java/com/augmentalis/voiceos/VoiceOS.kt
 * 
 * Author: Manoj Jhawar
 * Code-Reviewed-By: CCA
 * Created: 2024-08-18
 * Updated: 2025-08-22 - Removed CoreManager pattern, implemented direct access
 */

package com.augmentalis.voiceos

import android.app.Application
import android.util.Log
import com.augmentalis.devicemanager.DeviceManager
import com.augmentalis.commandmanager.CommandManager
import com.augmentalis.datamanager.core.DatabaseModule
import com.augmentalis.speechrecognition.SpeechConfig
import com.augmentalis.speechrecognition.SpeechEngine
import com.augmentalis.speechrecognition.SpeechMode
import com.augmentalis.voiceui.core.MagicEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Main VoiceOS Application
 * Direct access to modules without CoreManager layer
 */
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
        
    lateinit var dataManager: DatabaseModule
        private set
        
    // Direct speech configuration - no wrapper needed
    lateinit var speechConfig: SpeechConfig
        private set
        
    lateinit var commandManager: CommandManager
        private set
        
    // VoiceUI uses MagicEngine directly - object singleton, no need for lateinit
    private val magicEngine = MagicEngine
    
    override fun onCreate() {
        super.onCreate()
        instance = this
        
        // Initialize modules directly
        initializeModules()
        
        Log.d(TAG, "VoiceOS Application initialized with direct access")
    }
    
    private fun initializeModules() {
        try {
            // Initialize core modules directly
            deviceManager = DeviceManager.getInstance(this)
            dataManager = DatabaseModule(this)
            
            // Direct speech configuration - no wrapper, just config
            speechConfig = SpeechConfig(
                language = "en-US",
                mode = SpeechMode.DYNAMIC_COMMAND,
                enableVAD = true,
                confidenceThreshold = 0.7f
            )
            
            commandManager = CommandManager.getInstance(this)
            // MagicEngine is object singleton, just initialize it
            
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
            dataManager.initialize()
            // Speech config is just data, no initialization needed
            commandManager.initialize()
            magicEngine.initialize(this@VoiceOS)
            
            Log.d(TAG, "All core modules initialized successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize core modules", e)
        }
    }
    
    override fun onTerminate() {
        super.onTerminate()
        
        // Shutdown modules directly
        applicationScope.launch {
            try {
                magicEngine.dispose()
                // Speech config is just data, no shutdown needed
                commandManager.cleanup()
                dataManager.shutdown()
                deviceManager.shutdown()
                
                Log.d(TAG, "All modules shutdown")
            } catch (e: Exception) {
                Log.e(TAG, "Error during shutdown", e)
            }
        }
    }
}