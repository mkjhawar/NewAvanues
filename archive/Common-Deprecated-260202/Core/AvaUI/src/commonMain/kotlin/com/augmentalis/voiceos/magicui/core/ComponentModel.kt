package com.augmentalis.avanues.avaui.core

import kotlinx.serialization.Serializable

/**
 * Core data structure representing a UI component in the AvaUI system.
 *
 * ComponentModel is the fundamental building block for all UI elements, supporting:
 * - Namespaced unique identification
 * - 3D positioning
 * - Dynamic properties via map
 * - Hierarchical composition (parent-child relationships)
 *
 * ## Example Usage
 *
 * ```kotlin
 * val button = ComponentModel(
 *     uuid = "app.main/button_submit",
 *     type = "Button",
 *     position = ComponentPosition(x = 0f, y = 0f),
 *     properties = mapOf(
 *         "text" to "Submit",
 *         "color" to "#007AFF",
 *         "enabled" to true
 *     )
 * )
 * ```
 *
 * @property uuid Namespaced unique identifier (format: "namespace/local-id")
 * @property type Component type (e.g., "Button", "TextField", "Row", "Column")
 * @property position 3D position in the layout
 * @property properties Dynamic component properties (text, color, size, etc.)
 * @property children Child components for containers (empty for leaf components)
 *
 * @since 3.1.0
 */
@Serializable
data class ComponentModel(
    val uuid: String,
    val type: String,
    val position: ComponentPosition,
    val properties: Map<String, String> = emptyMap(),
    val children: List<ComponentModel> = emptyList()
) {
    /**
     * Returns a copy of this component with updated properties.
     *
     * Existing properties are preserved; new properties are added or overwritten.
     */
    fun withProperties(vararg pairs: Pair<String, String>): ComponentModel {
        return copy(properties = properties + pairs)
    }

    /**
     * Returns a copy of this component with a child added.
     */
    fun withChild(child: ComponentModel): ComponentModel {
        return copy(children = children + child)
    }

    /**
     * Returns a copy of this component with updated position.
     */
    fun withPosition(newPosition: ComponentPosition): ComponentModel {
        return copy(position = newPosition)
    }

    /**
     * Returns true if this component is a container (has or can have children).
     */
    fun isContainer(): Boolean {
        return type in CONTAINER_TYPES || children.isNotEmpty()
    }

    /**
     * Recursively counts all descendant components.
     */
    fun descendantCount(): Int {
        return children.size + children.sumOf { it.descendantCount() }
    }

    /**
     * Finds the first descendant matching the given predicate.
     */
    fun findDescendant(predicate: (ComponentModel) -> Boolean): ComponentModel? {
        if (predicate(this)) return this
        for (child in children) {
            val found = child.findDescendant(predicate)
            if (found != null) return found
        }
        return null
    }

    companion object {
        /**
         * Component types that support children.
         */
        val CONTAINER_TYPES = setOf(
            "Row", "Column", "Container", "Stack", "Grid",
            "ScrollView", "Dialog", "BottomSheet", "Overlay"
        )
    }
}
