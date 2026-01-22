package com.augmentalis.avamagic.layout

import kotlinx.serialization.Serializable

/**
 * Complete layout configuration for the AvaUI system.
 *
 * A layout defines the hierarchical structure of UI components, including:
 * - **Metadata**: Name, version, description
 * - **Root Components**: Top-level UI elements
 * - **Nesting**: Parent-child relationships via component hierarchy
 *
 * ## Example Usage
 *
 * ```kotlin
 * val mainLayout = LayoutConfig(
 *     name = "MainScreen",
 *     version = "1.0.0",
 *     description = "Main application screen",
 *     components = listOf(
 *         LayoutComponent(
 *             id = "header",
 *             type = ComponentType.ROW,
 *             properties = mapOf("align" to "center"),
 *             children = listOf(
 *                 LayoutComponent(
 *                     id = "title",
 *                     type = ComponentType.TEXT,
 *                     properties = mapOf("text" to "Welcome", "style" to "h1")
 *                 )
 *             )
 *         )
 *     )
 * )
 * ```
 *
 * @property name Layout identifier name
 * @property version Semantic version string (e.g., "1.0.0")
 * @property description Optional human-readable description
 * @property components Root-level components in the layout
 *
 * @since 3.2.0
 */
@Serializable
data class LayoutConfig(
    val name: String,
    val version: String = "1.0.0",
    val description: String = "",
    val components: List<LayoutComponent> = emptyList()
) {
    /**
     * Returns the total count of all components (including nested).
     */
    fun totalComponentCount(): Int {
        return components.sumOf { it.totalCount() }
    }

    /**
     * Finds a component by ID across the entire layout hierarchy.
     *
     * @param id The component ID to find
     * @return The component if found, null otherwise
     */
    fun findById(id: String): LayoutComponent? {
        return components.firstNotNullOfOrNull { it.findById(id) }
    }
}

/**
 * A single UI component within a layout.
 *
 * Components form a tree structure where container types (Row, Column) can
 * have children, and leaf types (Text, Button, Image) cannot.
 *
 * @property id Unique identifier within the layout
 * @property type Component type (Row, Column, Text, Button, etc.)
 * @property properties Key-value pairs for component configuration
 * @property children Nested child components (for container types)
 *
 * @since 3.2.0
 */
@Serializable
data class LayoutComponent(
    val id: String,
    val type: ComponentType,
    val properties: Map<String, String> = emptyMap(),
    val children: List<LayoutComponent> = emptyList()
) {
    /**
     * Returns total count of this component plus all descendants.
     */
    fun totalCount(): Int {
        return 1 + children.sumOf { it.totalCount() }
    }

    /**
     * Finds a component by ID in this subtree.
     */
    fun findById(id: String): LayoutComponent? {
        if (this.id == id) return this
        return children.firstNotNullOfOrNull { it.findById(id) }
    }

    /**
     * Returns true if this is a container type that can have children.
     */
    fun isContainer(): Boolean = type.isContainer

    /**
     * Returns a copy with an additional child component.
     */
    fun withChild(child: LayoutComponent): LayoutComponent {
        return copy(children = children + child)
    }

    /**
     * Returns a copy with updated properties.
     */
    fun withProperties(vararg pairs: Pair<String, String>): LayoutComponent {
        return copy(properties = properties + pairs)
    }
}

/**
 * Supported component types in the AMF layout format.
 *
 * Each type maps to a specific record prefix in AMF format:
 * - `COL:` -> COLUMN
 * - `ROW:` -> ROW
 * - `TXT:` -> TEXT
 * - `BTN:` -> BUTTON
 * - `IMG:` -> IMAGE
 * - `SPC:` -> SPACER
 *
 * @property prefix The AMF record prefix for this type
 * @property isContainer Whether this type can contain children
 *
 * @since 3.2.0
 */
@Serializable
enum class ComponentType(val prefix: String, val isContainer: Boolean) {
    /** Column container - vertical arrangement */
    COLUMN("COL", true),

    /** Row container - horizontal arrangement */
    ROW("ROW", true),

    /** Text display component */
    TEXT("TXT", false),

    /** Button component with action */
    BUTTON("BTN", false),

    /** Image display component */
    IMAGE("IMG", false),

    /** Spacer for layout spacing */
    SPACER("SPC", false),

    /** Generic container for custom layouts */
    CONTAINER("CNT", true),

    /** Stack container for overlapping elements */
    STACK("STK", true),

    /** Scroll view container */
    SCROLL("SCR", true),

    /** Grid container for grid layouts */
    GRID("GRD", true);

    companion object {
        private val prefixMap = entries.associateBy { it.prefix }

        /**
         * Parse component type from AMF prefix.
         *
         * @param prefix The AMF prefix (e.g., "COL", "ROW", "TXT")
         * @return The component type or null if not found
         */
        fun fromPrefix(prefix: String): ComponentType? = prefixMap[prefix]

        /**
         * Parse component type from AMF prefix, throwing if not found.
         *
         * @param prefix The AMF prefix
         * @return The component type
         * @throws IllegalArgumentException if prefix is unknown
         */
        fun fromPrefixOrThrow(prefix: String): ComponentType {
            return fromPrefix(prefix)
                ?: throw IllegalArgumentException("Unknown component type prefix: $prefix")
        }
    }
}

/**
 * Alignment options for layout components.
 *
 * Used in Row and Column components to control child positioning.
 *
 * @since 3.2.0
 */
@Serializable
enum class LayoutAlignment(val value: String) {
    /** Align to start (left for Row, top for Column) */
    START("start"),

    /** Align to center */
    CENTER("center"),

    /** Align to end (right for Row, bottom for Column) */
    END("end"),

    /** Stretch to fill available space */
    STRETCH("stretch"),

    /** Space children evenly with equal gaps */
    SPACE_BETWEEN("space-between"),

    /** Space children evenly with equal space around */
    SPACE_AROUND("space-around");

    companion object {
        private val valueMap = entries.associateBy { it.value }

        /**
         * Parse alignment from string value.
         *
         * @param value The alignment value (e.g., "center", "start")
         * @return The alignment or null if not found
         */
        fun fromValue(value: String): LayoutAlignment? = valueMap[value.lowercase()]

        /**
         * Parse alignment with default fallback.
         *
         * @param value The alignment value
         * @param default Default alignment if not found
         * @return The parsed or default alignment
         */
        fun fromValueOrDefault(value: String?, default: LayoutAlignment = START): LayoutAlignment {
            return value?.let { fromValue(it) } ?: default
        }
    }
}

/**
 * Scale mode for image components.
 *
 * @since 3.2.0
 */
@Serializable
enum class ImageScale(val value: String) {
    /** Scale to fit within bounds, maintaining aspect ratio */
    FIT("fit"),

    /** Scale to fill bounds, cropping if necessary */
    FILL("fill"),

    /** No scaling, display at original size */
    NONE("none"),

    /** Scale to fit width, maintaining aspect ratio */
    FIT_WIDTH("fit-width"),

    /** Scale to fit height, maintaining aspect ratio */
    FIT_HEIGHT("fit-height");

    companion object {
        private val valueMap = entries.associateBy { it.value }

        /**
         * Parse scale from string value.
         */
        fun fromValue(value: String): ImageScale? = valueMap[value.lowercase()]

        /**
         * Parse scale with default fallback.
         */
        fun fromValueOrDefault(value: String?, default: ImageScale = FIT): ImageScale {
            return value?.let { fromValue(it) } ?: default
        }
    }
}
