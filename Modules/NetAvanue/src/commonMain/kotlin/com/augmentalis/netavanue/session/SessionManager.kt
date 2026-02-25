package com.augmentalis.netavanue.session

import com.avanues.logging.LoggerFactory
import com.augmentalis.netavanue.capability.CapabilityCollector
import com.augmentalis.netavanue.capability.DeviceFingerprint
import com.augmentalis.netavanue.signaling.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * Manages the full session lifecycle: create, join, rejoin, leave.
 *
 * Listens to [SignalingClient.serverEvents] and automatically updates the
 * current [Session] state (participants, hub, role changes). Consumers
 * observe session state via reactive [StateFlow]s.
 *
 * Usage:
 * ```
 * val manager = SessionManager(signalingClient, fingerprint, collector)
 * val session = manager.createSession(licenseToken, SessionType.CAST)
 * manager.currentSession.collect { session -> ... }
 * ```
 */
class SessionManager(
    private val client: SignalingClient,
    private val fingerprint: DeviceFingerprint,
    private val capabilityCollector: CapabilityCollector,
    private val deviceName: String,
    private val platform: DevicePlatform,
    private val deviceType: DeviceType = DeviceType.PHONE,
) {
    private val logger = LoggerFactory.getLogger("SessionManager")

    private val _currentSession = MutableStateFlow<Session?>(null)
    val currentSession: StateFlow<Session?> = _currentSession.asStateFlow()

    /** Start listening for server events. Call once after SignalingClient.connect(). */
    fun startListening(scope: CoroutineScope) {
        scope.launch { observeSessionEvents() }
    }

    /** Register this device with the signaling server (first-time or refresh). */
    suspend fun registerDevice(): DeviceRegisteredEvent? {
        return client.registerDevice(
            RegisterDeviceMessage(
                fingerprint = fingerprint.fingerprint,
                publicKey = fingerprint.publicKey,
                deviceName = deviceName,
                platform = platform,
                deviceType = deviceType,
            )
        )
    }

    /** Create a new session (requires valid license). Returns the session or null on error. */
    suspend fun createSession(
        licenseToken: String,
        sessionType: SessionType,
    ): Session? {
        val caps = capabilityCollector.collect()
        val response = client.createSession(
            CreateSessionMessage(
                fingerprint = fingerprint.fingerprint,
                licenseToken = licenseToken,
                sessionType = sessionType,
                capabilities = caps.toDto(),
                publicKey = fingerprint.publicKey,
                deviceName = deviceName,
                platform = platform.name,
                deviceType = deviceType.name,
            )
        ) ?: return null

        val session = Session(
            sessionId = response.sessionId,
            inviteCode = response.inviteCode,
            sessionType = sessionType,
            initialRole = ParticipantRole.valueOf(response.role),
            initialHubFingerprint = response.hubFingerprint,
            turnCredentials = response.turnCredentials,
        )
        _currentSession.value = session
        logger.i { "Created session ${session.sessionId} (invite: ${session.inviteCode})" }
        return session
    }

    /** Join an existing session by invite code. Returns the session or null on error. */
    suspend fun joinSession(inviteCode: String): Session? {
        val caps = capabilityCollector.collect()
        val response = client.joinSession(
            JoinSessionMessage(
                inviteCode = inviteCode,
                fingerprint = fingerprint.fingerprint,
                capabilities = caps.toDto(),
                publicKey = fingerprint.publicKey,
                deviceName = deviceName,
                platform = platform.name,
                deviceType = deviceType.name,
            )
        ) ?: return null

        val session = Session(
            sessionId = response.sessionId,
            inviteCode = inviteCode,
            sessionType = SessionType.CALL, // Server doesn't echo type in join response
            initialRole = ParticipantRole.GUEST,
            initialHubFingerprint = response.hubFingerprint,
            turnCredentials = response.turnCredentials,
        )
        session.updateParticipants(response.participants)
        _currentSession.value = session
        logger.i { "Joined session ${session.sessionId} (${response.participants.size} participants)" }
        return session
    }

    /** Rejoin a session after disconnect (uses signed fingerprint for auth). */
    suspend fun rejoinSession(sessionId: String): Session? {
        val caps = capabilityCollector.collect()
        val signature = fingerprint.sign(sessionId.encodeToByteArray())
        val response = client.rejoinSession(
            RejoinSessionMessage(
                sessionId = sessionId,
                fingerprint = fingerprint.fingerprint,
                signature = signature,
                capabilities = caps.toDto(),
            )
        ) ?: return null

        val session = Session(
            sessionId = response.sessionId,
            inviteCode = "",
            sessionType = SessionType.CALL,
            initialRole = ParticipantRole.GUEST,
            initialHubFingerprint = response.hubFingerprint,
        )
        session.updateParticipants(response.participants)
        _currentSession.value = session
        logger.i { "Rejoined session ${session.sessionId}" }
        return session
    }

    /** Leave the current session gracefully. */
    suspend fun leaveSession() {
        val session = _currentSession.value ?: return
        client.leaveSession(
            LeaveSessionMessage(
                sessionId = session.sessionId,
                fingerprint = fingerprint.fingerprint,
            )
        )
        session.end()
        _currentSession.value = null
        logger.i { "Left session ${session.sessionId}" }
    }

    /** Send updated capabilities to the server (e.g. battery/network changed). */
    suspend fun refreshCapabilities() {
        val session = _currentSession.value ?: return
        val caps = capabilityCollector.collect()
        client.updateCapabilities(
            CapabilityUpdateMessage(
                sessionId = session.sessionId,
                fingerprint = fingerprint.fingerprint,
                capabilities = caps.toDto(),
            )
        )
    }

    /** Observe server events and update the current session state. */
    private suspend fun observeSessionEvents() {
        client.serverEvents.collect { event ->
            val session = _currentSession.value ?: return@collect
            when (event) {
                is ServerEvent.ParticipantJoined -> {
                    if (event.event.sessionId == session.sessionId) {
                        session.addParticipant(event.event.participant)
                        logger.d { "Participant joined: ${event.event.participant.deviceName}" }
                    }
                }
                is ServerEvent.ParticipantLeft -> {
                    if (event.event.sessionId == session.sessionId) {
                        session.removeParticipant(event.event.fingerprint)
                        logger.d { "Participant left: ${event.event.fingerprint}" }
                    }
                }
                is ServerEvent.HubElected -> {
                    if (event.event.sessionId == session.sessionId) {
                        session.updateHub(event.event.hubFingerprint)
                        if (event.event.hubFingerprint == fingerprint.fingerprint) {
                            session.updateRole(ParticipantRole.HUB)
                        }
                        logger.i { "Hub elected: ${event.event.hubFingerprint} (${event.event.reason})" }
                    }
                }
                is ServerEvent.TurnCredentials -> {
                    session.updateTurnCredentials(
                        TurnCredential(
                            username = event.event.username,
                            password = event.event.password,
                            urls = event.event.urls,
                            ttlSeconds = event.event.ttlSeconds,
                        )
                    )
                }
                is ServerEvent.PeerDisconnected -> {
                    logger.w { "Peer disconnected: ${event.event.fingerprint} (grace: ${event.event.graceSeconds}s)" }
                }
                is ServerEvent.Error -> {
                    logger.e { "Signaling error: ${event.event.code} - ${event.event.message}" }
                }
                else -> { /* handled by other managers */ }
            }
        }
    }
}
