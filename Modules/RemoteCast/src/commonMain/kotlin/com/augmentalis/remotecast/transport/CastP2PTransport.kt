package com.augmentalis.remotecast.transport

import com.avanues.logging.LoggerFactory
import com.augmentalis.netavanue.peer.DataChannel
import com.augmentalis.netavanue.peer.DataChannelState
import com.augmentalis.remotecast.protocol.CastFrameData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * P2P transport for CAST frames using NetAvanue DataChannel.
 *
 * Sends CAST-framed JPEG data over a peer-to-peer DataChannel established
 * by NetAvanue's ICE/STUN connectivity. Works through NAT without needing
 * a direct LAN connection.
 *
 * Usage:
 * ```
 * // After PeerConnection establishes a DataChannel:
 * val transport = CastP2PTransport(scope)
 * transport.addChannel(dataChannel)
 * transport.start()
 * transport.sendFrame(frameData)
 * ```
 */
class CastP2PTransport(
    private val scope: CoroutineScope,
) : CastTransport {
    private val logger = LoggerFactory.getLogger("CastP2P")

    private val channelMutex = Mutex()
    private val channels = mutableListOf<DataChannel>()

    private val _isRunning = MutableStateFlow(false)
    override val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

    private val _clientConnected = MutableStateFlow(false)
    override val clientConnected: StateFlow<Boolean> = _clientConnected.asStateFlow()

    private var frameCount = 0L

    /**
     * Add a DataChannel as a receiver. Can add multiple channels for
     * multi-peer broadcasting (e.g. phone → multiple glasses).
     */
    fun addChannel(channel: DataChannel) {
        scope.launch {
            channelMutex.withLock { channels.add(channel) }
            val hasOpen = channelMutex.withLock { channels.any { it.state.value == DataChannelState.OPEN } }
            _clientConnected.value = hasOpen
            val count = channelMutex.withLock { channels.size }
            logger.i { "P2P receiver added: '${channel.label}' (total: $count)" }

            // Monitor channel state for disconnect detection
            channel.state.collect { state ->
                if (state == DataChannelState.CLOSED) {
                    channelMutex.withLock { channels.remove(channel) }
                    val stillOpen = channelMutex.withLock { channels.any { it.state.value == DataChannelState.OPEN } }
                    _clientConnected.value = stillOpen
                    val remaining = channelMutex.withLock { channels.size }
                    logger.i { "P2P receiver disconnected (remaining: $remaining)" }
                }
            }
        }
    }

    override fun start() {
        _isRunning.value = true
        frameCount = 0L
        logger.i { "CastP2PTransport started" }
    }

    override suspend fun sendFrame(frameData: CastFrameData) {
        if (!_isRunning.value) return
        val openChannels = channelMutex.withLock { channels.filter { it.state.value == DataChannelState.OPEN } }
        if (openChannels.isEmpty()) return

        val packet = CastFrameData.buildPacket(frameData)
        frameCount++

        for (channel in openChannels) {
            try {
                channel.send(packet)
            } catch (e: Exception) {
                logger.w { "Failed to send frame via P2P: ${e.message}" }
            }
        }
    }

    override suspend fun stop() {
        _isRunning.value = false
        channelMutex.withLock {
            for (channel in channels) {
                try { channel.close() } catch (_: Exception) {}
            }
            channels.clear()
        }
        _clientConnected.value = false
        logger.i { "CastP2PTransport stopped ($frameCount frames sent)" }
    }
}
