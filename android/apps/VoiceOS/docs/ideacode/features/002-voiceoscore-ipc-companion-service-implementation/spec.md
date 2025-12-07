# Feature Specification: VoiceOSCore IPC Companion Service Implementation

**Feature ID:** 002
**Feature Name:** VoiceOSCore IPC Companion Service Implementation
**Created:** 2025-11-11
**Author:** Manoj Jhawar <manoj@ideahq.net>
**Profile:** android-app
**Priority:** HIGH
**Complexity:** Tier 2 (Moderate)
**Estimated Effort:** 8-12 hours

---

## Executive Summary

Implement a companion IPC service for VoiceOSCore to enable cross-process communication with other Augmentalis applications. This completes Phase 2 IPC implementation (currently 2/3) by addressing the architectural constraint that AccessibilityService.onBind() is final. The companion service pattern provides AIDL-based IPC while preserving VoiceOSService's accessibility functionality.

**Business Value:** Enables modular architecture where VoiceCursor, LearnApp, and future modules can access VoiceOSCore functionality via IPC, reducing coupling and improving maintainability.

**User Benefit:** Seamless integration between Augmentalis apps with shared voice command infrastructure.

---

## Problem Statement

### Current State

**Phase 2 IPC Status:** 2/3 Complete
- ✅ VoiceCursor service binder implemented
- ✅ UUIDCreator service binder implemented
- ❌ VoiceOSCore IPC deferred (architectural constraint)

**Technical Blocker:**
- `AccessibilityService.onBind()` is final and cannot be overridden
- Cannot add custom IPC binding to VoiceOSService directly
- Attempted integration results in compilation errors

**Reference:** ADR-006 (docs/planning/architecture/decisions/ADR-006-VoiceOSCore-IPC-Architecture-Phase3-Requirement.md)

### Pain Points

1. **Module Isolation:** VoiceCursor and other modules cannot access VoiceOSCore functionality via IPC
2. **Tight Coupling:** Modules must use direct references or workarounds instead of clean IPC
3. **Incomplete Architecture:** Phase 2 incomplete at 67% (2/3 modules)
4. **Technical Debt:** `.phase3` files waiting for activation

### Desired State

- VoiceOSCore functionality accessible via AIDL IPC
- Companion service handles binding for external apps
- Clean separation: AccessibilityService (system) + IPCService (apps)
- Phase 2 marked as 100% complete (3/3 modules)
- Signature-protected for Augmentalis apps only (SDK support deferred to Phase 4)

---

## Requirements

### Functional Requirements

#### FR-1: VoiceOSIPCService Creation
**Priority:** HIGH
**Description:** Create companion service for IPC binding

**Acceptance Criteria:**
- [ ] New class `VoiceOSIPCService` extends `Service`
- [ ] Located in `modules/apps/VoiceOSCore/src/main/java/com/augmentalis/voiceoscore/accessibility/`
- [ ] Implements custom `onBind()` method
- [ ] Returns `VoiceOSServiceBinder` for IPC action
- [ ] Delegates all calls to VoiceOSService singleton

**Implementation:**
```kotlin
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

#### FR-2: VoiceOSService API Exposure
**Priority:** HIGH
**Description:** Expose internal VoiceOSService APIs for IPC access

**Acceptance Criteria:**
- [ ] `isServiceReady` changed from private to internal
- [ ] `getInstance()` companion method returns service instance
- [ ] Static `executeCommand(String)` remains public
- [ ] Service lifecycle properly managed

**API Changes:**
```kotlin
class VoiceOSService : AccessibilityService() {
    companion object {
        private var instanceRef: WeakReference<VoiceOSService>? = null

        fun getInstance(): VoiceOSService? = instanceRef?.get()

        fun isServiceRunning(): Boolean = getInstance() != null

        fun executeCommand(commandText: String): Boolean {
            // Existing implementation
        }
    }

    internal var isServiceReady = false  // Changed from private
}
```

#### FR-3: AIDL Interface Implementation
**Priority:** HIGH
**Description:** Implement selected AIDL methods in VoiceOSServiceBinder

**AIDL Methods (All 14 implemented, 12 in public API):**

**Existing AIDL Methods (7 total):**
1. `isServiceReady()` - Check service status (⚠️ AIDL: implemented, API: hidden)
2. `executeCommand(String)` - Execute voice commands (✅ PUBLIC API)
3. `executeAccessibilityAction(String, String)` - Custom actions (✅ PUBLIC API)
4. `scrapeCurrentScreen()` - UI scraping (⚠️ AIDL: implemented, API: hidden)
5. `registerCallback(IVoiceOSCallback)` - Event callbacks (✅ PUBLIC API)
6. `unregisterCallback(IVoiceOSCallback)` - Remove callbacks (✅ PUBLIC API)
7. `getServiceStatus()` - Status JSON (✅ PUBLIC API)
8. `getAvailableCommands()` - Command list (✅ PUBLIC API)

**Extended Functionality (6 new methods):**
9. `startVoiceRecognition(String engine, String language)` - Start listening (✅ PUBLIC API)
10. `stopVoiceRecognition()` - Stop listening (✅ PUBLIC API)
11. `learnCurrentApp()` - Trigger app learning (✅ PUBLIC API)
12. `getLearnedApps()` - Query learned apps (✅ PUBLIC API)
13. `getCommandsForApp(String packageName)` - Get app commands (✅ PUBLIC API)
14. `registerDynamicCommand(String pattern, String action)` - Add command (✅ PUBLIC API)

**Implementation Notes:**
- All 14 methods implemented in AIDL and VoiceOSServiceBinder
- Methods #1 and #4 marked with @hide annotation (internal use only)
- Only 12 methods documented in public API documentation

**Acceptance Criteria:**
- [ ] All 14 methods implemented in VoiceOSServiceBinder
- [ ] Thread-safe with proper coroutine handling
- [ ] Error handling and logging for all methods
- [ ] Callback management with RemoteCallbackList
- [ ] Documentation with KDoc comments

#### FR-4: AIDL Interface Updates
**Priority:** HIGH
**Description:** Update IVoiceOSService.aidl to include all methods (keep existing + add new)

**Acceptance Criteria:**
- [ ] KEEP `isServiceReady()` method (existing)
- [ ] KEEP `scrapeCurrentScreen()` method (existing)
- [ ] Add 6 new extended methods to AIDL
- [ ] Update KDoc comments
- [ ] Mark internal-only methods with @hide annotation
- [ ] Verify AIDL compilation succeeds

**Complete AIDL Interface:**
```aidl
interface IVoiceOSService {
    // Internal use only (not in public API docs)
    /** @hide */
    boolean isServiceReady();

    // Public API (documented)
    boolean executeCommand(String commandText);
    boolean executeAccessibilityAction(String actionType, String parameters);
    void registerCallback(IVoiceOSCallback callback);
    void unregisterCallback(IVoiceOSCallback callback);
    String getServiceStatus();
    List<String> getAvailableCommands();

    // Extended API (new, documented)
    boolean startVoiceRecognition(String engine, String language);
    boolean stopVoiceRecognition();
    boolean learnCurrentApp();
    List<String> getLearnedApps();
    List<String> getCommandsForApp(String packageName);
    boolean registerDynamicCommand(String pattern, String action);

    // Internal use only (not in public API docs)
    /** @hide */
    String scrapeCurrentScreen();
}
```

**Note:** All methods implemented, but only selected 12 methods documented in public API

#### FR-5: Manifest Configuration
**Priority:** HIGH
**Description:** Register VoiceOSIPCService in AndroidManifest.xml

**Acceptance Criteria:**
- [ ] Service declared with proper attributes
- [ ] Signature permission protection configured
- [ ] IPC action intent-filter added
- [ ] Exported=true for external binding

**Manifest Entry:**
```xml
<service
    android:name=".accessibility.VoiceOSIPCService"
    android:exported="true"
    android:permission="signature">
    <intent-filter>
        <action android:name="com.augmentalis.voiceoscore.BIND_IPC" />
    </intent-filter>
</service>
```

**Security Model:** Signature protection (only com.augmentalis.* apps)

#### FR-6: Service Lifecycle Management
**Priority:** MEDIUM
**Description:** Proper initialization and cleanup

**Acceptance Criteria:**
- [ ] onCreate() initializes binder
- [ ] onDestroy() cleans up callbacks
- [ ] Handles VoiceOSService not running gracefully
- [ ] Logs lifecycle events

### Non-Functional Requirements

#### NFR-1: Performance
**Requirement:** IPC calls complete within acceptable time

**Acceptance Criteria:**
- [ ] executeCommand() < 100ms (90th percentile)
- [ ] getServiceStatus() < 50ms
- [ ] Callback notifications < 20ms
- [ ] No memory leaks in long-running scenarios

#### NFR-2: Thread Safety
**Requirement:** All IPC methods are thread-safe

**Acceptance Criteria:**
- [ ] RemoteCallbackList used for callbacks
- [ ] Proper synchronization on shared state
- [ ] Coroutines used for async operations
- [ ] No race conditions in concurrent access

#### NFR-3: Error Handling
**Requirement:** Graceful degradation on errors

**Acceptance Criteria:**
- [ ] All methods have try-catch blocks
- [ ] Errors logged with context
- [ ] RemoteException handled for callbacks
- [ ] Client receives error status, not crashes

#### NFR-4: Documentation
**Requirement:** Complete API documentation

**Acceptance Criteria:**
- [ ] All public methods have KDoc
- [ ] AIDL interface documented
- [ ] Usage examples in comments
- [ ] Integration guide created

#### NFR-5: Build Configuration
**Requirement:** AIDL compilation succeeds

**Acceptance Criteria:**
- [ ] AIDL files compile before Kotlin
- [ ] ksp task ordering correct
- [ ] No Hilt + AIDL circular dependencies
- [ ] Clean build succeeds

### Success Criteria

**Primary Success Metrics:**
- [ ] Phase 2 marked as 100% complete (3/3 modules)
- [ ] VoiceOSIPCService builds and deploys
- [ ] External app can bind and call methods
- [ ] All 12 IPC methods functional
- [ ] Zero compilation errors

**Quality Metrics:**
- [ ] 85%+ test coverage (android-app profile requirement)
- [ ] Zero critical bugs in IPC layer
- [ ] Performance within NFR targets
- [ ] Documentation complete

---

## User Stories

### US-1: Module Developer - Basic IPC Access
**As a** VoiceCursor module developer
**I want to** bind to VoiceOSCore via IPC
**So that** I can execute voice commands without direct dependencies

**Acceptance Criteria:**
- Can bind to VoiceOSIPCService from VoiceCursor
- Can call executeCommand() and receive results
- Service handles connection lifecycle properly

**Test Scenario:**
```kotlin
// In VoiceCursor
val intent = Intent("com.augmentalis.voiceoscore.BIND_IPC")
intent.setPackage("com.augmentalis.voiceoscore")
bindService(intent, connection, Context.BIND_AUTO_CREATE)

// Once connected
service.executeCommand("go back") // Returns true/false
```

### US-2: Module Developer - Voice Recognition Control
**As a** LearnApp module developer
**I want to** start/stop voice recognition programmatically
**So that** I can trigger learning mode with voice input

**Acceptance Criteria:**
- Can start voice recognition with specific engine
- Can stop voice recognition
- Receives recognition results via callback

**Test Scenario:**
```kotlin
service.registerCallback(object : IVoiceOSCallback.Stub() {
    override fun onCommandRecognized(command: String, confidence: Float) {
        // Handle recognized command
    }
})

service.startVoiceRecognition("vivoka", "en-US")
// User speaks...
service.stopVoiceRecognition()
```

### US-3: Module Developer - Dynamic Commands
**As a** third-party app developer (future)
**I want to** register custom voice commands
**So that** my app can respond to voice input

**Acceptance Criteria:**
- Can register command patterns
- Commands trigger app-specific actions
- Can query registered commands

**Test Scenario:**
```kotlin
service.registerDynamicCommand(
    pattern = "take a note *",
    action = "com.myapp.TAKE_NOTE"
)

service.getCommandsForApp("com.myapp") // Returns registered commands
```

### US-4: System Administrator - Service Monitoring
**As a** system administrator
**I want to** query VoiceOS service status
**So that** I can monitor health and troubleshoot issues

**Acceptance Criteria:**
- getServiceStatus() returns JSON with metrics
- Can check if service is running
- Can get available commands list

**Test Scenario:**
```kotlin
val status = service.getServiceStatus()
// JSON: {"isReady": true, "isRunning": true, "timestamp": 1234567890}

val commands = service.getAvailableCommands()
// List: ["back", "home", "recent", ...]
```

---

## Technical Constraints

### Android Platform Constraints
- **API Level:** Minimum SDK 24 (Android 7.0)
- **Target SDK:** 34 (Android 14)
- **Accessibility Service:** Cannot override onBind() - final method
- **IPC Mechanism:** AIDL only (no other options)

### VoiceOS Architecture Constraints
- **Profile:** android-app (Kotlin, Jetpack Compose, Room, Hilt)
- **Existing Service:** VoiceOSService is AccessibilityService
- **Singleton Pattern:** VoiceOSService uses static instance reference
- **Thread Model:** Coroutines (Dispatchers.Main + IO)

### Build System Constraints
- **Gradle:** Version 8.10.2
- **Kotlin:** Version 1.9.x
- **AIDL:** Must compile before Kotlin (ksp ordering)
- **Hilt:** Dependency injection present, must not conflict

### Security Constraints
- **Phase 1 (Current):** Signature protection only
- **Phase 2 (Future):** SDK with custom permission
- **Permission:** android:permission="signature"
- **Exported:** true (required for IPC)

---

## Dependencies

### Internal Dependencies
- **VoiceOSService** - Core accessibility service
- **VoiceOSServiceBinder.kt.phase3** - Already prepared, needs activation
- **IVoiceOSService.aidl** - AIDL interface (needs updates)
- **IVoiceOSCallback.aidl** - Callback interface (already exists)
- **Room Database** - For command/app queries
- **ActionCoordinator** - For accessibility actions
- **SpeechEngineManager** - For voice recognition control

### External Dependencies
- None (all Augmentalis internal)

### Blocking Dependencies
- None - all prerequisites complete

---

## Out of Scope

**Explicitly NOT included in this feature:**

### Deferred to Future Phases
- ❌ SDK for third-party developers (Phase 4)
- ❌ Custom permission infrastructure (Phase 4)
- ❌ OAuth/token-based auth (Phase 4)
- ❌ API rate limiting beyond Android defaults
- ❌ IPC versioning/compatibility layer

### Removed by User Request
- ❌ `isServiceReady()` method (removed from AIDL)
- ❌ `scrapeCurrentScreen()` method (removed from AIDL)

### Not Part of IPC Layer
- ❌ VoiceOSService internal refactoring
- ❌ Accessibility tree scraping improvements
- ❌ Voice recognition engine changes
- ❌ UI components (overlays, menus)
- ❌ Database schema changes

---

## Implementation Plan (High-Level)

### Phase 3a: AIDL Interface Updates (1-2 hours)
1. Update `IVoiceOSService.aidl`:
   - Remove `isServiceReady()` and `scrapeCurrentScreen()`
   - Add 6 new extended methods
2. Verify AIDL compilation
3. Update build configuration if needed

### Phase 3b: VoiceOSService API Exposure (1-2 hours)
1. Change `isServiceReady` visibility to internal
2. Verify `getInstance()` companion method works
3. Add extended method implementations if missing
4. Test service lifecycle

### Phase 3c: Activate VoiceOSServiceBinder (2-3 hours)
1. Rename `VoiceOSServiceBinder.kt.phase3` → `VoiceOSServiceBinder.kt`
2. Implement 6 new extended methods
3. Update existing methods per AIDL changes
4. Add error handling and logging
5. Test callback mechanism

### Phase 3d: Create VoiceOSIPCService (2-3 hours)
1. Create new service class
2. Implement onBind() with action filtering
3. Add lifecycle management (onCreate, onDestroy)
4. Integrate with VoiceOSServiceBinder
5. Add logging and error handling

### Phase 3e: Manifest & Build (1 hour)
1. Add service declaration to AndroidManifest.xml
2. Configure signature permission
3. Add intent-filter
4. Build and verify compilation

### Phase 3f: Testing & Documentation (2-3 hours)
1. Create integration tests
2. Test from VoiceCursor module
3. Verify all 12 methods work
4. Update developer documentation
5. Create usage examples

---

## Testing Strategy

### Unit Tests
- [ ] VoiceOSServiceBinder method tests (mock VoiceOSService)
- [ ] AIDL method parameter validation
- [ ] Error handling paths
- [ ] Callback registration/unregistration

### Integration Tests
- [ ] Bind from external app (VoiceCursor)
- [ ] Execute all 12 IPC methods
- [ ] Callback delivery verification
- [ ] Service lifecycle scenarios

### Manual Tests
- [ ] Install on device/emulator
- [ ] Bind from VoiceCursor
- [ ] Execute voice commands via IPC
- [ ] Start/stop voice recognition
- [ ] Learn current app
- [ ] Query commands

### Performance Tests
- [ ] IPC call latency (< 100ms)
- [ ] Memory usage (no leaks)
- [ ] Concurrent client handling
- [ ] Long-running stability

---

## Risk Assessment

### High Risk
**Risk:** AIDL + Hilt circular dependency
**Mitigation:** Keep AIDL separate, use late-init for dependencies
**Contingency:** Move binder to non-Hilt injected class

**Risk:** VoiceOSService not initialized when IPC binds
**Mitigation:** Check getInstance() != null, return error status
**Contingency:** Queue operations until service ready

### Medium Risk
**Risk:** Signature permission blocks legitimate use
**Mitigation:** Document signing requirements, test with VoiceCursor
**Contingency:** Switch to custom permission if needed

**Risk:** Performance degradation from IPC overhead
**Mitigation:** Use coroutines, measure latency, optimize hot paths
**Contingency:** Add caching layer if needed

### Low Risk
**Risk:** Build configuration issues
**Mitigation:** Follow VoiceCursor/UUIDCreator patterns
**Contingency:** Rollback to .phase3 if needed

---

## Acceptance Checklist

Before marking this feature complete:

### Functional
- [ ] VoiceOSIPCService created and compiles
- [ ] All 12 IPC methods implemented
- [ ] AIDL interfaces updated correctly
- [ ] Manifest configured properly
- [ ] Can bind from VoiceCursor successfully

### Quality
- [ ] 85%+ test coverage achieved
- [ ] All tests passing
- [ ] Performance within NFR limits
- [ ] No memory leaks detected
- [ ] Error handling complete

### Documentation
- [ ] KDoc on all public methods
- [ ] AIDL interface documented
- [ ] Developer integration guide created
- [ ] Usage examples provided
- [ ] ADR-006 updated with completion status

### Zero-Tolerance Compliance
- [ ] No AI attribution in code/commits
- [ ] Local timestamps only (no UTC)
- [ ] Functional equivalency maintained
- [ ] Documentation committed with code
- [ ] Explicit file staging (no git add .)

---

## References

- **ADR-006:** VoiceOSCore IPC Architecture - Phase 3 Requirement
  - Location: `docs/planning/architecture/decisions/ADR-006-VoiceOSCore-IPC-Architecture-Phase3-Requirement.md`
- **Phase 2 Commit:** `89921f2` - "Phase 2: IPC Service Binder Implementations (2/3 Complete)"
- **VoiceCursor Binder:** `modules/apps/VoiceCursor/src/main/java/com/augmentalis/voiceos/cursor/VoiceCursorServiceBinder.kt`
- **UUIDCreator Binder:** `modules/libraries/UUIDCreator/src/main/java/com/augmentalis/uuidcreator/UUIDCreatorServiceBinder.kt`
- **VoiceOSCore Binder (Phase 3):** `modules/apps/VoiceOSCore/src/main/java/com/augmentalis/voiceoscore/accessibility/VoiceOSServiceBinder.kt.phase3`
- **Android Docs:** https://developer.android.com/guide/components/aidl

---

## Approval

**Created By:** Manoj Jhawar
**Reviewed By:** [Pending]
**Approved By:** [Pending]
**Status:** Draft - Ready for Review

---

**Next Steps:**
1. Review specification for clarity and completeness
2. Run `/ideacode.clarify` if any ambiguities exist
3. Run `/ideacode.plan` to create detailed implementation plan
4. Begin Phase 3a: AIDL Interface Updates

---

**Document Version:** 1.0
**Last Updated:** 2025-11-11 19:00 PST
**IDEACODE Version:** 5.0
