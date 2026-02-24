package com.augmentalis.remotecast.transport

import com.augmentalis.remotecast.protocol.CastFrameData
import kotlinx.coroutines.flow.StateFlow

/**
 * Abstract transport for sending CAST frames to receivers.
 *
 * Two implementations:
 * - [CastWebSocketServer]: LAN-only WebSocket (fast, no NAT traversal needed)
 * - [CastP2PTransport]: NetAvanue P2P DataChannel (works through NAT/internet)
 *
 * Cast managers use this interface to decouple frame production from transport.
 */
interface CastTransport {
    /** Whether the transport is active and ready to send frames */
    val isRunning: StateFlow<Boolean>

    /** Whether at least one receiver is connected */
    val clientConnected: StateFlow<Boolean>

    /** Start the transport */
    fun start()

    /** Send a CAST-framed JPEG to all connected receivers */
    suspend fun sendFrame(frameData: CastFrameData)

    /** Stop the transport and disconnect all receivers */
    suspend fun stop()
}
