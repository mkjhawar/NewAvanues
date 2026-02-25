package com.augmentalis.netavanue.signaling

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * All signaling protocol messages exchanged between the NetAvanue KMP client
 * and the AvanueCentral signaling server.
 *
 * Wire format is JSON over Socket.IO. The `type` field discriminates message
 * direction — client messages are emitted as Socket.IO events (event name =
 * message type), server messages arrive as "message" events with a `type` field.
 */

// ─── Enums (mirror AvanueCentral signaling.enums.ts) ────────────────

@Serializable
enum class DevicePlatform { ANDROID, IOS, DESKTOP, WEB }

@Serializable
enum class DeviceType { PHONE, TABLET, DESKTOP, GLASSES, TV }

@Serializable
enum class SessionType { CALL, CAST, FILE_TRANSFER, SCREEN_SHARE }

@Serializable
enum class SessionEndReason { HOST_LEFT, TIMEOUT, ERROR, COMPLETED }

@Serializable
enum class ParticipantRole { HOST, HUB, SPOKE, GUEST }

@Serializable
enum class PairingStatus { @SerialName("active") ACTIVE, @SerialName("revoked") REVOKED }

@Serializable
enum class SignalingLicenseTier { FREE, PRO, BUSINESS, ENTERPRISE }

@Serializable
enum class NetworkType { WIFI, CELLULAR, ETHERNET, UNKNOWN }

@Serializable
enum class HubElectionReason { INITIAL, DISCONNECT, BATTERY_LOW, CAPABILITY_CHANGE }

// ─── Data Models ────────────────────────────────────────────────────

@Serializable
data class DeviceCapabilityDto(
    val cpuCores: Int,
    val ramMb: Int,
    val batteryPercent: Int? = null,
    val isCharging: Boolean? = null,
    val networkType: NetworkType? = null,
    val bandwidthMbps: Int? = null,
    val deviceType: String? = null,
    val screenWidth: Int? = null,
    val screenHeight: Int? = null,
    val supportedCodecs: List<String>? = null,
    val modules: List<String>? = null,
)

@Serializable
data class TurnCredential(
    val username: String,
    val password: String,
    val urls: List<String>,
    val ttlSeconds: Int,
)

@Serializable
data class ParticipantInfo(
    val fingerprint: String,
    val deviceName: String,
    val platform: String,
    val deviceType: String,
    val role: String,
    val capabilityScore: Int,
    val isLicensed: Boolean,
)

// ─── Client → Server Messages ───────────────────────────────────────

@Serializable
data class RegisterDeviceMessage(
    val fingerprint: String,
    val publicKey: String,
    val deviceName: String,
    val platform: DevicePlatform,
    val deviceType: DeviceType = DeviceType.PHONE,
)

@Serializable
data class CreateSessionMessage(
    val fingerprint: String,
    val licenseToken: String,
    val sessionType: SessionType,
    val capabilities: DeviceCapabilityDto,
    val publicKey: String? = null,
    val deviceName: String? = null,
    val platform: String? = null,
    val deviceType: String? = null,
)

@Serializable
data class JoinSessionMessage(
    val inviteCode: String,
    val fingerprint: String,
    val capabilities: DeviceCapabilityDto,
    val publicKey: String? = null,
    val deviceName: String? = null,
    val platform: String? = null,
    val deviceType: String? = null,
)

@Serializable
data class RejoinSessionMessage(
    val sessionId: String,
    val fingerprint: String,
    val signature: String,
    val capabilities: DeviceCapabilityDto,
)

@Serializable
data class LeaveSessionMessage(
    val sessionId: String,
    val fingerprint: String,
)

@Serializable
data class IceCandidateMessage(
    val sessionId: String,
    val fromFingerprint: String,
    val toFingerprint: String,
    val candidate: String,
)

@Serializable
data class SdpOfferMessage(
    val sessionId: String,
    val fromFingerprint: String,
    val toFingerprint: String,
    val sdp: String,
)

@Serializable
data class SdpAnswerMessage(
    val sessionId: String,
    val fromFingerprint: String,
    val toFingerprint: String,
    val sdp: String,
)

@Serializable
data class CapabilityUpdateMessage(
    val sessionId: String,
    val fingerprint: String,
    val capabilities: DeviceCapabilityDto,
)

@Serializable
data class RequestTurnMessage(
    val sessionId: String,
    val fingerprint: String,
)

@Serializable
data class PairRequestMessage(
    val fingerprint: String,
    val targetFingerprint: String,
)

@Serializable
data class PairResponseMessage(
    val fingerprint: String,
    val requestId: String,
)

@Serializable
data class RegisterPushMessage(
    val fingerprint: String,
    val platform: DevicePlatform,
    val token: String,
)

// ─── Server → Client Messages ───────────────────────────────────────

@Serializable
data class DeviceRegisteredEvent(
    val type: String = "DEVICE_REGISTERED",
    val deviceId: String,
)

@Serializable
data class SessionCreatedEvent(
    val type: String = "SESSION_CREATED",
    val sessionId: String,
    val inviteCode: String,
    val hubFingerprint: String,
    val role: String,
    val turnCredentials: TurnCredential? = null,
)

@Serializable
data class SessionJoinedEvent(
    val type: String = "SESSION_JOINED",
    val sessionId: String,
    val participants: List<ParticipantInfo>,
    val hubFingerprint: String,
    val turnCredentials: TurnCredential? = null,
)

@Serializable
data class SessionRejoinedEvent(
    val type: String = "SESSION_REJOINED",
    val sessionId: String,
    val participants: List<ParticipantInfo>,
    val hubFingerprint: String,
)

@Serializable
data class ParticipantJoinedEvent(
    val type: String = "PARTICIPANT_JOINED",
    val sessionId: String,
    val participant: ParticipantInfo,
)

@Serializable
data class ParticipantLeftEvent(
    val type: String = "PARTICIPANT_LEFT",
    val sessionId: String,
    val fingerprint: String,
    val reason: String,
)

@Serializable
data class HubElectedEvent(
    val type: String = "HUB_ELECTED",
    val sessionId: String,
    val hubFingerprint: String,
    val reason: HubElectionReason,
    val score: Int,
)

@Serializable
data class PeerDisconnectedEvent(
    val type: String = "PEER_DISCONNECTED",
    val sessionId: String,
    val fingerprint: String,
    val graceSeconds: Int,
)

@Serializable
data class TurnCredentialsEvent(
    val type: String = "TURN_CREDENTIALS",
    val username: String,
    val password: String,
    val urls: List<String>,
    val ttlSeconds: Int,
)

@Serializable
data class PairRequestedEvent(
    val type: String = "PAIR_REQUESTED",
    val fromFingerprint: String,
    val fromDeviceName: String,
    val requestId: String,
)

@Serializable
data class PairEstablishedEvent(
    val type: String = "PAIR_ESTABLISHED",
    val pairingId: String,
    val peerFingerprint: String,
)

@Serializable
data class SignalingErrorEvent(
    val type: String = "ERROR",
    val code: String,
    val message: String,
)

/**
 * Discriminated union of all server-pushed events.
 * Parsed from the `type` field of incoming "message" events.
 */
sealed class ServerEvent {
    data class DeviceRegistered(val event: DeviceRegisteredEvent) : ServerEvent()
    data class SessionCreated(val event: SessionCreatedEvent) : ServerEvent()
    data class SessionJoined(val event: SessionJoinedEvent) : ServerEvent()
    data class SessionRejoined(val event: SessionRejoinedEvent) : ServerEvent()
    data class ParticipantJoined(val event: ParticipantJoinedEvent) : ServerEvent()
    data class ParticipantLeft(val event: ParticipantLeftEvent) : ServerEvent()
    data class HubElected(val event: HubElectedEvent) : ServerEvent()
    data class PeerDisconnected(val event: PeerDisconnectedEvent) : ServerEvent()
    data class TurnCredentials(val event: TurnCredentialsEvent) : ServerEvent()
    data class PairRequested(val event: PairRequestedEvent) : ServerEvent()
    data class PairEstablished(val event: PairEstablishedEvent) : ServerEvent()
    data class Error(val event: SignalingErrorEvent) : ServerEvent()
    data class Unknown(val type: String, val raw: String) : ServerEvent()
}
