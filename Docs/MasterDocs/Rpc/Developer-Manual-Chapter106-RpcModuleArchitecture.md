# Developer Manual Chapter 106: Rpc Module Architecture

**Location:** `Modules/Rpc/`
**Package:** `com.augmentalis.rpc`
**Version:** 1.0.0 (formerly UniversalRPC, renamed 2026-02-02)
**Author:** Manoj Jhawar
**Created:** 2026-02-22

---

## 1. Overview

The **Rpc module** is a cross-platform gRPC and Wire-based RPC framework that provides language-agnostic, network-resilient communication between VoiceOS components across multiple platforms (Android, Desktop, iOS). It replaces legacy AIDL interfaces with a modern, serializable protocol that supports both local (same-device) and remote (cross-device) communication.

### Key Characteristics

| Aspect | Detail |
|--------|--------|
| **Architecture** | gRPC services + Wire Protocol serialization |
| **Serialization** | Wire v5.1.0 (Square) with manual pre-generation (KotlinPoet compat) |
| **Platforms** | Android (Netty + OkHttp), Desktop (JVM), iOS (Phase 2 stubs) |
| **Transport** | Unix Domain Sockets (UDS), TCP, In-Memory, Platform-Native |
| **Proto Format** | Proto3 with gRPC service definitions |
| **File Count** | 185 Kotlin files, 10 .proto files, 8 gRPC services |
| **Status** | Production-ready for Android/Desktop, iOS foundation in place |

---

## 2. Module Architecture

### 2.1 Design Principles

1. **Cross-Platform Abstraction**: Single API for all transports and platforms
2. **Non-Blocking**: All I/O via Coroutines (Kotlin suspend functions)
3. **Reactive**: StateFlow-based connection state, reactive event streaming
4. **Fault Tolerant**: Automatic reconnection, exponential backoff, graceful degradation
5. **Wire Format Neutral**: Protocol Buffer serialization compatible with other platforms (C++, Go, Python, etc.)

### 2.2 Layered Architecture

```
┌─────────────────────────────────────────────────────┐
│ Service Layer (8 gRPC Services)                     │
│ - VoiceOSService, AvaService, CockpitService, etc. │
└──────────────────┬──────────────────────────────────┘
                   │
┌──────────────────▼──────────────────────────────────┐
│ Generated Service Code                              │
│ - Wire-generated Kotlin data classes                │
│ - gRPC service stubs (client-side)                  │
│ - Service interface definitions                     │
└──────────────────┬──────────────────────────────────┘
                   │
┌──────────────────▼──────────────────────────────────┐
│ Transport Abstraction Layer                         │
│ - Transport interface (connect/disconnect/send/recv)│
│ - Connection state machine (DISCONNECTED → CONNECTED)
│ - Event streaming (StateFlow + SharedFlow)          │
└──────────────────┬──────────────────────────────────┘
                   │
┌──────────────────▼──────────────────────────────────┐
│ Platform-Specific Transports                        │
│ Android: UnixDomainSocket + TCP                     │
│ Desktop: TCP + In-Memory                            │
│ iOS: TCP only (stubs)                               │
└─────────────────────────────────────────────────────┘
```

### 2.3 Core Abstractions

#### ServiceRegistry
Central service discovery and registration system:
- **Local services**: Registered IRpcService instances
- **Remote endpoints**: ServiceEndpoint (host:port, protocol, metadata)
- **Reactive discovery**: Flow-based service availability updates
- **Well-known names**: Constants for standard service names and default ports

```kotlin
// Well-known services
const val SERVICE_VOICEOS = "com.augmentalis.voiceos"           // Port 50051
const val SERVICE_AVA = "com.augmentalis.ava"                   // Port 50052
const val SERVICE_COCKPIT = "com.augmentalis.cockpit"           // Port 50053
const val SERVICE_NLU = "com.augmentalis.nlu"                   // Port 50054
const val SERVICE_WEBAVANUE = "com.augmentalis.webavanue"       // Port 50055
```

#### IRpcService Interface
Base interface for all service implementations:
```kotlin
interface IRpcService {
    val serviceName: String
    val version: String
    suspend fun isReady(): Boolean
    suspend fun shutdown()
}
```

#### Connection State Machine
| State | Meaning | Transitions |
|-------|---------|-------------|
| **DISCONNECTED** | No connection attempt | → CONNECTING (on connect) |
| **CONNECTING** | Establishing connection | → CONNECTED (success) or FAILED (error) |
| **CONNECTED** | Active, ready for I/O | → DISCONNECTING (on close) or RECONNECTING (on error) |
| **RECONNECTING** | Automatic recovery attempt | → CONNECTED (success) or FAILED (max retries) |
| **FAILED** | Permanent failure | → DISCONNECTED (manual reset) |
| **CLOSING** | Graceful shutdown | → DISCONNECTED (complete) |

---

## 3. The Eight gRPC Services

### 3.1 VoiceOSService (Port 50051)

**Purpose:** Command execution, screen scraping, voice recognition, app learning

| RPC | Type | Description |
|-----|------|-------------|
| `IsReady` | Unary | Service health/readiness probe |
| `ExecuteCommand` | Unary | Execute voice command by text |
| `ExecuteAccessibilityAction` | Unary | Trigger system accessibility actions (back, home, recent) |
| `ScrapeCurrentScreen` | Unary | Full accessibility tree of current screen |
| `StartVoiceRecognition` | Unary | Begin listening (language, recognizer type) |
| `StopVoiceRecognition` | Unary | End listening session |
| `LearnCurrentApp` | Unary | Generate commands from current app's UI |
| `GetLearnedApps` | Unary | Retrieve app learning history |
| `GetCommandsForApp` | Unary | Query commands for specific app |
| `RegisterDynamicCommand` | Unary | Register new voice command at runtime |
| `StreamEvents` | Server Stream | Event stream (command results, screen changes, accessibility events) |

**Key Messages:**
- `CommandRequest`: text, context map, request ID
- `CommandResponse`: success, message, JSON result
- `ScrapeScreenRequest/Response`: Screen hierarchy with AvidElements
- `VoiceOSEvent`: Oneof event type (command_result, status_change, screen_change, accessibility_event)

### 3.2 VoiceCursorService (Port not explicitly defined)

**Purpose:** Cursor position tracking, gesture actions, cursor-specific streaming

**Typical RPCs:**
- Get/Set cursor position
- Execute cursor actions (click, long-click, scroll)
- Stream cursor location updates

### 3.3 NLUService (Port 50054)

**Purpose:** Intent classification, entity extraction, batch processing

| RPC | Type | Description |
|-----|------|-------------|
| `ClassifyIntent` | Unary | Single text → intent + entities |
| `ClassifyIntentBatch` | Unary | Multiple texts → batch results |
| `ExtractEntities` | Unary | Entity extraction from text |

### 3.4 AvaServiceClient (Port 50052)

**Purpose:** AI assistant (AvaAI) chat, streaming responses, action execution, provider management

| RPC | Type | Description |
|-----|------|-------------|
| `Chat` | Unary | Single turn chat query |
| `StreamChat` | Server Stream | Streaming chat response (tokens streamed back) |
| `ExecuteAction` | Unary | Execute structured action (from AI output) |
| `ListProviders` | Unary | Available AI providers (OpenAI, Gemini, etc.) |
| `SetActiveProvider` | Unary | Switch between providers |

### 3.5 AvidCreatorService

**Purpose:** AVID (voice element identifier) generation, screen scraping, element queries

| RPC | Type | Description |
|-----|------|-------------|
| `QueryElement` | Unary | Find element by AVID, text, bounds, or resource ID |
| `ScrapeScreen` | Unary | Full screen tree with AVID for each element |
| `ExecuteAction` | Unary | Click, long-click, set_text, scroll on element |
| `GenerateAvid` | Unary | Create AVID from bounds/hint |
| `StreamScreenChanges` | Server Stream | Notify on UI hierarchy changes (debounced) |

### 3.6 PluginServiceClient (Port not explicitly defined)

**Purpose:** Plugin lifecycle, discovery, event bus

| RPC | Type | Description |
|-----|------|-------------|
| `RegisterPlugin` | Unary | Plugin init, capabilities declaration |
| `UnregisterPlugin` | Unary | Plugin teardown |
| `DiscoverPlugins` | Unary | List active plugins |
| `DispatchEvent` | Server Stream | Plugin event bus (broadcast to all listening plugins) |

### 3.7 CockpitService (Port 50053)

**Purpose:** Spatial UI management (multi-window, voice-to-3D, HUD coordination)

| RPC | Type | Description |
|-----|------|-------------|
| `RegisterWindow` | Unary | New spatial window |
| `UpdateWindowState` | Unary | Position, size, focus, visibility |
| `StreamWindowEvents` | Server Stream | Window lifecycle & interaction events |

### 3.8 WebAvanueService (Port 50055)

**Purpose:** Web browser integration, DOM scraping, JavaScript bridge

| RPC | Type | Description |
|-----|------|-------------|
| `ScrapeDom` | Unary | Extract interactive DOM elements |
| `ExecuteJavaScript` | Unary | Run JS and return result |
| `StreamDomChanges` | Server Stream | Notify on DOM mutations |

---

## 4. Transport Layer

### 4.1 Transport Types

| Type | Protocol | Use Case | Latency | Platform |
|------|----------|----------|---------|----------|
| **Unix Domain Socket (UDS)** | AF_UNIX | Local IPC (service-to-service) | <1ms | Android, Desktop |
| **TCP** | TCP/IP | Cross-device, WiFi/network | 5-100ms | All platforms |
| **In-Memory** | Direct call | Testing, same-process | <0.1ms | All platforms |
| **Platform-Native** | Platform API | iOS BLE, Android AIDL compat | Varies | iOS (future) |

### 4.2 Transport Configuration

```kotlin
data class TransportConfig(
    val connectionTimeoutMs: Long = 5000,        // Max time to connect
    val readTimeoutMs: Long = 10000,             // Max time per read
    val writeTimeoutMs: Long = 5000,             // Max time per write
    val retryDelayMs: Long = 100,                // Initial backoff
    val maxRetryAttempts: Int = 5,               // Max reconnection attempts
    val autoReconnect: Boolean = true,           // Auto-retry on failure
    val bufferSize: Int = 8,                     // KB, buffer size for streams
    val keepAliveIntervalMs: Long = 30000        // Keep-alive ping interval
)
```

### 4.3 Transport Addresses

```kotlin
sealed class TransportAddress {
    // Abstract or filesystem-based Unix socket
    data class UnixSocket(
        val path: String,
        val abstract: Boolean = true
    ) : TransportAddress()

    // TCP socket
    data class TcpSocket(
        val host: String,
        val port: Int
    ) : TransportAddress()

    // In-memory (testing)
    data class InMemory(val label: String = "memory") : TransportAddress()
}
```

### 4.4 Transport Lifecycle

```
Create Transport
       ↓
[DISCONNECTED] ─(connect)─→ [CONNECTING]
       ↑                         │
       │                         ├─(success)→ [CONNECTED]
       │                         │                  │
       └──(reconnect)──(max retries exceeded)← [RECONNECTING]
                                │
                         (error/timeout)→ [FAILED]
                                │
       (user closes)←──────────────
       [CLOSING] ─(complete)→ [DISCONNECTED]
```

### 4.5 Message Framing

All transports use **length-prefixed framing**:
```
[4-byte big-endian length] [message bytes]
4-byte length = message.size()
Max message = bufferSize * 1024 KB
Keep-alive = length: 0 (empty message)
```

### 4.6 Error Handling

| Error Type | Recovery | State |
|-----------|----------|-------|
| Connection timeout | Retry with backoff | RECONNECTING |
| Read/Write failure | Notify listeners, attempt reconnect | RECONNECTING |
| Max retries exceeded | Emit error, fail permanently | FAILED |
| Graceful close | Clean shutdown | DISCONNECTED |

---

## 5. Platform Implementations

### 5.1 Android (`src/androidMain`)

**Transport Libraries:**
- `grpc-okhttp`: OkHttp transport for gRPC (HTTP/2)
- `grpc-protobuf-lite`: Lite protobuf for Android
- `grpc-kotlin-stub`: Kotlin coroutine stubs

**Key Classes:**
- `UnixDomainSocketTransport`: LocalSocket (sub-millisecond local IPC)
- `TcpSocketTransport`: Socket (cross-device)
- `UnixDomainSocketServerTransport`: LocalServerSocket (server-side listener)
- `TcpSocketServerTransport`: ServerSocket (server-side listener)

**Characteristics:**
- Uses Android `LocalSocket` API for UDS (abstract namespace preferred)
- TCP fallback for network/cross-device scenarios
- Automatic reconnection with exponential backoff (100ms → 3.2s max)
- Thread-safe via Mutex locks on send/receive
- Keep-alive pings every 30s to detect stale connections

### 5.2 Desktop/JVM (`src/desktopMain`)

**Transport Libraries:**
- `grpc-netty-shaded`: Netty transport (bundled, no version conflicts)
- `grpc-protobuf`: Full protobuf support
- `grpc-kotlin-stub`: Kotlin coroutine stubs

**Key Classes:**
- `TcpSocketTransport`: Standard Java Socket
- `InMemoryTransport`: Direct call (testing)
- Netty-based gRPC server

**Characteristics:**
- Targets JVM 17+
- TCP primary, In-Memory for testing
- Can host gRPC servers (unlike Android)
- More memory/CPU available, can use full Protobuf

### 5.3 iOS (`src/iosMain`)

**Status:** Phase 2 (foundation in place, stubs/placeholders)

**Expected Implementation:**
- TCP via URLSession or Network framework
- Swift Protobuf bindings (third-party, not in-tree)
- Possible BLE companion for wearables (Vuzix Z100)

**Current:** Framework build configuration, no active service code

---

## 6. Service Registry

The **ServiceRegistry** is the central lookup system for all services in the system.

### 6.1 Registration Pattern

```kotlin
val registry = ServiceRegistry()

// Local service (process-owned)
val voiceOsService = VoiceOSServiceImpl(context)
registry.registerLocal(
    voiceOsService,
    ServiceEndpoint(
        serviceName = ServiceRegistry.SERVICE_VOICEOS,
        host = "voiceos",                        // Socket name (UDS)
        port = 50051,
        protocol = "uds"
    )
)

// Remote service (discovered)
registry.registerRemote(
    ServiceEndpoint(
        serviceName = ServiceRegistry.SERVICE_AVA,
        host = "192.168.1.100",
        port = 50052,
        protocol = "grpc"
    )
)
```

### 6.2 Discovery Pattern

```kotlin
// Reactive discovery
registry.services.collect { serviceMap ->
    serviceMap.forEach { (name, endpoint) ->
        println("Service available: $name at ${endpoint.address}")
    }
}

// Lookup by name
val endpoint = registry.getEndpoint(ServiceRegistry.SERVICE_NLU)
if (endpoint != null) {
    // Connect to NLU service at endpoint.host:endpoint.port
}

// Check availability
if (registry.isAvailable(ServiceRegistry.SERVICE_COCKPIT)) {
    // Safe to call CockpitService
}
```

### 6.3 Well-Known Services and Default Ports

| Service | Constant | Default Port | Protocol |
|---------|----------|--------------|----------|
| VoiceOS | `SERVICE_VOICEOS` | 50051 | gRPC + UDS |
| VoiceCursor | `SERVICE_VOICE_CURSOR` | — | gRPC |
| Voice Recognition | `SERVICE_VOICE_RECOGNITION` | — | gRPC |
| AVID Creator | `SERVICE_AVID_CREATOR` | — | gRPC |
| Exploration | `SERVICE_EXPLORATION` | — | gRPC |
| Ava (AI) | `SERVICE_AVA` | 50052 | gRPC |
| Cockpit (UI) | `SERVICE_COCKPIT` | 50053 | gRPC |
| NLU | `SERVICE_NLU` | 50054 | gRPC |
| WebAvanue | `SERVICE_WEBAVANUE` | 50055 | gRPC |

---

## 7. Wire Protocol and Code Generation

### 7.1 Wire v5.1.0 Overview

**Wire** (by Square) is a lightweight serialization library that compiles Protobuf definitions to idiomatic Kotlin:
- Zero runtime reflection (unlike protobuf-java)
- Immutable data classes with copy() support
- Smaller binary footprint
- KMP-compatible

### 7.2 Proto File Structure

All `.proto` files in `Common/proto/`:

| File | Service | Purpose |
|------|---------|---------|
| `voiceos.proto` | VoiceOSService | Command execution, screen scraping, event streaming |
| `avid.proto` | AvidCreatorService | Element queries, AVID generation |
| `nlu.proto` | NLUService | Intent classification, entity extraction |
| `ava.proto` | AvaServiceClient | AI chat, provider management |
| `cockpit.proto` | CockpitService | Spatial window management |
| `webavanue.proto` | WebAvanueService | DOM scraping, JavaScript bridge |
| `voicecursor.proto` | VoiceCursorService | Cursor tracking, actions |
| `plugin.proto` | PluginServiceClient | Plugin lifecycle, events |
| `common.proto` | (shared) | Common types (Bounds, metadata) |

### 7.3 Code Generation (Manual)

**Why Manual?** Wire's KotlinPoet plugin conflicts with Kotlin 2.1.0 KSP2, so `.proto` → Kotlin is done offline:

```bash
# Generate Kotlin from proto (run once per proto change)
wire-cli compile-jvm \
  --proto_path=Common/proto \
  --kotlin_out=src/commonMain/kotlin \
  Common/proto/*.proto
```

**Generated Output:**
- Data classes (e.g., `CommandRequest`, `CommandResponse`)
- Service stubs (client-side RPC interfaces)
- Serializers (Wire Marshaller classes)
- Live in `src/commonMain/kotlin/com/augmentalis/rpc/` (pre-generated)

### 7.4 When to Regenerate

1. **Add new RPC**: Edit `.proto` → regenerate → import in code
2. **Change message fields**: Update `.proto` → regenerate (backward compatible if using proto3)
3. **Change service methods**: Edit `.proto` → regenerate

---

## 8. Build Configuration

### 8.1 Gradle Setup

**Root module config:**
```kotlin
group = "com.augmentalis.rpc"
version = "1.0.0"

kotlin {
    androidTarget { jvmTarget = "17" }
    jvm("desktop") { jvmTarget = "17" }
    // iOS configuration conditional (Phase 2)
}

android {
    namespace = "com.augmentalis.rpc"
    compileSdk = 34
    minSdk = 24
}
```

### 8.2 Dependencies by Platform

**commonMain:**
- `wire-runtime`: Core serialization
- `wire-grpc-client`: gRPC client stubs
- `kotlinx-coroutines-core`: Async/await
- `kotlinx-serialization-json`: JSON support (optional)

**androidMain:**
- `grpc-okhttp`: HTTP/2 transport
- `grpc-kotlin-stub`: Coroutine stubs
- `grpc-protobuf-lite`: Lightweight protobuf
- `kotlinx-coroutines-android`: Android-specific coroutines

**desktopMain:**
- `grpc-netty-shaded`: Netty bundled
- `grpc-kotlin-stub`: Coroutine stubs
- `grpc-protobuf`: Full protobuf support
- `kotlinx-coroutines-swing`: Swing-safe coroutines

### 8.3 Disabling Wire Plugin

```kotlin
// Wire plugin disabled in build.gradle.kts (commented out)
// id("com.squareup.wire") version "5.1.0"
// Reason: KotlinPoet compatibility with Kotlin 2.1.0 + KSP2
// Re-enable when issue resolved
```

---

## 9. Directory Structure

```
Modules/Rpc/
├── build.gradle.kts                          # KMP, targets, dependencies
├── settings.gradle.kts                       # (if subproject)
│
├── Common/
│   └── proto/                                # Protocol buffer definitions
│       ├── voiceos.proto
│       ├── avid.proto
│       ├── nlu.proto
│       ├── ava.proto
│       ├── cockpit.proto
│       ├── webavanue.proto
│       ├── voicecursor.proto
│       ├── plugin.proto
│       └── common.proto
│
├── src/
│   ├── commonMain/
│   │   └── kotlin/com/augmentalis/rpc/
│   │       ├── ServiceRegistry.kt            # Service discovery
│   │       ├── IRpcService.kt                # Base service interface
│   │       ├── (generated Kotlin files)      # Wire-compiled proto → .kt
│   │       │   ├── VoiceOSService*.kt
│   │       │   ├── AvidCreatorService*.kt
│   │       │   └── ...
│   │       └── transport/
│   │           ├── Transport.kt              # Interface definitions
│   │           ├── TransportConfig.kt
│   │           ├── TransportAddress.kt
│   │           └── TransportState.kt
│   │
│   ├── androidMain/
│   │   └── kotlin/com/augmentalis/rpc/
│   │       ├── transport/
│   │       │   ├── AndroidTransport.kt       # UDS + TCP client/server
│   │       │   ├── BaseAndroidTransport.kt
│   │       │   └── (specific implementations)
│   │       └── (Android service implementations)
│   │
│   ├── desktopMain/
│   │   └── kotlin/com/augmentalis/rpc/
│   │       ├── transport/
│   │       │   ├── TcpSocketTransport.kt
│   │       │   └── InMemoryTransport.kt
│   │       └── (Desktop service implementations)
│   │
│   └── iosMain/
│       └── kotlin/com/augmentalis/rpc/
│           ├── transport/
│           │   └── IosTransport.kt           # Stubs/Phase 2
│           └── (iOS service stubs)
│
└── README.md                                 # Module documentation
```

---

## 10. Integration Points

### 10.1 How Other Modules Consume Rpc

**VoiceOSCore module:**
- Imports: `com.augmentalis.rpc:Rpc:1.0.0` (dependency in build.gradle.kts)
- Registers: VoiceOSService via ServiceRegistry
- Uses: Transport layer for local IPC to accessibility service

**Cockpit module:**
- Imports: Rpc module
- Consumes: CockpitService for spatial window events
- Exposes: CockpitService implementation

**NoteAvanue, WebAvanue, etc.:**
- Client: Call VoiceOSService RPCs for command execution
- Service: Register their own gRPC services (VoiceCursorService, WebAvanueService)

### 10.2 Dependency Graph Position

```
VoiceOSCore ─→ Rpc ←─ Foundation (transport abstractions)
                 ├─→ AvanueUI (UI themes for responses)
                 ├→ SpeechRecognition (listen events)
                 └→ HTTPAvanue (optional, REST bridge)

Cockpit ─→ Rpc
CursorAvanue ─→ Rpc
WebAvanue ─→ Rpc
NoteAvanue ─→ Rpc
```

### 10.3 Typical Service Startup Sequence

```
1. App/Activity onCreate()
2. Initialize Rpc module (load transport, config)
3. Instantiate ServiceRegistry (global singleton or DI)
4. Create local service(s) → registerLocal(service, endpoint)
5. Discover remote services → registerRemote(endpoint)
6. Create Transport → connect()
7. Services ready to handle RPC calls
```

### 10.4 Typical Client RPC Call

```kotlin
// Get service endpoint from registry
val endpoint = registry.getEndpoint(ServiceRegistry.SERVICE_NLU)
    ?: throw Exception("NLU service not available")

// Create gRPC client (auto-generated from proto)
val nluClient = NLUServiceClient(endpoint)

// Call RPC (suspend function, coroutine-based)
val result = nluClient.classifyIntent(
    ClassifyIntentRequest(
        request_id = UUID.randomUUID().toString(),
        text = "turn on bluetooth"
    )
)

// Use result
if (result.success) {
    println("Intent: ${result.intent}, Confidence: ${result.confidence}")
}
```

---

## 11. Key Design Decisions

### 11.1 Why Wire Over Protobuf-Java?

| Aspect | Wire | Protobuf-Java |
|--------|------|---------------|
| Generated code | Idiomatic Kotlin (data classes) | Java with builders |
| KMP support | Native | Limited |
| Binary size | Smaller | Larger |
| Runtime reflection | None | Uses reflection |
| Learning curve | Easier (Kotlin) | Steeper |

### 11.2 Why gRPC + Wire Hybrid?

**gRPC** handles service method generation and HTTP/2 streaming. **Wire** handles efficient message serialization. Together they provide:
- Type-safe cross-platform communication
- Streaming RPC support (needed for events)
- Code generation in Kotlin first
- Network protocol independence (HTTP/2, custom, etc.)

### 11.3 Why UDS on Android?

Unix Domain Sockets achieve sub-millisecond latency for local IPC (same device), matching or exceeding AIDL in performance while being cross-platform serializable (proto3). TCP fallback covers network scenarios.

### 11.4 Why Manual Proto Generation?

Wire's KotlinPoet plugin has compatibility issues with Kotlin 2.1 + KSP2. Manual offline generation is stable; re-enabling plugin is a future optimization.

---

## 12. Common Usage Patterns

### 12.1 Register a Service

```kotlin
// Implement IRpcService
class MyCustomService(context: Context) : IRpcService {
    override val serviceName = "com.example.myservice"
    override val version = "1.0.0"

    override suspend fun isReady() = true
    override suspend fun shutdown() { }
}

// Register
val service = MyCustomService(context)
registry.registerLocal(
    service,
    ServiceEndpoint(
        serviceName = service.serviceName,
        host = "myservice",
        port = 50100,
        protocol = "uds"
    )
)
```

### 12.2 Listen for Service Availability

```kotlin
lifecycleScope.launch {
    registry.services.collect { services ->
        val voiceOsAvailable = services.containsKey(ServiceRegistry.SERVICE_VOICEOS)
        updateUI(voiceOsAvailable)
    }
}
```

### 12.3 Handle Connection State

```kotlin
val transport = UnixDomainSocketTransport(
    address = TransportAddress.UnixSocket("voiceos"),
    config = TransportConfig(maxRetryAttempts = 5)
)

lifecycleScope.launch {
    transport.state.collect { state ->
        when (state) {
            TransportState.CONNECTED -> enableControls()
            TransportState.DISCONNECTED -> disableControls()
            TransportState.RECONNECTING -> showRetryIndicator()
            TransportState.FAILED -> showErrorMessage("Service unavailable")
            else -> {}
        }
    }
}

transport.connect()
```

### 12.4 Stream Events

```kotlin
lifecycleScope.launch {
    val voiceOsClient = VoiceOSServiceClient(endpoint)

    voiceOsClient.streamEvents(
        StreamEventsRequest(
            request_id = UUID.randomUUID().toString(),
            event_types = listOf("COMMAND_RESULT", "SCREEN_CHANGE")
        )
    ).collect { event ->
        when {
            event.hasCommandResult() -> handleCommandResult(event.commandResult)
            event.hasScreenChange() -> handleScreenChange(event.screenChange)
            else -> {}
        }
    }
}
```

---

## 13. Testing and Debugging

### 13.1 In-Memory Transport for Tests

```kotlin
@Test
fun testServiceIntegration() = runTest {
    // No network, no sockets — direct call
    val transport = InMemoryTransport()

    val service = MockVoiceOSService()
    registry.registerLocal(service, endpoint)

    // Test RPC call directly
    val result = service.executeCommand(request)
    assertThat(result.success).isTrue()
}
```

### 13.2 Inspecting Service Registry

```kotlin
val allServices = registry.getAllServices()
allServices.forEach { endpoint ->
    Log.d("ServiceRegistry",
        "${endpoint.serviceName} at ${endpoint.address} (${endpoint.protocol})")
}
```

### 13.3 Connection Debugging

```kotlin
transport.events.collect { event ->
    when (event) {
        is TransportEvent.StateChanged -> {
            Log.d("Transport", "State: ${event.oldState} → ${event.newState}")
        }
        is TransportEvent.Error -> {
            Log.e("Transport", "Error: ${event.error.message}")
        }
        is TransportEvent.DataSent -> {
            Log.v("Transport", "Sent ${event.bytes} bytes")
        }
        is TransportEvent.ReconnectAttempt -> {
            Log.i("Transport", "Reconnect attempt ${event.attempt}/${event.maxAttempts}")
        }
        else -> {}
    }
}
```

---

## 14. Future Enhancements

### Phase 2 iOS Support
- Swift Protobuf integration
- Network framework for TCP
- BLE companion protocol (Vuzix Z100, XReal glasses)

### Proto Plugin Re-enablement
- Fix KotlinPoet compatibility
- Automate code generation in build
- Reduce manual maintenance

### Cross-Platform Interop
- C++ / Go service bindings (gRPC native support)
- REST API bridge (gRPC-JSON transcoding)
- gRPC-Web for browser clients

### Performance Optimization
- Connection pooling for high-traffic services
- Message compression (gzip)
- Circuit breaker pattern for failing services

---

## 15. Related Documentation

| Document | Purpose |
|----------|---------|
| `Modules/Rpc/README.md` | Module-specific quickstart |
| `Docs/MasterDocs/VoiceOSCore/` | VoiceOS service specifics |
| `Docs/MasterDocs/Foundation/` | Platform abstractions (transport foundation) |
| `Common/proto/*.proto` | Service definitions (canonical source) |
| Square Wire docs | Serialization format reference |
| gRPC docs | Service framework reference |

---

**Summary:** The Rpc module provides a production-grade, multi-platform RPC framework bridging accessibility services, AI providers, UI managers, and plugin ecosystems. Its layered architecture (services → transport → platform) ensures maintainability and platform independence while achieving sub-millisecond latency on local connections and graceful degradation on unreliable networks.

**Revision:** 1.0
**Date:** 2026-02-22
