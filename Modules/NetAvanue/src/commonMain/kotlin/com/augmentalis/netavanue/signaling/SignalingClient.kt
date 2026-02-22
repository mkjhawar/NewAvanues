package com.augmentalis.netavanue.signaling

import com.avanues.logging.LoggerFactory
import com.augmentalis.netavanue.transport.SocketIOClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.serialization.json.*

/**
 * High-level signaling client for AvanueCentral's signaling server.
 *
 * Wraps [SocketIOClient] with typed Kotlin APIs for device registration,
 * session lifecycle, ICE/SDP relay, capability exchange, and device pairing.
 *
 * Usage:
 * ```
 * val client = SignalingClient("wss://api.avanues.com")
 * client.connect(scope)
 * val session = client.createSession(fingerprint, licenseToken, SessionType.CAST, capabilities)
 * client.serverEvents.collect { event -> handleEvent(event) }
 * ```
 */
class SignalingClient(
    serverUrl: String,
    namespace: String = "/signaling",
) {
    private val logger = LoggerFactory.getLogger("SignalingClient")
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = false }
    private val socketIO = SocketIOClient(serverUrl = serverUrl, namespace = namespace)

    /** Connection state */
    val connectionState: StateFlow<SocketIOClient.State> = socketIO.state

    /** All server events parsed into typed [ServerEvent] instances */
    val serverEvents: SharedFlow<ServerEvent> = socketIO.events.map { (eventName, payload) ->
        if (eventName == "message") parseServerEvent(payload)
        else ServerEvent.Unknown(eventName, payload.toString())
    }.shareIn(
        scope = CoroutineScope(kotlinx.coroutines.Dispatchers.Default),
        started = SharingStarted.WhileSubscribed(),
    )

    /** Filtered flows for specific event types */
    val sessionEvents: Flow<ServerEvent> = serverEvents.filter {
        it is ServerEvent.SessionCreated || it is ServerEvent.SessionJoined ||
            it is ServerEvent.SessionRejoined || it is ServerEvent.ParticipantJoined ||
            it is ServerEvent.ParticipantLeft || it is ServerEvent.HubElected
    }
    val peerEvents: Flow<ServerEvent> = serverEvents.filter {
        it is ServerEvent.PeerDisconnected || it is ServerEvent.TurnCredentials
    }
    val pairingEvents: Flow<ServerEvent> = serverEvents.filter {
        it is ServerEvent.PairRequested || it is ServerEvent.PairEstablished
    }
    val errorEvents: Flow<ServerEvent.Error> = serverEvents.filterIsInstance()

    /** Connect to the signaling server */
    suspend fun connect(scope: CoroutineScope) {
        logger.i { "Connecting to signaling server" }
        socketIO.connect(scope)
    }

    /** Disconnect from the signaling server */
    suspend fun disconnect() {
        logger.i { "Disconnecting from signaling server" }
        socketIO.disconnect()
    }

    // ─── Device Registration ────────────────────────────────────────

    suspend fun registerDevice(msg: RegisterDeviceMessage): DeviceRegisteredEvent? {
        val response = emitWithAck("REGISTER_DEVICE", json.encodeToJsonElement(msg))
        return response?.let { json.decodeFromJsonElement(it) }
    }

    suspend fun registerPushToken(msg: RegisterPushMessage) {
        emit("REGISTER_PUSH", json.encodeToJsonElement(msg))
    }

    // ─── Session Lifecycle ──────────────────────────────────────────

    suspend fun createSession(msg: CreateSessionMessage): SessionCreatedEvent? {
        val response = emitWithAck("CREATE_SESSION", json.encodeToJsonElement(msg))
        return response?.let { json.decodeFromJsonElement(it) }
    }

    suspend fun joinSession(msg: JoinSessionMessage): SessionJoinedEvent? {
        val response = emitWithAck("JOIN_SESSION", json.encodeToJsonElement(msg))
        return response?.let { json.decodeFromJsonElement(it) }
    }

    suspend fun rejoinSession(msg: RejoinSessionMessage): SessionRejoinedEvent? {
        val response = emitWithAck("REJOIN_SESSION", json.encodeToJsonElement(msg))
        return response?.let { json.decodeFromJsonElement(it) }
    }

    suspend fun leaveSession(msg: LeaveSessionMessage) {
        emit("LEAVE_SESSION", json.encodeToJsonElement(msg))
    }

    // ─── ICE / SDP Relay ────────────────────────────────────────────

    suspend fun sendIceCandidate(msg: IceCandidateMessage) {
        emit("ICE_CANDIDATE", json.encodeToJsonElement(msg))
    }

    suspend fun sendSdpOffer(msg: SdpOfferMessage) {
        emit("SDP_OFFER", json.encodeToJsonElement(msg))
    }

    suspend fun sendSdpAnswer(msg: SdpAnswerMessage) {
        emit("SDP_ANSWER", json.encodeToJsonElement(msg))
    }

    // ─── Capabilities ───────────────────────────────────────────────

    suspend fun updateCapabilities(msg: CapabilityUpdateMessage) {
        emit("CAPABILITY_UPDATE", json.encodeToJsonElement(msg))
    }

    suspend fun requestTurnCredentials(msg: RequestTurnMessage): TurnCredentialsEvent? {
        val response = emitWithAck("REQUEST_TURN", json.encodeToJsonElement(msg))
        return response?.let { json.decodeFromJsonElement(it) }
    }

    // ─── Device Pairing ─────────────────────────────────────────────

    suspend fun requestPairing(msg: PairRequestMessage) {
        emit("PAIR_REQUEST", json.encodeToJsonElement(msg))
    }

    suspend fun acceptPairing(msg: PairResponseMessage) {
        emit("PAIR_ACCEPT", json.encodeToJsonElement(msg))
    }

    suspend fun rejectPairing(msg: PairResponseMessage) {
        emit("PAIR_REJECT", json.encodeToJsonElement(msg))
    }

    // ─── Internals ──────────────────────────────────────────────────

    private suspend fun emit(eventName: String, payload: JsonElement) {
        socketIO.emit(eventName, payload)
    }

    private suspend fun emitWithAck(eventName: String, payload: JsonElement): JsonElement? {
        return try {
            socketIO.emitWithAck(eventName, payload)
        } catch (e: Exception) {
            logger.e({ "Error in $eventName: ${e.message}" }, e)
            null
        }
    }

    /** Parse a "message" event into a typed [ServerEvent] */
    private fun parseServerEvent(payload: JsonElement): ServerEvent {
        val obj = payload as? JsonObject ?: return ServerEvent.Unknown("message", payload.toString())
        val type = obj["type"]?.jsonPrimitive?.contentOrNull ?: return ServerEvent.Unknown("message", payload.toString())

        return try {
            when (type) {
                "DEVICE_REGISTERED" -> ServerEvent.DeviceRegistered(json.decodeFromJsonElement(obj))
                "SESSION_CREATED" -> ServerEvent.SessionCreated(json.decodeFromJsonElement(obj))
                "SESSION_JOINED" -> ServerEvent.SessionJoined(json.decodeFromJsonElement(obj))
                "SESSION_REJOINED" -> ServerEvent.SessionRejoined(json.decodeFromJsonElement(obj))
                "PARTICIPANT_JOINED" -> ServerEvent.ParticipantJoined(json.decodeFromJsonElement(obj))
                "PARTICIPANT_LEFT" -> ServerEvent.ParticipantLeft(json.decodeFromJsonElement(obj))
                "HUB_ELECTED" -> ServerEvent.HubElected(json.decodeFromJsonElement(obj))
                "PEER_DISCONNECTED" -> ServerEvent.PeerDisconnected(json.decodeFromJsonElement(obj))
                "TURN_CREDENTIALS" -> ServerEvent.TurnCredentials(json.decodeFromJsonElement(obj))
                "PAIR_REQUESTED" -> ServerEvent.PairRequested(json.decodeFromJsonElement(obj))
                "PAIR_ESTABLISHED" -> ServerEvent.PairEstablished(json.decodeFromJsonElement(obj))
                "ERROR" -> ServerEvent.Error(json.decodeFromJsonElement(obj))
                else -> ServerEvent.Unknown(type, payload.toString())
            }
        } catch (e: Exception) {
            logger.w { "Failed to parse server event '$type': ${e.message}" }
            ServerEvent.Unknown(type, payload.toString())
        }
    }
}
