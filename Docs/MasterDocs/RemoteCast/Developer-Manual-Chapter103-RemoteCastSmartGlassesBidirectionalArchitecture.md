# Developer Manual — Chapter 103: RemoteCast — Smart Glasses Bidirectional Architecture

**Module:** `Modules/RemoteCast/`
**Apps:** `Apps/Android/GlassAvanue/` (server), `Apps/Android/GlassClient/` (receiver)
**Platforms:** Android (primary), iOS (planned), Desktop (sender only)
**Dependencies:** HTTPAvanue (WebSocket), VoiceOSCore (voice pipeline), DeviceManager (detection), AvanueUI (theme)
**Created:** 2026-02-21
**Author:** Manoj Jhawar

---

## 1. Overview

RemoteCast transforms from one-way screen mirror into a bidirectional VoiceOS thin-client architecture. The system enables any Android phone, tablet, or desktop computer to transmit voice commands, screen context, and UI vocabulary to a pair of smart glasses, which in turn relays voice input and IMU (head tracking) data back to the sender for coordinated interaction.

**Core Design Principle:** Glasses are thin clients—no heavy ML/scraping computation, no large VOS profiles. All intelligence lives on the phone/sender. Glasses receive: video stream (MJPEG/JPEG), executable command vocabulary (AVU format), and UI context. Glasses send back: voice transcription, head orientation (quaternion), and tap/gesture events.

**Three Key Components:**

1. **GlassAvanue** = phone/tablet/desktop SERVER app
   - Detects which glasses are connected (Bluetooth discovery + DeviceManager)
   - Captures screen + streams JPEG via WebSocket
   - Syncs command vocabulary (.VOS profiles) to receiver
   - Listens for incoming voice commands and IMU events
   - Auto-selects transmission mode (HUD vs Cast)

2. **GlassClient** = thin APK on full Android glasses
   - Receives JPEG stream, displays on eyepiece
   - Loads two .VOS profiles: local (glasses menu) + synced (phone commands)
   - Runs local speech recognizer (with offline models)
   - Routes commands: local (execute locally), remote (relay to phone), auto (context-decides)
   - Sends voice transcription + IMU quaternion back to phone

3. **RemoteCast Module** = infrastructure (Modules/RemoteCast/)
   - Transport: WebSocket (HTTP/2 h2c) via HTTPAvanue
   - Protocols: CAST (video), CMD (commands), IMU (head tracking), TTS (audio feedback)
   - Platform-specific implementations: AndroidCastManager, IosCastManager (future), DesktopCastManager
   - 20-byte wire protocol header (magic + sequence + timestamp + payload size)

**Two Auto-Selected Transmission Modes:**

| Mode | Glasses Type | Connection | Best For | Footprint |
|------|-------------|-----------|----------|-----------|
| **HUD Mode** | Z100, BLE peripherals | BLE only | Text labels, command numbers, battery-critical | ~0.1%/hr battery |
| **Cast Mode** | Moverio, RealWear, Blade, Rokid | WiFi/TCP WebSocket | Full screen, video context, rich UI | ~2-5%/hr battery |

---

## 2. Target Devices & Platform Tiers

| Tier | Connection | App Deployment | Devices | Display | Notes |
|------|-----------|---------------|---------|---------|-------|
| **Tier 1: Direct APK** | WiFi/TCP | Sideload APK | Vuzix Blade 2, Rokid Air/Max, Epson Moverio BT-45, RealWear Navigator | Full-color binocular | Standard Android; WiFi + TCP socket |
| **Tier 2: BLE Companion** | BLE only | Phone companion app + glasses peripheral | Vuzix Z100 | 640x480 mono green, right eye | Ultralite SDK; RemoteCast via BLE (future) |
| **Tier 3: Platform SDK** | Platform-specific | MiniApp (TypeScript) / XR App | Mentra Live, XReal Aura, Even Realities G1 | Color, platform-managed | SDK-specific code path; wasm or XR runtime |
| **Tier 4: Tethered** | USB-C | App runs on compute puck | XReal Aura (tethered mode) | Display on puck; glasses are display | No RemoteCast needed; same codebase as phone |

### Tier 1: Direct APK (Vuzix, RealWear, Rokid, Epson)

Standard Android devices with WiFi. GlassClient sideloaded via ADB. Full RemoteCast support: video stream (JPEG over WebSocket), commands (AVU text), IMU (accelerometer + gyro + magnetometer). 30-40 MB RAM, ~5 MB APK, ~20ms per frame (WiFi latency + decode).

### Tier 2: BLE Companion (Vuzix Z100)

Right-eye monocular display, 640x480, green monochrome (ITU-R BT.601 luminance → green channel). Ultralite SDK exposes: text + image + SVG drawing APIs. BLE bandwidth ~1 Mbps limits video to 2-5 FPS or text-only mode. For RemoteCast Phase B+, send text labels + AVID numbers via BLE instead of JPEG. (Currently unimplemented; HudModeController reserved for this.)

### Tier 3: Platform SDKs (Mentra, XReal, Even)

Mentra OS: TypeScript MiniApp runtime. GlassClient becomes a MiniApp (.zip bundle) that calls Mentra APIs for WebSocket, file I/O, voice. XReal Aura: Android XR Preview SDK (developer preview 3). Even Realities G1: proprietary OS with web view + native bridge. Each requires SDK-specific bindings; commonMain wire protocol unchanged. (Phase I: vendor SDK research and integration.)

### Tier 4: Tethered XReal Aura

Glasses are a display output device; compute puck runs Android. GlassClient same codebase as phone (Aura is Android XR, which extends Android APIs). No RemoteCast transport needed—IPC only. (Out of scope for Phase A-H.)

---

## 3. Current Architecture (Transport Layer)

### Wire Format: 20-Byte Header + Payload

All RemoteCast messages use a unified 20-byte header, with different magic bytes to denote message type:

```
Offset  Size  Field             Value / Range
 0-3     4    Magic             "CAST", "CMD\0", "IMU\0", "TTS\0" (null-padded)
 4-7     4    Sequence No.      Monotonic Int32 (per connection)
 8-15    8    Timestamp         System.currentTimeMillis() (Int64)
16-19    4    Payload Size      Byte count of payload (Int32)
```

**Total frame = 20 bytes header + N bytes payload.**

### Message Types

#### CAST (Video Frame)
```
[20-byte header: magic="CAST"] + [JPEG payload]
```
Sender → Receiver. JPEG payload is a complete image file (entropy-coded, can start decode mid-frame for progressive display). Sequence number increments per frame. Timestamp is capture moment (for frame rate measurement).

#### CMD (Command)
```
[20-byte header: magic="CMD\0"] + [AVU text payload]
```
Either direction (Glasses → Phone for remote commands, Phone → Glasses for broadcasts, future). AVU format (VOS compact v3.0 pipe-delimited) with optional target routing extension.

#### IMU (Head Tracking)
```
[20-byte header: magic="IMU\0"] + [binary IMU data]
```
Receiver → Sender. 24-byte quaternion payload: 4x float32 (qx, qy, qz, qw, each 4 bytes) + 8-byte timestamp. Represents 3D head rotation relative to device's "neutral" pose. Optional; disabled by default (battery impact on glasses).

#### TTS (Audio Feedback)
```
[20-byte header: magic="TTS\0"] + [audio payload]
```
Sender → Receiver. Future enhancement: text-to-speech feedback on glasses. Audio format TBD (likely MP3 or WAV with metadata header).

### Video Quality Profiles

| Profile | Resolution | Quality | FPS | Bandwidth | Latency |
|---------|-----------|---------|-----|-----------|---------|
| **LOW** | 640x360 | 50% JPEG | ~10 fps | ~0.5 Mbps | ~100ms |
| **MEDIUM** | 1280x720 | 70% JPEG | ~15 fps | ~1.2 Mbps | ~70ms |
| **HIGH** | 1920x1080 | 85% JPEG | ~20 fps | ~2.5 Mbps | ~50ms |

Sender auto-selects based on network condition (WiFi signal strength from WiFiManager) or manual override.

### Sender Path: Phone → Network

```
MediaProjection (Android 5.0+)
  ↓
VirtualDisplay (IImageReader callback)
  ↓
Bitmap.compress(JPEG, quality: 50-85)
  ↓
CastFrameData.encode(magic="CAST", sequenceNo, timestamp, jpegBytes)
  ↓
CastWebSocketServer (HTTPAvanue WebSocket)
  ↓
Binary message → IP:54321/cast/stream
```

On phone, `ScreenCaptureHelper` manages MediaProjection lifecycle. `AndroidCastManager` binds CastWebSocketServer, registers IImageReader callback, and encodes each frame. Max 20 FPS soft cap (configurable).

### Receiver Path: Network → Display

```
CastWebSocketClient (HTTPAvanue)
  ↓
Binary message received
  ↓
CastFrameData.decode() → validate magic="CAST", extract jpegBytes
  ↓
BitmapFactory.decodeByteArray(jpegBytes) → Bitmap
  ↓
Compose Image(bitmap, modifier=fillMaxSize())
  ↓
GPU: optional MonoGreen ColorMatrix (if MONO_GREEN display mode)
```

On glasses, `CastReceiverView` composable displays the stream. Flow<ByteArray> for WebSocket messages, collected in a LaunchedEffect. Each frame updates an MutableState<Bitmap>, triggering Compose recomposition.

### Existing File Inventory (12 files — all implemented, Phase A done)

| File | Source Set | Type | Status |
|------|-----------|------|--------|
| `ICastManager.kt` | commonMain | Interface | Complete |
| `CastState.kt` | commonMain | Sealed class | Complete |
| `CastFrameData.kt` | commonMain | Data model | Complete |
| `CastWebSocketServer.kt` | commonMain | Transport | Complete (HTTPAvanue WebSocket) |
| `CastWebSocketClient.kt` | commonMain | Transport | Complete (HTTPAvanue WebSocket) |
| `ScreenCaptureHelper.kt` | androidMain | Platform | Complete (MediaProjection → JPEG Flow) |
| `AndroidCastManager.kt` | androidMain | Implementation | Complete (wires server + helper) |
| `CastOverlay.kt` | androidMain | UI | Complete (sender HUD, AVID) |
| `CastReceiverView.kt` | androidMain | UI | Complete (JPEG → Bitmap → Compose) |
| `MjpegTcpServer.kt` | androidMain | Deprecated | @Deprecated (replaced by WebSocket) |
| `MjpegTcpClient.kt` | androidMain | Deprecated | @Deprecated (replaced by WebSocket) |
| `DesktopCastManager.kt` | desktopMain | Implementation | Complete (java.awt.Robot capture + WebSocket) |

---

## 4. Bidirectional Protocol Extension

RemoteCast moves beyond one-way video to a symmetric, multi-message protocol. The 20-byte header enables extensibility: new message types can be added (e.g., "TAPS" for touch events) without breaking existing clients (they ignore unknown magic bytes).

### AVU Command Format with Routing

Commands flow in both directions. Format: **VOS Compact v3.0 (pipe-delimited)** with optional routing target.

**Baseline AVU format (from VoiceOSCore):**
```
phrase|actionType|targetAvid|confidence|locale
```

**Extended for RemoteCast (target routing):**
```
phrase|actionType|targetAvid|confidence|locale|target:ROUTING_MODE
```

Where `ROUTING_MODE` is one of: `local`, `remote`, `auto`.

### Three Routing Modes Explained

#### Mode 1: LOCAL
Command executes on glasses only. Example: "volume up", "menu", "home".
```
voice:menu|NAVIGATION|BTN:menu|0.95|en-US|target:local
```
Receiver (`GlassClient`) parses, routes to local command handler (glasses menu controller). Phone never sees it.

#### Mode 2: REMOTE
Command relays to phone for execution. Example: "click submit", "type email".
```
voice:click_submit|INPUT|BTN:7c4d2a1e|0.92|en-US|target:remote
```
Receiver parses, sends CMD message back to sender (phone). Phone's VoiceOSCore handlers execute it.

#### Mode 3: AUTO
Receiver decides based on context. Example: "go back".
- If glasses menu is open: local (back button on menu)
- If phone is streaming: remote (back to previous screen)
- If both are active: disambiguate via modal dialog or default to remote

```
voice:go_back|NAVIGATION|BTN:back|0.88|en-US|target:auto
```

### Unified Frame Router (pseudocode)

```kotlin
fun handleIncomingMessage(header: CastHeader, payload: ByteArray) {
    when (header.magic) {
        "CAST" -> {
            // Video frame from sender
            val bitmap = BitmapFactory.decodeByteArray(payload, 0, payload.size)
            updateScreenDisplay(bitmap)
        }
        "CMD\0" -> {
            // Command: either incoming (remote execution) or outgoing (local ack)
            val avuText = payload.decodeToString()
            val cmd = VosParser.parseCommand(avuText)
            when (cmd.targetRouting) {
                TargetRouting.LOCAL -> executeLocally(cmd)
                TargetRouting.REMOTE -> relayCmdToPhone(cmd)
                TargetRouting.AUTO -> routeAuto(cmd)
            }
        }
        "IMU\0" -> {
            // Head tracking quaternion
            val qx = payload.getFloat(0)
            val qy = payload.getFloat(4)
            val qz = payload.getFloat(8)
            val qw = payload.getFloat(12)
            val ts = payload.getLong(16)
            updateHeadTracking(Quaternion(qx, qy, qz, qw), ts)
        }
        "TTS\0" -> {
            // Audio feedback (future)
            playAudioFeedback(payload)
        }
        else -> {
            // Unknown magic byte; ignore (forward compatibility)
            logger.warn("Unknown RemoteCast message type: ${header.magic}")
        }
    }
}
```

### Dual-Stream .VOS Profiles on Receiver

Two profiles loaded simultaneously on glasses:

1. **`glasses.local.vos`** (pre-installed in APK assets)
   - Commands: menu, back, home, volume ↑/↓, brightness ↑/↓, cursor show/hide, HUD toggle
   - Locale-specific, bundled at APK build time
   - Handler: `GlassLocalCommandHandler` (androidMain)

2. **`phone.synced.vos`** (synced from sender at stream start)
   - Commands: click <element>, type <text>, launch <app>, scroll, swipe, drag
   - Phone's live VOS profile (reflects phone's current app + context)
   - Handler: `GlassRemoteCommandHandler` (routes to phone via CMD message)

Parser loads both into memory. Grammar union: local + synced. Speech engine generates two confidence hypotheses (one per grammar), or single hypothesis matched against union.

---

## 5. GlassClient Architecture (Thin Receiver APK)

Full Android glasses run GlassClient: a lightweight app (~5 MB, ~30 MB RAM) that displays the incoming video stream and routes voice commands.

### App Structure

```
Apps/Android/GlassClient/
├── ui/
│   ├── GlassClientActivity.kt           ← Single activity, fullscreen
│   ├── CastViewScreen.kt               ← Cast stream + HUD composite
│   ├── ConnectionScreen.kt             ← Pairing UI (IP entry, mDNS discovery)
│   └── GlassSettings.kt                ← Display mode, quality, shortcuts
├── service/
│   ├── GlassClientService.kt           ← Foreground service
│   ├── CastReceiver.kt                 ← WebSocket listener
│   └── SpeechRecognitionManager.kt     ← On-device ASR
├── command/
│   ├── GlassCommandRouter.kt           ← Route to local/remote/auto
│   ├── GlassLocalCommandHandler.kt     ← Local commands
│   ├── GlassRemoteCommandHandler.kt    ← Relay to phone
│   └── CmdMessageBuilder.kt            ← Serialize commands back to phone
├── imu/
│   ├── IMUSensorManager.kt             ← Accelerometer + gyro + mag
│   └── QuaternionCalculator.kt         ← Sensor fusion (future: Madgwick)
└── model/
    ├── GlassSettings.kt
    ├── ConnectionState.kt
    └── IMUData.kt
```

### Dependencies
- `Modules/RemoteCast` (ICastManager, protocols)
- `Modules/VoiceOSCore` (speech pipeline, command parsing)
- `Modules/DeviceManager` (sensor access)
- `Modules/Foundation` (settings, file I/O)
- `Modules/AvanueUI` (theme, components, AVID)
- `HTTPAvanue` (transitive, via RemoteCast)

### Footprint
- **APK size:** ~5 MB (stripped, no resources)
- **Memory:** ~30 MB RAM at runtime (Compose + WebSocket + ASR model)
- **Battery (WiFi, Cast mode):** ~2-3%/hr (WiFi radio + display + compute)
- **Battery (BLE, HUD mode):** ~0.1%/hr (Bluetooth LE + minimal display)

### Key Screens

**GlassClientActivity** → Single fullscreen activity. No traditional navigation drawer (glasses UX: minimal chrome). Shows either ConnectionScreen or CastViewScreen based on `connectedPhoneIp`.

**ConnectionScreen** → mDNS discovery or manual IP entry. QR code scanner optional (future). Once paired, stores IP to DataStore. Launches CastReceiverService in foreground.

**CastViewScreen** → Composable that displays `CastReceiverView` (JPEG stream) + HUD overlays:
- Top-left: Connection status badge (WiFi strength, latency, FPS)
- Bottom: Command suggestions (AVID badges for top 3-5 active commands)
- Center-right: Head-tracking reticle (if IMU enabled)

**GlassSettings** → Toggle display mode (MONO_GREEN vs FULL_COLOR), video quality, IMU on/off, command log, battery saver preset.

---

## 6. GlassAvanue Server Architecture

Phone/tablet/desktop app that detects which glasses are connected and adapts output. On-device intelligence (screen scraping, command generation) runs here, not on glasses.

### Auto-Selection Logic

`GlassDetector` (androidMain) queries DeviceManager for connected peripherals:
- If BLE device UUID matches Vuzix Z100 → HUD Mode
- If WiFi device on known vendor list → Cast Mode
- Fallback: prompt user to select

### App Structure

```
Apps/Android/GlassAvanue/
├── ui/
│   ├── GlassAvanueActivity.kt         ← Main UI: pair, configure, stats
│   ├── PairingScreen.kt              ← QR code + mDNS + manual IP
│   ├── CastingScreen.kt              ← Live stream, device list, command log
│   └── DeviceSettingsScreen.kt       ← Per-device audio, quality, IMU prefs
├── service/
│   ├── GlassAvanueService.kt         ← Foreground service (MediaProjection)
│   ├── ScreenCaptureWorker.kt        ← Periodic screen capture (low battery mode)
│   └── CommandDispatcherService.kt   ← Listen for incoming commands (CMD msg)
├── detection/
│   ├── GlassDetector.kt              ← DeviceManager queries
│   ├── HudModeController.kt          ← Ultralite SDK integration (future)
│   └── CastModeController.kt         ← WebSocket streaming
├── vocab/
│   ├── VocabSyncManager.kt           ← Push phone's VOS to receiver
│   ├── VosProfileBuilder.kt          ← Extract active commands
│   └── DeltaVocabCalculator.kt       ← Optimize sync (only diffs)
└── model/
    ├── ConnectedGlass.kt
    ├── GlassAvanueSettings.kt
    └── CastingSession.kt
```

### Dependencies
- `Modules/RemoteCast` (ICastManager, CastWebSocketServer)
- `Modules/VoiceOSCore` (command handler registry, VosParser)
- `Modules/DeviceManager` (glass detection, sensor config)
- `Modules/Foundation` (settings, services)
- `Modules/AvanueUI` (UI components, AVID)

### Mode: HUD Mode vs Cast Mode

| Aspect | HUD Mode | Cast Mode |
|--------|----------|-----------|
| **Glasses** | Vuzix Z100, BLE peripherals | Moverio, RealWear, Blade, Rokid |
| **Transport** | Bluetooth LE (Ultralite SDK) | WiFi TCP (WebSocket) |
| **Phone sends** | Text labels + AVID numbers | MJPEG/JPEG video stream |
| **Frame rate** | ~2-5 FPS (BLE bandwidth) | ~15-20 FPS (WiFi) |
| **Latency** | ~200-500ms (BLE hops) | ~50-100ms (local network) |
| **Battery drain** | ~0.1%/hr | ~2-5%/hr |
| **User experience** | Heads-up command hints | Full-screen context |
| **Status** | Future (Phase I) | Current (Phase A-H) |

**HUD Mode Roadmap** (not yet implemented):
- Integrate Vuzix Ultralite SDK (Java library)
- Pack text labels + SVG command badges
- Send via BLE instead of WebSocket
- Display on monocular eyepiece (640x480 green)

**Cast Mode** (fully working):
- Already implemented: ScreenCaptureHelper, AndroidCastManager, WebSocket server
- Runs in foreground service with MediaProjection
- Continuous stream or on-demand (low-power mode)

### Key Services

**GlassAvanueService** → Foreground service that:
1. Acquires MediaProjection (consent dialog on start)
2. Creates VirtualDisplay (1280x720 or per-setting resolution)
3. Binds CastWebSocketServer on port 54321
4. Encodes frames to JPEG
5. Sends via WebSocket binary messages
6. Listens for CMD messages (incoming commands from glasses)
7. Updates TopAppBar with live stats (FPS, latency, connected device)

**VocabSyncManager** → On screen change (detected via AccessibilityService or manual trigger):
1. Queries current app's VOS profile from VoiceOSCore
2. Serializes to binary (VOS v3.0 compact format)
3. Sends to connected glasses via CMD message with target routing info
4. Receiver loads new .vos profile into grammar

**CommandDispatcherService** → Listens for CMD messages from glasses:
1. Deserializes AVU text from payload
2. Routes to appropriate handler (e.g., input, media, device)
3. Executes locally on phone
4. Optional: send confirmation CMD back to glasses

### Pairing Flow

1. User opens GlassAvanue on phone, taps "Pair Glasses"
2. Displays QR code (encodes phone's IP + WiFi SSID)
3. User points glasses camera at QR, scans
4. Phone's mDNS service advertises `_glassavanue._tcp.local` with IP
5. Glasses resolve mDNS, connect to phone's port 54321
6. Handshake: Glasses send device info (model, OS version, display mode)
7. Phone responds: app name, version, initial VOS profile
8. Glasses load profile, display "Ready" in HUD
9. Phone enters Cast mode, begins streaming JPEG

---

## 7. SDK-Specific Implementation Paths

### Tier 1: Vuzix Blade 2

Standard Android 9 device. No special SDK needed for video (native DisplayManager + MediaProjection).

**GlassClient sideload:**
```bash
adb connect <blade-ip>
adb install -r GlassClient.apk
adb shell am start -n com.augmentalis.glassclient/.ui.GlassClientActivity
```

**Networking:** Blade has WiFi + Bluetooth. For Cast Mode, use standard Socket on port 54321. For accelerometer/gyro, access via SensorManager as normal.

**Display:** Standard Compose, no special API. Use AvanueTheme with FULL_COLOR palette. Binocular display (side-by-side stereo possible but not required for RemoteCast Phase A).

### Tier 2: Vuzix Z100

Monocular right-eye display. 640x480, monochrome green (ITU-R BT.601 luminance only).

**Ultralite SDK (Android library):**
```gradle
repositories {
    maven { url "https://vuzix-ultralite-repository.jfrog.io/artifactory/gradle-release" }
}
dependencies {
    implementation "com.vuzix.ultralite:ultralite:+" // Latest version
}
```

**API model:**
- `UltraLiteManager` (singleton, request lifecycle permission)
- `UltraLiteManager.startUltraLite()` → initializes display
- `Text(x, y, text, color)` → render text
- `Image(x, y, bitmap)` → render bitmap
- `SVG(x, y, svgXml)` → render SVG (vector)
- `BLE` connection is automatic (phone discovers Z100 via `BluetoothAdapter.startDiscovery()`)

**HUD Mode implementation** (future Phase I):
- GlassClient variant: `Z100GlassClient` (androidMain, vendor-specific)
- Instead of CastReceiverView, render via Ultralite SDK
- Phone sends text labels (app name, command suggestions) instead of JPEG
- Bandwidth: ~50 bytes/frame = negligible BLE load
- Frame rate: ~5 FPS (BLE can sustain ~1 Mbps, enough for text)

### Tier 3a: Mentra Live (TypeScript MiniApp)

MentraOS is Android-based but MiniApps run in a sandbox (V8 JavaScript engine). Requires TypeScript SDK:

```npm
npm install @mentra/sdk
```

**MiniApp structure:**
```typescript
// index.ts
import { WebSocket, FileSystem, VoiceSDK } from "@mentra/sdk";

const ws = new WebSocket(`ws://${connectedPhoneIP}:54321/cast/stream`);

ws.onmessage = (event: MessageEvent<ArrayBuffer>) => {
    const header = new Uint8Array(event.data, 0, 20);
    const magic = String.fromCharCode(...header.subarray(0, 4)).trim();

    if (magic === "CAST") {
        const jpegPayload = new Uint8Array(event.data, 20);
        displayImage(jpegPayload); // Mentra Display API
    } else if (magic === "CMD\0") {
        const avuText = new TextDecoder().decode(
            new Uint8Array(event.data, 20)
        );
        routeCommand(avuText);
    }
};

VoiceSDK.recognize().then((text) => {
    ws.send(serializeAVU(text, "REMOTE"));
});
```

**Packaging:** MiniApp is a .zip with `manifest.json` + TypeScript compiled to WASM. Distribution: Mentra Marketplace. Same wire protocol; different SDK integration.

### Tier 3b: XReal Aura (Android XR Preview 3)

XReal Aura is a headset running Android XR OS (preview). For untethered operation, GlassClient requires Android XR SDK:

```gradle
dependencies {
    implementation "com.google.android.xr:xr-runtime:0.20241231" // Preview 3
}
```

**Key differences:**
- `android.xr.XrActivityCompat` base class (instead of AppCompatActivity)
- `android.xr.display.DisplayController` for eye buffer management (handles stereo automatically)
- Voice input: use `android.xr.speech.VoiceInputManager` (managed by OS, not custom ASR)
- Sensors: `android.xr.sensor.SensorManager` (hand tracking + 6DoF head tracking built-in)

**GlassClient for Aura:**
```kotlin
class GlassClientActivity : XrActivityCompat() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.cast_view_screen) // Same Compose layout
        // XR runtime handles stereo rendering
        connectToPhone()
    }
}
```

**IMU:** Aura provides head tracking natively via `android.xr.sensor.DeviceTrackingState`. No manual quaternion calculation needed.

### Tier 3c: Even Realities G1

Proprietary OS. API documentation: https://github.com/evrly-io/g1-sdk-beta (GitLab beta access).

**Key constraint:** OS is not Android. WebView-based UI, native bridge for hardware. WebSocket via WebView's native fetch API.

**Approach:**
- GlassClient becomes HTML5 + JavaScript single-page app
- Hosted on phone at `/assets/glassclient.html`
- Open in Even G1 browser (via deep link or manual URL)
- WebSocket client in JavaScript (same wire protocol)
- Voice input: proprietary G1 voice API
- Sensor access: G1 native bridge

**File structure:**
```
Modules/RemoteCast/
├── web/
│   ├── glassclient.html          ← Even G1 compatible SPA
│   └── glassclient.js
```

---

## 8. Voice Command Routing (VOCAB Sync)

Commands flow bidirectionally. Receiver (glasses) holds two .VOS profiles in memory, each feeding a separate speech grammar. Upon speech recognition, the engine returns confidence scores from both grammars; the router picks the highest-confidence match and routes it (local, remote, or auto).

### Dual-Grammar Architecture

```
LocalGrammar (glasses.local.vos)
    ↓
    ├─ "menu"
    ├─ "back"
    ├─ "home"
    ├─ "volume up"
    ├─ "volume down"
    ├─ "brightness up"
    ├─ "brightness down"
    └─ ... (10-15 glasses-specific commands)

SyncedGrammar (phone.synced.vos, dynamic)
    ↓
    ├─ "click submit"
    ├─ "click cancel"
    ├─ "type email"
    ├─ "launch calculator"
    ├─ "scroll down"
    ├─ "swipe left"
    └─ ... (varies per phone app; 20-50 commands)

Union → SpeechRecognizer (OFFLINE model, on-device)
    ↓
    Result: phrase + confidence + targetGrammar
    ↓
    GlassCommandRouter
        ├─ targetGrammar == LOCAL? → GlassLocalCommandHandler
        ├─ targetGrammar == REMOTE? → serialize CMD, send to phone
        └─ targetGrammar == AUTO? → context-dependent logic
```

### .VOS Profile Sync Mechanism

**On phone (sender):**
1. User launches an app (e.g., Gmail)
2. AccessibilityService or manual trigger fires
3. VoiceOSCore scrapes screen, generates VOS profile for current context
4. CommandHandler registry returns: list of executable commands (e.g., "send email", "add recipient")
5. VocabSyncManager serializes to VOS Compact v3.0 format
6. Wraps in CMD message (magic="CMD\0")
7. Sends via WebSocket: `ws.send(byteArrayOf(...header...) + avuPayload)`

**On glasses (receiver):**
1. WebSocket listener receives CMD message
2. Extracts payload (AVU text)
3. VosParser.parseMultiline() reads all commands
4. Registers commands to SyncedGrammar
5. Speech engine now recognizes both old and new commands
6. Upon next speech input, ASR can match synced commands

### AVU Payload Example (Phone → Glasses)

```
voice:send_email|MEDIA|BTN:a1b2c3d4|1.0|en-US|target:remote
voice:add_recipient|INPUT|BTN:e5f6g7h8|1.0|en-US|target:remote
voice:discard|NAVIGATION|BTN:i9j0k1l2|1.0|en-US|target:remote
```

Glasses parse these three commands, add to grammar. User says "send email" → matches SyncedGrammar entry, confidence 1.0, routes to remote (relayed to phone).

### Command Routing Rules

| Phrase | Best Grammar | Default Route | Auto Logic |
|--------|--------------|----------------|-----------|
| "menu" | LocalGrammar | local | (always local) |
| "back" | ? depends | auto | If glasses menu open → local. If cast active → remote. |
| "volume up" | LocalGrammar | local | (always local) |
| "click submit" | SyncedGrammar | remote | (always remote) |
| "scroll down" | SyncedGrammar OR LocalGrammar (both have scroll) | auto | If glasses HUD menu open → local. If phone cast → remote. |

### Context for AUTO Routing

```kotlin
data class RoutingContext(
    val glassesMenuOpen: Boolean,
    val phoneScreenActive: Boolean,
    val lastActiveCommand: String?, // History: what was the user doing?
    val focusedElement: AccessibilityNodeInfo?, // What element has keyboard focus?
)

fun routeAuto(command: VosCommand, context: RoutingContext): RoutingMode {
    return when {
        context.glassesMenuOpen && hasLocalMatch(command) -> LOCAL
        context.phoneScreenActive && hasSyncedMatch(command) -> REMOTE
        else -> {
            // Ambiguous: present choices or default to REMOTE
            // (phone typically has more app context than glasses)
            REMOTE
        }
    }
}
```

---

## 9. Head Tracking & IMU

IMU (Inertial Measurement Unit) streams real-time head orientation from glasses to phone. Enables: cursor head-tracking on phone, HUD reticle on glasses, future 6DoF interaction.

### IMU Data Format

Binary payload in IMU messages: 24 bytes total.

```
Offset  Size  Field       Type      Range
 0-3    4     qx          float32   [-1, 1]
 4-7    4     qy          float32   [-1, 1]
 8-11   4     qz          float32   [-1, 1]
12-15   4     qw          float32   [-1, 1]  (scalar component)
16-23   8     timestamp   int64     ms since epoch
```

Quaternion (qx, qy, qz, qw) represents 3D rotation: glasses' current orientation relative to a "neutral" baseline (e.g., looking straight ahead).

### Sensor Fusion (Accelerometer + Gyroscope + Magnetometer)

GlassClient reads raw sensor data and fuses into quaternion. Algorithms:

| Algorithm | Complexity | Latency | Accuracy | Drift |
|-----------|-----------|---------|----------|-------|
| Complementary filter | Low | <5ms | ±5° | Gyro drift over minutes |
| Kalman filter | Medium | ~10ms | ±3° | Compensated by accel |
| Madgwick | Medium | ~10ms | ±2° | Best (IMU + mag) |

**Default (Phase A):** Complementary filter (fast, minimal drift for interactive use).
**Future:** Madgwick AHRS if drift becomes noticeable.

### Implementation (IMUSensorManager.kt, androidMain)

```kotlin
class IMUSensorManager(context: Context) {
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    private val accelSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val gyroSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
    private val magSensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

    private val accelData = FloatArray(3)
    private val gyroData = FloatArray(3)
    private val magData = FloatArray(3)

    private val rotationMatrix = FloatArray(9)
    private val orientationAngles = FloatArray(3)

    fun startTracking(sampleRateHz: Int = 60) {
        val sensorDelay = when {
            sampleRateHz >= 100 -> SensorManager.SENSOR_DELAY_FASTEST
            sampleRateHz >= 50 -> SensorManager.SENSOR_DELAY_GAME
            else -> SensorManager.SENSOR_DELAY_NORMAL
        }

        sensorManager.registerListener(this, accelSensor, sensorDelay)
        sensorManager.registerListener(this, gyroSensor, sensorDelay)
        sensorManager.registerListener(this, magSensor, sensorDelay)
    }

    override fun onSensorEvent(event: SensorEvent) {
        when (event.sensor.type) {
            Sensor.TYPE_ACCELEROMETER -> accelData = event.values.clone()
            Sensor.TYPE_GYROSCOPE -> gyroData = event.values.clone()
            Sensor.TYPE_MAGNETIC_FIELD -> magData = event.values.clone()
        }

        // Update rotation matrix when all three inputs available
        if (accelData.isNotEmpty() && magData.isNotEmpty()) {
            SensorManager.getRotationMatrix(rotationMatrix, null, accelData, magData)
            SensorManager.getOrientation(rotationMatrix, orientationAngles)

            // Convert Euler angles to quaternion
            val quat = eulerToQuaternion(orientationAngles)
            imuDataFlow.emit(IMUData(quat, System.currentTimeMillis()))
        }
    }

    private fun eulerToQuaternion(angles: FloatArray): Quaternion {
        // angles: [azimuth (yaw), pitch, roll]
        val cy = cos(angles[0] * 0.5f)
        val sy = sin(angles[0] * 0.5f)
        val cp = cos(angles[1] * 0.5f)
        val sp = sin(angles[1] * 0.5f)
        val cr = cos(angles[2] * 0.5f)
        val sr = sin(angles[2] * 0.5f)

        return Quaternion(
            x = sr * cp * cy - cr * sp * sy,
            y = cr * sp * cy + sr * cp * sy,
            z = cr * cp * sy - sr * sp * cy,
            w = cr * cp * cy + sr * sp * sy
        )
    }
}

data class IMUData(
    val quaternion: Quaternion,
    val timestampMs: Long
)

data class Quaternion(
    val x: Float, val y: Float, val z: Float, val w: Float
)

// Phone-side: listen to IMU stream and map head rotation to cursor movement
val imuManager = IMUSensorManager(context)
imuManager.imuDataFlow.collect { imuData ->
    val headYaw = extractYaw(imuData.quaternion) // -180 to +180 degrees
    val headPitch = extractPitch(imuData.quaternion) // -90 to +90 degrees

    // Map head orientation to screen cursor position
    // Example: full head rotation left = cursor moves to left edge
    val screenWidth = displayMetrics.widthPixels
    val screenHeight = displayMetrics.heightPixels
    val cursorX = (screenWidth / 2f) + (headYaw / 180f) * (screenWidth / 4f)
    val cursorY = (screenHeight / 2f) - (headPitch / 90f) * (screenHeight / 4f)

    // Send CMD message to phone with cursor position (future)
}
```

### Integration with Phone (Cursor Head-Tracking)

Phone receives IMU stream, updates cursor position in real-time. Requires:
1. GlassAvanueService listening for IMU messages
2. CursorAvanueHandler mapping quaternion → screen position
3. AccessibilityService injecting synthetic MotionEvent at new cursor position

```kotlin
// Phone side: CursorHeadTrackingManager
fun onIMUData(imu: IMUData) {
    val yaw = extractYaw(imu.quaternion)
    val pitch = extractPitch(imu.quaternion)

    // Map to cursor screen position
    val cursorX = computeCursorX(yaw)
    val cursorY = computeCursorY(pitch)

    // Inject MotionEvent (requires INJECT_EVENTS permission + accessibility service)
    accessibilityClickDispatcher.moveCursor(cursorX, cursorY)

    // Broadcast event for UI components that want live cursor position
    cursorPositionFlow.emit(Pair(cursorX, cursorY))
}
```

---

## 10. Monochrome Display Support

Some glasses (RealWear, Z100) have monochrome displays (green or amber). RemoteCast handles this gracefully via GPU color transformation on the receiver.

### Monochrome Display Modes

| Mode | Color | Glasses | Use Case |
|------|-------|---------|----------|
| **MONO_GREEN** | #00FF00 (ITU-R BT.601 luminance) | Vuzix Z100, RealWear | Standard industrial |
| **MONO_WHITE** | #FFFFFF | Google Glass, legacy | Light backgrounds |
| **MONO_AMBER** | #FFAA00 | Military night vision | Low-light ops |
| **FULL_COLOR** | RGB (all channels) | Blade, Moverio, Rokid | Modern glasses |

### Luminance Transform (GPU Filter)

Monochrome conversion via ColorMatrix (ITU-R BT.601 standard):

```kotlin
// Green monochrome: weight R, G, B channels to single green output
val monoGreenMatrix = ColorMatrix(floatArrayOf(
    0f,     0f,     0f,     0f, 0f,      // Red channel → 0
    0.299f, 0.587f, 0.114f, 0f, 0f,     // RGB weighted sum → Green
    0f,     0f,     0f,     0f, 0f,      // Blue channel → 0
    0f,     0f,     0f,     1f, 0f       // Alpha passthrough
))

// Applied in Compose:
Image(
    bitmap = frameBuffer,
    contentDescription = null,
    modifier = Modifier
        .fillMaxSize()
        .graphicsLayer {
            if (displayMode == DisplayColorMode.MONO_GREEN) {
                colorFilter = ColorFilter.colorMatrix(monoGreenMatrix)
            }
        }
)
```

Zero network overhead—phone sends full-color JPEG; glasses apply filter locally upon decompression.

### MonoGreenColors Palette (AvanueColorScheme Subclass)

Custom color palette optimized for green monochrome displays:

```kotlin
object MonoGreenColors : AvanueColorScheme {
    override val primary = Color(0xFF00FF41)          // Bright green
    override val onPrimary = Color(0xFF001A00)        // Dark green
    override val secondary = Color(0xFF00CC33)        // Medium green
    override val onSecondary = Color(0xFF000F00)      // Very dark green

    override val surface = Color(0xFF001A00)          // Almost black (rest state)
    override val onSurface = Color(0xFF00FF41)        // Text: bright green

    override val background = Color(0xFF000000)       // Pure black
    override val onBackground = Color(0xFF00FF41)

    override val error = Color(0xFF00FF00)            // Vivid green for alerts
    override val onError = Color(0xFF000000)

    override val outline = Color(0xFF007A1F)          // Muted green for borders
    override val outlineVariant = Color(0xFF003D0F)   // Even darker green
}
```

When `displayMode == DisplayColorMode.MONO_GREEN`, GlassClient applies both:
1. **ColorMatrix filter** (RGB → luminance → boost green channel)
2. **MonoGreenColors theme** (UI components use green palette)

Result: user sees a cohesive green-on-black interface optimized for the Z100 eyepiece.

### DisplayProfile Integration

GlassClient reports its display capabilities via DeviceManager:

```kotlin
// On glasses startup
DeviceManager.registerDisplay(
    displayProfile = DisplayProfile.GLASS_MICRO,
    colorMode = DisplayColorMode.MONO_GREEN,
    resolutionDp = Pair(320, 240),  // 640x480 physical, scaled to density
    refreshRateHz = 30
)

// Phone can query and adapt output
val glassDisplay = DeviceManager.queryDisplay()
if (glassDisplay.colorMode == DisplayColorMode.MONO_GREEN) {
    // Send lower-contrast JPEG (more visible on mono display)
    jpegQuality = 80  // Slightly higher quality to compensate for filtering
    frameRateCap = 15  // Fewer FPS to save WiFi bandwidth on low-power glasses
}
```

---

## 11. Implementation Phases

| Phase | Title | Depends On | Est. Files | Status |
|-------|-------|-----------|-----------|--------|
| **A** | Extract HUDManager → Modules/HUDManager | Nothing | 18 new, 5 modified | Ready to start |
| **B** | RemoteCast foreground service + MediaProjection consent | Nothing | 5 new, 2 modified | In progress |
| **C** | CMD message type + AVU wire format in protocol | Phase B | 3 modified | Queued |
| **D** | Monochrome ColorMatrix + MONO_GREEN palette | AvanueUI v5.1 | 4 new, 1 modified | Queued |
| **E** | DeviceManager: FormFactor.GLASSES + DisplayColorMode | Foundation | 3 modified | Queued |
| **F** | GlassClient app scaffold (connection, CastViewScreen) | Phases A-E | 8 new | Queued |
| **G** | GlassCommandRouter (local vs remote) + handlers | Phase F | 2 new, 1 modified | Queued |
| **H** | .VOS profile sync to glasses (VocabSyncManager) | Phase G + SFTP | 2 modified | Queued |
| **I** | Vendor SDK integration (Vuzix, Mentra, XReal) | Phase H | TBD (phase per vendor) | Future research |

### Phase A: Extract HUDManager → Modules/HUDManager

**Goal:** Extract HUD rendering logic from Cockpit's SpatialVoice (command suggestions, badges, reticle, status) into a reusable KMP module.

**What's moving:**
- `HUDManager` interface (KMP commonMain)
- `SuggestionChip`, `AVIDBadge`, `StatusBadge`, `Reticle` composables (KMP commonMain)
- `AndroidHUDManager` (androidMain, uses Compose)
- `DesktopHUDManager` (desktopMain)
- Color theming: HUD uses AvanueTheme.colors.* + module accents

**Why:** GlassAvanueService + GlassClient both need HUD overlays. Sharing code avoids duplication.

**Files created:** 18 (HUDManager interface + 4 composables + 2 implementations + tests + unit test mocks)
**Files modified:** 5 (remove HUD code from Cockpit SpatialVoice, import from HUDManager module instead)

### Phase B: RemoteCast Foreground Service + MediaProjection Consent

**Goal:** Wire up ScreenCaptureHelper + AndroidCastManager into a foreground service that:
1. Acquires MediaProjection (via notification + consent dialog)
2. Captures screen continuously or on-demand
3. Runs JPEG encoding loop
4. Manages WebSocket server lifecycle

**What's new:**
- `GlassAvanueService` (foreground service, `foregroundServiceType="mediaProjection"`)
- `MediaProjectionHelper` (manages consent activity, notification builder)
- `ScreenCaptureWorker` (for low-battery mode: periodic capture instead of continuous)
- Intent filters: `Intent.ACTION_MEDIA_PROJECTION_DISMISS` handling

**Files created:** 5 (service, helpers, workers)
**Files modified:** 2 (AndroidManifest.xml for permission + service, build.gradle.kts for service type)

**Testing:** Create consent dialog, deny → service stops gracefully. Grant → JPEG loop runs. Pause → frame rate drops to 2 FPS.

### Phase C: CMD Message Type + AVU Wire Format

**Goal:** Extend RemoteCast protocol to handle bidirectional commands.

**What's new:**
- `CastFrameData` updated: already supports magic byte dispatch
- `AVUCommand` data class: phrase, actionType, targetAvid, confidence, locale, targetRouting
- `CmdMessageBuilder` for serializing AVU → UTF-8 bytes
- Unit tests: AVU round-trip (serialize/deserialize)

**Files modified:** 3
- `CastFrameData.kt`: add CMD case to frame router
- `AVU` or new `CmdProtocol.kt`: define AVUCommand + TargetRouting enum
- Tests: `CastFrameDataTest.kt` add CMD case

### Phase D: Monochrome ColorMatrix + MONO_GREEN Palette

**Goal:** Support Z100 green monochrome display.

**What's new:**
- `MonoGreenColors` (AvanueColorScheme subclass)
- `DisplayColorMode` enum (FULL_COLOR, MONO_GREEN, MONO_WHITE, MONO_AMBER)
- `MonochromeFilterFactory` (creates ColorMatrix for each mode)

**Files created:** 4
- `MonoGreenColors.kt`
- `DisplayColorMode.kt`
- `MonochromeFilterFactory.kt`
- Tests: `ColorMatrixTest.kt` (verify luminance transform)

**Files modified:** 1
- `AvanueColorScheme.kt`: add MONO_GREEN as optional subclass (not required for all colors)

### Phase E: DeviceManager Extended FormFactor & DisplayColorMode

**Goal:** Allow GlassAvanue to query connected glasses' capabilities.

**What's new:**
- `FormFactor.GLASSES` enum value
- `DisplayColorMode` enum (replicate from Phase D)
- `Display` model: colorMode, resolutionDp, refreshRateHz
- `DeviceManager.registerDisplay()` (glasses call this)
- `DeviceManager.queryDisplay()` (phone calls this)

**Files modified:** 3
- `FormFactor.kt`: add GLASSES
- `DeviceManager.kt`: add display registry + query methods
- Tests: `DeviceManagerTest.kt`

### Phase F: GlassClient App Scaffold

**Goal:** Create GlassClient app structure. Implement connection, pairing, CastViewScreen.

**What's new:**
- `GlassClientActivity` (single fullscreen activity)
- `ConnectionScreen` (IP entry, mDNS discovery)
- `CastViewScreen` (CastReceiverView + HUD composite)
- `CastReceiverService` (background WebSocket listener)
- Data models: ConnectionState, GlassSettings

**Files created:** 8
- `GlassClientActivity.kt`
- `ConnectionScreen.kt`
- `CastViewScreen.kt`
- `CastReceiverService.kt`
- `ConnectionState.kt`
- `GlassSettings.kt`
- `AndroidManifest.xml` entries
- `CastReceiverScope.kt` (dependency injection)

**Integration:** `Apps/Android/GlassClient/` app module. Depends on: RemoteCast, VoiceOSCore, Foundation, AvanueUI.

### Phase G: GlassCommandRouter + Local/Remote Handlers

**Goal:** Implement command routing logic.

**What's new:**
- `GlassCommandRouter` (context-aware routing)
- `GlassLocalCommandHandler` (menu, volume, brightness, HUD toggles)
- `GlassRemoteCommandHandler` (relay to phone)
- `RoutingContext` data class

**Files created:** 2
- `GlassCommandRouter.kt`
- `GlassLocalCommandHandler.kt` (GlassRemoteCommandHandler can be inline)

**Files modified:** 1
- `CastReceiverService.kt`: wire router into CMD message handler

### Phase H: .VOS Profile Sync (VOCAB Sync)

**Goal:** Phone syncs active command vocabulary to glasses.

**What's new:**
- `VocabSyncManager` (on phone side)
- `DeltaVocabCalculator` (optimize: only send diffs)
- `VosProfileBuilder` (extract active commands from context)
- Sync trigger: AccessibilityService detects app change OR manual trigger

**Files modified:** 2
- `GlassAvanueService.kt`: add VocabSyncManager, listen for screen changes
- `CastReceiverService.kt` (glasses): load synced .vos on CMD message arrival

**Integration with SFTP:** Phase H doesn't require SFTP. Sync is real-time, sent via WebSocket CMD message. SFTP would be future enhancement (save/load profiles across sessions).

### Phase I: Vendor SDK Integration (Research + TBD)

**Goal:** Tier 2, 3 support (Vuzix Z100, Mentra, XReal, Even Realities).

**What's involved (per vendor):**
- SDK download + license agreement
- Native binding layer (Kotlin wrapper around Java/C++ SDK)
- Proof-of-concept app
- Testing on real hardware (not emulator)

**Estimated timeline:**
- Vuzix Z100 Ultralite (Phase I-A): 2-3 weeks (BLE integration, UI kit layout)
- Mentra TypeScript MiniApp (Phase I-B): 3-4 weeks (WASM toolchain, Mentra Marketplace submission)
- XReal Aura Android XR (Phase I-C): 1-2 weeks (SDK already available, similar to Phase F)
- Even Realities G1 (Phase I-D): 2-3 weeks (reverse-engineer SDK docs, test in cloud sandbox)

Each sub-phase files: ~10-15 (SDK wrapper + proof-of-concept app).

---

## 12. Cross-Platform Support

RemoteCast wire protocol is platform-agnostic. Both sender and receiver can target multiple platforms.

### Sender Implementations

| Sender | Platform | Status | Notes |
|--------|----------|--------|-------|
| **GlassAvanue** | Android | Phase A-B in progress | Phone/tablet app |
| **GlassAvanue** | Desktop | DesktopCastManager complete | Java AWT Robot capture |
| **GlassAvanue** | iOS | Phase I (future) | ReplayKit screen capture |

### Receiver Implementations

| Receiver | Platform | Status | Notes |
|----------|----------|--------|-------|
| **GlassClient** | Android (Tier 1) | Phase F-G planned | Full APK |
| **GlassClient** | Android (Tier 2) | Phase I-A (future) | Ultralite SDK variant |
| **GlassClient** | Mentra (Tier 3a) | Phase I-B (future) | TypeScript MiniApp |
| **GlassClient** | XReal Aura (Tier 3b) | Phase I-C (future) | Android XR |
| **GlassClient** | Even G1 (Tier 3c) | Phase I-D (future) | HTML5 + JS |

### KMP Sharing Strategy

**commonMain modules:**
- `ICastManager`, `CastState`, `CastFrameData` — all platforms implement
- `CastWebSocketServer`, `CastWebSocketClient` — via HTTPAvanue (KMP)
- Protocol parsers (magic byte, CAST header, AVU format)

**Platform-specific (expect/actual):**
- `ICastManager.startCapture()` → AndroidCastManager (MediaProjection), DesktopCastManager (Robot), IosCastManager (ReplayKit)
- Socket binding: handled by HTTPAvanue (already has expect/actual for Socket)
- IMU sensor fusion: Android only (Phase A-E); iOS uses CMMotionManager (Phase I)

**Example: iOS Sender**

```kotlin
// commonMain
expect fun createCastManager(config: CastConfig): ICastManager

// iosMain
actual fun createCastManager(config: CastConfig): ICastManager {
    return IosCastManager(config)
}

// IosCastManager.kt (iosMain)
class IosCastManager(config: CastConfig) : ICastManager {
    private val screenRecorder = ScreenRecorder() // ReplayKit wrapper

    override fun startCapture() {
        screenRecorder.startRecording { pixelBuffer ->
            // Convert CVPixelBuffer → JPEG
            val jpegData = compressJPEG(pixelBuffer, quality = 70)
            val frameData = CastFrameData.encode(jpegData, sequenceNo++, System.currentTimeMillis())
            wsServer.broadcast(frameData.toByteArray())
        }
    }
}
```

### Cross-Platform Testing Matrix

| Test Scenario | Sender | Receiver | Expected | Status |
|---------------|--------|----------|----------|--------|
| Phone → Glasses (WiFi) | Android Phone | Android Blade | JPEG stream 15 FPS | Phase B |
| Desktop → Glasses (WiFi) | Desktop (macOS) | Android Blade | JPEG stream 15 FPS | Needs wiring |
| Phone → Phone (local) | Android Phone | Android Phone | Self-mirror test | Stretch goal |
| Phone → Web (future) | Android Phone | Browser (web receiver) | WebSocket + JPEG | Future |

---

## Conclusion

RemoteCast transforms augmented reality glasses from passive displays into active voice-enabled thin clients. By leveraging the bidirectional WebSocket protocol, VOCAB sync, and command routing, any phone can become a VoiceOS control center for any compatible glasses.

**Next Steps:**

1. **Phase A** → Extract HUDManager (start immediately)
2. **Phase B** → Foreground service + MediaProjection (depends on Phase A)
3. **Phase C-E** → Extend protocols and DeviceManager (can run in parallel with A + B)
4. **Phase F-H** → GlassClient app + command routing (blocks on phases C-E)
5. **Phase I** → Vendor SDKs (start research in parallel, but defer implementation until phase H is stable)

---

**For questions on architecture, protocol design, or vendor integration, see:**
- Chapter 101: HTTPAvanue (WebSocket + HTTP/2 foundation)
- Chapter 95: VOS Distribution & Handler Dispatch (command routing)
- Chapter 93: Voice Command Pipeline & Localization (speech recognition)
- Chapter 94: 4-Tier Voice Enablement (AVID system)

**Related documentation:**
- Spec: `docs/plans/RemoteCast/RemoteCast-Spec-GlassClientArchitecture-260219-V1.md`
- SDK research: `docs/analysis/RemoteCast/RemoteCast-Analysis-SmartGlassesSDKResearch-260219-V1.md`
- UI mockups: `docs/demo/remoteavanue/RemoteCast-UI-Mockups.html`
