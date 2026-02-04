package com.augmentalis.rpc.ipc.universal

import com.augmentalis.rpc.ipc.*
import kotlinx.coroutines.flow.Flow

/**
 * Universal IPC Manager
 *
 * Platform-agnostic IPC manager for Avanues Universal DSL Protocol.
 *
 * Features:
 * - Send/receive Universal DSL messages
 * - Subscribe to message types
 * - Request-response pattern
 * - Platform adapters (Android, iOS, Web)
 *
 * Usage:
 * ```kotlin
 * val ipcManager = UniversalIPCManager.create(platform)
 *
 * // Send message
 * ipcManager.send(
 *     target = "com.augmentalis.ava",
 *     message = VideoCallRequest("call1", "Pixel7", "Manoj")
 * )
 *
 * // Subscribe to video call requests
 * ipcManager.subscribe<VideoCallRequest>().collect { request ->
 *     showIncomingCallUI(request)
 * }
 *
 * // Request-response
 * val response = ipcManager.request(
 *     target = "com.augmentalis.voiceos",
 *     message = AIQueryMessage("q1", "What's the weather?"),
 *     timeout = 5000
 * )
 * ```
 *
 * @author Manoj Jhawar (manoj@ideahq.net)
 */
interface UniversalIPCManager {

    /**
     * Send message to target app
     *
     * @param target Target app ID (e.g., "com.augmentalis.ava")
     * @param message Universal DSL message
     * @return Result of send operation
     */
    suspend fun send(target: String, message: UniversalMessage): Result<Unit>

    /**
     * Broadcast message to all connected apps
     *
     * @param message Universal DSL message
     * @return Result of broadcast operation
     */
    suspend fun broadcast(message: UniversalMessage): Result<Unit>

    /**
     * Subscribe to specific message type
     *
     * @param T Message type to subscribe to
     * @return Flow of messages
     */
    fun <T : UniversalMessage> subscribe(): Flow<T>

    /**
     * Subscribe to all messages with filter
     *
     * @param filter Message filter
     * @return Flow of messages matching filter
     */
    fun subscribe(filter: MessageFilter): Flow<UniversalMessage>

    /**
     * Request-response pattern
     *
     * Send message and wait for response with matching requestId
     *
     * @param target Target app ID
     * @param message Request message
     * @param timeout Timeout in milliseconds
     * @return Response message or error
     */
    suspend fun request(
        target: String,
        message: UniversalMessage,
        timeout: Long = 10000
    ): Result<UniversalMessage>

    /**
     * Register this app with IPC system
     *
     * @param appId This app's ID
     * @param capabilities App capabilities
     * @return Success or error
     */
    suspend fun register(appId: String, capabilities: List<String>): Result<Unit>

    /**
     * Unregister this app from IPC system
     */
    suspend fun unregister()

    /**
     * Check if target app is available
     *
     * @param target Target app ID
     * @return True if app is available
     */
    suspend fun isAvailable(target: String): Boolean

    /**
     * Get list of connected apps
     *
     * @return List of app IDs
     */
    suspend fun getConnectedApps(): List<String>

    companion object
}

/**
 * Create platform-specific UniversalIPCManager
 *
 * Note: On Android, use AndroidUniversalIPCManager(context) directly
 */
expect fun UniversalIPCManager.Companion.create(): UniversalIPCManager

/**
 * Message filter for subscriptions
 */
data class MessageFilter(
    val sourceApp: String? = null,
    val messageCode: String? = null,
    val messageType: MessageType? = null
) {
    fun matches(message: UniversalMessage, source: String): Boolean {
        if (sourceApp != null && sourceApp != source) return false
        if (messageCode != null && messageCode != message.code) return false
        if (messageType != null && !matchesType(message, messageType)) return false
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
 * Message type categories
 */
enum class MessageType {
    REQUEST,
    RESPONSE,
    EVENT,
    STATE,
    CONTENT
}

/**
 * IPC Error types
 */
sealed class IPCError(
    override val message: String? = null,
    override val cause: Throwable? = null
) : Exception(message, cause) {
    data class TargetNotFound(val target: String) : IPCError("Target not found: $target")
    data class SendFailed(val reason: String, val throwable: Throwable? = null) : IPCError(reason, throwable)
    data class Timeout(val durationMs: Long) : IPCError("Timeout after ${durationMs}ms")
    data class ParseError(val errorMessage: String, val throwable: Throwable? = null) : IPCError(errorMessage, throwable)
    data class NotRegistered(val appId: String) : IPCError("Not registered: $appId")
    data class PermissionDenied(val permission: String) : IPCError("Permission denied: $permission")
}

/**
 * IPC Statistics for monitoring
 */
data class IPCStats(
    val messagesSent: Long = 0,
    val messagesReceived: Long = 0,
    val messagesFailed: Long = 0,
    val averageLatencyMs: Double = 0.0,
    val connectedApps: Int = 0,
    val uptime: Long = 0
)
