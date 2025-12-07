package com.augmentalis.uuidmanager

import com.augmentalis.uuidmanager.api.IUUIDManager
import com.augmentalis.uuidmanager.core.*
import com.augmentalis.uuidmanager.models.*
import com.augmentalis.uuidmanager.targeting.TargetResolver
import com.augmentalis.uuidmanager.spatial.SpatialNavigator
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.util.concurrent.ConcurrentHashMap
import android.util.Log

/**
 * Main UUIDManager library class
 * Universal Unique Identifier System for Voice & Spatial UI Control
 * 
 * Framework-agnostic UUID management extracted from UIKitVoiceCommandSystem
 */
open class UUIDManager : IUUIDManager {
    
    companion object {
        private const val TAG = "UUIDManager"
        private const val COMMAND_TIMEOUT = 5000L
        private const val MAX_COMMAND_HISTORY = 100
        
        /**
         * Global singleton instance
         */
        @JvmStatic
        val instance: UUIDManager by lazy { UUIDManager() }
        
        /**
         * Quick static access methods
         */
        @JvmStatic
        fun create(): UUIDManager = UUIDManager()
        
        @JvmStatic
        fun generate(): String = UUIDGenerator.generate()
    }
    
    private val registry = UUIDRegistry()
    private val targetResolver = TargetResolver(registry)
    private val spatialNavigator = SpatialNavigator(registry)
    
    // Legacy state management from UIKitVoiceCommandSystem (lines 105-113)
    private val registeredTargets = ConcurrentHashMap<String, VoiceTarget>()
    private val commandHistory = mutableListOf<VoiceCommand>()
    private val activeContext = MutableStateFlow<String?>(null)
    
    private val _commandEvents = MutableSharedFlow<VoiceCommand>()
    val commandEvents: SharedFlow<VoiceCommand> = _commandEvents.asSharedFlow()
    
    private val _commandResults = MutableSharedFlow<CommandResult>()
    val commandResults: SharedFlow<CommandResult> = _commandResults.asSharedFlow()
    
    /**
     * Generate a new UUID
     */
    override fun generateUUID(): String = UUIDGenerator.generate()
    
    /**
     * Register an element with UUID
     */
    override fun registerElement(element: UUIDElement): String {
        return runBlocking {
            registry.register(element)
        }
    }
    
    /**
     * Unregister an element
     */
    override fun unregisterElement(uuid: String): Boolean {
        return runBlocking {
            registry.unregister(uuid)
        }
    }
    
    /**
     * Find element by UUID
     */
    override fun findByUUID(uuid: String): UUIDElement? = registry.findByUUID(uuid)
    
    /**
     * Find elements by name
     */
    override fun findByName(name: String): List<UUIDElement> = registry.findByName(name)
    
    /**
     * Find elements by type
     */
    override fun findByType(type: String): List<UUIDElement> = registry.findByType(type)
    
    /**
     * Find element by position
     */
    override fun findByPosition(position: Int): UUIDElement? {
        val result = spatialNavigator.navigateToPosition(position)
        return result.target
    }
    
    /**
     * Find elements in direction
     */
    override fun findInDirection(fromUUID: String, direction: String): UUIDElement? {
        val dir = when (direction.lowercase()) {
            "left" -> SpatialNavigator.Direction.LEFT
            "right" -> SpatialNavigator.Direction.RIGHT
            "up" -> SpatialNavigator.Direction.UP
            "down" -> SpatialNavigator.Direction.DOWN
            "forward" -> SpatialNavigator.Direction.FORWARD
            "backward" -> SpatialNavigator.Direction.BACKWARD
            "next" -> SpatialNavigator.Direction.NEXT
            "previous" -> SpatialNavigator.Direction.PREVIOUS
            "first" -> SpatialNavigator.Direction.FIRST
            "last" -> SpatialNavigator.Direction.LAST
            else -> return null
        }
        
        val result = spatialNavigator.navigate(fromUUID, dir)
        return result.target
    }
    
    /**
     * Execute action on element
     */
    override suspend fun executeAction(uuid: String, action: String, parameters: Map<String, Any>): Boolean {
        val element = registry.findByUUID(uuid) ?: return false
        
        if (!element.isEnabled) return false
        
        val actionHandler = element.actions[action] ?: element.actions["default"] ?: return false
        
        return try {
            withTimeout(5000L) {
                actionHandler(parameters)
                true
            }
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Process voice command
     */
    override suspend fun processVoiceCommand(command: String): UUIDCommandResult {
        val startTime = System.currentTimeMillis()
        
        try {
            val parsedCommand = parseVoiceCommand(command)
            val targetResult = targetResolver.resolve(parsedCommand.targetRequest)
            
            if (targetResult.elements.isEmpty()) {
                return UUIDCommandResult(
                    success = false,
                    message = "No matching elements found",
                    executionTime = System.currentTimeMillis() - startTime
                )
            }
            
            // Execute on first matching element (highest priority)
            val target = targetResult.elements.first()
            val success = executeAction(target.uuid, parsedCommand.action, parsedCommand.parameters)
            
            return UUIDCommandResult(
                success = success,
                targetUUID = target.uuid,
                action = parsedCommand.action,
                message = if (success) "Command executed successfully" else "Command execution failed",
                executionTime = System.currentTimeMillis() - startTime
            )
            
        } catch (e: Exception) {
            return UUIDCommandResult(
                success = false,
                error = e.message,
                executionTime = System.currentTimeMillis() - startTime
            )
        }
    }
    
    /**
     * Get all registered elements
     */
    override fun getAllElements(): List<UUIDElement> = registry.getAllElements()
    
    /**
     * Clear all registrations
     */
    override fun clearAll() {
        runBlocking {
            registry.clear()
        }
    }
    
    /**
     * Create element with UUID
     */
    fun createElement(
        name: String? = null,
        type: String = "unknown",
        position: UUIDPosition? = null,
        actions: Map<String, (Map<String, Any>) -> Unit> = emptyMap()
    ): UUIDElement {
        return UUIDElement(
            uuid = generateUUID(),
            name = name,
            type = type,
            position = position,
            actions = actions
        )
    }
    
    /**
     * Register element with automatic UUID generation
     */
    fun registerWithAutoUUID(
        name: String? = null,
        type: String = "unknown",
        position: UUIDPosition? = null,
        actions: Map<String, (Map<String, Any>) -> Unit> = emptyMap()
    ): String {
        val element = createElement(name, type, position, actions)
        return registerElement(element)
    }
    
    /**
     * Get registry statistics
     */
    fun getStats(): RegistryStats = registry.getStats()
    
    /**
     * Navigate spatially
     */
    fun navigate(fromUUID: String, direction: String): UUIDElement? = findInDirection(fromUUID, direction)
    
    /**
     * Find nearest element
     */
    fun findNearest(fromUUID: String): UUIDElement? {
        return spatialNavigator.findNearest(fromUUID)?.target
    }
    
    /**
     * Parse voice command into structured request
     */
    private fun parseVoiceCommand(command: String): ParsedCommand {
        val normalizedCommand = command.lowercase().trim()
        
        // Direct UUID patterns
        val uuidPattern = Regex("(click|select|focus)\\s+(?:element\\s+)?(?:with\\s+)?uuid[\\s-]+([a-zA-Z0-9-]+)")
        uuidPattern.find(normalizedCommand)?.let { match ->
            return ParsedCommand(
                action = match.groupValues[1],
                targetRequest = TargetResolver.TargetRequest(TargetResolver.TargetType.UUID, match.groupValues[2])
            )
        }
        
        // Position patterns
        val positionPattern = Regex("(select|click)\\s+(first|second|third|fourth|fifth|last|\\d+)")
        positionPattern.find(normalizedCommand)?.let { match ->
            val positionText = match.groupValues[2]
            val position = when (positionText) {
                "first" -> 1
                "second" -> 2
                "third" -> 3
                "fourth" -> 4
                "fifth" -> 5
                "last" -> -1
                else -> positionText.toIntOrNull() ?: 1
            }
            return ParsedCommand(
                action = match.groupValues[1],
                targetRequest = TargetResolver.TargetRequest(TargetResolver.TargetType.POSITION, position = position)
            )
        }
        
        // Direction patterns
        val directionPattern = Regex("(move|go)\\s+(left|right|up|down|forward|backward|next|previous)")
        directionPattern.find(normalizedCommand)?.let { match ->
            return ParsedCommand(
                action = "focus",
                targetRequest = TargetResolver.TargetRequest(TargetResolver.TargetType.POSITION, direction = match.groupValues[2])
            )
        }
        
        // Name patterns
        val namePattern = Regex("(click|select|open|focus)\\s+(?:on\\s+)?(.+)")
        namePattern.find(normalizedCommand)?.let { match ->
            return ParsedCommand(
                action = match.groupValues[1],
                targetRequest = TargetResolver.TargetRequest(TargetResolver.TargetType.NAME, match.groupValues[2])
            )
        }
        
        // Default fallback
        return ParsedCommand(
            action = "click",
            targetRequest = TargetResolver.TargetRequest(TargetResolver.TargetType.CONTEXT, normalizedCommand)
        )
    }
    
    /**
     * Register a voice target - EXACT copy from legacy UIKitVoiceCommandSystem (lines 118-128)
     */
    fun registerTarget(target: VoiceTarget): String {
        registeredTargets[target.uuid] = target
        
        // Update parent's children list
        target.parent?.let { parentId ->
            registeredTargets[parentId]?.children?.add(target.uuid)
        }
        
        Log.d(TAG, "Registered voice target: ${target.uuid} (${target.name})")
        return target.uuid
    }
    
    /**
     * Unregister a voice target - EXACT copy from legacy UIKitVoiceCommandSystem (lines 133-147)
     */
    fun unregisterTarget(uuid: String) {
        val target = registeredTargets.remove(uuid)
        
        // Remove from parent's children
        target?.parent?.let { parentId ->
            registeredTargets[parentId]?.children?.remove(uuid)
        }
        
        // Unregister all children recursively
        target?.children?.forEach { childId ->
            unregisterTarget(childId)
        }
        
        Log.d(TAG, "Unregistered voice target: $uuid")
    }
    
    /**
     * Set active context - EXACT copy from legacy UIKitVoiceCommandSystem (lines 469-472)
     */
    fun setContext(context: String?) {
        activeContext.value = context
        Log.d(TAG, "Context changed to: $context")
    }
    
    /**
     * Clear all registered targets - EXACT copy from legacy UIKitVoiceCommandSystem (lines 477-480)
     */
    fun clearTargets() {
        registeredTargets.clear()
        commandHistory.clear()
    }

    /**
     * Parsed command structure
     */
    private data class ParsedCommand(
        val action: String,
        val targetRequest: TargetResolver.TargetRequest,
        val parameters: Map<String, Any> = emptyMap()
    )
}