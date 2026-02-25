package com.augmentalis.httpavanue

import com.augmentalis.httpavanue.io.AvanueBuffer
import com.augmentalis.httpavanue.mdns.DnsMessage
import com.augmentalis.httpavanue.mdns.DnsRecord
import com.augmentalis.httpavanue.mdns.MdnsService
import com.augmentalis.httpavanue.mdns.encodeName
import kotlin.test.*

class DnsMessageTest {

    @Test
    fun testNameEncoding() {
        val buffer = AvanueBuffer()
        encodeName(buffer, "myserver._http._tcp.local.")
        val bytes = buffer.toByteArray()

        // First label: "myserver" (8 bytes)
        assertEquals(8.toByte(), bytes[0])
        assertEquals('m'.code.toByte(), bytes[1])

        // Ends with zero byte (root label)
        assertEquals(0.toByte(), bytes[bytes.size - 1])
    }

    @Test
    fun testPtrRecordEncoding() {
        val record = DnsRecord.ptr("_http._tcp.local.", "My Server._http._tcp.local.")
        val encoded = record.encode()
        assertTrue(encoded.isNotEmpty())
    }

    @Test
    fun testSrvRecordEncoding() {
        val record = DnsRecord.srv("My-Server._http._tcp.local.", "My-Server.local.", 8080)
        val encoded = record.encode()
        assertTrue(encoded.isNotEmpty())
    }

    @Test
    fun testTxtRecordEncoding() {
        val record = DnsRecord.txt("test._http._tcp.local.", mapOf("version" to "2.0", "path" to "/api"))
        val encoded = record.encode()
        assertTrue(encoded.isNotEmpty())
        // TXT rdata should contain key=value entries prefixed by length
        val rdataStr = record.rdata.decodeToString()
        assertTrue(rdataStr.contains("version=2.0"))
    }

    @Test
    fun testARecordEncoding() {
        val ipv4 = byteArrayOf(192.toByte(), 168.toByte(), 1, 100)
        val record = DnsRecord.a("myhost.local.", ipv4)
        assertEquals(4, record.rdata.size)
        assertContentEquals(ipv4, record.rdata)
    }

    @Test
    fun testFullMessageEncoding() {
        val ipv4 = byteArrayOf(192.toByte(), 168.toByte(), 1, 42)
        val message = DnsMessage.advertise(
            MdnsService(name = "HTTPAvanue", port = 8080, txt = mapOf("v" to "2.0")),
            ipv4,
        )
        val encoded = message.encode()

        // DNS header is 12 bytes minimum
        assertTrue(encoded.size >= 12)

        // Check header fields
        // ID = 0 (bytes 0-1)
        assertEquals(0.toByte(), encoded[0])
        assertEquals(0.toByte(), encoded[1])

        // Flags = 0x8400 (bytes 2-3) — standard response, authoritative
        assertEquals(0x84.toByte(), encoded[2])
        assertEquals(0x00.toByte(), encoded[3])

        // QDCOUNT = 0 (bytes 4-5)
        assertEquals(0.toByte(), encoded[4])
        assertEquals(0.toByte(), encoded[5])

        // ANCOUNT = 1 (bytes 6-7) — 1 PTR answer
        assertEquals(0.toByte(), encoded[6])
        assertEquals(1.toByte(), encoded[7])
    }

    @Test
    fun testMdnsServiceProperties() {
        val service = MdnsService(name = "My Server", type = "_http._tcp", port = 8080)
        assertEquals("My Server._http._tcp.local.", service.fullName)
        assertEquals("_http._tcp.local.", service.typeDomain)
    }
}
