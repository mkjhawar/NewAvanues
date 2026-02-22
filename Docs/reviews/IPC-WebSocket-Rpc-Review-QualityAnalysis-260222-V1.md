# IPC + WebSocket + Rpc Layer — Quality Analysis — 260222

---

# IPC Quality Report — 260222

## Summary
SCORE: 38 | HEALTH: RED
FILES: 31 kt | LINES: ~3200 | KMP: yes (commonMain + androidMain + iosMain + desktopMain)

The IPC module has serious structural problems that make it non-functional in production.
The two most severe issues are: (1) `ConnectionManager.connectInternal()` and `invoke()` are pure
simulation stubs — no real inter-process channel is ever opened; and (2) iOS `send()`, `broadcast()`,
and `request()` all return `Result.failure(Exception("iOS IPC not implemented"))` explicitly.
Beyond these completeness failures, there is a type-system conflict (`IPCError` declared in two
packages with incompatible variants), an escaping incompatibility between `AndroidIPCManager`
(backslash escaping) and `AvuIPCParser` (percent-encoding), and unsafe generic casts in
`subscribe<T>()` across all three platform implementations. The `AvuIPCParser` and `AvuIPCSerializer`
are the strongest parts of the module — comprehensive, correct, and well-covered.

---

## P0 Critical Issues

- **[ConnectionManager.kt:141-142] `connectInternal()` is a delay-based simulation stub.**
  The method body is `delay(100)` — no socket is opened, no AIDL binding is established, no
  Content Provider is touched. The connection state transitions to `CONNECTED` based purely on
  the absence of an exception. Every downstream caller that believes it has an open IPC channel
  is operating on false state.

- **[ConnectionManager.kt:215] `invoke()` is a simulation stub.**
  The method delays 10 ms and returns `MethodResult.Success("Result from ${invocation.methodName}")`.
  No remote method is ever dispatched. Callers receive hardcoded fabricated results. Any system
  that exercises the IPC method-invocation path is silently receiving wrong data with no error.

- **[IPCManager.ios.kt:30-59] iOS IPC is entirely non-functional.**
  `send()`, `broadcast()`, `request()`, and `subscribe()` all return explicit failures or empty flows.
  `handleIncomingURL()` increments `messagesReceived` but has a TODO comment and never parses content.
  iOS apps that attempt to use IPC will fail on every operation. This is not an acceptable placeholder
  state for a shipped module.

- **[UniversalIPCManager.kt (ipc.universal):190-200] Duplicate `IPCError` sealed class with
  incompatible variant set.**
  The `universal` package defines its own `IPCError` sealed class containing only `Unavailable`,
  `Timeout`, `ConnectionFailed`, `SerializationError`, and `Unknown`. The main `ipc` package defines
  a richer `IPCError` with 12+ variants including `PermissionDenied`, `ResourceExhausted`,
  `AuthenticationFailed`, `NetworkFailure`, `SignatureVerificationFailed`, etc.
  Any code in the `universal` package that uses `is IPCError.PermissionDenied` will not compile.
  Any code that imports from `ipc.universal` and `ipc` simultaneously will have ambiguous resolution.
  One definition must be removed.

---

## P1 High Issues

- **[IPCManager.android.kt:88-89] `request()` is an explicit stub.**
  Returns `Result.failure(IPCError.SendFailed("Request-response not yet implemented"))`. The
  `request()` method is part of the public `IPCManager` interface. Any caller that invokes
  request/response on Android will receive a runtime failure regardless of connection state.

- **[IPCManager.android.kt:70-75] `subscribe<T>()` unsafe cast.**
  `messageFlow.map { it.second } as Flow<T>` — the cast from `Flow<IPCMessage>` to `Flow<T>` is
  unchecked. At collection time, when the flow emits an `IPCMessage` subtype that doesn't match `T`,
  a `ClassCastException` is thrown at the collector, not at the subscribe site. The stack trace will
  point into framework internals. Identical unsafe cast present in `IPCManager.ios.kt:44-46`.

- **[IPCManager.android.kt:70-74] Type filtering is entirely absent.**
  The comment says "Note: Type filtering happens at call site since reified types are not available"
  but no filtering is performed — the full message stream is returned regardless of `T`. Every
  subscriber of every type receives every IPC message. Filtering must be implemented using
  `filterIsInstance<T>()` with a `KClass<T>` parameter.

- **[IPCManager.android.kt:236-242] Backslash escape incompatibility.**
  `unescapeValue()` converts `\n` → newline, `\r` → carriage return, `\:` → colon.
  `AvuIPCParser` and `AvuIPCSerializer` use `AvuEscape` which percent-encodes: `%0A` → newline,
  `%0D` → carriage return, `%3A` → colon. If `AndroidIPCManager` encodes a message with backslash
  escaping and `AvuIPCParser` attempts to parse it, colons will not be escaped, splitting will
  be wrong, and messages with embedded colons (URLs, timestamps, connection IDs) will be corrupted.
  All escaping must route through `AvuEscape`.

- **[IPCManager.android.kt:114-116] `getConnectedApps()` returns empty list.**
  Returns `emptyList()` unconditionally. Any UI or system feature that lists connected apps for
  IPC routing will show nothing, regardless of actual connection state.

- **[ConnectionManager.kt:79] Connection limit race condition.**
  `connections.size >= resourceLimits.maxConnections` is evaluated WITHOUT holding the `mutex`.
  Between the size check and the subsequent `connections[id] = ...` write (which IS inside the
  mutex), a concurrent caller can pass the check, both proceed, and the connection map exceeds
  `maxConnections`. The check must be moved inside `mutex.withLock`.

- **[ConnectionManager.kt:86-92] `getOrPut()` on mutable maps outside mutex.**
  `circuitBreakers.getOrPut(id) { ... }` and `rateLimiters.getOrPut(id) { ... }` are called
  outside the `mutex.withLock` block. `HashMap.getOrPut()` is not thread-safe. Concurrent calls
  can trigger simultaneous map structural modifications. Both calls must be moved inside the lock.

- **[AndroidUniversalIPCManager.kt:175-177] `Companion.create()` throws `NotImplementedError`.**
  The `actual fun create()` factory method throws `kotlin.NotImplementedError` — a Rule 1 violation.
  Any code that calls `UniversalIPCManager.create()` on Android will crash immediately.

- **[ServiceConnector.android.kt:153-183] `invoke()` returns hardcoded fake success.**
  The binder alive-check is real, but the actual dispatch is commented pseudocode:
  `"In real implementation, you would: val service = IYourService.Stub.asInterface(binder)"`.
  The method returns `MethodResult.Success("Method ${invocation.methodName} invoked with params: $serializedParams")`
  — a fabricated success string. No AIDL stub is called. Remote methods are never executed.

- **[ContentProviderConnector.android.kt:97-98, 140-141, 187-188, 218-219]
  `SecurityException` loses typed error information.**
  All four content provider operation methods (query, insert, update, delete) catch
  `SecurityException` and re-throw it as `Exception("Permission denied: ${e.message}")`.
  The caller cannot check `is IPCError.PermissionDenied` — the typed information is lost.
  These should wrap and re-throw as `IPCError.PermissionDenied(...)`.

- **[UniversalIPCManager.kt (ipc.universal):135-174] `MessageFilter` duplicated.**
  `MessageFilter` data class with identical fields and semantics is defined in both
  `IPCModels.kt` (main `ipc` package) and `UniversalIPCManager.kt` (`universal` sub-package).
  One is dead code. The `universal` package should import from the parent.

---

## P2 Medium Issues

- **[IPCEncoder.kt] Pure delegation wrapper violates Rule 2.**
  `IPCEncoder` delegates all calls to `BaseEncoder`/`AVUEncoder` with zero added logic.
  The `typealias` declarations at the bottom add further confusion. This wrapper exists only to
  satisfy a naming convention. Apply Rule 2 and replace with direct use of `AVUEncoder`.

- **[IPCManager.android.kt:154-172] `parseMessage()` handles only 8 of ~30 message types.**
  `VDO`, `CAL`, `POS`, `SHR`, `SNC`, `VCM`, `URL`, `NAV`, `SSO`, `SSI`, `MIC`, `CAM`, and ~15 other
  prefixes are silently ignored. Incoming messages of these types are dropped without error. The
  catch-all `else -> null` in `parseMessage()` should at minimum log an unrecognized-type warning.

- **[IPCManager.android.kt:27-30] Unsynchronized metric counters.**
  `messagesSent`, `messagesReceived`, `messagesFailed` are plain `Long` fields incremented with
  `++` from coroutines. This is a non-atomic read-modify-write. Use `AtomicLong` or
  `kotlinx.coroutines.sync.Mutex` wrapping the counter update.

- **[ConnectionManager.kt / ServiceConnector.android.kt] `generateConnectionId()` collision risk.**
  Both use `currentTimeMillis().toString()` as the sole uniqueness component. Two concurrent
  `connect()` calls within the same millisecond produce the same ID. Use `UUID.randomUUID()`
  or combine milliseconds with an `AtomicInteger` sequence number.

- **[AvuIPCSerializer.kt:1027-1037] `toIPCMetrics()` maps `bytesSent` to `messagesSent`.**
  The serializer incorrectly maps `metrics.bytesSent` to the `messagesSent` AVU field and
  `metrics.bytesReceived` to `messagesReceived`. Bytes and message counts are different units.
  The serialized metrics will show byte counts where message counts are expected.

- **[AvuIPCParser.kt:109-113] `splitAvu()` off-by-one at string end.**
  The `%3A` check at position `i` uses `i + 2 < avu.length`. For a string ending exactly with
  `%3A`, `i + 2 == avu.length - 1` satisfies `i + 2 < avu.length`, but the read at `avu[i+2]`
  is the last character `A`. This is actually correct. However, the guard should be
  `i + 2 <= avu.length - 1` (i.e., `i + 2 < avu.length`) which it is — confirm this is
  not masking a one-char truncation on `%3` at the very end of the string.

- **[ServiceConnector.android.kt:68-73 / 76-80] No reconnection on disconnect or binding death.**
  `onServiceDisconnected()` updates the connection state map but does not trigger reconnection
  or notify any callback. `onBindingDied()` removes the connection entirely without notification.
  Applications relying on long-lived AIDL connections get silent drops.

---

## Code Smells

- **Three escape implementations**: `AvuEscape` (percent-encoding), `unescapeValue()` (backslash),
  `AvuSyncMessage` (local, percent-encoding). Only one should exist.
- **Dual `IPCError` definitions**: `ipc.IPCError` and `ipc.universal.IPCError` with different shapes.
- **Duplicate `MessageFilter`**: defined in both `IPCModels.kt` and `UniversalIPCManager.kt`.
- **`IPCEncoder` wrapper**: delegates everything, adds nothing — dead indirection layer.

## Missing Implementations

- `ConnectionManager.connectInternal()` — real IPC channel establishment (AIDL, Content Provider, or socket).
- `ConnectionManager.invoke()` — real remote method dispatch.
- iOS `IPCManager.send()` / `broadcast()` / `request()` — all fail by design (no iOS IPC equivalent of AIDL).
  If iOS IPC is not a supported target, the interface must declare `isAvailable() = false` and document
  that operations throw/fail on iOS, rather than silently failing at runtime.
- `AndroidUniversalIPCManager.create()` — throws `NotImplementedError`, must be implemented.
- `IPCManager.android.kt request()` — stub must be replaced with real AIDL/socket request-response.
- `IPCManager.android.kt getConnectedApps()` — empty list must be populated from active connections.

## Deprecated Usage

- None detected.

---

---

# WebSocket Quality Report — 260222

## Summary
SCORE: 62 | HEALTH: YELLOW
FILES: 8 kt | LINES: ~1100 | KMP: yes (commonMain + androidMain + iosMain)

The WebSocket module has a correct and well-structured interface with good reconnection and
keep-alive abstractions. The critical flaw is on Android: `connect()` returns `Result.success(Unit)`
before the OkHttp connection is actually established — the success signal is semantically wrong and
will mislead callers that gate subsequent operations on a successful `connect()` result. iOS has a
related issue where connection state transitions to `Connected` before the receive loop starts.
The `AvuSyncMessage.parse()` method splits on raw `:` without percent-encoding awareness, breaking
URL payload parsing. The third AVU escape implementation in this module diverges from the canonical
`AvuEscape` in the AVU module.

---

## P0 Critical Issues

- **[WebSocketClient.android.kt:141] `connect()` returns success before connection is established.**
  `OkHttpClient.newWebSocket()` is non-blocking. It returns immediately and the actual TCP handshake
  and WebSocket upgrade happen asynchronously. `connect()` returns `Result.success(Unit)` before any
  of this occurs. If the server is unreachable, the failure arrives only in `onFailure()` — after
  the caller has already proceeded. The fix is to use a `CompletableDeferred<Result<Unit>>` that is
  resolved inside `onOpen` (success) and `onFailure` (failure), and suspend until it completes.

---

## P1 High Issues

- **[AvuSyncMessage.kt:285] `parse()` splits on raw `:` without respecting percent-encoded colons.**
  `message.split(":")` at L285 will incorrectly split on colons inside field values. URL payloads
  (e.g., `https://example.com/path`) contain colons after the scheme. A sync message carrying a
  URL will be split into 3+ fragments instead of 2 (prefix + body), corrupting the entire parse.
  Fix: use the same split-aware logic as `AvuIPCParser.splitAvu()` that checks for `%3A` tokens
  before splitting, or use `message.split(":", limit = 2)` and handle percent-decoding after.

- **[WebSocketClient.android.kt:201-211] Connection state not updated during reconnection.**
  `handleDisconnect()` cancels the ping job and starts reconnection, but does NOT emit
  `ConnectionState.Reconnecting` or `ConnectionState.Disconnected` to `_connectionState`
  when `autoReconnect = true`. The state stays at its last value (typically `Connected`) throughout
  the reconnection window. Callers polling `connectionState` will believe the connection is live
  while it is in fact broken.

- **[WebSocketClient.ios.kt:63-84] State set to `Connected` before receive loop starts.**
  `_connectionState.value = Connected` at L75 is emitted before `startReceiving()` is called.
  If `startReceiving()` fails or the `webSocketTask` is nil at that point, the module emits a
  false Connected state. The state must only transition to `Connected` after the receive loop
  is confirmed running.

- **[WebSocketClient.ios.kt:send/sendBinary] Cancellation of `suspendCancellableCoroutine` after callback fires.**
  `NSURLSessionWebSocketTask.sendMessage` delivers its completion block asynchronously. If the
  coroutine is cancelled between sending and the callback, the `resumeWithException(CancellationException)`
  call inside the block can attempt to resume an already-cancelled continuation. This will throw
  `IllegalStateException: Already resumed`. The callback should check `cont.isActive` before resuming.

- **[PersistentSyncQueue.ios.kt:23-28] `save()` runs on `Dispatchers.Main`.**
  Serializing and writing the entire queue to `NSUserDefaults` on the main thread will block UI
  for large queues. iOS has a strict main-thread frame budget. Use `Dispatchers.Default` for
  serialization and restrict the `NSUserDefaults.set()` call to the main thread if required.

- **[PersistentSyncQueue.android.kt:33-38] `save()` throws `IllegalStateException` if `init()` not called.**
  No guard exists — a caller that instantiates `PersistentSyncQueue()` and calls `save()` before
  `init()` will crash. Either initialize lazily on first `save()`, or return `Result.failure(...)`.

- **[ReconnectionManager.kt:141] `KeepAliveManager` uses `System.currentTimeMillis()` directly.**
  `lastPongReceived = System.currentTimeMillis()` bypasses the `expect fun currentTimeMillis()`
  declared in the same file at L189. On JVM this works, but this is KMP code — the intent of the
  `expect` declaration is exactly to abstract this. The `expect` fun must be used consistently.

---

## P2 Medium Issues

- **[AvuSyncMessage.kt] Third AVU escape implementation in the codebase.**
  `escape()` / `unescape()` defined locally at L507-524 duplicate `AvuEscape` (in the AVU module)
  and `unescapeValue()` (in `AndroidIPCManager`). Three independent implementations risk
  divergence. `AvuSyncMessage` should import and use `com.augmentalis.avu.AvuEscape` directly.

- **[SyncQueue.kt:284-286] `generateOperationId()` collision risk.**
  `Clock.System.now().toEpochMilliseconds().toString() + (1000..9999).random()` — under rapid
  concurrent enqueueing, the millisecond granularity plus a 4-digit range gives a collision
  probability of 1/9000 per same-millisecond pair. Use `UUID.randomUUID().toString()`.

- **[SyncQueue.kt] In-memory queue not automatically flushed to `PersistentSyncQueue`.**
  The two types are not linked — callers must manually call `persistentQueue.save()`. On process
  kill, any operations enqueued but not manually saved are lost. The `SyncQueue` should accept
  an optional `PersistentSyncQueue` and auto-flush on enqueue, or on a debounced interval.

- **[WebSocketClient.android.kt:30] `CoroutineScope` not lifecycle-bound.**
  `scope = CoroutineScope(Dispatchers.IO + SupervisorJob())` is created at construction with
  no lifecycle owner. If the `AndroidWebSocketClient` is not `close()`d explicitly, the scope
  (and any running reconnection jobs) will leak indefinitely. Implement `Closeable`.

---

## Code Smells

- **Third AVU escape impl**: `AvuSyncMessage.escape/unescape()` duplicates `AvuEscape`.
- **Async connect misrepresented**: Android `connect()` contract says "returns true if connected" but OkHttp is async.
- **Orphaned `currentTimeMillis` expect**: Declared at L189 of `ReconnectionManager.kt` but not used in the same file.

## Missing Implementations

- No fully missing `expect/actual` pairs — Android, iOS, Desktop actuals all exist for `WebSocketClient`.
- `PersistentSyncQueue.android.kt` requires `init(context)` call — no guard, easy to misuse.

## Deprecated Usage

- None detected.

---

---

# Rpc Quality Report — 260222

## Summary
SCORE: 41 | HEALTH: RED
FILES: ~200 kt | LINES: ~14,000 | KMP: yes (commonMain + androidMain + desktopMain + iosMain stubs)

The Rpc module is architecturally ambitious — gRPC over both TCP and UDS, Wire-generated protos,
cross-platform transport abstraction, registry-aware routing. However the critical path is broken:
`PlatformClient.android.kt` is a 5-method complete stub with empty TODO bodies. This means the
entire `UniversalClient` → `RegistryAwareClient` → `PlatformClient` chain is non-functional on
Android. The `AvaGrpcClient` (UDS/TCP gRPC) and the gRPC server implementations (`VoiceOSGrpcServer`,
`CockpitGrpcServer`) are substantially more complete and form the real production path. Key issues
in those: `AvaGrpcClient.close()` is fire-and-forget causing resource leaks, `CockpitGrpcServer`
has a non-thread-safe listener list and accumulates JVM shutdown hooks on repeated starts, and
the Wire code generation plugin is disabled blocking proto regeneration. The transport layer
(`AndroidTransport`) is well-implemented with proper mutex-protected I/O and `CopyOnWriteArrayList`
for listeners.

---

## P0 Critical Issues

- **[PlatformClient.android.kt:51-79] Five-method stub — entire client abstraction is non-functional.**
  All five methods contain only TODO comments and return nothing:
  - `connectGrpc()` at L51-53: empty body — connection state transitions to `CONNECTED` without any channel established
  - `connectUDS()` at L55-57: empty body
  - `send()` at L64-67: returns `null` unconditionally
  - `request()` at L70-73: returns `ByteArray(0)` unconditionally
  - `receiveStream()` at L76-79: returns `emptyFlow()` unconditionally
  Any Android code that uses `UniversalClient` or `RegistryAwareClient` is operating on dead
  infrastructure. This is the primary implementation gap in the entire Rpc module.

---

## P1 High Issues

- **[AvaGrpcClient.kt:326-328] `close()` is fire-and-forget — resource leak.**
  `close()` calls `scope.launch { disconnect() }` which returns immediately. The channel is not
  shut down before `close()` returns. Any caller that relies on `Closeable` semantics (try-with-resources,
  `use {}` block) will release the `AvaGrpcClient` reference while the channel is still open.
  `EpollEventLoopGroup` threads continue running. Fix: make `close()` call `runBlocking { disconnect() }`
  or provide a separate `suspend fun close()` and document that the Closeable version is best-effort.

- **[AvaGrpcClient.kt:296-323] `startReconnection()` ignores `config.maxRetries`.**
  The reconnection loop hardcodes `val maxAttempts = 10` instead of using `config.maxRetries`.
  `config.maxRetries` (default: 3) is used by `withRetry()` but ignored by the reconnect path.
  These must be consistent. The config value should be used: `val maxAttempts = config.maxRetries`.

- **[CockpitGrpcServer.kt:102] Non-thread-safe `listeners` list.**
  `listeners = mutableListOf<CockpitServerListener>()` — `addListener()` and `removeListener()`
  may be called from any thread, while `updateStatus()` iterates the list from a coroutine on
  `Dispatchers.IO`. Concurrent modification will throw `ConcurrentModificationException`.
  Replace with `CopyOnWriteArrayList<CockpitServerListener>()` or protect all accesses with a mutex.

- **[CockpitGrpcServer.kt:183-186] Shutdown hooks accumulate on repeated `start()` calls.**
  `Runtime.getRuntime().addShutdownHook(Thread { ... })` is called every time `start()` is invoked.
  If the server is stopped and restarted (e.g., port conflict recovery, test teardown/setup), a new
  hook is added each time. The JVM will invoke ALL of them on shutdown. Use a flag or store/remove
  the previous hook reference before adding a new one.

- **[VoiceOSGrpcServer.kt:75-86] Socket file deletion swallows `SecurityException`.**
  `socketFile.delete()` is called without a try/catch for `SecurityException`. On a device where
  the socket path is owned by another process or protected directory, the delete throws, the
  `NettyServerBuilder` proceeds with a stale socket file, and the server silently fails to bind.
  Wrap in try/catch and propagate as `IllegalStateException("Cannot remove stale socket: ...")`.

- **[VoiceOSGrpcServer.kt:57] `start()` is not `suspend` but performs blocking I/O.**
  `server?.start()` (gRPC `Server.start()`) is a blocking call that opens the network socket.
  It should be called from `withContext(Dispatchers.IO)`. As written, if `start()` is called
  from a coroutine on `Dispatchers.Main` or `Dispatchers.Default`, it blocks that dispatcher thread.

- **[CockpitGrpcServer.kt:99-100] Unsynchronized `_status` field.**
  `_status` is a plain `var` read in `getStats()` and `getStatus()` (called from any thread)
  and written in `updateStatus()` (called from a `Dispatchers.IO` coroutine). The write is not
  published with a memory barrier. Mark with `@Volatile` or replace with `AtomicReference`.

- **[UniversalClient.kt:119-122] `connectionState` throws before `connect()` is called.**
  `RegistryAwareClient.connectionState` throws `IllegalStateException("Client not connected — call connect() first")`
  if accessed before `connect()`. A `StateFlow<ConnectionState>` should be safe to observe at any
  point — emitting `Disconnected` is the correct initial state, not throwing. Callers that observe
  connection state before connecting (a common pattern for UI binding) will crash.

---

## P2 Medium Issues

- **[build.gradle.kts:16-18] Wire code generation disabled.**
  The Wire plugin is commented out with "Re-enable when KotlinPoet compatibility issue is resolved."
  All protobuf-generated classes in `src/commonMain/kotlin` are hand-maintained. Any proto schema
  change requires manual edits to generated files — a fragile and error-prone maintenance burden.
  The KotlinPoet compatibility issue should be tracked and resolved; this comment has no associated
  ticket reference or ETA.

- **[AndroidTransport.kt:176-195] Recursive `attemptReconnect()` is not stack-safe.**
  `attemptReconnect()` is a suspend function that calls itself at L192 after a failed reconnection.
  Deep retry chains (e.g., maxRetries = 100) will overflow the call stack. Replace with a
  `repeat(maxRetries)` loop or `tailrec` equivalent in a loop structure.

- **[AndroidTransport.kt:271-273] Graceful disconnect uses `delay(100)` stub.**
  The `graceful = true` path in `disconnect()` calls `delay(100)` and marks the connection closed.
  No flush of the send buffer, no FIN packet, no actual graceful shutdown. Comment should be
  removed or replaced with real drain logic (await pending write mutex release).

- **[AvaGrpcClient.kt:265-279] `createUdsChannel()` does not await old `EpollEventLoopGroup` shutdown.**
  `udsEventLoopGroup?.shutdownGracefully()` returns a `Future` that is not awaited. A new group
  is created immediately. Old Epoll threads may still be running when the new channel tries to use
  the same socket path, causing `address already in use` or event loop contention.
  Call `awaitUninterruptibly()` on the returned future before creating the new group.

- **[CockpitGrpcServer.kt:279] `startTime` visibility not guaranteed.**
  `startTime` is set inside the `startHeartbeatMonitoring()` coroutine (`Dispatchers.IO`) and read
  from `getStats()` which may run on any thread. Without `@Volatile` or `AtomicLong`, the JVM
  memory model does not guarantee `getStats()` sees the written value.

- **[UniversalClient.kt:130-133] Dead code in `RegistryAwareClient.connect()`.**
  The block "For local services, we can use in-process communication / Platform client still
  needed for API consistency" resolves to nothing — the `if (isLocal)` branch has no implementation.
  Either implement in-process communication or remove the dead branch.

- **[VoiceOSGrpcServer.kt:57] Blocking `server.start()` on caller thread — no `Dispatchers.IO` dispatch.**
  As noted in P1, `start()` is synchronous. Minimally add a comment documenting the threading
  requirement. Ideally make `start()` a `suspend fun` and add `withContext(Dispatchers.IO)`.

---

## Code Smells

- **`PlatformClient` all-stub**: The most fundamental piece of the abstraction is a complete no-op.
- **Wire plugin disabled**: Hand-edited generated files will drift from proto definitions.
- **Hardcoded `maxAttempts = 10`**: Ignores `config.maxRetries` — config option has no effect on reconnect.
- **Blocking gRPC start**: `VoiceOSGrpcServer.start()` blocks caller thread without `suspend` or `withContext`.
- **Accumulating shutdown hooks**: `CockpitGrpcServer.start()` adds a new JVM hook on every call.

## Missing Implementations

- `PlatformClient.android.kt` — `connectGrpc()`, `connectUDS()`, `send()`, `request()`, `receiveStream()` — all five must be implemented using the gRPC channel from `AvaGrpcClient` or a direct socket.
- `RegistryAwareClient` in-process local service path — dead branch, never executes.

## Deprecated Usage

- None detected.

---

---

# Cross-Module Findings

## AVU Escaping — Three Divergent Implementations

The most pervasive structural problem across all three modules is the existence of three separate,
incompatible AVU escape/unescape implementations:

| Location | Strategy | Colon | Newline |
|----------|----------|-------|---------|
| `AvuEscape` (AVU module) | Percent-encoding | `%3A` | `%0A` |
| `AndroidIPCManager.unescapeValue()` | Backslash | `\:` | `\n` |
| `AvuSyncMessage.escape/unescape()` | Percent-encoding | `%3A` | `%0A` |

Any message encoded by `AndroidIPCManager` and parsed by `AvuIPCParser` (which uses `AvuEscape`)
will fail to decode backslash-encoded colons — the `%3A` check will not match, colons pass through
unsplit, and fields merge. This is a wire-level incompatibility across the same process.

**Fix**: Delete `AndroidIPCManager.unescapeValue()` and the local `AvuSyncMessage.escape/unescape()`
methods. Route all escaping through `com.augmentalis.avu.AvuEscape`.

## `subscribe<T>()` Unsafe Cast Pattern

All three platform IPC implementations (`androidMain`, `iosMain`, the universal variant) share
the same anti-pattern:

```kotlin
flow.map { it.second } as Flow<T>
```

This unchecked cast compiles without warning but throws `ClassCastException` at collection time
for mismatched types. The fix requires a `KClass<T>` parameter and explicit `filterIsInstance`:

```kotlin
fun <T : IPCMessage> subscribe(type: KClass<T>): Flow<T> =
    messageFlow.map { it.second }.filterIsInstance(type)
```

## Connection State Race on `connect()` Return

Both `AndroidWebSocketClient.connect()` (returns success before `onOpen`) and
`PlatformClient.android.kt connectGrpc()` (sets CONNECTED with empty body) violate the contract
that a successful `connect()` means the transport is ready for use. Callers immediately after
`connect()` should be able to `send()` or `subscribe()` without racing against async channel setup.
All platform `connect()` implementations must suspend until transport readiness is confirmed.
