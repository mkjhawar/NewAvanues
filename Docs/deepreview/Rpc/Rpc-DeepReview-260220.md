# Rpc Module — Deep Code Review
**Date:** 2026-02-20
**Reviewer:** Code-Reviewer Agent
**Scope:** `Modules/Rpc/` — 200 .kt files
**Branch:** HTTPAvanue

---

## Summary

The Rpc module has severe production-readiness problems: three `PlatformClient` actual implementations (Android, iOS, Desktop) are empty stubs that report `CONNECTED` while sending nothing; all eight `PluginServiceGrpcClient` network call methods return hardcoded fake data; and four stub delegate classes (`VoiceOSServiceDelegateStub`, `VoiceCursorServiceDelegateStub`, `VoiceRecognitionServiceDelegateStub`, `VoiceOSServiceDelegateStub` for Plugin) live in production source files rather than test fixtures. Additionally, the entire `android/`, `desktop/`, and `web/` directory layout violates MANDATORY RULE #2 (all platform code must be in `src/{platform}Main/`), and a frame-type bug in `GrpcWebTransport.serverStreamingCall()` causes all streaming data frames to be silently dropped in the gRPC-Web path.

---

## Issues

| # | Severity | File:Line | Issue | Suggestion |
|---|----------|-----------|-------|------------|
| 1 | **Critical** | `src/androidMain/kotlin/.../client/PlatformClient.kt` L51–57 | `connectGrpc()` and `connectUDS()` have empty bodies. `connect()` then unconditionally sets state to `CONNECTED`. Callers believe they are connected but no socket or channel has been opened. `send()` L67 returns null, `request()` L73 returns `ByteArray(0)`, `receiveStream()` L77 returns `emptyFlow()`. The entire Android RPC client path is a no-op. | Implement real connection logic using `ManagedChannelBuilder` (gRPC) or `LocalSocket` (UDS), matching the pattern in `AvaGrpcClient`. Remove the premature `CONNECTED` state assignment. |
| 2 | **Critical** | `src/iosMain/kotlin/.../client/PlatformClient.kt` L51–57 (identical) | Same empty-stub pattern as Android `PlatformClient`. iOS connects to nothing, sends nothing, receives nothing. | Implement using `GrpcWebTransport` or a Ktor-based socket client for iOS KMP targets. |
| 3 | **Critical** | `src/desktopMain/kotlin/.../client/PlatformClient.kt` L51–57 (identical) | Same empty-stub pattern as Android and iOS `PlatformClient`. Desktop target is non-functional. | Implement using `ManagedChannelBuilder` with Netty transport (same as `CockpitGrpcServer`'s client pattern). |
| 4 | **Critical** | `android/Plugin/PluginServiceGrpcClient.kt` L550–615 | 8 private `callXxx()` methods (`callRegisterPlugin`, `callUnregisterPlugin`, `callDiscoverPlugins`, `callGetPluginInfo`, `callSendLifecycleCommand`, `callSubscribeEvents`, `callPublishEvent`, `callHealthCheck`) all return hardcoded fake success responses. The `ManagedChannel` parameter is accepted but never used. Any production call through this client silently returns fake data. Comment reads: "TODO: Use generated gRPC stub". | Generate Wire/protoc gRPC stubs from `plugin.proto`, replace all 8 methods with real stub calls. |
| 5 | **Critical** | `web/WebAvanue/GrpcWebTransport.kt` L169–173 | Frame-type off-by-one bug in `serverStreamingCall()`. After `sliceArray(buffer, totalLength)` at L169, `buffer` has already been advanced past the current frame. `buffer[0]` at L171 reads the **first byte of the next frame's header**, not the type of the frame just extracted. All `GrpcWebFrameType.DATA` comparisons (L172) operate on the wrong byte, causing all data frames to be silently dropped whenever the next frame in the buffer is a trailer (common for the final frame). | Read `frameType` **before** slicing: `val frameType = buffer[0]` must be placed before line 169 (`buffer = sliceArray(buffer, totalLength)`), or capture it as `buffer[0]` from the un-advanced `buffer` before the slice. |
| 6 | **High** | `android/VoiceOS/VoiceOSGrpcServer.kt` L383–460 | `VoiceOSServiceDelegateStub` (10 methods) lives in the production source file, not in a test fixture. `streamEvents()` emits nothing (empty `callbackFlow { awaitClose {} }`). `executeCommand()`, `scrapeCurrentScreen()`, `startVoiceRecognition()` all return hardcoded stub responses. Any code that instantiates this stub instead of a real delegate silently gets fake data at runtime. | Move `VoiceOSServiceDelegateStub` to `src/androidTest/` or a dedicated `test/` source set. If needed for default DI wiring, make this explicit with a `@VisibleForTesting` annotation and document the risk. |
| 7 | **High** | `android/VoiceOS/VoiceCursorGrpcServer.kt` L250–320 (approx) | `VoiceCursorServiceDelegateStub` in production source. `streamPosition()` returns `callbackFlow { awaitClose {} }` — emits no cursor positions. | Same as above: move to test source set. |
| 8 | **High** | `android/VoiceOS/VoiceRecognitionGrpcServer.kt` L280–360 (approx) | `VoiceRecognitionServiceDelegateStub` in production source. Same pattern. | Move to test source set. |
| 9 | **High** | `android/Plugin/PluginServiceGrpcServer.kt` L539–581 | `abstract class PluginServiceGrpc` with all `open fun` bodies empty is hand-written in production code. Comment: "Stub implementation for compilation. This will be replaced by generated code from plugin.proto." It has not been replaced. `PluginServiceImpl` extends this class, meaning all gRPC framework integration (response observer lifecycle, error handling) is missing — the server methods silently succeed and call no observers. | Generate the gRPC base class from `plugin.proto` using Wire or `protoc` + `protoc-gen-grpc-kotlin`. Delete the hand-written stub. |
| 10 | **High** | `desktop/Cockpit/CockpitServiceImpl.kt` L597–598 | `performSync()` contains `kotlinx.coroutines.delay(500)` per data type with explicit comment "Simulate sync work (real implementation would transfer data)". No actual data is transferred. `startSync()` reports success, sync status eventually shows "completed", but no data has moved. | Implement real sync logic using the device's gRPC client (CockpitGrpcServer can reach registered devices via their stored endpoint). |
| 11 | **High** | `desktop/Cockpit/CockpitServiceImpl.kt` L536–555 | `handleSyncCommand()` and `handleScreenshotCommand()` return `CommandResult(success = true, ...)` without performing any actual sync or screenshot capture. The screenshot helper says "Screenshot would be requested from the device client" but the request is never made. | Wire these to real device client calls. The `DeviceRegistry` already holds device connection info. |
| 12 | **High** | `desktop/Cockpit/CockpitServiceImpl.kt` L491 | `streamDeviceEvents()` stores `responseObserver` in `eventObservers` map keyed by `streamId`, but there is no cleanup path when the stream completes normally (only the `catch` block removes the entry). If a stream completes without error, the `StreamObserver` reference is leaked in the map indefinitely. | Add a `finally` block inside the `launchIn` coroutine to call `eventObservers.remove(streamId)` on normal completion. |
| 13 | **High** | `android/Plugin/PluginServiceGrpcClient.kt` L484–495 | `createUdsChannel()` creates `EpollEventLoopGroup()` but never stores or shuts it down. `EpollEventLoopGroup` manages a thread pool; without `shutdownGracefully()` the threads leak. The `disconnect()` path calls `channel.shutdown()` but not `eventLoopGroup.shutdownGracefully()`. | Store the `EpollEventLoopGroup` as a member field, shut it down in `disconnect()`. See `AvaGrpcClient.createUdsChannel()` — same bug exists there at L262. |
| 14 | **High** | `android/AVA/AvaGrpcClient.kt` L262 | `createUdsChannel()` creates `EpollEventLoopGroup()` but does not store or shut it down. Same thread-pool leak as `PluginServiceGrpcClient`. All subclasses (`VoiceOSClient`, `VoiceCursorClient`, `VoiceRecognitionClient`) inherit this leak. | Store `eventLoopGroup` in `AvaGrpcClient` and shut it down in `close()`. |
| 15 | **High** | `android/VoiceOS/VoiceOSGrpcServer.kt` L49, `android/VoiceOS/VoiceCursorGrpcServer.kt`, `android/VoiceOS/VoiceRecognitionGrpcServer.kt` | `ServerConfig.useTls = false` is the default and the `createTcpServer()` path never sets up TLS regardless of the flag. gRPC traffic is plaintext over the network with no mechanism to enable encryption. For remote connections this is a security issue. | Either enforce TLS in `createTcpServer()` when `useTls = true` (using `SslContext`/`SslContextBuilder`), or document that TCP mode is explicitly LAN-only and add a runtime assertion that `useTls` is respected. |
| 16 | **High** | `web/WebAvanue/WebAvanueGrpcClient.kt` (reconnect path) | `handleConnectionFailure()` calls `connect()` (indirectly via retry loop), and `connect()` can call `handleConnectionFailure()` again on failure. At `maxReconnectAttempts`, this is a recursive mutual invocation chain that grows the call stack depth by 1 per attempt. If the reconnect delay is very short or zero, this risks `StackOverflowError`. | Convert the reconnect loop to iterative: use a `while` loop with `delay()` inside a launched coroutine rather than mutual recursion. |
| 17 | **Medium** | `android/`, `desktop/`, `web/` (entire directory trees) | These directories are at the **module root**, not inside `src/{platform}Main/` as required by MANDATORY RULE #2. `android/AVA/`, `android/VoiceOS/`, `android/Plugin/`, `desktop/Cockpit/`, `web/WebAvanue/` are all outside the KMP source set structure. The Gradle build cannot apply platform-specific compiler flags, expect/actual resolution, or source set dependencies correctly. | Migrate: `android/` → `src/androidMain/kotlin/com/augmentalis/rpc/`; `desktop/` → `src/desktopMain/kotlin/com/augmentalis/rpc/`; `web/` → `src/jsMain/kotlin/com/augmentalis/rpc/`. Update `build.gradle.kts` source sets accordingly. |
| 18 | **Medium** | `src/commonMain/kotlin/.../client/UniversalClient.kt` L129–134 | `RegistryAwareClient.connect()` L131: `if (localService != null && localService.isReady())` block is empty. The comment says "Platform client still needed for API consistency" but the in-process optimization (direct method call, no socket) is never implemented. Dead code that misleads readers about a capability that doesn't exist. | Either implement in-process call routing for local services, or remove the empty `if` block and the misleading comment. |
| 19 | **Medium** | `src/commonMain/kotlin/.../ServiceRegistry.kt` L44–58 | `registerLocal()` and `registerRemote()` both perform read-copy-update on `_services.value` without any synchronization. Two concurrent registrations can produce a lost-update race: both read the same `value`, both add their entry to separate copies, the second `value =` assignment overwrites the first registration. | Wrap mutation in a `Mutex` or use `update { }` with `AtomicRef` (expect/actual). For KMP, a `Mutex` from `kotlinx.coroutines` is the correct approach. |
| 20 | **Medium** | `src/commonMain/kotlin/.../client/ClientFactory.kt` | `ClientPool.acquire()`: `currentIndex` is a plain `var` with a comment "Not thread-safe, use proper synchronization in production". The pool is publicly exposed and callers have no way to know it is unsafe. Under concurrent callers, multiple threads can read the same `currentIndex`, get the same client, and use it simultaneously — violating gRPC channel thread-safety assumptions. | Replace `currentIndex` with `AtomicInteger` (JVM/Android) via an `expect/actual`, or synchronize `acquire()` with a `Mutex`. |
| 21 | **Medium** | `src/commonMain/kotlin/.../client/ClientConfig.kt` L182–184 | `RetryStrategy.delayForAttempt()` computes `jitterRange = cappedDelay.inWholeMilliseconds * jitterFactor`. If `jitterFactor` is large enough, `(-jitterRange).toLong()` can be negative and the random range `(-jitterRange..jitterRange)` can produce a negative millisecond value. `Duration.parse("${cappedDelay + negativeJitter}ms")` will throw `IllegalArgumentException` for negative values. | Clamp the jitter addition: `(cappedDelay.inWholeMilliseconds + jitter).coerceAtLeast(0)` before constructing the `Duration`. |
| 22 | **Medium** | `android/VoiceOS/VoiceRecognitionGrpcServer.kt` | `CombinedVoiceOSServer` passes a single `CoroutineScope` to all three service implementations (`VoiceOSServiceImpl`, `VoiceCursorServiceImpl`, `VoiceRecognitionServiceImpl`). A streaming call failure that throws an unhandled exception will cancel the shared `SupervisorJob`, silently terminating all three services. `SupervisorJob` protects the parent but not sibling children within the same scope. | Give each service implementation its own `CoroutineScope(SupervisorJob() + Dispatchers.IO)` so failures in one do not cascade to others. |
| 23 | **Medium** | `src/commonMain/.../IRpcService.kt` and `src/commonMain/.../client/UniversalClient.kt` | `ConnectionState` enum is defined in both `IRpcService.kt` and inside `UniversalClient.kt` (as `ClientConnectionInfo`). Multiple definitions of conceptually identical state enums exist across the codebase (`VoiceOSGrpcServer`, `AvaGrpcClient`, `WebAvanueGrpcClient` all define their own). DRY violation; callers must map between incompatible types. | Consolidate to a single `ConnectionState` in `IRpcService.kt` (already the most accessible location). Remove duplicate definitions and update all callers. |
| 24 | **Medium** | `android/VoiceOS/VoiceOSGrpcServer.kt` L75–78 | `createUdsServer()` deletes the socket file if it exists: `if (socketFile.exists()) socketFile.delete()`. On a controlled shutdown the file is cleaned up, but if the process crashes without cleanup, the next `start()` will succeed silently by deleting the stale file. However, if a different process or user owns the file, `socketFile.delete()` returns `false` silently and the `bind()` call will fail with a confusing error. | Check `socketFile.delete()` return value; throw a descriptive `IOException` if deletion fails. |
| 25 | **Medium** | `android/Plugin/PluginServiceGrpcServer.kt` L490–494 | `parsePort()` returns `toIntOrNull() ?: 50060` — silently falls back to port 50060 for non-numeric addresses with no log or error. Callers receive a server that may be listening on the wrong port. | Log a warning or throw an `IllegalArgumentException` for non-numeric port addresses instead of silently defaulting. |
| 26 | **Medium** | `web/WebAvanue/GrpcWebTransport.kt` L281 | `createRequestInit()` uses `js("({})").unsafeCast<RequestInit>()` — a raw JavaScript object literal cast to a Kotlin type. This is fragile: if the Kotlin/JS bridge changes serialization or if Compose Multiplatform adds stricter type checking, this will break silently at runtime. | Use Kotlin `RequestInit` constructor directly (available in `org.w3c.fetch`) or create a proper `RequestInit` via the standard API rather than relying on `unsafeCast`. |
| 27 | **Medium** | `desktop/Cockpit/CockpitGrpcServer.kt` L265, L279 | `getStats()` at L265 references `startTime` which is declared and initialized at L279 (after the method body). In JVM this is valid (field initialization order), but `getStats()` called between object construction and `start()` will return `uptimeMs = 0` because `startTime` is only set by the heartbeat coroutine launched in `start()`. | Document this pre-start behavior with a comment, or initialize `startTime` in the constructor rather than in `start()`. |
| 28 | **Low** | `android/Plugin/PluginServiceGrpcClient.kt` L548 | Comment block "Stub gRPC Calls (will use generated code)" appears in the production source file. This communicates an intent that has not been fulfilled and will mislead future reviewers into believing this is a known temporary state. | Remove the comment if/when the stub is replaced. Until then, elevate to a `@Deprecated` annotation with `level = DeprecationLevel.ERROR` to make the stub state visible to IDE and build tooling. |
| 29 | **Low** | Multiple files | `TransportBuilder.forService()` at `android/transport/TransportFactory.kt` L317–324 only handles `SERVICE_VOICEOS` and `SERVICE_AVA` for TCP port resolution, falling back to 50060 for all other services. The top-level `TransportFactory.getDefaultPort()` handles 5 services. Incomplete parity. | Unify port resolution into `TransportFactory.getDefaultPort()` and call it from `TransportBuilder.forService()`. |
| 30 | **Low** | `src/commonMain/.../client/UniversalClient.kt` | `RegistryAwareClient.connectionState` throws `IllegalStateException` before `connect()` is called (when `platformClient` is null). Normal pattern would be to return `DISCONNECTED` state. | Return a static `MutableStateFlow(ConnectionState.DISCONNECTED).asStateFlow()` before `connect()` rather than throwing. |

---

## Architecture Issues

### MANDATORY RULE #2 Violation — Non-KMP Directory Structure

The entire module has three non-standard top-level directories:

```
Modules/Rpc/
├── android/          <-- VIOLATION: should be src/androidMain/
│   ├── AVA/          (AvaGrpcClient, VoiceOSClient, VoiceCursorClient, VoiceRecognitionClient)
│   ├── Plugin/       (PluginServiceGrpcServer, PluginServiceGrpcClient)
│   └── VoiceOS/      (VoiceOSGrpcServer, VoiceCursorGrpcServer, VoiceRecognitionGrpcServer)
├── desktop/          <-- VIOLATION: should be src/desktopMain/
│   └── Cockpit/      (CockpitGrpcServer, CockpitServiceImpl, DeviceRegistry)
├── web/              <-- VIOLATION: should be src/jsMain/
│   └── WebAvanue/    (GrpcWebTransport, WebAvanueGrpcClient, WebAvanueServiceClient)
└── src/
    ├── commonMain/
    ├── androidMain/  (PlatformClient, AndroidTransport, TransportFactory)
    ├── iosMain/      (PlatformClient stub)
    └── desktopMain/  (PlatformClient stub)
```

Correct structure per MANDATORY RULE #2:
- `android/AVA/`, `android/Plugin/`, `android/VoiceOS/` → `src/androidMain/kotlin/com/augmentalis/rpc/`
- `desktop/Cockpit/` → `src/desktopMain/kotlin/com/augmentalis/rpc/`
- `web/WebAvanue/` → `src/jsMain/kotlin/com/augmentalis/rpc/`

### Two-Tier Stub Problem

The module has a structural stub problem at two distinct layers:

**Layer 1 — Transport (PlatformClient actuals):**
- Android, iOS, Desktop `PlatformClient` all pretend to connect without opening any socket.
- `UniversalClient` and `RegistryAwareClient` both delegate to `PlatformClient`, so all commonMain RPC logic is non-functional.

**Layer 2 — Service Delegates (VoiceOS/Plugin servers):**
- `VoiceOSServiceDelegateStub`, `VoiceCursorServiceDelegateStub`, `VoiceRecognitionServiceDelegateStub` live in production `.kt` files in `android/VoiceOS/`.
- `PluginServiceGrpcClient` contains 8 fake network call methods in production code.
- These are not in `*Test*` or `*Mock*` source sets where they would be harmlessly unused in production builds.

**Net effect:** Any code path that goes through `UniversalClient` → `PlatformClient` is silently a no-op on all platforms. Separately, any code path that instantiates a `*DelegateStub` at runtime will get fake responses.

### gRPC Base Class Generation Gap

`android/Plugin/PluginServiceGrpcServer.kt` contains a hand-written `abstract class PluginServiceGrpc` (L539–581) because the Wire/protoc generated base class was never added to the build. This is the only service that has this gap — `VoiceOS`, `VoiceCursor`, `VoiceRecognition`, and `Cockpit` all appear to use properly generated base classes (via Wire `*Grpc` generated files in `src/commonMain/kotlin/.../voiceos/`, `cursor/`, etc.).

---

## Stub Inventory (Rpc Module)

| Class | File | Methods Stubbed | Severity |
|-------|------|-----------------|----------|
| `PlatformClient` (Android actual) | `src/androidMain/.../PlatformClient.kt` | `connectGrpc()`, `connectUDS()`, `send()`, `request()`, `receiveStream()` | Critical |
| `PlatformClient` (iOS actual) | `src/iosMain/.../PlatformClient.kt` | Same 5 methods | Critical |
| `PlatformClient` (Desktop actual) | `src/desktopMain/.../PlatformClient.kt` | Same 5 methods | Critical |
| `PluginServiceGrpcClient` | `android/Plugin/PluginServiceGrpcClient.kt` L550–615 | `callRegisterPlugin`, `callUnregisterPlugin`, `callDiscoverPlugins`, `callGetPluginInfo`, `callSendLifecycleCommand`, `callSubscribeEvents`, `callPublishEvent`, `callHealthCheck` | Critical |
| `VoiceOSServiceDelegateStub` | `android/VoiceOS/VoiceOSGrpcServer.kt` L383 | All 10 delegate methods | High |
| `VoiceCursorServiceDelegateStub` | `android/VoiceOS/VoiceCursorGrpcServer.kt` | All delegate methods | High |
| `VoiceRecognitionServiceDelegateStub` | `android/VoiceOS/VoiceRecognitionGrpcServer.kt` | All delegate methods | High |
| `PluginServiceGrpc.PluginServiceImplBase` | `android/Plugin/PluginServiceGrpcServer.kt` L539 | All 8 gRPC handler methods (empty bodies) | High |
| `CockpitServiceImpl.performSync()` | `desktop/Cockpit/CockpitServiceImpl.kt` L597 | `delay(500)` loop, no real transfer | High |
| `CockpitServiceImpl.handleScreenshotCommand()` | `desktop/Cockpit/CockpitServiceImpl.kt` L546 | Returns success without requesting screenshot | High |

---

## What Is Working Well

- **`AvaGrpcClient`** and its subclasses (`VoiceOSClient`, `VoiceCursorClient`, `VoiceRecognitionClient`): solid gRPC client implementation with Mutex-protected channel, exponential backoff, auto-reconnect, proper `SupervisorJob` scoping.
- **`AndroidTransport` / transport layer**: `BaseAndroidTransport`, `UnixDomainSocketTransport`, `TcpSocketTransport`, and their server variants are well-implemented with Mutex guards, `AtomicBoolean`, length-prefix framing, and clean resource lifecycle.
- **`CockpitGrpcServer`**: clean desktop gRPC server with health check, reflection, heartbeat monitoring.
- **`GrpcWebTransport`** (minus the streaming frame-type bug): gRPC-Web framing, retry with exponential backoff, and separation of unary vs. streaming paths is architecturally sound.
- **`DeviceRegistry`**: proper use of `Mutex` + `ConcurrentHashMap`, clean device heartbeat/timeout logic.
- **`RpcEncoder`**: clean delegation to `AvuEscape`, proper validation, no duplicated escape logic.
- **`ServiceRegistry`**: correct use of `MutableStateFlow` for observable service map (concurrency fix needed, but the design is correct).
- **Wire-generated clients** (`GrpcVoiceOSServiceClient`, `GrpcAvaServiceClient`, `GrpcCockpitServiceClient`, `GrpcNLUServiceClient`, `GrpcExplorationServiceClient`, `GrpcAvidCreatorServiceClient`): legitimate generated code, correct use of `GrpcCall` / `GrpcStreamingCall`.
- **Model/DTO files** in `src/commonMain/kotlin/.../ava/`, `cursor/`, `cockpit/`, `nlu/`, `plugin/`, `avid/`, `exploration/`: clean `@Serializable` data classes, sensible defaults, no issues.
- **`TransportFactory`** and **`TransportBuilder`**: good fluent builder API, correct UDS vs. TCP selection logic.

---

## Priority Fix Order

1. **PlatformClient stubs** (Issues #1–3) — the entire `UniversalClient` abstraction is non-functional without these.
2. **PluginServiceGrpcClient stubs** (Issue #4) — 8 fake gRPC call methods in production.
3. **GrpcWebTransport frame-type bug** (Issue #5) — streaming data frames silently dropped.
4. **Stub delegates in production source** (Issues #6–8) — move to test source set.
5. **PluginServiceGrpc hand-written base class** (Issue #9) — generate from proto.
6. **EpollEventLoopGroup leaks** (Issues #13–14) — thread pool leaks on disconnect.
7. **CockpitServiceImpl fake sync** (Issues #10–11) — simulated delay instead of real transfer.
8. **KMP directory structure** (Issue #17) — migrate `android/`, `desktop/`, `web/` to `src/` source sets.
9. **Remaining concurrency issues** (Issues #19–21) — ServiceRegistry race, ClientPool thread-safety, RetryStrategy negative duration.

---

## Test Coverage

No test files were found under `Modules/Rpc/src/*/test/`. The entire module has zero unit or integration tests. Given the number of stubs and the complexity of transport/framing logic, minimum required coverage should include:

- `RpcEncoder`: encode/decode round-trip, delimiter escaping, `isValidMessage` edge cases.
- `AndroidTransport`: length-prefix framing correctness (truncated reads, multi-chunk reassembly).
- `GrpcWebTransport`: frame parsing for both DATA and TRAILER frame types, multi-frame buffers.
- `RetryStrategy.delayForAttempt()`: boundary values at attempt=1, attempt=maxAttempts+1.
- `ServiceRegistry`: concurrent `registerLocal` + `registerRemote` calls (once race condition is fixed).
- `ClientPool.acquire()`: round-robin ordering, empty pool behavior.

---

*Report covers all 200 .kt files in `Modules/Rpc/`. Wire-generated client files (`Grpc*ServiceClient.kt`) reviewed at architecture level only — generated code not subject to manual style review.*
