# Chapter 109: NetAvanue Peer Networking Module

## Overview

NetAvanue is a KMP peer-to-peer networking module providing signaling, device capability exchange, session management, and device pairing for the Avanues ecosystem. It connects to AvanueCentral's WebSocket signaling server (built in Phases 1-3) to enable NAT traversal, hub election, and peer coordination across Android, iOS, and Desktop platforms.

**Module Location**: `Modules/NetAvanue/`
**Package**: `com.augmentalis.netavanue`
**Dependencies**: HTTPAvanue (WebSocket), Foundation, Logging, kotlinx-serialization/coroutines/datetime
**Targets**: Android, Desktop (JVM), iOS (arm64 + simulatorArm64)

---

## Architecture

```
AvanueCentral (Cloud)          NetAvanue (KMP Client)
┌───────────────────┐          ┌─────────────────────────┐
│ Signaling Module  │   WSS    │  transport/              │
│ (NestJS + Socket.IO)◄───────►│    SocketIOClient        │
│ /signaling ns     │          │    SocketIOPacket        │
│                   │          │  signaling/              │
│ Session Service   │          │    SignalingClient        │
│ Device Service    │          │    SignalingMessage       │
│ Capability Svc    │          │  capability/             │
│ TURN Credential   │          │    DeviceCapability      │
│ Pairing Service   │          │    DeviceFingerprint     │
│ Push Notification │          │    CapabilityCollector   │
└───────────────────┘          │    CapabilityScorer      │
                               │  session/               │
                               │    Session               │
                               │    SessionManager        │
                               │    RoleManager           │
                               │  pairing/               │
                               │    PairingManager        │
                               │    PairedDevice          │
                               └─────────────────────────┘
```

---

## Transport Layer

### Socket.IO Client (`transport/`)

AvanueCentral uses NestJS with Socket.IO (v4). The `SocketIOClient` implements a minimal Socket.IO v4 client protocol on top of HTTPAvanue's raw WebSocket:

1. **Engine.IO**: Handshake (session ID, ping config), ping/pong keepalive
2. **Socket.IO**: Namespace connect, event emit/receive, acknowledgements

Wire format examples:
```
Engine.IO open:     0{"sid":"abc","upgrades":[],"pingInterval":25000}
Socket.IO connect:  40/signaling,
Socket.IO event:    42/signaling,["CREATE_SESSION",{...}]
Socket.IO ack:      42/signaling,5["CREATE_SESSION",{...}]  (5 = ack ID)
Server ack:         43/signaling,5[{...}]
Engine.IO ping:     2
Engine.IO pong:     3
```

Key features:
- **Auto-reconnection** via HTTPAvanue's `WebSocketReconnectConfig`
- **Event flow** as `SharedFlow<Pair<String, JsonElement>>`
- **Ack support** with `CompletableDeferred` + configurable timeout
- **Namespace isolation** (default: `/signaling`)

### Usage

```kotlin
val client = SocketIOClient(
    serverUrl = "wss://api.avanues.com",
    namespace = "/signaling",
)
client.connect(scope)

// Fire-and-forget
client.emit("LEAVE_SESSION", payload)

// Request-response with ack
val response = client.emitWithAck("CREATE_SESSION", payload, timeout = 10_000)
```

---

## Signaling Protocol

### Message Types (`signaling/SignalingMessage.kt`)

All 14 client-to-server and 12 server-to-client message types are defined as `@Serializable` data classes matching AvanueCentral's TypeScript DTOs exactly.

#### Client -> Server

| Event Name | Message Class | Requires License? |
|------------|--------------|-------------------|
| REGISTER_DEVICE | `RegisterDeviceMessage` | No |
| CREATE_SESSION | `CreateSessionMessage` | YES |
| JOIN_SESSION | `JoinSessionMessage` | No |
| REJOIN_SESSION | `RejoinSessionMessage` | No |
| LEAVE_SESSION | `LeaveSessionMessage` | No |
| ICE_CANDIDATE | `IceCandidateMessage` | No |
| SDP_OFFER | `SdpOfferMessage` | No |
| SDP_ANSWER | `SdpAnswerMessage` | No |
| CAPABILITY_UPDATE | `CapabilityUpdateMessage` | No |
| REQUEST_TURN | `RequestTurnMessage` | No |
| PAIR_REQUEST | `PairRequestMessage` | No |
| PAIR_ACCEPT | `PairResponseMessage` | No |
| PAIR_REJECT | `PairResponseMessage` | No |
| REGISTER_PUSH | `RegisterPushMessage` | No |

#### Server -> Client

| Type Field | Kotlin Type | When Sent |
|------------|------------|-----------|
| DEVICE_REGISTERED | `DeviceRegisteredEvent` | After REGISTER_DEVICE |
| SESSION_CREATED | `SessionCreatedEvent` | After CREATE_SESSION (ack) |
| SESSION_JOINED | `SessionJoinedEvent` | After JOIN_SESSION (ack) |
| SESSION_REJOINED | `SessionRejoinedEvent` | After REJOIN_SESSION (ack) |
| PARTICIPANT_JOINED | `ParticipantJoinedEvent` | Broadcast to all |
| PARTICIPANT_LEFT | `ParticipantLeftEvent` | Broadcast to all |
| HUB_ELECTED | `HubElectedEvent` | Broadcast on election |
| PEER_DISCONNECTED | `PeerDisconnectedEvent` | Broadcast (30s grace) |
| TURN_CREDENTIALS | `TurnCredentialsEvent` | On credential refresh |
| PAIR_REQUESTED | `PairRequestedEvent` | To pairing target |
| PAIR_ESTABLISHED | `PairEstablishedEvent` | To both devices |
| ERROR | `SignalingErrorEvent` | On any error |

### SignalingClient (`signaling/SignalingClient.kt`)

High-level typed API wrapping `SocketIOClient`:

```kotlin
val client = SignalingClient("wss://api.avanues.com")
client.connect(scope)

// Create session (requires license)
val session = client.createSession(CreateSessionMessage(
    fingerprint = fp.fingerprint,
    licenseToken = "license_abc",
    sessionType = SessionType.CAST,
    capabilities = caps.toDto(),
))

// Listen for server events
client.serverEvents.collect { event ->
    when (event) {
        is ServerEvent.ParticipantJoined -> updateUI(event.event.participant)
        is ServerEvent.HubElected -> handleHubChange(event.event)
        is ServerEvent.Error -> showError(event.event.message)
        else -> {}
    }
}
```

---

## Device Capabilities

### DeviceCapability (`capability/DeviceCapability.kt`)

Data model for hardware/software info:
- CPU cores, RAM, battery, charging state
- Network type (WiFi/Cellular/Ethernet), bandwidth
- Screen dimensions, device type
- Supported codecs, installed modules

### CapabilityCollector (expect/actual)

| Platform | Implementation | Key APIs |
|----------|---------------|----------|
| Android | `ActivityManager`, `BatteryManager`, `ConnectivityManager`, `WindowManager` | System services |
| Desktop | `Runtime`, `NetworkInterface`, `Toolkit` | JVM standard lib |
| iOS | `NSProcessInfo`, `UIDevice`, `UIScreen` | UIKit + Foundation |

Android requires `initialize(context)` call with application context.

### CapabilityScorer

Identical formula on client and server for consistent hub election:

```
score = cpuCores * 10
      + ramMb / 100
      + (isCharging ? 200 : batteryPercent * 2)
      + bandwidthMbps * 5
      + (isDesktop ? 100 : 0)
      + (hasEthernet ? 50 : 0)
      + screenWidth / 10
      + supportedCodecs.size * 15
```

Desktop devices score highest (plugged in + Ethernet + large screen + more codecs).

### DeviceFingerprint (expect/actual)

Stable device identifier + ECDSA P-256 signing key:

| Platform | Fingerprint Source | Key Storage |
|----------|-------------------|-------------|
| Android | SHA-256(ANDROID_ID + package) | Android Keystore |
| Desktop | SHA-256(MAC addresses + hostname) | `~/.avanues/device_key.{pub,priv}` |
| iOS | SHA-256(identifierForVendor + bundleId) | iOS Keychain (Secure Enclave) |

---

## Session Management

### Session (`session/Session.kt`)

Reactive state container for an active session:
- `participants: StateFlow<List<ParticipantInfo>>` -- live participant list
- `hubFingerprint: StateFlow<String>` -- current hub device
- `myRole: StateFlow<ParticipantRole>` -- own role (HOST/HUB/SPOKE/GUEST)
- `turnCredentials: StateFlow<TurnCredential?>` -- TURN relay creds
- `isActive: StateFlow<Boolean>` -- session lifecycle

### SessionManager (`session/SessionManager.kt`)

Full session lifecycle with auto-updating state:

```kotlin
val manager = SessionManager(client, fingerprint, collector, "Pixel 9", DevicePlatform.ANDROID)
manager.startListening(scope)

// Create (host)
val session = manager.createSession("license_token", SessionType.CAST)
println("Invite: ${session?.inviteCode}") // AVNE-K7M3-P9X2

// Join (guest)
val session = manager.joinSession("AVNE-K7M3-P9X2")

// Observe
manager.currentSession.collect { session ->
    session?.participants?.collect { participants ->
        updateParticipantList(participants)
    }
}

// Leave
manager.leaveSession()
```

### RoleManager (`session/RoleManager.kt`)

Hub election prediction utilities:
- `predictHub(participants)` -- predict hub from capability scores
- `determineLocalRole(myFingerprint, isHost, hubFingerprint)` -- compute own role
- `wouldTriggerReElection(newScore, currentHubScore)` -- predict re-election

---

## Device Pairing

### PairingManager (`pairing/PairingManager.kt`)

Handles device-to-device pairing:

```kotlin
val pairing = PairingManager(client, fingerprint)
pairing.startListening(scope)

// Request pairing
pairing.requestPairing("target_fingerprint")

// Handle incoming requests
pairing.incomingRequests.collect { request ->
    showPairingDialog(request.fromDeviceName) { accepted ->
        if (accepted) pairing.acceptPairing(request.requestId)
        else pairing.rejectPairing(request.requestId)
    }
}

// Check paired devices
pairing.pairedDevices.collect { devices -> updatePairedDeviceList(devices) }
```

---

## Licensing Model

| Tier | Price | Max Peers | TURN Relay | Session Duration |
|------|-------|-----------|------------|------------------|
| Free | $0 | 2 | No (STUN only) | 40 min |
| Pro | $9.99/mo | 10 | Yes (5 GB/mo) | Unlimited |
| Business | $24.99/mo | 50 | Yes (50 GB/mo) | Unlimited |
| Enterprise | Custom | Unlimited | Yes (unlimited) | Unlimited |

Only the session HOST needs a license. Guests join free.

---

## Invite Code Format

```
Format: AVNE-XXXX-XXXX  (8 chars from 30-char unambiguous alphabet)
Alphabet: 23456789ABCDEFGHJKMNPQRSTUVWXYZ (excludes 0/O, 1/I/L)
Combinations: 30^8 = 656 billion
Deep link: avanues://join/{inviteCode}
TTL: same as session (24h max)
```

---

## STUN/ICE Layer (Phase 6)

### STUN Message Codec (`ice/stun/`)

RFC 5389 compliant STUN message encoding/decoding:

| File | Lines | Purpose |
|------|-------|---------|
| `StunMessageType.kt` | 114 | Method + Class encoding with interleaved bits |
| `StunAttribute.kt` | 387 | TLV attributes (MAPPED-ADDRESS, XOR-MAPPED-ADDRESS, ERROR-CODE, SOFTWARE) |
| `StunMessage.kt` | 208 | 20-byte header + attribute list encode/decode |
| `StunClient.kt` | 120 | Binding Request with RFC 5389 retransmission (500ms RTO, 7 retries) |

### UdpSocket (`ice/UdpSocket.kt`)

| Platform | Implementation | Key API |
|----------|---------------|---------|
| Android/Desktop | `java.net.DatagramSocket` | `withContext(Dispatchers.IO)` for blocking I/O |
| iOS | POSIX `socket(AF_INET, SOCK_DGRAM, 0)` | `sendto`/`recvfrom` via cinterop |

### ICE Candidates (`ice/candidate/`)

| Type | Priority | Source |
|------|----------|--------|
| Host | 126 (highest) | Local network interfaces |
| ServerReflexive | 100 | STUN Binding Response |
| PeerReflexive | 110 | Discovered during checks |
| Relay | 0 (lowest) | TURN allocation (future) |

Priority formula: `(2^24 * typePreference) + (2^8 * localPreference) + (256 - componentId)`

---

## PeerConnection Layer (Phase 7)

### PeerConnection (`peer/PeerConnection.kt`)

High-level API for establishing P2P connections:

```kotlin
// Offerer
val pc = PeerConnection(config)
pc.start(scope)
val offer = pc.createOffer()  // Gathers candidates + creates SDP
// Send offer via SignalingClient...
pc.setRemoteAnswer(remoteSdp)
val channel = pc.createDataChannel("cast")
channel.sendText("hello")

// Answerer
val pc = PeerConnection(config.copy(isControlling = false))
pc.start(scope)
pc.setRemoteOffer(remoteSdp)
val answer = pc.createAnswer()  // Gathers + starts connectivity checks
pc.onDataChannel.collect { channel -> ... }
```

### DataChannel (`peer/DataChannel.kt`)

Bidirectional data over the nominated ICE pair:

Frame format: `[2B channel ID][2B flags][4B length][payload]`
- Flags: `0x01` TEXT, `0x02` BINARY, `0x04` CLOSE

```kotlin
val channel = pc.createDataChannel("data")
channel.sendText("hello")
channel.send(jpegBytes)
channel.textMessages.collect { msg -> println(msg) }
channel.binaryMessages.collect { frame -> renderFrame(frame) }
```

### SimpleSdp (`peer/SimpleSdp.kt`)

Minimal SDP for data-only connections (no codec negotiation):
- ICE credentials (ufrag + pwd)
- Candidate list
- Setup role (actpass for offer, active for answer)

---

## RemoteCast Integration (Phase 8)

### CastTransport Interface

```kotlin
interface CastTransport {
    val isRunning: StateFlow<Boolean>
    val clientConnected: StateFlow<Boolean>
    fun start()
    suspend fun sendFrame(frameData: CastFrameData)
    suspend fun stop()
}
```

### Two Transport Paths

| Transport | Class | When Used |
|-----------|-------|-----------|
| LAN WebSocket | `CastWebSocketServer` | Same network, port 54321 |
| P2P DataChannel | `CastP2PTransport` | Through NAT, via NetAvanue ICE |

Cast managers can use either transport interchangeably:
```kotlin
// LAN mode (existing)
val transport: CastTransport = CastWebSocketServer(port = 54321, scope = scope)

// P2P mode (new)
val transport: CastTransport = CastP2PTransport(scope).apply {
    addChannel(dataChannel)  // From PeerConnection
}
```

---

## File Inventory

```
Modules/NetAvanue/
├── build.gradle.kts
├── src/
│   ├── commonMain/kotlin/com/augmentalis/netavanue/
│   │   ├── transport/
│   │   │   ├── SocketIOClient.kt         (215 lines)
│   │   │   └── SocketIOPacket.kt         (160 lines)
│   │   ├── signaling/
│   │   │   ├── SignalingClient.kt        (170 lines)
│   │   │   └── SignalingMessage.kt       (255 lines)
│   │   ├── capability/
│   │   │   ├── DeviceCapability.kt       (50 lines)
│   │   │   ├── DeviceFingerprint.kt      (25 lines, expect)
│   │   │   └── CapabilityScorer.kt       (45 lines)
│   │   ├── ice/
│   │   │   ├── UdpSocket.kt             (84 lines, expect)
│   │   │   ├── stun/
│   │   │   │   ├── StunMessageType.kt   (114 lines)
│   │   │   │   ├── StunAttribute.kt     (387 lines)
│   │   │   │   ├── StunMessage.kt       (208 lines)
│   │   │   │   └── StunClient.kt        (120 lines)
│   │   │   └── candidate/
│   │   │       ├── IceCandidate.kt      (140 lines)
│   │   │       └── IceCandidatePair.kt  (105 lines)
│   │   ├── peer/
│   │   │   ├── PeerConnection.kt        (220 lines)
│   │   │   ├── PeerConnectionConfig.kt  (55 lines)
│   │   │   ├── DataChannel.kt           (155 lines)
│   │   │   └── SimpleSdp.kt            (115 lines)
│   │   ├── session/
│   │   │   ├── Session.kt               (80 lines)
│   │   │   ├── SessionManager.kt        (180 lines)
│   │   │   └── RoleManager.kt           (50 lines)
│   │   └── pairing/
│   │       ├── PairedDevice.kt           (15 lines)
│   │       └── PairingManager.kt         (100 lines)
│   ├── commonTest/kotlin/.../
│   │   ├── capability/CapabilityScorerTest.kt  (70 lines)
│   │   ├── ice/stun/StunCodecTest.kt           (90 lines)
│   │   ├── ice/candidate/IceCandidateTest.kt   (65 lines)
│   │   └── peer/SimpleSdpTest.kt               (55 lines)
│   ├── androidMain/ (UdpSocket, CapabilityCollector, DeviceFingerprint, TimeMillis, LocalAddresses)
│   ├── desktopMain/ (same)
│   └── iosMain/ (same)
Total: ~4,800 lines across 44 files, 24 tests

RemoteCast Integration:
├── CastTransport.kt      (interface)
├── CastP2PTransport.kt   (NetAvanue DataChannel transport)
└── CastWebSocketServer.kt (now implements CastTransport)
```

---

## Phase Status

| Phase | What | Status |
|-------|------|--------|
| 1-3 | AvanueCentral signaling backend | COMPLETE |
| 4-5 | KMP module + signaling client + session management | COMPLETE |
| 6 | STUN codec + UdpSocket + StunClient + ICE candidates | COMPLETE |
| 7 | PeerConnection + DataChannel + SimpleSdp | COMPLETE |
| 8 | RemoteCast P2P transport integration | COMPLETE |
| 9 | Web/JS target (browser RTCPeerConnection wrapper) | Future |
| 10 | End-to-end testing across NAT | Future |

---

## Related Documents

- NetAvanue Core Plan: `docs/plans/NetAvanue/NetAvanue-Plan-PeerNetworkingModule-260222-V1.md`
- Phase 6-10 Plan: `docs/plans/NetAvanue/NetAvanue-Plan-Phase6-10-Implementation-260223-V1.md`
- AvanueCentral Signaling Plan: `docs/plans/NetAvanue/NetAvanue-Plan-AvanueCentralSignalingIntegration-260222-V1.md`
- HTTPAvanue v2.0 (Chapter 104): `Docs/MasterDocs/HTTPAvanue/Developer-Manual-Chapter104-HTTPAvanueV2ZeroDepEnhancements.md`
- RemoteCast Architecture: `docs/plans/RemoteCast/RemoteCast-Spec-GlassClientArchitecture-260219-V1.md`
- AvanueCentral Signaling Module: `packages/api/src/modules/signaling/` (2,523 lines, fully implemented)
