# HTTPAvanue Analysis: NanoHTTPD Feature Comparison

**Module**: HTTPAvanue
**Type**: Analysis
**Date**: 2026-02-22
**Version**: V1
**Branch**: VoiceOS-1M-SpeechEngine
**Author**: Manoj Jhawar

---

## Purpose

Comprehensive feature parity evaluation of HTTPAvanue (Modules/HTTPAvanue/) against NanoHTTPD (the widely-used lightweight Java HTTP server, last release 2.3.1 in 2016), including all NanoHTTPD plugins (nanohttpd-websocket, nanohttpd-nanolets, nanohttpd-webserver, nanohttpd-apache-fileupload). This analysis informs the HTTPAvanue v2.0 enhancement plan.

---

## Module Overview

### HTTPAvanue (Current State)

- **Files**: 70 (69 .kt + 1 build.gradle.kts)
- **Source Sets**: commonMain (54), androidMain (5), desktopMain (5), iosMain (5)
- **KMP Score**: 78.3% shared code
- **Packages**: 13 feature packages (auth, client, core, hpack, http, http2, metrics, middleware, platform, routing, server, sse, websocket)
- **External Dependencies**: Okio (targeted for removal in v2.0), BouncyCastle (JVM TLS), kotlinx-coroutines, kotlinx-serialization, kotlinx-datetime

### NanoHTTPD

- **Artifacts**: 6 Maven modules (core, webserver, websocket, nanolets, apache-fileupload, samples)
- **Language**: Java only
- **Protocol**: HTTP/1.1 only
- **Threading**: Thread-per-connection (pluggable via IAsyncRunner)
- **Status**: Effectively unmaintained (no releases since 2016)

---

## Feature-by-Feature Comparison

### 1. Core Server

| Feature | NanoHTTPD | HTTPAvanue | Winner |
|---------|-----------|------------|--------|
| Accept loop | Thread-based daemon | Coroutine-based (SocketServer.accept()) | HTTPAvanue |
| Connection model | Thread-per-connection (IAsyncRunner) | Coroutine-per-connection (structured concurrency) | HTTPAvanue |
| Pluggable thread pool | IAsyncRunner interface, BoundRunner | Coroutine dispatcher (built-in) | HTTPAvanue |
| Keep-alive | HTTP/1.1 persistent connections | HTTP/1.1 persistent connections | Parity |
| Max connections | No built-in limit | ServerConfig.maxConnections with tracking | HTTPAvanue |
| Request timeout | Socket read timeout only (5s default) | ServerConfig.requestTimeout | Parity |
| Max body size | No limit (memory → temp file) | ServerConfig.maxRequestBodySize | HTTPAvanue |
| Server lifecycle | start()/stop()/isAlive() | start()/stop() with structured coroutine cleanup | Parity |
| DSL builder | None | httpServer { } Kotlin DSL | HTTPAvanue |

### 2. HTTP Protocol

| Feature | NanoHTTPD | HTTPAvanue | Winner |
|---------|-----------|------------|--------|
| HTTP/1.1 | Full | Full | Parity |
| HTTP/2 | **None** | RFC 7540 (prior knowledge + h2c upgrade) | HTTPAvanue |
| HPACK header compression | **None** | RFC 7541 (full encoder/decoder/Huffman) | HTTPAvanue |
| HTTP/2 flow control | **None** | Connection + stream-level window management | HTTPAvanue |
| HTTP/2 stream multiplexing | **None** | Full stream state machine (7 states) | HTTPAvanue |
| HTTP methods | 9 (all standard) | 9 (identical set) | Parity |
| Chunked transfer (request) | Yes | Yes | Parity |
| Chunked transfer (response) | newChunkedResponse() | HttpResponse.toChunked(chunkSize) | Parity |
| Query parameter parsing | Map<String, List<String>> | Map<String, List<String>> | Parity |
| Header size limit | 8 KB | 8 KB (MAX_HEADER_SIZE) | Parity |
| Request line limit | No explicit limit | 2 KB (MAX_REQUEST_LINE_SIZE) | HTTPAvanue |

### 3. Request Parsing

| Feature | NanoHTTPD | HTTPAvanue | Winner |
|---------|-----------|------------|--------|
| URL-encoded form data | parseBody() → parameters map | BodyParserMiddleware + extensions | Parity |
| Multipart form data | Built-in parseBody() with temp files | **Not yet** (in v2.0 plan Phase 2.5) | NanoHTTPD |
| Apache FileUpload integration | nanohttpd-apache-fileupload module | N/A (KMP, no Apache Commons) | NanoHTTPD (niche) |
| JSON body parsing | None built-in | parseJson<T>() with kotlinx.serialization | HTTPAvanue |
| Raw body access | getInputStream() → temp file path | request.body: ByteArray + bodyAsText() | Parity |
| Temp file management | ITempFileManager with 1KB threshold | No temp files (bounded by maxRequestBodySize) | Different approach |
| Remote address | getRemoteIpAddress() | request.remoteAddress | Parity |

### 4. Response Building

| Feature | NanoHTTPD | HTTPAvanue | Winner |
|---------|-----------|------------|--------|
| Fixed-length response | newFixedLengthResponse() | HttpResponse.ok(), .json(), etc. | Parity |
| Chunked response | newChunkedResponse(InputStream) | HttpResponse.toChunked(chunkSize) | Parity |
| Status codes | 25 codes (enum + custom IStatus) | 25 codes (enum with from() fallback) | Parity |
| JSON response | None built-in | HttpResponse.json() with serialization | HTTPAvanue |
| Error response envelope | None | ErrorResponse {error, message, status, path} | HTTPAvanue |
| MIME type map | Dynamic from classpath properties | 24 built-in extensions | Parity |
| Date header | Auto-added to every response | Not auto-added | NanoHTTPD (minor) |

### 5. Routing

| Feature | NanoHTTPD | HTTPAvanue | Winner |
|---------|-----------|------------|--------|
| URL routing | nanolets plugin: addRoute("/path", Handler.class) | RouterImpl: get("/path") { } DSL | HTTPAvanue |
| Path parameters | :id capture | :id capture via RoutePattern | Parity |
| Route groups | None | group(prefix, middleware) { } | HTTPAvanue |
| Per-route middleware | None | get("/path", listOf(middleware)) { } | HTTPAvanue |
| Before/after hooks | None | before { } / after { } lists | HTTPAvanue |
| Route prioritization | 3 strategies (DefaultRoutePrioritizer, etc.) | Dual-index: O(1) static + sequential dynamic | Different |
| Named routes | None | Named routes with find-by-name | HTTPAvanue |
| Route introspection | None | routes(), routesCount(), RegistryStats | HTTPAvanue |
| URL generation from pattern | None | RoutePattern.generate(params) | HTTPAvanue |

### 6. Middleware / Interceptors

| Feature | NanoHTTPD | HTTPAvanue | Winner |
|---------|-----------|------------|--------|
| Middleware pattern | addHTTPInterceptor(IHandler) (basic) | MiddlewarePipeline (Chain of Responsibility) | HTTPAvanue |
| CORS | SimpleWebServer --cors flag only | CorsMiddleware (full config, preflight, credentials) | HTTPAvanue |
| Rate limiting | **None** | RateLimitMiddleware (token bucket, per-IP + per-endpoint) | HTTPAvanue |
| Authentication | **None** | AuthenticationMiddleware (Bearer token, path exclusions) | HTTPAvanue |
| Error handling | None (manual try/catch) | ErrorHandlerMiddleware (JSON envelope, status mapping) | HTTPAvanue |
| Logging | Console with --quiet flag | LoggerMiddleware (platform logger) | Parity |
| Timing | **None** | TimingMiddleware (X-Response-Time, slow-request warnings) | HTTPAvanue |
| Metrics | **None** | MetricsMiddleware + ServerMetrics (per-status/method/path) | HTTPAvanue |
| Compression (gzip) | Response.setUseGzip() | CompressionMiddleware (JVM java.util.zip, iOS libz) | Parity |
| Static files | SimpleWebServer (directory listing, index) | StaticFileMiddleware (resource-based, traversal guard) | Parity |

### 7. SSL/TLS

| Feature | NanoHTTPD | HTTPAvanue | Winner |
|---------|-----------|------------|--------|
| TLS support | makeSecure(SSLSocketFactory) — JKS/PKCS12 | TlsConfig — PEM-based, BouncyCastle (JVM) | Parity |
| Client cert auth | requireClientCert on SSLServerSocket | TlsConfig.requireClientCert | Parity |
| Cipher suite selection | setEnabledCipherSuites() | TlsConfig.cipherSuites | Parity |
| Protocol selection | None explicit | TlsConfig.protocols (TLSv1.3/1.2 defaults) | HTTPAvanue |
| Dev/prod presets | None | TlsConfig.development() / .production() | HTTPAvanue |
| Self-signed cert support | Manual KeyStore | TlsConfig.allowSelfSigned flag | HTTPAvanue |
| iOS TLS | N/A (Java-only) | **Not yet** (POSIX sockets, no Security.framework) | Gap on iOS |

### 8. WebSocket

| Feature | NanoHTTPD | HTTPAvanue | Winner |
|---------|-----------|------------|--------|
| Server-side WebSocket | NanoWSD plugin (RFC 6455) | Built-in WebSocket + WebSocketHandshake | Parity |
| Client-side WebSocket | **None** | WebSocketClient with auto-reconnect, backoff + jitter | HTTPAvanue |
| Frame types | All 6 (TEXT, BINARY, CLOSE, PING, PONG, CONTINUATION) | All 6 | Parity |
| Fragmentation | Multi-frame reassembly | Fragmentation with timeout (30s) + size enforcement | HTTPAvanue |
| Close codes | 4 codes (1000, 1002, 1007, 1011) | 7 codes (1000-1011) | HTTPAvanue |
| Subprotocol negotiation | First offered echoed back | First matching selected | Parity |
| Connection state tracking | 5-state enum | StateFlow<WebSocketConnectionState> (reactive) | HTTPAvanue |
| Message API | Callback (onMessage, onClose, onPong) | Flow<WebSocketMessage> (reactive, composable) | HTTPAvanue |
| Auto-reconnect | **None** | Configurable (maxRetries, baseDelay, maxDelay, jitter) | HTTPAvanue |

### 9. HTTP Client

| Feature | NanoHTTPD | HTTPAvanue | Winner |
|---------|-----------|------------|--------|
| Built-in HTTP client | **None** | RealHttpClient — full client | HTTPAvanue |
| Connection pooling | N/A | ConnectionPool (idle limit + keep-alive) | HTTPAvanue |
| Retry with backoff | N/A | Exponential backoff + 50% jitter | HTTPAvanue |
| Redirect following | N/A | 301/302/303 → GET, 307/308 preserves method | HTTPAvanue |
| TLS client | N/A | Via SocketConfig.tls | HTTPAvanue |

### 10. Additional Features

| Feature | NanoHTTPD | HTTPAvanue | Winner |
|---------|-----------|------------|--------|
| Server-Sent Events | **None** | SseEmitter + SseConnectionManager | HTTPAvanue |
| Cookie handling | CookieHandler (basic, no Path/Secure/HttpOnly) | **Not built-in** (manual via headers) | NanoHTTPD (minor) |
| Session management | None (manual) | None (manual) | Parity |
| KMP / Cross-platform | **Java-only** | Android + Desktop/JVM + iOS | HTTPAvanue |
| Coroutine-native | No (threads) | Yes (structured concurrency) | HTTPAvanue |
| kotlinx.serialization | No | Deep integration | HTTPAvanue |

---

## Scorecard

| Category | NanoHTTPD (inc. plugins) | HTTPAvanue v1.x | Winner |
|----------|--------------------------|------------------|--------|
| Core Server | 7/10 | 9/10 | HTTPAvanue |
| HTTP Protocol | 6/10 | 10/10 | HTTPAvanue |
| Request Parsing | 8/10 | 7/10 | NanoHTTPD |
| Response Building | 7/10 | 8/10 | HTTPAvanue |
| Routing | 6/10 | 9/10 | HTTPAvanue |
| Middleware | 3/10 | 9/10 | HTTPAvanue |
| SSL/TLS | 7/10 | 7/10 | Tie |
| WebSocket | 6/10 | 9/10 | HTTPAvanue |
| HTTP Client | 0/10 | 8/10 | HTTPAvanue |
| Extras (SSE, Metrics) | 2/10 | 8/10 | HTTPAvanue |
| **Overall** | **52/100** | **84/100** | **HTTPAvanue** |

---

## Gaps: What NanoHTTPD Has That HTTPAvanue Doesn't

Only 3 gaps remain:

| Gap | NanoHTTPD Feature | v2.0 Plan Coverage | Priority |
|-----|-------------------|---------------------|----------|
| Multipart parsing | Built-in parseBody() + Apache FileUpload | Phase 2.5 (MultipartParser.kt) | High |
| Cookie helper | CookieHandler class (basic) | Not in v2.0 plan | Low (~30 lines as middleware) |
| Auto Date header | Added to every response | Not in v2.0 plan | Trivial (1-line addition) |

---

## Current VoiceOS Integration

### Architecture

HTTPAvanue is currently used **only by RemoteCast** — it serves as the WebSocket transport for screen casting to smart glasses.

```
Dependency Chain:
  VoiceOSCore ──(no dependency)──→ HTTPAvanue
  RemoteCast  ──(direct dep)────→ HTTPAvanue
  Cockpit     ──→ RemoteCast ───→ HTTPAvanue
  Avanues App ──→ RemoteCast ───→ HTTPAvanue
```

### RemoteCast Usage (2 files)

**CastWebSocketServer.kt** (commonMain):
- `HttpServer` on port 54321 with `http2Enabled = false`
- REST: `GET /cast/status`, `GET /cast/health` → `HttpResponse.json()`
- WebSocket: `/cast/stream` — binary CAST frame streaming (JPEG data)
- Uses: `HttpServer`, `ServerConfig`, `HttpResponse`, `WebSocket`, `WebSocketMessage`, `Socket`, `currentTimeMillis()`

**CastWebSocketClient.kt** (commonMain):
- `WebSocketClient` connecting to `ws://{host}:{port}/cast/stream`
- Auto-reconnect: `WebSocketReconnectConfig(maxRetries=5, baseDelayMs=2000, maxDelayMs=30000)`
- Receives binary `WebSocketMessage.Binary` → JPEG byte arrays
- Uses: `WebSocketClient`, `WebSocketClientConfig`, `WebSocketConnectionState`, `WebSocketMessage`, `WebSocketReconnectConfig`

### Proposed v2.0 VoiceOS Integration (Tier 3)

The v2.0 plan introduces two new VoiceOS integration points:

**Voice Routes (Phase 7.1)** — HTTP endpoints annotated with voice phrases:
- `RouterImpl.getVoiced(pattern, voice, handler)` registers routes with voice metadata
- `VoiceRouteExporter.toVosString()` exports to VOS compact format (pipe-delimited v3.0)
- Enables VoiceOS to discover and invoke HTTP endpoints by voice command

**AVID Responses (Phase 7.2)** — JSON responses with AVID metadata:
- `HttpResponse.avidJson(body, avidType, avidLabel)` wraps data with `_avid` identifiers
- Enables VoiceOS overlay system to render voice-targetable badges on API data
- Format: `{"data": ..., "_avid": {"type": "BTN", "label": "Save", "id": "BTN:save"}}`

---

## v2.0 Plan Enhancement Summary

| Phase | Feature | NanoHTTPD Equivalent | Value |
|-------|---------|----------------------|-------|
| 1 | AvanueIO (Okio elimination) | N/A (NanoHTTPD uses raw Java I/O) | Zero external deps |
| 2.1 | HSTS Middleware | None | Security baseline |
| 2.2 | Forwarded Headers | None | Reverse proxy support |
| 2.3 | Auto HEAD | None | RFC compliance |
| 2.4 | Content Negotiation | None | API ergonomics |
| 2.5 | Multipart Parser | Built-in (closes gap) | File upload support |
| 3.1 | Typed Routes DSL | None | Type-safe endpoints |
| 4.1 | ETag Middleware | SimpleWebServer only | Composable caching |
| 4.2 | Range Requests | SimpleWebServer only | Composable partial content |
| 5.1 | Typed WebSocket | None | Type-safe messaging |
| 6.1 | In-Process Test Engine | None | Testability |
| 7.1 | Voice Routes | None (not applicable) | VoiceOS-specific |
| 7.2 | AVID Responses | None (not applicable) | VoiceOS-specific |
| 8 | mDNS Discovery | None | LAN service discovery |
| 9 | Binary Protocol | None | High-performance wire format |

---

## AVACode Integration Decision (260222)

### Context

AVACode (`Modules/AVACode/`) is a code reduction DSL platform with two active layers:
- **Forms DSL**: Typed form definitions with validation, schema generation, completion tracking
- **Workflows DSL**: Multi-step process orchestration with state machines

### Decision

**AVACode recipes are a cross-module platform initiative, not HTTPAvanue-specific.**

The broader AVACode recipe system should cover ALL modules (AVID, Cockpit, WebAvanue, NoteAvanue, RemoteCast, VoiceOSCore, HTTPAvanue) and will be designed in a **separate dedicated session**.

For HTTPAvanue v2.0:
1. **Phase 7.1 (Voice Routes)** and **Phase 7.2 (AVID Responses)**: Independent HTTPAvanue features, AVACode-aware but not dependent
2. **Phase 10 (AVACode Bridge)**: Lightweight `mountForm()` + `mountWorkflow()` extension functions (~120 lines) that auto-generate REST endpoints from AVACode form/workflow definitions
3. **Broader recipe system**: Deferred to separate session — will include server presets (rest_api, streaming, file_server, voice_api) and runtime `.avp` file loading for the App Store bypass pattern

### AVACode P1 Blocker

`System.currentTimeMillis()` in `WorkflowInstance.kt:94` and `WorkflowPersistence.kt:30` blocks iOS/desktop targets. Must be fixed (replace with `kotlinx.datetime.Clock.System.now().toEpochMilliseconds()`) before any cross-module integration.

### Key Insight: App Store Bypass Pattern

AVACode's DSL approach enables functionality extension without dynamic code loading:
- Apps ship with compiled modules (HTTPAvanue, VoiceOSCore, etc.) in the APK/IPA
- `.avp` text-based recipe files configure functionality at runtime
- Apple/Google see static compiled code, not plugins
- Users customize server endpoints, voice commands, UI layouts via `.avp` files without app updates

---

## Conclusion

HTTPAvanue covers ~95% of NanoHTTPD functionality (including all plugins) while adding HTTP/2, HTTP client, SSE, metrics, rate limiting, authentication, coroutines, and KMP cross-platform support. The v2.0 plan closes the remaining 5% (multipart being the only significant gap) and adds differentiators no embedded HTTP server offers (voice routes, AVID responses, mDNS, binary protocol, AVACode bridge).

---

## Related Documents

- v2.0 Plan: `docs/plans/HTTPAvanue/HTTPAvanue-Plan-OkioEliminationAndEnhancements-260222-V1.md` (pending)
- Three-Path Evaluation: `docs/analysis/HTTPAvanue/HTTPAvanue-Analysis-ThreePathEvaluation-260219-V1.md`
- Chapter 101: `Docs/MasterDocs/HTTPAvanue/Developer-Manual-Chapter101-HTTPAvanueKMPHttpServerLibrary.md`
- Audit Fixes: `Docs/fixes/HTTPAvanue/HTTPAvanue-Fix-AuditBugs-260220-V1.md`
- RemoteCast WebSocket Migration: `Docs/fixes/RemoteCast/RemoteCast-Fix-AuditBugsAndWebSocketMigration-260220-V1.md`
- AVACode Quality Review: `Docs/reviews/AVID-AVU-AVACode-Review-QualityAnalysis-260222-V1.md`
- AVACode Deep Dive (Chapter 11): `Docs/MasterDocs/NewAvanues-Developer-Manual/11-AVACode-Deep-Dive.md`
