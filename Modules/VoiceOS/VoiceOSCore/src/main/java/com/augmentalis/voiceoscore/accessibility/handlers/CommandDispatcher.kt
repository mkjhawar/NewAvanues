/**
 * CommandDispatcher.kt - Command routing and execution
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Code-Reviewed-By: CCA
 * Created: 2025-12-17
 *
 * Responsibility: Routes and dispatches voice commands through tier system (CommandManager → VoiceCommandProcessor → ActionCoordinator)
 */
package com.augmentalis.voiceoscore.accessibility.handlers

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.util.Log
import android.view.accessibility.AccessibilityNodeInfo
import com.augmentalis.commandmanager.CommandManager
import com.augmentalis.voiceos.command.Command
import com.augmentalis.voiceos.command.CommandContext
import com.augmentalis.voiceos.command.CommandSource
import com.augmentalis.voiceoscore.accessibility.managers.ActionCoordinator
import com.augmentalis.voiceoscore.scraping.VoiceCommandProcessor
import com.augmentalis.voiceoscore.web.WebCommandCoordinator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

/**
 * Dispatches voice commands through multi-tier execution system
 *
 * Tier 1: CommandManager (PRIMARY) - Unified command system
 * Web Tier: WebCommandCoordinator - Browser-specific commands
 * Rename Tier: RenameCommandHandler - On-demand command renaming
 * Tier 2: VoiceCommandProcessor (SECONDARY) - Hash-based app commands
 * Tier 3: ActionCoordinator (FALLBACK) - Legacy handler-based commands
 */
class CommandDispatcher(
    private val context: Context,
    private val accessibilityService: AccessibilityService,
    private val actionCoordinator: ActionCoordinator,
    private val webCommandCoordinator: WebCommandCoordinator,
    private val onRenameCommand: suspend (String, String?) -> Boolean
) {
    companion object {
        private const val TAG = "CommandDispatcher"

        // FIX C5-P1-6, C5-P1-7 (2025-12-22): Add timeout constants to prevent indefinite hangs
        private const val TIER_EXECUTION_TIMEOUT_MS = 5000L  // 5 seconds per tier
        private const val COMMAND_TOTAL_TIMEOUT_MS = 15000L   // 15 seconds total (3 tiers max)
    }

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    // Tier 1: CommandManager (PRIMARY)
    private var commandManagerInstance: CommandManager? = null

    // Tier 2: VoiceCommandProcessor (SECONDARY)
    private var voiceCommandProcessor: VoiceCommandProcessor? = null

    // Fallback mode flag
    private var fallbackModeEnabled = false

    /**
     * Set CommandManager for Tier 1 execution
     */
    fun setCommandManager(manager: CommandManager?) {
        commandManagerInstance = manager
        Log.d(TAG, "CommandManager ${if (manager != null) "set" else "cleared"}")
    }

    /**
     * Set VoiceCommandProcessor for Tier 2 execution
     */
    fun setVoiceCommandProcessor(processor: VoiceCommandProcessor?) {
        voiceCommandProcessor = processor
        Log.d(TAG, "VoiceCommandProcessor ${if (processor != null) "set" else "cleared"}")
    }

    /**
     * Process voice command with confidence score
     */
    fun processVoiceCommand(command: String, confidence: Float) {
        Log.d(TAG, "processVoiceCommand: command='$command', confidence=$confidence")

        // Reject very low confidence (< 0.5)
        if (confidence < 0.5f) {
            Log.d(TAG, "Command rejected: confidence too low ($confidence)")
            return
        }

        val normalizedCommand = command.lowercase().trim()
        val currentPackage = accessibilityService.rootInActiveWindow?.packageName?.toString()

        // RENAME TIER: Check if this is a rename command (BEFORE other tiers)
        if (isRenameCommand(normalizedCommand)) {
            scope.launch {
                try {
                    Log.i(TAG, "Rename command detected: '$normalizedCommand'")
                    val handled = onRenameCommand(normalizedCommand, currentPackage)

                    if (handled) {
                        Log.i(TAG, "✓ Rename command executed successfully")
                        return@launch
                    } else {
                        Log.w(TAG, "Rename command failed, not continuing to regular tiers")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error processing rename command: ${e.message}", e)
                }
            }
            return
        }

        // WEB TIER: Check if this is a web command (BEFORE other tiers)
        if (currentPackage != null && webCommandCoordinator.isCurrentAppBrowser(currentPackage)) {
            scope.launch {
                try {
                    Log.d(TAG, "Browser detected, trying web command...")
                    val handled = webCommandCoordinator.processWebCommand(normalizedCommand, currentPackage)

                    if (handled) {
                        Log.i(TAG, "✓ Web command executed successfully: '$normalizedCommand'")
                        return@launch
                    } else {
                        Log.d(TAG, "Not a web command or no match found, continuing to regular tiers...")
                        handleRegularCommand(normalizedCommand, confidence)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error processing web command: ${e.message}", e)
                    handleRegularCommand(normalizedCommand, confidence)
                }
            }
            return
        }

        // Not a browser, handle as regular command through tier system
        handleRegularCommand(normalizedCommand, confidence)
    }

    /**
     * Check if voice input is a rename command
     */
    private fun isRenameCommand(voiceInput: String): Boolean {
        val patterns = listOf(
            Regex("rename .+ to .+"),
            Regex("rename .+ as .+"),
            Regex("change .+ to .+")
        )
        return patterns.any { it.matches(voiceInput) }
    }

    /**
     * Handle regular (non-web, non-rename) commands through tier system
     */
    private fun handleRegularCommand(normalizedCommand: String, confidence: Float) {
        // TIER 1: CommandManager (PRIMARY)
        if (!fallbackModeEnabled && commandManagerInstance != null) {
            scope.launch {
                val manager = commandManagerInstance ?: return@launch

                try {
                    Log.d(TAG, "Attempting Tier 1: CommandManager")

                    // Create Command object with full context
                    val cmd = Command(
                        id = normalizedCommand,
                        text = normalizedCommand,
                        source = CommandSource.VOICE,
                        context = createCommandContext(),
                        confidence = confidence,
                        timestamp = System.currentTimeMillis()
                    )

                    // Execute via CommandManager
                    val result = manager.executeCommand(cmd)

                    if (result.success) {
                        Log.i(TAG, "✓ Tier 1 (CommandManager) SUCCESS: '$normalizedCommand'")
                        return@launch
                    } else {
                        Log.w(TAG, "Tier 1 (CommandManager) FAILED: ${result.error?.message}")
                        Log.d(TAG, "  Falling through to Tier 2...")
                        executeTier2Command(normalizedCommand)
                    }

                } catch (e: Exception) {
                    Log.e(TAG, "Tier 1 (CommandManager) ERROR: ${e.message}", e)
                    Log.d(TAG, "  Falling through to Tier 2...")
                    executeTier2Command(normalizedCommand)
                }
            }
        } else {
            // CommandManager unavailable or in fallback mode
            if (fallbackModeEnabled) {
                Log.w(TAG, "Fallback mode active - skipping CommandManager")
            } else {
                Log.w(TAG, "CommandManager not available - using fallback path")
            }

            scope.launch {
                executeTier2Command(normalizedCommand)
            }
        }
    }

    /**
     * Create CommandContext from current accessibility service state
     */
    private fun createCommandContext(): CommandContext {
        val root = accessibilityService.rootInActiveWindow

        return CommandContext(
            packageName = root?.packageName?.toString(),
            activityName = root?.className?.toString(),
            focusedElement = root?.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)?.className?.toString(),
            deviceState = mapOf(
                "hasRoot" to (root != null),
                "childCount" to (root?.childCount ?: 0),
                "isAccessibilityFocused" to (root?.isAccessibilityFocused ?: false),
                "androidContext" to context,
                "accessibilityService" to accessibilityService
            ),
            customData = mapOf(
                "fallbackMode" to fallbackModeEnabled
            )
        )
    }

    /**
     * Execute Tier 2: VoiceCommandProcessor (secondary)
     *
     * FIX C5-P1-6, C5-P1-7 (2025-12-22): Added timeout wrapper to prevent indefinite hangs
     */
    private suspend fun executeTier2Command(normalizedCommand: String) {
        try {
            Log.d(TAG, "Attempting Tier 2: VoiceCommandProcessor")

            // Wrap in timeout to prevent indefinite hangs
            val timedOut = try {
                withTimeout(TIER_EXECUTION_TIMEOUT_MS) {
                    voiceCommandProcessor?.let { processor ->
                        val result = processor.processCommand(normalizedCommand)

                        if (result.success) {
                            Log.i(TAG, "✓ Tier 2 (VoiceCommandProcessor) SUCCESS: '$normalizedCommand'")
                            return@withTimeout false  // Not timed out, success
                        } else {
                            Log.w(TAG, "Tier 2 (VoiceCommandProcessor) FAILED: ${result.message}")
                            Log.d(TAG, "  Falling through to Tier 3...")
                        }
                    } ?: run {
                        Log.d(TAG, "VoiceCommandProcessor not available, skipping Tier 2")
                    }
                    true  // Continue to next tier
                }
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "Tier 2 (VoiceCommandProcessor) TIMEOUT after ${TIER_EXECUTION_TIMEOUT_MS}ms")
                true  // Continue to next tier
            }

            if (timedOut) {
                executeTier3Command(normalizedCommand)
            }

        } catch (e: Exception) {
            Log.e(TAG, "Tier 2 (VoiceCommandProcessor) ERROR: ${e.message}", e)
            Log.d(TAG, "  Falling through to Tier 3...")
            executeTier3Command(normalizedCommand)
        }
    }

    /**
     * Execute Tier 3: ActionCoordinator (tertiary/fallback)
     *
     * FIX C5-P1-6, C5-P1-7 (2025-12-22): Added timeout wrapper to prevent indefinite hangs
     */
    private suspend fun executeTier3Command(normalizedCommand: String) {
        try {
            Log.d(TAG, "Attempting Tier 3: ActionCoordinator (final fallback)")

            // Wrap in timeout to prevent indefinite hangs
            try {
                withTimeout(TIER_EXECUTION_TIMEOUT_MS) {
                    val result = actionCoordinator.executeAction(normalizedCommand)

                    if (result) {
                        Log.i(TAG, "✓ Tier 3 (ActionCoordinator) SUCCESS: '$normalizedCommand'")
                    } else {
                        Log.w(TAG, "✗ Tier 3 (ActionCoordinator) FAILED: No handler found for '$normalizedCommand'")
                        Log.e(TAG, "✗ All tiers failed for command: '$normalizedCommand'")
                    }
                }
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "Tier 3 (ActionCoordinator) TIMEOUT after ${TIER_EXECUTION_TIMEOUT_MS}ms")
                Log.e(TAG, "✗ All tiers failed for command: '$normalizedCommand'")
            }

        } catch (e: Exception) {
            Log.e(TAG, "Tier 3 (ActionCoordinator) ERROR: ${e.message}", e)
            Log.e(TAG, "✗ All tiers failed for command: '$normalizedCommand'")
        }
    }

    /**
     * Enable fallback mode when CommandManager is unavailable
     */
    fun enableFallbackMode() {
        fallbackModeEnabled = true
        Log.w(TAG, "Fallback mode enabled - using basic command handling only")
    }

    /**
     * Disable fallback mode
     */
    fun disableFallbackMode() {
        fallbackModeEnabled = false
        Log.i(TAG, "Fallback mode disabled")
    }

    /**
     * Cleanup resources
     */
    fun cleanup() {
        try {
            Log.d(TAG, "Cleaning up CommandDispatcher...")
            scope.cancel()
            commandManagerInstance = null
            voiceCommandProcessor = null
            Log.i(TAG, "CommandDispatcher cleaned up successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error cleaning up CommandDispatcher", e)
        }
    }
}
