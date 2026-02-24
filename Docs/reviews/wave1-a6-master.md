# Wave 1-A6 Master Analysis Entries
**Modules:** IPC | WebSocket | Rpc
**Date:** 260222 | **Full report:** `docs/reviews/IPC-WebSocket-Rpc-Review-QualityAnalysis-260222-V1.md`

---

## MODULE: IPC
**Files:** 31 kt | **Score:** 38/100 | **Health:** RED
**P0:** 4 | **P1:** 9 | **P2:** 5

### Status
Non-functional in production. `ConnectionManager.connectInternal()` and `invoke()` are pure
simulation stubs. iOS `send()`/`broadcast()`/`request()` all return explicit failures. The module
has strong parsing infrastructure (`AvuIPCParser`, `AvuIPCSerializer`) but the transport and
invocation layers are placeholders. `subscribe<T>()` has an unsafe cast across all platforms.
A second `IPCError` sealed class in the `universal` sub-package creates type-system conflicts
with the primary `ipc.IPCError`. AVU escaping in `AndroidIPCManager` uses backslash encoding
incompatible with the percent-encoding used by `AvuIPCParser`/`AvuEscape`.

### P0 Issues
- `ConnectionManager.kt:141-142` — `connectInternal()` is `delay(100)` only. No IPC channel opened. Connection state is fraudulently `CONNECTED`.
- `ConnectionManager.kt:215` — `invoke()` is `delay(10)` + hardcoded fake result. No remote method dispatched.
- `IPCManager.ios.kt:30-59` — `send()`, `broadcast()`, `request()` all return explicit `Result.failure`. iOS IPC is entirely non-functional.
- `UniversalIPCManager.kt (ipc.universal):190-200` — Second `IPCError` sealed class with only 5 of 12+ variants. Breaks `is IPCError.PermissionDenied` pattern-matching from `universal` package scope.

### P1 Issues
- `IPCManager.android.kt:88-89` — `request()` returns stub failure: `"Request-response not yet implemented"`.
- `IPCManager.android.kt:70-75` — `subscribe<T>()` casts `Flow<IPCMessage> as Flow<T>` — `ClassCastException` at collection site.
- `IPCManager.android.kt:70-74` — No actual type filtering: every subscriber gets every message type.
- `IPCManager.android.kt:236-242` — `unescapeValue()` uses backslash escaping (`\:`, `\n`) — incompatible with `AvuEscape` percent-encoding (`%3A`, `%0A`). Wire-level corruption for messages with colons.
- `IPCManager.android.kt:114-116` — `getConnectedApps()` returns empty list always.
- `ConnectionManager.kt:79` — Connection limit check runs outside `mutex.withLock` — race condition.
- `ConnectionManager.kt:86-92` — `circuitBreakers.getOrPut()` and `rateLimiters.getOrPut()` outside mutex — HashMap structural modification race.
- `AndroidUniversalIPCManager.kt:175-177` — `actual fun create()` throws `NotImplementedError`. Rule 1.
- `ServiceConnector.android.kt:153-183` — `invoke()` has real binder check but returns fabricated success string. No AIDL dispatch.
- `ContentProviderConnector.android.kt:97-98,140-141,187-188,218-219` — `SecurityException` wrapped as generic `Exception`, losing `IPCError.PermissionDenied` type.

### P2 Issues
- `IPCEncoder.kt` — Pure delegation wrapper, zero logic. Violates Rule 2. Remove and use `AVUEncoder` directly.
- `IPCManager.android.kt:154-172` — `parseMessage()` handles only 8 of ~30 message types. Others silently dropped.
- `IPCManager.android.kt:27-30` — `messagesSent`/`messagesReceived`/`messagesFailed` are plain `Long` incremented from coroutines. Use `AtomicLong`.
- `ConnectionManager.kt` / `ServiceConnector.android.kt` — `generateConnectionId()` uses milliseconds only — collision under concurrent connects. Use UUID.
- `AvuIPCSerializer.kt:1027-1037` — `bytesSent` mapped to `messagesSent` field. Wrong unit mapping.

### Known Correct (Do Not Re-flag)
- `CircuitBreaker.kt` — Clean state machine, correct `Mutex` usage. CLOSED→OPEN→HALF_OPEN logic is correct.
- `RateLimiter.kt` — Token bucket pattern with `Mutex` protection. Correct.
- `AvuIPCParser.kt` — Comprehensive, ~1039 lines, covers all AVU message prefixes correctly.
- `AvuIPCSerializer.kt` — Correct inverse of parser for all model types (one field mapping exception noted above).
- `IPCErrors.kt` — Well-structured sealed class hierarchy with typed error codes (ERR:T/P/S/R series) and round-trip `fromAvuLine()`.
- `IPCMessages.kt` — Complete `UniversalMessage` subtype hierarchy. Uses `AvuEscape` correctly.

---

## MODULE: WebSocket
**Files:** 8 kt | **Score:** 62/100 | **Health:** YELLOW
**P0:** 1 | **P1:** 6 | **P2:** 4

### Status
Correct interface design and solid reconnection/keep-alive abstraction. The critical flaw is the
async `connect()` on Android that returns `Result.success(Unit)` before OkHttp fires `onOpen` —
callers see a successful connect while the socket may still be negotiating or about to fail. iOS
has a parallel issue: `ConnectionState.Connected` is emitted before the receive loop starts. The
`AvuSyncMessage.parse()` URL-splitting bug will corrupt any sync message carrying an `https://`
URL or any colon-containing value. Third AVU escape implementation in this module duplicates the
canonical `AvuEscape` in the AVU module.

### P0 Issues
- `WebSocketClient.android.kt:141` — `connect()` returns `Result.success(Unit)` before `onOpen` fires. OkHttp is async. Use `CompletableDeferred<Result<Unit>>` resolved in `onOpen`/`onFailure`.

### P1 Issues
- `AvuSyncMessage.kt:285` — `message.split(":")` ignores percent-encoded colons. URL payloads (containing `://`) are incorrectly split. Use `split(":", limit = 2)` and percent-decode after, or port `AvuIPCParser.splitAvu()` logic.
- `WebSocketClient.android.kt:201-211` — `handleDisconnect()` does not emit `Reconnecting` or `Disconnected` state when `autoReconnect = true`. State stays `Connected` during reconnect window.
- `WebSocketClient.ios.kt:63-84` — `_connectionState = Connected` set before `startReceiving()` launches. False connected state if receive setup fails.
- `WebSocketClient.ios.kt` — `suspendCancellableCoroutine` inside `sendMessage` callback: callback may fire after cancellation, causing `IllegalStateException: Already resumed`. Guard with `cont.isActive` check.
- `PersistentSyncQueue.ios.kt:23-28` — `save()` uses `Dispatchers.Main` for JSON serialization. Blocks UI on large queues. Use `Dispatchers.Default`.
- `PersistentSyncQueue.android.kt:33-38` — `save()` throws `IllegalStateException` if `init()` not called. No graceful guard. Initialize lazily or return `Result.failure`.
- `ReconnectionManager.kt:141` — `KeepAliveManager` uses `System.currentTimeMillis()` directly. Bypasses the `expect fun currentTimeMillis()` declared in same file. KMP contract broken.

### P2 Issues
- `AvuSyncMessage.kt:507-524` — Third independent AVU escape implementation. Delete and use `AvuEscape` from AVU module.
- `SyncQueue.kt:284-286` — `generateOperationId()`: ms timestamp + 4-digit random. Collision probability ~1/9000 per concurrent pair. Use UUID.
- `SyncQueue.kt` — `SyncQueue` and `PersistentSyncQueue` not auto-linked. Enqueues not auto-persisted. On process kill, unpersistedoperations are lost.
- `WebSocketClient.android.kt:30` — `CoroutineScope` not lifecycle-bound. Reconnect jobs leak if `close()` not called. Implement `Closeable`.

### Known Correct (Do Not Re-flag)
- `ReconnectionManager.kt` — `CancellationException` re-throw pattern is correct. Exponential backoff loop structure is sound.
- `WebSocketClient.kt` (commonMain interface) — Clean interface with proper nullable returns and event callback model.
- `SyncQueue.kt` in-memory operations — Priority queue with `Mutex` protection is correct.
- `WebSocketClientConfig` defaults — `pingIntervalMs = 30_000`, `reconnectDelayMs = 1_000`, `maxReconnectAttempts = 10` are reasonable production values.

---

## MODULE: Rpc
**Files:** ~200 kt | **Score:** 41/100 | **Health:** RED
**P0:** 1 | **P1:** 7 | **P2:** 6

### Status
Architecturally ambitious with gRPC over TCP + UDS, Wire proto generation, registry-aware routing,
and cross-platform transport abstraction. The production path via `AvaGrpcClient` and
`VoiceOSGrpcServer`/`CockpitGrpcServer` is substantially functional. The `UniversalClient` →
`RegistryAwareClient` → `PlatformClient` abstraction layer is broken: `PlatformClient.android.kt`
is a 5-method complete stub with empty TODO bodies — no gRPC channel is ever created or used
through this path. `AvaGrpcClient.close()` is fire-and-forget causing resource leaks.
`CockpitGrpcServer` has a non-thread-safe listener list and accumulates JVM shutdown hooks on
repeated `start()` calls. Wire code generation is disabled, requiring hand-maintenance of
generated proto files.

### P0 Issues
- `PlatformClient.android.kt:51-79` — All 5 methods (`connectGrpc`, `connectUDS`, `send`, `request`, `receiveStream`) are empty TODO stubs. Connection state falsely transitions to `CONNECTED`. Returns `null`, `ByteArray(0)`, `emptyFlow()`. The entire `UniversalClient` abstraction is non-functional on Android.

### P1 Issues
- `AvaGrpcClient.kt:326-328` — `close()` calls `scope.launch { disconnect() }` — fire-and-forget. Channel not shut down before `close()` returns. Resource leak. Use `runBlocking { disconnect() }` or suspend close.
- `AvaGrpcClient.kt:296-323` — `startReconnection()` hardcodes `maxAttempts = 10` instead of `config.maxRetries`. Config value has no effect on reconnect path.
- `CockpitGrpcServer.kt:102` — `listeners` is `mutableListOf()` — not thread-safe. `addListener()`/`removeListener()` from any thread plus iteration in `updateStatus()` coroutine → `ConcurrentModificationException`. Use `CopyOnWriteArrayList`.
- `CockpitGrpcServer.kt:183-186` — `Runtime.getRuntime().addShutdownHook()` inside `start()`. Each restart adds a new hook. All hooks fire on JVM exit. Deduplicate.
- `VoiceOSGrpcServer.kt:75-86` — `socketFile.delete()` not guarded against `SecurityException`. Silent bind failure if socket file is unremovable.
- `VoiceOSGrpcServer.kt:57` — `start()` calls blocking `server?.start()` without `withContext(Dispatchers.IO)`. Blocks caller thread.
- `UniversalClient.kt:119-122` — `connectionState` throws `IllegalStateException` before `connect()` called. Should emit `Disconnected` as initial state — not throw.

### P2 Issues
- `build.gradle.kts:16-18` — Wire plugin commented out. Generated proto classes are hand-maintained. No ticket or ETA for resolution.
- `AndroidTransport.kt:176-195` — Recursive `attemptReconnect()` suspend call. Deep retry chains overflow stack. Replace with loop.
- `AndroidTransport.kt:271-273` — Graceful disconnect path is `delay(100)` stub. No real send-buffer drain.
- `AvaGrpcClient.kt:265-279` — `udsEventLoopGroup?.shutdownGracefully()` not awaited before new group created. Old Epoll threads may still be running. Call `.awaitUninterruptibly()`.
- `CockpitGrpcServer.kt:99-100` — `_status` plain `var` written from `Dispatchers.IO` coroutine, read from any thread. Mark `@Volatile` or use `AtomicReference`.
- `UniversalClient.kt:130-133` — Dead code: `if (isLocal)` in-process path has no implementation.

### Known Correct (Do Not Re-flag)
- `AvaGrpcClient.kt` — `channelMutex.withLock` pattern for channel access is correct. `withRetry()` non-retryable status code list (`INVALID_ARGUMENT`, `PERMISSION_DENIED`, etc.) is correct gRPC convention.
- `VoiceOSGrpcServer.kt` — `VoiceOSServiceImpl` delegation to `VoiceOSServiceDelegate` with try/catch + `responseObserver.onError(e)` is the correct gRPC error propagation pattern.
- `AndroidTransport.kt` — `writeMutex`/`readMutex` with `withLock` is correct. `CopyOnWriteArrayList` for `dataListeners` is correct for listener collections with concurrent iteration.
- `GrpcConnectionConfig` — Well-designed configuration data class with sensible defaults. `ConnectionMode` enum (UDS/TCP) is clean.
- `ConnectionState` sealed class — `Disconnected`, `Connecting`, `Connected`, `Reconnecting(attempt, maxAttempts)`, `Error` is the correct state machine shape.

---

## Cross-Module: AVU Escaping Incompatibility (All 3 Modules)

Three independent AVU escape implementations exist across the three modules:

| File | Strategy |
|------|----------|
| `AvuEscape` (AVU module) | Percent-encoding: `%3A`, `%0A` |
| `AndroidIPCManager.unescapeValue()` | Backslash: `\:`, `\n` |
| `AvuSyncMessage.escape/unescape()` | Percent-encoding: `%3A`, `%0A` |

`AndroidIPCManager` encodes with backslash; `AvuIPCParser` decodes with percent. A message with an
embedded colon (URL, timestamp) encoded by `AndroidIPCManager` and parsed by `AvuIPCParser` will
be split incorrectly — the colon will not be treated as escaped and the field boundary will be wrong.

**Fix**: Remove `AndroidIPCManager.unescapeValue()` and `AvuSyncMessage.escape/unescape()`.
Import `com.augmentalis.avu.AvuEscape` everywhere.

## Cross-Module: `subscribe<T>()` Unsafe Cast (IPC + WebSocket)

All platform IPC implementations use `flow.map { it.second } as Flow<T>` — an unchecked cast
that produces `ClassCastException` at collection time for mismatched types. Requires `KClass<T>`
parameter and `filterIsInstance(type)`.

## Cross-Module: `connect()` Contract Violation (WebSocket + Rpc)

`AndroidWebSocketClient.connect()` and `PlatformClient.android.connectGrpc()` both return success
before transport readiness is confirmed (async OkHttp callback vs empty body). All `connect()`
implementations must suspend until the transport is confirmed ready for use.
