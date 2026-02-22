package com.augmentalis.netavanue.peer

import com.avanues.logging.LoggerFactory
import com.augmentalis.netavanue.ice.UdpSocket
import com.augmentalis.netavanue.ice.candidate.*
import com.augmentalis.netavanue.ice.stun.StunClient
import com.augmentalis.netavanue.signaling.SignalingClient
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * High-level peer connection manager.
 *
 * Orchestrates ICE candidate gathering, connectivity checks, and data channel
 * establishment. This is the primary developer-facing API for creating
 * peer-to-peer connections.
 *
 * Usage:
 * ```
 * // Offerer (controlling)
 * val pc = PeerConnection(config, signalingClient)
 * pc.start(scope)
 * val offer = pc.createOffer()
 * // send offer via signaling...
 * pc.setRemoteAnswer(remoteSdp)
 * val channel = pc.createDataChannel("cast")
 * channel.sendText("hello")
 *
 * // Answerer (controlled)
 * val pc = PeerConnection(config.copy(isControlling = false), signalingClient)
 * pc.start(scope)
 * pc.setRemoteOffer(remoteSdp)
 * val answer = pc.createAnswer()
 * // send answer via signaling...
 * pc.onDataChannel.collect { channel -> channel.messages.collect { ... } }
 * ```
 */
enum class PeerConnectionState {
    NEW,
    GATHERING,
    CHECKING,
    CONNECTED,
    DISCONNECTED,
    FAILED,
    CLOSED,
}

class PeerConnection(
    private val config: PeerConnectionConfig,
    private val sessionId: String = generateSessionId(),
) {
    private val logger = LoggerFactory.getLogger("PeerConnection")

    private val _state = MutableStateFlow(PeerConnectionState.NEW)
    val state: StateFlow<PeerConnectionState> = _state.asStateFlow()

    private val _localCandidates = MutableStateFlow<List<IceCandidate>>(emptyList())
    val localCandidates: StateFlow<List<IceCandidate>> = _localCandidates.asStateFlow()

    private val _remoteCandidates = MutableStateFlow<List<IceCandidate>>(emptyList())

    private val _onDataChannel = MutableSharedFlow<DataChannel>(extraBufferCapacity = 4)
    val onDataChannel: SharedFlow<DataChannel> = _onDataChannel.asSharedFlow()

    private var socket: UdpSocket? = null
    private var stunClient: StunClient? = null
    private var scope: CoroutineScope? = null
    private val channelMutex = Mutex()
    private val dataChannels = mutableMapOf<Int, DataChannel>()
    private var nominatedPair: IceCandidatePair? = null
    private var nextChannelId = 0

    /**
     * Start the peer connection — binds a UDP socket and prepares for
     * candidate gathering.
     */
    suspend fun start(parentScope: CoroutineScope) {
        scope = CoroutineScope(parentScope.coroutineContext + SupervisorJob())
        socket = UdpSocket().apply { bind() }
        stunClient = StunClient(socket!!)
        logger.i { "PeerConnection started on port ${socket!!.localPort}" }
    }

    /**
     * Create an SDP offer by gathering local ICE candidates.
     * Returns the SDP string to send to the remote peer via signaling.
     */
    suspend fun createOffer(): String {
        _state.value = PeerConnectionState.GATHERING
        val candidates = gatherCandidates()
        _localCandidates.value = candidates
        _state.value = PeerConnectionState.CHECKING
        val sdp = SimpleSdp(
            sessionId = sessionId,
            iceUfrag = config.iceUfrag,
            icePwd = config.icePwd,
            candidates = candidates,
            isOffer = true,
        )
        logger.i { "Created offer with ${candidates.size} candidates" }
        return sdp.encode()
    }

    /**
     * Set the remote peer's SDP offer. Call createAnswer() after this.
     */
    suspend fun setRemoteOffer(sdp: String) {
        val parsed = SimpleSdp.decode(sdp)
        _remoteCandidates.value = parsed.candidates
        logger.i { "Set remote offer: ${parsed.candidates.size} candidates" }
    }

    /**
     * Create an SDP answer after receiving a remote offer.
     * Also starts connectivity checks.
     */
    suspend fun createAnswer(): String {
        _state.value = PeerConnectionState.GATHERING
        val candidates = gatherCandidates()
        _localCandidates.value = candidates
        val sdp = SimpleSdp(
            sessionId = sessionId,
            iceUfrag = config.iceUfrag,
            icePwd = config.icePwd,
            candidates = candidates,
            isOffer = false,
        )
        logger.i { "Created answer with ${candidates.size} candidates" }

        // Start connectivity checks
        scope?.launch { performConnectivityChecks() }

        return sdp.encode()
    }

    /**
     * Set the remote peer's SDP answer. Starts connectivity checks.
     */
    suspend fun setRemoteAnswer(sdp: String) {
        val parsed = SimpleSdp.decode(sdp)
        _remoteCandidates.value = parsed.candidates
        logger.i { "Set remote answer: ${parsed.candidates.size} candidates" }

        // Start connectivity checks
        scope?.launch { performConnectivityChecks() }
    }

    /**
     * Add a remote ICE candidate (trickle ICE — candidate arrives after initial SDP).
     */
    fun addRemoteCandidate(candidate: IceCandidate) {
        _remoteCandidates.value = _remoteCandidates.value + candidate
    }

    /**
     * Create a data channel for sending/receiving data.
     */
    suspend fun createDataChannel(label: String): DataChannel {
        val pair = requireNotNull(nominatedPair) { "No connected pair — wait for PeerConnectionState.CONNECTED" }
        val sock = requireNotNull(socket) { "PeerConnection not started" }
        val sc = requireNotNull(scope) { "PeerConnection not started" }
        val channelId = channelMutex.withLock { nextChannelId++ }
        val channel = DataChannel(
            label = label,
            channelId = channelId,
            socket = sock,
            remoteHost = pair.remote.address,
            remotePort = pair.remote.port,
        )
        channelMutex.withLock { dataChannels[channelId] = channel }
        channel.open(sc)
        return channel
    }

    /**
     * Close the peer connection and all data channels.
     */
    suspend fun close() {
        _state.value = PeerConnectionState.CLOSED
        channelMutex.withLock {
            for (channel in dataChannels.values) {
                channel.close()
            }
            dataChannels.clear()
        }
        socket?.close()
        socket = null
        scope?.cancel()
        scope = null
        logger.i { "PeerConnection closed" }
    }

    /** Gather ICE candidates from local interfaces + STUN servers */
    private suspend fun gatherCandidates(): List<IceCandidate> {
        val candidates = mutableListOf<IceCandidate>()
        val localPort = socket?.localPort ?: return candidates

        // Host candidates from local interfaces
        val hostAddresses = getLocalAddresses()
        for ((idx, addr) in hostAddresses.withIndex()) {
            candidates.add(
                IceCandidate.host(
                    address = addr,
                    port = localPort,
                    localPreference = 65535 - idx,
                )
            )
        }

        // Server-reflexive candidates from STUN servers
        for (stunUrl in config.stunServers) {
            try {
                val (host, port) = parseStunUrl(stunUrl)
                val response = withTimeout(config.gatheringTimeoutMs) {
                    stunClient?.bindingRequest(host, port)
                }
                if (response != null) {
                    val hostCandidate = candidates.firstOrNull()
                    // Only add if different from host (we're behind NAT)
                    if (hostCandidate == null || response.address != hostCandidate.address || response.port != hostCandidate.port) {
                        candidates.add(
                            IceCandidate.serverReflexive(
                                address = response.address,
                                port = response.port,
                                relatedAddress = hostAddresses.firstOrNull() ?: "0.0.0.0",
                                relatedPort = localPort,
                                stunServer = "$host:$port",
                            )
                        )
                    }
                    break // One successful STUN response is enough
                }
            } catch (e: Exception) {
                logger.w { "STUN gathering failed for $stunUrl: ${e.message}" }
            }
        }

        logger.i { "Gathered ${candidates.size} candidates (${candidates.count { it.type == CandidateType.HOST }} host, ${candidates.count { it.type == CandidateType.SERVER_REFLEXIVE }} srflx)" }
        return candidates
    }

    /** Perform ICE connectivity checks on all candidate pairs */
    private suspend fun performConnectivityChecks() {
        _state.value = PeerConnectionState.CHECKING
        val local = _localCandidates.value
        val remote = _remoteCandidates.value

        if (local.isEmpty() || remote.isEmpty()) {
            logger.e { "Cannot check: local=${local.size}, remote=${remote.size}" }
            _state.value = PeerConnectionState.FAILED
            return
        }

        val pairs = IceCandidatePair.formPairs(local, remote, config.isControlling)
        logger.i { "Checking ${pairs.size} candidate pairs" }

        for (pair in pairs) {
            if (_state.value == PeerConnectionState.CONNECTED) break

            pair.transitionTo(PairState.IN_PROGRESS)
            val success = checkPair(pair)
            if (success) {
                pair.transitionTo(PairState.SUCCEEDED)
                pair.nominate()
                nominatedPair = pair
                _state.value = PeerConnectionState.CONNECTED
                logger.i { "Connected via $pair" }
                return
            } else {
                pair.transitionTo(PairState.FAILED)
            }
        }

        logger.e { "All ${pairs.size} candidate pairs failed" }
        _state.value = PeerConnectionState.FAILED
    }

    /** Check a single candidate pair via STUN binding */
    private suspend fun checkPair(pair: IceCandidatePair): Boolean {
        val stun = stunClient ?: return false
        return try {
            withTimeout(config.checkTimeoutMs) {
                val response = stun.bindingRequest(pair.remote.address, pair.remote.port)
                response.address.isNotEmpty()
            }
        } catch (_: Exception) {
            false
        }
    }

    companion object {
        private fun generateSessionId(): String {
            return kotlin.random.Random.nextLong(1_000_000_000L, 9_999_999_999L).toString()
        }
    }
}

/**
 * Get local network interface addresses.
 * This is a common implementation that works on JVM targets.
 * iOS uses a different mechanism.
 */
expect fun getLocalAddresses(): List<String>
