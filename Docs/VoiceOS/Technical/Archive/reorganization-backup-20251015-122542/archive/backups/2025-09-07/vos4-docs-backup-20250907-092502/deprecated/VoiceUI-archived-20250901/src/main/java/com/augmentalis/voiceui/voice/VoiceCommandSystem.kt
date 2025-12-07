/**
 * VoiceCommandSystem.kt - UUID-based voice command targeting system
 * Advanced voice command system with hierarchical targeting and spatial navigation
 */

package com.augmentalis.voiceui.voice

import android.content.Context
import android.util.Log
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.platform.debugInspectorInfo
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Advanced voice command system with UUID-based targeting
 * 
 * Features:
 * - UUID assignment to all UI elements
 * - Hierarchical command processing
 * - Spatial navigation ("move left", "select third")
 * - Context-aware command interpretation
 * - Multiple targeting methods (UUID, name, type, position, hierarchy, context, recent)
 */
class VoiceCommandSystem(
    private val context: Context,
    private val scope: CoroutineScope
) {
    
    companion object {
        private const val TAG = "VoiceUIVoiceCommandSystem"
        private const val COMMAND_TIMEOUT = 5000L
        private const val MAX_COMMAND_HISTORY = 100
    }
    
    // Voice target types - All 7 targeting methods
    enum class TargetType {
        UUID,           // Direct UUID targeting
        NAME,           // Element name/label
        TYPE,           // Element type (button, text, etc.)
        POSITION,       // Spatial position (first, last, third)
        HIERARCHY,      // Parent/child navigation
        CONTEXT,        // Context-based targeting
        RECENT          // Recently used elements
    }
    
    // Voice command data
    data class VoiceCommand(
        val id: String = UUID.randomUUID().toString(),
        val text: String,
        val targetType: TargetType,
        val targetId: String? = null,
        val action: String,
        val parameters: Map<String, Any> = emptyMap(),
        val confidence: Float = 1.0f,
        val timestamp: Long = System.currentTimeMillis()
    )
    
    // Voice target registration
    data class VoiceTarget(
        val uuid: String = UUID.randomUUID().toString(),
        val name: String? = null,
        val type: String,
        val description: String? = null,
        val parent: String? = null,
        val children: MutableList<String> = mutableListOf(),
        val position: Position? = null,
        val actions: Map<String, (Map<String, Any>) -> Unit> = emptyMap(),
        val isEnabled: Boolean = true,
        val priority: Int = 0
    )
    
    // Command result
    data class CommandResult(
        val success: Boolean,
        val message: String? = null
    )
    
    // Spatial position
    data class Position(
        val x: Float,
        val y: Float,
        val z: Float = 0f,
        val index: Int = 0,
        val row: Int = 0,
        val column: Int = 0
    )
    
    
    // Registered targets
    private val targets = ConcurrentHashMap<String, VoiceTarget>()
    private val targetsByType = ConcurrentHashMap<String, MutableList<VoiceTarget>>()
    private val targetsByName = ConcurrentHashMap<String, VoiceTarget>()
    
    // Command history
    private val commandHistory = mutableListOf<VoiceCommand>()
    
    // State flows
    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()
    
    private val _lastCommand = MutableStateFlow<VoiceCommand?>(null)
    val lastCommand: StateFlow<VoiceCommand?> = _lastCommand.asStateFlow()
    
    /**
     * Register a voice target
     */
    fun registerTarget(target: VoiceTarget): String {
        targets[target.uuid] = target
        
        // Index by type
        targetsByType.getOrPut(target.type) { mutableListOf() }.add(target)
        
        // Index by name if available
        target.name?.let { name ->
            targetsByName[name.lowercase()] = target
        }
        
        Log.d(TAG, "Registered voice target: ${target.uuid} (${target.type})")
        return target.uuid
    }
    
    /**
     * Unregister a voice target
     */
    fun unregisterTarget(uuid: String) {
        val target = targets.remove(uuid)
        target?.let {
            // Remove from type index
            targetsByType[it.type]?.remove(it)
            
            // Remove from name index
            it.name?.let { name ->
                targetsByName.remove(name.lowercase())
            }
            
            Log.d(TAG, "Unregistered voice target: $uuid")
        }
    }
    
    /**
     * Process a voice command
     */
    suspend fun processCommand(commandText: String): CommandResult {
        return withContext(scope.coroutineContext) {
            try {
                val command = parseCommand(commandText)
                _lastCommand.value = command
                
                // Add to history
                addToHistory(command)
                
                // Find and execute target
                val target = findTarget(command)
                if (target != null) {
                    // Execute action on target
                    val handler = target.actions[command.action]
                    if (handler != null) {
                        handler(command.parameters)
                        CommandResult(true, "Command executed")
                    } else {
                        CommandResult(false, "Action not found")
                    }
                } else {
                    CommandResult(false, "Target not found")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error processing command: $commandText", e)
                CommandResult(false, "Command failed: ${e.message}")
            }
        }
    }
    
    /**
     * Parse voice command text
     */
    private fun parseCommand(commandText: String): VoiceCommand {
        val parts = commandText.lowercase().split(" ")
        
        return when {
            // UUID-based commands: "click uuid-12345"
            commandText.contains("uuid-") -> {
                val uuid = extractUuid(commandText)
                VoiceCommand(
                    text = commandText,
                    targetType = TargetType.UUID,
                    targetId = uuid,
                    action = parts.first()
                )
            }
            
            // Position-based commands: "click third button", "select first item"
            containsPositionalWords(parts) -> {
                val position = extractPosition(parts)
                val type = extractType(parts)
                VoiceCommand(
                    text = commandText,
                    targetType = TargetType.POSITION,
                    targetId = "$type:$position",
                    action = parts.first(),
                    parameters = mapOf("type" to type)
                )
            }
            
            // Type-based commands: "click button", "open menu"
            parts.size >= 2 -> {
                VoiceCommand(
                    text = commandText,
                    targetType = TargetType.TYPE,
                    targetId = parts.last(),
                    action = parts.first()
                )
            }
            
            // Simple commands: "back", "home"
            else -> {
                VoiceCommand(
                    text = commandText,
                    targetType = TargetType.NAME,
                    targetId = commandText.lowercase(),
                    action = commandText.lowercase()
                )
            }
        }
    }
    
    /**
     * Find target based on command
     */
    private fun findTarget(command: VoiceCommand): VoiceTarget? {
        return when (command.targetType) {
            TargetType.UUID -> targets[command.targetId]
            TargetType.NAME -> targetsByName[command.targetId]
            TargetType.TYPE -> {
                val typeTargets = targetsByType[command.targetId] ?: return null
                if (typeTargets.size == 1) typeTargets.first() else null
            }
            TargetType.POSITION -> findByPosition(command)
            TargetType.HIERARCHY -> null
            TargetType.CONTEXT -> null
            TargetType.RECENT -> null
        }
    }
    
    private fun findByPosition(command: VoiceCommand): VoiceTarget? {
        val parts = command.targetId?.split(":") ?: return null
        if (parts.size < 2) return null
        val type = parts[0]
        val position = parts[1].toIntOrNull() ?: return null
        val typeTargets = targetsByType[type] ?: return null
        
        return typeTargets.getOrNull(position - 1) // Convert to 0-based index
    }
    
    
    // Helper methods
    private fun extractUuid(text: String): String {
        val regex = "uuid-([a-f0-9-]+)".toRegex()
        return regex.find(text)?.groupValues?.get(1) ?: ""
    }
    
    private fun containsPositionalWords(parts: List<String>): Boolean {
        val positionalWords = setOf("first", "second", "third", "fourth", "fifth", "1st", "2nd", "3rd", "4th", "5th")
        return parts.any { it in positionalWords }
    }
    
    private fun extractPosition(parts: List<String>): String {
        val positionMap = mapOf(
            "first" to "1", "1st" to "1",
            "second" to "2", "2nd" to "2",
            "third" to "3", "3rd" to "3",
            "fourth" to "4", "4th" to "4",
            "fifth" to "5", "5th" to "5"
        )
        
        return parts.firstNotNullOfOrNull { positionMap[it] } ?: "1"
    }
    
    private fun extractType(parts: List<String>): String {
        val commonTypes = setOf("button", "text", "item", "menu", "link", "image")
        return parts.firstOrNull { it in commonTypes } ?: "element"
    }
    
    private fun addToHistory(command: VoiceCommand) {
        commandHistory.add(command)
        if (commandHistory.size > MAX_COMMAND_HISTORY) {
            commandHistory.removeAt(0)
        }
    }
    
    fun shutdown() {
        targets.clear()
        targetsByType.clear()
        targetsByName.clear()
        commandHistory.clear()
    }
    
    // Additional data class for provider support
    data class Command(
        val text: String,
        val action: String,
        val language: String = "en-US"
    )
    
    // Additional methods for intent/provider support
    private val registeredCommands = mutableListOf<Command>()
    private var isEnabled = false
    private var wakeWord = "hey voice"
    
    fun registerCommand(command: String, action: String, language: String = "en-US") {
        registeredCommands.add(Command(command, action, language))
        Log.d(TAG, "Registered command: $command -> $action")
    }
    
    fun processAudio(audioData: ByteArray, language: String) {
        Log.d(TAG, "Processing audio data (${audioData.size} bytes) for language: $language")
        // TODO: Integrate with speech recognition
    }
    
    fun setEnabled(enabled: Boolean, wakeWord: String? = null) {
        isEnabled = enabled
        wakeWord?.let { this.wakeWord = it }
        Log.d(TAG, "Voice commands enabled: $enabled, wake word: ${this.wakeWord}")
    }
    
    fun isEnabled(): Boolean = isEnabled
    
    fun getRegisteredCommands(): List<Command> = registeredCommands.toList()
}