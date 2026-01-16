/**
 * ComponentLoader.kt - Interface and implementations for loading component YAML
 *
 * Defines the abstraction for loading component definitions from various sources
 * (files, resources, memory) and provides an in-memory implementation for testing.
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Code-Reviewed-By: CCA
 * Created: 2026-01-06
 * Refactored: 2026-01-08 (SRP extraction from ComponentFactory.kt)
 */
package com.augmentalis.voiceoscoreng.common

/**
 * Interface for loading component YAML from various sources.
 */
interface IComponentLoader {

    /**
     * Load component YAML by name.
     *
     * @param name Component name (e.g., "ElementOverlay")
     * @return YAML content or null if not found
     */
    fun load(name: String): String?

    /**
     * Check if component exists.
     */
    fun exists(name: String): Boolean

    /**
     * List all available component names.
     */
    fun listComponents(): List<String>
}

/**
 * In-memory component loader for testing/embedded components.
 */
class InMemoryComponentLoader : IComponentLoader {

    private val components = mutableMapOf<String, String>()

    /**
     * Register a component.
     */
    fun register(name: String, yaml: String) {
        components[name] = yaml
    }

    /**
     * Register multiple components.
     */
    fun registerAll(components: Map<String, String>) {
        this.components.putAll(components)
    }

    override fun load(name: String): String? {
        return components[name]
    }

    override fun exists(name: String): Boolean {
        return components.containsKey(name)
    }

    override fun listComponents(): List<String> {
        return components.keys.toList()
    }
}
