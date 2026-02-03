package com.augmentalis.webavanue.sync

import com.augmentalis.webavanue.*
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

/**
 * Represents the type of operation performed on an entity
 */
@Serializable
enum class SyncOperation {
    CREATE,
    UPDATE,
    DELETE
}

/**
 * Base sealed class for all remote update events
 */
@Serializable
sealed class RemoteUpdateEvent {
    abstract val eventId: String
    abstract val timestamp: Instant
    abstract val deviceId: String
    abstract val operation: SyncOperation
    abstract val version: Long
}

/**
 * Remote update for tab operations
 */
@Serializable
data class RemoteTabUpdate(
    override val eventId: String,
    override val timestamp: Instant,
    override val deviceId: String,
    override val operation: SyncOperation,
    override val version: Long,
    val tab: Tab? = null,
    val tabId: String? = null
) : RemoteUpdateEvent()

/**
 * Remote update for favorite operations
 */
@Serializable
data class RemoteFavoriteUpdate(
    override val eventId: String,
    override val timestamp: Instant,
    override val deviceId: String,
    override val operation: SyncOperation,
    override val version: Long,
    val favorite: Favorite? = null,
    val favoriteId: String? = null
) : RemoteUpdateEvent()

/**
 * Remote update for history operations
 */
@Serializable
data class RemoteHistoryUpdate(
    override val eventId: String,
    override val timestamp: Instant,
    override val deviceId: String,
    override val operation: SyncOperation,
    override val version: Long,
    val historyEntry: HistoryEntry? = null,
    val entryId: String? = null
) : RemoteUpdateEvent()

/**
 * Remote update for download operations
 */
@Serializable
data class RemoteDownloadUpdate(
    override val eventId: String,
    override val timestamp: Instant,
    override val deviceId: String,
    override val operation: SyncOperation,
    override val version: Long,
    val download: Download? = null,
    val downloadId: String? = null
) : RemoteUpdateEvent()

/**
 * Remote update for settings changes
 */
@Serializable
data class RemoteSettingsUpdate(
    override val eventId: String,
    override val timestamp: Instant,
    override val deviceId: String,
    override val operation: SyncOperation,
    override val version: Long,
    val settings: BrowserSettings
) : RemoteUpdateEvent()

/**
 * Remote update for session operations
 */
@Serializable
data class RemoteSessionUpdate(
    override val eventId: String,
    override val timestamp: Instant,
    override val deviceId: String,
    override val operation: SyncOperation,
    override val version: Long,
    val session: Session? = null,
    val sessionId: String? = null,
    val tabs: List<SessionTab>? = null
) : RemoteUpdateEvent()

/**
 * Batch update containing multiple events
 */
@Serializable
data class SyncBatch(
    val batchId: String,
    val events: List<RemoteUpdateEvent>,
    val timestamp: Instant = Clock.System.now()
)

/**
 * Acknowledgment message sent back to server
 */
@Serializable
data class SyncAcknowledgment(
    val eventId: String,
    val deviceId: String,
    val success: Boolean,
    val errorMessage: String? = null,
    val timestamp: Instant = Clock.System.now()
)

/**
 * Sync status for tracking synchronization state
 */
@Serializable
data class SyncStatus(
    val lastSyncTimestamp: Instant?,
    val pendingEventCount: Int,
    val syncState: SyncState,
    val errorMessage: String? = null
)

/**
 * Current synchronization state
 */
@Serializable
enum class SyncState {
    IDLE,
    CONNECTING,
    CONNECTED,
    SYNCING,
    DISCONNECTED,
    ERROR,
    OFFLINE
}

/**
 * Entity type for sync operations
 */
@Serializable
enum class SyncEntityType {
    TAB,
    FAVORITE,
    HISTORY,
    DOWNLOAD,
    SETTINGS,
    SESSION
}

/**
 * Metadata for tracking entity versions
 */
@Serializable
data class SyncMetadata(
    val entityType: SyncEntityType,
    val entityId: String,
    val localVersion: Long,
    val remoteVersion: Long,
    val lastModified: Instant,
    val needsSync: Boolean
)
