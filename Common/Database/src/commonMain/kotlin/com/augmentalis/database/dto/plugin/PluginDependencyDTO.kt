/**
 * PluginDependencyDTO.kt - Data Transfer Object for Plugin Dependency
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 */

package com.avanues.database.dto.plugin

/**
 * Data Transfer Object for PluginDependency entity.
 * Represents inter-plugin dependencies for cross-platform use.
 */
data class PluginDependencyDTO(
    val id: Long?,
    val pluginId: String,
    val dependsOnPluginId: String,
    val versionConstraint: String?,
    val isOptional: Boolean
)
