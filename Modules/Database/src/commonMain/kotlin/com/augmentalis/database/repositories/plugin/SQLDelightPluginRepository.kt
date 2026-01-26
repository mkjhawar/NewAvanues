/**
 * SQLDelightPluginRepository.kt - SQLDelight implementation of IPluginRepository
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 */

package com.augmentalis.database.repositories.plugin

import com.augmentalis.database.VoiceOSDatabase
import com.augmentalis.database.dto.plugin.PluginDTO
import com.augmentalis.database.dto.plugin.PluginDependencyDTO
import com.augmentalis.database.dto.plugin.PluginPermissionDTO
import com.augmentalis.database.dto.plugin.SystemCheckpointDTO
import com.augmentalis.database.dto.plugin.PluginState
import com.augmentalis.database.Plugins
import com.augmentalis.database.Plugin_dependencies
import com.augmentalis.database.Plugin_permissions
import com.augmentalis.database.System_checkpoints
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * SQLDelight implementation of IPluginRepository.
 */
class SQLDelightPluginRepository(private val database: VoiceOSDatabase) : IPluginRepository {

    private val pluginQueries = database.pluginQueries
    private val dependencyQueries = database.pluginDependencyQueries
    private val permissionQueries = database.pluginPermissionQueries
    private val checkpointQueries = database.systemCheckpointQueries

    // ==================== Plugin Operations ====================

    override suspend fun getAllPlugins(): List<PluginDTO> = withContext(Dispatchers.Default) {
        pluginQueries.getAllPlugins().executeAsList().map { it.toPluginDTO() }
    }

    override suspend fun getPluginById(id: String): PluginDTO? = withContext(Dispatchers.Default) {
        pluginQueries.getPluginById(id).executeAsOneOrNull()?.toPluginDTO()
    }

    override suspend fun getPluginsByState(state: PluginState): List<PluginDTO> = withContext(Dispatchers.Default) {
        pluginQueries.getPluginsByState(state.name).executeAsList().map { it.toPluginDTO() }
    }

    override suspend fun getEnabledPlugins(): List<PluginDTO> = withContext(Dispatchers.Default) {
        pluginQueries.getEnabledPlugins().executeAsList().map { it.toPluginDTO() }
    }

    override suspend fun searchByName(name: String): List<PluginDTO> = withContext(Dispatchers.Default) {
        pluginQueries.searchByName(name).executeAsList().map { it.toPluginDTO() }
    }

    override suspend fun upsertPlugin(plugin: PluginDTO) = withContext(Dispatchers.Default) {
        pluginQueries.insertPlugin(
            id = plugin.id,
            name = plugin.name,
            version = plugin.version,
            description = plugin.description,
            author = plugin.author,
            state = plugin.state.name,
            enabled = if (plugin.enabled) 1L else 0L,
            install_path = plugin.installPath,
            installed_at = plugin.installedAt,
            updated_at = plugin.updatedAt,
            config_json = plugin.configJson
        )
    }

    override suspend fun updateState(id: String, state: PluginState, updatedAt: Long) = withContext(Dispatchers.Default) {
        pluginQueries.updateState(state.name, updatedAt, id)
    }

    override suspend fun updateEnabled(id: String, enabled: Boolean, updatedAt: Long) = withContext(Dispatchers.Default) {
        pluginQueries.updateEnabled(if (enabled) 1L else 0L, updatedAt, id)
    }

    override suspend fun deletePlugin(id: String) = withContext(Dispatchers.Default) {
        pluginQueries.deletePlugin(id)
    }

    override suspend fun countPlugins(): Long = withContext(Dispatchers.Default) {
        pluginQueries.countPlugins().executeAsOne()
    }

    override suspend fun countEnabledPlugins(): Long = withContext(Dispatchers.Default) {
        pluginQueries.countEnabledPlugins().executeAsOne()
    }

    // ==================== Dependency Operations ====================

    override suspend fun getAllDependencies(): List<PluginDependencyDTO> = withContext(Dispatchers.Default) {
        dependencyQueries.getAllDependencies().executeAsList().map { it.toDependencyDTO() }
    }

    override suspend fun getDependencies(pluginId: String): List<PluginDependencyDTO> = withContext(Dispatchers.Default) {
        dependencyQueries.getDependencies(pluginId).executeAsList().map { it.toDependencyDTO() }
    }

    override suspend fun getDependents(pluginId: String): List<PluginDependencyDTO> = withContext(Dispatchers.Default) {
        dependencyQueries.getDependents(pluginId).executeAsList().map { it.toDependencyDTO() }
    }

    override suspend fun getRequiredDependencies(pluginId: String): List<PluginDependencyDTO> = withContext(Dispatchers.Default) {
        dependencyQueries.getRequiredDependencies(pluginId).executeAsList().map { it.toDependencyDTO() }
    }

    override suspend fun insertDependency(dependency: PluginDependencyDTO) = withContext(Dispatchers.Default) {
        dependencyQueries.insertDependency(
            plugin_id = dependency.pluginId,
            depends_on_plugin_id = dependency.dependsOnPluginId,
            version_constraint = dependency.versionConstraint,
            is_optional = if (dependency.isOptional) 1L else 0L
        )
    }

    override suspend fun deleteDependency(id: Long) = withContext(Dispatchers.Default) {
        dependencyQueries.deleteDependency(id)
    }

    override suspend fun deleteDependenciesForPlugin(pluginId: String) = withContext(Dispatchers.Default) {
        dependencyQueries.deleteDependenciesForPlugin(pluginId)
    }

    override suspend fun countDependencies(pluginId: String): Long = withContext(Dispatchers.Default) {
        dependencyQueries.countDependencies(pluginId).executeAsOne()
    }

    override suspend fun countDependents(pluginId: String): Long = withContext(Dispatchers.Default) {
        dependencyQueries.countDependents(pluginId).executeAsOne()
    }

    // ==================== Permission Operations ====================

    override suspend fun getAllPermissions(): List<PluginPermissionDTO> = withContext(Dispatchers.Default) {
        permissionQueries.getAllPermissions().executeAsList().map { it.toPermissionDTO() }
    }

    override suspend fun getPermissions(pluginId: String): List<PluginPermissionDTO> = withContext(Dispatchers.Default) {
        permissionQueries.getPermissions(pluginId).executeAsList().map { it.toPermissionDTO() }
    }

    override suspend fun getPermission(pluginId: String, permission: String): PluginPermissionDTO? = withContext(Dispatchers.Default) {
        permissionQueries.getPermission(pluginId, permission).executeAsOneOrNull()?.toPermissionDTO()
    }

    override suspend fun getGrantedPermissions(pluginId: String): List<PluginPermissionDTO> = withContext(Dispatchers.Default) {
        permissionQueries.getGrantedPermissions(pluginId).executeAsList().map { it.toPermissionDTO() }
    }

    override suspend fun insertPermission(permission: PluginPermissionDTO) = withContext(Dispatchers.Default) {
        permissionQueries.insertPermission(
            plugin_id = permission.pluginId,
            permission = permission.permission,
            granted = if (permission.granted) 1L else 0L,
            granted_at = permission.grantedAt,
            granted_by = permission.grantedBy
        )
    }

    override suspend fun grantPermission(pluginId: String, permission: String, grantedAt: Long, grantedBy: String) = withContext(Dispatchers.Default) {
        permissionQueries.grantPermission(grantedAt, grantedBy, pluginId, permission)
    }

    override suspend fun revokePermission(pluginId: String, permission: String) = withContext(Dispatchers.Default) {
        permissionQueries.revokePermission(pluginId, permission)
    }

    override suspend fun deletePermission(id: Long) = withContext(Dispatchers.Default) {
        permissionQueries.deletePermission(id)
    }

    override suspend fun deletePermissionsForPlugin(pluginId: String) = withContext(Dispatchers.Default) {
        permissionQueries.deletePermissionsForPlugin(pluginId)
    }

    override suspend fun countPermissions(pluginId: String): Long = withContext(Dispatchers.Default) {
        permissionQueries.countPermissions(pluginId).executeAsOne()
    }

    override suspend fun countGrantedPermissions(pluginId: String): Long = withContext(Dispatchers.Default) {
        permissionQueries.countGrantedPermissions(pluginId).executeAsOne()
    }

    // ==================== Checkpoint Operations ====================

    override suspend fun getAllCheckpoints(): List<SystemCheckpointDTO> = withContext(Dispatchers.Default) {
        checkpointQueries.getAllCheckpoints().executeAsList().map { it.toCheckpointDTO() }
    }

    override suspend fun getLatestCheckpoint(): SystemCheckpointDTO? = withContext(Dispatchers.Default) {
        checkpointQueries.getLatestCheckpoint().executeAsOneOrNull()?.toCheckpointDTO()
    }

    override suspend fun getCheckpointById(id: String): SystemCheckpointDTO? = withContext(Dispatchers.Default) {
        checkpointQueries.getCheckpointById(id).executeAsOneOrNull()?.toCheckpointDTO()
    }

    override suspend fun getCheckpointsByName(name: String): List<SystemCheckpointDTO> = withContext(Dispatchers.Default) {
        checkpointQueries.getCheckpointsByName(name).executeAsList().map { it.toCheckpointDTO() }
    }

    override suspend fun getRecentCheckpoints(limit: Long): List<SystemCheckpointDTO> = withContext(Dispatchers.Default) {
        checkpointQueries.getRecentCheckpoints(limit).executeAsList().map { it.toCheckpointDTO() }
    }

    override suspend fun insertCheckpoint(checkpoint: SystemCheckpointDTO) = withContext(Dispatchers.Default) {
        checkpointQueries.insertCheckpoint(
            id = checkpoint.id,
            name = checkpoint.name,
            description = checkpoint.description,
            created_at = checkpoint.createdAt,
            state_json = checkpoint.stateJson,
            plugin_states_json = checkpoint.pluginStatesJson
        )
    }

    override suspend fun updateCheckpoint(checkpoint: SystemCheckpointDTO) = withContext(Dispatchers.Default) {
        checkpointQueries.updateCheckpoint(
            name = checkpoint.name,
            description = checkpoint.description,
            state_json = checkpoint.stateJson,
            plugin_states_json = checkpoint.pluginStatesJson,
            id = checkpoint.id
        )
    }

    override suspend fun deleteCheckpoint(id: String) = withContext(Dispatchers.Default) {
        checkpointQueries.deleteCheckpoint(id)
    }

    override suspend fun deleteOldCheckpoints(timestamp: Long) = withContext(Dispatchers.Default) {
        checkpointQueries.deleteOldCheckpoints(timestamp)
    }

    override suspend fun countCheckpoints(): Long = withContext(Dispatchers.Default) {
        checkpointQueries.countCheckpoints().executeAsOne()
    }

    // ==================== Extension Functions ====================

    private fun Plugins.toPluginDTO() = PluginDTO(
        id = id,
        name = name,
        version = version,
        description = description,
        author = author,
        state = PluginState.valueOf(state),
        enabled = enabled == 1L,
        installPath = install_path,
        installedAt = installed_at,
        updatedAt = updated_at,
        configJson = config_json
    )

    private fun Plugin_dependencies.toDependencyDTO() = PluginDependencyDTO(
        id = id,
        pluginId = plugin_id,
        dependsOnPluginId = depends_on_plugin_id,
        versionConstraint = version_constraint,
        isOptional = is_optional == 1L
    )

    private fun Plugin_permissions.toPermissionDTO() = PluginPermissionDTO(
        id = id,
        pluginId = plugin_id,
        permission = permission,
        granted = granted == 1L,
        grantedAt = granted_at,
        grantedBy = granted_by
    )

    private fun System_checkpoints.toCheckpointDTO() = SystemCheckpointDTO(
        id = id,
        name = name,
        description = description,
        createdAt = created_at,
        stateJson = state_json,
        pluginStatesJson = plugin_states_json
    )
}
