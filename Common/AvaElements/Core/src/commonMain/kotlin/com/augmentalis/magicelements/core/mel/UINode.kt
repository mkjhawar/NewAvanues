package com.augmentalis.magicelements.core.mel

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive

/**
 * UI tree data model for MEL plugin UI definitions
 *
 * Represents a node in the component tree with bindings to plugin state,
 * event handlers that dispatch to reducers, and static props.
 *
 * ## Examples
 *
 * ### Simple text with binding
 * ```kotlin
 * UINode(
 *     type = "Text",
 *     props = mapOf("fontSize" to JsonPrimitive(48)),
 *     bindings = mapOf("value" to "$state.display")
 * )
 * ```
 *
 * ### Button with event
 * ```kotlin
 * UINode(
 *     type = "Button",
 *     props = mapOf("label" to JsonPrimitive("7")),
 *     events = mapOf("onTap" to "appendDigit('7')")
 * )
 * ```
 *
 * ### Layout with children
 * ```kotlin
 * UINode(
 *     type = "Column",
 *     props = mapOf("spacing" to JsonPrimitive(16)),
 *     children = listOf(
 *         UINode(type = "Text", ...),
 *         UINode(type = "Button", ...)
 *     )
 * )
 * ```
 *
 * @property type Component type name (e.g., "Text", "Button", "Column")
 * @property props Static props as JSON elements
 * @property bindings Dynamic bindings (prop name -> MEL expression)
 * @property events Event handlers (event name -> reducer call)
 * @property children Child nodes for container components
 * @property id Optional unique identifier for this node
 *
 * @since 2.0.0
 */
@Serializable
data class UINode(
    val type: String,
    val props: Map<String, JsonElement> = emptyMap(),
    val bindings: Map<String, String> = emptyMap(),
    val events: Map<String, String> = emptyMap(),
    val children: List<UINode>? = null,
    val id: String? = null
) {
    /**
     * Get all prop names (both static and bound)
     */
    fun getAllPropNames(): Set<String> {
        return props.keys + bindings.keys
    }

    /**
     * Check if this node has any bindings
     */
    fun hasBindings(): Boolean {
        return bindings.isNotEmpty() || children?.any { it.hasBindings() } == true
    }

    /**
     * Check if this node has any events
     */
    fun hasEvents(): Boolean {
        return events.isNotEmpty() || children?.any { it.hasEvents() } == true
    }

    /**
     * Get all state paths referenced in bindings
     *
     * Extracts paths like "state.display", "state.count" from binding expressions
     * for dependency tracking in reactive rendering.
     */
    fun getReferencedStatePaths(): Set<String> {
        val paths = mutableSetOf<String>()

        // Extract from this node's bindings
        bindings.values.forEach { expr ->
            paths.addAll(extractStateRefs(expr))
        }

        // Recursively extract from children
        children?.forEach { child ->
            paths.addAll(child.getReferencedStatePaths())
        }

        return paths
    }

    /**
     * Extract state references from a binding expression
     *
     * Examples:
     * - "$state.display" -> ["state.display"]
     * - "$state.count + 1" -> ["state.count"]
     * - "$logic.if($state.x > 5, $state.y, $state.z)" -> ["state.x", "state.y", "state.z"]
     */
    private fun extractStateRefs(expr: String): Set<String> {
        val paths = mutableSetOf<String>()
        val stateRefRegex = Regex("""\$state\.([a-zA-Z_][a-zA-Z0-9_.]*)""")

        stateRefRegex.findAll(expr).forEach { match ->
            paths.add("state.${match.groupValues[1]}")
        }

        return paths
    }

    /**
     * Clone this node with new children
     */
    fun withChildren(newChildren: List<UINode>): UINode {
        return copy(children = newChildren)
    }

    /**
     * Clone this node with additional props
     */
    fun withProps(additionalProps: Map<String, JsonElement>): UINode {
        return copy(props = props + additionalProps)
    }

    companion object {
        /**
         * Create a simple text node
         */
        fun text(content: String, id: String? = null): UINode {
            return UINode(
                type = "Text",
                bindings = mapOf("content" to content),
                id = id
            )
        }

        /**
         * Create a simple button node
         */
        fun button(
            label: String,
            onTap: String,
            id: String? = null
        ): UINode {
            return UINode(
                type = "Button",
                props = mapOf("label" to JsonPrimitive(label)),
                events = mapOf("onTap" to onTap),
                id = id
            )
        }

        /**
         * Create a column layout node
         */
        fun column(
            children: List<UINode>,
            spacing: Int? = null,
            id: String? = null
        ): UINode {
            return UINode(
                type = "Column",
                props = spacing?.let { mapOf("spacing" to JsonPrimitive(it)) } ?: emptyMap(),
                children = children,
                id = id
            )
        }

        /**
         * Create a row layout node
         */
        fun row(
            children: List<UINode>,
            spacing: Int? = null,
            id: String? = null
        ): UINode {
            return UINode(
                type = "Row",
                props = spacing?.let { mapOf("spacing" to JsonPrimitive(it)) } ?: emptyMap(),
                children = children,
                id = id
            )
        }
    }
}
