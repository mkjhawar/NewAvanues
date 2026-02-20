# RemoteAvanue: Full System Implementation Plan
**Date:** 2026-02-19 | **Version:** V1 | **Mode:** .yolo .tot .cot

## Overview
- **Platforms:** Android (phone sender + glasses receiver), Desktop (Cockpit receiver), Browser (WebRTC receiver)
- **Apps:** GlassAvanue (phone server), GlassClient (thin glasses APK)
- **Modules:** RemoteCast (transport), VoiceOSCore (commands), HUDManager (glasses UI), DeviceManager (detection)
- **Estimated tasks:** 24 | **Swarm recommended:** Yes (3+ platforms)

---

## Battery Profile (Corrected Analysis)

VoiceOSCore is event-driven with excellent power efficiency:
- Screen scraping: event-driven only (no polling), fingerprint dedup → ~0% at idle
- Command generation: once per screen change, cached → negligible
- Overlay rendering: Compose StateFlow, recomposes only on change → negligible
- Speech recognition (Vivoka continuous): **~5-15%/hr** — dominant cost
- IMU head tracking (60Hz): **~5-8%/hr** — only when cursor enabled
- **Missing optimization:** wake-word gating not wired (could cut ASR idle to ~2-3%/hr)

**GlassAvanue server mode (phone):** ~7-12%/hr total
**GlassClient thin receiver (glasses):** ~2-5%/hr (no scraping, no MediaProjection)

## App Size Analysis

| Component | Debug APK | Release Est. |
|-----------|----------|-------------|
| Full Avanues app | 286 MB | ~120-150 MB |
| Vivoka SDK + models | ~150-200 MB | ~80-100 MB (single ABI) |
| GlassClient (no Vivoka) | N/A | ~15-25 MB |

---

## Phase 1: Prerequisites (Foundation)

### 1.1 Extract HUDManager to Modules:HUDManager
Move 16 files from VoiceOSCore/managers/hudmanager/ to new module.
Zero callers in VoiceOSCore — clean extraction.
Dependencies: DeviceManager, Localization, AvanueUI.
**Est:** 18 files touched

### 1.2 Extract LocalizationManager to Modules:LocalizationManager
Move 12 files from VoiceOSCore/managers/localizationmanager/ to new module.
Zero callers — clean extraction.
Dependencies: Database, VoiceDataManager.
**Est:** 15 files touched

### 1.3 Wire Wake-Word Gating (Battery Optimization)
Connect existing `enableWakeWord()` interface to Vivoka's wake-word API.
Low-power hotword detection between commands → full grammar on wake.
Reduces ASR idle drain from ~10%/hr to ~2-3%/hr.
**Files:** VivokaAndroidEngine.kt, SpeechEngineManager.kt
**Est:** ~30 lines changed

---

## Phase 2: RemoteCast Foreground Service (Blocking Prerequisite)

### 2.1 AndroidManifest Declarations
Add to Apps/Android/avanues manifest:
- `FOREGROUND_SERVICE_MEDIA_PROJECTION` permission
- `RemoteCastForegroundService` with `foregroundServiceType="mediaProjection"`
**Est:** 1 file, 10 lines

### 2.2 RemoteCastForegroundService
New service class:
- `startForeground()` with mediaProjection type + notification
- Holds AndroidCastManager instance
- Receives MediaProjection token from Activity intent
- Calls `castManager.setMediaProjection()` + `startCasting()`
- Lifecycle management (stop on disconnect, rebind on reconnect)
**Files:** New: RemoteCastForegroundService.kt
**Est:** ~120 lines

### 2.3 MediaProjection Consent Flow
Activity/Composable screen:
- `MediaProjectionManager.createScreenCaptureIntent()` → system consent dialog
- `ActivityResultLauncher` handles result
- On RESULT_OK: start foreground service with intent data
- On RESULT_CANCELED: show error, return to pairing screen
**Files:** New: CastConsentScreen.kt (Compose)
**Est:** ~80 lines

### 2.4 Wire CastCommandHandler
Inject `ICastManager` reference into CastCommandHandler.
Replace failure stubs with actual calls: startCasting(), stopCasting(), etc.
**Files:** CastCommandHandler.kt, VoiceOSCoreAndroidFactory.kt
**Est:** ~40 lines changed

---

## Phase 3: Protocol Extension (AVU Wire Format)

### 3.1 Multi-Message Protocol Support
Extend CastFrameData to support multiple magic types:
- `CAST` (existing) — video frames
- `CMD\0` — voice commands (AVU text)
- `VOC\0` — vocabulary sync (AVU command list)
- `IMU\0` — head tracking data (future)
- `TTS\0` — audio feedback (future)
Add message router in CastWebSocketServer/Client (updated 260220: was MjpegTcp*).
**Files:** CastFrameData.kt (commonMain), CastWebSocketServer.kt, CastWebSocketClient.kt
**Est:** ~60 lines changed

### 3.2 AVU Command Serialization
Serialize QuantizedCommand to/from VOS compact format for CMD packets:
`phrase|actionType|targetAvid|confidence|locale|target:local|remote|auto`
Use existing VosParser for deserialization.
**Files:** New: AvuWireSerializer.kt (commonMain)
**Est:** ~50 lines

### 3.3 VOCAB Sync Manager
On screen change (CommandOrchestrator output), serialize full command list
to AVU format and send as VOC packet to all connected receivers.
**Files:** New: VocabSyncManager.kt (commonMain)
**Est:** ~80 lines

### 3.4 Bidirectional WebSocket
Enable reverse channel on same WebSocket connection (updated 260220: was TCP).
Server reads CMD packets from client. Client reads CAST+VOC packets from server.
Mux by magic byte header (first 4 bytes determine handler).
WebSocket binary messages naturally support bidirectional communication.
**Files:** CastWebSocketServer.kt, CastWebSocketClient.kt
**Est:** ~80 lines changed

---

## Phase 4: GlassAvanue App (Phone Server)

### 4.1 App Scaffold
Create Apps/Android/GlassAvanue/ with:
- build.gradle.kts (deps: RemoteCast, VoiceOSCore, HUDManager, DeviceManager, AvanueUI)
- AndroidManifest.xml (foreground service, media projection, WiFi)
- GlassAvanueActivity.kt (single activity, Compose navigation)
**Est:** 4 new files

### 4.2 GlassDetector
Use DeviceManager to identify connected glasses type.
Auto-detect via BLE scan (Vuzix Z100 service UUID) or TCP handshake (GlassClient sends device info).
Add `FormFactor.GLASSES` + `DisplayColorMode` to DeviceCapabilities.
**Files:** New: GlassDetector.kt, modify DeviceCapabilities.kt
**Est:** ~100 lines

### 4.3 Pairing Screen
QR code display (encode IP:port + mode + device name).
mDNS service registration for auto-discovery.
Manual IP entry fallback.
Recent connections list (persisted via Foundation Settings).
**Files:** New: PairingScreen.kt
**Est:** ~150 lines

### 4.4 Casting Screen
Live stats: FPS, latency, bandwidth, resolution, connected device name.
Command log: last 10 received CMD packets with execution status.
Stop button, quality selector, mode indicator.
**Files:** New: CastingScreen.kt
**Est:** ~120 lines

### 4.5 HUD Mode Controller (Z100/BLE)
Ultralite SDK integration.
Push AVID labels, command status, notifications via BLE.
Format screen commands into Z100's 640x480 text layout.
**Files:** New: HudModeController.kt
**Dependency:** Vuzix Ultralite SDK (JitPack)
**Est:** ~150 lines

### 4.6 Cast Mode Controller
Wraps AndroidCastManager + VocabSyncManager.
Manages foreground service lifecycle.
Handles connection/disconnection events.
**Files:** New: CastModeController.kt
**Est:** ~100 lines

---

## Phase 5: GlassClient App (Thin Glasses Receiver)

### 5.1 App Scaffold
Create Apps/Android/GlassClient/ with:
- build.gradle.kts (deps: RemoteCast, AvanueUI, Foundation — NO Vivoka, NO VoiceOSCore full)
- AndroidManifest.xml (minimal permissions: WiFi, microphone, internet)
- GlassClientActivity.kt (fullscreen, voice-only nav)
**Est:** 4 new files, target APK ~15-25 MB

### 5.2 Connection Screen
QR scanner or manual IP entry.
Connect to GlassAvanue server via TCP.
Show connection status, retry logic.
**Files:** New: ConnectionScreen.kt
**Est:** ~80 lines

### 5.3 Cast View Screen
Full-screen MJPEG/WebRTC receiver display.
ColorMatrix filter for monochrome modes (GPU, zero CPU cost).
HUD overlay (voice status indicator, last command, connection quality).
**Files:** New: CastViewScreen.kt, uses CastReceiverView.kt (existing)
**Est:** ~100 lines

### 5.4 Voice Capture + VOCAB Matcher
On-device speech recognition (Android SpeechRecognizer or lighter engine).
Match recognized text against received VOCAB list (local, no round-trip).
Serialize matched command to AVU CMD packet and send to server.
**Files:** New: GlassVoiceCapture.kt, VocabMatcher.kt
**Est:** ~120 lines

### 5.5 Monochrome Palette
AvanueTheme MonoGreenColors, MonoWhiteColors, MonoAmberColors.
GPU ColorMatrix for cast frame filtering.
Auto-select based on DeviceDetector (RealWear → color, Z100 → green).
**Files:** New: MonochromeColors.kt (AvanueUI), modify AvanueTheme.kt
**Est:** ~60 lines

---

## Phase 6: WebRTC Browser Receiver (Phone-to-Phone)

### 6.1 Port WebRTC Manager
Port legacy `WebRtcManager.java` from /voiceos/ to KMP androidMain.
Modernize: Java → Kotlin, callbacks → coroutines, integrate with CastManager.
**Files:** New: WebRtcCastManager.kt (androidMain)
**Dependency:** `org.webrtc:google-webrtc`
**Est:** ~200 lines

### 6.2 HTTPS + WebSocket Server
Replace vendored NanoHTTPD with Ktor server (already in stack).
Serve browser receiver HTML from assets.
Handle WebSocket signaling (SDP, ICE, touch/key commands).
**Files:** New: WebRtcSignalingServer.kt
**Est:** ~150 lines

### 6.3 Browser Receiver HTML/JS
Port + modernize legacy `index.html` + `main.js` + `mouse.js`.
Remove jQuery CDN dependency (bundle vanilla JS).
Add: command palette sidebar (populated from VOCAB sync via WebSocket).
Add: QR code display for easy URL sharing.
**Files:** New: assets/remoteavanue/ (3 HTML/JS files)
**Est:** ~300 lines

### 6.4 Touch-to-Gesture Relay
Browser sends mouse/touch events via WebSocket.
Server translates to AccessibilityService gesture dispatches.
Coordinate mapping: browser video space → phone screen space.
**Files:** Reuse: AccessibilityService.dispatchGesture()
**Est:** ~80 lines

---

## Phase 7: Integration + Polish

### 7.1 Auto-Mode Selection
CastManager detects receiver type and selects mode:
- TCP handshake includes device info → MJPEG mode (glasses)
- HTTPS request from browser → WebRTC mode
- BLE scan detects Z100 → HUD mode
**Files:** AndroidCastManager.kt
**Est:** ~40 lines

### 7.2 Cockpit Integration
Wire RemoteAvanue into Cockpit ContentRenderer.
FrameContent.ScreenCast renders CastReceiverView.
Cockpit as both sender (desktop cast to glasses) and receiver.
**Files:** ContentRenderer.kt, FrameContent.kt
**Est:** ~30 lines

### 7.3 VOS Voice Commands
Add remote_cast VOS commands (5 per locale × 5 locales = 25 entries):
cast_start, cast_stop, cast_connect, cast_disconnect, cast_quality
**Files:** Existing VOS files, CastCommandHandler.kt
**Est:** ~25 VOS entries + handler wiring (already done)

### 7.4 Developer Manual Chapter
Chapter 101: RemoteAvanue Casting & Glass Integration
Cover: architecture, protocol, GlassAvanue server, GlassClient, VOCAB sync.
**Files:** New: Docs/MasterDocs/RemoteCast/Developer-Manual-Chapter101-RemoteAvanueCasting.md
**Est:** ~500 lines

---

## Execution Order (Proximity + Dependency Optimized)

| Step | Phase | What | Depends On | Parallel? |
|------|-------|------|-----------|-----------|
| 1 | 1.1 | Extract HUDManager | Nothing | Yes (with 1.2) |
| 2 | 1.2 | Extract LocalizationManager | Nothing | Yes (with 1.1) |
| 3 | 1.3 | Wire wake-word gating | Nothing | Yes (with 1.1+1.2) |
| 4 | 2.1-2.3 | Foreground service + consent | Nothing | After 1.x |
| 5 | 2.4 | Wire CastCommandHandler | 2.1-2.3 | Sequential |
| 6 | 3.1-3.4 | Protocol extension + VOCAB | 2.x | Sequential |
| 7 | 4.1-4.3 | GlassAvanue scaffold + pairing | 2.x + 3.x | Sequential |
| 8 | 4.4-4.6 | Casting screen + controllers | 4.1-4.3 | Sequential |
| 9 | 5.1-5.5 | GlassClient app | 3.x (protocol) | Parallel with 4.x |
| 10 | 6.1-6.4 | WebRTC browser receiver | 2.x | Parallel with 4.x + 5.x |
| 11 | 7.1-7.4 | Integration + polish | All above | Sequential |

**Swarm candidates:** Steps 1-3 (all parallel), Steps 7-10 (4 independent tracks)

---

## Time Estimate

| Track | Tasks | Sequential | With Swarm |
|-------|-------|-----------|-----------|
| Prerequisites (Phase 1) | 3 | 3 hrs | 1.5 hrs |
| Foreground Service (Phase 2) | 4 | 4 hrs | 3 hrs |
| Protocol Extension (Phase 3) | 4 | 4 hrs | 3 hrs |
| GlassAvanue App (Phase 4) | 6 | 8 hrs | 5 hrs |
| GlassClient App (Phase 5) | 5 | 6 hrs | 4 hrs |
| WebRTC Browser (Phase 6) | 4 | 6 hrs | 4 hrs |
| Integration (Phase 7) | 4 | 4 hrs | 3 hrs |
| **Total** | **30** | **35 hrs** | **23.5 hrs** |
| **Savings with swarm** | | | **11.5 hrs (33%)** |

---

## Commit Strategy

| Phase | Commit Pattern |
|-------|---------------|
| Phase 1 | `refactor(VoiceOSCore): Extract HUDManager/LocalizationManager to modules` |
| Phase 2 | `feat(RemoteCast): Foreground service + MediaProjection consent flow` |
| Phase 3 | `feat(RemoteCast): Multi-message protocol (CMD/VOC) + AVU wire format` |
| Phase 4 | `feat(GlassAvanue): Server app scaffold + pairing + casting screens` |
| Phase 5 | `feat(GlassClient): Thin glasses receiver + VOCAB matcher + mono palette` |
| Phase 6 | `feat(RemoteCast): WebRTC browser receiver + touch relay` |
| Phase 7 | `feat(RemoteCast): Auto-mode selection + Cockpit integration + Chapter 101` |
