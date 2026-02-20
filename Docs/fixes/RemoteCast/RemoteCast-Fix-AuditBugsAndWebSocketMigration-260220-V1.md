# RemoteCast — Fix: Audit Bug Fixes + WebSocket Migration
**Date:** 2026-02-20 | **Version:** V1 | **Branch:** HTTPAvanue

## Summary
Fixed 5 bugs in RemoteCast + migrated transport from raw TCP to HTTPAvanue WebSocket.

## Bug Fixes (Part 2)

### Fix 2.1: MemoryImageOutputStream typo (CRITICAL — compile error)
- **Problem:** `javax.imageio.stream.MemoryImageOutputStream` is not a real JDK class.
- **Fix:** Replaced with `javax.imageio.stream.MemoryCacheImageOutputStream`.
- **File:** `desktopMain/.../DesktopCastManager.kt`

### Fix 2.2: CastResolution missing @Serializable (CRITICAL — compile error)
- **Problem:** `CastState` is `@Serializable` with `resolution: CastResolution`, but `CastResolution` enum lacks annotation.
- **Fix:** Added `@Serializable` annotation.
- **File:** `commonMain/.../model/CastState.kt`

### Fix 2.3: Duplicate CastFrameData in desktopMain (CRITICAL — shadowing)
- **Problem:** desktopMain defined a local `CastFrameData` with different fields (`frameIndex`, `timestampMs`, `jpegBytes`) than commonMain version (`frameBytes`, `timestamp`, `sequenceNumber`). Name collision + incompatible with CAST wire protocol.
- **Fix:** Removed local class, imported `com.augmentalis.remotecast.protocol.CastFrameData`, refactored capture loop to use commonMain field names.
- **File:** `desktopMain/.../DesktopCastManager.kt`

### Fix 2.4: DesktopCastManager.release() never cancels scope (SIGNIFICANT)
- **Problem:** `CoroutineScope(SupervisorJob() + Dispatchers.Default)` never cancelled, causing resource leak.
- **Fix:** Added `scope.cancel()` in `release()`. Also changed from `scope.launch { stopCasting() }` to direct `captureJob?.cancel()` since scope cancellation will clean up.
- **File:** `desktopMain/.../DesktopCastManager.kt`

### Fix 2.5: Desktop has no transport (RESOLVED via WebSocket migration)
- **Problem:** DesktopCastManager captured frames into `MutableStateFlow` but never sent them over network.
- **Fix:** Wired `CastWebSocketServer` into DesktopCastManager — frames now transmitted via WebSocket.

## WebSocket Migration (Part 4)

### Architecture Change
```
BEFORE:                           AFTER:
AndroidCastManager                AndroidCastManager
  -> MjpegTcpServer (raw TCP)       -> CastWebSocketServer (HTTPAvanue)
  -> MjpegTcpClient (raw TCP)       -> CastWebSocketClient (HTTPAvanue)

DesktopCastManager                DesktopCastManager
  -> MutableStateFlow (no network)   -> CastWebSocketServer (HTTPAvanue)
```

### New Files (commonMain)
1. **`transport/CastWebSocketServer.kt`** — HTTPAvanue `HttpServer` + WebSocket handler
   - WebSocket at `/cast/stream` (binary CAST frames)
   - REST: `GET /cast/status` (JSON), `GET /cast/health`
   - Multi-client broadcast with dead-client cleanup
   - Uses CAST wire protocol (20-byte header + JPEG payload) for backward compat

2. **`transport/CastWebSocketClient.kt`** — HTTPAvanue `WebSocketClient` wrapper
   - Connects to `ws://host:port/cast/stream`
   - Decodes CAST headers, emits `Flow<ByteArray>` of raw JPEG data
   - Auto-reconnect via `WebSocketReconnectConfig`

### Modified Files
- **`build.gradle.kts`** — Added `api(project(":Modules:HTTPAvanue"))`, `alias(libs.plugins.compose)`, `compose.runtime` for desktop
- **`AndroidCastManager.kt`** — Swapped `MjpegTcpServer`/`MjpegTcpClient` for `CastWebSocketServer`/`CastWebSocketClient`
- **`DesktopCastManager.kt`** — Added `CastWebSocketServer`, frames now sent via `server.sendFrame(frame)`
- **`MjpegTcpServer.kt`** — Marked `@Deprecated`
- **`MjpegTcpClient.kt`** — Marked `@Deprecated`

### Build Fix: Compose Runtime on Desktop
- Pre-existing issue: RemoteCast had `kotlin.compose` plugin but no JB Compose plugin (`compose`)
- Desktop compilation failed with "Compose Runtime not found on classpath"
- Fixed by adding `alias(libs.plugins.compose)` + `implementation(compose.runtime)` to desktopMain

## Build Verification
- `./gradlew :Modules:RemoteCast:compileKotlinDesktop` — BUILD SUCCESSFUL
- `./gradlew :Modules:RemoteCast:compileDebugKotlinAndroid` — BUILD SUCCESSFUL
