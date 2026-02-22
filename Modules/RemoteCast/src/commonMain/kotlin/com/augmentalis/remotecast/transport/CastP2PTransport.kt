package com.augmentalis.remotecast.transport

import com.avanues.logging.LoggerFactory
import com.augmentalis.netavanue.peer.DataChannel
import com.augmentalis.netavanue.peer.DataChannelState
import com.augmentalis.remotecast.protocol.CastFrameData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

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
        channels.add(channel)
        _clientConnected.value = channels.any { it.state.value == DataChannelState.OPEN }

        // Monitor channel state for disconnect detection
        scope.launch {
            channel.state.collect { state ->
                if (state == DataChannelState.CLOSED) {
                    channels.remove(channel)
                    _clientConnected.value = channels.any { it.state.value == DataChannelState.OPEN }
                    logger.i { "P2P receiver disconnected (remaining: ${channels.size})" }
                }
            }
        }

        logger.i { "P2P receiver added: '${channel.label}' (total: ${channels.size})" }
    }

    override fun start() {
        _isRunning.value = true
        frameCount = 0L
        logger.i { "CastP2PTransport started" }
    }

    override suspend fun sendFrame(frameData: CastFrameData) {
        if (!_isRunning.value) return
        val openChannels = channels.filter { it.state.value == DataChannelState.OPEN }
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
        for (channel in channels) {
            try { channel.close() } catch (_: Exception) {}
        }
        channels.clear()
        _clientConnected.value = false
        logger.i { "CastP2PTransport stopped ($frameCount frames sent)" }
    }
}
