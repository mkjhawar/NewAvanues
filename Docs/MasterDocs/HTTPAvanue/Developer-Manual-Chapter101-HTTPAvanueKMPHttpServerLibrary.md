# Chapter 101: HTTPAvanue — KMP HTTP Server Library

**Module:** `Modules/HTTPAvanue`
**Dependency:** `implementation(project(":Modules:HTTPAvanue"))`
**Package:** `com.augmentalis.httpavanue`
**Branch:** HTTPAvanue
**Date:** 2026-02-20

---

## 1. Overview

HTTPAvanue is a standalone, pure-Kotlin multiplatform HTTP server library. It provides:
- HTTP/1.1 server and client
- HTTP/2 with HPACK header compression (RFC 7540/7541)
- WebSocket server and client (RFC 6455)
- Middleware pipeline (Chain of Responsibility pattern)
- Pattern-based routing with parameter extraction
- Server-Sent Events (SSE)
- Server metrics collection

The library was extracted from AvaConnect's production HTTP stack and adapted for the NewAvanues ecosystem. It has zero framework dependencies — no Ktor, no Netty, no NanoHTTPD runtime dependency.

## 2. Quick Start

### Minimal HTTP Server

```kotlin
import com.augmentalis.httpavanue.server.*
import com.augmentalis.httpavanue.http.*

val server = httpServer(ServerConfig(port = 8080)) {
    get("/hello") { request ->
        HttpResponse.ok("Hello, World!")
    }

    get("/users/:id") { request ->
        val userId = request.pathParam("id")
        HttpResponse.json("""{"id":"$userId","name":"User $userId"}""")
    }

    post("/echo") { request ->
        HttpResponse.ok(request.bodyString ?: "", "text/plain")
    }
}

server.start()
// server.stop() to shut down
```

### With Middleware

```kotlin
import com.augmentalis.httpavanue.middleware.*

val server = HttpServer(ServerConfig(port = 8080))

// Add middleware (order matters — first added = outermost)
server.use(
    timingMiddleware(),
    loggerMiddleware(),
    corsMiddleware(CorsConfig(allowedOrigins = setOf("*"))),
    compressionMiddleware(),
    errorHandler(),
    rateLimitMiddleware(maxRequests = 100, windowMs = 60_000),
)

server.routes {
    get("/api/health") { HttpResponse.ok("OK") }
    get("/api/data") { HttpResponse.json("""{"status":"ready"}""") }
}

server.start()
```

### WebSocket

```kotlin
import com.augmentalis.httpavanue.websocket.*

server.websocket("/ws/chat") { socket ->
    val ws = WebSocket(socket, isServer = true)
    ws.start(CoroutineScope(Dispatchers.IO))

    ws.messages.collect { message ->
        when (message) {
            is WebSocketMessage.Text -> {
                ws.sendText("Echo: ${message.data}")
            }
            is WebSocketMessage.Binary -> {
                ws.sendBinary(message.data)
            }
            is WebSocketMessage.Close -> {
                // Connection closed
            }
        }
    }
}
```

### HTTP Client

```kotlin
import com.augmentalis.httpavanue.client.*
import com.augmentalis.httpavanue.http.HttpClient

val client = RealHttpClient(HttpClientConfig(
    maxRedirects = 5,
    userAgent = "MyApp/1.0",
    retryConfig = HttpRetryConfig(maxRetries = 3),
))

val response = client.get("https://api.example.com/data")
if (response.isSuccess) {
    val body = response.body?.decodeToString()
    // process response
}

client.close()
```

## 3. Package Structure

```
com.augmentalis.httpavanue/
├── auth/               # AuthenticationManager interface
├── client/             # HTTP client (RealHttpClient, ResponseParser, ClientModels)
├── core/               # Result type, ByteArraySerializer
│   └── serialization/
├── hpack/              # HPACK header compression (RFC 7541)
│   ├── HpackStaticTable    # 61-entry static table
│   ├── HpackDynamicTable   # FIFO eviction table
│   ├── HpackDecoder        # Integer/string/indexed decoding
│   └── HpackEncoder        # Static lookup + dynamic table encoding
├── http/               # Core HTTP types
│   ├── HttpMethod          # GET, POST, PUT, DELETE, etc.
│   ├── HttpStatus          # 200 OK, 404 Not Found, etc.
│   ├── HttpRequest         # Immutable request with query/path params
│   ├── HttpResponse        # Immutable response with factory methods
│   ├── HttpException       # Status-code exceptions
│   └── HttpClient          # Client interface
├── http2/              # HTTP/2 protocol (RFC 7540)
│   ├── Http2FrameCodec     # 9-byte header read/write, all 10 frame types
│   ├── Http2Settings       # SETTINGS encode/decode
│   ├── Http2Stream         # Stream state machine + flow control
│   ├── Http2Connection     # Connection lifecycle, frame dispatch
│   ├── Http2FlowControl    # Window management
│   ├── Http2ServerHandler  # h2c upgrade + prior knowledge detection
│   └── Http2Error          # Error codes enum
├── metrics/            # Server metrics
│   ├── ServerMetrics       # Request/error/latency tracking
│   └── MetricsMiddleware   # Auto-collect per request
├── middleware/          # Middleware pipeline
│   ├── Middleware           # Interface + MiddlewarePipeline
│   ├── AuthenticationMiddleware
│   ├── BodyParserMiddleware
│   ├── CompressionMiddleware   # gzip (expect/actual per platform)
│   ├── CorsMiddleware
│   ├── ErrorHandlerMiddleware
│   ├── LoggerMiddleware
│   ├── RateLimitMiddleware     # Token bucket, per-IP + per-endpoint
│   ├── StaticFileMiddleware
│   └── TimingMiddleware
├── platform/           # Platform abstractions (expect/actual)
│   ├── Socket / SocketServer / SocketConfig
│   ├── TlsConfig
│   ├── PlatformTime        # currentTimeMillis()
│   └── Resources           # readResource()
├── routing/            # URL routing
│   ├── Router              # Basic router with DSL
│   ├── RoutePattern        # Static + param segments
│   ├── RouteRegistry       # Indexed lookup (static map + dynamic list)
│   └── RouterImpl          # Enhanced router with middleware groups
├── server/             # HTTP server
│   ├── HttpServer          # Main server class
│   └── HttpParser          # HTTP/1.1 request parser (Okio)
├── sse/                # Server-Sent Events
│   ├── SseEmitter          # Per-connection event emitter
│   └── SseConnectionManager # Broadcast to all connections
└── websocket/          # WebSocket (RFC 6455)
    ├── WebSocketFrame      # Opcodes, close codes, frame data
    ├── WebSocketHandshake  # Server-side handshake (expect sha1)
    ├── WebSocketParser     # Frame encode/decode with masking
    ├── WebSocket           # Connection with fragmentation + channels
    ├── WebSocketClient     # Client with auto-reconnect
    ├── WebSocketClientConfig
    └── WebSocketClientHandshake  # Client-side (uses Okio sha1)
```

## 4. Platform Support

| Platform | Socket Impl | TLS | Compression | SHA-1 |
|---|---|---|---|---|
| Android | `java.net.Socket` | BouncyCastle | `java.util.zip.GZIP*` | `MessageDigest` |
| Desktop (JVM) | `java.net.Socket` | BouncyCastle | `java.util.zip.GZIP*` | `MessageDigest` |
| iOS | BSD sockets (`posix`) | Not yet | `platform.zlib` | `CoreCrypto.CC_SHA1` |

### Adding TLS on iOS

The iOS socket implementation currently uses raw BSD sockets without TLS. To add TLS:
1. Use `Network.framework` (`nw_connection_t`) instead of raw BSD sockets, OR
2. Use `CFStream` with `kCFStreamSSLLevel` for SecureTransport integration

## 5. Middleware Reference

Middleware executes in the order added (first = outermost):

| Middleware | Purpose | Key Config |
|---|---|---|
| `timingMiddleware()` | Adds `X-Response-Time` header, warns on slow requests | `warnThresholdMs = 1000` |
| `loggerMiddleware()` | Logs `-> METHOD /path` and `<- STATUS` | Uses Modules/Logging |
| `corsMiddleware(config)` | CORS headers + preflight handling | `CorsConfig(allowedOrigins, allowedMethods, ...)` |
| `compressionMiddleware()` | Gzip compress/decompress based on `Accept-Encoding` | `minBytes = 1024` |
| `errorHandler()` | Catches exceptions, returns JSON error responses | `includeStackTrace = false` |
| `rateLimitMiddleware(100)` | Token bucket rate limiting with `X-RateLimit-*` headers | `maxRequests, windowMs` |
| `authenticationMiddleware(mgr)` | Bearer token validation | `requireAuth, excludedPaths` |
| `staticFileMiddleware("/", "webui")` | Serve static files from resources | `urlPrefix, resourcePath` |
| `bodyParser()` | Body parsing extensions (`parseJson<T>()`, `bodyAsText()`) | `Json` config |

### Custom Middleware

```kotlin
val myMiddleware = middleware { request, next ->
    // Pre-processing
    val modifiedRequest = request.copy(
        context = request.context + ("requestId" to generateId())
    )

    // Call next in chain
    val response = next(modifiedRequest)

    // Post-processing
    response.withHeader("X-Request-Id", modifiedRequest.context["requestId"]!!)
}

server.use(myMiddleware)
```

## 6. HTTP/2 Support

HTTPAvanue supports HTTP/2 via two mechanisms:

### h2c Upgrade (from HTTP/1.1)

The server detects `Upgrade: h2c` headers and branches to HTTP/2:

```kotlin
// In custom connection handler:
if (Http2ServerHandler.isH2cUpgradeRequest(request)) {
    Http2ServerHandler.handleH2cUpgrade(socket, request, settings) { req ->
        router.handle(req)
    }
}
```

### Prior Knowledge (h2c direct)

For clients that speak HTTP/2 directly (e.g., `curl --http2-prior-knowledge`):

```kotlin
// Detect HTTP/2 preface in first bytes:
if (Http2ServerHandler.isPriorKnowledgePreface(firstBytes)) {
    Http2ServerHandler.handlePriorKnowledge(socket, settings) { req ->
        router.handle(req)
    }
}
```

### HTTP/2 Features Implemented

- All 10 frame types: DATA, HEADERS, PRIORITY, RST_STREAM, SETTINGS, PUSH_PROMISE, PING, GOAWAY, WINDOW_UPDATE, CONTINUATION
- HPACK header compression with 61-entry static table + dynamic table
- Stream multiplexing (odd IDs = client-initiated)
- Connection + stream level flow control (WINDOW_UPDATE)
- SETTINGS exchange with ACK
- PING/PONG keepalive
- Graceful GOAWAY shutdown

### Not Yet Implemented

- Huffman encoding in HPACK (valid per spec — non-Huffman encoding works)
- Server Push (PUSH_PROMISE from server)
- HTTP/2 over TLS (h2 with ALPN)
- Stream prioritization (advisory, ignored per spec allowance)

## 7. Server-Sent Events (SSE)

```kotlin
import com.augmentalis.httpavanue.sse.*

val sseManager = SseConnectionManager()

server.routes {
    get("/events") { request ->
        val emitter = sseManager.createEmitter(request.queryParam("clientId") ?: "anon")

        // Return SSE response headers
        SseEmitter.createSseResponse()
        // Note: actual streaming requires writing to socket directly
    }
}

// Broadcast to all connected clients
sseManager.broadcast(SseEvent(
    data = """{"type":"update","value":42}""",
    event = "data-change",
    id = "evt-001",
))
```

## 8. Metrics

```kotlin
import com.augmentalis.httpavanue.metrics.*

val metrics = ServerMetrics()
server.use(metricsMiddleware(metrics))

// Expose metrics endpoint
server.routes {
    get("/metrics") { _ ->
        val snapshot = metrics.snapshot()
        HttpResponse.json("""
            {
                "totalRequests": ${snapshot.totalRequests},
                "activeConnections": ${snapshot.activeConnections},
                "averageResponseTimeMs": ${snapshot.averageResponseTimeMs},
                "requestsPerSecond": ${snapshot.requestsPerSecond},
                "totalErrors": ${snapshot.totalErrors},
                "uptimeMs": ${snapshot.uptimeMs}
            }
        """.trimIndent())
    }
}
```

## 9. Authentication

HTTPAvanue defines a minimal `AuthenticationManager` interface:

```kotlin
interface AuthenticationManager {
    fun validateToken(token: String): TokenValidation
}

data class TokenValidation(
    val valid: Boolean,
    val deviceId: String? = null,
    val capabilities: List<String>? = null,
    val error: String? = null,
)
```

Implement this interface to plug in any auth strategy (JWT, API keys, OAuth tokens):

```kotlin
class JwtAuthManager(private val secret: String) : AuthenticationManager {
    override fun validateToken(token: String): TokenValidation {
        // Validate JWT, extract claims
        return TokenValidation(valid = true, deviceId = "device-123")
    }
}

server.use(authenticationMiddleware(
    authManager = JwtAuthManager("my-secret"),
    excludedPaths = setOf("/health", "/login"),
))
```

## 10. Routing

### Basic Router (DSL)

```kotlin
val server = httpServer(ServerConfig(port = 8080)) {
    get("/users") { HttpResponse.json("[...]") }
    get("/users/:id") { req -> HttpResponse.json("""{"id":"${req.pathParam("id")}"}""") }
    post("/users") { req -> HttpResponse.json(req.bodyString ?: "{}") }
    delete("/users/:id") { HttpResponse(status = 204, statusMessage = "No Content") }
}
```

### Enhanced Router (with groups and middleware)

```kotlin
val router = routerImpl {
    // Public routes
    get("/health") { HttpResponse.ok("OK") }

    // Authenticated API group
    group("/api/v1", middleware = listOf(authMiddleware)) {
        get("/users") { /* ... */ }
        post("/users") { /* ... */ }

        group("/admin", middleware = listOf(adminMiddleware)) {
            delete("/users/:id") { /* ... */ }
        }
    }
}
```

### Route Pattern Matching

- Static: `/users` — exact match
- Parameters: `/users/:id` — captures `id` from path
- Multiple params: `/users/:userId/posts/:postId`
- Query params: accessed via `request.queryParam("key")`

## 11. Build Configuration

```kotlin
// In your module's build.gradle.kts:
dependencies {
    implementation(project(":Modules:HTTPAvanue"))
}
```

HTTPAvanue pulls in transitively:
- `Modules/Logging` (LoggerFactory)
- `kotlinx-coroutines-core`
- `kotlinx-serialization-json`
- `kotlinx-datetime`
- `okio 3.9.0`
- `bouncycastle 1.78.1` (Android + Desktop only, for TLS)

## 12. File Inventory

| Source Set | Package Count | File Count | Description |
|---|---|---|---|
| commonMain | 12 packages | 45 files | All shared logic |
| androidMain | 3 packages | 5 files | Socket (java.net), Sha1, gzip, time, resources |
| desktopMain | 3 packages | 5 files | Same as Android (JVM-based) |
| iosMain | 3 packages | 5 files | BSD sockets, CC_SHA1, zlib, NSDate, NSBundle |
| **Total** | | **68 files** | ~4,200 lines |

## 13. Relationship to Other Modules

```
HTTPAvanue (this module)
  ├── depends on: Modules/Logging (LoggerFactory)
  ├── used by (future): RemoteCast (replace raw TCP with HTTP/WebSocket)
  ├── used by (future): ConnectionAvanue (higher-level device communication)
  └── used by (future): AI modules (local REST API for on-device inference)
```

HTTPAvanue is a **pure library module** — no UI, no Compose dependency, no Android framework dependencies (except `android.content.res.AssetManager` for resource loading). It can be used in any KMP project.

---

**Next Steps:**
- HTTP/2 integration into HttpServer (auto-detect and branch)
- Huffman coding for HPACK
- iOS TLS via Network.framework
- ConnectionAvanue higher-level abstraction
