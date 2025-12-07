package com.augmentalis.voiceui.core

import androidx.compose.runtime.*
import com.augmentalis.uuidmanager.UUIDManager
import com.augmentalis.uuidmanager.models.UUIDElement
import com.augmentalis.uuidmanager.models.UUIDPosition
import com.augmentalis.uuidmanager.models.UUIDMetadata
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

/**
 * MagicUUIDIntegration - Seamless UUID tracking for all VoiceUI components
 * 
 * Features:
 * - 🆔 Automatic UUID generation for all components
 * - 🎤 Voice command registration with UUIDs
 * - 📍 Spatial navigation support
 * - 🔍 Component discovery by UUID/name/type
 * - 📊 Hierarchy tracking (parent/child relationships)
 */
object MagicUUIDIntegration {
    
    private val uuidManager = UUIDManager.instance
    private val scope = CoroutineScope(Dispatchers.Default)
    
    // Track component metadata
    private val componentMetadata = ConcurrentHashMap<String, ComponentMetadata>()
    
    // Track screen hierarchy
    private val screenHierarchy = ConcurrentHashMap<String, ScreenInfo>()
    
    // Track voice commands
    private val voiceCommandMap = ConcurrentHashMap<String, VoiceCommandInfo>()
    
    /**
     * Generate UUID for a screen
     */
    fun generateScreenUUID(screenName: String): String {
        val uuid = uuidManager.generateUUID()
        val element = UUIDElement(
            uuid = uuid,
            name = screenName,
            type = "magic_screen",
            metadata = UUIDMetadata(
                state = mapOf(
                    "screenName" to screenName,
                    "createdAt" to System.currentTimeMillis()
                )
            )
        )
        
        uuidManager.registerElement(element)
        
        screenHierarchy[uuid] = ScreenInfo(
            uuid = uuid,
            name = screenName,
            components = mutableSetOf(),
            childScreens = mutableSetOf()
        )
        
        return uuid
    }
    
    /**
     * Generate UUID for a component
     */
    fun generateComponentUUID(
        componentType: String,
        screenUUID: String? = null,
        name: String? = null,
        position: ComponentPosition? = null
    ): String {
        val uuid = uuidManager.generateUUID()
        
        val uuidPosition = position?.let {
            UUIDPosition(
                x = it.x,
                y = it.y,
                z = it.z,
                row = it.row ?: 0,
                column = it.column ?: 0,
                index = it.index ?: 0
            )
        }
        
        val element = UUIDElement(
            uuid = uuid,
            name = name ?: "$componentType-$uuid",
            type = "magic_component_$componentType",
            position = uuidPosition,
            parent = screenUUID,
            metadata = UUIDMetadata(
                state = mapOf(
                    "componentType" to componentType,
                    "screenUUID" to (screenUUID ?: ""),
                    "createdAt" to System.currentTimeMillis()
                )
            ),
            actions = createComponentActions(componentType)
        )
        
        uuidManager.registerElement(element)
        
        // Track metadata
        componentMetadata[uuid] = ComponentMetadata(
            uuid = uuid,
            type = componentType,
            screenUUID = screenUUID,
            name = name,
            position = position
        )
        
        // Update screen hierarchy
        screenUUID?.let {
            screenHierarchy[it]?.components?.add(uuid)
        }
        
        return uuid
    }
    
    /**
     * Generate UUID for a voice command
     */
    fun generateVoiceCommandUUID(
        command: String,
        targetUUID: String,
        action: String,
        context: String? = null
    ): String {
        val uuid = uuidManager.generateUUID()
        
        val element = UUIDElement(
            uuid = uuid,
            name = "voice_command_$command",
            type = "voice_command",
            metadata = UUIDMetadata(
                state = mapOf(
                    "command" to command,
                    "targetUUID" to targetUUID,
                    "action" to action,
                    "context" to (context ?: "")
                )
            ),
            actions = mapOf(
                "execute" to { params ->
                    executeVoiceCommand(targetUUID, action, params)
                }
            )
        )
        
        uuidManager.registerElement(element)
        
        voiceCommandMap[uuid] = VoiceCommandInfo(
            uuid = uuid,
            command = command,
            targetUUID = targetUUID,
            action = action,
            context = context
        )
        
        return uuid
    }
    
    /**
     * Register a composable component with UUID tracking
     */
    @Composable
    fun rememberComponentUUID(
        componentType: String,
        screenUUID: String? = null,
        name: String? = null
    ): String {
        return remember {
            generateComponentUUID(componentType, screenUUID, name)
        }.also { uuid ->
            DisposableEffect(uuid) {
                onDispose {
                    unregisterComponent(uuid)
                }
            }
        }
    }
    
    /**
     * Register a screen with UUID tracking
     */
    @Composable
    fun rememberScreenUUID(screenName: String): String {
        return remember {
            generateScreenUUID(screenName)
        }.also { uuid ->
            DisposableEffect(uuid) {
                onDispose {
                    unregisterScreen(uuid)
                }
            }
        }
    }
    
    /**
     * Find component by various criteria
     */
    fun findComponent(
        uuid: String? = null,
        name: String? = null,
        type: String? = null,
        screenUUID: String? = null
    ): ComponentMetadata? {
        return when {
            uuid != null -> componentMetadata[uuid]
            name != null -> {
                val elements = uuidManager.findByName(name)
                elements.firstOrNull()?.uuid?.let { componentMetadata[it] }
            }
            type != null -> {
                val elements = uuidManager.findByType("magic_component_$type")
                elements.firstOrNull()?.uuid?.let { componentMetadata[it] }
            }
            screenUUID != null -> {
                screenHierarchy[screenUUID]?.components
                    ?.mapNotNull { componentMetadata[it] }
                    ?.firstOrNull()
            }
            else -> null
        }
    }
    
    /**
     * Navigate spatially between components
     */
    fun navigateToComponent(
        fromUUID: String,
        direction: NavigationDirection
    ): String? {
        val directionString = when (direction) {
            NavigationDirection.UP -> "up"
            NavigationDirection.DOWN -> "down"
            NavigationDirection.LEFT -> "left"
            NavigationDirection.RIGHT -> "right"
            NavigationDirection.NEXT -> "next"
            NavigationDirection.PREVIOUS -> "previous"
            NavigationDirection.FIRST -> "first"
            NavigationDirection.LAST -> "last"
        }
        
        return uuidManager.findInDirection(fromUUID, directionString)?.uuid
    }
    
    /**
     * Execute voice command on component
     */
    fun executeVoiceCommand(
        targetUUID: String,
        action: String,
        parameters: Map<String, Any> = emptyMap()
    ) {
        scope.launch {
            uuidManager.executeAction(targetUUID, action, parameters)
        }
    }
    
    /**
     * Process natural language voice command
     */
    fun processVoiceCommand(command: String) {
        scope.launch {
            val result = uuidManager.processVoiceCommand(command)
            
            // Log or handle result
            if (result.success) {
                // Successfully executed
                result.targetUUID?.let { uuid ->
                    componentMetadata[uuid]?.let { _ ->
                        // Update component state if needed
                    }
                }
            }
        }
    }
    
    /**
     * Unregister component
     */
    private fun unregisterComponent(uuid: String) {
        uuidManager.unregisterElement(uuid)
        componentMetadata.remove(uuid)
        
        // Remove from screen hierarchy
        screenHierarchy.values.forEach { screen ->
            screen.components.remove(uuid)
        }
    }
    
    /**
     * Unregister screen and all its components
     */
    private fun unregisterScreen(uuid: String) {
        val screen = screenHierarchy[uuid]
        screen?.components?.forEach { componentUUID ->
            unregisterComponent(componentUUID)
        }
        
        uuidManager.unregisterElement(uuid)
        screenHierarchy.remove(uuid)
    }
    
    /**
     * Create component-specific actions
     */
    private fun createComponentActions(componentType: String): Map<String, (Map<String, Any>) -> Unit> {
        return when (componentType) {
            "button", "submit" -> mapOf(
                "click" to { _ -> /* Handle click */ },
                "focus" to { _ -> /* Handle focus */ },
                "hover" to { _ -> /* Handle hover */ }
            )
            "input", "email", "password" -> mapOf(
                "focus" to { _ -> /* Handle focus */ },
                "clear" to { _ -> /* Clear input */ },
                "setValue" to { params -> 
                    params["value"] as? String
                    // Set input value
                }
            )
            "toggle", "switch" -> mapOf(
                "toggle" to { _ -> /* Toggle state */ },
                "setOn" to { _ -> /* Set to on */ },
                "setOff" to { _ -> /* Set to off */ }
            )
            else -> mapOf(
                "default" to { _ -> /* Default action */ }
            )
        }
    }
    
    /**
     * Get statistics about registered components
     */
    fun getStatistics(): UUIDStatistics {
        val stats = uuidManager.getStats()
        return UUIDStatistics(
            totalComponents = componentMetadata.size,
            totalScreens = screenHierarchy.size,
            totalVoiceCommands = voiceCommandMap.size,
            componentsByType = componentMetadata.values
                .groupBy { it.type }
                .mapValues { it.value.size },
            registryStats = stats
        )
    }
    
    /**
     * Clear all registrations (use with caution)
     */
    fun clearAll() {
        componentMetadata.clear()
        screenHierarchy.clear()
        voiceCommandMap.clear()
        uuidManager.clearAll()
    }
}

// Data classes

data class ComponentMetadata(
    val uuid: String,
    val type: String,
    val screenUUID: String?,
    val name: String?,
    val position: ComponentPosition?
)

data class ComponentPosition(
    val x: Float = 0f,
    val y: Float = 0f,
    val z: Float = 0f,
    val row: Int? = null,
    val column: Int? = null,
    val index: Int? = null
)

data class ScreenInfo(
    val uuid: String,
    val name: String,
    val components: MutableSet<String>,
    val childScreens: MutableSet<String>
)

data class VoiceCommandInfo(
    val uuid: String,
    val command: String,
    val targetUUID: String,
    val action: String,
    val context: String?
)

enum class NavigationDirection {
    UP, DOWN, LEFT, RIGHT, NEXT, PREVIOUS, FIRST, LAST
}

data class UUIDStatistics(
    val totalComponents: Int,
    val totalScreens: Int,
    val totalVoiceCommands: Int,
    val componentsByType: Map<String, Int>,
    val registryStats: Any? // RegistryStats from UUIDManager
)

/**
 * Extension functions for easy integration
 */

@Composable
fun rememberMagicUUID(type: String, name: String? = null): String {
    return MagicUUIDIntegration.rememberComponentUUID(type, name = name)
}

@Composable
fun rememberScreenUUID(name: String): String {
    return MagicUUIDIntegration.rememberScreenUUID(name)
}