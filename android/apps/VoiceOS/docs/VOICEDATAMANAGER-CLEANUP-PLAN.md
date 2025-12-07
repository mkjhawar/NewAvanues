# VoiceDataManager Cleanup Plan

**Created:** 2025-11-24
**Status:** Not Implemented (Planning Only)
**Issue:** Modules reference disabled VoiceDataManager module

---

## Problem Statement

After disabling the SQLDelight database module and VoiceDataManager module (due to pre-existing schema errors), several modules still contain code references to VoiceDataManager that cause compilation failures.

**Affected Modules:**
1. SpeechRecognition (`modules/libraries/SpeechRecognition`)
2. VoiceKeyboard (`modules/libraries/VoiceKeyboard`)
3. HUDManager (`modules/managers/HUDManager`)

---

## Root Cause Analysis

### Why VoiceDataManager Was Disabled

1. **SQLDelight Schema Errors:** Pre-existing errors in `ScrapedHierarchy.sq` prevent compilation
2. **User Decision:** SQLDelight conversion deferred to separate task (Option 2)
3. **Build Blocker:** Database module prevented full project build

### Dependency Chain

```
VoiceDataManager → SQLDelight Database → Schema Errors
         ↑
         |
    Depends on:
    - SpeechRecognition
    - VoiceKeyboard
    - HUDManager
```

---

## Cleanup Strategy

### Option 1: Remove VoiceDataManager Usage (Recommended)

**For each affected module:**

1. **Identify All References**
   ```bash
   # Search for VoiceDataManager imports
   grep -r "import.*VoiceDataManager" modules/libraries/SpeechRecognition/
   grep -r "import.*VoiceDataManager" modules/libraries/VoiceKeyboard/
   grep -r "import.*VoiceDataManager" modules/managers/HUDManager/

   # Search for VoiceDataManager usage
   grep -r "VoiceDataManager" modules/libraries/SpeechRecognition/src/
   grep -r "VoiceDataManager" modules/libraries/VoiceKeyboard/src/
   grep -r "VoiceDataManager" modules/managers/HUDManager/src/
   ```

2. **Categorize Usage Patterns**
   - Data persistence calls
   - Configuration reads
   - Logging/telemetry
   - User preferences
   - Command history

3. **Determine Replacement Strategy**
   - **Option A:** Use Room database directly (VoiceOSAppDatabase)
   - **Option B:** Use SharedPreferences for simple key-value data
   - **Option C:** Remove data persistence temporarily (if non-critical)
   - **Option D:** Implement local file-based storage

4. **Implement Replacement**
   - Replace VoiceDataManager calls with chosen alternative
   - Update imports
   - Update initialization code
   - Preserve functional equivalency

5. **Test Each Module**
   ```bash
   ./gradlew :modules:libraries:SpeechRecognition:compileDebugKotlin
   ./gradlew :modules:libraries:VoiceKeyboard:compileDebugKotlin
   ./gradlew :modules:managers:HUDManager:compileDebugKotlin
   ```

---

## Detailed Steps Per Module

### 1. SpeechRecognition Module

**Files to Check:**
```bash
find modules/libraries/SpeechRecognition/src -name "*.kt" -exec grep -l "VoiceDataManager" {} \;
```

**Expected Usage Patterns:**
- Recognition results persistence
- Language model data
- Voice profile storage
- Recognition metrics

**Recommended Replacement:**
- **Simple data:** SharedPreferences
- **Complex data:** Room database (VoiceOSAppDatabase)
- **Temporary data:** In-memory cache with file backup

**Steps:**
1. Audit all VoiceDataManager calls
2. Create replacement repository/DAO for Room
3. Update dependency injection (if using Hilt/Dagger)
4. Migrate data access code
5. Test recognition functionality
6. Verify no data loss

---

### 2. VoiceKeyboard Module

**Files to Check:**
```bash
find modules/libraries/VoiceKeyboard/src -name "*.kt" -exec grep -l "VoiceDataManager" {} \;
```

**Expected Usage Patterns:**
- Keyboard shortcuts
- Custom word dictionary
- User typing preferences
- Input history

**Recommended Replacement:**
- **Preferences:** SharedPreferences
- **Dictionary:** Room database or local JSON file
- **History:** Room database (with TTL cleanup)

**Steps:**
1. Identify data types being stored
2. Choose appropriate storage mechanism per data type
3. Create migration logic for existing data (if any)
4. Update all VoiceDataManager references
5. Test keyboard functionality
6. Verify settings persistence

---

### 3. HUDManager Module

**Files to Check:**
```bash
find modules/managers/HUDManager/src -name "*.kt" -exec grep -l "VoiceDataManager" {} \;
```

**Expected Usage Patterns:**
- HUD layout preferences
- Gaze calibration data
- Display settings
- Tracking metrics

**Recommended Replacement:**
- **Settings:** SharedPreferences
- **Calibration data:** Room database
- **Metrics:** Room database or in-memory with periodic flush

**Steps:**
1. Review HUD data requirements
2. Separate critical vs non-critical data
3. Implement SharedPreferences for preferences
4. Implement Room for calibration/metrics
5. Update HUD initialization
6. Test gaze tracking functionality

---

## Implementation Checklist

### Pre-Implementation
- [ ] Identify all VoiceDataManager references in each module
- [ ] Document current data flows
- [ ] Choose replacement strategy per module
- [ ] Plan data migration (if needed)

### Implementation (Per Module)
- [ ] Create replacement data layer (Room DAO / SharedPreferences wrapper)
- [ ] Update imports
- [ ] Replace VoiceDataManager calls
- [ ] Update dependency injection
- [ ] Add error handling
- [ ] Document changes

### Testing (Per Module)
- [ ] Module compiles successfully
- [ ] Functional tests pass
- [ ] Data persistence works
- [ ] No data loss
- [ ] Performance acceptable

### Verification
- [ ] All three modules compile
- [ ] VoiceOSCore builds successfully
- [ ] Full project builds
- [ ] Integration tests pass
- [ ] No regression in functionality

---

## Risk Assessment

### High Risk Areas

1. **Data Loss:** Incorrect migration could lose user data
   - **Mitigation:** Backup before migration, verify data integrity

2. **Performance Degradation:** Direct database access may be slower
   - **Mitigation:** Profile before/after, optimize queries

3. **Breaking Changes:** API changes could affect other modules
   - **Mitigation:** Maintain interface compatibility where possible

### Low Risk Areas

1. **Compilation Errors:** Easy to catch and fix
2. **Import Changes:** Straightforward find/replace
3. **Test Failures:** Indicate exactly what needs fixing

---

## Alternative Approaches

### Option 2: Re-enable VoiceDataManager (Not Recommended)

**Why Not Recommended:**
- Requires fixing SQLDelight schema errors first
- Adds complexity to LearnApp integration
- Blocks immediate progress

**If Pursued:**
1. Fix ScrapedHierarchy.sq foreign key issues
2. Resolve SQLDelight schema mismatches
3. Re-enable database module
4. Re-enable VoiceDataManager module
5. Test full build

### Option 3: Create VoiceDataManager Stub (Temporary)

**Pros:**
- Quick unblocking
- Maintains API compatibility
- Easy to replace later

**Cons:**
- No actual persistence
- Data loss on restart
- Technical debt

**Implementation:**
1. Create stub VoiceDataManager with in-memory storage
2. Implement minimal interface compatibility
3. Add TODO comments for real implementation
4. Use until proper replacement ready

---

## Recommended Execution Order

**Phase 1: Immediate (Unblock Build)**
1. Create VoiceDataManager stub (if needed for quick unblock)
2. OR remove non-critical VoiceDataManager calls

**Phase 2: Short-term (Proper Fix)**
1. Fix SpeechRecognition (highest priority - core functionality)
2. Fix VoiceKeyboard (medium priority - user-facing)
3. Fix HUDManager (lower priority - specialized use case)

**Phase 3: Long-term (SQLDelight Migration)**
1. Fix SQLDelight schema errors
2. Complete Room → SQLDelight migration
3. Re-enable VoiceDataManager with SQLDelight backend
4. Migrate modules back to VoiceDataManager

---

## Success Criteria

### Build Success
- [ ] All three modules compile without errors
- [ ] VoiceOSCore builds successfully
- [ ] Full project builds (./gradlew assembleDebug)

### Functional Equivalency
- [ ] SpeechRecognition works as before
- [ ] VoiceKeyboard preserves user data
- [ ] HUDManager displays correctly

### No Regressions
- [ ] LearnApp integration still functional
- [ ] VoiceOSCore accessibility service works
- [ ] No new crashes or errors

---

## Notes for Implementation

### Code Review Requirements
1. **All data access code:** Review for null safety
2. **Migration logic:** Verify no data loss
3. **Error handling:** Ensure graceful degradation
4. **Performance:** Profile critical paths

### Testing Requirements
1. **Unit tests:** For new data layer code
2. **Integration tests:** For module functionality
3. **Manual testing:** For user-facing features
4. **Regression tests:** For LearnApp integration

### Documentation Requirements
1. **Inline comments:** Explain replacement decisions
2. **Architecture docs:** Update data flow diagrams
3. **Changelog:** Document breaking changes
4. **Migration guide:** If API changes

---

## Related Documents

- **LearnApp Integration:** Complete (commit df051dc5)
- **SQLDelight Disabling:** Complete (commit 81b87b98)
- **CoT/ToT Review:** Complete (this session)
- **SQLDelight Migration:** Deferred (separate task)

---

## Estimated Effort

**Per Module:**
- Investigation: 30-60 minutes
- Implementation: 2-4 hours
- Testing: 1-2 hours
- Documentation: 30 minutes

**Total for 3 modules:** 12-20 hours

**Stub approach (quick unblock):** 2-4 hours total

---

## Contact / Questions

For questions about this cleanup plan:
1. Review this document
2. Check related commit messages (df051dc5, 24e81d68, 81b87b98)
3. Review SQLDelight schema errors in libraries/core/database

---

**Document Version:** 1.0
**Last Updated:** 2025-11-24
**Next Review:** After implementation begins
