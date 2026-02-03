package com.augmentalis.websocket

import kotlinx.datetime.Clock

/**
 * AVU-format sync message for WebSocket communication
 *
 * Uses the AVU IPC protocol format with 3-letter prefixes:
 * - REQ: Request
 * - RES: Response
 * - EVT: Event
 * - ERR: Error
 * - ACK: Acknowledgment
 * - PNG: Ping
 * - PON: Pong
 * - HND: Handshake
 *
 * Format: PREFIX:id:field1:field2:...
 * Escape: : -> %3A, % -> %25, \n -> %0A
 */
object AvuSyncMessage {

    // AVU IPC Prefixes for WebSocket sync
    const val PREFIX_REQUEST = "REQ"
    const val PREFIX_RESPONSE = "RES"
    const val PREFIX_EVENT = "EVT"
    const val PREFIX_ERROR = "ERR"
    const val PREFIX_ACK = "ACK"
    const val PREFIX_PING = "PNG"
    const val PREFIX_PONG = "PON"
    const val PREFIX_HANDSHAKE = "HND"
    const val PREFIX_CAPABILITY = "CAP"

    // Sync-specific prefixes
    const val PREFIX_SYNC_CREATE = "SCR"  // Sync Create
    const val PREFIX_SYNC_UPDATE = "SUP"  // Sync Update
    const val PREFIX_SYNC_DELETE = "SDL"  // Sync Delete
    const val PREFIX_SYNC_BATCH = "SBT"   // Sync Batch
    const val PREFIX_SYNC_FULL = "SFL"    // Sync Full Request
    const val PREFIX_SYNC_RESPONSE = "SRS" // Sync Response (server push)
    const val PREFIX_SYNC_CONFLICT = "SCF" // Sync Conflict
    const val PREFIX_SYNC_STATUS = "SST"   // Sync Status

    // Connection state prefixes
    const val PREFIX_CONNECT = "CON"      // Connected
    const val PREFIX_DISCONNECT = "DIS"   // Disconnected
    const val PREFIX_RECONNECT = "RCN"    // Reconnecting

    // Entity type identifiers
    const val ENTITY_TAB = "TAB"
    const val ENTITY_FAVORITE = "FAV"
    const val ENTITY_HISTORY = "HST"
    const val ENTITY_DOWNLOAD = "DWN"
    const val ENTITY_SETTINGS = "SET"
    const val ENTITY_SESSION = "SES"

    // Status codes
    const val STATUS_OK = "OK"
    const val STATUS_ERROR = "ERR"
    const val STATUS_CONFLICT = "CNF"
    const val STATUS_PENDING = "PND"

    /**
     * Create a ping message
     * Format: PNG:sessionId:timestamp
     */
    fun ping(sessionId: String): String {
        val timestamp = Clock.System.now().toEpochMilliseconds()
        return "$PREFIX_PING:${escape(sessionId)}:$timestamp"
    }

    /**
     * Create a pong message
     * Format: PON:sessionId:timestamp
     */
    fun pong(sessionId: String): String {
        val timestamp = Clock.System.now().toEpochMilliseconds()
        return "$PREFIX_PONG:${escape(sessionId)}:$timestamp"
    }

    /**
     * Create a handshake message
     * Format: HND:sessionId:deviceId:appVersion:platform
     */
    fun handshake(
        sessionId: String,
        deviceId: String,
        appVersion: String,
        platform: String,
        userId: String? = null
    ): String {
        val base = "$PREFIX_HANDSHAKE:${escape(sessionId)}:${escape(deviceId)}:${escape(appVersion)}:${escape(platform)}"
        return if (userId != null) "$base:${escape(userId)}" else base
    }

    /**
     * Create a capability message
     * Format: CAP:sessionId:cap1,cap2,cap3
     */
    fun capabilities(sessionId: String, capabilities: List<String>): String {
        val caps = capabilities.joinToString(",") { escape(it) }
        return "$PREFIX_CAPABILITY:${escape(sessionId)}:$caps"
    }

    /**
     * Create a sync entity create message
     * Format: SCR:msgId:entityType:entityId:version:data
     */
    fun syncCreate(
        messageId: String,
        entityType: String,
        entityId: String,
        version: Long,
        data: String
    ): String {
        return "$PREFIX_SYNC_CREATE:${escape(messageId)}:$entityType:${escape(entityId)}:$version:${escape(data)}"
    }

    /**
     * Create a sync entity update message
     * Format: SUP:msgId:entityType:entityId:version:data
     */
    fun syncUpdate(
        messageId: String,
        entityType: String,
        entityId: String,
        version: Long,
        data: String
    ): String {
        return "$PREFIX_SYNC_UPDATE:${escape(messageId)}:$entityType:${escape(entityId)}:$version:${escape(data)}"
    }

    /**
     * Create a sync entity delete message
     * Format: SDL:msgId:entityType:entityId
     */
    fun syncDelete(
        messageId: String,
        entityType: String,
        entityId: String
    ): String {
        return "$PREFIX_SYNC_DELETE:${escape(messageId)}:$entityType:${escape(entityId)}"
    }

    /**
     * Create a full sync request
     * Format: SFL:msgId:entityTypes:lastSyncTimestamp
     */
    fun syncFullRequest(
        messageId: String,
        entityTypes: List<String>,
        lastSyncTimestamp: Long? = null
    ): String {
        val types = entityTypes.joinToString(",")
        val timestamp = lastSyncTimestamp?.toString() ?: "0"
        return "$PREFIX_SYNC_FULL:${escape(messageId)}:$types:$timestamp"
    }

    /**
     * Create an acknowledgment message
     * Format: ACK:msgId:timestamp
     */
    fun ack(messageId: String): String {
        val timestamp = Clock.System.now().toEpochMilliseconds()
        return "$PREFIX_ACK:${escape(messageId)}:$timestamp"
    }

    /**
     * Create an error message
     * Format: ERR:code:message
     */
    fun error(code: String, message: String, originalMessageId: String? = null): String {
        val base = "$PREFIX_ERROR:${escape(code)}:${escape(message)}"
        return if (originalMessageId != null) "$base:${escape(originalMessageId)}" else base
    }

    /**
     * Create a response message
     * Format: RES:msgId:status:data
     */
    fun response(messageId: String, status: String, data: String? = null): String {
        val base = "$PREFIX_RESPONSE:${escape(messageId)}:${escape(status)}"
        return if (data != null) "$base:${escape(data)}" else base
    }

    /**
     * Create an event message
     * Format: EVT:type:source:data
     */
    fun event(type: String, source: String, data: String): String {
        return "$PREFIX_EVENT:${escape(type)}:${escape(source)}:${escape(data)}"
    }

    /**
     * Create a sync batch message (multiple operations)
     * Format: SBT:msgId:count:op1|op2|op3
     * Each operation: entityType,entityId,action,version,data
     */
    fun syncBatch(
        messageId: String,
        operations: List<BatchOperation>
    ): String {
        val ops = operations.joinToString("|") { op ->
            "${op.entityType},${escape(op.entityId)},${op.action},${op.version},${escape(op.data)}"
        }
        return "$PREFIX_SYNC_BATCH:${escape(messageId)}:${operations.size}:$ops"
    }

    /**
     * Create a sync conflict message
     * Format: SCF:msgId:entityType:entityId:localVersion:remoteVersion:resolution
     */
    fun syncConflict(
        messageId: String,
        entityType: String,
        entityId: String,
        localVersion: Long,
        remoteVersion: Long,
        resolution: String // "LOCAL", "REMOTE", "MERGE", "MANUAL"
    ): String {
        return "$PREFIX_SYNC_CONFLICT:${escape(messageId)}:$entityType:${escape(entityId)}:$localVersion:$remoteVersion:$resolution"
    }

    /**
     * Create a sync status message
     * Format: SST:sessionId:state:pendingCount:lastSync
     */
    fun syncStatus(
        sessionId: String,
        state: String, // "IDLE", "SYNCING", "ERROR", "OFFLINE"
        pendingCount: Int,
        lastSyncTimestamp: Long?
    ): String {
        val timestamp = lastSyncTimestamp?.toString() ?: "0"
        return "$PREFIX_SYNC_STATUS:${escape(sessionId)}:$state:$pendingCount:$timestamp"
    }

    /**
     * Create a connected message
     * Format: CON:sessionId:serverVersion:timestamp
     */
    fun connected(sessionId: String, serverVersion: String): String {
        val timestamp = Clock.System.now().toEpochMilliseconds()
        return "$PREFIX_CONNECT:${escape(sessionId)}:${escape(serverVersion)}:$timestamp"
    }

    /**
     * Create a disconnected message
     * Format: DIS:sessionId:reason:timestamp
     */
    fun disconnected(sessionId: String, reason: String): String {
        val timestamp = Clock.System.now().toEpochMilliseconds()
        return "$PREFIX_DISCONNECT:${escape(sessionId)}:${escape(reason)}:$timestamp"
    }

    /**
     * Create a reconnecting message
     * Format: RCN:sessionId:attempt:maxAttempts:delayMs
     */
    fun reconnecting(sessionId: String, attempt: Int, maxAttempts: Int, delayMs: Long): String {
        return "$PREFIX_RECONNECT:${escape(sessionId)}:$attempt:$maxAttempts:$delayMs"
    }

    /**
     * Create a sync response (server push of entity)
     * Format: SRS:msgId:entityType:entityId:version:data
     */
    fun syncResponse(
        messageId: String,
        entityType: String,
        entityId: String,
        version: Long,
        data: String
    ): String {
        return "$PREFIX_SYNC_RESPONSE:${escape(messageId)}:$entityType:${escape(entityId)}:$version:${escape(data)}"
    }

    // ==================== Parsing ====================

    /**
     * Parse an AVU message
     * @return Parsed message or null if invalid
     */
    fun parse(message: String): ParsedAvuMessage? {
        val parts = message.split(":")
        if (parts.isEmpty()) return null

        val prefix = parts[0]
        val fields = parts.drop(1).map { unescape(it) }

        return when (prefix) {
            PREFIX_PING -> parsePing(fields)
            PREFIX_PONG -> parsePong(fields)
            PREFIX_HANDSHAKE -> parseHandshake(fields)
            PREFIX_CAPABILITY -> parseCapability(fields)
            PREFIX_SYNC_CREATE -> parseSyncCreate(fields)
            PREFIX_SYNC_UPDATE -> parseSyncUpdate(fields)
            PREFIX_SYNC_DELETE -> parseSyncDelete(fields)
            PREFIX_SYNC_FULL -> parseSyncFullRequest(fields)
            PREFIX_SYNC_BATCH -> parseSyncBatch(fields)
            PREFIX_SYNC_CONFLICT -> parseSyncConflict(fields)
            PREFIX_SYNC_STATUS -> parseSyncStatus(fields)
            PREFIX_SYNC_RESPONSE -> parseSyncResponse(fields)
            PREFIX_CONNECT -> parseConnected(fields)
            PREFIX_DISCONNECT -> parseDisconnected(fields)
            PREFIX_RECONNECT -> parseReconnecting(fields)
            PREFIX_ACK -> parseAck(fields)
            PREFIX_ERROR -> parseError(fields)
            PREFIX_RESPONSE -> parseResponse(fields)
            PREFIX_EVENT -> parseEvent(fields)
            else -> ParsedAvuMessage.Unknown(prefix, fields)
        }
    }

    private fun parsePing(fields: List<String>): ParsedAvuMessage? {
        if (fields.size < 2) return null
        return ParsedAvuMessage.Ping(sessionId = fields[0], timestamp = fields[1].toLongOrNull() ?: 0)
    }

    private fun parsePong(fields: List<String>): ParsedAvuMessage? {
        if (fields.size < 2) return null
        return ParsedAvuMessage.Pong(sessionId = fields[0], timestamp = fields[1].toLongOrNull() ?: 0)
    }

    private fun parseHandshake(fields: List<String>): ParsedAvuMessage? {
        if (fields.size < 4) return null
        return ParsedAvuMessage.Handshake(
            sessionId = fields[0],
            deviceId = fields[1],
            appVersion = fields[2],
            platform = fields[3],
            userId = fields.getOrNull(4)
        )
    }

    private fun parseCapability(fields: List<String>): ParsedAvuMessage? {
        if (fields.size < 2) return null
        return ParsedAvuMessage.Capability(
            sessionId = fields[0],
            capabilities = fields[1].split(",")
        )
    }

    private fun parseSyncCreate(fields: List<String>): ParsedAvuMessage? {
        if (fields.size < 5) return null
        return ParsedAvuMessage.SyncCreate(
            messageId = fields[0],
            entityType = fields[1],
            entityId = fields[2],
            version = fields[3].toLongOrNull() ?: 0,
            data = fields[4]
        )
    }

    private fun parseSyncUpdate(fields: List<String>): ParsedAvuMessage? {
        if (fields.size < 5) return null
        return ParsedAvuMessage.SyncUpdate(
            messageId = fields[0],
            entityType = fields[1],
            entityId = fields[2],
            version = fields[3].toLongOrNull() ?: 0,
            data = fields[4]
        )
    }

    private fun parseSyncDelete(fields: List<String>): ParsedAvuMessage? {
        if (fields.size < 3) return null
        return ParsedAvuMessage.SyncDelete(
            messageId = fields[0],
            entityType = fields[1],
            entityId = fields[2]
        )
    }

    private fun parseSyncFullRequest(fields: List<String>): ParsedAvuMessage? {
        if (fields.size < 3) return null
        return ParsedAvuMessage.SyncFullRequest(
            messageId = fields[0],
            entityTypes = fields[1].split(","),
            lastSyncTimestamp = fields[2].toLongOrNull()
        )
    }

    private fun parseAck(fields: List<String>): ParsedAvuMessage? {
        if (fields.size < 2) return null
        return ParsedAvuMessage.Ack(
            messageId = fields[0],
            timestamp = fields[1].toLongOrNull() ?: 0
        )
    }

    private fun parseError(fields: List<String>): ParsedAvuMessage? {
        if (fields.size < 2) return null
        return ParsedAvuMessage.Error(
            code = fields[0],
            message = fields[1],
            originalMessageId = fields.getOrNull(2)
        )
    }

    private fun parseResponse(fields: List<String>): ParsedAvuMessage? {
        if (fields.size < 2) return null
        return ParsedAvuMessage.Response(
            messageId = fields[0],
            status = fields[1],
            data = fields.getOrNull(2)
        )
    }

    private fun parseEvent(fields: List<String>): ParsedAvuMessage? {
        if (fields.size < 3) return null
        return ParsedAvuMessage.Event(
            type = fields[0],
            source = fields[1],
            data = fields[2]
        )
    }

    private fun parseSyncBatch(fields: List<String>): ParsedAvuMessage? {
        if (fields.size < 3) return null
        val count = fields[1].toIntOrNull() ?: 0
        val operations = fields[2].split("|").mapNotNull { op ->
            val parts = op.split(",")
            if (parts.size >= 5) {
                BatchOperation(
                    entityType = parts[0],
                    entityId = unescape(parts[1]),
                    action = parts[2],
                    version = parts[3].toLongOrNull() ?: 0,
                    data = unescape(parts.drop(4).joinToString(","))
                )
            } else null
        }
        return ParsedAvuMessage.SyncBatch(
            messageId = fields[0],
            count = count,
            operations = operations
        )
    }

    private fun parseSyncConflict(fields: List<String>): ParsedAvuMessage? {
        if (fields.size < 6) return null
        return ParsedAvuMessage.SyncConflict(
            messageId = fields[0],
            entityType = fields[1],
            entityId = fields[2],
            localVersion = fields[3].toLongOrNull() ?: 0,
            remoteVersion = fields[4].toLongOrNull() ?: 0,
            resolution = fields[5]
        )
    }

    private fun parseSyncStatus(fields: List<String>): ParsedAvuMessage? {
        if (fields.size < 4) return null
        return ParsedAvuMessage.SyncStatusMsg(
            sessionId = fields[0],
            state = fields[1],
            pendingCount = fields[2].toIntOrNull() ?: 0,
            lastSyncTimestamp = fields[3].toLongOrNull()
        )
    }

    private fun parseSyncResponse(fields: List<String>): ParsedAvuMessage? {
        if (fields.size < 5) return null
        return ParsedAvuMessage.SyncResponse(
            messageId = fields[0],
            entityType = fields[1],
            entityId = fields[2],
            version = fields[3].toLongOrNull() ?: 0,
            data = fields[4]
        )
    }

    private fun parseConnected(fields: List<String>): ParsedAvuMessage? {
        if (fields.size < 3) return null
        return ParsedAvuMessage.Connected(
            sessionId = fields[0],
            serverVersion = fields[1],
            timestamp = fields[2].toLongOrNull() ?: 0
        )
    }

    private fun parseDisconnected(fields: List<String>): ParsedAvuMessage? {
        if (fields.size < 3) return null
        return ParsedAvuMessage.Disconnected(
            sessionId = fields[0],
            reason = fields[1],
            timestamp = fields[2].toLongOrNull() ?: 0
        )
    }

    private fun parseReconnecting(fields: List<String>): ParsedAvuMessage? {
        if (fields.size < 4) return null
        return ParsedAvuMessage.Reconnecting(
            sessionId = fields[0],
            attempt = fields[1].toIntOrNull() ?: 0,
            maxAttempts = fields[2].toIntOrNull() ?: 0,
            delayMs = fields[3].toLongOrNull() ?: 0
        )
    }

    // ==================== Escape Helpers ====================

    /**
     * Escape reserved characters in AVU format
     */
    fun escape(value: String): String {
        return value
            .replace("%", "%25")  // Escape % first
            .replace(":", "%3A")
            .replace("\n", "%0A")
            .replace("\r", "%0D")
    }

    /**
     * Unescape reserved characters from AVU format
     */
    fun unescape(value: String): String {
        return value
            .replace("%0D", "\r")
            .replace("%0A", "\n")
            .replace("%3A", ":")
            .replace("%25", "%")  // Unescape % last
    }

    /**
     * Generate unique message ID
     */
    fun generateMessageId(): String {
        return "msg_${Clock.System.now().toEpochMilliseconds()}_${(1000..9999).random()}"
    }
}

/**
 * Parsed AVU message types
 */
sealed class ParsedAvuMessage {
    data class Ping(val sessionId: String, val timestamp: Long) : ParsedAvuMessage()
    data class Pong(val sessionId: String, val timestamp: Long) : ParsedAvuMessage()

    data class Handshake(
        val sessionId: String,
        val deviceId: String,
        val appVersion: String,
        val platform: String,
        val userId: String?
    ) : ParsedAvuMessage()

    data class Capability(
        val sessionId: String,
        val capabilities: List<String>
    ) : ParsedAvuMessage()

    data class SyncCreate(
        val messageId: String,
        val entityType: String,
        val entityId: String,
        val version: Long,
        val data: String
    ) : ParsedAvuMessage()

    data class SyncUpdate(
        val messageId: String,
        val entityType: String,
        val entityId: String,
        val version: Long,
        val data: String
    ) : ParsedAvuMessage()

    data class SyncDelete(
        val messageId: String,
        val entityType: String,
        val entityId: String
    ) : ParsedAvuMessage()

    data class SyncFullRequest(
        val messageId: String,
        val entityTypes: List<String>,
        val lastSyncTimestamp: Long?
    ) : ParsedAvuMessage()

    data class Ack(
        val messageId: String,
        val timestamp: Long
    ) : ParsedAvuMessage()

    data class Error(
        val code: String,
        val message: String,
        val originalMessageId: String?
    ) : ParsedAvuMessage()

    data class Response(
        val messageId: String,
        val status: String,
        val data: String?
    ) : ParsedAvuMessage()

    data class Event(
        val type: String,
        val source: String,
        val data: String
    ) : ParsedAvuMessage()

    data class SyncBatch(
        val messageId: String,
        val count: Int,
        val operations: List<BatchOperation>
    ) : ParsedAvuMessage()

    data class SyncConflict(
        val messageId: String,
        val entityType: String,
        val entityId: String,
        val localVersion: Long,
        val remoteVersion: Long,
        val resolution: String
    ) : ParsedAvuMessage()

    data class SyncStatusMsg(
        val sessionId: String,
        val state: String,
        val pendingCount: Int,
        val lastSyncTimestamp: Long?
    ) : ParsedAvuMessage()

    data class SyncResponse(
        val messageId: String,
        val entityType: String,
        val entityId: String,
        val version: Long,
        val data: String
    ) : ParsedAvuMessage()

    data class Connected(
        val sessionId: String,
        val serverVersion: String,
        val timestamp: Long
    ) : ParsedAvuMessage()

    data class Disconnected(
        val sessionId: String,
        val reason: String,
        val timestamp: Long
    ) : ParsedAvuMessage()

    data class Reconnecting(
        val sessionId: String,
        val attempt: Int,
        val maxAttempts: Int,
        val delayMs: Long
    ) : ParsedAvuMessage()

    data class Unknown(
        val prefix: String,
        val fields: List<String>
    ) : ParsedAvuMessage()
}

/**
 * Batch operation for sync batch messages
 */
data class BatchOperation(
    val entityType: String,
    val entityId: String,
    val action: String, // "CREATE", "UPDATE", "DELETE"
    val version: Long,
    val data: String
)
