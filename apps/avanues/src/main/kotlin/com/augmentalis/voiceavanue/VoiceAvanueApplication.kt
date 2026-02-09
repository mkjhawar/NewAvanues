/**
 * VoiceAvanueApplication.kt - Application entry point for Avanues
 *
 * Initializes all KMP modules and services:
 * - VoiceOSCore for voice commands, accessibility, and AVU DSL interpreter
 * - WebAvanue for full browser functionality (tabs, bookmarks, XR)
 * - VoiceCursor for eye tracking and gaze control
 *
 * Communication strategy:
 * - PRIMARY: AVU DSL interpreter pipeline (.vos/.avp text files)
 * - DORMANT: RPC servers (available on demand for 3rd-party compiled apps)
 *   RPC is NOT auto-started. Enable via Settings or call startRpcServers() explicitly.
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 */

package com.augmentalis.voiceavanue

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import android.util.Log
import com.augmentalis.devicemanager.DeviceCapabilityFactory
import com.augmentalis.voiceoscore.rpc.VoiceOSAvuRpcServer
import com.augmentalis.voiceoscore.rpc.VoiceOSServerConfig
import com.augmentalis.webavanue.rpc.WebAvanueJsonRpcServer
import com.augmentalis.webavanue.rpc.WebAvanueServerConfig
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

private const val TAG = "VoiceAvanue"

@HiltAndroidApp
class VoiceAvanueApplication : Application() {

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    // RPC Servers
    private var voiceOSRpcServer: VoiceOSAvuRpcServer? = null
    private var webAvanueRpcServer: WebAvanueJsonRpcServer? = null

    override fun onCreate() {
        super.onCreate()

        Log.i(TAG, "Avanues starting...")
        Log.i(TAG, "Version: ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")

        // Initialize DeviceManager for display profile detection
        DeviceCapabilityFactory.initialize(this)

        // Create notification channels early so system knows we send notifications
        createNotificationChannels()

        initializeModules()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java)

            val serviceChannel = NotificationChannel(
                "voiceos_service",
                "VoiceOS\u00AE Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Ongoing notification while speech recognition or voice cursor is active"
                setShowBadge(false)
            }

            val alertsChannel = NotificationChannel(
                "voiceos_alerts",
                "VoiceOS\u00AE Alerts",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Alerts when speech recognition encounters errors or needs attention"
            }

            val rpcChannel = NotificationChannel(
                "ava_rpc_service",
                "AVA Services",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Background service notifications for inter-module communication"
                setShowBadge(false)
            }

            manager.createNotificationChannels(listOf(serviceChannel, alertsChannel, rpcChannel))
            Log.i(TAG, "Notification channels created")
        }
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

                Log.i(TAG, "Avanues modules initialized")
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
}
