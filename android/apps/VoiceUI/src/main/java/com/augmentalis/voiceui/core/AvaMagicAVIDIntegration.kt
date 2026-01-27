package com.augmentalis.voiceui.core

import androidx.compose.runtime.*
import com.augmentalis.avidcreator.AvidElementManager
import com.augmentalis.avidcreator.AvidElement
import com.augmentalis.avidcreator.AvidPosition
import com.augmentalis.avidcreator.AvidMetadata
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

/**
 * AvaMagicAVIDIntegration - Seamless AVID tracking for all VoiceUI components
 *
 * Features:
 * - Automatic AVID generation for all components
 * - Voice command registration with AVIDs
 * - Spatial navigation support
 * - Component discovery by AVID/name/type
 * - Hierarchy tracking (parent/child relationships)
 *
 * Note: Migrated from UUID to AVID (Augmentalis Voice ID) for universal identification.
 */
object AvaMagicAVIDIntegration {

    private val avidManager = AvidElementManager.getInstance()
    private val scope = CoroutineScope(Dispatchers.Default)

    // Track component metadata
    private val componentMetadata = ConcurrentHashMap<String, ComponentMetadata>()

    // Track screen hierarchy
    private val screenHierarchy = ConcurrentHashMap<String, ScreenInfo>()

    // Track voice commands
    private val voiceCommandMap = ConcurrentHashMap<String, VoiceCommandInfo>()

    /**
     * Generate AVID for a screen
     */
    fun generateScreenUUID(screenName: String): String {
        val avid = avidManager.generateAvid()
        val element = AvidElement(
            avid = avid,
            name = screenName,
            type = "magic_screen",
            metadata = AvidMetadata(
                state = mapOf(
                    "screenName" to screenName,
                    "createdAt" to System.currentTimeMillis()
                )
            )
        )

        avidManager.registerElement(element)

        screenHierarchy[avid] = ScreenInfo(
            uuid = avid,
            name = screenName,
            components = mutableSetOf(),
            childScreens = mutableSetOf()
        )

        return avid
    }

    /**
     * Generate AVID for a component
     */
    fun generateComponentUUID(
        componentType: String,
        screenUUID: String? = null,
        name: String? = null,
        position: ComponentPosition? = null
    ): String {
        val avid = avidManager.generateAvid()

        val avidPosition = position?.let {
            AvidPosition(
                x = it.x,
                y = it.y,
                z = it.z,
                row = it.row ?: 0,
                column = it.column ?: 0,
                index = it.index ?: 0
            )
        }

        val element = AvidElement(
            avid = avid,
            name = name ?: "$componentType-$avid",
            type = "magic_component_$componentType",
            position = avidPosition,
            parent = screenUUID,
            metadata = AvidMetadata(
                state = mapOf(
                    "componentType" to componentType,
                    "screenUUID" to (screenUUID ?: ""),
                    "createdAt" to System.currentTimeMillis()
                )
            ),
            actions = createComponentActions(componentType)
        )

        avidManager.registerElement(element)

        // Track metadata
        componentMetadata[avid] = ComponentMetadata(
            uuid = avid,
            type = componentType,
            screenUUID = screenUUID,
            name = name,
            position = position
        )

        // Update screen hierarchy
        screenUUID?.let {
            screenHierarchy[it]?.components?.add(avid)
        }

        return avid
    }

    /**
     * Generate AVID for a voice command
     */
    fun generateVoiceCommandUUID(
        command: String,
        targetUUID: String,
        action: String,
        context: String? = null
    ): String {
        val avid = avidManager.generateAvid()

        val element = AvidElement(
            avid = avid,
            name = "voice_command_$command",
            type = "voice_command",
            metadata = AvidMetadata(
                state = mapOf(
                    "command" to command,
                    "targetUUID" to targetUUID,
                    "action" to action,
                    "context" to (context ?: "")
                )
            ),
            actions = mapOf(
                "execute" to { params: Map<String, Any> ->
                    executeVoiceCommand(targetUUID, action, params)
                }
            )
        )

        avidManager.registerElement(element)

        voiceCommandMap[avid] = VoiceCommandInfo(
            uuid = avid,
            command = command,
            targetUUID = targetUUID,
            action = action,
            context = context
        )

        return avid
    }

    /**
     * Register a composable component with AVID tracking
     */
    @Composable
    fun rememberComponentUUID(
        componentType: String,
        screenUUID: String? = null,
        name: String? = null
    ): String {
        return remember {
            generateComponentUUID(componentType, screenUUID, name)
        }.also { avid ->
            DisposableEffect(avid) {
                onDispose {
                    unregisterComponent(avid)
                }
            }
        }
    }

    /**
     * Register a screen with AVID tracking
     */
    @Composable
    fun rememberScreenUUID(screenName: String): String {
        return remember {
            generateScreenUUID(screenName)
        }.also { avid ->
            DisposableEffect(avid) {
                onDispose {
                    unregisterScreen(avid)
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
                val elements = avidManager.findByName(name)
                elements.firstOrNull()?.avid?.let { foundAvid -> componentMetadata[foundAvid] }
            }
            type != null -> {
                val elements = avidManager.findByType("magic_component_$type")
                elements.firstOrNull()?.avid?.let { foundAvid -> componentMetadata[foundAvid] }
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

        return avidManager.findInDirection(fromUUID, directionString)?.avid
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
            avidManager.executeAction(targetUUID, action, parameters)
        }
    }

    /**
     * Process natural language voice command
     */
    fun processVoiceCommand(command: String) {
        scope.launch {
            val result = avidManager.processVoiceCommand(command)

            // Log or handle result
            if (result.success) {
                // Successfully executed
                result.targetAvid?.let { avid ->
                    componentMetadata[avid]?.let { _ ->
                        // Update component state if needed
                    }
                }
            }
        }
    }

    /**
     * Unregister component
     */
    private fun unregisterComponent(avid: String) {
        avidManager.unregisterElement(avid)
        componentMetadata.remove(avid)

        // Remove from screen hierarchy
        screenHierarchy.values.forEach { screen ->
            screen.components.remove(avid)
        }
    }

    /**
     * Unregister screen and all its components
     */
    private fun unregisterScreen(avid: String) {
        val screen = screenHierarchy[avid]
        screen?.components?.forEach { componentAvid ->
            unregisterComponent(componentAvid)
        }

        avidManager.unregisterElement(avid)
        screenHierarchy.remove(avid)
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
        val stats = avidManager.getStats()
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
        avidManager.clearAll()
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
    val registryStats: Any? // RegistryStats from AvidElementManager
)

/**
 * Extension functions for easy integration
 */

@Composable
fun rememberMagicVUID(type: String, name: String? = null): String {
    return AvaMagicAVIDIntegration.rememberComponentUUID(type, name = name)
}

@Composable
fun rememberScreenVUID(name: String): String {
    return AvaMagicAVIDIntegration.rememberScreenUUID(name)
}

// Deprecated aliases for backwards compatibility
@Deprecated("Use rememberMagicVUID instead", ReplaceWith("rememberMagicVUID(type, name)"))
@Composable
fun rememberMagicUUID(type: String, name: String? = null): String = rememberMagicVUID(type, name)

@Deprecated("Use rememberScreenVUID instead", ReplaceWith("rememberScreenVUID(name)"))
@Composable
fun rememberScreenUUID(name: String): String = rememberScreenVUID(name)

// Backward compatibility typealias
@Deprecated("Use AvaMagicAVIDIntegration instead", ReplaceWith("AvaMagicAVIDIntegration"))
typealias MagicVUIDIntegration = AvaMagicAVIDIntegration
