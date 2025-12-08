package com.augmentalis.universal.thememanager

import com.augmentalis.avaelements.core.Theme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Theme synchronization system
 *
 * Manages synchronization between local and cloud theme storage with:
 * - Bidirectional sync (local <-> cloud)
 * - Conflict resolution (last-write-wins)
 * - Version tracking
 * - Sync state monitoring
 *
 * Usage:
 * ```kotlin
 * val sync = ThemeSync(
 *     localRepository = LocalThemeRepository(),
 *     cloudRepository = CloudThemeRepository(provider)
 * )
 *
 * // Sync from cloud to local
 * sync.syncFromCloud()
 *
 * // Sync from local to cloud
 * sync.syncToCloud()
 *
 * // Observe sync state
 * sync.syncState.collect { state ->
 *     when (state) {
 *         is SyncState.Syncing -> showProgress()
 *         is SyncState.Success -> hideProgress()
 *         is SyncState.Error -> showError(state.message)
 *     }
 * }
 * ```
 */
class ThemeSync(
    private val localRepository: ThemeRepository,
    private val cloudRepository: ThemeRepository,
    private val conflictResolver: ConflictResolver = LastWriteWinsResolver()
) {

    // ==================== State ====================

    private val _syncState = MutableStateFlow<SyncState>(SyncState.Idle)
    val syncState: StateFlow<SyncState> = _syncState.asStateFlow()

    private val _lastSyncTime = MutableStateFlow<Long?>(null)
    val lastSyncTime: StateFlow<Long?> = _lastSyncTime.asStateFlow()

    // ==================== Synchronization ====================

    /**
     * Sync themes from cloud to local storage
     * Merges cloud themes with local themes using conflict resolution
     */
    suspend fun syncFromCloud() {
        _syncState.value = SyncState.Syncing(direction = SyncDirection.FromCloud)

        try {
            // Load cloud universal theme
            val cloudUniversalTheme = cloudRepository.loadUniversalTheme()
            val localUniversalTheme = localRepository.loadUniversalTheme()

            // Resolve universal theme conflict
            if (cloudUniversalTheme != null) {
                val resolvedTheme = if (localUniversalTheme != null) {
                    conflictResolver.resolveUniversalTheme(
                        local = localUniversalTheme,
                        cloud = cloudUniversalTheme
                    )
                } else {
                    cloudUniversalTheme
                }
                localRepository.saveUniversalTheme(resolvedTheme)
            }

            // Load all cloud app themes
            val cloudAppThemes = cloudRepository.loadAllAppThemes()
            val localAppThemes = localRepository.loadAllAppThemes()

            // Sync each app theme
            cloudAppThemes.forEach { (appId, cloudTheme) ->
                val localTheme = localAppThemes[appId]
                val resolvedTheme = if (localTheme != null) {
                    conflictResolver.resolveAppTheme(
                        appId = appId,
                        local = localTheme,
                        cloud = cloudTheme
                    )
                } else {
                    cloudTheme
                }
                localRepository.saveAppTheme(appId, resolvedTheme)

                // Sync override configuration
                val cloudOverride = cloudRepository.loadAppOverride(appId)
                val localOverride = localRepository.loadAppOverride(appId)
                if (cloudOverride != null) {
                    val resolvedOverride = if (localOverride != null) {
                        conflictResolver.resolveAppOverride(
                            appId = appId,
                            local = localOverride,
                            cloud = cloudOverride
                        )
                    } else {
                        cloudOverride
                    }
                    localRepository.saveAppOverride(appId, resolvedOverride)
                }
            }

            _lastSyncTime.value = System.currentTimeMillis()
            _syncState.value = SyncState.Success
        } catch (e: Exception) {
            _syncState.value = SyncState.Error("Failed to sync from cloud: ${e.message}")
        }
    }

    /**
     * Sync themes from local to cloud storage
     * Uploads local themes to cloud
     */
    suspend fun syncToCloud() {
        _syncState.value = SyncState.Syncing(direction = SyncDirection.ToCloud)

        try {
            // Upload universal theme
            localRepository.loadUniversalTheme()?.let { theme ->
                cloudRepository.saveUniversalTheme(theme)
            }

            // Upload all app themes
            val localAppThemes = localRepository.loadAllAppThemes()
            localAppThemes.forEach { (appId, theme) ->
                cloudRepository.saveAppTheme(appId, theme)

                // Upload override configuration
                localRepository.loadAppOverride(appId)?.let { override ->
                    cloudRepository.saveAppOverride(appId, override)
                }
            }

            _lastSyncTime.value = System.currentTimeMillis()
            _syncState.value = SyncState.Success
        } catch (e: Exception) {
            _syncState.value = SyncState.Error("Failed to sync to cloud: ${e.message}")
        }
    }

    /**
     * Perform bidirectional sync
     * Merges local and cloud themes intelligently
     */
    suspend fun sync() {
        _syncState.value = SyncState.Syncing(direction = SyncDirection.Bidirectional)

        try {
            // First, sync from cloud to get latest remote changes
            syncFromCloud()

            // Then, sync to cloud to upload any local changes
            syncToCloud()

            _lastSyncTime.value = System.currentTimeMillis()
            _syncState.value = SyncState.Success
        } catch (e: Exception) {
            _syncState.value = SyncState.Error("Failed to perform bidirectional sync: ${e.message}")
        }
    }

    /**
     * Check if sync is needed based on time elapsed
     */
    fun shouldSync(intervalMillis: Long = DEFAULT_SYNC_INTERVAL): Boolean {
        val lastSync = _lastSyncTime.value ?: return true
        return System.currentTimeMillis() - lastSync >= intervalMillis
    }

    /**
     * Reset sync state
     */
    fun resetSyncState() {
        _syncState.value = SyncState.Idle
    }

    companion object {
        const val DEFAULT_SYNC_INTERVAL = 5 * 60 * 1000L // 5 minutes
    }
}

// ==================== Sync State ====================

/**
 * Represents the current state of theme synchronization
 */
sealed class SyncState {
    /**
     * No sync operation in progress
     */
    data object Idle : SyncState()

    /**
     * Sync operation in progress
     */
    data class Syncing(val direction: SyncDirection) : SyncState()

    /**
     * Sync completed successfully
     */
    data object Success : SyncState()

    /**
     * Sync failed with error
     */
    data class Error(val message: String) : SyncState()
}

/**
 * Direction of synchronization
 */
enum class SyncDirection {
    /**
     * Sync from cloud to local
     */
    FromCloud,

    /**
     * Sync from local to cloud
     */
    ToCloud,

    /**
     * Bidirectional sync
     */
    Bidirectional
}

// ==================== Conflict Resolution ====================

/**
 * Interface for resolving conflicts between local and cloud themes
 */
interface ConflictResolver {
    /**
     * Resolve conflict between local and cloud universal theme
     */
    suspend fun resolveUniversalTheme(local: Theme, cloud: Theme): Theme

    /**
     * Resolve conflict between local and cloud app theme
     */
    suspend fun resolveAppTheme(appId: String, local: Theme, cloud: Theme): Theme

    /**
     * Resolve conflict between local and cloud app override
     */
    suspend fun resolveAppOverride(
        appId: String,
        local: ThemeOverride,
        cloud: ThemeOverride
    ): ThemeOverride
}

/**
 * Last-write-wins conflict resolver
 * Uses modification timestamp to determine which version to keep
 */
class LastWriteWinsResolver : ConflictResolver {
    override suspend fun resolveUniversalTheme(local: Theme, cloud: Theme): Theme {
        // For themes without timestamps, we prefer cloud version
        // In a real implementation, themes should have metadata with timestamps
        return cloud
    }

    override suspend fun resolveAppTheme(appId: String, local: Theme, cloud: Theme): Theme {
        // For themes without timestamps, we prefer cloud version
        return cloud
    }

    override suspend fun resolveAppOverride(
        appId: String,
        local: ThemeOverride,
        cloud: ThemeOverride
    ): ThemeOverride {
        // Use modifiedAt timestamp to determine which version is newer
        return if (local.modifiedAt > cloud.modifiedAt) local else cloud
    }
}

/**
 * Local-first conflict resolver
 * Always prefers local changes over cloud
 */
class LocalFirstResolver : ConflictResolver {
    override suspend fun resolveUniversalTheme(local: Theme, cloud: Theme): Theme = local

    override suspend fun resolveAppTheme(appId: String, local: Theme, cloud: Theme): Theme = local

    override suspend fun resolveAppOverride(
        appId: String,
        local: ThemeOverride,
        cloud: ThemeOverride
    ): ThemeOverride = local
}

/**
 * Cloud-first conflict resolver
 * Always prefers cloud changes over local
 */
class CloudFirstResolver : ConflictResolver {
    override suspend fun resolveUniversalTheme(local: Theme, cloud: Theme): Theme = cloud

    override suspend fun resolveAppTheme(appId: String, local: Theme, cloud: Theme): Theme = cloud

    override suspend fun resolveAppOverride(
        appId: String,
        local: ThemeOverride,
        cloud: ThemeOverride
    ): ThemeOverride = cloud
}

/**
 * Manual conflict resolver
 * Allows user to choose which version to keep
 * Requires a callback to be provided for conflict resolution
 */
class ManualConflictResolver(
    private val onConflict: suspend (local: Any, cloud: Any) -> ConflictResolution
) : ConflictResolver {

    override suspend fun resolveUniversalTheme(local: Theme, cloud: Theme): Theme {
        return when (onConflict(local, cloud)) {
            ConflictResolution.UseLocal -> local
            ConflictResolution.UseCloud -> cloud
            ConflictResolution.Merge -> cloud // Default to cloud for merge
        }
    }

    override suspend fun resolveAppTheme(appId: String, local: Theme, cloud: Theme): Theme {
        return when (onConflict(local, cloud)) {
            ConflictResolution.UseLocal -> local
            ConflictResolution.UseCloud -> cloud
            ConflictResolution.Merge -> cloud
        }
    }

    override suspend fun resolveAppOverride(
        appId: String,
        local: ThemeOverride,
        cloud: ThemeOverride
    ): ThemeOverride {
        return when (onConflict(local, cloud)) {
            ConflictResolution.UseLocal -> local
            ConflictResolution.UseCloud -> cloud
            ConflictResolution.Merge -> if (local.modifiedAt > cloud.modifiedAt) local else cloud
        }
    }
}

/**
 * Conflict resolution strategy
 */
enum class ConflictResolution {
    /**
     * Use local version
     */
    UseLocal,

    /**
     * Use cloud version
     */
    UseCloud,

    /**
     * Merge both versions (implementation-specific)
     */
    Merge
}

// ==================== Version Tracking ====================

/**
 * Theme version metadata for tracking changes
 * Can be extended to include user info, device info, etc.
 */
data class ThemeVersion(
    val themeId: String,
    val version: Int,
    val timestamp: Long,
    val deviceId: String? = null,
    val userId: String? = null
)
