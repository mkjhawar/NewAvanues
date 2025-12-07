/**
 * IPluginRepository.kt - Repository interface for Plugin operations
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 */

package com.augmentalis.database.repositories.plugin

import com.augmentalis.database.dto.plugin.PluginDTO
import com.augmentalis.database.dto.plugin.PluginDependencyDTO
import com.augmentalis.database.dto.plugin.PluginPermissionDTO
import com.augmentalis.database.dto.plugin.SystemCheckpointDTO
import com.augmentalis.database.dto.plugin.PluginState

/**
 * Repository interface for plugin operations.
 * Provides methods for managing plugins, dependencies, permissions, and checkpoints.
 */
interface IPluginRepository {

    // ==================== Plugin Operations ====================

    /**
     * Get all plugins.
     */
    suspend fun getAllPlugins(): List<PluginDTO>

    /**
     * Get plugin by ID.
     */
    suspend fun getPluginById(id: String): PluginDTO?

    /**
     * Get plugins by state.
     */
    suspend fun getPluginsByState(state: PluginState): List<PluginDTO>

    /**
     * Get enabled plugins.
     */
    suspend fun getEnabledPlugins(): List<PluginDTO>

    /**
     * Search plugins by name.
     */
    suspend fun searchByName(name: String): List<PluginDTO>

    /**
     * Insert or update plugin.
     */
    suspend fun upsertPlugin(plugin: PluginDTO)

    /**
     * Update plugin state.
     */
    suspend fun updateState(id: String, state: PluginState, updatedAt: Long)

    /**
     * Update plugin enabled status.
     */
    suspend fun updateEnabled(id: String, enabled: Boolean, updatedAt: Long)

    /**
     * Delete plugin.
     */
    suspend fun deletePlugin(id: String)

    /**
     * Count all plugins.
     */
    suspend fun countPlugins(): Long

    /**
     * Count enabled plugins.
     */
    suspend fun countEnabledPlugins(): Long

    // ==================== Dependency Operations ====================

    /**
     * Get all dependencies.
     */
    suspend fun getAllDependencies(): List<PluginDependencyDTO>

    /**
     * Get dependencies for a plugin.
     */
    suspend fun getDependencies(pluginId: String): List<PluginDependencyDTO>

    /**
     * Get dependents of a plugin (who depends on this plugin).
     */
    suspend fun getDependents(pluginId: String): List<PluginDependencyDTO>

    /**
     * Get required dependencies (non-optional).
     */
    suspend fun getRequiredDependencies(pluginId: String): List<PluginDependencyDTO>

    /**
     * Insert dependency.
     */
    suspend fun insertDependency(dependency: PluginDependencyDTO)

    /**
     * Delete dependency.
     */
    suspend fun deleteDependency(id: Long)

    /**
     * Delete all dependencies for a plugin.
     */
    suspend fun deleteDependenciesForPlugin(pluginId: String)

    /**
     * Count dependencies for a plugin.
     */
    suspend fun countDependencies(pluginId: String): Long

    /**
     * Count dependents of a plugin.
     */
    suspend fun countDependents(pluginId: String): Long

    // ==================== Permission Operations ====================

    /**
     * Get all permissions.
     */
    suspend fun getAllPermissions(): List<PluginPermissionDTO>

    /**
     * Get permissions for a plugin.
     */
    suspend fun getPermissions(pluginId: String): List<PluginPermissionDTO>

    /**
     * Get specific permission.
     */
    suspend fun getPermission(pluginId: String, permission: String): PluginPermissionDTO?

    /**
     * Get granted permissions for a plugin.
     */
    suspend fun getGrantedPermissions(pluginId: String): List<PluginPermissionDTO>

    /**
     * Insert permission.
     */
    suspend fun insertPermission(permission: PluginPermissionDTO)

    /**
     * Grant permission.
     */
    suspend fun grantPermission(pluginId: String, permission: String, grantedAt: Long, grantedBy: String)

    /**
     * Revoke permission.
     */
    suspend fun revokePermission(pluginId: String, permission: String)

    /**
     * Delete permission.
     */
    suspend fun deletePermission(id: Long)

    /**
     * Delete all permissions for a plugin.
     */
    suspend fun deletePermissionsForPlugin(pluginId: String)

    /**
     * Count permissions for a plugin.
     */
    suspend fun countPermissions(pluginId: String): Long

    /**
     * Count granted permissions for a plugin.
     */
    suspend fun countGrantedPermissions(pluginId: String): Long

    // ==================== Checkpoint Operations ====================

    /**
     * Get all checkpoints.
     */
    suspend fun getAllCheckpoints(): List<SystemCheckpointDTO>

    /**
     * Get latest checkpoint.
     */
    suspend fun getLatestCheckpoint(): SystemCheckpointDTO?

    /**
     * Get checkpoint by ID.
     */
    suspend fun getCheckpointById(id: String): SystemCheckpointDTO?

    /**
     * Get checkpoints by name.
     */
    suspend fun getCheckpointsByName(name: String): List<SystemCheckpointDTO>

    /**
     * Get recent checkpoints.
     */
    suspend fun getRecentCheckpoints(limit: Long): List<SystemCheckpointDTO>

    /**
     * Insert checkpoint.
     */
    suspend fun insertCheckpoint(checkpoint: SystemCheckpointDTO)

    /**
     * Update checkpoint.
     */
    suspend fun updateCheckpoint(checkpoint: SystemCheckpointDTO)

    /**
     * Delete checkpoint.
     */
    suspend fun deleteCheckpoint(id: String)

    /**
     * Delete old checkpoints.
     */
    suspend fun deleteOldCheckpoints(timestamp: Long)

    /**
     * Count checkpoints.
     */
    suspend fun countCheckpoints(): Long
}
