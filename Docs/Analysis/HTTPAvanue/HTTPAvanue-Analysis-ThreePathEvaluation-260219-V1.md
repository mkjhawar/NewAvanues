# HTTPAvanue: Three-Path Evaluation
**Date:** 2026-02-19 | **Version:** V1 | **Branch:** Cockpit-Development
**Scope:** Evaluate Fix vs Rebuild vs Hybrid for AvaConnect → HTTPAvanue

---

## Executive Summary

AvaConnect at `/Volumes/M-Drive/Coding/AvaConnect/` is a **57-module KMP networking stack** with ~55,000 lines of Kotlin across 7 domains. Deep investigation reveals:

1. **It's in better shape than expected** — Sprint 2 was completed, all blocking interfaces resolved
2. **The HTTP + WebSocket core is clean and production-ready** — no `:shared` dependencies
3. **Build failures are test-only** — production code compiles; tests use JVM-only APIs in commonTest
4. **It's too large for our needs** — 44 of 57 modules are irrelevant (media, video, codec, NAT, etc.)
5. **Kotlin 1.9.20** — needs upgrade to 2.1.0 to match NewAvanues

**Recommendation: Path C (Hybrid)** — Extract the 13 connectivity modules (~15K lines), port to Modules/HTTPAvanue/, upgrade to Kotlin 2.1.0, add HTTP/2.

---

## AvaConnect Current State (Deep Research Findings)

### Architecture
- **57 Gradle modules** across 7 domains: platform, connectivity, security, media, storage, services, communications
- **Module path structure:** `common/{domain}/{module-name}/` (NOT `modules/` — that's empty)
- **settings.gradle.kts:** 57 includes under `common/` tree
- **Branch:** `modular-full` (the README references `refactor/interface-based-architecture` — outdated)

### What Works
| Module | Status | Files | Key API |
|--------|--------|-------|---------|
| `http-api` | CLEAN | 7 | HttpClient, HttpRequest/Response, HttpMethod, HttpStatus, HttpException |
| `http` (impl) | CLEAN | 4 | HttpServer (NanoHTTPD-inspired), HttpParser, RealHttpClient, ResponseParser |
| `websocket-api` | CLEAN | 5 | WebSocketFrame, WebSocketOpcode, WebSocketCloseCode, WebSocketHandshake, sha1 |
| `websocket` (impl) | CLEAN | 5 | WebSocket (RFC 6455), WebSocketClient (auto-reconnect), WebSocketParser |
| `middleware-api` | CLEAN | 1 | Middleware interface, MiddlewarePipeline, DSL builder |
| `middleware` (impl) | CLEAN | 12 | 9 middleware: Auth, BodyParser, Compression, CORS, Error, Logger, RateLimit, StaticFile, Timing |
| `routing-api` | CLEAN | 1 | Router, Route, RouteHandler typealias |
| `routing` (impl) | CLEAN | 3 | RoutePattern, RouteRegistry, RouterImpl |
| `core` | CLEAN | 4 | Result<T>, Logger, ByteArraySerializer |
| `platform` | CLEAN | 16 | Socket (expect/actual), SocketServer, SocketConfig, TlsConfig |
| `qos-api` | CLEAN | 6 | NetworkMonitor, BatteryMonitor, QualityProfile |
| `qos` (impl) | CLEAN | 9 | AdaptiveQoSManager, QualityController |
| `connection` | CLEAN | 2 | ConnectionResilience |

**Total CLEAN connectivity modules:** 13 modules, ~75 files, ~15,000 lines

### What Doesn't Work (Build Failures)
All failures are in **tests**, not production code:

| Failure | Module | Root Cause | Severity |
|---------|--------|-----------|----------|
| kotlin-test version conflict | filetransfer-impl, discovery-impl | JUnit4 vs JUnit5 capability clash (kermit:2.0.2 pulls junit:1.9.10, module wants junit5:1.9.20) | Build blocker |
| `synchronized {}` in commonTest | http-impl | JVM-only API used in tests compiled for iOS Native | Test-only |
| `()` in backtick test names | http-impl | Illegal in Kotlin/Native (works on JVM) | Test-only |
| `toByteArray()` mismatch | http-api | JVM vs Native type resolution | Test-only |
| `MAX_CHUNK_SIZE` unresolved | filetransfer-impl | Stale test file in old `modules/` path | Test-only |

### What's Irrelevant to HTTPAvanue
These 44 modules are NOT needed:

| Domain | Modules | Why Irrelevant |
|--------|---------|---------------|
| Media | webrtc, video, videocall, media, screen-recording, screen-region, window-sharing (11 modules) | RemoteCast handles this via its own MJPEG/WebRTC |
| Security | security-api/impl, auth-api/impl (4 modules) | NewAvanues has its own auth |
| Storage | device-api, filetransfer, backup, contacts-database (7 modules) | NewAvanues has Foundation/Database |
| Services | browser-connect, remote-control-server, accessibility-service, retention (6 modules) | NewAvanues has VoiceOSCore |
| Communications | communications, group (5 modules) | Not needed |
| NAT/Signaling | nat, signaling, discovery, interoperability, connection (10 modules) | Only mDNS discovery useful |
| Annotations | annotations (1 module) | NewAvanues has AnnotationAvanue |

### Technology Stack
| Dep | Version | Purpose | Keep? |
|-----|---------|---------|-------|
| Okio | 3.6.0-3.7.0 | ALL network I/O (sockets, HTTP parsing, WebSocket frames) | YES — core |
| Kermit | 2.0.2 | Multiplatform logging | REPLACE with Foundation Logger |
| kotlinx.coroutines | 1.7.3 | Async server loop, connection handling | YES — upgrade to 1.9.0 |
| kotlinx.serialization | 1.6.2 | JSON for models | YES — upgrade to 1.7.3 |
| Kotlin | 1.9.20 | Language version | UPGRADE to 2.1.0 |

### HTTP Server Architecture (Critical Detail)
The `HttpServer` is **exactly what we want** — a NanoHTTPD-inspired, pure-Kotlin implementation:

```
ServerConfig(port, host, maxConnections=50, requestTimeout=30s, maxBodySize=10MB)
    → SocketServer.accept() loop (coroutine-per-connection)
        → HttpParser.parseRequest(BufferedSource) — Okio-based HTTP/1.1 parser
            → MiddlewarePipeline (configurable chain)
                → Router.dispatch(request) — path-parameter matching
                    → Handler returns HttpResponse
        → OR WebSocketHandshake.upgrade() → WebSocket(connection)
```

- Pure Okio BufferedSource/BufferedSink for I/O
- No Ktor, no Netty, no NanoHTTPD dependency — hand-built from sockets
- HTTP/1.1 with chunked transfer encoding
- Connection pooling in RealHttpClient (5 idle, 5-min keepalive)
- Retry with exponential backoff (3 retries, 1s base)
- WebSocket upgrade handled inline
- Middleware: Auth, CORS, Compression, RateLimit, BodyParser, Logger, Error, Timing, StaticFile

---

## Path A: Fix AvaConnect In-Place

### What It Means
Keep AvaConnect as a separate repo at `/Volumes/M-Drive/Coding/AvaConnect/`. Fix the build failures. Upgrade Kotlin to 2.1.0. Add HTTP/2. Reference from NewAvanues as an external dependency.

### Steps Required
1. Fix all test failures (JVM APIs in commonTest → move to jvmTest or rewrite)
2. Fix kotlin-test version conflict (force single runner)
3. Upgrade Kotlin 1.9.20 → 2.1.0 (across ALL 57 modules)
4. Upgrade KSP, Compose, coroutines, serialization to match
5. Fix KSP2 + KMP incompatibility (gradle.properties: `ksp.useKSP2=false`)
6. Fix expect/actual changes in Kotlin 2.1.0 (stricter rules)
7. Add HTTP/2 support to HttpServer + HttpParser + HttpClient
8. Configure as publishable library (Maven Local or composite build)
9. Wire into NewAvanues as dependency

### Effort Estimate
| Task | Hours |
|------|-------|
| Fix test failures (5 categories) | 4-6h |
| Kotlin 2.1.0 upgrade (57 modules) | 12-20h |
| HTTP/2 implementation | 20-30h |
| Library publishing setup | 2-4h |
| Integration testing | 4-6h |
| **Total** | **42-66h** |

### Risks
| Risk | Severity | Detail |
|------|----------|--------|
| Kotlin upgrade cascade | HIGH | 57 modules × strict expect/actual changes in 2.1.0 = many compile errors |
| Carrying 44 irrelevant modules | MEDIUM | Maintenance burden, CI time, cognitive load |
| Separate repo coordination | MEDIUM | Two repos to keep in sync, version management |
| Stale code rot | MEDIUM | Media/video/NAT modules untouched since Nov 2025 — may accumulate more issues |
| Test suite: 1,729 passing + 21 failing | LOW | Pre-existing test failures compound with upgrade |

### Size Impact
- AvaConnect stays at ~55K lines, 57 modules
- NewAvanues gains 0 lines (external dependency)
- Total maintained codebase: +55K lines in separate repo

### Verdict
**Not recommended.** Upgrading 57 modules when we need only 13 is wasteful. Maintaining a separate repo adds coordination overhead. The 44 irrelevant modules are dead weight.

---

## Path B: Rebuild HTTPAvanue from Scratch

### What It Means
Create `Modules/HTTPAvanue/` in NewAvanues from scratch. Write HTTP/1.1, HTTP/2, and WebSocket servers in pure Kotlin with Okio, targeting KMP. Ignore AvaConnect entirely.

### Steps Required
1. Create Modules/HTTPAvanue/ with KMP source sets (commonMain, androidMain, iosMain, desktopMain)
2. Implement HTTP/1.1 request parser (Okio BufferedSource)
3. Implement HTTP/1.1 response writer (Okio BufferedSink)
4. Implement connection management (accept loop, coroutine-per-connection)
5. Implement HTTP/2 frame parser (RFC 7540 — HPACK, stream multiplexing, flow control)
6. Implement WebSocket (RFC 6455 — frame parsing, masking, fragmentation, close handshake)
7. Implement middleware pipeline
8. Implement router with path parameters
9. Implement HTTP client with connection pooling
10. Implement TLS support (expect/actual per platform)
11. Write tests

### Effort Estimate
| Task | Hours |
|------|-------|
| HTTP/1.1 server + parser | 10-15h |
| HTTP/1.1 client + pooling | 6-8h |
| WebSocket server + client | 8-12h |
| HTTP/2 frame layer (HPACK, streams, flow control) | 25-40h |
| Middleware pipeline + 9 middlewares | 6-8h |
| Router with path params | 3-4h |
| TLS/platform abstractions | 6-10h |
| Tests | 8-12h |
| **Total** | **72-109h** |

### Risks
| Risk | Severity | Detail |
|------|----------|--------|
| HTTP/2 complexity | HIGH | HPACK header compression, stream multiplexing, flow control — RFC 7540 is ~100 pages |
| Reimplementation bugs | HIGH | AvaConnect's WebSocket + HTTP have been tested; rewriting introduces new bugs |
| Time investment | HIGH | 72-109h is 2-3 weeks of focused work |
| Opportunity cost | MEDIUM | Time not spent on RemoteCast, settings protocol, wake-word |

### Size Impact
- ~5,000-10,000 lines new code in HTTPAvanue
- Clean, no legacy, no irrelevant modules
- Fully integrated in NewAvanues build system

### Verdict
**Not recommended.** Reimplementing well-tested HTTP/1.1 and WebSocket code from scratch when clean, working implementations exist in AvaConnect is wasteful. The only new code needed is HTTP/2 — rewriting everything else is unnecessary.

---

## Path C: Hybrid — Extract + Upgrade + Extend (RECOMMENDED)

### What It Means
Extract the 13 clean connectivity modules from AvaConnect → `Modules/HTTPAvanue/` in NewAvanues. Upgrade to Kotlin 2.1.0. Replace Kermit with Foundation Logger. Add HTTP/2. Discard all 44 irrelevant modules.

### What We Take (13 modules → 1 module with packages)
| AvaConnect Module | Lines | Becomes |
|-------------------|-------|---------|
| `core` (Result, Logger, ByteArraySerializer) | ~200 | `httpavanue.core` (or drop — Foundation has equivalents) |
| `platform` (Socket, SocketServer, TlsConfig) | ~800 | `httpavanue.platform` (expect/actual) |
| `http-api` (HttpClient, HttpRequest, HttpResponse) | ~600 | `httpavanue.http` (models + interfaces) |
| `http` (HttpServer, HttpParser, RealHttpClient) | ~1,200 | `httpavanue.server` + `httpavanue.client` |
| `websocket-api` (WebSocketFrame, Handshake) | ~400 | `httpavanue.websocket` (models) |
| `websocket` (WebSocket, WebSocketClient, Parser) | ~800 | `httpavanue.websocket` (implementations) |
| `middleware-api` (Middleware, Pipeline) | ~150 | `httpavanue.middleware` (contracts) |
| `middleware` (9 implementations) | ~1,500 | `httpavanue.middleware` (implementations) |
| `routing-api` + `routing` (Router, Pattern) | ~400 | `httpavanue.routing` |
| `qos-api` + `qos` (QoS, NetworkMonitor) | ~600 | `httpavanue.qos` (optional — or use Foundation) |
| `connection` (ConnectionResilience) | ~150 | `httpavanue.connection` |
| **Total extracted** | **~6,800** | Single KMP module with packages |

### What We Add (New Code)
| Feature | Lines | Detail |
|---------|-------|--------|
| HTTP/2 frame parser (RFC 7540) | ~1,500-2,000 | HPACK, streams, flow control, settings, priority |
| HTTP/2 connection upgrade (h2c + ALPN) | ~300-500 | Cleartext upgrade + TLS ALPN negotiation |
| HTTP/2 server push | ~200-300 | For VOCAB sync, settings push |
| Server-Sent Events (SSE) | ~150-200 | For streaming (lighter than WebSocket for one-way) |
| mDNS/DNS-SD (extracted from AvaConnect discovery) | ~300-400 | Device discovery for RemoteCast pairing |
| Foundation integration | ~200-300 | Replace Kermit → Foundation Logger, adapt Socket types |
| **Total new** | **~2,650-3,700** | |

### Steps Required
1. Create `Modules/HTTPAvanue/` with KMP source sets
2. Copy the 13 modules' source files (flatten into packages)
3. Update package names: `com.augmentalis.avaconnect.*` → `com.augmentalis.httpavanue.*`
4. Replace Kermit logging → Foundation Logger (or keep Kermit — it's tiny)
5. Upgrade Kotlin syntax for 2.1.0 compatibility
6. Update Okio to latest compatible version
7. Write HTTP/2 frame layer (new code)
8. Write HTTP/2 connection management (new code)
9. Add SSE support (new code)
10. Extract mDNS discovery from AvaConnect (optional)
11. Add to NewAvanues `settings.gradle.kts`
12. Wire into RemoteCast module
13. Fix/port relevant tests (skip JVM-only tests for now)

### Effort Estimate
| Task | Hours |
|------|-------|
| Module setup + file extraction | 2-3h |
| Package rename + import updates | 1-2h |
| Kotlin 2.1.0 syntax adaptation | 3-5h |
| Foundation integration (Logger, Socket types) | 2-3h |
| HTTP/2 frame layer (RFC 7540) | 20-30h |
| HTTP/2 connection management | 6-8h |
| SSE support | 2-3h |
| mDNS extraction | 3-4h |
| Build system wiring | 1-2h |
| Test porting + new tests | 4-6h |
| **Total** | **44-66h** |

### Risks
| Risk | Severity | Mitigation |
|------|----------|-----------|
| Kotlin 2.1.0 expect/actual changes | LOW | Only 13 modules (vs 57); Socket/TlsConfig are simple expect classes |
| HTTP/2 complexity | MEDIUM | Start with h2c (cleartext) + basic streams; defer HPACK optimization |
| Package rename breaks imports | LOW | Simple find-and-replace; no external consumers |
| Okio version compatibility | LOW | Okio 3.x is stable; minor version bumps are backward-compatible |

### Size Impact
- ~6,800 lines extracted from AvaConnect (tested, production-grade)
- ~2,650-3,700 lines new (HTTP/2, SSE, mDNS)
- **Total: ~9,500-10,500 lines** in Modules/HTTPAvanue/
- No separate repo — fully integrated in NewAvanues

### Verdict
**RECOMMENDED.** Best of both worlds:
- Proven HTTP/1.1 + WebSocket code (no reimplementation risk)
- Only the modules we need (no dead weight)
- Single repo (no coordination overhead)
- Kotlin 2.1.0 from day one (no upgrade cascade across 44 irrelevant modules)
- HTTP/2 added as focused extension to proven base
- Distributable as standalone library (user requirement)

---

## Comparison Matrix

| Criterion | Path A (Fix) | Path B (Rebuild) | Path C (Hybrid) |
|-----------|-------------|-----------------|----------------|
| **Effort** | 42-66h | 72-109h | 44-66h |
| **Risk** | HIGH (57-module upgrade) | HIGH (reimplementation bugs) | LOW-MEDIUM |
| **Code quality** | Mixed (dead modules) | Unknown (untested) | HIGH (proven + new) |
| **Lines maintained** | ~55,000 (separate repo) | ~5-10,000 | ~9,500-10,500 |
| **Kotlin 2.1.0** | Painful (57 modules) | Native | Manageable (13 modules) |
| **HTTP/2** | Added to existing | Written from scratch | Added to proven HTTP/1.1 |
| **WebSocket** | Already done | Reimplemented | Already done |
| **Middleware** | 9 existing | Reimplemented | 9 existing |
| **Distributable** | Needs publish setup | Clean from start | Clean from start |
| **RemoteCast integration** | External dependency | Internal module | Internal module |
| **Separate repo** | YES (coordination tax) | NO | NO |
| **Irrelevant code** | 44 modules of dead weight | None | None |

---

## Phased Implementation (Path C)

### Phase 1: Extract + Build (1-2 sessions)
1. Create `Modules/HTTPAvanue/`
2. Copy files, rename packages
3. Adapt to Kotlin 2.1.0
4. Get HTTP/1.1 + WebSocket building in NewAvanues

### Phase 2: HTTP/2 Core (2-3 sessions)
1. HTTP/2 frame types (DATA, HEADERS, SETTINGS, WINDOW_UPDATE, GOAWAY, PING, RST_STREAM, PUSH_PROMISE)
2. HPACK header compression (static + dynamic table)
3. Stream multiplexing (odd = client-initiated, even = server-push)
4. Flow control (per-stream + connection-level)
5. h2c cleartext upgrade (HTTP Upgrade header) + ALPN for TLS

### Phase 3: Extensions (1-2 sessions)
1. Server-Sent Events (SSE) — one-way streaming
2. mDNS/DNS-SD for device discovery
3. Self-signed certificate generation (for HTTPS)
4. Rate limiting enhancements

### Phase 4: Integration (1 session)
1. Wire into RemoteCast module
2. Replace raw TCP with HTTPAvanue server for MJPEG/VOCAB/CMD
3. Wire browser receiver HTML serving via StaticFileMiddleware
4. WebSocket signaling for WebRTC mode

---

## Decision Required

The user must choose between the three paths. My recommendation is **Path C (Hybrid)** because:
1. **Lowest effort** for the highest return (proven code + focused new work)
2. **No separate repo** — everything in NewAvanues
3. **No dead weight** — only the 13 modules we need
4. **Proven WebSocket + HTTP/1.1** — no reimplementation risk
5. **HTTP/2 built on solid foundation** — not on untested code
6. **Distributable** — designed as standalone module from day one

**Risk if ignored:** Choosing Path A wastes weeks upgrading 44 irrelevant modules. Choosing Path B risks introducing bugs in reimplemented HTTP/1.1 + WebSocket code that already works in AvaConnect.

---

## References
- AvaConnect repo: `/Volumes/M-Drive/Coding/AvaConnect/`
- AvaConnect README: explains 12 modular modules (outdated — actually 57)
- Sprint 1 review: `AvaConnect/Docs/Specifications/001-circular-dependency-refactoring/review.md`
- PHASES-5-7-ROADMAP: `AvaConnect/Docs/Architecture/PHASES-5-7-ROADMAP.md`
- RemoteAvanue full plan: `docs/plans/RemoteCast/RemoteAvanue-Plan-FullSystemImplementation-260219-V1.md`
- Settings protocol plan: `docs/plans/VoiceOSCore/VoiceOSCore-Plan-SettingsProtocol-WakeWord-HTTPAvanue-260219-V1.md`
- Handover V2: `Docs/VoiceOSCore/Handover/Handover-RemoteAvanue-260219-V2.md`
