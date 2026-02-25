# NetAvanue — KMP Peer-to-Peer Networking Module

**Module**: NetAvanue (NEW — `Modules/NetAvanue/`)
**Type**: Plan
**Date**: 2026-02-22
**Version**: V1
**Status**: PENDING — Requires dedicated session
**Author**: Manoj Jhawar

---

## Vision

NetAvanue is a **KMP peer-to-peer networking library** providing STUN, TURN, ICE, and WebRTC signaling capabilities. It enables NAT traversal, direct peer connections, and relay fallback across all platforms (Android, iOS, Desktop, Web). Combined with HTTPAvanue, it forms the complete network services platform for the Avanues ecosystem.

No equivalent exists in the Kotlin Multiplatform ecosystem — this would be a first.

---

## Architecture

### Module Relationship

```
Modules/HTTPAvanue/     ← HTTP/1.1+2, WebSocket, REST, middleware
Modules/NetAvanue/      ← STUN, TURN, ICE, signaling, NAT traversal
    └── depends on HTTPAvanue (WebSocket signaling, AvanueIO, mDNS)
```

### Consumer Dependency Flow

```
RemoteCast   → HTTPAvanue (WebSocket cast) + NetAvanue (NAT traversal)
AvaConnect   → NetAvanue (P2P connection) + HTTPAvanue (signaling server)
GlassAvanue  → RemoteCast → both
Cockpit      → RemoteCast → both
Avanues App  → RemoteCast → both
Third-party  → Pick either or both independently
```

---

## Feature Inventory

### Tier 1: STUN Client + Server (RFC 5389)

**Purpose**: Discover public IP:port behind NAT. Required for ICE candidate gathering.

| Component | Description | Lines (est.) |
|-----------|-------------|-------------|
| `StunMessage.kt` | 20-byte header + attribute encoding/decoding | ~120 |
| `StunAttributes.kt` | MAPPED-ADDRESS, XOR-MAPPED-ADDRESS, ERROR-CODE, etc. | ~100 |
| `StunClient.kt` | Send Binding Request, receive Binding Response | ~60 |
| `StunServer.kt` | Listen for requests, respond with reflexive address | ~80 |
| `StunTransaction.kt` | Transaction ID tracking, retransmission, timeout | ~50 |

**Protocols**: UDP (primary), TCP (fallback)

### Tier 2: TURN Client + Server (RFC 5766)

**Purpose**: Relay data when direct P2P fails (symmetric NAT, firewall). TURN is an extension of STUN.

| Component | Description | Lines (est.) |
|-----------|-------------|-------------|
| `TurnClient.kt` | Allocate relay, create permissions, channel binding | ~150 |
| `TurnServer.kt` | Manage allocations, relay data, enforce quotas | ~200 |
| `TurnAllocation.kt` | Relay address lifecycle, 5-tuple tracking | ~60 |
| `TurnChannel.kt` | Channel-bound data forwarding (efficient path) | ~40 |
| `TurnAuth.kt` | Long-term credential mechanism (HMAC-SHA1) | ~60 |

**Protocols**: UDP (primary), TCP (fallback), TLS (secure)

### Tier 3: ICE Agent (RFC 5245)

**Purpose**: Coordinate STUN+TURN to find the best working network path between two peers.

| Component | Description | Lines (est.) |
|-----------|-------------|-------------|
| `IceAgent.kt` | Full ICE agent — gathering, checking, nomination | ~200 |
| `IceCandidate.kt` | Host, server-reflexive, relay candidates | ~40 |
| `IceCandidatePair.kt` | Pair prioritization, state machine | ~60 |
| `IceConnectivityCheck.kt` | STUN binding on each candidate pair | ~80 |
| `IceGatherer.kt` | Gather candidates from all interfaces + STUN + TURN | ~80 |

**Modes**: Full ICE (both sides check), ICE-Lite (server-side only)

### Tier 4: WebRTC Signaling

**Purpose**: Exchange SDP offers/answers and ICE candidates between peers. Uses HTTPAvanue WebSocket.

| Component | Description | Lines (est.) |
|-----------|-------------|-------------|
| `SignalingClient.kt` | WebSocket-based signaling channel | ~60 |
| `SignalingServer.kt` | Room-based signaling hub (matches peers) | ~100 |
| `SdpMessage.kt` | SDP offer/answer parsing and generation | ~120 |
| `SignalingProtocol.kt` | JSON message format (offer, answer, candidate, bye) | ~40 |

### Tier 5: Peer Connection Manager

**Purpose**: High-level API that orchestrates everything — developer-facing.

| Component | Description | Lines (est.) |
|-----------|-------------|-------------|
| `PeerConnection.kt` | Create connection, exchange offers, establish data channel | ~100 |
| `DataChannel.kt` | Bidirectional data channel (ordered/unordered, reliable/unreliable) | ~80 |
| `PeerConnectionConfig.kt` | STUN/TURN server URLs, ICE configuration, timeouts | ~30 |

### Tier 6: Device Capability + Role Election

**Purpose**: Peers advertise hardware/software capabilities, automatically elect the most capable device as hub/server. Enables persistent calls that survive device disconnections via re-election.

| Component | Description | Lines (est.) |
|-----------|-------------|-------------|
| `DeviceCapability.kt` | Device fingerprint: CPU, RAM, battery, camera, mic, network, codecs, modules | ~80 |
| `DeviceFingerprint.kt` | Unique stable device ID (survives app reinstall) + signing key pair | ~60 |
| `CapabilityScorer.kt` | Score calculation: hardware + battery + network + codec support | ~50 |
| `RoleElection.kt` | Hub election algorithm — highest score wins, auto re-election on disconnect/degradation | ~80 |
| `CapabilityExchange.kt` | Secure capability broadcast: signed with device key, verified by all peers | ~70 |
| `GroupTopology.kt` | Star (hub-spoke) / Mesh / Hybrid topology management | ~60 |
| `SessionPersistence.kt` | Call persistence: reconnect to same session after network change, hub migration | ~60 |

**Key features**:
- **Device fingerprint**: Stable ID per device (platform-specific: Android ID, iOS identifierForVendor, Desktop MAC hash). Used for re-identification on reconnect.
- **Signed capability messages**: Each device generates an Ed25519 key pair on first run. Capability advertisements are signed — peers verify signatures to prevent spoofing.
- **Automatic hub election**: Highest `CapabilityScore` becomes hub. If hub disconnects, next-highest takes over with zero user intervention. Open sessions migrate seamlessly.
- **Battery-aware re-election**: Hub automatically yields when battery drops below threshold (configurable, default 20%). Plugged-in devices get bonus score.
- **Persistent sessions**: Session state (participants, topology, active channels) survives network changes. Peers reconnect to the same session via session ID + device fingerprint. No new handshake needed — just ICE restart.
- **Capability change events**: If a device plugs in/unplugs, switches WiFi/cellular, or a module becomes available, it re-broadcasts capabilities. May trigger re-election.

**Scoring formula**:
```
score = cpuCores × 10
      + ramMb / 100
      + (batteryPercent × 2 if !charging, else 200)
      + bandwidthMbps × 5
      + (isDesktop ? 100 : 0)
      + (hasEthernet ? 50 : 0)
      + (screenWidth / 10)
      + (supportedCodecs.size × 15)
```

### Tier 6: Platform Abstractions

| Platform | UDP Socket | WebRTC | Notes |
|----------|-----------|--------|-------|
| Android | `java.net.DatagramSocket` | Pure Kotlin OR Google `libwebrtc` | Pure Kotlin preferred for KMP |
| Desktop | `java.net.DatagramSocket` | Pure Kotlin | No browser deps |
| iOS | POSIX `sendto`/`recvfrom` | Pure Kotlin OR Apple WebRTC framework | Kotlin/Native cinterop |
| Web/JS | Browser `RTCPeerConnection` | Browser-native (wraps JS API) | No custom STUN/TURN needed — browser handles it |

---

## Estimated Scope

| Tier | Files | Lines | Effort |
|------|-------|-------|--------|
| STUN | 5 | ~410 | 4 hrs |
| TURN | 5 | ~510 | 6 hrs |
| ICE | 5 | ~460 | 6 hrs |
| Signaling | 4 | ~320 | 3 hrs |
| Peer Manager | 3 | ~210 | 2 hrs |
| Capability + Election | 7 | ~460 | 5 hrs |
| Platform (4 targets) | 10 | ~500 | 5 hrs |
| Tests | 8 | ~600 | 4 hrs |
| **Total** | **~47** | **~3,470** | **~35 hrs** |

---

## Use Cases

### AvaConnect (Screen Sharing)
```kotlin
// Sender (phone)
val connection = PeerConnection(config = PeerConnectionConfig(
    stunServers = listOf("stun:stun.avanues.com:3478"),
    turnServers = listOf(TurnServer("turn:turn.avanues.com:3478", "user", "pass")),
))
val channel = connection.createDataChannel("cast")
connection.createOffer() // → sends via signaling
channel.send(jpegFrame)

// Receiver (browser/glasses)
val connection = PeerConnection(config)
connection.onDataChannel { channel ->
    channel.messages.collect { frame -> renderFrame(frame) }
}
connection.setRemoteOffer(sdp) // → received via signaling
```

### RemoteCast NAT Traversal
```kotlin
// Before: direct WebSocket on LAN only (port 54321)
val server = CastWebSocketServer(port = 54321)  // fails through NAT

// After: ICE-negotiated connection works through NAT
val ice = IceAgent(config)
ice.gatherCandidates() // host + STUN reflexive + TURN relay
ice.startConnectivityChecks(remoteCandidates)
val bestPath = ice.nominatedPair() // direct or relayed
// Stream cast frames through the negotiated path
```

### Third-Party P2P App
```kotlin
// Simple peer-to-peer chat
val peer = PeerConnection(PeerConnectionConfig(
    stunServers = listOf("stun:stun.l.google.com:19302"),
))
val chat = peer.createDataChannel("chat")
chat.send("Hello from peer A!")
chat.messages.collect { println("Received: $it") }
```

---

## Web Target Considerations

On the **Web/JS target**, NetAvanue wraps the browser's built-in WebRTC:

```kotlin
// commonMain
expect class PeerConnection(config: PeerConnectionConfig) {
    fun createOffer(): SdpMessage
    fun createDataChannel(label: String): DataChannel
}

// jsMain
actual class PeerConnection actual constructor(config: PeerConnectionConfig) {
    private val rtc = RTCPeerConnection(config.toJsConfig()) // Browser API
    actual fun createOffer() = rtc.createOffer().toSdpMessage()
    actual fun createDataChannel(label: String) = DataChannel(rtc.createDataChannel(label))
}

// androidMain / desktopMain
actual class PeerConnection actual constructor(config: PeerConnectionConfig) {
    private val iceAgent = IceAgent(config) // Our pure Kotlin implementation
    // ... full STUN/TURN/ICE implementation
}
```

This means:
- **Browser**: Uses native WebRTC (no custom STUN/TURN implementation needed)
- **Native**: Full custom implementation in pure Kotlin
- **Both**: Same API surface via expect/actual

---

## Dependencies

```
NetAvanue
├── HTTPAvanue (api) — AvanueIO for byte I/O, WebSocket for signaling, mDNS for LAN discovery
├── Foundation (api) — Platform abstractions
├── Logging (api) — Logging
├── kotlinx-coroutines-core
├── kotlinx-serialization-json — SDP/signaling message parsing
└── kotlinx-datetime — Allocation timeouts, keepalive
```

No new external dependencies — builds entirely on HTTPAvanue + kotlinx.

---

## HTTPAvanue Updates Needed (SimpleWebServer)

Before NetAvanue, HTTPAvanue needs the SimpleWebServer feature for RemoteCast's browser receiver:

| Feature | Description | Lines |
|---------|-------------|-------|
| `StaticDirectoryServer.kt` | Filesystem directory serving (not just resources) | ~60 |
| Directory listing | HTML index page with file links, sizes | ~40 |
| Index file resolution | Auto-serve index.html from directory | ~10 |
| `HttpServer.serveDirectory()` extension | One-line directory server setup | ~10 |

This should be added to HTTPAvanue before starting NetAvanue.

---

## Implementation Order (Suggested)

| Session | Work | Prerequisite |
|---------|------|-------------|
| 1 | HTTPAvanue: Add SimpleWebServer + keep-alive loop | None |
| 2 | NetAvanue: STUN client + server (Tier 1) | HTTPAvanue AvanueIO |
| 3 | NetAvanue: TURN client + server (Tier 2) | STUN |
| 4 | NetAvanue: ICE agent (Tier 3) | STUN + TURN |
| 5 | NetAvanue: Signaling + PeerConnection (Tier 4-5) | ICE + HTTPAvanue WebSocket |
| 6 | RemoteCast: Wire up NetAvanue for NAT traversal | NetAvanue complete |
| 7 | Web/JS target actuals | Core working on native |

---

## Related Documents

- HTTPAvanue v2.0: `docs/plans/HTTPAvanue/HTTPAvanue-Plan-V2Implementation-260222-V1.md`
- HTTPAvanue Chapter 104: `Docs/MasterDocs/HTTPAvanue/Developer-Manual-Chapter104-HTTPAvanueV2ZeroDepEnhancements.md`
- RemoteCast Architecture: `docs/plans/RemoteCast/RemoteCast-Spec-GlassClientArchitecture-260219-V1.md`
- AVACode Recipes: `docs/plans/AVACode/AVACode-Plan-RecipeSystemPending-260222-V1.md`
- Legacy VoiceOS Screenshare: `/Users/manoj_mbpm14/Coding/voiceos/app/src/main/java/com/augmentalis/dev/screenshare/`
