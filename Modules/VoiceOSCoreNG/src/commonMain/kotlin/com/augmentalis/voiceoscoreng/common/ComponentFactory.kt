/**
 * ComponentFactory.kt - Factory for creating components from YAML definitions
 *
 * Main facade for parsing, caching, and validating YAML component definitions.
 * Delegates to specialized classes for each responsibility.
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Code-Reviewed-By: CCA
 * Created: 2026-01-06
 * Refactored: 2026-01-08 (SRP - split into focused files)
 *
 * Related files:
 * - YamlComponentParser.kt: YAML parsing logic
 * - ComponentValidator.kt: Validation logic
 * - ComponentLoader.kt: Loading abstraction
 * - BuiltInComponents.kt: Pre-built widget definitions
 */
package com.augmentalis.voiceoscoreng.common

/**
 * Factory for creating ComponentDefinition from YAML.
 *
 * Uses a simple map-based YAML parser (KMP-compatible).
 * For full YAML parsing, integrate kaml or similar library.
 *
 * ## Usage
 *
 * ```kotlin
 * // Parse from YAML content
 * val definition = ComponentFactory.parse(yamlContent)
 *
 * // Load and cache
 * val factory = ComponentFactory()
 * val cached = factory.loadOrCache("ElementOverlay", yamlContent)
 * ```
 */
class ComponentFactory {

    private val cache = mutableMapOf<String, ComponentDefinition>()
    private val parser = YamlComponentParser()

    /**
     * Parse YAML content into ComponentDefinition.
     *
     * @param yaml YAML string content
     * @return Parsed ComponentDefinition
     * @throws ComponentParseException if parsing fails
     */
    fun parse(yaml: String): ComponentDefinition = parser.parse(yaml)

    /**
     * Parse YAML and cache with given name.
     *
     * @param name Cache key (usually component name)
     * @param yaml YAML content
     * @return Cached or newly parsed ComponentDefinition
     */
    fun loadOrCache(name: String, yaml: String): ComponentDefinition =
        cache.getOrPut(name) { parse(yaml) }

    /**
     * Get cached component by name.
     */
    fun getCached(name: String): ComponentDefinition? = cache[name]

    /**
     * Clear component cache.
     */
    fun clearCache() {
        cache.clear()
    }

    /**
     * Validate a component definition.
     *
     * @param definition Component to validate
     * @return Validation result with errors
     */
    fun validate(definition: ComponentDefinition): ValidationResult =
        ComponentValidator.validate(definition)

    companion object {
        /** Shared instance */
        val shared = ComponentFactory()

        /**
         * Quick parse without caching.
         */
        fun parse(yaml: String): ComponentDefinition = shared.parse(yaml)
    }
}

/**
 * Exception thrown when component parsing fails.
 */
class ComponentParseException(
    message: String,
    cause: Throwable? = null
) : Exception(message, cause)
