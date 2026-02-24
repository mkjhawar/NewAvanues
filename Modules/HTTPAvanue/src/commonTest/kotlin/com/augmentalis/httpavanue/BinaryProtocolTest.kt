package com.augmentalis.httpavanue

import com.augmentalis.httpavanue.io.AvanueBuffer
import com.augmentalis.httpavanue.websocket.BinaryFrame
import com.augmentalis.httpavanue.websocket.BinaryProtocol
import com.augmentalis.httpavanue.websocket.BinaryProtocolException
import kotlin.test.*

class BinaryProtocolTest {

    @Test
    fun testEncodeAndDecode() {
        val payload = "Hello Binary".encodeToByteArray()
        val encoded = BinaryProtocol.encode(BinaryProtocol.TYPE_CMD, payload)

        // Create a source from the encoded bytes
        val buffer = AvanueBuffer()
        buffer.write(encoded)
        val source = BufferSource(buffer)

        val frame = BinaryProtocol.decode(source)
        assertNotNull(frame)
        assertEquals(BinaryProtocol.TYPE_CMD, frame.type)
        assertContentEquals(payload, frame.payload)
    }

    @Test
    fun testEncodedFormat() {
        val payload = byteArrayOf(1, 2, 3)
        val encoded = BinaryProtocol.encode(0x01u, payload)

        // Verify header structure: 4 magic + 2 type + 4 length + 3 payload = 13 bytes
        assertEquals(13, encoded.size)

        // Verify magic bytes "AVNE"
        assertEquals(0x41.toByte(), encoded[0]) // 'A'
        assertEquals(0x56.toByte(), encoded[1]) // 'V'
        assertEquals(0x4E.toByte(), encoded[2]) // 'N'
        assertEquals(0x45.toByte(), encoded[3]) // 'E'

        // Verify type (big-endian short)
        assertEquals(0x00.toByte(), encoded[4])
        assertEquals(0x01.toByte(), encoded[5])

        // Verify length (big-endian int = 3)
        assertEquals(0x00.toByte(), encoded[6])
        assertEquals(0x00.toByte(), encoded[7])
        assertEquals(0x00.toByte(), encoded[8])
        assertEquals(0x03.toByte(), encoded[9])
    }

    @Test
    fun testEmptyPayload() {
        val encoded = BinaryProtocol.encode(BinaryProtocol.TYPE_ACK, byteArrayOf())
        val buffer = AvanueBuffer()
        buffer.write(encoded)
        val source = BufferSource(buffer)

        val frame = BinaryProtocol.decode(source)
        assertNotNull(frame)
        assertEquals(BinaryProtocol.TYPE_ACK, frame.type)
        assertEquals(0, frame.payload.size)
    }

    @Test
    fun testInvalidMagicThrows() {
        val buffer = AvanueBuffer()
        buffer.writeInt(0xDEADBEEF.toInt()) // Wrong magic
        buffer.writeShort(1)
        buffer.writeInt(0)
        val source = BufferSource(buffer)

        assertFailsWith<BinaryProtocolException> {
            BinaryProtocol.decode(source)
        }
    }

    @Test
    fun testMultipleFrames() {
        val buffer = AvanueBuffer()
        // Write 3 frames back to back
        buffer.write(BinaryProtocol.encode(BinaryProtocol.TYPE_CAST, "frame1".encodeToByteArray()))
        buffer.write(BinaryProtocol.encode(BinaryProtocol.TYPE_VOCAB, "frame2".encodeToByteArray()))
        buffer.write(BinaryProtocol.encode(BinaryProtocol.TYPE_CMD, "frame3".encodeToByteArray()))
        val source = BufferSource(buffer)

        val frame1 = BinaryProtocol.decode(source)
        val frame2 = BinaryProtocol.decode(source)
        val frame3 = BinaryProtocol.decode(source)

        assertEquals(BinaryProtocol.TYPE_CAST, frame1!!.type)
        assertEquals("frame1", frame1.payload.decodeToString())
        assertEquals(BinaryProtocol.TYPE_VOCAB, frame2!!.type)
        assertEquals("frame2", frame2.payload.decodeToString())
        assertEquals(BinaryProtocol.TYPE_CMD, frame3!!.type)
        assertEquals("frame3", frame3.payload.decodeToString())
    }

    @Test
    fun testEofReturnsNull() {
        val buffer = AvanueBuffer() // empty
        val source = BufferSource(buffer)
        val frame = BinaryProtocol.decode(source)
        assertNull(frame)
    }

    @Test
    fun testFrameEquality() {
        val f1 = BinaryFrame(0x01u, "test".encodeToByteArray())
        val f2 = BinaryFrame(0x01u, "test".encodeToByteArray())
        val f3 = BinaryFrame(0x02u, "test".encodeToByteArray())
        assertEquals(f1, f2)
        assertNotEquals(f1, f3)
    }
}

/**
 * Simple AvanueSource backed by an AvanueBuffer — for testing without network.
 */
private class BufferSource(override val buffer: AvanueBuffer) : com.augmentalis.httpavanue.io.AvanueSource {
    override fun readByte() = buffer.readByte()
    override fun readShort() = buffer.readShort()
    override fun readInt() = buffer.readInt()
    override fun readLong() = buffer.readLong()
    override fun readByteArray(byteCount: Long) = buffer.readByteArray(byteCount)
    override fun readUtf8(byteCount: Long) = buffer.readUtf8(byteCount)
    override fun request(byteCount: Long) = buffer.size >= byteCount
    override fun require(byteCount: Long) {
        if (buffer.size < byteCount) throw com.augmentalis.httpavanue.io.AvanueEofException()
    }
    override fun skip(byteCount: Long) = buffer.skip(byteCount)
    override fun peek(): com.augmentalis.httpavanue.io.AvanueSource = BufferSource(
        AvanueBuffer().also { it.write(buffer.snapshot()) }
    )
    override fun close() {}
}
