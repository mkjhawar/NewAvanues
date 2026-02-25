package com.augmentalis.netavanue.peer

import com.augmentalis.netavanue.ice.UdpSocket
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Integration test — creates two DataChannels on localhost and
 * verifies bidirectional message passing over UDP loopback.
 */
class DataChannelLoopbackTest {

    @Test
    fun `text message roundtrip over localhost`() = runTest {
        val socketA = UdpSocket()
        val socketB = UdpSocket()

        try {
            socketA.bind()
            socketB.bind()

            val channelA = DataChannel(
                label = "test-a",
                channelId = 1,
                socket = socketA,
                remoteHost = "127.0.0.1",
                remotePort = socketB.localPort,
            )
            val channelB = DataChannel(
                label = "test-b",
                channelId = 1,
                socket = socketB,
                remoteHost = "127.0.0.1",
                remotePort = socketA.localPort,
            )

            channelA.open(this)
            channelB.open(this)

            // Send from A to B
            channelA.sendText("hello from A")

            // Receive on B
            val received = withTimeout(3000) {
                channelB.textMessages.first()
            }
            assertEquals("hello from A", received)

            channelA.close()
            channelB.close()
        } finally {
            socketA.close()
            socketB.close()
        }
    }

    @Test
    fun `binary message roundtrip over localhost`() = runTest {
        val socketA = UdpSocket()
        val socketB = UdpSocket()

        try {
            socketA.bind()
            socketB.bind()

            val channelA = DataChannel("bin-a", 2, socketA, "127.0.0.1", socketB.localPort)
            val channelB = DataChannel("bin-b", 2, socketB, "127.0.0.1", socketA.localPort)

            channelA.open(this)
            channelB.open(this)

            val testData = byteArrayOf(0xCA.toByte(), 0xFE.toByte(), 0xBA.toByte(), 0xBE.toByte())
            channelA.send(testData)

            val received = withTimeout(3000) {
                channelB.binaryMessages.first()
            }
            assertTrue(testData.contentEquals(received), "Binary data should match")

            channelA.close()
            channelB.close()
        } finally {
            socketA.close()
            socketB.close()
        }
    }

    @Test
    fun `UDP socket bind and localPort`() = runTest {
        val socket = UdpSocket()
        socket.bind()
        assertTrue(socket.localPort > 0, "Should bind to an ephemeral port")
        assertTrue(!socket.isClosed, "Should not be closed")
        socket.close()
        assertTrue(socket.isClosed, "Should be closed after close()")
    }
}
