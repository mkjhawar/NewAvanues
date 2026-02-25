# Chapter 104: HTTPAvanue v2.0 — Zero-Dependency I/O + Full Enhancement Suite

**Module**: HTTPAvanue (`Modules/HTTPAvanue/`)
**Version**: 2.0
**Date**: 2026-02-22
**Author**: Manoj Jhawar
**Previous**: Chapter 101 (HTTPAvanue KMP HTTP Server Library)

---

## Overview

HTTPAvanue v2.0 eliminates the Okio dependency (the last external I/O library) and adds 20+ new features across middleware, routing, WebSocket, testing, VoiceOS integration, network discovery, and binary protocols. The module now has **zero external I/O dependencies** — only `kotlinx-coroutines`, `kotlinx-serialization`, `kotlinx-datetime`, and the internal `Logging` module.

### Architecture: Dual-Publication Model

HTTPAvanue uses a **Branch B + Branch C** architecture for independent vs. ecosystem use:

```
httpavanue-core (Branch C — Third-Party / Standalone)
├── server/        HTTP/1.1 + HTTP/2 server
├── client/        HTTP client with pooling + retries
├── http/          Request/Response/Status/Method
├── http2/         RFC 7540 + HPACK RFC 7541
├── websocket/     RFC 6455 client + server
├── middleware/    12 middlewares
├── routing/       Router + TypedRoutes DSL
├── io/            AvanueIO (Okio replacement)
├── sse/           Server-Sent Events
├── metrics/       ServerMetrics + MetricsMiddleware
├── testing/       InProcessEngine
├── mdns/          mDNS service discovery
├── platform/      Socket, Time, Resources expect/actual
├── auth/          AuthenticationManager
├── hpack/         HPACK codec
└── core/          Result, serialization

httpavanue (Branch B — Avanues Internal, extends core)
├── voice/         VoiceRoute, VoiceRouteExporter
├── avid/          AvidResponse
└── avacode/       mountForm, mountWorkflow (future Phase 10)
```

**Rule**: `voice/` and `avid/` packages MUST NOT import from any external module. They only use HTTPAvanue types. This ensures third-party users never pull in Avanues-specific dependencies.

---

## 1. AvanueIO — Okio Replacement

### Package: `com.augmentalis.httpavanue.io`

AvanueIO replaces Okio's 3 core abstractions with owned implementations:

| Okio Type | AvanueIO Replacement | Role |
|-----------|---------------------|------|
| `okio.BufferedSource` | `AvanueSource` (interface) | Buffered byte reading |
| `okio.BufferedSink` | `AvanueSink` (interface) | Buffered byte writing |
| `okio.Buffer` | `AvanueBuffer` (class) | Growable byte buffer (read + write) |
| `okio.ByteString` | `AvanueByteString` (extensions) | Base64 + SHA-1 utilities |

### AvanueSource

```kotlin
interface AvanueSource {
    val buffer: AvanueBuffer          // Internal prefetch buffer
    fun readByte(): Byte
    fun readShort(): Short            // Big-endian
    fun readInt(): Int                // Big-endian
    fun readLong(): Long              // Big-endian
    fun readByteArray(byteCount: Long): ByteArray
    fun readUtf8(byteCount: Long): String
    fun request(byteCount: Long): Boolean  // Non-blocking peek fill
    fun require(byteCount: Long)           // Throws AvanueEofException
    fun skip(byteCount: Long)
    fun peek(): AvanueSource               // Non-consuming read
    fun close()
}
```

### AvanueSink

```kotlin
interface AvanueSink {
    fun writeByte(value: Int): AvanueSink
    fun writeShort(value: Int): AvanueSink
    fun writeInt(value: Int): AvanueSink
    fun write(bytes: ByteArray): AvanueSink
    fun write(bytes: ByteArray, offset: Int, count: Int): AvanueSink
    fun writeUtf8(string: String): AvanueSink
    fun flush()
    fun close()
}
```

### AvanueBuffer

Growing `ByteArray` storage (starts at 256, doubles on overflow). Supports both read and write operations. Key methods:
- `snapshot(): ByteArray` — non-destructive copy (critical for HTTP/2 preface peek)
- `readUtf8Line(): String?` — reads one line (CR LF or LF terminated)
- `toByteArray(): ByteArray` — same as snapshot, compatibility alias
- `clear()` — reset positions

### Platform Implementations

| Platform | Source | Sink |
|----------|--------|------|
| Android (JVM) | `AvanueSourceJvm` wraps `InputStream` | `AvanueSinkJvm` wraps `OutputStream` |
| Desktop (JVM) | Same as Android | Same as Android |
| iOS (Native) | `AvanueSourceIos` wraps POSIX `recv()` | `AvanueSinkIos` wraps POSIX `send()` |

All implementations use 8 KB chunk sizes for I/O operations.

### Socket Contract Change

```kotlin
// Before (Okio)
expect class Socket {
    fun source(): BufferedSource
    fun sink(): BufferedSink
}

// After (AvanueIO)
expect class Socket {
    fun source(): AvanueSource
    fun sink(): AvanueSink
}
```

### AvanueByteString Extensions

```kotlin
fun ByteArray.toBase64(): String          // kotlin.io.encoding.Base64
fun String.fromBase64(): ByteArray
fun ByteArray.sha1Digest(): ByteArray     // Delegates to expect fun sha1()
```

---

## 2. New Middlewares (7 added, 12 total)

### HSTS Middleware

```kotlin
server.use(hstsMiddleware(HstsConfig(
    maxAge = 31_536_000,     // 1 year
    includeSubdomains = true,
    preload = false,
)))
```

### Forwarded Headers Middleware

```kotlin
server.use(forwardedHeadersMiddleware(ForwardedHeadersConfig(
    trustedProxies = setOf("10.0.0.1", "10.0.0.2"),
)))
// Reads: X-Forwarded-For, X-Forwarded-Proto, X-Forwarded-Host, X-Real-IP
// Writes: request.context["remote_address"], ["scheme"], ["host"]
```

### Auto HEAD Middleware

```kotlin
server.use(autoHeadMiddleware())
// HEAD requests automatically run GET handler, strip body, preserve Content-Length
```

### Content Negotiation Middleware

```kotlin
server.use(contentNegotiationMiddleware(ContentNegotiationConfig(
    supportedTypes = listOf("application/json", "text/plain"),
    defaultType = "application/json",
)))
// Returns 406 Not Acceptable when client rejects all supported types
```

### Multipart Parser

```kotlin
// Direct parsing (recommended)
val parts = request.multipartParts()  // List<MultipartPart>?

// Or via middleware (stores marker in context)
server.use(multipartMiddleware())
// Then in handler:
val parts = request.parsedMultipartParts()

// MultipartPart fields:
data class MultipartPart(
    val name: String,
    val filename: String?,      // null for text fields
    val contentType: String?,
    val data: ByteArray,
) {
    fun asText(): String        // UTF-8 decode
}
```

### Cookie Middleware

Full HTTP cookie support with all attributes (exceeds NanoHTTPD's limited CookieHandler):

```kotlin
// Read cookies from request
val session = request.cookie("session")
val all = request.cookies()  // Map<String, String>

// Set cookie on response
val response = HttpResponse.ok("logged in")
    .withCookie(Cookie(
        name = "session", value = "abc123",
        maxAge = 3600,
        path = "/",
        secure = true,
        httpOnly = true,
        sameSite = SameSite.Strict,
    ))

// Expire (delete) cookie
val loggedOut = response.withoutCookie("session")
```

### Date Header Middleware

```kotlin
server.use(dateHeaderMiddleware())
// Auto-adds: "Date: Sat, 22 Feb 2026 08:30:00 GMT" (RFC 7231 format)
```

---

## 3. Typed Routes DSL

### Package: `com.augmentalis.httpavanue.routing`

Extension functions on `RouterImpl` that auto-serialize/deserialize JSON:

```kotlin
@Serializable data class CreateUserReq(val name: String, val email: String)
@Serializable data class UserResp(val id: Int, val name: String)

router.postTyped<CreateUserReq, UserResp>("/api/users") { request, body ->
    UserResp(id = 1, name = body.name)
}

router.getTyped<UserResp>("/api/users/:id") { request ->
    UserResp(id = request.pathParam("id")?.toInt() ?: 0, name = "Alice")
}
```

Available: `getTyped`, `postTyped`, `putTyped`, `deleteTyped`, `patchTyped`

---

## 4. ETag + Range Middlewares

### ETag

```kotlin
server.use(etagMiddleware())
// Computes weak ETag via FNV-1a hash (pure Kotlin, no crypto)
// Returns 304 Not Modified when If-None-Match matches
```

### Range Requests (RFC 7233)

```kotlin
server.use(rangeMiddleware())
// Parses Range: bytes=N-M header
// Returns 206 Partial Content with Content-Range
// Returns 416 Range Not Satisfiable for invalid ranges
// Adds Accept-Ranges: bytes to all responses
// Validates If-Range (ETag check)
```

---

## 5. Typed WebSocket

```kotlin
@Serializable data class ChatMessage(val user: String, val text: String)
@Serializable data class ChatResponse(val status: String, val echo: String)

server.websocket("/chat") { socket ->
    val ws = WebSocket(socket, isServer = true)
    val typed = ws.typed<ChatMessage, ChatResponse>()
    ws.start(scope)
    typed.messages.collect { msg ->
        typed.send(ChatResponse("ok", msg.text))
    }
}
```

---

## 6. In-Process Test Engine

Test middleware + routes without network:

```kotlin
val router = Router().apply {
    get("/health") { HttpResponse.ok("ok") }
    post("/echo") { req -> HttpResponse.ok(req.bodyAsText() ?: "") }
}

val engine = InProcessEngine(router, middlewarePipeline)
val response = engine.get("/health")
assertEquals(200, response.status)
```

---

## 7. Voice Routes + AVID Responses (Branch B — Avanues Only)

### Voice Routes

Annotate endpoints with voice phrases for VoiceOS discovery:

```kotlin
router.getVoiced("/api/status",
    VoiceRouteConfig(
        phrase = "show status",
        aliases = listOf("check status"),
        category = "API",
    ),
) { HttpResponse.ok("running") }

// Export to VOS compact format
val vos = VoiceRouteExporter.toVosString()
// Output: "API|show status|HTTP_GET|GET /api/status|en-US"
```

### AVID Responses

Wrap JSON with AVID metadata for VoiceOS overlay badges:

```kotlin
HttpResponse.avidJson(
    body = """{"name":"Save"}""",
    avidType = AvidType.BTN,
    avidLabel = "Save",
)
// Output: {"data":{"name":"Save"},"_avid":{"type":"BTN","label":"Save","id":"BTN:save"}}
```

---

## 8. mDNS Service Discovery

### Package: `com.augmentalis.httpavanue.mdns`

Advertise HTTP servers on the local network via multicast DNS (224.0.0.251:5353):

```kotlin
val server = httpServer { port = 8080; ... }
val advertiser = server.advertise("My Server", mapOf("v" to "2.0"))

// In a coroutine scope:
launch { advertiser.start(MdnsService(name = "My Server", port = 8080)) }

// Later:
advertiser.stop()
```

DNS wire format (RFC 1035): PTR → SRV → TXT → A records, fully encoded in pure Kotlin.

---

## 9. Binary Protocol

High-performance frame-based protocol for WebSocket upgrade:

```
Wire format: [4-byte magic "AVNE"] [2-byte type] [4-byte length] [payload]
```

```kotlin
// Encode
val frame = BinaryProtocol.encode(BinaryProtocol.TYPE_CAST, jpegBytes)

// Decode
val frame = BinaryProtocol.decode(source)  // BinaryFrame(type, payload)

// Session (dispatch loop)
val session = BinaryProtocolSession(socket) { frame ->
    when (frame.type) {
        BinaryProtocol.TYPE_CAST -> handleCastFrame(frame.payload)
        BinaryProtocol.TYPE_CMD -> handleCommand(frame.payload)
    }
}
session.run()  // Runs until socket closes
```

Pre-defined types: `TYPE_CAST(0x01)`, `TYPE_VOCAB(0x02)`, `TYPE_CMD(0x03)`, `TYPE_ACK(0x04)`, `TYPE_IMU(0x05)`, `TYPE_TTS(0x06)`

---

## 10. New HTTP Status Codes (v2.0)

Added to `HttpStatus` enum:
- `PARTIAL_CONTENT(206)` — Range requests
- `NOT_ACCEPTABLE(406)` — Content negotiation
- `RANGE_NOT_SATISFIABLE(416)` — Invalid range

---

## Test Suite

48 tests across 5 suites, all passing on Desktop/JVM:

| Suite | Tests | Coverage |
|-------|-------|----------|
| AvanueBufferTest | 15 | Read/write, endianness, growth, overflow, line parsing |
| MiddlewareTest | 15 | HSTS, Auto HEAD, Date, ETag, Range, Cookies, Forwarded Headers, Multipart |
| BinaryProtocolTest | 7 | Encode/decode, wire format, multi-frame, EOF, invalid magic |
| DnsMessageTest | 7 | Name encoding, record types (PTR/SRV/TXT/A), full message |
| VoiceRouteTest | 4 | VOS format, multi-route, custom locale |

---

## File Inventory

### New Files (34)

| # | Path | Feature |
|---|------|---------|
| 1-4 | `commonMain/io/Avanue{Source,Sink,Buffer,ByteString}.kt` | AvanueIO interfaces |
| 5-6 | `androidMain/io/Avanue{Source,Sink}Jvm.kt` | JVM I/O |
| 7-8 | `desktopMain/io/Avanue{Source,Sink}Jvm.kt` | JVM I/O (identical) |
| 9-10 | `iosMain/io/Avanue{Source,Sink}Ios.kt` | iOS POSIX I/O |
| 11-17 | `commonMain/middleware/{Hsts,Forwarded,AutoHead,ContentNeg,Multipart,Cookie,DateHeader}` | 7 middlewares |
| 18 | `commonMain/routing/TypedRoutes.kt` | Typed Routes DSL |
| 19-20 | `commonMain/middleware/{ETag,Range}Middleware.kt` | Caching middlewares |
| 21 | `commonMain/websocket/TypedWebSocket.kt` | Typed WebSocket |
| 22 | `commonMain/testing/InProcessEngine.kt` | Test engine |
| 23 | `commonMain/voice/VoiceRoute.kt` | Voice route annotations |
| 24 | `commonMain/avid/AvidResponse.kt` | AVID responses |
| 25-28 | `commonMain/mdns/{DnsMessage,MdnsRecord,MdnsService,MdnsAdvertiser}.kt` | mDNS discovery |
| 29-31 | `{android,desktop,ios}Main/mdns/MdnsAdvertiser.*.kt` | Platform advertisers |
| 32 | `commonMain/websocket/BinaryProtocol.kt` | Binary protocol |
| 33-37 | `commonTest/{AvanueBuffer,Middleware,BinaryProtocol,DnsMessage,VoiceRoute}Test.kt` | 48 tests |

### Modified Files (18)

Socket contract (4), all parsers/codecs (12), build.gradle.kts (1), HttpStatus.kt (1)

---

## Related Documents

- Chapter 101: HTTPAvanue v1.0 KMP HTTP Server Library
- NanoHTTPD Comparison: `docs/analysis/HTTPAvanue/HTTPAvanue-Analysis-NanoHTTPDFeatureComparison-260222-V1.md`
- v2.0 Plan: `docs/plans/HTTPAvanue/HTTPAvanue-Plan-V2Implementation-260222-V1.md`
- AVACode Recipe Plan: `docs/plans/AVACode/AVACode-Plan-RecipeSystemPending-260222-V1.md`
- Audit Fixes (v1.1): `Docs/fixes/HTTPAvanue/HTTPAvanue-Fix-AuditBugs-260220-V1.md`

---

## JS/Browser Target (260223)

Added `js(IR) { browser(); nodejs() }` target as part of NetAvanue JS dependency chain.

### jsMain Actuals (6 files)

| Expect | JS Implementation | Notes |
|--------|-------------------|-------|
| `currentTimeMillis()` | `kotlin.js.Date.now().toLong()` | Trivial |
| `readResource()` | Returns `null` | Browser loads resources via fetch/bundler |
| `Socket` | Throws `UnsupportedOperationException` | Raw TCP unavailable in browsers |
| `SocketServer` | Throws `UnsupportedOperationException` | Browsers can't be servers |
| `gzipCompress/Decompress` | Throws `UnsupportedOperationException` | Server-side middleware; browsers handle Content-Encoding transparently |
| `sha1()` | Pure-Kotlin SHA-1 implementation | SubtleCrypto is async-only; synchronous expect requires pure implementation |
| `MdnsAdvertiser` | Throws `UnsupportedOperationException` | Multicast UDP unavailable in browsers |

### Why UnsupportedOperationException (Not Stubs)

HTTPAvanue is fundamentally an HTTP **server** library. Browsers are clients by design — they cannot open TCP server sockets, send raw UDP multicast, or run gzip middleware. These throws represent genuine platform limitations, not incomplete implementations.
