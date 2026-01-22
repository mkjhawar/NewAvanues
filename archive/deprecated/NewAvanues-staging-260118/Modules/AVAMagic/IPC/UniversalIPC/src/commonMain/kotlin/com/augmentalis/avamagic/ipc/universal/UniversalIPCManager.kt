package com.augmentalis.avamagic.ipc.universal

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

    companion object {
        /**
         * Create platform-specific IPC manager
         *
         * @return Platform IPC manager
         */
        expect fun create(): UniversalIPCManager
    }
}

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
sealed class IPCError : Exception() {
    data class TargetNotFound(val target: String) : IPCError()
    data class SendFailed(val reason: String, override val cause: Throwable? = null) : IPCError(cause)
    data class Timeout(val durationMs: Long) : IPCError()
    data class ParseError(val message: String, override val cause: Throwable? = null) : IPCError(cause)
    data class NotRegistered(val appId: String) : IPCError()
    data class PermissionDenied(val permission: String) : IPCError()
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
