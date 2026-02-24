package com.augmentalis.netavanue.transport

import kotlinx.serialization.json.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class SocketIOCodecTest {

    @Test
    fun `EIO open packet parses correctly`() {
        val raw = """0{"sid":"abc123","upgrades":["websocket"],"pingInterval":25000,"pingTimeout":20000}"""
        val handshake = SocketIOCodec.parseEioOpen(raw)
        assertEquals("abc123", handshake.sid)
        assertEquals(listOf("websocket"), handshake.upgrades)
        assertEquals(25000L, handshake.pingInterval)
        assertEquals(20000L, handshake.pingTimeout)
    }

    @Test
    fun `SIO connect encodes for default namespace`() {
        val encoded = SocketIOCodec.encodeSioConnect("/")
        assertEquals("40", encoded)
    }

    @Test
    fun `SIO connect encodes for custom namespace`() {
        val encoded = SocketIOCodec.encodeSioConnect("/signaling")
        assertEquals("40/signaling,", encoded)
    }

    @Test
    fun `SIO connect with auth encodes correctly`() {
        val auth = buildJsonObject { put("token", "abc") }
        val encoded = SocketIOCodec.encodeSioConnect("/signaling", auth)
        assertEquals("""40/signaling,{"token":"abc"}""", encoded)
    }

    @Test
    fun `SIO event encodes without ack`() {
        val payload = buildJsonObject { put("fingerprint", "abc123") }
        val encoded = SocketIOCodec.encodeSioEvent("/signaling", "CREATE_SESSION", payload)
        assertTrue(encoded.startsWith("42/signaling,"))
        assertTrue(encoded.contains("CREATE_SESSION"))
        assertTrue(encoded.contains("abc123"))
    }

    @Test
    fun `SIO event encodes with ack id`() {
        val payload = buildJsonObject { put("key", "val") }
        val encoded = SocketIOCodec.encodeSioEvent("/signaling", "TEST", payload, ackId = 5)
        assertTrue(encoded.startsWith("42/signaling,5"))
    }

    @Test
    fun `SIO event for default namespace omits namespace`() {
        val payload = JsonPrimitive("hello")
        val encoded = SocketIOCodec.encodeSioEvent("/", "greet", payload)
        assertTrue(encoded.startsWith("42["))
    }

    @Test
    fun `SIO packet parses connect response`() {
        val packet = SocketIOCodec.parseSioPacket("""0/signaling,{"sid":"xyz"}""")
        assertEquals(SioType.CONNECT, packet.type)
        assertEquals("/signaling", packet.namespace)
        assertNotNull(packet.data)
    }

    @Test
    fun `SIO packet parses event with namespace`() {
        val raw = """2/signaling,["message",{"type":"SESSION_CREATED","sessionId":"123"}]"""
        val packet = SocketIOCodec.parseSioPacket(raw)
        assertEquals(SioType.EVENT, packet.type)
        assertEquals("/signaling", packet.namespace)
        val arr = packet.data?.jsonArray
        assertNotNull(arr)
        assertEquals("message", arr[0].jsonPrimitive.content)
    }

    @Test
    fun `SIO packet parses ack with id`() {
        val raw = """3/signaling,5[{"type":"SESSION_CREATED"}]"""
        val packet = SocketIOCodec.parseSioPacket(raw)
        assertEquals(SioType.ACK, packet.type)
        assertEquals("/signaling", packet.namespace)
        assertEquals(5, packet.ackId)
        assertNotNull(packet.data)
    }

    @Test
    fun `SIO packet parses connect error`() {
        val raw = """4/signaling,{"message":"Invalid namespace"}"""
        val packet = SocketIOCodec.parseSioPacket(raw)
        assertEquals(SioType.CONNECT_ERROR, packet.type)
        assertEquals("/signaling", packet.namespace)
    }

    @Test
    fun `SIO ack encodes correctly`() {
        val data = buildJsonObject { put("ok", true) }
        val encoded = SocketIOCodec.encodeSioAck("/signaling", 7, data)
        assertTrue(encoded.startsWith("43/signaling,7"))
        assertTrue(encoded.contains("ok"))
    }

    @Test
    fun `EIO type codes are correct`() {
        assertEquals('0', EioType.OPEN.code)
        assertEquals('1', EioType.CLOSE.code)
        assertEquals('2', EioType.PING.code)
        assertEquals('3', EioType.PONG.code)
        assertEquals('4', EioType.MESSAGE.code)
    }

    @Test
    fun `SIO type codes are correct`() {
        assertEquals('0', SioType.CONNECT.code)
        assertEquals('1', SioType.DISCONNECT.code)
        assertEquals('2', SioType.EVENT.code)
        assertEquals('3', SioType.ACK.code)
        assertEquals('4', SioType.CONNECT_ERROR.code)
    }

    @Test
    fun `roundtrip event encode then parse`() {
        val payload = buildJsonObject {
            put("fingerprint", "abc")
            put("licenseToken", "xyz")
        }
        val encoded = SocketIOCodec.encodeSioEvent("/signaling", "CREATE_SESSION", payload, ackId = 42)
        // Strip the "4" EIO prefix to get the SIO payload
        val sioPayload = encoded.substring(1)
        val packet = SocketIOCodec.parseSioPacket(sioPayload)

        assertEquals(SioType.EVENT, packet.type)
        assertEquals("/signaling", packet.namespace)
        assertEquals(42, packet.ackId)
        val arr = packet.data?.jsonArray
        assertNotNull(arr)
        assertEquals("CREATE_SESSION", arr[0].jsonPrimitive.content)
        assertEquals("abc", arr[1].jsonObject["fingerprint"]?.jsonPrimitive?.content)
    }
}
