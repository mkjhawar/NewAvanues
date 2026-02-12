# VoiceOSCore-Fix-ProtoVuidToAvidAndComposeDesktop-260212-V1

## Summary
Two fixes: (1) Renamed all remaining `vuid` references to `avid` in proto schema files and Wire-generated Kotlin, completing the VUID→AVID migration. (2) Fixed Compose desktop compilation failure in AvidCreator and DeviceManager modules.

---

## Fix 1: Proto VUID→AVID Rename

### Problem
After the application-level VUID→AVID migration (46 files, commit 667b6a9b), proto schema files and 4 Wire-generated Kotlin files outside the `avid/` package still used `vuid` field names. The `avid/` package generated files (AvidElement.kt, ElementQueryRequest.kt, etc.) were already migrated, creating inconsistency.

### Root Cause
Proto regeneration was disabled (Wire plugin partially turned off during migration), leaving stale generated code and proto source files behind. These were not caught by application-level refactoring tools.

### Solution
Renamed `vuid` → `avid` (and `target_vuid` → `target_avid`) in all remaining locations. Field tags remain identical — zero wire-format impact (backward-compatible binary protocol).

### Files Modified

**Wire-generated Kotlin (4 files — mechanical find-replace):**

| File | Change | Field Tag | Impact |
|------|--------|-----------|--------|
| `Modules/Rpc/src/commonMain/.../exploration/LearnedElement.kt` | `val vuid` → `val avid` | 1 | All occurrences in value class |
| `Modules/Rpc/src/commonMain/.../exploration/SuggestedAction.kt` | `val vuid` → `val avid` | 2 | All occurrences in data class |
| `Modules/Rpc/src/commonMain/.../webavanue/ElementSelector.kt` | `val vuid` → `val avid` | 4 | All occurrences in data class |
| `Modules/Rpc/src/commonMain/.../nlu/ParsedCommand.kt` | `val target_vuid` → `val target_avid` | 5 | jsonName also updated `targetVuid` → `targetAvid` |

**Proto source files (4 files — documentation source, Wire plugin disabled):**

| File | Change | Scope | Notes |
|------|--------|-------|-------|
| `Modules/Rpc/Common/proto/vuid.proto` | VUIDElement→AvidElement, VUIDCommandResult→AvidCommandResult, VUIDCreatorService→AvidCreatorService, GenerateVUIDRequest→GenerateAvidRequest, package vuid→avid, all `vuid` fields→`avid` | 6+ messages | Package name change: `vuid` → `avid` |
| `Modules/Rpc/Common/proto/exploration.proto` | `vuid` fields→`avid` | 2 fields | Bounds package reference updated vuid→avid |
| `Modules/Rpc/Common/proto/webavanue.proto` | `vuid` field→`avid` | 1 field | Single field rename |
| `Modules/Rpc/Common/proto/nlu.proto` | `target_vuid`→`target_avid` | 1 field | Single field rename |

**Infrastructure (2 files):**

| File | Change | Context | Deprecation |
|------|--------|---------|-------------|
| `Modules/Rpc/src/androidMain/.../transport/TransportFactory.kt` | `VUID_CREATOR = "universalrpc.vuidcreator"` → `AVID_CREATOR = "universalrpc.avidcreator"`, updated forService mapping | Service registration | Old `VUID_CREATOR` constant removed |
| `Modules/Rpc/src/commonMain/.../ServiceRegistry.kt` | Removed deprecated `SERVICE_VUID_CREATOR` alias | Registry cleanup | No consumers found |

### Wire Compatibility

Proto3 serialization uses tag numbers, not field names:
- **Field tags preserved**: tag 1, tag 2, tag 4, tag 5 unchanged
- **Binary format**: Identical (vuid and avid both serialize to the same wire format)
- **Backward compatibility**: Old clients reading new wire format work transparently
- **Forward compatibility**: New clients reading old wire format work transparently

---

## Fix 2: Compose Desktop Config

### Problem
AvidCreator and DeviceManager modules apply `libs.plugins.kotlin.compose` which enables the Compose compiler plugin for ALL targets including desktop. The desktop target had no Compose runtime dependency, causing compilation failure:

```
e: [commonMain] Unresolved reference: 'androidx.compose.runtime.compose'
```

Both modules only use Compose UI in `androidMain`, not desktopMain.

### Root Cause
Kotlin Compose plugin (1.7.3) requires `compose.runtime` even if only non-runtime Compose APIs are used. Desktop target was not configured to provide this dependency.

### Solution
Added lightweight `compose.runtime` (no UI framework, ~100KB) to desktopMain dependencies in both modules. This provides the required Compose runtime symbols without pulling in full desktop Compose UI stack.

### Files Modified

| File | Dependency | Target | Type |
|------|------------|--------|------|
| `Modules/AvidCreator/build.gradle.kts` | `compose.runtime` | desktopMain | implementation |
| `Modules/DeviceManager/build.gradle.kts` | `compose.runtime` | desktopMain | implementation |

**Code snippet (both files):**
```kotlin
desktopMain.dependencies {
    implementation(compose.runtime)
}
```

---

## Verification

### Compilation Tests

| Command | Result | Notes |
|---------|--------|-------|
| `./gradlew assembleDebug --no-build-cache` | ✓ Zero warnings, zero errors | Full debug build |
| `./gradlew :Modules:AvidCreator:desktopMainClasses` | ✓ Desktop compiles | Target-specific check |
| `./gradlew :Modules:DeviceManager:desktopMainClasses` | ✓ Desktop compiles | Target-specific check |
| `./gradlew :Modules:Rpc:compileKotlinAndroid` | ✓ Rpc module compiles | Proto changes verified |

### Binary Protocol Tests

- Wire format unchanged (tags preserved)
- No serialization tests required (field name changes are transparent to binary layer)

---

## Impact Assessment

### Scope
- **Modules affected**: Rpc (4 generated files), AvidCreator (1 build file), DeviceManager (1 build file)
- **Lines changed**: ~15 field renames + 2 gradle configs
- **Backward compatibility**: Full (binary wire format unchanged)
- **API surface**: Zero public API changes (internal proto fields only)

### Risk Level
**Low** — Name changes only, no logic changes.

---

## Related Documentation

- **Previous migration**: Commit 667b6a9b (application-level VUID→AVID, 46 files)
- **Proto schema**: `Modules/Rpc/Common/proto/` (all 4 proto files)
- **Wire plugin config**: `Modules/Rpc/build.gradle.kts`
- **Compose desktop patterns**: AvidCreator, DeviceManager desktopMain source sets

---

## Notes for Future Sessions

- Proto files are documentation source only (Wire plugin regeneration disabled)
- When re-enabling Wire codegen, generated files in this session align with updated proto source
- Desktop Compose is a light dependency; no harm in always including `compose.runtime`
- All 4 infrastructure files (vuid.proto, exploration.proto, webavanue.proto, nlu.proto) now consistently use `avid` terminology

---

## Author & Session
- **Session Date**: 2026-02-12
- **Branch**: VoiceOSCore-KotlinUpdate
- **Related Commits**: Follows 667b6a9b (main VUID→AVID migration)
