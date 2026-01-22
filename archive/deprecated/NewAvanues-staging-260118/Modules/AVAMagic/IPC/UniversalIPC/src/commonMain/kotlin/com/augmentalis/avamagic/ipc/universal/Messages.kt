package com.augmentalis.avamagic.ipc.universal

/**
 * Universal DSL Messages
 *
 * All message types for Avanues Universal DSL Protocol v2.0
 *
 * Categories:
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
// BASE MESSAGE
// ============================================================================

sealed class UniversalMessage {
    abstract val code: String
    abstract fun serialize(): String

    companion object {
        const val DELIMITER = ":"
    }
}

// ============================================================================
// ENUMS
// ============================================================================

enum class ScreenShareDirection { OUTGOING, INCOMING }
enum class RemoteControlDirection { OUTGOING, INCOMING }
enum class RecordingStateValue { START, STOP, PAUSE, RESUME }
enum class Role { CLIENT, SERVER }

// ============================================================================
// REQUEST MESSAGES
// ============================================================================

data class VideoCallRequest(
    val requestId: String,
    val fromDevice: String,
    val fromName: String? = null
) : UniversalMessage() {
    override val code = "VCA"
    override fun serialize() = buildString {
        append("$code$DELIMITER$requestId$DELIMITER${UniversalDSL.escape(fromDevice)}")
        fromName?.let { append("$DELIMITER${UniversalDSL.escape(it)}") }
    }
}

data class FileTransferRequest(
    val requestId: String,
    val fileName: String,
    val fileSize: Long,
    val fileCount: Int = 1
) : UniversalMessage() {
    override val code = "FTR"
    override fun serialize() = buildString {
        append("$code$DELIMITER$requestId$DELIMITER${UniversalDSL.escape(fileName)}$DELIMITER$fileSize")
        if (fileCount > 1) append("$DELIMITER$fileCount")
    }
}

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

data class WhiteboardRequest(
    val requestId: String,
    val fromDevice: String
) : UniversalMessage() {
    override val code = "WBS"
    override fun serialize() = "$code$DELIMITER$requestId$DELIMITER${UniversalDSL.escape(fromDevice)}"
}

data class RemoteControlRequest(
    val requestId: String,
    val direction: RemoteControlDirection,
    val fromDevice: String
) : UniversalMessage() {
    override val code = if (direction == RemoteControlDirection.OUTGOING) "RCO" else "RCI"
    override fun serialize() = "$code$DELIMITER$requestId$DELIMITER${UniversalDSL.escape(fromDevice)}"
}

// ============================================================================
// RESPONSE MESSAGES
// ============================================================================

data class AcceptResponse(
    val requestId: String,
    val metadata: Map<String, String>? = null
) : UniversalMessage() {
    override val code = if (metadata.isNullOrEmpty()) "ACC" else "ACD"
    override fun serialize() = buildString {
        append("$code$DELIMITER$requestId")
        metadata?.forEach { (k, v) ->
            append("$DELIMITER$k$DELIMITER${UniversalDSL.escape(v)}")
        }
    }
}

data class DeclineResponse(
    val requestId: String,
    val reason: String? = null
) : UniversalMessage() {
    override val code = if (reason == null) "DEC" else "DCR"
    override fun serialize() = buildString {
        append("$code$DELIMITER$requestId")
        reason?.let { append("$DELIMITER${UniversalDSL.escape(it)}") }
    }
}

data class BusyResponse(
    val requestId: String,
    val currentFeature: String? = null
) : UniversalMessage() {
    override val code = if (currentFeature == null) "BSY" else "BCF"
    override fun serialize() = buildString {
        append("$code$DELIMITER$requestId")
        currentFeature?.let { append("$DELIMITER${UniversalDSL.escape(it)}") }
    }
}

data class ErrorResponse(
    val requestId: String,
    val errorMessage: String
) : UniversalMessage() {
    override val code = "ERR"
    override fun serialize() = "$code$DELIMITER$requestId$DELIMITER${UniversalDSL.escape(errorMessage)}"
}

// ============================================================================
// EVENT MESSAGES
// ============================================================================

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

data class DisconnectedEvent(
    val sessionId: String,
    val reason: String? = null
) : UniversalMessage() {
    override val code = "DIS"
    override fun serialize() = buildString {
        append("$code$DELIMITER$sessionId")
        reason?.let { append("$DELIMITER${UniversalDSL.escape(it)}") }
    }
}

data class ICEReadyEvent(
    val sessionId: String
) : UniversalMessage() {
    override val code = "ICE"
    override fun serialize() = "$code$DELIMITER$sessionId"
}

data class DataChannelOpenEvent(
    val sessionId: String
) : UniversalMessage() {
    override val code = "DCO"
    override fun serialize() = "$code$DELIMITER$sessionId"
}

data class DataChannelCloseEvent(
    val sessionId: String
) : UniversalMessage() {
    override val code = "DCC"
    override fun serialize() = "$code$DELIMITER$sessionId"
}

// ============================================================================
// STATE MESSAGES
// ============================================================================

data class MicrophoneState(
    val sessionId: String,
    val enabled: Boolean
) : UniversalMessage() {
    override val code = "MIC"
    override fun serialize() = "$code$DELIMITER$sessionId$DELIMITER${if (enabled) 1 else 0}"
}

data class CameraState(
    val sessionId: String,
    val enabled: Boolean
) : UniversalMessage() {
    override val code = "CAM"
    override fun serialize() = "$code$DELIMITER$sessionId$DELIMITER${if (enabled) 1 else 0}"
}

data class RecordingState(
    val sessionId: String,
    val state: RecordingStateValue,
    val fileName: String? = null
) : UniversalMessage() {
    override val code = "REC"
    override fun serialize() = buildString {
        append("$code$DELIMITER$sessionId$DELIMITER${state.name.lowercase()}")
        fileName?.let { append("$DELIMITER${UniversalDSL.escape(it)}") }
    }
}

// ============================================================================
// CONTENT MESSAGES
// ============================================================================

data class ChatMessage(
    val messageId: String?,
    val text: String
) : UniversalMessage() {
    override val code = "CHT"
    override fun serialize() = "$code$DELIMITER${messageId ?: ""}$DELIMITER${UniversalDSL.escape(text)}"
}

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

data class VoiceCommandMessage(
    val commandId: String,
    val command: String
) : UniversalMessage() {
    override val code = "VCM"
    override fun serialize() = "$code$DELIMITER$commandId$DELIMITER${UniversalDSL.escape(command)}"
}

data class SpeechToTextMessage(
    val sessionId: String,
    val text: String
) : UniversalMessage() {
    override val code = "STT"
    override fun serialize() = "$code$DELIMITER$sessionId$DELIMITER${UniversalDSL.escape(text)}"
}

// ============================================================================
// BROWSER MESSAGES
// ============================================================================

data class URLShareMessage(
    val sessionId: String,
    val url: String
) : UniversalMessage() {
    override val code = "URL"
    override fun serialize() = "$code$DELIMITER$sessionId$DELIMITER${UniversalDSL.escape(url)}"
}

data class NavigateMessage(
    val sessionId: String,
    val url: String
) : UniversalMessage() {
    override val code = "NAV"
    override fun serialize() = "$code$DELIMITER$sessionId$DELIMITER${UniversalDSL.escape(url)}"
}

data class TabEventMessage(
    val sessionId: String,
    val action: String,
    val tabId: String? = null
) : UniversalMessage() {
    override val code = "TAB"
    override fun serialize() = buildString {
        append("$code$DELIMITER$sessionId$DELIMITER${UniversalDSL.escape(action)}")
        tabId?.let { append("$DELIMITER${UniversalDSL.escape(it)}") }
    }
}

data class PageLoadedMessage(
    val sessionId: String,
    val url: String
) : UniversalMessage() {
    override val code = "PLD"
    override fun serialize() = "$code$DELIMITER$sessionId$DELIMITER${UniversalDSL.escape(url)}"
}

// ============================================================================
// AI MESSAGES
// ============================================================================

data class AIQueryMessage(
    val queryId: String,
    val query: String
) : UniversalMessage() {
    override val code = "AIQ"
    override fun serialize() = "$code$DELIMITER$queryId$DELIMITER${UniversalDSL.escape(query)}"
}

data class AIResponseMessage(
    val queryId: String,
    val response: String
) : UniversalMessage() {
    override val code = "AIR"
    override fun serialize() = "$code$DELIMITER$queryId$DELIMITER${UniversalDSL.escape(response)}"
}

// ============================================================================
// SYSTEM MESSAGES
// ============================================================================

data class HandshakeMessage(
    val protocolVersion: String,
    val appVersion: String,
    val deviceId: String
) : UniversalMessage() {
    override val code = "HND"
    override fun serialize() = "$code$DELIMITER$protocolVersion$DELIMITER${UniversalDSL.escape(appVersion)}$DELIMITER${UniversalDSL.escape(deviceId)}"
}

data class PingMessage(
    val timestamp: Long
) : UniversalMessage() {
    override val code = "PNG"
    override fun serialize() = "$code$DELIMITER$timestamp"
}

data class PongMessage(
    val timestamp: Long
) : UniversalMessage() {
    override val code = "PON"
    override fun serialize() = "$code$DELIMITER$timestamp"
}

data class CapabilityMessage(
    val capabilities: List<String>
) : UniversalMessage() {
    override val code = "CAP"
    override fun serialize() = "$code$DELIMITER${capabilities.joinToString(",")}"
}

// ============================================================================
// SERVER MESSAGES
// ============================================================================

data class PromotionMessage(
    val deviceId: String,
    val priority: Int,
    val timestamp: Long
) : UniversalMessage() {
    override val code = "PRO"
    override fun serialize() = "$code$DELIMITER$deviceId$DELIMITER$priority$DELIMITER$timestamp"
}

data class RoleChangeMessage(
    val deviceId: String,
    val role: Role
) : UniversalMessage() {
    override val code = "ROL"
    override fun serialize() = "$code$DELIMITER$deviceId$DELIMITER${role.name.lowercase()}"
}
