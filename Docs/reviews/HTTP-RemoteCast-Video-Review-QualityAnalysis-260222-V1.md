# Code Review: HTTPAvanue / RemoteCast / VideoAvanue
**Date:** 260222
**Reviewer:** code-reviewer agent
**Branch:** VoiceOS-1M-SpeechEngine
**Scope:** 69 kt (HTTPAvanue) + 12 kt (RemoteCast) + 5 kt (VideoAvanue)

---

## Summary

HTTPAvanue is a well-structured pure-Kotlin multiplatform HTTP server with HTTP/2, WebSocket, SSE,
HPACK, and TLS support. The recent flow control and Huffman fixes are in place. Three RFC-compliance
gaps remain in HTTP/2 frame handling. RemoteCast is clean and well-architected with a proper
deprecation path from TCP to WebSocket transport. VideoAvanue is lean but carries two issues: a
suspend-in-non-suspend executor callback and a non-thread-safe global callback slot.

---

## Module 1 — HTTPAvanue

### HTTP/2 Compliance

#### Issue H1 — High: DATA/HEADERS/PUSH_PROMISE PADDED flag not stripped
**File:** `Http2Connection.kt` — `handleData()` L143–172; `handleHeaders()` L102–131
**RFC:** 7540 Section 6.1 (DATA), 6.2 (HEADERS)

When a peer sends a DATA or HEADERS frame with the `PADDED` flag set (0x8), the payload contains
a 1-byte Pad Length field at the front followed by Pad Length bytes of zeros at the back. The
current code passes `frame.payload` directly to `stream.dataChannel.trySend` or
`hpackDecoder.decode` without stripping padding. This corrupts the body of every padded request
and will cause HPACK decompression failures on padded HEADERS frames — a connection-fatal
COMPRESSION_ERROR per spec.

The `PADDED` constant is defined in `Http2FrameCodec.kt` L20 but is never checked in the handlers.

**Fix required in `handleData`:**
```kotlin
val dataPayload = if (frame.hasFlag(Http2Flags.PADDED)) {
    val padLength = frame.payload[0].toInt() and 0xFF
    if (padLength >= frame.payload.size)
        throw Http2Exception(Http2ErrorCode.PROTOCOL_ERROR, "Pad length exceeds payload")
    frame.payload.copyOfRange(1, frame.payload.size - padLength)
} else frame.payload
```
Apply the same pattern in `handleHeaders` before calling `hpackDecoder.decode`.

---

#### Issue H2 — High: SETTINGS INITIAL_WINDOW_SIZE not validated against 2^31-1
**File:** `Http2Settings.kt` L61
**RFC:** 7540 Section 6.9.2

RFC 7540 Section 6.5.2 states that receiving an INITIAL_WINDOW_SIZE value larger than `2^31 - 1`
(2147483647) MUST be treated as a connection error of type FLOW_CONTROL_ERROR. The current
`decode` function simply stores the raw int value with no bounds check. Because `Int` is signed
in Kotlin/JVM the 32-bit wire value is treated as negative if bit 31 is set, silently corrupting
all window sizes for streams opened after the malicious SETTINGS frame.

**Fix in `Http2Settings.decode`:**
```kotlin
INITIAL_WINDOW_SIZE -> {
    if (value and Int.MIN_VALUE.inv().inv() != 0 || value > 0x7FFFFFFF)
        throw Http2Exception(Http2ErrorCode.FLOW_CONTROL_ERROR, "INITIAL_WINDOW_SIZE exceeds 2^31-1")
    settings.copy(initialWindowSize = value)
}
```
The simpler check: `if (value < 0) throw ...` works because Kotlin `Int` sign-extends the
unsigned wire value.

---

#### Issue H3 — Medium: HEADERS PRIORITY flag payload not consumed
**File:** `Http2Connection.kt` `handleHeaders()` L102–131
**RFC:** 7540 Section 6.2

When a HEADERS frame has the `PRIORITY` flag (0x20) set, the payload contains a 5-byte priority
block (stream dependency + weight) before the header block fragment. The code decodes
`frame.payload` directly with HPACK without skipping those bytes, producing corrupted header
parsing. PRIORITY data in HEADERS is deprecated as of RFC 9113 but still sent by older clients
(curl, Java 11 `HttpClient`, some Android OkHttp builds).

**Fix in `handleHeaders`:**
```kotlin
val headerBlockOffset = when {
    frame.hasFlag(Http2Flags.PADDED) && frame.hasFlag(Http2Flags.PRIORITY) -> {
        val padLength = frame.payload[0].toInt() and 0xFF
        // payload[1..5] = priority block; header block starts at [6]
        6
    }
    frame.hasFlag(Http2Flags.PADDED) -> 1
    frame.hasFlag(Http2Flags.PRIORITY) -> 5
    else -> 0
}
val headerBlock = frame.payload.copyOfRange(headerBlockOffset, /* ... strip tail padding too */)
val headers = hpackDecoder.decode(headerBlock)
```

---

#### Issue H4 — Medium: Flow control busy-wait with 30-second spin
**File:** `Http2Connection.kt` `sendResponse()` L256–268

The flow-control wait loop:
```kotlin
while (available <= 0 && waited < 30_000L) {
    delay(10)
    waited += 10
    available = flowControl.availableSendBytes(stream, remoteSettings.maxFrameSize)
}
```
This busy-polls every 10 ms for up to 30 seconds, consuming CPU and introducing up to 10 ms of
unnecessary additional latency per DATA frame when the window is temporarily exhausted. The
correct approach is a `Mutex` + `Condition`-like signal: when `handleWindowUpdate` is called,
it should wake waiting senders. In coroutine terms this is typically a `Channel<Unit>` or
`CompletableDeferred` per stream.

This is not a correctness bug (it works) but it will degrade throughput under real load —
particularly for RemoteCast MJPEG-over-WebSocket sending large JPEG frames.

**Recommended fix:** Add a `sendWindowAvailable: Channel<Unit>` to `Http2Stream` and call
`trySend(Unit)` in `increaseSendWindow`. `sendResponse` then does `sendWindowAvailable.receive()`
instead of polling.

---

#### Issue H5 — Medium: `frameCount` in `CastWebSocketServer` not thread-safe
**File:** `CastWebSocketServer.kt` L67, L121

```kotlin
private var frameCount = 0L   // L67
// ...
frameCount++                   // L121 — outside clientMutex
```

`frameCount` is a plain `Long` var incremented in `sendFrame`, which can be called concurrently
from the capture coroutine. This is a data race. Use `AtomicLong` or increment inside
`clientMutex.withLock`.

---

#### Issue H6 — Medium: `WebSocket.isOpen()` reads `_state` without lock
**File:** `WebSocket.kt` L135

```kotlin
fun isOpen() = _state == WebSocketState.OPEN
```

`_state` is mutated via `setState()` which uses `stateMutex.withLock`. This `isOpen()` function
reads `_state` directly without the mutex — a data race visible from `CastWebSocketServer.sendFrame`
which calls `ws.isOpen()` from a coroutine that is not the WebSocket's own `receiveJob`/`sendJob`.
Mark `_state` as `@Volatile` or route `isOpen()` through `stateMutex`.

---

#### Issue H7 — Low: `enablePush = true` by default — server push not implemented
**File:** `Http2Settings.kt` L8

The server advertises `ENABLE_PUSH = 1` in its SETTINGS frame, but `Http2Connection` never sends
PUSH_PROMISE frames (receiving one triggers GOAWAY). Advertising support that is not implemented
is misleading and may cause well-behaved clients to wait for promised resources. Set
`enablePush = false` by default until push is implemented.

---

#### Issue H8 — Low: `HpackHuffman.decode` does not validate 0-bit padding
**File:** `HpackHuffman.kt` L56–63

The comment correctly acknowledges the padding validation gap. RFC 7541 Section 5.2 requires that
if padding bits are not MSBs of EOS (all 1s), a COMPRESSION_ERROR must be returned. The current
code silently accepts invalid padding. While in practice malformed Huffman padding only appears in
broken or malicious implementations, strict RFC conformance requires the check.

---

#### Issue H9 — Low: `RateLimitMiddleware.requestCount` not thread-safe
**File:** `RateLimitMiddleware.kt` L15, L18

```kotlin
private var requestCount = 0
// ...
if (++requestCount % 100 == 0) cleanupExpiredBuckets()
```

`requestCount` is incremented outside `mutex` — since `handle()` can run on multiple IO
coroutines concurrently, this is a data race. Use `AtomicInt` or move the increment inside
`mutex.withLock`. The cleanup call is idempotent so missing an occasional trigger is not
critical, but the race is a correctness issue.

---

#### Issue H10 — Low: `Http2Connection` HPACK encoder not re-initialized when `remoteSettings` changes
**File:** `Http2Connection.kt` L25, L97

```kotlin
private val hpackEncoder = HpackEncoder(remoteSettings.headerTableSize)   // L25
// ...
remoteSettings = Http2Settings.decode(frame.payload)  // L97 — replaces but encoder keeps old table size
```

When the client sends a SETTINGS frame with a new `HEADER_TABLE_SIZE`, `remoteSettings` is
updated but `hpackEncoder` still uses the original table size. Per RFC 7541 Section 6.3, the
encoder must acknowledge and apply the table size change. The fix is to call
`hpackEncoder.setMaxDynamicTableSize(remoteSettings.headerTableSize)` after updating
`remoteSettings`. (This requires adding a `setMaxDynamicTableSize` method to `HpackEncoder`
and delegating to `HpackDynamicTable.setMaxSize`.)

---

### Server Robustness

#### Issue H11 — Medium: `HttpServer` accept loop does not back off on repeated accept errors
**File:** `HttpServer.kt` L83–91

Any non-shutdown exception from `socketServer.accept()` is logged and the loop immediately
retries. Under OS-level file-descriptor exhaustion or repeated connection resets this can produce
a tight retry loop consuming 100% CPU. A minimum `delay(50)` on every transient error would
prevent this.

---

#### Issue H12 — Low: Connection pool does not validate pooled socket health
**File:** `RealHttpClient.kt` L186–199

`ConnectionPool.get()` returns a pooled socket without verifying it is still alive. A socket can
be closed by the remote server between being pooled and being reused. The HTTP write will then
throw, which is caught and surfaces as `HttpClientException`, but the retry logic classifies it
as retryable and starts a fresh connection — correct in outcome but wastes one round trip. A
`socket.isConnected()` check before returning from the pool would avoid the retry overhead.

---

### Performance

#### Issue H13 — Low: `HpackEncoder` uses `ArrayList<Byte>` as output buffer
**File:** `HpackEncoder.kt` L17

```kotlin
val buffer = ArrayList<Byte>(estimated)
```

`ArrayList<Byte>` boxes every byte. For HTTP/2 header encoding (called on every request/response)
this allocates one `Byte` object per byte of header data. Use `ByteArrayOutputStream` or a
`ByteArray` with manual offset tracking instead. For typical response headers (200–400 bytes)
this creates ~200–400 boxed Byte objects per request.

---

### Tests

No test coverage was observed for:
- PADDED flag handling in DATA and HEADERS frames
- PRIORITY flag payload skipping in HEADERS frames
- `INITIAL_WINDOW_SIZE` bounds enforcement
- WebSocket masking / unmasking correctness
- Huffman padding validation

---

## Module 2 — RemoteCast

### Architecture Assessment

The module is well-designed with a clean protocol definition (`CastFrameData`), proper deprecation
markers on the TCP transport classes, and correct use of `kotlinx.coroutines.sync.Mutex` for the
WebSocket client list. The `ScreenCaptureHelper` correctly handles row-padding in
`ImageReader` output and recycles `Bitmap` objects. The `DesktopCastManager` uses `SCALE_SMOOTH`
which is CPU-intensive but appropriate for low-FPS screen mirroring.

---

#### Issue R1 — Medium: `AndroidCastManager.release()` launches a coroutine from a non-suspending function
**File:** `AndroidCastManager.kt` L196–199

```kotlin
override fun release() {
    captureJob?.cancel()
    captureHelper?.stopCapture()
    scope.launch {
        server.stop()       // suspend fun
        client.disconnect() // suspend fun
    }
    mediaProjection?.stop()
    ...
}
```

`release()` is a fire-and-forget launcher on `scope`. If `scope` is a `ViewModel`'s
`viewModelScope`, it is already cancelling by the time `release()` is called (from `onCleared`),
so the `scope.launch` block will never execute. `server.stop()` and `client.disconnect()` are
then silently skipped — the server keeps port 54321 bound until the process is killed, and
the WebSocket socket is not closed cleanly.

**Fix:** Make `release()` a `suspend fun` (it can be renamed `releaseAsync()`), or add a
dedicated `CoroutineScope(NonCancellable)` launch:
```kotlin
scope.launch(NonCancellable) {
    server.stop()
    client.disconnect()
}
```

---

#### Issue R2 — Medium: `CastWebSocketServer.start()` creates HttpServer on calling thread, not IO
**File:** `CastWebSocketServer.kt` L73–106

`HttpServer.start()` calls `SocketServer.bind()` which blocks while binding the port. If `start()`
is called from the main/UI thread, port binding blocks the UI momentarily. `HttpServer.start()` is
not a `suspend fun`, so `withContext(Dispatchers.IO)` cannot be used inside it without restructuring.
The binding should be moved to the coroutine scope or made asynchronous.

---

#### Issue R3 — Low: `DesktopCastManager.connectToDevice()` is a stub
**File:** `DesktopCastManager.kt` L95–114

The method sets state to `isConnected = true` but opens no TCP socket. Comment says
"Future: open a TCP socket to device.address here." This is documented intent, but since the
interface contract of `connectToDevice` is to establish a real connection, callers observing
`state.isConnected == true` will believe they are connected when no transport path exists.
`ICastManager` (the interface file) documents no caveat about this being deferred.

For now, this should either return `false` with an error message or the comment should say
"no-op stub; always returns true for testing" to make the limitation explicit.

---

#### Issue R4 — Low: `ScreenCaptureHelper.estimatedJpegSize` formula is wrong for high quality
**File:** `ScreenCaptureHelper.kt` L154–159

```kotlin
val ratio = 1.0 - (quality / 200.0)  // quality=60 → ratio ≈ 0.7 → ~30% of raw
```

At `quality = 100` → `ratio = 0.5` → estimate = 50% of raw pixels. But JPEG at quality=100
typically produces output larger than 50% of raw for complex images. The pre-allocation is used
by `ByteArrayOutputStream`, so underestimating just causes one or more internal buffer growth
copies — no correctness issue, but the comment is misleading (it says "~30% of raw" for quality=60
but the math gives `ratio = 0.7` which is used as a multiplier, yielding 70% of raw, not 30%).

Minor readability fix:
```kotlin
// quality=60 → ratio = 0.7 → estimated size ≈ 70% of raw pixels
```

---

#### Issue R5 — Low: mDNS device discovery returns `emptyFlow()` with no indication
**File:** `AndroidCastManager.kt` L79; `DesktopCastManager.kt` L89

`discoverDevices()` returns `emptyFlow()` silently. A UI observing this flow will show an empty
device list with no loading indicator and no error message, giving the user no feedback. At minimum
the `ICastManager` doc comment should note "returns emptyFlow until mDNS is implemented."

---

## Module 3 — VideoAvanue

### Architecture Assessment

`VideoPlayer` is a functional single-composable ExoPlayer wrapper with correct bitmap recycling in
`CastReceiverView`, proper use of `DisposableEffect` for resource cleanup, and working voice
command dispatch. `VideoGalleryScreen` uses `LaunchedEffect(Unit)` for a one-shot MediaStore query
with correct IO dispatching.

---

#### Issue V1 — High: `ModuleCommandCallbacks.videoExecutor` accepts a `suspend fun` but is called from non-suspend context
**File:** `VideoPlayer.kt` L102–114

```kotlin
DisposableEffect(exoPlayer) {
    ModuleCommandCallbacks.videoExecutor = { actionType, _ ->
        withContext(Dispatchers.Main) {    // suspend call in lambda
            executeVideoCommand(...)
        }
    }
    ...
}
```

The lambda assigned to `videoExecutor` calls `withContext(Dispatchers.Main)`, which is a `suspend`
function. If the `videoExecutor` callback type is `(CommandActionType, Any?) -> HandlerResult`
(non-suspend), this will not compile. If it is declared as a suspend lambda
`(suspend (CommandActionType, Any?) -> HandlerResult)?`, then the caller site must itself be
inside a coroutine — which works only if VoiceOS dispatches the callback inside a coroutine.
The actual type of `ModuleCommandCallbacks.videoExecutor` is not visible in this file, but the
pattern suggests fragility: if the caller is not in a coroutine, `withContext` cannot be called
and the code will not compile or will crash.

**Action:** Verify `ModuleCommandCallbacks.videoExecutor` type. If it is non-suspend, remove
`withContext` and move ExoPlayer calls directly (ExoPlayer main-thread methods must be called on
Main; if the handler is already dispatched there, no `withContext` is needed). If it is suspend,
the architecture is intentional.

---

#### Issue V2 — High: `ModuleCommandCallbacks.videoExecutor` is a global mutable slot — unsafe for multi-window
**File:** `VideoPlayer.kt` L102–114

`ModuleCommandCallbacks.videoExecutor` is a singleton mutable field. If two `VideoPlayer`
composables are active simultaneously (e.g., in a Cockpit multi-frame layout), the second will
overwrite the first. Voice commands will then only control the last-mounted player. If the
first player is unmounted while the second is active, `onDispose` sets `videoExecutor = null`
and the second player stops responding to voice commands entirely.

**Fix:** Use a scoped registration system (e.g., a `MutableStateFlow<List<VideoExecutor>>`) or
scope the executor by a player ID, and have VoiceOS route to the focused player.

---

#### Issue V3 — Medium: `VideoGalleryScreen.queryVideos()` loads all device videos without pagination
**File:** `VideoGalleryScreen.kt` L188–233

The MediaStore query loads every video on the device into memory at once with no LIMIT clause.
On devices with large video libraries (hundreds of items) this allocates a large `List<VideoItem>`
on the IO thread. The `LazyVerticalGrid` will handle rendering lazily, but all metadata is loaded
upfront. Add a `LIMIT ? OFFSET ?` clause or use `Pager`/`PagingSource` for incremental loading.

---

#### Issue V4 — Low: `DesktopVideoController.toggleFullscreen()` uses `println`
**File:** `DesktopVideoController.kt` L162

```kotlin
println("[DesktopVideoController] toggleFullscreen requested — handle in UI layer")
```

Use the project logging framework (`LoggerFactory.getLogger`) instead of `println`. This is
consistent with every other module in the codebase.

---

#### Issue V5 — Low: `IVideoController.toggleLoop` doc says "toggle loop" but `DesktopVideoController` cycles OFF → ONE → OFF
**File:** `IVideoController.kt` L52; `DesktopVideoController.kt` L147–156

The interface comment says "Toggle loop playback" (binary) but the desktop implementation cycles
through OFF → ONE → OFF, skipping `RepeatMode.ALL`. The comment in the implementation says
"cycles OFF → ONE → OFF (matching the interface description)" but the interface does not describe
this cycle. If `ALL` is unused, remove it from `RepeatMode`. If it is used by the Android
implementation, the interface should document the tri-state behavior.

---

## Cross-Module Issues

#### Issue X1 — Medium: Both `CastWebSocketServer` and `VideoPlayer` have AVID on interactive elements but `CastOverlay` status bar labels are missing AVID
**File:** `CastOverlay.kt` L143–164

The bottom status row with "Casting: 1280x720" and "30 fps" Text composables are not interactive
and correctly lack AVID. However, the streaming indicator Row at the top (`L57–95`) that contains
the stop button does have AVID. The connect button at the center (`L124–138`) has AVID. This is
complete — no gap found in interactive elements.

*No action required — status confirmed correct.*

---

## Issues Table

| Severity | File:Line | Issue | Suggestion |
|----------|-----------|-------|------------|
| High | `Http2Connection.kt:143` | PADDED flag in DATA frames not stripped — body corruption | Strip pad length byte + trailing padding from payload |
| High | `Http2Connection.kt:119` | PADDED+PRIORITY flags in HEADERS not stripped — HPACK corruption | Strip 1 (PADDED) + 5 (PRIORITY) prefix bytes before decode |
| High | `Http2Settings.kt:61` | INITIAL_WINDOW_SIZE not validated ≤ 2^31-1 — silent window corruption | Throw FLOW_CONTROL_ERROR if decoded value > 0x7FFFFFFF |
| High | `VideoPlayer.kt:102` | `videoExecutor` suspend/non-suspend type mismatch — possible crash | Verify callback type; remove `withContext` if caller dispatches on Main |
| High | `VideoPlayer.kt:102` | Global mutable `videoExecutor` — multi-window voice control broken | Scope by player ID; route to focused player |
| Medium | `Http2Connection.kt:256` | Flow control wait is a 10ms busy-poll for up to 30s | Use `Channel<Unit>` in `Http2Stream` to signal window updates |
| Medium | `Http2Connection.kt:25` | HPACK encoder table size not updated when client sends SETTINGS | Call `setMaxDynamicTableSize` after `remoteSettings` update |
| Medium | `CastWebSocketServer.kt:67` | `frameCount` var incremented outside mutex — data race | Use `AtomicLong` or increment inside `clientMutex` |
| Medium | `WebSocket.kt:135` | `isOpen()` reads `_state` without `stateMutex` — data race | Mark `_state` as `@Volatile` or route through mutex |
| Medium | `HttpServer.kt:83` | Accept loop no backoff on repeated errors — CPU spin possible | Add `delay(50)` in the catch block |
| Medium | `AndroidCastManager.kt:196` | `release()` `scope.launch` silently skipped if scope cancelled | Use `NonCancellable` or make `release` suspend |
| Medium | `CastWebSocketServer.kt:73` | `HttpServer.start()` blocks on `SocketServer.bind()` on calling thread | Move bind to IO or make start suspend |
| Medium | `VideoGalleryScreen.kt:188` | All device videos loaded in one query — memory spike on large libraries | Use paged query with LIMIT/OFFSET or Paging library |
| Medium | `Http2Settings.kt:8` | `enablePush = true` but push never implemented — misleading SETTINGS | Set `enablePush = false` by default |
| Low | `HpackHuffman.kt:56` | Padding bits not validated as MSBs of EOS | Add `COMPRESSION_ERROR` on invalid padding after final byte |
| Low | `HpackEncoder.kt:17` | `ArrayList<Byte>` boxes every output byte | Replace with `ByteArrayOutputStream` |
| Low | `RateLimitMiddleware.kt:18` | `requestCount` incremented unsafely across coroutines | Use `AtomicInt` |
| Low | `RealHttpClient.kt:186` | Pooled socket not validated alive before reuse | Check `socket.isConnected()` before returning from pool |
| Low | `DesktopCastManager.kt:95` | `connectToDevice` is a silent stub returning `true` | Return `false` with error, or document clearly as no-op |
| Low | `DesktopVideoController.kt:162` | `println` instead of logger | Use `LoggerFactory.getLogger` |
| Low | `IVideoController.kt:52` | `RepeatMode.ALL` unreachable in `DesktopVideoController` | Remove `ALL` or document tri-state behavior in interface |

---

## Recommendations

1. **Fix the three HTTP/2 PADDED/PRIORITY payload issues (H1, H2, H3) together** — they share
   the same code path in frame dispatch. Write a `stripPaddingAndPriority(frame)` helper that
   normalizes the payload before dispatch. Add unit tests with padded frames.

2. **Fix the INITIAL_WINDOW_SIZE overflow check (H2)** — a single `if (value < 0)` guard in
   `Http2Settings.decode` for the `INITIAL_WINDOW_SIZE` case. One line.

3. **Fix `VideoPlayer` voice executor (V1, V2)** — verify the callback type and decide on a
   scoped registration model before multi-window use is attempted.

4. **Fix `AndroidCastManager.release()` (R1)** — change `scope.launch` to
   `scope.launch(NonCancellable)`. This is a one-line fix with real impact on resource leaks.

5. **Replace `ArrayList<Byte>` in `HpackEncoder` (H13)** — switch to `ByteArrayOutputStream`
   for zero-allocation-overhead header encoding. This affects every HTTP/2 response.

6. **Add HPACK encoder table size update on SETTINGS (H10)** — required for correct long-lived
   HTTP/2 connections where the client shrinks its header table.

7. **Remove `enablePush = true` default (H7)** — a server that does not implement push MUST
   advertise `ENABLE_PUSH = 0` per RFC 7540 Section 8.2.

Author: Manoj Jhawar | Review date: 260222
