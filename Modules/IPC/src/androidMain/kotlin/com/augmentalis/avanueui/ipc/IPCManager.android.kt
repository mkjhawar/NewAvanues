package com.augmentalis.avanueui.ipc

import android.content.Context
import android.content.Intent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map

/**
 * Android implementation of IPCManager
 *
 * Uses Android Intents for IPC transport and AvuIPCParser for
 * message serialization/deserialization.
 *
 * @since 1.0.0
 * @author Avanues Platform Team
 */
class AndroidIPCManager(
    private val context: Context
) : IPCManager {

    private val messageFlow = MutableSharedFlow<Pair<String, UniversalMessage>>(replay = 0)
    private var registeredAppId: String? = null
    private var startTime: Long = System.currentTimeMillis()

    // Metrics tracking
    private var messagesSent: Long = 0
    private var messagesReceived: Long = 0
    private var messagesFailed: Long = 0

    companion object {
        const val ACTION_UNIVERSAL_IPC = "com.augmentalis.avanueui.IPC.UNIVERSAL"
        const val EXTRA_SOURCE_APP = "source_app"
        const val EXTRA_MESSAGE = "message"
    }

    override suspend fun send(target: String, message: UniversalMessage): Result<Unit> {
        return try {
            val intent = Intent(ACTION_UNIVERSAL_IPC).apply {
                setPackage(target)
                putExtra(EXTRA_SOURCE_APP, registeredAppId)
                putExtra(EXTRA_MESSAGE, message.serialize())
            }
            context.sendBroadcast(intent)
            messagesSent++
            Result.success(Unit)
        } catch (e: Exception) {
            messagesFailed++
            Result.failure(IPCError.SendFailed("Failed to send intent", e))
        }
    }

    override suspend fun broadcast(message: UniversalMessage): Result<Unit> {
        return try {
            val intent = Intent(ACTION_UNIVERSAL_IPC).apply {
                putExtra(EXTRA_SOURCE_APP, registeredAppId)
                putExtra(EXTRA_MESSAGE, message.serialize())
            }
            context.sendBroadcast(intent)
            messagesSent++
            Result.success(Unit)
        } catch (e: Exception) {
            messagesFailed++
            Result.failure(IPCError.SendFailed("Failed to broadcast", e))
        }
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T : UniversalMessage> subscribe(): Flow<T> {
        // Note: Type filtering happens at call site since reified types
        // are not available in interface implementations
        return messageFlow
            .map { it.second } as Flow<T>
    }

    override fun subscribe(filter: MessageFilter): Flow<UniversalMessage> {
        return messageFlow
            .filter { (source, message) -> matchesFilter(message, source, filter) }
            .map { it.second }
    }

    override suspend fun request(
        target: String,
        message: UniversalMessage,
        timeout: Long
    ): Result<UniversalMessage> {
        // TODO: Implement request-response with correlation ID
        return Result.failure(IPCError.SendFailed("Request-response not yet implemented"))
    }

    override suspend fun register(appId: String, capabilities: List<String>): Result<Unit> {
        registeredAppId = appId
        startTime = System.currentTimeMillis()
        // TODO: Register with system IPC registry
        return Result.success(Unit)
    }

    override suspend fun unregister() {
        registeredAppId = null
        // TODO: Unregister from system
    }

    override suspend fun isAvailable(target: String): Boolean {
        // Query package manager to check if app is installed
        return try {
            context.packageManager.getPackageInfo(target, 0)
            true
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun getConnectedApps(): List<String> {
        // TODO: Query IPC registry for connected apps
        return emptyList()
    }

    override fun getMetrics(): IPCMetrics {
        return IPCMetrics(
            messagesSent = messagesSent,
            messagesReceived = messagesReceived,
            messagesFailed = messagesFailed,
            uptime = System.currentTimeMillis() - startTime
        )
    }

    /**
     * Handle incoming Intent from BroadcastReceiver
     *
     * Call this from your BroadcastReceiver:
     * ```kotlin
     * class IPCReceiver : BroadcastReceiver() {
     *     override fun onReceive(context: Context, intent: Intent) {
     *         ipcManager.handleIncomingIntent(intent)
     *     }
     * }
     * ```
     */
    suspend fun handleIncomingIntent(intent: Intent) {
        val sourceApp = intent.getStringExtra(EXTRA_SOURCE_APP) ?: return
        val messageStr = intent.getStringExtra(EXTRA_MESSAGE) ?: return

        val parsed = parseMessage(messageStr)
        if (parsed != null) {
            messagesReceived++
            messageFlow.emit(sourceApp to parsed)
        }
    }

    /**
     * Parse message string using AVU format
     */
    private fun parseMessage(messageStr: String): UniversalMessage? {
        // Parse using the message code prefix
        val parts = messageStr.split(UniversalMessage.DELIMITER, limit = 2)
        if (parts.isEmpty()) return null

        val code = parts[0]
        val data = if (parts.size > 1) parts[1] else ""

        return when (code) {
            "VCA" -> parseVideoCallRequest(data)
            "FTR" -> parseFileTransferRequest(data)
            "ACC", "ACD" -> parseAcceptResponse(data, code == "ACD")
            "DEC", "DCR" -> parseDeclineResponse(data, code == "DCR")
            "CHT" -> parseChatMessage(data)
            "PNG" -> parsePingMessage(data)
            "PON" -> parsePongMessage(data)
            else -> null
        }
    }

    private fun parseVideoCallRequest(data: String): VideoCallRequest? {
        val parts = data.split(UniversalMessage.DELIMITER)
        if (parts.size < 2) return null
        return VideoCallRequest(
            requestId = parts[0],
            fromDevice = unescapeValue(parts[1]),
            fromName = parts.getOrNull(2)?.let { unescapeValue(it) }
        )
    }

    private fun parseFileTransferRequest(data: String): FileTransferRequest? {
        val parts = data.split(UniversalMessage.DELIMITER)
        if (parts.size < 3) return null
        return FileTransferRequest(
            requestId = parts[0],
            fileName = unescapeValue(parts[1]),
            fileSize = parts[2].toLongOrNull() ?: return null,
            fileCount = parts.getOrNull(3)?.toIntOrNull() ?: 1
        )
    }

    private fun parseAcceptResponse(data: String, hasMetadata: Boolean): AcceptResponse {
        val parts = data.split(UniversalMessage.DELIMITER)
        val requestId = parts.firstOrNull() ?: ""
        val metadata = if (hasMetadata && parts.size > 1) {
            val metaMap = mutableMapOf<String, String>()
            var i = 1
            while (i + 1 < parts.size) {
                metaMap[parts[i]] = unescapeValue(parts[i + 1])
                i += 2
            }
            metaMap
        } else null
        return AcceptResponse(requestId, metadata)
    }

    private fun parseDeclineResponse(data: String, hasReason: Boolean): DeclineResponse {
        val parts = data.split(UniversalMessage.DELIMITER)
        val requestId = parts.firstOrNull() ?: ""
        val reason = if (hasReason && parts.size > 1) unescapeValue(parts[1]) else null
        return DeclineResponse(requestId, reason)
    }

    private fun parseChatMessage(data: String): ChatMessage? {
        val parts = data.split(UniversalMessage.DELIMITER, limit = 2)
        if (parts.size < 2) return null
        return ChatMessage(
            messageId = parts[0].ifEmpty { null },
            text = unescapeValue(parts[1])
        )
    }

    private fun parsePingMessage(data: String): PingMessage? {
        val timestamp = data.toLongOrNull() ?: return null
        return PingMessage(timestamp)
    }

    private fun parsePongMessage(data: String): PongMessage? {
        val timestamp = data.toLongOrNull() ?: return null
        return PongMessage(timestamp)
    }

    private fun unescapeValue(value: String): String {
        return value
            .replace("\\n", "\n")
            .replace("\\r", "\r")
            .replace("\\:", ":")
            .replace("\\\\", "\\")
    }

    /**
     * Check if message matches the filter criteria
     */
    private fun matchesFilter(message: UniversalMessage, source: String, filter: MessageFilter): Boolean {
        if (filter.sourceApp != null && filter.sourceApp != source) return false
        if (filter.messageCode != null && filter.messageCode != message.code) return false
        if (filter.messageType != null && !matchesType(message, filter.messageType)) return false
        return true
    }

    private fun matchesType(message: UniversalMessage, type: MessageType): Boolean {
        return when (type) {
            MessageType.REQUEST -> message is VideoCallRequest ||
                    message is FileTransferRequest ||
                    message is ScreenShareRequest ||
                    message is WhiteboardRequest ||
                    message is RemoteControlRequest

            MessageType.RESPONSE -> message is AcceptResponse ||
                    message is DeclineResponse ||
                    message is BusyResponse ||
                    message is ErrorResponse

            MessageType.EVENT -> message is ConnectedEvent ||
                    message is DisconnectedEvent ||
                    message is ICEReadyEvent ||
                    message is DataChannelOpenEvent ||
                    message is DataChannelCloseEvent

            MessageType.STATE -> message is MicrophoneState ||
                    message is CameraState ||
                    message is RecordingState

            MessageType.CONTENT -> message is ChatMessage ||
                    message is UIComponentMessage
        }
    }
}

/**
 * Create Android IPC Manager factory method.
 *
 * Note: This throws on Android since Context is required.
 * Use createIPCManager(context) instead.
 */
actual fun createIPCManager(): IPCManager {
    throw NotImplementedError("Use createIPCManager(context: Context) for Android")
}

/**
 * Create Android IPC Manager with Context
 *
 * @param context Android application or activity context
 * @return AndroidIPCManager instance
 */
fun createIPCManager(context: Context): IPCManager {
    return AndroidIPCManager(context)
}
