package com.augmentalis.netavanue.ice.stun

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertNotNull

class StunCodecTest {

    @Test
    fun `binding request encodes valid 20-byte header`() {
        val msg = StunMessage(
            type = StunMessageType(StunMethod.BINDING, StunClass.REQUEST),
            transactionId = StunMessage.generateTransactionId(),
            attributes = emptyList(),
        )
        val encoded = msg.encode()
        // Header is 20 bytes, no attributes
        assertEquals(20, encoded.size)
        // Magic cookie at bytes 4-7
        assertEquals(0x21, encoded[4].toInt() and 0xFF)
        assertEquals(0x12, encoded[5].toInt() and 0xFF)
        assertEquals(0xA4, encoded[6].toInt() and 0xFF)
        assertEquals(0x42, encoded[7].toInt() and 0xFF)
    }

    @Test
    fun `message type encoding for binding request`() {
        val type = StunMessageType(StunMethod.BINDING, StunClass.REQUEST)
        val encoded = type.encode()
        // Binding Request = 0x0001
        assertEquals(0x0001, encoded)
    }

    @Test
    fun `message type encoding for binding success`() {
        val type = StunMessageType(StunMethod.BINDING, StunClass.SUCCESS)
        val encoded = type.encode()
        // Binding Success Response = 0x0101
        assertEquals(0x0101, encoded)
    }

    @Test
    fun `message type encoding for binding error`() {
        val type = StunMessageType(StunMethod.BINDING, StunClass.ERROR)
        val encoded = type.encode()
        // Binding Error Response = 0x0111
        assertEquals(0x0111, encoded)
    }

    @Test
    fun `roundtrip encode-decode preserves message`() {
        val original = StunMessage(
            type = StunMessageType(StunMethod.BINDING, StunClass.REQUEST),
            transactionId = StunMessage.generateTransactionId(),
            attributes = listOf(
                StunAttribute.Software("NetAvanue/1.0"),
            ),
        )
        val encoded = original.encode()
        val decoded = StunMessage.decode(encoded)

        assertEquals(original.type, decoded.type)
        assertTrue(original.transactionId.contentEquals(decoded.transactionId))
        assertEquals(original.attributes.size, decoded.attributes.size)
    }

    @Test
    fun `transaction ID is 12 bytes`() {
        val txId = StunMessage.generateTransactionId()
        assertEquals(12, txId.size)
    }

    @Test
    fun `transaction IDs are unique`() {
        val ids = (1..10).map { StunMessage.generateTransactionId() }
        val uniqueIds = ids.map { it.toList() }.toSet()
        assertEquals(10, uniqueIds.size)
    }

    @Test
    fun `software attribute roundtrip`() {
        val msg = StunMessage(
            type = StunMessageType(StunMethod.BINDING, StunClass.REQUEST),
            transactionId = StunMessage.generateTransactionId(),
            attributes = listOf(StunAttribute.Software("TestAgent/2.0")),
        )
        val decoded = StunMessage.decode(msg.encode())
        val software = decoded.attributes.filterIsInstance<StunAttribute.Software>().firstOrNull()
        assertNotNull(software)
        assertEquals("TestAgent/2.0", software.value)
    }

    @Test
    fun `message type decode roundtrip for all classes`() {
        for (clazz in StunClass.entries) {
            val original = StunMessageType(StunMethod.BINDING, clazz)
            val encoded = original.encode()
            val decoded = StunMessageType.decode(encoded)
            assertEquals(original.method, decoded.method, "Method mismatch for $clazz")
            assertEquals(original.clazz, decoded.clazz, "Class mismatch for $clazz")
        }
    }
}
