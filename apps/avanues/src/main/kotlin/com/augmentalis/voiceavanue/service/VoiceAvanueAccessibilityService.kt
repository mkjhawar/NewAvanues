/**
 * VoiceAvanueAccessibilityService.kt - App-level accessibility service
 *
 * Required because:
 * 1. VoiceOSAccessibilityService is abstract (requires getActionCoordinator())
 * 2. Android manifest must declare a concrete service class in the app
 *
 * All core logic is in VoiceOSCore module.
 * This wrapper adds:
 * - CommandOverlayService lifecycle management
 * - DynamicCommandGenerator for numbered badge overlays
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
import com.augmentalis.voiceavanue.data.DeveloperPreferencesRepository
import com.augmentalis.voiceavanue.data.DeveloperSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout

private const val TAG = "VoiceAvanueService"

/**
 * Accessibility service wrapper with overlay badge support.
 * Core command generation is in VoiceOSCore; this adds overlay display.
 */
class VoiceAvanueAccessibilityService : VoiceOSAccessibilityService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private var voiceOSCore: VoiceOSCore? = null
    private var actionCoordinator: ActionCoordinator? = null
    private var boundsResolver: BoundsResolver? = null
    private var dynamicCommandGenerator: DynamicCommandGenerator? = null

    override fun getActionCoordinator(): ActionCoordinator {
        return actionCoordinator ?: ActionCoordinator(commandRegistry = CommandRegistry()).also {
            actionCoordinator = it
        }
    }

    override fun getBoundsResolver(): BoundsResolver? = boundsResolver

    override fun onServiceReady() {
        instance = this

        // Start the overlay service
        try {
            if (Settings.canDrawOverlays(applicationContext)) {
                CommandOverlayService.start(applicationContext)
                Log.i(TAG, "CommandOverlayService started")
            } else {
                Log.w(TAG, "Overlay permission not granted, skipping overlay service")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start CommandOverlayService", e)
        }

        // Initialize the dynamic command generator for overlay badges
        dynamicCommandGenerator = DynamicCommandGenerator(resources)

        serviceScope.launch(Dispatchers.IO) {
            try {
                val db = VoiceOSDatabaseManager.getInstance(DatabaseDriverFactory(applicationContext))
                db.waitForInitialization()

                boundsResolver = BoundsResolver(this@VoiceAvanueAccessibilityService)

                // Read developer settings from DataStore
                val devPrefs = DeveloperPreferencesRepository(applicationContext)
                val devSettings: DeveloperSettings = devPrefs.settings.first()

                val registry = CommandRegistry()
                actionCoordinator = ActionCoordinator(commandRegistry = registry)

                voiceOSCore = VoiceOSCore.createForAndroid(
                    service = this@VoiceAvanueAccessibilityService,
                    configuration = ServiceConfiguration(
                        speechEngine = devSettings.sttEngine,
                        voiceLanguage = devSettings.voiceLanguage,
                        confidenceThreshold = devSettings.confidenceThreshold,
                        autoStartListening = devSettings.autoStartListening,
                        synonymsEnabled = devSettings.synonymsEnabled,
                        debugMode = devSettings.debugMode
                    ),
                    commandRegistry = registry
                )
                voiceOSCore?.initialize()

                Log.i(TAG, "VoiceOSCore initialized with dev settings: engine=${devSettings.sttEngine}, lang=${devSettings.voiceLanguage}, confidence=${devSettings.confidenceThreshold}")
            } catch (e: Exception) {
                Log.e(TAG, "Initialization failed", e)
            }
        }
    }

    override fun onCommandsUpdated(commands: List<QuantizedCommand>) {
        serviceScope.launch {
            voiceOSCore?.updateCommands(commands.map { it.phrase })

            // Update overlay badges from accessibility tree
            try {
                val root = rootInActiveWindow ?: return@launch
                val packageName = root.packageName?.toString() ?: return@launch
                val isTargetApp = OverlayStateManager.TARGET_APPS.contains(packageName)

                dynamicCommandGenerator?.processScreen(root, packageName, isTargetApp)
            } catch (e: Exception) {
                Log.e(TAG, "Error updating overlay", e)
            }
        }
    }

    override fun onDestroy() {
        instance = null

        // Stop overlay service
        try {
            CommandOverlayService.stop(applicationContext)
        } catch (e: Exception) {
            Log.w(TAG, "Error stopping CommandOverlayService", e)
        }

        try {
            runBlocking {
                withTimeout(3000L) {
                    voiceOSCore?.dispose()
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "VoiceOSCore dispose timed out or failed", e)
        }
        serviceScope.cancel()
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
