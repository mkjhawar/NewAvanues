/**
 * AvaUnifiedApplication.kt - Application entry point
 *
 * Initializes all KMP modules and services:
 * - VoiceOSCore for voice commands and accessibility
 * - WebAvanue for browser functionality
 * - VoiceCursor for eye tracking and gaze control
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 */

package com.augmentalis.avaunified

import android.app.Application
import android.util.Log
import com.augmentalis.voiceoscore.rpc.VoiceOSAvuRpcServer
import com.augmentalis.voiceoscore.rpc.VoiceOSServerConfig
import com.augmentalis.webavanue.rpc.WebAvanueJsonRpcServer
import com.augmentalis.webavanue.rpc.WebAvanueServerConfig
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

private const val TAG = "AvaUnified"

@HiltAndroidApp
class AvaUnifiedApplication : Application() {

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    // RPC Servers
    private var voiceOSRpcServer: VoiceOSAvuRpcServer? = null
    private var webAvanueRpcServer: WebAvanueJsonRpcServer? = null

    override fun onCreate() {
        super.onCreate()
        instance = this

        Log.i(TAG, "AVA Unified starting...")
        Log.i(TAG, "Version: ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")

        initializeModules()
    }

    private fun initializeModules() {
        applicationScope.launch(Dispatchers.IO) {
            try {
                // Initialize logging
                initializeLogging()

                // Initialize database
                initializeDatabase()

                // Note: RPC servers are started when services are bound
                // See RpcServerService for actual server initialization

                Log.i(TAG, "AVA Unified modules initialized")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to initialize modules", e)
            }
        }
    }

    private fun initializeLogging() {
        // Configure logging for debug builds
        if (BuildConfig.DEBUG) {
            Log.i(TAG, "Debug logging enabled")
        }
    }

    private fun initializeDatabase() {
        // Database initialization is handled by Hilt modules
        Log.i(TAG, "Database initialization delegated to DI")
    }

    /**
     * Start RPC servers for inter-module communication
     */
    fun startRpcServers(
        voiceOSDelegate: com.augmentalis.voiceoscore.rpc.IVoiceOSServiceDelegate,
        webAvanueDelegate: com.augmentalis.webavanue.rpc.IWebAvanueServiceDelegate
    ) {
        applicationScope.launch(Dispatchers.IO) {
            try {
                // Start VoiceOS AVU RPC server on port 50051
                voiceOSRpcServer = VoiceOSAvuRpcServer(
                    delegate = voiceOSDelegate,
                    config = VoiceOSServerConfig(port = 50051)
                ).also { it.start() }

                // Start WebAvanue JSON-RPC server on port 50055
                webAvanueRpcServer = WebAvanueJsonRpcServer(
                    delegate = webAvanueDelegate,
                    config = WebAvanueServerConfig(port = 50055)
                ).also { it.start() }

                Log.i(TAG, "RPC servers started - VoiceOS:50051, WebAvanue:50055")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start RPC servers", e)
            }
        }
    }

    /**
     * Stop RPC servers
     */
    fun stopRpcServers() {
        voiceOSRpcServer?.stop()
        voiceOSRpcServer = null

        webAvanueRpcServer?.stop()
        webAvanueRpcServer = null

        Log.i(TAG, "RPC servers stopped")
    }

    override fun onTerminate() {
        super.onTerminate()
        stopRpcServers()
    }

    companion object {
        @Volatile
        private var instance: AvaUnifiedApplication? = null

        fun getInstance(): AvaUnifiedApplication =
            instance ?: throw IllegalStateException("Application not initialized")
    }
}
