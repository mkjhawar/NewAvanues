# ADR-006: VoiceOS Command Delegation Pattern

**Status:** Accepted
**Date:** 2025-11-16
**Authors:** AVA AI Team
**Related:** ADR-005 (Multi-Source Intent System), Chapter 36

---

## Context

After implementing the multi-source intent system (ADR-005), we discovered VoiceOS already provides:
- AccessibilityService for UI automation
- Command hierarchy database
- Clickable elements database
- App context information

This raised an architectural question: Should AVA implement its own AccessibilityService to execute commands, or delegate execution to VoiceOS?

### Problem Statement

**User Requirement:**
> "AVA should check if VoiceOS is installed, then query VoiceOS databases for app information (clickable elements, command hierarchies) to execute multi-step commands like 'call John Thomas on teams'"

**Initial Assumption (INCORRECT):**
We initially planned to have AVA:
1. Query VoiceOS for command hierarchies ✅ Correct
2. Implement its own AccessibilityService ❌ Wrong
3. Execute commands using AVA's AccessibilityService ❌ Wrong

**User Clarification:**
> "Why do you need to implement AccessibilityService when that is part of /voiceos? Since we already have VoiceOS serving as the command manager and executing instructions, should we not delegate back to VoiceOS, or should we keep AVA execution within AVA?"

This clarification revealed the correct architecture.

---

## Decision

**AVA will delegate all command execution to VoiceOS.**

AVA's role is limited to:
1. Natural language understanding (NLU)
2. Intent classification
3. Query VoiceOS for available commands
4. Delegate execution to VoiceOS
5. Generate LLM responses based on execution results

VoiceOS's role remains:
1. Platform services (AccessibilityService)
2. Command management and storage
3. UI automation and execution
4. Result reporting

---

## Rationale

### Why Delegation is Correct

#### 1. Separation of Concerns

| Component | Responsibility | Layer |
|-----------|----------------|-------|
| **AVA** | NLU, conversation, LLM responses | Application Layer |
| **VoiceOS** | Platform services, UI automation | OS/Platform Layer |

This follows standard OS architecture:
- **VoiceOS = Platform** (like Android Framework)
- **AVA = App** (like Gmail, Chrome)
- Apps don't implement platform services, they use them

#### 2. Avoids Duplicate AccessibilityService

**Problem with duplicate:**
```
AVA AccessibilityService      VoiceOS AccessibilityService
         │                              │
         ├──────── Competing ───────────┤
         │       for UI events           │
         │                              │
    Which one handles?              Conflicts!
```

**With delegation:**
```
AVA (No AccessibilityService)     VoiceOS AccessibilityService
         │                                 │
         │  1. Request execution           │
         ├─────────────────────────────────>
         │                                 │
         │                         2. Execute via
         │                         AccessibilityService
         │                                 │
         │  3. Return result               │
         <─────────────────────────────────┤
```

#### 3. Single Source of Truth

**Without delegation:**
- AVA execution logic (in AVA codebase)
- VoiceOS execution logic (in VoiceOS codebase)
- Must keep in sync
- Bug fixes in one don't help the other

**With delegation:**
- VoiceOS execution logic (single location)
- All apps using VoiceOS benefit from improvements
- Bug fixes automatically propagate

#### 4. Permission Model

**Without delegation:**
```xml
<!-- AVA AndroidManifest.xml -->
<uses-permission android:name="android.permission.BIND_ACCESSIBILITY_SERVICE" />

<!-- VoiceOS AndroidManifest.xml -->
<uses-permission android:name="android.permission.BIND_ACCESSIBILITY_SERVICE" />
```
**Problem:** Two apps requesting same dangerous permission, user confusion

**With delegation:**
```xml
<!-- AVA AndroidManifest.xml -->
<!-- No dangerous permissions needed -->

<!-- VoiceOS AndroidManifest.xml -->
<uses-permission android:name="android.permission.BIND_ACCESSIBILITY_SERVICE" />
```
**Benefit:** Clean permission model, only platform requests dangerous permission

#### 5. Follows Android Best Practices

**Android Principle:**
> "Don't reinvent the wheel. If a platform service exists, use it."

Examples:
- Apps don't implement their own location service → Use `LocationManager`
- Apps don't implement their own camera → Use `CameraX`
- Apps don't implement their own notifications → Use `NotificationManager`

**Our case:**
- AVA shouldn't implement its own UI automation → Use VoiceOS

---

## Alternatives Considered

### Alternative 1: AVA Self-Execution (REJECTED)

**Architecture:**
```
User → AVA NLU → Query VoiceOS DB → AVA AccessibilityService executes
```

**Pros:**
- Full control over execution
- No dependency on VoiceOS execution service
- Can customize for AVA-specific needs

**Cons:**
- ❌ Duplicate AccessibilityService (AVA + VoiceOS)
- ❌ Duplicate execution logic
- ❌ Permission bloat (AVA needs dangerous permissions)
- ❌ Two AccessibilityServices competing
- ❌ Maintenance burden (keep two codebases in sync)
- ❌ Violates DRY principle
- ❌ Larger attack surface

**Rejected because:** Cons far outweigh pros

### Alternative 2: Hybrid Approach (REJECTED)

**Architecture:**
```
Simple commands → AVA executes
Complex commands → VoiceOS executes
```

**Pros:**
- AVA can work standalone for simple commands
- Complex commands use VoiceOS

**Cons:**
- ❌ Complexity: Which path to use?
- ❌ Still requires AVA AccessibilityService
- ❌ Inconsistent UX (some commands work, some don't)
- ❌ Hard to define "simple" vs "complex"

**Rejected because:** Adds complexity without sufficient benefit

### Alternative 3: Delegation (CHOSEN) ⭐

**Architecture:**
```
User → AVA NLU → Query VoiceOS DB → Delegate to VoiceOS → VoiceOS executes
```

**Pros:**
- ✅ Clean separation of concerns
- ✅ Single AccessibilityService
- ✅ Single source of truth
- ✅ No permission bloat
- ✅ Follows Android best practices
- ✅ Automatic improvements propagate
- ✅ Easier testing (mock VoiceOS service)

**Cons:**
- ⚠️ Dependency on VoiceOS (already exists for queries)
- ⚠️ Extra IPC call (~5-10ms overhead, negligible)
- ⚠️ Can't execute without VoiceOS (graceful degradation)

**Chosen because:** Pros far outweigh cons, aligns with platform architecture

---

## Implementation Details

### AVA Changes (2-3 hours)

**File:** `Universal/AVA/Features/NLU/src/androidMain/kotlin/com/augmentalis/ava/features/nlu/VoiceOSIntegration.kt`

**Add Methods:**
```kotlin
suspend fun delegateCommandExecution(
    commandId: String,
    parameters: Map<String, String> = emptyMap()
): ExecutionResult

private fun requestExecution(
    commandId: String,
    parameters: Map<String, String>
): String?

private suspend fun waitForExecutionResult(
    executionId: String
): ExecutionResult

fun registerExecutionCallback(
    callback: (ExecutionResult) -> Unit
)
```

**Add Data Classes:**
```kotlin
sealed class ExecutionResult {
    data class Success(message: String, executedSteps: Int, executionTimeMs: Long)
    data class Error(reason: String, failedAtStep: Int?)
    object Timeout
    object VoiceOSNotInstalled
}
```

### VoiceOS Changes (If Not Already Implemented)

**ContentProvider Endpoints:**

1. `/execute_command` - Accept execution requests
   - Input: command_id, parameters, requested_by, timestamp
   - Output: execution_id

2. `/execution_result/{id}` - Query execution status
   - Input: execution_id
   - Output: status, message, executed_steps, execution_time_ms

**Broadcast:**
- Action: `com.avanues.voiceos.EXECUTION_COMPLETE`
- Extras: execution_id, status, message, executed_steps

---

## Consequences

### Positive

✅ **Clean Architecture**
- AVA = Application layer (NLU, LLM, conversation)
- VoiceOS = Platform layer (UI automation, command execution)
- Clear separation of concerns

✅ **No Duplication**
- Single AccessibilityService (VoiceOS)
- Single execution logic (VoiceOS)
- DRY principle maintained

✅ **Automatic Improvements**
- VoiceOS execution improvements benefit all apps
- Bug fixes propagate automatically
- No sync required

✅ **Simpler AVA**
- Fewer permissions
- Less code
- Easier to maintain
- Smaller APK

✅ **Better UX**
- No AccessibilityService conflicts
- Consistent execution across all apps
- Single permission request (VoiceOS)

✅ **Easier Testing**
- Mock VoiceOS execution service
- No AccessibilityService needed in tests
- Integration tests simpler

### Negative

⚠️ **VoiceOS Dependency**
- AVA cannot execute complex commands without VoiceOS
- Mitigation: Graceful degradation to basic actions

⚠️ **IPC Overhead**
- Extra ContentProvider call (~5-10ms)
- Mitigation: Negligible for 30s command execution

⚠️ **Polling Overhead**
- Poll every 500ms for status
- Mitigation: Use broadcast receiver alternative

### Neutral

◼️ **VoiceOS Required**
- User must install VoiceOS for complex commands
- This was already a requirement for database queries

---

## Migration Path

### Phase 1: ✅ COMPLETE
- Implement VoiceOS database queries
- Query app context, clickable elements, command hierarchies

### Phase 2: 🔄 IN PROGRESS
- Add delegation methods to VoiceOSIntegration.kt
- Implement ExecutionResult sealed class
- Add polling logic

### Phase 3: TODO
- Update ChatViewModel to use delegation
- Remove placeholder executeCommandStep() method
- Update documentation

### Phase 4: TODO
- Write unit tests with mock VoiceOS service
- Test with real VoiceOS installed
- Performance testing

---

## Testing Strategy

### Unit Tests

**Mock VoiceOS ContentProvider:**
```kotlin
@Test
fun testSuccessfulDelegation() = runTest {
    val mockProvider = MockVoiceOSProvider().apply {
        mockExecutionResult("exec_123", ExecutionResult.Success(
            message = "Command executed",
            executedSteps = 3,
            executionTimeMs = 2500
        ))
    }

    val integration = VoiceOSIntegration(mockContext)
    val result = integration.delegateCommandExecution("cmd_call_teams")

    assertTrue(result is ExecutionResult.Success)
    assertEquals(3, (result as ExecutionResult.Success).executedSteps)
}
```

### Integration Tests

**With Real VoiceOS:**
```kotlin
@Test
@RequiresDevice  // VoiceOS must be installed
fun testRealVoiceOSExecution() = runTest {
    val integration = VoiceOSIntegration(context)

    assumeTrue(integration.isVoiceOSInstalled())

    val result = integration.delegateCommandExecution("test_command")

    when (result) {
        is ExecutionResult.Success -> Log.i(TAG, "Test passed")
        is ExecutionResult.Error -> fail("Execution failed: ${result.reason}")
        else -> fail("Unexpected result: $result")
    }
}
```

---

## Performance Considerations

### IPC Overhead

**Baseline (Self-execution):**
- AVA AccessibilityService finds element: ~50ms
- Execute action: ~100ms
- Total: ~150ms

**With Delegation:**
- AVA → VoiceOS ContentProvider: ~5ms
- VoiceOS AccessibilityService finds element: ~50ms
- Execute action: ~100ms
- VoiceOS → AVA result: ~5ms
- Total: ~160ms

**Overhead:** 10ms (~6% increase, negligible for 30s operations)

### Polling vs Broadcast

**Polling (Current):**
- Poll every 500ms
- For 5s execution: ~10 queries
- Network overhead: ~50ms total

**Broadcast (Alternative):**
- Zero polling
- Single broadcast on completion
- Network overhead: ~5ms

**Recommendation:** Implement both, use broadcast when available

---

## Security Considerations

### Permission Model

**Without Delegation:**
```
AVA permissions:
- android.permission.BIND_ACCESSIBILITY_SERVICE (dangerous)
- android.permission.SYSTEM_ALERT_WINDOW (dangerous)

VoiceOS permissions:
- android.permission.BIND_ACCESSIBILITY_SERVICE (dangerous)
- android.permission.SYSTEM_ALERT_WINDOW (dangerous)
```
**Risk:** Two apps with dangerous permissions

**With Delegation:**
```
AVA permissions:
- (none dangerous)

VoiceOS permissions:
- android.permission.BIND_ACCESSIBILITY_SERVICE (dangerous)
- android.permission.SYSTEM_ALERT_WINDOW (dangerous)
```
**Benefit:** Single app with dangerous permissions, smaller attack surface

### IPC Security

**ContentProvider Security:**
```xml
<!-- VoiceOS AndroidManifest.xml -->
<provider
    android:name=".VoiceOSProvider"
    android:authorities="com.avanues.voiceos.provider"
    android:exported="true"
    android:permission="com.avanues.voiceos.permission.EXECUTE_COMMAND" />

<!-- Define custom permission -->
<permission
    android:name="com.avanues.voiceos.permission.EXECUTE_COMMAND"
    android:protectionLevel="signature" />
```

**Ensures:** Only apps signed with same key can execute commands

---

## References

- **Developer Manual Chapter 36**: VoiceOS Command Delegation
- **ADR-005**: Multi-Source Intent System
- **VoiceOS Documentation**: Command Execution API
- **Android Guide**: Accessibility Services Best Practices

---

## Decision Log

| Date | Decision | Rationale |
|------|----------|-----------|
| 2025-11-16 | Initially planned AVA AccessibilityService | Misunderstanding of VoiceOS role |
| 2025-11-16 | User clarified VoiceOS already has service | VoiceOS is platform layer |
| 2025-11-16 | **Decided: Delegate to VoiceOS** | Clean architecture, no duplication |
| 2025-11-16 | Use ContentProvider for IPC | Standard Android IPC mechanism |
| 2025-11-16 | Support both polling and broadcast | Flexibility for different use cases |

---

**Status:** Accepted
**Implementation:** Pending (2-3 hours)
**Documentation:** Complete
**Testing:** Pending
