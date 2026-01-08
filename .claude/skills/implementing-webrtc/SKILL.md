---
name: implementing-webrtc
description: Implements real-time communication with WebRTC. Use for peer-to-peer audio/video, data channels, STUN/TURN servers, signaling, and media stream handling.
---

# WebRTC Implementation

## Components

| Component | Purpose |
|-----------|---------|
| RTCPeerConnection | P2P connection |
| MediaStream | Audio/video tracks |
| RTCDataChannel | Arbitrary data |
| Signaling | Exchange SDP/ICE |

## Connection Flow

```
1. Create RTCPeerConnection
2. Add local media tracks
3. Create offer (caller) / answer (callee)
4. Exchange SDP via signaling server
5. Exchange ICE candidates
6. Connection established
```

## Code Patterns

| Operation | Code |
|-----------|------|
| Create PC | `new RTCPeerConnection({ iceServers })` |
| Add track | `pc.addTrack(track, stream)` |
| Create offer | `await pc.createOffer()` |
| Set local | `await pc.setLocalDescription(offer)` |
| Set remote | `await pc.setRemoteDescription(answer)` |
| ICE candidate | `pc.onicecandidate = (e) => send(e.candidate)` |

## ICE Servers

```javascript
const config = {
  iceServers: [
    { urls: 'stun:stun.l.google.com:19302' },
    { urls: 'turn:turn.example.com', username: 'user', credential: 'pass' }
  ]
};
```

## Data Channels

| Pattern | Usage |
|---------|-------|
| Create | `pc.createDataChannel('chat')` |
| Receive | `pc.ondatachannel = (e) => {}` |
| Send | `channel.send(data)` |
| Binary | `channel.binaryType = 'arraybuffer'` |

## Troubleshooting

| Issue | Check |
|-------|-------|
| No connection | ICE servers, firewall |
| No media | Permissions, track enabled |
| Quality | Bandwidth, codec selection |

## Quality Gates

| Gate | Target |
|------|--------|
| Connection time | <3s |
| Fallback | TURN when P2P fails |
| Graceful degradation | Handle track loss |
