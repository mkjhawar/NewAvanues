/**
 * CommandDispatcher.kt - Centralized voice command dispatch and execution
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Code-Reviewed-By: CCA (IDEACODE v18)
 * Created: 2026-01-15
 *
 * Extracts command dispatch logic from VoiceOSService to follow Single Responsibility Principle.
 * Handles voice command routing through tiered execution system.
 *
 * P2-8e: Part of SOLID refactoring - CommandDispatcher extracts ~300 lines from VoiceOSService
 */

package com.augmentalis.voiceoscore.accessibility.managers

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.util.Log
import android.view.accessibility.AccessibilityNodeInfo
import com.augmentalis.commandmanager.CommandManager
import com.augmentalis.voiceos.command.Command
import com.augmentalis.voiceos.command.CommandContext
import com.augmentalis.voiceos.command.CommandSource
import com.augmentalis.voiceoscore.accessibility.extractors.UIScrapingEngine.UIElement
import com.augmentalis.voiceoscore.learnapp.commands.RenameCommandHandler
import com.augmentalis.voiceoscore.learnapp.commands.RenameResult
import com.augmentalis.voiceoscore.scraping.VoiceCommandProcessor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Command Dispatcher
 *
 * Centralizes voice command dispatch for VoiceOSService:
 * - Tiered command execution (CommandManager -> VoiceCommandProcessor -> ActionCoordinator)
 * - Rename command handling ("rename X to Y")
 * - Web command routing (browser detection)
 * - Command context creation
 * - Fallback mode management
 *
 * COMMAND TIERS:
 * 1. RENAME TIER: "rename X to Y" commands (processed first)
 * 2. WEB TIER: Browser-specific commands
 * 3. TIER 1: CommandManager (primary)
 * 4. TIER 2: VoiceCommandProcessor (database lookup)
 * 5. TIER 3: ActionCoordinator (legacy fallback)
 *
 * @param context Application context
 * @param accessibilityService Parent service for accessibility actions
 * @param serviceScope Coroutine scope for async operations
 * @param actionCoordinator Legacy action handler
 * @param integrationCoordinator Integration manager for web commands
 * @param rootNodeProvider Supplier for current root AccessibilityNodeInfo
 */
class CommandDispatcher(
    private val context: Context,
    private val accessibilityService: AccessibilityService,
    private val serviceScope: CoroutineScope,
    private val actionCoordinator: ActionCoordinator,
    private val integrationCoordinator: IntegrationCoordinator,
    private val rootNodeProvider: () -> AccessibilityNodeInfo?,
    private val commandCacheProvider: () -> List<String>,
    private val nodeCacheProvider: () -> List<UIElement>
) {

    companion object {
        private const val TAG = "CommandDispatcher"

        // Minimum confidence threshold
        private const val MIN_CONFIDENCE = 0.5f
    }

    // CommandManager instance
    private var commandManagerInstance: CommandManager? = null

    // VoiceCommandProcessor instance
    private var voiceCommandProcessor: VoiceCommandProcessor? = null

    // RenameCommandHandler (initialized on-demand)
    private var renameCommandHandler: RenameCommandHandler? = null

    // Fallback mode flag
    private val fallbackModeEnabled = AtomicBoolean(false)

    /**
     * Initialize CommandManager
     */
    fun initializeCommandManager() {
        try {
            Log.i(TAG, "Initializing CommandManager...")
            commandManagerInstance = CommandManager.getInstance(context)
            commandManagerInstance?.initialize()
            Log.i(TAG, "CommandManager initialized successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize CommandManager", e)
            commandManagerInstance = null
        }
    }

    /**
     * Initialize VoiceCommandProcessor
     */
    fun initializeVoiceCommandProcessor() {
        try {
            Log.i(TAG, "Initializing VoiceCommandProcessor...")
            voiceCommandProcessor = VoiceCommandProcessor(
                context = context,
                accessibilityService = accessibilityService
            )
            Log.i(TAG, "VoiceCommandProcessor initialized successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize VoiceCommandProcessor", e)
            voiceCommandProcessor = null
        }
    }

    /**
     * Get CommandManager instance
     */
    fun getCommandManager(): CommandManager? = commandManagerInstance

    /**
     * Enable fallback mode (skip CommandManager)
     */
    fun enableFallbackMode() {
        fallbackModeEnabled.set(true)
        Log.w(TAG, "Fallback mode enabled - using basic command handling only")
    }

    /**
     * Disable fallback mode
     */
    fun disableFallbackMode() {
        fallbackModeEnabled.set(false)
        Log.i(TAG, "Fallback mode disabled - using full command handling")
    }

    /**
     * Check if in fallback mode
     */
    fun isFallbackModeEnabled(): Boolean = fallbackModeEnabled.get()

    /**
     * Dispatch voice command through tiered execution
     *
     * @param command Voice command text
     * @param confidence Recognition confidence (0.0-1.0)
     */
    fun dispatchCommand(command: String, confidence: Float) {
        Log.d(TAG, "dispatchCommand: command='$command', confidence=$confidence")

        // Reject very low confidence
        if (confidence < MIN_CONFIDENCE) {
            Log.d(TAG, "Command rejected: confidence too low ($confidence)")
            return
        }

        val normalizedCommand = command.lowercase().trim()
        val currentPackage = extractCurrentPackage()

        // RENAME TIER: Check if this is a rename command (BEFORE other tiers)
        if (isRenameCommand(normalizedCommand)) {
            serviceScope.launch {
                try {
                    Log.i(TAG, "Rename command detected: '$normalizedCommand'")
                    val handled = handleRenameCommand(normalizedCommand, currentPackage)

                    if (handled) {
                        Log.i(TAG, "Rename command executed successfully")
                    } else {
                        Log.w(TAG, "Rename command failed")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error processing rename command: ${e.message}", e)
                }
            }
            return // Rename commands don't fall through
        }

        // WEB TIER: Check if this is a web command (BEFORE other tiers)
        if (currentPackage != null && integrationCoordinator.isCurrentAppBrowser(currentPackage)) {
            serviceScope.launch {
                try {
                    Log.d(TAG, "Browser detected, trying web command...")
                    val handled = integrationCoordinator.processWebCommand(normalizedCommand, currentPackage)

                    if (handled) {
                        Log.i(TAG, "Web command executed successfully: '$normalizedCommand'")
                        return@launch
                    } else {
                        Log.d(TAG, "Not a web command, continuing to regular tiers...")
                        handleRegularCommand(normalizedCommand, confidence)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error processing web command: ${e.message}", e)
                    handleRegularCommand(normalizedCommand, confidence)
                }
            }
            return // Return to prevent dual execution
        }

        // Not a browser, handle as regular command through tier system
        handleRegularCommand(normalizedCommand, confidence)
    }

    /**
     * Handle regular (non-rename, non-web) commands through tier system
     */
    private fun handleRegularCommand(normalizedCommand: String, confidence: Float) {
        // TIER 1: CommandManager (PRIMARY)
        if (!fallbackModeEnabled.get() && commandManagerInstance != null) {
            serviceScope.launch {
                val manager = commandManagerInstance ?: return@launch

                try {
                    Log.d(TAG, "Attempting Tier 1: CommandManager")

                    val cmd = Command(
                        id = normalizedCommand,
                        text = normalizedCommand,
                        source = CommandSource.VOICE,
                        context = createCommandContext(),
                        confidence = confidence,
                        timestamp = System.currentTimeMillis()
                    )

                    val result = manager.executeCommand(cmd)

                    if (result.success) {
                        Log.i(TAG, "Tier 1 (CommandManager) SUCCESS: '$normalizedCommand'")
                        return@launch
                    } else {
                        Log.w(TAG, "Tier 1 (CommandManager) FAILED: ${result.error?.message}")
                        Log.d(TAG, "Falling through to Tier 2...")
                        executeTier2Command(normalizedCommand)
                    }

                } catch (e: Exception) {
                    Log.e(TAG, "Tier 1 (CommandManager) ERROR: ${e.message}", e)
                    Log.d(TAG, "Falling through to Tier 2...")
                    executeTier2Command(normalizedCommand)
                }
            }
        } else {
            // CommandManager unavailable or in fallback mode
            if (fallbackModeEnabled.get()) {
                Log.w(TAG, "Fallback mode active - skipping CommandManager")
            } else {
                Log.w(TAG, "CommandManager not available - using fallback path")
            }

            serviceScope.launch {
                executeTier2Command(normalizedCommand)
            }
        }
    }

    /**
     * Execute Tier 2: VoiceCommandProcessor (secondary)
     */
    private suspend fun executeTier2Command(normalizedCommand: String) {
        try {
            Log.d(TAG, "Attempting Tier 2: VoiceCommandProcessor")

            voiceCommandProcessor?.let { processor ->
                val result = processor.processCommand(normalizedCommand)

                if (result.success) {
                    Log.i(TAG, "Tier 2 (VoiceCommandProcessor) SUCCESS: '$normalizedCommand'")
                    return
                } else {
                    Log.w(TAG, "Tier 2 (VoiceCommandProcessor) FAILED: ${result.message}")
                    Log.d(TAG, "Falling through to Tier 3...")
                }
            } ?: run {
                Log.d(TAG, "VoiceCommandProcessor not available, skipping Tier 2")
            }

            // Fall through to Tier 3
            executeTier3Command(normalizedCommand)

        } catch (e: Exception) {
            Log.e(TAG, "Tier 2 (VoiceCommandProcessor) ERROR: ${e.message}", e)
            Log.d(TAG, "Falling through to Tier 3...")
            executeTier3Command(normalizedCommand)
        }
    }

    /**
     * Execute Tier 3: ActionCoordinator (tertiary/fallback)
     */
    private suspend fun executeTier3Command(normalizedCommand: String) {
        try {
            Log.d(TAG, "Attempting Tier 3: ActionCoordinator (final fallback)")

            val result = actionCoordinator.executeAction(normalizedCommand)

            if (result) {
                Log.i(TAG, "Tier 3 (ActionCoordinator) SUCCESS: '$normalizedCommand'")
            } else {
                Log.w(TAG, "Tier 3 (ActionCoordinator) FAILED: No handler found for '$normalizedCommand'")
                Log.e(TAG, "All tiers failed for command: '$normalizedCommand'")
            }

        } catch (e: Exception) {
            Log.e(TAG, "Tier 3 (ActionCoordinator) ERROR: ${e.message}", e)
            Log.e(TAG, "All tiers failed for command: '$normalizedCommand'")
        }
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
     * Handle rename command
     */
    private suspend fun handleRenameCommand(
        voiceInput: String,
        packageName: String?
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            if (packageName == null) {
                Log.w(TAG, "Cannot process rename: no current package")
                return@withContext false
            }

            // Initialize handler on-demand
            if (renameCommandHandler == null) {
                Log.d(TAG, "Initializing RenameCommandHandler on-demand...")
                renameCommandHandler = RenameCommandHandler(context = context)
                Log.d(TAG, "RenameCommandHandler initialized")
            }

            // Parse rename command
            val renamePattern = "rename\\s+(.+?)\\s+to\\s+(.+)".toRegex(RegexOption.IGNORE_CASE)
            val matchResult = renamePattern.find(voiceInput)

            if (matchResult != null) {
                val (oldName, newName) = matchResult.destructured
                val result = renameCommandHandler?.handleRename(oldName.trim(), newName.trim())
                val success = result is RenameResult.Success

                if (success) {
                    Log.i(TAG, "Rename successful: $oldName -> $newName")
                } else {
                    Log.e(TAG, "Rename failed for: $oldName -> $newName")
                }
                success
            } else {
                Log.w(TAG, "Could not parse rename command from: $voiceInput")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in handleRenameCommand", e)
            false
        }
    }

    /**
     * Create CommandContext from current accessibility service state
     */
    private fun createCommandContext(): CommandContext {
        val root = rootNodeProvider()

        // Extract values before recycling
        val packageName = root?.packageName?.toString()
        val activityName = root?.className?.toString()
        val hasRoot = root != null
        val childCount = root?.childCount ?: 0
        val isAccessibilityFocused = root?.isAccessibilityFocused ?: false

        // Get focused element
        val focusedElement = root?.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)?.let { focusNode ->
            val className = focusNode.className?.toString()
            recycleNodeTree(focusNode)
            className
        }

        // Recycle root node
        recycleNodeTree(root)

        return CommandContext(
            packageName = packageName,
            activityName = activityName,
            focusedElement = focusedElement,
            deviceState = mapOf(
                "hasRoot" to hasRoot,
                "childCount" to childCount,
                "isAccessibilityFocused" to isAccessibilityFocused,
                "androidContext" to (context as android.content.Context),
                "accessibilityService" to accessibilityService
            ),
            customData = mapOf(
                "commandCacheSize" to commandCacheProvider().size,
                "nodeCacheSize" to nodeCacheProvider().size,
                "fallbackMode" to fallbackModeEnabled.get()
            )
        )
    }

    /**
     * Extract current package name from root node
     */
    private fun extractCurrentPackage(): String? {
        val root = rootNodeProvider()
        val packageName = root?.packageName?.toString()
        recycleNodeTree(root)
        return packageName
    }

    /**
     * Recursively recycle AccessibilityNodeInfo and all descendants
     */
    private fun recycleNodeTree(node: AccessibilityNodeInfo?) {
        if (node == null) return

        try {
            for (i in 0 until node.childCount) {
                try {
                    val child = node.getChild(i)
                    recycleNodeTree(child)
                } catch (e: Exception) {
                    Log.w(TAG, "Error recycling child node: ${e.message}")
                }
            }

            if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                @Suppress("DEPRECATION")
                node.recycle()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error in recycleNodeTree: ${e.message}")
        }
    }

    /**
     * Cleanup resources
     */
    fun cleanup() {
        Log.d(TAG, "Cleaning up CommandDispatcher...")

        commandManagerInstance?.cleanup()
        commandManagerInstance = null

        voiceCommandProcessor = null
        renameCommandHandler = null

        fallbackModeEnabled.set(false)

        Log.i(TAG, "CommandDispatcher cleanup complete")
    }
}
