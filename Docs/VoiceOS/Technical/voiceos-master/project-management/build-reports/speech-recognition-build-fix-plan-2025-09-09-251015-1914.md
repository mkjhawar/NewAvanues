# SpeechRecognition Build Error Fix Plan
**Created:** 2025-09-09
**Module:** SpeechRecognition Library
**Issue:** Vivoka Engine Compilation Errors

## Executive Summary
The SpeechRecognition module is experiencing build failures due to unresolved references in the Vivoka engine implementation. The errors stem from:
1. Local RecognizerError object conflicting with expected Vivoka SDK import
2. Incorrect SpeechError class structure (needs to be a data class, not an object)
3. Missing Vivoka SDK class references (Recognizer methods)

## COT (Chain of Thought) Analysis

### Root Cause Analysis
1. **VivokaErrorMapper.kt Issues:**
   - Line 4: Attempting to import `com.vivoka.vsdk.asr.recognizer.RecognizerError` but it doesn't exist in SDK
   - Lines 168-182: Local `RecognizerError` object defined at bottom of file
   - Problem: The local object is defined AFTER its usage, and the import statement conflicts

2. **SpeechError Reference Issues:**
   - Current: `SpeechError` is defined as an `object` with constants
   - Expected: `SpeechError` should be a data class with properties: `code`, `message`, `isRecoverable`, `suggestedAction`
   - The mapper expects `SpeechError.Action` enum which doesn't exist

3. **VivokaInitializer.kt Issues:**
   - Lines 157-166: Calling static methods on `Recognizer` class that don't exist
   - Line 226: `Vsdk.cleanup()` method doesn't exist

## ROT (Reflection on Thought) Evaluation

### Solution Evaluation
After analyzing the code structure:
- The Vivoka implementation appears to be a placeholder or incomplete integration
- The error handling pattern mirrors the VOSK implementation but with different requirements
- The SpeechError structure needs redesign to support both engines

### Edge Cases Considered
- Vivoka SDK might be optional (compileOnly dependency)
- Need to maintain compatibility with existing VOSK implementation
- Must preserve functional equivalency per VOS4 protocols

## TOT (Tree of Thought) - Solution Options

### Branch A: Quick Fix (Minimal Changes)
**Approach:** Fix imports and restructure classes
**Time:** 1 hour
**Risk:** Low
**Quality:** 7/10
**Changes:**
1. Remove Vivoka SDK import for RecognizerError
2. Move local RecognizerError object before usage
3. Create SpeechError data class with required structure
4. Comment out unsupported Vivoka SDK method calls

### Branch B: Complete Refactor
**Approach:** Redesign error handling architecture
**Time:** 4 hours
**Risk:** Medium
**Quality:** 9/10
**Changes:**
1. Create unified error handling interface
2. Implement engine-specific error mappers
3. Full Vivoka SDK integration review
4. Comprehensive testing

### Branch C: Fallback Mode
**Approach:** Disable Vivoka temporarily
**Time:** 30 minutes
**Risk:** Low
**Quality:** 5/10
**Changes:**
1. Comment out Vivoka engine files
2. Update build config to exclude Vivoka
3. Focus on VOSK engine only

### Branch D: Hybrid Approach
**Approach:** Fix critical errors, defer complete integration
**Time:** 2 hours
**Risk:** Low
**Quality:** 8/10
**Changes:**
1. Fix immediate compilation errors
2. Create proper SpeechError structure
3. Stub out missing Vivoka SDK methods
4. Add TODO markers for future completion

## Recommendation Matrix

| Criteria | Branch A | Branch B | Branch C | Branch D |
|----------|----------|----------|----------|----------|
| Time     | 1 hr     | 4 hrs    | 0.5 hr   | 2 hrs    |
| Risk     | Low      | Medium   | Low      | Low      |
| Quality  | 7        | 9        | 5        | 8        |
| **TOTAL**| **7.5**  | **7**    | **6**    | **8**     |

**Recommendation:** Branch D (Hybrid Approach) - Best balance of time, risk, and quality

## Implementation Plan

### Phase 1: Fix SpeechError Structure
1. Create new SpeechError data class in common package
2. Add Action enum for suggested actions
3. Update existing error code constants

### Phase 2: Fix VivokaErrorMapper
1. Remove conflicting import statement
2. Use local RecognizerError object
3. Update SpeechError references to use new structure

### Phase 3: Fix VivokaInitializer
1. Comment out or stub unsupported Recognizer methods
2. Add TODO markers for SDK integration
3. Ensure basic initialization flow works

### Phase 4: Verify Build
1. Run gradlew build for SpeechRecognition module
2. Fix any remaining compilation errors
3. Document deferred work items

## Files to Modify

1. `/modules/libraries/SpeechRecognition/src/main/java/com/augmentalis/voiceos/speech/engines/common/SpeechError.kt`
   - Convert from object to data class structure

2. `/modules/libraries/SpeechRecognition/src/main/java/com/augmentalis/voiceos/speech/engines/vivoka/VivokaErrorMapper.kt`
   - Remove line 4 import
   - Move RecognizerError object to top of file

3. `/modules/libraries/SpeechRecognition/src/main/java/com/augmentalis/voiceos/speech/engines/vivoka/VivokaInitializer.kt`
   - Comment out lines 157-166 (Recognizer method calls)
   - Comment out line 226 (Vsdk.cleanup())

## Success Criteria
- [ ] Module compiles without errors
- [ ] All existing tests pass
- [ ] VOSK engine remains functional
- [ ] Vivoka stubs don't break runtime

## Risk Mitigation
- Create backup of current files before modification
- Test each change incrementally
- Maintain functional equivalency per VOS4 protocols
- Document all temporary workarounds for future resolution

## Next Steps
1. Implement Branch D solution
2. Run build verification
3. Update module status documentation
4. Create follow-up tasks for complete Vivoka integration

---
**Status:** Ready for Implementation
**Priority:** CRITICAL
**Estimated Completion:** 2 hours