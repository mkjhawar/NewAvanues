# Vivoka Error Listener Fix - Phased Correction Plan
**Date:** 2025-01-26
**Priority:** 🔴 CRITICAL
**Time Estimate:** 35 minutes total

## Overview

This document outlines the phased approach to fix the Vivoka error listener issue discovered during system readiness verification. The fix restores 100% functional equivalency with LegacyAvenue implementation.

## Phase 1: Implement Error Listener Fix (5 minutes)

### Objective
Connect error listener to enable proper error propagation

### Files to Modify
- `/libraries/SpeechRecognition/src/main/java/com/augmentalis/voiceos/speech/engines/vivoka/VivokaEngine.kt`

### Implementation Steps

#### Step 1.1: Add Error Listener Storage
```kotlin
// Add at line 62 (after other listener declarations)
private var errorListener: OnSpeechErrorListener? = null
```

#### Step 1.2: Update setErrorListener Method
```kotlin
// Replace lines 330-333
fun setErrorListener(listener: OnSpeechErrorListener) {
    errorListener = listener
    Log.d(TAG, "Error listener registered")
}
```

#### Step 1.3: Invoke Listener in onError
```kotlin
// Replace lines 365-376
override fun onError(codeString: String?, message: String?) {
    Log.e(TAG, "VSDK error - Code: $codeString, Message: $message")
    
    // Record performance failure
    performance.recordRecognition(System.currentTimeMillis(), null, 0f, false)
    
    // CRITICAL FIX: Notify error listener (matching LegacyAvenue functionality)
    errorListener?.invoke(
        "Vivoka SDK error [$codeString]: $message",  // Formatted like Legacy
        codeString?.toIntOrNull() ?: 500             // Error code
    )
    
    // Continue with existing error recovery
    coroutineScope.launch {
        Log.e(TAG, "VSDK Error - Code: $codeString, Message: $message")
    }
}
```

### Validation
- Verify code compiles without errors
- Check no existing functionality is broken
- Confirm error listener pattern matches other engines

## Phase 2: Create Unit Tests (15 minutes)

### Objective
Verify error handling works end-to-end

### Files to Create
- `/libraries/SpeechRecognition/src/test/java/com/augmentalis/voiceos/speech/engines/vivoka/VivokaEngineErrorTest.kt`

### Test Scenarios
```kotlin
class VivokaEngineErrorTest {
    
    @Test
    fun `test error listener receives SDK errors`() {
        // Given: VivokaEngine with error listener
        // When: onError is called with error code and message
        // Then: Error listener is invoked with formatted message
    }
    
    @Test
    fun `test error formatting matches Legacy`() {
        // Given: Error code "404" and message "Not found"
        // When: onError is called
        // Then: Listener receives "Vivoka SDK error [404]: Not found"
    }
    
    @Test
    fun `test null error code handling`() {
        // Given: Null error code
        // When: onError is called
        // Then: Listener receives code 500 as default
    }
    
    @Test
    fun `test error propagation through AIDL`() {
        // Given: VoiceRecognitionService with Vivoka engine
        // When: Vivoka engine triggers error
        // Then: AIDL callback receives error
    }
}
```

### Integration Test
- Test with actual VoiceRecognitionService
- Verify AIDL client receives errors
- Check error format consistency

## Phase 3: Documentation Updates (5 minutes)

### Objective
Document the fix and update project status

### Files to Update

#### 3.1 Update CHANGELOG
`/docs/CHANGELOG-MASTER.md`
```markdown
## [2025-01-26] - Critical Fixes
### Fixed
- Vivoka error listener now properly connected and invokes callbacks
- Error propagation chain restored: Vivoka → VoiceRecognitionService → AIDL clients
- Achieved 100% functional equivalency with LegacyAvenue error handling
```

#### 3.2 Update Session Learnings
`/Volumes/M Drive/Coding/Warp/Agent-Instructions/SESSION-LEARNINGS.md`
```markdown
## 2025-01-26: Critical Error Handling Fix

### Issue
- Vivoka error listener marked as UNUSED_PARAMETER
- Errors not propagated to AIDL clients

### Root Cause
- During SOLID refactoring, error listener connection was missed
- Comment indicated "handled by error handler component" but wasn't

### Learning
- Always verify listener connections are complete
- Test error scenarios, not just success paths
- Compare with Legacy implementation for functional equivalency
```

#### 3.3 Update Status Documents
- Mark Vivoka-Error-Listener-Issue-2025-01-26.md as RESOLVED
- Update VOS4-Status-Comprehensive-2025-01-28.md to 100% complete

## Phase 4: Verify Other Engines (10 minutes)

### Objective
Ensure all speech engines properly handle errors

### Files to Check
1. `/libraries/SpeechRecognition/src/main/java/com/augmentalis/voiceos/speech/engines/vosk/VoskEngine.kt`
2. `/libraries/SpeechRecognition/src/main/java/com/augmentalis/voiceos/speech/engines/whisper/WhisperEngine.kt`
3. `/libraries/SpeechRecognition/src/main/java/com/augmentalis/voiceos/speech/engines/android/AndroidSTTEngine.kt`

### Verification Checklist
- [ ] Error listener is stored when set
- [ ] Error listener is invoked in error callbacks
- [ ] Error format is consistent across engines
- [ ] All engines tested with error scenarios

## Phase 5: Final Testing & Validation (5 minutes)

### Manual Testing
1. Start VoiceRecognition app
2. Select Vivoka engine
3. Trigger various error conditions:
   - Network disconnection
   - Invalid configuration
   - Model loading failure
4. Verify error callbacks received in VoiceAccessibility app

### Automated Testing
```bash
# Run unit tests
./gradlew :libraries:SpeechRecognition:test

# Run integration tests
./gradlew :apps:VoiceRecognition:connectedAndroidTest

# Run AIDL communication tests
./gradlew :tests:testAIDLErrorPropagation
```

## Success Criteria

### Must Have (Critical)
- ✅ Error listener stored when set
- ✅ Error listener invoked on errors
- ✅ Errors propagate through AIDL
- ✅ No regression in existing functionality

### Should Have (Important)
- ✅ Unit tests pass
- ✅ Integration tests pass
- ✅ Error format matches Legacy
- ✅ All engines verified

### Nice to Have (Optional)
- Performance metrics for error handling
- Error recovery strategies documented
- Enhanced error logging

## Risk Mitigation

### Potential Risks
1. **Risk:** Breaking existing error recovery
   - **Mitigation:** Only add listener invocation, don't modify recovery logic

2. **Risk:** Performance impact
   - **Mitigation:** Simple callback invocation, negligible overhead

3. **Risk:** Thread safety issues
   - **Mitigation:** Use existing coroutine scope, follow existing patterns

## Rollback Plan

If issues occur after fix:
1. Revert changes in VivokaEngine.kt
2. Keep error logging for debugging
3. Document issue for further investigation
4. Consider alternative error handling approach

## Approval Checklist

Before proceeding with implementation:
- [ ] User approves phased plan
- [ ] Fix approach confirmed correct
- [ ] Testing strategy acceptable
- [ ] Documentation updates approved
- [ ] Timeline acceptable (35 minutes)

## Post-Implementation

After successful implementation:
1. Update all status documents to RESOLVED
2. Close related issues
3. Notify team of fix completion
4. Monitor for any error handling issues
5. Consider applying similar pattern to future engines

---

**Ready for implementation approval.**