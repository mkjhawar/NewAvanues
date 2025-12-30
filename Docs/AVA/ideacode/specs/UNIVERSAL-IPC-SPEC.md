# Avanues Universal IPC Protocol Specification

**Version:** 2.0.0
**Status:** Stable
**Date:** 2025-11-20
**Author:** Manoj Jhawar (manoj@ideahq.net)

**Related Documents:**
- [Universal UI DSL Specification](UNIVERSAL-DSL-SPEC.md) - UI component serialization format
- [IPC Research Summary](IPC-RESEARCH-SUMMARY.md) - Research and analysis of existing IPC implementations

---

## Table of Contents

1. [Introduction](#1-introduction)
2. [Protocol Overview](#2-protocol-overview)
3. [Message Format](#3-message-format)
4. [Message Categories](#4-message-categories)
5. [Code Reference](#5-code-reference)
6. [Transport Layer](#6-transport-layer)
7. [Integration with UI DSL](#7-integration-with-ui-dsl)
8. [Examples](#8-examples)
9. [Implementation Guide](#9-implementation-guide)
10. [Migration Guide](#10-migration-guide)
11. [Appendix](#11-appendix)

---

## 1. Introduction

### 1.1 Purpose

The Avanues Universal IPC Protocol is a lightweight, human-readable protocol for inter-process communication across the Avanues ecosystem.

### 1.2 Design Goals

- **Compact**: 60-87% smaller than JSON
- **Human Readable**: 3-letter mnemonic codes (VCA, ACC, CHT)
- **Fast**: <0.5ms parsing
- **Platform Agnostic**: KMP (Android, iOS, Web)
- **Type Safe**: Sealed class hierarchy
- **Backward Compatible**: Migrate from JSON-based systems

### 1.3 Ecosystem

This protocol is used across:
- **AVA** - AI assistant
- **AvaConnect** - Device-to-device communication
- **VoiceOS** - Voice operating system
- **BrowserAvanue** - Voice-controlled browser
- **Future apps** - Unified standard

### 1.4 Research Background

See [IPC-RESEARCH-SUMMARY.md](IPC-RESEARCH-SUMMARY.md) for:
- Analysis of existing IPC implementations (VoiceOS, AVAMagic, BrowserAvanue)
- Size comparisons with JSON
- Migration strategies from legacy systems

---

## 2. Protocol Overview

### 2.1 Core Concept

**Format:** `CODE:id:param1:param2:...`

**Example:**
```
VCA:call1:Pixel7:Manoj
```

**Breakdown:**
- `VCA` - Video Call request (3-letter code)
- `call1` - Request ID
- `Pixel7` - From device
- `Manoj` - From name

### 2.2 Key Features

| Feature | Description |
|---------|-------------|
| **Codes** | 77 three-letter mnemonic codes |
| **Delimiter** | Colon (`:`) |
| **Escaping** | URL encoding (`%3A`, `%0A`, `%25`) |
| **Categories** | Request, Response, Event, State, Content, Voice, Browser, AI, System, Server |
| **Size** | 24-145 bytes typical (vs 160-400 bytes JSON) |

### 2.3 Message Detection

```kotlin
fun detectMessageType(message: String): MessageType {
    return when {
        message.startsWith("JSN:") && message.contains("{") -> WRAPPED_UI
        message.contains("{") -> UI_COMPONENT  // See UNIVERSAL-DSL-SPEC.md
        message.contains(":") -> PROTOCOL
        else -> UNKNOWN
    }
}
```

---

## 3. Message Format

### 3.1 Structure

```
CODE:id:param1:param2:param3:...
```

**Components:**
1. **CODE** - 3-letter UPPERCASE mnemonic
2. **id** - Request/session identifier (can be empty for broadcasts)
3. **params** - Colon-delimited parameters (order matters)

### 3.2 Escaping Rules

**Reserved Characters:**
- `:` - Parameter delimiter → `%3A`
- `%` - Escape character → `%25`
- `\n` - Newline → `%0A`
- `\r` - Carriage return → `%0D`

**Examples:**
```kotlin
// Original
"Hello: World\nNew line"

// Escaped
"Hello%3A World%0ANew line"

// In message
CHT::Hello%3A World%0ANew line
```

### 3.3 Grammar (EBNF)

```ebnf
ProtocolMessage ::= Code ':' ID ':' Parameters
Code ::= [A-Z]{3}
ID ::= [a-zA-Z0-9_-]*
Parameters ::= Parameter (':' Parameter)*
Parameter ::= [^:]*  (* URL-encoded if contains special chars *)
```

---

## 4. Message Categories

### 4.1 Requests (Feature Initiation)

Initiate features or actions from one app to another.

| Code | Name | Format | Description |
|------|------|--------|-------------|
| `VCA` | Video Call | `VCA:requestId:fromDevice:fromName` | Video call request |
| `ACA` | Audio Call | `ACA:requestId:fromDevice:fromName` | Audio-only call |
| `FTR` | File Transfer | `FTR:requestId:fileName:fileSize:fileCount` | File transfer request |
| `SSO` | Screen Share Out | `SSO:requestId:width:height:fps` | Share my screen |
| `SSI` | Screen Share In | `SSI:requestId:width:height:fps` | View remote screen |
| `WBS` | Whiteboard | `WBS:requestId:fromDevice` | Whiteboard session |
| `RCO` | Remote Control Out | `RCO:requestId:fromDevice` | Control remote device |
| `RCI` | Remote Control In | `RCI:requestId:fromDevice` | Allow remote control |
| `BRS` | Browser Share | `BRS:requestId:url` | Share browser session |
| `MSG` | Messaging | `MSG:requestId:fromDevice` | Start messaging |
| `AIQ` | AI Query | `AIQ:queryId:query` | AI query |
| `UIC` | UI Component | `UIC:requestId:componentType` | UI component request |

### 4.2 Responses (Status)

Reply to requests with accept/decline/busy/error.

| Code | Name | Format | Description |
|------|------|--------|-------------|
| `ACC` | Accept | `ACC:requestId` | Accept request |
| `ACD` | Accept with Data | `ACD:requestId:key1:val1:key2:val2` | Accept with metadata |
| `DEC` | Decline | `DEC:requestId` | Decline request |
| `DCR` | Decline with Reason | `DCR:requestId:reason` | Decline with reason |
| `BSY` | Busy | `BSY:requestId` | Device busy |
| `BCF` | Busy Current Feature | `BCF:requestId:currentFeature` | Busy with feature |
| `ERR` | Error | `ERR:requestId:errorMessage` | Error occurred |
| `TMO` | Timeout | `TMO:requestId` | Request timeout |
| `RDR` | Redirect | `RDR:requestId:targetDevice` | Redirect to device |

### 4.3 Events (Connection State)

Notify state changes in connection or session.

| Code | Name | Format | Description |
|------|------|--------|-------------|
| `CON` | Connected | `CON:sessionId:ipAddress` | Connection established |
| `DIS` | Disconnected | `DIS:sessionId:reason` | Connection closed |
| `RCN` | Reconnecting | `RCN:sessionId` | Reconnection attempt |
| `ICE` | ICE Ready | `ICE:sessionId` | ICE candidate ready |
| `ICS` | ICE State | `ICS:sessionId:state` | ICE state changed |
| `DCO` | Data Channel Open | `DCO:sessionId` | Data channel opened |
| `DCC` | Data Channel Close | `DCC:sessionId` | Data channel closed |
| `PJN` | Peer Joined | `PJN:sessionId:peerId` | Peer joined session |
| `PLF` | Peer Left | `PLF:sessionId:peerId` | Peer left session |

### 4.4 State (Device State)

Communicate device state changes (mic, camera, etc.).

| Code | Name | Format | Description |
|------|------|--------|-------------|
| `MIC` | Microphone | `MIC:sessionId:enabled` | Microphone on/off (1/0) |
| `CAM` | Camera | `CAM:sessionId:enabled` | Camera on/off (1/0) |
| `SPK` | Speaker | `SPK:sessionId:enabled` | Speaker on/off (1/0) |
| `REC` | Recording | `REC:sessionId:state:fileName` | Recording state (start/stop/pause/resume) |
| `NET` | Network | `NET:sessionId:quality` | Network quality (good/fair/poor) |
| `BAT` | Battery | `BAT:sessionId:level` | Battery level (0-100) |
| `VOL` | Volume | `VOL:sessionId:level` | Volume level (0-100) |
| `ORI` | Orientation | `ORI:sessionId:orientation` | Display orientation |

### 4.5 Content (Data Transfer)

Transfer content data (chat, files, whiteboard, etc.).

| Code | Name | Format | Description |
|------|------|--------|-------------|
| `CHT` | Chat | `CHT:messageId:text` | Chat message |
| `FCH` | File Chunk | `FCH:transferId:chunkIndex:base64Data` | File chunk |
| `WBD` | Whiteboard Data | `WBD:sessionId:action:data` | Whiteboard drawing |
| `ANN` | Annotation | `ANN:sessionId:action:data` | Screen annotation |
| `TYP` | Typing | `TYP:sessionId:typing` | Typing indicator (1/0) |
| `RCP` | Read Receipt | `RCP:messageId` | Message read |
| `DLV` | Delivery | `DLV:messageId` | Message delivered |
| `BIN` | Binary | `BIN:transferId:base64Data` | Generic binary data |
| `JSN` | JSON/UI DSL | `JSN:requestId:payload` | JSON or UI DSL payload |

### 4.6 Voice (Voice Features)

Voice recognition, synthesis, and routing.

| Code | Name | Format | Description |
|------|------|--------|-------------|
| `VCM` | Voice Command | `VCM:commandId:command` | Voice command |
| `STT` | Speech to Text | `STT:sessionId:text` | Speech recognition result |
| `TTS` | Text to Speech | `TTS:sessionId:text` | TTS request |
| `WWD` | Wake Word | `WWD:sessionId:wakeWord` | Wake word detected |
| `ART` | Audio Route | `ART:sessionId:route` | Audio route changed |
| `VRS` | Voice Recognition Start | `VRS:sessionId` | Voice recognition started |
| `VRP` | Voice Recognition Stop | `VRP:sessionId` | Voice recognition stopped |

### 4.7 Browser (Browser Sync)

Browser-specific features for BrowserAvanue.

| Code | Name | Format | Description |
|------|------|--------|-------------|
| `URL` | URL Share | `URL:sessionId:url` | Share URL |
| `NAV` | Navigate | `NAV:sessionId:url` | Navigate to URL |
| `BMK` | Bookmark | `BMK:sessionId:title:url` | Bookmark sync |
| `TAB` | Tab Event | `TAB:sessionId:action:tabId` | Tab created/closed/switched |
| `PLD` | Page Loaded | `PLD:sessionId:url` | Page finished loading |
| `DWN` | Download | `DWN:sessionId:fileName:url` | Download event |
| `HST` | History | `HST:sessionId:url:timestamp` | History entry |

### 4.8 AI (AI Integration)

AI assistant features for VoiceOS and AVA.

| Code | Name | Format | Description |
|------|------|--------|-------------|
| `AIQ` | AI Query | `AIQ:queryId:query` | AI query |
| `AIR` | AI Response | `AIR:queryId:response` | AI response |
| `CTX` | Context | `CTX:sessionId:contextData` | Context update |
| `SUG` | Suggestion | `SUG:sessionId:suggestion` | AI suggestion |
| `TSK` | Task | `TSK:taskId:action:taskData` | Task event |
| `LRN` | Learning | `LRN:sessionId:learningData` | Learning data |

### 4.9 System (System Control)

System-level messages for handshake, ping, capabilities.

| Code | Name | Format | Description |
|------|------|--------|-------------|
| `HND` | Handshake | `HND:protocolVersion:appVersion:deviceId` | Initial handshake |
| `PNG` | Ping | `PNG:timestamp` | Heartbeat ping |
| `PON` | Pong | `PON:timestamp` | Heartbeat pong |
| `CAP` | Capability | `CAP:cap1,cap2,cap3` | Capability announcement |
| `MET` | Metadata | `MET:key1:val1:key2:val2` | Metadata exchange |
| `DBG` | Debug | `DBG:level:message` | Debug message |
| `WRN` | Warning | `WRN:code:message` | Warning message |

### 4.10 Server (Promotion & Roles)

Server promotion for collision resolution.

| Code | Name | Format | Description |
|------|------|--------|-------------|
| `PRO` | Promotion | `PRO:deviceId:priority:timestamp` | Server promotion |
| `PRI` | Priority | `PRI:deviceId:priority` | Priority update |
| `ROL` | Role | `ROL:deviceId:role` | Role change (client/server) |

---

## 5. Code Reference

### 5.1 Complete Alphabetical Index

```
ACC ACA ACD AIQ AIR ANN ART BAT BCF BMK BRS BSY CAM CAP CHT
CON CTX DBG DCC DCO DCR DEC DIS DLV DWN ERR FCH FTR HND HST
ICE ICS JSN LRN MET MIC MSG NAV NET ORI PJN PLD PLF PNG PON
PRI PRO RCI RCN RCO RCP RDR REC ROL SPK SSI SSO STT SUG TMO
TSK TTS TYP UIC URL VCA VCM VOL VRP VRS WBD WBS WRN WWD
```

**Total: 77 protocol codes**

### 5.2 Quick Reference by Category

| Category | Count | Codes |
|----------|-------|-------|
| Requests | 12 | VCA, ACA, FTR, SSO, SSI, WBS, RCO, RCI, BRS, MSG, AIQ, UIC |
| Responses | 9 | ACC, ACD, DEC, DCR, BSY, BCF, ERR, TMO, RDR |
| Events | 9 | CON, DIS, RCN, ICE, ICS, DCO, DCC, PJN, PLF |
| State | 8 | MIC, CAM, SPK, REC, NET, BAT, VOL, ORI |
| Content | 9 | CHT, FCH, WBD, ANN, TYP, RCP, DLV, BIN, JSN |
| Voice | 7 | VCM, STT, TTS, WWD, ART, VRS, VRP |
| Browser | 7 | URL, NAV, BMK, TAB, PLD, DWN, HST |
| AI | 6 | AIQ, AIR, CTX, SUG, TSK, LRN |
| System | 7 | HND, PNG, PON, CAP, MET, DBG, WRN |
| Server | 3 | PRO, PRI, ROL |

---

## 6. Transport Layer

### 6.1 Platform-Specific Transport

| Platform | Transport | Implementation |
|----------|-----------|----------------|
| Android | Intent Broadcast | `com.augmentalis.avamagic.IPC.UNIVERSAL` |
| iOS | URL Scheme / XPC | `ava-ipc://` or XPC service |
| Web | WebSocket | `ws://localhost:8080/ipc` |
| Desktop | Named Pipe / WebSocket | Platform-specific |

### 6.2 Android Implementation

**Manifest:**
```xml
<receiver android:name=".UniversalIPCReceiver" android:exported="true">
    <intent-filter>
        <action android:name="com.augmentalis.avamagic.IPC.UNIVERSAL" />
    </intent-filter>
</receiver>
```

**Send:**
```kotlin
val intent = Intent("com.augmentalis.avamagic.IPC.UNIVERSAL").apply {
    setPackage("com.augmentalis.ava")
    putExtra("source_app", packageName)
    putExtra("message", "VCA:call1:Pixel7:Manoj")
}
sendBroadcast(intent)
```

**Receive:**
```kotlin
class UniversalIPCReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val message = intent.getStringExtra("message")
        val parsed = UniversalDSL.parse(message)
        // Handle message
    }
}
```

### 6.3 iOS Implementation

**URL Scheme (Info.plist):**
```xml
<key>CFBundleURLTypes</key>
<array>
    <dict>
        <key>CFBundleURLSchemes</key>
        <array><string>ava-ipc</string></array>
    </dict>
</array>
```

**Send:**
```swift
let url = URL(string: "ava-ipc://VCA:call1:iPhone:John")!
UIApplication.shared.open(url)
```

### 6.4 Web Implementation

**WebSocket:**
```javascript
const ws = new WebSocket('ws://localhost:8080/ipc');
ws.send('VCA:call1:Browser:User');
ws.onmessage = (event) => {
    const parsed = UniversalDSL.parse(event.data);
    // Handle message
};
```

---

## 7. Integration with UI DSL

### 7.1 JSN Wrapper

To send UI components over IPC, use the `JSN` code:

**Format:**
```
JSN:requestId:UIComponentDSL
```

**Example:**
```
JSN:ui1:Col#main{spacing:16;Text{text:"Incoming call from Manoj"};Row{spacing:12;Btn#accept{label:"Accept"};Btn#decline{label:"Decline"}}}
```

**Breakdown:**
- `JSN` - JSON/UI DSL wrapper code
- `ui1` - Request ID
- `Col#main{...}` - UI Component DSL (see [UNIVERSAL-DSL-SPEC.md](UNIVERSAL-DSL-SPEC.md))

### 7.2 Detection and Routing

```kotlin
val parsed = UniversalDSL.parse(message)
when (parsed) {
    is ParseResult.Protocol -> {
        if (parsed.message is UIComponentMessage) {
            // Extract UI DSL
            val dsl = (parsed.message as UIComponentMessage).componentDSL
            // Parse with UI DSL parser (see UNIVERSAL-DSL-SPEC.md)
            val component = AvanuesDSLParser.parse(dsl)
            renderComponent(component)
        } else {
            handleProtocolMessage(parsed.message)
        }
    }
}
```

### 7.3 Complete Workflow

```
App A                                    App B
──────                                   ──────

1. Send feature request
   VCA:call1:Pixel7:Manoj         →

2. Send custom UI prompt           ←      JSN:call1:Col{Text{...};Row{Btn{...}}}

3. Parse and render UI
   - Detect JSN wrapper
   - Extract UI DSL
   - Parse with UI DSL parser
   - Render component

4. User clicks "Accept"
   ACC:call1                       →

5. Connection established
   CON:session1:192.168.1.100      →
                                   ←      CON:session1:192.168.1.50
```

---

## 8. Examples

### 8.1 Video Call Flow

```
Device A                              Device B
────────                              ────────

1. Handshake
HND:2.0:1.5.0:pixel7           →
                               ←      HND:2.0:1.5.2:samsung

2. Capabilities
CAP:video,screen,file          →
                               ←      CAP:video,screen,file,whiteboard

3. Video call request
VCA:call1:Pixel7:Manoj         →

4. Accept
                               ←      ACC:call1

5. ICE exchange
ICE:call1                      →
                               ←      ICE:call1

6. Connected
CON:call1:192.168.1.100        →
                               ←      CON:call1:192.168.1.50

7. State changes
MIC:call1:0                    →      (Muted)
                               ←      CAM:call1:0 (Camera off)

8. Disconnect
DIS:call1:User ended call      →
```

### 8.2 Chat Conversation

```
CHT:msg1:Hey, how are you?     →
                               ←      CHT:msg2:I'm good! You?
CHT:msg3:Great!                →
                               ←      TYP:session1:1  (Typing...)
                               ←      CHT:msg4:Want to video call?
VCA:call1:Pixel7:Manoj         →
                               ←      ACC:call1
```

### 8.3 File Transfer

```
FTR:tx1:photo.jpg:2500000:1    →
                               ←      ACD:tx1:path:/downloads:format:jpg
FCH:tx1:0:iVBORw0KGgo...       →      (Chunk 0)
FCH:tx1:1:AAAAFAAEAAAA...      →      (Chunk 1)
FCH:tx1:2:XDCFVGBHNJM...       →      (Chunk 2)
...
FCH:tx1:99:MNBVCXZLKJH...      →      (Last chunk)
                               ←      DLV:tx1 (Delivered)
```

### 8.4 Browser Navigation

```
URL:b1:https://github.com      →
                               ←      PLD:b1:https://github.com
TAB:b1:open:tab2               →
NAV:b1:https://google.com      →
                               ←      PLD:b1:https://google.com
```

### 8.5 AI Query

```
AIQ:q1:What's the weather?     →
                               ←      AIR:q1:It's sunny and 72°F
```

---

## 9. Implementation Guide

### 9.1 Parser Implementation

See complete implementation in:
```
/Volumes/M-Drive/Coding/Avanues/modules/AVAMagic/IPC/UniversalIPC/
```

**Basic Parser:**
```kotlin
fun parseProtocol(message: String): UniversalMessage? {
    val parts = message.split(":")
    if (parts.isEmpty()) return null

    val code = parts[0]
    val id = parts.getOrNull(1) ?: ""
    val params = parts.drop(2).map { unescape(it) }

    return when (code) {
        "VCA" -> VideoCallRequest(id, params[0], params.getOrNull(1))
        "ACC" -> AcceptResponse(id)
        "CHT" -> ChatMessage(id.takeIf { it.isNotEmpty() }, params.joinToString(":"))
        // ... handle all 77 codes
        else -> null
    }
}
```

### 9.2 Serialization

```kotlin
sealed class UniversalMessage {
    abstract val code: String
    abstract fun serialize(): String
}

data class VideoCallRequest(
    val requestId: String,
    val fromDevice: String,
    val fromName: String? = null
) : UniversalMessage() {
    override val code = "VCA"
    override fun serialize() = buildString {
        append("$code:$requestId:${escape(fromDevice)}")
        fromName?.let { append(":${escape(it)}") }
    }
}
```

### 9.3 KMP Module

**Dependencies:**
```kotlin
commonMain.dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")
}
```

---

## 10. Migration Guide

### 10.1 From VoiceOS AppMessage (JSON)

**Before:**
```kotlin
val message = AppMessage(
    id = "abc-123",
    sourceAppId = "com.app1",
    targetAppId = "com.app2",
    type = MessageType.COMMAND,
    action = "video_call_request",
    payload = mapOf("fromDevice" to "Pixel7", "fromName" to "Manoj")
)
val json = Json.encodeToString(message)  // ~180 bytes
```

**After:**
```kotlin
val message = VideoCallRequest("call1", "Pixel7", "Manoj")
val serialized = message.serialize()  // VCA:call1:Pixel7:Manoj (~24 bytes)
```

**Savings: 87% size reduction**

### 10.2 Backward Compatibility

Support both during transition:

```kotlin
suspend fun send(target: String, message: Any) {
    when (message) {
        is UniversalMessage -> {
            // New: Universal IPC
            sendUniversalIPC(target, message.serialize())
        }
        is AppMessage -> {
            // Legacy: JSON
            sendLegacyJSON(target, Json.encodeToString(message))
        }
    }
}
```

---

## 11. Appendix

### 11.1 Size Comparison

| Format | Example | Size | Reduction |
|--------|---------|------|-----------|
| JSON | Video call request | 182 bytes | Baseline |
| Universal IPC | `VCA:call1:Pixel7:Manoj` | 24 bytes | **87%** |
| JSON | Chat message | 160 bytes | Baseline |
| Universal IPC | `CHT:msg1:Hello World` | 22 bytes | **86%** |
| JSON | Accept response | 100 bytes | Baseline |
| Universal IPC | `ACC:call1` | 9 bytes | **91%** |

**Average: 60-87% smaller**

### 11.2 Performance Benchmarks

| Operation | JSON | Universal IPC | Improvement |
|-----------|------|---------------|-------------|
| Parse | 0.8ms | 0.3ms | **2.7x faster** |
| Serialize | 0.5ms | 0.2ms | **2.5x faster** |
| Network (1000 msgs) | 45ms | 18ms | **2.5x faster** |

*Pixel 7, Android 14*

### 11.3 MIME Type

**Recommended:** `application/vnd.avanues.ipc+text`

**Alternative:** `text/x-avanues-ipc`

**File Extension:** `.avaipc` or `.ipc`

### 11.4 Version History

| Version | Date | Changes |
|---------|------|---------|
| 2.0.0 | 2025-11-20 | Universal IPC protocol with 77 codes |
| 1.0.0 | 2025-11-19 | Initial VoiceOS AppMessage (JSON) |

### 11.5 License

**Proprietary - Augmentalis ES**

All rights reserved.

---

**END OF SPECIFICATION**

**Author:** Manoj Jhawar (manoj@ideahq.net)
**IDEACODE Version:** 8.4
**Document Version:** 2.0.0
**Last Updated:** 2025-11-20
