package com.augmentalis.netavanue.ice.stun

import com.augmentalis.netavanue.ice.UdpSocket
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Integration test — sends a real STUN Binding Request to Google's
 * public STUN server and verifies we get a valid server-reflexive address.
 *
 * Requires network access. Skipped in CI if no network.
 */
class StunClientIntegrationTest {

    @Test
    fun `binding request to Google STUN returns valid address`() = runTest {
        val socket = UdpSocket()
        try {
            socket.bind()
            val client = StunClient(socket, maxRetries = 2, initialRtoMs = 1000)
            val response = client.bindingRequest("stun.l.google.com", 19302)

            assertNotNull(response)
            assertTrue(response.address.isNotEmpty(), "Address should not be empty")
            assertTrue(response.port > 0, "Port should be positive")
            assertTrue(response.serverReflexive, "Should be server-reflexive")

            // Verify it's a valid IP format (x.x.x.x)
            val parts = response.address.split(".")
            assertTrue(parts.size == 4, "Should be IPv4 format: ${response.address}")
            parts.forEach { part ->
                val num = part.toIntOrNull()
                assertNotNull(num, "IP octet should be a number: $part")
                assertTrue(num in 0..255, "IP octet should be 0-255: $num")
            }

            println("STUN response: ${response.address}:${response.port}")
        } finally {
            socket.close()
        }
    }

    @Test
    fun `binding request fails gracefully for unreachable server`() = runTest {
        val socket = UdpSocket()
        try {
            socket.bind()
            val client = StunClient(socket, maxRetries = 1, initialRtoMs = 500)
            try {
                client.bindingRequest("192.0.2.1", 3478) // RFC 5737 TEST-NET, never routable
                assertTrue(false, "Should have thrown StunException")
            } catch (e: StunException) {
                assertTrue(e.message?.contains("failed") == true)
            }
        } finally {
            socket.close()
        }
    }
}
