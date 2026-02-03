package com.augmentalis.avaelements.core.runtime

import com.augmentalis.avaelements.core.types.ComponentStyle
import com.augmentalis.avaelements.core.types.Color
import com.augmentalis.avaelements.core.types.Size
import com.augmentalis.avaelements.core.types.Spacing
import com.augmentalis.avaelements.core.types.Border
import com.augmentalis.avaelements.core.types.Shadow
import com.augmentalis.avaelements.core.*
import kotlinx.datetime.Clock

/**
 * Component Loader
 *
 * High-level API for loading and creating components.
 * Works with both built-in and plugin components.
 */
object ComponentLoader {

    /**
     * Load a component by type and configuration
     *
     * @param type Component type (e.g., "Button", "CustomCard")
     * @param configure Configuration lambda
     * @return Component instance
     * @throws IllegalArgumentException if type not found
     */
    suspend fun load(type: String, configure: ComponentConfigBuilder.() -> Unit = {}): Component {
        val builder = ComponentConfigBuilder(type = type)
        builder.configure()
        val config = builder.build()

        return ComponentRegistry.create(config)
    }

    /**
     * Load a component from config
     */
    suspend fun load(config: ComponentConfig): Component {
        return ComponentRegistry.create(config)
    }

    /**
     * Check if component type is available
     */
    suspend fun isAvailable(type: String): Boolean {
        return ComponentRegistry.isRegistered(type)
    }

    /**
     * Get all available component types
     */
    suspend fun getAvailableTypes(): Set<String> {
        return ComponentRegistry.getAllTypes()
    }
}

/**
 * Component configuration builder
 */
class ComponentConfigBuilder(
    private val type: String
) {
    private var id: String? = null
    private val properties = mutableMapOf<String, Any?>()
    private var style: ComponentStyle? = null
    private val modifiers = mutableListOf<Modifier>()

    /**
     * Set component ID
     */
    fun id(value: String) {
        this.id = value
    }

    /**
     * Set a property
     */
    fun property(key: String, value: Any?) {
        properties[key] = value
    }

    /**
     * Set multiple properties
     */
    fun properties(vararg pairs: Pair<String, Any?>) {
        properties.putAll(pairs)
    }

    /**
     * Set component style
     */
    fun style(builder: ComponentStyle.() -> ComponentStyle) {
        this.style = ComponentStyle().builder()
    }

    /**
     * Set component style directly
     */
    fun style(style: ComponentStyle) {
        this.style = style
    }

    /**
     * Add a modifier
     */
    fun modifier(modifier: Modifier) {
        modifiers.add(modifier)
    }

    /**
     * Add multiple modifiers
     */
    fun modifiers(vararg modifiers: Modifier) {
        this.modifiers.addAll(modifiers)
    }

    /**
     * Build configuration
     */
    fun build(): ComponentConfig {
        return ComponentConfig(
            id = id ?: generateId(type),
            type = type,
            properties = properties.toMap(),
            style = style,
            modifiers = modifiers.toList()
        )
    }

    private fun generateId(type: String): String {
        // Generate unique ID based on type and timestamp
        return "${type}_${Clock.System.now().toEpochMilliseconds()}"
    }
}

/**
 * DSL for creating components
 */
suspend inline fun <reified T : Component> component(
    type: String,
    noinline configure: ComponentConfigBuilder.() -> Unit = {}
): T {
    val component = ComponentLoader.load(type, configure)
    return component as? T
        ?: throw IllegalStateException("Component is not of expected type ${T::class.simpleName}")
}
