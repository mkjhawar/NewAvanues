package com.augmentalis.rpc.ipc

import kotlinx.coroutines.flow.Flow

/**
 * IPC Manager
 *
 * Platform-agnostic IPC manager for Avanues Universal DSL Protocol.
 * Merges functionality from UniversalIPCManager interface with high-level
 * connection management capabilities.
 *
 * Features:
 * - Send/receive Universal DSL messages
 * - Subscribe to message types
 * - Request-response pattern
 * - Platform adapters (Android, iOS, Web)
 * - Uses AvuIPCParser for all message serialization/deserialization
 *
 * ## Usage
 * ```kotlin
 * val ipcManager = IPCManager.create()
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
 * @since 1.0.0
 * @author Avanues Platform Team
 */
interface IPCManager {

    /**
     * Send message to target app
     *
     * Serializes the message using AvuIPCParser format before transmission.
     *
     * @param target Target app ID (e.g., "com.augmentalis.ava")
     * @param message Universal DSL message to send
     * @return Result of send operation
     */
    suspend fun send(target: String, message: UniversalMessage): Result<Unit>

    /**
     * Broadcast message to all connected apps
     *
     * @param message Universal DSL message to broadcast
     * @return Result of broadcast operation
     */
    suspend fun broadcast(message: UniversalMessage): Result<Unit>

    /**
     * Subscribe to specific message type
     *
     * @param T Message type to subscribe to (must extend UniversalMessage)
     * @return Flow of messages of the specified type
     */
    fun <T : UniversalMessage> subscribe(): Flow<T>

    /**
     * Subscribe to all messages with filter
     *
     * @param filter Message filter criteria
     * @return Flow of messages matching filter
     */
    fun subscribe(filter: MessageFilter): Flow<UniversalMessage>

    /**
     * Request-response pattern
     *
     * Send message and wait for response with matching requestId.
     * Uses AvuIPCParser for correlation tracking.
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
     * @param appId This app's unique identifier
     * @param capabilities List of capability identifiers this app supports
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
     * @return True if app is available and responding
     */
    suspend fun isAvailable(target: String): Boolean

    /**
     * Get list of connected apps
     *
     * @return List of app IDs currently connected
     */
    suspend fun getConnectedApps(): List<String>

    /**
     * Get current IPC metrics
     *
     * @return Current metrics snapshot
     */
    fun getMetrics(): IPCMetrics

}

/**
 * Create platform-specific IPC manager
 *
 * @return Platform IPC manager instance
 */
expect fun createIPCManager(): IPCManager
