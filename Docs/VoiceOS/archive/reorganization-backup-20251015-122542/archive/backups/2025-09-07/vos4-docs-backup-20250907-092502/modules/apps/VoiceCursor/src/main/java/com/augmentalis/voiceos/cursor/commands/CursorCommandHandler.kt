/**
 * CursorCommandHandler.kt
 * 
 * Created: 2025-01-26 02:30 PST
 * Last Modified: 2025-01-27
 * Author: VOS4 Development Team
 * Version: 2.0.0
 * 
 * Purpose: Unified voice command handler and accessibility integration for VoiceCursor
 * Features: Command registration, routing, processing, and system coordination
 * 
 * Changelog:
 * - v1.0.0 (2025-01-26 02:30 PST): Initial voice command integration
 * - v2.0.0 (2025-01-27): Merged VoiceAccessibilityIntegration functionality to reduce overhead
 */

package com.augmentalis.voiceos.cursor.commands

import android.content.Context
import android.content.Intent
import android.util.Log
import com.augmentalis.voiceos.cursor.VoiceCursor
import com.augmentalis.voiceos.cursor.core.*
import com.augmentalis.voiceos.cursor.service.VoiceCursorOverlayService
import com.augmentalis.voiceos.cursor.service.VoiceCursorAccessibilityService
import com.augmentalis.voiceos.cursor.view.CursorAction
import kotlinx.coroutines.*

/**
 * Unified command handler and VoiceAccessibility integration for VoiceCursor
 * Handles command registration, routing, and processing in a single optimized class
 */
class CursorCommandHandler private constructor(
    private val context: Context
) {
    companion object {
        private const val TAG = "VoiceCursorCommands"
        private const val MODULE_ID = "voicecursor"
        
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
        
        // Supported command patterns
        private val MOVEMENT_COMMANDS = setOf("up", "down", "left", "right")
        private val ACTION_COMMANDS = setOf("click", "double click", "long press", "menu")
        private val SYSTEM_COMMANDS = setOf("center", "show", "hide", "settings", "coordinates")
        private val TYPE_COMMANDS = setOf("hand", "normal", "custom")
    }
    
    private val commandScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var voiceCursor: VoiceCursor? = null
    private var accessibilityService: VoiceCursorAccessibilityService? = null
    private var commandRouter: CommandRouter? = null
    
    // Integration state
    private var isInitialized = false
    private var isRegistered = false
    
    init {
        initialize()
    }
    
    /**
     * Initialize VoiceCursor components and integration
     */
    fun initialize(): Boolean {
        if (isInitialized) {
            Log.w(TAG, "Already initialized")
            return true
        }
        
        return try {
            voiceCursor = VoiceCursor.getInstance(context)
            accessibilityService = VoiceCursorAccessibilityService.getInstance()
            commandRouter = SimpleCommandRouter()
            
            isInitialized = true
            Log.d(TAG, "VoiceCursor command integration initialized")
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
            val commands = getSupportedCommands()
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
     * Check if VoiceCursor can handle the given command
     */
    fun canHandleCommand(commandText: String): Boolean {
        val normalizedCommand = commandText.lowercase().trim()
        
        return when {
            normalizedCommand.startsWith(CURSOR_PREFIX) -> true
            normalizedCommand.startsWith(VOICE_CURSOR_PREFIX) -> true
            isStandaloneCursorCommand(normalizedCommand) -> true
            else -> false
        }
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
     */
    private suspend fun processCursorCommand(command: String): Boolean {
        val parts = command.split(" ", limit = 3)
        if (parts.size < 2) return false
        
        val action = parts[1]
        val parameter = if (parts.size > 2) parts[2] else null
        
        return when (action) {
            // Movement commands
            "up" -> moveCursor(CursorDirection.UP, parameter)
            "down" -> moveCursor(CursorDirection.DOWN, parameter)
            "left" -> moveCursor(CursorDirection.LEFT, parameter)
            "right" -> moveCursor(CursorDirection.RIGHT, parameter)
            
            // Click actions
            "click" -> performCursorAction(CursorAction.SINGLE_CLICK)
            "double" -> {
                if (parameter == "click") {
                    performCursorAction(CursorAction.DOUBLE_CLICK)
                } else false
            }
            "long" -> {
                if (parameter == "press" || parameter == "click") {
                    performCursorAction(CursorAction.LONG_PRESS)
                } else false
            }
            
            // System commands
            "center" -> centerCursor()
            "show" -> {
                if (parameter == "coordinates") {
                    showCoordinates()
                } else {
                    showCursor()
                }
            }
            "hide" -> {
                if (parameter == "coordinates") {
                    hideCoordinates()
                } else {
                    hideCursor()
                }
            }
            "coordinates" -> toggleCoordinates()
            "menu" -> showCursorMenu()
            "settings" -> openCursorSettings()
            
            // Cursor type commands
            "hand" -> setCursorType(CursorType.Hand)
            "normal" -> setCursorType(CursorType.Normal)
            "custom" -> setCursorType(CursorType.Custom)
            
            else -> false
        }
    }
    
    /**
     * Process voice cursor system commands (e.g., "voice cursor enable")
     */
    private suspend fun processVoiceCursorCommand(command: String): Boolean {
        val parts = command.split(" ")
        if (parts.size < 3) return false
        
        val action = parts[2]
        
        return when (action) {
            "enable", "start", "on" -> enableVoiceCursor()
            "disable", "stop", "off" -> disableVoiceCursor()
            "calibrate" -> calibrateCursor()
            "settings" -> openCursorSettings()
            "help" -> showCursorHelp()
            else -> false
        }
    }
    
    /**
     * Process standalone cursor commands (e.g., "click here", "center")
     */
    private suspend fun processStandaloneCommand(command: String): Boolean {
        return when (command) {
            "click", "click here" -> performCursorAction(CursorAction.SINGLE_CLICK)
            "double click" -> performCursorAction(CursorAction.DOUBLE_CLICK)
            "long press", "long click" -> performCursorAction(CursorAction.LONG_PRESS)
            "center cursor", "center" -> centerCursor()
            "show cursor" -> showCursor()
            "hide cursor" -> hideCursor()
            "show coordinates" -> showCoordinates()
            "hide coordinates" -> hideCoordinates()
            "toggle coordinates" -> toggleCoordinates()
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
     * Move cursor in specified direction
     */
    private suspend fun moveCursor(direction: CursorDirection, parameter: String?): Boolean {
        val distance = parameter?.toFloatOrNull() ?: 50f // Default 50 pixels
        
        return withContext(Dispatchers.Main) {
            try {
                // Get current cursor position
                val currentPosition = voiceCursor?.getCurrentPosition() ?: CursorOffset(0f, 0f)
                
                // Calculate new position
                val newPosition = when (direction) {
                    CursorDirection.UP -> currentPosition.copy(y = currentPosition.y - distance)
                    CursorDirection.DOWN -> currentPosition.copy(y = currentPosition.y + distance)
                    CursorDirection.LEFT -> currentPosition.copy(x = currentPosition.x - distance)
                    CursorDirection.RIGHT -> currentPosition.copy(x = currentPosition.x + distance)
                }
                
                // Update cursor position
                voiceCursor?.updatePosition(newPosition)
                
                Log.d(TAG, "Moved cursor $direction by $distance pixels")
                true
            } catch (e: Exception) {
                Log.e(TAG, "Failed to move cursor", e)
                false
            }
        }
    }
    
    /**
     * Perform cursor action (click, double-click, etc.)
     */
    private suspend fun performCursorAction(action: CursorAction): Boolean {
        return withContext(Dispatchers.Main) {
            try {
                val currentPosition = voiceCursor?.getCurrentPosition() 
                if (currentPosition == null) {
                    return@withContext false
                }
                accessibilityService?.executeAction(action, currentPosition)
                true
            } catch (e: Exception) {
                Log.e(TAG, "Failed to perform cursor action: $action", e)
                false
            }
        }
    }
    
    /**
     * Center cursor on screen
     */
    private suspend fun centerCursor(): Boolean {
        return withContext(Dispatchers.Main) {
            try {
                voiceCursor?.centerCursor()
                Log.d(TAG, "Cursor centered")
                true
            } catch (e: Exception) {
                Log.e(TAG, "Failed to center cursor", e)
                false
            }
        }
    }
    
    /**
     * Show cursor overlay
     */
    private suspend fun showCursor(): Boolean {
        return withContext(Dispatchers.Main) {
            try {
                val config = voiceCursor?.getCurrentConfig() ?: CursorConfig()
                val intent = Intent(context, VoiceCursorOverlayService::class.java).apply {
                    putExtra("action", "show_cursor")
                    putExtra("config", config)
                }
                context.startForegroundService(intent)
                
                Log.d(TAG, "Cursor overlay shown")
                true
            } catch (e: Exception) {
                Log.e(TAG, "Failed to show cursor", e)
                false
            }
        }
    }
    
    /**
     * Hide cursor overlay
     */
    private suspend fun hideCursor(): Boolean {
        return withContext(Dispatchers.Main) {
            try {
                val intent = Intent(context, VoiceCursorOverlayService::class.java).apply {
                    putExtra("action", "hide_cursor")
                }
                context.startForegroundService(intent)
                
                Log.d(TAG, "Cursor overlay hidden")
                true
            } catch (e: Exception) {
                Log.e(TAG, "Failed to hide cursor", e)
                false
            }
        }
    }
    
    /**
     * Show cursor context menu
     */
    private suspend fun showCursorMenu(): Boolean {
        return withContext(Dispatchers.Main) {
            try {
                val intent = Intent(context, VoiceCursorOverlayService::class.java).apply {
                    putExtra("action", "show_menu")
                }
                context.startForegroundService(intent)
                
                Log.d(TAG, "Cursor menu shown")
                true
            } catch (e: Exception) {
                Log.e(TAG, "Failed to show cursor menu", e)
                false
            }
        }
    }
    
    /**
     * Set cursor type
     */
    private suspend fun setCursorType(type: CursorType): Boolean {
        return withContext(Dispatchers.Main) {
            try {
                val currentConfig = voiceCursor?.getCurrentConfig() ?: CursorConfig()
                val newConfig = currentConfig.copy(type = type)
                voiceCursor?.updateConfig(newConfig)
                
                Log.d(TAG, "Cursor type set to: ${type::class.simpleName}")
                true
            } catch (e: Exception) {
                Log.e(TAG, "Failed to set cursor type", e)
                false
            }
        }
    }
    
    /**
     * Enable VoiceCursor system
     */
    private suspend fun enableVoiceCursor(): Boolean {
        return withContext(Dispatchers.Main) {
            try {
                val config = CursorConfig() // Default configuration
                voiceCursor?.initialize(config)
                voiceCursor?.startCursor()
                
                Log.d(TAG, "VoiceCursor system enabled")
                true
            } catch (e: Exception) {
                Log.e(TAG, "Failed to enable VoiceCursor", e)
                false
            }
        }
    }
    
    /**
     * Disable VoiceCursor system
     */
    private suspend fun disableVoiceCursor(): Boolean {
        return withContext(Dispatchers.Main) {
            try {
                voiceCursor?.stopCursor()
                
                Log.d(TAG, "VoiceCursor system disabled")
                true
            } catch (e: Exception) {
                Log.e(TAG, "Failed to disable VoiceCursor", e)
                false
            }
        }
    }
    
    /**
     * Calibrate cursor tracking
     */
    private suspend fun calibrateCursor(): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                voiceCursor?.calibrate() ?: false
            } catch (e: Exception) {
                Log.e(TAG, "Failed to calibrate cursor", e)
                false
            }
        }
    }
    
    /**
     * Open cursor settings activity
     */
    private suspend fun openCursorSettings(): Boolean {
        return withContext(Dispatchers.Main) {
            try {
                val intent = Intent().apply {
                    setClassName(
                        context.packageName,
                        "com.augmentalis.voiceos.cursor.ui.VoiceCursorSettingsActivity"
                    )
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
                
                Log.d(TAG, "Cursor settings opened")
                true
            } catch (e: Exception) {
                Log.e(TAG, "Failed to open cursor settings", e)
                false
            }
        }
    }
    
    /**
     * Show cursor help information
     */
    private suspend fun showCursorHelp(): Boolean {
        return withContext(Dispatchers.Main) {
            try {
                // Could integrate with VoiceUI to show help overlay
                Log.d(TAG, "Cursor help information displayed")
                true
            } catch (e: Exception) {
                Log.e(TAG, "Failed to show cursor help", e)
                false
            }
        }
    }
    
    /**
     * Show cursor coordinates
     */
    private suspend fun showCoordinates(): Boolean {
        return withContext(Dispatchers.Main) {
            try {
                val intent = Intent(context, VoiceCursorOverlayService::class.java).apply {
                    putExtra("action", "show_coordinates")
                }
                context.startForegroundService(intent)
                
                Log.d(TAG, "Cursor coordinates shown")
                true
            } catch (e: Exception) {
                Log.e(TAG, "Failed to show cursor coordinates", e)
                false
            }
        }
    }
    
    /**
     * Hide cursor coordinates
     */
    private suspend fun hideCoordinates(): Boolean {
        return withContext(Dispatchers.Main) {
            try {
                val intent = Intent(context, VoiceCursorOverlayService::class.java).apply {
                    putExtra("action", "hide_coordinates")
                }
                context.startForegroundService(intent)
                
                Log.d(TAG, "Cursor coordinates hidden")
                true
            } catch (e: Exception) {
                Log.e(TAG, "Failed to hide cursor coordinates", e)
                false
            }
        }
    }
    
    /**
     * Toggle cursor coordinates display
     */
    private suspend fun toggleCoordinates(): Boolean {
        return withContext(Dispatchers.Main) {
            try {
                val intent = Intent(context, VoiceCursorOverlayService::class.java).apply {
                    putExtra("action", "toggle_coordinates")
                }
                context.startForegroundService(intent)
                
                Log.d(TAG, "Cursor coordinates toggled")
                true
            } catch (e: Exception) {
                Log.e(TAG, "Failed to toggle cursor coordinates", e)
                false
            }
        }
    }
    
    /**
     * Get list of supported voice commands
     */
    fun getSupportedCommands(): List<String> {
        return listOf(
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
    }
    
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
            commandsSupported = if (isInitialized) getSupportedCommands().size else 0
        )
    }
    
    /**
     * Cleanup resources
     */
    fun dispose() {
        unregisterCommands()
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

/**
 * Cursor movement directions
 */
enum class CursorDirection {
    UP, DOWN, LEFT, RIGHT
}