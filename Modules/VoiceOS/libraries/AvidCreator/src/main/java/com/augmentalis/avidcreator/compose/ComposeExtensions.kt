/**
 * ComposeExtensions.kt - Jetpack Compose extensions for AvidManager
 * Path: libraries/AvidManager/src/main/java/com/ai/uuidmgr/compose/ComposeExtensions.kt
 * 
 * Author: Manoj Jhawar
 * Code-Reviewed-By: CCA
 * Created: 2024-08-20
 * 
 * Jetpack Compose modifier extensions for UUID management
 */

package com.augmentalis.avidcreator.compose

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.platform.debugInspectorInfo
import com.augmentalis.avidcreator.AvidCreator
import com.augmentalis.avidcreator.models.AvidElement
import com.augmentalis.avidcreator.models.VUIDPosition
import com.augmentalis.avidcreator.models.VUIDMetadata
import java.util.UUID

/**
 * Compose modifier for UUID registration and management
 * 
 * @param manager AvidManager instance (defaults to singleton)
 * @param uuid Custom UUID (generated if null)
 * @param name Human-readable name for the element
 * @param type Element type (button, text, etc.)
 * @param description Optional description for accessibility
 * @param parent Parent element UUID for hierarchy
 * @param position Spatial position information
 * @param actions Available actions for this element
 * @param priority Priority for targeting when multiple matches
 * @param metadata Additional metadata
 */
fun Modifier.withUUID(
    manager: AvidCreator = AvidCreator.getInstance(),
    uuid: String? = null,
    name: String? = null,
    type: String = "composable",
    description: String? = null,
    parent: String? = null,
    position: VUIDPosition? = null,
    actions: Map<String, (Map<String, Any>) -> Unit> = emptyMap(),
    priority: Int = 0,
    metadata: VUIDMetadata? = null
): Modifier = composed(
    inspectorInfo = debugInspectorInfo {
        this.name = "withUUID"
        properties["uuid"] = uuid
        properties["name"] = name
        properties["type"] = type
    }
) {
    val elementUuid = remember { uuid ?: UUID.randomUUID().toString() }
    
    DisposableEffect(elementUuid) {
        val element = AvidElement(
            vuid = elementUuid,
            name = name,
            type = type,
            description = description,
            parent = parent,
            position = position,
            actions = actions,
            priority = priority,
            metadata = metadata
        )
        
        manager.registerElement(element)
        
        onDispose {
            manager.unregisterElement(elementUuid)
        }
    }
    
    this
}

/**
 * Simplified UUID modifier for basic elements
 */
fun Modifier.assignUUID(
    name: String? = null,
    type: String = "composable",
    actions: Map<String, (Map<String, Any>) -> Unit> = emptyMap()
): Modifier = withUUID(
    name = name,
    type = type,
    actions = actions
)

/**
 * UUID modifier for buttons with click action
 */
fun Modifier.uuidButton(
    name: String,
    onClick: () -> Unit
): Modifier = withUUID(
    name = name,
    type = "button",
    actions = mapOf(
        "click" to { onClick() },
        "tap" to { onClick() },
        "select" to { onClick() },
        "activate" to { onClick() }
    )
)

/**
 * UUID modifier for text inputs with focus and text actions
 */
fun Modifier.uuidTextInput(
    name: String,
    onFocus: (() -> Unit)? = null,
    onTextChange: ((String) -> Unit)? = null
): Modifier = withUUID(
    name = name,
    type = "textfield",
    actions = buildMap {
        put("focus") { onFocus?.invoke() }
        onTextChange?.let { handler ->
            put("setText") { params ->
                val text = params["text"] as? String ?: ""
                handler(text)
            }
        }
        put("clear") { onTextChange?.invoke("") }
    }
)

/**
 * UUID modifier for navigable elements with spatial position
 */
fun Modifier.uuidNavigable(
    name: String? = null,
    type: String = "navigable",
    row: Int = 0,
    column: Int = 0,
    index: Int = 0,
    onNavigate: ((String) -> Unit)? = null
): Modifier = withUUID(
    name = name,
    type = type,
    position = VUIDPosition(
        row = row,
        column = column,
        index = index
    ),
    actions = buildMap {
        onNavigate?.let { handler ->
            put("navigate") { params ->
                val direction = params["direction"] as? String ?: "unknown"
                handler(direction)
            }
        }
    }
)

/**
 * UUID modifier for containers with child management
 */
fun Modifier.uuidContainer(
    name: String,
    type: String = "container",
    @Suppress("UNUSED_PARAMETER") children: List<String> = emptyList()
): Modifier = withUUID(
    name = name,
    type = type,
    actions = mapOf(
        "focus" to { /* Focus container */ },
        "collapse" to { /* Collapse container */ },
        "expand" to { /* Expand container */ }
    )
)

/**
 * Get UUID from a Composable element (requires the element to be registered)
 */
@Composable
fun rememberUUID(
    @Suppress("UNUSED_PARAMETER") name: String? = null,
    @Suppress("UNUSED_PARAMETER") type: String = "composable"
): String {
    return remember { UUID.randomUUID().toString() }
}

/**
 * Composable function to register elements in the composition tree
 */
@Composable
fun VUIDScope(
    manager: AvidCreator = AvidCreator.getInstance(),
    name: String? = null,
    type: String = "scope",
    content: @Composable () -> Unit
) {
    val scopeVuid = remember { UUID.randomUUID().toString() }

    DisposableEffect(scopeVuid) {
        val element = AvidElement(
            vuid = scopeVuid,
            name = name,
            type = type
        )
        manager.registerElement(element)

        onDispose {
            manager.unregisterElement(scopeVuid)
        }
    }

    content()
}

@Suppress("DEPRECATION")
@Deprecated("Use VUIDScope instead", ReplaceWith("VUIDScope(manager, name, type, content)"))
@Composable
fun UUIDScope(
    manager: AvidCreator = AvidCreator.getInstance(),
    name: String? = null,
    type: String = "scope",
    content: @Composable () -> Unit
) = VUIDScope(manager, name, type, content)

/**
 * Voice command integration for Compose elements
 */
@Composable
fun VoiceCommandHandler(
    @Suppress("UNUSED_PARAMETER") manager: AvidCreator = AvidCreator.getInstance(),
    @Suppress("UNUSED_PARAMETER") onCommandReceived: suspend (String) -> Unit = { command ->
        manager.processVoiceCommand(command)
    }
) {
    // This would integrate with speech recognition system
    // For now, it's a placeholder for voice command handling
}