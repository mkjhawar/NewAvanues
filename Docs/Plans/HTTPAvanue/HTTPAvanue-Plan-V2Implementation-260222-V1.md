# HTTPAvanue v2.0 — Implementation Plan

**Module**: HTTPAvanue
**Type**: Plan
**Date**: 2026-02-22
**Version**: V1
**Branch**: VoiceOS-1M-SpeechEngine
**Flags**: `.tot .cot .swarm .yolo .implement`
**Author**: Manoj Jhawar

---

## Overview

- **Platforms**: Android, Desktop/JVM, iOS (KMP)
- **Swarm Recommended**: Yes (15+ tasks, 3 platforms)
- **Estimated**: 42 tasks, ~14 hours
- **KMP Score**: 78% → targeting 82% (more commonMain code)
- **Architecture**: Branch B (internal Avanues) + Branch C (third-party core)

## Architecture Decision

### Dual-Publication Model

```
httpavanue-core (Branch C — Third-Party)
├── server/       HTTP/1.1 + HTTP/2 server
├── client/       HTTP client with pooling + retries
├── http/         Request/Response/Status/Method
├── http2/        RFC 7540 + HPACK RFC 7541
├── websocket/    RFC 6455 client + server
├── middleware/   12 middlewares (CORS, rate limit, auth, etc.)
├── routing/      Router + TypedRoutes DSL
├── io/           AvanueIO (Okio replacement)
├── sse/          Server-Sent Events
├── metrics/      ServerMetrics + MetricsMiddleware
├── testing/      InProcessEngine
├── mdns/         mDNS service discovery
├── platform/     Socket, Time, Resources expect/actual
├── auth/         AuthenticationManager
├── hpack/        HPACK codec
└── core/         Result, serialization

httpavanue (Branch B — Avanues Internal, extends core)
├── voice/        VoiceRoute, VoiceRouteExporter (uses only HTTPAvanue types)
├── avid/         AvidResponse (uses only HTTPAvanue types)
└── avacode/      mountForm, mountWorkflow (imports from AVACode module)
```

**Rule**: `voice/` and `avid/` packages MUST NOT import from any external module. They only use HTTPAvanue types. The `avacode/` package is the only one with a cross-module dependency (on AVACode).

---

## CoT: Dependency Analysis

```
AvanueIO interfaces ──→ AvanueIO platform impls ──→ Socket contract change
    │                                                      │
    └── ALL consumers depend on I/O types ─────────────────┘
                    │
    ┌───────────────┼───────────────────────┐
    │               │                       │
    ▼               ▼                       ▼
  Parsers      HTTP/2 codec          WebSocket codec
  (HttpParser   (Http2FrameCodec     (WebSocketParser
   ResponseParser) Http2FlowControl    WebSocketClient
                   Http2Connection)    WebSocketClientHandshake)
                    │
                    ▼
              build.gradle.kts
              (remove okio dep)
              ═══════════════
              VERIFICATION GATE
              ═══════════════
                    │
    ┌───────────────┼────────────────────┐
    │               │                    │
    ▼               ▼                    ▼
  Middleware     Routing            StaticFile
  cluster        cluster            cluster
  (independent)  (independent)      (independent)
    │               │                    │
    └───────┬───────┘────────────────────┘
            │
    ┌───────┼────────────────┐
    │       │                │
    ▼       ▼                ▼
  WebSocket Testing        VoiceOS
  cluster   cluster        cluster
    │       │              (voice/ avid/)
    │       │                │
    └───┬───┘────────────────┘
        │
    ┌───┼────────┐
    │   │        │
    ▼   ▼        ▼
  mDNS Binary  AVACode
  cluster Proto  bridge
```

## ToT: Approach Branches

| Branch | Description | Verdict |
|--------|-------------|---------|
| A: Bottom-up AvanueIO first | I/O → consumers → features | **Selected** — correct dependency order |
| B: Features first, Okio later | New middlewares → then swap I/O | Rejected — new code would use Okio then need rewrite |
| C: Parallel everything | Swarm agents do I/O + features simultaneously | Rejected — type conflicts in shared files |

---

## Execution Plan (Code-Proximity Ordered)

### Phase 1: AvanueIO Foundation (io/ package) — BLOCKING

All other phases depend on this.

#### Task 1.1: AvanueIO Interfaces (commonMain)
**New package**: `com.augmentalis.httpavanue.io`

**Create `io/AvanueSource.kt`**:
- Interface replacing `okio.BufferedSource`
- Methods: `readByte()`, `readShort()`, `readInt()`, `readLong()`, `readByteArray(byteCount)`, `readUtf8(byteCount)`, `request(byteCount): Boolean`, `require(byteCount)`, `skip(byteCount)`, `peek(): AvanueSource`
- Property: `val buffer: AvanueBuffer`
- `fun close()`

**Create `io/AvanueSink.kt`**:
- Interface replacing `okio.BufferedSink`
- Methods: `writeByte(value)`, `writeShort(value)`, `writeInt(value)`, `write(bytes)`, `write(bytes, offset, count)`, `writeUtf8(string)`, `flush()`, `close()`

**Create `io/AvanueBuffer.kt`**:
- Growing `ByteArray` internal storage (start 256, double on overflow)
- Implements both read and write methods
- `snapshot(): ByteArray` (non-destructive copy — needed for HTTP/2 preface peek)
- `size: Long` property
- `readUtf8(): String`, `readUtf8Line(): String?`

**Create `io/AvanueByteString.kt`**:
- `ByteArray.toBase64(): String` (kotlin.io.encoding.Base64)
- `ByteArray.sha1(): ByteArray` (delegates to existing `expect fun sha1()`)
- `String.decodeBase64(): ByteArray`

#### Task 1.2: Platform Implementations

**JVM (androidMain + desktopMain)**:
- `io/AvanueSourceJvm.kt`: wraps `java.io.InputStream` with prefetch buffer
- `io/AvanueSinkJvm.kt`: wraps `java.io.OutputStream` with batch write buffer

**iOS (iosMain)**:
- `io/AvanueSourceIos.kt`: replaces existing `SocketSource` (BSD `recv()` → buffer)
- `io/AvanueSinkIos.kt`: replaces existing `SocketSink` (buffer → BSD `send()`)

#### Task 1.3: Socket Contract Migration

**Modify `platform/Socket.kt`** (commonMain expect):
- `fun source(): BufferedSource` → `fun source(): AvanueSource`
- `fun sink(): BufferedSink` → `fun sink(): AvanueSink`

**Modify `platform/Socket.android.kt`**:
- Remove okio imports (`okio.buffer`, `okio.source`, `okio.sink`)
- `bufferedSource = jvmSocket.getInputStream().source().buffer()` → `bufferedSource = AvanueSourceJvm(jvmSocket.getInputStream())`
- Same pattern for sink

**Modify `platform/Socket.desktop.kt`**: Same changes as Android (identical JVM code)

**Modify `platform/Socket.ios.kt`**:
- Remove `okio.*` wildcard import
- Remove `SocketSource`/`SocketSink` inner classes (replaced by `AvanueSourceIos`/`AvanueSinkIos`)
- `bufferedSource = SocketSource(socketFd).buffer()` → `bufferedSource = AvanueSourceIos(socketFd)`

#### Task 1.4: Consumer Migration (12 files, mechanical)

| File | Changes |
|------|---------|
| `server/HttpParser.kt` | `import okio.{Buffer,BufferedSource}` → `import ...io.{AvanueBuffer,AvanueSource}`, `Buffer()` → `AvanueBuffer()` |
| `server/HttpServer.kt` | `source.buffer.snapshot().toByteArray()` → `source.buffer.snapshot()` (already ByteArray) |
| `client/RealHttpClient.kt` | `import okio.{Buffer,BufferedSink}` → `import ...io.{AvanueBuffer,AvanueSink}`, `sink.write(buffer, buffer.size)` → `sink.write(buffer.toByteArray())` |
| `client/ResponseParser.kt` | Same pattern as HttpParser |
| `websocket/WebSocketParser.kt` | `import okio.BufferedSource` → `import ...io.AvanueSource` |
| `websocket/WebSocketClient.kt` | `import okio.Buffer` → `import ...io.AvanueBuffer`, `buffer.snapshot().toByteArray()` → `buffer.snapshot()` |
| `websocket/WebSocketClientHandshake.kt` | `import okio.ByteString.Companion.toByteString` → remove; `.toByteString().base64()` → `.toBase64()`, `.toByteString().sha1().base64()` → `.sha1().toBase64()` |
| `http2/Http2FrameCodec.kt` | `BufferedSource`/`BufferedSink` → `AvanueSource`/`AvanueSink` in all 11 method signatures |
| `http2/Http2FlowControl.kt` | `import okio.BufferedSink` → `import ...io.AvanueSink` |
| `http2/Http2Connection.kt` | `okio.BufferedSink` → `AvanueSink` in all 7 method signatures (inline qualified refs) |

#### Task 1.5: Remove Okio Dependency

**Modify `build.gradle.kts`**: Remove `implementation(libs.okio)` from commonMain.

**Verification Gate**: `./gradlew :Modules:HTTPAvanue:compileKotlinAndroid :Modules:HTTPAvanue:compileKotlinDesktop` must pass with zero okio references.

---

### Phase 2: Middleware Cluster (middleware/ package)

Adjacent to existing CorsMiddleware, LoggerMiddleware, etc. All 7 tasks are independent — swarm-eligible.

#### Task 2.1: HSTS Middleware
**Create `middleware/HstsMiddleware.kt`** (~25 lines)
- Adds `Strict-Transport-Security: max-age=31536000; includeSubDomains` to all responses
- Config: `maxAge: Long`, `includeSubdomains: Boolean`, `preload: Boolean`
- `hstsMiddleware(config)` factory

#### Task 2.2: Forwarded Headers Middleware
**Create `middleware/ForwardedHeadersMiddleware.kt`** (~50 lines)
- Reads `X-Forwarded-For`, `X-Forwarded-Proto`, `X-Forwarded-Host`, `X-Real-IP`
- Writes canonical values into `request.context` (`remote_address`, `scheme`, `host`)
- Config: `trustedProxies: Set<String>` (prevents spoofing from untrusted sources)
- `forwardedHeadersMiddleware(config)` factory

#### Task 2.3: Auto HEAD Middleware
**Create `middleware/AutoHeadMiddleware.kt`** (~25 lines)
- Intercepts HEAD requests, runs GET handler, strips body, preserves Content-Length
- `autoHeadMiddleware()` factory

#### Task 2.4: Content Negotiation Middleware
**Create `middleware/ContentNegotiationMiddleware.kt`** (~65 lines)
- Parses `Accept` header, matches against registered content types
- Default: `application/json` when Accept matches or is `*/*`
- Extension: `inline fun <reified T> HttpResponse.Companion.jsonTyped(value: T): HttpResponse`
- Builds on existing `HttpRequest.parseJson<T>()` from BodyParserMiddleware
- `contentNegotiationMiddleware()` factory

#### Task 2.5: Multipart Parser (NanoHTTPD gap #1)
**Create `middleware/MultipartParser.kt`** (~110 lines)
- Parses `multipart/form-data` from `request.body: ByteArray`
- Extracts boundary from Content-Type header
- Returns `List<MultipartPart>` via `HttpRequest.multipartParts()` extension
- `data class MultipartPart(name: String, filename: String?, contentType: String?, data: ByteArray)`
- Handles Content-Disposition parsing, boundary scanning, CRLF handling
- `multipartMiddleware()` factory (auto-parses multipart requests, stores parts in context)

#### Task 2.6: Cookie Middleware (NanoHTTPD gap #2)
**Create `middleware/CookieMiddleware.kt`** (~70 lines)
- `data class Cookie(name, value, maxAge, path, domain, secure, httpOnly, sameSite)`
- `HttpRequest.cookies(): Map<String, String>` — parses Cookie header
- `HttpRequest.cookie(name): String?` — get single cookie
- `HttpResponse.withCookie(cookie): HttpResponse` — adds Set-Cookie header
- `HttpResponse.withoutCookie(name): HttpResponse` — expires cookie (maxAge=-1)
- `SameSite` enum: `Strict`, `Lax`, `None`
- Full attribute support (Path, Secure, HttpOnly, SameSite, Domain) — exceeds NanoHTTPD's limited CookieHandler

#### Task 2.7: Date Header Middleware (NanoHTTPD gap #3)
**Create `middleware/DateHeaderMiddleware.kt`** (~20 lines)
- Auto-adds `Date: <RFC 7231 format>` header to every response
- Uses `kotlinx.datetime` for cross-platform formatting
- `dateHeaderMiddleware()` factory

---

### Phase 3: Routing Cluster (routing/ package)

#### Task 3.1: Typed Route DSL
**Create `routing/TypedRoutes.kt`** (~45 lines)
- `inline fun <reified Req, reified Resp> RouterImpl.getTyped(pattern, handler)`
- Variants: `postTyped`, `putTyped`, `deleteTyped`, `patchTyped`
- Auto-deserializes request body via `parseJson<Req>()`
- Auto-serializes response via `Json.encodeToString()`
- Pure extension functions — zero changes to RouterImpl.kt
- Content-Type auto-set to `application/json`

---

### Phase 4: StaticFile Cluster (middleware/ package)

#### Task 4.1: ETag Middleware
**Create `middleware/ETagMiddleware.kt`** (~65 lines)
- Computes weak ETag via FNV-1a hash of response body (pure Kotlin, no crypto deps)
- Adds `ETag` header to 200 responses with body
- Returns `304 Not Modified` when `If-None-Match` matches
- Configurable: `includeWeakPrefix: Boolean` (default true)
- `etagMiddleware()` factory

#### Task 4.2: Range Request Middleware
**Create `middleware/RangeMiddleware.kt`** (~85 lines)
- Parses `Range: bytes=N-M` header (single range only)
- Returns `206 Partial Content` with `Content-Range: bytes N-M/total`, sliced body
- Returns `416 Range Not Satisfiable` for invalid ranges (N > body size)
- Adds `Accept-Ranges: bytes` to all responses
- Interacts correctly with ETag (If-Range validation)
- `rangeMiddleware()` factory

---

### Phase 5: WebSocket Cluster (websocket/ package)

#### Task 5.1: Typed WebSocket
**Create `websocket/TypedWebSocket.kt`** (~55 lines)
- `class TypedWebSocket<Req, Resp>(socket, reqSerializer, respSerializer, json)`
- `val messages: Flow<Req>` — maps `WebSocketMessage.Text` → deserialized `Req`
- `suspend fun send(response: Resp)` — serializes → `sendText`
- `suspend fun sendBinary(data: ByteArray)` — pass-through binary
- Extension: `inline fun <reified Req, reified Resp> WebSocket.typed(): TypedWebSocket<Req, Resp>`

---

### Phase 6: Testing Cluster (testing/ + server/)

#### Task 6.1: In-Process Test Engine
**Create `testing/InProcessEngine.kt`** (~65 lines)
- `class InProcessEngine(server: HttpServer)` — calls middleware pipeline directly, no network
- `suspend fun handle(request: HttpRequest): HttpResponse`
- Convenience: `get(path)`, `post(path, body)`, `put(path, body)`, `delete(path)`
- Extension: `fun HttpServer.testEngine(): InProcessEngine`

**Modify `server/HttpServer.kt`**: Add `internal suspend fun testHandle(request): HttpResponse`
- One line: `middlewarePipeline.execute(request) { req -> router.handle(req) }`

---

### Phase 7: VoiceOS Integration (voice/ + avid/ packages) — Branch B Only

**RULE**: These packages MUST NOT import from VoiceOSCore, AVACode, or any external module. They only use HTTPAvanue types (HttpResponse, RouteRegistry, RouteEntry, etc.).

#### Task 7.1: Voice Route Annotations
**Create `voice/VoiceRoute.kt`** (~85 lines)
- `data class VoiceRouteConfig(phrase: String, aliases: List<String>, category: String, description: String)`
- Stored in `RouteEntry.metadata["voice"]` (existing metadata map)
- `RouterImpl.getVoiced(pattern, voice, handler)` — registers route with voice metadata
- Variants: `postVoiced`, `putVoiced`, `deleteVoiced`
- `object VoiceRouteExporter` — extracts voice metadata from all routes
- `toVosString(registry): String` — generates VOS compact format (pipe-delimited v3.0)
- No imports from VoiceOSCore — VOS format is just a string output

#### Task 7.2: AVID-aware Responses
**Create `avid/AvidResponse.kt`** (~55 lines)
- `HttpResponse.Companion.avidJson(body, avidType, avidLabel)` — wraps JSON with `_avid` metadata
- `HttpResponse.Companion.avidJsonList(items, avidType, labelOf)` — list with indexed AVID identifiers
- AVID format: `{"data": ..., "_avid": {"type": "BTN", "label": "Save", "id": "BTN:save"}}`
- AVID types: `BTN`, `INP`, `SEL`, `LNK`, `NAV`, `TAB` (string constants, no enum from other modules)
- No imports from VoiceOSCore — AVID format is just JSON structure

---

### Phase 8: mDNS Discovery (mdns/ package)

Self-contained, no dependencies on other phases (except AvanueIO for socket).

#### Task 8.1: DNS Wire Format
**Create `mdns/DnsMessage.kt`** + **`mdns/MdnsRecord.kt`** (~125 lines)
- `DnsMessage(id, flags, questions, answers, additionals)`
- `encode(): ByteArray` — RFC 1035 wire format encoding
- `companion fun decode(data: ByteArray): DnsMessage` — wire format decoding
- Record types: `A` (IPv4), `PTR` (pointer), `SRV` (service), `TXT` (text)
- Each record type: `encode()/decode()`, `name`, `ttl`, `data`
- Name compression (pointer encoding 0xC0xx)
- Pure byte manipulation — zero external deps

#### Task 8.2: mDNS Advertiser
**Create `mdns/MdnsService.kt`** (~15 lines):
- `data class MdnsService(name: String, type: String, domain: String, port: Int, txt: Map<String, String>)`

**Create `mdns/MdnsAdvertiser.kt`** (expect):
- `expect class MdnsAdvertiser { suspend fun start(service: MdnsService); fun stop() }`

**Platform actuals**:
- `androidMain/mdns/MdnsAdvertiser.android.kt` — `java.net.MulticastSocket` on 224.0.0.251:5353
- `desktopMain/mdns/MdnsAdvertiser.desktop.kt` — Same JVM implementation
- `iosMain/mdns/MdnsAdvertiser.ios.kt` — `platform.posix.sendto` to multicast address

**Integration**: `fun HttpServer.advertise(name: String, txt: Map<String, String> = emptyMap()): MdnsAdvertiser`

---

### Phase 9: Binary Protocol (websocket/ package)

#### Task 9.1: Binary Protocol
**Create `websocket/BinaryProtocol.kt`** (~75 lines)
- Wire format: `[4-byte magic 0x4156_4E45 "AVNE"][2-byte type][4-byte length][payload]`
- `BinaryProtocol.encode(type: UShort, payload: ByteArray): ByteArray`
- `BinaryProtocol.decode(source: AvanueSource): BinaryFrame`
- `data class BinaryFrame(type: UShort, payload: ByteArray)`
- `class BinaryProtocolSession(socket, onFrame: suspend (BinaryFrame) -> Unit)` — frame dispatch loop
- Pre-defined type constants: `TYPE_CAST = 0x01`, `TYPE_VOCAB = 0x02`, `TYPE_CMD = 0x03`, `TYPE_ACK = 0x04`

---

### Phase 10: AVACode Bridge (avacode/ package) — Branch B Only

**NOTE**: This phase has a cross-module dependency on `Modules/AVACode`. It's the ONLY package in HTTPAvanue that imports from another module.

#### Task 10.1: mountForm Extension
**Create `avacode/FormRoutes.kt`** (~45 lines)
- `fun RouterImpl.mountForm(form: FormDefinition, prefix: String = "/api", voiced: Boolean = false)`
- Auto-generates: `POST {prefix}/{form.id}` (validate + submit), `GET {prefix}/{form.id}/schema` (field definitions), `POST {prefix}/{form.id}/completion` (progress check)
- Validation errors return 400 with `{valid: false, errors: {field: [messages]}}`
- When `voiced = true`, registers Voice Routes for each endpoint

#### Task 10.2: mountWorkflow Extension
**Create `avacode/WorkflowRoutes.kt`** (~65 lines)
- `fun RouterImpl.mountWorkflow(workflow: WorkflowDefinition, prefix: String = "/api", voiced: Boolean = false)`
- Auto-generates: `POST` (create), `GET /:id` (state), `POST /:id/next`, `POST /:id/back`, `POST /:id/skip`, `POST /:id/jump/:stepId`, `POST /:id/cancel`, `GET /:id/progress`, `GET /:id/history`
- In-memory instance store via `ConcurrentHashMap` (simple, no external DB needed)
- When `voiced = true`, registers "start {workflow.id}", "next step", "go back", "skip step"

---

### Phase 11: Build Configuration + Verification

#### Task 11.1: Gradle Multi-Publication Setup
**Modify `build.gradle.kts`**:
- Add conditional source set for `avacode/` package (only compiles when AVACode dep is present)
- Ensure `voice/` and `avid/` packages compile with zero external imports
- Verify okio is fully removed from all configurations

#### Task 11.2: Final Verification
- `grep -r "okio" Modules/HTTPAvanue/src/` returns 0 results
- `./gradlew :Modules:HTTPAvanue:assembleDebug` compiles clean
- HTTP/2 prior-knowledge handshake still works (preface peek via `source.buffer.snapshot()`)
- All existing RemoteCast consumers still compile (CastWebSocketServer, CastWebSocketClient)

---

## File Manifest

### New Files (33)

| # | Path (relative to `Modules/HTTPAvanue/src/`) | Phase |
|---|----------------------------------------------|-------|
| 1 | `commonMain/.../io/AvanueSource.kt` | 1.1 |
| 2 | `commonMain/.../io/AvanueSink.kt` | 1.1 |
| 3 | `commonMain/.../io/AvanueBuffer.kt` | 1.1 |
| 4 | `commonMain/.../io/AvanueByteString.kt` | 1.1 |
| 5 | `androidMain/.../io/AvanueSourceJvm.kt` | 1.2 |
| 6 | `androidMain/.../io/AvanueSinkJvm.kt` | 1.2 |
| 7 | `desktopMain/.../io/AvanueSourceJvm.kt` | 1.2 |
| 8 | `desktopMain/.../io/AvanueSinkJvm.kt` | 1.2 |
| 9 | `iosMain/.../io/AvanueSourceIos.kt` | 1.2 |
| 10 | `iosMain/.../io/AvanueSinkIos.kt` | 1.2 |
| 11 | `commonMain/.../middleware/HstsMiddleware.kt` | 2.1 |
| 12 | `commonMain/.../middleware/ForwardedHeadersMiddleware.kt` | 2.2 |
| 13 | `commonMain/.../middleware/AutoHeadMiddleware.kt` | 2.3 |
| 14 | `commonMain/.../middleware/ContentNegotiationMiddleware.kt` | 2.4 |
| 15 | `commonMain/.../middleware/MultipartParser.kt` | 2.5 |
| 16 | `commonMain/.../middleware/CookieMiddleware.kt` | 2.6 |
| 17 | `commonMain/.../middleware/DateHeaderMiddleware.kt` | 2.7 |
| 18 | `commonMain/.../routing/TypedRoutes.kt` | 3.1 |
| 19 | `commonMain/.../middleware/ETagMiddleware.kt` | 4.1 |
| 20 | `commonMain/.../middleware/RangeMiddleware.kt` | 4.2 |
| 21 | `commonMain/.../websocket/TypedWebSocket.kt` | 5.1 |
| 22 | `commonMain/.../testing/InProcessEngine.kt` | 6.1 |
| 23 | `commonMain/.../voice/VoiceRoute.kt` | 7.1 |
| 24 | `commonMain/.../avid/AvidResponse.kt` | 7.2 |
| 25 | `commonMain/.../mdns/DnsMessage.kt` | 8.1 |
| 26 | `commonMain/.../mdns/MdnsRecord.kt` | 8.1 |
| 27 | `commonMain/.../mdns/MdnsService.kt` | 8.2 |
| 28 | `commonMain/.../mdns/MdnsAdvertiser.kt` | 8.2 |
| 29 | `androidMain/.../mdns/MdnsAdvertiser.android.kt` | 8.2 |
| 30 | `desktopMain/.../mdns/MdnsAdvertiser.desktop.kt` | 8.2 |
| 31 | `iosMain/.../mdns/MdnsAdvertiser.ios.kt` | 8.2 |
| 32 | `commonMain/.../websocket/BinaryProtocol.kt` | 9.1 |
| 33 | `commonMain/.../avacode/FormRoutes.kt` | 10.1 |
| 34 | `commonMain/.../avacode/WorkflowRoutes.kt` | 10.2 |

### Files to Modify (17)

| # | Path | Phase | Changes |
|---|------|-------|---------|
| 1 | `commonMain/.../io/` (new package) | 1.1 | Create package |
| 2 | `commonMain/.../platform/Socket.kt` | 1.3 | Return type change |
| 3 | `androidMain/.../platform/Socket.android.kt` | 1.3 | Replace okio wrapping |
| 4 | `desktopMain/.../platform/Socket.desktop.kt` | 1.3 | Replace okio wrapping |
| 5 | `iosMain/.../platform/Socket.ios.kt` | 1.3 | Remove SocketSource/SocketSink, use AvanueSourceIos/AvanueSinkIos |
| 6 | `commonMain/.../server/HttpParser.kt` | 1.4 | BufferedSource → AvanueSource |
| 7 | `commonMain/.../server/HttpServer.kt` | 1.4 + 6.1 | AvanueSource + testHandle() |
| 8 | `commonMain/.../client/RealHttpClient.kt` | 1.4 | Buffer → AvanueBuffer |
| 9 | `commonMain/.../client/ResponseParser.kt` | 1.4 | BufferedSource → AvanueSource |
| 10 | `commonMain/.../websocket/WebSocketParser.kt` | 1.4 | BufferedSource → AvanueSource |
| 11 | `commonMain/.../websocket/WebSocketClient.kt` | 1.4 | Buffer → AvanueBuffer |
| 12 | `commonMain/.../websocket/WebSocketClientHandshake.kt` | 1.4 | ByteString → AvanueByteString |
| 13 | `commonMain/.../http2/Http2FrameCodec.kt` | 1.4 | Both types |
| 14 | `commonMain/.../http2/Http2FlowControl.kt` | 1.4 | BufferedSink → AvanueSink |
| 15 | `commonMain/.../http2/Http2Connection.kt` | 1.4 | okio.BufferedSink → AvanueSink |
| 16 | `build.gradle.kts` | 1.5 + 11.1 | Remove okio dep, add conditional source sets |

---

## Swarm Dispatch Plan

### Wave 1 (Sequential — Phase 1 is blocking)
- **Agent 1**: Phase 1 — AvanueIO (Tasks 1.1 → 1.5) — MUST complete before Wave 2

### Wave 2 (Parallel — all independent)
- **Agent 2**: Phase 2 Tasks 2.1-2.3 (HSTS + Forwarded + AutoHEAD)
- **Agent 3**: Phase 2 Tasks 2.4-2.5 (ContentNeg + Multipart)
- **Agent 4**: Phase 2 Tasks 2.6-2.7 (Cookie + DateHeader)
- **Agent 5**: Phase 3 Task 3.1 (TypedRoutes)

### Wave 3 (Parallel — all independent)
- **Agent 6**: Phase 4 Tasks 4.1-4.2 (ETag + Range)
- **Agent 7**: Phase 5 Task 5.1 (TypedWebSocket)
- **Agent 8**: Phase 6 Task 6.1 (InProcessEngine)

### Wave 4 (Parallel — all independent)
- **Agent 9**: Phase 7 Tasks 7.1-7.2 (VoiceRoute + AvidResponse)
- **Agent 10**: Phase 8 Tasks 8.1-8.2 (mDNS)
- **Agent 11**: Phase 9 Task 9.1 (BinaryProtocol)

### Wave 5 (Sequential — depends on AVACode module)
- **Agent 12**: Phase 10 Tasks 10.1-10.2 (AVACode bridge)
- **Agent 13**: Phase 11 Tasks 11.1-11.2 (Build config + verification)

---

## Time Estimates

| Phase | Sequential | Swarm | Notes |
|-------|-----------|-------|-------|
| Phase 1 (AvanueIO) | 4 hrs | 4 hrs | Cannot parallelize — dependency chain |
| Phase 2 (Middlewares) | 3 hrs | 1 hr | 3 agents parallel |
| Phase 3 (Routing) | 30 min | 30 min | Single task |
| Phase 4 (StaticFile) | 1 hr | 1 hr | 1 agent |
| Phase 5 (WebSocket) | 30 min | 30 min | 1 agent |
| Phase 6 (Testing) | 30 min | 30 min | 1 agent |
| Phase 7 (VoiceOS) | 1 hr | 1 hr | 1 agent |
| Phase 8 (mDNS) | 2 hrs | 2 hrs | 1 agent |
| Phase 9 (Binary) | 30 min | 30 min | 1 agent |
| Phase 10 (AVACode) | 1 hr | 1 hr | 1 agent |
| Phase 11 (Build) | 30 min | 30 min | Sequential |
| **Total** | **14.5 hrs** | **~8.5 hrs** | **41% savings** |

---

## Commit Strategy

| # | Commit Message | Phases | Files |
|---|----------------|--------|-------|
| 1 | `feat(HTTPAvanue): replace Okio with AvanueIO — zero external I/O deps` | 1 | 4 new + 17 modified |
| 2 | `feat(HTTPAvanue): add 7 Tier 1 middlewares + typed routes + multipart + cookies` | 2-3 | 8 new |
| 3 | `feat(HTTPAvanue): add ETag, Range, TypedWebSocket, InProcessEngine` | 4-6 | 4 new + 1 modified |
| 4 | `feat(HTTPAvanue): add VoiceRoutes, AVID responses, mDNS, BinaryProtocol` | 7-9 | 8 new |
| 5 | `feat(HTTPAvanue): add AVACode bridge — mountForm + mountWorkflow` | 10 | 2 new |
| 6 | `docs: add HTTPAvanue v2.0 plan + NanoHTTPD comparison + Chapter 104` | 11 | docs only |

---

## Verification Checklist

| # | Test | Phase |
|---|------|-------|
| 1 | `grep -r "okio" Modules/HTTPAvanue/src/` returns 0 results | 1 |
| 2 | `./gradlew :Modules:HTTPAvanue:compileKotlinAndroid` passes | 1 |
| 3 | `./gradlew :Modules:HTTPAvanue:compileKotlinDesktop` passes | 1 |
| 4 | HTTP/2 prior-knowledge handshake (preface peek) still works | 1 |
| 5 | RemoteCast CastWebSocketServer/Client still compiles | 1 |
| 6 | Multipart form data parsing extracts boundary + parts | 2 |
| 7 | Cookie round-trip: set → read → expire | 2 |
| 8 | Date header format matches RFC 7231 | 2 |
| 9 | Typed route JSON round-trip with @Serializable data class | 3 |
| 10 | GET with `Range: bytes=0-99` → 206 Partial Content | 4 |
| 11 | Second GET → 304 Not Modified via ETag | 4 |
| 12 | Typed WebSocket with @Serializable data class round-trip | 5 |
| 13 | InProcessEngine handles GET/POST without network binding | 6 |
| 14 | `VoiceRouteExporter.toVosString()` produces valid VOS entries | 7 |
| 15 | AVID response JSON contains `_avid` metadata field | 7 |
| 16 | mDNS: `dns-sd -B _http._tcp` discovers advertised service | 8 |
| 17 | Binary protocol session handles 1000 frames without error | 9 |
| 18 | `mountForm()` auto-generates POST validation endpoint | 10 |
| 19 | `voice/` and `avid/` packages have zero external imports | 11 |
| 20 | Full build: `./gradlew :Modules:HTTPAvanue:assembleDebug` clean | 11 |

---

## Related Documents

- NanoHTTPD Comparison: `docs/analysis/HTTPAvanue/HTTPAvanue-Analysis-NanoHTTPDFeatureComparison-260222-V1.md`
- AVACode Recipe Plan: `docs/plans/AVACode/AVACode-Plan-RecipeSystemPending-260222-V1.md`
- Chapter 101: `Docs/MasterDocs/HTTPAvanue/Developer-Manual-Chapter101-HTTPAvanueKMPHttpServerLibrary.md`
- Audit Fixes: `Docs/fixes/HTTPAvanue/HTTPAvanue-Fix-AuditBugs-260220-V1.md`
