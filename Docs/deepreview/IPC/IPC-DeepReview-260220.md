# IPC Module — Deep Code Review
**Date:** 260220
**Reviewer:** Code-Reviewer Agent
**Scope:** `Modules/IPC/src/` — all 31 .kt files
**Module purpose:** Unified inter-process communication layer for the Avanues ecosystem (Android, iOS, Desktop); AVU protocol codec; AIDL service binding; Content Provider access; UI component DSL over IPC.

---

## Summary

The IPC module is architecturally ambitious — it defines a KMP-first universal messaging layer (AVU protocol), an AIDL service connector, a Content Provider connector, a compact UI-component DSL (DSLSerializer), and a file parser for all Avanues file formats (.ava, .vos, .avc, etc.). The foundations — `IPCMessages.kt`, `UniversalDSL.kt`, `AvuIPCParser.kt`, `AvuIPCSerializer.kt`, `UniversalFileParser.kt`, `ServiceConnector.android.kt`, `ContentProviderConnector.android.kt` — are largely well-implemented, with correct escape handling, clear sealed-class hierarchies, and proper KMP expect/actual structure. The test coverage in `UniversalFileParserTest.kt` is thorough.

However, the module has a critical structural defect: **duplicate conflicting type definitions**. `UniversalIPCManager.kt` (in the `universal` sub-package) defines its own `MessageFilter`, `MessageType`, and `IPCError` sealed classes, which directly conflict with identical-purpose types in `IPCModels.kt` and `IPCErrors.kt`. The two `IPCError` types have different variant names and signatures, making them silently incompatible — code importing from one sub-package will not type-check against code importing from the other. The Android implementation of `IPCManager` (`IPCManager.android.kt`) has four stub methods (request-response, register/unregister system registry, `getConnectedApps`) that return failures or empty results, and the `actual fun createIPCManager()` factory throws `NotImplementedError`. The iOS `iOSIPCManager` similarly cannot send or receive any real messages. `ConnectionManager.kt` (commonMain) contains hardcoded simulation delays, stub connection handles, and at least eight locations where the `connections` map is read without holding the mutex. The AVU codec metrics mapping in both `AvuIPCParser` and `AvuIPCSerializer` swaps field semantics (bytes vs. messages, connections vs. requests). `ServiceConnector.android.kt`'s `invoke()` returns a stub success string without executing the actual AIDL method. The iOS and Desktop `UniversalIPCManager` implementations always return `SendFailed`. Rule 7 is not violated; all author fields are either "Manoj Jhawar", "Augmentalis Engineering", "Avanues Platform Team", or omitted.

---

## Issues

| Severity | File:Line | Issue | Suggestion |
|----------|-----------|-------|------------|
| **Critical** | `universal/UniversalIPCManager.kt:190-200` | Defines a private `IPCError` sealed class (`TargetNotFound`, `SendFailed(throwable)`, `Timeout`, `ParseError`, `NotRegistered`, `PermissionDenied`) entirely separate from `IPCErrors.kt`'s `IPCError` (`ServiceNotFound`, `NetworkFailure`, `PermissionDenied`, etc.). Code in the `universal` package will type-check against a different `IPCError` than code in the `ipc` package. Silent incompatibility; catch blocks handling `IPCError.ServiceNotFound` will miss `IPCError.TargetNotFound` from this class and vice versa. | Delete the duplicate sealed class from `UniversalIPCManager.kt` and use the canonical `IPCError` from `IPCErrors.kt` throughout. |
| **Critical** | `universal/UniversalIPCManager.kt:~50-60` | Defines a `MessageFilter` interface distinct from the `MessageFilter` data class in `IPCModels.kt`. Both define `sourceApp`, `messageCode` fields but are different types — code using `IPCModels.MessageFilter` cannot be passed to `UniversalIPCManager.subscribe(filter: MessageFilter)`. | Delete the duplicate `MessageFilter` from `UniversalIPCManager.kt` and import from `IPCModels.kt`. |
| **Critical** | `IPCManager.android.kt:289-291` | `actual fun createIPCManager(): IPCManager` throws `NotImplementedError("Android IPCManager factory not yet implemented")`. Any code calling `createIPCManager()` on Android will crash. | Implement the factory: instantiate `AndroidIPCManager` and return it. The class already exists in this file. |
| **Critical** | `ConnectionManager.kt:130-133` | `connectInternal()` stores `handle = Any()` as the connection handle — a placeholder object with no meaning. Comment says "Will be replaced by actual implementation". All `invoke()` calls that cast `handle` will operate on a no-op `Any`. | Replace the stub with actual socket, AIDL binder, or file descriptor establishment, depending on the `endpoint.type`. |
| **Critical** | `ConnectionManager.kt:215-216` | `invoke()` returns `MethodResult.Success("Result from ${invocation.methodName}")` — a hardcoded stub string as the method result. Any caller relying on real method invocation results will receive fabricated data silently. | Implement actual dispatch to the appropriate transport (AIDL stub, socket, etc.) based on connection state. |
| **Critical** | `IPCManager.android.kt:70-75` | `subscribe<T>()` does `messageFlow.map { it.second } as Flow<T>`. This is an unchecked generic cast. When the flow emits a message of type `M ≠ T`, a `ClassCastException` is thrown at the collection site at runtime, not at the `subscribe()` call site, making the error extremely hard to trace. | Use `filterIsInstance<T>()` (requires `reified` via an inline function). See the correct pattern in `iOSIPCManager`. |
| **Critical** | `ConnectionManager.kt:79` | `connections.size >= resourceLimits.maxConnections` is read **without** holding the `mutex`. A concurrent `connect()` call can pass this check simultaneously, allowing more connections than the limit. Multiple reads and writes to `connections`, `circuitBreakers`, `rateLimiters`, `callbacks`, `reconnectJobs` in `disconnect()` (L164), `invoke()` (L198), `isConnected()` (L261), `getConnection()` (L271), `getAllConnections()` (L280), `updateMetrics()` (L392), `shutdown()` (L353), and `notifyConnected/Error/StateChange()` (L366-389) are all performed without mutex. | Add `mutex.withLock { }` around every read/write of these shared maps. Or use `ConcurrentHashMap` for the maps themselves and a separate lock only where check-then-act atomicity is needed. |
| **High** | `IPCManager.android.kt:89` | `request()` returns `Result.failure(IPCError.SendFailed("Request-response not yet implemented"))` — Rule 1 stub. Any request-response IPC call returns an immediate failure. | Implement request-response using a correlation ID and a `CompletableDeferred` stored in a pending-requests map, awaiting a matching response message from the flow. |
| **High** | `IPCManager.android.kt:95-96` | `register()` has a `// TODO: Register with system IPC registry` comment and does not actually register. Callers believe they are registered but are not discoverable. | Implement registration, likely by broadcasting a capability advertisement intent on Android. |
| **High** | `IPCManager.android.kt:99-102` | `unregister()` has a `// TODO: Unregister from system` comment. Symmetric with the registration issue. | Implement unregistration broadcast. |
| **High** | `IPCManager.android.kt:114-116` | `getConnectedApps()` returns `emptyList()` — stub. Callers checking for connected peers always see none. | Implement by querying a registered app registry (maintained by broadcasts or a shared ContentProvider). |
| **High** | `AvuIPCParser.kt:1027-1037` | `AvuIPCMetrics.toIPCMetrics()` maps `totalRequests` → `connectionsTotal` and `bytesSent` → `messagesSent`. These are semantically wrong: request counts ≠ connection counts, byte sizes ≠ message counts. The round-trip codec produces silently wrong metrics. | Map `totalRequests` → `requestsTotal` (or create that field), `bytesSent` → `bytesSent`. Match field semantics precisely on both sides. |
| **High** | `AvuIPCSerializer.kt:601-613` | Mirror of the above: `serializeFromIPCMetrics()` maps `connectionsTotal` → `totalRequests` and `messagesSent` → `bytesSent`. The serializer and parser together form a closed cycle of wrong mappings, so a serialize→parse round-trip produces consistent (but still semantically wrong) output. | Fix the field mappings in both parser and serializer simultaneously to match semantic intent. |
| **High** | `ConnectionManager.kt:143` | `delay(100)` in `connectInternal()` is a hardcoded simulation delay in production code. Every connect call adds 100ms of artificial latency. | Remove the delay. |
| **High** | `ConnectionManager.kt:179` | `delay(50)` in `disconnectInternal()` — same issue. | Remove the delay. |
| **High** | `ConnectionManager.kt:363` | `generateConnectionId()` uses `"${endpoint.id}-${currentTimeMillis()}"`. If two concurrent `connect()` calls arrive in the same millisecond (very possible in production), they generate the same ID. The second `connections[connectionId] = ...` overwrites the first, silently dropping the connection. | Use `"${endpoint.id}-${UUID.randomUUID()}"` or an `AtomicLong` counter for uniqueness. |
| **High** | `universal/UniversalIPCManager.android.kt:175-177` | `actual fun UniversalIPCManager.Companion.create()` throws `NotImplementedError("Use create(context: Context) for Android")`. Any code calling `create()` without context crashes. Rule 1 violation. | Either make `context` an expect/actual parameter, use a dependency injection mechanism, or document that `create(context)` is the correct call on Android by marking the no-arg `create()` as `@Deprecated` rather than throwing. |
| **High** | `iOSIPCManager.kt:29-38` | `send()` and `broadcast()` always return `Result.failure(Exception("iOS IPC not implemented"))`. All iOS IPC is non-functional. `handleIncomingURL()` has a TODO and does not parse URL schemes. | Implement URL scheme–based IPC for iOS, or clearly gate the feature behind a platform capability flag and surface `NotImplementedError` at the module level rather than per-call. |
| **High** | `DesktopUniversalIPCManager.kt:20-27` | `send()` and `broadcast()` always return `Result.failure(IPCError.SendFailed("Desktop IPC not yet implemented"))`. Desktop IPC is non-functional. The `subscribe(filter)` override at L35-38 ignores the filter entirely and returns all messages. | Implement socket-based IPC for Desktop or document as a planned Phase 2 feature with explicit feature flag. Fix the subscribe override to apply the filter. |
| **High** | `ServiceConnector.android.kt:153-183` | `invoke()` validates the binder is alive, serializes parameters, then returns `MethodResult.Success("Method ${invocation.methodName} invoked with params: $serializedParams")` — a stub string response. The comment says "In real implementation, you would use IYourService.Stub.asInterface(binder)". Method invocation never actually happens. | Implement actual AIDL method dispatch using the generated stubs, or if generic dispatch is needed, use `android.os.Parcel` directly via `IBinder.transact()`. |
| **High** | `ServiceConnector.android.kt:52-53` | `endpoint.id.substringBefore(".")` is used as the package for `setPackage(...)` in the bind Intent. For an endpoint ID like `"com.augmentalis.voiceos.VoiceOSService"`, this extracts only `"com"`, which is incorrect. | Use the proper package name field: if `endpoint.id` is a component name, use `ComponentName.unflattenFromString(endpoint.id)` and extract the package from there. |
| **Medium** | `IPCMessages.kt:484` | `ChatMessage.serialize()` when `messageId` is null: `"$code$DELIMITER${messageId ?: ""}$DELIMITER..."` emits an empty string as the ID field. The parser at `UniversalDSL.kt:235` reads `id.takeIf { it.isNotEmpty() }` which correctly returns null for empty string. Round-trip is correct, but a leading `::` in the wire format (`CHT::text`) is unusual and could trip up third-party parsers. | Use a generated UUID when `messageId` is null at construction time rather than emitting an empty field. |
| **Medium** | `UniversalDSL.kt:50-51` | `parse()` checks `message.startsWith("JSN:") && message.contains("{")`. A message like `"JSN:id:text content without braces"` falls through to the `contains(DELIMITER)` branch and is parsed as a protocol message (which then maps to `UIComponentMessage` with `componentDSL = "text content without braces"`). This is correct by coincidence — but the dual-dispatch logic is fragile. | Simplify: always route any message with a recognized code through `parseProtocol()`, removing the `{` heuristic for JSN detection. |
| **Medium** | `UniversalDSL.kt:90` | `parseProtocol()` calls `message.split(DELIMITER)` — this splits ALL colons including those in escaped content (`%3A`). The `params.drop(2).map { unescape(it) }` call then unescapes each part, but splitting first and unescaping second means a value like `"key%3Aval"` is correctly preserved (split on unescaped colons only). However, this depends on all values being properly escaped by the sender. If a sender sends raw colons in field values, the parser will silently produce wrong splits. | Consistently require all senders to call `escape()` on values. Consider adding a parse-time validation that warns when splits produce unexpected field counts. |
| **Medium** | `DSLSerializer.kt:384` | `parseComponentAt()` uses `!dsl[pos].isLowerCase()` to detect component start (line 384). This means any token starting with a digit, punctuation, or non-letter (e.g., a property value like `"16"`) could be misidentified as a component type. | Use a more specific check: component type tokens should be in the known `aliasToType` keys, or start with an uppercase letter and be followed by `{` or `#`. |
| **Medium** | `DSLSerializer.kt:430-445` | `parseValue()` in the DSL parser handles string escaping with `c == '"' && (pos == 0 || dsl[pos - 1] != '\\')`. However, the `serializeValue()` method (L302-316) does NOT escape backslashes or internal quote characters in strings — `serializeValue("say \"hi\"")` produces `"say "hi""` which breaks the parser. | In `serializeValue()`, escape `\` as `\\` and `"` as `\"` before wrapping in quotes. |
| **Medium** | `UIIPCProtocol.kt:299` | `handler.onQuery(payload.componentId, QueryType.valueOf(payload.queryType))` throws `IllegalArgumentException` if `payload.queryType` does not match any `QueryType` enum value (e.g., due to case differences or unknown future values). The outer `try-catch` at L305 catches this but the error message is generic. | Use `QueryType.entries.find { it.name == payload.queryType }` and return a typed error if not found. |
| **Medium** | `UniversalFileParser.kt:57` | `FileType.valueOf(typeStr.uppercase())` throws `IllegalArgumentException` on unrecognized type strings. The error message from `require()` (L25) is caught by callers as an `IllegalArgumentException` with only the section count message. Unknown file types surface with no indication of which type string was unrecognised. | Add `try-catch` around `FileType.valueOf(...)` and throw a descriptive `IllegalArgumentException("Unknown file type: $typeStr")`. |
| **Medium** | `UniversalFileParser.kt:78-92` | `parseMetadata()` sets `inMetadataBlock = true` when it sees `"metadata:"` and never resets it. If the metadata block is followed by another top-level key (e.g., `"schema2: ..."`), it will be incorrectly parsed as a metadata entry. However, the current file format places metadata last in the section, so this is currently harmless but structurally fragile. | Reset `inMetadataBlock = false` when a non-indented line is encountered, or use indentation level to determine block membership. |
| **Medium** | `ServiceConnector.android.kt:62-64` | `onServiceConnected` resumes the continuation synchronously: `continuation.resume(ConnectionResult.Success(connection))`. Simultaneously, `connections[connectionId] = connectionData` and `serviceConnections[connectionId] = this` are set. If the resumed coroutine immediately calls `invoke(connectionId)` before the map writes complete (possible on a different thread), `connections[connectionId]` returns null and invoke fails. | Set the connection data in the maps **before** resuming the continuation. |
| **Medium** | `iOSIPCManager.kt:42-46` | `subscribe<T>()` does `messageFlow.map { it.second as T }` — unchecked cast, same issue as in `IPCManager.android.kt`. The comment acknowledges this but the `@Suppress` masks the real problem. | Use an inline `reified` wrapper or `filterIsInstance`. |
| **Low** | `IPCEncoder.kt` | `IPCEncoder` is a pure delegation object that re-exports all constants and methods from `BaseEncoder`. No added behaviour. This is exactly the type of indirection that Rule 2 prohibits. 3 `typealias` declarations for the same class (`AVUEncoder`, `UniversalIPCEncoder`, `RpcEncoder`) add further confusion. | Delete `IPCEncoder.kt` and update importers to use `AVUEncoder` directly. Deprecate the aliases in a migration phase. |
| **Low** | `ConnectionManager.kt:366-389` | `notifyConnected()`, `notifyDisconnected()`, `notifyError()`, `notifyStateChange()` all iterate `callbacks` list (a `MutableList<ConnectionCallback>`) without synchronization. Concurrent `addCallback()`/`removeCallback()` calls can trigger `ConcurrentModificationException`. | Copy the list before iterating: `callbacks.toList().forEach { ... }`. Or replace `MutableList` with `CopyOnWriteArrayList`. |
| **Low** | `UniversalFileParser.kt:398-402` | `LicenseData.fromUniversalFile()` uses `entries.find { it.code == "FEA" }` but the actual license code constant is `LicenseCodes.FEA`. If that constant value ever changes (it is `"FEA"` today), the parsing breaks silently. | Reference the constant directly: `entries.filter { it.code == LicenseCodes.FEA }`. |
| **Low** | `DSLBenchmark.kt:522-538` (companion) | `DSLSerializer.compareSizes()` instantiates a new `DSLSerializer` inside the static method and then calls `Json.encodeToString(component)`. Since `UIComponent.properties` contains `Any` values, the `AnySerializer` is invoked at encode time. This will fail at runtime if properties contain types that `AnySerializer` doesn't handle (e.g., a nested data class). | Document the limitation, or broaden `AnySerializer` to handle nested data classes via a recursive approach. |
| **Low** | `UniversalFileParserTest.kt:113-140` | Tests reference `FileType.AVW` and `FileType.AVN` for file type strings "AVW" and "AVN". These types do not exist in the `FileType` enum defined in `UniversalFileParser.kt`. The enum has `AWB` and no `AVN`. These tests will fail with `IllegalArgumentException: No enum constant FileType.AVW`. | Either add `AVW` and `AVN` to the `FileType` enum, or fix the test content strings to use the existing `AWB` type. This is a confirmed test failure. |
| **Low** | `ServiceConnector.kt:29` | `@author Avanues Platform Team` — while not a Rule 7 AI attribution violation, this is not the project owner name. Acceptable but inconsistent with other files using "Manoj Jhawar". | Standardize to "Manoj Jhawar" or omit. |

---

## Detailed Findings

### Critical: Duplicate IPCError types cause silent type incompatibility

**Files:** `Modules/IPC/src/commonMain/kotlin/com/augmentalis/avamagic/ipc/IPCErrors.kt` vs. `Modules/IPC/src/commonMain/kotlin/com/augmentalis/avamagic/ipc/universal/UniversalIPCManager.kt`

`IPCErrors.kt` (canonical):
```kotlin
sealed class IPCError {
    data class ServiceNotFound(val serviceId: String) : IPCError()
    data class NetworkFailure(val cause: Throwable) : IPCError()
    data class PermissionDenied(val reason: String) : IPCError()
    data class ServiceUnavailable(val message: String) : IPCError()
    data class InvalidResponse(val message: String) : IPCError()
    ...
}
typealias TargetNotFound = IPCError.ServiceNotFound  // backward compat alias
```

`UniversalIPCManager.kt` (duplicate — WRONG):
```kotlin
sealed class IPCError {   // DIFFERENT class in different package
    data class TargetNotFound(val target: String) : IPCError()
    data class SendFailed(val message: String, val throwable: Throwable? = null) : IPCError()
    data class Timeout(val timeoutMs: Long) : IPCError()
    data class ParseError(val message: String) : IPCError()
    data class NotRegistered : IPCError()
    data class PermissionDenied(val reason: String) : IPCError()
}
```

Code in the `universal` package (e.g., `AndroidUniversalIPCManager`, `DesktopUniversalIPCManager`) returns `IPCError.SendFailed(...)` — which is the *local* `IPCError`, not the canonical one. A caller in the `ipc` package expecting `com.augmentalis.avamagic.ipc.IPCError.SendFailed` will never match via `is IPCError.SendFailed` because the packages differ. The only solution is to delete the duplicate class.

---

### Critical: createIPCManager() factory throws NotImplementedError on Android

**File:** `Modules/IPC/src/androidMain/kotlin/com/augmentalis/avamagic/ipc/IPCManager.android.kt`

```kotlin
actual fun createIPCManager(): IPCManager {
    throw NotImplementedError("Android IPCManager factory not yet implemented")  // Line 289
}
```

The `AndroidIPCManager` class is fully defined above this function in the same file. The factory just needs to instantiate it:
```kotlin
actual fun createIPCManager(): IPCManager {
    // Cannot inject Context here — use DI (Hilt) or Application-level singleton
    // For now, require callers to use dependency injection
    throw IllegalStateException(
        "Use DI to inject AndroidIPCManager(context) directly. " +
        "See ActionsManager for the recommended injection pattern."
    )
}
```
Or, if a no-arg factory is required, store an application-level context:
```kotlin
actual fun createIPCManager(): IPCManager = AndroidIPCManager(applicationContext)
```

---

### Critical: ConnectionManager mutex gaps — complete list

**File:** `Modules/IPC/src/commonMain/kotlin/com/augmentalis/avamagic/ipc/ConnectionManager.kt`

Lines that read or write shared mutable maps **without** holding `mutex`:

| Line | Operation | Map accessed |
|------|-----------|-------------|
| L79 | `connections.size` read | `connections` |
| L164 | `connections[connectionId]` read | `connections` |
| L198 | `connections[connectionId]` read | `connections` |
| L261 | `connections.containsKey(...)` read | `connections` |
| L271 | `connections[connectionId]` read | `connections` |
| L280 | `connections.values.toList()` read | `connections` |
| L338 | `reconnectJobs.values.forEach` + `reconnectJobs.clear()` | `reconnectJobs` |
| L342-345 | `connections.keys.toList()` then `disconnect()` | `connections` |
| L353-358 | `connections.clear()`, `circuitBreakers.clear()`, etc. | all maps |
| L366-389 | `callbacks.forEach { ... }` iteration | `callbacks` |
| L392 | `connections.values` iteration | `connections` |

The `mutex` is only held for `connect()` (L85-125), `disconnect()` (L134-145), and `addCallback/removeCallback`. All other access is unguarded.

---

### High: AVU metrics round-trip produces semantically wrong values

**Files:** `AvuIPCParser.kt:1027-1037` and `AvuIPCSerializer.kt:601-613`

Serializer writes:
```kotlin
// AvuIPCSerializer.serializeFromIPCMetrics()
append(metrics.connectionsTotal)    // field: connections_total → maps to totalRequests field
append(metrics.messagesSent)        // field: messages_sent → maps to bytesSent field
```

Parser reads:
```kotlin
// AvuIPCMetrics.toIPCMetrics()
totalRequests = avuMetrics.connectionsTotal    // connectionsTotal is NOT the same as requests
messagesSent = avuMetrics.bytesSent            // bytesSent is NOT the same as message count
```

A system sending 5 connections and 1000 messages across 50KB would report:
- `connectionsTotal = 5` → `totalRequests = 5` (wrong: lost the actual request count)
- `messagesSent = 50000` (bytes) → `messagesSent = 50000` (messages) — wrong by three orders of magnitude

The codec is internally consistent (the error round-trips), but the semantics of the `IPCMetrics` data class are violated.

---

### High: ServiceConnector.android.kt sets wrong package on bind Intent

**File:** `Modules/IPC/src/androidMain/kotlin/com/augmentalis/avamagic/ipc/ServiceConnector.android.kt`

```kotlin
// L84-88
val intent = Intent().apply {
    action = endpoint.aidlInterface
    setPackage(endpoint.id.substringBefore("."))  // ← endpoint.id = "com.augmentalis.voiceos.IVoiceOSService"
}                                                   //   substringBefore(".") = "com"
                                                    //   setPackage("com") is wrong
```

`endpoint.id` is a fully-qualified service identifier. `substringBefore(".")` takes only the first segment. This should be:
```kotlin
setPackage(endpoint.id.substringBeforeLast("."))  // → "com.augmentalis.voiceos"
```
or better, require `endpoint` to carry a separate `packageName` field.

---

### Confirmed test failure: AVW and AVN file types missing from enum

**File:** `Modules/IPC/src/commonTest/kotlin/.../UniversalFileParserTest.kt`

Tests at L113 and L143 use:
```kotlin
assertEquals(FileType.AVW, file.type)   // → IllegalArgumentException: No enum constant FileType.AVW
assertEquals(FileType.AVN, file.type)   // → IllegalArgumentException: No enum constant FileType.AVN
```

The `FileType` enum in `UniversalFileParser.kt` contains: `AVA, VOS, AVC, AWB, AMI, AMC, HOV, IDC, AVL, LICENSE`.
It does NOT contain `AVW` or `AVN`. The tests reference types that do not exist and will always throw.

Fix option 1 — add the types to the enum:
```kotlin
enum class FileType {
    AVA, VOS, AVC, AWB, AVW, AVN, AMI, AMC, HOV, IDC, AVL, LICENSE;
    ...
}
```

Fix option 2 — fix the test strings to use the existing `AWB` for the web browser type, and determine the correct enum value for `AVN`.

---

## Recommendations

1. **Delete the duplicate `IPCError`, `MessageFilter`, and `MessageType` in `UniversalIPCManager.kt`** — this is the most dangerous structural defect. Import and use the canonical versions from `IPCErrors.kt` and `IPCModels.kt` throughout the `universal` package.

2. **Implement `createIPCManager()` on Android** — the current `NotImplementedError` means no code path that calls `createIPCManager()` works. At minimum, document the DI pattern and throw `IllegalStateException` with a clear message directing developers to inject `AndroidIPCManager(context)`.

3. **Fix `ConnectionManager`'s mutex discipline** — all 10+ unguarded map accesses must be wrapped in `mutex.withLock { }`. Consider replacing `connections` and `callbacks` with `ConcurrentHashMap` and `CopyOnWriteArrayList` respectively to allow reads without the mutex and only lock writes.

4. **Fix the `ServiceConnector.android.kt` package extraction bug** — `substringBefore(".")` extracts only `"com"` from a fully-qualified name; use `substringBeforeLast(".")` or require a dedicated `packageName` field on `ServiceEndpoint`.

5. **Fix the AVU metrics codec field mapping** — `totalRequests` ≠ `connectionsTotal`, and `bytesSent` ≠ `messagesSent`. Fix both `AvuIPCParser.toIPCMetrics()` and `AvuIPCSerializer.serializeFromIPCMetrics()` simultaneously.

6. **Fix the confirmed test failures** — `UniversalFileParserTest` references `FileType.AVW` and `FileType.AVN` which don't exist in the enum. These tests will fail at compile or runtime. Either add the missing types or update the tests.

7. **Remove simulation delays in `ConnectionManager`** — the `delay(100)` and `delay(50)` hardcoded values add artificial latency on every connect/disconnect. These are development scaffolding that must be removed before production use.

8. **Replace `IPCEncoder.kt` delegation object** — it adds a layer of indirection over `AVUEncoder` with no added behaviour, violating Rule 2. Direct imports of `AVUEncoder` are simpler.

9. **Implement iOS and Desktop IPC** — both currently always return `SendFailed`. If these are Phase 2 features, make them clearly gated behind a platform capability flag rather than silently failing at runtime. Fix the `subscribe(filter)` in `DesktopUniversalIPCManager` to actually apply the filter.

10. **Fix `DSLSerializer.serializeValue()` string escaping** — strings containing `"` characters are not escaped before wrapping in quotes, producing malformed DSL output that the parser cannot correctly reconstruct.
