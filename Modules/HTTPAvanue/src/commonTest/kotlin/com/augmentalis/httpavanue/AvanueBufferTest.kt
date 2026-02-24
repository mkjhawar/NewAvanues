package com.augmentalis.httpavanue

import com.augmentalis.httpavanue.io.AvanueBuffer
import com.augmentalis.httpavanue.io.AvanueEofException
import kotlin.test.*

class AvanueBufferTest {

    @Test
    fun testWriteAndReadByte() {
        val buffer = AvanueBuffer()
        buffer.writeByte(0x42)
        assertEquals(1L, buffer.size)
        assertEquals(0x42.toByte(), buffer.readByte())
        assertEquals(0L, buffer.size)
    }

    @Test
    fun testWriteAndReadShort() {
        val buffer = AvanueBuffer()
        buffer.writeShort(0x1234)
        assertEquals(2L, buffer.size)
        assertEquals(0x1234.toShort(), buffer.readShort())
    }

    @Test
    fun testWriteAndReadInt() {
        val buffer = AvanueBuffer()
        buffer.writeInt(0x12345678)
        assertEquals(4L, buffer.size)
        assertEquals(0x12345678, buffer.readInt())
    }

    @Test
    fun testWriteAndReadLong() {
        val buffer = AvanueBuffer()
        buffer.writeInt(0x12345678) // write upper 4 bytes via two ints
        buffer.writeInt(0x9ABCDEF0.toInt())
        assertEquals(8L, buffer.size)
        assertEquals(0x123456789ABCDEF0L, buffer.readLong())
    }

    @Test
    fun testWriteAndReadByteArray() {
        val buffer = AvanueBuffer()
        val data = byteArrayOf(1, 2, 3, 4, 5)
        buffer.write(data)
        assertEquals(5L, buffer.size)
        val result = buffer.readByteArray(5)
        assertContentEquals(data, result)
    }

    @Test
    fun testWriteAndReadUtf8() {
        val buffer = AvanueBuffer()
        buffer.writeUtf8("Hello, World!")
        val result = buffer.readUtf8()
        assertEquals("Hello, World!", result)
    }

    @Test
    fun testSnapshot() {
        val buffer = AvanueBuffer()
        buffer.writeUtf8("test")
        val snapshot = buffer.snapshot()
        assertEquals(4, snapshot.size)
        // snapshot doesn't consume
        assertEquals(4L, buffer.size)
        // can still read after snapshot
        assertEquals("test", buffer.readUtf8())
    }

    @Test
    fun testReadUtf8Line() {
        val buffer = AvanueBuffer()
        buffer.writeUtf8("line1\r\nline2\nline3")
        assertEquals("line1", buffer.readUtf8Line())
        assertEquals("line2", buffer.readUtf8Line())
        assertEquals("line3", buffer.readUtf8Line())
    }

    @Test
    fun testReadUtf8LineEmpty() {
        val buffer = AvanueBuffer()
        assertNull(buffer.readUtf8Line())
    }

    @Test
    fun testSkip() {
        val buffer = AvanueBuffer()
        buffer.writeUtf8("abcdef")
        buffer.skip(3)
        assertEquals("def", buffer.readUtf8())
    }

    @Test
    fun testGrowthOnOverflow() {
        val buffer = AvanueBuffer()
        // Write more than initial capacity (256)
        val largeData = ByteArray(1000) { it.toByte() }
        buffer.write(largeData)
        assertEquals(1000L, buffer.size)
        val result = buffer.readByteArray(1000)
        assertContentEquals(largeData, result)
    }

    @Test
    fun testReadBeyondAvailableThrows() {
        val buffer = AvanueBuffer()
        buffer.writeByte(1)
        buffer.readByte() // consume it
        assertFailsWith<AvanueEofException> { buffer.readByte() }
    }

    @Test
    fun testClear() {
        val buffer = AvanueBuffer()
        buffer.writeUtf8("data")
        buffer.clear()
        assertEquals(0L, buffer.size)
    }

    @Test
    fun testWriteWithOffset() {
        val buffer = AvanueBuffer()
        val data = byteArrayOf(0, 1, 2, 3, 4, 5)
        buffer.write(data, 2, 3) // write bytes [2, 3, 4]
        assertEquals(3L, buffer.size)
        val result = buffer.readByteArray(3)
        assertContentEquals(byteArrayOf(2, 3, 4), result)
    }

    @Test
    fun testBigEndianByteOrder() {
        val buffer = AvanueBuffer()
        buffer.writeInt(0x01020304)
        val bytes = buffer.snapshot()
        assertEquals(0x01.toByte(), bytes[0])
        assertEquals(0x02.toByte(), bytes[1])
        assertEquals(0x03.toByte(), bytes[2])
        assertEquals(0x04.toByte(), bytes[3])
    }
}
