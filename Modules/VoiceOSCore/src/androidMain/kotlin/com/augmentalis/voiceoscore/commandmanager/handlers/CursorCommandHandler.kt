/**
 * CursorCommandHandler.kt
 *
 * Created: 2025-01-26 02:30 PST
 * Last Modified: 2025-10-10 18:01 PDT
 * Author: VOS4 Development Team
 * Version: 3.0.0
 *
 * Purpose: Voice command routing for VoiceCursor - delegates to CursorActions
 * Features: Command registration, routing, and processing
 * Location: CommandManager module (moved from VoiceCursor module)
 *
 * Changelog:
 * - v3.0.0 (2025-10-10): Moved to CommandManager, refactored to delegate to CursorActions
 * - v2.0.0 (2025-01-27): Merged VoiceAccessibilityIntegration functionality to reduce overhead
 * - v1.0.0 (2025-01-26 02:30 PST): Initial voice command integration
 */

package com.augmentalis.voiceoscore.commandmanager.handlers

import android.content.Context
import android.util.Log
import com.augmentalis.voiceoscore.commandmanager.CommandHandler
import com.augmentalis.voiceoscore.commandmanager.CommandRegistry
import com.augmentalis.voiceoscore.commandmanager.actions.CursorActions
import com.augmentalis.voiceoscore.commandmanager.actions.CursorDirection
import com.augmentalis.voicecursor.core.CursorType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Voice command handler for VoiceCursor
 * Routes commands to CursorActions which uses CursorController directly
 *
 * Design:
 * - Command parsing and routing only (no cursor logic)
 * - Delegates all execution to CursorActions
 * - CursorActions uses CursorController + AndroidGestureDispatcher directly
 * - Clean separation: CommandManager -> CursorActions -> CursorController
 * - Implements CommandHandler for CommandRegistry integration
 */
class CursorCommandHandler private constructor(
    private val context: Context
) : CommandHandler {
    companion object {
        private const val TAG = "CursorCommandHandler"
        private const val MODULE_ID = "voice_cursor"

        @Volatile
        private var instance: CursorCommandHandler? = null

        /**
         * Get singleton instance
         */
        fun getInstance(context: Context): CursorCommandHandler {
            return instance ?: synchronized(this) {
                instance ?: CursorCommandHandler(context.applicationContext).also {
                    instance = it
                }
            }
        }

        // Command prefixes for voice recognition
        private const val CURSOR_PREFIX = "cursor"
        private const val VOICE_CURSOR_PREFIX = "voice cursor"

        // Supported command patterns (unused - for reference only)
        private val MOVEMENT_COMMANDS = setOf("up", "down", "left", "right")
        private val ACTION_COMMANDS = setOf("click", "double click", "long press", "menu")
        private val SYSTEM_COMMANDS = setOf("center", "show", "hide", "settings", "coordinates")
        private val TYPE_COMMANDS = setOf("hand", "normal", "custom")
    }

    // CommandHandler interface implementation
    override val moduleId: String = "voicecursor"

    override val supportedCommands: List<String> = listOf(
        // Movement commands
        "cursor up [distance]",
        "cursor down [distance]",
        "cursor left [distance]",
        "cursor right [distance]",

        // Action commands
        "cursor click",
        "cursor double click",
        "cursor long press",
        "cursor menu",

        // System commands
        "cursor center",
        "cursor show",
        "cursor hide",
        "cursor settings",

        // Type commands
        "cursor hand",
        "cursor normal",
        "cursor custom",

        // Voice cursor system
        "voice cursor enable",
        "voice cursor disable",
        "voice cursor calibrate",
        "voice cursor settings",
        "voice cursor help",

        // Standalone commands
        "click",
        "click here",
        "double click",
        "long press",
        "center cursor",
        "show cursor",
        "hide cursor",
        "show coordinates",
        "hide coordinates",
        "toggle coordinates"
    )

    private val commandScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var commandRouter: CommandRouter? = null

    // Integration state
    private var isInitialized = false
    private var isRegistered = false

    init {
        initialize()
        // Register with CommandRegistry automatically
        CommandRegistry.registerHandler(moduleId, this)
    }

    /**
     * Initialize command handler
     */
    fun initialize(): Boolean {
        if (isInitialized) {
            Log.w(TAG, "Already initialized")
            return true
        }

        return try {
            commandRouter = SimpleCommandRouter()

            isInitialized = true
            Log.d(TAG, "CursorCommandHandler initialized")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize", e)
            false
        }
    }
    
    /**
     * Register commands with VOS4 system
     */
    fun registerCommands(): Boolean {
        if (!isInitialized) {
            Log.e(TAG, "Not initialized")
            return false
        }
        
        if (isRegistered) {
            Log.w(TAG, "Commands already registered")
            return true
        }
        
        return try {
            // Register with command router
            commandScope.launch {
                commandRouter?.registerCommandHandler(MODULE_ID) { command ->
                    handleVoiceCommand(command)
                }
            }
            
            isRegistered = true
            val commands = supportedCommands
            Log.d(TAG, "Registered ${commands.size} voice commands")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to register commands", e)
            false
        }
    }
    
    /**
     * Unregister commands from system
     */
    fun unregisterCommands() {
        if (!isRegistered) return
        
        try {
            commandScope.launch {
                commandRouter?.unregisterCommandHandler(MODULE_ID)
            }
            isRegistered = false
            Log.d(TAG, "Commands unregistered")
        } catch (e: Exception) {
            Log.e(TAG, "Error unregistering commands", e)
        }
    }
    
    /**
     * CommandHandler interface: Check if this handler can process the command
     * (command is already normalized by CommandRegistry)
     */
    override fun canHandle(command: String): Boolean {
        return when {
            command.startsWith(CURSOR_PREFIX) -> true
            command.startsWith(VOICE_CURSOR_PREFIX) -> true
            isStandaloneCursorCommand(command) -> true
            else -> false
        }
    }

    /**
     * CommandHandler interface: Execute the command
     * (command is already normalized by CommandRegistry)
     */
    override suspend fun handleCommand(command: String): Boolean {
        return handleVoiceCommand(command)
    }

    /**
     * Check if VoiceCursor can handle the given command (legacy method)
     * @deprecated Use canHandle() from CommandHandler interface
     */
    @Deprecated("Use canHandle() instead", ReplaceWith("canHandle(commandText)"))
    fun canHandleCommand(commandText: String): Boolean {
        val normalizedCommand = commandText.lowercase().trim()
        return canHandle(normalizedCommand)
    }
    
    /**
     * Process incoming voice command
     * @param commandText The recognized voice command text
     * @return true if command was handled, false otherwise
     */
    suspend fun handleVoiceCommand(commandText: String): Boolean {
        if (!isInitialized) {
            Log.w(TAG, "Not initialized for command processing")
            return false
        }
        
        val normalizedCommand = commandText.lowercase().trim()
        
        Log.d(TAG, "Processing voice command: '$commandText'")
        
        return try {
            when {
                // Direct cursor commands
                normalizedCommand.startsWith(CURSOR_PREFIX) -> {
                    processCursorCommand(normalizedCommand)
                }
                
                // Voice cursor system commands
                normalizedCommand.startsWith(VOICE_CURSOR_PREFIX) -> {
                    processVoiceCursorCommand(normalizedCommand)
                }
                
                // Standalone cursor action commands
                isStandaloneCursorCommand(normalizedCommand) -> {
                    processStandaloneCommand(normalizedCommand)
                }
                
                else -> false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error processing command: $commandText", e)
            false
        }
    }
    
    /**
     * Process cursor-prefixed commands (e.g., "cursor up", "cursor click")
     * Delegates to CursorActions for execution
     */
    private suspend fun processCursorCommand(command: String): Boolean {
        val parts = command.split(" ", limit = 3)
        if (parts.size < 2) return false

        val action = parts[1]
        val parameter = if (parts.size > 2) parts[2] else null

        return when (action) {
            // Movement commands - delegate to CursorActions
            "up" -> {
                val distance = parameter?.toFloatOrNull() ?: 50f
                CursorActions.moveCursor(CursorDirection.UP, distance)
            }
            "down" -> {
                val distance = parameter?.toFloatOrNull() ?: 50f
                CursorActions.moveCursor(CursorDirection.DOWN, distance)
            }
            "left" -> {
                val distance = parameter?.toFloatOrNull() ?: 50f
                CursorActions.moveCursor(CursorDirection.LEFT, distance)
            }
            "right" -> {
                val distance = parameter?.toFloatOrNull() ?: 50f
                CursorActions.moveCursor(CursorDirection.RIGHT, distance)
            }

            // Click actions - delegate to CursorActions
            "click" -> CursorActions.click()
            "double" -> {
                if (parameter == "click") {
                    CursorActions.doubleClick()
                } else false
            }
            "long" -> {
                if (parameter == "press" || parameter == "click") {
                    CursorActions.longPress()
                } else false
            }

            // System commands - delegate to CursorActions
            "center" -> CursorActions.centerCursor()
            "show" -> {
                if (parameter == "coordinates") {
                    CursorActions.showCoordinates()
                } else {
                    CursorActions.showCursor()
                }
            }
            "hide" -> {
                if (parameter == "coordinates") {
                    CursorActions.hideCoordinates()
                } else {
                    CursorActions.hideCursor()
                }
            }
            "coordinates" -> CursorActions.toggleCoordinates()
            "menu" -> CursorActions.showMenu()
            "settings" -> CursorActions.openSettings(context)

            // Cursor type commands - delegate to CursorActions
            "hand" -> CursorActions.setCursorType(CursorType.Hand)
            "normal" -> CursorActions.setCursorType(CursorType.Normal)
            "custom" -> CursorActions.setCursorType(CursorType.Custom)

            else -> false
        }
    }
    
    /**
     * Process voice cursor system commands (e.g., "voice cursor enable")
     * Delegates to CursorActions for execution
     */
    private suspend fun processVoiceCursorCommand(command: String): Boolean {
        val parts = command.split(" ")
        if (parts.size < 3) return false

        val action = parts[2]

        return when (action) {
            "enable", "start", "on" -> CursorActions.showCursor() // Enable = show cursor
            "disable", "stop", "off" -> CursorActions.hideCursor() // Disable = hide cursor
            "calibrate" -> CursorActions.calibrate()
            "settings" -> CursorActions.openSettings(context)
            "help" -> {
                // Help functionality - could show help overlay
                Log.d(TAG, "Voice cursor help requested")
                false // Not implemented yet
            }
            else -> false
        }
    }

    /**
     * Process standalone cursor commands (e.g., "click here", "center")
     * Delegates to CursorActions for execution
     */
    private suspend fun processStandaloneCommand(command: String): Boolean {
        return when (command) {
            "click", "click here" -> CursorActions.click()
            "double click" -> CursorActions.doubleClick()
            "long press", "long click" -> CursorActions.longPress()
            "center cursor", "center" -> CursorActions.centerCursor()
            "show cursor" -> CursorActions.showCursor()
            "hide cursor" -> CursorActions.hideCursor()
            "show coordinates" -> CursorActions.showCoordinates()
            "hide coordinates" -> CursorActions.hideCoordinates()
            "toggle coordinates" -> CursorActions.toggleCoordinates()
            else -> false
        }
    }
    
    /**
     * Check if command is a standalone cursor command
     */
    private fun isStandaloneCursorCommand(command: String): Boolean {
        return command in setOf(
            "click", "click here", "double click", "long press", "long click",
            "center cursor", "center", "show cursor", "hide cursor",
            "show coordinates", "hide coordinates", "toggle coordinates"
        )
    }
    
    /**
     * Get list of supported voice commands (legacy method)
     * Removed due to platform declaration clash with supportedCommands property getter
     * Use supportedCommands property directly instead
     */
    
    /**
     * Check if ready for operation
     */
    fun isReady(): Boolean = isInitialized && isRegistered
    
    /**
     * Get integration status
     */
    fun getStatus(): IntegrationStatus {
        return IntegrationStatus(
            isInitialized = isInitialized,
            isRegistered = isRegistered,
            commandsSupported = if (isInitialized) supportedCommands.size else 0
        )
    }
    
    /**
     * Cleanup resources
     */
    fun dispose() {
        unregisterCommands()
        // Unregister from CommandRegistry
        CommandRegistry.unregisterHandler(moduleId)
        commandScope.cancel()
        instance = null
        Log.d(TAG, "VoiceCursor command integration disposed")
    }
}

/**
 * Command router interface
 */
interface CommandRouter {
    suspend fun registerCommandHandler(moduleId: String, handler: suspend (String) -> Boolean)
    suspend fun unregisterCommandHandler(moduleId: String)
}

/**
 * Simple command router implementation
 */
private class SimpleCommandRouter : CommandRouter {
    private val handlers = mutableMapOf<String, suspend (String) -> Boolean>()
    
    override suspend fun registerCommandHandler(moduleId: String, handler: suspend (String) -> Boolean) {
        handlers[moduleId] = handler
        Log.d("CommandRouter", "Registered handler for: $moduleId")
    }
    
    override suspend fun unregisterCommandHandler(moduleId: String) {
        handlers.remove(moduleId)
        Log.d("CommandRouter", "Unregistered handler for: $moduleId")
    }
}

/**
 * Integration status
 */
data class IntegrationStatus(
    val isInitialized: Boolean,
    val isRegistered: Boolean,
    val commandsSupported: Int
)

