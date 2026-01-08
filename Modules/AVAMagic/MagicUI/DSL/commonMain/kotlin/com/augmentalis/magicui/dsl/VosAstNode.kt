package com.augmentalis.magicui.dsl

import kotlinx.serialization.Serializable

/**
 * Abstract Syntax Tree (AST) node types for VoiceOS DSL (.vos files).
 *
 * This sealed class hierarchy represents the parsed structure of VoiceOS
 * application definitions. Each node type corresponds to a DSL construct:
 * - App: Top-level application definition
 * - Component: UI component with properties, children, and callbacks
 *
 * Example DSL structure:
 * ```
 * app "MyApp" {
 *     ColorPicker {
 *         id = "picker1"
 *         onColorChange = { color ->
 *             VoiceOS.speak("Color selected")
 *         }
 *     }
 * }
 * ```
 *
 * Created by Manoj Jhawar, manoj@ideahq.net
 * Date: 2025-10-27 12:12:37 PDT
 *
 * @see VosValue for property value types
 * @see VosLambda for callback/event handler representation
 */
sealed class VosAstNode {

    /**
     * Represents a VoiceOS application definition.
     *
     * The App node is the root of the AST and contains all application metadata,
     * component hierarchy, voice commands, and global properties.
     *
     * @property id Unique application identifier (e.g., "com.example.myapp")
     * @property name Human-readable application name
     * @property runtime Target runtime environment (default: "AvaUI")
     * @property components List of top-level UI components
     * @property voiceCommands Map of voice command phrases to handler names
     * @property properties Global application properties (theme, config, etc.)
     */
    @Serializable
    data class App(
        val id: String,
        val name: String,
        val runtime: String = "AvaUI",
        val components: List<Component>,
        val voiceCommands: Map<String, String> = emptyMap(),
        val properties: Map<String, VosValue> = emptyMap()
    ) : VosAstNode() {

        /**
         * Returns a human-readable string representation of this App node.
         * Useful for debugging and logging AST structure.
         */
        override fun toString(): String {
            return buildString {
                append("App(id='$id', name='$name', runtime='$runtime', ")
                append("components=${components.size}, ")
                append("voiceCommands=${voiceCommands.size}, ")
                append("properties=${properties.size})")
            }
        }

        companion object {
            /**
             * Creates a minimal App node with required fields only.
             * Useful for testing and default app creation.
             *
             * @param id Application identifier
             * @param name Application name
             * @param components Initial component list (empty by default)
             * @return App node with default runtime and no voice commands
             */
            fun minimal(
                id: String,
                name: String,
                components: List<Component> = emptyList()
            ): App = App(
                id = id,
                name = name,
                components = components
            )

            /**
             * Creates an App node with voice commands enabled.
             * Convenience factory for voice-first applications.
             *
             * @param id Application identifier
             * @param name Application name
             * @param voiceCommands Map of phrases to handler names
             * @param components Initial component list (empty by default)
             * @return App node configured for voice interaction
             */
            fun withVoiceCommands(
                id: String,
                name: String,
                voiceCommands: Map<String, String>,
                components: List<Component> = emptyList()
            ): App = App(
                id = id,
                name = name,
                voiceCommands = voiceCommands,
                components = components
            )
        }
    }

    /**
     * Represents a UI component in the VoiceOS DSL.
     *
     * Components are the building blocks of VoiceOS applications. Each component
     * has a type (e.g., "ColorPicker", "Notepad"), optional properties, child
     * components, and event callbacks.
     *
     * Example DSL:
     * ```
     * ColorPicker {
     *     id = "mainPicker"
     *     initialColor = "#FF5733"
     *     onColorChange = { color -> /* ... */ }
     *
     *     Label {
     *         text = "Select a color"
     *     }
     * }
     * ```
     *
     * @property type Component type identifier (maps to AvaUI component)
     * @property id Optional unique identifier for this component instance
     * @property properties Key-value map of component properties
     * @property children Nested child components (for container components)
     * @property callbacks Event handlers/callbacks (e.g., onClick, onChange)
     */
    @Serializable
    data class Component(
        val type: String,
        val id: String? = null,
        val properties: Map<String, VosValue> = emptyMap(),
        val children: List<Component> = emptyList(),
        val callbacks: Map<String, VosLambda> = emptyMap()
    ) : VosAstNode() {

        /**
         * Returns a human-readable string representation of this Component node.
         * Shows component hierarchy and configuration summary.
         */
        override fun toString(): String {
            return buildString {
                append("Component(type='$type'")
                if (id != null) append(", id='$id'")
                append(", properties=${properties.size}")
                append(", children=${children.size}")
                append(", callbacks=${callbacks.size})")
            }
        }

        /**
         * Recursively collects all descendant components (children, grandchildren, etc.).
         * Useful for AST traversal and component lookup.
         *
         * @return Flat list of all nested components
         */
        fun allDescendants(): List<Component> {
            return children + children.flatMap { it.allDescendants() }
        }

        /**
         * Finds the first descendant component with the specified ID.
         * Performs depth-first search through the component tree.
         *
         * @param targetId ID to search for
         * @return Component with matching ID, or null if not found
         */
        fun findById(targetId: String): Component? {
            if (id == targetId) return this
            return children.firstNotNullOfOrNull { it.findById(targetId) }
        }

        companion object {
            /**
             * Creates a leaf component (no children) with properties only.
             * Common pattern for simple UI elements like labels, buttons.
             *
             * @param type Component type identifier
             * @param id Optional component ID
             * @param properties Component properties
             * @return Component with no children or callbacks
             */
            fun leaf(
                type: String,
                id: String? = null,
                properties: Map<String, VosValue> = emptyMap()
            ): Component = Component(
                type = type,
                id = id,
                properties = properties
            )

            /**
             * Creates a container component with children.
             * Common pattern for layout containers like Column, Row, Stack.
             *
             * @param type Component type identifier
             * @param id Optional component ID
             * @param children Child components
             * @param properties Container properties (layout, spacing, etc.)
             * @return Component configured as a container
             */
            fun container(
                type: String,
                id: String? = null,
                children: List<Component>,
                properties: Map<String, VosValue> = emptyMap()
            ): Component = Component(
                type = type,
                id = id,
                properties = properties,
                children = children
            )

            /**
             * Creates an interactive component with callbacks.
             * Common pattern for input elements like buttons, pickers, forms.
             *
             * @param type Component type identifier
             * @param id Optional component ID
             * @param callbacks Event handler map
             * @param properties Component properties
             * @return Component configured with event handling
             */
            fun interactive(
                type: String,
                id: String? = null,
                callbacks: Map<String, VosLambda>,
                properties: Map<String, VosValue> = emptyMap()
            ): Component = Component(
                type = type,
                id = id,
                properties = properties,
                callbacks = callbacks
            )
        }
    }
}
