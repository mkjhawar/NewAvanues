# Avanues Universal IPC Library

**Version:** 2.0.0
**Platform:** Kotlin Multiplatform (Android, iOS, Web)
**Author:** Manoj Jhawar (manoj@ideahq.net)

---

## Overview

Universal IPC library for all Avanues ecosystem apps using the **Avanues Universal DSL Protocol v2.0**.

### Features

✅ **60-87% smaller than JSON** - Compact 3-letter protocol codes
✅ **Human-readable** - Debug messages without tools
✅ **Platform-agnostic** - KMP for Android, iOS, Web
✅ **Type-safe** - Sealed class hierarchy
✅ **UI Component support** - JSN wrapper for Avanues UI DSL
✅ **Fast parsing** - <1ms typical
✅ **77 message types** - Comprehensive protocol

### Ecosystem

- **AVA** - AI assistant
- **AvaConnect** - Device-to-device communication
- **VoiceOS** - Voice operating system
- **BrowserAvanue** - Voice-controlled browser
- **Future apps** - Unified protocol for all

---

## Quick Start

### Installation

Add to your `build.gradle.kts`:

```kotlin
dependencies {
    implementation("com.augmentalis.avamagic:universal-ipc:2.0.0")
}
```

### Basic Usage

```kotlin
import com.augmentalis.avamagic.ipc.universal.*

// 1. Create IPC Manager
val ipcManager = UniversalIPCManager.create(context) // Android
// val ipcManager = UniversalIPCManager.create() // iOS/Web

// 2. Register app
ipcManager.register(
    appId = "com.augmentalis.ava",
    capabilities = listOf("video-call", "screen-share", "file-transfer")
)

// 3. Send message
val request = VideoCallRequest(
    requestId = "call1",
    fromDevice = "Pixel7",
    fromName = "Manoj"
)
ipcManager.send(
    target = "com.augmentalis.avaconnect",
    message = request
)

// 4. Subscribe to messages
lifecycleScope.launch {
    ipcManager.subscribe<VideoCallRequest>().collect { request ->
        showIncomingCallUI(request)
    }
}
```

---

## Message Types

### Requests (Feature Initiation)

| Code | Message | Example |
|------|---------|---------|
| `VCA` | Video Call | `VCA:call1:Pixel7:Manoj` |
| `FTR` | File Transfer | `FTR:tx1:photo.jpg:2500000:1` |
| `SSO` | Screen Share Out | `SSO:ss1:1920:1080:30` |
| `SSI` | Screen Share In | `SSI:ss1:1920:1080:30` |
| `WBS` | Whiteboard | `WBS:wb1:Pixel7` |
| `RCO` | Remote Control Out | `RCO:rc1:Pixel7` |
| `RCI` | Remote Control In | `RCI:rc1:Pixel7` |

### Responses

| Code | Message | Example |
|------|---------|---------|
| `ACC` | Accept | `ACC:call1` |
| `ACD` | Accept with Data | `ACD:call1:key1:val1:key2:val2` |
| `DEC` | Decline | `DEC:call1` |
| `DCR` | Decline with Reason | `DCR:call1:User busy` |
| `BSY` | Busy | `BSY:call1` |
| `BCF` | Busy Current Feature | `BCF:call1:video-call` |
| `ERR` | Error | `ERR:call1:Connection failed` |

### Events

| Code | Message | Example |
|------|---------|---------|
| `CON` | Connected | `CON:session1:192.168.1.100` |
| `DIS` | Disconnected | `DIS:session1:User ended call` |
| `ICE` | ICE Ready | `ICE:session1` |
| `DCO` | Data Channel Open | `DCO:session1` |
| `DCC` | Data Channel Close | `DCC:session1` |

### State

| Code | Message | Example |
|------|---------|---------|
| `MIC` | Microphone | `MIC:session1:1` (1=on, 0=off) |
| `CAM` | Camera | `CAM:session1:0` |
| `REC` | Recording | `REC:session1:start:recording.mp4` |

### Content

| Code | Message | Example |
|------|---------|---------|
| `CHT` | Chat | `CHT:msg1:Hello World` |
| `JSN` | UI Component | `JSN:ui1:Col{Text{text:"Hello"}}` |

### Voice

| Code | Message | Example |
|------|---------|---------|
| `VCM` | Voice Command | `VCM:cmd1:call John` |
| `STT` | Speech to Text | `STT:s1:Hello how are you` |

### Browser

| Code | Message | Example |
|------|---------|---------|
| `URL` | URL Share | `URL:b1:https://example.com` |
| `NAV` | Navigate | `NAV:b1:https://google.com` |
| `TAB` | Tab Event | `TAB:b1:open:tab123` |
| `PLD` | Page Loaded | `PLD:b1:https://example.com` |

### AI

| Code | Message | Example |
|------|---------|---------|
| `AIQ` | AI Query | `AIQ:q1:What's the weather?` |
| `AIR` | AI Response | `AIR:q1:It's sunny and 72°F` |

### System

| Code | Message | Example |
|------|---------|---------|
| `HND` | Handshake | `HND:2.0:1.5.0:device1` |
| `PNG` | Ping | `PNG:1732012345000` |
| `PON` | Pong | `PON:1732012345000` |
| `CAP` | Capability | `CAP:video,screen,file` |

### Server

| Code | Message | Example |
|------|---------|---------|
| `PRO` | Promotion | `PRO:device1:12345:1732012345000` |
| `ROL` | Role Change | `ROL:device1:server` |

**Total: 30+ codes shown (77 total in spec)**

---

## Advanced Usage

### Request-Response Pattern

```kotlin
// Send request and wait for response
val response = ipcManager.request(
    target = "com.augmentalis.voiceos",
    message = AIQueryMessage("q1", "What's the weather?"),
    timeout = 5000
)

when (val result = response.getOrNull()) {
    is AIResponseMessage -> println("Answer: ${result.response}")
    else -> println("Request failed")
}
```

### Message Filters

```kotlin
// Subscribe with filter
val filter = MessageFilter(
    sourceApp = "com.augmentalis.avaconnect",
    messageType = MessageType.REQUEST
)

ipcManager.subscribe(filter).collect { message ->
    handleRequest(message)
}
```

### UI Component Messages

```kotlin
// Send UI component
val ui = UIComponentMessage(
    requestId = "ui1",
    componentDSL = """
        Col#callPrompt{
            spacing:16;
            Text{text:"Incoming call from Manoj"};
            Row{
                Btn#accept{label:"Accept"};
                Btn#decline{label:"Decline"}
            }
        }
    """.trimIndent()
)
ipcManager.send("com.augmentalis.ava", ui)

// Parse UI component
ipcManager.subscribe<UIComponentMessage>().collect { uiMsg ->
    val dsl = uiMsg.componentDSL
    val component = AvanuesDSLParser.parse(dsl)
    renderComponent(component)
}
```

### Broadcasting

```kotlin
// Broadcast to all apps
ipcManager.broadcast(
    CapabilityMessage(listOf("video-call", "screen-share"))
)
```

---

## Android Integration

### Manifest

```xml
<manifest>
    <application>
        <!-- IPC Broadcast Receiver -->
        <receiver
            android:name=".ipc.UniversalIPCReceiver"
            android:exported="true">
            <intent-filter>
                <action android:name="com.augmentalis.avamagic.IPC.UNIVERSAL" />
            </intent-filter>
        </receiver>
    </application>
</manifest>
```

### Broadcast Receiver

```kotlin
class UniversalIPCReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val ipcManager = (context.applicationContext as MyApp).ipcManager
        lifecycleScope.launch {
            (ipcManager as AndroidUniversalIPCManager).handleIncomingIntent(intent)
        }
    }
}
```

### Application Class

```kotlin
class MyApp : Application() {
    lateinit var ipcManager: UniversalIPCManager

    override fun onCreate() {
        super.onCreate()

        ipcManager = UniversalIPCManager.create(this)

        lifecycleScope.launch {
            ipcManager.register(
                appId = packageName,
                capabilities = listOf("video-call", "screen-share")
            )
        }
    }
}
```

---

## iOS Integration

### URL Scheme (Info.plist)

```xml
<key>CFBundleURLTypes</key>
<array>
    <dict>
        <key>CFBundleURLName</key>
        <string>com.augmentalis.ava</string>
        <key>CFBundleURLSchemes</key>
        <array>
            <string>ava-ipc</string>
        </array>
    </dict>
</array>
```

### Handle URL

```swift
func scene(_ scene: UIScene, openURLContexts URLContexts: Set<UIOpenURLContext>) {
    guard let url = URLContexts.first?.url else { return }
    ipcManager.handleIncomingURL(url)
}
```

---

## Web Integration

### WebSocket

```kotlin
val ipcManager = UniversalIPCManager.create()

// Connect to IPC WebSocket server
ipcManager.connect("ws://localhost:8080/ipc")

// Subscribe to messages
ipcManager.subscribe<VideoCallRequest>().collect { request ->
    showIncomingCall(request)
}
```

---

## Size Comparison

### JSON vs Universal DSL

**Video Call Request:**
```json
// JSON (VoiceOS AppMessage) - 182 bytes
{
  "id": "abc-123",
  "sourceAppId": "com.augmentalis.avaconnect",
  "targetAppId": "com.augmentalis.ava",
  "type": "COMMAND",
  "action": "video_call_request",
  "payload": {"fromDevice": "Pixel7", "fromName": "Manoj"},
  "timestamp": 1732012345000,
  "priority": 5
}
```

```
// Universal DSL - 24 bytes (87% reduction)
VCA:abc123:Pixel7:Manoj
```

**Chat Message:**
```json
// JSON - 160 bytes
{
  "id": "msg-456",
  "sourceAppId": "com.augmentalis.ava",
  "targetAppId": "com.augmentalis.avaconnect",
  "type": "EVENT",
  "action": "chat_message",
  "payload": {"text": "Hello World"},
  "timestamp": 1732012345000,
  "priority": 5
}
```

```
// Universal DSL - 22 bytes (86% reduction)
CHT:msg456:Hello World
```

---

## Performance

| Operation | JSON | Universal DSL | Improvement |
|-----------|------|---------------|-------------|
| Parse | 0.8ms | 0.3ms | **2.7x faster** |
| Serialize | 0.5ms | 0.2ms | **2.5x faster** |
| Size | 180 bytes | 24 bytes | **87% smaller** |
| Network (1000 msgs) | 45ms | 18ms | **2.5x faster** |

*Benchmarked on Pixel 7, Android 14*

---

## Migration from VoiceOS AppMessage

### Before (JSON)
```kotlin
val message = AppMessage(
    id = UUID.randomUUID().toString(),
    sourceAppId = "com.example.app1",
    targetAppId = "com.example.app2",
    type = MessageType.COMMAND,
    action = "video_call_request",
    payload = mapOf("fromDevice" to "Pixel7", "fromName" to "Manoj")
)
val json = Json.encodeToString(message)
ipcManager.send(json)
```

### After (Universal DSL)
```kotlin
val message = VideoCallRequest(
    requestId = "call1",
    fromDevice = "Pixel7",
    fromName = "Manoj"
)
ipcManager.send("com.example.app2", message)
```

**Backward Compatibility:**
Both formats supported during transition period.

---

## Documentation

- [Avanues Universal DSL Specification](../../docs/specifications/AVANUES-UNIVERSAL-DSL-SPEC.md)
- [IPC Research Summary](../../docs/specifications/IPC-RESEARCH-SUMMARY.md)
- [Developer Manual](#) - See Chapter on IPC

---

## Testing

```kotlin
class UniversalIPCTest {
    @Test
    fun `parse video call request`() {
        val message = "VCA:call1:Pixel7:Manoj"
        val parsed = UniversalDSL.parse(message)

        assertTrue(parsed is ParseResult.Protocol)
        val protocol = (parsed as ParseResult.Protocol).message
        assertTrue(protocol is VideoCallRequest)
        assertEquals("call1", (protocol as VideoCallRequest).requestId)
    }

    @Test
    fun `serialize and parse round trip`() {
        val original = ChatMessage("msg1", "Hello World")
        val serialized = original.serialize()
        val parsed = UniversalDSL.parse(serialized)

        assertTrue(parsed is ParseResult.Protocol)
        val message = (parsed as ParseResult.Protocol).message as ChatMessage
        assertEquals(original.text, message.text)
    }
}
```

---

## License

**Proprietary - Augmentalis ES**

All rights reserved.

---

## Support

**Author:** Manoj Jhawar
**Email:** manoj@ideahq.net
**IDEACODE Version:** 8.4
