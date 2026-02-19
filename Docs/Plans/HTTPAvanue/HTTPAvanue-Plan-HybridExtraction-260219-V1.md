# HTTPAvanue: Hybrid Extraction Implementation Plan
**Date:** 2026-02-19 | **Version:** V1 | **Branch:** HTTPAvanue
**Module:** HTTPAvanue | **Type:** Plan
**Analysis:** `Docs/Analysis/HTTPAvanue/HTTPAvanue-Analysis-ThreePathEvaluation-260219-V1.md`

---

## Context

**Problem:** NewAvanues has no HTTP server infrastructure. RemoteCast uses raw TCP with a custom binary protocol (CAST frames on port 54321). This limits browser-based receivers, lacks HTTP debugging tools, and prevents standard protocol features (middleware, routing, auth, CORS).

**Solution:** Create `Modules/HTTPAvanue/` — a pure-Kotlin, Okio-based, KMP HTTP/1.1 + HTTP/2 + WebSocket server/client module. Extract proven code from AvaConnect's 13 clean connectivity modules, upgrade to Kotlin 2.1.0, add HTTP/2, and fully replace RemoteCast's TCP transport.

**Source:** AvaConnect at `/Volumes/M-Drive/Coding/AvaConnect/` has 13 clean connectivity modules (~7,600 LOC production + ~11,000 LOC tests). All production code compiles cleanly. Build failures are test-only (JVM APIs in commonTest).

**Decisions:**
- **Path C (Hybrid Extract)** — extract proven code, analyze/rebuild to conform to AvanueUI and UX goals
- **Full stack in Phase 1** — HTTP/1.1 + WebSocket + Middleware + HTTP/2
- **Full RemoteCast replacement** — all traffic (video + control) through HTTPAvanue
- **KMP First** — maximize commonMain, platform-specific only where required
- **Developer settings** via Unified Adaptive Settings (Hilt @IntoSet)
- **AvanueUI/SpatialVoice** for any UI surfaces

---

## Phase 1: Module Scaffold + Build System

### 1.1 Create Module Structure

```
Modules/HTTPAvanue/
  build.gradle.kts
  src/
    commonMain/kotlin/com/augmentalis/httpavanue/
      core/           # Result<T>, ByteArraySerializer
      platform/       # Socket (expect), SocketServer, TlsConfig, TimeProvider (NEW)
      http/           # HttpMethod, HttpRequest, HttpResponse, HttpStatus, HttpException, HttpClient interface
      server/         # HttpServer, HttpParser, ServerConfig
      client/         # RealHttpClient, ResponseParser, ConnectionPool
      websocket/      # WebSocketFrame, WebSocketHandshake, WebSocket, WebSocketClient, WebSocketParser
      middleware/     # Middleware interface + 9 implementations (Auth, CORS, Compression, etc.)
      routing/        # Router, RouterImpl, RoutePattern, RouteRegistry
      connection/     # ConnectionResilience, ResilienceTypes
      qos/            # AdaptiveQosManager, QualityController, battery/network types
      http2/          # NEW: Http2Frame, HPACK, streams, flow control
    androidMain/kotlin/com/augmentalis/httpavanue/
      platform/       # Socket.android.kt (actual, BouncyCastle TLS), Sha1.android.kt, TimeProvider
    desktopMain/kotlin/com/augmentalis/httpavanue/
      platform/       # Socket.desktop.kt (actual, JVM), Sha1.desktop.kt, TimeProvider
    iosMain/kotlin/com/augmentalis/httpavanue/
      platform/       # Socket.ios.kt (actual, NSStream), Sha1.ios.kt, TimeProvider
    commonTest/kotlin/com/augmentalis/httpavanue/
```

### 1.2 `settings.gradle.kts`
Add: `include(":Modules:HTTPAvanue")`

### 1.3 `gradle/libs.versions.toml` Additions

```toml
# [versions]
okio = "3.9.0"
bouncycastle = "1.78.1"

# [libraries]
okio = { module = "com.squareup.okio:okio", version.ref = "okio" }
bouncycastle-bcprov = { module = "org.bouncycastle:bcprov-jdk18on", version.ref = "bouncycastle" }
bouncycastle-bcpkix = { module = "org.bouncycastle:bcpkix-jdk18on", version.ref = "bouncycastle" }
```

### 1.4 `build.gradle.kts` Key Dependencies
```
commonMain:
  api(project(":Modules:Foundation"))        # ISettingsStore, IFileSystem, etc.
  api(project(":Modules:Logging"))           # LoggerFactory.getLogger()
  implementation(libs.kotlinx.coroutines.core)  # 1.8.1
  implementation(libs.kotlinx.serialization.json) # 1.6.0
  implementation(libs.okio)                       # 3.9.0

androidMain + desktopMain:
  implementation(libs.bouncycastle.bcprov)   # TLS support
  implementation(libs.bouncycastle.bcpkix)   # PEM parsing
```

KMP targets: androidTarget, jvm("desktop"), iOS (conditional on `enableNativeTargets`).
Compiler flags: `-Xexpect-actual-classes` (required for expect/actual classes in 2.1.0).

**Verify:** `./gradlew :Modules:HTTPAvanue:assemble`

---

## Phase 2: File Extraction + Package Rename

### Package Rename Map
All `com.augmentalis.avaconnect.*` → `com.augmentalis.httpavanue.*`

### Extraction by Module Group

#### Group 1: Core Foundation (~293 LOC)
**Source:** `AvaConnect/common/platform/core/src/commonMain/`

| File | Lines | Target Package | Action |
|------|-------|---------------|--------|
| `Result.kt` | 60 | `httpavanue.core` | Extract, rename package |
| `ByteArraySerializer.kt` | 41 | `httpavanue.core` | Extract, rename package |
| `Logger.kt` | 43 | — | **SKIP** — use `Modules/Logging` |

#### Group 2: Platform Sockets (~2,400 LOC, excluding skipped files)
**Source:** `AvaConnect/common/platform/platform/src/`

| File | Lines | Source Set | Action |
|------|-------|-----------|--------|
| `Socket.kt` | 106 | commonMain | Extract → `httpavanue.platform` (expect class) |
| `TlsConfig.kt` | 120 | commonMain | Extract |
| `MimeTypeResolver.kt` | 208 | commonMain | Extract |
| `Socket.android.kt` | 339 | androidMain | Extract (includes BouncyCastle TLS helper) |
| `Socket.ios.kt` | 308 | iosMain | Extract |
| `Socket.jvm.kt` | 339 | jvmMain → **desktopMain** | Extract + rename file to `.desktop.kt` |
| `PlatformContext.kt` | — | — | **SKIP** — Foundation has equivalents |
| `PlatformUtils.kt` | — | — | **SKIP** |
| `DeviceInfoProvider.kt` | — | — | **SKIP** |
| `Resources.kt` | — | — | **SKIP** |

**NEW: TimeProvider** (replaces AvaConnect's `currentTimeMillis` expect function):
```kotlin
// commonMain: expect fun currentTimeMillis(): Long
// androidMain/desktopMain: actual = System.currentTimeMillis()
// iosMain: actual = (NSDate.date().timeIntervalSince1970 * 1000).toLong()
```

#### Group 3: HTTP (~4,222 LOC)
**Source:** `AvaConnect/common/connectivity/http-api/` + `http/`

| File | Lines | Target Package | Action |
|------|-------|---------------|--------|
| `HttpClient.kt` (interface) | 112 | `httpavanue.http` | Extract |
| `HttpRequest.kt` | 70 | `httpavanue.http` | Extract |
| `HttpResponse.kt` | 155 | `httpavanue.http` | Extract |
| `HttpStatus.kt` | 44 | `httpavanue.http` | Extract |
| `HttpMethod.kt` | 23 | `httpavanue.http` | Extract |
| `HttpException.kt` | 134 | `httpavanue.http` | Extract (sealed class) |
| `ClientModels.kt` | 115 | `httpavanue.client` | Extract |
| `HttpServer.kt` | 312 | `httpavanue.server` | Extract, replace Kermit |
| `HttpParser.kt` | 244 | `httpavanue.server` | Extract |
| `RealHttpClient.kt` | 537 | `httpavanue.client` | Extract, replace Kermit |
| `ResponseParser.kt` | 211 | `httpavanue.client` | Extract |

#### Group 4: WebSocket (~2,559 LOC)
**Source:** `AvaConnect/common/connectivity/websocket-api/` + `websocket/`

| File | Lines | Target Package | Action |
|------|-------|---------------|--------|
| `WebSocketFrame.kt` | 171 | `httpavanue.websocket` | Extract |
| `WebSocketHandshake.kt` | 73 | `httpavanue.websocket` | Extract (contains `expect fun sha1()`) |
| `WebSocket.kt` | 410 | `httpavanue.websocket` | Extract, replace Kermit, uses `currentTimeMillis` |
| `WebSocketClient.kt` | 402 | `httpavanue.websocket` | Extract, replace Kermit |
| `WebSocketClientConfig.kt` | 120 | `httpavanue.websocket` | Extract |
| `WebSocketClientHandshake.kt` | 137 | `httpavanue.websocket` | Extract |
| `WebSocketParser.kt` | 183 | `httpavanue.websocket` | Extract |
| `Sha1.android.kt` | 11 | androidMain | Extract (MessageDigest SHA-1) |
| `Sha1.ios.kt` | 25 | iosMain | Extract (CommonCrypto CC_SHA1) |
| `Sha1.jvm.kt` | 11 | desktopMain | Extract + rename |

#### Group 5: Middleware (~3,264 LOC)
**Source:** `AvaConnect/common/connectivity/middleware-api/` + `middleware/`

| File | Lines | Target Package | Action |
|------|-------|---------------|--------|
| `Middleware.kt` (interface + pipeline) | 80 | `httpavanue.middleware` | Extract |
| `AuthenticationMiddleware.kt` | 96 | `httpavanue.middleware` | Extract, replace `auth-api` import with local `Credentials` type |
| `BodyParserMiddleware.kt` | 51 | `httpavanue.middleware` | Extract |
| `CompressionMiddleware.kt` | 229 | `httpavanue.middleware` | Extract (has expect/actual for compression) |
| `CorsMiddleware.kt` | 107 | `httpavanue.middleware` | Extract |
| `ErrorHandlerMiddleware.kt` | 97 | `httpavanue.middleware` | Extract, replace Kermit |
| `LoggerMiddleware.kt` | 30 | `httpavanue.middleware` | Extract, replace Kermit |
| `RateLimitMiddleware.kt` | 295 | `httpavanue.middleware` | Extract |
| `StaticFileMiddleware.kt` | 114 | `httpavanue.middleware` | Extract |
| `TimingMiddleware.kt` | 47 | `httpavanue.middleware` | Extract |
| Platform compression actuals | 27+135+27 | android/ios/desktop | Extract to platform source sets |

**Note:** `AuthenticationMiddleware` imports `com.augmentalis.avaconnect.security.auth` — create a minimal `Credentials` data class in `httpavanue.middleware` to replace.

#### Group 6: Routing (~2,273 LOC)
**Source:** `AvaConnect/common/connectivity/routing-api/` + `routing/`

| File | Lines | Target Package | Action |
|------|-------|---------------|--------|
| `Router.kt` | 137 | `httpavanue.routing` | Extract (interface + Route DSL) |
| `RouterImpl.kt` | 182 | `httpavanue.routing` | Extract |
| `RouteRegistry.kt` | 195 | `httpavanue.routing` | Extract |
| `RoutePattern.kt` | 143 | `httpavanue.routing` | Extract |

#### Group 7: Connection Resilience (~1,035 LOC)
**Source:** `AvaConnect/common/connectivity/connection-api/` + `connection/`

| File | Lines | Target Package | Action |
|------|-------|---------------|--------|
| `ResilienceTypes.kt` | 48 | `httpavanue.connection` | Extract |
| `ConnectionResilience.kt` | 228 | `httpavanue.connection` | Extract, replace Kermit, uses `currentTimeMillis` |

#### Group 8: QoS (~1,050 LOC, selective)
**Source:** `AvaConnect/common/platform/qos-api/` + `qos/`

| File | Lines | Target Package | Action |
|------|-------|---------------|--------|
| `BatteryMonitor.kt` (expect) | ~50 | `httpavanue.qos` | Extract |
| `BatteryState.kt` | ~30 | `httpavanue.qos` | Extract |
| `NetworkQuality.kt` | ~40 | `httpavanue.qos` | Extract |
| `QualityProfile.kt` | ~60 | `httpavanue.qos` | Extract |
| `AdaptiveQoSManager.kt` | ~200 | `httpavanue.qos` | Extract, replace Kermit |
| `QualityController.kt` | ~150 | `httpavanue.qos` | Extract, replace Kermit |
| `BatteryMonitor.{android,ios,jvm}.kt` | ~60ea | Platform sets | Extract |
| `*VideoQualityApplier.kt` | — | — | **SKIP** — WebRTC specific |
| `WebRTCNetworkMonitor.kt` | — | — | **SKIP** — WebRTC specific |

**Verify:** `./gradlew :Modules:HTTPAvanue:compileKotlinDesktop` — zero unresolved references

---

## Phase 3: Kermit → Logging Adaptation

### Pattern Change (~46 call sites across 9 files)
```kotlin
// BEFORE (AvaConnect — Kermit)
import co.touchlab.kermit.Logger
private val logger = Logger.withTag("HttpServer")
logger.d { "message" }
logger.e(exception) { "message" }

// AFTER (NewAvanues — Logging module)
import com.avanues.logging.LoggerFactory
private val logger = LoggerFactory.getLogger("HttpServer")
logger.d { "message" }
logger.e({ "message" }, exception)   // CRITICAL: parameter order REVERSED for .e()
```

**Files needing Kermit replacement:**
| File | ~Call Sites |
|------|------------|
| `HttpServer.kt` | 8 |
| `WebSocket.kt` | 12 |
| `WebSocketClient.kt` | 6 |
| `RealHttpClient.kt` | 4 |
| `ConnectionResilience.kt` | 5 |
| `ErrorHandlerMiddleware.kt` | 2 |
| `LoggerMiddleware.kt` | 2 |
| `AdaptiveQosManager.kt` | 4 |
| `QualityController.kt` | 3 |

### Kotlin 2.1.0 Adaptations
- `-Xexpect-actual-classes` compiler flag (in build.gradle.kts template)
- `@OptIn(ExperimentalEncodingApi::class)` on WebSocketHandshake — keep as-is
- Verify `@Serializable(with = ByteArraySerializer::class)` explicit annotation on `ByteArray?` fields

**Verify:** `./gradlew :Modules:HTTPAvanue:compileDebugKotlin` — clean compile on Android target

---

## Phase 4: HTTP/2 Frame Layer (New Code)

All new code in `commonMain/.../httpavanue/http2/`. Estimated ~2,000-3,000 LOC.

### Files to Create

| File | Purpose | LOC est. |
|------|---------|----------|
| `Http2FrameType.kt` | 10 frame type enum (DATA, HEADERS, PRIORITY, RST_STREAM, SETTINGS, PUSH_PROMISE, PING, GOAWAY, WINDOW_UPDATE, CONTINUATION) | ~30 |
| `Http2Frame.kt` | 9-byte fixed header + payload model; Okio read/write | ~100 |
| `Http2Settings.kt` | Settings identifiers (RFC 7540 Section 6.5) + defaults | ~50 |
| `Http2Error.kt` | Error codes enum (NO_ERROR through HTTP_1_1_REQUIRED) | ~40 |
| `HpackTable.kt` | Static table (61 entries, RFC 7541 Appendix A) + dynamic table (ArrayDeque, 4096 byte default) | ~200 |
| `HpackDecoder.kt` | Indexed/literal header decode, integer decode, dynamic table update | ~300 |
| `HpackEncoder.kt` | Static lookup + dynamic insert, integer encode (Huffman deferred to V2) | ~200 |
| `Http2Stream.kt` | Stream state machine (IDLE→OPEN→HALF_CLOSED→CLOSED), data channel, window sizes | ~150 |
| `Http2Connection.kt` | Connection preface, SETTINGS exchange, frame dispatch loop, PING/GOAWAY handling | ~500 |
| `Http2FlowControl.kt` | Connection-level + stream-level window management (default 65535) | ~100 |
| `Http2ServerHandler.kt` | Integration: detect `Upgrade: h2c` header in HttpServer, branch into HTTP/2 | ~200 |

### Connection Upgrade: h2c (Cleartext, Phase 1)
Client sends `Upgrade: h2c` + `HTTP2-Settings` header → server responds `101 Switching Protocols` → HTTP/2 framing begins. Modify `HttpServer.handleConnection()` to detect upgrade header and delegate to `Http2Connection`.

**ALPN (TLS) deferred** — local network (RemoteCast LAN) doesn't need TLS for Phase 1.

### HTTP/2 Frame Format (RFC 7540 Section 4.1)
```
+-----------------------------------------------+
|                 Length (24)                    |
+---------------+---------------+---------------+
|   Type (8)    |   Flags (8)   |
+-+-------------+---------------+
|R|                 Stream Identifier (31)      |
+=+=============+===============================+
|                   Frame Payload (0...)        |
+-----------------------------------------------+
```

**Verify:** `./gradlew :Modules:HTTPAvanue:desktopTest` — HTTP/2 frame round-trip + HPACK encode/decode tests pass

---

## Phase 5: RemoteCast Full Replacement

### New Cast Layer in HTTPAvanue
Create `commonMain/.../httpavanue/cast/`:

| File | Purpose |
|------|---------|
| `CastHttpServer.kt` | HTTP server wrapping HttpServer — routes for `/cast/stream`, `/cast/status`, `/cast/ws` |
| `MjpegHttpStreamer.kt` | `multipart/x-mixed-replace` streaming — boundary + Content-Type + Content-Length + JPEG bytes per frame |
| `CastWebSocketChannel.kt` | WebSocket handler for VOC (vocabulary sync) + CMD (commands) + settings push |

### MJPEG-over-HTTP Protocol
```http
GET /cast/stream HTTP/1.1

HTTP/1.1 200 OK
Content-Type: multipart/x-mixed-replace; boundary=--FRAME

--FRAME
Content-Type: image/jpeg
Content-Length: <N>

<JPEG bytes>
--FRAME
Content-Type: image/jpeg
Content-Length: <N>

<JPEG bytes>
...
```

Browser receiver: `<img src="http://phone:54321/cast/stream">` — works natively in all browsers.

### Endpoint Map
| Old (TCP) | New (HTTP) | Protocol |
|-----------|-----------|----------|
| Port 54321, CAST binary frames | `GET /cast/stream` | MJPEG multipart |
| N/A | `GET /cast/status` | REST JSON |
| N/A | `WS /cast/ws` | WebSocket (VOC+CMD bidirectional) |
| N/A | `POST /cast/settings` | REST JSON |

### RemoteCast Module Changes
**Note:** Per CLAUDE.md Rule #1, RemoteCast transport changes require user permission. This modifies transport layer only, NOT the scraping system.

**Modify:**
- `MjpegTcpServer.kt` → delegate to `CastHttpServer` from HTTPAvanue
- `MjpegTcpClient.kt` → delegate to HTTP client `GET /cast/stream`
- `build.gradle.kts` → add `api(project(":Modules:HTTPAvanue"))` to commonMain dependencies

**DO NOT modify (unchanged):**
- `ICastManager.kt` — interface unchanged
- `AndroidCastManager.kt` — orchestration unchanged (calls same start/stop/sendFrame API)
- `CastFrameData.kt` — keep for backward compat reference

**Verify:** `./gradlew :Modules:RemoteCast:compileDebugKotlin :Modules:RemoteCast:compileKotlinDesktop`

---

## Phase 6: Developer Settings Screen

### New File: `HTTPAvanueSettingsProvider.kt`
**Location:** `apps/avanues/src/main/kotlin/com/augmentalis/voiceavanue/ui/settings/providers/HTTPAvanueSettingsProvider.kt`

```
moduleId = "httpavanue"
displayName = "HTTP Server"
sortOrder = 600  (after System=500)
```

### Three Sections

**Server Section (id="server"):**
| Setting | Type | Default | AVID |
|---------|------|---------|------|
| Enable Server | Toggle | OFF | `SWT:httpavanue_server_enabled` |
| Port | Number Input | 8080 | `INP:httpavanue_server_port` |
| Enable Logging | Toggle | OFF | `SWT:httpavanue_server_logging` |
| Server Address | Info (read-only) | — | — |

**Cast Section (id="cast"):**
| Setting | Type | Default | AVID |
|---------|------|---------|------|
| MJPEG Streaming | Toggle | OFF | `SWT:httpavanue_cast_enabled` |
| Cast Port | Number Input | 54321 | `INP:httpavanue_cast_port` |
| Quality | Select (480p/720p/1080p) | 720p | `SEL:httpavanue_cast_quality` |
| Max FPS | Number Input (1-60) | 30 | `INP:httpavanue_cast_fps` |

**Security Section (id="security"):**
| Setting | Type | Default | AVID |
|---------|------|---------|------|
| Enable TLS (HTTPS) | Toggle | OFF | `SWT:httpavanue_tls_enabled` |
| Allow Self-Signed Certs | Toggle (dev only) | ON | `SWT:httpavanue_tls_selfsigned` |
| Generate Self-Signed Cert | Button | — | `BTN:httpavanue_tls_generate` |

### UI Requirements (MANDATORY)
- All components use `AvanueTheme.colors.*` (NEVER `MaterialTheme.colorScheme.*`)
- Section containers: `AvanueCard`
- Buttons: `AvanueButton`
- AVID voice identifiers on ALL interactive elements
- SpatialVoice design: vertical gradient background, glass surface containers

### Hilt Registration
Add to `apps/avanues/src/main/kotlin/com/augmentalis/voiceavanue/di/SettingsModule.kt`:
```kotlin
@Provides @IntoSet
fun provideHTTPAvanueSettings(): ComposableSettingsProvider = HTTPAvanueSettingsProvider()
```

**Verify:** `./gradlew :apps:avanues:assembleDebug`

---

## Phase 7: Testing

### Port from AvaConnect (Fix JVM-Only Issues)
| Issue | Fix |
|-------|-----|
| `synchronized {}` in commonTest | Replace with `Mutex` + `withLock {}` |
| Backtick test names with `()` | Rename (illegal in Kotlin/Native) |
| `toByteArray()` type mismatch | Use `.encodeToByteArray()` explicitly |
| `MAX_CHUNK_SIZE` unresolved | Use `FileChunker.MAX_CHUNK_SIZE` qualified reference |

### New Tests for HTTP/2
| Test File | Coverage |
|-----------|----------|
| `Http2FrameTest.kt` | Encode/decode round-trip for all 10 frame types |
| `HpackEncoderDecoderTest.kt` | Static table, dynamic table, integer encoding edge cases |
| `Http2ConnectionTest.kt` | SETTINGS exchange, PING/PONG, stream lifecycle |
| `Http2FlowControlTest.kt` | Window depletion, WINDOW_UPDATE handling |

### Integration Test
`CastHttpServerTest.kt` — start server on port 0 (OS-assigned), send frame, verify MJPEG multipart received. Place in `desktopTest` (JVM, fastest).

**Verify:** `./gradlew :Modules:HTTPAvanue:desktopTest`

---

## Critical Files Reference

| Purpose | Path |
|---------|------|
| AvaConnect HttpServer (extract) | `/Volumes/M-Drive/Coding/AvaConnect/common/connectivity/http/src/commonMain/.../server/HttpServer.kt` |
| AvaConnect Socket expect | `/Volumes/M-Drive/Coding/AvaConnect/common/platform/platform/src/commonMain/.../platform/Socket.kt` |
| AvaConnect Socket android actual | `/Volumes/M-Drive/Coding/AvaConnect/common/platform/platform/src/androidMain/.../platform/Socket.android.kt` |
| NewAvanues Logging module | `/Volumes/M-Drive/Coding/NewAvanues/Modules/Logging/` (LoggerFactory.getLogger) |
| NewAvanues Foundation module | `/Volumes/M-Drive/Coding/NewAvanues/Modules/Foundation/src/commonMain/` |
| RemoteCast transport (replace) | `/Volumes/M-Drive/Coding/NewAvanues/Modules/RemoteCast/src/androidMain/.../transport/` |
| Settings pattern (extend) | `/Volumes/M-Drive/Coding/NewAvanues/apps/avanues/src/main/.../di/SettingsModule.kt` |
| Version catalog (update) | `/Volumes/M-Drive/Coding/NewAvanues/gradle/libs.versions.toml` |
| Module includes (update) | `/Volumes/M-Drive/Coding/NewAvanues/settings.gradle.kts` |

## Reuse — Do NOT Reimplement

| What | Where | Why |
|------|-------|-----|
| `LoggerFactory.getLogger()` | `Modules/Logging` | Cross-platform logging |
| `ISettingsStore<T>` | `Modules/Foundation` | Settings persistence |
| `ComposableSettingsProvider` | `apps/avanues/.../settings/` | Settings UI integration |
| `AvanueCard`, `AvanueButton` | `Modules/AvanueUI` | UI components |
| `AvanueTheme.colors.*` | `Modules/AvanueUI` | Theme tokens |
| `CastFrameData` format knowledge | `Modules/RemoteCast` | Protocol understanding |

---

## Execution Sequence

```
Phase 1 (scaffold + build)  ─┐
Phase 2 (extraction)         ├── Session 1 (~4-6h)
Phase 3 (Kermit→Logging)    ─┘
         ↓ compile gate
Phase 4 (HTTP/2 frame layer) ── Session 2 (~6-8h)
Phase 5 (RemoteCast replace)  ── Session 2-3 (~4-6h)
         ↓ both complete
Phase 6 (Dev Settings)       ─┐
Phase 7 (Tests)              ─┤ Session 3 (~3-4h)
                              ─┘
```

**Total estimated:** 3 sessions, ~17-24h of work

## Full Verification
```bash
./gradlew :Modules:HTTPAvanue:check \
          :Modules:RemoteCast:check \
          :apps:avanues:assembleDebug
```

---

## Documentation Deliverables
- This plan: `docs/plans/HTTPAvanue/HTTPAvanue-Plan-HybridExtraction-260219-V1.md`
- Analysis: `Docs/Analysis/HTTPAvanue/HTTPAvanue-Analysis-ThreePathEvaluation-260219-V1.md` (done)
- Developer Manual Chapter 101: HTTPAvanue KMP HTTP Server Module (after implementation)
- Update Chapter 97 (Cockpit) with HTTP transport notes (after RemoteCast integration)
