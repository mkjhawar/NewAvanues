# NetAvanue Phases 6-10 — ICE/STUN, PeerConnection, RemoteCast Integration

**Module**: NetAvanue (`Modules/NetAvanue/`)
**Type**: Plan (Detailed Implementation)
**Date**: 2026-02-23
**Version**: V1
**Status**: In Progress
**Depends On**: Phases 4-5 (complete), HTTPAvanue WebSocket, AvanueCentral signaling

---

## 1. Phase 6: STUN Client + ICE Agent

### 6A: STUN Message Codec (RFC 5389)

STUN messages have a fixed 20-byte header followed by TLV attributes:

```
 0                   1                   2                   3
 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
|0 0|     STUN Message Type     |         Message Length        |
+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
|                         Magic Cookie (0x2112A442)             |
+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
|                                                               |
|                     Transaction ID (96 bits)                  |
|                                                               |
+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
```

Files:
- `ice/stun/StunMessage.kt` — Header + encode/decode
- `ice/stun/StunAttribute.kt` — MAPPED-ADDRESS, XOR-MAPPED-ADDRESS, ERROR-CODE, etc.
- `ice/stun/StunMessageType.kt` — Method (Binding) + Class (Request/Response/Error)

### 6B: UDP Socket Platform Abstraction

```kotlin
// commonMain
expect class UdpSocket() {
    suspend fun bind(port: Int = 0)
    suspend fun send(data: ByteArray, host: String, port: Int)
    suspend fun receive(buffer: ByteArray): UdpPacket
    fun close()
    val localPort: Int
}
```

Actuals:
- Android/Desktop: `java.net.DatagramSocket`
- iOS: POSIX `sendto`/`recvfrom` via cinterop

### 6C: STUN Client

```kotlin
class StunClient(private val socket: UdpSocket) {
    suspend fun bindingRequest(stunServer: String, port: Int): StunResponse
    // Returns server-reflexive address (public IP:port)
}
```

Retransmission: RTO starts at 500ms, doubles each retry, max 7 retries (39.5s total per RFC 5389).

### 6D: ICE Agent (RFC 8445)

```
Gathering: collect candidates
  1. Host candidates (local interfaces)
  2. Server-reflexive (STUN binding response)
  3. Relay (TURN allocation — future)

Checking: verify connectivity
  For each candidate pair (local, remote):
    Send STUN Binding Request from local to remote
    If response → pair succeeds

Nomination: select best working pair
  Regular nomination: controlling agent nominates after all checks
```

Files:
- `ice/IceCandidate.kt` — Host, ServerReflexive, Relay types
- `ice/IceCandidatePair.kt` — Pair + priority + state machine
- `ice/IceGatherer.kt` — Gather candidates from all sources
- `ice/IceAgent.kt` — Full ICE: gather → check → nominate

---

## 2. Phase 7: PeerConnection + DataChannel

### PeerConnection

High-level API orchestrating ICE + signaling:

```kotlin
class PeerConnection(
    config: PeerConnectionConfig,
    signalingClient: SignalingClient,
) {
    suspend fun createOffer(): String   // SDP offer
    suspend fun setRemoteOffer(sdp: String)
    suspend fun createAnswer(): String  // SDP answer
    suspend fun setRemoteAnswer(sdp: String)

    fun createDataChannel(label: String): DataChannel
    val onDataChannel: Flow<DataChannel>

    suspend fun close()
}
```

### DataChannel

Bidirectional data over the ICE-negotiated path:

```kotlin
class DataChannel(label: String) {
    val messages: Flow<ByteArray>
    suspend fun send(data: ByteArray)
    suspend fun sendText(text: String)
    val state: StateFlow<DataChannelState>
}
```

Transport: Raw UDP datagrams over the nominated ICE pair.
Reliability: Unreliable by default (for real-time media). Optional reliability via sequence numbers + retransmit.

### Minimal SDP

For our use case (data-only, no media negotiation with codecs), SDP is simplified:
```
v=0
o=- {sessionId} {version} IN IP4 0.0.0.0
s=-
t=0 0
a=ice-ufrag:{ufrag}
a=ice-pwd:{pwd}
a=fingerprint:sha-256 {fingerprint}
m=application {port} UDP/DTLS/SCTP webrtc-datachannel
c=IN IP4 0.0.0.0
a=candidate:{foundation} 1 udp {priority} {ip} {port} typ {type}
```

---

## 3. Phase 8: RemoteCast Integration

Wire NetAvanue as an alternative transport in RemoteCast:

```kotlin
// RemoteCast currently:
class CastWebSocketServer(port: Int) // Direct LAN WebSocket

// After integration:
class CastTransport {
    fun startLan(port: Int)  // existing WebSocket path
    fun startP2P(session: Session)  // new NetAvanue path
}
```

Key changes:
1. `RemoteCast` depends on `NetAvanue`
2. `CastTransport` wraps both LAN (WebSocket) and P2P (DataChannel)
3. Auto-detection: try LAN first, fall back to P2P through NAT
4. Browser receiver via HTTPAvanue SimpleWebServer (existing)

---

## 4. Phase 9: Testing

| Test | Type | Scope |
|------|------|-------|
| STUN codec roundtrip | Unit | Encode → decode all message types |
| STUN client binding | Integration | Real STUN server (stun.l.google.com:19302) |
| ICE gathering | Unit | Mock STUN, verify candidate collection |
| ICE connectivity | Integration | Two local agents, loopback connectivity check |
| PeerConnection offer/answer | Unit | SDP generation/parsing |
| DataChannel send/receive | Integration | Two peers, loopback |
| Capability scorer | Unit | Already done (4 tests) |
| Socket.IO client | Unit | Mock WebSocket, verify protocol |

---

## 5. Phase 10: Documentation

- Update Chapter 109 with Phase 6-10 details
- Add STUN/ICE protocol reference
- Add PeerConnection usage examples
- Add RemoteCast integration guide

---

## Time Estimates

| Phase | Sequential | Swarm | Notes |
|-------|-----------|-------|-------|
| 6A STUN codec | 2h | 2h | Foundation, must be first |
| 6B UdpSocket | 2h | 1h | 3 platforms in parallel |
| 6C STUN client | 1h | 1h | Depends on 6A+6B |
| 6D ICE agent | 3h | 2h | Complex state machine |
| 7 PeerConnection | 3h | 2h | DataChannel can parallel |
| 8 RemoteCast | 2h | 2h | Integration |
| 9 Testing | 2h | 1h | Multiple test files in parallel |
| 10 Docs | 1h | 1h | |
| **Total** | **16h** | **12h** | **25% savings with swarm** |
