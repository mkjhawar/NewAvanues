# RemoteCast — Full Implementation Plan
**Document:** RemoteCast-Plan-FullImplementation-260219-V1.md
**Date:** 2026-02-19
**Branch:** Cockpit-Development
**Mode:** .yolo .cot
**Author:** Manoj Jhawar

---

## 1. Summary

RemoteCast is a KMP screen-casting module that streams device screens over MJPEG-over-TCP. The module currently contains two files (`CastState.kt` + `CastDevice` inline model). This plan implements the full capture, encode, transmit, receive, and decode pipeline on Android and Desktop, wires 5 voice commands in all 5 locales, and connects the receiver view into the Cockpit ScreenCast frame.

**Target KMP Score:** ~35% (protocol models + wire format in commonMain; capture is inherently platform-specific)

---

## 2. Current State

### Existing Files (2)

| File | Path | Status |
|------|------|--------|
| `CastState.kt` | `commonMain/.../model/CastState.kt` | Exists — needs enhancement |
| `CastDevice` | Inline in `CastState.kt` | Exists — extract to own file |

### What Is Missing
- No capture pipeline (MediaProjection / Robot)
- No TCP transport layer
- No MJPEG frame encode/decode
- No receiver Composable for Cockpit
- No foreground service
- No voice commands
- No `ICastManager` interface
- No device discovery (mDNS/NsdManager)

### Build Config (Already Correct)
`build.gradle.kts` already declares:
- KMP targets: `androidTarget` + `jvm("desktop")`
- `AvanueUI` dependency in `androidMain`
- Kotlin serialization plugin
- `minSdk = 29` (MediaProjection available since API 21, full stability at 29)

---

## 3. Architecture

```
commonMain
  model/
    CastState.kt          (ENHANCE — richer status + quality)
    CastDevice.kt         (EXTRACT from CastState.kt)
    CastQualityProfile.kt (NEW)
  protocol/
    CastFrameData.kt      (NEW — wire format)
    ICastManager.kt       (NEW — interface)

androidMain
  service/
    CastCaptureService.kt   (NEW — foreground service + MediaProjection)
    ScreenCaptureHelper.kt  (NEW — VirtualDisplay + ImageReader)
  transport/
    MjpegTcpServer.kt       (NEW — ktor-network TCP server)
    MjpegTcpClient.kt       (NEW — ktor-network TCP client)
  ui/
    CastOverlay.kt          (NEW — floating cast-in-progress indicator)
    CastReceiverView.kt     (NEW — Composable frame renderer)
  controller/
    AndroidCastManager.kt   (NEW — implements ICastManager)

desktopMain
  transport/
    DesktopMjpegServer.kt   (NEW)
    DesktopMjpegClient.kt   (NEW)
  controller/
    DesktopCastManager.kt   (NEW — java.awt.Robot capture)

VoiceOSCore/androidMain
  handlers/
    CastCommandHandler.kt   (NEW — CAST ActionCategory)
```

---

## 4. Wire Protocol

### Frame Format (MJPEG-over-TCP)

Every frame is preceded by a fixed 20-byte header:

```
Offset  Size  Field
0       4     Magic bytes: 0x43 0x41 0x53 0x54  ("CAST")
4       4     Sequence number (Int, big-endian)
8       8     Timestamp epoch ms (Long, big-endian)
16      4     Payload size in bytes (Int, big-endian)
20      N     JPEG payload bytes
```

The receiver reads the 20-byte header first, extracts `payloadSize`, then reads exactly that many bytes for the JPEG frame. Out-of-order frames (sequence gap > 5) are dropped silently; the display shows the last valid frame.

### Quality Profiles

| Profile | Height | Width  | FPS | JPEG Quality |
|---------|-------:|-------:|----:|-------------|
| LOW     | 360    | 640    | 5   | 30           |
| MEDIUM  | 720    | 1280   | 15  | 60           |
| HIGH    | 1080   | 1920   | 30  | 80           |

---

## 5. Phase 1 — commonMain Models

### 5.1 CastState.kt (ENHANCE)

**Path:** `Modules/RemoteCast/src/commonMain/kotlin/com/augmentalis/remotecast/model/CastState.kt`

Current fields: `deviceName`, `deviceId`, `isConnected`, `isStreaming`, `resolution` (CastResolution enum), `frameRate`, `latencyMs`, `error`.

**Changes:**
- Replace `CastResolution` enum with `CastQualityProfile` reference
- Add `connectionStatus: CastConnectionStatus` enum (replaces two Boolean flags)
- Add `sourceDeviceId`, `sourceDeviceName`, `targetDeviceId`, `targetDeviceName`
- Rename `error` to `errorMessage` for clarity
- Keep `@Serializable` annotation
- Extract `CastDevice` to its own file

**New `CastConnectionStatus` enum (inline in `CastState.kt`):**
```kotlin
enum class CastConnectionStatus {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    STREAMING,
    ERROR
}
```

### 5.2 CastDevice.kt (EXTRACT + ENHANCE)

**Path:** `Modules/RemoteCast/src/commonMain/kotlin/com/augmentalis/remotecast/model/CastDevice.kt`

```kotlin
@Serializable
data class CastDevice(
    val id: String,
    val name: String,
    val address: String,
    val port: Int = 54321,
    val deviceType: CastDeviceType = CastDeviceType.PHONE,
    val isAvailable: Boolean = true
)

enum class CastDeviceType { PHONE, TABLET, DESKTOP, GLASS }
```

### 5.3 CastQualityProfile.kt (NEW)

**Path:** `Modules/RemoteCast/src/commonMain/kotlin/com/augmentalis/remotecast/model/CastQualityProfile.kt`

```kotlin
enum class CastQualityProfile(
    val height: Int,
    val width: Int,
    val fps: Int,
    val jpegQuality: Int  // 0-100
) {
    LOW(360, 640, 5, 30),
    MEDIUM(720, 1280, 15, 60),
    HIGH(1080, 1920, 30, 80)
}
```

### 5.4 CastFrameData.kt (NEW)

**Path:** `Modules/RemoteCast/src/commonMain/kotlin/com/augmentalis/remotecast/protocol/CastFrameData.kt`

```kotlin
data class CastFrameData(
    val frameBytes: ByteArray,
    val timestamp: Long,
    val sequenceNumber: Int,
    val width: Int,
    val height: Int
) {
    companion object {
        const val FRAME_HEADER_SIZE = 20
        val MAGIC_BYTES = byteArrayOf(0x43, 0x41, 0x53, 0x54) // "CAST"
    }
}
```

Includes `encodeHeader(): ByteArray` and `companion fun decodeHeader(bytes: ByteArray): HeaderInfo` for the 20-byte header read/write logic.

### 5.5 ICastManager.kt (NEW)

**Path:** `Modules/RemoteCast/src/commonMain/kotlin/com/augmentalis/remotecast/protocol/ICastManager.kt`

```kotlin
interface ICastManager {
    val state: StateFlow<CastState>
    val availableDevices: Flow<List<CastDevice>>

    suspend fun startCasting(quality: CastQualityProfile = CastQualityProfile.MEDIUM)
    suspend fun stopCasting()
    suspend fun connectToDevice(device: CastDevice)
    suspend fun disconnect()
    fun setQuality(quality: CastQualityProfile)
}
```

---

## 6. Phase 2 — androidMain Implementation

### 6.1 CastCaptureService.kt (NEW)

**Path:** `Modules/RemoteCast/src/androidMain/kotlin/com/augmentalis/remotecast/service/CastCaptureService.kt`

**Type:** `Service` (not `LifecycleService` — no lifecycle dependency needed)

**Key responsibilities:**
- Declared as `FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION` in AndroidManifest
- Receives `MediaProjection` via `Intent` extra (passed from Activity after user grant)
- Creates `VirtualDisplay` using `MediaProjectionManager.createVirtualDisplay()`
- Creates `ImageReader` at quality profile dimensions, format `JPEG`
- Installs `ImageReader.OnImageAvailableListener` → compresses each frame to JPEG ByteArray → sends to `MjpegTcpServer`
- Posts persistent notification: "RemoteCast: Screen casting in progress" with Stop action
- Cleans up `VirtualDisplay` + `ImageReader` + `MediaProjection` in `onDestroy()`

**Notification channel ID:** `"remotecast_cast_channel"`

### 6.2 ScreenCaptureHelper.kt (NEW)

**Path:** `Modules/RemoteCast/src/androidMain/kotlin/com/augmentalis/remotecast/service/ScreenCaptureHelper.kt`

Static object with:
```kotlin
fun createVirtualDisplay(
    projection: MediaProjection,
    displayManager: DisplayManager,
    profile: CastQualityProfile,
    imageReader: ImageReader
): VirtualDisplay
```

Uses `DisplayManager.createVirtualDisplay()` with `VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR`. Returns the VirtualDisplay for lifecycle management by the caller (Service).

### 6.3 MjpegTcpServer.kt (NEW)

**Path:** `Modules/RemoteCast/src/androidMain/kotlin/com/augmentalis/remotecast/transport/MjpegTcpServer.kt`

**Dependency:** `io.ktor:ktor-network:3.0.3`

```kotlin
class MjpegTcpServer(
    private val port: Int = 54321,
    private val scope: CoroutineScope
) {
    val isClientConnected: StateFlow<Boolean>

    fun start()          // Binds TCP socket, waits for single client
    fun stop()           // Cancels all sockets + coroutines
    suspend fun sendFrame(frame: CastFrameData)  // Writes header + payload
}
```

Uses `aSocket(ActorSelectorManager(Dispatchers.IO)).tcp().bind()`. Accepts only one client at a time. If a second client connects, the first is dropped. Write failures emit `isClientConnected = false`.

### 6.4 MjpegTcpClient.kt (NEW)

**Path:** `Modules/RemoteCast/src/androidMain/kotlin/com/augmentalis/remotecast/transport/MjpegTcpClient.kt`

```kotlin
class MjpegTcpClient(
    private val scope: CoroutineScope
) {
    val frames: SharedFlow<ByteArray>          // JPEG frame bytes
    val connectionState: StateFlow<Boolean>

    suspend fun connect(device: CastDevice)
    fun disconnect()
}
```

Reads 20-byte header, validates magic bytes, reads `payloadSize` bytes. On invalid magic → drops frame and re-syncs by scanning for next magic sequence. On connection loss → emits `connectionState = false`, caller decides whether to reconnect.

### 6.5 CastOverlay.kt (NEW)

**Path:** `Modules/RemoteCast/src/androidMain/kotlin/com/augmentalis/remotecast/ui/CastOverlay.kt`

Composable floating indicator shown while casting is active. Uses `AvanueTheme.colors.error` (red = live) as accent. Displays:
- Red dot pulse animation
- "Casting" label
- Quality badge (LOW / MEDIUM / HIGH)
- Latency display (e.g., "42 ms")

AVID: `Modifier.semantics { contentDescription = "Voice: Screen cast indicator" }`

### 6.6 CastReceiverView.kt (NEW)

**Path:** `Modules/RemoteCast/src/androidMain/kotlin/com/augmentalis/remotecast/ui/CastReceiverView.kt`

```kotlin
@Composable
fun CastReceiverView(
    frameFlow: Flow<ByteArray>,
    state: CastState,
    modifier: Modifier = Modifier
)
```

Collects frames from `frameFlow`. Decodes `ByteArray` → `Bitmap` via `BitmapFactory.decodeByteArray()` → renders with `Image(bitmap.asImageBitmap())`. Maintains aspect ratio. Overlays connection status when `state.connectionStatus != STREAMING`.

AVID on status overlay: `contentDescription = "Voice: Cast receiver screen, ${state.connectionStatus.name}"`

### 6.7 AndroidCastManager.kt (NEW)

**Path:** `Modules/RemoteCast/src/androidMain/kotlin/com/augmentalis/remotecast/controller/AndroidCastManager.kt`

Implements `ICastManager`. Manages:
- `MjpegTcpServer` lifecycle (starts on `startCasting()`, stops on `stopCasting()`)
- `CastCaptureService` binding (sends intent to start the foreground service)
- `MjpegTcpClient` (for receive mode: `connectToDevice()`)
- `NsdManager` for mDNS discovery — registers service type `_remotecast._tcp` when broadcasting; browses same type when discovering

State machine:
```
DISCONNECTED → (startCasting) → CONNECTING → (client connects) → STREAMING
STREAMING → (stopCasting) → DISCONNECTED
STREAMING → (client drops) → CONNECTED (waiting)
ANY → (error) → ERROR → (retry) → CONNECTING
```

### 6.8 CastCommandHandler.kt (NEW)

**Path:** `Modules/VoiceOSCore/src/androidMain/kotlin/com/augmentalis/voiceoscore/handlers/CastCommandHandler.kt`

Extends `BaseHandler`. `canHandle(category)` returns `category == ActionCategory.CAST`.

Handles 5 `CommandActionType` values:

| CommandActionType | Voice Phrase (en-US) | Action |
|-------------------|---------------------|--------|
| `CAST_START` | "start casting" | calls `castManager.startCasting()` |
| `CAST_STOP` | "stop casting" | calls `castManager.stopCasting()` |
| `CAST_CONNECT` | "cast connect" | calls `castManager.connectToDevice(nearest)` |
| `CAST_DISCONNECT` | "cast disconnect" | calls `castManager.disconnect()` |
| `CAST_SET_QUALITY` | "cast quality [low/medium/high]" | calls `castManager.setQuality(profile)` |

Registered in `AndroidHandlerFactory.createHandlers()` as entry 12 (after existing 11 handlers).

---

## 7. Phase 3 — desktopMain Implementation

### 7.1 DesktopMjpegServer.kt (NEW)

**Path:** `Modules/RemoteCast/src/desktopMain/kotlin/com/augmentalis/remotecast/transport/DesktopMjpegServer.kt`

Identical protocol to `MjpegTcpServer.kt` (same `CastFrameData` header format). Uses `ktor-network` JVM artifact. No Android-specific imports.

### 7.2 DesktopMjpegClient.kt (NEW)

**Path:** `Modules/RemoteCast/src/desktopMain/kotlin/com/augmentalis/remotecast/transport/DesktopMjpegClient.kt`

Identical protocol to `MjpegTcpClient.kt`. Emits `Flow<ByteArray>` of JPEG frames for display in Compose Desktop `Image()`.

### 7.3 DesktopCastManager.kt (NEW)

**Path:** `Modules/RemoteCast/src/desktopMain/kotlin/com/augmentalis/remotecast/controller/DesktopCastManager.kt`

Implements `ICastManager` for Desktop. Capture via:

```kotlin
val robot = java.awt.Robot()
val screenSize = java.awt.Toolkit.getDefaultToolkit().screenSize
val capture: BufferedImage = robot.createScreenCapture(
    java.awt.Rectangle(0, 0, screenSize.width, screenSize.height)
)
```

Scales to quality profile dimensions with `BufferedImage.getScaledInstance()`. Compresses to JPEG via `javax.imageio.ImageIO.write(scaled, "jpg", baos)` at `jpegQuality` level using `JPEGImageWriteParam`. Repeats on a `FixedRateTimer` at `profile.fps` interval.

No foreground service needed on Desktop; runs as a coroutine loop.

---

## 8. Phase 4 — Cockpit Integration

### 8.1 FrameContent (Already Exists)

`FrameContent.ScreenCast` is already declared in `FrameContent.kt` (confirmed in handover research). No change needed.

### 8.2 ContentRenderer (WIRE CastReceiverView)

**Path:** `Modules/Cockpit/src/androidMain/kotlin/com/augmentalis/cockpit/ui/ContentRenderer.kt`

In the `FrameContent.ScreenCast ->` branch, replace the stub with:

```kotlin
FrameContent.ScreenCast -> {
    val castManager = remember { AndroidCastManager(LocalContext.current, scope) }
    val state by castManager.state.collectAsState()
    CastReceiverView(
        frameFlow = castManager.frames,
        state = state,
        modifier = Modifier.fillMaxSize()
    )
}
```

### 8.3 ContentAccent (WIRE red = live)

In `ContentAccent.kt`, the `FrameContent.ScreenCast` entry maps to `AvanueTheme.colors.error` (red). This signals "live/recording" status consistent with broadcast conventions.

If the entry is missing, add:

```kotlin
FrameContent.ScreenCast -> AvanueTheme.colors.error
```

---

## 9. Phase 5 — VOS Commands (5 Locales)

### 9.1 New ActionCategory Value

Add to `ActionCategory.kt` in `VoiceOSCore`:

```kotlin
CAST(priority = 18)
```

Placed after `CAMERA(14)`, `ANNOTATION(15)`, `IMAGE(16)`, `VIDEO(17)` — consistent with other new categories being added in the six-module implementation.

### 9.2 New CommandActionType Values

Add to `CommandActionType.kt` in `VoiceOSCore`:

```kotlin
CAST_START,
CAST_STOP,
CAST_CONNECT,
CAST_DISCONNECT,
CAST_SET_QUALITY,
```

### 9.3 VOS Command Entries (en-US.app.vos)

Append 5 new entries to `en-US.app.vos`:

```
CAST_START|start casting|start screen cast|begin casting|ActionCategory.CAST
CAST_STOP|stop casting|stop screen cast|end casting|ActionCategory.CAST
CAST_CONNECT|cast connect|connect to cast|connect screen|ActionCategory.CAST
CAST_DISCONNECT|cast disconnect|disconnect cast|stop cast connection|ActionCategory.CAST
CAST_SET_QUALITY|cast quality|set cast quality|change cast quality|ActionCategory.CAST
```

### 9.4 Translations (4 Locales)

| Command | es-ES | fr-FR | de-DE | hi-IN |
|---------|-------|-------|-------|-------|
| CAST_START | iniciar transmisión | lancer la diffusion | Übertragung starten | कास्टिंग शुरू करें |
| CAST_STOP | detener transmisión | arrêter la diffusion | Übertragung stoppen | कास्टिंग बंद करें |
| CAST_CONNECT | conectar transmisión | connecter la diffusion | Übertragung verbinden | कास्ट कनेक्ट करें |
| CAST_DISCONNECT | desconectar transmisión | déconnecter la diffusion | Übertragung trennen | कास्ट डिस्कनेक्ट करें |
| CAST_SET_QUALITY | calidad de transmisión | qualité de diffusion | Übertragungsqualität | कास्ट गुणवत्ता |

---

## 10. Dependencies to Add

### build.gradle.kts Changes

```kotlin
// In commonMain dependencies:
implementation("io.ktor:ktor-network:3.0.3")

// In androidMain dependencies:
// No new additions — AvanueUI + Compose already present
// MediaProjection is part of Android SDK (no extra dependency)
// NsdManager is part of Android SDK (no extra dependency)
```

**Note:** `ktor-network` 3.0.3 is the same version already used elsewhere in the project for KMP networking. Verify with `libs.toml` alias before adding — it may already be catalogued.

---

## 11. Manifest Additions (Apps/Android/VoiceOS)

```xml
<!-- In AndroidManifest.xml -->
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_MEDIA_PROJECTION" />
<uses-permission android:name="android.permission.INTERNET" />

<service
    android:name="com.augmentalis.remotecast.service.CastCaptureService"
    android:foregroundServiceType="mediaProjection"
    android:exported="false" />
```

---

## 12. File Inventory

### New Files (17 Kotlin + 5 VOS)

| File | Source Set | Type |
|------|-----------|------|
| `model/CastDevice.kt` | commonMain | EXTRACT from CastState.kt |
| `model/CastQualityProfile.kt` | commonMain | NEW |
| `protocol/CastFrameData.kt` | commonMain | NEW |
| `protocol/ICastManager.kt` | commonMain | NEW |
| `service/CastCaptureService.kt` | androidMain | NEW |
| `service/ScreenCaptureHelper.kt` | androidMain | NEW |
| `transport/MjpegTcpServer.kt` | androidMain | NEW |
| `transport/MjpegTcpClient.kt` | androidMain | NEW |
| `ui/CastOverlay.kt` | androidMain | NEW |
| `ui/CastReceiverView.kt` | androidMain | NEW |
| `controller/AndroidCastManager.kt` | androidMain | NEW |
| `transport/DesktopMjpegServer.kt` | desktopMain | NEW |
| `transport/DesktopMjpegClient.kt` | desktopMain | NEW |
| `controller/DesktopCastManager.kt` | desktopMain | NEW |
| `handlers/CastCommandHandler.kt` | VoiceOSCore/androidMain | NEW |
| `en-US.app.vos` | assets | APPEND (5 entries) |
| `es-ES.app.vos` | assets | APPEND |
| `fr-FR.app.vos` | assets | APPEND |
| `de-DE.app.vos` | assets | APPEND |
| `hi-IN.app.vos` | assets | APPEND |

### Modified Files

| File | Change |
|------|--------|
| `model/CastState.kt` | Enhance — richer status fields, replace CastResolution with CastQualityProfile |
| `ActionCategory.kt` | Add `CAST(priority = 18)` |
| `CommandActionType.kt` | Add 5 `CAST_*` values |
| `AndroidHandlerFactory.kt` | Register `CastCommandHandler` |
| `ContentRenderer.kt` | Wire `CastReceiverView` in ScreenCast branch |
| `ContentAccent.kt` | Confirm/add `FrameContent.ScreenCast -> colors.error` |
| `build.gradle.kts` | Add `ktor-network` dependency |
| `AndroidManifest.xml` | Add permissions + service declaration |

---

## 13. Implementation Order

Work is ordered by dependency (foundation before consumers):

1. **commonMain models** — `CastState`, `CastDevice`, `CastQualityProfile`, `CastFrameData`, `ICastManager`
2. **androidMain transport** — `MjpegTcpServer`, `MjpegTcpClient`
3. **androidMain service** — `ScreenCaptureHelper`, `CastCaptureService`
4. **androidMain controller** — `AndroidCastManager`
5. **androidMain UI** — `CastOverlay`, `CastReceiverView`
6. **desktopMain** — `DesktopMjpegServer`, `DesktopMjpegClient`, `DesktopCastManager`
7. **VoiceOSCore** — `ActionCategory` + `CommandActionType` additions, `CastCommandHandler`, `AndroidHandlerFactory` registration
8. **VOS files** — all 5 locales
9. **Cockpit wiring** — `ContentRenderer`, `ContentAccent`
10. **Manifest** — permissions + service declaration

---

## 14. Testing Checklist

### Unit Tests

| Component | Test |
|-----------|------|
| `CastFrameData` | Header encode + decode round-trip (magic, seq, timestamp, size) |
| `CastQualityProfile` | All 3 profiles have valid dimensions (width > height for landscape) |
| `CastCommandHandler` | `canHandle(CAST)` = true; `canHandle(MEDIA)` = false |
| `CastCommandHandler` | Each CAST_* action type routes to correct manager call |

### Integration Tests

| Flow | Test |
|------|------|
| Server + Client | MjpegTcpServer sends 3 frames; MjpegTcpClient receives all 3 in order |
| Frame drop | Client receives frame with seq=10 after seq=5; verifies gap=5 triggers drop |
| Quality change | `setQuality(HIGH)` propagates to VirtualDisplay recreation |

### Manual Testing (Device)

| Step | Expected |
|------|----------|
| Say "start casting" | Foreground service notification appears; VirtualDisplay created |
| Open Cockpit ScreenCast frame on second device | Receiver connects; live frames visible |
| Say "stop casting" | Notification dismissed; client receives connection close |
| Say "cast quality high" | Quality badge in overlay updates to HIGH |
| Kill casting app | Client detects disconnect within 2 seconds |

---

## 15. KMP Score Breakdown

| Source Set | Files | Rationale |
|-----------|------:|-----------|
| commonMain | 5 | Models + protocol interface — fully shared |
| androidMain | 9 | MediaProjection, NsdManager, and Compose Bitmap are Android-only |
| desktopMain | 3 | java.awt.Robot is Desktop-only |
| VoiceOSCore (androidMain) | 1 | Handler is Android-only |

KMP Score = commonMain feature areas / total feature areas = 5 / (5 + 9 + 3 + 1) ≈ **27%** functional KMP.

The theoretical maximum for a screen-casting module is ~40% (the capture API itself cannot be shared across platforms). The 27% score is appropriate.

---

## 16. Related Documentation

| Document | Path |
|----------|------|
| Cockpit SpatialVoice Chapter 97 | `Docs/MasterDocs/Cockpit/Developer-Manual-Chapter97-CockpitSpatialVoiceMultiWindow.md` |
| VOS Distribution Plan | `docs/plans/VoiceOSCore/VoiceOSCore-Plan-VOSCompactFormat-260216-V1.md` |
| Six-Module Unified Spec | `docs/plans/Modules-Spec-SixModuleImplementation-260219-V1.md` |
| Handler Architecture (Chapter 95) | `Docs/MasterDocs/NewAvanues-Developer-Manual/Developer-Manual-Chapter95-VOSDistributionAndHandlerDispatch.md` |
| Session Handover | `Docs/handover/handover-260219-0100.md` |
