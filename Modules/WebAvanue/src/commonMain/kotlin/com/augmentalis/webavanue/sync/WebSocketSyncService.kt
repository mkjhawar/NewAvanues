package com.augmentalis.webavanue.sync

import com.augmentalis.webavanue.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import io.github.aakira.napier.Napier

/**
 * WebSocket synchronization service for real-time data updates
 *
 * This service manages:
 * - WebSocket connection lifecycle
 * - Real-time event push/pull
 * - Offline queue management
 * - Conflict resolution
 * - Automatic reconnection
 */
interface WebSocketSyncService {

    /** Current connection state */
    val connectionState: StateFlow<ConnectionState>

    /** Sync status including pending operations */
    val syncStatus: StateFlow<SyncStatus>

    /** Flow of incoming remote updates */
    val remoteUpdates: Flow<RemoteUpdateEvent>

    /**
     * Connect to the sync server
     * @param config WebSocket configuration
     * @return Result indicating success or failure
     */
    suspend fun connect(config: WebSocketConfig): Result<Unit>

    /**
     * Disconnect from the sync server
     */
    suspend fun disconnect()

    /**
     * Check if currently connected
     */
    fun isConnected(): Boolean

    // ==================== Entity Sync Methods ====================

    /**
     * Send a tab update to the server
     */
    suspend fun sendTabUpdate(tab: Tab, operation: SyncOperation): Result<Unit>

    /**
     * Send a favorite update to the server
     */
    suspend fun sendFavoriteUpdate(favorite: Favorite, operation: SyncOperation): Result<Unit>

    /**
     * Send a history entry update to the server
     */
    suspend fun sendHistoryUpdate(entry: HistoryEntry, operation: SyncOperation): Result<Unit>

    /**
     * Send a download update to the server
     */
    suspend fun sendDownloadUpdate(download: Download, operation: SyncOperation): Result<Unit>

    /**
     * Send settings update to the server
     */
    suspend fun sendSettingsUpdate(settings: BrowserSettings): Result<Unit>

    /**
     * Send session update to the server
     */
    suspend fun sendSessionUpdate(session: Session, tabs: List<SessionTab>, operation: SyncOperation): Result<Unit>

    // ==================== Sync Control ====================

    /**
     * Request a full sync from the server
     * @param entityTypes Types of entities to sync (null = all)
     */
    suspend fun requestFullSync(entityTypes: Set<SyncEntityType>? = null): Result<Unit>

    /**
     * Flush pending operations in the queue
     */
    suspend fun flushQueue(): Result<Int>

    /**
     * Get queue statistics
     */
    suspend fun getQueueStats(): QueueStats

    /**
     * Cleanup resources
     */
    fun cleanup()
}

/**
 * Connection state for WebSocket
 */
sealed class ConnectionState {
    object Disconnected : ConnectionState()
    object Connecting : ConnectionState()
    data class Connected(val sessionId: String) : ConnectionState()
    data class Reconnecting(val attempt: Int, val maxAttempts: Int) : ConnectionState()
    data class Error(val message: String, val throwable: Throwable? = null) : ConnectionState()
}

/**
 * Implementation of WebSocket sync service
 */
class WebSocketSyncServiceImpl : WebSocketSyncService {

    private val TAG = "WebSocketSync"

    private var config: WebSocketConfig? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var reconnectJob: Job? = null
    private var pingJob: Job? = null

    private val syncQueue = SyncQueue()
    private val conflictResolver = ConflictResolver()

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    override val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _syncStatus = MutableStateFlow(SyncStatus(
        lastSyncTimestamp = null,
        pendingEventCount = 0,
        syncState = SyncState.IDLE,
        errorMessage = null
    ))
    override val syncStatus: StateFlow<SyncStatus> = _syncStatus.asStateFlow()

    private val _remoteUpdates = MutableSharedFlow<RemoteUpdateEvent>(
        replay = 0,
        extraBufferCapacity = 100
    )
    override val remoteUpdates: Flow<RemoteUpdateEvent> = _remoteUpdates.asSharedFlow()

    // Platform-specific WebSocket client (expect/actual)
    private var webSocketClient: WebSocketClient? = null

    override suspend fun connect(config: WebSocketConfig): Result<Unit> {
        // Validate configuration
        val errors = config.validate()
        if (errors.isNotEmpty()) {
            return Result.failure(IllegalArgumentException("Invalid config: ${errors.joinToString()}"))
        }

        this.config = config
        _connectionState.value = ConnectionState.Connecting
        updateSyncStatus(SyncState.CONNECTING)

        return try {
            // Create platform-specific WebSocket client
            webSocketClient = createWebSocketClient(config)

            webSocketClient?.connect { message ->
                scope.launch {
                    handleIncomingMessage(message)
                }
            }

            // Authenticate
            val authResult = authenticate(config)
            if (authResult.isFailure) {
                disconnect()
                return authResult
            }

            // Start keep-alive ping
            startPingJob(config.pingIntervalMs)

            // Flush any queued operations
            flushQueue()

            Napier.i("Connected to sync server", tag = TAG)
            Result.success(Unit)
        } catch (e: Exception) {
            Napier.e("Connection failed: ${e.message}", e, tag = TAG)
            _connectionState.value = ConnectionState.Error(e.message ?: "Unknown error", e)
            updateSyncStatus(SyncState.ERROR, e.message)

            if (config.autoReconnect) {
                scheduleReconnect()
            }

            Result.failure(e)
        }
    }

    override suspend fun disconnect() {
        reconnectJob?.cancel()
        pingJob?.cancel()

        webSocketClient?.disconnect()
        webSocketClient = null

        _connectionState.value = ConnectionState.Disconnected
        updateSyncStatus(SyncState.DISCONNECTED)

        Napier.i("Disconnected from sync server", tag = TAG)
    }

    override fun isConnected(): Boolean {
        return _connectionState.value is ConnectionState.Connected
    }

    // ==================== Entity Sync Methods ====================

    override suspend fun sendTabUpdate(tab: Tab, operation: SyncOperation): Result<Unit> {
        val event = RemoteTabUpdate(
            eventId = generateEventId(),
            timestamp = Clock.System.now(),
            deviceId = config?.deviceId ?: return Result.failure(IllegalStateException("Not configured")),
            operation = operation,
            version = Clock.System.now().toEpochMilliseconds(),
            tab = if (operation != SyncOperation.DELETE) tab else null,
            tabId = tab.id
        )
        return sendEvent(event, SyncPriority.NORMAL)
    }

    override suspend fun sendFavoriteUpdate(favorite: Favorite, operation: SyncOperation): Result<Unit> {
        val event = RemoteFavoriteUpdate(
            eventId = generateEventId(),
            timestamp = Clock.System.now(),
            deviceId = config?.deviceId ?: return Result.failure(IllegalStateException("Not configured")),
            operation = operation,
            version = Clock.System.now().toEpochMilliseconds(),
            favorite = if (operation != SyncOperation.DELETE) favorite else null,
            favoriteId = favorite.id
        )
        return sendEvent(event, SyncPriority.NORMAL)
    }

    override suspend fun sendHistoryUpdate(entry: HistoryEntry, operation: SyncOperation): Result<Unit> {
        // Don't sync incognito history
        if (entry.isIncognito) {
            return Result.success(Unit)
        }

        val event = RemoteHistoryUpdate(
            eventId = generateEventId(),
            timestamp = Clock.System.now(),
            deviceId = config?.deviceId ?: return Result.failure(IllegalStateException("Not configured")),
            operation = operation,
            version = Clock.System.now().toEpochMilliseconds(),
            historyEntry = if (operation != SyncOperation.DELETE) entry else null,
            entryId = entry.id
        )
        return sendEvent(event, SyncPriority.LOW)
    }

    override suspend fun sendDownloadUpdate(download: Download, operation: SyncOperation): Result<Unit> {
        val event = RemoteDownloadUpdate(
            eventId = generateEventId(),
            timestamp = Clock.System.now(),
            deviceId = config?.deviceId ?: return Result.failure(IllegalStateException("Not configured")),
            operation = operation,
            version = Clock.System.now().toEpochMilliseconds(),
            download = if (operation != SyncOperation.DELETE) download else null,
            downloadId = download.id
        )
        return sendEvent(event, SyncPriority.LOW)
    }

    override suspend fun sendSettingsUpdate(settings: BrowserSettings): Result<Unit> {
        val event = RemoteSettingsUpdate(
            eventId = generateEventId(),
            timestamp = Clock.System.now(),
            deviceId = config?.deviceId ?: return Result.failure(IllegalStateException("Not configured")),
            operation = SyncOperation.UPDATE,
            version = Clock.System.now().toEpochMilliseconds(),
            settings = settings
        )
        return sendEvent(event, SyncPriority.HIGH)
    }

    override suspend fun sendSessionUpdate(
        session: Session,
        tabs: List<SessionTab>,
        operation: SyncOperation
    ): Result<Unit> {
        val event = RemoteSessionUpdate(
            eventId = generateEventId(),
            timestamp = Clock.System.now(),
            deviceId = config?.deviceId ?: return Result.failure(IllegalStateException("Not configured")),
            operation = operation,
            version = Clock.System.now().toEpochMilliseconds(),
            session = if (operation != SyncOperation.DELETE) session else null,
            sessionId = session.id,
            tabs = if (operation != SyncOperation.DELETE) tabs else null
        )
        return sendEvent(event, SyncPriority.NORMAL)
    }

    // ==================== Sync Control ====================

    override suspend fun requestFullSync(entityTypes: Set<SyncEntityType>?): Result<Unit> {
        if (!isConnected()) {
            return Result.failure(IllegalStateException("Not connected"))
        }

        updateSyncStatus(SyncState.SYNCING)

        return try {
            val payload = SyncRequestPayload(
                entityTypes = (entityTypes ?: config?.syncEntities ?: emptySet()).toList(),
                lastSyncTimestamp = _syncStatus.value.lastSyncTimestamp?.toEpochMilliseconds(),
                fullSync = true
            )

            webSocketClient?.send(WebSocketMessage(
                type = WebSocketMessageType.SYNC_REQUEST,
                payload = serializePayload(payload),
                messageId = generateEventId(),
                timestamp = Clock.System.now().toEpochMilliseconds(),
                deviceId = config?.deviceId
            ))

            Result.success(Unit)
        } catch (e: Exception) {
            Napier.e("Full sync request failed: ${e.message}", e, tag = TAG)
            updateSyncStatus(SyncState.ERROR, e.message)
            Result.failure(e)
        }
    }

    override suspend fun flushQueue(): Result<Int> {
        var flushedCount = 0

        while (!syncQueue.isEmpty()) {
            val operation = syncQueue.dequeue() ?: break

            if (isConnected()) {
                try {
                    sendEventDirect(operation.event)
                    syncQueue.complete(operation.id)
                    flushedCount++
                } catch (e: Exception) {
                    syncQueue.fail(operation.id, e.message ?: "Unknown error")
                }
            } else {
                // Re-queue if not connected
                syncQueue.enqueue(operation.event, operation.priority)
                break
            }
        }

        return Result.success(flushedCount)
    }

    override suspend fun getQueueStats(): QueueStats {
        return syncQueue.getStats()
    }

    override fun cleanup() {
        scope.cancel()
        reconnectJob?.cancel()
        pingJob?.cancel()
        webSocketClient?.disconnect()
    }

    // ==================== Private Methods ====================

    private suspend fun sendEvent(event: RemoteUpdateEvent, priority: SyncPriority): Result<Unit> {
        return if (isConnected()) {
            try {
                sendEventDirect(event)
                Result.success(Unit)
            } catch (e: Exception) {
                // Queue for retry
                syncQueue.enqueue(event, priority)
                Result.failure(e)
            }
        } else {
            // Queue for later
            syncQueue.enqueue(event, priority)
            Result.success(Unit) // Success because it's queued
        }
    }

    private suspend fun sendEventDirect(event: RemoteUpdateEvent) {
        val message = WebSocketMessage(
            type = when (event) {
                is RemoteTabUpdate,
                is RemoteFavoriteUpdate,
                is RemoteHistoryUpdate,
                is RemoteDownloadUpdate,
                is RemoteSessionUpdate -> when (event.operation) {
                    SyncOperation.CREATE -> WebSocketMessageType.ENTITY_CREATE
                    SyncOperation.UPDATE -> WebSocketMessageType.ENTITY_UPDATE
                    SyncOperation.DELETE -> WebSocketMessageType.ENTITY_DELETE
                }
                is RemoteSettingsUpdate -> WebSocketMessageType.ENTITY_UPDATE
            },
            payload = serializeEvent(event),
            messageId = event.eventId,
            timestamp = event.timestamp.toEpochMilliseconds(),
            deviceId = event.deviceId
        )

        webSocketClient?.send(message)
    }

    private suspend fun handleIncomingMessage(message: WebSocketMessage) {
        when (message.type) {
            WebSocketMessageType.SYNC_PUSH -> {
                val event = deserializeEvent(message.payload)
                if (event != null) {
                    _remoteUpdates.emit(event)
                }
            }
            WebSocketMessageType.SYNC_RESPONSE -> {
                val response = deserializeSyncResponse(message.payload)
                response?.events?.forEach { event ->
                    _remoteUpdates.emit(event)
                }
                if (response?.hasMore == false) {
                    updateSyncStatus(SyncState.IDLE)
                }
            }
            WebSocketMessageType.PONG -> {
                // Keep-alive acknowledged
            }
            WebSocketMessageType.ERROR -> {
                Napier.e("Server error: ${message.payload}", tag = TAG)
                updateSyncStatus(SyncState.ERROR, message.payload)
            }
            WebSocketMessageType.AUTH_ERROR -> {
                Napier.e("Auth error: ${message.payload}", tag = TAG)
                disconnect()
            }
            else -> {
                if (config?.debugLogging == true) {
                    Napier.d("Received message: ${message.type}", tag = TAG)
                }
            }
        }
    }

    private suspend fun authenticate(config: WebSocketConfig): Result<Unit> {
        val authPayload = AuthPayload(
            token = config.authToken ?: "",
            deviceId = config.deviceId,
            userId = config.userId,
            appVersion = "1.0.0", // TODO: Get from app
            platform = getPlatformName()
        )

        val message = WebSocketMessage(
            type = WebSocketMessageType.AUTH_REQUEST,
            payload = serializePayload(authPayload),
            messageId = generateEventId(),
            timestamp = Clock.System.now().toEpochMilliseconds(),
            deviceId = config.deviceId
        )

        webSocketClient?.send(message)

        // Wait for auth response (simplified - real implementation would use CompletableDeferred)
        delay(1000)

        _connectionState.value = ConnectionState.Connected(config.deviceId)
        updateSyncStatus(SyncState.CONNECTED)

        return Result.success(Unit)
    }

    private fun startPingJob(intervalMs: Long) {
        pingJob?.cancel()
        pingJob = scope.launch {
            while (isActive && isConnected()) {
                delay(intervalMs)
                try {
                    webSocketClient?.send(WebSocketMessage(
                        type = WebSocketMessageType.PING,
                        payload = "",
                        messageId = generateEventId(),
                        timestamp = Clock.System.now().toEpochMilliseconds()
                    ))
                } catch (e: Exception) {
                    Napier.w("Ping failed: ${e.message}", tag = TAG)
                }
            }
        }
    }

    private fun scheduleReconnect() {
        val cfg = config ?: return

        reconnectJob?.cancel()
        reconnectJob = scope.launch {
            var attempt = 0
            var delay = cfg.initialReconnectDelayMs

            while (attempt < cfg.maxReconnectAttempts && !isConnected()) {
                attempt++
                _connectionState.value = ConnectionState.Reconnecting(attempt, cfg.maxReconnectAttempts)

                Napier.i("Reconnection attempt $attempt/${cfg.maxReconnectAttempts}", tag = TAG)

                delay(delay)

                try {
                    connect(cfg)
                    if (isConnected()) {
                        return@launch
                    }
                } catch (e: Exception) {
                    Napier.w("Reconnection attempt $attempt failed: ${e.message}", tag = TAG)
                }

                delay = (delay * cfg.reconnectDelayMultiplier).toLong()
                    .coerceAtMost(cfg.maxReconnectDelayMs)
            }

            if (!isConnected()) {
                _connectionState.value = ConnectionState.Error(
                    "Failed to reconnect after ${cfg.maxReconnectAttempts} attempts"
                )
                updateSyncStatus(SyncState.OFFLINE)
            }
        }
    }

    private fun updateSyncStatus(state: SyncState, error: String? = null) {
        scope.launch {
            val stats = syncQueue.getStats()
            _syncStatus.value = SyncStatus(
                lastSyncTimestamp = if (state == SyncState.IDLE) Clock.System.now() else _syncStatus.value.lastSyncTimestamp,
                pendingEventCount = stats.pendingCount,
                syncState = state,
                errorMessage = error
            )
        }
    }

    private fun generateEventId(): String {
        return "evt_${Clock.System.now().toEpochMilliseconds()}_${(1000..9999).random()}"
    }

    // Serialization helpers (would use kotlinx.serialization in real implementation)
    private fun serializeEvent(event: RemoteUpdateEvent): String = "{}" // Placeholder
    private fun deserializeEvent(json: String): RemoteUpdateEvent? = null // Placeholder
    private fun serializePayload(payload: Any): String = "{}" // Placeholder
    private fun deserializeSyncResponse(json: String): SyncResponsePayload? = null // Placeholder
}

/**
 * Platform-specific WebSocket client interface
 */
expect fun createWebSocketClient(config: WebSocketConfig): WebSocketClient

/**
 * Platform-specific WebSocket client
 */
interface WebSocketClient {
    suspend fun connect(onMessage: (WebSocketMessage) -> Unit)
    suspend fun send(message: WebSocketMessage)
    suspend fun disconnect()
}

/**
 * Get platform name for sync
 */
expect fun getPlatformName(): String
