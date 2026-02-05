/**
 * VoiceAvanueAccessibilityService.kt - Minimal app-level accessibility service
 *
 * Required because:
 * 1. VoiceOSAccessibilityService is abstract (requires getActionCoordinator())
 * 2. Android manifest must declare a concrete service class in the app
 *
 * All core logic is in VoiceOSCore module - this is just the required wrapper.
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 */

package com.augmentalis.voiceavanue.service

import android.content.Context
import android.provider.Settings
import android.util.Log
import com.augmentalis.database.DatabaseDriverFactory
import com.augmentalis.database.VoiceOSDatabaseManager
import com.augmentalis.voiceoscore.VoiceOSAccessibilityService
import com.augmentalis.voiceoscore.VoiceOSCore
import com.augmentalis.voiceoscore.ActionCoordinator
import com.augmentalis.voiceoscore.BoundsResolver
import com.augmentalis.voiceoscore.CommandRegistry
import com.augmentalis.voiceoscore.QuantizedCommand
import com.augmentalis.voiceoscore.ServiceConfiguration
import com.augmentalis.voiceoscore.SpeechEngine
import com.augmentalis.voiceoscore.createForAndroid
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

private const val TAG = "VoiceAvanueService"

/**
 * Minimal accessibility service wrapper - all logic is in VoiceOSCore.
 */
class VoiceAvanueAccessibilityService : VoiceOSAccessibilityService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private var voiceOSCore: VoiceOSCore? = null
    private var actionCoordinator: ActionCoordinator? = null
    private var boundsResolver: BoundsResolver? = null

    // Required abstract implementation
    override fun getActionCoordinator(): ActionCoordinator {
        return actionCoordinator ?: ActionCoordinator(CommandRegistry()).also {
            actionCoordinator = it
        }
    }

    override fun getBoundsResolver(): BoundsResolver? = boundsResolver

    override fun onServiceReady() {
        instance = this
        serviceScope.launch(Dispatchers.IO) {
            try {
                // Initialize database
                val db = VoiceOSDatabaseManager.getInstance(DatabaseDriverFactory(applicationContext))
                db.waitForInitialization()

                // Initialize BoundsResolver
                boundsResolver = BoundsResolver(this@VoiceAvanueAccessibilityService)

                // Create shared ActionCoordinator
                val registry = CommandRegistry()
                actionCoordinator = ActionCoordinator(registry)

                // Create VoiceOSCore
                voiceOSCore = VoiceOSCore.createForAndroid(
                    service = this@VoiceAvanueAccessibilityService,
                    configuration = ServiceConfiguration(
                        speechEngine = SpeechEngine.ANDROID_STT.name,
                        voiceLanguage = "en-US",
                        confidenceThreshold = 0.7f,
                        autoStartListening = false,
                        synonymsEnabled = true,
                        debugMode = true
                    ),
                    commandRegistry = registry
                )
                voiceOSCore?.initialize()

                Log.i(TAG, "VoiceOSCore initialized")
            } catch (e: Exception) {
                Log.e(TAG, "Initialization failed", e)
            }
        }
    }

    override fun onCommandsUpdated(commands: List<QuantizedCommand>) {
        serviceScope.launch { voiceOSCore?.updateCommands(commands.map { it.phrase }) }
    }

    override fun onDestroy() {
        serviceScope.launch { voiceOSCore?.dispose() }
        serviceScope.cancel()
        instance = null
        super.onDestroy()
    }

    companion object {
        @Volatile private var instance: VoiceAvanueAccessibilityService? = null
        fun getInstance(): VoiceAvanueAccessibilityService? = instance

        fun isEnabled(context: Context): Boolean {
            val services = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            ) ?: return false
            return services.contains("${context.packageName}/${VoiceAvanueAccessibilityService::class.java.canonicalName}")
        }
    }
}
