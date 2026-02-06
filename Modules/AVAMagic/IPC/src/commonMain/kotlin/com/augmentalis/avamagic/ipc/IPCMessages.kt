package com.augmentalis.avamagic.ipc

import com.avanues.avu.codec.core.AvuEscape

/**
 * RPC Messages - Unified Message Types for Remote Procedure Communication
 *
 * All message types for Avanues Universal DSL Protocol v2.2
 *
 * Categories:
 * - Base: UniversalMessage sealed class
 * - Enums: ScreenShareDirection, RemoteControlDirection, RecordingStateValue, Role
 * - Requests: Feature initiation (VCA, FTR, SSO, etc.)
 * - Responses: Accept/Decline/Busy/Error
 * - Events: Connection state changes
 * - State: Device state (mic, camera, recording)
 * - Content: Chat, files, whiteboard, UI components
 * - Voice: Speech recognition, commands
 * - Browser: URL sharing, navigation, tabs
 * - AI: Queries and responses
 * - System: Handshake, ping, capabilities
 * - Server: Promotion, roles
 *
 * @author Manoj Jhawar (manoj@ideahq.net)
 */

// ============================================================================
// UTILITY
// ============================================================================

/**
 * Utility object for message serialization helpers.
 *
 * Uses [AvuEscape] for consistent percent-encoding across all modules.
 */
internal object MessageUtils {
    /**
     * Escapes special characters in a string for safe transmission.
     *
     * Uses percent-encoding per AVU RPC Protocol specification:
     * - '%' -> %25 (must be first)
     * - ':' -> %3A
     * - '\n' -> %0A
     * - '\r' -> %0D
     *
     * @param value The string to escape
     * @return The escaped string with special characters encoded
     */
    fun escape(value: String): String = AvuEscape.escape(value)
}

// ============================================================================
// BASE MESSAGE
// ============================================================================

/**
 * Base sealed class for all IPC messages.
 *
 * All messages must extend this class and implement:
 * - [code]: A unique string identifier for the message type
 * - [serialize]: Converts the message to a string for transmission
 */
sealed class UniversalMessage {
    /**
     * The unique message type code (e.g., "VCA", "FTR", "CHT").
     */
    abstract val code: String

    /**
     * Serializes the message to a string format for transmission.
     *
     * @return The serialized message string
     */
    abstract fun serialize(): String

    companion object {
        /**
         * The delimiter used to separate fields in serialized messages.
         */
        const val DELIMITER = ":"
    }
}

// ============================================================================
// ENUMS
// ============================================================================

/**
 * Direction of a screen share session.
 *
 * - [OUTGOING]: This device is sharing its screen to others
 * - [INCOMING]: This device is receiving a screen share from another
 */
enum class ScreenShareDirection {
    /** Sharing screen to remote peer */
    OUTGOING,
    /** Receiving screen from remote peer */
    INCOMING
}

/**
 * Direction of a remote control session.
 *
 * - [OUTGOING]: This device is controlling another device
 * - [INCOMING]: This device is being controlled by another
 */
enum class RemoteControlDirection {
    /** Controlling remote device */
    OUTGOING,
    /** Being controlled by remote device */
    INCOMING
}

/**
 * Recording state values for media recording sessions.
 */
enum class RecordingStateValue {
    /** Start recording */
    START,
    /** Stop recording */
    STOP,
    /** Pause recording */
    PAUSE,
    /** Resume recording */
    RESUME
}

/**
 * Role in a client-server communication model.
 */
enum class Role {
    /** Acting as client */
    CLIENT,
    /** Acting as server */
    SERVER
}

// ============================================================================
// REQUEST MESSAGES
// ============================================================================

/**
 * Request to initiate a video call.
 *
 * Code: VCA
 *
 * @property requestId Unique identifier for this request
 * @property fromDevice Device ID of the caller
 * @property fromName Optional display name of the caller
 */
data class VideoCallRequest(
    val requestId: String,
    val fromDevice: String,
    val fromName: String? = null
) : UniversalMessage() {
    override val code = "VCA"
    override fun serialize() = buildString {
        append("$code$DELIMITER$requestId$DELIMITER${MessageUtils.escape(fromDevice)}")
        fromName?.let { append("$DELIMITER${MessageUtils.escape(it)}") }
    }
}

/**
 * Request to initiate a file transfer.
 *
 * Code: FTR
 *
 * @property requestId Unique identifier for this request
 * @property fileName Name of the file being transferred
 * @property fileSize Size of the file in bytes
 * @property fileCount Number of files being transferred (default: 1)
 */
data class FileTransferRequest(
    val requestId: String,
    val fileName: String,
    val fileSize: Long,
    val fileCount: Int = 1
) : UniversalMessage() {
    override val code = "FTR"
    override fun serialize() = buildString {
        append("$code$DELIMITER$requestId$DELIMITER${MessageUtils.escape(fileName)}$DELIMITER$fileSize")
        if (fileCount > 1) append("$DELIMITER$fileCount")
    }
}

/**
 * Request to initiate screen sharing.
 *
 * Code: SSO (outgoing) or SSI (incoming)
 *
 * @property requestId Unique identifier for this request
 * @property direction Whether sharing out or receiving in
 * @property width Optional screen width in pixels
 * @property height Optional screen height in pixels
 * @property fps Optional frames per second
 */
data class ScreenShareRequest(
    val requestId: String,
    val direction: ScreenShareDirection,
    val width: Int? = null,
    val height: Int? = null,
    val fps: Int? = null
) : UniversalMessage() {
    override val code = if (direction == ScreenShareDirection.OUTGOING) "SSO" else "SSI"
    override fun serialize() = buildString {
        append("$code$DELIMITER$requestId")
        width?.let { append("$DELIMITER$it") }
        height?.let { append("$DELIMITER$it") }
        fps?.let { append("$DELIMITER$it") }
    }
}

/**
 * Request to initiate a whiteboard session.
 *
 * Code: WBS
 *
 * @property requestId Unique identifier for this request
 * @property fromDevice Device ID of the requester
 */
data class WhiteboardRequest(
    val requestId: String,
    val fromDevice: String
) : UniversalMessage() {
    override val code = "WBS"
    override fun serialize() = "$code$DELIMITER$requestId$DELIMITER${MessageUtils.escape(fromDevice)}"
}

/**
 * Request to initiate remote control.
 *
 * Code: RCO (outgoing) or RCI (incoming)
 *
 * @property requestId Unique identifier for this request
 * @property direction Whether controlling or being controlled
 * @property fromDevice Device ID of the requester
 */
data class RemoteControlRequest(
    val requestId: String,
    val direction: RemoteControlDirection,
    val fromDevice: String
) : UniversalMessage() {
    override val code = if (direction == RemoteControlDirection.OUTGOING) "RCO" else "RCI"
    override fun serialize() = "$code$DELIMITER$requestId$DELIMITER${MessageUtils.escape(fromDevice)}"
}

// ============================================================================
// RESPONSE MESSAGES
// ============================================================================

/**
 * Response accepting a request.
 *
 * Code: ACC (simple) or ACD (with data)
 *
 * @property requestId The request ID being accepted
 * @property metadata Optional key-value pairs with additional data
 */
data class AcceptResponse(
    val requestId: String,
    val metadata: Map<String, String>? = null
) : UniversalMessage() {
    override val code = if (metadata.isNullOrEmpty()) "ACC" else "ACD"
    override fun serialize() = buildString {
        append("$code$DELIMITER$requestId")
        metadata?.forEach { (k, v) ->
            append("$DELIMITER$k$DELIMITER${MessageUtils.escape(v)}")
        }
    }
}

/**
 * Response declining a request.
 *
 * Code: DEC (simple) or DCR (with reason)
 *
 * @property requestId The request ID being declined
 * @property reason Optional reason for declining
 */
data class DeclineResponse(
    val requestId: String,
    val reason: String? = null
) : UniversalMessage() {
    override val code = if (reason == null) "DEC" else "DCR"
    override fun serialize() = buildString {
        append("$code$DELIMITER$requestId")
        reason?.let { append("$DELIMITER${MessageUtils.escape(it)}") }
    }
}

/**
 * Response indicating the device is busy.
 *
 * Code: BSY (simple) or BCF (with current feature)
 *
 * @property requestId The request ID being declined due to busy status
 * @property currentFeature Optional name of the feature currently in use
 */
data class BusyResponse(
    val requestId: String,
    val currentFeature: String? = null
) : UniversalMessage() {
    override val code = if (currentFeature == null) "BSY" else "BCF"
    override fun serialize() = buildString {
        append("$code$DELIMITER$requestId")
        currentFeature?.let { append("$DELIMITER${MessageUtils.escape(it)}") }
    }
}

/**
 * Response indicating an error occurred.
 *
 * Code: ERR
 *
 * @property requestId The request ID that caused the error
 * @property errorMessage Description of the error
 */
data class ErrorResponse(
    val requestId: String,
    val errorMessage: String
) : UniversalMessage() {
    override val code = "ERR"
    override fun serialize() = "$code$DELIMITER$requestId$DELIMITER${MessageUtils.escape(errorMessage)}"
}

// ============================================================================
// EVENT MESSAGES
// ============================================================================

/**
 * Event indicating a connection was established.
 *
 * Code: CON
 *
 * @property sessionId The session identifier
 * @property ipAddress Optional IP address of the connected peer
 */
data class ConnectedEvent(
    val sessionId: String,
    val ipAddress: String? = null
) : UniversalMessage() {
    override val code = "CON"
    override fun serialize() = buildString {
        append("$code$DELIMITER$sessionId")
        ipAddress?.let { append("$DELIMITER$it") }
    }
}

/**
 * Event indicating a disconnection occurred.
 *
 * Code: DIS
 *
 * @property sessionId The session identifier
 * @property reason Optional reason for disconnection
 */
data class DisconnectedEvent(
    val sessionId: String,
    val reason: String? = null
) : UniversalMessage() {
    override val code = "DIS"
    override fun serialize() = buildString {
        append("$code$DELIMITER$sessionId")
        reason?.let { append("$DELIMITER${MessageUtils.escape(it)}") }
    }
}

/**
 * Event indicating ICE candidates are ready for WebRTC negotiation.
 *
 * Code: ICE
 *
 * @property sessionId The session identifier
 */
data class ICEReadyEvent(
    val sessionId: String
) : UniversalMessage() {
    override val code = "ICE"
    override fun serialize() = "$code$DELIMITER$sessionId"
}

/**
 * Event indicating a WebRTC data channel has opened.
 *
 * Code: DCO
 *
 * @property sessionId The session identifier
 */
data class DataChannelOpenEvent(
    val sessionId: String
) : UniversalMessage() {
    override val code = "DCO"
    override fun serialize() = "$code$DELIMITER$sessionId"
}

/**
 * Event indicating a WebRTC data channel has closed.
 *
 * Code: DCC
 *
 * @property sessionId The session identifier
 */
data class DataChannelCloseEvent(
    val sessionId: String
) : UniversalMessage() {
    override val code = "DCC"
    override fun serialize() = "$code$DELIMITER$sessionId"
}

// ============================================================================
// STATE MESSAGES
// ============================================================================

/**
 * Message indicating microphone state change.
 *
 * Code: MIC
 *
 * @property sessionId The session identifier
 * @property enabled Whether the microphone is enabled (true) or muted (false)
 */
data class MicrophoneState(
    val sessionId: String,
    val enabled: Boolean
) : UniversalMessage() {
    override val code = "MIC"
    override fun serialize() = "$code$DELIMITER$sessionId$DELIMITER${if (enabled) 1 else 0}"
}

/**
 * Message indicating camera state change.
 *
 * Code: CAM
 *
 * @property sessionId The session identifier
 * @property enabled Whether the camera is enabled (true) or disabled (false)
 */
data class CameraState(
    val sessionId: String,
    val enabled: Boolean
) : UniversalMessage() {
    override val code = "CAM"
    override fun serialize() = "$code$DELIMITER$sessionId$DELIMITER${if (enabled) 1 else 0}"
}

/**
 * Message indicating recording state change.
 *
 * Code: REC
 *
 * @property sessionId The session identifier
 * @property state The recording state (START, STOP, PAUSE, RESUME)
 * @property fileName Optional name of the recording file
 */
data class RecordingState(
    val sessionId: String,
    val state: RecordingStateValue,
    val fileName: String? = null
) : UniversalMessage() {
    override val code = "REC"
    override fun serialize() = buildString {
        append("$code$DELIMITER$sessionId$DELIMITER${state.name.lowercase()}")
        fileName?.let { append("$DELIMITER${MessageUtils.escape(it)}") }
    }
}

// ============================================================================
// CONTENT MESSAGES
// ============================================================================

/**
 * Chat message for text communication.
 *
 * Code: CHT
 *
 * @property messageId Optional unique identifier for the message
 * @property text The chat message text
 */
data class ChatMessage(
    val messageId: String?,
    val text: String
) : UniversalMessage() {
    override val code = "CHT"
    override fun serialize() = "$code$DELIMITER${messageId ?: ""}$DELIMITER${MessageUtils.escape(text)}"
}

/**
 * Message containing a UI component definition in DSL format.
 *
 * Code: JSN
 *
 * @property requestId Unique identifier for this request
 * @property componentDSL The UI component definition in DSL format
 */
data class UIComponentMessage(
    val requestId: String,
    val componentDSL: String
) : UniversalMessage() {
    override val code = "JSN"
    override fun serialize() = "$code$DELIMITER$requestId$DELIMITER$componentDSL"
}

// ============================================================================
// VOICE MESSAGES
// ============================================================================

/**
 * Voice command message for voice-controlled actions.
 *
 * Code: VCM
 *
 * @property commandId Unique identifier for this command
 * @property command The voice command text
 */
data class VoiceCommandMessage(
    val commandId: String,
    val command: String
) : UniversalMessage() {
    override val code = "VCM"
    override fun serialize() = "$code$DELIMITER$commandId$DELIMITER${MessageUtils.escape(command)}"
}

/**
 * Speech-to-text transcription message.
 *
 * Code: STT
 *
 * @property sessionId The session identifier
 * @property text The transcribed text from speech
 */
data class SpeechToTextMessage(
    val sessionId: String,
    val text: String
) : UniversalMessage() {
    override val code = "STT"
    override fun serialize() = "$code$DELIMITER$sessionId$DELIMITER${MessageUtils.escape(text)}"
}

// ============================================================================
// BROWSER MESSAGES
// ============================================================================

/**
 * Message to share a URL with peers.
 *
 * Code: URL
 *
 * @property sessionId The session identifier
 * @property url The URL to share
 */
data class URLShareMessage(
    val sessionId: String,
    val url: String
) : UniversalMessage() {
    override val code = "URL"
    override fun serialize() = "$code$DELIMITER$sessionId$DELIMITER${MessageUtils.escape(url)}"
}

/**
 * Message to navigate to a URL.
 *
 * Code: NAV
 *
 * @property sessionId The session identifier
 * @property url The URL to navigate to
 */
data class NavigateMessage(
    val sessionId: String,
    val url: String
) : UniversalMessage() {
    override val code = "NAV"
    override fun serialize() = "$code$DELIMITER$sessionId$DELIMITER${MessageUtils.escape(url)}"
}

/**
 * Message for browser tab events.
 *
 * Code: TAB
 *
 * @property sessionId The session identifier
 * @property action The tab action (e.g., "open", "close", "switch")
 * @property tabId Optional identifier for the specific tab
 */
data class TabEventMessage(
    val sessionId: String,
    val action: String,
    val tabId: String? = null
) : UniversalMessage() {
    override val code = "TAB"
    override fun serialize() = buildString {
        append("$code$DELIMITER$sessionId$DELIMITER${MessageUtils.escape(action)}")
        tabId?.let { append("$DELIMITER${MessageUtils.escape(it)}") }
    }
}

/**
 * Message indicating a page has finished loading.
 *
 * Code: PLD
 *
 * @property sessionId The session identifier
 * @property url The URL of the loaded page
 */
data class PageLoadedMessage(
    val sessionId: String,
    val url: String
) : UniversalMessage() {
    override val code = "PLD"
    override fun serialize() = "$code$DELIMITER$sessionId$DELIMITER${MessageUtils.escape(url)}"
}

// ============================================================================
// AI MESSAGES
// ============================================================================

/**
 * Message containing an AI query.
 *
 * Code: AIQ
 *
 * @property queryId Unique identifier for this query
 * @property query The query text to send to the AI
 */
data class AIQueryMessage(
    val queryId: String,
    val query: String
) : UniversalMessage() {
    override val code = "AIQ"
    override fun serialize() = "$code$DELIMITER$queryId$DELIMITER${MessageUtils.escape(query)}"
}

/**
 * Message containing an AI response.
 *
 * Code: AIR
 *
 * @property queryId The query ID this response corresponds to
 * @property response The AI response text
 */
data class AIResponseMessage(
    val queryId: String,
    val response: String
) : UniversalMessage() {
    override val code = "AIR"
    override fun serialize() = "$code$DELIMITER$queryId$DELIMITER${MessageUtils.escape(response)}"
}

// ============================================================================
// SYSTEM MESSAGES
// ============================================================================

/**
 * Handshake message for initial connection setup.
 *
 * Code: HND
 *
 * @property protocolVersion Version of the protocol being used
 * @property appVersion Version of the application
 * @property deviceId Unique identifier for the device
 */
data class HandshakeMessage(
    val protocolVersion: String,
    val appVersion: String,
    val deviceId: String
) : UniversalMessage() {
    override val code = "HND"
    override fun serialize() = "$code$DELIMITER$protocolVersion$DELIMITER${MessageUtils.escape(appVersion)}$DELIMITER${MessageUtils.escape(deviceId)}"
}

/**
 * Ping message for connection keep-alive.
 *
 * Code: PNG
 *
 * @property timestamp The timestamp when the ping was sent (milliseconds)
 */
data class PingMessage(
    val timestamp: Long
) : UniversalMessage() {
    override val code = "PNG"
    override fun serialize() = "$code$DELIMITER$timestamp"
}

/**
 * Pong message in response to a ping.
 *
 * Code: PON
 *
 * @property timestamp The timestamp from the original ping
 */
data class PongMessage(
    val timestamp: Long
) : UniversalMessage() {
    override val code = "PON"
    override fun serialize() = "$code$DELIMITER$timestamp"
}

/**
 * Message advertising device capabilities.
 *
 * Code: CAP
 *
 * @property capabilities List of capability identifiers supported by the device
 */
data class CapabilityMessage(
    val capabilities: List<String>
) : UniversalMessage() {
    override val code = "CAP"
    override fun serialize() = "$code$DELIMITER${capabilities.joinToString(",")}"
}

// ============================================================================
// SERVER MESSAGES
// ============================================================================

/**
 * Message for server promotion negotiation.
 *
 * Code: PRO
 *
 * @property deviceId The device ID being promoted
 * @property priority Priority level for the promotion
 * @property timestamp Timestamp of the promotion request
 */
data class PromotionMessage(
    val deviceId: String,
    val priority: Int,
    val timestamp: Long
) : UniversalMessage() {
    override val code = "PRO"
    override fun serialize() = "$code$DELIMITER$deviceId$DELIMITER$priority$DELIMITER$timestamp"
}

/**
 * Message indicating a role change for a device.
 *
 * Code: ROL
 *
 * @property deviceId The device ID whose role changed
 * @property role The new role (CLIENT or SERVER)
 */
data class RoleChangeMessage(
    val deviceId: String,
    val role: Role
) : UniversalMessage() {
    override val code = "ROL"
    override fun serialize() = "$code$DELIMITER$deviceId$DELIMITER${role.name.lowercase()}"
}
