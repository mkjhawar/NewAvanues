package com.augmentalis.webavanue.sync

import com.augmentalis.webavanue.*
import kotlinx.datetime.Instant

/**
 * Strategy for resolving conflicts between local and remote data
 */
enum class ConflictStrategy {
    /** Remote data always wins */
    SERVER_WINS,
    /** Local data always wins */
    CLIENT_WINS,
    /** Most recent modification wins */
    LAST_WRITE_WINS,
    /** Merge both changes when possible */
    MERGE,
    /** Prompt user to resolve */
    MANUAL
}

/**
 * Result of a conflict resolution
 */
sealed class ConflictResult<T> {
    data class Resolved<T>(val value: T) : ConflictResult<T>()
    data class ManualRequired<T>(val local: T, val remote: T) : ConflictResult<T>()
    data class Failed<T>(val error: String) : ConflictResult<T>()
}

/**
 * Conflict resolver for handling synchronization conflicts
 */
class ConflictResolver(
    private val defaultStrategy: ConflictStrategy = ConflictStrategy.LAST_WRITE_WINS
) {

    /**
     * Resolve tab conflicts
     */
    fun resolveTabConflict(
        local: Tab,
        remote: Tab,
        localTimestamp: Instant,
        remoteTimestamp: Instant,
        strategy: ConflictStrategy = defaultStrategy
    ): ConflictResult<Tab> {
        return when (strategy) {
            ConflictStrategy.SERVER_WINS -> ConflictResult.Resolved(remote)
            ConflictStrategy.CLIENT_WINS -> ConflictResult.Resolved(local)
            ConflictStrategy.LAST_WRITE_WINS -> {
                if (remoteTimestamp > localTimestamp) {
                    ConflictResult.Resolved(remote)
                } else {
                    ConflictResult.Resolved(local)
                }
            }
            ConflictStrategy.MERGE -> {
                // Merge tab properties intelligently
                val merged = mergeTab(local, remote, localTimestamp, remoteTimestamp)
                ConflictResult.Resolved(merged)
            }
            ConflictStrategy.MANUAL -> ConflictResult.ManualRequired(local, remote)
        }
    }

    /**
     * Resolve favorite conflicts
     */
    fun resolveFavoriteConflict(
        local: Favorite,
        remote: Favorite,
        localTimestamp: Instant,
        remoteTimestamp: Instant,
        strategy: ConflictStrategy = defaultStrategy
    ): ConflictResult<Favorite> {
        return when (strategy) {
            ConflictStrategy.SERVER_WINS -> ConflictResult.Resolved(remote)
            ConflictStrategy.CLIENT_WINS -> ConflictResult.Resolved(local)
            ConflictStrategy.LAST_WRITE_WINS -> {
                if (remoteTimestamp > localTimestamp) {
                    ConflictResult.Resolved(remote)
                } else {
                    ConflictResult.Resolved(local)
                }
            }
            ConflictStrategy.MERGE -> {
                val merged = mergeFavorite(local, remote, localTimestamp, remoteTimestamp)
                ConflictResult.Resolved(merged)
            }
            ConflictStrategy.MANUAL -> ConflictResult.ManualRequired(local, remote)
        }
    }

    /**
     * Resolve history conflicts
     * History typically uses server-wins since it's append-only
     */
    fun resolveHistoryConflict(
        local: HistoryEntry,
        remote: HistoryEntry,
        strategy: ConflictStrategy = ConflictStrategy.SERVER_WINS
    ): ConflictResult<HistoryEntry> {
        return when (strategy) {
            ConflictStrategy.SERVER_WINS -> ConflictResult.Resolved(remote)
            ConflictStrategy.CLIENT_WINS -> ConflictResult.Resolved(local)
            else -> ConflictResult.Resolved(remote) // Default to server for history
        }
    }

    /**
     * Resolve download conflicts
     * Downloads use client-wins since local progress is authoritative
     */
    fun resolveDownloadConflict(
        local: Download,
        remote: Download,
        strategy: ConflictStrategy = ConflictStrategy.CLIENT_WINS
    ): ConflictResult<Download> {
        return when (strategy) {
            ConflictStrategy.SERVER_WINS -> ConflictResult.Resolved(remote)
            ConflictStrategy.CLIENT_WINS -> ConflictResult.Resolved(local)
            ConflictStrategy.LAST_WRITE_WINS,
            ConflictStrategy.MERGE -> {
                // For downloads, prefer local state as it has actual progress
                ConflictResult.Resolved(local)
            }
            ConflictStrategy.MANUAL -> ConflictResult.ManualRequired(local, remote)
        }
    }

    /**
     * Resolve settings conflicts
     */
    fun resolveSettingsConflict(
        local: BrowserSettings,
        remote: BrowserSettings,
        localTimestamp: Instant,
        remoteTimestamp: Instant,
        strategy: ConflictStrategy = defaultStrategy
    ): ConflictResult<BrowserSettings> {
        return when (strategy) {
            ConflictStrategy.SERVER_WINS -> ConflictResult.Resolved(remote)
            ConflictStrategy.CLIENT_WINS -> ConflictResult.Resolved(local)
            ConflictStrategy.LAST_WRITE_WINS -> {
                if (remoteTimestamp > localTimestamp) {
                    ConflictResult.Resolved(remote)
                } else {
                    ConflictResult.Resolved(local)
                }
            }
            ConflictStrategy.MERGE -> {
                val merged = mergeSettings(local, remote, localTimestamp, remoteTimestamp)
                ConflictResult.Resolved(merged)
            }
            ConflictStrategy.MANUAL -> ConflictResult.ManualRequired(local, remote)
        }
    }

    // ==================== Merge Strategies ====================

    /**
     * Merge two tabs, preferring newer values for each field
     */
    private fun mergeTab(
        local: Tab,
        remote: Tab,
        localTimestamp: Instant,
        remoteTimestamp: Instant
    ): Tab {
        val useRemote = remoteTimestamp > localTimestamp
        return Tab(
            id = local.id, // Keep local ID
            url = if (useRemote) remote.url else local.url,
            title = if (useRemote) remote.title else local.title,
            favicon = remote.favicon ?: local.favicon, // Prefer non-null
            isActive = local.isActive, // Local state is authoritative for active
            isPinned = if (useRemote) remote.isPinned else local.isPinned,
            isIncognito = local.isIncognito, // Local incognito state
            createdAt = minOf(local.createdAt, remote.createdAt), // Earliest creation
            lastAccessedAt = maxOf(local.lastAccessedAt, remote.lastAccessedAt), // Latest access
            position = local.position // Local position is authoritative
        )
    }

    /**
     * Merge two favorites
     */
    private fun mergeFavorite(
        local: Favorite,
        remote: Favorite,
        localTimestamp: Instant,
        remoteTimestamp: Instant
    ): Favorite {
        val useRemote = remoteTimestamp > localTimestamp
        return Favorite(
            id = local.id,
            url = if (useRemote) remote.url else local.url,
            title = if (useRemote) remote.title else local.title,
            favicon = remote.favicon ?: local.favicon,
            folderId = if (useRemote) remote.folderId else local.folderId,
            position = local.position, // Keep local position
            createdAt = minOf(local.createdAt, remote.createdAt),
            lastVisitedAt = maxOf(
                local.lastVisitedAt ?: local.createdAt,
                remote.lastVisitedAt ?: remote.createdAt
            )
        )
    }

    /**
     * Merge two settings objects
     */
    private fun mergeSettings(
        local: BrowserSettings,
        remote: BrowserSettings,
        localTimestamp: Instant,
        remoteTimestamp: Instant
    ): BrowserSettings {
        // For settings, we use server values for sync-able settings
        // but keep local values for device-specific settings
        val useRemote = remoteTimestamp > localTimestamp
        return if (useRemote) {
            remote.copy(
                // Keep device-specific settings from local
                downloadPath = local.downloadPath,
                askDownloadLocation = local.askDownloadLocation
            )
        } else {
            local
        }
    }
}

/**
 * Extension to determine if entities need conflict resolution
 */
fun <T> needsConflictResolution(
    localVersion: Long,
    remoteVersion: Long,
    localModified: Boolean
): Boolean {
    // Conflict exists if:
    // 1. Both have been modified (versions don't match)
    // 2. Local has changes that haven't been synced
    return localVersion != remoteVersion && localModified
}
