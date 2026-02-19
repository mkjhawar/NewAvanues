# RemoteCast Spec: Smart Glasses Client Architecture
**Date:** 2026-02-19 | **Version:** V1 | **Status:** DRAFT — Pending SDK Research

## 1. Overview

Transform RemoteCast from a one-way screen mirror into a **bidirectional VoiceOS thin client** system. Smart glasses become a voice-controlled window into any connected phone/tablet/desktop.

**Target Devices:**
- Vuzix Z100 (enterprise monocular)
- Vuzix Blade (consumer AR)
- Mentra glasses (to be researched)
- Future: RealWear Navigator, XReal, Rokid, TCL RayNeo

**Open Question:** Which devices require vendor SDKs vs standard Android APIs?

---

## 2. How RemoteCast Currently Works

### Sender Path (Phone → Network)
```
MediaProjection
  → VirtualDisplay (mirrors framebuffer to ImageReader surface)
  → ImageReader (acquires RGBA_8888 frames)
  → Bitmap.compress(JPEG, quality%)
  → CastFrameData (20-byte header + JPEG payload)
  → MjpegTcpServer (TCP port 54321)
```

### Receiver Path (Network → Display)
```
MjpegTcpClient (TCP port 54321)
  → Read 20-byte header (validate "CAST" magic)
  → Read N bytes JPEG payload
  → Flow<ByteArray> of frames
  → BitmapFactory.decodeByteArray()
  → Compose Image() renders
```

### Wire Protocol (20-byte CAST header)
```
Offset  Size  Field           Value
 0-3     4    Magic           "CAST" (0x43415354)
 4-7     4    Sequence No.    Monotonic Int
 8-15    8    Timestamp       System.currentTimeMillis()
16-19    4    Payload Size    Byte count of JPEG that follows
```

### Quality Profiles
| Profile | Resolution | JPEG Quality | Target FPS |
|---------|-----------|-------------|-----------|
| LOW     | 640×360   | 50%         | ~10       |
| MEDIUM  | 1280×720  | 70%         | ~15       |
| HIGH    | 1920×1080 | 85%         | ~20       |

### Existing Files (All Implemented)
| File | Location | Status |
|------|----------|--------|
| ICastManager.kt | commonMain | Complete |
| CastState.kt | commonMain | Complete |
| CastFrameData.kt | commonMain | Complete (encode/decode/buildPacket) |
| ScreenCaptureHelper.kt | androidMain | Complete (VirtualDisplay → JPEG Flow) |
| MjpegTcpServer.kt | androidMain | Complete (ServerSocket, mutex writes) |
| MjpegTcpClient.kt | androidMain | Complete (cold Flow, magic validation) |
| AndroidCastManager.kt | androidMain | Complete (orchestrator, needs MediaProjection) |
| CastOverlay.kt | androidMain | Complete (sender HUD overlay, AVID) |
| CastReceiverView.kt | androidMain | Complete (JPEG → Bitmap → Compose) |
| DesktopCastManager.kt | desktopMain | Complete (java.awt.Robot, no TCP wired) |
| CastCommandHandler.kt | VoiceOSCore handlers | Stub (returns failure, not wired) |

### What's Missing (The "Wiring")
1. **Foreground service** with `foregroundServiceType="mediaProjection"` — Android OS requirement
2. **Consent activity flow** — `MediaProjectionManager.createScreenCaptureIntent()` → result → token
3. **CastCommandHandler** wiring to actual `AndroidCastManager` instance

---

## 3. Bidirectional Protocol Extension (AVU Wire Format)

### Multi-Message Protocol
Same 20-byte header structure, different magic bytes per message type:

```
CAST frame:  [20-byte header, magic="CAST"][JPEG payload]       — video
CMD command: [20-byte header, magic="CMD\0"][AVU text payload]   — voice commands
IMU data:    [20-byte header, magic="IMU\0"][binary IMU data]    — head tracking (future)
TTS audio:   [20-byte header, magic="TTS\0"][audio payload]      — speech feedback (future)
```

### AVU Command Payload Format
Uses VOS compact format v3.0 (pipe-delimited) with target routing extension:

```
phrase|actionType|targetAvid|confidence|locale|target:local|remote|auto
```

**Examples:**
```
# Voice command from glasses → phone executes
click 3|CLICK|BTN:a3f2b1c0|0.95|en-US|target:remote

# Local glasses command
menu|NAVIGATE||1.0|en-US|target:local

# Auto-routed (context decides)
scroll down|SCROLL_DOWN||1.0|en-US|target:auto

# Phone → glasses notification
Battery low|NOTIFICATION||1.0|en-US|target:local
```

### Unified Frame Router (Receiver Side)
```kotlin
when (magic) {
    "CAST" -> handleVideoFrame(payload)      // JPEG → display
    "CMD\0" -> handleCommand(payload)         // AVU → processVoiceCommand()
    "IMU\0" -> handleIMUData(payload)         // head tracking → cursor
    "TTS\0" -> handleAudio(payload)           // speech → speaker
}
```

---

## 4. Split Execution Model

### Command Routing on Glasses
```
Speech Recognition → VosParser → CommandRouter
                                      │
                    ┌─────────────────┼─────────────────┐
                    ▼                 ▼                  ▼
                 LOCAL            REMOTE              AUTO
              (glasses)         (relay AVU)       (context decides)
                    │                │                  │
          HUD menu, volume,   click element,     scroll, go back,
          brightness, cursor   type text,         depends on what's
          show/hide HUD        launch app         active on screen
```

### Local Commands (Execute on Glasses)
- menu / back / home (glasses UI navigation)
- volume up/down, brightness
- cursor show/hide (HUD gaze cursor)
- show/hide HUD overlays
- "what's on screen" (local TTS readout)

### Remote Commands (Relay to Phone via AVU)
- click <element> (AVID-targeted)
- type <text>
- launch <app>
- swipe / drag / scroll (on phone screen)
- Any command with a targetAvid

### Auto Commands (Context-Dependent)
- "go back" → glasses menu if menu open, phone if cast stream active
- "scroll down" → glasses overlay list if open, phone screen if cast active

---

## 5. Module Architecture

### Glasses App Dependencies
```
Apps/Android/GlassClient/
├── Modules:DeviceManager       ← hardware detection (FormFactor.GLASSES, display type)
├── Modules:HUDManager          ← local UI, spatial rendering, gaze tracking, voice indicators
├── Modules:RemoteCast          ← cast receiver + CMD relay + AVU wire format
├── Modules:VoiceOSCore         ← speech recognition + command routing (relay mode)
├── Modules:AvanueUI            ← theme system (MONO_GREEN palette, GLASS_MICRO profile)
├── Modules:Foundation          ← settings, logging, platform abstractions
└── Modules:Localization        ← multi-locale voice commands
```

### DeviceManager Role
```kotlin
data class DeviceCapabilities(
    val formFactor: FormFactor,              // PHONE, TABLET, GLASSES, DESKTOP
    val displayColorMode: DisplayColorMode,  // FULL_COLOR, MONO_GREEN, MONO_WHITE, MONO_AMBER
    val hasTouch: Boolean,
    val hasMicrophone: Boolean,
    val isVoiceOnlyInput: Boolean,
    val supportsHeadTracking: Boolean,       // IMU available
)

enum class FormFactor { PHONE, TABLET, GLASSES, DESKTOP, WATCH }
enum class DisplayColorMode { FULL_COLOR, MONO_GREEN, MONO_WHITE, MONO_AMBER }
```

DeviceDetector auto-detects glasses hardware:
- Vuzix: `Build.MANUFACTURER == "Vuzix"` or package prefix `com.vuzix`
- RealWear: `Build.MANUFACTURER == "RealWear"`
- Mentra: TBD (needs SDK research)

### HUDManager Role
- Spatial rendering (cast stream + overlay composites)
- Gaze/head tracking via IMU (already implemented: GazeTracker.kt)
- Voice indicator system ("listening" / "processing" / "recognized")
- Local menu/HUD overlays
- Settings UI for glasses display preferences

### VoiceOSCore Role (Relay Mode)
- Speech recognition (local, on-device)
- VosParser for AVU command parsing
- CommandRouter for local vs remote dispatch
- .VOS profile loading (local + synced from phone via SFTP)
- 4-Tier voice still applies: Tier 1 AVID (from cast overlays), Tier 4 .VOS profiles

---

## 6. Monochrome Display Support

### Layer A: Cast Frame Color Transform (GPU)
```kotlin
// Convert RGB → green-channel luminance (ITU-R BT.601 weights)
val monochromeGreen = ColorMatrix(floatArrayOf(
    0f,     0f,     0f,     0f, 0f,   // R = 0
    0.299f, 0.587f, 0.114f, 0f, 0f,   // G = luminance
    0f,     0f,     0f,     0f, 0f,   // B = 0
    0f,     0f,     0f,     1f, 0f    // A = unchanged
))
// Applied via Compose Modifier.drawWithContent or Paint.colorFilter
```

Phone sends full-color JPEG. Glasses apply GPU filter. Zero network overhead.

### Layer B: Local UI Palette
```kotlin
object MonoGreenColors : AvanueColorScheme(
    primary       = Color(0xFF00FF41),  // Bright green (accent)
    surface       = Color(0xFF001A00),  // Near-black green (background)
    textPrimary   = Color(0xFF00CC33),  // Medium green (text)
    textSecondary = Color(0xFF007A1F),  // Dim green
    error         = Color(0xFF00FF00),  // Flash green (warnings)
)
```

### Layer C: DisplayProfile Integration
```kotlin
AvanueThemeProvider(
    colors = MonoGreenColors,
    materialMode = MaterialMode.Glass,
    isDark = true,  // glasses always dark
    displayProfile = DisplayProfile.GLASS_MICRO
)
```

### Color Mode Options
| Mode | Use Case | Cast Filter | UI Palette |
|------|----------|------------|-----------|
| FULL_COLOR | Vuzix Blade, color displays | None | Standard HYDRA |
| MONO_GREEN | RealWear, Vuzix Z100 | Green luminance | MonoGreenColors |
| MONO_WHITE | Google Glass style | White luminance | MonoWhiteColors |
| MONO_AMBER | Military/night vision | Amber luminance | MonoAmberColors |

---

## 7. .VOS Profile Sync

Glasses load TWO .vos profiles simultaneously:

### glasses.local.vos (Pre-installed on glasses)
```
menu|NAVIGATE||1.0|en-US|target:local
go home|NAVIGATE||1.0|en-US|target:local
volume up|DEVICE||1.0|en-US|target:local
brightness up|DEVICE||1.0|en-US|target:local
show hud|UI||1.0|en-US|target:local
hide hud|UI||1.0|en-US|target:local
```

### phone.synced.vos (Synced from phone via VOS SFTP Sync)
```
# Static + scraped commands from phone
# Gives glasses full knowledge of phone's command vocabulary
click *|CLICK||0.95|en-US|target:remote
type *|TYPE||1.0|en-US|target:remote
launch *|APP||1.0|en-US|target:remote
```

Existing VOS SFTP Sync (Phase B, already implemented) handles the file transfer.

---

## 8. GlassClient App Structure

```
Apps/Android/GlassClient/
├── build.gradle.kts
├── src/main/
│   ├── AndroidManifest.xml
│   └── kotlin/com/augmentalis/glassclient/
│       ├── GlassClientActivity.kt        ← Single activity, fullscreen
│       ├── GlassClientService.kt         ← Background: speech + command relay
│       ├── ConnectionScreen.kt           ← Pair with phone (IP entry or mDNS)
│       ├── CastViewScreen.kt            ← Cast stream + HUD overlays composite
│       ├── GlassCommandRouter.kt         ← local/remote/auto routing
│       └── GlassSettings.kt             ← Color mode, quality, display prefs
```

---

## 9. Implementation Phases

| Phase | What | Depends On | Est. Files |
|-------|------|-----------|-----------|
| A | Extract HUDManager → Modules:HUDManager | Nothing | ~18 |
| B | RemoteCast foreground service + consent flow | Nothing | ~5 new |
| C | CMD message type + AVU wire format in protocol | Phase B | ~3 modified |
| D | Monochrome ColorMatrix + MONO_GREEN palette | AvanueUI | ~4 new |
| E | DeviceManager: FormFactor.GLASSES + DisplayColorMode | DeviceManager | ~3 modified |
| F | GlassClient app scaffold | Phases A-E | ~8 new |
| G | CommandRouter (local vs remote) | Phase F | ~2 new |
| H | .VOS profile sync to glasses | Phase G + SFTP | ~2 modified |
| I | Vendor SDK integration (Vuzix, Mentra) | Phase F + research | TBD |

Phases A + B can run in parallel.

---

## 10. Open Research Questions

### Vendor SDK Requirements
- [ ] **Vuzix Z100:** Does the Z100 require the Vuzix SDK for speech/display/IMU? Or is standard Android sufficient?
- [ ] **Vuzix Blade:** Same question. Blade has a touchpad — does it need Vuzix HID SDK?
- [ ] **Mentra glasses:** What platform do they run? Android? Custom OS? What SDKs are available?
- [ ] **Display detection:** Can we detect monochrome display capability via standard Android APIs, or do we need vendor-specific queries?
- [ ] **Voice input:** Do these glasses have built-in speech recognition, or do we need to run our own (VoiceOSCore)?

### Protocol Questions
- [ ] mDNS/DNS-SD for device discovery (vs manual IP entry)
- [ ] Authentication between glasses and phone (pairing flow)
- [ ] Encryption for the TCP stream (TLS or pre-shared key?)
- [ ] Maximum viable latency for voice→action loop (target: <300ms)

### UX Questions
- [ ] How does the user pair glasses with phone for the first time?
- [ ] What happens when cast connection drops? (reconnect policy)
- [ ] Should glasses show a "connecting..." screen or go to standby?
- [ ] Battery optimization — can we reduce JPEG quality when glasses battery is low?

---

## 11. Key Architectural Decisions

| Decision | Choice | Rationale |
|----------|--------|-----------|
| Protocol format | AVU (VOS compact v3.0) | Already exists, human-readable, same parser everywhere |
| Command routing | target:local/remote/auto | Explicit routing with context-aware fallback |
| Color adaptation | GPU ColorMatrix on receiver | Zero network overhead, phone sends full color |
| Glasses local UI | HUDManager + AvanueTheme | Already built, spatial rendering + gaze tracking |
| Device detection | DeviceManager.DeviceDetector | Centralized hardware abstraction |
| Profile sync | VOS SFTP Sync | Already implemented (Phase B) |
| Communication | Bidirectional TCP (same port, muxed by magic) | Simple, reuses existing transport |
