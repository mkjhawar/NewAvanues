package com.augmentalis.rpc.ipc

import com.augmentalis.avucodec.AVUEncoder as BaseEncoder
import com.augmentalis.avucodec.AVUDecoder as BaseDecoder

/**
 * IPC Encoder - Wrapper around AVUEncoder for AvaMagic module.
 *
 * This provides backward compatibility for existing AvaMagic consumers
 * while delegating to the standalone AVUCodec module.
 *
 * @author Augmentalis Engineering
 * @since 1.0.0
 */
object IPCEncoder {
    // Re-export constants from RpcEncoder/AVUEncoder
    const val IPC_ACTION = BaseEncoder.IPC_ACTION
    const val EXTRA_MESSAGE = BaseEncoder.EXTRA_MESSAGE
    const val EXTRA_SOURCE_APP = BaseEncoder.EXTRA_SOURCE_APP

    // Protocol codes
    const val CODE_VOICE_COMMAND = BaseEncoder.CODE_VOICE_COMMAND
    const val CODE_ACCEPT = BaseEncoder.CODE_ACCEPT
    const val CODE_ACCEPT_DATA = BaseEncoder.CODE_ACCEPT_DATA
    const val CODE_DECLINE = BaseEncoder.CODE_DECLINE
    const val CODE_DECLINE_REASON = BaseEncoder.CODE_DECLINE_REASON
    const val CODE_BUSY = BaseEncoder.CODE_BUSY
    const val CODE_BUSY_CALLBACK = BaseEncoder.CODE_BUSY_CALLBACK
    const val CODE_ERROR = BaseEncoder.CODE_ERROR
    const val CODE_CHAT = BaseEncoder.CODE_CHAT
    const val CODE_URL = BaseEncoder.CODE_URL
    const val CODE_NAV = BaseEncoder.CODE_NAV
    const val CODE_AI_QUERY = BaseEncoder.CODE_AI_QUERY
    const val CODE_AI_RESPONSE = BaseEncoder.CODE_AI_RESPONSE
    const val CODE_JSON = BaseEncoder.CODE_JSON
    const val CODE_SPEECH_TO_TEXT = BaseEncoder.CODE_SPEECH_TO_TEXT
    const val CODE_CONNECTED = BaseEncoder.CODE_CONNECTED
    const val CODE_DISCONNECTED = BaseEncoder.CODE_DISCONNECTED
    const val CODE_HANDSHAKE = BaseEncoder.CODE_HANDSHAKE
    const val CODE_PING = BaseEncoder.CODE_PING
    const val CODE_PONG = BaseEncoder.CODE_PONG
    const val CODE_CAPABILITY = BaseEncoder.CODE_CAPABILITY

    // Delegate all methods to AVUEncoder (RpcEncoder compatible)
    fun encodeVoiceCommand(commandId: String, action: String, params: Map<String, Any> = emptyMap()) =
        BaseEncoder.encodeVoiceCommand(commandId, action, params)

    fun encodeAccept(requestId: String, data: String? = null) = BaseEncoder.encodeAccept(requestId, data)
    fun encodeDecline(requestId: String, reason: String? = null) = BaseEncoder.encodeDecline(requestId, reason)
    fun encodeBusy(requestId: String, callbackUrl: String? = null) = BaseEncoder.encodeBusy(requestId, callbackUrl)
    fun encodeError(requestId: String, errorCode: Int, errorMessage: String) = BaseEncoder.encodeError(requestId, errorCode, errorMessage)
    fun encodeError(requestId: String, errorMessage: String) = BaseEncoder.encodeError(requestId, errorMessage)
    fun encodeChat(messageId: String = "", senderId: String = "", text: String) = BaseEncoder.encodeChat(messageId, senderId, text)
    fun encodeUrlShare(sessionId: String, url: String) = BaseEncoder.encodeUrlShare(sessionId, url)
    fun encodeNavigate(sessionId: String, url: String) = BaseEncoder.encodeNavigate(sessionId, url)
    fun encodeAIQuery(queryId: String, query: String, context: String? = null) = BaseEncoder.encodeAIQuery(queryId, query, context)
    fun encodeAIResponse(queryId: String, response: String, confidence: Float? = null) = BaseEncoder.encodeAIResponse(queryId, response, confidence)
    fun encodeJson(requestId: String, jsonOrDsl: String) = BaseEncoder.encodeJson(requestId, jsonOrDsl)
    fun encodeSpeechToText(sessionId: String, transcript: String, confidence: Float, isFinal: Boolean) =
        BaseEncoder.encodeSpeechToText(sessionId, transcript, confidence, isFinal)
    fun encodeHandshake(sessionId: String, appId: String, version: String) = BaseEncoder.encodeHandshake(sessionId, appId, version)
    fun encodePing(sessionId: String, timestamp: Long) = BaseEncoder.encodePing(sessionId, timestamp)
    fun encodePong(sessionId: String, timestamp: Long) = BaseEncoder.encodePong(sessionId, timestamp)
    fun encodeCapabilities(sessionId: String, capabilities: List<String>) = BaseEncoder.encodeCapabilities(sessionId, capabilities)
    fun encodeGeneric(code: String, id: String, vararg params: String) = BaseEncoder.encodeGeneric(code, id, *params)
    fun escape(text: String) = BaseEncoder.escape(text)
    fun unescape(text: String) = BaseEncoder.unescape(text)
    fun isValidMessage(message: String) = BaseEncoder.isValidMessage(message)
    fun extractCode(message: String) = BaseEncoder.extractCode(message)
    fun calculateSizeReduction(ipcMessage: String, jsonEquivalent: String) = BaseEncoder.calculateSizeReduction(ipcMessage, jsonEquivalent)
}

/**
 * Type alias for backward compatibility.
 * Prefer using com.augmentalis.avucodec.AVUEncoder directly.
 */
typealias AVUEncoder = BaseEncoder

@Deprecated("Use RpcEncoder or AVUEncoder instead", ReplaceWith("AVUEncoder"))
typealias UniversalIPCEncoder = BaseEncoder

// Modern name
typealias RpcEncoder = BaseEncoder

/**
 * Type alias for backward compatibility for decoder.
 * Prefer using com.augmentalis.avucodec.AVUDecoder directly.
 */
typealias AVUDecoder = BaseDecoder

@Deprecated("Use AVUDecoder instead", ReplaceWith("AVUDecoder"))
typealias IPCDecoder = BaseDecoder
