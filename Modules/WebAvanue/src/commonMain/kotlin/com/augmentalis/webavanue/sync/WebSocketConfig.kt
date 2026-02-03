package com.augmentalis.webavanue.sync

import kotlinx.serialization.Serializable

/**
 * Configuration for WebSocket synchronization service
 */
@Serializable
data class WebSocketConfig(
    /** WebSocket server URL */
    val serverUrl: String,

    /** Authentication token for server connection */
    val authToken: String? = null,

    /** Unique device identifier for sync */
    val deviceId: String,

    /** User ID for multi-device sync */
    val userId: String? = null,

    /** Connection timeout in milliseconds */
    val connectionTimeoutMs: Long = 30_000,

    /** Read timeout in milliseconds */
    val readTimeoutMs: Long = 30_000,

    /** Ping interval in milliseconds (keep-alive) */
    val pingIntervalMs: Long = 30_000,

    /** Maximum reconnection attempts before giving up */
    val maxReconnectAttempts: Int = 5,

    /** Initial reconnection delay in milliseconds */
    val initialReconnectDelayMs: Long = 1_000,

    /** Maximum reconnection delay in milliseconds */
    val maxReconnectDelayMs: Long = 60_000,

    /** Reconnection delay multiplier (exponential backoff) */
    val reconnectDelayMultiplier: Double = 2.0,

    /** Enable automatic reconnection */
    val autoReconnect: Boolean = true,

    /** Enable message compression */
    val enableCompression: Boolean = true,

    /** Maximum message size in bytes */
    val maxMessageSize: Long = 1_048_576, // 1MB

    /** Batch size for sync operations */
    val syncBatchSize: Int = 50,

    /** Sync interval when connected (milliseconds) */
    val syncIntervalMs: Long = 5_000,

    /** Enable debug logging */
    val debugLogging: Boolean = false,

    /** Conflict resolution strategy */
    val conflictStrategy: ConflictStrategy = ConflictStrategy.LAST_WRITE_WINS,

    /** Entities to sync */
    val syncEntities: Set<SyncEntityType> = setOf(
        SyncEntityType.TAB,
        SyncEntityType.FAVORITE,
        SyncEntityType.SETTINGS,
        SyncEntityType.SESSION
    )
) {
    companion object {
        /**
         * Default configuration for development
         */
        fun development(deviceId: String) = WebSocketConfig(
            serverUrl = "wss://dev-sync.webavanue.local/ws",
            deviceId = deviceId,
            debugLogging = true,
            maxReconnectAttempts = 3
        )

        /**
         * Default configuration for production
         */
        fun production(deviceId: String, authToken: String) = WebSocketConfig(
            serverUrl = "wss://sync.webavanue.com/ws",
            deviceId = deviceId,
            authToken = authToken,
            debugLogging = false,
            enableCompression = true
        )

        /**
         * Minimal sync configuration (tabs and settings only)
         */
        fun minimal(deviceId: String) = WebSocketConfig(
            serverUrl = "wss://sync.webavanue.com/ws",
            deviceId = deviceId,
            syncEntities = setOf(SyncEntityType.TAB, SyncEntityType.SETTINGS),
            syncIntervalMs = 30_000
        )
    }

    /**
     * Validate configuration
     */
    fun validate(): List<String> {
        val errors = mutableListOf<String>()

        if (serverUrl.isBlank()) {
            errors.add("Server URL cannot be empty")
        }
        if (!serverUrl.startsWith("ws://") && !serverUrl.startsWith("wss://")) {
            errors.add("Server URL must start with ws:// or wss://")
        }
        if (deviceId.isBlank()) {
            errors.add("Device ID cannot be empty")
        }
        if (connectionTimeoutMs <= 0) {
            errors.add("Connection timeout must be positive")
        }
        if (maxReconnectAttempts < 0) {
            errors.add("Max reconnect attempts cannot be negative")
        }
        if (syncBatchSize <= 0) {
            errors.add("Sync batch size must be positive")
        }

        return errors
    }

    /**
     * Check if configuration is valid
     */
    val isValid: Boolean get() = validate().isEmpty()
}

/**
 * WebSocket message types for protocol
 */
@Serializable
enum class WebSocketMessageType {
    // Connection lifecycle
    CONNECT,
    DISCONNECT,
    PING,
    PONG,

    // Authentication
    AUTH_REQUEST,
    AUTH_RESPONSE,
    AUTH_ERROR,

    // Sync operations
    SYNC_REQUEST,
    SYNC_RESPONSE,
    SYNC_PUSH,
    SYNC_ACK,
    SYNC_ERROR,

    // Entity operations
    ENTITY_CREATE,
    ENTITY_UPDATE,
    ENTITY_DELETE,
    ENTITY_BATCH,

    // Status
    STATUS_REQUEST,
    STATUS_RESPONSE,

    // Error
    ERROR
}

/**
 * Base WebSocket message structure
 */
@Serializable
data class WebSocketMessage(
    val type: WebSocketMessageType,
    val payload: String, // JSON-encoded payload
    val messageId: String,
    val timestamp: Long,
    val deviceId: String? = null
)

/**
 * Authentication request payload
 */
@Serializable
data class AuthPayload(
    val token: String,
    val deviceId: String,
    val userId: String?,
    val appVersion: String,
    val platform: String
)

/**
 * Authentication response
 */
@Serializable
data class AuthResponse(
    val success: Boolean,
    val sessionId: String?,
    val serverVersion: String?,
    val errorCode: String? = null,
    val errorMessage: String? = null
)

/**
 * Sync request payload
 */
@Serializable
data class SyncRequestPayload(
    val entityTypes: List<SyncEntityType>,
    val lastSyncTimestamp: Long?,
    val fullSync: Boolean = false
)

/**
 * Sync response payload
 */
@Serializable
data class SyncResponsePayload(
    val events: List<RemoteUpdateEvent>,
    val hasMore: Boolean,
    val serverTimestamp: Long,
    val nextCursor: String? = null
)
