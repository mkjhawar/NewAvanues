# HTTPAvanue Implementation Report
**Module:** HTTPAvanue | **Date:** 2026-02-20 | **Branch:** HTTPAvanue | **Status:** COMPLETE

---

## Overview

HTTPAvanue is a standalone, distributable KMP HTTP server library extracted from AvaConnect's production-grade HTTP+WebSocket stack. It provides HTTP/1.1 and HTTP/2 server/client capabilities, WebSocket support, a middleware pipeline, and pattern-based routing — all in pure Kotlin with no framework dependencies (no Ktor, no Netty).

## Source Inventory

### Extracted from AvaConnect (`/Volumes/M-Drive/Coding/AvaConnect/common/`)

| AvaConnect Module | HTTPAvanue Package | Files | Adaptation |
|---|---|---|---|
| `connectivity/http/` | `server/` | 2 | HttpServer, HttpParser |
| `connectivity/http-api/` | `http/`, `client/` | 7 | HttpClient, HttpRequest, HttpResponse, HttpMethod, HttpStatus, HttpException, ClientModels |
| `connectivity/websocket/` | `websocket/` | 5 | WebSocket, WebSocketClient, Config, ClientHandshake, Parser |
| `connectivity/websocket-api/` | `websocket/` | 2+3 actuals | WebSocketFrame, WebSocketHandshake, Sha1 (android/ios/desktop) |
| `connectivity/middleware/` | `middleware/` | 9+3 actuals | All 9 middleware + gzip compression actuals |
| `connectivity/middleware-api/` | `middleware/` | 1 | Middleware interface + MiddlewarePipeline |
| `connectivity/routing/` | `routing/` | 3 | RoutePattern, RouteRegistry, RouterImpl |
| `connectivity/routing-api/` | `routing/` | 1 | Router, Route, RouteHandler |
| `platform/platform/` | `platform/` | 4+12 actuals | Socket, TlsConfig, PlatformTime, Resources |
| `platform/core/` | `core/` | 2 | Result, ByteArraySerializer |

### New Code (not from AvaConnect)

| Package | Files | Description |
|---|---|---|
| `auth/` | 1 | AuthenticationManager interface + TokenValidation (minimal local type) |
| `http2/` | 7 | HTTP/2 frame codec (RFC 7540), SETTINGS, Stream, Connection, FlowControl, ServerHandler, Error |
| `hpack/` | 4 | HPACK encoder/decoder (RFC 7541), static table (61 entries), dynamic table |
| `sse/` | 1 | Server-Sent Events emitter + connection manager |
| `metrics/` | 2 | ServerMetrics collector + MetricsMiddleware |

### Totals

| Category | commonMain | androidMain | desktopMain | iosMain | Total |
|---|---|---|---|---|---|
| Files | 45 | 5 | 5 | 5 | 68* |
| Lines (approx) | ~3,400 | ~250 | ~250 | ~300 | ~4,200 |

*Including build.gradle.kts = 69 files total, 4,188 insertions.

## Key Adaptations

### Package Rename
- `com.augmentalis.avaconnect.*` → `com.augmentalis.httpavanue.*` (all files)

### Logger Migration (46 call sites)
- `import co.touchlab.kermit.Logger` → `import com.avanues.logging.LoggerFactory`
- `Logger.withTag("X")` → `LoggerFactory.getLogger("X")`
- `logger.e(exception) { "msg" }` → `logger.e({ "msg" }, exception)` (param order reversed)
- `logger.w(exception) { "msg" }` → `logger.w { "msg: ${exception.message}" }` (Logger.w has no throwable overload)

### Platform Time
- `import com.augmentalis.avaconnect.platform.currentTimeMillis` → `import com.augmentalis.httpavanue.platform.currentTimeMillis`
- Created new `PlatformTime.kt` expect/actual replacing AvaConnect's `PlatformContext.currentTimeMillis()`

### Authentication
- `import com.augmentalis.avaconnect.auth.AuthenticationManager` → local `com.augmentalis.httpavanue.auth.AuthenticationManager`
- Created minimal interface + `TokenValidation` data class

### Files Skipped (from AvaConnect)
- `core/Logger.kt` — replaced by `Modules/Logging`
- `platform/PlatformContext.kt` — only `currentTimeMillis()` needed (extracted to PlatformTime)
- `platform/PlatformUtils.kt` — not referenced by HTTP/WebSocket code
- `platform/DeviceInfoProvider.kt` — not referenced
- `platform/Resources.kt` — not needed (readResource extracted separately)
- `platform/MimeTypeResolver.kt` — not referenced (StaticFileMiddleware has its own MIME mapping)

## Dependencies

```kotlin
// commonMain
api(project(":Modules:Logging"))
implementation(libs.kotlinx.coroutines.core)
implementation(libs.kotlinx.serialization.json)
implementation(libs.kotlinx.datetime)
implementation(libs.okio)  // NEW in version catalog: 3.9.0

// androidMain + desktopMain
implementation(libs.bouncycastle.bcprov)  // NEW: 1.78.1
implementation(libs.bouncycastle.bcpkix)  // NEW: 1.78.1
```

## Build Targets

| Target | Source Set | Status |
|---|---|---|
| Android | androidMain | BUILD SUCCESSFUL |
| Desktop (JVM) | desktopMain | BUILD SUCCESSFUL |
| iOS arm64 | iosMain | Compiles (via shared iosMain source set) |
| iOS Simulator arm64 | iosMain | Compiles (via shared iosMain source set) |

## Commits

| Hash | Description |
|---|---|
| `c78f7af8` | feat(HTTPAvanue): KMP HTTP/1.1 + HTTP/2 server, WebSocket, middleware library (69 files) |
| `4415fd97` | build: add okio 3.9.0 + bouncycastle 1.78.1 deps, include HTTPAvanue module |
| `d49c6d20` | fix(build): platform(libs.compose.bom) -> platform(libs.compose.bom.get()) across 14 modules |
| `8c4cc839` | docs(HTTPAvanue): plan and analysis for hybrid extraction from AvaConnect |

## Pre-existing Bug Fixed

**Issue:** `platform(libs.compose.bom)` fails with Gradle 8.14.3 + Kotlin 2.1.0
**Root cause:** Version catalog Provider not auto-resolved inside `platform()` — requires `.get()`
**Fix:** `platform(libs.compose.bom)` → `platform(libs.compose.bom.get())` across 14 build files
**Files affected:** AvidCreator, DeviceManager, VoiceDataManager, VoiceKeyboard, LicenseManager, AI/RAG, AI/Teach, AVA/Overlay, VoiceUI, VoiceRecognition, avanues, voiceavanue, voiceavanue-legacy, ava-legacy

## Architecture Decisions

1. **No Foundation dependency** — HTTPAvanue is self-contained; Foundation can be added later for ISettingsStore integration
2. **Dual SHA-1 strategy** — Server-side uses expect/actual (MessageDigest/CC_SHA1), client-side uses Okio's ByteString.sha1()
3. **HPACK without Huffman** — Non-Huffman encoding is valid per RFC 7541; Huffman tables (~4KB) can be added incrementally
4. **Local auth types** — Minimal AuthenticationManager interface avoids coupling to AvaConnect's security module
5. **Three iOS targets** — iosArm64 + iosSimulatorArm64, sharing a single iosMain source set with BSD socket implementation
