/**
 * VoiceOSService.kt - Main Accessibility Service for VoiceOS App
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2026-01-19
 *
 * Concrete implementation of the KMP VoiceOSAccessibilityService.
 * Bridges the app to the unified KMP voice command infrastructure.
 */
package com.augmentalis.voiceoscore.accessibility

import android.util.Log
import com.augmentalis.voiceoscore.ActionCoordinator
import com.augmentalis.voiceoscore.CommandRegistry
import com.augmentalis.voiceoscore.QuantizedCommand
import com.augmentalis.voiceoscore.ServiceConfiguration
import com.augmentalis.voiceoscore.VoiceOSAccessibilityService
import com.augmentalis.voiceoscore.VoiceOSCore
import com.augmentalis.voiceoscore.createForAndroid
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * VoiceOS Accessibility Service - App-level implementation.
 *
 * Extends the KMP VoiceOSAccessibilityService base class and provides:
 * 1. VoiceOSCore facade integration for voice command processing
 * 2. Speech result collection and command execution
 * 3. App-specific UI feedback (overlays, TTS, etc.)
 *
 * Voice Command Flow:
 * Vivoka/Speech Engine -> VoiceOSCore.speechResults -> processCommand() -> ActionCoordinator
 */
class VoiceOSService : VoiceOSAccessibilityService() {

    companion object {
        private const val TAG = "VoiceOSService"

        @Volatile
        private var instance: VoiceOSService? = null

        fun getInstance(): VoiceOSService? = instance
    }

    // Coroutine scope for service operations
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    // Shared command registry between VoiceOSCore and ActionCoordinator
    private val commandRegistry = CommandRegistry()

    // VoiceOSCore facade - main entry point for voice processing
    private var voiceOSCore: VoiceOSCore? = null

    // Action coordinator for command execution
    private var actionCoordinator: ActionCoordinator? = null

    // =========================================================================
    // VoiceOSAccessibilityService abstract method implementations
    // =========================================================================

    override fun getActionCoordinator(): ActionCoordinator {
        return actionCoordinator ?: throw IllegalStateException(
            "ActionCoordinator not initialized. Service not ready."
        )
    }

    override fun onServiceReady() {
        Log.i(TAG, "Service ready - initializing VoiceOSCore")
        instance = this
        initializeVoiceOSCore()
    }

    override fun onCommandsUpdated(commands: List<QuantizedCommand>) {
        Log.d(TAG, "Commands updated: ${commands.size} commands")
        // Update VoiceOSCore with new dynamic commands
        serviceScope.launch {
            voiceOSCore?.updateDynamicCommands(commands)
        }
    }

    override fun onCommandExecuted(command: QuantizedCommand, success: Boolean) {
        Log.d(TAG, "Command executed: ${command.phrase} - success: $success")
        // TODO: Add feedback (TTS, haptic, overlay)
    }

    // =========================================================================
    // VoiceOSCore initialization
    // =========================================================================

    private fun initializeVoiceOSCore() {
        serviceScope.launch {
            try {
                Log.d(TAG, "Creating VoiceOSCore instance...")

                // Create VoiceOSCore with Android-specific configuration
                voiceOSCore = VoiceOSCore.createForAndroid(
                    service = this@VoiceOSService,
                    configuration = ServiceConfiguration(
                        autoStartListening = true,
                        speechEngine = "VIVOKA",
                        debugMode = true
                    ),
                    commandRegistry = commandRegistry
                )

                // Initialize the facade
                voiceOSCore?.initialize()
                Log.i(TAG, "VoiceOSCore initialized successfully")

                // Create ActionCoordinator with shared registry
                actionCoordinator = ActionCoordinator(commandRegistry = commandRegistry)
                Log.d(TAG, "ActionCoordinator created with shared CommandRegistry")

                // Start collecting speech results
                collectSpeechResults()

            } catch (e: Exception) {
                Log.e(TAG, "Failed to initialize VoiceOSCore", e)
            }
        }
    }

    /**
     * Collect speech results from VoiceOSCore and process commands.
     */
    private fun collectSpeechResults() {
        serviceScope.launch {
            voiceOSCore?.speechResults?.collect { speechResult ->
                Log.d(TAG, "Speech result: '${speechResult.text}' (confidence: ${speechResult.confidence}, final: ${speechResult.isFinal})")

                if (speechResult.isFinal) {
                    // Process the voice command
                    processVoiceCommand(speechResult.text, speechResult.confidence)
                }
            }
        }
    }

    // =========================================================================
    // Lifecycle
    // =========================================================================

    override fun onDestroy() {
        Log.i(TAG, "Service destroying")
        instance = null

        serviceScope.launch {
            try {
                voiceOSCore?.dispose()
                Log.d(TAG, "VoiceOSCore disposed")
            } catch (e: Exception) {
                Log.e(TAG, "Error disposing VoiceOSCore", e)
            }
        }

        serviceScope.cancel()
        super.onDestroy()
    }

    // =========================================================================
    // Public API
    // =========================================================================

    /**
     * Start voice listening.
     */
    fun startListening() {
        serviceScope.launch {
            voiceOSCore?.startListening()
        }
    }

    /**
     * Stop voice listening.
     */
    fun stopListening() {
        serviceScope.launch {
            voiceOSCore?.stopListening()
        }
    }

    /**
     * Get the VoiceOSCore instance.
     */
    fun getVoiceOSCore(): VoiceOSCore? = voiceOSCore

    /**
     * Get the shared command registry.
     */
    fun getCommandRegistry(): CommandRegistry = commandRegistry
}
