# JIT-LearnApp Merge - Testing Guide

**Document**: JIT-LearnApp Merge Testing Guide
**Created**: 2025-12-04
**Author**: Manoj Jhawar
**Status**: Phase 4 Testing
**Related**: [jit-learnapp-merge-developer-guide-251204.md](./jit-learnapp-merge-developer-guide-251204.md)

---

## Overview

This guide provides comprehensive testing procedures for the JIT-LearnApp merge implementation. The merge unifies element processing between JIT Mode and Full Exploration Mode by extracting shared business logic into LearnAppCore.

**Critical Gap Addressed**: Full Exploration mode now generates voice commands (previously only generated UUIDs, making discovered elements unusable with voice control).

---

## Testing Summary

| Phase | Status | Duration |
|-------|--------|----------|
| Phase 1 - LearnAppCore Extraction | ✅ Complete | 1.5 hours |
| Phase 2 - JIT Refactor | ✅ Complete | 1.5 hours |
| Phase 3 - Exploration Refactor | ✅ Complete | 1.5 hours |
| **Phase 4 - Testing** | 🔄 In Progress | 2 hours |

---

## Test Categories

### 1. Unit Tests (Code Level)
- **Status**: Deferred (requires test infrastructure setup)
- **Reason**: Complex mocking requirements for database and UUID generator
- **Recommendation**: Create after verifying integration tests pass

### 2. Integration Tests (Feature Level)
- **Status**: Manual testing required
- **Coverage**: JIT Mode, Exploration Mode, Database, Error handling

### 3. Manual Tests (End-to-End)
- **Status**: Ready for execution
- **Coverage**: Real-world app learning scenarios

---

## Manual Testing Procedures

### Test 1: JIT Mode - Voice Command Generation

**Objective**: Verify JIT Mode generates voice commands when user taps elements

**Prerequisites**:
- VoiceOS installed on device
- Accessibility Service enabled
- Test app installed (e.g., Google Photos, Instagram)

**Steps**:
1. Open VoiceOS settings
2. Enable "Learn new apps" (JIT Mode)
3. Open test app (e.g., Google Photos)
4. Grant consent when prompted
5. Tap various UI elements (buttons, tabs, menus)
6. For each tap, observe overlay showing:
   - Element UUID
   - Generated voice command
   - "Learning..." progress

**Expected Results**:
- ✅ Each tapped element shows UUID in overlay
- ✅ Each tapped element shows voice command (e.g., "click photos", "tap settings")
- ✅ Commands use appropriate action types:
  - Buttons → "click"
  - EditText → "type"
  - Scrollable → "scroll"
- ✅ Commands include synonyms (tap/press/select for click)
- ✅ Database contains both UUID and command after each tap

**How to Verify Database**:
```bash
# Use adb shell to query database
adb shell "su -c 'sqlite3 /data/data/com.augmentalis.voiceos/databases/voiceos.db \"SELECT elementHash, commandText, actionType FROM GeneratedCommand ORDER BY createdAt DESC LIMIT 10;\"'"
```

**Pass Criteria**:
- All tapped elements have commands in database
- Action types are correct (click/type/scroll)
- Command text is meaningful (not empty or gibberish)

---

### Test 2: Exploration Mode - Batch Voice Command Generation

**Objective**: Verify Exploration Mode generates voice commands for all discovered elements

**Prerequisites**:
- VoiceOS installed on device
- Accessibility Service enabled
- Test app installed (e.g., Teams, Gmail)

**Steps**:
1. Open VoiceOS settings
2. Enable "Full App Learning" (Exploration Mode)
3. Select test app from list
4. Grant consent when prompted
5. Wait for exploration to complete (progress overlay shows)
6. After exploration finishes, check:
   - Number of elements discovered
   - Number of commands generated
   - Database contents

**Expected Results**:
- ✅ Progress overlay shows element count incrementing
- ✅ ALL discovered elements have voice commands generated (not just UUIDs)
- ✅ Batch processing is fast (~50ms for 100 elements)
- ✅ Database contains commands for ALL interactive elements
- ✅ Commands are unique (no duplicates for same element)
- ✅ Commands use correct action types based on element class

**How to Verify Database**:
```bash
# Count commands generated during exploration
adb shell "su -c 'sqlite3 /data/data/com.augmentalis.voiceos/databases/voiceos.db \"SELECT COUNT(*) FROM GeneratedCommand WHERE createdAt > strftime('%s','now') - 300;\"'"

# View sample commands
adb shell "su -c 'sqlite3 /data/data/com.augmentalis.voiceos/databases/voiceos.db \"SELECT commandText, actionType, confidence FROM GeneratedCommand ORDER BY createdAt DESC LIMIT 20;\"'"
```

**Pass Criteria**:
- Element count == Command count (1:1 ratio)
- No elements have NULL command text
- Batch flush completes in <100ms
- No duplicate commands for same element hash

---

### Test 3: Database Insertion Verification

**Objective**: Verify commands are correctly inserted with all required fields

**Prerequisites**:
- Tests 1 and 2 completed
- Database has sample commands

**Steps**:
1. Query database for generated commands
2. Verify each command has:
   - elementHash (12-char MD5)
   - commandText (lowercase, with action)
   - actionType (click/type/scroll/long_click)
   - confidence (0.85)
   - synonyms (JSON array)
   - createdAt (timestamp)

**Expected Database Schema**:
```sql
CREATE TABLE GeneratedCommand (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    elementHash TEXT NOT NULL,
    commandText TEXT NOT NULL,
    actionType TEXT NOT NULL,
    confidence REAL NOT NULL,
    synonyms TEXT,  -- JSON array
    isUserApproved INTEGER DEFAULT 0,
    usageCount INTEGER DEFAULT 0,
    lastUsed INTEGER,
    createdAt INTEGER NOT NULL
);
```

**Verification Query**:
```bash
adb shell "su -c 'sqlite3 /data/data/com.augmentalis.voiceos/databases/voiceos.db \"SELECT * FROM GeneratedCommand LIMIT 5;\"'"
```

**Pass Criteria**:
- ✅ All required fields are non-NULL
- ✅ elementHash is 12 characters
- ✅ commandText is lowercase
- ✅ actionType is valid (click/type/scroll/long_click)
- ✅ confidence is 0.85
- ✅ synonyms is valid JSON array
- ✅ createdAt is recent timestamp

---

### Test 4: Error Handling

**Objective**: Verify graceful error handling for edge cases

**Test 4.1: Element with No Label**

**Steps**:
1. Use adb to simulate element with no text/contentDescription/resourceId
2. Process element through LearnAppCore
3. Verify no crash, error logged, command is NULL

**Expected**:
- ✅ No crash
- ✅ Result.success = false
- ✅ Result.error = "No label found for command"
- ✅ No database insertion

**Test 4.2: Very Short Label**

**Steps**:
1. Process element with text = "X" (1 char)
2. Verify command is skipped

**Expected**:
- ✅ Command skipped (labels < 2 chars ignored)
- ✅ No database insertion

**Test 4.3: Digit-Only Label**

**Steps**:
1. Process element with text = "123"
2. Verify command is skipped

**Expected**:
- ✅ Command skipped (meaningless label)
- ✅ No database insertion

**Test 4.4: Database Insert Failure**

**Steps**:
1. Fill database to capacity (or simulate disk full)
2. Attempt to insert command
3. Verify error is caught and logged

**Expected**:
- ✅ No crash
- ✅ Result.success = false
- ✅ Error logged to VoiceOSLogger

---

### Test 5: Backward Compatibility

**Objective**: Verify old JIT/Exploration code still works if LearnAppCore is NULL

**Test 5.1: JIT Mode Fallback**

**Steps**:
1. Temporarily set `learnAppCore = null` in JustInTimeLearner
2. Tap elements
3. Verify old UUID generation still works

**Expected**:
- ✅ Elements get UUIDs (using old ThirdPartyUuidGenerator)
- ✅ No voice commands generated (expected)
- ✅ No crashes

**Test 5.2: Exploration Mode Fallback**

**Steps**:
1. Temporarily set `learnAppCore = null` in ExplorationEngine
2. Run exploration
3. Verify old UUID-only behavior

**Expected**:
- ✅ Elements get UUIDs
- ✅ No voice commands generated (expected)
- ✅ No crashes

---

### Test 6: Performance Benchmarks

**Objective**: Verify batch processing performance meets targets

**Test 6.1: IMMEDIATE Mode Performance**

**Steps**:
1. Enable JIT Mode
2. Tap 10 elements rapidly
3. Measure time per element

**Expected**:
- ✅ ~10ms per element (UUID + command + insert)
- ✅ No UI lag
- ✅ Overlay updates smoothly

**Test 6.2: BATCH Mode Performance**

**Steps**:
1. Enable Exploration Mode
2. Explore app with 100+ elements
3. Measure batch flush time

**Expected**:
- ✅ ~50ms for 100 elements (batch flush)
- ✅ 20x faster than individual inserts
- ✅ Peak memory: ~150KB (batch queue)

**How to Measure**:
```kotlin
// In LearnAppCore.kt, logs are already present:
Log.i(TAG, "Flushed $count commands in ${elapsedMs}ms (~$rate commands/sec)")
```

**Pass Criteria**:
- Batch flush completes in <100ms for 100 commands
- Individual insert completes in <15ms

---

## Test Checklist

### Pre-Testing Setup
- [ ] VoiceOS installed on physical device (NOT emulator for voice features)
- [ ] Accessibility Service enabled
- [ ] Test apps installed (Google Photos, Teams, Gmail, Instagram)
- [ ] adb access configured for database queries
- [ ] Logs enabled (verbose level for LearnAppCore, JustInTimeLearner, ExplorationEngine)

### JIT Mode Tests
- [ ] Test 1: Voice command generation
- [ ] Test 4.1: Element with no label
- [ ] Test 4.2: Very short label
- [ ] Test 4.3: Digit-only label
- [ ] Test 5.1: Backward compatibility
- [ ] Test 6.1: IMMEDIATE mode performance

### Exploration Mode Tests
- [ ] Test 2: Batch voice command generation
- [ ] Test 4.4: Database insert failure
- [ ] Test 5.2: Backward compatibility
- [ ] Test 6.2: BATCH mode performance

### Database Tests
- [ ] Test 3: Database insertion verification
- [ ] Verify schema matches expected structure
- [ ] Check for duplicate commands

### Error Handling
- [ ] All Test 4 scenarios
- [ ] No crashes observed
- [ ] All errors logged properly

---

## Known Issues & Limitations

### Current Limitations
1. **Unit tests not implemented**: Requires test infrastructure setup for database mocking
2. **No automated integration tests**: Manual testing required
3. **Performance benchmarks**: Based on logs, not automated metrics

### Future Enhancements
1. **Automated test suite**: Create MockK-based unit tests for LearnAppCore
2. **CI/CD integration**: Add tests to build pipeline
3. **Performance regression tests**: Automated performance benchmarks
4. **Robolectric tests**: Android unit tests without emulator

---

## Troubleshooting

### Issue: No voice commands in database after JIT learning

**Diagnosis**:
```bash
# Check if LearnAppCore is NULL
adb logcat -s LearnAppCore:D
# Look for: "Generated command: ..." (indicates LearnAppCore active)
```

**Solution**: Verify LearnAppIntegration.kt creates and passes LearnAppCore

### Issue: Exploration mode very slow

**Diagnosis**:
```bash
# Check batch flush time
adb logcat -s LearnAppCore:I | grep "Flushed"
# Should see: "Flushed 100 commands in ~50ms"
```

**Solution**: Verify BATCH mode is used, not IMMEDIATE

### Issue: Duplicate commands for same element

**Diagnosis**:
```bash
# Check for duplicate element hashes
adb shell "su -c 'sqlite3 /data/data/com.augmentalis.voiceos/databases/voiceos.db \"SELECT elementHash, COUNT(*) FROM GeneratedCommand GROUP BY elementHash HAVING COUNT(*) > 1;\"'"
```

**Solution**: Verify UUID generation is deterministic (same element = same hash)

---

## Regression Testing

**Before Each Release:**
- [ ] Run full test suite on 3+ different apps
- [ ] Verify no performance degradation
- [ ] Check database for data corruption
- [ ] Verify backward compatibility

**Apps for Regression Testing:**
1. Google Photos (complex UI, many elements)
2. Microsoft Teams (tabs, menus, drawers)
3. Gmail (scrollable lists, swipe actions)
4. Instagram (image buttons, stories)

---

## Test Results Template

```markdown
## Test Execution Report

**Date**: YYYY-MM-DD
**Tester**: Name
**Device**: Model (Android version)
**Build**: VoiceOS version

### JIT Mode Tests
- [ ] Test 1: Voice command generation - PASS/FAIL
  - Notes: ...
- [ ] Test 4.1-4.3: Error handling - PASS/FAIL
  - Notes: ...

### Exploration Mode Tests
- [ ] Test 2: Batch generation - PASS/FAIL
  - Elements discovered: X
  - Commands generated: Y
  - Time taken: Z ms
  - Notes: ...

### Performance Benchmarks
- IMMEDIATE mode: X ms/element
- BATCH mode: Y ms/100 elements

### Issues Found
1. Issue description
   - Severity: Critical/High/Medium/Low
   - Steps to reproduce
   - Expected vs Actual

### Overall Assessment
- All critical tests: PASS/FAIL
- Ready for release: YES/NO
```

---

## Conclusion

This testing guide provides comprehensive manual testing procedures for the JIT-LearnApp merge. Focus on:

1. **Functional correctness**: Both modes generate voice commands
2. **Performance**: Batch processing is fast
3. **Data integrity**: Database has valid, complete commands
4. **Error handling**: Graceful degradation for edge cases
5. **Backward compatibility**: Old code still works if LearnAppCore disabled

**Next Steps**:
1. Execute manual test suite
2. Document results
3. Fix any issues found
4. Create automated tests (future enhancement)
5. Merge to main branch after QA approval

---

**Document Version**: 1.0
**Last Updated**: 2025-12-04
**Next Review**: After Phase 4 testing completion
