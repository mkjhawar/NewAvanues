# Wave 3 — Cycle 2 Master Analysis Entries
## HTTPAvanue / RemoteCast / VideoAvanue
**Review date:** 260222
**Session:** VoiceOS-1M-SpeechEngine branch

---

## Module: HTTPAvanue

### [CRIT] HTTP/2 PADDED flag not stripped in DATA and HEADERS frames
- **File:** `Modules/HTTPAvanue/src/commonMain/kotlin/com/augmentalis/httpavanue/http2/Http2Connection.kt`
- **Lines:** `handleData()` ~L143; `handleHeaders()` ~L119
- **Status:** Open
- When a DATA or HEADERS frame has flag `0x8` (PADDED), the 1-byte Pad Length prefix and trailing
  pad bytes must be removed before the payload is used. Neither handler checks `Http2Flags.PADDED`.
  Result: corrupted request bodies and HPACK COMPRESSION_ERROR on padded HEADERS, which is a
  connection-fatal error. The constant `PADDED = 0x8` is defined in `Http2FrameCodec.kt:20` but
  never read in `Http2Connection.kt`.
- **Fix:** Strip `payload[0]` (pad length) prefix and `payload[size - padLength .. size]` suffix
  before decode/channel-send. Add a helper `stripPaddingAndPriority(frame)` shared by both handlers.
- **RFC:** 7540 Section 6.1 (DATA), 6.2 (HEADERS)

### [CRIT] SETTINGS INITIAL_WINDOW_SIZE not validated — values > 2^31-1 silently corrupt windows
- **File:** `Modules/HTTPAvanue/src/commonMain/kotlin/com/augmentalis/httpavanue/http2/Http2Settings.kt:61`
- **Status:** Open
- `Http2Settings.decode()` stores the raw 32-bit wire value directly as a Kotlin `Int`. Values
  with bit 31 set are interpreted as negative, silently producing a negative initial window size
  for all new streams. RFC 7540 Section 6.5.2 mandates a FLOW_CONTROL_ERROR for values > 2^31-1.
- **Fix:** In the `INITIAL_WINDOW_SIZE` decode branch: `if (value < 0) throw Http2Exception(FLOW_CONTROL_ERROR, ...)`.

### [HIGH] HTTP/2 HEADERS PRIORITY flag payload not skipped — HPACK corruption
- **File:** `Http2Connection.kt handleHeaders()` ~L119
- **Status:** Open
- When HEADERS has `PRIORITY` flag (0x20), a 5-byte priority block precedes the header block
  fragment. The code decodes the raw payload including those 5 bytes, producing incorrect header
  parsing for clients that send PRIORITY (curl ≤ 7.87, Java 11 HttpClient, older OkHttp).
- **Fix:** `val skip = (if PADDED 1 else 0) + (if PRIORITY 5 else 0); decode(payload, skip, size - skip - tailPad)`

### [HIGH] Flow-control wait is a 10ms busy-poll — throughput degradation
- **File:** `Http2Connection.kt sendResponse()` L256–268
- **Status:** Open
- Loop `while (available <= 0 && waited < 30_000L) { delay(10); ... }` polls every 10 ms
  waiting for WINDOW_UPDATE. Under back-pressure this wastes CPU and adds latency. Particularly
  impactful for RemoteCast, which sends large JPEG DATA frames through the same stack.
- **Fix:** Add `val sendWindowAvailable = Channel<Unit>(Channel.CONFLATED)` to `Http2Stream`;
  `handleWindowUpdate` calls `stream.sendWindowAvailable.trySend(Unit)`;
  `sendResponse` calls `stream.sendWindowAvailable.receive()` instead of delay-looping.

### [HIGH] HPACK encoder table size not updated when client sends SETTINGS
- **File:** `Http2Connection.kt` L25 (`hpackEncoder` init) and L97 (`remoteSettings` update)
- **Status:** Open
- `hpackEncoder` is constructed with the initial `remoteSettings.headerTableSize` and is never
  updated when the client sends a SETTINGS frame changing `HEADER_TABLE_SIZE`. Per RFC 7541
  Section 6.3 the encoder must apply the new size. Causes COMPRESSION_ERROR on long-lived
  connections that renegotiate table size.
- **Fix:** Add `setMaxDynamicTableSize(size: Int)` to `HpackEncoder` delegating to
  `dynamicTable.setMaxSize(size)`. Call after `remoteSettings = Http2Settings.decode(...)`.

### [MED] `CastWebSocketServer.frameCount` is a plain Long — data race under concurrent senders
- **File:** `Modules/RemoteCast/src/commonMain/kotlin/com/augmentalis/remotecast/transport/CastWebSocketServer.kt` L67, L121
- **Status:** Open
- `frameCount++` executes outside any mutex. Concurrent calls to `sendFrame` from multiple
  coroutines will produce lost updates. Use `AtomicLong` or place inside `clientMutex.withLock`.

### [MED] `WebSocket.isOpen()` reads `_state` without mutex — data race
- **File:** `Modules/HTTPAvanue/src/commonMain/kotlin/com/augmentalis/httpavanue/websocket/WebSocket.kt:135`
- **Status:** Open
- `_state` is mutated under `stateMutex` by `setState()` but `isOpen()` reads it directly.
  `CastWebSocketServer.sendFrame` calls `ws.isOpen()` from an arbitrary coroutine context.
- **Fix:** `@Volatile private var _state` or route `isOpen()` through `stateMutex.withLock { _state == WebSocketState.OPEN }`.

### [MED] `enablePush = true` default — server push not implemented
- **File:** `Http2Settings.kt:8`
- **Status:** Open
- Advertising ENABLE_PUSH when no push is ever sent is a protocol violation. RFC 7540 Section 8.2
  requires a server that does not support push to advertise `ENABLE_PUSH = 0`.
- **Fix:** Change default to `enablePush = false`.

### [MED] `HttpServer` accept loop has no backoff on repeated errors
- **File:** `Modules/HTTPAvanue/src/commonMain/kotlin/com/augmentalis/httpavanue/server/HttpServer.kt:83`
- **Status:** Open
- Non-shutdown accept exceptions are caught and the loop immediately retries without any delay.
  Under fd-exhaustion this produces a hot loop. Add `delay(50)` in the exception handler.

### [LOW] `HpackEncoder` uses `ArrayList<Byte>` — boxes every output byte
- **File:** `Modules/HTTPAvanue/src/commonMain/kotlin/com/augmentalis/httpavanue/hpack/HpackEncoder.kt:17`
- **Status:** Open
- Every byte of HPACK output is boxed as a `Byte` object. Replace with `ByteArrayOutputStream`
  or a manual growing `ByteArray` to eliminate allocation overhead per header field.

### [LOW] `HpackHuffman.decode` does not reject invalid EOS padding bits
- **File:** `Modules/HTTPAvanue/src/commonMain/kotlin/com/augmentalis/httpavanue/hpack/HpackHuffman.kt:56`
- **Status:** Open
- RFC 7541 Section 5.2: padding bits must be MSBs of EOS (all 1s); non-all-ones padding
  MUST trigger COMPRESSION_ERROR. Current code silently accepts any padding.

### [LOW] `RateLimitMiddleware.requestCount` incremented without synchronization
- **File:** `Modules/HTTPAvanue/src/commonMain/kotlin/com/augmentalis/httpavanue/middleware/RateLimitMiddleware.kt:15,18`
- **Status:** Open
- `requestCount` is a plain `var Int` read/written by concurrent `handle()` coroutines.
  Use `AtomicInt` or `volatile + compareAndSet`.

### [LOW] `RealHttpClient` connection pool returns potentially stale sockets
- **File:** `Modules/HTTPAvanue/src/commonMain/kotlin/com/augmentalis/httpavanue/client/RealHttpClient.kt:186`
- **Status:** Open
- A pooled socket is returned without checking `socket.isConnected()`. Server-side close between
  pool and reuse causes a retryable exception. Check health before returning from pool.

---

## Module: RemoteCast

### [MED] `AndroidCastManager.release()` loses `server.stop()` + `client.disconnect()` when scope cancelled
- **File:** `Modules/RemoteCast/src/androidMain/kotlin/com/augmentalis/remotecast/controller/AndroidCastManager.kt:196`
- **Status:** Open
- `release()` is non-suspend and launches cleanup in `scope`. ViewModel scopes are already
  cancelled when `onCleared` calls `release()`, so the coroutine is never started. Port stays
  bound and WebSocket stays open until process death.
- **Fix:** `scope.launch(NonCancellable) { server.stop(); client.disconnect() }`

### [MED] `CastWebSocketServer.start()` blocks caller thread on port bind
- **File:** `Modules/RemoteCast/src/commonMain/kotlin/com/augmentalis/remotecast/transport/CastWebSocketServer.kt:73`
- **Status:** Open
- `HttpServer.start()` → `SocketServer.bind()` is synchronous. If `start()` is called from a
  UI coroutine on `Dispatchers.Main`, this briefly blocks the main thread.
- **Fix:** Restructure `start()` to `suspend fun start()` with `withContext(Dispatchers.IO)`.

### [LOW] `DesktopCastManager.connectToDevice()` is a silent stub returning `true`
- **File:** `Modules/RemoteCast/src/desktopMain/kotlin/com/augmentalis/remotecast/controller/DesktopCastManager.kt:95`
- **Status:** Open — documented-deferred
- No real network connection is established. State reports `isConnected = true` with no transport.
  Callers cannot distinguish connected from stubbed. Add explicit "not yet implemented" error or
  document in the interface that desktop receiver mode is deferred.

### [LOW] mDNS `discoverDevices()` returns `emptyFlow()` silently
- **File:** `AndroidCastManager.kt:79`, `DesktopCastManager.kt:89`
- **Status:** Open — documented-deferred
- UI observing this flow shows empty list with no feedback. Document in `ICastManager`.

---

## Module: VideoAvanue

### [HIGH] `ModuleCommandCallbacks.videoExecutor` — suspend/non-suspend type mismatch risk
- **File:** `Modules/VideoAvanue/src/androidMain/kotlin/com/augmentalis/videoavanue/VideoPlayer.kt:102`
- **Status:** Open — requires cross-module verification
- Lambda uses `withContext(Dispatchers.Main)` (a suspend call). If `videoExecutor` type is
  non-suspend, this fails to compile. If it is suspend, the VoiceOS handler must dispatch in a
  coroutine. Verify `ModuleCommandCallbacks.videoExecutor` type definition.

### [HIGH] Global mutable `videoExecutor` slot — last-mounted player wins, multi-window broken
- **File:** `VideoPlayer.kt:102`
- **Status:** Open
- Singleton mutable callback. Multiple `VideoPlayer` instances overwrite each other's callback.
  `onDispose` from one player nullifies the callback for all active players.
- **Fix:** Use a `MutableStateFlow<Map<PlayerId, VideoExecutor>>` and route voice commands to the
  focused player. Register/unregister by stable player identity.

### [MED] `VideoGalleryScreen.queryVideos()` loads entire video library without pagination
- **File:** `VideoGalleryScreen.kt:188`
- **Status:** Open
- No `LIMIT`/`OFFSET` in MediaStore query. Devices with 500+ videos allocate large list upfront.
  Use Paging 3 `PagingSource` or add a reasonable LIMIT (e.g., 200) with a "load more" button.

### [LOW] `DesktopVideoController.toggleFullscreen()` uses `println` instead of logger
- **File:** `DesktopVideoController.kt:162`
- **Status:** Open
- Replace `println(...)` with `LoggerFactory.getLogger("DesktopVideoController").d { ... }`.

### [LOW] `RepeatMode.ALL` unreachable in `DesktopVideoController.toggleLoop()`
- **File:** `DesktopVideoController.kt:147`
- **Status:** Open — interface/impl inconsistency
- `toggleLoop` cycles OFF → ONE → OFF. `ALL` is defined in `RepeatMode` but never reached.
  Either remove `ALL` or cycle through all three modes. Android `VideoPlayer.kt` uses
  `REPEAT_MODE_ALL` via ExoPlayer directly, so the model diverges between platforms.

---

## Summary Counts

| Module | Critical | High | Medium | Low | Total |
|--------|----------|------|--------|-----|-------|
| HTTPAvanue | 2 | 2 | 4 | 4 | 12 |
| RemoteCast | 0 | 0 | 2 | 2 | 4 |
| VideoAvanue | 0 | 2 | 1 | 2 | 5 |
| **Total** | **2** | **4** | **7** | **8** | **21** |

---

## Priority Fix Order

1. `Http2Connection` PADDED flag stripping — affects all real HTTP/2 clients (Critical)
2. `Http2Settings` INITIAL_WINDOW_SIZE bounds — prevents protocol-error on adversarial SETTINGS (Critical)
3. `Http2Connection` HEADERS PRIORITY payload skip — affects curl/Java clients (High)
4. `VideoPlayer` voice executor type + multi-window registration (High, pre-Cockpit multi-frame)
5. `AndroidCastManager.release()` NonCancellable fix — prevents port leak (Medium, one line)
6. `CastWebSocketServer.frameCount` AtomicLong — fix before production (Medium)
7. `WebSocket.isOpen()` Volatile fix (Medium, one line)
8. HPACK encoder `ArrayList<Byte>` → `ByteArrayOutputStream` (Low, performance)

Author: Manoj Jhawar | 260222
