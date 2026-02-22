package com.augmentalis.netavanue.transport

import kotlinx.serialization.json.*

/**
 * Minimal Engine.IO + Socket.IO packet codec.
 *
 * Wire format:
 *   Engine.IO: single digit prefix (0=open, 1=close, 2=ping, 3=pong, 4=message)
 *   Socket.IO: digit prefix after EIO message (0=connect, 1=disconnect, 2=event, 3=ack)
 *   Combined example: "42/signaling,["EVENT_NAME",{...}]"
 *     - 4 = EIO message
 *     - 2 = SIO event
 *     - /signaling = namespace
 *     - optional ack id between namespace and data
 *     - JSON array = [eventName, payload]
 */

/** Engine.IO packet types */
enum class EioType(val code: Char) {
    OPEN('0'),
    CLOSE('1'),
    PING('2'),
    PONG('3'),
    MESSAGE('4'),
    UPGRADE('5'),
    NOOP('6');

    companion object {
        fun fromCode(c: Char): EioType? = entries.find { it.code == c }
    }
}

/** Socket.IO packet types (carried inside EIO MESSAGE) */
enum class SioType(val code: Char) {
    CONNECT('0'),
    DISCONNECT('1'),
    EVENT('2'),
    ACK('3'),
    CONNECT_ERROR('4'),
    BINARY_EVENT('5'),
    BINARY_ACK('6');

    companion object {
        fun fromCode(c: Char): SioType? = entries.find { it.code == c }
    }
}

/** Engine.IO open handshake data */
data class EioHandshake(
    val sid: String,
    val upgrades: List<String>,
    val pingInterval: Long,
    val pingTimeout: Long,
)

/** Parsed Socket.IO packet */
data class SioPacket(
    val type: SioType,
    val namespace: String = "/",
    val ackId: Int? = null,
    val data: JsonElement? = null,
)

object SocketIOCodec {
    private val json = Json { ignoreUnknownKeys = true }

    /** Parse an Engine.IO open handshake: "0{...json...}" */
    fun parseEioOpen(raw: String): EioHandshake {
        val jsonStr = raw.substring(1) // strip '0' prefix
        val obj = json.parseToJsonElement(jsonStr).jsonObject
        return EioHandshake(
            sid = obj["sid"]?.jsonPrimitive?.content ?: "",
            upgrades = obj["upgrades"]?.jsonArray?.map { it.jsonPrimitive.content } ?: emptyList(),
            pingInterval = obj["pingInterval"]?.jsonPrimitive?.long ?: 25000,
            pingTimeout = obj["pingTimeout"]?.jsonPrimitive?.long ?: 20000,
        )
    }

    /** Parse a Socket.IO packet from within an EIO MESSAGE ("4" already stripped) */
    fun parseSioPacket(raw: String): SioPacket {
        if (raw.isEmpty()) return SioPacket(SioType.CONNECT)
        val sioType = SioType.fromCode(raw[0]) ?: return SioPacket(SioType.CONNECT)
        var idx = 1

        // Parse namespace (starts with /)
        var namespace = "/"
        if (idx < raw.length && raw[idx] == '/') {
            val commaIdx = raw.indexOf(',', idx)
            namespace = if (commaIdx >= 0) raw.substring(idx, commaIdx) else raw.substring(idx)
            idx = if (commaIdx >= 0) commaIdx + 1 else raw.length
        }

        // Parse optional ack ID (digits before the JSON data)
        var ackId: Int? = null
        val ackStart = idx
        while (idx < raw.length && raw[idx].isDigit()) idx++
        if (idx > ackStart) {
            ackId = raw.substring(ackStart, idx).toIntOrNull()
        }

        // Parse JSON data
        val data = if (idx < raw.length) {
            try {
                json.parseToJsonElement(raw.substring(idx))
            } catch (_: Exception) {
                null
            }
        } else null

        return SioPacket(type = sioType, namespace = namespace, ackId = ackId, data = data)
    }

    /** Encode a Socket.IO event: "42/namespace,ackId[eventName, payload]" */
    fun encodeSioEvent(
        namespace: String,
        eventName: String,
        payload: JsonElement,
        ackId: Int? = null,
    ): String {
        val dataArray = buildJsonArray {
            add(JsonPrimitive(eventName))
            add(payload)
        }
        return buildString {
            append(EioType.MESSAGE.code)
            append(SioType.EVENT.code)
            if (namespace != "/") {
                append(namespace)
                append(',')
            }
            if (ackId != null) append(ackId)
            append(dataArray.toString())
        }
    }

    /** Encode a Socket.IO connect message: "40/namespace," */
    fun encodeSioConnect(namespace: String, auth: JsonElement? = null): String {
        return buildString {
            append(EioType.MESSAGE.code)
            append(SioType.CONNECT.code)
            if (namespace != "/") {
                append(namespace)
                append(',')
            }
            if (auth != null) append(auth.toString())
        }
    }

    /** Encode a Socket.IO ack response: "43/namespace,ackId[data]" */
    fun encodeSioAck(namespace: String, ackId: Int, data: JsonElement): String {
        val dataArray = buildJsonArray { add(data) }
        return buildString {
            append(EioType.MESSAGE.code)
            append(SioType.ACK.code)
            if (namespace != "/") {
                append(namespace)
                append(',')
            }
            append(ackId)
            append(dataArray.toString())
        }
    }
}
