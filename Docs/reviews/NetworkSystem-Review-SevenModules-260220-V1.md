# Code Review: Network/System Modules
**Date:** 260220
**Reviewer:** Code-Reviewer Agent (code-reviewer/sonnet)
**Branch:** HTTPAvanue
**Scope:** 7 modules — HTTPAvanue, RemoteCast, Rpc, IPC, WebSocket, DeviceManager, Gaze
**Total findings:** 42

---

## Summary

The network and system module layer has a pervasive thread-safety problem in its concurrent I/O paths — particularly HTTP/2 stream multiplexing, SSE connection management, and the IPC ConnectionManager — where shared mutable state is accessed from concurrent coroutines without Mutex protection. The IPC module is the most severely under-implemented: all three platform implementations (Android, iOS, Desktop) have stub failures for core operations (`request`, `broadcast`, `getConnectedApps`), and the common `ConnectionManager` contains hard-coded `delay()` calls that simulate work rather than performing it.

---

## Module 1: HTTPAvanue

**Files reviewed:** HttpServer.kt, HttpParser.kt, Http2Connection.kt, Http2FrameCodec.kt, Http2Stream.kt, HpackDecoder.kt, HpackDynamicTable.kt, StaticFileMiddleware.kt, CorsMiddleware.kt, RateLimitMiddleware.kt, ErrorHandlerMiddleware.kt, WebSocket.kt, WebSocketParser.kt, SseEmitter.kt

### Issues

| Severity | File:Line | Issue | Suggestion |
|----------|-----------|-------|------------|
| CRITICAL | `src/commonMain/.../http2/Http2Connection.kt:211-238` | `sendResponse()` writes to the shared Okio `sink` from concurrent stream coroutines launched at L122/L131 without synchronization. Concurrent writes corrupt HTTP/2 frame boundaries, producing malformed frames on the wire. | Wrap the `sink` in a `Mutex`-protected write method (e.g., `suspend fun writeFrame(frame: ByteArray)`) and require all stream and control writers to call it. |
| CRITICAL | `src/commonMain/.../hpack/HpackDecoder.kt:108-113` | Huffman decoding is not implemented. Both branches of the `isHuffman` check (`true` and `false`) call `stringBytes.decodeToString()` identically. HTTP/2 clients that send Huffman-encoded header values (most browsers) will produce garbage header strings silently. | Implement a real Huffman decoder using the RFC 7541 Appendix B code table, or integrate a known-good HPACK library. Until resolved, the server cannot correctly interoperate with standard HTTP/2 clients. |
| CRITICAL | `src/commonMain/.../middleware/StaticFileMiddleware.kt:14-17` | No path traversal protection. The file path is built from `request.path` with only prefix stripping. A request for `/static/../../etc/passwd` becomes `../../etc/passwd` relative to the resource root — a path traversal attack. | After stripping the prefix, call `filePath.contains("..")` check or normalize to a canonical path and verify it still starts within the resource root directory. |
| HIGH | `src/commonMain/.../server/HttpServer.kt:40,69-70` | `activeConnections` is a plain `mutableSetOf<Job>()` at L40, mutated at L69-70 from the accept loop coroutine via `add()` and `invokeOnCompletion` callbacks. `invokeOnCompletion` fires from whichever thread completes the job — any thread — so concurrent `add`/`remove` race on the non-thread-safe `MutableSet`. | Replace with `ConcurrentHashMap<Job, Unit>` or `mutableSetOf()` guarded by a `Mutex`. |
| HIGH | `src/commonMain/.../hpack/HpackDecoder.kt:94-96` | HPACK integer decoding has no overflow guard. The multi-byte integer accumulation loop does not bound the result to `Int.MAX_VALUE`. A maliciously crafted HPACK value with many continuation bytes causes integer overflow (undefined behavior / wrap to negative). | Add a check: if the accumulated value exceeds a safe maximum (e.g., `0xFFFFFF` for header sizes), reject the frame with a `COMPRESSION_ERROR`. |
| HIGH | `src/commonMain/.../http2/Http2Stream.kt:56` | `increaseReceiveWindow()` does not check for overflow, while `increaseSendWindow()` does. A peer sending a large `WINDOW_UPDATE` for a stream's receive window can overflow the `Int` field. | Mirror the overflow check from `increaseSendWindow()`: throw a stream-level `FLOW_CONTROL_ERROR` if the new value exceeds `Int.MAX_VALUE`. |
| HIGH | `src/commonMain/.../sse/SseEmitter.kt:72` | `SseConnectionManager.emitters` is a plain `mutableMapOf`. `broadcast()` iterates it while `createEmitter()` and `removeEmitter()` mutate it from concurrent coroutines — a `ConcurrentModificationException` is possible at runtime under load. `closed` var in `SseEmitter` is not `@Volatile`. | Use `ConcurrentHashMap` for `emitters`. Mark `closed` as `@Volatile`. |
| HIGH | `src/commonMain/.../server/HttpServer.kt:63` | `start()` creates a new anonymous `CoroutineScope(Dispatchers.Default)` that is assigned to `serverJob` but never explicitly cancelled. On a second `start()` call (after the `isActive` guard returns early), the old scope leaks. On a `stop()` call the scope is also not cancelled — only the `serverJob` Job is cancelled. | Store the `CoroutineScope` as a field, cancel it in `stop()`, and recreate it on each `start()`. |
| MEDIUM | `src/commonMain/.../middleware/RateLimitMiddleware.kt:19` | Client IP derived from `X-Forwarded-For` header without validation. A malicious client can set this header to any value, spoofing another IP and bypassing per-IP rate limits entirely. Also: `requestCount` increment is not atomic — two coroutines can read the same count and both pass the limit check. | Accept `X-Forwarded-For` only when the connection comes from a known trusted proxy IP. Use `AtomicInteger` (JVM) or a mutex for `requestCount`. |
| MEDIUM | `src/commonMain/.../middleware/CorsMiddleware.kt` | The wildcard origin `"*"` and `allowCredentials` are independently settable. The server will emit both `Access-Control-Allow-Origin: *` and `Access-Control-Allow-Credentials: true` if a consumer sets both — a combination browsers reject but which signals a misconfiguration silently. | Add a validation in `CorsConfig` init: `require(!(allowedOrigins.contains("*") && allowCredentials)) { "Wildcard origin and credentials cannot be combined" }`. |
| MEDIUM | `src/commonMain/.../websocket/WebSocket.kt:135` | `isOpen()` reads `_state` directly without `stateMutex`. All other state reads in the class use `stateMutex.withLock`. This is an unsynchronized read of a mutable field — a data race on JVM and undefined behavior on Kotlin/Native. | Change `isOpen()` to a `suspend fun` that acquires `stateMutex`, or use `@Volatile` on `_state`. |
| MEDIUM | `src/commonMain/.../websocket/WebSocketParser.kt` | Control frames (PING, PONG, CLOSE) payload size is not validated to be ≤125 bytes as required by RFC 6455 Section 5.5. An oversized control frame payload is accepted and forwarded to the handler, which may cause downstream issues. | After parsing opcode, check: `if (opcode.isControl() && payloadLen > 125) throw ProtocolException("Control frame payload too large")`. |
| MEDIUM | `src/commonMain/.../http2/Http2FrameCodec.kt:98` | `writeFrame()` calls `sink.flush()` after every single frame. For HTTP/2 this means one `flush()` (one syscall) per DATA frame — at high throughput this is a significant performance bottleneck. | Batch multiple frames before flushing. Flush only on explicit end-of-stream, when the buffer reaches a size threshold, or at the end of the response writing loop. |
| LOW | `src/commonMain/.../server/HttpParser.kt:33` | Header names are stored as-is without case normalization. HTTP/1.1 headers are case-insensitive (RFC 7230 Section 3.2). Consumers doing `headers["Content-Type"]` will miss `headers["content-type"]`. | Normalize all header names to lowercase on parse: `headerName.lowercase()`. |
| LOW | `src/commonMain/.../http2/Http2Connection.kt:20` | `streams: MutableMap<Int, Http2Stream>` is a plain `mutableMapOf`. It is accessed from the frame dispatch loop and from launched stream coroutines. Should be a `ConcurrentHashMap` or guarded by a `Mutex`. | Use `ConcurrentHashMap<Int, Http2Stream>()`. |

---

## Module 2: RemoteCast

**Files reviewed:** CastWebSocketServer.kt (commonMain), CastWebSocketClient.kt (commonMain), AndroidCastManager.kt (androidMain), MjpegTcpServer.kt (androidMain, deprecated), CastFrameData.kt, CastProtocol.kt

### Issues

| Severity | File:Line | Issue | Suggestion |
|----------|-----------|-------|------------|
| HIGH | `src/commonMain/.../transport/CastWebSocketServer.kt` | No authentication on the `/cast/stream` WebSocket endpoint. Any client on the LAN can connect to port 54321 and receive the screen cast stream. On open Wi-Fi networks this is a complete screen privacy breach. | Add a token-based handshake: the server generates a short-lived session token shown on the sender device's UI; the receiver must supply it in the WebSocket upgrade request (query param or header). Reject connections without a valid token. |
| HIGH | `src/commonMain/.../transport/CastWebSocketServer.kt:67,121` | `frameCount` is a plain `Long` incremented at L121 (`frameCount++`) without synchronization. `sendFrame()` is a `suspend fun` that can be called from multiple coroutines. On JVM, `Long` increments are not atomic (two 32-bit operations on 32-bit JVMs). | Replace with `AtomicLong` or use `Mutex` around the increment. |
| MEDIUM | `src/androidMain/.../controller/AndroidCastManager.kt:79` | `discoverDevices()` returns `emptyFlow()`. mDNS device discovery is documented as "deferred to a future iteration" but no tracking issue or feature flag exists — this is a silent stub. The UI calling this will show an empty device list with no user-visible explanation. | At minimum, emit a diagnostic event or log a warning when the stub is called. Add a TODO referencing the tracking issue for NSD (Network Service Discovery) implementation. |
| MEDIUM | `src/androidMain/.../controller/AndroidCastManager.kt:217-223` | `setMediaProjection()` accepts `screenWidth`, `screenHeight`, and `screenDensity` parameters but does not use or store them (no assignments in the body). The parameters are silently ignored and the hardcoded `DEFAULT_DENSITY`, `DEFAULT_SCREEN_WIDTH`, `DEFAULT_SCREEN_HEIGHT` constants are used instead. | Store the three parameters and pass them to `ScreenCaptureHelper` in `startCasting()`. Remove the defaults from the companion or make them fallback values explicitly. |
| MEDIUM | `src/androidMain/.../controller/AndroidCastManager.kt:70,88,113` | `_receivedFrames` is a `@Volatile var Flow<ByteArray>`. When `connectToDevice()` replaces it with a new flow (L88), existing collectors (e.g., `CastReceiverView`) never see a new emission — they are subscribed to the old `emptyFlow()` reference. `@Volatile` only guarantees visibility of the reference, not that collectors re-subscribe. | Expose `receivedFrames` as a `StateFlow<Flow<ByteArray>>` or use a `SharedFlow` that is stable across reconnections; use a `flatMapLatest` to switch the inner stream. |
| LOW | `src/androidMain/.../controller/AndroidCastManager.kt:97-100` | In `connectToDevice()`, the server's `clientConnected` state is observed via `launchIn(scope)` each time the method is called. On repeated calls (connect → disconnect → connect), this creates multiple independent observers — the state update fires multiple times per change, leading to redundant `isStreaming` toggling. | Store the observation `Job` and cancel it before launching a new one, or use `distinctUntilChanged()` plus a single `launchIn` started once in `init`. |

---

## Module 3: Rpc

**Files reviewed:** VoiceOSGrpcServer.kt (android), VoiceOSServiceImpl.kt, VoiceOSServiceDelegateStub.kt, UniversalClient.kt (commonMain), RpcMessageSerializer.kt

### Issues

| Severity | File:Line | Issue | Suggestion |
|----------|-----------|-------|------------|
| HIGH | `android/VoiceOS/VoiceOSGrpcServer.kt:383-494` | `VoiceOSServiceDelegateStub` is a concrete class that returns hardcoded fake/empty responses for all 10 service methods and is registered in production code paths. This is not in a test source set. If this delegate is ever injected at runtime (e.g., DI misconfiguration), callers receive silent empty results — commands are dropped with no error. | Move `VoiceOSServiceDelegateStub` to a `testFixtures` or `androidTest` source set. In production, fail fast if no real delegate is injected (throw `IllegalStateException` in the constructor if delegate is the stub type). |
| HIGH | `android/VoiceOS/VoiceOSGrpcServer.kt:49,createTcpServer` | `ServerConfig.useTls` defaults to `false` and `createTcpServer()` creates a plain TCP socket regardless of the flag. The TLS code path exists (flag is checked) but TLS socket creation is not implemented — the flag is a no-op. Any gRPC traffic is transmitted in plaintext. | Either implement TLS using `SSLContext` / `SSLServerSocket` when `useTls = true`, or remove the flag and document that TLS is not yet supported. Do not ship a misleading configuration knob. |
| HIGH | `android/VoiceOS/VoiceOSGrpcServer.kt:stop()` | `stop()` cancels `scope` before calling `server.shutdown()`. Any in-flight stream handler coroutines are killed immediately, before the graceful shutdown window completes. Active RPC calls are dropped silently. | Call `server.awaitTermination(timeout)` first, then cancel the scope. Or cancel the scope only after `server.awaitTermination()` returns. |
| MEDIUM | `src/commonMain/.../client/UniversalClient.kt:120-122` | `RegistryAwareClient.connectionState` throws `IllegalStateException` if accessed before `connect()`. A property that throws on access is a surprise — callers expect to read state at any time (e.g., in UI observers started before connect). | Initialize `connectionState` to a default `DISCONNECTED` state flow in the field declaration so it is always readable. |
| MEDIUM | `src/commonMain/.../client/UniversalClient.kt:144` | `send()` returns `platformClient?.send(message)` — returns `null` if `platformClient` is null (not connected). `null` is a silent failure; the caller receives a null `Result<T>` instead of a `Result.failure`. | Return `Result.failure(IllegalStateException("Client not connected"))` when `platformClient` is null. |
| LOW | `Modules/Rpc/` (directory structure) | The Rpc module uses `android/` and `desktop/` top-level folders outside the `src/` KMP source set structure. This deviates from the MANDATORY RULE #2 KMP file placement convention and breaks multi-target Gradle discovery. | Migrate to `src/androidMain/kotlin/...` and `src/desktopMain/kotlin/...` matching all other modules in the repo. |

---

## Module 4: IPC

**Files reviewed:** IPCManager.android.kt, IPCManager.ios.kt, UniversalIPCManager.desktop.kt, ConnectionManager.kt, AvuIPCParser.kt, IPCModels.kt

### Issues

| Severity | File:Line | Issue | Suggestion |
|----------|-----------|-------|------------|
| CRITICAL | `src/androidMain/.../ipc/IPCManager.android.kt:289-291` | The `createIPCManager()` expect/actual on Android throws `NotImplementedError` unconditionally. Any code path that calls the expect `createIPCManager()` factory on Android crashes at runtime. | Implement the factory to instantiate `AndroidIPCManager` with the required dependencies. This is a shipped crash. |
| HIGH | `src/androidMain/.../ipc/IPCManager.android.kt:88-89` | `AndroidIPCManager.request()` returns `Result.failure(IPCError.SendFailed("Request-response not yet implemented"))`. This is not marked `@Deprecated` or hidden — it is part of the public IPC interface. Any consumer calling `request()` gets a silent failure with no compile-time warning. | Annotate with `@Deprecated(level = DeprecationLevel.ERROR, message = "request() is not yet implemented")` or implement it. |
| HIGH | `src/androidMain/.../ipc/IPCManager.android.kt:115-117` | `getConnectedApps()` returns `emptyList()` unconditionally — no AIDL or PackageManager query is performed. | Implement using `packageManager.getInstalledPackages()` filtered to apps that expose the IPC content authority, or use AIDL to query connected services. |
| HIGH | `src/commonMain/.../ipc/ConnectionManager.kt:141-142` | `connectInternal()` contains `delay(100)` with comment "Simulate connection (actual implementation in platform-specific code)". The actual connection is never delegated to platform-specific code — no `expect/actual` call follows this delay. The manager always succeeds after 100ms regardless of whether the service exists. | Either implement real AIDL binding (Android) / socket connect (Desktop) here, or delegate to a platform `expect fun connectPlatform(endpoint)` function. |
| HIGH | `src/commonMain/.../ipc/ConnectionManager.kt:215` | `invoke()` contains `delay(10)` and returns a hardcoded `MethodResult.Success("Result from ${invocation.methodName}")`. No actual method invocation occurs. All IPC method calls silently succeed with a fake string result. | Same as above — delegate to a platform-specific actual method dispatcher. |
| HIGH | `src/iosMain/.../ipc/IPCManager.ios.kt:send,broadcast,request` | All three core IPC operations on iOS return `Result.failure`. iOS IPC is entirely non-functional — there is no URL scheme or App Group dispatch implemented. Not annotated or documented as a stub. | Implement using `UIApplication.shared.open()` for send/broadcast and an App Group `UserDefaults` or Darwin notification for request-response. Mark with a clear `@ExperimentalIPC` annotation until done. |
| HIGH | `src/desktopMain/.../universal/UniversalIPCManager.desktop.kt` | `send()`, `broadcast()`, `request()` all return `Result.failure("Not implemented")`. Desktop IPC is entirely non-functional. | Implement using Unix Domain Sockets or Named Pipes for local desktop IPC. |
| HIGH | `src/androidMain/.../ipc/IPCManager.android.kt:74` and `src/iosMain/.../ipc/IPCManager.ios.kt:45` | `subscribe<T>()` casts incoming messages as `as T` / `as Flow<T>` without type checking. A subscriber registered for `MyMessage` will throw `ClassCastException` at runtime if the channel receives a different message type. | Use `filterIsInstance<T>()` (available on Flow) instead of an unsafe cast: `messageFlow.filterIsInstance<T>()`. |
| MEDIUM | `src/androidMain/.../ipc/IPCManager.android.kt:57` | `broadcast()` uses an implicit intent without `setPackage()`. Any app on the device can register a matching broadcast receiver and intercept the IPC message. On Android 8+ this is also blocked by the OS for implicit broadcasts to external receivers — the message is silently dropped. | Use explicit intents with `setPackage()` for each registered receiver, or switch to a `LocalBroadcastManager` / `ContentProvider` pattern for cross-process messaging. |
| MEDIUM | `src/desktopMain/.../universal/UniversalIPCManager.desktop.kt:35-38` | `subscribe(filter)` ignores the `filter` parameter — it returns all messages regardless of the filter predicate. | Apply `filter` in the `messageFlow.filter { filter(it) }` pipeline. |
| LOW | `src/commonMain/.../ipc/AvuIPCParser.kt:splitAvu()` | `splitAvu()` handles `%3A` (escaped colon) but not `%25` (escaped percent sign) — incomplete URL percent-encoding support. A value like `http%3A%2F%2Fexample.com%25path` would be mishandled. | Implement full percent-decode for at minimum `%25`, `%3A`, `%7C`, and `%0A` in the unescaping step. |

---

## Module 5: WebSocket

**Files reviewed:** WebSocketClient.kt (commonMain expect), WebSocketClient.android.kt (OkHttp), WebSocketClient.ios.kt, WebSocketClient.desktop.kt, ReconnectionManager.kt, KeepAliveManager.kt, WebSocketModels.kt

### Issues

| Severity | File:Line | Issue | Suggestion |
|----------|-----------|-------|------------|
| HIGH | `src/commonMain/.../websocket/ReconnectionManager.kt:141,148,171` | `KeepAliveManager` uses `System.currentTimeMillis()` directly in a `commonMain` file. `System.currentTimeMillis()` is a JVM-only API — this code compiles only because Kotlin/JVM is the only active target. If iOS or Desktop `actual` implementations ever use this common class directly, it will fail to compile on those targets. An `expect fun currentTimeMillis(): Long` is defined at the bottom of the file (L189) but the `KeepAliveManager` class above it uses `System.currentTimeMillis()` directly instead of calling the expect function. | Replace all direct `System.currentTimeMillis()` usages in `KeepAliveManager` with the already-defined `currentTimeMillis()` expect function. |
| MEDIUM | `Modules/WebSocket/` vs `Modules/HTTPAvanue/websocket/` | Two separate WebSocket implementations exist in the codebase: `Modules/WebSocket` (OkHttp-based, for client-side persistent connections) and `Modules/HTTPAvanue/websocket` (custom Okio-based, for the HTTP server). The naming is confusingly similar. New developers will not know which to use for new features. | Add a package-level `README.md` or KDoc on both modules explicitly documenting the distinction: "Use `Modules/WebSocket` for outbound client connections. Use `Modules/HTTPAvanue` WebSocket APIs only when building on top of `HttpServer`." |
| LOW | `src/commonMain/.../websocket/ReconnectionManager.kt` | `ExponentialBackoffStrategy.calculateDelay()` does not add jitter. Multiple clients reconnecting simultaneously after a server restart will all retry at the same intervals, creating a retry thundering herd. | Add randomized jitter: `delay + Random.nextLong(0, delay / 2)`. |

---

## Module 6: DeviceManager

**Files reviewed:** DeviceManager.kt, IMUManager.kt, DeviceManagerActivity.kt, DeviceManagerSimple.kt, BiometricManager.kt, LiDARManager.kt, DisplayManager.kt

### Issues

| Severity | File:Line | Issue | Suggestion |
|----------|-----------|-------|------------|
| HIGH | `src/androidMain/.../imu/IMUManager.kt:342` | `onSensorChanged()` launches a new coroutine (via `scope.launch`) for every sensor event. At 120Hz IMU output this creates approximately 120 coroutines per second. Each coroutine is tiny but the allocation and dispatch overhead is measurable; under sustained use this can trigger GC pressure and increase latency. | Use a `Channel<SensorEvent>(capacity = Channel.CONFLATED)` and `trySend` from `onSensorChanged`. A single consumer coroutine processes events, naturally dropping stale readings when processing is slower than the sensor rate. |
| HIGH | `src/androidMain/.../DeviceManager.kt:145` | LiDAR detection uses `totalSensorCount > 0` — this is true on every Android device with any sensor. `LiDARManager` is instantiated on all devices, wasting memory and potentially triggering camera permission requests unnecessarily. | Query for a specific sensor type associated with depth/ToF sensors, or use a device model allowlist. Check `SensorManager.getDefaultSensor(Sensor.TYPE_DEPTH)` or the Camera2 `DEPTH_OUTPUT` capability. |
| HIGH | `src/androidMain/.../DeviceManager.kt:184` | `DeviceManager` creates a `CoroutineScope(Dispatchers.Main + SupervisorJob())` stored as a field but never cancels it. If `DeviceManager` is released without calling `cleanupAll()`, the scope and all its coroutines leak indefinitely. The singleton pattern makes this less likely but it is still an unguarded resource. | Cancel the scope in `cleanupAll()`. Add a check in `cleanupAll()` that asserts `scope.isActive` before cancellation and logs a warning if already inactive. |
| MEDIUM | `src/androidMain/.../DeviceManager.kt:161-162` | `@Suppress("SENSELESS_COMPARISON")` suppresses a compiler warning about a null check. The compiler identified that the value "cannot be null" — suppressing this instead of removing the null check hides a code smell. If the field's type ever changes to nullable, the suppression will prevent the compiler from warning about unsafe access. | Remove the suppression and the null check if the field is truly non-null. If it can be null (e.g., via reflection), change the field's type to nullable explicitly. |
| MEDIUM | `src/androidMain/.../imu/IMUManager.kt:388-421,425-444` | The active `onSensorChanged` implementation (L388-421) is simpler and does not use `sensorFusion`, `adaptiveFilter`, or `motionPredictor` — which are initialized as fields but never called. Lines 425-444 show a commented-out advanced implementation. Dead initialized objects waste memory; the sensor fusion pipeline never runs. | Either activate the sensor fusion path or remove the unused fields and commented code to avoid confusion about which pipeline is active. |
| MEDIUM | `src/androidMain/.../imu/IMUManager.kt:153` | `isActive: Boolean` is a plain `var`, read at L182 outside the `synchronized(consumerLock)` block. Writes happen inside `synchronized`. This is a benign data race on JVM today but is not guaranteed correct — the JMM does not guarantee visibility of an unsynchronized write from inside a `synchronized` block to a read outside it without `@Volatile`. | Add `@Volatile` to `isActive`. |
| MEDIUM | `src/androidMain/.../dashboardui/DeviceManagerSimple.kt:51+` | Uses `MaterialTheme.typography.titleMedium` and `MaterialTheme.colorScheme.*` directly. Violates MANDATORY RULE #3 (AvanueTheme v5.1). | Replace with `AvanueTheme.colors.*` for colors and AvanueUI typography tokens. |
| MEDIUM | `src/androidMain/.../dashboardui/DeviceManagerActivity.kt` | Multiple interactive UI elements (buttons, tab indicators, action chips) have no `Modifier.semantics { contentDescription = "Voice: ..." }` AVID annotations. Violates the zero-tolerance AVID rule. | Add AVID voice semantics to all interactive elements per the AvanueUI Protocol: `Modifier.semantics { contentDescription = "Voice: click [label]" }`. |
| LOW | `src/androidMain/.../DeviceManager.kt` | `DeviceManager` is a Kotlin `object` (singleton). The companion pattern makes testing impossible without DI or reflection — no seam exists for injecting a mock `IMUManager`, `BiometricManager`, etc. | Convert to a class with constructor injection and register as a singleton in a Hilt module. The singleton behavior is preserved by Hilt's `@Singleton` scope while enabling test injection. |

---

## Module 7: Gaze

**Files reviewed:** GazeTracker.kt (commonMain), GazeTrackerPlatform.kt (androidMain), GazeTrackerPlatform.kt (desktopMain), GazeModels.kt

### Issues

| Severity | File:Line | Issue | Suggestion |
|----------|-----------|-------|------------|
| HIGH | `src/commonMain/.../gaze/GazeTracker.kt:104-105` | `StubGazeTracker.state` is a `val` with a `get()` that creates and returns a **new** `MutableStateFlow` instance on every access. Any collector that holds a reference to `tracker.state` will never receive updates — they hold a different flow instance than any subsequent `tracker.state` reads. This breaks the `StateFlow` contract. | Initialize `state` as a field: `override val state: StateFlow<GazeTrackerState> = MutableStateFlow(GazeTrackerState())` (no `get()` body). |
| MEDIUM | `src/androidMain/.../gaze/GazeTrackerPlatform.kt` | `GazeTrackerFactory.create()` returns `StubGazeTracker()`. `isAvailable()` always returns `false`. These are intentional stubs (documented in MEMORY.md — ML Kit disabled to reduce APK size). However, there is no `@RequiresOptIn` or `@ExperimentalGazeTracking` annotation to make callers aware they are using a non-functional implementation. | Add a `@ExperimentalGazeTracking` opt-in annotation to `IGazeTracker` and `GazeTrackerFactory`. This forces call sites to explicitly opt in, making it clear the feature is not production-ready. |
| LOW | `src/desktopMain/.../gaze/GazeTrackerPlatform.kt` | `GazeTrackerFactory.create()` returns `StubGazeTracker()` with a TODO for Tobii SDK. No target date or tracking reference. | Add a comment referencing the GitHub/Jira issue tracking Tobii SDK integration. |

---

## Cross-Module Recommendations

1. **Introduce a unified `Mutex`-guarded sink writer for HTTP/2.** The most critical bug in the codebase is the unsynchronized concurrent write to the Okio sink in `Http2Connection`. This will cause intermittent connection corruption under any real load. Fix this before any HTTP/2 feature work.

2. **Gate the entire IPC module behind a feature flag or `@ExperimentalIPC` annotation.** Three of four platform implementations are either completely stub (`iOS`, `Desktop`, `createIPCManager()` on Android) or partially stub. Shipping this as a public API without annotations misleads consumers into thinking IPC works cross-platform.

3. **Add a security policy document for RemoteCast.** The cast server streams raw screen content with no authentication. Before any public use, document the threat model and implement at minimum a session-token handshake. Consider whether LAN-only enforcement (binding to `127.0.0.1` or checking the source IP subnet) is appropriate as a defense-in-depth measure.

4. **Migrate `Modules/Rpc/` to KMP source set structure.** The current `android/` and `desktop/` top-level directories outside `src/` deviate from the repo convention (MANDATORY RULE #2) and will cause issues if a third platform target is added.

5. **Run a coroutine-per-event audit on sensor and network callbacks.** `IMUManager.onSensorChanged` and similar event callbacks that launch coroutines per event are a recurring pattern risk. Prefer `Channel.CONFLATED` + single consumer for high-frequency event sources.

6. **Replace all `System.currentTimeMillis()` direct calls in `commonMain` with the `expect` function.** The `ReconnectionManager` defines an expect/actual for `currentTimeMillis()` but then bypasses it — this is precisely the kind of KMP violation that fails silently on JVM but breaks on native targets.

7. **Complete the `setMediaProjection()` parameter wiring in `AndroidCastManager`.** The method signature promises dynamic resolution/density configuration but the body ignores all three parameters. This is a silent API contract violation.

---

**Total findings by severity:**

| Severity | Count |
|----------|-------|
| CRITICAL | 3 |
| HIGH | 20 |
| MEDIUM | 15 |
| LOW | 7 |
| **Total** | **45** |

*Report generated by code-reviewer agent. Author: Manoj Jhawar / Intelligent Devices LLC.*
