# ADR-006: VoiceOSCore IPC Architecture - Phase 3 Requirement

**Status:** Accepted
**Date:** 2025-11-11
**Deciders:** Development Team
**Context:** Phase 2 IPC/AIDL Implementation

---

## Context and Problem Statement

During Phase 2 IPC implementation (2/3 complete), VoiceOSCore service binder was deferred to Phase 3 due to architectural constraints. This document explains why VoiceOSCore IPC implementation requires Phase 3 architectural work, while VoiceCursor and UUIDCreator were completed in Phase 2.

## Decision Drivers

* **Android Platform Constraint:** `AccessibilityService.onBind()` is final and cannot be overridden
* **Service Binding Model:** Accessibility services use a different binding mechanism than regular services
* **Architectural Complexity:** VoiceOSCore is the central accessibility service, requiring special IPC design
* **Dependency Management:** Hilt + ksp + AIDL circular dependency resolution needed

## Considered Options

### Option 1: Override onBind() in VoiceOSService
**Status:** ❌ REJECTED - Not possible

**Technical Analysis:**
```kotlin
class VoiceOSService : AccessibilityService() {
    override fun onBind(intent: Intent?): IBinder? {  // ❌ Compilation error
        // Error: 'onBind' in 'AccessibilityService' is final and cannot be overridden
    }
}
```

**Why it fails:**
- Android's `AccessibilityService` class marks `onBind()` as final
- Accessibility services are bound automatically by Android system
- Cannot intercept or customize binding process

### Option 2: Create Companion IPC Service
**Status:** ✅ SELECTED for Phase 3

**Architecture:**
```
┌─────────────────────────────────────┐
│   VoiceOSService                    │
│   (AccessibilityService)            │
│   - Accessibility tree access       │
│   - Event handling                  │
│   - Voice command processing        │
│   - Bound by Android system         │
└─────────────────────────────────────┘
              ▲
              │ Static reference
              │
┌─────────────────────────────────────┐
│   VoiceOSIPCService                 │
│   (Regular Service)                 │
│   - AIDL service binder             │
│   - Delegates to VoiceOSService     │
│   - Custom onBind() implementation  │
│   - Bound by external apps          │
└─────────────────────────────────────┘
```

**Implementation Pattern:**
```kotlin
// Phase 3: Companion IPC Service
class VoiceOSIPCService : Service() {

    private val binder by lazy {
        VoiceOSServiceBinder(VoiceOSService.getInstance())
    }

    override fun onBind(intent: Intent?): IBinder? {
        return when (intent?.action) {
            "com.augmentalis.voiceoscore.BIND_IPC" -> binder
            else -> null
        }
    }
}
```

**Manifest Entry:**
```xml
<service
    android:name=".accessibility.VoiceOSIPCService"
    android:exported="true"
    android:permission="com.augmentalis.voiceoscore.BIND_IPC">
    <intent-filter>
        <action android:name="com.augmentalis.voiceoscore.BIND_IPC" />
    </intent-filter>
</service>
```

### Option 3: Use Content Provider for IPC
**Status:** ❌ REJECTED - Not suitable for bidirectional communication

**Analysis:**
- Content Providers are for data sharing, not service invocation
- No native callback mechanism
- Cannot handle real-time voice command execution
- Would require polling or inefficient workarounds

### Option 4: Use Broadcast Receivers
**Status:** ❌ REJECTED - Not suitable for request-response patterns

**Analysis:**
- One-way communication only
- No return values from commands
- Unreliable delivery
- Poor performance for high-frequency operations

## Decision Outcome

**Chosen Option:** Option 2 - Companion IPC Service (Phase 3)

### Positive Consequences
✅ Clean separation of concerns (Accessibility vs IPC)
✅ No modifications to AccessibilityService required
✅ Standard Android service binding pattern
✅ Full AIDL functionality preserved
✅ Callback support for asynchronous events

### Negative Consequences
⚠️ Requires additional service (minimal overhead)
⚠️ Static reference pattern needed for service communication
⚠️ Additional manifest configuration required

## Technical Details

### Why Phase 2 Was 2/3 Complete

**Completed in Phase 2:**
1. ✅ **VoiceCursor** - Regular service, onBind() overridable
   - File: `VoiceCursorServiceBinder.kt`
   - Pattern: Direct AIDL service implementation
   - No architectural constraints

2. ✅ **UUIDCreator** - Library module, onBind() overridable
   - File: `UUIDCreatorServiceBinder.kt`
   - Pattern: Direct AIDL service implementation
   - No architectural constraints

**Deferred to Phase 3:**
3. ⏳ **VoiceOSCore** - AccessibilityService, onBind() final
   - File: `VoiceOSServiceBinder.kt.phase3` (prepared, not activated)
   - Pattern: Requires companion IPC service
   - **Reason:** Architectural constraint (onBind is final)

### Compilation Errors Encountered

When attempting direct integration:
```
e: 'onBind' in 'AccessibilityService' is final and cannot be overridden
e: Cannot access 'isServiceReady': it is private in 'VoiceOSService'
e: Unresolved reference: Stub (AIDL not generated yet)
```

### Phase 3 Requirements

1. **Create VoiceOSIPCService** (companion service)
   - Extends Service, not AccessibilityService
   - Implements custom onBind() with AIDL binder
   - Delegates calls to VoiceOSService singleton

2. **Expose VoiceOSService API** (visibility changes)
   - Make `isServiceReady` internal (not private)
   - Provide `getInstance()` for companion service access
   - Add `executeCommand()` public API

3. **Update Manifest** (service declaration)
   - Add VoiceOSIPCService entry
   - Define IPC action and permissions
   - Configure exported and permission attributes

4. **Resolve AIDL Generation** (build configuration)
   - Ensure AIDL interfaces compile before Kotlin
   - Fix ksp task ordering if needed
   - Verify Hilt doesn't interfere with AIDL

## Implementation Roadmap

### Phase 3a: Prepare VoiceOSService
- [ ] Make `isServiceReady` internal/public
- [ ] Add `getInstance()` companion method
- [ ] Add `executeCommand(String)` public API
- [ ] Verify static reference pattern

### Phase 3b: Create Companion Service
- [ ] Create `VoiceOSIPCService.kt`
- [ ] Implement `onBind()` with action filtering
- [ ] Integrate `VoiceOSServiceBinder.kt.phase3`
- [ ] Add service lifecycle management

### Phase 3c: Build Configuration
- [ ] Fix AIDL generation order
- [ ] Resolve Hilt + ksp + AIDL dependencies
- [ ] Update build.gradle.kts if needed
- [ ] Verify compilation succeeds

### Phase 3d: Manifest & Testing
- [ ] Add service declaration to AndroidManifest.xml
- [ ] Define IPC action constant
- [ ] Add permission declaration
- [ ] Test IPC binding from external app
- [ ] Verify callback mechanism works

## References

- **Phase 2 Completion Commit:** `89921f2` - "Phase 2: IPC Service Binder Implementations (2/3 Complete)"
- **VoiceCursor Binder:** `modules/apps/VoiceCursor/src/main/java/com/augmentalis/voiceos/cursor/VoiceCursorServiceBinder.kt`
- **UUIDCreator Binder:** `modules/libraries/UUIDCreator/src/main/java/com/augmentalis/uuidcreator/UUIDCreatorServiceBinder.kt`
- **VoiceOSCore Binder (Phase 3):** `modules/apps/VoiceOSCore/src/main/java/com/augmentalis/voiceoscore/accessibility/VoiceOSServiceBinder.kt.phase3`
- **Android AccessibilityService Docs:** https://developer.android.com/reference/android/accessibilityservice/AccessibilityService

## Status

**Current Phase:** Phase 2 Complete (2/3 modules)
**Next Phase:** Phase 3 - VoiceOSCore IPC Architecture
**Blocker:** AccessibilityService.onBind() is final
**Solution:** Companion IPC service pattern

---

**Decision Date:** 2025-11-11
**Last Updated:** 2025-11-11
**Status:** Documented and Ready for Phase 3
